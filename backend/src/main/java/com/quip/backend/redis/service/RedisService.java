package com.quip.backend.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quip.backend.redis.exception.RedisExceptionHandler;
import com.quip.backend.redis.metrics.RedisMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service providing Redis operations abstraction for caching and data management.
 * <p>
 * This service provides a high-level interface for Redis operations including:
 * - Basic CRUD operations (set, get, delete, exists)
 * - TTL support for cached data with Duration parameters
 * - Pattern-based key operations (keys, deleteByPattern)
 * - Hash operations (hset, hget, hdel) for session support
 * - List operations (lpush, rpop) for queue functionality
 * - Batch operations for multiple key handling
 * </p>
 * <p>
 * The service handles serialization/deserialization automatically and provides
 * type-safe operations for cached data.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisExceptionHandler exceptionHandler;
    private final RedisMetricsService metricsService;

    /**
     * Stores a key-value pair in Redis without expiration.
     *
     * @param key   the key to store
     * @param value the value to store
     * @throws IllegalArgumentException if key is null or empty
     */
    public void set(String key, Object value) {
        validateKey(key);
        Instant start = Instant.now();
        boolean success = false;
        
        try {
            exceptionHandler.handleRedisOperation(() -> {
                redisTemplate.opsForValue().set(key, value);
                log.debug("Successfully stored key: {}", key);
            });
            success = true;
        } catch (Exception e) {
            metricsService.recordCacheError("set", e);
            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            metricsService.recordSetOperation(duration, success);
        }
    }

    /**
     * Stores a key-value pair in Redis with TTL (Time To Live).
     *
     * @param key   the key to store
     * @param value the value to store
     * @param ttl   the time to live duration
     * @throws IllegalArgumentException if key is null/empty or ttl is null/negative
     */
    public void set(String key, Object value, Duration ttl) {
        validateKey(key);
        validateTtl(ttl);
        Instant start = Instant.now();
        boolean success = false;
        
        try {
            exceptionHandler.handleRedisOperation(() -> {
                redisTemplate.opsForValue().set(key, value, ttl);
                log.debug("Successfully stored key: {} with TTL: {}", key, ttl);
            });
            success = true;
        } catch (Exception e) {
            metricsService.recordCacheError("set", e);
            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            metricsService.recordSetOperation(duration, success);
        }
    }

    /**
     * Retrieves a value from Redis by key and casts it to the specified type.
     *
     * @param key  the key to retrieve
     * @param type the expected type of the value
     * @param <T>  the type parameter
     * @return the value cast to the specified type, or null if key doesn't exist
     * @throws IllegalArgumentException if key is null/empty or type is null
     * @throws ClassCastException       if the value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        validateKey(key);
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        
        Instant start = Instant.now();
        boolean success = false;
        T result = null;
        
        try {
            result = exceptionHandler.handleRedisOperation(() -> {
                Object value = redisTemplate.opsForValue().get(key);
                if (value == null) {
                    log.debug("Key not found: {}", key);
                    return null;
                }
                
                log.debug("Successfully retrieved key: {}", key);
                return type.cast(value);
            }, () -> null); // Fallback to null when Redis is unavailable
            
            success = true;
            
            // Record cache hit or miss
            if (result != null) {
                metricsService.recordCacheHit(key);
            } else {
                metricsService.recordCacheMiss(key);
            }
            
            return result;
        } catch (Exception e) {
            metricsService.recordCacheError("get", e);
            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            metricsService.recordGetOperation(duration, success);
        }
    }

    /**
     * Deletes a key from Redis.
     *
     * @param key the key to delete
     * @return true if the key was deleted, false if it didn't exist
     * @throws IllegalArgumentException if key is null or empty
     */
    public boolean delete(String key) {
        validateKey(key);
        Instant start = Instant.now();
        boolean success = false;
        boolean result = false;
        
        try {
            result = exceptionHandler.handleRedisOperation(() -> {
                Boolean deleteResult = redisTemplate.delete(key);
                boolean deleted = Boolean.TRUE.equals(deleteResult);
                log.debug("Delete operation for key: {} - result: {}", key, deleted);
                return deleted;
            }, () -> false); // Fallback to false when Redis is unavailable
            
            success = true;
            return result;
        } catch (Exception e) {
            metricsService.recordCacheError("delete", e);
            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            metricsService.recordDeleteOperation(duration, success);
        }
    }

    /**
     * Checks if a key exists in Redis.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     * @throws IllegalArgumentException if key is null or empty
     */
    public boolean exists(String key) {
        validateKey(key);
        Instant start = Instant.now();
        boolean success = false;
        boolean result = false;
        
        try {
            result = exceptionHandler.handleRedisOperation(() -> {
                Boolean existsResult = redisTemplate.hasKey(key);
                boolean exists = Boolean.TRUE.equals(existsResult);
                log.debug("Existence check for key: {} - result: {}", key, exists);
                return exists;
            }, () -> false); // Fallback to false when Redis is unavailable
            
            success = true;
            return result;
        } catch (Exception e) {
            metricsService.recordCacheError("exists", e);
            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            metricsService.recordExistsOperation(duration, success);
        }
    }

    /**
     * Sets expiration time for an existing key.
     *
     * @param key the key to set expiration for
     * @param ttl the time to live duration
     * @return true if the expiration was set, false if key doesn't exist
     * @throws IllegalArgumentException if key is null/empty or ttl is null/negative
     */
    public boolean setExpire(String key, Duration ttl) {
        validateKey(key);
        validateTtl(ttl);
        return exceptionHandler.handleRedisOperation(() -> {
            Boolean result = redisTemplate.expire(key, ttl);
            boolean expired = Boolean.TRUE.equals(result);
            log.debug("Set expiration for key: {} with TTL: {} - result: {}", key, ttl, expired);
            return expired;
        }, () -> false); // Fallback to false when Redis is unavailable
    }

    /**
     * Retrieves all keys matching the given pattern.
     *
     * @param pattern the pattern to match (supports Redis glob-style patterns)
     * @return set of keys matching the pattern
     * @throws IllegalArgumentException if pattern is null or empty
     */
    public Set<String> keys(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }
        
        return exceptionHandler.handleRedisOperation(() -> {
            Set<String> keys = redisTemplate.keys(pattern);
            log.debug("Found {} keys matching pattern: {}", keys != null ? keys.size() : 0, pattern);
            return keys != null ? keys : Set.of();
        }, Set::of); // Fallback to empty set when Redis is unavailable
    }

    /**
     * Deletes all keys matching the given pattern.
     *
     * @param pattern the pattern to match (supports Redis glob-style patterns)
     * @return the number of keys deleted
     * @throws IllegalArgumentException if pattern is null or empty
     */
    public long deleteByPattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("Pattern cannot be null or empty");
        }
        
        return exceptionHandler.handleRedisOperation(() -> {
            Set<String> keysToDelete = keys(pattern);
            if (keysToDelete.isEmpty()) {
                log.debug("No keys found matching pattern: {}", pattern);
                return 0L;
            }
            
            Long deletedCount = redisTemplate.delete(keysToDelete);
            long deleted = deletedCount != null ? deletedCount : 0;
            log.debug("Deleted {} keys matching pattern: {}", deleted, pattern);
            return deleted;
        }, () -> 0L); // Fallback to 0 when Redis is unavailable
    }

    // ========== Hash Operations ==========

    /**
     * Sets a field in a Redis hash.
     *
     * @param key   the hash key
     * @param field the field name within the hash
     * @param value the value to store
     * @throws IllegalArgumentException if key or field is null or empty
     */
    public void hset(String key, String field, Object value) {
        validateKey(key);
        validateField(field);
        exceptionHandler.handleRedisOperation(() -> {
            redisTemplate.opsForHash().put(key, field, value);
            log.debug("Successfully stored hash field: {} in key: {}", field, key);
        });
    }

    /**
     * Gets a field value from a Redis hash.
     *
     * @param key   the hash key
     * @param field the field name within the hash
     * @param type  the expected type of the value
     * @param <T>   the type parameter
     * @return the value cast to the specified type, or null if field doesn't exist
     * @throws IllegalArgumentException if key, field is null/empty or type is null
     * @throws ClassCastException       if the value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T hget(String key, String field, Class<T> type) {
        validateKey(key);
        validateField(field);
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        
        return exceptionHandler.handleRedisOperation(() -> {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (value == null) {
                log.debug("Hash field not found: {} in key: {}", field, key);
                return null;
            }
            
            log.debug("Successfully retrieved hash field: {} from key: {}", field, key);
            return type.cast(value);
        }, () -> null); // Fallback to null when Redis is unavailable
    }

    /**
     * Deletes a field from a Redis hash.
     *
     * @param key   the hash key
     * @param field the field name to delete
     * @return true if the field was deleted, false if it didn't exist
     * @throws IllegalArgumentException if key or field is null or empty
     */
    public boolean hdel(String key, String field) {
        validateKey(key);
        validateField(field);
        return exceptionHandler.handleRedisOperation(() -> {
            Long result = redisTemplate.opsForHash().delete(key, field);
            boolean deleted = result != null && result > 0;
            log.debug("Delete hash field operation for key: {}, field: {} - result: {}", key, field, deleted);
            return deleted;
        }, () -> false); // Fallback to false when Redis is unavailable
    }

    /**
     * Gets all fields and values from a Redis hash.
     *
     * @param key the hash key
     * @return map of all field-value pairs in the hash
     * @throws IllegalArgumentException if key is null or empty
     */
    public Map<Object, Object> hgetAll(String key) {
        validateKey(key);
        return exceptionHandler.handleRedisOperation(() -> {
            Map<Object, Object> result = redisTemplate.opsForHash().entries(key);
            log.debug("Retrieved {} fields from hash key: {}", result.size(), key);
            return result;
        }, Map::of); // Fallback to empty map when Redis is unavailable
    }

    /**
     * Checks if a field exists in a Redis hash.
     *
     * @param key   the hash key
     * @param field the field name to check
     * @return true if the field exists, false otherwise
     * @throws IllegalArgumentException if key or field is null or empty
     */
    public boolean hexists(String key, String field) {
        validateKey(key);
        validateField(field);
        return exceptionHandler.handleRedisOperation(() -> {
            Boolean result = redisTemplate.opsForHash().hasKey(key, field);
            boolean exists = Boolean.TRUE.equals(result);
            log.debug("Hash field existence check for key: {}, field: {} - result: {}", key, field, exists);
            return exists;
        }, () -> false); // Fallback to false when Redis is unavailable
    }

    // ========== List Operations ==========

    /**
     * Pushes a value to the left (head) of a Redis list.
     *
     * @param key   the list key
     * @param value the value to push
     * @return the length of the list after the push operation
     * @throws IllegalArgumentException if key is null or empty
     */
    public long lpush(String key, Object value) {
        validateKey(key);
        return exceptionHandler.handleRedisOperation(() -> {
            Long result = redisTemplate.opsForList().leftPush(key, value);
            long length = result != null ? result : 0;
            log.debug("Successfully pushed value to left of list key: {} - new length: {}", key, length);
            return length;
        }, () -> 0L); // Fallback to 0 when Redis is unavailable
    }

    /**
     * Pushes multiple values to the left (head) of a Redis list.
     *
     * @param key    the list key
     * @param values the values to push
     * @return the length of the list after the push operation
     * @throws IllegalArgumentException if key is null or empty
     */
    public long lpush(String key, Object... values) {
        validateKey(key);
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values cannot be null or empty");
        }
        
        return exceptionHandler.handleRedisOperation(() -> {
            Long result = redisTemplate.opsForList().leftPushAll(key, values);
            long length = result != null ? result : 0;
            log.debug("Successfully pushed {} values to left of list key: {} - new length: {}", values.length, key, length);
            return length;
        }, () -> 0L); // Fallback to 0 when Redis is unavailable
    }

    /**
     * Pops a value from the right (tail) of a Redis list.
     *
     * @param key  the list key
     * @param type the expected type of the value
     * @param <T>  the type parameter
     * @return the popped value cast to the specified type, or null if list is empty
     * @throws IllegalArgumentException if key is null/empty or type is null
     * @throws ClassCastException       if the value cannot be cast to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T rpop(String key, Class<T> type) {
        validateKey(key);
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }
        
        return exceptionHandler.handleRedisOperation(() -> {
            Object value = redisTemplate.opsForList().rightPop(key);
            if (value == null) {
                log.trace("List is empty or key not found: {}", key);
                return null;
            }
            
            log.debug("Successfully popped value from right of list key: {}", key);
            
            // Special handling for String type when Redis returns deserialized objects
            if (type == String.class && !(value instanceof String)) {
                // Convert the deserialized object back to JSON string
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String jsonString = objectMapper.writeValueAsString(value);
                    return type.cast(jsonString);
                } catch (Exception e) {
                    log.warn("Failed to serialize Redis value back to JSON string: {}", e.getMessage());
                    // Fallback to toString if JSON serialization fails
                    return type.cast(value.toString());
                }
            }
            
            return type.cast(value);
        }, () -> null); // Fallback to null when Redis is unavailable
    }

    /**
     * Gets the length of a Redis list.
     *
     * @param key the list key
     * @return the length of the list
     * @throws IllegalArgumentException if key is null or empty
     */
    public long llen(String key) {
        validateKey(key);
        return exceptionHandler.handleRedisOperation(() -> {
            Long result = redisTemplate.opsForList().size(key);
            long length = result != null ? result : 0;
            log.debug("List length for key: {} - length: {}", key, length);
            return length;
        }, () -> 0L); // Fallback to 0 when Redis is unavailable
    }

    /**
     * Gets a range of elements from a Redis list.
     *
     * @param key   the list key
     * @param start the start index (0-based)
     * @param end   the end index (0-based, -1 for last element)
     * @return list of elements in the specified range
     * @throws IllegalArgumentException if key is null or empty
     */
    public List<Object> lrange(String key, long start, long end) {
        validateKey(key);
        return exceptionHandler.handleRedisOperation(() -> {
            List<Object> result = redisTemplate.opsForList().range(key, start, end);
            log.debug("Retrieved {} elements from list key: {} (range: {}-{})", 
                     result != null ? result.size() : 0, key, start, end);
            return result != null ? result : List.of();
        }, List::of); // Fallback to empty list when Redis is unavailable
    }

    // ========== Batch Operations ==========

    /**
     * Sets multiple key-value pairs in a single operation.
     *
     * @param keyValueMap map of keys and values to set
     * @throws IllegalArgumentException if keyValueMap is null or empty
     */
    public void mset(Map<String, Object> keyValueMap) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            throw new IllegalArgumentException("Key-value map cannot be null or empty");
        }
        
        // Validate all keys
        keyValueMap.keySet().forEach(this::validateKey);
        
        exceptionHandler.handleRedisOperation(() -> {
            redisTemplate.opsForValue().multiSet(keyValueMap);
            log.debug("Successfully set {} key-value pairs", keyValueMap.size());
        });
    }

    /**
     * Gets multiple values by their keys in a single operation.
     *
     * @param keys the keys to retrieve
     * @return list of values corresponding to the keys (null for non-existent keys)
     * @throws IllegalArgumentException if keys is null or empty
     */
    public List<Object> mget(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Keys list cannot be null or empty");
        }
        
        // Validate all keys
        keys.forEach(this::validateKey);
        
        return exceptionHandler.handleRedisOperation(() -> {
            List<Object> result = redisTemplate.opsForValue().multiGet(keys);
            log.debug("Successfully retrieved {} values for {} keys", 
                     result != null ? result.size() : 0, keys.size());
            return result != null ? result : List.of();
        }, List::of); // Fallback to empty list when Redis is unavailable
    }

    /**
     * Deletes multiple keys in a single operation.
     *
     * @param keys the keys to delete
     * @return the number of keys that were deleted
     * @throws IllegalArgumentException if keys is null or empty
     */
    public long mdel(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Keys list cannot be null or empty");
        }
        
        // Validate all keys
        keys.forEach(this::validateKey);
        
        return exceptionHandler.handleRedisOperation(() -> {
            Long result = redisTemplate.delete(keys);
            long deleted = result != null ? result : 0;
            log.debug("Successfully deleted {} out of {} keys", deleted, keys.size());
            return deleted;
        }, () -> 0L); // Fallback to 0 when Redis is unavailable
    }

    /**
     * Checks if multiple keys exist in a single operation.
     *
     * @param keys the keys to check
     * @return map of keys to their existence status
     * @throws IllegalArgumentException if keys is null or empty
     */
    public Map<String, Boolean> mexists(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Keys list cannot be null or empty");
        }
        
        // Validate all keys
        keys.forEach(this::validateKey);
        
        return exceptionHandler.handleRedisOperation(() -> {
            Map<String, Boolean> result = new java.util.HashMap<>();
            for (String key : keys) {
                Boolean exists = redisTemplate.hasKey(key);
                result.put(key, Boolean.TRUE.equals(exists));
            }
            
            long existingCount = result.values().stream().mapToLong(exists -> exists ? 1 : 0).sum();
            log.debug("Checked existence of {} keys - {} exist", keys.size(), existingCount);
            return result;
        }, () -> {
            // Fallback: return map with all keys marked as non-existent
            Map<String, Boolean> fallbackResult = new java.util.HashMap<>();
            keys.forEach(key -> fallbackResult.put(key, false));
            return fallbackResult;
        });
    }

    /**
     * Validates that the key is not null or empty.
     *
     * @param key the key to validate
     * @throws IllegalArgumentException if key is null or empty
     */
    private void validateKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    /**
     * Validates that the field is not null or empty.
     *
     * @param field the field to validate
     * @throws IllegalArgumentException if field is null or empty
     */
    private void validateField(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field cannot be null or empty");
        }
    }

    /**
     * Validates that the TTL is not null and is positive.
     *
     * @param ttl the TTL to validate
     * @throws IllegalArgumentException if ttl is null, negative, or zero
     */
    private void validateTtl(Duration ttl) {
        if (ttl == null) {
            throw new IllegalArgumentException("TTL cannot be null");
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }
    }
}