package com.chipin.chipin.repository;

import com.chipin.chipin.model.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for BudgetCategory entity in Feature 11.
 * Provides category querying and uniqueness verification for budget allocations.
 */
@Repository
public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, Long> {

    /**
     * Finds all spending categories belonging to a specific budget.
     *
     * @param budgetId the budget primary key
     * @return list of allocated categories
     */
    List<BudgetCategory> findByBudgetId(Long budgetId);

    /**
     * Finds all spending categories for a budget sorted alphabetically by name.
     *
     * @param budgetId the budget primary key
     * @return alphabetically ordered categories
     */
    List<BudgetCategory> findByBudgetIdOrderByNameAsc(Long budgetId);

    /**
     * Finds a category within a budget by its normalized (trimmed, lowercase) name.
     * Used to prevent duplicate category names within the same budget.
     *
     * @param budgetId the budget primary key
     * @param normalizedName the normalized category name
     * @return Optional containing the category if found
     */
    Optional<BudgetCategory> findByBudgetIdAndNormalizedName(Long budgetId, String normalizedName);

    /**
     * Checks if a category with the normalized name already exists within a budget.
     *
     * @param budgetId the budget primary key
     * @param normalizedName the normalized category name
     * @return true if a duplicate name exists, false otherwise
     */
    boolean existsByBudgetIdAndNormalizedName(Long budgetId, String normalizedName);
}
