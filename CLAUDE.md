# URL Shortener & Analytics Platform Description
Local-First, Production-Grade Architecture
A scalable URL shortener service (similar to bit.ly) designed for low-latency redirects, event-driven analytics, and horizontal scalability, built with Spring Boot, Kafka, Redis, PostgreSQL, and Kubernetes.

Design Goals
Ultra-fast redirects (< 50ms)
Asynchronous, replayable analytics processing
Clear separation between hot path and analytics path
Cost-efficient, local-first development
Production-ready architecture without unnecessary service sprawl

Microservices (Spring Boot)
1. API Gateway (Port 8080)
Technology: Spring Cloud Gateway + Spring Security
Responsibilities:
Single external entry point 
JWT validation
Authentication & authorization
Global rate limiting
Request routing
CORS handling
Why auth lives here:
Fewer network hops
Centralized security
Cleaner service responsibilities
Still practices real-world gateway patterns
User Data:
Stored in the same PostgreSQL instance as URL Service
Separate schema (e.g. users.*)
No standalone User Service

2. URL Service (Port 8081)
Core service for URL shortening and redirects
Responsibilities:
Generate short codes
Resolve short → long URLs
Handle expirations
Publish Kafka events
Enforce per-user limits (via Redis)
REST Endpoints:
POST /api/urls/shorten
GET /{shortCode} (redirect)
GET /api/urls/{shortCode}
DELETE /api/urls/{shortCode}
GET /api/urls/user/{userId}

Redis Usage
Redis is a first-class performance component, not a convenience cache.
Use cases:
Cache shortCode → longUrl mappings
Atomic click counters (INCR)
Rate limiting (per user / IP)
TTL-based URL expiration
Recently accessed URLs
Deployment:
Single-node Redis for demo
Redis Sentinel / Cluster documented as future scaling option

Kafka Architecture
Kafka is used only where it provides clear value: decoupling redirects from analytics.
Kafka Topics
1. url-lifecycle-events
Produced by: URL Service
 Events:
URLCreated
URLDeleted
URLExpired
Purpose:
Track URL state changes
Enable downstream consumers (analytics, auditing, alerts)

2. url-access-events
Produced by: Redirect path
 Events:
URLAccessed
Purpose:
Capture click data asynchronously
Preserve redirect performance
Enable replayable analytics

Why Kafka Makes Sense Here
Redirects must never block on analytics
High read : write ratio
Event replay for recovery
Easy to add new consumers (fraud detection, alerting)
Demonstrates real event-driven design
This is not event sourcing.
It is an append-only analytics event stream for replay and aggregation.

Analytics Service (Port 8082)
Single Spring Boot service with two logical layers
1. Ingest Layer (Kafka Consumers)
Consumes url-access-events
Consumes url-lifecycle-events
Performs:
Validation
Aggregation
Deduplication (idempotent processing)
2. Query Layer (REST APIs)
Exposes read-only endpoints
Serves dashboards and reports
REST Endpoints:
GET /api/analytics/url/{shortCode}
GET /api/analytics/user/{userId}
GET /api/analytics/top
GET /api/analytics/clicks/{shortCode}
This CQRS-lite approach keeps Kafka central without introducing extra services.

Databases (PostgreSQL)
URL Service Schema
urls
users
Analytics Service Schema
url_clicks (raw events)
url_analytics (aggregates)


PostgreSQL is the source of truth; Redis is strictly a performance layer.

Kubernetes Deployment
Cluster Components:
API Gateway Deployment
URL Service Deployment
Analytics Service Deployment
Kafka (Strimzi Operator)
Redis StatefulSet
PostgreSQL (local or managed)
Kubernetes Features Used:
Deployments
HPAs
ConfigMaps
Secrets
StatefulSets
Readiness & liveness probes
Kafka and Redis are deployed with persistence but minimal HA for demo realism.

Local-First Development Strategy
This project follows a Design for Scale, Run Locally approach.
Entire system runs via Docker Compose
Kafka, Redis, and PostgreSQL run locally
No always-on cloud infrastructure
Same architecture, same failure modes, zero cloud cost
docker-compose up

Scaling & Production Readiness
If deployed to production:
Kafka partitions scale analytics throughput
Redis can migrate to Cluster or Sentinel
Services scale horizontally
Kubernetes manifests apply without code changes


The demo version omits HA purely for cost efficiency.

Key Design Tradeoffs
No User Service
 Auth centralized at gateway for clarity and fewer hops.


No synchronous analytics writes
 Kafka ensures redirect latency remains low.


No event sourcing terminology
 Avoids unnecessary conceptual complexity.


Minimal Redis HA
 Documented, not overbuilt.

Frontend (React + TypeScript)
Pages
Home Page - Input long URL, get short URL
Dashboard - User's URLs with analytics
Analytics Page - Detailed stats for a URL (clicks over time, geographic data)
Login/Register - Authentication
Key Features
Copy to clipboard button
QR code generation for short URL
Real-time click counter (WebSocket or polling)

# Implementation Plan
Phase 1: Foundation & Database Setup - COMPLETED
Goal: Set up core infrastructure and data models
Project Structure Setup
Create multi-module Maven/Gradle project
Set up module structure: api-gateway, url-service, analytics-service, common
Configure shared dependencies and version management
Database Schema Design
URL Service Schema:
users table (id, username, email, password_hash, created_at)
urls table (id, short_code, long_url, user_id, created_at, expires_at, click_count)
Analytics Service Schema:
url_clicks table (id, short_code, timestamp, ip_address, user_agent, referer, country, city)
url_analytics table (short_code, total_clicks, unique_ips, last_updated)
Docker Compose Infrastructure
PostgreSQL container with init scripts
Redis container (single node)
Kafka + Zookeeper (using Strimzi or wurstmeister images)
Configure volumes for data persistence

Phase 2: URL Service (Core Business Logic) - COMPLETED
Goal: Build the heart of the system - URL shortening and redirects
Short Code Generation
Implement Base62 encoding algorithm
Add collision detection logic
Configure custom short code support
URL CRUD Operations
POST /api/urls/shorten - Create short URL
GET /api/urls/{shortCode} - Get URL details
DELETE /api/urls/{shortCode} - Delete URL
GET /api/urls/user/{userId} - List user's URLs
High-Performance Redirect Path
GET /{shortCode} - Redis-first lookup
PostgreSQL fallback with cache warming
Asynchronous Kafka event publishing
Target: < 50ms response time
Redis Integration
Cache layer for short code → long URL
Atomic click counters (INCR)
Per-user rate limiting
TTL-based expiration handling
Kafka Producer Integration
Publish URLCreated events to url-lifecycle-events
Publish URLDeleted events
Publish URLExpired events
Publish URLAccessed events to url-access-events (async)

Phase 3: API Gateway (Security & Routing) - COMPLETED
Goal: Single entry point with authentication
Spring Cloud Gateway Setup
Route configuration to URL Service and Analytics Service
CORS configuration
Authentication & Authorization
JWT token generation and validation
Spring Security configuration
User registration endpoint
Login endpoint
Rate Limiting
Global rate limiting filter
Per-IP rate limiting using Redis
Request/Response Filters
Logging filter
Error handling
Request validation

Phase 4: Analytics Service (Event Processing & Querying) - COMPLETED
Goal: Process click events and provide analytics APIs
Kafka Consumer Setup
Consumer for url-access-events topic
Consumer for url-lifecycle-events topic
Idempotent processing (deduplication)
Error handling and dead letter queue
Data Aggregation Logic
Real-time aggregation of click data
Geographic data extraction (IP → location)
User agent parsing
Time-series bucketing (hourly/daily)
Analytics Query APIs
GET /api/analytics/url/{shortCode} - URL-specific stats
GET /api/analytics/user/{userId} - User's aggregated stats
GET /api/analytics/top - Top URLs by clicks
GET /api/analytics/clicks/{shortCode} - Time-series click data
Data Storage
Persist raw events to url_clicks
Update aggregates in url_analytics
Optimize queries with indexes

Phase 5: Frontend (React + TypeScript)
Goal: User interface for URL management and analytics
Project Setup
Create React app with TypeScript
Set up routing (React Router)
Configure API client (Axios)
Set up state management (Zustand)
Authentication Pages
Login page
Registration page
JWT token storage (localStorage/cookies)
Protected route wrapper
Core Pages
Home Page: URL shortening form, display shortened URL
Dashboard: List of user's URLs with basic stats
Analytics Page: Detailed charts and metrics
Features Implementation
Copy-to-clipboard functionality
QR code generation (using library like qrcode.react)
Real-time click counter (polling or WebSocket)
Responsive design
Data Visualization
Charts for click trends (Chart.js or Recharts)
Top referrers table

Phase 6: Kubernetes Deployment
Goal: Production-ready deployment manifests
Kubernetes Manifests
Deployments for each service
Services (ClusterIP/LoadBalancer)
ConfigMaps for configuration
Secrets for credentials
StatefulSets for Redis
Kafka deployment using Strimzi Operator
Scaling Configuration
Horizontal Pod Autoscalers (HPA)
Resource requests and limits
Readiness and liveness probes
Persistent Storage
PersistentVolumeClaims for PostgreSQL
PersistentVolumeClaims for Kafka
Volume mounts for Redis

Phase 7: Testing & Optimization
Goal: Ensure reliability and performance
Unit Tests
Service layer tests
Repository tests
Utility function tests
Integration Tests
API endpoint tests
Kafka producer/consumer tests
Redis integration tests
Performance Testing
Load test redirect endpoint
Measure p50, p95, p99 latency
Optimize for < 50ms target
End-to-End Tests
Full user flow testing
Cross-service integration

Phase 8: Documentation & Polish
Goal: Make the project presentable and maintainable
Documentation
README with architecture diagram
Setup instructions for Docker Compose
Kubernetes deployment guide
API documentation (Swagger/OpenAPI)
Observability
Structured logging
Metrics endpoints (Prometheus)
Health check endpoints
Final Touches
Error handling improvements
Input validation
Security hardening
Code cleanup and formatting

