package com.chipin.chipin.controller;

import com.chipin.chipin.model.*;
import com.chipin.chipin.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.List;

@Controller
public class DashboardController {

    private final VotingService votingService;
    private final EmergencyService emergencyService;
    private final HealthScoreService healthScoreService;
    private final RecurringExpenseService recurringService;
    private final ReceiptService receiptService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public DashboardController(VotingService votingService,
                               EmergencyService emergencyService,
                               HealthScoreService healthScoreService,
                               RecurringExpenseService recurringService,
                               ReceiptService receiptService) {
        this.votingService = votingService;
        this.emergencyService = emergencyService;
        this.healthScoreService = healthScoreService;
        this.recurringService = recurringService;
        this.receiptService = receiptService;
    }

    @GetMapping("/")
    public String homeDashboard(@RequestParam(defaultValue = "Musarrat") String currentUser, Model model) {
        List<ExpenseProposal> proposals = votingService.getGroupProposals(DEFAULT_GROUP_ID);
        List<EmergencyExpense> emergencies = emergencyService.getGroupEmergencies(DEFAULT_GROUP_ID);
        HealthScoreReport healthReport = healthScoreService.generateReport(DEFAULT_GROUP_ID, DEMO_MEMBERS);
        List<RecurringExpense> schedules = recurringService.getGroupRecurringExpenses(DEFAULT_GROUP_ID);
        List<ExpenseReceipt> receipts = receiptService.getGroupReceipts(DEFAULT_GROUP_ID);

        long pendingVotes = proposals.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
        long activeEmergencies = emergencies.stream().filter(e -> "ACTIVE".equals(e.getStatus())).count();
        long activeBills = schedules.stream().filter(RecurringExpense::isActive).count();
        long unverifiedReceipts = receipts.stream().filter(r -> "UNVERIFIED".equals(r.getVerificationStatus())).count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("healthReport", healthReport);
        model.addAttribute("pendingVotes", pendingVotes);
        model.addAttribute("activeEmergencies", activeEmergencies);
        model.addAttribute("activeBills", activeBills);
        model.addAttribute("unverifiedReceipts", unverifiedReceipts);
        model.addAttribute("recentExpenses", receiptService.getGroupExpenses(DEFAULT_GROUP_ID));
        return "dashboard";
    }
}