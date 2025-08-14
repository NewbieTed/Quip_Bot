# Discord Bot Frontend - Test Suite

This directory contains comprehensive tests for the Discord bot frontend enhancement project.

## Test Structure

### Unit Tests
- **API Client Tests** (`services/backend-api-client.test.ts`)
  - HTTP client functionality
  - Streaming response parsing
  - Error handling and retries
  - Tool whitelist management

- **Streaming Response Handler Tests** (`services/streaming-response-handler.test.ts`)
  - Real-time message updates
  - Tool approval integration
  - Message splitting for long content
  - Progress indicators

- **Tool Approval Handler Tests** (`services/tool-approval-handler.test.ts`)
  - Button-based approval workflow
  - Timeout handling
  - User permission validation
  - Whitelist management

- **Conversation Manager Tests** (`services/conversation-manager.test.ts`)
  - Context extraction from Discord interactions
  - Payload building for API requests
  - Input validation
  - Error handling

- **Command Tests**
  - `commands/utility/lenza-new.test.ts` - New conversation command
  - `commands/utility/lenza-resume.test.ts` - Resume conversation command

- **Error Handling Tests** (`errors/agent-error.test.ts`)
  - Custom error types
  - User-friendly error messages
  - Error context preservation

- **Network Error Handler Tests** (`utils/network-error-handler.test.ts`)
  - Retry logic with exponential backoff
  - Retryable vs non-retryable error classification
  - Concurrent operation handling

### Integration Tests
- **Command Integration Tests** (`test/integration/command-integration.test.ts`)
  - End-to-end command workflows
  - API integration testing
  - Tool approval workflows
  - Error handling scenarios

### Performance Tests
- **Streaming Performance Tests** (`test/performance/streaming-performance.test.ts`)
  - High-frequency update handling
  - Memory usage validation
  - Concurrent streaming scenarios
  - Large content processing

## Test Coverage Areas

### Core Functionality
✅ **Command Execution**
- Slash command structure validation
- Parameter extraction and validation
- Error handling and user feedback

✅ **API Communication**
- HTTP client with streaming support
- Request/response handling
- Network error recovery

✅ **Tool Approval System**
- Interactive button-based approvals
- Timeout mechanisms
- User permission validation

✅ **Conversation Management**
- Context extraction from Discord
- Payload building for backend API
- Input validation and sanitization

### Error Handling
✅ **Network Errors**
- Connection failures
- Timeout handling
- Retry logic with backoff

✅ **API Errors**
- HTTP status code handling
- Malformed response handling
- Service unavailability

✅ **User Input Errors**
- Invalid message content
- Missing required parameters
- Permission violations

### Performance & Reliability
✅ **Streaming Performance**
- High-frequency message updates
- Memory usage optimization
- Concurrent user handling

✅ **Message Handling**
- Long message splitting
- Discord API rate limiting
- Content formatting

## Running Tests

### All Tests
```bash
npm test
```

### Specific Test Suites
```bash
# Unit tests only
npx vitest --run "src/**/*.test.ts"

# Integration tests only
npx vitest --run "src/test/integration/**/*.test.ts"

# Performance tests only
npx vitest --run "src/test/performance/**/*.test.ts"
```

### With Coverage
```bash
npm run test:coverage
```

### Watch Mode
```bash
npm run test:watch
```

### Test UI
```bash
npm run test:ui
```

## Test Configuration

The test suite uses:
- **Vitest** - Fast unit test framework
- **jsdom** - DOM environment for Discord.js mocking
- **vi.mock()** - Comprehensive mocking system
- **Fake timers** - For testing timeout and retry logic

## Mocking Strategy

### External Dependencies
- **Discord.js** - Mocked interaction objects and API calls
- **fetch** - Mocked HTTP requests and responses
- **Timers** - Fake timers for timeout testing

### Internal Services
- Services are mocked at the module level
- Mock implementations preserve interface contracts
- Test-specific behavior injection via mock functions

## Test Data Management

### Mock Data
- Standardized mock Discord interactions
- Predefined API response patterns
- Error scenario templates

### Test Utilities
- Helper functions for common test setup
- Mock factory functions
- Assertion helpers for complex objects

## Validation Requirements

All tests validate:

### Requirement 1 - Resume Conversations
- ✅ Context extraction from Discord interactions
- ✅ API payload construction
- ✅ Streaming response handling
- ✅ Error scenarios and recovery

### Requirement 2 - New Conversations
- ✅ Fresh conversation initialization
- ✅ Message validation and processing
- ✅ Backend API integration
- ✅ User feedback mechanisms

### Requirement 3 - Tool Approval System
- ✅ Interactive approval workflows
- ✅ Button interaction handling
- ✅ Timeout and permission validation
- ✅ Whitelist management

### Requirement 4 - Tool Whitelist Management
- ✅ Whitelist CRUD operations
- ✅ Backend synchronization
- ✅ User preference persistence
- ✅ Validation and error handling

### Requirement 5 - Error Handling
- ✅ Network error recovery
- ✅ User-friendly error messages
- ✅ Graceful degradation
- ✅ Logging and monitoring

### Requirement 6 - User Experience
- ✅ Consistent command interfaces
- ✅ Helpful error messages
- ✅ Performance optimization
- ✅ Accessibility compliance

### Requirement 7 - Utility Commands
- ✅ Help and status commands
- ✅ Bot health monitoring
- ✅ User guidance systems
- ✅ Documentation integration

## Test Maintenance

### Adding New Tests
1. Follow existing naming conventions
2. Use appropriate test categories (unit/integration/performance)
3. Include comprehensive error scenarios
4. Validate against requirements

### Updating Tests
1. Update tests when implementation changes
2. Maintain mock compatibility
3. Preserve test coverage levels
4. Update documentation

### Test Quality Standards
- Each test should be independent
- Clear test descriptions and assertions
- Comprehensive error scenario coverage
- Performance benchmarks where applicable

## Continuous Integration

Tests are designed to run in CI/CD environments with:
- Deterministic timing (fake timers)
- No external dependencies
- Comprehensive error reporting
- Coverage threshold enforcement

## Known Limitations

### Current Test Gaps
- Some tests require implementation alignment
- Mock Discord.js interactions need refinement
- Performance tests need baseline establishment

### Future Improvements
- Add visual regression testing
- Implement load testing scenarios
- Add accessibility testing
- Enhance error scenario coverage

## Troubleshooting

### Common Issues
1. **Mock timing issues** - Use fake timers consistently
2. **Discord.js mocking** - Ensure complete interaction object mocking
3. **Async test failures** - Properly await all async operations
4. **Memory leaks** - Clean up timers and event listeners

### Debug Tips
- Use `--reporter=verbose` for detailed output
- Enable test debugging with `--inspect-brk`
- Use `console.log` sparingly in tests
- Leverage Vitest UI for interactive debugging