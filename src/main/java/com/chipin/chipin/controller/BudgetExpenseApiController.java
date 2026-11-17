package com.chipin.chipin.controller;

import org.springframework.web.bind.annotation.CrossOrigin;

import com.chipin.chipin.dto.ExpenseResponseDTO;
import com.chipin.chipin.dto.RecordExpenseRequest;
import com.chipin.chipin.dto.UpdateExpenseRequest;
import com.chipin.chipin.dto.VoidExpenseRequest;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetExpense;
import com.chipin.chipin.model.ExpenseStatus;
import com.chipin.chipin.service.BudgetExpenseService;
import com.chipin.chipin.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller managing expense transactions recorded under group budgets in Feature 11.
 * Enforces nested resource ownership, chronological-descending ordering, and partial update semantics.
 * Resolves category metadata safely using pre-fetched category lookups to eliminate lazy-loading traversal
 * and avoid N+1 query patterns outside transaction boundaries.
 */
@RestController
@RequestMapping("/api/budgets/{budgetId}/expenses")
@CrossOrigin
public class BudgetExpenseApiController {

    private final BudgetExpenseService budgetExpenseService;
    private final BudgetService budgetService;

    public BudgetExpenseApiController(BudgetExpenseService budgetExpenseService, BudgetService budgetService) {
        this.budgetExpenseService = budgetExpenseService;
        this.budgetService = budgetService;
    }

    /**
     * 1. Records a new financial expense transaction against a budget category.
     * POST /api/budgets/{budgetId}/expenses
     */
    @PostMapping
    public ResponseEntity<ExpenseResponseDTO> recordExpense(
            @PathVariable("budgetId") Long budgetId,
            @Valid @RequestBody RecordExpenseRequest request) {
        BudgetExpense expense = budgetExpenseService.recordExpense(
                budgetId,
                request.getCategoryId(),
                request.getPayerId(),
                request.getPayerName(),
                request.getAmount(),
                request.getDescription(),
                request.getExpenseDate(),
                request.getExternalExpenseId()
        );

        Map<Long, String> categoryNames = getCategoryNameMap(budgetId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toExpenseResponseDTO(expense, budgetId, categoryNames));
    }

    /**
     * 2. Lists expense transactions for a budget, supporting optional category and status filtering.
     * Preserves chronological-descending ordering and eliminates N+1 category queries.
     * GET /api/budgets/{budgetId}/expenses?categoryId=...&status=...
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponseDTO>> getExpenses(
            @PathVariable("budgetId") Long budgetId,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) ExpenseStatus status) {
        // Retrieve budget categories once to validate budget existence and build category lookup map
        Map<Long, String> categoryNames = getCategoryNameMap(budgetId);

        List<BudgetExpense> expenses;
        if (categoryId != null) {
            // Enforce that category belongs to this budget without traversing lazy JPA relationships
            if (!categoryNames.containsKey(categoryId)) {
                throw new IllegalArgumentException(
                        "Category ID " + categoryId + " does not belong to budget ID " + budgetId
                );
            }

            expenses = (status != null)
                    ? budgetExpenseService.getExpensesByCategoryIdAndStatus(categoryId, status)
                    : budgetExpenseService.getExpensesByCategoryId(categoryId);
        } else {
            expenses = (status != null)
                    ? budgetExpenseService.getExpensesByBudgetIdAndStatus(budgetId, status)
                    : budgetExpenseService.getExpensesByBudgetId(budgetId);
        }

        // Preserve chronological-descending ordering (newest first)
        List<ExpenseResponseDTO> response = expenses.stream()
                .sorted(Comparator.comparing(BudgetExpense::getExpenseDate, Comparator.reverseOrder())
                        .thenComparing(BudgetExpense::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(e -> toExpenseResponseDTO(e, budgetId, categoryNames))
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 3. Retrieves an individual expense transaction by ID, enforcing nested budget ownership.
     * GET /api/budgets/{budgetId}/expenses/{expenseId}
     */
    @GetMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponseDTO> getExpenseById(
            @PathVariable("budgetId") Long budgetId,
            @PathVariable("expenseId") Long expenseId) {
        Map<Long, String> categoryNames = getCategoryNameMap(budgetId);
        BudgetExpense expense = getAndValidateExpenseBelongsToBudget(budgetId, expenseId, categoryNames);
        return ResponseEntity.ok(toExpenseResponseDTO(expense, budgetId, categoryNames));
    }

    /**
     * 4. Partially updates an active expense transaction, preserving omitted fields.
     * PATCH /api/budgets/{budgetId}/expenses/{expenseId}
     */
    @PatchMapping("/{expenseId}")
    public ResponseEntity<ExpenseResponseDTO> updateExpense(
            @PathVariable("budgetId") Long budgetId,
            @PathVariable("expenseId") Long expenseId,
            @Valid @RequestBody UpdateExpenseRequest request) {
        Map<Long, String> categoryNames = getCategoryNameMap(budgetId);
        getAndValidateExpenseBelongsToBudget(budgetId, expenseId, categoryNames);

        BudgetExpense updated = budgetExpenseService.updateExpense(
                expenseId,
                request.getAmount(),
                request.getDescription(),
                request.getExpenseDate(),
                request.getPayerName(),
                request.getCategoryId()
        );
        return ResponseEntity.ok(toExpenseResponseDTO(updated, budgetId, categoryNames));
    }

    /**
     * 5. Voids an expense transaction with a mandatory cancellation reason.
     * POST /api/budgets/{budgetId}/expenses/{expenseId}/void
     */
    @PostMapping("/{expenseId}/void")
    public ResponseEntity<ExpenseResponseDTO> voidExpense(
            @PathVariable("budgetId") Long budgetId,
            @PathVariable("expenseId") Long expenseId,
            @Valid @RequestBody VoidExpenseRequest request) {
        Map<Long, String> categoryNames = getCategoryNameMap(budgetId);
        getAndValidateExpenseBelongsToBudget(budgetId, expenseId, categoryNames);

        BudgetExpense voided = budgetExpenseService.voidExpense(expenseId, request.getReason());
        return ResponseEntity.ok(toExpenseResponseDTO(voided, budgetId, categoryNames));
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private Map<Long, String> getCategoryNameMap(Long budgetId) {
        return budgetService.getCategoriesByBudgetId(budgetId).stream()
                .collect(Collectors.toMap(BudgetCategory::getId, BudgetCategory::getName));
    }

    private BudgetExpense getAndValidateExpenseBelongsToBudget(
            Long budgetId, Long expenseId, Map<Long, String> categoryNames) {
        BudgetExpense expense = budgetExpenseService.getExpenseById(expenseId);

        Long categoryId = expense.getCategory().getId();
        if (!categoryNames.containsKey(categoryId)) {
            throw new IllegalArgumentException(
                    "Expense ID " + expenseId + " does not belong to budget ID " + budgetId
            );
        }

        return expense;
    }

    private ExpenseResponseDTO toExpenseResponseDTO(
            BudgetExpense expense, Long budgetId, Map<Long, String> categoryNames) {
        Long categoryId = expense.getCategory().getId();
        String categoryName = categoryNames.get(categoryId);
        return ExpenseResponseDTO.fromEntity(expense, budgetId, categoryId, categoryName);
    }
}
