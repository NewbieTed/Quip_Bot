"""
Unit tests for API routes.
"""

import pytest
import json
from unittest.mock import Mock, patch, AsyncMock
from fastapi import HTTPException
from fastapi.testclient import TestClient
from http import HTTPStatus

from src.agent.api.routes import (
    RequestValidationError,
    _parse_request_json,
    _extract_common_fields,
    _validate_required_fields,
    _validate_approval_logic,
    _create_event_stream,
    _validate_conversation_dto,
    _validate_conversations_list,
    _validate_tool_lists,
    router
)


class TestUtilityFunctions:
    """Test cases for utility functions."""

    @pytest.mark.asyncio
    async def test_parse_request_json_success(self):
        """Test successful JSON parsing."""
        mock_request = Mock()
        mock_request.json = AsyncMock(return_value={"key": "value"})
        
        result = await _parse_request_json(mock_request)
        
        assert result == {"key": "value"}

    @pytest.mark.asyncio
    async def test_parse_request_json_invalid(self):
        """Test JSON parsing with invalid JSON."""
        mock_request = Mock()
        mock_request.json = AsyncMock(side_effect=Exception("Invalid JSON"))
        
        with pytest.raises(HTTPException) as exc_info:
            await _parse_request_json(mock_request)
        
        assert exc_info.value.status_code == HTTPStatus.BAD_REQUEST
        assert "Invalid JSON payload" in str(exc_info.value.detail)

    def test_extract_common_fields(self):
        """Test extracting common fields from request data."""
        data = {
            "serverId": 123,
            "channelId": 456,
            "memberId": 789,
            "conversationId": 101112,
            "otherField": "ignored"
        }
        
        result = _extract_common_fields(data)
        
        expected = {
            "server_id": 123,
            "channel_id": 456,
            "member_id": 789,
            "conversation_id": 101112
        }
        assert result == expected

    def test_validate_required_fields_success(self):
        """Test successful required fields validation."""
        fields = {
            "server_id": 123,
            "channel_id": 456,
            "member_id": 789
        }
        required = ["server_id", "channel_id", "member_id"]
        
        # Should not raise any exception
        _validate_required_fields(fields, required)

    def test_validate_required_fields_missing(self):
        """Test required fields validation with missing fields."""
        fields = {
            "server_id": 123,
            "member_id": None
        }
        required = ["server_id", "channel_id", "member_id"]
        
        with pytest.raises(HTTPException) as exc_info:
            _validate_required_fields(fields, required)
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "channelId" in str(exc_info.value.detail)
        assert "memberId" in str(exc_info.value.detail)

    def test_validate_approval_logic_success(self):
        """Test successful approval logic validation."""
        # Should not raise any exception
        _validate_approval_logic(True, None)
        _validate_approval_logic(True, "")
        _validate_approval_logic(False, "New message")
        _validate_approval_logic(None, "New message")

    def test_validate_approval_logic_invalid(self):
        """Test approval logic validation with invalid combination."""
        with pytest.raises(HTTPException) as exc_info:
            _validate_approval_logic(True, "New message")
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "Cannot approve with a new message" in str(exc_info.value.detail)

    @pytest.mark.asyncio
    async def test_create_event_stream(self):
        """Test event stream creation."""
        async def mock_generator():
            yield "chunk1"
            yield "chunk2"
            yield None  # Empty chunk
        
        results = []
        async for chunk in _create_event_stream(mock_generator()):
            results.append(chunk)
        
        assert results == ["chunk1", "chunk2", "\n"]

    def test_validate_conversation_dto_success(self):
        """Test successful conversation DTO validation."""
        conversation = {
            "conversationId": 123,
            "serverId": 456,
            "memberId": 789
        }
        
        # Should not raise any exception
        _validate_conversation_dto(conversation, 0)

    def test_validate_conversation_dto_missing_fields(self):
        """Test conversation DTO validation with missing fields."""
        conversation = {
            "conversationId": 123,
            "serverId": 456
            # Missing memberId
        }
        
        with pytest.raises(HTTPException) as exc_info:
            _validate_conversation_dto(conversation, 0)
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "missing required fields" in str(exc_info.value.detail)
        assert "memberId" in str(exc_info.value.detail)

    def test_validate_conversation_dto_invalid_types(self):
        """Test conversation DTO validation with invalid field types."""
        conversation = {
            "conversationId": "not_an_int",
            "serverId": 456,
            "memberId": 789
        }
        
        with pytest.raises(HTTPException) as exc_info:
            _validate_conversation_dto(conversation, 0)
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "must be a positive integer" in str(exc_info.value.detail)

    def test_validate_conversation_dto_negative_values(self):
        """Test conversation DTO validation with negative values."""
        conversation = {
            "conversationId": -1,
            "serverId": 456,
            "memberId": 789
        }
        
        with pytest.raises(HTTPException) as exc_info:
            _validate_conversation_dto(conversation, 0)
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "must be a positive integer" in str(exc_info.value.detail)

    def test_validate_conversations_list_success(self):
        """Test successful conversations list validation."""
        conversations = [
            {"conversationId": 1, "serverId": 123, "memberId": 789},
            {"conversationId": 2, "serverId": 456, "memberId": 789}
        ]
        
        # Should not raise any exception
        _validate_conversations_list(conversations)

    def test_validate_conversations_list_not_list(self):
        """Test conversations list validation when not a list."""
        with pytest.raises(HTTPException) as exc_info:
            _validate_conversations_list("not_a_list")
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "must be a list" in str(exc_info.value.detail)

    def test_validate_conversations_list_empty(self):
        """Test conversations list validation when empty."""
        with pytest.raises(HTTPException) as exc_info:
            _validate_conversations_list([])
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "cannot be empty" in str(exc_info.value.detail)

    def test_validate_conversations_list_different_members(self):
        """Test conversations list validation with different member IDs."""
        conversations = [
            {"conversationId": 1, "serverId": 123, "memberId": 789},
            {"conversationId": 2, "serverId": 456, "memberId": 999}  # Different member
        ]
        
        with pytest.raises(HTTPException) as exc_info:
            _validate_conversations_list(conversations)
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "same member" in str(exc_info.value.detail)

    def test_validate_tool_lists_success(self):
        """Test successful tool lists validation."""
        # Should not raise any exception
        _validate_tool_lists(["tool1", "tool2"], ["tool3"])
        _validate_tool_lists(["tool1"], [])
        _validate_tool_lists([], ["tool1"])

    def test_validate_tool_lists_not_list(self):
        """Test tool lists validation when not lists."""
        with pytest.raises(HTTPException) as exc_info:
            _validate_tool_lists("not_a_list", [])
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "addedTools must be a list" in str(exc_info.value.detail)

    def test_validate_tool_lists_invalid_tool_names(self):
        """Test tool lists validation with invalid tool names."""
        with pytest.raises(HTTPException) as exc_info:
            _validate_tool_lists(["", "tool2"], [])
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "non-empty strings" in str(exc_info.value.detail)

    def test_validate_tool_lists_no_changes(self):
        """Test tool lists validation with no changes."""
        with pytest.raises(HTTPException) as exc_info:
            _validate_tool_lists([], [])
        
        assert exc_info.value.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "At least one tool must be added or removed" in str(exc_info.value.detail)


class TestHealthEndpoints:
    """Test cases for health check endpoints."""

    def test_health_check(self):
        """Test basic health check endpoint."""
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.get("/health")
        
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "OK"
        assert "fine" in data["message"]

    @pytest.mark.asyncio
    async def test_detailed_health_check_success(self):
        """Test detailed health check endpoint success."""
        mock_health_status = {
            "status": "healthy",
            "components": {},
            "timestamp": "2025-01-28T10:30:00Z"
        }
        
        with patch('src.agent.monitoring.health_check.get_system_status', return_value=mock_health_status):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.get("/health/detailed")
            
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "healthy"
            assert "components" in data

    @pytest.mark.asyncio
    async def test_detailed_health_check_error(self):
        """Test detailed health check endpoint with error."""
        with patch('src.agent.monitoring.health_check.get_system_status', side_effect=Exception("Health check failed")):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.get("/health/detailed")
            
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ERROR"
            assert "Health check failed" in data["error"]

    def test_sync_recovery_health_success(self):
        """Test sync recovery health check endpoint success."""
        mock_metrics = {
            "system_metrics": {
                "resync_requests_received": 5,
                "resync_requests_successful": 4,
                "resync_requests_failed": 1,
                "resync_success_rate": 0.8,
                "average_resync_latency_ms": 150.0,
                "last_resync_request": "2025-01-28T10:30:00Z"
            }
        }
        
        mock_health_status = {
            "status": "healthy",
            "issues": []
        }
        
        with patch('src.agent.monitoring.metrics_service.get_metrics_service') as mock_service:
            mock_service.return_value.get_metrics_summary.return_value = mock_metrics
            mock_service.return_value.get_health_status.return_value = mock_health_status
            
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.get("/health/sync-recovery")
            
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "healthy"
            assert data["sync_recovery_metrics"]["resync_requests_received"] == 5
            assert data["sync_recovery_metrics"]["resync_success_rate"] == 0.8

    def test_sync_recovery_health_error(self):
        """Test sync recovery health check endpoint with error."""
        with patch('src.agent.monitoring.metrics_service.get_metrics_service', side_effect=Exception("Metrics error")):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.get("/health/sync-recovery")
            
            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ERROR"
            assert "Metrics error" in data["error"]


class TestAgentEndpoints:
    """Test cases for agent endpoints."""

    @pytest.mark.asyncio
    async def test_invoke_agent_success(self):
        """Test successful agent invocation."""
        request_data = {
            "serverId": 123,
            "channelId": 456,
            "memberId": 789,
            "conversationId": 101112,
            "message": "Test message",
            "approved": None,
            "toolWhitelistUpdate": []
        }
        
        async def mock_agent_generator():
            yield "response1"
            yield "response2"
        
        with patch('src.agent.api.routes.run_agent', return_value=mock_agent_generator()):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.post("/assistant", json=request_data)
            
            assert response.status_code == 200
            assert response.headers["content-type"] == "text/plain; charset=utf-8"

    @pytest.mark.asyncio
    async def test_invoke_agent_missing_fields(self):
        """Test agent invocation with missing required fields."""
        request_data = {
            "serverId": 123,
            # Missing other required fields
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/assistant", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "Missing or empty required fields" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_invoke_agent_invalid_approval(self):
        """Test agent invocation with invalid approval logic."""
        request_data = {
            "serverId": 123,
            "channelId": 456,
            "memberId": 789,
            "conversationId": 101112,
            "message": "New message",
            "approved": True  # Invalid: can't approve with new message
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/assistant", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "Cannot approve with a new message" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_invoke_new_agent_success(self):
        """Test successful new agent invocation."""
        request_data = {
            "serverId": 123,
            "channelId": 456,
            "memberId": 789,
            "conversationId": 101112,
            "message": "Test message",
            "toolWhitelist": ["tool1", "tool2"]
        }
        
        async def mock_agent_generator():
            yield "response1"
        
        with patch('src.agent.api.routes.run_new_agent', return_value=mock_agent_generator()):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.post("/assistant/new", json=request_data)
            
            assert response.status_code == 200

    @pytest.mark.asyncio
    async def test_invoke_new_agent_missing_message(self):
        """Test new agent invocation with missing message."""
        request_data = {
            "serverId": 123,
            "channelId": 456,
            "memberId": 789,
            "conversationId": 101112
            # Missing message
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/assistant/new", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "message" in response.json()["detail"]


class TestToolWhitelistEndpoint:
    """Test cases for tool whitelist update endpoint."""

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_success(self):
        """Test successful tool whitelist update."""
        request_data = {
            "memberId": 789,
            "conversations": [
                {"conversationId": 1, "serverId": 123, "memberId": 789},
                {"conversationId": 2, "serverId": 456, "memberId": 789}
            ],
            "addedTools": ["tool1", "tool2"],
            "removedTools": ["tool3"]
        }
        
        mock_result = {
            "memberId": 789,
            "totalConversations": 2,
            "successfulUpdates": 2,
            "failedUpdates": 0,
            "addedTools": ["tool1", "tool2"],
            "removedTools": ["tool3"]
        }
        
        with patch('src.agent.agent_runner.update_tool_whitelist', return_value=mock_result):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.post("/tool-whitelist/update", json=request_data)
            
            assert response.status_code == 200
            data = response.json()
            assert data["status"] is True
            assert data["affectedConversations"] == 2
            assert data["addedTools"] == 2
            assert data["removedTools"] == 1

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_missing_member_id(self):
        """Test tool whitelist update with missing member ID."""
        request_data = {
            "conversations": [
                {"conversationId": 1, "serverId": 123, "memberId": 789}
            ],
            "addedTools": ["tool1"],
            "removedTools": []
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/tool-whitelist/update", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "memberId" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_invalid_conversations(self):
        """Test tool whitelist update with invalid conversations."""
        request_data = {
            "memberId": 789,
            "conversations": [],  # Empty list
            "addedTools": ["tool1"],
            "removedTools": []
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/tool-whitelist/update", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "cannot be empty" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_member_mismatch(self):
        """Test tool whitelist update with member ID mismatch."""
        request_data = {
            "memberId": 789,
            "conversations": [
                {"conversationId": 1, "serverId": 123, "memberId": 999}  # Different member
            ],
            "addedTools": ["tool1"],
            "removedTools": []
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/tool-whitelist/update", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "must match" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_no_changes(self):
        """Test tool whitelist update with no changes."""
        request_data = {
            "memberId": 789,
            "conversations": [
                {"conversationId": 1, "serverId": 123, "memberId": 789}
            ],
            "addedTools": [],
            "removedTools": []
        }
        
        from src.agent.main import app
        client = TestClient(app)
        
        response = client.post("/tool-whitelist/update", json=request_data)
        
        assert response.status_code == HTTPStatus.UNPROCESSABLE_ENTITY
        assert "At least one tool must be added or removed" in response.json()["detail"]

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_update_error(self):
        """Test tool whitelist update with update error."""
        request_data = {
            "memberId": 789,
            "conversations": [
                {"conversationId": 1, "serverId": 123, "memberId": 789}
            ],
            "addedTools": ["tool1"],
            "removedTools": []
        }
        
        with patch('src.agent.agent_runner.update_tool_whitelist', side_effect=Exception("Update failed")):
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.post("/tool-whitelist/update", json=request_data)
            
            assert response.status_code == HTTPStatus.INTERNAL_SERVER_ERROR
            assert "Failed to update tool whitelist" in response.json()["detail"]


class TestToolResyncEndpoint:
    """Test cases for tool resync endpoint."""

    @pytest.mark.asyncio
    async def test_handle_tool_resync_success(self):
        """Test successful tool resync handling."""
        from src.agent.models.tool_sync import ToolResyncRequest, ToolInventoryResponse, ToolInfo
        from datetime import datetime, timezone
        
        request_data = {
            "request_id": "550e8400-e29b-41d4-a716-446655440000",
            "timestamp": "2025-01-28T10:30:00Z",
            "reason": "test_reason"
        }
        
        mock_response = ToolInventoryResponse(
            request_id="550e8400-e29b-41d4-a716-446655440000",
            timestamp=datetime.now(timezone.utc),
            current_tools=[ToolInfo(name="tool1", mcp_server_name="built-in")],
            discovery_timestamp=datetime.now(timezone.utc)
        )
        
        with patch('src.agent.api.routes.get_tool_sync_controller') as mock_controller:
            mock_controller.return_value.handle_resync_request.return_value = mock_response
            
            from src.agent.main import app
            client = TestClient(app)
            
            response = client.post("/api/tools/resync", json=request_data)
            
            assert response.status_code == 200
            data = response.json()
            assert data["request_id"] == "550e8400-e29b-41d4-a716-446655440000"
            assert len(data["current_tools"]) == 1