package com.chipin.chipin.controller;

import com.chipin.chipin.service.DebtSimplificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

// Feature 1: Debt Simplification Engine with Graph Visualization
@Controller
@RequestMapping("/debts")
public class DebtSimplificationController {

    private final DebtSimplificationService debtService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public DebtSimplificationController(DebtSimplificationService debtService) {
        this.debtService = debtService;
    }

    @GetMapping
    public String viewDebtsPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("overview", debtService.getOverview(DEFAULT_GROUP_ID, DEMO_MEMBERS));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "debts";
    }

    @PostMapping("/settle")
    public String recordPayment(@RequestParam String currentUser,
                                @RequestParam String fromUserId,
                                @RequestParam String toUserId,
                                @RequestParam BigDecimal amount,
                                RedirectAttributes redirectAttributes) {
        try {
            debtService.recordPayment(DEFAULT_GROUP_ID, fromUserId, toUserId, amount, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment recorded: " + fromUserId + " paid " + toUserId + " ৳" + amount + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/debts?currentUser=" + currentUser;
    }

    @PostMapping("/settle-all")
    public String settleAll(@RequestParam String currentUser, RedirectAttributes redirectAttributes) {
        try {
            int count = debtService.settleAll(DEFAULT_GROUP_ID, DEMO_MEMBERS, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    count + " payment(s) recorded. The group is all settled up!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/debts?currentUser=" + currentUser;
    }
}
