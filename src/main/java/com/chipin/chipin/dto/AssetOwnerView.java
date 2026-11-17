package com.chipin.chipin.dto;

import java.math.BigDecimal;

// One owner of a shared asset: their share and what it is worth today.
public class AssetOwnerView {

    private final String userId;
    private final BigDecimal sharePercent;
    private final BigDecimal shareValue;
    private final int uses; // uses logged since the last cost

    public AssetOwnerView(String userId, BigDecimal sharePercent, BigDecimal shareValue, int uses) {
        this.userId = userId;
        this.sharePercent = sharePercent;
        this.shareValue = shareValue;
        this.uses = uses;
    }

    public String getUserId() { return userId; }
    public BigDecimal getSharePercent() { return sharePercent; }
    public BigDecimal getShareValue() { return shareValue; }
    public int getUses() { return uses; }
}
