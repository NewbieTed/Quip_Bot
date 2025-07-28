package com.quip.backend.redis.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler component for Redis metrics updates.
 * This component is separated from RedisConfig to avoid circular dependencies.
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisMetricsScheduler {

    private final RedisMetricsService metricsService;

    @Autowired
    public RedisMetricsScheduler(RedisMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * Scheduled task to update Redis connection pool metrics every 30 seconds.
     */
    @Scheduled(fixedRate = 30000) // 30 seconds
    public void updateRedisMetrics() {
        try {
            metricsService.updateConnectionPoolMetrics();
            log.debug("Redis metrics updated successfully");
        } catch (Exception e) {
            log.warn("Failed to update Redis metrics: {}", e.getMessage());
        }
    }
}