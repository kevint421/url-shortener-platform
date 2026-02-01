# Kubernetes Deployment 

Production-ready Kubernetes manifests for the URL Shortener Platform. This guide contains everything for deploying, managing, and troubleshooting the stack.

## Overview

This directory contains production-ready Kubernetes manifests for deploying the entire URL Shortener Platform with proper high availability, scaling, and monitoring capabilities.

### Directory Structure

```
kubernetes/
├── base/                          # Base namespace configuration
├── config/                        # ConfigMaps and Secrets
│   ├── common-configmap.yaml      # Shared settings (DB, Redis, Kafka)
│   ├── url-service-configmap.yaml # URL service specific config
│   ├── api-gateway-configmap.yaml # Gateway routing, CORS, JWT
│   ├── analytics-service-configmap.yaml
│   └── secrets-dev.yaml           # Development secrets
├── infrastructure/                # StatefulSets for databases
│   ├── postgres/                  # PostgreSQL with init scripts
│   ├── redis/                     # Redis cache with AOF persistence
│   ├── kafka-strimzi/             # Kafka via Strimzi operator (production)
│   └── kafka-statefulset/         # Kafka via StatefulSet (development)
├── services/                      # Application deployments
│   ├── url-service/               # URL shortening service
│   ├── analytics-service/         # Event processing and analytics
│   ├── api-gateway/               # Authentication and routing
│   └── frontend/                  # React frontend
├── ingress/                       # External access configuration
├── overlays/                      # Kustomize environment overlays
│   ├── minimal/                   # For 4GB RAM systems
│   ├── development/               # Single replicas, smaller storage
│   └── production/                # Multi-replicas, HA, HPAs
└── scripts/                       # Deployment automation
    ├── build-images.sh            # Build all Docker images
    ├── deploy.sh                  # Automated deployment
    ├── status.sh                  # Health check
    ├── rollback.sh                # Rollback deployments
    └── cleanup.sh                 # Delete all resources
```


## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Ingress                               │
│                   (nginx-ingress)                            │
└────────────┬──────────────────────────────┬─────────────────┘
             │                               │
             ▼                               ▼
      ┌────────────┐                   ┌──────────┐
      │    API     │                   │ Frontend │
      │  Gateway   │                   │ (React)  │
      └─────┬──────┘                   └──────────┘
            │
            ├──────────┬─────────────┐
            ▼          ▼             ▼
      ┌──────────┐ ┌──────────┐ ┌──────────┐
      │   URL    │ │Analytics │ │  Redis   │
      │ Service  │ │ Service  │ │  Cache   │
      └────┬─────┘ └────┬─────┘ └──────────┘
           │            │
           ├────────────┼─────────┐
           ▼            ▼         ▼
      ┌──────────┐ ┌──────────┐ ┌──────────┐
      │PostgreSQL│ │  Kafka   │ │ ZooKeeper│
      └──────────┘ └──────────┘ └──────────┘
```

### Deployment Dependency Chain

```
PostgreSQL Ready
    ↓
Redis Ready
    ↓
Kafka Ready
    ↓
URL Service Ready (depends on all above)
    ↓
Analytics Service Ready (depends on PostgreSQL, Kafka)
    ↓
API Gateway Ready (depends on PostgreSQL, Redis, URL Service)
    ↓
Frontend Ready
    ↓
Ingress Configured
```

## Quick Start

### Quick Deployment

```bash
# 1. Start Minikube (adjust memory based on your system)
minikube start --cpus=2 --memory=4096
minikube addons enable ingress

# 2. Build Docker images
cd url-shortener-platform
docker build -t url-shortener/url-service:latest -f url-service/Dockerfile .
docker build -t url-shortener/analytics-service:latest -f analytics-service/Dockerfile .
docker build -t url-shortener/api-gateway:latest -f api-gateway/Dockerfile .
docker build -t url-shortener/frontend:latest frontend/

# 3. Load images into Minikube
minikube image load url-shortener/url-service:latest
minikube image load url-shortener/analytics-service:latest
minikube image load url-shortener/api-gateway:latest
minikube image load url-shortener/frontend:latest

# 4. Deploy to Kubernetes
cd kubernetes/scripts
./deploy.sh statefulset development

# 5. Access the application
minikube tunnel  # Run in separate terminal
kubectl get ingress -n urlshortener
# Open the IP shown in browser
```


## Prerequisites

### Required Tools

1. **kubectl** (v1.24+)
   ```bash
   # macOS
   brew install kubectl

   # Linux
   curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
   chmod +x kubectl
   sudo mv kubectl /usr/local/bin/
   ```

2. **Kubernetes Cluster** (choose one):

   **Minikube** (local development):
   ```bash
   brew install minikube

   # For systems with 6GB+ available RAM
   minikube start --cpus=2 --memory=4096
   minikube addons enable ingress

   # For systems with 4GB available RAM
   minikube start --cpus=2 --memory=3584
   minikube addons enable ingress
   # Then use: kubectl apply -k kubernetes/overlays/minimal/
   # Note: minimal overlay may fail with not enough resources
   ```

   **Kind** (Kubernetes in Docker):
   ```bash
   brew install kind
   kind create cluster --config kind-config.yaml
   ```

   **Cloud Providers** (GKE, EKS, AKS):
   - Follow your cloud provider's setup instructions

3. **Docker** - For building images

4. **Kustomize** (optional, for overlays):
   ```bash
   brew install kustomize
   ```

### Storage Class Setup

The manifests use `fast-ssd` storage class for production. For local development:

```bash
# Use standard storage class (already configured in minimal/development overlays)
# No action needed for Minikube/development

# For production, create storage class:
```

**GKE:**
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-ssd
provisioner: kubernetes.io/gce-pd
parameters:
  type: pd-ssd
  replication-type: regional-pd
```

**AWS EKS:**
```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-ssd
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
```

---

## Deployment Options

### Option 1: Automated Deployment (Recommended)

```bash
cd kubernetes/scripts

# Development deployment (StatefulSet Kafka, single replicas)
./deploy.sh statefulset development

# Production deployment (Strimzi Kafka, multi-replicas, HPAs)
./deploy.sh strimzi production

# Check deployment status
./status.sh
```

The deployment script executes in 6 phases:
1. Creates namespace and configuration
2. Deploys PostgreSQL database
3. Deploys Redis cache
4. Deploys Kafka message broker
5. Deploys application services
6. Configures Ingress

Each phase waits for resources to be ready before proceeding.

### Option 2: Manual Deployment

```bash
# 1. Create namespace and configuration
kubectl apply -f base/namespace.yaml
kubectl apply -k config/

# 2. Deploy infrastructure (in order)
kubectl apply -k infrastructure/postgres/
kubectl wait --for=condition=ready pod -l app=postgres -n urlshortener --timeout=300s

kubectl apply -k infrastructure/redis/
kubectl wait --for=condition=ready pod -l app=redis -n urlshortener --timeout=120s

kubectl apply -k infrastructure/kafka-statefulset/  # or kafka-strimzi/
kubectl wait --for=condition=ready pod -l app=kafka -n urlshortener --timeout=600s

# 3. Deploy application services
kubectl apply -k services/url-service/
kubectl apply -k services/analytics-service/
kubectl apply -k services/api-gateway/
kubectl apply -k services/frontend/

# Wait for all deployments
kubectl wait --for=condition=available deployment --all -n urlshortener --timeout=300s

# 4. Deploy ingress
kubectl apply -k ingress/

# 5. Verify
kubectl get pods -n urlshortener
```

### Option 3: Using Kustomize Overlays

```bash
# Minimal deployment, may not work (4GB RAM systems)
kubectl apply -k overlays/minimal/

# Development deployment (6GB+ RAM, single replicas)
kubectl apply -k overlays/development/

# Production deployment (multi-replicas, HA, HPAs)
kubectl apply -k overlays/production/
```

### Memory Allocation Recommendations

| Total System RAM | Docker/Minikube | OS/Other Apps | Recommended Config |
|------------------|-----------------|---------------|--------------------|
| 4GB | 3-3.5GB | 500Mi-1GB | Minimal overlay |
| 6GB | 4-5GB | 1-2GB | Development overlay |
| 8GB | 6-7GB | 1-2GB | Development + more replicas |
| 16GB+ | 8-12GB | 4-8GB | Production overlay (local) |
| Cloud | N/A | N/A | Production overlay |

---

## Building Docker Images

### Option 1: Using Build Script

```bash
cd url-shortener-platform
./kubernetes/scripts/build-images.sh
```

The script builds all four images with correct contexts.

### Option 2: Manual Build

```bash
cd url-shortener-platform

# Build Spring Boot services
docker build -t url-shortener/url-service:latest -f url-service/Dockerfile .
docker build -t url-shortener/analytics-service:latest -f analytics-service/Dockerfile .
docker build -t url-shortener/api-gateway:latest -f api-gateway/Dockerfile .

# Build frontend (NOTE: build context is frontend/ directory)
docker build -t url-shortener/frontend:latest frontend/
```

### Loading Images into Minikube

```bash
# After building, load into Minikube
minikube image load url-shortener/url-service:latest
minikube image load url-shortener/analytics-service:latest
minikube image load url-shortener/api-gateway:latest
minikube image load url-shortener/frontend:latest

# Verify images are loaded
minikube image ls | grep url-shortener
```

### For Production: Push to Registry

```bash
# Tag with registry URL
docker tag url-shortener/url-service:latest your-registry/url-service:v1.0.0
docker tag url-shortener/analytics-service:latest your-registry/analytics-service:v1.0.0
docker tag url-shortener/api-gateway:latest your-registry/api-gateway:v1.0.0
docker tag url-shortener/frontend:latest your-registry/frontend:v1.0.0

# Push to registry
docker push your-registry/url-service:v1.0.0
docker push your-registry/analytics-service:v1.0.0
docker push your-registry/api-gateway:v1.0.0
docker push your-registry/frontend:v1.0.0

# Update image references in manifests
# Edit overlays/production/kustomization.yaml to use registry URLs
```

## Accessing the App

### With Minikube

```bash
# Start tunnel (required for LoadBalancer services)
# Keep this running in a separate terminal
minikube tunnel

# Get ingress URL
kubectl get ingress -n urlshortener

# Get the IP address
INGRESS_IP=$(kubectl get ingress urlshortener-ingress -n urlshortener \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# Optional: Add to /etc/hosts
echo "$(minikube ip) urlshortener.local" | sudo tee -a /etc/hosts

# Access application
open http://${INGRESS_IP}
# or
open http://urlshortener.local
```

### With Cloud Providers

```bash
# Get external IP
INGRESS_IP=$(kubectl get ingress urlshortener-ingress -n urlshortener \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

echo "Application URL: http://${INGRESS_IP}"

# URLs available:
# http://${INGRESS_IP}/                    - Frontend
# http://${INGRESS_IP}/api/auth/login      - Login API
# http://${INGRESS_IP}/api/urls/shorten    - Create short URL
# http://${INGRESS_IP}/{shortCode}         - Redirect
# http://${INGRESS_IP}/api/analytics/...   - Analytics
# http://${INGRESS_IP}/actuator/health     - Health checks
```

### Service URLs (Internal)

```
postgres:5432              - PostgreSQL database
redis:6379                 - Redis cache
kafka-bootstrap:9092       - Kafka broker
zookeeper:2181            - Zookeeper (StatefulSet option)

url-service:8081          - URL Service
analytics-service:8082    - Analytics Service
api-gateway:8080          - API Gateway
frontend:80               - Frontend
```

## Verification

### Automated Status Check

```bash
cd kubernetes/scripts
./status.sh
```

This displays:
- All pods with status
- StatefulSets status
- Deployments status
- Services
- Ingress configuration
- PVCs
- Recent events

### Manual Verification Steps

#### 1. Check All Pods are Running

```bash
kubectl get pods -n urlshortener
```

Expected output:
```
NAME                                  READY   STATUS    RESTARTS   AGE
analytics-service-xxxxx-xxxxx         1/1     Running   0          2m
api-gateway-xxxxx-xxxxx               1/1     Running   0          2m
frontend-xxxxx-xxxxx                  1/1     Running   0          2m
kafka-0                               1/1     Running   0          3m
postgres-0                            1/1     Running   0          4m
redis-0                               1/1     Running   0          3m
url-service-xxxxx-xxxxx               1/1     Running   0          2m
zookeeper-0                           1/1     Running   0          3m
```

All should show `READY: 1/1` or `2/2` and `STATUS: Running`.

#### 2. Test Database Initialization

```bash
kubectl exec -n urlshortener postgres-0 -- psql -U urluser -d urlshortener -c '\dt'
```

Expected tables:
- users
- urls
- url_clicks
- url_analytics
- url_daily_analytics
- url_geo_analytics

#### 3. Test Redis Connectivity

```bash
kubectl exec -n urlshortener redis-0 -- redis-cli ping
```

Expected: `PONG`

#### 4. Test Kafka Topics

```bash
# For Strimzi
kubectl get kafkatopics -n urlshortener

# For StatefulSet
kubectl exec -n urlshortener kafka-0 -- \
  kafka-topics --bootstrap-server localhost:9092 --list
```

Expected topics:
- url-lifecycle-events
- url-access-events

#### 5. Test Service Health

```bash
# URL Service
kubectl exec -n urlshortener deployment/url-service -- \
  wget -qO- http://localhost:8081/actuator/health

# Analytics Service
kubectl exec -n urlshortener deployment/analytics-service -- \
  wget -qO- http://localhost:8082/actuator/health

# API Gateway
kubectl exec -n urlshortener deployment/api-gateway -- \
  wget -qO- http://localhost:8080/actuator/health
```

All should return: `{"status":"UP",...}`

## Common Commands

### Deployment & Status

```bash
# Quick deploy
cd kubernetes/scripts && ./deploy.sh statefulset development

# Check status
./status.sh

# View all resources
kubectl get all -n urlshortener

# Watch pods starting
kubectl get pods -n urlshortener -w
```

### Logs

```bash
# All application pods
kubectl logs -f -n urlshortener -l tier=application

# Specific service
kubectl logs -f -n urlshortener deployment/url-service

# Previous crashed container
kubectl logs -p -n urlshortener <pod-name>

# All logs (for debugging)
kubectl logs -n urlshortener --all-containers=true
```

### Port Forwarding

```bash
# PostgreSQL
kubectl port-forward -n urlshortener postgres-0 5432:5432

# Redis
kubectl port-forward -n urlshortener redis-0 6379:6379

# Kafka
kubectl port-forward -n urlshortener kafka-0 9092:9092

# URL Service
kubectl port-forward -n urlshortener deployment/url-service 8081:8081

# API Gateway
kubectl port-forward -n urlshortener deployment/api-gateway 8080:8080
```

### Database Operations

```bash
# Connect to PostgreSQL
kubectl exec -it postgres-0 -n urlshortener -- psql -U urluser -d urlshortener

# List tables
kubectl exec postgres-0 -n urlshortener -- psql -U urluser -d urlshortener -c '\dt'

# Query data
kubectl exec postgres-0 -n urlshortener -- \
  psql -U urluser -d urlshortener -c 'SELECT * FROM users;'

# Database backup
kubectl exec postgres-0 -n urlshortener -- \
  pg_dump -U urluser urlshortener | gzip > backup-$(date +%Y%m%d).sql.gz

# Restore
gunzip -c backup-20240101.sql.gz | \
  kubectl exec -i postgres-0 -n urlshortener -- psql -U urluser urlshortener
```

### Redis Operations

```bash
# Connect to Redis CLI
kubectl exec -it redis-0 -n urlshortener -- redis-cli

# Check keys
kubectl exec redis-0 -n urlshortener -- redis-cli KEYS '*'

# Get value
kubectl exec redis-0 -n urlshortener -- redis-cli GET 'url:abc123'

# Monitor commands
kubectl exec redis-0 -n urlshortener -- redis-cli MONITOR
```

### Kafka Operations

```bash
# List topics (Strimzi)
kubectl get kafkatopics -n urlshortener

# List topics (StatefulSet)
kubectl exec kafka-0 -n urlshortener -- \
  kafka-topics --bootstrap-server localhost:9092 --list

# Describe topic
kubectl exec kafka-0 -n urlshortener -- \
  kafka-topics --bootstrap-server localhost:9092 --describe --topic url-access-events

# Check consumer groups
kubectl exec kafka-0 -n urlshortener -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Consumer group details
kubectl exec kafka-0 -n urlshortener -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group analytics-service-group
```

### Scaling

```bash
# Manual scale
kubectl scale deployment url-service --replicas=5 -n urlshortener

# Check HPA status
kubectl get hpa -n urlshortener

# Describe HPA
kubectl describe hpa url-service-hpa -n urlshortener
```

### Configuration Updates

```bash
# Edit ConfigMap
kubectl edit configmap common-config -n urlshortener

# Apply changes from file
kubectl apply -f config/common-configmap.yaml

# Restart deployments to pick up changes
kubectl rollout restart deployment/url-service -n urlshortener
kubectl rollout restart deployment/analytics-service -n urlshortener
kubectl rollout restart deployment/api-gateway -n urlshortener
```

### Rollback

```bash
# Using script
./scripts/rollback.sh url-service
./scripts/rollback.sh  # All services

# Manual rollback
kubectl rollout history deployment/url-service -n urlshortener
kubectl rollout undo deployment/url-service -n urlshortener
kubectl rollout undo deployment/url-service --to-revision=2 -n urlshortener
```

### Cleanup

```bash
# Complete cleanup
cd kubernetes/scripts && ./cleanup.sh

# Delete namespace (deletes everything)
kubectl delete namespace urlshortener

# Delete specific resources
kubectl delete deployment url-service -n urlshortener
kubectl delete statefulset postgres -n urlshortener
```

## Troubleshooting

### Quick Diagnosis Commands

```bash
# Check pod status
kubectl get pods -n urlshortener

# Describe pod (see events and errors)
kubectl describe pod <pod-name> -n urlshortener

# View logs
kubectl logs <pod-name> -n urlshortener

# Previous container logs (if crashed)
kubectl logs -p <pod-name> -n urlshortener

# Check events
kubectl get events -n urlshortener --sort-by='.lastTimestamp'

# Interactive shell
kubectl exec -it <pod-name> -n urlshortener -- /bin/sh
```

## Scaling

### Horizontal Pod Autoscaling (HPA)

HPAs are included in production overlay:

```bash
# Check HPA status
kubectl get hpa -n urlshortener

# Describe HPA
kubectl describe hpa url-service-hpa -n urlshortener
```

**HPA Configuration:**
- **URL Service**: 3-10 replicas (70% CPU, 80% memory)
- **Analytics Service**: 2-5 replicas (75% CPU, 600s scale-down)
- **API Gateway**: 2-4 replicas (70% CPU)

### Manual Scaling

```bash
# Scale URL Service
kubectl scale deployment url-service --replicas=5 -n urlshortener

# Scale Analytics Service
kubectl scale deployment analytics-service --replicas=3 -n urlshortener

# Scale API Gateway
kubectl scale deployment api-gateway --replicas=3 -n urlshortener
```

### StatefulSet Scaling

For production high availability:

```bash
# Scale PostgreSQL (requires replication setup)
kubectl scale statefulset postgres --replicas=3 -n urlshortener

# Scale Redis (upgrade to Sentinel first)
kubectl scale statefulset redis --replicas=3 -n urlshortener

# Scale Kafka (Strimzi only)
# Edit kafka-cluster.yaml: spec.kafka.replicas: 3
kubectl apply -k infrastructure/kafka-strimzi/
```

## Monitoring

### Resource Usage

```bash
# Pod metrics
kubectl top pods -n urlshortener

# Node metrics
kubectl top nodes

# Watch resource usage
watch kubectl top pods -n urlshortener
```

### Logs Monitoring

```bash
# All application pods
kubectl logs -f -n urlshortener -l tier=application

# Specific service
kubectl logs -f -n urlshortener deployment/url-service

# Search logs for errors
kubectl logs -n urlshortener -l app=url-service --tail=100 | grep ERROR

# Logs from all containers
kubectl logs -n urlshortener --all-containers=true > all-logs.txt
```

### Health Monitoring

```bash
# Check all health endpoints
for svc in url-service analytics-service api-gateway; do
  echo "=== $svc ==="
  kubectl exec -n urlshortener deployment/$svc -- \
    wget -qO- http://localhost:808*/actuator/health | jq .status
done
```

### Events Monitoring

```bash
# Watch events
kubectl get events -n urlshortener --watch

# Recent events
kubectl get events -n urlshortener --sort-by='.lastTimestamp' | tail -20
```

### Prometheus Integration

All Spring Boot services expose Prometheus metrics at `/actuator/prometheus`:

```bash
# Access metrics
kubectl port-forward -n urlshortener deployment/url-service 8081:8081
curl http://localhost:8081/actuator/prometheus

# Key metrics:
# - jvm_memory_used_bytes
# - http_server_requests_seconds
# - kafka_consumer_records_consumed_total
# - hikaricp_connections_active
```

---

## Production Considerations

### Security

1. **Secrets Management**

   **Current:** Plain YAML secrets (development only)

   **Production:** Use Sealed Secrets or External Secrets Operator

2. **Network Policies**

   Restrict pod-to-pod communication:

   ```yaml
   apiVersion: networking.k8s.io/v1
   kind: NetworkPolicy
   metadata:
     name: url-service-netpol
   spec:
     podSelector:
       matchLabels:
         app: url-service
     policyTypes:
     - Ingress
     ingress:
     - from:
       - podSelector:
           matchLabels:
             app: api-gateway
       ports:
       - protocol: TCP
         port: 8081
   ```

3. **Pod Security Standards**

   ```yaml
   apiVersion: v1
   kind: Namespace
   metadata:
     name: urlshortener
     labels:
       pod-security.kubernetes.io/enforce: restricted
       pod-security.kubernetes.io/audit: restricted
       pod-security.kubernetes.io/warn: restricted
   ```

4. **TLS/HTTPS**

   Install cert-manager for automatic TLS:

   ```bash
   kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
   ```

### High Availability

1. **Multi-Zone Deployment**

   ```bash
   # Spread pods across zones
   kubectl label nodes node1 topology.kubernetes.io/zone=us-east-1a
   kubectl label nodes node2 topology.kubernetes.io/zone=us-east-1b

   # Add pod anti-affinity to deployments
   ```

2. **Redis High Availability**

   Upgrade to Redis Sentinel:

   ```yaml
   # Deploy Redis Sentinel (3 sentinels + 3 Redis instances)
   # See: redis-sentinel/ configuration
   ```

3. **Kafka Multi-Broker**

   ```yaml
   # In kafka-cluster.yaml
   spec:
     kafka:
       replicas: 3
       config:
         offsets.topic.replication.factor: 3
         transaction.state.log.replication.factor: 3
   ```

4. **PostgreSQL Replication**

   Set up streaming replication:

   ```bash
   # Primary-Standby replication
   # Or use PostgreSQL Operator (e.g., Zalando Postgres Operator)
   ```

### Backup Strategy

```bash
# PostgreSQL automated backups
kubectl create cronjob postgres-backup \
  --image=postgres:15-alpine \
  --schedule="0 2 * * *" \
  --restart=Never \
  -- /bin/sh -c "pg_dump -U urluser -h postgres urlshortener | gzip > /backups/backup-\$(date +%Y%m%d).sql.gz"

# PVC snapshots (cloud provider)
# Create VolumeSnapshot resources
# Schedule with external tools (Velero, K8up)
```

### Cost Optimization

**Development:**
- Single replicas for all services
- Smaller storage
- Standard storage class
- Single Kafka broker

**Production:**
- Multi-replicas with HPA
- Larger storage (20Gi+ PostgreSQL, 5Gi Redis)
- SSD storage class
- Multi-broker Kafka with replication


## Implementation Details

### Infrastructure Layer (StatefulSets)

#### PostgreSQL
- **StatefulSet** with stable network identity
- **PVC:** 20Gi (prod), 5Gi (dev), 2Gi (minimal)
- **Init container:** Permission setup (chown 999:999)
- **Database initialization:** Full schema via ConfigMap
- **Health probes:** Liveness (pg_isready), Readiness (SELECT 1)
- **Resources:** 500m-2000m CPU, 1-4Gi memory
- **Seed data:** Test users and sample URLs included

#### Redis
- **StatefulSet** with AOF persistence
- **PVC:** 5Gi (prod), 1Gi (dev), 500Mi (minimal)
- **Command:** `redis-server --appendonly yes`
- **Health probes:** redis-cli ping
- **Resources:** 200m-500m CPU, 256-512Mi memory

#### Kafka Options

**Option A: Strimzi Operator (Production)**
- Kafka CRD with 1 broker (dev) or 3 brokers (prod)
- Zookeeper managed by Strimzi
- Topics: url-lifecycle-events (3 partitions), url-access-events (6 partitions)
- Retention: 7 days, compression: snappy
- Storage: 50Gi per broker
- Automatic topic creation and management

**Option B: StatefulSet (Development)**
- Zookeeper StatefulSet (1 replica)
- Kafka StatefulSet (1 replica)
- Manual topic creation required
- Init containers: wait for Zookeeper
- DNS fixed: Full FQDN `zookeeper.urlshortener.svc.cluster.local`

### Application Layer (Deployments)

#### URL Service
- **Deployment:** 2 replicas (dev), 3-10 with HPA (prod)
- **Init containers:** wait-for-postgres, wait-for-redis, wait-for-kafka
- **Health probes:**
  - Liveness: /actuator/health/liveness (60s initial, 20s period)
  - Readiness: /actuator/health/readiness (30s initial, 10s period)
- **HPA:** 3-10 replicas, 70% CPU, 80% memory
- **PDB:** minAvailable 2
- **Resources:** 250m-1000m CPU, 512Mi-1Gi memory

#### Analytics Service
- **Deployment:** 2 replicas (dev), 2-5 with HPA (prod)
- **Init containers:** wait-for-postgres, wait-for-kafka
- **Health probes:**
  - Liveness: 90s initial, 20s period
  - Readiness: 60s initial, 10s period
- **HPA:** 2-5 replicas, 75% CPU, 600s scale-down (Kafka rebalancing)
- **PDB:** minAvailable 1
- **Resources:** 500m-1500m CPU, 768Mi-1.5Gi memory

#### API Gateway
- **Deployment:** 2 replicas (dev), 2-4 with HPA (prod)
- **Init containers:** wait-for-postgres, wait-for-redis, wait-for-url-service
- **HPA:** 2-4 replicas, 70% CPU
- **PDB:** minAvailable 1
- **Resources:** 200m-500m CPU, 384-768Mi memory

#### Frontend
- **Deployment:** 2 replicas
- **Nginx** serving React SPA
- **Security:** readOnlyRootFilesystem with tmpfs mounts
- **Resources:** 100m-200m CPU, 128-256Mi memory

### Configuration Management

#### ConfigMaps
1. **common-config:** DB, Redis, Kafka hosts
2. **url-service-config:** Short code settings, rate limiting, cache TTL
3. **api-gateway-config:** CORS, JWT settings (fixed invalid keys)
4. **analytics-service-config:** Kafka consumer settings
5. **postgres-init-db:** Complete database schema

#### Secrets
- **urlshortener-secrets:** POSTGRES_PASSWORD, JWT_SECRET, JWT_EXPIRATION

### Networking

#### Services
- **ClusterIP (headless)** for StatefulSets
- **ClusterIP** for Deployments
- **kafka-bootstrap:** Service pointing to Kafka brokers

#### Ingress
- **nginx-ingress** controller required
- **Routes:**
  - `/api` → api-gateway:8080
  - `/actuator` → api-gateway:8080
  - `/` → frontend:80
- **TLS:** Template ready for cert-manager

### Kustomize Overlays

#### Minimal Overlay (4GB RAM, may not work due to memory constraints)
- Reduced resource requests and limits
- Smaller PVCs
- Standard storage class
- Single replicas

#### Development Overlay
- Single replicas
- 5Gi PostgreSQL, 1Gi Redis
- Standard storage class
- StatefulSet Kafka

#### Production Overlay
- Multi-replicas with HPAs
- 20Gi PostgreSQL, 5Gi Redis, 50Gi Kafka
- fast-ssd storage class
- Strimzi Kafka
- Full PDBs

### Security Best Practices Implemented

- **runAsNonRoot:** All application pods (UID 1000)
- **readOnlyRootFilesystem:** Frontend
- **No privilege escalation:** `allowPrivilegeEscalation: false`
- **Secrets management:** Separate secrets file
- **Network isolation:** ClusterIP (internal only)
- **Security contexts:** Pod and container level


**Current limitation:** Resource constraint ONLY (need 5Gi, have 3.5Gi)

### Alternative: Docker Compose

If Kubernetes is too resource-intensive for your local system:

```bash
cd url-shortener-platform
docker-compose up
```

This uses less memory (~2-3GB total) and is sufficient for development.