package com.quip.backend.assistant.handler;

import com.quip.backend.assistant.model.InterruptMessage;
import com.quip.backend.assistant.service.AssistantConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler for processing interrupt messages from the agent.
 * <p>
 * This component processes interrupt messages received via Redis and updates
 * the corresponding assistant conversation's interrupt status. It handles
 * validation, error cases, and provides detailed logging for monitoring.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterruptMessageHandler {

    private final AssistantConversationService assistantConversationService;

    /**
     * Handles an interrupt message by updating the conversation's interrupt status.
     * <p>
     * This method validates the message, finds the appropriate conversation,
     * and marks it as interrupted. It handles various error cases gracefully
     * and provides detailed logging for monitoring and debugging.
     * </p>
     *
     * @param message the interrupt message to process
     * @return true if the message was processed successfully, false otherwise
     */
    public boolean handleInterrupt(InterruptMessage message) {
        if (message == null) {
            log.error("Received null interrupt message");
            return false;
        }

        // Validate message
        if (!message.isValid()) {
            log.error("Invalid interrupt message received - messageId={}, memberId={}, serverId={}, source={}", 
                    message.getMessageId(), message.getMemberId(), message.getServerId(), message.getSource());
            return false;
        }

        Long memberId = message.getMemberId();
        Long serverId = message.getServerId();
        String messageId = message.getMessageId();

        log.info("Processing interrupt message - messageId={}, memberId={}, serverId={}, toolName={}, reason={}", 
                messageId, memberId, serverId, message.getInterruptedToolName(), message.getReason());

        try {
            // Mark the conversation as interrupted
            // Use memberId as updatedBy since this is an agent-initiated action on behalf of the member
            boolean success = assistantConversationService.markAsInterrupted(memberId, serverId, memberId);

            if (success) {
                log.info("Successfully marked conversation as interrupted - messageId={}, memberId={}, serverId={}", 
                        messageId, memberId, serverId);
                return true;
            } else {
                log.warn("Failed to mark conversation as interrupted - no active conversation found - messageId={}, memberId={}, serverId={}", 
                        messageId, memberId, serverId);
                return false;
            }

        } catch (Exception e) {
            log.error("Error processing interrupt message - messageId={}, memberId={}, serverId={}", 
                    messageId, memberId, serverId, e);
            return false;
        }
    }
}