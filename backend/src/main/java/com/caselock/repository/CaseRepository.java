package com.caselock.repository;

import com.caselock.entity.Case;
import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<Case, Long> {

    Optional<Case> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    long countByStatus(CaseStatus status);

    @Query("""
            SELECT c FROM Case c
            WHERE (:keyword IS NULL OR LOWER(c.caseNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR c.status = :status)
              AND (:priority IS NULL OR c.priority = :priority)
              AND (:investigatorId IS NULL OR c.investigator.id = :investigatorId)
              AND (:restrictedUserId IS NULL
                   OR c.investigator.id = :restrictedUserId
                   OR :restrictedUserId IN (SELECT t.id FROM c.assignedTeam t))
            """)
    Page<Case> search(@Param("keyword") String keyword,
                       @Param("status") CaseStatus status,
                       @Param("priority") CasePriority priority,
                       @Param("investigatorId") Long investigatorId,
                       @Param("restrictedUserId") Long restrictedUserId,
                       Pageable pageable);

    /**
     * IDs of every case a given user may access as INVESTIGATOR (lead or team
     * member). Used to scope evidence/audit-adjacent list queries for that
     * role without needing to load the full Case entities.
     */
    @Query("""
            SELECT c.id FROM Case c
            WHERE c.investigator.id = :userId
               OR :userId IN (SELECT t.id FROM c.assignedTeam t)
            """)
    List<Long> findAccessibleCaseIds(@Param("userId") Long userId);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
