-- 02_bidding_flow_part2.sql — Step 1 (Bidding flow), PART 2 of 2
--
-- এই ফাইলে কভার করা হয়েছে: accept_direct_contract, decline_direct_contract,
-- user_delete_problem — এই ৩টা ফাংশন দিয়ে Step 1 সম্পূর্ণ হয় (বাকি অংশ
-- 01_bidding_flow.sql-এ, PART 1: accept_bid/cancel_bid/reject_bid/
-- solver_has_ended_bid)। ফাংশন body verify করা হয়েছে
-- supabase/migrations/recovered_bidding_contracts.sql (accept_direct_contract,
-- decline_direct_contract) এবং
-- supabase/migrations/step32_85_user_delete_problem.sql (user_delete_problem)
-- থেকে — অনুমান করে লেখা হয়নি।
--
-- ⚠️ এই টেস্টগুলো 01_bidding_flow_schema_stub.sql-এর inferred/TEMPORARY
-- schema-র উপর নির্ভরশীল (বিশেষভাবে problems.is_direct_contract,
-- direct_contract_status, is_user_deleted, accepted_bid_id,
-- accepted_solver_id — এই সবকটা কলাম PART 1 সেশনেই stub-এ যোগ করা হয়েছিল,
-- তাই এখানে নতুন কোনো ALTER লাগেনি) — আসল live schema-র সাথে mismatch
-- থাকলে ফলাফল ভুল হতে পারে।

BEGIN;
SELECT plan(22);

SELECT test.seed_users();

-- test user ids (00_helpers.sql / test.seed_users থেকে)
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...

----------------------------------------------------------------------
-- accept_direct_contract — happy path
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name, accepted_amount
) VALUES (
  'P6', '11111111-1111-1111-1111-111111111111', 'Direct Contract Problem 1', 'OPEN', true,
  true, 'PENDING_ACCEPTANCE',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One', 700
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.accept_direct_contract('P6'))->>'result',
  'OK',
  'accept_direct_contract: assigned solver accept করলে result OK'
);

SELECT is(
  (SELECT status FROM public.problems WHERE id = 'P6'),
  'IN_PROGRESS',
  'accept_direct_contract: problem status IN_PROGRESS হয়ে যায়'
);

SELECT is(
  (SELECT direct_contract_status FROM public.problems WHERE id = 'P6'),
  'ACCEPTED',
  'accept_direct_contract: direct_contract_status ACCEPTED হয়ে যায়'
);

SELECT ok(
  (SELECT applied_commission_rate FROM public.problems WHERE id = 'P6') IS NOT NULL,
  'accept_direct_contract: applied_commission_rate সেট হয় (resolve_commission_rate থেকে)'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.escrows
    WHERE problem_id = 'P6' AND status = 'HELD' AND base_amount = 700
      AND solver_id = '22222222-2222-2222-2222-222222222222'
  ),
  'accept_direct_contract: escrow HELD status-এ accepted_amount (700) দিয়ে তৈরি হয়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE related_problem_id = 'P6' AND user_id = '11111111-1111-1111-1111-111111111111'
  ),
  'accept_direct_contract: problem owner-কে notification পাঠানো হয়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- accept_direct_contract — NOT_A_DIRECT_CONTRACT (is_direct_contract=false)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name
) VALUES (
  'P7', '11111111-1111-1111-1111-111111111111', 'Not A Direct Contract', 'OPEN', true,
  false, NULL,
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.accept_direct_contract('P7') $$,
  'P0001',
  'NOT_A_DIRECT_CONTRACT',
  'accept_direct_contract: is_direct_contract=false হলে NOT_A_DIRECT_CONTRACT'
);
SELECT test.logout();

----------------------------------------------------------------------
-- accept_direct_contract — ALREADY_PROCESSED (direct_contract_status আগেই resolved)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name
) VALUES (
  'P8', '11111111-1111-1111-1111-111111111111', 'Already Processed Direct Contract', 'IN_PROGRESS', true,
  true, 'ACCEPTED',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.accept_direct_contract('P8'))->>'result',
  'ALREADY_PROCESSED',
  'accept_direct_contract: direct_contract_status আগে থেকেই PENDING_ACCEPTANCE না হলে ALREADY_PROCESSED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- accept_direct_contract — NOT_AUTHORIZED (caller accepted_solver_id না)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name
) VALUES (
  'P9', '11111111-1111-1111-1111-111111111111', 'Direct Contract Wrong Caller', 'OPEN', true,
  true, 'PENDING_ACCEPTANCE',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.accept_direct_contract('P9') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'accept_direct_contract: assigned solver ছাড়া অন্য কেউ accept করতে চাইলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- decline_direct_contract — happy path (reason সহ)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name
) VALUES (
  'P10', '11111111-1111-1111-1111-111111111111', 'Direct Contract Decline 1', 'OPEN', true,
  true, 'PENDING_ACCEPTANCE',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.decline_direct_contract('P10', 'test reason abc'))->>'result',
  'OK',
  'decline_direct_contract: assigned solver decline করলে result OK'
);

SELECT is(
  (SELECT status FROM public.problems WHERE id = 'P10'),
  'CANCELLED',
  'decline_direct_contract: problem status CANCELLED হয়ে যায়'
);

SELECT is(
  (SELECT direct_contract_status FROM public.problems WHERE id = 'P10'),
  'DECLINED',
  'decline_direct_contract: direct_contract_status DECLINED হয়ে যায়'
);

SELECT ok(
  EXISTS (
    SELECT 1 FROM public.notifications
    WHERE related_problem_id = 'P10' AND user_id = '11111111-1111-1111-1111-111111111111'
      AND message LIKE '%test reason abc%'
  ),
  'decline_direct_contract: notification message-এ দেওয়া reason অন্তর্ভুক্ত থাকে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- decline_direct_contract — NOT_AUTHORIZED
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name
) VALUES (
  'P11', '11111111-1111-1111-1111-111111111111', 'Direct Contract Decline Wrong Caller', 'OPEN', true,
  true, 'PENDING_ACCEPTANCE',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.decline_direct_contract('P11') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'decline_direct_contract: assigned solver ছাড়া অন্য কেউ decline করতে চাইলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- decline_direct_contract — ALREADY_PROCESSED
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, is_direct_contract, direct_contract_status,
  accepted_solver_id, accepted_solver_name
) VALUES (
  'P12', '11111111-1111-1111-1111-111111111111', 'Direct Contract Already Declined', 'CANCELLED', true,
  true, 'DECLINED',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One'
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.decline_direct_contract('P12'))->>'result',
  'ALREADY_PROCESSED',
  'decline_direct_contract: direct_contract_status আগে থেকেই PENDING_ACCEPTANCE না হলে ALREADY_PROCESSED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- user_delete_problem — happy path (owner নিজের OPEN, no-accepted-bid problem delete করে)
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public)
VALUES ('P13', '11111111-1111-1111-1111-111111111111', 'Deletable Problem', 'OPEN', true);

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status) VALUES
  ('B13_PENDING', 'P13', '22222222-2222-2222-2222-222222222222', 'Test Solver One', 200, 'PENDING'),
  ('B13_CANCELLED', 'P13', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 250, 'CANCELLED');

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.user_delete_problem('P13'))->>'result',
  'OK',
  'user_delete_problem: owner নিজের OPEN/no-accepted-bid problem delete করলে OK'
);

SELECT ok(
  (SELECT is_user_deleted FROM public.problems WHERE id = 'P13'),
  'user_delete_problem: problem.is_user_deleted true হয়ে যায়'
);

SELECT is(
  (SELECT status FROM public.problems WHERE id = 'P13'),
  'CANCELLED',
  'user_delete_problem: problem.status CANCELLED হয়ে যায়'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B13_PENDING'),
  'CANCELLED',
  'user_delete_problem: PENDING bid CANCELLED হয়ে যায়'
);

SELECT is(
  (SELECT status FROM public.bids WHERE id = 'B13_CANCELLED'),
  'CANCELLED',
  'user_delete_problem: আগে থেকেই CANCELLED bid অপরিবর্তিত (এখনও CANCELLED) থাকে'
);

SELECT test.logout();

----------------------------------------------------------------------
-- user_delete_problem — NOT_AUTHORIZED (owner ছাড়া অন্য কেউ delete করতে চাইলে)
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public)
VALUES ('P14', '11111111-1111-1111-1111-111111111111', 'Not My Problem', 'OPEN', true);

SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.user_delete_problem('P14') $$,
  'P0001',
  'NOT_AUTHORIZED',
  'user_delete_problem: owner ছাড়া অন্য কেউ delete করতে চাইলে NOT_AUTHORIZED'
);
SELECT test.logout();

----------------------------------------------------------------------
-- user_delete_problem — NOT_DELETABLE (bid ইতিমধ্যে accept হয়ে গেছে)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, is_public, accepted_bid_id, accepted_solver_id
) VALUES (
  'P15', '11111111-1111-1111-1111-111111111111', 'Already Has Accepted Bid', 'IN_PROGRESS', true,
  'SOME_BID_ID', '22222222-2222-2222-2222-222222222222'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.user_delete_problem('P15') $$,
  'P0001',
  'NOT_DELETABLE',
  'user_delete_problem: accepted_bid_id/accepted_solver_id থাকলে (OPEN না বা bid accept হয়ে গেলে) NOT_DELETABLE'
);
SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
