#!/usr/bin/env bash
#
# Milestone 5 gate.
#
# One Balance Enquiry must satisfy all five criteria from the brief, and one
# IMPS transfer must prove the audit path. Anything less and Layer 1 is not
# done.
#
#   ./smoke-test.sh
#
# Works against a docker compose stack or against services started with
# java -jar. Loki is skipped rather than failed when it is not running, since
# it only exists in the compose stack.
#
# Exits non-zero if anything failed, so it can gate a pipeline.

set -uo pipefail

BANK=${BANK_URL:-http://localhost:8080}
API=${API_URL:-http://localhost:8090}
LOKI=${LOKI_URL:-http://localhost:3100}

MYSQL_CMD=${MYSQL_CMD:-mysql}
MYSQL_ARGS=${MYSQL_ARGS:--h 127.0.0.1 -u npst -pnpst123 observability}

TRACE="SMOKE-$(date +%s)"
PASS=0
FAIL=0
SKIPPED=0

GREEN=$'\033[32m'
RED=$'\033[31m'
YELLOW=$'\033[33m'
RESET=$'\033[0m'

# Quoted binary - its path contains spaces on Windows - with the argument
# string left to split as intended.
query() {
  "$MYSQL_CMD" $MYSQL_ARGS -N -B -e "$1" 2>/dev/null | tr -d '\r' | tail -1
}

# Always yields a clean integer. An empty result would otherwise turn a
# numeric comparison into a syntax error rather than a failed check.
number() {
  local digits
  digits=$(printf '%s' "${1:-}" | tr -cd '0-9')
  printf '%s' "${digits:-0}"
}

pass() { printf '  %sPASS%s  %s\n' "$GREEN" "$RESET" "$1"; PASS=$((PASS + 1)); }

fail() {
  printf '  %sFAIL%s  %s\n' "$RED" "$RESET" "$1"
  [ -n "${2:-}" ] && printf '        %s\n' "$2"
  FAIL=$((FAIL + 1))
}

skip() {
  printf '  %sSKIP%s  %s\n' "$YELLOW" "$RESET" "$1"
  printf '        %s\n' "$2"
  SKIPPED=$((SKIPPED + 1))
}

check() {
  if [ "$2" = "true" ]; then pass "$1"; else fail "$1" "${3:-}"; fi
}

echo
echo "Milestone 5 gate - trace $TRACE"
echo
echo "Balance Enquiry"

BODY=$(curl -s -w '\n%{http_code}' \
  -H "X-Trace-Id: $TRACE" -H "X-Channel: MOBILE" \
  -H "X-Device-Id: smoke-device" -H "X-Customer-Id: CIF-99001" \
  "$BANK/api/v1/accounts/918273645510/balance")

STATUS=$(printf '%s' "$BODY" | tail -1)
PAYLOAD=$(printf '%s' "$BODY" | head -1)

# 1 -------------------------------------------------------------------------
check "API returns success (HTTP $STATUS)" \
      "$([ "$STATUS" = "200" ] && echo true || echo false)" "$PAYLOAD"

# The log leaves on a background thread, by design. Give it a moment to land.
sleep 4

# 2 -------------------------------------------------------------------------
if curl -s -o /dev/null --max-time 3 "$LOKI/ready" 2>/dev/null; then

  LOKI_HITS=$(number "$(curl -s -G "$LOKI/loki/api/v1/query_range" \
    --data-urlencode "query={job=\"observability\"} |= \"$TRACE\"" \
    --data-urlencode "limit=5" 2>/dev/null | grep -c "$TRACE")")

  check "log is searchable in Grafana via Loki (hits=$LOKI_HITS)" \
        "$([ "$LOKI_HITS" -gt 0 ] && echo true || echo false)" \
        "Loki returned no lines for this trace"
else
  skip "log is searchable in Grafana via Loki" \
       "Loki is not reachable - run 'docker compose up -d' to include this check"
fi

# 3 -------------------------------------------------------------------------
SENT=$(curl -s "$BANK/actuator/prometheus" | grep '^observability_logs_sent_total' | awk '{print $2}')
DROPPED=$(curl -s "$BANK/actuator/prometheus" | grep '^observability_logs_dropped_total' | awk '{print $2}')

check "logging-api accepted the log (sent=${SENT:-0} dropped=${DROPPED:-0})" \
      "$(awk -v s="${SENT:-0}" 'BEGIN { exit !(s > 0) }' && echo true || echo false)"

# 4 -------------------------------------------------------------------------
ROWS=$(number "$(query "SELECT COUNT(*) FROM application_logs WHERE trace_id='$TRACE';")")

check "MySQL stored the log (rows=$ROWS)" \
      "$([ "$ROWS" -gt 0 ] && echo true || echo false)"

# 5 -------------------------------------------------------------------------
FOUND=$(number "$(curl -s "$API/api/v1/logs/$TRACE" | grep -c "\"traceId\":\"$TRACE\"")")

check "search by trace id returns the record" \
      "$([ "$FOUND" -gt 0 ] && echo true || echo false)"

# ---------------------------------------------------------------------------
echo
echo "IMPS transfer - the audit path"

TRACE_TXN="$TRACE-TXN"

TXN_STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST \
  -H "Content-Type: application/json" -H "X-Trace-Id: $TRACE_TXN" \
  -H "X-Channel: MOBILE" -H "X-Customer-Id: CIF-99001" \
  -d '{"debitAccount":"918273645510","beneficiaryAccount":"918273645599","amount":82450.00,"otp":"483920"}' \
  "$BANK/api/v1/transfers/imps")

check "transfer succeeded (HTTP $TXN_STATUS)" \
      "$([ "$TXN_STATUS" = "201" ] && echo true || echo false)"

sleep 4

AUDIT=$(query "SELECT CONCAT(action,' ',amount,' ',currency,' ',customer_id) FROM audit_logs WHERE trace_id='$TRACE_TXN';")

check "audit row written with amount and customer [$AUDIT]" \
      "$([ -n "$AUDIT" ] && echo true || echo false)" \
      "no audit row for this trace"

CHAINED=$(number "$(query "SELECT COUNT(*) FROM audit_logs WHERE trace_id='$TRACE_TXN' AND row_hash IS NOT NULL AND prev_hash IS NOT NULL;")")

check "audit row is linked into the hash chain" \
      "$([ "$CHAINED" -gt 0 ] && echo true || echo false)"

# The check that matters most. Deliberately searches the whole table, not just
# this trace - an OTP anywhere is a regression.
LEAKS=$(number "$(query "SELECT COUNT(*) FROM application_logs WHERE metadata LIKE '%483920%' OR message LIKE '%483920%';")")

check "OTP appears in no stored application log" \
      "$([ "$LEAKS" -eq 0 ] && echo true || echo false)" \
      "an OTP reached the database - masking has regressed"

AUDIT_LEAKS=$(number "$(query "SELECT COUNT(*) FROM audit_logs WHERE description LIKE '%483920%' OR business_context LIKE '%483920%';")")

check "OTP appears in no audit record" \
      "$([ "$AUDIT_LEAKS" -eq 0 ] && echo true || echo false)"

# ---------------------------------------------------------------------------
echo
printf '  %d passed, %d failed, %d skipped\n' "$PASS" "$FAIL" "$SKIPPED"
echo

if [ "$FAIL" -ne 0 ]; then
  echo "Milestone 5 gate: FAILED"
  exit 1
fi

echo "Milestone 5 gate: PASSED"
