package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// One member's round-up rules (Feature 3). One row per member per group.
@Entity
@Table(name = "round_up_settings")
public class RoundUpSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String userId;

    // Master switch: when off, payments are not rounded up at all.
    private boolean enabled;

    // Payments are rounded up to the next multiple of this (10, 50 or 100).
    @Column(precision = 12, scale = 2)
    private BigDecimal roundTo;

    // Booster: double the round-up on weekends.
    private boolean weekendDoubleEnabled;

    // Booster: add a fixed amount after every payment.
    private boolean fixedExtraEnabled;

    @Column(precision = 12, scale = 2)
    private BigDecimal fixedExtraAmount;

    // Where the saved money goes: PERSONAL or GROUP, plus the goal's name.
    private String destinationType;
    private String destinationName;

    public RoundUpSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public BigDecimal getRoundTo() { return roundTo; }
    public void setRoundTo(BigDecimal roundTo) { this.roundTo = roundTo; }
    public boolean isWeekendDoubleEnabled() { return weekendDoubleEnabled; }
    public void setWeekendDoubleEnabled(boolean weekendDoubleEnabled) { this.weekendDoubleEnabled = weekendDoubleEnabled; }
    public boolean isFixedExtraEnabled() { return fixedExtraEnabled; }
    public void setFixedExtraEnabled(boolean fixedExtraEnabled) { this.fixedExtraEnabled = fixedExtraEnabled; }
    public BigDecimal getFixedExtraAmount() { return fixedExtraAmount; }
    public void setFixedExtraAmount(BigDecimal fixedExtraAmount) { this.fixedExtraAmount = fixedExtraAmount; }
    public String getDestinationType() { return destinationType; }
    public void setDestinationType(String destinationType) { this.destinationType = destinationType; }
    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
}
