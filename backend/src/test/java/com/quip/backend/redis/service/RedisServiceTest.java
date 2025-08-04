package com.quip.backend.redis.service;

import com.quip.backend.redis.exception.RedisExceptionHandler;
import com.quip.backend.redis.metrics.RedisMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for RedisService.
 * Tests all CRUD operations, TTL support, and pattern-based operations.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ListOperations<String, Object> listOperations;

    @Mock
    private RedisExceptionHandler exceptionHandler;

    @Mock
    private RedisMetricsService metricsService;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        // Configure exception handler to execute operations directly for successful cases
        doAnswer(invocation -> {
            Runnable operation = invocation.getArgument(0);
            operation.run();
            return null;
        }).when(exceptionHandler).handleRedisOperation(any(Runnable.class));
        
        doAnswer(invocation -> {
            Runnable operation = invocation.getArgument(0);
            Runnable fallback = invocation.getArgument(1);
            if (fallback != null) {
                operation.run();
            }
            return null;
        }).when(exceptionHandler).handleRedisOperation(any(Runnable.class), any(Runnable.class));
        
        when(exceptionHandler.handleRedisOperation(any(Supplier.class), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<Object> operation = invocation.getArgument(0);
                return operation.get();
            });
    }

    @Test
    void set_WithValidKeyAndValue_ShouldStoreSuccessfully() {
        // Given
        String key = "test:key";
        String value = "test value";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        redisService.set(key, value);

        // Then
        verify(valueOperations).set(key, value);
    }

    @Test
    void set_WithNullKey_ShouldThrowException() {
        // Given
        String key = null;
        String value = "test value";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.set(key, value)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void set_WithEmptyKey_ShouldThrowException() {
        // Given
        String key = "";
        String value = "test value";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.set(key, value)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void set_WithRedisException_ShouldThrowRuntimeException() {
        // Given
        String key = "test:key";
        String value = "test value";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        doThrow(new RuntimeException("Failed to store data in Redis"))
            .when(exceptionHandler).handleRedisOperation(any(Runnable.class));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.set(key, value)
        );
        assertEquals("Failed to store data in Redis", exception.getMessage());
    }

    @Test
    void setWithTtl_WithValidParameters_ShouldStoreWithExpiration() {
        // Given
        String key = "test:key";
        String value = "test value";
        Duration ttl = Duration.ofMinutes(5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        redisService.set(key, value, ttl);

        // Then
        verify(valueOperations).set(key, value, ttl);
    }

    @Test
    void setWithTtl_WithNullTtl_ShouldThrowException() {
        // Given
        String key = "test:key";
        String value = "test value";
        Duration ttl = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.set(key, value, ttl)
        );
        assertEquals("TTL cannot be null", exception.getMessage());
    }

    @Test
    void setWithTtl_WithNegativeTtl_ShouldThrowException() {
        // Given
        String key = "test:key";
        String value = "test value";
        Duration ttl = Duration.ofMinutes(-1);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.set(key, value, ttl)
        );
        assertEquals("TTL must be positive", exception.getMessage());
    }

    @Test
    void setWithTtl_WithZeroTtl_ShouldThrowException() {
        // Given
        String key = "test:key";
        String value = "test value";
        Duration ttl = Duration.ZERO;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.set(key, value, ttl)
        );
        assertEquals("TTL must be positive", exception.getMessage());
    }

    @Test
    void get_WithExistingKey_ShouldReturnValue() {
        // Given
        String key = "test:key";
        String expectedValue = "test value";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // When
        String result = redisService.get(key, String.class);

        // Then
        assertEquals(expectedValue, result);
        verify(valueOperations).get(key);
    }

    @Test
    void get_WithNonExistingKey_ShouldReturnNull() {
        // Given
        String key = "test:key";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // When
        String result = redisService.get(key, String.class);

        // Then
        assertNull(result);
        verify(valueOperations).get(key);
    }

    @Test
    void get_WithNullKey_ShouldThrowException() {
        // Given
        String key = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.get(key, String.class)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void get_WithNullType_ShouldThrowException() {
        // Given
        String key = "test:key";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.get(key, null)
        );
        assertEquals("Type cannot be null", exception.getMessage());
    }

    @Test
    void get_WithWrongType_ShouldThrowClassCastException() {
        // Given
        String key = "test:key";
        Integer value = 123;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(value);

        // When & Then
        assertThrows(
            ClassCastException.class,
            () -> redisService.get(key, String.class)
        );
    }

    @Test
    void get_WithRedisException_ShouldThrowRuntimeException() {
        // Given
        String key = "test:key";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        when(exceptionHandler.handleRedisOperation(any(Supplier.class), any(Supplier.class)))
            .thenThrow(new RuntimeException("Failed to retrieve data from Redis"));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.get(key, String.class)
        );
        assertEquals("Failed to retrieve data from Redis", exception.getMessage());
    }

    @Test
    void delete_WithExistingKey_ShouldReturnTrue() {
        // Given
        String key = "test:key";
        when(redisTemplate.delete(key)).thenReturn(true);

        // When
        boolean result = redisService.delete(key);

        // Then
        assertTrue(result);
        verify(redisTemplate).delete(key);
    }

    @Test
    void delete_WithNonExistingKey_ShouldReturnFalse() {
        // Given
        String key = "test:key";
        when(redisTemplate.delete(key)).thenReturn(false);

        // When
        boolean result = redisService.delete(key);

        // Then
        assertFalse(result);
        verify(redisTemplate).delete(key);
    }

    @Test
    void delete_WithNullKey_ShouldThrowException() {
        // Given
        String key = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.delete(key)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void exists_WithExistingKey_ShouldReturnTrue() {
        // Given
        String key = "test:key";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean result = redisService.exists(key);

        // Then
        assertTrue(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    void exists_WithNonExistingKey_ShouldReturnFalse() {
        // Given
        String key = "test:key";
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // When
        boolean result = redisService.exists(key);

        // Then
        assertFalse(result);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    void exists_WithNullKey_ShouldThrowException() {
        // Given
        String key = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.exists(key)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void setExpire_WithExistingKey_ShouldReturnTrue() {
        // Given
        String key = "test:key";
        Duration ttl = Duration.ofMinutes(5);
        when(redisTemplate.expire(key, ttl)).thenReturn(true);

        // When
        boolean result = redisService.setExpire(key, ttl);

        // Then
        assertTrue(result);
        verify(redisTemplate).expire(key, ttl);
    }

    @Test
    void setExpire_WithNonExistingKey_ShouldReturnFalse() {
        // Given
        String key = "test:key";
        Duration ttl = Duration.ofMinutes(5);
        when(redisTemplate.expire(key, ttl)).thenReturn(false);

        // When
        boolean result = redisService.setExpire(key, ttl);

        // Then
        assertFalse(result);
        verify(redisTemplate).expire(key, ttl);
    }

    @Test
    void setExpire_WithNullTtl_ShouldThrowException() {
        // Given
        String key = "test:key";
        Duration ttl = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.setExpire(key, ttl)
        );
        assertEquals("TTL cannot be null", exception.getMessage());
    }

    @Test
    void keys_WithValidPattern_ShouldReturnMatchingKeys() {
        // Given
        String pattern = "test:*";
        Set<String> expectedKeys = Set.of("test:key1", "test:key2", "test:key3");
        when(redisTemplate.keys(pattern)).thenReturn(expectedKeys);

        // When
        Set<String> result = redisService.keys(pattern);

        // Then
        assertEquals(expectedKeys, result);
        verify(redisTemplate).keys(pattern);
    }

    @Test
    void keys_WithNoMatches_ShouldReturnEmptySet() {
        // Given
        String pattern = "nonexistent:*";
        when(redisTemplate.keys(pattern)).thenReturn(null);

        // When
        Set<String> result = redisService.keys(pattern);

        // Then
        assertTrue(result.isEmpty());
        verify(redisTemplate).keys(pattern);
    }

    @Test
    void keys_WithNullPattern_ShouldThrowException() {
        // Given
        String pattern = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.keys(pattern)
        );
        assertEquals("Pattern cannot be null or empty", exception.getMessage());
    }

    @Test
    void keys_WithEmptyPattern_ShouldThrowException() {
        // Given
        String pattern = "";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.keys(pattern)
        );
        assertEquals("Pattern cannot be null or empty", exception.getMessage());
    }

    @Test
    void deleteByPattern_WithMatchingKeys_ShouldDeleteAndReturnCount() {
        // Given
        String pattern = "test:*";
        Set<String> keysToDelete = Set.of("test:key1", "test:key2", "test:key3");
        when(redisTemplate.keys(pattern)).thenReturn(keysToDelete);
        when(redisTemplate.delete(keysToDelete)).thenReturn(3L);

        // When
        long result = redisService.deleteByPattern(pattern);

        // Then
        assertEquals(3L, result);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(keysToDelete);
    }

    @Test
    void deleteByPattern_WithNoMatches_ShouldReturnZero() {
        // Given
        String pattern = "nonexistent:*";
        when(redisTemplate.keys(pattern)).thenReturn(Set.of());

        // When
        long result = redisService.deleteByPattern(pattern);

        // Then
        assertEquals(0L, result);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate, never()).delete(any(Set.class));
    }

    @Test
    void deleteByPattern_WithNullPattern_ShouldThrowException() {
        // Given
        String pattern = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.deleteByPattern(pattern)
        );
        assertEquals("Pattern cannot be null or empty", exception.getMessage());
    }

    @Test
    void deleteByPattern_WithEmptyPattern_ShouldThrowException() {
        // Given
        String pattern = "";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.deleteByPattern(pattern)
        );
        assertEquals("Pattern cannot be null or empty", exception.getMessage());
    }

    // ========== Hash Operations Tests ==========

    @Test
    void hset_WithValidParameters_ShouldStoreHashField() {
        // Given
        String key = "test:hash";
        String field = "field1";
        String value = "value1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // When
        redisService.hset(key, field, value);

        // Then
        verify(hashOperations).put(key, field, value);
    }

    @Test
    void hset_WithNullKey_ShouldThrowException() {
        // Given
        String key = null;
        String field = "field1";
        String value = "value1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hset(key, field, value)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void hset_WithNullField_ShouldThrowException() {
        // Given
        String key = "test:hash";
        String field = null;
        String value = "value1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hset(key, field, value)
        );
        assertEquals("Field cannot be null or empty", exception.getMessage());
    }

    @Test
    void hget_WithExistingField_ShouldReturnValue() {
        // Given
        String key = "test:hash";
        String field = "field1";
        String expectedValue = "value1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(key, field)).thenReturn(expectedValue);

        // When
        String result = redisService.hget(key, field, String.class);

        // Then
        assertEquals(expectedValue, result);
        verify(hashOperations).get(key, field);
    }

    @Test
    void hget_WithNonExistingField_ShouldReturnNull() {
        // Given
        String key = "test:hash";
        String field = "field1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(key, field)).thenReturn(null);

        // When
        String result = redisService.hget(key, field, String.class);

        // Then
        assertNull(result);
        verify(hashOperations).get(key, field);
    }

    @Test
    void hget_WithNullType_ShouldThrowException() {
        // Given
        String key = "test:hash";
        String field = "field1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hget(key, field, null)
        );
        assertEquals("Type cannot be null", exception.getMessage());
    }

    @Test
    void hdel_WithExistingField_ShouldReturnTrue() {
        // Given
        String key = "test:hash";
        String field = "field1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(key, field)).thenReturn(1L);

        // When
        boolean result = redisService.hdel(key, field);

        // Then
        assertTrue(result);
        verify(hashOperations).delete(key, field);
    }

    @Test
    void hdel_WithNonExistingField_ShouldReturnFalse() {
        // Given
        String key = "test:hash";
        String field = "field1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(key, field)).thenReturn(0L);

        // When
        boolean result = redisService.hdel(key, field);

        // Then
        assertFalse(result);
        verify(hashOperations).delete(key, field);
    }

    @Test
    void hgetAll_WithValidKey_ShouldReturnAllFields() {
        // Given
        String key = "test:hash";
        Map<Object, Object> expectedMap = Map.of("field1", "value1", "field2", "value2");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(key)).thenReturn(expectedMap);

        // When
        Map<Object, Object> result = redisService.hgetAll(key);

        // Then
        assertEquals(expectedMap, result);
        verify(hashOperations).entries(key);
    }

    @Test
    void hexists_WithExistingField_ShouldReturnTrue() {
        // Given
        String key = "test:hash";
        String field = "field1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey(key, field)).thenReturn(true);

        // When
        boolean result = redisService.hexists(key, field);

        // Then
        assertTrue(result);
        verify(hashOperations).hasKey(key, field);
    }

    @Test
    void hexists_WithNonExistingField_ShouldReturnFalse() {
        // Given
        String key = "test:hash";
        String field = "field1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.hasKey(key, field)).thenReturn(false);

        // When
        boolean result = redisService.hexists(key, field);

        // Then
        assertFalse(result);
        verify(hashOperations).hasKey(key, field);
    }

    // ========== List Operations Tests ==========

    @Test
    void lpush_WithSingleValue_ShouldReturnListLength() {
        // Given
        String key = "test:list";
        String value = "value1";
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPush(key, value)).thenReturn(1L);

        // When
        long result = redisService.lpush(key, value);

        // Then
        assertEquals(1L, result);
        verify(listOperations).leftPush(key, value);
    }

    @Test
    void lpush_WithMultipleValues_ShouldReturnListLength() {
        // Given
        String key = "test:list";
        Object[] values = {"value1", "value2", "value3"};
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPushAll(key, values)).thenReturn(3L);

        // When
        long result = redisService.lpush(key, values);

        // Then
        assertEquals(3L, result);
        verify(listOperations).leftPushAll(key, values);
    }

    @Test
    void lpush_WithNullValues_ShouldThrowException() {
        // Given
        String key = "test:list";
        Object[] values = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.lpush(key, values)
        );
        assertEquals("Values cannot be null or empty", exception.getMessage());
    }

    @Test
    void lpush_WithEmptyValues_ShouldThrowException() {
        // Given
        String key = "test:list";
        Object[] values = {};

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.lpush(key, values)
        );
        assertEquals("Values cannot be null or empty", exception.getMessage());
    }

    @Test
    void rpop_WithNonEmptyList_ShouldReturnValue() {
        // Given
        String key = "test:list";
        String expectedValue = "value1";
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(key)).thenReturn(expectedValue);

        // When
        String result = redisService.rpop(key, String.class);

        // Then
        assertEquals(expectedValue, result);
        verify(listOperations).rightPop(key);
    }

    @Test
    void rpop_WithEmptyList_ShouldReturnNull() {
        // Given
        String key = "test:list";
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPop(key)).thenReturn(null);

        // When
        String result = redisService.rpop(key, String.class);

        // Then
        assertNull(result);
        verify(listOperations).rightPop(key);
    }

    @Test
    void rpop_WithNullType_ShouldThrowException() {
        // Given
        String key = "test:list";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.rpop(key, null)
        );
        assertEquals("Type cannot be null", exception.getMessage());
    }

    @Test
    void llen_WithValidKey_ShouldReturnLength() {
        // Given
        String key = "test:list";
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.size(key)).thenReturn(5L);

        // When
        long result = redisService.llen(key);

        // Then
        assertEquals(5L, result);
        verify(listOperations).size(key);
    }

    @Test
    void lrange_WithValidRange_ShouldReturnElements() {
        // Given
        String key = "test:list";
        long start = 0;
        long end = 2;
        List<Object> expectedList = List.of("value1", "value2", "value3");
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(key, start, end)).thenReturn(expectedList);

        // When
        List<Object> result = redisService.lrange(key, start, end);

        // Then
        assertEquals(expectedList, result);
        verify(listOperations).range(key, start, end);
    }

    @Test
    void lrange_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        String key = "test:list";
        long start = 0;
        long end = 2;
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(key, start, end)).thenReturn(null);

        // When
        List<Object> result = redisService.lrange(key, start, end);

        // Then
        assertTrue(result.isEmpty());
        verify(listOperations).range(key, start, end);
    }

    // ========== Batch Operations Tests ==========

    @Test
    void mset_WithValidKeyValueMap_ShouldSetAllKeys() {
        // Given
        Map<String, Object> keyValueMap = Map.of(
            "key1", "value1",
            "key2", "value2",
            "key3", "value3"
        );
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        redisService.mset(keyValueMap);

        // Then
        verify(valueOperations).multiSet(keyValueMap);
    }

    @Test
    void mset_WithNullMap_ShouldThrowException() {
        // Given
        Map<String, Object> keyValueMap = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mset(keyValueMap)
        );
        assertEquals("Key-value map cannot be null or empty", exception.getMessage());
    }

    @Test
    void mset_WithEmptyMap_ShouldThrowException() {
        // Given
        Map<String, Object> keyValueMap = new HashMap<>();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mset(keyValueMap)
        );
        assertEquals("Key-value map cannot be null or empty", exception.getMessage());
    }

    @Test
    void mget_WithValidKeys_ShouldReturnValues() {
        // Given
        List<String> keys = List.of("key1", "key2", "key3");
        List<Object> expectedValues = List.of("value1", "value2", "value3");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(expectedValues);

        // When
        List<Object> result = redisService.mget(keys);

        // Then
        assertEquals(expectedValues, result);
        verify(valueOperations).multiGet(keys);
    }

    @Test
    void mget_WithNullKeys_ShouldThrowException() {
        // Given
        List<String> keys = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mget(keys)
        );
        assertEquals("Keys list cannot be null or empty", exception.getMessage());
    }

    @Test
    void mget_WithEmptyKeys_ShouldThrowException() {
        // Given
        List<String> keys = List.of();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mget(keys)
        );
        assertEquals("Keys list cannot be null or empty", exception.getMessage());
    }

    @Test
    void mdel_WithValidKeys_ShouldReturnDeletedCount() {
        // Given
        List<String> keys = List.of("key1", "key2", "key3");
        when(redisTemplate.delete(keys)).thenReturn(2L);

        // When
        long result = redisService.mdel(keys);

        // Then
        assertEquals(2L, result);
        verify(redisTemplate).delete(keys);
    }

    @Test
    void mdel_WithNullKeys_ShouldThrowException() {
        // Given
        List<String> keys = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mdel(keys)
        );
        assertEquals("Keys list cannot be null or empty", exception.getMessage());
    }

    @Test
    void mexists_WithValidKeys_ShouldReturnExistenceMap() {
        // Given
        List<String> keys = List.of("key1", "key2", "key3");
        when(redisTemplate.hasKey("key1")).thenReturn(true);
        when(redisTemplate.hasKey("key2")).thenReturn(false);
        when(redisTemplate.hasKey("key3")).thenReturn(true);

        // When
        Map<String, Boolean> result = redisService.mexists(keys);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.get("key1"));
        assertFalse(result.get("key2"));
        assertTrue(result.get("key3"));
        verify(redisTemplate).hasKey("key1");
        verify(redisTemplate).hasKey("key2");
        verify(redisTemplate).hasKey("key3");
    }

    @Test
    void mexists_WithNullKeys_ShouldThrowException() {
        // Given
        List<String> keys = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mexists(keys)
        );
        assertEquals("Keys list cannot be null or empty", exception.getMessage());
    }

    // ========== Additional Tests for Mutation Coverage ==========

    @Test
    void setWithTtl_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        String value = "test value";
        Duration ttl = Duration.ofMinutes(5);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.set(key, value, ttl)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void setExpire_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        Duration ttl = Duration.ofMinutes(5);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.setExpire(key, ttl)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void hget_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        String field = "field1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hget(key, field, String.class)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void hdel_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        String field = "field1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hdel(key, field)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void hgetAll_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hgetAll(key)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void hexists_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        String field = "field1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hexists(key, field)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void lpush_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        String value = "value1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.lpush(key, value)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void lpush_WithMultipleValues_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";
        Object[] values = {"value1", "value2"};

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.lpush(key, values)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void rpop_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.rpop(key, String.class)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void llen_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.llen(key)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void lrange_WithWhitespaceKey_ShouldThrowException() {
        // Given
        String key = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.lrange(key, 0, 5)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void mset_WithInvalidKeyInMap_ShouldThrowException() {
        // Given
        Map<String, Object> keyValueMap = Map.of(
            "validKey", "value1",
            "   ", "value2"  // whitespace key
        );

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mset(keyValueMap)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void mget_WithInvalidKeyInList_ShouldThrowException() {
        // Given
        List<String> keys = List.of("validKey", "   ");  // whitespace key

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mget(keys)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void mdel_WithInvalidKeyInList_ShouldThrowException() {
        // Given
        List<String> keys = List.of("validKey", "   ");  // whitespace key

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mdel(keys)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    @Test
    void mexists_WithInvalidKeyInList_ShouldThrowException() {
        // Given
        List<String> keys = List.of("validKey", "   ");  // whitespace key

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.mexists(keys)
        );
        assertEquals("Key cannot be null or empty", exception.getMessage());
    }

    // ========== Tests for Metrics Service Calls ==========

    @Test
    void set_WithException_ShouldRecordMetricsError() {
        // Given
        String key = "test:key";
        String value = "test value";
        RuntimeException testException = new RuntimeException("Redis error");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        doThrow(testException).when(exceptionHandler).handleRedisOperation(any(Runnable.class));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.set(key, value)
        );
        
        assertEquals("Redis error", exception.getMessage());
        verify(metricsService).recordCacheError("set", testException);
        verify(metricsService).recordSetOperation(any(Duration.class), eq(false));
    }

    @Test
    void setWithTtl_WithException_ShouldRecordMetricsError() {
        // Given
        String key = "test:key";
        String value = "test value";
        Duration ttl = Duration.ofMinutes(5);
        RuntimeException testException = new RuntimeException("Redis error");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        doThrow(testException).when(exceptionHandler).handleRedisOperation(any(Runnable.class));

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.set(key, value, ttl)
        );
        
        assertEquals("Redis error", exception.getMessage());
        verify(metricsService).recordCacheError("set", testException);
        verify(metricsService).recordSetOperation(any(Duration.class), eq(false));
    }

    @Test
    void get_WithException_ShouldRecordMetricsError() {
        // Given
        String key = "test:key";
        RuntimeException testException = new RuntimeException("Redis error");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        when(exceptionHandler.handleRedisOperation(any(Supplier.class), any(Supplier.class)))
            .thenThrow(testException);

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.get(key, String.class)
        );
        
        assertEquals("Redis error", exception.getMessage());
        verify(metricsService).recordCacheError("get", testException);
        verify(metricsService).recordGetOperation(any(Duration.class), eq(false));
    }

    @Test
    void get_WithSuccessfulHit_ShouldRecordCacheHit() {
        // Given
        String key = "test:key";
        String expectedValue = "test value";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(expectedValue);

        // When
        String result = redisService.get(key, String.class);

        // Then
        assertEquals(expectedValue, result);
        verify(metricsService).recordCacheHit(key);
        verify(metricsService, never()).recordCacheMiss(key);
        verify(metricsService).recordGetOperation(any(Duration.class), eq(true));
    }

    @Test
    void get_WithCacheMiss_ShouldRecordCacheMiss() {
        // Given
        String key = "test:key";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(key)).thenReturn(null);

        // When
        String result = redisService.get(key, String.class);

        // Then
        assertNull(result);
        verify(metricsService).recordCacheMiss(key);
        verify(metricsService, never()).recordCacheHit(key);
        verify(metricsService).recordGetOperation(any(Duration.class), eq(true));
    }

    @Test
    void delete_WithException_ShouldRecordMetricsError() {
        // Given
        String key = "test:key";
        RuntimeException testException = new RuntimeException("Redis error");
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        when(exceptionHandler.handleRedisOperation(any(Supplier.class), any(Supplier.class)))
            .thenThrow(testException);

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.delete(key)
        );
        
        assertEquals("Redis error", exception.getMessage());
        verify(metricsService).recordCacheError("delete", testException);
        verify(metricsService).recordDeleteOperation(any(Duration.class), eq(false));
    }

    @Test
    void delete_WithSuccess_ShouldRecordMetrics() {
        // Given
        String key = "test:key";
        when(redisTemplate.delete(key)).thenReturn(true);

        // When
        boolean result = redisService.delete(key);

        // Then
        assertTrue(result);
        verify(metricsService).recordDeleteOperation(any(Duration.class), eq(true));
        verify(metricsService, never()).recordCacheError(eq("delete"), any());
    }

    @Test
    void exists_WithException_ShouldRecordMetricsError() {
        // Given
        String key = "test:key";
        RuntimeException testException = new RuntimeException("Redis error");
        
        // Reset and configure exception handler to throw exception
        reset(exceptionHandler);
        when(exceptionHandler.handleRedisOperation(any(Supplier.class), any(Supplier.class)))
            .thenThrow(testException);

        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> redisService.exists(key)
        );
        
        assertEquals("Redis error", exception.getMessage());
        verify(metricsService).recordCacheError("exists", testException);
        verify(metricsService).recordExistsOperation(any(Duration.class), eq(false));
    }

    @Test
    void exists_WithSuccess_ShouldRecordMetrics() {
        // Given
        String key = "test:key";
        when(redisTemplate.hasKey(key)).thenReturn(true);

        // When
        boolean result = redisService.exists(key);

        // Then
        assertTrue(result);
        verify(metricsService).recordExistsOperation(any(Duration.class), eq(true));
        verify(metricsService, never()).recordCacheError(eq("exists"), any());
    }

    @Test
    void set_WithSuccess_ShouldRecordMetrics() {
        // Given
        String key = "test:key";
        String value = "test value";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        redisService.set(key, value);

        // Then
        verify(metricsService).recordSetOperation(any(Duration.class), eq(true));
        verify(metricsService, never()).recordCacheError(eq("set"), any());
    }

    @Test
    void setWithTtl_WithSuccess_ShouldRecordMetrics() {
        // Given
        String key = "test:key";
        String value = "test value";
        Duration ttl = Duration.ofMinutes(5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        redisService.set(key, value, ttl);

        // Then
        verify(metricsService).recordSetOperation(any(Duration.class), eq(true));
        verify(metricsService, never()).recordCacheError(eq("set"), any());
    }

    // ========== Additional Edge Cases ==========

    @Test
    void keys_WithWhitespacePattern_ShouldThrowException() {
        // Given
        String pattern = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.keys(pattern)
        );
        assertEquals("Pattern cannot be null or empty", exception.getMessage());
    }

    @Test
    void deleteByPattern_WithWhitespacePattern_ShouldThrowException() {
        // Given
        String pattern = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.deleteByPattern(pattern)
        );
        assertEquals("Pattern cannot be null or empty", exception.getMessage());
    }

    @Test
    void hset_WithWhitespaceField_ShouldThrowException() {
        // Given
        String key = "test:hash";
        String field = "   ";
        String value = "value1";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hset(key, field, value)
        );
        assertEquals("Field cannot be null or empty", exception.getMessage());
    }

    @Test
    void hget_WithWhitespaceField_ShouldThrowException() {
        // Given
        String key = "test:hash";
        String field = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hget(key, field, String.class)
        );
        assertEquals("Field cannot be null or empty", exception.getMessage());
    }

    @Test
    void hdel_WithWhitespaceField_ShouldThrowException() {
        // Given
        String key = "test:hash";
        String field = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hdel(key, field)
        );
        assertEquals("Field cannot be null or empty", exception.getMessage());
    }

    @Test
    void hexists_WithWhitespaceField_ShouldThrowException() {
        // Given
        String key = "test:hash";
        String field = "   ";

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> redisService.hexists(key, field)
        );
        assertEquals("Field cannot be null or empty", exception.getMessage());
    }

    @Test
    void deleteByPattern_WithNullKeysReturned_ShouldReturnZero() {
        // Given
        String pattern = "test:*";
        when(redisTemplate.keys(pattern)).thenReturn(null);

        // When
        long result = redisService.deleteByPattern(pattern);

        // Then
        assertEquals(0L, result);
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate, never()).delete(any(Set.class));
    }

    @Test
    void hdel_WithNullResult_ShouldReturnFalse() {
        // Given
        String key = "test:hash";
        String field = "field1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.delete(key, field)).thenReturn(null);

        // When
        boolean result = redisService.hdel(key, field);

        // Then
        assertFalse(result);
        verify(hashOperations).delete(key, field);
    }

    @Test
    void lpush_WithNullResult_ShouldReturnZero() {
        // Given
        String key = "test:list";
        String value = "value1";
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPush(key, value)).thenReturn(null);

        // When
        long result = redisService.lpush(key, value);

        // Then
        assertEquals(0L, result);
        verify(listOperations).leftPush(key, value);
    }

    @Test
    void lpush_WithMultipleValues_WithNullResult_ShouldReturnZero() {
        // Given
        String key = "test:list";
        Object[] values = {"value1", "value2"};
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPushAll(key, values)).thenReturn(null);

        // When
        long result = redisService.lpush(key, values);

        // Then
        assertEquals(0L, result);
        verify(listOperations).leftPushAll(key, values);
    }

    @Test
    void llen_WithNullResult_ShouldReturnZero() {
        // Given
        String key = "test:list";
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.size(key)).thenReturn(null);

        // When
        long result = redisService.llen(key);

        // Then
        assertEquals(0L, result);
        verify(listOperations).size(key);
    }

    @Test
    void mget_WithNullResult_ShouldReturnEmptyList() {
        // Given
        List<String> keys = List.of("key1", "key2");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.multiGet(keys)).thenReturn(null);

        // When
        List<Object> result = redisService.mget(keys);

        // Then
        assertTrue(result.isEmpty());
        verify(valueOperations).multiGet(keys);
    }

    @Test
    void mdel_WithNullResult_ShouldReturnZero() {
        // Given
        List<String> keys = List.of("key1", "key2");
        when(redisTemplate.delete(keys)).thenReturn(null);

        // When
        long result = redisService.mdel(keys);

        // Then
        assertEquals(0L, result);
        verify(redisTemplate).delete(keys);
    }
}