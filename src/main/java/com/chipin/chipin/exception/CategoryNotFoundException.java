package com.chipin.chipin.exception;

/**
 * Exception thrown when a requested BudgetCategory cannot be found.
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String message) {
        super(message);
    }

    public CategoryNotFoundException(Long id) {
        super("Category not found with ID: " + id);
    }
}
