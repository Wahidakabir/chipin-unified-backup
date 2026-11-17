package com.chipin.chipin.service;

import com.chipin.chipin.dto.GoalResponseDTO;
import com.chipin.chipin.dto.MilestoneResponseDTO;
import com.chipin.chipin.model.Milestone;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.exception.GoalNotFoundException;
import com.chipin.chipin.repository.MilestoneRepository;
import com.chipin.chipin.repository.SavingsGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service handling business logic for SavingsGoal management and milestone initialization.
 */
@Service
public class GoalService {

    private static final int[] STANDARD_MILESTONE_PERCENTAGES = {25, 50, 75, 100};

    private final SavingsGoalRepository savingsGoalRepository;
    private final MilestoneRepository milestoneRepository;

    public GoalService(SavingsGoalRepository savingsGoalRepository, MilestoneRepository milestoneRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.milestoneRepository = milestoneRepository;
    }

    /**
     * Creates a new SavingsGoal and atomically initializes its four standard milestones (25%, 50%, 75%, 100%).
     *
     * @param groupId the group identifier (required)
     * @param creatorId the creator member identifier (required)
     * @param title the goal title (1-100 characters, required)
     * @param description optional detailed description
     * @param targetAmount target amount to save (must be > 0.00)
     * @param deadline deadline date (must be today or in the future)
     * @return the saved SavingsGoal with initialized milestones
     * @throws IllegalArgumentException if any validation rule is violated
     */
    @Transactional
    public SavingsGoal createGoal(String groupId, String creatorId, String title,
                                  String description, BigDecimal targetAmount, LocalDate deadline) {
        // 1. Service-layer input validation
        validateGoalInputs(groupId, creatorId, title, targetAmount, deadline);

        // 2. Instantiate SavingsGoal using the entity's parameterized constructor
        SavingsGoal goal = new SavingsGoal(
                groupId.trim(),
                creatorId.trim(),
                title.trim(),
                description != null ? description.trim() : null,
                targetAmount,
                deadline
        );

        // 3. Initialize exactly four milestone checkpoints (25%, 50%, 75%, 100%)
        initializeMilestones(goal, targetAmount);

        // 4. Save goal and cascaded milestones atomically to PostgreSQL
        return savingsGoalRepository.save(goal);
    }

    /**
     * Validates input parameters before creating a savings goal.
     */
    private void validateGoalInputs(String groupId, String creatorId, String title,
                                    BigDecimal targetAmount, LocalDate deadline) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID is required and cannot be blank");
        }
        if (creatorId == null || creatorId.isBlank()) {
            throw new IllegalArgumentException("Creator ID is required and cannot be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Goal title is required and cannot be blank");
        }
        if (title.trim().length() > 100) {
            throw new IllegalArgumentException("Goal title cannot exceed 100 characters");
        }
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target amount must be strictly greater than zero");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("Deadline date is required");
        }
        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline date cannot be in the past");
        }
    }

    /**
     * Calculates snapshots and attaches the 4 standard milestones to the goal.
     * Snapshot formula: (targetAmount * percentage) / 100 with scale 2 and RoundingMode.HALF_UP.
     */
    private void initializeMilestones(SavingsGoal goal, BigDecimal targetAmount) {
        for (int percentage : STANDARD_MILESTONE_PERCENTAGES) {
            BigDecimal snapshot = targetAmount
                    .multiply(BigDecimal.valueOf(percentage))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            Milestone milestone = new Milestone(goal, percentage, snapshot);
            goal.getMilestones().add(milestone);
        }
    }

    /**
     * Retrieves a single savings goal by its ID and returns a GoalResponseDTO with all milestones.
     *
     * @param id the savings goal ID
     * @return GoalResponseDTO containing goal details and milestones
     * @throws IllegalArgumentException if id is null or goal is not found
     */
    @Transactional(readOnly = true)
    public GoalResponseDTO getGoalById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Savings goal not found with ID: " + id));

        List<Milestone> milestones = milestoneRepository.findByGoalIdOrderByPercentageAsc(id);
        return GoalResponseDTO.fromEntityWithMilestones(goal, milestones);
    }

    /**
     * Alias for getGoalById(Long id) returning GoalResponseDTO.
     */
    @Transactional(readOnly = true)
    public GoalResponseDTO getGoalDTOById(Long id) {
        return getGoalById(id);
    }

    /**
     * Retrieves the underlying SavingsGoal JPA entity by ID.
     * Intended for internal service-to-service usage.
     *
     * @param id the savings goal ID
     * @return SavingsGoal entity
     * @throws IllegalArgumentException if id is null or goal is not found
     */
    @Transactional(readOnly = true)
    public SavingsGoal getGoalEntityById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        return savingsGoalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException("Savings goal not found with ID: " + id));
    }

    /**
     * Retrieves all savings goals for a group, returning a list of GoalResponseDTOs with milestones.
     * Uses a single bulk milestone query to avoid N+1 queries.
     *
     * @param groupId the group identifier (required)
     * @return list of GoalResponseDTOs
     * @throws IllegalArgumentException if groupId is blank
     */
    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getGoalsByGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("Group ID cannot be blank");
        }
        List<SavingsGoal> goals = savingsGoalRepository.findByGroupId(groupId);
        if (goals.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> goalIds = goals.stream()
                .map(SavingsGoal::getId)
                .collect(Collectors.toList());

        List<Milestone> allMilestones = milestoneRepository.findByGoalIdIn(goalIds);
        Map<Long, List<Milestone>> milestonesByGoal = allMilestones.stream()
                .collect(Collectors.groupingBy(m -> m.getGoal().getId()));

        return goals.stream()
                .map(goal -> GoalResponseDTO.fromEntityWithMilestones(
                        goal,
                        milestonesByGoal.getOrDefault(goal.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    /**
     * Alias for getGoalsByGroupId(String groupId) returning List<GoalResponseDTO>.
     */
    @Transactional(readOnly = true)
    public List<GoalResponseDTO> getGoalDTOsByGroupId(String groupId) {
        return getGoalsByGroupId(groupId);
    }

    /**
     * Retrieves all milestone checkpoints for a goal in ascending percentage order (25%, 50%, 75%, 100%).
     *
     * @param goalId the savings goal ID
     * @return list of MilestoneResponseDTOs ordered ascending by percentage
     * @throws IllegalArgumentException if goalId is null or goal is not found
     */
    @Transactional(readOnly = true)
    public List<MilestoneResponseDTO> getMilestonesByGoalId(Long goalId) {
        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }
        if (!savingsGoalRepository.existsById(goalId)) {
            throw new GoalNotFoundException("Savings goal not found with ID: " + goalId);
        }
        return milestoneRepository.findByGoalIdOrderByPercentageAsc(goalId).stream()
                .map(MilestoneResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
