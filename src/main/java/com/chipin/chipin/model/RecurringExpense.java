package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "recurring_expenses")
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String createdByUserId;

    @NotBlank(message = "Bill title is required")
    private String title;

    private String category;

    @Positive(message = "Amount must be greater than 0")
    private double amount;

    @NotBlank(message = "Frequency is required")
    private String frequency; // "WEEKLY", "MONTHLY", "YEARLY"

    private LocalDate nextDueDate;
    private LocalDate lastProcessedDate;
    private boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recurring_expense_splits", joinColumns = @JoinColumn(name = "recurring_expense_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "share_amount")
    private Map<String, Double> splits = new HashMap<>();

    public RecurringExpense() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }
    public LocalDate getLastProcessedDate() { return lastProcessedDate; }
    public void setLastProcessedDate(LocalDate lastProcessedDate) { this.lastProcessedDate = lastProcessedDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Map<String, Double> getSplits() { return splits; }
    public void setSplits(Map<String, Double> splits) { this.splits = splits; }
}