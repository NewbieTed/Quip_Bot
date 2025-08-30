package com.quip.backend.assistant.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.assistant.handler.InterruptMessageHandler;
import com.quip.backend.assistant.model.InterruptMessage;
import com.quip.backend.redis.service.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis message consumer for interrupt messages from the agent.
 * <p>
 * This component listens for interrupt messages on the "assistant:interrupts" Redis list
 * and processes them to update conversation interrupt status. The consumer runs in a 
 * separate thread and continuously polls Redis for new messages.
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
public class InterruptMessageConsumer {

    private static final String INTERRUPT_MESSAGES_KEY = "assistant:interrupts";
    private static final long POLLING_TIMEOUT_SECONDS = 5L;
    private static final int CONSUMER_THREAD_POOL_SIZE = 1;

    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final InterruptMessageHandler messageHandler;

    @Value("${app.assistant.interrupt-consumer.enabled:true}")
    private boolean consumerEnabled;

    @Value("${app.assistant.interrupt-consumer.polling-timeout:5}")
    private long pollingTimeoutSeconds;

    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private CompletableFuture<Void> consumerTask;

    /**
     * Initializes and starts the Redis message consumer.
     * Creates a dedicated thread pool for message consumption and begins polling.
     */
    @PostConstruct
    public void startConsumer() {
        if (!consumerEnabled) {
            log.info("Interrupt message consumer is disabled via configuration (app.assistant.interrupt-consumer.enabled=false)");
            return;
        }

        log.info("Starting interrupt message consumer with configuration: redisKey={}, pollingTimeout={}s, threadPoolSize={}", 
                INTERRUPT_MESSAGES_KEY, pollingTimeoutSeconds, CONSUMER_THREAD_POOL_SIZE);
        
        try {
            executorService = Executors.newFixedThreadPool(CONSUMER_THREAD_POOL_SIZE, 
                r -> {
                    Thread t = new Thread(r, "interrupt-message-consumer");
                    t.setDaemon(true);
                    return t;
                });

            running.set(true);
            consumerTask = CompletableFuture.runAsync(this::consumeMessages, executorService);
            
            log.info("Interrupt message consumer started successfully and is now listening for messages");
            
        } catch (Exception e) {
            log.error("Failed to start interrupt message consumer", e);
            
            // Clean up resources if startup failed
            running.set(false);
            if (executorService != null) {
                executorService.shutdown();
                executorService = null;
            }
            
            throw new RuntimeException("Failed to start interrupt message consumer", e);
        }
    }

    /**
     * Stops the Redis message consumer and cleans up resources.
     * Ensures graceful shutdown of the polling thread and executor service.
     */
    @PreDestroy
    public void stopConsumer() {
        if (!consumerEnabled) {
            log.debug("Interrupt message consumer was not enabled, no shutdown needed");
            return;
        }
        
        if (!running.get()) {
            log.debug("Interrupt message consumer is not running, no shutdown needed");
            return;
        }

        log.info("Stopping interrupt message consumer...");
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
                    log.warn("Interrupt message consumer did not terminate gracefully within 10 seconds, forcing shutdown");
                    executorService.shutdownNow();
                    
                    // Wait a bit more for forced shutdown
                    terminated = executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (!terminated) {
                        log.error("Interrupt message consumer could not be terminated even after forced shutdown");
                    }
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for interrupt message consumer to terminate, forcing shutdown");
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        long shutdownTime = System.currentTimeMillis() - shutdownStartTime;
        log.info("Interrupt message consumer stopped successfully in {}ms", shutdownTime);
    }

    /**
     * Main message consumption loop.
     * Continuously polls Redis for new messages and processes them.
     * Handles errors gracefully and continues processing with exponential backoff.
     */
    private void consumeMessages() {
        log.info("Interrupt message consumer thread started, polling Redis key: {}", INTERRUPT_MESSAGES_KEY);
        
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
                String messageJson = redisService.rpop(INTERRUPT_MESSAGES_KEY, String.class);
                
                if (messageJson != null) {
                    log.debug("Received interrupt message from Redis: {}", messageJson);
                    
                    long messageProcessingStart = System.currentTimeMillis();
                    boolean processingSuccess = processMessage(messageJson);
                    long messageProcessingTime = System.currentTimeMillis() - messageProcessingStart;
                    
                    messagesProcessed++;
                    totalProcessingTime += messageProcessingTime;
                    
                    if (processingSuccess) {
                        log.info("Interrupt message processed successfully - messageCount={}, avgProcessingTime={}ms, lastProcessingTime={}ms", 
                                messagesProcessed, 
                                messagesProcessed > 0 ? totalProcessingTime / messagesProcessed : 0,
                                messageProcessingTime);
                    } else {
                        log.warn("Interrupt message processing failed");
                    }
                    
                    // Reset error counter on successful message processing
                    if (consecutiveErrors > 0) {
                        log.info("Redis connection recovered after {} consecutive errors", consecutiveErrors);
                        consecutiveErrors = 0;
                    }
                } else {
                    // No message received within timeout, continue polling
                    // Only log every 60 seconds to reduce noise
                    if (messagesProcessed % 12 == 0) { // 12 * 5 seconds = 60 seconds
                        log.trace("No interrupt messages received within {} seconds, continuing to poll", pollingTimeoutSeconds);
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
                
                if (consecutiveErrors <= maxConsecutiveErrors) {
                    log.error("Error occurred while consuming interrupt messages - attempt={}/{}, loopTime={}ms, totalMessages={}", 
                            consecutiveErrors, maxConsecutiveErrors, loopTime, messagesProcessed, e);
                } else {
                    log.error("Critical: Too many consecutive errors ({}) while consuming interrupt messages. " +
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
                    log.info("Interrupt message consumer thread interrupted during error recovery");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // Log final statistics
        log.info("Interrupt message consumer thread stopped - running={}, interrupted={}, totalMessages={}, avgProcessingTime={}ms", 
                running.get(), Thread.currentThread().isInterrupted(), messagesProcessed,
                messagesProcessed > 0 ? totalProcessingTime / messagesProcessed : 0);
    }

    /**
     * Processes a single interrupt message.
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
            InterruptMessage message = objectMapper.readValue(messageJson, InterruptMessage.class);
            messageId = message.getMessageId();
            
            log.info("Processing interrupt message - messageId={}, timestamp={}, source={}, memberId={}, serverId={}, toolName={}", 
                    message.getMessageId(), 
                    message.getTimestamp(),
                    message.getSource(),
                    message.getMemberId(),
                    message.getServerId(),
                    message.getInterruptedToolName());
            
            // Delegate processing to the message handler
            boolean success = messageHandler.handleInterrupt(message);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                log.info("Successfully processed interrupt message - messageId={}, processingTime={}ms, result=success", 
                        messageId, processingTime);
                return true;
            } else {
                log.error("Failed to process interrupt message - messageId={}, processingTime={}ms, result=failure", 
                        messageId, processingTime);
                return false;
            }
            
        } catch (JsonProcessingException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.error("Failed to deserialize interrupt message JSON - messageId={}, processingTime={}ms, errorType=JsonProcessingException, jsonLength={}", 
                    messageId, processingTime, messageJson != null ? messageJson.length() : 0, e);
            
            return false;
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.error("Unexpected error processing interrupt message - messageId={}, processingTime={}ms, errorType={}, jsonLength={}", 
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
     * Returns the Redis key being monitored for interrupt messages.
     * 
     * @return the Redis key name
     */
    public String getRedisKey() {
        return INTERRUPT_MESSAGES_KEY;
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
        log.info("Interrupt message consumer status: enabled={}, running={}, redisKey={}, pollingTimeout={}s", 
                consumerEnabled, running.get(), INTERRUPT_MESSAGES_KEY, pollingTimeoutSeconds);
    }
}