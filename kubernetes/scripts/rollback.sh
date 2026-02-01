#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

NAMESPACE="urlshortener"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}URL Shortener Platform - Rollback${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to rollback deployment
rollback_deployment() {
    local deployment=$1
    echo -e "${YELLOW}Rolling back ${deployment}...${NC}"
    kubectl rollout undo deployment/${deployment} -n ${NAMESPACE}
    kubectl rollout status deployment/${deployment} -n ${NAMESPACE}
}

# Check if specific deployment is provided
if [ -n "$1" ]; then
    rollback_deployment "$1"
else
    echo -e "${YELLOW}Rolling back all application deployments...${NC}"
    echo ""

    rollback_deployment "url-service"
    rollback_deployment "analytics-service"
    rollback_deployment "api-gateway"
    rollback_deployment "frontend"
fi

echo ""
echo -e "${GREEN}Rollback complete!${NC}"
echo ""

echo -e "${YELLOW}Current deployment status:${NC}"
kubectl get deployments -n ${NAMESPACE}
