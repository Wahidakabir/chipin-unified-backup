package com.chipin.chipin.model;

/**
 * Lifecycle status of a group budget in Feature 11: Group Budget Management System.
 */
public enum BudgetStatus {

    /**
     * The budget is active and currently tracking spending against allocated category limits.
     * Expenses can be recorded and allocated against its categories.
     */
    ACTIVE,

    /**
     * One or more categories (or the total budget limit) have exceeded allocated spending limits.
     * The budget remains open for recording, but visual warnings and overrun alerts are active.
     */
    EXCEEDED,

    /**
     * The budget period has concluded or the budget has been closed/archived by group members.
     * Archived budgets are preserved for historical reporting and audit trails;
     * no new expenses can be recorded against an archived budget.
     */
    ARCHIVED
}
