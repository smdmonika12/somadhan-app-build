-- 03_instant_jobs_lifecycle.sql — Step 2 (Instant jobs / broadcasting), PART 1 of 2
--
-- এই ফাইলে কভার করা হয়েছে: broadcast_instant_job, cancel_instant_job,
-- expire_broadcasting_instant_job, mark_job_started, mark_solver_arrived,
-- mark_solver_on_way — সবগুলোই supabase/migrations/recovered_instant_jobs.sql
-- থেকে verify করা।
--
-- এই ধাপ ইচ্ছাকৃতভাবে অসম্পূর্ণ (PART 1 of 2) — বাকি ৫টা ফাংশন
-- (solver_cancel_job, expire_stale_instant_jobs, clear_solver_cancelled_notice,
-- update_solver_live_location, sync_solver_free_job_quota)
-- 04_instant_jobs_part2.sql-এ পরের সেশনে কভার হবে। বিস্তারিত কারণ ও
-- context CI_TEST_SUITE_PROGRESS.md-তে "Step 2 — PART 1" নোটে।
--
-- ⚠️ এই টেস্টগুলো 01_bidding_flow_schema_stub.sql-এর (এই সেশনে extend করা)
-- inferred/TEMPORARY schema-র উপর নির্ভরশীল।

BEGIN;
SELECT plan(30);

SELECT test.seed_users();

-- test user ids (00_helpers.sql / test.seed_users থেকে)
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...

-- broadcast_instant_job-এর "eligible solver" query solver_categories/location/
-- notification-flag ব্যবহার করে, যেগুলো seed_users সেট করে না — তাই এখানে
-- আলাদাভাবে সেট করা হচ্ছে (00_helpers.sql শেয়ার্ড, এই ফাইল-স্কোপড দরকার তাতে
-- হাত না দিয়ে)।
UPDATE public.users SET
  has_solver_role = true,
  instant_job_notifications_enabled = true,
  solver_categories = 'plumbing',
  latitude = 23.8000, longitude = 90.4000
WHERE id = '22222222-2222-2222-2222-222222222222'; -- Solver One: matching category, কাছের লোকেশন

UPDATE public.users SET
  has_solver_role = true,
  instant_job_notifications_enabled = true,
  solver_categories = 'plumbing',
  latitude = 24.9000, longitude = 91.8000  -- Sylhet-ish, ঢাকা থেকে বহুদূর -> radius-এর বাইরে
WHERE id = '33333333-3333-3333-3333-333333333333'; -- Solver Two: category মিললেও দূরত্বের কারণে বাদ পড়া উচিত

----------------------------------------------------------------------
-- broadcast_instant_job — happy path (category + radius দুটোই মিলছে এমন
-- একজন solver-কেই notification যাওয়া উচিত, দূরের জনকে না)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, user_name, title, status, is_instant_job, is_public,
  category_id, category_name, min_budget, max_budget,
  broadcast_radius_km, latitude, longitude
) VALUES (
  'P20', '11111111-1111-1111-1111-111111111111', 'Test Client', 'Leaking pipe', 'OPEN', true, true,
  'plumbing', 'Plumbing', 500, 500,
  10.0, 23.8010, 90.4010
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.broadcast_instant_job('P20'))->>'result',
  'OK',
  'broadcast_instant_job: is_instant_job=true পোস্টের owner কল করলে OK'
);

SELECT is(
  (public.broadcast_instant_job('P20'))->>'notified_count',
  '1',
  'broadcast_instant_job: category+radius দুটোই মেলা একজন solver-কেই notified_count ধরে (দ্বিতীয় কলে ফের ১ notification যায়)'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE related_problem_id = 'P20' AND user_id = '22222222-2222-2222-2222-222222222222'
  ),
  'broadcast_instant_job: কাছের, category-matching solver (Solver One) notification পায়'
);

SELECT ok(
  NOT EXISTS (
    SELECT 1 FROM public.notifications
    WHERE related_problem_id = 'P20' AND user_id = '33333333-3333-3333-3333-333333333333'
  ),
  'broadcast_instant_job: category মিললেও radius-এর বাইরে থাকা solver (Solver Two) notification পায় না'
);

SELECT test.logout();

----------------------------------------------------------------------
-- broadcast_instant_job — edge cases: NOT_AUTHORIZED, NOT_INSTANT_JOB
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.broadcast_instant_job('P20') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'broadcast_instant_job: owner ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public)
VALUES ('P21', '11111111-1111-1111-1111-111111111111', 'Not instant', 'OPEN', false, true);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.broadcast_instant_job('P21') $$,
  'P0001',
  'NOT_INSTANT_JOB',
  'broadcast_instant_job: is_instant_job=false পোস্টে NOT_INSTANT_JOB'
);
SELECT test.logout();

----------------------------------------------------------------------
-- cancel_instant_job — happy path
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public,
  accepted_solver_id
) VALUES (
  'P22', '11111111-1111-1111-1111-111111111111', 'Cancel me', 'IN_PROGRESS', 'IN_PROGRESS', true, true,
  '22222222-2222-2222-2222-222222222222'
);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B22', 'P22', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 400, 'ACCEPTED');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.cancel_instant_job('P22', 'পরিকল্পনা বদলেছে', 2))->>'result',
  'OK',
  'cancel_instant_job: owner কল করলে OK'
);

SELECT is(
  (SELECT status FROM public.problems WHERE id = 'P22'),
  'CANCELLED',
  'cancel_instant_job: problem status CANCELLED হয়ে যায়'
);

SELECT is(
  (SELECT job_status FROM public.problems WHERE id = 'P22'),
  'CANCELLED',
  'cancel_instant_job: job_status ও CANCELLED হয়ে যায়'
);

SELECT is(
  (SELECT accepted_bid_id FROM public.problems WHERE id = 'P22'),
  NULL,
  'cancel_instant_job: accepted_bid_id null হয়ে যায়'
);

SELECT results_eq(
  $$ SELECT status, resolution_type, progress_at_cancel FROM public.bids WHERE id = 'B22' $$,
  $$ VALUES ('CANCELLED'::text, 'USER_CANCEL'::text, 2::integer) $$,
  'cancel_instant_job: bid CANCELLED + resolution_type=USER_CANCEL + progress_at_cancel সঠিক ধাপে সেট হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- cancel_instant_job — edge cases: NOT_AUTHORIZED, ALREADY_TERMINAL
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.cancel_instant_job('P21') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'cancel_instant_job: owner ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.cancel_instant_job('P22'))->>'result',
  'ALREADY_TERMINAL',
  'cancel_instant_job: আগে থেকেই CANCELLED পোস্টে দ্বিতীয়বার কল করলে ALREADY_TERMINAL'
);
SELECT test.logout();

----------------------------------------------------------------------
-- expire_broadcasting_instant_job — happy path
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, job_status, is_instant_job, is_public)
VALUES ('P23', '11111111-1111-1111-1111-111111111111', 'Nobody bid', 'OPEN', 'BROADCASTING', true, true);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B23', 'P23', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 300, 'PENDING');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.expire_broadcasting_instant_job('P23'))->>'result',
  'OK',
  'expire_broadcasting_instant_job: BROADCASTING অবস্থায় owner কল করলে OK'
);

SELECT results_eq(
  $$ SELECT status, job_status, is_user_deleted FROM public.problems WHERE id = 'P23' $$,
  $$ VALUES ('CANCELLED'::text, 'CANCELLED'::text, true) $$,
  'expire_broadcasting_instant_job: problem CANCELLED + is_user_deleted=true হয়ে যায়'
);

SELECT results_eq(
  $$ SELECT status, resolution_type FROM public.bids WHERE id = 'B23' $$,
  $$ VALUES ('CANCELLED'::text, 'EXPIRED'::text) $$,
  'expire_broadcasting_instant_job: PENDING bid CANCELLED + resolution_type=EXPIRED হয়ে যায়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- expire_broadcasting_instant_job — edge cases: NOT_AUTHORIZED, NOT_BROADCASTING
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, job_status, is_instant_job, is_public)
VALUES ('P24', '11111111-1111-1111-1111-111111111111', 'Still broadcasting', 'OPEN', 'BROADCASTING', true, true);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.expire_broadcasting_instant_job('P24') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'expire_broadcasting_instant_job: owner ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.expire_broadcasting_instant_job('P22'))->>'result',
  'NOT_BROADCASTING',
  'expire_broadcasting_instant_job: job_status BROADCASTING না হলে (এখানে P22 আগে থেকেই CANCELLED) NOT_BROADCASTING'
);
SELECT test.logout();

----------------------------------------------------------------------
-- mark_job_started / mark_solver_arrived / mark_solver_on_way — common setup
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, job_status, is_instant_job, is_public, accepted_solver_id, accepted_solver_name
) VALUES (
  'P25', '11111111-1111-1111-1111-111111111111', 'On the way test', 'IN_PROGRESS', 'ACCEPTED', true, true,
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

----------------------------------------------------------------------
-- mark_solver_on_way — happy path + NOT_AUTHORIZED
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.mark_solver_on_way('P25'))->>'result',
  'OK',
  'mark_solver_on_way: accepted solver কল করলে OK'
);

SELECT is(
  (SELECT job_status FROM public.problems WHERE id = 'P25'),
  'ON_THE_WAY',
  'mark_solver_on_way: job_status ON_THE_WAY হয়ে যায়'
);

SELECT ok(
  (SELECT on_way_at FROM public.problems WHERE id = 'P25') IS NOT NULL,
  'mark_solver_on_way: on_way_at টাইমস্ট্যাম্প সেট হয়'
);

SELECT test.logout();

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.mark_solver_on_way('P25') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'mark_solver_on_way: accepted solver ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- mark_solver_arrived — happy path + NOT_AUTHORIZED
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.mark_solver_arrived('P25'))->>'result',
  'OK',
  'mark_solver_arrived: accepted solver কল করলে OK'
);

SELECT is(
  (SELECT job_status FROM public.problems WHERE id = 'P25'),
  'ARRIVED',
  'mark_solver_arrived: job_status ARRIVED হয়ে যায়'
);

SELECT ok(
  (SELECT arrived_at FROM public.problems WHERE id = 'P25') IS NOT NULL,
  'mark_solver_arrived: arrived_at টাইমস্ট্যাম্প সেট হয়'
);

SELECT test.logout();

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.mark_solver_arrived('P25') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'mark_solver_arrived: accepted solver ছাড়া (এমনকি problem owner হলেও) NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- mark_job_started — happy path (+ coalesce/idempotency) + NOT_AUTHORIZED
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.mark_job_started('P25'))->>'result',
  'OK',
  'mark_job_started: accepted solver কল করলে OK'
);

SELECT results_eq(
  $$ SELECT status, job_status FROM public.problems WHERE id = 'P25' $$,
  $$ VALUES ('IN_PROGRESS'::text, 'IN_PROGRESS'::text) $$,
  'mark_job_started: status ও job_status দুটোই IN_PROGRESS হয়ে যায়'
);

-- দ্বিতীয়বার কল করলেও job_started_at বদলায় না (function-এ coalesce(job_started_at, v_now))
CREATE TEMP TABLE tmp_job_started_at AS
  SELECT job_started_at AS ts1 FROM public.problems WHERE id = 'P25';

SELECT public.mark_job_started('P25'); -- দ্বিতীয় কল

SELECT ok(
  (SELECT job_started_at FROM public.problems WHERE id = 'P25') = (SELECT ts1 FROM tmp_job_started_at),
  'mark_job_started: দ্বিতীয়বার কল করলেও job_started_at (coalesce-এর কারণে) অপরিবর্তিত থাকে'
);

SELECT test.logout();

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.mark_job_started('P25') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'mark_job_started: accepted solver ছাড়া (এমনকি problem owner হলেও) NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
