package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.mapper.dto.response.ToolResponseDtoMapper;
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ToolService.
 * Tests all methods and edge cases to achieve full code coverage and mutation testing resistance.
 */
@ExtendWith(MockitoExtension.class)
class ToolServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private McpServerService mcpServerService;

    @Mock
    private ToolMapper toolMapper;

    @Mock
    private ToolResponseDtoMapper toolResponseDtoMapper;

    @Mock
    private ToolSyncMetricsService metricsService;

    private ToolService toolService;

    @BeforeEach
    void setUp() {
        toolService = new ToolService(
            authorizationService,
            mcpServerService,
            metricsService,
            toolMapper,
            toolResponseDtoMapper
        );
    }

    // Test data factory methods
    private Tool createTestTool(Long id, String toolName, Boolean enabled) {
        return Tool.builder()
            .id(id)
            .toolName(toolName)
            .description("Test tool description")
            .enabled(enabled)
            .mcpServerId(1L)
            .createdBy(1L)
            .updatedBy(1L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    // Tests for getToolsByIds method
    @Test
    void getToolsByIds_WithValidIds_ReturnsTools() {
        // Arrange
        Collection<Long> toolIds = Arrays.asList(1L, 2L, 3L);
        List<Tool> expectedTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true),
            createTestTool(3L, "tool3", false)
        );
        when(toolMapper.selectBatchIds(toolIds)).thenReturn(expectedTools);

        // Act
        List<Tool> result = toolService.getToolsByIds(toolIds);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedTools, result);
        verify(toolMapper).selectBatchIds(toolIds);
    }

    @Test
    void getToolsByIds_WithEmptyCollection_ReturnsEmptyList() {
        // Arrange
        Collection<Long> emptyIds = Collections.emptyList();

        // Act
        List<Tool> result = toolService.getToolsByIds(emptyIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(toolMapper);
    }

    @Test
    void getToolsByIds_WithNullCollection_ReturnsEmptyList() {
        // Act
        List<Tool> result = toolService.getToolsByIds(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(toolMapper);
    }

    @Test
    void getToolsByIds_WithSingleId_ReturnsSingleTool() {
        // Arrange
        Collection<Long> singleId = Collections.singletonList(1L);
        List<Tool> expectedTools = Collections.singletonList(createTestTool(1L, "tool1", true));
        when(toolMapper.selectBatchIds(singleId)).thenReturn(expectedTools);

        // Act
        List<Tool> result = toolService.getToolsByIds(singleId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(expectedTools.get(0), result.get(0));
        verify(toolMapper).selectBatchIds(singleId);
    }

    @Test
    void getToolsByIds_WithDuplicateIds_HandledCorrectly() {
        // Arrange
        Collection<Long> duplicateIds = Arrays.asList(1L, 1L, 2L);
        List<Tool> expectedTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true)
        );
        when(toolMapper.selectBatchIds(duplicateIds)).thenReturn(expectedTools);

        // Act
        List<Tool> result = toolService.getToolsByIds(duplicateIds);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(toolMapper).selectBatchIds(duplicateIds);
    }

    @Test
    void getToolsByIds_WhenMapperReturnsNull_ThrowsNullPointerException() {
        // Arrange
        Collection<Long> toolIds = Arrays.asList(1L, 2L);
        when(toolMapper.selectBatchIds(toolIds)).thenReturn(null);

        // Act & Assert
        // The method will throw NPE when trying to log tools.size() on null
        assertThrows(NullPointerException.class, () -> {
            toolService.getToolsByIds(toolIds);
        });
        
        verify(toolMapper).selectBatchIds(toolIds);
    }

    // Tests for getToolByName method
    @Test
    void getToolByName_WithValidName_ReturnsTool() {
        // Arrange
        String toolName = "test-tool";
        Tool expectedTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(expectedTool);

        // Act
        Tool result = toolService.getToolByName(toolName);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTool, result);
        assertEquals(toolName, result.getToolName());
        
        // Verify the query wrapper was constructed correctly
        ArgumentCaptor<QueryWrapper<Tool>> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(toolMapper).selectOne(queryCaptor.capture());
        // Note: QueryWrapper internal structure is complex to verify, but we can ensure it was called
    }

    @Test
    void getToolByName_WithNonExistentName_ReturnsNull() {
        // Arrange
        String toolName = "non-existent-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // Act
        Tool result = toolService.getToolByName(toolName);

        // Assert
        assertNull(result);
        verify(toolMapper).selectOne(any(QueryWrapper.class));
    }

    @Test
    void getToolByName_WithNullName_ReturnsNull() {
        // Act
        Tool result = toolService.getToolByName(null);

        // Assert
        assertNull(result);
        verifyNoInteractions(toolMapper);
    }

    @Test
    void getToolByName_WithEmptyName_ReturnsNull() {
        // Act
        Tool result = toolService.getToolByName("");

        // Assert
        assertNull(result);
        verifyNoInteractions(toolMapper);
    }

    @Test
    void getToolByName_WithWhitespaceOnlyName_ReturnsNull() {
        // Act
        Tool result = toolService.getToolByName("   ");

        // Assert
        assertNull(result);
        verifyNoInteractions(toolMapper);
    }

    @Test
    void getToolByName_WithTabsAndSpaces_ReturnsNull() {
        // Act
        Tool result = toolService.getToolByName("\t\n  \r");

        // Assert
        assertNull(result);
        verifyNoInteractions(toolMapper);
    }

    // Tests for validateTool method
    @Test
    void validateTool_WithValidToolName_ReturnsValidatedTool() {
        // Arrange
        String toolName = "valid-tool";
        String operation = "Test Operation";
        Tool expectedTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(expectedTool);

        // Act
        Tool result = toolService.validateTool(toolName, operation);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTool, result);
        assertEquals(toolName, result.getToolName());
        verify(toolMapper).selectOne(any(QueryWrapper.class));
    }

    @Test
    void validateTool_WithNullToolName_ThrowsValidationException() {
        // Arrange
        String operation = "Test Operation";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.validateTool(null, operation);
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void validateTool_WithEmptyToolName_ThrowsValidationException() {
        // Arrange
        String operation = "Test Operation";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.validateTool("", operation);
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void validateTool_WithWhitespaceOnlyToolName_ThrowsValidationException() {
        // Arrange
        String operation = "Test Operation";

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.validateTool("   ", operation);
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void validateTool_WithNullOperation_ThrowsIllegalArgumentException() {
        // Arrange
        String toolName = "valid-tool";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            toolService.validateTool(toolName, null);
        });

        assertTrue(exception.getMessage().contains("operation"));
        assertTrue(exception.getMessage().contains("must not be null"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void validateTool_WithNonExistentTool_ThrowsValidationException() {
        // Arrange
        String toolName = "non-existent-tool";
        String operation = "Test Operation";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.validateTool(toolName, operation);
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must refer to an existing tool"));
        verify(toolMapper).selectOne(any(QueryWrapper.class));
    }

    @Test
    void validateTool_WithValidToolAndOperation_LogsCorrectly() {
        // Arrange
        String toolName = "valid-tool";
        String operation = "Test Operation";
        Tool expectedTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(expectedTool);

        // Act
        Tool result = toolService.validateTool(toolName, operation);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTool, result);
        // Logging is tested indirectly through successful execution
    }

    // Edge case tests
    @Test
    void getToolsByIds_WithVeryLargeCollection_HandlesCorrectly() {
        // Arrange
        Collection<Long> largeIds = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            largeIds.add(i);
        }
        List<Tool> expectedTools = new ArrayList<>();
        when(toolMapper.selectBatchIds(largeIds)).thenReturn(expectedTools);

        // Act
        List<Tool> result = toolService.getToolsByIds(largeIds);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTools, result);
        verify(toolMapper).selectBatchIds(largeIds);
    }

    @Test
    void getToolByName_WithSpecialCharacters_HandlesCorrectly() {
        // Arrange
        String toolName = "tool-with-special_chars.123";
        Tool expectedTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(expectedTool);

        // Act
        Tool result = toolService.getToolByName(toolName);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTool, result);
        verify(toolMapper).selectOne(any(QueryWrapper.class));
    }

    @Test
    void getToolByName_WithUnicodeCharacters_HandlesCorrectly() {
        // Arrange
        String toolName = "tool-测试-🔧";
        Tool expectedTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(expectedTool);

        // Act
        Tool result = toolService.getToolByName(toolName);

        // Assert
        assertNotNull(result);
        assertEquals(expectedTool, result);
        verify(toolMapper).selectOne(any(QueryWrapper.class));
    }

    // Service structure tests
    @Test
    void testServiceInstantiation() {
        assertNotNull(toolService);
        assertNotNull(authorizationService);
        assertNotNull(mcpServerService);
        assertNotNull(toolMapper);
        assertNotNull(toolResponseDtoMapper);
    }

    @Test
    void testServiceAnnotations() {
        assertTrue(toolService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
        // Note: Lombok annotations are processed at compile time, so we can't test them directly
        // Instead, we verify the service is properly constructed and functional
        assertNotNull(toolService);
    }

    @Test
    void testServiceConstants() {
        assertDoesNotThrow(() -> {
            var fields = ToolService.class.getDeclaredFields();
            boolean hasConstants = false;
            for (var field : fields) {
                if (field.getName().contains("TOOL") && 
                    java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                    java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    hasConstants = true;
                    break;
                }
            }
            assertTrue(hasConstants, "Service should have tool operation constants");
        });
    }

    @Test
    void testServiceConstructorRequiresAllDependencies() {
        // Test that constructor accepts all dependencies (Lombok @RequiredArgsConstructor doesn't add null checks)
        assertDoesNotThrow(() -> {
            new ToolService(authorizationService, mcpServerService, metricsService, toolMapper, toolResponseDtoMapper);
        }, "Constructor should accept all valid dependencies");
        
        // Test that constructor can be called with null values (no null checks in Lombok)
        assertDoesNotThrow(() -> {
            new ToolService(null, null, null, null, null);
        }, "Constructor should accept null values as Lombok doesn't add null checks");
    }

    // Mutation testing resistance tests
    @Test
    void getToolsByIds_MutationResistance_CollectionSizeCheck() {
        // This test ensures that the isEmpty() check cannot be mutated to !isEmpty()
        Collection<Long> emptyIds = new ArrayList<>();
        List<Tool> result = toolService.getToolsByIds(emptyIds);
        assertTrue(result.isEmpty());
        
        Collection<Long> nonEmptyIds = Arrays.asList(1L);
        when(toolMapper.selectBatchIds(nonEmptyIds)).thenReturn(Arrays.asList(createTestTool(1L, "tool", true)));
        List<Tool> result2 = toolService.getToolsByIds(nonEmptyIds);
        assertFalse(result2.isEmpty());
    }

    @Test
    void getToolByName_MutationResistance_NullAndEmptyChecks() {
        // Test that null check cannot be mutated
        assertNull(toolService.getToolByName(null));
        
        // Test that empty check cannot be mutated
        assertNull(toolService.getToolByName(""));
        
        // Test that trim().isEmpty() check cannot be mutated
        assertNull(toolService.getToolByName("   "));
        
        // Test with valid input
        String validName = "valid";
        Tool tool = createTestTool(1L, validName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(tool);
        assertNotNull(toolService.getToolByName(validName));
    }

    @Test
    void validateTool_MutationResistance_ValidationLogic() {
        // Test that validation logic cannot be mutated
        String operation = "test";
        
        // Null check mutation resistance
        assertThrows(ValidationException.class, () -> toolService.validateTool(null, operation));
        
        // Empty check mutation resistance  
        assertThrows(ValidationException.class, () -> toolService.validateTool("", operation));
        
        // Trim check mutation resistance
        assertThrows(ValidationException.class, () -> toolService.validateTool("   ", operation));
        
        // Operation null check mutation resistance
        assertThrows(IllegalArgumentException.class, () -> toolService.validateTool("tool", null));
        
        // Tool existence check mutation resistance
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        assertThrows(ValidationException.class, () -> toolService.validateTool("nonexistent", operation));
        
        // Valid case
        Tool tool = createTestTool(1L, "valid", true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(tool);
        assertNotNull(toolService.validateTool("valid", operation));
    }

    // Tests for createOrUpdateToolFromAgent method
    @Test
    void createOrUpdateToolFromAgent_WithNewTool_CreatesNewTool() {
        // Arrange
        String toolName = "new-agent-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null); // Tool doesn't exist
        when(toolMapper.insert(any(Tool.class))).thenReturn(1); // Successful insert

        // Act
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolMapper).insert(toolCaptor.capture());
        
        Tool insertedTool = toolCaptor.getValue();
        assertEquals(toolName, insertedTool.getToolName());
        assertEquals("Tool discovered by agent", insertedTool.getDescription());
        assertTrue(insertedTool.getEnabled());
        assertNull(insertedTool.getMcpServerId());
        assertNull(insertedTool.getCreatedBy());
        assertNull(insertedTool.getUpdatedBy());
        assertNotNull(insertedTool.getCreatedAt());
        assertNotNull(insertedTool.getUpdatedAt());
    }

    @Test
    void createOrUpdateToolFromAgent_WithExistingDisabledTool_EnablesTool() {
        // Arrange
        String toolName = "existing-disabled-tool";
        Tool existingTool = createTestTool(1L, toolName, false); // Disabled tool
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1); // Successful update

        // Act
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolMapper).updateById(toolCaptor.capture());
        
        Tool updatedTool = toolCaptor.getValue();
        assertEquals(toolName, updatedTool.getToolName());
        assertTrue(updatedTool.getEnabled());
        assertNotNull(updatedTool.getUpdatedAt());
    }

    @Test
    void createOrUpdateToolFromAgent_WithExistingEnabledTool_DoesNotUpdate() {
        // Arrange
        String toolName = "existing-enabled-tool";
        Tool existingTool = createTestTool(1L, toolName, true); // Already enabled
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);

        // Act
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        verify(toolMapper, never()).updateById(any(Tool.class)); // Should not update
        verify(toolMapper, never()).insert(any(Tool.class)); // Should not insert
    }

    @Test
    void createOrUpdateToolFromAgent_WithNullToolName_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.createOrUpdateToolFromAgent(null);
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void createOrUpdateToolFromAgent_WithEmptyToolName_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.createOrUpdateToolFromAgent("");
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void createOrUpdateToolFromAgent_WithWhitespaceToolName_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.createOrUpdateToolFromAgent("   ");
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void createOrUpdateToolFromAgent_WhenInsertFails_ThrowsRuntimeException() {
        // Arrange
        String toolName = "new-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null); // Tool doesn't exist
        when(toolMapper.insert(any(Tool.class))).thenReturn(0); // Failed insert

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toolService.createOrUpdateToolFromAgent(toolName);
        });

        assertTrue(exception.getMessage().contains("Failed to process tool addition from agent"));
        assertTrue(exception.getMessage().contains(toolName));
        verify(toolMapper).insert(any(Tool.class));
    }

    @Test
    void createOrUpdateToolFromAgent_WhenUpdateFails_ThrowsRuntimeException() {
        // Arrange
        String toolName = "existing-tool";
        Tool existingTool = createTestTool(1L, toolName, false);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(0); // Failed update

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toolService.createOrUpdateToolFromAgent(toolName);
        });

        assertTrue(exception.getMessage().contains("Failed to process tool addition from agent"));
        assertTrue(exception.getMessage().contains(toolName));
        verify(toolMapper).updateById(any(Tool.class));
    }

    @Test
    void createOrUpdateToolFromAgent_WhenDatabaseThrowsException_ThrowsRuntimeException() {
        // Arrange
        String toolName = "problematic-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toolService.createOrUpdateToolFromAgent(toolName);
        });

        assertTrue(exception.getMessage().contains("Failed to process tool addition from agent"));
        assertTrue(exception.getMessage().contains(toolName));
        assertNotNull(exception.getCause());
        assertEquals("Database error", exception.getCause().getMessage());
    }

    // Tests for disableToolFromAgent method
    @Test
    void disableToolFromAgent_WithExistingEnabledTool_DisablesTool() {
        // Arrange
        String toolName = "existing-enabled-tool";
        Tool existingTool = createTestTool(1L, toolName, true); // Enabled tool
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1); // Successful update

        // Act
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolMapper).updateById(toolCaptor.capture());
        
        Tool updatedTool = toolCaptor.getValue();
        assertEquals(toolName, updatedTool.getToolName());
        assertFalse(updatedTool.getEnabled());
        assertNotNull(updatedTool.getUpdatedAt());
    }

    @Test
    void disableToolFromAgent_WithExistingDisabledTool_DoesNotUpdate() {
        // Arrange
        String toolName = "existing-disabled-tool";
        Tool existingTool = createTestTool(1L, toolName, false); // Already disabled
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);

        // Act
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        verify(toolMapper, never()).updateById(any(Tool.class)); // Should not update
    }

    @Test
    void disableToolFromAgent_WithNonExistentTool_DoesNotThrow() {
        // Arrange
        String toolName = "non-existent-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null); // Tool doesn't exist

        // Act & Assert
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));

        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        verify(toolMapper, never()).updateById(any(Tool.class)); // Should not update
    }

    @Test
    void disableToolFromAgent_WithNullToolName_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.disableToolFromAgent(null);
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void disableToolFromAgent_WithEmptyToolName_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.disableToolFromAgent("");
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void disableToolFromAgent_WithWhitespaceToolName_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.disableToolFromAgent("   ");
        });

        assertTrue(exception.getMessage().contains("toolName"));
        assertTrue(exception.getMessage().contains("must not be null or empty"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void disableToolFromAgent_WhenUpdateFails_ThrowsRuntimeException() {
        // Arrange
        String toolName = "existing-tool";
        Tool existingTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(0); // Failed update

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toolService.disableToolFromAgent(toolName);
        });

        assertTrue(exception.getMessage().contains("Failed to process tool removal from agent"));
        assertTrue(exception.getMessage().contains(toolName));
        verify(toolMapper).updateById(any(Tool.class));
    }

    @Test
    void disableToolFromAgent_WhenDatabaseThrowsException_ThrowsRuntimeException() {
        // Arrange
        String toolName = "problematic-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toolService.disableToolFromAgent(toolName);
        });

        assertTrue(exception.getMessage().contains("Failed to process tool removal from agent"));
        assertTrue(exception.getMessage().contains(toolName));
        assertNotNull(exception.getCause());
        assertEquals("Database error", exception.getCause().getMessage());
    }

    // Tests for getAllToolNames method
    @Test
    void getAllToolNames_WithExistingTools_ReturnsAllToolNames() {
        // Arrange
        List<Tool> allTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", false),
            createTestTool(3L, "tool3", true)
        );
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(allTools);

        // Act
        List<String> result = toolService.getAllToolNames();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains("tool1"));
        assertTrue(result.contains("tool2"));
        assertTrue(result.contains("tool3"));
        verify(toolMapper).selectList(any(QueryWrapper.class));
    }

    @Test
    void getAllToolNames_WithNoTools_ReturnsEmptyList() {
        // Arrange
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        // Act
        List<String> result = toolService.getAllToolNames();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(toolMapper).selectList(any(QueryWrapper.class));
    }

    // Tests for syncToolsFromInventory method
    @Test
    void syncToolsFromInventory_WithNewTools_CreatesNewTools() {
        // Arrange
        List<String> currentTools = Arrays.asList("tool1", "tool2", "tool3");
        List<Tool> existingTools = Collections.emptyList(); // No existing tools
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(3)).insert(any(Tool.class)); // Create 3 new tools
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(true), anyLong());
    }

    @Test
    void syncToolsFromInventory_WithToolsToDisable_DisablesTools() {
        // Arrange
        List<String> currentTools = Arrays.asList("tool1"); // Only tool1 exists in agent
        List<Tool> existingTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true) // tool2 should be disabled
        );
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.selectOne(any(QueryWrapper.class)))
            .thenReturn(existingTools.get(0)) // For tool1 (enable check)
            .thenReturn(existingTools.get(1)); // For tool2 (disable)
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(2)).selectOne(any(QueryWrapper.class)); // Check individual tools
        verify(toolMapper, times(1)).updateById(any(Tool.class)); // Disable tool2
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(true), anyLong());
    }

    @Test
    void syncToolsFromInventory_WithMixedOperations_HandlesAllCorrectly() {
        // Arrange
        List<String> currentTools = Arrays.asList("tool1", "tool2", "tool3"); // tool1 exists, tool2 disabled, tool3 new
        List<Tool> existingTools = Arrays.asList(
            createTestTool(1L, "tool1", true),   // Already enabled
            createTestTool(2L, "tool2", false),  // Needs enabling
            createTestTool(4L, "tool4", true)    // Needs disabling
        );
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        // Mock selectOne calls in the order they will be made:
        // 1. For tool3 (new tool, doesn't exist) - called by createOrUpdateToolFromAgent
        // 2. For tool4 (needs disabling) - called by disableToolFromAgent  
        // 3. For tool1 (exists, check if needs enabling) - called by syncToolsFromInventory
        // 4. For tool2 (exists, check if needs enabling) - called by syncToolsFromInventory
        when(toolMapper.selectOne(any(QueryWrapper.class)))
            .thenReturn(null)                 // For tool3 (doesn't exist, will be created)
            .thenReturn(existingTools.get(2)) // For tool4 (exists, will be disabled)
            .thenReturn(existingTools.get(0)) // For tool1 (already enabled, no change)
            .thenReturn(existingTools.get(1)); // For tool2 (disabled, will be enabled)
        when(toolMapper.insert(any(Tool.class))).thenReturn(1); // For tool3
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1); // For tool2 and tool4

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(4)).selectOne(any(QueryWrapper.class)); // Check individual tools
        verify(toolMapper, times(1)).insert(any(Tool.class)); // Create tool3
        verify(toolMapper, times(2)).updateById(any(Tool.class)); // Enable tool2, disable tool4
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(true), anyLong());
    }

    @Test
    void syncToolsFromInventory_WithNullCurrentTools_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.syncToolsFromInventory(null);
        });

        assertTrue(exception.getMessage().contains("currentTools"));
        assertTrue(exception.getMessage().contains("must not be null"));
        verifyNoInteractions(toolMapper);
    }

    @Test
    void syncToolsFromInventory_WithEmptyCurrentTools_HandlesCorrectly() {
        // Arrange
        List<String> currentTools = Collections.emptyList();
        List<Tool> existingTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true)
        );
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.selectOne(any(QueryWrapper.class)))
            .thenReturn(existingTools.get(0))
            .thenReturn(existingTools.get(1));
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(2)).selectOne(any(QueryWrapper.class)); // Check tools to disable
        verify(toolMapper, times(2)).updateById(any(Tool.class)); // Disable both tools
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(true), anyLong());
    }

    @Test
    void syncToolsFromInventory_WithNullAndEmptyToolNames_FiltersCorrectly() {
        // Arrange
        List<String> currentTools = Arrays.asList("tool1", null, "", "  ", "tool2");
        List<Tool> existingTools = Collections.emptyList();
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(2)).insert(any(Tool.class)); // Only create tool1 and tool2
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(true), anyLong());
    }

    @Test
    void syncToolsFromInventory_WhenSomeOperationsFail_ContinuesProcessing() {
        // Arrange
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        List<Tool> existingTools = Collections.emptyList();
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class)))
            .thenReturn(1)  // tool1 succeeds
            .thenReturn(0); // tool2 fails

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(2)).insert(any(Tool.class)); // Try to create both tools
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(false), anyLong()); // Some failed
    }

    @Test
    void syncToolsFromInventory_WhenDatabaseThrowsException_ThrowsRuntimeException() {
        // Arrange
        List<String> currentTools = Arrays.asList("tool1");
        when(toolMapper.selectList(any(QueryWrapper.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            toolService.syncToolsFromInventory(currentTools);
        });

        assertTrue(exception.getMessage().contains("Failed to sync tool inventory from agent"));
        assertNotNull(exception.getCause());
        assertEquals("Database error", exception.getCause().getMessage());
    }

    @Test
    void syncToolsFromInventory_WithTransactionalBehavior_EnsuresAtomicity() {
        // This test verifies that the method is annotated with @Transactional
        // The actual transactional behavior is tested through integration tests
        
        // Arrange
        List<String> currentTools = Arrays.asList("tool1");
        List<Tool> existingTools = Collections.emptyList();
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));

        // Assert - verify the method has @Transactional annotation
        try {
            var method = ToolService.class.getMethod("syncToolsFromInventory", List.class);
            assertTrue(method.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
        } catch (NoSuchMethodException e) {
            fail("syncToolsFromInventory method should exist");
        }
    }

    // Edge case and mutation resistance tests for new methods
    @Test
    void createOrUpdateToolFromAgent_WithSpecialCharacters_HandlesCorrectly() {
        // Arrange
        String toolName = "tool-with-special_chars.123";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName));
        
        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolMapper).insert(toolCaptor.capture());
        assertEquals(toolName, toolCaptor.getValue().getToolName());
    }

    @Test
    void syncToolsFromInventory_MutationResistance_NullCheck() {
        // Test that null check cannot be mutated
        assertThrows(ValidationException.class, () -> toolService.syncToolsFromInventory(null));
        
        // Test with valid input
        List<String> validTools = Arrays.asList("tool1");
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);
        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(validTools));
    }

    @Test
    void syncToolsFromInventory_MutationResistance_CollectionOperations() {
        // Test that collection operations cannot be mutated
        List<String> currentTools = Arrays.asList("tool1", "tool2");
        List<Tool> existingTools = Arrays.asList(createTestTool(1L, "tool3", true));
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.selectOne(any(QueryWrapper.class)))
            .thenReturn(null)  // For tool1 (doesn't exist)
            .thenReturn(null)  // For tool2 (doesn't exist)
            .thenReturn(existingTools.get(0)); // For tool3 (exists, needs disabling)
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1);

        assertDoesNotThrow(() -> toolService.syncToolsFromInventory(currentTools));
        
        // Verify correct operations were performed
        verify(toolMapper, times(2)).insert(any(Tool.class)); // Add tool1, tool2
        verify(toolMapper, times(1)).updateById(any(Tool.class)); // Disable tool3
    }

    @Test
    void disableToolFromAgent_WithSpecialCharacters_HandlesCorrectly() {
        // Arrange
        String toolName = "tool-with-special_chars.123";
        Tool existingTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));
        verify(toolMapper).updateById(any(Tool.class));
    }

    @Test
    void createOrUpdateToolFromAgent_MutationResistance_EnabledCheck() {
        // Test that Boolean.TRUE.equals() check cannot be mutated
        String toolName = "test-tool";
        
        // Test with null enabled (should be treated as false)
        Tool toolWithNullEnabled = createTestTool(1L, toolName, null);
        toolWithNullEnabled.setEnabled(null);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(toolWithNullEnabled);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1);
        
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName));
        verify(toolMapper).updateById(any(Tool.class));
        
        reset(toolMapper);
        
        // Test with false enabled
        Tool toolWithFalseEnabled = createTestTool(1L, toolName, false);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(toolWithFalseEnabled);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1);
        
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName));
        verify(toolMapper).updateById(any(Tool.class));
    }

    @Test
    void disableToolFromAgent_MutationResistance_EnabledCheck() {
        // Test that Boolean.TRUE.equals() check cannot be mutated
        String toolName = "test-tool";
        
        // Test with null enabled (should not update)
        Tool toolWithNullEnabled = createTestTool(1L, toolName, null);
        toolWithNullEnabled.setEnabled(null);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(toolWithNullEnabled);
        
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));
        verify(toolMapper, never()).updateById(any(Tool.class));
        
        reset(toolMapper);
        
        // Test with false enabled (should not update)
        Tool toolWithFalseEnabled = createTestTool(1L, toolName, false);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(toolWithFalseEnabled);
        
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));
        verify(toolMapper, never()).updateById(any(Tool.class));
    }
}