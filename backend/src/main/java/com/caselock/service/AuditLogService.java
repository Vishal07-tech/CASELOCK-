package com.caselock.service;

import com.caselock.dto.response.AuditLogResponse;
import com.caselock.dto.response.PageResponse;
import com.caselock.entity.AuditLog;
import com.caselock.entity.User;
import com.caselock.entity.enums.AuditAction;
import com.caselock.entity.enums.AuditResult;
import com.caselock.repository.AuditLogRepository;
import com.caselock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Central place every other service calls to append an audit trail entry.
 * Entries are written in their own REQUIRES_NEW transaction so that an
 * audit record for a *failed* operation (e.g. an unauthorized access
 * attempt) still gets persisted even if the surrounding transaction rolls
 * back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, AuditAction action, String entityType, Long entityId,
                        String description, String ipAddress, AuditResult result) {
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        AuditLog log = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(ipAddress)
                .result(result)
                .eventTimestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(Long userId, AuditAction action, String entityType, Long entityId,
                                                  AuditResult result, LocalDateTime from, LocalDateTime to,
                                                  Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.search(userId, action, entityType, entityId, result, from, to, pageable);
        return PageResponse.from(page.map(AuditLogResponse::from));
    }
}
