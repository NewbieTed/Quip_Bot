import os
import yaml
import logging
from pathlib import Path
from dotenv import load_dotenv
from typing import Dict, Any, Optional

# Load environment variables from .env file
load_dotenv()


class Config:
    """Configuration management for MCP Server"""
    
    _config_cache: Optional[Dict[str, Any]] = None
    _config_file = Path("config.yaml")
    
    # Environment Variables (sensitive data)
    DEBUG = os.getenv("DEBUG", "false").lower() == "true"
    HOST = os.getenv("HOST", "0.0.0.0")
    PORT = int(os.getenv("PORT", "8000"))
    BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
    HTTP_TIMEOUT = int(os.getenv("HTTP_TIMEOUT", "10"))
    REQUEST_TIMEOUT = int(os.getenv("REQUEST_TIMEOUT", "30"))
    
    # Database Configuration
    DB_HOST = os.getenv("DB_HOST", "localhost")
    DB_PORT = int(os.getenv("DB_PORT", "5432"))
    DB_NAME = os.getenv("DB_NAME", "quip_db")
    DB_USER = os.getenv("DB_USER", "postgres")
    DB_PASSWORD = os.getenv("DB_PASSWORD")
    
    @classmethod
    def load_yaml_config(cls) -> Dict[str, Any]:
        """Load configuration from YAML file"""
        if cls._config_cache is None:
            try:
                with open(cls._config_file, 'r') as f:
                    cls._config_cache = yaml.safe_load(f)
            except FileNotFoundError:
                cls._config_cache = {}
        return cls._config_cache
    
    @classmethod
    def get_backend_url(cls) -> str:
        """Get backend URL"""
        return cls.BACKEND_URL
    
    @classmethod
    def get_http_timeout(cls) -> int:
        """Get HTTP timeout"""
        return cls.HTTP_TIMEOUT

    @classmethod
    def get_logging_config(cls) -> Dict[str, Any]:
        """Get logging configuration with validated types"""
        config = cls.load_yaml_config().get("logging", {})

        level_str = config.get("level", "INFO").upper()
        level = getattr(logging, level_str, logging.INFO)
        
        # Ensure logs directory exists
        os.makedirs("logs", exist_ok=True)
        
        handlers = [logging.StreamHandler()]
        
        # Add file handler if specified in config
        if "file" in config.get("handlers", []):
            file_path = config.get("file_path", "logs/mcp-server.log")
            handlers.append(logging.FileHandler(file_path))

        return {
            "level": level,
            "format": config.get("format", "%(asctime)s - %(name)s - %(levelname)s - %(message)s"),
            "handlers": handlers
        }
    
    @classmethod
    def get_http_config(cls) -> Dict[str, Any]:
        """Get HTTP client configuration"""
        config = cls.load_yaml_config()
        return config.get("http", {
            "user_agent": "mcp-server/1.0.0",
            "retry_attempts": 3,
            "retry_delay": 1
        })
    
    @classmethod
    def get_tools_config(cls) -> Dict[str, Any]:
        """Get tools configuration"""
        config = cls.load_yaml_config()
        return config.get("tools", {
            "weather": {"enabled": True},
            "quip_backend": {"enabled": True}
        })
