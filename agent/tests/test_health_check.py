"""
Unit tests for health check service.
"""

import pytest
import asyncio
from datetime import datetime, timezone
from unittest.mock import Mock, patch, AsyncMock

from src.agent.monitoring.health_check import (
    HealthChecker, 
    get_health_checker,
    log_system_metrics,
    log_system_health,
    get_system_status
)


class TestHealthChecker:
    """Test cases for HealthChecker class."""

    def test_init(self):
        """Test HealthChecker initialization."""
        checker = HealthChecker()
        
        assert checker.metrics_service is not None

    @pytest.mark.asyncio
    async def test_perform_health_check_all_healthy(self):
        """Test comprehensive health check when all components are healthy."""
        checker = HealthChecker()
        
        # Mock all health check methods to return healthy status
        with patch.object(checker, '_check_redis_health', return_value={
            "status": "healthy",
            "connected": True,
            "ping_time_ms": 5.0
        }):
            with patch.object(checker, '_check_discovery_health', return_value={
                "status": "healthy",
                "tools_discovered": 10,
                "discovery_time_ms": 100.0
            }):
                with patch.object(checker, '_check_publisher_health', return_value={
                    "status": "healthy",
                    "queue_size": 0,
                    "redis_connected": True
                }):
                    with patch.object(checker.metrics_service, 'get_metrics_summary', return_value={"test": "metrics"}):
                        with patch.object(checker.metrics_service, 'get_health_status', return_value={
                            "status": "healthy",
                            "issues": []
                        }):
                            
                            result = await checker.perform_health_check()
                            
                            assert result["overall_status"] == "healthy"
                            assert len(result["issues"]) == 0
                            assert "timestamp" in result
                            assert "components" in result
                            assert "metrics" in result
                            assert "check_duration_ms" in result
                            
                            # Check component statuses
                            assert result["components"]["redis"]["status"] == "healthy"
                            assert result["components"]["tool_discovery"]["status"] == "healthy"
                            assert result["components"]["tool_publisher"]["status"] == "healthy"

    @pytest.mark.asyncio
    async def test_perform_health_check_with_issues(self):
        """Test health check when components have issues."""
        checker = HealthChecker()
        
        # Mock health check methods with some issues
        with patch.object(checker, '_check_redis_health', return_value={
            "status": "unhealthy",
            "connected": False,
            "error": "Connection failed"
        }):
            with patch.object(checker, '_check_discovery_health', return_value={
                "status": "degraded",
                "tools_discovered": 5,
                "mcp_connection_failed": True
            }):
                with patch.object(checker, '_check_publisher_health', return_value={
                    "status": "healthy",
                    "queue_size": 0
                }):
                    with patch.object(checker.metrics_service, 'get_metrics_summary', return_value={"test": "metrics"}):
                        with patch.object(checker.metrics_service, 'get_health_status', return_value={
                            "status": "degraded",
                            "issues": ["redis_disconnected"]
                        }):
                            
                            result = await checker.perform_health_check()
                            
                            assert result["overall_status"] == "degraded"
                            assert "redis_disconnected" in result["issues"]
                            assert "redis_unhealthy" in result["issues"]
                            assert "tool_discovery_degraded" in result["issues"]

    @pytest.mark.asyncio
    async def test_check_redis_health_success(self):
        """Test Redis health check when Redis is healthy."""
        checker = HealthChecker()
        
        mock_redis_client = Mock()
        mock_redis_client.is_connected.return_value = True
        mock_redis_client.lpush.return_value = 1
        mock_redis_client.llen.return_value = 1
        
        with patch('src.agent.monitoring.health_check.get_redis_client', return_value=mock_redis_client):
            result = await checker._check_redis_health()
            
            assert result["status"] == "healthy"
            assert result["connected"] is True
            assert "ping_time_ms" in result
            assert result["test_operations"] == "passed"
            assert result["list_length"] == 1

    @pytest.mark.asyncio
    async def test_check_redis_health_connection_failed(self):
        """Test Redis health check when connection fails."""
        checker = HealthChecker()
        
        mock_redis_client = Mock()
        mock_redis_client.is_connected.return_value = False
        
        with patch('src.agent.monitoring.health_check.get_redis_client', return_value=mock_redis_client):
            result = await checker._check_redis_health()
            
            assert result["status"] == "unhealthy"
            assert result["connected"] is False
            assert "error" in result

    @pytest.mark.asyncio
    async def test_check_redis_health_exception(self):
        """Test Redis health check when exception occurs."""
        checker = HealthChecker()
        
        with patch('src.agent.monitoring.health_check.get_redis_client', side_effect=Exception("Redis error")):
            result = await checker._check_redis_health()
            
            assert result["status"] == "unhealthy"
            assert result["connected"] is False
            assert result["error"] == "Redis error"
            assert result["error_type"] == "Exception"

    @pytest.mark.asyncio
    async def test_check_discovery_health_success(self):
        """Test tool discovery health check when discovery is healthy."""
        checker = HealthChecker()
        
        mock_discovery_service = Mock()
        mock_discovery_service.discover_tools = AsyncMock(return_value=[Mock(), Mock(), Mock()])
        mock_discovery_service.get_cached_tool_inventory.return_value = {"tool1", "tool2"}
        mock_discovery_service._mcp_connection_failed = False
        
        with patch('src.agent.monitoring.health_check.get_tool_discovery_service', return_value=mock_discovery_service):
            result = await checker._check_discovery_health()
            
            assert result["status"] == "healthy"
            assert result["tools_discovered"] == 3
            assert "discovery_time_ms" in result
            assert result["cached_inventory_size"] == 2
            assert result["mcp_connection_failed"] is False

    @pytest.mark.asyncio
    async def test_check_discovery_health_exception(self):
        """Test tool discovery health check when exception occurs."""
        checker = HealthChecker()
        
        mock_discovery_service = Mock()
        mock_discovery_service.discover_tools = AsyncMock(side_effect=Exception("Discovery error"))
        
        with patch('src.agent.monitoring.health_check.get_tool_discovery_service', return_value=mock_discovery_service):
            result = await checker._check_discovery_health()
            
            assert result["status"] == "unhealthy"
            assert result["error"] == "Discovery error"
            assert result["error_type"] == "Exception"

    def test_check_publisher_health_healthy(self):
        """Test tool publisher health check when publisher is healthy."""
        checker = HealthChecker()
        
        mock_publisher_service = Mock()
        mock_publisher_service.get_metrics.return_value = {
            "messages_published": 10,
            "messages_failed": 0,
            "redis_connected": True,
            "average_latency_ms": 50.0
        }
        mock_publisher_service.get_queue_size.return_value = 0
        
        with patch('src.agent.monitoring.health_check.get_tool_publisher_service', return_value=mock_publisher_service):
            result = checker._check_publisher_health()
            
            assert result["status"] == "healthy"
            assert result["queue_size"] == 0
            assert result["messages_published"] == 10
            assert result["messages_failed"] == 0
            assert result["redis_connected"] is True
            assert result["average_latency_ms"] == 50.0

    def test_check_publisher_health_degraded(self):
        """Test tool publisher health check when queue is high."""
        checker = HealthChecker()
        
        mock_publisher_service = Mock()
        mock_publisher_service.get_metrics.return_value = {
            "messages_published": 10,
            "messages_failed": 2,
            "redis_connected": True,
            "average_latency_ms": 100.0
        }
        mock_publisher_service.get_queue_size.return_value = 60  # High queue size
        
        with patch('src.agent.monitoring.health_check.get_tool_publisher_service', return_value=mock_publisher_service):
            result = checker._check_publisher_health()
            
            assert result["status"] == "degraded"
            assert result["queue_size"] == 60

    def test_check_publisher_health_unhealthy(self):
        """Test tool publisher health check when Redis is disconnected."""
        checker = HealthChecker()
        
        mock_publisher_service = Mock()
        mock_publisher_service.get_metrics.return_value = {
            "messages_published": 5,
            "messages_failed": 5,
            "redis_connected": False,
            "average_latency_ms": 200.0
        }
        mock_publisher_service.get_queue_size.return_value = 10
        
        with patch('src.agent.monitoring.health_check.get_tool_publisher_service', return_value=mock_publisher_service):
            result = checker._check_publisher_health()
            
            assert result["status"] == "unhealthy"
            assert result["redis_connected"] is False

    def test_check_publisher_health_exception(self):
        """Test tool publisher health check when exception occurs."""
        checker = HealthChecker()
        
        with patch('src.agent.monitoring.health_check.get_tool_publisher_service', side_effect=Exception("Publisher error")):
            result = checker._check_publisher_health()
            
            assert result["status"] == "unhealthy"
            assert result["error"] == "Publisher error"
            assert result["error_type"] == "Exception"

    def test_log_health_summary_with_event_loop(self):
        """Test logging health summary when event loop is running."""
        checker = HealthChecker()
        
        mock_health_status = {
            "status": "healthy",
            "issues": []
        }
        
        with patch.object(checker.metrics_service, 'get_health_status', return_value=mock_health_status):
            with patch('asyncio.get_event_loop') as mock_get_loop:
                mock_loop = Mock()
                mock_loop.is_running.return_value = True
                mock_get_loop.return_value = mock_loop
                
                # Should not raise any exceptions
                checker.log_health_summary()

    def test_log_health_summary_without_event_loop(self):
        """Test logging health summary when no event loop is running."""
        checker = HealthChecker()
        
        mock_health_results = {
            "overall_status": "healthy",
            "issues": [],
            "check_duration_ms": 100.0
        }
        
        with patch('asyncio.get_event_loop') as mock_get_loop:
            mock_loop = Mock()
            mock_loop.is_running.return_value = False
            mock_loop.run_until_complete.return_value = mock_health_results
            mock_get_loop.return_value = mock_loop
            
            # Should not raise any exceptions
            checker.log_health_summary()

    def test_log_health_summary_exception(self):
        """Test logging health summary when exception occurs."""
        checker = HealthChecker()
        
        with patch('asyncio.get_event_loop', side_effect=Exception("Loop error")):
            # Should not raise any exceptions
            checker.log_health_summary()


class TestGlobalFunctions:
    """Test cases for global health check functions."""

    def test_get_health_checker_singleton(self):
        """Test that get_health_checker returns singleton instance."""
        # Clear any existing global checker
        import src.agent.monitoring.health_check
        src.agent.monitoring.health_check._health_checker = None
        
        checker1 = get_health_checker()
        checker2 = get_health_checker()
        
        assert checker1 is checker2
        assert isinstance(checker1, HealthChecker)

    def test_log_system_metrics(self):
        """Test log_system_metrics convenience function."""
        mock_metrics_service = Mock()
        
        with patch('src.agent.monitoring.health_check.get_metrics_service', return_value=mock_metrics_service):
            log_system_metrics()
            
            mock_metrics_service.log_metrics_summary.assert_called_once()

    def test_log_system_health(self):
        """Test log_system_health convenience function."""
        mock_health_checker = Mock()
        
        with patch('src.agent.monitoring.health_check.get_health_checker', return_value=mock_health_checker):
            log_system_health()
            
            mock_health_checker.log_health_summary.assert_called_once()

    @pytest.mark.asyncio
    async def test_get_system_status(self):
        """Test get_system_status convenience function."""
        mock_health_results = {
            "overall_status": "healthy",
            "components": {},
            "metrics": {}
        }
        
        mock_health_checker = Mock()
        mock_health_checker.perform_health_check = AsyncMock(return_value=mock_health_results)
        
        with patch('src.agent.monitoring.health_check.get_health_checker', return_value=mock_health_checker):
            result = await get_system_status()
            
            assert result == mock_health_results
            mock_health_checker.perform_health_check.assert_called_once()