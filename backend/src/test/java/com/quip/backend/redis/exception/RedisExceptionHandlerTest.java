package com.quip.backend.redis.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RedisExceptionHandler.
 */
@ExtendWith(MockitoExtension.class)
class RedisExceptionHandlerTest {

    private RedisExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new RedisExceptionHandler();
    }

    @Test
    void handleRedisOperation_SuccessfulOperation_ReturnsResult() {
        // Given
        String expectedResult = "success";
        Supplier<String> operation = () -> expectedResult;
        Supplier<String> fallback = () -> "fallback";

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals(expectedResult, result);
        assertEquals("CLOSED", exceptionHandler.getCircuitBreakerState());
        assertEquals(0, exceptionHandler.getFailureCount());
    }

    @Test
    void handleRedisOperation_OperationFails_UsesFallback() {
        // Given
        Supplier<String> operation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        String fallbackResult = "fallback";
        Supplier<String> fallback = () -> fallbackResult;

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals(fallbackResult, result);
        assertEquals(1, exceptionHandler.getFailureCount());
    }

    @Test
    void handleRedisOperation_NullFallback_ReturnsNull() {
        // Given
        Supplier<String> operation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };

        // When
        String result = exceptionHandler.handleRedisOperation(operation, null);

        // Then
        assertNull(result);
        assertEquals(1, exceptionHandler.getFailureCount());
    }

    @Test
    void handleRedisOperation_VoidOperation_ExecutesSuccessfully() {
        // Given
        boolean[] executed = {false};
        Runnable operation = () -> executed[0] = true;

        // When
        exceptionHandler.handleRedisOperation(operation);

        // Then
        assertTrue(executed[0]);
        assertEquals("CLOSED", exceptionHandler.getCircuitBreakerState());
        assertEquals(0, exceptionHandler.getFailureCount());
    }

    @Test
    void handleRedisOperation_VoidOperationFails_ExecutesFallback() {
        // Given
        Runnable operation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        boolean[] fallbackExecuted = {false};
        Runnable fallback = () -> fallbackExecuted[0] = true;

        // When
        exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertTrue(fallbackExecuted[0]);
        assertEquals(1, exceptionHandler.getFailureCount());
    }

    @Test
    void circuitBreaker_OpensAfterThresholdFailures() {
        // Given
        Supplier<String> failingOperation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        Supplier<String> fallback = () -> "fallback";

        // When - Execute failing operations to reach threshold
        for (int i = 0; i < 5; i++) {
            exceptionHandler.handleRedisOperation(failingOperation, fallback);
        }

        // Then
        assertEquals("OPEN", exceptionHandler.getCircuitBreakerState());
        assertEquals(5, exceptionHandler.getFailureCount());
    }

    @Test
    void circuitBreaker_OpenState_UsesFallbackDirectly() {
        // Given - Force circuit breaker to open
        Supplier<String> failingOperation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        Supplier<String> fallback = () -> "fallback";
        
        for (int i = 0; i < 5; i++) {
            exceptionHandler.handleRedisOperation(failingOperation, fallback);
        }
        
        // When - Try operation with open circuit
        boolean[] operationExecuted = {false};
        Supplier<String> testOperation = () -> {
            operationExecuted[0] = true;
            return "success";
        };
        
        String result = exceptionHandler.handleRedisOperation(testOperation, fallback);

        // Then
        assertEquals("fallback", result);
        assertFalse(operationExecuted[0]); // Operation should not be executed
        assertEquals("OPEN", exceptionHandler.getCircuitBreakerState());
    }

    @Test
    void circuitBreaker_SuccessfulOperationClosesCircuit() {
        // Given - Force circuit breaker to open
        Supplier<String> failingOperation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        Supplier<String> fallback = () -> "fallback";
        
        for (int i = 0; i < 5; i++) {
            exceptionHandler.handleRedisOperation(failingOperation, fallback);
        }
        assertEquals("OPEN", exceptionHandler.getCircuitBreakerState());
        
        // Reset circuit breaker to simulate timeout (instead of waiting)
        exceptionHandler.resetCircuitBreaker();
        
        // When - Execute successful operation
        Supplier<String> successfulOperation = () -> "success";
        String result = exceptionHandler.handleRedisOperation(successfulOperation, fallback);

        // Then
        assertEquals("success", result);
        assertEquals("CLOSED", exceptionHandler.getCircuitBreakerState());
        assertEquals(0, exceptionHandler.getFailureCount());
    }

    @Test
    void resetCircuitBreaker_ResetsState() {
        // Given - Force circuit breaker to open
        Supplier<String> failingOperation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        Supplier<String> fallback = () -> "fallback";
        
        for (int i = 0; i < 5; i++) {
            exceptionHandler.handleRedisOperation(failingOperation, fallback);
        }
        assertEquals("OPEN", exceptionHandler.getCircuitBreakerState());

        // When
        exceptionHandler.resetCircuitBreaker();

        // Then
        assertEquals("CLOSED", exceptionHandler.getCircuitBreakerState());
        assertEquals(0, exceptionHandler.getFailureCount());
    }

    @Test
    void retryLogic_ConnectionException_RetriesOperation() {
        // Given
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            if (attemptCount[0] < 3) {
                throw new RedisConnectionFailureException("Connection failed");
            }
            return "success";
        };
        Supplier<String> fallback = () -> "fallback";

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals("success", result);
        assertEquals(3, attemptCount[0]); // Should have retried twice before success
    }

    @Test
    void retryLogic_TimeoutException_RetriesOperation() {
        // Given
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            if (attemptCount[0] < 2) {
                throw new RuntimeException("Timeout", new TimeoutException("Operation timed out"));
            }
            return "success";
        };
        Supplier<String> fallback = () -> "fallback";

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals("success", result);
        assertEquals(2, attemptCount[0]); // Should have retried once before success
    }

    @Test
    void retryLogic_DataAccessException_DoesNotRetry() {
        // Given
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            throw new DataAccessException("Data access error") {};
        };
        Supplier<String> fallback = () -> "fallback";

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals("fallback", result);
        assertEquals(1, attemptCount[0]); // Should not have retried
    }

    @Test
    void retryLogic_RuntimeException_DoesNotRetry() {
        // Given
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            throw new RuntimeException("Runtime error");
        };
        Supplier<String> fallback = () -> "fallback";

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals("fallback", result);
        assertEquals(1, attemptCount[0]); // Should not have retried
    }

    @Test
    void retryLogic_MaxRetriesExceeded_UsesFallback() {
        // Given
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            throw new RedisConnectionFailureException("Connection failed");
        };
        Supplier<String> fallback = () -> "fallback";

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertEquals("fallback", result);
        assertEquals(3, attemptCount[0]); // Should have tried 3 times (max retries)
    }

    @Test
    void fallbackOperation_ThrowsException_ReturnsNull() {
        // Given
        Supplier<String> operation = () -> {
            throw new RedisConnectionFailureException("Connection failed");
        };
        Supplier<String> fallback = () -> {
            throw new RuntimeException("Fallback failed");
        };

        // When
        String result = exceptionHandler.handleRedisOperation(operation, fallback);

        // Then
        assertNull(result); // Should return null when fallback fails
    }

    @Test
    void exceptionClassification_ConnectionExceptions_AreRetried() {
        // Test that connection exceptions are retried
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            if (attemptCount[0] < 3) {
                throw new RedisConnectionFailureException("Connection failed");
            }
            return "success";
        };
        
        String result = exceptionHandler.handleRedisOperation(operation, () -> "fallback");
        
        assertEquals("success", result);
        assertEquals(3, attemptCount[0]); // Should have retried
    }

    @Test
    void exceptionClassification_TimeoutExceptions_AreRetried() {
        // Test that timeout exceptions are retried
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            if (attemptCount[0] < 2) {
                throw new RuntimeException("Timeout", new TimeoutException("Operation timed out"));
            }
            return "success";
        };
        
        String result = exceptionHandler.handleRedisOperation(operation, () -> "fallback");
        
        assertEquals("success", result);
        assertEquals(2, attemptCount[0]); // Should have retried once
    }

    @Test
    void exceptionClassification_DataAccessExceptions_AreNotRetried() {
        // Test that data access exceptions are not retried
        int[] attemptCount = {0};
        Supplier<String> operation = () -> {
            attemptCount[0]++;
            throw new DataAccessException("Data error") {};
        };
        
        String result = exceptionHandler.handleRedisOperation(operation, () -> "fallback");
        
        assertEquals("fallback", result);
        assertEquals(1, attemptCount[0]); // Should not have retried
    }
}