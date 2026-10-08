package com.integrateai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.integrateai.agent.ToolCallResult;

import java.util.Map;

public record AgentChatResponse(
        @JsonProperty("message") String message,
        @JsonProperty("toolResult") ToolCallResult toolResult,
        @JsonProperty("githubConnected") boolean githubConnected,
        @JsonProperty("pendingAction") Map<String, Object> pendingAction,
        @JsonProperty("requiresConfirmation") boolean requiresConfirmation
) {}
