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
                "addedTools": ["tool1", "tool2"],
                "removedTools": ["tool3"],
                "source": "agent"
            }
            """;

        ToolUpdateMessage message = objectMapper.readValue(json, ToolUpdateMessage.class);

        assertNotNull(message);
        assertEquals("test-uuid-123", message.getMessageId());
        assertNotNull(message.getTimestamp());
        assertEquals(Arrays.asList("tool1", "tool2"), message.getAddedTools());
        assertEquals(Arrays.asList("tool3"), message.getRemovedTools());
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
                .addedTools(Arrays.asList("tool1", "tool2"))
                .removedTools(Arrays.asList("tool3"))
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
                .addedTools(Arrays.asList("tool1"))
                .removedTools(Collections.emptyList())
                .build();

        assertTrue(message.hasChanges());
    }

    @Test
    void testHasChanges_WithRemovedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Collections.emptyList())
                .removedTools(Arrays.asList("tool1"))
                .build();

        assertTrue(message.hasChanges());
    }

    @Test
    void testHasChanges_WithBothAddedAndRemovedTools() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList("tool1"))
                .removedTools(Arrays.asList("tool2"))
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
                .addedTools(Arrays.asList("tool1", "tool-2", "tool_3", "Tool123"))
                .removedTools(Arrays.asList("another-tool", "another_tool"))
                .build();

        assertTrue(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_InvalidNames() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList("tool with spaces", "tool@invalid"))
                .removedTools(Arrays.asList("valid-tool"))
                .build();

        assertFalse(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_EmptyToolName() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList("valid-tool", ""))
                .removedTools(Collections.emptyList())
                .build();

        assertFalse(message.hasValidToolNames());
    }

    @Test
    void testHasValidToolNames_NullToolName() {
        ToolUpdateMessage message = ToolUpdateMessage.builder()
                .addedTools(Arrays.asList("valid-tool", null))
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
}