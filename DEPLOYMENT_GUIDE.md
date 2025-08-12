# Quip Backend Deployment Guide with Redis Integration

This guide provides comprehensive instructions for deploying the Quip Backend application with Redis integration across different environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Development Deployment](#development-deployment)
4. [Staging Deployment](#staging-deployment)
5. [Production Deployment](#production-deployment)
6. [Redis Configuration](#redis-configuration)
7. [Monitoring and Health Checks](#monitoring-and-health-checks)
8. [Troubleshooting](#troubleshooting)
9. [Maintenance](#maintenance)

## Prerequisites

### System Requirements

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **Memory**: Minimum 4GB RAM (8GB recommended for production)
- **Storage**: Minimum 10GB free space
- **Network**: Ports 3000, 5001, 6379, 8000, 8080 available

### Software Dependencies

- Git (for cloning the repository)
- curl (for health checks)
- jq (optional, for JSON processing)

## Environment Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd quip-backend
```

### 2. Environment Configuration

Choose the appropriate environment file:

#### Development
```bash
cp .env.example .env
```

#### Production
```bash
cp .env.prod.example .env.prod
```

### 3. Configure Environment Variables

Edit your environment file and set the following required variables:

**Database Configuration:**
```bash
DB_NAME=your_database_name
DB_USER=your_database_user
DB_PASSWORD=your_secure_database_password
```

**Redis Configuration (REQUIRED):**
```bash
REDIS_PASSWORD=your_secure_redis_password  # Required for production
REDIS_HOST=quip-redis                      # Use localhost for external Redis
REDIS_PORT=6379
```

## Development Deployment

### Quick Start

```bash
# Using the deployment script
./scripts/deploy-with-redis.sh deploy

# Or manually with docker-compose
docker-compose up -d
```

### Development Features

- **Hot Reloading**: Code changes are reflected automatically
- **Debug Logging**: Enhanced logging for development
- **Shorter TTL**: Cache expires faster for testing
- **Local Redis**: Redis runs in a container with no password

### Development URLs

- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080
- **Agent**: http://localhost:5001
- **MCP Server**: http://localhost:8000
- **Redis**: localhost:6379 (no password)

## Staging Deployment

### Setup

```bash
# Set environment
export ENVIRONMENT=staging

# Copy staging configuration
cp .env.example .env
# Edit .env with staging-specific values

# Deploy
./scripts/deploy-with-redis.sh deploy
```

### Staging Features

- **Production-like**: Similar to production but with relaxed settings
- **Testing-friendly**: Shorter TTL for faster testing cycles
- **Enhanced Monitoring**: Detailed health checks and metrics
- **SSL Optional**: Can test SSL configurations

## Production Deployment

### Prerequisites

1. **Security Review**: Ensure all passwords are secure
2. **SSL Certificates**: Prepare SSL certificates if using HTTPS
3. **Monitoring Setup**: Configure external monitoring tools
4. **Backup Strategy**: Plan for database and Redis backups

### Production Setup

```bash
# Set environment
export ENVIRONMENT=prod

# Copy production configuration
cp .env.prod.example .env.prod
# Edit .env.prod with production values

# Deploy with production script
./scripts/deploy-prod.sh deploy

# Or manually with production compose file
docker-compose -f docker-compose.prod.yml up -d
```

### Production Security Checklist

- [ ] Strong passwords for all services
- [ ] Redis password authentication enabled
- [ ] Database access restricted
- [ ] SSL/TLS encryption configured
- [ ] Firewall rules in place
- [ ] Regular security updates scheduled

### Production Monitoring

The production deployment includes:

- **Health Endpoints**: `/actuator/health`, `/actuator/health/redis`
- **Metrics**: Prometheus-compatible metrics at `/actuator/metrics`
- **Resource Limits**: CPU and memory limits for all containers
- **Restart Policies**: Automatic restart on failure

## Redis Configuration

### Redis Security

#### Development
- No password required
- Basic configuration
- Local access only

#### Production
- **Password Required**: Set `REDIS_PASSWORD` environment variable
- **Command Restrictions**: Dangerous commands disabled
- **Memory Limits**: 512MB limit with LRU eviction
- **Persistence**: Both RDB and AOF enabled

### Redis Persistence

#### RDB (Redis Database)
- **Snapshots**: Automatic snapshots at intervals
- **Fast Recovery**: Quick startup from snapshots
- **Configuration**: `save 900 1`, `save 300 10`, `save 60 10000`

#### AOF (Append Only File)
- **Durability**: Every operation logged
- **Consistency**: Better data consistency
- **Configuration**: `appendfsync everysec`

### Cache Configuration

#### Cache Types and TTL

| Cache Type | Development | Staging | Production |
|------------|-------------|---------|------------|
| Tool Whitelist | 30 min | 1 hour | 2 hours |
| Problem Categories | 1 hour | 12 hours | 24 hours |
| Server Data | 30 min | 3 hours | 6 hours |
| Member Data | 15 min | 30 min | 1 hour |
| Assistant Session | 1 hour | 1 hour | 2 hours |
| Temporary Data | 5 min | 5 min | 5 min |

## Monitoring and Health Checks

### Health Endpoints

#### Application Health
```bash
curl http://localhost:8080/actuator/health
```

#### Redis Health
```bash
curl http://localhost:8080/actuator/health/redis
```

#### Cache Metrics
```bash
curl http://localhost:8080/actuator/metrics/cache.gets
curl http://localhost:8080/actuator/metrics/cache.puts
curl http://localhost:8080/actuator/metrics/cache.evictions
```

### Redis Monitoring

#### Redis CLI Access
```bash
# Development
docker exec -it quip-redis redis-cli

# Production (with password)
docker exec -it quip-redis-prod redis-cli -a your_redis_password
```

#### Redis Information
```bash
# Memory usage
redis-cli info memory

# Keyspace information
redis-cli info keyspace

# Performance stats
redis-cli info stats
```

### Log Monitoring

#### Application Logs
```bash
docker-compose logs -f quip-backend-app
```

#### Redis Logs
```bash
docker-compose logs -f quip-redis
```

#### All Services
```bash
docker-compose logs -f
```

## Troubleshooting

### Common Issues

#### 1. Redis Connection Failed

**Symptoms:**
- `RedisConnectionFailureException` in logs
- Health check failures

**Solutions:**
```bash
# Check Redis container status
docker ps | grep redis

# Check Redis logs
docker logs quip-redis

# Test Redis connectivity
docker exec quip-redis redis-cli ping

# Restart Redis
docker-compose restart quip-redis
```

#### 2. Cache Not Working

**Symptoms:**
- No cache hits in metrics
- Slow response times

**Solutions:**
```bash
# Check cache configuration
curl http://localhost:8080/actuator/configprops | jq '.spring.cache'

# Check Redis keys
docker exec -it quip-redis redis-cli keys "quip:backend:*"

# Monitor cache operations
curl http://localhost:8080/actuator/metrics/cache.gets
```

#### 3. Memory Issues

**Symptoms:**
- Redis out of memory errors
- Application performance degradation

**Solutions:**
```bash
# Check Redis memory usage
docker exec quip-redis redis-cli info memory

# Check container resource usage
docker stats

# Adjust memory limits in docker-compose.yml
# Reduce cache TTL values
```

#### 4. Performance Issues

**Symptoms:**
- Slow response times
- High CPU usage

**Solutions:**
```bash
# Check slow queries
docker exec quip-redis redis-cli slowlog get 10

# Monitor Redis performance
docker exec quip-redis redis-cli --latency

# Check connection pool usage
curl http://localhost:8080/actuator/metrics/redis.connections.active
```

### Diagnostic Commands

#### System Health Check
```bash
# Development
./scripts/deploy-with-redis.sh health

# Production
./scripts/deploy-prod.sh health
```

#### Redis Diagnostics
```bash
# Redis info
docker exec quip-redis redis-cli info all

# Check specific cache keys
docker exec quip-redis redis-cli keys "quip:backend:tool:*"

# Monitor Redis operations
docker exec quip-redis redis-cli monitor
```

#### Application Diagnostics
```bash
# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

## Maintenance

### Regular Maintenance Tasks

#### Daily
- [ ] Check service health status
- [ ] Monitor resource usage
- [ ] Review error logs

#### Weekly
- [ ] Analyze cache hit/miss ratios
- [ ] Review slow query logs
- [ ] Check disk space usage

#### Monthly
- [ ] Update security patches
- [ ] Review and optimize cache TTL settings
- [ ] Backup configuration files

### Backup Procedures

#### Redis Backup
```bash
# Create Redis backup
docker exec quip-redis redis-cli BGSAVE

# Copy backup file
docker cp quip-redis:/data/dump.rdb ./backup/redis-backup-$(date +%Y%m%d).rdb
```

#### Database Backup
```bash
# PostgreSQL backup
docker exec quip-db pg_dump -U $DB_USER $DB_NAME > backup/db-backup-$(date +%Y%m%d).sql
```

### Updates and Upgrades

#### Application Updates
```bash
# Pull latest changes
git pull origin main

# Rebuild and restart
./scripts/deploy-with-redis.sh restart
```

#### Redis Updates
```bash
# Update Redis image
docker-compose pull quip-redis

# Restart with new image
docker-compose up -d quip-redis
```

### Scaling Considerations

#### Horizontal Scaling
- Use Redis Cluster for multiple Redis instances
- Implement session affinity for stateful operations
- Consider load balancing for backend services

#### Vertical Scaling
- Increase memory limits in docker-compose.yml
- Adjust Redis maxmemory settings
- Scale database resources

## Security Best Practices

### Redis Security
- Always use password authentication in production
- Disable dangerous Redis commands
- Use SSL/TLS for Redis connections in production
- Implement network segmentation

### Application Security
- Use environment variables for sensitive configuration
- Implement proper access controls
- Regular security updates
- Monitor for security vulnerabilities

### Network Security
- Use private networks for inter-service communication
- Implement firewall rules
- Use VPN for remote access
- Monitor network traffic

## Support and Documentation

For additional support:

1. **Configuration Reference**: See [REDIS_CONFIGURATION.md](backend/REDIS_CONFIGURATION.md)
2. **Performance Testing**: See [REDIS_PERFORMANCE_TESTS.md](backend/REDIS_PERFORMANCE_TESTS.md)
3. **API Documentation**: Available at `/swagger-ui.html` when running
4. **Health Dashboard**: Available at `/actuator` endpoints

## Conclusion

This deployment guide provides comprehensive instructions for deploying the Quip Backend with Redis integration. Follow the environment-specific instructions and maintain regular monitoring for optimal performance and security.