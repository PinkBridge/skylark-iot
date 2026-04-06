#!/usr/bin/env sh
set -euf

EMQX_HOST="${EMQX_HOST:-http://emqx:18083}"
API_KEY="${EMQX_API_KEY:-}"
API_SECRET="${EMQX_API_SECRET:-}"
WEBHOOK_SECRET="${IOT_ACCESS_WEBHOOK_SECRET:-}"
DB_USER="${EMQX_DASHBOARD_USERNAME:-admin}"
DB_PASS="${EMQX_DASHBOARD_PASSWORD:-public}"
SYSTEM_USER="${EMQX_SYSTEM_API_USERNAME:-system}"
SYSTEM_PASS="${SYSTEM_API_PASSWORD:-}"

echo "==> wait for EMQX HTTP API at ${EMQX_HOST} ..."
for i in $(seq 1 30); do
  if curl -sf "${EMQX_HOST}/status" >/dev/null 2>&1; then
    echo "EMQX is ready."
    break
  fi
  echo "  ...not ready yet (${i})"
  sleep 2
done

api() {
  token="$1"
  shift 1
  curl -sS \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${token}" \
    "$@"
}

echo "==> login to EMQX dashboard REST API ..."
LOGIN_JSON="$(curl -sS -H "Content-Type: application/json" \
  -d "{\"username\":\"${DB_USER}\",\"password\":\"${DB_PASS}\"}" \
  "${EMQX_HOST}/api/v5/login" || true)"
# response: {"token":"...","version":"...","license":{...}}
TOKEN="$(echo "$LOGIN_JSON" | sed -n 's/.*\"token\":\"\([^\"]*\)\".*/\1/p')"
if [ -z "$TOKEN" ]; then
  echo "ERROR: EMQX login failed. Response: $LOGIN_JSON"
  exit 1
fi
echo "EMQX login ok."

ensure_system_user() {
  if [ -z "$SYSTEM_PASS" ]; then
    echo "WARN: SYSTEM_API_PASSWORD is empty, skip ensure system api user."
    return 0
  fi
  echo "==> EMQX init: ensure dashboard user '${SYSTEM_USER}' for system API calls"
  USER_GET="$(api "$TOKEN" -X GET "$EMQX_HOST/api/v5/users/${SYSTEM_USER}" || true)"
  if echo "$USER_GET" | grep -qE "\"username\"[[:space:]]*:[[:space:]]*\"${SYSTEM_USER}\""; then
    echo "User ${SYSTEM_USER} exists; try updating password."
    api "$TOKEN" -X PUT "$EMQX_HOST/api/v5/users/${SYSTEM_USER}" -d "{
      \"password\": \"${SYSTEM_PASS}\"
    }" >/dev/null 2>&1 || echo "WARN: update user password failed (endpoint may differ by EMQX version)."
    return 0
  fi
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/users" -d "{
    \"username\": \"${SYSTEM_USER}\",
    \"password\": \"${SYSTEM_PASS}\",
    \"description\": \"system api user for iot-access\"
  }" >/dev/null 2>&1 || echo "WARN: create user ${SYSTEM_USER} failed (endpoint may differ by EMQX version)."
}

ensure_system_user

echo "==> EMQX init: create HTTP Auth (password_based -> /api/access/emqx/auth)"
api "$TOKEN" -X POST "$EMQX_HOST/api/v5/authentication" -d '{
  "mechanism": "password_based",
  "backend": "http",
  "method": "post",
  "url": "http://iot-access:8089/api/access/emqx/auth",
  "headers": {
    "content-type": "application/json"
  },
  "body": {
    "clientid": "${clientid}",
    "username": "${username}",
    "password": "${password}"
  },
  "enable": true
}' || echo "HTTP Auth may already exist, skipping."

echo "==> EMQX init: create HTTP ACL source (-> /api/access/emqx/acl)"
api "$TOKEN" -X POST "$EMQX_HOST/api/v5/authorization/sources" -d '{
  "type": "http",
  "method": "post",
  "url": "http://iot-access:8089/api/access/emqx/acl",
  "headers": {
    "content-type": "application/json"
  },
  "body": {
    "clientid": "${clientid}",
    "username": "${username}",
    "action": "${action}",
    "topic": "${topic}"
  },
  "enable": true
}' || echo "HTTP ACL source may already exist, skipping."

echo "==> EMQX init: create Webhook bridge for session events"
if [ -z "$WEBHOOK_SECRET" ]; then
  echo "WARN: IOT_ACCESS_WEBHOOK_SECRET is empty, webhook will be created without secret header."
  RES_HEADERS='{"Content-Type":"application/json"}'
else
  RES_HEADERS="{\"Content-Type\":\"application/json\",\"X-Emqx-Webhook-Secret\":\"$WEBHOOK_SECRET\"}"
fi

BRIDGES_LIST="$(api "$TOKEN" -X GET "$EMQX_HOST/api/v5/bridges" || true)"
bridge_exists() {
  name="$1"
  echo "$BRIDGES_LIST" | grep -qE "\"name\"[[:space:]]*:[[:space:]]*\"${name}\""
}

if bridge_exists "iot-session-webhook"; then
  echo "Bridge iot-session-webhook already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/bridges" -d "{
    \"type\": \"webhook\",
    \"name\": \"iot-session-webhook\",
    \"enable\": true,
    \"url\": \"http://iot-access:8089/api/access/emqx/webhook/session\",
    \"method\": \"post\",
    \"headers\": $RES_HEADERS
  }"
fi

echo "==> EMQX init: create Webhook bridge for upstream ingest"
if bridge_exists "iot-upstream-webhook"; then
  echo "Bridge iot-upstream-webhook already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/bridges" -d '{
    "type": "webhook",
    "name": "iot-upstream-webhook",
    "enable": true,
    "url": "http://iot-access:8089/api/access/upstream",
    "method": "post",
    "headers": {
      "Content-Type": "application/json"
    }
  }'
fi

# Rules: EMQX assigns a new id on every POST; duplicate "name" is still allowed, so re-running
# this script without a guard creates duplicate rules. List first and skip if name exists.
RULES_LIST="$(api "$TOKEN" -X GET "$EMQX_HOST/api/v5/rules" || true)"
rule_exists() {
  name="$1"
  echo "$RULES_LIST" | grep -qE "\"name\"[[:space:]]*:[[:space:]]*\"${name}\""
}

echo "==> EMQX init: create rule for client_connected -> webhook"
if rule_exists "iot-client-connected"; then
  echo "Rule iot-client-connected already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/rules" -d '{
    "name": "iot-client-connected",
    "sql": "SELECT clientid, username, peername, node, timestamp, '\''client.connected'\'' AS event FROM \"$events/client_connected\"",
    "actions": ["webhook:iot-session-webhook"],
    "enable": true
  }'
fi

echo "==> EMQX init: create rule for client_disconnected -> webhook"
if rule_exists "iot-client-disconnected"; then
  echo "Rule iot-client-disconnected already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/rules" -d '{
    "name": "iot-client-disconnected",
    "sql": "SELECT clientid, username, peername, reason, node, timestamp, '\''client.disconnected'\'' AS event FROM \"$events/client_disconnected\"",
    "actions": ["webhook:iot-session-webhook"],
    "enable": true
  }'
fi

echo "==> EMQX init: create rule for upstream model/up_raw -> webhook"
if rule_exists "iot-upstream-model-up-raw"; then
  echo "Rule iot-upstream-model-up-raw already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/rules" -d '{
    "name": "iot-upstream-model-up-raw",
    "sql": "SELECT topic, payload, timestamp FROM \"/sys/+/+/thing/model/up_raw\"",
    "actions": ["webhook:iot-upstream-webhook"],
    "enable": true
  }'
fi

echo "==> EMQX init: create rule for upstream event/post -> webhook"
if rule_exists "iot-upstream-event-post"; then
  echo "Rule iot-upstream-event-post already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/rules" -d '{
    "name": "iot-upstream-event-post",
    "sql": "SELECT topic, payload, timestamp FROM \"/sys/+/+/thing/event/+/post\"",
    "actions": ["webhook:iot-upstream-webhook"],
    "enable": true
  }'
fi

echo "==> EMQX init: create rule for upstream service/reply -> webhook"
if rule_exists "iot-upstream-service-reply"; then
  echo "Rule iot-upstream-service-reply already present, skipping."
else
  api "$TOKEN" -X POST "$EMQX_HOST/api/v5/rules" -d '{
    "name": "iot-upstream-service-reply",
    "sql": "SELECT topic, payload, timestamp FROM \"/sys/+/+/thing/service/+/reply\"",
    "actions": ["webhook:iot-upstream-webhook"],
    "enable": true
  }'
fi

echo "==> EMQX init finished."

