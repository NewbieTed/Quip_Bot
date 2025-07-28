#!/bin/bash

# Redis Initialization Script for Quip Backend
# This script initializes Redis with application-specific settings

set -e

echo "Starting Redis initialization..."

# Configuration variables
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}
REDIS_DATABASE=${REDIS_DATABASE:-0}

# Wait for Redis to be ready
echo "Waiting for Redis to be ready..."
until redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ${REDIS_PASSWORD:+-a "$REDIS_PASSWORD"} ping > /dev/null 2>&1; do
    echo "Redis is unavailable - sleeping"
    sleep 1
done

echo "Redis is ready!"

# Function to execute Redis commands
redis_exec() {
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ${REDIS_PASSWORD:+-a "$REDIS_PASSWORD"} -n "$REDIS_DATABASE" "$@"
}

# Set up application-specific Redis configuration
echo "Setting up application-specific Redis configuration..."

# Configure memory policy if not already set
CURRENT_POLICY=$(redis_exec CONFIG GET maxmemory-policy | tail -n 1)
if [ "$CURRENT_POLICY" != "allkeys-lru" ]; then
    echo "Setting maxmemory-policy to allkeys-lru..."
    redis_exec CONFIG SET maxmemory-policy allkeys-lru
fi

# Set up key expiration notifications (optional)
echo "Enabling keyspace notifications for expired keys..."
redis_exec CONFIG SET notify-keyspace-events Ex

# Create application-specific key prefixes (for documentation purposes)
echo "Setting up application key prefixes..."
redis_exec SET "quip:backend:initialized" "$(date -u +%Y-%m-%dT%H:%M:%SZ)" EX 86400

# Set up cache warming keys (these will be populated by the application)
echo "Preparing cache structure..."
redis_exec SET "quip:backend:cache:structure:tool-whitelist" "ready" EX 3600
redis_exec SET "quip:backend:cache:structure:problem-categories" "ready" EX 86400
redis_exec SET "quip:backend:cache:structure:server-data" "ready" EX 21600
redis_exec SET "quip:backend:cache:structure:member-data" "ready" EX 1800
redis_exec SET "quip:backend:cache:structure:assistant-session" "ready" EX 7200
redis_exec SET "quip:backend:cache:structure:temporary-data" "ready" EX 300

# Display Redis info
echo "Redis initialization completed successfully!"
echo "Redis Info:"
redis_exec INFO server | grep -E "(redis_version|os|arch_bits|process_id|uptime_in_seconds)"
redis_exec INFO memory | grep -E "(used_memory_human|maxmemory_human|maxmemory_policy)"
redis_exec INFO keyspace

echo "Application-specific keys:"
redis_exec KEYS "quip:backend:*"

echo "Redis initialization script completed!"