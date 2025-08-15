"""
Pytest configuration and fixtures for agent tests.
"""

import pytest
import asyncio
import os
import tempfile
from unittest.mock import Mock, MagicMock, patch
from pathlib import Path
from datetime import datetime, timezone

# Test fixtures for common mocks and test data


@pytest.fixture
def mock_config():
    """Mock configuration for tests."""
    config = {
        "openai": {
            "model": "gpt-4o-mini",
            "temperature": 0,
            "max_tokens": 4000
        },
        "redis": {
            "host": "localhost",
            "port": 6379,
            "db": 0,
            "password": None,
            "retry": {
                "max_attempts": 3,
                "base_delay": 1.0,
                "max_delay": 30.0
            }
        },
        "mcp": {
            "enabled": True,
            "servers": {
                "test-server": {
                    "url": "http://localhost:8001/mcp",
                    "transport": "streamable_http"
                }
            }
        },
        "tool_sync": {
            "enabled": True,
            "discovery_on_startup": True,
            "http_server": {
                "enabled": True,
                "host": "0.0.0.0",
                "port": 5001,
                "timeout": 10,
                "discovery": {
                    "timeout": 10,
                    "retry_attempts": 2,
                    "retry_delay": 1
                }
            }
        }
    }
    return config


@pytest.fixture
def mock_redis_client():
    """Mock Redis client for tests."""
    client = Mock()
    client.ping.return_value = True
    client.lpush.return_value = 1
    client.rpush.return_value = 1
    client.llen.return_value = 0
    client.is_connected.return_value = True
    client.connect.return_value = True
    return client


@pytest.fixture
def mock_openai_api_key():
    """Mock OpenAI API key for tests."""
    with patch.dict(os.environ, {"OPENAI_API_KEY": "test-api-key"}):
        yield "test-api-key"


@pytest.fixture
def mock_tool():
    """Mock BaseTool for tests."""
    tool = Mock()
    tool.name = "test-tool"
    tool.description = "A test tool"
    tool._mcp_server_name = "built-in"
    return tool


@pytest.fixture
def mock_mcp_tool():
    """Mock MCP tool for tests."""
    tool = Mock()
    tool.name = "test-server-weather"
    tool.description = "Weather tool from test server"
    tool._mcp_server_name = "test-server"
    return tool


@pytest.fixture
def sample_tool_info():
    """Sample tool info for tests."""
    from src.agent.models.tool_sync import ToolInfo
    return ToolInfo(name="test-tool", mcp_server_name="built-in")


@pytest.fixture
def sample_resync_request():
    """Sample resync request for tests."""
    from src.agent.models.tool_sync import ToolResyncRequest
    return ToolResyncRequest(
        request_id="550e8400-e29b-41d4-a716-446655440000",
        timestamp=datetime.now(timezone.utc),
        reason="test_reason"
    )


@pytest.fixture
def temp_prompts_dir():
    """Temporary directory with test prompt files."""
    with tempfile.TemporaryDirectory() as temp_dir:
        prompts_dir = Path(temp_dir) / "prompts"
        prompts_dir.mkdir()
        
        # Create test prompt files
        (prompts_dir / "main_system_prompt.txt").write_text("Test system prompt")
        (prompts_dir / "human_confirmation_prompt.txt").write_text("Test confirmation prompt")
        (prompts_dir / "progress_report_prompt.txt").write_text("Test progress prompt")
        
        yield prompts_dir


@pytest.fixture
def mock_agent_state():
    """Mock agent state for tests."""
    return {
        "messages": [],
        "server_id": 123,
        "channel_id": 456,
        "member_id": 789,
        "conversation_id": 101112,
        "tool_call_ids": [],
        "tool_whitelist": set()
    }


@pytest.fixture(scope="session")
def event_loop():
    """Create an instance of the default event loop for the test session."""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def mock_datetime():
    """Mock datetime for consistent testing."""
    fixed_time = datetime(2025, 1, 28, 10, 30, 0, tzinfo=timezone.utc)
    with patch('src.agent.models.tool_sync.datetime') as mock_dt:
        mock_dt.now.return_value = fixed_time
        mock_dt.fromisoformat = datetime.fromisoformat
        yield fixed_time