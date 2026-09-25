-- 04_instant_jobs_part2.sql — Step 2 (Instant jobs / broadcasting), PART 2 of 2
--
-- এই ফাইলে কভার করা হয়েছে বাকি ৫টা ফাংশন: solver_cancel_job (recovered_instant_jobs.sql),
-- expire_stale_instant_jobs (step28_expire_stale_instant_jobs_cron.sql — শুধু function
-- body verify করা হয়েছে, pg_cron extension/schedule অংশ টেস্ট-স্কোপে না),
-- clear_solver_cancelled_notice (step32_95_clear_solver_cancelled_notice.sql),
-- update_solver_live_location (step23_update_solver_live_location_rpc.sql),
-- sync_solver_free_job_quota (step_money_flow_fix4_sync_solver_free_job_quota.sql)।
--
-- ⚠️ solver_cancel_job ভেতরে public.refund_escrow_once(...) কল করে, যেটা এখনো
-- আনুষ্ঠানিকভাবে Step 3-এর কাজ না হলেও schema stub-এ (rule #6 অনুযায়ী) আসল
-- বডি ধার করে বসানো হয়েছে — দেখুন 01_bidding_flow_schema_stub.sql-এর
-- "Step 2 PART 2" সেকশনের নোট।
--
-- expire_stale_instant_jobs()-এ client থেকে EXECUTE revoke করা (cron-only) —
-- তাই এই টেস্টে test.login_as(...) দিয়ে authenticated-এ সুইচ না করে সরাসরি
-- superuser (postgres, এই স্ক্রিপ্ট যেভাবে চলে) হিসেবে কল করা হয়েছে, যেটা
-- বাস্তবেও ঠিক এভাবেই (owner/superuser হিসেবেই, RPC role হিসেবে না) চলার কথা।

BEGIN;
SELECT plan(32);

SELECT test.seed_users();

-- test user ids (00_helpers.sql / test.seed_users থেকে)
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...

----------------------------------------------------------------------
-- solver_cancel_job — happy path (non-instant job, escrow HELD থাকা অবস্থায়,
-- reopen_as_open ডিফল্ট true): escrow refund + problem reset হয়
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  accepted_bid_id, accepted_solver_id, accepted_solver_name, accepted_amount
) VALUES (
  'P30', '11111111-1111-1111-1111-111111111111', 'Cancel with escrow', 'IN_PROGRESS', 'IN_PROGRESS', false, true,
  'B30', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 300
);

INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status)
VALUES ('E30', 'P30', 'Cancel with escrow', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 300, 0, 'HELD');

UPDATE public.users SET balance_user = 200, balance = 200 WHERE id = '11111111-1111-1111-1111-111111111111';

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.solver_cancel_job('P30', 'ব্যক্তিগত সমস্যা'))->>'result',
  'OK',
  'solver_cancel_job: accepted solver কল করলে OK'
);

SELECT results_eq(
  $$ SELECT status, accepted_bid_id, accepted_solver_id, accepted_amount FROM public.problems WHERE id = 'P30' $$,
  $$ VALUES ('OPEN'::text, NULL::text, NULL::uuid, NULL::numeric) $$,
  'solver_cancel_job: reopen_as_open=true (ডিফল্ট) হলে status OPEN + accepted_* সব null হয়ে যায়'
);

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'E30'),
  'REFUNDED',
  'solver_cancel_job: সংশ্লিষ্ট HELD escrow REFUNDED হয়ে যায়'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111')::text,
  '500.00',
  'solver_cancel_job: refund_escrow_once-এর মাধ্যমে owner-এর balance_user 300 বেড়ে যায় (200+300)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- solver_cancel_job — instant job case: job_status BROADCASTING হয় +
-- broadcast_timer_started_at রিসেট হয় (reopen=true, is_instant_job=true)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  accepted_solver_id, broadcast_timer_started_at
) VALUES (
  'P31', '11111111-1111-1111-1111-111111111111', 'Instant cancel', 'IN_PROGRESS', 'IN_PROGRESS', true, true,
  '22222222-2222-2222-2222-222222222222', now() - interval '1 hour'
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.solver_cancel_job('P31', 'reason'))->>'result',
  'OK',
  'solver_cancel_job: instant job-এও OK রিটার্ন করে'
);

SELECT is(
  (SELECT job_status FROM public.problems WHERE id = 'P31'),
  'BROADCASTING',
  'solver_cancel_job: is_instant_job=true + reopen=true হলে job_status BROADCASTING হয়'
);

SELECT ok(
  (SELECT broadcast_timer_started_at FROM public.problems WHERE id = 'P31') > (now() - interval '1 minute'),
  'solver_cancel_job: instant job রিওপেন হলে broadcast_timer_started_at নতুন করে সেট হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- solver_cancel_job — p_reopen_as_open = false: status CANCELLED,
-- accepted_* অপরিবর্তিত থাকে (else-branch, escrow না থাকলে NO_ESCROW)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  accepted_bid_id, accepted_solver_id, accepted_amount
) VALUES (
  'P32', '11111111-1111-1111-1111-111111111111', 'Cancel no reopen', 'IN_PROGRESS', 'IN_PROGRESS', false, true,
  'B32', '22222222-2222-2222-2222-222222222222', 250
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.solver_cancel_job('P32', 'reason', false))->'refund'->>'result',
  'NO_ESCROW',
  'solver_cancel_job: এই problem-এ কোনো HELD escrow না থাকলে refund.result = NO_ESCROW'
);

SELECT results_eq(
  $$ SELECT status, accepted_bid_id, accepted_amount FROM public.problems WHERE id = 'P32' $$,
  $$ VALUES ('CANCELLED'::text, 'B32'::text, 250::numeric) $$,
  'solver_cancel_job: p_reopen_as_open=false হলে status CANCELLED হয় কিন্তু accepted_* অপরিবর্তিত থাকে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- solver_cancel_job — edge cases: NOT_AUTHORIZED, ALREADY_TERMINAL
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.solver_cancel_job('P32', 'x') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'solver_cancel_job: accepted solver ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.solver_cancel_job('P32', 'x'))->>'result',
  'ALREADY_TERMINAL',
  'solver_cancel_job: আগে থেকেই CANCELLED পোস্টে দ্বিতীয়বার কল করলে ALREADY_TERMINAL'
);
SELECT test.logout();

----------------------------------------------------------------------
-- expire_stale_instant_jobs — happy path (superuser/cron হিসেবে কল, client
-- থেকে না — দেখুন উপরের ফাইল-হেডার নোট)
--
-- ⚠️ এই সেশনে (Step 5, প্রথমবার real Postgres+pgTAP দিয়ে পুরো 01-07 চেইন
-- চালিয়ে) একটা real bug ধরা পড়েছে: test.logout() session role-কে 'anon'-এ
-- সেট করে দেয় (আগের কোনো login_as-এর 'রোল' GUC ওভাররাইট করে) এবং কখনো
-- postgres-এ ফেরত আসে না। expire_stale_instant_jobs() client(anon)/
-- authenticated থেকে EXECUTE revoke করা, তাই logout()-এর পরে সরাসরি এটা
-- কল করলে role এখনো 'anon' থাকায় "permission denied for function
-- expire_stale_instant_jobs" দিয়ে ভেঙে পড়ে — ফাইলের হেডার-কমেন্ট
-- (উপরে) ভুলভাবে ধরে নিয়েছিল এই কলটা "superuser হিসেবে" চলছে, আসলে
-- চলছিল না। ফিক্স: RESET ROLE দিয়ে session-এর আসল login role (postgres,
-- এই script যে role দিয়ে চলছে)-এ ফিরিয়ে আনা, cron/superuser sweep-এর
-- বাস্তব আচরণের সাথে মিলিয়ে।
RESET ROLE;

INSERT INTO public.platform_settings (key, value) VALUES ('instant_job_broadcast_timeout_seconds', '300')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  is_user_deleted, broadcast_timer_started_at
) VALUES (
  'P33', '11111111-1111-1111-1111-111111111111', 'Stale broadcast', 'OPEN', 'BROADCASTING', true, true,
  false, now() - interval '10 minutes'
);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B33', 'P33', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 350, 'PENDING');

-- একটা এখনো তরতাজা (timeout-এর মধ্যে) broadcasting job — expire হওয়া উচিত না
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  is_user_deleted, broadcast_timer_started_at
) VALUES (
  'P34', '11111111-1111-1111-1111-111111111111', 'Fresh broadcast', 'OPEN', 'BROADCASTING', true, true,
  false, now()
);

SELECT is(
  (public.expire_stale_instant_jobs())->>'expired_count',
  '1',
  'expire_stale_instant_jobs: শুধু timeout পার হওয়া একটা broadcasting job-ই expire হয় (তাজাটা না)'
);

SELECT results_eq(
  $$ SELECT status, job_status, is_user_deleted FROM public.problems WHERE id = 'P33' $$,
  $$ VALUES ('CANCELLED'::text, 'CANCELLED'::text, true) $$,
  'expire_stale_instant_jobs: stale problem CANCELLED + is_user_deleted=true হয়ে যায়'
);

SELECT results_eq(
  $$ SELECT status, resolution_type FROM public.bids WHERE id = 'B33' $$,
  $$ VALUES ('CANCELLED'::text, 'EXPIRED'::text) $$,
  'expire_stale_instant_jobs: সংশ্লিষ্ট PENDING bid CANCELLED + resolution_type=EXPIRED হয়ে যায়'
);

SELECT is(
  (SELECT job_status FROM public.problems WHERE id = 'P34'),
  'BROADCASTING',
  'expire_stale_instant_jobs: timeout-এর মধ্যে থাকা তাজা broadcasting job অপরিবর্তিত থাকে'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE related_problem_id = 'P33' AND user_id = '22222222-2222-2222-2222-222222222222'
  ),
  'expire_stale_instant_jobs: bid দেওয়া solver-কে "জব বাতিল" নোটিফিকেশন যায়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE related_problem_id = 'P33' AND user_id = '11111111-1111-1111-1111-111111111111'
  ),
  'expire_stale_instant_jobs: পোস্ট owner-কেও "পোস্ট বাতিল" নোটিফিকেশন যায়'
);

----------------------------------------------------------------------
-- clear_solver_cancelled_notice — happy path
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  accepted_bid_id, accepted_solver_id, accepted_solver_name, accepted_amount, solver_cancelled_notice
) VALUES (
  'P35', '11111111-1111-1111-1111-111111111111', 'Re-broadcast me', 'CANCELLED', 'CANCELLED', true, true,
  'B35', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 400, 'সলভার বাতিল করেছেন'
);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES
  ('B35', 'P35', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 400, 'ACCEPTED'),
  ('B36', 'P35', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 380, 'PENDING');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.clear_solver_cancelled_notice('P35'))->>'result',
  'OK',
  'clear_solver_cancelled_notice: problem owner কল করলে OK'
);

SELECT results_eq(
  $$ SELECT solver_cancelled_notice, accepted_bid_id, accepted_solver_id, accepted_amount FROM public.problems WHERE id = 'P35' $$,
  $$ VALUES (NULL::text, NULL::text, NULL::uuid, NULL::numeric) $$,
  'clear_solver_cancelled_notice: notice + সব accepted_* ফিল্ড null হয়ে যায়'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B35'),
  'CANCELLED',
  'clear_solver_cancelled_notice: আগের accepted (এখন ACCEPTED) বিড CANCELLED হয়ে যায়'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B36'),
  'PENDING',
  'clear_solver_cancelled_notice: অসম্পর্কিত অন্য solver-এর PENDING বিড অপরিবর্তিত থাকে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- clear_solver_cancelled_notice — edge case: NOT_AUTHORIZED
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public)
VALUES ('P37', '11111111-1111-1111-1111-111111111111', 'Not yours', 'CANCELLED', true, true);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.clear_solver_cancelled_notice('P37') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'clear_solver_cancelled_notice: owner ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- update_solver_live_location — happy path
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id)
VALUES ('P38', '11111111-1111-1111-1111-111111111111', 'Live GPS', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222');

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.update_solver_live_location('P38', 23.81, 90.41))->>'result',
  'OK',
  'update_solver_live_location: accepted solver কল করলে OK'
);

SELECT results_eq(
  $$ SELECT solver_live_lat, solver_live_lng FROM public.problems WHERE id = 'P38' $$,
  $$ VALUES (23.81::numeric, 90.41::numeric) $$,
  'update_solver_live_location: lat/lng ঠিকভাবে আপডেট হয়'
);

SELECT ok(
  (SELECT solver_live_updated_at FROM public.problems WHERE id = 'P38') IS NOT NULL,
  'update_solver_live_location: solver_live_updated_at সেট হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- update_solver_live_location — edge cases: NOT_AUTHORIZED, ALREADY_TERMINAL
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.update_solver_live_location('P38', 1, 1) $$,
  'P0001',
  'NOT_AUTHORIZED',
  'update_solver_live_location: accepted solver ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id)
VALUES ('P39', '11111111-1111-1111-1111-111111111111', 'Done job', 'COMPLETED', false, true, '22222222-2222-2222-2222-222222222222');

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.update_solver_live_location('P39', 1, 1))->>'result',
  'ALREADY_TERMINAL',
  'update_solver_live_location: COMPLETED/CANCELLED problem-এ ALREADY_TERMINAL'
);
SELECT test.logout();

----------------------------------------------------------------------
-- sync_solver_free_job_quota — happy path (caller = problem owner, acceptBid path)
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public, accepted_solver_id)
VALUES ('P40', '11111111-1111-1111-1111-111111111111', 'Quota sync', 'IN_PROGRESS', false, true, '22222222-2222-2222-2222-222222222222');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.sync_solver_free_job_quota('P40', '22222222-2222-2222-2222-222222222222', 3, '2026-09'))->>'result',
  'OK',
  'sync_solver_free_job_quota: problem owner (acceptBid-এর caller) কল করলে OK'
);

SELECT results_eq(
  $$ SELECT free_jobs_used_this_month, free_jobs_month_key FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES (3::integer, '2026-09'::text) $$,
  'sync_solver_free_job_quota: solver-এর quota কাউন্টার + month key ঠিকভাবে আপডেট হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- sync_solver_free_job_quota — caller = solver নিজে (acceptDirectContractProposal path)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.sync_solver_free_job_quota('P40', '22222222-2222-2222-2222-222222222222', 4, '2026-09'))->>'result',
  'OK',
  'sync_solver_free_job_quota: solver নিজে কল করলেও (caller=p_solver_id) OK'
);
SELECT test.logout();

----------------------------------------------------------------------
-- sync_solver_free_job_quota — edge cases: SOLVER_MISMATCH, NOT_AUTHORIZED
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.sync_solver_free_job_quota('P40', '33333333-3333-3333-3333-333333333333', 1, '2026-09') $$,
  'P0001',
  'SOLVER_MISMATCH',
  'sync_solver_free_job_quota: p_solver_id প্রকৃত accepted_solver_id না হলে SOLVER_MISMATCH'
);
SELECT test.logout();

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.sync_solver_free_job_quota('P40', '22222222-2222-2222-2222-222222222222', 1, '2026-09') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'sync_solver_free_job_quota: caller owner/সংশ্লিষ্ট solver/admin কেউ না হলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
