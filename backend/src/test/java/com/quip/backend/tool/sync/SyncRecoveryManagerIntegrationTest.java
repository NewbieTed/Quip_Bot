package com.quip.backend.tool.sync;

import com.quip.backend.tool.model.ToolInfo;
import com.quip.backend.tool.model.ToolInventoryResponse;
import com.quip.backend.tool.service.ToolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test to verify that sync recovery correctly preserves MCP server information.
 */
@ExtendWith(MockitoExtension.class)
class SyncRecoveryManagerIntegrationTest {

    @Mock
    private ToolService toolService;

    @Test
    void processSyncResponse_PreservesServerInformation() {
        // Arrange
        SyncRecoveryManager syncRecoveryManager = new SyncRecoveryManager(
            null, // redisService - not needed for this test
            toolService,
            null, // restTemplate - not needed for this test
            null  // metricsService - not needed for this test
        );

        ToolInventoryResponse response = new ToolInventoryResponse();
        response.setRequestId("test-request-id");
        response.setTimestamp(Instant.now());
        response.setDiscoveryTimestamp(Instant.now());
        
        List<ToolInfo> toolInfos = Arrays.asList(
            ToolInfo.builder().name("tool1").mcpServerName("external-server").build(),
            ToolInfo.builder().name("tool2").mcpServerName("built-in").build(),
            ToolInfo.builder().name("tool3").mcpServerName("weather-server").build()
        );
        response.setCurrentTools(toolInfos);

        // Act
        // Use reflection to call the private method for testing
        try {
            var method = SyncRecoveryManager.class.getDeclaredMethod("processSyncResponse", ToolInventoryResponse.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(syncRecoveryManager, response);

            // Assert
            assertTrue(result, "processSyncResponse should return true for valid response");

            // Verify that the correct method was called with ToolInfo objects
            ArgumentCaptor<List<ToolInfo>> captor = ArgumentCaptor.forClass(List.class);
            verify(toolService).syncToolsFromInventoryWithServerInfo(captor.capture());

            List<ToolInfo> capturedToolInfos = captor.getValue();
            assertEquals(3, capturedToolInfos.size());
            
            // Verify that server information is preserved
            assertEquals("tool1", capturedToolInfos.get(0).getName());
            assertEquals("external-server", capturedToolInfos.get(0).getMcpServerName());
            
            assertEquals("tool2", capturedToolInfos.get(1).getName());
            assertEquals("built-in", capturedToolInfos.get(1).getMcpServerName());
            
            assertEquals("tool3", capturedToolInfos.get(2).getName());
            assertEquals("weather-server", capturedToolInfos.get(2).getMcpServerName());

        } catch (Exception e) {
            fail("Failed to test processSyncResponse method: " + e.getMessage());
        }
    }

    @Test
    void processSyncResponse_FiltersInvalidToolInfo() {
        // Arrange
        SyncRecoveryManager syncRecoveryManager = new SyncRecoveryManager(
            null, toolService, null, null
        );

        ToolInventoryResponse response = new ToolInventoryResponse();
        response.setRequestId("test-request-id");
        response.setTimestamp(Instant.now());
        response.setDiscoveryTimestamp(Instant.now());
        
        List<ToolInfo> toolInfos = Arrays.asList(
            ToolInfo.builder().name("valid-tool").mcpServerName("valid-server").build(),
            ToolInfo.builder().name(null).mcpServerName("server").build(), // Invalid - null name
            ToolInfo.builder().name("tool").mcpServerName(null).build(), // Invalid - null server
            ToolInfo.builder().name("another-valid-tool").mcpServerName("another-server").build()
        );
        response.setCurrentTools(toolInfos);

        // Act
        try {
            var method = SyncRecoveryManager.class.getDeclaredMethod("processSyncResponse", ToolInventoryResponse.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(syncRecoveryManager, response);

            // Assert
            assertTrue(result, "processSyncResponse should return true even with some invalid tools");

            // Verify that only valid tools are passed to the service
            ArgumentCaptor<List<ToolInfo>> captor = ArgumentCaptor.forClass(List.class);
            verify(toolService).syncToolsFromInventoryWithServerInfo(captor.capture());

            List<ToolInfo> capturedToolInfos = captor.getValue();
            assertEquals(2, capturedToolInfos.size(), "Only valid tools should be passed");
            
            assertEquals("valid-tool", capturedToolInfos.get(0).getName());
            assertEquals("valid-server", capturedToolInfos.get(0).getMcpServerName());
            
            assertEquals("another-valid-tool", capturedToolInfos.get(1).getName());
            assertEquals("another-server", capturedToolInfos.get(1).getMcpServerName());

        } catch (Exception e) {
            fail("Failed to test processSyncResponse method: " + e.getMessage());
        }
    }
}