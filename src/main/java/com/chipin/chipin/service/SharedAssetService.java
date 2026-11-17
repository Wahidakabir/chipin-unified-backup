package com.chipin.chipin.service;

import com.chipin.chipin.dto.AssetOwnerView;
import com.chipin.chipin.dto.AssetView;
import com.chipin.chipin.model.AssetCost;
import com.chipin.chipin.model.AssetEvent;
import com.chipin.chipin.model.AssetShare;
import com.chipin.chipin.model.AssetUsage;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.SharedAsset;
import com.chipin.chipin.repository.AssetCostRepository;
import com.chipin.chipin.repository.AssetEventRepository;
import com.chipin.chipin.repository.AssetShareRepository;
import com.chipin.chipin.repository.AssetUsageRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import com.chipin.chipin.repository.SharedAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Feature 5: Shared Asset Co-Ownership with Usage-Based Costs and Buy-In/Buyout.
 *
 * Tracks who owns what share of something the group bought together, splits its
 * running costs by actual use, and works out a fair price when a member joins
 * or leaves. Money that members owe each other is posted to the group ledger
 * as Expense rows, so it shows up in the debt feature.
 */
@Service
public class SharedAssetService {

    private final SharedAssetRepository assetRepository;
    private final AssetShareRepository shareRepository;
    private final AssetUsageRepository usageRepository;
    private final AssetCostRepository costRepository;
    private final AssetEventRepository eventRepository;
    private final ExpenseRepository expenseRepository;

    public SharedAssetService(SharedAssetRepository assetRepository, AssetShareRepository shareRepository,
                              AssetUsageRepository usageRepository, AssetCostRepository costRepository,
                              AssetEventRepository eventRepository, ExpenseRepository expenseRepository) {
        this.assetRepository = assetRepository;
        this.shareRepository = shareRepository;
        this.usageRepository = usageRepository;
        this.costRepository = costRepository;
        this.eventRepository = eventRepository;
        this.expenseRepository = expenseRepository;
    }

    // ---------- Reading ----------

    public List<AssetView> getGroupAssetViews(String groupId, List<String> members) {
        List<AssetView> views = new ArrayList<>();
        for (SharedAsset asset : assetRepository.findByGroupIdOrderByIdDesc(groupId)) {
            views.add(buildView(asset, members));
        }
        return views;
    }

    private AssetView buildView(SharedAsset asset, List<String> members) {
        BigDecimal value = valueToday(asset);
        Map<String, BigDecimal> shares = loadShares(asset.getId());
        Map<String, BigDecimal> uses = usesSinceLastCost(asset.getId());

        List<AssetOwnerView> owners = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : shares.entrySet()) {
            owners.add(new AssetOwnerView(e.getKey(), e.getValue(),
                    AssetCalculator.shareValue(e.getValue(), value), countOf(uses, e.getKey())));
        }

        List<AssetOwnerView> otherUsers = new ArrayList<>();
        int totalUses = 0;
        for (Map.Entry<String, BigDecimal> e : uses.entrySet()) {
            totalUses += e.getValue().intValue();
            if (!shares.containsKey(e.getKey())) {
                otherUsers.add(new AssetOwnerView(e.getKey(), BigDecimal.ZERO, BigDecimal.ZERO,
                        e.getValue().intValue()));
            }
        }

        List<String> nonOwners = new ArrayList<>();
        for (String member : members) {
            if (!shares.containsKey(member)) {
                nonOwners.add(member);
            }
        }

        return new AssetView(asset, value,
                AssetCalculator.monthlyDepreciation(asset.getPurchasePrice(), asset.getUsefulLifeMonths()),
                AssetCalculator.monthsOwned(asset.getPurchaseDate(), LocalDate.now()),
                owners, otherUsers, nonOwners, totalUses,
                eventRepository.findByAssetIdOrderByIdDesc(asset.getId()));
    }

    // ---------- Writing ----------

    // Registers an asset. contributions = what each member paid; the purchase
    // price is their total and each member's share follows what they paid.
    @Transactional
    public SharedAsset registerAsset(String groupId, String name, LocalDate purchaseDate, int usefulLifeMonths,
                                     Map<String, BigDecimal> contributions, String createdByUserId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Give the asset a name.");
        }
        if (usefulLifeMonths < 1) {
            throw new IllegalArgumentException("Useful life must be at least 1 month.");
        }
        LocalDate date = purchaseDate != null ? purchaseDate : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The purchase date cannot be in the future.");
        }

        // Keep only members who actually paid something.
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        BigDecimal price = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : contributions.entrySet()) {
            BigDecimal amount = e.getValue();
            if (amount == null || amount.signum() == 0) {
                continue;
            }
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("A contribution cannot be negative.");
            }
            paid.put(e.getKey(), amount.setScale(2, RoundingMode.HALF_UP));
            price = price.add(amount);
        }
        if (paid.isEmpty()) {
            throw new IllegalArgumentException("Enter what at least one member paid.");
        }

        SharedAsset asset = new SharedAsset();
        asset.setGroupId(groupId);
        asset.setName(name.trim());
        asset.setPurchasePrice(price.setScale(2, RoundingMode.HALF_UP));
        asset.setPurchaseDate(date);
        asset.setUsefulLifeMonths(usefulLifeMonths);
        asset.setCreatedByUserId(createdByUserId);
        asset.setCreatedAt(LocalDateTime.now());
        SharedAsset saved = assetRepository.save(asset);

        Map<String, BigDecimal> shares = AssetCalculator.sharesFromContributions(paid);
        replaceShares(saved.getId(), shares);
        recordEvent(saved, "REGISTERED",
                "Registered, bought for ৳" + saved.getPurchasePrice() + " with a useful life of "
                        + usefulLifeMonths + " months. Paid: " + describeMoney(paid) + ".", shares);
        return saved;
    }

    @Transactional
    public void logUse(Long assetId, String userId) {
        requireAsset(assetId);
        AssetUsage usage = new AssetUsage();
        usage.setAssetId(assetId);
        usage.setUserId(userId);
        usage.setUsedAt(LocalDateTime.now());
        usageRepository.save(usage);
    }

    // Adds a repair or running cost. It is split by each member's share of the
    // uses logged since the last cost, or by ownership if nobody logged a use.
    @Transactional
    public AssetCost addCost(Long assetId, String description, BigDecimal amount, String paidByUserId) {
        SharedAsset asset = requireAsset(assetId);
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Describe the cost, e.g. Motor repair.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Cost amount must be greater than 0.");
        }
        BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> shares = loadShares(assetId);
        Map<String, BigDecimal> uses = usesSinceLastCost(assetId);
        boolean byUsage = !uses.isEmpty();
        Map<String, BigDecimal> split = AssetCalculator.splitByWeights(total, byUsage ? uses : shares);

        AssetCost cost = new AssetCost();
        cost.setAssetId(assetId);
        cost.setDescription(description.trim());
        cost.setAmount(total);
        cost.setPaidByUserId(paidByUserId);
        cost.setSplitBasis(byUsage ? "USAGE" : "OWNERSHIP");
        cost.setIncurredAt(LocalDateTime.now());
        AssetCost saved = costRepository.save(cost);

        postExpense(asset, paidByUserId, "[ASSET] " + asset.getName() + ": " + cost.getDescription(),
                "Asset Cost", split);
        recordEvent(asset, "COST",
                cost.getDescription() + " cost ৳" + total + ", paid by " + paidByUserId + ". Split by "
                        + (byUsage ? "use since the last cost (" + describeUses(uses) + ")" : "ownership share")
                        + ": " + describeMoney(split) + ".", shares);
        return saved;
    }

    // A member leaves. They are owed their share of today's value; the remaining
    // owners take over that share in proportion and each owes the leaver their part.
    @Transactional
    public BigDecimal buyOut(Long assetId, String leavingUserId) {
        SharedAsset asset = requireAsset(assetId);
        Map<String, BigDecimal> shares = loadShares(assetId);
        if (!shares.containsKey(leavingUserId)) {
            throw new IllegalArgumentException(leavingUserId + " does not own a share of this asset.");
        }
        if (shares.size() < 2) {
            throw new IllegalStateException("The last owner cannot be bought out.");
        }

        BigDecimal value = valueToday(asset);
        BigDecimal leavingShare = shares.get(leavingUserId);
        BigDecimal buyout = AssetCalculator.shareValue(leavingShare, value);

        Map<String, BigDecimal> remaining = new LinkedHashMap<>(shares);
        remaining.remove(leavingUserId);
        Map<String, BigDecimal> owed = AssetCalculator.splitByWeights(buyout, remaining);
        Map<String, BigDecimal> newShares = AssetCalculator.sharesAfterBuyout(shares, leavingUserId);

        replaceShares(assetId, newShares);
        // The leaver is the one being paid, so they are the "payer" of this ledger entry.
        postExpense(asset, leavingUserId, "[ASSET] " + asset.getName() + ": buyout of " + leavingUserId,
                "Asset Buyout", owed);
        recordEvent(asset, "BUYOUT",
                leavingUserId + " left with a " + percent(leavingShare) + "% share, bought out for ৳" + buyout
                        + ". Owed by: " + describeMoney(owed) + ".", newShares);
        return buyout;
    }

    // A member joins by buying a chosen share at today's value. Existing owners
    // give up that share in proportion and are paid in the same proportion.
    @Transactional
    public BigDecimal buyIn(Long assetId, String joiningUserId, BigDecimal sharePercent) {
        SharedAsset asset = requireAsset(assetId);
        Map<String, BigDecimal> shares = loadShares(assetId);
        Map<String, BigDecimal> newShares = AssetCalculator.sharesAfterBuyIn(shares, joiningUserId, sharePercent);

        BigDecimal value = valueToday(asset);
        BigDecimal price = AssetCalculator.shareValue(newShares.get(joiningUserId), value);
        Map<String, BigDecimal> received = AssetCalculator.splitByWeights(price, shares);

        replaceShares(assetId, newShares);
        // One ledger entry per existing owner: the joiner owes each of them their part.
        for (Map.Entry<String, BigDecimal> e : received.entrySet()) {
            Map<String, BigDecimal> owes = new LinkedHashMap<>();
            owes.put(joiningUserId, e.getValue());
            postExpense(asset, e.getKey(), "[ASSET] " + asset.getName() + ": buy-in by " + joiningUserId,
                    "Asset Buy-In", owes);
        }
        recordEvent(asset, "BUY_IN",
                joiningUserId + " joined with a " + percent(newShares.get(joiningUserId)) + "% share for ৳" + price
                        + ". Paid to: " + describeMoney(received) + ".", newShares);
        return price;
    }

    // ---------- Helpers ----------

    private SharedAsset requireAsset(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found."));
    }

    private BigDecimal valueToday(SharedAsset asset) {
        return AssetCalculator.currentValue(asset.getPurchasePrice(), asset.getUsefulLifeMonths(),
                asset.getPurchaseDate(), LocalDate.now());
    }

    // member -> ownership percentage, in name order
    private Map<String, BigDecimal> loadShares(Long assetId) {
        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        for (AssetShare share : shareRepository.findByAssetIdOrderByUserIdAsc(assetId)) {
            shares.put(share.getUserId(), share.getSharePercent());
        }
        return shares;
    }

    private void replaceShares(Long assetId, Map<String, BigDecimal> shares) {
        shareRepository.deleteAll(shareRepository.findByAssetIdOrderByUserIdAsc(assetId));
        for (Map.Entry<String, BigDecimal> e : shares.entrySet()) {
            AssetShare share = new AssetShare();
            share.setAssetId(assetId);
            share.setUserId(e.getKey());
            share.setSharePercent(e.getValue());
            shareRepository.save(share);
        }
    }

    // member -> number of uses logged after the most recent cost
    private Map<String, BigDecimal> usesSinceLastCost(Long assetId) {
        List<AssetCost> costs = costRepository.findByAssetIdOrderByIncurredAtDesc(assetId);
        LocalDateTime lastCostAt = costs.isEmpty() ? null : costs.get(0).getIncurredAt();

        Map<String, BigDecimal> uses = new TreeMap<>();
        for (AssetUsage usage : usageRepository.findByAssetId(assetId)) {
            if (lastCostAt == null || usage.getUsedAt().isAfter(lastCostAt)) {
                uses.merge(usage.getUserId(), BigDecimal.ONE, BigDecimal::add);
            }
        }
        return uses;
    }

    private int countOf(Map<String, BigDecimal> uses, String userId) {
        BigDecimal count = uses.get(userId);
        return count == null ? 0 : count.intValue();
    }

    // Posts "these members owe paidBy" to the group ledger. Zero amounts are left out.
    private void postExpense(SharedAsset asset, String paidByUserId, String title, String category,
                             Map<String, BigDecimal> owedByMember) {
        Map<String, Double> splits = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : owedByMember.entrySet()) {
            if (e.getValue().signum() > 0) {
                splits.put(e.getKey(), e.getValue().doubleValue());
                total = total.add(e.getValue());
            }
        }
        if (splits.isEmpty()) {
            return; // nothing is owed (for example the asset is worth 0 now)
        }

        Expense expense = new Expense();
        expense.setGroupId(asset.getGroupId());
        expense.setPaidByUserId(paidByUserId);
        expense.setTitle(title);
        expense.setCategory(category);
        expense.setTotalAmount(total.doubleValue());
        expense.setSplits(splits);
        expense.setStatus("APPROVED");
        expense.setCreatedAt(LocalDateTime.now());
        expenseRepository.save(expense);
    }

    private void recordEvent(SharedAsset asset, String type, String description, Map<String, BigDecimal> shares) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : shares.entrySet()) {
            parts.add(e.getKey() + " " + percent(e.getValue()) + "%");
        }

        AssetEvent event = new AssetEvent();
        event.setAssetId(asset.getId());
        event.setType(type);
        event.setDescription(description);
        event.setValueAtTime(valueToday(asset));
        event.setSharesSnapshot(String.join(", ", parts));
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    // Shares are stored with 4 decimals; show 2.
    private String percent(BigDecimal share) {
        return share.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String describeMoney(Map<String, BigDecimal> amounts) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : amounts.entrySet()) {
            parts.add(e.getKey() + " ৳" + e.getValue());
        }
        return String.join(", ", parts);
    }

    private String describeUses(Map<String, BigDecimal> uses) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : uses.entrySet()) {
            parts.add(e.getKey() + " " + e.getValue().intValue());
        }
        return String.join(", ", parts);
    }
}
