package com.chipin.chipin.controller;

import org.springframework.web.bind.annotation.CrossOrigin;

import com.chipin.chipin.dto.ContributionPlanResponseDTO;
import com.chipin.chipin.dto.ContributionResponseDTO;
import com.chipin.chipin.dto.CreateGoalRequest;
import com.chipin.chipin.dto.GoalResponseDTO;
import com.chipin.chipin.dto.MilestoneResponseDTO;
import com.chipin.chipin.dto.RecordContributionRequest;
import com.chipin.chipin.dto.SetPlanRequest;
import com.chipin.chipin.model.Contribution;
import com.chipin.chipin.model.ContributionPlan;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.exception.PlanNotFoundException;
import com.chipin.chipin.service.ContributionPlanService;
import com.chipin.chipin.service.ContributionService;
import com.chipin.chipin.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing API endpoints for Feature 15: Collaborative Savings Goals and Milestone Tracking.
 * Delegates all business operations to the service layer and returns structured DTOs.
 */
@RestController
@RequestMapping("/api/savings-goals")
@CrossOrigin
public class SavingsGoalApiController {

    private final GoalService goalService;
    private final ContributionPlanService contributionPlanService;
    private final ContributionService contributionService;

    public SavingsGoalApiController(GoalService goalService,
                                 ContributionPlanService contributionPlanService,
                                 ContributionService contributionService) {
        this.goalService = goalService;
        this.contributionPlanService = contributionPlanService;
        this.contributionService = contributionService;
    }

    // ==========================================
    // 1. Goal Endpoints
    // ==========================================

    /**
     * 1. Creates a new savings goal with 4 initialized milestones (25%, 50%, 75%, 100%).
     * POST /api/savings-goals
     */
    @PostMapping
    public ResponseEntity<GoalResponseDTO> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        SavingsGoal createdGoal = goalService.createGoal(
                request.getGroupId(),
                request.getCreatorId(),
                request.getTitle(),
                request.getDescription(),
                request.getTargetAmount(),
                request.getDeadline()
        );
        GoalResponseDTO response = goalService.getGoalById(createdGoal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2. Retrieves all savings goals belonging to a group.
     * GET /api/savings-goals?groupId=...
     */
    @GetMapping
    public ResponseEntity<List<GoalResponseDTO>> getGoalsByGroupId(@RequestParam("groupId") String groupId) {
        List<GoalResponseDTO> goals = goalService.getGoalsByGroupId(groupId);
        return ResponseEntity.ok(goals);
    }

    /**
     * 3. Retrieves a single savings goal by its ID.
     * GET /api/savings-goals/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<GoalResponseDTO> getGoalById(@PathVariable("id") Long id) {
        GoalResponseDTO goal = goalService.getGoalById(id);
        return ResponseEntity.ok(goal);
    }

    /**
     * 4. Retrieves milestone checkpoints for a goal in ascending percentage order.
     * GET /api/savings-goals/{id}/milestones
     */
    @GetMapping("/{id}/milestones")
    public ResponseEntity<List<MilestoneResponseDTO>> getMilestonesByGoalId(@PathVariable("id") Long id) {
        List<MilestoneResponseDTO> milestones = goalService.getMilestonesByGoalId(id);
        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // 2. Contribution Plan (Pledge) Endpoints
    // ==========================================

    /**
     * 5. Sets or updates a member's planned contribution pledge for a goal.
     * POST /api/savings-goals/{id}/plans
     */
    @PostMapping("/{id}/plans")
    public ResponseEntity<ContributionPlanResponseDTO> setOrUpdatePlan(
            @PathVariable("id") Long id,
            @Valid @RequestBody SetPlanRequest request) {
        ContributionPlan plan = contributionPlanService.setOrUpdatePlan(
                id,
                request.getMemberId(),
                request.getMemberName(),
                request.getPlannedAmount()
        );
        return ResponseEntity.ok(ContributionPlanResponseDTO.fromEntity(plan));
    }

    /**
     * 6. Retrieves all contribution plans (pledges) for a goal.
     * GET /api/savings-goals/{id}/plans
     */
    @GetMapping("/{id}/plans")
    public ResponseEntity<List<ContributionPlanResponseDTO>> getPlansByGoalId(@PathVariable("id") Long id) {
        List<ContributionPlanResponseDTO> plans = contributionPlanService.getPlanDTOsByGoalId(id);
        return ResponseEntity.ok(plans);
    }

    /**
     * 7. Retrieves a specific member's contribution plan for a goal.
     * GET /api/savings-goals/{id}/plans/{memberId}
     */
    @GetMapping("/{id}/plans/{memberId}")
    public ResponseEntity<ContributionPlanResponseDTO> getPlanByMemberId(
            @PathVariable("id") Long id,
            @PathVariable("memberId") String memberId) {
        ContributionPlanResponseDTO plan = contributionPlanService.getPlanDTOByGoalIdAndMemberId(id, memberId)
                .orElseThrow(() -> new PlanNotFoundException(
                        String.format("Contribution plan not found for member '%s' on goal ID %d", memberId, id)
                ));
        return ResponseEntity.ok(plan);
    }

    // ==========================================
    // 3. Contribution Endpoints
    // ==========================================

    /**
     * 8. Records a financial deposit toward a savings goal inside an atomic transaction.
     * POST /api/savings-goals/{id}/contributions
     */
    @PostMapping("/{id}/contributions")
    public ResponseEntity<ContributionResponseDTO> recordContribution(
            @PathVariable("id") Long id,
            @Valid @RequestBody RecordContributionRequest request) {
        Contribution contribution = contributionService.recordContribution(
                id,
                request.getMemberId(),
                request.getMemberName(),
                request.getAmount(),
                request.getNote()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ContributionResponseDTO.fromEntity(contribution));
    }

    /**
     * 9. Retrieves chronological contribution history for a goal (optionally filtered by member).
     * GET /api/savings-goals/{id}/contributions?memberId=...
     */
    @GetMapping("/{id}/contributions")
    public ResponseEntity<List<ContributionResponseDTO>> getContributions(
            @PathVariable("id") Long id,
            @RequestParam(value = "memberId", required = false) String memberId) {
        List<ContributionResponseDTO> contributions;
        if (memberId != null && !memberId.isBlank()) {
            contributions = contributionService.getContributionDTOsByMember(id, memberId);
        } else {
            contributions = contributionService.getContributionDTOsByGoalId(id);
        }
        return ResponseEntity.ok(contributions);
    }
}
