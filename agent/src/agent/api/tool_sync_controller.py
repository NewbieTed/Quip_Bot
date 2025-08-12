"""
Tool sync controller for handling HTTP-based tool synchronization requests.

This controller provides endpoints for the backend to request immediate tool
synchronization when Redis message processing fails or needs recovery.
"""

import logging
import asyncio
from datetime import datetime
from typing import List

from fastapi import HTTPException
from http import HTTPStatus

from src.agent.models.tool_sync import ToolResyncRequest, ToolInventoryResponse, create_resync_response
from src.agent.tools.discovery import get_tool_discovery_service
from src.agent.monitoring.metrics_service import get_metrics_service
from src.config.config_loader import Config

logger = logging.getLogger(__name__)


class ToolSyncController:
    """Controller for handling tool synchronization HTTP requests."""
    
    def __init__(self):
        self.tool_discovery_service = get_tool_discovery_service()
        self.metrics_service = get_metrics_service()
        # Load configuration from config system
        tool_sync_config = Config.get_tool_sync_config()
        http_server_config = tool_sync_config.get("http_server", {})
        discovery_config = http_server_config.get("discovery", {})
        
        self.discovery_timeout = float(discovery_config.get("timeout", 10))
        self.retry_attempts = discovery_config.get("retry_attempts", 2)
        self.retry_delay = discovery_config.get("retry_delay", 1)
    
    async def handle_resync_request(self, request: ToolResyncRequest) -> ToolInventoryResponse:
        """
        Handle a tool resync request from the backend.
        
        Performs immediate tool discovery and returns the complete current tool inventory.
        
        Args:
            request: The resync request from the backend
            
        Returns:
            ToolInventoryResponse with the current tool inventory
            
        Raises:
            HTTPException: If tool discovery fails or times out
        """
        start_time = datetime.now()
        
        logger.info("Received tool resync request", extra={
            "event_type": "resync_request_received",
            "request_id": request.request_id,
            "reason": request.reason,
            "request_timestamp": request.timestamp.isoformat()
        })
        
        try:
            # Perform tool discovery with timeout
            current_tools = await self._discover_tools_with_timeout()
            
            # Extract tool info from BaseTool instances
            from src.agent.models.tool_sync import ToolInfo
            tool_infos = []
            for tool in current_tools:
                server_name = getattr(tool, '_mcp_server_name', 'built-in')
                tool_infos.append(ToolInfo(name=tool.name, mcp_server_name=server_name))
            
            # Create response
            response = create_resync_response(request, tool_infos)
            
            # Record successful resync metrics
            latency_ms = (datetime.now() - start_time).total_seconds() * 1000
            self.metrics_service.record_resync_request(True, latency_ms)
            
            logger.info("Tool resync request completed successfully", extra={
                "event_type": "resync_request_completed",
                "request_id": request.request_id,
                "tool_count": len(tool_infos),
                "tool_names": [tool.name for tool in tool_infos],
                "discovery_timestamp": response.discovery_timestamp.isoformat(),
                "latency_ms": latency_ms
            })
            
            return response
            
        except asyncio.TimeoutError:
            # Record failed resync metrics
            latency_ms = (datetime.now() - start_time).total_seconds() * 1000
            self.metrics_service.record_resync_request(False, latency_ms)
            
            error_msg = f"Tool discovery timed out after {self.discovery_timeout} seconds"
            logger.error("Tool resync request failed due to timeout", extra={
                "event_type": "resync_request_timeout",
                "request_id": request.request_id,
                "timeout_seconds": self.discovery_timeout,
                "error": error_msg,
                "latency_ms": latency_ms
            })
            raise HTTPException(
                status_code=HTTPStatus.REQUEST_TIMEOUT,
                detail=error_msg
            )
            
        except Exception as e:
            # Record failed resync metrics
            latency_ms = (datetime.now() - start_time).total_seconds() * 1000
            self.metrics_service.record_resync_request(False, latency_ms)
            
            error_msg = f"Tool discovery failed: {str(e)}"
            logger.error("Tool resync request failed due to discovery error", extra={
                "event_type": "resync_request_error",
                "request_id": request.request_id,
                "error": error_msg,
                "exception_type": type(e).__name__,
                "latency_ms": latency_ms
            })
            raise HTTPException(
                status_code=HTTPStatus.INTERNAL_SERVER_ERROR,
                detail=error_msg
            )
    
    async def _discover_tools_with_timeout(self) -> List:
        """
        Perform tool discovery with timeout management.
        
        Returns:
            List of BaseTool instances
            
        Raises:
            asyncio.TimeoutError: If discovery takes longer than the configured timeout
            Exception: If tool discovery fails for other reasons
        """
        try:
            # Use asyncio.wait_for to enforce timeout
            current_tools = await asyncio.wait_for(
                self.tool_discovery_service.discover_tools(),
                timeout=self.discovery_timeout
            )
            
            logger.debug("Tool discovery completed within timeout", extra={
                "event_type": "tool_discovery_success",
                "tool_count": len(current_tools),
                "timeout_seconds": self.discovery_timeout
            })
            
            return current_tools
            
        except asyncio.TimeoutError:
            logger.warning("Tool discovery exceeded timeout", extra={
                "event_type": "tool_discovery_timeout",
                "timeout_seconds": self.discovery_timeout
            })
            raise
            
        except Exception as e:
            logger.error("Tool discovery failed with exception", extra={
                "event_type": "tool_discovery_error",
                "error": str(e),
                "exception_type": type(e).__name__
            })
            raise


# Global controller instance
_tool_sync_controller = None


def get_tool_sync_controller() -> ToolSyncController:
    """Get the global tool sync controller instance."""
    global _tool_sync_controller
    if _tool_sync_controller is None:
        _tool_sync_controller = ToolSyncController()
    return _tool_sync_controller