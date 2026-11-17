package com.chipin.chipin.model;

/**
 * Status of an expense recorded under a budget category.
 * Supports financial audit preservation by distinguishing active expenses from voided transactions.
 */
public enum ExpenseStatus {

    /**
     * Active expense transaction that contributes directly to category and overall spending.
     */
    RECORDED,

    /**
     * Voided or cancelled expense transaction.
     * Preserved in the database for financial audit history, but excluded from active spending totals.
     */
    VOIDED
}
