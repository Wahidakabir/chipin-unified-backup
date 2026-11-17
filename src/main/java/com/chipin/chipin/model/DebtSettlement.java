package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// A payment one member made to another to clear debt (Feature 1).
@Entity
@Table(name = "debt_settlements")
public class DebtSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String fromUserId;
    private String toUserId;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private String recordedByUserId;
    private LocalDateTime settledAt;

    public DebtSettlement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getRecordedByUserId() { return recordedByUserId; }
    public void setRecordedByUserId(String recordedByUserId) { this.recordedByUserId = recordedByUserId; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
}
