package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "group_budget_configs")
public class GroupBudgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String groupId;

    @Positive(message = "Monthly budget limit must be greater than 0")
    private double monthlyBudgetLimit = 25000.0;

    public GroupBudgetConfig() {}

    public GroupBudgetConfig(String groupId, double monthlyBudgetLimit) {
        this.groupId = groupId;
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public double getMonthlyBudgetLimit() { return monthlyBudgetLimit; }
    public void setMonthlyBudgetLimit(double monthlyBudgetLimit) { this.monthlyBudgetLimit = monthlyBudgetLimit; }
}