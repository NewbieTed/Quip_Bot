package com.quip.backend.tool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for creating a new MCP server.
 * <p>
 * This DTO contains all the information needed to create a new MCP server,
 * including the server name, URL, description, and metadata about the request context
 * for authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMcpServerRequestDto {
    /**
     * ID of the channel where this MCP server is being created.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the member creating this MCP server.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * ID of the Discord server this MCP server is associated with.
     * If null, the MCP server will be available globally.
     */
    @PositiveOrZero(message = "Discord Server ID must be a non negative number")
    private Long discordServerId;

    /**
     * Name of the MCP server.
     * Must not be blank.
     */
    @NotNull(message = "Server name cannot be null")
    @NotBlank(message = "Server name cannot be an empty string")
    private String serverName;

    /**
     * URL endpoint of the MCP server.
     * Must not be blank.
     */
    @NotNull(message = "Server URL cannot be null")
    @NotBlank(message = "Server URL cannot be an empty string")
    private String serverUrl;

    /**
     * Optional description of what this MCP server provides.
     */
    private String description;
}