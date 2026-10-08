package com.integrateai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ToolRegistry {
    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> toolList) {
        this.tools = toolList.stream()
                .collect(Collectors.toMap(Tool::getName, Function.identity()));
    }

    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<Tool> getAllTools() {
        return tools.values();
    }

    public ToolCallResult executeTool(String toolName, JsonNode args, String connectionId) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            return ToolCallResult.fail(toolName, "Unknown tool: " + toolName);
        }
        return tool.execute(args, connectionId);
    }
}
