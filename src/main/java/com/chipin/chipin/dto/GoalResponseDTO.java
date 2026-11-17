package com.chipin.chipin.dto;

import com.chipin.chipin.model.GoalStatus;
import com.chipin.chipin.model.Milestone;
import com.chipin.chipin.model.SavingsGoal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO representing a complete SavingsGoal view, including financial summaries,
 * progress percentage, effective lifecycle status, and milestone progression checkpoints.
 */
public class GoalResponseDTO {

    private Long id;
    private String groupId;
    private String creatorId;
    private String title;
    private String description;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal progressPercentage;
    private GoalStatus status;
    private LocalDate deadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MilestoneResponseDTO> milestones = new ArrayList<>();

    public GoalResponseDTO() {
    }

    public GoalResponseDTO(Long id, String groupId, String creatorId, String title, String description,
                           BigDecimal targetAmount, BigDecimal currentAmount, BigDecimal remainingAmount,
                           BigDecimal progressPercentage, GoalStatus status, LocalDate deadline,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           List<MilestoneResponseDTO> milestones) {
        this.id = id;
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.remainingAmount = remainingAmount;
        this.progressPercentage = progressPercentage;
        this.status = status;
        this.deadline = deadline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.milestones = milestones != null ? milestones : new ArrayList<>();
    }

    /**
     * Maps a SavingsGoal entity to a GoalResponseDTO, using explicitly provided MilestoneResponseDTO list.
     * Prevents unexpected lazy-loading database queries outside transaction boundaries.
     *
     * @param goal the savings goal entity
     * @param milestoneDTOs the milestone response DTOs
     * @return the mapped GoalResponseDTO, or null if goal is null
     */
    public static GoalResponseDTO fromEntity(SavingsGoal goal, List<MilestoneResponseDTO> milestoneDTOs) {
        if (goal == null) {
            return null;
        }

        GoalResponseDTO dto = new GoalResponseDTO();
        dto.setId(goal.getId());
        dto.setGroupId(goal.getGroupId());
        dto.setCreatorId(goal.getCreatorId());
        dto.setTitle(goal.getTitle());
        dto.setDescription(goal.getDescription());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setCurrentAmount(goal.getCurrentAmount());
        dto.setRemainingAmount(calculateRemainingAmount(goal.getTargetAmount(), goal.getCurrentAmount()));
        dto.setProgressPercentage(calculateProgressPercentage(goal.getCurrentAmount(), goal.getTargetAmount()));
        dto.setStatus(determineEffectiveStatus(goal.getStatus(), goal.getDeadline()));
        dto.setDeadline(goal.getDeadline());
        dto.setCreatedAt(goal.getCreatedAt());
        dto.setUpdatedAt(goal.getUpdatedAt());
        dto.setMilestones(milestoneDTOs != null ? milestoneDTOs : Collections.emptyList());
        return dto;
    }

    /**
     * Maps a SavingsGoal entity to a GoalResponseDTO with milestone entities passed explicitly from service.
     *
     * @param goal the savings goal entity
     * @param milestones the milestone entities
     * @return the mapped GoalResponseDTO
     */
    public static GoalResponseDTO fromEntityWithMilestones(SavingsGoal goal, Collection<Milestone> milestones) {
        if (goal == null) {
            return null;
        }
        List<MilestoneResponseDTO> milestoneDTOs = milestones != null
                ? milestones.stream().map(MilestoneResponseDTO::fromEntity).collect(Collectors.toList())
                : Collections.emptyList();
        return fromEntity(goal, milestoneDTOs);
    }

    /**
     * Maps a SavingsGoal entity to a GoalResponseDTO, safely mapping attached milestones if available.
     *
     * @param goal the savings goal entity
     * @return the mapped GoalResponseDTO
     */
    public static GoalResponseDTO fromEntity(SavingsGoal goal) {
        if (goal == null) {
            return null;
        }
        List<MilestoneResponseDTO> milestoneDTOs = new ArrayList<>();
        try {
            if (goal.getMilestones() != null) {
                for (Milestone m : goal.getMilestones()) {
                    milestoneDTOs.add(MilestoneResponseDTO.fromEntity(m));
                }
            }
        } catch (Exception ignored) {
            // Uninitialized proxy or detached entity; return empty milestone list
        }
        return fromEntity(goal, milestoneDTOs);
    }

    /**
     * Calculates the remaining amount needed to achieve the target amount.
     * Always returns scale 2.
     *
     * @param targetAmount target amount
     * @param currentAmount current amount saved
     * @return remaining amount, or 0.00 if already reached or exceeded
     */
    public static BigDecimal calculateRemainingAmount(BigDecimal targetAmount, BigDecimal currentAmount) {
        if (targetAmount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal current = currentAmount != null ? currentAmount : BigDecimal.ZERO;
        BigDecimal remaining = targetAmount.subtract(current);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return remaining.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates progress percentage using strict BigDecimal arithmetic with RoundingMode.HALF_UP.
     * Formula: (currentAmount * 100) / targetAmount, formatted to 2 decimal places.
     *
     * @param currentAmount current saved amount
     * @param targetAmount target goal amount
     * @return progress percentage (e.g. 25.00, 50.00, 100.00)
     */
    public static BigDecimal calculateProgressPercentage(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progress = currentAmount
                .multiply(new BigDecimal("100"))
                .divide(targetAmount, 2, RoundingMode.HALF_UP);

        if (progress.compareTo(new BigDecimal("100.00")) > 0) {
            return new BigDecimal("100.00");
        }
        return progress;
    }

    /**
     * Determines the effective lifecycle status of a goal.
     * If an ACTIVE goal has passed its deadline, it is presented as OVERDUE without requiring
     * an immediate database write.
     * COMPLETED goals remain COMPLETED even if their deadline is in the past.
     *
     * @param status the persisted status
     * @param deadline the goal deadline
     * @return effective status
     */
    public static GoalStatus determineEffectiveStatus(GoalStatus status, LocalDate deadline) {
        if (status == GoalStatus.COMPLETED) {
            return GoalStatus.COMPLETED;
        }
        if (status == GoalStatus.ACTIVE && deadline != null && LocalDate.now().isAfter(deadline)) {
            return GoalStatus.OVERDUE;
        }
        return status != null ? status : GoalStatus.ACTIVE;
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

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(BigDecimal progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MilestoneResponseDTO> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<MilestoneResponseDTO> milestones) {
        this.milestones = milestones;
    }
}
