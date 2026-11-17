package com.chipin.chipin.repository;

import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for GroupBudget entity in Feature 11.
 * Provides basic data access operations for group budgets.
 */
@Repository
public interface GroupBudgetRepository extends JpaRepository<GroupBudget, Long> {

    /**
     * Finds all budgets created for a specific group.
     *
     * @param groupId the group identifier
     * @return list of budgets belonging to the group
     */
    List<GroupBudget> findByGroupId(String groupId);

    /**
     * Finds all budgets for a group filtered by status (ACTIVE, EXCEEDED, ARCHIVED).
     *
     * @param groupId the group identifier
     * @param status the budget status
     * @return list of matching budgets
     */
    List<GroupBudget> findByGroupIdAndStatus(String groupId, BudgetStatus status);

    /**
     * Finds all budgets for a group sorted by start date in descending order (newest first).
     *
     * @param groupId the group identifier
     * @return list of budgets ordered chronologically
     */
    List<GroupBudget> findByGroupIdOrderByPeriodStartDesc(String groupId);

    /**
     * Finds budgets for a group whose period covers a specific date.
     *
     * @param groupId the group identifier
     * @param date the date to check against period_start and period_end
     * @return list of budgets covering the date
     */
    List<GroupBudget> findByGroupIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqual(
            String groupId, LocalDate date, LocalDate sameDate);
}
