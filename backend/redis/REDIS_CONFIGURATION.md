# Redis Configuration Guide

This document provides comprehensive guidance on configuring Redis for the Quip Backend application across different environments.

## Table of Contents

1. [Overview](#overview)
2. [Configuration Structure](#configuration-structure)
3. [Environment-Specific Configurations](#environment-specific-configurations)
4. [Configuration Properties Reference](#configuration-properties-reference)
5. [Best Practices](#best-practices)
6. [Troubleshooting](#troubleshooting)
7. [Security Considerations](#security-considerations)

## Overview

The application uses Redis for:
- **Caching**: High-performance caching of frequently accessed data
- **Session Storage**: Future support for distributed session management
- **Health Monitoring**: Redis connectivity and performance monitoring
- **Metrics Collection**: Cache hit/miss ratios and performance metrics

## Configuration Structure

Redis configuration is organized into several layers:

### 1. Spring Boot Redis Configuration (`spring.redis`)
Core Redis connection and pool settings.

### 2. Spring Cache Configuration (`spring.cache`)
Cache-specific settings including TTL and serialization.

### 3. Application-Specific Configuration (`app.redis`)
Custom application settings for cache TTL, retry logic, and circuit breaker.

## Environment-Specific Configurations

### Development Environment (`application-dev.yml`)

**Purpose**: Local development with relaxed settings for faster iteration.

**Key Features**:
- Shorter TTL values (30 minutes default)
- Smaller connection pool (4 connections)
- More frequent health checks (10 seconds)
- Enhanced debug logging
- Lower circuit breaker thresholds

**Usage**:
```bash
java -jar backend.jar --spring.profiles.active=dev
```

### Staging Environment (`application-staging.yml`)

**Purpose**: Pre-production testing with production-like settings.

**Key Features**:
- Medium TTL values (30 minutes to 12 hours)
- Medium connection pool (12 connections)
- Balanced monitoring intervals
- Detailed logging for debugging
- Moderate circuit breaker settings

**Usage**:
```bash
java -jar backend.jar --spring.profiles.active=staging
```

### Production Environment (`application-prod.yml`)

**Purpose**: Production deployment with optimized performance and security.

**Key Features**:
- Longer TTL values (1 hour to 24 hours)
- Larger connection pool (16 connections)
- SSL/TLS encryption support
- Minimal logging for performance
- Higher circuit breaker thresholds
- Prometheus metrics export

**Usage**:
```bash
java -jar backend.jar --spring.profiles.active=prod
```

## Configuration Properties Reference

### Core Redis Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.redis.enabled` | `true` | Enable/disable Redis integration |
| `spring.redis.host` | `localhost` | Redis server hostname |
| `spring.redis.port` | `6379` | Redis server port |
| `spring.redis.password` | `""` | Redis authentication password |
| `spring.redis.database` | `0` | Redis database index |
| `spring.redis.timeout` | `2000ms` | Command timeout |
| `spring.redis.connect-timeout` | `2000ms` | Connection timeout |

### Connection Pool Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.redis.lettuce.pool.max-active` | `8` | Maximum active connections |
| `spring.redis.lettuce.pool.max-idle` | `8` | Maximum idle connections |
| `spring.redis.lettuce.pool.min-idle` | `0` | Minimum idle connections |
| `spring.redis.lettuce.pool.max-wait` | `-1ms` | Maximum wait time for connection |

### Cache Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.cache.redis.time-to-live` | `3600000` | Default TTL in milliseconds |
| `spring.cache.redis.cache-null-values` | `false` | Whether to cache null values |
| `spring.cache.redis.key-prefix` | `"quip:backend:"` | Key prefix for all cache entries |

### Application-Specific Properties

| Property | Default | Description |
|----------|---------|-------------|
| `app.redis.default-ttl` | `3600` | Default TTL in seconds |
| `app.redis.key-prefix` | `"quip:backend:"` | Application key prefix |
| `app.redis.health-check-interval` | `30000` | Health check interval in ms |

### Cache-Specific TTL Settings

| Cache Type | Property | Default (seconds) | Description |
|------------|----------|-------------------|-------------|
| Tool Whitelist | `app.redis.cache-ttl.tool-whitelist` | `3600` | Tool whitelist data |
| Problem Categories | `app.redis.cache-ttl.problem-categories` | `86400` | Problem categories |
| Server Data | `app.redis.cache-ttl.server-data` | `21600` | Server information |
| Member Data | `app.redis.cache-ttl.member-data` | `1800` | Member information |
| Assistant Session | `app.redis.cache-ttl.assistant-session` | `7200` | Assistant sessions |
| Temporary Data | `app.redis.cache-ttl.temporary-data` | `300` | Short-term data |

## Best Practices

### 1. Environment Variables

Use environment variables for sensitive and environment-specific configuration:

```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
```

### 2. Connection Pool Sizing

**Development**: Small pool (4-8 connections)
**Staging**: Medium pool (8-12 connections)  
**Production**: Large pool (12-20 connections)

### 3. TTL Strategy

- **Short-term (5 minutes)**: Real-time data, temporary calculations
- **Medium-term (30 minutes - 2 hours)**: User sessions, dynamic content
- **Long-term (6-24 hours)**: Static configuration, reference data

### 4. Key Naming Convention

Use consistent key prefixes:
```
quip:backend:tool:whitelist:{serverId}
quip:backend:problem:categories
quip:backend:server:{serverId}
quip:backend:member:{memberId}
```

### 5. Monitoring and Health Checks

Enable comprehensive monitoring:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  health:
    redis:
      enabled: true
```

## Troubleshooting

### Common Issues

#### 1. Connection Timeouts
**Symptoms**: `RedisConnectionFailureException`
**Solutions**:
- Increase `connect-timeout` and `timeout` values
- Check network connectivity to Redis server
- Verify Redis server is running and accessible

#### 2. Pool Exhaustion
**Symptoms**: `Could not get a resource from the pool`
**Solutions**:
- Increase `max-active` pool size
- Check for connection leaks in application code
- Monitor connection usage patterns

#### 3. Memory Issues
**Symptoms**: Redis running out of memory
**Solutions**:
- Review TTL settings and reduce if necessary
- Implement cache eviction policies
- Monitor cache hit/miss ratios

#### 4. SSL/TLS Issues (Production)
**Symptoms**: SSL handshake failures
**Solutions**:
- Verify SSL certificates are valid
- Check SSL configuration in Redis server
- Ensure proper SSL bundle configuration

### Diagnostic Commands

Check Redis connectivity:
```bash
redis-cli -h <host> -p <port> ping
```

Monitor Redis performance:
```bash
redis-cli -h <host> -p <port> info
redis-cli -h <host> -p <port> monitor
```

Check application health:
```bash
curl http://localhost:8080/actuator/health/redis
curl http://localhost:8080/actuator/metrics/cache.gets
```

## Security Considerations

### 1. Authentication
Always use password authentication in non-development environments:
```yaml
spring:
  redis:
    password: ${REDIS_PASSWORD}  # Never hardcode passwords
```

### 2. Network Security
- Use private networks for Redis communication
- Implement firewall rules to restrict Redis access
- Consider VPN or private cloud networks

### 3. SSL/TLS Encryption
Enable SSL for production environments:
```yaml
spring:
  redis:
    ssl: true
    ssl-bundle: redis-ssl
```

### 4. Data Sensitivity
- Avoid caching sensitive personal information
- Implement data masking for cached sensitive data
- Use appropriate TTL for sensitive data

### 5. Access Control
- Use Redis ACL (Access Control Lists) when available
- Limit Redis commands available to application
- Implement audit logging for sensitive operations

## Environment Variable Reference

Create a `.env` file for local development:

```bash
# Redis Configuration
REDIS_ENABLED=true
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
REDIS_TIMEOUT=2000
REDIS_CONNECT_TIMEOUT=2000

# Connection Pool
REDIS_POOL_MAX_ACTIVE=8
REDIS_POOL_MAX_IDLE=8
REDIS_POOL_MIN_IDLE=0
REDIS_POOL_MAX_WAIT=-1

# Cache TTL Settings
CACHE_DEFAULT_TTL=3600000
CACHE_TTL_TOOL_WHITELIST=3600
CACHE_TTL_PROBLEM_CATEGORIES=86400
CACHE_TTL_SERVER_DATA=21600
CACHE_TTL_MEMBER_DATA=1800
CACHE_TTL_ASSISTANT_SESSION=7200
CACHE_TTL_TEMPORARY_DATA=300

# Application Redis Settings
APP_REDIS_DEFAULT_TTL=3600
APP_REDIS_HEALTH_INTERVAL=30000

# Retry and Circuit Breaker
REDIS_RETRY_MAX_ATTEMPTS=3
REDIS_RETRY_DELAY=1000
REDIS_CB_FAILURE_THRESHOLD=5
REDIS_CB_RECOVERY_TIMEOUT=30000

# SSL (Production)
REDIS_SSL_ENABLED=false
```

## Migration Guide

When updating Redis configuration:

1. **Backup existing configuration**
2. **Test changes in development environment**
3. **Validate configuration with staging environment**
4. **Deploy to production with monitoring**
5. **Monitor application health and performance**

For questions or issues, refer to the [Redis Integration Design Document](design.md) or contact the development team.