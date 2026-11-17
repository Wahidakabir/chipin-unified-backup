package com.chipin.chipin.exception;

/**
 * Exception thrown when attempting to link an external expense ID that is already linked.
 */
public class DuplicateExternalExpenseException extends RuntimeException {

    public DuplicateExternalExpenseException(Long externalExpenseId) {
        super("An expense transaction is already linked to external expense ID: " + externalExpenseId);
    }
}
