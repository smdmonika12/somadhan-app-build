-- 13_trigger_notifications_bids.sql — Step 13.3 (Database trigger coverage), আপডেট করা হয়েছে
-- Step 13.3→13.4-এর মধ্যবর্তী সেশনে (২০২৬-০৯-২২, ব্যবহারকারীর স্পষ্ট অনুমতিতে notifications-fix)
--
-- 13_trigger_helpers.sql-এর helper (test.clear_broadcasts/broadcast_count/
-- broadcast_count_topic/last_broadcast_payload) ব্যবহার করে `broadcast_notifications_changes`
-- ও `broadcast_bids_changes` — এই দুটো trigger-এর কভারেজ, 13_trigger_users_escrows.sql
-- (Step 13.2)-এর প্যাটার্ন অনুসরণ করে।
--
-- ✅ event নাম — notifications বনাম bids (দুটোই এখন সঠিক/প্রিফিক্সড):
--   bids          : 'bids_'||TG_OP  (শুরু থেকেই সঠিক, কোনো collision-বাগ কখনো ছিল না —
--                   migration বডি `realtime_scoping_step4_bids_broadcast.sql`-এ verify করা)
--   notifications : 'notifications_'||TG_OP (✅ ফিক্সড এই সেশনে — আগে বেয়ার TG_OP ছিল)
--
-- 🔴→✅ ইতিহাস: Step 13.3-এ ধরা পড়েছিল `notify_notifications_broadcast()` bare TG_OP
-- ('INSERT'/'UPDATE'/'DELETE') ব্যবহার করছে, আর এটা **live-এও বিদ্যমান** ছিল (users-এর
-- আগের বাগের মতো শুধু CI-reconstruction-order সমস্যা না) — client-side Kotlin কোডও তখন bare
-- event-ই subscribe করছিল, তাই দুই পাশ সামঞ্জস্যপূর্ণ থেকে app-এ কোনো active bug ছিল না, শুধু
-- ভবিষ্যতের জন্য একটা residual collision-ঝুঁকি ছিল (users-এর পুরনো বাগের মতোই প্যাটার্ন)।
--
-- ব্যবহারকারী স্পষ্ট অনুমতি দেওয়ার পর ("Tumi ei fix suru koro") এই সেশনেই দুই জায়গাতেই ফিক্স
-- করা হয়েছে:
--   1. DB migration `supabase/migrations/zz_20260913085320_notifications_broadcast_event_naming_fix.sql`
--      — **live-এ সরাসরি apply করা হয়েছে** (mcp__Supabase__apply_migration, project
--      mghvvpndkxnscwryfkib) — users-এর CI-only ফিক্সের থেকে ভিন্ন, কারণ এখানে বাগটা live-এও
--      ছিল। live md5 যাচাই করে কনফার্ম করা হয়েছে (নিচে)।
--   2. Kotlin: `SupabaseRealtimeManager.kt`-এর `startUserTopicBroadcastSubscription()`-এ
--      notifications-এর event এখন `"notifications_$op"` (আগে bare `op`)।
-- দুটো একসাথে বদলানো হয়েছে বলে breaking change হয়নি (client নতুন prefixed event-ই শুনছে,
-- server নতুন prefixed event-ই পাঠাচ্ছে)।
--
-- নিচের assertion-গুলো তাই এখন **fixed/প্রিফিক্সড event নাম** ধরেই লেখা।

BEGIN;
SELECT plan(17);

SELECT test.seed_users();

----------------------------------------------------------------------
-- broadcast_notifications_changes — dedicated fixture user, single-owner topic
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
VALUES ('e3000001-0000-0000-0000-000000000001', 'CLIENT', 'Notif-Trigger Fixture', '01711110004', 0, 0, true);

SELECT test.clear_broadcasts();  -- উপরের fixture-user INSERT-এর broadcast বাদ দিয়ে শুরু

INSERT INTO public.notifications (id, user_id, title, message, target_type, role)
VALUES ('NOTIF_TRIGGER_1', 'e3000001-0000-0000-0000-000000000001', 'Test Title', 'Test Message', 'PROBLEM', 'USER');

SELECT is(
  test.broadcast_count('user:e3000001-0000-0000-0000-000000000001', 'notifications_INSERT'),
  1::bigint,
  'broadcast_notifications_changes: INSERT-এ ঠিক ১বার fire করে, event=notifications_INSERT (✅ ফিক্সের পর)'
);

-- এজ-কেস: ভুল/অসম্পর্কিত topic-এ কোনো broadcast leak করেনি
SELECT is(
  test.broadcast_count_topic('user:e3000001-0000-0000-0000-000000000099'),
  0::bigint,
  'broadcast_notifications_changes: ভুল/অসম্পর্কিত topic-এ কোনো broadcast leak করেনি'
);

UPDATE public.notifications SET message = 'Updated Message'
WHERE id = 'NOTIF_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e3000001-0000-0000-0000-000000000001', 'notifications_UPDATE'),
  1::bigint,
  'broadcast_notifications_changes: UPDATE-এ ঠিক ১বার fire করে, event=notifications_UPDATE'
);
SELECT is(
  test.last_broadcast_payload('user:e3000001-0000-0000-0000-000000000001', 'notifications_UPDATE') -> 'record' ->> 'message',
  'Updated Message',
  'broadcast_notifications_changes: UPDATE payload->record->message আপডেট-পরবর্তী মান দেখায়'
);

DELETE FROM public.notifications WHERE id = 'NOTIF_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e3000001-0000-0000-0000-000000000001', 'notifications_DELETE'),
  1::bigint,
  'broadcast_notifications_changes: DELETE-এ ঠিক ১বার fire করে, event=notifications_DELETE'
);

-- এজ-কেস: তিনটা op মিলিয়ে ওই topic-এ মোট ঠিক ৩টাই broadcast (duplicate fire নেই)
SELECT is(
  test.broadcast_count_topic('user:e3000001-0000-0000-0000-000000000001'),
  3::bigint,
  'broadcast_notifications_changes: INSERT+UPDATE+DELETE মিলিয়ে মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

----------------------------------------------------------------------
-- ✅ regression check — bare (unprefixed) event আর কখনো fire হচ্ছে না, ফিক্স স্থায়ীভাবে কাজ
-- করছে কিনা এটাই মূল assertion (আগে এই ব্লকটাই "collision-risk demonstration" ছিল, ফিক্সের
-- পর এখন "fix সত্যিই কাজ করছে" রিগ্রেশন-চেক হিসেবে রাখা হলো — ডুপ্লিকেট না, ভিন্ন উদ্দেশ্য)।
----------------------------------------------------------------------
SELECT test.clear_broadcasts();

INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
VALUES ('e3000002-0000-0000-0000-000000000002', 'CLIENT', 'Notif-Regression Fixture', '01711110005', 0, 0, true);
-- ↑ এই INSERT নিজেই broadcast_users_changes ফায়ার করে, event='users_INSERT'

INSERT INTO public.notifications (id, user_id, title, message, target_type, role)
VALUES ('NOTIF_TRIGGER_2', 'e3000002-0000-0000-0000-000000000002', 'Welcome', 'Welcome message', 'SYSTEM', 'USER');
-- ↑ এই INSERT broadcast_notifications_changes ফায়ার করে, event='notifications_INSERT' (✅ ফিক্সড)

SELECT is(
  test.broadcast_count('user:e3000002-0000-0000-0000-000000000002', 'users_INSERT'),
  1::bigint,
  'regression: users INSERT প্রিফিক্সড event (users_INSERT) হিসেবে আলাদাভাবে গণনা হয়'
);
SELECT is(
  test.broadcast_count('user:e3000002-0000-0000-0000-000000000002', 'notifications_INSERT'),
  1::bigint,
  'regression: notifications INSERT এখন প্রিফিক্সড event (notifications_INSERT) হিসেবে গণনা হয়'
);
SELECT is(
  test.broadcast_count('user:e3000002-0000-0000-0000-000000000002', 'INSERT'),
  0::bigint,
  'regression: bare/unprefixed ''INSERT'' event আর কখনো fire হচ্ছে না (ফিক্সের আগে notifications এই নামেই পাঠাত)'
);
SELECT is(
  test.broadcast_count_topic('user:e3000002-0000-0000-0000-000000000002'),
  2::bigint,
  'regression: একই topic-এ দুটো ভিন্ন প্রিফিক্সড broadcast (users_INSERT + notifications_INSERT) সঠিকভাবে আলাদা গণনা হয়, কোনো bare-event leftover নেই'
);

----------------------------------------------------------------------
-- broadcast_bids_changes — resource-based topic (problem:<id>:bids), সঠিকভাবে প্রিফিক্সড
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role, has_solver_role)
VALUES
  ('e4000001-0000-0000-0000-000000000001', 'CLIENT', 'Bid Owner Fixture', '01711110006', 0, 0, true, false),
  ('e4000002-0000-0000-0000-000000000002', 'SOLVER', 'Bid Solver Fixture', '01711110007', 0, 0, false, true);

INSERT INTO public.problems (id, user_id, title, status)
VALUES ('P_E4_FIXTURE', 'e4000001-0000-0000-0000-000000000001', 'Bid Trigger Fixture Problem', 'OPEN');

SELECT test.clear_broadcasts();  -- উপরের user/problem INSERT-broadcast বাদ দিয়ে শুরু

INSERT INTO public.bids (id, problem_id, solver_id, solver_name, amount, status)
VALUES ('BID_TRIGGER_1', 'P_E4_FIXTURE', 'e4000002-0000-0000-0000-000000000002', 'Bid Solver Fixture', 750.00, 'PENDING');

SELECT is(
  test.broadcast_count('problem:P_E4_FIXTURE:bids', 'bids_INSERT'),
  1::bigint,
  'broadcast_bids_changes: INSERT-এ ঠিক ১বার fire করে, event=bids_INSERT'
);

-- এজ-কেস: bids-এর topic messages-এর topic (problem:<id>, :bids ছাড়া) থেকে ইচ্ছাকৃতভাবে
-- আলাদা (master prompt-এর নোট অনুযায়ী) — সেই "plain" problem topic-এ কোনো leak হয়নি যাচাই
SELECT is(
  test.broadcast_count_topic('problem:P_E4_FIXTURE'),
  0::bigint,
  'broadcast_bids_changes: bids broadcast প্লেইন problem:<id> topic-এ (messages-এর topic, :bids ছাড়া) leak করেনি — ইচ্ছাকৃত topic-বিভাজন কনফার্ম'
);

UPDATE public.bids SET status = 'ACCEPTED' WHERE id = 'BID_TRIGGER_1';

SELECT is(
  test.broadcast_count('problem:P_E4_FIXTURE:bids', 'bids_UPDATE'),
  1::bigint,
  'broadcast_bids_changes: UPDATE-এ ঠিক ১বার fire করে, event=bids_UPDATE'
);
SELECT is(
  test.last_broadcast_payload('problem:P_E4_FIXTURE:bids', 'bids_UPDATE') -> 'record' ->> 'status',
  'ACCEPTED',
  'broadcast_bids_changes: UPDATE payload->record->status আপডেট-পরবর্তী মান দেখায়'
);

DELETE FROM public.bids WHERE id = 'BID_TRIGGER_1';

SELECT is(
  test.broadcast_count('problem:P_E4_FIXTURE:bids', 'bids_DELETE'),
  1::bigint,
  'broadcast_bids_changes: DELETE-এ ঠিক ১বার fire করে, event=bids_DELETE'
);
SELECT is(
  test.last_broadcast_payload('problem:P_E4_FIXTURE:bids', 'bids_DELETE') -> 'old_record' ->> 'id',
  'BID_TRIGGER_1',
  'broadcast_bids_changes: DELETE payload->old_record->id মোছা bid-এর id দেখায়'
);

-- এজ-কেস: তিনটা op মিলিয়ে ওই topic-এ মোট ঠিক ৩টাই broadcast (duplicate fire নেই)
SELECT is(
  test.broadcast_count_topic('problem:P_E4_FIXTURE:bids'),
  3::bigint,
  'broadcast_bids_changes: INSERT+UPDATE+DELETE মিলিয়ে মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

SELECT * FROM finish();
ROLLBACK;
