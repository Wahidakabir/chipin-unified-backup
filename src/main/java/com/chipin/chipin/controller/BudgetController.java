package com.chipin.chipin.controller;

import com.chipin.chipin.dto.CategoryResponseDTO;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetExpense;
import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;
import com.chipin.chipin.service.BudgetExpenseService;
import com.chipin.chipin.service.BudgetService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Thymeleaf Web Controller for Feature 11: Group Budget Management System.
 * Renders server-side HTML views matching Chipin design system and user session conventions.
 */
@Controller
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetExpenseService budgetExpenseService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public BudgetController(BudgetService budgetService, BudgetExpenseService budgetExpenseService) {
        this.budgetService = budgetService;
        this.budgetExpenseService = budgetExpenseService;
    }

    @GetMapping
    public String viewBudgetsPage(@RequestParam(defaultValue = "Mufliha") String currentUser,
                                  @RequestParam(required = false) Long budgetId,
                                  Model model) {
        List<GroupBudget> budgets = budgetService.getBudgetsByGroupIdOrdered(DEFAULT_GROUP_ID);

        GroupBudget selectedBudget = null;
        if (budgetId != null) {
            selectedBudget = budgets.stream()
                    .filter(b -> b.getId().equals(budgetId))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedBudget == null && !budgets.isEmpty()) {
            selectedBudget = budgets.stream()
                    .filter(b -> b.getStatus() == BudgetStatus.ACTIVE || b.getStatus() == BudgetStatus.EXCEEDED)
                    .findFirst()
                    .orElse(budgets.get(0));
        }

        List<CategoryResponseDTO> categoryViews = new ArrayList<>();
        List<BudgetExpense> expenses = new ArrayList<>();
        BigDecimal totalAllocated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal unallocated = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingLimit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        boolean isBudgetOverrun = false;
        BigDecimal budgetOverrunAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (selectedBudget != null) {
            Long bId = selectedBudget.getId();
            totalAllocated = budgetService.calculateTotalAllocatedAmount(bId);
            unallocated = budgetService.calculateUnallocatedAmount(bId);
            totalSpent = budgetExpenseService.calculateTotalBudgetSpending(bId);
            remainingLimit = selectedBudget.getTotalLimit().subtract(totalSpent).setScale(2, RoundingMode.HALF_UP);
            isBudgetOverrun = budgetExpenseService.isBudgetOverrun(bId);
            budgetOverrunAmount = budgetExpenseService.getBudgetOverrunAmount(bId);

            List<BudgetCategory> categories = budgetService.getCategoriesByBudgetId(bId);
            for (BudgetCategory cat : categories) {
                BigDecimal spent = budgetExpenseService.calculateCategorySpending(cat.getId());
                categoryViews.add(CategoryResponseDTO.fromEntity(cat, spent, bId));
            }

            expenses = budgetExpenseService.getExpensesByBudgetId(bId);
        }

        model.addAttribute("budgets", budgets);
        model.addAttribute("selectedBudget", selectedBudget);
        model.addAttribute("categories", categoryViews);
        model.addAttribute("expenses", expenses);
        model.addAttribute("totalAllocated", totalAllocated);
        model.addAttribute("unallocated", unallocated);
        model.addAttribute("totalSpent", totalSpent);
        model.addAttribute("remainingLimit", remainingLimit);
        model.addAttribute("isBudgetOverrun", isBudgetOverrun);
        model.addAttribute("budgetOverrunAmount", budgetOverrunAmount);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("today", LocalDate.now());

        return "budgets";
    }

    @PostMapping("/create")
    public String createBudget(@RequestParam String currentUser,
                               @RequestParam String name,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodEnd,
                               @RequestParam BigDecimal totalLimit,
                               RedirectAttributes redirectAttributes) {
        try {
            GroupBudget budget = budgetService.createBudget(DEFAULT_GROUP_ID, currentUser, name,
                    periodStart, periodEnd, totalLimit);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Budget '" + budget.getName() + "' created successfully with limit ৳" + budget.getTotalLimit() + ".");
            return "redirect:/budgets?currentUser=" + currentUser + "&budgetId=" + budget.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/budgets?currentUser=" + currentUser;
        }
    }

    @PostMapping("/categories/add")
    public String addCategory(@RequestParam String currentUser,
                              @RequestParam Long budgetId,
                              @RequestParam String name,
                              @RequestParam BigDecimal allocatedAmount,
                              RedirectAttributes redirectAttributes) {
        try {
            BudgetCategory category = budgetService.allocateCategory(budgetId, name, allocatedAmount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Category '" + category.getName() + "' allocated with ৳" + category.getAllocatedAmount() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/budgets?currentUser=" + currentUser + "&budgetId=" + budgetId;
    }

    @PostMapping("/expenses/record")
    public String recordExpense(@RequestParam String currentUser,
                                @RequestParam Long budgetId,
                                @RequestParam Long categoryId,
                                @RequestParam String payerId,
                                @RequestParam BigDecimal amount,
                                @RequestParam String description,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseDate,
                                RedirectAttributes redirectAttributes) {
        try {
            BudgetExpense expense = budgetExpenseService.recordExpense(budgetId, categoryId, payerId,
                    payerId, amount, description, expenseDate);
            String message = "Expense recorded: " + payerId + " spent ৳" + amount + " on " + description + ".";
            if (budgetExpenseService.isCategoryOverrun(categoryId)) {
                message += " Warning: Category '" + expense.getCategory().getName() + "' has exceeded its limit!";
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/budgets?currentUser=" + currentUser + "&budgetId=" + budgetId;
    }

    @PostMapping("/expenses/{expenseId}/void")
    public String voidExpense(@RequestParam String currentUser,
                              @RequestParam Long budgetId,
                              @PathVariable Long expenseId,
                              @RequestParam String reason,
                              RedirectAttributes redirectAttributes) {
        try {
            budgetExpenseService.voidExpense(expenseId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Expense transaction voided successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/budgets?currentUser=" + currentUser + "&budgetId=" + budgetId;
    }

    @PostMapping("/{budgetId}/archive")
    public String archiveBudget(@RequestParam String currentUser,
                                @PathVariable Long budgetId,
                                RedirectAttributes redirectAttributes) {
        try {
            budgetService.archiveBudget(budgetId);
            redirectAttributes.addFlashAttribute("successMessage", "Budget archived successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/budgets?currentUser=" + currentUser + "&budgetId=" + budgetId;
    }
}
