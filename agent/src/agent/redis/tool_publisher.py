"""
Tool publisher service for publishing tool updates to Redis.

This module provides functionality to publish tool availability changes
to Redis with message queuing and retry logic for connection failures.
"""

import json
import logging
import time
import uuid
from datetime import datetime, timezone
from typing import List, Dict, Any, Optional
from collections import deque
from threading import Lock

from src.agent.redis.client import get_redis_client, RedisClient

logger = logging.getLogger(__name__)


class ToolPublisherMetrics:
    """Metrics tracking for tool publisher operations."""
    
    def __init__(self):
        self.messages_published = 0
        self.messages_queued = 0
        self.messages_failed = 0
        self.publish_latency_total = 0.0
        self.publish_latency_count = 0
        self.queue_size_max = 0
        self.redis_connection_failures = 0
        self.redis_connection_recoveries = 0
        self.last_publish_time = None
        self.last_failure_time = None
        
    def record_publish_success(self, latency_ms: float):
        """Record a successful publish operation."""
        self.messages_published += 1
        self.publish_latency_total += latency_ms
        self.publish_latency_count += 1
        self.last_publish_time = datetime.now(timezone.utc)
        
    def record_publish_failure(self):
        """Record a failed publish operation."""
        self.messages_failed += 1
        self.last_failure_time = datetime.now(timezone.utc)
        
    def record_message_queued(self, queue_size: int):
        """Record a message being queued."""
        self.messages_queued += 1
        self.queue_size_max = max(self.queue_size_max, queue_size)
        
    def record_redis_connection_failure(self):
        """Record a Redis connection failure."""
        self.redis_connection_failures += 1
        
    def record_redis_connection_recovery(self):
        """Record a Redis connection recovery."""
        self.redis_connection_recoveries += 1
        
    def get_average_latency(self) -> float:
        """Get average publish latency in milliseconds."""
        if self.publish_latency_count == 0:
            return 0.0
        return self.publish_latency_total / self.publish_latency_count
        
    def get_metrics_summary(self) -> Dict[str, Any]:
        """Get a summary of all metrics."""
        return {
            "messages_published": self.messages_published,
            "messages_queued": self.messages_queued,
            "messages_failed": self.messages_failed,
            "average_latency_ms": self.get_average_latency(),
            "queue_size_max": self.queue_size_max,
            "redis_connection_failures": self.redis_connection_failures,
            "redis_connection_recoveries": self.redis_connection_recoveries,
            "last_publish_time": self.last_publish_time.isoformat() if self.last_publish_time else None,
            "last_failure_time": self.last_failure_time.isoformat() if self.last_failure_time else None
        }


class ToolUpdateMessage:
    """Represents a tool update message."""
    
    def __init__(self, added_tools: List[Dict[str, str]], removed_tools: List[Dict[str, str]]):
        self.message_id = str(uuid.uuid4())
        self.timestamp = datetime.now(timezone.utc).isoformat()
        self.added_tools = added_tools  # List of {"name": "tool_name", "mcpServerName": "server_name"}
        self.removed_tools = removed_tools  # List of {"name": "tool_name", "mcpServerName": "server_name"}
        self.source = "agent"
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert message to dictionary."""
        return {
            "messageId": self.message_id,
            "timestamp": self.timestamp,
            "addedTools": self.added_tools,
            "removedTools": self.removed_tools,
            "source": self.source
        }
    
    def to_json(self) -> str:
        """Convert message to JSON string."""
        return json.dumps(self.to_dict())


class ToolPublisherService:
    """Service for publishing tool updates to Redis with queuing and retry logic."""
    
    REDIS_KEY = "tools:updates"
    
    def __init__(self):
        self._redis_client: Optional[RedisClient] = None
        self._message_queue: deque = deque()
        self._queue_lock = Lock()
        self._max_queue_size = 100  # Prevent memory issues
        self._metrics = ToolPublisherMetrics()
        self._redis_connected = False
        
    def _get_redis_client(self) -> RedisClient:
        """Get Redis client instance."""
        if self._redis_client is None:
            self._redis_client = get_redis_client()
        return self._redis_client
    
    def publish_tool_changes(self, added_tools: List[Dict[str, str]], removed_tools: List[Dict[str, str]]) -> bool:
        """
        Publish tool changes to Redis.
        
        Args:
            added_tools: List of tool info dicts with name and mcpServerName
            removed_tools: List of tool info dicts with name and mcpServerName
            
        Returns:
            bool: True if published successfully, False if queued for retry
        """
        start_time = time.time()
        
        # Skip if no changes
        if not added_tools and not removed_tools:
            logger.debug("No tool changes to publish")
            return True
        
        message = ToolUpdateMessage(added_tools, removed_tools)
        
        # Structured logging for tool update events
        logger.info("Publishing tool changes", extra={
            "event_type": "tool_update_publish",
            "message_id": message.message_id,
            "added_tools": added_tools,
            "removed_tools": removed_tools,
            "added_count": len(added_tools),
            "removed_count": len(removed_tools),
            "timestamp": message.timestamp
        })
        
        # Try to publish immediately
        if self._publish_message(message):
            # Record metrics for successful publish
            latency_ms = (time.time() - start_time) * 1000
            self._metrics.record_publish_success(latency_ms)
            
            # If successful, also try to publish any queued messages
            self._process_queued_messages()
            return True
        else:
            # Record failure metrics
            self._metrics.record_publish_failure()
            
            # If failed, queue the message for retry
            self._queue_message(message)
            return False
    
    def _publish_message(self, message: ToolUpdateMessage) -> bool:
        """
        Publish a single message to Redis.
        
        Args:
            message: ToolUpdateMessage to publish
            
        Returns:
            bool: True if published successfully, False otherwise
        """
        start_time = time.time()
        
        try:
            redis_client = self._get_redis_client()
            json_message = message.to_json()
            
            # Check Redis connection status and log changes (Requirement 4.3)
            was_connected = self._redis_connected
            is_connected = redis_client.is_connected()
            
            if not was_connected and is_connected:
                logger.info("Redis connection established", extra={
                    "event_type": "redis_connection_status",
                    "status": "connected",
                    "message_id": message.message_id
                })
                self._metrics.record_redis_connection_recovery()
                self._redis_connected = True
            elif was_connected and not is_connected:
                logger.warning("Redis connection lost", extra={
                    "event_type": "redis_connection_status", 
                    "status": "disconnected",
                    "message_id": message.message_id
                })
                self._metrics.record_redis_connection_failure()
                self._redis_connected = False
            
            # Use rpush to add message to the end of the list
            result = redis_client.rpush(self.REDIS_KEY, json_message)
            
            if result is not None:
                latency_ms = (time.time() - start_time) * 1000
                logger.debug("Successfully published message to Redis", extra={
                    "event_type": "message_publish_success",
                    "message_id": message.message_id,
                    "latency_ms": latency_ms,
                    "redis_key": self.REDIS_KEY
                })
                return True
            else:
                logger.warning("Failed to publish message to Redis (null result)", extra={
                    "event_type": "message_publish_failure",
                    "message_id": message.message_id,
                    "error_type": "null_result"
                })
                return False
                
        except Exception as e:
            latency_ms = (time.time() - start_time) * 1000
            
            # Error logging with context information (Requirement 4.3)
            logger.error("Error publishing message to Redis", extra={
                "event_type": "message_publish_error",
                "message_id": message.message_id,
                "error_type": type(e).__name__,
                "error_message": str(e),
                "latency_ms": latency_ms,
                "redis_key": self.REDIS_KEY
            }, exc_info=True)
            
            self._metrics.record_redis_connection_failure()
            self._redis_connected = False
            return False
    
    def _queue_message(self, message: ToolUpdateMessage) -> None:
        """
        Queue a message for retry when Redis is unavailable.
        
        Args:
            message: ToolUpdateMessage to queue
        """
        with self._queue_lock:
            # Check queue size limit
            if len(self._message_queue) >= self._max_queue_size:
                # Remove oldest message to make room
                oldest_message = self._message_queue.popleft()
                logger.warning("Message queue full, dropping oldest message", extra={
                    "event_type": "message_queue_overflow",
                    "dropped_message_id": oldest_message.message_id if hasattr(oldest_message, 'message_id') else 'unknown',
                    "queue_size": self._max_queue_size,
                    "new_message_id": message.message_id
                })
            
            self._message_queue.append(message)
            queue_size = len(self._message_queue)
            
            # Record metrics and log queuing event
            self._metrics.record_message_queued(queue_size)
            
            logger.info("Queued message for retry", extra={
                "event_type": "message_queued",
                "message_id": message.message_id,
                "queue_size": queue_size,
                "max_queue_size": self._max_queue_size
            })
    
    def _process_queued_messages(self) -> None:
        """Process all queued messages."""
        if not self._message_queue:
            return
        
        logger.info("Processing %d queued messages", len(self._message_queue))
        
        with self._queue_lock:
            # Process messages in FIFO order
            processed_count = 0
            failed_messages = deque()
            
            while self._message_queue:
                message = self._message_queue.popleft()
                
                if self._publish_message(message):
                    processed_count += 1
                    logger.debug("Successfully published queued message %s", message.message_id)
                else:
                    # If publishing fails, stop processing and re-queue remaining messages
                    failed_messages.append(message)
                    failed_messages.extend(self._message_queue)
                    self._message_queue.clear()
                    break
            
            # Re-queue any failed messages
            self._message_queue.extend(failed_messages)
            
            if processed_count > 0:
                logger.info("Successfully processed %d queued messages", processed_count)
            
            if self._message_queue:
                logger.warning("%d messages remain in queue for retry", len(self._message_queue))
    
    def retry_queued_messages(self) -> int:
        """
        Manually retry publishing queued messages.
        
        Returns:
            int: Number of messages successfully published
        """
        if not self._message_queue:
            logger.debug("No queued messages to retry")
            return 0
        
        logger.info("Manually retrying %d queued messages", len(self._message_queue))
        initial_queue_size = len(self._message_queue)
        
        self._process_queued_messages()
        
        processed_count = initial_queue_size - len(self._message_queue)
        logger.info("Retry completed: %d messages published, %d remain queued", 
                   processed_count, len(self._message_queue))
        
        return processed_count
    
    def get_queue_size(self) -> int:
        """Get the current size of the message queue."""
        with self._queue_lock:
            return len(self._message_queue)
    
    def clear_queue(self) -> int:
        """
        Clear all queued messages.
        
        Returns:
            int: Number of messages that were cleared
        """
        with self._queue_lock:
            cleared_count = len(self._message_queue)
            self._message_queue.clear()
            
        if cleared_count > 0:
            logger.warning("Cleared %d queued messages", cleared_count)
        
        return cleared_count
    
    def get_queued_messages_info(self) -> List[Dict[str, Any]]:
        """
        Get information about queued messages for debugging.
        
        Returns:
            List of dictionaries with message information
        """
        with self._queue_lock:
            return [
                {
                    "message_id": msg.message_id,
                    "timestamp": msg.timestamp,
                    "added_tools_count": len(msg.added_tools),
                    "removed_tools_count": len(msg.removed_tools)
                }
                for msg in self._message_queue
            ]
    
    def get_metrics(self) -> Dict[str, Any]:
        """
        Get publisher metrics for monitoring.
        
        Returns:
            Dictionary containing all metrics
        """
        metrics = self._metrics.get_metrics_summary()
        metrics.update({
            "current_queue_size": self.get_queue_size(),
            "redis_connected": self._redis_connected,
            "redis_key": self.REDIS_KEY
        })
        return metrics
    
    def log_metrics_summary(self) -> None:
        """Log a summary of publisher metrics for monitoring."""
        metrics = self.get_metrics()
        
        logger.info("Tool publisher metrics summary", extra={
            "event_type": "metrics_summary",
            "metrics": metrics
        })
    
    def reset_metrics(self) -> None:
        """Reset all metrics counters."""
        self._metrics = ToolPublisherMetrics()
        logger.info("Tool publisher metrics reset", extra={
            "event_type": "metrics_reset"
        })


# Global tool publisher service instance
_tool_publisher_service: Optional[ToolPublisherService] = None


def get_tool_publisher_service() -> ToolPublisherService:
    """Get the global tool publisher service instance."""
    global _tool_publisher_service
    if _tool_publisher_service is None:
        _tool_publisher_service = ToolPublisherService()
    return _tool_publisher_service