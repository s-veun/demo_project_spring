#!/bin/bash

# ============================================
# Quick Deploy Script for Google Cloud Run
# ============================================

set -e  # Exit on error

echo "🚀 Google Cloud Deployment Script"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Google Cloud Deployment${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check prerequisites
MISSING_DEPS=()

if ! command -v gcloud &> /dev/null; then
    MISSING_DEPS+=("gcloud")
fi

if ! command -v docker &> /dev/null; then
    MISSING_DEPS+=("docker")
fi

if [ ${#MISSING_DEPS[@]} -ne 0 ]; then
    echo -e "${RED}❌ Missing required dependencies:${NC}"
    for dep in "${MISSING_DEPS[@]}"; do
        echo -e "${RED}   - $dep${NC}"
    done
    echo ""
    echo -e "${YELLOW}📦 Install missing dependencies:${NC}"
    
    if [[ " ${MISSING_DEPS[@]} " =~ " gcloud " ]]; then
        echo ""
        echo "  Install gcloud CLI:"
        echo "    brew install --cask google-cloud-sdk"
        echo "    gcloud init"
        echo ""
    fi
    
    if [[ " ${MISSING_DEPS[@]} " =~ " docker " ]]; then
        echo "  Install Docker:"
        echo "    https://docs.docker.com/get-docker/"
        echo ""
    fi
    
    echo -e "${YELLOW}💡 ALTERNATIVE: Test locally first (no gcloud needed):${NC}"
    echo "  ./test-local.sh          # Full stack with database"
    echo "  ./build-and-test.sh      # API only"
    echo ""
    exit 1
fi

echo -e "${GREEN}✅ All prerequisites installed${NC}"

# Configuration - UPDATE THESE VALUES
PROJECT_ID="${PROJECT_ID:-project-a63e587d-e5b4-4dbc-9d7}"
REGION="${REGION:-us-central1}"
SERVICE_NAME="${SERVICE_NAME:-ecommerce-api}"
CLOUD_SQL_INSTANCE="$PROJECT_ID:$REGION:ecommerce-db"

# Verify project ID is set
if [ "$PROJECT_ID" = "your-project-id" ]; then
    echo ""
    echo -e "${RED}❌ Please update PROJECT_ID in deploy.sh${NC}"
    echo -e "${YELLOW}   Or set it as environment variable:${NC}"
    echo "   export PROJECT_ID=your-actual-project-id"
    echo ""
    echo -e "${YELLOW}   Find your project ID at: https://console.cloud.google.com${NC}"
    echo ""
    exit 1
fi

# Set project
echo -e "${YELLOW}📦 Setting project: $PROJECT_ID${NC}"
gcloud config set project $PROJECT_ID

# Enable required APIs
echo -e "${YELLOW}🔧 Enabling required APIs...${NC}"
gcloud services enable \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  sqladmin.googleapis.com \
  containerregistry.googleapis.com \
  secretmanager.googleapis.com

# Build Docker image
echo -e "${YELLOW}🐳 Building Docker image...${NC}"
docker build -t gcr.io/$PROJECT_ID/$SERVICE_NAME:latest .

# Push to Container Registry
echo -e "${YELLOW}📤 Pushing image to Container Registry...${NC}"
docker push gcr.io/$PROJECT_ID/$SERVICE_NAME:latest

# Deploy to Cloud Run
echo -e "${YELLOW}🚀 Deploying to Cloud Run...${NC}"
gcloud run deploy $SERVICE_NAME \
  --image gcr.io/$PROJECT_ID/$SERVICE_NAME:latest \
  --region $REGION \
  --platform managed \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 10 \
  --timeout 300 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod" \
  --set-secrets="DB_PASSWORD=db-password:latest,DB_USERNAME=db-username:latest" \
  --add-cloudsql-instances="$CLOUD_SQL_INSTANCE" \
  --allow-unauthenticated

# Get service URL
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region $REGION --format="value(status.url)")

echo -e "${GREEN}✅ Deployment successful!${NC}"
echo -e "${GREEN}🌐 Service URL: $SERVICE_URL${NC}"
echo -e "${GREEN}📊 Health Check: $SERVICE_URL/actuator/health${NC}"
echo -e "${GREEN}📝 API Base URL: $SERVICE_URL/api/v1${NC}"

echo ""
echo -e "${YELLOW}📋 Next Steps:${NC}"
echo "1. Test your API: curl $SERVICE_URL/api/v1/products"
echo "2. Check logs: gcloud run services logs read $SERVICE_NAME --region $REGION --limit=50"
echo "3. Monitor: https://console.cloud.google.com/run/detail/$REGION/$SERVICE_NAME"
