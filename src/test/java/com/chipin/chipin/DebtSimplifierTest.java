package com.chipin.chipin;

import com.chipin.chipin.dto.DebtEdge;
import com.chipin.chipin.service.DebtSimplifier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DebtSimplifierTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private static DebtEdge owes(String from, String to, String amount) {
        return new DebtEdge(from, to, bd(amount));
    }

    @Test
    void netBalanceIsOwedMinusOwes() {
        Map<String, BigDecimal> balances = DebtSimplifier.netBalances(List.of(
                owes("Arif", "Bushra", "100.00"),
                owes("Bushra", "Chaity", "40.00")));

        assertEquals(bd("-100.00"), balances.get("Arif"));
        assertEquals(bd("60.00"), balances.get("Bushra"));
        assertEquals(bd("40.00"), balances.get("Chaity"));
    }

    @Test
    void chainOfDebtsBecomesOnePayment() {
        // Arif owes Bushra 100, Bushra owes Chaity 100 -> Arif just pays Chaity
        List<DebtEdge> debts = List.of(
                owes("Arif", "Bushra", "100.00"),
                owes("Bushra", "Chaity", "100.00"));

        List<DebtEdge> payments = DebtSimplifier.simplify(DebtSimplifier.netBalances(debts));

        assertEquals(1, payments.size());
        assertEquals("Arif", payments.get(0).getFromUserId());
        assertEquals("Chaity", payments.get(0).getToUserId());
        assertEquals(bd("100.00"), payments.get(0).getAmount());
    }

    @Test
    void circleOfDebtsNeedsNoPayments() {
        // A owes B, B owes C, C owes A the same amount -> everything cancels
        List<DebtEdge> debts = List.of(
                owes("Arif", "Bushra", "50.00"),
                owes("Bushra", "Chaity", "50.00"),
                owes("Chaity", "Arif", "50.00"));

        assertEquals(0, DebtSimplifier.simplify(DebtSimplifier.netBalances(debts)).size());
    }

    @Test
    void paymentsClearEveryBalance() {
        List<DebtEdge> debts = List.of(
                owes("Arif", "Bushra", "510.00"),
                owes("Chaity", "Bushra", "340.00"),
                owes("Bushra", "Dipu", "125.50"),
                owes("Arif", "Dipu", "74.25"),
                owes("Dipu", "Chaity", "20.00"));
        Map<String, BigDecimal> balances = new LinkedHashMap<>(DebtSimplifier.netBalances(debts));

        for (DebtEdge p : DebtSimplifier.simplify(balances)) {
            balances.merge(p.getFromUserId(), p.getAmount(), BigDecimal::add);
            balances.merge(p.getToUserId(), p.getAmount().negate(), BigDecimal::add);
        }

        for (BigDecimal left : balances.values()) {
            assertEquals(0, left.signum());
        }
    }

    @Test
    void findsFewerPaymentsThanSimpleMatching() {
        // Matching biggest debtor with biggest creditor needs 4 payments here.
        // Spotting that Eva and Dipu cancel exactly (7 and 7) gets it down to 3.
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        balances.put("Arif", bd("-9.00"));
        balances.put("Bushra", bd("3.00"));
        balances.put("Chaity", bd("6.00"));
        balances.put("Dipu", bd("7.00"));
        balances.put("Eva", bd("-7.00"));

        assertEquals(3, DebtSimplifier.simplify(balances).size());
    }

    @Test
    void pairwiseDebtsCancelOppositeDirections() {
        List<DebtEdge> pairs = DebtSimplifier.pairwiseDebts(List.of(
                owes("Arif", "Bushra", "100.00"),
                owes("Bushra", "Arif", "30.00")));

        assertEquals(1, pairs.size());
        assertEquals("Arif", pairs.get(0).getFromUserId());
        assertEquals("Bushra", pairs.get(0).getToUserId());
        assertEquals(bd("70.00"), pairs.get(0).getAmount());
    }

    @Test
    void simplifyRejectsBalancesThatDoNotAddUpToZero() {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        balances.put("Arif", bd("-10.00"));
        balances.put("Bushra", bd("5.00"));

        assertThrows(IllegalArgumentException.class, () -> DebtSimplifier.simplify(balances));
    }
}
