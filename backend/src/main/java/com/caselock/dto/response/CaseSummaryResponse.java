package com.caselock.dto.response;

import com.caselock.entity.Case;
import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.CaseType;

import java.time.LocalDateTime;

// NOTE: evidenceCount below reads the lazy `evidenceItems` collection size,
// which costs one extra SELECT per case in a list result (a known N+1
// trade-off accepted for simplicity in this build - see README's "Build
// Notes" section). For a high-volume production deployment, replace this
// with a batched GROUP BY query (e.g. EvidenceRepository.countByCaseIds)
// joined onto the page of cases instead.
public record CaseSummaryResponse(
        Long id,
        String caseNumber,
        String title,
        CaseType caseType,
        CasePriority priority,
        CaseStatus status,
        String investigatorName,
        int evidenceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CaseSummaryResponse from(Case c) {
        int count = 0;
        try {
            if (c.getEvidenceItems() != null) {
                count = c.getEvidenceItems().size();
            }
        } catch (Exception ignored) {
        }
        return new CaseSummaryResponse(
                c.getId(),
                c.getCaseNumber(),
                c.getTitle(),
                c.getCaseType(),
                c.getPriority(),
                c.getStatus(),
                c.getInvestigator() != null ? c.getInvestigator().getFullName() : null,
                count,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
