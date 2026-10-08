package com.integrateai.nango;

import com.integrateai.config.NangoProperties;
import com.integrateai.nango.dto.NangoConnectSessionDto;
import com.integrateai.nango.dto.NangoConnectionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NangoService {
    private static final Logger log = LoggerFactory.getLogger(NangoService.class);

    private final NangoClient nangoClient;
    private final NangoProperties nangoProperties;

    public NangoService(NangoClient nangoClient, NangoProperties nangoProperties) {
        this.nangoClient = nangoClient;
        this.nangoProperties = nangoProperties;
    }

    public boolean isConnectionActive(String connectionId) {
        try {
            String targetConnectionId = resolveConnectionId(connectionId);
            NangoConnectionDto conn = nangoClient.getConnection(targetConnectionId, nangoProperties.integrationId());
            return conn != null;
        } catch (Exception ex) {
            log.debug("[NANGO_STATUS] Connection inactive or check error: {}", ex.getMessage());
            return false;
        }
    }

    public NangoConnectionDto getConnection(String connectionId) {
        try {
            String targetConnectionId = resolveConnectionId(connectionId);
            return nangoClient.getConnection(targetConnectionId, nangoProperties.integrationId());
        } catch (Exception ex) {
            log.debug("[NANGO_STATUS] Failed to get connection: {}", ex.getMessage());
            return null;
        }
    }

    public NangoConnectSessionDto createSession(String connectionId) {
        String targetConnectionId = resolveConnectionId(connectionId);
        return nangoClient.createConnectSession(targetConnectionId);
    }

    public String proxyGet(String path, String connectionId) {
        String targetConnectionId = resolveConnectionId(connectionId);
        return nangoClient.proxyGet(path, targetConnectionId);
    }

    public String proxyPost(String path, Object body, String connectionId) {
        String targetConnectionId = resolveConnectionId(connectionId);
        return nangoClient.proxyPost(path, body, targetConnectionId);
    }

    public String getDefaultConnectionId() {
        return nangoProperties.defaultConnectionId();
    }

    public String getIntegrationId() {
        return nangoProperties.integrationId();
    }

    private String resolveConnectionId(String connectionId) {
        return (connectionId != null && !connectionId.trim().isEmpty()) ? connectionId : nangoProperties.defaultConnectionId();
    }
}
