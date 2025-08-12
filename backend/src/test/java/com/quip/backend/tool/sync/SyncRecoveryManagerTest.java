package com.quip.backend.tool.sync;

import com.quip.backend.redis.service.RedisService;
import com.quip.backend.tool.model.ToolInfo;
import com.quip.backend.tool.model.ToolInventoryResponse;
import com.quip.backend.tool.model.ToolResyncRequest;
import com.quip.backend.tool.monitoring.ToolSyncMetricsService;
import com.quip.backend.tool.service.ToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncRecoveryManagerTest {

    @Mock
    private RedisService redisService;

    @Mock
    private ToolService toolService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ToolSyncMetricsService metricsService;

    @InjectMocks
    private SyncRecoveryManager syncRecoveryManager;

    private static final String AGENT_BASE_URL = "http://localhost:5001";
    private static final String TOOLS_UPDATES_KEY = "tools:updates";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncRecoveryManager, "agentBaseUrl", AGENT_BASE_URL);
        ReflectionTestUtils.setField(syncRecoveryManager, "recoveryEnabled", true);
        ReflectionTestUtils.setField(syncRecoveryManager, "recoveryTimeoutMs", 10000);
        ReflectionTestUtils.setField(syncRecoveryManager, "maxRetryAttempts", 3);
        ReflectionTestUtils.setField(syncRecoveryManager, "initialRetryDelayMs", 1000L);
        ReflectionTestUtils.setField(syncRecoveryManager, "resyncEndpoint", "/api/tools/resync");
    }

    @Test
    void triggerResync_Success_ShouldReturnTrue() {
        // Arrange
        String reason = "message_processing_failure";
        List<ToolInfo> tools = Arrays.asList(
            new ToolInfo("tool1", "built-in"),
            new ToolInfo("tool2", "built-in"),
            new ToolInfo("tool3", "QuipMCPServer")
        );
        
        ToolInventoryResponse mockResponse = new ToolInventoryResponse();
        mockResponse.setRequestId("test-request-id");
        mockResponse.setCurrentTools(tools);
        mockResponse.setTimestamp(Instant.now());
        mockResponse.setDiscoveryTimestamp(Instant.now().minusSeconds(5));

        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(5L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));
        doNothing().when(toolService).syncToolsFromInventoryWithServerInfo(anyList());

        // Act
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertTrue(result);
        verify(redisService).llen(TOOLS_UPDATES_KEY);
        verify(redisService).delete(TOOLS_UPDATES_KEY);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class));
    }

    @Test
    void triggerResync_RecoveryDisabled_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(syncRecoveryManager, "recoveryEnabled", false);
        String reason = "test_reason";

        // Act
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertFalse(result);
        verifyNoInteractions(redisService, restTemplate);
    }

    @Test
    void triggerResync_RecoveryInProgress_ShouldReturnFalse() {
        // Arrange
        String reason = "test_reason";
        
        // Start first recovery (simulate in progress)
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenAnswer(invocation -> {
                    // Simulate long-running operation
                    Thread.sleep(100);
                    ToolInventoryResponse response = new ToolInventoryResponse();
                    response.setCurrentTools(Arrays.asList(new ToolInfo("tool1", "built-in")));
                    return new ResponseEntity<>(response, HttpStatus.OK);
                });

        // Act - start first recovery in separate thread
        Thread firstRecovery = new Thread(() -> syncRecoveryManager.triggerResync(reason));
        firstRecovery.start();
        
        // Give first recovery time to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Try to start second recovery
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertFalse(result);
        
        // Wait for first recovery to complete
        try {
            firstRecovery.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void triggerResync_AgentRequestFails_ShouldReturnFalse() {
        // Arrange
        String reason = "test_reason";
        
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenThrow(new RestClientException("Connection failed"));

        // Act
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertFalse(result);
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class));
    }

    @Test
    void clearRedisQueue_Success_ShouldReturnTrue() {
        // Arrange
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(5L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);

        // Act
        boolean result = syncRecoveryManager.clearRedisQueue();

        // Assert
        assertTrue(result);
        verify(redisService).llen(TOOLS_UPDATES_KEY);
        verify(redisService).delete(TOOLS_UPDATES_KEY);
    }

    @Test
    void clearRedisQueue_EmptyQueue_ShouldReturnTrue() {
        // Arrange
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(false);

        // Act
        boolean result = syncRecoveryManager.clearRedisQueue();

        // Assert
        assertTrue(result);
        verify(redisService).llen(TOOLS_UPDATES_KEY);
        verify(redisService).delete(TOOLS_UPDATES_KEY);
    }

    @Test
    void clearRedisQueue_RedisError_ShouldReturnFalse() {
        // Arrange
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        boolean result = syncRecoveryManager.clearRedisQueue();

        // Assert
        assertFalse(result);
        verify(redisService).llen(TOOLS_UPDATES_KEY);
        verify(redisService, never()).delete(TOOLS_UPDATES_KEY);
    }

    @Test
    void triggerResync_ValidatesHttpRequest() {
        // Arrange
        String reason = "validation_test";
        List<ToolInfo> tools = Arrays.asList(
            new ToolInfo("tool1", "built-in"),
            new ToolInfo("tool2", "built-in")
        );
        
        ToolInventoryResponse mockResponse = new ToolInventoryResponse();
        mockResponse.setCurrentTools(tools);

        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        syncRecoveryManager.triggerResync(reason);

        // Assert
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(ToolInventoryResponse.class)
        );

        // Verify URL
        assertEquals(AGENT_BASE_URL + "/api/tools/resync", urlCaptor.getValue());

        // Verify request body
        HttpEntity<ToolResyncRequest> capturedEntity = entityCaptor.getValue();
        ToolResyncRequest request = capturedEntity.getBody();
        assertNotNull(request);
        assertNotNull(request.getRequestId());
        assertNotNull(request.getTimestamp());
        assertEquals(reason, request.getReason());
    }

    @Test
    void triggerResync_RetryLogic_ShouldRetryOnFailure() {
        // Arrange
        String reason = "retry_test";
        List<ToolInfo> tools = Arrays.asList(new ToolInfo("tool1", "built-in"));
        
        ToolInventoryResponse mockResponse = new ToolInventoryResponse();
        mockResponse.setCurrentTools(tools);

        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenThrow(new RestClientException("First attempt failed"))
                .thenThrow(new RestClientException("Second attempt failed"))
                .thenReturn(new ResponseEntity<>(mockResponse, HttpStatus.OK));

        // Act
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertTrue(result);
        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class));
    }

    @Test
    void isRecoveryInProgress_InitialState_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(syncRecoveryManager.isRecoveryInProgress());
    }

    @Test
    void isRecoveryEnabled_DefaultState_ShouldReturnTrue() {
        // Act & Assert
        assertTrue(syncRecoveryManager.isRecoveryEnabled());
    }

    @Test
    void isRecoveryEnabled_WhenDisabled_ShouldReturnFalse() {
        // Arrange
        ReflectionTestUtils.setField(syncRecoveryManager, "recoveryEnabled", false);

        // Act & Assert
        assertFalse(syncRecoveryManager.isRecoveryEnabled());
    }

    @Test
    void triggerResync_NullResponseFromAgent_ShouldReturnFalse() {
        // Arrange
        String reason = "null_response_test";
        
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // Act
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertFalse(result);
    }

    @Test
    void triggerResync_NonSuccessStatusFromAgent_ShouldReturnFalse() {
        // Arrange
        String reason = "error_status_test";
        
        when(redisService.llen(TOOLS_UPDATES_KEY)).thenReturn(0L);
        when(redisService.delete(TOOLS_UPDATES_KEY)).thenReturn(true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ToolInventoryResponse.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        boolean result = syncRecoveryManager.triggerResync(reason);

        // Assert
        assertFalse(result);
    }
}