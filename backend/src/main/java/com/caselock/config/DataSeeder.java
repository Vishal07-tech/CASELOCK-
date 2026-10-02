package com.caselock.config;

import com.caselock.entity.AuditLog;
import com.caselock.entity.Case;
import com.caselock.entity.ChainOfCustodyEvent;
import com.caselock.entity.Evidence;
import com.caselock.entity.LoginHistory;
import com.caselock.entity.Notification;
import com.caselock.entity.User;
import com.caselock.entity.enums.AccountStatus;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.CaseType;
import com.caselock.entity.enums.CustodyAction;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.EvidenceStatus;
import com.caselock.entity.enums.IntegrityStatus;
import com.caselock.entity.enums.NotificationType;
import com.caselock.entity.enums.Role;
import com.caselock.repository.AuditLogRepository;
import com.caselock.repository.CaseRepository;
import com.caselock.repository.ChainOfCustodyEventRepository;
import com.caselock.repository.EvidenceRepository;
import com.caselock.repository.LoginHistoryRepository;
import com.caselock.repository.NotificationRepository;
import com.caselock.repository.UserRepository;
import com.caselock.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Seeds a realistic (but entirely fictional) demo dataset - users for each
 * role, a handful of cases, evidence with real SHA-256 hashes computed
 * against small sample files actually written to disk, chain-of-custody
 * events, audit log entries and notifications - so the application is
 * immediately explorable after first startup.
 *
 * Only runs when {@code caselock.seed.enabled=true} (the dev profile sets
 * this; the prod profile disables it) and only if the users table is empty,
 * so it never re-seeds or duplicates data on subsequent restarts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;
    private final ChainOfCustodyEventRepository custodyEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageProperties fileStorageProperties;

    private final org.springframework.core.env.Environment environment;

    @Override
    public void run(String... args) throws Exception {
        boolean seedEnabled = Boolean.parseBoolean(environment.getProperty("caselock.seed.enabled", "false"));
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        log.info("Seeding CaseLock demo data...");

        User admin = saveUser("admin", "admin@caselock.demo", "Admin123!", "Ava Mitchell",
                "ADM-001", "IT & Systems", Role.ADMIN);
        User investigator = saveUser("investigator", "investigator@caselock.demo", "Investigate123!", "Daniel Reyes",
                "INV-104", "Criminal Investigations", Role.INVESTIGATOR);
        User investigator2 = saveUser("investigator2", "investigator2@caselock.demo", "Investigate123!", "Priya Nair",
                "INV-118", "Cybercrime Unit", Role.INVESTIGATOR);
        User analyst = saveUser("analyst", "analyst@caselock.demo", "Analyze123!", "Marcus Lee",
                "FOR-027", "Digital Forensics Lab", Role.FORENSIC_ANALYST);
        User legal = saveUser("legal", "legal@caselock.demo", "Legal123!", "Sofia Grant",
                "LEG-009", "Legal Affairs", Role.LEGAL_OFFICER);
        User viewer = saveUser("viewer", "viewer@caselock.demo", "Viewer123!", "Tom Alvarez",
                "VWR-002", "External Audit", Role.VIEWER);

        Case case1 = saveCase("CASE-2026-000001", "Downtown Data Breach Investigation",
                "Suspected unauthorized access to a municipal billing database; investigating exfiltrated records and entry vector.",
                CaseType.CYBERCRIME, CasePriority.HIGH, CaseStatus.UNDER_INVESTIGATION,
                investigator, Set.of(analyst, legal), "Precinct 4 Digital Forensics Lab", LocalDate.now().minusDays(21));

        Case case2 = saveCase("CASE-2026-000002", "Warehouse Fraud & Falsified Invoices",
                "Internal audit flagged a pattern of inflated invoices tied to a logistics vendor; evidence includes emails and spreadsheets.",
                CaseType.FRAUD, CasePriority.MEDIUM, CaseStatus.EVIDENCE_REVIEW,
                investigator2, Set.of(legal, viewer), "Corporate HQ, 3rd Floor", LocalDate.now().minusDays(10));

        Case case3 = saveCase("CASE-2026-000003", "Employee Harassment Complaint - Archived",
                "Internal HR investigation closed after mediation; retained for compliance records.",
                CaseType.INTERNAL_INVESTIGATION, CasePriority.LOW, CaseStatus.ARCHIVED,
                investigator, Set.of(), "Corporate HQ, HR Wing", LocalDate.now().minusDays(120));
        case3.setCaseClosingDate(LocalDate.now().minusDays(60));
        caseRepository.save(case3);

        seedEvidence(case1, investigator, analyst,
                "network_capture_04-18.pcap", EvidenceCategory.OTHER,
                "Packet capture from the suspected point of entry, collected during initial triage.");
        seedEvidence(case1, analyst, analyst,
                "server_access_log.txt", EvidenceCategory.TEXT_FILE,
                "Web server access log covering the 48 hours surrounding the breach.");
        seedEvidence(case2, investigator2, investigator2,
                "vendor_invoice_batch.pdf", EvidenceCategory.PDF,
                "Batch of vendor invoices flagged by the internal audit team as potentially falsified.");

        auditLogRepository.save(AuditLog.builder()
                .user(admin).action(AuditAction.USER_CREATED).entityType("User").entityId(admin.getId())
                .description("Demo dataset initialized.").ipAddress("127.0.0.1").result(AuditResult.SUCCESS)
                .eventTimestamp(LocalDateTime.now().minusDays(21)).build());

        notificationRepository.save(Notification.builder()
                .recipient(investigator).type(NotificationType.CASE_ASSIGNED)
                .title("New case assigned")
                .message("You have been assigned as investigator on case CASE-2026-000001.")
                .relatedCaseId(case1.getId()).read(false).build());

        notificationRepository.save(Notification.builder()
                .recipient(analyst).type(NotificationType.EVIDENCE_ASSIGNED)
                .title("Evidence added to your case")
                .message("New evidence was uploaded to case CASE-2026-000001 for analysis.")
                .relatedCaseId(case1.getId()).read(false).build());

        loginHistoryRepository.save(LoginHistory.builder()
                .user(admin).username(admin.getUsername()).successful(true)
                .ipAddress("127.0.0.1").userAgent("Seed/1.0").attemptedAt(LocalDateTime.now().minusDays(1)).build());

        log.info("CaseLock demo data seeded: {} users, {} cases, {} evidence items.",
                userRepository.count(), caseRepository.count(), evidenceRepository.count());
        log.info("Demo credentials (username / password): admin/Admin123!, investigator/Investigate123!, " +
                "analyst/Analyze123!, legal/Legal123!, viewer/Viewer123!");
    }

    private User saveUser(String username, String email, String rawPassword, String fullName,
                           String badge, String department, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .badgeNumber(badge)
                .department(department)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build());
    }

    private Case saveCase(String number, String title, String description, CaseType type, CasePriority priority,
                           CaseStatus status, User investigator, Set<User> team, String location, LocalDate start) {
        return caseRepository.save(Case.builder()
                .caseNumber(number)
                .title(title)
                .description(description)
                .caseType(type)
                .priority(priority)
                .status(status)
                .investigator(investigator)
                .assignedTeam(team)
                .location(location)
                .caseStartDate(start)
                .build());
    }

    private void seedEvidence(Case caseEntity, User uploader, User custodian, String fileName,
                               EvidenceCategory category, String description) throws IOException {
        String content = "CaseLock demo evidence file\nCase: " + caseEntity.getCaseNumber()
                + "\nFile: " + fileName + "\nThis is synthetic sample content generated for demo purposes only.\n";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        Path root = Paths.get(fileStorageProperties.path()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String storedFileName = java.util.UUID.randomUUID() + "-" + fileName;
        Files.write(root.resolve(storedFileName), bytes);

        String hash = HashUtil.sha256(bytes);
        String evidenceNumber = "EVD-2026-%06d".formatted((int) (evidenceRepository.count() + 1));

        Evidence evidence = evidenceRepository.save(Evidence.builder()
                .evidenceNumber(evidenceNumber)
                .caseEntity(caseEntity)
                .fileName(fileName)
                .storedFileName(storedFileName)
                .fileType("text/plain")
                .fileSizeBytes(bytes.length)
                .uploadedBy(uploader)
                .uploadDate(LocalDateTime.now().minusDays(15))
                .description(description)
                .evidenceCategory(category)
                .currentCustodian(custodian)
                .status(EvidenceStatus.VERIFIED)
                .originalSha256(hash)
                .currentSha256(hash)
                .integrityStatus(IntegrityStatus.VERIFIED)
                .lastVerifiedAt(LocalDateTime.now().minusDays(14))
                .build());

        custodyEventRepository.save(ChainOfCustodyEvent.builder()
                .evidence(evidence).toUser(uploader).action(CustodyAction.UPLOADED)
                .eventTimestamp(LocalDateTime.now().minusDays(15))
                .reason("Initial registration").location(caseEntity.getLocation())
                .remarks("Evidence registered and SHA-256 hash computed.")
                .hashAtTransfer(hash).ipAddress("127.0.0.1").build());

        custodyEventRepository.save(ChainOfCustodyEvent.builder()
                .evidence(evidence).fromUser(uploader).toUser(custodian).action(CustodyAction.VERIFIED)
                .eventTimestamp(LocalDateTime.now().minusDays(14))
                .reason("Integrity verification").remarks("Hash matches original - integrity verified.")
                .hashAtTransfer(hash).ipAddress("127.0.0.1").build());

        auditLogRepository.save(AuditLog.builder()
                .user(uploader).action(AuditAction.EVIDENCE_UPLOADED).entityType("Evidence").entityId(evidence.getId())
                .description("Evidence " + evidenceNumber + " (" + fileName + ") uploaded to case " + caseEntity.getCaseNumber() + ".")
                .ipAddress("127.0.0.1").result(AuditResult.SUCCESS)
                .eventTimestamp(LocalDateTime.now().minusDays(15)).build());
    }
}
