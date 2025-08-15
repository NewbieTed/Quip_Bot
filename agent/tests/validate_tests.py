"""
Test validation script to ensure all test modules can be imported correctly.

This script helps identify import issues before running the full test suite.
"""

import sys
import importlib
from pathlib import Path


def validate_test_imports():
    """Validate that all test modules can be imported."""
    test_dir = Path(__file__).parent
    test_files = list(test_dir.glob("test_*.py"))
    
    print(f"Validating {len(test_files)} test modules...")
    print("-" * 50)
    
    success_count = 0
    failure_count = 0
    
    for test_file in sorted(test_files):
        module_name = test_file.stem
        print(f"Importing {module_name}...", end=" ")
        
        try:
            # Add tests directory to path if not already there
            if str(test_dir) not in sys.path:
                sys.path.insert(0, str(test_dir))
            
            # Import the test module
            importlib.import_module(module_name)
            print("✓ OK")
            success_count += 1
            
        except Exception as e:
            print(f"✗ FAILED: {e}")
            failure_count += 1
    
    print("-" * 50)
    print(f"Results: {success_count} successful, {failure_count} failed")
    
    if failure_count > 0:
        print("\nSome test modules failed to import. Please fix the import issues before running tests.")
        return False
    else:
        print("\nAll test modules imported successfully!")
        return True


def check_test_dependencies():
    """Check that required test dependencies are available."""
    required_packages = [
        "pytest",
        "pytest_asyncio",
        "unittest.mock"
    ]
    
    print("Checking test dependencies...")
    print("-" * 30)
    
    missing_packages = []
    
    for package in required_packages:
        try:
            importlib.import_module(package)
            print(f"✓ {package}")
        except ImportError:
            print(f"✗ {package} (missing)")
            missing_packages.append(package)
    
    if missing_packages:
        print(f"\nMissing packages: {', '.join(missing_packages)}")
        print("Install with: pip install pytest pytest-asyncio")
        return False
    else:
        print("\nAll test dependencies are available!")
        return True


def main():
    """Main validation function."""
    print("Agent Test Validation")
    print("=" * 50)
    
    # Check dependencies first
    deps_ok = check_test_dependencies()
    print()
    
    if not deps_ok:
        print("Cannot proceed with import validation due to missing dependencies.")
        return False
    
    # Validate test imports
    imports_ok = validate_test_imports()
    
    return imports_ok


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)