package com.caselock.repository;

import com.caselock.entity.ChainOfCustodyEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChainOfCustodyEventRepository extends JpaRepository<ChainOfCustodyEvent, Long> {

    List<ChainOfCustodyEvent> findByEvidenceIdOrderByEventTimestampAsc(Long evidenceId);

    Page<ChainOfCustodyEvent> findByEvidenceCaseEntityId(Long caseId, Pageable pageable);

    long countByAction(com.caselock.entity.enums.CustodyAction action);
}
