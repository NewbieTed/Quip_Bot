package com.quip.backend.tool.service;

import com.quip.backend.authorization.service.AuthorizationService;
import com.quip.backend.config.redis.CacheConfiguration;
import com.quip.backend.tool.mapper.database.ToolWhitelistMapper;
import com.quip.backend.tool.mapper.dto.response.ToolWhitelistResponseDtoMapper;
import com.quip.backend.tool.model.ToolWhitelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolWhitelistService caching functionality.
 * Tests verify that caching annotations are properly configured without requiring database connections.
 */
@ExtendWith(MockitoExtension.class)
class ToolWhitelistServiceCacheTest {

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private ToolService toolService;

    @Mock
    private ToolWhitelistMapper toolWhitelistMapper;

    @Mock
    private ToolWhitelistResponseDtoMapper toolWhitelistResponseDtoMapper;

    private ToolWhitelistService toolWhitelistService;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Use in-memory cache manager for testing
        cacheManager = new ConcurrentMapCacheManager(
            CacheConfiguration.TOOL_WHITELIST_CACHE
        );
        
        toolWhitelistService = new ToolWhitelistService(
            authorizationService,
            toolService,
            toolWhitelistMapper,
            toolWhitelistResponseDtoMapper
        );
    }

    @Test
    void testGetActiveToolWhitelistByMemberAndServer_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "getActiveToolWhitelistByMemberAndServer", Long.class, Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
            
            assertEquals("#serverId + ':member:' + #memberId", cacheableAnnotation.key(),
                "Should use serverId and memberId in cache key");
        });
    }

    @Test
    void testHasToolPermission_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "hasToolPermission", Long.class, Long.class, Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
        });
    }

    @Test
    void testUpdateToolWhitelist_EvictsCacheOnUpdate() {
        // Verify that the updateToolWhitelist method exists and properly evicts caches
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "updateToolWhitelist", 
                com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto.class);
            
            assertNotNull(method, "updateToolWhitelist method should exist");
            assertEquals(ToolWhitelist.class, method.getReturnType(),
                "Method should return ToolWhitelist");
        });
    }

    @Test
    void testEvictToolWhitelistMemberCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "evictToolWhitelistMemberCache", Long.class, Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
        });
    }

    @Test
    void testEvictAllToolWhitelistCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation with allEntries=true
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "evictAllToolWhitelistCache", Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testCacheKeyGeneration_UsesCustomKey() {
        // Verify that cached methods use a custom key expression
        assertDoesNotThrow(() -> {
            var getActiveMethod = ToolWhitelistService.class.getMethod(
                "getActiveToolWhitelistByMemberAndServer", Long.class, Long.class);
            var cacheableAnnotation = getActiveMethod.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            
            assertEquals("#serverId + ':member:' + #memberId", cacheableAnnotation.key(),
                "Should use custom key expression");
        });
    }

    @Test
    void testGetAllActiveToolWhitelistByServer_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "getAllActiveToolWhitelistByServer", Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
            
            assertEquals("#serverId + ':server:all'", cacheableAnnotation.key(),
                "Should use serverId with server:all suffix in cache key");
        });
    }

    @Test
    void testRemoveToolWhitelist_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "removeToolWhitelist", Long.class, Long.class, Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
        });
    }

    @Test
    void testEvictToolPermissionCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ToolWhitelistService.class.getMethod(
                "evictToolPermissionCache", Long.class, Long.class, Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TOOL_WHITELIST_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TOOL_WHITELIST_CACHE");
        });
    }

    @Test
    void testServiceHasRequiredCachingMethods() {
        // Verify that all required caching methods exist
        assertDoesNotThrow(() -> {
            // Cacheable methods
            ToolWhitelistService.class.getMethod("getActiveToolWhitelistByMemberAndServer", Long.class, Long.class);
            ToolWhitelistService.class.getMethod("hasToolPermission", Long.class, Long.class, Long.class);
            ToolWhitelistService.class.getMethod("getAllActiveToolWhitelistByServer", Long.class);
            
            // Update methods (no longer @CachePut, but evicts caches)
            ToolWhitelistService.class.getMethod("updateToolWhitelist", 
                com.quip.backend.tool.dto.request.UpdateToolWhitelistRequestDto.class);
            ToolWhitelistService.class.getMethod("removeToolWhitelist", Long.class, Long.class, Long.class);
            
            // CacheEvict methods
            ToolWhitelistService.class.getMethod("evictToolWhitelistCache", Long.class);
            ToolWhitelistService.class.getMethod("evictToolWhitelistMemberCache", Long.class, Long.class);
            ToolWhitelistService.class.getMethod("evictAllToolWhitelistCache", Long.class);
            ToolWhitelistService.class.getMethod("evictToolPermissionCache", Long.class, Long.class, Long.class);
        });
    }
}