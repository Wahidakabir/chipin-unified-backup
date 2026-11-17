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

/**
 * Represents an individual expense transaction recorded under a BudgetCategory.
 * 
 * AUDIT INTEGRITY:
 * Expenses cannot be permanently deleted. Instead, they can be marked VOIDED to preserve
 * historical financial transparency and audit trails.
 */
@Entity
@Table(
    name = "budget_expenses",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_budget_expenses_external_expense_id",
            columnNames = {"external_expense_id"}
        )
    },
    indexes = {
        @Index(name = "idx_budget_expenses_category_id", columnList = "category_id"),
        @Index(name = "idx_budget_expenses_expense_date", columnList = "expense_date"),
        @Index(name = "idx_budget_expenses_payer_id", columnList = "payer_id"),
        @Index(name = "idx_budget_expenses_status", columnList = "status")
    }
)
public class BudgetExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Category reference is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private BudgetCategory category;

    /** Member ID of the person who paid the expense. */
    @NotBlank(message = "Payer ID is required")
    @Column(name = "payer_id", nullable = false)
    private String payerId;

    /** Member name snapshot for audit history. */
    @NotBlank(message = "Payer name is required")
    @Column(name = "payer_name", nullable = false)
    private String payerName;

    /**
     * Exact monetary amount of the expense.
     * Uses BigDecimal(15, 2) to eliminate rounding errors.
     */
    @NotNull(message = "Expense amount is required")
    @Positive(message = "Expense amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Expense amount cannot exceed 15 digits with 2 decimal places")
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotBlank(message = "Expense description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @NotNull(message = "Expense date is required")
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /**
     * Optional foreign identifier linking this budget expenditure to an external
     * itemized split expense (e.g. Feature 2 Itemized Receipt Splitting).
     * Decouples modules during development while enabling clean integration.
     * Guaranteed unique when non-null to prevent duplicate external expense linking.
     */
    @Column(name = "external_expense_id")
    private Long externalExpenseId;

    /**
     * Transaction state (RECORDED or VOIDED).
     */
    @NotNull(message = "Expense status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.RECORDED;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Size(max = 255, message = "Void reason must not exceed 255 characters")
    @Column(name = "void_reason", length = 255)
    private String voidReason;

    /**
     * Optional transient in-memory field; not persisted because the PostgreSQL
     * budget_expenses table does not contain a created_by column.
     */
    @Transient
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional transient in-memory timestamp; not persisted because the PostgreSQL
     * budget_expenses table does not contain an updated_at column.
     */
    @Transient
    private LocalDateTime updatedAt;

    // --- Constructors ---

    public BudgetExpense() {
    }

    public BudgetExpense(BudgetCategory category, String payerId, String payerName,
                         BigDecimal amount, String description, LocalDate expenseDate,
                         String createdBy) {
        this.category = category;
        this.payerId = payerId;
        this.payerName = payerName;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.createdBy = createdBy;
        this.status = ExpenseStatus.RECORDED;
    }

    // --- Lifecycle Callbacks ---

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ExpenseStatus.RECORDED;
        }
        if (this.description != null) {
            this.description = this.description.trim();
        }
        validateAmount();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.description != null) {
            this.description = this.description.trim();
        }
        validateAmount();
    }

    private void validateAmount() {
        if (this.amount == null) {
            throw new IllegalArgumentException("Expense amount is required");
        }
        if (this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be strictly greater than zero");
        }
        if (this.amount.scale() > 2) {
            throw new IllegalArgumentException("Expense amount cannot have more than 2 decimal places");
        }
        if (this.amount.precision() - this.amount.scale() > 13) {
            throw new IllegalArgumentException("Expense amount cannot exceed 13 integer digits");
        }
    }

    // --- Business Logic Helper Methods ---

    /**
     * Voids this expense transaction.
     * Preserves row in database while removing amount from active spending tallies.
     *
     * @param reason the explanation for voiding (required)
     * @throws IllegalStateException if the expense is already voided
     * @throws IllegalArgumentException if reason is blank
     */
    public void markVoided(String reason) {
        if (this.status == ExpenseStatus.VOIDED) {
            throw new IllegalStateException("Expense transaction is already voided");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Void reason is required");
        }
        this.status = ExpenseStatus.VOIDED;
        this.voidReason = reason.trim();
        this.voidedAt = LocalDateTime.now();
    }

    public void voidExpense(String reason) {
        markVoided(reason);
    }

    /**
     * Checks if this transaction is actively contributing to budget spending.
     *
     * @return true if RECORDED, false if VOIDED
     */
    public boolean isActive() {
        return this.status == ExpenseStatus.RECORDED;
    }

    public boolean isVoided() {
        return this.status == ExpenseStatus.VOIDED;
    }

    // --- Getters & Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BudgetCategory getCategory() {
        return category;
    }

    public void setCategory(BudgetCategory category) {
        this.category = category;
    }

    public String getPayerId() {
        return payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public Long getExternalExpenseId() {
        return externalExpenseId;
    }

    public void setExternalExpenseId(Long externalExpenseId) {
        this.externalExpenseId = externalExpenseId;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public LocalDateTime getVoidedAt() {
        return voidedAt;
    }

    public void setVoidedAt(LocalDateTime voidedAt) {
        this.voidedAt = voidedAt;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void setVoidReason(String voidReason) {
        this.voidReason = voidReason;
    }

    public String getCreatedBy() {
        return createdBy != null ? createdBy : payerId;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
