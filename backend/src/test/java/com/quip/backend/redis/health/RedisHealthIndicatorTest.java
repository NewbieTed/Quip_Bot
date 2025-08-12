package com.quip.backend.redis.health;

import com.quip.backend.config.redis.RedisProperties;
import com.quip.backend.redis.metrics.RedisMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for RedisHealthIndicator.
 * All tests use mocks to avoid actual Redis connections.
 */
@ExtendWith(MockitoExtension.class)
class RedisHealthIndicatorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private LettuceConnectionFactory lettuceConnectionFactory;

    @Mock
    private RedisMetricsService metricsService;

    private RedisProperties redisProperties;
    private RedisHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        redisProperties = new RedisProperties();
        redisProperties.setHost("localhost");
        redisProperties.setPort(6379);
        redisProperties.setDatabase(0);
        redisProperties.setTimeout(Duration.ofMillis(2000));
        redisProperties.setPassword("");

        // Configure pool properties
        RedisProperties.Lettuce.Pool pool = new RedisProperties.Lettuce.Pool();
        pool.setMaxActive(8);
        pool.setMaxIdle(8);
        pool.setMinIdle(0);
        pool.setMaxWait(Duration.ofMillis(-1));
        redisProperties.getLettuce().setPool(pool);

        healthIndicator = new RedisHealthIndicator(redisTemplate, connectionFactory, redisProperties, metricsService);
    }

    @Test
    void checkHealth_WhenRedisIsHealthy_ShouldReturnUpStatus() {
        // Given
        Properties serverInfo = new Properties();
        serverInfo.setProperty("redis_version", "6.2.0");
        serverInfo.setProperty("redis_mode", "standalone");
        serverInfo.setProperty("os", "Linux");
        serverInfo.setProperty("uptime_in_seconds", "3600");
        serverInfo.setProperty("connected_clients", "5");

        Properties memoryInfo = new Properties();
        memoryInfo.setProperty("used_memory_human", "1.5M");
        memoryInfo.setProperty("used_memory_peak_human", "2.0M");
        memoryInfo.setProperty("total_system_memory_human", "8.0G");
        memoryInfo.setProperty("mem_fragmentation_ratio", "1.2");
        memoryInfo.setProperty("keyspace_hits", "100");
        memoryInfo.setProperty("keyspace_misses", "20");

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("PONG")
                .thenReturn(serverInfo)
                .thenReturn(memoryInfo);

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health).containsKey("ping");
        assertThat(health).containsKey("responseTime");
        assertThat(health).containsKey("timestamp");
        assertThat(health).containsKey("connection");
        assertThat(health).containsKey("server");
        assertThat(health).containsKey("memory");

        // Verify ping response
        assertThat(health.get("ping")).isEqualTo("PONG");
        assertThat(health.get("status")).isEqualTo("UP");
    }

    @Test
    void checkHealth_WhenPingFails_ShouldReturnDownStatus() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("ERROR");

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("DOWN");
        assertThat(health).containsKey("error");
        assertThat(health).containsKey("response");
        assertThat(health).containsKey("timestamp");
        assertThat(health.get("error")).isEqualTo("Redis ping failed");
        assertThat(health.get("response")).isEqualTo("ERROR");
    }

    @Test
    void checkHealth_WhenExceptionOccurs_ShouldReturnDownStatus() {
        // Given
        RuntimeException exception = new RuntimeException("Connection failed");
        when(redisTemplate.execute(any(RedisCallback.class))).thenThrow(exception);

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("DOWN");
        assertThat(health).containsKey("error");
        assertThat(health).containsKey("exception");
        assertThat(health).containsKey("timestamp");
        assertThat(health.get("error")).isEqualTo("Connection failed");
        assertThat(health.get("exception")).isEqualTo("RuntimeException");
    }

    @Test
    void checkHealth_ShouldIncludeConnectionDetails() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health).containsKey("connection");
        @SuppressWarnings("unchecked")
        var connectionDetails = (java.util.Map<String, Object>) health.get("connection");
        
        assertThat(connectionDetails.get("host")).isEqualTo("localhost");
        assertThat(connectionDetails.get("port")).isEqualTo(6379);
        assertThat(connectionDetails.get("database")).isEqualTo(0);
        assertThat(connectionDetails.get("timeout")).isEqualTo("2000ms");
        assertThat(connectionDetails.get("passwordEnabled")).isEqualTo(false);
        assertThat(connectionDetails.get("connectionUrl")).isEqualTo("redis://localhost:6379/0");
    }

    @Test
    void checkHealth_WithPasswordEnabled_ShouldShowPasswordEnabled() {
        // Given
        redisProperties.setPassword("secret");
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        @SuppressWarnings("unchecked")
        var connectionDetails = (java.util.Map<String, Object>) health.get("connection");
        assertThat(connectionDetails.get("passwordEnabled")).isEqualTo(true);
    }

    @Test
    void checkHealth_ShouldIncludeServerInfo() {
        // Given
        Properties serverInfo = new Properties();
        serverInfo.setProperty("redis_version", "6.2.0");
        serverInfo.setProperty("redis_mode", "standalone");
        serverInfo.setProperty("os", "Linux");
        serverInfo.setProperty("uptime_in_seconds", "3600");
        serverInfo.setProperty("connected_clients", "5");

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("PONG")
                .thenReturn(serverInfo);

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health).containsKey("server");
        @SuppressWarnings("unchecked")
        var serverDetails = (java.util.Map<String, Object>) health.get("server");
        
        assertThat(serverDetails.get("version")).isEqualTo("6.2.0");
        assertThat(serverDetails.get("mode")).isEqualTo("standalone");
        assertThat(serverDetails.get("os")).isEqualTo("Linux");
        assertThat(serverDetails.get("uptime")).isEqualTo("3600s");
        assertThat(serverDetails.get("connectedClients")).isEqualTo("5");
    }

    @Test
    void checkHealth_ShouldIncludeMemoryInfo() {
        // Given
        Properties memoryInfo = new Properties();
        memoryInfo.setProperty("used_memory_human", "1.5M");
        memoryInfo.setProperty("used_memory_peak_human", "2.0M");
        memoryInfo.setProperty("total_system_memory_human", "8.0G");
        memoryInfo.setProperty("mem_fragmentation_ratio", "1.2");
        memoryInfo.setProperty("keyspace_hits", "100");
        memoryInfo.setProperty("keyspace_misses", "20");

        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("PONG")
                .thenReturn(null) // server info
                .thenReturn(memoryInfo);

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health).containsKey("memory");
        @SuppressWarnings("unchecked")
        var memoryDetails = (java.util.Map<String, Object>) health.get("memory");
        
        assertThat(memoryDetails.get("usedMemory")).isEqualTo("1.5M");
        assertThat(memoryDetails.get("usedMemoryPeak")).isEqualTo("2.0M");
        assertThat(memoryDetails.get("totalSystemMemory")).isEqualTo("8.0G");
        assertThat(memoryDetails.get("fragmentationRatio")).isEqualTo("1.2");
        assertThat(memoryDetails.get("keyspaceHits")).isEqualTo(100L);
        assertThat(memoryDetails.get("keyspaceMisses")).isEqualTo(20L);
        assertThat(memoryDetails.get("hitRatio")).isEqualTo("83.33%");
    }

    @Test
    void checkHealth_WithLettuceConnectionFactory_ShouldIncludePoolMetrics() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");
        when(lettuceConnectionFactory.getValidateConnection()).thenReturn(true);
        when(lettuceConnectionFactory.getShareNativeConnection()).thenReturn(false);

        healthIndicator = new RedisHealthIndicator(redisTemplate, lettuceConnectionFactory, redisProperties, metricsService);

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health).containsKey("connectionPool");
        @SuppressWarnings("unchecked")
        var poolDetails = (java.util.Map<String, Object>) health.get("connectionPool");
        
        assertThat(poolDetails.get("maxActive")).isEqualTo(8);
        assertThat(poolDetails.get("maxIdle")).isEqualTo(8);
        assertThat(poolDetails.get("minIdle")).isEqualTo(0);
        assertThat(poolDetails.get("maxWait")).isEqualTo("-1ms");
        assertThat(poolDetails.get("validateConnections")).isEqualTo(true);
        assertThat(poolDetails.get("shareNativeConnection")).isEqualTo(false);
    }

    @Test
    void checkHealth_WhenServerInfoFails_ShouldHandleGracefully() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("PONG")
                .thenThrow(new RuntimeException("Server info failed"));

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health).containsKey("server");
        assertThat(health.get("server")).isEqualTo("info_unavailable");
    }

    @Test
    void checkHealth_WhenMemoryInfoFails_ShouldHandleGracefully() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenReturn("PONG")
                .thenReturn(null) // server info
                .thenThrow(new RuntimeException("Memory info failed"));

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health).containsKey("memory");
        assertThat(health.get("memory")).isEqualTo("info_unavailable");
    }

    @Test
    void checkHealth_ShouldIncludeApplicationMetrics() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");
        when(metricsService.getCacheHitRatio()).thenReturn(0.75);
        when(metricsService.getTotalCacheOperations()).thenReturn(100L);
        when(metricsService.getTotalCacheErrors()).thenReturn(5L);
        when(metricsService.getAverageGetTime()).thenReturn(15.5);
        when(metricsService.getAverageSetTime()).thenReturn(12.3);
        when(metricsService.getAverageDeleteTime()).thenReturn(8.7);
        when(metricsService.getAverageExistsTime()).thenReturn(6.2);

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health).containsKey("applicationMetrics");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> appMetrics = (Map<String, Object>) health.get("applicationMetrics");
        
        // Verify cache metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> cacheMetrics = (Map<String, Object>) appMetrics.get("cache");
        assertThat(cacheMetrics.get("hitRatio")).isEqualTo("75.00%");
        assertThat(cacheMetrics.get("totalOperations")).isEqualTo(100L);
        assertThat(cacheMetrics.get("totalErrors")).isEqualTo(5L);
        
        // Verify response time metrics
        @SuppressWarnings("unchecked")
        Map<String, Object> responseTimeMetrics = (Map<String, Object>) appMetrics.get("responseTimes");
        assertThat(responseTimeMetrics.get("averageGetTime")).isEqualTo("15.50ms");
        assertThat(responseTimeMetrics.get("averageSetTime")).isEqualTo("12.30ms");
        assertThat(responseTimeMetrics.get("averageDeleteTime")).isEqualTo("8.70ms");
        assertThat(responseTimeMetrics.get("averageExistsTime")).isEqualTo("6.20ms");
        
        // Verify metrics service was called to update connection pool metrics
        verify(metricsService).updateConnectionPoolMetrics();
    }

    @Test
    void checkHealth_WhenApplicationMetricsFail_ShouldHandleGracefully() {
        // Given
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");
        doThrow(new RuntimeException("Metrics error")).when(metricsService).updateConnectionPoolMetrics();

        // When
        Map<String, Object> health = healthIndicator.checkHealth();

        // Then
        assertThat(health.get("status")).isEqualTo("UP");
        assertThat(health).containsKey("applicationMetrics");
        assertThat(health.get("applicationMetrics")).isEqualTo("metrics_unavailable");
    }
}