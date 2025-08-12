package com.quip.backend.common.exception;

/**
 * Custom exception thrown when an operation cannot be completed because
 * it would create a conflict with a tool that is currently interrupting a conversation.
 * <p>
 * This exception indicates that the request is valid but cannot be fulfilled
 * because it would create an inconsistent state where a tool that is currently
 * causing an interruption would be approved for use. The client should resolve
 * the interruption first before attempting to approve the tool.
 * </p>
 */
public class InterruptedToolConflictException extends RuntimeException {
    
    /**
     * Creates an interrupted tool conflict exception with a custom message.
     *
     * @param message The error message
     */
    public InterruptedToolConflictException(String message) {
        super(message);
    }
    
    /**
     * Creates an interrupted tool conflict exception with a custom message and cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public InterruptedToolConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}