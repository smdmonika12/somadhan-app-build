-- 13_trigger_payments_additional_charges.sql — Step 13.5 (Database trigger coverage)
-- (⚠️ ফাইলের নাম `13_trigger_gateway_payments_additional_charges.sql` থেকে rename করা হয়েছে
-- এই সেশনেই — মূল নামটা alphabetically 'g' < 'h' হওয়ায় `13_trigger_helpers.sql`-এর **আগে**
-- sort হতো, ফলে fresh DB-তে প্রথমবার রান করলে `test.clear_broadcasts()` ইত্যাদি ফাংশন তখনো
-- তৈরি না হওয়ায় fail করতো — ঠিক users-এর CI-order বাগের মতোই একটা ভিন্ন প্যাটার্ন, এবার
-- migration-এ না, টেস্ট-ফাইলের নিজের নামেই। এই সেশনেই ধরা পড়ে সাথে সাথে ঠিক করা হয়েছে, fresh
-- DB rebuild করে পুনরায় verify করা হয়েছে — বিস্তারিত progress doc-এ।)
--
-- 13_trigger_helpers.sql-এর helper ব্যবহার করে `broadcast_gateway_payments_changes`
-- (single-owner, nullable-guarded) ও `broadcast_additional_charges_changes` (dual-party,
-- unconditional) — 13_trigger_users_escrows.sql (Step 13.2)-এর প্যাটার্ন অনুসরণ করে।
-- এই দুটোই মাস্টার প্রম্পট অনুযায়ী শেষ দুটো "সাধারণ" broadcast trigger (13.6-এ messages আলাদা,
-- multi-update loop-check-এর জন্য বিশেষভাবে সংরক্ষিত)।
--
-- event নাম — দুটোই সঠিকভাবে table-prefixed (migration বডি
-- `realtime_scoping_step2_wallet_event_naming_fix.sql`-এ verify করা, 13.4-এর মতোই
-- filename-order নিজে থেকে সঠিক, কোনো CI-reconstruction বাগ নেই):
--   gateway_payments   : 'gateway_payments_'||TG_OP, single-owner (user_id nullable),
--                         `coalesce(NEW.user_id, OLD.user_id) is not null` গার্ড দিয়ে conditional।
--   additional_charges : 'additional_charges_'||TG_OP, dual-party (user_id ও solver_id) —
--                         **কোনো nullable-guard নেই**, প্রতি op-এ unconditionally দুইবার
--                         broadcast_changes() কল হয় — migration বডির কমেন্টেই লেখা আছে এটা
--                         নিরাপদ কারণ দুটো কলামই real production টেবিলে সরাসরি NOT NULL।
--
-- 💰 money-related টেবিল বলে বিশেষ ফোকাস:
--   ১. gateway_payments-এর nullable-guard আসলেই কাজ করছে কিনা (user_id NULL হলে কোনো
--      broadcast-ই fire না হওয়া, malformed topic তৈরি না হওয়া) — `realtime.messages`-এ
--      সরাসরি raw total-row-count চেক করে verify করা হলো (topic নিজেই NULL হয়ে যায় বলে
--      `broadcast_count_topic('user:')`-জাতীয় string-match কাজ করবে না, তাই raw count)।
--   ২. ⚠️ **schema-stub সীমাবদ্ধতা নোট (rule #6-এর মতোই স্পষ্ট করে বলা হলো):** এই CI-র
--      reverse-engineered schema stub (`05_job_release_escrow_schema_stub.sql`)-এ
--      additional_charges.user_id/solver_id-তে **কোনো NOT NULL constraint বসানো নেই**
--      (stub permissive, real production-এর মতো strict না) — কিন্তু migration বডির কমেন্ট ও
--      13_trigger_helpers.sql-এর টেবিল নিজেই বলছে real টেবিলে এই দুটো কলাম সরাসরি NOT NULL।
--      তাই এই ফাইলে ইচ্ছাকৃতভাবে **কোনো NULL-value fixture insert করা হয়নি** additional_charges-এ
--      (transactions/gateway_payments-এর মতো NULL-guard এজ-কেস এখানে প্রযোজ্য না বলে ধরে নেওয়া
--      হলো, real schema-র উপর নির্ভর করে) — যদি real production-এ কখনো এই constraint শিথিল হয়,
--      unconditional dual-broadcast একটা malformed-topic broadcast পাঠাতে পারে (未verified রিস্ক,
--      শুধু ডকুমেন্ট করা হলো, rule #1 অনুযায়ী fix/schema-stub-পরিবর্তন করা হয়নি)।

BEGIN;
SELECT plan(16);

SELECT test.seed_users();

----------------------------------------------------------------------
-- broadcast_gateway_payments_changes — single-owner, nullable-guarded
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
VALUES ('e7000001-0000-0000-0000-000000000001', 'CLIENT', 'GWPay Fixture', '01711110012', 0, 0, true);

SELECT test.clear_broadcasts();  -- উপরের fixture-user INSERT-broadcast বাদ দিয়ে শুরু

-- 💰 nullable-guard চেক: user_id NULL (যেমন কোনো admin-side reconciliation log)
INSERT INTO public.gateway_payments (id, user_id, amount, gateway, purpose, status)
VALUES ('GWPAY_TRIGGER_NULL', NULL, 300.00, 'BKASH', 'RECONCILIATION', 'PENDING');

SELECT is(
  (SELECT count(*) FROM realtime.messages)::bigint,
  0::bigint,
  '💰 leak-guard: user_id NULL হলে nullable-guard broadcast সম্পূর্ণ আটকায় — কোনো malformed ''user:''-topic broadcast তৈরি হয়নি (raw total-row-count = ০)'
);

INSERT INTO public.gateway_payments (id, user_id, amount, gateway, purpose, status)
VALUES ('GWPAY_TRIGGER_1', 'e7000001-0000-0000-0000-000000000001', 500.00, 'NAGAD', 'WALLET_DEPOSIT', 'PENDING');

SELECT is(
  test.broadcast_count('user:e7000001-0000-0000-0000-000000000001', 'gateway_payments_INSERT'),
  1::bigint,
  'broadcast_gateway_payments_changes: user_id সেট থাকলে INSERT-এ ঠিক ১বার fire করে'
);

-- এজ-কেস: ভুল/অসম্পর্কিত topic-এ কোনো broadcast leak করেনি
SELECT is(
  test.broadcast_count_topic('user:e7000001-0000-0000-0000-000000000099'),
  0::bigint,
  'broadcast_gateway_payments_changes: ভুল/অসম্পর্কিত topic-এ কোনো broadcast leak করেনি'
);

UPDATE public.gateway_payments SET status = 'SUCCESS' WHERE id = 'GWPAY_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e7000001-0000-0000-0000-000000000001', 'gateway_payments_UPDATE'),
  1::bigint,
  'broadcast_gateway_payments_changes: UPDATE-এ ঠিক ১বার fire করে'
);
SELECT is(
  test.last_broadcast_payload('user:e7000001-0000-0000-0000-000000000001', 'gateway_payments_UPDATE') -> 'record' ->> 'status',
  'SUCCESS',
  'broadcast_gateway_payments_changes: UPDATE payload->record->status আপডেট-পরবর্তী মান দেখায়'
);

DELETE FROM public.gateway_payments WHERE id = 'GWPAY_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e7000001-0000-0000-0000-000000000001', 'gateway_payments_DELETE'),
  1::bigint,
  'broadcast_gateway_payments_changes: DELETE-এ ঠিক ১বার fire করে'
);

-- এজ-কেস: তিনটা op মিলিয়ে ওই topic-এ মোট ঠিক ৩টাই broadcast (duplicate fire নেই)
SELECT is(
  test.broadcast_count_topic('user:e7000001-0000-0000-0000-000000000001'),
  3::bigint,
  'broadcast_gateway_payments_changes: INSERT+UPDATE+DELETE মিলিয়ে মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

----------------------------------------------------------------------
-- broadcast_additional_charges_changes — dual-party, unconditional (no nullable-guard)
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role, has_solver_role)
VALUES
  ('e8000001-0000-0000-0000-000000000001', 'CLIENT', 'AddChg Owner Fixture', '01711110013', 0, 0, true, false),
  ('e8000002-0000-0000-0000-000000000002', 'SOLVER', 'AddChg Solver Fixture', '01711110014', 0, 0, false, true);

SELECT test.clear_broadcasts();  -- উপরের ২টা fixture-user INSERT-broadcast বাদ দিয়ে শুরু

INSERT INTO public.additional_charges (id, problem_id, solver_id, user_id, reason, amount, status)
VALUES ('AC_TRIGGER_1', 'P_E8_FIXTURE', 'e8000002-0000-0000-0000-000000000002', 'e8000001-0000-0000-0000-000000000001', 'Extra material cost', 200.00, 'PENDING');

SELECT is(
  test.broadcast_count('user:e8000001-0000-0000-0000-000000000001', 'additional_charges_INSERT'),
  1::bigint,
  'broadcast_additional_charges_changes: INSERT owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e8000002-0000-0000-0000-000000000002', 'additional_charges_INSERT'),
  1::bigint,
  'broadcast_additional_charges_changes: INSERT solver-topic-এও ১বার fire করে (unconditional dual)'
);
SELECT is(
  (test.last_broadcast_payload('user:e8000001-0000-0000-0000-000000000001', 'additional_charges_INSERT') -> 'record' ->> 'amount')::numeric,
  (test.last_broadcast_payload('user:e8000002-0000-0000-0000-000000000002', 'additional_charges_INSERT') -> 'record' ->> 'amount')::numeric,
  '💰 owner ও solver উভয় topic-এই একই amount দেখায় (ইচ্ছাকৃত — escrows/transactions-এর মতোই, উভয় পক্ষই extra-charge-এ সরাসরি জড়িত)'
);

UPDATE public.additional_charges SET status = 'ACCEPTED' WHERE id = 'AC_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e8000001-0000-0000-0000-000000000001', 'additional_charges_UPDATE'),
  1::bigint,
  'broadcast_additional_charges_changes: UPDATE owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e8000002-0000-0000-0000-000000000002', 'additional_charges_UPDATE'),
  1::bigint,
  'broadcast_additional_charges_changes: UPDATE solver-topic-এও ১বার fire করে'
);

DELETE FROM public.additional_charges WHERE id = 'AC_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e8000001-0000-0000-0000-000000000001', 'additional_charges_DELETE'),
  1::bigint,
  'broadcast_additional_charges_changes: DELETE owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e8000002-0000-0000-0000-000000000002', 'additional_charges_DELETE'),
  1::bigint,
  'broadcast_additional_charges_changes: DELETE solver-topic-এও ১বার fire করে'
);
SELECT is(
  test.last_broadcast_payload('user:e8000001-0000-0000-0000-000000000001', 'additional_charges_DELETE') -> 'old_record' ->> 'id',
  'AC_TRIGGER_1',
  'broadcast_additional_charges_changes: DELETE payload->old_record->id মোছা charge-এর id দেখায়'
);

-- এজ-কেস: তিনটা op মিলিয়ে owner-topic-এ মোট ঠিক ৩টাই broadcast (duplicate fire নেই)
SELECT is(
  test.broadcast_count_topic('user:e8000001-0000-0000-0000-000000000001'),
  3::bigint,
  'broadcast_additional_charges_changes: INSERT+UPDATE+DELETE মিলিয়ে owner-topic-এ মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

SELECT * FROM finish();
ROLLBACK;
