package com.integrateai.nango;

import com.integrateai.config.NangoProperties;
import com.integrateai.nango.dto.NangoConnectSessionDto;
import com.integrateai.nango.dto.NangoConnectionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class NangoServiceTest {

    private NangoClient nangoClient;
    private NangoProperties nangoProperties;
    private NangoService nangoService;

    @BeforeEach
    void setUp() {
        nangoClient = Mockito.mock(NangoClient.class);
        nangoProperties = new NangoProperties("test-secret", "https://api.nango.dev", "github", "demo-user-1");
        nangoService = new NangoService(nangoClient, nangoProperties);
    }

    @Test
    void isConnectionActive_ShouldReturnTrue_WhenConnectionExists() {
        NangoConnectionDto dto = new NangoConnectionDto("1", "demo-user-1", "github", "2026-10-01", "2026-10-01", Map.of(), Map.of());
        when(nangoClient.getConnection("demo-user-1", "github")).thenReturn(dto);

        boolean active = nangoService.isConnectionActive("demo-user-1");
        assertTrue(active);
    }

    @Test
    void isConnectionActive_ShouldReturnFalse_WhenConnectionNull() {
        when(nangoClient.getConnection(anyString(), anyString())).thenReturn(null);

        boolean active = nangoService.isConnectionActive("unknown-user");
        assertFalse(active);
    }

    @Test
    void createSession_ShouldReturnSessionToken() {
        NangoConnectSessionDto sessionDto = new NangoConnectSessionDto("token-123", "2026-10-02", "https://connect.nango.dev");
        when(nangoClient.createConnectSession("demo-user-1")).thenReturn(sessionDto);

        NangoConnectSessionDto res = nangoService.createSession("demo-user-1");
        assertNotNull(res);
        assertEquals("token-123", res.token());
    }
}
