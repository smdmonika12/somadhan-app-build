#!/usr/bin/env bash
# scripts/setup_test_env.sh
#
# ============================================================================
# উদ্দেশ্য
# ============================================================================
# CI_TEST_SUITE_PROGRESS.md-এ বারবার একই environment-setup ধাপগুলো (Postgres+
# pgTAP install, role তৈরি, extension enable, auth/realtime stub, migration
# apply-এর order, কোন migration-গুলো "known/expected" ভাবে fail করে ইত্যাদি)
# প্রতি সেশনে হাতে-লিখে আবার আবিষ্কার করতে হচ্ছিল — এটাই সময় নষ্টের সবচেয়ে
# বড় কারণ ছিল, real verification-এর কারণে না।
#
# এই স্ক্রিপ্ট সেই setup-টুকু (শুধু setup — pgTAP test চালানো না, সেটা এখনো
# scripts/run_tests.sh-এর কাজ) একটা জায়গায় বেঁধে রাখে, যাতে:
#   ১. পরের প্রতিটা সেশন শুধু `bash scripts/setup_test_env.sh` চালাবে —
#      Postgres install করা লাগবে, না লাগবে, role আগে থেকে আছে কিনা —
#      এসব প্রতিবার নতুন করে ভাবা লাগবে না।
#   ২. কিন্তু নির্ভরযোগ্যতা কমে না: প্রতিটা ধাপ IF NOT EXISTS / idempotent
#      চেক দিয়ে লেখা (তাই দ্বিতীয়বার চালালেও নিরাপদ, কিছু "মেরে ফেলে" না,
#      কিছু "silently skip" ও করে না — প্রতিটা ধাপের ফলাফল প্রিন্ট হয়)।
#   ৩. migration apply করার পর প্রতিটা migration-এর pass/fail *সত্যিই* চেক
#      করা হয় এবং যেগুলো "known/expected" (নিচের KNOWN_FAILING_MIGRATIONS
#      তালিকা, progress doc থেকে) তার সাথে মিলিয়ে দেখানো হয় — কোনো ফলাফল
#      অন্ধভাবে "ঠিক আছে" ধরে নেওয়া হয় না। নতুন/অপ্রত্যাশিত fail এলে সেটা
#      স্পষ্টভাবে আলাদা করে ⚠️ দিয়ে দেখানো হয় ও স্ক্রিপ্ট non-zero exit করে —
#      অর্থাৎ "same জিনিস re-scan না করা" মানে idempotent/scripted করা, কিন্তু
#      "ফলাফল না দেখা" না।
#
# ============================================================================
# ⚠️ ২০২৬-০৯-২০ (Step 8→9 real-run সেশন) আপডেট
# ============================================================================
# ওই সেশনে আসল DB-তে চালিয়ে দুটো real bug ধরা পড়েছিল এই স্ক্রিপ্টের আগের
# ভার্সনে, নিচে দুটোই ফিক্স করা হয়েছে:
#   1. pgcrypto **public schema-তে বসানো যাবে না** — supabase convention অনুযায়ী
#      schema stub `extensions` schema-তে বসায়, আর `admin_credentials_update()`
#      RPC `extensions.gen_salt()` কল করে। public-এ থাকলে সেই stub লাইন no-op
#      হয়ে যায় আর `10_..._part1.sql` hard ERROR দেয়।
#   2. `realtime.topic()` stub না থাকলে ৪টা realtime-scoping migration apply
#      হয় না (broadcast trigger তৈরি হয় না) — suite তাও pass করে, কিন্তু stub
#      থাকলে production-এর আচরণের কাছাকাছি থাকে।
# একই সেশনে `scripts/local_pgtap_bootstrap.sh` নামে একটা সমতুল্য স্ক্রিপ্টও
# তৈরি হয়েছিল স্বাধীনভাবে (এই একই bug দুটো সেখানেও ফিক্স করা)। দুটো স্ক্রিপ্টই
# রাখা হলো — `local_pgtap_bootstrap.sh` হলো ন্যূনতম/প্রমাণিত সংস্করণ যেটা
# CI_TEST_SUITE_PROGRESS.md-এর ৯৯৭-ok baseline বানিয়েছে; এই স্ক্রিপ্ট
# (`setup_test_env.sh`) তার উপরে known-vs-new migration-failure তুলনা আর
# fresh-DB/role/extension idempotency-চেক যোগ করে (নিচে দেখো)। যেকোনো একটা
# ব্যবহার করলেই চলবে — দুটোর ফলাফল একই হওয়া উচিত।
#
# ============================================================================
# ব্যবহার
# ============================================================================
#   bash scripts/setup_test_env.sh [DB_NAME]
#
#   DB_NAME না দিলে ডিফল্ট: ci_test_verify
#   প্রতিবার fresh/throwaway DB (DROP + CREATE) — পুরনো সেশনের leftover data
#   এর কারণে ভুল ফলাফল আসার ঝুঁকি এড়াতে (আগের সেশনগুলোর শেখা শিক্ষা)।
#
# সফল হলে DB প্রস্তুত থাকবে schema stub + সব migration apply হয়ে; তারপর
#   PGDATABASE=<DB_NAME> bash scripts/run_tests.sh
# দিয়ে আসল pgTAP test suite চালানো যাবে (run_tests.sh নিজে থেকেই আবার
# schema stub apply করবে — এটা idempotent, ক্ষতি নেই, তাই এখানে ইচ্ছাকৃতভাবে
# ছোঁয়া হয়নি)।
#
# এই স্ক্রিপ্ট নিজে কোনো pgTAP assertion চালায় না — শুধু DB প্রস্তুত করে।
# ============================================================================

set -uo pipefail

DB_NAME="${1:-ci_test_verify}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

log()  { printf '\n== %s ==\n' "$1"; }
ok()   { printf '  ✅ %s\n' "$1"; }
skip() { printf '  ⏭  %s (আগে থেকেই ঠিক আছে — skip)\n' "$1"; }
warn() { printf '  ⚠️  %s\n' "$1"; }
die()  { printf '  ‼️  %s\n' "$1"; exit 1; }

# ----------------------------------------------------------------------------
# ধাপ ১ — Postgres 16 + pgTAP প্যাকেজ (আগে থেকে থাকলে skip, না থাকলে install)
# ----------------------------------------------------------------------------
log "ধাপ ১: Postgres + pgTAP প্যাকেজ"
if command -v psql >/dev/null 2>&1 && dpkg -s postgresql-16-pgtap >/dev/null 2>&1; then
  skip "postgresql + postgresql-16-pgtap ইতিমধ্যে ইনস্টল করা"
else
  echo "  ইনস্টল করার চেষ্টা করছি (network বন্ধ থাকলে এখানেই আটকে যাবে,"
  echo "  সেটাই আগের সেশনগুলোর সবচেয়ে সাধারণ ব্লকার ছিল)..."
  apt-get update -y >/tmp/apt_update.log 2>&1
  UPDATE_RC=$?
  if [ $UPDATE_RC -ne 0 ]; then
    # কোনো একটা অপ্রাসঙ্গিক third-party repo (যেমন nodesource) ব্লক থাকলেও
    # apt-get update নন-জিরো exit দেয়, যদিও Ubuntu-র নিজের repo (যেখান থেকে
    # postgresql আসে) ঠিকই fetch হয়ে যায়। তাই সত্যিই ব্লকার কিনা সেটা
    # postgresql প্যাকেজ আসলে resolve হয় কিনা তা দিয়ে যাচাই করি, পুরো
    # exit code দিয়ে না — নাহলে অপ্রাসঙ্গিক repo-র কারণে মিথ্যা "network বন্ধ"
    # সিদ্ধান্তে চলে যেতাম।
    warn "apt-get update নন-জিরো exit দিয়েছে — লগ: /tmp/apt_update.log (যাচাই করছি আসল দরকারি প্যাকেজ পাওয়া যায় কিনা)"
    if ! apt-cache show postgresql-16-pgtap >/dev/null 2>&1; then
      die "postgresql-16-pgtap প্যাকেজ metadata resolve হচ্ছে না — Ubuntu archive পর্যন্তও network পৌঁছাচ্ছে না সম্ভবত। এই সেশনে static verification-এ ফিরে যেতে হবে।"
    fi
    ok "দরকারি প্যাকেজ metadata আসলে পাওয়া যাচ্ছে (অপ্রাসঙ্গিক repo-র সমস্যা ছিল, এগিয়ে যাচ্ছি)"
  fi
  if ! apt-get install -y postgresql postgresql-contrib postgresql-16-pgtap >/tmp/apt_install.log 2>&1; then
    die "apt-get install ব্যর্থ — লগ: /tmp/apt_install.log"
  fi
  ok "install সম্পন্ন"
fi

# ----------------------------------------------------------------------------
# ধাপ ২ — সার্ভিস চালু আছে কিনা
# ----------------------------------------------------------------------------
log "ধাপ ২: PostgreSQL সার্ভিস"
if pg_isready -h localhost >/dev/null 2>&1; then
  skip "সার্ভিস আগে থেকেই চালু"
else
  service postgresql start >/tmp/pg_start.log 2>&1
  sleep 2
  pg_isready -h localhost >/dev/null 2>&1 || die "সার্ভিস চালু করা গেল না — লগ: /tmp/pg_start.log"
  ok "সার্ভিস চালু হয়েছে"
fi

# ----------------------------------------------------------------------------
# ধাপ ৩ — postgres ইউজারের password (peer auth থেকে -h localhost কানেকশনের জন্য লাগে)
# ----------------------------------------------------------------------------
log "ধাপ ৩: postgres user password"
# sudo সব container-এ থাকে না (এই sandbox-এ root হিসেবেই চলে) — থাকলে sudo,
# না থাকলে su দিয়ে চেষ্টা করি; দুটোই না থাকলে/ব্যর্থ হলে স্পষ্ট মেসেজ দিয়ে থামি।
if command -v sudo >/dev/null 2>&1; then
  sudo -u postgres psql -X -q -c "ALTER USER postgres PASSWORD 'postgres';" \
    || die "password সেট করা গেল না (sudo দিয়ে)"
else
  su postgres -c "psql -X -q -c \"ALTER USER postgres PASSWORD 'postgres';\"" \
    || die "password সেট করা গেল না (su দিয়ে — sudo এই container-এ নেই)"
fi
export PGPASSWORD=postgres
ok "সেট করা হলো (idempotent — বারবার চালালেও ক্ষতি নেই)"

PSQL_SUPER="psql -h localhost -U postgres -X -q -v ON_ERROR_STOP=1"

# ----------------------------------------------------------------------------
# ধাপ ৪ — role তৈরি (আগে থেকে থাকলে skip — real Supabase-এর role এগুলো,
# plain Ubuntu postgres প্যাকেজে থাকে না)
# ----------------------------------------------------------------------------
log 'ধাপ ৪: role — "-", anon, authenticated, service_role, supabase_admin'
for role in '"-"' anon authenticated service_role supabase_admin; do
  bare_name=$(echo "$role" | tr -d '"')
  exists=$($PSQL_SUPER -tAc "SELECT 1 FROM pg_roles WHERE rolname = '${bare_name}'")
  if [ "$exists" = "1" ]; then
    skip "role ${role}"
  else
    $PSQL_SUPER -c "CREATE ROLE ${role};" || die "role ${role} তৈরি ব্যর্থ"
    ok "role ${role} তৈরি হলো"
  fi
done

# ----------------------------------------------------------------------------
# ধাপ ৫ — fresh throwaway DB (প্রতিবার DROP+CREATE — পুরনো সেশনের leftover
# ডেটার কারণে false result এড়াতে; role/extension DB-independent তাই সেগুলো
# উপরেই একবার করা হয়েছে)
# ----------------------------------------------------------------------------
log "ধাপ ৫: ডাটাবেজ ${DB_NAME} (fresh)"
$PSQL_SUPER -c "DROP DATABASE IF EXISTS ${DB_NAME};" || die "পুরনো DB drop ব্যর্থ"
$PSQL_SUPER -c "CREATE DATABASE ${DB_NAME};" || die "নতুন DB তৈরি ব্যর্থ"
ok "${DB_NAME} ফ্রেশ তৈরি হলো"

PSQL="psql -h localhost -U postgres -X -q -v ON_ERROR_STOP=1 -d ${DB_NAME}"

# ----------------------------------------------------------------------------
# ধাপ ৬ — extension (pgcrypto: gen_random_uuid(); pgtap: plan()/ok()/...)
# ----------------------------------------------------------------------------
log "ধাপ ৬: extension"
# ⚠️ pgcrypto **public-এ না** — supabase convention অনুযায়ী `extensions` schema-তে
# (schema stub নিজেই এভাবে বসায়; `admin_credentials_update()` RPC
# `extensions.gen_salt()` কল করে — public-এ থাকলে ওই কল fail করে বা stub no-op
# হয়ে যায়, ধরা পড়ে অনেক পরে গিয়ে)।
$PSQL -c "CREATE SCHEMA IF NOT EXISTS extensions;" || die "extensions schema তৈরি ব্যর্থ"
$PSQL -c "CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;" || die "pgcrypto extension ব্যর্থ"
$PSQL -c "CREATE EXTENSION IF NOT EXISTS pgtap;" || die "pgtap extension ব্যর্থ"
ok "pgcrypto (extensions schema-তে) + pgtap নিশ্চিত (⚠️ pgtap বাদ পড়লে সব plan() কল ভেঙে পুরো রান অর্থহীন হয়ে যায়; pgcrypto public-এ গেলে admin_credentials_update() silently ভাঙে — দুটোই আগে ধরা পড়া real bug)"

# ----------------------------------------------------------------------------
# ধাপ ৭ — auth/realtime minimal stub (real Supabase image-এ built-in থাকে,
# plain Ubuntu postgres-এ নেই — rule #6 অনুযায়ী inferred/TEMPORARY)
# ----------------------------------------------------------------------------
log "ধাপ ৭: auth.uid() + realtime stub"
$PSQL <<'SQL' || die "auth/realtime stub apply ব্যর্থ"
CREATE SCHEMA IF NOT EXISTS auth;
CREATE OR REPLACE FUNCTION auth.uid() RETURNS uuid AS $fn$
  SELECT nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$fn$ LANGUAGE sql STABLE;

CREATE SCHEMA IF NOT EXISTS realtime;
CREATE TABLE IF NOT EXISTS realtime.messages (
  id bigserial primary key, payload jsonb, event text,
  topic text, private boolean, extension text, inserted_at timestamptz default now()
);

-- broadcast_changes: anyelement দিয়ে যাতে সব টেবিলের row-type (users, escrows,
-- bids, ...) এক ফাংশনেই মেলে। এটা ছাড়া প্রতিটা broadcast trigger fire হওয়া
-- মাত্র "function does not exist" দিয়ে সব INSERT/UPDATE ভেঙে যেত।
-- ⚠️ Step 13.1 ফিক্স (আগে এখানে ছিল): এই ফাংশন সম্পূর্ণ no-op ছিল (শুধু
-- `BEGIN RETURN; END;`) — realtime.messages টেবিলে কিছুই লেখা হতো না। তার মানে
-- এই টেবিল থাকা সত্ত্বেও কোনো pgTAP টেস্ট কখনো assert করতে পারত না যে কোনো
-- broadcast trigger আসলেই fire হয়েছে কিনা (row-count সবসময় ০)। এখন
-- realtime.messages-এ topic/event/payload সহ একটা row insert করে — বাস্তব
-- Supabase realtime.broadcast_changes()-এর documented আচরণের (broadcast
-- payload realtime.messages-এ যায়, সেখান থেকেই Realtime সার্ভার ডেলিভার করে)
-- একটা ন্যূনতম টেস্ট-উপযোগী approximation, sandbox-এ real supabase/postgres
-- image pull করা যায় না বলে (rule #6-এর মতোই inferred/TEMPORARY — real image-এ
-- আচরণ সামান্য ভিন্ন হতে পারে, GitHub Actions real-run-এই চূড়ান্ত যাচাই হবে)।
-- সাহায্যকারী query function (supabase/tests/13_trigger_helpers.sql-এ) এই
-- টেবিল থেকেই পড়ে।
CREATE OR REPLACE FUNCTION realtime.broadcast_changes(
  topic_name text, event_name text, operation text,
  table_name name, table_schema name, new anyelement, old anyelement
) RETURNS void AS $$
BEGIN
  INSERT INTO realtime.messages (topic, event, payload, private, extension)
  VALUES (
    topic_name,
    event_name,
    jsonb_build_object(
      'operation', operation,
      'table', table_name,
      'schema', table_schema,
      'record', to_jsonb(new),
      'old_record', to_jsonb(old)
    ),
    true,
    'broadcast'
  );
END;
$$ LANGUAGE plpgsql;

-- realtime.topic() না থাকলে ৪টা realtime-scoping migration apply হয় না
-- (broadcast trigger/policy তৈরি হতে গিয়ে "function does not exist")।
CREATE OR REPLACE FUNCTION realtime.topic() RETURNS text AS $fn2$
  SELECT nullif(current_setting('realtime.topic', true), '')::text;
$fn2$ LANGUAGE sql STABLE;
SQL
ok "auth/realtime stub প্রস্তুত (broadcast_changes + topic())"

# publication (idempotent — না থাকলেই তৈরি)
pub_exists=$($PSQL -tAc "SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime'")
if [ "$pub_exists" = "1" ]; then
  skip "publication supabase_realtime"
else
  $PSQL -c "CREATE PUBLICATION supabase_realtime FOR TABLE public.users;" 2>/dev/null \
    || warn "publication তৈরি এই মুহূর্তে সম্ভব হয়নি (public.users এখনো নেই — schema stub-এর পরে ঠিক হয়ে যাবে, নিচে দেখুন)"
fi

# ----------------------------------------------------------------------------
# ধাপ ৮ — schema stub apply (supabase/tests/*_schema_stub.sql, sorted)
# ----------------------------------------------------------------------------
log "ধাপ ৮: schema stub"
shopt -s nullglob
stub_files=(supabase/tests/*_schema_stub.sql)
if [ ${#stub_files[@]} -eq 0 ]; then
  die "কোনো *_schema_stub.sql পাওয়া যায়নি — supabase/tests/ ঠিক জায়গায় আছে তো?"
fi
for f in $(printf '%s\n' "${stub_files[@]}" | sort); do
  echo "  >> $f"
  $PSQL -f "$f" || die "schema stub ব্যর্থ: $f"
done
ok "সব schema stub apply হলো (${#stub_files[@]} ফাইল)"

# publication-এ users টেবিল এখন নিশ্চিতভাবে আছে — আগের ধাপে বাদ পড়ে থাকলে এখন চেষ্টা
pub_exists=$($PSQL -tAc "SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime'")
[ "$pub_exists" = "1" ] || $PSQL -c "CREATE PUBLICATION supabase_realtime FOR TABLE public.users;" \
  || die "publication supabase_realtime তৈরি ব্যর্থ (schema stub-এর পরেও)"

# ----------------------------------------------------------------------------
# ধাপ ৯ — CI pre-migration fixups (আসল DB-র state মিমিক করা constraint ইত্যাদি)
# ----------------------------------------------------------------------------
log "ধাপ ৯: scripts/ci_pre_migration_fixups.sql"
if [ -f scripts/ci_pre_migration_fixups.sql ]; then
  $PSQL -f scripts/ci_pre_migration_fixups.sql || die "ci_pre_migration_fixups.sql ব্যর্থ"
  ok "apply হলো"
else
  warn "scripts/ci_pre_migration_fixups.sql পাওয়া যায়নি — skip (থাকলে ভালো হতো)"
fi

# ----------------------------------------------------------------------------
# ধাপ ১০ — migration apply (sorted, প্রতিটার ফলাফল ট্র্যাক করে; কোনোটা
# ব্যর্থ হলেও বাকিগুলো চলতে থাকে — শেষে known-vs-unexpected তুলনা)
# ----------------------------------------------------------------------------
log "ধাপ ১০: migration apply (supabase/migrations/*.sql, sorted)"

# progress doc অনুযায়ী (Step 8→9 real-run, ২০২৬-০৯-২০) "known/expected" fail করা
# migration — সবই environment-gap-এর কারণে (real Supabase image-এ এগুলো সমস্যা
# না)। realtime.topic() stub যোগ হওয়ার পর ৩টা realtime-scoping migration
# (messages_solver_thread_isolation, step1_notifications, step4_bids) এখন
# আসলেই pass করে — তাই আগের তালিকা থেকে বাদ দেওয়া হলো (১৩টা fail → এখন ৭টা,
# যার মধ্যে step28 নিচে আলাদাভাবে handle হয় বলে এই তালিকায় নেই — কার্যত ৬টা
# এখানে ধরা পড়বে):
#   add_display_uid_generator.sql — schema stub-এ created_at কলাম নেই (পুরনো, ডকুমেন্টেড)
#   fix_problems_select_bids_rls_recursion.sql — যে policy নেই তার উপর DROP
#   problems_select_allow_ended_bid_solver.sql — যে policy নেই তার উপর ALTER
#   step23_bids_select_open_public_visibility.sql — একই কারণ
#   realtime_scoping_step3_messages_broadcast.sql — alphabetical-order-এ
#     messages_solver_thread_isolation আগে চলে একই policy বানিয়ে ফেলে (real
#     Supabase-এ timestamp-ক্রমে চলে বলে সমস্যা হয় না — নিরীহ)
#   step20_enable_realtime_users_problems_bids.sql — "already member of publication" (নিরীহ)
# ⚠️ নতুন migration যোগ হলে বা কোনোটা আসলেই ফিক্স হয়ে গেলে এই তালিকা হাতে
# আপডেট করতে হবে — script নিজে থেকে এই তালিকা অনুমান করে না।
KNOWN_FAILING_MIGRATIONS=(
  "add_display_uid_generator.sql"
  "fix_problems_select_bids_rls_recursion.sql"
  "problems_select_allow_ended_bid_solver.sql"
  "step23_bids_select_open_public_visibility.sql"
  "realtime_scoping_step3_messages_broadcast.sql"
  "step20_enable_realtime_users_problems_bids.sql"
)

is_known_failure() {
  local name="$1"
  for k in "${KNOWN_FAILING_MIGRATIONS[@]}"; do
    [ "$name" = "$k" ] && return 0
  done
  return 1
}

APPLIED_OK=()
FAILED_KNOWN=()
FAILED_NEW=()

for f in $(ls supabase/migrations/*.sql | sort); do
  base=$(basename "$f")

  if [ "$base" = "step28_expire_stale_instant_jobs_cron.sql" ]; then
    # pg_cron এই environment-এ নেই — regex দিয়ে শুধু
    # "create or replace function ... $function$;" অংশটুকু বের করে apply করি
    # (python３ ব্যবহার — sed-এর চেয়ে multi-line block-এর জন্য নির্ভরযোগ্য;
    # scripts/local_pgtap_bootstrap.sh-এও একই পদ্ধতি প্রমাণিত)
    tmp=$(mktemp)
    if python3 - "$f" "$tmp" <<'PY'
import re, sys
src = open(sys.argv[1]).read()
m = re.search(r'(create or replace function.*?\$function\$\s*;)', src, re.S | re.I)
assert m, "function body not found"
open(sys.argv[2], 'w').write(
    m.group(1) +
    "\nrevoke all on function public.expire_stale_instant_jobs() from public, anon, authenticated;\n")
PY
    then
      if $PSQL -v ON_ERROR_STOP=1 -f "$tmp" >/tmp/mig_out.log 2>&1; then
        APPLIED_OK+=("$base (pg_cron অংশ বাদ দিয়ে, শুধু function)")
      else
        FAILED_NEW+=("$base (function-only ভার্সনও ব্যর্থ — /tmp/mig_out.log দেখো)")
      fi
    else
      FAILED_NEW+=("$base (python3 regex দিয়ে function body বের করা যায়নি — ফাইল ফরম্যাট বদলেছে কিনা দেখো)")
    fi
    rm -f "$tmp"
    continue
  fi

  if $PSQL -v ON_ERROR_STOP=1 -f "$f" >/tmp/mig_out.log 2>&1; then
    APPLIED_OK+=("$base")
  else
    if is_known_failure "$base"; then
      FAILED_KNOWN+=("$base")
    else
      FAILED_NEW+=("$base")
      echo "  ⚠️ নতুন/অপ্রত্যাশিত migration failure: $base"
      tail -n 5 /tmp/mig_out.log | sed 's/^/      /'
    fi
  fi
done

echo
echo "  --- migration ফলাফল সারাংশ ---"
echo "  ✅ সফল:            ${#APPLIED_OK[@]}"
echo "  ⏭  known-expected fail: ${#FAILED_KNOWN[@]}"
echo "  ⚠️  NEW/unexpected fail: ${#FAILED_NEW[@]}"
if [ ${#FAILED_NEW[@]} -gt 0 ]; then
  printf '      - %s\n' "${FAILED_NEW[@]}"
fi

# schema stub আবার apply (idempotent — কিছু migration নতুন কলাম যোগ করতে পারে
# যেটা পুরনো stub-কলামের সাথে conflict করতে পারত, বা stub-এর কিছু কলাম migration-এর
# আগে দরকার ছিল — run_tests.sh নিজেও এটা আবার করবে, DB-কে সবসময় stub-consistent
# রাখতে এখানে একবার নিশ্চিত করে রাখা হলো)
log "ধাপ ১১: schema stub আবার apply (migration-পরবর্তী consistency)"
for f in $(printf '%s\n' "${stub_files[@]}" | sort); do
  $PSQL -f "$f" >/tmp/stub_reapply.log 2>&1 || die "schema stub re-apply ব্যর্থ: $f — /tmp/stub_reapply.log দেখো"
done
ok "নিশ্চিত করা হলো"

echo
echo "================================================================"
echo " DB '${DB_NAME}' প্রস্তুত। এখন টেস্ট চালাতে:"
echo "   PGPASSWORD=postgres PGDATABASE=${DB_NAME} psql -h localhost -U postgres -c 'select 1;'  # sanity check"
echo "   PGDATABASE=${DB_NAME} bash scripts/run_tests.sh"
echo "================================================================"

if [ ${#FAILED_NEW[@]} -gt 0 ]; then
  echo
  echo "‼️ ${#FAILED_NEW[@]}টা migration অপ্রত্যাশিতভাবে ব্যর্থ হয়েছে — এগুলো আগে"
  echo "   ঠিক করে/তদন্ত করে, দরকার হলে KNOWN_FAILING_MIGRATIONS তালিকায় (উপরে,"
  echo "   এই স্ক্রিপ্টেই) যোগ করে CI_TEST_SUITE_PROGRESS.md-এ নোট লিখে তারপর"
  echo "   run_tests.sh চালাও — না হলে test-এর ফলাফল ভুল ব্যাখ্যা হতে পারে।"
  exit 1
fi

exit 0
