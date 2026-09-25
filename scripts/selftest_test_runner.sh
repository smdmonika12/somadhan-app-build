#!/usr/bin/env bash
# scripts/selftest_test_runner.sh
#
# run_tests.sh আর generate_report.sh-এর নিজস্ব লজিক পরীক্ষা করে — কোনো Postgres/pgTAP লাগে না।
# একটা নকল `psql` (নিচে) বানিয়ে PATH-এ বসানো হয়, যেটা আগে থেকে ঠিক করা আউটপুট প্রিন্ট করে;
# তারপর দেখা হয় run_tests.sh সঠিক exit code দেয় কিনা।
#
# কেন দরকার: এই দুটো স্ক্রিপ্ট এতদিন "assertion fail করলে CI সবুজ" ধরনের নীরব ভুলে ভুগেছে
# (দেখুন run_tests.sh-এর হেডার) — scanner/runner নিজেই silently ভেঙে গেলে পুরো test suite
# মূল্যহীন হয়ে যায়, তাই runner-এর জন্যই একটা regression-guard।
#
# ⚠️ সীমা: নকল psql আসল psql-এর ফরম্যাটিং প্রমাণ করে না — শুধু "এই ফরম্যাটের আউটপুট পেলে
# আমাদের স্ক্রিপ্ট কী করে" যাচাই করে। আসল `psql -t -A` আউটপুট column 0-তে TAP লাইন দেয় কিনা,
# সেটা প্রথম আসল CI রানেই নিশ্চিত হবে (তাই দুই ফরম্যাটই সমর্থিত)।

set -uo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

mkdir -p "$WORK/bin" "$WORK/supabase/tests" "$WORK/scripts"
cp "$REPO/scripts/run_tests.sh" "$REPO/scripts/generate_report.sh" "$WORK/scripts/"
: > "$WORK/supabase/tests/00_helpers.sql"
: > "$WORK/supabase/tests/01_x_schema_stub.sql"
: > "$WORK/supabase/tests/09_case.sql"

# নকল psql: stub/helper ফাইলে চুপচাপ সফল; টেস্ট ফাইলে $FAKE_OUT প্রিন্ট করে $FAKE_RC দিয়ে বের হয়।
cat > "$WORK/bin/psql" <<'EOF'
#!/usr/bin/env bash
file=""
while [ $# -gt 0 ]; do
  case "$1" in -f) file="$2"; shift 2;; *) shift;; esac
done
case "$(basename "$file")" in
  00_helpers.sql|*_schema_stub.sql) exit 0;;
esac
printf '%b\n' "${FAKE_OUT:-}"
exit "${FAKE_RC:-0}"
EOF
chmod +x "$WORK/bin/psql"

PASS=0; FAIL=0
expect() { # expect <name> <expected-exit> <fake_out> [fake_rc]
  local name="$1" want="$2" out="$3" rc="${4:-0}" got
  ( cd "$WORK" && PATH="$WORK/bin:$PATH" FAKE_OUT="$out" FAKE_RC="$rc" bash scripts/run_tests.sh >/dev/null 2>&1 )
  got=$?
  if [ "$got" -eq "$want" ]; then PASS=$((PASS+1)); echo "  ok   - $name (exit $got)"
  else FAIL=$((FAIL+1)); echo "  FAIL - $name: expected exit $want, got $got"; fi
}

echo "== run_tests.sh =="
expect "unaligned: সব ok → 0"                               0 '1..2\nok 1 - a\nok 2 - b'
expect "aligned (leading space): সব ok → 0"                 0 '   ok    \n--------\n ok 1 - a\n ok 2 - b\n(2 rows)'
expect "unaligned: not ok → 1"                              1 'ok 1 - a\nnot ok 2 - b\n# Failed test 2'
expect "aligned (leading space): not ok → 1 (আগের বাগ)"     1 ' ok 1 - a\n not ok 2 - b'
expect "pgTAP plan mismatch → 1"                            1 'ok 1 - a\n# Looks like you planned 2 tests but ran 1'
expect "hard SQL ERROR → 1"                                 1 'psql:/x/09_case.sql:12: ERROR:  relation "foo" does not exist\nok 1 - a'
expect "psql non-zero exit → 1"                             1 'ok 1 - a' 3
expect "টেস্ট description-এ 'not ok' শব্দ থাকলে false-positive হয় না" 0 'ok 1 - "not ok" শব্দটা description-এর ভেতরে'

echo "== generate_report.sh =="
rep() { # rep <name> <input> <want-ok> <want-notok>
  local f="$WORK/in.txt" o
  printf '%b\n' "$2" > "$f"
  o="$(bash "$WORK/scripts/generate_report.sh" "$f")"
  if echo "$o" | grep -q "Passed | $3 |" && echo "$o" | grep -q "Failed | $4 |"; then
    PASS=$((PASS+1)); echo "  ok   - $1 (✅$3 ❌$4)"
  else FAIL=$((FAIL+1)); echo "  FAIL - $1: report=$(echo "$o" | grep -E 'Passed|Failed' | tr '\n' ' ')"; fi
}
rep "unaligned গণনা"            'ok 1 - a\nok 2 - b\nnot ok 3 - c' 2 1
rep "aligned (leading space) গণনা" ' ok 1 - a\n ok 2 - b\n not ok 3 - c\n not ok 4 - d' 2 2
rep "শুধু pass"                 'ok 1 - a\nok 2 - b' 2 0

echo
echo "self-test: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
