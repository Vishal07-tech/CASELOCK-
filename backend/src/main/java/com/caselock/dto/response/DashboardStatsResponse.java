package com.caselock.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardStatsResponse(
        long totalCases,
        long activeCases,
        long closedCases,
        long totalEvidence,
        long verifiedEvidence,
        long integrityAlerts,
        long pendingTransfers,
        Map<String, Long> casesByStatus,
        Map<String, Long> evidenceByCategory,
        Map<String, Long> evidenceByIntegrityStatus,
        List<MonthlyActivity> monthlyCaseActivity,
        List<CaseSummaryResponse> recentCases,
        List<EvidenceSummaryResponse> recentEvidence,
        List<AuditLogResponse> recentActivity,
        List<AuditLogResponse> securityAlerts
) {
    public record MonthlyActivity(String month, long caseCount, long evidenceCount) {
    }
}
