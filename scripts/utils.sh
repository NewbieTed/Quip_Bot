#!/bin/bash

# Utility script for common Quip Backend operations
# This script provides shortcuts for common development and maintenance tasks

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if services are running
check_services() {
    log_info "Checking service status..."
    
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    echo "Development services:"
    $COMPOSE_CMD ps
    
    echo
    echo "Production services:"
    $COMPOSE_CMD -f docker-compose.prod.yml ps 2>/dev/null || echo "Production services not running"
}

# View logs for specific service
view_logs() {
    local service=${1:-}
    local environment=${2:-dev}
    
    if [ -z "$service" ]; then
        log_error "Please specify a service name"
        echo "Available services: quip-backend-app, quip-redis, quip-db, quip-agent, quip-mcp-server, quip-frontend"
        return 1
    fi
    
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    if [ "$environment" = "prod" ]; then
        log_info "Viewing production logs for $service..."
        $COMPOSE_CMD -f docker-compose.prod.yml logs -f "$service"
    else
        log_info "Viewing development logs for $service..."
        $COMPOSE_CMD logs -f "$service"
    fi
}

# Redis CLI access
redis_cli() {
    local environment=${1:-dev}
    
    if [ "$environment" = "prod" ]; then
        log_info "Connecting to production Redis..."
        docker exec -it quip-redis-prod redis-cli
    else
        log_info "Connecting to development Redis..."
        docker exec -it quip-redis redis-cli
    fi
}

# Database CLI access
db_cli() {
    local environment=${1:-dev}
    
    # Load environment variables
    if [ -f "$PROJECT_ROOT/.env" ]; then
        source "$PROJECT_ROOT/.env"
    fi
    
    if [ "$environment" = "prod" ]; then
        log_info "Connecting to production database..."
        docker exec -it quip-db-prod psql -U "$DB_USER" -d "$DB_NAME"
    else
        log_info "Connecting to development database..."
        docker exec -it quip-db psql -U "$DB_USER" -d "$DB_NAME"
    fi
}

# Clean up Docker resources
cleanup() {
    log_info "Cleaning up Docker resources..."
    
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    # Stop all services
    $COMPOSE_CMD down 2>/dev/null || true
    $COMPOSE_CMD -f docker-compose.prod.yml down 2>/dev/null || true
    
    # Remove unused Docker resources
    docker system prune -f
    
    log_success "Cleanup completed"
}

# Reset Redis data
reset_redis() {
    local environment=${1:-dev}
    
    log_warning "This will delete all Redis data!"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        if [ "$environment" = "prod" ]; then
            log_info "Resetting production Redis data..."
            docker exec quip-redis-prod redis-cli FLUSHALL
        else
            log_info "Resetting development Redis data..."
            docker exec quip-redis redis-cli FLUSHALL
        fi
        log_success "Redis data reset completed"
    else
        log_info "Operation cancelled"
    fi
}

# Show cache statistics
cache_stats() {
    local environment=${1:-dev}
    local port=8080
    
    if [ "$environment" = "prod" ]; then
        port=${BACKEND_PORT:-8080}
    fi
    
    log_info "Fetching cache statistics..."
    
    echo "Cache Hit/Miss Ratios:"
    curl -s "http://localhost:${port}/actuator/metrics/cache.gets" | jq -r '.measurements[] | select(.statistic=="COUNT") | "Gets: \(.value)"' 2>/dev/null || echo "Cache metrics not available"
    
    echo
    echo "Redis Info:"
    if [ "$environment" = "prod" ]; then
        docker exec quip-redis-prod redis-cli info stats | grep -E "(keyspace_hits|keyspace_misses|used_memory_human)"
    else
        docker exec quip-redis redis-cli info stats | grep -E "(keyspace_hits|keyspace_misses|used_memory_human)"
    fi
}

# Validate Redis configuration
validate_redis_config() {
    local environment=${1:-dev}
    
    log_info "Validating Redis configuration for $environment..."
    
    if [ -f "$SCRIPT_DIR/redis-config.sh" ]; then
        "$SCRIPT_DIR/redis-config.sh" validate
        "$SCRIPT_DIR/redis-config.sh" show "$environment"
    else
        log_warning "Redis configuration validator not found"
        
        # Basic validation
        if [ "$environment" = "prod" ]; then
            local config_file="$PROJECT_ROOT/redis/redis-prod.conf"
        else
            local config_file="$PROJECT_ROOT/redis/redis.conf"
        fi
        
        if [ -f "$config_file" ]; then
            log_success "Redis config file found: $config_file"
        else
            log_error "Redis config file not found: $config_file"
        fi
    fi
}

# Show help
show_help() {
    echo "Quip Backend Utility Script"
    echo
    echo "Usage: $0 <command> [options]"
    echo
    echo "Commands:"
    echo "  status                    - Check service status"
    echo "  logs <service> [env]      - View logs for a service (env: dev|prod)"
    echo "  redis-cli [env]           - Connect to Redis CLI (env: dev|prod)"
    echo "  db-cli [env]              - Connect to database CLI (env: dev|prod)"
    echo "  cleanup                   - Clean up Docker resources"
    echo "  reset-redis [env]         - Reset Redis data (env: dev|prod)"
    echo "  cache-stats [env]         - Show cache statistics (env: dev|prod)"
    echo "  validate-redis [env]      - Validate Redis configuration (env: dev|prod)"
    echo "  help                      - Show this help message"
    echo
    echo "Examples:"
    echo "  $0 status"
    echo "  $0 logs quip-backend-app"
    echo "  $0 logs quip-redis prod"
    echo "  $0 redis-cli"
    echo "  $0 cache-stats prod"
}

# Main function
main() {
    case "${1:-help}" in
        "status")
            check_services
            ;;
        "logs")
            view_logs "$2" "$3"
            ;;
        "redis-cli")
            redis_cli "$2"
            ;;
        "db-cli")
            db_cli "$2"
            ;;
        "cleanup")
            cleanup
            ;;
        "reset-redis")
            reset_redis "$2"
            ;;
        "cache-stats")
            cache_stats "$2"
            ;;
        "validate-redis")
            validate_redis_config "$2"
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function
main "$@"