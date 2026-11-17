package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_contributions")
public class EmergencyContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long emergencyExpenseId;
    private String contributorUserId;
    private double amount;
    private LocalDateTime contributedAt;

    public EmergencyContribution() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmergencyExpenseId() { return emergencyExpenseId; }
    public void setEmergencyExpenseId(Long emergencyExpenseId) { this.emergencyExpenseId = emergencyExpenseId; }
    public String getContributorUserId() { return contributorUserId; }
    public void setContributorUserId(String contributorUserId) { this.contributorUserId = contributorUserId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDateTime getContributedAt() { return contributedAt; }
    public void setContributedAt(LocalDateTime contributedAt) { this.contributedAt = contributedAt; }
}