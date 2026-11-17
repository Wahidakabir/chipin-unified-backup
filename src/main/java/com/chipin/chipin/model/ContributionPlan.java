package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores a member's planned (pledged) contribution toward a specific SavingsGoal.
 * Unique constraint guarantees each member has at most one plan per goal.
 */
@Entity
@Table(
    name = "contribution_plans",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_plan_goal_member", columnNames = {"goal_id", "member_id"})
    }
)
public class ContributionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private SavingsGoal goal;

    @NotBlank(message = "Member ID is required")
    @Column(name = "member_id", nullable = false)
    private String memberId;

    @NotBlank(message = "Member name is required")
    @Column(name = "member_name", nullable = false)
    private String memberName;

    @NotNull(message = "Planned amount is required")
    @Positive(message = "Planned amount must be strictly greater than zero")
    @Column(name = "planned_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal plannedAmount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Constructors ---

    public ContributionPlan() {
    }

    public ContributionPlan(SavingsGoal goal, String memberId, String memberName, BigDecimal plannedAmount) {
        this.goal = goal;
        this.memberId = memberId;
        this.memberName = memberName;
        this.plannedAmount = plannedAmount;
    }

    // --- Lifecycle Callbacks ---

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SavingsGoal getGoal() {
        return goal;
    }

    public void setGoal(SavingsGoal goal) {
        this.goal = goal;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
