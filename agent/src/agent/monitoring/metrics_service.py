"""
Metrics service for monitoring tool synchronization and system health.

This service provides centralized metrics collection and reporting
for tool discovery, Redis operations, and overall system performance.
"""

import logging
import time
from datetime import datetime, timezone
from typing import Dict, Any, Optional
from dataclasses import dataclass, field
from threading import Lock

from src.agent.tools.discovery import get_tool_discovery_service
from src.agent.redis.tool_publisher import get_tool_publisher_service
from src.agent.redis.client import get_redis_client

logger = logging.getLogger(__name__)


@dataclass
class SystemMetrics:
    """System-wide metrics for monitoring."""
    
    # Tool discovery metrics
    tool_discoveries_total: int = 0
    tool_discovery_latency_total: float = 0.0
    tool_discovery_latency_count: int = 0
    local_tools_discovered: int = 0
    mcp_tools_discovered: int = 0
    mcp_connection_failures: int = 0
    
    # Tool change metrics
    tools_added_total: int = 0
    tools_removed_total: int = 0
    tool_changes_detected: int = 0
    
    # Redis metrics (aggregated from publisher)
    redis_messages_published: int = 0
    redis_messages_failed: int = 0
    redis_connection_failures: int = 0
    redis_connection_recoveries: int = 0
    
    # System health
    last_discovery_time: Optional[datetime] = None
    last_publish_time: Optional[datetime] = None
    last_metrics_reset: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    
    # Sync recovery metrics (agent side)
    resync_requests_received: int = 0
    resync_requests_successful: int = 0
    resync_requests_failed: int = 0
    resync_response_latency_total: float = 0.0
    resync_response_latency_count: int = 0
    last_resync_request: Optional[datetime] = None
    
    def get_average_discovery_latency(self) -> float:
        """Get average tool discovery latency in milliseconds."""
        if self.tool_discovery_latency_count == 0:
            return 0.0
        return self.tool_discovery_latency_total / self.tool_discovery_latency_count
    
    def get_average_resync_latency(self) -> float:
        """Get average resync response latency in milliseconds."""
        if self.resync_response_latency_count == 0:
            return 0.0
        return self.resync_response_latency_total / self.resync_response_latency_count
    
    def get_resync_success_rate(self) -> float:
        """Get resync request success rate."""
        if self.resync_requests_received == 0:
            return 0.0
        return self.resync_requests_successful / self.resync_requests_received


class MetricsService:
    """Service for collecting and reporting system metrics."""
    
    def __init__(self):
        self._metrics = SystemMetrics()
        self._lock = Lock()
        
    def record_tool_discovery(self, latency_ms: float, local_count: int, mcp_count: int, mcp_failed: bool = False):
        """Record tool discovery metrics."""
        with self._lock:
            self._metrics.tool_discoveries_total += 1
            self._metrics.tool_discovery_latency_total += latency_ms
            self._metrics.tool_discovery_latency_count += 1
            self._metrics.local_tools_discovered = local_count
            self._metrics.mcp_tools_discovered = mcp_count
            self._metrics.last_discovery_time = datetime.now(timezone.utc)
            
            if mcp_failed:
                self._metrics.mcp_connection_failures += 1
                
        logger.debug("Recorded tool discovery metrics", extra={
            "event_type": "metrics_recorded",
            "metric_type": "tool_discovery",
            "latency_ms": latency_ms,
            "local_count": local_count,
            "mcp_count": mcp_count,
            "mcp_failed": mcp_failed
        })
    
    def record_tool_changes(self, added_count: int, removed_count: int):
        """Record tool change metrics."""
        with self._lock:
            self._metrics.tools_added_total += added_count
            self._metrics.tools_removed_total += removed_count
            if added_count > 0 or removed_count > 0:
                self._metrics.tool_changes_detected += 1
                
        logger.debug("Recorded tool change metrics", extra={
            "event_type": "metrics_recorded",
            "metric_type": "tool_changes",
            "added_count": added_count,
            "removed_count": removed_count
        })
    
    def record_redis_publish(self, success: bool):
        """Record Redis publish metrics."""
        with self._lock:
            if success:
                self._metrics.redis_messages_published += 1
                self._metrics.last_publish_time = datetime.now(timezone.utc)
            else:
                self._metrics.redis_messages_failed += 1
                
        logger.debug("Recorded Redis publish metrics", extra={
            "event_type": "metrics_recorded",
            "metric_type": "redis_publish",
            "success": success
        })
    
    def record_redis_connection_event(self, event_type: str):
        """Record Redis connection events."""
        with self._lock:
            if event_type == "failure":
                self._metrics.redis_connection_failures += 1
            elif event_type == "recovery":
                self._metrics.redis_connection_recoveries += 1
                
        logger.debug("Recorded Redis connection event", extra={
            "event_type": "metrics_recorded",
            "metric_type": "redis_connection",
            "connection_event": event_type
        })
    
    def record_resync_request(self, success: bool, latency_ms: float):
        """Record resync request metrics."""
        with self._lock:
            self._metrics.resync_requests_received += 1
            self._metrics.resync_response_latency_total += latency_ms
            self._metrics.resync_response_latency_count += 1
            self._metrics.last_resync_request = datetime.now(timezone.utc)
            
            if success:
                self._metrics.resync_requests_successful += 1
            else:
                self._metrics.resync_requests_failed += 1
                
        logger.info("Recorded resync request metrics", extra={
            "event_type": "metrics_recorded",
            "metric_type": "resync_request",
            "success": success,
            "latency_ms": latency_ms,
            "total_requests": self._metrics.resync_requests_received
        })
    
    def get_metrics_summary(self) -> Dict[str, Any]:
        """Get comprehensive metrics summary."""
        with self._lock:
            # Get publisher metrics
            publisher_service = get_tool_publisher_service()
            publisher_metrics = publisher_service.get_metrics()
            
            # Get Redis connection status
            redis_client = get_redis_client()
            redis_connected = redis_client.is_connected()
            
            # Get discovery service info
            discovery_service = get_tool_discovery_service()
            cached_inventory = discovery_service.get_cached_tool_inventory()
            
            return {
                "system_metrics": {
                    "tool_discoveries_total": self._metrics.tool_discoveries_total,
                    "average_discovery_latency_ms": self._metrics.get_average_discovery_latency(),
                    "local_tools_discovered": self._metrics.local_tools_discovered,
                    "mcp_tools_discovered": self._metrics.mcp_tools_discovered,
                    "mcp_connection_failures": self._metrics.mcp_connection_failures,
                    "tools_added_total": self._metrics.tools_added_total,
                    "tools_removed_total": self._metrics.tools_removed_total,
                    "tool_changes_detected": self._metrics.tool_changes_detected,
                    "redis_messages_published": self._metrics.redis_messages_published,
                    "redis_messages_failed": self._metrics.redis_messages_failed,
                    "redis_connection_failures": self._metrics.redis_connection_failures,
                    "redis_connection_recoveries": self._metrics.redis_connection_recoveries,
                    "resync_requests_received": self._metrics.resync_requests_received,
                    "resync_requests_successful": self._metrics.resync_requests_successful,
                    "resync_requests_failed": self._metrics.resync_requests_failed,
                    "average_resync_latency_ms": self._metrics.get_average_resync_latency(),
                    "resync_success_rate": self._metrics.get_resync_success_rate(),
                    "last_discovery_time": self._metrics.last_discovery_time.isoformat() if self._metrics.last_discovery_time else None,
                    "last_publish_time": self._metrics.last_publish_time.isoformat() if self._metrics.last_publish_time else None,
                    "last_resync_request": self._metrics.last_resync_request.isoformat() if self._metrics.last_resync_request else None,
                    "last_metrics_reset": self._metrics.last_metrics_reset.isoformat()
                },
                "publisher_metrics": publisher_metrics,
                "redis_status": {
                    "connected": redis_connected
                },
                "discovery_status": {
                    "cached_tool_count": len(cached_inventory) if cached_inventory else 0,
                    "has_cached_inventory": cached_inventory is not None
                }
            }
    
    def log_metrics_summary(self):
        """Log comprehensive metrics summary for monitoring."""
        metrics = self.get_metrics_summary()
        
        logger.info("System metrics summary", extra={
            "event_type": "system_metrics_summary",
            "metrics": metrics
        })
    
    def reset_metrics(self):
        """Reset all metrics counters."""
        with self._lock:
            self._metrics = SystemMetrics()
            
        # Also reset publisher metrics
        publisher_service = get_tool_publisher_service()
        publisher_service.reset_metrics()
        
        logger.info("All metrics reset", extra={
            "event_type": "metrics_reset"
        })
    
    def get_health_status(self) -> Dict[str, Any]:
        """Get system health status for monitoring."""
        metrics = self.get_metrics_summary()
        now = datetime.now(timezone.utc)
        
        # Calculate time since last discovery
        last_discovery = self._metrics.last_discovery_time
        discovery_age_minutes = None
        if last_discovery:
            discovery_age_minutes = (now - last_discovery).total_seconds() / 60
        
        # Calculate time since last publish
        last_publish = self._metrics.last_publish_time
        publish_age_minutes = None
        if last_publish:
            publish_age_minutes = (now - last_publish).total_seconds() / 60
        
        # Determine health status
        health_issues = []
        
        if discovery_age_minutes and discovery_age_minutes > 60:  # No discovery in 1 hour
            health_issues.append("tool_discovery_stale")
        
        if self._metrics.mcp_connection_failures > 0:
            health_issues.append("mcp_connection_issues")
        
        if self._metrics.redis_connection_failures > self._metrics.redis_connection_recoveries:
            health_issues.append("redis_connection_issues")
        
        redis_connected = metrics["redis_status"]["connected"]
        if not redis_connected:
            health_issues.append("redis_disconnected")
        
        # Check resync request success rate
        if self._metrics.resync_requests_received > 0:
            resync_success_rate = self._metrics.get_resync_success_rate()
            if resync_success_rate < 0.8:  # Less than 80% success rate
                health_issues.append("resync_failures")
        
        health_status = "healthy" if not health_issues else "degraded"
        
        return {
            "status": health_status,
            "issues": health_issues,
            "last_discovery_age_minutes": discovery_age_minutes,
            "last_publish_age_minutes": publish_age_minutes,
            "redis_connected": redis_connected,
            "cached_tools": metrics["discovery_status"]["cached_tool_count"],
            "queue_size": metrics["publisher_metrics"]["current_queue_size"],
            "resync_requests": self._metrics.resync_requests_received,
            "resync_success_rate": self._metrics.get_resync_success_rate()
        }


# Global metrics service instance
_metrics_service: Optional[MetricsService] = None


def get_metrics_service() -> MetricsService:
    """Get the global metrics service instance."""
    global _metrics_service
    if _metrics_service is None:
        _metrics_service = MetricsService()
    return _metrics_service