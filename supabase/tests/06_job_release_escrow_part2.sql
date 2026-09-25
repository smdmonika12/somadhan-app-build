-- 06_job_release_escrow_part2.sql — Step 3 (Job release & escrow), PART 2 of 2
--
-- এই ফাইলে কভার করা ৬টা ফাংশন (PART 1-এর বাকি ৬টা, Step 3 এখন সম্পূর্ণ):
--   request_extra_amount               (supabase/migrations/recovered_money_flow.sql —
--                                        grep করে নিশ্চিত হয়েছে কোনো পরের migration এটা override করে না)
--   respond_additional_charge          (supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql —
--                                        recovered_money_flow.sql-এর পুরোনো সংজ্ঞা override করে, এটাই চূড়ান্ত)
--   increment_escrow_extra_amount      (supabase/migrations/step32_7_increment_escrow_extra_amount.sql)
--   record_gateway_payment_log         (supabase/migrations/step32_7_record_gateway_payment_log.sql)
--   mark_additional_charge_settled     (supabase/migrations/step29_mark_additional_charge_settled.sql —
--                                        bookkeeping-only, ইচ্ছাকৃতভাবে কোনো balance/escrow ছোঁয় না, টেস্টেও তাই assert হলো)
--   system_track_extra_payment_miss    (supabase/migrations/step32_7_system_track_extra_payment_miss.sql)
--
-- request_additional_charge, user_confirm_extra_amount, user_reject_extra_amount —
-- এই তিনটা ইচ্ছাকৃতভাবে এখানে না (master prompt-এ Step 3-এর তালিকায় নেই, Step 10
-- gap-fill-এ যোগ হয়েছে — CI_TEST_SUITE_PROGRESS.md দেখুন)।
--
-- সব setup data superuser হিসেবে (login_as-এর আগে) বসানো হয়েছে। id গুলো এই ফাইলে
-- ইউনিক (P150../E150../AC150../GW180..) যাতে অন্য ফাইলের সাথে ধাক্কা না লাগে।
-- respond_additional_charge-এর balance-নির্ভর কেসগুলো ক্রমানুসারে client(1111)-এর
-- balance/balance_user-এর উপর cumulative effect ট্র্যাক করে লেখা হয়েছে (refund_escrow_once,
-- PART 1-এর মতোই প্যাটার্ন) — মন্তব্যে প্রতিটা ধাপে বর্তমান প্রত্যাশিত ব্যালেন্স লেখা আছে।
--
-- test user ids (00_helpers.sql / test.seed_users):
--   CLIENT 1111…  SOLVER-1 2222…  SOLVER-2 3333…  ADMIN 9999…
--
-- ⚠️⚠️ এই সেশনেও (আগের Step 3 PART 1 সেশনের মতোই) sandbox-এ network বন্ধ ছিল
-- (apt-get দিয়ে postgresql/pgtap ইনস্টল করার চেষ্টা 403 Forbidden দিয়েছে) —
-- তাই এই ফাইলটাও **আসলে চালিয়ে ok/not ok দেখা যায়নি**, শুধু static verification:
-- (ক) নিচের প্রতিটা function body সরাসরি migration ফাইল থেকে পড়ে assertion লেখা
-- হয়েছে (অনুমান না), (খ) assertion সংখ্যা স্ক্রিপ্ট দিয়ে গুনে plan()-এর সাথে মেলানো
-- হয়েছে (Step 2/3 PART 1-এ ধরা পড়া numeric-text-scale ও plan()-mismatch ভুল
-- এড়াতে), (গ) balance/escrow cumulative হিসাব হাতে trace করে করা হয়েছে।
-- **পরের সেশনের প্রথম কাজ: Postgres+pgTAP পাওয়া গেলে stub → 01,02,03,04,05,06
-- চালিয়ে দেখা** — Step 3 PART 1-এর 05_...sql ফাইলটাও এখনো real Postgres-এ
-- verify করা হয়নি (দুই সেশন ধরে network ব্লক), তাই ওটাও একই সাথে চালিয়ে দেখা জরুরি।

BEGIN;
SELECT plan(60);

SELECT test.seed_users();

----------------------------------------------------------------------
-- SETUP (superuser)
----------------------------------------------------------------------
-- respond_additional_charge/system_track_extra_payment_miss-এর জন্য
UPDATE public.users SET has_solver_role = true WHERE id = '22222222-2222-2222-2222-222222222222';
-- respond_additional_charge cumulative balance টেস্টের জন্য client-এর জানা শুরুর ব্যালেন্স
UPDATE public.users SET balance = 500, balance_user = 500 WHERE id = '11111111-1111-1111-1111-111111111111';
-- respond_additional_charge admin-path টেস্টের জন্য solver1-এর জানা শুরুর USER-role ব্যালেন্স
UPDATE public.users SET balance = 200, balance_user = 200 WHERE id = '22222222-2222-2222-2222-222222222222';
-- respond_additional_charge-এর USER_ROLE_INACTIVE টেস্টের জন্য solver2-এর USER role নিষ্ক্রিয়
UPDATE public.users SET has_user_role = false WHERE id = '33333333-3333-3333-3333-333333333333';

-- ---- request_extra_amount ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, user_name) VALUES
  ('P160', '11111111-1111-1111-1111-111111111111', 'JobExtra160', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 'Test Client'),
  ('P161', '11111111-1111-1111-1111-111111111111', 'JobExtra161', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 'Test Client');

-- ---- respond_additional_charge ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, user_name) VALUES
  ('P150', '11111111-1111-1111-1111-111111111111', 'JobRAC150', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P152', '11111111-1111-1111-1111-111111111111', 'JobRAC152', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P151', '11111111-1111-1111-1111-111111111111', 'JobRAC151', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P155', '33333333-3333-3333-3333-333333333333', 'JobRAC155', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver Two'),
  ('P157', '11111111-1111-1111-1111-111111111111', 'JobRAC157', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client');

INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status) VALUES
  ('E150', 'P150', 'JobRAC150', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 300, 0, 'HELD'),
  ('E151', 'P151', 'JobRAC151', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 0, 'HELD'),
  ('E157', 'P157', 'JobRAC157', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 0, 'HELD');
  -- P152-র জন্য ইচ্ছাকৃতভাবে কোনো escrow নেই ("no HELD escrow found" branch টেস্ট করার জন্য)

INSERT INTO public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status) VALUES
  ('AC150', 'P150', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r150', 60,   'PENDING'),
  ('AC152', 'P152', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r152', 30,   'PENDING'),
  ('AC151', 'P151', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r151', 1000, 'PENDING'),
  ('AC153', 'P150', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r153', 20,   'REJECTED'),
  ('AC154', 'P150', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r154', 5,    'PENDING'),
  ('AC155', 'P155', '22222222-2222-2222-2222-222222222222', '33333333-3333-3333-3333-333333333333', 'r155', 10,   'PENDING'),
  ('AC156', 'P150', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'r156', 15,   'PENDING'),
  ('AC157', 'P157', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'r157', 50,   'PENDING');

-- ---- increment_escrow_extra_amount ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, user_name) VALUES
  ('P170', '11111111-1111-1111-1111-111111111111', 'JobInc170', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client');
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status) VALUES
  ('E170', 'P170', 'JobInc170', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 100, 20, 'HELD');

-- ---- mark_additional_charge_settled ----
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, user_name) VALUES
  ('P190', '11111111-1111-1111-1111-111111111111', 'JobSettle190', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P191', '11111111-1111-1111-1111-111111111111', 'JobSettle191', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P192', '11111111-1111-1111-1111-111111111111', 'JobSettle192', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P193', '11111111-1111-1111-1111-111111111111', 'JobSettle193', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client'),
  ('P194', '11111111-1111-1111-1111-111111111111', 'JobSettle194', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Client');
INSERT INTO public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status) VALUES
  ('AC190', 'P190', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'set190', 40, 'PENDING'),
  ('AC191', 'P191', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'set191', 10, 'PENDING'),
  ('AC192', 'P192', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'set192', 10, 'PENDING'),
  ('AC193', 'P193', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'set193', 10, 'ACCEPTED'),
  ('AC194', 'P194', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'set194', 10, 'PENDING');

----------------------------------------------------------------------
-- request_extra_amount — happy path (accepted solver কল করে)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.request_extra_amount('P160', 150, 'extra materials'))->>'result',
  'OK',
  'request_extra_amount: accepted solver কল করলে OK'
);

SELECT results_eq(
  $$ SELECT pending_extra_amount::numeric, pending_extra_amount_note, pending_extra_amount_requested_at IS NOT NULL
     FROM public.problems WHERE id = 'P160' $$,
  $$ VALUES (150::numeric, 'extra materials'::text, true) $$,
  'request_extra_amount: pending_extra_amount=150, note সেট, requested_at সেট'
);

SELECT is(
  (SELECT count(*) FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P160')::int,
  1,
  'request_extra_amount: owner-কে ঠিক ১টা notification যায়'
);

SELECT ok(
  (SELECT message FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P160') LIKE '%৳150%'
  AND (SELECT message FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'P160') LIKE '%অতিরিক্ত বিল অনুরোধ%',
  'request_extra_amount: notification-এ ৳150 এবং "অতিরিক্ত বিল অনুরোধ" টেক্সট থাকে'
);

----------------------------------------------------------------------
-- request_extra_amount — edge: negative amount 0-তে clamp হয়
----------------------------------------------------------------------
SELECT is(
  (public.request_extra_amount('P161', -50, ''))->>'result',
  'OK',
  'request_extra_amount: negative amount দিলেও ব্যর্থ না হয়ে OK আসে'
);

SELECT is(
  (SELECT pending_extra_amount FROM public.problems WHERE id = 'P161')::numeric,
  0::numeric,
  'request_extra_amount: negative amount 0-তে clamp হয় (greatest(...,0))'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_extra_amount — failure cases
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.request_extra_amount('P160', 10, 'x') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'request_extra_amount: accepted solver ছাড়া (owner সহ) কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.request_extra_amount('NOPE', 10, 'x') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'request_extra_amount: অস্তিত্বহীন problem_id-তে PROBLEM_NOT_FOUND'
);
SELECT test.logout();

----------------------------------------------------------------------
-- respond_additional_charge — happy path accept, escrow পাওয়া যায়
-- (client: balance/balance_user শুরু 500 → 60 কর্তনের পর 440)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'status' = 'ACCEPTED' AND (r->>'wallet_deduction')::numeric = 60
   FROM (SELECT public.respond_additional_charge('AC150', true) AS r) s),
  'respond_additional_charge: happy accept — result OK, status ACCEPTED, wallet_deduction=60'
);

SELECT results_eq(
  $$ SELECT status, responded_at IS NOT NULL FROM public.additional_charges WHERE id = 'AC150' $$,
  $$ VALUES ('ACCEPTED'::text, true) $$,
  'respond_additional_charge: additional_charges.status=ACCEPTED, responded_at সেট'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (440::numeric, 440::numeric) $$,
  'respond_additional_charge: owner-এর balance ও balance_user দুটোই 60 কমে (500 → 440)'
);

SELECT is(
  (SELECT extra_amount FROM public.escrows WHERE id = 'E150')::numeric,
  60::numeric,
  'respond_additional_charge: escrow E150.extra_amount 0 → 60'
);

SELECT is(
  (SELECT confirmed_extra_amount_total FROM public.problems WHERE id = 'P150')::numeric,
  60::numeric,
  'respond_additional_charge: problems.confirmed_extra_amount_total 0 → 60'
);

SELECT results_eq(
  $$ SELECT type, gross_amount::numeric, net_amount::numeric, role FROM public.transactions WHERE id = 'TRX_EXTRA_CHARGE_AC150' $$,
  $$ VALUES ('EXTRA_CHARGE_DEDUCTION'::text, 60::numeric, -60::numeric, 'USER'::text) $$,
  'respond_additional_charge: TRX_EXTRA_CHARGE_AC150 — EXTRA_CHARGE_DEDUCTION/USER role, gross=60 net=-60'
);

----------------------------------------------------------------------
-- respond_additional_charge — escrow না থাকলে সেই আপডেট skip হয়, কিন্তু wallet ঠিকই কাটে
-- (balance 440 → 410, confirmed_extra_amount_total P152-তে 0-ই থাকে)
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'wallet_deduction')::numeric = 30
   FROM (SELECT public.respond_additional_charge('AC152', true) AS r) s),
  'respond_additional_charge: escrow না থাকলেও OK, wallet_deduction=30'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111')::numeric,
  410::numeric,
  'respond_additional_charge: escrow না থাকলেও wallet ঠিকই কাটে (440 → 410)'
);

SELECT is(
  (SELECT confirmed_extra_amount_total FROM public.problems WHERE id = 'P152')::numeric,
  0::numeric,
  'respond_additional_charge: escrow না পাওয়া গেলে confirmed_extra_amount_total অপরিবর্তিত থাকে'
);

----------------------------------------------------------------------
-- respond_additional_charge — wallet_deduction remaining balance-এ clamp হয়
-- (charge amount 1000 > balance 410 → শুধু 410 কাটে, balance_user 0 হয়ে যায়)
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'wallet_deduction')::numeric = 410
   FROM (SELECT public.respond_additional_charge('AC151', true) AS r) s),
  'respond_additional_charge: charge amount balance-এর চেয়ে বেশি হলে wallet_deduction remaining balance-এ clamp হয় (410)'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111')::numeric,
  0::numeric,
  'respond_additional_charge: clamp-এর পর balance_user 0'
);

SELECT is(
  (SELECT extra_amount FROM public.escrows WHERE id = 'E151')::numeric,
  410::numeric,
  'respond_additional_charge: escrow E151.extra_amount clamp হওয়া অংক (410) দিয়ে বাড়ে'
);

----------------------------------------------------------------------
-- respond_additional_charge — already responded guard
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_RESPONDED' AND r->>'status' = 'REJECTED'
   FROM (SELECT public.respond_additional_charge('AC153', true) AS r) s),
  'respond_additional_charge: status PENDING না হলে ALREADY_RESPONDED + বর্তমান status ফেরত আসে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- respond_additional_charge — reject path (p_accept=false), কোনো টাকা নড়ে না
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'status' = 'REJECTED' AND (r->>'wallet_deduction')::numeric = 0
   FROM (SELECT public.respond_additional_charge('AC156', false) AS r) s),
  'respond_additional_charge: p_accept=false হলে status REJECTED, wallet_deduction=0'
);

SELECT is(
  (SELECT status FROM public.additional_charges WHERE id = 'AC156'),
  'REJECTED',
  'respond_additional_charge: additional_charges.status=REJECTED (reject path)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- respond_additional_charge — failure cases
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.respond_additional_charge('NOPE_AC', true) $$,
  'P0001',
  'CHARGE_NOT_FOUND',
  'respond_additional_charge: অস্তিত্বহীন charge_id-তে CHARGE_NOT_FOUND'
);
SELECT test.logout();

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.respond_additional_charge('AC154', true) $$,
  'P0001',
  'NOT_AUTHORIZED',
  'respond_additional_charge: charge.user_id/admin ছাড়া কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.respond_additional_charge('AC155', true) $$,
  'P0001',
  'USER_ROLE_INACTIVE',
  'respond_additional_charge: charge.user_id-র has_user_role=false হলে USER_ROLE_INACTIVE'
);
SELECT test.logout();

----------------------------------------------------------------------
-- respond_additional_charge — admin-ও পারে (charge.user_id না হয়েও)
-- (solver1: balance/balance_user শুরু 200 → 50 কর্তনের পর 150)
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'wallet_deduction')::numeric = 50
   FROM (SELECT public.respond_additional_charge('AC157', true) AS r) s),
  'respond_additional_charge: admin কল করলে OK (charge.user_id না হয়েও)'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222')::numeric,
  150::numeric,
  'respond_additional_charge: admin কল করলেও charge.user_id (solver1)-র balance_user ঠিকই কাটে (200 → 150)'
);

SELECT is(
  (SELECT extra_amount FROM public.escrows WHERE id = 'E157')::numeric,
  50::numeric,
  'respond_additional_charge: admin কলেও escrow E157.extra_amount বাড়ে (50)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- increment_escrow_extra_amount — happy path (owner কল করে)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.increment_escrow_extra_amount('E170', 30))->>'result',
  'OK',
  'increment_escrow_extra_amount: owner কল করলে OK'
);

SELECT is(
  (SELECT extra_amount FROM public.escrows WHERE id = 'E170')::numeric,
  50::numeric,
  'increment_escrow_extra_amount: extra_amount 20 → 50 (coalesce + increment)'
);

SELECT throws_ok(
  $$ SELECT public.increment_escrow_extra_amount('E170', NULL) $$,
  'P0001',
  'MISSING_PARAMS',
  'increment_escrow_extra_amount: p_amount NULL হলে MISSING_PARAMS'
);

SELECT throws_ok(
  $$ SELECT public.increment_escrow_extra_amount('NOPE', 10) $$,
  'P0001',
  'ESCROW_NOT_FOUND',
  'increment_escrow_extra_amount: অস্তিত্বহীন escrow_id-তে ESCROW_NOT_FOUND'
);
SELECT test.logout();

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.increment_escrow_extra_amount('E170', 10) $$,
  'P0001',
  'NOT_AUTHORIZED',
  'increment_escrow_extra_amount: escrow-র owner/solver/admin কেউ না হলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT throws_ok(
  $$ SELECT public.increment_escrow_extra_amount('E170', 10) $$,
  'P0001',
  'AUTH_REQUIRED',
  'increment_escrow_extra_amount: লগইন ছাড়া কল করলে AUTH_REQUIRED'
);

----------------------------------------------------------------------
-- record_gateway_payment_log — happy path (default args ব্যবহার করে, client নিজে)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.record_gateway_payment_log(
    p_id => 'GW180', p_gateway_trx_id => 'TRXG180', p_user_id => '11111111-1111-1111-1111-111111111111',
    p_user_name => 'Test Client', p_user_phone => '01700000001', p_amount => 500, p_gateway => 'BKASH'
  ))->>'result',
  'OK',
  'record_gateway_payment_log: caller নিজের জন্য কল করলে (ডিফল্ট args সহ) OK'
);

SELECT results_eq(
  $$ SELECT gateway_trx_id, amount::numeric, gateway, purpose, status, role, note, problem_id
     FROM public.gateway_payments WHERE id = 'GW180' $$,
  $$ VALUES ('TRXG180'::text, 500::numeric, 'BKASH'::text, 'ESCROW_PAYMENT'::text, 'SUCCESS'::text, 'USER'::text, ''::text, ''::text) $$,
  'record_gateway_payment_log: row-এ ডিফল্ট purpose/status/role/note/problem_id ঠিকভাবে বসে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- record_gateway_payment_log — admin অন্য user-এর জন্য পূর্ণ args দিয়ে কল করে
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT is(
  (public.record_gateway_payment_log(
    p_id => 'GW181', p_gateway_trx_id => 'TRXG181', p_user_id => '22222222-2222-2222-2222-222222222222',
    p_user_name => 'Test Solver One', p_user_phone => '01700000002', p_amount => 300, p_gateway => 'NAGAD',
    p_purpose => 'WALLET_DEPOSIT', p_problem_id => '', p_problem_title => '', p_status => 'SUCCESS',
    p_note => 'admin-triggered', p_role => 'SOLVER'
  ))->>'result',
  'OK',
  'record_gateway_payment_log: admin অন্য user-এর জন্য পূর্ণ args দিয়ে কল করলে OK'
);

SELECT results_eq(
  $$ SELECT purpose, role, note FROM public.gateway_payments WHERE id = 'GW181' $$,
  $$ VALUES ('WALLET_DEPOSIT'::text, 'SOLVER'::text, 'admin-triggered'::text) $$,
  'record_gateway_payment_log: admin-কলে explicit purpose/role/note ঠিকভাবে বসে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- record_gateway_payment_log — idempotency (একই id দ্বিতীয়বার কল করলে ওভাররাইট হয় না)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.record_gateway_payment_log(
    p_id => 'GW180', p_gateway_trx_id => 'TRXG180-DUP', p_user_id => '11111111-1111-1111-1111-111111111111',
    p_user_name => 'Test Client', p_user_phone => '01700000001', p_amount => 999, p_gateway => 'CARD'
  ))->>'result',
  'OK',
  'record_gateway_payment_log: একই id দ্বিতীয়বার কল করলেও error না, OK ফেরত আসে'
);

SELECT is(
  (SELECT amount FROM public.gateway_payments WHERE id = 'GW180')::numeric,
  500::numeric,
  'record_gateway_payment_log: on conflict(id) do nothing — আসল row (amount=500) বদলায় না'
);

----------------------------------------------------------------------
-- record_gateway_payment_log — failure cases
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.record_gateway_payment_log(
       p_id => NULL, p_gateway_trx_id => 'TRXG183', p_user_id => '11111111-1111-1111-1111-111111111111',
       p_user_name => 'x', p_user_phone => 'x', p_amount => 10, p_gateway => 'BKASH'
     ) $$,
  'P0001',
  'MISSING_PARAMS',
  'record_gateway_payment_log: p_id NULL হলে MISSING_PARAMS'
);

SELECT throws_ok(
  $$ SELECT public.record_gateway_payment_log(
       p_id => 'GW184', p_gateway_trx_id => 'TRXG184', p_user_id => '11111111-1111-1111-1111-111111111111',
       p_user_name => 'x', p_user_phone => 'x', p_amount => 10, p_gateway => 'BKASH', p_role => 'ADMIN'
     ) $$,
  'P0001',
  'INVALID_ROLE',
  'record_gateway_payment_log: p_role অচেনা হলে INVALID_ROLE'
);
SELECT test.logout();

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.record_gateway_payment_log(
       p_id => 'GW185', p_gateway_trx_id => 'TRXG185', p_user_id => '11111111-1111-1111-1111-111111111111',
       p_user_name => 'x', p_user_phone => 'x', p_amount => 10, p_gateway => 'BKASH'
     ) $$,
  'P0001',
  'NOT_AUTHORIZED',
  'record_gateway_payment_log: caller নিজের জন্য না, admin-ও না হলে NOT_AUTHORIZED'
);
SELECT test.logout();

SELECT throws_ok(
  $$ SELECT public.record_gateway_payment_log(
       p_id => 'GW186', p_gateway_trx_id => 'TRXG186', p_user_id => '11111111-1111-1111-1111-111111111111',
       p_user_name => 'x', p_user_phone => 'x', p_amount => 10, p_gateway => 'BKASH'
     ) $$,
  'P0001',
  'AUTH_REQUIRED',
  'record_gateway_payment_log: লগইন ছাড়া কল করলে AUTH_REQUIRED'
);

----------------------------------------------------------------------
-- mark_additional_charge_settled — happy path (charge.user_id নিজে কল করে)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'status' = 'ACCEPTED'
   FROM (SELECT public.mark_additional_charge_settled('AC190') AS r) s),
  'mark_additional_charge_settled: charge.user_id নিজে কল করলে OK, status ACCEPTED'
);

SELECT results_eq(
  $$ SELECT status, responded_at IS NOT NULL FROM public.additional_charges WHERE id = 'AC190' $$,
  $$ VALUES ('ACCEPTED'::text, true) $$,
  'mark_additional_charge_settled: additional_charges.status=ACCEPTED, responded_at সেট'
);

SELECT is(
  (SELECT count(*) FROM public.transactions WHERE problem_id = 'P190')::int,
  0,
  'mark_additional_charge_settled: bookkeeping-only — কোনো transaction row তৈরি হয় না (money-movement নেই)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- mark_additional_charge_settled — charge.solver_id নিজেও কল করতে পারে
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.mark_additional_charge_settled('AC191'))->>'result',
  'OK',
  'mark_additional_charge_settled: charge.solver_id নিজে কল করলেও OK'
);
SELECT test.logout();

----------------------------------------------------------------------
-- mark_additional_charge_settled — admin-ও পারে
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.mark_additional_charge_settled('AC192'))->>'result',
  'OK',
  'mark_additional_charge_settled: admin কল করলেও OK'
);
SELECT test.logout();

----------------------------------------------------------------------
-- mark_additional_charge_settled — already responded + failure cases
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_RESPONDED' AND r->>'status' = 'ACCEPTED'
   FROM (SELECT public.mark_additional_charge_settled('AC193') AS r) s),
  'mark_additional_charge_settled: status PENDING না হলে ALREADY_RESPONDED'
);

SELECT throws_ok(
  $$ SELECT public.mark_additional_charge_settled('NOPE_AC') $$,
  'P0001',
  'CHARGE_NOT_FOUND',
  'mark_additional_charge_settled: অস্তিত্বহীন charge_id-তে CHARGE_NOT_FOUND'
);
SELECT test.logout();

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.mark_additional_charge_settled('AC194') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'mark_additional_charge_settled: charge.user_id/solver_id/admin কেউ না হলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- system_track_extra_payment_miss — happy path (caller solver নিজে নাও হতে পারে)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.system_track_extra_payment_miss('22222222-2222-2222-2222-222222222222', 5, 2))->>'result',
  'OK',
  'system_track_extra_payment_miss: caller solver নিজে না হলেও (owner-এর ডিভাইস থেকেও) OK'
);

SELECT results_eq(
  $$ SELECT cycle_job_count, cycle_miss_count FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES (5, 2) $$,
  'system_track_extra_payment_miss: solver1-এর cycle_job_count/cycle_miss_count আপডেট হয়'
);

SELECT throws_ok(
  $$ SELECT public.system_track_extra_payment_miss('22222222-2222-2222-2222-222222222222', NULL, 2) $$,
  'P0001',
  'MISSING_PARAMS',
  'system_track_extra_payment_miss: p_new_job_count NULL হলে MISSING_PARAMS'
);

SELECT throws_ok(
  $$ SELECT public.system_track_extra_payment_miss('22222222-2222-2222-2222-222222222222', 5, -1) $$,
  'P0001',
  'INVALID_PARAMS',
  'system_track_extra_payment_miss: negative count দিলে INVALID_PARAMS'
);

SELECT throws_ok(
  $$ SELECT public.system_track_extra_payment_miss('00000000-0000-0000-0000-000000000000', 5, 2) $$,
  'P0001',
  'SOLVER_NOT_FOUND',
  'system_track_extra_payment_miss: অস্তিত্বহীন solver_id-তে SOLVER_NOT_FOUND'
);

SELECT throws_ok(
  $$ SELECT public.system_track_extra_payment_miss('33333333-3333-3333-3333-333333333333', 5, 2) $$,
  'P0001',
  'SOLVER_ROLE_INACTIVE',
  'system_track_extra_payment_miss: solver-এর has_solver_role=false হলে SOLVER_ROLE_INACTIVE'
);

SELECT test.logout();

SELECT throws_ok(
  $$ SELECT public.system_track_extra_payment_miss('22222222-2222-2222-2222-222222222222', 5, 2) $$,
  'P0001',
  'AUTH_REQUIRED',
  'system_track_extra_payment_miss: লগইন ছাড়া কল করলে AUTH_REQUIRED'
);

SELECT * FROM finish();
ROLLBACK;
