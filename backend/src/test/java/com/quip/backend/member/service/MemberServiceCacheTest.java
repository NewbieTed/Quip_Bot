package com.quip.backend.member.service;

import com.quip.backend.config.redis.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemberService caching functionality.
 * Tests verify that caching annotations are properly configured without requiring database connections.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceCacheTest {

    @Test
    void testValidateMember_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = MemberService.class.getMethod(
                "validateMember", Long.class, String.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.MEMBER_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match MEMBER_DATA_CACHE");
            
            assertEquals("#memberId", cacheableAnnotation.key(),
                "Should use memberId as cache key");
        });
    }

    @Test
    void testGetMemberById_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = MemberService.class.getMethod(
                "getMemberById", Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.MEMBER_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match MEMBER_DATA_CACHE");
        });
    }

    @Test
    void testEvictMemberCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = MemberService.class.getMethod(
                "evictMemberCache", Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.MEMBER_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match MEMBER_DATA_CACHE");
        });
    }

    @Test
    void testEvictAllMemberCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation with allEntries=true
        assertDoesNotThrow(() -> {
            var method = MemberService.class.getMethod(
                "evictAllMemberCache");
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.MEMBER_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match MEMBER_DATA_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testServiceHasRequiredCachingMethods() {
        // Verify that all required caching methods exist
        assertDoesNotThrow(() -> {
            // Cacheable methods
            MemberService.class.getMethod("validateMember", Long.class, String.class);
            MemberService.class.getMethod("getMemberById", Long.class);
            
            // CacheEvict methods
            MemberService.class.getMethod("evictMemberCache", Long.class);
            MemberService.class.getMethod("evictAllMemberCache");
        });
    }
}