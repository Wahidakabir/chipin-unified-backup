package com.chipin.chipin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for recording a new financial expense transaction against a budget category in Feature 11.
 */
public class RecordExpenseRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Payer ID is required")
    private String payerId;

    @NotBlank(message = "Payer name is required")
    private String payerName;

    @NotNull(message = "Expense amount is required")
    @Positive(message = "Expense amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Expense amount cannot exceed 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Expense description is required")
    @Size(min = 1, max = 255, message = "Expense description cannot exceed 255 characters")
    private String description;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    /**
     * Optional external expense identifier linking this transaction to Feature 2 (Receipt Splitting).
     * Must be unique when provided.
     */
    private Long externalExpenseId;

    public RecordExpenseRequest() {
    }

    public RecordExpenseRequest(Long categoryId, String payerId, String payerName,
                                BigDecimal amount, String description, LocalDate expenseDate,
                                Long externalExpenseId) {
        this.categoryId = categoryId;
        this.payerId = payerId;
        this.payerName = payerName;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.externalExpenseId = externalExpenseId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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
}
