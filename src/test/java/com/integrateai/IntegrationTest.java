package com.integrateai;

import com.integrateai.nango.NangoService;
import com.integrateai.nango.dto.NangoConnectionDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NangoService nangoService;

    @Test
    void getStatus_ShouldReturnConnectedStatus() throws Exception {
        NangoConnectionDto conn = new NangoConnectionDto("1", "demo-user-1", "github", "2026-10-01", "2026-10-01", Map.of(), Map.of());
        when(nangoService.getConnection("demo-user-1")).thenReturn(conn);
        when(nangoService.getDefaultConnectionId()).thenReturn("demo-user-1");
        when(nangoService.getIntegrationId()).thenReturn("github");

        mockMvc.perform(get("/api/connection/status?connectionId=demo-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.connectionId").value("demo-user-1"))
                .andExpect(jsonPath("$.providerKey").value("github"));
    }

    @Test
    void handleWebhook_ShouldAcceptAndRegisterNangoEvent() throws Exception {
        String payload = """
                {
                    "type": "issues.opened",
                    "operation": "webhook",
                    "connection_id": "demo-user-1",
                    "provider_config_key": "github",
                    "data": { "issue": { "title": "New Bug" } }
                }
                """;

        mockMvc.perform(post("/api/webhooks/nango")
                        .header("X-Nango-Signature", "sha256=test-sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.type").value("issues.opened"));
    }
}
