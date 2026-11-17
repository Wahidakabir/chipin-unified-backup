package com.chipin.chipin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for voiding an existing expense transaction with a mandatory cancellation reason in Feature 11.
 */
public class VoidExpenseRequest {

    @NotBlank(message = "Void reason is required and cannot be blank")
    @Size(min = 1, max = 255, message = "Void reason cannot exceed 255 characters")
    private String reason;

    public VoidExpenseRequest() {
    }

    public VoidExpenseRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
