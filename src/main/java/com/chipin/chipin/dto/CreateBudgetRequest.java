package com.chipin.chipin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for creating a new group budget in Feature 11.
 */
public class CreateBudgetRequest {

    @NotBlank(message = "Group ID is required")
    private String groupId;

    @NotBlank(message = "Creator ID is required")
    private String creatorId;

    @NotBlank(message = "Budget name is required")
    @Size(min = 1, max = 100, message = "Budget name must be between 1 and 100 characters")
    private String name;

    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;

    @NotNull(message = "Total limit is required")
    @Positive(message = "Total limit must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Total limit cannot exceed 13 integer digits and 2 decimal places")
    private BigDecimal totalLimit;

    public CreateBudgetRequest() {
    }

    public CreateBudgetRequest(String groupId, String creatorId, String name,
                               LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit) {
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.name = name;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalLimit = totalLimit;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(BigDecimal totalLimit) {
        this.totalLimit = totalLimit;
    }
}
