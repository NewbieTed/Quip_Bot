from fastmcp import FastMCP
import logging

logger = logging.getLogger(__name__)

mcp = FastMCP(
    name="QuipMCPServer",
    instructions="""
        This server provides all tools related to the Quip's backend app.
    """
)

# Do NOT remove this, otherwise FastMCP will not register the tools
from tools import *

# Log the loaded tools
logger.info(f"Successfully loaded MCP tools.")
