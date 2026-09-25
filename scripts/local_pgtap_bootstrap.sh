#!/usr/bin/env bash
# scripts/local_pgtap_bootstrap.sh
#
# ⚠️ এটা CI-র অংশ নয় (`full-test.yml` এটা ব্যবহার করে না)। এটা শুধু একটা *লোকাল/স্যান্ডবক্স*
# হেল্পার: plain Ubuntu-র `postgresql-16` + `postgresql-16-pgtap` প্যাকেজের ওপর
# supabase/postgres ইমেজের মতো একটা মিনিমাল পরিবেশ বানিয়ে পুরো pgTAP suite চালানোর জন্য।
# (২০২৬-০৯-২০, Step 8→9 real-run সেশনে তৈরি — আগে প্রতিটা সেশনে এই কমান্ডগুলো হাতে
# আবিষ্কার করতে হতো; CI_TEST_SUITE_PROGRESS.md-এর "Environment setup" অনুচ্ছেদের
# কার্যকর রূপ।)
#
# ব্যবহার:
#   apt-get install -y postgresql postgresql-contrib postgresql-16-pgtap
#   service postgresql start
#   su postgres -c "psql -c \"ALTER USER postgres PASSWORD 'postgres';\""
#   ./scripts/local_pgtap_bootstrap.sh [dbname]        # ডিফল্ট: ci_verify
#   PGPASSWORD=postgres PGDATABASE=ci_verify ./scripts/run_tests.sh
#
# গুরুত্বপূর্ণ দুটো শিক্ষা (দুটোই এই স্ক্রিপ্টে ধরা আছে):
#   1. pgcrypto **public-এ ইনস্টল করা যাবে না**। `10_..._schema_stub.sql` ইচ্ছাকৃতভাবে
#      `CREATE EXTENSION pgcrypto WITH SCHEMA extensions` করে (Supabase কনভেনশন), আর
#      `admin_credentials_update()` RPC `extensions.gen_salt()` কল করে। আগে public-এ
#      বসিয়ে ফেললে stub-এর লাইনটা no-op হয় → `10_..._part1.sql` hard ERROR-এ ভাঙে।
#   2. `realtime.topic()` stub না থাকলে ৪টা realtime-scoping migration apply হয় না
#      (broadcast trigger/policy তৈরি হয় না)। suite দু'ভাবেই pass করে, কিন্তু stub
#      থাকলে production-এর কাছাকাছি হয়।
set -uo pipefail
cd "$(dirname "$0")/.."
export PGPASSWORD="${PGPASSWORD:-postgres}"
DB="${1:-ci_verify}"

echo "== (re)create database $DB =="
psql -h localhost -U postgres -X -q -d postgres -c "DROP DATABASE IF EXISTS $DB;"
psql -h localhost -U postgres -X -q -d postgres -c "CREATE DATABASE $DB;"
PD="psql -h localhost -U postgres -X -q -v ON_ERROR_STOP=1 -d $DB"

echo "== Supabase roles (cluster-wide) =="
psql -h localhost -U postgres -X -q -d postgres <<'SQL'
DO $$
DECLARE r text;
BEGIN
  FOREACH r IN ARRAY ARRAY['-','anon','authenticated','service_role','supabase_admin'] LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = r) THEN
      EXECUTE format('CREATE ROLE %I', r);
    END IF;
  END LOOP;
END $$;
SQL

echo "== pgtap + auth/realtime stubs =="
# ⚠️ এখানে pgcrypto ইনস্টল করা হচ্ছে না — উপরের ১ নং শিক্ষা দেখো।
$PD <<'SQL'
CREATE EXTENSION IF NOT EXISTS pgtap;

CREATE SCHEMA IF NOT EXISTS auth;
CREATE OR REPLACE FUNCTION auth.uid() RETURNS uuid AS $fn$
  SELECT nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$fn$ LANGUAGE sql STABLE;

CREATE SCHEMA IF NOT EXISTS realtime;
CREATE TABLE IF NOT EXISTS realtime.messages (
  id bigserial primary key, payload jsonb, event text, topic text,
  private boolean, extension text, inserted_at timestamptz default now()
);
-- Step 13.1 ফিক্স: আগে এখানে সম্পূর্ণ no-op ছিল (broadcast fire হয়েছে কিনা
-- pgTAP দিয়ে কখনো assert করা যেত না, realtime.messages সবসময় খালি থাকত)।
-- এখন সত্যিই realtime.messages-এ topic/event/payload insert করে (বিস্তারিত
-- মন্তব্য scripts/setup_test_env.sh-এর একই জায়গায়, দুটো ফাইল সবসময় sync রাখা
-- হয়)। supabase/tests/13_trigger_helpers.sql-এর helper function এই টেবিল
-- থেকেই পড়ে।
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
CREATE OR REPLACE FUNCTION realtime.topic() RETURNS text AS $$
  SELECT nullif(current_setting('realtime.topic', true), '')::text;
$$ LANGUAGE sql STABLE;
SQL

echo "== schema stubs =="
for f in $(ls supabase/tests/*_schema_stub.sql | sort); do
  echo ">> stub $f"
  $PD -f "$f" > /dev/null || { echo "‼️ stub failed: $f"; exit 1; }
done

echo "== publication + ci_pre_migration_fixups =="
$PD -c "CREATE PUBLICATION supabase_realtime FOR TABLE public.users;" > /dev/null 2>&1
$PD -f scripts/ci_pre_migration_fixups.sql > /dev/null || exit 1

echo "== migrations (ব্যর্থগুলো শুধু রিপোর্ট করা হয়, থামানো হয় না) =="
failed=0
for f in $(ls supabase/migrations/*.sql | sort); do
  if ! $PD -f "$f" > /tmp/mig.log 2>&1; then
    failed=$((failed + 1))
    echo "!! FAILED $f : $(grep -m1 ERROR /tmp/mig.log)"
  fi
done
echo "migration failures: $failed (প্রত্যাশিত ৭টা — বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এর Step 8→9 ADDENDUM-এ)"

echo "== step28: pg_cron বাদ দিয়ে শুধু expire_stale_instant_jobs() =="
python3 - <<'PY'
import re
src = open('supabase/migrations/step28_expire_stale_instant_jobs_cron.sql').read()
m = re.search(r'(create or replace function.*?\$function\$\s*;)', src, re.S | re.I)
assert m, "expire_stale_instant_jobs body not found"
open('/tmp/step28_fn.sql', 'w').write(
    m.group(1) +
    "\nrevoke all on function public.expire_stale_instant_jobs() from public, anon, authenticated;\n")
PY
$PD -f /tmp/step28_fn.sql > /dev/null || exit 1

echo "== DONE — এখন চালাও: PGPASSWORD=postgres PGDATABASE=$DB ./scripts/run_tests.sh =="
