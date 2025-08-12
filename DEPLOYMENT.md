# Deployment Configuration

## Basic Setup

1. Copy environment files:
   ```bash
   cp agent/.env.example agent/.env
   cp backend/.env.example backend/.env
   ```

2. Update the `.env` files with your values:
   - `OPENAI_API_KEY` in agent/.env
   - `DB_PASSWORD` in backend/.env
   - `REDIS_PASSWORD` (optional, leave empty for no password)

3. Start services:
   ```bash
   docker-compose up -d
   ```

## Redis Configuration

The system uses Redis for tool synchronization. Basic configuration:

**Agent (.env)**:
- `REDIS_HOST`: Redis server host (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

**Backend (.env)**:
- `REDIS_HOST`: Redis server host (default: redis)
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

## Logging

Logs are written to:
- Agent: `agent/logs/agent.log`
- Backend: `backend/logs/backend.log`

## Health Checks

Check if services are running:
```bash
docker-compose ps
docker-compose logs -f
```

Test Redis connectivity:
```bash
docker-compose exec quip-redis redis-cli ping
```