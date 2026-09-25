#!/usr/bin/env bash
# scripts/selftest_scan_duplicate_overloads.sh
#
# scan_duplicate_overloads.sh নিজেই ভবিষ্যতে silently ভেঙে না যায় তা নিশ্চিত করতে —
# একটা mktemp fixture ফোল্ডারে ইচ্ছাকৃত duplicate/clean/dropped-overload migration ফাইল
# বসিয়ে scanner চালিয়ে exit code + আউটপুট যাচাই করে। কোনো real DB/Postgres লাগে না,
# তাই CI-তে সবচেয়ে সস্তা/দ্রুততম self-test হিসেবে চলতে পারে (`selftest_test_runner.sh`-এর
# মতোই — সেটা run_tests.sh/generate_report.sh-এর জন্য, এটা scan_duplicate_overloads.sh-এর জন্য)।
#
# চালানোর নিয়ম (repo root থেকে):
#   bash scripts/selftest_scan_duplicate_overloads.sh
#
# Exit code: 0 হলে সব কেস pass, নাহলে non-zero (FAIL-সংখ্যা যত)।

set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCANNER="$REPO/scripts/scan_duplicate_overloads.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

PASS=0
FAIL=0

# expect_exit <case-name> <expected-exit> <fixture-subdir>
expect_exit() {
  local name="$1" want="$2" dir="$3" got out
  out="$(bash "$SCANNER" "$dir" 2>&1)"
  got=$?
  if [ "$got" -eq "$want" ]; then
    PASS=$((PASS+1)); echo "  ok   - $name (exit $got)"
  else
    FAIL=$((FAIL+1))
    echo "  FAIL - $name: expected exit $want, got $got"
    echo "         --- scanner output ---"
    echo "$out" | sed 's/^/         /'
  fi
}

# expect_contains <case-name> <fixture-subdir> <needle>
expect_contains() {
  local name="$1" dir="$2" needle="$3" out
  out="$(bash "$SCANNER" "$dir" 2>&1)"
  if echo "$out" | grep -qF "$needle"; then
    PASS=$((PASS+1)); echo "  ok   - $name (আউটপুটে '$needle' আছে)"
  else
    FAIL=$((FAIL+1)); echo "  FAIL - $name: আউটপুটে '$needle' পাওয়া যায়নি"
  fi
}

# ---------------------------------------------------------------------------
# কেস ১: clean — কোনো duplicate নেই → exit 0
# ---------------------------------------------------------------------------
CLEAN="$WORK/clean"
mkdir -p "$CLEAN"
cat > "$CLEAN/001_clean.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.foo(p_id uuid, p_name text)
RETURNS jsonb AS $$
BEGIN
  RETURN jsonb_build_object('result', 'OK');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION public.bar(p_amount numeric)
RETURNS void AS $$
BEGIN
  NULL;
END;
$$ LANGUAGE plpgsql;
SQL

# ---------------------------------------------------------------------------
# কেস ২: একই ফাংশন একই signature-এ বারবার CREATE OR REPLACE হচ্ছে (স্বাভাবিক — duplicate না)
# ---------------------------------------------------------------------------
REPLACE_SAME="$WORK/replace_same"
mkdir -p "$REPLACE_SAME"
cat > "$REPLACE_SAME/001_first.sql" <<'SQL'
CREATE FUNCTION public.foo(p_id uuid, p_name text)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL
cat > "$REPLACE_SAME/002_override.sql" <<'SQL'
-- parameter নাম বদলেছে, কিন্তু টাইপ-ক্রম এক (uuid, text) — এটা override, নতুন overload না
CREATE OR REPLACE FUNCTION public.foo(p_user_id uuid, p_display_name text)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL

# ---------------------------------------------------------------------------
# কেস ৩: সত্যিকারের duplicate — একই নাম, ভিন্ন arity → exit 1, রিপোর্টে নাম থাকা উচিত
# ---------------------------------------------------------------------------
DUP="$WORK/dup"
mkdir -p "$DUP"
cat > "$DUP/001_v1.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.admin_do_thing(p_user_id uuid, p_flag boolean)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL
cat > "$DUP/002_v2.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.admin_do_thing(p_user_id uuid, p_flag boolean, p_role text DEFAULT 'USER')
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL

# ---------------------------------------------------------------------------
# কেস ৪: duplicate তৈরি হয়েছিল, কিন্তু পরের migration-এ পুরনোটা DROP হয়ে গেছে → এখন আর
# live duplicate না, exit 0 (কিন্তু ⚪ HISTORICAL সেকশনে দেখানো উচিত)
# ---------------------------------------------------------------------------
DROPPED="$WORK/dropped"
mkdir -p "$DROPPED"
cat > "$DROPPED/001_v1.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.request_withdrawal(p_amount numeric, p_method text)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL
cat > "$DROPPED/002_v2_new_overload.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.request_withdrawal(p_amount numeric, p_method text, p_client_id text DEFAULT NULL)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL
cat > "$DROPPED/003_drop_old.sql" <<'SQL'
DROP FUNCTION IF EXISTS public.request_withdrawal(numeric, text);
SQL

# ---------------------------------------------------------------------------
# কেস ৫: case-insensitive নাম-ম্যাচ (rule #5a) — Function/FUNCTION/function মিশিয়ে লিখলেও ধরা পড়া উচিত
# ---------------------------------------------------------------------------
CASE_INSENSITIVE="$WORK/case_insensitive"
mkdir -p "$CASE_INSENSITIVE"
cat > "$CASE_INSENSITIVE/001_v1.sql" <<'SQL'
create function public.Mixed_Case_Fn(p_x integer)
returns void as $$ begin null; end; $$ language plpgsql;
SQL
cat > "$CASE_INSENSITIVE/002_v2.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.mixed_case_fn(p_x integer, p_y integer)
RETURNS void AS $$ BEGIN NULL; END; $$ LANGUAGE plpgsql;
SQL

# ---------------------------------------------------------------------------
# কেস ৬ (২০২৬-০৯-২১, Step 12.12): ALLOWLIST_NAMES-এ থাকা ফাংশনের duplicate arity →
# exit 0 (STILL-LIVE গণ্য হবে না), কিন্তু "ALLOWLISTED" সেকশনে দেখানো উচিত
# ---------------------------------------------------------------------------
ALLOWLISTED="$WORK/allowlisted"
mkdir -p "$ALLOWLISTED"
cat > "$ALLOWLISTED/001_v1.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.request_wallet_deposit(p_amount numeric, p_gateway text)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL
cat > "$ALLOWLISTED/002_v2_new_overload.sql" <<'SQL'
CREATE OR REPLACE FUNCTION public.request_wallet_deposit(p_amount numeric, p_gateway text, p_expected_user_id uuid)
RETURNS jsonb AS $$ BEGIN RETURN '{}'::jsonb; END; $$ LANGUAGE plpgsql;
SQL

echo "== scan_duplicate_overloads.sh self-test =="
expect_exit     "clean migrations → exit 0"                         0 "$CLEAN"
expect_exit     "একই signature বারবার override → exit 0 (duplicate না)" 0 "$REPLACE_SAME"
expect_exit     "সত্যিকারের duplicate arity → exit 1"                 1 "$DUP"
expect_contains "duplicate রিপোর্টে ফাংশনের নাম আছে"                 "$DUP" "admin_do_thing"
expect_exit     "DROP করা পুরনো overload → exit 0 (আর live duplicate না)" 0 "$DROPPED"
expect_contains "DROP করা duplicate HISTORICAL সেকশনে দেখায়"         "$DROPPED" "HISTORICAL"
expect_exit     "case-insensitive নাম-ম্যাচেও duplicate ধরা পড়ে → exit 1" 1 "$CASE_INSENSITIVE"
expect_contains "case-insensitive duplicate রিপোর্টে (lowercase) নাম আছে" "$CASE_INSENSITIVE" "mixed_case_fn"
expect_exit     "ALLOWLIST_NAMES-এ থাকা duplicate → exit 0 (still-live গণ্য না)" 0 "$ALLOWLISTED"
expect_contains "ALLOWLIST duplicate ALLOWLISTED সেকশনে দেখায়"       "$ALLOWLISTED" "ALLOWLISTED"

echo
echo "মোট: $PASS pass, $FAIL fail"
[ "$FAIL" -eq 0 ]
