-- 01_bidding_flow.sql — Step 1 (Bidding flow), PART 1 of 2
--
-- এই ফাইলে কভার করা হয়েছে: accept_bid, cancel_bid, reject_bid,
-- solver_has_ended_bid।
--
-- বাকি অংশ (accept_direct_contract, decline_direct_contract,
-- user_delete_problem) এই ফাইলে ইচ্ছাকৃতভাবে রাখা হয়নি — সেগুলো
-- supabase/tests/02_bidding_flow_part2.sql-এ কভার করা হয়েছে (এই ফাইল
-- ভাঙা হয়নি)।
--
-- ⚠️ এই টেস্টগুলো 01_bidding_flow_schema_stub.sql-এর inferred/TEMPORARY
-- schema-র উপর নির্ভরশীল — আসল live schema-র সাথে mismatch থাকলে ফলাফল ভুল
-- হতে পারে (বিস্তারিত সেই ফাইলের হেডার কমেন্টে)।

BEGIN;
SELECT plan(23);

SELECT test.seed_users();

-- test user ids (00_helpers.sql / test.seed_users থেকে)
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...

----------------------------------------------------------------------
-- accept_bid — happy path (sufficient wallet balance, no gateway needed)
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public)
VALUES ('P1', '11111111-1111-1111-1111-111111111111', 'Test Problem 1', 'OPEN', false, true);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B1', 'P1', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 500, 'PENDING');

-- অন্য একটা competing bid, verify করার জন্য accept_bid অন্য bids-কে PENDING-এ
-- রেখে দেয় (শুধু accepted বাদে বাকিদের CANCELLED করে না — মূল ফাংশনে
-- explicit দেখা যায় শুধু status='PENDING' রেখে দেয়, CANCELLED না থাকলে)
INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B1_OTHER', 'P1', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 600, 'PENDING');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.accept_bid('P1', 'B1'))->>'result',
  'OK',
  'accept_bid: যথেষ্ট balance_user থাকলে result OK'
);

SELECT is(
  (SELECT status FROM public.problems WHERE id = 'P1'),
  'IN_PROGRESS',
  'accept_bid: problem status IN_PROGRESS হয়ে যায়'
);

SELECT is(
  (SELECT accepted_bid_id FROM public.problems WHERE id = 'P1'),
  'B1',
  'accept_bid: accepted_bid_id সঠিক bid-এ সেট হয়'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B1'),
  'ACCEPTED',
  'accept_bid: accepted bid-এর status ACCEPTED হয়'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B1_OTHER'),
  'PENDING',
  'accept_bid: bid_id ছাড়া অন্য bid PENDING-ই থেকে যায় (CANCELLED হয় না)'
);

SELECT is(
  (SELECT balance_user FROM public.users WHERE id = '11111111-1111-1111-1111-111111111111'),
  500::numeric,
  'accept_bid: balance_user থেকে bid amount ঠিকমতো deduct হয় (1000 - 500 = 500)'
);

SELECT ok(
  EXISTS (SELECT 1 FROM public.escrows WHERE problem_id = 'P1' AND status = 'HELD' AND base_amount = 500),
  'accept_bid: escrow HELD status-এ base_amount=500 দিয়ে তৈরি হয়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.transactions
    WHERE problem_id = 'P1' AND type = 'BID_ACCEPT_DEDUCTION' AND role = 'USER' AND net_amount = -500
  ),
  'accept_bid: BID_ACCEPT_DEDUCTION transaction (role=USER, net_amount=-500) লেখা হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- accept_bid — insufficient balance, no gateway info supplied
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_instant_job, is_public)
VALUES ('P2', '11111111-1111-1111-1111-111111111111', 'Test Problem 2', 'OPEN', false, true);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B2', 'P2', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 800, 'PENDING');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
-- client-এর balance_user এখন 500 (আগের টেস্টের পর), bid amount 800 → shortfall 300

SELECT is(
  (public.accept_bid('P2', 'B2'))->>'result',
  'INSUFFICIENT_BALANCE',
  'accept_bid: gateway তথ্য ছাড়া shortfall থাকলে INSUFFICIENT_BALANCE'
);

SELECT is(
  ((public.accept_bid('P2', 'B2'))->>'shortfall')::numeric,
  300::numeric,
  'accept_bid: shortfall সঠিকভাবে হিসাব হয় (800 - 500 = 300)'
);

SELECT is(
  (SELECT status FROM public.problems WHERE id = 'P2'),
  'OPEN',
  'accept_bid: INSUFFICIENT_BALANCE হলে problem status অপরিবর্তিত (OPEN) থাকে'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B2'),
  'PENDING',
  'accept_bid: INSUFFICIENT_BALANCE হলে bid status অপরিবর্তিত (PENDING) থাকে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- accept_bid — not authorized (caller problem owner না, admin ও না)
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.accept_bid('P2', 'B2') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'accept_bid: owner/admin ছাড়া অন্য কেউ কল করলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- cancel_bid — happy path (নিজের PENDING bid নিজে cancel করা)
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public)
VALUES ('P3', '11111111-1111-1111-1111-111111111111', 'Test Problem 3', 'OPEN', true);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B3', 'P3', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 300, 'PENDING');

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.cancel_bid('B3'))->>'result',
  'OK',
  'cancel_bid: নিজের PENDING bid নিজে cancel করলে OK'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B3'),
  'CANCELLED',
  'cancel_bid: bid status CANCELLED হয়ে যায়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- cancel_bid — not authorized (অন্য solver-এর bid cancel করার চেষ্টা)
----------------------------------------------------------------------
INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B4', 'P3', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 350, 'PENDING');

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.cancel_bid('B4') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'cancel_bid: bid owner (solver) ছাড়া অন্য কেউ cancel করতে গেলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- cancel_bid — invalid transition (আগে থেকেই CANCELLED bid আবার cancel)
----------------------------------------------------------------------
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.cancel_bid('B3'))->>'result',
  'INVALID_TRANSITION',
  'cancel_bid: আগে থেকেই non-PENDING bid cancel করতে গেলে INVALID_TRANSITION'
);
SELECT test.logout();

----------------------------------------------------------------------
-- reject_bid — happy path (problem owner নিজের problem-এর PENDING bid reject করে)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.reject_bid('B4'))->>'result',
  'OK',
  'reject_bid: problem owner PENDING bid reject করলে OK'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B4'),
  'REJECTED',
  'reject_bid: bid status REJECTED হয়ে যায়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- reject_bid — not authorized (owner/admin না এমন কেউ reject করার চেষ্টা)
----------------------------------------------------------------------
INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('B5', 'P3', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 400, 'PENDING');

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.reject_bid('B5') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'reject_bid: problem owner/admin ছাড়া অন্য কেউ reject করতে গেলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- solver_has_ended_bid — RLS helper predicate (fix_problems_select_bids_rls_recursion.sql)
----------------------------------------------------------------------
-- B4 (উপরে REJECTED করা হয়েছে) solver 22222222...-এর জন্য "ended" হওয়া উচিত
SELECT ok(
  public.solver_has_ended_bid('P3', '22222222-2222-2222-2222-222222222222'),
  'solver_has_ended_bid: REJECTED bid থাকলে true রিটার্ন করে'
);

-- B5 এখনো PENDING, solver 33333333...-এর জন্য "ended" হওয়া উচিত না
SELECT ok(
  NOT public.solver_has_ended_bid('P3', '33333333-3333-3333-3333-333333333333'),
  'solver_has_ended_bid: PENDING bid হলে false রিটার্ন করে'
);

-- সম্পূর্ণ unrelated solver-problem জোড়ার জন্যও false
SELECT ok(
  NOT public.solver_has_ended_bid('P3', '99999999-9999-9999-9999-999999999999'),
  'solver_has_ended_bid: কোনো bid-ই না থাকলে false রিটার্ন করে'
);

SELECT * FROM finish();
ROLLBACK;
