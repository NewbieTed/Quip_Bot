"""
Unit tests for tool synchronization data models.
"""

import pytest
from datetime import datetime, timezone
from pydantic import ValidationError
import uuid

from .tool_sync import (
    ToolInfo,
    ToolResyncRequest,
    ToolInventoryResponse,
    create_resync_response,
    create_resync_request
)


class TestToolResyncRequest:
    """Test cases for ToolResyncRequest model."""

    def test_valid_tool_resync_request(self):
        """Test creating a valid ToolResyncRequest."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        reason = "message_processing_failure"
        
        # When
        request = ToolResyncRequest(
            request_id=request_id,
            timestamp=timestamp,
            reason=reason
        )
        
        # Then
        assert request.request_id == request_id
        assert request.timestamp == timestamp
        assert request.reason == reason

    def test_invalid_request_id_format(self):
        """Test validation fails for invalid UUID format."""
        # Given
        invalid_request_id = "invalid-uuid"
        timestamp = datetime.now(timezone.utc)
        reason = "test_reason"
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolResyncRequest(
                request_id=invalid_request_id,
                timestamp=timestamp,
                reason=reason
            )
        
        assert "Request ID must be a valid UUID" in str(exc_info.value)

    def test_empty_reason(self):
        """Test validation fails for empty reason."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        reason = ""
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolResyncRequest(
                request_id=request_id,
                timestamp=timestamp,
                reason=reason
            )
        
        assert "Reason cannot be empty" in str(exc_info.value)

    def test_whitespace_only_reason(self):
        """Test validation fails for whitespace-only reason."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        reason = "   "
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolResyncRequest(
                request_id=request_id,
                timestamp=timestamp,
                reason=reason
            )
        
        assert "Reason cannot be empty" in str(exc_info.value)

    def test_reason_whitespace_trimming(self):
        """Test that reason whitespace is trimmed."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        reason = "  test_reason  "
        
        # When
        request = ToolResyncRequest(
            request_id=request_id,
            timestamp=timestamp,
            reason=reason
        )
        
        # Then
        assert request.reason == "test_reason"

    def test_json_serialization(self):
        """Test JSON serialization and deserialization."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.fromisoformat("2025-01-28T10:30:00")
        reason = "message_processing_failure"
        
        request = ToolResyncRequest(
            request_id=request_id,
            timestamp=timestamp,
            reason=reason
        )
        
        # When
        json_data = request.json()
        deserialized_request = ToolResyncRequest.parse_raw(json_data)
        
        # Then
        assert deserialized_request.request_id == request.request_id
        assert deserialized_request.timestamp == request.timestamp
        assert deserialized_request.reason == request.reason


class TestToolInfo:
    """Test cases for ToolInfo model."""

    def test_valid_tool_info(self):
        """Test creating a valid ToolInfo."""
        # Given
        name = "test-tool"
        mcp_server_name = "built-in"
        
        # When
        tool_info = ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        # Then
        assert tool_info.name == name
        assert tool_info.mcp_server_name == mcp_server_name

    def test_valid_tool_info_with_mcp_server(self):
        """Test creating a valid ToolInfo with MCP server."""
        # Given
        name = "geo-data-weather"
        mcp_server_name = "geo-data"
        
        # When
        tool_info = ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        # Then
        assert tool_info.name == name
        assert tool_info.mcp_server_name == mcp_server_name

    def test_empty_tool_name(self):
        """Test validation fails for empty tool name."""
        # Given
        name = ""
        mcp_server_name = "built-in"
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        assert "Tool name cannot be empty" in str(exc_info.value)

    def test_whitespace_only_tool_name(self):
        """Test validation fails for whitespace-only tool name."""
        # Given
        name = "   "
        mcp_server_name = "built-in"
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        assert "Tool name cannot be empty" in str(exc_info.value)

    def test_invalid_tool_name_characters(self):
        """Test validation fails for invalid tool name characters."""
        # Given
        name = "invalid tool!"
        mcp_server_name = "built-in"
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        assert "Tool names must contain only alphanumeric characters, hyphens, and underscores" in str(exc_info.value)

    def test_valid_tool_name_characters(self):
        """Test validation passes for valid tool name characters."""
        # Given
        valid_names = ["tool1", "tool_2", "tool-3", "Tool4", "TOOL5", "tool123", "a", "A"]
        mcp_server_name = "built-in"
        
        # When/Then
        for name in valid_names:
            tool_info = ToolInfo(name=name, mcp_server_name=mcp_server_name)
            assert tool_info.name == name

    def test_tool_name_whitespace_trimming(self):
        """Test that tool name whitespace is trimmed."""
        # Given
        name = "  test-tool  "
        mcp_server_name = "built-in"
        
        # When
        tool_info = ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        # Then
        assert tool_info.name == "test-tool"

    def test_empty_mcp_server_name(self):
        """Test validation fails for empty MCP server name."""
        # Given
        name = "test-tool"
        mcp_server_name = ""
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        assert "MCP server name cannot be empty" in str(exc_info.value)

    def test_whitespace_only_mcp_server_name(self):
        """Test validation fails for whitespace-only MCP server name."""
        # Given
        name = "test-tool"
        mcp_server_name = "   "
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        assert "MCP server name cannot be empty" in str(exc_info.value)

    def test_mcp_server_name_whitespace_trimming(self):
        """Test that MCP server name whitespace is trimmed."""
        # Given
        name = "test-tool"
        mcp_server_name = "  built-in  "
        
        # When
        tool_info = ToolInfo(name=name, mcp_server_name=mcp_server_name)
        
        # Then
        assert tool_info.mcp_server_name == "built-in"


class TestToolInventoryResponse:
    """Test cases for ToolInventoryResponse model."""

    def test_valid_tool_inventory_response(self):
        """Test creating a valid ToolInventoryResponse."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        current_tools = [
            ToolInfo(name="tool1", mcp_server_name="built-in"),
            ToolInfo(name="geo-data-weather", mcp_server_name="geo-data"),
            ToolInfo(name="tool3", mcp_server_name="built-in")
        ]
        discovery_timestamp = datetime.now(timezone.utc)
        
        # When
        response = ToolInventoryResponse(
            request_id=request_id,
            timestamp=timestamp,
            current_tools=current_tools,
            discovery_timestamp=discovery_timestamp
        )
        
        # Then
        assert response.request_id == request_id
        assert response.timestamp == timestamp
        assert response.current_tools == current_tools
        assert response.discovery_timestamp == discovery_timestamp

    def test_invalid_request_id_format(self):
        """Test validation fails for invalid UUID format."""
        # Given
        invalid_request_id = "invalid-uuid"
        timestamp = datetime.now(timezone.utc)
        current_tools = [
            ToolInfo(name="tool1", mcp_server_name="built-in"),
            ToolInfo(name="tool2", mcp_server_name="built-in")
        ]
        discovery_timestamp = datetime.now(timezone.utc)
        
        # When/Then
        with pytest.raises(ValidationError) as exc_info:
            ToolInventoryResponse(
                request_id=invalid_request_id,
                timestamp=timestamp,
                current_tools=current_tools,
                discovery_timestamp=discovery_timestamp
            )
        
        assert "Request ID must be a valid UUID" in str(exc_info.value)

    def test_invalid_tool_info_in_list(self):
        """Test validation fails for invalid ToolInfo objects in list."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        discovery_timestamp = datetime.now(timezone.utc)
        
        # When/Then - invalid tool name should fail validation
        with pytest.raises(ValidationError):
            ToolInventoryResponse(
                request_id=request_id,
                timestamp=timestamp,
                current_tools=[
                    ToolInfo(name="valid-tool", mcp_server_name="built-in"),
                    ToolInfo(name="invalid tool!", mcp_server_name="built-in")  # Invalid name
                ],
                discovery_timestamp=discovery_timestamp
            )

    def test_valid_tool_info_objects(self):
        """Test validation passes for valid ToolInfo objects."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        current_tools = [
            ToolInfo(name="tool1", mcp_server_name="built-in"),
            ToolInfo(name="tool_2", mcp_server_name="built-in"),
            ToolInfo(name="tool-3", mcp_server_name="geo-data"),
            ToolInfo(name="Tool4", mcp_server_name="another-server"),
            ToolInfo(name="TOOL5", mcp_server_name="built-in")
        ]
        discovery_timestamp = datetime.now(timezone.utc)
        
        # When
        response = ToolInventoryResponse(
            request_id=request_id,
            timestamp=timestamp,
            current_tools=current_tools,
            discovery_timestamp=discovery_timestamp
        )
        
        # Then
        assert response.current_tools == current_tools

    def test_empty_tools_list(self):
        """Test that empty tools list is valid."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        current_tools = []
        discovery_timestamp = datetime.now(timezone.utc)
        
        # When
        response = ToolInventoryResponse(
            request_id=request_id,
            timestamp=timestamp,
            current_tools=current_tools,
            discovery_timestamp=discovery_timestamp
        )
        
        # Then
        assert response.current_tools == []

    def test_mixed_server_types(self):
        """Test that tools from different server types are handled correctly."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        current_tools = [
            ToolInfo(name="builtin-tool1", mcp_server_name="built-in"),
            ToolInfo(name="builtin-tool2", mcp_server_name="built-in"),
            ToolInfo(name="geo-data-weather", mcp_server_name="geo-data"),
            ToolInfo(name="geo-data-location", mcp_server_name="geo-data"),
            ToolInfo(name="aws-docs-search", mcp_server_name="aws-docs")
        ]
        discovery_timestamp = datetime.now(timezone.utc)
        
        # When
        response = ToolInventoryResponse(
            request_id=request_id,
            timestamp=timestamp,
            current_tools=current_tools,
            discovery_timestamp=discovery_timestamp
        )
        
        # Then
        assert len(response.current_tools) == 5
        built_in_tools = [t for t in response.current_tools if t.mcp_server_name == "built-in"]
        geo_data_tools = [t for t in response.current_tools if t.mcp_server_name == "geo-data"]
        aws_docs_tools = [t for t in response.current_tools if t.mcp_server_name == "aws-docs"]
        
        assert len(built_in_tools) == 2
        assert len(geo_data_tools) == 2
        assert len(aws_docs_tools) == 1

    def test_json_serialization(self):
        """Test JSON serialization and deserialization."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.fromisoformat("2025-01-28T10:30:00")
        current_tools = [
            ToolInfo(name="tool1", mcp_server_name="built-in"),
            ToolInfo(name="geo-data-weather", mcp_server_name="geo-data"),
            ToolInfo(name="tool3", mcp_server_name="built-in")
        ]
        discovery_timestamp = datetime.fromisoformat("2025-01-28T10:29:55")
        
        response = ToolInventoryResponse(
            request_id=request_id,
            timestamp=timestamp,
            current_tools=current_tools,
            discovery_timestamp=discovery_timestamp
        )
        
        # When
        json_data = response.json()
        deserialized_response = ToolInventoryResponse.parse_raw(json_data)
        
        # Then
        assert deserialized_response.request_id == response.request_id
        assert deserialized_response.timestamp == response.timestamp
        assert len(deserialized_response.current_tools) == len(response.current_tools)
        for i, tool in enumerate(deserialized_response.current_tools):
            assert tool.name == response.current_tools[i].name
            assert tool.mcp_server_name == response.current_tools[i].mcp_server_name
        assert deserialized_response.discovery_timestamp == response.discovery_timestamp


class TestHelperFunctions:
    """Test cases for helper functions."""

    def test_create_resync_response(self):
        """Test create_resync_response helper function."""
        # Given
        request_id = "550e8400-e29b-41d4-a716-446655440000"
        timestamp = datetime.now(timezone.utc)
        reason = "test_reason"
        
        request = ToolResyncRequest(
            request_id=request_id,
            timestamp=timestamp,
            reason=reason
        )
        
        tools = [
            ToolInfo(name="tool1", mcp_server_name="built-in"),
            ToolInfo(name="geo-data-weather", mcp_server_name="geo-data"),
            ToolInfo(name="tool3", mcp_server_name="built-in")
        ]
        
        # When
        response = create_resync_response(request, tools)
        
        # Then
        assert response.request_id == request.request_id
        assert response.current_tools == tools
        assert isinstance(response.timestamp, datetime)
        assert isinstance(response.discovery_timestamp, datetime)
        
        # Verify tool info is preserved
        assert len(response.current_tools) == 3
        assert response.current_tools[0].name == "tool1"
        assert response.current_tools[0].mcp_server_name == "built-in"
        assert response.current_tools[1].name == "geo-data-weather"
        assert response.current_tools[1].mcp_server_name == "geo-data"

    def test_create_resync_request(self):
        """Test create_resync_request helper function."""
        # Given
        reason = "message_processing_failure"
        
        # When
        request = create_resync_request(reason)
        
        # Then
        assert request.reason == reason
        assert isinstance(request.timestamp, datetime)
        # Validate that request_id is a valid UUID
        uuid.UUID(request.request_id)  # This will raise ValueError if invalid

    def test_create_resync_request_generates_unique_ids(self):
        """Test that create_resync_request generates unique request IDs."""
        # Given
        reason = "test_reason"
        
        # When
        request1 = create_resync_request(reason)
        request2 = create_resync_request(reason)
        
        # Then
        assert request1.request_id != request2.request_id