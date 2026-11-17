package com.chipin.chipin.exception;

/**
 * Thrown when a requested savings goal cannot be found in the database.
 * Extends IllegalArgumentException to preserve backwards compatibility with Stage 3 service tests.
 */
public class GoalNotFoundException extends IllegalArgumentException {

    public GoalNotFoundException(String message) {
        super(message);
    }
}
