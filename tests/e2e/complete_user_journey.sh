#!/bin/bash

##############################################################################
# URL Shortener - End-to-End Test
#
# Tests complete user journey:
# 1. Register user
# 2. Login
# 3. Create short URL
# 4. Click short URL (redirect)
# 5. View analytics
##############################################################################

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
API_URL="${API_URL:-http://localhost:8080}"
TEST_USER="e2etest_$(date +%s)"
TEST_EMAIL="${TEST_USER}@example.com"
TEST_PASSWORD="password123"
TEST_LONG_URL="https://www.github.com"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}URL Shortener - End-to-End Test${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "API URL: $API_URL"
echo "Test User: $TEST_USER"
echo ""

# Prerequisites check
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl is required but not installed.${NC}"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq is not installed. Install for better output parsing.${NC}"
    echo "  macOS: brew install jq"
    echo "  Ubuntu/Debian: sudo apt-get install jq"
    echo ""
fi

# Test results
TESTS_PASSED=0
TESTS_FAILED=0

function assert_success() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((TESTS_PASSED++)) || true
    else
        echo -e "${RED}✗ FAIL: $2${NC}"
        ((TESTS_FAILED++)) || true
        return 1
    fi
}

function assert_contains() {
    if echo "$1" | grep -q "$2"; then
        echo -e "${GREEN}✓ PASS${NC}"
        ((TESTS_PASSED++)) || true
    else
        echo -e "${RED}✗ FAIL: Expected response to contain '$2'${NC}"
        echo "  Actual response: $1"
        ((TESTS_FAILED++)) || true
        return 1
    fi
}

# Test 1: Register User
echo -e "${BLUE}Test 1: Register User${NC}"
echo -n "  Registering user '$TEST_USER'... "

REGISTER_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TEST_USER\",\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | tail -n1)
REGISTER_BODY=$(echo "$REGISTER_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    assert_success 0 "Registration failed"
else
    assert_success 1 "Registration failed with HTTP $HTTP_CODE: $REGISTER_BODY"
fi
echo ""

# Test 2: Login
echo -e "${BLUE}Test 2: Login${NC}"
echo -n "  Logging in as '$TEST_USER'... "

LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASSWORD\"}")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -n1)
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    # Extract token
    if command -v jq &> /dev/null; then
        TOKEN=$(echo "$LOGIN_BODY" | jq -r '.token')
    else
        TOKEN=$(echo "$LOGIN_BODY" | grep -o '"token":"[^"]*' | grep -o '[^"]*$')
    fi

    if [ -n "$TOKEN" ] && [ "$TOKEN" != "null" ]; then
        assert_success 0 "Login failed"
        echo "  Token received: ${TOKEN:0:20}..."
    else
        assert_success 1 "Login succeeded but no token received"
    fi
else
    assert_success 1 "Login failed with HTTP $HTTP_CODE: $LOGIN_BODY"
fi
echo ""

# Test 3: Create Short URL
echo -e "${BLUE}Test 3: Create Short URL${NC}"
echo -n "  Creating short URL for '$TEST_LONG_URL'... "

CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL/api/urls/shorten" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"longUrl\":\"$TEST_LONG_URL\"}")

HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    # Extract short code
    if command -v jq &> /dev/null; then
        SHORT_CODE=$(echo "$CREATE_BODY" | jq -r '.shortCode')
    else
        SHORT_CODE=$(echo "$CREATE_BODY" | grep -o '"shortCode":"[^"]*' | grep -o '[^"]*$')
    fi

    if [ -n "$SHORT_CODE" ] && [ "$SHORT_CODE" != "null" ]; then
        assert_success 0 "URL creation failed"
        echo "  Short code: $SHORT_CODE"
        echo "  Short URL: $API_URL/$SHORT_CODE"
    else
        assert_success 1 "URL creation succeeded but no short code received"
    fi
else
    assert_success 1 "URL creation failed with HTTP $HTTP_CODE: $CREATE_BODY"
fi
echo ""

# Test 4: Redirect (Click Short URL)
echo -e "${BLUE}Test 4: Redirect (Click Short URL)${NC}"
echo -n "  Testing redirect from /$SHORT_CODE... "

REDIRECT_RESPONSE=$(curl -s -w "\n%{http_code}" -L -o /dev/null "$API_URL/$SHORT_CODE")

HTTP_CODE=$(echo "$REDIRECT_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 302 ]; then
    assert_success 0 "Redirect failed"
    echo "  Redirected successfully (HTTP $HTTP_CODE)"
else
    assert_success 1 "Redirect failed with HTTP $HTTP_CODE"
fi
echo ""

# Wait a moment for analytics to process
echo "  Waiting 2 seconds for analytics processing..."
sleep 2
echo ""

# Test 5: View Analytics
echo -e "${BLUE}Test 5: View Analytics${NC}"
echo -n "  Fetching analytics for short code '$SHORT_CODE'... "

ANALYTICS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$API_URL/api/analytics/url/$SHORT_CODE" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$ANALYTICS_RESPONSE" | tail -n1)
ANALYTICS_BODY=$(echo "$ANALYTICS_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    # Check if analytics contains expected data
    if echo "$ANALYTICS_BODY" | grep -q "$SHORT_CODE"; then
        assert_success 0 "Analytics fetch failed"

        # Display analytics summary
        if command -v jq &> /dev/null; then
            echo ""
            echo "  Analytics Summary:"
            echo "    Total Clicks: $(echo "$ANALYTICS_BODY" | jq -r '.totalClicks // 0')"
            echo "    Unique IPs: $(echo "$ANALYTICS_BODY" | jq -r '.uniqueIps // 0')"
        fi
    else
        assert_success 1 "Analytics fetch succeeded but data incomplete"
    fi
else
    assert_success 1 "Analytics fetch failed with HTTP $HTTP_CODE: $ANALYTICS_BODY"
fi
echo ""

# Cleanup
echo -e "${YELLOW}Cleaning up test data...${NC}"
echo -n "  Deleting short URL... "

DELETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "$API_URL/api/urls/$SHORT_CODE" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$DELETE_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 204 ]; then
    echo -e "${GREEN}✓ Deleted${NC}"
else
    echo -e "${YELLOW}⚠ Could not delete (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# Summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}TEST SUMMARY${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Tests Passed: $TESTS_PASSED"
echo "Tests Failed: $TESTS_FAILED"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
    echo ""
    echo "The complete user journey works end-to-end:"
    echo "  1. ✓ User registration"
    echo "  2. ✓ User login"
    echo "  3. ✓ URL shortening"
    echo "  4. ✓ Redirect functionality"
    echo "  5. ✓ Analytics tracking"
    exit 0
else
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    echo ""
    echo "Please check the logs above for details."
    exit 1
fi
