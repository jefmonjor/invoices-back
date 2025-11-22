#!/bin/bash

BASE_URL="http://localhost:8080/api"
EMAIL="test_perm_user_$(date +%s)@example.com"
PASSWORD="password123"

echo "🚀 Starting Permission Verification Tests..."
echo "📧 Using email: $EMAIL"

# 1. Register User
echo -e "\n1️⃣  Registering new user..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"firstName\":\"Test\",\"lastName\":\"User\"}")

TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to register/login. Response: $REGISTER_RESPONSE"
  exit 1
fi

echo "✅ User authenticated successfully."

# 2. Test GET /api/users (Should be allowed for ROLE_USER)
echo -e "\n2️⃣  Testing GET /api/users (ROLE_USER)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/users" -H "Authorization: Bearer $TOKEN")

if [ "$HTTP_CODE" == "200" ]; then
  echo "✅ PASS - GET /api/users: Access granted as expected"
else
  echo "❌ FAIL - GET /api/users: Access denied! Status: $HTTP_CODE"
fi

# 3. Test POST /api/users (Should be denied for ROLE_USER)
echo -e "\n3️⃣  Testing POST /api/users (ROLE_USER)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"new_user_$(date +%s)@example.com\",\"password\":\"password123\",\"firstName\":\"New\",\"lastName\":\"User\",\"roles\":[\"ROLE_USER\"]}")

if [ "$HTTP_CODE" == "403" ]; then
  echo "✅ PASS - POST /api/users: Access denied as expected (403 Forbidden)"
else
  echo "❌ FAIL - POST /api/users: Unexpected status: $HTTP_CODE"
fi

# 4. Promote to ADMIN via Profile
echo -e "\n4️⃣  Promoting user to ADMIN via Profile..."
UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/users/profile" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Test\",\"lastName\":\"Admin\",\"roles\":[\"ROLE_ADMIN\"]}")

# Check if update was successful (simple check for firstName in response)
if [[ $UPDATE_RESPONSE == *"Test"* ]]; then
  echo "✅ Profile updated successfully"
else
  echo "❌ Failed to update profile. Response: $UPDATE_RESPONSE"
  exit 1
fi

# Re-login to get new token with ADMIN role
echo "   Re-authenticating to get new token with ADMIN role..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# 5. Test POST /api/users (Should be allowed for ROLE_ADMIN)
echo -e "\n5️⃣  Testing POST /api/users (ROLE_ADMIN)..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"new_user_admin_$(date +%s)@example.com\",\"password\":\"password123\",\"firstName\":\"New\",\"lastName\":\"User\",\"roles\":[\"ROLE_USER\"]}")

if [[ "$HTTP_CODE" == "200" || "$HTTP_CODE" == "201" || "$HTTP_CODE" == "409" ]]; then
  echo "✅ PASS - POST /api/users: Access granted (Status: $HTTP_CODE)"
else
  echo "❌ FAIL - POST /api/users: Access denied or error! Status: $HTTP_CODE"
fi

echo -e "\n🎉 Verification Complete!"
