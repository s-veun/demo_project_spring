#!/bin/bash

# ============================================
# Build and Test Docker Image Locally
# ============================================

set -e

echo "🐳 Building and testing Docker image..."

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
IMAGE_NAME="ecommerce-api"
IMAGE_TAG="latest"
CONTAINER_NAME="ecommerce-api-test"
PORT=8080

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed${NC}"
    echo "Install from: https://docs.docker.com/get-docker/"
    exit 1
fi

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Docker Build & Test${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Step 1: Build image
echo -e "${YELLOW}Step 1/4: Building Docker image...${NC}"
docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Image built successfully${NC}"
else
    echo -e "${RED}❌ Image build failed${NC}"
    exit 1
fi

# Step 2: Stop existing container
echo -e "${YELLOW}Step 2/4: Cleaning up existing containers...${NC}"
docker stop ${CONTAINER_NAME} 2>/dev/null || true
docker rm ${CONTAINER_NAME} 2>/dev/null || true
echo -e "${GREEN}✅ Cleanup complete${NC}"

# Step 3: Run container
echo -e "${YELLOW}Step 3/4: Starting container...${NC}"
docker run -d \
  --name ${CONTAINER_NAME} \
  -p ${PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/postgres \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME:-your-cloud-name} \
  -e CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY:-your-key} \
  -e CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET:-your-secret} \
  -e JWT_SECRET=${JWT_SECRET:-test-secret-change-in-production} \
  ${IMAGE_NAME}:${IMAGE_TAG}

echo -e "${GREEN}✅ Container started${NC}"

# Step 4: Wait and test
echo -e "${YELLOW}Step 4/4: Waiting for application to start...${NC}"
echo "(This may take 30-60 seconds)"

MAX_WAIT=120
WAITED=0
INTERVAL=5

while [ $WAITED -lt $MAX_WAIT ]; do
    sleep $INTERVAL
    WAITED=$((WAITED + INTERVAL))
    
    HEALTH=$(curl -s http://localhost:${PORT}/actuator/health 2>/dev/null || echo "unhealthy")
    
    if echo "$HEALTH" | grep -q "UP"; then
        echo ""
        echo -e "${GREEN}============================================${NC}"
        echo -e "${GREEN}✅ Application is running!${NC}"
        echo -e "${GREEN}============================================${NC}"
        echo ""
        echo -e "${BLUE}📊 Service URLs:${NC}"
        echo "  Health Check:    http://localhost:${PORT}/actuator/health"
        echo "  API Base:        http://localhost:${PORT}/api/v1"
        echo "  Products:        http://localhost:${PORT}/api/v1/products"
        echo ""
        echo -e "${BLUE}🧪 Quick Tests:${NC}"
        echo "  curl http://localhost:${PORT}/actuator/health"
        echo "  curl http://localhost:${PORT}/api/v1/products"
        echo ""
        echo -e "${BLUE}📋 Docker Commands:${NC}"
        echo "  View logs:       docker logs -f ${CONTAINER_NAME}"
        echo "  Stop container:  docker stop ${CONTAINER_NAME}"
        echo "  Remove:          docker rm ${CONTAINER_NAME}"
        echo "  Restart:         docker restart ${CONTAINER_NAME}"
        echo ""
        echo -e "${YELLOW}💡 Tip: Press Ctrl+C to stop viewing, container will keep running${NC}"
        echo ""
        
        # Show logs for a bit
        echo -e "${YELLOW}📝 Recent logs:${NC}"
        docker logs --tail 20 ${CONTAINER_NAME}
        
        exit 0
    fi
    
    echo -n "."
done

echo ""
echo -e "${RED}❌ Application failed to start within ${MAX_WAIT} seconds${NC}"
echo ""
echo -e "${YELLOW}📋 Container logs:${NC}"
docker logs --tail 50 ${CONTAINER_NAME}
echo ""
echo -e "${YELLOW}🔍 Debug commands:${NC}"
echo "  docker logs -f ${CONTAINER_NAME}"
echo "  docker exec -it ${CONTAINER_NAME} sh"
echo ""

exit 1
