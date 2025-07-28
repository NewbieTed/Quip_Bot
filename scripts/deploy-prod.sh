#!/bin/bash

# Production Deployment Script for Quip Backend with Redis
# This script handles production deployment using docker-compose.prod.yml

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
COMPOSE_FILE="${PROJECT_ROOT}/docker-compose.prod.yml"

# Default values
SKIP_BUILD=${SKIP_BUILD:-false}
REDIS_HEALTH_TIMEOUT=${REDIS_HEALTH_TIMEOUT:-120}

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

# Check production prerequisites
check_prerequisites() {
    log_info "Checking production prerequisites..."
    
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
    
    # Check if production env file exists
    if [ ! -f "$ENV_FILE" ]; then
        log_error "Production .env file not found at $ENV_FILE"
        log_error "Please create a production .env file based on .env.prod.example"
        exit 1
    fi
    
    # Validate required production environment variables
    source "$ENV_FILE"
    
    if [ -z "$REDIS_PASSWORD" ]; then
        log_error "REDIS_PASSWORD must be set in production environment"
        exit 1
    fi
    
    if [ -z "$DB_PASSWORD" ]; then
        log_error "DB_PASSWORD must be set in production environment"
        exit 1
    fi
    
    log_success "Production prerequisites check completed."
}

# Load environment variables
load_environment() {
    log_info "Loading production environment configuration..."
    
    # Source the .env file
    if [ -f "$ENV_FILE" ]; then
        set -a
        source "$ENV_FILE"
        set +a
        log_success "Production environment variables loaded from $ENV_FILE"
    else
        log_error "Environment file $ENV_FILE not found."
        exit 1
    fi
    
    # Set production defaults
    export REDIS_ENABLED=${REDIS_ENABLED:-true}
    export REDIS_HOST=${REDIS_HOST:-quip-redis}
    export REDIS_PORT=${REDIS_PORT:-6379}
    export REDIS_DATABASE=${REDIS_DATABASE:-0}
    
    log_info "Production Redis configuration: Host=$REDIS_HOST, Port=$REDIS_PORT, Database=$REDIS_DATABASE"
}

# Build services for production
build_services() {
    if [ "$SKIP_BUILD" = "true" ]; then
        log_info "Skipping build step as requested."
        return
    fi
    
    log_info "Building services for production..."
    
    # Change to project root for docker-compose commands
    cd "$PROJECT_ROOT"
    
    # Use docker-compose or docker compose based on availability
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    $COMPOSE_CMD -f "$COMPOSE_FILE" build --no-cache
    log_success "Production services built successfully."
}

# Start production services
start_services() {
    log_info "Starting production services..."
    
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
    
    # Wait for database
    local db_ready=false
    local counter=0
    while [ $counter -lt 60 ] && [ "$db_ready" = false ]; do
        if $COMPOSE_CMD -f "$COMPOSE_FILE" exec quip-db pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
            db_ready=true
            log_success "Database is ready!"
        else
            log_info "Database not ready yet, waiting... ($counter/60)"
            sleep 2
            counter=$((counter + 1))
        fi
    done
    
    if [ "$db_ready" = false ]; then
        log_error "Database failed to start within timeout."
        exit 1
    fi
    
    # Check Redis health
    wait_for_redis
    
    # Start remaining services
    log_info "Starting application services..."
    $COMPOSE_CMD -f "$COMPOSE_FILE" up -d
    
    log_success "All production services started successfully."
}

# Wait for Redis to be ready (production version with longer timeout)
wait_for_redis() {
    log_info "Waiting for Redis to be ready..."
    
    local timeout=$REDIS_HEALTH_TIMEOUT
    local counter=0
    
    while [ $counter -lt $timeout ]; do
        if docker exec quip-redis-prod redis-cli ping > /dev/null 2>&1; then
            log_success "Redis is ready!"
            return 0
        fi
        
        log_info "Redis not ready yet, waiting... ($counter/$timeout)"
        sleep 2
        counter=$((counter + 2))
    done
    
    log_error "Redis failed to start within $timeout seconds."
    return 1
}

# Production health check
health_check() {
    log_info "Performing production health checks..."
    
    # Check Redis health
    if docker exec quip-redis-prod redis-cli ping > /dev/null 2>&1; then
        log_success "Redis health check: OK"
    else
        log_error "Redis health check: FAILED"
        return 1
    fi
    
    # Check database health
    if docker exec quip-db-prod pg_isready -U "$DB_USER" -d "$DB_NAME" > /dev/null 2>&1; then
        log_success "Database health check: OK"
    else
        log_error "Database health check: FAILED"
        return 1
    fi
    
    # Wait longer for backend to start in production
    log_info "Waiting for backend service to be ready (production startup may take longer)..."
    sleep 30
    
    # Check backend health
    local backend_port=${BACKEND_PORT:-8080}
    local backend_health_url="http://localhost:${backend_port}/actuator/health"
    
    local backend_ready=false
    local counter=0
    while [ $counter -lt 30 ] && [ "$backend_ready" = false ]; do
        if curl -f "$backend_health_url" > /dev/null 2>&1; then
            backend_ready=true
            log_success "Backend health check: OK"
            
            # Check Redis integration in backend
            local redis_health_url="http://localhost:${backend_port}/actuator/health/redis"
            if curl -f "$redis_health_url" > /dev/null 2>&1; then
                log_success "Backend Redis integration: OK"
            else
                log_warning "Backend Redis integration: Not available"
            fi
        else
            log_info "Backend not ready yet, waiting... ($counter/30)"
            sleep 5
            counter=$((counter + 1))
        fi
    done
    
    if [ "$backend_ready" = false ]; then
        log_warning "Backend health check: Not ready within timeout (may still be starting)"
    fi
    
    log_success "Production health checks completed."
}

# Display production service information
display_info() {
    log_info "Production deployment completed successfully!"
    echo
    echo "Production Service URLs:"
    echo "  Backend:     http://localhost:${BACKEND_PORT:-8080}"
    echo "  Frontend:    http://localhost:${FRONTEND_PORT:-3000}"
    echo "  Agent:       http://localhost:${AGENT_PORT:-5001}"
    echo "  MCP Server:  http://localhost:${MCP_SERVER_PORT:-8000}"
    echo
    echo "Health Endpoints:"
    echo "  Backend Health:      http://localhost:${BACKEND_PORT:-8080}/actuator/health"
    echo "  Redis Health:        http://localhost:${BACKEND_PORT:-8080}/actuator/health/redis"
    echo "  Cache Metrics:       http://localhost:${BACKEND_PORT:-8080}/actuator/metrics/cache.gets"
    echo
    echo "Production Redis Information:"
    echo "  Host: $REDIS_HOST"
    echo "  Port: $REDIS_PORT"
    echo "  Database: $REDIS_DATABASE"
    echo "  Password: [CONFIGURED]"
    echo
    echo "Useful Production Commands:"
    echo "  View logs:           docker-compose -f docker-compose.prod.yml logs -f"
    echo "  Redis CLI:           docker exec -it quip-redis-prod redis-cli"
    echo "  Stop services:       docker-compose -f docker-compose.prod.yml down"
    echo "  Restart services:    docker-compose -f docker-compose.prod.yml restart"
    echo
    echo "Monitoring:"
    echo "  Container stats:     docker stats"
    echo "  Service status:      docker-compose -f docker-compose.prod.yml ps"
}

# Cleanup function
cleanup() {
    log_info "Stopping production services..."
    
    # Change to project root for docker-compose commands
    cd "$PROJECT_ROOT"
    
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi
    
    $COMPOSE_CMD -f "$COMPOSE_FILE" down
    log_success "Production services stopped."
}

# Main deployment function
main() {
    log_info "Starting Quip Backend PRODUCTION deployment with Redis integration..."
    log_info "Project root: $PROJECT_ROOT"
    
    # Handle script arguments
    case "${1:-deploy}" in
        "deploy")
            check_prerequisites
            load_environment
            build_services
            start_services
            health_check
            display_info
            ;;
        "start")
            load_environment
            start_services
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
            health_check
            display_info
            ;;
        "health")
            load_environment
            health_check
            ;;
        *)
            echo "Usage: $0 {deploy|start|stop|restart|health}"
            echo
            echo "Commands:"
            echo "  deploy     - Full production deployment (build, start, health check)"
            echo "  start      - Start production services without building"
            echo "  stop       - Stop all production services"
            echo "  restart    - Stop and start production services"
            echo "  health     - Run production health checks"
            echo
            echo "Environment Variables:"
            echo "  SKIP_BUILD=true               - Skip building services"
            echo "  REDIS_HEALTH_TIMEOUT=120     - Redis health check timeout"
            exit 1
            ;;
    esac
}

# Handle script interruption
trap cleanup INT TERM

# Run main function
main "$@"