-- 05_job_release_escrow_part1.sql — Step 3 (Job release & escrow), PART 1 of 2
--
-- এই ফাইলে কভার করা ৫টা ফাংশন:
--   request_job_release, cancel_job_release_request, reject_job_release_request
--       (supabase/migrations/recovered_job_release.sql)
--   release_escrow, refund_escrow_once
--       (supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql —
--        recovered_money_flow.sql-এর পুরোনো সংজ্ঞা override করে, তাই এটাই চূড়ান্ত body)
--
-- PART 2-এর কাজ (এই ফাইলে নেই): request_extra_amount, respond_additional_charge,
-- increment_escrow_extra_amount, record_gateway_payment_log,
-- mark_additional_charge_settled, system_track_extra_payment_miss।
--
-- ইচ্ছাকৃতভাবে টেস্ট না করা branch (FK-র কারণে stub-এ অপ্রাপ্য): release_escrow-এর
-- SOLVER_NOT_FOUND, refund_escrow_once-এর USER_NOT_FOUND — এই দুটো তখনই হতো যদি
-- escrow.solver_id/user_id-র users row না থাকত, কিন্তু escrows FK সেটা আটকায়।
--
-- সব setup ডেটা একদম শুরুতে (superuser হিসেবে, login_as-এর আগে) বসানো হয়েছে।
-- সব id এই ফাইলে ইউনিক (P50.. / E80.. / E90..) যাতে অন্য ফাইলের সাথে ধাক্কা না লাগে
-- (প্রতিটা ফাইল আলাদা BEGIN..ROLLBACK-এ চলে, তবু পরিষ্কার রাখার জন্য)।
--
-- test user ids (00_helpers.sql / test.seed_users):
--   CLIENT 1111…  SOLVER-1 2222…  SOLVER-2 3333…  ADMIN 9999…

BEGIN;
SELECT plan(68);

SELECT test.seed_users();

----------------------------------------------------------------------
-- SETUP (superuser)
----------------------------------------------------------------------
UPDATE public.users SET has_solver_role = true WHERE id = '22222222-2222-2222-2222-222222222222';
-- refund টেস্টের জন্য জানা শুরুর balance (owner)
UPDATE public.users SET balance = 100, balance_user = 100 WHERE id = '11111111-1111-1111-1111-111111111111';
-- USER_ROLE_INACTIVE টেস্টের জন্য: Solver-2 এর USER role নিষ্ক্রিয় (has_solver_role ডিফল্ট false, তাই
-- SOLVER_ROLE_INACTIVE টেস্টও এই ইউজার দিয়েই চলবে)
UPDATE public.users SET has_user_role = false WHERE id = '33333333-3333-3333-3333-333333333333';

-- ---- request_job_release ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, accepted_amount, user_name) VALUES
  ('P50', '11111111-1111-1111-1111-111111111111', 'Job50', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'Test Client'),
  ('P51', '11111111-1111-1111-1111-111111111111', 'Job51', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 400, 'Test Client'),
  ('P52', '11111111-1111-1111-1111-111111111111', 'Job52', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 300, 'Test Client'),
  ('P53', '11111111-1111-1111-1111-111111111111', 'Job53', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 300, 'Test Client');

-- ---- cancel_job_release_request ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, accepted_amount, user_name,
  has_release_request, release_request_extra_amount, release_request_note, release_requested_at)
VALUES
  ('P60', '11111111-1111-1111-1111-111111111111', 'Job60', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'Test Client',
   true, 100, 'n', now());

INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, accepted_amount, user_name,
  has_release_request, release_request_extra_amount, release_request_note, release_requested_at,
  is_disputed, dispute_reason, dispute_initiator_id, dispute_initiator_role, disputed_at,
  is_admin_involved_in_chat, admin_assistance_requested_by, admin_assistance_requested_at)
VALUES
  ('P61', '11111111-1111-1111-1111-111111111111', 'Job61', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'Test Client',
   true, 50, 'n', now(),
   true, 'quality issue', '11111111-1111-1111-1111-111111111111', 'USER', now(),
   true, '99999999-9999-9999-9999-999999999999', now());

-- ---- reject_job_release_request ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, accepted_amount, user_name,
  has_release_request, release_request_extra_amount, release_request_note, release_requested_at) VALUES
  ('P70', '11111111-1111-1111-1111-111111111111', 'Job70', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'Test Client', true, 100, 'n', now()),
  ('P71', '11111111-1111-1111-1111-111111111111', 'Job71', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'Test Client', true, 0, '', now());
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, user_name, has_release_request)
VALUES ('P72', '11111111-1111-1111-1111-111111111111', 'Job72', 'IN_PROGRESS', NULL, 'Test Client', true);

-- P70: escrow.extra_amount=100 (রিকোয়েস্টের সময় বাড়ানো), কিন্তু ACCEPTED charge মাত্র 60 → reject-এ 60-এ নামবে
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status) VALUES
  ('E70', 'P70', 'Job70', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 500, 100, 'HELD'),
  ('E71', 'P71', 'Job71', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 500, 0, 'HELD');
INSERT INTO public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status) VALUES
  ('AC70a', 'P70', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r1', 60,  'ACCEPTED'),
  ('AC70b', 'P70', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r2', 100, 'PENDING'),
  ('AC71a', 'P71', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r3', 30,  'ACCEPTED');

-- ---- release_escrow ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_amount, applied_commission_rate) VALUES
  ('P80', '11111111-1111-1111-1111-111111111111', 'Job80', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 400, 10.00),
  ('P81', '11111111-1111-1111-1111-111111111111', 'Job81', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 300, 10.00),
  ('P82', '11111111-1111-1111-1111-111111111111', 'Job82', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 200, NULL),
  ('P83', '11111111-1111-1111-1111-111111111111', 'Job83', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 100, 10.00),
  ('P84', '11111111-1111-1111-1111-111111111111', 'Job84', 'IN_PROGRESS', '33333333-3333-3333-3333-333333333333', 100, 10.00);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status) VALUES
  ('E80', 'P80', 'Job80', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 400, 100, 'HELD'),
  ('E81', 'P81', 'Job81', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 300, 0,   'HELD'),
  ('E82', 'P82', 'Job82', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 200, 0,   'HELD'),
  ('E83', 'P83', 'Job83', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 0,   'HELD'),
  ('E84', 'P84', 'Job84', '11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 100, 0,   'HELD');
-- E81: আগে থেকেই release-এর transaction আছে (ALREADY_RELEASED guard টেস্ট করার জন্য), কিন্তু escrow এখনো HELD
INSERT INTO public.transactions (id, problem_id, user_id, solver_id, gross_amount, net_amount, type, role, escrow_id)
VALUES ('TRX_RELEASE_E81', 'P81', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 300, 270, 'PAYMENT', 'SOLVER', 'E81');
-- E82 (applied_commission_rate NULL) → platform_settings-এর commission_percent fallback
INSERT INTO public.platform_settings (key, value) VALUES ('commission_percent', '15')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- ---- refund_escrow_once ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_amount) VALUES
  ('P90', '11111111-1111-1111-1111-111111111111', 'Job90', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 300),
  ('P91', '11111111-1111-1111-1111-111111111111', 'Job91', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 200),
  ('P92', '11111111-1111-1111-1111-111111111111', 'Job92', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 100),
  ('P93', '11111111-1111-1111-1111-111111111111', 'Job93', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 100),
  ('P94', '11111111-1111-1111-1111-111111111111', 'Job94', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 100),
  ('P95', '33333333-3333-3333-3333-333333333333', 'Job95', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 100);
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status) VALUES
  ('E90', 'P90', 'Job90', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 300, 50, 'HELD'),
  ('E91', 'P91', 'Job91', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 200, 0,  'HELD'),
  ('E92', 'P92', 'Job92', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 0,  'HELD'),
  ('E93', 'P93', 'Job93', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 0,  'HELD'),
  ('E94', 'P94', 'Job94', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 0,  'HELD'),
  ('E95', 'P95', 'Job95', '33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222222', 100, 0,  'HELD');
-- E92: আগে থেকেই refund transaction আছে (ALREADY_REFUNDED guard), escrow এখনো HELD
INSERT INTO public.transactions (id, problem_id, user_id, solver_id, gross_amount, net_amount, type, role, escrow_id)
VALUES ('TRX_REFUND_E92', 'P92', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 100, 'REFUND', 'USER', 'E92');

----------------------------------------------------------------------
-- request_job_release — happy path, extra=0 (solver নিজে কল করে)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.request_job_release('P50', 0, 'done'))->>'result',
  'OK',
  'request_job_release: accepted solver কল করলে OK'
);

SELECT results_eq(
  $$ SELECT has_release_request, release_request_extra_amount::numeric, release_request_note, release_requested_at IS NOT NULL
     FROM public.problems WHERE id = 'P50' $$,
  $$ VALUES (true, 0::numeric, 'done'::text, true) $$,
  'request_job_release: has_release_request=true, extra=0, note সেট, release_requested_at সেট'
);

SELECT is(
  (SELECT count(*) FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P50')::int,
  1,
  'request_job_release: owner-কে ঠিক ১টা notification যায়'
);

SELECT ok(
  (SELECT message FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P50') LIKE '%500.00%',
  'request_job_release: notification-এর মোট অংক = accepted_amount(500) + extra(0)'
);

SELECT is(
  (SELECT count(*) FROM public.additional_charges WHERE problem_id = 'P50')::int,
  0,
  'request_job_release: extra=0 হলে কোনো additional_charges row তৈরি হয় না'
);

----------------------------------------------------------------------
-- request_job_release — extra>0: PENDING additional_charge তৈরি হয় + charge_id ফেরত আসে
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'charge_id' LIKE 'EXTRA_%'
   FROM (SELECT public.request_job_release('P51', 100, 'extra parts') AS r) s),
  'request_job_release: extra>0 হলে result OK এবং EXTRA_ prefix-ওয়ালা charge_id ফেরত আসে'
);

SELECT results_eq(
  $$ SELECT status, amount::numeric, solver_id, user_id, reason FROM public.additional_charges WHERE problem_id = 'P51' $$,
  $$ VALUES ('PENDING'::text, 100::numeric, '22222222-2222-2222-2222-222222222222'::uuid, '11111111-1111-1111-1111-111111111111'::uuid, 'extra parts'::text) $$,
  'request_job_release: additional_charges row — PENDING, সঠিক amount/solver/user, reason = note'
);

SELECT is(
  (SELECT release_request_extra_amount FROM public.problems WHERE id = 'P51')::numeric,
  100::numeric,
  'request_job_release: problems.release_request_extra_amount = 100'
);

SELECT ok(
  (SELECT message FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P51') LIKE '%500.00%',
  'request_job_release: notification-এর মোট অংক = accepted_amount(400) + extra(100)'
);

----------------------------------------------------------------------
-- request_job_release — edge: negative extra clamp হয়ে 0, charge তৈরি হয় না
----------------------------------------------------------------------
SELECT is(
  (public.request_job_release('P52', -50, ''))->>'result',
  'OK',
  'request_job_release: negative extra দিলে ব্যর্থ না হয়ে OK আসে'
);

SELECT is(
  (SELECT release_request_extra_amount FROM public.problems WHERE id = 'P52')::numeric,
  0::numeric,
  'request_job_release: negative extra 0-তে clamp হয় (greatest(..., 0))'
);

SELECT is(
  (SELECT count(*) FROM public.additional_charges WHERE problem_id = 'P52')::int,
  0,
  'request_job_release: negative extra-এ কোনো additional_charges row তৈরি হয় না'
);

----------------------------------------------------------------------
-- request_job_release — edge: note খালি/হোয়াইটস্পেস + extra>0 → ডিফল্ট reason বসে
----------------------------------------------------------------------
SELECT is(
  (public.request_job_release('P53', 20, '   '))->>'result',
  'OK',
  'request_job_release: হোয়াইটস্পেস-শুধু note + extra>0 হলেও OK'
);

SELECT ok(
  (SELECT reason FROM public.additional_charges WHERE problem_id = 'P53') LIKE '%অতিরিক্ত বিল%',
  'request_job_release: note trim হয়ে খালি হলে additional_charges.reason-এ ডিফল্ট বাংলা টেক্সট বসে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_job_release — failure cases
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.request_job_release('P50', 0, 'x') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'request_job_release: accepted solver ছাড়া (owner সহ) কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.request_job_release('NOPE', 0, 'x') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'request_job_release: অস্তিত্বহীন problem_id-তে PROBLEM_NOT_FOUND'
);
SELECT test.logout();

----------------------------------------------------------------------
-- cancel_job_release_request — happy path (dispute নেই)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'was_disputed' = 'false'
   FROM (SELECT public.cancel_job_release_request('P60') AS r) s),
  'cancel_job_release_request: dispute না থাকলে result OK, was_disputed=false'
);

SELECT results_eq(
  $$ SELECT has_release_request, release_request_extra_amount::numeric, release_request_note, release_requested_at
     FROM public.problems WHERE id = 'P60' $$,
  $$ VALUES (false, 0::numeric, ''::text, NULL::timestamptz) $$,
  'cancel_job_release_request: release-request সংক্রান্ত সব ফিল্ড রিসেট হয়ে যায়'
);

SELECT is(
  (SELECT count(*) FROM public.notifications
   WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P60'
     AND title LIKE '%প্রত্যাহার করা হয়েছে%')::int,
  1,
  'cancel_job_release_request: owner-কে "রিলিজের অনুরোধ প্রত্যাহার" notification যায়'
);

----------------------------------------------------------------------
-- cancel_job_release_request — disputed case: dispute-ও বন্ধ হয়ে যায়
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'was_disputed' = 'true'
   FROM (SELECT public.cancel_job_release_request('P61') AS r) s),
  'cancel_job_release_request: disputed problem-এ result OK, was_disputed=true'
);

SELECT results_eq(
  $$ SELECT is_disputed, dispute_reason, dispute_initiator_id, is_admin_involved_in_chat, has_release_request
     FROM public.problems WHERE id = 'P61' $$,
  $$ VALUES (false, NULL::text, NULL::uuid, false, false) $$,
  'cancel_job_release_request: disputed হলে is_disputed/dispute_reason/initiator/admin-involvement সব রিসেট হয়'
);

SELECT is(
  (SELECT count(*) FROM public.notifications
   WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P61'
     AND title LIKE '%বিরোধ বন্ধ%')::int,
  1,
  'cancel_job_release_request: disputed হলে notification-এর title-এ "বিরোধ বন্ধ" থাকে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- cancel_job_release_request — failure cases
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.cancel_job_release_request('P60') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'cancel_job_release_request: accepted solver ছাড়া (owner সহ) কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.cancel_job_release_request('NOPE') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'cancel_job_release_request: অস্তিত্বহীন problem_id-তে PROBLEM_NOT_FOUND'
);
SELECT test.logout();

----------------------------------------------------------------------
-- reject_job_release_request — happy path: escrow.extra_amount ACCEPTED charge-এর sum-এ নামে
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.reject_job_release_request('P70', 'not good'))->>'result',
  'OK',
  'reject_job_release_request: owner কল করলে OK'
);

SELECT results_eq(
  $$ SELECT has_release_request, release_request_extra_amount::numeric, release_request_note, release_requested_at
     FROM public.problems WHERE id = 'P70' $$,
  $$ VALUES (false, 0::numeric, ''::text, NULL::timestamptz) $$,
  'reject_job_release_request: release-request ফিল্ডগুলো রিসেট হয়'
);

SELECT is(
  (SELECT extra_amount FROM public.escrows WHERE id = 'E70')::numeric,
  60::numeric,
  'reject_job_release_request: escrow.extra_amount ফিরে আসে শুধু ACCEPTED charge-এর যোগফলে (60, PENDING 100 বাদ)'
);

SELECT is(
  (SELECT count(*) FROM public.notifications
   WHERE user_id = '22222222-2222-2222-2222-222222222222' AND related_problem_id = 'P70'
     AND message LIKE '%not good%')::int,
  1,
  'reject_job_release_request: solver-কে notification যায়, message-এ দেওয়া reason থাকে'
);

----------------------------------------------------------------------
-- reject_job_release_request — edge: escrow.extra_amount = 0 হলে escrow অপরিবর্তিত;
-- reason NULL হলে message-এ "কারণ:" অংশ বসে না
----------------------------------------------------------------------
SELECT is(
  (public.reject_job_release_request('P71', NULL))->>'result',
  'OK',
  'reject_job_release_request: reason NULL দিলেও OK'
);

SELECT is(
  (SELECT extra_amount FROM public.escrows WHERE id = 'E71')::numeric,
  0::numeric,
  'reject_job_release_request: escrow.extra_amount আগে থেকেই 0 হলে ACCEPTED charge (30) থাকলেও escrow বদলায় না'
);

SELECT ok(
  (SELECT message FROM public.notifications WHERE user_id = '22222222-2222-2222-2222-222222222222' AND related_problem_id = 'P71') NOT LIKE '%কারণ:%',
  'reject_job_release_request: reason না দিলে notification message-এ "কারণ:" থাকে না'
);

----------------------------------------------------------------------
-- reject_job_release_request — edge: accepted_solver_id NULL → notification যায় না
----------------------------------------------------------------------
SELECT is(
  (public.reject_job_release_request('P72', 'x'))->>'result',
  'OK',
  'reject_job_release_request: accepted_solver_id null হলেও OK'
);

SELECT is(
  (SELECT count(*) FROM public.notifications WHERE related_problem_id = 'P72')::int,
  0,
  'reject_job_release_request: accepted_solver_id null হলে কোনো notification তৈরি হয় না'
);

SELECT test.logout();

----------------------------------------------------------------------
-- reject_job_release_request — failure cases
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.reject_job_release_request('P70', 'x') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'reject_job_release_request: owner ছাড়া (solver সহ) কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.reject_job_release_request('NOPE', 'x') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'reject_job_release_request: অস্তিত্বহীন problem_id-তে PROBLEM_NOT_FOUND'
);
SELECT test.logout();

----------------------------------------------------------------------
-- release_escrow — happy path (owner, applied_commission_rate=10):
-- gross = 400+100 = 500, commission 50, net 450
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'net_amount')::numeric = 450 AND (r->>'commission')::numeric = 50
   FROM (SELECT public.release_escrow('E80') AS r) s),
  'release_escrow: owner কল করলে OK, net_amount=450, commission=50 (gross 500 @10%)'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES (450::numeric, 450::numeric) $$,
  'release_escrow: solver-এর legacy balance এবং balance_solver দুটোই net (450) বাড়ে (dual-write)'
);

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'E80'),
  'RELEASED',
  'release_escrow: escrow status RELEASED হয়ে যায়'
);

SELECT results_eq(
  $$ SELECT status, completed_at IS NOT NULL FROM public.problems WHERE id = 'P80' $$,
  $$ VALUES ('COMPLETED'::text, true) $$,
  'release_escrow: problem status COMPLETED + completed_at সেট'
);

SELECT results_eq(
  $$ SELECT type, role, gross_amount::numeric, commission_percent::numeric, commission_amount::numeric,
            net_amount::numeric, base_amount::numeric, extra_amount::numeric, release_type, escrow_id
     FROM public.transactions WHERE id = 'TRX_RELEASE_E80' $$,
  $$ VALUES ('PAYMENT'::text, 'SOLVER'::text, 500::numeric, 10::numeric, 50::numeric,
             450::numeric, 400::numeric, 100::numeric, 'FULL'::text, 'E80'::text) $$,
  'release_escrow: TRX_RELEASE_E80 — PAYMENT/SOLVER role, gross/commission/net/base/extra সঠিক'
);

SELECT is(
  (SELECT count(*) FROM public.notifications
   WHERE user_id = '22222222-2222-2222-2222-222222222222' AND target_type = 'balance' AND message LIKE '%450.00%')::int,
  1,
  'release_escrow: solver-কে "পেমেন্ট প্রকাশিত" balance notification যায় (net 450.00)'
);

----------------------------------------------------------------------
-- release_escrow — idempotency: একই escrow দ্বিতীয়বার release হয় না
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_TERMINAL' AND r->>'status' = 'RELEASED'
   FROM (SELECT public.release_escrow('E80') AS r) s),
  'release_escrow: দ্বিতীয়বার কল করলে ALREADY_TERMINAL/RELEASED'
);

SELECT is(
  (SELECT balance_solver FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222')::numeric,
  450::numeric,
  'release_escrow: দ্বিতীয় কলে solver-এর balance_solver আবার বাড়ে না (double-pay নেই)'
);

----------------------------------------------------------------------
-- release_escrow — guard: escrow HELD কিন্তু TRX_RELEASE_<id> আগে থেকেই আছে → ALREADY_RELEASED
----------------------------------------------------------------------
SELECT is(
  (public.release_escrow('E81'))->>'result',
  'ALREADY_RELEASED',
  'release_escrow: transaction আগে থেকেই থাকলে ALREADY_RELEASED'
);

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'E81'),
  'HELD',
  'release_escrow: ALREADY_RELEASED guard-এ escrow status বদলায় না'
);

SELECT is(
  (SELECT balance_solver FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222')::numeric,
  450::numeric,
  'release_escrow: ALREADY_RELEASED guard-এ balance বদলায় না'
);

SELECT test.logout();

----------------------------------------------------------------------
-- release_escrow — admin-ও পারে + commission fallback:
-- problem.applied_commission_rate NULL হলে platform_settings.commission_percent (15) ব্যবহার হয়।
-- gross 200 → commission 30, net 170
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'net_amount')::numeric = 170 AND (r->>'commission')::numeric = 30
   FROM (SELECT public.release_escrow('E82') AS r) s),
  'release_escrow: admin কল করলে OK; applied_commission_rate NULL হলে platform_settings (15%) — net 170, commission 30'
);

SELECT is(
  (SELECT commission_percent FROM public.transactions WHERE id = 'TRX_RELEASE_E82')::numeric,
  15::numeric,
  'release_escrow: transaction-এ commission_percent = 15 (fallback থেকে)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- release_escrow — failure cases
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.release_escrow('E83') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'release_escrow: owner/admin ছাড়া (solver নিজেও) কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.release_escrow('NOPE') $$,
  'P0001',
  'ESCROW_NOT_FOUND',
  'release_escrow: অস্তিত্বহীন escrow-তে ESCROW_NOT_FOUND'
);

SELECT throws_ok(
  $$ SELECT public.release_escrow('E84') $$,
  'P0001',
  'SOLVER_ROLE_INACTIVE',
  'release_escrow: solver-এর has_solver_role=false হলে SOLVER_ROLE_INACTIVE (payout আটকায়)'
);
SELECT test.logout();

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'E84'),
  'HELD',
  'release_escrow: SOLVER_ROLE_INACTIVE-এ ব্যর্থ হলে escrow HELD-ই থাকে'
);

----------------------------------------------------------------------
-- refund_escrow_once — happy path (owner, ডিফল্ট আর্গুমেন্ট = SOLVER_CANCEL, 100%):
-- (300+50) = 350 ফেরত, owner-এর balance 100 → 450
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'amount')::numeric = 350
   FROM (SELECT public.refund_escrow_once('E90') AS r) s),
  'refund_escrow_once: owner কল করলে OK, amount = base+extra = 350'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (450::numeric, 450::numeric) $$,
  'refund_escrow_once: owner-এর legacy balance ও balance_user দুটোই 350 বাড়ে (100 → 450)'
);

SELECT results_eq(
  $$ SELECT status, released_at IS NOT NULL FROM public.escrows WHERE id = 'E90' $$,
  $$ VALUES ('REFUNDED'::text, true) $$,
  'refund_escrow_once: escrow REFUNDED + released_at সেট'
);

SELECT results_eq(
  $$ SELECT type, role, gross_amount::numeric, net_amount::numeric, refund_type, refund_percentage::numeric, escrow_id
     FROM public.transactions WHERE id = 'TRX_REFUND_E90' $$,
  $$ VALUES ('REFUND'::text, 'USER'::text, 350::numeric, 350::numeric, 'SOLVER_CANCEL'::text, 100::numeric, 'E90'::text) $$,
  'refund_escrow_once: TRX_REFUND_E90 — REFUND/USER role, ডিফল্ট refund_type=SOLVER_CANCEL, 100%'
);

SELECT is(
  (SELECT count(*) FROM public.notifications
   WHERE user_id = '11111111-1111-1111-1111-111111111111' AND target_type = 'balance' AND message LIKE '%350.00%')::int,
  1,
  'refund_escrow_once: owner-কে "রিফান্ড সম্পন্ন" balance notification যায় (350.00)'
);

----------------------------------------------------------------------
-- refund_escrow_once — idempotency
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_TERMINAL' AND r->>'status' = 'REFUNDED'
   FROM (SELECT public.refund_escrow_once('E90') AS r) s),
  'refund_escrow_once: দ্বিতীয়বার কল করলে ALREADY_TERMINAL/REFUNDED'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111')::numeric,
  450::numeric,
  'refund_escrow_once: দ্বিতীয় কলে balance_user আবার বাড়ে না (double-refund নেই)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- refund_escrow_once — partial refund, solver নিজে কল করে (solver-ও authorized):
-- 200 × 50% = 100, refund_type/percentage transaction-এ লেখা হয়
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'amount')::numeric = 100
   FROM (SELECT public.refund_escrow_once('E91', 'PARTIAL', 50) AS r) s),
  'refund_escrow_once: solver কল করলে OK; 50% partial refund = 100'
);

SELECT results_eq(
  $$ SELECT refund_type, refund_percentage::numeric, net_amount::numeric FROM public.transactions WHERE id = 'TRX_REFUND_E91' $$,
  $$ VALUES ('PARTIAL'::text, 50::numeric, 100::numeric) $$,
  'refund_escrow_once: transaction-এ refund_type=PARTIAL, refund_percentage=50, net=100'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111')::numeric,
  550::numeric,
  'refund_escrow_once: partial refund-এ owner-এর balance_user 450 → 550'
);

SELECT test.logout();

----------------------------------------------------------------------
-- refund_escrow_once — guard: TRX_REFUND_<id> আগে থেকেই আছে, escrow এখনো HELD
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.refund_escrow_once('E92'))->>'result',
  'ALREADY_REFUNDED',
  'refund_escrow_once: transaction আগে থেকেই থাকলে ALREADY_REFUNDED'
);

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'E92'),
  'HELD',
  'refund_escrow_once: ALREADY_REFUNDED guard-এ escrow status বদলায় না'
);

SELECT test.logout();

----------------------------------------------------------------------
-- refund_escrow_once — admin-ও পারে
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT is(
  (public.refund_escrow_once('E93'))->>'result',
  'OK',
  'refund_escrow_once: admin কল করলে OK'
);

SELECT test.logout();

----------------------------------------------------------------------
-- refund_escrow_once — failure cases
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.refund_escrow_once('E94') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'refund_escrow_once: escrow-র owner/solver/admin কেউ না হলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.refund_escrow_once('NOPE') $$,
  'P0001',
  'ESCROW_NOT_FOUND',
  'refund_escrow_once: অস্তিত্বহীন escrow-তে ESCROW_NOT_FOUND'
);

SELECT throws_ok(
  $$ SELECT public.refund_escrow_once('E95') $$,
  'P0001',
  'USER_ROLE_INACTIVE',
  'refund_escrow_once: escrow.user_id-র has_user_role=false হলে USER_ROLE_INACTIVE (refund আটকায়)'
);
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
