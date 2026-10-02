package com.caselock.repository;

import com.caselock.entity.Evidence;
import com.caselock.entity.enums.EvidenceCategory;
import com.caselock.entity.enums.IntegrityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    Optional<Evidence> findByEvidenceNumber(String evidenceNumber);

    boolean existsByEvidenceNumber(String evidenceNumber);

    List<Evidence> findByCaseEntityId(Long caseId);

    long countByIntegrityStatus(IntegrityStatus status);

    long countByCaseEntityId(Long caseId);

    @Query("""
            SELECT e FROM Evidence e
            WHERE (:keyword IS NULL OR LOWER(e.evidenceNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(e.originalSha256) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:caseId IS NULL OR e.caseEntity.id = :caseId)
              AND (:category IS NULL OR e.evidenceCategory = :category)
              AND (:custodianId IS NULL OR e.currentCustodian.id = :custodianId)
              AND (:accessibleCaseIds IS NULL OR e.caseEntity.id IN :accessibleCaseIds)
            """)
    Page<Evidence> search(@Param("keyword") String keyword,
                           @Param("caseId") Long caseId,
                           @Param("category") EvidenceCategory category,
                           @Param("custodianId") Long custodianId,
                           @Param("accessibleCaseIds") List<Long> accessibleCaseIds,
                           Pageable pageable);

    @Query("SELECT COUNT(e) FROM Evidence e WHERE e.currentCustodian.id = :userId")
    long countByCurrentCustodian(@Param("userId") Long userId);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
