# Testing Suite

This directory contains performance and end-to-end tests for the URL Shortener Platform.

## Quick Start

```bash
# Ensure application is running
docker-compose up -d

# Run end-to-end test
./e2e/complete_user_journey.sh

# Run performance test
./performance/redirect_load_test.sh
```

## Tests

### End-to-End Test

**File:** `e2e/complete_user_journey.sh`

Tests the complete user journey:
1. Register user
2. Login
3. Create short URL
4. Click short URL (redirect)
5. View analytics

**Usage:**
```bash
./e2e/complete_user_journey.sh

# Custom API URL
API_URL=http://my-api.com ./e2e/complete_user_journey.sh
```

### Performance Test

**File:** `performance/redirect_load_test.sh`

Load tests the redirect endpoint and measures latency.

**Usage:**
```bash
./performance/redirect_load_test.sh

# Custom parameters
NUM_REQUESTS=2000 CONCURRENCY=100 ./performance/redirect_load_test.sh
```

**Metrics:**
- p50, p95, p99 latency
- Requests per second
- Success rate

**Target:** p95 < 50ms

## Requirements

- **curl**: HTTP client (usually pre-installed)
- **jq**: JSON processor (recommended)
- **ab**: Apache Bench for load testing

Install on macOS:
```bash
brew install jq httpd
```

Install on Linux:
```bash
sudo apt install jq apache2-utils
```
