# Deployment Scripts

This directory contains deployment and utility scripts for the Quip Backend application with Redis integration.

## Scripts Overview

### `deploy-with-redis.sh`
Main deployment script for development and staging environments.

**Usage:**
```bash
./scripts/deploy-with-redis.sh [command]
```

**Commands:**
- `deploy` - Full deployment (build, start, initialize)
- `start` - Start services without building
- `stop` - Stop all services
- `restart` - Stop and start services
- `health` - Run health checks
- `init-redis` - Initialize Redis only

**Environment Variables:**
- `ENVIRONMENT=dev|staging|prod` - Set deployment environment
- `SKIP_BUILD=true` - Skip building services
- `SKIP_REDIS_INIT=true` - Skip Redis initialization
- `REDIS_HEALTH_TIMEOUT=60` - Redis health check timeout

### `deploy-prod.sh`
Production deployment script with enhanced security and monitoring.

**Usage:**
```bash
./scripts/deploy-prod.sh [command]
```

**Commands:**
- `deploy` - Full production deployment
- `start` - Start production services
- `stop` - Stop all production services
- `restart` - Restart production services
- `health` - Run production health checks

**Features:**
- Enhanced security checks
- Longer health check timeouts
- Production-specific configuration validation
- Resource monitoring

### `utils.sh`
Utility script for common development and maintenance tasks.

**Usage:**
```bash
./scripts/utils.sh [command] [options]
```

**Commands:**
- `status` - Check service status
- `logs <service> [env]` - View logs for a service
- `redis-cli [env]` - Connect to Redis CLI
- `db-cli [env]` - Connect to database CLI
- `cleanup` - Clean up Docker resources
- `reset-redis [env]` - Reset Redis data
- `cache-stats [env]` - Show cache statistics
- `validate-redis [env]` - Validate Redis configuration

### `redis-config.sh`
Redis configuration management and validation script.

**Usage:**
```bash
./scripts/redis-config.sh [command] [options]
```

**Commands:**
- `validate` - Validate all Redis configuration files
- `show [env]` - Show configuration summary
- `test [env]` - Test Redis configuration
- `template [env]` - Generate configuration template

## Quick Start Examples

### Development Deployment
```bash
# Full deployment
./scripts/deploy-with-redis.sh deploy

# Start services only
./scripts/deploy-with-redis.sh start

# Check health
./scripts/deploy-with-redis.sh health
```

### Production Deployment
```bash
# Ensure .env is configured for production
cp .env.prod.example .env

# Deploy to production
./scripts/deploy-prod.sh deploy

# Check production health
./scripts/deploy-prod.sh health
```

### Common Utilities
```bash
# Check service status
./scripts/utils.sh status

# View backend logs
./scripts/utils.sh logs quip-backend-app

# Connect to Redis
./scripts/utils.sh redis-cli

# View cache statistics
./scripts/utils.sh cache-stats

# Validate Redis configuration
./scripts/utils.sh validate-redis

# Clean up resources
./scripts/utils.sh cleanup
```

### Redis Configuration Management
```bash
# Validate all Redis config files
./scripts/redis-config.sh validate

# Show Redis configuration summary
./scripts/redis-config.sh show prod

# Test Redis configuration
./scripts/redis-config.sh test dev

# Generate configuration template
./scripts/redis-config.sh template prod
```

## Environment Configuration

### Development (.env)
```bash
# Copy from example
cp .env.example .env

# Key settings for development
REDIS_PASSWORD=  # Optional for development
REDIS_ENABLED=true
```

### Production (.env)
```bash
# Copy from production example
cp .env.prod.example .env

# Required production settings
REDIS_PASSWORD=your_secure_password  # REQUIRED
DB_PASSWORD=your_secure_db_password  # REQUIRED
```

## Service Dependencies

The scripts handle service startup order automatically:

1. **Database** (PostgreSQL)
2. **Redis** (Cache layer)
3. **Backend** (Spring Boot application)
4. **Agent** (Python service)
5. **MCP Server** (Model Context Protocol)
6. **Frontend** (React application)

## Health Checks

All scripts include comprehensive health checks:

- **Database**: PostgreSQL readiness check
- **Redis**: Connection and ping test
- **Backend**: HTTP health endpoint
- **Cache Integration**: Redis integration in backend
- **Service Dependencies**: Proper startup order verification

## Troubleshooting

### Common Issues

1. **Port Conflicts**: Ensure ports 3000, 5001, 6379, 8000, 8080 are available
2. **Docker Issues**: Restart Docker daemon if containers fail to start
3. **Permission Issues**: Ensure scripts are executable (`chmod +x scripts/*.sh`)
4. **Environment Variables**: Verify all required variables are set in .env

### Debug Commands

```bash
# Check Docker status
docker ps
docker logs <container_name>

# Check service health
curl http://localhost:8080/actuator/health

# Monitor Redis
./scripts/utils.sh redis-cli
> ping
> info stats

# View all logs
docker-compose logs -f
```

## Security Considerations

### Development
- Redis password is optional
- Relaxed security settings
- Debug logging enabled

### Production
- Redis password is **required**
- Enhanced security validation
- Resource limits enforced
- SSL/TLS recommended

## Maintenance

### Regular Tasks
- Monitor service health with `./scripts/utils.sh status`
- Check cache performance with `./scripts/utils.sh cache-stats`
- Review logs with `./scripts/utils.sh logs <service>`
- Clean up resources with `./scripts/utils.sh cleanup`

### Updates
```bash
# Pull latest changes
git pull origin main

# Restart services
./scripts/deploy-with-redis.sh restart
```

## Support

For additional help:
- See [DEPLOYMENT_GUIDE.md](../DEPLOYMENT_GUIDE.md) for detailed deployment instructions
- Check service logs for error details
- Use health endpoints for service status
- Review Docker container status with `docker ps`