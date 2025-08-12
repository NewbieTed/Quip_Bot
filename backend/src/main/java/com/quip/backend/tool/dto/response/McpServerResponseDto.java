package com.quip.backend.tool.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object for returning MCP server information.
 * <p>
 * This DTO contains the essential information about an MCP server
 * to be returned to the client, including its ID, name, URL, description,
 * and associated Discord server.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerResponseDto {
    /**
     * Unique identifier of the MCP server.
     */
    private Long id;

    /**
     * ID of the Discord server this MCP server is associated with.
     * Null if the MCP server is available globally.
     */
    private Long discordServerId;

    /**
     * Display name of the MCP server.
     */
    private String serverName;

    /**
     * URL endpoint of the MCP server.
     */
    private String serverUrl;

    /**
     * Description of what this MCP server provides.
     */
    private String description;

    /**
     * Timestamp when this MCP server record was created.
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this MCP server record was last updated.
     */
    private OffsetDateTime updatedAt;
}