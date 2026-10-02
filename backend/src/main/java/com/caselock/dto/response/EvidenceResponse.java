package com.caselock.dto.response;

import com.caselock.entity.Evidence;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.EvidenceStatus;
import com.caselock.entity.enums.IntegrityStatus;

import java.time.LocalDateTime;

public record EvidenceResponse(
        Long id,
        String evidenceNumber,
        Long caseId,
        String caseNumber,
        String caseTitle,
        String fileName,
        String fileType,
        long fileSizeBytes,
        UserResponse uploadedBy,
        LocalDateTime uploadDate,
        String description,
        EvidenceCategory evidenceCategory,
        UserResponse currentCustodian,
        EvidenceStatus status,
        String originalSha256,
        String currentSha256,
        IntegrityStatus integrityStatus,
        LocalDateTime lastVerifiedAt,
        boolean sealed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EvidenceResponse from(Evidence e) {
        return new EvidenceResponse(
                e.getId(),
                e.getEvidenceNumber(),
                e.getCaseEntity() != null ? e.getCaseEntity().getId() : null,
                e.getCaseEntity() != null ? e.getCaseEntity().getCaseNumber() : null,
                e.getCaseEntity() != null ? e.getCaseEntity().getTitle() : null,
                e.getFileName(),
                e.getFileType(),
                e.getFileSizeBytes(),
                e.getUploadedBy() != null ? UserResponse.from(e.getUploadedBy()) : null,
                e.getUploadDate(),
                e.getDescription(),
                e.getEvidenceCategory(),
                e.getCurrentCustodian() != null ? UserResponse.from(e.getCurrentCustodian()) : null,
                e.getStatus(),
                e.getOriginalSha256(),
                e.getCurrentSha256(),
                e.getIntegrityStatus(),
                e.getLastVerifiedAt(),
                e.isSealed(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
