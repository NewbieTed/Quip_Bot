package com.quip.backend.tool.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolUpdateMessage model.
 * Tests JSON deserialization, validation, and business logic methods.
 */
class ToolUpdateMessageTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testJsonDeserialization_ValidMessage() throws Exception {
        String json = """
            {
                "messageId": "test-uuid-123",
                "timestamp": "2025-01-28T10:30:00Z",
                "addedTools": [
                    {"name": "tool1", "mcpServerName": "built-in"},
                    {"name": "geo-data-weather", "mcpServerName": "geo-data"}
                ],
                "removedTools": [
                    {"name": "tool3", "mcpServerName": "built-in"}
                ],
                "source": "agent"
            }
            """;

        ToolUpdateMessage message = objectMapper.readValue(json, ToolUpdateMessage.class);

        assertNotNull(message);
        assertEquals("test-uuid-123", message.getMessageId());
        assertNotNull(message.getTimestamp());
        assertEquals(2, message.getAddedTools().size());
        assertEquals("tool1", message.getAddedTools().get(0).getName());
        assertEquals("built-in", message.getAddedTools().get(0).getMcpServerName());
        assertEquals("geo-data-weather", message.getAddedTools().get(1).getName());
        assertEquals("geo-data", message.getAddedTools().get(1).getMcpServerName());
        assertEquals(1, message.getRemovedTools().size());
        assertEquals("tool3", message.getRemovedTools().get(0).getName());
        assertEquals("built-in", message.getRemovedTools().get(0).getMcpServerName());
        assertEquals("agent", message.getSource());
    }

    @Test
    void testJsonDeserialization_EmptyLists() throws Exception {
        String json = """
            {
                "messageId": "test-uuid-123",
                "timestamp": "2025-01-28T10:30:00Z",
                "addedTools": [],
                "removedTools": [],
                "source": "agent"
            }
            """;

        ToolUpdateMessage message = objectMapper.readValue(json, ToolUpdateMessage.class);

        assertNotNull(message);
        assertTrue(message.getAddedTools().isEmpty());
        assertTrue(message.getRemovedTools().isEmpty());
    }

    @Test
    void testValidation_ValidMessage() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build()
                ))
                .removedTools(Arrays.asList(
                    ToolInfo.builder().name("tool3").mcpServerName("built-in").build()
                ))
                .source("agent")
                .build();

        Set<ConstraintViolation<ToolUpdateMessage>> violations = validator.validate(message);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidation_NullMessageId() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId(null)
                .timestamp(OffsetDateTime.now())
                .addedTools(Collections.emptyList())
                .removedTools(Collections.emptyList())
                .source("agent")
                .build();

        Set<ConstraintViolation<ToolUpdateMessage>> violations = validator.validate(message);
        assertEquals(2, violations.size()); // Both @NotNull and @NotBlank are triggered
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Message ID cannot be null")));
    }

    @Test
    void testValidation_BlankMessageId() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("   ")
                .timestamp(OffsetDateTime.now())
                .addedTools(Collections.emptyList())
                .removedTools(Collections.emptyList())
                .source("agent")
                .build();

        Set<ConstraintViolation<ToolUpdateMessage>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Message ID cannot be blank"));
    }

    @Test
    void testValidation_NullTimestamp() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(null)
                .addedTools(Collections.emptyList())
                .removedTools(Collections.emptyList())
                .source("agent")
                .build();

        Set<ConstraintViolation<ToolUpdateMessage>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("Timestamp cannot be null"));
    }

    @Test
    void testValidation_NullSource() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(Collections.emptyList())
                .removedTools(Collections.emptyList())
                .source(null)
                .build();

        Set<ConstraintViolation<ToolUpdateMessage>> violations = validator.validate(message);
        assertEquals(2, violations.size()); // Both @NotNull and @NotBlank are triggered
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Source cannot be null")));
    }

    @Test
    void testValidation_NullLists() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(null)
                .removedTools(null)
                .source("agent")
                .build();

        Set<ConstraintViolation<ToolUpdateMessage>> violations = validator.validate(message);
        assertEquals(2, violations.size());
    }

    @Test
    void testHasChanges_WithAddedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("tool1").mcpServerName("built-in").build()
                ))
                .removedTools(Collections.emptyList())
                .build();

        assertTrue(message.hasChanges());
    }

    @Test
    void testHasChanges_WithRemovedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Collections.emptyList())
                .removedTools(Arrays.asList(
                    ToolInfo.builder().name("tool1").mcpServerName("built-in").build()
                ))
                .build();

        assertTrue(message.hasChanges());
    }

    @Test
    void testHasChanges_WithBothAddedAndRemovedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("tool1").mcpServerName("built-in").build()
                ))
                .removedTools(Arrays.asList(
                    ToolInfo.builder().name("tool2").mcpServerName("geo-data").build()
                ))
                .build();

        assertTrue(message.hasChanges());
    }

    @Test
    void testHasChanges_WithEmptyLists() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Collections.emptyList())
                .removedTools(Collections.emptyList())
                .build();

        assertFalse(message.hasChanges());
    }

    @Test
    void testHasChanges_WithNullLists() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(null)
                .removedTools(null)
                .build();

        assertFalse(message.hasChanges());
    }

    @Test
    void testHasValidToolNames_ValidNames() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("tool1").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("tool-2").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("tool_3").mcpServerName("geo-data").build(),
                    ToolInfo.builder().name("Tool123").mcpServerName("aws-docs").build()
                ))
                .removedTools(Arrays.asList(
                    ToolInfo.builder().name("another-tool").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("another_tool").mcpServerName("geo-data").build()
                ))
                .build();

        assertTrue(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_InvalidNames() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("tool with spaces").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("tool@invalid").mcpServerName("built-in").build()
                ))
                .removedTools(Arrays.asList(
                    ToolInfo.builder().name("valid-tool").mcpServerName("built-in").build()
                ))
                .build();

        assertFalse(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_EmptyToolName() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("valid-tool").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("").mcpServerName("built-in").build()
                ))
                .removedTools(Collections.emptyList())
                .build();

        assertFalse(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_NullToolInfo() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("valid-tool").mcpServerName("built-in").build(),
                    null
                ))
                .removedTools(Collections.emptyList())
                .build();

        assertFalse(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_EmptyLists() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Collections.emptyList())
                .removedTools(Collections.emptyList())
                .build();

        assertTrue(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_NullLists() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(null)
                .removedTools(null)
                .build();

        assertTrue(message.hasValidToolNames());
    }

    @Test
    void testMcpServerNameHandling_BuiltInTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("builtin-tool1").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("builtin-tool2").mcpServerName("built-in").build()
                ))
                .removedTools(Collections.emptyList())
                .source("agent")
                .build();

        assertEquals(2, message.getAddedTools().size());
        message.getAddedTools().forEach(tool -> {
            assertEquals("built-in", tool.getMcpServerName());
            assertTrue(tool.getName().startsWith("builtin-tool"));
        });
    }

    @Test
    void testMcpServerNameHandling_McpTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build(),
                    ToolInfo.builder().name("geo-data-location").mcpServerName("geo-data").build(),
                    ToolInfo.builder().name("aws-docs-search").mcpServerName("aws-docs").build()
                ))
                .removedTools(Collections.emptyList())
                .source("agent")
                .build();

        assertEquals(3, message.getAddedTools().size());
        
        // Verify geo-data tools
        List<ToolInfo> geoDataTools = message.getAddedTools().stream()
                .filter(tool -> "geo-data".equals(tool.getMcpServerName()))
                .toList();
        assertEquals(2, geoDataTools.size());
        assertTrue(geoDataTools.stream().allMatch(tool -> tool.getName().startsWith("geo-data-")));
        
        // Verify aws-docs tools
        List<ToolInfo> awsDocsTools = message.getAddedTools().stream()
                .filter(tool -> "aws-docs".equals(tool.getMcpServerName()))
                .toList();
        assertEquals(1, awsDocsTools.size());
        assertTrue(awsDocsTools.get(0).getName().startsWith("aws-docs-"));
    }

    @Test
    void testMcpServerNameHandling_MixedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .messageId("test-uuid-123")
                .timestamp(OffsetDateTime.now())
                .addedTools(Arrays.asList(
                    ToolInfo.builder().name("builtin-tool").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build()
                ))
                .removedTools(Arrays.asList(
                    ToolInfo.builder().name("old-builtin-tool").mcpServerName("built-in").build(),
                    ToolInfo.builder().name("aws-docs-old").mcpServerName("aws-docs").build()
                ))
                .source("agent")
                .build();

        // Verify added tools
        assertEquals(2, message.getAddedTools().size());
        assertEquals("built-in", message.getAddedTools().get(0).getMcpServerName());
        assertEquals("geo-data", message.getAddedTools().get(1).getMcpServerName());
        
        // Verify removed tools
        assertEquals(2, message.getRemovedTools().size());
        assertEquals("built-in", message.getRemovedTools().get(0).getMcpServerName());
        assertEquals("aws-docs", message.getRemovedTools().get(1).getMcpServerName());
    }
}