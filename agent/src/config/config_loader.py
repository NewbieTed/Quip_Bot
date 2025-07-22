import os
import yaml
from pathlib import Path
from dotenv import load_dotenv
from typing import Dict, Any, Optional

# Load environment variables from .env file (for local development)
# In Docker, environment variables are set by Docker Compose
load_dotenv()


class Config:
    """Configuration management for Agent"""
    
    _config_cache: Optional[Dict[str, Any]] = None
    _config_file = Path("config.yaml")
    
    # Environment Variables
    DEBUG = os.getenv("DEBUG", "false").lower() == "true"
    HOST = os.getenv("HOST", "0.0.0.0")
    PORT = int(os.getenv("PORT", "5000"))
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080")
    MCP_SERVER_URL = os.getenv("MCP_SERVER_URL", "http://localhost:8000/mcp")
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
    def get_openai_config(cls) -> Dict[str, Any]:
        """Get OpenAI configuration"""
        config = cls.load_yaml_config()
        return config.get("openai", {
            "model": "gpt-4o-mini",
            "temperature": 0,
            "max_tokens": 4000
        })
    
    @classmethod
    def get_logging_config(cls) -> Dict[str, Any]:
        """Get logging configuration"""
        config = cls.load_yaml_config()
        return config.get("logging", {
            "level": "INFO",
            "format": "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
        })
    
    @classmethod
    def get_agent_config(cls) -> Dict[str, Any]:
        """Get agent configuration"""
        config = cls.load_yaml_config()
        return config.get("agent", {
            "system_prompt_path": "src/agent/prompts/system_prompt.txt",
            "max_iterations": 10
        })
    
    @classmethod
    def get_mcp_config(cls) -> Dict[str, Any]:
        """Get MCP configuration"""
        config = cls.load_yaml_config()
        return config.get("mcp", {
            "enabled": True,
            "servers": {
                "QuipMCPServer": {
                    "transport": "streamable_http"
                }
            }
        })
        
    @classmethod
    def get_backend_config(cls) -> Dict[str, Any]:
        """Get backend API configuration"""
        config = cls.load_yaml_config()
        return config.get("backend", {
            "url": cls.BACKEND_URL
        })