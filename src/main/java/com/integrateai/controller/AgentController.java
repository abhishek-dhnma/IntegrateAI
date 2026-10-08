package com.integrateai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integrateai.agent.AgentService;
import com.integrateai.dto.AgentChatRequest;
import com.integrateai.dto.AgentChatResponse;
import com.integrateai.dto.ConfirmationRequest;
import com.integrateai.nango.NangoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final NangoService nangoService;
    private final ObjectMapper objectMapper;

    public AgentController(AgentService agentService, NangoService nangoService, ObjectMapper objectMapper) {
        this.agentService = agentService;
        this.nangoService = nangoService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentChatResponse> chat(@Valid @RequestBody AgentChatRequest request) {
        String connectionId = (request.connectionId() != null && !request.connectionId().isBlank())
                ? request.connectionId()
                : nangoService.getDefaultConnectionId();

        log.info("[HTTP_POST] /api/agent/chat connectionId={} prompt='{}'", connectionId, request.prompt());
        AgentChatResponse response = agentService.processUserPrompt(request.prompt(), connectionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<AgentChatResponse> confirmAction(@Valid @RequestBody ConfirmationRequest request) {
        String connectionId = (request.connectionId() != null && !request.connectionId().isBlank())
                ? request.connectionId()
                : nangoService.getDefaultConnectionId();

        log.info("[HTTP_POST] /api/agent/confirm connectionId={} confirmed={}", connectionId, request.confirmed());

        if (!Boolean.TRUE.equals(request.confirmed())) {
            return ResponseEntity.ok(new AgentChatResponse(
                    "Action cancelled by user.",
                    null,
                    true,
                    null,
                    false
            ));
        }

        Map<String, Object> pendingAction = request.pendingAction();
        if (pendingAction == null || !pendingAction.containsKey("tool")) {
            return ResponseEntity.badRequest().body(new AgentChatResponse(
                    "Invalid pending action payload.",
                    null,
                    true,
                    null,
                    false
            ));
        }

        String toolName = (String) pendingAction.get("tool");
        ObjectNode argsNode = objectMapper.valueToTree(pendingAction);
        argsNode.put("confirmed", true); // Flag explicitly confirmed

        AgentChatResponse response = agentService.executeToolAndBuildResponse(toolName, argsNode, connectionId);
        return ResponseEntity.ok(response);
    }
}
