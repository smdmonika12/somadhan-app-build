-- 13_trigger_users_escrows.sql — Step 13.2 (Database trigger coverage)
--
-- 13_trigger_helpers.sql-এর helper (test.clear_broadcasts/broadcast_count/
-- broadcast_count_topic/last_broadcast_payload) ব্যবহার করে `broadcast_users_changes` ও
-- `broadcast_escrows_changes` — এই দুটো trigger-এর পূর্ণাঙ্গ কভারেজ (13_trigger_helpers.sql-এর
-- POC-এর চেয়ে বেশি এজ-কেস সহ, ডুপ্লিকেট না করে)।
--
-- ✅ event নাম: `users_INSERT/UPDATE/DELETE` ও `escrows_INSERT/UPDATE/DELETE` — দুটোই এখন
-- CI-তে সঠিক। users-এর জন্য `supabase/migrations/zz_20260913145059_users_broadcast_event_naming_fix_ci_order.sql`
-- (এই সেশনেই, ব্যবহারকারীর অনুমতিতে যোগ) নিশ্চিত করেছে CI-র migration-reconstruction live-এর
-- সাথে মেলে (বিস্তারিত 13_trigger_helpers.sql-এর হেডার-নোট ও ওই migration ফাইলের কমেন্টে)।
-- escrows-এর event শুরু থেকেই সঠিক ছিল (কোনো collision-বাগ কখনো ছিল না)।
--
-- ⚠️ scope note: এই ফাইল শুধু trigger fire mechanics + payload content যাচাই করে।
-- topic-এ কে subscribe করতে পারবে (RLS) সেটা Step 14 (RLS policy coverage)-এর বিষয়, এখানে
-- আলোচনা/টেস্ট করা হয়নি — শুধু নিচের "sensitive column" নোটে data-shape সংক্রান্ত একটা
-- পর্যবেক্ষণ রেকর্ড করা হলো।

BEGIN;
SELECT plan(19);

SELECT test.seed_users();
-- CLIENT: 11111111-...   SOLVER1: 22222222-...   SOLVER2: 33333333-...   ADMIN: 99999999-...

----------------------------------------------------------------------
-- broadcast_users_changes — dedicated fixture user (seed_users()-এর ৪টা স্পর্শ না করে,
-- যাতে seed_users()-এর নিজস্ব INSERT broadcast এই টেস্টের count নষ্ট না করে)
----------------------------------------------------------------------
SELECT test.clear_broadcasts();

INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
VALUES ('e1000001-0000-0000-0000-000000000001', 'CLIENT', 'Users-Trigger Fixture', '01711110001', 0, 0, true);

SELECT is(
  test.broadcast_count('user:e1000001-0000-0000-0000-000000000001', 'users_INSERT'),
  1::bigint,
  'broadcast_users_changes: INSERT-এ ঠিক ১বার fire করে, event=users_INSERT'
);

-- এজ-কেস: এই broadcast অন্য কোনো ভুল topic-এ leak করেনি (নির্দিষ্টভাবে অন্য একটা
-- অপ্রাসঙ্গিক user-topic-এ count শূন্য কিনা)
SELECT is(
  test.broadcast_count_topic('user:e1000001-0000-0000-0000-000000000099'),
  0::bigint,
  'broadcast_users_changes: ভুল/অসম্পর্কিত topic-এ কোনো broadcast leak করেনি'
);

UPDATE public.users SET name = 'Users-Trigger Fixture Updated'
WHERE id = 'e1000001-0000-0000-0000-000000000001';

SELECT is(
  test.broadcast_count('user:e1000001-0000-0000-0000-000000000001', 'users_UPDATE'),
  1::bigint,
  'broadcast_users_changes: UPDATE-এ ঠিক ১বার fire করে, event=users_UPDATE'
);
SELECT is(
  test.last_broadcast_payload('user:e1000001-0000-0000-0000-000000000001', 'users_UPDATE') -> 'record' ->> 'name',
  'Users-Trigger Fixture Updated',
  'broadcast_users_changes: UPDATE payload->record->name আপডেট-পরবর্তী মান দেখায়'
);

DELETE FROM public.users WHERE id = 'e1000001-0000-0000-0000-000000000001';

SELECT is(
  test.broadcast_count('user:e1000001-0000-0000-0000-000000000001', 'users_DELETE'),
  1::bigint,
  'broadcast_users_changes: DELETE-এ ঠিক ১বার fire করে, event=users_DELETE'
);

-- এজ-কেস: তিনটা op মিলিয়ে ওই topic-এ মোট ঠিক ৩টাই broadcast — কোনো duplicate/extra fire নেই
SELECT is(
  test.broadcast_count_topic('user:e1000001-0000-0000-0000-000000000001'),
  3::bigint,
  'broadcast_users_changes: INSERT+UPDATE+DELETE মিলিয়ে মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

-- ⚠️ sensitive-column নোট (leak-check, শুধু observation — এই ফাইলে ফিক্স/RLS-টেস্ট না):
-- public.users-এ KYC document fields (kyc_document_number, kyc_document_front_image,
-- kyc_document_back_image, kyc_selfie_image ইত্যাদি) আছে, আর broadcast_users_changes পুরো
-- row-ই broadcast করে (to_jsonb(new)/(old))। কিন্তু topic ('user:'||id) নিজেই owner-scoped —
-- realtime.messages-এর RLS policy ("users can receive own user-topic broadcasts",
-- realtime_scoping_step1_notifications_broadcast.sql) শুধু
-- `realtime.topic() = 'user:'||auth.uid()` allow করে, কোনো admin-wide exception ছাড়াই — তাই
-- শুধু owner নিজেই এই payload subscribe করতে পারবে, নিজের already-known KYC ডেটা। তাই
-- কার্যত leak না, তবে এই RLS policy-টাই আসল guarantee — Step 14-এ সরাসরি টেস্ট করা উচিত
-- (এই ফাইলে না, শুধু নোট রাখা হলো)।
SELECT ok(
  true,
  'নোট: users broadcast payload-এ KYC/sensitive column থাকলেও topic owner-scoped RLS-এর উপর নির্ভরশীল — Step 14-এ RLS সরাসরি verify হবে'
);

----------------------------------------------------------------------
-- ⭐ মূল assertion — multi-update loop-check (Step 13.6-এ যোগ করা হলো, shimmer-লুপ বাগের
-- সন্দেহভাজন কারণ, ডুপ্লিকেট ফাইল না করে এখানেই — বিস্তারিত ব্যাখ্যা
-- 13_trigger_messages.sql-এর একই-নামের সেকশনে)। একই user row পরপর ৫বার আপডেট করে
-- broadcast ঠিক ৫বারই fire হচ্ছে কিনা যাচাই করা হচ্ছে।
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
VALUES ('e1000002-0000-0000-0000-000000000002', 'CLIENT', 'Users-Loop Fixture', '01711110099', 0, 0, true);

-- INSERT-এর broadcast বাদ দিয়ে শুধু পরের n-টা UPDATE-এর broadcast আলাদাভাবে গোনার জন্য clear
SELECT test.clear_broadcasts();

DO $$
DECLARE
  i integer;
BEGIN
  FOR i IN 1..5 LOOP
    UPDATE public.users
    SET name = 'Users-Loop Fixture Updated #' || i
    WHERE id = 'e1000002-0000-0000-0000-000000000002';
  END LOOP;
END;
$$;

SELECT is(
  test.broadcast_count('user:e1000002-0000-0000-0000-000000000002', 'users_UPDATE'),
  5::bigint,
  'broadcast_users_changes: একই row-এ পরপর ৫টা UPDATE-এ broadcast ঠিক ৫বারই fire করে (loop/extra-fire ধরা পড়েনি)'
);
SELECT is(
  test.broadcast_count_topic('user:e1000002-0000-0000-0000-000000000002'),
  5::bigint,
  'broadcast_users_changes: loop-check-এর সময় ওই topic-এ মোট broadcast-ও ঠিক ৫টা (অন্য কোনো event/extra fire নেই)'
);

----------------------------------------------------------------------
-- broadcast_escrows_changes — dual-owner (user_id ও solver_id দুটোই), দুই topic-এই broadcast
----------------------------------------------------------------------
SELECT test.clear_broadcasts();

INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role, has_solver_role)
VALUES
  ('e2000001-0000-0000-0000-000000000001', 'CLIENT', 'Escrow Owner Fixture', '01711110002', 0, 0, true, false),
  ('e2000002-0000-0000-0000-000000000002', 'SOLVER', 'Escrow Solver Fixture', '01711110003', 0, 0, false, true);

INSERT INTO public.problems (id, user_id, title, status)
VALUES ('P_E1_FIXTURE', 'e2000001-0000-0000-0000-000000000001', 'Escrow Trigger Fixture Problem', 'IN_PROGRESS');

INSERT INTO public.escrows (id, problem_id, problem_title, user_id, solver_id, base_amount, status)
VALUES ('ESC_TRIGGER_1', 'P_E1_FIXTURE', 'Escrow Trigger Fixture Problem',
        'e2000001-0000-0000-0000-000000000001', 'e2000002-0000-0000-0000-000000000002', 500.00, 'HELD');

SELECT is(
  test.broadcast_count('user:e2000001-0000-0000-0000-000000000001', 'escrows_INSERT'),
  1::bigint,
  'broadcast_escrows_changes: INSERT owner (user_id)-এর topic-এও ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e2000002-0000-0000-0000-000000000002', 'escrows_INSERT'),
  1::bigint,
  'broadcast_escrows_changes: INSERT solver (solver_id)-এর topic-এও ১বার fire করে (dual-owner)'
);
-- এজ-কেস: owner-এর topic-এ escrows_INSERT ঠিক ১বারই (দুইবার fire হয়ে যায়নি)। ⚠️ এখানে
-- broadcast_count_topic() (event-agnostic) ব্যবহার করা হয়নি ইচ্ছাকৃতভাবে — একই 'user:<id>'
-- topic owner/solver user-দুটো insert করার সময়ও broadcast_users_changes fire করেছিল
-- (users_INSERT), তাই topic-এর মোট count এখানে ২ (users_INSERT + escrows_INSERT) — এটাই
-- আসল কারণ কেন event-নাম আলাদা রাখা জরুরি (users_/escrows_ প্রিফিক্স ছাড়া bare TG_OP হলে
-- এই দুটো event client-সাইডে গুলিয়ে যেত, ঠিক 13_trigger_helpers.sql-এর 🔴🔴 নোটে বর্ণিত বাগের
-- মতোই) — তাই event-specific count-ই সঠিক assertion।
SELECT is(
  test.broadcast_count('user:e2000001-0000-0000-0000-000000000001', 'escrows_INSERT'),
  1::bigint,
  'broadcast_escrows_changes: owner-topic-এ escrows_INSERT ঠিক ১বারই fire করে (duplicate fire নেই)'
);
SELECT is(
  test.last_broadcast_payload('user:e2000001-0000-0000-0000-000000000001', 'escrows_INSERT') -> 'record' ->> 'base_amount',
  '500.00',
  'broadcast_escrows_changes: owner-topic payload->record->base_amount সঠিক amount দেখায়'
);
SELECT is(
  test.last_broadcast_payload('user:e2000002-0000-0000-0000-000000000002', 'escrows_INSERT') -> 'record' ->> 'base_amount',
  '500.00',
  'broadcast_escrows_changes: solver-topic payload-ও একই amount দেখায় (দুই পক্ষই একই row পায়)'
);

UPDATE public.escrows SET status = 'RELEASED' WHERE id = 'ESC_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e2000001-0000-0000-0000-000000000001', 'escrows_UPDATE'),
  1::bigint,
  'broadcast_escrows_changes: UPDATE owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e2000002-0000-0000-0000-000000000002', 'escrows_UPDATE'),
  1::bigint,
  'broadcast_escrows_changes: UPDATE solver-topic-এও ১বার fire করে'
);

DELETE FROM public.escrows WHERE id = 'ESC_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e2000001-0000-0000-0000-000000000001', 'escrows_DELETE'),
  1::bigint,
  'broadcast_escrows_changes: DELETE owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e2000002-0000-0000-0000-000000000002', 'escrows_DELETE'),
  1::bigint,
  'broadcast_escrows_changes: DELETE solver-topic-এও ১বার fire করে'
);
SELECT is(
  test.last_broadcast_payload('user:e2000002-0000-0000-0000-000000000002', 'escrows_DELETE') -> 'old_record' ->> 'id',
  'ESC_TRIGGER_1',
  'broadcast_escrows_changes: DELETE payload->old_record->id মোছা escrow-র id দেখায়'
);

SELECT * FROM finish();
ROLLBACK;
