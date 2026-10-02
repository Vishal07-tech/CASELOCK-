package com.caselock.service;

import com.caselock.dto.request.EvidenceTransferRequest;
import com.caselock.dto.response.ChainOfCustodyResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.ChainOfCustodyEvent;
import com.caselock.entity.Evidence;
import com.caselock.entity.User;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.CustodyAction;
import com.caselock.entity.enums.EvidenceStatus;
import com.caselock.entity.enums.NotificationType;
import com.caselock.exception.BadRequestException;
import com.caselock.exception.ResourceNotFoundException;
import com.caselock.repository.ChainOfCustodyEventRepository;
import com.caselock.repository.EvidenceRepository;
import com.caselock.repository.UserRepository;
import com.caselock.security.UserPrincipal;
import com.caselock.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChainOfCustodyService {

    private final ChainOfCustodyEventRepository custodyEventRepository;
    private final EvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final EvidenceService evidenceService;
    private final CaseService caseService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public ChainOfCustodyResponse transfer(Long evidenceId, EvidenceTransferRequest request, String ipAddress) {
        Evidence evidence = evidenceService.findEvidenceOrThrow(evidenceId);
        evidenceService.assertAccess(evidence, "transfer");

        if (evidence.isSealed()) {
            throw new BadRequestException("This evidence item is sealed and cannot be transferred until it is reopened.",
                    "EVIDENCE_SEALED");
        }

        User toUser = userRepository.findById(request.toUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found.", "USER_NOT_FOUND"));

        User fromUser = evidence.getCurrentCustodian();

        ChainOfCustodyEvent event = ChainOfCustodyEvent.builder()
                .evidence(evidence)
                .fromUser(fromUser)
                .toUser(toUser)
                .action(CustodyAction.TRANSFERRED)
                .eventTimestamp(LocalDateTime.now())
                .reason(request.reason())
                .location(request.location())
                .remarks(request.remarks())
                .hashAtTransfer(evidence.getCurrentSha256())
                .ipAddress(ipAddress)
                .build();
        event = custodyEventRepository.save(event);

        evidence.setCurrentCustodian(toUser);
        evidence.setStatus(EvidenceStatus.TRANSFERRED);
        evidenceRepository.save(evidence);

        UserPrincipal principal = SecurityUtil.currentUser();
        auditLogService.record(principal.getId(), AuditAction.EVIDENCE_TRANSFERRED, "Evidence", evidence.getId(),
                "Evidence " + evidence.getEvidenceNumber() + " transferred from "
                        + (fromUser != null ? fromUser.getUsername() : "unknown") + " to " + toUser.getUsername() + ".",
                ipAddress, AuditResult.SUCCESS);

        notificationService.notify(toUser.getId(), NotificationType.EVIDENCE_TRANSFER_REQUESTED,
                "Evidence transferred to you",
                "Evidence " + evidence.getEvidenceNumber() + " has been transferred to your custody.",
                evidence.getCaseEntity().getId(), evidence.getId());

        return ChainOfCustodyResponse.from(event);
    }

    @Transactional
    public ChainOfCustodyResponse recordAction(Long evidenceId, CustodyAction action, String reason, String remarks,
                                                String ipAddress) {
        Evidence evidence = evidenceService.findEvidenceOrThrow(evidenceId);
        evidenceService.assertAccess(evidence, "update the custody status of");

        UserPrincipal principal = SecurityUtil.currentUser();

        ChainOfCustodyEvent event = ChainOfCustodyEvent.builder()
                .evidence(evidence)
                .fromUser(evidence.getCurrentCustodian())
                .toUser(evidence.getCurrentCustodian())
                .action(action)
                .eventTimestamp(LocalDateTime.now())
                .reason(reason)
                .remarks(remarks)
                .hashAtTransfer(evidence.getCurrentSha256())
                .ipAddress(ipAddress)
                .build();
        event = custodyEventRepository.save(event);

        if (action == CustodyAction.SEALED) {
            evidence.setSealed(true);
            evidence.setStatus(EvidenceStatus.SEALED);
        } else if (action == CustodyAction.REOPENED) {
            evidence.setSealed(false);
        } else if (action == CustodyAction.RETURNED) {
            evidence.setStatus(EvidenceStatus.RETURNED);
        } else if (action == CustodyAction.ARCHIVED) {
            evidence.setStatus(EvidenceStatus.ARCHIVED);
        } else if (action == CustodyAction.ANALYZED) {
            evidence.setStatus(EvidenceStatus.UNDER_ANALYSIS);
        }
        evidenceRepository.save(evidence);

        auditLogService.record(principal.getId(), AuditAction.EVIDENCE_UPDATED, "Evidence", evidence.getId(),
                "Custody action " + action + " recorded for evidence " + evidence.getEvidenceNumber() + ".",
                ipAddress, AuditResult.SUCCESS);

        return ChainOfCustodyResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<ChainOfCustodyResponse> timelineForEvidence(Long evidenceId) {
        Evidence evidence = evidenceService.findEvidenceOrThrow(evidenceId);
        evidenceService.assertAccess(evidence, "view the chain of custody for");

        return custodyEventRepository.findByEvidenceIdOrderByEventTimestampAsc(evidenceId).stream()
                .map(ChainOfCustodyResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ChainOfCustodyResponse> timelineForCase(Long caseId, Pageable pageable) {
        // Same case-level authorization rule as everything else touching this
        // case - without this check an INVESTIGATOR not assigned to the case
        // could otherwise read its custody history just by guessing the id.
        com.caselock.entity.Case caseEntity = caseService.findCaseOrThrow(caseId);
        caseService.assertAccess(caseEntity, "view the chain of custody for");

        Page<ChainOfCustodyEvent> page = custodyEventRepository.findByEvidenceCaseEntityId(caseId, pageable);
        return PageResponse.from(page.map(ChainOfCustodyResponse::from));
    }
}
