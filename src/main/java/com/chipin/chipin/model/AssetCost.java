package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// A repair or running cost of a shared asset.
@Entity
@Table(name = "asset_costs")
public class AssetCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assetId;
    @Column(length = 1000)
    private String description;
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    private String paidByUserId;
    private String splitBasis;
    private LocalDateTime incurredAt;

    public AssetCost() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaidByUserId() { return paidByUserId; }
    public void setPaidByUserId(String paidByUserId) { this.paidByUserId = paidByUserId; }
    public String getSplitBasis() { return splitBasis; }
    public void setSplitBasis(String splitBasis) { this.splitBasis = splitBasis; }
    public LocalDateTime getIncurredAt() { return incurredAt; }
    public void setIncurredAt(LocalDateTime incurredAt) { this.incurredAt = incurredAt; }
}
