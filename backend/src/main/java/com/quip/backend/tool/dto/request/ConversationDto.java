package com.quip.backend.tool.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a conversation for agent notifications.
 * This is used when notifying the agent about tool whitelist updates
 * that affect multiple conversations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    
    /**
     * The conversation ID
     */
    private Long conversationId;
    
    /**
     * The server ID where the conversation is active
     */
    private Long serverId;
    
    /**
     * The member ID who owns the conversation
     */
    private Long memberId;
}