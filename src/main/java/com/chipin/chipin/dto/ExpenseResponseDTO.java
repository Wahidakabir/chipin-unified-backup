package com.chipin.chipin.dto;

import com.chipin.chipin.model.BudgetExpense;
import com.chipin.chipin.model.ExpenseStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Detailed response DTO representing an individual budget expense transaction,
 * including payer metadata, category/budget association, and historical audit voiding information.
 */
public class ExpenseResponseDTO {

    private Long id;
    private Long budgetId;
    private Long categoryId;
    private String categoryName;
    private String payerId;
    private String payerName;
    private BigDecimal amount;
    private String description;
    private LocalDate expenseDate;
    private Long externalExpenseId;
    private ExpenseStatus status;
    private boolean isVoided;
    private LocalDateTime voidedAt;
    private String voidReason;
    private LocalDateTime createdAt;

    public ExpenseResponseDTO() {
    }

    public ExpenseResponseDTO(Long id, Long budgetId, Long categoryId, String categoryName,
                              String payerId, String payerName, BigDecimal amount,
                              String description, LocalDate expenseDate, Long externalExpenseId,
                              ExpenseStatus status, boolean isVoided, LocalDateTime voidedAt,
                              String voidReason, LocalDateTime createdAt) {
        this.id = id;
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.payerId = payerId;
        this.payerName = payerName;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.externalExpenseId = externalExpenseId;
        this.status = status;
        this.isVoided = isVoided;
        this.voidedAt = voidedAt;
        this.voidReason = voidReason;
        this.createdAt = createdAt;
    }

    /**
     * Maps a BudgetExpense entity into an ExpenseResponseDTO using explicitly supplied relationship values.
     * Guarantees lazy-loading safety by strictly requiring the caller to supply pre-resolved relationship identifiers.
     *
     * @param expense the BudgetExpense entity
     * @param budgetId the parent budget ID
     * @param categoryId the category ID
     * @param categoryName the category display name
     * @return the mapped ExpenseResponseDTO, or null if expense is null
     */
    public static ExpenseResponseDTO fromEntity(BudgetExpense expense, Long budgetId, Long categoryId, String categoryName) {
        if (expense == null) {
            return null;
        }

        BigDecimal amt = expense.getAmount() != null
                ? expense.getAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new ExpenseResponseDTO(
                expense.getId(),
                budgetId,
                categoryId,
                categoryName,
                expense.getPayerId(),
                expense.getPayerName(),
                amt,
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getExternalExpenseId(),
                expense.getStatus(),
                expense.isVoided(),
                expense.getVoidedAt(),
                expense.getVoidReason(),
                expense.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
        this.description = description;
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

    public boolean isVoided() {
        return isVoided;
    }

    public void setVoided(boolean voided) {
        isVoided = voided;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
