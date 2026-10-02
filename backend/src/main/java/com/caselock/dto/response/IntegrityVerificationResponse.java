package com.caselock.dto.response;

import com.caselock.entity.enums.IntegrityStatus;

import java.time.LocalDateTime;

public record IntegrityVerificationResponse(
        Long evidenceId,
        String evidenceNumber,
        String originalSha256,
        String currentSha256,
        IntegrityStatus integrityStatus,
        boolean match,
        LocalDateTime verifiedAt,
        String message
) {
}
