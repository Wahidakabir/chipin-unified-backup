package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an overall group budget in Feature 11: Group Budget Management System.
 * Defines a spending cap and category allocations for a specific group and time period.
 */
@Entity
@Table(
    name = "group_budgets",
    indexes = {
        @Index(name = "idx_group_budgets_group_id", columnList = "group_id"),
        @Index(name = "idx_group_budgets_status", columnList = "status"),
        @Index(name = "idx_group_budgets_period", columnList = "period_start, period_end")
    }
)
public class GroupBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Group reference owning this budget. Consistent with Feature 15 conventions. */
    @NotBlank(message = "Group ID is required")
    @Column(name = "group_id", nullable = false)
    private String groupId;

    /** Member reference who created this budget. */
    @NotBlank(message = "Creator ID is required")
    @Column(name = "creator_id", nullable = false)
    private String creatorId;

    @NotBlank(message = "Budget name is required")
    @Size(min = 1, max = 100, message = "Budget name must be between 1 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Start date of the budget period (inclusive). */
    @NotNull(message = "Period start date is required")
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /** End date of the budget period (inclusive). */
    @NotNull(message = "Period end date is required")
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /**
     * Overall monetary limit for the group budget.
     * Uses BigDecimal(15, 2) to eliminate floating-point rounding errors.
     */
    @NotNull(message = "Total limit is required")
    @Positive(message = "Total limit must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Total limit cannot exceed 15 digits with 2 decimal places")
    @Column(name = "total_limit", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalLimit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BudgetStatus status = BudgetStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Spending categories allocated under this budget.
     * Safe financial cascade: persist and merge only; no blind deletion cascade
     * that could destroy category and expense audit history.
     */
    @OneToMany(mappedBy = "budget", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BudgetCategory> categories = new ArrayList<>();

    // --- Constructors ---

    public GroupBudget() {
    }

    public GroupBudget(String groupId, String creatorId, String name,
                       LocalDate periodStart, LocalDate periodEnd, BigDecimal totalLimit) {
        this.groupId = groupId;
        this.creatorId = creatorId;
        this.name = name;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.totalLimit = totalLimit;
        this.status = BudgetStatus.ACTIVE;
    }

    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BudgetStatus.ACTIVE;
        }
        if (this.name != null) {
            this.name = this.name.trim();
        }
        validateState();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.name != null) {
            this.name = this.name.trim();
        }
        validateState();
    }

    private void validateState() {
        validatePeriod();
        validateTotalLimit();
    }

    private void validatePeriod() {
        if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
            throw new IllegalArgumentException("Budget period start date cannot be after period end date");
        }
    }

    private void validateTotalLimit() {
        if (totalLimit == null) {
            throw new IllegalArgumentException("Total limit is required");
        }
        if (totalLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total limit must be strictly greater than zero");
        }
        if (totalLimit.scale() > 2) {
            throw new IllegalArgumentException("Total limit cannot have more than 2 decimal places");
        }
        if (totalLimit.precision() - totalLimit.scale() > 13) {
            throw new IllegalArgumentException("Total limit cannot exceed 13 integer digits");
        }
    }

    // --- Helper Methods ---

    public void addCategory(BudgetCategory category) {
        if (category != null) {
            this.categories.add(category);
            category.setBudget(this);
        }
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getTotalLimit() {
        return totalLimit;
    }

    public void setTotalLimit(BigDecimal totalLimit) {
        this.totalLimit = totalLimit;
    }

    public BudgetStatus getStatus() {
        return status;
    }

    public void setStatus(BudgetStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<BudgetCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<BudgetCategory> categories) {
        this.categories = categories;
    }
}
