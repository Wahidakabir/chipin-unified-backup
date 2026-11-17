package com.chipin.chipin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for recording a financial contribution toward a savings goal.
 */
public class RecordContributionRequest {

    @NotBlank(message = "Member ID is required")
    private String memberId;

    @NotBlank(message = "Member name is required")
    private String memberName;

    @NotNull(message = "Contribution amount is required")
    @Positive(message = "Contribution amount must be strictly greater than zero")
    private BigDecimal amount;

    private String note;

    public RecordContributionRequest() {
    }

    public RecordContributionRequest(String memberId, String memberName, BigDecimal amount, String note) {
        this.memberId = memberId;
        this.memberName = memberName;
        this.amount = amount;
        this.note = note;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
