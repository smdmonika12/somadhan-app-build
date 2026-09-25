-- 14_rls_00_helpers.sql — Step 14.1 (RLS policy coverage: discovery + dual-auth-context helper)
--
-- ⚠️ নাম-কনভেনশন (ইচ্ছাকৃত, Step 13.5-এর ভুল এড়াতে): এই ফাইল "14_rls_00_helpers.sql" —
-- "00_" prefix যোগ করা হয়েছে যাতে ভবিষ্যতের 14_rls_bids.sql / 14_rls_messages.sql /
-- 14_rls_notifications.sql ফাইলগুলোর তুলনায় এটা **নিশ্চিতভাবে আগে** sort হয় (13_trigger_helpers.sql
-- নিছক "helpers" < "messages"/"notifications"/"payments"/"transactions"/"uid"/"users" হওয়ায় ভাগ্যক্রমে
-- আগে পড়েছিল — কিন্তু "helpers" > "bids" (h > b)! তাই যদি এখানে "14_rls_helpers.sql" নাম দেওয়া হতো,
-- future 14_rls_bids.sql এর *আগে* পড়ে যেত, যেটা ভাঙা helper-নির্ভরতা তৈরি করত। ডিজিট prefix "00_"
-- যেকোনো টেবিল-গ্রুপ নামের চেয়ে ASCII-তে ছোট, তাই এই সমস্যা কাঠামোগতভাবেই এড়ানো গেল।)
-- **পরের সেশনগুলো (14.2/14.3/14.4) এই কনভেনশন মেনে চলবে:** 14_rls_bids.sql, 14_rls_messages.sql,
-- 14_rls_notifications.sql — এই তিনটার যেকোনোটা বানানোর সাথে সাথেই `ls supabase/tests/*.sql | sort`
-- দিয়ে position confirm করবে (blind trust না)।
--
-- ⚠️ এই ফাইলে **কোনো ফাংশনাল policy-টেস্ট নেই** (master prompt-এর Step 14.1-এর স্পষ্ট নির্দেশ) —
-- শুধু (ক) discovery/investigation নোট (কমেন্টে), আর (খ) 14.2–14.4-এর জন্য reusable helper function।
-- কোনো plan()/finish() নেই ইচ্ছাকৃতভাবে — এই ফাইল শুধু DDL (function definitions), তাই
-- scripts/run_tests.sh-এর loop-এ এটা চললে কোনো "not ok"/ERROR আসার কথা না।

-- ═══════════════════════════════════════════════════════════════════════════════════
-- DISCOVERY — ৫টা CREATE POLICY statement (case-insensitive grep, rule #5a) → ৪টা distinct policy
-- ═══════════════════════════════════════════════════════════════════════════════════
-- grep -rniE "create policy" supabase/migrations/*.sql দিয়ে হুবহু ৫টা লাইন মেলে, কিন্তু এর মধ্যে
-- ২টা লাইন একই policy-র (idempotency-গার্ড হিসেবে দ্বিতীয়বার তৈরি, Step 13.8-এর CI-migration-order
-- বাগ-ফিক্সের অংশ) — তাই আসলে **৪টা distinct policy**:
--
-- ১) "messages_select" — public.messages, FOR SELECT TO authenticated
--    ফাইল: realtime_scoping_messages_solver_thread_isolation.sql:26-39
--    USING: auth.uid()=sender_id OR auth.uid()=receiver_id OR is_admin(auth.uid())
--           OR EXISTS(problems p: p.id=messages.problem_id AND p.user_id=auth.uid())
--    অর্থ: sender/receiver/admin/problem-owner — যেকেউ একটা মেসেজ-রো পড়তে পারে। এটাই সেই
--    solver-thread-isolation ফিক্স (আগে accepted_solver ব্লানকেট এক্সেস দিতো, এখন sender/receiver-
--    ভিত্তিক)। Step 14.2-এর মূল regression-check এটাই।
--
-- ২) "problem participants can receive problem-topic broadcasts" — realtime.messages, FOR SELECT
--    TO authenticated, topic LIKE 'problem:%'
--    ফাইল: realtime_scoping_messages_solver_thread_isolation.sql:44-67 (মূল তৈরি),
--          realtime_scoping_step3_messages_broadcast.sql:27-57 (Step 13.8: idempotent drop+recreate
--          guard, হুবহু একই definition — CI-migration-order বাগ ফিক্স, নতুন logic না)
--    USING: extension='broadcast' AND topic LIKE 'problem:%' AND (is_admin OR EXISTS(problem p:
--           topic='problem:'||p.id AND (p.user_id=auth.uid() OR EXISTS(messages m: m.problem_id=p.id
--           AND (m.sender_id=auth.uid() OR m.receiver_id=auth.uid())))))
--    অর্থ: messages_select (#১)-এর broadcast-layer আয়না — কে লাইভ 'problem:<id>' topic subscribe
--    করে messages-এর broadcast পেতে পারে। Step 14.2 এটাও কভার করবে (table SELECT + broadcast SELECT
--    দুটোই মিলিয়ে "প্রকৃত regression-check")।
--
-- ৩) "users can receive own user-topic broadcasts" — realtime.messages, FOR SELECT TO authenticated
--    ফাইল: realtime_scoping_step1_notifications_broadcast.sql:16-23
--    USING: extension='broadcast' AND topic = 'user:'||auth.uid()::text
--    অর্থ: প্রতিটা authenticated user শুধু নিজের 'user:<uuid>' topic-ই subscribe করতে পারে। এই
--    একটা policy-ই ৭টা ভিন্ন টেবিলের broadcast trigger-কে সার্ভ করে (notifications, transactions,
--    withdrawals, gateway_payments, additional_charges, users, escrows — সবগুলোই একই
--    'user:<uuid>' topic-প্যাটার্ন ব্যবহার করে, তাই আলাদা policy লাগেনি — step2/step5 migration-এর
--    নিজস্ব কমেন্টে স্পষ্ট করে বলা আছে)। Step 14.3 (notifications) এটাই টেস্ট করবে।
--    ⚠️ **স্কোপ-নোট (14.3-এর জন্য)**: public.notifications টেবিলে কোনো টেবিল-লেভেল SELECT RLS
--    policy migrations-এ CREATE বা ALTER কোনোভাবেই পাওয়া যায়নি (নিচের "বোনাস আবিষ্কার" সেকশন
--    দেখুন) — তাই এই policy (#৩, broadcast-only) ছাড়া notifications-এর table-level visibility
--    এই repo থেকে কনফার্ম করা যাচ্ছে না। 14.3 তাই শুধু broadcast-layer regression-check করতে
--    পারবে নিশ্চিতভাবে; table-level SELECT policy টেস্ট করতে হলে আগে লাইভ প্রজেক্ট থেকে
--    (Supabase MCP/pg_policies) exact definition আনতে হবে, অন্যথায় অনুমান করে লেখা যাবে না (rule #5)।
--
-- ৪) "problem bids visibility broadcasts" — realtime.messages, FOR SELECT TO authenticated,
--    topic LIKE 'problem:%:bids'
--    ফাইল: realtime_scoping_step4_bids_broadcast.sql:34-57
--    USING: extension='broadcast' AND topic LIKE 'problem:%:bids' AND (is_admin OR EXISTS(problem p:
--           topic='problem:'||p.id||':bids' AND (p.user_id=auth.uid()
--           OR EXISTS(bids b: b.problem_id=p.id AND b.solver_id=auth.uid())
--           OR (p.status='OPEN' AND p.is_public=true AND p.is_user_deleted=false))))
--    অর্থ: admin, problem owner, সেই problem-এ bid থাকা solver, অথবা (deliberately broad,
--    Firebase-parity সিদ্ধান্ত অনুযায়ী) যেকোনো OPEN+public+non-deleted problem — এদের কেউ
--    'problem:<id>:bids' topic subscribe করতে পারে। Step 14.4 এটা কভার করবে।
--
-- ঐতিহাসিক/মৃত (টেস্টের স্কোপে না): "bid participants can receive problem-bids-topic broadcasts" —
-- realtime_scoping_step4_bids_drop_orphan_policy.sql-এ শুধু DROP করা হয়েছে (৪-এর আগের খসড়া নাম,
-- কখনো live-এ ছিল না migration-চেইনে পুরোপুরি apply হলে) — বর্তমানে অস্তিত্বহীন, টেস্ট লাগবে না।
--
-- ═══════════════════════════════════════════════════════════════════════════════════
-- বোনাস আবিষ্কার (মূল ৫-policy স্কোপের বাইরে, কিন্তু 14.4-এর "owner/solver-ভিত্তিক visibility"
-- বর্ণনার সাথে সরাসরি প্রাসঙ্গিক — পরের সেশনের জন্য নথিভুক্ত করা হলো, কাজ এখনই করা হয়নি)
-- ═══════════════════════════════════════════════════════════════════════════════════
-- `grep -rniE "alter policy"` দিয়ে আরও ২টা **core table-level** RLS policy পাওয়া গেছে যেগুলোর
-- কোনো CREATE POLICY *কোথাও* migrations-এ নেই (শুধু ALTER) — মানে rule #6-এর "কোনো real
-- CREATE TABLE নেই" ব্লকারের হুবহু সমান্তরাল সংস্করণ, কিন্তু policy-র জন্য: এই দুটো policy
-- সরাসরি লাইভ Supabase প্রজেক্টে তৈরি হয়েছিল, কখনো migration ফাইলে CREATE হয়নি।
--
-- • `problems_select` (public.problems) — ২বার ALTER হয়েছে:
--   `problems_select_allow_ended_bid_solver.sql` (পুরনো, bids-এ সরাসরি EXISTS সাবকোয়েরি —
--   এটাই bids_select-এর সাথে মিলে recursion বাগের কারণ ছিল, migration-এর নিজস্ব কমেন্টে স্বীকৃত)
--   → `fix_problems_select_bids_rls_recursion.sql` (ফিক্সড, `solver_has_ended_bid()` SECURITY
--   DEFINER হেল্পার দিয়ে) — **চূড়ান্ত/লাইভ USING clause**:
--     (auth.uid() = user_id) OR (auth.uid() = accepted_solver_id) OR is_admin(auth.uid())
--     OR (status='OPEN' AND is_public=true AND is_user_deleted=false)
--     OR public.solver_has_ended_bid(id, auth.uid())
--
-- • `bids_select` (public.bids) — `step23_bids_select_open_public_visibility.sql`-এ ALTER —
--   **চূড়ান্ত/লাইভ USING clause**:
--     (auth.uid() = solver_id) OR is_admin(auth.uid())
--     OR EXISTS(problems p: p.id=bids.problem_id AND p.user_id=auth.uid())
--     OR EXISTS(problems p: p.id=bids.problem_id AND p.status='OPEN' AND p.is_public=true
--                AND p.is_user_deleted=false)
--
-- **এই সেশনে real psql+pgTAP দিয়ে যাচাই করা হয়েছে (`local_pgtap_bootstrap.sh ci_verify`):**
-- এই দুটো ALTER POLICY statement fresh migration-apply-এ **"policy does not exist" এরর দিয়ে ব্যর্থ
-- হয়** — কিন্তু এটা **নতুন আবিষ্কার না**, CI_TEST_SUITE_PROGRESS.md-এ Step 8→9-এর সেশনেই এটা
-- known-environment-gap হিসেবে নথিভুক্ত হয়েছিল ("যে policy নেই তার উপর DROP/ALTER")। এই সেশনে
-- যেটা নতুন করে স্পষ্ট করা হলো: (ক) এই ব্যর্থতা শুধু এই sandbox-এর সীমাবদ্ধতা না — যেহেতু কোনো
-- migration ফাইলই কখনো `problems_select`/`bids_select` CREATE করে না, **আসল GitHub Actions CI-ও
-- (একই migration সেট, fresh ephemeral Postgres) একই এররে ব্যর্থ হওয়ার কথা**, pg_cron-এর মতো
-- "sandbox-এ নেই কিন্তু real Supabase image-এ আছে" case না — এটা যাচাই করা হয়নি (real CI run কখনো
-- হয়নি বলে জানা তথ্য অনুযায়ী), শুধু যুক্তি দিয়ে বলা হলো; (খ) যেহেতু ALTER উভয়ই ব্যর্থ হয় প্রথম থেকেই
-- (policy-ই নেই), migration-apply-order (fix_...আগে, problems_select_allow_...পরে — alphabetically
-- উল্টো ক্রমে, users/notifications broadcast-trigger বাগের মতোই একই প্যাটার্ন) বাস্তবে কখনো এই
-- দুটোর মধ্যে recursion বাগ manifest করায় না — উভয়ই "does not exist"-এ থেমে যায়, তাই order-বাগটা
-- এখানে latent/moot (কিন্তু live প্রজেক্টে timestamp-ক্রমে ঠিকভাবে চলে, তাই লাইভ প্রভাবিত না)।
--
-- **এর মানে 14.4 (bids)-এর জন্য**: যদি bids_select-এর "owner/solver-ভিত্তিক visibility"ও (শুধু
-- broadcast policy #৪ না) টেস্ট করতে হয়, তাহলে rule #6-এর প্যাটার্নেই একটা inferred baseline
-- `CREATE POLICY bids_select ...` (উপরের reconstructed USING clause দিয়ে, TEMPORARY/INFERRED
-- কমেন্ট সহ) schema stub-এ লাগবে যাতে ALTER-এর বদলে এই stub থেকেই policy-টা প্রথমে তৈরি হয় —
-- এই সেশনে সেই stub এখনো লেখা হয়নি (14.1-এর "কোনো ফাংশনাল টেস্ট না" নিয়ম মেনে), শুধু exact
-- USING clause-টা উপরে সংরক্ষণ করা হলো যাতে 14.4 আবার migration থেকে reverse-engineer না করে।
-- একইভাবে problems_select-ও (যেহেতু bids_select-এর ৩য়/৪র্থ OR-শর্ত problems টেবিল ছোঁয়) লাগতে
-- পারে — নির্ভর করে 14.4 কী পর্যন্ত কভার করতে চায় তার উপর, এই সিদ্ধান্ত সেই সেশনেই নেওয়া হবে।
--
-- ═══════════════════════════════════════════════════════════════════════════════════
-- RLS-enablement gap (14.2–14.4-এর জন্য গুরুত্বপূর্ণ) — এখনো enable করা হয়নি এই ফাইলে
-- ═══════════════════════════════════════════════════════════════════════════════════
-- কোনো migration/script-এ কোথাও `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` public.messages,
-- public.notifications, public.bids, বা realtime.messages-এর জন্য নেই (গ্রেপ করে যাচাই করা হয়েছে)।
-- Real Supabase প্রজেক্টে RLS platform-managed ভাবে আগে থেকেই enabled (realtime.messages) বা
-- Phase-2-এ সরাসরি enable করা হয়েছিল (core টেবিল) — কোনোটাই এই repo-র migration ফাইলে নেই।
-- ফলে policy থাকলেও RLS enable না থাকলে সেগুলো টেস্ট DB-তে সম্পূর্ণ no-op (superuser/table-owner
-- ছাড়া বাকি সবার জন্যও restriction কাজ করবে না, যদি RLS enable-ই না থাকে)।
--
-- **সিদ্ধান্ত (এই সেশনে নেওয়া, পরের সেশনগুলো অনুসরণ করবে):** এই shared helper ফাইলে RLS enable
-- করা হচ্ছে **না** — কারণ এই ফাইল সব test file-এর আগে একবার চলে (persistent, transaction-wrap
-- ছাড়া), তাই এখানে enable করলে **Step 1–13-এর পুরনো সব টেস্ট ফাইলে regression ঝুঁকি** থাকতো (ওই
-- ফাইলগুলো test.login_as()-এর পরে সরাসরি public.messages/notifications/bids ছুঁয়ে থাকতে পারে —
-- এই সেশনে পুরো audit করার সময়/স্কোপ ছিল না, rule #১ "কখনো ভাঙবে না" মেনে সতর্কতা হিসেবে বাদ
-- দেওয়া হলো)। **বদলে**: প্রতিটা 14.2/14.3/14.4 টেস্ট-ফাইল নিজের `BEGIN; ... ROLLBACK;` ব্লকের
-- **ভেতরে** নিজের প্রয়োজনীয় টেবিলে RLS enable করবে (DDL transactional, তাই ROLLBACK-এ এমনিতেই
-- আগের অবস্থায় ফিরে যায় — অন্য কোনো test file, এমনকি একই suite-এর পরের ফাইলও, এতে প্রভাবিত হবে
-- না, কারণ run_tests.sh প্রতিটা ফাইল আলাদা `psql -f` কলে/আলাদা কানেকশনে চালায়)। উদাহরণ প্যাটার্ন
-- (14.2 এভাবে শুরু করবে):
--   BEGIN;
--   ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
--   ALTER TABLE realtime.messages ENABLE ROW LEVEL SECURITY;
--   SELECT plan(N);
--   ... (fixtures superuser হিসেবে, তারপর test.login_as() দিয়ে সুইচ করে assertion) ...
--   SELECT * FROM finish();
--   ROLLBACK;

-- ---------------------------------------------------------------------------
-- Reusable helper functions (schema "test") — 14.2–14.4 এগুলো ব্যবহার করবে
-- ---------------------------------------------------------------------------

-- auth.uid() সুইচ করা: 00_helpers.sql-এর test.login_as(uuid)/test.logout() ইতিমধ্যেই এই কাজ
-- করে (set_config('role', 'authenticated'/'anon', true) + request.jwt.claim.sub) — এখানে
-- ডুপ্লিকেট করা হচ্ছে না, শুধু reuse করার সিদ্ধান্তটা এখানে স্পষ্ট করে লেখা হলো।

-- broadcast RLS policy-গুলো (#২/#৩/#৪) realtime.topic()-এর উপর নির্ভর করে, যেটা এই sandbox-এ
-- 'realtime.topic' নামের একটা GUC থেকে পড়ে (scripts/local_pgtap_bootstrap.sh:91-93)। নতুন এই
-- দুটো helper সেই GUC সেট/ক্লিয়ার করে — 00_helpers.sql-এ কিছু নেই এই জন্য, তাই এখানে যোগ হলো।
CREATE OR REPLACE FUNCTION test.set_topic(p_topic text) RETURNS void AS $$
BEGIN
  PERFORM set_config('realtime.topic', p_topic, true);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION test.clear_topic() RETURNS void AS $$
BEGIN
  PERFORM set_config('realtime.topic', '', true);
END;
$$ LANGUAGE plpgsql;

-- Dual-auth-context boilerplate কমানোর জন্য: নির্দিষ্ট user হিসেবে login করে, একটা dynamic SQL
-- (যেটা একটা bigint রিটার্ন করে — সাধারণত একটা COUNT(*)) চালিয়ে, logout করে রেজাল্ট রিটার্ন করে।
-- 14.2–14.4-এ এভাবে ব্যবহার হবে, উদাহরণ:
--   SELECT is(
--     test.count_as('<user-A-uuid>', 'select count(*) from public.messages'),
--     2::bigint,
--     'user A নিজের ২টা মেসেজ-ই দেখতে পারে'
--   );
-- ⚠️ p_sql-এ topic-নির্ভর broadcast policy টেস্ট করতে হলে আগে test.set_topic(...) আলাদা কল করে
-- নিতে হবে (এই helper নিজে topic ছোঁয় না, শুধু auth.uid() সুইচ করে) — কারণ topic একটা টেস্টে
-- একাধিক ভিন্ন মান নিতে পারে, user-switch থেকে independent।
CREATE OR REPLACE FUNCTION test.count_as(p_user_id uuid, p_sql text) RETURNS bigint AS $$
DECLARE
  v_count bigint;
BEGIN
  PERFORM test.login_as(p_user_id);
  EXECUTE p_sql INTO v_count;
  PERFORM test.logout();
  RETURN v_count;
END;
$$ LANGUAGE plpgsql;

-- দুটো dedicated RLS-fixture user (00_helpers.sql-এর ৪টা seed_users()-এর user স্পর্শ না করে,
-- 13_trigger_helpers.sql-এর "dedicated fixture user" প্যাটার্নে) — 14.2–14.4 এদের ঘিরে
-- problem/message/notification/bid ফিক্সচার বানাবে নিজের প্রয়োজন অনুযায়ী (একই দুইজনকেই
-- তিনটা ফাইলে reuse করা যাবে, prefix 'e1400001'/'e1400002' দিয়ে চেনা যায় "Step 14" ফিক্সচার)।
CREATE OR REPLACE FUNCTION test.seed_rls_users() RETURNS void AS $$
BEGIN
  INSERT INTO public.users (id, role, name, phone, balance, balance_user, has_user_role)
  VALUES
    ('e1400001-0000-0000-0000-000000000001', 'CLIENT', 'RLS Test User A', '01788800001', 0, 0, true),
    ('e1400002-0000-0000-0000-000000000002', 'SOLVER', 'RLS Test User B', '01788800002', 0, 0, true)
  ON CONFLICT (id) DO NOTHING;
END;
$$ LANGUAGE plpgsql;
