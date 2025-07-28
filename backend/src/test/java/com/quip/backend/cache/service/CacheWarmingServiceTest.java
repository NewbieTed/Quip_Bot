package com.quip.backend.cache.service;

import com.quip.backend.problem.service.ProblemCategoryService;
import com.quip.backend.server.mapper.database.ServerMapper;
import com.quip.backend.server.model.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CacheWarmingService.
 * Tests verify cache warming functionality without requiring actual Redis or database connections.
 */
@ExtendWith(MockitoExtension.class)
class CacheWarmingServiceTest {

    @Mock
    private ProblemCategoryService problemCategoryService;

    @Mock
    private ServerMapper serverMapper;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @InjectMocks
    private CacheWarmingService cacheWarmingService;

    private Server testServer1;
    private Server testServer2;

    @BeforeEach
    void setUp() {
        testServer1 = new Server();
        testServer1.setId(1L);
        testServer1.setServerName("Test Server 1");

        testServer2 = new Server();
        testServer2.setId(2L);
        testServer2.setServerName("Test Server 2");
    }

    @Test
    void testWarmCaches_Success() {
        // Given
        List<Server> servers = Arrays.asList(testServer1, testServer2);
        when(serverMapper.selectList(null)).thenReturn(servers);

        // When
        cacheWarmingService.warmCaches();

        // Then
        verify(serverMapper).selectList(null);
        verify(problemCategoryService).getProblemCategoriesByServerId(1L);
        verify(problemCategoryService).getProblemCategoriesByServerId(2L);
    }

    @Test
    void testWarmCaches_NoServers() {
        // Given
        when(serverMapper.selectList(null)).thenReturn(Collections.emptyList());

        // When
        cacheWarmingService.warmCaches();

        // Then
        verify(serverMapper).selectList(null);
        verify(problemCategoryService, never()).getProblemCategoriesByServerId(any());
    }

    @Test
    void testWarmCaches_NullServers() {
        // Given
        when(serverMapper.selectList(null)).thenReturn(null);

        // When
        cacheWarmingService.warmCaches();

        // Then
        verify(serverMapper).selectList(null);
        verify(problemCategoryService, never()).getProblemCategoriesByServerId(any());
    }

    @Test
    void testWarmCaches_DatabaseException() {
        // Given
        when(serverMapper.selectList(null)).thenThrow(new RuntimeException("Database error"));

        // When - should not throw exception
        assertDoesNotThrow(() -> cacheWarmingService.warmCaches());

        // Then
        verify(serverMapper).selectList(null);
        verify(problemCategoryService, never()).getProblemCategoriesByServerId(any());
    }

    @Test
    void testWarmCaches_PartialFailure() {
        // Given
        List<Server> servers = Arrays.asList(testServer1, testServer2);
        when(serverMapper.selectList(null)).thenReturn(servers);
        when(problemCategoryService.getProblemCategoriesByServerId(1L)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Cache error")).when(problemCategoryService).getProblemCategoriesByServerId(2L);

        // When - should not throw exception
        assertDoesNotThrow(() -> cacheWarmingService.warmCaches());

        // Then
        verify(serverMapper).selectList(null);
        verify(problemCategoryService).getProblemCategoriesByServerId(1L);
        verify(problemCategoryService).getProblemCategoriesByServerId(2L);
    }

    @Test
    void testWarmProblemCategoriesCacheForServer_Success() {
        // Given
        Long serverId = 1L;

        // When
        cacheWarmingService.warmProblemCategoriesCacheForServer(serverId);

        // Then
        verify(problemCategoryService).getProblemCategoriesByServerId(serverId);
    }

    @Test
    void testWarmProblemCategoriesCacheForServer_NullServerId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            cacheWarmingService.warmProblemCategoriesCacheForServer(null));
        
        verify(problemCategoryService, never()).getProblemCategoriesByServerId(any());
    }

    @Test
    void testWarmProblemCategoriesCacheForServer_ServiceException() {
        // Given
        Long serverId = 1L;
        doThrow(new RuntimeException("Service error")).when(problemCategoryService).getProblemCategoriesByServerId(serverId);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            cacheWarmingService.warmProblemCategoriesCacheForServer(serverId));
        
        verify(problemCategoryService).getProblemCategoriesByServerId(serverId);
    }

    @Test
    void testManualWarmProblemCategoriesCache() {
        // Given
        List<Server> servers = Arrays.asList(testServer1, testServer2);
        when(serverMapper.selectList(null)).thenReturn(servers);

        // When
        cacheWarmingService.manualWarmProblemCategoriesCache();

        // Then
        verify(serverMapper).selectList(null);
        verify(problemCategoryService).getProblemCategoriesByServerId(1L);
        verify(problemCategoryService).getProblemCategoriesByServerId(2L);
    }

    @Test
    void testServiceHasRequiredMethods() {
        // Verify that all required methods exist
        assertDoesNotThrow(() -> {
            CacheWarmingService.class.getMethod("warmCaches");
            CacheWarmingService.class.getMethod("manualWarmProblemCategoriesCache");
            CacheWarmingService.class.getMethod("warmProblemCategoriesCacheForServer", Long.class);
        });
    }

    @Test
    void testWarmCachesHasCorrectAnnotations() {
        // Verify that the warmCaches method has the correct annotations
        assertDoesNotThrow(() -> {
            var method = CacheWarmingService.class.getMethod("warmCaches");
            
            var eventListenerAnnotation = method.getAnnotation(org.springframework.context.event.EventListener.class);
            assertNotNull(eventListenerAnnotation, "Method should have @EventListener annotation");
            
            assertEquals(ApplicationReadyEvent.class, eventListenerAnnotation.value()[0],
                "Should listen for ApplicationReadyEvent");
            
            var asyncAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Async.class);
            assertNotNull(asyncAnnotation, "Method should have @Async annotation");
        });
    }
}