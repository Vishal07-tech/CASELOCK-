package com.caselock.entity;

import com.caselock.entity.enums.CasePriority;
import com.caselock.entity.enums.CaseStatus;
import com.caselock.entity.enums.CaseType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"assignedTeam", "evidenceItems"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "cases", indexes = {
        @Index(name = "idx_case_number", columnList = "case_number", unique = true),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_created_at", columnList = "created_at")
})
public class Case extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(name = "case_number", nullable = false, unique = true, length = 40)
    private String caseNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 40)
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigator_id", nullable = false)
    private User investigator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "case_team_members",
            joinColumns = @JoinColumn(name = "case_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<User> assignedTeam = new HashSet<>();

    @Column(length = 200)
    private String location;

    @Column(name = "case_start_date")
    private LocalDate caseStartDate;

    @Column(name = "case_closing_date")
    private LocalDate caseClosingDate;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private Set<Evidence> evidenceItems = new HashSet<>();
}
