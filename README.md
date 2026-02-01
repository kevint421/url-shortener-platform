# URL Shortener & Analytics Platform

> A production-grade, event-driven URL shortener with real-time analytics - built with Spring Boot, Kafka, Redis, PostgreSQL, and Kubernetes.

## Project Overview

This is a **local-first, production-ready** URL shortener service similar to bit.ly, designed with:
- **Ultra-fast redirects** (< 50ms p95 latency)
- **Event-driven analytics** processing with Kafka
- **Microservices architecture** with Spring Boot
- **Horizontal scalability** via Kubernetes
- **Zero cloud cost** for local development

### Key Features

**High-Performance URL Shortening**
- Sub-50ms redirect latency with Redis caching
- Custom alias support
- Expiration dates and TTL
- Base62 short code generation
- Atomic click counters

**Event-Driven Architecture**
- Asynchronous analytics processing via Kafka
- Decoupled redirect and analytics paths
- Replayable event streams
- CQRS-lite pattern for analytics

**Real-Time Analytics**
- Click tracking and aggregation
- Geographic distribution (IP geolocation)
- Device/browser analytics (user agent parsing)
- Time-series metrics (hourly/daily buckets)

**Production-Ready**
- Docker Compose for local development
- Kubernetes manifests for production deployment
- Health checks and actuator endpoints
- Comprehensive test suite (unit, integration, E2E, performance)
- Structured logging and monitoring

## Architecture

### System Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client / Browser                        │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │    API Gateway         │
                    │    (Port 8080)         │
                    │                        │
                    │  - JWT Authentication  │
                    │  - Rate Limiting       │
                    │  - Request Routing     │
                    │  - CORS Handling       │
                    └─────────┬──────────────┘
                              │
                 ┌────────────┴────────────┐
                 ▼                         ▼
    ┌───────────────────┐        ┌──────────────────┐
    │   URL Service     │        │ Analytics Service │
    │   (Port 8081)     │        │   (Port 8082)     │
    │                   │        │                   │
    │ - URL Shortening  │        │ - Event Consumer  │
    │ - Redirect Logic  │        │ - Data Aggregation│
    │ - Cache Mgmt      │        │ - Query API       │
    └─┬──────────┬──────┘        └────────┬──────────┘
      │          │                        │
      │          │     ┌──────────────────▼─────────────┐
      │          │     │  Kafka Event Streams           │
      │          └────>│  ┌──────────────────────────┐  │
      │                │  │ url-lifecycle-events     │  │
      │                │  │  - URLCreated            │  │
      │                │  │  - URLDeleted            │  │
      │                │  │  - URLExpired            │  │
      │                │  └──────────────────────────┘  │
      │                │  ┌──────────────────────────┐  │
      │                │  │ url-access-events        │  │
      │                │  │  - URLAccessed           │  │
      │                │  │  (IP, User-Agent, etc)   │  │
      │                │  └──────────────────────────┘  │
      │                └────────────────────────────────┘
      │
      ├────────────────> Redis Cache
      │                  ┌─────────────────────────┐
      │                  │ Key-Value Store         │
      │                  │ - shortCode → longUrl   │
      │                  │ - Click counters (INCR) │
      │                  │ - Rate limiting         │
      │                  └─────────────────────────┘
      │
      └────────────────> PostgreSQL Database
                         ┌──────────────────────────┐
                         │ Source of Truth          │
                         │ - users                  │
                         │ - urls                   │
                         │ - url_clicks (analytics) │
                         │ - url_analytics (aggreg) │
                         └──────────────────────────┘
```

### Microservices

**1. API Gateway (Port 8080)**
- Single entry point for all client requests
- JWT-based authentication and authorization
- Spring Cloud Gateway for routing
- Global rate limiting with Redis
- CORS configuration

**2. URL Service (Port 8081)**
- Core URL shortening logic
- High-performance redirect path (< 50ms)
- Redis-first caching strategy
- Kafka event publishing
- User quota enforcement

**3. Analytics Service (Port 8082)**
- Kafka consumer for click events
- Data aggregation and analytics
- Query API for dashboards
- Geographic and device analytics

**4. Frontend (Port 3000)**
- React + TypeScript SPA
- URL management dashboard
- Real-time analytics visualization
- QR code generation

### Tech Stack

**Backend:**
- Java 21
- Spring Boot 3.2.0
- Spring Cloud Gateway
- Spring Data JPA
- Spring Kafka

**Databases:**
- PostgreSQL 15 (persistent storage)
- Redis 7 (caching layer)

**Messaging:**
- Apache Kafka 3.6 (event streaming)
- Confluent Platform

**Infrastructure:**
- Docker & Docker Compose
- Kubernetes with Helm
- Maven (build tool)

**Testing:**
- JUnit 5
- Testcontainers
- Apache Bench (performance)

**Frontend:**
- React 18
- TypeScript
- Axios (API client)

## Quick Start

### Prerequisites

- **Java 21** (OpenJDK or Oracle JDK)
- **Maven 3.8+**
- **Docker Desktop** (with Docker Compose)
- **Git**

Optional for testing:
- **curl** (API testing)
- **jq** (JSON parsing)
- **Apache Bench** (performance testing)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/url-shortener-platform.git
cd url-shortener-platform
```

### 2. Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, Kafka, and Zookeeper
docker-compose up -d postgres redis zookeeper kafka

# Verify services are healthy
docker-compose ps

# Expected output: All services should show "Up" status
```

### 3. Build the Project

```bash
# Build all modules (common, url-service, api-gateway, analytics-service)
mvn clean install

# Skip tests for faster build
mvn clean install -DskipTests
```

### 4. Run the Application

**Option A: Run with Docker Compose (Recommended)**

```bash
# Build service images
docker-compose build

# Start all services
docker-compose up

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f
```

**Option B: Run Services Locally (Development)**

```bash
# Terminal 1 - API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 2 - URL Service
cd url-service
mvn spring-boot:run

# Terminal 3 - Analytics Service
cd analytics-service
mvn spring-boot:run

# Terminal 4 - Frontend (if implemented)
cd frontend
npm install
npm start
```

### 5. Verify the Setup

```bash
# Check health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# All should return: {"status":"UP"}
```

### 6. Create Your First Short URL

```bash
# Register a user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'

# Login and get JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | jq -r '.token')

echo "Your token: $TOKEN"

# Create a short URL
curl -X POST http://localhost:8080/api/urls/shorten \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com"
  }'

# Response:
# {
#   "shortCode": "abc123",
#   "shortUrl": "http://localhost:8080/abc123",
#   "longUrl": "https://www.example.com",
#   "createdAt": "2024-01-15T10:30:00",
#   "expiresAt": null
# }

# Test the redirect
curl -v http://localhost:8080/abc123
# Should see 301 redirect to https://www.example.com
```

## API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Register new user | No |
| `POST` | `/api/auth/login` | Login and get JWT | No |

**Register Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com"
  }
}
```

### URL Management Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/api/urls/shorten` | Create a short URL | Yes |
| `GET` | `/api/urls/{shortCode}` | Get URL details | Yes |
| `GET` | `/api/urls/user/{userId}` | Get user's URLs | Yes |
| `DELETE` | `/api/urls/{shortCode}` | Delete a URL | Yes |

**Shorten URL Request:**
```json
{
  "longUrl": "https://www.example.com/very/long/url",
  "customAlias": "mylink",  // Optional
  "expirationDays": 30      // Optional
}
```

**Shorten URL Response:**
```json
{
  "shortCode": "mylink",
  "shortUrl": "http://localhost:8080/mylink",
  "longUrl": "https://www.example.com/very/long/url",
  "createdAt": "2024-01-15T10:30:00",
  "expiresAt": "2024-02-14T10:30:00"
}
```

### Redirect Endpoint

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/{shortCode}` | Redirect to long URL | No |

This is the **hot path** - optimized for < 50ms response time with Redis caching.

### Analytics Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/api/analytics/url/{shortCode}` | URL statistics | Yes |
| `GET` | `/api/analytics/user/{userId}` | User analytics | Yes |
| `GET` | `/api/analytics/top` | Top URLs by clicks | Yes |

**Analytics Response:**
```json
{
  "shortCode": "abc123",
  "totalClicks": 1547,
  "uniqueIps": 432,
  "topReferrers": [
    {"referrer": "https://google.com", "count": 234},
    {"referrer": "https://twitter.com", "count": 156}
  ],
  "clicksByDay": [
    {"date": "2024-01-15", "clicks": 45},
    {"date": "2024-01-16", "clicks": 67}
  ]
}
```

## Testing

The project includes a test suite covering unit, integration, performance, and end-to-end testing.

### Test Overview

| Test Type | Count | Tools | Purpose |
|-----------|-------|-------|---------|
| Unit Tests | 13 | JUnit 5 | Business logic validation |
| Integration Tests | 6 | Testcontainers, JUnit | Service integration testing |
| Performance Tests | 1 | Apache Bench | Redirect latency benchmarking |
| End-to-End Tests | 1 | Shell script | Complete user journey |

### Running Tests

**Unit and Integration Tests:**

```bash
# Run all tests
mvn test

# Run specific module tests
mvn test -pl common
mvn test -pl url-service
mvn test -pl analytics-service

# Run specific test class
mvn test -pl url-service -Dtest=UrlServiceIntegrationTest

# Integration tests with verbose logging
mvn test -pl url-service -Dtest=UrlServiceIntegrationTest \
  -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

**Performance Tests:**

```bash
# Ensure application is running
docker-compose up -d

# Run redirect performance test
cd tests/performance
./redirect_load_test.sh

# Custom parameters
NUM_REQUESTS=2000 CONCURRENCY=100 ./redirect_load_test.sh
```

**Expected output:**
```
PERFORMANCE TEST RESULTS
========================================

Throughput:
  Requests/sec: 450.32
  Time/request: 2.221 ms (mean)

Latency Percentiles:
  p50 (median): 15 ms
  p95: 35 ms  ✓ PASS (target: < 50ms)
  p99: 48 ms

Reliability:
  Failed requests: 0  ✓ PASS
```

**End-to-End Tests:**

```bash
# Ensure application is running
docker-compose up -d

# Run complete user journey test
cd tests/e2e
./complete_user_journey.sh
```

**Test flow:**
1. Register user
2. Login and get JWT
3. Create short URL
4. Test redirect
5. View analytics
6. Cleanup

### Test Requirements

**For Java Tests:**
- Docker Desktop running
- Testcontainers will automatically pull required images

**For Shell Script Tests:**
- `curl` (usually pre-installed)
- `jq` (recommended): `brew install jq` or `sudo apt install jq`
- `ab` (Apache Bench): `brew install httpd` or `sudo apt install apache2-utils`

### Performance Targets

| Metric | Target | Notes |
|--------|--------|-------|
| Redirect p50 | < 20ms | Median latency with warm cache |
| Redirect p95 | < 50ms | 95th percentile - our SLA |
| Redirect p99 | < 100ms | 99th percentile |
| Throughput | > 500 req/s | Single instance, commodity hardware |
| Success Rate | 100% | No errors under normal load |

## Monitoring & Observability

### Health Checks

All services expose Spring Boot Actuator health endpoints with automatic dependency monitoring:

```bash
# API Gateway health
curl http://localhost:8080/actuator/health

# URL Service health
curl http://localhost:8081/actuator/health

# Analytics Service health
curl http://localhost:8082/actuator/health

# Detailed health (includes DB, Redis, Kafka)
curl http://localhost:8081/actuator/health/readiness
curl http://localhost:8081/actuator/health/liveness
```

**Health Check Components (Automatic):**
- Database Connectivity (PostgreSQL)
- Redis Connectivity (URL Service, API Gateway)
- Kafka Connectivity (URL Service, Analytics Service)
- Disk Space
- Application Status

**Kubernetes Probes:**
- **Startup Probe**: 60-90s max startup time (Kafka consumer initialization)
- **Liveness Probe**: `/actuator/health/liveness` - detects hung processes
- **Readiness Probe**: `/actuator/health/readiness` - ensures pod ready for traffic

### Metrics & Prometheus

Prometheus-compatible metrics available at `/actuator/prometheus`:

```bash
# Application metrics
curl http://localhost:8081/actuator/metrics

# Specific metric
curl http://localhost:8081/actuator/metrics/http.server.requests

# Prometheus scrape endpoint
curl http://localhost:8081/actuator/prometheus
```

**HTTP & Application Metrics:**
- `http_server_requests_seconds_count` - Request count by endpoint, method, status
- `http_server_requests_seconds{quantile="0.95"}` - p95 latency
- `url_created_total` - URL creation rate
- `url_redirected_total` - Redirect rate

**Database Connection Pool (HikariCP):**
- `hikaricp_connections_active` - Active connections
- `hikaricp_connections_idle` - Idle connections
- `hikaricp_connections_pending` - Pending threads waiting for connection
- `hikaricp_connections_timeout_total` - Connection timeouts
- `hikaricp_connections_creation_seconds` - Connection creation time

**Kafka Metrics:**
- `kafka_producer_records_sent_total{topic="url-access-events"}` - Producer throughput
- `kafka_consumer_records_consumed_total{topic="url-access-events"}` - Consumer throughput
- `kafka_consumer_lag{group="analytics-service"}` - Consumer lag (critical for analytics)

### Database Monitoring

```bash
# Connection count
docker exec postgres psql -U urluser -d urlshortener \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Database size
docker exec postgres psql -U urluser -d urlshortener \
  -c "SELECT pg_size_pretty(pg_database_size('urlshortener'));"

# Table sizes
docker exec postgres psql -U urluser -d urlshortener -c "
  SELECT tablename, pg_size_pretty(pg_total_relation_size('public.'||tablename)) AS size
  FROM pg_tables WHERE schemaname = 'public'
  ORDER BY pg_total_relation_size('public.'||tablename) DESC;"
```

### Redis Monitoring

```bash
# Server info
docker exec redis redis-cli INFO

# Cache statistics (hit/miss ratio)
docker exec redis redis-cli INFO stats

# Connected clients
docker exec redis redis-cli CLIENT LIST

# Monitor commands in real-time
docker exec redis redis-cli MONITOR

# Memory usage
docker exec redis redis-cli INFO memory
```

**Expected cache hit rate:** > 80% for URL lookups

### Kafka Monitoring

Kafka UI is available at `http://localhost:8090` for visual monitoring.

**CLI Commands:**
```bash
# List topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Topic details
docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --describe --topic url-access-events

# Consumer group status (check lag)
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group analytics-service --describe

# Expected output:
# GROUP              TOPIC               PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# analytics-service  url-access-events   0          1234           1234            0
```

**Consumer Lag Alerts:**
- **Warning**: Lag > 1000 messages
- **Critical**: Lag > 10000 messages
- **Action**: Scale Analytics Service if lag grows

### Logging

Structured logging with consistent patterns across all services:

**Log Configuration:**
```yaml
# Pattern: timestamp - message
logging:
  level:
    root: INFO
    com.urlshortener.*: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

**Viewing Logs:**
```bash
# Docker Compose
docker-compose logs -f url-service
docker-compose logs -f analytics-service
docker-compose logs -f api-gateway

# Filter for errors
docker-compose logs url-service | grep ERROR

# Last 100 lines
docker-compose logs --tail=100 url-service

# Kubernetes
kubectl logs -f -n urlshortener deployment/url-service
kubectl logs -f -n urlshortener -l app=url-service  # All pods
kubectl logs -p -n urlshortener <pod-name>  # Previous container (after crash)
```

**Log Levels:**
- `TRACE` - SQL parameter binding
- `DEBUG` - Application flow, SQL queries
- `INFO` - Important events (startup, requests)
- `WARN` - Recoverable issues (Redis timeout, fallback to DB)
- `ERROR` - Application errors (Kafka failures, database errors)

## Docker Commands

```bash
# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d url-service

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f url-service

# Restart a service
docker-compose restart url-service

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Rebuild and start
docker-compose up --build

# Scale a service
docker-compose up -d --scale url-service=3

# Check resource usage
docker stats
```

## Kubernetes Deployment

Kubernetes manifests are available for production deployment. See [`kubernetes/README.md`](kubernetes/README.md) for detailed instructions.

**Quick deploy:**

```bash
# Apply all manifests
kubectl apply -f kubernetes/

# Check status
kubectl get pods -n urlshortener
kubectl get services -n urlshortener

# View logs
kubectl logs -f deployment/url-service -n urlshortener

# Scale deployment
kubectl scale deployment url-service --replicas=5 -n urlshortener
```

**Production features:**
- Horizontal Pod Autoscaling (HPA)
- Rolling updates with zero downtime
- Redis cluster mode
- Kafka via Strimzi operator
- Persistent volumes for data
- Ingress with TLS termination

## Configuration

### Environment Variables

Core configuration via environment variables or `.env` file:

```env
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_USER=urluser
POSTGRES_PASSWORD=changeme
POSTGRES_DB=urlshortener

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Application
BASE_URL=http://localhost:8080
MAX_URLS_PER_USER=1000
SHORT_CODE_LENGTH=7

# JWT
JWT_SECRET=your-256-bit-secret-key-change-in-production
JWT_EXPIRATION=86400000

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_URLSHORTENER=DEBUG
```

### Application Profiles

Spring profiles for different environments:

- `default` - Development (embedded configs)
- `docker` - Docker Compose deployment
- `kubernetes` - Kubernetes deployment
- `test` - Testing (Testcontainers)

Activate profile:
```bash
# Via environment variable
export SPRING_PROFILES_ACTIVE=docker

# Via application argument
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=docker

# In Docker Compose
environment:
  - SPRING_PROFILES_ACTIVE=docker
```

## Project Structure

```
url-shortener-platform/
├── api-gateway/                 # API Gateway service
│   ├── src/main/java/          # Source code
│   ├── src/main/resources/     # Configurations
│   └── pom.xml
├── url-service/                 # URL shortening service
│   ├── src/main/java/
│   ├── src/test/java/          # Integration tests
│   └── pom.xml
├── analytics-service/           # Analytics service
│   ├── src/main/java/
│   ├── src/test/java/
│   └── pom.xml
├── common/                      # Shared utilities
│   ├── src/main/java/
│   │   ├── constants/          # Shared constants
│   │   ├── dto/                # Data Transfer Objects
│   │   ├── event/              # Kafka event models
│   │   └── util/               # Utilities
│   └── src/test/java/          # Unit tests
├── frontend/                    # React frontend
│   ├── src/
│   ├── public/
│   └── package.json
├── kubernetes/                  # Kubernetes manifests
│   ├── base/                   # Namespace
│   ├── infrastructure/         # PostgreSQL, Redis, Kafka
│   ├── services/               # Application deployments
│   └── README.md
├── docker/                      # Docker configs
│   └── init-db/                # Database initialization
├── tests/                       # Test scripts
│   ├── e2e/                    # End-to-end tests
│   └── performance/            # Performance tests
├── docker-compose.yml           # Local development stack
├── pom.xml                      # Parent Maven config
└── README.md                    # This file
```

## Security Considerations

**Implemented:**
- JWT-based authentication
- Password hashing with BCrypt
- Rate limiting per user/IP
- Input validation on all endpoints
- SQL injection prevention (JPA/Hibernate)
- CORS configuration
- No sensitive data in logs

**Future Improvements for Production**
- Use Kubernetes Secrets for credentials
- Enable HTTPS/TLS
- Implement API key rotation
- Add request signing for webhooks
- Implement IP whitelisting for admin endpoints


## License
MIT License
