package com.quip.backend.tool.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for sync recovery monitoring functionality in ToolSyncMetricsService.
 */
class ToolSyncMetricsServiceSyncRecoveryTest {

    private ToolSyncMetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new ToolSyncMetricsService();
    }

    @Test
    @DisplayName("Should record successful sync recovery metrics")
    void shouldRecordSuccessfulSyncRecovery() {
        // Given
        String reason = "message_processing_failure";
        long recoveryTimeMs = 1500;
        Integer toolInventorySize = 25;

        // When
        metricsService.recordSyncRecovery(true, recoveryTimeMs, reason, toolInventorySize);

        // Then
        Map<String, Object> metrics = metricsService.getMetricsSummary();
        Map<String, Object> syncRecoveryMetrics = (Map<String, Object>) metrics.get("syncRecovery");

        assertEquals(1L, syncRecoveryMetrics.get("syncRecoveryTriggered"));
        assertEquals(1L, syncRecoveryMetrics.get("syncRecoverySuccessful"));
        assertEquals(0L, syncRecoveryMetrics.get("syncRecoveryFailed"));
        assertEquals(1.0, (Double) syncRecoveryMetrics.get("syncRecoverySuccessRate"), 0.001);
        assertEquals(1500.0, (Double) syncRecoveryMetrics.get("averageSyncRecoveryTimeMs"), 0.001);
        assertEquals(25, syncRecoveryMetrics.get("currentToolInventorySize"));
        assertNotNull(syncRecoveryMetrics.get("lastSyncRecovery"));
        assertNotNull(syncRecoveryMetrics.get("lastSuccessfulSyncRecovery"));
    }

    @Test
    @DisplayName("Should record failed sync recovery metrics")
    void shouldRecordFailedSyncRecovery() {
        // Given
        String reason = "agent_timeout";
        long recoveryTimeMs = 10000;

        // When
        metricsService.recordSyncRecovery(false, recoveryTimeMs, reason, null);

        // Then
        Map<String, Object> metrics = metricsService.getMetricsSummary();
        Map<String, Object> syncRecoveryMetrics = (Map<String, Object>) metrics.get("syncRecovery");

        assertEquals(1L, syncRecoveryMetrics.get("syncRecoveryTriggered"));
        assertEquals(0L, syncRecoveryMetrics.get("syncRecoverySuccessful"));
        assertEquals(1L, syncRecoveryMetrics.get("syncRecoveryFailed"));
        assertEquals(0.0, (Double) syncRecoveryMetrics.get("syncRecoverySuccessRate"), 0.001);
        assertEquals(10000.0, (Double) syncRecoveryMetrics.get("averageSyncRecoveryTimeMs"), 0.001);
        assertNotNull(syncRecoveryMetrics.get("lastSyncRecovery"));
        assertNull(syncRecoveryMetrics.get("lastSuccessfulSyncRecovery"));
    }

    @Test
    @DisplayName("Should track tool inventory size changes")
    void shouldTrackToolInventorySizeChanges() {
        // Given
        metricsService.recordSyncRecovery(true, 1000, "test", 10);
        metricsService.recordSyncRecovery(true, 1200, "test", 15);
        metricsService.recordSyncRecovery(true, 1100, "test", 12);

        // When
        Map<String, Object> metrics = metricsService.getMetricsSummary();
        Map<String, Object> syncRecoveryMetrics = (Map<String, Object>) metrics.get("syncRecovery");

        // Then
        assertEquals(3L, syncRecoveryMetrics.get("toolInventorySizeChanges")); // 0->10, 10->15, 15->12
        assertEquals(12, syncRecoveryMetrics.get("currentToolInventorySize"));
    }

    @Test
    @DisplayName("Should detect frequent sync recovery events")
    void shouldDetectFrequentSyncRecoveryEvents() {
        // Given - trigger multiple recoveries
        for (int i = 0; i < 5; i++) {
            metricsService.recordSyncRecovery(true, 1000, "test_" + i, 10);
        }

        // When
        boolean isFrequent = metricsService.isSyncRecoveryFrequent();

        // Then
        assertTrue(isFrequent, "Should detect frequent sync recovery events");
    }

    @Test
    @DisplayName("Should not detect frequent sync recovery when count is low")
    void shouldNotDetectFrequentSyncRecoveryWhenCountIsLow() {
        // Given - trigger only 2 recoveries
        metricsService.recordSyncRecovery(true, 1000, "test1", 10);
        metricsService.recordSyncRecovery(true, 1200, "test2", 12);

        // When
        boolean isFrequent = metricsService.isSyncRecoveryFrequent();

        // Then
        assertFalse(isFrequent, "Should not detect frequent sync recovery events");
    }

    @Test
    @DisplayName("Should calculate correct success rate with mixed results")
    void shouldCalculateCorrectSuccessRateWithMixedResults() {
        // Given
        metricsService.recordSyncRecovery(true, 1000, "success1", 10);
        metricsService.recordSyncRecovery(false, 2000, "failure1", null);
        metricsService.recordSyncRecovery(true, 1500, "success2", 12);
        metricsService.recordSyncRecovery(false, 3000, "failure2", null);
        metricsService.recordSyncRecovery(true, 1200, "success3", 15);

        // When
        Map<String, Object> metrics = metricsService.getMetricsSummary();
        Map<String, Object> syncRecoveryMetrics = (Map<String, Object>) metrics.get("syncRecovery");

        // Then
        assertEquals(5L, syncRecoveryMetrics.get("syncRecoveryTriggered"));
        assertEquals(3L, syncRecoveryMetrics.get("syncRecoverySuccessful"));
        assertEquals(2L, syncRecoveryMetrics.get("syncRecoveryFailed"));
        assertEquals(0.6, (Double) syncRecoveryMetrics.get("syncRecoverySuccessRate"), 0.001);
        assertEquals(1740.0, (Double) syncRecoveryMetrics.get("averageSyncRecoveryTimeMs"), 0.001); // (1000+2000+1500+3000+1200)/5
    }

    @Test
    @DisplayName("Should include sync recovery in system health status")
    void shouldIncludeSyncRecoveryInSystemHealthStatus() {
        // Given - simulate some message activity first to avoid "idle" status
        metricsService.recordMessageProcessed(true, 100);
        
        // Then trigger many failed recoveries
        for (int i = 0; i < 5; i++) {
            metricsService.recordSyncRecovery(false, 1000, "failure_" + i, null);
        }

        // When
        String healthStatus = metricsService.getSystemHealthStatus();

        // Then
        assertEquals("degraded", healthStatus, "System should be degraded with frequent recovery events");
    }

    @Test
    @DisplayName("Should provide comprehensive sync recovery health status")
    void shouldProvideComprehensiveSyncRecoveryHealthStatus() {
        // Given
        metricsService.recordSyncRecovery(true, 1000, "success", 10);
        metricsService.recordSyncRecovery(false, 2000, "failure", null);

        // When
        Map<String, Object> healthStatus = metricsService.getSyncRecoveryHealthStatus();

        // Then
        assertFalse((Boolean) healthStatus.get("isFrequent"));
        assertEquals(0.5, (Double) healthStatus.get("successRate"), 0.001);
        assertEquals(2L, healthStatus.get("totalTriggered"));
        assertEquals(1L, healthStatus.get("totalSuccessful"));
        assertEquals(1L, healthStatus.get("totalFailed"));
        assertEquals(1500.0, (Double) healthStatus.get("averageRecoveryTimeMs"), 0.001);
        assertNotNull(healthStatus.get("lastRecovery"));
        assertNotNull(healthStatus.get("lastSuccessfulRecovery"));
        assertEquals(10, healthStatus.get("currentToolInventorySize"));
    }

    @Test
    @DisplayName("Should reset sync recovery metrics")
    void shouldResetSyncRecoveryMetrics() {
        // Given
        metricsService.recordSyncRecovery(true, 1000, "test", 10);
        metricsService.recordSyncRecovery(false, 2000, "test", null);

        // When
        metricsService.resetMetrics();

        // Then
        Map<String, Object> metrics = metricsService.getMetricsSummary();
        Map<String, Object> syncRecoveryMetrics = (Map<String, Object>) metrics.get("syncRecovery");

        assertEquals(0L, syncRecoveryMetrics.get("syncRecoveryTriggered"));
        assertEquals(0L, syncRecoveryMetrics.get("syncRecoverySuccessful"));
        assertEquals(0L, syncRecoveryMetrics.get("syncRecoveryFailed"));
        assertEquals(0.0, (Double) syncRecoveryMetrics.get("syncRecoverySuccessRate"), 0.001);
        assertEquals(0.0, (Double) syncRecoveryMetrics.get("averageSyncRecoveryTimeMs"), 0.001);
        assertEquals(0L, syncRecoveryMetrics.get("toolInventorySizeChanges"));
        assertNull(syncRecoveryMetrics.get("lastSyncRecovery"));
        assertNull(syncRecoveryMetrics.get("lastSuccessfulSyncRecovery"));
    }

    @Test
    @DisplayName("Should record tool inventory size changes outside sync recovery")
    void shouldRecordToolInventorySizeChangesOutsideSyncRecovery() {
        // Given
        metricsService.recordToolInventorySize(10);
        metricsService.recordToolInventorySize(15);
        metricsService.recordToolInventorySize(12);

        // When
        Map<String, Object> metrics = metricsService.getMetricsSummary();
        Map<String, Object> syncRecoveryMetrics = (Map<String, Object>) metrics.get("syncRecovery");

        // Then
        assertEquals(3L, syncRecoveryMetrics.get("toolInventorySizeChanges")); // 0->10, 10->15, 15->12
        assertEquals(12, syncRecoveryMetrics.get("currentToolInventorySize"));
    }
}