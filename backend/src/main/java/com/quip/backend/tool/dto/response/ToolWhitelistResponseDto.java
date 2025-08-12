package com.quip.backend.tool.dto.response;

import com.quip.backend.tool.enums.ToolWhitelistScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object for returning tool whitelist information.
 * <p>
 * This DTO contains the essential information about a tool whitelist entry
 * to be returned to the client, including the member, tool, scope, and expiration details.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolWhitelistResponseDto {
    /**
     * ID of the member who has approval to use the tool.
     */
    private Long memberId;

    /**
     * Name of the member who has approval to use the tool.
     */
    private String memberName;

    /**
     * ID of the tool that has been approved for use.
     */
    private Long toolId;

    /**
     * Name of the tool that has been approved for use.
     */
    private String toolName;

    /**
     * ID of the server context for this approval.
     */
    private Long serverId;

    /**
     * Name of the server context for this approval.
     */
    private String serverName;

    /**
     * ID of the agent conversation for conversation-scoped approvals.
     */
    private Long agentConversationId;

    /**
     * Scope of the tool approval (global, server, or conversation).
     */
    private ToolWhitelistScope scope;

    /**
     * Optional expiration timestamp for the tool approval.
     * If null, the approval is permanent.
     */
    private OffsetDateTime expiresAt;

    /**
     * Flag indicating whether this whitelist entry is currently active (not expired).
     */
    private Boolean isActive;

    /**
     * Timestamp when this whitelist entry was created.
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this whitelist entry was last updated.
     */
    private OffsetDateTime updatedAt;
}