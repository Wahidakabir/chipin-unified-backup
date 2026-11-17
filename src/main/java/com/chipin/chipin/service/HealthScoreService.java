package com.chipin.chipin.service;

import com.chipin.chipin.model.*;
import com.chipin.chipin.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HealthScoreService {

    private final GroupBudgetConfigRepository budgetConfigRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseProposalRepository proposalRepository;
    private final EmergencyExpenseRepository emergencyRepository;

    public HealthScoreService(GroupBudgetConfigRepository budgetConfigRepository,
                              ExpenseRepository expenseRepository,
                              ExpenseProposalRepository proposalRepository,
                              EmergencyExpenseRepository emergencyRepository) {
        this.budgetConfigRepository = budgetConfigRepository;
        this.expenseRepository = expenseRepository;
        this.proposalRepository = proposalRepository;
        this.emergencyRepository = emergencyRepository;
    }

    @Transactional
    public GroupBudgetConfig getOrCreateBudgetConfig(String groupId) {
        return budgetConfigRepository.findByGroupId(groupId)
                .orElseGet(() -> budgetConfigRepository.save(new GroupBudgetConfig(groupId, 25000.0)));
    }

    @Transactional
    public void updateMonthlyBudget(String groupId, double newLimit) {
        if (newLimit <= 0) {
            throw new IllegalArgumentException("Budget limit must be greater than 0.");
        }
        GroupBudgetConfig config = getOrCreateBudgetConfig(groupId);
        config.setMonthlyBudgetLimit(Math.round(newLimit * 100.0) / 100.0);
        budgetConfigRepository.save(config);
    }

    public HealthScoreReport generateReport(String groupId, List<String> members) {
        GroupBudgetConfig config = getOrCreateBudgetConfig(groupId);
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        List<ExpenseProposal> proposals = proposalRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        List<EmergencyExpense> emergencies = emergencyRepository.findByGroupIdOrderByCreatedAtDesc(groupId);

        HealthScoreReport report = new HealthScoreReport();
        double budgetLimit = config.getMonthlyBudgetLimit();
        double totalSpent = expenses.stream().mapToDouble(Expense::getTotalAmount).sum();
        totalSpent = Math.round(totalSpent * 100.0) / 100.0;

        report.setMonthlyBudgetLimit(budgetLimit);
        report.setTotalApprovedSpent(totalSpent);
        report.setRemainingBudget(Math.round((budgetLimit - totalSpent) * 100.0) / 100.0);
        int usagePct = budgetLimit > 0 ? (int) Math.round((totalSpent / budgetLimit) * 100.0) : 0;
        report.setBudgetUsagePercent(usagePct);

        // 1. Budget Discipline Score (Max 40 pts)
        int budgetScore;
        if (totalSpent <= budgetLimit * 0.75) {
            budgetScore = 40;
        } else if (totalSpent <= budgetLimit) {
            budgetScore = 30;
        } else if (totalSpent <= budgetLimit * 1.25) {
            budgetScore = 15;
        } else {
            budgetScore = 5;
        }
        report.setBudgetDisciplineScore(budgetScore);

        // 2. Consensus & Voting Governance Score (Max 30 pts)
        long totalProps = proposals.size();
        long approvedProps = proposals.stream().filter(p -> "APPROVED".equals(p.getStatus())).count();
        long pendingProps = proposals.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
        report.setTotalProposals(totalProps);
        report.setApprovedProposals(approvedProps);
        report.setPendingProposals(pendingProps);

        int consensusScore;
        if (totalProps == 0) {
            consensusScore = 25; // Neutral baseline
        } else {
            long resolvedProps = totalProps - pendingProps;
            double resolutionRatio = (double) resolvedProps / totalProps;
            consensusScore = (int) Math.round(resolutionRatio * 30.0);
        }
        report.setConsensusRateScore(consensusScore);

        // 3. Emergency Readiness Score (Max 30 pts)
        long totalEm = emergencies.size();
        long fulfilledEm = emergencies.stream().filter(e -> "FULFILLED".equals(e.getStatus())).count();
        report.setTotalEmergencies(totalEm);
        report.setFulfilledEmergencies(fulfilledEm);

        int emergencyScore;
        if (totalEm == 0) {
            emergencyScore = 30; // No unresolved emergencies
        } else {
            double fulfillmentRatio = (double) fulfilledEm / totalEm;
            emergencyScore = (int) Math.round(fulfillmentRatio * 30.0);
        }
        report.setEmergencyReadinessScore(emergencyScore);

        // Overall Score & Grade
        int overall = Math.min(100, budgetScore + consensusScore + emergencyScore);
        report.setOverallScore(overall);

        if (overall >= 85) {
            report.setGrade("A+ (Excellent)");
            report.setStatusColor("green");
        } else if (overall >= 70) {
            report.setGrade("B (Healthy)");
            report.setStatusColor("blue");
        } else if (overall >= 50) {
            report.setGrade("C (Needs Attention)");
            report.setStatusColor("amber");
        } else {
            report.setGrade("D (At Risk)");
            report.setStatusColor("red");
        }

        // Per-member cumulative share breakdown
        Map<String, Double> memberShares = new LinkedHashMap<>();
        for (String m : members) {
            memberShares.put(m, 0.0);
        }
        for (Expense exp : expenses) {
            if (exp.getSplits() != null) {
                for (Map.Entry<String, Double> entry : exp.getSplits().entrySet()) {
                    memberShares.merge(entry.getKey(), entry.getValue(),
                            (a, b) -> Math.round((a + b) * 100.0) / 100.0);
                }
            }
        }
        report.setMemberSpendingShares(memberShares);

        // Smart Recommendations
        List<String> tips = new ArrayList<>();
        if (totalSpent > budgetLimit) {
            tips.add("Spending has exceeded your ৳" + budgetLimit + " monthly target by ৳"
                    + Math.round((totalSpent - budgetLimit) * 100.0) / 100.0 + ". Consider tightening non-essential proposals.");
        } else {
            tips.add("Group spending is within budget (৳" + report.getRemainingBudget() + " remaining this cycle).");
        }
        if (pendingProps > 0) {
            tips.add(pendingProps + " expense proposal(s) are still awaiting member votes in Feature 6.");
        }
        if (totalEm > fulfilledEm) {
            tips.add((totalEm - fulfilledEm) + " active emergency fund(s) in Feature 7 still need instant contributions.");
        }
        if (tips.size() == 1 && overall >= 85) {
            tips.add("All votes are resolved and emergency pools are fully funded. Keep it up!");
        }
        report.setRecommendations(tips);

        return report;
    }
}