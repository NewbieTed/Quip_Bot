package com.quip.backend.tool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for registering a new tool.
 * <p>
 * This DTO contains all the information needed to register a new tool,
 * including the tool name, description, MCP server association, and metadata
 * about the request context for authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterToolRequestDto {
    /**
     * ID of the channel where this tool is being registered.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the member registering this tool.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * ID of the MCP server that provides this tool.
     */
    @NotNull(message = "MCP Server ID cannot be null")
    @PositiveOrZero(message = "MCP Server ID must be a non negative number")
    private Long mcpServerId;

    /**
     * Name of the tool as used by the agent.
     * Must not be blank.
     */
    @NotNull(message = "Tool name cannot be null")
    @NotBlank(message = "Tool name cannot be an empty string")
    private String toolName;

    /**
     * Description of what this tool does and how to use it.
     */
    private String description;

    /**
     * Flag indicating whether this tool should be enabled upon registration.
     * Defaults to false for security reasons.
     */
    private Boolean enabled;
}