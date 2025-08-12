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
 * Represents a tool available in the system.
 * <p>
 * Tools are capabilities that can be used by the AI agent to perform various tasks.
 * Tools can be provided by MCP servers or be built-in to the agent. Each tool has
 * a name, description, and can be enabled or disabled. Tools are associated with
 * MCP servers when they come from external sources.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tool")
public class Tool {
    /**
     * Unique identifier for the tool, auto-generated.
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * ID of the MCP server that provides this tool.
     * For built-in tools, this may reference a special "built-in" MCP server record.
     */
    @TableField("mcp_server_id")
    private Long mcpServerId;

    /**
     * Name of the tool as used by the agent.
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * Description of what this tool does and how to use it.
     */
    @TableField("description")
    private String description;

    /**
     * Flag indicating whether this tool is currently enabled and available for use.
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * ID of the member who created this tool record.
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * ID of the member who last updated this tool record.
     */
    @TableField("updated_by")
    private Long updatedBy;

    /**
     * Timestamp when this tool record was created.
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * Timestamp when this tool record was last updated.
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}