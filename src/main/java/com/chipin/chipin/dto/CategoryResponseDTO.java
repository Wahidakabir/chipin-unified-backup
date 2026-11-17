package com.chipin.chipin.dto;

import com.chipin.chipin.model.BudgetCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Response DTO representing a spending category under a GroupBudget,
 * including allocated allowance, current spent amount, remaining allocation, and overrun status.
 */
public class CategoryResponseDTO {

    private Long id;
    private Long budgetId;
    private String name;
    private String normalizedName;
    private BigDecimal allocatedAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAllocation;
    private boolean isOverrun;
    private BigDecimal overrunAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CategoryResponseDTO() {
    }

    public CategoryResponseDTO(Long id, Long budgetId, String name, String normalizedName,
                               BigDecimal allocatedAmount, BigDecimal spentAmount,
                               BigDecimal remainingAllocation, boolean isOverrun,
                               BigDecimal overrunAmount, LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
        this.id = id;
        this.budgetId = budgetId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.allocatedAmount = allocatedAmount;
        this.spentAmount = spentAmount;
        this.remainingAllocation = remainingAllocation;
        this.isOverrun = isOverrun;
        this.overrunAmount = overrunAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Maps a BudgetCategory entity into a CategoryResponseDTO with explicitly provided parent budgetId.
     * Guarantees lazy-loading safety without relying on defensive try-catch exception suppression.
     *
     * @param category the BudgetCategory entity
     * @param spentAmount total non-voided spending recorded under this category
     * @param budgetId explicitly provided parent budget ID
     * @return the CategoryResponseDTO, or null if category is null
     */
    public static CategoryResponseDTO fromEntity(BudgetCategory category, BigDecimal spentAmount, Long budgetId) {
        if (category == null) {
            return null;
        }

        BigDecimal spent = spentAmount != null ? spentAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocated = category.getAllocatedAmount() != null ? category.getAllocatedAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = allocated.subtract(spent).setScale(2, RoundingMode.HALF_UP);
        boolean overrun = spent.compareTo(allocated) > 0;
        BigDecimal overrunAmt = overrun ? spent.subtract(allocated).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new CategoryResponseDTO(
                category.getId(),
                budgetId,
                category.getName(),
                category.getNormalizedName(),
                allocated,
                spent,
                remaining,
                overrun,
                overrunAmt,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
    }

    public BigDecimal getRemainingAllocation() {
        return remainingAllocation;
    }

    public void setRemainingAllocation(BigDecimal remainingAllocation) {
        this.remainingAllocation = remainingAllocation;
    }

    public boolean isOverrun() {
        return isOverrun;
    }

    public void setOverrun(boolean overrun) {
        isOverrun = overrun;
    }

    public BigDecimal getOverrunAmount() {
        return overrunAmount;
    }

    public void setOverrunAmount(BigDecimal overrunAmount) {
        this.overrunAmount = overrunAmount;
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
}
