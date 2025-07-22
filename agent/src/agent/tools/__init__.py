"""
Direct tool bindings for the AI agent.
These tools are bound directly to the LangGraph agent instead of using MCP.
"""

import os
import sys
import inspect
import importlib
import glob
from pathlib import Path
from langchain_core.tools import BaseTool
import logging

logger = logging.getLogger(__name__)

# Export all tools for easy import
__all__ = ['get_all_tools']

_cached_tools = None


def _load_tools():
    """Discover and load all tools from the tools directory and other tool locations."""
    tools = []
    
    # Get the tools directory
    tools_dir = Path(__file__).parent
    logger.info("Scanning for tools in: %s", tools_dir)
    
    # Collect all Python files in the tools directory and subdirectories
    tool_files = []
    
    # Add files from tools directory (top level)
    tool_files.extend([
        (f, f"src.agent.tools.{Path(f).stem}")
        for f in glob.glob(str(tools_dir / "*.py"))
        if not os.path.basename(f).startswith("_")
    ])
    
    # Add files from subdirectories
    for subdir in [d for d in tools_dir.iterdir() if d.is_dir() and not d.name.startswith("_")]:
        # Get the module name for this subdirectory
        subdir_module = f"src.agent.tools.{subdir.name}"
        
        # Add Python files from this subdirectory
        tool_files.extend([
            (f, f"{subdir_module}.{Path(f).stem}")
            for f in glob.glob(str(subdir / "*.py"))
            if not os.path.basename(f).startswith("_")
        ])
    
    # Process each file
    for file_path, module_name in tool_files:
        try:
            logger.debug("Loading module: %s from %s", module_name, file_path)
            
            # Import the module
            if module_name in sys.modules:
                # Reload if already imported (for development)
                module = importlib.reload(sys.modules[module_name])
            else:
                module = importlib.import_module(module_name)
            
            # Find all BaseTool instances in the module
            module_tools = []
            current_module = module
            
            for name, obj in inspect.getmembers(current_module):
                if isinstance(obj, BaseTool):
                    logger.info(f"Added {name} to tools from {module_name}")
                    module_tools.append(obj)
            
            # Also check for __all_tools__ list in the module
            if hasattr(module, '__all_tools__'):
                for tool in module.__all_tools__:
                    if isinstance(tool, BaseTool) and tool not in module_tools:
                        logger.info(f"Added tool {tool.name} from __all_tools__ in {module_name}")
                        module_tools.append(tool)
            
            tools.extend(module_tools)
            logger.info("Loaded %d tools from %s", len(module_tools), module_name)
            
        except Exception as e:
            logger.error("Failed to load tools from %s: %s", module_name, str(e))
            continue
    
    logger.info("Total tools discovered: %d", len(tools))
    return tools


def get_all_tools(force_reload=False):
    """
    Get all available tools for the agent.
    
    Args:
        force_reload: If True, force reload all tool modules (useful for development)
    
    Returns:
        List of BaseTool instances
    """
    global _cached_tools
    
    if _cached_tools is None or force_reload:
        logger.info("Loading tools from all tool locations...")
        _cached_tools = _load_tools()
    
    return _cached_tools
