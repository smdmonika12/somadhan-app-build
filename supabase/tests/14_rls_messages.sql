-- 14_rls_messages.sql — Step 14.2 (RLS policy coverage: `messages` টেবিল)
--
-- 14_rls_00_helpers.sql-এর discovery/helper ব্যবহার করে policy #১ (table SELECT
-- `messages_select`) ও #২ (broadcast SELECT "problem participants can receive
-- problem-topic broadcasts") দুটোই কভার করে — এটাই সেই solver-thread-isolation
-- ফিক্সের (`realtime_scoping_messages_solver_thread_isolation.sql`) regression-check।
--
-- ফাইল-নাম sort-order: `ls supabase/tests/*.sql | sort` দিয়ে এই সেশনে manually
-- confirm করা হয়েছে — "14_rls_00_helpers.sql" < "14_rls_messages.sql" (14.1-এর
-- "00_" কনভেনশন অনুযায়ী, '0' < 'm')।
--
-- স্কোপ-নোট: `messages_insert` policy টেস্ট করা হয়নি — কোনো migration-এ এটার
-- CREATE/ALTER POLICY নেই (শুধু `step32_95_system_event_message.sql`-এর একটা
-- কমেন্টে উল্লেখ আছে যে "auth.uid() = sender_id দাবি করে")। rule #5 অনুযায়ী
-- migration-এ verify-না-করা policy অনুমান করে টেস্ট লেখা হয়নি — এটা
-- problems_select/bids_select-এর (14_rls_00_helpers.sql-এর "বোনাস আবিষ্কার")
-- মতোই আরেকটা "policy migrations-এ কোথাও নেই" ব্লকার, নতুন করে এখানে নোট করা
-- হলো (14.1-এর ৫-CREATE-POLICY গণনায় এটা ধরা পড়েনি কারণ কখনো CREATE/ALTER হয়নি,
-- শুধু কমেন্টে উল্লেখ)। শুধু SELECT (দেখা/পড়া) visibility — table + broadcast
-- দুই স্তরেই — এই ফাইলের স্কোপ, 14.1-এর HANDOFF নোট অনুযায়ী।
--
-- 🔍 নতুন observation (এই সেশনে migration বডি পড়ে ধরা পড়েছে, নতুন কোড/fix না,
-- শুধু টেস্ট-ডিজাইনের জন্য গুরুত্বপূর্ণ): broadcast policy-র USING clause
-- (realtime.messages-এর উপর) row-এর নিজের `topic` কলাম কখনো ছোঁয় না — শুধু
-- সেশনের `realtime.topic()` GUC (session-এর subscribed-topic context) বনাম
-- `public.problems`-এ EXISTS চেক করে। বাস্তব Supabase Realtime-এ row-থেকে-
-- subscriber রাউটিং (কোন row কোন topic-এ যাবে) সম্পূর্ণ infra-লেয়ারে হয়, RLS
-- শুধু "এই session কি ওই topic subscribe করতে পারে" এই boolean gate-টা করে —
-- তাই আমাদের SQL-টেস্টেও বাস্তব সিস্টেমের আচরণ নকল করতে প্রতিটা query-তে
-- নিজে `WHERE topic = '<target>'` filter যোগ করা হয়েছে (নাহলে policy শুধু
-- session-context চেক করে, row-এর topic না — ভুলভাবে সব broadcast-row একসাথে
-- গোনা হয়ে যেত)।
--
-- 🔍 আরেকটা real-run-এ ধরা পড়া observation (প্রথম খসড়ায় ভুল করা হয়েছিল, real
-- psql+pgTAP দিয়ে যাচাই করার সময়ই ধরা পড়ে ঠিক করা হলো): `broadcast_messages_changes`
-- trigger (Step 13.6/13.7/13.8-এ কভার করা) ইতিমধ্যেই **সত্যিই সক্রিয়** — তাই section
-- (২)-এর জন্য আলাদা করে `realtime.messages`-এ ম্যানুয়ালি simulated broadcast-row বসানোর
-- দরকার নেই/ভুল: section (১)-এর `public.messages` INSERT দুটোই trigger নিজে থেকেই
-- ২টা broadcast row (topic='problem:P_RLS_MSG1', event='messages_INSERT') তৈরি করে
-- ফেলে — ম্যানুয়ালি আরেকটা বসালে (প্রথম খসড়ায় যেটা করা হয়েছিল) count ভুলভাবে ৩ দেখাতো
-- (২টা trigger-generated + ১টা ম্যানুয়াল)। তাই section (২) এখন কোনো নতুন INSERT করে না —
-- সরাসরি section (১)-এর trigger-generated broadcast-রো দুটোর উপর RLS visibility টেস্ট
-- করে (প্রত্যাশিত count তাই ২, ১ না)। section (৩)-এর বোনাস edge-case-এ ম্যানুয়াল INSERT
-- এখনো দরকার — কারণ `public.messages.problem_id`-এ FK আছে (`references
-- public.problems(id)`), তাই একটা অস্তিত্বহীন problem-এর জন্য real message insert করা
-- সম্ভবই না (FK violation), ম্যানুয়াল `realtime.messages` insert-ই একমাত্র উপায়।
--
-- 🔧 নতুন grant-gap (এই সেশনে ধরা পড়েছে, 14.1-এর "RLS-enablement gap"-এর
-- সমান্তরাল): `realtime` schema/`realtime.messages` টেবিলে কোনো GRANT
-- anon/authenticated-কে migrations-এ নেই (শুধু `public` schema-র টেবিলে আছে,
-- `01_bidding_flow_schema_stub.sql`-এ)। real Supabase-এ এই schema/টেবিল
-- platform-managed (আগে থেকেই GRANT করা)। rule #১ অনুযায়ী শেয়ার্ড
-- helper/schema-stub ফাইল বদলানো হয়নি — এই টেস্ট-ফাইলের নিজের
-- `BEGIN;...ROLLBACK;`-এর ভেতরেই স্থানীয়ভাবে GRANT করা হয়েছে (RLS-enable-এর
-- মতোই একই প্যাটার্নে, অন্য কোনো test file-এ leak করে না)।

BEGIN;

-- স্থানীয় grant (শুধু এই transaction-এর ভেতরে, অন্য কোনো ফাইলে leak করবে না)
GRANT USAGE ON SCHEMA realtime TO anon, authenticated;
GRANT SELECT ON realtime.messages TO anon, authenticated;

-- 14_rls_00_helpers.sql-এর "RLS-enablement gap" সেকশনের নির্দেশিত প্যাটার্ন
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;

SELECT plan(14);

SELECT test.seed_users();
-- CLIENT: 11111111-...   SOLVER1: 22222222-...   SOLVER2: 33333333-...   ADMIN: 99999999-...
SELECT test.seed_rls_users();
-- RLS User A (CLIENT-role, সম্পূর্ণ অসম্পর্কিত): e1400001-...
-- RLS User B (SOLVER-role, ব্যবহৃত হয়নি এই ফাইলে): e1400002-...

----------------------------------------------------------------------
-- ফিক্সচার — problem owner=CLIENT, accepted_solver=SOLVER1, দুইটা মেসেজ
-- (CLIENT<->SOLVER1 থ্রেড)। SOLVER2 ইচ্ছাকৃতভাবে এই থ্রেডের বাইরে — এটাই
-- "reassigned/অন্য solver পুরনো থ্রেড দেখতে পারে না" রিগ্রেশন-চেকের সিমুলেশন।
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, accepted_solver_id, title, status)
VALUES ('P_RLS_MSG1', '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222', 'RLS Messages Thread Fixture', 'IN_PROGRESS');

INSERT INTO public.messages (id, problem_id, sender_id, receiver_id, sender_name, content)
VALUES
  ('MSG_RLS_1', 'P_RLS_MSG1', '11111111-1111-1111-1111-111111111111',
   '22222222-2222-2222-2222-222222222222', 'Test Client', 'Hi solver, need help with this'),
  ('MSG_RLS_2', 'P_RLS_MSG1', '22222222-2222-2222-2222-222222222222',
   '11111111-1111-1111-1111-111111111111', 'Test Solver One', 'Sure, on my way');

----------------------------------------------------------------------
-- ১) Table SELECT policy (`messages_select`) — ৬টা assertion
----------------------------------------------------------------------
SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from public.messages where problem_id = ''P_RLS_MSG1'''),
  2::bigint,
  'messages_select: owner+sender (CLIENT) নিজের থ্রেডের দুটো মেসেজ-ই দেখতে পারে'
);
SELECT is(
  test.count_as('22222222-2222-2222-2222-222222222222',
    'select count(*) from public.messages where problem_id = ''P_RLS_MSG1'''),
  2::bigint,
  'messages_select: বর্তমান accepted_solver (SOLVER1, sender/receiver) দুটো মেসেজ-ই দেখতে পারে'
);
SELECT is(
  test.count_as('33333333-3333-3333-3333-333333333333',
    'select count(*) from public.messages where problem_id = ''P_RLS_MSG1'''),
  0::bigint,
  '⭐ messages_select: অন্য solver (SOLVER2, না owner না sender/receiver) এই থ্রেডের কোনো মেসেজ দেখতে পারে না (solver-thread-isolation regression-check)'
);
SELECT is(
  test.count_as('99999999-9999-9999-9999-999999999999',
    'select count(*) from public.messages where problem_id = ''P_RLS_MSG1'''),
  2::bigint,
  'messages_select: admin সব মেসেজ দেখতে পারে (is_admin() bypass)'
);
SELECT is(
  test.count_as('e1400001-0000-0000-0000-000000000001',
    'select count(*) from public.messages where problem_id = ''P_RLS_MSG1'''),
  0::bigint,
  'messages_select: সম্পূর্ণ অসম্পর্কিত user (owner/sender/receiver/admin কিছুই না) কোনো মেসেজ দেখতে পারে না'
);
SELECT test.logout();
SELECT is(
  (SELECT count(*) FROM public.messages WHERE problem_id = 'P_RLS_MSG1'),
  0::bigint,
  'messages_select: anon (login ছাড়া) কোনো মেসেজ দেখতে পারে না (policy শুধু "to authenticated")'
);
-- ⚠️ test.logout() যেভাবে set_config(...,true) দিয়ে role='anon' সেট করে, সেটা পুরো
-- transaction জুড়ে (এই পুরো BEGIN...ROLLBACK ব্লক) local-persist করে — পরের ফিক্সচার
-- INSERT-গুলো (superuser দরকার, anon-এ INSERT গ্রান্ট নেই) চালানোর আগে role আবার
-- superuser-এ ফিরিয়ে আনা বাধ্যতামূলক, নাহলে "permission denied for table messages" এররে
-- transaction hard-abort হয়ে যায় (এই সেশনে real-run-এ প্রথম খসড়ায় এটাই ধরা পড়েছিল)।
RESET ROLE;

----------------------------------------------------------------------
-- ২) Broadcast SELECT policy ("problem participants can receive
--    problem-topic broadcasts") — ৬টা assertion, topic = 'problem:P_RLS_MSG1'
--    (নতুন কোনো INSERT না — section (১)-এর message-INSERT দুটোই
--    `broadcast_messages_changes` trigger দিয়ে ইতিমধ্যেই ২টা broadcast-row
--    তৈরি করে ফেলেছে, তাই প্রত্যাশিত visible-count ২, উপরের নোট দ্রষ্টব্য)
----------------------------------------------------------------------
SELECT test.set_topic('problem:P_RLS_MSG1');

SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG1'''),
  2::bigint,
  'broadcast policy: owner (CLIENT) নিজের problem-এর broadcast subscribe করতে পারে (trigger-generated ২টা রো-ই দেখা যায়)'
);
SELECT is(
  test.count_as('22222222-2222-2222-2222-222222222222',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG1'''),
  2::bigint,
  'broadcast policy: accepted_solver (SOLVER1, message-participant) subscribe করতে পারে'
);
SELECT is(
  test.count_as('33333333-3333-3333-3333-333333333333',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG1'''),
  0::bigint,
  '⭐ broadcast policy: অন্য solver (SOLVER2) এই problem-topic subscribe করতে পারে না (broadcast-লেয়ারে table SELECT-এর সাথে parity, regression-check)'
);
SELECT is(
  test.count_as('99999999-9999-9999-9999-999999999999',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG1'''),
  2::bigint,
  'broadcast policy: admin subscribe করতে পারে (is_admin() bypass)'
);
SELECT is(
  test.count_as('e1400001-0000-0000-0000-000000000001',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG1'''),
  0::bigint,
  'broadcast policy: সম্পূর্ণ অসম্পর্কিত user subscribe করতে পারে না'
);
SELECT test.logout();
SELECT is(
  (SELECT count(*) FROM realtime.messages WHERE topic = 'problem:P_RLS_MSG1'),
  0::bigint,
  'broadcast policy: anon subscribe করতে পারে না (policy শুধু "to authenticated")'
);
-- উপরের একই কারণে (RESET ROLE নোট দেখুন) — পরের ফিক্সচার INSERT-এর আগে role ফেরানো বাধ্যতামূলক।
RESET ROLE;

----------------------------------------------------------------------
-- ৩) বোনাস edge-case — is_admin() OR exists(...) গঠন: admin problem-এর
--    অস্তিত্ব ছাড়াই bypass করে, সাধারণ user করতে পারে না — policy-র
--    USING clause-এর গঠন থেকে সরাসরি ডিরাইভড, ২টা assertion
----------------------------------------------------------------------
INSERT INTO realtime.messages (topic, event, payload, private, extension)
VALUES ('problem:P_RLS_MSG_NOPROBLEM', 'messages_INSERT', '{}'::jsonb, true, 'broadcast');
-- ইচ্ছাকৃতভাবে 'P_RLS_MSG_NOPROBLEM' নামে কোনো public.problems row তৈরি করা
-- হয়নি — যাতে "exists(problem)" শর্ত সবসময় false হয়, শুধু is_admin() bypass-ই
-- pass করাতে পারে কিনা সেটা বিচ্ছিন্নভাবে পরীক্ষা করা যায়।

SELECT test.set_topic('problem:P_RLS_MSG_NOPROBLEM');

SELECT is(
  test.count_as('99999999-9999-9999-9999-999999999999',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG_NOPROBLEM'''),
  1::bigint,
  'broadcast policy: is_admin() bypass — problem না থাকলেও admin subscribe করতে পারে (USING clause গঠন অনুযায়ী প্রত্যাশিত)'
);
SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_MSG_NOPROBLEM'''),
  0::bigint,
  'broadcast policy: is_admin() না হলে অস্তিত্বহীন problem-topic-এ কেউ subscribe করতে পারে না'
);

SELECT * FROM finish();
ROLLBACK;
