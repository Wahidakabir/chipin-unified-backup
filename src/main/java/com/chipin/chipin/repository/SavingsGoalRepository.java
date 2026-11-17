package com.chipin.chipin.repository;

import com.chipin.chipin.model.GoalStatus;
import com.chipin.chipin.model.SavingsGoal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for SavingsGoal entity.
 * Provides data access operations and concurrency locking.
 */
@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    /**
     * Retrieves a SavingsGoal by its primary key with a Pessimistic Write Lock (SELECT ... FOR UPDATE).
     * Serializes concurrent transactions modifying the goal balance to prevent overfunding race conditions.
     *
     * @param id the savings goal ID
     * @return Optional containing the locked SavingsGoal, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM SavingsGoal g WHERE g.id = :id")
    Optional<SavingsGoal> findByIdForUpdate(@Param("id") Long id);

    /**
     * Finds all savings goals associated with a specific group.
     *
     * @param groupId the group identifier
     * @return list of goals for the group
     */
    List<SavingsGoal> findByGroupId(String groupId);

    /**
     * Finds all savings goals for a group filtered by status (ACTIVE, COMPLETED, OVERDUE).
     *
     * @param groupId the group identifier
     * @param status the goal status
     * @return list of matching goals
     */
    List<SavingsGoal> findByGroupIdAndStatus(String groupId, GoalStatus status);

    /**
     * Finds active goals whose deadline has passed (for overdue status evaluation).
     *
     * @param status expected status (e.g. ACTIVE)
     * @param date current comparison date
     * @return list of goals needing status update
     */
    List<SavingsGoal> findByStatusAndDeadlineBefore(GoalStatus status, LocalDate date);
}
