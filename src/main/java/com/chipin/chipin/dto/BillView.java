package com.chipin.chipin.dto;

import com.chipin.chipin.model.BillShare;
import com.chipin.chipin.model.ItemizedBill;
import java.math.BigDecimal;
import java.util.List;

// Everything the bills page needs to show one itemized bill.
public class BillView {

    private final ItemizedBill bill;
    private final List<BillItemView> items;
    private final List<BillShare> shares;
    private final BigDecimal itemsSubtotal;
    private final BigDecimal extras;
    private final BigDecimal grandTotal;

    public BillView(ItemizedBill bill, List<BillItemView> items, List<BillShare> shares,
                    BigDecimal itemsSubtotal, BigDecimal extras, BigDecimal grandTotal) {
        this.bill = bill;
        this.items = items;
        this.shares = shares;
        this.itemsSubtotal = itemsSubtotal;
        this.extras = extras;
        this.grandTotal = grandTotal;
    }

    public ItemizedBill getBill() { return bill; }
    public List<BillItemView> getItems() { return items; }
    public List<BillShare> getShares() { return shares; }
    public BigDecimal getItemsSubtotal() { return itemsSubtotal; }
    public BigDecimal getExtras() { return extras; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public boolean isDraft() { return "DRAFT".equals(bill.getStatus()); }
}
