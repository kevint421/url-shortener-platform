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
echo -e "${BLUE}URL Shortener Platform - Cleanup${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

read -p "This will delete ALL resources in namespace '${NAMESPACE}'. Continue? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${YELLOW}Cleanup cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${YELLOW}Deleting all resources in namespace ${NAMESPACE}...${NC}"

# Delete in reverse order of creation
echo -e "${YELLOW}Deleting Ingress...${NC}"
kubectl delete ingress --all -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting Application Services...${NC}"
kubectl delete deployment --all -n ${NAMESPACE} --ignore-not-found=true
kubectl delete service --all -n ${NAMESPACE} --ignore-not-found=true
kubectl delete hpa --all -n ${NAMESPACE} --ignore-not-found=true
kubectl delete pdb --all -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting Kafka resources...${NC}"
kubectl delete kafka --all -n ${NAMESPACE} --ignore-not-found=true
kubectl delete kafkatopic --all -n ${NAMESPACE} --ignore-not-found=true
kubectl delete statefulset kafka zookeeper -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting Redis...${NC}"
kubectl delete statefulset redis -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting PostgreSQL...${NC}"
kubectl delete statefulset postgres -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting ConfigMaps and Secrets...${NC}"
kubectl delete configmap --all -n ${NAMESPACE} --ignore-not-found=true
kubectl delete secret --all -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting PVCs (data will be lost)...${NC}"
kubectl delete pvc --all -n ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting namespace...${NC}"
kubectl delete namespace ${NAMESPACE} --ignore-not-found=true

echo -e "${YELLOW}Deleting Strimzi operator (if exists)...${NC}"
kubectl delete deployment strimzi-cluster-operator -n kafka --ignore-not-found=true
kubectl delete namespace kafka --ignore-not-found=true

echo ""
echo -e "${GREEN}Cleanup complete!${NC}"
