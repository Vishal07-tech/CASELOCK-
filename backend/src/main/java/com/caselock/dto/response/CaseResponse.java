package com.caselock.dto.response;

import com.caselock.entity.Case;
import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.CaseType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record CaseResponse(
        Long id,
        String caseNumber,
        String title,
        String description,
        CaseType caseType,
        CasePriority priority,
        CaseStatus status,
        UserResponse investigator,
        Set<UserResponse> assignedTeam,
        String location,
        LocalDate caseStartDate,
        LocalDate caseClosingDate,
        int evidenceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CaseResponse from(Case c) {
        return new CaseResponse(
                c.getId(),
                c.getCaseNumber(),
                c.getTitle(),
                c.getDescription(),
                c.getCaseType(),
                c.getPriority(),
                c.getStatus(),
                c.getInvestigator() != null ? UserResponse.from(c.getInvestigator()) : null,
                c.getAssignedTeam() == null ? Set.of() :
                        c.getAssignedTeam().stream().map(UserResponse::from).collect(Collectors.toSet()),
                c.getLocation(),
                c.getCaseStartDate(),
                c.getCaseClosingDate(),
                c.getEvidenceItems() != null ? c.getEvidenceItems().size() : 0,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
