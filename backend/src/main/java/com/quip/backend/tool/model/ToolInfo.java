package com.quip.backend.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents tool information with MCP server details.
 * Used in tool update messages to identify which MCP server provides each tool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfo {
    /**
     * Name of the tool.
     */
    @JsonProperty("name")
    @NotNull(message = "Tool name cannot be null")
    @NotBlank(message = "Tool name cannot be blank")
    private String name;

    /**
     * Name of the MCP server that provides this tool.
     * For built-in tools, this will be "built-in".
     */
    @JsonProperty("mcpServerName")
    @NotNull(message = "MCP server name cannot be null")
    @NotBlank(message = "MCP server name cannot be blank")
    private String mcpServerName;

    /**
     * Validates that the tool name is valid.
     * Tool names should contain only alphanumeric characters, hyphens, and underscores.
     * 
     * @return true if the tool name is valid, false otherwise
     */
    public boolean hasValidToolName() {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // Tool names should contain only alphanumeric characters, hyphens, and underscores
        return name.matches("^[a-zA-Z0-9_-]+$");
    }
}