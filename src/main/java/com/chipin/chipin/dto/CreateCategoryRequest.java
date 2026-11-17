package com.chipin.chipin.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new spending category under a GroupBudget in Feature 11.
 */
public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
    private String name;

    @NotNull(message = "Allocated amount is required")
    @Positive(message = "Allocated amount must be strictly greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Allocated amount cannot exceed 13 integer digits and 2 decimal places")
    private BigDecimal allocatedAmount;

    public CreateCategoryRequest() {
    }

    public CreateCategoryRequest(String name, BigDecimal allocatedAmount) {
        this.name = name;
        this.allocatedAmount = allocatedAmount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }
}
