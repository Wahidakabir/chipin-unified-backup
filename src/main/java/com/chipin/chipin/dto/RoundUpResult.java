package com.chipin.chipin.dto;

import java.math.BigDecimal;

// The outcome of rounding up one payment.
public class RoundUpResult {

    private final BigDecimal paymentAmount;
    private final BigDecimal roundUpAmount;  // the basic round-up
    private final BigDecimal boosterAmount;  // extra from boosters
    private final String rulesApplied;

    public RoundUpResult(BigDecimal paymentAmount, BigDecimal roundUpAmount,
                         BigDecimal boosterAmount, String rulesApplied) {
        this.paymentAmount = paymentAmount;
        this.roundUpAmount = roundUpAmount;
        this.boosterAmount = boosterAmount;
        this.rulesApplied = rulesApplied;
    }

    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public BigDecimal getRoundUpAmount() { return roundUpAmount; }
    public BigDecimal getBoosterAmount() { return boosterAmount; }
    public String getRulesApplied() { return rulesApplied; }
    public BigDecimal getTotalSaved() { return roundUpAmount.add(boosterAmount); }
    public BigDecimal getTotalCharged() { return paymentAmount.add(getTotalSaved()); }
}
