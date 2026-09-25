#!/usr/bin/env bash
# scripts/run_tests.sh
#
# ⚠️ GAP FOUND (Step 1 session, 2026-09-19): `full-test.yml` (Step 0) already
# referenced this script and `generate_report.sh`, but neither file actually
# existed in the repo — so the CI backend-feature-tests job would have failed
# immediately at "chmod +x scripts/run_tests.sh" on the very first push. This
# had to be created before any pgTAP test (Step 1 onward) could ever run.
#
# What this does:
#   1. Applies any `supabase/tests/*_schema_stub.sql` files (inferred/TEMPORARY
#      table + helper-function stubs — see rule #6 in
#      CI_TEST_SUITE_MASTER_PROMPT.md) BEFORE the real pgTAP test files, since
#      the real migrations never CREATE TABLE the core tables (users/problems/
#      bids/etc. — they were created directly on the live Supabase project,
#      only later ALTERs are captured as migrations).
#   2. Loads supabase/tests/00_helpers.sql (test.login_as / test.seed_users).
#   3. Runs every other supabase/tests/*.sql file (the actual pgTAP suites) in
#      sorted order, printing each file's TAP output.
#   4. Because `psql -f` exits 0 even when a pgTAP assertion is "not ok"
#      (psql only errors on a hard SQL error), we capture each file's output
#      and grep it for "not ok" lines ourselves to decide the script's real
#      exit code — otherwise a failing assertion would silently show as a
#      green CI run.

# ⚠️ FIX (Step 5 PART 2 সেশনের পরের follow-up, 2026-09-19) — failure-detection-এর ৩টা ফাঁক
# ধরা পড়ে (কোনোটাই real psql-এ চালিয়ে নয়, বরং কোড পড়ে + সিমুলেশনে; তাই আসল CI-তে প্রথম
# রানে নিশ্চিত হতে হবে — scripts/selftest_test_runner.sh শুধু এই স্ক্রিপ্টের লজিক পরীক্ষা করে):
#   (ক) psql-এর ডিফল্ট aligned আউটপুটে প্রতিটা row-এর আগে একটা space থাকে (" not ok 3 - …"),
#       তাই আগের `grep -q '^not ok'` কখনো match করত না → failing assertion-এও CI সবুজ।
#       এখন `psql -t -A` (tuples-only, unaligned → TAP লাইন column 0-তে) *এবং* tolerant
#       regex (`^[[:space:]]*not ok`) — দুটোই, যাতে একটা ব্যর্থ হলেও অন্যটা ধরে।
#   (খ) test file-এর psql exit code আর hard `ERROR:` লাইন এতদিন উপেক্ষিত ছিল। এখন
#       non-zero exit বা `ERROR:` থাকলে fail।
#   (গ) pgTAP-এর `# Looks like you planned N tests but ran M` / `# Looks like you failed …`
#       (plan mismatch বা ব্যর্থতার সারাংশ) এখন fail হিসেবে গণ্য — plan()-এর ভুল সংখ্যাও ধরা পড়ে।

set -uo pipefail
export PGPASSWORD="${PGPASSWORD:-postgres}"
PSQL="psql -h localhost -U postgres -X -q -v ON_ERROR_STOP=1"

echo "== Applying schema stubs (supabase/tests/*_schema_stub.sql) =="
shopt -s nullglob
stub_files=(supabase/tests/*_schema_stub.sql)
if [ ${#stub_files[@]} -eq 0 ]; then
  echo "(কোনো schema stub পাওয়া যায়নি — যদি পরের কোনো step নতুন টেবিল ব্যবহার করে, সেই step-এই stub বানাতে হবে)"
fi
for f in $(printf '%s\n' "${stub_files[@]}" | sort); do
  echo ">> applying $f"
  $PSQL -f "$f"
  if [ $? -ne 0 ]; then
    echo "‼️ SCHEMA STUB FAILED: $f"
    exit 1
  fi
done

echo "== Loading test helpers (00_helpers.sql) =="
$PSQL -f supabase/tests/00_helpers.sql
if [ $? -ne 0 ]; then
  echo "‼️ HELPERS FAILED TO LOAD"
  exit 1
fi

FAIL_COUNT=0
test_files=$(ls supabase/tests/*.sql 2>/dev/null | grep -v '/00_helpers\.sql$' | grep -v '_schema_stub\.sql$' | sort)

if [ -z "$test_files" ]; then
  echo "(supabase/tests/-এ এখনো কোনো actual pgTAP test file নেই — শুধু helpers/stub আছে)"
fi

FAILED_FILES=()
for f in $test_files; do
  echo "=== Running $f ==="
  # -t -A: tuples-only + unaligned → TAP লাইনগুলো column 0 থেকে শুরু হয় (উপরের ফিক্স (ক))
  OUT=$(psql -h localhost -U postgres -X -q -t -A -f "$f" 2>&1)
  RC=$?
  echo "$OUT"

  REASON=""
  if [ "$RC" -ne 0 ]; then
    REASON="psql exit code $RC"
  elif echo "$OUT" | grep -qE '^[[:space:]]*not ok'; then
    REASON="'not ok' assertion"
  elif echo "$OUT" | grep -qE '(^|:)[[:space:]]*ERROR:'; then
    REASON="hard SQL ERROR"
  elif echo "$OUT" | grep -qE '^[[:space:]]*# Looks like you (planned|failed)'; then
    REASON="pgTAP plan mismatch / failure summary"
  fi

  if [ -n "$REASON" ]; then
    echo "‼️ FAILED: $f — $REASON"
    FAILED_FILES+=("$f")
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi
done

if [ "$FAIL_COUNT" -gt 0 ]; then
  echo "RESULT: $FAIL_COUNT test file(s) failed:"
  printf '  - %s\n' "${FAILED_FILES[@]}"
  exit 1
fi

echo "RESULT: all pgTAP assertions passed."
exit 0
