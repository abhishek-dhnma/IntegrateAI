package com.integrateai.agent;

import java.util.Map;

public record ToolCallResult(
        String toolName,
        boolean success,
        Object data,
        String error,
        boolean requiresConfirmation,
        Map<String, Object> pendingAction
) {
    public static ToolCallResult ok(String toolName, Object data) {
        return new ToolCallResult(toolName, true, data, null, false, null);
    }

    public static ToolCallResult fail(String toolName, String error) {
        return new ToolCallResult(toolName, false, null, error, false, null);
    }

    public static ToolCallResult needsConfirmation(String toolName, Map<String, Object> pendingAction) {
        return new ToolCallResult(toolName, false, null, "Confirmation required for write operation", true, pendingAction);
    }
}
