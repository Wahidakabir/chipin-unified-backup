package com.chipin.chipin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure money maths for Feature 2 (Itemized Receipt Splitting).
 * No database or Spring code here, so it is easy to unit test.
 */
public class BillSplitCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // Splits amount across weights so the parts sum to exactly amount (2 decimals).
    // Uses the largest-remainder method: everyone gets their share rounded down to
    // the paisa, then the leftover paisa go one each to the largest fractions.
    public static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        if (amount == null || weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException("Amount and weights are required.");
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            if (w == null || w.signum() < 0) {
                throw new IllegalArgumentException("Weights cannot be negative.");
            }
            totalWeight = totalWeight.add(w);
        }
        if (totalWeight.signum() == 0) {
            throw new IllegalArgumentException("Total weight must be greater than 0.");
        }

        // Work in whole paisa. A negative amount (e.g. a discount bigger than the
        // tax and tip) is split as a positive number and flipped back at the end.
        boolean negative = amount.signum() < 0;
        long totalPaisa = amount.abs().multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact();

        int n = weights.size();
        long[] paisa = new long[n];
        BigDecimal[] remainders = new BigDecimal[n];
        long handedOut = 0;

        for (int i = 0; i < n; i++) {
            // exact share = totalPaisa * weight / totalWeight
            BigDecimal[] divided = BigDecimal.valueOf(totalPaisa)
                    .multiply(weights.get(i))
                    .divideAndRemainder(totalWeight);
            paisa[i] = divided[0].longValueExact();
            remainders[i] = divided[1];
            handedOut += paisa[i];
        }

        // Give the leftover paisa to the biggest remainders (earlier person wins a tie).
        long leftover = totalPaisa - handedOut;
        boolean[] gotExtra = new boolean[n];
        for (long k = 0; k < leftover; k++) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (gotExtra[i]) continue;
                if (best == -1 || remainders[i].compareTo(remainders[best]) > 0) {
                    best = i;
                }
            }
            paisa[best] += 1;
            gotExtra[best] = true;
        }

        List<BigDecimal> parts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BigDecimal part = BigDecimal.valueOf(paisa[i], 2); // paisa -> taka with 2 decimals
            parts.add(negative ? part.negate() : part);
        }
        return parts;
    }

    // itemAmounts: item id -> amount; assignees: item id -> user ids sharing it.
    // Returns user id -> subtotal.
    public static Map<String, BigDecimal> subtotals(
            Map<Long, BigDecimal> itemAmounts, Map<Long, List<String>> assignees) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        for (Map.Entry<Long, BigDecimal> item : itemAmounts.entrySet()) {
            List<String> users = assignees.get(item.getKey());
            if (users == null || users.isEmpty()) {
                throw new IllegalArgumentException("Every item must be assigned to at least one member.");
            }

            // Equal split of this item among the people who shared it.
            List<BigDecimal> equalWeights = new ArrayList<>();
            for (int i = 0; i < users.size(); i++) {
                equalWeights.add(BigDecimal.ONE);
            }
            List<BigDecimal> parts = allocate(item.getValue(), equalWeights);

            for (int i = 0; i < users.size(); i++) {
                result.merge(users.get(i), parts.get(i), BigDecimal::add);
            }
        }
        return result;
    }

    // Returns user id -> final total (subtotal + share of the extras).
    // Extras (tax + service charge + tip - discount) follow each person's subtotal,
    // so someone who ordered more also pays more of the tax and tip.
    public static Map<String, BigDecimal> totals(
            Map<String, BigDecimal> subtotals, BigDecimal extras) {
        List<String> users = new ArrayList<>(subtotals.keySet());
        List<BigDecimal> weights = new ArrayList<>(subtotals.values());
        List<BigDecimal> extraParts = allocate(extras, weights);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < users.size(); i++) {
            result.put(users.get(i), weights.get(i).add(extraParts.get(i)));
        }
        return result;
    }
}
