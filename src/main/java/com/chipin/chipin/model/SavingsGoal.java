package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collaborative savings goal set by a group in Chipin.
 */
@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Group ID is required")
    @Column(name = "group_id", nullable = false)
    private String groupId;

    @NotBlank(message = "Creator ID is required")
    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be strictly greater than zero")
    @Column(name = "target_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @NotNull
    @PositiveOrZero(message = "Current saved amount cannot be negative")
    @Column(name = "current_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @NotNull(message = "Deadline is required")
    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GoalStatus status = GoalStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContributionPlan> plans = new ArrayList<>();

    @OneToMany(mappedBy = "goal", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Contribution> contributions = new ArrayList<>();

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Milestone> milestones = new ArrayList<>();

    // --- Constructors ---

    public SavingsGoal() {
    }

    public SavingsGoal(String groupId, String creatorId, String title, String description,
                       BigDecimal targetAmount, LocalDate deadline) {
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
        this.currentAmount = BigDecimal.ZERO;
        this.status = GoalStatus.ACTIVE;
    }

    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.currentAmount == null) {
            this.currentAmount = BigDecimal.ZERO;
        }
        if (this.status == null) {
            this.status = GoalStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Helper Methods ---

    public BigDecimal getRemainingAmount() {
        if (targetAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal current = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        BigDecimal remaining = targetAmount.subtract(current);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ContributionPlan> getPlans() {
        return plans;
    }

    public void setPlans(List<ContributionPlan> plans) {
        this.plans = plans;
    }

    public List<Contribution> getContributions() {
        return contributions;
    }

    public void setContributions(List<Contribution> contributions) {
        this.contributions = contributions;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<Milestone> milestones) {
        this.milestones = milestones;
    }
}
