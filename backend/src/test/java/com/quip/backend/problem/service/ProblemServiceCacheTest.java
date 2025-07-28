package com.quip.backend.problem.service;

import com.quip.backend.config.redis.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProblemService caching functionality.
 * Tests verify that caching annotations are properly configured without requiring database connections.
 */
@ExtendWith(MockitoExtension.class)
class ProblemServiceCacheTest {

    @Test
    void testGetProblemsByCategory_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ProblemService.class.getMethod(
                "getProblemsByCategory", 
                com.quip.backend.problem.dto.request.GetProblemRequestDto.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.TEMPORARY_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match TEMPORARY_DATA_CACHE");
            
            assertEquals("'problems:category:' + #getProblemRequestDto.problemCategoryId", cacheableAnnotation.key(),
                "Should use problemCategoryId in cache key");
        });
    }

    @Test
    void testGetProblemsByCategoryId_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ProblemService.class.getMethod(
                "getProblemsByCategoryId", Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.TEMPORARY_DATA_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match TEMPORARY_DATA_CACHE");
        });
    }

    @Test
    void testAddProblem_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ProblemService.class.getMethod(
                "addProblem", 
                com.quip.backend.problem.dto.request.CreateProblemRequestDto.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TEMPORARY_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TEMPORARY_DATA_CACHE");
        });
    }

    @Test
    void testEvictProblemsCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ProblemService.class.getMethod(
                "evictProblemsCache", Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TEMPORARY_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TEMPORARY_DATA_CACHE");
        });
    }

    @Test
    void testEvictAllProblemsCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation with allEntries=true
        assertDoesNotThrow(() -> {
            var method = ProblemService.class.getMethod(
                "evictAllProblemsCache");
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.TEMPORARY_DATA_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match TEMPORARY_DATA_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testServiceHasRequiredCachingMethods() {
        // Verify that all required caching methods exist
        assertDoesNotThrow(() -> {
            // Cacheable methods
            ProblemService.class.getMethod("getProblemsByCategory", 
                com.quip.backend.problem.dto.request.GetProblemRequestDto.class);
            ProblemService.class.getMethod("getProblemsByCategoryId", Long.class);
            
            // CacheEvict methods
            ProblemService.class.getMethod("addProblem", 
                com.quip.backend.problem.dto.request.CreateProblemRequestDto.class);
            ProblemService.class.getMethod("evictProblemsCache", Long.class);
            ProblemService.class.getMethod("evictAllProblemsCache");
        });
    }
}