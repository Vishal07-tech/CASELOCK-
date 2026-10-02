package com.caselock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "caselock.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
