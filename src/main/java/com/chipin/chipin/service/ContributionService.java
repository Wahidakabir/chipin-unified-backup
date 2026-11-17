package com.chipin.chipin.service;

import com.chipin.chipin.dto.ContributionResponseDTO;
import com.chipin.chipin.model.Contribution;
import com.chipin.chipin.model.GoalStatus;
import com.chipin.chipin.model.Milestone;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.exception.GoalNotFoundException;
import com.chipin.chipin.exception.OverfundingException;
import com.chipin.chipin.repository.ContributionRepository;
import com.chipin.chipin.repository.MilestoneRepository;
import com.chipin.chipin.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing financial contributions and ledger operations.
 * Coordinates pessimistic concurrency locking, overfunding prevention,
 * balance updates, and milestone progression.
 */
@Service
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final MilestoneRepository milestoneRepository;

    public ContributionService(ContributionRepository contributionRepository,
                               SavingsGoalRepository savingsGoalRepository,
                               MilestoneRepository milestoneRepository) {
        this.contributionRepository = contributionRepository;
        this.savingsGoalRepository = savingsGoalRepository;
        this.milestoneRepository = milestoneRepository;
    }

    /**
     * Records a financial contribution toward a SavingsGoal inside an atomic transaction.
     * Uses pessimistic write locking (SELECT ... FOR UPDATE) to serialize concurrent deposits
     * and strictly prevent total savings from exceeding the target amount.
     *
     * @param goalId the savings goal ID (required)
     * @param memberId the contributor member identifier (required)
     * @param memberName the display name of contributor (required)
     * @param amount the monetary contribution amount (must be > 0.00)
     * @param note optional note or memo
     * @return the saved immutable Contribution ledger record
     * @throws IllegalArgumentException if validation fails or overfunding is attempted
     * @throws IllegalStateException if the goal is already completed
     */
    @Transactional
    public Contribution recordContribution(Long goalId, String memberId, String memberName,
                                           BigDecimal amount, String note) {
        // 1. Input validation
        validateContributionInputs(goalId, memberId, memberName, amount);

        // 2. Pessimistic Row Lock: Serializes access to this goal in PostgreSQL
        SavingsGoal goal = savingsGoalRepository.findByIdForUpdate(goalId)
                .orElseThrow(() -> new GoalNotFoundException("Savings goal not found with ID: " + goalId));

        // 3. Reject contributions to already completed goals
        if (goal.getStatus() == GoalStatus.COMPLETED) {
            throw new IllegalStateException("Cannot contribute to a goal that has already been COMPLETED");
        }

        // 4. Overfunding Prevention: Validate amount does not exceed remaining target
        BigDecimal remaining = goal.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new OverfundingException(String.format(
                    "Contribution amount $%s exceeds the remaining target of $%s. Maximum allowed contribution is $%s.",
                    amount.toPlainString(), remaining.toPlainString(), remaining.toPlainString()
            ));
        }

        // 5. Instantiate and save immutable Contribution ledger record
        Contribution contribution = new Contribution(
                goal,
                memberId.trim(),
                memberName.trim(),
                amount,
                note != null ? note.trim() : null
        );
        Contribution savedContribution = contributionRepository.save(contribution);

        // 6. Increase SavingsGoal currentAmount by exact contribution amount
        BigDecimal newTotal = goal.getCurrentAmount().add(amount);
        goal.setCurrentAmount(newTotal);

        // 7. Update goal status (COMPLETED if target reached; preserve/apply OVERDUE if deadline passed)
        if (newTotal.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(GoalStatus.COMPLETED);
        } else if (LocalDate.now().isAfter(goal.getDeadline())) {
            goal.setStatus(GoalStatus.OVERDUE);
        }

        // 8. Milestone tracking: evaluate and unlock milestones atomically
        evaluateMilestones(goal, newTotal);

        // 9. Persist updated goal state
        savingsGoalRepository.save(goal);

        return savedContribution;
    }

    /**
     * Checks unachieved milestones and marks them achieved if the new total meets their target snapshot.
     */
    private void evaluateMilestones(SavingsGoal goal, BigDecimal newTotal) {
        List<Milestone> pendingMilestones = milestoneRepository
                .findByGoalIdAndIsAchievedFalseOrderByPercentageAsc(goal.getId());

        for (Milestone milestone : pendingMilestones) {
            if (newTotal.compareTo(milestone.getTargetAmountSnapshot()) >= 0) {
                milestone.achieve();
                milestoneRepository.save(milestone);
            }
        }
    }

    /**
     * Validates input parameters for recording a contribution.
     */
    private void validateContributionInputs(Long goalId, String memberId, String memberName, BigDecimal amount) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID is required and cannot be blank");
        }
        if (memberName == null || memberName.isBlank()) {
            throw new IllegalArgumentException("Member name is required and cannot be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution amount must be strictly greater than zero");
        }
    }

    /**
     * Retrieves the chronological contribution ledger history for a goal (newest first).
     */
    @Transactional(readOnly = true)
    public List<Contribution> getContributionsByGoalId(Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (!savingsGoalRepository.existsById(goalId)) {
            throw new GoalNotFoundException("Savings goal not found with ID: " + goalId);
        }
        return contributionRepository.findByGoalIdOrderByContributedAtDesc(goalId);
    }

    /**
     * Retrieves the chronological contribution ledger history for a goal as response DTOs (newest first).
     *
     * @param goalId the savings goal ID
     * @return list of ContributionResponseDTOs ordered newest first
     * @throws IllegalArgumentException if goalId is null or goal is not found
     */
    @Transactional(readOnly = true)
    public List<ContributionResponseDTO> getContributionDTOsByGoalId(Long goalId) {
        return getContributionsByGoalId(goalId).stream()
                .map(ContributionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all contributions made by a specific member toward a goal.
     */
    @Transactional(readOnly = true)
    public List<Contribution> getContributionsByMember(Long goalId, String memberId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("Member ID is required and cannot be blank");
        }
        if (!savingsGoalRepository.existsById(goalId)) {
            throw new GoalNotFoundException("Savings goal not found with ID: " + goalId);
        }
        return contributionRepository.findByGoalIdAndMemberIdOrderByContributedAtDesc(goalId, memberId.trim());
    }

    /**
     * Retrieves all contributions made by a specific member toward a goal as response DTOs (newest first).
     *
     * @param goalId the savings goal ID
     * @param memberId the contributor member ID
     * @return list of ContributionResponseDTOs for this member ordered newest first
     * @throws IllegalArgumentException if goalId is null, memberId is blank, or goal is not found
     */
    @Transactional(readOnly = true)
    public List<ContributionResponseDTO> getContributionDTOsByMember(Long goalId, String memberId) {
        return getContributionsByMember(goalId, memberId).stream()
                .map(ContributionResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the exact total amount saved from the immutable contribution ledger entries.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalContributed(Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (!savingsGoalRepository.existsById(goalId)) {
            throw new GoalNotFoundException("Savings goal not found with ID: " + goalId);
        }
        return contributionRepository.sumAmountByGoalId(goalId);
    }
}
