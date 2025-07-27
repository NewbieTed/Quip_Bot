package com.quip.backend.tool.dto.request;

import com.quip.backend.tool.enums.ToolWhitelistScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object for updating tool whitelist permissions.
 * <p>
 * This DTO contains all the information needed to add or update a tool whitelist entry,
 * including the tool ID, scope, expiration, and metadata about the request context
 * for authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateToolWhitelistRequestDto {
    /**
     * ID of the channel where this whitelist update is being requested.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the member requesting this whitelist update.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * ID of the member who will have access to the tool.
     */
    @NotNull(message = "Target Member ID cannot be null")
    @PositiveOrZero(message = "Target Member ID must be a non negative number")
    private Long targetMemberId;

    /**
     * ID of the tool to be whitelisted.
     */
    @NotNull(message = "Tool ID cannot be null")
    @PositiveOrZero(message = "Tool ID must be a non negative number")
    private Long toolId;

    /**
     * ID of the agent conversation for conversation-scoped approvals.
     * Required only for conversation scope, defaults to 0 for other scopes.
     */
    private Long agentConversationId;

    /**
     * Scope of the tool approval (global, server, or conversation).
     */
    @NotNull(message = "Scope cannot be null")
    private ToolWhitelistScope scope;

    /**
     * Optional expiration timestamp for the tool approval.
     * If null, the approval is permanent.
     */
    private OffsetDateTime expiresAt;
}