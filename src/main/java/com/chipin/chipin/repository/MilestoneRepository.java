package com.chipin.chipin.repository;

import com.chipin.chipin.model.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Milestone tracking entities.
 * Supports checkpoint tracking and milestone status queries.
 */
@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    /**
     * Retrieves all milestone checkpoints for a goal in ascending percentage order (25%, 50%, 75%, 100%).
     *
     * @param goalId the savings goal ID
     * @return list of milestones ordered by percentage
     */
    List<Milestone> findByGoalIdOrderByPercentageAsc(Long goalId);

    /**
     * Finds a specific milestone checkpoint by goal ID and percentage.
     *
     * @param goalId the savings goal ID
     * @param percentage the percentage checkpoint (e.g. 50)
     * @return Optional containing the milestone if found
     */
    Optional<Milestone> findByGoalIdAndPercentage(Long goalId, Integer percentage);

    /**
     * Retrieves all pending (unachieved) milestones for a goal, ordered ascending.
     * Used when checking which milestones should be unlocked after a contribution.
     *
     * @param goalId the savings goal ID
     * @return list of pending milestones
     */
    List<Milestone> findByGoalIdAndIsAchievedFalseOrderByPercentageAsc(Long goalId);

    /**
     * Retrieves all achieved milestones for a goal, ordered ascending.
     *
     * @param goalId the savings goal ID
     * @return list of achieved milestones
     */
    List<Milestone> findByGoalIdAndIsAchievedTrueOrderByPercentageAsc(Long goalId);

    /**
     * Retrieves all milestone checkpoints for a collection of goals in ascending percentage order.
     * Used for bulk-loading milestones to avoid N+1 queries when fetching multiple goals.
     *
     * @param goalIds collection of savings goal IDs
     * @return list of milestones ordered by goal ID and percentage
     */
    @Query("SELECT m FROM Milestone m WHERE m.goal.id IN :goalIds ORDER BY m.goal.id ASC, m.percentage ASC")
    List<Milestone> findByGoalIdIn(@Param("goalIds") Collection<Long> goalIds);
}
