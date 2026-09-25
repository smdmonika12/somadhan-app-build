-- 10_admin_moderation_balance_part1.sql — Step 7 (Admin moderation & balance), PART 1 of 2
--
-- এই ফাইলে কভার করা হলো "money & account-status" থিমের ৯টা ফাংশন (২৫টা তালিকাভুক্ত
-- Step 7 ফাংশনের মধ্যে অর্ধেকটা — বাকি অর্ধেক PART 2-এর জন্য CI_TEST_SUITE_PROGRESS.md-এ
-- বিস্তারিত handoff নোট রাখা হয়েছে):
--   admin_adjust_balance (৪-আর্গ ও ৫-আর্গ overload দুটোই)  → recovered_admin_money.sql
--                                                             (কিন্তু আসল/effective সংজ্ঞা
--                                                             step36_transaction_role_column_and_rpc_dual_write.sql-এর,
--                                                             নিচের নোট দেখুন)
--   admin_confirm_gateway_deposit                          → ঐ একই, effective সংজ্ঞা step36-এর
--   admin_refund_and_reopen_problem                        → recovered_admin_money.sql (override হয়নি)
--   admin_credentials_get_phone/_update/_verify_password   → recovered_admin_credentials.sql
--   admin_set_banned (২-আর্গ ও ৩-আর্গ)                      → recovered_admin_kyc_ban_role.sql
--   admin_set_restricted (২-আর্গ ও ৩-আর্গ)                  → recovered_admin_kyc_ban_role.sql
--   admin_set_verified_badge (৩-আর্গ, কোনো ২-আর্গ ওভারলোড নেই) → recovered_admin_kyc_ban_role.sql
--
-- ⚠️⚠️ গুরুত্বপূর্ণ আবিষ্কার (migration সোর্স সরাসরি পড়ে নিশ্চিত করা, দুটো ফাইলে
-- একই ফাংশনের দুটো সংজ্ঞা মিলিয়ে): `full-test.yml`-এ migration apply হয়
-- `ls supabase/migrations/*.sql | sort` ক্রমে (alphabetical) — অর্থাৎ
-- `recovered_admin_money.sql` (r...) আগে apply হয়, তারপর
-- `step36_transaction_role_column_and_rpc_dual_write.sql` (s...) সেটাকে
-- `CREATE OR REPLACE` দিয়ে override করে। তাই real DB-তে **effective সংজ্ঞা সবসময়
-- step36-এরটাই** admin_adjust_balance (দুটো overload) আর admin_confirm_gateway_deposit-এর
-- জন্য — recovered_admin_money.sql-এর ভার্সন কখনো active থাকে না। নিচের টেস্টগুলো
-- step36-এর body অনুযায়ী লেখা হয়েছে (recovered-এর না)। এই ওভাররাইডের ফলে একটা
-- আচরণ-রিগ্রেশনও ধরা পড়েছে (DOCUMENTED CURRENT BEHAVIOUR হিসেবে নিচে টেস্ট করা):
-- recovered_admin_money.sql-এর মূল ভার্সনে admin_adjust_balance-এর notifications ও
-- admin_audit_logs INSERT-এ `role` কলাম লেখা হতো (৪-আর্গ ওভারলোডে '', ৫-আর্গে p_role) —
-- কিন্তু step36-এর override ভার্সনে **দুটো টেবিলের INSERT থেকেই `role` কলামটা সম্পূর্ণ
-- বাদ পড়ে গেছে** (শুধু transactions.role টিকে আছে)। admin_confirm_gateway_deposit-এও
-- একই প্যাটার্ন — notifications INSERT-এ role নেই step36-এ, যদিও recovered ভার্সনে ছিল।
-- এটা কোনো migration/ফাংশন এখানে বদলানো হয়নি (rule #1), শুধু বর্তমান আচরণ হিসেবে
-- লক করে রাখা হলো — মানুষের review-সাপেক্ষে সম্ভবত একটা প্রকৃত রিগ্রেশন-বাগ।
--
-- admin_refund_and_reopen_problem নামে "refund" থাকলেও ফাংশন-বডিতে escrows টেবিল
-- কোথাও ছোঁয়া হয় না (শুধু problems.status→OPEN + bid CANCELLED করে) — DOCUMENTED
-- CURRENT BEHAVIOUR হিসেবে নিচে টেস্ট করা হয়েছে (escrow status অপরিবর্তিত HELD থাকে)।
--
-- ⚠️ সব ৯টা ফাংশনের real body এই সেশনে সরাসরি পড়ে verify করা হয়েছে (grep -i দিয়ে
-- case-insensitive স্ক্যানে প্রতিটার definition কোথায় কোথায় আছে নিশ্চিত করার পর)।
--
-- ⚠️ এই সেশনেও sandbox network বন্ধ ছিল (`apt-get install postgresql
-- postgresql-16-pgtap` → 403 Forbidden, চেষ্টা করে নিশ্চিত হওয়া হয়েছে) — তাই এই
-- ফাইলও শুধু static ভাবে verify করা হয়েছে (body-র সাথে assertion মিলিয়ে,
-- error-message/notification-title migration টেক্সটের সাথে হুবহু মিলিয়ে, plan()
-- সংখ্যা script দিয়ে গুনে মিলিয়ে), real Postgres+pgTAP-এ কখনো চালানো হয়নি।
--
-- ⚠️⚠️ ২০২৬-০৯-২১ (Step 12.12, F1 ফিক্স, ব্যবহারকারীর অনুমতিতে) — এই ফাইল আপডেট:
-- step12_9 migration দিয়ে ৩টা পুরনো overload DROP হওয়ায় আগের "ambiguity যাচাই +
-- ট্রানজ্যাকশনের ভেতরে RENAME করে legacy আচরণ টেস্ট" কৌশল আর কাজ করে না (RENAME
-- এখন hard ERROR দেয়, কারণ ওই signature-এর ফাংশন সত্যিই আর নেই)। বিস্তারিত কী
-- বদলেছে ও কেন — CI_TEST_SUITE_PROGRESS.md-এর এই সেশনের "Step 12.12" সেকশন দেখুন।
-- plan() 71→66 (admin_adjust_balance-এর legacy-only-balance-column আচরণ বাস্তবিকভাবে
-- আর সম্ভব না বলে সেই ৩টা ambiguity-assertion বাদ, নতুন dual-write আচরণ ৫-arg-এই
-- টেস্ট করা)। এই সেশনেও sandbox network বন্ধ — নতুন/পরিবর্তিত সব assertion migration
-- body সরাসরি পড়ে static-ভাবে যাচাই করা হয়েছে (উপরে admin_adjust_balance ৫-arg
-- body quote করা আছে), real Postgres+pgTAP-এ কখনো চালানো হয়নি।

BEGIN;
SELECT plan(66);

SELECT test.seed_users();
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...  ADMIN: 99999999-...

----------------------------------------------------------------------
-- admin_adjust_balance — টেস্ট-নির্দিষ্ট target users
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, balance_solver, has_user_role, has_solver_role) VALUES
  ('b0000001-0001-0001-0001-000000000001', 'CLIENT', 'Adj 4arg Add',          '01730000001', 500, 500,   0, true,  false),
  ('b0000002-0002-0002-0002-000000000002', 'CLIENT', 'Adj 4arg Duplicate',    '01730000002', 300, 300,   0, true,  false),
  ('b0000003-0003-0003-0003-000000000003', 'CLIENT', 'Adj 4arg Clamp',       '01730000003',  50,  50,   0, true,  false),
  ('b0000004-0004-0004-0004-000000000004', 'SOLVER', 'Adj 5arg Solver Add',  '01730000004', 100,   0, 100, true,  true),
  ('b0000005-0005-0005-0005-000000000005', 'CLIENT', 'Adj 5arg User Clamp',  '01730000005',  20,  20,   0, true,  false),
  ('b0000006-0006-0006-0006-000000000006', 'CLIENT', 'Adj 5arg RoleInactive','01730000006',   0,   0,   0, true,  false);

-- ⚠️ ২০২৬-০৯-২১ (Step 12.12) আপডেট — DROP-জনিত পুনর্লিখন:
-- step12_9 part_b (migration `zz_20260921053223_...`) দিয়ে পুরনো p_role-বিহীন ৩টা
-- overload সম্পূর্ণ DROP হয়ে গেছে: admin_adjust_balance(uuid,numeric,boolean,text),
-- admin_set_banned(uuid,boolean), admin_set_restricted(uuid,boolean)। আগে (Step 8 PART 2)
-- এগুলো "ambiguous overload" ছিল (৪২৭২৫), এখন DROP হওয়ায় ambiguity নেই — কিন্তু দুই
-- ফাংশন-পরিবারের ফলাফল ভিন্ন (migration body সরাসরি পড়ে static-যাচাই করা, sandbox
-- network বন্ধ থাকায় real Postgres+pgTAP-এ চালানো যায়নি):
--   • admin_set_banned/admin_set_restricted — অবশিষ্ট ৩-arg overload-এ p_role
--     DEFAULT NULL, আর p_role NULL হলে body হুবহু পুরনো ২-arg overload-এর legacy কলাম
--     (is_banned/is_restricted) ছোঁয় ও notification role=coalesce(p_role,'')=''
--     লেখে — অর্থাৎ পুরনো ２-arg call syntax (`admin_set_banned(id, true)`) আজও
--     default-resolution দিয়ে identical আচরণ দেয়। তাই নিচে (admin_set_banned/
--     admin_set_restricted সেকশনে) `__legacy2` suffix ছাড়াই সরাসরি পুরনো call syntax
--     ব্যবহার করা হয়েছে — কোনো RENAME/আলাদা টেস্ট লাগে না, ambiguity assertion দুটো
--     শুধু বাদ দেওয়া হলো (আর প্রাসঙ্গিক না)।
--   • admin_adjust_balance — এখানে বাস্তব আচরণ-পরিবর্তন আছে: অবশিষ্ট ৫-arg overload-এ
--     p_role DEFAULT 'USER' (NULL না), আর body-তে কোনো "legacy-only balance column"
--     শাখা নেই — সবসময় role অনুযায়ী balance_user/balance_solver dual-write হয় এবং
--     transactions.role=p_role বসে (আগে unscoped ৪-arg কলে role='' লেখা হতো, সেই
--     শাখা এখন DROP হয়ে গেছে)। নিচের ব্লক তাই নতুন আচরণ অনুযায়ী সম্পূর্ণ পুনর্লিখিত।
RESET ROLE;
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_adjust_balance('b0000001-0001-0001-0001-000000000001', 100, true, 'bonus', 'USER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_adjust_balance: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- happy addition (role=USER) — DROP-পরবর্তী: পুরনো role-unscoped শাখা নেই, তাই সবসময় dual-write
SELECT is(
  (public.admin_adjust_balance('b0000001-0001-0001-0001-000000000001', 100, true, 'bonus', 'USER'))->>'result',
  'OK',
  'admin_adjust_balance: addition happy path (role=USER) → result OK'
);
SELECT results_eq(
  $$ SELECT balance, balance_user FROM public.users WHERE id = 'b0000001-0001-0001-0001-000000000001' $$,
  $$ VALUES (600::numeric(12,2), 600::numeric(12,2)) $$,
  'admin_adjust_balance: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী পুনর্লিখন) — পুরনো role-unscoped ৪-arg overload DROP হওয়ায় এখন সবসময় dual-write (balance ও balance_user দুটোই +100)'
);

-- transactions.role এখন সবসময় p_role (আগে unscoped কলে '' হতো, সেই overload আর নেই)
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.transactions
    WHERE user_id = 'b0000001-0001-0001-0001-000000000001'
      AND type = 'ADMIN_ADJUSTMENT' AND role = 'USER'
  ),
  'admin_adjust_balance: transactions row role=p_role (''USER'') দিয়ে insert হয় — DROP-পরবর্তী role-unclassified ('''') আর সম্ভব না'
);

-- notifications/admin_audit_logs — উভয় overload-এই (৪-arg ও ৫-arg) এই দুটো INSERT-এ কখনোই role কলাম ছিল না (body সরাসরি পড়ে যাচাই); DROP-এ এই অংশ বদলায়নি
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.notifications
    WHERE user_id = 'b0000001-0001-0001-0001-000000000001'
      AND target_type = 'balance' AND role IS NULL
  ),
  'admin_adjust_balance: DOCUMENTED CURRENT BEHAVIOUR — notifications INSERT-এ role কলাম নেই, তাই role সবসময় NULL'
);
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.admin_audit_logs
    WHERE target_id = 'b0000001-0001-0001-0001-000000000001'
      AND action_type = 'ADD_BALANCE' AND role IS NULL
  ),
  'admin_adjust_balance: DOCUMENTED CURRENT BEHAVIOUR — admin_audit_logs INSERT-এও role কলাম নেই, সবসময় NULL'
);

-- DUPLICATE_SKIPPED — একই signature (user+is_addition+amount+reason+role) দিয়ে দ্বিতীয়বার কল, ৫ সেকেন্ডের মধ্যে
SELECT is(
  (public.admin_adjust_balance('b0000002-0002-0002-0002-000000000002', 50, true, 'first-call', 'USER'))->>'result',
  'OK',
  'admin_adjust_balance: প্রথম কল স্বাভাবিকভাবে OK'
);
SELECT is(
  (public.admin_adjust_balance('b0000002-0002-0002-0002-000000000002', 50, true, 'first-call', 'USER'))->>'result',
  'DUPLICATE_SKIPPED',
  'admin_adjust_balance: হুবহু একই আর্গুমেন্ট (role-সহ) দিয়ে ৫ সেকেন্ডের মধ্যে আবার কল করলে DUPLICATE_SKIPPED'
);
SELECT results_eq(
  $$ SELECT balance, balance_user FROM public.users WHERE id = 'b0000002-0002-0002-0002-000000000002' $$,
  $$ VALUES (350::numeric(12,2), 350::numeric(12,2)) $$,
  'admin_adjust_balance: DUPLICATE_SKIPPED হলে balance/balance_user দ্বিতীয়বার বদলায় না (শুধু একবারই +50)'
);

-- 5-arg INVALID_ROLE
SELECT throws_ok(
  $$ SELECT public.admin_adjust_balance('b0000004-0004-0004-0004-000000000004', 10, true, 'x', 'ADMIN') $$,
  'P0001', 'INVALID_ROLE',
  'admin_adjust_balance(5-arg): p_role শুধু USER/SOLVER — অন্য কিছু দিলে INVALID_ROLE'
);

-- 5-arg ROLE_INACTIVE
SELECT throws_ok(
  $$ SELECT public.admin_adjust_balance('b0000006-0006-0006-0006-000000000006', 10, true, 'x', 'SOLVER') $$,
  'P0001', 'ROLE_INACTIVE',
  'admin_adjust_balance(5-arg): টার্গেটের has_solver_role=false হলে p_role=SOLVER দিয়ে কল করলে ROLE_INACTIVE'
);

-- 5-arg happy SOLVER addition — balance + balance_solver দুটোই বাড়ে
SELECT is(
  (public.admin_adjust_balance('b0000004-0004-0004-0004-000000000004', 50, true, 'bonus', 'SOLVER'))->>'result',
  'OK',
  'admin_adjust_balance(5-arg): SOLVER addition happy path → result OK'
);
SELECT results_eq(
  $$ SELECT balance, balance_solver FROM public.users WHERE id = 'b0000004-0004-0004-0004-000000000004' $$,
  $$ VALUES (150::numeric(12,2), 150::numeric(12,2)) $$,
  'admin_adjust_balance(5-arg): p_role=SOLVER হলে balance ও balance_solver দুটোই +50 হয় (dual-write)'
);

-- 5-arg happy USER deduction clamp
SELECT is(
  (public.admin_adjust_balance('b0000005-0005-0005-0005-000000000005', 100, false, 'penalty', 'USER'))->>'result',
  'OK',
  'admin_adjust_balance(5-arg): USER deduction happy path → result OK'
);
SELECT results_eq(
  $$ SELECT balance, balance_user FROM public.users WHERE id = 'b0000005-0005-0005-0005-000000000005' $$,
  $$ VALUES (0::numeric(12,2), 0::numeric(12,2)) $$,
  'admin_adjust_balance(5-arg): p_role=USER deduction amount(100) > balance(20) হলে balance ও balance_user দুটোই 0-তে clamp হয়'
);
SELECT test.logout();

----------------------------------------------------------------------
-- admin_confirm_gateway_deposit
----------------------------------------------------------------------
INSERT INTO public.gateway_payments (id, user_id, amount, gateway, status, role) VALUES
  ('PAY-APPROVE-1', '33333333-3333-3333-3333-333333333333', 200, 'bKash', 'PENDING', 'SOLVER'),
  ('PAY-REJECT-1',  '33333333-3333-3333-3333-333333333333',  80, 'bKash', 'PENDING', 'SOLVER');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_confirm_gateway_deposit('PAY-APPROVE-1', 'APPROVE') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_confirm_gateway_deposit: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT throws_ok(
  $$ SELECT public.admin_confirm_gateway_deposit('PAY-APPROVE-1', 'MAYBE') $$,
  'P0001', 'INVALID_ACTION',
  'admin_confirm_gateway_deposit: p_action শুধু APPROVE/REJECT — অন্য কিছু দিলে INVALID_ACTION'
);
SELECT throws_ok(
  $$ SELECT public.admin_confirm_gateway_deposit('NOPE', 'APPROVE') $$,
  'P0001', 'PAYMENT_NOT_FOUND',
  'admin_confirm_gateway_deposit: অস্তিত্বহীন p_payment_id হলে PAYMENT_NOT_FOUND'
);

-- happy APPROVE
SELECT is(
  (public.admin_confirm_gateway_deposit('PAY-APPROVE-1', 'APPROVE'))->>'result',
  'OK',
  'admin_confirm_gateway_deposit: APPROVE happy path → result OK'
);
SELECT results_eq(
  $$ SELECT status FROM public.gateway_payments WHERE id = 'PAY-APPROVE-1' $$,
  $$ VALUES ('SUCCESS'::text) $$,
  'admin_confirm_gateway_deposit: APPROVE-এর পর gateway_payments.status = SUCCESS'
);
SELECT results_eq(
  $$ SELECT balance, balance_solver FROM public.users WHERE id = '33333333-3333-3333-3333-333333333333' $$,
  $$ VALUES (200::numeric(12,2), 200::numeric(12,2)) $$,
  'admin_confirm_gateway_deposit: role=SOLVER payment হলে balance ও balance_solver দুটোই amount দিয়ে বাড়ে'
);
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.transactions
    WHERE id = 'TRX_DEP_PAY-APPROVE-1' AND type = 'WALLET_DEPOSIT'
      AND escrow_id = 'PAY-APPROVE-1' AND base_amount = 200 AND role = 'SOLVER'
  ),
  'admin_confirm_gateway_deposit: transactions row id=TRX_DEP_<payment_id>, escrow_id=payment_id, base_amount=amount, role=v_pay.role দিয়ে insert হয়'
);

-- ALREADY_PROCESSED (একই payment আবার APPROVE)
SELECT is(
  public.admin_confirm_gateway_deposit('PAY-APPROVE-1', 'APPROVE'),
  jsonb_build_object('result', 'ALREADY_PROCESSED', 'status', 'SUCCESS'),
  'admin_confirm_gateway_deposit: status ইতিমধ্যে PENDING না হলে (SUCCESS/FAILED) ALREADY_PROCESSED রিটার্ন করে, exception না'
);

-- REJECT
SELECT is(
  (public.admin_confirm_gateway_deposit('PAY-REJECT-1', 'REJECT'))->>'result',
  'REJECTED',
  'admin_confirm_gateway_deposit: REJECT happy path → result REJECTED'
);
SELECT results_eq(
  $$ SELECT status FROM public.gateway_payments WHERE id = 'PAY-REJECT-1' $$,
  $$ VALUES ('FAILED'::text) $$,
  'admin_confirm_gateway_deposit: REJECT-এর পর gateway_payments.status = FAILED'
);
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.notifications
    WHERE user_id = '33333333-3333-3333-3333-333333333333' AND title = 'রিচার্জ প্রত্যাখ্যাত'
  ),
  'admin_confirm_gateway_deposit: REJECT-এ "রিচার্জ প্রত্যাখ্যাত" শিরোনামের notification পাঠানো হয়'
);
SELECT results_eq(
  $$ SELECT balance, balance_solver FROM public.users WHERE id = '33333333-3333-3333-3333-333333333333' $$,
  $$ VALUES (200::numeric(12,2), 200::numeric(12,2)) $$,
  'admin_confirm_gateway_deposit: REJECT-এ balance/balance_solver অপরিবর্তিত থাকে (আগের APPROVE-এর ২০০-ই থাকে)'
);
SELECT test.logout();

----------------------------------------------------------------------
-- admin_refund_and_reopen_problem
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, accepted_bid_id, accepted_solver_id, accepted_solver_name, accepted_amount) VALUES
  ('PROB-REFUND-1', '11111111-1111-1111-1111-111111111111', 'Refund Test Problem', 'IN_PROGRESS',
   'BID-REFUND-1', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500);
INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status) VALUES
  ('BID-REFUND-1', 'PROB-REFUND-1', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'ACCEPTED');
INSERT INTO public.escrows (id, problem_id, user_id, solver_id, base_amount, status) VALUES
  ('ESC-REFUND-1', 'PROB-REFUND-1', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 500, 'HELD');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_refund_and_reopen_problem('PROB-REFUND-1') $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_refund_and_reopen_problem: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_refund_and_reopen_problem('NOPE') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'admin_refund_and_reopen_problem: অস্তিত্বহীন p_problem_id হলে PROBLEM_NOT_FOUND'
);

SELECT is(
  (public.admin_refund_and_reopen_problem('PROB-REFUND-1'))->>'result',
  'OK',
  'admin_refund_and_reopen_problem: happy path → result OK'
);
SELECT results_eq(
  $$ SELECT status, accepted_bid_id, accepted_solver_id, accepted_solver_name, accepted_amount
     FROM public.problems WHERE id = 'PROB-REFUND-1' $$,
  $$ VALUES ('OPEN'::text, NULL::text, NULL::uuid, NULL::text, NULL::numeric(12,2)) $$,
  'admin_refund_and_reopen_problem: problem status→OPEN, accepted_bid_id/accepted_solver_id/accepted_solver_name/accepted_amount সব NULL হয়ে যায়'
);
SELECT results_eq(
  $$ SELECT status, resolution_type FROM public.bids WHERE id = 'BID-REFUND-1' $$,
  $$ VALUES ('CANCELLED'::text, 'ADMIN_MANUAL_REFUND'::text) $$,
  'admin_refund_and_reopen_problem: accepted_bid_id দিয়ে খুঁজে পাওয়া bid CANCELLED + resolution_type=ADMIN_MANUAL_REFUND হয়ে যায়'
);
SELECT results_eq(
  $$ SELECT status FROM public.escrows WHERE id = 'ESC-REFUND-1' $$,
  $$ VALUES ('HELD'::text) $$,
  'admin_refund_and_reopen_problem: DOCUMENTED CURRENT BEHAVIOUR — নাম "refund" হলেও ফাংশন-বডি escrows টেবিল কোথাও ছোঁয় না, escrow status HELD-ই থেকে যায় (টাকা আসলে ফেরত যায় না)'
);
SELECT test.logout();

----------------------------------------------------------------------
-- admin_credentials_get_phone / admin_credentials_update / admin_credentials_verify_password
----------------------------------------------------------------------
-- কোনো admin_credentials row না থাকলে get_phone() → NULL (client fallback করে local default-এ)
SELECT ok(
  public.admin_credentials_get_phone() IS NULL,
  'admin_credentials_get_phone: কোনো row না থাকলে NULL রিটার্ন করে'
);

-- প্রথম কল — row নেই বলে নতুন insert হয়, p_new_password_hash NULL দেওয়া হলো যাতে
-- crypt(p_current_password, gen_salt('bf')) দিয়ে আসল bcrypt hash তৈরি হয় (পরে verify_password
-- দিয়ে যাচাই করা যায়)
SELECT is(
  public.admin_credentials_update('mySecret123', '01711112222', NULL),
  true,
  'admin_credentials_update: কোনো row না থাকলে নতুন row insert করে true রিটার্ন করে'
);
SELECT is(
  public.admin_credentials_get_phone(),
  '01711112222',
  'admin_credentials_get_phone: row তৈরির পর সঠিক phone রিটার্ন করে'
);
SELECT is(
  public.admin_credentials_verify_password('mySecret123'),
  true,
  'admin_credentials_verify_password: সঠিক password দিলে true (crypt() দিয়ে bcrypt hash মিলিয়ে)'
);
SELECT is(
  public.admin_credentials_verify_password('wrongPassword'),
  false,
  'admin_credentials_verify_password: ভুল password দিলে false'
);

-- দ্বিতীয়বার update — ভুল current password দিলে false, row অপরিবর্তিত
SELECT is(
  public.admin_credentials_update('wrongCurrent', '01799999999', NULL),
  false,
  'admin_credentials_update: ভুল p_current_password দিলে false রিটার্ন করে (কিছু আপডেট হয় না)'
);
SELECT is(
  public.admin_credentials_get_phone(),
  '01711112222',
  'admin_credentials_update: ভুল password-এর attempt-এর পরেও phone অপরিবর্তিত থাকে'
);

-- সঠিক current password দিয়ে update
SELECT is(
  public.admin_credentials_update('mySecret123', '01799999999', 'literal-new-hash'),
  true,
  'admin_credentials_update: সঠিক p_current_password দিলে phone/password_hash আপডেট হয়ে true রিটার্ন করে'
);
SELECT is(
  public.admin_credentials_get_phone(),
  '01799999999',
  'admin_credentials_get_phone: successful update-এর পর নতুন phone রিটার্ন করে'
);

----------------------------------------------------------------------
-- admin_set_banned
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('c0000001-0001-0001-0001-000000000001', 'CLIENT', 'Ban Target Legacy',     '01740000001', true, false),
  ('c0000002-0002-0002-0002-000000000002', 'SOLVER', 'Ban Target RoleScoped', '01740000002', true, true);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_set_banned('c0000001-0001-0001-0001-000000000001', true) $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_set_banned(2-arg): non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.admin_set_banned('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', true))->>'result',
  'USER_NOT_FOUND',
  'admin_set_banned(2-arg): অস্তিত্বহীন p_user_id → result USER_NOT_FOUND (exception না)'
);

SELECT is(
  (public.admin_set_banned('c0000001-0001-0001-0001-000000000001', true))->>'result',
  'OK',
  'admin_set_banned(2-arg): legacy (role-unspecified) happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_banned FROM public.users WHERE id = 'c0000001-0001-0001-0001-000000000001' $$,
  $$ VALUES (true) $$,
  'admin_set_banned(2-arg): legacy কলামে is_banned=true সেট হয়'
);
SELECT ok(
  EXISTS(
    SELECT 1 FROM public.notifications
    WHERE user_id = 'c0000001-0001-0001-0001-000000000001'
      AND title = 'অ্যাকাউন্ট স্থগিত (Banned) করা হয়েছে' AND role = ''
  ),
  'admin_set_banned(2-arg): "Banned" notification role='''' দিয়ে পাঠানো হয় (এই ফাংশন step36-এ override হয়নি, তাই role এখনো লেখা হয়)'
);

SELECT throws_ok(
  $$ SELECT public.admin_set_banned('c0000002-0002-0002-0002-000000000002', true, 'ADMIN') $$,
  'P0001', 'INVALID_ROLE',
  'admin_set_banned(3-arg): p_role শুধু USER/SOLVER (বা NULL) — অন্য কিছু দিলে INVALID_ROLE'
);

SELECT is(
  (public.admin_set_banned('c0000002-0002-0002-0002-000000000002', true, 'SOLVER'))->>'result',
  'OK',
  'admin_set_banned(3-arg): p_role=SOLVER happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_banned, is_banned_solver, is_banned_user
     FROM public.users WHERE id = 'c0000002-0002-0002-0002-000000000002' $$,
  $$ VALUES (false, true, false) $$,
  'admin_set_banned(3-arg): p_role=SOLVER হলে শুধু is_banned_solver বদলায়, legacy is_banned/is_banned_user অপরিবর্তিত থাকে'
);

SELECT is(
  (public.admin_set_banned('c0000002-0002-0002-0002-000000000002', true, 'USER'))->>'result',
  'OK',
  'admin_set_banned(3-arg): p_role=USER happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_banned, is_banned_solver, is_banned_user
     FROM public.users WHERE id = 'c0000002-0002-0002-0002-000000000002' $$,
  $$ VALUES (false, true, true) $$,
  'admin_set_banned(3-arg): p_role=USER হলে শুধু is_banned_user বদলায়, আগের is_banned_solver=true স্পর্শ হয় না'
);
SELECT test.logout();

----------------------------------------------------------------------
-- admin_set_restricted
----------------------------------------------------------------------
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT is(
  (public.admin_set_restricted('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', true))->>'result',
  'USER_NOT_FOUND',
  'admin_set_restricted(2-arg): অস্তিত্বহীন p_user_id → result USER_NOT_FOUND'
);
SELECT is(
  (public.admin_set_restricted('c0000001-0001-0001-0001-000000000001', true))->>'result',
  'OK',
  'admin_set_restricted(2-arg): legacy happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_restricted FROM public.users WHERE id = 'c0000001-0001-0001-0001-000000000001' $$,
  $$ VALUES (true) $$,
  'admin_set_restricted(2-arg): legacy কলামে is_restricted=true সেট হয়'
);

SELECT is(
  (public.admin_set_restricted('c0000002-0002-0002-0002-000000000002', true, 'SOLVER'))->>'result',
  'OK',
  'admin_set_restricted(3-arg): p_role=SOLVER happy path → result OK'
);
SELECT is(
  (public.admin_set_restricted('c0000002-0002-0002-0002-000000000002', true, 'USER'))->>'result',
  'OK',
  'admin_set_restricted(3-arg): p_role=USER happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_restricted, is_restricted_solver, is_restricted_user
     FROM public.users WHERE id = 'c0000002-0002-0002-0002-000000000002' $$,
  $$ VALUES (false, true, true) $$,
  'admin_set_restricted(3-arg): role-scoped দুটো কলই যথাক্রমে is_restricted_solver/is_restricted_user সেট করে, legacy is_restricted অপরিবর্তিত থাকে'
);
SELECT test.logout();

----------------------------------------------------------------------
-- admin_set_verified_badge (শুধু ৩-আর্গ, কোনো ২-আর্গ ওভারলোড নেই)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.admin_set_verified_badge('c0000001-0001-0001-0001-000000000001', true, NULL) $$,
  'P0001', 'NOT_AUTHORIZED',
  'admin_set_verified_badge: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT throws_ok(
  $$ SELECT public.admin_set_verified_badge('c0000002-0002-0002-0002-000000000002', true, 'ADMIN') $$,
  'P0001', 'INVALID_ROLE',
  'admin_set_verified_badge: p_role শুধু USER/SOLVER (বা NULL) — অন্য কিছু দিলে INVALID_ROLE'
);

-- legacy (p_role NULL)
SELECT is(
  (public.admin_set_verified_badge('c0000001-0001-0001-0001-000000000001', true, NULL))->>'result',
  'OK',
  'admin_set_verified_badge: legacy (p_role NULL) happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_verified_badge FROM public.users WHERE id = 'c0000001-0001-0001-0001-000000000001' $$,
  $$ VALUES (true) $$,
  'admin_set_verified_badge: p_role NULL হলে legacy is_verified_badge কলামে true সেট হয়'
);

-- role-scoped
SELECT is(
  (public.admin_set_verified_badge('c0000002-0002-0002-0002-000000000002', true, 'SOLVER'))->>'result',
  'OK',
  'admin_set_verified_badge: p_role=SOLVER happy path → result OK'
);
SELECT is(
  (public.admin_set_verified_badge('c0000002-0002-0002-0002-000000000002', true, 'USER'))->>'result',
  'OK',
  'admin_set_verified_badge: p_role=USER happy path → result OK'
);
SELECT results_eq(
  $$ SELECT is_verified_badge, verified_badge_solver, verified_badge_user
     FROM public.users WHERE id = 'c0000002-0002-0002-0002-000000000002' $$,
  $$ VALUES (false, true, true) $$,
  'admin_set_verified_badge: role-scoped কল দুটো যথাক্রমে verified_badge_solver/verified_badge_user সেট করে, legacy is_verified_badge অপরিবর্তিত থাকে'
);

-- USER_NOT_FOUND
SELECT is(
  (public.admin_set_verified_badge('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', true, NULL))->>'result',
  'USER_NOT_FOUND',
  'admin_set_verified_badge: অস্তিত্বহীন p_user_id → result USER_NOT_FOUND'
);
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
