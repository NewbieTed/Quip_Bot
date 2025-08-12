package com.quip.backend.server.service;

import com.quip.backend.config.redis.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ServerService caching functionality.
 * Tests verify that caching annotations are properly configured without requiring database connections.
 */
@ExtendWith(MockitoExtension.class)
class ServerServiceCacheTest {

    @Test
    void testValidateServer_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ServerService.class.getMethod(
                "validateServer", Long.class, String.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.SERVER_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match SERVER_DATA_CACHE");
            
            assertEquals("#serverId", cacheableAnnotation.key(),
                "Should use serverId as cache key");
        });
    }

    @Test
    void testAssertServerExist_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ServerService.class.getMethod(
                "assertServerExist", Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.SERVER_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match SERVER_DATA_CACHE");
        });
    }

    @Test
    void testGetServerById_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ServerService.class.getMethod(
                "getServerById", Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.SERVER_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match SERVER_DATA_CACHE");
        });
    }

    @Test
    void testEvictServerCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ServerService.class.getMethod(
                "evictServerCache", Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.SERVER_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match SERVER_DATA_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testEvictAllServerCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation with allEntries=true
        assertDoesNotThrow(() -> {
            var method = ServerService.class.getMethod(
                "evictAllServerCache");
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.SERVER_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match SERVER_DATA_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testServiceHasRequiredCachingMethods() {
        // Verify that all required caching methods exist
        assertDoesNotThrow(() -> {
            // Cacheable methods
            ServerService.class.getMethod("validateServer", Long.class, String.class);
            ServerService.class.getMethod("assertServerExist", Long.class);
            ServerService.class.getMethod("getServerById", Long.class);
            
            // CacheEvict methods
            ServerService.class.getMethod("evictServerCache", Long.class);
            ServerService.class.getMethod("evictAllServerCache");
        });
    }
}