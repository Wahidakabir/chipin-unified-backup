package com.chipin.chipin.dto;

import com.chipin.chipin.model.DebtSettlement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Everything the debt simplification page shows for one group.
public class DebtOverview {

    private final List<MemberBalance> balances;
    private final List<DebtEdge> rawDebts;           // "before": who owes whom directly
    private final List<DebtEdge> simplifiedPayments; // "after": fewest payments still needed to clear everything
    private final List<DebtSettlement> settlements;  // every payment recorded so far (history)
    private final int expenseCount;
    private final int paymentsSaved;

    public DebtOverview(List<MemberBalance> balances, List<DebtEdge> rawDebts,
                        List<DebtEdge> simplifiedPayments, List<DebtSettlement> settlements,
                        int expenseCount, int paymentsSaved) {
        this.balances = balances;
        this.rawDebts = rawDebts;
        this.simplifiedPayments = simplifiedPayments;
        this.settlements = settlements;
        this.expenseCount = expenseCount;
        this.paymentsSaved = paymentsSaved;
    }

    public List<MemberBalance> getBalances() { return balances; }
    public List<DebtEdge> getRawDebts() { return rawDebts; }
    public List<DebtEdge> getSimplifiedPayments() { return simplifiedPayments; }
    public List<DebtSettlement> getSettlements() { return settlements; }
    public int getExpenseCount() { return expenseCount; }
    public int getRawCount() { return rawDebts.size(); }
    public int getSimplifiedCount() { return simplifiedPayments.size(); }
    public int getPaymentsSaved() { return paymentsSaved; }
    public boolean isSettledUp() { return simplifiedPayments.isEmpty(); }

    // Bar length for a member's balance as a percentage of half the track (0 to 50),
    // scaled so the biggest balance in the group fills its half.
    public int getBarWidth(MemberBalance member) {
        BigDecimal largest = BigDecimal.ZERO;
        for (MemberBalance b : balances) {
            largest = largest.max(b.getAbsoluteBalance());
        }
        if (largest.signum() == 0) {
            return 0;
        }
        return member.getAbsoluteBalance()
                .multiply(new BigDecimal("50"))
                .divide(largest, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
