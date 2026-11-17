package com.chipin.chipin.exception;

/**
 * Thrown when a contribution would cause total saved funds to exceed the goal target amount.
 * Extends IllegalArgumentException to preserve backwards compatibility with Stage 3 service tests.
 */
public class OverfundingException extends IllegalArgumentException {

    public OverfundingException(String message) {
        super(message);
    }
}
