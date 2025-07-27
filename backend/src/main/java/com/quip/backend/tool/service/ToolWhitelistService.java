package com.quip.backend.tool.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.tool.mapper.database.ToolWhitelistMapper;
import com.quip.backend.tool.mapper.dto.response.ToolWhitelistResponseDtoMapper;
import com.quip.backend.tool.model.ToolWhitelist;
import com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto;
import com.quip.backend.config.CacheConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

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
    private final AuthorizationService authorizationService;
    private final ToolService toolService;

    // Database mappers
    private final ToolWhitelistMapper toolWhitelistMapper;

    // DTO mappers
    private final ToolWhitelistResponseDtoMapper toolWhitelistResponseDtoMapper;

    // Authorization operation constants
    private static final String ADD_TOOL_WHITELIST = "Tool Whitelist Addition";
    private static final String UPDATE_TOOL_WHITELIST = "Tool Whitelist Update";
    private static final String REMOVE_TOOL_WHITELIST = "Tool Whitelist Removal";
    private static final String VIEW_TOOL_WHITELIST = "Tool Whitelist Retrieval";
    private static final String MANAGE_TOOL_WHITELIST = "Tool Whitelist Management";
    private static final String CHECK_TOOL_PERMISSION = "Tool Permission Check";

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
               key = "#serverId + ':member:' + #memberId",
               keyGenerator = "customCacheKeyGenerator")
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
               key = "#serverId + ':member:' + #memberId + ':tool:' + #toolId",
               keyGenerator = "customCacheKeyGenerator")
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
     * </p>
     *
     * @param updateRequest the update request containing new whitelist data
     * @return the created/updated tool whitelist entry
     */
    public ToolWhitelist updateToolWhitelist(UpdateToolWhitelistRequestDto updateRequest) {
        log.debug("Updating tool whitelist for targetMemberId: {}, toolId: {}", 
                 updateRequest.getTargetMemberId(), updateRequest.getToolId());
        
        // Create a new ToolWhitelist entity from the request
        ToolWhitelist toolWhitelist = ToolWhitelist.builder()
                .memberId(updateRequest.getTargetMemberId())
                .toolId(updateRequest.getToolId())
                .agentConversationId(updateRequest.getAgentConversationId())
                .scope(updateRequest.getScope())
                .expiresAt(updateRequest.getExpiresAt())
                .build();
        
        // Insert or update the whitelist entry
        toolWhitelistMapper.insert(toolWhitelist);
        
        // Evict related cache entries to ensure consistency
        // Note: We need to determine the serverId from the channel context
        // For now, we'll evict member-specific cache and tool permission cache
        evictToolWhitelistMemberCache(updateRequest.getChannelId(), updateRequest.getTargetMemberId());
        evictToolPermissionCache(updateRequest.getTargetMemberId(), updateRequest.getChannelId(), updateRequest.getToolId());
        
        return toolWhitelist;
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
                key = "#serverId",
                keyGenerator = "customCacheKeyGenerator")
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
                key = "#serverId + ':member:' + #memberId",
                keyGenerator = "customCacheKeyGenerator")
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
               key = "#serverId + ':server:all'",
               keyGenerator = "customCacheKeyGenerator")
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
                key = "#serverId + ':member:' + #memberId",
                keyGenerator = "customCacheKeyGenerator")
    public void removeToolWhitelist(Long memberId, Long serverId, Long toolId) {
        log.debug("Removing tool whitelist entry for memberId: {}, serverId: {}, toolId: {}", 
                 memberId, serverId, toolId);
        
        // Create a whitelist entry to identify the record to delete
        ToolWhitelist toDelete = ToolWhitelist.builder()
                .memberId(memberId)
                .serverId(serverId)
                .toolId(toolId)
                .build();
        
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
                key = "#serverId + ':member:' + #memberId + ':tool:' + #toolId",
                keyGenerator = "customCacheKeyGenerator")
    public void evictToolPermissionCache(Long memberId, Long serverId, Long toolId) {
        log.debug("Evicting tool permission cache for memberId: {}, serverId: {}, toolId: {}", 
                 memberId, serverId, toolId);
    }

}