package com.chipin.chipin.dto;

import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Summary response DTO representing high-level budget status, designed for multi-budget dashboard views.
 */
public class BudgetSummaryResponseDTO {

    private Long id;
    private String groupId;
    private String name;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalLimit;
    private BudgetStatus status;
    private BigDecimal totalAllocatedAmount;
    private BigDecimal unallocatedAmount;
    private BigDecimal totalSpent;
    private boolean isOverrun;
    private BigDecimal overrunAmount;
    private int categoryCount;

    public BudgetSummaryResponseDTO() {
    }

    public BudgetSummaryResponseDTO(Long id, String groupId, String name,
                                    LocalDate periodStart, LocalDate periodEnd,
                                    BigDecimal totalLimit, BudgetStatus status,
                                    BigDecimal totalAllocatedAmount, BigDecimal unallocatedAmount,
                                    BigDecimal totalSpent, boolean isOverrun,
                                    BigDecimal overrunAmount, int categoryCount) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalLimit = totalLimit;
        this.status = status;
        this.totalAllocatedAmount = totalAllocatedAmount;
        this.unallocatedAmount = unallocatedAmount;
        this.totalSpent = totalSpent;
        this.isOverrun = isOverrun;
        this.overrunAmount = overrunAmount;
        this.categoryCount = categoryCount;
    }

    /**
     * Maps a GroupBudget entity and calculated metrics into a compact BudgetSummaryResponseDTO.
     * Prevents lazy-loading issues by taking pre-calculated values as parameters.
     *
     * @param budget the GroupBudget entity
     * @param totalAllocated sum of allocated category limits
     * @param unallocated remaining unallocated budget limit
     * @param totalSpent total active expenditures in this budget
     * @param isOverrun whether budget spending exceeds totalLimit
     * @param overrunAmount amount exceeding totalLimit, or ZERO
     * @param categoryCount number of allocated categories
     * @return the mapped BudgetSummaryResponseDTO
     */
    public static BudgetSummaryResponseDTO fromEntity(GroupBudget budget, BigDecimal totalAllocated,
                                                      BigDecimal unallocated, BigDecimal totalSpent,
                                                      boolean isOverrun, BigDecimal overrunAmount,
                                                      int categoryCount) {
        if (budget == null) {
            return null;
        }

        BigDecimal limit = budget.getTotalLimit() != null ? budget.getTotalLimit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal spent = totalSpent != null ? totalSpent.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocated = totalAllocated != null ? totalAllocated.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal unalloc = unallocated != null ? unallocated.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal overrun = overrunAmount != null ? overrunAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new BudgetSummaryResponseDTO(
                budget.getId(),
                budget.getGroupId(),
                budget.getName(),
                budget.getPeriodStart(),
                budget.getPeriodEnd(),
                limit,
                budget.getStatus(),
                allocated,
                unalloc,
                spent,
                isOverrun,
                overrun,
                categoryCount
        );
    }

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

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAllocatedAmount() {
        return totalAllocatedAmount;
    }

    public void setTotalAllocatedAmount(BigDecimal totalAllocatedAmount) {
        this.totalAllocatedAmount = totalAllocatedAmount;
    }

    public BigDecimal getUnallocatedAmount() {
        return unallocatedAmount;
    }

    public void setUnallocatedAmount(BigDecimal unallocatedAmount) {
        this.unallocatedAmount = unallocatedAmount;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
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

    public int getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(int categoryCount) {
        this.categoryCount = categoryCount;
    }
}
