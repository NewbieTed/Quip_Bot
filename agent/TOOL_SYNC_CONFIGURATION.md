# Agent Tool Sync Configuration Guide

This document describes the configuration options for the agent's tool synchronization HTTP server.

## Overview

The agent provides HTTP endpoints for tool synchronization recovery. When Redis-based tool updates fail, the backend can request a complete tool inventory directly from the agent via HTTP.

## Configuration Structure

### YAML Configuration

Tool sync configuration in `config.yaml`:

```yaml
tool_sync:
  enabled: true                        # Enable/disable tool synchronization
  discovery_on_startup: true           # Perform tool discovery on startup
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

All configuration can be overridden using environment variables:

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `TOOL_SYNC_HTTP_ENABLED` | `true` | Enable HTTP sync endpoints |
| `TOOL_SYNC_HTTP_HOST` | `0.0.0.0` | HTTP server bind host |
| `TOOL_SYNC_HTTP_PORT` | `5001` | HTTP server port |
| `TOOL_SYNC_HTTP_TIMEOUT` | `10` | Request timeout (seconds) |
| `TOOL_DISCOVERY_TIMEOUT` | `10` | Tool discovery timeout (seconds) |
| `TOOL_DISCOVERY_RETRY_ATTEMPTS` | `2` | Discovery retry attempts |
| `TOOL_DISCOVERY_RETRY_DELAY` | `1` | Retry delay (seconds) |

## HTTP Endpoints

### POST /api/tools/resync

Requests a complete tool inventory from the agent.

**Request Body:**
```json
{
  "requestId": "uuid-v4",
  "timestamp": "2025-01-28T10:30:00Z",
  "reason": "message_processing_failure"
}
```

**Response Body:**
```json
{
  "requestId": "uuid-v4",
  "timestamp": "2025-01-28T10:30:05Z",
  "currentTools": ["tool1", "tool2", "tool3"],
  "discoveryTimestamp": "2025-01-28T10:30:04Z"
}
```

**Status Codes:**
- `200 OK`: Successful tool inventory response
- `408 Request Timeout`: Tool discovery timed out
- `500 Internal Server Error`: Tool discovery failed

## Configuration Examples

### Development Environment

```yaml
tool_sync:
  http_server:
    enabled: true
    host: "localhost"
    port: 5001
    timeout: 15                       # Longer timeout for debugging
    discovery:
      timeout: 15                     # Longer discovery timeout
      retry_attempts: 1               # Fewer retries for faster feedback
      retry_delay: 0.5
```

### Production Environment

```yaml
tool_sync:
  http_server:
    enabled: true
    host: "0.0.0.0"
    port: 5001
    timeout: 10
    discovery:
      timeout: 10
      retry_attempts: 3               # More retries for reliability
      retry_delay: 2
```

### Docker Compose

```yaml
services:
  agent:
    environment:
      - TOOL_SYNC_HTTP_ENABLED=true
      - TOOL_SYNC_HTTP_PORT=5001
      - TOOL_DISCOVERY_TIMEOUT=10
    ports:
      - "5001:5001"                   # Expose for backend communication
```

### Kubernetes

```yaml
apiVersion: v1
kind: Service
metadata:
  name: agent-service
spec:
  selector:
    app: agent
  ports:
    - name: http
      port: 5001
      targetPort: 5001
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agent
spec:
  template:
    spec:
      containers:
      - name: agent
        env:
        - name: TOOL_SYNC_HTTP_ENABLED
          value: "true"
        - name: TOOL_SYNC_HTTP_PORT
          value: "5001"
        - name: TOOL_DISCOVERY_TIMEOUT
          value: "10"
        ports:
        - containerPort: 5001
```

## Feature Flags

### Disabling HTTP Server

To disable the HTTP sync server completely:

```yaml
tool_sync:
  http_server:
    enabled: false
```

Or via environment variable:
```bash
TOOL_SYNC_HTTP_ENABLED=false
```

### Disabling Tool Sync Entirely

```yaml
tool_sync:
  enabled: false
```

## Performance Tuning

### Timeout Configuration

**Tool Discovery Timeout:**
- **Fast systems**: 5-10 seconds
- **Slow systems**: 15-30 seconds
- **MCP-heavy setups**: 20-60 seconds

**HTTP Request Timeout:**
- Should be longer than discovery timeout
- Add 5-10 seconds buffer for processing
- Consider network latency

### Retry Configuration

**Retry Attempts:**
- **Development**: 1-2 attempts for faster feedback
- **Production**: 2-3 attempts for reliability
- **Unstable MCP servers**: 3-5 attempts

**Retry Delay:**
- **Fast networks**: 0.5-1 second
- **Slow networks**: 1-2 seconds
- **High-latency MCP**: 2-5 seconds

## Monitoring and Logging

### Log Configuration

Enable debug logging for tool sync:

```python
logging.getLogger('agent.api.tool_sync_controller').setLevel(logging.DEBUG)
logging.getLogger('agent.tools.discovery').setLevel(logging.DEBUG)
```

### Log Events

The system logs the following events:

- `resync_request_received`: When resync request is received
- `resync_request_completed`: When resync completes successfully
- `resync_request_timeout`: When tool discovery times out
- `resync_request_error`: When tool discovery fails
- `tool_discovery_success`: When tool discovery succeeds
- `tool_discovery_timeout`: When tool discovery times out
- `tool_discovery_error`: When tool discovery fails

### Health Checks

Check if the HTTP server is running:

```bash
curl -f http://localhost:5001/health || echo "Agent HTTP server not responding"
```

Test the resync endpoint:

```bash
curl -X POST http://localhost:5001/api/tools/resync \
  -H "Content-Type: application/json" \
  -d '{"requestId":"test","timestamp":"2025-01-28T10:00:00Z","reason":"test"}'
```

## Security Considerations

### Network Security

- Bind to `0.0.0.0` only in containerized environments
- Use `localhost` or specific IPs for development
- Consider firewall rules for production

### Authentication

Current implementation has no authentication. Consider adding:

- API key authentication
- JWT token validation
- IP allowlisting
- Mutual TLS

### Rate Limiting

Consider implementing rate limiting for production:

- Limit requests per minute per IP
- Implement circuit breaker patterns
- Add request queuing for high load

## Troubleshooting

### Common Issues

**HTTP Server Not Starting:**
- Check port availability: `netstat -ln | grep 5001`
- Verify host binding configuration
- Check for permission issues on privileged ports

**Tool Discovery Timeouts:**
- Increase `TOOL_DISCOVERY_TIMEOUT`
- Check MCP server connectivity
- Review tool discovery logs

**Connection Refused:**
- Verify agent is running and HTTP server is enabled
- Check firewall rules
- Confirm port mapping in Docker/Kubernetes

### Debug Commands

```bash
# Check if agent HTTP server is running
curl -v http://localhost:5001/health

# Test tool discovery endpoint
curl -X POST http://localhost:5001/api/tools/resync \
  -H "Content-Type: application/json" \
  -d '{"requestId":"debug","timestamp":"2025-01-28T10:00:00Z","reason":"debug"}'

# Check agent logs
tail -f logs/agent.log | grep -E "(tool_sync|resync|discovery)"
```

### Performance Testing

```bash
# Test discovery performance
time curl -X POST http://localhost:5001/api/tools/resync \
  -H "Content-Type: application/json" \
  -d '{"requestId":"perf-test","timestamp":"2025-01-28T10:00:00Z","reason":"performance_test"}'

# Load testing (requires Apache Bench)
ab -n 10 -c 2 -T application/json -p resync_payload.json http://localhost:5001/api/tools/resync
```

## Migration Guide

### From Previous Versions

If upgrading from a version without HTTP sync:

1. Add HTTP server configuration to `config.yaml`
2. Set environment variables in deployment
3. Update firewall rules to allow port 5001
4. Test connectivity from backend to agent
5. Monitor logs for any configuration issues

### Configuration Validation

Validate your configuration:

```python
from src.config.config_loader import Config

# Check tool sync configuration
config = Config.get_tool_sync_config()
print(f"HTTP Server Enabled: {config['http_server']['enabled']}")
print(f"HTTP Server Port: {config['http_server']['port']}")
print(f"Discovery Timeout: {config['http_server']['discovery']['timeout']}")
```