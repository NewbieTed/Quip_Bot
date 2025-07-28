package com.quip.backend.problem.service;

import com.quip.backend.config.redis.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProblemCategoryService caching functionality.
 * Tests verify that caching annotations are properly configured without requiring database connections.
 */
@ExtendWith(MockitoExtension.class)
class ProblemCategoryServiceCacheTest {

    @Test
    void testGetServerProblemCategories_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ProblemCategoryService.class.getMethod(
                "getServerProblemCategories", 
                com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.PROBLEM_CATEGORIES_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match PROBLEM_CATEGORIES_CACHE");
            
            assertEquals("#getProblemCategoryRequestDto.channelId", cacheableAnnotation.key(),
                "Should use channelId as cache key");
        });
    }

    @Test
    void testGetProblemCategoriesByServerId_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ProblemCategoryService.class.getMethod(
                "getProblemCategoriesByServerId", Long.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.PROBLEM_CATEGORIES_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match PROBLEM_CATEGORIES_CACHE");
        });
    }

    @Test
    void testValidateProblemCategory_CacheableAnnotationExists() {
        // Verify that the method has @Cacheable annotation
        assertDoesNotThrow(() -> {
            var method = ProblemCategoryService.class.getMethod(
                "validateProblemCategory", Long.class, String.class);
            
            var cacheableAnnotation = method.getAnnotation(org.springframework.cache.annotation.Cacheable.class);
            assertNotNull(cacheableAnnotation, "Method should have @Cacheable annotation");
            
            assertEquals(CacheConfiguration.PROBLEM_CATEGORIES_CACHE, cacheableAnnotation.value()[0],
                "Cache name should match PROBLEM_CATEGORIES_CACHE");
        });
    }

    @Test
    void testAddProblemCategory_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ProblemCategoryService.class.getMethod(
                "addProblemCategory", 
                com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.PROBLEM_CATEGORIES_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match PROBLEM_CATEGORIES_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testEvictProblemCategoriesCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation
        assertDoesNotThrow(() -> {
            var method = ProblemCategoryService.class.getMethod(
                "evictProblemCategoriesCache", Long.class);
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.PROBLEM_CATEGORIES_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match PROBLEM_CATEGORIES_CACHE");
        });
    }

    @Test
    void testEvictAllProblemCategoriesCache_CacheEvictAnnotationExists() {
        // Verify that the method has @CacheEvict annotation with allEntries=true
        assertDoesNotThrow(() -> {
            var method = ProblemCategoryService.class.getMethod(
                "evictAllProblemCategoriesCache");
            
            var cacheEvictAnnotation = method.getAnnotation(org.springframework.cache.annotation.CacheEvict.class);
            assertNotNull(cacheEvictAnnotation, "Method should have @CacheEvict annotation");
            
            assertEquals(CacheConfiguration.PROBLEM_CATEGORIES_CACHE, cacheEvictAnnotation.value()[0],
                "Cache name should match PROBLEM_CATEGORIES_CACHE");
            
            assertTrue(cacheEvictAnnotation.allEntries(),
                "Should evict all entries");
        });
    }

    @Test
    void testServiceHasRequiredCachingMethods() {
        // Verify that all required caching methods exist
        assertDoesNotThrow(() -> {
            // Cacheable methods
            ProblemCategoryService.class.getMethod("getServerProblemCategories", 
                com.quip.backend.problem.dto.request.GetProblemCategoryRequestDto.class);
            ProblemCategoryService.class.getMethod("getProblemCategoriesByServerId", Long.class);
            ProblemCategoryService.class.getMethod("validateProblemCategory", Long.class, String.class);
            
            // CacheEvict methods
            ProblemCategoryService.class.getMethod("addProblemCategory", 
                com.quip.backend.problem.dto.request.CreateProblemCategoryRequestDto.class);
            ProblemCategoryService.class.getMethod("evictProblemCategoriesCache", Long.class);
            ProblemCategoryService.class.getMethod("evictAllProblemCategoriesCache");
        });
    }
}