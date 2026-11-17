package com.chipin.chipin.service;

import com.chipin.chipin.dto.BillItemView;
import com.chipin.chipin.dto.BillView;
import com.chipin.chipin.model.BillItem;
import com.chipin.chipin.model.BillShare;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.ItemAssignment;
import com.chipin.chipin.model.ItemizedBill;
import com.chipin.chipin.repository.BillItemRepository;
import com.chipin.chipin.repository.BillShareRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import com.chipin.chipin.repository.ItemAssignmentRepository;
import com.chipin.chipin.repository.ItemizedBillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Feature 2: Itemized Receipt Splitting with Proportional Tax and Tip Allocation.
 *
 * Flow: create a DRAFT bill -> add items and choose who shared each one ->
 * finalize. Finalizing saves each member's share and posts one Expense to the
 * group ledger so other features (e.g. debt simplification) can use it.
 */
@Service
public class ItemizedBillService {

    private static final String DRAFT = "DRAFT";
    private static final String FINALIZED = "FINALIZED";

    private final ItemizedBillRepository billRepository;
    private final BillItemRepository itemRepository;
    private final ItemAssignmentRepository assignmentRepository;
    private final BillShareRepository shareRepository;
    private final ExpenseRepository expenseRepository;

    public ItemizedBillService(ItemizedBillRepository billRepository,
                               BillItemRepository itemRepository,
                               ItemAssignmentRepository assignmentRepository,
                               BillShareRepository shareRepository,
                               ExpenseRepository expenseRepository) {
        this.billRepository = billRepository;
        this.itemRepository = itemRepository;
        this.assignmentRepository = assignmentRepository;
        this.shareRepository = shareRepository;
        this.expenseRepository = expenseRepository;
    }

    // ---------- Reading ----------

    public List<BillView> getGroupBillViews(String groupId) {
        List<BillView> views = new ArrayList<>();
        for (ItemizedBill bill : billRepository.findByGroupIdOrderByIdDesc(groupId)) {
            views.add(buildView(bill));
        }
        return views;
    }

    private BillView buildView(ItemizedBill bill) {
        List<BillItem> items = itemRepository.findByBillId(bill.getId());
        Map<Long, List<String>> assignees = loadAssignees(items);

        List<BillItemView> itemViews = new ArrayList<>();
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        for (BillItem item : items) {
            itemViews.add(new BillItemView(item, assignees.getOrDefault(item.getId(), List.of())));
            itemsSubtotal = itemsSubtotal.add(item.getAmount());
        }

        BigDecimal extras = extrasOf(bill);

        // A finalized bill shows its saved shares; a draft shows a live preview.
        List<BillShare> shares;
        if (FINALIZED.equals(bill.getStatus())) {
            shares = shareRepository.findByBillId(bill.getId());
        } else {
            try {
                shares = computeShares(bill, items, assignees);
            } catch (IllegalArgumentException | IllegalStateException e) {
                shares = new ArrayList<>(); // not splittable yet (e.g. discount too big)
            }
        }

        return new BillView(bill, itemViews, shares, itemsSubtotal, extras, itemsSubtotal.add(extras));
    }

    // ---------- Writing ----------

    @Transactional
    public ItemizedBill createBill(String groupId, String paidByUserId, String title, LocalDate billDate,
                                   BigDecimal tax, BigDecimal serviceCharge, BigDecimal tip, BigDecimal discount) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Please give the bill a title.");
        }
        if (paidByUserId == null || paidByUserId.isBlank()) {
            throw new IllegalArgumentException("Please choose who paid the bill.");
        }

        ItemizedBill bill = new ItemizedBill();
        bill.setGroupId(groupId);
        bill.setPaidByUserId(paidByUserId);
        bill.setTitle(title.trim());
        bill.setBillDate(billDate != null ? billDate : LocalDate.now());
        bill.setTax(money(tax, "Tax"));
        bill.setServiceCharge(money(serviceCharge, "Service charge"));
        bill.setTip(money(tip, "Tip"));
        bill.setDiscount(money(discount, "Discount"));
        bill.setStatus(DRAFT);
        return billRepository.save(bill);
    }

    @Transactional
    public void updateExtras(Long billId, BigDecimal tax, BigDecimal serviceCharge,
                             BigDecimal tip, BigDecimal discount) {
        ItemizedBill bill = requireDraft(billId);
        bill.setTax(money(tax, "Tax"));
        bill.setServiceCharge(money(serviceCharge, "Service charge"));
        bill.setTip(money(tip, "Tip"));
        bill.setDiscount(money(discount, "Discount"));
        billRepository.save(bill);
    }

    @Transactional
    public BillItem addItem(Long billId, String name, BigDecimal amount, List<String> userIds) {
        requireDraft(billId);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Please enter the item name.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Item amount must be greater than 0.");
        }
        // LinkedHashSet removes duplicates but keeps the order the members were picked in.
        List<String> uniqueUsers = userIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(userIds));
        if (uniqueUsers.isEmpty()) {
            throw new IllegalArgumentException("Select at least one member who shared this item.");
        }

        BillItem item = new BillItem();
        item.setBillId(billId);
        item.setName(name.trim());
        item.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        BillItem saved = itemRepository.save(item);

        for (String userId : uniqueUsers) {
            ItemAssignment assignment = new ItemAssignment();
            assignment.setItemId(saved.getId());
            assignment.setUserId(userId);
            assignmentRepository.save(assignment);
        }
        return saved;
    }

    @Transactional
    public void removeItem(Long itemId) {
        BillItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found."));
        requireDraft(item.getBillId());

        assignmentRepository.deleteAll(assignmentRepository.findByItemId(itemId));
        itemRepository.delete(item);
    }

    @Transactional
    public void deleteDraft(Long billId) {
        ItemizedBill bill = requireDraft(billId);
        List<BillItem> items = itemRepository.findByBillId(billId);
        for (BillItem item : items) {
            assignmentRepository.deleteAll(assignmentRepository.findByItemId(item.getId()));
        }
        itemRepository.deleteAll(items);
        billRepository.delete(bill);
    }

    // Locks the bill, saves every member's share and posts the expense to the ledger.
    @Transactional
    public ItemizedBill finalizeBill(Long billId) {
        ItemizedBill bill = requireDraft(billId);
        List<BillItem> items = itemRepository.findByBillId(billId);
        if (items.isEmpty()) {
            throw new IllegalStateException("Add at least one item before finalizing the bill.");
        }

        List<BillShare> shares = computeShares(bill, items, loadAssignees(items));
        shareRepository.saveAll(shares);

        // One ledger entry: who paid, the full amount, and what each member owes.
        Map<String, Double> splits = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (BillShare share : shares) {
            splits.put(share.getUserId(), share.getTotal().doubleValue());
            grandTotal = grandTotal.add(share.getTotal());
        }

        Expense expense = new Expense();
        expense.setGroupId(bill.getGroupId());
        expense.setPaidByUserId(bill.getPaidByUserId());
        expense.setTitle("[ITEMIZED] " + bill.getTitle());
        expense.setCategory("Itemized Bill");
        expense.setTotalAmount(grandTotal.doubleValue());
        expense.setSplits(splits);
        expense.setStatus("APPROVED");
        expense.setCreatedAt(LocalDateTime.now());
        expenseRepository.save(expense);

        bill.setStatus(FINALIZED);
        return billRepository.save(bill);
    }

    // ---------- Helpers ----------

    // Builds each member's subtotal, share of the extras and final total.
    private List<BillShare> computeShares(ItemizedBill bill, List<BillItem> items,
                                          Map<Long, List<String>> assignees) {
        List<BillShare> shares = new ArrayList<>();
        if (items.isEmpty()) {
            return shares;
        }

        Map<Long, BigDecimal> itemAmounts = new LinkedHashMap<>();
        BigDecimal itemsSubtotal = BigDecimal.ZERO;
        for (BillItem item : items) {
            itemAmounts.put(item.getId(), item.getAmount());
            itemsSubtotal = itemsSubtotal.add(item.getAmount());
        }

        BigDecimal extras = extrasOf(bill);
        if (itemsSubtotal.add(extras).signum() < 0) {
            throw new IllegalStateException("The discount is larger than the whole bill.");
        }

        Map<String, BigDecimal> subtotals = BillSplitCalculator.subtotals(itemAmounts, assignees);
        Map<String, BigDecimal> totals = BillSplitCalculator.totals(subtotals, extras);

        for (Map.Entry<String, BigDecimal> entry : subtotals.entrySet()) {
            BigDecimal total = totals.get(entry.getKey());
            BillShare share = new BillShare();
            share.setBillId(bill.getId());
            share.setUserId(entry.getKey());
            share.setSubtotal(entry.getValue());
            share.setExtrasShare(total.subtract(entry.getValue()));
            share.setTotal(total);
            shares.add(share);
        }
        return shares;
    }

    // item id -> the members who shared that item
    private Map<Long, List<String>> loadAssignees(List<BillItem> items) {
        Map<Long, List<String>> assignees = new LinkedHashMap<>();
        if (items.isEmpty()) {
            return assignees;
        }
        List<Long> itemIds = new ArrayList<>();
        for (BillItem item : items) {
            itemIds.add(item.getId());
        }
        for (ItemAssignment a : assignmentRepository.findByItemIdIn(itemIds)) {
            assignees.computeIfAbsent(a.getItemId(), k -> new ArrayList<>()).add(a.getUserId());
        }
        return assignees;
    }

    // tax + service charge + tip - discount
    private BigDecimal extrasOf(ItemizedBill bill) {
        return bill.getTax().add(bill.getServiceCharge()).add(bill.getTip()).subtract(bill.getDiscount());
    }

    private ItemizedBill requireDraft(Long billId) {
        ItemizedBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found."));
        if (!DRAFT.equals(bill.getStatus())) {
            throw new IllegalStateException("This bill is already finalized and cannot be changed.");
        }
        return bill;
    }

    // Empty input counts as 0; negative amounts are rejected.
    private BigDecimal money(BigDecimal value, String label) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(label + " cannot be negative.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
