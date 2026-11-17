package com.chipin.chipin.dto;

import com.chipin.chipin.model.RoundUpSaving;
import com.chipin.chipin.model.RoundUpSettings;
import java.math.BigDecimal;
import java.util.List;

// Everything the round-up page shows for one member.
public class RoundUpOverview {

    private final RoundUpSettings settings;
    private final BigDecimal mySavedTotal;       // everything this member has saved
    private final BigDecimal myPersonalTotal;    // the part that went to personal goals
    private final BigDecimal groupGoalTotal;     // what all members sent to group goals
    private final List<RoundUpSaving> myHistory;
    private final List<RoundUpResult> examples;  // what the current rules do to sample payments
    private final boolean weekendToday;

    public RoundUpOverview(RoundUpSettings settings, BigDecimal mySavedTotal, BigDecimal myPersonalTotal,
                           BigDecimal groupGoalTotal, List<RoundUpSaving> myHistory,
                           List<RoundUpResult> examples, boolean weekendToday) {
        this.settings = settings;
        this.mySavedTotal = mySavedTotal;
        this.myPersonalTotal = myPersonalTotal;
        this.groupGoalTotal = groupGoalTotal;
        this.myHistory = myHistory;
        this.examples = examples;
        this.weekendToday = weekendToday;
    }

    public RoundUpSettings getSettings() { return settings; }
    public BigDecimal getMySavedTotal() { return mySavedTotal; }
    public BigDecimal getMyPersonalTotal() { return myPersonalTotal; }
    public BigDecimal getGroupGoalTotal() { return groupGoalTotal; }
    public List<RoundUpSaving> getMyHistory() { return myHistory; }
    public List<RoundUpResult> getExamples() { return examples; }
    public boolean isWeekendToday() { return weekendToday; }
    public int getPaymentCount() { return myHistory.size(); }
}
