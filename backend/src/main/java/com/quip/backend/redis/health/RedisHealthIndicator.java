package com.quip.backend.redis.health;

import com.quip.backend.config.redis.RedisProperties;
import com.quip.backend.redis.metrics.RedisMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Health indicator for Redis connectivity and metrics collection.
 * Provides detailed health status reporting including connection metrics,
 * memory usage, and Redis server information.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisHealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final RedisProperties redisProperties;
    private final RedisMetricsService metricsService;

    @Autowired
    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate,
                               RedisConnectionFactory connectionFactory,
                               RedisProperties redisProperties,
                               RedisMetricsService metricsService) {
        this.redisTemplate = redisTemplate;
        this.connectionFactory = connectionFactory;
        this.redisProperties = redisProperties;
        this.metricsService = metricsService;
    }

    /**
     * Performs comprehensive health check for Redis connectivity and metrics.
     *
     * @return Health status with detailed Redis information
     */
    public Map<String, Object> checkHealth() {
        try {
            log.debug("Performing Redis health check");
            
            Map<String, Object> healthDetails = new HashMap<>();
            
            // Test basic connectivity
            long startTime = System.currentTimeMillis();
            String pingResult = testRedisConnectivity();
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (!"PONG".equals(pingResult)) {
                healthDetails.put("status", "DOWN");
                healthDetails.put("error", "Redis ping failed");
                healthDetails.put("response", pingResult);
                healthDetails.put("timestamp", Instant.now());
                return healthDetails;
            }

            // Add basic connectivity details
            healthDetails.put("status", "UP");
            healthDetails.put("ping", pingResult);
            healthDetails.put("responseTime", responseTime + "ms");
            healthDetails.put("timestamp", Instant.now());

            // Add connection details
            addConnectionDetails(healthDetails);

            // Add Redis server information
            addRedisServerInfo(healthDetails);

            // Add connection pool metrics
            addConnectionPoolMetrics(healthDetails);

            // Add memory usage information
            addMemoryUsageInfo(healthDetails);

            // Add application-level metrics
            addApplicationMetrics(healthDetails);

            log.debug("Redis health check completed successfully in {}ms", responseTime);
            return healthDetails;

        } catch (Exception e) {
            log.error("Redis health check failed", e);
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", "DOWN");
            errorDetails.put("error", e.getMessage());
            errorDetails.put("exception", e.getClass().getSimpleName());
            errorDetails.put("timestamp", Instant.now());
            return errorDetails;
        }
    }

    /**
     * Tests basic Redis connectivity using PING command.
     *
     * @return PING response from Redis server
     * @throws Exception if connection fails
     */
    private String testRedisConnectivity() throws Exception {
        return redisTemplate.execute((RedisCallback<String>) connection -> {
            return connection.ping();
        });
    }

    /**
     * Adds connection configuration details to health status.
     *
     * @param healthDetails the health details map to add details to
     */
    private void addConnectionDetails(Map<String, Object> healthDetails) {
        Map<String, Object> connectionDetails = new HashMap<>();
        connectionDetails.put("host", redisProperties.getHost());
        connectionDetails.put("port", redisProperties.getPort());
        connectionDetails.put("database", redisProperties.getDatabase());
        connectionDetails.put("timeout", redisProperties.getTimeout().toMillis() + "ms");
        connectionDetails.put("passwordEnabled", redisProperties.isPasswordEnabled());
        connectionDetails.put("connectionUrl", redisProperties.getConnectionUrl());
        
        healthDetails.put("connection", connectionDetails);
    }

    /**
     * Adds Redis server information to health status.
     *
     * @param healthDetails the health details map to add details to
     */
    private void addRedisServerInfo(Map<String, Object> healthDetails) {
        try {
            Properties serverInfo = redisTemplate.execute((RedisCallback<Properties>) connection -> {
                return connection.info();
            });

            if (serverInfo != null) {
                Map<String, Object> serverDetails = new HashMap<>();
                serverDetails.put("version", serverInfo.getProperty("redis_version", "unknown"));
                serverDetails.put("mode", serverInfo.getProperty("redis_mode", "unknown"));
                serverDetails.put("os", serverInfo.getProperty("os", "unknown"));
                serverDetails.put("uptime", serverInfo.getProperty("uptime_in_seconds", "unknown") + "s");
                serverDetails.put("connectedClients", serverInfo.getProperty("connected_clients", "unknown"));
                
                healthDetails.put("server", serverDetails);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve Redis server info: {}", e.getMessage());
            healthDetails.put("server", "info_unavailable");
        }
    }

    /**
     * Adds connection pool metrics to health status.
     *
     * @param healthDetails the health details map to add details to
     */
    private void addConnectionPoolMetrics(Map<String, Object> healthDetails) {
        try {
            if (connectionFactory instanceof LettuceConnectionFactory) {
                LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
                
                Map<String, Object> poolMetrics = new HashMap<>();
                
                // Pool configuration
                RedisProperties.Lettuce.Pool poolConfig = redisProperties.getLettuce().getPool();
                poolMetrics.put("maxActive", poolConfig.getMaxActive());
                poolMetrics.put("maxIdle", poolConfig.getMaxIdle());
                poolMetrics.put("minIdle", poolConfig.getMinIdle());
                poolMetrics.put("maxWait", poolConfig.getMaxWait().toMillis() + "ms");
                
                // Connection factory status
                poolMetrics.put("validateConnections", lettuceFactory.getValidateConnection());
                poolMetrics.put("shareNativeConnection", lettuceFactory.getShareNativeConnection());
                
                healthDetails.put("connectionPool", poolMetrics);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve connection pool metrics: {}", e.getMessage());
            healthDetails.put("connectionPool", "metrics_unavailable");
        }
    }

    /**
     * Adds Redis memory usage information to health status.
     *
     * @param healthDetails the health details map to add details to
     */
    private void addMemoryUsageInfo(Map<String, Object> healthDetails) {
        try {
            Properties memoryInfo = redisTemplate.execute((RedisCallback<Properties>) connection -> {
                return connection.info("memory");
            });

            if (memoryInfo != null) {
                Map<String, Object> memoryDetails = new HashMap<>();
                
                // Memory usage
                String usedMemory = memoryInfo.getProperty("used_memory_human", "unknown");
                String usedMemoryPeak = memoryInfo.getProperty("used_memory_peak_human", "unknown");
                String totalSystemMemory = memoryInfo.getProperty("total_system_memory_human", "unknown");
                
                memoryDetails.put("usedMemory", usedMemory);
                memoryDetails.put("usedMemoryPeak", usedMemoryPeak);
                memoryDetails.put("totalSystemMemory", totalSystemMemory);
                
                // Memory fragmentation
                String fragRatio = memoryInfo.getProperty("mem_fragmentation_ratio", "unknown");
                memoryDetails.put("fragmentationRatio", fragRatio);
                
                // Key statistics
                String keyspaceHits = memoryInfo.getProperty("keyspace_hits", "0");
                String keyspaceMisses = memoryInfo.getProperty("keyspace_misses", "0");
                
                if (!"0".equals(keyspaceHits) || !"0".equals(keyspaceMisses)) {
                    long hits = Long.parseLong(keyspaceHits);
                    long misses = Long.parseLong(keyspaceMisses);
                    double hitRatio = hits + misses > 0 ? (double) hits / (hits + misses) * 100 : 0;
                    
                    memoryDetails.put("keyspaceHits", hits);
                    memoryDetails.put("keyspaceMisses", misses);
                    memoryDetails.put("hitRatio", String.format("%.2f%%", hitRatio));
                }
                
                healthDetails.put("memory", memoryDetails);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve Redis memory info: {}", e.getMessage());
            healthDetails.put("memory", "info_unavailable");
        }
    }

    /**
     * Adds application-level Redis metrics to health status.
     *
     * @param healthDetails the health details map to add details to
     */
    private void addApplicationMetrics(Map<String, Object> healthDetails) {
        try {
            // Update connection pool metrics before reporting
            metricsService.updateConnectionPoolMetrics();
            
            Map<String, Object> appMetrics = new HashMap<>();
            
            // Cache hit/miss metrics
            Map<String, Object> cacheMetrics = new HashMap<>();
            cacheMetrics.put("hitRatio", String.format("%.2f%%", metricsService.getCacheHitRatio() * 100));
            cacheMetrics.put("totalOperations", metricsService.getTotalCacheOperations());
            cacheMetrics.put("totalErrors", metricsService.getTotalCacheErrors());
            appMetrics.put("cache", cacheMetrics);
            
            // Response time metrics
            Map<String, Object> responseTimeMetrics = new HashMap<>();
            responseTimeMetrics.put("averageGetTime", String.format("%.2fms", metricsService.getAverageGetTime()));
            responseTimeMetrics.put("averageSetTime", String.format("%.2fms", metricsService.getAverageSetTime()));
            responseTimeMetrics.put("averageDeleteTime", String.format("%.2fms", metricsService.getAverageDeleteTime()));
            responseTimeMetrics.put("averageExistsTime", String.format("%.2fms", metricsService.getAverageExistsTime()));
            appMetrics.put("responseTimes", responseTimeMetrics);
            
            healthDetails.put("applicationMetrics", appMetrics);
            
        } catch (Exception e) {
            log.warn("Failed to retrieve application metrics: {}", e.getMessage());
            healthDetails.put("applicationMetrics", "metrics_unavailable");
        }
    }
}