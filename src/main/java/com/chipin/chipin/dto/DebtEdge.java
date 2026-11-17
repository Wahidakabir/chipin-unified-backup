package com.chipin.chipin.dto;

import java.math.BigDecimal;

// One arrow in the debt graph: fromUserId owes (or should pay) toUserId this amount.
public class DebtEdge {

    private final String fromUserId;
    private final String toUserId;
    private final BigDecimal amount;

    public DebtEdge(String fromUserId, String toUserId, BigDecimal amount) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.amount = amount;
    }

    public String getFromUserId() { return fromUserId; }
    public String getToUserId() { return toUserId; }
    public BigDecimal getAmount() { return amount; }

    @Override
    public String toString() { return fromUserId + " -> " + toUserId + " " + amount; }
}
