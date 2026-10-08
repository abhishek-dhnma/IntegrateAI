package com.integrateai.nango.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NangoConnectionDto(
        @JsonProperty("id") String id,
        @JsonProperty("connection_id") String connectionId,
        @JsonProperty("provider_config_key") String providerConfigKey,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("credentials") Map<String, Object> credentials,
        @JsonProperty("metadata") Map<String, Object> metadata
) {}
