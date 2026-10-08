package com.integrateai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(
        @NotBlank(message = "Prompt is required")
        @JsonProperty("prompt") String prompt,

        @JsonProperty("connectionId") String connectionId
) {}
