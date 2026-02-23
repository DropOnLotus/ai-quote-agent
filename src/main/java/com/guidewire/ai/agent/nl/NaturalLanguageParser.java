package com.guidewire.ai.agent.nl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidewire.ai.agent.config.AgentConfig;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Extracts structured policy fields from a free-form user message by calling
 * the Claude API.  Falls back to an empty {@link ExtractedPolicyRequest} when
 * no API key is configured or the API returns an error.
 */
public class NaturalLanguageParser implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NaturalLanguageParser.class);

    private static final String CLAUDE_ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private static final String SYSTEM_PROMPT =
        "You are a JSON extractor for insurance policy requests. " +
        "Extract vehicle and policyholder information from the user's message. " +
        "Return ONLY a valid JSON object — no explanation, no markdown — with exactly these field names " +
        "(use JSON null for any value not present in the message):\n" +
        "addressLine1 (street address), city, state (2-letter US code, e.g. \"CT\"), " +
        "postalCode (5-digit string), vehicleYear (integer), vehicleMake, vehicleModel, " +
        "licensePlate, licenseState (2-letter code), firstName, lastName, " +
        "dateOfBirth (MM/DD/YYYY), licenseNumber, email.";

    private final AgentConfig config;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;

    public NaturalLanguageParser() {
        this.config = AgentConfig.getInstance();
        this.mapper = new ObjectMapper();

        RequestConfig reqCfg = RequestConfig.custom()
                .setConnectTimeout(10_000)
                .setSocketTimeout(15_000)
                .build();

        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(reqCfg)
                .build();
    }

    /**
     * Parses {@code userMessage} and returns an {@link ExtractedPolicyRequest}
     * with as many fields populated as Claude can extract.  On any error an
     * empty (all-null) POJO is returned so the conversation agent falls back
     * to asking each field individually.
     */
    public ExtractedPolicyRequest parse(String userMessage) {
        if (!config.hasApiKey()) {
            logger.debug("No API key — skipping NL parse, returning empty request.");
            return new ExtractedPolicyRequest();
        }

        try {
            return callClaudeApi(userMessage);
        } catch (Exception e) {
            logger.error("NL parse failed, falling back to manual collection: {}", e.getMessage());
            return new ExtractedPolicyRequest();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────

    private ExtractedPolicyRequest callClaudeApi(String userMessage) throws IOException {
        // Build request body
        ObjectNode body = mapper.createObjectNode();
        body.put("model", config.getClaudeModel());
        body.put("max_tokens", config.getClaudeMaxTokens());
        body.put("system", SYSTEM_PROMPT);

        ArrayNode messages = body.putArray("messages");
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        String requestJson = mapper.writeValueAsString(body);

        HttpPost post = new HttpPost(CLAUDE_ENDPOINT);
        post.setHeader("x-api-key", config.getClaudeApiKey());
        post.setHeader("anthropic-version", ANTHROPIC_VERSION);
        post.setHeader("Content-Type", "application/json");
        post.setEntity(new StringEntity(requestJson, StandardCharsets.UTF_8));

        try (CloseableHttpResponse resp = httpClient.execute(post)) {
            int status = resp.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);

            if (status < 200 || status >= 300) {
                logger.error("Claude API returned HTTP {}: {}", status, responseBody);
                return new ExtractedPolicyRequest();
            }

            // Response shape: { "content": [{"type":"text","text":"..."}], ... }
            JsonNode root = mapper.readTree(responseBody);
            String text = root.path("content").path(0).path("text").asText("{}");

            logger.debug("Claude raw response: {}", text);

            // Strip any accidental markdown fences
            text = text.trim();
            if (text.startsWith("```")) {
                text = text.replaceAll("```[a-z]*\\n?", "").replaceAll("```", "").trim();
            }

            return mapper.readValue(text, ExtractedPolicyRequest.class);
        }
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }
}
