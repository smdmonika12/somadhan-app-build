-- 08_disputes_part1.sql — Step 5 (Disputes), PART 1 of 2
--
-- এই ফাইলে কভার করা হয়েছে ৩টা ফাংশন: raise_dispute, settle_dispute,
-- admin_manually_flag_dispute — সবগুলোই recovered_disputes.sql /
-- recovered_admin_moderation.sql থেকে real body verify করে লেখা, অনুমান
-- করে না।
--
-- বাকি ২টা (resolve_dispute, resolve_dispute_split) ইচ্ছাকৃতভাবে এই
-- সেশনে করা হয়নি (money-flow-heavy, বেশি জটিল) — পরের সেশনের কাজ, দেখুন
-- CI_TEST_SUITE_PROGRESS.md-এর "পরবর্তী ধাপ" সেকশন।
--
-- ⚠️ এই টেস্টগুলো 01_bidding_flow_schema_stub.sql + 05_job_release_escrow_schema_stub.sql
-- + 08_disputes_schema_stub.sql (নতুন, এই সেশনে যোগ) — এই তিনটা stub মিলিয়ে
-- inferred schema-র উপর নির্ভরশীল।
--
-- ✅ এই সেশনেই সত্যিই local Postgres 16 + pgTAP দিয়ে (network এবার পাওয়া
-- গেছে, ৫ সেশন পর প্রথমবার) পুরো চেইন (01, 05, 07, 08 schema stubs →
-- 00_helpers → 01…07_part2 → এই ফাইল) fresh throwaway DB-তে চালিয়ে
-- verify করা হয়েছে — শুধু static check না।

BEGIN;
SELECT plan(25);

SELECT test.seed_users();
-- CLIENT: 11111111-...  SOLVER 1: 22222222-...  SOLVER 2: 33333333-...  ADMIN: 99999999-...

----------------------------------------------------------------------
-- raise_dispute — happy path, caller = owner (USER)
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, accepted_solver_id, accepted_solver_name,
  is_public, is_disputed
) VALUES (
  'D1', '11111111-1111-1111-1111-111111111111', 'Dispute Test Problem 1', 'IN_PROGRESS',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, false
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.raise_dispute('D1', '  কাজ সম্পূর্ণ হয়নি  '))->>'result',
  'OK',
  'raise_dispute: owner হিসেবে কল করলে result OK'
);

SELECT results_eq(
  $$ SELECT is_disputed, dispute_reason, dispute_initiator_id, dispute_initiator_role
     FROM public.problems WHERE id = 'D1' $$,
  $$ VALUES (true, 'কাজ সম্পূর্ণ হয়নি'::text, '11111111-1111-1111-1111-111111111111'::uuid, 'USER'::text) $$,
  'raise_dispute: is_disputed/dispute_reason(trim)/dispute_initiator_id/role owner-এর জন্য সঠিক সেট হয়'
);

SELECT isnt(
  (SELECT disputed_at FROM public.problems WHERE id = 'D1'),
  NULL,
  'raise_dispute: disputed_at সেট হয়'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE user_id = '22222222-2222-2222-2222-222222222222' AND related_problem_id = 'D1'),
  1,
  'raise_dispute: solver-কে ১টা notification যায় (owner disputed)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- raise_dispute — happy path, caller = solver
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, accepted_solver_id, accepted_solver_name,
  is_public, is_disputed
) VALUES (
  'D2', '11111111-1111-1111-1111-111111111111', 'Dispute Test Problem 2', 'IN_PROGRESS',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, false
);

SELECT test.login_as('22222222-2222-2222-2222-222222222222');

SELECT is(
  (public.raise_dispute('D2', 'পেমেন্ট নিয়ে সমস্যা'))->>'result',
  'OK',
  'raise_dispute: solver হিসেবে কল করলে result OK'
);

SELECT results_eq(
  $$ SELECT dispute_initiator_role FROM public.problems WHERE id = 'D2' $$,
  $$ VALUES ('SOLVER'::text) $$,
  'raise_dispute: solver কল করলে dispute_initiator_role=SOLVER হয়'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND related_problem_id = 'D2'),
  1,
  'raise_dispute: solver dispute তুললে owner-কে notification যায়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- raise_dispute — edge cases
----------------------------------------------------------------------
SELECT test.login_as('33333333-3333-3333-3333-333333333333');

SELECT throws_ok(
  $$ SELECT public.raise_dispute('D1', 'x') $$,
  'NOT_AUTHORIZED',
  'raise_dispute: সম্পর্কহীন তৃতীয় ব্যক্তি কল করলে NOT_AUTHORIZED'
);

SELECT throws_ok(
  $$ SELECT public.raise_dispute('NO_SUCH_PROBLEM', 'x') $$,
  'PROBLEM_NOT_FOUND',
  'raise_dispute: PROBLEM_NOT_FOUND'
);

SELECT test.logout();

----------------------------------------------------------------------
-- settle_dispute — happy path, caller = owner
----------------------------------------------------------------------
UPDATE public.problems SET
  is_disputed = true, dispute_reason = 'কাজ সম্পূর্ণ হয়নি',
  dispute_initiator_id = '11111111-1111-1111-1111-111111111111',
  dispute_initiator_role = 'USER', disputed_at = now(), dispute_settled_at = null
WHERE id = 'D1';

SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.settle_dispute('D1'))->>'result',
  'OK',
  'settle_dispute: owner হিসেবে কল করলে result OK'
);

SELECT results_eq(
  $$ SELECT is_disputed, dispute_reason, dispute_initiator_id, dispute_initiator_role
     FROM public.problems WHERE id = 'D1' $$,
  $$ VALUES (false, NULL::text, NULL::uuid, NULL::text) $$,
  'settle_dispute: is_disputed=false + dispute_reason/initiator_id/role সব null হয়ে যায়'
);

SELECT isnt(
  (SELECT dispute_settled_at FROM public.problems WHERE id = 'D1'),
  NULL,
  'settle_dispute: dispute_settled_at সেট হয়'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE user_id = '22222222-2222-2222-2222-222222222222' AND related_problem_id = 'D1' AND title = 'সমঝোতা সম্পন্ন হয়েছে 🤝'),
  1,
  'settle_dispute: অন্য পক্ষকে "সমঝোতা সম্পন্ন" notification যায়'
);

SELECT test.logout();

----------------------------------------------------------------------
-- settle_dispute — ALREADY_SETTLED (is_disputed=false, dispute_settled_at থাকা অবস্থায়)
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT is(
  (public.settle_dispute('D1'))->>'result',
  'ALREADY_SETTLED',
  'settle_dispute: দ্বিতীয়বার কল করলে (is_disputed=false, dispute_settled_at থাকা অবস্থায়) ALREADY_SETTLED'
);

SELECT test.logout();

----------------------------------------------------------------------
-- settle_dispute — edge cases
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, accepted_solver_id, accepted_solver_name,
  is_public, is_disputed, dispute_initiator_id, dispute_initiator_role, disputed_at
) VALUES (
  'D3', '11111111-1111-1111-1111-111111111111', 'Dispute Test Problem 3', 'IN_PROGRESS',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, true,
  '11111111-1111-1111-1111-111111111111', 'USER', now()
);

SELECT test.login_as('33333333-3333-3333-3333-333333333333');

SELECT throws_ok(
  $$ SELECT public.settle_dispute('D3') $$,
  'NOT_AUTHORIZED',
  'settle_dispute: সম্পর্কহীন তৃতীয় ব্যক্তি কল করলে NOT_AUTHORIZED'
);

SELECT throws_ok(
  $$ SELECT public.settle_dispute('NO_SUCH_PROBLEM') $$,
  'PROBLEM_NOT_FOUND',
  'settle_dispute: PROBLEM_NOT_FOUND'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_manually_flag_dispute — happy path (accepted_solver_id আছে)
--
-- ⚠️ এই সেশনে প্রথম real-run-এ ধরা পড়েছে: এখানে আগে D2 (উপরে raise_dispute
-- solver-happy-path সেকশনে ব্যবহৃত) ভুলবশত reuse করা হয়েছিল — কিন্তু D2
-- ততক্ষণে is_disputed=true (solver dispute তুলেছিল), তাই admin flag করতে
-- গেলে ALREADY_DISPUTED আসছিল (ফাংশনের বাগ না, টেস্ট-ডেটার ভুল reuse)।
-- ফিক্স: এই happy-path-এর জন্য আলাদা fresh problem (D5) ব্যবহার করা হলো।
----------------------------------------------------------------------
INSERT INTO public.problems (
  id, user_id, title, status, accepted_solver_id, accepted_solver_name,
  is_public, is_disputed
) VALUES (
  'D5', '11111111-1111-1111-1111-111111111111', 'Dispute Test Problem 5', 'IN_PROGRESS',
  '22222222-2222-2222-2222-222222222222', 'Test Solver One', true, false
);

RESET ROLE;

SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT is(
  (public.admin_manually_flag_dispute('D5', '  সন্দেহজনক কার্যকলাপ  '))->>'result',
  'OK',
  'admin_manually_flag_dispute: admin হিসেবে কল করলে result OK'
);

SELECT results_eq(
  $$ SELECT is_disputed, dispute_reason, dispute_initiator_id, dispute_initiator_role
     FROM public.problems WHERE id = 'D5' $$,
  $$ VALUES (true, 'সন্দেহজনক কার্যকলাপ'::text, NULL::uuid, 'ADMIN'::text) $$,
  'admin_manually_flag_dispute: is_disputed=true, dispute_reason(trim), initiator_id=null, role=ADMIN'
);

SELECT isnt(
  (SELECT disputed_at FROM public.problems WHERE id = 'D5'),
  NULL,
  'admin_manually_flag_dispute: disputed_at সেট হয়'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE related_problem_id = 'D5' AND title = '⚠️ অ্যাডমিন দ্বারা বিরোধ (Dispute) ফ্ল্যাগ করা হয়েছে'),
  2,
  'admin_manually_flag_dispute: accepted_solver থাকলে owner+solver দুজনকেই notification যায় (২টা)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_manually_flag_dispute — accepted_solver_id NULL হলে শুধু owner-কে notification
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public, is_disputed)
VALUES ('D4', '11111111-1111-1111-1111-111111111111', 'Dispute Test Problem 4 (no solver)', 'OPEN', true, false);

RESET ROLE;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT is(
  (public.admin_manually_flag_dispute('D4', 'টেস্ট কারণ'))->>'result',
  'OK',
  'admin_manually_flag_dispute: accepted_solver_id NULL থাকা problem-এও result OK'
);

SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE related_problem_id = 'D4'),
  1,
  'admin_manually_flag_dispute: accepted_solver_id NULL হলে শুধু owner-কে ১টা notification যায়'
);

----------------------------------------------------------------------
-- admin_manually_flag_dispute — ALREADY_DISPUTED
-- (D5 এখন is_disputed=true, উপরের happy-path কলের ফলে — দ্বিতীয়বার কল করলে)
----------------------------------------------------------------------
SELECT is(
  (public.admin_manually_flag_dispute('D5', 'আবার চেষ্টা'))->>'result',
  'ALREADY_DISPUTED',
  'admin_manually_flag_dispute: আগে থেকেই is_disputed=true থাকলে ALREADY_DISPUTED (কোনো পরিবর্তন হয় না)'
);

SELECT test.logout();

----------------------------------------------------------------------
-- admin_manually_flag_dispute — edge cases
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

SELECT throws_ok(
  $$ SELECT public.admin_manually_flag_dispute('D3', 'x') $$,
  'NOT_AUTHORIZED',
  'admin_manually_flag_dispute: non-admin কল করলে NOT_AUTHORIZED'
);

SELECT test.logout();
RESET ROLE;
SELECT test.login_as('99999999-9999-9999-9999-999999999999');

SELECT throws_ok(
  $$ SELECT public.admin_manually_flag_dispute('NO_SUCH_PROBLEM', 'x') $$,
  'PROBLEM_NOT_FOUND',
  'admin_manually_flag_dispute: PROBLEM_NOT_FOUND'
);

SELECT test.logout();

SELECT * FROM finish();
ROLLBACK;
