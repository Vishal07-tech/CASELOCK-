package com.caselock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "caselock.file-storage")
public record FileStorageProperties(
        String path,
        List<String> allowedExtensions,
        long maxSizeMb
) {
}
