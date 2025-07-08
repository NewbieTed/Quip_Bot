package com.quip.backend.common.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String category, String field, String requirement) {
        super("Validation failed in [" + category + "]: Field '" + field + "' must satisfy '" + requirement + "'.");
    }
}
