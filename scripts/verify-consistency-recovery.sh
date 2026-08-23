#!/usr/bin/env bash
set -euo pipefail

API_URL="${API_URL:-http://localhost:8081}"
KEY_PREFIX="consistency-$(date +%s)"
CREATE_KEY="${KEY_PREFIX}-create"
CORRECTION_KEY="${KEY_PREFIX}-correction"
RESULT_ID="DPP-${KEY_PREFIX}"

json_field() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)[sys.argv[1]])' "$1"
}

wait_for_api() {
  for _ in $(seq 1 60); do
    if curl -fsS "${API_URL}/actuator/health" >/dev/null 2>&1; then return 0; fi
    sleep 2
  done
  echo "API did not become healthy" >&2
  return 1
}

echo "Starting stack..."
docker compose up -d --build
wait_for_api

echo "Creating review..."
create_response=$(curl -fsS -X POST "${API_URL}/api/dpp-reviews" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: ${CREATE_KEY}" \
  -d "{\"dppResultId\":\"${RESULT_ID}\",\"transmissionOwnerId\":\"TO-DEMO\"}")
review_id=$(printf '%s' "$create_response" | json_field reviewId)

echo "Review: ${review_id}"

echo "Recreating API with workflow failure enabled..."
WORKFLOW_SIMULATE_FAILURE=true docker compose up -d --force-recreate dpp-collaboration-api
wait_for_api

set +e
failure_body=$(mktemp)
status=$(curl -sS -o "$failure_body" -w '%{http_code}' -X POST \
  "${API_URL}/api/dpp-reviews/${review_id}/corrections" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: ${CORRECTION_KEY}" \
  -d '{"comment":"Automated consistency recovery test"}')
set -e
if [[ "$status" != "502" ]]; then
  echo "Expected 502 from simulated workflow failure, got ${status}" >&2
  cat "$failure_body" >&2
  exit 1
fi
rm -f "$failure_body"

corrections=$(curl -fsS "${API_URL}/api/dpp-reviews/${review_id}/corrections")
count=$(printf '%s' "$corrections" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))')
[[ "$count" == "1" ]] || { echo "Expected one persisted correction after failure, got ${count}" >&2; exit 1; }

echo "Recreating API with workflow failure disabled..."
WORKFLOW_SIMULATE_FAILURE=false docker compose up -d --force-recreate dpp-collaboration-api
wait_for_api

curl -fsS -X POST "${API_URL}/api/dpp-reviews/${review_id}/corrections" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: ${CORRECTION_KEY}" \
  -d '{"comment":"Automated consistency recovery test"}' >/dev/null

corrections=$(curl -fsS "${API_URL}/api/dpp-reviews/${review_id}/corrections")
count=$(printf '%s' "$corrections" | python3 -c 'import json,sys; print(len(json.load(sys.stdin)))')
[[ "$count" == "1" ]] || { echo "Expected one correction after reconciliation, got ${count}" >&2; exit 1; }

review=$(curl -fsS "${API_URL}/api/dpp-reviews/${review_id}")
state=$(printf '%s' "$review" | json_field status)
[[ "$state" == "PENDING_MISO_REVIEW" ]] || { echo "Expected PENDING_MISO_REVIEW, got ${state}" >&2; exit 1; }

echo "PASS: database mutation survived workflow failure and same-key retry reconciled without duplication."
