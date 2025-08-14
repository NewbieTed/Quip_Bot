# Discord Bot Frontend - Core Infrastructure

This document describes the core infrastructure components implemented for the Discord bot frontend enhancement.

## Architecture Overview

The core infrastructure provides a robust foundation for communicating with the Spring Boot backend and handling streaming AI responses. It includes:

- **Configuration Management**: Environment-based configuration with sensible defaults
- **Error Handling**: Comprehensive error handling with retry logic and user-friendly messages
- **HTTP Client**: Backend API client with streaming response support
- **Response Handling**: Real-time Discord message updates for streaming responses

## Components

### Configuration (`src/config/`)

- `bot-config.ts`: Centralized configuration management with environment variable support

**Key Configuration Options:**
- `BACKEND_URL`: Backend API base URL
- `BACKEND_TIMEOUT`: Request timeout in milliseconds
- `MAX_MESSAGE_LENGTH`: Discord message length limit
- `APPROVAL_TIMEOUT`: Tool approval timeout in seconds
- `RETRY_ATTEMPTS`: Number of retry attempts for failed requests
- `RETRY_DELAY_MS`: Base delay between retries

### Error Handling (`src/errors/`)

- `agent-error.ts`: Custom error types and user-friendly error messages

**Error Types:**
- `network`: Network connectivity issues
- `timeout`: Request timeouts
- `parsing`: JSON parsing failures
- `api`: Backend API errors
- `unknown`: Unexpected errors

### Services (`src/services/`)

#### BackendApiClient
- HTTP client for communicating with Spring Boot backend
- Streaming response support with async generators
- Automatic retry logic with exponential backoff
- Proper timeout handling and error conversion

**Key Methods:**
- `invokeAgent()`: Continue existing conversation
- `invokeNewAgent()`: Start new conversation
- `sendApprovalDecision()`: Send tool approval decisions
- `updateToolWhitelist()`: Update user tool preferences

#### StreamingResponseHandler
- Real-time Discord message updates
- Message length handling and splitting
- Progress indicators and formatting
- Error recovery with partial content preservation

#### AgentService
- High-level service combining API client and streaming handler
- Simplified interface for command implementations
- Automatic error handling and user feedback

### Utilities (`src/utils/`)

#### NetworkErrorHandler
- Retry logic with exponential backoff and jitter
- Error type classification for retry decisions
- Configurable retry attempts and delays

## Usage Examples

### Basic Agent Invocation

```typescript
import { AgentService } from '../services';

const agentService = new AgentService();

// In a Discord command handler
await agentService.invokeAgent(interaction, userMessage);
```

### Direct API Client Usage

```typescript
import { BackendApiClient } from '../services';

const client = new BackendApiClient();

// Stream responses
for await (const response of client.invokeAgent(request)) {
  console.log('Received:', response.content);
}
```

### Custom Error Handling

```typescript
import { AgentError, getUserFriendlyErrorMessage } from '../errors';

try {
  // Some operation
} catch (error) {
  if (error instanceof AgentError) {
    const userMessage = getUserFriendlyErrorMessage(error);
    await interaction.reply(userMessage);
  }
}
```

## Environment Configuration

Create a `.env` file in the frontend directory:

```env
BACKEND_URL=http://quip-backend-app:8080
BACKEND_TIMEOUT=30000
MAX_MESSAGE_LENGTH=2000
APPROVAL_TIMEOUT=60
RETRY_ATTEMPTS=3
RETRY_DELAY_MS=1000
```

## Testing

Run the infrastructure tests:

```bash
npx ts-node src/test/infrastructure-test.ts
```

## Backend API Integration

The infrastructure is designed to work with the following backend endpoints:

- `POST /assistant/invoke` - Continue existing conversation
- `POST /assistant/approve` - Send approval decisions
- `POST /tool-whitelist/update` - Update tool whitelist

**Request Format:**
```json
{
  "message": "User message",
  "channelId": 123456789,
  "memberId": 987654321
}
```

**Streaming Response Format:**
```json
{
  "content": "Response content",
  "type": "update|progress|interrupt",
  "tool_name": "optional_tool_name"
}
```

## Next Steps

This core infrastructure supports the implementation of:

1. `/lenza-new` and `/lenza-resume` commands
2. Tool approval workflows
3. Tool whitelist management
4. Enhanced error handling and user experience

The infrastructure is designed to be extensible and maintainable, providing a solid foundation for the Discord bot's AI agent integration.