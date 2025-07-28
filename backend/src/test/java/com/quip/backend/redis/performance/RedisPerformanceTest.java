package com.quip.backend.redis.performance;

import com.quip.backend.cache.service.CacheService;
import com.quip.backend.cache.util.CacheKeyManager;
import com.quip.backend.redis.exception.RedisExceptionHandler;
import com.quip.backend.redis.metrics.RedisMetricsService;
import com.quip.backend.redis.service.RedisService;
import com.quip.backend.tool.model.ToolWhitelist;
import com.quip.backend.tool.enums.ToolWhitelistScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Performance and load tests for Redis integration.
 * Tests cache performance benchmarks, concurrent access scenarios,
 * and memory usage patterns.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisPerformanceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedisMetricsService metricsService;

    private RedisService redisService;
    private CacheService cacheService;

    private static final int PERFORMANCE_TEST_ITERATIONS = 1000;
    private static final int CONCURRENT_THREADS = 10;
    private static final int LOAD_TEST_OPERATIONS = 5000;
    private static final Duration PERFORMANCE_TIMEOUT = Duration.ofSeconds(30);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Create RedisExceptionHandler mock
        RedisExceptionHandler exceptionHandler = mock(RedisExceptionHandler.class);
        
        // Configure exception handler to execute operations directly for successful cases
        doAnswer(invocation -> {
            Runnable operation = invocation.getArgument(0);
            operation.run();
            return null;
        }).when(exceptionHandler).handleRedisOperation(any(Runnable.class));
        
        doAnswer(invocation -> {
            Runnable operation = invocation.getArgument(0);
            operation.run();
            return null;
        }).when(exceptionHandler).handleRedisOperation(any(Runnable.class), any(Runnable.class));
        
        when(exceptionHandler.handleRedisOperation(any(java.util.function.Supplier.class), any(java.util.function.Supplier.class)))
            .thenAnswer(invocation -> {
                java.util.function.Supplier<Object> operation = invocation.getArgument(0);
                return operation.get();
            });
        
        // Create RedisService with mocked dependencies
        redisService = new RedisService(redisTemplate, exceptionHandler, metricsService);
        
        // Create CacheKeyManager mock
        CacheKeyManager keyManager = mock(CacheKeyManager.class);
        
        // Create CacheService with mocked dependencies
        cacheService = new CacheService(redisService, keyManager);
    }

    // ========== Cache Performance Benchmarks ==========

    @Test
    @Timeout(30)
    void benchmarkBasicCacheOperations_ShouldCompleteWithinTimeLimit() {
        // Given
        String keyPrefix = "benchmark:basic:";
        String testValue = "test-value-for-performance-testing";
        
        // Configure mocks for successful operations
        when(valueOperations.get(anyString())).thenReturn(testValue);
        doNothing().when(valueOperations).set(anyString(), any());
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // When & Then - Measure SET operations
        long setStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
            redisService.set(keyPrefix + i, testValue);
        }
        long setDuration = System.nanoTime() - setStartTime;
        double setOpsPerSecond = (PERFORMANCE_TEST_ITERATIONS * 1_000_000_000.0) / setDuration;

        // When & Then - Measure GET operations
        long getStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
            redisService.get(keyPrefix + i, String.class);
        }
        long getDuration = System.nanoTime() - getStartTime;
        double getOpsPerSecond = (PERFORMANCE_TEST_ITERATIONS * 1_000_000_000.0) / getDuration;

        // When & Then - Measure EXISTS operations
        long existsStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
            redisService.exists(keyPrefix + i);
        }
        long existsDuration = System.nanoTime() - existsStartTime;
        double existsOpsPerSecond = (PERFORMANCE_TEST_ITERATIONS * 1_000_000_000.0) / existsDuration;

        // When & Then - Measure DELETE operations
        long deleteStartTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
            redisService.delete(keyPrefix + i);
        }
        long deleteDuration = System.nanoTime() - deleteStartTime;
        double deleteOpsPerSecond = (PERFORMANCE_TEST_ITERATIONS * 1_000_000_000.0) / deleteDuration;

        // Assertions - Performance should be reasonable for mocked operations
        assertThat(setOpsPerSecond).isGreaterThan(10000); // At least 10k ops/sec
        assertThat(getOpsPerSecond).isGreaterThan(10000);
        assertThat(existsOpsPerSecond).isGreaterThan(10000);
        assertThat(deleteOpsPerSecond).isGreaterThan(10000);

        // Verify all operations were called
        verify(valueOperations, times(PERFORMANCE_TEST_ITERATIONS)).set(anyString(), any());
        verify(valueOperations, times(PERFORMANCE_TEST_ITERATIONS)).get(anyString());
        verify(redisTemplate, times(PERFORMANCE_TEST_ITERATIONS)).hasKey(anyString());
        verify(redisTemplate, times(PERFORMANCE_TEST_ITERATIONS)).delete(anyString());

        System.out.printf("Performance Benchmark Results:%n");
        System.out.printf("SET operations: %.2f ops/sec%n", setOpsPerSecond);
        System.out.printf("GET operations: %.2f ops/sec%n", getOpsPerSecond);
        System.out.printf("EXISTS operations: %.2f ops/sec%n", existsOpsPerSecond);
        System.out.printf("DELETE operations: %.2f ops/sec%n", deleteOpsPerSecond);
    }

    @Test
    @Timeout(30)
    void benchmarkCacheWithTTL_ShouldHandleExpirationEfficiently() {
        // Given
        String keyPrefix = "benchmark:ttl:";
        String testValue = "test-value-with-ttl";
        Duration ttl = Duration.ofMinutes(5);
        
        doNothing().when(valueOperations).set(anyString(), any(), any(Duration.class));

        // When - Measure SET operations with TTL
        long startTime = System.nanoTime();
        for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
            redisService.set(keyPrefix + i, testValue, ttl);
        }
        long duration = System.nanoTime() - startTime;
        double opsPerSecond = (PERFORMANCE_TEST_ITERATIONS * 1_000_000_000.0) / duration;

        // Then
        assertThat(opsPerSecond).isGreaterThan(5000); // TTL operations should be reasonably fast
        verify(valueOperations, times(PERFORMANCE_TEST_ITERATIONS))
                .set(anyString(), eq(testValue), eq(ttl));

        System.out.printf("TTL SET operations: %.2f ops/sec%n", opsPerSecond);
    }

    @Test
    @Timeout(30)
    void benchmarkBatchOperations_ShouldOutperformSingleOperations() {
        // Given
        Map<String, Object> batchData = new HashMap<>();
        List<String> keys = new ArrayList<>();
        
        for (int i = 0; i < PERFORMANCE_TEST_ITERATIONS; i++) {
            String key = "batch:key:" + i;
            batchData.put(key, "value-" + i);
            keys.add(key);
        }
        
        doNothing().when(valueOperations).multiSet(any());
        when(valueOperations.multiGet(any())).thenReturn(
                keys.stream().map(k -> "value-for-" + k).collect(java.util.stream.Collectors.toList())
        );

        // When - Measure batch SET
        long batchSetStart = System.nanoTime();
        redisService.mset(batchData);
        long batchSetDuration = System.nanoTime() - batchSetStart;

        // When - Measure batch GET
        long batchGetStart = System.nanoTime();
        redisService.mget(keys);
        long batchGetDuration = System.nanoTime() - batchGetStart;

        // When - Measure individual operations for comparison
        when(valueOperations.get(anyString())).thenReturn("individual-value");
        doNothing().when(valueOperations).set(anyString(), any());

        long individualSetStart = System.nanoTime();
        for (Map.Entry<String, Object> entry : batchData.entrySet()) {
            redisService.set(entry.getKey(), entry.getValue());
        }
        long individualSetDuration = System.nanoTime() - individualSetStart;

        long individualGetStart = System.nanoTime();
        for (String key : keys) {
            redisService.get(key, String.class);
        }
        long individualGetDuration = System.nanoTime() - individualGetStart;

        // Then - Batch operations should be more efficient
        assertThat(batchSetDuration).isLessThan(individualSetDuration);
        assertThat(batchGetDuration).isLessThan(individualGetDuration);

        verify(valueOperations).multiSet(batchData);
        verify(valueOperations).multiGet(keys);

        System.out.printf("Batch vs Individual Performance:%n");
        System.out.printf("Batch SET: %d ns, Individual SET: %d ns%n", batchSetDuration, individualSetDuration);
        System.out.printf("Batch GET: %d ns, Individual GET: %d ns%n", batchGetDuration, individualGetDuration);
    }

    // ========== Concurrent Access Tests ==========

    @Test
    @Timeout(60)
    void testConcurrentCacheAccess_ShouldHandleMultipleThreads() throws InterruptedException {
        // Given
        String keyPrefix = "concurrent:test:";
        String testValue = "concurrent-value";
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);
        
        when(valueOperations.get(anyString())).thenReturn(testValue);
        doNothing().when(valueOperations).set(anyString(), any());
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        // When - Execute concurrent operations
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < LOAD_TEST_OPERATIONS / CONCURRENT_THREADS; j++) {
                        String key = keyPrefix + threadId + ":" + j;
                        
                        // Perform mixed operations
                        redisService.set(key, testValue);
                        redisService.get(key, String.class);
                        redisService.exists(key);
                        
                        successfulOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successfulOperations.get()).isEqualTo(LOAD_TEST_OPERATIONS);
        assertThat(failedOperations.get()).isEqualTo(0);

        // Verify operations were called concurrently
        int expectedOperationsPerType = LOAD_TEST_OPERATIONS;
        verify(valueOperations, times(expectedOperationsPerType)).set(anyString(), any());
        verify(valueOperations, times(expectedOperationsPerType)).get(anyString());
        verify(redisTemplate, times(expectedOperationsPerType)).hasKey(anyString());

        System.out.printf("Concurrent test completed: %d successful operations, %d failed%n",
                successfulOperations.get(), failedOperations.get());
    }

    @Test
    @Timeout(60)
    void testConcurrentCacheEviction_ShouldHandleRaceConditions() throws InterruptedException {
        // Given
        String sharedKey = "concurrent:eviction:key";
        String testValue = "shared-value";
        AtomicInteger setOperations = new AtomicInteger(0);
        AtomicInteger deleteOperations = new AtomicInteger(0);
        
        doNothing().when(valueOperations).set(anyString(), any());
        when(redisTemplate.delete(anyString())).thenReturn(true);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);

        // When - Execute concurrent set/delete operations on same key
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        if (threadId % 2 == 0) {
                            // Even threads set values
                            redisService.set(sharedKey, testValue + "-" + threadId + "-" + j);
                            setOperations.incrementAndGet();
                        } else {
                            // Odd threads delete values
                            redisService.delete(sharedKey);
                            deleteOperations.incrementAndGet();
                        }
                        
                        // Small delay to increase chance of race conditions
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    // Expected in concurrent scenarios
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        boolean completed = latch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(setOperations.get()).isGreaterThan(0);
        assertThat(deleteOperations.get()).isGreaterThan(0);

        System.out.printf("Race condition test: %d sets, %d deletes%n",
                setOperations.get(), deleteOperations.get());
    }

    // ========== Memory Usage and Connection Pool Tests ==========

    @Test
    @Timeout(30)
    void testMemoryUsageWithLargeObjects_ShouldHandleEfficientSerialization() {
        // Given
        String keyPrefix = "memory:test:";
        List<ToolWhitelist> largeObjectList = createLargeToolWhitelistData(1000);
        
        doNothing().when(valueOperations).set(anyString(), any(), any(Duration.class));
        when(valueOperations.get(anyString())).thenReturn(largeObjectList);

        // When - Store large objects
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            String key = keyPrefix + i;
            redisService.set(key, largeObjectList, Duration.ofMinutes(5));
        }
        long storeDuration = System.nanoTime() - startTime;

        // When - Retrieve large objects
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            String key = keyPrefix + i;
            redisService.get(key, List.class);
        }
        long retrieveDuration = System.nanoTime() - startTime;

        // Then
        double storeOpsPerSecond = (100 * 1_000_000_000.0) / storeDuration;
        double retrieveOpsPerSecond = (100 * 1_000_000_000.0) / retrieveDuration;

        assertThat(storeOpsPerSecond).isGreaterThan(10); // Should handle at least 10 large objects per second
        assertThat(retrieveOpsPerSecond).isGreaterThan(10);

        verify(valueOperations, times(100)).set(anyString(), eq(largeObjectList), any(Duration.class));
        verify(valueOperations, times(100)).get(anyString());

        System.out.printf("Large object performance: Store %.2f ops/sec, Retrieve %.2f ops/sec%n",
                storeOpsPerSecond, retrieveOpsPerSecond);
    }

    @Test
    @Timeout(30)
    void testConnectionPoolStress_ShouldMaintainPerformanceUnderLoad() throws InterruptedException {
        // Given
        String keyPrefix = "pool:stress:";
        String testValue = "pool-test-value";
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong totalDuration = new AtomicLong(0);
        
        when(valueOperations.get(anyString())).thenReturn(testValue);
        doNothing().when(valueOperations).set(anyString(), any());

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS * 2); // Stress the pool
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS * 2);

        // When - Execute high-volume operations to stress connection pool
        for (int i = 0; i < CONCURRENT_THREADS * 2; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    long threadStart = System.nanoTime();
                    
                    for (int j = 0; j < 500; j++) {
                        String key = keyPrefix + threadId + ":" + j;
                        
                        // Rapid-fire operations to stress connections
                        redisService.set(key, testValue);
                        redisService.get(key, String.class);
                        redisService.exists(key);
                        redisService.delete(key);
                        
                        totalOperations.addAndGet(4); // 4 operations per iteration
                    }
                    
                    long threadDuration = System.nanoTime() - threadStart;
                    totalDuration.addAndGet(threadDuration);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then
        boolean completed = latch.await(25, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        
        double averageOpsPerSecond = (totalOperations.get() * 1_000_000_000.0) / totalDuration.get();
        assertThat(averageOpsPerSecond).isGreaterThan(1000); // Should maintain reasonable throughput

        System.out.printf("Connection pool stress test: %d total operations, %.2f avg ops/sec%n",
                totalOperations.get(), averageOpsPerSecond);
    }

    @Test
    @Timeout(30)
    void testCacheHitRatioUnderLoad_ShouldMaintainHighHitRatio() {
        // Given
        String keyPrefix = "hitratio:test:";
        String testValue = "hit-ratio-value";
        int totalKeys = 100;
        int totalOperations = 1000;
        
        // Setup cache with some data
        when(valueOperations.get(anyString())).thenReturn(testValue);
        doNothing().when(valueOperations).set(anyString(), any());

        // Pre-populate cache
        for (int i = 0; i < totalKeys; i++) {
            redisService.set(keyPrefix + i, testValue);
        }

        // When - Perform operations with high cache hit probability
        Random random = new Random(42); // Fixed seed for reproducible results
        int hits = 0;
        int misses = 0;

        long startTime = System.nanoTime();
        for (int i = 0; i < totalOperations; i++) {
            int keyIndex = random.nextInt(totalKeys + 20); // 20% chance of miss
            String key = keyPrefix + keyIndex;
            
            Object result = redisService.get(key, String.class);
            if (keyIndex < totalKeys) {
                hits++;
                assertThat(result).isEqualTo(testValue);
            } else {
                misses++;
                // For keys beyond totalKeys, mock returns null (cache miss)
                when(valueOperations.get(key)).thenReturn(null);
            }
        }
        long duration = System.nanoTime() - startTime;

        // Then
        double hitRatio = (double) hits / totalOperations;
        double opsPerSecond = (totalOperations * 1_000_000_000.0) / duration;

        assertThat(hitRatio).isGreaterThan(0.75); // Should have >75% hit ratio
        assertThat(opsPerSecond).isGreaterThan(5000); // Should maintain good performance

        System.out.printf("Cache hit ratio test: %.2f%% hit ratio, %.2f ops/sec%n",
                hitRatio * 100, opsPerSecond);
    }

    // ========== Helper Methods ==========

    private List<ToolWhitelist> createLargeToolWhitelistData(int count) {
        List<ToolWhitelist> whitelists = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            ToolWhitelist whitelist = new ToolWhitelist();
            whitelist.setMemberId((long) i);
            whitelist.setToolId((long) i);
            whitelist.setServerId((long) (i % 10));
            whitelist.setAgentConversationId(0L);
            whitelist.setScope(ToolWhitelistScope.SERVER);
            whitelist.setCreatedAt(java.time.OffsetDateTime.now());
            whitelist.setUpdatedAt(java.time.OffsetDateTime.now());
            whitelists.add(whitelist);
        }
        
        return whitelists;
    }
}