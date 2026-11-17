package com.chipin.chipin.service;

import com.chipin.chipin.exception.BudgetNotFoundException;
import com.chipin.chipin.exception.CategoryNotFoundException;
import com.chipin.chipin.exception.DuplicateExternalExpenseException;
import com.chipin.chipin.exception.ExpenseNotFoundException;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetExpense;
import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.ExpenseStatus;
import com.chipin.chipin.model.GroupBudget;
import com.chipin.chipin.repository.BudgetCategoryRepository;
import com.chipin.chipin.repository.BudgetExpenseRepository;
import com.chipin.chipin.repository.GroupBudgetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Service managing financial expense transactions under budget categories.
 * Handles expense recording, lifecycle modification, audit voiding, spending calculations,
 * category and overall budget overrun detection, and external receipt split integration.
 */
@Service
public class BudgetExpenseService {

    private final BudgetExpenseRepository expenseRepository;
    private final BudgetCategoryRepository categoryRepository;
    private final GroupBudgetRepository budgetRepository;
    private final EntityManager entityManager;

    public BudgetExpenseService(BudgetExpenseRepository expenseRepository,
                                BudgetCategoryRepository categoryRepository,
                                GroupBudgetRepository budgetRepository,
                                EntityManager entityManager) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
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

    private boolean isExternalExpenseDuplicateViolation(DataIntegrityViolationException ex, Long externalExpenseId) {
        if (externalExpenseId == null) {
            return false;
        }
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraint = cve.getConstraintName();
                if (constraint != null && constraint.toLowerCase().contains("uk_budget_expenses_external_expense_id")) {
                    return true;
                }
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("uk_budget_expenses_external_expense_id") ||
                    (lower.contains("external_expense_id") && (lower.contains("unique") || lower.contains("duplicate key")))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    // ==========================================
    // 1. Expense Recording
    // ==========================================

    /**
     * Records a new expense against a budget category.
     * Overruns are detected and recorded, updating budget status to EXCEEDED if limits are passed,
     * without rejecting legitimate expenditures.
     *
     * @param budgetId the parent budget ID
     * @param categoryId the target category ID
     * @param payerId the member identifier who paid
     * @param payerName the member display name
     * @param amount the exact monetary amount (must be strictly > 0)
     * @param description the description of the purchase
     * @param expenseDate the date the expense occurred
     * @param externalExpenseId optional external receipt/itemized expense ID (must be unique when present)
     * @return the saved BudgetExpense entity
     */
    @Transactional
    public BudgetExpense recordExpense(Long budgetId, Long categoryId, String payerId, String payerName,
                                       BigDecimal amount, String description, LocalDate expenseDate,
                                       Long externalExpenseId) {
        validateExpenseInputs(budgetId, categoryId, payerId, payerName, amount, description, expenseDate);

        // Pessimistic Row Lock on parent budget to serialize concurrent expense postings
        GroupBudget budget = findAndLockBudget(budgetId);

        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot record an expense against an archived budget");
        }

        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        if (!category.getBudget().getId().equals(budgetId)) {
            throw new IllegalArgumentException("Category ID " + categoryId + " does not belong to budget ID " + budgetId);
        }

        // Prevent duplicate external expense linking (e.g. from Feature 2 Receipt Splitting)
        if (externalExpenseId != null) {
            if (expenseRepository.findByExternalExpenseId(externalExpenseId).isPresent()) {
                throw new DuplicateExternalExpenseException(externalExpenseId);
            }
        }

        BudgetExpense expense = new BudgetExpense(
                category,
                payerId.trim(),
                payerName.trim(),
                amount,
                description.trim(),
                expenseDate,
                payerId.trim()
        );
        expense.setExternalExpenseId(externalExpenseId);
        expense.setStatus(ExpenseStatus.RECORDED);

        BudgetExpense savedExpense;
        try {
            savedExpense = expenseRepository.saveAndFlush(expense);
        } catch (DataIntegrityViolationException ex) {
            if (isExternalExpenseDuplicateViolation(ex, externalExpenseId)) {
                throw new DuplicateExternalExpenseException(externalExpenseId);
            }
            throw ex;
        }

        // Re-evaluate overrun status across budget and categories
        evaluateBudgetStatus(budget);

        return savedExpense;
    }

    /**
     * Overload for direct expense recording without an external expense reference.
     */
    @Transactional
    public BudgetExpense recordExpense(Long budgetId, Long categoryId, String payerId, String payerName,
                                       BigDecimal amount, String description, LocalDate expenseDate) {
        return recordExpense(budgetId, categoryId, payerId, payerName, amount, description, expenseDate, null);
    }

    // ==========================================
    // 2. Expense Retrieval
    // ==========================================

    /**
     * Retrieves an individual expense by its ID.
     *
     * @param id the expense ID
     * @return the BudgetExpense entity
     * @throws ExpenseNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public BudgetExpense getExpenseById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Expense ID is required");
        }
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    /**
     * Retrieves all expenses recorded under a budget across all categories (newest first).
     *
     * @param budgetId the budget primary key
     * @return chronologically ordered list of expenses
     */
    @Transactional(readOnly = true)
    public List<BudgetExpense> getExpensesByBudgetId(Long budgetId) {
        validateBudgetExists(budgetId);
        return expenseRepository.findByCategoryBudgetIdOrderByExpenseDateDesc(budgetId);
    }

    /**
     * Retrieves expenses for a budget filtered by transaction status (e.g. RECORDED or VOIDED).
     *
     * @param budgetId the budget primary key
     * @param status the transaction status
     * @return list of matching expenses
     */
    @Transactional(readOnly = true)
    public List<BudgetExpense> getExpensesByBudgetIdAndStatus(Long budgetId, ExpenseStatus status) {
        validateBudgetExists(budgetId);
        if (status == null) {
            throw new IllegalArgumentException("Expense status is required");
        }
        return expenseRepository.findByCategoryBudgetIdAndStatus(budgetId, status);
    }

    /**
     * Retrieves all expenses recorded for a specific category.
     *
     * @param categoryId the category primary key
     * @return list of expenses in the category
     */
    @Transactional(readOnly = true)
    public List<BudgetExpense> getExpensesByCategoryId(Long categoryId) {
        validateCategoryExists(categoryId);
        return expenseRepository.findByCategoryId(categoryId);
    }

    /**
     * Retrieves expenses for a category filtered by transaction status.
     *
     * @param categoryId the category primary key
     * @param status the transaction status
     * @return list of matching expenses
     */
    @Transactional(readOnly = true)
    public List<BudgetExpense> getExpensesByCategoryIdAndStatus(Long categoryId, ExpenseStatus status) {
        validateCategoryExists(categoryId);
        if (status == null) {
            throw new IllegalArgumentException("Expense status is required");
        }
        return expenseRepository.findByCategoryIdAndStatus(categoryId, status);
    }

    // ==========================================
    // 3. Expense Lifecycle Updates & Voiding
    // ==========================================

    /**
     * Updates an existing expense where permitted by its lifecycle.
     * Modifications to VOIDED expenses or expenses belonging to ARCHIVED budgets are strictly rejected.
     *
     * @param expenseId the expense ID to update
     * @param newAmount updated monetary amount
     * @param newDescription updated purchase description
     * @param newExpenseDate updated date
     * @param newPayerName updated payer display name
     * @param newCategoryId updated category ID (must belong to the same budget)
     * @return the updated BudgetExpense
     */
    @Transactional
    public BudgetExpense updateExpense(Long expenseId, BigDecimal newAmount, String newDescription,
                                       LocalDate newExpenseDate, String newPayerName, Long newCategoryId) {
        if (expenseId == null) {
            throw new IllegalArgumentException("Expense ID is required");
        }

        BudgetExpense expense = getExpenseById(expenseId);
        Long budgetId = expense.getCategory().getBudget().getId();

        // Lock parent budget row to serialize concurrent modifications affecting budget/category spending
        GroupBudget budget = findAndLockBudget(budgetId);

        if (expense.getStatus() == ExpenseStatus.VOIDED) {
            throw new IllegalStateException("Cannot modify a voided expense");
        }

        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify an expense belonging to an archived budget");
        }

        // Change category if requested (must belong to the same budget)
        if (newCategoryId != null && !newCategoryId.equals(expense.getCategory().getId())) {
            BudgetCategory newCategory = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new CategoryNotFoundException(newCategoryId));
            if (!newCategory.getBudget().getId().equals(budgetId)) {
                throw new IllegalArgumentException("New category must belong to the same budget");
            }
            expense.setCategory(newCategory);
        }

        if (newAmount != null) {
            validateMonetaryAmount(newAmount, "Expense amount");
            expense.setAmount(newAmount);
        }

        if (newDescription != null) {
            if (newDescription.trim().isEmpty()) {
                throw new IllegalArgumentException("Expense description cannot be blank");
            }
            if (newDescription.trim().length() > 255) {
                throw new IllegalArgumentException("Expense description cannot exceed 255 characters");
            }
            expense.setDescription(newDescription.trim());
        }

        if (newExpenseDate != null) {
            expense.setExpenseDate(newExpenseDate);
        }

        if (newPayerName != null) {
            if (newPayerName.trim().isEmpty()) {
                throw new IllegalArgumentException("Payer name cannot be blank");
            }
            expense.setPayerName(newPayerName.trim());
        }

        BudgetExpense updated = expenseRepository.saveAndFlush(expense);
        evaluateBudgetStatus(budget);
        return updated;
    }

    /**
     * Voids an expense with a mandatory cancellation reason.
     * The record is NOT deleted from the database; it transitions to VOIDED and is excluded
     * from all active spending and overrun calculations.
     *
     * @param expenseId the expense ID to void
     * @param reason the reason for voiding (cannot be null or blank)
     * @return the voided BudgetExpense
     */
    @Transactional
    public BudgetExpense voidExpense(Long expenseId, String reason) {
        if (expenseId == null) {
            throw new IllegalArgumentException("Expense ID is required");
        }

        BudgetExpense expense = getExpenseById(expenseId);
        Long budgetId = expense.getCategory().getBudget().getId();

        // Lock parent budget row to serialize concurrent modifications affecting budget/category spending
        GroupBudget budget = findAndLockBudget(budgetId);

        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot void an expense belonging to an archived budget");
        }

        expense.voidExpense(reason);
        BudgetExpense voided = expenseRepository.saveAndFlush(expense);

        // Re-evaluate budget status: if previously EXCEEDED and voiding brought it back within bounds, restore to ACTIVE
        evaluateBudgetStatus(budget);

        return voided;
    }

    // ==========================================
    // 4. Spending & Remaining Calculations
    // ==========================================

    /**
     * Calculates the total active spending for a category.
     * Excludes all VOIDED expenses to preserve financial accuracy.
     *
     * @param categoryId the category primary key
     * @return sum of active expenses with scale 2
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCategorySpending(Long categoryId) {
        validateCategoryExists(categoryId);
        List<BudgetExpense> expenses = expenseRepository.findByCategoryIdAndStatus(categoryId, ExpenseStatus.RECORDED);
        if (expenses == null || expenses.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return expenses.stream()
                .map(BudgetExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the total active spending across all categories in a budget.
     * Excludes all VOIDED expenses.
     *
     * @param budgetId the budget primary key
     * @return sum of all active expenditures in the budget with scale 2
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBudgetSpending(Long budgetId) {
        validateBudgetExists(budgetId);
        List<BudgetExpense> expenses = expenseRepository.findByCategoryBudgetIdAndStatus(budgetId, ExpenseStatus.RECORDED);
        if (expenses == null || expenses.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return expenses.stream()
                .map(BudgetExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the remaining category allocation balance (allocatedAmount - categorySpent).
     * Returns negative value if the category is currently overrun.
     *
     * @param categoryId the category ID
     * @return remaining allocation balance
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRemainingCategoryAllocation(Long categoryId) {
        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        BigDecimal spent = calculateCategorySpending(categoryId);
        return category.getAllocatedAmount().subtract(spent).setScale(2, RoundingMode.HALF_UP);
    }

    // ==========================================
    // 5. Overrun Detection
    // ==========================================

    /**
     * Determines whether a category has exceeded its allocated limit.
     *
     * @param categoryId the category primary key
     * @return true if active spending > allocated amount
     */
    @Transactional(readOnly = true)
    public boolean isCategoryOverrun(Long categoryId) {
        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        BigDecimal spent = calculateCategorySpending(categoryId);
        return spent.compareTo(category.getAllocatedAmount()) > 0;
    }

    /**
     * Calculates the overrun amount for a category.
     *
     * @param categoryId the category primary key
     * @return overrun excess amount or ZERO if within limit
     */
    @Transactional(readOnly = true)
    public BigDecimal getCategoryOverrunAmount(Long categoryId) {
        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        BigDecimal spent = calculateCategorySpending(categoryId);
        BigDecimal excess = spent.subtract(category.getAllocatedAmount());
        return excess.compareTo(BigDecimal.ZERO) > 0
                ? excess.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Determines whether the total spending across a budget has exceeded its overall total limit.
     *
     * @param budgetId the budget primary key
     * @return true if total active spending > budget totalLimit
     */
    @Transactional(readOnly = true)
    public boolean isBudgetOverrun(Long budgetId) {
        GroupBudget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
        BigDecimal spent = calculateTotalBudgetSpending(budgetId);
        return spent.compareTo(budget.getTotalLimit()) > 0;
    }

    /**
     * Calculates the overall overrun amount for a budget.
     *
     * @param budgetId the budget primary key
     * @return overrun excess amount or ZERO if within limit
     */
    @Transactional(readOnly = true)
    public BigDecimal getBudgetOverrunAmount(Long budgetId) {
        GroupBudget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
        BigDecimal spent = calculateTotalBudgetSpending(budgetId);
        BigDecimal excess = spent.subtract(budget.getTotalLimit());
        return excess.compareTo(BigDecimal.ZERO) > 0
                ? excess.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if any category within a budget has an active overrun.
     *
     * @param budgetId the budget primary key
     * @return true if one or more categories have spent > allocated
     */
    @Transactional(readOnly = true)
    public boolean hasAnyCategoryOverrun(Long budgetId) {
        List<BudgetCategory> categories = categoryRepository.findByBudgetId(budgetId);
        if (categories == null || categories.isEmpty()) {
            return false;
        }
        return categories.stream().anyMatch(c -> isCategoryOverrun(c.getId()));
    }

    // ==========================================
    // 6. Private Helpers & Invariants
    // ==========================================

    private void evaluateBudgetStatus(GroupBudget budget) {
        if (budget.getStatus() == BudgetStatus.ARCHIVED) {
            return;
        }

        boolean budgetExceeded = isBudgetOverrun(budget.getId());
        boolean categoryExceeded = hasAnyCategoryOverrun(budget.getId());

        if (budgetExceeded || categoryExceeded) {
            if (budget.getStatus() == BudgetStatus.ACTIVE) {
                budget.setStatus(BudgetStatus.EXCEEDED);
                budgetRepository.save(budget);
            }
        } else {
            if (budget.getStatus() == BudgetStatus.EXCEEDED) {
                budget.setStatus(BudgetStatus.ACTIVE);
                budgetRepository.save(budget);
            }
        }
    }

    private void validateExpenseInputs(Long budgetId, Long categoryId, String payerId, String payerName,
                                       BigDecimal amount, String description, LocalDate expenseDate) {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required");
        }
        if (payerId == null || payerId.isBlank()) {
            throw new IllegalArgumentException("Payer ID is required and cannot be blank");
        }
        if (payerName == null || payerName.isBlank()) {
            throw new IllegalArgumentException("Payer name is required and cannot be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Expense description is required and cannot be blank");
        }
        if (description.trim().length() > 255) {
            throw new IllegalArgumentException("Expense description cannot exceed 255 characters");
        }
        if (expenseDate == null) {
            throw new IllegalArgumentException("Expense date is required");
        }
        validateMonetaryAmount(amount, "Expense amount");
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

    private void validateBudgetExists(Long budgetId) {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        if (!budgetRepository.existsById(budgetId)) {
            throw new BudgetNotFoundException(budgetId);
        }
    }

    private void validateCategoryExists(Long categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required");
        }
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }
}
