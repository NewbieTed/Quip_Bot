"""
Unit tests for agent runner functionality.
"""

import pytest
import json
from unittest.mock import Mock, patch, AsyncMock, MagicMock
from langchain_core.messages import HumanMessage

from src.agent.agent_runner import (
    AgentRunnerError,
    load_system_prompt,
    _format_json_response,
    _validate_message,
    _log_agent_start,
    _log_agent_complete,
    _member_message_validation,
    _setup_agent,
    _process_stream,
    run_agent,
    run_new_agent,
    _build_graph_config,
    update_tool_whitelist
)


class TestAgentRunnerError:
    """Test cases for AgentRunnerError exception."""

    def test_init(self):
        """Test AgentRunnerError initialization."""
        error = AgentRunnerError("Test error message")
        
        assert str(error) == "Test error message"
        assert isinstance(error, Exception)


class TestUtilityFunctions:
    """Test cases for utility functions."""

    def test_load_system_prompt(self):
        """Test loading system prompt."""
        with patch('src.agent.agent_runner.load_prompt', return_value="Test system prompt") as mock_load:
            prompt = load_system_prompt()
            
            assert prompt == "Test system prompt"
            mock_load.assert_called_once_with("main_system")

    def test_format_json_response_with_newline(self):
        """Test JSON response formatting when content already has newline."""
        data = {"content": "Test message\n", "type": "update"}
        
        result = _format_json_response(data)
        
        parsed = json.loads(result)
        assert parsed["content"] == "Test message\n"
        assert parsed["type"] == "update"

    def test_format_json_response_without_newline(self):
        """Test JSON response formatting when content needs newline."""
        data = {"content": "Test message", "type": "update"}
        
        result = _format_json_response(data)
        
        parsed = json.loads(result)
        assert parsed["content"] == "Test message\n"
        assert parsed["type"] == "update"

    def test_format_json_response_no_content(self):
        """Test JSON response formatting when no content field."""
        data = {"type": "update", "other": "data"}
        
        result = _format_json_response(data)
        
        parsed = json.loads(result)
        assert parsed["type"] == "update"
        assert parsed["other"] == "data"

    def test_format_json_response_empty_content(self):
        """Test JSON response formatting when content is empty."""
        data = {"content": "", "type": "update"}
        
        result = _format_json_response(data)
        
        parsed = json.loads(result)
        assert parsed["content"] == ""

    def test_validate_message_valid(self):
        """Test message validation with valid message."""
        # Should not raise any exception
        _validate_message("Valid message")
        _validate_message("  Valid message with spaces  ")

    def test_validate_message_invalid_type(self):
        """Test message validation with invalid type."""
        with pytest.raises(AgentRunnerError) as exc_info:
            _validate_message(123)
        
        assert "non-empty string" in str(exc_info.value)

    def test_validate_message_empty_string(self):
        """Test message validation with empty string."""
        with pytest.raises(AgentRunnerError) as exc_info:
            _validate_message("")
        
        assert "non-empty string" in str(exc_info.value)

    def test_validate_message_whitespace_only(self):
        """Test message validation with whitespace-only string."""
        with pytest.raises(AgentRunnerError) as exc_info:
            _validate_message("   ")
        
        assert "non-empty string" in str(exc_info.value)

    def test_log_agent_start(self):
        """Test agent start logging."""
        # Should not raise any exceptions
        _log_agent_start(123, 456, 789)
        _log_agent_start(123, 456, 789, "Custom action")

    def test_log_agent_complete(self):
        """Test agent completion logging."""
        # Should not raise any exceptions
        _log_agent_complete(789)

    def test_member_message_validation_valid(self):
        """Test member message validation with valid message."""
        result = _member_message_validation("Valid message")
        
        assert result is None

    def test_member_message_validation_invalid(self):
        """Test member message validation with invalid message."""
        result = _member_message_validation("")
        
        assert result is not None
        parsed = json.loads(result)
        assert "Error:" in parsed["content"]

    def test_build_graph_config(self):
        """Test graph configuration building."""
        config = _build_graph_config(123, 456, 789)
        
        expected_thread_id = "123456789"
        assert config["configurable"]["thread_id"] == expected_thread_id
        assert config["configurable"]["member_id"] == 123
        assert config["configurable"]["server_id"] == 456
        assert config["configurable"]["conversation_id"] == 789


class TestSetupAgent:
    """Test cases for agent setup functionality."""

    @pytest.mark.asyncio
    async def test_setup_agent_success(self):
        """Test successful agent setup."""
        mock_graph = Mock()
        mock_tools = [Mock(name="tool1"), Mock(name="tool2")]
        
        with patch('src.agent.agent_runner.get_cached_graph', return_value=mock_graph):
            with patch('src.agent.agent_runner.get_all_tools', return_value=mock_tools):
                with patch('src.agent.agent_runner.load_system_prompt', return_value="System prompt"):
                    
                    graph, config, initial_state = await _setup_agent(
                        "Test message", 123, 456, 789, 101112, ["tool1"]
                    )
                    
                    assert graph == mock_graph
                    assert config["configurable"]["thread_id"] == "123456789"
                    assert initial_state["server_id"] == 123
                    assert initial_state["channel_id"] == 456
                    assert initial_state["member_id"] == 789
                    assert initial_state["conversation_id"] == 101112
                    assert initial_state["tool_whitelist"] == {"tool1"}
                    
                    # Check messages structure
                    messages = initial_state["messages"]
                    assert len(messages) == 2
                    assert messages[0]["role"] == "system"
                    assert messages[0]["content"] == "System prompt"
                    assert messages[1]["role"] == "user"
                    assert messages[1]["content"] == "Test message"


class TestProcessStream:
    """Test cases for stream processing functionality."""

    @pytest.mark.asyncio
    async def test_process_stream_custom_progress(self):
        """Test stream processing with custom progress messages."""
        mock_graph = Mock()
        
        # Mock async generator that yields custom messages
        async def mock_astream(state, config, stream_mode):
            yield ("custom", {"progress": "Processing step 1"})
            yield ("custom", {"progress": "Processing step 2"})
        
        mock_graph.astream = mock_astream
        
        results = []
        async for message in _process_stream(mock_graph, {}, {}):
            results.append(json.loads(message))
        
        assert len(results) == 2
        assert results[0]["content"] == "Processing step 1\n"
        assert results[0]["type"] == "progress"
        assert results[1]["content"] == "Processing step 2\n"
        assert results[1]["type"] == "progress"

    @pytest.mark.asyncio
    async def test_process_stream_interrupt_message(self):
        """Test stream processing with interrupt messages."""
        mock_graph = Mock()
        
        # Mock interrupt message
        interrupt_obj = Mock()
        interrupt_obj.value = {
            "request": {
                "content": "Do you approve this action?",
                "tool_name": "test_tool"
            }
        }
        
        async def mock_astream(state, config, stream_mode):
            yield ("updates", {"__interrupt__": [interrupt_obj]})
        
        mock_graph.astream = mock_astream
        
        results = []
        async for message in _process_stream(mock_graph, {}, {}):
            results.append(json.loads(message))
        
        assert len(results) == 1
        assert results[0]["content"] == "Do you approve this action?\n"
        assert results[0]["tool_name"] == "test_tool"
        assert results[0]["type"] == "interrupt"

    @pytest.mark.asyncio
    async def test_process_stream_agent_response(self):
        """Test stream processing with agent response."""
        mock_graph = Mock()
        
        # Mock agent message
        mock_message = Mock()
        mock_message.content = "Agent response"
        
        async def mock_astream(state, config, stream_mode):
            yield ("updates", {"agent": {"messages": [mock_message]}})
        
        mock_graph.astream = mock_astream
        
        results = []
        async for message in _process_stream(mock_graph, {}, {}):
            results.append(json.loads(message))
        
        assert len(results) == 1
        assert results[0]["content"] == "Agent response\n"
        assert results[0]["type"] == "update"

    @pytest.mark.asyncio
    async def test_process_stream_exception(self):
        """Test stream processing with exception."""
        mock_graph = Mock()
        
        async def mock_astream(state, config, stream_mode):
            raise Exception("Stream error")
        
        mock_graph.astream = mock_astream
        
        results = []
        async for message in _process_stream(mock_graph, {}, {}):
            results.append(json.loads(message))
        
        assert len(results) == 1
        assert "Error: Stream error" in results[0]["content"]

    @pytest.mark.asyncio
    async def test_process_stream_no_response(self):
        """Test stream processing when no response is generated."""
        mock_graph = Mock()
        
        async def mock_astream(state, config, stream_mode):
            # Yield nothing that generates content
            yield ("updates", {"other": "data"})
        
        mock_graph.astream = mock_astream
        
        results = []
        async for message in _process_stream(mock_graph, {}, {}):
            results.append(json.loads(message))
        
        assert len(results) == 1
        assert "No response generated" in results[0]["content"]


class TestRunAgent:
    """Test cases for run_agent functionality."""

    @pytest.mark.asyncio
    async def test_run_agent_with_approval(self):
        """Test running agent with approval flow."""
        mock_graph = Mock()
        mock_graph.get_state.return_value.values = {"messages": []}
        
        with patch('src.agent.agent_runner._setup_agent', return_value=(mock_graph, {}, {})):
            with patch('src.agent.agent_runner._process_stream') as mock_process:
                mock_process.return_value = AsyncMock()
                mock_process.return_value.__aiter__ = AsyncMock(return_value=iter(["response1", "response2"]))
                
                results = []
                async for message in run_agent(
                    "Test message", 123, 456, 789, 101112, True, ["tool1"]
                ):
                    results.append(message)
                
                assert len(results) == 4  # 2 for approval, 2 for message
                # Verify that _process_stream was called twice (approval + message)
                assert mock_process.call_count == 2

    @pytest.mark.asyncio
    async def test_run_agent_empty_message(self):
        """Test running agent with empty message."""
        mock_graph = Mock()
        
        with patch('src.agent.agent_runner._setup_agent', return_value=(mock_graph, {}, {})):
            results = []
            async for message in run_agent(
                "", 123, 456, 789, 101112, None, []
            ):
                results.append(message)
            
            # Should return immediately for empty message
            assert len(results) == 0

    @pytest.mark.asyncio
    async def test_run_agent_no_approval(self):
        """Test running agent without approval flow."""
        mock_graph = Mock()
        mock_graph.get_state.return_value.values = {"messages": []}
        
        with patch('src.agent.agent_runner._setup_agent', return_value=(mock_graph, {}, {})):
            with patch('src.agent.agent_runner._process_stream') as mock_process:
                mock_process.return_value = AsyncMock()
                mock_process.return_value.__aiter__ = AsyncMock(return_value=iter(["response1"]))
                
                results = []
                async for message in run_agent(
                    "Test message", 123, 456, 789, 101112, None, []
                ):
                    results.append(message)
                
                assert len(results) == 1
                # Verify that _process_stream was called once (message only)
                assert mock_process.call_count == 1


class TestRunNewAgent:
    """Test cases for run_new_agent functionality."""

    @pytest.mark.asyncio
    async def test_run_new_agent_success(self):
        """Test successful new agent run."""
        with patch('src.agent.agent_runner._setup_agent', return_value=(Mock(), {}, {})):
            with patch('src.agent.agent_runner._process_stream') as mock_process:
                mock_process.return_value = AsyncMock()
                mock_process.return_value.__aiter__ = AsyncMock(return_value=iter(["response1", "response2"]))
                
                results = []
                async for message in run_new_agent(
                    "Test message", 123, 456, 789, 101112, ["tool1"]
                ):
                    results.append(message)
                
                assert len(results) == 2
                mock_process.assert_called_once()

    @pytest.mark.asyncio
    async def test_run_new_agent_validation_error(self):
        """Test new agent run with validation error."""
        results = []
        async for message in run_new_agent(
            "", 123, 456, 789, 101112, []
        ):
            results.append(message)
        
        assert len(results) == 1
        parsed = json.loads(results[0])
        assert "Error:" in parsed["content"]


class TestUpdateToolWhitelist:
    """Test cases for update_tool_whitelist functionality."""

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_success(self):
        """Test successful tool whitelist update."""
        mock_graph = Mock()
        
        # Mock graph state
        mock_state = {"tool_whitelist": {"existing_tool"}}
        mock_graph.get_state.return_value.values = mock_state
        
        conversations = [
            {"conversationId": 1, "serverId": 123, "memberId": 789},
            {"conversationId": 2, "serverId": 123, "memberId": 789}
        ]
        
        with patch('src.agent.agent_runner.get_cached_graph', return_value=mock_graph):
            result = await update_tool_whitelist(
                789, conversations, ["new_tool"], ["existing_tool"]
            )
            
            assert result["memberId"] == 789
            assert result["totalConversations"] == 2
            assert result["successfulUpdates"] == 2
            assert result["failedUpdates"] == 0
            assert result["addedTools"] == ["new_tool"]
            assert result["removedTools"] == ["existing_tool"]
            
            # Verify graph.update_state was called for each conversation
            assert mock_graph.update_state.call_count == 2

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_no_state(self):
        """Test tool whitelist update when conversation has no state."""
        mock_graph = Mock()
        mock_graph.get_state.return_value.values = None
        
        conversations = [
            {"conversationId": 1, "serverId": 123, "memberId": 789}
        ]
        
        with patch('src.agent.agent_runner.get_cached_graph', return_value=mock_graph):
            result = await update_tool_whitelist(
                789, conversations, ["new_tool"], []
            )
            
            assert result["successfulUpdates"] == 0
            assert result["failedUpdates"] == 1
            assert len(result["failedConversations"]) == 1
            assert result["failedConversations"][0]["error"] == "No state found"

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_update_error(self):
        """Test tool whitelist update when graph update fails."""
        mock_graph = Mock()
        mock_graph.get_state.return_value.values = {"tool_whitelist": set()}
        mock_graph.update_state.side_effect = Exception("Update failed")
        
        conversations = [
            {"conversationId": 1, "serverId": 123, "memberId": 789}
        ]
        
        with patch('src.agent.agent_runner.get_cached_graph', return_value=mock_graph):
            result = await update_tool_whitelist(
                789, conversations, ["new_tool"], []
            )
            
            assert result["successfulUpdates"] == 0
            assert result["failedUpdates"] == 1
            assert "Update failed" in result["failedConversations"][0]["error"]

    @pytest.mark.asyncio
    async def test_update_tool_whitelist_mixed_results(self):
        """Test tool whitelist update with mixed success/failure results."""
        mock_graph = Mock()
        
        # First conversation succeeds, second fails
        mock_graph.get_state.side_effect = [
            Mock(values={"tool_whitelist": set()}),  # Success
            Mock(values=None)  # Failure - no state
        ]
        
        conversations = [
            {"conversationId": 1, "serverId": 123, "memberId": 789},
            {"conversationId": 2, "serverId": 123, "memberId": 789}
        ]
        
        with patch('src.agent.agent_runner.get_cached_graph', return_value=mock_graph):
            result = await update_tool_whitelist(
                789, conversations, ["new_tool"], []
            )
            
            assert result["successfulUpdates"] == 1
            assert result["failedUpdates"] == 1
            assert len(result["updatedConversations"]) == 1
            assert len(result["failedConversations"]) == 1