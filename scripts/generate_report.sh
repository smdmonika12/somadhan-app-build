#!/usr/bin/env bash
# scripts/generate_report.sh
# Usage: generate_report.sh <path-to-captured-test-output.txt>
#
# See the note at the top of run_tests.sh — this file was also missing and
# had to be created in an earlier session so the CI job's report step
# ("cat feature-test-report.md >> $GITHUB_STEP_SUMMARY") has something to read.
#
# Scans the raw output of `run_tests.sh` (as captured by
# `run_tests.sh | tee test_output.txt` in full-test.yml) and renders a
# Markdown pass/fail summary.
#
# ✅ POLISH (Step 9, ২০২৬-০৯-২০): আগের ভার্সন শুধু গোটা রানের একটা মোট
# pass/fail সংখ্যা দিত — কোন feature-এ কী হলো বোঝা যেত না। এখন run_tests.sh-এর
# প্রতিটা টেস্ট ফাইলের আগে ছাপানো `=== Running supabase/tests/<file>.sql ===`
# মার্কার দিয়ে আউটপুটকে ফাইল-অনুযায়ী ভাগ করে, প্রতিটা ফাইলকে তার feature নামে
# ম্যাপ করে (নিচের FEATURE_NAME_OF ফাংশন — CI_TEST_SUITE_MASTER_PROMPT.md-এর
# Step তালিকা অনুসরণ করে), এবং feature-গ্রুপ করা একটা টেবিল দেখায়। একই feature-এর
# একাধিক ফাইল (part1/part2 ইত্যাদি) এক সারিতে যোগ হয়।

set -uo pipefail
INPUT="${1:-test_output.txt}"

if [ ! -f "$INPUT" ]; then
  echo "# Backend Feature Test Report"
  echo ""
  echo "⚠️ Test output file not found at \`$INPUT\` — নতুন কিছু রিপোর্ট করার নেই।"
  exit 0
fi

# ফাইলের বেসনাম (এক্সটেনশন ছাড়া, part-স্যাফিক্স ছাড়া) থেকে মানুষ-পড়া feature নাম।
# CI_TEST_SUITE_MASTER_PROMPT.md-এর Step 1-8 তালিকা অনুযায়ী — নতুন test file
# (Step 9/10-এ) এলে এখানে একটা কেস যোগ করতে হবে, নাহলে সেটা "(অজানা feature)"
# গ্রুপে পড়বে (নিচে fallback আছে, স্ক্রিপ্ট ভাঙবে না)।
feature_name_of() {
  local base="$1"
  case "$base" in
    01_bidding_flow*|02_bidding_flow*)                 echo "Step 1 — Bidding flow" ;;
    03_instant_jobs*|04_instant_jobs*)                 echo "Step 2 — Instant jobs / broadcasting" ;;
    05_job_release_escrow*|06_job_release_escrow*)     echo "Step 3 — Job release & escrow" ;;
    07_wallet_withdrawals*)                            echo "Step 4 — Wallet & withdrawals" ;;
    08_disputes*)                                       echo "Step 5 — Disputes" ;;
    09_kyc_roles*)                                       echo "Step 6 — KYC & roles" ;;
    10_admin_moderation_balance*)                       echo "Step 7 — Admin moderation & balance" ;;
    11_notifications_ratings*)                          echo "Step 8 — Notifications & ratings" ;;
    12_coverage_gap_fill*)                              echo "Step 10 — Coverage audit / gap-fill" ;;
    *)                                                   echo "(অজানা feature — নতুন test file, generate_report.sh-এ ম্যাপিং যোগ করো)" ;;
  esac
}

TOTAL_OK=$(grep -cE '^[[:space:]]*ok ' "$INPUT")
TOTAL_NOTOK=$(grep -cE '^[[:space:]]*not ok ' "$INPUT")

echo "# Backend Feature Test Report"
echo ""
echo "| | count |"
echo "|---|---|"
echo "| ✅ Passed | $TOTAL_OK |"
echo "| ❌ Failed | $TOTAL_NOTOK |"
echo ""

# --- ফাইল-অনুযায়ী ভাগ করা, feature-নামে group করে টেবিল বানানো ---
# run_tests.sh প্রতিটা টেস্ট ফাইলের ঠিক আগে ছাপায়: "=== Running supabase/tests/<name>.sql ==="
# awk দিয়ে সেই মার্কার-এর মাঝের ব্লকগুলো আলাদা করে প্রতিটার ok/not ok গোনা হচ্ছে।
declare -A FEATURE_OK=()
declare -A FEATURE_NOTOK=()
declare -A FEATURE_FILES=()
declare -a FEATURE_ORDER=()

current_file=""
while IFS= read -r line; do
  if [[ "$line" =~ ^===\ Running\ (supabase/tests/[A-Za-z0-9_]+\.sql)\ ===$ ]]; then
    current_file="${BASH_REMATCH[1]}"
    continue
  fi
  [ -z "$current_file" ] && continue

  base="$(basename "$current_file" .sql)"
  feature="$(feature_name_of "$base")"

  if [[ -z "${FEATURE_FILES[$feature]:-}" ]]; then
    FEATURE_ORDER+=("$feature")
  fi
  if [[ "$line" =~ ^[[:space:]]*not\ ok ]]; then
    FEATURE_NOTOK[$feature]=$(( ${FEATURE_NOTOK[$feature]:-0} + 1 ))
  elif [[ "$line" =~ ^[[:space:]]*ok\  ]]; then
    FEATURE_OK[$feature]=$(( ${FEATURE_OK[$feature]:-0} + 1 ))
  fi
  case " ${FEATURE_FILES[$feature]:-} " in
    *" $base "*) : ;;
    *) FEATURE_FILES[$feature]="${FEATURE_FILES[$feature]:-} $base" ;;
  esac
done < "$INPUT"

if [ "${#FEATURE_ORDER[@]}" -gt 0 ]; then
  echo "## Feature-অনুযায়ী ফলাফল"
  echo ""
  echo "| Feature | ফাইল | Passed | Failed |"
  echo "|---|---|---:|---:|"
  for feature in "${FEATURE_ORDER[@]+"${FEATURE_ORDER[@]}"}"; do
    files_list="$(echo "${FEATURE_FILES[$feature]}" | xargs -n1 | sort | xargs)"
    ok="${FEATURE_OK[$feature]:-0}"
    notok="${FEATURE_NOTOK[$feature]:-0}"
    mark="✅"
    [ "$notok" -gt 0 ] && mark="❌"
    echo "| $mark $feature | \`$files_list\` | $ok | $notok |"
  done
  echo ""
fi

if [ "$TOTAL_NOTOK" -gt 0 ]; then
  echo "## Failing assertions"
  echo ""
  echo '```'
  grep -E '^[[:space:]]*not ok ' "$INPUT"
  echo '```'
else
  echo "সব pgTAP assertion pass করেছে।"
fi
