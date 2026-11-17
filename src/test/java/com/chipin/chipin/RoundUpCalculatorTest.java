package com.chipin.chipin;

import com.chipin.chipin.dto.RoundUpResult;
import com.chipin.chipin.service.RoundUpCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RoundUpCalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void roundsUpToNextTen() {
        // 237 becomes 240, so 3 is saved
        assertEquals(bd("3.00"), RoundUpCalculator.roundUpAmount(bd("237"), bd("10")));
    }

    @Test
    void roundsUpToNextFifty() {
        assertEquals(bd("13.00"), RoundUpCalculator.roundUpAmount(bd("237"), bd("50")));
    }

    @Test
    void handlesPaisa() {
        // 76.67 becomes 80.00
        assertEquals(bd("3.33"), RoundUpCalculator.roundUpAmount(bd("76.67"), bd("10")));
    }

    @Test
    void paymentAlreadyOnAMultipleSavesNothing() {
        assertEquals(bd("0.00"), RoundUpCalculator.roundUpAmount(bd("240"), bd("10")));
    }

    @Test
    void weekendDoubleOnlyAppliesOnWeekends() {
        RoundUpResult weekday = RoundUpCalculator.calculate(bd("237"), bd("10"), false, true, null);
        RoundUpResult weekend = RoundUpCalculator.calculate(bd("237"), bd("10"), true, true, null);

        assertEquals(bd("3.00"), weekday.getTotalSaved());
        assertEquals(bd("6.00"), weekend.getTotalSaved());
    }

    @Test
    void fixedExtraIsAddedAfterEveryPayment() {
        // even a payment with nothing to round still saves the fixed amount
        RoundUpResult result = RoundUpCalculator.calculate(bd("240"), bd("10"), false, false, bd("5"));

        assertEquals(bd("0.00"), result.getRoundUpAmount());
        assertEquals(bd("5.00"), result.getBoosterAmount());
        assertEquals(bd("5.00"), result.getTotalSaved());
    }

    @Test
    void boostersStack() {
        // 237 -> 3 round-up, doubled on a weekend (+3), plus 5 fixed = 11 saved, 248 charged
        RoundUpResult result = RoundUpCalculator.calculate(bd("237"), bd("10"), true, true, bd("5"));

        assertEquals(bd("3.00"), result.getRoundUpAmount());
        assertEquals(bd("8.00"), result.getBoosterAmount());
        assertEquals(bd("11.00"), result.getTotalSaved());
        assertEquals(bd("248.00"), result.getTotalCharged());
    }

    @Test
    void weekendIsFridayAndSaturday() {
        assertEquals(true, RoundUpCalculator.isWeekend(LocalDate.of(2026, 11, 20)));  // Friday
        assertEquals(true, RoundUpCalculator.isWeekend(LocalDate.of(2026, 11, 21)));  // Saturday
        assertEquals(false, RoundUpCalculator.isWeekend(LocalDate.of(2026, 11, 22))); // Sunday
    }

    @Test
    void rejectsZeroPayment() {
        assertThrows(IllegalArgumentException.class, () ->
                RoundUpCalculator.roundUpAmount(bd("0"), bd("10")));
    }
}
