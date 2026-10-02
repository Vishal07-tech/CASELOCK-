package com.caselock.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CaseReportResponse(
        CaseResponse caseInfo,
        List<EvidenceResponse> evidenceSummary,
        List<ChainOfCustodyResponse> chainOfCustody,
        List<IntegrityVerificationResponse> integrityResults,
        List<AuditLogResponse> auditHistory,
        LocalDateTime generatedAt,
        String generatedBy
) {
}
