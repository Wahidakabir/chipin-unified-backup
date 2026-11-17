package com.chipin.chipin.model;

/**
 * Status lifecycle of a Savings Goal in Chipin.
 */
public enum GoalStatus {
    /** Goal is currently open for savings and deadline has not passed. */
    ACTIVE,

    /** Goal has successfully accumulated contributions equal to or exceeding its target amount. */
    COMPLETED,

    /** Goal deadline has passed before reaching the target amount. Contributions may still be accepted. */
    OVERDUE
}
