#!/bin/bash

# RBAC Permission Testing Script
# This script tests the role-based access control implementation

BASE_URL="http://localhost:8080/api"
ADMIN_TOKEN=""
PM_DM_TOKEN=""
TESTER_TOKEN=""
CLIENT_TOKEN=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=== RBAC Permission Testing Script ==="
echo ""

# Function to make API call and check response
test_endpoint() {
    local method=$1
    local url=$2
    local token=$3
    local expected_status=$4
    local description=$5

    echo -n "Testing: $description... "

    if [ "$method" = "GET" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" "$BASE_URL$url")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{}' "$BASE_URL$url")
    fi

    if [ "$response" = "$expected_status" ]; then
        echo -e "${GREEN}PASS${NC} (Status: $response)"
    else
        echo -e "${RED}FAIL${NC} (Expected: $expected_status, Got: $response)"
    fi
}

# Login function
login_user() {
    local username=$1
    local password=$2
    local role=$3

    echo "Logging in as $role ($username)..."
    response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    token=$(echo $response | grep -o '"token":"[^"]*' | cut -d'"' -f4)

    if [ -z "$token" ]; then
        echo -e "${RED}Failed to login as $role${NC}"
        return 1
    else
        echo -e "${GREEN}Successfully logged in as $role${NC}"
        eval "${role}_TOKEN=\"$token\""
        return 0
    fi
}

# Test login for all roles
echo "=== User Authentication Tests ==="
login_user "admin_user" "password" "ADMIN"
login_user "pm_user" "password" "PM_DM"
login_user "tester_user" "password" "TESTER"
login_user "client_user" "password" "CLIENT"
echo ""

# Test ADMIN permissions (should have full access)
echo "=== ADMIN Role Tests ==="
test_endpoint "GET" "/dashboard/stats" "$ADMIN_TOKEN" "200" "ADMIN - Dashboard access"
test_endpoint "GET" "/clients" "$ADMIN_TOKEN" "200" "ADMIN - Clients list"
test_endpoint "GET" "/applications" "$ADMIN_TOKEN" "200" "ADMIN - Applications list"
test_endpoint "GET" "/test-plans" "$ADMIN_TOKEN" "200" "ADMIN - Test plans list"
test_endpoint "GET" "/vapt-reports" "$ADMIN_TOKEN" "200" "ADMIN - VAPT reports list"
test_endpoint "GET" "/defects" "$ADMIN_TOKEN" "200" "ADMIN - Defects list"
test_endpoint "GET" "/users" "$ADMIN_TOKEN" "200" "ADMIN - Users list"
echo ""

# Test PM_DM permissions
echo "=== PM_DM Role Tests ==="
test_endpoint "GET" "/dashboard/stats" "$PM_DM_TOKEN" "200" "PM_DM - Dashboard access"
test_endpoint "GET" "/clients" "$PM_DM_TOKEN" "200" "PM_DM - Clients list (read-only)"
test_endpoint "GET" "/applications" "$PM_DM_TOKEN" "200" "PM_DM - Applications list (read-only)"
test_endpoint "POST" "/clients" "$PM_DM_TOKEN" "403" "PM_DM - Create client (should be denied)"
test_endpoint "POST" "/applications" "$PM_DM_TOKEN" "403" "PM_DM - Create application (should be denied)"
test_endpoint "GET" "/test-plans" "$PM_DM_TOKEN" "403" "PM_DM - Test plans access (should be denied)"
test_endpoint "GET" "/defects" "$PM_DM_TOKEN" "403" "PM_DM - Defects access (should be denied)"
test_endpoint "GET" "/users" "$PM_DM_TOKEN" "403" "PM_DM - Users access (should be denied)"

# Test VAPT report download access (PM_DM should only download assigned reports)
# Assuming report ID 1 is for TechCorp (assigned to PM_DM), report ID 3 is for FinanceHub (assigned to PM_DM)
test_endpoint "GET" "/vapt-reports/download/1/pdf" "$PM_DM_TOKEN" "200" "PM_DM - Download assigned VAPT report"
test_endpoint "GET" "/vapt-reports/download/3/pdf" "$PM_DM_TOKEN" "200" "PM_DM - Download assigned VAPT report"
# Test access to unassigned report (should be 403)
test_endpoint "GET" "/vapt-reports/download/2/pdf" "$PM_DM_TOKEN" "403" "PM_DM - Download unassigned VAPT report (should be denied)"
echo ""

# Test TESTER permissions
echo "=== TESTER Role Tests ==="
test_endpoint "GET" "/dashboard/stats" "$TESTER_TOKEN" "200" "TESTER - Dashboard access"
test_endpoint "GET" "/test-plans" "$TESTER_TOKEN" "200" "TESTER - Test plans access"
test_endpoint "GET" "/vapt-reports" "$TESTER_TOKEN" "200" "TESTER - VAPT reports list"
test_endpoint "POST" "/vapt-reports/initialize" "$TESTER_TOKEN" "200" "TESTER - Initialize VAPT report"
test_endpoint "GET" "/clients" "$TESTER_TOKEN" "403" "TESTER - Clients access (should be denied)"
test_endpoint "GET" "/defects" "$TESTER_TOKEN" "403" "TESTER - Defects access (should be denied)"
test_endpoint "GET" "/users" "$TESTER_TOKEN" "403" "TESTER - Users access (should be denied)"
echo ""

# Test CLIENT permissions
echo "=== CLIENT Role Tests ==="
test_endpoint "GET" "/dashboard/stats" "$CLIENT_TOKEN" "200" "CLIENT - Dashboard access"
test_endpoint "GET" "/clients" "$CLIENT_TOKEN" "403" "CLIENT - Clients access (should be denied)"
test_endpoint "GET" "/applications" "$CLIENT_TOKEN" "403" "CLIENT - Applications access (should be denied)"
test_endpoint "GET" "/test-plans" "$CLIENT_TOKEN" "403" "CLIENT - Test plans access (should be denied)"
test_endpoint "GET" "/vapt-reports" "$CLIENT_TOKEN" "403" "CLIENT - VAPT reports access (should be denied)"
test_endpoint "GET" "/defects" "$CLIENT_TOKEN" "403" "CLIENT - Defects access (should be denied)"
test_endpoint "GET" "/users" "$CLIENT_TOKEN" "403" "CLIENT - Users access (should be denied)"
echo ""

echo "=== Testing Complete ==="
echo "Note: This script assumes the application is running on localhost:8080"
echo "and that the test users exist with the specified credentials."
echo "Update the login credentials and URLs as needed for your environment."