-- ============================================================================
-- Step 12.9 — Duplicate RPC-overload: READ-ONLY live check (কোনো ডেটা বদলায় না)
-- ============================================================================
-- Supabase SQL editor-এ (production project) চালাও। শুধু SELECT — কোনো টাকা/state/row
-- বদলায় না, কোনো RPC নিজে কল করা হচ্ছে না। ফল এই ধাপের সেশনে (পরের session-এর শুরুতে) paste
-- করলে Claude সিদ্ধান্ত নেবে কোন পুরনো overload DROP করা নিরাপদ।
--
-- পটভূমি: নিচের ৭টা ফাংশনের local migration ফাইলে (supabase/migrations/*.sql) দুটো করে
-- signature পাওয়া গেছে (scripts/scan_duplicate_overloads.sh দিয়ে স্ট্যাটিক স্ক্যানে, ২০২৬-০৯-২১):
--   admin_adjust_balance, admin_notify_user, admin_set_banned, admin_set_restricted,
--   create_notification, log_admin_action, submit_reputation_event
-- কিন্তু এটা কখনো সরাসরি লাইভ DB-তে (pg_proc) verify করা হয়নি — migration ফাইল আর লাইভ DB-র
-- state আলাদা হতে পারে (যেমন Step 12.8b/12.8c-এ `request_wallet_deposit` ও
-- `user_confirm_extra_amount`-এর নতুন overload Supabase MCP দিয়ে সরাসরি লাইভে apply হয়েছে,
-- কিন্তু matching migration ফাইল এখনো repo-তে commit হয়নি — তাই local scanner ওই দুটো
-- ফাংশনকে "duplicate" হিসেবে একদমই ধরছে না, যদিও লাইভে ওরাও এখন ২-signature অবস্থায়)।

-- Q1: উপরের ৭টা ফাংশনের সব signature (arg-count, arg-types, security definer কিনা)
select
  p.proname                                   as function_name,
  pg_get_function_identity_arguments(p.oid)   as identity_args,
  p.pronargs                                  as arg_count,
  p.prosecdef                                 as is_security_definer,
  p.oid::regprocedure::text                   as full_signature_oid
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = any(array[
    'admin_adjust_balance',
    'admin_notify_user',
    'admin_set_banned',
    'admin_set_restricted',
    'create_notification',
    'log_admin_action',
    'submit_reputation_event'
  ])
order by p.proname, p.pronargs;

-- Q2: প্রতিটা signature-এর উপর কার (role) কী grant (EXECUTE) আছে — কোনটা PUBLIC/anon-এর জন্যও
--     এখনো open আছে, কোনটা authenticated/service_role-এ টাইট করা আছে, সেটা বুঝতে (12.8b/12.8c-এ
--     নতুন overload-এ grants টাইট করে আগের pattern অনুসরণ করা হয়েছিল)।
select
  routine_name,
  specific_name,
  grantee,
  privilege_type
from information_schema.routine_privileges
where routine_schema = 'public'
  and routine_name = any(array[
    'admin_adjust_balance',
    'admin_notify_user',
    'admin_set_banned',
    'admin_set_restricted',
    'create_notification',
    'log_admin_action',
    'submit_reputation_event'
  ])
order by routine_name, grantee, privilege_type;

-- Q3 (প্রসঙ্গের জন্য, শুধু নিশ্চিত করা — এই দুটো Step 12.8b/12.8c-তে ইচ্ছাকৃতভাবে ২-signature
--     করা হয়েছে, বাগ না; এই ধাপের "fix করার তালিকায়" এই দুটো পড়বে না, শুধু নিশ্চিত করার জন্য):
select
  p.proname                                 as function_name,
  pg_get_function_identity_arguments(p.oid) as identity_args,
  p.pronargs                                as arg_count
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname = any(array['request_wallet_deposit', 'user_confirm_extra_amount'])
order by p.proname, p.pronargs;

-- ⚠️ এই ফাইলে ইচ্ছাকৃতভাবে কোনো state-বদলানো RPC কল বা "safe dry-run call উদাহরণ" নেই — ৭টার
-- কোনোটাই read-only/dry-run-নিরাপদ না (সবগুলোই admin action/notification/log/reputation-write),
-- তাই master prompt-এর Step 12.9 নিয়ম অনুযায়ী শুধু উপরের signature/grant-তালিকা কুয়েরিই দেওয়া হলো।
