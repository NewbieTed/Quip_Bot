# Tool Update System Architecture

This document describes how the tool update system components are wired together to enable agent-driven tool synchronization via Redis.

## Component Overview

The tool update system consists of three main components that work together:

1. **ToolUpdateConsumer** - Redis message consumer
2. **ToolUpdateMessageHandler** - Message validation and routing
3. **ToolService** - Database operations for tool management

## Component Wiring

### ToolUpdateConsumer
- **Location**: `com.quip.backend.tool.consumer.ToolUpdateConsumer`
- **Dependencies**: 
  - `RedisService` - For Redis operations
  - `ObjectMapper` - For JSON deserialization
  - `ToolUpdateMessageHandler` - For message processing
- **Responsibilities**:
  - Polls Redis for tool update messages
  - Deserializes JSON messages
  - Delegates processing to ToolUpdateMessageHandler
  - Handles Redis connection errors with exponential backoff
  - Provides comprehensive logging and metrics

### ToolUpdateMessageHandler
- **Location**: `com.quip.backend.tool.handler.ToolUpdateMessageHandler`
- **Dependencies**:
  - `ToolService` - For database operations
- **Responsibilities**:
  - Validates tool update messages
  - Processes added and removed tools separately
  - Calls appropriate ToolService methods
  - Provides detailed logging and error handling

### ToolService (Enhanced)
- **Location**: `com.quip.backend.tool.service.ToolService`
- **New Methods**:
  - `createOrUpdateToolFromAgent(String toolName)` - Handles tool additions
  - `disableToolFromAgent(String toolName)` - Handles tool removals
- **Responsibilities**:
  - Creates new tool records for discovered tools
  - Enables existing tools that were previously disabled
  - Disables tools that are no longer available
  - Maintains audit trail by disabling rather than deleting

## Message Flow

1. **Agent Discovery**: Agent discovers tool changes and publishes to Redis
2. **Message Consumption**: ToolUpdateConsumer polls Redis and receives messages
3. **JSON Deserialization**: ObjectMapper converts JSON to ToolUpdateMessage objects
4. **Message Validation**: ToolUpdateMessageHandler validates message format and content
5. **Tool Processing**: Handler processes added/removed tools separately
6. **Database Updates**: ToolService creates/updates/disables tools in database
7. **Logging**: Comprehensive logging throughout the entire flow

## Configuration

The system is configured via application properties:

```yaml
app:
  tool-sync:
    consumer:
      enabled: ${TOOL_SYNC_CONSUMER_ENABLED:true}
      polling-timeout: ${TOOL_SYNC_POLLING_TIMEOUT:5}
```

## Error Handling

- **Redis Connection Failures**: Exponential backoff retry with detailed logging
- **Message Processing Failures**: Individual tool failures don't stop batch processing
- **Validation Failures**: Invalid messages are logged and skipped
- **Database Failures**: Proper exception handling with context logging

## Monitoring

The system provides extensive logging for monitoring:

- Message consumption rates and processing times
- Redis connection status changes
- Tool processing success/failure rates
- Validation failure details
- Performance metrics (processing time per message/tool)

## Testing

The system includes comprehensive unit tests:

- **ToolUpdateConsumerTest**: Tests message consumption and Redis integration
- **ToolUpdateMessageHandlerTest**: Tests message validation and processing
- **ToolServiceTest**: Tests database operations for agent-driven updates

All components are properly mocked to ensure isolated testing of each layer.