package com.integrateai.nango;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrateai.config.NangoProperties;
import com.integrateai.exception.NangoException;
import com.integrateai.nango.dto.NangoConnectSessionDto;
import com.integrateai.nango.dto.NangoConnectionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class NangoClient {
    private static final Logger log = LoggerFactory.getLogger(NangoClient.class);

    private final WebClient webClient;
    private final NangoProperties nangoProperties;
    private final ObjectMapper objectMapper;

    public NangoClient(WebClient webClient, NangoProperties nangoProperties, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.nangoProperties = nangoProperties;
        this.objectMapper = objectMapper;
    }

    private final java.util.Map<String, String> resolvedConnectionIds = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Checks if a connection exists for given connectionId (or user tag) and providerConfigKey.
     */
    public NangoConnectionDto getConnection(String connectionId, String providerConfigKey) {
        // 1. Try direct connection ID lookup
        NangoConnectionDto direct = fetchDirectConnection(connectionId, providerConfigKey);
        if (direct != null) {
            resolvedConnectionIds.put(connectionId, direct.connectionId());
            return direct;
        }

        // 2. Fallback: Lookup by end_user_id tag (since Nango generates UUID connection_ids)
        NangoConnectionDto byTag = fetchConnectionByTag(connectionId, providerConfigKey);
        if (byTag != null) {
            resolvedConnectionIds.put(connectionId, byTag.connectionId());
            return byTag;
        }

        return null;
    }

    private NangoConnectionDto fetchDirectConnection(String connectionId, String providerConfigKey) {
        String url = String.format("%s/connection/%s?provider_config_key=%s",
                nangoProperties.host(), connectionId, providerConfigKey);

        log.debug("[NANGO_REQ] GET direct {}", url);
        long startTime = System.currentTimeMillis();

        try {
            NangoConnectionDto connection = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + nangoProperties.secretKey())
                    .retrieve()
                    .bodyToMono(NangoConnectionDto.class)
                    .block(Duration.ofSeconds(10));

            log.info("[NANGO_RES] GET direct connectionStatus=SUCCESS connectionId={} duration={}ms",
                    connectionId, System.currentTimeMillis() - startTime);
            return connection;
        } catch (WebClientResponseException ex) {
            log.debug("[NANGO_RES] Direct connection lookup not found for connectionId={} status={}", connectionId, ex.getStatusCode().value());
            return null;
        } catch (Exception ex) {
            log.debug("[NANGO_RES] Direct connection lookup failed: {}", ex.getMessage());
            return null;
        }
    }

    private NangoConnectionDto fetchConnectionByTag(String endUserId, String providerConfigKey) {
        String url = String.format("%s/connection?tags[end_user_id]=%s",
                nangoProperties.host(), endUserId);

        log.debug("[NANGO_REQ] GET by tag {}", url);
        long startTime = System.currentTimeMillis();

        try {
            String json = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + nangoProperties.secretKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            if (root.has("connections") && root.get("connections").isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode conn : root.get("connections")) {
                    String provKey = conn.has("provider_config_key") ? conn.get("provider_config_key").asText() : "";
                    if (providerConfigKey == null || providerConfigKey.isBlank() || providerConfigKey.equalsIgnoreCase(provKey)) {
                        String connId = conn.has("connection_id") ? conn.get("connection_id").asText() : "";
                        String id = conn.has("id") ? conn.get("id").asText() : connId;
                        String createdAt = conn.has("created") ? conn.get("created").asText() : "";

                        log.info("[NANGO_RES] Tag lookup SUCCESS for end_user_id={} resolvedConnectionId={} duration={}ms",
                                endUserId, connId, System.currentTimeMillis() - startTime);
                        return new NangoConnectionDto(id, connId, provKey, createdAt, createdAt, Map.of(), Map.of());
                    }
                }
            }
            return null;
        } catch (Exception ex) {
            log.warn("[NANGO_RES] Tag lookup error for end_user_id={}: {}", endUserId, ex.getMessage());
            return null;
        }
    }

    /**
     * Creates a Nango connect session token for frontend authentication flow.
     */
    public NangoConnectSessionDto createConnectSession(String connectionId) {
        String url = nangoProperties.host() + "/connect/sessions";
        Map<String, Object> body = Map.of(
                "tags", Map.of("end_user_id", connectionId),
                "allowed_integrations", java.util.List.of(nangoProperties.integrationId())
        );

        log.debug("[NANGO_REQ] POST /connect/sessions for connectionId={}", connectionId);
        long startTime = System.currentTimeMillis();

        try {
            String json = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + nangoProperties.secretKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            com.fasterxml.jackson.databind.JsonNode target = root.has("data") ? root.get("data") : root;
            String token = target.has("token") ? target.get("token").asText() : "";
            String connectUrl = target.has("connect_link") ? target.get("connect_link").asText() :
                    (target.has("connect_url") ? target.get("connect_url").asText() : "");
            String expiresAt = target.has("expires_at") ? target.get("expires_at").asText() : "";
            NangoConnectSessionDto session = new NangoConnectSessionDto(token, expiresAt, connectUrl);

            log.info("[NANGO_RES] CreateConnectSession=SUCCESS duration={}ms", System.currentTimeMillis() - startTime);
            return session;
        } catch (WebClientResponseException ex) {
            log.error("[NANGO_RES] CreateConnectSession=FAILED statusCode={}", ex.getStatusCode().value());
            throw new NangoException("Failed to create Nango connect session: " + ex.getResponseBodyAsString(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("[NANGO_RES] CreateConnectSession=ERROR error={}", ex.getMessage());
            throw new NangoException("Nango connect session failure: " + ex.getMessage(), ex);
        }
    }

    /**
     * Proxies a GET request to a provider API (e.g. GitHub) through Nango Proxy.
     */
    public String proxyGet(String path, String connectionId) {
        return proxyRequest(HttpMethod.GET, path, null, connectionId);
    }

    /**
     * Proxies a POST request to a provider API (e.g. GitHub) through Nango Proxy.
     */
    public String proxyPost(String path, Object requestBody, String connectionId) {
        return proxyRequest(HttpMethod.POST, path, requestBody, connectionId);
    }

    private String resolveRealConnectionId(String connectionId) {
        if (connectionId == null || connectionId.isBlank()) {
            connectionId = nangoProperties.defaultConnectionId();
        }
        if (resolvedConnectionIds.containsKey(connectionId)) {
            return resolvedConnectionIds.get(connectionId);
        }
        NangoConnectionDto conn = getConnection(connectionId, nangoProperties.integrationId());
        if (conn != null && conn.connectionId() != null && !conn.connectionId().isBlank()) {
            return conn.connectionId();
        }
        return connectionId;
    }

    private String proxyRequest(HttpMethod method, String path, Object requestBody, String connectionId) {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String proxyUrl = String.format("%s/proxy/%s", nangoProperties.host(), cleanPath);
        String realConnectionId = resolveRealConnectionId(connectionId);

        log.info("[NANGO_PROXY_REQ] method={} endpoint=/proxy/{} connectionId={} realConnectionId={}",
                method, cleanPath, connectionId, realConnectionId);
        long startTime = System.currentTimeMillis();

        try {
            WebClient.RequestBodySpec spec = webClient.method(method)
                    .uri(proxyUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + nangoProperties.secretKey())
                    .header("Provider-Config-Key", nangoProperties.integrationId())
                    .header("Connection-Id", realConnectionId)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

            if (requestBody != null) {
                spec.contentType(MediaType.APPLICATION_JSON).bodyValue(requestBody);
            }

            String responseBody = spec.retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(15));

            log.info("[NANGO_PROXY_RES] method={} endpoint=/proxy/{} status=200 duration={}ms",
                    method, cleanPath, System.currentTimeMillis() - startTime);
            return responseBody;
        } catch (WebClientResponseException ex) {
            log.error("[NANGO_PROXY_RES] method={} endpoint=/proxy/{} status={} body={}",
                    method, cleanPath, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new NangoException("Nango Proxy Request Failed (" + ex.getStatusCode().value() + "): " + ex.getResponseBodyAsString(), ex.getStatusCode().value());
        } catch (Exception ex) {
            log.error("[NANGO_PROXY_RES] method={} endpoint=/proxy/{} error={}", method, cleanPath, ex.getMessage());
            throw new NangoException("Nango Proxy Error: " + ex.getMessage(), ex);
        }
    }
}
