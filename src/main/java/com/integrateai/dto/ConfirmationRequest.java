package com.integrateai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ConfirmationRequest(
        @JsonProperty("connectionId") String connectionId,
        @NotNull(message = "Confirmation decision is required")
        @JsonProperty("confirmed") Boolean confirmed,
        @JsonProperty("pendingAction") Map<String, Object> pendingAction
) {}
