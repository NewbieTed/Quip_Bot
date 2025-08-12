package com.quip.backend.tool.dto.request;

import com.quip.backend.tool.enums.ToolWhitelistScope;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToolWhitelistRequestDto {

    /**
     * Name of the tool to be whitelisted
     * Used for authorization checks as well as whitelist update.
     */
    @NotNull(message = "Tool name cannot be null")
    @NotEmpty(message = "Tool name cannot be empty")
    private String toolName;

    /**
     * Scope of the tool whitelist entry (global, server, or conversation).
     */
    @NotNull(message = "Scope cannot be null")
    private ToolWhitelistScope scope;

    /**
     * Optional conversation ID for conversation-scoped whitelist entries.
     * Required when scope is CONVERSATION, ignored otherwise.
     */
    private Long agentConversationId;

    /**
     * Optional expiration timestamp for the tool approval.
     * If null, the approval is permanent.
     */
    private OffsetDateTime expiresAt;
}
