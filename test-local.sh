#!/bin/bash

# ============================================
# Local Testing with Docker Compose
# ============================================

set -e

echo "🐳 Starting local environment with Docker Compose..."

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    echo "Install from: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed${NC}"
    echo "Install from: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✅ Docker is running${NC}"

# Create .env file if it doesn't exist
if [ ! -f .env ]; then
    echo -e "${YELLOW}📝 Creating .env file from template...${NC}"
    cp .env.example .env
    echo -e "${YELLOW}⚠️  Please update .env file with your actual credentials${NC}"
    echo ""
    echo "Required environment variables:"
    echo "  - CLOUDINARY_CLOUD_NAME"
    echo "  - CLOUDINARY_API_KEY"
    echo "  - CLOUDINARY_API_SECRET"
    echo "  - JWT_SECRET"
    echo ""
    read -p "Press Enter to continue with defaults (or Ctrl+C to edit .env first)..."
fi

# Stop any running containers
echo -e "${YELLOW}🛑 Stopping existing containers...${NC}"
docker-compose down 2>/dev/null || true

# Build and start
echo -e "${YELLOW}🔨 Building Docker images...${NC}"
docker-compose build

echo -e "${YELLOW}🚀 Starting services...${NC}"
docker-compose up -d

# Wait for services to be ready
echo -e "${YELLOW}⏳ Waiting for services to start...${NC}"
sleep 15

# Check health
echo -e "${YELLOW}🏥 Checking service health...${NC}"

# Check PostgreSQL
if docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL is running${NC}"
else
    echo -e "${RED}❌ PostgreSQL failed to start${NC}"
    docker-compose logs postgres
    exit 1
fi

# Check API
API_HEALTH=$(curl -s http://localhost:8080/actuator/health 2>/dev/null || echo "unhealthy")
if echo "$API_HEALTH" | grep -q "UP"; then
    echo -e "${GREEN}✅ API is running${NC}"
else
    echo -e "${YELLOW}⚠️  API is starting... (check logs: docker-compose logs -f api)${NC}"
fi

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}✅ Local environment is ready!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${YELLOW}📋 Useful Commands:${NC}"
echo "  View logs:          docker-compose logs -f"
echo "  View API logs:      docker-compose logs -f api"
echo "  View DB logs:       docker-compose logs -f postgres"
echo "  Stop services:      docker-compose down"
echo "  Restart services:   docker-compose restart"
echo ""
echo -e "${YELLOW}🌐 URLs:${NC}"
echo "  API:                http://localhost:8080"
echo "  Health Check:       http://localhost:8080/actuator/health"
echo "  Products:           http://localhost:8080/api/v1/products"
echo "  H2 Console:         http://localhost:8080/h2-console (if enabled)"
echo ""
echo -e "${YELLOW}🧪 Test your API:${NC}"
echo "  curl http://localhost:8080/api/v1/products"
echo "  curl http://localhost:8080/actuator/health"
echo ""
