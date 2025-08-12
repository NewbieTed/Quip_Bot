package com.quip.backend.tool.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents a tool whitelist entry in the system.
 * <p>
 * Tool whitelist entries track which tools have been approved for use by specific members
 * in specific contexts. The approval can be scoped to global, server-wide, or conversation-specific
 * usage. Entries can have expiration times for temporary approvals.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tool_whitelist")
public class ToolWhitelist {
    /**
     * ID of the member who has approval to use the tool.
     * Part of the composite primary key.
     */
    @TableField("member_id")
    private Long memberId;

    /**
     * ID of the tool that has been approved for use.
     * Part of the composite primary key.
     */
    @TableField("tool_id")
    private Long toolId;

    /**
     * ID of the server context for this approval.
     * Part of the composite primary key.
     */
    @TableField("server_id")
    private Long serverId;

    /**
     * ID of the agent conversation for conversation-scoped approvals.
     * Part of the composite primary key. Default value is 0 for non-conversation scoped approvals.
     */
    @TableField("agent_conversation_id")
    private Long agentConversationId;

    /**
     * Scope of the tool approval (global, server, or conversation).
     */
    @TableField("scope")
    private ToolWhitelistScope scope;

    /**
     * Optional expiration timestamp for the tool approval.
     * If null, the approval is permanent.
     */
    @TableField("expires_at")
    private OffsetDateTime expiresAt;

    /**
     * ID of the member who created this whitelist entry.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this whitelist entry.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this whitelist entry was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this whitelist entry was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}