"""
Redis client with connection management and retry logic for the agent.
"""

import asyncio
import logging
import time
from typing import Optional, Any, Dict
import redis
from redis.exceptions import ConnectionError, TimeoutError, RedisError
from src.config.config_loader import Config

logger = logging.getLogger(__name__)


class RedisClient:
    """Redis client with connection management and retry logic."""
    
    def __init__(self):
        self._client: Optional[redis.Redis] = None
        self._config = Config.get_redis_config()
        self._connection_pool: Optional[redis.ConnectionPool] = None
        self._is_connected = False
        
    def _create_connection_pool(self) -> redis.ConnectionPool:
        """Create Redis connection pool."""
        pool_kwargs = {
            'host': self._config['host'],
            'port': self._config['port'],
            'db': self._config['db'],
            'decode_responses': True,
            'socket_connect_timeout': 5,
            'socket_timeout': 5,
            'retry_on_timeout': True,
            'health_check_interval': 30,
        }
        
        if self._config['password']:
            pool_kwargs['password'] = self._config['password']
            
        return redis.ConnectionPool(**pool_kwargs)
    
    def _create_client(self) -> redis.Redis:
        """Create Redis client with connection pool."""
        if not self._connection_pool:
            self._connection_pool = self._create_connection_pool()
        
        return redis.Redis(connection_pool=self._connection_pool)
    
    def connect(self) -> bool:
        """
        Establish connection to Redis with retry logic.
        
        Returns:
            bool: True if connection successful, False otherwise
        """
        if self._is_connected and self._client:
            try:
                # Test existing connection
                self._client.ping()
                return True
            except RedisError:
                # Log Redis connection status change (Requirement 4.3)
                logger.warning("Existing Redis connection failed ping test, reconnecting", extra={
                    "event_type": "redis_connection_status",
                    "status": "connection_lost",
                    "host": self._config['host'],
                    "port": self._config['port']
                })
                self._is_connected = False
        
        retry_config = self._config['retry']
        max_attempts = retry_config['max_attempts']
        base_delay = retry_config['base_delay']
        max_delay = retry_config['max_delay']
        
        for attempt in range(max_attempts):
            try:
                logger.info("Attempting to connect to Redis", extra={
                    "event_type": "redis_connection_attempt",
                    "attempt": attempt + 1,
                    "max_attempts": max_attempts,
                    "host": self._config['host'],
                    "port": self._config['port']
                })
                
                self._client = self._create_client()
                
                # Test connection
                self._client.ping()
                
                self._is_connected = True
                
                # Log successful connection (Requirement 4.3)
                logger.info("Successfully connected to Redis", extra={
                    "event_type": "redis_connection_status",
                    "status": "connected",
                    "attempt": attempt + 1,
                    "host": self._config['host'],
                    "port": self._config['port']
                })
                return True
                
            except (ConnectionError, TimeoutError) as e:
                logger.warning("Redis connection attempt failed", extra={
                    "event_type": "redis_connection_failure",
                    "attempt": attempt + 1,
                    "max_attempts": max_attempts,
                    "error_type": type(e).__name__,
                    "error_message": str(e),
                    "host": self._config['host'],
                    "port": self._config['port']
                })
                
                if attempt < max_attempts - 1:
                    # Calculate exponential backoff delay
                    delay = min(base_delay * (2 ** attempt), max_delay)
                    logger.info("Retrying Redis connection", extra={
                        "event_type": "redis_retry_delay",
                        "delay_seconds": delay,
                        "next_attempt": attempt + 2
                    })
                    time.sleep(delay)
                else:
                    logger.error("Failed to connect to Redis after all attempts", extra={
                        "event_type": "redis_connection_exhausted",
                        "max_attempts": max_attempts,
                        "host": self._config['host'],
                        "port": self._config['port']
                    })
                    
            except Exception as e:
                logger.error("Unexpected error connecting to Redis", extra={
                    "event_type": "redis_connection_error",
                    "error_type": type(e).__name__,
                    "error_message": str(e),
                    "attempt": attempt + 1,
                    "host": self._config['host'],
                    "port": self._config['port']
                }, exc_info=True)
                break
        
        self._is_connected = False
        return False
    
    def disconnect(self):
        """Disconnect from Redis."""
        if self._client:
            try:
                self._client.close()
                logger.info("Disconnected from Redis")
            except Exception as e:
                logger.warning(f"Error disconnecting from Redis: {e}")
            finally:
                self._client = None
                self._is_connected = False
        
        if self._connection_pool:
            try:
                self._connection_pool.disconnect()
            except Exception as e:
                logger.warning(f"Error disconnecting Redis connection pool: {e}")
            finally:
                self._connection_pool = None
    
    def is_connected(self) -> bool:
        """Check if Redis client is connected."""
        if not self._is_connected or not self._client:
            return False
        
        try:
            self._client.ping()
            return True
        except RedisError:
            logger.warning("Redis connection lost")
            self._is_connected = False
            return False
    
    def execute_with_retry(self, operation_name: str, operation_func, *args, **kwargs) -> Any:
        """
        Execute Redis operation with retry logic.
        
        Args:
            operation_name: Name of the operation for logging
            operation_func: Redis operation function to execute
            *args: Arguments for the operation
            **kwargs: Keyword arguments for the operation
            
        Returns:
            Result of the operation or None if all retries failed
        """
        retry_config = self._config['retry']
        max_attempts = retry_config['max_attempts']
        base_delay = retry_config['base_delay']
        max_delay = retry_config['max_delay']
        
        for attempt in range(max_attempts):
            try:
                # Ensure we're connected
                if not self.is_connected():
                    if not self.connect():
                        raise ConnectionError("Failed to establish Redis connection")
                
                # Execute the operation
                result = operation_func(*args, **kwargs)
                
                if attempt > 0:
                    logger.info(f"Redis operation '{operation_name}' succeeded on attempt {attempt + 1}")
                
                return result
                
            except (ConnectionError, TimeoutError) as e:
                logger.warning(f"Redis operation '{operation_name}' failed on attempt {attempt + 1}: {e}")
                
                # Mark as disconnected to force reconnection
                self._is_connected = False
                
                if attempt < max_attempts - 1:
                    # Calculate exponential backoff delay
                    delay = min(base_delay * (2 ** attempt), max_delay)
                    logger.info(f"Retrying Redis operation '{operation_name}' in {delay} seconds...")
                    time.sleep(delay)
                else:
                    logger.error(f"Redis operation '{operation_name}' failed after {max_attempts} attempts")
                    
            except Exception as e:
                logger.error(f"Unexpected error in Redis operation '{operation_name}': {e}")
                break
        
        return None
    
    def lpush(self, key: str, *values) -> Optional[int]:
        """Push values to the left of a Redis list with retry logic."""
        return self.execute_with_retry(
            f"lpush({key})",
            lambda: self._client.lpush(key, *values)
        )
    
    def rpush(self, key: str, *values) -> Optional[int]:
        """Push values to the right of a Redis list with retry logic."""
        return self.execute_with_retry(
            f"rpush({key})",
            lambda: self._client.rpush(key, *values)
        )
    
    def llen(self, key: str) -> Optional[int]:
        """Get length of a Redis list with retry logic."""
        return self.execute_with_retry(
            f"llen({key})",
            lambda: self._client.llen(key)
        )
    
    def ping(self) -> bool:
        """Ping Redis server with retry logic."""
        result = self.execute_with_retry(
            "ping",
            lambda: self._client.ping()
        )
        return result is True
    
    def __enter__(self):
        """Context manager entry."""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit."""
        self.disconnect()


# Global Redis client instance
_redis_client: Optional[RedisClient] = None


def get_redis_client() -> RedisClient:
    """Get the global Redis client instance."""
    global _redis_client
    if _redis_client is None:
        _redis_client = RedisClient()
    return _redis_client


def close_redis_client():
    """Close the global Redis client instance."""
    global _redis_client
    if _redis_client:
        _redis_client.disconnect()
        _redis_client = None