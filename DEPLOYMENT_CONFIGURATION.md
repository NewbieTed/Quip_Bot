# Deployment Configuration Guide

This document provides comprehensive configuration options for deploying the agent-tool-sync feature across different environments.

## Overview

The agent-tool-sync feature requires configuration for both the agent and backend services. This guide covers all environment variables, configuration files, and deployment-specific settings.

## Environment Variables Reference

### Redis Configuration

| Variable | Default | Description | Used By |
|----------|---------|-------------|---------|
| `REDIS_ENABLED` | `true` | Enable/disable Redis integration | Backend |
| `REDIS_HOST` | `localhost` | Redis server hostname | Both |
| `REDIS_PORT` | `6379` | Redis server port | Both |
| `REDIS_PASSWORD` | `` | Redis authentication password | Both |
| `REDIS_DATABASE` | `0` | Redis database number | Backend |
| `REDIS_DB` | `0` | Redis database number | Agent |
| `REDIS_TIMEOUT` | `2000ms` | Redis operation timeout | Backend |
| `REDIS_CONNECT_TIMEOUT` | `2000ms` | Redis connection timeout | Backend |

### Redis Connection Pool (Backend)

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_POOL_MAX_ACTIVE` | `8` | Maximum active connections |
| `REDIS_POOL_MAX_IDLE` | `8` | Maximum idle connections |
| `REDIS_POOL_MIN_IDLE` | `0` | Minimum idle connections |
| `REDIS_POOL_MAX_WAIT` | `-1ms` | Maximum wait time for connection |

### Redis Retry Configuration

| Variable | Default | Description | Used By |
|----------|---------|-------------|---------|
| `REDIS_RETRY_MAX_ATTEMPTS` | `3` | Maximum retry attempts | Both |
| `REDIS_RETRY_BASE_DELAY` | `1.0` | Base retry delay (seconds) | Agent |
| `REDIS_RETRY_MAX_DELAY` | `30.0` | Maximum retry delay (seconds) | Agent |
| `REDIS_RETRY_DELAY` | `1000` | Retry delay (milliseconds) | Backend |

### Tool Synchronization Configuration

| Variable | Default | Description | Used By |
|----------|---------|-------------|---------|
| `TOOL_SYNC_CONSUMER_ENABLED` | `true` | Enable tool update consumer | Backend |
| `TOOL_SYNC_POLLING_TIMEOUT` | `5` | Redis polling timeout (seconds) | Backend |

### Tool Sync Recovery Configuration (Backend)

| Variable | Default | Description |
|----------|---------|-------------|
| `TOOL_SYNC_RECOVERY_ENABLED` | `true` | Enable sync recovery feature |
| `TOOL_SYNC_RECOVERY_TIMEOUT` | `10000` | HTTP request timeout (ms) |
| `TOOL_SYNC_RECOVERY_MAX_RETRIES` | `3` | Maximum retry attempts |
| `TOOL_SYNC_RECOVERY_INITIAL_DELAY` | `1000` | Initial retry delay (ms) |
| `TOOL_SYNC_FAILURE_CONSECUTIVE` | `5` | Consecutive failures threshold |
| `TOOL_SYNC_FAILURE_INVALID_MESSAGES` | `10` | Invalid messages threshold |
| `TOOL_SYNC_FAILURE_TIME_WINDOW` | `60000` | Failure detection window (ms) |

### Agent HTTP Endpoint Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `TOOL_SYNC_AGENT_URL` | `http://localhost:5001` | Agent HTTP server base URL |
| `TOOL_SYNC_AGENT_RESYNC_ENDPOINT` | `/api/tools/resync` | Resync endpoint path |
| `TOOL_SYNC_AGENT_CONNECT_TIMEOUT` | `5000` | Connection timeout (ms) |
| `TOOL_SYNC_AGENT_READ_TIMEOUT` | `10000` | Read timeout (ms) |

### Agent HTTP Server Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `TOOL_SYNC_HTTP_ENABLED` | `true` | Enable HTTP sync endpoints |
| `TOOL_SYNC_HTTP_HOST` | `0.0.0.0` | HTTP server bind host |
| `TOOL_SYNC_HTTP_PORT` | `5001` | HTTP server port |
| `TOOL_SYNC_HTTP_TIMEOUT` | `10` | Request timeout (seconds) |

### Tool Discovery Configuration (Agent)

| Variable | Default | Description |
|----------|---------|-------------|
| `TOOL_DISCOVERY_TIMEOUT` | `10` | Tool discovery timeout (seconds) |
| `TOOL_DISCOVERY_RETRY_ATTEMPTS` | `2` | Discovery retry attempts |
| `TOOL_DISCOVERY_RETRY_DELAY` | `1` | Discovery retry delay (seconds) |

### Logging Configuration

#### Backend Logging

| Variable | Default | Description |
|----------|---------|-------------|
| `LOG_LEVEL_ROOT` | `INFO` | Root logger level |
| `LOG_LEVEL_QUIP_BACKEND` | `INFO` | Application logger level |
| `LOG_LEVEL_SPRING` | `WARN` | Spring framework logger level |
| `LOG_LEVEL_MYBATIS` | `WARN` | MyBatis logger level |
| `LOG_LEVEL_REDIS` | `INFO` | Redis operations logger level |
| `LOG_LEVEL_CACHE` | `INFO` | Cache operations logger level |
| `LOG_LEVEL_TOOL_SYNC` | `INFO` | Tool sync logger level |
| `LOG_LEVEL_TOOL_CONSUMER` | `INFO` | Tool consumer logger level |
| `LOG_LEVEL_TOOL_HANDLER` | `INFO` | Tool handler logger level |
| `LOG_LEVEL_SPRING_CACHE` | `WARN` | Spring cache logger level |
| `LOG_LEVEL_LETTUCE` | `INFO` | Lettuce Redis client logger level |

#### Agent Logging

| Variable | Default | Description |
|----------|---------|-------------|
| `LOG_LEVEL` | `INFO` | Main logger level |
| `LOG_FORMAT` | `"%(asctime)s - %(name)s - %(levelname)s - %(message)s"` | Log format |
| `LOG_FILE_PATH` | `logs/agent.log` | Log file path |
| `LOG_LEVEL_REDIS` | `INFO` | Redis operations logger level |
| `LOG_LEVEL_TOOL_SYNC` | `INFO` | Tool sync logger level |
| `LOG_LEVEL_TOOL_DISCOVERY` | `INFO` | Tool discovery logger level |
| `LOG_LEVEL_MCP` | `INFO` | MCP client logger level |

## Environment-Specific Configurations

### Development Environment

**Characteristics:**
- Faster iteration with shorter timeouts
- More verbose logging for debugging
- Lower failure thresholds for quick feedback
- Local service URLs

**Key Settings:**
```bash
# Shorter timeouts for development
TOOL_SYNC_RECOVERY_TIMEOUT=15000
TOOL_DISCOVERY_TIMEOUT=15

# More verbose logging
LOG_LEVEL_TOOL_SYNC=DEBUG
LOG_LEVEL_TOOL_CONSUMER=DEBUG
LOG_LEVEL_TOOL_HANDLER=DEBUG

# Lower thresholds for faster testing
TOOL_SYNC_FAILURE_CONSECUTIVE=3
TOOL_SYNC_FAILURE_INVALID_MESSAGES=5

# Local URLs
TOOL_SYNC_AGENT_URL=http://localhost:5001
REDIS_HOST=localhost
```

### Staging Environment

**Characteristics:**
- Similar to production but with enhanced debugging
- Moderate timeouts for testing
- Detailed logging for troubleshooting
- Internal service names

**Key Settings:**
```bash
# Moderate timeouts for staging
TOOL_SYNC_RECOVERY_TIMEOUT=15000
TOOL_DISCOVERY_TIMEOUT=15

# Enhanced logging for debugging
LOG_LEVEL_TOOL_SYNC=DEBUG
LOG_LEVEL_TOOL_CONSUMER=DEBUG

# Moderate thresholds
TOOL_SYNC_FAILURE_CONSECUTIVE=3
TOOL_SYNC_FAILURE_TIME_WINDOW=30000

# Staging service URLs
TOOL_SYNC_AGENT_URL=http://quip-agent-staging:5001
REDIS_HOST=redis-staging
```

### Production Environment

**Characteristics:**
- Optimized for stability and performance
- Standard timeouts for reliability
- Reduced logging verbosity
- Production service names and security

**Key Settings:**
```bash
# Standard production timeouts
TOOL_SYNC_RECOVERY_TIMEOUT=10000
TOOL_DISCOVERY_TIMEOUT=10

# Production logging levels
LOG_LEVEL_REDIS=INFO
LOG_LEVEL_TOOL_SYNC=INFO
LOG_LEVEL_LETTUCE=WARN

# Higher thresholds for stability
TOOL_SYNC_FAILURE_CONSECUTIVE=5
TOOL_SYNC_FAILURE_INVALID_MESSAGES=10

# Production service URLs
TOOL_SYNC_AGENT_URL=http://agent-service:5001
REDIS_HOST=redis-cluster.production.local
REDIS_PASSWORD=your_production_redis_password
```

## Configuration Files

### Agent Configuration Files

- `config.yaml` - Base configuration
- `config-staging.yaml` - Staging environment overrides
- `config-prod.yaml` - Production environment overrides

### Backend Configuration Files

- `application.yml` - Base configuration
- `application-dev.yml` - Development environment
- `application-staging.yml` - Staging environment
- `application-prod.yml` - Production environment

## Docker Compose Configuration

### Development

```yaml
services:
  backend:
    environment:
      - REDIS_HOST=redis
      - TOOL_SYNC_AGENT_URL=http://agent:5001
      - LOG_LEVEL_TOOL_SYNC=DEBUG
      - TOOL_SYNC_RECOVERY_TIMEOUT=15000
  
  agent:
    environment:
      - REDIS_HOST=redis
      - TOOL_SYNC_HTTP_PORT=5001
      - LOG_LEVEL_TOOL_SYNC=DEBUG
      - TOOL_DISCOVERY_TIMEOUT=15
    ports:
      - "5001:5001"
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

### Production

```yaml
services:
  backend:
    environment:
      - REDIS_HOST=redis-cluster
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - TOOL_SYNC_AGENT_URL=http://agent:5001
      - LOG_LEVEL_TOOL_SYNC=INFO
      - TOOL_SYNC_RECOVERY_TIMEOUT=10000
  
  agent:
    environment:
      - REDIS_HOST=redis-cluster
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - TOOL_SYNC_HTTP_PORT=5001
      - LOG_LEVEL_TOOL_SYNC=INFO
      - TOOL_DISCOVERY_TIMEOUT=10
    # No external port exposure in production
  
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
```

## Kubernetes Configuration

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: tool-sync-config
data:
  # Redis Configuration
  REDIS_HOST: "redis-service"
  REDIS_PORT: "6379"
  REDIS_DATABASE: "0"
  
  # Tool Sync Configuration
  TOOL_SYNC_RECOVERY_ENABLED: "true"
  TOOL_SYNC_RECOVERY_TIMEOUT: "10000"
  TOOL_SYNC_RECOVERY_MAX_RETRIES: "3"
  TOOL_SYNC_AGENT_URL: "http://agent-service:5001"
  
  # Agent HTTP Server
  TOOL_SYNC_HTTP_ENABLED: "true"
  TOOL_SYNC_HTTP_PORT: "5001"
  TOOL_DISCOVERY_TIMEOUT: "10"
  
  # Logging
  LOG_LEVEL_TOOL_SYNC: "INFO"
  LOG_LEVEL_REDIS: "INFO"
```

### Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tool-sync-secrets
type: Opaque
data:
  REDIS_PASSWORD: <base64-encoded-password>
  OPENAI_API_KEY: <base64-encoded-api-key>
```

### Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  template:
    spec:
      containers:
      - name: backend
        envFrom:
        - configMapRef:
            name: tool-sync-config
        - secretRef:
            name: tool-sync-secrets
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
        envFrom:
        - configMapRef:
            name: tool-sync-config
        - secretRef:
            name: tool-sync-secrets
        ports:
        - containerPort: 5001
```

## Security Considerations

### Redis Security

- **Authentication**: Always use `REDIS_PASSWORD` in production
- **SSL/TLS**: Enable Redis SSL for production deployments
- **Network**: Restrict Redis access to application services only
- **Firewall**: Configure firewall rules to limit Redis port access

### Agent HTTP Server Security

- **Network Binding**: Use `0.0.0.0` only in containerized environments
- **Port Exposure**: Don't expose agent HTTP port externally in production
- **Authentication**: Consider adding API key authentication for production
- **Rate Limiting**: Implement rate limiting for resync endpoints

### Environment Variables

- **Secrets Management**: Use Kubernetes secrets or Docker secrets for sensitive data
- **Credential Rotation**: Regularly rotate Redis passwords and API keys
- **Access Control**: Limit access to environment variable configuration

## Performance Tuning

### Timeout Configuration

**Development:**
- Tool Discovery: 15-30 seconds (for debugging)
- HTTP Requests: 15-20 seconds
- Redis Operations: 5-10 seconds

**Production:**
- Tool Discovery: 10-15 seconds
- HTTP Requests: 10-15 seconds
- Redis Operations: 2-5 seconds

### Retry Configuration

**Development:**
- Fewer retries (1-2) for faster feedback
- Shorter delays (0.5-1 second)

**Production:**
- More retries (3-5) for reliability
- Longer delays (1-2 seconds)

### Failure Thresholds

**Development:**
- Lower thresholds for faster testing
- Shorter time windows (30 seconds)

**Production:**
- Higher thresholds for stability
- Longer time windows (60 seconds)

## Monitoring and Alerting

### Key Metrics to Monitor

- Tool sync recovery events per hour
- Redis connection failures
- Tool discovery timeout rates
- HTTP request success/failure rates
- Message processing latency

### Recommended Alerts

- Sync recovery triggered > 3 times in 1 hour
- Redis connection down for > 5 minutes
- Tool discovery timeout rate > 20%
- HTTP request failure rate > 50%

### Health Checks

```bash
# Backend health check
curl -f http://backend:8080/actuator/health

# Agent health check
curl -f http://agent:5001/health

# Redis connectivity check
redis-cli -h redis ping
```

## Troubleshooting

### Common Issues

1. **Redis Connection Failures**
   - Check `REDIS_HOST` and `REDIS_PORT`
   - Verify Redis server is running
   - Check network connectivity

2. **Agent HTTP Server Not Responding**
   - Verify `TOOL_SYNC_HTTP_ENABLED=true`
   - Check port binding and firewall rules
   - Review agent startup logs

3. **Tool Discovery Timeouts**
   - Increase `TOOL_DISCOVERY_TIMEOUT`
   - Check MCP server connectivity
   - Review tool discovery logs

4. **Sync Recovery Not Working**
   - Verify `TOOL_SYNC_RECOVERY_ENABLED=true`
   - Check agent HTTP endpoint URL
   - Review failure threshold settings

### Debug Commands

```bash
# Test Redis connectivity
redis-cli -h $REDIS_HOST -p $REDIS_PORT ping

# Test agent HTTP endpoint
curl -X POST http://$TOOL_SYNC_AGENT_URL/api/tools/resync \
  -H "Content-Type: application/json" \
  -d '{"requestId":"test","timestamp":"2025-01-28T10:00:00Z","reason":"test"}'

# Check backend logs
tail -f logs/backend.log | grep -E "(tool_sync|redis|recovery)"

# Check agent logs
tail -f logs/agent.log | grep -E "(tool_sync|redis|discovery)"
```

## Migration Guide

### Upgrading from Previous Versions

1. **Update Environment Variables**: Add new tool sync variables to your deployment
2. **Update Configuration Files**: Use new configuration file formats
3. **Test Connectivity**: Verify agent HTTP server is accessible from backend
4. **Monitor Logs**: Check for any configuration warnings or errors
5. **Validate Functionality**: Test tool sync recovery manually

### Configuration Validation

Before deploying, validate your configuration:

1. Check all required environment variables are set
2. Verify service URLs are accessible
3. Test Redis connectivity
4. Validate timeout values are reasonable
5. Confirm logging levels are appropriate for environment

This comprehensive configuration guide ensures proper deployment of the agent-tool-sync feature across all environments.

## Related Documentation

- [Environment-Specific Configuration](ENVIRONMENT_CONFIGURATION.md) - Detailed environment-specific settings
- [Agent Tool Sync Configuration](agent/TOOL_SYNC_CONFIGURATION.md) - Agent-specific configuration guide
- [Backend Sync Recovery Configuration](backend/SYNC_RECOVERY_CONFIGURATION.md) - Backend-specific configuration guide