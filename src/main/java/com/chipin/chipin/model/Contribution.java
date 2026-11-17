package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable financial ledger record of a contribution toward a SavingsGoal.
 * Financial records cannot be modified or deleted once recorded.
 */
@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false, updatable = false)
    private SavingsGoal goal;

    @NotBlank(message = "Member ID is required")
    @Column(name = "member_id", nullable = false, updatable = false)
    private String memberId;

    @NotBlank(message = "Member name is required")
    @Column(name = "member_name", nullable = false, updatable = false)
    private String memberName;

    @NotNull(message = "Contribution amount is required")
    @Positive(message = "Contribution amount must be strictly greater than zero")
    @Column(name = "amount", precision = 15, scale = 2, nullable = false, updatable = false)
    private BigDecimal amount;

    @Column(name = "contributed_at", nullable = false, updatable = false)
    private LocalDateTime contributedAt;

    @Column(name = "note", length = 255, updatable = false)
    private String note;

    // --- Constructors ---

    public Contribution() {
    }

    public Contribution(SavingsGoal goal, String memberId, String memberName, BigDecimal amount, String note) {
        this.goal = goal;
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.note = note;
        this.contributedAt = LocalDateTime.now();
    }

    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        if (this.contributedAt == null) {
            this.contributedAt = LocalDateTime.now();
        }
    }

    // --- Getters & Immutability Enforcement ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SavingsGoal getGoal() {
        return goal;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
