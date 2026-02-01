#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Building Docker Images${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if we're in the correct directory
if [ ! -f "docker-compose.yml" ]; then
    echo -e "${RED}Error: Must run from project root directory${NC}"
    echo "cd url-shortener-platform"
    exit 1
fi

# Build URL Service
echo -e "${YELLOW}Building URL Service...${NC}"
docker build -t url-shortener/url-service:latest -f url-service/Dockerfile .
echo -e "${GREEN}✓ URL Service built${NC}"
echo ""

# Build Analytics Service
echo -e "${YELLOW}Building Analytics Service...${NC}"
docker build -t url-shortener/analytics-service:latest -f analytics-service/Dockerfile .
echo -e "${GREEN}✓ Analytics Service built${NC}"
echo ""

# Build API Gateway
echo -e "${YELLOW}Building API Gateway...${NC}"
docker build -t url-shortener/api-gateway:latest -f api-gateway/Dockerfile .
echo -e "${GREEN}✓ API Gateway built${NC}"
echo ""

# Build Frontend
echo -e "${YELLOW}Building Frontend...${NC}"
docker build -t url-shortener/frontend:latest frontend/
echo -e "${GREEN}✓ Frontend built${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All images built successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if Minikube is running
if minikube status &> /dev/null; then
    echo -e "${YELLOW}Minikube is running. Load images? (y/n)${NC}"
    read -r LOAD_IMAGES

    if [ "$LOAD_IMAGES" = "y" ] || [ "$LOAD_IMAGES" = "Y" ]; then
        echo ""
        echo -e "${BLUE}Loading images into Minikube...${NC}"

        minikube image load url-shortener/url-service:latest
        echo -e "${GREEN}✓ URL Service loaded${NC}"

        minikube image load url-shortener/analytics-service:latest
        echo -e "${GREEN}✓ Analytics Service loaded${NC}"

        minikube image load url-shortener/api-gateway:latest
        echo -e "${GREEN}✓ API Gateway loaded${NC}"

        minikube image load url-shortener/frontend:latest
        echo -e "${GREEN}✓ Frontend loaded${NC}"

        echo ""
        echo -e "${GREEN}All images loaded into Minikube!${NC}"
    fi
else
    echo -e "${YELLOW}Note: Minikube is not running.${NC}"
    echo "To load images later, run:"
    echo "  minikube image load url-shortener/url-service:latest"
    echo "  minikube image load url-shortener/analytics-service:latest"
    echo "  minikube image load url-shortener/api-gateway:latest"
    echo "  minikube image load url-shortener/frontend:latest"
fi

echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "1. cd kubernetes/scripts"
echo "2. ./deploy.sh statefulset development"
echo ""
