package com.caselock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caselock.security")
public record SecurityProperties(
        int maxFailedLoginAttempts,
        int accountLockoutMinutes
) {
}
