"""
Unit tests for prompt loader utility.
"""

import pytest
import tempfile
from pathlib import Path
from unittest.mock import patch, Mock

from src.agent.utils.prompt_loader import PromptLoader, load_prompt, reload_prompt


class TestPromptLoader:
    """Test cases for PromptLoader class."""

    def test_load_prompt_success(self, temp_prompts_dir):
        """Test successful prompt loading."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt",
                "human_confirmation": "human_confirmation_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            prompt = loader.load_prompt("main_system")
            assert prompt == "Test system prompt"
            
            prompt = loader.load_prompt("human_confirmation")
            assert prompt == "Test confirmation prompt"

    def test_load_prompt_caching(self, temp_prompts_dir):
        """Test that prompts are cached after first load."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            # First load
            prompt1 = loader.load_prompt("main_system")
            # Second load should use cache
            prompt2 = loader.load_prompt("main_system")
            
            assert prompt1 == prompt2 == "Test system prompt"
            assert "main_system" in loader._prompt_cache

    def test_load_prompt_key_not_found(self, temp_prompts_dir):
        """Test error when prompt key is not configured."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            with pytest.raises(KeyError) as exc_info:
                loader.load_prompt("nonexistent_key")
            
            assert "not found in configuration" in str(exc_info.value)
            assert "main_system" in str(exc_info.value)  # Should show available keys

    def test_load_prompt_file_not_found(self, temp_prompts_dir):
        """Test error when prompt file doesn't exist."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "missing_prompt": "nonexistent_file.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            with pytest.raises(FileNotFoundError) as exc_info:
                loader.load_prompt("missing_prompt")
            
            assert "Prompt file not found" in str(exc_info.value)

    def test_reload_prompt(self, temp_prompts_dir):
        """Test prompt reloading bypasses cache."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            # Load prompt first time
            prompt1 = loader.load_prompt("main_system")
            assert prompt1 == "Test system prompt"
            
            # Modify the file
            prompt_file = temp_prompts_dir / "main_system_prompt.txt"
            prompt_file.write_text("Updated system prompt")
            
            # Reload should get updated content
            prompt2 = loader.reload_prompt("main_system")
            assert prompt2 == "Updated system prompt"
            
            # Cache should be updated
            prompt3 = loader.load_prompt("main_system")
            assert prompt3 == "Updated system prompt"

    def test_clear_cache(self, temp_prompts_dir):
        """Test cache clearing."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            # Load prompt to populate cache
            loader.load_prompt("main_system")
            assert len(loader._prompt_cache) == 1
            
            # Clear cache
            loader.clear_cache()
            assert len(loader._prompt_cache) == 0

    def test_load_prompt_with_encoding(self, temp_prompts_dir):
        """Test loading prompt with special characters."""
        # Create a prompt file with special characters
        special_prompt = "Test prompt with émojis 🤖 and special chars: ñáéíóú"
        prompt_file = temp_prompts_dir / "special_prompt.txt"
        prompt_file.write_text(special_prompt, encoding="utf-8")
        
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "special": "special_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            prompt = loader.load_prompt("special")
            assert prompt == special_prompt

    def test_load_prompt_strips_whitespace(self, temp_prompts_dir):
        """Test that loaded prompts have whitespace stripped."""
        # Create a prompt file with leading/trailing whitespace
        prompt_with_whitespace = "  \n  Test prompt with whitespace  \n  "
        prompt_file = temp_prompts_dir / "whitespace_prompt.txt"
        prompt_file.write_text(prompt_with_whitespace)
        
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "whitespace": "whitespace_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            prompt = loader.load_prompt("whitespace")
            assert prompt == "Test prompt with whitespace"

    def test_load_prompt_read_error(self, temp_prompts_dir):
        """Test error handling when file read fails."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            loader = PromptLoader()
            
            # Mock file read to raise an exception
            with patch('pathlib.Path.read_text', side_effect=IOError("Read error")):
                with pytest.raises(IOError):
                    loader.load_prompt("main_system")


class TestConvenienceFunctions:
    """Test cases for convenience functions."""

    def test_load_prompt_function(self, temp_prompts_dir):
        """Test the load_prompt convenience function."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        # Clear the global loader cache and create a new one with mocked config
        import src.agent.utils.prompt_loader
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            # Force recreation of the global loader
            src.agent.utils.prompt_loader._prompt_loader = src.agent.utils.prompt_loader.PromptLoader()
            
            prompt = load_prompt("main_system")
            assert prompt == "Test system prompt"

    def test_reload_prompt_function(self, temp_prompts_dir):
        """Test the reload_prompt convenience function."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        # Clear the global loader cache and create a new one with mocked config
        import src.agent.utils.prompt_loader
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            # Force recreation of the global loader
            src.agent.utils.prompt_loader._prompt_loader = src.agent.utils.prompt_loader.PromptLoader()
            
            # Load first
            prompt1 = load_prompt("main_system")
            assert prompt1 == "Test system prompt"
            
            # Modify file
            prompt_file = temp_prompts_dir / "main_system_prompt.txt"
            prompt_file.write_text("Reloaded prompt")
            
            # Reload
            prompt2 = reload_prompt("main_system")
            assert prompt2 == "Reloaded prompt"

    def test_global_prompt_loader_instance(self, temp_prompts_dir):
        """Test that convenience functions use the same global instance."""
        mock_config = {
            "prompts_dir": str(temp_prompts_dir),
            "prompts": {
                "main_system": "main_system_prompt.txt"
            }
        }
        
        with patch('src.agent.utils.prompt_loader.Config.get_agent_config', return_value=mock_config):
            # Load using convenience function
            load_prompt("main_system")
            
            # Check that the global instance has the cached prompt
            from src.agent.utils.prompt_loader import _prompt_loader
            assert "main_system" in _prompt_loader._prompt_cache