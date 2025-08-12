#!/bin/bash

# Redis Configuration Management Script
# This script helps manage Redis configuration files and validation

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
REDIS_DIR="${PROJECT_ROOT}/redis"

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

# Validate Redis configuration files
validate_config() {
    log_info "Validating Redis configuration files..."
    
    local errors=0
    
    # Check if redis directory exists
    if [ ! -d "$REDIS_DIR" ]; then
        log_error "Redis directory not found: $REDIS_DIR"
        return 1
    fi
    
    # Check development config
    if [ ! -f "$REDIS_DIR/redis.conf" ]; then
        log_error "Development Redis config not found: $REDIS_DIR/redis.conf"
        errors=$((errors + 1))
    else
        log_success "Development Redis config found"
        
        # Validate basic configuration
        if ! grep -q "maxmemory 256mb" "$REDIS_DIR/redis.conf"; then
            log_warning "Development config: maxmemory not set to 256mb"
        fi
        
        if ! grep -q "appendonly yes" "$REDIS_DIR/redis.conf"; then
            log_warning "Development config: AOF not enabled"
        fi
    fi
    
    # Check production config
    if [ ! -f "$REDIS_DIR/redis-prod.conf" ]; then
        log_error "Production Redis config not found: $REDIS_DIR/redis-prod.conf"
        errors=$((errors + 1))
    else
        log_success "Production Redis config found"
        
        # Validate production-specific settings
        if ! grep -q "requirepass" "$REDIS_DIR/redis-prod.conf"; then
            log_error "Production config: requirepass not configured"
            errors=$((errors + 1))
        fi
        
        if ! grep -q "maxmemory 512mb" "$REDIS_DIR/redis-prod.conf"; then
            log_warning "Production config: maxmemory not set to 512mb"
        fi
        
        if ! grep -q "rename-command FLUSHALL" "$REDIS_DIR/redis-prod.conf"; then
            log_warning "Production config: dangerous commands not disabled"
        fi
    fi
    
    # Check initialization script
    if [ ! -f "$REDIS_DIR/init-redis.sh" ]; then
        log_error "Redis initialization script not found: $REDIS_DIR/init-redis.sh"
        errors=$((errors + 1))
    else
        log_success "Redis initialization script found"
        
        # Check if script is executable
        if [ ! -x "$REDIS_DIR/init-redis.sh" ]; then
            log_warning "Redis initialization script is not executable"
            chmod +x "$REDIS_DIR/init-redis.sh"
            log_info "Made init-redis.sh executable"
        fi
    fi
    
    if [ $errors -eq 0 ]; then
        log_success "All Redis configuration files are valid"
        return 0
    else
        log_error "Found $errors configuration errors"
        return 1
    fi
}

# Show Redis configuration summary
show_config() {
    local environment=${1:-dev}
    
    log_info "Redis configuration summary for $environment environment:"
    echo
    
    if [ "$environment" = "prod" ]; then
        local config_file="$REDIS_DIR/redis-prod.conf"
    else
        local config_file="$REDIS_DIR/redis.conf"
    fi
    
    if [ ! -f "$config_file" ]; then
        log_error "Configuration file not found: $config_file"
        return 1
    fi
    
    echo "Configuration file: $config_file"
    echo
    
    # Extract key configuration values
    echo "Key Settings:"
    echo "  Port: $(grep "^port" "$config_file" | awk '{print $2}' || echo "6379 (default)")"
    echo "  Max Memory: $(grep "^maxmemory" "$config_file" | awk '{print $2}' || echo "not set")"
    echo "  Memory Policy: $(grep "^maxmemory-policy" "$config_file" | awk '{print $2}' || echo "not set")"
    echo "  AOF Enabled: $(grep "^appendonly" "$config_file" | awk '{print $2}' || echo "no")"
    echo "  Auth Required: $(grep "^requirepass" "$config_file" > /dev/null && echo "yes" || echo "no")"
    
    if [ "$environment" = "prod" ]; then
        echo
        echo "Production Security:"
        echo "  Dangerous commands disabled: $(grep "rename-command.*\"\"" "$config_file" | wc -l) commands"
        echo "  Active defragmentation: $(grep "^activedefrag" "$config_file" | awk '{print $2}' || echo "no")"
    fi
    
    echo
    echo "Persistence:"
    echo "  RDB saves: $(grep "^save" "$config_file" | wc -l) rules"
    echo "  AOF fsync: $(grep "^appendfsync" "$config_file" | awk '{print $2}' || echo "not set")"
}

# Test Redis configuration
test_config() {
    local environment=${1:-dev}
    
    log_info "Testing Redis configuration for $environment environment..."
    
    # Check if Redis container is running
    local container_name="quip-redis"
    if [ "$environment" = "prod" ]; then
        container_name="quip-redis-prod"
    fi
    
    if ! docker ps | grep -q "$container_name"; then
        log_error "Redis container '$container_name' is not running"
        log_info "Start the services first with: ./scripts/deploy-with-redis.sh start"
        return 1
    fi
    
    # Test basic connectivity
    log_info "Testing Redis connectivity..."
    if docker exec "$container_name" redis-cli ping > /dev/null 2>&1; then
        log_success "Redis is responding to ping"
    else
        log_error "Redis is not responding to ping"
        return 1
    fi
    
    # Test configuration values
    log_info "Checking configuration values..."
    
    local max_memory=$(docker exec "$container_name" redis-cli CONFIG GET maxmemory | tail -n 1)
    local memory_policy=$(docker exec "$container_name" redis-cli CONFIG GET maxmemory-policy | tail -n 1)
    
    echo "  Max Memory: $max_memory"
    echo "  Memory Policy: $memory_policy"
    
    # Test keyspace notifications
    local notify_events=$(docker exec "$container_name" redis-cli CONFIG GET notify-keyspace-events | tail -n 1)
    echo "  Keyspace Events: $notify_events"
    
    log_success "Redis configuration test completed"
}

# Generate Redis configuration template
generate_template() {
    local environment=${1:-dev}
    local output_file="$REDIS_DIR/redis-${environment}-template.conf"
    
    log_info "Generating Redis configuration template for $environment..."
    
    if [ "$environment" = "prod" ]; then
        cat > "$output_file" << 'EOF'
# Redis Production Configuration Template
# Copy this file to redis-prod.conf and customize as needed

# Network and Security
bind 0.0.0.0
port 6379
protected-mode yes
requirepass YOUR_SECURE_PASSWORD_HERE

# Memory Management
maxmemory 512mb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec

# Security - Disable dangerous commands
rename-command FLUSHDB ""
rename-command FLUSHALL ""
rename-command KEYS ""
rename-command CONFIG "CONFIG_$(openssl rand -hex 16)"

# Performance
activedefrag yes
lazyfree-lazy-eviction yes
EOF
    else
        cat > "$output_file" << 'EOF'
# Redis Development Configuration Template
# Copy this file to redis.conf and customize as needed

# Network
bind 0.0.0.0
port 6379
protected-mode yes

# Memory Management
maxmemory 256mb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec

# Development settings
loglevel notice
EOF
    fi
    
    log_success "Template generated: $output_file"
    log_info "Customize the template and rename it to remove '-template' suffix"
}

# Show help
show_help() {
    echo "Redis Configuration Management Script"
    echo
    echo "Usage: $0 <command> [options]"
    echo
    echo "Commands:"
    echo "  validate              - Validate all Redis configuration files"
    echo "  show [env]            - Show configuration summary (env: dev|prod)"
    echo "  test [env]            - Test Redis configuration (env: dev|prod)"
    echo "  template [env]        - Generate configuration template (env: dev|prod)"
    echo "  help                  - Show this help message"
    echo
    echo "Examples:"
    echo "  $0 validate"
    echo "  $0 show prod"
    echo "  $0 test dev"
    echo "  $0 template prod"
}

# Main function
main() {
    case "${1:-help}" in
        "validate")
            validate_config
            ;;
        "show")
            show_config "$2"
            ;;
        "test")
            test_config "$2"
            ;;
        "template")
            generate_template "$2"
            ;;
        "help"|*)
            show_help
            ;;
    esac
}

# Run main function
main "$@"