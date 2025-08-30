package com.quip.backend.assistant.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Represents an interrupt message received from the agent via Redis.
 * <p>
 * This message contains information about conversation interruptions detected
 * by the agent. The agent publishes these messages to Redis when it detects
 * that a conversation has been interrupted, and the backend consumes them
 * to update the conversation's interrupt status.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterruptMessage {
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
     * The ID of the member whose conversation was interrupted.
     */
    @JsonProperty("memberId")
    @NotNull(message = "Member ID cannot be null")
    private Long memberId;

    /**
     * The ID of the server where the conversation was interrupted.
     */
    @JsonProperty("serverId")
    @NotNull(message = "Server ID cannot be null")
    private Long serverId;

    /**
     * The ID of the assistant conversation that was interrupted.
     * Optional - if not provided, the active conversation will be used.
     */
    @JsonProperty("assistantConversationId")
    private Long assistantConversationId;

    /**
     * The name of the tool that was interrupted, if applicable.
     * Optional - used for more detailed interrupt tracking.
     */
    @JsonProperty("interruptedToolName")
    private String interruptedToolName;

    /**
     * Source of this message, typically "agent".
     * Used for message routing and validation.
     */
    @JsonProperty("source")
    @NotNull(message = "Source cannot be null")
    @NotBlank(message = "Source cannot be blank")
    private String source;

    /**
     * Additional context about the interruption.
     * Optional - used for debugging and logging.
     */
    @JsonProperty("reason")
    private String reason;

    /**
     * Validates that the message contains required fields.
     * 
     * @return true if the message is valid, false otherwise
     */
    public boolean isValid() {
        return messageId != null && !messageId.trim().isEmpty() &&
               timestamp != null &&
               memberId != null &&
               serverId != null &&
               source != null && !source.trim().isEmpty();
    }
}