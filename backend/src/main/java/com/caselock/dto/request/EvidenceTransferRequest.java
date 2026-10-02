package com.caselock.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvidenceTransferRequest(
        @NotNull(message = "The recipient user id is required") Long toUserId,
        @NotBlank(message = "A reason for the transfer is required") String reason,
        String location,
        String remarks
) {
}
