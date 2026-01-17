# URL Shortener & Analytics Platform

> A production-grade, event-driven URL shortener with real-time analytics - built with Spring Boot, Kafka, Redis, PostgreSQL, and Kubernetes.

## 🎯 Project Overview

This is a **local-first, production-ready** URL shortener service similar to bit.ly, designed with:
- **Ultra-fast redirects**
- **Event-driven analytics** processing
- **Microservices architecture**
- **Horizontal scalability**
- **Zero cloud cost** for local development

### Key Features

✅ **High-Performance URL Shortening**
- Sub-50ms redirect latency with Redis caching
- Custom alias support
- Expiration dates and TTL
- Base62 short code generation

✅ **Event-Driven Architecture**
- Asynchronous analytics processing via Kafka
- Decoupled redirect and analytics paths
- Replayable event streams

✅ **Real-Time Analytics**
- Click tracking and aggregation
- Geographic distribution
- Device/browser analytics
- Time-series metrics

✅ **Production-Ready**
- Docker Compose for local development
- Kubernetes manifests for production
- Health checks and monitoring
- Comprehensive error handling

## Architecture

### System Design

```
┌─────────┐
│ Client  │
└────┬────┘
     │
     ▼
┌─────────────────┐
│  API Gateway    │  ← Authentication, Rate Limiting
│  (Port 8080)    │
└────┬────────────┘
     │
     ├──────────────────┐
     ▼                  ▼
┌──────────┐      ┌─────────────┐
│   URL    │      │  Analytics  │
│ Service  │      │   Service   │
│(Port 8081)│     │ (Port 8082) │
└─┬──────┬─┘      └──────┬──────┘
  │      │               │
  │      │    ┌──────────▼──────────┐
  │      │    │  Kafka Topics       │
  │      │    │  - url-lifecycle    │
  │      └───>│  - url-access       │
  │           └─────────────────────┘
  │
  ├────> Redis Cache
  │      (shortCode → longUrl)
  │
  └────> PostgreSQL
         (Source of Truth)
```

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

**Infrastructure:**
- Docker & Docker Compose
- Kubernetes (production)
- Maven (build tool)

**Frontend:**
- React 18
- TypeScript

## 🚀 Quick Start

### Prerequisites

- **Java 21** (OpenJDK or Oracle)
- **Maven 3.8+**
- **Docker** & **Docker Compose**
- **Git**

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/url-shortener-platform.git
cd url-shortener-platform
```

### 2. Start Infrastructure

```bash
# Start PostgreSQL, Redis, and Kafka
docker-compose up -d

# Verify all services are healthy
docker-compose ps
```

### 3. Build the Project

```bash
# Build all modules
mvn clean install
```

### 4. Run the Services

**Option A: Run with Docker (Recommended)**
```bash
docker-compose build
docker-compose up
```

**Option B: Run Locally**
```bash
# Terminal 1 - URL Service
cd url-service
mvn spring-boot:run

# Terminal 2 - Analytics Service (when implemented)
cd analytics-service
mvn spring-boot:run

# Terminal 3 - API Gateway (when implemented)
cd api-gateway
mvn spring-boot:run
```

### 5. Test the Service

```bash
# Shorten a URL
curl -X POST http://localhost:8081/api/urls/shorten \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"longUrl": "https://www.example.com"}'

# Response:
# {
#   "shortCode": "abc123",
#   "shortUrl": "http://localhost:8080/abc123",
#   ...
# }

# Test redirect
curl -v http://localhost:8081/abc123
# Should see 301 redirect
```

## 🔌 API Endpoints

### URL Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/urls/shorten` | Create a short URL |
| `GET` | `/api/urls/{shortCode}` | Get URL details |
| `GET` | `/api/urls/user/{userId}` | Get user's URLs |
| `DELETE` | `/api/urls/{shortCode}` | Delete a URL |

### Redirect

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/{shortCode}` | Redirect to long URL |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/analytics/url/{shortCode}` | URL statistics |
| `GET` | `/api/analytics/user/{userId}` | User analytics |
| `GET` | `/api/analytics/top` | Top URLs |

## 📊 Monitoring

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Application metrics
curl http://localhost:8081/actuator/metrics
```

### Kafka UI

Access Kafka UI for monitoring topics and messages:
```
http://localhost:8090
```

## 🐳 Docker Commands

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Restart a service
docker-compose restart url-service

# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Rebuild and start
docker-compose up --build
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file:

```env
# Database
POSTGRES_USER=urluser
POSTGRES_PASSWORD=your-secure-password
POSTGRES_DB=urlshortener

# Redis
REDIS_PASSWORD=your-redis-password

# Application
BASE_URL=http://localhost:8080
MAX_URLS_PER_USER=1000
```

## License

MIT