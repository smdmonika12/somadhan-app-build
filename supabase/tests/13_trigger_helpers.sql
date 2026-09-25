-- 13_trigger_helpers.sql — Step 13.1 (Database trigger coverage: investigation + scaffold)
--
-- ⚠️ UPDATE (Step 13.2 সেশন, ব্যবহারকারীর অনুমোদনে): নিচের POC টেস্ট এখন
-- `'users_INSERT'/'users_UPDATE'/'users_DELETE'` (সঠিক/live-matching event) আশা করে —
-- 13.1-এ যেটা bare TG_OP ধরে লেখা হয়েছিল (CI-migration-order বাগের কারণে), সেটা এখন আর
-- প্রযোজ্য না, কারণ `supabase/migrations/zz_20260913145059_users_broadcast_event_naming_fix_ci_order.sql`
-- (নতুন, ব্যবহারকারীর স্পষ্ট অনুমতিতে যোগ করা) CI-র reconstruction-কে live-এর সাথে মিলিয়ে
-- দিয়েছে — বিস্তারিত সেই migration ফাইলের হেডার-কমেন্টে ও CI_TEST_SUITE_PROGRESS.md-এর
-- "Step 13.2" সেকশনে।
--
-- ⬇️ নিচের 13.1-era investigation-নোট (mechanism, topic/event টেবিল ইত্যাদি) ঐতিহাসিক
-- রেফারেন্সের জন্য অপরিবর্তিত রাখা হলো, শুধু users-এর event-column-এ যা লেখা ছিল সেটা এখন
-- পুরনো তথ্য — উপরের এই নোট-ই সর্বশেষ/সঠিক অবস্থা।
--
-- ⚠️ এই ফাইলে এখনো কোনো ফাংশনাল trigger-কভারেজ টেস্ট নেই (master prompt Step 13.1-এর
-- স্পষ্ট নির্দেশ: "কোনো trigger-test এখনো লেখা হবে না")। এই ফাইলের কাজ শুধু দুটো:
--   (ক) 13.2–13.7-এর জন্য reusable pgTAP helper function বানানো
--   (খ) একটা ছোট প্রুফ-অফ-কনসেপ্ট টেস্ট (broadcast_users_changes দিয়ে) — শুধু নিশ্চিত করা
--       যে নিচের mechanism/helper আসলেই কাজ করছে, users trigger-এর নিজস্ব পূর্ণাঙ্গ
--       কভারেজ (একাধিক edge case সহ) Step 13.2-এর স্কোপ, এই ফাইলের না — তাই এখানে
--       ইচ্ছাকৃতভাবে সংক্ষিপ্ত রাখা হলো, 13.2 এটা ডুপ্লিকেট করবে না বরং নিজের ফাইলে বাড়াবে।
--
-- ফাইল-নাম-কনভেনশন (এই সেশনেই ঠিক করা হলো, বাকি Step 13.x এটা অনুসরণ করবে):
--   supabase/tests/13_trigger_<table_group>.sql — যেমন 13.2 → 13_trigger_users_escrows.sql,
--   13.3 → 13_trigger_notifications_bids.sql, ইত্যাদি। এই ফাইল (13_trigger_helpers.sql)
--   sort-order-এ "13_trigger_" প্রিফিক্সের বাকি সব ফাইলের ঠিক আগে পড়তে হবে — ⚠️ **সতর্কতা
--   (Step 13.5-এ একবার ভুল হয়ে ধরা পড়েছিল ও ঠিক হয়েছে):** টেবিল-group-এর নাম সরাসরি ব্যবহার
--   করলে সবসময় এটা এমনি এমনি ঠিক হয় না ('gateway_payments' যেমন 'g' < 'h', ভুলভাবে আগে পড়ে
--   যেত) — তাই নতুন ফাইল লেখার পরপরই `ls supabase/tests/*.sql | sort` দিয়ে ম্যানুয়ালি
--   position confirm করতে হবে, প্রয়োজনে নাম rename করে (যেমন
--   `13_trigger_payments_additional_charges.sql`), শুধু অনুমান করা যাবে না।
--
-- ═══════════════════════════════════════════════════════════════════════════════════
-- INVESTIGATION — broadcast mechanism (migration ফাইল থেকে সরাসরি পড়ে, অনুমান না করে)
-- ═══════════════════════════════════════════════════════════════════════════════════
-- ১০টা trigger-ই একই প্যাটার্ন অনুসরণ করে (case-insensitive grep দিয়ে migration বডি
-- পড়ে কনফার্ম করা হয়েছে — supabase/migrations/realtime_scoping_step*.sql,
-- fix_users_broadcast_event_naming_collision.sql, add_display_uid_generator.sql):
--
--   AFTER INSERT OR UPDATE OR DELETE trigger → notify_<table>_broadcast() ফাংশন
--   (SECURITY DEFINER, set search_path = '') → ভেতরে:
--     perform realtime.broadcast_changes(<topic>, <event>, TG_OP, TG_TABLE_NAME,
--                                         TG_TABLE_SCHEMA, new, old);
--     return null;  -- AFTER trigger, row বদলায় না
--
--   ব্যতিক্রম শুধু `trg_set_display_uid`/`set_display_uid_on_insert` (Step 13.7-এর বিষয়):
--   এটা BEFORE INSERT trigger, broadcast করে না — display_uid generate করে NEW-তে বসায়
--   ও `return new` করে। সম্পূর্ণ ভিন্ন প্যাটার্ন, তাই নিচের helper এটার জন্য প্রযোজ্য না।
--
-- Topic/event প্যাটার্ন (৯টা broadcast trigger, প্রতিটা migration বডি থেকে যাচাই করা):
--   টেবিল                  | topic                              | event (TG_OP-প্রিফিক্সড)
--   ------------------------|-------------------------------------|---------------------------
--   users                   | 'user:'||id (single-owner)          | 'users_'||TG_OP
--                           |   (fix_users_broadcast_event_naming_collision.sql দিয়ে ফিক্সড —
--                           |    আগে bare TG_OP ছিল, notifications-এর সাথে collide করতো)
--                           |   ✅ Step 13.2 সেশনে CI-তেও এখন সঠিক (zz_ migration যোগ হয়েছে,
--                           |    নিচের 🔴🔴 নোট দ্রষ্টব্য — 13.1-এ CI-তে এটা bare ছিল)
--   escrows                 | 'user:'||user_id ও/অথবা 'user:'||solver_id (dual-owner, দুইবার call)
--                           |                                      | 'escrows_'||TG_OP
--   notifications           | 'user:'||user_id                    | bare TG_OP (⚠️ নিচে দেখুন)
--   transactions            | 'user:'||user_id ও/অথবা 'user:'||solver_id (dual, nullable-guarded)
--                           |                                      | 'transactions_'||TG_OP
--   withdrawals             | 'user:'||solver_id (single-owner)   | 'withdrawals_'||TG_OP
--   gateway_payments        | 'user:'||user_id (nullable-guarded) | 'gateway_payments_'||TG_OP
--   additional_charges      | 'user:'||user_id এবং 'user:'||solver_id (dual, unconditional —
--                           |   দুটো কলামই NOT NULL সরাসরি টেবিলে, নাল-গার্ড লাগে না)
--                           |                                      | 'additional_charges_'||TG_OP
--   messages                | 'problem:'||problem_id (resource-based, single call)
--                           |                                      | 'messages_'||TG_OP
--   bids                    | 'problem:'||problem_id||':bids' (resource-based, messages থেকে
--                           |   ইচ্ছাকৃতভাবে আলাদা topic — bids_select-এর broader visibility
--                           |   এর সাথে সামঞ্জস্যপূর্ণ, messages-এর owner/solver-only privacy না)
--                           |                                      | 'bids_'||TG_OP
--
--   🔴🔴 **আবিষ্কার — সবচেয়ে গুরুত্বপূর্ণ, এই সেশনে real psql+pgTAP রান করে ধরা পড়েছে:**
--   CI/sandbox-এর migration-apply order (`ls supabase/migrations/*.sql | sort`, alphabetical —
--   `full-test.yml`, `scripts/setup_test_env.sh`, `scripts/local_pgtap_bootstrap.sh` তিনটাই এই
--   পদ্ধতি ব্যবহার করে) আর **live Supabase-এর প্রকৃত chronological apply order আলাদা** —
--   ঠিক Step 12.12-এ যে সমস্যার জন্য `zz_`-প্রিফিক্স ট্রিক ব্যবহার করা হয়েছিল, একই
--   ক্যাটাগরির বাগ, এবার একটা trigger-function-এ। প্রমাণ (এই সেশনে যাচাই করা):
--     - `mcp__Supabase__list_migrations` (project `mghvvpndkxnscwryfkib`) অনুযায়ী live-এ প্রকৃত
--       ক্রম: `realtime_scoping_step5_users_escrows_broadcast` (version 20260913144826, users-এ
--       bare TG_OP দিয়ে trigger বানায়) **তারপর** `fix_users_broadcast_event_naming_collision`
--       (version 20260913145059, ঠিক করে 'users_'||TG_OP-এ) — অর্থাৎ live-এ ফিক্সটাই
--       শেষপর্যন্ত জেতে। live body সরাসরি পড়ে কনফার্ম করা হয়েছে:
--       `perform realtime.broadcast_changes('user:'||coalesce(new.id,old.id)::text,
--        'users_'||TG_OP, TG_OP, ...)` — **live সঠিক**।
--     - কিন্তু ফাইলনাম হিসেবে `fix_users_broadcast_event_naming_collision.sql` ('f'...)
--       alphabetically `realtime_scoping_step5_users_escrows_broadcast.sql` ('r'...)-এর
--       **আগে** পড়ে — তাই CI/sandbox-এ reconstruct করা DB-তে step5 migration সবার শেষে চলে
--       আর `notify_users_broadcast()`-কে **আবার বেয়ার TG_OP-তে ওভাররাইট করে ফেলে**।
--       এই সেশনেই বাস্তবে psql+pgTAP চালিয়ে সরাসরি দেখা গেছে: fresh CI-reconstructed
--       DB-তে insert/update/delete করলে `realtime.messages.event` হয় **'INSERT'/'UPDATE'/
--       'DELETE' (bare)**, `'users_INSERT'` না — অর্থাৎ **CI-র trigger আসলে সেই পুরনো বাগযুক্ত
--       সংস্করণটাই টেস্ট করছে যেটা live-এ আর নেই।**
--   ✅ **ফিক্স সম্পূর্ণ (Step 13.2 সেশন, ব্যবহারকারীর স্পষ্ট অনুমতিতে — "13.2 er sathe eta fix
--   kore deo"):** `supabase/migrations/zz_20260913145059_users_broadcast_event_naming_fix_ci_order.sql`
--   যোগ করা হয়েছে — `fix_users_broadcast_event_naming_collision.sql`-এর ঠিক একই body
--   (কোনো নতুন লজিক না, শুধু alphabetically সবার শেষে re-apply, যাতে CI-র reconstruction
--   `realtime_scoping_step5_...`-এ আর ওভাররাইট না হয়)। **live-এ কিছুই apply করা হয়নি** (live
--   আগে থেকেই সঠিক ছিল, শুধু CI/sandbox-এর reconstruction-order সমস্যা ছিল)। যাচাই: fresh
--   `setup_test_env.sh` রান করে `md5(prosrc)` এখন live-এর সাথে বাইট-বাই-বাইট মেলে
--   (`f866909a0a81bffc61c1b5f969ebc157`)। নিচের POC টেস্ট তাই এখন সঠিক
--   `'users_INSERT'/'users_UPDATE'/'users_DELETE'` ধরেই লেখা।
--
--
--   ✅ **আবিষ্কার — notifications-এর bare-TG_OP naming collision (Step 13.3-এ পাওয়া, এই সেশনেই
--   ফিক্সড):** `realtime_scoping_step1_notifications_broadcast.sql`-এর
--   `notify_notifications_broadcast()` bare `TG_OP` ব্যবহার করত event নামে — ঠিক সেই একই
--   বাগ-প্যাটার্ন যেটা users-এ ও wallet-গ্রুপে আলাদাভাবে ফিক্স করা হয়েছিল। এই বাগ **live-এও
--   বিদ্যমান ছিল** (users-এর মতো শুধু CI-order সমস্যা না) — client-side Kotlin কোডও তখন bare
--   event subscribe করছিল বলে app-এ active bug ছিল না, শুধু ভবিষ্যতের collision-ঝুঁকি ছিল।
--   ব্যবহারকারীর স্পষ্ট অনুমতিতে (Step 13.3→13.4-এর মাঝের সেশনে, ২০২৬-০৯-২২) দুই জায়গাতেই ফিক্স
--   করা হয়েছে: (১) নতুন migration
--   `zz_20260913085320_notifications_broadcast_event_naming_fix.sql` — **live-এ সরাসরি apply**
--   (mcp__Supabase__apply_migration), md5 দিয়ে কনফার্ম; (২) Kotlin
--   `SupabaseRealtimeManager.kt`-এ event এখন `"notifications_$op"`। সব ৭টা table-broadcast
--   (users/escrows/transactions/withdrawals/gateway_payments/additional_charges/notifications)
--   এখন table-prefixed — কোনো bare-event ব্যতিক্রম আর নেই এই topic-এ।
--
-- ═══════════════════════════════════════════════════════════════════════════════════
-- pgTAP-এ "fire হয়েছে কিনা" assert করার পদ্ধতি + sandbox-এ পাওয়া একটা ব্লকার-ফিক্স
-- ═══════════════════════════════════════════════════════════════════════════════════
-- realtime.messages টেবিলের stub আগে থেকেই ছিল (scripts/setup_test_env.sh ও
-- scripts/local_pgtap_bootstrap.sh, ধাপ ৭ "auth/realtime stub") — কিন্তু
-- `realtime.broadcast_changes()` ফাংশনটা **সম্পূর্ণ no-op ছিল** (`BEGIN RETURN; END;`),
-- তাই এই টেবিলে কখনো কোনো row লেখা হতো না, row-count-ভিত্তিক কোনো assertion লেখা
-- সম্ভবই ছিল না। **এই সেশনেই এই ব্লকার ফিক্স করা হলো** — দুটো script ফাইলেই
-- (setup_test_env.sh, local_pgtap_bootstrap.sh) `realtime.broadcast_changes()`-এর বডি
-- বদলে এখন সত্যিই realtime.messages-এ (topic, event, payload jsonb, private, extension)
-- insert করে — payload-এ operation/table/schema/record/old_record-ও থাকে (Step 13.4-এর
-- "sensitive payload leak" চেক-এর জন্যও এটাই দরকার হবে)।
--
-- ⚠️ **সীমাবদ্ধতা/অনুমান (rule #6-এর মতোই স্পষ্ট করে বলা হলো):** এটা real Supabase
-- `realtime.broadcast_changes()`-এর behavior-এর একটা inferred approximation — এই sandbox-এ
-- real `supabase/postgres` Docker image pull করা যায় না (network allowlist-এ নেই), তাই real
-- ইমেজের বিপরীতে verify করা যায়নি। তবে এটা গুরুত্বপূর্ণ: **আসল GitHub Actions CI**
-- (`.github/workflows/full-test.yml`) `supabase/postgres:15.1.0.117` ইমেজ ব্যবহার করে —
-- এটা real Supabase Postgres ইমেজ, যেখানে `realtime` schema/broadcast_changes/messages
-- সত্যিকারের বিল্ট-ইন (documented Supabase Realtime Broadcast আর্কিটেকচার অনুযায়ী
-- broadcast_changes()-এর payload আসলেই realtime.messages-এ যায়) — এই sandbox-এর
-- vanilla-Ubuntu-Postgres + upore stub (setup_test_env.sh/local_pgtap_bootstrap.sh) শুধু
-- **এই Claude sandbox-এ local self-verification-এর জন্য**, `full-test.yml` এই দুটো script
-- ব্যবহারই করে না (নিজের ইনলাইন step আছে, real ইমেজের উপর নির্ভর করে)। তাই real
-- CI-তে আচরণ এই stub-এর থেকে ভিন্ন হতে পারে — **চূড়ান্ত যাচাই real GitHub Actions
-- run-এই হবে** (এখনো কখনো real run হয়নি বলে জানা তথ্য অনুযায়ী), ততক্ষণ এই approximation-ই
-- sandbox-এ ব্যবহার করা একমাত্র উপায়।

-- ---------------------------------------------------------------------------
-- Helper functions (schema "test", 00_helpers.sql-এর সাথে সামঞ্জস্যপূর্ণ) — এগুলো
-- top-level statement হিসেবে (কোনো BEGIN/ROLLBACK ব্লকের ভেতরে না) তৈরি হচ্ছে, যাতে
-- psql-এর autocommit-এ এগুলো স্থায়ীভাবে তৈরি হয় ও পরের 13_trigger_*.sql ফাইলগুলো
-- (আলাদা psql invocation-এ, sorted পরে চলে) সরাসরি ব্যবহার করতে পারে।
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION test.clear_broadcasts() RETURNS void AS $$
  DELETE FROM realtime.messages;
$$ LANGUAGE sql;

-- নির্দিষ্ট topic+event-এ ঠিক কতবার broadcast fire হয়েছে (13.6-এর multi-update
-- loop-check-এর মূল ভিত্তি — n-বার UPDATE করে count ঠিক n কিনা যাচাই করা যাবে)।
CREATE OR REPLACE FUNCTION test.broadcast_count(p_topic text, p_event text) RETURNS bigint AS $$
  SELECT count(*) FROM realtime.messages WHERE topic = p_topic AND event = p_event;
$$ LANGUAGE sql;

-- শুধু topic-ভিত্তিক count (event-agnostic) — যেমন কোনো row-এ কয়টা total broadcast
-- গেছে (একাধিক event মিলিয়ে) সেটা দেখতে।
CREATE OR REPLACE FUNCTION test.broadcast_count_topic(p_topic text) RETURNS bigint AS $$
  SELECT count(*) FROM realtime.messages WHERE topic = p_topic;
$$ LANGUAGE sql;

-- সর্বশেষ broadcast-এর payload (Step 13.4-এর sensitive-field-leak চেকে দরকার হবে —
-- payload->'record' দিয়ে ঠিক কোন কলামগুলো ক্লায়েন্টের কাছে যাচ্ছে সেটা পরীক্ষা করা যায়)।
CREATE OR REPLACE FUNCTION test.last_broadcast_payload(p_topic text, p_event text) RETURNS jsonb AS $$
  SELECT payload FROM realtime.messages
  WHERE topic = p_topic AND event = p_event
  ORDER BY id DESC LIMIT 1;
$$ LANGUAGE sql;

-- ---------------------------------------------------------------------------
-- প্রুফ-অফ-কনসেপ্ট: broadcast_users_changes (INSERT/UPDATE/DELETE তিনটাই) —
-- শুধু infrastructure+helper কাজ করছে সেটা নিশ্চিত করা, users-এর পূর্ণাঙ্গ কভারেজ
-- (এবং escrows) Step 13.2-এ, এই ফাইলে না।
--
-- ⚠️ event নাম bare TG_OP ('INSERT'/'UPDATE'/'DELETE') ধরে assert করা হচ্ছে —
-- 'users_INSERT' না — উপরের 🔴🔴 আবিষ্কার-নোট দ্রষ্টব্য: এটা এই CI/sandbox
-- migration-reconstruction-এর *বর্তমান প্রকৃত (বাগযুক্ত)* আচরণ, live-এর সঠিক
-- আচরণ ছিল। ✅ Step 13.2 সেশনে ব্যবহারকারী migration-ফিক্স অনুমোদন করেছেন (zz_ ফাইল
-- যোগ হয়েছে), তাই এখন সঠিক 'users_'-প্রিফিক্সড event ধরেই assertion লেখা।
-- ---------------------------------------------------------------------------
BEGIN;
SELECT plan(7);

SELECT test.clear_broadcasts();

-- dedicated fixture user, seed_users()-এর ৪টা user স্পর্শ করা হয়নি
INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
VALUES ('d3000001-0000-0000-0000-000000000001', 'CLIENT', 'Trigger POC User', '01799999999', 0, 0, true);

SELECT is(
  test.broadcast_count('user:d3000001-0000-0000-0000-000000000001', 'users_INSERT'),
  1::bigint,
  '13.1/13.2 POC: broadcast_users_changes INSERT-এ ঠিক ১বার fire করে, event=users_INSERT (zz_ ফিক্সের পর সঠিক)'
);
SELECT is(
  test.broadcast_count_topic('user:d3000001-0000-0000-0000-000000000001'),
  1::bigint,
  '13.1 POC: INSERT-এর পরে ওই topic-এ মোট ১টাই broadcast (extra fire নেই)'
);

UPDATE public.users SET name = 'Trigger POC User Updated'
WHERE id = 'd3000001-0000-0000-0000-000000000001';

SELECT is(
  test.broadcast_count('user:d3000001-0000-0000-0000-000000000001', 'users_UPDATE'),
  1::bigint,
  '13.1/13.2 POC: UPDATE-এ ঠিক ১বার fire করে, event=users_UPDATE'
);

DELETE FROM public.users WHERE id = 'd3000001-0000-0000-0000-000000000001';

SELECT is(
  test.broadcast_count('user:d3000001-0000-0000-0000-000000000001', 'users_DELETE'),
  1::bigint,
  '13.1/13.2 POC: DELETE-এ ঠিক ১বার fire করে, event=users_DELETE'
);

-- payload sanity — record-এর ভেতরের id inserted row-এর সাথে মেলে কিনা (13.4-এর জন্য
-- last_broadcast_payload() হেল্পার আসলেই ব্যবহারযোগ্য সেটা এখানেই প্রমাণ করা হলো)
SELECT is(
  test.last_broadcast_payload('user:d3000001-0000-0000-0000-000000000001', 'users_INSERT') -> 'record' ->> 'id',
  'd3000001-0000-0000-0000-000000000001',
  '13.1 POC: broadcast payload->record->id INSERT-এর row-এর id-র সাথে মেলে'
);
SELECT is(
  test.last_broadcast_payload('user:d3000001-0000-0000-0000-000000000001', 'users_UPDATE') -> 'record' ->> 'name',
  'Trigger POC User Updated',
  '13.1 POC: broadcast payload->record->name UPDATE-এর পরের মান দেখায় (NEW, OLD না)'
);
SELECT is(
  test.last_broadcast_payload('user:d3000001-0000-0000-0000-000000000001', 'users_DELETE') -> 'old_record' ->> 'id',
  'd3000001-0000-0000-0000-000000000001',
  '13.1 POC: DELETE-এ payload->old_record->id (record NULL, AFTER DELETE-এ NEW থাকে না) দিয়ে মোছা row identify করা যায়'
);

SELECT * FROM finish();
ROLLBACK;
