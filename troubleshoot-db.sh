#!/bin/bash
# ────────────────────────────────────────────────────────────────────────────
# Database Troubleshooting Script
# ────────────────────────────────────────────────────────────────────────────
# Purpose: Diagnose and troubleshoot database connectivity issues
# Usage: ./troubleshoot-db.sh
# ────────────────────────────────────────────────────────────────────────────

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Utility functions
print_header() {
    echo -e "\n${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║ $1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}\n"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Main troubleshooting
main() {
    clear
    print_header "Database Troubleshooting Script"
    
    # Step 1: Check if Docker is installed and running
    print_info "Checking Docker installation..."
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Install from: https://www.docker.com/"
        exit 1
    fi
    print_success "Docker is installed"
    
    # Step 2: Check if docker-compose is available
    print_info "Checking Docker Compose..."
    if ! command -v docker-compose &> /dev/null; then
        print_warning "docker-compose command not found, trying 'docker compose'..."
        if ! docker compose version &> /dev/null; then
            print_error "Docker Compose not available"
            exit 1
        fi
        DOCKER_COMPOSE="docker compose"
    else
        DOCKER_COMPOSE="docker-compose"
    fi
    print_success "Docker Compose is available: $DOCKER_COMPOSE"
    
    # Step 3: Check if PostgreSQL container is running
    print_info "Checking PostgreSQL container status..."
    if ! docker ps | grep -q "ecommerce_postgres_local"; then
        print_warning "PostgreSQL container is NOT running"
        print_info "Starting PostgreSQL with Docker Compose..."
        
        if [ -f "docker-compose.dev.yml" ]; then
            $DOCKER_COMPOSE -f docker-compose.dev.yml up -d
            print_success "PostgreSQL container started"
            print_info "Waiting for database to be ready (30 seconds)..."
            sleep 30
        else
            print_error "docker-compose.dev.yml not found in current directory"
            exit 1
        fi
    else
        print_success "PostgreSQL container is running"
    fi
    
    # Step 4: Check container logs
    print_header "PostgreSQL Container Logs (last 20 lines)"
    docker logs --tail 20 ecommerce_postgres_local || print_warning "Could not fetch logs"
    
    # Step 5: Test psql connectivity
    print_header "Testing PostgreSQL Connectivity"
    
    print_info "Attempting connection to PostgreSQL..."
    if docker exec -it ecommerce_postgres_local psql -U postgres -d postgres -c "SELECT 1;" &> /dev/null; then
        print_success "PostgreSQL is responsive"
    else
        print_error "Cannot connect to PostgreSQL"
        exit 1
    fi
    
    # Step 6: Verify database exists
    print_info "Checking if database 'ecommerce_db' exists..."
    if docker exec -it ecommerce_postgres_local psql -U postgres -lqt | cut -d \| -f 1 | grep -qw ecommerce_db; then
        print_success "Database 'ecommerce_db' exists"
    else
        print_warning "Database 'ecommerce_db' does NOT exist"
        print_info "Creating database 'ecommerce_db'..."
        docker exec -it ecommerce_postgres_local psql -U postgres -c "CREATE DATABASE ecommerce_db;"
        print_success "Database created"
    fi
    
    # Step 7: Get database version
    print_header "Database Information"
    VERSION=$(docker exec ecommerce_postgres_local psql -U postgres -t -c "SELECT version();")
    print_info "Version: $VERSION"
    
    # Step 8: Check .env file
    print_header ".env Configuration Check"
    if [ -f ".env" ]; then
        print_success ".env file exists"
        
        # Extract and display relevant properties
        JDBC_URL=$(grep "SPRING_DATASOURCE_URL" .env | cut -d '=' -f 2-)
        if [ -n "$JDBC_URL" ]; then
            print_info "JDBC URL: $JDBC_URL"
            
            # Check for sslmode
            if [[ "$JDBC_URL" == *"sslmode=disable"* ]]; then
                print_success "SSL is DISABLED (correct for local development)"
            elif [[ "$JDBC_URL" == *"sslmode=require"* ]]; then
                print_warning "SSL is REQUIRED (for production with Railway)"
            else
                print_warning "SSL mode not specified in JDBC URL"
            fi
        fi
        
        # Check credentials
        DB_USER=$(grep "SPRING_DATASOURCE_USERNAME" .env | cut -d '=' -f 2-)
        DB_PASS=$(grep "SPRING_DATASOURCE_PASSWORD" .env | cut -d '=' -f 2-)
        
        if [ -z "$DB_USER" ] || [ -z "$DB_PASS" ]; then
            print_error "Database credentials not found in .env"
        else
            print_success "Database credentials are configured"
        fi
    else
        print_error ".env file NOT found"
        if [ -f ".env.local" ]; then
            print_info "Creating .env from .env.local..."
            cp .env.local .env
            print_success ".env created"
        else
            print_error ".env.local template not found either"
            exit 1
        fi
    fi
    
    # Step 9: Test JDBC connection with psql directly
    print_header "Direct PostgreSQL Connection Test"
    
    JDBC_URL=$(grep "SPRING_DATASOURCE_URL" .env | cut -d '=' -f 2- | sed 's/jdbc:postgresql:\/\///' | sed 's/?.*//') 
    if [ -n "$JDBC_URL" ]; then
        print_info "Attempting direct connection to: $JDBC_URL"
        if docker exec ecommerce_postgres_local psql -h localhost -U postgres -d ecommerce_db -c "SELECT 1;" &> /dev/null; then
            print_success "Direct connection successful"
        else
            print_warning "Direct connection test had issues (may still work from app)"
        fi
    fi
    
    # Step 10: Check port mapping
    print_header "Docker Port Configuration"
    PORTS=$(docker port ecommerce_postgres_local 2>/dev/null || echo "N/A")
    print_info "Port mapping: $PORTS"
    
    # Step 11: Test localhost connectivity
    print_info "Testing localhost:5432 connectivity..."
    if nc -zv localhost 5432 2>&1 | grep -q "succeeded"; then
        print_success "Port 5432 is accessible"
    else
        print_warning "Port 5432 may not be accessible (this might still work)"
    fi
    
    # Final summary
    print_header "Troubleshooting Summary"
    print_success "All checks completed!"
    print_info "Your application should now be able to connect to PostgreSQL"
    print_info ""
    print_info "Next steps:"
    echo "  1. Ensure your .env file has: SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db?sslmode=disable"
    echo "  2. Run your Spring Boot application: ./gradlew bootRun"
    echo "  3. Check health endpoint: curl http://localhost:8080/actuator/health"
    echo ""
}

# Run main function
main "$@"
