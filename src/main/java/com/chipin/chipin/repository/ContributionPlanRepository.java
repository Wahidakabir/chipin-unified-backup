package com.chipin.chipin.repository;

import com.chipin.chipin.model.ContributionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for ContributionPlan entity.
 * Handles agreed member pledges per goal.
 */
@Repository
public interface ContributionPlanRepository extends JpaRepository<ContributionPlan, Long> {

    /**
     * Retrieves all member contribution plans for a specific savings goal.
     *
     * @param goalId the savings goal ID
     * @return list of contribution plans for the goal
     */
    List<ContributionPlan> findByGoalId(Long goalId);

    /**
     * Finds a specific member's planned contribution pledge for a goal.
     *
     * @param goalId the savings goal ID
     * @param memberId the member ID
     * @return Optional containing the plan if defined, or empty
     */
    Optional<ContributionPlan> findByGoalIdAndMemberId(Long goalId, String memberId);

    /**
     * Checks if a member already has a contribution plan defined for a goal.
     *
     * @param goalId the savings goal ID
     * @param memberId the member ID
     * @return true if plan exists, false otherwise
     */
    boolean existsByGoalIdAndMemberId(Long goalId, String memberId);
}
