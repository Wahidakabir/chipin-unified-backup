package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Spare change saved from one payment (Feature 3).
@Entity
@Table(name = "round_up_savings")
public class RoundUpSaving {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String userId;
    private Long settlementId; // the DebtSettlement this came from

    @Column(precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal roundUpAmount;   // the basic round-up

    @Column(precision = 12, scale = 2)
    private BigDecimal boosterAmount;   // extra added by boosters

    @Column(precision = 12, scale = 2)
    private BigDecimal totalSaved;      // roundUpAmount + boosterAmount

    private String destinationType;
    private String destinationName;
    private String rulesApplied;        // e.g. "Rounded up to next 10, weekend double"
    private LocalDateTime createdAt;

    public RoundUpSaving() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public BigDecimal getRoundUpAmount() { return roundUpAmount; }
    public void setRoundUpAmount(BigDecimal roundUpAmount) { this.roundUpAmount = roundUpAmount; }
    public BigDecimal getBoosterAmount() { return boosterAmount; }
    public void setBoosterAmount(BigDecimal boosterAmount) { this.boosterAmount = boosterAmount; }
    public BigDecimal getTotalSaved() { return totalSaved; }
    public void setTotalSaved(BigDecimal totalSaved) { this.totalSaved = totalSaved; }
    public String getDestinationType() { return destinationType; }
    public void setDestinationType(String destinationType) { this.destinationType = destinationType; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public String getRulesApplied() { return rulesApplied; }
    public void setRulesApplied(String rulesApplied) { this.rulesApplied = rulesApplied; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
