package com.integrateai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrateai.dto.AgentChatResponse;
import com.integrateai.nango.NangoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    private ToolRegistry toolRegistry;
    private NangoService nangoService;
    private WebClient webClient;
    private ObjectMapper objectMapper;
    private AgentService agentService;

    @BeforeEach
    void setUp() {
        toolRegistry = Mockito.mock(ToolRegistry.class);
        nangoService = Mockito.mock(NangoService.class);
        webClient = Mockito.mock(WebClient.class);
        objectMapper = new ObjectMapper();

        agentService = new AgentService(toolRegistry, nangoService, webClient, objectMapper);
    }

    @Test
    void processUserPrompt_ShouldReturnNotConnected_WhenNangoConnectionInactive() {
        when(nangoService.isConnectionActive(anyString())).thenReturn(false);

        AgentChatResponse response = agentService.processUserPrompt("List my repositories", "demo-user-1");
        assertNotNull(response);
        assertFalse(response.githubConnected());
        assertTrue(response.message().contains("GitHub account is not connected"));
    }

    @Test
    void processUserPrompt_ShouldInterceptCreateIssue_WhenNotConfirmed() {
        when(nangoService.isConnectionActive(anyString())).thenReturn(true);

        Tool createIssueTool = Mockito.mock(Tool.class);
        when(createIssueTool.getName()).thenReturn("createIssue");
        when(toolRegistry.executeTool(Mockito.eq("createIssue"), Mockito.any(), Mockito.anyString()))
                .thenReturn(ToolCallResult.needsConfirmation("createIssue", java.util.Map.of(
                        "tool", "createIssue",
                        "owner", "example-org",
                        "repo", "integrateai",
                        "title", "Fix bug"
                )));

        AgentChatResponse response = agentService.processUserPrompt("Create an issue in example-org/integrateai with title Fix bug", "demo-user-1");
        assertNotNull(response);
        assertTrue(response.requiresConfirmation());
        assertNotNull(response.pendingAction());
        assertEquals("createIssue", response.pendingAction().get("tool"));
    }
}
