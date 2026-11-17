package com.chipin.chipin.dto;

import com.chipin.chipin.model.BillItem;
import java.util.List;

// One receipt line plus the members who shared it (for the bills page).
public class BillItemView {

    private final BillItem item;
    private final List<String> assignees;

    public BillItemView(BillItem item, List<String> assignees) {
        this.item = item;
        this.assignees = assignees;
    }

    public BillItem getItem() { return item; }
    public List<String> getAssignees() { return assignees; }
}
