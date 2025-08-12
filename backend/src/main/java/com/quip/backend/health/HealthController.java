package com.quip.backend.health;

import com.quip.backend.redis.health.RedisHealthIndicator;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import com.quip.backend.tool.sync.SyncRecoveryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring backend status including Redis connectivity.
 */
@RestController
public class HealthController {

    private final RedisHealthIndicator redisHealthIndicator;
    private final ToolSyncMetricsService toolSyncMetricsService;
    private final SyncRecoveryManager syncRecoveryManager;

    @Autowired
    public HealthController(
            @Autowired(required = false) RedisHealthIndicator redisHealthIndicator,
            @Autowired(required = false) ToolSyncMetricsService toolSyncMetricsService,
            @Autowired(required = false) SyncRecoveryManager syncRecoveryManager) {
        this.redisHealthIndicator = redisHealthIndicator;
        this.toolSyncMetricsService = toolSyncMetricsService;
        this.syncRecoveryManager = syncRecoveryManager;
    }

    /**
     * Basic health check endpoint.
     * @return Simple status message
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    /**
     * Detailed health check endpoint with Redis status.
     * @return Detailed health information including Redis metrics
     */
    @GetMapping("/health/detailed")
    public Map<String, Object> detailedHealth() {
        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("status", "UP");
        healthStatus.put("service", "Quip Backend Service");
        
        // Add Redis health information if available
        if (redisHealthIndicator != null) {
            try {
                Map<String, Object> redisHealth = redisHealthIndicator.checkHealth();
                healthStatus.put("redis", redisHealth);
            } catch (Exception e) {
                Map<String, Object> redisError = new HashMap<>();
                redisError.put("status", "DOWN");
                redisError.put("error", "Redis health check failed: " + e.getMessage());
                healthStatus.put("redis", redisError);
            }
        } else {
            healthStatus.put("redis", "disabled");
        }
        
        // Add tool sync health information if available
        if (toolSyncMetricsService != null) {
            try {
                String toolSyncHealth = toolSyncMetricsService.getSystemHealthStatus();
                Map<String, Object> toolSyncStatus = new HashMap<>();
                toolSyncStatus.put("status", toolSyncHealth);
                toolSyncStatus.put("syncRecoveryHealth", toolSyncMetricsService.getSyncRecoveryHealthStatus());
                healthStatus.put("toolSync", toolSyncStatus);
            } catch (Exception e) {
                Map<String, Object> toolSyncError = new HashMap<>();
                toolSyncError.put("status", "ERROR");
                toolSyncError.put("error", "Tool sync health check failed: " + e.getMessage());
                healthStatus.put("toolSync", toolSyncError);
            }
        } else {
            healthStatus.put("toolSync", "disabled");
        }
        
        return healthStatus;
    }

    /**
     * Redis-specific health check endpoint.
     * @return Redis health information and metrics
     */
    @GetMapping("/health/redis")
    @ConditionalOnBean(RedisHealthIndicator.class)
    public Map<String, Object> redisHealth() {
        if (redisHealthIndicator != null) {
            return redisHealthIndicator.checkHealth();
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "DISABLED");
            response.put("message", "Redis is not enabled");
            return response;
        }
    }

    /**
     * Tool sync specific health check endpoint.
     * @return Tool synchronization health information and metrics
     */
    @GetMapping("/health/tool-sync")
    public Map<String, Object> toolSyncHealth() {
        Map<String, Object> response = new HashMap<>();
        
        if (toolSyncMetricsService != null) {
            try {
                response.put("status", toolSyncMetricsService.getSystemHealthStatus());
                response.put("metrics", toolSyncMetricsService.getMetricsSummary());
                response.put("syncRecovery", toolSyncMetricsService.getSyncRecoveryHealthStatus());
                
                if (syncRecoveryManager != null) {
                    response.put("recoveryInProgress", syncRecoveryManager.isRecoveryInProgress());
                    response.put("recoveryEnabled", syncRecoveryManager.isRecoveryEnabled());
                }
                
            } catch (Exception e) {
                response.put("status", "ERROR");
                response.put("error", "Tool sync health check failed: " + e.getMessage());
            }
        } else {
            response.put("status", "DISABLED");
            response.put("message", "Tool sync monitoring is not enabled");
        }
        
        return response;
    }

    /**
     * Sync recovery specific health check endpoint.
     * @return Sync recovery status and metrics
     */
    @GetMapping("/health/sync-recovery")
    public Map<String, Object> syncRecoveryHealth() {
        Map<String, Object> response = new HashMap<>();
        
        if (syncRecoveryManager != null && toolSyncMetricsService != null) {
            try {
                response.put("enabled", syncRecoveryManager.isRecoveryEnabled());
                response.put("inProgress", syncRecoveryManager.isRecoveryInProgress());
                response.put("metrics", toolSyncMetricsService.getSyncRecoveryHealthStatus());
                response.put("isFrequent", toolSyncMetricsService.isSyncRecoveryFrequent());
                
            } catch (Exception e) {
                response.put("status", "ERROR");
                response.put("error", "Sync recovery health check failed: " + e.getMessage());
            }
        } else {
            response.put("status", "DISABLED");
            response.put("message", "Sync recovery is not enabled or configured");
        }
        
        return response;
    }

    /**
     * Root endpoint for basic connectivity testing.
     * @return Simple status message
     */
    @GetMapping("/")
    public String root() {
        return "Quip Backend Service is running";
    }
}