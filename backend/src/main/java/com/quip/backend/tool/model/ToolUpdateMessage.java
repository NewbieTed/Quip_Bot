package com.quip.backend.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Represents a tool update message received from the agent via Redis.
 * <p>
 * This message contains information about tools that have been added or removed
 * from the agent's available tool set. The agent publishes these messages to
 * Redis when it detects changes in tool availability, and the backend consumes
 * them to keep the tool database synchronized.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolUpdateMessage {
    /**
     * Unique identifier for this message.
     * Used for tracking and deduplication.
     */
    @JsonProperty("messageId")
    @NotNull(message = "Message ID cannot be null")
    @NotBlank(message = "Message ID cannot be blank")
    private String messageId;

    /**
     * Timestamp when this message was created by the agent.
     * Used for ordering and freshness validation.
     */
    @JsonProperty("timestamp")
    @NotNull(message = "Timestamp cannot be null")
    private OffsetDateTime timestamp;

    /**
     * List of tool names that have been added to the agent's available tools.
     * Can be empty if no tools were added.
     */
    @JsonProperty("addedTools")
    @NotNull(message = "Added tools list cannot be null")
    private List<String> addedTools;

    /**
     * List of tool names that have been removed from the agent's available tools.
     * Can be empty if no tools were removed.
     */
    @JsonProperty("removedTools")
    @NotNull(message = "Removed tools list cannot be null")
    private List<String> removedTools;

    /**
     * Source of this message, typically "agent".
     * Used for message routing and validation.
     */
    @JsonProperty("source")
    @NotNull(message = "Source cannot be null")
    @NotBlank(message = "Source cannot be blank")
    private String source;

    /**
     * Validates that the message contains at least one tool change.
     * 
     * @return true if the message has added or removed tools, false otherwise
     */
    public boolean hasChanges() {
        return (addedTools != null && !addedTools.isEmpty()) || 
               (removedTools != null && !removedTools.isEmpty());
    }

    /**
     * Validates that all tool names in the message are valid.
     * Tool names should be non-null, non-blank, and contain only alphanumeric characters,
     * hyphens, and underscores.
     * 
     * @return true if all tool names are valid, false otherwise
     */
    public boolean hasValidToolNames() {
        return isValidToolNameList(addedTools) && isValidToolNameList(removedTools);
    }

    /**
     * Helper method to validate a list of tool names.
     * 
     * @param toolNames the list of tool names to validate
     * @return true if all tool names in the list are valid, false otherwise
     */
    private boolean isValidToolNameList(List<String> toolNames) {
        if (toolNames == null) {
            return true; // null lists are considered valid (empty)
        }
        
        return toolNames.stream()
                .allMatch(this::isValidToolName);
    }

    /**
     * Validates a single tool name.
     * Tool names must be non-null, non-blank, and contain only alphanumeric characters,
     * hyphens, and underscores.
     * 
     * @param toolName the tool name to validate
     * @return true if the tool name is valid, false otherwise
     */
    private boolean isValidToolName(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return false;
        }
        
        // Tool names should contain only alphanumeric characters, hyphens, and underscores
        return toolName.matches("^[a-zA-Z0-9_-]+$");
    }
}