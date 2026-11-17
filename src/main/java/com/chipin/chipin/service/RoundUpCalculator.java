package com.chipin.chipin.service;

import com.chipin.chipin.dto.RoundUpResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure money maths for Feature 3 (Round-Up with Boosters).
 * No database or Spring code here, so it is easy to unit test.
 */
public class RoundUpCalculator {

    // The weekend in Bangladesh is Friday and Saturday.
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY;
    }

    // How much is needed to reach the next multiple of roundTo.
    // 237 with roundTo 10 -> 3. A payment already on a multiple (240) -> 0.
    public static BigDecimal roundUpAmount(BigDecimal payment, BigDecimal roundTo) {
        if (payment == null || payment.signum() <= 0) {
            throw new IllegalArgumentException("Payment must be greater than 0.");
        }
        if (roundTo == null || roundTo.signum() <= 0) {
            throw new IllegalArgumentException("Round-up step must be greater than 0.");
        }
        BigDecimal steps = payment.divide(roundTo, 0, RoundingMode.CEILING); // whole steps, rounded up
        return steps.multiply(roundTo).subtract(payment).setScale(2, RoundingMode.HALF_UP);
    }

    // Applies the round-up and any boosters that are switched on.
    //   weekendDouble: on a weekend the round-up is doubled
    //   fixedExtra:    a set amount added after every payment (null or 0 = off)
    public static RoundUpResult calculate(BigDecimal payment, BigDecimal roundTo, boolean weekend,
                                          boolean weekendDouble, BigDecimal fixedExtra) {
        BigDecimal base = roundUpAmount(payment, roundTo);
        BigDecimal booster = BigDecimal.ZERO.setScale(2);
        List<String> rules = new ArrayList<>();
        rules.add("Rounded up to next " + roundTo.stripTrailingZeros().toPlainString());

        if (weekendDouble && weekend && base.signum() > 0) {
            booster = booster.add(base); // doubling = adding the round-up once more
            rules.add("weekend double");
        }
        if (fixedExtra != null && fixedExtra.signum() > 0) {
            booster = booster.add(fixedExtra.setScale(2, RoundingMode.HALF_UP));
            rules.add("+" + fixedExtra.stripTrailingZeros().toPlainString() + " fixed");
        }

        return new RoundUpResult(payment.setScale(2, RoundingMode.HALF_UP), base, booster,
                String.join(", ", rules));
    }
}
