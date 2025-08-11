package com.quip.backend.tool.monitoring;

import com.quip.backend.tool.consumer.ToolUpdateConsumer;
import com.quip.backend.tool.sync.SyncRecoveryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodic monitoring and metrics logging.
 * <p>
 * This component runs scheduled tasks to log system metrics and health status
 * for monitoring purposes. It provides regular snapshots of system performance
 * and health indicators that can be consumed by monitoring systems.
 * </p>
 * <p>
 * The scheduler is enabled by default but can be disabled via configuration
 * using the property: app.tool-sync.monitoring.enabled=false
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.tool-sync.monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class ToolSyncMonitoringScheduler {

    private final ToolSyncMetricsService metricsService;
    private final ToolUpdateConsumer toolUpdateConsumer;
    private final SyncRecoveryManager syncRecoveryManager;

    /**
     * Logs comprehensive metrics summary every 5 minutes.
     * This provides regular monitoring data for system administrators.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logMetricsSummary() {
        try {
            log.debug("Running scheduled metrics summary logging");
            metricsService.logMetricsSummary();
        } catch (Exception e) {
            log.error("Error during scheduled metrics summary logging", e);
        }
    }

    /**
     * Logs consumer status every 10 minutes.
     * This provides monitoring data for the Redis message consumer health.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void logConsumerStatus() {
        try {
            log.debug("Running scheduled consumer status logging");
            toolUpdateConsumer.logMetrics();
        } catch (Exception e) {
            log.error("Error during scheduled consumer status logging", e);
        }
    }

    /**
     * Logs system health status every 15 minutes.
     * This provides high-level health indicators for monitoring systems.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void logSystemHealth() {
        try {
            log.debug("Running scheduled system health logging");
            
            String healthStatus = metricsService.getSystemHealthStatus();
            boolean consumerRunning = toolUpdateConsumer.isRunning();
            boolean consumerEnabled = toolUpdateConsumer.isEnabled();
            boolean recoveryInProgress = syncRecoveryManager.isRecoveryInProgress();
            boolean recoveryEnabled = syncRecoveryManager.isRecoveryEnabled();
            
            log.info("System health status - toolSyncHealth={}, consumerEnabled={}, consumerRunning={}, recoveryEnabled={}, recoveryInProgress={}", 
                    healthStatus, consumerEnabled, consumerRunning, recoveryEnabled, recoveryInProgress);
            
            // Log warning if system is not healthy
            if (!"healthy".equals(healthStatus) && !"idle".equals(healthStatus)) {
                log.warn("Tool synchronization system health is degraded: {}", healthStatus);
            }
            
            // Log warning if consumer is not running when it should be
            if (consumerEnabled && !consumerRunning) {
                log.warn("Tool update consumer is enabled but not running");
            }
            
            // Log warning if recovery is in progress for too long
            if (recoveryInProgress) {
                log.warn("Sync recovery operation is currently in progress");
            }
            
        } catch (Exception e) {
            log.error("Error during scheduled system health logging", e);
        }
    }

    /**
     * Monitors sync recovery events every 5 minutes and logs alerts if recovery is frequent.
     * This provides early warning for sync recovery issues.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void monitorSyncRecovery() {
        try {
            log.debug("Running scheduled sync recovery monitoring");
            
            // Check if sync recovery is happening too frequently
            if (metricsService.isSyncRecoveryFrequent()) {
                log.warn("ALERT: Sync recovery events are occurring frequently - this may indicate system issues");
                
                // Log detailed sync recovery status for debugging
                var recoveryStatus = metricsService.getSyncRecoveryHealthStatus();
                log.warn("Sync recovery status - triggered={}, successful={}, failed={}, successRate={}, avgTime={}ms", 
                        recoveryStatus.get("totalTriggered"),
                        recoveryStatus.get("totalSuccessful"), 
                        recoveryStatus.get("totalFailed"),
                        recoveryStatus.get("successRate"),
                        recoveryStatus.get("averageRecoveryTimeMs"));
            }
            
            // Log sync recovery metrics summary
            var recoveryStatus = metricsService.getSyncRecoveryHealthStatus();
            log.info("Sync recovery metrics - triggered={}, successRate={}, toolInventoryChanges={}, currentSize={}", 
                    recoveryStatus.get("totalTriggered"),
                    recoveryStatus.get("successRate"),
                    recoveryStatus.get("toolInventoryChanges"),
                    recoveryStatus.get("currentToolInventorySize"));
            
        } catch (Exception e) {
            log.error("Error during scheduled sync recovery monitoring", e);
        }
    }

    /**
     * Resets metrics counters daily at midnight.
     * This prevents metrics from growing indefinitely and provides daily snapshots.
     */
    @Scheduled(cron = "0 0 0 * * *") // Daily at midnight
    public void resetDailyMetrics() {
        try {
            log.info("Running scheduled daily metrics reset");
            metricsService.resetMetrics();
            log.info("Daily metrics reset completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled daily metrics reset", e);
        }
    }
}