package com.quip.backend.tool.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents an MCP (Model Context Protocol) server in the system.
 * <p>
 * MCP servers provide tools and capabilities that can be used by the AI agent.
 * Each MCP server has a unique URL and can be associated with a specific Discord server
 * or be available globally. MCP servers expose tools that can be discovered and used
 * by the agent system.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mcp_server")
public class McpServer {
    /**
     * Unique identifier for the MCP server, auto-generated.
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * ID of the Discord server this MCP server is associated with.
     * If null, the MCP server is available globally.
     */
    @TableField("discord_server_id")
    private Long discordServerId;

    /**
     * Display name of the MCP server.
     */
    @TableField("server_name")
    private String serverName;

    /**
     * URL endpoint of the MCP server.
     */
    @TableField("server_url")
    private String serverUrl;

    /**
     * Optional description of what this MCP server provides.
     */
    @TableField("description")
    private String description;

    /**
     * ID of the member who created this MCP server record.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this MCP server record.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this MCP server record was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this MCP server record was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}