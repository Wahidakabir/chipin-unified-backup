package com.chipin.chipin.model;

import java.util.List;
import java.util.Map;

public class HealthScoreReport {
    private int overallScore;        // 0 - 100
    private String grade;            // "A+ (Excellent)", "B (Healthy)", "C (Needs Attention)", "D (At Risk)"
    private String statusColor;      // "green", "blue", "amber", "red"

    // Sub-scores (out of their respective weights)
    private int budgetDisciplineScore;   // Max 40 pts
    private int consensusRateScore;      // Max 30 pts
    private int emergencyReadinessScore; // Max 30 pts

    // Raw Financial Metrics
    private double monthlyBudgetLimit;
    private double totalApprovedSpent;
    private double remainingBudget;
    private int budgetUsagePercent;

    private long totalProposals;
    private long approvedProposals;
    private long pendingProposals;

    private long totalEmergencies;
    private long fulfilledEmergencies;

    private Map<String, Double> memberSpendingShares;
    private List<String> recommendations;

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getStatusColor() { return statusColor; }
    public void setStatusColor(String statusColor) { this.statusColor = statusColor; }
    public int getBudgetDisciplineScore() { return budgetDisciplineScore; }
    public void setBudgetDisciplineScore(int budgetDisciplineScore) { this.budgetDisciplineScore = budgetDisciplineScore; }
    public int getConsensusRateScore() { return consensusRateScore; }
    public void setConsensusRateScore(int consensusRateScore) { this.consensusRateScore = consensusRateScore; }
    public int getEmergencyReadinessScore() { return emergencyReadinessScore; }
    public void setEmergencyReadinessScore(int emergencyReadinessScore) { this.emergencyReadinessScore = emergencyReadinessScore; }
    public double getMonthlyBudgetLimit() { return monthlyBudgetLimit; }
    public void setMonthlyBudgetLimit(double monthlyBudgetLimit) { this.monthlyBudgetLimit = monthlyBudgetLimit; }
    public double getTotalApprovedSpent() { return totalApprovedSpent; }
    public void setTotalApprovedSpent(double totalApprovedSpent) { this.totalApprovedSpent = totalApprovedSpent; }
    public double getRemainingBudget() { return remainingBudget; }
    public void setRemainingBudget(double remainingBudget) { this.remainingBudget = remainingBudget; }
    public int getBudgetUsagePercent() { return budgetUsagePercent; }
    public void setBudgetUsagePercent(int budgetUsagePercent) { this.budgetUsagePercent = budgetUsagePercent; }
    public long getTotalProposals() { return totalProposals; }
    public void setTotalProposals(long totalProposals) { this.totalProposals = totalProposals; }
    public long getApprovedProposals() { return approvedProposals; }
    public void setApprovedProposals(long approvedProposals) { this.approvedProposals = approvedProposals; }
    public long getPendingProposals() { return pendingProposals; }
    public void setPendingProposals(long pendingProposals) { this.pendingProposals = pendingProposals; }
    public long getTotalEmergencies() { return totalEmergencies; }
    public void setTotalEmergencies(long totalEmergencies) { this.totalEmergencies = totalEmergencies; }
    public long getFulfilledEmergencies() { return fulfilledEmergencies; }
    public void setFulfilledEmergencies(long fulfilledEmergencies) { this.fulfilledEmergencies = fulfilledEmergencies; }
    public Map<String, Double> getMemberSpendingShares() { return memberSpendingShares; }
    public void setMemberSpendingShares(Map<String, Double> memberSpendingShares) { this.memberSpendingShares = memberSpendingShares; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}