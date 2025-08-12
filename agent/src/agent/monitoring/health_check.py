"""
Health check and monitoring utilities for the agent.

This module provides health check endpoints and monitoring utilities
for system administrators to monitor the agent's health and performance.
"""

import logging
import asyncio
import time
from typing import Dict, Any
from datetime import datetime, timezone

from src.agent.monitoring.metrics_service import get_metrics_service
from src.agent.tools.discovery import get_tool_discovery_service
from src.agent.redis.tool_publisher import get_tool_publisher_service
from src.agent.redis.client import get_redis_client

logger = logging.getLogger(__name__)


class HealthChecker:
    """Health checker for agent components."""
    
    def __init__(self):
        self.metrics_service = get_metrics_service()
        
    async def perform_health_check(self) -> Dict[str, Any]:
        """
        Perform comprehensive health check of all agent components.
        
        Returns:
            Dictionary containing health check results
        """
        start_time = time.time()
        
        logger.info("Starting comprehensive health check", extra={
            "event_type": "health_check_start"
        })
        
        health_results = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "overall_status": "healthy",
            "components": {},
            "metrics": {},
            "issues": []
        }
        
        # Check Redis connectivity
        redis_health = await self._check_redis_health()
        health_results["components"]["redis"] = redis_health
        
        # Check tool discovery service
        discovery_health = await self._check_discovery_health()
        health_results["components"]["tool_discovery"] = discovery_health
        
        # Check tool publisher service
        publisher_health = self._check_publisher_health()
        health_results["components"]["tool_publisher"] = publisher_health
        
        # Get system metrics
        health_results["metrics"] = self.metrics_service.get_metrics_summary()
        
        # Get overall health status
        system_health = self.metrics_service.get_health_status()
        health_results["overall_status"] = system_health["status"]
        health_results["issues"] = system_health["issues"]
        
        # Determine if any components are unhealthy
        component_issues = []
        for component, status in health_results["components"].items():
            if status["status"] != "healthy":
                component_issues.append(f"{component}_{status['status']}")
        
        if component_issues:
            health_results["issues"].extend(component_issues)
            if health_results["overall_status"] == "healthy":
                health_results["overall_status"] = "degraded"
        
        check_time = (time.time() - start_time) * 1000
        health_results["check_duration_ms"] = check_time
        
        logger.info("Health check completed", extra={
            "event_type": "health_check_complete",
            "overall_status": health_results["overall_status"],
            "check_duration_ms": check_time,
            "issues_count": len(health_results["issues"])
        })
        
        return health_results
    
    async def _check_redis_health(self) -> Dict[str, Any]:
        """Check Redis connectivity and health."""
        try:
            redis_client = get_redis_client()
            
            # Test connection
            ping_start = time.time()
            is_connected = redis_client.is_connected()
            ping_time = (time.time() - ping_start) * 1000
            
            if is_connected:
                # Test basic operations
                test_key = "health_check_test"
                redis_client.lpush(test_key, "test_value")
                length = redis_client.llen(test_key)
                
                return {
                    "status": "healthy",
                    "connected": True,
                    "ping_time_ms": ping_time,
                    "test_operations": "passed",
                    "list_length": length
                }
            else:
                return {
                    "status": "unhealthy",
                    "connected": False,
                    "ping_time_ms": ping_time,
                    "error": "Redis connection failed"
                }
                
        except Exception as e:
            logger.error("Redis health check failed", exc_info=True)
            return {
                "status": "unhealthy",
                "connected": False,
                "error": str(e),
                "error_type": type(e).__name__
            }
    
    async def _check_discovery_health(self) -> Dict[str, Any]:
        """Check tool discovery service health."""
        try:
            discovery_service = get_tool_discovery_service()
            
            # Test tool discovery
            discovery_start = time.time()
            tools = await discovery_service.discover_tools()
            discovery_time = (time.time() - discovery_start) * 1000
            
            cached_inventory = discovery_service.get_cached_tool_inventory()
            
            return {
                "status": "healthy",
                "tools_discovered": len(tools),
                "discovery_time_ms": discovery_time,
                "cached_inventory_size": len(cached_inventory) if cached_inventory else 0,
                "mcp_connection_failed": discovery_service._mcp_connection_failed
            }
            
        except Exception as e:
            logger.error("Tool discovery health check failed", exc_info=True)
            return {
                "status": "unhealthy",
                "error": str(e),
                "error_type": type(e).__name__
            }
    
    def _check_publisher_health(self) -> Dict[str, Any]:
        """Check tool publisher service health."""
        try:
            publisher_service = get_tool_publisher_service()
            
            # Get publisher metrics
            metrics = publisher_service.get_metrics()
            queue_size = publisher_service.get_queue_size()
            
            # Determine health based on queue size and connection status
            status = "healthy"
            if queue_size > 50:  # High queue size indicates issues
                status = "degraded"
            elif not metrics.get("redis_connected", False):
                status = "unhealthy"
            
            return {
                "status": status,
                "queue_size": queue_size,
                "messages_published": metrics.get("messages_published", 0),
                "messages_failed": metrics.get("messages_failed", 0),
                "redis_connected": metrics.get("redis_connected", False),
                "average_latency_ms": metrics.get("average_latency_ms", 0)
            }
            
        except Exception as e:
            logger.error("Tool publisher health check failed", exc_info=True)
            return {
                "status": "unhealthy",
                "error": str(e),
                "error_type": type(e).__name__
            }
    
    def log_health_summary(self):
        """Log a summary of system health for monitoring."""
        try:
            # Use asyncio to run the async health check
            loop = asyncio.get_event_loop()
            if loop.is_running():
                # If we're already in an async context, we can't run another event loop
                health_status = self.metrics_service.get_health_status()
                logger.info("System health summary (sync)", extra={
                    "event_type": "health_summary",
                    "health_status": health_status
                })
            else:
                health_results = loop.run_until_complete(self.perform_health_check())
                logger.info("System health summary", extra={
                    "event_type": "health_summary",
                    "overall_status": health_results["overall_status"],
                    "issues": health_results["issues"],
                    "check_duration_ms": health_results["check_duration_ms"]
                })
        except Exception as e:
            logger.error("Failed to log health summary", exc_info=True)


# Global health checker instance
_health_checker = None


def get_health_checker() -> HealthChecker:
    """Get the global health checker instance."""
    global _health_checker
    if _health_checker is None:
        _health_checker = HealthChecker()
    return _health_checker


# Convenience functions for monitoring
def log_system_metrics():
    """Log system metrics for monitoring."""
    metrics_service = get_metrics_service()
    metrics_service.log_metrics_summary()


def log_system_health():
    """Log system health for monitoring."""
    health_checker = get_health_checker()
    health_checker.log_health_summary()


async def get_system_status() -> Dict[str, Any]:
    """Get comprehensive system status for monitoring."""
    health_checker = get_health_checker()
    return await health_checker.perform_health_check()