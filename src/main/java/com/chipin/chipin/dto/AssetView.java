package com.chipin.chipin.dto;

import com.chipin.chipin.model.AssetEvent;
import com.chipin.chipin.model.SharedAsset;
import java.math.BigDecimal;
import java.util.List;

// Everything the shared assets page shows for one asset.
public class AssetView {

    private final SharedAsset asset;
    private final BigDecimal currentValue;
    private final BigDecimal monthlyDepreciation;
    private final int monthsOwned;
    private final List<AssetOwnerView> owners;
    private final List<AssetOwnerView> otherUsers; // non-owners who used it since the last cost
    private final List<String> nonOwners;          // members who could buy in
    private final int usesSinceLastCost;
    private final List<AssetEvent> events;

    public AssetView(SharedAsset asset, BigDecimal currentValue, BigDecimal monthlyDepreciation,
                     int monthsOwned, List<AssetOwnerView> owners, List<AssetOwnerView> otherUsers,
                     List<String> nonOwners, int usesSinceLastCost, List<AssetEvent> events) {
        this.asset = asset;
        this.currentValue = currentValue;
        this.monthlyDepreciation = monthlyDepreciation;
        this.monthsOwned = monthsOwned;
        this.owners = owners;
        this.otherUsers = otherUsers;
        this.nonOwners = nonOwners;
        this.usesSinceLastCost = usesSinceLastCost;
        this.events = events;
    }

    public SharedAsset getAsset() { return asset; }
    public BigDecimal getCurrentValue() { return currentValue; }
    public BigDecimal getMonthlyDepreciation() { return monthlyDepreciation; }
    public int getMonthsOwned() { return monthsOwned; }
    public List<AssetOwnerView> getOwners() { return owners; }
    public List<AssetOwnerView> getOtherUsers() { return otherUsers; }
    public List<String> getNonOwners() { return nonOwners; }
    public int getUsesSinceLastCost() { return usesSinceLastCost; }
    public List<AssetEvent> getEvents() { return events; }
    public boolean isBuyoutPossible() { return owners.size() > 1; }

    // How much of the original value is left, 0 to 100, for the value bar.
    public int getValueLeftPercent() {
        if (asset.getPurchasePrice().signum() == 0) {
            return 0;
        }
        return currentValue.multiply(new BigDecimal("100"))
                .divide(asset.getPurchasePrice(), 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    public int getMonthsLeft() {
        return Math.max(0, asset.getUsefulLifeMonths() - monthsOwned);
    }
}
