package com.integrateai.agent;

import com.fasterxml.jackson.databind.JsonNode;

public interface Tool {
    String getName();
    String getDescription();
    JsonNode getParametersSchema();
    boolean requiresConfirmation();

    ToolCallResult execute(JsonNode args, String connectionId);
}
