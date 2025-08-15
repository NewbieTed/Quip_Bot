"""
Unit tests for Redis client with connection management and retry logic.
"""

import pytest
import time
from unittest.mock import Mock, patch, MagicMock
from redis.exceptions import ConnectionError, TimeoutError, RedisError

from src.agent.redis.client import RedisClient, get_redis_client, close_redis_client


class TestRedisClient:
    """Test cases for RedisClient class."""

    def test_init(self, mock_config):
        """Test RedisClient initialization."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            client = RedisClient()
            
            assert client._client is None
            assert client._connection_pool is None
            assert client._is_connected is False
            assert client._config == mock_config["redis"]

    def test_create_connection_pool(self, mock_config):
        """Test connection pool creation."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('src.agent.redis.client.redis.ConnectionPool') as mock_pool_class:
                mock_pool = Mock()
                mock_pool_class.return_value = mock_pool
                
                client = RedisClient()
                pool = client._create_connection_pool()
                
                assert pool == mock_pool
                mock_pool_class.assert_called_once()
                call_kwargs = mock_pool_class.call_args[1]
                assert call_kwargs['host'] == 'localhost'
                assert call_kwargs['port'] == 6379
                assert call_kwargs['db'] == 0

    def test_create_connection_pool_with_password(self, mock_config):
        """Test connection pool creation with password."""
        config_with_password = mock_config["redis"].copy()
        config_with_password["password"] = "test-password"
        
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=config_with_password):
            with patch('src.agent.redis.client.redis.ConnectionPool') as mock_pool_class:
                client = RedisClient()
                client._create_connection_pool()
                
                call_kwargs = mock_pool_class.call_args[1]
                assert call_kwargs['password'] == "test-password"

    def test_create_client(self, mock_config):
        """Test Redis client creation."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('src.agent.redis.client.redis.Redis') as mock_redis_class:
                mock_redis = Mock()
                mock_redis_class.return_value = mock_redis
                
                client = RedisClient()
                redis_client = client._create_client()
                
                assert redis_client == mock_redis
                mock_redis_class.assert_called_once()

    def test_connect_success(self, mock_config):
        """Test successful Redis connection."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('src.agent.redis.client.redis.Redis') as mock_redis_class:
                mock_redis = Mock()
                mock_redis.ping.return_value = True
                mock_redis_class.return_value = mock_redis
                
                client = RedisClient()
                result = client.connect()
                
                assert result is True
                assert client._is_connected is True
                assert client._client == mock_redis
                mock_redis.ping.assert_called_once()

    def test_connect_existing_connection_valid(self, mock_config):
        """Test connect with existing valid connection."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            mock_redis = Mock()
            mock_redis.ping.return_value = True
            
            client = RedisClient()
            client._client = mock_redis
            client._is_connected = True
            
            result = client.connect()
            
            assert result is True
            mock_redis.ping.assert_called_once()

    def test_connect_existing_connection_invalid(self, mock_config):
        """Test connect with existing invalid connection."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('src.agent.redis.client.redis.Redis') as mock_redis_class:
                # Existing connection that fails ping
                old_redis = Mock()
                old_redis.ping.side_effect = RedisError("Connection lost")
                
                # New connection that works
                new_redis = Mock()
                new_redis.ping.return_value = True
                mock_redis_class.return_value = new_redis
                
                client = RedisClient()
                client._client = old_redis
                client._is_connected = True
                
                result = client.connect()
                
                assert result is True
                assert client._client == new_redis
                assert client._is_connected is True

    def test_connect_retry_logic(self, mock_config):
        """Test connection retry logic."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('src.agent.redis.client.redis.Redis') as mock_redis_class:
                with patch('time.sleep') as mock_sleep:
                    # First two attempts fail, third succeeds
                    mock_redis1 = Mock()
                    mock_redis1.ping.side_effect = ConnectionError("Connection failed")
                    mock_redis2 = Mock()
                    mock_redis2.ping.side_effect = TimeoutError("Timeout")
                    mock_redis3 = Mock()
                    mock_redis3.ping.return_value = True
                    
                    mock_redis_class.side_effect = [mock_redis1, mock_redis2, mock_redis3]
                    
                    client = RedisClient()
                    result = client.connect()
                    
                    assert result is True
                    assert client._is_connected is True
                    assert mock_redis_class.call_count == 3
                    assert mock_sleep.call_count == 2  # Two retries

    def test_connect_max_retries_exceeded(self, mock_config):
        """Test connection failure after max retries."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('src.agent.redis.client.redis.Redis') as mock_redis_class:
                with patch('time.sleep'):
                    # All attempts fail
                    mock_redis = Mock()
                    mock_redis.ping.side_effect = ConnectionError("Connection failed")
                    mock_redis_class.return_value = mock_redis
                    
                    client = RedisClient()
                    result = client.connect()
                    
                    assert result is False
                    assert client._is_connected is False
                    assert mock_redis_class.call_count == 3  # max_attempts

    def test_disconnect(self, mock_config):
        """Test Redis disconnection."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            mock_redis = Mock()
            mock_pool = Mock()
            
            client = RedisClient()
            client._client = mock_redis
            client._connection_pool = mock_pool
            client._is_connected = True
            
            client.disconnect()
            
            assert client._client is None
            assert client._connection_pool is None
            assert client._is_connected is False
            mock_redis.close.assert_called_once()
            mock_pool.disconnect.assert_called_once()

    def test_disconnect_with_errors(self, mock_config):
        """Test disconnection with errors."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            mock_redis = Mock()
            mock_redis.close.side_effect = Exception("Close error")
            mock_pool = Mock()
            mock_pool.disconnect.side_effect = Exception("Pool disconnect error")
            
            client = RedisClient()
            client._client = mock_redis
            client._connection_pool = mock_pool
            client._is_connected = True
            
            # Should not raise exceptions
            client.disconnect()
            
            assert client._client is None
            assert client._connection_pool is None
            assert client._is_connected is False

    def test_is_connected_true(self, mock_config):
        """Test is_connected when connection is valid."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            mock_redis = Mock()
            mock_redis.ping.return_value = True
            
            client = RedisClient()
            client._client = mock_redis
            client._is_connected = True
            
            result = client.is_connected()
            
            assert result is True
            mock_redis.ping.assert_called_once()

    def test_is_connected_false_no_client(self, mock_config):
        """Test is_connected when no client exists."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            client = RedisClient()
            
            result = client.is_connected()
            
            assert result is False

    def test_is_connected_false_ping_fails(self, mock_config):
        """Test is_connected when ping fails."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            mock_redis = Mock()
            mock_redis.ping.side_effect = RedisError("Ping failed")
            
            client = RedisClient()
            client._client = mock_redis
            client._is_connected = True
            
            result = client.is_connected()
            
            assert result is False
            assert client._is_connected is False

    def test_execute_with_retry_success(self, mock_config):
        """Test successful operation execution."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            mock_redis = Mock()
            mock_redis.ping.return_value = True
            
            client = RedisClient()
            client._client = mock_redis
            client._is_connected = True
            
            # Mock operation
            mock_operation = Mock(return_value="success")
            
            result = client.execute_with_retry("test_op", mock_operation, "arg1", key="value")
            
            assert result == "success"
            mock_operation.assert_called_once_with("arg1", key="value")

    def test_execute_with_retry_connection_recovery(self, mock_config):
        """Test operation execution with connection recovery."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch.object(RedisClient, 'connect', return_value=True) as mock_connect:
                with patch.object(RedisClient, 'is_connected', side_effect=[False, True]):
                    mock_operation = Mock(return_value="success")
                    
                    client = RedisClient()
                    result = client.execute_with_retry("test_op", mock_operation)
                    
                    assert result == "success"
                    mock_connect.assert_called_once()

    def test_execute_with_retry_max_attempts(self, mock_config):
        """Test operation execution with max retry attempts."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch('time.sleep'):
                with patch.object(RedisClient, 'is_connected', return_value=True):
                    # Operation always fails
                    mock_operation = Mock(side_effect=ConnectionError("Always fails"))
                    
                    client = RedisClient()
                    result = client.execute_with_retry("test_op", mock_operation)
                    
                    assert result is None
                    assert mock_operation.call_count == 3  # max_attempts

    def test_redis_operations(self, mock_config):
        """Test Redis operation methods."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch.object(RedisClient, 'execute_with_retry') as mock_execute:
                mock_execute.return_value = 1
                
                client = RedisClient()
                
                # Test lpush
                result = client.lpush("test_key", "value1", "value2")
                assert result == 1
                mock_execute.assert_called()
                
                # Test rpush
                result = client.rpush("test_key", "value1", "value2")
                assert result == 1
                
                # Test llen
                result = client.llen("test_key")
                assert result == 1
                
                # Test ping
                mock_execute.return_value = True
                result = client.ping()
                assert result is True

    def test_context_manager(self, mock_config):
        """Test RedisClient as context manager."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            with patch.object(RedisClient, 'connect', return_value=True) as mock_connect:
                with patch.object(RedisClient, 'disconnect') as mock_disconnect:
                    
                    client = RedisClient()
                    
                    with client as ctx_client:
                        assert ctx_client == client
                    
                    mock_connect.assert_called_once()
                    mock_disconnect.assert_called_once()


class TestGlobalFunctions:
    """Test cases for global Redis client functions."""

    def test_get_redis_client_singleton(self, mock_config):
        """Test that get_redis_client returns singleton instance."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            # Clear any existing global client
            close_redis_client()
            
            client1 = get_redis_client()
            client2 = get_redis_client()
            
            assert client1 is client2
            assert isinstance(client1, RedisClient)

    def test_close_redis_client(self, mock_config):
        """Test closing global Redis client."""
        with patch('src.agent.redis.client.Config.get_redis_config', return_value=mock_config["redis"]):
            # Get a client instance
            client = get_redis_client()
            
            with patch.object(client, 'disconnect') as mock_disconnect:
                close_redis_client()
                mock_disconnect.assert_called_once()
            
            # Next call should create new instance
            new_client = get_redis_client()
            assert new_client is not client

    def test_close_redis_client_no_existing_client(self):
        """Test closing Redis client when none exists."""
        # Clear any existing client
        close_redis_client()
        
        # Should not raise any errors
        close_redis_client()