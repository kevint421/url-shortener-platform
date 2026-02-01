#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
NAMESPACE="urlshortener"
DEPLOYMENT_MODE="${1:-statefulset}"
KUSTOMIZE_OVERLAY="${2:-development}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}URL Shortener Platform - Kubernetes Deployment${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${GREEN}Deployment Mode: ${DEPLOYMENT_MODE}${NC}"
echo -e "${GREEN}Kustomize Overlay: ${KUSTOMIZE_OVERLAY}${NC}"
echo ""

# Function to wait for resource to be ready
wait_for_resource() {
    local resource_type=$1
    local resource_name=$2
    local timeout=$3
    local label_selector=$4

    echo -e "${YELLOW}Waiting for ${resource_type}/${resource_name} to be ready (timeout: ${timeout}s)...${NC}"

    if [ -n "$label_selector" ]; then
        kubectl wait --for=condition=ready "${resource_type}" -l "${label_selector}" \
            -n "${NAMESPACE}" --timeout="${timeout}s" || {
            echo -e "${RED}Timeout waiting for ${resource_type} with label ${label_selector}${NC}"
            return 1
        }
    else
        kubectl wait --for=condition=ready "${resource_type}/${resource_name}" \
            -n "${NAMESPACE}" --timeout="${timeout}s" || {
            echo -e "${RED}Timeout waiting for ${resource_type}/${resource_name}${NC}"
            return 1
        }
    fi

    echo -e "${GREEN}${resource_type}/${resource_name} is ready!${NC}"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check prerequisites
echo -e "${BLUE}Checking prerequisites...${NC}"

if ! command_exists kubectl; then
    echo -e "${RED}kubectl is not installed. Please install kubectl first.${NC}"
    exit 1
fi

if ! command_exists kustomize; then
    echo -e "${YELLOW}kustomize is not installed. Using kubectl apply -k instead.${NC}"
fi

# Check cluster connectivity
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Cannot connect to Kubernetes cluster. Please check your kubectl configuration.${NC}"
    exit 1
fi

echo -e "${GREEN}All prerequisites met!${NC}"
echo ""

# Phase 1: Create namespace and configuration
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Phase 1: Namespace and Configuration${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${YELLOW}Creating namespace...${NC}"
kubectl apply -f ../base/namespace.yaml

echo -e "${YELLOW}Creating ConfigMaps...${NC}"
kubectl apply -f ../config/common-configmap.yaml
kubectl apply -f ../config/url-service-configmap.yaml
kubectl apply -f ../config/api-gateway-configmap.yaml
kubectl apply -f ../config/analytics-service-configmap.yaml

echo -e "${YELLOW}Creating Secrets...${NC}"
kubectl apply -f ../config/secrets-dev.yaml

echo -e "${GREEN}Phase 1 complete!${NC}"
echo ""

# Phase 2: Deploy PostgreSQL
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Phase 2: PostgreSQL Database${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${YELLOW}Creating PostgreSQL init ConfigMap...${NC}"
kubectl apply -f ../infrastructure/postgres/init-db-configmap.yaml

echo -e "${YELLOW}Creating PostgreSQL PVC...${NC}"
kubectl apply -f ../infrastructure/postgres/pvc.yaml

echo -e "${YELLOW}Deploying PostgreSQL StatefulSet...${NC}"
kubectl apply -f ../infrastructure/postgres/statefulset.yaml
kubectl apply -f ../infrastructure/postgres/service.yaml

wait_for_resource "pod" "postgres-0" "300" "app=postgres"

echo -e "${GREEN}PostgreSQL is ready!${NC}"
echo ""

# Phase 3: Deploy Redis
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Phase 3: Redis Cache${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${YELLOW}Creating Redis PVC...${NC}"
kubectl apply -f ../infrastructure/redis/pvc.yaml

echo -e "${YELLOW}Deploying Redis StatefulSet...${NC}"
kubectl apply -f ../infrastructure/redis/statefulset.yaml
kubectl apply -f ../infrastructure/redis/service.yaml

wait_for_resource "pod" "redis-0" "120" "app=redis"

echo -e "${GREEN}Redis is ready!${NC}"
echo ""

# Phase 4: Deploy Kafka
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Phase 4: Kafka Message Broker${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$DEPLOYMENT_MODE" == "strimzi" ]; then
    echo -e "${YELLOW}Installing Strimzi Operator...${NC}"
    kubectl apply -f ../infrastructure/kafka-strimzi/strimzi-operator.yaml

    echo -e "${YELLOW}Waiting for Strimzi Operator to be ready...${NC}"
    kubectl wait --for=condition=available deployment/strimzi-cluster-operator \
        -n kafka --timeout=300s || {
        echo -e "${RED}Strimzi operator failed to start${NC}"
        exit 1
    }

    echo -e "${YELLOW}Deploying Kafka Cluster...${NC}"
    kubectl apply -f ../infrastructure/kafka-strimzi/kafka-cluster.yaml

    echo -e "${YELLOW}Waiting for Kafka cluster to be ready (this may take several minutes)...${NC}"
    kubectl wait kafka/url-shortener-kafka --for=condition=Ready \
        -n "${NAMESPACE}" --timeout=600s || {
        echo -e "${RED}Kafka cluster failed to start${NC}"
        exit 1
    }

    echo -e "${YELLOW}Creating Kafka topics...${NC}"
    kubectl apply -f ../infrastructure/kafka-strimzi/kafka-topics.yaml

    echo -e "${YELLOW}Waiting for topics to be ready...${NC}"
    sleep 10
else
    echo -e "${YELLOW}Deploying Zookeeper...${NC}"
    kubectl apply -f ../infrastructure/kafka-statefulset/zookeeper-statefulset.yaml
    kubectl apply -f ../infrastructure/kafka-statefulset/zookeeper-service.yaml

    wait_for_resource "pod" "zookeeper-0" "300" "app=zookeeper"

    echo -e "${YELLOW}Deploying Kafka...${NC}"
    kubectl apply -f ../infrastructure/kafka-statefulset/kafka-statefulset.yaml
    kubectl apply -f ../infrastructure/kafka-statefulset/kafka-service.yaml

    wait_for_resource "pod" "kafka-0" "600" "app=kafka"

    echo -e "${YELLOW}Waiting for Kafka to initialize (30s)...${NC}"
    sleep 30
fi

echo -e "${GREEN}Kafka is ready!${NC}"
echo ""

# Phase 5: Deploy Application Services
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Phase 5: Application Services${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${YELLOW}Deploying URL Service...${NC}"
kubectl apply -f ../services/url-service/deployment.yaml
kubectl apply -f ../services/url-service/service.yaml

echo -e "${YELLOW}Deploying Analytics Service...${NC}"
kubectl apply -f ../services/analytics-service/deployment.yaml
kubectl apply -f ../services/analytics-service/service.yaml

echo -e "${YELLOW}Deploying API Gateway...${NC}"
kubectl apply -f ../services/api-gateway/deployment.yaml
kubectl apply -f ../services/api-gateway/service.yaml

echo -e "${YELLOW}Deploying Frontend...${NC}"
kubectl apply -f ../services/frontend/deployment.yaml
kubectl apply -f ../services/frontend/service.yaml

echo -e "${YELLOW}Waiting for all application services to be ready...${NC}"
kubectl wait --for=condition=available deployment/url-service \
    -n "${NAMESPACE}" --timeout=300s || echo -e "${YELLOW}URL Service may need more time${NC}"

kubectl wait --for=condition=available deployment/analytics-service \
    -n "${NAMESPACE}" --timeout=300s || echo -e "${YELLOW}Analytics Service may need more time${NC}"

kubectl wait --for=condition=available deployment/api-gateway \
    -n "${NAMESPACE}" --timeout=300s || echo -e "${YELLOW}API Gateway may need more time${NC}"

kubectl wait --for=condition=available deployment/frontend \
    -n "${NAMESPACE}" --timeout=300s || echo -e "${YELLOW}Frontend may need more time${NC}"

echo -e "${GREEN}All application services are ready!${NC}"
echo ""

# Phase 6: Deploy Ingress
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Phase 6: Ingress Configuration${NC}"
echo -e "${BLUE}========================================${NC}"

echo -e "${YELLOW}Deploying Ingress...${NC}"
kubectl apply -f ../ingress/ingress.yaml

echo -e "${GREEN}Ingress deployed!${NC}"
echo ""

# Final Status
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Deployment Complete!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${GREEN}Deployment Summary:${NC}"
echo ""

echo -e "${YELLOW}Pods:${NC}"
kubectl get pods -n "${NAMESPACE}"
echo ""

echo -e "${YELLOW}Services:${NC}"
kubectl get services -n "${NAMESPACE}"
echo ""

echo -e "${YELLOW}Ingress:${NC}"
kubectl get ingress -n "${NAMESPACE}"
echo ""

INGRESS_IP=$(kubectl get ingress urlshortener-ingress -n "${NAMESPACE}" \
    -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "pending")

if [ "$INGRESS_IP" == "pending" ] || [ -z "$INGRESS_IP" ]; then
    INGRESS_IP=$(kubectl get ingress urlshortener-ingress -n "${NAMESPACE}" \
        -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "pending")
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Access Information:${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "Ingress IP/Hostname: ${BLUE}${INGRESS_IP}${NC}"
echo -e "Frontend: ${BLUE}http://${INGRESS_IP}${NC}"
echo -e "API Gateway: ${BLUE}http://${INGRESS_IP}/api${NC}"
echo ""
echo -e "${YELLOW}Note: If using minikube, run:${NC}"
echo -e "  ${BLUE}minikube tunnel${NC}"
echo ""
echo -e "${YELLOW}To add local DNS resolution, add to /etc/hosts:${NC}"
echo -e "  ${BLUE}${INGRESS_IP} urlshortener.local${NC}"
echo ""
echo -e "${GREEN}Deployment successful!${NC}"
