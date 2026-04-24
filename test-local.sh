#!/bin/bash

# ============================================
# Local Testing with Docker Compose (Fixed for Port 8088)
# ============================================

set -e

echo "🐳 Starting local environment with Docker Compose..."

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# [ត្រួតពិនិត្យ Docker ដូចដើម...]
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    exit 1
fi

# បង្កើត .env ប្រសិនបើមិនទាន់មាន
if [ ! -f .env ]; then
    echo -e "${YELLOW}📝 Creating .env file...${NC}"
    cp .env.example .env || touch .env
fi

# បញ្ឈប់ Container ចាស់ៗ
echo -e "${YELLOW}🛑 Stopping existing containers...${NC}"
docker-compose down 2>/dev/null || true

# Build និង Start (ប្រើលេខ Port 8088 តាម config ថ្មី)
echo -e "${YELLOW}🔨 Building Docker images...${NC}"
docker-compose build

echo -e "${YELLOW}🚀 Starting services...${NC}"
docker-compose up -d

echo -e "${YELLOW}⏳ Waiting for services to start (20s)...${NC}"
sleep 20

# --- ផ្នែកកែសម្រួលសំខាន់ ---

echo -e "${YELLOW}🏥 Checking service health...${NC}"

# Check PostgreSQL
if docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✅ PostgreSQL is running${NC}"
else
    echo -e "${RED}❌ PostgreSQL failed to start${NC}"
    exit 1
fi

# Check API (ប្តូរទៅ Port 8088)
API_HEALTH=$(curl -s http://localhost:8088/actuator/health 2>/dev/null || echo "unhealthy")
if echo "$API_HEALTH" | grep -q "UP"; then
    echo -e "${GREEN}✅ API is running on port 8088${NC}"
else
    echo -e "${YELLOW}⚠️  API is still starting... (Check logs: docker-compose logs -f api)${NC}"
fi

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}✅ Local environment is ready!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${YELLOW}🌐 Local URLs:${NC}"
echo "  API:                http://localhost:8088"
echo "  Swagger UI:         http://localhost:8088/swagger-ui.html"
echo "  Health Check:       http://localhost:8088/actuator/health"
echo ""
echo -e "${YELLOW}🧪 Quick Test:${NC}"
echo "  curl http://localhost:8088/api/v1/products"