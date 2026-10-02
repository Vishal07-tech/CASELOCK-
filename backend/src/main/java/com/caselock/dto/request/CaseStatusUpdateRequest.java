package com.caselock.dto.request;

import com.caselock.entity.enums.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record CaseStatusUpdateRequest(
        @NotNull(message = "Status is required") CaseStatus status,
        String remarks
) {
}
