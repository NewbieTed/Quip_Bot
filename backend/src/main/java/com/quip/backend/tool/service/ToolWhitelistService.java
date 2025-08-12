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
import com.quip.backend.tool.model.Tool;
import com.quip.backend.tool.model.ToolWhitelist;
import com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.AddToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.RemoveToolWhitelistRequestDto;
import com.quip.backend.tool.dto.request.ConversationDto;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.common.exception.ConversationInProgressException;
import com.quip.backend.common.exception.InterruptedToolConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
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

        // Determine the highest scope in the update request and validate accordingly
        ToolWhitelistScope highestScope = determineHighestScope(updateRequest);
        
        // Validate conversation state based on the highest scope and collect conversations to notify
        List<AssistantConversation> conversationsToNotify = new ArrayList<>();
        switch (highestScope) {
            case GLOBAL -> {
                // For global scope, check all servers where member has conversations
                conversationsToNotify = validateNoProcessingConversationsGlobally(memberId);
            }
            case SERVER -> {
                // For server scope, check only the current server
                conversationsToNotify = validateNoProcessingConversationsInServer(memberId, serverId);
            }
            case CONVERSATION -> {
                // For conversation scope, validate specific conversations
                conversationsToNotify = validateConversationScopeUpdates(updateRequest, memberId, serverId);
            }
        }

        // Validate that no interrupted tool is being added to whitelist
        validateNoInterruptedToolsBeingAdded(updateRequest.getAddRequests(), conversationsToNotify);

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
        
        // Notify agent for all relevant conversations
        if (!conversationsToNotify.isEmpty()) {
            notifyAgentOfWhitelistUpdate(memberId, conversationsToNotify, addedToolNames, removedToolNames);
        }
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
     * Determines the highest scope from all add and remove requests in the update.
     * Scope hierarchy: GLOBAL > SERVER > CONVERSATION
     *
     * @param updateRequest the update request containing add and remove requests
     * @return the highest scope found in the request
     */
    private ToolWhitelistScope determineHighestScope(UpdateToolWhitelistRequestDto updateRequest) {
        ToolWhitelistScope highestScope = ToolWhitelistScope.CONVERSATION; // Start with the lowest scope
        
        // Check add requests
        if (updateRequest.getAddRequests() != null) {
            for (AddToolWhitelistRequestDto addRequest : updateRequest.getAddRequests()) {
                if (addRequest.getScope() == ToolWhitelistScope.GLOBAL) {
                    return ToolWhitelistScope.GLOBAL; // Global is highest, return immediately
                } else if (addRequest.getScope() == ToolWhitelistScope.SERVER && highestScope == ToolWhitelistScope.CONVERSATION) {
                    highestScope = ToolWhitelistScope.SERVER;
                }
            }
        }
        
        // Check remove requests
        if (updateRequest.getRemoveRequests() != null) {
            for (RemoveToolWhitelistRequestDto removeRequest : updateRequest.getRemoveRequests()) {
                if (removeRequest.getScope() == ToolWhitelistScope.GLOBAL) {
                    return ToolWhitelistScope.GLOBAL; // Global is highest, return immediately
                } else if (removeRequest.getScope() == ToolWhitelistScope.SERVER && highestScope == ToolWhitelistScope.CONVERSATION) {
                    highestScope = ToolWhitelistScope.SERVER;
                }
            }
        }
        
        return highestScope;
    }

    /**
     * Validates that there are no processing conversations globally for the member.
     * This is used for GLOBAL scope changes that affect all servers.
     * Returns all conversations (both active and inactive) that may need notification.
     *
     * @param memberId the member ID to check
     * @return list of all conversations for the member that may need notification
     * @throws ConversationInProgressException if any processing conversations are found
     */
    private List<AssistantConversation> validateNoProcessingConversationsGlobally(Long memberId) {
        // Get all conversations for the member across all servers (both active and inactive)
        List<AssistantConversation> allConversations = assistantConversationService.getAllConversationsForMember(memberId);
        
        // Check for any processing conversations
        for (AssistantConversation conversation : allConversations) {
            if (conversation.getIsProcessing() != null && conversation.getIsProcessing()) {
                throw new ConversationInProgressException(
                    "Cannot update global tool whitelist while member has processing conversations. " +
                    "Found processing conversation " + conversation.getId() + " in server " + conversation.getServerId() + 
                    ". Please wait for all conversations to complete before making global changes."
                );
            }
        }
        
        log.debug("Global validation passed: no processing conversations found for member {}", memberId);
        return allConversations;
    }

    /**
     * Validates that there are no processing conversations in the specific server.
     * This is used for SERVER scope changes.
     * Returns all conversations in the server that may need notification.
     *
     * @param memberId the member ID to check
     * @param serverId the server ID to check
     * @return list of conversations in the server that may need notification
     * @throws ConversationInProgressException if any processing conversations are found
     */
    private List<AssistantConversation> validateNoProcessingConversationsInServer(Long memberId, Long serverId) {
        // Get all conversations for the member in this specific server only
        List<AssistantConversation> serverConversations =
                assistantConversationService.getAllConversationsForMemberInServer(memberId, serverId);
        
        // Check for any processing conversations in this server
        for (AssistantConversation conversation : serverConversations) {
            if (conversation.getIsProcessing() != null && conversation.getIsProcessing()) {
                throw new ConversationInProgressException(
                    "Cannot update server tool whitelist while conversation " + conversation.getId() + 
                    " is being processed. Please wait for the conversation to complete before making changes."
                );
            }
        }
        
        log.debug("Server validation passed: no processing conversations found for member {} in server {}", memberId, serverId);
        return serverConversations;
    }

    /**
     * Validates conversation-specific updates by checking if the target conversations are processing.
     * This is used for CONVERSATION scope changes.
     *
     * @param updateRequest the update request containing conversation-specific changes
     * @param memberId the member ID
     * @param serverId the server ID
     * @return list of conversations that may need notification
     * @throws ConversationInProgressException if any target conversations are processing
     */
    private List<AssistantConversation> validateConversationScopeUpdates(UpdateToolWhitelistRequestDto updateRequest, Long memberId, Long serverId) {
        // For conversation scope, we need to validate specific conversations mentioned in the requests
        Set<Long> conversationIds = new java.util.HashSet<>();
        
        // Collect conversation IDs from add requests
        if (updateRequest.getAddRequests() != null) {
            updateRequest.getAddRequests().stream()
                .filter(req -> req.getScope() == ToolWhitelistScope.CONVERSATION && req.getAgentConversationId() != null)
                .forEach(req -> conversationIds.add(req.getAgentConversationId()));
        }
        
        // Collect conversation IDs from remove requests
        if (updateRequest.getRemoveRequests() != null) {
            updateRequest.getRemoveRequests().stream()
                .filter(req -> req.getScope() == ToolWhitelistScope.CONVERSATION && req.getAgentConversationId() != null)
                .forEach(req -> conversationIds.add(req.getAgentConversationId()));
        }

        // Use batch retrieval for better performance instead of individual queries
        List<AssistantConversation> conversations = assistantConversationService.getConversationsByIds(
            new java.util.ArrayList<>(conversationIds));
        
        // Validate that none of the target conversations are processing
        for (AssistantConversation conversation : conversations) {
            if (conversation != null && conversation.getIsProcessing() != null && conversation.getIsProcessing()) {
                throw new ConversationInProgressException(
                    "Cannot update conversation-scoped tool whitelist while conversation " + conversation.getId() + 
                    " is being processed. Please wait for the conversation to complete."
                );
            }
        }
        
        // Return the validated conversations for agent notification
        log.debug("Conversation validation passed: no target conversations are processing for member {} in server {}", memberId, serverId);
        return conversations;
    }

    /**
     * Validates that no interrupted tools are being added to the whitelist.
     * If a conversation is interrupted by a tool and that same tool is being added to the whitelist,
     * this creates a conflicting state and should be rejected.
     * All filtering is pushed to the database level for optimal performance.
     *
     * @param addRequest the add request
     * @param conversationsToNotify list of conversations that would be affected
     * @throws InterruptedToolConflictException if any interrupted tool is being added
     */
    private void validateNoInterruptedToolsBeingAdded(List<AddToolWhitelistRequestDto> addRequest, List<AssistantConversation> conversationsToNotify) {
        // Only check if there are add requests and conversations
        if (addRequest == null || addRequest.isEmpty() || conversationsToNotify.isEmpty()) {
            log.debug("No add requests or conversations to validate, skipping interrupted tool validation");
            return;
        }

        // Get list of tool names being added
        List<String> addedToolNames = addRequest.stream()
                .map(AddToolWhitelistRequestDto::getToolName)
                .collect(Collectors.toList());

        // Use database-level filtering to find conflicting tools
        List<String> conflictingToolNames = assistantConversationService.getConflictingInterruptedToolNames(
            conversationsToNotify, addedToolNames);

        // If any conflicts found, throw exception with the first conflicting tool
        if (!conflictingToolNames.isEmpty()) {
            String conflictingToolName = conflictingToolNames.get(0);
            
            // Find a conversation that has this interrupted tool for better error message
            AssistantConversation conflictingConversation = conversationsToNotify.stream()
                    .filter(conv -> conv.getIsInterrupt() && conv.getInterruptedToolId() != null)
                    .findFirst()
                    .orElse(null);

            throw new InterruptedToolConflictException(
                "Cannot add tool '" + conflictingToolName + "' to whitelist while it is currently " +
                "interrupting a conversation" + 
                (conflictingConversation != null ? 
                    " (conversation " + conflictingConversation.getId() + " in server " + conflictingConversation.getServerId() + ")" : "") +
                ". Please resolve the interruption first before adding this tool to the whitelist." +
                (conflictingToolNames.size() > 1 ? 
                    " Additional conflicting tools: " + String.join(", ", conflictingToolNames.subList(1, conflictingToolNames.size())) : "")
            );
        }
        
        log.debug("Interrupted tool validation passed: no interrupted tools are being added to whitelist");
    }


    /**
     * Notifies the agent service about tool whitelist updates for multiple conversations.
     * <p>
     * This method sends a single HTTP request to the agent service with a list of all
     * affected conversations. For global scope changes, this will include all servers
     * where the member has active conversations.
     * </p>
     *
     * @param memberId the member ID whose whitelist was updated
     * @param conversations list of conversations to notify (each represents a server/conversation pair)
     * @param addedToolNames list of tool names that were added
     * @param removedToolNames list of tool names that were removed
     */
    private void notifyAgentOfWhitelistUpdate(Long memberId, List<AssistantConversation> conversations, List<String> addedToolNames, List<String> removedToolNames) {
        if (conversations.isEmpty()) {
            log.debug("No conversations to notify for member {}", memberId);
            return;
        }


        // Convert AssistantConversation objects to DTOs
        List<ConversationDto> conversationDtos = conversations.stream()
                .map(conversation -> ConversationDto.builder()
                        .conversationId(conversation.getId())
                        .serverId(conversation.getServerId())
                        .memberId(conversation.getMemberId())
                        .build())
                .toList();

        // Prepare the request payload with the list of conversations
        Map<String, Object> requestBody = Map.of(
                "conversations", conversationDtos,
                "addedTools", addedToolNames,
                "removedTools", removedToolNames
        );

        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create HTTP entity with body and headers
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        String agentEndpoint = agentUrl + "/tool-whitelist/update";

        try {
            // Send blocking HTTP POST request to agent
            ResponseEntity<String> response = restTemplate.postForEntity(agentEndpoint, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully notified agent of whitelist update for member {} across {} conversations: {}", 
                        memberId, conversations.size(), response.getBody());
                
                // Log individual conversations for debugging
                conversations.forEach(conversation -> 
                    log.debug("Notified conversation {} in server {}", conversation.getId(), conversation.getServerId()));
            } else {
                log.warn("Agent notification returned non-success status {} for member {} across {} conversations: {}", 
                        response.getStatusCode(), memberId, conversations.size(), response.getBody());
            }
                    
        } catch (Exception e) {
            // Don't let agent notification failures break the main operation
            log.error("Failed to notify agent of whitelist update for member {} across {} conversations: {}", 
                    memberId, conversations.size(), e.getMessage(), e);
        }
    }

}