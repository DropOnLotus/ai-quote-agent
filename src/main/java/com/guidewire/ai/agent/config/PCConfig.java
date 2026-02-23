package com.guidewire.ai.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads PolicyCenter connection settings from application.properties.
 * Credentials are never hardcoded – change the values in
 * src/main/resources/application.properties to match your environment.
 */
public class PCConfig {

    private static final Logger logger = LoggerFactory.getLogger(PCConfig.class);
    private static final String PROPS_FILE = "application.properties";

    // Singleton
    private static PCConfig instance;

    private final String baseUrl;
    private final String username;
    private final String password;
    private final int connectTimeoutMs;
    private final int socketTimeoutMs;
    private final int connectionRequestTimeoutMs;
    private final int maxConnections;
    private final int maxConnectionsPerRoute;

    // ── Constructor ───────────────────────────────────────────

    private PCConfig() {
        Properties props = loadProperties();

        this.baseUrl                    = props.getProperty("pc.base.url",
                                            "http://localhost:8180/pc/rest");
        this.username                   = props.getProperty("pc.auth.username", "su");
        this.password                   = props.getProperty("pc.auth.password", "gw");
        this.connectTimeoutMs           = intProp(props, "pc.http.connect.timeout.ms", 10000);
        this.socketTimeoutMs            = intProp(props, "pc.http.socket.timeout.ms", 30000);
        this.connectionRequestTimeoutMs = intProp(props, "pc.http.connection.request.timeout.ms", 5000);
        this.maxConnections             = intProp(props, "pc.http.max.connections", 20);
        this.maxConnectionsPerRoute     = intProp(props, "pc.http.max.connections.per.route", 10);

        logger.info("PCConfig loaded – baseUrl={}, username={}", baseUrl, username);
    }

    public static synchronized PCConfig getInstance() {
        if (instance == null) instance = new PCConfig();
        return instance;
    }

    // ── Accessors ─────────────────────────────────────────────

    public String getBaseUrl()                    { return baseUrl; }
    public String getUsername()                   { return username; }
    public String getPassword()                   { return password; }
    public int    getConnectTimeoutMs()           { return connectTimeoutMs; }
    public int    getSocketTimeoutMs()            { return socketTimeoutMs; }
    public int    getConnectionRequestTimeoutMs() { return connectionRequestTimeoutMs; }
    public int    getMaxConnections()             { return maxConnections; }
    public int    getMaxConnectionsPerRoute()     { return maxConnectionsPerRoute; }

    // ── Helpers ───────────────────────────────────────────────

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(PROPS_FILE)) {
            if (is != null) {
                props.load(is);
                logger.debug("Loaded {}", PROPS_FILE);
            } else {
                logger.warn("{} not found on classpath – using built-in defaults", PROPS_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to load {}: {}", PROPS_FILE, e.getMessage());
        }
        return props;
    }

    private int intProp(Properties props, String key, int defaultVal) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer for '{}', using default {}", key, defaultVal);
            return defaultVal;
        }
    }
}
