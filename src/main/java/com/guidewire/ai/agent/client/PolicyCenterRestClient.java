package com.guidewire.ai.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guidewire.ai.agent.config.PCConfig;
import com.guidewire.ai.agent.model.*;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * REST client for Guidewire PolicyCenter 10.
 *
 * Authentication  : HTTP Basic Auth (pre-emptive) – credentials loaded from
 *                   src/main/resources/application.properties
 * Connection pool : Pooling connection manager for parallel quote processing
 * Timeouts        : Connect / socket / connection-request all configurable
 */
public class PolicyCenterRestClient {

    private static final Logger logger = LoggerFactory.getLogger(PolicyCenterRestClient.class);

    private final PCConfig                        config;
    private final CloseableHttpClient             httpClient;
    private final HttpClientContext               authContext;
    private final ObjectMapper                    objectMapper;

    // ── Construction ──────────────────────────────────────────────

    public PolicyCenterRestClient() {
        this.config = PCConfig.getInstance();

        // 1. Credentials provider
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(config.getUsername(), config.getPassword())
        );

        // 2. Pre-emptive Basic Auth cache
        //    This tells Apache HttpClient to send the Authorization header on the
        //    very FIRST request instead of waiting for a 401 challenge – required
        //    by PolicyCenter's REST API.
        URI uri = URI.create(config.getBaseUrl());
        HttpHost targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());

        this.authContext = HttpClientContext.create();
        this.authContext.setCredentialsProvider(credentialsProvider);
        this.authContext.setAuthCache(authCache);

        // 3. Connection pool
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(config.getMaxConnections());
        cm.setDefaultMaxPerRoute(config.getMaxConnectionsPerRoute());

        // 4. Request timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getConnectTimeoutMs())
                .setSocketTimeout(config.getSocketTimeoutMs())
                .setConnectionRequestTimeout(config.getConnectionRequestTimeoutMs())
                .build();

        // 5. Build client
        this.httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 6. JSON mapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.configure(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false   // tolerate extra fields returned by PC that are not in our model
        );
        this.objectMapper.setSerializationInclusion(
                com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
        );

        logger.info("PolicyCenterRestClient initialised – {}, user={}",
                config.getBaseUrl(), config.getUsername());
    }

    // ── Public API ────────────────────────────────────────────────

    /** POST /createaccount/v1/account */
    public CreateAccountResponse createAccount(CreateAccountRequest request) throws IOException {
        logger.info("Creating account for: {} {}", request.getAccountholder().getFirstname(), request.getAccountholder().getLastname());
        return post(config.getBaseUrl() + "/createaccount/v1/account",
                request, CreateAccountResponse.class);
    }

    /** POST /submission/v1/new-submission */
    public SubmissionResponse createSubmission(SubmissionRequest request) throws IOException {
        logger.info("Creating submission for account: {}", request.getAccountNumber());
        return post(config.getBaseUrl() + "/submission/v1/new-submission",
                request, SubmissionResponse.class);
    }

    /** POST /submission/v1/draft-submission */
    public SubmissionResponse draftSubmission(SubmissionQuoteRequest request) throws IOException {
        logger.info("Drafting submission: JobNumber={}", request.getJobNumber());
        return post(config.getBaseUrl() + "/submission/v1/draft-submission",
                request, SubmissionResponse.class);
    }

    /** POST /submission/v1/quote-submission */
    public SubmissionQuoteResponse quoteSubmission(SubmissionQuoteRequest request) throws IOException {
        logger.info("Quoting submission: JobNumber={}", request.getJobNumber());
        return post(config.getBaseUrl() + "/submission/v1/quote-submission",
                request, SubmissionQuoteResponse.class);
    }

    /** POST /submission/v1/issue-policy */
    public PolicyIssuanceResponse issuePolicy(PolicyIssuanceRequest request) throws IOException {
        logger.info("Issuing policy for JobNumber: {}", request.getJobNumber());
        return post(config.getBaseUrl() + "/submission/v1/issue-policy",
                request, PolicyIssuanceResponse.class);
    }

    /**
     * Lightweight connectivity check – calls GET on the base URL.
     * Returns true if PolicyCenter responds with any 2xx or 4xx (auth/not-found
     * still means the server is up); false if the server is unreachable.
     */
    public boolean testConnection() {
        String url = config.getBaseUrl();
        logger.info("Testing connection to: {}", url);
        try {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/json");
            try (CloseableHttpResponse response = httpClient.execute(get, authContext)) {
                int status = response.getStatusLine().getStatusCode();
                boolean reachable = status > 0;
                logger.info("Connection test – HTTP {}: {}", status,
                        reachable ? "PolicyCenter reachable" : "unexpected");
                return reachable;
            }
        } catch (Exception e) {
            logger.error("Connection test FAILED – {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public void close() throws IOException {
        if (httpClient != null) httpClient.close();
    }

    // ── Internal HTTP helper ──────────────────────────────────────

    private <T> T post(String url, Object requestBody, Class<T> responseType) throws IOException {
        String json = objectMapper.writeValueAsString(requestBody);
        logger.debug("POST {} → {}", url, json);

        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");
        httpPost.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = httpClient.execute(httpPost, authContext)) {
            int    status = response.getStatusLine().getStatusCode();
            String body   = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            logger.debug("← HTTP {} : {}", status, body);

            if (status == 401) {
                throw new IOException(
                    "HTTP 401 Unauthorized – check pc.auth.username / pc.auth.password " +
                    "in application.properties (current user: " + config.getUsername() + ")");
            }
            if (status == 403) {
                throw new IOException(
                    "HTTP 403 Forbidden – user '" + config.getUsername() +
                    "' does not have permission to call this endpoint");
            }
            if (status >= 200 && status < 300) {
                return objectMapper.readValue(body, responseType);
            }

            // Everything else – surface the full body so it is easy to debug
            logger.error("API Error [{}] {}: {}", status, url, body);
            throw new IOException("API call failed – HTTP " + status + " at " + url +
                                  "\nResponse body: " + body);
        }
    }
}
