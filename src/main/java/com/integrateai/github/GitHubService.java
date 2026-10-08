package com.integrateai.github;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrateai.exception.GitHubIntegrationException;
import com.integrateai.github.dto.GitHubIssue;
import com.integrateai.github.dto.GitHubRepo;
import com.integrateai.nango.NangoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GitHubService {
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final NangoService nangoService;
    private final ObjectMapper objectMapper;

    public GitHubService(NangoService nangoService, ObjectMapper objectMapper) {
        this.nangoService = nangoService;
        this.objectMapper = objectMapper;
    }

    public List<GitHubRepo> listRepositories(String connectionId) {
        log.info("[GITHUB_SVC] Action: listRepositories, connectionId: {}", connectionId);
        try {
            String json = nangoService.proxyGet("user/repos?sort=updated&per_page=15", connectionId);
            return objectMapper.readValue(json, new TypeReference<List<GitHubRepo>>() {});
        } catch (Exception ex) {
            log.error("[GITHUB_SVC_ERR] Failed to list repositories: {}", ex.getMessage());
            throw new GitHubIntegrationException("Failed to fetch user repositories from GitHub via Nango: " + ex.getMessage(), 500);
        }
    }

    public List<GitHubIssue> listOpenIssues(String owner, String repo, String connectionId) {
        log.info("[GITHUB_SVC] Action: listOpenIssues, owner: {}, repo: {}, connectionId: {}", owner, repo, connectionId);
        try {
            String endpoint = String.format("repos/%s/%s/issues?state=open&per_page=15", owner, repo);
            String json = nangoService.proxyGet(endpoint, connectionId);
            return objectMapper.readValue(json, new TypeReference<List<GitHubIssue>>() {});
        } catch (Exception ex) {
            log.error("[GITHUB_SVC_ERR] Failed to list issues for {}/{}: {}", owner, repo, ex.getMessage());
            throw new GitHubIntegrationException("Failed to fetch issues for repository " + owner + "/" + repo + ": " + ex.getMessage(), 500);
        }
    }

    public GitHubRepo getRepository(String owner, String repo, String connectionId) {
        log.info("[GITHUB_SVC] Action: getRepository, owner: {}, repo: {}, connectionId: {}", owner, repo, connectionId);
        try {
            String endpoint = String.format("repos/%s/%s", owner, repo);
            String json = nangoService.proxyGet(endpoint, connectionId);
            return objectMapper.readValue(json, GitHubRepo.class);
        } catch (Exception ex) {
            log.error("[GITHUB_SVC_ERR] Failed to get repo {}/{}: {}", owner, repo, ex.getMessage());
            throw new GitHubIntegrationException("Failed to get repository details for " + owner + "/" + repo + ": " + ex.getMessage(), 500);
        }
    }

    public GitHubIssue createIssue(String owner, String repo, String title, String body, String connectionId) {
        log.info("[GITHUB_SVC] Action: createIssue, owner: {}, repo: {}, title: {}, connectionId: {}", owner, repo, title, connectionId);
        try {
            String endpoint = String.format("repos/%s/%s/issues", owner, repo);
            Map<String, String> requestBody = Map.of(
                    "title", title,
                    "body", body != null ? body : ""
            );
            String json = nangoService.proxyPost(endpoint, requestBody, connectionId);
            return objectMapper.readValue(json, GitHubIssue.class);
        } catch (Exception ex) {
            log.error("[GITHUB_SVC_ERR] Failed to create issue in {}/{}: {}", owner, repo, ex.getMessage());
            throw new GitHubIntegrationException("Failed to create issue in repository " + owner + "/" + repo + ": " + ex.getMessage(), 500);
        }
    }
}
