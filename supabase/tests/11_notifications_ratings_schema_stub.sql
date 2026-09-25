-- 11_notifications_ratings_schema_stub.sql
-- ⚠️ TEMPORARY / INFERRED — CI_TEST_SUITE_MASTER_PROMPT.md rule #6।
--
-- Step 8 (Notifications & ratings) — create_notification, notify_admins, request_admin_assistance,
-- submit_rating, submit_reputation_event, mark_completion_result_seen, mark_dispute_result_seen,
-- mark_problem_seen, system_event_message-এর জন্য দরকারি কলাম/টাইপ-সংশোধন।
--
-- এই ফাইল কোনো আগের stub ভাঙে না — শুধু (ক) নতুন কলাম ADD, (খ) দুটো আগের ভুল-অনুমান করা
-- কলাম-টাইপ ঠিক করা (নিচে কারণ), (গ) একটা inferred CHECK। সবকিছু idempotent, কারণ
-- `scripts/run_tests.sh` প্রতিটা *_schema_stub.sql migration-এর *পরে* আবার apply করে
-- (`ADD COLUMN IF NOT EXISTS` / একই টাইপে `ALTER COLUMN … TYPE` / guarded ADD CONSTRAINT —
-- সবই দ্বিতীয়বার চালালে নিরীহ)।
--
-- ফাইলের নাম `11_` — run_tests.sh/full-test.yml `ls supabase/tests/*_schema_stub.sql | sort`
-- করে, তাই 09/10-এর পরে চলে (এই ফাইল ratings/reputation_events ALTER করে, যেগুলো 10-এ তৈরি)।
--
-- প্রতিটা কলামের টাইপ/উৎস কোথা থেকে নেওয়া (rule #5 — অনুমান করে লেখা হয়নি):
--   • কলামের *নাম* — সরাসরি ফাংশন-বডির INSERT/UPDATE/SELECT থেকে
--     (recovered_kyc_rating_reputation.sql, step32_85_mark_*.sql, recovered_disputes.sql,
--      step32_95_system_event_message.sql)।
--   • কলামের *টাইপ/ডিফল্ট* — app/src/main/java/com/example/data/remote/dto/*.kt-এর
--     @SerialName + টাইপ + কমেন্ট (UserDto, ProblemDto, RatingDto) থেকে; migration/সংজ্ঞা নেই বলে
--     এটাই একমাত্র উপলব্ধ প্রমাণ। এটা এখনো `supabase db dump` নয় — আসল dump পেলে এটাই replace হবে।

-- ---------------------------------------------------------------------------
-- (ক) নতুন কলাম
-- ---------------------------------------------------------------------------

-- users.reputation_score — "legacy/combined" স্কোর; submit_reputation_event ও (Step 7-এর)
-- admin RPC-গুলো পড়ে/লেখে। UserDto: `reputation_score: Double = 50.0`।
-- plain `numeric` (কোনো precision নয়) — ফাংশন স্কোর 0..100-এ clamp করে, নির্দিষ্ট scale অজানা।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS reputation_score numeric NOT NULL DEFAULT 50;

-- users.last_reputation_decay_check_at — INACTIVE_7_DAYS/INACTIVE_30_DAYS-এর ৭-দিনের rate-limit।
-- UserDto: `last_reputation_decay_check_at: String? // timestamptz`।
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS last_reputation_decay_check_at timestamptz;

-- problems: mark_completion_result_seen দুটো flag লেখে (ProblemDto: Boolean = false)।
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS completion_result_seen_by_user boolean NOT NULL DEFAULT false;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS completion_result_seen_by_solver boolean NOT NULL DEFAULT false;

-- problems: mark_problem_seen (ProblemDto: `String? // timestamptz`)।
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS user_last_seen_at timestamptz;
ALTER TABLE public.problems ADD COLUMN IF NOT EXISTS solver_last_seen_at timestamptz;

-- reputation_events.role — ৬-আর্গ submit_reputation_event প্রতিটা INSERT-এ `role` কলামে লেখে
-- (recovered_kyc_rating_reputation.sql:৫৩৭, ৬৬৬)। 10-stub-এর টেবিলে এই কলাম ছিল না
-- (তখন শুধু admin_wipe_all_data-এর জন্য বানানো হয়েছিল)। ⚠️ ReputationEventDto-তেও `role` নেই
-- (DTO পুরনো) — তাই টাইপ শুধু text ধরা হলো ('USER'|'SOLVER', ফাংশন থেকে)।
ALTER TABLE public.reputation_events ADD COLUMN IF NOT EXISTS role text;

-- ---------------------------------------------------------------------------
-- (খ) আগের stub-এর ভুল-অনুমান করা টাইপ ঠিক করা
-- ---------------------------------------------------------------------------

-- ⚠️ সংশোধন ১ — users.reputation_score_user / reputation_score_solver: 09_kyc_roles_schema_stub.sql
-- এগুলো `numeric(4,2)` ধরেছিল (Step 6, "টাইপ অনুমান, conservative")। কিন্তু numeric(4,2)-এর সর্বোচ্চ
-- ৯৯.৯৯, আর submit_reputation_event স্কোর `least(…, 100)`-এ clamp করে — অর্থাৎ ১০০ লিখতে গেলে
-- "numeric field overflow" হতো। এটা আসল বাগ না, stub-এর ভুল; `ADD COLUMN IF NOT EXISTS`
-- (09-এ) পরে টাইপ বদলাতে পারে না, তাই এখানে স্পষ্ট ALTER। শুধু প্রশস্ত করা হচ্ছে —
-- Step 6-এর টেস্ট এই দুই কলাম কেবল পড়ে/কপি করে, মান অপরিবর্তিত থাকে।
ALTER TABLE public.users ALTER COLUMN reputation_score_user TYPE numeric;
ALTER TABLE public.users ALTER COLUMN reputation_score_solver TYPE numeric;

-- ⚠️ সংশোধন ২ — problems.admin_assistance_requested_by: 05_job_release_escrow_schema_stub.sql
-- এটা `uuid` ধরেছিল (Step 3-এ শুধু `= null` করার জন্য লাগত, তাই ভুলটা ধরা পড়েনি)। কিন্তু
-- request_admin_assistance (recovered_disputes.sql:২৯৬) এতে `p_requester_role` (text: 'USER'/
-- 'SOLVER') লেখে, আর ProblemDto বলছে `adminAssistanceRequestedBy: String? // check: USER | SOLVER`
-- — অর্থাৎ আসল কলাম text। uuid থাকলে টেস্টে "column is of type uuid but expression is of
-- type text" আসত — stub-জনিত ভুল, আসল বাগ নয়। এখানে কোনো CHECK যোগ করা হলো না (শুধু DTO-র
-- কমেন্ট থেকে অনুমান — নির্দিষ্ট constraint অজানা; টেস্টও শুধু canonical 'USER'/'SOLVER' ব্যবহার করে)।
ALTER TABLE public.problems ALTER COLUMN admin_assistance_requested_by TYPE text USING admin_assistance_requested_by::text;

-- ---------------------------------------------------------------------------
-- (গ) inferred CHECK
-- ---------------------------------------------------------------------------

-- ratings.stars — RatingDto: `val stars: Int // check: 1..5`। submit_rating নিজে stars validate
-- করে না (কোনো range-check বডিতে নেই) — অর্থাৎ range-নিরাপত্তা শুধু টেবিল CHECK-এ, যেটা
-- migrations-এ নেই (live DB-তে সরাসরি বানানো)। CHECK না থাকলে টেস্ট "RPC ১..৫-এর বাইরের
-- stars গ্রহণ করে" এমন ভুল ধারণা দিত। constraint-এর আসল নাম অজানা — নামটা inferred।
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conname = 'ratings_stars_range_inferred' AND conrelid = 'public.ratings'::regclass
  ) THEN
    ALTER TABLE public.ratings
      ADD CONSTRAINT ratings_stars_range_inferred CHECK (stars BETWEEN 1 AND 5);
  END IF;
END $$;
