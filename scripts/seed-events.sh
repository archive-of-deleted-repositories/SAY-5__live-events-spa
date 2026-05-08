#!/usr/bin/env bash
# Posts a handful of synthetic events to the running API. Requires
# `livevents.seed.enabled=true` (the default in docker-compose.yml).
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
COUNT="${COUNT:-25}"

types=(order payment shipment alert)
sevs=(info info info warning warning critical)

for i in $(seq 1 "$COUNT"); do
  t="${types[$RANDOM % ${#types[@]}]}"
  s="${sevs[$RANDOM % ${#sevs[@]}]}"
  ts=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  curl -fsS -X POST "$BASE/api/events/_seed" \
    -H 'content-type: application/json' \
    -d "{\"type\":\"$t\",\"severity\":\"$s\",\"source\":\"seed-script\",\"timestamp\":\"$ts\",\"payload\":{\"i\":$i}}" \
    > /dev/null
  echo "seeded $i ($t/$s)"
done
