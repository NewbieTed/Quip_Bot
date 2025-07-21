#!/bin/bash

# MCP Server Setup Script
set -e

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_debug() {
    echo -e "${BLUE}[DEBUG]${NC} $1"
}

# Function to check if virtual environment is valid
check_venv_validity() {
    local venv_path="$1"

    if [ ! -d "$venv_path" ]; then
        return 1
    fi

    # Check if activation script exists
    if [ ! -f "$venv_path/bin/activate" ]; then
        print_warning "Virtual environment missing activation script"
        return 1
    fi

    # Check if Python executable exists
    if [ ! -f "$venv_path/bin/python" ]; then
        print_warning "Virtual environment missing Python executable"
        return 1
    fi

    # Check Python version in venv
    local venv_python_version
    venv_python_version=$("$venv_path/bin/python" -c 'import sys; print(".".join(map(str, sys.version_info[:2])))' 2>/dev/null || echo "")

    if [ -z "$venv_python_version" ]; then
        print_warning "Cannot determine Python version in virtual environment"
        return 1
    fi

    # Check if version meets requirements
    if [ "$(printf '%s\n' "3.12" "$venv_python_version" | sort -V | head -n1)" != "3.12" ]; then
        print_warning "Virtual environment Python version ($venv_python_version) does not meet requirements (>=3.12)"
        return 1
    fi

    print_debug "Virtual environment is valid (Python $venv_python_version)"
    return 0
}

# Function to check if a dependency is available
check_dependency() {
    local dep="$1"
    local python_cmd="$2"

    case "$dep" in
        "yaml")
            $python_cmd -c "import yaml" 2>/dev/null
            ;;
        "python-dotenv")
            $python_cmd -c "import dotenv" 2>/dev/null
            ;;
        *)
            $python_cmd -c "import ${dep//-/_}" 2>/dev/null
            ;;
    esac
}

# Function to verify and fix dependencies
verify_and_fix_dependencies() {
    local python_cmd="$1"
    local deps=("${@:2}")
    local missing_deps=()

    print_status "Verifying critical dependencies..."

    # Check each dependency
    for dep in "${deps[@]}"; do
        if ! check_dependency "$dep" "$python_cmd"; then
            print_warning "Critical dependency '$dep' is missing"
            missing_deps+=("$dep")
        fi
    done

    # If any dependencies are missing, force reinstall
    if [ ${#missing_deps[@]} -gt 0 ]; then
        print_error "Missing critical dependencies: ${missing_deps[*]}"
        print_status "Triggering full clean reinstall..."

        # Remove cached dependency hash to force reinstall
        [ -f "$PYPROJECT_HASH_FILE" ] && rm "$PYPROJECT_HASH_FILE"

        # Clear all cache and state files
        print_status "Clearing pip cache and state files..."
        "$VENV_PATH/bin/pip" cache purge 2>/dev/null || true
        [ -f "$PYPROJECT_HASH_FILE" ] && rm "$PYPROJECT_HASH_FILE"
        [ -f "pip-selfcheck.json" ] && rm "pip-selfcheck.json"

        # Clean reinstall with no cache
        print_status "Clean reinstalling all Python dependencies (no cache)..."
        if ! "$VENV_PATH/bin/pip" install --no-cache-dir --force-reinstall -e .; then
            print_error "Failed to reinstall dependencies from pyproject.toml"
            exit 1
        fi

        # Update hash
        echo "$CURRENT_HASH" > "$PYPROJECT_HASH_FILE"
        print_status "Dependencies clean-reinstalled successfully"

        # Re-verify all dependencies
        print_status "Re-verifying critical dependencies..."
        for dep in "${deps[@]}"; do
            if ! check_dependency "$dep" "$python_cmd"; then
                print_error "Critical dependency '$dep' still not available after reinstall"
                exit 1
            fi
        done
        print_status "All critical dependencies verified after reinstall ✓"
    else
        print_status "All critical dependencies verified ✓"
    fi
}

# Function to ensure logs directory exists
ensure_logs_directory() {
    if [ ! -d "logs" ]; then
        print_status "Creating logs directory..."
        mkdir -p logs
    fi
}

echo "🚀 Setting up MCP Server..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    print_error "Python 3 is not installed. Please install Python 3.12 or higher."
    print_error "On macOS: brew install python@3.12"
    print_error "On Ubuntu/Debian: sudo apt install python3.12 python3.12-venv"
    exit 1
fi

# Check Python version
PYTHON_VERSION=$(python3 -c 'import sys; print(".".join(map(str, sys.version_info[:2])))')
REQUIRED_VERSION="3.12"

if [ "$(printf '%s\n' "$REQUIRED_VERSION" "$PYTHON_VERSION" | sort -V | head -n1)" != "$REQUIRED_VERSION" ]; then
    print_error "Python $REQUIRED_VERSION or higher is required. Found: $PYTHON_VERSION"
    print_error "Please install Python 3.12 or higher:"
    print_error "On macOS: brew install python@3.12"
    print_error "On Ubuntu/Debian: sudo apt install python3.12 python3.12-venv"
    exit 1
fi

print_status "Python version: $PYTHON_VERSION ✓"

# Handle virtual environment
VENV_PATH="venv"
RECREATE_VENV=false

if [ -d "$VENV_PATH" ]; then
    print_status "Checking existing virtual environment..."
    if check_venv_validity "$VENV_PATH"; then
        print_status "Virtual environment is valid"
    else
        print_warning "Existing virtual environment is invalid or corrupted"
        print_status "Removing invalid virtual environment..."
        rm -rf "$VENV_PATH"
        RECREATE_VENV=true
    fi
else
    RECREATE_VENV=true
fi

if [ "$RECREATE_VENV" = true ]; then
    print_status "Creating new virtual environment..."
    if ! python3 -m venv "$VENV_PATH"; then
        print_error "Failed to create virtual environment"
        print_error "Make sure you have python3-venv installed:"
        print_error "On Ubuntu/Debian: sudo apt install python3.12-venv"
        exit 1
    fi
    print_status "Virtual environment created successfully"
fi

print_debug "Using virtual environment: $VENV_PATH"

# Check if dependencies are already installed
PYPROJECT_HASH_FILE=".pyproject_hash"
CURRENT_HASH=$(shasum -a 256 pyproject.toml | cut -d' ' -f1)

if [ -f "$PYPROJECT_HASH_FILE" ]; then
    STORED_HASH=$(cat "$PYPROJECT_HASH_FILE")
    if [ "$CURRENT_HASH" = "$STORED_HASH" ]; then
        print_status "Dependencies are up to date, skipping installation"
        SKIP_INSTALL=true
    else
        print_status "pyproject.toml has changed, updating dependencies..."
        SKIP_INSTALL=false
    fi
else
    print_status "First time setup, installing dependencies..."
    SKIP_INSTALL=false
fi

# Install/upgrade pip
print_status "Upgrading pip..."
if ! "$VENV_PATH/bin/pip" install --upgrade pip; then
    print_error "Failed to upgrade pip"
    exit 1
fi

# Install dependencies if needed
if [ "$SKIP_INSTALL" != true ]; then
    print_status "Installing Python dependencies from pyproject.toml..."
    if ! "$VENV_PATH/bin/pip" install -e .; then
        print_error "Failed to install dependencies from pyproject.toml"
        print_error "Check the error messages above and ensure all dependencies are available"
        exit 1
    fi

    # Store the hash of pyproject.toml
    echo "$CURRENT_HASH" > "$PYPROJECT_HASH_FILE"
    print_status "Dependencies installed successfully"
else
    print_status "Skipping dependency installation (already up to date)"
fi

# Verify critical dependencies
CRITICAL_DEPS=("uvicorn" "pydantic" "python-dotenv" "yaml" "fastmcp")
verify_and_fix_dependencies "$VENV_PATH/bin/python" "${CRITICAL_DEPS[@]}"

# Load environment variables
if [ -f ".env" ]; then
    print_status "Loading environment variables from .env..."
    export $(grep -v '^#' .env | grep -v '^$' | xargs)
    print_status "Environment variables loaded"
else
    print_warning ".env file not found. Please create one based on the template."
fi


ensure_logs_directory

print_status "✅ MCP Server setup completed successfully!"
print_status ""
print_status "Next steps:"
print_status "1. To activate the environment manually: source venv/bin/activate"
print_status "2. To start the server: python src/mcp_server/main.py"
print_status "3. Make sure to configure your .env file with required service URLs"