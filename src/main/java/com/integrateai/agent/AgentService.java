package com.integrateai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integrateai.dto.AgentChatResponse;
import com.integrateai.nango.NangoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Service
public class AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ToolRegistry toolRegistry;
    private final NangoService nangoService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.mock-mode:false}")
    private boolean mockMode;

    @Value("${ai.openrouter.api-key:mock-key}")
    private String openrouterApiKey;

    @Value("${ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openrouterBaseUrl;

    @Value("${ai.openrouter.model:openrouter/free}")
    private String modelName;

    public AgentService(ToolRegistry toolRegistry, NangoService nangoService, WebClient webClient, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.nangoService = nangoService;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public AgentChatResponse processUserPrompt(String prompt, String connectionId) {
        log.info("[AGENT_REQUEST] Prompt: '{}', ConnectionId: '{}'", prompt, connectionId);

        // Verify Nango connection status
        boolean isConnected = nangoService.isConnectionActive(connectionId);
        if (!isConnected) {
            log.warn("[AGENT_WARN] GitHub connection not found or inactive for connectionId: {}", connectionId);
            return new AgentChatResponse(
                    "GitHub account is not connected. Please click 'Connect GitHub' to authenticate via Nango.",
                    null,
                    false,
                    null,
                    false
            );
        }

        if (mockMode || openrouterApiKey == null || "mock-key".equalsIgnoreCase(openrouterApiKey) || openrouterApiKey.isBlank()) {
            log.info("[AGENT_MODE] Using Mock LLM mode (mockMode={} apiKeyPresent={})", mockMode, openrouterApiKey != null && !"mock-key".equals(openrouterApiKey));
            return processWithMockLLM(prompt, connectionId);
        } else {
            log.info("[AGENT_MODE] Using OpenRouter API (model: {}, baseUrl: {})", modelName, openrouterBaseUrl);
            return processWithOpenRouter(prompt, connectionId);
        }
    }

    private AgentChatResponse processWithMockLLM(String prompt, String connectionId) {
        String lower = prompt.toLowerCase().trim();
        log.info("[AGENT_MOCK_LLM] Evaluated prompt: '{}'", lower);

        // Pattern matching for tool selection in mock mode
        if (lower.contains("list") && (lower.contains("repo") || lower.contains("repository") || lower.contains("repositories"))) {
            return executeToolAndBuildResponse("listRepositories", objectMapper.createObjectNode(), connectionId);
        } else if (lower.contains("issue") && (lower.contains("list") || lower.contains("show") || lower.contains("open"))) {
            ObjectNode args = objectMapper.createObjectNode();
            String owner = extractOwner(prompt);
            String repo = extractRepo(prompt);
            args.put("owner", owner);
            args.put("repo", repo);
            return executeToolAndBuildResponse("listOpenIssues", args, connectionId);
        } else if (lower.contains("issue") && (lower.contains("create") || lower.contains("new") || lower.contains("add"))) {
            ObjectNode args = objectMapper.createObjectNode();
            String owner = extractOwner(prompt);
            String repo = extractRepo(prompt);
            String title = extractTitle(prompt);
            args.put("owner", owner);
            args.put("repo", repo);
            args.put("title", title);
            args.put("body", "Created via IntegrateAI Agent on behalf of user prompt.");
            args.put("confirmed", lower.contains("confirm") || lower.contains("yes"));
            return executeToolAndBuildResponse("createIssue", args, connectionId);
        } else if (lower.contains("repo") || lower.contains("repository")) {
            ObjectNode args = objectMapper.createObjectNode();
            args.put("owner", extractOwner(prompt));
            args.put("repo", extractRepo(prompt));
            return executeToolAndBuildResponse("getRepository", args, connectionId);
        } else {
            return executeToolAndBuildResponse("listRepositories", objectMapper.createObjectNode(), connectionId);
        }
    }

    /**
     * Executes the tool-calling flow with OpenRouter:
     * User -> OpenRouter -> Tool call -> Spring Boot tool -> Nango -> GitHub -> Nango -> Spring Boot -> OpenRouter -> User
     */
    private AgentChatResponse processWithOpenRouter(String prompt, String connectionId) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", modelName);

            ArrayNode messages = payload.putArray("messages");
            messages.addObject()
                    .put("role", "system")
                    .put("content", "You are IntegrateAI assistant. You can manage GitHub repositories and issues using available tools. Call tools when needed to query or modify GitHub data.");
            messages.addObject()
                    .put("role", "user")
                    .put("content", prompt);

            // Add tools schema for OpenRouter function calling
            ArrayNode toolsArray = payload.putArray("tools");
            for (Tool tool : toolRegistry.getAllTools()) {
                ObjectNode toolObj = toolsArray.addObject();
                toolObj.put("type", "function");
                ObjectNode functionObj = toolObj.putObject("function");
                functionObj.put("name", tool.getName());
                functionObj.put("description", tool.getDescription());
                functionObj.set("parameters", tool.getParametersSchema());
            }

            String url = openrouterBaseUrl.replaceAll("/+$", "") + "/chat/completions";
            log.info("[OPENROUTER_REQ] Sending initial chat completion request to {}", url);

            String initialResponseJson = webClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openrouterApiKey)
                    .header("HTTP-Referer", "http://localhost:8080")
                    .header("X-Title", "IntegrateAI")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            JsonNode root = objectMapper.readTree(initialResponseJson);
            JsonNode message = root.path("choices").get(0).path("message");

            // Check if OpenRouter requested a tool call
            if (message.has("tool_calls") && message.path("tool_calls").isArray() && message.path("tool_calls").size() > 0) {
                JsonNode toolCall = message.path("tool_calls").get(0);
                String toolCallId = toolCall.path("id").asText("call_1");
                String functionName = toolCall.path("function").path("name").asText();
                String argsString = toolCall.path("function").path("arguments").asText("{}");
                JsonNode args = objectMapper.readTree(argsString);

                log.info("[OPENROUTER_TOOL_CALL] Model requested tool '{}' with arguments: {}", functionName, argsString);

                // Execute tool via Spring Boot -> Nango -> GitHub
                ToolCallResult result = toolRegistry.executeTool(functionName, args, connectionId);

                // Safety intercept for write actions requiring confirmation
                if (result.requiresConfirmation()) {
                    Map<String, Object> pendingAction = result.pendingAction();
                    String owner = (String) pendingAction.getOrDefault("owner", "owner");
                    String repo = (String) pendingAction.getOrDefault("repo", "repo");
                    String title = (String) pendingAction.getOrDefault("title", "title");
                    String body = (String) pendingAction.getOrDefault("body", "");

                    String confirmationMessage = String.format(
                            "**Action Requires Confirmation**\n\nI am prepared to create the following GitHub issue:\n" +
                            "- **Repository**: `%s/%s`\n" +
                            "- **Title**: `%s`\n" +
                            "- **Description**: `%s`\n\n" +
                            "Do you want me to proceed with creating this issue?",
                            owner, repo, title, body
                    );

                    return new AgentChatResponse(
                            confirmationMessage,
                            result,
                            true,
                            pendingAction,
                            true
                    );
                }

                if (!result.success()) {
                    return new AgentChatResponse(
                            "**Tool Execution Failed**: " + result.error(),
                            result,
                            true,
                            null,
                            false
                    );
                }

                // Second turn: Return tool result to OpenRouter to generate the final natural language response
                try {
                    log.info("[OPENROUTER_SECOND_TURN] Returning tool result to OpenRouter for final response generation");
                    ObjectNode followUpPayload = objectMapper.createObjectNode();
                    followUpPayload.put("model", modelName);

                    ArrayNode followUpMessages = followUpPayload.putArray("messages");
                    // Copy initial system & user messages
                    followUpMessages.add(messages.get(0));
                    followUpMessages.add(messages.get(1));

                    // Append assistant message containing the tool_calls
                    followUpMessages.add(message);

                    // Append tool result message
                    ObjectNode toolMessage = followUpMessages.addObject();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCallId);
                    toolMessage.put("name", functionName);
                    toolMessage.put("content", objectMapper.writeValueAsString(result.data()));

                    String secondResponseJson = webClient.post()
                            .uri(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + openrouterApiKey)
                            .header("HTTP-Referer", "http://localhost:8080")
                            .header("X-Title", "IntegrateAI")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(followUpPayload)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(30));

                    JsonNode secondRoot = objectMapper.readTree(secondResponseJson);
                    String finalContent = secondRoot.path("choices").get(0).path("message").path("content").asText();

                    if (finalContent != null && !finalContent.isBlank()) {
                        log.info("[OPENROUTER_RES] Final response generated successfully by OpenRouter");
                        return new AgentChatResponse(finalContent, result, true, null, false);
                    }
                } catch (Exception followUpEx) {
                    log.warn("[OPENROUTER_SECOND_TURN_WARN] Second turn failed ({}), falling back to direct tool output formatter", followUpEx.getMessage());
                }

                // Graceful fallback to formatted tool output if second turn fails
                String humanReadable = formatToolOutput(functionName, result.data());
                return new AgentChatResponse(humanReadable, result, true, null, false);
            } else {
                String content = message.path("content").asText("I received your prompt.");
                return new AgentChatResponse(content, null, true, null, false);
            }
        } catch (Exception ex) {
            log.error("[OPENROUTER_ERROR] OpenRouter request failed, falling back to Mock LLM: {}", ex.getMessage());
            return processWithMockLLM(prompt, connectionId);
        }
    }

    public AgentChatResponse executeToolAndBuildResponse(String toolName, JsonNode args, String connectionId) {
        log.info("[TOOL_SELECTED] Tool: '{}', Arguments: '{}'", toolName, args);
        ToolCallResult result = toolRegistry.executeTool(toolName, args, connectionId);

        if (result.requiresConfirmation()) {
            Map<String, Object> pendingAction = result.pendingAction();
            String owner = (String) pendingAction.getOrDefault("owner", "owner");
            String repo = (String) pendingAction.getOrDefault("repo", "repo");
            String title = (String) pendingAction.getOrDefault("title", "title");
            String body = (String) pendingAction.getOrDefault("body", "");

            String confirmationMessage = String.format(
                    "**Action Requires Confirmation**\n\nI am prepared to create the following GitHub issue:\n" +
                    "- **Repository**: `%s/%s`\n" +
                    "- **Title**: `%s`\n" +
                    "- **Description**: `%s`\n\n" +
                    "Do you want me to proceed with creating this issue?",
                    owner, repo, title, body
            );

            return new AgentChatResponse(
                    confirmationMessage,
                    result,
                    true,
                    pendingAction,
                    true
            );
        }

        if (!result.success()) {
            return new AgentChatResponse(
                    "**Tool Execution Failed**: " + result.error(),
                    result,
                    true,
                    null,
                    false
            );
        }

        String humanReadable = formatToolOutput(toolName, result.data());
        return new AgentChatResponse(humanReadable, result, true, null, false);
    }

    private String formatToolOutput(String toolName, Object data) {
        StringBuilder sb = new StringBuilder();
        try {
            if ("listRepositories".equals(toolName)) {
                List<?> list = objectMapper.convertValue(data, List.class);
                sb.append("Here are your GitHub repositories:\n\n");
                for (int i = 0; i < Math.min(list.size(), 10); i++) {
                    Map<?, ?> repo = objectMapper.convertValue(list.get(i), Map.class);
                    sb.append(String.format("%d. **[%s](%s)** — %s (%s stars, %s open issues)\n",
                            i + 1,
                            repo.get("full_name"),
                            repo.get("html_url"),
                            repo.get("description") != null ? repo.get("description") : "No description",
                            repo.get("stargazers_count"),
                            repo.get("open_issues_count")));
                }
            } else if ("listOpenIssues".equals(toolName)) {
                List<?> list = objectMapper.convertValue(data, List.class);
                if (list.isEmpty()) {
                    sb.append("No open issues found in this repository.");
                } else {
                    sb.append("Here are the open issues:\n\n");
                    for (int i = 0; i < Math.min(list.size(), 10); i++) {
                        Map<?, ?> issue = objectMapper.convertValue(list.get(i), Map.class);
                        sb.append(String.format("%d. **#%s: [%s](%s)** (State: `%s`)\n",
                                i + 1,
                                issue.get("number"),
                                issue.get("title"),
                                issue.get("html_url"),
                                issue.get("state")));
                    }
                }
            } else if ("createIssue".equals(toolName)) {
                Map<?, ?> issue = objectMapper.convertValue(data, Map.class);
                sb.append(String.format("**GitHub Issue Created Successfully!**\n\n" +
                        "- **Title**: %s\n" +
                        "- **Issue Number**: #%s\n" +
                        "- **URL**: [%s](%s)\n",
                        issue.get("title"),
                        issue.get("number"),
                        issue.get("html_url"),
                        issue.get("html_url")));
            } else {
                sb.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
            }
        } catch (Exception e) {
            sb.append("Execution complete. Output: ").append(data.toString());
        }
        return sb.toString();
    }

    private String extractOwner(String prompt) {
        if (prompt.contains("/")) {
            String[] parts = prompt.split("/");
            String before = parts[0].trim();
            String[] words = before.split("\\s+");
            return words[words.length - 1];
        }
        return "example-org";
    }

    private String extractRepo(String prompt) {
        if (prompt.contains("/")) {
            String[] parts = prompt.split("/");
            String after = parts[1].trim();
            return after.split("\\s+")[0];
        }
        return "example-repo";
    }

    private String extractTitle(String prompt) {
        if (prompt.contains("title")) {
            int idx = prompt.indexOf("title");
            return prompt.substring(idx + 5).trim();
        }
        return "Fix bug reported by IntegrateAI user";
    }
}
