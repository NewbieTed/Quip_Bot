package com.quip.backend.redis.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and managing Redis metrics including cache hit/miss ratios,
 * response times, and connection pool monitoring.
 * <p>
 * This service integrates with Micrometer to provide comprehensive Redis metrics
 * that can be exposed through Spring Boot Actuator endpoints and monitoring systems.
 * </p>
 */
@Slf4j
@Service
public class RedisMetricsService {

    private final MeterRegistry meterRegistry;
    private final RedisConnectionFactory connectionFactory;

    // Cache hit/miss counters
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheErrorCounter;

    // Operation counters
    private final Counter getOperationCounter;
    private final Counter setOperationCounter;
    private final Counter deleteOperationCounter;
    private final Counter existsOperationCounter;

    // Response time timers
    private final Timer getOperationTimer;
    private final Timer setOperationTimer;
    private final Timer deleteOperationTimer;
    private final Timer existsOperationTimer;

    // Connection pool metrics
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong idleConnections = new AtomicLong(0);
    private final AtomicLong totalConnections = new AtomicLong(0);

    @Autowired
    public RedisMetricsService(MeterRegistry meterRegistry, RedisConnectionFactory connectionFactory) {
        this.meterRegistry = meterRegistry;
        this.connectionFactory = connectionFactory;

        // Initialize cache hit/miss counters
        this.cacheHitCounter = Counter.builder("redis.cache.hits")
                .description("Number of cache hits")
                .register(meterRegistry);

        this.cacheMissCounter = Counter.builder("redis.cache.misses")
                .description("Number of cache misses")
                .register(meterRegistry);

        this.cacheErrorCounter = Counter.builder("redis.cache.errors")
                .description("Number of cache operation errors")
                .register(meterRegistry);

        // Initialize operation counters
        this.getOperationCounter = Counter.builder("redis.operations.get")
                .description("Number of GET operations")
                .register(meterRegistry);

        this.setOperationCounter = Counter.builder("redis.operations.set")
                .description("Number of SET operations")
                .register(meterRegistry);

        this.deleteOperationCounter = Counter.builder("redis.operations.delete")
                .description("Number of DELETE operations")
                .register(meterRegistry);

        this.existsOperationCounter = Counter.builder("redis.operations.exists")
                .description("Number of EXISTS operations")
                .register(meterRegistry);

        // Initialize response time timers
        this.getOperationTimer = Timer.builder("redis.operations.get.duration")
                .description("Duration of GET operations")
                .register(meterRegistry);

        this.setOperationTimer = Timer.builder("redis.operations.set.duration")
                .description("Duration of SET operations")
                .register(meterRegistry);

        this.deleteOperationTimer = Timer.builder("redis.operations.delete.duration")
                .description("Duration of DELETE operations")
                .register(meterRegistry);

        this.existsOperationTimer = Timer.builder("redis.operations.exists.duration")
                .description("Duration of EXISTS operations")
                .register(meterRegistry);

        // Initialize connection pool gauges
        initializeConnectionPoolGauges();

        log.info("Redis metrics service initialized with {} counters and {} timers", 
                8, 4);
    }

    /**
     * Records a cache hit event.
     *
     * @param key the cache key that was hit
     */
    public void recordCacheHit(String key) {
        cacheHitCounter.increment();
        log.debug("Cache hit recorded for key: {}", key);
    }

    /**
     * Records a cache miss event.
     *
     * @param key the cache key that was missed
     */
    public void recordCacheMiss(String key) {
        cacheMissCounter.increment();
        log.debug("Cache miss recorded for key: {}", key);
    }

    /**
     * Records a cache error event.
     *
     * @param operation the operation that failed
     * @param error the error that occurred
     */
    public void recordCacheError(String operation, Throwable error) {
        cacheErrorCounter.increment();
        log.debug("Cache error recorded for operation: {} - error: {}", operation, error.getMessage());
    }

    /**
     * Records a GET operation and its execution time.
     *
     * @param duration the duration of the operation
     * @param success whether the operation was successful
     */
    public void recordGetOperation(Duration duration, boolean success) {
        getOperationCounter.increment();
        getOperationTimer.record(duration);
        log.debug("GET operation recorded - duration: {}ms, success: {}", 
                duration.toMillis(), success);
    }

    /**
     * Records a SET operation and its execution time.
     *
     * @param duration the duration of the operation
     * @param success whether the operation was successful
     */
    public void recordSetOperation(Duration duration, boolean success) {
        setOperationCounter.increment();
        setOperationTimer.record(duration);
        log.debug("SET operation recorded - duration: {}ms, success: {}", 
                duration.toMillis(), success);
    }

    /**
     * Records a DELETE operation and its execution time.
     *
     * @param duration the duration of the operation
     * @param success whether the operation was successful
     */
    public void recordDeleteOperation(Duration duration, boolean success) {
        deleteOperationCounter.increment();
        deleteOperationTimer.record(duration);
        log.debug("DELETE operation recorded - duration: {}ms, success: {}", 
                duration.toMillis(), success);
    }

    /**
     * Records an EXISTS operation and its execution time.
     *
     * @param duration the duration of the operation
     * @param success whether the operation was successful
     */
    public void recordExistsOperation(Duration duration, boolean success) {
        existsOperationCounter.increment();
        existsOperationTimer.record(duration);
        log.debug("EXISTS operation recorded - duration: {}ms, success: {}", 
                duration.toMillis(), success);
    }

    /**
     * Gets the current cache hit ratio as a percentage.
     *
     * @return cache hit ratio (0.0 to 1.0)
     */
    public double getCacheHitRatio() {
        double hits = cacheHitCounter.count();
        double misses = cacheMissCounter.count();
        double total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return hits / total;
    }

    /**
     * Gets the total number of cache operations.
     *
     * @return total cache operations
     */
    public long getTotalCacheOperations() {
        return (long) (cacheHitCounter.count() + cacheMissCounter.count());
    }

    /**
     * Gets the total number of cache errors.
     *
     * @return total cache errors
     */
    public long getTotalCacheErrors() {
        return (long) cacheErrorCounter.count();
    }

    /**
     * Gets the average response time for GET operations.
     *
     * @return average GET operation time in milliseconds
     */
    public double getAverageGetTime() {
        return getOperationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the average response time for SET operations.
     *
     * @return average SET operation time in milliseconds
     */
    public double getAverageSetTime() {
        return setOperationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the average response time for DELETE operations.
     *
     * @return average DELETE operation time in milliseconds
     */
    public double getAverageDeleteTime() {
        return deleteOperationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Gets the average response time for EXISTS operations.
     *
     * @return average EXISTS operation time in milliseconds
     */
    public double getAverageExistsTime() {
        return existsOperationTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Updates connection pool metrics.
     * This method should be called periodically to update connection pool statistics.
     */
    public void updateConnectionPoolMetrics() {
        try {
            if (connectionFactory instanceof LettuceConnectionFactory) {
                LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
                
                // Note: Lettuce doesn't expose detailed pool metrics directly
                // These are placeholder implementations that would need to be enhanced
                // with actual pool monitoring if detailed metrics are required
                
                // For now, we'll track basic connection factory status
                boolean isConnected = false;
                try {
                    isConnected = lettuceFactory.getConnection().isClosed() == false;
                    totalConnections.set(isConnected ? 1 : 0);
                    activeConnections.set(isConnected ? 1 : 0);
                    idleConnections.set(0);
                } catch (Exception e) {
                    // Connection check failed, assume disconnected
                    totalConnections.set(0);
                    activeConnections.set(0);
                    idleConnections.set(0);
                }
                
                log.debug("Connection pool metrics updated - connected: {}", isConnected);
            }
        } catch (Exception e) {
            log.warn("Failed to update connection pool metrics: {}", e.getMessage());
        }
    }

    /**
     * Initializes connection pool gauges for monitoring.
     */
    private void initializeConnectionPoolGauges() {
        Gauge.builder("redis.connections.active", this, RedisMetricsService::getActiveConnections)
                .description("Number of active Redis connections")
                .register(meterRegistry);

        Gauge.builder("redis.connections.idle", this, RedisMetricsService::getIdleConnections)
                .description("Number of idle Redis connections")
                .register(meterRegistry);

        Gauge.builder("redis.connections.total", this, RedisMetricsService::getTotalConnections)
                .description("Total number of Redis connections")
                .register(meterRegistry);

        Gauge.builder("redis.cache.hit.ratio", this, RedisMetricsService::getCacheHitRatio)
                .description("Cache hit ratio")
                .register(meterRegistry);

        log.info("Connection pool gauges initialized");
    }

    /**
     * Gets the number of active connections.
     *
     * @return number of active connections
     */
    private long getActiveConnections() {
        return activeConnections.get();
    }

    /**
     * Gets the number of idle connections.
     *
     * @return number of idle connections
     */
    private long getIdleConnections() {
        return idleConnections.get();
    }

    /**
     * Gets the total number of connections.
     *
     * @return total number of connections
     */
    private long getTotalConnections() {
        return totalConnections.get();
    }
}