package com.guidewire.ai.agent.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidewire.ai.agent.config.AgentConfig;
import com.guidewire.ai.agent.conversation.ConversationAgent;
import com.guidewire.ai.agent.conversation.ConversationResponse;
import com.guidewire.ai.agent.conversation.ConversationState;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight REST server for the AI Quote Agent, built on the JDK-bundled
 * {@code com.sun.net.httpserver} — zero additional dependencies required.
 *
 * <h3>Endpoints</h3>
 * <pre>
 *   POST /api/quote/message
 *     Request:  { "sessionId": "uuid | null", "message": "..." }
 *     Response: { "sessionId": "uuid", "response": "...", "isComplete": false }
 *
 *   GET /api/quote/health
 *     Response: { "status": "ok", "activeSessions": N }
 * </pre>
 *
 * <pre>
 *   java -jar target/ai-quote-agent-server.jar
 * </pre>
 */
public class QuoteAgentServer {

    private static final Logger logger = LoggerFactory.getLogger(QuoteAgentServer.class);

    /** Bundles a session's conversation state with its last-access timestamp. */
    private static final class SessionEntry {
        final ConversationState state;
        volatile long lastAccessMs;

        SessionEntry(ConversationState state) {
            this.state        = state;
            this.lastAccessMs = System.currentTimeMillis();
        }

        void touch() { this.lastAccessMs = System.currentTimeMillis(); }
    }

    // ── Fields ────────────────────────────────────────────────────────

    private final int                            port;
    private final long                           sessionTtlMs;
    private final ConversationAgent              agent;
    private final ObjectMapper                   mapper;
    private final ConcurrentHashMap<String, SessionEntry> sessions;
    private final ScheduledExecutorService       reaper;
    private       HttpServer                     server;

    // ── Constructor ───────────────────────────────────────────────────

    public QuoteAgentServer() {
        AgentConfig cfg   = AgentConfig.getInstance();
        this.port         = cfg.getServerPort();
        this.sessionTtlMs = cfg.getSessionTimeoutMinutes() * 60_000L;
        this.agent        = new ConversationAgent();
        this.mapper       = new ObjectMapper();
        this.sessions     = new ConcurrentHashMap<>();
        this.reaper       = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-reaper");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/quote/message", this::handleMessage);
        server.createContext("/api/quote/health",  this::handleHealth);
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        // Reap expired sessions every 5 minutes
        reaper.scheduleAtFixedRate(this::reapExpiredSessions, 5, 5, TimeUnit.MINUTES);

        logger.info("QuoteAgentServer started on port {}", port);
        System.out.println("QuoteAgentServer listening on http://localhost:" + port);
        System.out.println("  POST /api/quote/message");
        System.out.println("  GET  /api/quote/health");
        System.out.println("Press Ctrl+C to stop.");
    }

    public void stop() {
        if (server != null) server.stop(2);
        reaper.shutdownNow();
        agent.shutdown();
        logger.info("QuoteAgentServer stopped.");
    }

    // ── HTTP handlers ─────────────────────────────────────────────────

    private void handleMessage(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }

        try {
            // Parse request JSON
            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            JsonNode req = mapper.readTree(requestBody);
            String message   = req.path("message").asText(null);
            String sessionId = req.path("sessionId").isNull()
                    ? null : req.path("sessionId").asText(null);

            if (message == null || message.trim().isEmpty()) {
                sendError(exchange, 400, "\"message\" field is required");
                return;
            }

            // Resolve or create session
            if (sessionId == null || !sessions.containsKey(sessionId)) {
                sessionId = UUID.randomUUID().toString();
                sessions.put(sessionId, new SessionEntry(null));
                logger.debug("New session: {}", sessionId);
            }

            SessionEntry entry = sessions.get(sessionId);
            entry.touch();

            // Delegate to conversation agent
            ConversationResponse response = agent.processMessage(message, entry.state);

            // Update stored state
            sessions.put(sessionId, new SessionEntry(response.getUpdatedState()));
            // Re-touch after update (SessionEntry is immutable above, recreated)
            sessions.get(sessionId).touch();

            // Build response JSON
            ObjectNode respJson = mapper.createObjectNode();
            respJson.put("sessionId",  sessionId);
            respJson.put("response",   response.getMessage());
            respJson.put("isComplete", response.isComplete());

            // Remove session if complete (optional — reaper handles stragglers)
            if (response.isComplete()) {
                sessions.remove(sessionId);
            }

            sendJson(exchange, 200, mapper.writeValueAsString(respJson));

        } catch (Exception e) {
            logger.error("Error handling /api/quote/message", e);
            sendError(exchange, 500, "Internal server error: " + e.getMessage());
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method Not Allowed");
            return;
        }
        ObjectNode body = mapper.createObjectNode();
        body.put("status",          "ok");
        body.put("activeSessions",  sessions.size());
        body.put("timestamp",       Instant.now().toString());
        sendJson(exchange, 200, mapper.writeValueAsString(body));
    }

    // ── Session management ─────────────────────────────────────────────

    private void reapExpiredSessions() {
        long now = System.currentTimeMillis();
        int before = sessions.size();
        Iterator<Map.Entry<String, SessionEntry>> it = sessions.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SessionEntry> e = it.next();
            if (now - e.getValue().lastAccessMs > sessionTtlMs) {
                it.remove();
            }
        }
        int removed = before - sessions.size();
        if (removed > 0) {
            logger.info("Session reaper removed {} expired session(s); {} active.",
                    removed, sessions.size());
        }
    }

    // ── Response helpers ──────────────────────────────────────────────

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        body.put("error", message);
        sendJson(exchange, status, mapper.writeValueAsString(body));
    }

    // ── Entry point ───────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        QuoteAgentServer srv = new QuoteAgentServer();
        srv.start();

        // Graceful shutdown on Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(srv::stop, "shutdown-hook"));

        // Keep main thread alive
        Thread.currentThread().join();
    }
}
