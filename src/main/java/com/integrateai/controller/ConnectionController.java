package com.integrateai.controller;

import com.integrateai.dto.ConnectionStatusResponse;
import com.integrateai.nango.NangoService;
import com.integrateai.nango.dto.NangoConnectSessionDto;
import com.integrateai.nango.dto.NangoConnectionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/connection")
public class ConnectionController {
    private static final Logger log = LoggerFactory.getLogger(ConnectionController.class);

    private final NangoService nangoService;

    public ConnectionController(NangoService nangoService) {
        this.nangoService = nangoService;
    }

    @GetMapping("/status")
    public ResponseEntity<ConnectionStatusResponse> getStatus(
            @RequestParam(name = "connectionId", required = false) String connectionId) {
        String targetConnectionId = (connectionId != null && !connectionId.isBlank())
                ? connectionId
                : nangoService.getDefaultConnectionId();

        log.info("[HTTP_GET] /api/connection/status connectionId={}", targetConnectionId);
        NangoConnectionDto connection = nangoService.getConnection(targetConnectionId);
        boolean isConnected = connection != null;

        return ResponseEntity.ok(new ConnectionStatusResponse(
                isConnected,
                targetConnectionId,
                nangoService.getIntegrationId(),
                connection
        ));
    }

    @PostMapping("/session")
    public ResponseEntity<NangoConnectSessionDto> createConnectSession(
            @RequestBody(required = false) Map<String, String> request) {
        String connectionId = (request != null && request.containsKey("connectionId"))
                ? request.get("connectionId")
                : nangoService.getDefaultConnectionId();

        log.info("[HTTP_POST] /api/connection/session connectionId={}", connectionId);
        NangoConnectSessionDto session = nangoService.createSession(connectionId);
        return ResponseEntity.ok(session);
    }
}
