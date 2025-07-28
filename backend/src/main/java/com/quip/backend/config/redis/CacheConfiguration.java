package com.quip.backend.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration that defines cache-specific settings with different TTL values
 * and custom serialization for different cache types.
 */
@Slf4j
@Configuration
@EnableCaching
@EnableAsync
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfiguration {

    // Cache names with their specific TTL settings
    public static final String TOOL_WHITELIST_CACHE = "toolWhitelist";
    public static final String PROBLEM_CATEGORIES_CACHE = "problemCategories";
    public static final String SERVER_DATA_CACHE = "serverData";
    public static final String MEMBER_DATA_CACHE = "memberData";
    public static final String ASSISTANT_SESSION_CACHE = "assistantSession";
    public static final String TEMPORARY_DATA_CACHE = "temporaryData";

    private final RedisCacheProperties cacheProperties;
    private final AppRedisProperties appRedisProperties;

    @Autowired
    public CacheConfiguration(
            @Qualifier("redisCacheProperties") RedisCacheProperties cacheProperties,
            AppRedisProperties appRedisProperties
    ) {
        this.cacheProperties = cacheProperties;
        this.appRedisProperties = appRedisProperties;
    }

    /**
     * Creates and configures the cache manager with cache-specific configurations.
     *
     * @param connectionFactory the Redis connection factory
     * @param objectMapper the primary ObjectMapper bean with JavaTimeModule
     * @return configured RedisCacheManager with cache-specific settings
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        log.info("Configuring Redis cache manager with cache-specific TTL settings");

        // Create default cache configuration
        RedisCacheConfiguration defaultConfig = createDefaultCacheConfiguration(objectMapper);

        // Create cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheSpecificConfigurations(objectMapper);

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("Redis cache manager configured with {} cache-specific configurations", 
                cacheConfigurations.size());
        
        return cacheManager;
    }

    /**
     * Creates the default cache configuration with standard settings.
     *
     * @param objectMapper the ObjectMapper with JavaTimeModule support
     * @return default RedisCacheConfiguration
     */
    private RedisCacheConfiguration createDefaultCacheConfiguration(ObjectMapper objectMapper) {
        // Create a copy and configure for Redis serialization
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        redisObjectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, 
                ObjectMapper.DefaultTyping.NON_FINAL);
        Jackson2JsonRedisSerializer<Object> jsonSerializer = 
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Object.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getTimeToLive())
                .computePrefixWith(cacheName -> cacheProperties.getKeyPrefix() + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // Configure null value caching based on properties
        if (!cacheProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }

        return config;
    }

    /**
     * Creates cache-specific configurations with different TTL settings.
     *
     * @param objectMapper the ObjectMapper with JavaTimeModule support
     * @return map of cache names to their specific configurations
     */
    private Map<String, RedisCacheConfiguration> createCacheSpecificConfigurations(ObjectMapper objectMapper) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        RedisCacheConfiguration baseConfig = createDefaultCacheConfiguration(objectMapper);

        // Tool whitelist cache - medium-term (1 hour)
        cacheConfigurations.put(TOOL_WHITELIST_CACHE, 
                baseConfig.entryTtl(Duration.ofHours(1)));

        // Problem categories cache - long-term (24 hours) - static data
        cacheConfigurations.put(PROBLEM_CATEGORIES_CACHE, 
                baseConfig.entryTtl(Duration.ofHours(24)));

        // Server data cache - medium-term (2 hours)
        cacheConfigurations.put(SERVER_DATA_CACHE, 
                baseConfig.entryTtl(Duration.ofHours(2)));

        // Member data cache - medium-term (30 minutes)
        cacheConfigurations.put(MEMBER_DATA_CACHE, 
                baseConfig.entryTtl(Duration.ofMinutes(30)));

        // Assistant session cache - short-term (15 minutes)
        cacheConfigurations.put(ASSISTANT_SESSION_CACHE, 
                baseConfig.entryTtl(Duration.ofMinutes(15)));

        // Temporary data cache - very short-term (5 minutes)
        cacheConfigurations.put(TEMPORARY_DATA_CACHE, 
                baseConfig.entryTtl(Duration.ofMinutes(5)));

        log.info("Created cache configurations: {}", cacheConfigurations.keySet());
        
        return cacheConfigurations;
    }


}