package com.quip.backend.health;

import com.quip.backend.redis.health.RedisHealthIndicator;
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

    @Autowired
    public HealthController(@Autowired(required = false) RedisHealthIndicator redisHealthIndicator) {
        this.redisHealthIndicator = redisHealthIndicator;
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
     * Root endpoint for basic connectivity testing.
     * @return Simple status message
     */
    @GetMapping("/")
    public String root() {
        return "Quip Backend Service is running";
    }
}