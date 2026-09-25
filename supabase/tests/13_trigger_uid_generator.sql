-- 13_trigger_uid_generator.sql — Step 13.7 (Database trigger coverage)
--
-- `trg_set_display_uid` / `set_display_uid_on_insert()` / `generate_unique_display_uid()` —
-- broadcast-প্যাটার্নের সম্পূর্ণ ব্যতিক্রম (13_trigger_helpers.sql-এর নোট দ্রষ্টব্য): এটা
-- BEFORE INSERT trigger, কোনো `realtime.broadcast_changes()` কল করে না, শুধু NEW-তে
-- `display_uid` বসায় ও `return new` করে। তাই এই ফাইল **helpers-এর broadcast-helper
-- (test.clear_broadcasts/broadcast_count ইত্যাদি) ব্যবহার করে না** — সম্পূর্ণ আলাদা
-- assertion-পদ্ধতি (inserted row-এর `display_uid` কলাম ও `public.display_uid_state`
-- টেবিলের state সরাসরি যাচাই)।
--
-- ফাইল-নাম sort-order: 'uid_generator' ('u', তারপর 'i') — `13_trigger_transactions_withdrawals.sql`
-- ('t')-এর পরে, `13_trigger_users_escrows.sql` ('us') এর ঠিক আগে (uid < use, 'i'<'s')। এই ফাইলের
-- কোনো broadcast-helper dependency নেই বলে আসলে অবস্থান functionally গুরুত্বপূর্ণ না, কিন্তু
-- convention বজায় রাখতে ও Step 13.5-এর সতর্কতা-নিয়ম অনুযায়ী `ls supabase/tests/*.sql | sort`
-- দিয়ে এই সেশনেই manually confirm করা হয়েছে।
--
-- ⚠️ **environment নোট:** এই সেশনেও sandbox network/apt কাজ করেনি (403 Forbidden, Step 13.6-এর
-- মতোই) — তাই এই ফাইলও **static-only** যাচাই করে লেখা হয়েছে (migration বডি থেকে সরাসরি,
-- rule #5/#5a অনুযায়ী কোনো অনুমান ছাড়া)। real psql+pgTAP run পরের সেশনে (network কাজ করলে) বা
-- GitHub Actions-এ হওয়া উচিত।
--
-- ═══════════════════════════════════════════════════════════════════════════════════
-- migration বডি থেকে যাচাই করা logic (`supabase/migrations/add_display_uid_generator.sql`)
-- ═══════════════════════════════════════════════════════════════════════════════════
-- 🔴 **জানা ব্লকার (এই সেশনে discover করা, আগের সেশনে অন্য একটা কাজের সময় প্রথম নোট হয়েছিল —
-- progress doc-এর আগের এন্ট্রি দ্রষ্টব্য, rule #1 অনুযায়ী migration ফাইল এখানে ছোঁয়া হয়নি):**
-- এই migration ফাইলের শেষের দিকে একটা backfill `DO $$ ... ORDER BY created_at ... $$` ব্লক আছে
-- যেটা `public.users`-এ `created_at` কলাম আশা করে — কিন্তু CI schema stub-এ `public.users`-এর
-- শুধু `updated_at` আছে, `created_at` নেই। **ফলাফল:** migration apply statement-by-statement
-- (psql autocommit, `ON_ERROR_STOP=1`) চলে বলে backfill DO-ব্লকের **আগের** সব statement
-- (table/column/index/function/trigger তৈরি — নিচের তালিকা) **সফলভাবেই কমিট হয়ে যায়**, শুধু
-- backfill DO-ব্লক ও তার পরের `ALTER ... SET NOT NULL` ব্যর্থ হয় ও বাকি migration থেমে যায়। এটাই
-- CI-র "৬টা known-expected fail migration"-এর একটা (progress doc-এ আগে থেকেই ডকুমেন্টেড, নতুন
-- আবিষ্কার না)। **এই ফাইলের টেস্টের জন্য এটা কোনো ব্লকার না** — trigger/function/table সবই তৈরি
-- হয়ে যায়, শুধু `display_uid` কলাম CI-তে nullable থেকে যায় (live-এ `NOT NULL`) — নিচের টেস্ট এই
-- পার্থক্যটা প্রভাবিত করে এমন কিছু assert করে না (আমরা সবসময় trigger দিয়েই generate করাচ্ছি,
-- কখনো ইচ্ছাকৃতভাবে NULL রেখে insert করছি না)।
--
-- ফাংশন লজিক (`generate_unique_display_uid()`):
--   - `public.display_uid_state` (single-row, id=1) থেকে `current_digits` পড়ে (`FOR UPDATE` লক
--     সহ) — শুরুর ডিফল্ট মান 6 (মাইগ্রেশনের নিজস্ব seed insert থেকে)।
--   - window: [10^(digits-1), 10^digits - 1] (৬ digits হলে [100000, 999999])।
--   - সেই window-এ কতগুলো user-এর display_uid ইতিমধ্যে আছে (`v_used`) গোনে — যদি
--     `v_used >= (v_range * 0.9)::bigint` হয়, `current_digits` স্থায়ীভাবে ১ বাড়িয়ে (কখনো কমে
--     না) আবার loop করে (নতুন, বড় window দিয়ে)।
--   - এরপর random candidate তৈরি করে, `not exists` দিয়ে collision-check করে — না মিললে রিটার্ন;
--     মিললে retry (সর্বোচ্চ ৫০বার), ৫০ বারেও না পেলে জোর করে digit-বৃদ্ধি করে recursive call।
-- Trigger (`set_display_uid_on_insert()`, BEFORE INSERT): `if new.display_uid is null then
--   new.display_uid := generate_unique_display_uid(); end if; return new;` — অর্থাৎ **explicit
--   ভাবে দেওয়া display_uid কখনো override হয় না**, শুধু NULL হলেই generate হয়।
--
-- ═══════════════════════════════════════════════════════════════════════════════════

BEGIN;
SELECT plan(10);

----------------------------------------------------------------------
-- (১) স্বাভাবিক INSERT — display_uid না দিলে trigger auto-generate করে, ৬-digit ফরম্যাটে
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone)
VALUES ('f1000001-0000-0000-0000-000000000001', 'CLIENT', 'UID-Trigger Fixture 1', '01722220001');

SELECT isnt(
  (SELECT display_uid FROM public.users WHERE id = 'f1000001-0000-0000-0000-000000000001'),
  NULL,
  'set_display_uid_on_insert: display_uid না দিয়ে insert করলে trigger auto-generate করে (NULL থাকে না)'
);

SELECT ok(
  (SELECT display_uid FROM public.users WHERE id = 'f1000001-0000-0000-0000-000000000001')
    BETWEEN 100000 AND 999999,
  'set_display_uid_on_insert: auto-generated display_uid ৬-digit window-এর ([100000,999999]) মধ্যে (ডিফল্ট current_digits=6)'
);

----------------------------------------------------------------------
-- (২) uniqueness — দুইটা পরপর insert (দুটোই auto-generate) ভিন্ন display_uid পায়
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone)
VALUES ('f1000002-0000-0000-0000-000000000002', 'CLIENT', 'UID-Trigger Fixture 2', '01722220002');

SELECT isnt(
  (SELECT display_uid FROM public.users WHERE id = 'f1000001-0000-0000-0000-000000000001'),
  (SELECT display_uid FROM public.users WHERE id = 'f1000002-0000-0000-0000-000000000002'),
  'set_display_uid_on_insert: পরপর দুইটা auto-generated display_uid ভিন্ন (কোনো collision/duplicate না) — unique index-ও এটা নিশ্চিত করছে (violation হলে INSERT-ই fail করত)'
);

----------------------------------------------------------------------
-- (৩) explicit display_uid দিলে trigger সেটা override করে না
----------------------------------------------------------------------
INSERT INTO public.users (id, role, name, phone, display_uid)
VALUES ('f1000003-0000-0000-0000-000000000003', 'CLIENT', 'UID-Trigger Fixture 3', '01722220003', 777777);

SELECT is(
  (SELECT display_uid FROM public.users WHERE id = 'f1000003-0000-0000-0000-000000000003'),
  777777::bigint,
  'set_display_uid_on_insert: explicit display_uid দিলে trigger সেটা override করে না (NEW.display_uid IS NULL চেক অনুযায়ী)'
);

----------------------------------------------------------------------
-- (৪) ⭐ digit-window auto-expand — মূল edge-case assertion
-- current_digits জোর করে ১-এ নামিয়ে (window [1,9], v_range=9), ৮টা explicit-display_uid দিয়ে
-- insert করে window ৮৯%+ ভরিয়ে ফেলা হচ্ছে (v_used=8 >= (9*0.9)::bigint = 8) — তারপর একটা নতুন
-- auto-generate insert করলে ফাংশনের নিজস্ব লজিক অনুযায়ী এটা current_digits-কে ২-এ বাড়িয়ে দেওয়ার
-- কথা (আর কখনো কমার কথা না) এবং নতুন candidate নতুন, বড় window ([10,99]) থেকে আসার কথা।
----------------------------------------------------------------------
UPDATE public.display_uid_state SET current_digits = 1 WHERE id = 1;

INSERT INTO public.users (id, role, name, phone, display_uid) VALUES
  ('f2000001-0000-0000-0000-000000000001', 'CLIENT', 'UID-Window Fixture 1', '01733330001', 1),
  ('f2000002-0000-0000-0000-000000000002', 'CLIENT', 'UID-Window Fixture 2', '01733330002', 2),
  ('f2000003-0000-0000-0000-000000000003', 'CLIENT', 'UID-Window Fixture 3', '01733330003', 3),
  ('f2000004-0000-0000-0000-000000000004', 'CLIENT', 'UID-Window Fixture 4', '01733330004', 4),
  ('f2000005-0000-0000-0000-000000000005', 'CLIENT', 'UID-Window Fixture 5', '01733330005', 5),
  ('f2000006-0000-0000-0000-000000000006', 'CLIENT', 'UID-Window Fixture 6', '01733330006', 6),
  ('f2000007-0000-0000-0000-000000000007', 'CLIENT', 'UID-Window Fixture 7', '01733330007', 7),
  ('f2000008-0000-0000-0000-000000000008', 'CLIENT', 'UID-Window Fixture 8', '01733330008', 8);

SELECT is(
  (SELECT current_digits FROM public.display_uid_state WHERE id = 1),
  1::smallint,
  '(pre-check) ৮টা explicit-value insert-এর পরেও current_digits এখনো ১ (এগুলো explicit ছিল, generate_unique_display_uid() কল হয়নি)'
);

-- এবার auto-generate insert — v_used(৮) >= threshold(৮) ধরার কথা, তাই digit বাড়বে
INSERT INTO public.users (id, role, name, phone)
VALUES ('f2000009-0000-0000-0000-000000000009', 'CLIENT', 'UID-Window Fixture 9 (auto)', '01733330009');

SELECT is(
  (SELECT current_digits FROM public.display_uid_state WHERE id = 1),
  2::smallint,
  'generate_unique_display_uid: [1,9] window ৮/৯ (≈৮৯%) ভরে যাওয়ার পর পরের auto-generate call current_digits ১→২-এ বাড়িয়ে দেয় (কখনো কমে না)'
);
SELECT ok(
  (SELECT display_uid FROM public.users WHERE id = 'f2000009-0000-0000-0000-000000000009')
    BETWEEN 10 AND 99,
  'generate_unique_display_uid: digit-বৃদ্ধির পরের candidate নতুন ২-digit window ([10,99])-এর মধ্যে থেকে আসে, পুরনো [1,9] window থেকে না'
);
SELECT isnt(
  (SELECT display_uid FROM public.users WHERE id = 'f2000009-0000-0000-0000-000000000009'),
  NULL,
  'generate_unique_display_uid: digit-বৃদ্ধির পরেও সফলভাবে একটা non-NULL candidate রিটার্ন করে (কোনো infinite loop/error ছাড়া)'
);

----------------------------------------------------------------------
-- (৫) collision-retry robustness — window প্রায় (কিন্তু threshold-এর নিচে) ভরা অবস্থায়ও
-- সফলভাবে একটা ফাঁকা slot খুঁজে বের করে (retry loop কাজ করছে তার প্রমাণ, ইনফিনিট লুপ/এরর নেই)
----------------------------------------------------------------------
-- সতর্কতা: f2000009 (উপরের ধাপ ৪) নিজেই [10,99] window থেকে একটা random মান পেয়েছিল — নিচের
-- explicit bulk-fill (10..89) এর সাথে সেই মান collide করলে unique-constraint violation-এ পুরো
-- টেস্ট hard-fail করবে। তাই সেই fixture user (শুধু তার assertion গুলো উপরে নেওয়ার পরে) এখানে
-- মুছে ফেলা হলো, যাতে window-এর হিসাব predictable/clean থাকে।
DELETE FROM public.users WHERE id = 'f2000009-0000-0000-0000-000000000009';

-- এখন current_digits=2 (window [10,99], v_range=90, threshold=(90*0.9)::bigint=81)। ৮০টা slot
-- ভরিয়ে (১০-৮৯, explicit), ঠিক ১০টা ([90,99]) ফাঁকা রাখা হচ্ছে —
-- v_used=80 < 81 থ্রেশহোল্ডের নিচে, তাই digit বাড়বে না, কিন্তু random-এ কয়েকবার collision-retry
-- লাগলেও শেষমেশ ওই ১০টা ফাঁকা slot-এর একটা পাওয়ার কথা।
INSERT INTO public.users (id, role, name, phone, display_uid)
SELECT
  ('f3000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
  'CLIENT',
  'UID-Retry Fixture ' || n,
  '017444' || lpad(n::text, 5, '0'),
  n
FROM generate_series(10, 89) AS n;

INSERT INTO public.users (id, role, name, phone)
VALUES ('f3000090-0000-0000-0000-000000000090', 'CLIENT', 'UID-Retry Fixture (auto)', '01744400090');

SELECT ok(
  (SELECT display_uid FROM public.users WHERE id = 'f3000090-0000-0000-0000-000000000090')
    BETWEEN 90 AND 99,
  'generate_unique_display_uid: window প্রায় (৮০/৯০) ভরা অবস্থায়ও collision-retry দিয়ে বাকি ফাঁকা slot-গুলোর ([90,99]) একটা সফলভাবে খুঁজে পায়'
);
SELECT is(
  (SELECT current_digits FROM public.display_uid_state WHERE id = 1),
  2::smallint,
  'generate_unique_display_uid: v_used(80) থ্রেশহোল্ড(81)-এর নিচে থাকায় current_digits অপরিবর্তিত (২) থাকে — অকারণে বাড়েনি'
);

SELECT * FROM finish();
ROLLBACK;
