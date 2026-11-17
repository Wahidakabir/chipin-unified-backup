package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One logged use of a shared asset by a member.
@Entity
@Table(name = "asset_usages")
public class AssetUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long assetId;
    private String userId;
    private LocalDateTime usedAt;

    public AssetUsage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
