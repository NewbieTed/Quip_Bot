package com.quip.backend.health;

import com.quip.backend.redis.health.RedisHealthIndicator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HealthController.
 * Uses mocks to avoid actual Redis connections.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private RedisHealthIndicator redisHealthIndicator;

    @InjectMocks
    private HealthController healthController;

    @Test
    void health_ShouldReturnOK() {
        // When
        String result = healthController.health();

        // Then
        assertThat(result).isEqualTo("OK");
    }

    @Test
    void root_ShouldReturnServiceMessage() {
        // When
        String result = healthController.root();

        // Then
        assertThat(result).isEqualTo("Quip Backend Service is running");
    }

    @Test
    void detailedHealth_WithRedisHealthy_ShouldIncludeRedisStatus() {
        // Given
        Map<String, Object> redisHealth = new HashMap<>();
        redisHealth.put("status", "UP");
        redisHealth.put("ping", "PONG");
        
        when(redisHealthIndicator.checkHealth()).thenReturn(redisHealth);

        // When
        Map<String, Object> result = healthController.detailedHealth();

        // Then
        assertThat(result.get("status")).isEqualTo("UP");
        assertThat(result.get("service")).isEqualTo("Quip Backend Service");
        assertThat(result).containsKey("redis");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> redisStatus = (Map<String, Object>) result.get("redis");
        assertThat(redisStatus.get("status")).isEqualTo("UP");
        assertThat(redisStatus.get("ping")).isEqualTo("PONG");
    }

    @Test
    void detailedHealth_WithRedisException_ShouldHandleGracefully() {
        // Given
        when(redisHealthIndicator.checkHealth()).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        Map<String, Object> result = healthController.detailedHealth();

        // Then
        assertThat(result.get("status")).isEqualTo("UP");
        assertThat(result.get("service")).isEqualTo("Quip Backend Service");
        assertThat(result).containsKey("redis");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> redisStatus = (Map<String, Object>) result.get("redis");
        assertThat(redisStatus.get("status")).isEqualTo("DOWN");
        assertThat(redisStatus.get("error")).isEqualTo("Redis health check failed: Redis connection failed");
    }

    @Test
    void detailedHealth_WithoutRedisHealthIndicator_ShouldShowDisabled() {
        // Given
        healthController = new HealthController(null);

        // When
        Map<String, Object> result = healthController.detailedHealth();

        // Then
        assertThat(result.get("status")).isEqualTo("UP");
        assertThat(result.get("service")).isEqualTo("Quip Backend Service");
        assertThat(result.get("redis")).isEqualTo("disabled");
    }

    @Test
    void redisHealth_WithHealthIndicator_ShouldReturnRedisStatus() {
        // Given
        Map<String, Object> redisHealth = new HashMap<>();
        redisHealth.put("status", "UP");
        redisHealth.put("ping", "PONG");
        redisHealth.put("responseTime", "5ms");
        
        when(redisHealthIndicator.checkHealth()).thenReturn(redisHealth);

        // When
        Map<String, Object> result = healthController.redisHealth();

        // Then
        assertThat(result.get("status")).isEqualTo("UP");
        assertThat(result.get("ping")).isEqualTo("PONG");
        assertThat(result.get("responseTime")).isEqualTo("5ms");
    }

    @Test
    void redisHealth_WithoutHealthIndicator_ShouldReturnDisabled() {
        // Given
        healthController = new HealthController(null);

        // When
        Map<String, Object> result = healthController.redisHealth();

        // Then
        assertThat(result.get("status")).isEqualTo("DISABLED");
        assertThat(result.get("message")).isEqualTo("Redis is not enabled");
    }
}