-- 07_wallet_withdrawals_part1.sql — Step 4 (Wallet & withdrawals), PART 1 of 2
--
-- এই ফাইলে কভার করা ২টা ফাংশন (Step 4-এর ৪টার অর্ধেক — বাকি অর্ধেক PART 2-এ,
-- দেখুন CI_TEST_SUITE_PROGRESS.md-এর "Step 4 PART 2" নোট):
--   deposit_money_via_gateway(uuid,numeric,text,text,text,text)
--       → supabase/migrations/recovered_money_flow.sql (এই একটাই সংজ্ঞা, কোনো
--         পরের migration override করে না — case-insensitive grep দিয়ে যাচাই করা)
--   request_wallet_deposit(numeric,text,text,text,text,text)
--       → supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql
--         (recovered_money_flow.sql-এর পুরোনো সংজ্ঞা override করে, এটাই চূড়ান্ত body)
--
-- PART 2-এর কাজ (এই ফাইলে নেই): request_withdrawal, process_withdrawal —
-- দুটোরই body ইতিমধ্যে zip-এ পড়া হয়ে গেছে (progress note দ্রষ্টব্য), কিন্তু
-- `withdrawals` টেবিল এখনো stub-এ নেই বলে এখানে test লেখা হলো না (rule #3:
-- একবারে একটা ধাপ, আর নতুন টেবিল-নির্ভর কাজ আলাদা রাখাই পরিষ্কার)।
--
-- ⚠️ স্কিমা নোট: এই দুটো ফাংশনের কোনোটারই নতুন টেবিল/কলাম লাগেনি — users
-- (balance/balance_user/balance_solver/has_user_role/has_solver_role),
-- gateway_payments (role সহ — 05 stub-এ যোগ হয়েছিল), transactions (base_amount/
-- role/escrow_id সহ — 01+05 stub-এ যোগ হয়েছিল), platform_settings, notifications —
-- সবই আগের ধাপের stub-এ আগে থেকেই আছে। তাই এই PART 1-এর জন্য কোনো নতুন
-- `07_..._schema_stub.sql` লাগেনি (PART 2-এ `withdrawals` টেবিলের জন্য লাগবে)।
--
-- deposit_money_via_gateway লক্ষণীয়ভাবে request_wallet_deposit থেকে আলাদা:
-- এটা p_role নেয় না, কোনো gateway whitelist validate করে না, আর শুধু legacy
-- `balance` কলামে যোগ করে (balance_user/balance_solver dual-write করে না) —
-- সম্ভবত admin-driven/legacy path, role-system আসার আগে লেখা। এই পার্থক্যটা
-- ইচ্ছাকৃতভাবে টেস্টে verify করা হয়েছে (assertion #3)।
--
-- সব setup superuser হিসেবে শুরুতে বসানো হয়েছে। client(1111)-এর balance/
-- balance_user-এর উপর cumulative effect ক্রমানুসারে ট্র্যাক করে টেস্ট লেখা
-- হয়েছে (Step 3-এর প্যাটার্ন অনুসরণ করে) — কমেন্টে প্রতিটা ধাপে হিসাব লেখা আছে।
--
-- test user ids (00_helpers.sql / test.seed_users):
--   CLIENT 1111…  SOLVER-1 2222…  SOLVER-2 3333…  ADMIN 9999…

BEGIN;
SELECT plan(29);

SELECT test.seed_users();

----------------------------------------------------------------------
-- SETUP (superuser)
----------------------------------------------------------------------
-- request_wallet_deposit(role='SOLVER') টেস্টের জন্য solver1-এর SOLVER role active
UPDATE public.users SET has_solver_role = true WHERE id = '22222222-2222-2222-2222-222222222222';
-- solver2 ইচ্ছাকৃতভাবে has_solver_role=false-ই থাকছে (default) — ROLE_INACTIVE টেস্টের জন্য

-- client(1111) শুরুর balance/balance_user (seed_users থেকে): 1000 / 1000

----------------------------------------------------------------------
-- deposit_money_via_gateway — happy path (self, client নিজের জন্য কল করছে)
-- balance 1000 → 1200 (legacy balance-ই বাড়ে, balance_user অপরিবর্তিত থাকে)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'payment_id') LIKE 'GWPAY_%'
   FROM (SELECT public.deposit_money_via_gateway(
     '11111111-1111-1111-1111-111111111111', 200, 'BKASH', 'GWTRX-D1') AS r) s),
  'deposit_money_via_gateway: নিজের জন্য কল করলে OK, payment_id ফেরত আসে'
);

SELECT results_eq(
  $$ SELECT status::text, purpose::text FROM public.gateway_payments WHERE gateway_trx_id = 'GWTRX-D1' $$,
  $$ VALUES ('SUCCESS'::text, 'WALLET_DEPOSIT'::text) $$,
  'deposit_money_via_gateway: gateway_payments row status=SUCCESS, purpose=WALLET_DEPOSIT দিয়ে insert হয়'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (1200::numeric, 1000::numeric) $$,
  'deposit_money_via_gateway: legacy balance +200 (1200) কিন্তু balance_user অপরিবর্তিত (1000) — role-scoped dual-write নেই এই ফাংশনে'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.transactions
    WHERE user_id = '11111111-1111-1111-1111-111111111111'
      AND type = 'WALLET_DEPOSIT' AND net_amount::numeric = 200 AND role IS NULL
  ),
  'deposit_money_via_gateway: transactions row type=WALLET_DEPOSIT net_amount=200 (role সেট হয় না, এই ফাংশনে p_role নেই)'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE user_id = '11111111-1111-1111-1111-111111111111' AND title = 'রিচার্জ সফল'
  ),
  'deposit_money_via_gateway: সফল রিচার্জের notification পাঠানো হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- deposit_money_via_gateway — NOT_AUTHORIZED (non-admin, অন্যের জন্য কল)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.deposit_money_via_gateway('11111111-1111-1111-1111-111111111111', 50, 'BKASH', 'GWTRX-D-BAD') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'deposit_money_via_gateway: caller নিজে p_user_id না হয়ে, admin-ও না হয়ে অন্যের জন্য কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- deposit_money_via_gateway — admin bypass (admin অন্যের জন্য কল করতে পারে)
-- balance 1200 → 1250
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT ok(
  (SELECT r->>'result' = 'OK'
   FROM (SELECT public.deposit_money_via_gateway(
     '11111111-1111-1111-1111-111111111111', 50, 'NAGAD', 'GWTRX-D2') AS r) s),
  'deposit_money_via_gateway: admin অন্য user-এর (p_user_id) জন্য কল করলেও OK (NOT_AUTHORIZED bypass)'
);

SELECT results_eq(
  $$ SELECT balance::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (1250::numeric) $$,
  'deposit_money_via_gateway: admin-এর কলের পরেও balance ঠিকভাবে যোগ হয় (1200+50=1250)'
);

----------------------------------------------------------------------
-- deposit_money_via_gateway — INVALID_AMOUNT
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.deposit_money_via_gateway('11111111-1111-1111-1111-111111111111', 0, 'BKASH', 'GWTRX-D3') $$,
  'P0001',
  'INVALID_AMOUNT',
  'deposit_money_via_gateway: p_amount<=0 হলে INVALID_AMOUNT'
);

----------------------------------------------------------------------
-- deposit_money_via_gateway — ALREADY_PROCESSED (একই gateway_trx_id পুনরায়)
-- balance অপরিবর্তিত থাকে (1250)
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'ALREADY_PROCESSED'
   FROM (SELECT public.deposit_money_via_gateway(
     '11111111-1111-1111-1111-111111111111', 999, 'BKASH', 'GWTRX-D1') AS r) s),
  'deposit_money_via_gateway: আগে থেকে ব্যবহৃত gateway_trx_id পুনরায় দিলে ALREADY_PROCESSED, কোনো নতুন insert হয় না'
);

SELECT results_eq(
  $$ SELECT balance::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (1250::numeric) $$,
  'deposit_money_via_gateway: ALREADY_PROCESSED-এ balance-এ কোনো ডবল-ক্রেডিট হয় না (এখনো 1250)'
);

----------------------------------------------------------------------
-- deposit_money_via_gateway — খালি/NULL gateway_trx_id ডেডুপ-চেক বাইপাস করে
-- (function-এর নিজস্ব শর্ত: p_gateway_trx_id IS NOT NULL AND <> '' তবেই dedupe চেক হয়)
-- balance 1250 → 1260 → 1270 (দুটো কলই সফল, ALREADY_PROCESSED আসে না)
----------------------------------------------------------------------
SELECT ok(
  (SELECT r->>'result' = 'OK' FROM (SELECT public.deposit_money_via_gateway(
    '11111111-1111-1111-1111-111111111111', 10, 'ROCKET', '') AS r) s),
  'deposit_money_via_gateway: খালি gateway_trx_id দিয়ে প্রথম কল OK'
);
SELECT ok(
  (SELECT r->>'result' = 'OK' FROM (SELECT public.deposit_money_via_gateway(
    '11111111-1111-1111-1111-111111111111', 10, 'ROCKET', '') AS r) s),
  'deposit_money_via_gateway: খালি gateway_trx_id দিয়ে দ্বিতীয় কলও OK (ALREADY_PROCESSED না — dedupe চেক null/খালি-তে স্কিপ হয়)'
);

SELECT results_eq(
  $$ SELECT balance::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (1270::numeric) $$,
  'deposit_money_via_gateway: খালি-trx_id দুই কল মিলে +20 (1250→1270)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_wallet_deposit — happy path, role=USER, auto-approve (platform_settings-এ
-- কোনো row নেই → coalesce ডিফল্ট true)
-- balance 1270 → 1420, balance_user 1000 → 1150
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'payment_id') LIKE 'GWPAY_%'
   FROM (SELECT public.request_wallet_deposit(150, 'NAGAD', 'WTRX-A1', '', '', 'USER') AS r) s),
  'request_wallet_deposit: role=USER, auto-approve ডিফল্ট true (platform_settings-এ row নেই) → OK'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (1420::numeric, 1150::numeric) $$,
  'request_wallet_deposit: role=USER হলে legacy balance এবং balance_user দুটোই +150 (dual-write)'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.transactions
    WHERE user_id = '11111111-1111-1111-1111-111111111111'
      AND type = 'WALLET_DEPOSIT' AND role = 'USER' AND net_amount::numeric = 150
  ),
  'request_wallet_deposit: auto-approve পথে transactions row role=USER দিয়ে তৈরি হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_wallet_deposit — happy path, role=SOLVER (solver1, has_solver_role=true)
-- solver1 শুরুর balance/balance_solver: 0 / 0 → 150 / 150
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT ok(
  (SELECT r->>'result' = 'OK' FROM (SELECT public.request_wallet_deposit(
    150, 'CARD', 'WTRX-B1', '', '', 'SOLVER') AS r) s),
  'request_wallet_deposit: role=SOLVER, has_solver_role=true হলে OK'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES (150::numeric, 150::numeric) $$,
  'request_wallet_deposit: role=SOLVER হলে legacy balance এবং balance_solver দুটোই +150'
);

----------------------------------------------------------------------
-- request_wallet_deposit — ROLE_INACTIVE (solver2, has_solver_role=false ডিফল্ট)
----------------------------------------------------------------------
SELECT test.logout();
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.request_wallet_deposit(50, 'BKASH', 'WTRX-I1', '', '', 'SOLVER') $$,
  'P0001',
  'ROLE_INACTIVE',
  'request_wallet_deposit: has_solver_role=false অবস্থায় role=SOLVER দিয়ে কল করলে ROLE_INACTIVE'
);
SELECT test.logout();

----------------------------------------------------------------------
-- request_wallet_deposit — auto_approve=false → PENDING_APPROVAL (কোনো balance/
-- transaction পরিবর্তন হয় না)
----------------------------------------------------------------------
INSERT INTO public.platform_settings (key, value) VALUES ('gateway_auto_approve_deposits', 'false')
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT ok(
  (SELECT r->>'result' = 'PENDING_APPROVAL'
   FROM (SELECT public.request_wallet_deposit(90, 'BKASH', 'WTRX-C1', '', '', 'USER') AS r) s),
  'request_wallet_deposit: platform_settings.gateway_auto_approve_deposits=false হলে PENDING_APPROVAL'
);

SELECT results_eq(
  $$ SELECT status::text FROM public.gateway_payments WHERE gateway_trx_id = 'WTRX-C1' $$,
  $$ VALUES ('PENDING'::text) $$,
  'request_wallet_deposit: PENDING_APPROVAL পথে gateway_payments.status=PENDING (SUCCESS না)'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111' $$,
  $$ VALUES (1420::numeric, 1150::numeric) $$,
  'request_wallet_deposit: PENDING_APPROVAL পথে balance/balance_user অপরিবর্তিত থাকে (কোনো transaction insert হয় না)'
);

----------------------------------------------------------------------
-- request_wallet_deposit — validation failure cases
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.request_wallet_deposit(50, 'BKASH', 'WTRX-D1', '', '', 'ADMIN') $$,
  'P0001',
  'INVALID_ROLE',
  'request_wallet_deposit: p_role শুধু USER/SOLVER নিতে পারে, নাহলে INVALID_ROLE'
);

SELECT throws_ok(
  $$ SELECT public.request_wallet_deposit(0, 'BKASH', 'WTRX-E1', '', '', 'USER') $$,
  'P0001',
  'INVALID_AMOUNT',
  'request_wallet_deposit: p_amount<=0 হলে INVALID_AMOUNT'
);

SELECT throws_ok(
  $$ SELECT public.request_wallet_deposit(50, 'PAYPAL', 'WTRX-F1', '', '', 'USER') $$,
  'P0001',
  'INVALID_GATEWAY',
  'request_wallet_deposit: p_gateway শুধু BKASH/NAGAD/ROCKET/CARD নিতে পারে, নাহলে INVALID_GATEWAY'
);

SELECT throws_ok(
  $$ SELECT public.request_wallet_deposit(50, 'BKASH', '', '', '', 'USER') $$,
  'P0001',
  'TRX_ID_REQUIRED',
  'request_wallet_deposit: খালি p_gateway_trx_id দিলে TRX_ID_REQUIRED (deposit_money_via_gateway-এর বিপরীতে — এই ফাংশনে খালি trx_id বাইপাস করে না)'
);

SELECT ok(
  (SELECT r->>'result' = 'ALREADY_SUBMITTED'
   FROM (SELECT public.request_wallet_deposit(50, 'BKASH', 'WTRX-A1', '', '', 'USER') AS r) s),
  'request_wallet_deposit: আগে থেকে ব্যবহৃত gateway_trx_id পুনরায় দিলে ALREADY_SUBMITTED'
);

SELECT test.logout();

----------------------------------------------------------------------
-- request_wallet_deposit — USER_NOT_FOUND (auth.uid() valid uuid কিন্তু users
-- টেবিলে কোনো row নেই)
----------------------------------------------------------------------
SELECT test.login_as('00000000-0000-0000-0000-000000000000');
SELECT throws_ok(
  $$ SELECT public.request_wallet_deposit(50, 'BKASH', 'WTRX-G1', '', '', 'USER') $$,
  'P0001',
  'USER_NOT_FOUND',
  'request_wallet_deposit: auth.uid()-এর সাথে মিলে এমন কোনো users row না থাকলে USER_NOT_FOUND'
);
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
