package com.quip.backend.redis.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RedisMetricsService.
 */
@ExtendWith(MockitoExtension.class)
class RedisMetricsServiceTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private LettuceConnectionFactory lettuceConnectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private MeterRegistry meterRegistry;
    private RedisMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new RedisMetricsService(meterRegistry, connectionFactory);
    }

    @Test
    void shouldInitializeMetricsCorrectly() {
        // Verify that all counters are initialized
        assertThat(meterRegistry.find("redis.cache.hits").counter()).isNotNull();
        assertThat(meterRegistry.find("redis.cache.misses").counter()).isNotNull();
        assertThat(meterRegistry.find("redis.cache.errors").counter()).isNotNull();
        
        // Verify that all operation counters are initialized
        assertThat(meterRegistry.find("redis.operations.get").counter()).isNotNull();
        assertThat(meterRegistry.find("redis.operations.set").counter()).isNotNull();
        assertThat(meterRegistry.find("redis.operations.delete").counter()).isNotNull();
        assertThat(meterRegistry.find("redis.operations.exists").counter()).isNotNull();
        
        // Verify that all timers are initialized
        assertThat(meterRegistry.find("redis.operations.get.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("redis.operations.set.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("redis.operations.delete.duration").timer()).isNotNull();
        assertThat(meterRegistry.find("redis.operations.exists.duration").timer()).isNotNull();
        
        // Verify that all gauges are initialized
        assertThat(meterRegistry.find("redis.connections.active").gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.connections.idle").gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.connections.total").gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.cache.hit.ratio").gauge()).isNotNull();
    }

    @Test
    void shouldRecordCacheHit() {
        // Given
        String key = "test:key";
        
        // When
        metricsService.recordCacheHit(key);
        
        // Then
        Counter hitCounter = meterRegistry.find("redis.cache.hits").counter();
        assertThat(hitCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordCacheMiss() {
        // Given
        String key = "test:key";
        
        // When
        metricsService.recordCacheMiss(key);
        
        // Then
        Counter missCounter = meterRegistry.find("redis.cache.misses").counter();
        assertThat(missCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordCacheError() {
        // Given
        String operation = "get";
        Exception error = new RuntimeException("Redis connection failed");
        
        // When
        metricsService.recordCacheError(operation, error);
        
        // Then
        Counter errorCounter = meterRegistry.find("redis.cache.errors").counter();
        assertThat(errorCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordGetOperation() {
        // Given
        Duration duration = Duration.ofMillis(50);
        boolean success = true;
        
        // When
        metricsService.recordGetOperation(duration, success);
        
        // Then
        Counter getCounter = meterRegistry.find("redis.operations.get").counter();
        Timer getTimer = meterRegistry.find("redis.operations.get.duration").timer();
        
        assertThat(getCounter.count()).isEqualTo(1.0);
        assertThat(getTimer.count()).isEqualTo(1);
        assertThat(getTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(50.0);
    }

    @Test
    void shouldRecordSetOperation() {
        // Given
        Duration duration = Duration.ofMillis(30);
        boolean success = true;
        
        // When
        metricsService.recordSetOperation(duration, success);
        
        // Then
        Counter setCounter = meterRegistry.find("redis.operations.set").counter();
        Timer setTimer = meterRegistry.find("redis.operations.set.duration").timer();
        
        assertThat(setCounter.count()).isEqualTo(1.0);
        assertThat(setTimer.count()).isEqualTo(1);
        assertThat(setTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(30.0);
    }

    @Test
    void shouldRecordDeleteOperation() {
        // Given
        Duration duration = Duration.ofMillis(20);
        boolean success = true;
        
        // When
        metricsService.recordDeleteOperation(duration, success);
        
        // Then
        Counter deleteCounter = meterRegistry.find("redis.operations.delete").counter();
        Timer deleteTimer = meterRegistry.find("redis.operations.delete.duration").timer();
        
        assertThat(deleteCounter.count()).isEqualTo(1.0);
        assertThat(deleteTimer.count()).isEqualTo(1);
        assertThat(deleteTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(20.0);
    }

    @Test
    void shouldRecordExistsOperation() {
        // Given
        Duration duration = Duration.ofMillis(10);
        boolean success = true;
        
        // When
        metricsService.recordExistsOperation(duration, success);
        
        // Then
        Counter existsCounter = meterRegistry.find("redis.operations.exists").counter();
        Timer existsTimer = meterRegistry.find("redis.operations.exists.duration").timer();
        
        assertThat(existsCounter.count()).isEqualTo(1.0);
        assertThat(existsTimer.count()).isEqualTo(1);
        assertThat(existsTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(10.0);
    }

    @Test
    void shouldCalculateCacheHitRatio() {
        // Given - record some hits and misses
        metricsService.recordCacheHit("key1");
        metricsService.recordCacheHit("key2");
        metricsService.recordCacheHit("key3");
        metricsService.recordCacheMiss("key4");
        
        // When
        double hitRatio = metricsService.getCacheHitRatio();
        
        // Then - 3 hits out of 4 total = 0.75
        assertThat(hitRatio).isEqualTo(0.75);
    }

    @Test
    void shouldReturnZeroHitRatioWhenNoOperations() {
        // When
        double hitRatio = metricsService.getCacheHitRatio();
        
        // Then
        assertThat(hitRatio).isEqualTo(0.0);
    }

    @Test
    void shouldGetTotalCacheOperations() {
        // Given
        metricsService.recordCacheHit("key1");
        metricsService.recordCacheHit("key2");
        metricsService.recordCacheMiss("key3");
        
        // When
        long totalOperations = metricsService.getTotalCacheOperations();
        
        // Then
        assertThat(totalOperations).isEqualTo(3);
    }

    @Test
    void shouldGetTotalCacheErrors() {
        // Given
        metricsService.recordCacheError("get", new RuntimeException("Error 1"));
        metricsService.recordCacheError("set", new RuntimeException("Error 2"));
        
        // When
        long totalErrors = metricsService.getTotalCacheErrors();
        
        // Then
        assertThat(totalErrors).isEqualTo(2);
    }

    @Test
    void shouldCalculateAverageGetTime() {
        // Given
        metricsService.recordGetOperation(Duration.ofMillis(10), true);
        metricsService.recordGetOperation(Duration.ofMillis(20), true);
        metricsService.recordGetOperation(Duration.ofMillis(30), true);
        
        // When
        double averageTime = metricsService.getAverageGetTime();
        
        // Then - average of 10, 20, 30 = 20
        assertThat(averageTime).isEqualTo(20.0);
    }

    @Test
    void shouldCalculateAverageSetTime() {
        // Given
        metricsService.recordSetOperation(Duration.ofMillis(15), true);
        metricsService.recordSetOperation(Duration.ofMillis(25), true);
        
        // When
        double averageTime = metricsService.getAverageSetTime();
        
        // Then - average of 15, 25 = 20
        assertThat(averageTime).isEqualTo(20.0);
    }

    @Test
    void shouldCalculateAverageDeleteTime() {
        // Given
        metricsService.recordDeleteOperation(Duration.ofMillis(5), true);
        metricsService.recordDeleteOperation(Duration.ofMillis(15), true);
        
        // When
        double averageTime = metricsService.getAverageDeleteTime();
        
        // Then - average of 5, 15 = 10
        assertThat(averageTime).isEqualTo(10.0);
    }

    @Test
    void shouldCalculateAverageExistsTime() {
        // Given
        metricsService.recordExistsOperation(Duration.ofMillis(8), true);
        metricsService.recordExistsOperation(Duration.ofMillis(12), true);
        
        // When
        double averageTime = metricsService.getAverageExistsTime();
        
        // Then - average of 8, 12 = 10
        assertThat(averageTime).isEqualTo(10.0);
    }

    @Test
    void shouldUpdateConnectionPoolMetricsWithLettuceFactory() {
        // Given
        when(lettuceConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.isClosed()).thenReturn(false);
        
        // Create service with Lettuce factory
        metricsService = new RedisMetricsService(meterRegistry, lettuceConnectionFactory);
        
        // When
        metricsService.updateConnectionPoolMetrics();
        
        // Then - verify gauges are updated (basic test since Lettuce doesn't expose detailed metrics)
        Gauge activeGauge = meterRegistry.find("redis.connections.active").gauge();
        Gauge totalGauge = meterRegistry.find("redis.connections.total").gauge();
        
        assertThat(activeGauge).isNotNull();
        assertThat(totalGauge).isNotNull();
        // Note: The actual values depend on the connection state, so we just verify the gauges exist
    }

    @Test
    void shouldHandleConnectionPoolMetricsUpdateFailure() {
        // When/Then - should not throw exception even if connection factory is not Lettuce
        metricsService.updateConnectionPoolMetrics();
        
        // Verify that gauges still exist
        assertThat(meterRegistry.find("redis.connections.active").gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.connections.idle").gauge()).isNotNull();
        assertThat(meterRegistry.find("redis.connections.total").gauge()).isNotNull();
    }

    @Test
    void shouldReturnZeroForAverageTimesWhenNoOperations() {
        // When/Then
        assertThat(metricsService.getAverageGetTime()).isEqualTo(0.0);
        assertThat(metricsService.getAverageSetTime()).isEqualTo(0.0);
        assertThat(metricsService.getAverageDeleteTime()).isEqualTo(0.0);
        assertThat(metricsService.getAverageExistsTime()).isEqualTo(0.0);
    }

    @Test
    void shouldTrackMultipleOperationsCorrectly() {
        // Given - perform multiple operations
        metricsService.recordGetOperation(Duration.ofMillis(10), true);
        metricsService.recordSetOperation(Duration.ofMillis(20), true);
        metricsService.recordDeleteOperation(Duration.ofMillis(15), false);
        metricsService.recordExistsOperation(Duration.ofMillis(5), true);
        
        metricsService.recordCacheHit("key1");
        metricsService.recordCacheMiss("key2");
        metricsService.recordCacheError("get", new RuntimeException("Test error"));
        
        // Then - verify all metrics are tracked
        assertThat(meterRegistry.find("redis.operations.get").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("redis.operations.set").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("redis.operations.delete").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("redis.operations.exists").counter().count()).isEqualTo(1.0);
        
        assertThat(meterRegistry.find("redis.cache.hits").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("redis.cache.misses").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.find("redis.cache.errors").counter().count()).isEqualTo(1.0);
        
        assertThat(metricsService.getCacheHitRatio()).isEqualTo(0.5); // 1 hit out of 2 total
        assertThat(metricsService.getTotalCacheOperations()).isEqualTo(2);
        assertThat(metricsService.getTotalCacheErrors()).isEqualTo(1);
    }
}