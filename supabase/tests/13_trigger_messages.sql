-- 13_trigger_messages.sql — Step 13.6 (Database trigger coverage, সর্বোচ্চ অগ্রাধিকার)
--
-- 13_trigger_helpers.sql-এর helper (test.clear_broadcasts/broadcast_count/
-- broadcast_count_topic/last_broadcast_payload) ব্যবহার করে `broadcast_messages_changes`-এর
-- পূর্ণাঙ্গ কভারেজ + **মূল উদ্দেশ্য: multi-update loop-check** — chat/profile screen-এ রিপোর্ট
-- করা "শুধু শিমার লোড হতেই থাকে" বাগের সন্দেহভাজন root cause যাচাই। একই row পরপর n বার আপডেট
-- করে broadcast ঠিক n বার fire হচ্ছে কিনা (loop-এ আটকে n-এর বেশি fire হচ্ছে না তো), এটাই এই
-- ফাইলের কেন্দ্রীয় assertion — শুধু coverage-এর জন্য না।
--
-- `broadcast_users_changes`-এর জন্যও একই loop-check এই সেশনে চালানো হয়েছে, কিন্তু
-- ডুপ্লিকেট ফাইল না করে **13_trigger_users_escrows.sql-এই যোগ করা হয়েছে** (users-এর
-- বাকি সব টেস্ট যেখানে আছে) — master prompt Step 13.6 নির্দেশ অনুযায়ী।
--
-- ফাইল-নাম sort-order: 'messages' ('m') 'helpers' ('h')-এর পরে, 'notifications_bids' ('n')-এর
-- আগে — `ls supabase/tests/*.sql | sort` দিয়ে এই সেশনে manually confirm করা হয়েছে
-- (13.5-এর সতর্কতা-নিয়ম অনুযায়ী)।
--
-- ⚠️ **environment নোট (এই সেশন):** sandbox network/apt এই সেশনে কাজ করেনি (apt-get update/
-- install দুটোই 403 Forbidden দিয়েছে, আগের সেশনগুলোর মতো real psql+pgTAP ইনস্টল করা যায়নি)।
-- তাই এই ফাইল **static-only** যাচাই করে লেখা হয়েছে: migration বডি (নিচে দেখুন) থেকে সরাসরি
-- topic/event/payload-logic নিশ্চিত করে, আর SQL syntax/braces/pgTAP plan-count ম্যানুয়ালি
-- কয়েকবার পড়ে verify করা হয়েছে (rule #5 অনুযায়ী কোনো column/logic অনুমান করা হয়নি — সবই
-- migration ফাইল থেকে সরাসরি)। **real run পরের সেশনে (network কাজ করলে) বা GitHub Actions-এ
-- হওয়া উচিত** — এই সীমাবদ্ধতা rule #6-এর মতোই স্পষ্ট করে রাখা হলো।
--
-- migration বডি (`supabase/migrations/realtime_scoping_step3_messages_broadcast.sql`) থেকে
-- confirmed: topic = 'problem:'||coalesce(NEW.problem_id, OLD.problem_id) (resource-based, single
-- call — owner+accepted_solver উভয়েই একই topic থেকে পায়, dual-broadcast লাগে না), event =
-- 'messages_'||TG_OP (শুরু থেকেই table-prefixed, কোনো bare-TG_OP বাগ কখনো ছিল না এই ফাংশনে)।
--
-- ⚠️ scope note (13_trigger_users_escrows.sql-এর মতোই): এই ফাইল শুধু trigger fire mechanics +
-- payload + loop-behavior যাচাই করে। topic-এ কে subscribe করতে পারবে (RLS,
-- "problem participants can receive problem-topic broadcasts") সেটা Step 14-এর বিষয়।

BEGIN;
SELECT plan(10);

SELECT test.seed_users();
-- CLIENT: 11111111-...   SOLVER1: 22222222-...   SOLVER2: 33333333-...   ADMIN: 99999999-...

----------------------------------------------------------------------
-- broadcast_messages_changes — dedicated fixture problem (seed_users() টাচ করা হয়নি,
-- একটা নতুন problem+message fixture ব্যবহার করা হচ্ছে যাতে অন্য কোনো টেস্টের broadcast এই
-- টেস্টের count নষ্ট না করে)
----------------------------------------------------------------------
SELECT test.clear_broadcasts();

INSERT INTO public.problems (id, user_id, accepted_solver_id, title, status)
VALUES ('P_MSG_FIXTURE', '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222', 'Messages Trigger Fixture Problem', 'IN_PROGRESS');

INSERT INTO public.messages (id, problem_id, sender_id, receiver_id, sender_name, content)
VALUES ('MSG_TRIGGER_1', 'P_MSG_FIXTURE',
        '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222',
        'Test Client', 'Hello, first message');

SELECT is(
  test.broadcast_count('problem:P_MSG_FIXTURE', 'messages_INSERT'),
  1::bigint,
  'broadcast_messages_changes: INSERT-এ ঠিক ১বার fire করে, event=messages_INSERT'
);

-- এজ-কেস: ভুল/অসম্পর্কিত problem-topic-এ কোনো broadcast leak করেনি
SELECT is(
  test.broadcast_count_topic('problem:P_MSG_FIXTURE_UNRELATED'),
  0::bigint,
  'broadcast_messages_changes: অসম্পর্কিত problem-topic-এ কোনো broadcast leak করেনি'
);

SELECT is(
  test.last_broadcast_payload('problem:P_MSG_FIXTURE', 'messages_INSERT') -> 'record' ->> 'content',
  'Hello, first message',
  'broadcast_messages_changes: INSERT payload->record->content সঠিক মান দেখায়'
);

UPDATE public.messages SET content = 'Hello, edited message', is_read = true
WHERE id = 'MSG_TRIGGER_1';

SELECT is(
  test.broadcast_count('problem:P_MSG_FIXTURE', 'messages_UPDATE'),
  1::bigint,
  'broadcast_messages_changes: single UPDATE-এ ঠিক ১বার fire করে, event=messages_UPDATE'
);
SELECT is(
  test.last_broadcast_payload('problem:P_MSG_FIXTURE', 'messages_UPDATE') -> 'record' ->> 'content',
  'Hello, edited message',
  'broadcast_messages_changes: UPDATE payload->record->content আপডেট-পরবর্তী মান দেখায়'
);

DELETE FROM public.messages WHERE id = 'MSG_TRIGGER_1';

SELECT is(
  test.broadcast_count('problem:P_MSG_FIXTURE', 'messages_DELETE'),
  1::bigint,
  'broadcast_messages_changes: DELETE-এ ঠিক ১বার fire করে, event=messages_DELETE'
);
SELECT is(
  test.last_broadcast_payload('problem:P_MSG_FIXTURE', 'messages_DELETE') -> 'old_record' ->> 'id',
  'MSG_TRIGGER_1',
  'broadcast_messages_changes: DELETE payload->old_record->id মোছা message-র id দেখায়'
);

-- এজ-কেস: তিনটা op মিলিয়ে ওই topic-এ মোট ঠিক ৩টাই broadcast — কোনো duplicate/extra fire নেই
SELECT is(
  test.broadcast_count_topic('problem:P_MSG_FIXTURE'),
  3::bigint,
  'broadcast_messages_changes: INSERT+UPDATE+DELETE মিলিয়ে মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

----------------------------------------------------------------------
-- ⭐ মূল assertion — multi-update loop-check (shimmer-লুপ বাগের সন্দেহভাজন কারণ)
-- একই message row পরপর ৫বার আপডেট করে broadcast ঠিক ৫বারই fire হচ্ছে কিনা যাচাই করা হচ্ছে —
-- trigger নিজে যদি কোনো loop/re-trigger-এ আটকায় (যেমন trigger-এর ভেতরেই একই টেবিলে আরেকটা
-- UPDATE চালিয়ে দেয়, বা কোনো cascading trigger থাকে), তাহলে count ৫-এর বেশি হবে। migration
-- বডি অনুযায়ী trigger শুধু AFTER, `return null` করে, কোনো recursive UPDATE নেই — তাই প্রত্যাশা
-- হলো ঠিক n = broadcast fire, কোনো loop না। এই টেস্টই সেই প্রত্যাশা প্রমাণ/খণ্ডন করবে।
----------------------------------------------------------------------
INSERT INTO public.problems (id, user_id, accepted_solver_id, title, status)
VALUES ('P_MSG_LOOP', '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222', 'Messages Loop-Check Problem', 'IN_PROGRESS');

INSERT INTO public.messages (id, problem_id, sender_id, receiver_id, sender_name, content)
VALUES ('MSG_LOOP_1', 'P_MSG_LOOP',
        '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222',
        'Test Client', 'Loop-check initial content');

-- INSERT-এর broadcast বাদ দিয়ে শুধু পরের n-টা UPDATE-এর broadcast আলাদাভাবে গোনার জন্য clear
SELECT test.clear_broadcasts();

DO $$
DECLARE
  i integer;
BEGIN
  FOR i IN 1..5 LOOP
    UPDATE public.messages
    SET content = 'Loop-check update #' || i
    WHERE id = 'MSG_LOOP_1';
  END LOOP;
END;
$$;

SELECT is(
  test.broadcast_count('problem:P_MSG_LOOP', 'messages_UPDATE'),
  5::bigint,
  'broadcast_messages_changes: একই row-এ পরপর ৫টা UPDATE-এ broadcast ঠিক ৫বারই fire করে (loop/extra-fire ধরা পড়েনি)'
);
SELECT is(
  test.broadcast_count_topic('problem:P_MSG_LOOP'),
  5::bigint,
  'broadcast_messages_changes: loop-check-এর সময় ওই topic-এ মোট broadcast-ও ঠিক ৫টা (অন্য কোনো event/extra fire নেই)'
);

SELECT * FROM finish();
ROLLBACK;
