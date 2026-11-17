package com.chipin.chipin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing category's allocation allowance in Feature 11.
 */
public class UpdateCategoryRequest {

    @NotNull(message = "Allocated amount is required")
    @Positive(message = "Allocated amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Allocated amount cannot exceed 13 integer digits and 2 decimal places")
    private BigDecimal allocatedAmount;

    public UpdateCategoryRequest() {
    }

    public UpdateCategoryRequest(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }
}
