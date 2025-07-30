package com.quip.backend.common.exception;

/**
 * Custom exception thrown when an operation cannot be completed because
 * a conversation is currently being processed.
 * <p>
 * This exception indicates that the request is valid but cannot be fulfilled
 * at this time due to the current state of the conversation resource.
 * The client should wait for the conversation to finish processing before
 * attempting the operation again.
 * </p>
 */
public class ConversationInProgressException extends RuntimeException {
    
    /**
     * Creates a conversation in progress exception with a custom message.
     *
     * @param message The error message
     */
    public ConversationInProgressException(String message) {
        super(message);
    }
    
    /**
     * Creates a conversation in progress exception with a custom message and cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public ConversationInProgressException(String message, Throwable cause) {
        super(message, cause);
    }
}