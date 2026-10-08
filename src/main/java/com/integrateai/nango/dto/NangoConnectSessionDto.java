package com.integrateai.nango.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NangoConnectSessionDto(
        @JsonProperty("token") String token,
        @JsonProperty("expires_at") String expiresAt,
        @JsonProperty("connect_url") String connectUrl
) {}
