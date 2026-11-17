package com.chipin.chipin.service;

import com.chipin.chipin.dto.DebtEdge;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure debt maths for Feature 1 (Debt Simplification Engine).
 * No database or Spring code here, so it is easy to unit test.
 *
 * Idea: it does not matter WHO you owe, only your net balance
 * (what you are owed minus what you owe). Once everyone's net balance is
 * known, people who owe are matched with people who are owed.
 */
public class DebtSimplifier {

    // Above this many people the exact search gets slow, so we only use the greedy matching.
    private static final int EXACT_LIMIT = 16;

    // Net balance per member: positive = is owed money, negative = owes money.
    public static Map<String, BigDecimal> netBalances(List<DebtEdge> debts) {
        Map<String, BigDecimal> balances = new TreeMap<>();
        for (DebtEdge d : debts) {
            balances.merge(d.getFromUserId(), d.getAmount().negate(), BigDecimal::add);
            balances.merge(d.getToUserId(), d.getAmount(), BigDecimal::add);
        }
        return balances;
    }

    // The "before" picture: one arrow per pair of members, after cancelling
    // debts that run in opposite directions between the same two people.
    public static List<DebtEdge> pairwiseDebts(List<DebtEdge> debts) {
        // key "A|B" with A < B alphabetically; positive value means A owes B
        Map<String, BigDecimal> perPair = new TreeMap<>();
        for (DebtEdge d : debts) {
            String from = d.getFromUserId();
            String to = d.getToUserId();
            if (from.equals(to)) continue;
            if (from.compareTo(to) < 0) {
                perPair.merge(from + "|" + to, d.getAmount(), BigDecimal::add);
            } else {
                perPair.merge(to + "|" + from, d.getAmount().negate(), BigDecimal::add);
            }
        }

        List<DebtEdge> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : perPair.entrySet()) {
            String[] pair = e.getKey().split("\\|");
            BigDecimal amount = e.getValue();
            if (amount.signum() > 0) {
                result.add(new DebtEdge(pair[0], pair[1], amount));
            } else if (amount.signum() < 0) {
                result.add(new DebtEdge(pair[1], pair[0], amount.negate()));
            }
        }
        return result;
    }

    // The "after" picture: the fewest payments that bring every balance to zero.
    public static List<DebtEdge> simplify(Map<String, BigDecimal> balances) {
        // Keep only members with a non-zero balance, in whole paisa, in a stable order.
        List<String> names = new ArrayList<>();
        List<Long> paisa = new ArrayList<>();
        long sum = 0;
        for (Map.Entry<String, BigDecimal> e : new TreeMap<>(balances).entrySet()) {
            long p = e.getValue().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
            if (p != 0) {
                names.add(e.getKey());
                paisa.add(p);
                sum += p;
            }
        }
        if (sum != 0) {
            throw new IllegalArgumentException("Balances must add up to zero.");
        }

        List<DebtEdge> payments = new ArrayList<>();
        for (List<Integer> group : splitIntoZeroSumGroups(paisa)) {
            settleGroup(group, names, paisa, payments);
        }
        return payments;
    }

    // A group of k people whose balances cancel out needs at most k - 1 payments.
    // So the more separate "cancelling" groups we find, the fewer payments overall.
    // This tries every way of grouping (bitmask dynamic programming) and keeps the
    // one with the most groups. best[mask] = most groups using the members in mask.
    private static List<List<Integer>> splitIntoZeroSumGroups(List<Long> paisa) {
        int n = paisa.size();
        List<List<Integer>> groups = new ArrayList<>();
        if (n == 0) {
            return groups;
        }
        if (n > EXACT_LIMIT) {
            List<Integer> everyone = new ArrayList<>();
            for (int i = 0; i < n; i++) everyone.add(i);
            groups.add(everyone);
            return groups;
        }

        int full = (1 << n) - 1;
        long[] sumOf = new long[full + 1];
        int[] best = new int[full + 1];
        int[] removed = new int[full + 1]; // which member was removed to reach the best smaller mask

        for (int mask = 1; mask <= full; mask++) {
            int lowest = Integer.numberOfTrailingZeros(mask);
            sumOf[mask] = sumOf[mask & (mask - 1)] + paisa.get(lowest);

            int bestSmaller = -1;
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) == 0) continue;
                int smaller = mask ^ (1 << i);
                if (best[smaller] > bestSmaller) {
                    bestSmaller = best[smaller];
                    removed[mask] = i;
                }
            }
            best[mask] = bestSmaller + (sumOf[mask] == 0 ? 1 : 0);
        }

        // Walk back from "everyone" to "nobody". Each time the remaining members
        // cancel out again, the members removed since the last time form one group.
        int mask = full;
        int boundary = full;
        while (mask != 0) {
            mask ^= (1 << removed[mask]);
            if (sumOf[mask] == 0) {
                int groupMask = boundary ^ mask;
                List<Integer> group = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    if ((groupMask & (1 << i)) != 0) group.add(i);
                }
                groups.add(group);
                boundary = mask;
            }
        }
        return groups;
    }

    // Inside one group: the person who owes the most pays the person who is owed the most,
    // repeat until everyone is at zero. Each payment clears at least one person.
    private static void settleGroup(List<Integer> group, List<String> names, List<Long> paisa,
                                    List<DebtEdge> payments) {
        long[] left = new long[names.size()];
        for (int i : group) left[i] = paisa.get(i);

        while (true) {
            int debtor = -1;
            int creditor = -1;
            for (int i : group) {
                if (left[i] < 0 && (debtor == -1 || left[i] < left[debtor])) debtor = i;
                if (left[i] > 0 && (creditor == -1 || left[i] > left[creditor])) creditor = i;
            }
            if (debtor == -1 || creditor == -1) {
                return;
            }
            long pay = Math.min(-left[debtor], left[creditor]);
            left[debtor] += pay;
            left[creditor] -= pay;
            payments.add(new DebtEdge(names.get(debtor), names.get(creditor), BigDecimal.valueOf(pay, 2)));
        }
    }
}
