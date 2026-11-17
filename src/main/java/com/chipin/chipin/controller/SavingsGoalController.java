package com.chipin.chipin.controller;

import com.chipin.chipin.dto.ContributionPlanResponseDTO;
import com.chipin.chipin.dto.ContributionResponseDTO;
import com.chipin.chipin.dto.GoalResponseDTO;
import com.chipin.chipin.model.GoalStatus;
import com.chipin.chipin.model.SavingsGoal;
import com.chipin.chipin.service.ContributionPlanService;
import com.chipin.chipin.service.ContributionService;
import com.chipin.chipin.service.GoalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Thymeleaf Web Controller for Feature 15: Collaborative Savings Goals and Milestone Tracking.
 * Matches Chipin UI layout and user session conventions.
 */
@Controller
@RequestMapping("/savings")
public class SavingsGoalController {

    private final GoalService goalService;
    private final ContributionPlanService contributionPlanService;
    private final ContributionService contributionService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public SavingsGoalController(GoalService goalService,
                                 ContributionPlanService contributionPlanService,
                                 ContributionService contributionService) {
        this.goalService = goalService;
        this.contributionPlanService = contributionPlanService;
        this.contributionService = contributionService;
    }

    @GetMapping
    public String viewSavingsPage(@RequestParam(defaultValue = "Mufliha") String currentUser,
                                  @RequestParam(required = false) Long goalId,
                                  Model model) {
        List<GoalResponseDTO> allGoals = goalService.getGoalsByGroupId(DEFAULT_GROUP_ID);

        GoalResponseDTO selectedGoal = null;
        if (goalId != null) {
            selectedGoal = allGoals.stream()
                    .filter(g -> g.getId().equals(goalId))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedGoal == null && !allGoals.isEmpty()) {
            selectedGoal = allGoals.stream()
                    .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                    .findFirst()
                    .orElse(allGoals.get(0));
        }

        List<ContributionPlanResponseDTO> plans = new ArrayList<>();
        List<ContributionResponseDTO> contributions = new ArrayList<>();

        if (selectedGoal != null) {
            plans = contributionPlanService.getPlanDTOsByGoalId(selectedGoal.getId());
            contributions = contributionService.getContributionDTOsByGoalId(selectedGoal.getId());
        }

        model.addAttribute("goals", allGoals);
        model.addAttribute("selectedGoal", selectedGoal);
        model.addAttribute("plans", plans);
        model.addAttribute("contributions", contributions);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("today", LocalDate.now());

        return "savings";
    }

    @PostMapping("/create")
    public String createGoal(@RequestParam String currentUser,
                             @RequestParam String title,
                             @RequestParam(required = false) String description,
                             @RequestParam BigDecimal targetAmount,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
                             RedirectAttributes redirectAttributes) {
        try {
            SavingsGoal goal = goalService.createGoal(DEFAULT_GROUP_ID, currentUser, title,
                    description, targetAmount, deadline);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Savings goal '" + goal.getTitle() + "' created successfully with target ৳" + goal.getTargetAmount() + "!");
            return "redirect:/savings?currentUser=" + currentUser + "&goalId=" + goal.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/savings?currentUser=" + currentUser;
        }
    }

    @PostMapping("/plans/set")
    public String setPlan(@RequestParam String currentUser,
                          @RequestParam Long goalId,
                          @RequestParam String memberId,
                          @RequestParam BigDecimal plannedAmount,
                          RedirectAttributes redirectAttributes) {
        try {
            contributionPlanService.setOrUpdatePlan(goalId, memberId, memberId, plannedAmount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pledge recorded for " + memberId + ": ৳" + plannedAmount + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/savings?currentUser=" + currentUser + "&goalId=" + goalId;
    }

    @PostMapping("/contributions/record")
    public String recordContribution(@RequestParam String currentUser,
                                     @RequestParam Long goalId,
                                     @RequestParam String memberId,
                                     @RequestParam BigDecimal amount,
                                     @RequestParam(required = false) String note,
                                     RedirectAttributes redirectAttributes) {
        try {
            contributionService.recordContribution(goalId, memberId, memberId, amount, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Contribution recorded: " + memberId + " contributed ৳" + amount + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/savings?currentUser=" + currentUser + "&goalId=" + goalId;
    }
}
