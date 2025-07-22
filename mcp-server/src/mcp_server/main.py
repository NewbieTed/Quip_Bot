"""
Main entry point for the MCP Server
"""

import logging
from src.mcp_server.app import mcp
from src.mcp_server.config import Config

# Configure logging
logging.basicConfig(**Config.get_logging_config())
logger = logging.getLogger(__name__)

if __name__ == "__main__":
    logger.info("Starting MCP Server...")
    try:
        # Use streamable-http transport and set the path prefix to match AI backend expectations
        mcp.run(
            transport="streamable-http",
            host=Config.HOST, 
            port=Config.PORT,
            path="/mcp"
        )
        logger.info(f"MCP Server running at http://{Config.HOST}:{Config.PORT}/mcp")
    except Exception as e:
        logger.error(f"Error starting MCP Server: {str(e)}")
        raise
