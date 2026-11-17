package com.chipin.chipin.controller;

import com.chipin.chipin.model.ExpenseProposal;
import com.chipin.chipin.service.VotingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/voting")
public class VotingController {

    private static final String GROUP_ID = "group-1";
    private static final List<String> MEMBERS = List.of("Mufliha", "Afifa", "Sayma", "Wahida");

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @GetMapping
    public String showPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        ExpenseProposal blank = new ExpenseProposal();
        blank.setApprovalRule("MAJORITY");
        fillModel(model, currentUser, blank);
        return "voting";
    }

    @PostMapping("/create")
    public String create(@RequestParam String currentUser,
                         @Valid @ModelAttribute("newProposal") ExpenseProposal newProposal,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            fillModel(model, currentUser, newProposal);
            return "voting";
        }
        try {
            ExpenseProposal saved = votingService.createProposal(newProposal, GROUP_ID, currentUser, MEMBERS);
            if ("APPROVED".equals(saved.getStatus())) {
                redirect.addFlashAttribute("successMessage",
                        "Under ৳" + (int) votingService.getSpendingApprovalLimit()
                                + ", so it was added to the ledger without a vote.");
            } else {
                redirect.addFlashAttribute("successMessage",
                        "Proposal submitted. Your YES vote has been counted.");
            }
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        redirect.addAttribute("currentUser", currentUser);
        return "redirect:/voting";
    }

    @PostMapping("/{id}/vote")
    public String vote(@PathVariable Long id,
                       @RequestParam String currentUser,
                       @RequestParam String choice,
                       RedirectAttributes redirect) {
        try {
            ExpenseProposal p = votingService.castVote(id, currentUser, choice, MEMBERS);
            String msg = switch (p.getStatus()) {
                case "APPROVED" -> "Approved! The expense was added to the group ledger.";
                case "REJECTED" -> "The proposal was rejected.";
                default -> "Your vote was recorded.";
            };
            redirect.addFlashAttribute("successMessage", msg);
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        redirect.addAttribute("currentUser", currentUser);
        return "redirect:/voting";
    }

    private void fillModel(Model model, String currentUser, ExpenseProposal newProposal) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", MEMBERS);
        model.addAttribute("proposals", votingService.getGroupProposals(GROUP_ID));
        model.addAttribute("approvalLimit", votingService.getSpendingApprovalLimit());
        model.addAttribute("newProposal", newProposal);
    }
}