package com.quip.backend.tool.sync;

import com.quip.backend.redis.service.RedisService;
import com.quip.backend.tool.model.ToolInfo;
import com.quip.backend.tool.model.ToolInventoryResponse;
import com.quip.backend.tool.model.ToolResyncRequest;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import com.quip.backend.tool.service.ToolService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service responsible for managing sync recovery operations when Redis message processing fails.
 * <p>
 * This service handles critical tool synchronization failures by:
 * - Detecting when sync recovery is needed
 * - Clearing stale Redis message queues
 * - Sending HTTP resync requests to the agent
 * - Processing complete tool inventory responses
 * - Implementing retry logic with exponential backoff
 * </p>
 * <p>
 * The sync recovery process ensures that the backend's tool database remains consistent
 * with the agent's actual tool capabilities even when individual Redis messages fail.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncRecoveryManager {

    private static final String TOOLS_UPDATES_KEY = "tools:updates";
    
    @Value("${app.tool-sync.recovery.agent.resync-endpoint:/api/tools/resync}")
    private String resyncEndpoint;
    
    private final RedisService redisService;
    private final ToolService toolService;
    private final RestTemplate restTemplate;
    private final ToolSyncMetricsService metricsService;
    
    @Value("${app.tool-sync.recovery.agent.base-url:http://localhost:5001}")
    private String agentBaseUrl;

    /**
     * -- GETTER --
     *  Gets the current configuration status of sync recovery.
     *
     * @return true if sync recovery is enabled, false otherwise
     */
    @Getter
    @Value("${app.tool-sync.recovery.enabled:true}")
    private boolean recoveryEnabled;
    
    @Value("${app.tool-sync.recovery.timeout:10000}")
    private int recoveryTimeoutMs;
    
    @Value("${app.tool-sync.recovery.max-retries:3}")
    private int maxRetryAttempts;
    
    @Value("${app.tool-sync.recovery.initial-delay:1000}")
    private long initialRetryDelayMs;
    
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);

    /**
     * Triggers a sync recovery operation when critical tool update processing failures are detected.
     * <p>
     * This method implements the complete sync recovery flow:
     * 1. Clears the Redis message queue to prevent processing stale messages
     * 2. Sends an HTTP request to the agent for a complete tool inventory
     * 3. Processes the agent's response to synchronize the database
     * 4. Implements retry logic with exponential backoff for failed requests
     * </p>
     * <p>
     * The recovery process is atomic - only one recovery can run at a time.
     * If recovery is already in progress, subsequent calls will be ignored.
     * </p>
     *
     * @param reason the reason for triggering sync recovery (for logging purposes)
     * @return true if recovery was successfully initiated and completed, false otherwise
     */
    public boolean triggerResync(String reason) {
        if (!recoveryEnabled) {
            log.warn("Sync recovery is disabled - skipping recovery for reason: {}", reason);
            return false;
        }
        
        if (!recoveryInProgress.compareAndSet(false, true)) {
            log.warn("Sync recovery already in progress - ignoring new recovery request for reason: {}", reason);
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Starting sync recovery process - reason: {}", reason);
            
            // Step 1: Clear Redis queue to prevent processing stale messages
            boolean queueCleared = clearRedisQueue();
            if (!queueCleared) {
                log.warn("Failed to clear Redis queue during sync recovery - continuing anyway");
            }
            
            // Step 2: Request resync from agent with retry logic
            ToolInventoryResponse inventoryResponse = requestAgentResyncWithRetry(reason);
            if (inventoryResponse == null) {
                log.error("Failed to get tool inventory from agent after {} attempts", maxRetryAttempts);
                long recoveryTime = System.currentTimeMillis() - startTime;
                metricsService.recordSyncRecovery(false, recoveryTime, reason, null);
                return false;
            }
            
            // Step 3: Process the complete tool inventory
            boolean syncSuccess = processSyncResponse(inventoryResponse);
            if (!syncSuccess) {
                log.error("Failed to process sync response from agent");
                long recoveryTime = System.currentTimeMillis() - startTime;
                metricsService.recordSyncRecovery(false, recoveryTime, reason, null);
                return false;
            }
            
            long recoveryTime = System.currentTimeMillis() - startTime;
            int toolInventorySize = inventoryResponse.getCurrentTools().size();
            metricsService.recordSyncRecovery(true, recoveryTime, reason, toolInventorySize);
            
            log.info("Sync recovery completed successfully - processed {} tools in {}ms", 
                    toolInventorySize, recoveryTime);
            return true;
            
        } catch (Exception e) {
            log.error("Unexpected error during sync recovery", e);
            long recoveryTime = System.currentTimeMillis() - startTime;
            metricsService.recordSyncRecovery(false, recoveryTime, reason, null);
            return false;
        } finally {
            recoveryInProgress.set(false);
        }
    }

    /**
     * Clears the Redis message queue to prevent processing stale messages.
     * <p>
     * This method removes all pending tool update messages from the Redis queue
     * to ensure that only fresh data from the agent resync is processed.
     * </p>
     *
     * @return true if the queue was successfully cleared, false otherwise
     */
    public boolean clearRedisQueue() {
        try {
            log.debug("Clearing Redis queue: {}", TOOLS_UPDATES_KEY);
            
            // Get current queue length for logging
            long queueLength = redisService.llen(TOOLS_UPDATES_KEY);
            
            // Delete the entire queue
            boolean deleted = redisService.delete(TOOLS_UPDATES_KEY);
            
            if (deleted && queueLength > 0) {
                log.info("Successfully cleared Redis queue - removed {} pending messages", queueLength);
            } else if (queueLength == 0) {
                log.debug("Redis queue was already empty");
            } else {
                log.warn("Failed to clear Redis queue or queue did not exist");
            }
            
            return deleted || queueLength == 0;
            
        } catch (Exception e) {
            log.error("Error clearing Redis queue", e);
            return false;
        }
    }

    /**
     * Sends an HTTP resync request to the agent with retry logic and exponential backoff.
     * <p>
     * This method implements retry logic with exponential backoff:
     * - Attempt 1: immediate
     * - Attempt 2: 1 second delay
     * - Attempt 3: 2 seconds delay
     * - Attempt 4: 4 seconds delay
     * </p>
     *
     * @param reason the reason for the resync request
     * @return the tool inventory response from the agent, or null if all attempts failed
     */
    private ToolInventoryResponse requestAgentResyncWithRetry(String reason) {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    long delayMs = initialRetryDelayMs * (1L << (attempt - 2)); // Exponential backoff
                    log.debug("Waiting {}ms before retry attempt {}", delayMs, attempt);
                    Thread.sleep(delayMs);
                }
                
                log.debug("Attempting to request agent resync - attempt {} of {}", attempt, maxRetryAttempts);
                ToolInventoryResponse response = requestAgentResync(reason);
                
                if (response != null) {
                    log.info("Successfully received tool inventory from agent on attempt {}", attempt);
                    return response;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Sync recovery interrupted during retry delay", e);
                return null;
            } catch (Exception e) {
                log.warn("Attempt {} failed to get tool inventory from agent: {}", attempt, e.getMessage());
                if (attempt == maxRetryAttempts) {
                    log.error("All {} attempts failed to get tool inventory from agent", maxRetryAttempts, e);
                }
            }
        }
        
        return null;
    }

    /**
     * Sends a direct HTTP request to the agent requesting a complete tool inventory.
     * <p>
     * This method creates a resync request with a unique ID and timestamp,
     * sends it to the agent's resync endpoint, and returns the response.
     * </p>
     *
     * @param reason the reason for the resync request
     * @return the tool inventory response from the agent, or null if the request failed
     * @throws RestClientException if the HTTP request fails
     */
    private ToolInventoryResponse requestAgentResync(String reason) {
        try {
            String requestId = UUID.randomUUID().toString();
            Instant timestamp = Instant.now();
            
            ToolResyncRequest request = new ToolResyncRequest();
            request.setRequestId(requestId);
            request.setTimestamp(timestamp);
            request.setReason(reason);
            
            String url = agentBaseUrl + resyncEndpoint;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<ToolResyncRequest> httpEntity = new HttpEntity<>(request, headers);
            
            log.debug("Sending resync request to agent: {} with requestId: {}", url, requestId);
            
            ResponseEntity<ToolInventoryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                ToolInventoryResponse.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ToolInventoryResponse inventoryResponse = response.getBody();
                log.info("Received tool inventory from agent - requestId: {}, tools count: {}", 
                        requestId, inventoryResponse.getCurrentTools().size());
                return inventoryResponse;
            } else {
                log.error("Agent resync request failed - status: {}, requestId: {}", 
                         response.getStatusCode(), requestId);
                return null;
            }
            
        } catch (RestClientException e) {
            log.error("HTTP error during agent resync request", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during agent resync request", e);
            return null;
        }
    }

    /**
     * Processes the complete tool inventory response from the agent.
     * <p>
     * This method validates the response and delegates to the ToolService
     * to synchronize the database with the agent's current tool inventory.
     * </p>
     *
     * @param response the tool inventory response from the agent
     * @return true if the sync was successful, false otherwise
     */
    private boolean processSyncResponse(ToolInventoryResponse response) {
        try {
            if (response == null) {
                log.error("Cannot process null sync response");
                return false;
            }
            
            if (response.getCurrentTools() == null) {
                log.error("Sync response contains null tool list");
                return false;
            }
            
            log.info("Processing sync response - requestId: {}, tools count: {}", 
                    response.getRequestId(), response.getCurrentTools().size());
            
            // Filter valid tool info objects from the response
            List<ToolInfo> currentToolInfos = response.getCurrentTools().stream()
                    .filter(toolInfo -> toolInfo != null && toolInfo.getName() != null && toolInfo.getMcpServerName() != null)
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("Tool inventory sync processing {} tools with server info: {}", 
                    currentToolInfos.size(), 
                    currentToolInfos.stream().map(t -> t.getName() + "(" + t.getMcpServerName() + ")").collect(java.util.stream.Collectors.toList()));
            
            // Delegate to ToolService for database synchronization
            toolService.syncToolsFromInventoryWithServerInfo(currentToolInfos);
            
            log.info("Successfully synchronized tool inventory with database");
            return true;
            
        } catch (Exception e) {
            log.error("Error processing sync response", e);
            return false;
        }
    }

    /**
     * Checks if a sync recovery operation is currently in progress.
     *
     * @return true if recovery is in progress, false otherwise
     */
    public boolean isRecoveryInProgress() {
        return recoveryInProgress.get();
    }

}