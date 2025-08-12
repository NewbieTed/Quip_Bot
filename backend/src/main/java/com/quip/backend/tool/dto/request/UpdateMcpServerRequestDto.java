package com.quip.backend.tool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for updating an existing MCP server.
 * <p>
 * This DTO contains all the information needed to update an MCP server,
 * including the updated server name, URL, description, and metadata about the request context
 * for authorization checks.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMcpServerRequestDto {
    /**
     * ID of the channel where this update is being requested.
     * Used for authorization checks.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    /**
     * ID of the member updating this MCP server.
     * Used for authorization checks.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * Updated name of the MCP server.
     * Must not be blank.
     */
    @NotNull(message = "Server name cannot be null")
    @NotBlank(message = "Server name cannot be an empty string")
    private String serverName;

    /**
     * Updated URL endpoint of the MCP server.
     * Must not be blank.
     */
    @NotNull(message = "Server URL cannot be null")
    @NotBlank(message = "Server URL cannot be an empty string")
    private String serverUrl;

    /**
     * Updated description of what this MCP server provides.
     */
    private String description;
}