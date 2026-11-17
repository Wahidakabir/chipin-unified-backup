package com.chipin.chipin.controller;

import com.chipin.chipin.service.HealthScoreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/health-score")
public class HealthScoreController {

    private final HealthScoreService healthScoreService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public HealthScoreController(HealthScoreService healthScoreService) {
        this.healthScoreService = healthScoreService;
    }

    @GetMapping
    public String viewHealthScorePage(@RequestParam(defaultValue = "Musarrat") String currentUser, Model model) {
        model.addAttribute("report", healthScoreService.generateReport(DEFAULT_GROUP_ID, DEMO_MEMBERS));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "health-score";
    }

    @PostMapping("/budget")
    public String updateBudget(@RequestParam double monthlyBudgetLimit,
                               @RequestParam String currentUser,
                               RedirectAttributes redirectAttributes) {
        try {
            healthScoreService.updateMonthlyBudget(DEFAULT_GROUP_ID, monthlyBudgetLimit);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Monthly group budget updated to ৳" + monthlyBudgetLimit + " and health score recalculated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/health-score?currentUser=" + currentUser;
    }
}