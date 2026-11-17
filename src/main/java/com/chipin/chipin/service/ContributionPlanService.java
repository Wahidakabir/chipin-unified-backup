package com.chipin.chipin.service;

import com.chipin.chipin.dto.ContributionPlanResponseDTO;
import com.chipin.chipin.model.ContributionPlan;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.exception.GoalNotFoundException;
import com.chipin.chipin.repository.ContributionPlanRepository;
import com.chipin.chipin.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service handling business logic for member contribution plans (pledges).
 * Manages pledge targets per group member without modifying actual saved balances.
 */
@Service
public class ContributionPlanService {

    private final ContributionPlanRepository contributionPlanRepository;
    private final SavingsGoalRepository savingsGoalRepository;

    public ContributionPlanService(ContributionPlanRepository contributionPlanRepository,
                                   SavingsGoalRepository savingsGoalRepository) {
        this.contributionPlanRepository = contributionPlanRepository;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    /**
     * Sets or updates a member's planned contribution pledge for a specific savings goal.
     * Uses pessimistic locking on the goal to serialize concurrent plan setup and prevent duplicates.
     *
     * @param goalId the savings goal identifier (required)
     * @param memberId the contributor member identifier (required)
     * @param memberName the display name of the member (required)
     * @param plannedAmount the pledged amount to contribute (must be > 0.00)
     * @return the saved or updated ContributionPlan
     * @throws IllegalArgumentException if validation fails or goal does not exist
     */
    @Transactional
    public ContributionPlan setOrUpdatePlan(Long goalId, String memberId, String memberName, BigDecimal plannedAmount) {
        // 1. Input validation
        validatePlanInputs(goalId, memberId, memberName, plannedAmount);

        // 2. Concurrency Lock: Lock the parent SavingsGoal row to serialize plan creation
        SavingsGoal goal = savingsGoalRepository.findByIdForUpdate(goalId)
                .orElseThrow(() -> new GoalNotFoundException("Savings goal not found with ID: " + goalId));

        // 3. Check if a plan already exists for this member and goal
        Optional<ContributionPlan> existingPlan = contributionPlanRepository.findByGoalIdAndMemberId(goalId, memberId.trim());

        if (existingPlan.isPresent()) {
            // Update existing plan (idempotent, prevents duplicate row creation)
            ContributionPlan plan = existingPlan.get();
            plan.setPlannedAmount(plannedAmount);
            plan.setMemberName(memberName.trim());
            return contributionPlanRepository.save(plan);
        } else {
            // Create new plan for this member
            ContributionPlan newPlan = new ContributionPlan(goal, memberId.trim(), memberName.trim(), plannedAmount);
            return contributionPlanRepository.save(newPlan);
        }
    }

    /**
     * Retrieves all member contribution plans for a specific savings goal.
     *
     * @param goalId the savings goal ID
     * @return list of contribution plans
     */
    @Transactional(readOnly = true)
    public List<ContributionPlan> getPlansByGoalId(Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (!savingsGoalRepository.existsById(goalId)) {
            throw new GoalNotFoundException("Savings goal not found with ID: " + goalId);
        }
        return contributionPlanRepository.findByGoalId(goalId);
    }

    /**
     * Retrieves all member contribution plans for a savings goal as response DTOs.
     *
     * @param goalId the savings goal ID
     * @return list of ContributionPlanResponseDTOs
     * @throws IllegalArgumentException if goalId is null or goal is not found
     */
    @Transactional(readOnly = true)
    public List<ContributionPlanResponseDTO> getPlanDTOsByGoalId(Long goalId) {
        return getPlansByGoalId(goalId).stream()
                .map(ContributionPlanResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a specific member's planned contribution for a goal.
     *
     * @param goalId the savings goal ID
     * @param memberId the member ID
     * @return Optional containing the plan if found, or empty
     */
    @Transactional(readOnly = true)
    public Optional<ContributionPlan> getPlanByGoalIdAndMemberId(Long goalId, String memberId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID is required and cannot be blank");
        }
        if (!savingsGoalRepository.existsById(goalId)) {
            throw new GoalNotFoundException("Savings goal not found with ID: " + goalId);
        }
        return contributionPlanRepository.findByGoalIdAndMemberId(goalId, memberId.trim());
    }

    /**
     * Retrieves a specific member's planned contribution for a goal as response DTO.
     *
     * @param goalId the savings goal ID
     * @param memberId the member ID
     * @return Optional containing the ContributionPlanResponseDTO if found, or empty
     * @throws IllegalArgumentException if goalId is null, memberId is blank, or goal is not found
     */
    @Transactional(readOnly = true)
    public Optional<ContributionPlanResponseDTO> getPlanDTOByGoalIdAndMemberId(Long goalId, String memberId) {
        return getPlanByGoalIdAndMemberId(goalId, memberId)
                .map(ContributionPlanResponseDTO::fromEntity);
    }

    /**
     * Validates input parameters for creating or updating a contribution plan.
     */
    private void validatePlanInputs(Long goalId, String memberId, String memberName, BigDecimal plannedAmount) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID is required and cannot be blank");
        }
        if (memberName == null || memberName.isBlank()) {
            throw new IllegalArgumentException("Member name is required and cannot be blank");
        }
        if (plannedAmount == null || plannedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Planned contribution amount must be strictly greater than zero");
        }
    }
}
