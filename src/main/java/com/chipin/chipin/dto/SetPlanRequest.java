package com.chipin.chipin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for setting or updating a member's planned contribution pledge for a savings goal.
 */
public class SetPlanRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotBlank(message = "Member name is required")
    private String memberName;

    @NotNull(message = "Planned amount is required")
    @Positive(message = "Planned amount must be strictly greater than zero")
    private BigDecimal plannedAmount;

    public SetPlanRequest() {
    }

    public SetPlanRequest(String memberId, String memberName, BigDecimal plannedAmount) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.plannedAmount = plannedAmount;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public void setPlannedAmount(BigDecimal plannedAmount) {
        this.plannedAmount = plannedAmount;
    }
}
