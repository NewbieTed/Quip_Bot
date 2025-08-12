# Sync Recovery Configuration Guide

This document describes all configuration options for the tool synchronization recovery system.

## Overview

The sync recovery system handles failures in Redis-based tool synchronization by providing HTTP-based fallback communication between the backend and agent. When Redis message processing fails, the backend can request a complete tool inventory resync directly from the agent.

## Backend Configuration

### Application Properties

All sync recovery configuration is located under the `app.tool-sync.recovery` section in `application.yml`:

```yaml
app:
  tool-sync:
    recovery:
      enabled: true                    # Enable/disable sync recovery feature
      timeout: 10000                   # HTTP request timeout in milliseconds
      max-retries: 3                   # Maximum retry attempts for HTTP requests
      initial-delay: 1000              # Initial retry delay in milliseconds
      
      # Failure detection thresholds
      failure-threshold:
        consecutive-failures: 5        # Consecutive failures before recovery
        invalid-messages: 10           # Invalid messages per minute threshold
        time-window: 60000             # Time window for failure detection (ms)
      
      # Agent HTTP endpoint configuration
      agent:
        base-url: http://localhost:5001              # Agent HTTP server base URL
        resync-endpoint: /api/tools/resync           # Resync endpoint path
        connect-timeout: 5000                        # Connection timeout in milliseconds
        read-timeout: 10000                          # Read timeout in milliseconds
```

### Environment Variables

All configuration properties can be overridden using environment variables:

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `TOOL_SYNC_RECOVERY_ENABLED` | `true` | Enable/disable sync recovery |
| `TOOL_SYNC_RECOVERY_TIMEOUT` | `10000` | HTTP request timeout (ms) |
| `TOOL_SYNC_RECOVERY_MAX_RETRIES` | `3` | Maximum retry attempts |
| `TOOL_SYNC_RECOVERY_INITIAL_DELAY` | `1000` | Initial retry delay (ms) |
| `TOOL_SYNC_FAILURE_CONSECUTIVE` | `5` | Consecutive failures threshold |
| `TOOL_SYNC_FAILURE_INVALID_MESSAGES` | `10` | Invalid messages threshold |
| `TOOL_SYNC_FAILURE_TIME_WINDOW` | `60000` | Failure detection time window (ms) |
| `TOOL_SYNC_AGENT_URL` | `http://localhost:5001` | Agent base URL |
| `TOOL_SYNC_AGENT_RESYNC_ENDPOINT` | `/api/tools/resync` | Resync endpoint path |
| `TOOL_SYNC_AGENT_CONNECT_TIMEOUT` | `5000` | Connection timeout (ms) |
| `TOOL_SYNC_AGENT_READ_TIMEOUT` | `10000` | Read timeout (ms) |

### Environment-Specific Configuration

#### Development (`application-dev.yml`)
- Lower failure thresholds for faster testing
- Shorter timeouts for quicker feedback
- Enhanced debug logging enabled
- Agent URL: `http://localhost:5001`

#### Production (`application-prod.yml`)
- Higher failure thresholds for stability
- Standard timeouts for reliability
- Reduced logging verbosity
- Agent URL: `http://agent-service:5001` (Docker service name)

## Agent Configuration

### YAML Configuration

Tool sync HTTP server configuration in `config.yaml`:

```yaml
tool_sync:
  enabled: true
  discovery_on_startup: true
  http_server:
    enabled: true                      # Enable/disable HTTP sync endpoints
    host: "0.0.0.0"                   # HTTP server host
    port: 5001                        # HTTP server port
    timeout: 10                       # Request timeout in seconds
    discovery:
      timeout: 10                     # Tool discovery timeout in seconds
      retry_attempts: 2               # Retry attempts for failed discovery
      retry_delay: 1                  # Delay between retries in seconds
```

### Environment Variables

Agent configuration can be overridden using environment variables:

| Environment Variable | Default Value | Description |
|---------------------|---------------|-------------|
| `TOOL_SYNC_HTTP_ENABLED` | `true` | Enable HTTP sync endpoints |
| `TOOL_SYNC_HTTP_HOST` | `0.0.0.0` | HTTP server host |
| `TOOL_SYNC_HTTP_PORT` | `5001` | HTTP server port |
| `TOOL_SYNC_HTTP_TIMEOUT` | `10` | Request timeout (seconds) |
| `TOOL_DISCOVERY_TIMEOUT` | `10` | Tool discovery timeout (seconds) |
| `TOOL_DISCOVERY_RETRY_ATTEMPTS` | `2` | Discovery retry attempts |
| `TOOL_DISCOVERY_RETRY_DELAY` | `1` | Discovery retry delay (seconds) |

## Configuration Examples

### Docker Compose

```yaml
services:
  backend:
    environment:
      - TOOL_SYNC_RECOVERY_ENABLED=true
      - TOOL_SYNC_AGENT_URL=http://agent:5001
      - TOOL_SYNC_RECOVERY_TIMEOUT=15000
      - TOOL_SYNC_RECOVERY_MAX_RETRIES=5
  
  agent:
    environment:
      - TOOL_SYNC_HTTP_ENABLED=true
      - TOOL_SYNC_HTTP_PORT=5001
      - TOOL_DISCOVERY_TIMEOUT=15
    ports:
      - "5001:5001"
```

### Kubernetes

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: sync-recovery-config
data:
  TOOL_SYNC_RECOVERY_ENABLED: "true"
  TOOL_SYNC_AGENT_URL: "http://agent-service:5001"
  TOOL_SYNC_RECOVERY_TIMEOUT: "10000"
  TOOL_SYNC_RECOVERY_MAX_RETRIES: "3"
  TOOL_SYNC_HTTP_ENABLED: "true"
  TOOL_SYNC_HTTP_PORT: "5001"
  TOOL_DISCOVERY_TIMEOUT: "10"
```

## Feature Flags

### Disabling Sync Recovery

To completely disable sync recovery:

**Backend:**
```yaml
app:
  tool-sync:
    recovery:
      enabled: false
```

**Agent:**
```yaml
tool_sync:
  http_server:
    enabled: false
```

### Partial Disabling

You can disable specific components:

- Set `TOOL_SYNC_RECOVERY_ENABLED=false` to disable backend recovery logic
- Set `TOOL_SYNC_HTTP_ENABLED=false` to disable agent HTTP endpoints
- Set `TOOL_SYNC_RECOVERY_MAX_RETRIES=0` to disable retry logic

## Monitoring Configuration

### Logging Levels

Configure logging for sync recovery components:

```yaml
logging:
  level:
    com.quip.backend.tool.sync: DEBUG
    com.quip.backend.tool.consumer: DEBUG
    com.quip.backend.tool.handler: DEBUG
```

### Metrics

Sync recovery metrics are automatically enabled when the feature is active. No additional configuration required.

## Troubleshooting

### Common Configuration Issues

1. **Agent Unreachable**: Verify `TOOL_SYNC_AGENT_URL` points to correct agent endpoint
2. **Timeout Issues**: Increase `TOOL_SYNC_RECOVERY_TIMEOUT` for slow networks
3. **Too Many Retries**: Adjust `TOOL_SYNC_RECOVERY_MAX_RETRIES` based on network reliability
4. **False Positives**: Increase failure thresholds in unstable environments

### Validation

Test your configuration:

1. Check backend logs for "Sync recovery configuration loaded" messages
2. Verify agent HTTP endpoint is accessible: `curl http://agent:5001/api/tools/resync`
3. Monitor sync recovery metrics in application metrics endpoint

## Security Considerations

### Network Security
- Use internal service names in production (e.g., `http://agent-service:5001`)
- Avoid exposing agent HTTP ports externally
- Consider using HTTPS for production deployments

### Authentication
- Current implementation uses internal service communication
- Add authentication headers if required by your security policy
- Consider mutual TLS for service-to-service communication

## Performance Tuning

### Timeout Configuration
- **Development**: Use shorter timeouts (5-10 seconds) for faster feedback
- **Production**: Use longer timeouts (10-15 seconds) for reliability
- **High-latency networks**: Increase timeouts accordingly

### Retry Configuration
- **Stable networks**: Use fewer retries (2-3 attempts)
- **Unstable networks**: Use more retries (3-5 attempts)
- **Critical systems**: Increase retry attempts but monitor performance impact

### Failure Thresholds
- **Development**: Lower thresholds for faster testing
- **Production**: Higher thresholds to avoid false positives
- **High-volume systems**: Adjust time windows and thresholds accordingly