package com.quip.backend.tool.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for collecting and reporting tool synchronization metrics.
 * <p>
 * This service provides centralized metrics collection for tool update operations,
 * message processing latency, and system health monitoring. It tracks metrics
 * across the tool update consumer, message handler, and tool service components.
 * </p>
 * <p>
 * Metrics collected include:
 * - Message processing counts and latency
 * - Tool database operation counts and timing
 * - Error rates and types
 * - System health indicators
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolSyncMetricsService {

    // Message processing metrics
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private final AtomicLong messagesProcessedSuccessfully = new AtomicLong(0);
    private final AtomicLong messagesProcessedWithErrors = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    
    // Tool database operation metrics
    private final AtomicLong toolsCreated = new AtomicLong(0);
    private final AtomicLong toolsEnabled = new AtomicLong(0);
    private final AtomicLong toolsDisabled = new AtomicLong(0);
    private final AtomicLong toolDatabaseErrors = new AtomicLong(0);
    private final AtomicLong totalDatabaseOperationTimeMs = new AtomicLong(0);
    
    // Error tracking
    private final AtomicLong validationErrors = new AtomicLong(0);
    private final AtomicLong deserializationErrors = new AtomicLong(0);
    private final AtomicLong unexpectedErrors = new AtomicLong(0);
    
    // System health tracking
    private final AtomicReference<OffsetDateTime> lastMessageProcessed = new AtomicReference<>();
    private final AtomicReference<OffsetDateTime> lastSuccessfulOperation = new AtomicReference<>();
    private final AtomicReference<OffsetDateTime> lastError = new AtomicReference<>();
    private final AtomicReference<OffsetDateTime> metricsResetTime = new AtomicReference<>(OffsetDateTime.now());
    
    // Sync recovery metrics
    private final AtomicLong syncRecoveryTriggered = new AtomicLong(0);
    private final AtomicLong syncRecoverySuccessful = new AtomicLong(0);
    private final AtomicLong syncRecoveryFailed = new AtomicLong(0);
    private final AtomicLong totalSyncRecoveryTimeMs = new AtomicLong(0);
    private final AtomicLong toolInventorySizeChanges = new AtomicLong(0);
    private final AtomicReference<OffsetDateTime> lastSyncRecovery = new AtomicReference<>();
    private final AtomicReference<OffsetDateTime> lastSuccessfulSyncRecovery = new AtomicReference<>();
    private final AtomicReference<Integer> lastToolInventorySize = new AtomicReference<>(0);

    /**
     * Records metrics for a processed message.
     * 
     * @param success whether the message was processed successfully
     * @param processingTimeMs the time taken to process the message in milliseconds
     */
    public void recordMessageProcessed(boolean success, long processingTimeMs) {
        messagesReceived.incrementAndGet();
        totalProcessingTimeMs.addAndGet(processingTimeMs);
        lastMessageProcessed.set(OffsetDateTime.now());
        
        if (success) {
            messagesProcessedSuccessfully.incrementAndGet();
            lastSuccessfulOperation.set(OffsetDateTime.now());
            
            log.debug("Recorded successful message processing - processingTime={}ms, totalMessages={}", 
                    processingTimeMs, messagesReceived.get());
        } else {
            messagesProcessedWithErrors.incrementAndGet();
            lastError.set(OffsetDateTime.now());
            
            log.debug("Recorded failed message processing - processingTime={}ms, totalErrors={}", 
                    processingTimeMs, messagesProcessedWithErrors.get());
        }
    }

    /**
     * Records metrics for a tool database operation.
     * 
     * @param operationType the type of operation (create, enable, disable)
     * @param success whether the operation was successful
     * @param operationTimeMs the time taken for the operation in milliseconds
     */
    public void recordToolDatabaseOperation(String operationType, boolean success, long operationTimeMs) {
        totalDatabaseOperationTimeMs.addAndGet(operationTimeMs);
        
        if (success) {
            switch (operationType.toLowerCase()) {
                case "create":
                    toolsCreated.incrementAndGet();
                    break;
                case "enable":
                    toolsEnabled.incrementAndGet();
                    break;
                case "disable":
                    toolsDisabled.incrementAndGet();
                    break;
            }
            
            lastSuccessfulOperation.set(OffsetDateTime.now());
            
            log.debug("Recorded successful tool database operation - operation={}, operationTime={}ms", 
                    operationType, operationTimeMs);
        } else {
            toolDatabaseErrors.incrementAndGet();
            lastError.set(OffsetDateTime.now());
            
            log.debug("Recorded failed tool database operation - operation={}, operationTime={}ms", 
                    operationType, operationTimeMs);
        }
    }

    /**
     * Records an error by type.
     * 
     * @param errorType the type of error (validation, deserialization, unexpected)
     */
    public void recordError(String errorType) {
        lastError.set(OffsetDateTime.now());
        
        switch (errorType.toLowerCase()) {
            case "validation":
                validationErrors.incrementAndGet();
                break;
            case "deserialization":
                deserializationErrors.incrementAndGet();
                break;
            case "unexpected":
                unexpectedErrors.incrementAndGet();
                break;
            default:
                unexpectedErrors.incrementAndGet();
                log.warn("Unknown error type recorded: {}", errorType);
        }
        
        log.debug("Recorded error - type={}, totalErrors={}", 
                errorType, getTotalErrors());
    }

    /**
     * Records metrics for a sync recovery operation.
     * 
     * @param success whether the sync recovery was successful
     * @param recoveryTimeMs the time taken for the recovery operation in milliseconds
     * @param reason the reason for triggering sync recovery
     * @param toolInventorySize the size of the tool inventory after recovery (if successful)
     */
    public void recordSyncRecovery(boolean success, long recoveryTimeMs, String reason, Integer toolInventorySize) {
        syncRecoveryTriggered.incrementAndGet();
        totalSyncRecoveryTimeMs.addAndGet(recoveryTimeMs);
        lastSyncRecovery.set(OffsetDateTime.now());
        
        if (success) {
            syncRecoverySuccessful.incrementAndGet();
            lastSuccessfulSyncRecovery.set(OffsetDateTime.now());
            
            // Track tool inventory size changes
            if (toolInventorySize != null) {
                Integer previousSize = lastToolInventorySize.getAndSet(toolInventorySize);
                if (previousSize != null && !previousSize.equals(toolInventorySize)) {
                    toolInventorySizeChanges.incrementAndGet();
                    
                    log.info("Tool inventory size changed during sync recovery - previous={}, current={}, change={}", 
                            previousSize, toolInventorySize, toolInventorySize - previousSize);
                }
            }
            
            log.info("Recorded successful sync recovery - reason={}, recoveryTime={}ms, toolInventorySize={}, totalRecoveries={}", 
                    reason, recoveryTimeMs, toolInventorySize, syncRecoverySuccessful.get());
        } else {
            syncRecoveryFailed.incrementAndGet();
            
            log.warn("Recorded failed sync recovery - reason={}, recoveryTime={}ms, totalFailures={}", 
                    reason, recoveryTimeMs, syncRecoveryFailed.get());
        }
    }

    /**
     * Records a tool inventory size change outside of sync recovery.
     * 
     * @param newSize the new tool inventory size
     */
    public void recordToolInventorySize(int newSize) {
        Integer previousSize = lastToolInventorySize.getAndSet(newSize);
        if (previousSize != null && !previousSize.equals(newSize)) {
            toolInventorySizeChanges.incrementAndGet();
            
            log.debug("Tool inventory size changed - previous={}, current={}, change={}", 
                    previousSize, newSize, newSize - previousSize);
        }
    }

    /**
     * Gets comprehensive metrics summary.
     * 
     * @return Map containing all metrics
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Message processing metrics
        Map<String, Object> messageMetrics = new HashMap<>();
        messageMetrics.put("messagesReceived", messagesReceived.get());
        messageMetrics.put("messagesProcessedSuccessfully", messagesProcessedSuccessfully.get());
        messageMetrics.put("messagesProcessedWithErrors", messagesProcessedWithErrors.get());
        messageMetrics.put("averageProcessingTimeMs", getAverageProcessingTime());
        messageMetrics.put("successRate", getMessageSuccessRate());
        metrics.put("messageProcessing", messageMetrics);
        
        // Tool database operation metrics
        Map<String, Object> databaseMetrics = new HashMap<>();
        databaseMetrics.put("toolsCreated", toolsCreated.get());
        databaseMetrics.put("toolsEnabled", toolsEnabled.get());
        databaseMetrics.put("toolsDisabled", toolsDisabled.get());
        databaseMetrics.put("toolDatabaseErrors", toolDatabaseErrors.get());
        databaseMetrics.put("averageDatabaseOperationTimeMs", getAverageDatabaseOperationTime());
        databaseMetrics.put("totalToolOperations", getTotalToolOperations());
        metrics.put("databaseOperations", databaseMetrics);
        
        // Error metrics
        Map<String, Object> errorMetrics = new HashMap<>();
        errorMetrics.put("validationErrors", validationErrors.get());
        errorMetrics.put("deserializationErrors", deserializationErrors.get());
        errorMetrics.put("unexpectedErrors", unexpectedErrors.get());
        errorMetrics.put("totalErrors", getTotalErrors());
        metrics.put("errors", errorMetrics);
        
        // Sync recovery metrics
        Map<String, Object> syncRecoveryMetrics = new HashMap<>();
        syncRecoveryMetrics.put("syncRecoveryTriggered", syncRecoveryTriggered.get());
        syncRecoveryMetrics.put("syncRecoverySuccessful", syncRecoverySuccessful.get());
        syncRecoveryMetrics.put("syncRecoveryFailed", syncRecoveryFailed.get());
        syncRecoveryMetrics.put("averageSyncRecoveryTimeMs", getAverageSyncRecoveryTime());
        syncRecoveryMetrics.put("syncRecoverySuccessRate", getSyncRecoverySuccessRate());
        syncRecoveryMetrics.put("toolInventorySizeChanges", toolInventorySizeChanges.get());
        syncRecoveryMetrics.put("lastSyncRecovery", lastSyncRecovery.get());
        syncRecoveryMetrics.put("lastSuccessfulSyncRecovery", lastSuccessfulSyncRecovery.get());
        syncRecoveryMetrics.put("currentToolInventorySize", lastToolInventorySize.get());
        metrics.put("syncRecovery", syncRecoveryMetrics);
        
        // System health metrics
        Map<String, Object> healthMetrics = new HashMap<>();
        healthMetrics.put("lastMessageProcessed", lastMessageProcessed.get());
        healthMetrics.put("lastSuccessfulOperation", lastSuccessfulOperation.get());
        healthMetrics.put("lastError", lastError.get());
        healthMetrics.put("metricsResetTime", metricsResetTime.get());
        healthMetrics.put("systemHealthStatus", getSystemHealthStatus());
        metrics.put("systemHealth", healthMetrics);
        
        return metrics;
    }

    /**
     * Logs comprehensive metrics summary for monitoring.
     */
    public void logMetricsSummary() {
        Map<String, Object> metrics = getMetricsSummary();
        
        log.info("Tool sync metrics summary - messages={}, success={}, errors={}, avgProcessingTime={}ms, toolsCreated={}, toolsEnabled={}, toolsDisabled={}, syncRecoveries={}, syncRecoverySuccessRate={}, toolInventoryChanges={}, healthStatus={}", 
                messagesReceived.get(),
                messagesProcessedSuccessfully.get(),
                messagesProcessedWithErrors.get(),
                getAverageProcessingTime(),
                toolsCreated.get(),
                toolsEnabled.get(),
                toolsDisabled.get(),
                syncRecoveryTriggered.get(),
                getSyncRecoverySuccessRate(),
                toolInventorySizeChanges.get(),
                getSystemHealthStatus());
    }

    /**
     * Resets all metrics counters.
     */
    public void resetMetrics() {
        messagesReceived.set(0);
        messagesProcessedSuccessfully.set(0);
        messagesProcessedWithErrors.set(0);
        totalProcessingTimeMs.set(0);
        
        toolsCreated.set(0);
        toolsEnabled.set(0);
        toolsDisabled.set(0);
        toolDatabaseErrors.set(0);
        totalDatabaseOperationTimeMs.set(0);
        
        validationErrors.set(0);
        deserializationErrors.set(0);
        unexpectedErrors.set(0);
        
        syncRecoveryTriggered.set(0);
        syncRecoverySuccessful.set(0);
        syncRecoveryFailed.set(0);
        totalSyncRecoveryTimeMs.set(0);
        toolInventorySizeChanges.set(0);
        
        lastMessageProcessed.set(null);
        lastSuccessfulOperation.set(null);
        lastError.set(null);
        lastSyncRecovery.set(null);
        lastSuccessfulSyncRecovery.set(null);
        metricsResetTime.set(OffsetDateTime.now());
        
        log.info("Tool sync metrics reset");
    }

    /**
     * Gets system health status based on metrics.
     * 
     * @return health status string
     */
    public String getSystemHealthStatus() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime lastSuccess = lastSuccessfulOperation.get();
        OffsetDateTime lastErr = lastError.get();
        OffsetDateTime lastRecovery = lastSyncRecovery.get();
        
        // Check if we've had recent activity
        if (lastSuccess == null && messagesReceived.get() == 0) {
            return "idle"; // No activity yet
        }
        
        // Check sync recovery frequency - if more than 3 recoveries in recent period, system is degraded
        if (syncRecoveryTriggered.get() > 3) {
            OffsetDateTime resetTime = metricsResetTime.get();
            long hoursSinceReset = java.time.Duration.between(resetTime, now).toHours();
            if (hoursSinceReset < 1) { // More than 3 recoveries in less than 1 hour
                return "degraded";
            }
        }
        
        // Check sync recovery success rate
        double recoverySuccessRate = getSyncRecoverySuccessRate();
        if (syncRecoveryTriggered.get() > 0 && recoverySuccessRate < 0.5) { // Less than 50% recovery success rate
            return "unhealthy";
        }
        
        // Check error rate
        double errorRate = getMessageErrorRate();
        if (errorRate > 0.5) { // More than 50% error rate
            return "unhealthy";
        }
        
        // Check if we've had recent errors without recovery
        if (lastErr != null && (lastSuccess == null || lastErr.isAfter(lastSuccess))) {
            long minutesSinceError = java.time.Duration.between(lastErr, now).toMinutes();
            if (minutesSinceError < 5) { // Recent error within 5 minutes
                return "degraded";
            }
        }
        
        return "healthy";
    }

    /**
     * Checks if sync recovery events are occurring too frequently.
     * 
     * @return true if recovery events exceed the alerting threshold
     */
    public boolean isSyncRecoveryFrequent() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime resetTime = metricsResetTime.get();
        long hoursSinceReset = java.time.Duration.between(resetTime, now).toHours();
        
        // Alert if more than 3 recoveries in 1 hour
        return syncRecoveryTriggered.get() > 3 && hoursSinceReset <= 1;
    }

    /**
     * Gets sync recovery health status for monitoring.
     * 
     * @return Map containing sync recovery health information
     */
    public Map<String, Object> getSyncRecoveryHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("isFrequent", isSyncRecoveryFrequent());
        status.put("successRate", getSyncRecoverySuccessRate());
        status.put("totalTriggered", syncRecoveryTriggered.get());
        status.put("totalSuccessful", syncRecoverySuccessful.get());
        status.put("totalFailed", syncRecoveryFailed.get());
        status.put("averageRecoveryTimeMs", getAverageSyncRecoveryTime());
        status.put("lastRecovery", lastSyncRecovery.get());
        status.put("lastSuccessfulRecovery", lastSuccessfulSyncRecovery.get());
        status.put("toolInventoryChanges", toolInventorySizeChanges.get());
        status.put("currentToolInventorySize", lastToolInventorySize.get());
        
        return status;
    }

    // Helper methods for calculations
    private double getAverageProcessingTime() {
        long messages = messagesReceived.get();
        return messages > 0 ? (double) totalProcessingTimeMs.get() / messages : 0.0;
    }
    
    private double getAverageDatabaseOperationTime() {
        long operations = getTotalToolOperations();
        return operations > 0 ? (double) totalDatabaseOperationTimeMs.get() / operations : 0.0;
    }
    
    private double getMessageSuccessRate() {
        long total = messagesReceived.get();
        return total > 0 ? (double) messagesProcessedSuccessfully.get() / total : 0.0;
    }
    
    private double getMessageErrorRate() {
        long total = messagesReceived.get();
        return total > 0 ? (double) messagesProcessedWithErrors.get() / total : 0.0;
    }
    
    private long getTotalToolOperations() {
        return toolsCreated.get() + toolsEnabled.get() + toolsDisabled.get() + toolDatabaseErrors.get();
    }
    
    private long getTotalErrors() {
        return validationErrors.get() + deserializationErrors.get() + unexpectedErrors.get();
    }
    
    private double getAverageSyncRecoveryTime() {
        long recoveries = syncRecoveryTriggered.get();
        return recoveries > 0 ? (double) totalSyncRecoveryTimeMs.get() / recoveries : 0.0;
    }
    
    private double getSyncRecoverySuccessRate() {
        long total = syncRecoveryTriggered.get();
        return total > 0 ? (double) syncRecoverySuccessful.get() / total : 0.0;
    }
}