# Environment-Specific Configuration Guide

This document provides detailed configuration examples for different deployment environments.

## Environment Overview

The agent-tool-sync feature supports three main deployment environments:

- **Development**: Local development with enhanced debugging
- **Staging**: Pre-production testing with moderate settings
- **Production**: Optimized for stability and performance

## Development Environment

### Characteristics
- Enhanced logging for debugging
- Shorter timeouts for faster feedback
- Lower failure thresholds for quick testing
- Local service URLs

### Configuration Files

**Agent Configuration (`config.yaml` or environment variables):**
```yaml
# Development-specific settings
tool_sync:
  http_server:
    timeout: 15  # Longer timeout for debugging
    discovery:
      timeout: 15
      retry_attempts: 1  # Fewer retries for faster feedback
      retry_delay: 0.5

logging:
  level: INFO
  loggers:
    tool_sync: DEBUG
    tool_discovery: DEBUG
    redis: DEBUG
```

**Backend Configuration (`application-dev.yml`):**
```yaml
app:
  tool-sync:
    recovery:
      timeout: 15000
      max-retries: 2
      failure-threshold:
        consecutive-failures: 3
        invalid-messages: 5
        time-window: 30000

logging:
  level:
    com.quip.backend.tool.sync: DEBUG
    com.quip.backend.tool.consumer: DEBUG
    com.quip.backend.tool.handler: DEBUG
```

### Environment Variables

```bash
# Development .env file
REDIS_HOST=localhost
TOOL_SYNC_AGENT_URL=http://localhost:5001

# Enhanced debugging
LOG_LEVEL_TOOL_SYNC=DEBUG
LOG_LEVEL_TOOL_CONSUMER=DEBUG
LOG_LEVEL_TOOL_HANDLER=DEBUG
LOG_LEVEL_REDIS=DEBUG

# Faster feedback settings
TOOL_SYNC_RECOVERY_TIMEOUT=15000
TOOL_DISCOVERY_TIMEOUT=15
TOOL_SYNC_FAILURE_CONSECUTIVE=3
TOOL_SYNC_FAILURE_TIME_WINDOW=30000
```

### Docker Compose

```yaml
services:
  backend:
    environment:
      - LOG_LEVEL_TOOL_SYNC=DEBUG
      - TOOL_SYNC_RECOVERY_TIMEOUT=15000
      - TOOL_SYNC_FAILURE_CONSECUTIVE=3
  
  agent:
    environment:
      - LOG_LEVEL_TOOL_SYNC=DEBUG
      - TOOL_DISCOVERY_TIMEOUT=15
      - TOOL_DISCOVERY_RETRY_ATTEMPTS=1
    ports:
      - "5001:5001"  # Expose for local testing
```

## Staging Environment

### Characteristics
- Similar to production but with enhanced debugging
- Moderate timeouts for testing
- Detailed logging for troubleshooting
- Internal service names

### Configuration Files

**Agent Configuration (`config-staging.yaml`):**
```yaml
tool_sync:
  http_server:
    timeout: 15  # Slightly longer for staging
    discovery:
      timeout: 15
      retry_attempts: 2
      retry_delay: 1

logging:
  level: INFO
  loggers:
    tool_sync: INFO
    redis: INFO
```

**Backend Configuration (`application-staging.yml`):**
```yaml
app:
  tool-sync:
    recovery:
      timeout: 15000
      max-retries: 2
      failure-threshold:
        consecutive-failures: 3
        time-window: 30000
      agent:
        base-url: http://quip-agent-staging:5001

logging:
  level:
    com.quip.backend.tool.sync: DEBUG
    com.quip.backend.tool.consumer: DEBUG
```

### Environment Variables

```bash
# Staging environment variables
REDIS_HOST=redis-staging
TOOL_SYNC_AGENT_URL=http://quip-agent-staging:5001

# Moderate debugging
LOG_LEVEL_TOOL_SYNC=DEBUG
LOG_LEVEL_TOOL_CONSUMER=DEBUG
LOG_LEVEL_REDIS=INFO

# Staging-optimized settings
TOOL_SYNC_RECOVERY_TIMEOUT=15000
TOOL_DISCOVERY_TIMEOUT=15
TOOL_SYNC_FAILURE_CONSECUTIVE=3
TOOL_SYNC_FAILURE_TIME_WINDOW=30000
```

### Kubernetes Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: staging-config
data:
  REDIS_HOST: "redis-staging-service"
  TOOL_SYNC_AGENT_URL: "http://agent-staging-service:5001"
  LOG_LEVEL_TOOL_SYNC: "DEBUG"
  TOOL_SYNC_RECOVERY_TIMEOUT: "15000"
  TOOL_DISCOVERY_TIMEOUT: "15"
  TOOL_SYNC_FAILURE_CONSECUTIVE: "3"
```

## Production Environment

### Characteristics
- Optimized for stability and performance
- Standard timeouts for reliability
- Reduced logging verbosity
- Production service names and security

### Configuration Files

**Agent Configuration (`config-prod.yaml`):**
```yaml
tool_sync:
  http_server:
    timeout: 10  # Standard production timeout
    discovery:
      timeout: 10
      retry_attempts: 3  # More retries for reliability
      retry_delay: 2

redis:
  password: ${REDIS_PASSWORD}  # Required in production
  retry:
    max_attempts: 5
    base_delay: 2.0
    max_delay: 60.0

logging:
  level: INFO
  loggers:
    tool_sync: INFO
    redis: WARN  # Less verbose in production
```

**Backend Configuration (`application-prod.yml`):**
```yaml
app:
  tool-sync:
    recovery:
      timeout: 10000
      max-retries: 3
      failure-threshold:
        consecutive-failures: 5  # Higher for stability
        invalid-messages: 10
        time-window: 60000
      agent:
        base-url: http://agent-service:5001

logging:
  level:
    com.quip.backend.tool.sync: INFO
    com.quip.backend.redis: INFO
    io.lettuce.core: WARN
```

### Environment Variables

```bash
# Production environment variables
REDIS_HOST=redis-cluster.production.local
REDIS_PASSWORD=your_production_redis_password
TOOL_SYNC_AGENT_URL=http://agent-service:5001

# Production logging levels
LOG_LEVEL_TOOL_SYNC=INFO
LOG_LEVEL_REDIS=INFO
LOG_LEVEL_LETTUCE=WARN

# Production-optimized settings
TOOL_SYNC_RECOVERY_TIMEOUT=10000
TOOL_DISCOVERY_TIMEOUT=10
TOOL_SYNC_FAILURE_CONSECUTIVE=5
TOOL_SYNC_FAILURE_INVALID_MESSAGES=10
TOOL_SYNC_FAILURE_TIME_WINDOW=60000

# Production retry settings
REDIS_RETRY_MAX_ATTEMPTS=5
REDIS_RETRY_BASE_DELAY=2.0
REDIS_RETRY_MAX_DELAY=60.0
```

### Kubernetes Production Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: production-config
data:
  REDIS_HOST: "redis-cluster-service"
  TOOL_SYNC_AGENT_URL: "http://agent-service:5001"
  LOG_LEVEL_TOOL_SYNC: "INFO"
  LOG_LEVEL_REDIS: "INFO"
  LOG_LEVEL_LETTUCE: "WARN"
  TOOL_SYNC_RECOVERY_TIMEOUT: "10000"
  TOOL_DISCOVERY_TIMEOUT: "10"
  TOOL_SYNC_FAILURE_CONSECUTIVE: "5"
  TOOL_SYNC_FAILURE_INVALID_MESSAGES: "10"
  REDIS_RETRY_MAX_ATTEMPTS: "5"
  REDIS_RETRY_BASE_DELAY: "2.0"
  REDIS_RETRY_MAX_DELAY: "60.0"

---
apiVersion: v1
kind: Secret
metadata:
  name: production-secrets
type: Opaque
data:
  REDIS_PASSWORD: <base64-encoded-password>
  OPENAI_API_KEY: <base64-encoded-api-key>
```

## Configuration Comparison Table

| Setting | Development | Staging | Production |
|---------|-------------|---------|------------|
| **Timeouts** | | | |
| Tool Discovery | 15s | 15s | 10s |
| HTTP Recovery | 15s | 15s | 10s |
| Redis Operations | 5s | 4s | 3s |
| **Retry Settings** | | | |
| Discovery Retries | 1 | 2 | 3 |
| Recovery Retries | 2 | 2 | 3 |
| Redis Retries | 3 | 3 | 5 |
| **Failure Thresholds** | | | |
| Consecutive Failures | 3 | 3 | 5 |
| Invalid Messages | 5 | 5 | 10 |
| Time Window | 30s | 30s | 60s |
| **Logging Levels** | | | |
| Tool Sync | DEBUG | DEBUG | INFO |
| Redis | DEBUG | INFO | INFO |
| Lettuce | INFO | INFO | WARN |
| **Security** | | | |
| Redis Password | Optional | Required | Required |
| SSL/TLS | No | Optional | Yes |
| Port Exposure | Yes | Limited | No |

## Migration Between Environments

### Development to Staging

1. Update service URLs from localhost to internal service names
2. Reduce logging verbosity slightly
3. Add Redis authentication
4. Remove external port exposure

### Staging to Production

1. Update to production service names
2. Reduce logging verbosity further
3. Increase failure thresholds for stability
4. Enable SSL/TLS for Redis
5. Add production monitoring and alerting

### Configuration Validation

Before deploying to any environment, validate:

1. **Service Connectivity**: Ensure all service URLs are accessible
2. **Redis Authentication**: Verify Redis password is set correctly
3. **Timeout Values**: Confirm timeouts are appropriate for network latency
4. **Logging Levels**: Check log levels won't overwhelm log storage
5. **Resource Limits**: Ensure container resource limits are adequate

### Environment-Specific Testing

**Development:**
```bash
# Test tool sync endpoint
curl -X POST http://localhost:5001/api/tools/resync \
  -H "Content-Type: application/json" \
  -d '{"requestId":"test","timestamp":"2025-01-28T10:00:00Z","reason":"test"}'

# Check Redis connectivity
redis-cli -h localhost ping
```

**Staging:**
```bash
# Test from within cluster
kubectl exec -it backend-pod -- curl -X POST http://agent-service:5001/api/tools/resync \
  -H "Content-Type: application/json" \
  -d '{"requestId":"test","timestamp":"2025-01-28T10:00:00Z","reason":"test"}'
```

**Production:**
```bash
# Health check only (no direct testing in production)
kubectl exec -it backend-pod -- curl -f http://agent-service:5001/health
```

This guide ensures proper configuration across all deployment environments while maintaining appropriate security and performance characteristics for each.