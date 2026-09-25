-- 12_coverage_gap_fill_part1.sql — Step 10 (Coverage audit + gap-fill)
--
-- ⚠️ এই সেশনে fresh case-insensitive স্ক্যান করা হয়েছে (master prompt-এর নিয়ম অনুযায়ী,
-- পুরনো তালিকা অন্ধভাবে বিশ্বাস করা হয়নি):
--   grep -rhoiP "create\s+(or\s+replace\s+)?function\s+public\.\w+" supabase/migrations/*.sql
--     | grep -oiP "public\.\K\w+" | tr 'A-Z' 'a-z' | sort -u   →  ১০৪টা function (master prompt-এর
--     সংখ্যার সাথে মিলে যাচ্ছে)
-- তারপর প্রতিটা নামের জন্য supabase/tests/*.sql (schema_stub বাদে) grep -liw করে যাচাই করা হলো
-- কোনগুলোর আদৌ কোনো test-file reference নেই। ফলাফল: ১৩টা।
--   → ১০টা হলো broadcast trigger-function (notify_*_broadcast, set_display_uid_on_insert) —
--     master prompt অনুযায়ী এগুলো সরাসরি কল হয় না (INSERT/UPDATE/DELETE-এ trigger হয়ে চলে),
--     আর এদের coverage স্পষ্টভাবে Step 13 (Database trigger coverage)-এর স্কোপ, Step 10-এর না।
--     তাই ইচ্ছাকৃতভাবে এই ফাইলে ধরা হয়নি (নিচের "কভারেজ সারাংশ" সেকশনে পুনরায় নোট করা আছে)।
--   → বাকি ৩টাই এই ফাইলের বিষয়:
--       admin_reconcile_user_balances  (step32_6 → step36 → step37; সর্বশেষ/effective সংজ্ঞা
--                                        step37_admin_reconcile_user_balances_v2_flat_balance.sql,
--                                        migration নাম alphabetically step36-এর পরে চলে)
--       admin_send_message_to_problem_chat  (recovered_admin_moderation.sql)
--       system_notify_48hour_auto_release   (recovered_instant_jobs.sql)
--
-- master prompt-এ Step 10-এ তালিকাভুক্ত আরও অনেক function-এর নাম (request_additional_charge,
-- user_confirm_extra_amount, user_reject_extra_amount, withdraw_dispute, solver_has_ended_bid,
-- user_delete_problem, expire_stale_instant_jobs, admin_get_dashboard_metrics,
-- admin_wipe_all_data, admin_soft_delete_user, admin_reconcile_escrow_states,
-- admin_repair_missing_refunds, admin_cleanup_duplicate_refunds, admin_update_withdrawal_trx_id,
-- admin_reset_free_job_quota, admin_bulk_reset_free_job_quota, admin_reset_miss_cycle,
-- owner_reset_orphaned_accepted_bid) — এই সেশনে প্রতিটা `grep -rliw <name> supabase/tests/*.sql`
-- দিয়ে যাচাই করা হয়েছে, সবগুলোরই ইতিমধ্যে test coverage আছে (06_job_release_escrow_part2.sql,
-- 08_disputes_part2.sql, 01/02_bidding_flow*.sql, 03/04_instant_jobs*.sql,
-- 10_admin_moderation_balance_part2.sql) — এই ফাইলগুলোর নাম-নম্বর master prompt-এর Step-নম্বরের
-- সাথে সরাসরি মেলে না (ফাইল-নম্বর তৈরির ক্রম অনুযায়ী, Step-নম্বর কাজের বিষয়বস্তু অনুযায়ী), কিন্তু
-- কভারেজ আসলেই আছে — তাই এগুলো এই "gap-fill" ফাইলে পুনরাবৃত্তি করা হয়নি (rule অনুযায়ী নতুন test
-- ফাইলে existing coverage duplicate করার দরকার নেই)।
--
-- ⚠️ SCHEMA STUB: এই ৩টা ফাংশনের জন্য দরকারি সব কলাম/টেবিল (users.balance/balance_user/
-- balance_solver, transactions.role, admin_audit_logs, messages, problems.accepted_solver_id/
-- is_admin_involved_in_chat/last_activity_at/has_release_request/release_requested_at/
-- is_disputed, notifications.role/target_type/target_id) — সবই আগের 01/10/11 schema_stub-এ
-- ইতিমধ্যেই আছে (grep করে নিশ্চিত করা হয়েছে)। তাই এই ধাপে নতুন কোনো
-- `12_coverage_gap_fill_schema_stub.sql` লাগেনি।
--
-- ⚠️ ordering: `admin_reconcile_user_balances`-এর টেস্ট নিজের একটা ফ্রেশ, dedicated ইউজার
-- (RECON_USER) বানায়, seed_users()-এর ৪টা user (CLIENT/SOLVER1/SOLVER2/ADMIN) স্পর্শ করে না।
-- কারণ (এই সেশনে ধরা পড়া একটা গুরুত্বপূর্ণ পর্যবেক্ষণ, নিচে ⚠️ NOTE-এ বিস্তারিত): seed_users()
-- সরাসরি balance/balance_user সেট করে দেয় (কোনো matching transaction insert না করে), তাই ওই
-- ৪টা ইউজারের উপর ledger-reconstruction (transactions টেবিল থেকে) চালালে "phantom mismatch"
-- দেখাবে যেটা আমাদের ইচ্ছাকৃত corruption না — সেই কারণেই একটা আলাদা, পুরোপুরি self-consistent
-- ফিক্সচার দরকার হলো যেখানে stored balance == transaction-derived ledger, শুরুতেই।
--
-- ⚠️ NOTE (এই সেশনে নতুন আবিষ্কার, কোনো bug না — শুধু একটা সীমাবদ্ধতা, ভবিষ্যতের জন্য ডকুমেন্টেড):
-- admin_reconcile_user_balances()-এর ledger পুরোপুরি `public.transactions`-এর net_amount যোগফল
-- থেকে reconstruct হয় (baseline শূন্য ধরে)। প্রোডাকশনে এটা ঠিক আছে কারণ প্রতিটা balance-পরিবর্তনকারী
-- action (deposit, bid-accept deduction, escrow release, ইত্যাদি) নিজেই একটা transaction row
-- ইনসার্ট করে — তাই "প্রাথমিক" ব্যালেন্স ধারণা বলে কিছু নেই, সবকিছুই কোনো না কোনো transaction থেকে
-- আসে। কিন্তু কোনো টেস্ট-ফিক্সচার (বা প্রোডাকশনে কোনো ম্যানুয়াল SQL দিয়ে সরাসরি balance সেট করা,
-- matching transaction row ছাড়া) যদি matching transaction row ছাড়াই সরাসরি balance/balance_user/
-- balance_solver সেট করে, admin_reconcile_user_balances() সেটাকে "mismatch" হিসেবে ধরবে এবং
-- dry_run=false-এ "সংশোধন" করে দেবে — even যদি সেই balance আসলে সঠিক ছিল। এই সীমাবদ্ধতা এই ফাইলে
-- কোনো assertion দিয়ে টেস্ট করা হয়নি (সেটা করতে হলে RPC নিজেই বদলাতে হতো, rule #1 লঙ্ঘন করে) —
-- শুধু ভবিষ্যতের কোনো সেশনের জন্য এখানে ফ্ল্যাগ করে রাখা হলো (CI_TEST_SUITE_PROGRESS.md-এও)।

BEGIN;
SELECT plan(38);

SELECT test.seed_users();
-- ADMIN: 99999999-...   non-admin (CLIENT): 11111111-...

----------------------------------------------------------------------
-- admin_reconcile_user_balances(p_dry_run boolean default true)
-- সর্বশেষ সংজ্ঞা: step37_admin_reconcile_user_balances_v2_flat_balance.sql
----------------------------------------------------------------------

-- ---- auth guard ----
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_reconcile_user_balances(true) $$,
  'P0001',
  'NOT_AUTHORIZED',
  'admin_reconcile_user_balances: non-admin কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

-- ---- fixture: dedicated, self-consistent user (উপরের NOTE দ্রষ্টব্য) ----
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, balance, balance_user, balance_solver, has_user_role, has_solver_role)
VALUES ('c0000001-0000-0000-0000-000000000001', 'SOLVER', 'Recon Test User', '01760000001', -50.00, -500.00, 450.00, true, true);

-- USER-side ledger উৎস: ঠিক accept_bid()-এর BID_ACCEPT_DEDUCTION insert-এর শেপ (role='USER',
-- type <> PAYMENT/BALANCE_RECONCILIATION, তাই ledger_user-এ গোনা হয়)
INSERT INTO public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount, commission_percent, commission_amount, net_amount, type, role, "timestamp")
VALUES ('TRX_RECON_U_1', 'P_RECON', 'Recon Fixture Problem', 'c0000001-0000-0000-0000-000000000001', NULL, 500, 0, 0, -500.00, 'BID_ACCEPT_DEDUCTION', 'USER', now());

-- SOLVER-side ledger উৎস: ঠিক release_escrow()-এর PAYMENT insert-এর শেপ (role='SOLVER',
-- type = PAYMENT, তাই ledger_solver-এ গোনা হয়, ledger_user-এ না)
INSERT INTO public.transactions (id, problem_id, problem_title, user_id, solver_id, gross_amount, commission_percent, commission_amount, net_amount, type, role, "timestamp")
VALUES ('TRX_RECON_S_1', 'P_RECON', 'Recon Fixture Problem', NULL, 'c0000001-0000-0000-0000-000000000001', 500, 10, 50, 450.00, 'PAYMENT', 'SOLVER', now());

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- ---- baseline: stored == ledger, কোনো mismatch নেই ----
SELECT is(
  (SELECT count(*) FROM jsonb_array_elements(public.admin_reconcile_user_balances(true)->'items') item
     WHERE item->>'user_id' = 'c0000001-0000-0000-0000-000000000001')::int,
  0,
  'admin_reconcile_user_balances: fixture শুরুতেই self-consistent, dry_run=true-তে কোনো item নেই'
);

-- ---- tolerance boundary: diff ঠিক ১.০০ (abs(diff) > tolerance, > কঠোরভাবে বড়, তাই ১.০০ ফ্ল্যাগ হবে না) ----
RESET ROLE;
UPDATE public.users SET balance_user = -499.00 WHERE id = 'c0000001-0000-0000-0000-000000000001'; -- diff = -499 - (-500) = 1.00
-- ⚠️ admin_reconcile_user_balances() ভেতরে `CREATE TEMP TABLE tmp_ledger_user/tmp_ledger_solver
-- ON COMMIT DROP` ব্যবহার করে — এই ফাইল পুরোটা একটা BEGIN…ROLLBACK-এর ভেতরে (কখনো COMMIT হয় না),
-- তাই একই transaction-এ RPC-টা দ্বিতীয়বার কল করলে "relation already exists" হার্ড ERROR দেয় (এই
-- সেশনে real psql রান করে প্রথমবার ধরা পড়েছে)। প্রোডাকশনে সমস্যা না (PostgREST প্রতিটা কল আলাদা
-- transaction-এ চালায়) — শুধু এই multi-call-in-one-transaction টেস্ট-স্টাইলের সাথে সাংঘর্ষিক।
-- তাই প্রতিটা repeat কলের আগে ম্যানুয়ালি DROP করে দেওয়া হচ্ছে।
DROP TABLE IF EXISTS tmp_ledger_user, tmp_ledger_solver;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (SELECT count(*) FROM jsonb_array_elements(public.admin_reconcile_user_balances(true)->'items') item
     WHERE item->>'user_id' = 'c0000001-0000-0000-0000-000000000001' AND item->>'role' = 'USER')::int,
  0,
  'admin_reconcile_user_balances: diff ঠিক tolerance (১.০০)-এর সমান হলে flag হয় না (> কঠোরভাবে বড়, boundary)'
);
RESET ROLE;
UPDATE public.users SET balance_user = -500.00 WHERE id = 'c0000001-0000-0000-0000-000000000001'; -- revert
DROP TABLE IF EXISTS tmp_ledger_user, tmp_ledger_solver;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- ---- ইচ্ছাকৃত corruption: balance_user ও balance_solver সরাসরি row-এডিট করে (master prompt-এর
-- নির্দেশ অনুযায়ী "accept_bid ও release_escrow-এর মাঝে সরাসরি row এডিট" সিমুলেট করা — এখানে দুটো
-- সোর্স-ট্রানজ্যাকশন insert হয়ে যাওয়ার *পরে*, যেন কেউ ম্যানুয়ালি balance ভুল বসিয়ে দিয়েছে) ----
RESET ROLE;
UPDATE public.users
  SET balance_user = -800.00,     -- সঠিক -500.00, diff = -300
      balance_solver = 350.00     -- সঠিক 450.00, diff = -100
  WHERE id = 'c0000001-0000-0000-0000-000000000001';
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT is(
  (SELECT (item->>'stored')::numeric || ',' || (item->>'ledger')::numeric || ',' || (item->>'difference')::numeric
     FROM jsonb_array_elements(public.admin_reconcile_user_balances(true)->'items') item
     WHERE item->>'user_id' = 'c0000001-0000-0000-0000-000000000001' AND item->>'role' = 'USER'),
  '-800.00,-500.00,-300.00',
  'admin_reconcile_user_balances: USER role — corrupted balance_user ধরা পড়ে, stored/ledger/difference সঠিক'
);
-- DROP-এর আগে RESET ROLE বাধ্যতামূলক — temp table SECURITY DEFINER ফাংশনের owner হিসেবে তৈরি হয়,
-- authenticated role হিসেবে DROP করতে গেলে "must be owner of table" (এই সেশনে real-run-এ ধরা পড়েছে)
RESET ROLE;
DROP TABLE IF EXISTS tmp_ledger_user, tmp_ledger_solver;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (SELECT (item->>'stored')::numeric || ',' || (item->>'ledger')::numeric || ',' || (item->>'difference')::numeric
     FROM jsonb_array_elements(public.admin_reconcile_user_balances(true)->'items') item
     WHERE item->>'user_id' = 'c0000001-0000-0000-0000-000000000001' AND item->>'role' = 'SOLVER'),
  '350.00,450.00,-100.00',
  'admin_reconcile_user_balances: SOLVER role — corrupted balance_solver ধরা পড়ে, stored/ledger/difference সঠিক'
);
-- FLAT (legacy `balance`) এখনো ছোঁয়া হয়নি, আর step37-এর v_effective_* সবসময় ledger-ভিত্তিক
-- preview ব্যবহার করে (dry_run-নির্বিশেষে) — তাই balance(-50.00) == ledger_user+ledger_solver
-- (-500+450=-50.00), FLAT flag হওয়ার কথা না, এমনকি USER/SOLVER দুটোই একসাথে mismatch থাকলেও।
RESET ROLE;
DROP TABLE IF EXISTS tmp_ledger_user, tmp_ledger_solver;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (SELECT count(*) FROM jsonb_array_elements(public.admin_reconcile_user_balances(true)->'items') item
     WHERE item->>'user_id' = 'c0000001-0000-0000-0000-000000000001' AND item->>'role' = 'FLAT')::int,
  0,
  'admin_reconcile_user_balances: FLAT check preview সবসময় effective(ledger-corrected) মান দিয়ে হয় — USER+SOLVER দুটোই mismatch থাকলেও legacy balance আলাদাভাবে না-ছোঁয়া হলে FLAT flag হয় না'
);

-- dry_run=true হওয়ায় DB-তে আসলে কিছু বদলায়নি, এখনো corrupted অবস্থাতেই আছে
SELECT is(
  (SELECT balance_user::text || ',' || balance_solver::text FROM public.users WHERE id = 'c0000001-0000-0000-0000-000000000001'),
  '-800.00,350.00',
  'admin_reconcile_user_balances: dry_run=true হলে DB-তে কোনো actual write হয় না'
);

-- ---- এবার legacy `balance`-ও আলাদাভাবে corrupt করে FLAT mismatch যোগ করা হলো, তারপর dry_run=false ----
RESET ROLE;
UPDATE public.users SET balance = -1000.00 WHERE id = 'c0000001-0000-0000-0000-000000000001'; -- সঠিক টার্গেট হবে -50 (effective -500+450)
DROP TABLE IF EXISTS tmp_ledger_user, tmp_ledger_solver;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
DO $$ BEGIN PERFORM set_config('test.recon_before_audit', (SELECT count(*)::text FROM public.admin_audit_logs), true); END $$;

SELECT is(
  (public.admin_reconcile_user_balances(false))->>'dry_run',
  'false',
  'admin_reconcile_user_balances: dry_run=false পাস করলে response-এ dry_run:false ফেরত আসে'
);

SELECT is(
  (SELECT balance::text || ',' || balance_user::text || ',' || balance_solver::text
     FROM public.users WHERE id = 'c0000001-0000-0000-0000-000000000001'),
  '-50.00,-500.00,450.00',
  'admin_reconcile_user_balances: dry_run=false হলে balance/balance_user/balance_solver তিনটাই ledger-এর সাথে মিলিয়ে সংশোধন হয়'
);

SELECT results_eq(
  $$ SELECT type, role, net_amount FROM public.transactions
       WHERE id = 'TRX_RECONCILE_U_c0000001-0000-0000-0000-000000000001_' || (
         SELECT extract(epoch FROM "timestamp")::bigint FROM public.transactions
         WHERE id LIKE 'TRX_RECONCILE_U_c0000001-0000-0000-0000-000000000001_%' LIMIT 1) $$,
  $$ VALUES ('BALANCE_RECONCILIATION'::text, 'USER'::text, 300.00::numeric) $$,
  'admin_reconcile_user_balances: USER-সংশোধনের জন্য BALANCE_RECONCILIATION transaction insert হয়, role=USER, net_amount = ledger-stored (৩০০)'
);

SELECT results_eq(
  $$ SELECT type, role, net_amount FROM public.transactions
       WHERE id = 'TRX_RECONCILE_S_c0000001-0000-0000-0000-000000000001_' || (
         SELECT extract(epoch FROM "timestamp")::bigint FROM public.transactions
         WHERE id LIKE 'TRX_RECONCILE_S_c0000001-0000-0000-0000-000000000001_%' LIMIT 1) $$,
  $$ VALUES ('BALANCE_RECONCILIATION'::text, 'SOLVER'::text, 100.00::numeric) $$,
  'admin_reconcile_user_balances: SOLVER-সংশোধনের জন্য BALANCE_RECONCILIATION transaction insert হয়, role=SOLVER, net_amount = ledger-stored (১০০)'
);

SELECT is(
  (SELECT count(*)::int FROM public.transactions WHERE id LIKE 'TRX_RECONCILE_F_c0000001-0000-0000-0000-000000000001_%'),
  1,
  'admin_reconcile_user_balances: FLAT-সংশোধনের জন্যও একটা BALANCE_RECONCILIATION transaction insert হয় (role="")'
);

SELECT is(
  (SELECT (count(*)::int) FROM public.admin_audit_logs WHERE action_type = 'ADMIN_RECONCILE_USER_BALANCES')
    - current_setting('test.recon_before_audit')::int,
  1,
  'admin_reconcile_user_balances: corrected_count > 0 হলে admin_audit_logs-এ ঠিক একটা নতুন ADMIN_RECONCILE_USER_BALANCES entry যোগ হয়'
);

-- ---- idempotency: সংশোধনের পরে আবার dry_run=true চালালে আর কোনো mismatch দেখানোর কথা না ----
RESET ROLE;
DROP TABLE IF EXISTS tmp_ledger_user, tmp_ledger_solver;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (SELECT count(*) FROM jsonb_array_elements(public.admin_reconcile_user_balances(true)->'items') item
     WHERE item->>'user_id' = 'c0000001-0000-0000-0000-000000000001')::int,
  0,
  'admin_reconcile_user_balances: সংশোধনের পরে idempotent — আর কোনো mismatch দেখায় না (USER/SOLVER/FLAT তিনটাই ঠিক হয়ে গেছে)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_send_message_to_problem_chat(p_problem_id, p_content, p_receiver_id default null)
-- সংজ্ঞা: recovered_admin_moderation.sql
----------------------------------------------------------------------

RESET ROLE;
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id)
VALUES ('P_MSG1', '11111111-1111-1111-1111-111111111111', 'Message Test Problem', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222');

-- ---- auth guard: non-admin ----
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_send_message_to_problem_chat('P_MSG1', 'হ্যালো') $$,
  'P0001',
  'ADMIN_ONLY',
  'admin_send_message_to_problem_chat: non-admin কল করলে ADMIN_ONLY'
);
SELECT test.logout();

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- ---- content required ----
SELECT throws_ok(
  $$ SELECT public.admin_send_message_to_problem_chat('P_MSG1', '   ') $$,
  'P0001',
  'CONTENT_REQUIRED',
  'admin_send_message_to_problem_chat: শুধু whitespace content দিলে CONTENT_REQUIRED (trim করে ফাঁকা)'
);

-- ---- problem not found ----
SELECT throws_ok(
  $$ SELECT public.admin_send_message_to_problem_chat('NOPE', 'হ্যালো') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'admin_send_message_to_problem_chat: অস্তিত্বহীন problem_id-তে PROBLEM_NOT_FOUND'
);

-- ---- invalid receiver (owner-ও না, accepted_solver-ও না) ----
SELECT throws_ok(
  $$ SELECT public.admin_send_message_to_problem_chat('P_MSG1', 'হ্যালো', '33333333-3333-3333-3333-333333333333') $$,
  'P0001',
  'INVALID_RECEIVER',
  'admin_send_message_to_problem_chat: p_receiver_id owner/accepted_solver কোনোটাই না হলে INVALID_RECEIVER'
);

-- ---- happy path: receiver NULL → owner-এ resolve ----
SELECT is(
  (public.admin_send_message_to_problem_chat('P_MSG1', 'আপনার সমস্যাটি দেখা হচ্ছে।'))->>'result',
  'OK',
  'admin_send_message_to_problem_chat: receiver না দিলেও (NULL) OK রিটার্ন করে'
);
SELECT is(
  (SELECT receiver_id::text FROM public.messages WHERE problem_id = 'P_MSG1' ORDER BY "timestamp" LIMIT 1),
  '11111111-1111-1111-1111-111111111111',
  'admin_send_message_to_problem_chat: p_receiver_id NULL হলে receiver স্বয়ংক্রিয়ভাবে problem owner-এ resolve হয়'
);
SELECT is(
  (SELECT sender_name || ',' || is_admin_message::text FROM public.messages WHERE problem_id = 'P_MSG1' ORDER BY "timestamp" LIMIT 1),
  'Support Manager 🛡️,true',
  'admin_send_message_to_problem_chat: sender_name হার্ডকোডেড "Support Manager 🛡️", is_admin_message=true'
);
SELECT is(
  (SELECT is_admin_involved_in_chat FROM public.problems WHERE id = 'P_MSG1'),
  true,
  'admin_send_message_to_problem_chat: problems.is_admin_involved_in_chat = true সেট হয়ে যায়'
);

-- ---- happy path: receiver = accepted_solver_id ----
SELECT is(
  (public.admin_send_message_to_problem_chat('P_MSG1', 'সলভারের জন্য বার্তা', '22222222-2222-2222-2222-222222222222'))->>'result',
  'OK',
  'admin_send_message_to_problem_chat: receiver হিসেবে সরাসরি accepted_solver_id দিলেও OK'
);
SELECT is(
  (SELECT receiver_id::text FROM public.messages WHERE problem_id = 'P_MSG1' AND content = 'সলভারের জন্য বার্তা'),
  '22222222-2222-2222-2222-222222222222',
  'admin_send_message_to_problem_chat: p_receiver_id = accepted_solver_id দিলে ঠিক সেই solver-কেই receiver হিসেবে ইনসার্ট করে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- system_notify_48hour_auto_release(p_problem_id, p_target_user_id, p_title, p_message,
--                                    p_target_type default 'problem', p_target_id default null)
-- সংজ্ঞা: recovered_instant_jobs.sql
----------------------------------------------------------------------

-- ---- auth required: লগইন ছাড়া ----
SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_MSG1', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001',
  'AUTH_REQUIRED',
  'system_notify_48hour_auto_release: auth.uid() null হলে AUTH_REQUIRED'
);

-- (বাকি সব কল কোনো authenticated user দিয়ে — RPC-টা নিজে শুধু auth.uid() IS NOT NULL চেক করে,
--  owner/solver-নির্দিষ্ট কোনো caller-role guard নেই, শুধু p_target_user_id owner/solver হতে হবে)
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- ---- missing params ----
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release(NULL, '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001',
  'MISSING_PARAMS',
  'system_notify_48hour_auto_release: p_problem_id NULL হলে MISSING_PARAMS'
);

-- ---- title required ----
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_MSG1', '11111111-1111-1111-1111-111111111111', '   ', 'বার্তা') $$,
  'P0001',
  'TITLE_REQUIRED',
  'system_notify_48hour_auto_release: শুধু whitespace title দিলে TITLE_REQUIRED'
);

-- ---- problem not found ----
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('NOPE', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'system_notify_48hour_auto_release: অস্তিত্বহীন problem_id-তে PROBLEM_NOT_FOUND'
);

-- ---- eligibility fixtures ----
RESET ROLE;
-- (ক) status COMPLETED → not eligible
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id, has_release_request, release_requested_at, is_disputed)
VALUES ('P_48H_DONE', '11111111-1111-1111-1111-111111111111', '48h Done', 'COMPLETED', false, true, '22222222-2222-2222-2222-222222222222', true, now() - interval '72 hours', false);
-- (খ) accepted_solver_id NULL → not eligible
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id, has_release_request, release_requested_at, is_disputed)
VALUES ('P_48H_NOSOLVER', '11111111-1111-1111-1111-111111111111', '48h No Solver', 'IN_PROGRESS', false, true, NULL, true, now() - interval '72 hours', false);
-- (গ) has_release_request false → not eligible
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id, has_release_request, release_requested_at, is_disputed)
VALUES ('P_48H_NOREQ', '11111111-1111-1111-1111-111111111111', '48h No Request', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222', false, NULL, false);
-- (ঘ) is_disputed true → not eligible
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id, has_release_request, release_requested_at, is_disputed)
VALUES ('P_48H_DISPUTED', '11111111-1111-1111-1111-111111111111', '48h Disputed', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222', true, now() - interval '72 hours', true);
-- (ঙ) release_requested_at মাত্র ২ ঘণ্টা আগে → NOT_YET_ELIGIBLE
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id, has_release_request, release_requested_at, is_disputed)
VALUES ('P_48H_TOOSOON', '11111111-1111-1111-1111-111111111111', '48h Too Soon', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222', true, now() - interval '2 hours', false);
-- (চ) সব শর্ত পূরণ (৭২ ঘণ্টা আগে) → eligible, happy path-এর জন্য
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id, has_release_request, release_requested_at, is_disputed)
VALUES ('P_48H_OK', '11111111-1111-1111-1111-111111111111', '48h Eligible', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222', true, now() - interval '72 hours', false);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_48H_DONE', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001', 'PROBLEM_NOT_ELIGIBLE',
  'system_notify_48hour_auto_release: status IN_PROGRESS/OPEN-এর বাইরে (COMPLETED) হলে PROBLEM_NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_48H_NOSOLVER', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001', 'PROBLEM_NOT_ELIGIBLE',
  'system_notify_48hour_auto_release: accepted_solver_id NULL হলে PROBLEM_NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_48H_NOREQ', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001', 'PROBLEM_NOT_ELIGIBLE',
  'system_notify_48hour_auto_release: has_release_request=false হলে PROBLEM_NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_48H_DISPUTED', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001', 'PROBLEM_NOT_ELIGIBLE',
  'system_notify_48hour_auto_release: is_disputed=true হলে PROBLEM_NOT_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_48H_TOOSOON', '11111111-1111-1111-1111-111111111111', 'শিরোনাম', 'বার্তা') $$,
  'P0001', 'NOT_YET_ELIGIBLE',
  'system_notify_48hour_auto_release: release_requested_at ৪৮ ঘণ্টার কম আগে হলে NOT_YET_ELIGIBLE'
);
SELECT throws_ok(
  $$ SELECT public.system_notify_48hour_auto_release('P_48H_OK', '33333333-3333-3333-3333-333333333333', 'শিরোনাম', 'বার্তা') $$,
  'P0001', 'NOT_AUTHORIZED',
  'system_notify_48hour_auto_release: p_target_user_id owner/accepted_solver কোনোটাই না হলে NOT_AUTHORIZED'
);

-- ---- happy path: target = owner → role USER ----
SELECT is(
  (public.system_notify_48hour_auto_release('P_48H_OK', '11111111-1111-1111-1111-111111111111', '৪৮ ঘণ্টা রিমাইন্ডার', 'অটো-রিলিজ শীঘ্রই হবে'))->>'result',
  'OK',
  'system_notify_48hour_auto_release: সব শর্ত পূরণ হলে (owner target) OK'
);
SELECT is(
  (SELECT role FROM public.notifications WHERE related_problem_id = 'P_48H_OK' AND user_id = '11111111-1111-1111-1111-111111111111'),
  'USER',
  'system_notify_48hour_auto_release: target owner হলে notification role=USER'
);

-- ---- happy path: target = accepted_solver → role SOLVER ----
SELECT is(
  (public.system_notify_48hour_auto_release('P_48H_OK', '22222222-2222-2222-2222-222222222222', '৪৮ ঘণ্টা রিমাইন্ডার', 'অটো-রিলিজ শীঘ্রই হবে'))->>'result',
  'OK',
  'system_notify_48hour_auto_release: সব শর্ত পূরণ হলে (solver target) OK'
);
SELECT is(
  (SELECT role FROM public.notifications WHERE related_problem_id = 'P_48H_OK' AND user_id = '22222222-2222-2222-2222-222222222222'),
  'SOLVER',
  'system_notify_48hour_auto_release: target accepted_solver হলে notification role=SOLVER'
);

SELECT test.logout();
SELECT * FROM finish();
ROLLBACK;
