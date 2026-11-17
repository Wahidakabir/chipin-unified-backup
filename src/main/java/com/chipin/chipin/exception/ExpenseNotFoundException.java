package com.chipin.chipin.exception;

/**
 * Exception thrown when a requested BudgetExpense cannot be found.
 */
public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(String message) {
        super(message);
    }

    public ExpenseNotFoundException(Long id) {
        super("Expense not found with ID: " + id);
    }
}
