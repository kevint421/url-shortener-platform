#!/bin/bash

##############################################################################
# URL Shortener - Redirect Performance Test
#
# Tests redirect endpoint performance and measures p50, p95, p99 latency
# Target: < 50ms for p95
##############################################################################

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
API_URL="${API_URL:-http://localhost:8080}"
NUM_REQUESTS="${NUM_REQUESTS:-1000}"
CONCURRENCY="${CONCURRENCY:-50}"
TEST_SHORT_CODE=""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}URL Shortener - Redirect Performance Test${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Prerequisites check
if ! command -v ab &> /dev/null; then
    echo -e "${RED}Error: Apache Bench (ab) is required but not installed.${NC}"
    echo "Install it with:"
    echo "  macOS: brew install httpd"
    echo "  Ubuntu/Debian: sudo apt-get install apache2-utils"
    echo "  CentOS/RHEL: sudo yum install httpd-tools"
    exit 1
fi

if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is required but not installed.${NC}"
    exit 1
fi

echo "Test Configuration:"
echo "  API URL: $API_URL"
echo "  Requests: $NUM_REQUESTS"
echo "  Concurrency: $CONCURRENCY"
echo ""

# Step 1: Create a test short URL
echo -e "${YELLOW}Step 1: Creating test short URL...${NC}"

# Register a test user (if not exists)
REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"perftest","email":"perftest@example.com","password":"password123"}' \
  || echo '{"error":"may already exist"}')

# Login to get token
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"perftest","password":"password123"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | grep -o '[^"]*$' || echo "")

if [ -z "$TOKEN" ]; then
    echo -e "${RED}Failed to obtain authentication token${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo "✓ Authenticated successfully"

# Create a short URL for testing
CREATE_RESPONSE=$(curl -s -X POST "$API_URL/api/urls/shorten" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.example.com"}')

TEST_SHORT_CODE=$(echo "$CREATE_RESPONSE" | grep -o '"shortCode":"[^"]*' | grep -o '[^"]*$' || echo "")

if [ -z "$TEST_SHORT_CODE" ]; then
    echo -e "${RED}Failed to create test short URL${NC}"
    echo "Response: $CREATE_RESPONSE"
    exit 1
fi

echo "✓ Created test short URL: $TEST_SHORT_CODE"
echo ""

# Step 2: Run performance test
echo -e "${YELLOW}Step 2: Running redirect performance test...${NC}"
echo "  URL: $API_URL/$TEST_SHORT_CODE"
echo "  Total requests: $NUM_REQUESTS"
echo "  Concurrent requests: $CONCURRENCY"
echo ""

# Create temporary file for ab output
AB_OUTPUT=$(mktemp)

# Run Apache Bench
ab -n "$NUM_REQUESTS" -c "$CONCURRENCY" -g "$AB_OUTPUT.tsv" \
   "$API_URL/$TEST_SHORT_CODE" > "$AB_OUTPUT"

if [ $? -ne 0 ]; then
    echo -e "${RED}Performance test failed${NC}"
    cat "$AB_OUTPUT"
    rm -f "$AB_OUTPUT" "$AB_OUTPUT.tsv"
    exit 1
fi

# Parse results
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}PERFORMANCE TEST RESULTS${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Extract key metrics
REQUESTS_PER_SEC=$(grep "Requests per second" "$AB_OUTPUT" | awk '{print $4}')
TIME_PER_REQUEST=$(grep "Time per request.*mean\)" "$AB_OUTPUT" | awk '{print $4}')
P50=$(grep "50%" "$AB_OUTPUT" | awk '{print $2}')
P95=$(grep "95%" "$AB_OUTPUT" | awk '{print $2}')
P99=$(grep "99%" "$AB_OUTPUT" | awk '{print $2}')
FAILED_REQUESTS=$(grep "Failed requests" "$AB_OUTPUT" | awk '{print $3}')

echo "Throughput:"
echo "  Requests/sec: $REQUESTS_PER_SEC"
echo "  Time/request: ${TIME_PER_REQUEST} ms (mean)"
echo ""

echo "Latency Percentiles:"
echo "  p50 (median): ${P50} ms"
echo "  p95: ${P95} ms"
echo "  p99: ${P99} ms"
echo ""

echo "Reliability:"
echo "  Failed requests: $FAILED_REQUESTS"
echo ""

# Evaluate against target
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}EVALUATION${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

TARGET_P95=50
if [ $(echo "$P95 < $TARGET_P95" | bc -l) -eq 1 ]; then
    echo -e "${GREEN}✓ PASS: p95 latency (${P95}ms) is below target (${TARGET_P95}ms)${NC}"
else
    echo -e "${YELLOW}⚠ WARNING: p95 latency (${P95}ms) exceeds target (${TARGET_P95}ms)${NC}"
    echo "  Consider optimizing Redis cache, database queries, or increasing resources"
fi

if [ "$FAILED_REQUESTS" -eq 0 ]; then
    echo -e "${GREEN}✓ PASS: No failed requests (100% success rate)${NC}"
else
    TOTAL_REQUESTS=$NUM_REQUESTS
    SUCCESS_RATE=$(echo "scale=2; ($TOTAL_REQUESTS - $FAILED_REQUESTS) * 100 / $TOTAL_REQUESTS" | bc)
    echo -e "${YELLOW}⚠ WARNING: ${FAILED_REQUESTS} failed requests (${SUCCESS_RATE}% success rate)${NC}"
fi

echo ""
echo "Full Apache Bench output saved to: $AB_OUTPUT"
echo "TSV data saved to: $AB_OUTPUT.tsv (for graphing)"
echo ""

# Cleanup
echo -e "${YELLOW}Cleaning up test data...${NC}"
curl -s -X DELETE "$API_URL/api/urls/$TEST_SHORT_CODE" \
  -H "Authorization: Bearer $TOKEN" > /dev/null

echo "✓ Test complete"
echo ""

# Exit with appropriate code
if [ $(echo "$P95 < $TARGET_P95" | bc -l) -eq 1 ] && [ "$FAILED_REQUESTS" -eq 0 ]; then
    exit 0
else
    exit 1
fi
