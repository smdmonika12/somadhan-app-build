-- 14_rls_bids.sql — Step 14.4 (RLS policy coverage: `bids` broadcast policy + Step 14 wiring/সারাংশ)
--
-- 14_rls_00_helpers.sql-এর discovery/helper ব্যবহার করে policy #৪ ("problem bids
-- visibility broadcasts", realtime.messages, topic LIKE 'problem:%:bids') কভার করে —
-- এটাই Step 14-এর formal "৫টা CREATE POLICY" (৪টা distinct) তালিকার শেষ/৪র্থ বাকি
-- আইটেম। 14.1/14.2/14.3 policy #১/#২/#৩ কভার করেছে (messages table+broadcast,
-- notifications broadcast) — এই ফাইলের পর সব ৪টা distinct policy কভারড।
--
-- ফাইল-নাম sort-order: `ls supabase/tests/*.sql | sort` দিয়ে এই সেশনে manually
-- confirm করা হয়েছে — "14_rls_00_helpers.sql" < "14_rls_bids.sql" < "14_rls_messages.sql"
-- < "14_rls_notifications.sql" (14.1-এর "00_" কনভেনশন অনুযায়ী, helpers সবসময় আগে
-- পড়ে, বাকিগুলোর আপেক্ষিক ক্রম কোনো functional নির্ভরতা তৈরি করে না কারণ প্রতিটা
-- test file নিজের `BEGIN;...ROLLBACK;`-এ স্বয়ংসম্পূর্ণ)।
--
-- ⚠️ স্কোপ-সিদ্ধান্ত (14.1-এর HANDOFF-এ এই সেশনের জন্য রাখা হয়েছিল): `bids_select`
-- (public.bids-এর table-level SELECT policy, `step23_bids_select_open_public_visibility.sql`-এ
-- শুধু ALTER, কোনো migration-এ কখনো CREATE POLICY হয়নি) এই ফাইলের স্কোপের **বাইরে**
-- রাখা হলো — messages_insert-এর (14.2-এর স্কোপ-নোট) হুবহু একই কারণে: Step 14-এর
-- formal সংজ্ঞা (master prompt-এ "৫টা CREATE POLICY statement") অনুযায়ী শুধু CREATE
-- POLICY হওয়া policy-গুলোই এই ধাপের বাধ্যতামূলক scope, আর bids_select কখনো CREATE
-- হয়নি (শুধু ALTER, rule #6-এর মতোই একটা schema/policy gap — 14.1-এর "বোনাস
-- আবিষ্কার" সেকশনে documented)। এটা কভার করতে rule #6-প্যাটার্নের একটা নতুন
-- inferred `CREATE POLICY bids_select` schema-stub লাগবে যেটা এই single-step
-- সেশনের bounded-scope নীতির (rule #3, "একবারে শুধু একটা ধাপ") বাইরে চলে যায় —
-- তাই **backlog হিসেবে নোট করা হলো** (নিচের "Step 14 চূড়ান্ত সারাংশ" সেকশনে আবার
-- উল্লেখ করা হয়েছে), করা হলো না। একই যুক্তিতে `problems_select`ও (bids_select-এর
-- OR-শর্ত problems ছোঁয় বলে সম্পর্কিত) এই ফাইলের স্কোপে নেই।
--
-- 🔍 broadcast policy #৪-এর USING clause-এ তিনটা ভিন্ন allow-branch আছে (messages/
-- notifications-এর policy #২/#৩-এর চেয়ে বেশি জটিল, তাই bids-এর টেস্ট মেসেজের
-- ৬-assertion প্যাটার্নের সাথে একটা অতিরিক্ত সেকশন যোগ করা হয়েছে):
--   (ক) is_admin(auth.uid()) — bypass
--   (খ) problem owner (p.user_id = auth.uid())
--   (গ) সেই problem-এ bid থাকা solver (EXISTS বিডস b.solver_id = auth.uid())
--   (ঘ) problem status='OPEN' AND is_public=true AND is_user_deleted=false —
--       Firebase-parity সিদ্ধান্ত অনুযায়ী **যেকোনো** authenticated user, সম্পূর্ণ
--       অসম্পর্কিত হলেও। এই ৪র্থ branch-টাই messages/notifications policy-তে নেই,
--       তাই এটাই bids-নির্দিষ্ট সবচেয়ে গুরুত্বপূর্ণ regression-surface: (i) branch
--       (ঘ) সত্যিই কাজ করে কিনা (unrelated user OPEN+public problem-এ ঢুকতে পারে),
--       আর (ii) branch (ঘ)-এর `is_public=false` শর্তটা সত্যিই honored হয় কিনা
--       (private problem হলে OPEN থাকা সত্ত্বেও unrelated user ব্লক থাকা উচিত —
--       নাহলে সেটাই একটা privacy leak হতো)।
--
-- 🔧 grant-gap/RLS-enablement — 14.2/14.3-এর হুবহু একই প্যাটার্ন (14_rls_00_helpers.sql-এর
-- "RLS-enablement gap" সেকশন) — `realtime.messages`-এ GRANT নেই কোনো migration-এ, আর
-- RLS enable-ও নেই — তাই এই ফাইলের নিজের `BEGIN;...ROLLBACK;`-এর ভেতরেই local করা হলো।
-- `public.bids`/`public.problems`-এ RLS enable করার দরকার নেই এই ফাইলে (শুধু broadcast-লেয়ার
-- policy #৪ টেস্ট হচ্ছে, table-level `bids_select` না — উপরের স্কোপ-নোট দ্রষ্টব্য)।

BEGIN;

-- স্থানীয় grant (শুধু এই transaction-এর ভেতরে, অন্য কোনো ফাইলে leak করবে না)
GRANT USAGE ON SCHEMA realtime TO anon, authenticated;
GRANT SELECT ON realtime.messages TO anon, authenticated;

ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;

SELECT plan(10);

SELECT test.seed_users();
-- CLIENT: 11111111-...   SOLVER1: 22222222-...   SOLVER2: 33333333-...   ADMIN: 99999999-...
SELECT test.seed_rls_users();
-- RLS User A (CLIENT-role, সম্পূর্ণ অসম্পর্কিত): e1400001-...
-- RLS User B (SOLVER-role, সম্পূর্ণ অসম্পর্কিত): e1400002-...

----------------------------------------------------------------------
-- ফিক্সচার ১ — P_RLS_BIDS1: status='IN_PROGRESS' (OPEN না, ইচ্ছাকৃতভাবে branch (ঘ)
-- বন্ধ রাখতে), owner=CLIENT, SOLVER1-এর একটা bid আছে, SOLVER2-এর কোনো bid নেই।
-- এভাবে branch (খ)/(গ) বিচ্ছিন্নভাবে টেস্ট করা যায়, branch (ঘ)-এর হস্তক্ষেপ ছাড়াই।
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public, is_user_deleted)
VALUES ('P_RLS_BIDS1', '11111111-1111-1111-1111-111111111111',
        'RLS Bids Owner/Solver Fixture', 'IN_PROGRESS', true, false);

SELECT test.clear_broadcasts();  -- উপরের problem-INSERT-এর broadcast বাদ দিয়ে শুরু

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('BID_RLS_1', 'P_RLS_BIDS1', '22222222-2222-2222-2222-222222222222',
        'Test Solver One', 500, 'PENDING');
-- ↑ এই INSERT নিজেই broadcast_bids_changes trigger fire করে (Step 13.3-এ কভার করা),
-- topic='problem:P_RLS_BIDS1:bids', event='bids_INSERT' — ম্যানুয়াল broadcast-insert লাগবে না
-- (14.2-এর messages-test-এ ধরা পড়া "trigger ইতিমধ্যেই সক্রিয়" observation এখানেও প্রযোজ্য)।

SELECT test.set_topic('problem:P_RLS_BIDS1:bids');

SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS1:bids'''),
  1::bigint,
  'bids broadcast: problem owner (CLIENT) নিজের problem-এর bids-topic subscribe করতে পারে'
);
SELECT is(
  test.count_as('22222222-2222-2222-2222-222222222222',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS1:bids'''),
  1::bigint,
  'bids broadcast: এই problem-এ bid দেওয়া solver (SOLVER1) subscribe করতে পারে'
);
SELECT is(
  test.count_as('33333333-3333-3333-3333-333333333333',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS1:bids'''),
  0::bigint,
  '⭐ bids broadcast: না owner না bid-দাতা solver (SOLVER2), আর problem OPEN+public-ও না — subscribe করতে পারে না'
);
SELECT is(
  test.count_as('99999999-9999-9999-9999-999999999999',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS1:bids'''),
  1::bigint,
  'bids broadcast: admin subscribe করতে পারে (is_admin() bypass)'
);
SELECT is(
  test.count_as('e1400001-0000-0000-0000-000000000001',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS1:bids'''),
  0::bigint,
  'bids broadcast: সম্পূর্ণ অসম্পর্কিত user (owner/solver-with-bid/admin কিছুই না) subscribe করতে পারে না'
);
SELECT test.logout();
SELECT is(
  (SELECT count(*) FROM realtime.messages WHERE topic = 'problem:P_RLS_BIDS1:bids'),
  0::bigint,
  'bids broadcast: anon (login ছাড়া) subscribe করতে পারে না (policy শুধু "to authenticated")'
);
-- পরের ফিক্সচার INSERT-এর আগে role ফেরানো বাধ্যতামূলক (14.2/14.3-এর একই RESET ROLE নোট)।
RESET ROLE;

----------------------------------------------------------------------
-- ফিক্সচার ২ — P_RLS_BIDS2: status='OPEN', is_public=true (branch (ঘ) — Firebase-parity
-- broad visibility)। owner/bid-দাতা কেউই e1400001 না, তবু OPEN+public হওয়ায় দেখতে
-- পারার কথা — এটাই bids policy-র messages/notifications থেকে আলাদা করা মূল feature।
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public, is_user_deleted)
VALUES ('P_RLS_BIDS2', 'e1400002-0000-0000-0000-000000000002',
        'RLS Bids OPEN+Public Fixture', 'OPEN', true, false);

SELECT test.clear_broadcasts();

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('BID_RLS_2', 'P_RLS_BIDS2', '22222222-2222-2222-2222-222222222222',
        'Test Solver One', 300, 'PENDING');

SELECT test.set_topic('problem:P_RLS_BIDS2:bids');

SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS2:bids'''),
  1::bigint,
  '⭐ bids broadcast: সম্পূর্ণ অসম্পর্কিত authenticated user (না owner, না bid-দাতা) OPEN+public problem-এর bids-topic subscribe করতে পারে (Firebase-parity branch (ঘ))'
);
RESET ROLE;

----------------------------------------------------------------------
-- ফিক্সচার ৩ — P_RLS_BIDS3: status='OPEN' কিন্তু is_public=**false** (private-OPEN
-- problem)। এজ-কেস: শুধু "status='OPEN'" যথেষ্ট না, is_public=true-ও লাগে — এই
-- privacy-boundary সত্যিই honored হচ্ছে কিনা এটাই এখানকার regression-check।
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, title, status, is_public, is_user_deleted)
VALUES ('P_RLS_BIDS3', 'e1400002-0000-0000-0000-000000000002',
        'RLS Bids OPEN-but-Private Fixture', 'OPEN', false, false);

SELECT test.clear_broadcasts();

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('BID_RLS_3', 'P_RLS_BIDS3', '33333333-3333-3333-3333-333333333333',
        'Test Solver Two', 300, 'PENDING');

SELECT test.set_topic('problem:P_RLS_BIDS3:bids');

SELECT is(
  test.count_as('e1400001-0000-0000-0000-000000000001',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS3:bids'''),
  0::bigint,
  '⭐ bids broadcast: OPEN কিন্তু is_public=false (private) problem-এ অসম্পর্কিত user subscribe করতে পারে না (branch (ঘ)-এর is_public শর্ত সত্যিই honored হয়, শুধু status=OPEN যথেষ্ট না)'
);
RESET ROLE;

----------------------------------------------------------------------
-- বোনাস edge-case — is_admin() OR exists(...) গঠন: admin problem-এর অস্তিত্ব ছাড়াই
-- bypass করে, সাধারণ user করতে পারে না (14.2-এর messages বোনাস edge-case-এর
-- হুবহু বিড-সংস্করণ) — ২টা assertion
----------------------------------------------------------------------
INSERT INTO realtime.messages (topic, event, payload, private, extension)
VALUES ('problem:P_RLS_BIDS_NOPROBLEM:bids', 'bids_INSERT', '{}'::jsonb, true, 'broadcast');
-- ইচ্ছাকৃতভাবে 'P_RLS_BIDS_NOPROBLEM' নামে কোনো public.problems row নেই — যাতে
-- branch (খ)/(গ)/(ঘ) তিনটাই সবসময় false হয়, শুধু is_admin() bypass-ই pass করাতে
-- পারে কিনা সেটা বিচ্ছিন্নভাবে পরীক্ষা করা যায়।

SELECT test.set_topic('problem:P_RLS_BIDS_NOPROBLEM:bids');

SELECT is(
  test.count_as('99999999-9999-9999-9999-999999999999',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS_NOPROBLEM:bids'''),
  1::bigint,
  'bids broadcast: is_admin() bypass — problem না থাকলেও admin subscribe করতে পারে (USING clause গঠন অনুযায়ী প্রত্যাশিত)'
);
SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from realtime.messages where topic = ''problem:P_RLS_BIDS_NOPROBLEM:bids'''),
  0::bigint,
  'bids broadcast: is_admin() না হলে অস্তিত্বহীন problem-topic-এ কেউ subscribe করতে পারে না'
);

SELECT * FROM finish();
ROLLBACK;
