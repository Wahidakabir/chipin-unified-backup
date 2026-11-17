package com.chipin.chipin.exception;

/**
 * Exception thrown when a requested GroupBudget cannot be found.
 */
public class BudgetNotFoundException extends RuntimeException {

    public BudgetNotFoundException(String message) {
        super(message);
    }

    public BudgetNotFoundException(Long id) {
        super("Budget not found with ID: " + id);
    }
}
