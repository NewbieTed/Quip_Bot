package com.quip.backend.tool.handler;

import com.quip.backend.tool.model.ToolUpdateMessage;
import com.quip.backend.tool.service.ToolService;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Handler for processing tool update messages from the agent.
 * <p>
 * This component validates and routes tool update messages received from Redis.
 * It processes added and removed tools separately, ensuring proper validation
 * and error handling throughout the process.
 * </p>
 * <p>
 * The handler performs the following operations:
 * - Message validation (format, required fields, tool names)
 * - Timestamp freshness validation
 * - Separate processing of added and removed tools
 * - Comprehensive logging of all operations
 * - Error handling with appropriate logging levels
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolUpdateMessageHandler {

    private final ToolService toolService;
    private final ToolSyncMetricsService metricsService;

    // Maximum age for messages to be considered fresh (in minutes)
    private static final long MAX_MESSAGE_AGE_MINUTES = 30;

    /**
     * Validates and processes a tool update message.
     * <p>
     * This is the main entry point for processing tool update messages.
     * It performs comprehensive validation and routes the message for processing
     * if valid, or logs appropriate warnings/errors if invalid.
     * </p>
     *
     * @param message the tool update message to process
     * @return true if the message was processed successfully, false otherwise
     */
    public boolean handleToolUpdates(ToolUpdateMessage message) {
        if (message == null) {
            log.error("Received null tool update message");
            return false;
        }

        long startTime = System.currentTimeMillis();
        String messageId = message.getMessageId();

        log.info("Starting to handle tool update message: messageId={}, source={}, timestamp={}, addedTools={}, removedTools={}", 
                messageId, message.getSource(), message.getTimestamp(), 
                message.getAddedTools().size(), message.getRemovedTools().size());

        // Validate the message
        long validationStartTime = System.currentTimeMillis();
        if (!validateMessage(message)) {
            long validationTime = System.currentTimeMillis() - validationStartTime;
            
            // Record validation error metrics
            metricsService.recordError("validation");
            
            log.warn("Tool update message validation failed, skipping processing: messageId={}, validationTime={}ms", 
                    messageId, validationTime);
            return false;
        }
        long validationTime = System.currentTimeMillis() - validationStartTime;
        log.debug("Message validation completed: messageId={}, validationTime={}ms", messageId, validationTime);

        // Process the message
        try {
            long processingStartTime = System.currentTimeMillis();
            boolean success = processValidatedMessage(message);
            long processingTime = System.currentTimeMillis() - processingStartTime;
            long totalTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                log.info("Successfully processed tool update message: messageId={}, addedTools={}, removedTools={}, " +
                        "validationTime={}ms, processingTime={}ms, totalTime={}ms", 
                        messageId, message.getAddedTools().size(), message.getRemovedTools().size(),
                        validationTime, processingTime, totalTime);
            } else {
                log.error("Failed to process tool update message: messageId={}, processingTime={}ms, totalTime={}ms", 
                        messageId, processingTime, totalTime);
            }
            
            return success;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error processing tool update message: messageId={}, totalTime={}ms", 
                    messageId, totalTime, e);
            return false;
        }
    }

    /**
     * Validates a tool update message for completeness and correctness.
     * <p>
     * Performs comprehensive validation including:
     * - Required field presence and format
     * - Tool name validation
     * - Message freshness validation
     * - Change detection (ensures message has actual changes)
     * </p>
     *
     * @param message the message to validate
     * @return true if the message is valid, false otherwise
     */
    public boolean validateMessage(ToolUpdateMessage message) {
        if (message == null) {
            log.error("Message validation failed: message is null");
            return false;
        }

        // Validate required fields
        if (!validateRequiredFields(message)) {
            return false;
        }

        // Validate tool names
        if (!validateToolNames(message)) {
            return false;
        }

        // Validate message freshness
        if (!validateMessageFreshness(message)) {
            return false;
        }

        // Validate that message has changes
        if (!validateHasChanges(message)) {
            return false;
        }

        log.debug("Message validation passed: messageId={}", message.getMessageId());
        return true;
    }

    /**
     * Validates that all required fields are present and properly formatted.
     *
     * @param message the message to validate
     * @return true if all required fields are valid, false otherwise
     */
    private boolean validateRequiredFields(ToolUpdateMessage message) {
        // Validate messageId
        if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
            log.warn("Message validation failed: messageId is null or empty");
            return false;
        }

        // Validate timestamp
        if (message.getTimestamp() == null) {
            log.warn("Message validation failed: timestamp is null, messageId={}", message.getMessageId());
            return false;
        }

        // Validate source
        if (message.getSource() == null || message.getSource().trim().isEmpty()) {
            log.warn("Message validation failed: source is null or empty, messageId={}", message.getMessageId());
            return false;
        }

        // Validate tool lists are not null
        if (message.getAddedTools() == null) {
            log.warn("Message validation failed: addedTools list is null, messageId={}", message.getMessageId());
            return false;
        }

        if (message.getRemovedTools() == null) {
            log.warn("Message validation failed: removedTools list is null, messageId={}", message.getMessageId());
            return false;
        }

        return true;
    }

    /**
     * Validates that all tool names in the message are properly formatted.
     *
     * @param message the message to validate
     * @return true if all tool names are valid, false otherwise
     */
    private boolean validateToolNames(ToolUpdateMessage message) {
        if (!message.hasValidToolNames()) {
            log.warn("Message validation failed: invalid tool names detected, messageId={}", message.getMessageId());
            
            // Log specific invalid tool names for debugging
            logInvalidToolNames(message.getAddedTools(), "added", message.getMessageId());
            logInvalidToolNames(message.getRemovedTools(), "removed", message.getMessageId());
            
            return false;
        }

        return true;
    }

    /**
     * Validates that the message timestamp is recent enough to be processed.
     *
     * @param message the message to validate
     * @return true if the message is fresh enough, false otherwise
     */
    private boolean validateMessageFreshness(ToolUpdateMessage message) {
        OffsetDateTime now = OffsetDateTime.now();
        long messageAgeMinutes = ChronoUnit.MINUTES.between(message.getTimestamp(), now);

        if (messageAgeMinutes > MAX_MESSAGE_AGE_MINUTES) {
            log.warn("Message validation failed: message is too old ({} minutes), messageId={}", 
                    messageAgeMinutes, message.getMessageId());
            return false;
        }

        if (messageAgeMinutes < 0) {
            log.warn("Message validation failed: message timestamp is in the future, messageId={}", 
                    message.getMessageId());
            return false;
        }

        return true;
    }

    /**
     * Validates that the message contains actual changes.
     *
     * @param message the message to validate
     * @return true if the message has changes, false otherwise
     */
    private boolean validateHasChanges(ToolUpdateMessage message) {
        if (!message.hasChanges()) {
            log.warn("Message validation failed: no tool changes detected, messageId={}", message.getMessageId());
            return false;
        }

        return true;
    }

    /**
     * Logs invalid tool names for debugging purposes.
     *
     * @param toolNames the list of tool names to check
     * @param type the type of tools (added/removed) for logging
     * @param messageId the message ID for context
     */
    private void logInvalidToolNames(List<String> toolNames, String type, String messageId) {
        if (toolNames == null) {
            return;
        }

        for (String toolName : toolNames) {
            if (toolName == null || toolName.trim().isEmpty()) {
                log.warn("Invalid {} tool name: null or empty, messageId={}", type, messageId);
            } else if (!toolName.matches("^[a-zA-Z0-9_-]+$")) {
                log.warn("Invalid {} tool name: '{}' (contains invalid characters), messageId={}", 
                        type, toolName, messageId);
            }
        }
    }

    /**
     * Processes a validated tool update message.
     * <p>
     * This method handles the actual processing of tool updates after validation
     * has passed. It processes added and removed tools separately to ensure
     * proper error handling and logging.
     * </p>
     *
     * @param message the validated message to process
     * @return true if processing was successful, false otherwise
     */
    private boolean processValidatedMessage(ToolUpdateMessage message) {
        boolean addedSuccess = true;
        boolean removedSuccess = true;

        // Process added tools
        if (!message.getAddedTools().isEmpty()) {
            log.info("Processing {} added tools: {}", 
                    message.getAddedTools().size(), message.getAddedTools());
            addedSuccess = processAddedTools(message.getAddedTools(), message.getMessageId());
        }

        // Process removed tools
        if (!message.getRemovedTools().isEmpty()) {
            log.info("Processing {} removed tools: {}", 
                    message.getRemovedTools().size(), message.getRemovedTools());
            removedSuccess = processRemovedTools(message.getRemovedTools(), message.getMessageId());
        }

        return addedSuccess && removedSuccess;
    }

    /**
     * Processes a list of added tools.
     * <p>
     * This method calls the ToolService to create or update tools in the database
     * based on agent discovery. Each tool is processed individually to ensure
     * proper error handling and logging.
     * </p>
     *
     * @param addedTools the list of tool names that were added
     * @param messageId the message ID for logging context
     * @return true if all tools were processed successfully, false otherwise
     */
    private boolean processAddedTools(List<String> addedTools, String messageId) {
        boolean allSuccess = true;
        long startTime = System.currentTimeMillis();

        for (String toolName : addedTools) {
            long toolStartTime = System.currentTimeMillis();
            try {
                log.info("Processing added tool: '{}', messageId={}", toolName, messageId);
                
                // Call ToolService to create or update the tool
                toolService.createOrUpdateToolFromAgent(toolName);
                
                long toolProcessingTime = System.currentTimeMillis() - toolStartTime;
                log.info("Successfully processed added tool: '{}', messageId={}, processingTime={}ms", 
                        toolName, messageId, toolProcessingTime);
                
            } catch (Exception e) {
                long toolProcessingTime = System.currentTimeMillis() - toolStartTime;
                log.error("Failed to process added tool: '{}', messageId={}, processingTime={}ms", 
                        toolName, messageId, toolProcessingTime, e);
                allSuccess = false;
            }
        }

        long totalProcessingTime = System.currentTimeMillis() - startTime;
        
        if (allSuccess) {
            log.info("Successfully processed all {} added tools, messageId={}, totalProcessingTime={}ms", 
                    addedTools.size(), messageId, totalProcessingTime);
        } else {
            log.error("Failed to process some added tools, messageId={}, totalProcessingTime={}ms", 
                    messageId, totalProcessingTime);
        }

        return allSuccess;
    }

    /**
     * Processes a list of removed tools.
     * <p>
     * This method calls the ToolService to disable tools in the database
     * based on agent discovery. Each tool is processed individually to ensure
     * proper error handling and logging.
     * </p>
     *
     * @param removedTools the list of tool names that were removed
     * @param messageId the message ID for logging context
     * @return true if all tools were processed successfully, false otherwise
     */
    private boolean processRemovedTools(List<String> removedTools, String messageId) {
        boolean allSuccess = true;
        long startTime = System.currentTimeMillis();

        for (String toolName : removedTools) {
            long toolStartTime = System.currentTimeMillis();
            try {
                log.info("Processing removed tool: '{}', messageId={}", toolName, messageId);
                
                // Call ToolService to disable the tool
                toolService.disableToolFromAgent(toolName);
                
                long toolProcessingTime = System.currentTimeMillis() - toolStartTime;
                log.info("Successfully processed removed tool: '{}', messageId={}, processingTime={}ms", 
                        toolName, messageId, toolProcessingTime);
                
            } catch (Exception e) {
                long toolProcessingTime = System.currentTimeMillis() - toolStartTime;
                log.error("Failed to process removed tool: '{}', messageId={}, processingTime={}ms", 
                        toolName, messageId, toolProcessingTime, e);
                allSuccess = false;
            }
        }

        long totalProcessingTime = System.currentTimeMillis() - startTime;
        
        if (allSuccess) {
            log.info("Successfully processed all {} removed tools, messageId={}, totalProcessingTime={}ms", 
                    removedTools.size(), messageId, totalProcessingTime);
        } else {
            log.error("Failed to process some removed tools, messageId={}, totalProcessingTime={}ms", 
                    messageId, totalProcessingTime);
        }

        return allSuccess;
    }
}