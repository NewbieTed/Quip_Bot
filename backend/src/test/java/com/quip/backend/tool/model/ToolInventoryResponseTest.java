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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("tool3").mcpServerName("built-in").build()
        );
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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
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
        List<ToolInfo> currentTools = null;
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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
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
        List<ToolInfo> currentTools = Arrays.asList(); // Empty list should be valid
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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("tool3").mcpServerName("built-in").build()
        );
        Instant discoveryTimestamp = Instant.parse("2025-01-28T10:29:55Z");
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When
        String json = objectMapper.writeValueAsString(response);
        ToolInventoryResponse deserializedResponse = objectMapper.readValue(json, ToolInventoryResponse.class);
        
        // Then
        assertEquals(response.getRequestId(), deserializedResponse.getRequestId());
        assertEquals(response.getTimestamp(), deserializedResponse.getTimestamp());
        assertEquals(response.getCurrentTools().size(), deserializedResponse.getCurrentTools().size());
        for (int i = 0; i < response.getCurrentTools().size(); i++) {
            assertEquals(response.getCurrentTools().get(i).getName(), 
                        deserializedResponse.getCurrentTools().get(i).getName());
            assertEquals(response.getCurrentTools().get(i).getMcpServerName(), 
                        deserializedResponse.getCurrentTools().get(i).getMcpServerName());
        }
        assertEquals(response.getDiscoveryTimestamp(), deserializedResponse.getDiscoveryTimestamp());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
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
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
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

    @Test
    void testMcpServerNameHandling_BuiltInTools() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("builtin-tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("builtin-tool2").mcpServerName("built-in").build(),
            ToolInfo.builder().name("builtin-tool3").mcpServerName("built-in").build()
        );
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When/Then
        assertEquals(3, response.getCurrentTools().size());
        response.getCurrentTools().forEach(tool -> {
            assertEquals("built-in", tool.getMcpServerName());
            assertTrue(tool.getName().startsWith("builtin-tool"));
        });
    }

    @Test
    void testMcpServerNameHandling_McpTools() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("geo-data-location").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("aws-docs-search").mcpServerName("aws-docs").build()
        );
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When/Then
        assertEquals(3, response.getCurrentTools().size());
        
        // Verify geo-data tools
        List<ToolInfo> geoDataTools = response.getCurrentTools().stream()
                .filter(tool -> "geo-data".equals(tool.getMcpServerName()))
                .toList();
        assertEquals(2, geoDataTools.size());
        assertTrue(geoDataTools.stream().allMatch(tool -> tool.getName().startsWith("geo-data-")));
        
        // Verify aws-docs tools
        List<ToolInfo> awsDocsTools = response.getCurrentTools().stream()
                .filter(tool -> "aws-docs".equals(tool.getMcpServerName()))
                .toList();
        assertEquals(1, awsDocsTools.size());
        assertTrue(awsDocsTools.get(0).getName().startsWith("aws-docs-"));
    }

    @Test
    void testMcpServerNameHandling_MixedTools() {
        // Given
        String requestId = "550e8400-e29b-41d4-a716-446655440000";
        Instant timestamp = Instant.now();
        List<ToolInfo> currentTools = Arrays.asList(
            ToolInfo.builder().name("builtin-tool1").mcpServerName("built-in").build(),
            ToolInfo.builder().name("builtin-tool2").mcpServerName("built-in").build(),
            ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("geo-data-location").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("aws-docs-search").mcpServerName("aws-docs").build()
        );
        Instant discoveryTimestamp = Instant.now();
        
        ToolInventoryResponse response = new ToolInventoryResponse(requestId, timestamp, currentTools, discoveryTimestamp);
        
        // When/Then
        assertEquals(5, response.getCurrentTools().size());
        
        // Count tools by server type
        long builtInCount = response.getCurrentTools().stream()
                .filter(tool -> "built-in".equals(tool.getMcpServerName()))
                .count();
        long geoDataCount = response.getCurrentTools().stream()
                .filter(tool -> "geo-data".equals(tool.getMcpServerName()))
                .count();
        long awsDocsCount = response.getCurrentTools().stream()
                .filter(tool -> "aws-docs".equals(tool.getMcpServerName()))
                .count();
        
        assertEquals(2, builtInCount);
        assertEquals(2, geoDataCount);
        assertEquals(1, awsDocsCount);
    }
}