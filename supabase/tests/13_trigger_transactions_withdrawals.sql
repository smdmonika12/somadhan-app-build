-- 13_trigger_transactions_withdrawals.sql — Step 13.4 (Database trigger coverage)
--
-- 13_trigger_helpers.sql-এর helper ব্যবহার করে `broadcast_transactions_changes`
-- (dual-owner, nullable-guarded) ও `broadcast_withdrawals_changes` (single-owner) —
-- 13_trigger_users_escrows.sql (Step 13.2)-এর প্যাটার্ন অনুসরণ করে।
--
-- event নাম — দুটোই সঠিকভাবে table-prefixed, কোনো bare-TG_OP বাগ নেই (migration বডি
-- `realtime_scoping_step2_wallet_event_naming_fix.sql`-এ verify করা, যেটা base migration
-- `realtime_scoping_step2_wallet_broadcast.sql` (bare TG_OP দিয়ে শুরু হয়েছিল)-এর ঠিক পরে
-- alphabetically apply হয়ে fix করে দেয় — users-এর 13.1-এ পাওয়া CI-order বাগের বিপরীতে, এখানে
-- filename-order নিজে থেকেই সঠিক, তাই কোনো CI-reconstruction সমস্যা নেই):
--   transactions : 'transactions_'||TG_OP, dual-owner (user_id ও solver_id দুটোই nullable),
--                   coalesce(NEW.col, OLD.col) is not null গার্ড দিয়ে conditionally broadcast।
--   withdrawals   : 'withdrawals_'||TG_OP, single-owner (solver_id NOT NULL সরাসরি স্কিমাতেই)।
--
-- 💰 money-related টেবিল বলে এই ফাইলে বিশেষ ফোকাস — payload-এ amount/balance ভুল user-এর কাছে
-- leak হচ্ছে কিনা:
--   ১. dual-owner nullable-guard আসলেই কাজ করছে কিনা (solver_id NULL হলে দ্বিতীয় broadcast
--      একদমই fire না হওয়া, malformed 'user:' topic তৈরি না হওয়া)।
--   ২. 🔍 নতুন পর্যবেক্ষণ (এই সেশনে টেস্ট করে ধরা): dual-owner guard `coalesce(NEW.col, OLD.col)`
--      ব্যবহার করে, অর্থাৎ UPDATE-এ যদি solver_id পরিবর্তন হয় (reassignment), broadcast শুধু
--      **নতুন** solver-এর topic-এ যায় — **পুরনো solver সেই reassignment-broadcast পায় না।**
--      এটা কোনো crash/error না, আর transactions টেবিলে solver_id সচরাচর reassign হয় না
--      (bidding flow-এ ঠিক হয়ে যায়, পরে বদলায় না) — কিন্তু যদি কখনো admin-tool দিয়ে reassign
--      করা হয়, পুরনো solver real-time আপডেট মিস করবে (leak না, বরং miss — কেউ ভুল ডেটা পাবে
--      না, কিন্তু প্রাপ্য আপডেট না পাওয়ার ঝুঁকি)। rule #1 অনুযায়ী শুধু ধরা ও রিপোর্ট করা হলো,
--      fix করা হয়নি (মাস্টার প্রম্পটের নির্দেশমতো)।
--   ৩. dual-owner broadcast-এ উভয় topic-এই **একই সম্পূর্ণ row** যায় (কোনো field-level
--      filtering নেই) — তাই owner ও solver দুজনেই net_amount/commission_amount দেখতে পায়,
--      এটা ইচ্ছাকৃত (উভয় পক্ষই লেনদেনের সাথে সরাসরি জড়িত) — payload equality assert করে
--      confirm করা হলো, কোনো বাগ না।

BEGIN;
SELECT plan(18);

SELECT test.seed_users();

----------------------------------------------------------------------
-- broadcast_transactions_changes — dual-owner, nullable-guarded
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role, has_solver_role)
VALUES
  ('e5000001-0000-0000-0000-000000000001', 'CLIENT', 'Txn Owner A2', '01711110008', 0, 0, true, false),
  ('e5000002-0000-0000-0000-000000000002', 'SOLVER', 'Txn Solver B2', '01711110009', 0, 0, false, true),
  ('e5000003-0000-0000-0000-000000000003', 'SOLVER', 'Txn Solver C2', '01711110010', 0, 0, false, true);

SELECT test.clear_broadcasts();  -- উপরের ৩টা fixture-user INSERT-broadcast বাদ দিয়ে শুরু

-- (১) user_id-only row (solver_id NULL) — যেমন wallet deposit-জাতীয় লেনদেন
INSERT INTO public.transactions (id, user_id, solver_id, gross_amount, net_amount, type)
VALUES ('TXN_TRIGGER_1', 'e5000001-0000-0000-0000-000000000001', NULL, 500.00, 500.00, 'WALLET_DEPOSIT');

SELECT is(
  test.broadcast_count('user:e5000001-0000-0000-0000-000000000001', 'transactions_INSERT'),
  1::bigint,
  'broadcast_transactions_changes: user_id-only (solver_id NULL) INSERT-এ owner-topic-এ ঠিক ১বার fire করে'
);
SELECT is(
  test.broadcast_count_topic('user:e5000001-0000-0000-0000-000000000001'),
  1::bigint,
  '💰 leak-guard: solver_id NULL হলে nullable-guard সঠিকভাবে ২য় broadcast আটকায় — owner-topic-এ মোট ঠিক ১টাই broadcast (malformed/duplicate ''user:'' broadcast নেই)'
);

-- (২) dual-owner row (user_id ও solver_id দুটোই সেট — সাধারণ job-payment লেনদেন)
INSERT INTO public.transactions (id, user_id, solver_id, gross_amount, net_amount, type)
VALUES ('TXN_TRIGGER_2', 'e5000001-0000-0000-0000-000000000001', 'e5000002-0000-0000-0000-000000000002', 1000.00, 900.00, 'JOB_PAYMENT');

SELECT is(
  test.broadcast_count('user:e5000001-0000-0000-0000-000000000001', 'transactions_INSERT'),
  2::bigint,  -- আগের TXN_TRIGGER_1-এর ১টা + এটার ১টা
  'broadcast_transactions_changes: dual-owner INSERT owner-topic-এও fire করে'
);
SELECT is(
  test.broadcast_count('user:e5000002-0000-0000-0000-000000000002', 'transactions_INSERT'),
  1::bigint,
  'broadcast_transactions_changes: dual-owner INSERT solver-topic-এও ১বার fire করে'
);
SELECT is(
  (test.last_broadcast_payload('user:e5000001-0000-0000-0000-000000000001', 'transactions_INSERT') -> 'record' ->> 'net_amount')::numeric,
  (test.last_broadcast_payload('user:e5000002-0000-0000-0000-000000000002', 'transactions_INSERT') -> 'record' ->> 'net_amount')::numeric,
  '💰 owner ও solver উভয় topic-এই একই net_amount দেখায় (ইচ্ছাকৃত — field-level filtering নেই, উভয় পক্ষই লেনদেনে সরাসরি জড়িত)'
);

SELECT test.clear_broadcasts();

UPDATE public.transactions SET type = 'JOB_PAYMENT_CONFIRMED' WHERE id = 'TXN_TRIGGER_2';

SELECT is(
  test.broadcast_count('user:e5000001-0000-0000-0000-000000000001', 'transactions_UPDATE'),
  1::bigint,
  'broadcast_transactions_changes: UPDATE owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e5000002-0000-0000-0000-000000000002', 'transactions_UPDATE'),
  1::bigint,
  'broadcast_transactions_changes: UPDATE solver-topic-এও ১বার fire করে'
);

SELECT test.clear_broadcasts();

-- 🔍 solver_id reassignment এজ-কেস (B2 → C2)
UPDATE public.transactions SET solver_id = 'e5000003-0000-0000-0000-000000000003' WHERE id = 'TXN_TRIGGER_2';

SELECT is(
  test.broadcast_count('user:e5000003-0000-0000-0000-000000000003', 'transactions_UPDATE'),
  1::bigint,
  '🔍 reassignment: নতুন solver (C2)-এর topic-এ UPDATE broadcast যায় (coalesce(NEW.solver_id,...) NEW-কেই prefer করে)'
);
SELECT is(
  test.broadcast_count('user:e5000002-0000-0000-0000-000000000002', 'transactions_UPDATE'),
  0::bigint,
  '🔍 reassignment: পুরনো solver (B2) এই reassignment-broadcast পায় না (design-এর সহজাত সীমাবদ্ধতা, fix করা হয়নি — rule #1)'
);

SELECT test.clear_broadcasts();

DELETE FROM public.transactions WHERE id = 'TXN_TRIGGER_2';

SELECT is(
  test.broadcast_count('user:e5000001-0000-0000-0000-000000000001', 'transactions_DELETE'),
  1::bigint,
  'broadcast_transactions_changes: DELETE owner-topic-এ ১বার fire করে'
);
SELECT is(
  test.broadcast_count('user:e5000003-0000-0000-0000-000000000003', 'transactions_DELETE'),
  1::bigint,
  'broadcast_transactions_changes: DELETE বর্তমান solver (C2, reassignment-পরবর্তী)-এর topic-এও ১বার fire করে'
);
SELECT is(
  test.last_broadcast_payload('user:e5000001-0000-0000-0000-000000000001', 'transactions_DELETE') -> 'old_record' ->> 'id',
  'TXN_TRIGGER_2',
  'broadcast_transactions_changes: DELETE payload->old_record->id মোছা transaction-এর id দেখায়'
);

DELETE FROM public.transactions WHERE id = 'TXN_TRIGGER_1';  -- cleanup, পরের সেকশনের topic-গণনা পরিষ্কার রাখতে

----------------------------------------------------------------------
-- broadcast_withdrawals_changes — single-owner (solver_id NOT NULL)
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_solver_role)
VALUES ('e6000001-0000-0000-0000-000000000001', 'SOLVER', 'Withdraw Fixture', '01711110011', 0, 0, true);

SELECT test.clear_broadcasts();  -- উপরের fixture-user INSERT-broadcast বাদ দিয়ে শুরু

INSERT INTO public.withdrawals (id, solver_id, solver_name, amount, method, status)
VALUES ('WD_TRIGGER_1', 'e6000001-0000-0000-0000-000000000001', 'Withdraw Fixture', 250.00, 'BKASH', 'PENDING');

SELECT is(
  test.broadcast_count('user:e6000001-0000-0000-0000-000000000001', 'withdrawals_INSERT'),
  1::bigint,
  'broadcast_withdrawals_changes: INSERT-এ ঠিক ১বার fire করে, event=withdrawals_INSERT'
);

-- 💰 leak-check: অসম্পর্কিত topic-এ কোনো broadcast leak করেনি (amount/account_number-এর মতো
-- sensitive field ভুল user-এর topic-এ যায়নি)
SELECT is(
  test.broadcast_count_topic('user:e6000001-0000-0000-0000-000000000099'),
  0::bigint,
  '💰 leak-check: অসম্পর্কিত topic-এ withdrawal broadcast leak করেনি'
);

UPDATE public.withdrawals SET status = 'COMPLETED' WHERE id = 'WD_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e6000001-0000-0000-0000-000000000001', 'withdrawals_UPDATE'),
  1::bigint,
  'broadcast_withdrawals_changes: UPDATE-এ ঠিক ১বার fire করে, event=withdrawals_UPDATE'
);
SELECT is(
  test.last_broadcast_payload('user:e6000001-0000-0000-0000-000000000001', 'withdrawals_UPDATE') -> 'record' ->> 'status',
  'COMPLETED',
  'broadcast_withdrawals_changes: UPDATE payload->record->status আপডেট-পরবর্তী মান দেখায়'
);

DELETE FROM public.withdrawals WHERE id = 'WD_TRIGGER_1';

SELECT is(
  test.broadcast_count('user:e6000001-0000-0000-0000-000000000001', 'withdrawals_DELETE'),
  1::bigint,
  'broadcast_withdrawals_changes: DELETE-এ ঠিক ১বার fire করে, event=withdrawals_DELETE'
);

-- এজ-কেস: তিনটা op মিলিয়ে ওই topic-এ মোট ঠিক ৩টাই broadcast (duplicate fire নেই)
SELECT is(
  test.broadcast_count_topic('user:e6000001-0000-0000-0000-000000000001'),
  3::bigint,
  'broadcast_withdrawals_changes: INSERT+UPDATE+DELETE মিলিয়ে মোট ঠিক ৩টা broadcast (duplicate fire নেই)'
);

SELECT * FROM finish();
ROLLBACK;
