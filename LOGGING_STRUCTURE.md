# Logging Structure

This document outlines the centralized logging structure implemented across all projects in the Quip Bot workspace.

## Directory Structure

Each project now has its own `logs/` directory for organized log file management:

```
Quip_Bot/
├── logs/                    # Root level logs (if any)
├── agent/
│   └── logs/
│       └── agent.log        # Agent service logs
├── backend/
│   └── logs/
│       └── backend.log      # Spring Boot backend logs
├── frontend/
│   └── logs/
│       └── frontend.log     # Discord bot logs
└── mcp-server/
    └── logs/
        └── mcp-server.log   # MCP server logs
```

## Project-Specific Configurations

### Agent (Python/FastAPI)
- **Location**: `agent/logs/agent.log`
- **Configuration**: Updated `agent/src/agent/main.py` to use `logs/agent.log`
- **Format**: `%(asctime)s - %(name)s - %(levelname)s - %(message)s`
- **Handlers**: Console + File

### Backend (Java/Spring Boot)
- **Location**: `backend/logs/backend.log`
- **Configuration**: Updated `backend/src/main/resources/application.yml`
- **Format**: `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n`
- **Levels**: INFO for application, WARN for frameworks

### Frontend (TypeScript/Discord.js)
- **Location**: `frontend/logs/frontend.log`
- **Configuration**: New logger utility at `frontend/src/utils/logger.ts`
- **Usage**: Import `{ logger }` and use `logger.info()`, `logger.error()`, etc.
- **Features**: Automatic log directory creation, console + file output

### MCP Server (Python)
- **Location**: `mcp-server/logs/mcp-server.log`
- **Configuration**: Updated `mcp-server/src/mcp_server/config.py`
- **Format**: `%(asctime)s - %(name)s - %(levelname)s - %(message)s`
- **Handlers**: Console + File (when enabled in config)

## Git Configuration

Updated `.gitignore` to properly handle the new log structure:
- `*.log` - Ignores all log files
- `logs/` - Ignores logs directories
- `*/logs/` - Ignores logs directories in subdirectories
- `**/logs/*.log` - Ignores log files in any logs directory

## Benefits

1. **Organization**: Each project's logs are contained within their own directory
2. **Consistency**: Standardized logging format across all projects
3. **Maintenance**: Easy to clean up logs per project
4. **Development**: Clear separation of concerns for debugging
5. **Deployment**: Logs directories can be easily mounted in containers

## Usage Examples

### Agent/MCP Server (Python)
```python
import logging
logger = logging.getLogger(__name__)
logger.info("Application started")
logger.error("An error occurred", exc_info=True)
```

### Backend (Java/Spring Boot)
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(ClassName.class);
logger.info("Service initialized");
logger.error("Error processing request", exception);
```

### Frontend (TypeScript)
```typescript
import { logger } from './utils/logger';
logger.info("Bot started successfully");
logger.error("Failed to process command", error);
```

## Migration Notes

- Existing log files have been moved to their respective `logs/` directories
- All projects now create their logs directories automatically if they don't exist
- No breaking changes to existing functionality - only improved organization