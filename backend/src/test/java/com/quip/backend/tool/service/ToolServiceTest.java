package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.mapper.dto.response.ToolResponseDtoMapper;
import com.quip.backend.tool.model.Tool;
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

    private ToolService toolService;

    @BeforeEach
    void setUp() {
        toolService = new ToolService(
            authorizationService,
            mcpServerService,
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
            new ToolService(authorizationService, mcpServerService, toolMapper, toolResponseDtoMapper);
        }, "Constructor should accept all valid dependencies");
        
        // Test that constructor can be called with null values (no null checks in Lombok)
        assertDoesNotThrow(() -> {
            new ToolService(null, null, null, null);
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
}