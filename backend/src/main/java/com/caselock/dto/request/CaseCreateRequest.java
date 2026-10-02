package com.caselock.dto.request;

import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record CaseCreateRequest(
        @NotBlank(message = "Case title is required") String title,
        String description,
        @NotNull(message = "Case type is required") CaseType caseType,
        CasePriority priority,
        @NotNull(message = "An investigator must be assigned") Long investigatorId,
        Set<Long> assignedTeamIds,
        String location,
        LocalDate caseStartDate
) {
}
