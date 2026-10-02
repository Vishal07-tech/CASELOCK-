package com.caselock.service;

import com.caselock.dto.request.EvidenceUploadMetadata;
import com.caselock.dto.response.EvidenceResponse;
import com.caselock.dto.response.EvidenceSummaryResponse;
import com.caselock.dto.response.IntegrityVerificationResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.Case;
import com.caselock.entity.ChainOfCustodyEvent;
import com.caselock.entity.Evidence;
import com.caselock.entity.User;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.CustodyAction;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.EvidenceStatus;
import com.caselock.entity.enums.IntegrityStatus;
import com.caselock.entity.enums.NotificationType;
import com.caselock.entity.enums.Role;
import com.caselock.exception.BadRequestException;
import com.caselock.exception.ForbiddenException;
import com.caselock.exception.ResourceNotFoundException;
import com.caselock.repository.CaseRepository;
import com.caselock.repository.ChainOfCustodyEventRepository;
import com.caselock.repository.EvidenceRepository;
import com.caselock.repository.UserRepository;
import com.caselock.security.UserPrincipal;
import com.caselock.util.HashUtil;
import com.caselock.util.IdentifierGenerator;
import com.caselock.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyEventRepository custodyEventRepository;
    private final CaseService caseService;
    private final CaseAccessService caseAccessService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;

    @Transactional
    public EvidenceResponse register(MultipartFile file, EvidenceUploadMetadata metadata, String ipAddress) {
        Case caseEntity = caseService.findCaseOrThrow(metadata.caseId());
        caseService.assertAccess(caseEntity, "upload evidence to");

        fileStorageService.validate(file);

        String storedFileName = fileStorageService.store(file);
        String hash;
        try (InputStream verifyStream = fileStorageService.load(storedFileName)) {
            hash = HashUtil.sha256(verifyStream);
        } catch (Exception ex) {
            throw new BadRequestException("Something went wrong while processing the evidence file. Please try again.");
        }

        UserPrincipal principal = SecurityUtil.currentUser();
        User uploader = userRef(principal.getId());

        Evidence evidence = Evidence.builder()
                .evidenceNumber(generateUniqueEvidenceNumber())
                .caseEntity(caseEntity)
                .fileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .fileType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .uploadedBy(uploader)
                .uploadDate(LocalDateTime.now())
                .description(metadata.description())
                .evidenceCategory(metadata.evidenceCategory())
                .currentCustodian(uploader)
                .status(EvidenceStatus.REGISTERED)
                .originalSha256(hash)
                .currentSha256(hash)
                .integrityStatus(IntegrityStatus.NOT_YET_VERIFIED)
                .build();

        evidence = evidenceRepository.save(evidence);

        recordCustodyEvent(evidence, null, uploader, CustodyAction.UPLOADED,
                "Initial registration", caseEntity.getLocation(), "Evidence registered and SHA-256 hash computed.",
                hash, ipAddress);

        auditLogService.record(principal.getId(), AuditAction.EVIDENCE_UPLOADED, "Evidence", evidence.getId(),
                "Evidence " + evidence.getEvidenceNumber() + " (" + evidence.getFileName() + ") uploaded to case "
                        + caseEntity.getCaseNumber() + ".", ipAddress, AuditResult.SUCCESS);

        return EvidenceResponse.from(evidence);
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getEvidence(Long id) {
        Evidence evidence = findEvidenceOrThrow(id);
        assertAccess(evidence, "view");
        return EvidenceResponse.from(evidence);
    }

    @Transactional(readOnly = true)
    public PageResponse<EvidenceSummaryResponse> search(String keyword, Long caseId, EvidenceCategory category,
                                                          Long custodianId, Pageable pageable) {
        UserPrincipal principal = SecurityUtil.currentUser();

        if (caseId != null) {
            // A specific case was requested - enforce that case's access rule
            // directly rather than silently filtering it out.
            Case caseEntity = caseService.findCaseOrThrow(caseId);
            caseService.assertAccess(caseEntity, "view evidence for");
        }

        List<Long> accessibleCaseIds = null;
        if (caseId == null && Role.valueOf(principal.getRole()) == Role.INVESTIGATOR) {
            // INVESTIGATOR has no case filter applied - restrict the result set
            // to only the cases they lead or are a team member on, otherwise
            // this endpoint would leak evidence from every case system-wide.
            accessibleCaseIds = caseRepository.findAccessibleCaseIds(principal.getId());
            if (accessibleCaseIds.isEmpty()) {
                return PageResponse.from(org.springframework.data.domain.Page.<Evidence>empty(pageable).map(EvidenceSummaryResponse::from));
            }
        }

        Page<Evidence> page = evidenceRepository.search(keyword, caseId, category, custodianId, accessibleCaseIds, pageable);
        return PageResponse.from(page.map(EvidenceSummaryResponse::from));
    }

    @Transactional
    public byte[] download(Long id, String ipAddress) {
        Evidence evidence = findEvidenceOrThrow(id);
        assertAccess(evidence, "download");

        try (InputStream in = fileStorageService.load(evidence.getStoredFileName())) {
            byte[] bytes = in.readAllBytes();

            recordCustodyEvent(evidence, evidence.getCurrentCustodian(), evidence.getCurrentCustodian(),
                    CustodyAction.DOWNLOADED, "Evidence file download", null, null, evidence.getCurrentSha256(), ipAddress);

            auditLogService.record(SecurityUtil.currentUserId(), AuditAction.EVIDENCE_DOWNLOADED, "Evidence", evidence.getId(),
                    "Evidence " + evidence.getEvidenceNumber() + " downloaded.", ipAddress, AuditResult.SUCCESS);

            return bytes;
        } catch (Exception ex) {
            throw new BadRequestException("Something went wrong while downloading the evidence file. Please try again.");
        }
    }

    public Evidence loadEvidenceEntityForDownloadMetadata(Long id) {
        return findEvidenceOrThrow(id);
    }

    @Transactional
    public IntegrityVerificationResponse verifyIntegrity(Long id, String ipAddress) {
        Evidence evidence = findEvidenceOrThrow(id);
        assertAccess(evidence, "verify the integrity of");

        String currentHash;
        try (InputStream in = fileStorageService.load(evidence.getStoredFileName())) {
            currentHash = HashUtil.sha256(in);
        } catch (Exception ex) {
            throw new BadRequestException("Something went wrong while verifying the evidence. Please try again.");
        }

        boolean match = currentHash.equalsIgnoreCase(evidence.getOriginalSha256());
        evidence.setCurrentSha256(currentHash);
        evidence.setIntegrityStatus(match ? IntegrityStatus.VERIFIED : IntegrityStatus.COMPROMISED);
        evidence.setLastVerifiedAt(LocalDateTime.now());
        if (match && evidence.getStatus() == EvidenceStatus.REGISTERED) {
            evidence.setStatus(EvidenceStatus.VERIFIED);
        }
        evidence = evidenceRepository.save(evidence);

        UserPrincipal principal = SecurityUtil.currentUser();
        recordCustodyEvent(evidence, evidence.getCurrentCustodian(), userRef(principal.getId()),
                CustodyAction.VERIFIED, "Integrity verification",
                null, match ? "Hash matches original - integrity verified." : "HASH MISMATCH - integrity compromised.",
                currentHash, ipAddress);

        auditLogService.record(principal.getId(), AuditAction.INTEGRITY_VERIFIED, "Evidence", evidence.getId(),
                "Integrity check on " + evidence.getEvidenceNumber() + ": " + evidence.getIntegrityStatus() + ".",
                ipAddress, AuditResult.SUCCESS);

        if (!match) {
            Long custodianId = evidence.getCurrentCustodian() != null ? evidence.getCurrentCustodian().getId() : null;
            notificationService.notify(custodianId, NotificationType.EVIDENCE_INTEGRITY_FAILED,
                    "Evidence integrity verification failed",
                    "Evidence " + evidence.getEvidenceNumber() + " failed integrity verification (hash mismatch).",
                    evidence.getCaseEntity().getId(), evidence.getId());
        }

        return new IntegrityVerificationResponse(
                evidence.getId(),
                evidence.getEvidenceNumber(),
                evidence.getOriginalSha256(),
                currentHash,
                evidence.getIntegrityStatus(),
                match,
                evidence.getLastVerifiedAt(),
                match
                        ? "Integrity verified: the file's current SHA-256 hash matches the original hash recorded at registration."
                        : "Integrity compromised: the file's current SHA-256 hash does NOT match the original hash recorded at registration."
        );
    }

    @Transactional
    public EvidenceResponse recordAccess(Long id, String ipAddress) {
        Evidence evidence = findEvidenceOrThrow(id);
        assertAccess(evidence, "access");

        UserPrincipal principal = SecurityUtil.currentUser();
        recordCustodyEvent(evidence, evidence.getCurrentCustodian(), userRef(principal.getId()),
                CustodyAction.ACCESSED, "Evidence details viewed", null, null, evidence.getCurrentSha256(), ipAddress);

        auditLogService.record(principal.getId(), AuditAction.EVIDENCE_ACCESSED, "Evidence", evidence.getId(),
                "Evidence " + evidence.getEvidenceNumber() + " accessed.", ipAddress, AuditResult.SUCCESS);

        return EvidenceResponse.from(evidence);
    }

    private void recordCustodyEvent(Evidence evidence, User from, User to, CustodyAction action, String reason,
                                     String location, String remarks, String hash, String ipAddress) {
        ChainOfCustodyEvent event = ChainOfCustodyEvent.builder()
                .evidence(evidence)
                .fromUser(from)
                .toUser(to)
                .action(action)
                .eventTimestamp(LocalDateTime.now())
                .reason(reason)
                .location(location)
                .remarks(remarks)
                .hashAtTransfer(hash)
                .ipAddress(ipAddress)
                .build();
        custodyEventRepository.save(event);
    }

    private String generateUniqueEvidenceNumber() {
        String candidate;
        do {
            candidate = IdentifierGenerator.nextEvidenceNumber();
        } while (evidenceRepository.existsByEvidenceNumber(candidate));
        return candidate;
    }

    Evidence findEvidenceOrThrow(Long id) {
        return evidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found.", "EVIDENCE_NOT_FOUND"));
    }

    void assertAccess(Evidence evidence, String action) {
        UserPrincipal principal = SecurityUtil.currentUser();
        if (!caseAccessService.canAccess(principal, evidence.getCaseEntity())) {
            auditLogService.record(principal.getId(), AuditAction.UNAUTHORIZED_ACCESS_ATTEMPT, "Evidence", evidence.getId(),
                    "User attempted to " + action + " evidence " + evidence.getEvidenceNumber() + " without authorization.",
                    null, AuditResult.DENIED);
            throw new ForbiddenException("You are not authorized to access this evidence item.", "EVIDENCE_ACCESS_DENIED");
        }
    }

    private User userRef(Long id) {
        // A managed lazy-loading proxy, not a real DB round-trip, so it is safe
        // to assign to a @ManyToOne association without triggering Hibernate's
        // "unsaved transient instance" error.
        return userRepository.getReferenceById(id);
    }
}
