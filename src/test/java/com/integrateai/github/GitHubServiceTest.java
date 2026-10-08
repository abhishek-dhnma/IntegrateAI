package com.integrateai.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrateai.exception.GitHubIntegrationException;
import com.integrateai.github.dto.GitHubIssue;
import com.integrateai.github.dto.GitHubRepo;
import com.integrateai.nango.NangoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class GitHubServiceTest {

    private NangoService nangoService;
    private ObjectMapper objectMapper;
    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        nangoService = Mockito.mock(NangoService.class);
        objectMapper = new ObjectMapper();
        gitHubService = new GitHubService(nangoService, objectMapper);
    }

    @Test
    void listRepositories_ShouldReturnParsedRepos() {
        String mockJsonResponse = """
                [
                    {
                        "id": 101,
                        "name": "integrateai",
                        "full_name": "example-org/integrateai",
                        "description": "Spring Boot AI Agent with Nango",
                        "private": false,
                        "html_url": "https://github.com/example-org/integrateai",
                        "stargazers_count": 42,
                        "open_issues_count": 2,
                        "updated_at": "2026-10-01T12:00:00Z",
                        "language": "Java"
                    }
                ]
                """;

        when(nangoService.proxyGet(anyString(), anyString())).thenReturn(mockJsonResponse);

        List<GitHubRepo> repos = gitHubService.listRepositories("demo-user-1");
        assertNotNull(repos);
        assertEquals(1, repos.size());
        assertEquals("example-org/integrateai", repos.get(0).fullName());
        assertEquals(42, repos.get(0).starsCount());
    }

    @Test
    void createIssue_ShouldReturnCreatedIssue() {
        String mockJsonResponse = """
                {
                    "id": 501,
                    "number": 7,
                    "title": "Fix auth bug",
                    "body": "Detailed description",
                    "state": "open",
                    "html_url": "https://github.com/example-org/integrateai/issues/7",
                    "created_at": "2026-10-02T00:00:00Z"
                }
                """;

        when(nangoService.proxyPost(anyString(), any(), anyString())).thenReturn(mockJsonResponse);

        GitHubIssue issue = gitHubService.createIssue("example-org", "integrateai", "Fix auth bug", "Detailed description", "demo-user-1");
        assertNotNull(issue);
        assertEquals(7, issue.number());
        assertEquals("Fix auth bug", issue.title());
    }

    @Test
    void listRepositories_ShouldThrowGitHubIntegrationException_OnFailure() {
        when(nangoService.proxyGet(anyString(), anyString())).thenThrow(new RuntimeException("Nango Proxy Failed"));

        assertThrows(GitHubIntegrationException.class, () -> gitHubService.listRepositories("demo-user-1"));
    }
}
