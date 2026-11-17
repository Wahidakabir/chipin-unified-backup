package com.chipin.chipin.controller;

import org.springframework.web.bind.annotation.CrossOrigin;

import com.chipin.chipin.dto.BudgetResponseDTO;
import com.chipin.chipin.dto.BudgetSpendingResponseDTO;
import com.chipin.chipin.dto.BudgetSummaryResponseDTO;
import com.chipin.chipin.dto.CategoryResponseDTO;
import com.chipin.chipin.dto.CreateBudgetRequest;
import com.chipin.chipin.dto.CreateCategoryRequest;
import com.chipin.chipin.dto.UpdateBudgetRequest;
import com.chipin.chipin.dto.UpdateCategoryRequest;
import com.chipin.chipin.model.BudgetCategory;
import com.chipin.chipin.model.BudgetStatus;
import com.chipin.chipin.model.GroupBudget;
import com.chipin.chipin.service.BudgetExpenseService;
import com.chipin.chipin.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * REST controller managing group budget configurations, category allocations,
 * lifecycle transitions, and real-time spending summaries for Feature 11.
 * Delegates all business invariants and database transactions to the service layer.
 */
@RestController
@RequestMapping("/api/budgets")
@CrossOrigin
public class BudgetApiController {

    private final BudgetService budgetService;
    private final BudgetExpenseService budgetExpenseService;

    public BudgetApiController(BudgetService budgetService, BudgetExpenseService budgetExpenseService) {
        this.budgetService = budgetService;
        this.budgetExpenseService = budgetExpenseService;
    }

    // ==========================================
    // 1. Budget Endpoints
    // ==========================================

    /**
     * 1. Creates a new GroupBudget with ACTIVE status.
     * POST /api/budgets
     */
    @PostMapping
    public ResponseEntity<BudgetResponseDTO> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        GroupBudget budget = budgetService.createBudget(
                request.getGroupId(),
                request.getCreatorId(),
                request.getName(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                request.getTotalLimit()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toBudgetResponseDTO(budget));
    }

    /**
     * 2. Retrieves a single budget by its ID with all category allowances and financial metrics.
     * GET /api/budgets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponseDTO> getBudgetById(@PathVariable("id") Long id) {
        GroupBudget budget = budgetService.getBudgetById(id);
        return ResponseEntity.ok(toBudgetResponseDTO(budget));
    }

    /**
     * 3. Retrieves all budgets for a group with optional status filtering.
     * GET /api/budgets?groupId=...&status=...
     */
    @GetMapping
    public ResponseEntity<List<BudgetSummaryResponseDTO>> getBudgetsByGroupId(
            @RequestParam("groupId") String groupId,
            @RequestParam(value = "status", required = false) BudgetStatus status) {
        if (groupId == null || groupId.trim().isEmpty()) {
            throw new IllegalArgumentException("Group ID is required and cannot be blank");
        }

        List<GroupBudget> budgets = (status != null)
                ? budgetService.getBudgetsByGroupIdAndStatus(groupId.trim(), status)
                : budgetService.getBudgetsByGroupId(groupId.trim());

        List<BudgetSummaryResponseDTO> summaries = budgets.stream()
                .map(this::toBudgetSummaryResponseDTO)
                .toList();

        return ResponseEntity.ok(summaries);
    }

    /**
     * 4. Updates basic budget details (name, period dates, total limit).
     * PUT /api/budgets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponseDTO> updateBudget(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        GroupBudget updated = budgetService.updateBudget(
                id,
                request.getName(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                request.getTotalLimit()
        );
        return ResponseEntity.ok(toBudgetResponseDTO(updated));
    }

    /**
     * 5. Transitions a budget to ARCHIVED terminal status.
     * POST /api/budgets/{id}/archive
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<BudgetResponseDTO> archiveBudget(@PathVariable("id") Long id) {
        GroupBudget archived = budgetService.archiveBudget(id);
        return ResponseEntity.ok(toBudgetResponseDTO(archived));
    }

    /**
     * 6. Retrieves real-time spending summaries and overrun analysis for a budget and its categories.
     * GET /api/budgets/{id}/spending
     */
    @GetMapping("/{id}/spending")
    public ResponseEntity<BudgetSpendingResponseDTO> getBudgetSpending(@PathVariable("id") Long id) {
        GroupBudget budget = budgetService.getBudgetById(id);
        BigDecimal totalLimit = budget.getTotalLimit();
        BigDecimal totalAllocated = budgetService.calculateTotalAllocatedAmount(id);
        BigDecimal unallocated = budgetService.calculateUnallocatedAmount(id);
        BigDecimal totalSpent = budgetExpenseService.calculateTotalBudgetSpending(id);
        BigDecimal remainingLimit = totalLimit.subtract(totalSpent).setScale(2, RoundingMode.HALF_UP);
        boolean isBudgetOverrun = budgetExpenseService.isBudgetOverrun(id);
        BigDecimal budgetOverrunAmount = budgetExpenseService.getBudgetOverrunAmount(id);
        boolean hasAnyCategoryOverrun = budgetExpenseService.hasAnyCategoryOverrun(id);

        List<BudgetCategory> categories = budgetService.getCategoriesByBudgetId(id);
        List<CategoryResponseDTO> categoryDTOs = categories.stream()
                .map(cat -> toCategoryResponseDTO(cat, id))
                .toList();

        BudgetSpendingResponseDTO spendingDTO = new BudgetSpendingResponseDTO(
                id,
                totalLimit,
                totalAllocated,
                unallocated,
                totalSpent,
                remainingLimit,
                isBudgetOverrun,
                budgetOverrunAmount,
                hasAnyCategoryOverrun,
                categoryDTOs
        );

        return ResponseEntity.ok(spendingDTO);
    }

    // ==========================================
    // 2. Category Endpoints
    // ==========================================

    /**
     * 7. Creates and allocates a new spending category under a budget.
     * POST /api/budgets/{budgetId}/categories
     */
    @PostMapping("/{budgetId}/categories")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @PathVariable("budgetId") Long budgetId,
            @Valid @RequestBody CreateCategoryRequest request) {
        BudgetCategory category = budgetService.allocateCategory(
                budgetId,
                request.getName(),
                request.getAllocatedAmount()
        );
        CategoryResponseDTO response = CategoryResponseDTO.fromEntity(
                category,
                BigDecimal.ZERO,
                budgetId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 8. Lists all spending categories allocated under a budget with real-time spent figures.
     * GET /api/budgets/{budgetId}/categories
     */
    @GetMapping("/{budgetId}/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getCategoriesByBudgetId(
            @PathVariable("budgetId") Long budgetId) {
        List<BudgetCategory> categories = budgetService.getCategoriesByBudgetId(budgetId);
        List<CategoryResponseDTO> response = categories.stream()
                .map(cat -> toCategoryResponseDTO(cat, budgetId))
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 9. Updates the monetary allocation of an existing spending category.
     * PUT /api/budgets/{budgetId}/categories/{categoryId}
     */
    @PutMapping("/{budgetId}/categories/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> updateCategoryAllocation(
            @PathVariable("budgetId") Long budgetId,
            @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        BudgetCategory updated = budgetService.updateCategoryAllocation(
                budgetId,
                categoryId,
                request.getAllocatedAmount()
        );
        return ResponseEntity.ok(toCategoryResponseDTO(updated, budgetId));
    }

    // ==========================================
    // 3. Mapping Helpers
    // ==========================================

    private BudgetResponseDTO toBudgetResponseDTO(GroupBudget budget) {
        Long budgetId = budget.getId();
        BigDecimal totalAllocated = budgetService.calculateTotalAllocatedAmount(budgetId);
        BigDecimal unallocated = budgetService.calculateUnallocatedAmount(budgetId);
        BigDecimal totalSpent = budgetExpenseService.calculateTotalBudgetSpending(budgetId);
        boolean isOverrun = budgetExpenseService.isBudgetOverrun(budgetId);
        BigDecimal overrunAmount = budgetExpenseService.getBudgetOverrunAmount(budgetId);

        List<BudgetCategory> categories = budgetService.getCategoriesByBudgetId(budgetId);
        List<CategoryResponseDTO> categoryDTOs = categories.stream()
                .map(cat -> toCategoryResponseDTO(cat, budgetId))
                .toList();

        return BudgetResponseDTO.fromEntity(
                budget,
                totalAllocated,
                unallocated,
                totalSpent,
                isOverrun,
                overrunAmount,
                categoryDTOs
        );
    }

    private BudgetSummaryResponseDTO toBudgetSummaryResponseDTO(GroupBudget budget) {
        Long budgetId = budget.getId();
        BigDecimal totalAllocated = budgetService.calculateTotalAllocatedAmount(budgetId);
        BigDecimal unallocated = budgetService.calculateUnallocatedAmount(budgetId);
        BigDecimal totalSpent = budgetExpenseService.calculateTotalBudgetSpending(budgetId);
        boolean isOverrun = budgetExpenseService.isBudgetOverrun(budgetId);
        BigDecimal overrunAmount = budgetExpenseService.getBudgetOverrunAmount(budgetId);
        int categoryCount = budgetService.getCategoriesByBudgetId(budgetId).size();

        return BudgetSummaryResponseDTO.fromEntity(
                budget,
                totalAllocated,
                unallocated,
                totalSpent,
                isOverrun,
                overrunAmount,
                categoryCount
        );
    }

    private CategoryResponseDTO toCategoryResponseDTO(BudgetCategory category, Long budgetId) {
        BigDecimal spent = budgetExpenseService.calculateCategorySpending(category.getId());
        return CategoryResponseDTO.fromEntity(category, spent, budgetId);
    }
}
