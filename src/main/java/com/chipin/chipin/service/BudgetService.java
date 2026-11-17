package com.chipin.chipin.service;

import com.chipin.chipin.exception.BudgetNotFoundException;
import com.chipin.chipin.exception.CategoryNotFoundException;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;
import com.chipin.chipin.repository.BudgetCategoryRepository;
import com.chipin.chipin.repository.GroupBudgetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Service managing core business logic for GroupBudget and BudgetCategory allocations.
 * Enforces financial invariants, category allocation limits, and status lifecycle transitions.
 */
@Service
public class BudgetService {

    private final GroupBudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final EntityManager entityManager;

    public BudgetService(GroupBudgetRepository budgetRepository,
                         BudgetCategoryRepository categoryRepository,
                         EntityManager entityManager) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
    }

    private GroupBudget findAndLockBudget(Long budgetId) {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        GroupBudget budget = entityManager.find(GroupBudget.class, budgetId, LockModeType.PESSIMISTIC_WRITE);
        if (budget == null) {
            throw new BudgetNotFoundException(budgetId);
        }
        return budget;
    }

    // ==========================================
    // 1. Budget Creation & Retrieval
    // ==========================================

    /**
     * Creates a new GroupBudget with ACTIVE status.
     *
     * @param groupId the group identifier (required)
     * @param creatorId the creator member identifier (required)
     * @param name the budget name (1-100 characters, required)
     * @param periodStart start date of the budget period (required)
     * @param periodEnd end date of the budget period (required, must be >= periodStart)
     * @param totalLimit total monetary spending cap (must be strictly > 0)
     * @return the persisted GroupBudget entity
     */
    @Transactional
    public GroupBudget createBudget(String groupId, String creatorId, String name,
                                    LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit) {
        validateBudgetInputs(groupId, creatorId, name, periodStart, periodEnd, totalLimit);

        GroupBudget budget = new GroupBudget(
                groupId.trim(),
                creatorId.trim(),
                name.trim(),
                periodStart,
                periodEnd,
                totalLimit
        );
        budget.setStatus(BudgetStatus.ACTIVE);

        return budgetRepository.save(budget);
    }

    /**
     * Retrieves a single GroupBudget by its ID.
     *
     * @param id the budget primary key
     * @return the GroupBudget entity
     * @throws IllegalArgumentException if id is null
     * @throws BudgetNotFoundException if no budget exists with the given ID
     */
    @Transactional(readOnly = true)
    public GroupBudget getBudgetById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        return budgetRepository.findById(id)
                .orElseThrow(() -> new BudgetNotFoundException(id));
    }

    /**
     * Retrieves all budgets created for a specific group.
     *
     * @param groupId the group identifier
     * @return list of budgets belonging to the group
     * @throws IllegalArgumentException if groupId is null or blank
     */
    @Transactional(readOnly = true)
    public List<GroupBudget> getBudgetsByGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID is required and cannot be blank");
        }
        return budgetRepository.findByGroupId(groupId.trim());
    }

    /**
     * Retrieves all budgets for a group filtered by lifecycle status.
     *
     * @param groupId the group identifier
     * @param status the budget status (ACTIVE, EXCEEDED, ARCHIVED)
     * @return list of matching budgets
     */
    @Transactional(readOnly = true)
    public List<GroupBudget> getBudgetsByGroupIdAndStatus(String groupId, BudgetStatus status) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID is required and cannot be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("Budget status is required");
        }
        return budgetRepository.findByGroupIdAndStatus(groupId.trim(), status);
    }

    /**
     * Retrieves all budgets for a group ordered by start date (newest first).
     *
     * @param groupId the group identifier
     * @return chronologically ordered list of budgets
     */
    @Transactional(readOnly = true)
    public List<GroupBudget> getBudgetsByGroupIdOrdered(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID is required and cannot be blank");
        }
        return budgetRepository.findByGroupIdOrderByPeriodStartDesc(groupId.trim());
    }

    // ==========================================
    // 2. Budget Updates & Safe Modification
    // ==========================================

    /**
     * Updates details of an existing budget.
     * Enforces the critical rule that reducing totalLimit cannot make existing category allocations invalid.
     *
     * @param id the budget ID
     * @param name new budget name (1-100 characters)
     * @param periodStart new period start date
     * @param periodEnd new period end date
     * @param totalLimit new total spending limit
     * @return the updated GroupBudget entity
     */
    @Transactional
    public GroupBudget updateBudget(Long id, String name,
                                    LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit) {
        GroupBudget budget = findAndLockBudget(id);

        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify an archived budget");
        }

        validatePeriodAndLimit(name, periodStart, periodEnd, totalLimit);

        // Invariant: New totalLimit must be >= total existing category allocations
        BigDecimal totalAllocated = calculateTotalAllocatedAmount(id);
        if (totalLimit.compareTo(totalAllocated) < 0) {
            throw new IllegalArgumentException(
                    "New total budget limit (" + totalLimit + ") cannot be less than total category allocations (" + totalAllocated + ")"
            );
        }

        budget.setName(name.trim());
        budget.setPeriodStart(periodStart);
        budget.setPeriodEnd(periodEnd);
        budget.setTotalLimit(totalLimit);

        return budgetRepository.saveAndFlush(budget);
    }

    // ==========================================
    // 3. Status Lifecycle Management
    // ==========================================

    /**
     * Transitions the budget to a new status according to valid lifecycle rules.
     *
     * @param id the budget ID
     * @param newStatus the target BudgetStatus
     * @return the updated GroupBudget
     */
    @Transactional
    public GroupBudget updateBudgetStatus(Long id, BudgetStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status is required");
        }
        GroupBudget budget = findAndLockBudget(id);

        if (budget.getStatus() == newStatus) {
            throw new IllegalStateException("Budget is already in status " + newStatus);
        }
        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot transition an archived budget to " + newStatus);
        }

        budget.setStatus(newStatus);
        return budgetRepository.saveAndFlush(budget);
    }

    /**
     * Archives a budget. Archived budgets are preserved for historical audit trails.
     *
     * @param id the budget ID
     * @return the archived GroupBudget
     */
    @Transactional
    public GroupBudget archiveBudget(Long id) {
        return updateBudgetStatus(id, BudgetStatus.ARCHIVED);
    }

    // ==========================================
    // 4. Category Allocation & Limit Calculations
    // ==========================================

    /**
     * Calculates the total amount allocated across all categories belonging to a budget.
     *
     * @param budgetId the budget primary key
     * @return total allocated amount with scale 2
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalAllocatedAmount(Long budgetId) {
        List<BudgetCategory> categories = categoryRepository.findByBudgetId(budgetId);
        if (categories == null || categories.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return categories.stream()
                .map(BudgetCategory::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the unallocated budget amount (totalLimit - totalAllocated).
     *
     * @param budgetId the budget primary key
     * @return unallocated allowance remaining
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateUnallocatedAmount(Long budgetId) {
        GroupBudget budget = getBudgetById(budgetId);
        BigDecimal totalAllocated = calculateTotalAllocatedAmount(budgetId);
        BigDecimal unallocated = budget.getTotalLimit().subtract(totalAllocated);
        return unallocated.compareTo(BigDecimal.ZERO) < 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : unallocated.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Allocates a new spending category under a GroupBudget.
     * Prevents category allocations from exceeding the total budget limit.
     *
     * @param budgetId the budget ID
     * @param categoryName display name for the category (e.g. "Groceries")
     * @param allocatedAmount monetary amount allocated to this category
     * @return the created BudgetCategory
     */
    @Transactional
    public BudgetCategory allocateCategory(Long budgetId, String categoryName, BigDecimal allocatedAmount) {
        GroupBudget budget = findAndLockBudget(budgetId);

        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot allocate categories to an archived budget");
        }

        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        if (categoryName.trim().length() > 100) {
            throw new IllegalArgumentException("Category name cannot exceed 100 characters");
        }

        validateMonetaryAmount(allocatedAmount, "Allocated amount");

        String normalized = BudgetCategory.normalize(categoryName);
        if (categoryRepository.existsByBudgetIdAndNormalizedName(budgetId, normalized)) {
            throw new IllegalArgumentException("A category with the name '" + categoryName.trim() + "' already exists in this budget");
        }

        // Invariant: sum(categoryAllocations) <= totalLimit
        BigDecimal currentAllocated = calculateTotalAllocatedAmount(budgetId);
        BigDecimal unallocated = budget.getTotalLimit().subtract(currentAllocated);

        if (allocatedAmount.compareTo(unallocated) > 0) {
            throw new IllegalArgumentException(
                    "Category allocation (" + allocatedAmount + ") exceeds remaining unallocated budget limit (" + unallocated + ")"
            );
        }

        BudgetCategory category = new BudgetCategory(budget, categoryName.trim(), allocatedAmount);
        return categoryRepository.saveAndFlush(category);
    }

    /**
     * Updates an existing category's allocation amount while ensuring total budget limit is not exceeded.
     *
     * @param budgetId the budget ID
     * @param categoryId the category ID
     * @param newAllocatedAmount the updated allocation amount
     * @return the updated BudgetCategory
     */
    @Transactional
    public BudgetCategory updateCategoryAllocation(Long budgetId, Long categoryId, BigDecimal newAllocatedAmount) {
        GroupBudget budget = findAndLockBudget(budgetId);

        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify category allocations of an archived budget");
        }

        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!category.getBudget().getId().equals(budgetId)) {
            throw new IllegalArgumentException("Category ID " + categoryId + " does not belong to budget ID " + budgetId);
        }

        validateMonetaryAmount(newAllocatedAmount, "Allocated amount");

        // Calculate other category allocations excluding this one
        BigDecimal otherAllocations = calculateTotalAllocatedAmount(budgetId).subtract(category.getAllocatedAmount());
        BigDecimal maxAllowed = budget.getTotalLimit().subtract(otherAllocations);

        if (newAllocatedAmount.compareTo(maxAllowed) > 0) {
            throw new IllegalArgumentException(
                    "New category allocation (" + newAllocatedAmount + ") exceeds allowed limit (" + maxAllowed + ")"
            );
        }

        category.setAllocatedAmount(newAllocatedAmount);
        return categoryRepository.saveAndFlush(category);
    }

    /**
     * Retrieves all categories allocated under a budget.
     *
     * @param budgetId the budget ID
     * @return list of allocated categories
     */
    @Transactional(readOnly = true)
    public List<BudgetCategory> getCategoriesByBudgetId(Long budgetId) {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        if (!budgetRepository.existsById(budgetId)) {
            throw new BudgetNotFoundException(budgetId);
        }
        return categoryRepository.findByBudgetId(budgetId);
    }

    // ==========================================
    // 5. Validation Helpers
    // ==========================================

    private void validateBudgetInputs(String groupId, String creatorId, String name,
                                      LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID is required and cannot be blank");
        }
        if (creatorId == null || creatorId.isBlank()) {
            throw new IllegalArgumentException("Creator ID is required and cannot be blank");
        }
        validatePeriodAndLimit(name, periodStart, periodEnd, totalLimit);
    }

    private void validatePeriodAndLimit(String name, LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Budget name is required and cannot be blank");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Budget name cannot exceed 100 characters");
        }
        if (periodStart == null) {
            throw new IllegalArgumentException("Period start date is required");
        }
        if (periodEnd == null) {
            throw new IllegalArgumentException("Period end date is required");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException("Budget period start date cannot be after period end date");
        }
        validateMonetaryAmount(totalLimit, "Total limit");
    }

    private void validateMonetaryAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(fieldName + " must be strictly greater than zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException(fieldName + " cannot have more than 2 decimal places");
        }
        if (amount.precision() - amount.scale() > 13) {
            throw new IllegalArgumentException(fieldName + " cannot exceed 13 integer digits");
        }
    }
}
