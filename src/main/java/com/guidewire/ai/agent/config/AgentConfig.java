package com.guidewire.ai.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Reads claude.* and server.* properties from application.properties.
 * Supports ANTHROPIC_API_KEY environment variable override.
 */
public class AgentConfig {

    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);
    private static final String PROPS_FILE = "application.properties";

    private static AgentConfig instance;

    private final String claudeApiKey;
    private final String claudeModel;
    private final int claudeMaxTokens;
    private final int serverPort;
    private final int sessionTimeoutMinutes;

    private AgentConfig() {
        Properties props = loadProperties();

        // Environment variable takes priority over property file
        String envKey = System.getenv("ANTHROPIC_API_KEY");
        this.claudeApiKey = (envKey != null && !envKey.isEmpty())
                ? envKey
                : props.getProperty("claude.api.key", "");

        this.claudeModel = props.getProperty("claude.model", "claude-haiku-4-5-20251001");
        this.claudeMaxTokens = intProp(props, "claude.max.tokens", 512);
        this.serverPort = intProp(props, "server.port", 8080);
        this.sessionTimeoutMinutes = intProp(props, "server.session.timeout.minutes", 30);

        if (claudeApiKey.isEmpty()) {
            logger.warn("No Claude API key configured. NL parsing disabled — all fields will be asked manually.");
        } else {
            logger.info("AgentConfig loaded: model={}, port={}", claudeModel, serverPort);
        }
    }

    public static synchronized AgentConfig getInstance() {
        if (instance == null) {
            instance = new AgentConfig();
        }
        return instance;
    }

    public String getClaudeApiKey()         { return claudeApiKey; }
    public boolean hasApiKey()              { return claudeApiKey != null && !claudeApiKey.isEmpty(); }
    public String getClaudeModel()          { return claudeModel; }
    public int getClaudeMaxTokens()         { return claudeMaxTokens; }
    public int getServerPort()              { return serverPort; }
    public int getSessionTimeoutMinutes()   { return sessionTimeoutMinutes; }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(PROPS_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            logger.warn("Could not load {}: {}", PROPS_FILE, e.getMessage());
        }
        return props;
    }

    private int intProp(Properties props, String key, int defaultVal) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultVal)));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
