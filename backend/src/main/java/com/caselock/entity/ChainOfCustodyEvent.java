package com.caselock.entity;

import com.caselock.entity.enums.CustodyAction;
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

/**
 * One immutable event in an evidence item's chain of custody. Rows are
 * append-only: the service layer never updates or deletes an existing
 * event, it only ever inserts new ones.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "chain_of_custody_events", indexes = {
        @Index(name = "idx_custody_evidence", columnList = "evidence_id"),
        @Index(name = "idx_custody_timestamp", columnList = "event_timestamp")
})
public class ChainOfCustodyEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustodyAction action;

    @Column(name = "event_timestamp", nullable = false)
    private java.time.LocalDateTime eventTimestamp;

    @Column(length = 500)
    private String reason;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "hash_at_transfer", length = 64)
    private String hashAtTransfer;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;
}
