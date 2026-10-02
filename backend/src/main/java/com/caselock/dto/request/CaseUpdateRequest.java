package com.caselock.dto.request;

import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.CaseType;

import java.time.LocalDate;
import java.util.Set;

public record CaseUpdateRequest(
        String title,
        String description,
        CaseType caseType,
        CasePriority priority,
        CaseStatus status,
        Long investigatorId,
        Set<Long> assignedTeamIds,
        String location,
        LocalDate caseStartDate,
        LocalDate caseClosingDate
) {
}
