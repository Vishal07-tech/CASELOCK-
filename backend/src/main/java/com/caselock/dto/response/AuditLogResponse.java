package com.caselock.dto.response;

import com.caselock.entity.AuditLog;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String username,
        AuditAction action,
        String entityType,
        Long entityId,
        String description,
        String ipAddress,
        AuditResult result,
        LocalDateTime eventTimestamp
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUser() != null ? log.getUser().getUsername() : "SYSTEM",
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getIpAddress(),
                log.getResult(),
                log.getEventTimestamp()
        );
    }
}
