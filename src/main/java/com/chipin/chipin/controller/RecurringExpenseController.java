package com.chipin.chipin.controller;

import com.chipin.chipin.model.RecurringExpense;
import com.chipin.chipin.service.RecurringExpenseService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/recurring")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public RecurringExpenseController(RecurringExpenseService recurringService) {
        this.recurringService = recurringService;
    }

    @GetMapping
    public String viewRecurringPage(@RequestParam(defaultValue = "Musarrat") String currentUser, Model model) {
        RecurringExpense formObj = new RecurringExpense();
        formObj.setNextDueDate(LocalDate.now());

        model.addAttribute("schedules", recurringService.getGroupRecurringExpenses(DEFAULT_GROUP_ID));
        model.addAttribute("newRecurring", formObj);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "recurring";
    }

    @PostMapping("/create")
    public String createRecurring(@Valid @ModelAttribute("newRecurring") RecurringExpense newRecurring,
                                  BindingResult bindingResult,
                                  @RequestParam String currentUser,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("schedules", recurringService.getGroupRecurringExpenses(DEFAULT_GROUP_ID));
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("members", DEMO_MEMBERS);
            return "recurring";
        }

        recurringService.createRecurringExpense(newRecurring, DEFAULT_GROUP_ID, currentUser, DEMO_MEMBERS);
        redirectAttributes.addFlashAttribute("successMessage",
                "Recurring schedule created and split equally across all " + DEMO_MEMBERS.size() + " members!");
        return "redirect:/recurring?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/trigger")
    public String triggerNow(@PathVariable Long id,
                             @RequestParam String currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            recurringService.triggerBillingCycle(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Billing cycle executed! Expense logged to group ledger and next due date advanced.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/recurring?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/toggle")
    public String toggleSchedule(@PathVariable Long id,
                                 @RequestParam String currentUser,
                                 RedirectAttributes redirectAttributes) {
        try {
            recurringService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/recurring?currentUser=" + currentUser;
    }
}