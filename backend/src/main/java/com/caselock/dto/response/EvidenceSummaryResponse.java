package com.caselock.dto.response;

import com.caselock.entity.Evidence;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.EvidenceStatus;
import com.caselock.entity.enums.IntegrityStatus;

import java.time.LocalDateTime;

public record EvidenceSummaryResponse(
        Long id,
        String evidenceNumber,
        Long caseId,
        String caseNumber,
        String fileName,
        EvidenceCategory evidenceCategory,
        long fileSizeBytes,
        EvidenceStatus status,
        IntegrityStatus integrityStatus,
        String currentCustodianName,
        LocalDateTime uploadDate
) {
    public static EvidenceSummaryResponse from(Evidence e) {
        return new EvidenceSummaryResponse(
                e.getId(),
                e.getEvidenceNumber(),
                e.getCaseEntity() != null ? e.getCaseEntity().getId() : null,
                e.getCaseEntity() != null ? e.getCaseEntity().getCaseNumber() : null,
                e.getFileName(),
                e.getEvidenceCategory(),
                e.getFileSizeBytes(),
                e.getStatus(),
                e.getIntegrityStatus(),
                e.getCurrentCustodian() != null ? e.getCurrentCustodian().getFullName() : null,
                e.getUploadDate()
        );
    }
}
