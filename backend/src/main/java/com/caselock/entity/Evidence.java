package com.caselock.entity;

import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.EvidenceStatus;
import com.caselock.entity.enums.IntegrityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A single item of digital evidence.
 *
 * DATA INTEGRITY RULE: {@code originalSha256} is written exactly once, at
 * registration time, and must never be overwritten afterwards - it is the
 * immutable baseline every later integrity check is compared against.
 * {@code currentSha256} / {@code integrityStatus} are updated each time
 * VERIFY INTEGRITY runs.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "caseEntity")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "evidence", indexes = {
        @Index(name = "idx_evidence_number", columnList = "evidence_number", unique = true),
        @Index(name = "idx_evidence_case", columnList = "case_id"),
        @Index(name = "idx_evidence_hash", columnList = "original_sha256"),
        @Index(name = "idx_evidence_file_name", columnList = "file_name"),
        @Index(name = "idx_evidence_status", columnList = "status")
})
public class Evidence extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(name = "evidence_number", nullable = false, unique = true, length = 40)
    private String evidenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_category", nullable = false, length = 30)
    private EvidenceCategory evidenceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_custodian_id", nullable = false)
    private User currentCustodian;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EvidenceStatus status = EvidenceStatus.REGISTERED;

    /** Immutable - set once on registration, never overwritten. */
    @Column(name = "original_sha256", nullable = false, length = 64, updatable = false)
    private String originalSha256;

    @Column(name = "current_sha256", length = 64)
    private String currentSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_status", nullable = false, length = 20)
    @Builder.Default
    private IntegrityStatus integrityStatus = IntegrityStatus.NOT_YET_VERIFIED;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "is_sealed", nullable = false)
    @Builder.Default
    private boolean sealed = false;
}
