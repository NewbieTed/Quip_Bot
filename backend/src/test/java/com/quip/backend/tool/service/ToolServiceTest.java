package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.tool.mapper.database.ToolMapper;
import com.quip.backend.tool.mapper.dto.response.ToolResponseDtoMapper;
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.model.ToolInfo;
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

    // Helper method to create test tools
    private Tool createTestTool(Long id, String name, Boolean enabled) {
        return Tool.builder()
            .id(id)
            .toolName(name)
            .description("Test tool")
            .enabled(enabled)
            .mcpServerId(0L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    // Helper method to create test MCP server
    private com.quip.backend.tool.model.McpServer createTestMcpServer(Long id, String serverName) {
        return com.quip.backend.tool.model.McpServer.builder()
                .id(id)
                .serverName(serverName)
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
        assertEquals(expectedTools, result);
        verify(toolMapper).selectBatchIds(toolIds);
    }

    @Test
    void getToolsByIds_WithEmptyIds_ReturnsEmptyList() {
        // Arrange
        Collection<Long> emptyIds = Collections.emptyList();

        // Act
        List<Tool> result = toolService.getToolsByIds(emptyIds);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(toolMapper);
    }

    @Test
    void getToolsByIds_WithNullIds_ReturnsEmptyList() {
        // Act
        List<Tool> result = toolService.getToolsByIds(null);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(toolMapper);
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
        assertEquals(expectedTool, result);
        verify(toolMapper).selectOne(any(QueryWrapper.class));
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

    // Tests for validateTool method
    @Test
    void validateTool_WithValidTool_ReturnsTool() {
        // Arrange
        String toolName = "valid-tool";
        String operation = "Test Operation";
        Tool expectedTool = createTestTool(1L, toolName, true);
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(expectedTool);

        // Act
        Tool result = toolService.validateTool(toolName, operation);

        // Assert
        assertEquals(expectedTool, result);
        verify(toolMapper).selectOne(any(QueryWrapper.class));
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

        assertTrue(exception.getMessage().contains("must refer to an existing tool"));
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

        assertTrue(exception.getMessage().contains("must not be null"));
        verifyNoInteractions(toolMapper);
    }

    // Tests for createOrUpdateToolFromAgent method (two-parameter version)
    @Test
    void createOrUpdateToolFromAgent_WithNewTool_CreatesNewTool() {
        // Arrange
        String toolName = "new-agent-tool";
        String mcpServerName = "test-server";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null); // Tool doesn't exist
        when(toolMapper.insert(any(Tool.class))).thenReturn(1); // Successful insert
        when(mcpServerService.findByServerName(mcpServerName)).thenReturn(createTestMcpServer(1L, mcpServerName));

        // Act
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName, mcpServerName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        ArgumentCaptor<Tool> toolCaptor = ArgumentCaptor.forClass(Tool.class);
        verify(toolMapper).insert(toolCaptor.capture());
        
        Tool insertedTool = toolCaptor.getValue();
        assertEquals(toolName, insertedTool.getToolName());
        assertEquals("Tool discovered by agent", insertedTool.getDescription());
        assertTrue(insertedTool.getEnabled());
        assertEquals(1L, insertedTool.getMcpServerId());
        assertNull(insertedTool.getCreatedBy());
        assertNull(insertedTool.getUpdatedBy());
        assertNotNull(insertedTool.getCreatedAt());
        assertNotNull(insertedTool.getUpdatedAt());
    }

    @Test
    void createOrUpdateToolFromAgent_WithExistingDisabledTool_EnablesTool() {
        // Arrange
        String toolName = "existing-disabled-tool";
        String mcpServerName = "test-server";
        Tool existingTool = createTestTool(1L, toolName, false); // Disabled tool
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(toolMapper.updateById(any(Tool.class))).thenReturn(1); // Successful update
        when(mcpServerService.findByServerName(mcpServerName)).thenReturn(createTestMcpServer(1L, mcpServerName));

        // Act
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName, mcpServerName));

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
        String mcpServerName = "test-server";
        Tool existingTool = createTestTool(1L, toolName, true); // Already enabled
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingTool);
        when(mcpServerService.findByServerName(mcpServerName)).thenReturn(createTestMcpServer(1L, mcpServerName));

        // Act
        assertDoesNotThrow(() -> toolService.createOrUpdateToolFromAgent(toolName, mcpServerName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        verify(toolMapper, never()).updateById(any(Tool.class)); // Should not update
        verify(toolMapper, never()).insert(any(Tool.class)); // Should not insert
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
    void disableToolFromAgent_WithNonExistentTool_DoesNotFail() {
        // Arrange
        String toolName = "non-existent-tool";
        when(toolMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // Act
        assertDoesNotThrow(() -> toolService.disableToolFromAgent(toolName));

        // Assert
        verify(toolMapper).selectOne(any(QueryWrapper.class)); // Check if tool exists
        verify(toolMapper, never()).updateById(any(Tool.class)); // Should not update
    }

    // Tests for getAllToolNames method
    @Test
    void getAllToolNames_ReturnsAllToolNames() {
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
        assertEquals(Arrays.asList("tool1", "tool2", "tool3"), result);
        verify(toolMapper).selectList(any(QueryWrapper.class));
    }

    // Tests for syncToolsFromInventoryWithServerInfo method
    @Test
    void syncToolsFromInventoryWithServerInfo_WithNewTools_CreatesNewToolsWithCorrectServerNames() {
        // Arrange
        List<ToolInfo> currentToolInfos = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("server1").build(),
            ToolInfo.builder().name("tool2").mcpServerName("server2").build(),
            ToolInfo.builder().name("tool3").mcpServerName("built-in").build()
        );
        List<Tool> existingTools = Collections.emptyList(); // No existing tools
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);
        when(mcpServerService.findByServerName("server1")).thenReturn(createTestMcpServer(1L, "server1"));
        when(mcpServerService.findByServerName("server2")).thenReturn(createTestMcpServer(2L, "server2"));
        when(mcpServerService.findByServerName("built-in")).thenReturn(createTestMcpServer(0L, "built-in"));

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventoryWithServerInfo(currentToolInfos));

        // Assert
        verify(toolMapper).selectList(any(QueryWrapper.class)); // Get existing tools
        verify(toolMapper, times(3)).insert(any(Tool.class)); // Create 3 new tools
        verify(mcpServerService).findByServerName("server1");
        verify(mcpServerService).findByServerName("server2");
        verify(mcpServerService).findByServerName("built-in");
        verify(metricsService).recordToolDatabaseOperation(eq("inventory_sync"), eq(true), anyLong());
    }

    @Test
    void syncToolsFromInventoryWithServerInfo_WithMixedServerNames_HandlesCorrectly() {
        // Arrange
        List<ToolInfo> currentToolInfos = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("external-server").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build()
        );
        List<Tool> existingTools = Collections.emptyList();
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);
        when(mcpServerService.findByServerName("external-server")).thenReturn(createTestMcpServer(5L, "external-server"));
        when(mcpServerService.findByServerName("built-in")).thenReturn(createTestMcpServer(0L, "built-in"));

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventoryWithServerInfo(currentToolInfos));

        // Assert
        verify(mcpServerService).findByServerName("external-server");
        verify(mcpServerService).findByServerName("built-in");
        verify(toolMapper, times(2)).insert(any(Tool.class));
    }

    @Test
    void syncToolsFromInventoryWithServerInfo_WithNullToolInfos_ThrowsValidationException() {
        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            toolService.syncToolsFromInventoryWithServerInfo(null);
        });

        assertTrue(exception.getMessage().contains("currentToolInfos"));
        assertTrue(exception.getMessage().contains("must not be null"));
    }

    @Test
    void syncToolsFromInventoryWithServerInfo_WithInvalidToolInfo_FiltersCorrectly() {
        // Arrange
        List<ToolInfo> currentToolInfos = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("server1").build(),
            ToolInfo.builder().name(null).mcpServerName("server2").build(), // Invalid - null name
            ToolInfo.builder().name("tool3").mcpServerName(null).build(), // Invalid - null server name
            ToolInfo.builder().name("").mcpServerName("server4").build(), // Invalid - empty name
            ToolInfo.builder().name("tool5").mcpServerName("server5").build()
        );
        List<Tool> existingTools = Collections.emptyList();
        
        when(toolMapper.selectList(any(QueryWrapper.class))).thenReturn(existingTools);
        when(toolMapper.insert(any(Tool.class))).thenReturn(1);
        when(mcpServerService.findByServerName("server1")).thenReturn(createTestMcpServer(1L, "server1"));
        when(mcpServerService.findByServerName("server5")).thenReturn(createTestMcpServer(5L, "server5"));

        // Act
        assertDoesNotThrow(() -> toolService.syncToolsFromInventoryWithServerInfo(currentToolInfos));

        // Assert - only valid tools should be processed
        verify(toolMapper, times(2)).insert(any(Tool.class)); // Only tool1 and tool5
        verify(mcpServerService).findByServerName("server1");
        verify(mcpServerService).findByServerName("server5");
        verify(mcpServerService, never()).findByServerName("server2");
        verify(mcpServerService, never()).findByServerName("server4");
    }
}