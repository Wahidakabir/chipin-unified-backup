package com.chipin.chipin.service;

import com.chipin.chipin.dto.RoundUpOverview;
import com.chipin.chipin.dto.RoundUpResult;
import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.model.RoundUpSaving;
import com.chipin.chipin.model.RoundUpSettings;
import com.chipin.chipin.repository.RoundUpSavingRepository;
import com.chipin.chipin.repository.RoundUpSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Feature 3: Round-Up with Boosters.
 *
 * Each time a member pays a settlement, the payment is rounded up and the
 * spare change is saved to the goal that member chose. Members control their
 * own rules: the rounding step, a weekend double, and a fixed extra amount.
 */
@Service
public class RoundUpService {

    public static final String PERSONAL = "PERSONAL";
    public static final String GROUP = "GROUP";

    private static final List<BigDecimal> ALLOWED_STEPS =
            List.of(new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("100"));
    private static final List<String> EXAMPLE_PAYMENTS = List.of("237", "1235", "76.67");

    private final RoundUpSettingsRepository settingsRepository;
    private final RoundUpSavingRepository savingRepository;

    public RoundUpService(RoundUpSettingsRepository settingsRepository,
                          RoundUpSavingRepository savingRepository) {
        this.settingsRepository = settingsRepository;
        this.savingRepository = savingRepository;
    }

    // ---------- Reading ----------

    // A member's saved rules, or the defaults if they have never changed them.
    public RoundUpSettings getSettings(String groupId, String userId) {
        return settingsRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseGet(() -> defaultSettings(groupId, userId));
    }

    public RoundUpOverview getOverview(String groupId, String userId) {
        RoundUpSettings settings = getSettings(groupId, userId);

        BigDecimal mine = BigDecimal.ZERO.setScale(2);
        BigDecimal minePersonal = BigDecimal.ZERO.setScale(2);
        BigDecimal groupGoals = BigDecimal.ZERO.setScale(2);
        List<RoundUpSaving> myHistory = new ArrayList<>();

        for (RoundUpSaving saving : savingRepository.findByGroupIdOrderByCreatedAtDesc(groupId)) {
            boolean toGroup = GROUP.equals(saving.getDestinationType());
            if (toGroup) {
                groupGoals = groupGoals.add(saving.getTotalSaved());
            }
            if (userId.equals(saving.getUserId())) {
                myHistory.add(saving);
                mine = mine.add(saving.getTotalSaved());
                if (!toGroup) {
                    minePersonal = minePersonal.add(saving.getTotalSaved());
                }
            }
        }

        // Show what the current rules would do to a few sample payments made today.
        boolean weekendToday = RoundUpCalculator.isWeekend(LocalDate.now());
        List<RoundUpResult> examples = new ArrayList<>();
        if (settings.isEnabled()) {
            for (String sample : EXAMPLE_PAYMENTS) {
                examples.add(calculate(new BigDecimal(sample), settings, weekendToday));
            }
        }

        return new RoundUpOverview(settings, mine, minePersonal, groupGoals, myHistory, examples, weekendToday);
    }

    // The saving made from one settlement, if round-up was on for the payer.
    public Optional<RoundUpSaving> findForSettlement(Long settlementId) {
        return savingRepository.findBySettlementId(settlementId).stream().findFirst();
    }

    // ---------- Writing ----------

    @Transactional
    public RoundUpSettings saveSettings(String groupId, String userId, boolean enabled, BigDecimal roundTo,
                                        boolean weekendDoubleEnabled, boolean fixedExtraEnabled,
                                        BigDecimal fixedExtraAmount, String destinationType,
                                        String destinationName) {
        if (roundTo == null || ALLOWED_STEPS.stream().noneMatch(step -> step.compareTo(roundTo) == 0)) {
            throw new IllegalArgumentException("Round up to the nearest 10, 50 or 100.");
        }
        if (fixedExtraEnabled && (fixedExtraAmount == null || fixedExtraAmount.signum() <= 0)) {
            throw new IllegalArgumentException("Enter a fixed amount greater than 0, or switch that booster off.");
        }
        if (fixedExtraAmount != null && fixedExtraAmount.signum() < 0) {
            throw new IllegalArgumentException("The fixed amount cannot be negative.");
        }
        if (!PERSONAL.equals(destinationType) && !GROUP.equals(destinationType)) {
            throw new IllegalArgumentException("Choose a personal goal or a group goal.");
        }
        if (destinationName == null || destinationName.isBlank()) {
            throw new IllegalArgumentException("Give your savings goal a name.");
        }

        RoundUpSettings settings = getSettings(groupId, userId);
        settings.setEnabled(enabled);
        settings.setRoundTo(roundTo.setScale(2, RoundingMode.HALF_UP));
        settings.setWeekendDoubleEnabled(weekendDoubleEnabled);
        settings.setFixedExtraEnabled(fixedExtraEnabled);
        settings.setFixedExtraAmount(fixedExtraAmount == null
                ? BigDecimal.ZERO.setScale(2)
                : fixedExtraAmount.setScale(2, RoundingMode.HALF_UP));
        settings.setDestinationType(destinationType);
        settings.setDestinationName(destinationName.trim());
        return settingsRepository.save(settings);
    }

    // Called by the debt feature right after a payment is recorded.
    // Rounds the payment up using the PAYER's rules and saves the spare change.
    // Returns nothing saved if the payer has round-up off or there is nothing to save.
    @Transactional
    public Optional<RoundUpSaving> applyToSettlement(DebtSettlement settlement) {
        RoundUpSettings settings = getSettings(settlement.getGroupId(), settlement.getFromUserId());
        if (!settings.isEnabled()) {
            return Optional.empty();
        }

        LocalDateTime paidAt = settlement.getSettledAt() != null ? settlement.getSettledAt() : LocalDateTime.now();
        RoundUpResult result = calculate(settlement.getAmount(), settings,
                RoundUpCalculator.isWeekend(paidAt.toLocalDate()));
        if (result.getTotalSaved().signum() <= 0) {
            return Optional.empty();
        }

        RoundUpSaving saving = new RoundUpSaving();
        saving.setGroupId(settlement.getGroupId());
        saving.setUserId(settlement.getFromUserId());
        saving.setSettlementId(settlement.getId());
        saving.setPaymentAmount(result.getPaymentAmount());
        saving.setRoundUpAmount(result.getRoundUpAmount());
        saving.setBoosterAmount(result.getBoosterAmount());
        saving.setTotalSaved(result.getTotalSaved());
        saving.setDestinationType(settings.getDestinationType());
        saving.setDestinationName(settings.getDestinationName());
        saving.setRulesApplied(result.getRulesApplied());
        saving.setCreatedAt(paidAt);
        return Optional.of(savingRepository.save(saving));
    }

    // ---------- Helpers ----------

    private RoundUpResult calculate(BigDecimal payment, RoundUpSettings settings, boolean weekend) {
        BigDecimal fixedExtra = settings.isFixedExtraEnabled() ? settings.getFixedExtraAmount() : null;
        return RoundUpCalculator.calculate(payment, settings.getRoundTo(), weekend,
                settings.isWeekendDoubleEnabled(), fixedExtra);
    }

    // New members start with round-up on, to the next 10, into a personal goal, no boosters.
    private RoundUpSettings defaultSettings(String groupId, String userId) {
        RoundUpSettings settings = new RoundUpSettings();
        settings.setGroupId(groupId);
        settings.setUserId(userId);
        settings.setEnabled(true);
        settings.setRoundTo(new BigDecimal("10.00"));
        settings.setWeekendDoubleEnabled(false);
        settings.setFixedExtraEnabled(false);
        settings.setFixedExtraAmount(new BigDecimal("5.00"));
        settings.setDestinationType(PERSONAL);
        settings.setDestinationName("My savings");
        return settings;
    }
}
