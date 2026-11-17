package com.chipin.chipin;

import com.chipin.chipin.service.BillSplitCalculator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BillSplitCalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void allocateSplitsHundredIntoThreeExactly() {
        List<BigDecimal> parts = BillSplitCalculator.allocate(
                bd("100.00"), List.of(bd("1"), bd("1"), bd("1")));
        assertEquals(List.of(bd("33.34"), bd("33.33"), bd("33.33")), parts);
    }

    @Test
    void allocateIsProportionalToWeights() {
        List<BigDecimal> parts = BillSplitCalculator.allocate(
                bd("200.00"), List.of(bd("450"), bd("750"), bd("300")));
        assertEquals(List.of(bd("60.00"), bd("100.00"), bd("40.00")), parts);
    }

    @Test
    void allocateRejectsZeroTotalWeight() {
        assertThrows(IllegalArgumentException.class, () ->
                BillSplitCalculator.allocate(bd("50.00"), List.of(bd("0"), bd("0"))));
    }

    @Test
    void subtotalsSplitSharedItemsEqually() {
        Map<Long, BigDecimal> items = new LinkedHashMap<>();
        items.put(1L, bd("900.00")); // pizza, shared by all three
        items.put(2L, bd("150.00")); // drink, Arif only
        items.put(3L, bd("450.00")); // pasta, Bushra only
        Map<Long, List<String>> who = new LinkedHashMap<>();
        who.put(1L, List.of("Arif", "Bushra", "Chaity"));
        who.put(2L, List.of("Arif"));
        who.put(3L, List.of("Bushra"));

        Map<String, BigDecimal> result = BillSplitCalculator.subtotals(items, who);

        assertEquals(bd("450.00"), result.get("Arif"));
        assertEquals(bd("750.00"), result.get("Bushra"));
        assertEquals(bd("300.00"), result.get("Chaity"));
    }

    @Test
    void totalsShareExtrasProportionally() {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        subtotals.put("Arif", bd("450.00"));
        subtotals.put("Bushra", bd("750.00"));
        subtotals.put("Chaity", bd("300.00"));

        // tax 150 + tip 100 - discount 50 = 200
        Map<String, BigDecimal> result = BillSplitCalculator.totals(subtotals, bd("200.00"));

        assertEquals(bd("510.00"), result.get("Arif"));
        assertEquals(bd("850.00"), result.get("Bushra"));
        assertEquals(bd("340.00"), result.get("Chaity"));
    }

    @Test
    void allocateGivesLeftoverPaisaToLargestRemainders() {
        // 0.05 split 1:1:1 -> 0.0166.. each, so two people get the extra paisa
        List<BigDecimal> parts = BillSplitCalculator.allocate(
                bd("0.05"), List.of(bd("1"), bd("1"), bd("1")));
        assertEquals(List.of(bd("0.02"), bd("0.02"), bd("0.01")), parts);
    }

    @Test
    void allocateHandlesNegativeAmountForBigDiscounts() {
        List<BigDecimal> parts = BillSplitCalculator.allocate(
                bd("-100.00"), List.of(bd("1"), bd("3")));
        assertEquals(List.of(bd("-25.00"), bd("-75.00")), parts);
    }

    @Test
    void subtotalsRejectItemWithNobodyAssigned() {
        Map<Long, BigDecimal> items = new LinkedHashMap<>();
        items.put(1L, bd("100.00"));
        Map<Long, List<String>> who = new LinkedHashMap<>();
        who.put(1L, List.of());
        assertThrows(IllegalArgumentException.class, () ->
                BillSplitCalculator.subtotals(items, who));
    }

    @Test
    void totalsAlwaysAddUpToTheWholeBill() {
        Map<String, BigDecimal> subtotals = new LinkedHashMap<>();
        subtotals.put("Arif", bd("333.33"));
        subtotals.put("Bushra", bd("333.33"));
        subtotals.put("Chaity", bd("333.34"));

        Map<String, BigDecimal> result = BillSplitCalculator.totals(subtotals, bd("100.00"));

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : result.values()) sum = sum.add(v);
        assertEquals(bd("1100.00"), sum);
    }
}
