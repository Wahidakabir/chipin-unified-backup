package com.chipin.chipin.exception;

/**
 * Thrown when a specific member's planned contribution pledge cannot be found for a goal.
 */
public class PlanNotFoundException extends RuntimeException {

    public PlanNotFoundException(String message) {
        super(message);
    }
}
