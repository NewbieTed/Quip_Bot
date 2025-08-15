"""
Unit tests for metrics service.
"""

import pytest
from datetime import datetime, timezone
from unittest.mock import Mock, patch

from src.agent.monitoring.metrics_service import (
    SystemMetrics, 
    MetricsService, 
    get_metrics_service
)


class TestSystemMetrics:
    """Test cases for SystemMetrics dataclass."""

    def test_init_defaults(self):
        """Test SystemMetrics initialization with defaults."""
        metrics = SystemMetrics()
        
        assert metrics.tool_discoveries_total == 0
        assert metrics.tool_discovery_latency_total == 0.0
        assert metrics.tool_discovery_latency_count == 0
        assert metrics.local_tools_discovered == 0
        assert metrics.mcp_tools_discovered == 0
        assert metrics.mcp_connection_failures == 0
        assert metrics.tools_added_total == 0
        assert metrics.tools_removed_total == 0
        assert metrics.tool_changes_detected == 0
        assert metrics.redis_messages_published == 0
        assert metrics.redis_messages_failed == 0
        assert metrics.redis_connection_failures == 0
        assert metrics.redis_connection_recoveries == 0
        assert metrics.last_discovery_time is None
        assert metrics.last_publish_time is None
        assert isinstance(metrics.last_metrics_reset, datetime)
        assert metrics.resync_requests_received == 0
        assert metrics.resync_requests_successful == 0
        assert metrics.resync_requests_failed == 0
        assert metrics.resync_response_latency_total == 0.0
        assert metrics.resync_response_latency_count == 0
        assert metrics.last_resync_request is None

    def test_get_average_discovery_latency(self):
        """Test average discovery latency calculation."""
        metrics = SystemMetrics()
        
        # No data
        assert metrics.get_average_discovery_latency() == 0.0
        
        # With data
        metrics.tool_discovery_latency_total = 300.0
        metrics.tool_discovery_latency_count = 2
        
        assert metrics.get_average_discovery_latency() == 150.0

    def test_get_average_resync_latency(self):
        """Test average resync latency calculation."""
        metrics = SystemMetrics()
        
        # No data
        assert metrics.get_average_resync_latency() == 0.0
        
        # With data
        metrics.resync_response_latency_total = 500.0
        metrics.resync_response_latency_count = 2
        
        assert metrics.get_average_resync_latency() == 250.0

    def test_get_resync_success_rate(self):
        """Test resync success rate calculation."""
        metrics = SystemMetrics()
        
        # No requests
        assert metrics.get_resync_success_rate() == 0.0
        
        # With requests
        metrics.resync_requests_received = 10
        metrics.resync_requests_successful = 8
        
        assert metrics.get_resync_success_rate() == 0.8


class TestMetricsService:
    """Test cases for MetricsService class."""

    def test_init(self):
        """Test MetricsService initialization."""
        service = MetricsService()
        
        assert isinstance(service._metrics, SystemMetrics)
        assert service._lock is not None

    def test_record_tool_discovery(self):
        """Test recording tool discovery metrics."""
        service = MetricsService()
        
        service.record_tool_discovery(150.5, 5, 3, mcp_failed=True)
        
        metrics = service._metrics
        assert metrics.tool_discoveries_total == 1
        assert metrics.tool_discovery_latency_total == 150.5
        assert metrics.tool_discovery_latency_count == 1
        assert metrics.local_tools_discovered == 5
        assert metrics.mcp_tools_discovered == 3
        assert metrics.mcp_connection_failures == 1
        assert metrics.last_discovery_time is not None

    def test_record_tool_discovery_no_mcp_failure(self):
        """Test recording tool discovery without MCP failure."""
        service = MetricsService()
        
        service.record_tool_discovery(100.0, 3, 2, mcp_failed=False)
        
        metrics = service._metrics
        assert metrics.mcp_connection_failures == 0

    def test_record_tool_changes(self):
        """Test recording tool change metrics."""
        service = MetricsService()
        
        service.record_tool_changes(2, 1)
        
        metrics = service._metrics
        assert metrics.tools_added_total == 2
        assert metrics.tools_removed_total == 1
        assert metrics.tool_changes_detected == 1

    def test_record_tool_changes_no_changes(self):
        """Test recording tool changes with no actual changes."""
        service = MetricsService()
        
        service.record_tool_changes(0, 0)
        
        metrics = service._metrics
        assert metrics.tools_added_total == 0
        assert metrics.tools_removed_total == 0
        assert metrics.tool_changes_detected == 0

    def test_record_redis_publish_success(self):
        """Test recording successful Redis publish."""
        service = MetricsService()
        
        service.record_redis_publish(True)
        
        metrics = service._metrics
        assert metrics.redis_messages_published == 1
        assert metrics.redis_messages_failed == 0
        assert metrics.last_publish_time is not None

    def test_record_redis_publish_failure(self):
        """Test recording failed Redis publish."""
        service = MetricsService()
        
        service.record_redis_publish(False)
        
        metrics = service._metrics
        assert metrics.redis_messages_published == 0
        assert metrics.redis_messages_failed == 1
        assert metrics.last_publish_time is None

    def test_record_redis_connection_event_failure(self):
        """Test recording Redis connection failure."""
        service = MetricsService()
        
        service.record_redis_connection_event("failure")
        
        metrics = service._metrics
        assert metrics.redis_connection_failures == 1
        assert metrics.redis_connection_recoveries == 0

    def test_record_redis_connection_event_recovery(self):
        """Test recording Redis connection recovery."""
        service = MetricsService()
        
        service.record_redis_connection_event("recovery")
        
        metrics = service._metrics
        assert metrics.redis_connection_failures == 0
        assert metrics.redis_connection_recoveries == 1

    def test_record_resync_request_success(self):
        """Test recording successful resync request."""
        service = MetricsService()
        
        service.record_resync_request(True, 250.5)
        
        metrics = service._metrics
        assert metrics.resync_requests_received == 1
        assert metrics.resync_requests_successful == 1
        assert metrics.resync_requests_failed == 0
        assert metrics.resync_response_latency_total == 250.5
        assert metrics.resync_response_latency_count == 1
        assert metrics.last_resync_request is not None

    def test_record_resync_request_failure(self):
        """Test recording failed resync request."""
        service = MetricsService()
        
        service.record_resync_request(False, 500.0)
        
        metrics = service._metrics
        assert metrics.resync_requests_received == 1
        assert metrics.resync_requests_successful == 0
        assert metrics.resync_requests_failed == 1
        assert metrics.resync_response_latency_total == 500.0

    @patch('src.agent.monitoring.metrics_service.get_tool_publisher_service')
    @patch('src.agent.monitoring.metrics_service.get_redis_client')
    @patch('src.agent.monitoring.metrics_service.get_tool_discovery_service')
    def test_get_metrics_summary(self, mock_discovery, mock_redis, mock_publisher):
        """Test getting comprehensive metrics summary."""
        # Mock dependencies
        mock_publisher_service = Mock()
        mock_publisher_service.get_metrics.return_value = {
            "messages_published": 10,
            "messages_failed": 2,
            "current_queue_size": 1
        }
        mock_publisher.return_value = mock_publisher_service
        
        mock_redis_client = Mock()
        mock_redis_client.is_connected.return_value = True
        mock_redis.return_value = mock_redis_client
        
        mock_discovery_service = Mock()
        mock_discovery_service.get_cached_tool_inventory.return_value = {"tool1", "tool2"}
        mock_discovery.return_value = mock_discovery_service
        
        service = MetricsService()
        
        # Add some metrics
        service.record_tool_discovery(100.0, 3, 2)
        service.record_resync_request(True, 200.0)
        
        summary = service.get_metrics_summary()
        
        assert "system_metrics" in summary
        assert "publisher_metrics" in summary
        assert "redis_status" in summary
        assert "discovery_status" in summary
        
        system_metrics = summary["system_metrics"]
        assert system_metrics["tool_discoveries_total"] == 1
        assert system_metrics["average_discovery_latency_ms"] == 100.0
        assert system_metrics["local_tools_discovered"] == 3
        assert system_metrics["mcp_tools_discovered"] == 2
        assert system_metrics["resync_requests_received"] == 1
        assert system_metrics["average_resync_latency_ms"] == 200.0
        assert system_metrics["resync_success_rate"] == 1.0
        
        assert summary["redis_status"]["connected"] is True
        assert summary["discovery_status"]["cached_tool_count"] == 2

    def test_reset_metrics(self):
        """Test resetting all metrics."""
        service = MetricsService()
        
        # Add some metrics
        service.record_tool_discovery(100.0, 3, 2)
        service.record_resync_request(True, 200.0)
        
        with patch('src.agent.monitoring.metrics_service.get_tool_publisher_service') as mock_publisher:
            mock_publisher_service = Mock()
            mock_publisher.return_value = mock_publisher_service
            
            service.reset_metrics()
            
            # Metrics should be reset
            metrics = service._metrics
            assert metrics.tool_discoveries_total == 0
            assert metrics.resync_requests_received == 0
            
            # Publisher metrics should also be reset
            mock_publisher_service.reset_metrics.assert_called_once()

    @patch('src.agent.monitoring.metrics_service.get_tool_publisher_service')
    @patch('src.agent.monitoring.metrics_service.get_redis_client')
    @patch('src.agent.monitoring.metrics_service.get_tool_discovery_service')
    def test_get_health_status_healthy(self, mock_discovery, mock_redis, mock_publisher):
        """Test health status when system is healthy."""
        # Mock dependencies for healthy state
        mock_publisher_service = Mock()
        mock_publisher_service.get_metrics.return_value = {
            "current_queue_size": 0,
            "redis_connected": True
        }
        mock_publisher.return_value = mock_publisher_service
        
        mock_redis_client = Mock()
        mock_redis_client.is_connected.return_value = True
        mock_redis.return_value = mock_redis_client
        
        mock_discovery_service = Mock()
        mock_discovery_service.get_cached_tool_inventory.return_value = {"tool1", "tool2"}
        mock_discovery.return_value = mock_discovery_service
        
        service = MetricsService()
        
        # Set recent discovery time
        service._metrics.last_discovery_time = datetime.now(timezone.utc)
        
        health_status = service.get_health_status()
        
        assert health_status["status"] == "healthy"
        assert len(health_status["issues"]) == 0
        assert health_status["redis_connected"] is True
        assert health_status["cached_tools"] == 2

    @patch('src.agent.monitoring.metrics_service.get_tool_publisher_service')
    @patch('src.agent.monitoring.metrics_service.get_redis_client')
    @patch('src.agent.monitoring.metrics_service.get_tool_discovery_service')
    def test_get_health_status_degraded(self, mock_discovery, mock_redis, mock_publisher):
        """Test health status when system is degraded."""
        # Mock dependencies for degraded state
        mock_publisher_service = Mock()
        mock_publisher_service.get_metrics.return_value = {
            "current_queue_size": 0,
            "redis_connected": False
        }
        mock_publisher.return_value = mock_publisher_service
        
        mock_redis_client = Mock()
        mock_redis_client.is_connected.return_value = False
        mock_redis.return_value = mock_redis_client
        
        mock_discovery_service = Mock()
        mock_discovery_service.get_cached_tool_inventory.return_value = {"tool1"}
        mock_discovery.return_value = mock_discovery_service
        
        service = MetricsService()
        
        # Add some issues
        service._metrics.mcp_connection_failures = 1
        service._metrics.redis_connection_failures = 2
        service._metrics.redis_connection_recoveries = 1
        service._metrics.resync_requests_received = 10
        service._metrics.resync_requests_successful = 5  # 50% success rate
        
        health_status = service.get_health_status()
        
        assert health_status["status"] == "degraded"
        assert "mcp_connection_issues" in health_status["issues"]
        assert "redis_connection_issues" in health_status["issues"]
        assert "redis_disconnected" in health_status["issues"]
        assert "resync_failures" in health_status["issues"]
        assert health_status["resync_success_rate"] == 0.5

    def test_log_metrics_summary(self):
        """Test logging metrics summary."""
        service = MetricsService()
        
        with patch.object(service, 'get_metrics_summary', return_value={"test": "data"}):
            # Should not raise any exceptions
            service.log_metrics_summary()


class TestGlobalFunctions:
    """Test cases for global metrics service functions."""

    def test_get_metrics_service_singleton(self):
        """Test that get_metrics_service returns singleton instance."""
        # Clear any existing global service
        import src.agent.monitoring.metrics_service
        src.agent.monitoring.metrics_service._metrics_service = None
        
        service1 = get_metrics_service()
        service2 = get_metrics_service()
        
        assert service1 is service2
        assert isinstance(service1, MetricsService)