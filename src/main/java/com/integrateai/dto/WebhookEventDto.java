package com.integrateai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEventDto(
        @JsonProperty("type") String type,
        @JsonProperty("operation") String operation,
        @JsonProperty("connection_id") String connectionId,
        @JsonProperty("provider_config_key") String providerConfigKey,
        @JsonProperty("data") Map<String, Object> data,
        @JsonProperty("timestamp") String timestamp
) {
    public WebhookEventDto {
        if (timestamp == null) {
            timestamp = Instant.now().toString();
        }
    }
}
