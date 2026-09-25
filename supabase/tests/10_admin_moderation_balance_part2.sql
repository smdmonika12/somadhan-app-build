-- 10_admin_moderation_balance_part2.sql — Step 7 (Admin moderation & balance), PART 2 of 2
--
-- PART 1 (10_admin_moderation_balance_part1.sql) কভার করেছে "money & account-status" থিমের ৯টা
-- ফাংশন। এই ফাইলে বাকি সব — মোট ২৩টা ফাংশন-নাম (২টা overload-জোড়া সহ):
--   admin_get_dashboard_metrics                → step32_5_admin_get_dashboard_metrics.sql
--   admin_delete_message / admin_delete_rating / admin_delete_notification_group
--   admin_notify_user (২টা overload) / admin_broadcast_notification / log_admin_action (২টা overload)
--   admin_reassign_solver / admin_remove_category_from_solvers
--   admin_update_problem_budget / admin_update_problem_status
--   admin_update_direct_contract_status / admin_force_cancel_instant_job
--                                              → recovered_admin_moderation.sql
--   admin_soft_delete_user / admin_update_withdrawal_trx_id
--   admin_reset_free_job_quota / admin_bulk_reset_free_job_quota / admin_reset_miss_cycle
--   admin_reconcile_escrow_states / admin_repair_missing_refunds / admin_cleanup_duplicate_refunds
--   owner_reset_orphaned_accepted_bid          → step32_* ফাইলগুলো
--   admin_wipe_all_data (⚠️ destructive — সবার শেষে, নিচের নোট দেখুন)
--
-- সব ফাংশনের real body এই সেশনে সরাসরি পড়ে verify করা হয়েছে; প্রতিটার জন্য
-- `grep -i "function <name>"` চালিয়ে দেখা হয়েছে migrations-এ আর কোথাও override নেই
-- (Step 7 PART 1-এর `admin_adjust_balance`-এর মতো step36 override এই ২৩টার কোনোটার নেই),
-- তাই টেস্ট প্রতিটার একমাত্র সংজ্ঞার সাথে মেলানো।
--
-- ⚠️ DOCUMENTED CURRENT BEHAVIOUR (কিছু বদলানো হয়নি, rule #1 — টেস্টে "যেমন আছে তেমন"
-- লক করা; মানুষের review দরকার — বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এ):
--   ক) admin_get_dashboard_metrics non-admin-কে 'admin access required' (ছোট হাতের, স্পেস সহ)
--      দিয়ে ফেরায় — বাকি সব admin RPC-র ADMIN_ONLY/NOT_AUTHORIZED কনভেনশন থেকে আলাদা।
--   খ) log_admin_action শুধু auth.uid() IS NOT NULL চেক করে — is_admin() না। অর্থাৎ যেকোনো
--      লগইন-করা (non-admin) ইউজারও admin_audit_logs-এ সারি ঢোকাতে পারে।
--   গ) admin_notify_user (৬ vs ৭-আর্গ) আর log_admin_action (৪ vs ৫-আর্গ) — দুটো overload-এই
--      শেষের প্যারামিটারগুলো DEFAULT-যুক্ত, তাই ছোট overload-টা কল করলেই Postgres
--      "function ... is not unique" (42725) দেয় — ছোট ভার্সনগুলো কার্যত কল-অযোগ্য (এটাই
--      Step 11-এর "KNOWN AMBIGUOUS OVERLOAD" candidate)। এখানে শুধু catalog-এ দুটো overload
--      আছে + ambiguity এখনো আছে তা লক করা; ফিক্স হলে এই assertion ইচ্ছাকৃতভাবে ভাঙবে।
--   ঘ) [Step 7.2-এ ফিক্সড] admin_force_cancel_instant_job এখন accepted solver থাকলে HELD escrow নিজেই
--      refund_escrow_once() দিয়ে REFUNDED করে (আগে ছুঁতো না); আগে-থেকে-REFUNDED escrow-এ double-refund হয় না।
--      CANCEL branch-এ accepted_solver_id নাল করে না (শুধু accepted_bid_id করে)।
--   ঙ) admin_update_direct_contract_status-এ p_status='COMPLETED' দিলে HELD escrow না থাকলে
--      problems.status আদৌ COMPLETED হয় না (শুধু direct_contract_status বদলায়)।
--   চ) owner_reset_orphaned_accepted_bid problems পরিষ্কার করে কিন্তু accepted bid-এর নিজের
--      status ACCEPTED-ই রেখে দেয়।
--   ছ) admin_reconcile_escrow_states: dry-run-এ ৫-repair ক্যাপ কাজ করে না (v_repairs_this_run
--      শুধু live-এ বাড়ে) — তাই dry-run সব candidate গোনে, live সর্বোচ্চ ৫টা repair করে।
--   জ) admin_update_problem_status যেকোনো স্ট্রিং status হিসেবে গ্রহণ করে (কোনো validation নেই)।
--   ঝ) admin_reset_free_job_quota টার্গেট আদৌ solver কিনা যাচাই করে না (যেকোনো users.id চলে)।
--   ঞ) admin_update_withdrawal_trx_id-এ status/NULL/ফাঁকা কোনো validation নেই।
--
-- ⚠️ ordering: admin_reconcile_escrow_states সব HELD escrow-তে কাজ করে — তাই এই ফাইলে
-- (১) reconcile/repair/cleanup সেকশন আগে, (২) নতুন HELD escrow তৈরি করে এমন সেকশন
-- (direct-contract, force-cancel, owner-reset) পরে, আর (৩) admin_wipe_all_data সবশেষে।
-- প্রতিটা সেকশন নিজের fixture নিজেই `RESET ROLE; INSERT …` দিয়ে বসায় (আগের সেকশনের
-- session role যা-ই থাকুক)। ফাইল পুরোটা BEGIN … ROLLBACK-এর ভেতরে — wipe-ও rollback হয়ে
-- যায়, অন্য ফাইলের data মোছার ঝুঁকি নেই (run_tests.sh প্রতিটা ফাইল আলাদা psql সেশনে চালায়)।
--
-- ⚠️ ⚠️ এই সেশনে sandbox network বন্ধ ছিল (apt-get update → 403; Step 7 PART 1-এর নোটের
-- "handoff ২" প্রযোজ্য) — তাই এই ফাইলও শুধু static ভাবে verify করা: প্রতিটা assertion
-- ফাংশন-বডি, error-message, notification-title/কলাম-নামের সাথে হুবহু মিলিয়ে, plan()
-- সংখ্যা script দিয়ে গুনে মিলিয়ে। Real Postgres+pgTAP-এ কখনো চালানো হয়নি।
--
-- ⚠️⚠️ ২০২৬-০৯-২১ (Step 12.12, F1 ফিক্স, ব্যবহারকারীর অনুমতিতে) — admin_notify_user ও
-- log_admin_action-এর পুরনো (p_role-বিহীন) overload step12_9 migration-এ DROP হওয়ায়
-- "২টা overload আছে" ও "ছোট-arg কল ambiguous (42725)" assertion দুটো ঠিক থাকে না —
-- এখন ১টাই overload, আর ছোট-arg কল p_role DEFAULT ('''') দিয়ে resolve হয়ে সফল হয়
-- (migration body সরাসরি পড়ে p_role DEFAULT ''::text নিশ্চিত করা হয়েছে)। plan()
-- 194→196 (প্রতিটাতে ambiguity-throws_ok বাদ, তার বদলে success+role-column assertion
-- ২টা করে যোগ)। এই সেশনেও sandbox network বন্ধ — static-ভাবেই যাচাই, real Postgres-এ
-- চালানো হয়নি। বিস্তারিত: CI_TEST_SUITE_PROGRESS.md-এর এই সেশনের "Step 12.12" সেকশন।
--
-- সাধারণ actor: CLIENT 11111111-… (non-admin caller)  SOLVER1 22222222-…  SOLVER2 33333333-…
--               ADMIN 99999999-…   অজানা uuid aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa

-- ⚠️⚠️ ২০২৬-০৯-২৪ (Somadhan Bug-Fix Step 7.8, ব্যবহারকারীর অনুমতিতে) — admin_reconcile_escrow_states
-- Case 2/3-এ per-escrow exception-handling যোগ হয়েছে (একটা escrow ব্যর্থ হলে আর পুরো রান rollback
-- হয় না)। নতুন ৩য় live-রান scenario (E_RC10..E_RC13: ২টা সুস্থ + ২টা role-নিষ্ক্রিয়) ৭টা নতুন
-- assertion যোগ করেছে (plan ২০১→২০৮)। এই সেশনেও sandbox network বন্ধ — static-ভাবে ফাংশন-বডি ও
-- refund_escrow_once/release_escrow-এর real exception-message (USER_ROLE_INACTIVE/SOLVER_ROLE_
-- INACTIVE) মিলিয়ে যাচাই করা হয়েছে, real Postgres+pgTAP-এ চালানো হয়নি।
--
BEGIN;
SELECT plan(208);

SELECT test.seed_users();

----------------------------------------------------------------------
-- admin_get_dashboard_metrics
-- baseline (GUC-এ) → fixture বসানো → delta মেলানো। পুরো টেবিল-গণনা বলে অন্য
-- সেকশনের/migration-এর data থাকলেও delta ঠিক থাকে।
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
DO $$ BEGIN PERFORM set_config('test.dm_before', public.admin_get_dashboard_metrics()::text, true); END $$;

RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('e1000001-0000-0000-0000-000000000001', 'SOLVER', 'DM Solver 1', '01750000001', true, true),
  ('e1000002-0000-0000-0000-000000000002', 'SOLVER', 'DM Solver 2', '01750000002', true, true),
  ('e1000003-0000-0000-0000-000000000003', 'USER',   'DM User',     '01750000003', true, false);
INSERT INTO public.problems (id, user_id, title, status, category_name) VALUES
  ('DM_P1', 'e1000003-0000-0000-0000-000000000003', 'DM P1', 'OPEN',        'CAT_DM_A'),
  ('DM_P2', 'e1000003-0000-0000-0000-000000000003', 'DM P2', 'OPEN',        'CAT_DM_A'),
  ('DM_P3', 'e1000003-0000-0000-0000-000000000003', 'DM P3', 'COMPLETED',   'CAT_DM_B'),
  ('DM_P4', 'e1000003-0000-0000-0000-000000000003', 'DM P4', 'IN_PROGRESS', NULL),
  ('DM_P5', 'e1000003-0000-0000-0000-000000000003', 'DM P5', 'CANCELLED',   '');
INSERT INTO public.bids (id, problem_id, solver_id, amount, status) VALUES
  ('DM_B1', 'DM_P1', 'e1000001-0000-0000-0000-000000000001', 100, 'PENDING'),
  ('DM_B2', 'DM_P1', 'e1000002-0000-0000-0000-000000000002', 110, 'PENDING'),
  ('DM_B3', 'DM_P3', 'e1000001-0000-0000-0000-000000000001', 120, 'ACCEPTED'),
  ('DM_B4', 'DM_P4', 'e1000002-0000-0000-0000-000000000002', 130, 'REJECTED'),
  ('DM_B5', 'DM_P5', 'e1000001-0000-0000-0000-000000000001', 140, 'CANCELLED');
INSERT INTO public.transactions (id, problem_id, user_id, gross_amount, commission_amount, net_amount, type) VALUES
  ('DM_T1', 'DM_P3', NULL, 1000, 100, 900, 'PAYMENT'),
  ('DM_T2', 'DM_P3', NULL,  500,  50, 450, 'PAYMENT'),
  ('DM_T3', NULL,    NULL, NULL, NULL, NULL, 'MISC');
INSERT INTO public.withdrawals (id, solver_id, amount, status) VALUES
  ('DM_W1', 'e1000001-0000-0000-0000-000000000001', 100, 'PENDING'),
  ('DM_W2', 'e1000002-0000-0000-0000-000000000002', 200, 'PENDING'),
  ('DM_W3', 'e1000001-0000-0000-0000-000000000001', 300, 'COMPLETED'),
  ('DM_W4', 'e1000002-0000-0000-0000-000000000002', 400, 'COMPLETED'),
  ('DM_W5', 'e1000001-0000-0000-0000-000000000001', 999, 'REJECTED');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_get_dashboard_metrics() $$,
  'P0001', 'admin access required',
  'admin_get_dashboard_metrics: non-admin কল করলে "admin access required" (⚠️ অন্য admin RPC-র ADMIN_ONLY/NOT_AUTHORIZED কনভেনশন থেকে আলাদা মেসেজ)'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT m ?& ARRAY['total_users','total_solvers','total_problems','open_problems','completed_problems',
                     'in_progress_problems','total_bids','pending_bids','accepted_bids',
                     'total_transaction_volume','platform_revenue','pending_withdrawals',
                     'completed_withdrawals','category_problem_counts','category_bid_counts']
      AND (SELECT count(*) FROM jsonb_object_keys(m)) = 15
   FROM (SELECT public.admin_get_dashboard_metrics() AS m) s),
  'admin_get_dashboard_metrics: রিটার্ন jsonb-তে হুবহু ১৫টা প্রত্যাশিত key আছে (বেশি/কম না)'
);
SELECT ok(
  (SELECT ARRAY[
     (a->>'total_users')::int   - (b->>'total_users')::int,
     (a->>'total_solvers')::int - (b->>'total_solvers')::int
   ] = ARRAY[3, 2]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: fixture-এর পর total_users +৩, total_solvers +২ (role=SOLVER ছাড়া USER-role গোনা হয় না)'
);
SELECT ok(
  (SELECT ARRAY[
     (a->>'total_problems')::int       - (b->>'total_problems')::int,
     (a->>'open_problems')::int        - (b->>'open_problems')::int,
     (a->>'completed_problems')::int   - (b->>'completed_problems')::int,
     (a->>'in_progress_problems')::int - (b->>'in_progress_problems')::int
   ] = ARRAY[5, 2, 1, 1]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: problems delta — total +৫, OPEN +২, COMPLETED +১, IN_PROGRESS +১ (CANCELLED শুধু total-এ)'
);
SELECT ok(
  (SELECT ARRAY[
     (a->>'total_bids')::int    - (b->>'total_bids')::int,
     (a->>'pending_bids')::int  - (b->>'pending_bids')::int,
     (a->>'accepted_bids')::int - (b->>'accepted_bids')::int
   ] = ARRAY[5, 2, 1]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: bids delta — total +৫, PENDING +২, ACCEPTED +১ (REJECTED/CANCELLED শুধু total-এ)'
);
SELECT ok(
  (SELECT ARRAY[
     (a->>'total_transaction_volume')::numeric - (b->>'total_transaction_volume')::numeric,
     (a->>'platform_revenue')::numeric         - (b->>'platform_revenue')::numeric
   ] = ARRAY[1500, 150]::numeric[]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: gross volume +১৫০০, commission revenue +১৫০ (NULL-amount transaction sum-এ কোনো প্রভাব ফেলে না)'
);
SELECT ok(
  (SELECT ARRAY[
     (a->>'pending_withdrawals')::numeric   - (b->>'pending_withdrawals')::numeric,
     (a->>'completed_withdrawals')::numeric - (b->>'completed_withdrawals')::numeric
   ] = ARRAY[2, 700]::numeric[]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: pending_withdrawals +২ (count), completed_withdrawals +৭০০ (শুধু COMPLETED-এর sum, REJECTED ৯৯৯ বাদ)'
);
SELECT ok(
  (SELECT ARRAY[
     coalesce((a->'category_problem_counts'->>'CAT_DM_A')::int, 0) - coalesce((b->'category_problem_counts'->>'CAT_DM_A')::int, 0),
     coalesce((a->'category_problem_counts'->>'CAT_DM_B')::int, 0) - coalesce((b->'category_problem_counts'->>'CAT_DM_B')::int, 0),
     coalesce((a->'category_problem_counts'->>'অন্যান্য')::int, 0) - coalesce((b->'category_problem_counts'->>'অন্যান্য')::int, 0)
   ] = ARRAY[2, 1, 2]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: category_problem_counts — CAT_DM_A +২, CAT_DM_B +১, NULL/ফাঁকা category "অন্যান্য" key-তে +২'
);
SELECT ok(
  (SELECT ARRAY[
     coalesce((a->'category_bid_counts'->>'CAT_DM_A')::int, 0) - coalesce((b->'category_bid_counts'->>'CAT_DM_A')::int, 0),
     coalesce((a->'category_bid_counts'->>'CAT_DM_B')::int, 0) - coalesce((b->'category_bid_counts'->>'CAT_DM_B')::int, 0),
     coalesce((a->'category_bid_counts'->>'সাধারণ')::int, 0)  - coalesce((b->'category_bid_counts'->>'সাধারণ')::int, 0)
   ] = ARRAY[2, 1, 2]
   FROM (SELECT public.admin_get_dashboard_metrics() AS a, current_setting('test.dm_before')::jsonb AS b) s),
  'admin_get_dashboard_metrics: category_bid_counts — CAT_DM_A +২, CAT_DM_B +১, NULL/ফাঁকা category "সাধারণ" key-তে +২ (problems-এর "অন্যান্য" থেকে আলাদা fallback নাম)'
);

----------------------------------------------------------------------
-- admin_delete_message / admin_delete_rating
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.messages (id, problem_id, sender_id, receiver_id, sender_name, content) VALUES
  ('MSG_DEL_1', NULL, '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'Test Client', 'hello');
INSERT INTO public.ratings (id, problem_id, user_id, solver_id, stars) VALUES
  ('RAT_DEL_1', NULL, '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 5);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_delete_message('MSG_DEL_1') $$,
  'P0001', 'ADMIN_ONLY',
  'admin_delete_message: non-admin কল করলে ADMIN_ONLY'
);
SELECT throws_ok(
  $$ SELECT public.admin_delete_rating('RAT_DEL_1') $$,
  'P0001', 'ADMIN_ONLY',
  'admin_delete_rating: non-admin কল করলে ADMIN_ONLY'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'id' = 'MSG_DEL_1'
   FROM (SELECT public.admin_delete_message('MSG_DEL_1') AS r) s),
  'admin_delete_message: happy path → result OK + মোছা id ফেরত আসে'
);
SELECT is(
  (SELECT count(*)::int FROM public.messages WHERE id = 'MSG_DEL_1'),
  0,
  'admin_delete_message: সারিটা আসলেই মুছে গেছে'
);
SELECT is(
  (public.admin_delete_message('MSG_DEL_1'))->>'result',
  'NOT_FOUND',
  'admin_delete_message: দ্বিতীয়বার/অস্তিত্বহীন id → result NOT_FOUND (exception না)'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'id' = 'RAT_DEL_1'
   FROM (SELECT public.admin_delete_rating('RAT_DEL_1') AS r) s),
  'admin_delete_rating: happy path → result OK + মোছা id ফেরত আসে'
);
SELECT is(
  (SELECT count(*)::int FROM public.ratings WHERE id = 'RAT_DEL_1'),
  0,
  'admin_delete_rating: সারিটা আসলেই মুছে গেছে'
);
SELECT is(
  (public.admin_delete_rating('RAT_DEL_1'))->>'result',
  'NOT_FOUND',
  'admin_delete_rating: দ্বিতীয়বার/অস্তিত্বহীন id → result NOT_FOUND (exception না)'
);

----------------------------------------------------------------------
-- admin_delete_notification_group
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.notifications (id, user_id, title, message, scheduled_for, "timestamp") VALUES
  ('NG_A1', '11111111-1111-1111-1111-111111111111', 'NGroupA', 'm', '2030-01-01 10:00:00+00', '2026-01-01 00:00:00+00'),
  ('NG_A2', '11111111-1111-1111-1111-111111111111', 'NGroupA', 'm', '2030-01-01 10:00:00+00', '2026-01-01 00:00:00+00'),
  ('NG_A3', '11111111-1111-1111-1111-111111111111', 'NGroupA', 'm', '2030-02-02 10:00:00+00', '2026-01-01 00:00:00+00'),
  ('NG_B1', '11111111-1111-1111-1111-111111111111', 'NGroupB', 'm', NULL, '2026-03-01 10:00:00+00'),
  ('NG_B2', '11111111-1111-1111-1111-111111111111', 'NGroupB', 'm', NULL, '2026-03-02 10:00:00+00'),
  ('NG_C1', '11111111-1111-1111-1111-111111111111', 'NGroupC', 'm', NULL, '2026-04-01 00:00:00+00'),
  ('NG_C2', '22222222-2222-2222-2222-222222222222', 'NGroupC', 'm', '2030-05-05 10:00:00+00', '2026-04-02 00:00:00+00'),
  ('NG_D1', '11111111-1111-1111-1111-111111111111', 'NGroupD', 'm', NULL, '2026-05-01 00:00:00+00'),
  ('NG_E1', '11111111-1111-1111-1111-111111111111', 'NGroupE', 'm', '2030-06-06 10:00:00+00', '2026-06-01 00:00:00+00'),
  ('NG_O1', '11111111-1111-1111-1111-111111111111', 'NGroupOther', 'm', NULL, '2026-07-01 00:00:00+00');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_delete_notification_group('NGroupA') $$,
  'P0001', 'ADMIN_ONLY',
  'admin_delete_notification_group: non-admin কল করলে ADMIN_ONLY'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_delete_notification_group('   ') $$,
  'P0001', 'TITLE_REQUIRED',
  'admin_delete_notification_group: শুধু স্পেসের title → TITLE_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.admin_delete_notification_group(NULL) $$,
  'P0001', 'TITLE_REQUIRED',
  'admin_delete_notification_group: NULL title → TITLE_REQUIRED'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'deleted')::int = 2
   FROM (SELECT public.admin_delete_notification_group('NGroupA', '2030-01-01 10:00:00+00'::timestamptz) AS r) s),
  'admin_delete_notification_group: scheduled_for branch → শুধু ওই schedule-এর ২টা সারি মোছে (deleted=2)'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE title = 'NGroupA'),
  1,
  'admin_delete_notification_group: scheduled_for branch-এ অন্য schedule-এর (2030-02-02) সারিটা অক্ষত'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'deleted')::int = 1
   FROM (SELECT public.admin_delete_notification_group('NGroupB', NULL, '2026-03-01 10:00:00+00'::timestamptz) AS r) s),
  'admin_delete_notification_group: "timestamp" branch (scheduled_for NULL) → শুধু ওই timestamp-এর ১টা সারি মোছে'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE title = 'NGroupB'),
  1,
  'admin_delete_notification_group: "timestamp" branch-এ অন্য timestamp-এর সারিটা অক্ষত'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'deleted')::int = 2
   FROM (SELECT public.admin_delete_notification_group('NGroupC') AS r) s),
  'admin_delete_notification_group: শুধু title দিলে schedule/timestamp নির্বিশেষে ওই title-এর সব সারি মোছে (২টা, দুই ইউজারের)'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE title = 'NGroupOther'),
  1,
  'admin_delete_notification_group: অন্য title-এর সারি (NGroupOther) কখনো ছোঁয়া হয় না'
);
SELECT ok(
  (SELECT (r->>'deleted')::int = 1
   FROM (SELECT public.admin_delete_notification_group('  NGroupD  ') AS r) s),
  'admin_delete_notification_group: p_title trim হয় — "  NGroupD  " দিয়েও NGroupD সারিটা মোছে'
);
SELECT ok(
  (SELECT (r->>'deleted')::int = 1
   FROM (SELECT public.admin_delete_notification_group('NGroupE', '2030-06-06 10:00:00+00'::timestamptz, '1999-01-01 00:00:00+00'::timestamptz) AS r) s),
  'admin_delete_notification_group: scheduled_for আর "timestamp" দুটোই দিলে scheduled_for-ই অগ্রাধিকার পায় (অমিল timestamp উপেক্ষিত)'
);

----------------------------------------------------------------------
-- admin_notify_user (৬-আর্গ ও ৭-আর্গ overload)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_notify_user('11111111-1111-1111-1111-111111111111', 'T', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'ADMIN_ONLY',
  'admin_notify_user(7-arg): non-admin কল করলে ADMIN_ONLY'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_notify_user(NULL::uuid, 'T', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'USER_ID_REQUIRED',
  'admin_notify_user(7-arg): p_user_id NULL → USER_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.admin_notify_user('11111111-1111-1111-1111-111111111111', '   ', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'TITLE_REQUIRED',
  'admin_notify_user(7-arg): শুধু স্পেসের title → TITLE_REQUIRED'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND left(r->>'id', 6) = 'NOTIF_'
   FROM (SELECT public.admin_notify_user('11111111-1111-1111-1111-111111111111', '  NU Title  ', '  NU msg  ', NULL, 'tgt-1', 'NU_PROB', 'SOLVER') AS r) s),
  'admin_notify_user(7-arg): happy path → result OK + id "NOTIF_…" প্রিফিক্সে'
);
SELECT results_eq(
  $$ SELECT title, message, target_type, target_id, related_problem_id, role
       FROM public.notifications WHERE related_problem_id = 'NU_PROB' $$,
  $$ VALUES ('NU Title'::text, 'NU msg'::text, 'general'::text, 'tgt-1'::text, 'NU_PROB'::text, 'SOLVER'::text) $$,
  'admin_notify_user(7-arg): title/message trim হয়, NULL target_type → "general", p_role সারিতে বসে'
);
SELECT is(
  (SELECT count(*)::int FROM pg_proc WHERE proname = 'admin_notify_user' AND pronamespace = 'public'::regnamespace),
  1,
  'admin_notify_user: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৬-আর্গ overload DROP হওয়ায় এখন public schema-য় ঠিক ১টা overload (৭-আর্গ)'
);
SELECT ok(
  (SELECT r->>'result' = 'OK'
   FROM (SELECT public.admin_notify_user('11111111-1111-1111-1111-111111111111', 'T2', 'M2', 'general', NULL, NULL) AS r) s),
  'admin_notify_user: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৬-আর্গ overload DROP হওয়ায় ৬-আর্গ কল আর ambiguous না, বরং p_role DEFAULT ('''') দিয়ে ৭-আর্গ ফাংশনেই resolve হয়ে সফল হয়'
);
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.notifications
    WHERE user_id = '11111111-1111-1111-1111-111111111111' AND title = 'T2' AND role = ''
  ),
  'admin_notify_user: ৬-আর্গ কলে p_role বাদ দিলে DEFAULT '''' (ফাঁকা স্ট্রিং) সারিতে বসে'
);

----------------------------------------------------------------------
-- admin_broadcast_notification
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_broadcast_notification('ALL', 'BCAST_X', 'm') $$,
  'P0001', 'ADMIN_ONLY',
  'admin_broadcast_notification: non-admin কল করলে ADMIN_ONLY'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_broadcast_notification('ALL', '   ', 'm') $$,
  'P0001', 'TITLE_REQUIRED',
  'admin_broadcast_notification: শুধু স্পেসের title → TITLE_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.admin_broadcast_notification('ADMIN', 'BCAST_X', 'm') $$,
  'P0001', 'INVALID_TARGET_ROLE',
  'admin_broadcast_notification: ALL/USER/SOLVER-এর বাইরের role (ADMIN) → INVALID_TARGET_ROLE'
);
SELECT ok(
  (SELECT r->>'result' = 'OK'
      AND (r->>'count')::int = (SELECT count(*) FROM public.users WHERE role = 'SOLVER')
      AND (r->>'count')::int > 0
   FROM (SELECT public.admin_broadcast_notification('SOLVER', 'BCAST_SOLVER', 'msg', 'announcement', '2031-01-01 00:00:00+00'::timestamptz) AS r) s),
  'admin_broadcast_notification(SOLVER): result OK, count = users.role=SOLVER-এর সংখ্যা (>0)'
);
SELECT ok(
  (SELECT count(*) = (SELECT count(*) FROM public.users WHERE role = 'SOLVER')
      AND bool_and(scheduled_for = '2031-01-01 00:00:00+00'::timestamptz AND target_type = 'announcement' AND message = 'msg')
   FROM public.notifications WHERE title = 'BCAST_SOLVER'),
  'admin_broadcast_notification(SOLVER): প্রতিটা SOLVER-এর জন্য ঠিক ১টা সারি; scheduled_for/target_type/message ঠিকভাবে বসেছে'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications n JOIN public.users u ON u.id = n.user_id
    WHERE n.title = 'BCAST_SOLVER' AND u.role <> 'SOLVER'),
  0,
  'admin_broadcast_notification(SOLVER): non-SOLVER (CLIENT/ADMIN/USER) কেউ নোটিফিকেশন পায়নি'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'count')::int = (SELECT count(*) FROM public.users)
   FROM (SELECT public.admin_broadcast_notification(NULL, 'BCAST_ALL', 'msg2') AS r) s),
  'admin_broadcast_notification: p_target_role NULL → ALL ধরা হয়, count = সব users'
);
SELECT ok(
  (SELECT count(*) = (SELECT count(*) FROM public.users)
      AND bool_and(target_type = 'general' AND scheduled_for IS NULL)
   FROM public.notifications WHERE title = 'BCAST_ALL'),
  'admin_broadcast_notification(ALL): NULL target_type → "general", NULL scheduled_for সারিতে NULL থাকে'
);
SELECT ok(
  (SELECT r->>'result' = 'OK'
      AND (r->>'count')::int = (SELECT count(*) FROM public.users WHERE role = 'USER')
      AND (r->>'count')::int > 0
   FROM (SELECT public.admin_broadcast_notification('user', 'BCAST_USER', 'm') AS r) s),
  'admin_broadcast_notification: ছোট হাতের "user" upper() হয়ে বৈধ; count = users.role=USER (fixture-এ ১টা USER-role ইউজার)'
);

----------------------------------------------------------------------
-- log_admin_action (৪-আর্গ ও ৫-আর্গ overload)
----------------------------------------------------------------------
SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.log_admin_action('LOGTEST_X', 'a', 'b', 'c', 'd') $$,
  'P0001', 'AUTH_REQUIRED',
  'log_admin_action(5-arg): লগইন ছাড়া (auth.uid() NULL) কল করলে AUTH_REQUIRED'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.log_admin_action('   ', 'a', 'b', 'c', 'd') $$,
  'P0001', 'ACTION_TYPE_REQUIRED',
  'log_admin_action(5-arg): শুধু স্পেসের action_type → ACTION_TYPE_REQUIRED'
);
SELECT is(
  (public.log_admin_action('LOGTEST_C', 'x', 'y', 'z', ''))->>'result',
  'OK',
  'log_admin_action(5-arg): DOCUMENTED CURRENT BEHAVIOUR — non-admin (CLIENT) ইউজারও admin_audit_logs-এ সারি ঢোকাতে পারে (শুধু auth.uid() IS NOT NULL চেক)'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.log_admin_action('LOGTEST_A', 'tgt-1', 'Target One', 'details text', 'ADMIN'))->>'result',
  'OK',
  'log_admin_action(5-arg): admin কল করলে result OK'
);
SELECT results_eq(
  $$ SELECT action_type, target_id, target_name, details, role
       FROM public.admin_audit_logs WHERE action_type = 'LOGTEST_A' $$,
  $$ VALUES ('LOGTEST_A'::text, 'tgt-1'::text, 'Target One'::text, 'details text'::text, 'ADMIN'::text) $$,
  'log_admin_action(5-arg): সব ফিল্ড (p_role সহ) admin_audit_logs সারিতে হুবহু বসে'
);
SELECT ok(
  (SELECT id ~ '^LOG_[0-9]+_[0-9a-f]{6}$' FROM public.admin_audit_logs WHERE action_type = 'LOGTEST_A'),
  'log_admin_action(5-arg): id ফরম্যাট "LOG_<epoch>_<৬-hex>"'
);
SELECT is(
  (public.log_admin_action('LOGTEST_N', NULL, NULL, NULL, NULL))->>'result',
  'OK',
  'log_admin_action(5-arg): NULL target_id/target_name/details/role দিলেও result OK'
);
SELECT ok(
  (SELECT count(*) = 1 AND bool_and(target_id = '' AND target_name = '' AND details = '' AND role = '')
     FROM public.admin_audit_logs WHERE action_type = 'LOGTEST_N'),
  'log_admin_action(5-arg): NULL target_id/target_name/details/role সারিতে ফাঁকা স্ট্রিং ('''') হয়ে বসে'
);
SELECT is(
  (SELECT count(*)::int FROM pg_proc WHERE proname = 'log_admin_action' AND pronamespace = 'public'::regnamespace),
  1,
  'log_admin_action: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৪-আর্গ overload DROP হওয়ায় এখন public schema-য় ঠিক ১টা overload (৫-আর্গ)'
);
SELECT is(
  (public.log_admin_action('LOGTEST_AMB', 'a', 'b', 'c'))->>'result',
  'OK',
  'log_admin_action: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৪-আর্গ overload DROP হওয়ায় ৪-আর্গ কল আর ambiguous না, বরং p_role DEFAULT ('''') দিয়ে ৫-আর্গ ফাংশনেই resolve হয়ে সফল হয়'
);
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.admin_audit_logs
    WHERE action_type = 'LOGTEST_AMB' AND target_id = 'a' AND target_name = 'b' AND details = 'c' AND role = ''
  ),
  'log_admin_action: ৪-আর্গ কলে p_role বাদ দিলে DEFAULT '''' (ফাঁকা স্ট্রিং) সারিতে বসে'
);

----------------------------------------------------------------------
-- admin_reassign_solver / admin_update_problem_budget / admin_update_problem_status
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, last_activity_at) VALUES
  ('RS_P1', '11111111-1111-1111-1111-111111111111', 'RS P1', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Old Name', '2020-01-01 00:00:00+00');
INSERT INTO public.problems (id, user_id, title, status, min_budget, max_budget, last_activity_at) VALUES
  ('BUD_P1', '11111111-1111-1111-1111-111111111111', 'BUD P1', 'OPEN', 100, 200, '2020-01-01 00:00:00+00');
INSERT INTO public.problems (id, user_id, title, status, last_activity_at) VALUES
  ('ST_P1', '11111111-1111-1111-1111-111111111111', 'ST P1', 'OPEN', '2020-01-01 00:00:00+00');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_reassign_solver('RS_P1', '33333333-3333-3333-3333-333333333333', 'New Solver') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_reassign_solver: non-admin কল করলে NOT_AUTHORIZED'
);
SELECT throws_ok(
  $$ SELECT public.admin_update_problem_budget('BUD_P1', 300, 600) $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_update_problem_budget: non-admin কল করলে NOT_AUTHORIZED'
);
SELECT throws_ok(
  $$ SELECT public.admin_update_problem_status('ST_P1', 'IN_PROGRESS') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_update_problem_status: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_reassign_solver('NOPE', '33333333-3333-3333-3333-333333333333', 'X') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'admin_reassign_solver: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.admin_update_problem_budget('NOPE', 1, 2) $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'admin_update_problem_budget: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.admin_update_problem_status('NOPE', 'OPEN') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'admin_update_problem_status: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);

SELECT is(
  (public.admin_reassign_solver('RS_P1', '33333333-3333-3333-3333-333333333333', 'New Solver'))->>'result',
  'OK',
  'admin_reassign_solver: happy path → result OK'
);
SELECT results_eq(
  $$ SELECT accepted_solver_id, accepted_solver_name, status FROM public.problems WHERE id = 'RS_P1' $$,
  $$ VALUES ('33333333-3333-3333-3333-333333333333'::uuid, 'New Solver'::text, 'IN_PROGRESS'::text) $$,
  'admin_reassign_solver: accepted_solver_id/name বদলায়, status অপরিবর্তিত'
);
SELECT ok(
  (SELECT last_activity_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'RS_P1'),
  'admin_reassign_solver: last_activity_at এগোয়'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE related_problem_id = 'RS_P1'),
  0,
  'admin_reassign_solver: ইচ্ছাকৃতভাবে কোনো notification তৈরি হয় না (Kotlin-সাইডে আলাদা dual-write)'
);

SELECT is(
  (public.admin_update_problem_budget('BUD_P1', 300, 600))->>'result',
  'OK',
  'admin_update_problem_budget: happy path → result OK'
);
SELECT results_eq(
  $$ SELECT min_budget, max_budget FROM public.problems WHERE id = 'BUD_P1' $$,
  $$ VALUES (300::numeric(12,2), 600::numeric(12,2)) $$,
  'admin_update_problem_budget: min/max budget বদলায়'
);
SELECT ok(
  (SELECT last_activity_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'BUD_P1'),
  'admin_update_problem_budget: last_activity_at এগোয়'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE related_problem_id = 'BUD_P1'),
  0,
  'admin_update_problem_budget: ইচ্ছাকৃতভাবে কোনো notification তৈরি হয় না'
);

SELECT is(
  (public.admin_update_problem_status('ST_P1', 'IN_PROGRESS'))->>'result',
  'OK',
  'admin_update_problem_status: happy path → result OK'
);
SELECT is(
  (SELECT status FROM public.problems WHERE id = 'ST_P1'),
  'IN_PROGRESS',
  'admin_update_problem_status: problems.status বদলায়'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications
    WHERE related_problem_id = 'ST_P1' AND title = 'সমস্যার স্ট্যাটাস আপডেট'
      AND user_id = '11111111-1111-1111-1111-111111111111' AND role = 'USER'
      AND position('IN_PROGRESS' in message) > 0),
  1,
  'admin_update_problem_status: owner-কে role=USER notification যায়, message-এ নতুন status থাকে'
);
SELECT is(
  (public.admin_update_problem_status('ST_P1', 'NOT_A_REAL_STATUS'))->>'result',
  'OK',
  'admin_update_problem_status: অচেনা status স্ট্রিং দিলেও result OK'
);
SELECT is(
  (SELECT status FROM public.problems WHERE id = 'ST_P1'),
  'NOT_A_REAL_STATUS',
  'admin_update_problem_status: DOCUMENTED CURRENT BEHAVIOUR — যেকোনো স্ট্রিং status হিসেবে গৃহীত (কোনো validation নেই)'
);

----------------------------------------------------------------------
-- admin_remove_category_from_solvers
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, solver_categories) VALUES
  ('e4000001-0000-0000-0000-000000000001', 'SOLVER', 'Cat U1', '01760000001', true, true, 'cat1,cat2,cat3'),
  ('e4000002-0000-0000-0000-000000000002', 'SOLVER', 'Cat U2', '01760000002', true, true, 'cat1'),
  ('e4000003-0000-0000-0000-000000000003', 'SOLVER', 'Cat U3', '01760000003', true, true, ' cat1 , cat2 '),
  ('e4000004-0000-0000-0000-000000000004', 'SOLVER', 'Cat U4', '01760000004', true, true, ''),
  ('e4000005-0000-0000-0000-000000000005', 'SOLVER', 'Cat U5', '01760000005', true, true, 'cat10,cat1'),
  ('e4000006-0000-0000-0000-000000000006', 'SOLVER', 'Cat U6', '01760000006', true, true, 'cat2,cat3');

SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.admin_remove_category_from_solvers('cat1') $$,
  'P0001', 'AUTH_REQUIRED',
  'admin_remove_category_from_solvers: লগইন ছাড়া → AUTH_REQUIRED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_remove_category_from_solvers('cat1') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_remove_category_from_solvers: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'rows_affected')::int = 4
   FROM (SELECT public.admin_remove_category_from_solvers('cat1') AS r) s),
  'admin_remove_category_from_solvers: result OK, rows_affected = ৪ (cat1 থাকা U1,U2,U3,U5; ফাঁকা/অমিল U4,U6 বাদ)'
);
SELECT set_eq(
  $$ SELECT id, solver_categories FROM public.users WHERE id::text LIKE 'e400000%' $$,
  $$ VALUES ('e4000001-0000-0000-0000-000000000001'::uuid, 'cat2,cat3'::text),
            ('e4000002-0000-0000-0000-000000000002'::uuid, ''::text),
            ('e4000003-0000-0000-0000-000000000003'::uuid, 'cat2'::text),
            ('e4000004-0000-0000-0000-000000000004'::uuid, ''::text),
            ('e4000005-0000-0000-0000-000000000005'::uuid, 'cat10'::text),
            ('e4000006-0000-0000-0000-000000000006'::uuid, 'cat2,cat3'::text) $$,
  'admin_remove_category_from_solvers: শুধু হুবহু-মিল "cat1" সরে (cat10 অক্ষত), প্রতিটা আইটেম trim হয়, শেষ কিছু না থাকলে ''''-এ নামে'
);
SELECT ok(
  (SELECT (r->>'rows_affected')::int = 0
   FROM (SELECT public.admin_remove_category_from_solvers('cat1') AS r) s),
  'admin_remove_category_from_solvers: দ্বিতীয়বার চালালে rows_affected = 0 (idempotent)'
);

----------------------------------------------------------------------
-- admin_soft_delete_user
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, updated_at) VALUES
  ('e5000001-0000-0000-0000-000000000001', 'CLIENT', 'SD Target', '01770000001', true, '2020-01-01 00:00:00+00'),
  ('e5000002-0000-0000-0000-000000000002', 'CLIENT', 'SD Other',  '01770000002', true, '2020-01-01 00:00:00+00');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_soft_delete_user('e5000001-0000-0000-0000-000000000001') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_soft_delete_user: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.admin_soft_delete_user('e5000001-0000-0000-0000-000000000001'))->>'result',
  'OK',
  'admin_soft_delete_user: happy path → result OK'
);
SELECT results_eq(
  $$ SELECT id, is_deleted FROM public.users WHERE id::text LIKE 'e500000%' ORDER BY id $$,
  $$ VALUES ('e5000001-0000-0000-0000-000000000001'::uuid, true),
            ('e5000002-0000-0000-0000-000000000002'::uuid, false) $$,
  'admin_soft_delete_user: শুধু টার্গেটের is_deleted=true; সারি মোছা হয় না (hard delete না), অন্য ইউজার অক্ষত'
);
SELECT ok(
  (SELECT updated_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.users WHERE id = 'e5000001-0000-0000-0000-000000000001'),
  'admin_soft_delete_user: updated_at এগোয়'
);
SELECT is(
  (public.admin_soft_delete_user('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'))->>'result',
  'USER_NOT_FOUND',
  'admin_soft_delete_user: অস্তিত্বহীন id → result USER_NOT_FOUND (exception না)'
);
SELECT is(
  (public.admin_soft_delete_user('e5000001-0000-0000-0000-000000000001'))->>'result',
  'OK',
  'admin_soft_delete_user: আগে থেকেই deleted ইউজারে আবার চালালে OK (idempotent)'
);

----------------------------------------------------------------------
-- admin_update_withdrawal_trx_id
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.withdrawals (id, solver_id, amount, status, trx_id) VALUES
  ('WTX_1', '22222222-2222-2222-2222-222222222222', 100, 'COMPLETED', 'OLD_TRX'),
  ('WTX_2', '22222222-2222-2222-2222-222222222222',  50, 'PENDING',   NULL);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_update_withdrawal_trx_id('WTX_1', 'NEW_TRX') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_update_withdrawal_trx_id: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.admin_update_withdrawal_trx_id('WTX_1', 'NEW_TRX'))->>'result',
  'OK',
  'admin_update_withdrawal_trx_id: happy path → result OK'
);
SELECT ok(
  (SELECT trx_id = 'NEW_TRX' AND status = 'COMPLETED' AND amount = 100 FROM public.withdrawals WHERE id = 'WTX_1'),
  'admin_update_withdrawal_trx_id: trx_id বদলায়; status/amount অপরিবর্তিত (money-movement নেই)'
);
SELECT is(
  (public.admin_update_withdrawal_trx_id('NOPE', 'X'))->>'result',
  'WITHDRAWAL_NOT_FOUND',
  'admin_update_withdrawal_trx_id: অস্তিত্বহীন id → result WITHDRAWAL_NOT_FOUND'
);
SELECT is(
  (public.admin_update_withdrawal_trx_id('WTX_2', 'PEND_TRX'))->>'result',
  'OK',
  'admin_update_withdrawal_trx_id: PENDING withdrawal-এও result OK'
);
SELECT ok(
  (SELECT trx_id = 'PEND_TRX' AND status = 'PENDING' FROM public.withdrawals WHERE id = 'WTX_2'),
  'admin_update_withdrawal_trx_id: DOCUMENTED CURRENT BEHAVIOUR — PENDING withdrawal-এও trx_id বসে, status PENDING-ই থাকে (status চেক নেই)'
);
SELECT is(
  (public.admin_update_withdrawal_trx_id('WTX_1', NULL))->>'result',
  'OK',
  'admin_update_withdrawal_trx_id: NULL trx_id দিলেও result OK'
);
SELECT ok(
  (SELECT trx_id IS NULL FROM public.withdrawals WHERE id = 'WTX_1'),
  'admin_update_withdrawal_trx_id: DOCUMENTED CURRENT BEHAVIOUR — NULL trx_id গৃহীত, আগের trx_id মুছে যায় (validation নেই)'
);

----------------------------------------------------------------------
-- admin_reset_free_job_quota
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, free_jobs_used_this_month, free_jobs_month_key) VALUES
  ('e5100001-0000-0000-0000-000000000001', 'SOLVER', 'Quota S1',     '01780000001', true, true,  7, '2020-01'),
  ('e5100002-0000-0000-0000-000000000002', 'CLIENT', 'Quota Client', '01780000002', true, false, 3, '2020-01'),
  ('e5100003-0000-0000-0000-000000000003', 'SOLVER', 'Quota S3',     '01780000003', true, true,  9, '2020-01');

SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.admin_reset_free_job_quota('e5100001-0000-0000-0000-000000000001') $$,
  'P0001', 'AUTH_REQUIRED',
  'admin_reset_free_job_quota: লগইন ছাড়া → AUTH_REQUIRED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_reset_free_job_quota('e5100001-0000-0000-0000-000000000001') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_reset_free_job_quota: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_reset_free_job_quota('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa') $$,
  'P0001', 'SOLVER_NOT_FOUND',
  'admin_reset_free_job_quota: অস্তিত্বহীন id → SOLVER_NOT_FOUND (exception)'
);
SELECT ok(
  (SELECT r->>'result' = 'OK'
      AND r->>'month_key' = to_char(now(), 'YYYY-MM')
      AND r->>'solver_id' = 'e5100001-0000-0000-0000-000000000001'
   FROM (SELECT public.admin_reset_free_job_quota('e5100001-0000-0000-0000-000000000001') AS r) s),
  'admin_reset_free_job_quota: happy path → OK + বর্তমান month_key + solver_id echo'
);
SELECT ok(
  (SELECT free_jobs_used_this_month = 0 AND free_jobs_month_key = to_char(now(), 'YYYY-MM')
     FROM public.users WHERE id = 'e5100001-0000-0000-0000-000000000001')
  AND (SELECT free_jobs_used_this_month = 9 AND free_jobs_month_key = '2020-01'
         FROM public.users WHERE id = 'e5100003-0000-0000-0000-000000000003'),
  'admin_reset_free_job_quota: টার্গেটের used=0 ও month_key বর্তমান মাস; অন্য solver-এর (used=9) অক্ষত'
);
SELECT is(
  (public.admin_reset_free_job_quota('e5100002-0000-0000-0000-000000000002'))->>'result',
  'OK',
  'admin_reset_free_job_quota: non-solver (CLIENT) id-তেও result OK'
);
SELECT ok(
  (SELECT free_jobs_used_this_month = 0 FROM public.users WHERE id = 'e5100002-0000-0000-0000-000000000002'),
  'admin_reset_free_job_quota: DOCUMENTED CURRENT BEHAVIOUR — non-solver (CLIENT, has_solver_role=false) id-তেও reset চলে'
);

----------------------------------------------------------------------
-- admin_bulk_reset_free_job_quota
----------------------------------------------------------------------
RESET ROLE;
UPDATE public.users SET free_jobs_used_this_month = 5, free_jobs_month_key = '2020-01'
 WHERE id IN ('e5100001-0000-0000-0000-000000000001', 'e5100002-0000-0000-0000-000000000002', 'e5100003-0000-0000-0000-000000000003');

SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.admin_bulk_reset_free_job_quota('2031-07') $$,
  'P0001', 'AUTH_REQUIRED',
  'admin_bulk_reset_free_job_quota: লগইন ছাড়া → AUTH_REQUIRED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_bulk_reset_free_job_quota('2031-07') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_bulk_reset_free_job_quota: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'month_key' = '2031-07'
      AND (r->>'rows_affected')::int = (SELECT count(*) FROM public.users)
   FROM (SELECT public.admin_bulk_reset_free_job_quota('2031-07') AS r) s),
  'admin_bulk_reset_free_job_quota: result OK, month_key echo, rows_affected = সব users'
);
SELECT is(
  (SELECT count(*)::int FROM public.users
    WHERE free_jobs_used_this_month <> 0 OR free_jobs_month_key IS DISTINCT FROM '2031-07'),
  0,
  'admin_bulk_reset_free_job_quota: প্রতিটা ইউজারের used=0 ও month_key=2031-07 (solver/non-solver নির্বিশেষে)'
);
SELECT ok(
  (SELECT r->>'month_key' = to_char(now(), 'YYYY-MM')
   FROM (SELECT public.admin_bulk_reset_free_job_quota(NULL) AS r) s),
  'admin_bulk_reset_free_job_quota: p_month_key NULL → বর্তমান মাস (YYYY-MM) ডিফল্ট'
);

----------------------------------------------------------------------
-- admin_reset_miss_cycle
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role, cycle_job_count, cycle_miss_count) VALUES
  ('e5200001-0000-0000-0000-000000000001', 'SOLVER', 'Miss S1', '01790000001', true, true, 12, 3),
  ('e5200002-0000-0000-0000-000000000002', 'SOLVER', 'Miss S2', '01790000002', true, true,  8, 2);

SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.admin_reset_miss_cycle('e5200001-0000-0000-0000-000000000001') $$,
  'P0001', 'AUTH_REQUIRED',
  'admin_reset_miss_cycle: লগইন ছাড়া → AUTH_REQUIRED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_reset_miss_cycle('e5200001-0000-0000-0000-000000000001') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_reset_miss_cycle: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_reset_miss_cycle('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa') $$,
  'P0001', 'SOLVER_NOT_FOUND',
  'admin_reset_miss_cycle: অস্তিত্বহীন id → SOLVER_NOT_FOUND'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'solver_id' = 'e5200001-0000-0000-0000-000000000001'
   FROM (SELECT public.admin_reset_miss_cycle('e5200001-0000-0000-0000-000000000001') AS r) s),
  'admin_reset_miss_cycle: happy path → OK + solver_id echo'
);
SELECT results_eq(
  $$ SELECT cycle_job_count, cycle_miss_count FROM public.users WHERE id = 'e5200001-0000-0000-0000-000000000001' $$,
  $$ VALUES (0, 0) $$,
  'admin_reset_miss_cycle: cycle_job_count ও cycle_miss_count দুটোই 0'
);
SELECT results_eq(
  $$ SELECT cycle_job_count, cycle_miss_count FROM public.users WHERE id = 'e5200002-0000-0000-0000-000000000002' $$,
  $$ VALUES (8, 2) $$,
  'admin_reset_miss_cycle: অন্য solver-এর counter অক্ষত'
);

----------------------------------------------------------------------
-- admin_reconcile_escrow_states
-- ৯টা HELD escrow (created_at স্পষ্টভাবে সাজানো — নাহলে একই transaction-এর now() টাই হয়ে
-- `order by created_at` অনির্দিষ্ট হতো):
--   E_RC1 case1 (আগে থেকে REFUND tx আছে)     E_RC2,E_RC3,E_RC6..E_RC9 case2 (CANCELLED / unassigned OPEN)
--   E_RC4 case3 (COMPLETED problem)           E_RC5 কোনো case না (IN_PROGRESS)
-- live ক্যাপ ৫ repair: RC2,RC3,RC4,RC6,RC7 → RC8,RC9 পরের রানে।
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, balance, balance_user, balance_solver, has_user_role, has_solver_role) VALUES
  ('e6000001-0000-0000-0000-000000000001', 'CLIENT', 'RC Owner',  '01800000001', 0, 0, 0, true, false),
  ('e6000002-0000-0000-0000-000000000002', 'SOLVER', 'RC Solver', '01800000002', 0, 0, 0, true, true);
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, applied_commission_rate) VALUES
  ('P_RC1', 'e6000001-0000-0000-0000-000000000001', 'RC1', 'CANCELLED',   NULL, NULL),
  ('P_RC2', 'e6000001-0000-0000-0000-000000000001', 'RC2', 'CANCELLED',   NULL, NULL),
  ('P_RC3', 'e6000001-0000-0000-0000-000000000001', 'RC3', 'OPEN',        NULL, NULL),
  ('P_RC4', 'e6000001-0000-0000-0000-000000000001', 'RC4', 'COMPLETED',   'e6000002-0000-0000-0000-000000000002', 10),
  ('P_RC5', 'e6000001-0000-0000-0000-000000000001', 'RC5', 'IN_PROGRESS', 'e6000002-0000-0000-0000-000000000002', NULL),
  ('P_RC6', 'e6000001-0000-0000-0000-000000000001', 'RC6', 'CANCELLED',   NULL, NULL),
  ('P_RC7', 'e6000001-0000-0000-0000-000000000001', 'RC7', 'CANCELLED',   NULL, NULL),
  ('P_RC8', 'e6000001-0000-0000-0000-000000000001', 'RC8', 'CANCELLED',   NULL, NULL),
  ('P_RC9', 'e6000001-0000-0000-0000-000000000001', 'RC9', 'CANCELLED',   NULL, NULL);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, created_at) VALUES
  ('E_RC1', 'P_RC1', 'RC1', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002', 500, 0, 'HELD', '2026-01-01 01:00:00+00'),
  ('E_RC2', 'P_RC2', 'RC2', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002', 200, 0, 'HELD', '2026-01-01 02:00:00+00'),
  ('E_RC3', 'P_RC3', 'RC3', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002', 100, 0, 'HELD', '2026-01-01 03:00:00+00'),
  ('E_RC4', 'P_RC4', 'RC4', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002', 400, 0, 'HELD', '2026-01-01 04:00:00+00'),
  ('E_RC5', 'P_RC5', 'RC5', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002', 999, 0, 'HELD', '2026-01-01 05:00:00+00'),
  ('E_RC6', 'P_RC6', 'RC6', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002',  50, 0, 'HELD', '2026-01-01 06:00:00+00'),
  ('E_RC7', 'P_RC7', 'RC7', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002',  50, 0, 'HELD', '2026-01-01 07:00:00+00'),
  ('E_RC8', 'P_RC8', 'RC8', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002',  50, 0, 'HELD', '2026-01-01 08:00:00+00'),
  ('E_RC9', 'P_RC9', 'RC9', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002',  50, 0, 'HELD', '2026-01-01 09:00:00+00');
INSERT INTO public.transactions (id, problem_id, user_id, gross_amount, net_amount, type, escrow_id) VALUES
  ('TRX_RC1_PRE', 'P_RC1', 'e6000001-0000-0000-0000-000000000001', 500, 500, 'REFUND', 'E_RC1');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_reconcile_escrow_states() $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_reconcile_escrow_states: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT (r->>'dry_run')::boolean AND (r->>'case1_synced')::int = 1 AND (r->>'case2_repaired')::int = 6
      AND (r->>'case3_repaired')::int = 1 AND jsonb_array_length(r->'items') = 8
   FROM (SELECT public.admin_reconcile_escrow_states() AS r) s),
  'admin_reconcile_escrow_states: ডিফল্ট dry-run — case1=১, case2=৬, case3=১, items=৮ (⚠️ dry-run-এ ৫-repair ক্যাপ প্রযোজ্য না, সব candidate গোনা হয়)'
);
SELECT ok(
  (SELECT count(*) = 9 FROM public.escrows WHERE status = 'HELD' AND id LIKE 'E\_RC%')
  AND (SELECT balance_user = 0 AND balance = 0 FROM public.users WHERE id = 'e6000001-0000-0000-0000-000000000001')
  AND (SELECT count(*) = 0 FROM public.admin_audit_logs WHERE action_type = 'ADMIN_RECONCILE_ESCROW_STATES'),
  'admin_reconcile_escrow_states: dry-run কোনো escrow/balance বদলায় না, audit log-ও লেখে না'
);
SELECT ok(
  (SELECT NOT (r->>'dry_run')::boolean AND (r->>'case1_synced')::int = 1 AND (r->>'case2_repaired')::int = 4
      AND (r->>'case3_repaired')::int = 1
   FROM (SELECT public.admin_reconcile_escrow_states(false) AS r) s),
  'admin_reconcile_escrow_states: live রান ১ — ক্যাপ ৫ repair: case1=১ (repair না), case2=৪ (RC2,RC3,RC6,RC7), case3=১ (RC4)'
);
SELECT set_eq(
  $$ SELECT id, status FROM public.escrows WHERE id LIKE 'E\_RC%' $$,
  $$ VALUES ('E_RC1'::text, 'REFUNDED'::text), ('E_RC2', 'REFUNDED'), ('E_RC3', 'REFUNDED'),
            ('E_RC4', 'RELEASED'), ('E_RC5', 'HELD'), ('E_RC6', 'REFUNDED'), ('E_RC7', 'REFUNDED'),
            ('E_RC8', 'HELD'), ('E_RC9', 'HELD') $$,
  'admin_reconcile_escrow_states: live রান ১-এর পর escrow status — RC8/RC9 ক্যাপের জন্য HELD, RC5 কোনো case-এ পড়ে না তাই HELD'
);
SELECT ok(
  (SELECT balance_user = 400 AND balance = 400 FROM public.users WHERE id = 'e6000001-0000-0000-0000-000000000001'),
  'admin_reconcile_escrow_states: owner-এর balance_user ও balance ঠিক +৪০০ (RC2 ২০০ + RC3 ১০০ + RC6 ৫০ + RC7 ৫০); case1 কোনো টাকা নাড়ায়নি'
);
SELECT ok(
  (SELECT balance_solver = 360 AND balance = 360 FROM public.users WHERE id = 'e6000002-0000-0000-0000-000000000002'),
  'admin_reconcile_escrow_states: case3 release_escrow — solver-এর balance_solver +৩৬০ (৪০০ − ১০% commission)'
);
SELECT ok(
  (SELECT count(*) = 1 AND bool_and(type = 'REFUND' AND refund_type = 'SOLVER_CANCEL' AND net_amount = 200 AND escrow_id = 'E_RC2')
     FROM public.transactions WHERE id = 'TRX_REFUND_E_RC2')
  AND (SELECT count(*) = 0 FROM public.transactions WHERE id = 'TRX_REFUND_E_RC1'),
  'admin_reconcile_escrow_states: case2 refund tx (SOLVER_CANCEL, ২০০) তৈরি; case1 escrow-এর জন্য নতুন refund tx তৈরি হয়নি'
);
SELECT is(
  (SELECT details FROM public.admin_audit_logs WHERE action_type = 'ADMIN_RECONCILE_ESCROW_STATES'),
  'case1_synced=1, case2_repaired=4, case3_repaired=1',
  'admin_reconcile_escrow_states: live রানে audit log ঠিক সারাংশসহ লেখা হয়'
);
SELECT ok(
  (SELECT (r->>'case1_synced')::int = 0 AND (r->>'case2_repaired')::int = 2 AND (r->>'case3_repaired')::int = 0
   FROM (SELECT public.admin_reconcile_escrow_states(false) AS r) s),
  'admin_reconcile_escrow_states: live রান ২ — বাকি RC8, RC9 repair (case2=২), আর কিছু না'
);
SELECT ok(
  (SELECT count(*) = 2 FROM public.escrows WHERE id IN ('E_RC8', 'E_RC9') AND status = 'REFUNDED')
  AND (SELECT balance_user = 500 FROM public.users WHERE id = 'e6000001-0000-0000-0000-000000000001')
  AND (SELECT status = 'HELD' FROM public.escrows WHERE id = 'E_RC5'),
  'admin_reconcile_escrow_states: রান ২-এর পর RC8/RC9 REFUNDED, owner balance_user ৫০০, RC5 তখনো HELD'
);
SELECT is(
  (SELECT count(*)::int FROM public.admin_audit_logs WHERE action_type = 'ADMIN_RECONCILE_ESCROW_STATES'),
  2,
  'admin_reconcile_escrow_states: প্রতিটা live রানে ১টা করে audit log (মোট ২)'
);

----------------------------------------------------------------------
-- admin_reconcile_escrow_states — [Somadhan Bug-Fix Step 7.8] per-escrow exception handling
-- E_RC10 (case2, সুস্থ owner) ও E_RC12 (case3, সুস্থ solver) সফল হওয়া উচিত;
-- E_RC11 (case2, owner-এর has_user_role=false → USER_ROLE_INACTIVE) ও E_RC13 (case3,
-- solver-এর has_solver_role=false → SOLVER_ROLE_INACTIVE) ব্যর্থ হওয়া উচিত — কিন্তু এই ব্যর্থতা
-- যেন পুরো রান rollback না করে, বাকি (RC10/RC12) সফল-ই থাকে (Step 7.8-এর আগে পুরো রানই rollback
-- হয়ে যেত)।
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, balance, balance_user, balance_solver, has_user_role, has_solver_role) VALUES
  ('e6000003-0000-0000-0000-000000000003', 'CLIENT', 'RC Inactive Owner',  '01800000003', 0, 0, 0, false, false),
  ('e6000004-0000-0000-0000-000000000004', 'SOLVER', 'RC Inactive Solver', '01800000004', 0, 0, 0, true, false);
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, applied_commission_rate) VALUES
  ('P_RC10', 'e6000001-0000-0000-0000-000000000001', 'RC10', 'CANCELLED', NULL, NULL),
  ('P_RC11', 'e6000003-0000-0000-0000-000000000003', 'RC11', 'CANCELLED', NULL, NULL),
  ('P_RC12', 'e6000001-0000-0000-0000-000000000001', 'RC12', 'COMPLETED', 'e6000002-0000-0000-0000-000000000002', 10),
  ('P_RC13', 'e6000001-0000-0000-0000-000000000001', 'RC13', 'COMPLETED', 'e6000004-0000-0000-0000-000000000004', 10);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, created_at) VALUES
  ('E_RC10', 'P_RC10', 'RC10', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002',  70, 0, 'HELD', '2026-01-01 10:00:00+00'),
  ('E_RC11', 'P_RC11', 'RC11', 'e6000003-0000-0000-0000-000000000003', 'e6000002-0000-0000-0000-000000000002',  80, 0, 'HELD', '2026-01-01 11:00:00+00'),
  ('E_RC12', 'P_RC12', 'RC12', 'e6000001-0000-0000-0000-000000000001', 'e6000002-0000-0000-0000-000000000002', 400, 0, 'HELD', '2026-01-01 12:00:00+00'),
  ('E_RC13', 'P_RC13', 'RC13', 'e6000001-0000-0000-0000-000000000001', 'e6000004-0000-0000-0000-000000000004', 300, 0, 'HELD', '2026-01-01 13:00:00+00');

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
CREATE TEMP TABLE step7_8_run3_result AS SELECT public.admin_reconcile_escrow_states(false) AS r;
SELECT ok(
  (SELECT NOT (r->>'dry_run')::boolean AND (r->>'case2_repaired')::int = 1 AND (r->>'case2_failed')::int = 1
      AND (r->>'case3_repaired')::int = 1 AND (r->>'case3_failed')::int = 1
   FROM step7_8_run3_result),
  'admin_reconcile_escrow_states (৭.৮): live রান ৩ — RC10/RC12 সফল, RC11/RC13 ব্যর্থ (role নিষ্ক্রিয়), কাউন্ট ঠিক'
);
SELECT set_eq(
  $$ SELECT id, status FROM public.escrows WHERE id IN ('E_RC10','E_RC11','E_RC12','E_RC13','E_RC5') $$,
  $$ VALUES ('E_RC10'::text, 'REFUNDED'::text), ('E_RC11', 'HELD'), ('E_RC12', 'RELEASED'), ('E_RC13', 'HELD'),
            ('E_RC5', 'HELD') $$,
  'admin_reconcile_escrow_states (৭.৮): সফল escrow-গুলো ঠিকভাবে বদলেছে, ব্যর্থগুলো HELD-ই থেকেছে (rollback হয়নি বাকি লুপের)'
);
SELECT ok(
  (SELECT balance_user = 570 AND balance = 570 FROM public.users WHERE id = 'e6000001-0000-0000-0000-000000000001')
  AND (SELECT balance_user = 0 AND balance = 0 FROM public.users WHERE id = 'e6000003-0000-0000-0000-000000000003'),
  'admin_reconcile_escrow_states (৭.৮): RC10 refund owner-কে +৭০ দিয়েছে (৫০০→৫৭০); RC11 ব্যর্থ owner-এর টাকা নাড়ায়নি'
);
SELECT ok(
  (SELECT balance_solver = 720 AND balance = 720 FROM public.users WHERE id = 'e6000002-0000-0000-0000-000000000002')
  AND (SELECT balance_solver = 0 AND balance = 0 FROM public.users WHERE id = 'e6000004-0000-0000-0000-000000000004'),
  'admin_reconcile_escrow_states (৭.৮): RC12 release সুস্থ solver-কে +৩৬০ দিয়েছে (৩৬০→৭২০); RC13 ব্যর্থ inactive solver-এর টাকা নাড়ায়নি'
);
SELECT ok(
  (SELECT count(*) = 1 FROM public.transactions WHERE id = 'TRX_REFUND_E_RC10')
  AND (SELECT count(*) = 1 FROM public.transactions WHERE id = 'TRX_RELEASE_E_RC12')
  AND (SELECT count(*) = 0 FROM public.transactions WHERE id IN ('TRX_REFUND_E_RC11', 'TRX_RELEASE_E_RC13')),
  'admin_reconcile_escrow_states (৭.৮): সফল দুটোর transaction তৈরি হয়েছে, ব্যর্থ দুটোর হয়নি'
);
SELECT ok(
  (SELECT count(*) FROM step7_8_run3_result, jsonb_array_elements(r->'items') item
   WHERE item->>'status' = 'failed' AND item->>'escrow_id' IN ('E_RC11', 'E_RC13') AND item->>'error' is not null) = 2,
  'admin_reconcile_escrow_states (৭.৮): ব্যর্থ escrow দুটোই একই রান-৩ result-এর items-এ status=failed + আসল error message (sqlerrm) সহ রিপোর্টেড'
);
SELECT is(
  (SELECT count(*)::int FROM public.admin_audit_logs WHERE action_type = 'ADMIN_RECONCILE_ESCROW_STATES'),
  3,
  'admin_reconcile_escrow_states (৭.৮): তৃতীয় live রানেও ১টা audit log যোগ হয়েছে (মোট ৩), rollback-জনিত মিসিং না'
);
DROP TABLE step7_8_run3_result;

----------------------------------------------------------------------
-- admin_repair_missing_refunds
-- E_RP1..: REFUNDED/REFUND_PENDING_SYNC অথচ REFUND transaction নেই।
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role) VALUES
  ('e6100001-0000-0000-0000-000000000001', 'CLIENT', 'RP Owner',  '01810000001', 0, 0, true),
  ('e6100002-0000-0000-0000-000000000002', 'SOLVER', 'RP Solver', '01810000002', 0, 0, true);
INSERT INTO public.problems (id, user_id, title, status, dispute_resolution_decision, dispute_split_solver_percent) VALUES
  ('P_RP1',  'e6100001-0000-0000-0000-000000000001', 'RP1',  'CANCELLED',   NULL, NULL),
  ('P_RP2',  'e6100001-0000-0000-0000-000000000001', 'RP2',  'IN_PROGRESS', NULL, NULL),
  ('P_RP3',  'e6100001-0000-0000-0000-000000000001', 'RP3',  'COMPLETED',   'SPLIT_SETTLEMENT', 70),
  ('P_RP4',  'e6100001-0000-0000-0000-000000000001', 'RP4',  'COMPLETED',   'REFUND_TO_USER', NULL),
  ('P_RP5',  'e6100001-0000-0000-0000-000000000001', 'RP5',  'COMPLETED',   NULL, NULL),
  ('P_RP6',  'e6100001-0000-0000-0000-000000000001', 'RP6',  'CANCELLED',   NULL, NULL),
  ('P_RP7',  'e6100001-0000-0000-0000-000000000001', 'RP7',  'CANCELLED',   NULL, NULL),
  ('P_RP8',  'e6100001-0000-0000-0000-000000000001', 'RP8',  'CANCELLED',   NULL, NULL),
  ('P_RP9',  'e6100001-0000-0000-0000-000000000001', 'RP9',  'CANCELLED',   NULL, NULL),
  ('P_RP10', 'e6100001-0000-0000-0000-000000000001', 'RP10', 'COMPLETED',   'CUSTOM_SPLIT', NULL),
  ('P_RP11', 'e6100001-0000-0000-0000-000000000001', 'RP11', 'COMPLETED',   NULL, NULL);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, released_at) VALUES
  ('E_RP1',  'P_RP1',  'RP1',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002', 200,  0, 'REFUNDED', NULL),
  ('E_RP2',  'P_RP2',  'RP2',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002', 100, 50, 'REFUND_PENDING_SYNC', NULL),
  ('E_RP3',  'P_RP3',  'RP3',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002', 1000, 0, 'REFUNDED', NULL),
  ('E_RP4',  'P_RP4',  'RP4',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002',  80,  0, 'REFUNDED', NULL),
  ('E_RP5',  'P_RP5',  'RP5',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002', 300,  0, 'REFUNDED', NULL),
  ('E_RP6',  'P_RP6',  'RP6',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002',  60,  0, 'REFUNDED', NULL),
  ('E_RP7',  'P_RP7',  'RP7',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002',  70,  0, 'REFUNDED', NULL),
  ('E_RP8',  'P_RP8',  'RP8',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002',   0,  0, 'REFUNDED', NULL),
  ('E_RP9',  'P_RP9',  'RP9',  'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002',  90,  0, 'HELD', NULL),
  ('E_RP10', 'P_RP10', 'RP10', 'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002', 200,  0, 'REFUNDED', NULL),
  ('E_RP11', 'P_RP11', 'RP11', 'e6100001-0000-0000-0000-000000000001', 'e6100002-0000-0000-0000-000000000002',  40,  0, 'REFUNDED', '2026-02-01 00:00:00+00');
INSERT INTO public.transactions (id, problem_id, user_id, gross_amount, net_amount, type, escrow_id) VALUES
  ('TRX_RP6_ANY',      'P_RP6', 'e6100001-0000-0000-0000-000000000001', 60, 60, 'REFUND',  'E_RP6'),
  ('TRX_REFUND_E_RP7', 'P_RP7', 'e6100001-0000-0000-0000-000000000001', 70, 70, 'PAYMENT', NULL);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_repair_missing_refunds() $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_repair_missing_refunds: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT (r->>'dry_run')::boolean AND (r->>'missing_count')::int = 6 AND (r->>'repaired_count')::int = 0
      AND (r->>'total_amount_sum')::numeric = 1670
      AND (r->>'scanned_escrows')::int = (SELECT count(*) FROM public.escrows)
   FROM (SELECT public.admin_repair_missing_refunds() AS r) s),
  'admin_repair_missing_refunds: ডিফল্ট dry-run — missing=৬ (RP1,2,3,4,10,11), repaired=০, total_amount_sum=১৬৭০ (gross মোট, refund-amount না), scanned=সব escrow'
);
SELECT set_eq(
  $$ SELECT i->>'escrow_id' AS escrow_id, i->>'refund_type' AS refund_type, (i->>'refund_percentage')::numeric AS refund_percentage
       FROM jsonb_array_elements((SELECT public.admin_repair_missing_refunds(true)->'items')) i $$,
  $$ VALUES ('E_RP1'::text, 'SOLVER_CANCEL'::text, 100::numeric), ('E_RP2', 'SOLVER_CANCEL', 100),
            ('E_RP3', 'SPLIT_REFUND', 30), ('E_RP4', 'DISPUTE_REFUND', 100),
            ('E_RP10', 'SPLIT_REFUND', 50), ('E_RP11', 'SOLVER_CANCEL', 100) $$,
  'admin_repair_missing_refunds: items — refund_type/percentage ঠিক (SPLIT ৭০% solver → ৩০% refund; percent NULL → ৫০ ডিফল্ট; RP5 skip, RP6/RP7 আগে থেকে refund আছে, RP8 শূন্য, RP9 HELD বাদ)'
);
SELECT ok(
  (SELECT count(*) = 0 FROM public.transactions WHERE type = 'REFUND' AND id LIKE 'TRX\_REFUND\_E\_RP%')
  AND (SELECT balance_user = 0 AND balance = 0 FROM public.users WHERE id = 'e6100001-0000-0000-0000-000000000001'),
  'admin_repair_missing_refunds: dry-run কোনো transaction/balance বদলায় না'
);
SELECT ok(
  (SELECT NOT (r->>'dry_run')::boolean AND (r->>'repaired_count')::int = 6 AND (r->>'missing_count')::int = 6
   FROM (SELECT public.admin_repair_missing_refunds(false) AS r) s),
  'admin_repair_missing_refunds: live রান — repaired_count = ৬'
);
SELECT ok(
  (SELECT balance_user = 870 AND balance = 870 FROM public.users WHERE id = 'e6100001-0000-0000-0000-000000000001'),
  'admin_repair_missing_refunds: owner-এর balance ও balance_user ঠিক +৮৭০ (২০০+১৫০+৩০০+৮০+১০০+৪০)'
);
SELECT set_eq(
  $$ SELECT id, refund_type, refund_percentage, net_amount FROM public.transactions
      WHERE type = 'REFUND' AND id LIKE 'TRX\_REFUND\_E\_RP%' $$,
  $$ VALUES ('TRX_REFUND_E_RP1'::text,  'SOLVER_CANCEL'::text, 100::numeric(6,2), 200::numeric(12,2)),
            ('TRX_REFUND_E_RP2',  'SOLVER_CANCEL', 100, 150),
            ('TRX_REFUND_E_RP3',  'SPLIT_REFUND',   30, 300),
            ('TRX_REFUND_E_RP4',  'DISPUTE_REFUND', 100, 80),
            ('TRX_REFUND_E_RP10', 'SPLIT_REFUND',   50, 100),
            ('TRX_REFUND_E_RP11', 'SOLVER_CANCEL', 100, 40) $$,
  'admin_repair_missing_refunds: তৈরি হওয়া REFUND transaction-গুলোর id/refund_type/percentage/net_amount ঠিক'
);
SELECT ok(
  (SELECT count(*) = 1 FROM public.admin_audit_logs
    WHERE action_type = 'ADMIN_REPAIR_MISSING_REFUNDS' AND details LIKE 'repaired=6, total_amount=1670%'),
  'admin_repair_missing_refunds: live রানে audit log "repaired=6, total_amount=1670…" লেখা হয়'
);
SELECT is(
  (SELECT count(*)::int FROM public.transactions WHERE escrow_id IN ('E_RP5', 'E_RP8', 'E_RP9')),
  0,
  'admin_repair_missing_refunds: skip-করা escrow (RP5 genuine payout, RP8 শূন্য, RP9 HELD)-এর জন্য কোনো refund transaction তৈরি হয়নি'
);
SELECT ok(
  (SELECT (r->>'missing_count')::int = 0 AND (r->>'repaired_count')::int = 0
   FROM (SELECT public.admin_repair_missing_refunds(false) AS r) s),
  'admin_repair_missing_refunds: দ্বিতীয় live রান — missing=০, repaired=০ (idempotent, ডবল রিফান্ড নেই)'
);
SELECT is(
  (SELECT count(*)::int FROM public.admin_audit_logs WHERE action_type = 'ADMIN_REPAIR_MISSING_REFUNDS'),
  1,
  'admin_repair_missing_refunds: repaired_count=০ হলে নতুন audit log লেখা হয় না (মোট ১টাই)'
);

----------------------------------------------------------------------
-- admin_cleanup_duplicate_refunds
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role) VALUES
  ('e6200001-0000-0000-0000-000000000001', 'CLIENT', 'CL Owner', '01820000001', 500, 500, true),
  ('e6200002-0000-0000-0000-000000000002', 'CLIENT', 'CL Clamp', '01820000002',  10,  10, true),
  ('e6200003-0000-0000-0000-000000000003', 'SOLVER', 'CL Solver','01820000003',   0,   0, true);
INSERT INTO public.problems (id, user_id, title, status) VALUES
  ('P_CL1', 'e6200001-0000-0000-0000-000000000001', 'CL1', 'CANCELLED'),
  ('P_CL2', 'e6200001-0000-0000-0000-000000000001', 'CL2', 'CANCELLED'),
  ('P_CL3', 'e6200002-0000-0000-0000-000000000002', 'CL3', 'CANCELLED'),
  ('P_CL4', 'e6200001-0000-0000-0000-000000000001', 'CL4', 'CANCELLED'),
  ('P_CLE', 'e6200001-0000-0000-0000-000000000001', 'CLE', 'CANCELLED');
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, status) VALUES
  ('E_CL1',         'P_CL1', 'CL1', 'e6200001-0000-0000-0000-000000000001', 'e6200003-0000-0000-0000-000000000003', 100, 'REFUNDED'),
  ('E_CL2',         'P_CL2', 'CL2', 'e6200001-0000-0000-0000-000000000001', 'e6200003-0000-0000-0000-000000000003',  50, 'REFUNDED'),
  ('E_CL3',         'P_CL3', 'CL3', 'e6200002-0000-0000-0000-000000000002', 'e6200003-0000-0000-0000-000000000003',  30, 'REFUNDED'),
  ('E_CL4',         'P_CL4', 'CL4', 'e6200001-0000-0000-0000-000000000001', 'e6200003-0000-0000-0000-000000000003',  70, 'REFUNDED'),
  ('ESC_FALLBACK1', 'P_CLE', 'CLE', 'e6200001-0000-0000-0000-000000000001', 'e6200003-0000-0000-0000-000000000003',  25, 'REFUNDED');
INSERT INTO public.transactions (id, problem_id, user_id, gross_amount, net_amount, type, escrow_id, "timestamp") VALUES
  ('TRX_CL1_A', 'P_CL1', 'e6200001-0000-0000-0000-000000000001', 100, 100, 'REFUND',  'E_CL1', '2026-02-01 10:00:00+00'),
  ('TRX_CL1_B', 'P_CL1', 'e6200001-0000-0000-0000-000000000001', 100, 100, 'REFUND',  'E_CL1', '2026-02-01 10:05:00+00'),
  ('TRX_CL1_C', 'P_CL1', 'e6200001-0000-0000-0000-000000000001', 100, 100, 'REFUND',  'E_CL1', '2026-02-01 10:10:00+00'),
  ('TRX_CL2_A', 'P_CL2', 'e6200001-0000-0000-0000-000000000001',  50,  50, 'REFUND',  'E_CL2', '2026-02-01 10:00:00+00'),
  ('TRX_CL2_B', 'P_CL2', 'e6200001-0000-0000-0000-000000000001',   0,   0, 'REFUND',  'E_CL2', '2026-02-01 10:05:00+00'),
  ('TRX_CL3_A', 'P_CL3', 'e6200002-0000-0000-0000-000000000002',  30,  30, 'REFUND',  'E_CL3', '2026-02-01 10:00:00+00'),
  ('TRX_CL3_B', 'P_CL3', 'e6200002-0000-0000-0000-000000000002',  30,  30, 'REFUND',  'E_CL3', '2026-02-01 10:05:00+00'),
  ('TRX_CL4_A', 'P_CL4', 'e6200001-0000-0000-0000-000000000001',  70,  70, 'PAYMENT', 'E_CL4', '2026-02-01 10:00:00+00'),
  ('TRX_CL4_B', 'P_CL4', 'e6200001-0000-0000-0000-000000000001',  70,  70, 'PAYMENT', 'E_CL4', '2026-02-01 10:05:00+00'),
  ('TRX_ESC_A', 'P_CLE', 'e6200001-0000-0000-0000-000000000001',  25,  25, 'REFUND',  'ESC_FALLBACK1', '2026-02-01 10:00:00+00'),
  ('TRX_ESC_B', 'P_CLE', 'e6200001-0000-0000-0000-000000000001',  25,  25, 'REFUND',  'ESC_FALLBACK1', '2026-02-01 10:05:00+00');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_cleanup_duplicate_refunds() $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_cleanup_duplicate_refunds: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT (r->>'dry_run')::boolean AND (r->>'removed_count')::int = 0 AND (r->>'skipped_count')::int = 1
      AND jsonb_array_length(r->'items') = 5
      AND (SELECT count(*) = 1 FROM jsonb_array_elements(r->'items') i
            WHERE i->>'escrow_id' = 'ESC_FALLBACK1' AND i->>'action' = 'skipped_fallback_id_needs_manual_review')
   FROM (SELECT public.admin_cleanup_duplicate_refunds() AS r) s),
  'admin_cleanup_duplicate_refunds: ডিফল্ট dry-run — removed=০ (dry-run-এ বাড়ে না), skipped=১ (ESC_ fallback), items=৫ (CL1×২, CL2×১, CL3×১, ESC×১)'
);
SELECT ok(
  (SELECT count(*) = 3 FROM public.transactions WHERE type = 'REFUND' AND escrow_id = 'E_CL1')
  AND (SELECT balance = 500 AND balance_user = 500 FROM public.users WHERE id = 'e6200001-0000-0000-0000-000000000001')
  AND (SELECT count(*) = 0 FROM public.admin_audit_logs WHERE action_type = 'CLEANUP_DUPLICATE_REFUNDS'),
  'admin_cleanup_duplicate_refunds: dry-run কিছু মোছে না, balance বদলায় না, audit log লেখে না'
);
SELECT ok(
  (SELECT NOT (r->>'dry_run')::boolean AND (r->>'removed_count')::int = 4 AND (r->>'skipped_count')::int = 1
   FROM (SELECT public.admin_cleanup_duplicate_refunds(false) AS r) s),
  'admin_cleanup_duplicate_refunds: live — removed=৪ (CL1-এর ২ + CL2-এর ১ + CL3-এর ১), skipped=১'
);
SELECT set_eq(
  $$ SELECT escrow_id, type, count(*) FROM public.transactions
      WHERE escrow_id IN ('E_CL1', 'E_CL2', 'E_CL3', 'E_CL4', 'ESC_FALLBACK1') GROUP BY escrow_id, type $$,
  $$ VALUES ('E_CL1'::text, 'REFUND'::text, 1::bigint), ('E_CL2', 'REFUND', 1), ('E_CL3', 'REFUND', 1),
            ('E_CL4', 'PAYMENT', 2), ('ESC_FALLBACK1', 'REFUND', 2) $$,
  'admin_cleanup_duplicate_refunds: প্রতি escrow-তে earliest ১টা REFUND থাকে; PAYMENT-টাইপ ডুপ্লিকেট (CL4) আর ESC_ fallback (২টাই) অক্ষত'
);
SELECT ok(
  (SELECT balance = 300 AND balance_user = 300 FROM public.users WHERE id = 'e6200001-0000-0000-0000-000000000001'),
  'admin_cleanup_duplicate_refunds: owner-এর balance ও balance_user ৫০০ → ৩০০ (CL1-এর দুই ডুপ্লিকেট ২০০ কাটা; net_amount=০ ডুপ্লিকেটে কোনো কাটা নেই)'
);
SELECT ok(
  (SELECT balance = 0 AND balance_user = 0 FROM public.users WHERE id = 'e6200002-0000-0000-0000-000000000002'),
  'admin_cleanup_duplicate_refunds: ব্যালেন্সের চেয়ে বড় ডুপ্লিকেট (১০ − ৩০) হলে greatest(0, …) দিয়ে ০-তে clamp হয়, ঋণাত্মক না'
);
SELECT set_eq(
  $$ SELECT id, type, gross_amount, net_amount FROM public.transactions WHERE type = 'DUPLICATE_CORRECTION' $$,
  $$ VALUES ('TRX_DUP_CORRECTION_TRX_CL1_B'::text, 'DUPLICATE_CORRECTION'::text, 100::numeric(12,2), (-100)::numeric(12,2)),
            ('TRX_DUP_CORRECTION_TRX_CL1_C', 'DUPLICATE_CORRECTION', 100, -100),
            ('TRX_DUP_CORRECTION_TRX_CL3_B', 'DUPLICATE_CORRECTION',  30,  -30) $$,
  'admin_cleanup_duplicate_refunds: ৩টা DUPLICATE_CORRECTION transaction (gross=+amount, net=−amount); net=০ ডুপ্লিকেটের জন্য নেই'
);
SELECT is(
  (SELECT details FROM public.admin_audit_logs WHERE action_type = 'CLEANUP_DUPLICATE_REFUNDS'),
  'removed=4, skipped_fallback=1',
  'admin_cleanup_duplicate_refunds: live রানে audit log ঠিক সারাংশসহ লেখা হয়'
);
SELECT ok(
  (SELECT (r->>'removed_count')::int = 0 AND (r->>'skipped_count')::int = 1
   FROM (SELECT public.admin_cleanup_duplicate_refunds(false) AS r) s),
  'admin_cleanup_duplicate_refunds: দ্বিতীয় live রান — removed=০ (idempotent), ESC_ fallback এখনো skipped=১'
);
SELECT is(
  (SELECT count(*)::int FROM public.admin_audit_logs WHERE action_type = 'CLEANUP_DUPLICATE_REFUNDS'),
  1,
  'admin_cleanup_duplicate_refunds: removed=০ হলে নতুন audit log নেই (মোট ১টাই)'
);

----------------------------------------------------------------------
-- admin_update_direct_contract_status
-- (এই সেকশন থেকে নতুন HELD escrow তৈরি হচ্ছে — reconcile সেকশনের পরে বলে সেটা প্রভাবিত হয়নি)
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, balance, balance_user, balance_solver, has_user_role, has_solver_role) VALUES
  ('e7000001-0000-0000-0000-000000000001', 'CLIENT', 'DC Owner',  '01830000001', 100, 100, 0, true, false),
  ('e7000002-0000-0000-0000-000000000002', 'SOLVER', 'DC Solver', '01830000002',   0,   0, 0, true, true);
INSERT INTO public.problems (id, user_id, title, status, is_direct_contract, accepted_solver_id, applied_commission_rate) VALUES
  ('DC1', 'e7000001-0000-0000-0000-000000000001', 'DC Complete', 'IN_PROGRESS', true, 'e7000002-0000-0000-0000-000000000002', 10),
  ('DC2', 'e7000001-0000-0000-0000-000000000001', 'DC Cancel',   'IN_PROGRESS', true, 'e7000002-0000-0000-0000-000000000002', 10),
  ('DC3', 'e7000001-0000-0000-0000-000000000001', 'DC Other',    'OPEN',        true, NULL, NULL),
  ('DC4', 'e7000001-0000-0000-0000-000000000001', 'DC NoEscrow', 'IN_PROGRESS', true, 'e7000002-0000-0000-0000-000000000002', 10);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, status) VALUES
  ('E_DC1', 'DC1', 'DC Complete', 'e7000001-0000-0000-0000-000000000001', 'e7000002-0000-0000-0000-000000000002', 500, 'HELD'),
  ('E_DC2', 'DC2', 'DC Cancel',   'e7000001-0000-0000-0000-000000000001', 'e7000002-0000-0000-0000-000000000002', 300, 'HELD');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_update_direct_contract_status('DC3', 'IN_PROGRESS', 'ACCEPTED') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_update_direct_contract_status: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_update_direct_contract_status('NOPE', 'COMPLETED', 'COMPLETED') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'admin_update_direct_contract_status: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->'inner'->>'result' = 'OK'
   FROM (SELECT public.admin_update_direct_contract_status('DC1', 'COMPLETED', 'COMPLETED') AS r) s),
  'admin_update_direct_contract_status(COMPLETED): result OK + ভেতরের release_escrow result OK'
);
SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'E_DC1'), status, direct_contract_status
       FROM public.problems WHERE id = 'DC1' $$,
  $$ VALUES ('RELEASED'::text, 'COMPLETED'::text, 'COMPLETED'::text) $$,
  'admin_update_direct_contract_status(COMPLETED): HELD escrow RELEASED, problem COMPLETED, direct_contract_status COMPLETED'
);
SELECT ok(
  (SELECT balance_solver = 450 FROM public.users WHERE id = 'e7000002-0000-0000-0000-000000000002'),
  'admin_update_direct_contract_status(COMPLETED): solver-এর balance_solver +৪৫০ (৫০০ − ১০% commission)'
);
SELECT set_eq(
  $$ SELECT role FROM public.notifications WHERE related_problem_id = 'DC1' AND title = 'ডাইরেক্ট চুক্তি স্ট্যাটাস আপডেট' $$,
  $$ VALUES ('USER'::text), ('SOLVER'::text) $$,
  'admin_update_direct_contract_status: owner (role=USER) ও accepted solver (role=SOLVER) দুজনেই notification পায়'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->'inner'->>'amount')::numeric = 300
   FROM (SELECT public.admin_update_direct_contract_status('DC2', 'CANCELLED', 'CANCELLED') AS r) s),
  'admin_update_direct_contract_status(CANCELLED): result OK + ভেতরের refund_escrow_once amount ৩০০'
);
SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'E_DC2'),
            (SELECT refund_type FROM public.transactions WHERE id = 'TRX_REFUND_E_DC2') $$,
  $$ VALUES ('REFUNDED'::text, 'ADMIN_DIRECT_CONTRACT_CANCEL'::text) $$,
  'admin_update_direct_contract_status(CANCELLED): escrow REFUNDED, refund tx-এর refund_type = ADMIN_DIRECT_CONTRACT_CANCEL'
);
SELECT ok(
  (SELECT balance_user = 400 FROM public.users WHERE id = 'e7000001-0000-0000-0000-000000000001'),
  'admin_update_direct_contract_status(CANCELLED): owner-এর balance_user ১০০ → ৪০০ (+৩০০ রিফান্ড)'
);
SELECT results_eq(
  $$ SELECT status, direct_contract_status FROM public.problems WHERE id = 'DC2' $$,
  $$ VALUES ('CANCELLED'::text, 'CANCELLED'::text) $$,
  'admin_update_direct_contract_status(CANCELLED): problem status ও direct_contract_status দুটোই CANCELLED'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND jsonb_typeof(r->'inner') = 'null'
   FROM (SELECT public.admin_update_direct_contract_status('DC3', 'IN_PROGRESS', 'ACCEPTED') AS r) s),
  'admin_update_direct_contract_status(অন্য status): result OK, escrow-ফ্লো না চললে inner = JSON null'
);
SELECT results_eq(
  $$ SELECT status, direct_contract_status FROM public.problems WHERE id = 'DC3' $$,
  $$ VALUES ('IN_PROGRESS'::text, 'ACCEPTED'::text) $$,
  'admin_update_direct_contract_status(অন্য status): সরাসরি status ও direct_contract_status আপডেট হয়'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE related_problem_id = 'DC3' AND title = 'ডাইরেক্ট চুক্তি স্ট্যাটাস আপডেট'),
  1,
  'admin_update_direct_contract_status: accepted solver না থাকলে শুধু owner-এর ১টা notification'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' AND jsonb_typeof(r->'inner') = 'null'
   FROM (SELECT public.admin_update_direct_contract_status('DC4', 'COMPLETED', 'COMPLETED') AS r) s),
  'admin_update_direct_contract_status(COMPLETED, escrow নেই): result OK, inner = JSON null'
);
SELECT results_eq(
  $$ SELECT status, direct_contract_status FROM public.problems WHERE id = 'DC4' $$,
  $$ VALUES ('IN_PROGRESS'::text, 'COMPLETED'::text) $$,
  'admin_update_direct_contract_status: DOCUMENTED CURRENT BEHAVIOUR — HELD escrow না থাকলে p_status=COMPLETED দিলেও problems.status IN_PROGRESS-ই থাকে (শুধু direct_contract_status বদলায়)'
);

----------------------------------------------------------------------
-- admin_force_cancel_instant_job
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('e7100001-0000-0000-0000-000000000001', 'CLIENT', 'FC Owner',   '01840000001', true, false),
  ('e7100002-0000-0000-0000-000000000002', 'SOLVER', 'FC Solver1', '01840000002', true, true),
  ('e7100003-0000-0000-0000-000000000003', 'SOLVER', 'FC Solver2', '01840000003', true, true);
INSERT INTO public.problems (id, user_id, title, status, job_status, is_instant_job, accepted_bid_id, accepted_solver_id,
                             accepted_solver_name, accepted_amount, has_release_request, release_request_extra_amount, release_request_note) VALUES
  ('FC1', 'e7100001-0000-0000-0000-000000000001', 'FC Cancel',      'IN_PROGRESS', 'ON_THE_WAY', true, 'BID_FC1_A', 'e7100002-0000-0000-0000-000000000002', 'S1', 500, true, 50, 'note'),
  ('FC2', 'e7100001-0000-0000-0000-000000000001', 'FC Rebroadcast', 'IN_PROGRESS', 'ON_THE_WAY', true, 'BID_FC2_A', 'e7100002-0000-0000-0000-000000000002', 'S1', 500, true, 50, 'note'),
  ('FC3', 'e7100001-0000-0000-0000-000000000001', 'FC Normal',      'IN_PROGRESS', 'ON_THE_WAY', true, NULL,        'e7100002-0000-0000-0000-000000000002', 'S1', 500, false, 0, ''),
  ('FC4', 'e7100001-0000-0000-0000-000000000001', 'FC Terminal',    'COMPLETED',   'JOB_COMPLETED', true, NULL, NULL, NULL, NULL, false, 0, ''),
  ('FC5', 'e7100001-0000-0000-0000-000000000001', 'FC NoSolver',    'OPEN',        'BROADCASTING', true, NULL, NULL, NULL, NULL, false, 0, '');
INSERT INTO public.bids (id, problem_id, solver_id, amount, status, resolved_at) VALUES
  ('BID_FC1_A', 'FC1', 'e7100002-0000-0000-0000-000000000002', 500, 'ACCEPTED',  NULL),
  ('BID_FC1_B', 'FC1', 'e7100002-0000-0000-0000-000000000002', 450, 'PENDING',   NULL),
  ('BID_FC1_C', 'FC1', 'e7100003-0000-0000-0000-000000000003', 480, 'PENDING',   NULL),
  ('BID_FC1_D', 'FC1', 'e7100002-0000-0000-0000-000000000002', 400, 'CANCELLED', NULL),
  ('BID_FC2_A', 'FC2', 'e7100002-0000-0000-0000-000000000002', 500, 'ACCEPTED',  NULL);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, status) VALUES
  ('E_FC1', 'FC1', 'FC Cancel', 'e7100001-0000-0000-0000-000000000001', 'e7100002-0000-0000-0000-000000000002', 500, 'HELD'),
  ('E_FC2', 'FC2', 'FC Rebroadcast', 'e7100001-0000-0000-0000-000000000001', 'e7100002-0000-0000-0000-000000000002', 500, 'REFUNDED');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_force_cancel_instant_job('FC1', 'r') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_force_cancel_instant_job: non-admin → NOT_AUTHORIZED'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_force_cancel_instant_job('NOPE', 'r') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'admin_force_cancel_instant_job: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT is(
  (public.admin_force_cancel_instant_job('FC4', 'r'))->>'result',
  'ALREADY_TERMINAL',
  'admin_force_cancel_instant_job: COMPLETED/CANCELLED problem → result ALREADY_TERMINAL'
);
SELECT is(
  (public.admin_force_cancel_instant_job('FC1', 'because', 'CANCEL', 3))->>'result',
  'OK',
  'admin_force_cancel_instant_job(CANCEL): happy path → result OK'
);
SELECT results_eq(
  $$ SELECT status, job_status, accepted_bid_id IS NULL, accepted_solver_id = 'e7100002-0000-0000-0000-000000000002'::uuid,
            has_release_request, release_request_extra_amount = 0, release_request_note
       FROM public.problems WHERE id = 'FC1' $$,
  $$ VALUES ('CANCELLED'::text, 'CANCELLED'::text, true, true, false, true, ''::text) $$,
  'admin_force_cancel_instant_job(CANCEL): status/job_status CANCELLED, accepted_bid_id NULL, release-request পরিষ্কার; DOCUMENTED — accepted_solver_id নাল হয় না'
);
SELECT set_eq(
  $$ SELECT id, status, progress_at_cancel, resolution_type FROM public.bids WHERE problem_id = 'FC1' $$,
  $$ VALUES ('BID_FC1_A'::text, 'CANCELLED'::text, 3, 'ADMIN_FORCE_ACTION'::text),
            ('BID_FC1_B', 'CANCELLED', 3, 'ADMIN_FORCE_ACTION'),
            ('BID_FC1_C', 'PENDING', NULL::integer, NULL::text),
            ('BID_FC1_D', 'CANCELLED', NULL, NULL) $$,
  'admin_force_cancel_instant_job(CANCEL): accepted bid + accepted solver-এর অন্য বিড CANCELLED (step=৩, ADMIN_FORCE_ACTION); অন্য solver-এর বিড ও আগে-CANCELLED বিড অক্ষত'
);
SELECT is(
  (SELECT count(*)::int FROM public.bids WHERE problem_id = 'FC1' AND resolved_at IS NOT NULL),
  2,
  'admin_force_cancel_instant_job(CANCEL): শুধু নতুন-cancel হওয়া ২টা বিডে resolved_at বসে'
);
SELECT set_eq(
  $$ SELECT user_id FROM public.notifications
      WHERE related_problem_id = 'FC1' AND title = 'জরুরি কাজ বাতিল (অ্যাডমিন) ❌' AND position('because' in message) > 0 $$,
  $$ VALUES ('e7100001-0000-0000-0000-000000000001'::uuid), ('e7100002-0000-0000-0000-000000000002'::uuid) $$,
  'admin_force_cancel_instant_job(CANCEL): owner ও accepted solver — দুজনেই কারণসহ notification পায়'
);
SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'E_FC1'),
  'REFUNDED',
  'admin_force_cancel_instant_job (Step 7.2): accepted solver-এর HELD escrow এখন REFUNDED হয়'
);
SELECT results_eq(
  $$ SELECT count(*)::int, min(net_amount)::numeric, min(refund_type) FROM public.transactions
      WHERE escrow_id = 'E_FC1' AND type = 'REFUND' $$,
  $$ VALUES (1, 500::numeric, 'ADMIN_FORCE_ACTION'::text) $$,
  'admin_force_cancel_instant_job (Step 7.2): E_FC1-এর জন্য ঠিক ১টা REFUND transaction (৫০০, ADMIN_FORCE_ACTION)'
);
SELECT is(
  (public.admin_force_cancel_instant_job('FC2', 'r2', 'REBROADCAST', 0))->>'result',
  'OK',
  'admin_force_cancel_instant_job(REBROADCAST): result OK'
);
SELECT is(
  (SELECT count(*)::int FROM public.transactions WHERE escrow_id = 'E_FC2'),
  0,
  'admin_force_cancel_instant_job (Step 7.2): আগে-থেকে-REFUNDED escrow (E_FC2) এ double-refund হয় না — কোনো নতুন transaction নেই'
);
SELECT results_eq(
  $$ SELECT status, job_status, is_instant_job, accepted_bid_id IS NULL, accepted_solver_id IS NULL, accepted_amount IS NULL,
            has_release_request, broadcast_timer_started_at IS NOT NULL
       FROM public.problems WHERE id = 'FC2' $$,
  $$ VALUES ('OPEN'::text, 'BROADCASTING'::text, true, true, true, true, false, true) $$,
  'admin_force_cancel_instant_job(REBROADCAST): OPEN/BROADCASTING, accepted_* সব NULL, release-request পরিষ্কার, broadcast timer নতুন করে শুরু'
);
SELECT is(
  (SELECT progress_at_cancel FROM public.bids WHERE id = 'BID_FC2_A'),
  1,
  'admin_force_cancel_instant_job: p_progress_step=0 → greatest(…,1) দিয়ে ১-এ উঠে যায়'
);
SELECT set_eq(
  $$ SELECT user_id FROM public.notifications WHERE related_problem_id = 'FC2' $$,
  $$ VALUES ('e7100001-0000-0000-0000-000000000001'::uuid), ('e7100002-0000-0000-0000-000000000002'::uuid) $$,
  'admin_force_cancel_instant_job(REBROADCAST): solver-এর accepted_solver_id নাল হলেও (আগের snapshot থেকে) owner ও solver দুজনেই notification পায়'
);
SELECT is(
  (public.admin_force_cancel_instant_job('FC3', 'r3', 'TO_NORMAL_BIDDING'))->>'result',
  'OK',
  'admin_force_cancel_instant_job(TO_NORMAL_BIDDING): result OK'
);
SELECT results_eq(
  $$ SELECT status, job_status IS NULL, is_instant_job, accepted_solver_id IS NULL, accepted_solver_name IS NULL
       FROM public.problems WHERE id = 'FC3' $$,
  $$ VALUES ('OPEN'::text, true, false, true, true) $$,
  'admin_force_cancel_instant_job(TO_NORMAL_BIDDING): OPEN, is_instant_job=false, job_status NULL, accepted_* NULL'
);
SELECT is(
  (public.admin_force_cancel_instant_job('FC5', 'r5', 'SOMETHING_ELSE'))->>'result',
  'OK',
  'admin_force_cancel_instant_job: অচেনা p_target_action দিলেও result OK'
);
SELECT results_eq(
  $$ SELECT status, job_status FROM public.problems WHERE id = 'FC5' $$,
  $$ VALUES ('CANCELLED'::text, 'CANCELLED'::text) $$,
  'admin_force_cancel_instant_job: অচেনা p_target_action কার্যত CANCEL branch-এ পড়ে (else)'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE related_problem_id = 'FC5'),
  1,
  'admin_force_cancel_instant_job: accepted solver না থাকলে শুধু owner-এর ১টা notification'
);

-- [Step 7.2] owner-এর USER role নিষ্ক্রিয় থাকলে cancel চলে, refund pending থাকে; role active হলে trigger refund করে
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('e7100004-0000-0000-0000-000000000004', 'CLIENT', 'FC Inactive Owner', '01840000004', false, false);
INSERT INTO public.problems (id, user_id, title, status, job_status, is_instant_job, accepted_solver_id, accepted_solver_name, accepted_amount) VALUES
  ('FC6', 'e7100004-0000-0000-0000-000000000004', 'FC Inactive Role', 'IN_PROGRESS', 'ON_THE_WAY', true, 'e7100002-0000-0000-0000-000000000002', 'S1', 300);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, status) VALUES
  ('E_FC6', 'FC6', 'FC Inactive Role', 'e7100004-0000-0000-0000-000000000004', 'e7100002-0000-0000-0000-000000000002', 300, 'HELD');
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT results_eq(
  $$ SELECT r->>'result', r->>'refund_pending'
       FROM (SELECT public.admin_force_cancel_instant_job('FC6', 'r6', 'CANCEL', 1) AS r) x $$,
  $$ VALUES ('OK'::text, 'true'::text) $$,
  'admin_force_cancel_instant_job (Step 7.2): owner-এর USER role নিষ্ক্রিয় → cancel সফল, result OK + refund_pending=true'
);
SELECT results_eq(
  $$ SELECT (SELECT status FROM public.problems WHERE id = 'FC6'),
            (SELECT status FROM public.escrows WHERE id = 'E_FC6'),
            (SELECT count(*)::int FROM public.transactions WHERE escrow_id = 'E_FC6') $$,
  $$ VALUES ('CANCELLED'::text, 'HELD'::text, 0) $$,
  'admin_force_cancel_instant_job (Step 7.2): USER role নিষ্ক্রিয় থাকলে problem CANCELLED কিন্তু escrow HELD, কোনো refund transaction নেই'
);
RESET ROLE;
UPDATE public.users SET has_user_role = true WHERE id = 'e7100004-0000-0000-0000-000000000004';
SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'E_FC6'),
            (SELECT count(*)::int FROM public.transactions
              WHERE escrow_id = 'E_FC6' AND type = 'REFUND' AND refund_type = 'ROLE_REACTIVATION_REFUND') $$,
  $$ VALUES ('REFUNDED'::text, 1) $$,
  'Step 7.2 trigger: USER role আবার active হলে pending HELD escrow স্বয়ংক্রিয় REFUNDED + ঠিক ১টা REFUND transaction'
);

----------------------------------------------------------------------
-- owner_reset_orphaned_accepted_bid
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('e7200001-0000-0000-0000-000000000001', 'CLIENT', 'OR Owner',  '01850000001', true, false),
  ('e7200002-0000-0000-0000-000000000002', 'SOLVER', 'OR Solver', '01850000002', true, true);
INSERT INTO public.problems (id, user_id, title, status, job_status, is_instant_job, is_disputed, accepted_bid_id, accepted_solver_id,
                             accepted_solver_name, accepted_amount, has_release_request, release_request_note, solver_cancelled_notice) VALUES
  ('OR1', 'e7200001-0000-0000-0000-000000000001', 'OR1', 'IN_PROGRESS', 'PENDING_START', false, false, 'BID_OR1', 'e7200002-0000-0000-0000-000000000002', 'S', 100, true, 'x', 'gone'),
  ('OR2', 'e7200001-0000-0000-0000-000000000001', 'OR2', 'IN_PROGRESS', 'ON_THE_WAY',    true,  false, NULL,      'e7200002-0000-0000-0000-000000000002', 'S', 100, false, '', NULL),
  ('OR3', 'e7200001-0000-0000-0000-000000000001', 'OR3', 'IN_PROGRESS', NULL,            false, false, NULL,      'e7200002-0000-0000-0000-000000000002', 'S', 100, false, '', NULL),
  ('OR4', 'e7200001-0000-0000-0000-000000000001', 'OR4', 'OPEN',        NULL,            false, false, NULL,      NULL, NULL, NULL, false, '', NULL),
  ('OR5', 'e7200001-0000-0000-0000-000000000001', 'OR5', 'IN_PROGRESS', NULL,            false, true,  NULL,      'e7200002-0000-0000-0000-000000000002', 'S', 100, false, '', NULL),
  ('OR6', 'e7200001-0000-0000-0000-000000000001', 'OR6', 'COMPLETED',   NULL,            false, false, NULL,      'e7200002-0000-0000-0000-000000000002', 'S', 100, false, '', NULL),
  ('OR7', 'e7200001-0000-0000-0000-000000000001', 'OR7', 'CANCELLED',   NULL,            false, false, NULL,      'e7200002-0000-0000-0000-000000000002', 'S', 100, false, '', NULL),
  ('OR8', 'e7200001-0000-0000-0000-000000000001', 'OR8', 'IN_PROGRESS', NULL,            false, false, NULL,      'e7200002-0000-0000-0000-000000000002', 'S', 200, false, '', NULL);
INSERT INTO public.bids (id, problem_id, solver_id, amount, status) VALUES
  ('BID_OR1', 'OR1', 'e7200002-0000-0000-0000-000000000002', 100, 'ACCEPTED');
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status) VALUES
  ('E_OR1', 'OR1', 'OR1', 'e7200001-0000-0000-0000-000000000001', 'e7200002-0000-0000-0000-000000000002', 100, 0, 'REFUNDED'),
  ('E_OR3', 'OR3', 'OR3', 'e7200001-0000-0000-0000-000000000001', 'e7200002-0000-0000-0000-000000000002',   0, 0, 'HELD'),
  ('E_OR8', 'OR8', 'OR8', 'e7200001-0000-0000-0000-000000000001', 'e7200002-0000-0000-0000-000000000002', 200, 0, 'HELD');

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.owner_reset_orphaned_accepted_bid('OR1') $$,
  'P0001', 'NOT_AUTHORIZED',
  'owner_reset_orphaned_accepted_bid: owner না এমন caller (এখানে admin-ও) → NOT_AUTHORIZED'
);
SELECT test.login_as('e7200001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.owner_reset_orphaned_accepted_bid('NOPE') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'owner_reset_orphaned_accepted_bid: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT is(
  (public.owner_reset_orphaned_accepted_bid('OR4'))->>'result',
  'NOT_ACCEPTED',
  'owner_reset_orphaned_accepted_bid: accepted solver নেই → result NOT_ACCEPTED'
);
SELECT is(
  (SELECT array_agg(public.owner_reset_orphaned_accepted_bid(x)->>'result' ORDER BY x) FROM unnest(ARRAY['OR5', 'OR6', 'OR7']) x),
  ARRAY['NOT_ELIGIBLE', 'NOT_ELIGIBLE', 'NOT_ELIGIBLE'],
  'owner_reset_orphaned_accepted_bid: disputed / COMPLETED / CANCELLED — তিনটাই result NOT_ELIGIBLE'
);
SELECT is(
  (public.owner_reset_orphaned_accepted_bid('OR8'))->>'result',
  'FUNDS_LOCKED',
  'owner_reset_orphaned_accepted_bid: HELD escrow-এ টাকা (২০০) আটকা → result FUNDS_LOCKED'
);
SELECT results_eq(
  $$ SELECT status, accepted_solver_id IS NOT NULL FROM public.problems WHERE id = 'OR8' $$,
  $$ VALUES ('IN_PROGRESS'::text, true) $$,
  'owner_reset_orphaned_accepted_bid: FUNDS_LOCKED হলে problem অপরিবর্তিত (accepted solver বহাল)'
);
SELECT is(
  (public.owner_reset_orphaned_accepted_bid('OR1'))->>'result',
  'OK',
  'owner_reset_orphaned_accepted_bid: টাকা আটকা নেই (escrow REFUNDED) → result OK'
);
SELECT results_eq(
  $$ SELECT status, accepted_bid_id IS NULL, accepted_solver_id IS NULL, accepted_solver_name IS NULL, accepted_amount IS NULL,
            has_release_request, release_request_note, solver_cancelled_notice IS NULL, job_status
       FROM public.problems WHERE id = 'OR1' $$,
  $$ VALUES ('OPEN'::text, true, true, true, true, false, ''::text, true, 'PENDING_START'::text) $$,
  'owner_reset_orphaned_accepted_bid: problem OPEN, accepted_* NULL, release-request/solver_cancelled_notice পরিষ্কার; non-instant হওয়ায় job_status অপরিবর্তিত'
);
SELECT is(
  (SELECT status FROM public.bids WHERE id = 'BID_OR1'),
  'ACCEPTED',
  'owner_reset_orphaned_accepted_bid: DOCUMENTED CURRENT BEHAVIOUR — accepted bid-এর নিজের status ACCEPTED-ই থেকে যায় (শুধু problems পরিষ্কার হয়)'
);
SELECT is(
  (public.owner_reset_orphaned_accepted_bid('OR2'))->>'result',
  'OK',
  'owner_reset_orphaned_accepted_bid: instant job (escrow ছাড়া) → result OK'
);
SELECT ok(
  (SELECT status = 'OPEN' AND job_status = 'BROADCASTING' AND broadcast_timer_started_at IS NOT NULL
         FROM public.problems WHERE id = 'OR2'),
  'owner_reset_orphaned_accepted_bid: instant job (escrow ছাড়া) → OPEN + job_status BROADCASTING + নতুন broadcast timer'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' FROM (SELECT public.owner_reset_orphaned_accepted_bid('OR3') AS r) s),
  'owner_reset_orphaned_accepted_bid: DOCUMENTED — শূন্য-টাকার (base=০, extra=০) HELD escrow reset আটকায় না'
);

----------------------------------------------------------------------
-- admin_wipe_all_data — ⚠️ destructive, তাই সবার শেষে (এরপর admin ইউজারও আর থাকে না)।
-- পুরোটা BEGIN … ROLLBACK-এর ভেতরে — আসল DB/অন্য টেস্ট ফাইল অক্ষত।
----------------------------------------------------------------------
RESET ROLE;
INSERT INTO public.messages (id, problem_id, sender_id, receiver_id, sender_name, content)
  VALUES ('MSG_WIPE', 'DC3', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'W', 'w');
INSERT INTO public.ratings (id, problem_id, user_id, solver_id, stars)
  VALUES ('RAT_WIPE', 'DC3', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 4);
INSERT INTO public.reputation_events (id, user_id, event_type, problem_id, score_change, score_after, note)
  VALUES ('REP_WIPE', '22222222-2222-2222-2222-222222222222', 'TEST', 'DC3', 1, 1, 'n');
INSERT INTO public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status)
  VALUES ('AC_WIPE', 'DC3', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r', 10, 'PENDING');
INSERT INTO public.gateway_payments (id, gateway_trx_id, user_id, amount, status)
  VALUES ('GP_WIPE', 'GW_WIPE_1', '11111111-1111-1111-1111-111111111111', 10, 'SUCCESS');
INSERT INTO public.categories (id, name) VALUES ('CAT_WIPE', 'wipe');
INSERT INTO public.faqs (id, question, answer) VALUES ('FAQ_WIPE', 'q', 'a');
INSERT INTO public.platform_settings (key, value) VALUES ('wipe_key', 'x');
INSERT INTO public.admin_credentials (id, phone, password_hash) VALUES (1, '01700000099', 'h') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.idempotency_keys (key, request_type) VALUES ('IDEM_WIPE', 'TEST');
DO $$ BEGIN
  PERFORM set_config('test.wipe_keep_before',
    ((SELECT count(*) FROM public.admin_credentials) || ',' || (SELECT count(*) FROM public.idempotency_keys)), true);
END $$;

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_wipe_all_data() $$,
  '42501', 'NOT_AUTHORIZED',
  'admin_wipe_all_data: non-admin → NOT_AUTHORIZED (errcode 42501)'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT ok(
  (SELECT count(*) > 0 FROM public.transactions) AND (SELECT count(*) > 0 FROM public.messages)
  AND (SELECT count(*) > 0 FROM public.ratings) AND (SELECT count(*) > 0 FROM public.reputation_events)
  AND (SELECT count(*) > 0 FROM public.notifications) AND (SELECT count(*) > 0 FROM public.gateway_payments)
  AND (SELECT count(*) > 0 FROM public.additional_charges) AND (SELECT count(*) > 0 FROM public.escrows)
  AND (SELECT count(*) > 0 FROM public.bids) AND (SELECT count(*) > 0 FROM public.problems)
  AND (SELECT count(*) > 0 FROM public.withdrawals) AND (SELECT count(*) > 0 FROM public.admin_audit_logs)
  AND (SELECT count(*) > 0 FROM public.users) AND (SELECT count(*) > 0 FROM public.categories)
  AND (SELECT count(*) > 0 FROM public.faqs) AND (SELECT count(*) > 0 FROM public.platform_settings),
  'admin_wipe_all_data: precondition — wipe-যোগ্য ১৬টা টেবিলেই আগে data আছে (নাহলে নিচের "সব খালি" assertion অর্থহীন হতো)'
);
SELECT is(
  (public.admin_wipe_all_data())->>'result',
  'OK',
  'admin_wipe_all_data: admin কল করলে result OK (FK child→parent ক্রমে কোনো FK-ভায়োলেশন ছাড়াই)'
);
SELECT is(
  (SELECT (SELECT count(*) FROM public.transactions) + (SELECT count(*) FROM public.messages)
        + (SELECT count(*) FROM public.ratings) + (SELECT count(*) FROM public.reputation_events)
        + (SELECT count(*) FROM public.notifications) + (SELECT count(*) FROM public.gateway_payments)
        + (SELECT count(*) FROM public.additional_charges) + (SELECT count(*) FROM public.escrows)
        + (SELECT count(*) FROM public.bids) + (SELECT count(*) FROM public.problems)
        + (SELECT count(*) FROM public.withdrawals) + (SELECT count(*) FROM public.admin_audit_logs)
        + (SELECT count(*) FROM public.users) + (SELECT count(*) FROM public.categories)
        + (SELECT count(*) FROM public.faqs) + (SELECT count(*) FROM public.platform_settings))::int,
  0,
  'admin_wipe_all_data: ১৬টা টেবিলই সম্পূর্ণ খালি'
);
SELECT is(
  ((SELECT count(*) FROM public.admin_credentials) || ',' || (SELECT count(*) FROM public.idempotency_keys)),
  current_setting('test.wipe_keep_before'),
  'admin_wipe_all_data: admin_credentials ও idempotency_keys ইচ্ছাকৃতভাবে বাদ — wipe-এর আগে-পরে সারি-সংখ্যা অপরিবর্তিত'
);

SELECT test.logout();
SELECT * FROM finish();
ROLLBACK;

