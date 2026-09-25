-- 07_wallet_withdrawals_part2.sql — Step 4 (Wallet & withdrawals), PART 2 of 2
--
-- এই ফাইলে কভার করা বাকি ২টা ফাংশন (Step 4-এর ৪টার বাকি অর্ধেক — প্রথম
-- অর্ধেক 07_wallet_withdrawals_part1.sql-এ, PART 1):
--   request_withdrawal(numeric,text,text,text,text,text,text,text)
--       → supabase/migrations/step38_request_withdrawal_client_id.sql
--         (৮-argument, p_client_withdrawal_id সহ — পুরনো ৭-argument overload
--         step38b_drop_old_request_withdrawal_overload.sql-এ explicit DROP হয়ে
--         গেছে, তাই ambiguity নেই)
--   process_withdrawal(text,text,text)
--       → supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql
--         (recovered_money_flow.sql-এর পুরোনো সংজ্ঞা override করে, এটাই চূড়ান্ত)
--
-- দুটো ফাংশনেরই body PART 1 সেশনেই সম্পূর্ণ পড়ে যাচাই করা হয়ে গিয়েছিল
-- (CI_TEST_SUITE_PROGRESS.md-এর "Step 4 PART 2" এন্ট্রি দ্রষ্টব্য) — এখানে সেই
-- অনুযায়ীই test লেখা হয়েছে, migration আবার নতুন করে পড়া লাগেনি।
--
-- নতুন schema: 07_wallet_withdrawals_schema_stub.sql (শুধু `withdrawals`
-- টেবিল, বাকি সব কলাম আগের ধাপের stub-এ আগে থেকেই ছিল)।
--
-- ⚠️⚠️ ২০২৬-০৯-২১ (Step 12.12, F1 ফিক্স, ব্যবহারকারীর অনুমতিতে) — migration
-- `step12_10e_request_withdrawal_solver_kyc_guard` (এই সেশনে migration-ফাইল-সিঙ্ক
-- হওয়া) request_withdrawal-এ নতুন গার্ড যোগ করেছে: p_role='SOLVER' এবং
-- users.is_kyc_verified=false হলে KYC_REQUIRED (ROLE_INACTIVE-এর পরে, amount-চেকের
-- আগে)। এর ফলে নিচের solver1 (2222…) happy-path SOLVER-withdrawal টেস্ট আগে ভেঙে
-- যেত (is_kyc_verified ডিফল্ট false) — তাই solver1-এর SETUP-এ is_kyc_verified=true
-- যোগ করা হলো, এবং একটা নতুন fixture user (d0000001…, has_solver_role=true কিন্তু
-- is_kyc_verified=false) দিয়ে KYC_REQUIRED পথের জন্য আলাদা নতুন assertion যোগ করা
-- হলো (solver2 ব্যবহার করা যেত না, ওটা তখনো has_solver_role=false — ROLE_INACTIVE
-- আগে ধরত)। plan() 36→38। এই সেশনেও sandbox network বন্ধ থাকায় migration body
-- সরাসরি পড়ে (উপরে quote করা আছে) static-ভাবেই যাচাই — real Postgres+pgTAP-এ
-- চালানো হয়নি। বিস্তারিত: CI_TEST_SUITE_PROGRESS.md-এর এই সেশনের "Step 12.12" সেকশন।
--
-- client(1111)-এর balance/balance_user আর solver1(2222)-এর balance/
-- balance_solver-এর উপর cumulative effect ক্রমানুসারে ট্র্যাক করে টেস্ট লেখা
-- হয়েছে (Step 3/PART 1-এর প্যাটার্ন অনুসরণ করে), প্রতিটা ধাপে কমেন্টে হিসাব
-- লেখা আছে। solver2(3333) দুইবার ব্যবহার হয়েছে ইচ্ছাকৃতভাবে: প্রথমে
-- has_solver_role=false অবস্থায় (ROLE_INACTIVE টেস্টের জন্য, দুই ফাংশনেই),
-- তারপর সেশনের মাঝপথে সক্রিয় করে (happy-path REJECT-refund টেস্টের জন্য) —
-- এই দুটো ব্যবহারের মধ্যে কোনো balance/role পরিবর্তন ইচ্ছাকৃতভাবে না হওয়া
-- পর্যন্ত ক্রস-কনট্যামিনেশন নেই।
--
-- সব সংখ্যাসূচক তুলনা ::numeric-এ করা হয়েছে (Step 2-এর numeric-vs-text
-- strict-match ভুল এড়াতে)। plan() লেখার পর grep -cE দিয়ে assertion সংখ্যা
-- গুনে মিলিয়ে নেওয়া হয়েছে (Step 2/PART 1-এ ধরা পড়া plan()-mismatch ভুল
-- এড়াতে)।
--
-- test user ids (00_helpers.sql / test.seed_users):
--   CLIENT 1111…  SOLVER-1 2222…  SOLVER-2 3333…  ADMIN 9999…

BEGIN;
SELECT plan(38);

SELECT test.seed_users();

----------------------------------------------------------------------
-- SETUP (superuser)
----------------------------------------------------------------------
-- solver1: request_withdrawal(role=SOLVER) হ্যাপি-পাথ টেস্টের জন্য সক্রিয়
-- SOLVER role + যথেষ্ট balance_solver + KYC verified (12.10e-পরবর্তী: SOLVER
-- withdrawal-এ is_kyc_verified বাধ্যতামূলক, নাহলে KYC_REQUIRED)
UPDATE public.users
SET has_solver_role = true, balance = 500, balance_solver = 500, is_kyc_verified = true
WHERE id = '22222222-2222-2222-2222-222222222222';

-- solver2 ইচ্ছাকৃতভাবে has_solver_role=false-ই থাকছে এখনো (seed default) —
-- request_withdrawal + process_withdrawal দুটোরই ROLE_INACTIVE টেস্টের জন্য।
-- পরে (নিচে, process_withdrawal সেকশনের মাঝপথে) এটা সক্রিয় করা হবে।

-- ⚠️ নতুন (12.10e KYC guard, Step 12.12 F1): আলাদা fixture user — has_solver_role=true
-- কিন্তু is_kyc_verified=false (ডিফল্ট) — যাতে ROLE_INACTIVE-এর সাথে conflate না করে
-- খাঁটি KYC_REQUIRED পথ যাচাই করা যায় (solver2 ব্যবহার করা যেত না, কারণ ওটা এই মুহূর্তে
-- has_solver_role=false, তাই ROLE_INACTIVE আগে ধরা পড়ত)।
INSERT INTO public.users (id, role, name, phone, has_solver_role, balance, balance_solver) VALUES
  ('d0000001-0001-0001-0001-000000000001', 'SOLVER', 'Solver KYC Unverified', '01780000001', true, 200, 200);

-- client(1111) শুরুর balance/balance_user (seed_users থেকে): 1000 / 1000

----------------------------------------------------------------------
-- request_withdrawal — INVALID_ROLE
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT throws_ok(
  $$ SELECT public.request_withdrawal(50, 'BKASH', '01700000001', NULL, NULL, NULL, 'ADMIN', NULL) $$,
  'P0001',
  'INVALID_ROLE',
  'request_withdrawal: p_role শুধু USER/SOLVER নিতে পারে, নাহলে INVALID_ROLE'
);

----------------------------------------------------------------------
-- request_withdrawal — happy path, role=USER (client)
-- balance/balance_user 1000 → 800
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'withdrawal_id') LIKE 'WID-%'
   FROM (SELECT public.request_withdrawal(
     200, 'BKASH', '01700000001', NULL, NULL, NULL, 'USER', NULL) AS r) s),
  'request_withdrawal: role=USER হ্যাপি পাথ → OK, server-generated WID-... id ফেরত আসে'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (800::numeric, 800::numeric) $$,
  'request_withdrawal: role=USER হলে legacy balance এবং balance_user দুটোই -200 (dual-write deduct)'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.withdrawals
    WHERE solver_id = '11111111-1111-1111-1111-111111111111'
      AND amount::numeric = 200 AND status = 'PENDING' AND role = 'USER' AND method = 'BKASH'
  ),
  'request_withdrawal: withdrawals row status=PENDING role=USER দিয়ে insert হয়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.transactions
    WHERE user_id = '11111111-1111-1111-1111-111111111111'
      AND type = 'WITHDRAWAL_DEDUCTION' AND role = 'USER'
      AND gross_amount::numeric = 200 AND net_amount::numeric = -200
  ),
  'request_withdrawal: transactions row type=WITHDRAWAL_DEDUCTION, gross=200, net=-200, role=USER'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE user_id = '11111111-1111-1111-1111-111111111111' AND title = 'উইথড্র রিকোয়েস্ট জমা হয়েছে'
  ),
  'request_withdrawal: উইথড্র রিকোয়েস্ট জমা হওয়ার notification পাঠানো হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_withdrawal — happy path, role=SOLVER (solver1)
-- balance/balance_solver 500 → 350
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT ok(
  (SELECT r->>'result' = 'OK'
   FROM (SELECT public.request_withdrawal(
     150, 'NAGAD', '01700000002', 'DBBL', 'Gulshan', 'Test Solver One', 'SOLVER', NULL) AS r) s),
  'request_withdrawal: role=SOLVER, has_solver_role=true হলে OK'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES (350::numeric, 350::numeric) $$,
  'request_withdrawal: role=SOLVER হলে legacy balance এবং balance_solver দুটোই -150'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.withdrawals
    WHERE solver_id = '22222222-2222-2222-2222-222222222222'
      AND amount::numeric = 150 AND status = 'PENDING' AND role = 'SOLVER'
      AND bank_name = 'DBBL' AND branch_name = 'Gulshan'
  ),
  'request_withdrawal: role=SOLVER-এর withdrawals row bank/branch/account_holder সহ insert হয়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.transactions
    WHERE user_id = '22222222-2222-2222-2222-222222222222'
      AND type = 'WITHDRAWAL_DEDUCTION' AND role = 'SOLVER' AND net_amount::numeric = -150
  ),
  'request_withdrawal: role=SOLVER-এর জন্য transactions row role=SOLVER দিয়ে তৈরি হয়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE user_id = '22222222-2222-2222-2222-222222222222' AND title = 'উইথড্র রিকোয়েস্ট জমা হয়েছে'
  ),
  'request_withdrawal: role=SOLVER-এর জন্যও notification পাঠানো হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_withdrawal — KYC_REQUIRED (12.10e, নতুন, Step 12.12 F1)
-- has_solver_role=true কিন্তু is_kyc_verified=false → ROLE_INACTIVE-এর পরে,
-- BELOW_MIN/INSUFFICIENT_BALANCE-এর আগে KYC_REQUIRED (role=USER অপ্রভাবিত, নিচে confirm)
----------------------------------------------------------------------
SELECT test.login_as('d0000001-0001-0001-0001-000000000001');

SELECT throws_ok(
  $$ SELECT public.request_withdrawal(150, 'BKASH', '01700000099', NULL, NULL, NULL, 'SOLVER', NULL) $$,
  'P0001',
  'KYC_REQUIRED',
  'request_withdrawal: role=SOLVER, has_solver_role=true কিন্তু is_kyc_verified=false হলে KYC_REQUIRED (12.10e)'
);
SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = 'd0000001-0001-0001-0001-000000000001' $$,
  $$ VALUES (200::numeric, 200::numeric) $$,
  'request_withdrawal: KYC_REQUIRED-এ balance/balance_solver অপরিবর্তিত থাকে (কোনো deduction হয়নি)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_withdrawal — BELOW_MIN_WITHDRAWAL (client, platform_settings-এ
-- কোনো row নেই → coalesce ডিফল্ট min=100)
-- balance/balance_user অপরিবর্তিত থাকে (800)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT throws_ok(
  $$ SELECT public.request_withdrawal(50, 'BKASH', '01700000001', NULL, NULL, NULL, 'USER', NULL) $$,
  'P0001',
  'BELOW_MIN_WITHDRAWAL: 100',
  'request_withdrawal: p_amount < coalesce(min_withdrawal, 100) হলে BELOW_MIN_WITHDRAWAL: <min>'
);

----------------------------------------------------------------------
-- request_withdrawal — INSUFFICIENT_BALANCE (client-এর balance_user এখন 800,
-- amount 900 চাওয়া হচ্ছে)
-- balance/balance_user অপরিবর্তিত থাকে (800)
----------------------------------------------------------------------
-- ⚠️ v_role_balance PL/pgSQL variable numeric(12,2) টাইপে declare করা (step38
-- migration, লাইন 34) — এই typmod RAISE %-এর আউটপুটেও বজায় থাকে, তাই মান
-- '800' না, '800.00' (2-দশমিক) হিসেবে দেখাবে। v_min_withdrawal (উপরের
-- BELOW_MIN_WITHDRAWAL টেস্টে) প্লেইন `numeric` (কোনো typmod ছাড়া) হিসেবে
-- declare করা, তাই সেটা '100.00' না, '100' দেখায় — দুটো ভিন্ন আচরণ ইচ্ছাকৃতভাবে
-- আলাদাভাবে যাচাই করা হলো (Step 2-এর numeric-vs-text strict-match ভুল থেকে
-- শেখা শিক্ষা অনুযায়ী)।
SELECT throws_ok(
  $$ SELECT public.request_withdrawal(900, 'BKASH', '01700000001', NULL, NULL, NULL, 'USER', NULL) $$,
  'P0001',
  'INSUFFICIENT_BALANCE: 800.00',
  'request_withdrawal: p_amount > role balance হলে INSUFFICIENT_BALANCE: <balance, numeric(12,2) scale বজায় থেকে>'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (800::numeric, 800::numeric) $$,
  'request_withdrawal: BELOW_MIN/INSUFFICIENT_BALANCE কোনোটাতেই balance/balance_user পরিবর্তন হয় না (এখনো 800)'
);

----------------------------------------------------------------------
-- request_withdrawal — p_client_withdrawal_id: client-generated id ব্যবহার
-- হয় (server-generated WID- না)
-- balance/balance_user 800 → 700
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'withdrawal_id' = 'MYWID-001'
   FROM (SELECT public.request_withdrawal(
     100, 'ROCKET', '01700000001', NULL, NULL, NULL, 'USER', 'MYWID-001') AS r) s),
  'request_withdrawal: p_client_withdrawal_id দেওয়া থাকলে (ফাঁকা না) সেই id-ই withdrawal_id হিসেবে ব্যবহার হয়'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (700::numeric, 700::numeric) $$,
  'request_withdrawal: client-generated id দিয়ে কল করলেও deduction স্বাভাবিকভাবেই হয় (700)'
);

SELECT ok(
  EXISTS (SELECT 1 FROM public.withdrawals WHERE id = 'MYWID-001' AND amount::numeric = 100),
  'request_withdrawal: withdrawals row-এর primary key client-generated id (MYWID-001) দিয়েই তৈরি হয়'
);

----------------------------------------------------------------------
-- request_withdrawal — WITHDRAWAL_ID_COLLISION (একই client id দ্বিতীয়বার)
-- balance/balance_user অপরিবর্তিত থাকে (collision-check balance-touch-এর
-- আগে ঘটে) — এখনো 700
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.request_withdrawal(50, 'BKASH', '01700000001', NULL, NULL, NULL, 'USER', 'MYWID-001') $$,
  'P0001',
  'WITHDRAWAL_ID_COLLISION',
  'request_withdrawal: আগে থেকে ব্যবহৃত p_client_withdrawal_id পুনরায় দিলে WITHDRAWAL_ID_COLLISION'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_withdrawal — ROLE_INACTIVE (solver2, has_solver_role=false ডিফল্ট)
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.request_withdrawal(150, 'BKASH', '01700000003', NULL, NULL, NULL, 'SOLVER', NULL) $$,
  'P0001',
  'ROLE_INACTIVE',
  'request_withdrawal: has_solver_role=false অবস্থায় role=SOLVER দিয়ে কল করলে ROLE_INACTIVE'
);
SELECT test.logout();


----------------------------------------------------------------------
-- process_withdrawal — SETUP (superuser): তিনটা withdrawal row সরাসরি
-- insert করা হলো (নির্দিষ্ট status/role/amount নিয়ন্ত্রণের জন্য, request_withdrawal
-- চেইন না করে — Step 3-এর escrow-setup প্যাটার্ন অনুসরণ করে)
----------------------------------------------------------------------
INSERT INTO public.withdrawals (id, solver_id, solver_name, amount, method, account_number, status, role, created_at)
VALUES
  ('WD-COMPLETE-1', '11111111-1111-1111-1111-111111111111', 'Test Client', 100, 'BKASH', '01700000001', 'PENDING', 'USER', now()),
  -- solver2 এখনো এই মুহূর্তে has_solver_role=false — ROLE_INACTIVE (REJECT-refund path) টেস্টের জন্য
  ('WD-REJECT-INACTIVE', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 50, 'NAGAD', '01700000003', 'PENDING', 'SOLVER', now());

----------------------------------------------------------------------
-- process_withdrawal — NOT_AUTHORIZED (non-admin caller)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.process_withdrawal('WD-COMPLETE-1', 'COMPLETE', NULL) $$,
  'P0001',
  'NOT_AUTHORIZED',
  'process_withdrawal: is_admin(auth.uid())=false হলে NOT_AUTHORIZED (action/id ভ্যালিড হলেও)'
);
SELECT test.logout();

----------------------------------------------------------------------
-- process_withdrawal — admin সেকশন শুরু
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

----------------------------------------------------------------------
-- process_withdrawal — INVALID_ACTION
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.process_withdrawal('WD-COMPLETE-1', 'CANCEL', NULL) $$,
  'P0001',
  'INVALID_ACTION',
  'process_withdrawal: p_action শুধু COMPLETE/REJECT নিতে পারে, নাহলে INVALID_ACTION (row-lookup-এরও আগে চেক হয়)'
);

----------------------------------------------------------------------
-- process_withdrawal — WITHDRAWAL_NOT_FOUND
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.process_withdrawal('WD-NOPE', 'COMPLETE', NULL) $$,
  'P0001',
  'WITHDRAWAL_NOT_FOUND',
  'process_withdrawal: অস্তিত্বহীন p_withdrawal_id দিলে WITHDRAWAL_NOT_FOUND'
);

----------------------------------------------------------------------
-- process_withdrawal — COMPLETE happy path
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'status' = 'COMPLETED'
   FROM (SELECT public.process_withdrawal('WD-COMPLETE-1', 'COMPLETE', 'TRXABC') AS r) s),
  'process_withdrawal: COMPLETE happy path → OK, status=COMPLETED'
);

SELECT results_eq(
  $$ SELECT status::text, trx_id::text FROM public.withdrawals WHERE id = 'WD-COMPLETE-1' $$,
  $$ VALUES ('COMPLETED'::text, 'TRXABC'::text) $$,
  'process_withdrawal: withdrawals row status→COMPLETED, trx_id সেট হয়'
);

----------------------------------------------------------------------
-- process_withdrawal — ALREADY_TERMINAL (একই trx_id দিয়ে আবার COMPLETE)
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_TERMINAL' AND r->>'status' = 'COMPLETED'
   FROM (SELECT public.process_withdrawal('WD-COMPLETE-1', 'COMPLETE', 'TRXABC') AS r) s),
  'process_withdrawal: COMPLETED অবস্থায় একই trx_id দিয়ে আবার COMPLETE কল করলে ALREADY_TERMINAL (trx_id distinct না)'
);

----------------------------------------------------------------------
-- process_withdrawal — TRX_ID_UPDATED (COMPLETED অবস্থায় ভিন্ন নতুন trx_id)
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'TRX_ID_UPDATED'
   FROM (SELECT public.process_withdrawal('WD-COMPLETE-1', 'COMPLETE', 'TRXNEW') AS r) s),
  'process_withdrawal: COMPLETED অবস্থায় ভিন্ন (distinct) নতুন trx_id দিলে TRX_ID_UPDATED, শুধু trx_id আপডেট হয়'
);

SELECT results_eq(
  $$ SELECT trx_id::text FROM public.withdrawals WHERE id = 'WD-COMPLETE-1' $$,
  $$ VALUES ('TRXNEW'::text) $$,
  'process_withdrawal: TRX_ID_UPDATED পথে withdrawals.trx_id আসলেই নতুন মানে আপডেট হয়'
);

----------------------------------------------------------------------
-- process_withdrawal — ROLE_INACTIVE (REJECT-refund path, solver2 এখনো
-- has_solver_role=false)
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.process_withdrawal('WD-REJECT-INACTIVE', 'REJECT', NULL) $$,
  'P0001',
  'ROLE_INACTIVE',
  'process_withdrawal: REJECT-refund যে role-এ ফেরত যাচ্ছে সেটা নিষ্ক্রিয় (has_solver_role=false) হলে ROLE_INACTIVE'
);

----------------------------------------------------------------------
-- process_withdrawal সেকশনের মাঝপথে solver2-কে সক্রিয় করা হলো (superuser
-- context-এ, admin login-এর ভেতরেই direct UPDATE) — REJECT happy-path
-- টেস্টের জন্য
----------------------------------------------------------------------
UPDATE public.users
SET has_solver_role = true, balance = 300, balance_solver = 300
WHERE id = '33333333-3333-3333-3333-333333333333';

INSERT INTO public.withdrawals (id, solver_id, solver_name, amount, method, account_number, status, role, created_at)
VALUES ('WD-REJECT-1', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 80, 'NAGAD', '01700000003', 'PENDING', 'SOLVER', now());

----------------------------------------------------------------------
-- process_withdrawal — REJECT happy path
-- solver2 balance/balance_solver 300 → 380
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'status' = 'REJECTED' AND r->'note' IS NULL
   FROM (SELECT public.process_withdrawal('WD-REJECT-1', 'REJECT', 'admin note') AS r) s),
  'process_withdrawal: REJECT happy path → OK, status=REJECTED, প্রথমবার কোনো note নেই'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '33333333-3333-3333-3333-333333333333' $$,
  $$ VALUES (380::numeric, 380::numeric) $$,
  'process_withdrawal: REJECT-এ v_wd.role(SOLVER) অনুযায়ী legacy balance ও balance_solver দুটোই +80 রিফান্ড হয়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.transactions
    WHERE id = 'TRX_WD_REFUND_WD-REJECT-1' AND user_id = '33333333-3333-3333-3333-333333333333'
      AND type = 'WITHDRAWAL_REFUND' AND role = 'SOLVER' AND net_amount::numeric = 80
  ),
  'process_withdrawal: REJECT-এ TRX_WD_REFUND_<id> transaction row role=SOLVER net_amount=80 দিয়ে insert হয়'
);

SELECT results_eq(
  $$ SELECT status::text, rejection_reason::text FROM public.withdrawals WHERE id = 'WD-REJECT-1' $$,
  $$ VALUES ('REJECTED'::text, 'admin note'::text) $$,
  'process_withdrawal: withdrawals row status→REJECTED, rejection_reason=p_trx_id প্যারামিটারের মান'
);

----------------------------------------------------------------------
-- process_withdrawal — সত্যিকারের দ্বিতীয়বার REJECT কল (একই এখন-REJECTED id)
-- ⚠️ কোড সত্যিই পড়ে ধরা পড়েছে (অনুমান না): ফাংশনের একদম শুরুতেই
-- `if v_wd.status in ('COMPLETED','REJECTED') then ... return ALREADY_TERMINAL`
-- চেক হয় — এটা p_action='REJECT'-এও প্রযোজ্য (শুধু COMPLETE-এর TRX_ID_UPDATED
-- ব্রাঞ্চটাই এর ভেতরে আলাদা করে হ্যান্ডল হয়, REJECT-এর জন্য কোনো বিশেষ কেস নেই)।
-- মানে literal দ্বিতীয়বার REJECT কল করলে নিচের 'already_refunded' ব্রাঞ্চে
-- (line ~464-467, নিচের টেস্টে আলাদাভাবে কভার করা হয়েছে) পৌঁছানোরই সুযোগ নেই —
-- তার আগেই এই top-level terminal-check এ আটকে ALREADY_TERMINAL রিটার্ন করবে।
-- balance/balance_solver অপরিবর্তিত থাকে (380)।
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_TERMINAL' AND r->>'status' = 'REJECTED'
   FROM (SELECT public.process_withdrawal('WD-REJECT-1', 'REJECT', 'second attempt') AS r) s),
  'process_withdrawal: এখন-REJECTED withdrawal-এ আবার REJECT কল করলে top-level terminal-check-এ ALREADY_TERMINAL (status=REJECTED) — already_refunded ব্রাঞ্চ পর্যন্ত পৌঁছায় না'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '33333333-3333-3333-3333-333333333333' $$,
  $$ VALUES (380::numeric, 380::numeric) $$,
  'process_withdrawal: দ্বিতীয়বার REJECT কলেও কোনো ডবল-রিফান্ড হয় না (balance/balance_solver এখনো 380)'
);

----------------------------------------------------------------------
-- process_withdrawal — 'already_refunded' ব্রাঞ্চ (line ~464-467) সত্যিকার
-- অর্থে তখনই ট্রিগার হয় যখন withdrawals.status এখনো PENDING (COMPLETED/
-- REJECTED না — তাই উপরের top-level terminal-check এড়িয়ে ভেতরে ঢোকে) কিন্তু
-- সংশ্লিষ্ট TRX_WD_REFUND_<id> transaction row **আগে থেকেই** আছে — এটা একটা
-- partial-completion/crash-recovery safety-net (যেমন: আগের একটা কল টাকা
-- ফেরত+transaction insert করে ফেলেছিল কিন্তু status='REJECTED' আপডেট করার
-- আগেই ব্যর্থ/বিচ্ছিন্ন হয়েছিল), literal দ্বিতীয়বার-ক্লিকের কেস না। এখানে সেই
-- অবস্থা ইচ্ছাকৃতভাবে বানানো হলো (একটা নতুন PENDING withdrawal + hand-crafted
-- transaction row)।
-- solver2-এর balance অপরিবর্তিত থাকবে (এই ব্রাঞ্চ কোনো dual-write করে না)।
----------------------------------------------------------------------
INSERT INTO public.withdrawals (id, solver_id, solver_name, amount, method, account_number, status, role, created_at)
VALUES ('WD-REJECT-ORPHAN-TRX', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 40, 'NAGAD', '01700000003', 'PENDING', 'SOLVER', now());

INSERT INTO public.transactions (id, problem_id, problem_title, user_id, gross_amount, net_amount, type, role, "timestamp")
VALUES ('TRX_WD_REFUND_WD-REJECT-ORPHAN-TRX', '', 'পূর্বের আংশিক-সম্পন্ন রিফান্ড (টেস্ট ফিক্সচার)', '33333333-3333-3333-3333-333333333333', 40, 40, 'WITHDRAWAL_REFUND', 'SOLVER', now());

SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'status' = 'REJECTED' AND r->>'note' = 'already_refunded'
   FROM (SELECT public.process_withdrawal('WD-REJECT-ORPHAN-TRX', 'REJECT', NULL) AS r) s),
  'process_withdrawal: status এখনো PENDING কিন্তু TRX_WD_REFUND_<id> আগে থেকেই থাকলে note=already_refunded, শুধু status→REJECTED হয়'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '33333333-3333-3333-3333-333333333333' $$,
  $$ VALUES (380::numeric, 380::numeric) $$,
  'process_withdrawal: already_refunded ব্রাঞ্চ কোনো dual-write করে না, তাই balance/balance_solver অপরিবর্তিত (এখনো 380, ডবল-রিফান্ড হয় না)'
);

SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
