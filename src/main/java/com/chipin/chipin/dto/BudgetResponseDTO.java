package com.chipin.chipin.dto;

import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detailed response DTO representing a GroupBudget, including allocated category allowances,
 * unallocated amount, total spending, remaining limit, overrun calculations, and allocated categories.
 */
public class BudgetResponseDTO {

    private Long id;
    private String groupId;
    private String creatorId;
    private String name;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalLimit;
    private BudgetStatus status;
    private BigDecimal totalAllocatedAmount;
    private BigDecimal unallocatedAmount;
    private BigDecimal totalSpent;
    private BigDecimal remainingLimit;
    private boolean isOverrun;
    private BigDecimal overrunAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CategoryResponseDTO> categories = new ArrayList<>();

    public BudgetResponseDTO() {
    }

    public BudgetResponseDTO(Long id, String groupId, String creatorId, String name,
                             LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit,
                             BudgetStatus status, BigDecimal totalAllocatedAmount,
                             BigDecimal unallocatedAmount, BigDecimal totalSpent,
                             BigDecimal remainingLimit, boolean isOverrun,
                             BigDecimal overrunAmount, LocalDateTime createdAt,
                             LocalDateTime updatedAt, List<CategoryResponseDTO> categories) {
        this.id = id;
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.name = name;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalLimit = totalLimit;
        this.status = status;
        this.totalAllocatedAmount = totalAllocatedAmount;
        this.unallocatedAmount = unallocatedAmount;
        this.totalSpent = totalSpent;
        this.remainingLimit = remainingLimit;
        this.isOverrun = isOverrun;
        this.overrunAmount = overrunAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    /**
     * Maps a GroupBudget entity and calculated metrics into a comprehensive BudgetResponseDTO.
     * Prevents lazy-loading exceptions by accepting pre-calculated financial metrics and categories.
     *
     * @param budget the GroupBudget entity
     * @param totalAllocated sum of allocated category limits
     * @param unallocated remaining unallocated budget limit
     * @param totalSpent total active expenditures in this budget
     * @param isOverrun whether budget spending exceeds totalLimit
     * @param overrunAmount amount exceeding totalLimit, or ZERO
     * @param categories mapped category DTOs
     * @return the mapped BudgetResponseDTO
     */
    public static BudgetResponseDTO fromEntity(GroupBudget budget, BigDecimal totalAllocated,
                                               BigDecimal unallocated, BigDecimal totalSpent,
                                               boolean isOverrun, BigDecimal overrunAmount,
                                               List<CategoryResponseDTO> categories) {
        if (budget == null) {
            return null;
        }

        BigDecimal limit = budget.getTotalLimit() != null ? budget.getTotalLimit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal spent = totalSpent != null ? totalSpent.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = limit.subtract(spent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal allocated = totalAllocated != null ? totalAllocated.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal unalloc = unallocated != null ? unallocated.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal overrun = overrunAmount != null ? overrunAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new BudgetResponseDTO(
                budget.getId(),
                budget.getGroupId(),
                budget.getCreatorId(),
                budget.getName(),
                budget.getPeriodStart(),
                budget.getPeriodEnd(),
                limit,
                budget.getStatus(),
                allocated,
                unalloc,
                spent,
                remaining,
                isOverrun,
                overrun,
                budget.getCreatedAt(),
                budget.getUpdatedAt(),
                categories != null ? categories : Collections.emptyList()
        );
    }

    /**
     * Fallback mapping for a standalone GroupBudget without computed categories or spending metrics.
     *
     * @param budget the GroupBudget entity
     * @return basic BudgetResponseDTO
     */
    public static BudgetResponseDTO fromEntity(GroupBudget budget) {
        if (budget == null) {
            return null;
        }
        return fromEntity(
                budget,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                budget.getTotalLimit() != null ? budget.getTotalLimit().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                false,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                Collections.emptyList()
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

    public BigDecimal getRemainingLimit() {
        return remainingLimit;
    }

    public void setRemainingLimit(BigDecimal remainingLimit) {
        this.remainingLimit = remainingLimit;
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

    public List<CategoryResponseDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryResponseDTO> categories) {
        this.categories = categories;
    }
}
