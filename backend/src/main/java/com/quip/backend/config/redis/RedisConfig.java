package com.quip.backend.config.redis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quip.backend.redis.metrics.RedisMetricsService;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * Redis configuration class that sets up Redis connection factory, RedisTemplate,
 * and cache manager with proper serialization and connection pooling.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    private final RedisProperties redisProperties;
    private final RedisCacheProperties cacheProperties;
    private final AppRedisProperties appRedisProperties;

    @Autowired
    public RedisConfig(RedisProperties redisProperties,
                       @Qualifier("redisCacheProperties") RedisCacheProperties cacheProperties,
                       AppRedisProperties appRedisProperties) {
        this.redisProperties = redisProperties;
        this.cacheProperties = cacheProperties;
        this.appRedisProperties = appRedisProperties;
    }

    /**
     * Creates and configures the Lettuce connection factory with connection pooling
     * and timeout settings.
     *
     * @return configured LettuceConnectionFactory
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection factory for {}:{}",
                redisProperties.getHost(), redisProperties.getPort());

        // Redis standalone configuration
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setDatabase(redisProperties.getDatabase());

        // Set password if provided
        if (redisProperties.isPasswordEnabled()) {
            redisConfig.setPassword(redisProperties.getPassword());
            log.info("Redis password authentication enabled");
        }

        // Configure connection pool
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        RedisProperties.Lettuce.Pool pool = redisProperties.getLettuce().getPool();
        poolConfig.setMaxTotal(pool.getMaxActive());
        poolConfig.setMaxIdle(pool.getMaxIdle());
        poolConfig.setMinIdle(pool.getMinIdle());
        poolConfig.setMaxWait(pool.getMaxWait());
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        // Configure client options with timeouts
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(redisProperties.getTimeout())
                        .build())
                .build();

        // Build Lettuce client configuration with pooling
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(redisProperties.getTimeout())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);

        log.info("Redis connection factory configured successfully with pool settings: " +
                "maxActive={}, maxIdle={}, minIdle={}, timeout={}",
                pool.getMaxActive(), pool.getMaxIdle(), pool.getMinIdle(), redisProperties.getTimeout());

        return factory;
    }

    /**
     * Creates and configures RedisTemplate with JSON serialization for values
     * and String serialization for keys.
     *
     * @param connectionFactory the Redis connection factory
     * @param objectMapper the primary ObjectMapper bean
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory, 
                                                       ObjectMapper objectMapper) {
        log.info("Configuring RedisTemplate with JSON serialization");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Create a copy of the ObjectMapper for Redis-specific configuration
        ObjectMapper redisObjectMapper = objectMapper.copy();
        redisObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // Remove default typing to handle plain JSON from external sources (like the agent)
        // redisObjectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

        // Configure JSON serializer for values with ObjectMapper
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // Configure String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // Set serializers
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();

        log.info("RedisTemplate configured successfully with JSON serialization");
        return template;
    }



    /**
     * Creates and configures a custom cache key generator for consistent key generation.
     *
     * @return configured CustomCacheKeyGenerator
     */
    @Bean
    public CustomCacheKeyGenerator customCacheKeyGenerator() {
        log.info("Configuring custom cache key generator");
        return new CustomCacheKeyGenerator();
    }


}