package com.caselock.service;

import com.caselock.dto.response.AuditLogResponse;
import com.caselock.dto.response.CaseSummaryResponse;
import com.caselock.dto.response.DashboardStatsResponse;
import com.caselock.dto.response.EvidenceSummaryResponse;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.CustodyAction;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.IntegrityStatus;
import com.caselock.repository.AuditLogRepository;
import com.caselock.repository.CaseRepository;
import com.caselock.repository.ChainOfCustodyEventRepository;
import com.caselock.repository.EvidenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates the numbers shown on the main dashboard. Kept deliberately
 * focused on the handful of figures the spec calls out rather than trying
 * to be an exhaustive analytics engine.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyEventRepository custodyEventRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalCases = caseRepository.count();
        long closedCases = caseRepository.countByStatus(CaseStatus.CLOSED) + caseRepository.countByStatus(CaseStatus.ARCHIVED);
        long activeCases = totalCases - closedCases;

        long totalEvidence = evidenceRepository.count();
        long verifiedEvidence = evidenceRepository.countByIntegrityStatus(IntegrityStatus.VERIFIED);
        long integrityAlerts = evidenceRepository.countByIntegrityStatus(IntegrityStatus.COMPROMISED);
        long pendingTransfers = custodyEventRepository.countByAction(CustodyAction.TRANSFERRED);

        Map<String, Long> casesByStatus = new LinkedHashMap<>();
        for (CaseStatus status : CaseStatus.values()) {
            casesByStatus.put(status.name(), caseRepository.countByStatus(status));
        }

        Map<String, Long> evidenceByCategory = new LinkedHashMap<>();
        for (EvidenceCategory category : EvidenceCategory.values()) {
            long count = evidenceRepository.search(null, null, category, null, null, PageRequest.of(0, 1)).getTotalElements();
            evidenceByCategory.put(category.name(), count);
        }

        Map<String, Long> evidenceByIntegrity = new LinkedHashMap<>();
        for (IntegrityStatus status : IntegrityStatus.values()) {
            evidenceByIntegrity.put(status.name(), evidenceRepository.countByIntegrityStatus(status));
        }

        List<DashboardStatsResponse.MonthlyActivity> monthlyActivity = buildMonthlyActivity();

        List<CaseSummaryResponse> recentCases = caseRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(CaseSummaryResponse::from).collect(Collectors.toList());

        List<EvidenceSummaryResponse> recentEvidence = evidenceRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream().map(EvidenceSummaryResponse::from).collect(Collectors.toList());

        List<AuditLogResponse> recentActivity = auditLogRepository.findTop10ByOrderByEventTimestampDesc()
                .stream().map(AuditLogResponse::from).collect(Collectors.toList());

        List<AuditLogResponse> securityAlerts = auditLogRepository.findTop10ByResultOrderByEventTimestampDesc(AuditResult.DENIED)
                .stream().map(AuditLogResponse::from).collect(Collectors.toList());

        return new DashboardStatsResponse(
                totalCases, activeCases, closedCases, totalEvidence, verifiedEvidence, integrityAlerts, pendingTransfers,
                casesByStatus, evidenceByCategory, evidenceByIntegrity, monthlyActivity,
                recentCases, recentEvidence, recentActivity, securityAlerts
        );
    }

    private List<DashboardStatsResponse.MonthlyActivity> buildMonthlyActivity() {
        LocalDate now = LocalDate.now();
        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(now::minusMonths)
                .sorted()
                .map(monthDate -> {
                    LocalDate start = monthDate.withDayOfMonth(1);
                    LocalDate end = start.plusMonths(1);
                    long caseCount = caseRepository.countByCreatedAtBetween(start.atStartOfDay(), end.atStartOfDay());
                    long evidenceCount = countEvidenceCreatedBetween(start, end);
                    String label = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + start.getYear();
                    return new DashboardStatsResponse.MonthlyActivity(label, caseCount, evidenceCount);
                })
                .collect(Collectors.toList());
    }

    private long countEvidenceCreatedBetween(LocalDate start, LocalDate end) {
        return evidenceRepository.countByCreatedAtBetween(start.atStartOfDay(), end.atStartOfDay());
    }
}
