package com.chipin.chipin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for partially updating an active expense transaction in Feature 11.
 * All fields are optional; omitted (null) fields preserve their existing values.
 * Changes to payerId, externalExpenseId, createdAt, and status are strictly prohibited.
 */
public class UpdateExpenseRequest {

    /** Optional category reassignment (must belong to the same budget). */
    private Long categoryId;

    @Positive(message = "Expense amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Expense amount cannot exceed 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    @Size(max = 255, message = "Expense description cannot exceed 255 characters")
    private String description;

    private LocalDate expenseDate;

    private String payerName;

    public UpdateExpenseRequest() {
    }

    public UpdateExpenseRequest(Long categoryId, BigDecimal amount, String description,
                                LocalDate expenseDate, String payerName) {
        this.categoryId = categoryId;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
        this.payerName = payerName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }
}
