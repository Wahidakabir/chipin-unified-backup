package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents an allocated spending category belonging to a GroupBudget.
 * Defines the monetary limit allocated for a specific type of expenditure (e.g. Groceries, Transport).
 */
@Entity
@Table(
    name = "budget_categories",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_budget_categories_budget_normalized_name",
            columnNames = {"budget_id", "normalized_name"}
        )
    },
    indexes = {
        @Index(name = "idx_budget_categories_budget_id", columnList = "budget_id")
    }
)
public class BudgetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Budget reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private GroupBudget budget;

    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Trimmed, lowercased category name (using Locale.ROOT) used to enforce
     * case-insensitive uniqueness per budget.
     * Prevents accidental duplicates like "Groceries" and "groceries" in the same budget.
     */
    @Column(name = "normalized_name", nullable = false, length = 100)
    private String normalizedName;

    /**
     * Spending allowance allocated to this category.
     * Uses BigDecimal(15, 2) to eliminate rounding errors.
     */
    @NotNull(message = "Allocated amount is required")
    @Positive(message = "Allocated amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Allocated amount cannot exceed 15 digits with 2 decimal places")
    @Column(name = "allocated_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Expenses recorded against this category.
     * Safe financial cascade: persist and merge only; no orphanRemoval to prevent ledger destruction.
     */
    @OneToMany(mappedBy = "category", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BudgetExpense> expenses = new ArrayList<>();

    // --- Constructors ---

    public BudgetCategory() {
    }

    public BudgetCategory(GroupBudget budget, String name, BigDecimal allocatedAmount) {
        this.budget = budget;
        setName(name);
        this.allocatedAmount = allocatedAmount;
    }

    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        validateAndNormalize();
        validateAllocatedAmount();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        validateAndNormalize();
        validateAllocatedAmount();
    }

    private void validateAndNormalize() {
        if (this.name == null || this.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        this.name = this.name.trim();
        this.normalizedName = normalize(this.name);
    }

    private void validateAllocatedAmount() {
        if (this.allocatedAmount == null) {
            throw new IllegalArgumentException("Allocated amount is required");
        }
        if (this.allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Allocated amount must be strictly greater than zero");
        }
        if (this.allocatedAmount.scale() > 2) {
            throw new IllegalArgumentException("Allocated amount cannot have more than 2 decimal places");
        }
        if (this.allocatedAmount.precision() - this.allocatedAmount.scale() > 13) {
            throw new IllegalArgumentException("Allocated amount cannot exceed 13 integer digits");
        }
    }

    /**
     * Normalizes a category name by trimming whitespace and converting to lowercase using Locale.ROOT.
     *
     * @param input the raw category name
     * @return normalized string or empty string if input is null/blank
     */
    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }

    // --- Helper Methods ---

    public void addExpense(BudgetExpense expense) {
        if (expense != null) {
            this.expenses.add(expense);
            expense.setCategory(this);
        }
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GroupBudget getBudget() {
        return budget;
    }

    public void setBudget(GroupBudget budget) {
        this.budget = budget;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        this.name = name.trim();
        this.normalizedName = normalize(this.name);
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<BudgetExpense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<BudgetExpense> expenses) {
        this.expenses = expenses;
    }
}
