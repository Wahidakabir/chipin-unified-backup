package com.chipin.chipin.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a new savings goal in Chipin.
 */
public class CreateGoalRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Creator ID is required")
    private String creatorId;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title cannot exceed 100 characters")
    private String title;

    private String description;

    @NotNull(message = "Target amount is required")
    @Positive(message = "Target amount must be strictly greater than zero")
    private BigDecimal targetAmount;

    @NotNull(message = "Deadline is required")
    @FutureOrPresent(message = "Deadline cannot be in the past")
    private LocalDate deadline;

    public CreateGoalRequest() {
    }

    public CreateGoalRequest(String groupId, String creatorId, String title, String description,
                             BigDecimal targetAmount, LocalDate deadline) {
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
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

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
