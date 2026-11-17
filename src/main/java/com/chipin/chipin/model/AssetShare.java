package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

// One member's ownership share of a shared asset, as a percentage. Shares of an asset add up to 100.
@Entity
@Table(name = "asset_shares")
public class AssetShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assetId;
    private String userId;
    @Column(precision = 9, scale = 4)
    private BigDecimal sharePercent; // 4 decimals, e.g. 33.3333

    public AssetShare() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getSharePercent() { return sharePercent; }
    public void setSharePercent(BigDecimal sharePercent) { this.sharePercent = sharePercent; }
}
