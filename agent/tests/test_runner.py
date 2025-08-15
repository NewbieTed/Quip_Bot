"""
Test runner script for the agent project.

This script provides a convenient way to run all tests with proper configuration.
"""

import sys
import subprocess
from pathlib import Path


def run_tests():
    """Run all tests with pytest."""
    # Get the agent directory (parent of tests directory)
    agent_dir = Path(__file__).parent.parent
    
    # Change to agent directory
    original_cwd = Path.cwd()
    try:
        import os
        os.chdir(agent_dir)
        
        # Run pytest with coverage and verbose output
        cmd = [
            "poetry", "run", "pytest",
            "tests/",
            "-v",
            "--tb=short",
            "--durations=10",
            "--color=yes"
        ]
        
        print(f"Running tests in {agent_dir}")
        print(f"Command: {' '.join(cmd)}")
        print("-" * 60)
        
        result = subprocess.run(cmd, capture_output=False)
        return result.returncode
        
    finally:
        os.chdir(original_cwd)


def run_specific_test(test_pattern):
    """Run specific tests matching the pattern."""
    agent_dir = Path(__file__).parent.parent
    
    original_cwd = Path.cwd()
    try:
        import os
        os.chdir(agent_dir)
        
        cmd = [
            "poetry", "run", "pytest",
            "-v",
            "-k", test_pattern,
            "--tb=short",
            "--color=yes"
        ]
        
        print(f"Running tests matching '{test_pattern}' in {agent_dir}")
        print(f"Command: {' '.join(cmd)}")
        print("-" * 60)
        
        result = subprocess.run(cmd, capture_output=False)
        return result.returncode
        
    finally:
        os.chdir(original_cwd)


if __name__ == "__main__":
    if len(sys.argv) > 1:
        # Run specific test pattern
        test_pattern = sys.argv[1]
        exit_code = run_specific_test(test_pattern)
    else:
        # Run all tests
        exit_code = run_tests()
    
    sys.exit(exit_code)