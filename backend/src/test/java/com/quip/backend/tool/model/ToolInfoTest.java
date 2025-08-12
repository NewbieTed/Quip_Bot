package com.quip.backend.tool.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolInfo model.
 * Tests validation, JSON serialization, and business logic methods.
 */
class ToolInfoTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidToolInfo_BuiltIn() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("builtin-tool")
                .mcpServerName("built-in")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertTrue(violations.isEmpty());
        assertEquals("builtin-tool", toolInfo.getName());
        assertEquals("built-in", toolInfo.getMcpServerName());
        assertTrue(toolInfo.hasValidToolName());
    }

    @Test
    void testValidToolInfo_McpServer() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("geo-data-weather")
                .mcpServerName("geo-data")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertTrue(violations.isEmpty());
        assertEquals("geo-data-weather", toolInfo.getName());
        assertEquals("geo-data", toolInfo.getMcpServerName());
        assertTrue(toolInfo.hasValidToolName());
    }

    @Test
    void testValidToolNameCharacters() {
        // Given
        String[] validNames = {"tool1", "tool_2", "tool-3", "Tool4", "TOOL5", "tool123", "a", "A"};

        // When/Then
        for (String name : validNames) {
            ToolInfo toolInfo = ToolInfo.builder()
                    .name(name)
                    .mcpServerName("built-in")
                    .build();
            
            Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);
            assertTrue(violations.isEmpty(), "Tool name '" + name + "' should be valid");
            assertTrue(toolInfo.hasValidToolName(), "Tool name '" + name + "' should pass validation");
        }
    }

    @Test
    void testInvalidToolNameCharacters() {
        // Given
        String[] invalidNames = {"tool with spaces", "tool@invalid", "tool!", "tool#hash", "tool$dollar"};

        // When/Then
        for (String name : invalidNames) {
            ToolInfo toolInfo = ToolInfo.builder()
                    .name(name)
                    .mcpServerName("built-in")
                    .build();
            
            assertFalse(toolInfo.hasValidToolName(), "Tool name '" + name + "' should be invalid");
        }
    }

    @Test
    void testNullToolName() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name(null)
                .mcpServerName("built-in")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Tool name cannot be null")));
        assertFalse(toolInfo.hasValidToolName());
    }

    @Test
    void testBlankToolName() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("")
                .mcpServerName("built-in")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Tool name cannot be blank")));
        assertFalse(toolInfo.hasValidToolName());
    }

    @Test
    void testWhitespaceOnlyToolName() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("   ")
                .mcpServerName("built-in")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Tool name cannot be blank")));
        assertFalse(toolInfo.hasValidToolName());
    }

    @Test
    void testNullMcpServerName() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("valid-tool")
                .mcpServerName(null)
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("MCP server name cannot be null")));
    }

    @Test
    void testBlankMcpServerName() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("valid-tool")
                .mcpServerName("")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("MCP server name cannot be blank")));
    }

    @Test
    void testWhitespaceOnlyMcpServerName() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("valid-tool")
                .mcpServerName("   ")
                .build();

        // When
        Set<ConstraintViolation<ToolInfo>> violations = validator.validate(toolInfo);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("MCP server name cannot be blank")));
    }

    @Test
    void testJsonSerialization() throws Exception {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("geo-data-weather")
                .mcpServerName("geo-data")
                .build();

        // When
        String json = objectMapper.writeValueAsString(toolInfo);
        ToolInfo deserializedToolInfo = objectMapper.readValue(json, ToolInfo.class);

        // Then
        assertEquals(toolInfo.getName(), deserializedToolInfo.getName());
        assertEquals(toolInfo.getMcpServerName(), deserializedToolInfo.getMcpServerName());
        
        // Verify JSON structure
        assertTrue(json.contains("\"name\":\"geo-data-weather\""));
        assertTrue(json.contains("\"mcpServerName\":\"geo-data\""));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        // Given
        String json = """
            {
                "name": "aws-docs-search",
                "mcpServerName": "aws-docs"
            }
            """;

        // When
        ToolInfo toolInfo = objectMapper.readValue(json, ToolInfo.class);

        // Then
        assertEquals("aws-docs-search", toolInfo.getName());
        assertEquals("aws-docs", toolInfo.getMcpServerName());
        assertTrue(toolInfo.hasValidToolName());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        ToolInfo toolInfo1 = ToolInfo.builder()
                .name("tool1")
                .mcpServerName("built-in")
                .build();
        ToolInfo toolInfo2 = ToolInfo.builder()
                .name("tool1")
                .mcpServerName("built-in")
                .build();
        ToolInfo toolInfo3 = ToolInfo.builder()
                .name("tool2")
                .mcpServerName("built-in")
                .build();
        ToolInfo toolInfo4 = ToolInfo.builder()
                .name("tool1")
                .mcpServerName("geo-data")
                .build();

        // Then
        assertEquals(toolInfo1, toolInfo2);
        assertEquals(toolInfo1.hashCode(), toolInfo2.hashCode());
        
        assertNotEquals(toolInfo1, toolInfo3);
        assertNotEquals(toolInfo1.hashCode(), toolInfo3.hashCode());
        
        assertNotEquals(toolInfo1, toolInfo4);
        assertNotEquals(toolInfo1.hashCode(), toolInfo4.hashCode());
    }

    @Test
    void testToString() {
        // Given
        ToolInfo toolInfo = ToolInfo.builder()
                .name("geo-data-weather")
                .mcpServerName("geo-data")
                .build();

        // When
        String toString = toolInfo.toString();

        // Then
        assertTrue(toString.contains("ToolInfo"));
        assertTrue(toString.contains("geo-data-weather"));
        assertTrue(toString.contains("geo-data"));
    }

    @Test
    void testBuiltInToolsConvention() {
        // Given - Test that built-in tools use "built-in" as server name
        ToolInfo[] builtInTools = {
            ToolInfo.builder().name("readFile").mcpServerName("built-in").build(),
            ToolInfo.builder().name("writeFile").mcpServerName("built-in").build(),
            ToolInfo.builder().name("executeCommand").mcpServerName("built-in").build()
        };

        // When/Then
        for (ToolInfo tool : builtInTools) {
            assertEquals("built-in", tool.getMcpServerName());
            assertTrue(tool.hasValidToolName());
        }
    }

    @Test
    void testMcpToolsConvention() {
        // Given - Test that MCP tools follow naming convention
        ToolInfo[] mcpTools = {
            ToolInfo.builder().name("geo-data-weather").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("geo-data-location").mcpServerName("geo-data").build(),
            ToolInfo.builder().name("aws-docs-search").mcpServerName("aws-docs").build(),
            ToolInfo.builder().name("aws-docs-browse").mcpServerName("aws-docs").build()
        };

        // When/Then
        for (ToolInfo tool : mcpTools) {
            assertTrue(tool.getName().startsWith(tool.getMcpServerName() + "-"), 
                      "Tool '" + tool.getName() + "' should start with server name '" + tool.getMcpServerName() + "-'");
            assertTrue(tool.hasValidToolName());
        }
    }
}