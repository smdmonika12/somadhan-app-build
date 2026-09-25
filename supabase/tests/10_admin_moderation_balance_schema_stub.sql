-- 10_admin_moderation_balance_schema_stub.sql
-- ⚠️ TEMPORARY / INFERRED — CI_TEST_SUITE_MASTER_PROMPT.md rule #6।
--
-- Step 7 (Admin moderation & balance), PART 1 of 2 — এই সেশনে কভার করা ৯টা
-- ফাংশনের জন্য দরকারি নতুন কলাম/টেবিল। কোনোটাই আগের কোনো *_schema_stub.sql
-- ফাইলে নেই (grep করে নিশ্চিত করা হয়েছে) — 01/05/07/08/09-এর কোনো CREATE
-- TABLE ভাঙা হয়নি, শুধু ALTER/নতুন TABLE যোগ হলো।
--
-- এই ফাইল নতুন টেবিল বানায় বলে GRANT আলাদা করে লাগেনি — 01_bidding_flow_schema_stub.sql-এ
-- `ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO anon,
-- authenticated, service_role` ইতিমধ্যেই বসানো আছে, আর সব stub ফাইলই একই role
-- (postgres, run_tests.sh/full-test.yml-এর psql কানেকশন) দিয়ে চলে — তাই এখানে
-- তৈরি নতুন টেবিলগুলোও (idempotency_keys, admin_audit_logs, admin_credentials)
-- স্বয়ংক্রিয়ভাবে সেই default privilege পায়, আলাদা GRANT দরকার নেই।

-- admin_set_banned/admin_set_restricted/admin_set_verified_badge-এর "role না
-- দিলে" (legacy, non-role-scoped) branch এই কলামগুলো ছোঁয় — Step 6-এ
-- is_banned_user/_solver, is_restricted_user/_solver, is_verified_badge
-- (non-suffixed legacy verified badge) আগে থেকেই আছে; ৩টা নতুন:
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_banned boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_restricted boolean NOT NULL DEFAULT false;
-- admin_set_verified_badge-এর role-scoped branch দুটো — recovered_admin_kyc_ban_role.sql
-- লাইন ~৩০২-৩০৭ থেকে হুবহু কলাম নাম:
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS verified_badge_user boolean NOT NULL DEFAULT false;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS verified_badge_solver boolean NOT NULL DEFAULT false;

-- admin_adjust_balance (দুটো overload) idempotency guard-এর জন্য ব্যবহার করে —
-- কোনো migration-এই CREATE TABLE নেই, শুধু ব্যবহার হয়। key = md5(...) হ্যাশ,
-- request_type শুধু লগিং-এর জন্য মনে হয় (কোথাও পড়া হয় না)।
CREATE TABLE IF NOT EXISTS public.idempotency_keys (
  key           text PRIMARY KEY,
  request_type  text,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- admin_adjust_balance (শুধু recovered_admin_money.sql-এর পুরনো সংজ্ঞায় — step36
-- override এই টেবিলে role কলাম আর লেখে না, নিচের "গুরুত্বপূর্ণ আবিষ্কার" নোট দ্রষ্টব্য)
-- ব্যবহার করে। role কলাম রাখা হলো (nullable) কারণ recovered ভার্সনে ছিল, future-proof।
CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
  id           text PRIMARY KEY,
  action_type  text,
  target_id    text,
  target_name  text,
  details      text,
  role         text,
  "timestamp"  timestamptz NOT NULL DEFAULT now()
);

-- admin_credentials_get_phone/_update/_verify_password (recovered_admin_credentials.sql)
-- একটা single-row (id=1) settings টেবিল ধরে নেয়। কলাম নাম সব ফাংশন-বডি থেকে হুবহু।
CREATE TABLE IF NOT EXISTS public.admin_credentials (
  id             integer PRIMARY KEY,
  phone          text,
  password_hash  text,
  updated_at     timestamptz NOT NULL DEFAULT now()
);

-- admin_credentials_update/_verify_password extensions.crypt()/extensions.gen_salt()
-- ব্যবহার করে (pgcrypto, Supabase কনভেনশন অনুযায়ী "extensions" schema-তে ইনস্টল করা
-- থাকে, public-এ না)। local Postgres-এ এটা আগে থেকে থাকে না, তাই বসাতে হলো।
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;
GRANT USAGE ON SCHEMA extensions TO anon, authenticated, service_role;

-- is_admin(uuid) স্টাব (01-stub-এ সংজ্ঞায়িত) SECURITY DEFINER RPC ভেতর থেকে কল হয়
-- বলে GRANT EXECUTE লাগে না (owner যেই permission দিয়ে চলে); নতুন করে কিছু বদলাতে
-- হয়নি এই ধাপে।

----------------------------------------------------------------------
-- PART 2 (Step 7, দ্বিতীয় সেশন) — বাকি ২১টা ফাংশনের জন্য নতুন কলাম/টেবিল
-- ⚠️ TEMPORARY / INFERRED (rule #6) — আগের কোনো stub ফাইলে নেই (grep -i দিয়ে নিশ্চিত)।
-- শুধু ALTER ... IF NOT EXISTS / CREATE TABLE IF NOT EXISTS — উপরের/আগের কিছু বদলানো হয়নি।
----------------------------------------------------------------------

-- admin_soft_delete_user (step32_8_admin_soft_delete_user.sql) নিজেই এই কলাম যোগ করে
-- (`add column if not exists`), কিন্তু migration চালানোর আগেই stub-only harness-এ ফাংশনটা
-- টেস্ট করার জন্য এখানেও বসানো হলো (idempotent — migration পরে চললেও কোনো সংঘর্ষ নেই)।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS is_deleted boolean NOT NULL DEFAULT false;

-- admin_broadcast_notification / admin_delete_notification_group `notifications.scheduled_for`
-- ব্যবহার করে — কোনো migration-এ এই কলামের ALTER/CREATE নেই (শুধু ফাংশন-প্যারামিটার হিসেবে
-- দেখা যায়), তাই ফাংশন-বডি থেকে reverse-engineer করা।
ALTER TABLE public.notifications ADD COLUMN IF NOT EXISTS scheduled_for timestamptz;

-- messages — admin_delete_message (id দিয়ে DELETE), admin_wipe_all_data (পুরো টেবিল)।
-- কলাম নাম recovered_disputes.sql / step32_95_system_event_message.sql /
-- recovered_admin_moderation.sql-এর INSERT থেকে হুবহু। FK: admin_wipe_all_data-এর নিজস্ব
-- কমেন্ট ("FK নির্ভরতা অনুযায়ী child-to-parent ক্রমে delete… pg_constraint দিয়ে verify")
-- অনুযায়ী আসল টেবিলে users/problems-এর দিকে FK আছে — সেই কারণে এখানেও nullable FK
-- রাখা হলো, যাতে wipe-এর delete-ক্রম টেস্টে সত্যিই যাচাই হয়। (Step 8-এ system-event
-- মেসেজ টেস্টের সময় এই টেবিল extend করতে হতে পারে — ALTER ADD COLUMN IF NOT EXISTS দিয়ে।)
CREATE TABLE IF NOT EXISTS public.messages (
  id                text PRIMARY KEY,
  problem_id        text REFERENCES public.problems(id),
  sender_id         uuid REFERENCES public.users(id),
  receiver_id       uuid REFERENCES public.users(id),
  sender_name       text,
  content           text,
  "timestamp"       timestamptz NOT NULL DEFAULT now(),
  is_read           boolean NOT NULL DEFAULT false,
  is_admin_message  boolean NOT NULL DEFAULT false,
  is_system_event   boolean NOT NULL DEFAULT false,
  system_event_type text
);

-- ratings — admin_delete_rating (id দিয়ে DELETE), admin_wipe_all_data। কলাম নাম
-- recovered_kyc_rating_reputation.sql:80-এর INSERT থেকে।
CREATE TABLE IF NOT EXISTS public.ratings (
  id            text PRIMARY KEY,
  problem_id    text REFERENCES public.problems(id),
  problem_title text,
  user_id       uuid REFERENCES public.users(id),
  solver_id     uuid REFERENCES public.users(id),
  stars         integer,
  comment       text,
  rater_role    text,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- reputation_events — শুধু admin_wipe_all_data ছোঁয়। কলাম নাম
-- recovered_kyc_rating_reputation.sql:146-এর INSERT থেকে (Step 8 এই টেবিল ব্যবহার করবে)।
CREATE TABLE IF NOT EXISTS public.reputation_events (
  id            text PRIMARY KEY,
  user_id       uuid REFERENCES public.users(id),
  event_type    text,
  problem_id    text REFERENCES public.problems(id),
  score_change  numeric,
  score_after   numeric,
  note          text,
  created_at    timestamptz NOT NULL DEFAULT now()
);

-- categories, faqs — শুধু admin_wipe_all_data-এ নাম দেখা যায়; কোনো ফাংশন/migration
-- কলাম পড়ে না, তাই সবচেয়ে ন্যূনতম INFERRED স্কিমা (id + সহজ টেক্সট কলাম)।
CREATE TABLE IF NOT EXISTS public.categories (
  id    text PRIMARY KEY,
  name  text
);
CREATE TABLE IF NOT EXISTS public.faqs (
  id        text PRIMARY KEY,
  question  text,
  answer    text
);
