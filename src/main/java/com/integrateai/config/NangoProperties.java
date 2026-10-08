package com.integrateai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nango")
public record NangoProperties(
        String secretKey,
        String host,
        String integrationId,
        String defaultConnectionId
) {}
