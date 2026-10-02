package com.caselock.repository;

import com.caselock.entity.AuditLog;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop10ByOrderByEventTimestampDesc();

    List<AuditLog> findTop10ByResultOrderByEventTimestampDesc(AuditResult result);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:userId IS NULL OR a.user.id = :userId)
              AND (:action IS NULL OR a.action = :action)
              AND (:entityType IS NULL OR a.entityType = :entityType)
              AND (:entityId IS NULL OR a.entityId = :entityId)
              AND (:result IS NULL OR a.result = :result)
              AND (:from IS NULL OR a.eventTimestamp >= :from)
              AND (:to IS NULL OR a.eventTimestamp <= :to)
            ORDER BY a.eventTimestamp DESC
            """)
    Page<AuditLog> search(@Param("userId") Long userId,
                           @Param("action") AuditAction action,
                           @Param("entityType") String entityType,
                           @Param("entityId") Long entityId,
                           @Param("result") AuditResult result,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           Pageable pageable);
}
