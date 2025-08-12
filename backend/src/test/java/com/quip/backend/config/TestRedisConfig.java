package com.quip.backend.config;

import com.quip.backend.redis.health.RedisHealthIndicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * Test configuration to mock Redis components and prevent actual Redis connections during testing.
 */
@TestConfiguration
@Profile("test")
public class TestRedisConfig {

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RedisHealthIndicator redisHealthIndicator;
}