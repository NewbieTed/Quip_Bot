package com.quip.backend.assistant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the response format from the AI agent.
 * <p>
 * The agent returns JSON responses with a content field and optional
 * metadata fields like tool_name and type for different response types.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponseDto {
    
    /**
     * The main content of the response from the agent.
     */
    @JsonProperty("content")
    private String content;
    
    /**
     * Optional tool name when the response is related to tool execution.
     */
    @JsonProperty("tool_name")
    private String toolName;
    
    /**
     * Optional type field to indicate special response types (e.g., "interrupt").
     */
    @JsonProperty("type")
    private String type;
}