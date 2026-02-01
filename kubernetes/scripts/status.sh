#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

NAMESPACE="urlshortener"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}URL Shortener Platform - Status${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}Namespace:${NC}"
kubectl get namespace ${NAMESPACE} 2>/dev/null || echo -e "${RED}Namespace not found${NC}"
echo ""

echo -e "${YELLOW}Pods:${NC}"
kubectl get pods -n ${NAMESPACE} -o wide
echo ""

echo -e "${YELLOW}StatefulSets:${NC}"
kubectl get statefulsets -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Deployments:${NC}"
kubectl get deployments -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Services:${NC}"
kubectl get services -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Ingress:${NC}"
kubectl get ingress -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}PersistentVolumeClaims:${NC}"
kubectl get pvc -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}ConfigMaps:${NC}"
kubectl get configmaps -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Secrets:${NC}"
kubectl get secrets -n ${NAMESPACE}
echo ""

echo -e "${YELLOW}Events (last 10):${NC}"
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' | tail -10
echo ""

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Health Check${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Check if all deployments are ready
DEPLOYMENTS=("url-service" "analytics-service" "api-gateway" "frontend")
ALL_READY=true

for deployment in "${DEPLOYMENTS[@]}"; do
    READY=$(kubectl get deployment ${deployment} -n ${NAMESPACE} -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    DESIRED=$(kubectl get deployment ${deployment} -n ${NAMESPACE} -o jsonpath='{.status.replicas}' 2>/dev/null || echo "0")

    if [ "$READY" == "$DESIRED" ] && [ "$READY" != "0" ]; then
        echo -e "${GREEN}✓${NC} ${deployment}: ${READY}/${DESIRED} ready"
    else
        echo -e "${RED}✗${NC} ${deployment}: ${READY}/${DESIRED} ready"
        ALL_READY=false
    fi
done

echo ""

if [ "$ALL_READY" = true ]; then
    echo -e "${GREEN}All services are healthy!${NC}"
else
    echo -e "${YELLOW}Some services are not ready. Check pod logs for details.${NC}"
fi
