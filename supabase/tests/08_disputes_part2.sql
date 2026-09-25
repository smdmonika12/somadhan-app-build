-- 08_disputes_part2.sql — Step 5 (Disputes), PART 2 of 2
--
-- এই ফাইলে কভার করা বাকি ২টা ফাংশন (Step 5-এর ৫টার বাকি অর্ধেক — প্রথম অর্ধেক
-- 08_disputes_part1.sql-এ: raise_dispute, settle_dispute, admin_manually_flag_dispute):
--   resolve_dispute(text, text, text, numeric)            — recovered_disputes.sql, এখন
--                                                        step40_dispute_split_dual_write_fix.sql
--                                                        দিয়ে CREATE OR REPLACE হয়ে গেছে (নিচের
--                                                        সব "ধাপ ৪০" assertion এই নতুন সংজ্ঞা ধরে)
--   resolve_dispute_split(text, text, numeric x5, text, text, integer)
--                                                        — step29_5_resolve_dispute_split.sql,
--                                                        cosmetic ফিক্স step40-এ (role='SOLVER',
--                                                        %%% message) — is_disputed=true অপরিবর্তিত
-- দুটোর real body এই সেশনে migration ফাইল থেকে সরাসরি পড়ে verify করা (অনুমান না),
-- আর case-insensitive grep-এ নিশ্চিত হয়েছে কোনো পরের migration এই দুটোকে override
-- করে না (প্রতিটার একটাই সংজ্ঞা)। resolve_dispute-এর RELEASE/REFUND branch যে
-- release_escrow / refund_escrow_once কল করে, সেগুলোর আসল body
-- step36_transaction_role_column_and_rpc_dual_write.sql থেকে পড়া।
--
-- ⚠️⚠️ এই ফাইল এই সেশনে real Postgres+pgTAP-এ চালিয়ে verify করা যায়নি —
-- sandbox-এ network বন্ধ ছিল (apt-get → 403 Forbidden, pip/npm ও ব্লক; আগের
-- সেশনে network ফিরে এসেছিল, এবার আবার চলে গেছে)। যা করা হয়েছে তা static
-- verification: (ক) প্রতিটা assertion-এর প্রত্যাশিত মান Python Decimal দিয়ে আসল
-- SQL-এর arithmetic (round(...,2), clamp, ৳২ tolerance) মডেল করে যাচাই, (খ)
-- assertion সংখ্যা স্ক্রিপ্টে গুনে plan()-এ বসানো, (গ) শুধু সেই pgTAP কনস্ট্রাক্ট
-- ব্যবহার করা যেগুলো আগের ফাইলগুলোতে (01–08_part1) real-run-এ আগেই প্রমাণিত:
-- is / isnt / ok / results_eq / throws_ok(sql,'P0001',msg,desc) আর একটা
-- single-call `ok((SELECT r->>... FROM (SELECT public.f(...) AS r) s), ...)` প্যাটার্ন।
-- পরের সেশনে Postgres পাওয়া গেলে সবার আগে এই ফাইল চালিয়ে দেখতে হবে।
--
-- schema stub: 01 + 05 + 07 + 08_disputes_schema_stub.sql — নতুন কোনো কলাম লাগেনি
-- (dispute_split_solver_percent / dispute_result_seen_by_* / integer-typed
-- dispute_progress_at_settlement আগের সেশনেই 08 stub-এ যোগ হয়েছে)।
--
-- ⚠️ commission-সংক্রান্ত সংখ্যা (resolve_dispute-এর SPLIT branch) `resolve_commission_rate()`
-- stub-এর ফিক্সড ১০%-এর উপর নির্ভরশীল (rule #6 — আসল helper migrations-এ নেই)।
-- নিচের প্রথম assertion সেই অনুমান স্পষ্ট করে যাচাই করে — আসল helper দিয়ে stub
-- বদলালে সবার আগে এটাই fail করবে, তখন SP1–SP4-র সংখ্যাগুলো নতুন রেট দিয়ে আবার হিসাব করতে হবে।
--
-- ইচ্ছাকৃতভাবে টেস্ট না করা:
--   • resolve_dispute_split-এর SOLVER_NOT_FOUND — escrows.solver_id-এ users(id)-এর FK
--     আর has_solver_role NOT NULL থাকায় stub-এ এই branch-এ পৌঁছানো যায় না
--     (release_escrow-এর SOLVER_NOT_FOUND-এর মতোই — 05 ফাইলের হেডার দেখুন)।
--
-- 📌 "DOCUMENTED CURRENT BEHAVIOUR" চিহ্নিত assertion-গুলো ফাংশনের বাস্তব আচরণ
-- (migration পড়ে যা পাওয়া গেছে) লিখে রাখে — এগুলো "এটাই সঠিক" দাবি করে না, বরং
-- ভবিষ্যতে কেউ আচরণ বদলালে যেন সচেতনভাবে টেস্টও বদলাতে হয়। প্রতিটার পাশে কারণ লেখা।

BEGIN;
-- ⚠️ ধাপ ৪০ আপডেট (2026-09-20): supabase/migrations/step40_dispute_split_dual_write_fix.sql
-- প্রয়োগ হওয়ার পর resolve_dispute-এর আচরণ বদলেছে — নিচের অনেক "DOCUMENTED CURRENT
-- BEHAVIOUR" assertion এখন "ধাপ ৪০" লেবেলে নতুন প্রত্যাশিত মান দিয়ে আপডেট হয়েছে:
-- is_disputed এখন resolve-এর পরে false; SPLIT branch balance_solver/balance_user
-- dual-write + transactions.role + job_status='JOB_COMPLETED' + clamped percent;
-- SPLIT branch এখন solver-এর has_solver_role চেক করে (নতুন SPZ ফিক্সচার); RELEASE_TO_SOLVER/
-- REFUND_TO_USER-এ HELD escrow না থাকলে এখন exception (আগে silently OK ছিল)।
SELECT plan(87);

-- ----------------------------------------------------------------------
-- লোকাল helper (শুধু এই ফাইলের transaction-এর ভেতরে, ROLLBACK-এ চলে যায়):
-- resolve_dispute-এর ALREADY_RESOLVED / resolve_dispute_split-এর
-- AMOUNT_EXCEEDS_ESCROW / PERCENT_MISMATCH error message-এ ভ্যারিয়েবল
-- (problem_id + timestamp, numeric-এর দশমিক) interpolate হয়, তাই exact-match
-- না করে prefix-match করা হয় (Step 4-এর numeric-formatting শিক্ষা — 06 হ্যান্ডঅফ নোট)।
-- ব্যর্থ statement-এর effect sub-transaction-এ rollback হয়ে যায়।
-- ----------------------------------------------------------------------
CREATE OR REPLACE FUNCTION test.error_of(p_sql text) RETURNS text
LANGUAGE plpgsql AS $fn$
BEGIN
  EXECUTE p_sql;
  RETURN NULL;
EXCEPTION WHEN OTHERS THEN
  RETURN SQLERRM;
END;
$fn$;

SELECT test.seed_users();
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...  ADMIN: 99999999-...

-- ======================================================================
-- FIXTURES (সবই postgres superuser হিসেবে, কোনো login-এর আগে)
-- ======================================================================

-- solver1 সক্রিয় (release_escrow-এ has_solver_role=true লাগে); solver2 ইচ্ছাকৃতভাবে
-- নিষ্ক্রিয় থাকে (has_solver_role ডিফল্ট false) — SOLVER_ROLE_INACTIVE টেস্টের জন্য।
UPDATE public.users SET has_solver_role = true WHERE id = '22222222-2222-2222-2222-222222222222';

-- প্রতিটা money-scenario-র জন্য আলাদা owner/solver — যাতে cumulative balance হাতে ট্র্যাক
-- করতে না হয় (আগের ফাইলগুলোর ঝুঁকিপূর্ণ প্যাটার্ন)। owner: balance/balance_user = 1000;
-- solver: সব ০; শুধু has_solver_role=true।
-- ⚠️ Step 6 PART 2 সেশনে ফিক্স: এই INSERT-এ আগে একটা `display_uid` কলাম হার্ডকোডেড
-- টেক্সট ('T-OWN-A' ইত্যাদি) দিয়ে সেট করা হতো, যেটা 01-stub-এর তখনকার ভুল `text`
-- টাইপের সাথে মিলত। কলামটা `bigint`-এ ফিক্স হওয়ায় (দেখুন 01_bidding_flow_schema_stub.sql)
-- এই টেক্সট মানগুলো insert-এ cast error দিত — তাই বাদ দেওয়া হলো (00_helpers.sql-এর
-- seed_users()-এর মতোই; migration apply থাকলে trigger auto-fill করে, না থাকলে NULL থাকে)।
INSERT INTO public.users (id, role, name, phone, balance, balance_user, balance_solver, has_user_role, has_solver_role) VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', 'CLIENT', 'Owner A (REFUND)',   '01710000001', 1000, 1000, 0, true, false),
  ('aaaaaaaa-0000-0000-0000-000000000011', 'CLIENT', 'Owner B (SP1)',      '01710000011', 1000, 1000, 0, true, false),
  ('aaaaaaaa-0000-0000-0000-000000000012', 'SOLVER', 'Solver B (SP1)',     '01710000012', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000021', 'CLIENT', 'Owner C (SP2)',      '01710000021', 1000, 1000, 0, true, false),
  ('aaaaaaaa-0000-0000-0000-000000000022', 'SOLVER', 'Solver C (SP2)',     '01710000022', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000031', 'CLIENT', 'Owner D (SP3)',      '01710000031', 1000, 1000, 0, true, false),
  ('aaaaaaaa-0000-0000-0000-000000000032', 'SOLVER', 'Solver D (SP3)',     '01710000032', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000041', 'CLIENT', 'Owner E (SP4)',      '01710000041', 1000, 1000, 0, true, false),
  ('aaaaaaaa-0000-0000-0000-000000000042', 'SOLVER', 'Solver E (SP4)',     '01710000042', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000051', 'CLIENT', 'Owner F (RDS-A)',    '01710000051', 1000, 1000, 0, true, false),
  ('aaaaaaaa-0000-0000-0000-000000000052', 'SOLVER', 'Solver F (RDS-A)',   '01710000052', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000062', 'SOLVER', 'Solver G (RDS-B)',   '01710000062', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000072', 'SOLVER', 'Solver H (RDS-C)',   '01710000072', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000082', 'SOLVER', 'Solver I (RDS-E)',   '01710000082', 0, 0, 0, true, true),
  ('aaaaaaaa-0000-0000-0000-000000000092', 'SOLVER', 'Solver J (RDS-PAID)','01710000092', 0, 0, 0, true, true);

-- Problems — সবই is_disputed=true (বিরোধ চলছে), কোনোটাই এখনো resolved না।
-- applied_commission_rate=10 শুধু release_escrow পথে (R1/R3/R4) লাগে।
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, accepted_solver_name, is_disputed, dispute_reason, applied_commission_rate) VALUES
  ('R1',   '11111111-1111-1111-1111-111111111111',  'RD Release 1',            'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', 10.00),
  ('R2',   '11111111-1111-1111-1111-111111111111',  'RD Release (no escrow)',  'IN_PROGRESS', NULL,   NULL,              true, 'টেস্ট বিরোধ', 10.00),
  ('R3',   '11111111-1111-1111-1111-111111111111',  'RD Release (stale+held)', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', 10.00),
  ('R4',   '11111111-1111-1111-1111-111111111111',  'RD Release (inactive)',   'IN_PROGRESS', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', true, 'টেস্ট বিরোধ', 10.00),
  ('U1',   '11111111-1111-1111-1111-111111111111',  'RD Unknown resolution',   'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', NULL),
  ('F1',   'aaaaaaaa-0000-0000-0000-000000000001', 'RD Refund 1',             'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', NULL),
  ('F2',   '11111111-1111-1111-1111-111111111111',  'RD Refund (no escrow)',   'IN_PROGRESS', NULL,   NULL,              true, 'টেস্ট বিরোধ', NULL),
  ('SP1',  'aaaaaaaa-0000-0000-0000-000000000011', 'RD Split 50/50',          'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000012', 'Solver B',        true, 'টেস্ট বিরোধ', NULL),
  ('SP2',  'aaaaaaaa-0000-0000-0000-000000000021', 'RD Split custom 30',      'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000022', 'Solver C',        true, 'টেস্ট বিরোধ', NULL),
  ('SP3',  'aaaaaaaa-0000-0000-0000-000000000031', 'RD Split clamp-high',     'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000032', 'Solver D',        true, 'টেস্ট বিরোধ', NULL),
  ('SP4',  'aaaaaaaa-0000-0000-0000-000000000041', 'RD Split clamp-low',      'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000042', 'Solver E',        true, 'টেস্ট বিরোধ', NULL),
  ('SPN1', '11111111-1111-1111-1111-111111111111',  'RD Split (no escrow)',    'IN_PROGRESS', NULL,   NULL,              true, 'টেস্ট বিরোধ', NULL),
  ('SPN2', '11111111-1111-1111-1111-111111111111',  'RD Split (released only)','IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', NULL),
  ('RSA',  'aaaaaaaa-0000-0000-0000-000000000051', 'RDS-A realistic flow',    'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000052', 'Solver F',        true, 'টেস্ট বিরোধ', NULL),
  ('RSB',  '11111111-1111-1111-1111-111111111111',  'RDS-B solver 100%',       'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000062', 'Solver G',        true, 'টেস্ট বিরোধ', NULL),
  ('RSC',  '11111111-1111-1111-1111-111111111111',  'RDS-C tolerance edge',    'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000072', 'Solver H',        true, 'টেস্ট বিরোধ', NULL),
  ('RSE',  '11111111-1111-1111-1111-111111111111',  'RDS-E solver 0%',         'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000082', 'Solver I',        true, 'টেস্ট বিরোধ', NULL),
  ('RSP',  '11111111-1111-1111-1111-111111111111',  'RDS ALREADY_PAID',        'IN_PROGRESS', 'aaaaaaaa-0000-0000-0000-000000000092', 'Solver J',        true, 'টেস্ট বিরোধ', NULL),
  ('RSX',  '11111111-1111-1111-1111-111111111111',  'RDS error cases',         'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', NULL),
  ('RSY',  '11111111-1111-1111-1111-111111111111',  'RDS mismatch target',     'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, 'টেস্ট বিরোধ', NULL),
  ('RSZ',  '11111111-1111-1111-1111-111111111111',  'RDS inactive solver',     'IN_PROGRESS', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', true, 'টেস্ট বিরোধ', NULL),
  ('SPZ',  '11111111-1111-1111-1111-111111111111',  'RD Split inactive solver','IN_PROGRESS', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', true, 'টেস্ট বিরোধ', NULL);

-- Escrows (সবই HELD, যেখানে অন্যথা লেখা নেই)। ER3OLD পুরোনো+REFUNDED — resolve_dispute
-- শুধু status='HELD' escrow বেছে নেয়, এটা যেন কখনো বাছাই না হয়।
INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, extra_amount, status, created_at) VALUES
  ('ER1',    'R1',   'RD Release 1',            '11111111-1111-1111-1111-111111111111',  '22222222-2222-2222-2222-222222222222', 500,  0,   'HELD',     now()),
  ('ER3',    'R3',   'RD Release (stale+held)', '11111111-1111-1111-1111-111111111111',  '22222222-2222-2222-2222-222222222222', 100,  0,   'HELD',     now()),
  ('ER3OLD', 'R3',   'RD Release (stale+held)', '11111111-1111-1111-1111-111111111111',  '22222222-2222-2222-2222-222222222222', 100,  0,   'REFUNDED', now() - interval '2 days'),
  ('ER4',    'R4',   'RD Release (inactive)',   '11111111-1111-1111-1111-111111111111',  '33333333-3333-3333-3333-333333333333', 200,  0,   'HELD',     now()),
  ('EU1',    'U1',   'RD Unknown resolution',   '11111111-1111-1111-1111-111111111111',  '22222222-2222-2222-2222-222222222222', 100,  0,   'HELD',     now()),
  ('EF1',    'F1',   'RD Refund 1',             'aaaaaaaa-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 300,  50,  'HELD',     now()),
  ('ESP1',   'SP1',  'RD Split 50/50',          'aaaaaaaa-0000-0000-0000-000000000011', 'aaaaaaaa-0000-0000-0000-000000000012', 400,  100, 'HELD',     now()),
  ('ESP2',   'SP2',  'RD Split custom 30',      'aaaaaaaa-0000-0000-0000-000000000021', 'aaaaaaaa-0000-0000-0000-000000000022', 1000, 0,   'HELD',     now()),
  ('ESP3',   'SP3',  'RD Split clamp-high',     'aaaaaaaa-0000-0000-0000-000000000031', 'aaaaaaaa-0000-0000-0000-000000000032', 200,  0,   'HELD',     now()),
  ('ESP4',   'SP4',  'RD Split clamp-low',      'aaaaaaaa-0000-0000-0000-000000000041', 'aaaaaaaa-0000-0000-0000-000000000042', 300,  0,   'HELD',     now()),
  ('ESPN2',  'SPN2', 'RD Split (released only)','11111111-1111-1111-1111-111111111111',  '22222222-2222-2222-2222-222222222222', 100,  0,   'RELEASED', now()),
  ('ERA',    'RSA',  'RDS-A realistic flow',    'aaaaaaaa-0000-0000-0000-000000000051', 'aaaaaaaa-0000-0000-0000-000000000052', 600,  0,   'HELD',     now()),
  ('ERB',    'RSB',  'RDS-B solver 100%',       '11111111-1111-1111-1111-111111111111',  'aaaaaaaa-0000-0000-0000-000000000062', 200,  0,   'HELD',     now()),
  ('ERC',    'RSC',  'RDS-C tolerance edge',    '11111111-1111-1111-1111-111111111111',  'aaaaaaaa-0000-0000-0000-000000000072', 500,  100, 'HELD',     now()),
  ('ERE',    'RSE',  'RDS-E solver 0%',         '11111111-1111-1111-1111-111111111111',  'aaaaaaaa-0000-0000-0000-000000000082', 300,  0,   'HELD',     now()),
  ('ERP',    'RSP',  'RDS ALREADY_PAID',        '11111111-1111-1111-1111-111111111111',  'aaaaaaaa-0000-0000-0000-000000000092', 200,  0,   'HELD',     now()),
  ('EX',     'RSX',  'RDS error cases',         '11111111-1111-1111-1111-111111111111',  '22222222-2222-2222-2222-222222222222', 500,  100, 'HELD',     now()),
  ('EZ',     'RSZ',  'RDS inactive solver',     '11111111-1111-1111-1111-111111111111',  '33333333-3333-3333-3333-333333333333', 300,  0,   'HELD',     now()),
  ('ESPZ',   'SPZ',  'RD Split inactive solver','11111111-1111-1111-1111-111111111111',  '33333333-3333-3333-3333-333333333333', 200,  0,   'HELD',     now());

-- ALREADY_PAID fixture: resolve_dispute_split-এর deterministic transaction id
-- (TRX_SPLIT_<problem_id>) আগে থেকেই আছে, কিন্তু problem এখনো unresolved — মানে আগের কোনো
-- dual-write attempt টাকা দিয়েছিল কিন্তু problem-row আপডেটের আগে বিচ্ছিন্ন হয়েছিল।
INSERT INTO public.transactions (id, problem_id, type) VALUES ('TRX_SPLIT_RSP', 'RSP', 'PAYMENT');

-- ======================================================================
-- ০. Fixture sanity — resolve_commission_rate() stub-এর ফিক্সড ১০%
-- ======================================================================
SELECT is(
  public.resolve_commission_rate('aaaaaaaa-0000-0000-0000-000000000012')::numeric,
  10::numeric,
  'fixture sanity: resolve_commission_rate() = 10 (stub) — নিচের SP1–SP4 commission সংখ্যাগুলো এই অনুমানের উপর; আসল helper এলে সেগুলো আবার হিসাব করতে হবে'
);

-- ======================================================================
-- ক. non-admin (client) — দুটো ফাংশনই admin-only
-- ======================================================================
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT throws_ok(
  $$ SELECT public.resolve_dispute('R1', 'RELEASE_TO_SOLVER', 'x') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'resolve_dispute: non-admin (problem-এর owner নিজেও) কল করলে NOT_AUTHORIZED'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('RSX', 'EX', 50, 300, 30, 270, 300, 'SPLIT_SETTLEMENT', 'x') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'resolve_dispute_split: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.logout();

-- এখন থেকে সব admin হিসেবে
RESET ROLE;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

-- ======================================================================
-- খ. resolve_dispute — সাধারণ guard
-- ======================================================================
SELECT throws_ok(
  $$ SELECT public.resolve_dispute('NO_SUCH_PROBLEM', 'RELEASE_TO_SOLVER', 'x') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'resolve_dispute: PROBLEM_NOT_FOUND'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute('U1', 'BOGUS', 'x') $$,
  'P0001',
  'UNKNOWN_RESOLUTION: BOGUS',
  'resolve_dispute: অচেনা p_resolution হলে UNKNOWN_RESOLUTION: <মান>'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'EU1'),
            (SELECT dispute_resolved_at IS NULL FROM public.problems WHERE id = 'U1') $$,
  $$ VALUES ('HELD'::text, true) $$,
  'resolve_dispute: UNKNOWN_RESOLUTION-এ কোনো আংশিক পরিবর্তন থাকে না (escrow HELD, problem unresolved)'
);

-- ======================================================================
-- গ. resolve_dispute — RELEASE_TO_SOLVER, happy path (R1: escrow 500, solver1 @ ১০% → net 450)
-- ======================================================================
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'resolution' = 'RELEASE_TO_SOLVER'
      AND r->'release'->>'result' = 'OK' AND (r->'release'->>'net_amount')::numeric = 450
   FROM (SELECT public.resolve_dispute('R1', 'RELEASE_TO_SOLVER', 'প্রমাণ সঠিক') AS r) s),
  'resolve_dispute RELEASE_TO_SOLVER: OK + release_escrow-এর ফলাফল (net_amount=450) নেস্টেড রিটার্ন হয়'
);

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'ER1'),
  'RELEASED',
  'resolve_dispute RELEASE_TO_SOLVER: HELD escrow RELEASED হয়'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222' $$,
  $$ VALUES (450::numeric, 450::numeric) $$,
  'resolve_dispute RELEASE_TO_SOLVER: solver-এর balance আর balance_solver দুটোই ৪৫০ (release_escrow-এর dual-write পথ)'
);

SELECT results_eq(
  $$ SELECT status, job_status, dispute_resolution_decision, dispute_resolution_type, dispute_resolution_note,
            dispute_result_seen_by_user, dispute_result_seen_by_solver
     FROM public.problems WHERE id = 'R1' $$,
  $$ VALUES ('COMPLETED'::text, 'JOB_COMPLETED'::text, 'RELEASE_TO_SOLVER'::text, 'RELEASE_TO_SOLVER'::text, 'প্রমাণ সঠিক'::text, false, false) $$,
  'resolve_dispute RELEASE_TO_SOLVER: problem COMPLETED/JOB_COMPLETED + decision/type/note + দুই পক্ষের result_seen=false'
);

SELECT ok(
  (SELECT dispute_resolved_at IS NOT NULL AND dispute_settled_at IS NOT NULL AND completed_at IS NOT NULL
   FROM public.problems WHERE id = 'R1'),
  'resolve_dispute RELEASE_TO_SOLVER: dispute_resolved_at, dispute_settled_at, completed_at সব সেট'
);

-- ধাপ ৪০ ফিক্স: resolve-এর পরে is_disputed=false হয়ে যায় (dispute আর কখনো "open"
-- থাকে না — ব্যবহারকারীর সিদ্ধান্ত)। settle_dispute/withdraw_dispute-এর সাথে এখন সঙ্গতিপূর্ণ।
SELECT is(
  (SELECT is_disputed FROM public.problems WHERE id = 'R1'),
  false,
  'ধাপ ৪০: resolve_dispute RELEASE_TO_SOLVER-এর পরে is_disputed=false হয়ে যায়'
);

SELECT results_eq(
  $$ SELECT type, net_amount::numeric, escrow_id FROM public.transactions WHERE id = 'TRX_RELEASE_ER1' $$,
  $$ VALUES ('PAYMENT'::text, 450::numeric, 'ER1'::text) $$,
  'resolve_dispute RELEASE_TO_SOLVER: release_escrow-এর PAYMENT transaction (net 450) তৈরি হয়'
);

SELECT ok(
  coalesce(test.error_of($$ SELECT public.resolve_dispute('R1', 'RELEASE_TO_SOLVER', 'আবার') $$)
           LIKE 'ALREADY_RESOLVED: dispute for R1 was already resolved at %', false),
  'resolve_dispute: দ্বিতীয়বার কল করলে ALREADY_RESOLVED (message-এ problem id + timestamp, prefix-match)'
);

SELECT is(
  (SELECT balance FROM public.users WHERE id = '22222222-2222-2222-2222-222222222222')::numeric,
  450::numeric,
  'resolve_dispute: ALREADY_RESOLVED-এর পরে solver-এর balance অপরিবর্তিত (ডাবল-পেআউট নেই)'
);

-- ======================================================================
-- ঘ. resolve_dispute — RELEASE_TO_SOLVER, কোনো HELD escrow নেই (R2)
-- ধাপ ৪০ ফিক্স: এখন silently OK হয়ে যাওয়ার বদলে exception — escrow ছাড়া dispute
-- resolve করা যাবে না (ব্যবহারকারীর সিদ্ধান্ত: escrow না থাকলে dispute পর্যন্ত যাওয়ারই
-- কোনো way নেই, তাই resolve-ও হওয়া উচিত না)।
-- ======================================================================
SELECT throws_ok(
  $$ SELECT public.resolve_dispute('R2', 'RELEASE_TO_SOLVER', 'escrow ছাড়া') $$,
  'P0001',
  'NO_ESCROW_TO_RELEASE',
  'ধাপ ৪০: resolve_dispute RELEASE_TO_SOLVER-এ HELD escrow না থাকলে NO_ESCROW_TO_RELEASE'
);

SELECT results_eq(
  $$ SELECT status, dispute_resolved_at IS NULL, (SELECT count(*)::int FROM public.transactions WHERE problem_id = 'R2')
     FROM public.problems WHERE id = 'R2' $$,
  $$ VALUES ('IN_PROGRESS'::text, true, 0) $$,
  'ধাপ ৪০: resolve_dispute RELEASE_TO_SOLVER (escrow ছাড়া) ব্যর্থ হলে problem অপরিবর্তিত থাকে (rollback)'
);

-- ======================================================================
-- ঙ. resolve_dispute — শুধু status=HELD escrow বাছাই হয় (R3: পুরোনো REFUNDED + নতুন HELD)
-- ======================================================================
SELECT is(
  (SELECT r->>'result' FROM (SELECT public.resolve_dispute('R3', 'RELEASE_TO_SOLVER', 'stale escrow উপেক্ষা') AS r) s),
  'OK',
  'resolve_dispute RELEASE_TO_SOLVER: একই problem-এ পুরোনো REFUNDED + নতুন HELD escrow থাকলে OK'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'ER3'),
            (SELECT status FROM public.escrows WHERE id = 'ER3OLD') $$,
  $$ VALUES ('RELEASED'::text, 'REFUNDED'::text) $$,
  'resolve_dispute: শুধু HELD escrow (ER3) RELEASED হয়, পুরোনো REFUNDED escrow (ER3OLD) ছোঁয়া হয় না'
);

SELECT results_eq(
  $$ SELECT (SELECT count(*)::int FROM public.transactions WHERE id = 'TRX_RELEASE_ER3'),
            (SELECT count(*)::int FROM public.transactions WHERE id = 'TRX_RELEASE_ER3OLD') $$,
  $$ VALUES (1, 0) $$,
  'resolve_dispute: পুরোনো REFUNDED escrow-এর জন্য কোনো নতুন release transaction তৈরি হয় না'
);

-- ======================================================================
-- চ. resolve_dispute — release_escrow-এর SOLVER_ROLE_INACTIVE propagate করে (R4)
-- ======================================================================
SELECT throws_ok(
  $$ SELECT public.resolve_dispute('R4', 'RELEASE_TO_SOLVER', 'x') $$,
  'P0001',
  'SOLVER_ROLE_INACTIVE',
  'resolve_dispute RELEASE_TO_SOLVER: solver-এর has_solver_role=false হলে release_escrow-এর SOLVER_ROLE_INACTIVE propagate করে'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'ER4'),
            (SELECT dispute_resolved_at IS NULL FROM public.problems WHERE id = 'R4') $$,
  $$ VALUES ('HELD'::text, true) $$,
  'resolve_dispute: SOLVER_ROLE_INACTIVE-এ পুরো কল rollback — escrow HELD, problem unresolved'
);

-- ======================================================================
-- ছ. resolve_dispute — REFUND_TO_USER, happy path (F1: escrow 300+50=350, owner A)
-- ======================================================================
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'resolution' = 'REFUND_TO_USER'
      AND r->'refund'->>'result' = 'OK' AND (r->'refund'->>'amount')::numeric = 350
   FROM (SELECT public.resolve_dispute('F1', 'REFUND_TO_USER', 'গ্রাহকের পক্ষে') AS r) s),
  'resolve_dispute REFUND_TO_USER: OK + refund_escrow_once-এর ফলাফল (amount=350, ১০০%) নেস্টেড রিটার্ন হয়'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000001' $$,
  $$ VALUES (1350::numeric, 1350::numeric) $$,
  'resolve_dispute REFUND_TO_USER: owner-এর balance আর balance_user দুটোই ১০০০ → ১৩৫০ (refund_escrow_once-এর dual-write)'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'EF1'), t.type, t.refund_type,
            t.refund_percentage::numeric, t.net_amount::numeric
     FROM public.transactions t WHERE t.id = 'TRX_REFUND_EF1' $$,
  $$ VALUES ('REFUNDED'::text, 'REFUND'::text, 'DISPUTE_REFUND'::text, 100::numeric, 350::numeric) $$,
  'resolve_dispute REFUND_TO_USER: escrow REFUNDED + REFUND transaction (refund_type=DISPUTE_REFUND, ১০০%, net 350)'
);

SELECT results_eq(
  $$ SELECT status, job_status, is_disputed, dispute_resolution_decision, dispute_resolution_type, dispute_resolution_note,
            dispute_result_seen_by_user, dispute_result_seen_by_solver
     FROM public.problems WHERE id = 'F1' $$,
  $$ VALUES ('CANCELLED'::text, NULL::text, false, 'REFUND_TO_USER'::text, 'REFUND_TO_USER'::text, 'গ্রাহকের পক্ষে'::text, false, false) $$,
  'ধাপ ৪০: resolve_dispute REFUND_TO_USER: problem CANCELLED, is_disputed=false + decision/type/note + result_seen=false'
);

SELECT ok(
  (SELECT dispute_resolved_at IS NOT NULL AND dispute_settled_at IS NOT NULL FROM public.problems WHERE id = 'F1'),
  'resolve_dispute REFUND_TO_USER: dispute_resolved_at আর dispute_settled_at সেট'
);

SELECT ok(
  coalesce(test.error_of($$ SELECT public.resolve_dispute('F1', 'REFUND_TO_USER', 'আবার') $$)
           LIKE 'ALREADY_RESOLVED: dispute for F1 was already resolved at %', false),
  'resolve_dispute REFUND_TO_USER: দ্বিতীয়বার কল করলে ALREADY_RESOLVED'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000001' $$,
  $$ VALUES (1350::numeric, 1350::numeric) $$,
  'resolve_dispute REFUND_TO_USER: ALREADY_RESOLVED-এর পরে owner-এর balance অপরিবর্তিত (ডাবল-রিফান্ড নেই)'
);

-- ======================================================================
-- জ. resolve_dispute — REFUND_TO_USER, কোনো HELD escrow নেই (F2)
-- ধাপ ৪০ ফিক্স: এখন silently OK-এর বদলে exception।
-- ======================================================================
SELECT throws_ok(
  $$ SELECT public.resolve_dispute('F2', 'REFUND_TO_USER', 'escrow ছাড়া') $$,
  'P0001',
  'NO_ESCROW_TO_REFUND',
  'ধাপ ৪০: resolve_dispute REFUND_TO_USER-এ HELD escrow না থাকলে NO_ESCROW_TO_REFUND'
);

SELECT results_eq(
  $$ SELECT status, dispute_resolved_at IS NULL, (SELECT count(*)::int FROM public.transactions WHERE problem_id = 'F2')
     FROM public.problems WHERE id = 'F2' $$,
  $$ VALUES ('IN_PROGRESS'::text, true, 0) $$,
  'ধাপ ৪০: resolve_dispute REFUND_TO_USER (escrow ছাড়া) ব্যর্থ হলে problem অপরিবর্তিত থাকে (rollback)'
);

-- ======================================================================
-- ঝ. resolve_dispute — SPLIT branch (নিজে commission হিসাব করে)
--
-- ⚠️ resolve_dispute_split (নিচে) থেকে সম্পূর্ণ আলাদা পথ: এখানে কোনো Kotlin-precalculated
-- amount নেই, SQL নিজেই round(total × ratio, 2), resolve_commission_rate() দিয়ে কমিশন, আর
-- সরাসরি users.balance আপডেট করে — release_escrow/refund_escrow_once কল করে না।
-- ======================================================================

-- SP1: SPLIT_50_50, p_solver_percent ডিফল্ট (৫০)। total 400+100=500 → solver gross 250,
-- commission 25 (১০%), net 225; owner refund 250।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND r->>'resolution' = 'SPLIT_SETTLEMENT'
      AND (r->>'solver_net')::numeric = 225 AND (r->>'user_refund')::numeric = 250
   FROM (SELECT public.resolve_dispute('SP1', 'SPLIT_50_50', 'সমান ভাগ') AS r) s),
  'resolve_dispute SPLIT_50_50 (ডিফল্ট ৫০%): OK, resolution=SPLIT_SETTLEMENT, solver_net=225, user_refund=250'
);

-- ধাপ ৪০ ফিক্স: এখন balance_solver/balance_user (role-scoped) dual-write হয় —
-- resolve_dispute_split (নিচে, RDS-A/RDS-B)-এর সাথে এখন সঙ্গতিপূর্ণ।
SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000012' $$,
  $$ VALUES (225::numeric, 225::numeric) $$,
  'ধাপ ৪০: SPLIT branch solver-এর balance আর balance_solver দুটোই ২২৫'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000011' $$,
  $$ VALUES (1250::numeric, 1250::numeric) $$,
  'ধাপ ৪০: SPLIT branch owner-এর balance আর balance_user দুটোই ১০০০→১২৫০'
);

SELECT results_eq(
  $$ SELECT type, gross_amount::numeric, commission_percent::numeric, commission_amount::numeric,
            net_amount::numeric, escrow_id
     FROM public.transactions WHERE id = 'TRX_SPLIT_SOLVER_ESP1' $$,
  $$ VALUES ('DISPUTE_SPLIT'::text, 250::numeric, 10::numeric, 25::numeric, 225::numeric, 'ESP1'::text) $$,
  'resolve_dispute SPLIT: solver transaction (DISPUTE_SPLIT) — gross 250, commission ১০% = 25, net 225'
);

SELECT results_eq(
  $$ SELECT type, gross_amount::numeric, net_amount::numeric, refund_type, escrow_id
     FROM public.transactions WHERE id = 'TRX_SPLIT_USER_ESP1' $$,
  $$ VALUES ('DISPUTE_SPLIT_REFUND'::text, 250::numeric, 250::numeric, 'DISPUTE_SPLIT'::text, 'ESP1'::text) $$,
  'resolve_dispute SPLIT: owner refund transaction (DISPUTE_SPLIT_REFUND) — 250, refund_type=DISPUTE_SPLIT'
);

SELECT is(
  (SELECT role FROM public.transactions WHERE id = 'TRX_SPLIT_SOLVER_ESP1'),
  'SOLVER',
  'ধাপ ৪০: SPLIT solver transaction-এ role=SOLVER সেট হয় (আগে ফাঁকা থাকত)'
);

SELECT is(
  (SELECT role FROM public.transactions WHERE id = 'TRX_SPLIT_USER_ESP1'),
  'USER',
  'ধাপ ৪০: SPLIT owner refund transaction-এ role=USER সেট হয় (আগে ফাঁকা থাকত)'
);

SELECT is(
  ((SELECT net_amount + commission_amount FROM public.transactions WHERE id = 'TRX_SPLIT_SOLVER_ESP1')
   + (SELECT net_amount FROM public.transactions WHERE id = 'TRX_SPLIT_USER_ESP1'))::numeric,
  500::numeric,
  'resolve_dispute SPLIT: টাকা হারায় না — solver net + commission + owner refund = escrow total (500)'
);

SELECT results_eq(
  $$ SELECT status, released_at IS NOT NULL FROM public.escrows WHERE id = 'ESP1' $$,
  $$ VALUES ('RELEASED'::text, true) $$,
  'resolve_dispute SPLIT: escrow RELEASED + released_at সেট (owner refund থাকলেও)'
);

-- ধাপ ৪০ ফিক্স: SPLIT branch এখন job_status='JOB_COMPLETED' সেট করে (RELEASE_TO_SOLVER
-- আর resolve_dispute_split-এর সাথে সঙ্গতিপূর্ণ), আর is_disputed=false।
SELECT results_eq(
  $$ SELECT status, job_status, is_disputed, dispute_resolution_decision, dispute_resolution_type, dispute_resolution_note,
            dispute_split_solver_percent::numeric, dispute_result_seen_by_user, dispute_result_seen_by_solver
     FROM public.problems WHERE id = 'SP1' $$,
  $$ VALUES ('COMPLETED'::text, 'JOB_COMPLETED'::text, false, 'SPLIT_50_50'::text, 'SPLIT_SETTLEMENT'::text, 'সমান ভাগ'::text, 50::numeric, false, false) $$,
  'ধাপ ৪০: resolve_dispute SPLIT: problem COMPLETED/JOB_COMPLETED, is_disputed=false, decision/type/percent ঠিক আছে'
);

SELECT ok(
  (SELECT dispute_resolved_at IS NOT NULL AND dispute_settled_at IS NOT NULL AND completed_at IS NOT NULL
   FROM public.problems WHERE id = 'SP1'),
  'resolve_dispute SPLIT: dispute_resolved_at, dispute_settled_at, completed_at সব সেট'
);

SELECT ok(
  coalesce(test.error_of($$ SELECT public.resolve_dispute('SP1', 'SPLIT_50_50', 'আবার') $$)
           LIKE 'ALREADY_RESOLVED: dispute for SP1 was already resolved at %', false),
  'resolve_dispute SPLIT: দ্বিতীয়বার কল করলে ALREADY_RESOLVED'
);

SELECT results_eq(
  $$ SELECT (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000012'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000011') $$,
  $$ VALUES (225::numeric, 1250::numeric) $$,
  'resolve_dispute SPLIT: ALREADY_RESOLVED-এর পরে দুই পক্ষের balance অপরিবর্তিত (ডাবল-পেআউট/রিফান্ড নেই)'
);

-- SP2: CUSTOM_SPLIT, ৩০%। total 1000 → solver gross 300, commission 30, net 270; owner refund 700।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net')::numeric = 270 AND (r->>'user_refund')::numeric = 700
   FROM (SELECT public.resolve_dispute('SP2', 'CUSTOM_SPLIT', '৩০-৭০', 30) AS r) s),
  'resolve_dispute CUSTOM_SPLIT (৩০%): solver_net=270, user_refund=700'
);

SELECT results_eq(
  $$ SELECT (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000022'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000021') $$,
  $$ VALUES (270::numeric, 1700::numeric) $$,
  'resolve_dispute CUSTOM_SPLIT: solver balance ০→২৭০, owner balance ১০০০→১৭০০'
);

SELECT results_eq(
  $$ SELECT dispute_resolution_decision, dispute_split_solver_percent::numeric FROM public.problems WHERE id = 'SP2' $$,
  $$ VALUES ('CUSTOM_SPLIT'::text, 30::numeric) $$,
  'resolve_dispute CUSTOM_SPLIT: decision=CUSTOM_SPLIT, dispute_split_solver_percent=30'
);

-- SP3: SPLIT_SETTLEMENT, p_solver_percent=150 → টাকার হিসাবে ১০০%-এ clamp। total 200 →
-- solver gross 200, commission 20, net 180; owner refund 0 → owner-এর কোনো transaction/balance পরিবর্তন নেই।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net')::numeric = 180 AND (r->>'user_refund')::numeric = 0
   FROM (SELECT public.resolve_dispute('SP3', 'SPLIT_SETTLEMENT', 'সলভার ১০০%', 150) AS r) s),
  'resolve_dispute SPLIT (p_solver_percent=150 → ১০০%-এ clamp): solver_net=180, user_refund=0'
);

SELECT results_eq(
  $$ SELECT (SELECT count(*)::int FROM public.transactions WHERE id = 'TRX_SPLIT_USER_ESP3'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000031'),
            (SELECT status FROM public.escrows WHERE id = 'ESP3') $$,
  $$ VALUES (0, 1000::numeric, 'RELEASED'::text) $$,
  'resolve_dispute SPLIT (user_refund=0): owner refund transaction নেই, owner balance অপরিবর্তিত (১০০০), escrow তবুও RELEASED'
);

SELECT results_eq(
  $$ SELECT (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000032'),
            (SELECT net_amount::numeric FROM public.transactions WHERE id = 'TRX_SPLIT_SOLVER_ESP3') $$,
  $$ VALUES (180::numeric, 180::numeric) $$,
  'resolve_dispute SPLIT (১০০% solver): solver balance ও transaction net দুটোই ১৮০'
);

-- ধাপ ৪০ ফিক্স: dispute_split_solver_percent এখন clamped মান রাখে (১৫০ → ১০০) —
-- resolve_dispute_split (নিচে RDS-B)-এর সাথে এখন সঙ্গতিপূর্ণ।
SELECT results_eq(
  $$ SELECT dispute_resolution_decision, dispute_split_solver_percent::numeric FROM public.problems WHERE id = 'SP3' $$,
  $$ VALUES ('SPLIT_SETTLEMENT'::text, 100::numeric) $$,
  'ধাপ ৪০: SPLIT branch dispute_split_solver_percent-এ clamped মান (১০০) থাকে'
);

-- SP4: SETTLE, p_solver_percent=-20 → ০%-এ clamp। total 300 → solver gross 0, net 0 → solver-এর
-- কোনো transaction/balance পরিবর্তন নেই; owner refund 300 (শুধু legacy balance)।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net')::numeric = 0 AND (r->>'user_refund')::numeric = 300
   FROM (SELECT public.resolve_dispute('SP4', 'SETTLE', 'গ্রাহক সম্পূর্ণ', -20) AS r) s),
  'resolve_dispute SETTLE (p_solver_percent=-20 → ০%-এ clamp): solver_net=0, user_refund=300'
);

SELECT results_eq(
  $$ SELECT (SELECT count(*)::int FROM public.transactions WHERE id = 'TRX_SPLIT_SOLVER_ESP4'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000042'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000041'),
            (SELECT count(*)::int FROM public.transactions WHERE id = 'TRX_SPLIT_USER_ESP4'),
            (SELECT status FROM public.escrows WHERE id = 'ESP4') $$,
  $$ VALUES (0, 0::numeric, 1300::numeric, 1, 'RELEASED'::text) $$,
  'resolve_dispute SETTLE (০% solver): solver transaction নেই/balance ০, owner balance ১০০০→১৩০০ + refund transaction ১টা, escrow RELEASED'
);

SELECT results_eq(
  $$ SELECT dispute_resolution_decision, dispute_split_solver_percent::numeric FROM public.problems WHERE id = 'SP4' $$,
  $$ VALUES ('SETTLE'::text, 0::numeric) $$,
  'ধাপ ৪০: SETTLE-এও dispute_split_solver_percent clamped (০) থাকে'
);

-- SPLIT guard: escrow আছে কিনা
SELECT throws_ok(
  $$ SELECT public.resolve_dispute('SPN1', 'SPLIT_50_50', 'x') $$,
  'P0001',
  'NO_ESCROW_TO_SPLIT',
  'resolve_dispute SPLIT: কোনো escrow না থাকলে NO_ESCROW_TO_SPLIT'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute('SPN2', 'SETTLE', 'x') $$,
  'P0001',
  'NO_ESCROW_TO_SPLIT',
  'resolve_dispute SPLIT: শুধু RELEASED (HELD না) escrow থাকলেও NO_ESCROW_TO_SPLIT — আগে-শেষ-হওয়া escrow-এ আবার payout হয় না'
);

-- ধাপ ৪০ ফিক্স: SPLIT branch-এ solver পেআউটের আগে এখন has_solver_role চেক হয়
-- (SPZ: solver2, has_solver_role=false — release_escrow/resolve_dispute_split-এর প্যাটার্ন)।
SELECT throws_ok(
  $$ SELECT public.resolve_dispute('SPZ', 'SPLIT_50_50', 'x') $$,
  'P0001',
  'SOLVER_ROLE_INACTIVE',
  'ধাপ ৪০: resolve_dispute SPLIT-এ solver-এর has_solver_role=false হলে SOLVER_ROLE_INACTIVE'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'ESPZ'),
            (SELECT dispute_resolved_at IS NULL FROM public.problems WHERE id = 'SPZ') $$,
  $$ VALUES ('HELD'::text, true) $$,
  'ধাপ ৪০: SPLIT SOLVER_ROLE_INACTIVE-এ পুরো কল rollback — escrow HELD, problem unresolved, owner refund-ও হয়নি'
);

-- ======================================================================
-- ঞ. resolve_dispute_split — Kotlin-precalculated amount, RPC শুধু bounds re-verify করে
-- ======================================================================

-- RDS-A (বাস্তব flow): Kotlin আগে refund_escrow_once() দিয়ে owner-এর ৪০% (240) ফেরত দেয়
-- (escrow → REFUNDED), তারপর resolve_dispute_split() দিয়ে solver-এর ৬০% (gross 360,
-- commission 36, net 324) পরিশোধ করে। total 600।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'amount')::numeric = 240
   FROM (SELECT public.refund_escrow_once('ERA', 'DISPUTE_SPLIT', 40) AS r) s),
  'RDS-A পূর্ব-ধাপ (Kotlin flow): refund_escrow_once ৪০% → OK, amount=240'
);

SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net_amount')::numeric = 324
      AND (r->>'user_refund_amount')::numeric = 240 AND (r->>'escrow_closed_here')::boolean = false
   FROM (SELECT public.resolve_dispute_split('RSA', 'ERA', 60, 360, 36, 324, 240, 'CUSTOM_SPLIT', 'ষাট-চল্লিশ', 70) AS r) s),
  'resolve_dispute_split: OK, solver_net_amount=324, user_refund_amount=240, escrow_closed_here=false'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_solver::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000052' $$,
  $$ VALUES (324::numeric, 324::numeric) $$,
  'resolve_dispute_split: solver-এর balance আর balance_solver দুটোই +324 (dual-write — resolve_dispute-এর SPLIT branch-এর উল্টো)'
);

SELECT results_eq(
  $$ SELECT balance::numeric, balance_user::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000051' $$,
  $$ VALUES (1240::numeric, 1240::numeric) $$,
  'resolve_dispute_split: owner শুধু refund_escrow_once-এর ২৪০ পেয়েছে (১০০০→১২৪০) — RPC নিজে আবার রিফান্ড করে না (ডাবল-রিফান্ড নেই)'
);

SELECT results_eq(
  $$ SELECT type, release_type, gross_amount::numeric, commission_percent::numeric, commission_amount::numeric,
            net_amount::numeric, escrow_id
     FROM public.transactions WHERE id = 'TRX_SPLIT_RSA' $$,
  $$ VALUES ('PAYMENT'::text, 'SPLIT_RELEASE'::text, 360::numeric, 10::numeric, 36::numeric, 324::numeric, 'ERA'::text) $$,
  'resolve_dispute_split: TRX_SPLIT_<problem_id> PAYMENT/SPLIT_RELEASE — gross 360, commission_percent = 36/360 = ১০, net 324'
);

SELECT is(
  (SELECT status FROM public.escrows WHERE id = 'ERA'),
  'REFUNDED',
  'resolve_dispute_split: user_refund>0 হলে escrow ছোঁয়া হয় না — refund_escrow_once-এর REFUNDED-ই থাকে'
);

SELECT results_eq(
  $$ SELECT status, job_status, is_disputed, dispute_resolution_decision, dispute_resolution_type, dispute_resolution_note,
            dispute_split_solver_percent::numeric, dispute_progress_at_settlement,
            dispute_result_seen_by_user, dispute_result_seen_by_solver
     FROM public.problems WHERE id = 'RSA' $$,
  $$ VALUES ('COMPLETED'::text, 'JOB_COMPLETED'::text, true, 'CUSTOM_SPLIT'::text, 'SPLIT_SETTLEMENT'::text, 'ষাট-চল্লিশ'::text,
             60::numeric, 70, false, false) $$,
  'resolve_dispute_split: problem COMPLETED/JOB_COMPLETED, decision=p_resolution_decision, type=SPLIT_SETTLEMENT, percent=60, progress=70, result_seen=false'
);

SELECT ok(
  (SELECT dispute_resolved_at IS NOT NULL AND dispute_settled_at IS NOT NULL AND completed_at IS NOT NULL
   FROM public.problems WHERE id = 'RSA'),
  'resolve_dispute_split: dispute_resolved_at, dispute_settled_at, completed_at সব সেট'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications
   WHERE related_problem_id = 'RSA' AND user_id = 'aaaaaaaa-0000-0000-0000-000000000051' AND message LIKE '%৳240 আপনার ওয়ালেটে%'),
  1,
  'resolve_dispute_split: owner-কে ১টা "৳240 আপনার ওয়ালেটে ফেরত" notification যায়'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications
   WHERE related_problem_id = 'RSA' AND user_id = 'aaaaaaaa-0000-0000-0000-000000000052' AND message LIKE '%৳324 আপনার ব্যালেন্সে%'),
  1,
  'resolve_dispute_split: solver-কে ১টা "৳324 আপনার ব্যালেন্সে জমা" notification যায়'
);

SELECT ok(
  (SELECT r->>'result' = 'ALREADY_RESOLVED' AND r->>'resolved_at' IS NOT NULL
   FROM (SELECT public.resolve_dispute_split('RSA', 'ERA', 60, 360, 36, 324, 240, 'CUSTOM_SPLIT', 'ষাট-চল্লিশ', 70) AS r) s),
  'resolve_dispute_split: দ্বিতীয়বার কল করলে exception না ছুঁড়ে ALREADY_RESOLVED (+resolved_at) রিটার্ন করে — idempotent no-op'
);

SELECT results_eq(
  $$ SELECT (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000052'),
            (SELECT count(*)::int FROM public.transactions WHERE problem_id = 'RSA' AND id LIKE 'TRX_SPLIT_%') $$,
  $$ VALUES (324::numeric, 1) $$,
  'resolve_dispute_split: ALREADY_RESOLVED-এর পরে solver balance ৩২৪-ই, TRX_SPLIT transaction ১টাই (ডাবল-পেআউট নেই)'
);

-- RDS-B: solver ১০০% (p_split_solver_percent=150 → clamp ১০০), user_refund=0 → escrow এই RPC-ই বন্ধ করে।
-- total 200 → gross 200, commission 20, net 180।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net_amount')::numeric = 180
      AND (r->>'user_refund_amount')::numeric = 0 AND (r->>'escrow_closed_here')::boolean = true
   FROM (SELECT public.resolve_dispute_split('RSB', 'ERB', 150, 200, 20, 180, 0, 'SPLIT_SETTLEMENT', 'সলভার সম্পূর্ণ') AS r) s),
  'resolve_dispute_split (solver ১০০%): OK, solver_net_amount=180, user_refund_amount=0, escrow_closed_here=true'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'ERB'),
            (SELECT released_at IS NOT NULL FROM public.escrows WHERE id = 'ERB'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000062'),
            (SELECT balance_solver::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000062'),
            (SELECT dispute_split_solver_percent::numeric FROM public.problems WHERE id = 'RSB') $$,
  $$ VALUES ('RELEASED'::text, true, 180::numeric, 180::numeric, 100::numeric) $$,
  'resolve_dispute_split (user_refund=0): escrow এই RPC-ই RELEASED করে, solver balance/balance_solver +180, percent clamped=100 (resolve_dispute-এর কাঁচা মানের উল্টো)'
);

-- RDS-C: ৳২ tolerance-এর ঠিক প্রান্তবিন্দু। total 600, ৫০% → প্রত্যাশিত gross 300; Kotlin পাঠাল 302
-- (|302-300| = 2, `> 2` না, তাই গৃহীত হয়)। commission_percent = round(30/302×100, 2) = 9.93।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net_amount')::numeric = 272
      AND (r->>'user_refund_amount')::numeric = 300 AND (r->>'escrow_closed_here')::boolean = false
   FROM (SELECT public.resolve_dispute_split('RSC', 'ERC', 50, 302, 30, 272, 300, 'SPLIT_SETTLEMENT', 'সীমার প্রান্তে') AS r) s),
  'resolve_dispute_split: প্রত্যাশিত gross থেকে ঠিক ৳২ দূরে (302 vs 300) গৃহীত হয় — tolerance-এর প্রান্ত'
);

SELECT results_eq(
  $$ SELECT (SELECT commission_percent::numeric FROM public.transactions WHERE id = 'TRX_SPLIT_RSC'),
            (SELECT status FROM public.escrows WHERE id = 'ERC') $$,
  $$ VALUES (9.93::numeric, 'HELD'::text) $$,
  'resolve_dispute_split: commission_percent = round(30/302×100,2) = 9.93; user_refund>0 তাই escrow HELD-ই থাকে (RPC ছোঁয় না)'
);

-- RDS-E: solver ০% (p_split_solver_percent=0) — সব টাকা owner-এর (refund_escrow_once আলাদাভাবে করবে);
-- solver_net=0 তাই solver-এর কোনো credit/transaction নেই, তবু problem resolved হয়।
SELECT ok(
  (SELECT r->>'result' = 'OK' AND (r->>'solver_net_amount')::numeric = 0
      AND (r->>'user_refund_amount')::numeric = 300 AND (r->>'escrow_closed_here')::boolean = false
   FROM (SELECT public.resolve_dispute_split('RSE', 'ERE', 0, 0, 0, 0, 300, 'SPLIT_SETTLEMENT', 'গ্রাহক সম্পূর্ণ') AS r) s),
  'resolve_dispute_split (solver ০%): OK, solver_net_amount=0, user_refund_amount=300, escrow_closed_here=false'
);

SELECT results_eq(
  $$ SELECT (SELECT count(*)::int FROM public.transactions WHERE id = 'TRX_SPLIT_RSE'),
            (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000082'),
            (SELECT status FROM public.escrows WHERE id = 'ERE'),
            (SELECT status FROM public.problems WHERE id = 'RSE') $$,
  $$ VALUES (0, 0::numeric, 'HELD'::text, 'COMPLETED'::text) $$,
  'resolve_dispute_split (solver ০%): solver-এর transaction/balance পরিবর্তন নেই, escrow HELD-ই, তবু problem COMPLETED'
);

-- ALREADY_PAID: problem এখনো unresolved কিন্তু TRX_SPLIT_<problem_id> আগে থেকেই আছে (defense-in-depth)।
SELECT is(
  (SELECT r->>'result' FROM (SELECT public.resolve_dispute_split('RSP', 'ERP', 50, 100, 10, 90, 100, 'SPLIT_SETTLEMENT', 'x') AS r) s),
  'ALREADY_PAID',
  'resolve_dispute_split: TRX_SPLIT_<problem_id> আগে থেকে থাকলে (problem unresolved হলেও) ALREADY_PAID'
);

SELECT results_eq(
  $$ SELECT (SELECT balance::numeric FROM public.users WHERE id = 'aaaaaaaa-0000-0000-0000-000000000092'),
            (SELECT dispute_resolved_at IS NULL FROM public.problems WHERE id = 'RSP') $$,
  $$ VALUES (0::numeric, true) $$,
  'resolve_dispute_split ALREADY_PAID: solver-কে আবার টাকা দেওয়া হয় না, problem-ও আপডেট হয় না'
);

-- ---- resolve_dispute_split error branches (admin) --------------------------------
-- RSX/EX: total 500+100 = 600, HELD, solver1 সক্রিয়। প্রতিটা কলে আগের সব check পাস করিয়ে
-- শুধু টার্গেট branch-এ পৌঁছানো হয়েছে (check-এর ক্রম migration-এর ক্রম অনুযায়ী)।
SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('NO_SUCH_PROBLEM', 'EX', 50, 300, 30, 270, 300, 'x', 'x') $$,
  'P0001',
  'PROBLEM_NOT_FOUND',
  'resolve_dispute_split: PROBLEM_NOT_FOUND'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('RSX', 'NO_SUCH_ESCROW', 50, 300, 30, 270, 300, 'x', 'x') $$,
  'P0001',
  'ESCROW_NOT_FOUND',
  'resolve_dispute_split: ESCROW_NOT_FOUND'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('RSY', 'EX', 50, 300, 30, 270, 300, 'x', 'x') $$,
  'P0001',
  'ESCROW_PROBLEM_MISMATCH',
  'resolve_dispute_split: escrow অন্য problem-এর (EX ↔ RSY) হলে ESCROW_PROBLEM_MISMATCH'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('RSX', 'EX', 50, -1, 0, 0, 0, 'x', 'x') $$,
  'P0001',
  'INVALID_AMOUNT',
  'resolve_dispute_split: কোনো amount ঋণাত্মক (gross=-1) হলে INVALID_AMOUNT'
);

SELECT ok(
  coalesce(test.error_of($$ SELECT public.resolve_dispute_split('RSX', 'EX', 50, 602, 0, 0, 0, 'x', 'x') $$)
           LIKE 'AMOUNT_EXCEEDS_ESCROW:%', false),
  'resolve_dispute_split: gross 602 > escrow total 600 + ১ হলে AMOUNT_EXCEEDS_ESCROW (prefix-match)'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('RSX', 'EX', 50, 300, 0, 302, 300, 'x', 'x') $$,
  'P0001',
  'NET_EXCEEDS_GROSS',
  'resolve_dispute_split: net 302 > gross 300 + ১ হলে NET_EXCEEDS_GROSS'
);

SELECT ok(
  coalesce(test.error_of($$ SELECT public.resolve_dispute_split('RSX', 'EX', 50, 303, 30, 273, 297, 'x', 'x') $$)
           LIKE 'PERCENT_MISMATCH:%', false),
  'resolve_dispute_split: ৫০%-এ প্রত্যাশিত gross 300, পাঠানো 303 (৳৩ > ৳২ tolerance) হলে PERCENT_MISMATCH (prefix-match)'
);

SELECT throws_ok(
  $$ SELECT public.resolve_dispute_split('RSZ', 'EZ', 50, 150, 15, 135, 150, 'x', 'x') $$,
  'P0001',
  'SOLVER_ROLE_INACTIVE',
  'resolve_dispute_split: solver-এর has_solver_role=false (net>0) হলে SOLVER_ROLE_INACTIVE'
);

SELECT results_eq(
  $$ SELECT (SELECT status FROM public.escrows WHERE id = 'EX'),
            (SELECT dispute_resolved_at IS NULL FROM public.problems WHERE id = 'RSX'),
            (SELECT dispute_resolved_at IS NULL FROM public.problems WHERE id = 'RSZ'),
            (SELECT count(*)::int FROM public.transactions WHERE problem_id IN ('RSX', 'RSY', 'RSZ')) $$,
  $$ VALUES ('HELD'::text, true, true, 0) $$,
  'resolve_dispute_split: সব error branch-এ কোনো আংশিক পরিবর্তন থাকে না (escrow HELD, problem unresolved, কোনো transaction নেই)'
);

SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
