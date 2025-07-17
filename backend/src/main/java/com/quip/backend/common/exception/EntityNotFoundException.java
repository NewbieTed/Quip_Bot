package com.quip.backend.common.exception;

/**
 * Custom exception to replace jakarta.persistence.EntityNotFoundException
 * Used when an entity is not found in the database
 */
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}