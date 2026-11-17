package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// One line in an asset's history: what happened, the value then, and the shares afterwards.
@Entity
@Table(name = "asset_events")
public class AssetEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assetId;
    private String type;
    @Column(length = 1000)
    private String description;
    @Column(precision = 12, scale = 2)
    private BigDecimal valueAtTime;
    @Column(length = 1000)
    private String sharesSnapshot;
    private LocalDateTime createdAt;

    public AssetEvent() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getValueAtTime() { return valueAtTime; }
    public void setValueAtTime(BigDecimal valueAtTime) { this.valueAtTime = valueAtTime; }
    public String getSharesSnapshot() { return sharesSnapshot; }
    public void setSharesSnapshot(String sharesSnapshot) { this.sharesSnapshot = sharesSnapshot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
