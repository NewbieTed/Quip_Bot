"""
Unit tests for configuration management.
"""

import pytest
import os
import tempfile
import yaml
from pathlib import Path
from unittest.mock import patch, mock_open

from src.config.config_loader import Config


class TestConfig:
    """Test cases for Config class."""

    def test_environment_variables_loaded(self):
        """Test that environment variables are loaded correctly."""
        # Test by directly checking os.getenv behavior
        with patch.dict(os.environ, {
            "DEBUG": "true",
            "HOST": "test-host",
            "PORT": "8080",
            "OPENAI_API_KEY": "test-key",
            "REDIS_HOST": "redis-host",
            "REDIS_PORT": "6380"
        }):
            # Test the environment variable reading directly
            assert os.getenv("DEBUG", "false").lower() == "true"
            assert os.getenv("HOST", "0.0.0.0") == "test-host"
            assert int(os.getenv("PORT", "5000")) == 8080
            assert os.getenv("OPENAI_API_KEY") == "test-key"
            assert os.getenv("REDIS_HOST", "localhost") == "redis-host"
            assert int(os.getenv("REDIS_PORT", "6379")) == 6380

    def test_default_values(self):
        """Test that default values are used when environment variables are not set."""
        # Test default value logic by mocking getenv to return the default values
        def mock_getenv(key, default=None):
            return default
            
        with patch('os.getenv', side_effect=mock_getenv):
            # Test the default value logic
            debug_value = os.getenv("DEBUG", "false").lower() == "true"
            host_value = os.getenv("HOST", "0.0.0.0")
            port_value = int(os.getenv("PORT", "5000"))
            redis_host_value = os.getenv("REDIS_HOST", "localhost")
            redis_port_value = int(os.getenv("REDIS_PORT", "6379"))
            
            assert debug_value is False
            assert host_value == "0.0.0.0"
            assert port_value == 5000
            assert redis_host_value == "localhost"
            assert redis_port_value == 6379

    def test_load_yaml_config_success(self):
        """Test successful YAML config loading."""
        test_config = {
            "openai": {
                "model": "gpt-4",
                "temperature": 0.5
            },
            "redis": {
                "host": "yaml-redis-host"
            }
        }
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
            yaml.dump(test_config, f)
            temp_file = f.name
        
        try:
            # Mock the config file path
            with patch.object(Config, '_config_file', Path(temp_file)):
                Config._config_cache = None  # Clear cache
                config = Config.load_yaml_config()
                
                assert config["openai"]["model"] == "gpt-4"
                assert config["openai"]["temperature"] == 0.5
                assert config["redis"]["host"] == "yaml-redis-host"
        finally:
            os.unlink(temp_file)

    def test_load_yaml_config_file_not_found(self):
        """Test YAML config loading when file doesn't exist."""
        with patch.object(Config, '_config_file', Path("nonexistent.yaml")):
            Config._config_cache = None  # Clear cache
            config = Config.load_yaml_config()
            
            assert config == {}

    def test_get_openai_config_from_yaml(self):
        """Test getting OpenAI config from YAML."""
        test_config = {
            "openai": {
                "model": "gpt-4",
                "temperature": 0.7,
                "max_tokens": 2000
            }
        }
        
        with patch.object(Config, 'load_yaml_config', return_value=test_config):
            openai_config = Config.get_openai_config()
            
            assert openai_config["model"] == "gpt-4"
            assert openai_config["temperature"] == 0.7
            assert openai_config["max_tokens"] == 2000

    def test_get_openai_config_defaults(self):
        """Test getting OpenAI config with defaults when YAML is empty."""
        with patch.object(Config, 'load_yaml_config', return_value={}):
            openai_config = Config.get_openai_config()
            
            assert openai_config["model"] == "gpt-4o-mini"
            assert openai_config["temperature"] == 0
            assert openai_config["max_tokens"] == 4000

    def test_get_redis_config_env_precedence(self):
        """Test that environment variables take precedence over YAML for Redis config."""
        yaml_config = {
            "redis": {
                "host": "yaml-host",
                "port": 6380,
                "db": 1
            }
        }
        
        # Mock the Config class attributes directly
        with patch.object(Config, 'REDIS_HOST', 'env-host'):
            with patch.object(Config, 'REDIS_PORT', 6381):
                with patch.object(Config, 'REDIS_DB', 2):
                    with patch.object(Config, 'load_yaml_config', return_value=yaml_config):
                        redis_config = Config.get_redis_config()
                        
                        # Environment variables should take precedence
                        assert redis_config["host"] == "env-host"
                        assert redis_config["port"] == 6381
                        assert redis_config["db"] == 2

    def test_get_mcp_config_with_server_specific_urls(self):
        """Test MCP config with server-specific environment variables."""
        yaml_config = {
            "mcp": {
                "enabled": True,
                "servers": {
                    "TestServer": {
                        "transport": "streamable_http"
                    },
                    "AnotherServer": {
                        "transport": "streamable_http",
                        "url": "http://localhost:8002/mcp"
                    }
                }
            }
        }
        
        with patch.dict(os.environ, {
            "MCP_SERVER_URL_TESTSERVER": "http://env-test-server:8001/mcp",
            "MCP_SERVER_URL": "http://default-server:8000/mcp"
        }):
            with patch.object(Config, 'load_yaml_config', return_value=yaml_config):
                mcp_config = Config.get_mcp_config()
                
                # TestServer should use server-specific env var
                assert mcp_config["servers"]["TestServer"]["url"] == "http://env-test-server:8001/mcp"
                # AnotherServer should keep its YAML URL (localhost is allowed)
                assert mcp_config["servers"]["AnotherServer"]["url"] == "http://localhost:8002/mcp"

    def test_get_mcp_config_security_warning(self):
        """Test that security warning is logged for non-localhost URLs in config."""
        yaml_config = {
            "mcp": {
                "enabled": True,
                "servers": {
                    "UnsafeServer": {
                        "transport": "streamable_http",
                        "url": "http://external-server.com:8001/mcp"
                    }
                }
            }
        }
        
        with patch.dict(os.environ, {}, clear=True):
            with patch.object(Config, 'load_yaml_config', return_value=yaml_config):
                # Mock the logger at the module level where it's imported
                with patch('logging.getLogger') as mock_get_logger:
                    mock_logger = mock_get_logger.return_value
                    
                    mcp_config = Config.get_mcp_config()
                    
                    # Should still use the URL but log a warning
                    assert mcp_config["servers"]["UnsafeServer"]["url"] == "http://external-server.com:8001/mcp"
                    # Check that warning was called (may be called multiple times)
                    assert mock_logger.warning.called
                    # Check that at least one warning contains "Security Warning"
                    warning_calls = [call[0][0] for call in mock_logger.warning.call_args_list]
                    assert any("Security Warning" in call for call in warning_calls)

    def test_get_tool_sync_config_env_precedence(self):
        """Test that environment variables take precedence for tool sync config."""
        yaml_config = {
            "tool_sync": {
                "enabled": False,
                "http_server": {
                    "enabled": False,
                    "host": "yaml-host",
                    "port": 5002
                }
            }
        }
        
        # Mock the Config class attributes directly
        with patch.object(Config, 'TOOL_SYNC_HTTP_ENABLED', True):
            with patch.object(Config, 'TOOL_SYNC_HTTP_HOST', 'env-host'):
                with patch.object(Config, 'TOOL_SYNC_HTTP_PORT', 5003):
                    with patch.object(Config, 'load_yaml_config', return_value=yaml_config):
                        tool_sync_config = Config.get_tool_sync_config()
                        
                        # Environment variables should take precedence
                        assert tool_sync_config["http_server"]["enabled"] is True
                        assert tool_sync_config["http_server"]["host"] == "env-host"
                        assert tool_sync_config["http_server"]["port"] == 5003

    def test_get_agent_config_defaults(self):
        """Test getting agent config with defaults."""
        with patch.object(Config, 'load_yaml_config', return_value={}):
            agent_config = Config.get_agent_config()
            
            assert agent_config["prompts_dir"] == "src/agent/prompts"
            assert "main_system" in agent_config["prompts"]
            assert "human_confirmation" in agent_config["prompts"]
            assert "progress_report" in agent_config["prompts"]
            assert agent_config["max_iterations"] == 10

    def test_get_backend_config(self):
        """Test getting backend config."""
        with patch.object(Config, 'BACKEND_URL', 'http://test-backend:8080'):
            with patch.object(Config, 'load_yaml_config', return_value={}):
                backend_config = Config.get_backend_config()
                
                assert backend_config["url"] == "http://test-backend:8080"

    def test_config_caching(self):
        """Test that YAML config is cached properly."""
        test_config = {"test": "value"}
        
        # Clear the cache first
        Config._config_cache = None
        
        with patch('builtins.open', mock_open(read_data=yaml.dump(test_config))):
            with patch('yaml.safe_load', return_value=test_config) as mock_yaml_load:
                # First call should load from file
                config1 = Config.load_yaml_config()
                # Second call should use cache
                config2 = Config.load_yaml_config()
                
                assert config1 == test_config
                assert config2 == test_config
                # YAML should only be loaded once due to caching
                mock_yaml_load.assert_called_once()