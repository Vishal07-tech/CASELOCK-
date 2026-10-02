package com.caselock.dto.response;

import com.caselock.entity.LoginHistory;

import java.time.LocalDateTime;

public record LoginHistoryResponse(
        Long id,
        String username,
        boolean successful,
        String ipAddress,
        String userAgent,
        String failureReason,
        LocalDateTime attemptedAt
) {
    public static LoginHistoryResponse from(LoginHistory h) {
        return new LoginHistoryResponse(
                h.getId(),
                h.getUsername(),
                h.isSuccessful(),
                h.getIpAddress(),
                h.getUserAgent(),
                h.getFailureReason(),
                h.getAttemptedAt()
        );
    }
}
