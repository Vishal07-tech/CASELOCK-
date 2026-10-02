package com.caselock.service;

import com.caselock.dto.request.CaseCreateRequest;
import com.caselock.dto.request.CaseStatusUpdateRequest;
import com.caselock.dto.request.CaseUpdateRequest;
import com.caselock.dto.response.CaseResponse;
import com.caselock.dto.response.CaseSummaryResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.Case;
import com.caselock.entity.User;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.Role;
import com.caselock.exception.ConflictException;
import com.caselock.exception.ForbiddenException;
import com.caselock.exception.ResourceNotFoundException;
import com.caselock.repository.CaseRepository;
import com.caselock.repository.UserRepository;
import com.caselock.security.UserPrincipal;
import com.caselock.util.IdentifierGenerator;
import com.caselock.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final CaseAccessService caseAccessService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public CaseResponse createCase(CaseCreateRequest request, String ipAddress) {
        User investigator = userRepository.findById(request.investigatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Investigator not found.", "USER_NOT_FOUND"));

        Set<User> team = resolveTeam(request.assignedTeamIds());

        String caseNumber = generateUniqueCaseNumber();

        Case newCase = Case.builder()
                .caseNumber(caseNumber)
                .title(request.title())
                .description(request.description())
                .caseType(request.caseType())
                .priority(request.priority() != null ? request.priority() : CasePriority.MEDIUM)
                .status(CaseStatus.OPEN)
                .investigator(investigator)
                .assignedTeam(team)
                .location(request.location())
                .caseStartDate(request.caseStartDate())
                .build();

        newCase = caseRepository.save(newCase);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.CASE_CREATED, "Case", newCase.getId(),
                "Case " + newCase.getCaseNumber() + " (\"" + newCase.getTitle() + "\") created.", ipAddress, AuditResult.SUCCESS);

        notificationService.notify(investigator.getId(), com.caselock.entity.enums.NotificationType.CASE_ASSIGNED,
                "New case assigned", "You have been assigned as investigator on case " + newCase.getCaseNumber() + ".",
                newCase.getId(), null);
        // newCase was reassigned above (newCase = caseRepository.save(newCase)),
        // so it is not effectively final and cannot be captured directly by the
        // lambda below - copy the two fields the lambda needs into final locals.
        Long createdCaseId = newCase.getId();
        String createdCaseNumber = newCase.getCaseNumber();
        team.forEach(member -> notificationService.notify(member.getId(),
                com.caselock.entity.enums.NotificationType.CASE_ASSIGNED,
                "Added to case team", "You were added to the team for case " + createdCaseNumber + ".",
                createdCaseId, null));

        return CaseResponse.from(newCase);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(Long id) {
        Case caseEntity = findCaseOrThrow(id);
        assertAccess(caseEntity, "view");
        return CaseResponse.from(caseEntity);
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseSummaryResponse> search(String keyword, CaseStatus status, CasePriority priority,
                                                      Long investigatorId, Pageable pageable) {
        UserPrincipal principal = SecurityUtil.currentUser();
        // INVESTIGATOR is restricted to cases they lead or are a team member on,
        // but keyword/status/priority search still applies on top of that -
        // restriction and search are independent, not either/or.
        Long restrictedUserId = Role.valueOf(principal.getRole()) == Role.INVESTIGATOR ? principal.getId() : null;
        Page<Case> page = caseRepository.search(keyword, status, priority, investigatorId, restrictedUserId, pageable);
        return PageResponse.from(page.map(CaseSummaryResponse::from));
    }

    @Transactional
    public CaseResponse updateCase(Long id, CaseUpdateRequest request, String ipAddress) {
        Case caseEntity = findCaseOrThrow(id);
        assertAccess(caseEntity, "edit");

        if (request.title() != null) caseEntity.setTitle(request.title());
        if (request.description() != null) caseEntity.setDescription(request.description());
        if (request.caseType() != null) caseEntity.setCaseType(request.caseType());
        if (request.priority() != null) caseEntity.setPriority(request.priority());
        if (request.location() != null) caseEntity.setLocation(request.location());
        if (request.caseStartDate() != null) caseEntity.setCaseStartDate(request.caseStartDate());
        if (request.caseClosingDate() != null) caseEntity.setCaseClosingDate(request.caseClosingDate());
        if (request.investigatorId() != null) {
            User investigator = userRepository.findById(request.investigatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Investigator not found.", "USER_NOT_FOUND"));
            caseEntity.setInvestigator(investigator);
        }
        if (request.assignedTeamIds() != null) {
            caseEntity.setAssignedTeam(resolveTeam(request.assignedTeamIds()));
        }

        CaseStatus previousStatus = caseEntity.getStatus();
        if (request.status() != null && request.status() != previousStatus) {
            caseEntity.setStatus(request.status());
        }

        caseEntity = caseRepository.save(caseEntity);

        auditLogService.record(SecurityUtil.currentUserId(), AuditAction.CASE_UPDATED, "Case", caseEntity.getId(),
                "Case " + caseEntity.getCaseNumber() + " updated.", ipAddress, AuditResult.SUCCESS);

        return CaseResponse.from(caseEntity);
    }

    @Transactional
    public CaseResponse updateStatus(Long id, CaseStatusUpdateRequest request, String ipAddress) {
        Case caseEntity = findCaseOrThrow(id);
        assertAccess(caseEntity, "edit");

        CaseStatus previous = caseEntity.getStatus();
        caseEntity.setStatus(request.status());
        if (request.status() == CaseStatus.CLOSED && caseEntity.getCaseClosingDate() == null) {
            caseEntity.setCaseClosingDate(java.time.LocalDate.now());
        }
        caseEntity = caseRepository.save(caseEntity);

        AuditAction action = switch (request.status()) {
            case CLOSED -> AuditAction.CASE_CLOSED;
            case ARCHIVED -> AuditAction.CASE_ARCHIVED;
            default -> AuditAction.CASE_STATUS_CHANGED;
        };

        auditLogService.record(SecurityUtil.currentUserId(), action, "Case", caseEntity.getId(),
                "Case " + caseEntity.getCaseNumber() + " status changed from " + previous + " to " + request.status()
                        + (request.remarks() != null ? " (" + request.remarks() + ")" : "") + ".",
                ipAddress, AuditResult.SUCCESS);

        Long investigatorId = caseEntity.getInvestigator() != null ? caseEntity.getInvestigator().getId() : null;
        notificationService.notify(investigatorId, com.caselock.entity.enums.NotificationType.CASE_STATUS_CHANGED,
                "Case status changed", "Case " + caseEntity.getCaseNumber() + " is now " + request.status() + ".",
                caseEntity.getId(), null);

        return CaseResponse.from(caseEntity);
    }

    private Set<User> resolveTeam(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        Set<User> users = ids.stream()
                .map(id -> userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Team member with id " + id + " not found.", "USER_NOT_FOUND")))
                .collect(Collectors.toSet());
        return users;
    }

    private String generateUniqueCaseNumber() {
        String candidate;
        do {
            candidate = IdentifierGenerator.nextCaseNumber();
        } while (caseRepository.existsByCaseNumber(candidate));
        return candidate;
    }

    Case findCaseOrThrow(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found.", "CASE_NOT_FOUND"));
    }

    void assertAccess(Case caseEntity, String action) {
        UserPrincipal principal = SecurityUtil.currentUser();
        if (!caseAccessService.canAccess(principal, caseEntity)) {
            auditLogService.record(principal.getId(), AuditAction.UNAUTHORIZED_ACCESS_ATTEMPT, "Case", caseEntity.getId(),
                    "User attempted to " + action + " case " + caseEntity.getCaseNumber() + " without authorization.",
                    null, AuditResult.DENIED);
            throw new ForbiddenException("You are not authorized to access this case.", "CASE_ACCESS_DENIED");
        }
    }
}
