"""
Utility module for loading prompts from text files.
"""
import logging
from pathlib import Path
from typing import Dict
from src.config import Config

logger = logging.getLogger(__name__)


class PromptLoader:
    """Centralized prompt loader for all agent prompts."""
    
    def __init__(self):
        # Get configuration
        agent_config = Config.get_agent_config()
        prompts_dir = agent_config.get("prompts_dir", "src/agent/prompts")
        self.prompts_path = Path(prompts_dir)
        self.prompt_files = agent_config.get("prompts", {})
        
        # Cache for loaded prompts
        self._prompt_cache: Dict[str, str] = {}
    
    def load_prompt(self, prompt_key: str) -> str:
        """
        Load a prompt from a text file using the configured prompt key.
        
        Args:
            prompt_key: Key from the prompts' configuration (e.g., 'main_system', 'human_confirmation')
            
        Returns:
            The prompt content as a string
            
        Raises:
            KeyError: If the prompt key is not configured
            FileNotFoundError: If the prompt file doesn't exist
        """
        # Check cache first
        if prompt_key in self._prompt_cache:
            return self._prompt_cache[prompt_key]
        
        # Get filename from configuration
        if prompt_key not in self.prompt_files:
            raise KeyError(f"Prompt key '{prompt_key}' not found in configuration. Available keys: {list(self.prompt_files.keys())}")
        
        filename = self.prompt_files[prompt_key]
        prompt_file = self.prompts_path / filename
        
        if not prompt_file.exists():
            raise FileNotFoundError(f"Prompt file not found: {prompt_file}")
        
        try:
            content = prompt_file.read_text(encoding="utf-8").strip()
            self._prompt_cache[prompt_key] = content
            logger.debug("Loaded prompt '%s' from file '%s'", prompt_key, filename)
            return content
        except Exception as e:
            logger.error("Failed to load prompt %s from %s: %s", prompt_key, prompt_file, e)
            raise
    
    def reload_prompt(self, prompt_key: str) -> str:
        """
        Reload a prompt from file, bypassing cache.
        
        Args:
            prompt_key: Key from the prompts' configuration (e.g., 'main_system', 'human_confirmation')
            
        Returns:
            The prompt content as a string
        """
        # Clear from cache and reload
        self._prompt_cache.pop(prompt_key, None)
        return self.load_prompt(prompt_key)
    
    def clear_cache(self):
        """Clear the prompt cache."""
        self._prompt_cache.clear()
        logger.debug("Prompt cache cleared")


# Global prompt loader instance
_prompt_loader = PromptLoader()


def load_prompt(prompt_key: str) -> str:
    """
    Convenience function to load a prompt.
    
    Args:
        prompt_key: Key from the prompts configuration (e.g., 'main_system', 'human_confirmation')
        
    Returns:
        The prompt content as a string
    """
    return _prompt_loader.load_prompt(prompt_key)


def reload_prompt(prompt_key: str) -> str:
    """
    Convenience function to reload a prompt, bypassing cache.
    
    Args:
        prompt_key: Key from the prompts' configuration (e.g., 'main_system', 'human_confirmation')
        
    Returns:
        The prompt content as a string
    """
    return _prompt_loader.reload_prompt(prompt_key)