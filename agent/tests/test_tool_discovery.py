"""
Unit tests for tool discovery service.
"""

import pytest
import asyncio
from unittest.mock import Mock, patch, AsyncMock, MagicMock
from langchain_core.tools import BaseTool

from src.agent.tools.discovery import ToolDiscoveryService, get_tool_discovery_service


class TestToolDiscoveryService:
    """Test cases for ToolDiscoveryService class."""

    def test_init(self):
        """Test service initialization."""
        service = ToolDiscoveryService()
        
        assert service._cached_tool_inventory is None
        assert service._mcp_client is None
        assert service._mcp_connection_failed is False

    @pytest.mark.asyncio
    async def test_discover_tools_local_only(self):
        """Test tool discovery with local tools only."""
        service = ToolDiscoveryService()
        
        # Mock local tools
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "local-tool1"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "local-tool2"
        
        # Mock the get_all_tools function that's imported inside the method
        with patch('src.agent.tools.get_all_tools', return_value=[mock_tool1, mock_tool2]):
            with patch.object(service, '_discover_mcp_tools_with_fallback', return_value=[]):
                with patch.object(service, '_validate_tool_names'):
                    
                    tools = await service.discover_tools()
                    
                    assert len(tools) == 2
                    assert tools[0].name == "local-tool1"
                    assert tools[1].name == "local-tool2"
                    # Local tools should be marked as built-in
                    assert hasattr(tools[0], '_mcp_server_name')
                    assert hasattr(tools[1], '_mcp_server_name')
                    assert tools[0]._mcp_server_name == "built-in"
                    assert tools[1]._mcp_server_name == "built-in"

    @pytest.mark.asyncio
    async def test_discover_tools_with_mcp(self):
        """Test tool discovery with both local and MCP tools."""
        service = ToolDiscoveryService()
        
        # Mock local tools
        mock_local_tool = Mock(spec=BaseTool)
        mock_local_tool.name = "local-tool"
        
        # Mock MCP tools
        mock_mcp_tool = Mock(spec=BaseTool)
        mock_mcp_tool.name = "mcp-tool"
        mock_mcp_tool._mcp_server_name = "test-server"
        
        with patch.object(service, '_discover_local_tools', return_value=[mock_local_tool]):
            with patch.object(service, '_discover_mcp_tools_with_fallback', return_value=[mock_mcp_tool]):
                with patch.object(service, '_validate_tool_names'):
                    
                    tools = await service.discover_tools()
                    
                    assert len(tools) == 2
                    assert any(tool.name == "local-tool" for tool in tools)
                    assert any(tool.name == "mcp-tool" for tool in tools)

    def test_discover_local_tools_success(self):
        """Test successful local tool discovery."""
        service = ToolDiscoveryService()
        
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "tool1"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "tool2"
        
        # Patch the get_all_tools function where it's imported
        with patch('src.agent.tools.get_all_tools', return_value=[mock_tool1, mock_tool2]):
            tools = service._discover_local_tools()
            
            assert len(tools) == 2
            assert tools[0]._mcp_server_name == "built-in"
            assert tools[1]._mcp_server_name == "built-in"

    def test_discover_local_tools_error(self):
        """Test local tool discovery with error."""
        service = ToolDiscoveryService()
        
        with patch('src.agent.tools.get_all_tools', side_effect=Exception("Import error")):
            tools = service._discover_local_tools()
            
            assert tools == []

    @pytest.mark.asyncio
    async def test_discover_mcp_tools_disabled(self):
        """Test MCP tool discovery when disabled."""
        service = ToolDiscoveryService()
        
        mock_config = {"enabled": False}
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            tools = await service._discover_mcp_tools_with_fallback()
            
            assert tools == []

    @pytest.mark.asyncio
    async def test_discover_mcp_tools_connection_failed(self):
        """Test MCP tool discovery when connection previously failed."""
        service = ToolDiscoveryService()
        service._mcp_connection_failed = True
        
        tools = await service._discover_mcp_tools_with_fallback()
        
        assert tools == []

    @pytest.mark.asyncio
    async def test_discover_mcp_tools_success(self):
        """Test successful MCP tool discovery."""
        service = ToolDiscoveryService()
        
        # Mock MCP tools
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "server1-weather"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "server2-location"
        
        mock_client = AsyncMock()
        mock_client.get_tools.return_value = [mock_tool1, mock_tool2]
        
        mock_config = {
            "enabled": True,
            "servers": {
                "server1": {"url": "http://localhost:8001"},
                "server2": {"url": "http://localhost:8002"}
            }
        }
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            with patch.object(service, '_create_mcp_client', return_value=mock_client):
                
                tools = await service._discover_mcp_tools_with_fallback()
                
                assert len(tools) == 2
                assert tools[0]._mcp_server_name == "server1"
                assert tools[1]._mcp_server_name == "server2"

    @pytest.mark.asyncio
    async def test_discover_mcp_tools_naming_validation_error(self):
        """Test MCP tool discovery with naming validation error."""
        service = ToolDiscoveryService()
        
        # Mock tool with invalid name (doesn't follow server- prefix convention)
        mock_tool = Mock(spec=BaseTool)
        mock_tool.name = "invalid-tool-name"
        
        mock_client = AsyncMock()
        mock_client.get_tools.return_value = [mock_tool]
        
        mock_config = {
            "enabled": True,
            "servers": {
                "server1": {"url": "http://localhost:8001"}
            }
        }
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            with patch.object(service, '_create_mcp_client', return_value=mock_client):
                
                tools = await service._discover_mcp_tools_with_fallback()
                
                # Should return empty list due to validation error
                assert tools == []
                assert service._mcp_connection_failed is True

    @pytest.mark.asyncio
    async def test_discover_mcp_tools_connection_error(self):
        """Test MCP tool discovery with connection error."""
        service = ToolDiscoveryService()
        
        mock_config = {
            "enabled": True,
            "servers": {
                "server1": {"url": "http://localhost:8001"}
            }
        }
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            with patch.object(service, '_create_mcp_client', side_effect=Exception("Connection failed")):
                
                tools = await service._discover_mcp_tools_with_fallback()
                
                assert tools == []
                assert service._mcp_connection_failed is True

    @pytest.mark.asyncio
    async def test_create_mcp_client_success(self):
        """Test successful MCP client creation."""
        service = ToolDiscoveryService()
        
        mock_config = {
            "servers": {
                "server1": {"url": "http://localhost:8001"},
                "server2": {"url": "http://localhost:8002", "transport": "custom"}
            }
        }
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            with patch('src.agent.tools.discovery.MultiServerMCPClient') as mock_client_class:
                mock_client = Mock()
                mock_client_class.return_value = mock_client
                
                client = await service._create_mcp_client()
                
                assert client == mock_client
                # Verify client was created with correct config
                call_args = mock_client_class.call_args[0][0]
                assert "server1" in call_args
                assert "server2" in call_args
                assert call_args["server1"]["url"] == "http://localhost:8001"
                assert call_args["server2"]["transport"] == "custom"

    @pytest.mark.asyncio
    async def test_create_mcp_client_no_servers(self):
        """Test MCP client creation with no servers configured."""
        service = ToolDiscoveryService()
        
        mock_config = {"servers": {}}
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            client = await service._create_mcp_client()
            
            assert client is None

    @pytest.mark.asyncio
    async def test_create_mcp_client_error(self):
        """Test MCP client creation with error."""
        service = ToolDiscoveryService()
        
        mock_config = {
            "servers": {
                "server1": {"url": "http://localhost:8001"}
            }
        }
        
        with patch('src.agent.tools.discovery.Config.get_mcp_config', return_value=mock_config):
            with patch('src.agent.tools.discovery.MultiServerMCPClient', side_effect=Exception("Client error")):
                
                client = await service._create_mcp_client()
                
                assert client is None

    def test_validate_tool_names_success(self):
        """Test successful tool name validation."""
        service = ToolDiscoveryService()
        
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "tool1"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "tool2"
        
        # Should not raise any exception
        service._validate_tool_names([mock_tool1, mock_tool2])

    def test_validate_tool_names_duplicates(self):
        """Test tool name validation with duplicates."""
        service = ToolDiscoveryService()
        
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "duplicate-tool"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "duplicate-tool"
        
        with pytest.raises(ValueError) as exc_info:
            service._validate_tool_names([mock_tool1, mock_tool2])
        
        assert "Duplicate tool names detected" in str(exc_info.value)

    def test_compare_tool_sets_first_discovery(self):
        """Test tool set comparison on first discovery."""
        service = ToolDiscoveryService()
        
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "tool1"
        mock_tool1._mcp_server_name = "built-in"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "tool2"
        mock_tool2._mcp_server_name = "test-server"
        
        added_tools, removed_tools = service.compare_tool_sets([mock_tool1, mock_tool2])
        
        assert len(added_tools) == 2
        assert len(removed_tools) == 0
        assert added_tools[0]["name"] == "tool1"
        assert added_tools[0]["mcpServerName"] == "built-in"
        assert added_tools[1]["name"] == "tool2"
        assert added_tools[1]["mcpServerName"] == "test-server"

    def test_compare_tool_sets_with_changes(self):
        """Test tool set comparison with changes."""
        service = ToolDiscoveryService()
        
        # Set up cached inventory
        service._cached_tool_inventory = {"tool1", "tool2"}
        
        # Current tools: tool2 remains, tool3 is new, tool1 is removed
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "tool2"
        mock_tool2._mcp_server_name = "built-in"
        mock_tool3 = Mock(spec=BaseTool)
        mock_tool3.name = "tool3"
        mock_tool3._mcp_server_name = "test-server"
        
        added_tools, removed_tools = service.compare_tool_sets([mock_tool2, mock_tool3])
        
        assert len(added_tools) == 1
        assert len(removed_tools) == 1
        assert added_tools[0]["name"] == "tool3"
        assert added_tools[0]["mcpServerName"] == "test-server"
        assert removed_tools[0]["name"] == "tool1"
        assert removed_tools[0]["mcpServerName"] == "unknown"

    def test_compare_tool_sets_no_changes(self):
        """Test tool set comparison with no changes."""
        service = ToolDiscoveryService()
        
        # Set up cached inventory
        service._cached_tool_inventory = {"tool1", "tool2"}
        
        # Current tools match cached inventory
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "tool1"
        mock_tool1._mcp_server_name = "built-in"
        mock_tool2 = Mock(spec=BaseTool)
        mock_tool2.name = "tool2"
        mock_tool2._mcp_server_name = "built-in"
        
        added_tools, removed_tools = service.compare_tool_sets([mock_tool1, mock_tool2])
        
        assert len(added_tools) == 0
        assert len(removed_tools) == 0

    def test_get_tool_changes_updates_cache(self):
        """Test that get_tool_changes updates cached inventory."""
        service = ToolDiscoveryService()
        
        mock_tool1 = Mock(spec=BaseTool)
        mock_tool1.name = "tool1"
        mock_tool1._mcp_server_name = "built-in"
        
        added_tools, removed_tools = service.get_tool_changes([mock_tool1])
        
        assert service._cached_tool_inventory == {"tool1"}
        assert len(added_tools) == 1
        assert added_tools[0]["name"] == "tool1"

    def test_reset_mcp_connection(self):
        """Test resetting MCP connection state."""
        service = ToolDiscoveryService()
        service._mcp_connection_failed = True
        service._mcp_client = Mock()
        
        service.reset_mcp_connection()
        
        assert service._mcp_connection_failed is False
        assert service._mcp_client is None

    def test_get_cached_tool_inventory(self):
        """Test getting cached tool inventory."""
        service = ToolDiscoveryService()
        
        # No cache initially
        assert service.get_cached_tool_inventory() is None
        
        # Set cache
        service._cached_tool_inventory = {"tool1", "tool2"}
        cached = service.get_cached_tool_inventory()
        
        assert cached == {"tool1", "tool2"}
        # Should return a copy, not the original
        assert cached is not service._cached_tool_inventory


class TestGlobalFunctions:
    """Test cases for global discovery service functions."""

    def test_get_tool_discovery_service_singleton(self):
        """Test that get_tool_discovery_service returns singleton instance."""
        # Clear any existing global service
        import src.agent.tools.discovery
        src.agent.tools.discovery._tool_discovery_service = None
        
        service1 = get_tool_discovery_service()
        service2 = get_tool_discovery_service()
        
        assert service1 is service2
        assert isinstance(service1, ToolDiscoveryService)