package com.chipin.chipin.dto;

import java.math.BigDecimal;

// A member's net position: positive = is owed money, negative = owes money.
public class MemberBalance {

    private final String userId;
    private final BigDecimal balance;

    public MemberBalance(String userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public String getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getAbsoluteBalance() { return balance.abs(); }
    public boolean isOwed() { return balance.signum() > 0; }
    public boolean isOwing() { return balance.signum() < 0; }
}
