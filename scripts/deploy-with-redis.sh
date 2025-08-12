#!/bin/bash

# Quip Backend Deployment Script with Redis Integration
# This script handles the complete deployment of the Quip Backend with Redis support

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
ENV_FILE="${PROJECT_ROOT}/.env"
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.yml"

# Default values
ENVIRONMENT=${ENVIRONMENT:-dev}
SKIP_BUILD=${SKIP_BUILD:-false}
SKIP_REDIS_INIT=${SKIP_REDIS_INIT:-false}
REDIS_HEALTH_TIMEOUT=${REDIS_HEALTH_TIMEOUT:-60}

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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Docker is installed and running
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! docker info &> /dev/null; then
        log_error "Docker is not running. Please start Docker first."
        exit 1
    fi
    
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not available. Please install Docker Compose."
        exit 1
    fi
    
    # Check if .env file exists
    if [ ! -f "$ENV_FILE" ]; then
        log_warning ".env file not found. Creating from .env.example..."
        if [ -f "${PROJECT_ROOT}/.env.example" ]; then
            cp "${PROJECT_ROOT}/.env.example" "$ENV_FILE"
            log_info "Please edit .env file with your configuration before running again."
            exit 1
        else
            log_error ".env.example file not found. Cannot create .env file."
            exit 1
        fi
    fi
    
    # Check Redis configuration files
    log_info "Checking Redis configuration files..."
    if [ ! -f "${PROJECT_ROOT}/redis/redis.conf" ]; then
        log_error "Redis configuration file not found: ${PROJECT_ROOT}/redis/redis.conf"
        exit 1
    fi
    
    if [ ! -f "${PROJECT_ROOT}/redis/init-redis.sh" ]; then
        log_error "Redis initialization script not found: ${PROJECT_ROOT}/redis/init-redis.sh"
        exit 1
    fi
    
    # Make init script executable if it isn't
    if [ ! -x "${PROJECT_ROOT}/redis/init-redis.sh" ]; then
        chmod +x "${PROJECT_ROOT}/redis/init-redis.sh"
        log_info "Made Redis initialization script executable"
    fi
    
    log_success "Prerequisites check completed."
}

# Load environment variables
load_environment() {
    log_info "Loading environment configuration..."
    
    # Source the .env file
    if [ -f "$ENV_FILE" ]; then
        set -a
        source "$ENV_FILE"
        set +a
        log_success "Environment variables loaded from $ENV_FILE"
    else
        log_error "Environment file $ENV_FILE not found."
        exit 1
    fi
    
    # Set default values for Redis if not provided
    export REDIS_ENABLED=${REDIS_ENABLED:-true}
    export REDIS_HOST=${REDIS_HOST:-quip-redis}
    export REDIS_PORT=${REDIS_PORT:-6379}
    export REDIS_PASSWORD=${REDIS_PASSWORD:-}
    export REDIS_DATABASE=${REDIS_DATABASE:-0}
    
    log_info "Redis configuration: Host=$REDIS_HOST, Port=$REDIS_PORT, Database=$REDIS_DATABASE"
}

# Build services
build_services() {
    if [ "$SKIP_BUILD" = "true" ]; then
        log_info "Skipping build step as requested."
        return
    fi
    
    log_info "Building services..."
    
    # Change to project root for docker-compose commands
    cd "$PROJECT_ROOT"
    
    # Use docker-compose or docker compose based on availability
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    $COMPOSE_CMD -f "$COMPOSE_FILE" build --no-cache
    log_success "Services built successfully."
}

# Start services
start_services() {
    log_info "Starting services..."
    
    # Change to project root for docker-compose commands
    cd "$PROJECT_ROOT"
    
    # Use docker-compose or docker compose based on availability
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    # Start services in the correct order
    log_info "Starting database and Redis services..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" up -d quip-db quip-redis
    
    # Wait for database and Redis to be healthy
    log_info "Waiting for database and Redis to be ready..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" exec quip-db pg_isready -U "$DB_USER" -d "$DB_NAME" || {
        log_error "Database failed to start properly."
        exit 1
    }
    
    # Check Redis health
    wait_for_redis
    
    # Start remaining services
    log_info "Starting application services..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
    
    log_success "All services started successfully."
}

# Wait for Redis to be ready
wait_for_redis() {
    log_info "Waiting for Redis to be ready..."
    
    local timeout=$REDIS_HEALTH_TIMEOUT
    local counter=0
    
    while [ $counter -lt $timeout ]; do
        if docker exec quip-redis redis-cli ping > /dev/null 2>&1; then
            log_success "Redis is ready!"
            return 0
        fi
        
        log_info "Redis not ready yet, waiting... ($counter/$timeout)"
        sleep 1
        counter=$((counter + 1))
    done
    
    log_error "Redis failed to start within $timeout seconds."
    return 1
}

# Initialize Redis
initialize_redis() {
    if [ "$SKIP_REDIS_INIT" = "true" ]; then
        log_info "Skipping Redis initialization as requested."
        return
    fi
    
    log_info "Initializing Redis with application-specific configuration..."
    
    # Check if Redis initialization script exists
    local redis_init_script="${PROJECT_ROOT}/redis/init-redis.sh"
    if [ -f "$redis_init_script" ]; then
        # Copy initialization script to Redis container and execute
        docker cp "$redis_init_script" quip-redis:/tmp/init-redis.sh
        docker exec quip-redis chmod +x /tmp/init-redis.sh
        docker exec -e REDIS_HOST=localhost -e REDIS_PORT=6379 -e REDIS_PASSWORD="$REDIS_PASSWORD" -e REDIS_DATABASE="$REDIS_DATABASE" quip-redis /tmp/init-redis.sh
        log_success "Redis initialization completed."
    else
        log_warning "Redis initialization script not found at $redis_init_script. Skipping initialization."
    fi
}

# Health check
health_check() {
    log_info "Performing health checks..."
    
    # Check Redis health
    if docker exec quip-redis redis-cli ping > /dev/null 2>&1; then
        log_success "Redis health check: OK"
    else
        log_error "Redis health check: FAILED"
        return 1
    fi
    
    # Check database health
    if docker exec quip-db pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
        log_success "Database health check: OK"
    else
        log_error "Database health check: FAILED"
        return 1
    fi
    
    # Wait a bit for backend to start
    log_info "Waiting for backend service to be ready..."
    sleep 10
    
    # Check backend health
    local backend_health_url="http://localhost:8080/actuator/health"
    if curl -f "$backend_health_url" > /dev/null 2>&1; then
        log_success "Backend health check: OK"
        
        # Check Redis integration in backend
        local redis_health_url="http://localhost:8080/actuator/health/redis"
        if curl -f "$redis_health_url" > /dev/null 2>&1; then
            log_success "Backend Redis integration: OK"
        else
            log_warning "Backend Redis integration: Not available (may be disabled)"
        fi
    else
        log_warning "Backend health check: Not ready yet (this is normal during startup)"
    fi
    
    log_success "Health checks completed."
}

# Display service information
display_info() {
    log_info "Deployment completed successfully!"
    echo
    echo "Service URLs:"
    echo "  Backend:     http://localhost:8080"
    echo "  Frontend:    http://localhost:3000"
    echo "  Agent:       http://localhost:5001"
    echo "  MCP Server:  http://localhost:8000"
    echo
    echo "Health Endpoints:"
    echo "  Backend Health:      http://localhost:8080/actuator/health"
    echo "  Redis Health:        http://localhost:8080/actuator/health/redis"
    echo "  Cache Metrics:       http://localhost:8080/actuator/metrics/cache.gets"
    echo
    echo "Redis Information:"
    echo "  Host: $REDIS_HOST"
    echo "  Port: $REDIS_PORT"
    echo "  Database: $REDIS_DATABASE"
    echo
    echo "Useful Commands:"
    echo "  View logs:           docker-compose logs -f"
    echo "  Redis CLI:           docker exec -it quip-redis redis-cli"
    echo "  Stop services:       docker-compose down"
    echo "  Restart services:    docker-compose restart"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up..."
    
    # Change to project root for docker-compose commands
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    $COMPOSE_CMD -f "$COMPOSE_FILE" down
    log_success "Services stopped."
}

# Main deployment function
main() {
    log_info "Starting Quip Backend deployment with Redis integration..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Project root: $PROJECT_ROOT"
    
    # Handle script arguments
    case "${1:-deploy}" in
        "deploy")
            check_prerequisites
            load_environment
            build_services
            start_services
            initialize_redis
            health_check
            display_info
            ;;
        "start")
            load_environment
            start_services
            initialize_redis
            health_check
            display_info
            ;;
        "stop")
            cleanup
            ;;
        "restart")
            cleanup
            load_environment
            start_services
            initialize_redis
            health_check
            display_info
            ;;
        "health")
            load_environment
            health_check
            ;;
        "init-redis")
            load_environment
            initialize_redis
            ;;
        *)
            echo "Usage: $0 {deploy|start|stop|restart|health|init-redis}"
            echo
            echo "Commands:"
            echo "  deploy     - Full deployment (build, start, initialize)"
            echo "  start      - Start services without building"
            echo "  stop       - Stop all services"
            echo "  restart    - Stop and start services"
            echo "  health     - Run health checks"
            echo "  init-redis - Initialize Redis only"
            echo
            echo "Environment Variables:"
            echo "  ENVIRONMENT=dev|staging|prod  - Set deployment environment"
            echo "  SKIP_BUILD=true               - Skip building services"
            echo "  SKIP_REDIS_INIT=true          - Skip Redis initialization"
            echo "  REDIS_HEALTH_TIMEOUT=60       - Redis health check timeout"
            exit 1
            ;;
    esac
}

# Handle script interruption
trap cleanup INT TERM

# Run main function
main "$@"