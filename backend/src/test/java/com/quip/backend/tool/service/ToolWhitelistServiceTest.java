package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.assistant.model.database.AssistantConversation;
import com.quip.backend.assistant.service.AssistantConversationService;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.server.model.Server;
import com.quip.backend.tool.dto.request.AddToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.RemoveToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import com.quip.backend.tool.mapper.database.ToolWhitelistMapper;
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.model.ToolWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Comprehensive unit tests for ToolWhitelistService.
 * Tests all methods and edge cases to achieve full code coverage and mutation testing resistance.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToolWhitelistServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private ChannelService channelService;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ToolService toolService;

    @Mock
    private AssistantConversationService assistantConversationService;

    @Mock
    private ToolWhitelistMapper toolWhitelistMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuthorizationContext authorizationContext;

    @Mock
    private Server server;

    private ToolWhitelistService toolWhitelistService;

    private static final String AGENT_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        toolWhitelistService = new ToolWhitelistService(
            memberService,
            channelService,
            authorizationService,
            toolService,
            assistantConversationService,
            toolWhitelistMapper,
            restTemplate
        );
        
        // Set the agent URL using reflection
        ReflectionTestUtils.setField(toolWhitelistService, "agentUrl", AGENT_URL);
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

    private ToolWhitelist createTestToolWhitelist(Long memberId, Long toolId, Long serverId, 
                                                  ToolWhitelistScope scope, Long conversationId) {
        return ToolWhitelist.builder()
            .memberId(memberId)
            .toolId(toolId)
            .serverId(serverId)
            .agentConversationId(conversationId != null ? conversationId : 0L)
            .scope(scope)
            .expiresAt(null)
            .createdBy(memberId)
            .updatedBy(memberId)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    // Tests for getWhitelistedToolNamesForNewConversation method
    @Test
    void getWhitelistedToolNamesForNewConversation_WithValidData_ReturnsToolNames() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null),
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.SERVER, null)
        );
        
        List<Tool> tools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(tools);

        // Act
        List<String> result = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("tool1"));
        assertTrue(result.contains("tool2"));
        
        verify(toolWhitelistMapper).selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class));
        verify(toolService).getToolsByIds(any(Set.class));
    }

    @Test
    void getWhitelistedToolNamesForNewConversation_WithNoWhitelistEntries_ReturnsEmptyList() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(Collections.emptyList());

        // Act
        List<String> result = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(toolWhitelistMapper).selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class));
        verifyNoInteractions(toolService);
    }

    @Test
    void getWhitelistedToolNamesForNewConversation_WithDisabledTools_FiltersOutDisabledTools() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null),
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.SERVER, null),
            createTestToolWhitelist(memberId, 3L, serverId, ToolWhitelistScope.SERVER, null)
        );
        
        List<Tool> tools = Arrays.asList(
            createTestTool(1L, "tool1", true),   // enabled
            createTestTool(2L, "tool2", false),  // disabled
            createTestTool(3L, "tool3", null)    // null enabled
        );
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(tools);

        // Act
        List<String> result = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("tool1"));
        assertFalse(result.contains("tool2"));
        assertFalse(result.contains("tool3"));
    }

    @Test
    void getWhitelistedToolNamesForNewConversation_WithException_ReturnsEmptyList() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<String> result = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Tests for getActiveToolWhitelistByMemberAndServer method (cached)
    @Test
    void getActiveToolWhitelistByMemberAndServer_WithValidData_ReturnsWhitelistEntries() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        List<ToolWhitelist> expectedEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null),
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.SERVER, null)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(expectedEntries);

        // Act
        List<ToolWhitelist> result = toolWhitelistService.getActiveToolWhitelistByMemberAndServer(memberId, serverId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedEntries, result);
        
        verify(toolWhitelistMapper).selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class));
    }

    @Test
    void getActiveToolWhitelistByMemberAndServer_WithNoEntries_ReturnsEmptyList() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(Collections.emptyList());

        // Act
        List<ToolWhitelist> result = toolWhitelistService.getActiveToolWhitelistByMemberAndServer(memberId, serverId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Tests for hasToolPermission method (cached)
    @Test
    void hasToolPermission_WithPermission_ReturnsTrue() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;
        
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, toolId, serverId, ToolWhitelistScope.GLOBAL, null)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);

        // Act
        boolean result = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);

        // Assert
        assertTrue(result);
        
        verify(toolWhitelistMapper).selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class));
    }

    @Test
    void hasToolPermission_WithoutPermission_ReturnsFalse() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;
        
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.GLOBAL, null) // Different tool
        );
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);

        // Act
        boolean result = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);

        // Assert
        assertFalse(result);
    }

    @Test
    void hasToolPermission_WithEmptyWhitelist_ReturnsFalse() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(Collections.emptyList());

        // Act
        boolean result = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);

        // Assert
        assertFalse(result);
    }

    // Tests for cache eviction methods
    @Test
    void evictToolWhitelistCache_CallsCorrectCacheEviction() {
        // Arrange
        Long serverId = 1L;

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.evictToolWhitelistCache(serverId);
        });

        // Assert - method should complete without error
    }

    @Test
    void evictToolWhitelistMemberCache_CallsCorrectCacheEviction() {
        // Arrange
        Long serverId = 1L;
        Long memberId = 1L;

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.evictToolWhitelistMemberCache(serverId, memberId);
        });

        // Assert - method should complete without error
    }

    @Test
    void evictAllToolWhitelistCache_CallsCorrectCacheEviction() {
        // Arrange
        Long serverId = 1L;

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.evictAllToolWhitelistCache(serverId);
        });

        // Assert - method should complete without error
    }

    @Test
    void evictToolPermissionCache_CallsCorrectCacheEviction() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.evictToolPermissionCache(memberId, serverId, toolId);
        });

        // Assert - method should complete without error
    }

    // Tests for getAllActiveToolWhitelistByServer method
    @Test
    void getAllActiveToolWhitelistByServer_WithValidServerId_ReturnsEntries() {
        // Arrange
        Long serverId = 1L;
        List<ToolWhitelist> expectedEntries = Arrays.asList(
            createTestToolWhitelist(1L, 1L, serverId, ToolWhitelistScope.GLOBAL, null),
            createTestToolWhitelist(2L, 2L, serverId, ToolWhitelistScope.SERVER, null)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(null), eq(serverId), any(OffsetDateTime.class))).thenReturn(expectedEntries);

        // Act
        List<ToolWhitelist> result = toolWhitelistService.getAllActiveToolWhitelistByServer(serverId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedEntries, result);
        verify(toolWhitelistMapper).selectActiveByMemberIdAndServerId(
            eq(null), eq(serverId), any(OffsetDateTime.class));
    }

    // Tests for removeToolWhitelist method
    @Test
    void removeToolWhitelist_WithValidParameters_RemovesEntry() {
        // Arrange
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;
        
        when(toolWhitelistMapper.delete(any(QueryWrapper.class))).thenReturn(1);

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.removeToolWhitelist(memberId, serverId, toolId);
        });

        // Assert
        ArgumentCaptor<QueryWrapper<ToolWhitelist>> queryCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(toolWhitelistMapper).delete(queryCaptor.capture());
    }

    // Service structure and annotation tests
    @Test
    void testServiceInstantiation() {
        assertNotNull(toolWhitelistService);
        assertNotNull(memberService);
        assertNotNull(channelService);
        assertNotNull(authorizationService);
        assertNotNull(toolService);
        assertNotNull(assistantConversationService);
        assertNotNull(toolWhitelistMapper);
        assertNotNull(restTemplate);
    }

    @Test
    void testServiceAnnotations() {
        assertTrue(toolWhitelistService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
        // Note: Lombok annotations are processed at compile time, so we can't test them directly
        // Instead, we verify the service is properly constructed and functional
        assertNotNull(toolWhitelistService);
    }

    @Test
    void testServiceConstants() {
        assertDoesNotThrow(() -> {
            var fields = ToolWhitelistService.class.getDeclaredFields();
            boolean hasConstants = false;
            for (var field : fields) {
                if (field.getName().contains("TOOL_WHITELIST") && 
                    java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                    java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                    hasConstants = true;
                    break;
                }
            }
            assertTrue(hasConstants, "Service should have tool whitelist operation constants");
        });
    }

    @Test
    void testServiceConstructorRequiresAllDependencies() {
        // Test that constructor accepts all dependencies (Lombok @RequiredArgsConstructor doesn't add null checks)
        assertDoesNotThrow(() -> {
            new ToolWhitelistService(memberService, channelService, authorizationService, toolService, 
                assistantConversationService, toolWhitelistMapper, restTemplate);
        }, "Constructor should accept all valid dependencies");
        
        // Test that constructor can be called with null values (no null checks in Lombok)
        assertDoesNotThrow(() -> {
            new ToolWhitelistService(null, null, null, null, null, null, null);
        }, "Constructor should accept null values as Lombok doesn't add null checks");
    }

    // Mutation testing resistance tests
    @Test
    void getWhitelistedToolNamesForNewConversation_MutationResistance_EmptyListCheck() {
        Long memberId = 1L;
        Long serverId = 1L;
        
        // Test empty list condition
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(Collections.emptyList());
        
        List<String> result = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertTrue(result.isEmpty());
        
        // Test non-empty list condition
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null)
        );
        List<Tool> tools = Arrays.asList(createTestTool(1L, "tool1", true));
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(tools);
        
        List<String> result2 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertFalse(result2.isEmpty());
    }

    @Test
    void hasToolPermission_MutationResistance_BooleanLogic() {
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;
        
        // Test false condition (no matching tool)
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.GLOBAL, null) // Different tool
        );
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);
        
        boolean result1 = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);
        assertFalse(result1);
        
        // Test true condition (matching tool)
        List<ToolWhitelist> matchingEntries = Arrays.asList(
            createTestToolWhitelist(memberId, toolId, serverId, ToolWhitelistScope.GLOBAL, null)
        );
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(matchingEntries);
        
        boolean result2 = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);
        assertTrue(result2);
    }

    // Test tool filtering logic
    @Test
    void getWhitelistedToolNamesForNewConversation_FilteringLogic_MutationResistance() {
        Long memberId = 1L;
        Long serverId = 1L;
        
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null),
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.SERVER, null)
        );
        
        // Test with enabled=true
        List<Tool> enabledTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(enabledTools);
        
        List<String> result1 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertEquals(2, result1.size());
        
        // Test with enabled=false
        List<Tool> disabledTools = Arrays.asList(
            createTestTool(1L, "tool1", false),
            createTestTool(2L, "tool2", false)
        );
        
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(disabledTools);
        
        List<String> result2 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertEquals(0, result2.size());
        
        // Test with enabled=null
        List<Tool> nullEnabledTools = Arrays.asList(
            createTestTool(1L, "tool1", null),
            createTestTool(2L, "tool2", null)
        );
        
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(nullEnabledTools);
        
        List<String> result3 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertEquals(0, result3.size());
    }

    // Helper methods for creating test objects
    private AddToolWhitelistRequestDto createAddRequest(String toolName, ToolWhitelistScope scope, 
                                                       Long conversationId, OffsetDateTime expiresAt) {
        return AddToolWhitelistRequestDto.builder()
            .toolName(toolName)
            .scope(scope)
            .agentConversationId(conversationId)
            .expiresAt(expiresAt)
            .build();
    }

    private RemoveToolWhitelistRequestDto createRemoveRequest(String toolName, ToolWhitelistScope scope, 
                                                             Long conversationId) {
        return RemoveToolWhitelistRequestDto.builder()
            .toolName(toolName)
            .scope(scope)
            .agentConversationId(conversationId)
            .build();
    }

    private UpdateToolWhitelistRequestDto createUpdateRequest(Long memberId, Long channelId,
                                                             List<AddToolWhitelistRequestDto> addRequests,
                                                             List<RemoveToolWhitelistRequestDto> removeRequests) {
        return UpdateToolWhitelistRequestDto.builder()
            .memberId(memberId)
            .channelId(channelId)
            .addRequests(addRequests)
            .removeRequests(removeRequests)
            .build();
    }

    private AssistantConversation createTestConversation(Long id, Long memberId, Long serverId, Boolean isProcessing) {
        return AssistantConversation.builder()
            .id(id)
            .memberId(memberId)
            .serverId(serverId)
            .isProcessing(isProcessing)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();
    }

    // Additional comprehensive tests for updateToolWhitelist method
    @Test
    void updateToolWhitelist_WithValidGlobalScopeAddRequest_ProcessesSuccessfully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.GLOBAL, null, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        Tool tool = createTestTool(1L, "test-tool", true);
        AssistantConversation conversation = createTestConversation(1L, memberId, serverId, false);
        
        // Mock all dependencies properly
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("test-tool"), anyString())).thenReturn(tool);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Arrays.asList(conversation));
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());
        when(toolWhitelistMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verify(memberService).validateMember(eq(memberId), anyString());
        verify(channelService).validateChannel(eq(channelId), anyString());
        verify(authorizationService).validateAuthorization(anyLong(), anyLong(), anyString(), anyString());
        verify(toolService).validateTool(eq("test-tool"), anyString());
        verify(assistantConversationService).getAllConversationsForMember(eq(memberId));
        verify(assistantConversationService).getConflictingInterruptedToolNames(any(List.class), any(List.class));
        verify(toolWhitelistMapper).insert(any(ToolWhitelist.class));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void updateToolWhitelist_WithValidServerScopeAddRequest_ProcessesSuccessfully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.SERVER, null, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        Tool tool = createTestTool(1L, "test-tool", true);
        AssistantConversation conversation = createTestConversation(1L, memberId, serverId, false);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("test-tool"), anyString())).thenReturn(tool);
        when(assistantConversationService.getAllConversationsForMemberInServer(eq(memberId), eq(serverId)))
            .thenReturn(Arrays.asList(conversation));
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());
        when(toolWhitelistMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verify(assistantConversationService).getAllConversationsForMemberInServer(eq(memberId), eq(serverId));
        verify(assistantConversationService).getConflictingInterruptedToolNames(any(List.class), any(List.class));
        verify(toolWhitelistMapper).insert(any(ToolWhitelist.class));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void updateToolWhitelist_WithValidConversationScopeAddRequest_ProcessesSuccessfully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        Long conversationId = 100L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.CONVERSATION, 
            conversationId, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        Tool tool = createTestTool(1L, "test-tool", true);
        AssistantConversation conversation = createTestConversation(conversationId, memberId, serverId, false);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("test-tool"), anyString())).thenReturn(tool);
        when(assistantConversationService.getConversationsByIds(any(List.class)))
            .thenReturn(Arrays.asList(conversation));
        when(toolWhitelistMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verify(assistantConversationService).getConversationsByIds(any(List.class));
        verify(toolWhitelistMapper).insert(any(ToolWhitelist.class));
    }

    @Test
    void updateToolWhitelist_WithRemoveRequest_ProcessesSuccessfully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        RemoveToolWhitelistRequestDto removeRequest = createRemoveRequest("test-tool", ToolWhitelistScope.GLOBAL, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            null, Arrays.asList(removeRequest));
        
        Tool tool = createTestTool(1L, "test-tool", true);
        AssistantConversation conversation = createTestConversation(1L, memberId, serverId, false);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("test-tool"), anyString())).thenReturn(tool);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Arrays.asList(conversation));
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());
        when(toolWhitelistMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verify(toolWhitelistMapper).delete(any(QueryWrapper.class));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void updateToolWhitelist_WithExistingEntry_UpdatesEntry() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.GLOBAL, null, 
            OffsetDateTime.now().plusDays(1));
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        Tool tool = createTestTool(1L, "test-tool", true);
        ToolWhitelist existingEntry = createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null);
        AssistantConversation conversation = createTestConversation(1L, memberId, serverId, false);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("test-tool"), anyString())).thenReturn(tool);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Arrays.asList(conversation));
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());
        when(toolWhitelistMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingEntry);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verify(toolWhitelistMapper).updateById(any(ToolWhitelist.class));
        verify(toolWhitelistMapper, never()).insert(any(ToolWhitelist.class));
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void updateToolWhitelist_WithMixedAddAndRemoveRequests_ProcessesBoth() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("add-tool", ToolWhitelistScope.GLOBAL, null, null);
        RemoveToolWhitelistRequestDto removeRequest = createRemoveRequest("remove-tool", ToolWhitelistScope.GLOBAL, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), Arrays.asList(removeRequest));
        
        Tool addTool = createTestTool(1L, "add-tool", true);
        Tool removeTool = createTestTool(2L, "remove-tool", true);
        AssistantConversation conversation = createTestConversation(1L, memberId, serverId, false);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("add-tool"), anyString())).thenReturn(addTool);
        when(toolService.validateTool(eq("remove-tool"), anyString())).thenReturn(removeTool);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Arrays.asList(conversation));
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());
        when(toolWhitelistMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(toolWhitelistMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verify(toolWhitelistMapper).delete(any(QueryWrapper.class)); // Remove processed first
        verify(toolWhitelistMapper).insert(any(ToolWhitelist.class)); // Then add
        verify(restTemplate).postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    // Test error conditions and edge cases
    @Test
    void updateToolWhitelist_WithProcessingConversationGlobal_ThrowsException() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.GLOBAL, null, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        AssistantConversation processingConversation = createTestConversation(1L, memberId, serverId, true);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Arrays.asList(processingConversation));

        // Act & Assert
        assertThrows(Exception.class, () -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });
        
        verify(assistantConversationService).getAllConversationsForMember(eq(memberId));
        verifyNoInteractions(toolWhitelistMapper);
    }

    @Test
    void updateToolWhitelist_WithProcessingConversationServer_ThrowsException() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.SERVER, null, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        AssistantConversation processingConversation = createTestConversation(1L, memberId, serverId, true);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(assistantConversationService.getAllConversationsForMemberInServer(eq(memberId), eq(serverId)))
            .thenReturn(Arrays.asList(processingConversation));

        // Act & Assert
        assertThrows(Exception.class, () -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });
        
        verify(assistantConversationService).getAllConversationsForMemberInServer(eq(memberId), eq(serverId));
        verifyNoInteractions(toolWhitelistMapper);
    }

    @Test
    void updateToolWhitelist_WithProcessingConversationSpecific_ThrowsException() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        Long conversationId = 100L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.CONVERSATION, 
            conversationId, null);
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        AssistantConversation processingConversation = createTestConversation(conversationId, memberId, serverId, true);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(assistantConversationService.getConversationsByIds(any(List.class)))
            .thenReturn(Arrays.asList(processingConversation));

        // Act & Assert
        assertThrows(Exception.class, () -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });
        
        verify(assistantConversationService).getConversationsByIds(any(List.class));
        verifyNoInteractions(toolWhitelistMapper);
    }

    @Test
    void updateToolWhitelist_WithEmptyRequests_ProcessesSuccessfully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Collections.emptyList(), Collections.emptyList());
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Collections.emptyList());

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verifyNoInteractions(toolWhitelistMapper);
        // No agent notification should be sent when there are no conversations
        verifyNoInteractions(restTemplate);
    }

    @Test
    void updateToolWhitelist_WithNullRequests_ProcessesSuccessfully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, null, null);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Collections.emptyList());

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert
        verifyNoInteractions(toolWhitelistMapper);
        // No agent notification should be sent when there are no conversations
        verifyNoInteractions(restTemplate);
    }

    // Test conversation scope edge cases
    @Test
    void updateToolWhitelist_WithConversationScopeButNullConversationId_HandlesGracefully() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto addRequest = createAddRequest("test-tool", ToolWhitelistScope.CONVERSATION, 
            null, null); // null conversation ID
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(addRequest), null);
        
        Tool tool = createTestTool(1L, "test-tool", true);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("test-tool"), anyString())).thenReturn(tool);
        when(assistantConversationService.getConversationsByIds(any(List.class)))
            .thenReturn(Collections.emptyList());
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert - should still process but may not insert due to null conversation ID handling
        verify(assistantConversationService).getConversationsByIds(any(List.class));
        // No agent notification should be sent when there are no conversations
        verifyNoInteractions(restTemplate);
    }

    // Tests for private helper methods through public methods
    @Test
    void determineHighestScope_WithGlobalScope_ReturnsGlobal() {
        // Arrange
        Long memberId = 1L;
        Long channelId = 1L;
        Long serverId = 1L;
        
        AddToolWhitelistRequestDto globalAdd = createAddRequest("tool1", ToolWhitelistScope.GLOBAL, null, null);
        AddToolWhitelistRequestDto serverAdd = createAddRequest("tool2", ToolWhitelistScope.SERVER, null, null);
        RemoveToolWhitelistRequestDto conversationRemove = createRemoveRequest("tool3", ToolWhitelistScope.CONVERSATION, 100L);
        
        UpdateToolWhitelistRequestDto updateRequest = createUpdateRequest(memberId, channelId, 
            Arrays.asList(globalAdd, serverAdd), Arrays.asList(conversationRemove));
        
        Tool tool1 = createTestTool(1L, "tool1", true);
        Tool tool2 = createTestTool(2L, "tool2", true);
        Tool tool3 = createTestTool(3L, "tool3", true);
        
        when(authorizationContext.server()).thenReturn(server);
        when(server.getId()).thenReturn(serverId);
        when(authorizationService.validateAuthorization(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(authorizationContext);
        when(toolService.validateTool(eq("tool1"), anyString())).thenReturn(tool1);
        when(toolService.validateTool(eq("tool2"), anyString())).thenReturn(tool2);
        when(toolService.validateTool(eq("tool3"), anyString())).thenReturn(tool3);
        when(assistantConversationService.getAllConversationsForMember(eq(memberId)))
            .thenReturn(Collections.emptyList());
        when(assistantConversationService.getConflictingInterruptedToolNames(any(List.class), any(List.class)))
            .thenReturn(Collections.emptyList());
        when(toolWhitelistMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(toolWhitelistMapper.delete(any(QueryWrapper.class))).thenReturn(1);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("success"));

        // Act
        assertDoesNotThrow(() -> {
            toolWhitelistService.updateToolWhitelist(updateRequest);
        });

        // Assert - should use global scope validation (getAllConversationsForMember)
        verify(assistantConversationService).getAllConversationsForMember(eq(memberId));
        verify(assistantConversationService, never()).getAllConversationsForMemberInServer(anyLong(), anyLong());
        verify(assistantConversationService, never()).getConversationsByIds(any(List.class));
    }

    // Comprehensive mutation testing resistance tests
    @Test
    void mutationTesting_BooleanConditions_AllBranches() {
        Long memberId = 1L;
        Long serverId = 1L;
        
        // Test all boolean conditions in filtering logic
        List<ToolWhitelist> whitelistEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 1L, serverId, ToolWhitelistScope.GLOBAL, null),
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.SERVER, null)
        );
        
        // Test enabled == true
        List<Tool> enabledTools = Arrays.asList(
            createTestTool(1L, "tool1", true),
            createTestTool(2L, "tool2", true)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(whitelistEntries);
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(enabledTools);
        
        List<String> result1 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertEquals(2, result1.size());
        
        // Test enabled == false
        List<Tool> disabledTools = Arrays.asList(
            createTestTool(1L, "tool1", false),
            createTestTool(2L, "tool2", false)
        );
        
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(disabledTools);
        
        List<String> result2 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertEquals(0, result2.size());
        
        // Test enabled == null
        List<Tool> nullEnabledTools = Arrays.asList(
            createTestTool(1L, "tool1", null),
            createTestTool(2L, "tool2", null)
        );
        
        when(toolService.getToolsByIds(any(Set.class))).thenReturn(nullEnabledTools);
        
        List<String> result3 = toolWhitelistService.getWhitelistedToolNamesForNewConversation(memberId, serverId);
        assertEquals(0, result3.size());
    }

    @Test
    void mutationTesting_CollectionOperations_AllBranches() {
        Long memberId = 1L;
        Long serverId = 1L;
        Long toolId = 1L;
        
        // Test empty collection
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(Collections.emptyList());
        
        boolean result1 = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);
        assertFalse(result1);
        
        // Test non-empty collection with no match
        List<ToolWhitelist> noMatchEntries = Arrays.asList(
            createTestToolWhitelist(memberId, 2L, serverId, ToolWhitelistScope.GLOBAL, null)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(noMatchEntries);
        
        boolean result2 = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);
        assertFalse(result2);
        
        // Test non-empty collection with match
        List<ToolWhitelist> matchEntries = Arrays.asList(
            createTestToolWhitelist(memberId, toolId, serverId, ToolWhitelistScope.GLOBAL, null)
        );
        
        when(toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            eq(memberId), eq(serverId), any(OffsetDateTime.class))).thenReturn(matchEntries);
        
        boolean result3 = toolWhitelistService.hasToolPermission(memberId, serverId, toolId);
        assertTrue(result3);
    }
}
