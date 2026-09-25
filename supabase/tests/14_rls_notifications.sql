-- 14_rls_notifications.sql — Step 14.3 (RLS policy coverage: `notifications` টেবিল)
--
-- ✅ ফিক্সড সংস্করণ (আগের সেশনের "IN PROGRESS" খসড়ায় root cause ধরা পড়েছিল ও ভেরিফাই করা
-- হয়েছিল, বিস্তারিত CI_TEST_SUITE_PROGRESS.md-এর "Step 14.3 — IN PROGRESS" সেকশনে — এই
-- ফাইল সেই ভেরিফাইড ফিক্সটাই প্রয়োগ করে)। মূল বাগ ছিল: policy #৩-এর USING clause
-- (`topic = 'user:' || auth.uid()::text`) row-এর নিজের `topic` কলাম ছোঁয় না — শুধু সেশনের
-- `realtime.topic()` GUC (কোন topic-এ session subscribed আছে) চেক করে। প্রতিটা assertion-এর
-- আগে `SELECT test.set_topic('<target-topic>');` কল না করলে `realtime.topic()` সবসময় NULL
-- থাকে, ফলে policy-র প্রথম শর্তই সবসময় false হয়ে সব রো ব্লক হয়ে যায় (owner-এর নিজের
-- broadcast-ও দেখা যায় না) — 14_rls_messages.sql (14.2)-এ এটাই করা হয়েছিল, আগের খসড়ায়
-- ভুলে বাদ পড়েছিল। এই সংস্করণে প্রতিটা assertion-এর আগে set_topic() যোগ করা হয়েছে।
--
-- এছাড়াও: fixture-setup-এ `test.seed_rls_users()` নিজেই RLS User A/B-কে `public.users`-এ
-- INSERT করে, যেটা `broadcast_users_changes` trigger fire করিয়ে একই `'user:<uid>'` topic-এ
-- একটা `users_INSERT` broadcast-ও তৈরি করে ফেলে (আগের সেশনের root-cause নোটে চিহ্নিত)। তাই
-- fixture-setup-এর ঠিক পরে, notification INSERT করার **আগে**, `test.clear_broadcasts()` কল
-- করে সেই side-effect broadcast মুছে ফেলা হয়েছে (আগের সেশনের সুপারিশকৃত অপশন (ক) —
-- 13_trigger_*.sql-এর convention-এর সাথে সামঞ্জস্যপূর্ণ) — এর ফলে expected count প্রতিটা
-- ক্ষেত্রে পরিষ্কারভাবে ১ থাকে (শুধু notifications_INSERT broadcast, users_INSERT বাদে)।
--
-- ⚠️ স্কোপ-নোট (14_rls_00_helpers.sql-এর discovery + 14.2-এর HANDOFF থেকে, rule #5): কোনো
-- migration-এ `public.notifications`-এর জন্য কোনো table-level CREATE/ALTER POLICY নেই —
-- শুধু broadcast policy #৩ ("users can receive own user-topic broadcasts", realtime.messages,
-- topic='user:'||auth.uid()) আছে, যেটা notifications-সহ ৭টা টেবিলের broadcast trigger শেয়ার করে
-- (realtime_scoping_step1_notifications_broadcast.sql)। তাই এই ফাইল **শুধু broadcast-layer**
-- regression-check করে — table-level SELECT policy migrations-এ কোথাও define নেই বলে অনুমান করে
-- টেস্ট লেখা হয়নি (rule #5)। এটা messages-এর (14.2) থেকে গুণগতভাবে ভিন্ন স্কোপ, অসম্পূর্ণতা না।
--
-- ফাইল-নাম sort-order: `ls supabase/tests/*.sql | sort` দিয়ে manually confirm করা
-- হয়েছে — "14_rls_00_helpers.sql" < "14_rls_messages.sql" < "14_rls_notifications.sql"
-- ('0' < 'm' < 'n', 14.1-এর কনভেনশন অনুযায়ী, কোনো conflict নেই)।
--
-- 🔍 observation (migration বডি পড়ে ধরা পড়েছে): এই policy #৩-এর USING clause policy #২/#৪-এর
-- (messages/bids broadcast, 14.2-এ কভার করা) মতো `is_admin(...) OR exists(...)` গঠনের না —
-- শুধুই `topic = 'user:' || auth.uid()::text`, কোনো `is_admin()` bypass নেই। মানে **admin-ও
-- অন্য কারো notification-topic subscribe করতে পারে না এই policy দিয়ে** (messages/bids
-- broadcast policy-দুটোর থেকে সরাসরি ভিন্ন আচরণ) — নিচের assertion-এ এটাই মূল regression-check,
-- শুধু "user A নিজেরটা দেখে, user B-এরটা দেখে না" তার সাথে "admin-ও bypass করতে পারে না" যোগ
-- করা হলো যাতে এই পার্থক্যটা ভবিষ্যতে ভুলবশত is_admin() bypass যোগ হলে ধরা পড়ে।
--
-- notifications INSERT কলাম-প্যাটার্ন 13_trigger_notifications_bids.sql (Step 13.3, ইতিমধ্যে
-- verified) থেকে reuse করা হয়েছে — নতুন করে schema stub থেকে reverse-engineer করা হয়নি।
-- `broadcast_notifications_changes` trigger (Step 13.3-এ কভার করা) সক্রিয়, তাই 14.2-এর মতোই
-- notifications-এর নিজস্ব INSERT থেকেই trigger-generated broadcast-row ব্যবহার করা হয়েছে —
-- আলাদা ম্যানুয়াল simulated-broadcast-row বসানো হয়নি।
--
-- 🔧 grant-gap (14.2-এ ধরা পড়া প্যাটার্নের পুনরাবৃত্তি, একই ফিক্স): `realtime` schema/
-- `realtime.messages` টেবিলে anon/authenticated-কে কোনো migration-এ GRANT নেই — এই ফাইলের
-- নিজের `BEGIN;...ROLLBACK;`-এর ভেতরেই স্থানীয়ভাবে GRANT করা হয়েছে (অন্য কোনো ফাইলে leak করে না)।

BEGIN;

-- স্থানীয় grant (শুধু এই transaction-এর ভেতরে, অন্য কোনো ফাইলে leak করবে না)
GRANT USAGE ON SCHEMA realtime TO anon, authenticated;
GRANT SELECT ON realtime.messages TO anon, authenticated;

-- 14_rls_00_helpers.sql-এর "RLS-enablement gap" সেকশনের নির্দেশিত প্যাটার্ন — শুধু
-- realtime.messages লাগবে (এই ফাইলে public.notifications-এর কোনো table-level policy টেস্ট
-- করা হচ্ছে না, তাই public.notifications RLS enable করার দরকার নেই)।
ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;

SELECT plan(6);

SELECT test.seed_users();
-- CLIENT: 11111111-...   SOLVER1: 22222222-...   SOLVER2: 33333333-...   ADMIN: 99999999-...
SELECT test.seed_rls_users();
-- RLS User A (CLIENT-role): e1400001-...   RLS User B (SOLVER-role): e1400002-...

----------------------------------------------------------------------
-- seed_rls_users()-এর side-effect broadcast (users_INSERT trigger, একই 'user:<uid>' topic-এ)
-- মুছে ফেলা হলো, যাতে নিচের প্রতিটা assertion-এর expected count পরিষ্কারভাবে শুধু
-- notifications_INSERT broadcast-ই গোনে (root-cause নোটের সুপারিশকৃত অপশন (ক))।
----------------------------------------------------------------------
SELECT test.clear_broadcasts();

----------------------------------------------------------------------
-- ফিক্সচার — RLS User A-র নামে একটা notification, RLS User B-র নামে আরেকটা।
-- broadcast_notifications_changes trigger নিজে থেকেই 'user:<owner>' topic-এ broadcast করে।
----------------------------------------------------------------------
INSERT INTO public.notifications (id, user_id, title, message, target_type, role)
VALUES ('NOTIF_RLS_A', 'e1400001-0000-0000-0000-000000000001', 'Test Title A', 'Test Message A', 'SYSTEM', 'USER');

INSERT INTO public.notifications (id, user_id, title, message, target_type, role)
VALUES ('NOTIF_RLS_B', 'e1400002-0000-0000-0000-000000000002', 'Test Title B', 'Test Message B', 'SYSTEM', 'USER');

----------------------------------------------------------------------
-- broadcast SELECT policy ("users can receive own user-topic broadcasts") — ৬টা assertion
-- প্রতিটা assertion-এর আগে target topic-এর জন্য test.set_topic() কল বাধ্যতামূলক (মূল ফিক্স —
-- উপরের হেডার-কমেন্ট ও 14_rls_messages.sql-এর প্যাটার্ন দ্রষ্টব্য)।
----------------------------------------------------------------------
SELECT test.set_topic('user:e1400001-0000-0000-0000-000000000001');
SELECT is(
  test.count_as('e1400001-0000-0000-0000-000000000001',
    'select count(*) from realtime.messages where topic = ''user:e1400001-0000-0000-0000-000000000001'''),
  1::bigint,
  'notifications broadcast policy: owner (RLS User A) নিজের user-topic subscribe করে নিজের notification broadcast দেখতে পারে'
);

SELECT test.set_topic('user:e1400002-0000-0000-0000-000000000002');
SELECT is(
  test.count_as('e1400001-0000-0000-0000-000000000001',
    'select count(*) from realtime.messages where topic = ''user:e1400002-0000-0000-0000-000000000002'''),
  0::bigint,
  '⭐ notifications broadcast policy: RLS User A অন্য user (RLS User B)-এর topic subscribe করতে পারে না (per-user isolation regression-check)'
);

SELECT test.set_topic('user:e1400002-0000-0000-0000-000000000002');
SELECT is(
  test.count_as('e1400002-0000-0000-0000-000000000002',
    'select count(*) from realtime.messages where topic = ''user:e1400002-0000-0000-0000-000000000002'''),
  1::bigint,
  'notifications broadcast policy: owner (RLS User B) নিজের user-topic subscribe করে নিজের notification broadcast দেখতে পারে'
);

SELECT test.set_topic('user:e1400001-0000-0000-0000-000000000001');
SELECT is(
  test.count_as('99999999-9999-9999-9999-999999999999',
    'select count(*) from realtime.messages where topic = ''user:e1400001-0000-0000-0000-000000000001'''),
  0::bigint,
  '⭐ notifications broadcast policy: admin-ও অন্যের user-topic subscribe করতে পারে না — এই policy-তে কোনো is_admin() bypass নেই (messages/bids broadcast policy থেকে ভিন্ন, regression-check)'
);

SELECT test.set_topic('user:e1400001-0000-0000-0000-000000000001');
SELECT is(
  test.count_as('11111111-1111-1111-1111-111111111111',
    'select count(*) from realtime.messages where topic = ''user:e1400001-0000-0000-0000-000000000001'''),
  0::bigint,
  'notifications broadcast policy: সম্পূর্ণ অসম্পর্কিত user (owner না) কোনো notification broadcast দেখতে পারে না'
);

SELECT test.set_topic('user:e1400001-0000-0000-0000-000000000001');
SELECT test.logout();
SELECT is(
  (SELECT count(*) FROM realtime.messages WHERE topic = 'user:e1400001-0000-0000-0000-000000000001'),
  0::bigint,
  'notifications broadcast policy: anon (login ছাড়া) কোনো notification broadcast দেখতে পারে না (policy শুধু "to authenticated")'
);
-- ⚠️ test.logout()-এর পরে role='anon' পুরো transaction জুড়ে local-persist করে (14.2-এ ধরা পড়া
-- একই কারণ) — কিন্তু এখানে এটাই শেষ statement (finish()/ROLLBACK ছাড়া আর কোনো privileged
-- অপারেশন নেই এই ফাইলে), তাই RESET ROLE বাধ্যতামূলক না — নিরাপত্তার জন্য তবুও যোগ করা হলো।
RESET ROLE;

SELECT * FROM finish();
ROLLBACK;
