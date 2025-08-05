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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ToolInventoryResponseTest {

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
    void testValidToolInventoryResponse() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList("tool1", "tool2", "tool3");
        Instant discoveryTimestamp = Instant.now().minusSeconds(5);
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertTrue(violations.isEmpty());
        assertEquals(requestId, response.getRequestId());
        assertEquals(timestamp, response.getTimestamp());
        assertEquals(currentTools, response.getCurrentTools());
        assertEquals(discoveryTimestamp, response.getDiscoveryTimestamp());
    }

    @Test
    void testInvalidRequestId() {
        // Given
        String invalidRequestId = "invalid-uuid";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(invalidRequestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("valid UUID")));
    }

    @Test
    void testBlankRequestId() {
        // Given
        String blankRequestId = "";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(blankRequestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be blank")));
    }

    @Test
    void testNullTimestamp() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = null;
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void testNullCurrentTools() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<String> currentTools = null;
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void testNullDiscoveryTimestamp() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        Instant discoveryTimestamp = null;
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("cannot be null")));
    }

    @Test
    void testEmptyCurrentToolsList() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList(); // Empty list should be valid
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        Set<ConstraintViolation<ToolInventoryResponse>> violations = validator.validate(response);
        
        // Then
        assertTrue(violations.isEmpty()); // Empty list should be valid
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.parse("2025-01-28T10:30:00Z");
        List<String> currentTools = Arrays.asList("tool1", "tool2", "tool3");
        Instant discoveryTimestamp = Instant.parse("2025-01-28T10:29:55Z");
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        String json = objectMapper.writeValueAsString(response);
        ToolInventoryResponse deserializedResponse = objectMapper.readValue(json, ToolInventoryResponse.class);
        
        // Then
        assertEquals(response.getRequestId(), deserializedResponse.getRequestId());
        assertEquals(response.getTimestamp(), deserializedResponse.getTimestamp());
        assertEquals(response.getCurrentTools(), deserializedResponse.getCurrentTools());
        assertEquals(response.getDiscoveryTimestamp(), deserializedResponse.getDiscoveryTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response1 = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        ToolInventoryResponse response2 = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        ToolInventoryResponse response3 = new ToolInventoryResponse("different-id", timestamp, currentTools, discoveryTimestamp);
        
        // Then
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1, response3);
        assertNotEquals(response1.hashCode(), response3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        String toString = response.toString();
        
        // Then
        assertTrue(toString.contains("ToolInventoryResponse"));
        assertTrue(toString.contains(requestId));
        assertTrue(toString.contains("tool1"));
        assertTrue(toString.contains("tool2"));
    }
}