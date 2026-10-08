package com.integrateai.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.integrateai.agent.Tool;
import com.integrateai.agent.ToolCallResult;
import com.integrateai.github.dto.GitHubIssue;
import com.integrateai.github.dto.GitHubRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitHubTool {
    private static final Logger log = LoggerFactory.getLogger(GitHubTool.class);

    private final GitHubService gitHubService;
    private final ObjectMapper objectMapper;

    public GitHubTool(GitHubService gitHubService, ObjectMapper objectMapper) {
        this.gitHubService = gitHubService;
        this.objectMapper = objectMapper;
    }

    @Component
    public static class ListRepositoriesTool implements Tool {
        private final GitHubService gitHubService;
        private final ObjectMapper objectMapper;

        public ListRepositoriesTool(GitHubService gitHubService, ObjectMapper objectMapper) {
            this.gitHubService = gitHubService;
            this.objectMapper = objectMapper;
        }

        @Override
        public String getName() { return "listRepositories"; }

        @Override
        public String getDescription() { return "List the authenticated user's GitHub repositories sorted by recently updated."; }

        @Override
        public JsonNode getParametersSchema() {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "object");
            root.set("properties", objectMapper.createObjectNode());
            return root;
        }

        @Override
        public boolean requiresConfirmation() { return false; }

        @Override
        public ToolCallResult execute(JsonNode args, String connectionId) {
            try {
                List<GitHubRepo> repos = gitHubService.listRepositories(connectionId);
                return ToolCallResult.ok(getName(), repos);
            } catch (Exception e) {
                return ToolCallResult.fail(getName(), e.getMessage());
            }
        }
    }

    @Component
    public static class ListOpenIssuesTool implements Tool {
        private final GitHubService gitHubService;
        private final ObjectMapper objectMapper;

        public ListOpenIssuesTool(GitHubService gitHubService, ObjectMapper objectMapper) {
            this.gitHubService = gitHubService;
            this.objectMapper = objectMapper;
        }

        @Override
        public String getName() { return "listOpenIssues"; }

        @Override
        public String getDescription() { return "List open issues for a specified GitHub repository."; }

        @Override
        public JsonNode getParametersSchema() {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "object");
            ObjectNode props = root.putObject("properties");
            props.putObject("owner").put("type", "string").put("description", "Repository owner/organization");
            props.putObject("repo").put("type", "string").put("description", "Repository name");
            root.putArray("required").add("owner").add("repo");
            return root;
        }

        @Override
        public boolean requiresConfirmation() { return false; }

        @Override
        public ToolCallResult execute(JsonNode args, String connectionId) {
            try {
                String owner = args.has("owner") ? args.get("owner").asText() : "";
                String repo = args.has("repo") ? args.get("repo").asText() : "";
                if (owner.isBlank() || repo.isBlank()) {
                    return ToolCallResult.fail(getName(), "Parameters 'owner' and 'repo' are required.");
                }
                List<GitHubIssue> issues = gitHubService.listOpenIssues(owner, repo, connectionId);
                return ToolCallResult.ok(getName(), issues);
            } catch (Exception e) {
                return ToolCallResult.fail(getName(), e.getMessage());
            }
        }
    }

    @Component
    public static class GetRepositoryTool implements Tool {
        private final GitHubService gitHubService;
        private final ObjectMapper objectMapper;

        public GetRepositoryTool(GitHubService gitHubService, ObjectMapper objectMapper) {
            this.gitHubService = gitHubService;
            this.objectMapper = objectMapper;
        }

        @Override
        public String getName() { return "getRepository"; }

        @Override
        public String getDescription() { return "Get detailed information about a specific GitHub repository."; }

        @Override
        public JsonNode getParametersSchema() {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "object");
            ObjectNode props = root.putObject("properties");
            props.putObject("owner").put("type", "string").put("description", "Repository owner/organization");
            props.putObject("repo").put("type", "string").put("description", "Repository name");
            root.putArray("required").add("owner").add("repo");
            return root;
        }

        @Override
        public boolean requiresConfirmation() { return false; }

        @Override
        public ToolCallResult execute(JsonNode args, String connectionId) {
            try {
                String owner = args.has("owner") ? args.get("owner").asText() : "";
                String repo = args.has("repo") ? args.get("repo").asText() : "";
                GitHubRepo repoData = gitHubService.getRepository(owner, repo, connectionId);
                return ToolCallResult.ok(getName(), repoData);
            } catch (Exception e) {
                return ToolCallResult.fail(getName(), e.getMessage());
            }
        }
    }

    @Component
    public static class CreateIssueTool implements Tool {
        private final GitHubService gitHubService;
        private final ObjectMapper objectMapper;

        public CreateIssueTool(GitHubService gitHubService, ObjectMapper objectMapper) {
            this.gitHubService = gitHubService;
            this.objectMapper = objectMapper;
        }

        @Override
        public String getName() { return "createIssue"; }

        @Override
        public String getDescription() { return "Create a new issue in a specified GitHub repository. Requires user confirmation."; }

        @Override
        public JsonNode getParametersSchema() {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("type", "object");
            ObjectNode props = root.putObject("properties");
            props.putObject("owner").put("type", "string").put("description", "Repository owner");
            props.putObject("repo").put("type", "string").put("description", "Repository name");
            props.putObject("title").put("type", "string").put("description", "Title of the issue");
            props.putObject("body").put("type", "string").put("description", "Description / body content of the issue");
            props.putObject("confirmed").put("type", "boolean").put("description", "Explicit user confirmation flag");
            root.putArray("required").add("owner").add("repo").add("title");
            return root;
        }

        @Override
        public boolean requiresConfirmation() { return true; }

        @Override
        public ToolCallResult execute(JsonNode args, String connectionId) {
            String owner = args.has("owner") ? args.get("owner").asText() : "";
            String repo = args.has("repo") ? args.get("repo").asText() : "";
            String title = args.has("title") ? args.get("title").asText() : "";
            String body = args.has("body") ? args.get("body").asText() : "";
            boolean confirmed = args.has("confirmed") && args.get("confirmed").asBoolean();

            if (!confirmed) {
                Map<String, Object> pendingAction = new HashMap<>();
                pendingAction.put("tool", getName());
                pendingAction.put("owner", owner);
                pendingAction.put("repo", repo);
                pendingAction.put("title", title);
                pendingAction.put("body", body);
                log.info("[SAFETY_INTERCEPT] Intercepted write operation createIssue for {}/{}. Requiring user confirmation.", owner, repo);
                return ToolCallResult.needsConfirmation(getName(), pendingAction);
            }

            try {
                GitHubIssue issue = gitHubService.createIssue(owner, repo, title, body, connectionId);
                return ToolCallResult.ok(getName(), issue);
            } catch (Exception e) {
                return ToolCallResult.fail(getName(), e.getMessage());
            }
        }
    }
}
