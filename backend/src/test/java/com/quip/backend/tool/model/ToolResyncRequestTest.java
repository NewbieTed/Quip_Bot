package com.quip.backend.tool.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ToolResyncRequestTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testValidToolResyncRequest() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        String reason = "message_processing_failure";
        
        ToolResyncRequest request = new ToolResyncRequest(requestId, timestamp, reason);
        
        // When
        Set<ConstraintViolation<ToolResyncRequest>> violations = validator.validate(request);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(requestId, request.getRequestId());
        assertEquals(timestamp, request.getTimestamp());
        assertEquals(reason, request.getReason());
    }

    @Test
    void testInvalidRequestId() {
        // Given
        String invalidRequestId = "invalid-uuid";
        Instant timestamp = Instant.now();
        String reason = "test_reason";
        
        ToolResyncRequest request = new ToolResyncRequest(invalidRequestId, timestamp, reason);
        
        // When
        Set<ConstraintViolation<ToolResyncRequest>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("valid UUID")));
    }

    @Test
    void testBlankRequestId() {
        // Given
        String blankRequestId = "";
        Instant timestamp = Instant.now();
        String reason = "test_reason";
        
        ToolResyncRequest request = new ToolResyncRequest(blankRequestId, timestamp, reason);
        
        // When
        Set<ConstraintViolation<ToolResyncRequest>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be blank")));
    }

    @Test
    void testNullTimestamp() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = null;
        String reason = "test_reason";
        
        ToolResyncRequest request = new ToolResyncRequest(requestId, timestamp, reason);
        
        // When
        Set<ConstraintViolation<ToolResyncRequest>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void testBlankReason() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        String reason = "";
        
        ToolResyncRequest request = new ToolResyncRequest(requestId, timestamp, reason);
        
        // When
        Set<ConstraintViolation<ToolResyncRequest>> violations = validator.validate(request);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be blank")));
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.parse("2025-01-28T10:30:00Z");
        String reason = "message_processing_failure";
        
        ToolResyncRequest request = new ToolResyncRequest(requestId, timestamp, reason);
        
        // When
        String json = objectMapper.writeValueAsString(request);
        ToolResyncRequest deserializedRequest = objectMapper.readValue(json, ToolResyncRequest.class);
        
        // Then
        assertEquals(request.getRequestId(), deserializedRequest.getRequestId());
        assertEquals(request.getTimestamp(), deserializedRequest.getTimestamp());
        assertEquals(request.getReason(), deserializedRequest.getReason());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        String reason = "test_reason";
        
        ToolResyncRequest request1 = new ToolResyncRequest(requestId, timestamp, reason);
        ToolResyncRequest request2 = new ToolResyncRequest(requestId, timestamp, reason);
        ToolResyncRequest request3 = new ToolResyncRequest("different-id", timestamp, reason);
        
        // Then
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1, request3);
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        String reason = "test_reason";
        
        ToolResyncRequest request = new ToolResyncRequest(requestId, timestamp, reason);
        
        // When
        String toString = request.toString();
        
        // Then
        assertTrue(toString.contains("ToolResyncRequest"));
        assertTrue(toString.contains(requestId));
        assertTrue(toString.contains(reason));
    }
}