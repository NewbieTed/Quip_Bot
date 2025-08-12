package com.quip.backend.redis.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Exception handler for Redis operations providing graceful error handling,
 * circuit breaker pattern, and fallback mechanisms.
 * <p>
 * This handler ensures that Redis failures don't crash the application and
 * provides fallback mechanisms when Redis is unavailable.
 * </p>
 */
@Slf4j
@Component
public class RedisExceptionHandler {

    private static final int CIRCUIT_BREAKER_FAILURE_THRESHOLD = 5;
    private static final Duration CIRCUIT_BREAKER_TIMEOUT = Duration.ofMinutes(1);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofMillis(100);

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>();
    private volatile CircuitBreakerState circuitState = CircuitBreakerState.CLOSED;

    /**
     * Circuit breaker states
     */
    private enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, failing fast
        HALF_OPEN  // Testing if service is back
    }

    /**
     * Handles Redis operations with fallback support for operations that return a value.
     *
     * @param operation the Redis operation to execute
     * @param fallback  the fallback operation to execute if Redis fails
     * @param <T>       the return type
     * @return the result from either the Redis operation or fallback
     */
    public <T> T handleRedisOperation(Supplier<T> operation, Supplier<T> fallback) {
        if (isCircuitOpen()) {
            log.debug("Circuit breaker is open, using fallback");
            return executeFallback(fallback);
        }

        try {
            T result = executeWithRetry(operation);
            onOperationSuccess();
            return result;
        } catch (Exception e) {
            onOperationFailure(e);
            return executeFallback(fallback);
        }
    }

    /**
     * Handles Redis operations with fallback support for void operations.
     *
     * @param operation the Redis operation to execute
     * @param fallback  the fallback operation to execute if Redis fails (optional)
     */
    public void handleRedisOperation(Runnable operation, Runnable fallback) {
        if (isCircuitOpen()) {
            log.debug("Circuit breaker is open, using fallback");
            executeFallback(fallback);
            return;
        }

        try {
            executeWithRetry(() -> {
                operation.run();
                return null;
            });
            onOperationSuccess();
        } catch (Exception e) {
            onOperationFailure(e);
            executeFallback(fallback);
        }
    }

    /**
     * Handles Redis operations without fallback for void operations.
     *
     * @param operation the Redis operation to execute
     */
    public void handleRedisOperation(Runnable operation) {
        handleRedisOperation(operation, null);
    }

    /**
     * Executes an operation with retry logic.
     *
     * @param operation the operation to execute
     * @param <T>       the return type
     * @return the result of the operation
     * @throws Exception if all retry attempts fail
     */
    private <T> T executeWithRetry(Supplier<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                
                if (!shouldRetry(e) || attempt == MAX_RETRY_ATTEMPTS) {
                    throw e;
                }
                
                Duration delay = calculateRetryDelay(attempt);
                log.debug("Redis operation failed (attempt {}/{}), retrying in {}ms", 
                         attempt, MAX_RETRY_ATTEMPTS, delay.toMillis(), e);
                
                try {
                    Thread.sleep(delay.toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        throw lastException;
    }

    /**
     * Executes a fallback operation safely.
     *
     * @param fallback the fallback operation
     * @param <T>      the return type
     * @return the result of the fallback operation, or null if fallback is null or fails
     */
    private <T> T executeFallback(Supplier<T> fallback) {
        if (fallback == null) {
            return null;
        }
        
        try {
            return fallback.get();
        } catch (Exception e) {
            log.error("Fallback operation failed", e);
            return null;
        }
    }

    /**
     * Executes a fallback operation safely for void operations.
     *
     * @param fallback the fallback operation
     */
    private void executeFallback(Runnable fallback) {
        if (fallback == null) {
            return;
        }
        
        try {
            fallback.run();
        } catch (Exception e) {
            log.error("Fallback operation failed", e);
        }
    }

    /**
     * Checks if the circuit breaker is open.
     *
     * @return true if circuit is open, false otherwise
     */
    private boolean isCircuitOpen() {
        if (circuitState == CircuitBreakerState.CLOSED) {
            return false;
        }
        
        if (circuitState == CircuitBreakerState.OPEN) {
            Instant lastFailure = lastFailureTime.get();
            if (lastFailure != null && 
                Instant.now().isAfter(lastFailure.plus(CIRCUIT_BREAKER_TIMEOUT))) {
                // Transition to half-open to test the service
                circuitState = CircuitBreakerState.HALF_OPEN;
                log.info("Circuit breaker transitioning to HALF_OPEN state");
                return false;
            }
            return true;
        }
        
        // HALF_OPEN state - allow one request through
        return false;
    }

    /**
     * Handles successful operation execution.
     */
    private void onOperationSuccess() {
        if (circuitState != CircuitBreakerState.CLOSED) {
            log.info("Redis operation successful, closing circuit breaker");
            circuitState = CircuitBreakerState.CLOSED;
            failureCount.set(0);
            lastFailureTime.set(null);
        }
    }

    /**
     * Handles failed operation execution.
     *
     * @param exception the exception that occurred
     */
    private void onOperationFailure(Exception exception) {
        logRedisError(exception);
        
        int currentFailures = failureCount.incrementAndGet();
        lastFailureTime.set(Instant.now());
        
        if (currentFailures >= CIRCUIT_BREAKER_FAILURE_THRESHOLD && 
            circuitState == CircuitBreakerState.CLOSED) {
            circuitState = CircuitBreakerState.OPEN;
            log.warn("Circuit breaker opened after {} failures. Will retry after {}", 
                    currentFailures, CIRCUIT_BREAKER_TIMEOUT);
        } else if (circuitState == CircuitBreakerState.HALF_OPEN) {
            circuitState = CircuitBreakerState.OPEN;
            log.warn("Circuit breaker reopened after failed test in HALF_OPEN state");
        }
    }

    /**
     * Logs Redis errors with appropriate level and context.
     *
     * @param exception the exception to log
     */
    private void logRedisError(Exception exception) {
        if (isConnectionException(exception)) {
            log.warn("Redis connection failed: {}", exception.getMessage());
        } else if (isTimeoutException(exception)) {
            log.warn("Redis operation timed out: {}", exception.getMessage());
        } else if (isDataAccessException(exception)) {
            log.error("Redis data access error: {}", exception.getMessage(), exception);
        } else {
            log.error("Unexpected Redis error: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Determines if an exception should trigger a retry.
     *
     * @param exception the exception to check
     * @return true if the operation should be retried
     */
    private boolean shouldRetry(Exception exception) {
        // Retry on connection failures and timeouts
        if (isConnectionException(exception) || isTimeoutException(exception)) {
            return true;
        }
        
        // Don't retry on data access errors (likely application errors)
        if (isDataAccessException(exception)) {
            return false;
        }
        
        // Don't retry on runtime exceptions (likely application errors)
        if (exception instanceof RuntimeException) {
            return false;
        }
        
        // Default to not retrying unknown exceptions
        return false;
    }

    /**
     * Calculates retry delay using exponential backoff.
     *
     * @param attempt the current attempt number (1-based)
     * @return the delay duration
     */
    private Duration calculateRetryDelay(int attempt) {
        long delayMs = INITIAL_RETRY_DELAY.toMillis() * (1L << (attempt - 1));
        // Cap the delay at 5 seconds
        return Duration.ofMillis(Math.min(delayMs, 5000));
    }

    /**
     * Checks if the exception is a connection-related exception.
     *
     * @param exception the exception to check
     * @return true if it's a connection exception
     */
    private boolean isConnectionException(Exception exception) {
        return exception instanceof RedisConnectionFailureException ||
               (exception.getCause() instanceof java.net.ConnectException) ||
               (exception.getCause() instanceof java.net.SocketException) ||
               exception.getMessage().toLowerCase().contains("connection");
    }

    /**
     * Checks if the exception is a timeout-related exception.
     *
     * @param exception the exception to check
     * @return true if it's a timeout exception
     */
    private boolean isTimeoutException(Exception exception) {
        return exception instanceof java.util.concurrent.TimeoutException ||
               (exception.getCause() instanceof java.util.concurrent.TimeoutException) ||
               exception.getMessage().toLowerCase().contains("timeout");
    }

    /**
     * Checks if the exception is a data access exception.
     *
     * @param exception the exception to check
     * @return true if it's a data access exception
     */
    private boolean isDataAccessException(Exception exception) {
        return exception instanceof DataAccessException ||
               exception instanceof RedisSystemException;
    }

    /**
     * Gets the current circuit breaker state for monitoring purposes.
     *
     * @return the current circuit breaker state
     */
    public String getCircuitBreakerState() {
        return circuitState.name();
    }

    /**
     * Gets the current failure count for monitoring purposes.
     *
     * @return the current failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Resets the circuit breaker state (for testing or manual intervention).
     */
    public void resetCircuitBreaker() {
        circuitState = CircuitBreakerState.CLOSED;
        failureCount.set(0);
        lastFailureTime.set(null);
        log.info("Circuit breaker manually reset");
    }
}