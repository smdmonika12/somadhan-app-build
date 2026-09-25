-- ============================================================================
-- Step 12.10 — release_escrow live-check (READ-ONLY, শুধু SELECT)
-- Supabase প্রজেক্ট `somadhan` (mghvvpndkxnscwryfkib) — ২০২৬-০৯-২১-এ Supabase MCP দিয়ে চালানো হয়েছিল;
-- একই কুয়েরি নিজেও SQL editor-এ চালিয়ে মিলিয়ে নেওয়া যাবে। কোনো state বদলায় না; `release_escrow` কল করা হয় না।
-- ============================================================================

-- Q1 — live release_escrow: signature, security definer, search_path, দৈর্ঘ্য, md5, guard আছে কিনা
select p.oid::regprocedure::text as signature, p.prosecdef as security_definer, p.proconfig,
       length(p.prosrc) as src_len, md5(p.prosrc) as src_md5,
       (p.prosrc ilike '%is_disputed%') as mentions_is_disputed,
       (p.prosrc ilike '%PROBLEM_DISPUTED%') as has_guard,
       (p.prosrc ilike '%target_id, role, "timestamp")%') as notif_insert_has_role
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public' and p.proname = 'release_escrow';
-- ফল (২০২৬-০৯-২১): release_escrow(text), secdef=true, {search_path=public}, len=2909,
--   md5=443500c96c61b79e528606841f221593, mentions_is_disputed=false, has_guard=false, notif_insert_has_role=true
--   (step36 migration-এর বডি: len=3513, md5=1ee1295b8b809caaf995dfebc241aeeb, notif_insert_has_role=false → live ≠ step36)

-- Q2 — কে release_escrow কল করে (public, live), `problems.is_disputed` কলাম, `is_admin` সিগনেচার, grants
select 'caller' as kind, p.oid::regprocedure::text as name, (p.prosrc ilike '%is_admin(%')::text as a, (p.prosrc ilike '%is_disputed%')::text as b
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public' and p.prosrc ilike '%release_escrow(%' and p.proname <> 'release_escrow'
union all
select 'column', (table_name || '.' || column_name)::text, (data_type || ' nullable=' || is_nullable)::text, ('default=' || coalesce(column_default,'-'))::text
from information_schema.columns where table_schema='public' and table_name='problems' and column_name in ('is_disputed','dispute_status')
union all
select 'is_admin', p.oid::regprocedure::text, ('secdef=' || p.prosecdef::text)::text, pg_get_function_result(p.oid)::text
from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname='public' and p.proname='is_admin'
union all
select 'grant', (grantee || ' ' || privilege_type)::text, routine_name::text, ''::text
from information_schema.routine_privileges where routine_schema='public' and routine_name='release_escrow';
-- ফল: callers = resolve_dispute(text,text,text,numeric) / admin_reconcile_escrow_states(boolean) /
--   admin_update_direct_contract_status(text,text,text) — তিনটাই is_admin() চেক করে, is_disputed ছোঁয় না;
--   problems.is_disputed = boolean NOT NULL default false; is_admin(uuid) SECURITY DEFINER → boolean;
--   grants: postgres, anon, authenticated, service_role — সবার EXECUTE।

-- Q3 — auth.uid() NULL হলে release_escrow-এর auth-চেক-শর্ত কী দাঁড়ায় (ফাংশন কল না করে, শুধু বুলিয়ান এক্সপ্রেশন)
select public.is_admin(null::uuid) as is_admin_of_null,
       (not (null::uuid = gen_random_uuid() or public.is_admin(null::uuid))) as auth_guard_condition_when_uid_null;
-- ফল: is_admin_of_null = false; auth_guard_condition_when_uid_null = NULL  → `if NULL then raise` raise করে না
--   → anon (JWT ছাড়া) কলার এই চেক পাশ করে (PROPOSED_step12_10-এর ব্লক B এটা বন্ধ করে)।

-- Q4 — একই NULL-প্রবণ প্যাটার্ন আর কোন ফাংশনে (শুধু এই একটাই regex-রূপ খোঁজে — সম্পূর্ণ তালিকা নয়, candidate মাত্র)
select p.proname, p.oid::regprocedure::text as sig,
       (p.prosrc ~* 'auth\.uid\(\)\s+is\s+null') as has_explicit_null_check,
       has_function_privilege('anon', p.oid, 'EXECUTE') as anon_can_execute
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='public' and p.prokind='f' and p.prosecdef
  and p.prosrc ~* 'not\s*\(\s*auth\.uid\(\)\s*=[^;]*or\s+public\.is_admin\(auth\.uid\(\)\)\s*\)'
order by has_explicit_null_check, p.proname;
-- ফল: deposit_money_via_gateway(uuid,numeric,text,text,text,text) [anon EXECUTE = false],
--     refund_escrow_once(text,text,numeric) [anon EXECUTE = true ⚠️], release_escrow(text) [anon EXECUTE = true ⚠️]
--   তিনটাতেই explicit `auth.uid() is null` চেক নেই।

-- Q5 — `is_disputed` কে সেট/ক্লিয়ার করে (guard-এর সাথে ঠোকাঠুকি আছে কিনা দেখতে)
select p.oid::regprocedure::text as sig,
  (select string_agg(distinct m[1], ' | ') from regexp_matches(p.prosrc, '(is_disputed\s*=\s*[a-z_]+)', 'gi') as m) as assignments,
  (select string_agg(distinct m[1], ' | ') from regexp_matches(p.prosrc, '(escrows\s+set\s+status\s*=\s*''[A-Z_]+'')', 'gi') as m) as escrow_status_sets,
  (p.prosrc ilike '%release_escrow(%') as calls_release
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='public' and p.prosrc ilike '%is_disputed%'
order by 1;
-- ফল: resolve_dispute_split → `is_disputed = true` (ঐতিহাসিক মার্কার) + escrow সরাসরি RELEASED (terminal) —
--   পরের release_escrow `ALREADY_TERMINAL` পায়, guard-এ পৌঁছায় না। বাকিরা (`settle_dispute`, `withdraw_dispute`,
--   `cancel_job_release_request`, `accept_bid`, `solver_cancel_job`) `false` সেট করে; `raise_dispute`/
--   `admin_manually_flag_dispute` `true` সেট করে। কোনোটাই release_escrow কল করে না।

-- Q6 — cron/অন্য schema থেকে release_escrow কল আছে কিনা (cron.job সবসময় দৃশ্যমান না-ও হতে পারে)
select 'cron' as kind, jobname::text as name, left(command, 200) as detail from cron.job where command ilike '%release_escrow%'
union all
select 'non_public_fn', p.proname::text, 'refs release_escrow'
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname not in ('pg_catalog','information_schema','public') and p.prosrc ilike '%release_escrow%';
-- ফল: ০টা সারি (cron job নেই, public-এর বাইরে কোনো কলার নেই)।
