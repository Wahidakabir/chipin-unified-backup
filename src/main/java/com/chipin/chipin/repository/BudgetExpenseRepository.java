package com.chipin.chipin.repository;

import com.chipin.chipin.model.BudgetExpense;
import com.chipin.chipin.model.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for BudgetExpense entity in Feature 11.
 * Provides access to expenses recorded under budget categories.
 */
@Repository
public interface BudgetExpenseRepository extends JpaRepository<BudgetExpense, Long> {

    /**
     * Finds all expenses recorded under a specific category.
     *
     * @param categoryId the category primary key
     * @return list of expenses
     */
    List<BudgetExpense> findByCategoryId(Long categoryId);

    /**
     * Finds all expenses for a category filtered by transaction status (RECORDED, VOIDED).
     *
     * @param categoryId the category primary key
     * @param status the expense status
     * @return list of matching expenses
     */
    List<BudgetExpense> findByCategoryIdAndStatus(Long categoryId, ExpenseStatus status);

    /**
     * Finds all expenses associated with a budget across all its categories.
     *
     * @param budgetId the budget primary key
     * @return list of all expenses in the budget
     */
    List<BudgetExpense> findByCategoryBudgetId(Long budgetId);

    /**
     * Finds active (or voided) expenses associated with a budget across all categories.
     *
     * @param budgetId the budget primary key
     * @param status the expense status (e.g. RECORDED to calculate active spending)
     * @return list of matching expenses
     */
    List<BudgetExpense> findByCategoryBudgetIdAndStatus(Long budgetId, ExpenseStatus status);

    /**
     * Finds all expenses for a budget ordered by date in descending order (newest first).
     *
     * @param budgetId the budget primary key
     * @return chronologically ordered expenses
     */
    List<BudgetExpense> findByCategoryBudgetIdOrderByExpenseDateDesc(Long budgetId);

    /**
     * Finds all expenses paid by a specific member.
     *
     * @param payerId the member identifier
     * @return list of expenses paid by the member
     */
    List<BudgetExpense> findByPayerId(String payerId);

    /**
     * Finds an expense by its external Feature 2 expense identifier, if linked.
     *
     * @param externalExpenseId the external itemized split expense ID
     * @return Optional containing the linked budget expense
     */
    Optional<BudgetExpense> findByExternalExpenseId(Long externalExpenseId);
}
