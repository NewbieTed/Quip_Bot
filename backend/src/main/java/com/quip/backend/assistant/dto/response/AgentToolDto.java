package com.quip.backend.assistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a tool available on the agent side.
 * <p>
 * This DTO is used when communicating with the agent to discover
 * what tools are currently available and their metadata.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolDto {
    /**
     * The name of the tool as known by the agent.
     */
    private String name;

    /**
     * Description of what the tool does.
     */
    private String description;

    /**
     * The MCP server that provides this tool (if applicable).
     */
    private String mcpServer;

    /**
     * Whether the tool is currently enabled on the agent.
     */
    private Boolean enabled;
}