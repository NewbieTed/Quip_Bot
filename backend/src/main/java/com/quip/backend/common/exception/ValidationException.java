package com.quip.backend.common.exception;

/**
 * Custom exception for validation errors.
 * <p>
 * This exception is thrown when business rule validations fail,
 * such as when required fields are missing or have invalid values.
 * It provides structured error messages that can be returned to clients.
 * </p>
 */
public class ValidationException extends RuntimeException {
    /**
     * Creates a validation exception with a custom message.
     *
     * @param message The error message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Creates a validation exception with a structured error message.
     * <p>
     * This constructor formats the error message to include the validation category,
     * the field that failed validation, and the specific requirement that was not met.
     * </p>
     *
     * @param category The validation category or operation (e.g., "User Creation")
     * @param field The field that failed validation (e.g., "email")
     * @param requirement The requirement that was not met (e.g., "must be a valid email address")
     */
    public ValidationException(String category, String field, String requirement) {
        super("Validation failed in [" + category + "]: Field '" + field + "' " + requirement + ".");
    }
}
