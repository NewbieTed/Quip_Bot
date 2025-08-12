package com.quip.backend.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.quip.backend.assistant.mapper.database.AssistantConversationMapper;
import com.quip.backend.assistant.model.database.AssistantConversation;
import com.quip.backend.common.exception.ValidationException;
import com.quip.backend.config.redis.CacheConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantConversationService {
    private final AssistantConversationMapper assistantConversationMapper;

    /**
     * Retrieves the active assistant conversation for a member in a server.
     * Results are cached for 30 minutes to improve performance.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @return the active conversation, or null if not found
     * @throws IllegalArgumentException if memberId or serverId is null
     */
    @Cacheable(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE,
            key = "#serverId + ':member:' + #memberId + ':active'")
    public AssistantConversation validateAssistantConversation(Long memberId, Long serverId) {
        log.debug("Validating for active assistant conversation for member: {} in server: {}", memberId, serverId);

        // Should guarantee one conversation exist
        AssistantConversation activeConversation = findActiveConversation(memberId, serverId);
        if (activeConversation == null) {
            log.debug("No active assistant conversation found — validation failed");
            throw new ValidationException("No active assistant conversation found for member: " + memberId + " in server: " + serverId);
        }

        log.debug("Validated active assistant conversation {}", activeConversation.getId());
        return activeConversation;
    }

    /**
     * Retrieves the active assistant conversation for a member in a server.
     * Results are cached for 30 minutes to improve performance.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @return the active conversation, or null if not found
     * @throws IllegalArgumentException if memberId or serverId is null
     */
    @Cacheable(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE,
            key = "#serverId + ':member:' + #memberId + ':active'")
    public AssistantConversation getActiveAssistantConversation(Long memberId, Long serverId) {
        log.debug("Querying database for active assistant conversation for member: {} in server: {}", memberId, serverId);
        AssistantConversation activeConversation = findActiveConversation(memberId, serverId);

        if (activeConversation == null) {
            log.debug("No active assistant conversation found");
        } else {
            log.debug("Found active assistant conversation {}", activeConversation.getId());
        }

        return activeConversation;
    }

    /**
     * Creates a new active assistant conversation for a member in a server.
     * Deactivates any existing active conversations first and updates the cache.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param createdBy the user creating the conversation
     * @return the newly created conversation
     * @throws IllegalArgumentException if any parameter is null
     */
    @CachePut(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE, 
              key = "#serverId + ':member:' + #memberId + ':active'")
    public AssistantConversation createActiveConversation(Long memberId, Long serverId, Long createdBy) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by cannot be null");
        }

        // Deactivate existing active conversations using QueryWrapper
        int deactivated = assistantConversationMapper.update(null, 
                new UpdateWrapper<AssistantConversation>()
                        .set("is_active", false)
                        .set("updated_by", createdBy)
                        .eq("member_id", memberId)
                        .eq("server_id", serverId)
                        .eq("is_active", true));
        if (deactivated > 0) {
            log.debug("Deactivated {} existing conversations for member: {} in server: {}", 
                     deactivated, memberId, serverId);
        }

        // Create new active conversation
        AssistantConversation newConversation = AssistantConversation.builder()
                .memberId(memberId)
                .serverId(serverId)
                .isActive(true)
                .isInterrupt(false)
                .isProcessing(false)
                .createdBy(createdBy)
                .updatedBy(createdBy)
                .build();

        assistantConversationMapper.insert(newConversation);

        log.info("Created new active assistant conversation {} for member: {} in server: {}", 
                newConversation.getId(), memberId, serverId);
        
        return newConversation;
    }

    /**
     * Deactivates the current active conversation for a member in a server.
     * Evicts the cache entry for the conversation.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param updatedBy the user deactivating the conversation
     * @return true if a conversation was deactivated, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    @CacheEvict(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE, 
                key = "#serverId + ':member:' + #memberId + ':active'")
    public boolean deactivateActiveConversation(Long memberId, Long serverId, Long updatedBy) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("Updated by cannot be null");
        }
        // TODO: Call other service method

        AssistantConversation activeConversation = assistantConversationMapper.selectOne(
                new QueryWrapper<AssistantConversation>()
                        .eq("member_id", memberId)
                        .eq("server_id", serverId)
                        .eq("is_active", true)
                        .orderByDesc("updated_at"));
        if (activeConversation == null) {
            log.debug("No active conversation to deactivate for member: {} in server: {}", memberId, serverId);
            return false;
        }

        int updated = assistantConversationMapper.update(null,
                new UpdateWrapper<AssistantConversation>()
                        .set("is_active", false)
                        .set("updated_by", updatedBy)
                        .eq("id", activeConversation.getId()));

        if (updated > 0) {
            log.info("Deactivated assistant conversation {} for member: {} in server: {}",
                    activeConversation.getId(), memberId, serverId);
            return true;
        }
        
        return false;
    }

    /**
     * Updates the processing status of an active conversation.
     * Evicts the cache entry since the conversation data has changed.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param isProcessing the new processing status
     * @param updatedBy the user updating the status
     * @return true if the status was updated, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    @CacheEvict(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE, 
                key = "#serverId + ':member:' + #memberId + ':active'")
    public boolean updateProcessingStatus(Long memberId, Long serverId, boolean isProcessing, Long updatedBy) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("Updated by cannot be null");
        }

        // TODO: Call other service method

//        AssistantConversation activeConversation = assistantConversationMapper.findActiveConversation(memberId, serverId);
//        if (activeConversation == null) {
//            log.warn("No active conversation found to update processing status for member: {} in server: {}",
//                    memberId, serverId);
//            return false;
//        }
//
//        int updated = assistantConversationMapper.updateProcessingStatus(activeConversation.getId(), isProcessing, updatedBy);
//
//        if (updated > 0) {
//            log.debug("Updated processing status to {} for conversation {} (member: {}, server: {})",
//                     isProcessing, activeConversation.getId(), memberId, serverId);
//            return true;
//        }
        
        return false;
    }

    /**
     * Marks an active conversation as interrupted.
     * Evicts the cache entry since the conversation data has changed.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @param updatedBy the user marking the conversation as interrupted
     * @return true if the conversation was marked as interrupted, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    @CacheEvict(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE, 
                key = "#serverId + ':member:' + #memberId + ':active'")
    public boolean markAsInterrupted(Long memberId, Long serverId, Long updatedBy) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        if (updatedBy == null) {
            throw new IllegalArgumentException("Updated by cannot be null");
        }

        // TODO: Call other service method

//        AssistantConversation activeConversation = assistantConversationMapper.findActiveConversation(memberId, serverId);
//        if (activeConversation == null) {
//            log.warn("No active conversation found to mark as interrupted for member: {} in server: {}",
//                    memberId, serverId);
//            return false;
//        }
//
//        int updated = assistantConversationMapper.markAsInterrupted(activeConversation.getId(), updatedBy);
//
//        if (updated > 0) {
//            log.info("Marked conversation {} as interrupted for member: {} in server: {}",
//                    activeConversation.getId(), memberId, serverId);
//            return true;
//        }
        
        return false;
    }



    /**
     * Evicts cached conversation data for a specific member in a server.
     * This is a utility method for manual cache management.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     */
    @CacheEvict(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE, 
                key = "#serverId + ':member:' + #memberId + ':active'")
    public void evictMemberConversationCache(Long memberId, Long serverId) {
        if (memberId == null || serverId == null) {
            return;
        }
        
        log.debug("Evicted conversation cache for member: {} in server: {}", memberId, serverId);
    }

    /**
     * Evicts all cached conversation data.
     * This is a utility method for clearing all conversation cache entries.
     */
    @CacheEvict(value = CacheConfiguration.ASSISTANT_CONVERSATION_CACHE, 
                allEntries = true)
    public void evictAllConversationCache() {
        log.info("Evicted all assistant conversation cache entries");
    }

    private AssistantConversation findActiveConversation(Long memberId, Long serverId) {
        return assistantConversationMapper.selectOne(
                new QueryWrapper<AssistantConversation>()
                        .eq("member_id", memberId)
                        .eq("server_id", serverId)
                        .eq("is_active", true)
                        .orderByDesc("updated_at")
        );
    }

    /**
     * Retrieves all active assistant conversations for a member across all servers.
     * This method queries the database directly since we need cross-server data.
     *
     * @param memberId the member ID
     * @return list of all active conversations for the member
     */
    public List<AssistantConversation> getAllActiveConversationsForMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        
        // Query for all active conversations for this member across all servers
        QueryWrapper<AssistantConversation> queryWrapper = new QueryWrapper<AssistantConversation>()
                .eq("member_id", memberId)
                .eq("is_active", true);
        
        List<AssistantConversation> conversations = assistantConversationMapper.selectList(queryWrapper);
        log.debug("Found {} active conversations for member {}", conversations.size(), memberId);
        
        return conversations;
    }

    /**
     * Retrieves a conversation by its ID.
     *
     * @param conversationId the conversation ID
     * @return the conversation, or null if not found
     * @throws IllegalArgumentException if conversationId is null
     */
    public AssistantConversation getConversationById(Long conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("Conversation ID cannot be null");
        }
        
        AssistantConversation conversation = assistantConversationMapper.selectById(conversationId);
        log.debug("Retrieved conversation {} by ID", conversationId);
        
        return conversation;
    }

    /**
     * Retrieves multiple conversations by their IDs in a single batch query.
     * This method is more efficient than multiple individual getConversationById calls.
     *
     * @param conversationIds the list of conversation IDs
     * @return list of conversations found (may be fewer than requested if some IDs don't exist)
     * @throws IllegalArgumentException if conversationIds is null
     */
    public List<AssistantConversation> getConversationsByIds(List<Long> conversationIds) {
        if (conversationIds == null) {
            throw new IllegalArgumentException("Conversation IDs list cannot be null");
        }
        
        if (conversationIds.isEmpty()) {
            log.debug("Empty conversation IDs list provided, returning empty list");
            return List.of();
        }
        
        List<AssistantConversation> conversations = assistantConversationMapper.selectBatchIds(conversationIds);
        log.debug("Retrieved {} conversations from {} requested IDs", conversations.size(), conversationIds.size());
        
        return conversations;
    }

    /**
     * Retrieves all conversations (both active and inactive) for a member across all servers.
     * This method is used when we need to notify all conversations about whitelist changes.
     *
     * @param memberId the member ID
     * @return list of all conversations for the member
     */
    public List<AssistantConversation> getAllConversationsForMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        
        // Query for all conversations for this member across all servers (both active and inactive)
        QueryWrapper<AssistantConversation> queryWrapper = new QueryWrapper<AssistantConversation>()
                .eq("member_id", memberId);
        
        List<AssistantConversation> conversations = assistantConversationMapper.selectList(queryWrapper);
        log.debug("Found {} total conversations for member {}", conversations.size(), memberId);
        
        return conversations;
    }

    /**
     * Retrieves all conversations (both active and inactive) for a member in a specific server.
     * This method is more efficient than getting all conversations and filtering.
     *
     * @param memberId the member ID
     * @param serverId the server ID
     * @return list of conversations for the member in the specified server
     */
    public List<AssistantConversation> getAllConversationsForMemberInServer(Long memberId, Long serverId) {
        if (memberId == null) {
            throw new IllegalArgumentException("Member ID cannot be null");
        }
        if (serverId == null) {
            throw new IllegalArgumentException("Server ID cannot be null");
        }
        
        // Query for all conversations for this member in the specific server
        QueryWrapper<AssistantConversation> queryWrapper = new QueryWrapper<AssistantConversation>()
                .eq("member_id", memberId)
                .eq("server_id", serverId);
        
        List<AssistantConversation> conversations = assistantConversationMapper.selectList(queryWrapper);
        log.debug("Found {} conversations for member {} in server {}", conversations.size(), memberId, serverId);
        
        return conversations;
    }


    /**
     * Gets the tool names that are both being added to whitelist and currently interrupting conversations.
     * This method pushes all filtering to the database level for optimal performance.
     *
     * @param conversations list of conversations to check for interrupted tools
     * @param toolNames list of tool names being added to whitelist
     * @return list of conflicting tool names (both being added and currently interrupting)
     * @throws IllegalArgumentException if conversations or toolNames is null
     */
    public List<String> getConflictingInterruptedToolNames(List<AssistantConversation> conversations, List<String> toolNames) {
        if (conversations == null) {
            throw new IllegalArgumentException("Conversations list cannot be null");
        }
        if (toolNames == null) {
            throw new IllegalArgumentException("Tool names list cannot be null");
        }
        
        if (conversations.isEmpty() || toolNames.isEmpty()) {
            log.debug("No conversations or tool names provided, returning empty list");
            return List.of();
        }
        
        List<String> conflictingToolNames = assistantConversationMapper.getConflictingInterruptedToolNames(conversations, toolNames);
        log.debug("Found {} conflicting tool names for {} conversations and {} tool names", 
                 conflictingToolNames.size(), conversations.size(), toolNames.size());
        
        return conflictingToolNames;
    }

}
