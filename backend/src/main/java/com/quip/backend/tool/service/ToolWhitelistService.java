package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.assistant.model.database.AssistantConversation;
import com.quip.backend.assistant.service.AssistantConversationService;
import com.quip.backend.authorization.constants.AuthorizationConstants;
import com.quip.backend.authorization.context.AuthorizationContext;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.channel.service.ChannelService;
import com.quip.backend.member.service.MemberService;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import com.quip.backend.tool.mapper.database.ToolWhitelistMapper;
import com.quip.backend.tool.mapper.dto.response.ToolWhitelistResponseDtoMapper;
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.model.ToolWhitelist;
import com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.AddToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.RemoveToolWhitelistRequestDto;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.common.exception.ConversationInProgressException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for managing tool whitelist permissions in the system.
 * <p>
 * This service handles the creation, retrieval, updating, and deletion of tool whitelist entries.
 * It provides functionality for managing member permissions to use specific tools,
 * with support for different scopes (global, server, conversation) and expiration times.
 * </p>
 * <p>
 * The service enforces authorization rules and provides methods for checking
 * whether a member has permission to use a specific tool in a given context.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolWhitelistService {

    // Service dependencies
    private final MemberService memberService;
    private final ChannelService channelService;
    private final AuthorizationService authorizationService;
    private final ToolService toolService;
    private final AssistantConversationService assistantConversationService;

    // Database mappers
    private final ToolWhitelistMapper toolWhitelistMapper;

    // DTO mappers
//    private final ToolWhitelistResponseDtoMapper toolWhitelistResponseDtoMapper;
    
    // HTTP client for agent communication
    private final RestTemplate restTemplate;
    
    // Configuration
    @Value("${app.agent.url}")
    private String agentUrl;

    private static final String ADD_TOOL_WHITELIST = "Tool Whitelist Addition";
    private static final String UPDATE_TOOL_WHITELIST = "Tool Whitelist Update";
    private static final String REMOVE_TOOL_WHITELIST = "Tool Whitelist Removal";
//    private static final String RETRIEVE_TOOL_WHITELIST = "Tool Whitelist Retrieval";
//    private static final String MANAGE_TOOL_WHITELIST = "Tool Whitelist Management";
//    private static final String CHECK_TOOL_PERMISSION = "Tool Permission Check";

    // TODO: Update tool whitelist handler

    /**
     * Gets the list of whitelisted tool names for a specific member and server context.
     * <p>
     * This method retrieves tools that are whitelisted for the member at different scopes:
     * - Global scope: Tools available to the member across all servers
     * - Server scope: Tools available to the member within the specific server
     * - Conversation scope: Tools available to the member within a specific conversation (if provided)
     * </p>
     * <p>
     * This method uses the cached ToolWhitelistService to improve performance for
     * frequently accessed tool whitelist data.
     * </p>
     *
     * @param memberId The ID of the member
     * @param serverId The ID of the server
     * @return List of whitelisted tool names
     */
    public List<String> getWhitelistedToolNamesForNewConversation(Long memberId, Long serverId) {
        try {
            // Get valid tool whitelist entries with GLOBAL or SERVER scope
            List<ToolWhitelist> whitelistEntries = toolWhitelistMapper.selectActiveByMemberAndServerForNewConversation(
                    memberId, serverId, java.time.OffsetDateTime.now());

            if (whitelistEntries.isEmpty()) {
                log.debug("No whitelisted tools found for member {} in server {}", memberId, serverId);
                return List.of();
            }

            // Extract tool IDs from the whitelist entries
            Set<Long> whitelistedToolIds = whitelistEntries.stream()
                    .map(ToolWhitelist::getToolId)
                    .collect(Collectors.toSet());

            // Get all tools by IDs using ToolService
            List<Tool> tools = toolService.getToolsByIds(whitelistedToolIds);

            // Filter enabled tools and extract tool names
            List<String> toolNames = tools.stream()
                    .filter(tool -> tool.getEnabled() != null && tool.getEnabled())
                    .map(Tool::getToolName)
                    .collect(Collectors.toList());

            log.info("Found {} whitelisted tools for member {} in server {}: {}",
                    toolNames.size(), memberId, serverId, toolNames);

            return toolNames;
        } catch (Exception e) {
            log.error("Error retrieving whitelisted tools for member {} in server {}: {}",
                    memberId, serverId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Checks if a whitelist entry is applicable for the current context.
     * <p>
     * An entry is applicable if:
     * - It's a GLOBAL scope entry
     * - It's a SERVER scope entry
     * - It's a CONVERSATION scope entry and the conversation ID matches
     * </p>
     *
     * @param entry The whitelist entry to check
     * @param conversationId The current conversation ID (can be null)
     * @return true if the entry is applicable, false otherwise
     */
    private boolean isEntryApplicable(ToolWhitelist entry, Long conversationId) {
        ToolWhitelistScope scope = entry.getScope();

        return switch (scope) {
            case GLOBAL, SERVER -> true;
            case CONVERSATION -> conversationId != null && conversationId.equals(entry.getAgentConversationId());
        };
    }


    /**
     * Retrieves active tool whitelist entries for a specific member and server with caching.
     * <p>
     * This method caches member-specific tool whitelist data to improve performance
     * for permission checks and user-specific tool access.
     * </p>
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @return List of active tool whitelist entries for the member
     */
    @Cacheable(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
               key = "#serverId + ':member:' + #memberId")
    public List<ToolWhitelist> getActiveToolWhitelistByMemberAndServer(Long memberId, Long serverId) {
        log.debug("Retrieving active tool whitelist from database for memberId: {} and serverId: {}", memberId, serverId);
        return toolWhitelistMapper.selectActiveByMemberIdAndServerId(memberId, serverId, java.time.OffsetDateTime.now());
    }

    /**
     * Checks if a member has permission to use a specific tool with caching.
     * <p>
     * This method caches permission check results to improve performance for
     * frequently checked tool permissions. It checks active whitelist entries.
     * </p>
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param toolId the tool ID to check permission for
     * @return true if the member has permission, false otherwise
     */
    @Cacheable(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
               key = "#serverId + ':member:' + #memberId + ':tool:' + #toolId")
    public boolean hasToolPermission(Long memberId, Long serverId, Long toolId) {
        log.debug("Checking tool permission from database for memberId: {}, serverId: {}, toolId: {}", 
                 memberId, serverId, toolId);
        
        List<ToolWhitelist> activeWhitelist = toolWhitelistMapper.selectActiveByMemberIdAndServerId(
            memberId, serverId, java.time.OffsetDateTime.now());
        
        return activeWhitelist.stream()
                .anyMatch(whitelist -> whitelist.getToolId().equals(toolId));
    }

    /**
     * Creates or updates a tool whitelist entry and evicts related cache entries.
     * <p>
     * This method updates the database and evicts related cache entries to ensure
     * cache consistency. It evicts both member-specific and tool-specific caches.
     * Process deletion requests first, then addition requests to handle scope changes properly.
     * </p>
     *
     * @param updateRequest the update request containing new whitelist data
     */
    public void updateToolWhitelist(UpdateToolWhitelistRequestDto updateRequest) {
        Long memberId = updateRequest.getMemberId();
        Long channelId = updateRequest.getChannelId();

        memberService.validateMember(memberId, UPDATE_TOOL_WHITELIST);
        channelService.validateChannel(channelId, UPDATE_TOOL_WHITELIST);

        AuthorizationContext authorizationContext = authorizationService.validateAuthorization(
                memberId,
                channelId,
                AuthorizationConstants.MANAGE_TOOL_WHITELIST,
                UPDATE_TOOL_WHITELIST
        );

        // Get server ID from authorization context
        Long serverId = authorizationContext.server().getId();

        // Check if conversation state is fine to update the whitelist (conversation must be either not active, or not being processed).
        // Being processing implies the conversation is both active and not interrupted
        AssistantConversation assistantConversation = assistantConversationService.getActiveAssistantConversation(memberId, serverId);
        if (assistantConversation != null && assistantConversation.getIsProcessing()) {
            // Cannot perform change, abort
            throw new ConversationInProgressException(
                "Cannot update tool whitelist while conversation is being processed. " +
                        "Please wait for the conversation to complete before making changes."
            );
        }

        // Process removal requests first (important for scope changes)
        if (updateRequest.getRemoveRequests() != null && !updateRequest.getRemoveRequests().isEmpty()) {
            for (RemoveToolWhitelistRequestDto removeRequest : updateRequest.getRemoveRequests()) {
                processRemoveRequest(removeRequest, memberId, serverId);
            }
        }

        // Process addition requests after removals
        if (updateRequest.getAddRequests() != null && !updateRequest.getAddRequests().isEmpty()) {
            for (AddToolWhitelistRequestDto addRequest : updateRequest.getAddRequests()) {
                processAddRequest(addRequest, memberId, serverId);
            }
        }

        // Evict cache for the member and server
        evictToolWhitelistMemberCache(serverId, memberId);

        // Send HTTP request to agent to notify about whitelist update
        List<String> addedToolNames = updateRequest.getAddRequests() != null ? 
                updateRequest.getAddRequests().stream()
                        .map(AddToolWhitelistRequestDto::getToolName)
                        .collect(Collectors.toList()) : new ArrayList<>();
        
        List<String> removedToolNames = updateRequest.getRemoveRequests() != null ? 
                updateRequest.getRemoveRequests().stream()
                        .map(RemoveToolWhitelistRequestDto::getToolName)
                        .collect(Collectors.toList()) : new ArrayList<>();
        
        notifyAgentOfWhitelistUpdate(memberId, serverId, addedToolNames, removedToolNames);
    }

    /**
     * Processes a single remove request for tool whitelist.
     *
     * @param removeRequest the remove request
     * @param memberId the member ID
     * @param serverId the server ID
     */
    private void processRemoveRequest(RemoveToolWhitelistRequestDto removeRequest, Long memberId, Long serverId) {
        // Validate and get tool by name
        Tool tool = toolService.validateTool(removeRequest.getToolName(), REMOVE_TOOL_WHITELIST);

        // Build query conditions for deletion
        QueryWrapper<ToolWhitelist> queryWrapper = new QueryWrapper<ToolWhitelist>()
                .eq("member_id", memberId)
                .eq("server_id", serverId)
                .eq("tool_id", tool.getId())
                .eq("scope", removeRequest.getScope().getValue());

        // Add conversation ID condition if scope is CONVERSATION
        if (removeRequest.getScope() == ToolWhitelistScope.CONVERSATION) {
            if (removeRequest.getAgentConversationId() != null) {
                queryWrapper.eq("agent_conversation_id", removeRequest.getAgentConversationId());
            } else {
                log.warn("Conversation ID is required for CONVERSATION scope removal");
                return;
            }
        }

        // Delete the whitelist entry
        int deletedCount = toolWhitelistMapper.delete(queryWrapper);
        
        if (deletedCount > 0) {
            log.info("Removed tool whitelist entry: tool={}, member={}, server={}, scope={}", 
                    removeRequest.getToolName(), memberId, serverId, removeRequest.getScope());
            
            // Evict specific tool permission cache
            evictToolPermissionCache(memberId, serverId, tool.getId());
        } else {
            log.debug("No whitelist entry found to remove: tool={}, member={}, server={}, scope={}", 
                    removeRequest.getToolName(), memberId, serverId, removeRequest.getScope());
        }
    }

    /**
     * Processes a single add request for tool whitelist.
     *
     * @param addRequest the add request
     * @param memberId the member ID
     * @param serverId the server ID
     * @return the created tool whitelist entry
     */
    private ToolWhitelist processAddRequest(AddToolWhitelistRequestDto addRequest, Long memberId, Long serverId) {
        // Validate and get tool by name
        Tool tool = toolService.validateTool(addRequest.getToolName(), ADD_TOOL_WHITELIST);

        // Validate conversation ID for CONVERSATION scope
        Long conversationId = 0L; // Default value for non-conversation scoped entries
        if (addRequest.getScope() == ToolWhitelistScope.CONVERSATION) {
            if (addRequest.getAgentConversationId() != null) {
                conversationId = addRequest.getAgentConversationId();
            } else {
                log.warn("Conversation ID is required for CONVERSATION scope addition");
                return null;
            }
        }

        // Create new whitelist entry
        ToolWhitelist newEntry = ToolWhitelist.builder()
                .memberId(memberId)
                .toolId(tool.getId())
                .serverId(serverId)
                .agentConversationId(conversationId)
                .scope(addRequest.getScope())
                .expiresAt(addRequest.getExpiresAt())
                .createdBy(memberId)
                .updatedBy(memberId)
                .createdAt(java.time.OffsetDateTime.now())
                .updatedAt(java.time.OffsetDateTime.now())
                .build();

        // Insert or update the entry (using MyBatis Plus insertOrUpdate equivalent)
        // First try to find existing entry
        QueryWrapper<ToolWhitelist> queryWrapper = new QueryWrapper<ToolWhitelist>()
                .eq("member_id", memberId)
                .eq("server_id", serverId)
                .eq("tool_id", tool.getId())
                .eq("scope", addRequest.getScope().getValue())
                .eq("agent_conversation_id", conversationId);

        ToolWhitelist existingEntry = toolWhitelistMapper.selectOne(queryWrapper);
        
        if (existingEntry != null) {
            // Update existing entry
            existingEntry.setExpiresAt(addRequest.getExpiresAt());
            existingEntry.setUpdatedBy(memberId);
            existingEntry.setUpdatedAt(java.time.OffsetDateTime.now());
            
            toolWhitelistMapper.updateById(existingEntry);
            
            log.info("Updated tool whitelist entry: tool={}, member={}, server={}, scope={}", 
                    addRequest.getToolName(), memberId, serverId, addRequest.getScope());
            
            return existingEntry;
        } else {
            // Insert new entry
            toolWhitelistMapper.insert(newEntry);
            
            log.info("Added tool whitelist entry: tool={}, member={}, server={}, scope={}", 
                    addRequest.getToolName(), memberId, serverId, addRequest.getScope());
            
            return newEntry;
        }
    }

    /**
     * Evicts tool whitelist cache entries for a specific server.
     * <p>
     * This method removes all cached tool whitelist data for a server,
     * typically called when whitelist data is modified.
     * </p>
     *
     * @param serverId the server ID to evict cache for
     */
    @CacheEvict(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
                key = "#serverId")
    public void evictToolWhitelistCache(Long serverId) {
        log.debug("Evicting tool whitelist cache for serverId: {}", serverId);
    }

    /**
     * Evicts tool whitelist cache entries for a specific member in a server.
     * <p>
     * This method removes cached tool whitelist data for a specific member,
     * typically called when member-specific whitelist data is modified.
     * </p>
     *
     * @param serverId the server ID
     * @param memberId the member ID
     */
    @CacheEvict(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
                key = "#serverId + ':member:' + #memberId")
    public void evictToolWhitelistMemberCache(Long serverId, Long memberId) {
        log.debug("Evicting tool whitelist cache for serverId: {} and memberId: {}", serverId, memberId);
    }

    /**
     * Evicts all tool whitelist cache entries for a server using pattern-based eviction.
     * <p>
     * This method removes all cached tool whitelist data related to a server,
     * including member-specific entries, typically called during bulk operations.
     * </p>
     *
     * @param serverId the server ID to evict all cache entries for
     */
    @CacheEvict(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
                allEntries = true)
    public void evictAllToolWhitelistCache(Long serverId) {
        log.debug("Evicting all tool whitelist cache entries for serverId: {}", serverId);
    }

    /**
     * Retrieves all active tool whitelist entries for a specific server with caching.
     * <p>
     * This method caches server-wide tool whitelist data to improve performance
     * for administrative operations and bulk queries.
     * </p>
     *
     * @param serverId the server ID
     * @return List of all active tool whitelist entries for the server
     */
    @Cacheable(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
               key = "#serverId + ':server:all'")
    public List<ToolWhitelist> getAllActiveToolWhitelistByServer(Long serverId) {
        log.debug("Retrieving all active tool whitelist entries from database for serverId: {}", serverId);
        // Note: This would require a new mapper method to be implemented
        // For now, we'll use the existing method pattern but this shows the caching structure
        return toolWhitelistMapper.selectActiveByMemberIdAndServerId(null, serverId, java.time.OffsetDateTime.now());
    }

    /**
     * Removes a tool whitelist entry and evicts related cache entries.
     * <p>
     * This method removes a whitelist entry from the database and ensures
     * that all related cache entries are properly invalidated.
     * </p>
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param toolId the tool ID to remove from whitelist
     */
    @CacheEvict(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
                key = "#serverId + ':member:' + #memberId")
    public void removeToolWhitelist(Long memberId, Long serverId, Long toolId) {
        log.debug("Removing tool whitelist entry for memberId: {}, serverId: {}, toolId: {}", 
                 memberId, serverId, toolId);
        
        // Delete the whitelist entry using QueryWrapper for composite key
        toolWhitelistMapper.delete(new QueryWrapper<ToolWhitelist>()
                .eq("member_id", memberId)
                .eq("server_id", serverId)
                .eq("tool_id", toolId));
        
        // Also evict the specific tool permission cache
        evictToolPermissionCache(memberId, serverId, toolId);
    }

    /**
     * Evicts specific tool permission cache entry.
     * <p>
     * This method removes cached tool permission data for a specific member, server, and tool,
     * typically called when tool permissions are modified.
     * </p>
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param toolId the tool ID
     */
    @CacheEvict(value = CacheConfiguration.TOOL_WHITELIST_CACHE, 
                key = "#serverId + ':member:' + #memberId + ':tool:' + #toolId")
    public void evictToolPermissionCache(Long memberId, Long serverId, Long toolId) {
        log.debug("Evicting tool permission cache for memberId: {}, serverId: {}, toolId: {}", 
                 memberId, serverId, toolId);
    }

    /**
     * Notifies the agent service about tool whitelist updates.
     * <p>
     * This method sends a blocking HTTP request to the agent service to inform it about
     * changes to a member's tool whitelist. The agent can then update its internal
     * state or cache accordingly.
     * </p>
     *
     * @param memberId the member ID whose whitelist was updated
     * @param serverId the server ID where the update occurred
     * @param addedToolNames list of tool names that were added
     * @param removedToolNames list of tool names that were removed
     */
    private void notifyAgentOfWhitelistUpdate(Long memberId, Long serverId, List<String> addedToolNames, List<String> removedToolNames) {
        try {
            // Prepare the request payload with only the changes
            Map<String, Object> requestBody = Map.of(
                "serverId", serverId,
                "memberId", memberId,
                "addedTools", addedToolNames,
                "removedTools", removedToolNames
            );
            
            // Set up HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity with body and headers
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Send blocking HTTP POST request to agent
            String agentEndpoint = agentUrl + "/tool-whitelist/update";
            ResponseEntity<String> response = restTemplate.postForEntity(agentEndpoint, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully notified agent of whitelist update for member {} in server {}: {}", 
                        memberId, serverId, response.getBody());
                
                // Agent notified successfully
                log.debug("Agent response parsed successfully");
            } else {
                log.warn("Agent notification returned non-success status {} for member {} in server {}: {}", 
                        response.getStatusCode(), memberId, serverId, response.getBody());
            }
                    
        } catch (Exception e) {
            // Don't let agent notification failures break the main operation
            log.error("Failed to notify agent of whitelist update for member {} in server {}: {}", 
                    memberId, serverId, e.getMessage(), e);
        }
    }

}