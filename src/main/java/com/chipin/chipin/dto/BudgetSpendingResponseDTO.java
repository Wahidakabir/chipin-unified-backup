package com.chipin.chipin.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed response DTO summarizing real-time spending calculations and overrun analysis
 * for a budget and all its categories.
 */
public class BudgetSpendingResponseDTO {

    private Long budgetId;
    private BigDecimal totalLimit;
    private BigDecimal totalAllocated;
    private BigDecimal unallocated;
    private BigDecimal totalSpent;
    private BigDecimal remainingLimit;
    private boolean isBudgetOverrun;
    private BigDecimal budgetOverrunAmount;
    private boolean hasAnyCategoryOverrun;
    private List<CategoryResponseDTO> categories = new ArrayList<>();

    public BudgetSpendingResponseDTO() {
    }

    public BudgetSpendingResponseDTO(Long budgetId, BigDecimal totalLimit, BigDecimal totalAllocated,
                                     BigDecimal unallocated, BigDecimal totalSpent,
                                     BigDecimal remainingLimit, boolean isBudgetOverrun,
                                     BigDecimal budgetOverrunAmount, boolean hasAnyCategoryOverrun,
                                     List<CategoryResponseDTO> categories) {
        this.budgetId = budgetId;
        this.totalLimit = totalLimit;
        this.totalAllocated = totalAllocated;
        this.unallocated = unallocated;
        this.totalSpent = totalSpent;
        this.remainingLimit = remainingLimit;
        this.isBudgetOverrun = isBudgetOverrun;
        this.budgetOverrunAmount = budgetOverrunAmount;
        this.hasAnyCategoryOverrun = hasAnyCategoryOverrun;
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public BigDecimal getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(BigDecimal totalLimit) {
        this.totalLimit = totalLimit;
    }

    public BigDecimal getTotalAllocated() {
        return totalAllocated;
    }

    public void setTotalAllocated(BigDecimal totalAllocated) {
        this.totalAllocated = totalAllocated;
    }

    public BigDecimal getUnallocated() {
        return unallocated;
    }

    public void setUnallocated(BigDecimal unallocated) {
        this.unallocated = unallocated;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public BigDecimal getRemainingLimit() {
        return remainingLimit;
    }

    public void setRemainingLimit(BigDecimal remainingLimit) {
        this.remainingLimit = remainingLimit;
    }

    public boolean isBudgetOverrun() {
        return isBudgetOverrun;
    }

    public void setBudgetOverrun(boolean budgetOverrun) {
        isBudgetOverrun = budgetOverrun;
    }

    public BigDecimal getBudgetOverrunAmount() {
        return budgetOverrunAmount;
    }

    public void setBudgetOverrunAmount(BigDecimal budgetOverrunAmount) {
        this.budgetOverrunAmount = budgetOverrunAmount;
    }

    public boolean isHasAnyCategoryOverrun() {
        return hasAnyCategoryOverrun;
    }

    public void setHasAnyCategoryOverrun(boolean hasAnyCategoryOverrun) {
        this.hasAnyCategoryOverrun = hasAnyCategoryOverrun;
    }

    public List<CategoryResponseDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryResponseDTO> categories) {
        this.categories = categories;
    }
}
