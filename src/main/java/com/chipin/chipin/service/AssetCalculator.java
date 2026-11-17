package com.chipin.chipin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure maths for Feature 5 (Shared Asset Co-Ownership).
 * No database or Spring code here, so it is easy to unit test.
 * Shares are percentages with 4 decimals (e.g. 33.3333) that always add up to exactly 100.
 */
public class AssetCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100.0000");

    // The value lost each month: purchase price divided by useful life.
    public static BigDecimal monthlyDepreciation(BigDecimal purchasePrice, int usefulLifeMonths) {
        if (usefulLifeMonths <= 0) {
            throw new IllegalArgumentException("Useful life must be at least 1 month.");
        }
        return purchasePrice.divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP);
    }

    // Whole months between the purchase date and the given date (never negative).
    public static int monthsOwned(LocalDate purchaseDate, LocalDate onDate) {
        return (int) Math.max(0, ChronoUnit.MONTHS.between(purchaseDate, onDate));
    }

    // Straight-line value: drops by the same amount every month and stops at zero.
    public static BigDecimal currentValue(BigDecimal purchasePrice, int usefulLifeMonths,
                                          LocalDate purchaseDate, LocalDate onDate) {
        if (usefulLifeMonths <= 0) {
            throw new IllegalArgumentException("Useful life must be at least 1 month.");
        }
        int monthsLeft = Math.max(0, usefulLifeMonths - monthsOwned(purchaseDate, onDate));
        return purchasePrice.multiply(BigDecimal.valueOf(monthsLeft))
                .divide(BigDecimal.valueOf(usefulLifeMonths), 2, RoundingMode.HALF_UP);
    }

    // Ownership by how much each member paid. 6000 and 2000 -> 75% and 25%.
    public static Map<String, BigDecimal> sharesFromContributions(Map<String, BigDecimal> contributions) {
        return splitPercent(HUNDRED, contributions);
    }

    // Splits a percentage between members by weight, keeping 4 decimals.
    // (splitByWeights works to 2 decimals, so the value is shifted by 2 places and back.)
    private static Map<String, BigDecimal> splitPercent(BigDecimal percent, Map<String, BigDecimal> weights) {
        Map<String, BigDecimal> shifted = splitByWeights(percent.movePointRight(2), weights);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : shifted.entrySet()) {
            result.put(e.getKey(), e.getValue().movePointLeft(2));
        }
        return result;
    }

    // Splits an amount between members in proportion to their weights
    // (ownership shares, or number of uses). Parts add up to exactly the amount.
    public static Map<String, BigDecimal> splitByWeights(BigDecimal amount, Map<String, BigDecimal> weights) {
        List<String> users = new ArrayList<>(weights.keySet());
        List<BigDecimal> parts = BillSplitCalculator.allocate(amount, new ArrayList<>(weights.values()));
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 0; i < users.size(); i++) {
            result.put(users.get(i), parts.get(i));
        }
        return result;
    }

    // What a share is worth right now: share% of the current value.
    public static BigDecimal shareValue(BigDecimal sharePercent, BigDecimal currentValue) {
        return currentValue.multiply(sharePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    // A member leaves: the others take over the leaver's share in proportion
    // to what they already own. Returns the new shares (without the leaver).
    public static Map<String, BigDecimal> sharesAfterBuyout(Map<String, BigDecimal> shares, String leaver) {
        if (!shares.containsKey(leaver)) {
            throw new IllegalArgumentException(leaver + " does not own a share of this asset.");
        }
        Map<String, BigDecimal> remaining = new LinkedHashMap<>(shares);
        BigDecimal leaving = remaining.remove(leaver);
        if (remaining.isEmpty()) {
            throw new IllegalArgumentException("The last owner cannot be bought out.");
        }
        Map<String, BigDecimal> extra = splitPercent(leaving, remaining);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : remaining.entrySet()) {
            result.put(e.getKey(), e.getValue().add(extra.get(e.getKey())));
        }
        return result;
    }

    // A member joins with a chosen share: existing owners each give up part of
    // theirs in proportion to what they own. Returns the new shares (with the joiner).
    public static Map<String, BigDecimal> sharesAfterBuyIn(Map<String, BigDecimal> shares,
                                                           String joiner, BigDecimal joinPercent) {
        if (shares.containsKey(joiner)) {
            throw new IllegalArgumentException(joiner + " already owns a share of this asset.");
        }
        if (joinPercent == null || joinPercent.signum() <= 0 || joinPercent.compareTo(HUNDRED) >= 0) {
            throw new IllegalArgumentException("The new share must be more than 0% and less than 100%.");
        }
        BigDecimal share = joinPercent.setScale(4, RoundingMode.HALF_UP);
        Map<String, BigDecimal> givenUp = splitPercent(share, shares);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : shares.entrySet()) {
            result.put(e.getKey(), e.getValue().subtract(givenUp.get(e.getKey())));
        }
        result.put(joiner, share);
        return result;
    }
}
