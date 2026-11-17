package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tracks milestone checkpoints (25%, 50%, 75%, 100%) for a SavingsGoal.
 * Prevents duplicate recording or repeated celebrations of the same milestone.
 */
@Entity
@Table(
    name = "milestones",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_milestone_goal_percentage", columnNames = {"goal_id", "percentage"})
    }
)
@Check(constraints = "percentage IN (25, 50, 75, 100)")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private SavingsGoal goal;

    /**
     * Percentage milestone threshold (e.g. 25, 50, 75, 100).
     */
    @NotNull(message = "Milestone percentage is required")
    @Min(value = 1, message = "Percentage must be at least 1")
    @Max(value = 100, message = "Percentage cannot exceed 100")
    @Column(name = "percentage", nullable = false)
    private Integer percentage;

    /**
     * Monetary threshold required to achieve this milestone.
     * Calculated as: (targetAmount * percentage) / 100.
     */
    @NotNull(message = "Target amount snapshot is required")
    @Column(name = "target_amount_snapshot", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetAmountSnapshot;

    @NotNull
    @Column(name = "is_achieved", nullable = false)
    private boolean isAchieved = false;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    // --- Constructors ---

    public Milestone() {
    }

    public Milestone(SavingsGoal goal, Integer percentage, BigDecimal targetAmountSnapshot) {
        this.goal = goal;
        this.percentage = percentage;
        this.targetAmountSnapshot = targetAmountSnapshot;
        this.isAchieved = false;
    }

    // --- Milestone Business Action ---

    /**
     * Atomically marks the milestone as achieved with timestamp if not already achieved.
     * @return true if state changed, false if milestone was already achieved previously.
     */
    public boolean achieve() {
        if (!this.isAchieved) {
            this.isAchieved = true;
            this.achievedAt = LocalDateTime.now();
            return true;
        }
        return false;
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

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public BigDecimal getTargetAmountSnapshot() {
        return targetAmountSnapshot;
    }

    public void setTargetAmountSnapshot(BigDecimal targetAmountSnapshot) {
        this.targetAmountSnapshot = targetAmountSnapshot;
    }

    public boolean isAchieved() {
        return isAchieved;
    }

    public void setAchieved(boolean achieved) {
        isAchieved = achieved;
    }

    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }

    public void setAchievedAt(LocalDateTime achievedAt) {
        this.achievedAt = achievedAt;
    }
}
