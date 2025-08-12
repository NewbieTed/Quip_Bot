package com.quip.backend.tool.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.redis.service.RedisService;
import com.quip.backend.tool.handler.ToolUpdateMessageHandler;
import com.quip.backend.tool.model.ToolUpdateMessage;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import com.quip.backend.tool.sync.SyncRecoveryManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis message consumer for tool update messages from the agent.
 * <p>
 * This component listens for tool update messages on the "tools:updates" Redis list
 * and processes them to keep the backend's tool database synchronized with the agent's
 * available tools. The consumer runs in a separate thread and continuously polls
 * Redis for new messages.
 * </p>
 * <p>
 * Message processing includes:
 * - JSON deserialization and validation
 * - Error handling for malformed messages
 * - Logging of processing events
 * - Graceful shutdown handling
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolUpdateConsumer {

    private static final String TOOLS_UPDATES_KEY = "tools:updates";
    private static final long POLLING_TIMEOUT_SECONDS = 5L;
    private static final int CONSUMER_THREAD_POOL_SIZE = 1;

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final ToolUpdateMessageHandler messageHandler;
    private final ToolSyncMetricsService metricsService;
    private final SyncRecoveryManager syncRecoveryManager;

    @Value("${app.tool-sync.consumer.enabled:true}")
    private boolean consumerEnabled;

    @Value("${app.tool-sync.consumer.polling-timeout:5}")
    private long pollingTimeoutSeconds;

    @Value("${app.tool-sync.recovery.failure-threshold:5}")
    private int consecutiveFailureThreshold;

    @Value("${app.tool-sync.recovery.validation-failure-threshold:10}")
    private int validationFailureThreshold;

    @Value("${app.tool-sync.recovery.validation-failure-window-minutes:1}")
    private int validationFailureWindowMinutes;

    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean processingPaused = new AtomicBoolean(false);
    private CompletableFuture<Void> consumerTask;
    
    // Failure tracking for sync recovery triggers
    private final AtomicInteger consecutiveProcessingFailures = new AtomicInteger(0);
    private final AtomicInteger consecutiveDeserializationFailures = new AtomicInteger(0);
    private final AtomicInteger validationFailuresInWindow = new AtomicInteger(0);
    private final AtomicLong lastValidationFailureWindowReset = new AtomicLong(System.currentTimeMillis());

    /**
     * Initializes and starts the Redis message consumer.
     * Creates a dedicated thread pool for message consumption and begins polling.
     */
    @PostConstruct
    public void startConsumer() {
        if (!consumerEnabled) {
            log.info("Tool update consumer is disabled via configuration (app.tool-sync.consumer.enabled=false)");
            return;
        }

        log.info("Starting tool update consumer with configuration: redisKey={}, pollingTimeout={}s, threadPoolSize={}", 
                TOOLS_UPDATES_KEY, pollingTimeoutSeconds, CONSUMER_THREAD_POOL_SIZE);
        
        try {
            executorService = Executors.newFixedThreadPool(CONSUMER_THREAD_POOL_SIZE, 
                r -> {
                    Thread t = new Thread(r, "tool-update-consumer");
                    t.setDaemon(true);
                    return t;
                });

            running.set(true);
            consumerTask = CompletableFuture.runAsync(this::consumeMessages, executorService);
            
            log.info("Tool update consumer started successfully and is now listening for messages");
            
        } catch (Exception e) {
            log.error("Failed to start tool update consumer", e);
            
            // Clean up resources if startup failed
            running.set(false);
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
            
            throw new RuntimeException("Failed to start tool update consumer", e);
        }
    }

    /**
     * Stops the Redis message consumer and cleans up resources.
     * Ensures graceful shutdown of the polling thread and executor service.
     */
    @PreDestroy
    public void stopConsumer() {
        if (!consumerEnabled) {
            log.debug("Tool update consumer was not enabled, no shutdown needed");
            return;
        }
        
        if (!running.get()) {
            log.debug("Tool update consumer is not running, no shutdown needed");
            return;
        }

        log.info("Stopping tool update consumer...");
        long shutdownStartTime = System.currentTimeMillis();
        
        // Signal the consumer to stop
        running.set(false);
        
        // Cancel the consumer task
        if (consumerTask != null) {
            boolean cancelled = consumerTask.cancel(true);
            log.debug("Consumer task cancellation result: {}", cancelled);
        }
        
        // Shutdown the executor service
        if (executorService != null) {
            executorService.shutdown();
            try {
                boolean terminated = executorService.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
                if (!terminated) {
                    log.warn("Tool update consumer did not terminate gracefully within 10 seconds, forcing shutdown");
                    executorService.shutdownNow();
                    
                    // Wait a bit more for forced shutdown
                    terminated = executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (!terminated) {
                        log.error("Tool update consumer could not be terminated even after forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for tool update consumer to terminate, forcing shutdown");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        long shutdownTime = System.currentTimeMillis() - shutdownStartTime;
        log.info("Tool update consumer stopped successfully in {}ms", shutdownTime);
    }

    /**
     * Main message consumption loop.
     * Continuously polls Redis for new messages and processes them.
     * Handles errors gracefully and continues processing with exponential backoff.
     */
    private void consumeMessages() {
        log.info("Tool update consumer thread started, polling Redis key: {}", TOOLS_UPDATES_KEY);
        
        int consecutiveErrors = 0;
        final int maxConsecutiveErrors = 10;
        final long baseRetryDelayMs = 1000;
        final long maxRetryDelayMs = 30000;
        long messagesProcessed = 0;
        long totalProcessingTime = 0;
        
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            long loopStartTime = System.currentTimeMillis();
            
            try {
                // Poll for messages from the Redis list (blocking pop with timeout)
                String messageJson = redisService.rpop(TOOLS_UPDATES_KEY, String.class);
                
                if (messageJson != null) {
                    // Check if processing is paused for sync recovery
                    if (processingPaused.get()) {
                        log.debug("Message processing is paused for sync recovery - skipping message");
                        
                        // Put the message back at the front of the queue for later processing
                        redisService.lpush(TOOLS_UPDATES_KEY, messageJson);
                        
                        // Wait a bit before checking again
                        Thread.sleep(1000);
                        continue;
                    }
                    
                    log.debug("Received tool update message from Redis: {}", messageJson);
                    
                    long messageProcessingStart = System.currentTimeMillis();
                    boolean processingSuccess = processMessage(messageJson);
                    long messageProcessingTime = System.currentTimeMillis() - messageProcessingStart;
                    
                    messagesProcessed++;
                    totalProcessingTime += messageProcessingTime;
                    
                    if (processingSuccess) {
                        // Reset failure counters on successful processing
                        consecutiveProcessingFailures.set(0);
                        consecutiveDeserializationFailures.set(0);
                        
                        // Log processing metrics (Requirement 4.2)
                        log.info("Message processed successfully - messageCount={}, avgProcessingTime={}ms, lastProcessingTime={}ms", 
                                messagesProcessed, 
                                messagesProcessed > 0 ? totalProcessingTime / messagesProcessed : 0,
                                messageProcessingTime);
                    } else {
                        // Increment failure counter and check for sync recovery trigger
                        int failures = consecutiveProcessingFailures.incrementAndGet();
                        log.warn("Message processing failed - consecutiveFailures={}", failures);
                        
                        // Check if we should trigger sync recovery
                        if (shouldTriggerSyncRecovery("consecutive_processing_failures")) {
                            triggerSyncRecovery("consecutive_processing_failures");
                        }
                    }
                    
                    // Reset error counter on successful message processing
                    if (consecutiveErrors > 0) {
                        // Log Redis connection status change (Requirement 4.3)
                        log.info("Redis connection recovered after {} consecutive errors", consecutiveErrors);
                        consecutiveErrors = 0;
                    }
                } else {
                    // No message received within timeout, continue polling
                    // Only log every 60 seconds to reduce noise
                    if (messagesProcessed % 12 == 0) { // 12 * 5 seconds = 60 seconds
                        log.trace("No messages received within {} seconds, continuing to poll", pollingTimeoutSeconds);
                    }
                    
                    // Reset error counter on successful polling (even if no message)
                    if (consecutiveErrors > 0) {
                        log.debug("Redis polling successful, resetting error counter from {}", consecutiveErrors);
                        consecutiveErrors = 0;
                    }
                }
                
            } catch (Exception e) {
                consecutiveErrors++;
                long loopTime = System.currentTimeMillis() - loopStartTime;
                
                // Error logging with context information (Requirement 4.3)
                if (consecutiveErrors <= maxConsecutiveErrors) {
                    log.error("Error occurred while consuming tool update messages - attempt={}/{}, loopTime={}ms, totalMessages={}", 
                            consecutiveErrors, maxConsecutiveErrors, loopTime, messagesProcessed, e);
                } else {
                    log.error("Critical: Too many consecutive errors ({}) while consuming tool update messages. " +
                            "Redis may be unavailable. Will continue retrying... totalMessages={}, avgProcessingTime={}ms", 
                            consecutiveErrors, messagesProcessed, 
                            messagesProcessed > 0 ? totalProcessingTime / messagesProcessed : 0, e);
                }
                
                // Calculate exponential backoff delay
                long retryDelay = Math.min(baseRetryDelayMs * (1L << Math.min(consecutiveErrors - 1, 5)), maxRetryDelayMs);
                
                log.debug("Waiting {}ms before retrying Redis connection (consecutive errors: {})", 
                        retryDelay, consecutiveErrors);
                
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    log.info("Tool update consumer thread interrupted during error recovery");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // Log final statistics
        log.info("Tool update consumer thread stopped - running={}, interrupted={}, totalMessages={}, avgProcessingTime={}ms", 
                running.get(), Thread.currentThread().isInterrupted(), messagesProcessed,
                messagesProcessed > 0 ? totalProcessingTime / messagesProcessed : 0);
    }

    /**
     * Processes a single tool update message.
     * Deserializes the JSON message and delegates processing to the message handler.
     * 
     * @param messageJson the JSON message string from Redis
     * @return true if the message was processed successfully, false otherwise
     */
    private boolean processMessage(String messageJson) {
        long startTime = System.currentTimeMillis();
        String messageId = "unknown";
        
        try {
            // Deserialize the JSON message
            ToolUpdateMessage message = objectMapper.readValue(messageJson, ToolUpdateMessage.class);
            messageId = message.getMessageId();
            
            // Structured logging for tool update events (Requirement 4.2)
            log.info("Processing tool update message - messageId={}, timestamp={}, source={}, addedTools={}, removedTools={}, addedCount={}, removedCount={}", 
                    message.getMessageId(), 
                    message.getTimestamp(),
                    message.getSource(),
                    message.getAddedTools(), 
                    message.getRemovedTools(),
                    message.getAddedTools().size(),
                    message.getRemovedTools().size());
            
            // Delegate processing to the message handler
            boolean success = messageHandler.handleToolUpdates(message);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record metrics for message processing latency (Requirement 4.2)
            metricsService.recordMessageProcessed(success, processingTime);
            
            if (success) {
                // Reset deserialization failure counter on successful processing
                consecutiveDeserializationFailures.set(0);
                
                // Log processing results (Requirement 4.2)
                log.info("Successfully processed tool update message - messageId={}, processingTime={}ms, result=success", 
                        messageId, processingTime);
                
                return true;
            } else {
                // Error logging with context information (Requirement 4.3)
                log.error("Failed to process tool update message - messageId={}, processingTime={}ms, result=failure", 
                        messageId, processingTime);
                
                // Check if this was a critical database failure
                // This would be determined by the message handler's response
                // For now, we'll treat handler failures as potential critical failures
                if (shouldTriggerSyncRecovery("critical_database_failure")) {
                    triggerSyncRecovery("critical_database_failure");
                }
                
                return false;
            }
            
        } catch (JsonProcessingException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record deserialization error metrics
            metricsService.recordError("deserialization");
            metricsService.recordMessageProcessed(false, processingTime);
            
            // Error logging with context information (Requirement 4.3)
            log.error("Failed to deserialize tool update message JSON - messageId={}, processingTime={}ms, errorType=JsonProcessingException, jsonLength={}", 
                    messageId, processingTime, messageJson != null ? messageJson.length() : 0, e);
            
            return false;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record unexpected error metrics
            metricsService.recordError("unexpected");
            metricsService.recordMessageProcessed(false, processingTime);
            
            // Error logging with context information (Requirement 4.3)
            log.error("Unexpected error processing tool update message - messageId={}, processingTime={}ms, errorType={}, jsonLength={}", 
                    messageId, processingTime, e.getClass().getSimpleName(), 
                    messageJson != null ? messageJson.length() : 0, e);
            
            return false;
        }
    }



    /**
     * Returns whether the consumer is currently running.
     * 
     * @return true if the consumer is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns whether the consumer is enabled via configuration.
     * 
     * @return true if the consumer is enabled, false otherwise
     */
    public boolean isEnabled() {
        return consumerEnabled;
    }

    /**
     * Returns the Redis key being monitored for tool updates.
     * 
     * @return the Redis key name
     */
    public String getRedisKey() {
        return TOOLS_UPDATES_KEY;
    }

    /**
     * Returns the configured polling timeout in seconds.
     * 
     * @return the polling timeout
     */
    public long getPollingTimeoutSeconds() {
        return pollingTimeoutSeconds;
    }

    /**
     * Logs the current status of the consumer for monitoring purposes.
     */
    public void logStatus() {
        log.info("Tool update consumer status: enabled={}, running={}, redisKey={}, pollingTimeout={}s", 
                consumerEnabled, running.get(), TOOLS_UPDATES_KEY, pollingTimeoutSeconds);
    }
    
    /**
     * Gets comprehensive consumer metrics for monitoring.
     * 
     * @return Map containing consumer metrics
     */
    public java.util.Map<String, Object> getMetrics() {
        java.util.Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("enabled", consumerEnabled);
        metrics.put("running", running.get());
        metrics.put("redisKey", TOOLS_UPDATES_KEY);
        metrics.put("pollingTimeoutSeconds", pollingTimeoutSeconds);
        metrics.put("threadPoolSize", CONSUMER_THREAD_POOL_SIZE);
        metrics.put("executorServiceShutdown", executorService != null ? executorService.isShutdown() : true);
        metrics.put("executorServiceTerminated", executorService != null ? executorService.isTerminated() : true);
        return metrics;
    }
    
    /**
     * Logs comprehensive consumer metrics for monitoring.
     */
    public void logMetrics() {
        java.util.Map<String, Object> metrics = getMetrics();
        log.info("Tool update consumer metrics: {}", metrics);
    }
    
    /**
     * Pauses message processing temporarily for sync recovery operations.
     * <p>
     * When processing is paused, the consumer will continue polling Redis but will
     * not process any messages until processing is resumed. This ensures that
     * sync recovery can complete without interference from new messages.
     * </p>
     * <p>
     * This method is thread-safe and can be called multiple times safely.
     * </p>
     */
    public void pauseProcessing() {
        if (processingPaused.compareAndSet(false, true)) {
            log.info("Tool update message processing paused for sync recovery");
        } else {
            log.debug("Tool update message processing was already paused");
        }
    }
    
    /**
     * Resumes message processing after sync recovery operations complete.
     * <p>
     * This method re-enables message processing and resets failure counters
     * to provide a clean slate after sync recovery.
     * </p>
     * <p>
     * This method is thread-safe and can be called multiple times safely.
     * </p>
     */
    public void resumeProcessing() {
        if (processingPaused.compareAndSet(true, false)) {
            // Reset failure counters after successful recovery
            resetFailureCounters();
            
            log.info("Tool update message processing resumed after sync recovery");
        } else {
            log.debug("Tool update message processing was not paused");
        }
    }
    
    /**
     * Checks if message processing is currently paused.
     * 
     * @return true if processing is paused, false otherwise
     */
    public boolean isProcessingPaused() {
        return processingPaused.get();
    }
    
    /**
     * Resets all failure counters used for sync recovery triggers.
     * <p>
     * This method is called after successful sync recovery to provide
     * a clean slate for failure detection.
     * </p>
     */
    private void resetFailureCounters() {
        consecutiveProcessingFailures.set(0);
        consecutiveDeserializationFailures.set(0);
        validationFailuresInWindow.set(0);
        lastValidationFailureWindowReset.set(System.currentTimeMillis());
        
        log.debug("Reset all failure counters for sync recovery");
    }
    
    /**
     * Checks if sync recovery should be triggered based on current failure patterns.
     * <p>
     * Sync recovery is triggered when:
     * - Critical database failures occur during tool processing
     * - Message deserialization fails repeatedly (>5 consecutive failures)
     * - Tool validation failures exceed threshold (>10 invalid tools in 1 minute)
     * </p>
     * 
     * @param failureType the type of failure that occurred
     * @return true if sync recovery should be triggered, false otherwise
     */
    private boolean shouldTriggerSyncRecovery(String failureType) {
        switch (failureType.toLowerCase()) {
            case "critical_database_failure":
                log.warn("Critical database failure detected - triggering sync recovery immediately");
                return true;
                
            case "consecutive_deserialization_failures":
                int deserializationFailures = consecutiveDeserializationFailures.get();
                if (deserializationFailures >= consecutiveFailureThreshold) {
                    log.warn("Consecutive deserialization failures threshold reached: {} >= {} - triggering sync recovery", 
                            deserializationFailures, consecutiveFailureThreshold);
                    return true;
                }
                break;
                
            case "validation_failures_threshold":
                // Check if we need to reset the validation failure window
                long currentTime = System.currentTimeMillis();
                long lastReset = lastValidationFailureWindowReset.get();
                long windowDurationMs = validationFailureWindowMinutes * 60 * 1000L;
                
                if (currentTime - lastReset > windowDurationMs) {
                    // Reset the window
                    validationFailuresInWindow.set(0);
                    lastValidationFailureWindowReset.set(currentTime);
                    log.debug("Reset validation failure window - failures reset to 0");
                }
                
                int validationFailures = validationFailuresInWindow.incrementAndGet();
                if (validationFailures >= validationFailureThreshold) {
                    log.warn("Validation failures threshold reached: {} >= {} in {}min window - triggering sync recovery", 
                            validationFailures, validationFailureThreshold, validationFailureWindowMinutes);
                    return true;
                }
                break;
                
            case "consecutive_processing_failures":
                int processingFailures = consecutiveProcessingFailures.get();
                if (processingFailures >= consecutiveFailureThreshold) {
                    log.warn("Consecutive processing failures threshold reached: {} >= {} - triggering sync recovery", 
                            processingFailures, consecutiveFailureThreshold);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    /**
     * Triggers sync recovery and handles the coordination with message processing.
     * <p>
     * This method:
     * 1. Pauses message processing
     * 2. Initiates sync recovery via SyncRecoveryManager
     * 3. Logs recovery events and metrics
     * 4. Resumes processing after recovery completes
     * </p>
     * 
     * @param reason the reason for triggering sync recovery
     */
    private void triggerSyncRecovery(String reason) {
        try {
            log.info("Triggering sync recovery from ToolUpdateConsumer - reason: {}", reason);
            
            // Pause processing during recovery
            pauseProcessing();
            
            // Trigger the actual recovery
            boolean recoverySuccess = syncRecoveryManager.triggerResync(reason);
            
            if (recoverySuccess) {
                log.info("Sync recovery completed successfully - reason: {}", reason);
            } else {
                log.error("Sync recovery failed - reason: {}", reason);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during sync recovery trigger", e);
        } finally {
            // Always resume processing, even if recovery failed
            resumeProcessing();
        }
    }
}