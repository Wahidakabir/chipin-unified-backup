package com.chipin.chipin.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_expenses")
public class EmergencyExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String requestedByUserId;

    @NotBlank(message = "Emergency title is required")
    private String title;

    @NotBlank(message = "Urgency level is required")
    private String urgencyLevel; // "CRITICAL", "HIGH", "MODERATE"

    private String reason;

    @Positive(message = "Target amount must be greater than 0")
    private double targetAmount;

    private double collectedAmount = 0.0;
    private String status; // "ACTIVE", "FULFILLED"
    private LocalDateTime createdAt;

    public EmergencyExpense() {}

    public double getRemainingAmount() {
        return Math.max(0.0, Math.round((targetAmount - collectedAmount) * 100.0) / 100.0);
    }

    public int getProgressPercentage() {
        if (targetAmount <= 0) return 0;
        return (int) Math.min(100, Math.round((collectedAmount / targetAmount) * 100.0));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getRequestedByUserId() { return requestedByUserId; }
    public void setRequestedByUserId(String requestedByUserId) { this.requestedByUserId = requestedByUserId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public void setUrgencyLevel(String urgencyLevel) { this.urgencyLevel = urgencyLevel; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public double getCollectedAmount() { return collectedAmount; }
    public void setCollectedAmount(double collectedAmount) { this.collectedAmount = collectedAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}