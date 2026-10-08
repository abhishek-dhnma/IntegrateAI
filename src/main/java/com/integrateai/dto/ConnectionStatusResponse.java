package com.integrateai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.integrateai.nango.dto.NangoConnectionDto;

public record ConnectionStatusResponse(
        @JsonProperty("connected") boolean connected,
        @JsonProperty("connectionId") String connectionId,
        @JsonProperty("providerKey") String providerKey,
        @JsonProperty("connectionDetails") NangoConnectionDto connectionDetails
) {}
