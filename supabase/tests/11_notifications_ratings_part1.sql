-- 11_notifications_ratings_part1.sql — Step 8 (Notifications & ratings), PART 1 of 2
--
-- এই ফাইলে ৭টা ফাংশন কভার করা হলো (Step 8-এর ৯টা ফাংশনের প্রথম অর্ধেক — বাকি অর্ধেক
-- `submit_rating` ও `submit_reputation_event` PART 2-এর জন্য CI_TEST_SUITE_PROGRESS.md-এ
-- handoff নোট হিসেবে রাখা হয়েছে):
--   create_notification (৭-আর্গ; ৬-আর্গ overload-এর ambiguity সহ) → recovered_notifications.sql
--   notify_admins                                                  → recovered_notifications.sql
--   mark_problem_seen                                              → step32_85_mark_problem_seen.sql
--   mark_dispute_result_seen / mark_completion_result_seen        → step32_85_mark_result_seen_flags.sql
--   system_event_message                                           → step32_95_system_event_message.sql
--   request_admin_assistance                                       → recovered_disputes.sql
--
-- সব ৭টা ফাংশনের real body এই সেশনে সরাসরি পড়ে verify করা হয়েছে; case-insensitive grep
-- (rule #5a) দিয়ে নিশ্চিত: প্রতিটার ঠিক একটাই সংজ্ঞা (create_notification-এর দুটো overload
-- বাদে), কোনো পরবর্তী migration override/DROP করেনি।
--
-- ⚠️ "DOCUMENTED CURRENT BEHAVIOUR" লেবেলযুক্ত assertion-গুলো সম্ভাব্য বাগ লক করে রাখে (PostgreSQL-এর
-- three-valued logic: `x <> NULL` = NULL, plpgsql `IF NULL` = false — তাই `accepted_solver_id IS NULL`
-- এমন problem-এ বা `auth.uid()` NULL হলে কিছু authorization-guard নীরবে পাশ কাটে)। এই ফাইলে কোনো
-- migration/ফাংশন বদলানো হয়নি (rule #1) — ফিক্স হলে ওই assertion ইচ্ছাকৃতভাবে ভাঙবে, তখন
-- প্রত্যাশিত মান আপডেট করবে। বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এর Step 8 এন্ট্রিতে।
--
-- ⚠️ Bengali literal (`সিস্টেম`, `অ্যাডমিন সাপোর্ট ডেস্ক`, `গ্রাহক`, `সমাধানকারী`) হাতে টাইপ করা
-- হয়নি — migration ফাইল থেকে Python-এ regex দিয়ে হুবহু extract করে বসানো হয়েছে (Unicode
-- normalization mismatch এড়াতে)।
--
-- ⚠️ এই সেশনেও sandbox network বন্ধ (apt-get install postgresql postgresql-16-pgtap → 403) — তাই এই
-- ফাইল শুধু static ভাবে verify করা: প্রতিটা assertion ফাংশন-বডির সাথে মিলিয়ে, throws_ok-এর message
-- বডির `raise exception '…'`-এর সাথে script দিয়ে মিলিয়ে, fixture-কলাম ↔ stub মিলিয়ে, plan() সংখ্যা
-- script দিয়ে গুনে। real Postgres+pgTAP-এ কখনো চালানো হয়নি।
--
-- ⚠️⚠️ ২০২৬-০৯-২১ (Step 12.12, F1 ফিক্স, ব্যবহারকারীর অনুমতিতে) — create_notification-এর পুরনো
-- (p_role-বিহীন) ৬-আর্গ overload step12_9 migration-এ DROP হওয়ায় "২টা overload আছে" ও "৬-আর্গ কল
-- ambiguous (42725)" assertion দুটো ঠিক থাকে না — এখন ১টাই overload, আর ৬-আর্গ কল p_role
-- DEFAULT ('''') দিয়ে resolve হয়ে সফল হয় (migration body সরাসরি পড়ে p_role DEFAULT ''::text
-- নিশ্চিত করা হয়েছে)। plan() 101→102। এই সেশনেও sandbox network বন্ধ — static-ভাবেই যাচাই, real
-- Postgres-এ চালানো হয়নি। বিস্তারিত: CI_TEST_SUITE_PROGRESS.md-এর এই সেশনের "Step 12.12" সেকশন।
--
-- Fixture uuid prefix: f8… ; problem id prefix: NR_P… ; পুরো ফাইল BEGIN … ROLLBACK-এর ভেতরে।
-- ফাংশন-কল আর তার প্রভাব যাচাই আলাদা statement-এ; verification/fixture-এর আগে `RESET ROLE`।

BEGIN;
SELECT plan(102);

SELECT test.seed_users();
-- CLIENT(owner): 11111111-...  SOLVER 1(accepted): 22222222-...  SOLVER 2(bidder only): 33333333-...  ADMIN: 99999999-...

RESET ROLE;
INSERT INTO public.users (id, role, name, phone, has_user_role, has_solver_role) VALUES
  ('f8000001-0000-0000-0000-000000000001', 'CLIENT', 'NR Outsider',     '01790000001', true, false),
  ('f8000002-0000-0000-0000-000000000002', 'ADMIN',  'NR Second Admin', '01790000002', true, false);

-- NR_P1: owner + accepted solver (S1) + একটা PENDING bidder (S2)
-- NR_P2: accepted_solver_id IS NULL (solver-ছাড়া) — NULL-তুলনা guard-এর DOCUMENTED কেসগুলোর জন্য
-- NR_P3: mark_problem_seen-এর lower-case/'ADMIN' role-string কেস
-- NR_P4: mark_*_result_seen  |  NR_P6/P7/P8/P9: request_admin_assistance
INSERT INTO public.problems (id, user_id, title, status, accepted_solver_id, user_last_seen_at, solver_last_seen_at, last_activity_at) VALUES
  ('NR_P1', '11111111-1111-1111-1111-111111111111', 'NR P1', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', '2020-01-01 00:00:00+00', '2020-01-01 00:00:00+00', '2020-01-01 00:00:00+00'),
  ('NR_P2', '11111111-1111-1111-1111-111111111111', 'NR P2', 'OPEN',        NULL,                                   '2020-01-01 00:00:00+00', '2020-01-01 00:00:00+00', '2020-01-01 00:00:00+00'),
  ('NR_P3', '11111111-1111-1111-1111-111111111111', 'NR P3', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', '2020-01-01 00:00:00+00', '2020-01-01 00:00:00+00', '2020-01-01 00:00:00+00'),
  ('NR_P4', '11111111-1111-1111-1111-111111111111', 'NR P4', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', NULL, NULL, '2020-01-01 00:00:00+00'),
  ('NR_P6', '11111111-1111-1111-1111-111111111111', 'NR P6', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', NULL, NULL, '2020-01-01 00:00:00+00'),
  ('NR_P7', '11111111-1111-1111-1111-111111111111', 'NR P7', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', NULL, NULL, '2020-01-01 00:00:00+00'),
  ('NR_P8', '11111111-1111-1111-1111-111111111111', 'NR P8', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', NULL, NULL, '2020-01-01 00:00:00+00'),
  ('NR_P9', '11111111-1111-1111-1111-111111111111', 'NR P9', 'IN_PROGRESS', '22222222-2222-2222-2222-222222222222', NULL, NULL, '2020-01-01 00:00:00+00');
INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status) VALUES
  ('NR_B1', 'NR_P1', '33333333-3333-3333-3333-333333333333', 'Test Solver Two', 100, 'PENDING');

----------------------------------------------------------------------
-- create_notification (৭-আর্গ)
----------------------------------------------------------------------
SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.create_notification('11111111-1111-1111-1111-111111111111', 'T', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'AUTH_REQUIRED',
  'create_notification: লগইন ছাড়া (auth.uid() NULL) → AUTH_REQUIRED'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.create_notification(NULL, 'T', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'TARGET_USER_ID_REQUIRED',
  'create_notification: p_target_user_id NULL → TARGET_USER_ID_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.create_notification('11111111-1111-1111-1111-111111111111', NULL, 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'TITLE_REQUIRED',
  'create_notification: p_title NULL → TITLE_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.create_notification('11111111-1111-1111-1111-111111111111', '   ', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'TITLE_REQUIRED',
  'create_notification: p_title শুধু-স্পেস → TITLE_REQUIRED (trim-এর পর ফাঁকা)'
);

-- self-notify happy path (target_type NULL → 'general', p_role NULL → '', title/message trim)
SELECT set_config('test.cn_self',
  public.create_notification('11111111-1111-1111-1111-111111111111', '  Self Title  ', '  self body  ', NULL, NULL, NULL, NULL)::text,
  true);
SELECT is(
  current_setting('test.cn_self')::jsonb->>'result',
  'OK',
  'create_notification: self-notify happy path → result OK'
);
SELECT ok(
  starts_with(current_setting('test.cn_self')::jsonb->>'id', 'NOTIF_'),
  'create_notification: রিটার্ন করা id-র prefix NOTIF_'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT title, message, target_type, role, user_id::text FROM public.notifications WHERE id = (current_setting('test.cn_self')::jsonb->>'id') $$,
  $$ VALUES ('Self Title'::text, 'self body'::text, 'general'::text, ''::text, '11111111-1111-1111-1111-111111111111'::text) $$,
  'create_notification: title/message trim হয়, NULL target_type → "general", NULL p_role → "" (empty string), user_id = target'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.create_notification('22222222-2222-2222-2222-222222222222', 'T', 'M', 'general', NULL, NULL, '') $$,
  'P0001', 'NOT_AUTHORIZED',
  'create_notification: admin/self না, related_problem_id-ও নেই → NOT_AUTHORIZED'
);

-- দুজনেই problem-এর party (owner → accepted solver)
SELECT set_config('test.cn_party',
  public.create_notification('22222222-2222-2222-2222-222222222222', 'Party Ping', 'pp', 'problem', 'NR_P1', 'NR_P1', 'SOLVER')::text,
  true);
SELECT is(
  current_setting('test.cn_party')::jsonb->>'result',
  'OK',
  'create_notification: owner → accepted solver (দুজনেই NR_P1-এর party) → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT user_id::text, target_type, target_id, related_problem_id, role FROM public.notifications WHERE id = (current_setting('test.cn_party')::jsonb->>'id') $$,
  $$ VALUES ('22222222-2222-2222-2222-222222222222'::text, 'problem'::text, 'NR_P1'::text, 'NR_P1'::text, 'SOLVER'::text) $$,
  'create_notification: target_type/target_id/related_problem_id/p_role যেমন দেওয়া তেমন সারিতে বসে'
);

-- bidder-ও party (bids-এ solver_id মিললে)
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT set_config('test.cn_bidder',
  public.create_notification('11111111-1111-1111-1111-111111111111', 'Bidder Ping', 'bp', 'problem', 'NR_P1', 'NR_P1', '')::text,
  true);
SELECT is(
  current_setting('test.cn_bidder')::jsonb->>'result',
  'OK',
  'create_notification: bidder (bids-এ আছে) → owner, দুজনেই party → OK'
);
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND title = 'Bidder Ping'),
  1,
  'create_notification: bidder-এর notification owner-এর জন্য ঠিক ১টা সারি তৈরি করে'
);

-- caller party না
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.create_notification('11111111-1111-1111-1111-111111111111', 'T', 'M', 'problem', 'NR_P1', 'NR_P1', '') $$,
  'P0001', 'NOT_AUTHORIZED',
  'create_notification: caller সম্পর্কহীন (accepted solver থাকা problem-এ) → NOT_AUTHORIZED'
);

-- target party না / problem নেই / ambiguity
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.create_notification('f8000001-0000-0000-0000-000000000001', 'T', 'M', 'problem', 'NR_P1', 'NR_P1', '') $$,
  'P0001', 'NOT_AUTHORIZED',
  'create_notification: target সম্পর্কহীন (caller owner হলেও) → NOT_AUTHORIZED'
);
SELECT throws_ok(
  $$ SELECT public.create_notification('22222222-2222-2222-2222-222222222222', 'T', 'M', 'problem', 'NO_SUCH_PROBLEM', 'NO_SUCH_PROBLEM', '') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'create_notification: অস্তিত্বহীন related_problem_id → PROBLEM_NOT_FOUND'
);
SELECT is(
  (SELECT count(*)::int FROM pg_proc WHERE proname = 'create_notification' AND pronamespace = 'public'::regnamespace),
  1,
  'create_notification: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৬-আর্গ overload DROP হওয়ায় এখন public schema-য় ঠিক ১টা overload (৭-আর্গ)'
);
SELECT set_config('test.cn_6arg',
  public.create_notification('11111111-1111-1111-1111-111111111111'::uuid, 'T6'::text, 'M6'::text, 'general'::text, NULL::text, NULL::text)::text,
  true);
SELECT is(
  current_setting('test.cn_6arg')::jsonb->>'result',
  'OK',
  'create_notification: DOCUMENTED CURRENT BEHAVIOUR (12.9-পরবর্তী) — পুরনো ৬-আর্গ overload DROP হওয়ায় ৬-আর্গ কল আর ambiguous না, বরং p_role DEFAULT ('''') দিয়ে ৭-আর্গ ফাংশনেই resolve হয়ে সফল হয় (self-notify, caller=target)'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT role FROM public.notifications WHERE id = (current_setting('test.cn_6arg')::jsonb->>'id') $$,
  $$ VALUES (''::text) $$,
  'create_notification: ৬-আর্গ কলে p_role বাদ দিলে DEFAULT '''' (ফাঁকা স্ট্রিং) সারিতে বসে'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');

-- admin: যেকোনো target
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.create_notification('f8000001-0000-0000-0000-000000000001', 'Admin Ping', 'ap', 'general', NULL, NULL, ''))->>'result',
  'OK',
  'create_notification: admin → যেকোনো target (related problem ছাড়াই) → OK'
);

-- DOCUMENTED (পর্যবেক্ষণ ৩): solver-ছাড়া problem-এ অপরিচিত ইউজার owner-কে notification পাঠাতে পারে
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT is(
  (public.create_notification('11111111-1111-1111-1111-111111111111', 'Solverless Ping', 'sp', 'problem', 'NR_P2', 'NR_P2', ''))->>'result',
  'OK',
  'create_notification: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ) — accepted_solver_id NULL হলে v_caller_is_party NULL হয়ে `not (NULL and true)` = NULL, তাই সম্পর্কহীন অপরিচিত ইউজারও owner-কে notification পাঠাতে পারে (solver থাকলে NOT_AUTHORIZED হতো — ওপরের কেস)'
);
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE user_id = '11111111-1111-1111-1111-111111111111' AND title = 'Solverless Ping'),
  1,
  'create_notification: DOCUMENTED CURRENT BEHAVIOUR — solver-ছাড়া problem-এ অপরিচিতের notification সত্যিই owner-এর জন্য সারি হিসেবে বসে'
);

----------------------------------------------------------------------
-- notify_admins
----------------------------------------------------------------------
-- admin-সংখ্যা আপেক্ষিকভাবে গুনি (step33_2_seed_real_admin_auth_account.sql আলাদা admin seed করতে পারে)
SELECT set_config('test.admin_count', (SELECT count(*) FROM public.users WHERE role = 'ADMIN')::text, true);

SELECT test.logout();
SELECT throws_ok(
  $$ SELECT public.notify_admins('T', 'M') $$,
  'P0001', 'AUTH_REQUIRED',
  'notify_admins: লগইন ছাড়া → AUTH_REQUIRED'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.notify_admins(NULL, 'M') $$,
  'P0001', 'TITLE_REQUIRED',
  'notify_admins: p_title NULL → TITLE_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.notify_admins('   ', 'M') $$,
  'P0001', 'TITLE_REQUIRED',
  'notify_admins: p_title শুধু-স্পেস → TITLE_REQUIRED'
);

-- non-admin caller (CLIENT) ডাকতে পারে — "admin সাহায্য চাই" ব্যবহারের জন্য ইচ্ছাকৃত মনে হয়
SELECT set_config('test.na_res',
  public.notify_admins('  Help Needed  ', '  need admin  ', 'NR_P1')::text,
  true);
SELECT is(
  current_setting('test.na_res')::jsonb->>'result',
  'OK',
  'notify_admins: non-admin caller-ও ডাকতে পারে → result OK'
);
SELECT is(
  current_setting('test.na_res')::jsonb->>'count',
  current_setting('test.admin_count'),
  'notify_admins: রিটার্ন করা count = users-এ role=ADMIN এমন সারির সংখ্যা'
);
RESET ROLE;
SELECT is(
  (SELECT count(*) FROM public.notifications WHERE title = 'Help Needed' AND user_id IN (SELECT id FROM public.users WHERE role = 'ADMIN'))::text,
  current_setting('test.admin_count'),
  'notify_admins: প্রতিটা admin-এর জন্য ঠিক ১টা notification (admin-সংখ্যার সমান সারি)'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE title = 'Help Needed' AND user_id NOT IN (SELECT id FROM public.users WHERE role = 'ADMIN')),
  0,
  'notify_admins: non-admin কেউ notification পায় না'
);
SELECT results_eq(
  $$ SELECT DISTINCT message, target_type, target_id, related_problem_id FROM public.notifications WHERE title = 'Help Needed' $$,
  $$ VALUES ('need admin'::text, 'problem'::text, 'NR_P1'::text, 'NR_P1'::text) $$,
  'notify_admins: title/message trim হয়, সব সারিতে target_type=problem, target_id=related_problem_id=NR_P1'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE title = 'Help Needed' AND id NOT LIKE 'NOTIF_ADMIN_%'),
  0,
  'notify_admins: প্রতিটা সারির id-র prefix NOTIF_ADMIN_'
);
SELECT is(
  (SELECT count(*)::int FROM public.notifications WHERE title = 'Help Needed' AND user_id = 'f8000002-0000-0000-0000-000000000002'),
  1,
  'notify_admins: দ্বিতীয় admin-ও (fixture) ঠিক ১টা notification পায় — fan-out একাধিক admin-এ'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT set_config('test.na_res2',
  public.notify_admins('General Admin Ping', 'no problem attached')::text,
  true);
RESET ROLE;
SELECT results_eq(
  $$ SELECT DISTINCT target_type, target_id IS NULL FROM public.notifications WHERE title = 'General Admin Ping' $$,
  $$ VALUES ('problem'::text, true) $$,
  'notify_admins: p_related_problem_id না দিলে target_id NULL, কিন্তু target_type তবু "problem" (DOCUMENTED CURRENT BEHAVIOUR — hardcoded)'
);

----------------------------------------------------------------------
-- mark_problem_seen
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.mark_problem_seen('NO_SUCH_PROBLEM', 'USER') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'mark_problem_seen: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.mark_problem_seen('NR_P1', 'SOLVER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_problem_seen: owner নিজে p_role=SOLVER দিলে → NOT_AUTHORIZED (accepted solver না)'
);
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.mark_problem_seen('NR_P1', 'SOLVER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_problem_seen: শুধু bidder (accepted solver না) p_role=SOLVER → NOT_AUTHORIZED'
);
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.mark_problem_seen('NR_P1', 'USER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_problem_seen: accepted solver নিজে p_role=USER দিলে → NOT_AUTHORIZED (owner না)'
);

-- auth.uid() NULL কিন্তু role authenticated (mark_*_seen শুধু authenticated-কে GRANT করা)
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT set_config('request.jwt.claim.sub', '', true);
SELECT throws_ok(
  $$ SELECT public.mark_problem_seen('NR_P1', 'USER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_problem_seen: auth.uid() NULL + p_role=USER → NOT_AUTHORIZED (explicit `is null` guard আছে)'
);
SELECT throws_ok(
  $$ SELECT public.mark_problem_seen('NR_P1', 'SOLVER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_problem_seen: auth.uid() NULL + p_role=SOLVER → NOT_AUTHORIZED'
);

-- happy: owner (USER) → শুধু user_last_seen_at বদলায়
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.mark_problem_seen('NR_P1', 'USER'))->>'result',
  'OK',
  'mark_problem_seen: owner + p_role=USER → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT user_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz, solver_last_seen_at = '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'NR_P1' $$,
  $$ VALUES (true, true) $$,
  'mark_problem_seen: USER branch শুধু user_last_seen_at হালনাগাদ করে, solver_last_seen_at অপরিবর্তিত'
);
-- happy: accepted solver (SOLVER)
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.mark_problem_seen('NR_P1', 'SOLVER'))->>'result',
  'OK',
  'mark_problem_seen: accepted solver + p_role=SOLVER → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT user_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz, solver_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'NR_P1' $$,
  $$ VALUES (true, true) $$,
  'mark_problem_seen: SOLVER branch solver_last_seen_at হালনাগাদ করে'
);
-- lower-case 'solver' (upper() করে তুলনা) এবং 'ADMIN' (অন্য যেকোনো স্ট্রিং → user branch) — NR_P3 আলাদা
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.mark_problem_seen('NR_P3', 'solver'))->>'result',
  'OK',
  'mark_problem_seen: lower-case p_role=solver-ও SOLVER branch ধরে (upper() দিয়ে তুলনা) → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT user_last_seen_at = '2020-01-01 00:00:00+00'::timestamptz, solver_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'NR_P3' $$,
  $$ VALUES (true, true) $$,
  'mark_problem_seen: lower-case solver কল শুধু solver_last_seen_at বদলায়'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.mark_problem_seen('NR_P3', 'ADMIN'))->>'result',
  'OK',
  'mark_problem_seen: DOCUMENTED CURRENT BEHAVIOUR — p_role="ADMIN" (SOLVER ছাড়া যেকোনো স্ট্রিং) user-branch-এ যায়; caller owner হলে OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT user_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz, solver_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'NR_P3' $$,
  $$ VALUES (true, true) $$,
  'mark_problem_seen: p_role="ADMIN" কল user_last_seen_at বদলায় (এখন দুটোই হালনাগাদ)'
);

-- DOCUMENTED (পর্যবেক্ষণ ২): accepted_solver_id NULL হলে যেকোনো লগইন-করা ইউজার SOLVER হিসেবে পার পায়
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT is(
  (public.mark_problem_seen('NR_P2', 'SOLVER'))->>'result',
  'OK',
  'mark_problem_seen: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ) — accepted_solver_id NULL হলে `uid <> NULL` = NULL, তাই সম্পর্কহীন যেকোনো লগইন-করা ইউজার p_role=SOLVER দিয়ে OK পায়'
);
RESET ROLE;
SELECT ok(
  (SELECT solver_last_seen_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'NR_P2'),
  'mark_problem_seen: DOCUMENTED CURRENT BEHAVIOUR — solver-ছাড়া problem-এ অপরিচিতের কলে solver_last_seen_at সত্যিই বদলে যায়'
);

----------------------------------------------------------------------
-- mark_dispute_result_seen
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.mark_dispute_result_seen('NO_SUCH_PROBLEM', true) $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'mark_dispute_result_seen: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.mark_dispute_result_seen('NR_P4', true) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_dispute_result_seen: p_is_user=true কিন্তু caller owner না (accepted solver) → NOT_AUTHORIZED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.mark_dispute_result_seen('NR_P4', false) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_dispute_result_seen: p_is_user=false কিন্তু caller accepted solver না (owner) → NOT_AUTHORIZED'
);
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.mark_dispute_result_seen('NR_P4', false) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_dispute_result_seen: p_is_user=false, শুধু bidder → NOT_AUTHORIZED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT set_config('request.jwt.claim.sub', '', true);
SELECT throws_ok(
  $$ SELECT public.mark_dispute_result_seen('NR_P4', true) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_dispute_result_seen: auth.uid() NULL + p_is_user=true → NOT_AUTHORIZED'
);
SELECT throws_ok(
  $$ SELECT public.mark_dispute_result_seen('NR_P4', false) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_dispute_result_seen: auth.uid() NULL + p_is_user=false → NOT_AUTHORIZED'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.mark_dispute_result_seen('NR_P4', true))->>'result',
  'OK',
  'mark_dispute_result_seen: owner + p_is_user=true → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT dispute_result_seen_by_user, dispute_result_seen_by_solver, completion_result_seen_by_user, completion_result_seen_by_solver FROM public.problems WHERE id = 'NR_P4' $$,
  $$ VALUES (true, false, false, false) $$,
  'mark_dispute_result_seen: শুধু dispute_result_seen_by_user=true হয়, বাকি ৩টা flag (solver + completion দুটো) অপরিবর্তিত'
);
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.mark_dispute_result_seen('NR_P4', false))->>'result',
  'OK',
  'mark_dispute_result_seen: accepted solver + p_is_user=false → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT dispute_result_seen_by_user, dispute_result_seen_by_solver, completion_result_seen_by_user, completion_result_seen_by_solver FROM public.problems WHERE id = 'NR_P4' $$,
  $$ VALUES (true, true, false, false) $$,
  'mark_dispute_result_seen: এখন dispute_result_seen_by_solver-ও true, completion flag দুটো তখনো false'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.mark_dispute_result_seen('NR_P4', true))->>'result',
  'OK',
  'mark_dispute_result_seen: একই কল দ্বিতীয়বার → আবারও OK (idempotent, flag true-ই থাকে)'
);

----------------------------------------------------------------------
-- mark_completion_result_seen
----------------------------------------------------------------------
SELECT throws_ok(
  $$ SELECT public.mark_completion_result_seen('NO_SUCH_PROBLEM', true) $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'mark_completion_result_seen: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT throws_ok(
  $$ SELECT public.mark_completion_result_seen('NR_P4', true) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_completion_result_seen: p_is_user=true কিন্তু caller owner না → NOT_AUTHORIZED'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.mark_completion_result_seen('NR_P4', false) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_completion_result_seen: p_is_user=false কিন্তু caller accepted solver না → NOT_AUTHORIZED'
);
SELECT set_config('request.jwt.claim.sub', '', true);
SELECT throws_ok(
  $$ SELECT public.mark_completion_result_seen('NR_P4', false) $$,
  'P0001', 'NOT_AUTHORIZED',
  'mark_completion_result_seen: auth.uid() NULL + p_is_user=false → NOT_AUTHORIZED'
);

SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.mark_completion_result_seen('NR_P4', true))->>'result',
  'OK',
  'mark_completion_result_seen: owner + p_is_user=true → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT dispute_result_seen_by_user, dispute_result_seen_by_solver, completion_result_seen_by_user, completion_result_seen_by_solver FROM public.problems WHERE id = 'NR_P4' $$,
  $$ VALUES (true, true, true, false) $$,
  'mark_completion_result_seen: শুধু completion_result_seen_by_user=true হয়; dispute flag দুটো আগের মতোই true'
);
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.mark_completion_result_seen('NR_P4', false))->>'result',
  'OK',
  'mark_completion_result_seen: accepted solver + p_is_user=false → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT dispute_result_seen_by_user, dispute_result_seen_by_solver, completion_result_seen_by_user, completion_result_seen_by_solver FROM public.problems WHERE id = 'NR_P4' $$,
  $$ VALUES (true, true, true, true) $$,
  'mark_completion_result_seen: এখন ৪টা flag-ই true'
);

-- DOCUMENTED (পর্যবেক্ষণ ২): solver-ছাড়া problem-এ অপরিচিত ইউজার p_is_user=false দিয়ে পার পায়
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT is(
  (public.mark_dispute_result_seen('NR_P2', false))->>'result',
  'OK',
  'mark_dispute_result_seen: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ) — accepted_solver_id NULL হলে সম্পর্কহীন লগইন-করা ইউজার p_is_user=false দিয়ে OK পায়'
);
SELECT is(
  (public.mark_completion_result_seen('NR_P2', false))->>'result',
  'OK',
  'mark_completion_result_seen: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ) — accepted_solver_id NULL হলে সম্পর্কহীন ইউজার p_is_user=false দিয়ে OK পায়'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT dispute_result_seen_by_solver, completion_result_seen_by_solver FROM public.problems WHERE id = 'NR_P2' $$,
  $$ VALUES (true, true) $$,
  'mark_*_result_seen: DOCUMENTED CURRENT BEHAVIOUR — solver-ছাড়া problem-এ অপরিচিতের কলে দুটো by_solver flag-ই সত্যিই true হয়ে যায়'
);

----------------------------------------------------------------------
-- system_event_message
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.system_event_message('NO_SUCH_PROBLEM', NULL, 'EVT', 'c') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'system_event_message: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT throws_ok(
  $$ SELECT public.system_event_message('NR_P1', NULL, 'EVT', '   ') $$,
  'P0001', 'CONTENT_REQUIRED',
  'system_event_message: p_content শুধু-স্পেস (authorized caller) → CONTENT_REQUIRED'
);
SELECT throws_ok(
  $$ SELECT public.system_event_message('NR_P1', NULL, 'EVT', NULL) $$,
  'P0001', 'CONTENT_REQUIRED',
  'system_event_message: p_content NULL → CONTENT_REQUIRED'
);
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.system_event_message('NR_P1', NULL, 'EVT', 'x') $$,
  'P0001', 'NOT_AUTHORIZED',
  'system_event_message: সম্পর্কহীন ইউজার (accepted solver থাকা problem-এ) → NOT_AUTHORIZED'
);
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.system_event_message('NR_P1', NULL, 'EVT', 'x') $$,
  'P0001', 'NOT_AUTHORIZED',
  'system_event_message: শুধু bidder (admin/owner/accepted solver কোনোটাই না) → NOT_AUTHORIZED'
);

-- happy: owner
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT set_config('test.se_res',
  public.system_event_message('NR_P1', '22222222-2222-2222-2222-222222222222', 'DISPUTE_OPENED', '  Dispute opened  ', 'Sys Name')::text,
  true);
SELECT is(
  current_setting('test.se_res')::jsonb->>'result',
  'OK',
  'system_event_message: owner happy path → result OK'
);
SELECT ok(
  starts_with(current_setting('test.se_res')::jsonb->>'id', 'MSG_'),
  'system_event_message: রিটার্ন করা id-র prefix MSG_'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT problem_id, sender_id IS NULL, receiver_id::text, sender_name, content, is_system_event, system_event_type, is_read, is_admin_message
     FROM public.messages WHERE id = (current_setting('test.se_res')::jsonb->>'id') $$,
  $$ VALUES ('NR_P1'::text, true, '22222222-2222-2222-2222-222222222222'::text, 'Sys Name'::text, 'Dispute opened'::text, true, 'DISPUTE_OPENED'::text, false, false) $$,
  'system_event_message: sender_id NULL, receiver_id = দেওয়া uuid, content trim, is_system_event=true, system_event_type সেট, is_read=false, is_admin_message=false'
);
SELECT ok(
  (SELECT last_activity_at > '2020-01-01 00:00:00+00'::timestamptz FROM public.problems WHERE id = 'NR_P1'),
  'system_event_message: problems.last_activity_at হালনাগাদ হয়'
);

-- accepted solver ও admin-ও ডাকতে পারে
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.system_event_message('NR_P1', NULL, 'EVT', 'by solver'))->>'result',
  'OK',
  'system_event_message: accepted solver → OK'
);
SELECT test.login_as('99999999-9999-9999-9999-999999999999');
SELECT is(
  (public.system_event_message('NR_P1', NULL, 'EVT', 'by admin'))->>'result',
  'OK',
  'system_event_message: admin → OK'
);

-- p_receiver_id অবৈধ/ফাঁকা/NULL হলে নীরবে receiver_id NULL (exception খেয়ে ফেলে)
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
DO $$ BEGIN PERFORM public.system_event_message('NR_P1', 'not-a-uuid', 'RCV_TEST', 'r1'); END $$;
DO $$ BEGIN PERFORM public.system_event_message('NR_P1', '', 'RCV_TEST', 'r2'); END $$;
DO $$ BEGIN PERFORM public.system_event_message('NR_P1', NULL, 'RCV_TEST', 'r3'); END $$;
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM public.messages WHERE system_event_type = 'RCV_TEST' AND receiver_id IS NULL AND sender_id IS NULL AND is_system_event),
  3,
  'system_event_message: অবৈধ ("not-a-uuid") / ফাঁকা / NULL p_receiver_id — তিনটাতেই exception ছাড়া সারি তৈরি, receiver_id NULL'
);

-- p_sender_name-এর ডিফল্ট (৪-আর্গ কল)
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
DO $$ BEGIN PERFORM public.system_event_message('NR_P1', NULL, 'DEFAULT_NAME_TEST', 'default name'); END $$;
RESET ROLE;
SELECT is(
  (SELECT sender_name FROM public.messages WHERE system_event_type = 'DEFAULT_NAME_TEST'),
  'সিস্টেম',
  'system_event_message: p_sender_name না দিলে ডিফল্ট মান (migration থেকে extract করা) বসে'
);

-- DOCUMENTED (পর্যবেক্ষণ ২)
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT is(
  (public.system_event_message('NR_P2', NULL, 'EVT', 'solverless outsider'))->>'result',
  'OK',
  'system_event_message: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ) — accepted_solver_id NULL হলে guard `not (admin or owner or uid = NULL)` = NULL, তাই সম্পর্কহীন লগইন-করা ইউজারও সিস্টেম-ইভেন্ট মেসেজ ঢোকাতে পারে'
);
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT set_config('request.jwt.claim.sub', '', true);
SELECT is(
  (public.system_event_message('NR_P1', NULL, 'EVT', 'null uid'))->>'result',
  'OK',
  'system_event_message: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ; stub is_admin-এর উপর নির্ভরশীল) — auth.uid() NULL হলে তিনটা তুলনাই false/NULL, guard পাশ কাটে, মেসেজ ঢোকে'
);

----------------------------------------------------------------------
-- request_admin_assistance
----------------------------------------------------------------------
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT throws_ok(
  $$ SELECT public.request_admin_assistance('NO_SUCH_PROBLEM', 'USER') $$,
  'P0001', 'PROBLEM_NOT_FOUND',
  'request_admin_assistance: অস্তিত্বহীন problem → PROBLEM_NOT_FOUND'
);
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT throws_ok(
  $$ SELECT public.request_admin_assistance('NR_P1', 'USER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'request_admin_assistance: সম্পর্কহীন ইউজার → NOT_AUTHORIZED'
);
SELECT test.login_as('33333333-3333-3333-3333-333333333333');
SELECT throws_ok(
  $$ SELECT public.request_admin_assistance('NR_P1', 'SOLVER') $$,
  'P0001', 'NOT_AUTHORIZED',
  'request_admin_assistance: শুধু bidder (owner/accepted solver না) → NOT_AUTHORIZED'
);

-- happy: owner (USER)
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.request_admin_assistance('NR_P6', 'USER'))->>'result',
  'OK',
  'request_admin_assistance: owner + p_requester_role=USER → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT is_admin_involved_in_chat, admin_assistance_requested_by, admin_assistance_requested_at IS NOT NULL, last_activity_at > '2020-01-01 00:00:00+00'::timestamptz
     FROM public.problems WHERE id = 'NR_P6' $$,
  $$ VALUES (true, 'USER'::text, true, true) $$,
  'request_admin_assistance: problems-এ is_admin_involved_in_chat=true, requested_by=USER, requested_at সেট, last_activity_at হালনাগাদ'
);
SELECT results_eq(
  $$ SELECT sender_id::text, receiver_id::text, is_admin_message, is_read, sender_name LIKE 'অ্যাডমিন সাপোর্ট ডেস্ক%', content LIKE '%গ্রাহক%'
     FROM public.messages WHERE problem_id = 'NR_P6' $$,
  $$ VALUES ('11111111-1111-1111-1111-111111111111'::text, '22222222-2222-2222-2222-222222222222'::text, true, false, true, true) $$,
  'request_admin_assistance(USER): মেসেজের sender=caller, receiver=accepted solver, is_admin_message=true, is_read=false, sender_name সাপোর্ট-ডেস্ক prefix, content-এ "গ্রাহক"'
);

-- happy: accepted solver (SOLVER)
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.request_admin_assistance('NR_P7', 'SOLVER'))->>'result',
  'OK',
  'request_admin_assistance: accepted solver + p_requester_role=SOLVER → OK'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT is_admin_involved_in_chat, admin_assistance_requested_by FROM public.problems WHERE id = 'NR_P7' $$,
  $$ VALUES (true, 'SOLVER'::text) $$,
  'request_admin_assistance(SOLVER): problems-এ requested_by=SOLVER'
);
SELECT results_eq(
  $$ SELECT sender_id::text, receiver_id::text, content LIKE '%সমাধানকারী%' FROM public.messages WHERE problem_id = 'NR_P7' $$,
  $$ VALUES ('22222222-2222-2222-2222-222222222222'::text, '11111111-1111-1111-1111-111111111111'::text, true) $$,
  'request_admin_assistance(SOLVER): মেসেজের receiver=owner, content-এ "সমাধানকারী"'
);

-- DOCUMENTED: de-dup/rate-limit নেই — একই problem-এ দ্বিতীয় কলে আরেকটা মেসেজ
SELECT test.login_as('11111111-1111-1111-1111-111111111111');
SELECT is(
  (public.request_admin_assistance('NR_P6', 'USER'))->>'result',
  'OK',
  'request_admin_assistance: একই problem-এ আবার কল → আবারও OK (কোনো de-dup নেই)'
);
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM public.messages WHERE problem_id = 'NR_P6' AND is_admin_message),
  2,
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR — de-dup/rate-limit নেই, প্রতিবার কলে নতুন admin-message সারি (এখানে ২টা)'
);

-- DOCUMENTED (পর্যবেক্ষণ ৪): caller-এর আসল role যাচাই হয় না
SELECT test.login_as('22222222-2222-2222-2222-222222222222');
SELECT is(
  (public.request_admin_assistance('NR_P8', 'USER'))->>'result',
  'OK',
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR — accepted solver p_requester_role=USER দিলেও OK (caller-এর আসল role p_requester_role-এর সাথে মেলানো হয় না)'
);
RESET ROLE;
SELECT results_eq(
  $$ SELECT receiver_id = sender_id, content LIKE '%গ্রাহক%' FROM public.messages WHERE problem_id = 'NR_P8' $$,
  $$ VALUES (true, true) $$,
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR — solver "USER" দাবি করলে মেসেজ "গ্রাহক" লেখে আর receiver_id = accepted solver = caller নিজেই'
);

-- DOCUMENTED (পর্যবেক্ষণ ২): solver-ছাড়া problem-এ সম্পর্কহীন ইউজার
SELECT test.login_as('f8000001-0000-0000-0000-000000000001');
SELECT is(
  (public.request_admin_assistance('NR_P2', 'USER'))->>'result',
  'OK',
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ) — accepted_solver_id NULL হলে `uid <> owner AND uid <> NULL` = NULL, তাই সম্পর্কহীন লগইন-করা ইউজারও admin-সহায়তা চাইতে পারে'
);
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM public.messages WHERE problem_id = 'NR_P2' AND is_admin_message),
  1,
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR — solver-ছাড়া problem-এ অপরিচিতের কলে admin-message সারি সত্যিই তৈরি হয়'
);

-- DOCUMENTED (পর্যবেক্ষণ ২): anon (auth.uid() NULL) — migration-এ GRANT … TO anon আছে
SELECT test.logout();
SELECT is(
  (public.request_admin_assistance('NR_P9', 'USER'))->>'result',
  'OK',
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR (সম্ভাব্য বাগ; stub-এ messages.sender_id nullable ধরে) — লগইন ছাড়া (anon) কলেও authorization guard পাশ কাটে'
);
RESET ROLE;
SELECT is(
  (SELECT count(*)::int FROM public.messages WHERE problem_id = 'NR_P9' AND sender_id IS NULL AND is_admin_message),
  1,
  'request_admin_assistance: DOCUMENTED CURRENT BEHAVIOUR — anon কলে sender_id NULL সহ admin-message সারি তৈরি হয়'
);

SELECT * FROM finish();
ROLLBACK;
