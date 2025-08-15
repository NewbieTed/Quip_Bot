# Agent Test Suite

This directory contains comprehensive unit tests for the agent project. The tests are designed to validate all major components without requiring integration with external services.

## Test Structure

```
tests/
├── conftest.py                    # Pytest configuration and fixtures
├── test_config.py                 # Configuration management tests
├── test_prompt_loader.py          # Prompt loading utility tests
├── test_redis_client.py           # Redis client tests
├── test_tool_publisher.py         # Tool publisher service tests
├── test_tool_discovery.py         # Tool discovery service tests
├── test_metrics_service.py        # Metrics service tests
├── test_health_check.py           # Health check service tests
├── test_agent_runner.py           # Agent runner functionality tests
├── test_api_routes.py             # API routes tests
├── test_tool_sync_controller.py   # Tool sync controller tests
├── test_runner.py                 # Test runner script
└── README.md                      # This file
```

## Test Categories

### Configuration Tests (`test_config.py`)
- Environment variable loading
- YAML configuration parsing
- Configuration precedence (env vars over YAML)
- Security warnings for non-localhost URLs
- Default value handling

### Utility Tests (`test_prompt_loader.py`)
- Prompt file loading and caching
- Error handling for missing files/keys
- Whitespace trimming and encoding
- Cache management and reloading

### Redis Client Tests (`test_redis_client.py`)
- Connection management and retry logic
- Connection pool creation
- Operation execution with retry
- Context manager functionality
- Error handling and logging

### Tool Publisher Tests (`test_tool_publisher.py`)
- Message creation and serialization
- Redis publishing with retry logic
- Message queuing for failed publishes
- Metrics tracking and reporting
- Connection status monitoring

### Tool Discovery Tests (`test_tool_discovery.py`)
- Local tool discovery
- MCP tool discovery with fallback
- Tool change detection and comparison
- Naming validation and server assignment
- Error handling and connection recovery

### Metrics Service Tests (`test_metrics_service.py`)
- Metrics recording and aggregation
- Health status determination
- Average calculations and success rates
- Metrics reset functionality
- Comprehensive metrics summary

### Health Check Tests (`test_health_check.py`)
- Component health checking (Redis, discovery, publisher)
- Comprehensive system health assessment
- Error handling and status reporting
- Async health check operations
- Logging and monitoring integration

### Agent Runner Tests (`test_agent_runner.py`)
- Message validation and formatting
- Agent setup and configuration
- Stream processing and response handling
- Tool whitelist management
- Approval flow handling

### API Routes Tests (`test_api_routes.py`)
- Request validation and parsing
- Health check endpoints
- Agent invocation endpoints
- Tool whitelist update endpoints
- Error handling and HTTP status codes

### Tool Sync Controller Tests (`test_tool_sync_controller.py`)
- Resync request handling
- Tool discovery with timeout management
- Metrics recording for sync operations
- Error handling and HTTP responses
- Logging and monitoring integration

## Running Tests

### Run All Tests (Poetry)
```bash
# Install test dependencies first
poetry install --with dev,test --no-root

# From the agent directory
poetry run pytest tests/

# Or using the test runner
poetry run python tests/test_runner.py
```

### Run Specific Test Files
```bash
# Run configuration tests
poetry run pytest tests/test_config.py -v

# Run Redis client tests
poetry run pytest tests/test_redis_client.py -v
```

### Run Specific Test Classes or Methods
```bash
# Run specific test class
poetry run pytest tests/test_config.py::TestConfig -v

# Run specific test method
poetry run pytest tests/test_config.py::TestConfig::test_environment_variables_loaded -v
```

### Run Tests with Pattern Matching
```bash
# Run all tests containing "redis" in the name
poetry run python tests/test_runner.py redis

# Run all async tests
poetry run pytest -k "async" -v
```

### Run Tests with Coverage
```bash
# Coverage dependencies are included in the test group
poetry install --with test --no-root

# Run with coverage report
poetry run pytest tests/ --cov=src --cov-report=html --cov-report=term
```

## Test Configuration

### Pytest Configuration (`pytest.ini`)
- Async test support with `pytest-asyncio`
- Strict marker enforcement
- Warning filters for cleaner output
- Custom test discovery patterns

### Fixtures (`conftest.py`)
- Mock configuration objects
- Mock Redis clients and tools
- Temporary directories for file tests
- Sample data objects for testing
- Event loop management for async tests

## Mocking Strategy

The tests use extensive mocking to isolate units under test:

- **External Dependencies**: Redis, OpenAI API, MCP servers are mocked
- **File System**: Temporary directories and mock file operations
- **Network Calls**: All HTTP requests are mocked
- **Time-Dependent Operations**: Fixed timestamps for consistent testing
- **Async Operations**: AsyncMock for async function mocking

## Test Data

Tests use realistic but safe test data:
- UUIDs for request IDs
- Localhost URLs for server configurations
- Generic tool names and descriptions
- Fixed timestamps for time-dependent tests
- Placeholder API keys and credentials

## Best Practices

### Test Isolation
- Each test is independent and can run in any order
- No shared state between tests
- Proper setup and teardown using fixtures
- Mock objects are reset between tests

### Error Testing
- Tests cover both success and failure scenarios
- Edge cases and boundary conditions are tested
- Exception handling is validated
- Error messages and status codes are verified

### Async Testing
- Proper async/await usage in test functions
- AsyncMock for async dependencies
- Event loop management for session-scoped fixtures
- Timeout handling for async operations

### Assertions
- Clear and specific assertions
- Multiple assertions per test when appropriate
- Descriptive error messages
- Validation of both return values and side effects

## Continuous Integration

These tests are designed to run in CI environments:
- No external service dependencies
- Fast execution (all tests complete in under 30 seconds)
- Deterministic results with fixed test data
- Clear failure reporting with detailed error messages

## Adding New Tests

When adding new functionality to the agent:

1. **Create corresponding test file** following the naming convention `test_<module_name>.py`
2. **Add test class** for each class being tested
3. **Include both success and failure scenarios**
4. **Mock external dependencies** appropriately
5. **Add fixtures to `conftest.py`** if needed by multiple test files
6. **Update this README** if adding new test categories

### Test Template
```python
"""
Unit tests for new module.
"""

import pytest
from unittest.mock import Mock, patch

from src.agent.new_module import NewClass


class TestNewClass:
    """Test cases for NewClass."""

    def test_init(self):
        """Test class initialization."""
        instance = NewClass()
        assert instance is not None

    def test_method_success(self):
        """Test successful method execution."""
        instance = NewClass()
        result = instance.method()
        assert result == expected_value

    def test_method_error(self):
        """Test method error handling."""
        instance = NewClass()
        with pytest.raises(ExpectedException):
            instance.method_with_error()

    @pytest.mark.asyncio
    async def test_async_method(self):
        """Test async method execution."""
        instance = NewClass()
        result = await instance.async_method()
        assert result == expected_value
```

## Troubleshooting

### Common Issues

1. **Import Errors**: Ensure the agent source code is in your Python path
2. **Async Test Failures**: Make sure to use `@pytest.mark.asyncio` decorator
3. **Mock Issues**: Verify mock patches are targeting the correct module paths
4. **Fixture Errors**: Check that fixture dependencies are properly declared

### Debug Mode
```bash
# Run with debug output
python -m pytest tests/ -v -s --tb=long

# Run single test with debugging
python -m pytest tests/test_config.py::TestConfig::test_method -v -s --pdb
```

### Performance Issues
```bash
# Show slowest tests
python -m pytest tests/ --durations=10

# Run tests in parallel (requires pytest-xdist)
python -m pytest tests/ -n auto
```