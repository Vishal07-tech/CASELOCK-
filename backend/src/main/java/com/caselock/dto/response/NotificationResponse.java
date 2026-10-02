package com.caselock.dto.response;

import com.caselock.entity.Notification;
import com.caselock.entity.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long relatedCaseId,
        Long relatedEvidenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getRelatedCaseId(),
                n.getRelatedEvidenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
