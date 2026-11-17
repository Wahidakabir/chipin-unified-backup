package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String paidByUserId;
    private String title;
    private String category;
    private double totalAmount;

    @ElementCollection
    @CollectionTable(name = "expense_splits", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "owed_amount")
    private Map<String, Double> splits = new HashMap<>();

    private String status;
    private LocalDateTime createdAt;

    public Expense() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getPaidByUserId() { return paidByUserId; }
    public void setPaidByUserId(String paidByUserId) { this.paidByUserId = paidByUserId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public Map<String, Double> getSplits() { return splits; }
    public void setSplits(Map<String, Double> splits) { this.splits = splits; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}