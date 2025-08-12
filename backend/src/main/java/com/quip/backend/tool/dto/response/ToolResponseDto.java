package com.quip.backend.tool.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object for returning tool information.
 * <p>
 * This DTO contains the essential information about a tool
 * to be returned to the client, including its ID, name, description,
 * enabled status, and associated MCP server.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolResponseDto {
    /**
     * Unique identifier of the tool.
     */
    private Long id;

    /**
     * ID of the MCP server that provides this tool.
     */
    private Long mcpServerId;

    /**
     * Name of the MCP server that provides this tool.
     */
    private String mcpServerName;

    /**
     * Name of the tool as used by the agent.
     */
    private String toolName;

    /**
     * Description of what this tool does and how to use it.
     */
    private String description;

    /**
     * Flag indicating whether this tool is currently enabled and available for use.
     */
    private Boolean enabled;

    /**
     * Timestamp when this tool record was created.
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this tool record was last updated.
     */
    private OffsetDateTime updatedAt;
}