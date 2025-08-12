"""
Tool discovery service for the agent.

This module provides functionality to discover available tools on-demand,
detect changes in tool availability, and handle MCP server fallback scenarios.
"""

import logging
import asyncio
import time
from typing import List, Set, Dict, Tuple, Optional
from langchain_core.tools import BaseTool
from langchain_mcp_adapters.client import MultiServerMCPClient
from src.config import Config

logger = logging.getLogger(__name__)


class ToolDiscoveryService:
    """Service for discovering and tracking tool availability changes."""
    
    def __init__(self):
        self._cached_tool_inventory: Optional[Set[str]] = None
        self._mcp_client: Optional[MultiServerMCPClient] = None
        self._mcp_connection_failed = False
        
    async def discover_tools(self) -> List[BaseTool]:
        """
        Discover all available tools (local + MCP with fallback).
        
        Returns:
            List of all available BaseTool instances with MCP server info attached
        """
        start_time = time.time()
        
        logger.debug("Starting tool discovery", extra={
            "event_type": "tool_discovery_start"
        })
        
        # Get local tools
        local_start_time = time.time()
        local_tools = self._discover_local_tools()
        local_discovery_time = (time.time() - local_start_time) * 1000
        
        logger.info("Discovered local tools", extra={
            "event_type": "local_tools_discovered",
            "tool_count": len(local_tools),
            "tool_names": [tool.name for tool in local_tools],
            "discovery_time_ms": local_discovery_time
        })
        
        # Get MCP tools with fallback handling
        mcp_start_time = time.time()
        mcp_tools = await self._discover_mcp_tools_with_fallback()
        mcp_discovery_time = (time.time() - mcp_start_time) * 1000
        
        logger.info("Discovered MCP tools", extra={
            "event_type": "mcp_tools_discovered",
            "tool_count": len(mcp_tools),
            "tool_names": [tool.name for tool in mcp_tools],
            "discovery_time_ms": mcp_discovery_time,
            "mcp_connection_failed": self._mcp_connection_failed
        })
        
        # Combine and validate for duplicates
        all_tools = local_tools + mcp_tools
        self._validate_tool_names(all_tools)
        
        total_discovery_time = (time.time() - start_time) * 1000
        
        logger.info("Tool discovery completed", extra={
            "event_type": "tool_discovery_complete",
            "total_tools": len(all_tools),
            "local_tools": len(local_tools),
            "mcp_tools": len(mcp_tools),
            "total_discovery_time_ms": total_discovery_time,
            "local_discovery_time_ms": local_discovery_time,
            "mcp_discovery_time_ms": mcp_discovery_time
        })
        
        return all_tools
    
    def _discover_local_tools(self) -> List[BaseTool]:
        """
        Discover local tools using the existing tool loading mechanism.
        
        Returns:
            List of local BaseTool instances with built-in server info attached
        """
        try:
            # Import here to avoid circular import
            from src.agent.tools import get_all_tools
            
            # Use force_reload=True to ensure we get the latest tools
            local_tools = get_all_tools(force_reload=True)
            
            # Mark all local tools as coming from the built-in server
            for tool in local_tools:
                tool._mcp_server_name = "built-in"
            
            logger.debug("Successfully loaded %d local tools", len(local_tools))
            return local_tools
        except Exception as e:
            logger.error("Error discovering local tools: %s", str(e))
            return []
    
    async def _discover_mcp_tools_with_fallback(self) -> List[BaseTool]:
        """
        Discover MCP tools with connection failure fallback.
        
        Returns:
            List of MCP BaseTool instances with server info attached, empty list if MCP unavailable
        """
        mcp_config = Config.get_mcp_config()
        mcp_enabled = mcp_config.get("enabled", True)
        
        if not mcp_enabled:
            logger.info("MCP disabled in configuration, skipping MCP tool discovery")
            return []
        
        # If we previously failed to connect and haven't reset, skip MCP discovery
        if self._mcp_connection_failed:
            logger.debug("Skipping MCP discovery due to previous connection failure")
            return []
        
        try:
            # Create or reuse MCP client
            if self._mcp_client is None:
                self._mcp_client = await self._create_mcp_client()
                if self._mcp_client is None:
                    return []
            
            # Get tools from MCP client
            mcp_tools = await self._mcp_client.get_tools()
            
            # Validate tool naming and assign server information
            servers_config = mcp_config.get("servers", {})
            validated_tools = []
            
            for tool in mcp_tools:
                # Find which server this tool belongs to based on naming convention
                server_name = None
                tool_name = tool.name
                
                # Check each configured server to see if tool name starts with server name
                for configured_server_name in servers_config.keys():
                    expected_prefix = f"{configured_server_name}-"
                    if tool_name.startswith(expected_prefix):
                        server_name = configured_server_name
                        break
                
                if server_name is None:
                    # Tool doesn't follow naming convention
                    server_names = list(servers_config.keys())
                    expected_prefixes = [f"{name}-" for name in server_names]
                    
                    error_msg = (
                        f"MCP tool '{tool_name}' does not follow naming convention. "
                        f"Expected tool name to start with one of: {expected_prefixes}, "
                        f"but actual name is '{tool_name}'"
                    )
                    
                    logger.error("Tool naming validation failed", extra={
                        "event_type": "tool_naming_validation_error",
                        "tool_name": tool_name,
                        "expected_prefixes": expected_prefixes,
                        "configured_servers": server_names
                    })
                    
                    raise ValueError(error_msg)
                
                # Mark tool with its server name
                tool._mcp_server_name = server_name
                validated_tools.append(tool)
                
                logger.debug("Validated tool '%s' belongs to server '%s'", tool_name, server_name)
            
            logger.debug("Successfully validated and retrieved %d tools from MCP servers", len(validated_tools))
            
            # Reset connection failure flag on success
            self._mcp_connection_failed = False
            
            return validated_tools
            
        except Exception as e:
            logger.warning("MCP server connection failed, falling back to local tools only: %s", str(e))
            self._mcp_connection_failed = True
            self._mcp_client = None
            return []
    
    async def _create_mcp_client(self) -> Optional[MultiServerMCPClient]:
        """
        Create MCP client with server configuration.
        
        Returns:
            MultiServerMCPClient instance or None if creation fails
        """
        try:
            mcp_config = Config.get_mcp_config()
            servers_config = {}
            
            for server_name, server_config in mcp_config.get("servers", {}).items():
                server_url = server_config.get("url", Config.MCP_SERVER_URL)
                servers_config[server_name] = {
                    "url": server_url,
                    "transport": server_config.get("transport", "streamable_http")
                }
                logger.info("Configured MCP server '%s' with URL: %s", server_name, server_url)
            
            if not servers_config:
                logger.info("No MCP servers configured")
                return None
            
            client = MultiServerMCPClient(servers_config)
            logger.info("Created MCP client with %d servers", len(servers_config))
            return client
            
        except Exception as e:
            logger.error("Failed to create MCP client: %s", str(e))
            return None
    
    def _validate_tool_names(self, tools: List[BaseTool]) -> None:
        """
        Validate that there are no duplicate tool names.
        
        Args:
            tools: List of tools to validate
            
        Raises:
            ValueError: If duplicate tool names are found
        """
        name_counts = {}
        for tool in tools:
            name_counts[tool.name] = name_counts.get(tool.name, 0) + 1
        
        duplicate_names = [name for name, count in name_counts.items() if count > 1]
        if duplicate_names:
            logger.error("Duplicate tool names detected: %s", duplicate_names)
            raise ValueError(f"Duplicate tool names detected: {duplicate_names}")
    
    def compare_tool_sets(self, current_tools: List[BaseTool]) -> Tuple[List[Dict[str, str]], List[Dict[str, str]]]:
        """
        Compare current tools with cached inventory to detect changes.
        
        Args:
            current_tools: List of currently available tools
            
        Returns:
            Tuple of (added_tools, removed_tools) as lists of tool info dicts with name and mcpServerName
        """
        # Create tool info dictionaries for current tools
        current_tool_info = {}
        for tool in current_tools:
            server_name = getattr(tool, '_mcp_server_name', 'built-in')
            current_tool_info[tool.name] = {
                "name": tool.name,
                "mcpServerName": server_name
            }
        
        current_tool_names = set(current_tool_info.keys())
        
        if self._cached_tool_inventory is None:
            # First time discovery - all tools are "added"
            added_tools = list(current_tool_info.values())
            removed_tools = []
            
            logger.info("First tool discovery, treating all tools as added", extra={
                "event_type": "first_tool_discovery",
                "tool_count": len(current_tool_names),
                "added_tools": [tool["name"] for tool in added_tools]
            })
        else:
            # Compare with cached inventory
            added_tool_names = current_tool_names - self._cached_tool_inventory
            removed_tool_names = self._cached_tool_inventory - current_tool_names
            
            # Convert to tool info dictionaries
            added_tools = [current_tool_info[name] for name in added_tool_names]
            # For removed tools, we don't have server info, so we'll use a placeholder
            removed_tools = [{"name": name, "mcpServerName": "unknown"} for name in removed_tool_names]
            
            if added_tools or removed_tools:
                logger.info("Tool changes detected", extra={
                    "event_type": "tool_changes_detected",
                    "added_tools": [tool["name"] for tool in added_tools],
                    "removed_tools": [tool["name"] for tool in removed_tools],
                    "added_count": len(added_tools),
                    "removed_count": len(removed_tools),
                    "previous_count": len(self._cached_tool_inventory),
                    "current_count": len(current_tool_names)
                })
            else:
                logger.debug("No tool changes detected", extra={
                    "event_type": "no_tool_changes",
                    "tool_count": len(current_tool_names)
                })
        
        return added_tools, removed_tools
    
    def get_tool_changes(self, current_tools: List[BaseTool]) -> Tuple[List[Dict[str, str]], List[Dict[str, str]]]:
        """
        Get tool changes and update cached inventory.
        
        Args:
            current_tools: List of currently available tools
            
        Returns:
            Tuple of (added_tools, removed_tools) as lists of tool info dicts with name and mcpServerName
        """
        added_tools, removed_tools = self.compare_tool_sets(current_tools)
        
        # Update cached inventory
        current_tool_names = {tool.name for tool in current_tools}
        self._cached_tool_inventory = current_tool_names.copy()
        
        logger.debug("Updated cached tool inventory with %d tools", 
                    len(self._cached_tool_inventory))
        
        return added_tools, removed_tools
    
    def reset_mcp_connection(self) -> None:
        """
        Reset MCP connection state to allow retry on next discovery.
        
        This can be called to reset the connection failure flag and allow
        the service to attempt MCP connection again.
        """
        logger.info("Resetting MCP connection state")
        self._mcp_connection_failed = False
        self._mcp_client = None
    
    def get_cached_tool_inventory(self) -> Optional[Set[str]]:
        """
        Get the current cached tool inventory.
        
        Returns:
            Set of cached tool names or None if no cache exists
        """
        return self._cached_tool_inventory.copy() if self._cached_tool_inventory else None


# Global tool discovery service instance
_tool_discovery_service: Optional[ToolDiscoveryService] = None


def get_tool_discovery_service() -> ToolDiscoveryService:
    """Get the global tool discovery service instance."""
    global _tool_discovery_service
    if _tool_discovery_service is None:
        _tool_discovery_service = ToolDiscoveryService()
    return _tool_discovery_service