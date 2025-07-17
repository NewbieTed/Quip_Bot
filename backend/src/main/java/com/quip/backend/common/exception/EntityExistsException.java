package com.quip.backend.common.exception;

/**
 * Custom exception to replace jakarta.persistence.EntityExistsException
 * Used when trying to create an entity that already exists
 */
public class EntityExistsException extends RuntimeException {
    
    public EntityExistsException(String message) {
        super(message);
    }
    
    public EntityExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}