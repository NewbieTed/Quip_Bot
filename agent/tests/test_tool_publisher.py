"""
Unit tests for tool publisher service.
"""

import pytest
import json
import time
from datetime import datetime, timezone
from unittest.mock import Mock, patch, MagicMock
from collections import deque

from src.agent.redis.tool_publisher import (
    ToolPublisherService, 
    ToolUpdateMessage, 
    ToolPublisherMetrics,
    get_tool_publisher_service
)


class TestToolUpdateMessage:
    """Test cases for ToolUpdateMessage class."""

    def test_init(self):
        """Test ToolUpdateMessage initialization."""
        added_tools = [{"name": "tool1", "mcpServerName": "built-in"}]
        removed_tools = [{"name": "tool2", "mcpServerName": "test-server"}]
        
        message = ToolUpdateMessage(added_tools, removed_tools)
        
        assert message.added_tools == added_tools
        assert message.removed_tools == removed_tools
        assert message.source == "agent"
        assert isinstance(message.message_id, str)
        assert isinstance(message.timestamp, str)

    def test_to_dict(self):
        """Test message conversion to dictionary."""
        added_tools = [{"name": "tool1", "mcpServerName": "built-in"}]
        removed_tools = [{"name": "tool2", "mcpServerName": "test-server"}]
        
        message = ToolUpdateMessage(added_tools, removed_tools)
        message_dict = message.to_dict()
        
        assert message_dict["messageId"] == message.message_id
        assert message_dict["timestamp"] == message.timestamp
        assert message_dict["addedTools"] == added_tools
        assert message_dict["removedTools"] == removed_tools
        assert message_dict["source"] == "agent"

    def test_to_json(self):
        """Test message conversion to JSON."""
        added_tools = [{"name": "tool1", "mcpServerName": "built-in"}]
        removed_tools = []
        
        message = ToolUpdateMessage(added_tools, removed_tools)
        json_str = message.to_json()
        
        # Should be valid JSON
        parsed = json.loads(json_str)
        assert parsed["addedTools"] == added_tools
        assert parsed["removedTools"] == []
        assert parsed["source"] == "agent"


class TestToolPublisherMetrics:
    """Test cases for ToolPublisherMetrics class."""

    def test_init(self):
        """Test metrics initialization."""
        metrics = ToolPublisherMetrics()
        
        assert metrics.messages_published == 0
        assert metrics.messages_queued == 0
        assert metrics.messages_failed == 0
        assert metrics.publish_latency_total == 0.0
        assert metrics.publish_latency_count == 0
        assert metrics.queue_size_max == 0
        assert metrics.redis_connection_failures == 0
        assert metrics.redis_connection_recoveries == 0
        assert metrics.last_publish_time is None
        assert metrics.last_failure_time is None

    def test_record_publish_success(self):
        """Test recording successful publish."""
        metrics = ToolPublisherMetrics()
        
        metrics.record_publish_success(100.5)
        
        assert metrics.messages_published == 1
        assert metrics.publish_latency_total == 100.5
        assert metrics.publish_latency_count == 1
        assert metrics.last_publish_time is not None

    def test_record_publish_failure(self):
        """Test recording publish failure."""
        metrics = ToolPublisherMetrics()
        
        metrics.record_publish_failure()
        
        assert metrics.messages_failed == 1
        assert metrics.last_failure_time is not None

    def test_record_message_queued(self):
        """Test recording queued message."""
        metrics = ToolPublisherMetrics()
        
        metrics.record_message_queued(5)
        metrics.record_message_queued(10)
        
        assert metrics.messages_queued == 2
        assert metrics.queue_size_max == 10

    def test_get_average_latency(self):
        """Test average latency calculation."""
        metrics = ToolPublisherMetrics()
        
        # No data
        assert metrics.get_average_latency() == 0.0
        
        # With data
        metrics.record_publish_success(100.0)
        metrics.record_publish_success(200.0)
        
        assert metrics.get_average_latency() == 150.0

    def test_get_metrics_summary(self):
        """Test metrics summary generation."""
        metrics = ToolPublisherMetrics()
        
        metrics.record_publish_success(100.0)
        metrics.record_publish_failure()
        metrics.record_message_queued(5)
        
        summary = metrics.get_metrics_summary()
        
        assert summary["messages_published"] == 1
        assert summary["messages_failed"] == 1
        assert summary["messages_queued"] == 1
        assert summary["average_latency_ms"] == 100.0
        assert summary["queue_size_max"] == 5


class TestToolPublisherService:
    """Test cases for ToolPublisherService class."""

    def test_init(self):
        """Test service initialization."""
        service = ToolPublisherService()
        
        assert service._redis_client is None
        assert isinstance(service._message_queue, deque)
        assert service._max_queue_size == 100
        assert isinstance(service._metrics, ToolPublisherMetrics)
        assert service._redis_connected is False

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_get_redis_client(self, mock_get_client):
        """Test Redis client retrieval."""
        mock_client = Mock()
        mock_get_client.return_value = mock_client
        
        service = ToolPublisherService()
        client = service._get_redis_client()
        
        assert client == mock_client
        assert service._redis_client == mock_client

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_publish_tool_changes_success(self, mock_get_client):
        """Test successful tool changes publishing."""
        mock_redis = Mock()
        mock_redis.is_connected.return_value = True
        mock_redis.rpush.return_value = 1
        mock_get_client.return_value = mock_redis
        
        service = ToolPublisherService()
        
        added_tools = [{"name": "tool1", "mcpServerName": "built-in"}]
        removed_tools = [{"name": "tool2", "mcpServerName": "test-server"}]
        
        result = service.publish_tool_changes(added_tools, removed_tools)
        
        assert result is True
        mock_redis.rpush.assert_called_once()
        assert service._metrics.messages_published == 1

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_publish_tool_changes_no_changes(self, mock_get_client):
        """Test publishing with no changes."""
        service = ToolPublisherService()
        
        result = service.publish_tool_changes([], [])
        
        assert result is True
        mock_get_client.assert_not_called()

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_publish_tool_changes_failure_queues_message(self, mock_get_client):
        """Test that failed publish queues message."""
        mock_redis = Mock()
        mock_redis.is_connected.return_value = True
        mock_redis.rpush.side_effect = Exception("Redis error")
        mock_get_client.return_value = mock_redis
        
        service = ToolPublisherService()
        
        added_tools = [{"name": "tool1", "mcpServerName": "built-in"}]
        removed_tools = []
        
        result = service.publish_tool_changes(added_tools, removed_tools)
        
        assert result is False
        assert len(service._message_queue) == 1
        assert service._metrics.messages_failed == 1

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_publish_message_success(self, mock_get_client):
        """Test successful message publishing."""
        mock_redis = Mock()
        mock_redis.is_connected.return_value = True
        mock_redis.rpush.return_value = 1
        mock_get_client.return_value = mock_redis
        
        service = ToolPublisherService()
        service._redis_connected = False  # Test connection status change
        
        message = ToolUpdateMessage(
            [{"name": "tool1", "mcpServerName": "built-in"}], 
            []
        )
        
        result = service._publish_message(message)
        
        assert result is True
        mock_redis.rpush.assert_called_once_with(
            service.REDIS_KEY, 
            message.to_json()
        )
        assert service._redis_connected is True

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_publish_message_connection_status_logging(self, mock_get_client):
        """Test connection status change logging."""
        mock_redis = Mock()
        mock_redis.is_connected.side_effect = [True, False]  # Connected then disconnected
        mock_redis.rpush.side_effect = [1, Exception("Connection lost")]
        mock_get_client.return_value = mock_redis
        
        service = ToolPublisherService()
        service._redis_connected = False
        
        message = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        
        # First call - connection established
        result1 = service._publish_message(message)
        assert result1 is True
        assert service._redis_connected is True
        
        # Second call - connection lost
        result2 = service._publish_message(message)
        assert result2 is False
        assert service._redis_connected is False

    def test_queue_message(self):
        """Test message queuing."""
        service = ToolPublisherService()
        
        message = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        
        service._queue_message(message)
        
        assert len(service._message_queue) == 1
        assert service._message_queue[0] == message
        assert service._metrics.messages_queued == 1

    def test_queue_message_overflow(self):
        """Test queue overflow handling."""
        service = ToolPublisherService()
        service._max_queue_size = 2
        
        # Fill queue to capacity
        message1 = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        message2 = ToolUpdateMessage([{"name": "tool2", "mcpServerName": "built-in"}], [])
        message3 = ToolUpdateMessage([{"name": "tool3", "mcpServerName": "built-in"}], [])
        
        service._queue_message(message1)
        service._queue_message(message2)
        service._queue_message(message3)  # Should drop oldest
        
        assert len(service._message_queue) == 2
        assert service._message_queue[0] == message2
        assert service._message_queue[1] == message3

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_process_queued_messages_success(self, mock_get_client):
        """Test processing queued messages successfully."""
        mock_redis = Mock()
        mock_redis.is_connected.return_value = True
        mock_redis.rpush.return_value = 1
        mock_get_client.return_value = mock_redis
        
        service = ToolPublisherService()
        
        # Queue some messages
        message1 = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        message2 = ToolUpdateMessage([{"name": "tool2", "mcpServerName": "built-in"}], [])
        service._queue_message(message1)
        service._queue_message(message2)
        
        service._process_queued_messages()
        
        assert len(service._message_queue) == 0
        assert mock_redis.rpush.call_count == 2

    @patch('src.agent.redis.tool_publisher.get_redis_client')
    def test_process_queued_messages_partial_failure(self, mock_get_client):
        """Test processing queued messages with partial failure."""
        mock_redis = Mock()
        mock_redis.is_connected.return_value = True
        mock_redis.rpush.side_effect = [1, Exception("Redis error")]  # First succeeds, second fails
        mock_get_client.return_value = mock_redis
        
        service = ToolPublisherService()
        
        # Queue some messages
        message1 = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        message2 = ToolUpdateMessage([{"name": "tool2", "mcpServerName": "built-in"}], [])
        service._queue_message(message1)
        service._queue_message(message2)
        
        service._process_queued_messages()
        
        # Second message should remain in queue
        assert len(service._message_queue) == 1

    def test_retry_queued_messages(self):
        """Test manual retry of queued messages."""
        service = ToolPublisherService()
        
        with patch.object(service, '_process_queued_messages') as mock_process:
            # Queue a message
            message = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
            service._queue_message(message)
            
            result = service.retry_queued_messages()
            
            mock_process.assert_called_once()

    def test_get_queue_size(self):
        """Test getting queue size."""
        service = ToolPublisherService()
        
        assert service.get_queue_size() == 0
        
        message = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        service._queue_message(message)
        
        assert service.get_queue_size() == 1

    def test_clear_queue(self):
        """Test clearing message queue."""
        service = ToolPublisherService()
        
        # Queue some messages
        message1 = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        message2 = ToolUpdateMessage([{"name": "tool2", "mcpServerName": "built-in"}], [])
        service._queue_message(message1)
        service._queue_message(message2)
        
        cleared_count = service.clear_queue()
        
        assert cleared_count == 2
        assert len(service._message_queue) == 0

    def test_get_queued_messages_info(self):
        """Test getting queued messages info."""
        service = ToolPublisherService()
        
        message = ToolUpdateMessage(
            [{"name": "tool1", "mcpServerName": "built-in"}], 
            [{"name": "tool2", "mcpServerName": "test-server"}]
        )
        service._queue_message(message)
        
        info = service.get_queued_messages_info()
        
        assert len(info) == 1
        assert info[0]["message_id"] == message.message_id
        assert info[0]["added_tools_count"] == 1
        assert info[0]["removed_tools_count"] == 1

    def test_get_metrics(self):
        """Test getting publisher metrics."""
        service = ToolPublisherService()
        service._redis_connected = True
        
        # Add some metrics
        service._metrics.record_publish_success(100.0)
        message = ToolUpdateMessage([{"name": "tool1", "mcpServerName": "built-in"}], [])
        service._queue_message(message)
        
        metrics = service.get_metrics()
        
        assert metrics["messages_published"] == 1
        assert metrics["current_queue_size"] == 1
        assert metrics["redis_connected"] is True
        assert metrics["redis_key"] == service.REDIS_KEY

    def test_reset_metrics(self):
        """Test resetting metrics."""
        service = ToolPublisherService()
        
        # Add some metrics
        service._metrics.record_publish_success(100.0)
        service._metrics.record_publish_failure()
        
        service.reset_metrics()
        
        assert service._metrics.messages_published == 0
        assert service._metrics.messages_failed == 0


class TestGlobalFunctions:
    """Test cases for global publisher service functions."""

    def test_get_tool_publisher_service_singleton(self):
        """Test that get_tool_publisher_service returns singleton instance."""
        # Clear any existing global service
        import src.agent.redis.tool_publisher
        src.agent.redis.tool_publisher._tool_publisher_service = None
        
        service1 = get_tool_publisher_service()
        service2 = get_tool_publisher_service()
        
        assert service1 is service2
        assert isinstance(service1, ToolPublisherService)