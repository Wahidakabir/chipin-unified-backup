package com.chipin.chipin;

import com.chipin.chipin.service.AssetCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssetCalculatorTest {

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    // shares are kept to 4 decimals; compare them at 2
    private static BigDecimal pct(BigDecimal share) { return share.setScale(2, RoundingMode.HALF_UP); }

    private static Map<String, BigDecimal> map(String... kv) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], bd(kv[i + 1]));
        return m;
    }

    @Test
    void valueDropsBySameAmountEveryMonth() {
        // 24000 over 24 months loses 1000 a month; after 6 months it is worth 18000
        LocalDate bought = LocalDate.of(2026, 1, 15);
        assertEquals(bd("1000.00"), AssetCalculator.monthlyDepreciation(bd("24000"), 24));
        assertEquals(bd("18000.00"),
                AssetCalculator.currentValue(bd("24000"), 24, bought, LocalDate.of(2026, 7, 15)));
    }

    @Test
    void valueNeverGoesBelowZero() {
        LocalDate bought = LocalDate.of(2020, 1, 1);
        assertEquals(bd("0.00"),
                AssetCalculator.currentValue(bd("24000"), 24, bought, LocalDate.of(2026, 1, 1)));
    }

    @Test
    void ownershipFollowsWhatEachMemberPaid() {
        Map<String, BigDecimal> shares = AssetCalculator.sharesFromContributions(
                map("Arif", "6000", "Bushra", "2000"));
        assertEquals(bd("75.00"), pct(shares.get("Arif")));
        assertEquals(bd("25.00"), pct(shares.get("Bushra")));
    }

    @Test
    void sharesAlwaysAddUpToOneHundred() {
        Map<String, BigDecimal> shares = AssetCalculator.sharesFromContributions(
                map("Arif", "1000", "Bushra", "1000", "Chaity", "1000"));
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal s : shares.values()) sum = sum.add(s);
        assertEquals(bd("100.00"), pct(sum));
        assertEquals(0, sum.compareTo(bd("100"))); // exactly 100, not 99.99
    }

    @Test
    void costIsSplitByShareOfUses() {
        // Arif used it 3 times, Bushra once -> Arif pays three quarters of an 800 repair
        Map<String, BigDecimal> split = AssetCalculator.splitByWeights(bd("800.00"),
                map("Arif", "3", "Bushra", "1"));
        assertEquals(bd("600.00"), split.get("Arif"));
        assertEquals(bd("200.00"), split.get("Bushra"));
    }

    @Test
    void buyoutPriceIsShareOfCurrentValue() {
        assertEquals(bd("4500.00"), AssetCalculator.shareValue(bd("25.00"), bd("18000.00")));
    }

    @Test
    void remainingOwnersTakeOverLeaversShareInProportion() {
        // Chaity (20%) leaves. Arif (60%) and Bushra (20%) own 3:1, so they take 15 and 5.
        Map<String, BigDecimal> after = AssetCalculator.sharesAfterBuyout(
                map("Arif", "60.00", "Bushra", "20.00", "Chaity", "20.00"), "Chaity");
        assertEquals(bd("75.00"), pct(after.get("Arif")));
        assertEquals(bd("25.00"), pct(after.get("Bushra")));
        assertEquals(false, after.containsKey("Chaity"));
    }

    @Test
    void existingOwnersGiveUpShareInProportionWhenSomeoneJoins() {
        // Dipu buys 20%. Arif (75%) and Bushra (25%) give up 15 and 5.
        Map<String, BigDecimal> after = AssetCalculator.sharesAfterBuyIn(
                map("Arif", "75.00", "Bushra", "25.00"), "Dipu", bd("20"));
        assertEquals(bd("60.00"), pct(after.get("Arif")));
        assertEquals(bd("20.00"), pct(after.get("Bushra")));
        assertEquals(bd("20.00"), pct(after.get("Dipu")));
    }

    @Test
    void lastOwnerCannotBeBoughtOut() {
        assertThrows(IllegalArgumentException.class, () ->
                AssetCalculator.sharesAfterBuyout(map("Arif", "100.00"), "Arif"));
    }

    @Test
    void joinShareMustBeBelowOneHundred() {
        assertThrows(IllegalArgumentException.class, () ->
                AssetCalculator.sharesAfterBuyIn(map("Arif", "100.00"), "Dipu", bd("100")));
    }
}
