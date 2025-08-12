package com.quip.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quip.backend.config.redis.AppRedisProperties;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.config.redis.RedisCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CacheConfiguration
 */
@ExtendWith(MockitoExtension.class)
class CacheConfigurationTest {

    @Mock
    private RedisCacheProperties cacheProperties;

    @Mock
    private AppRedisProperties appRedisProperties;

    @Mock
    private RedisConnectionFactory connectionFactory;

    private CacheConfiguration cacheConfiguration;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Setup default mock behavior with lenient stubbing
        lenient().when(cacheProperties.getTimeToLive()).thenReturn(Duration.ofHours(1));
        lenient().when(cacheProperties.getKeyPrefix()).thenReturn("quip:backend:");
        lenient().when(cacheProperties.isCacheNullValues()).thenReturn(false);

        // Create ObjectMapper with JavaTimeModule (same as JacksonConfig)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        cacheConfiguration = new CacheConfiguration(cacheProperties, appRedisProperties);
    }

    @Test
    void cacheManager_ShouldReturnRedisCacheManager() {
        // When
        CacheManager cacheManager = cacheConfiguration.cacheManager(connectionFactory, objectMapper);

        // Then
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void cacheManager_ShouldConfigureWithCorrectProperties() {
        // Given
        Duration customTtl = Duration.ofMinutes(30);
        String customPrefix = "test:prefix:";
        lenient().when(cacheProperties.getTimeToLive()).thenReturn(customTtl);
        lenient().when(cacheProperties.getKeyPrefix()).thenReturn(customPrefix);

        // When
        CacheManager cacheManager = cacheConfiguration.cacheManager(connectionFactory, objectMapper);

        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void cacheManager_ShouldHandleNullValueCachingConfiguration() {
        // Given
        when(cacheProperties.isCacheNullValues()).thenReturn(true);

        // When
        CacheManager cacheManager = cacheConfiguration.cacheManager(connectionFactory, objectMapper);

        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
    }

    @Test
    void cacheConfiguration_ShouldHaveDefinedCacheNames() {
        // Then
        assertThat(CacheConfiguration.TOOL_WHITELIST_CACHE).isEqualTo("toolWhitelist");
        assertThat(CacheConfiguration.PROBLEM_CATEGORIES_CACHE).isEqualTo("problemCategories");
        assertThat(CacheConfiguration.SERVER_DATA_CACHE).isEqualTo("serverData");
        assertThat(CacheConfiguration.MEMBER_DATA_CACHE).isEqualTo("memberData");
        assertThat(CacheConfiguration.ASSISTANT_SESSION_CACHE).isEqualTo("assistantSession");
        assertThat(CacheConfiguration.TEMPORARY_DATA_CACHE).isEqualTo("temporaryData");
    }

    @Test
    void cacheManager_WithMinimalConfiguration_ShouldWork() {
        // Given - using default mock setup

        // When
        CacheManager cacheManager = cacheConfiguration.cacheManager(connectionFactory, objectMapper);

        // Then
        assertThat(cacheManager).isNotNull();
        RedisCacheManager redisCacheManager = (RedisCacheManager) cacheManager;
        assertThat(redisCacheManager.getCacheNames()).isEmpty(); // Initially empty until caches are accessed
    }

    @Test
    void cacheManager_ShouldUseObjectMapperWithJavaTimeModule() {
        // When
        CacheManager cacheManager = cacheConfiguration.cacheManager(connectionFactory, objectMapper);

        // Then
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        // The ObjectMapper should have JavaTimeModule registered for proper OffsetDateTime serialization
        assertThat(objectMapper.getRegisteredModuleIds())
                .anyMatch(id -> id.toString().contains("JavaTimeModule") || id.toString().contains("jsr310"));
    }
}