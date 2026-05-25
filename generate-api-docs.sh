#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────
# generate-api-docs.sh
#
# Exports the OpenAPI 3 contract from a running
# Rechnung-App instance into docs/openapi.yaml
#
# Prerequisites:
#   1. The application must be running (./mvnw spring-boot:run)
#   2. curl must be installed
#
# Usage:
#   ./generate-api-docs.sh
#   ./generate-api-docs.sh http://localhost:8080   # custom base URL
# ──────────────────────────────────────────────────────────

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
OUTPUT_DIR="docs"
OUTPUT_FILE="${OUTPUT_DIR}/openapi.yaml"

mkdir -p "${OUTPUT_DIR}"

echo "🔄 Fetching OpenAPI spec from ${BASE_URL}/v3/api-docs.yaml ..."

HTTP_CODE=$(curl -s -o "${OUTPUT_FILE}" -w "%{http_code}" "${BASE_URL}/v3/api-docs.yaml")

if [ "${HTTP_CODE}" -ne 200 ]; then
  echo "❌ Failed to fetch OpenAPI spec (HTTP ${HTTP_CODE})."
  echo "   Make sure the application is running at ${BASE_URL}."
  rm -f "${OUTPUT_FILE}"
  exit 1
fi

echo "✅ OpenAPI spec saved to ${OUTPUT_FILE}"
echo ""
echo "You can now share this file with the frontend team or use it"
echo "to generate API clients (e.g. openapi-generator, orval, etc.)."
