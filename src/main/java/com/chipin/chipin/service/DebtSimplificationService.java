package com.chipin.chipin.service;

import com.chipin.chipin.dto.DebtEdge;
import com.chipin.chipin.dto.DebtOverview;
import com.chipin.chipin.dto.MemberBalance;
import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.repository.DebtSettlementRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Feature 1: Debt Simplification Engine with Graph Visualization.
 *
 * Reads the approved expenses in the group ledger, works out who owes whom,
 * and reduces that to the fewest payments. Payments that members record are
 * saved as DebtSettlement rows and reduce what is still owed. Once everyone
 * is back at zero, those expenses count as settled and drop off the graphs.
 */
@Service
public class DebtSimplificationService {

    private final ExpenseRepository expenseRepository;
    private final DebtSettlementRepository settlementRepository;
    private final RoundUpService roundUpService;

    public DebtSimplificationService(ExpenseRepository expenseRepository,
                                     DebtSettlementRepository settlementRepository,
                                     RoundUpService roundUpService) {
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.roundUpService = roundUpService;
    }

    public DebtOverview getOverview(String groupId, List<String> members) {
        List<DebtSettlement> allSettlements = settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId);
        OpenPeriod open = findOpenPeriod(groupId, allSettlements);

        Map<String, BigDecimal> balances = new TreeMap<>();
        for (String member : members) {
            balances.put(member, BigDecimal.ZERO.setScale(2));
        }
        balances.putAll(DebtSimplifier.netBalances(open.allDebts()));

        List<MemberBalance> memberBalances = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : balances.entrySet()) {
            memberBalances.add(new MemberBalance(e.getKey(), e.getValue()));
        }

        List<DebtEdge> before = DebtSimplifier.pairwiseDebts(open.expenseDebts);
        List<DebtEdge> after = DebtSimplifier.simplify(balances);
        int saved = Math.max(0, before.size() - (open.settlements.size() + after.size()));

        return new DebtOverview(memberBalances, before, after, allSettlements, open.expenses.size(), saved);
    }

    @Transactional
    public DebtSettlement recordPayment(String groupId, String fromUserId, String toUserId,
                                        BigDecimal amount, String recordedByUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Choose two different members for the payment.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than 0.");
        }
        BigDecimal payment = amount.setScale(2, RoundingMode.HALF_UP);

        OpenPeriod open = findOpenPeriod(groupId,
                settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId));
        Map<String, BigDecimal> balances = DebtSimplifier.netBalances(open.allDebts());
        BigDecimal owes = balances.getOrDefault(fromUserId, BigDecimal.ZERO).negate();
        BigDecimal isOwed = balances.getOrDefault(toUserId, BigDecimal.ZERO);

        if (owes.signum() <= 0) {
            throw new IllegalStateException(fromUserId + " does not owe anything right now.");
        }
        if (isOwed.signum() <= 0) {
            throw new IllegalStateException(toUserId + " is not owed anything right now.");
        }
        if (payment.compareTo(owes) > 0 || payment.compareTo(isOwed) > 0) {
            throw new IllegalStateException("Payment is larger than what is owed (maximum ৳"
                    + owes.min(isOwed) + ").");
        }

        return saveSettlement(groupId, fromUserId, toUserId, payment, recordedByUserId);
    }

    @Transactional
    public int settleAll(String groupId, List<String> members, String recordedByUserId) {
        List<DebtEdge> payments = getOverview(groupId, members).getSimplifiedPayments();
        for (DebtEdge p : payments) {
            saveSettlement(groupId, p.getFromUserId(), p.getToUserId(), p.getAmount(), recordedByUserId);
        }
        return payments.size();
    }

    // Saves the payment, then lets Feature 3 round it up into the payer's savings.
    private DebtSettlement saveSettlement(String groupId, String fromUserId, String toUserId,
                                          BigDecimal amount, String recordedByUserId) {
        DebtSettlement settlement = new DebtSettlement();
        settlement.setGroupId(groupId);
        settlement.setFromUserId(fromUserId);
        settlement.setToUserId(toUserId);
        settlement.setAmount(amount);
        settlement.setRecordedByUserId(recordedByUserId);
        settlement.setSettledAt(LocalDateTime.now());
        DebtSettlement saved = settlementRepository.save(settlement);
        roundUpService.applyToSettlement(saved);
        return saved;
    }

    private static class OpenPeriod {
        final List<Expense> expenses = new ArrayList<>();
        final List<DebtSettlement> settlements = new ArrayList<>();
        final List<DebtEdge> expenseDebts = new ArrayList<>();

        List<DebtEdge> allDebts() {
            List<DebtEdge> all = new ArrayList<>(expenseDebts);
            for (DebtSettlement s : settlements) {
                all.add(new DebtEdge(s.getToUserId(), s.getFromUserId(), s.getAmount()));
            }
            return all;
        }
    }

    private OpenPeriod findOpenPeriod(String groupId, List<DebtSettlement> allSettlements) {
        List<Object> events = new ArrayList<>();
        events.addAll(debtExpenses(groupId));
        events.addAll(allSettlements);
        events.sort(Comparator.comparing(this::timeOf));

        OpenPeriod open = new OpenPeriod();
        for (Object event : events) {
            if (event instanceof Expense expense) {
                open.expenses.add(expense);
                open.expenseDebts.addAll(debtsOf(expense));
            } else {
                open.settlements.add((DebtSettlement) event);
            }

            boolean everyoneAtZero = true;
            for (BigDecimal balance : DebtSimplifier.netBalances(open.allDebts()).values()) {
                if (balance.signum() != 0) {
                    everyoneAtZero = false;
                    break;
                }
            }
            if (everyoneAtZero) {
                open = new OpenPeriod();
            }
        }
        return open;
    }

    private LocalDateTime timeOf(Object event) {
        LocalDateTime time = event instanceof Expense expense
                ? expense.getCreatedAt()
                : ((DebtSettlement) event).getSettledAt();
        return time != null ? time : LocalDateTime.MIN;
    }

    private List<Expense> debtExpenses(String groupId) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenseRepository.findByGroupId(groupId)) {
            boolean approved = "APPROVED".equals(e.getStatus());
            boolean emergency = "Emergency Expense".equals(e.getCategory());
            if (approved && !emergency && e.getPaidByUserId() != null && e.getSplits() != null) {
                result.add(e);
            }
        }
        return result;
    }

    private List<DebtEdge> debtsOf(Expense e) {
        List<DebtEdge> debts = new ArrayList<>();
        String payer = e.getPaidByUserId();
        for (Map.Entry<String, Double> split : e.getSplits().entrySet()) {
            if (split.getValue() == null || split.getKey().equals(payer)) {
                continue;
            }
            BigDecimal owed = BigDecimal.valueOf(split.getValue()).setScale(2, RoundingMode.HALF_UP);
            if (owed.signum() > 0) {
                debts.add(new DebtEdge(split.getKey(), payer, owed));
            }
        }
        return debts;
    }
}