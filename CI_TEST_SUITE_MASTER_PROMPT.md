# Master Prompt — "সমাধান" App: Automated CI Test Suite (ধাপে ধাপে)

**🎉 স্ট্যাটাস (২০২৬-০৯-২৩): Step 1–20 সবক'টা `[x]`, GATE অনুযায়ী সম্পূর্ণ।** নিচে কোনো Step 21+
সংজ্ঞায়িত নেই। এই zip আবার আপলোড করলে পরের Claude session প্রথমেই এটা দেখে বুঝে নেবে যে "প্রথম
অসম্পূর্ণ ধাপ" খোঁজার কিছু নেই — ব্যবহারকারী নতুন কী করতে চান (backlog আইটেম নিয়ে নতুন Step, নাকি
সম্পূর্ণ নতুন কোনো ফিচার/testing effort) সেটা জিজ্ঞেস করে শুরু করবে, অনুমান করে কিছু শুরু করবে না।
বিস্তারিত চূড়ান্ত সারাংশ ও backlog তালিকা `CI_TEST_SUITE_PROGRESS.md`-এর শেষে "Step 20 সম্পূর্ণ"
সেকশনে।

এই prompt টা প্রতিটা নতুন Claude session-এর শুরুতে হুবহু paste করবেন,
সাথে আপনার app-এর zip ফাইলটা আবার আপলোড করবেন। Claude নিজেই বুঝে নেবে
কোন ধাপে আছি এবং কোনটা পরের কাজ।

---

## প্রজেক্ট প্রসঙ্গ (context)

আমার একটা Android app আছে ("সমাধান" / com.aistudio.somadhan.bdapp) —
Kotlin + Jetpack Compose + Supabase backend (Postgres + RPC functions)।
App-এ bidding, escrow, wallet, KYC, dispute, admin moderation ইত্যাদি
অনেকগুলো ফিচার আছে, backend logic মূলত `supabase/migrations/*.sql`-এ
থাকা SQL RPC ফাংশনগুলোতে লেখা, আর client-side dual-write logic
`app/src/main/java/com/example/data/repository/SomadhanRepository.kt`-এ।

**লক্ষ্য:** একটা CI pipeline (GitHub Actions) বানানো যেটা প্রতিটা push-এ
নিজে থেকে (১) APK build করবে, (২) প্রতিটা backend ফিচারের RPC-লজিক
টেস্ট করবে, (৩) RPC-লেয়ারে কোনো ambiguous/duplicate function overload
আছে কিনা স্ট্রাকচারালি স্ক্যান করবে, (৪) client-side dual-write
কল-সাইটগুলোর মধ্যে কোনগুলো cloud-fail হলে retry/rollback ছাড়াই stuck
থেকে যায় সেটা ধরবে, এবং (৫) realtime broadcast trigger-গুলো সঠিকভাবে
fire হচ্ছে কিনা যাচাই করবে — pass/fail রিপোর্ট দেবে, কোনো real money বা
production database ছোঁয়া ছাড়াই।

**পরিচিত সংখ্যা (case-insensitive স্ক্যান অনুযায়ী, শেষ যাচাই এই session-এ):**
১০৪টা SQL function, ১০টা trigger, ৫টা RLS policy। এই সংখ্যা নতুন migration
যোগ হলে বদলাতে পারে — Step 10/11-এ প্রতিবার fresh স্ক্যান করে যাচাই করা হয়,
এই সংখ্যাটা অন্ধভাবে বিশ্বাস করা হয় না।

**Base zip-এ আগে থেকেই কিছু টুল/ডকুমেন্ট আছে (অন্য একটা সমান্তরাল
"RPC sync fix" effort থেকে) — এগুলো re-create না করে ব্যবহার/যাচাই করবে:**
- `scripts/check_rpc_sync.sh` — Kotlin থেকে যেসব RPC কল হয় বনাম migration-এ
  যেগুলো define করা আছে তার diff নেয়। ⚠️ **এর regex নিজেই case-insensitive
  ও সঠিক** — কিন্তু `docs/RPC_INVENTORY_REPORT.md`-এ লেখা সংখ্যা (৪০টা
  migration-defined RPC) **stale/ভুল** — script-টা আসলে এখনই চালালে **১০৪**
  দেখাবে (নিজে যাচাই করে নিশ্চিত হওয়া গেছে)। মানে রিপোর্ট ফাইলটা একবার
  বানানো হয়েছিল (2026-09-17, "Step 1"), তারপর আর refresh হয়নি। এটাই
  প্রমাণ করে কেন Step 10/11-এর "প্রতিবার fresh স্ক্যান করো, cached
  তালিকা বিশ্বাস করো না" নিয়মটা জরুরি — কোনো নতুন ধাপে পুরনো
  `RPC_INVENTORY_REPORT.md`-এর সংখ্যা উদ্ধৃত করার আগে script আবার চালিয়ে
  নিশ্চিত হবে।
- `docs/OUTBOX_RETRY_DESIGN.md` — outbox/retry প্যাটার্নের একটা design-only
  স্পেক (কোনো কোড implement হয়নি এখনো)। Step 12-এর সাথে সরাসরি সম্পর্কিত —
  Step 12 করার আগে এই ডকুমেন্ট পড়ে নেবে, ডুপ্লিকেট ডিজাইন-চিন্তা না করে
  এটার উপর ভিত্তি করে এগোবে।

## অলঙ্ঘনীয় নিয়ম (never break these)

1. **আসল app কোড (Kotlin/Compose, `.env`, `build.gradle.kts`, migrations)
   কখনো এডিট/মুছবে না।** শুধু নতুন ফাইল যোগ হবে এই জায়গাগুলোতে:
   - `.github/workflows/`
   - `supabase/tests/`
   - `scripts/`
   - `app/src/test/` (শুধু নতুন test ফাইল, existing test ফাইল এডিট না)
   - `CI_TEST_SUITE_PROGRESS.md` (root-এ)
1a. **সীমিত ব্যতিক্রম — শুধু `Step 12.x` (নিচের তালিকা) সেশনগুলোর জন্য।** ব্যবহারকারী
   (অ্যাপের মালিক) স্পষ্টভাবে সিদ্ধান্ত নিয়েছেন: Step 12-এ ধরা পড়া বাগগুলো ঠিক না করে
   Step 13-এ যাওয়া হবে না। তাই **শুধু** প্রতিটা Step 12.x-এর "অনুমোদিত ফাইল/ফাংশন" অংশে
   নাম ধরে লেখা জায়গাগুলোতে production কোড এডিট করা যাবে (এবং শুধু Step 12.2-এ existing
   `DualWriteGapTest.kt`-এর সাইট-খোঁজার পদ্ধতি) — এর বাইরে rule #1 আগের মতোই অলঙ্ঘনীয়। ব্যতিক্রমের শর্ত:
   - এডিট হবে **ন্যূনতম ও লক্ষ্যভিত্তিক** (নির্দিষ্ট `.onFailure {}` ব্লক আর মিলিয়ে
     `OutboxRpcDispatcher.kt`-এর branch) — কোনো refactor, rename, বা "সুযোগ পেয়ে ঠিক করে দেওয়া" নয়।
   - **`.env`, `build.gradle.kts`, `supabase/migrations/` কখনোই না।** migration লাগলে শুধু
     `docs/proposed_migrations/PROPOSED_<নাম>.sql` লিখবে (Step 40-এর মতো), ব্যবহারকারী নিজে
     review করে বসাবেন।
   - money-related retry যোগ করার আগে ওই RPC **idempotent কিনা** (একই কল দুইবার চললে টাকা
     দুইবার নড়ে কিনা) migration বডি খুলে যাচাই করবে। idempotent না হলে বা নিশ্চিত না হলে
     সেই সাইটে **retry যোগ করবে না** — `BLOCKED (idempotency)` হিসেবে progress doc-এ লিখে
     ব্যবহারকারীর সিদ্ধান্তের জন্য রাখবে। অন্ধভাবে retry যোগ করলে duplicate টাকা-নড়াচড়ার
     নতুন বাগ তৈরি হতে পারে।
   - প্রতিটা এডিটেড ফাইলের নাম ও কী বদলেছে (ফাংশন-নামসহ) সেশনের শেষ সারাংশে তালিকা করে
     দেবে, যাতে ব্যবহারকারী নিজের আসল git repo-তে একই বদল বসাতে পারেন।
2. টেস্টগুলো **local/disposable database**-এর বিপরীতে চলবে
   (GitHub Actions-এর ভেতরে ephemeral Postgres সার্ভিস, migrations
   fresh apply করা হয় প্রতি রানে) — কোনো production/staging real data
   বা real payment gateway ছোঁয়া হবে না।
2a. **`.env`-এ থাকা কোনো real value (Supabase URL/anon key ইত্যাদি)
   কখনো কোনো committed ফাইলে (workflow YAML, test SQL, script, doc)
   copy/hardcode করা যাবে না** — `.gitignore`-এই এই ফাইলটা "DO NOT
   commit" হিসেবে চিহ্নিত করা আছে, আর rule #2 অনুযায়ী এমনিতেও CI-তে
   real Supabase credential লাগার কথা না (সবকিছু disposable local DB-তে
   চলে)। zip-এ `.env` থাকা শুধু Claude-এর context/reference-এর জন্য,
   commit বা reuse করার জন্য না।
3. একবারে **শুধু একটা ধাপ (নিচের তালিকা থেকে)** করবে, পুরোটা একসাথে না —
   টুল/context limit বাঁচানোর জন্য।
4. প্রতিটা ধাপ শেষে **`CI_TEST_SUITE_PROGRESS.md` আপডেট করে তবেই থামবে** (নিচে ফরম্যাট
   দেওয়া আছে), যাতে পরের session ঠিক জায়গা থেকে শুরু করতে পারে।
5. অনিশ্চিত column/table নাম পেলে অনুমান করে চালিয়ে যাবে না — যা schema-তে
   সত্যিই আছে সেটা zip থেকে grep/view করে যাচাই করে তবেই টেস্ট লিখবে।
5a. **সব `grep`/function-নাম স্ক্যান অবশ্যই case-insensitive (`-i`) করতে হবে।**
   এই codebase-এ কিছু migration `CREATE OR REPLACE FUNCTION` (বড় হাতের)
   আর কিছু `create or replace function` (ছোট হাতের) লেখা — case-sensitive
   grep ব্যবহার করলে migrations-এর প্রায় এক-তৃতীয়াংশ ফাংশন (এবং সবগুলো
   trigger, যেগুলো লেখা হয়েছে `create trigger` ছোট হাতে) সম্পূর্ণ miss হয়ে
   যায় — এটা আগে একবার সত্যিই ঘটেছিল (৬৬ থেকে আসল সংখ্যা ১০৪ বেরিয়েছিল)।
6. **জানা ব্লকার:** এই zip-এ কোনো real table schema (`CREATE TABLE`) বা
   `is_admin()`/`resolve_commission_rate()`-এর মতো helper ফাংশন নেই
   (বিস্তারিত `CI_TEST_SUITE_PROGRESS.md`-এ)। প্রতিটা নতুন ধাপে যে টেবিল/হেল্পার নতুন
   লাগবে, সেটার জন্য RPC বডি থেকে reverse-engineer করা একটা
   `NN_<feature>_schema_stub.sql` বানাবে (স্পষ্টভাবে "TEMPORARY/INFERRED"
   কমেন্ট সহ, আগের ধাপের stub-গুলো না ভেঙে) — যতদিন না আসল
   `supabase db dump --schema public` পাওয়া যায়। এই inferred stub-এর
   উপর ভিত্তি করে লেখা প্রতিটা টেস্টের ফলাফল (pass বা fail, দুটোই)
   সম্ভাব্যভাবে schema-mismatch-জনিত ভুল হতে পারে — বাস্তব দুম্প পাওয়া
   গেলে এটাকেই priority দিয়ে replace করতে হবে।

## রিপোর্টের নিয়ম

প্রতিটা ধাপের শেষে ছোট একটা সারাংশ দেবে:
- কোন ফাংশনগুলোর টেস্ট লেখা হলো
- schema verify করতে গিয়ে কোনো mismatch/সমস্যা পেলে সেটা কী
- পরের ধাপ কী

---

## ধাপের তালিকা (Step list — একটা session = একটা ধাপ)

- [x] **Step 0 — Scaffold:** workflow file (`full-test.yml`) + test
      helper (`00_helpers.sql`, user login simulate করার জন্য) তৈরি
      হয়ে গেছে।
- [x] **Step 1 — Bidding flow:** `accept_bid`, `cancel_bid`, `reject_bid`,
      `accept_direct_contract`, `decline_direct_contract` — বিস্তারিত
      `CI_TEST_SUITE_PROGRESS.md`-এ (কিছু edge case বাকি আছে, extend করা যাবে)
      **[পুনরায় খুলতে হবে — নতুন case-insensitive স্ক্যানে আরও ২টা পাওয়া
      গেছে, এখনো test করা হয়নি]:** `solver_has_ended_bid` (bid-visibility
      helper predicate, সম্ভবত RLS policy-তে ব্যবহৃত), `user_delete_problem`
- [x] **Step 2 — Instant jobs / broadcasting:** `broadcast_instant_job`,
      `cancel_instant_job`, `expire_broadcasting_instant_job`,
      `mark_job_started`, `mark_solver_arrived`, `mark_solver_on_way`,
      `solver_cancel_job`, `expire_stale_instant_jobs` (আলাদা,
      `pg_cron`-দিয়ে সিস্টেম-ওয়াইড sweep — `expire_broadcasting_instant_job`
      থেকে গুলিয়ে ফেলবে না, নাম কাছাকাছি হলেও দুটো আলাদা ফাংশন),
      `clear_solver_cancelled_notice`, `update_solver_live_location`,
      `sync_solver_free_job_quota`
- [x] **Step 3 — Job release & escrow (money flow):** ⚠️ **চেকবক্স-সিঙ্ক ফিক্স (২০২৬-০৯-২২ সেশন, Step 13.1-এর আগে sanity-check-এ ধরা পড়েছে):** এই ধাপ progress doc অনুযায়ী আগেই ("Step 3 — সম্পূর্ণ (PART 1 + PART 2)") শেষ হয়েছিল, কিন্তু কোনো এক আগের সেশন এখানকার চেকবক্স `[x]` করতে ভুলে গিয়েছিল যদিও progress.md-এ `[x]` লেখা ছিল। এই সেশনে শুধু checkbox sync করা হলো, নতুন কোনো কাজ হয়নি — বিস্তারিত:
      `request_job_release`, `cancel_job_release_request`,
      `reject_job_release_request`, `release_escrow`,
      `refund_escrow_once`, `request_extra_amount`,
      `respond_additional_charge`, `increment_escrow_extra_amount`,
      `record_gateway_payment_log`, `mark_additional_charge_settled`,
      `system_track_extra_payment_miss`
- [x] **Step 4 — Wallet & withdrawals:** `deposit_money_via_gateway`,
      `request_wallet_deposit`, `request_withdrawal`,
      `process_withdrawal` ⚠️ চেকবক্স-সিঙ্ক ফিক্স (উপরের Step 3-এর নোট দ্রষ্টব্য) — progress.md-এ
      "PART 1 of 2 সম্পূর্ণ" ও "PART 2 of 2 সম্পূর্ণ" দুটোই আগে থেকে লেখা ছিল।
- [x] **Step 5 — Disputes:** `raise_dispute`, `resolve_dispute`,
      `settle_dispute`, `admin_manually_flag_dispute`,
      `resolve_dispute_split` ⚠️ চেকবক্স-সিঙ্ক ফিক্স (উপরের নোট দ্রষ্টব্য) — progress.md-এ
      "সম্পূর্ণ (PART 1 + PART 2)" আগে থেকে লেখা ছিল।
- [x] **Step 6 — KYC & roles:** `submit_kyc`, `admin_approve_kyc`,
      `admin_reject_kyc`, `admin_revoke_kyc`,
      `switch_role_get_or_create_linked_profile`, `admin_change_role`,
      `sync_linked_account_profile`, `generate_unique_display_uid`
- [x] **Step 7 — Admin moderation & balance:** ⚠️ চেকবক্স-সিঙ্ক ফিক্স (Step 3-এর নোট দ্রষ্টব্য) —
      progress.md-এ "PART 1 of 2" ও "PART 2 of 2" দুটোই সম্পূর্ণ লেখা ছিল, real
      Postgres+pgTAP real-run verified (`10_part1` ৭১/৭১ ok, `10_part2` ১৯৪/১৯৪ ok)।
      `admin_adjust_balance`,
      `admin_confirm_gateway_deposit`, `admin_credentials_*`,
      `admin_delete_message`, `admin_delete_notification_group`,
      `admin_delete_rating`, `admin_force_cancel_instant_job`,
      `admin_notify_user`, `admin_reassign_solver`,
      `admin_refund_and_reopen_problem`,
      `admin_remove_category_from_solvers`, `admin_set_banned`,
      `admin_set_restricted`, `admin_set_verified_badge`,
      `admin_update_problem_budget`, `admin_update_problem_status`,
      `admin_broadcast_notification`, `log_admin_action`,
      `admin_update_direct_contract_status` (Step 1 থেকে সরানো হলো —
      এটা `recovered_admin_moderation.sql`-এ আছে, bidding-এ না)।
      **[case-insensitive স্ক্যানে নতুন পাওয়া, আগে সম্পূর্ণ miss হয়ে
      গিয়েছিল]:** `admin_get_dashboard_metrics` (⚠️ সবচেয়ে জরুরি — এটাই
      সেই ফাংশন যেটার বাগের কারণে admin overview-তে metrics ফাঁকা
      দেখাচ্ছিল বলে রিপোর্ট হয়েছিল), `admin_wipe_all_data` (⚠️ destructive,
      বিশেষ সতর্কতার সাথে টেস্ট করতে হবে — শুধু disposable DB-তেই,
      এবং টেস্ট নিজেই যেন ভুলে অন্য টেস্টের data মুছে না দেয়),
      `admin_soft_delete_user`, `admin_reconcile_escrow_states`,
      `admin_repair_missing_refunds`, `admin_cleanup_duplicate_refunds`,
      `admin_update_withdrawal_trx_id`, `admin_reset_free_job_quota`,
      `admin_bulk_reset_free_job_quota`, `admin_reset_miss_cycle`,
      `owner_reset_orphaned_accepted_bid`
- [x] **Step 8 — Notifications & ratings:** ⚠️ চেকবক্স-সিঙ্ক ফিক্স (Step 3-এর নোট দ্রষ্টব্য) —
      progress.md-এ "সম্পূর্ণ (PART 1 + PART 2)" আগে থেকেই লেখা ছিল, real-run verified
      (`11_part1` ১০১/১০১ ok, `11_part2` ১৬০/১৬০ ok)। `create_notification`,
      `notify_admins`, `request_admin_assistance`, `submit_rating`,
      `submit_reputation_event`, `mark_completion_result_seen`,
      `mark_dispute_result_seen`, `mark_problem_seen`,
      `system_event_message`
- [x] **Step 9 — Report generator polish:** ⚠️ চেকবক্স-সিঙ্ক ফিক্স (Step 3-এর নোট দ্রষ্টব্য) —
      progress.md-এ "✅ Step 9 সম্পূর্ণ" আগে থেকেই লেখা ছিল (report polish + end-to-end dry
      run দুটোই)। `scripts/generate_report.sh`
      কে আরও readable করা (feature নাম অনুযায়ী group করা রিপোর্টে),
      এবং পুরো pipeline একবার end-to-end ড্রাই-রান করে দেখা।
- [x] **Step 10 — Coverage audit + বাদ পড়া function-গুলো (gap-fill):** সম্পূর্ণ, বিস্তারিত
      CI_TEST_SUITE_PROGRESS.md-এ ("Step 10 সম্পূর্ণ" সেকশন)। সারাংশ: fresh স্ক্যানে ১৩টা function-এর
      test coverage ছিল না — ১০টা broadcast trigger-function (ইচ্ছাকৃতভাবে Step 13-এর জন্য রাখা
      হলো), বাকি ৩টা (`admin_reconcile_user_balances`, `admin_send_message_to_problem_chat`,
      `system_notify_48hour_auto_release`) নতুন `12_coverage_gap_fill_part1.sql`-এ কভার করা হলো
      (plan 38, real psql+pgTAP-এ ৩৮/৩৮ pass; পুরো suite ১০৩৫/১০৩৫)। master prompt-এ তালিকাভুক্ত
      বাকি সব নাম আগে থেকেই test-covered ছিল (আলাদাভাবে verify করা হয়েছে, নিচের অরিজিনাল তালিকা
      অপরিবর্তিত রাখা হলো ইতিহাসের জন্য):
      Step 9 শেষ হওয়ার পরও এই ধাপ স্কিপ করা যাবে না। নিচের function-গুলো
      migrations-এ আছে কিন্তু আগে কোনো step-তালিকাতেই ছিল না — এখন Step
      1/2/3/5/6/7/8-এ যোগ করে দেওয়া হয়েছে, কিন্তু সেগুলোতে এখনো `[x]`
      মার্ক হয়নি, তাই এই ধাপে সেগুলোর টেস্ট লেখা এখনো বাকি ধরতে হবে:
      - `admin_reconcile_user_balances` (Step 7) — সবচেয়ে গুরুত্বপূর্ণ,
        এটাই সেই টুল যেটা আগে প্রোডাকশনে balance mismatch ঠিক করতে
        ব্যবহার হয়েছিল
      - `request_additional_charge`, `user_confirm_extra_amount`,
        `user_reject_extra_amount` (Step 3)
      - `withdraw_dispute` (Step 5)
      - `admin_send_message_to_problem_chat` (Step 8)
      - `system_notify_48hour_auto_release` (Step 8)
      - Step 1/2/3/5/6/7/8-এ এইমাত্র যোগ করা বাকি সব নতুন function
        (`solver_has_ended_bid`, `user_delete_problem`,
        `expire_stale_instant_jobs`, `admin_get_dashboard_metrics`,
        `admin_wipe_all_data` ইত্যাদি — পুরো তালিকার জন্য উপরের
        step-গুলো দেখো)

      এই ধাপে যা করতে হবে:
      1. `supabase/migrations/*.sql`-এ থাকা সব
         `create or replace function public.*` নাম বের করে
         **অবশ্যই case-insensitive grep দিয়ে**
         (`grep -rhoiP "(?<=create (or replace )?function public\\.)\\w+"`
         — বড়/ছোট হাতের অক্ষর দুটোই ধরার জন্য; শুধু বড়-হাতের প্যাটার্ন
         দিয়ে স্ক্যান করলে migrations-এর প্রায় এক-তৃতীয়াংশ ফাংশন miss
         হয়ে যায়, এটা আগে একবার সত্যিই ঘটেছিল)
         `supabase/tests/`-এ যে function-গুলোর test file আছে তার সাথে
         মিলিয়ে একটা fresh coverage-diff বানাবে — উপরের তালিকা পুরনো
         হয়ে গেলে (নতুন migration যোগ হলে) নতুন করে যাচাই করবে, শুধু
         এই তালিকা অন্ধভাবে বিশ্বাস করবে না।
      2. প্রতিটার জন্য (বাকি ধাপগুলোর মতোই) happy path + কমপক্ষে একটা
         failure/edge case সহ pgTAP test লিখবে; সংশ্লিষ্ট নতুন যে
         টেবিল/কলাম লাগে সেটা schema stub-এ (inferred/TEMPORARY কমেন্ট
         সহ) যোগ করবে।
      3. `admin_reconcile_user_balances`-এর জন্য বিশেষভাবে একটা টেস্ট
         রাখবে যেখানে ইচ্ছাকৃতভাবে balance_user/balance_solver-এ mismatch
         বানিয়ে (accept_bid ও release_escrow-এর মাঝে সরাসরি row এডিট করে)
         verify করবে যে reconcile ফাংশন সেটা ঠিক করে দেয়।
      4. এই ধাপ শেষে `CI_TEST_SUITE_PROGRESS.md`-এ নিশ্চিত করে লিখবে যে migrations-এর
         সব function-এর against কোনো test file আছে কিনা (১০০% নাম-ধরে
         মিলিয়ে), gap থাকলে সেটাও নোট করবে।

- [x] **Step 11 — Duplicate/ambiguous RPC-overload scanner (স্ট্রাকচারাল,
      pgTAP না):** pgTAP টেস্ট সরাসরি SQL দিয়ে ফাংশন কল করে বলে
      PostgREST-এর HTTP/JSON RPC layer-এ যে ambiguous-overload বাগ হয়
      (যেমন `admin_adjust_balance`-এ পাওয়া গেছে — একই নামে দুটো আলাদা
      argument-signature-এর function, PostgREST schema cache-এ
      PGRST203 "Could not choose the best candidate function" ঝুঁকি)
      সেটা কোনো pgTAP টেস্ট কখনো ধরবে না, even যদি সেই ফাংশনের নিজস্ব
      logic টেস্ট pass করে। এই ধাপে:
      1. `scripts/scan_duplicate_overloads.sh` (বা `.py`) লিখবে যেটা
         `supabase/migrations/*.sql`-এ প্রতিটা
         `create (or replace) function public.<name>(<args>)` বের করে
         **(case-insensitive ম্যাচ — বড়/ছোট হাতের অক্ষর দুটোই, rule
         #5a দেখো)**, একই `<name>`-এর জন্য কতগুলো **আলাদা**
         argument-signature পাওয়া গেছে গোনে (শুধু নাম না, পুরো signature
         সহ) — একাধিক থাকলে non-zero exit code দিয়ে সেই ফাংশনগুলোর
         তালিকা report করবে।
      2. এই স্ক্রিপ্টটা `full-test.yml`-এ একটা আলাদা, দ্রুত job হিসেবে
         বসাবে (Postgres service লাগবে না — শুধু text scan, তাই সবচেয়ে
         সস্তা/দ্রুততম check, আগে রান করানো ভালো যাতে দ্রুত fail করে)।
      3. এই মুহূর্তে যে function-গুলোতে duplicate signature পাওয়া যাবে
         (আগে identify করা known candidates: `admin_adjust_balance`,
         `submit_reputation_event`, `admin_set_banned`,
         `admin_set_restricted`, `create_notification`,
         `admin_notify_user`, `log_admin_action` — কিন্তু স্ক্রিপ্ট
         নিজে থেকেই migrations স্ক্যান করে নতুন/বাদ-পড়া candidate
         থাকলে সেগুলোও ধরবে, এই তালিকা শুধু sanity-check reference)
         সেগুলো `CI_TEST_SUITE_PROGRESS.md`-এ আলাদা করে "KNOWN AMBIGUOUS OVERLOAD —
         DB-তে fix (DROP FUNCTION পুরনো signature) ছাড়া client RPC call
         ambiguous থাকতে পারে" হিসেবে নোট করবে। এই ধাপ শুধু detect করে,
         কোনো migration/DROP FUNCTION নিজে থেকে লিখবে না (rule #1-এর
         আওতায়, সেটা মানুষের review-সাপেক্ষে আলাদা কাজ)।
      4. `scan_duplicate_overloads.sh`-এর জন্যও একটা ছোট self-test
         রাখবে (একটা fixture ফোল্ডারে ইচ্ছাকৃত duplicate signature
         দিয়ে যাচাই করা যে স্ক্রিপ্টটা আসলেই ধরতে পারছে) — যাতে scanner
         নিজেই ভবিষ্যতে silently ভেঙে না যায়।

- [x] **Step 12 — Kotlin client-side dual-write coverage:** সম্পূর্ণ, বিস্তারিত
      `CI_TEST_SUITE_PROGRESS.md`-এ (PART 1: inventory + classification; PART 2:
      `DualWriteGapTest.kt` — mockk/build.gradle.kts-এডিট ছাড়া সম্ভব না হওয়ায় static
      structural verification দিয়ে লেখা। **২০২৬-০৯-২০: ব্যবহারকারীর Windows মেশিনে real-run
      হয়েছে — `26 tests completed, 26 failed`, সবগুলোই প্রত্যাশিত "UNPROTECTED" মেসেজ সহ।**
      একই রানে production কোডের ২টা আগে-থেকে-থাকা compile error (`AdminUserLookupView.kt`,
      `HomeScreen.kt`) ধরা পড়ে ও ঠিক করা হয়)। আসল বাগগুলো ঠিক করার কাজ নিচের Step 12.x-এ।
      `SomadhanRepository.kt`-এ ৮০+ জায়গায় "instant local Room echo +
      best-effort Supabase RPC dual-write (fail করলে শুধু log)" প্যাটার্ন
      আছে, কিন্তু মাত্র একটা অংশে (`enqueueOutboxRetry(...)` কল থাকা
      সাইটগুলোতে) সেই fail থেকে recover করার (retry/outbox) ব্যবস্থা
      আছে। বাকি সাইটগুলোতে RPC fail করলে local state (balance/escrow/
      ইত্যাদি) cloud-এর সাথে চিরস্থায়ীভাবে out-of-sync থেকে যায় — এটাই
      `acceptBid()`-এর escrow বাগের root cause ছিল। এই ধাপে:
      1. `SomadhanRepository.kt`-এ `.onFailure {` ব্লক আছে এমন প্রতিটা
         dual-write কল সাইট বের করবে (`grep -n "\.onFailure {"`), আর
         প্রতিটার আশেপাশে `enqueueOutboxRetry(` আছে কিনা চেক করে একটা
         তালিকা বানাবে: "protected" (retry আছে) বনাম "unprotected"
         (retry নেই, local-only optimistic write)।
      2. এই তালিকাটা `CI_TEST_SUITE_PROGRESS.md`-এ money-related (balance/escrow/
         transaction ছোঁয়া) আর non-money — এই দুই ভাগে সাজিয়ে রাখবে;
         money-related unprotected সাইটগুলোই সবচেয়ে বেশি priority।
      3. money-related unprotected সাইটগুলোর জন্য
         `app/src/test/java/com/example/repository/DualWriteGapTest.kt`
         (বা উপযুক্ত নামে) — একটা JVM/Robolectric-স্তরের test লিখবে
         যেখানে Supabase RPC কল mock/fake করে ইচ্ছাকৃতভাবে fail করানো
         হয়, তারপর assert করে যে হয় (ক) outbox-এ retry entry queue
         হয়েছে, অথবা (খ) local optimistic write rollback হয়ে গেছে —
         দুটোর একটাও না হলে টেস্ট fail করবে (ঠিক এই না-হওয়াটাই এখন
         বাস্তব বাগ, তাই প্রথম রানে এই টেস্ট(গুলো) fail দেখানোর কথা)।
      4. `full-test.yml`-এর `build-apk` job-এর "Run local (JVM) unit
         tests" step-এই এটা চলবে (আলাদা job লাগবে না) — নিশ্চিত করবে যে
         নতুন test file আগে থেকে থাকা `./gradlew test` কমান্ডেই ধরা
         পড়ছে, আলাদা কিছু wire করা লাগবে না।
      5. যেসব unprotected সাইট money-related না (শুধু UI
         convenience/notification জাতীয়), সেগুলোর জন্য এই ধাপে টেস্ট
         লেখা বাধ্যতামূলক না — `CI_TEST_SUITE_PROGRESS.md`-এ শুধু তালিকা করে future
         backlog হিসেবে রেখে দিলেই চলবে।

### 🔧 Step 12.x — Step 12-এ ধরা বাগগুলো ঠিক করার ধাপ (Step 13-এর আগে বাধ্যতামূলক)

**🚧 GATE (২০২৬-০৯-২১ সরানো হলো — ✅ 12.1–12.12 সবক'টা `[x]`):** নিচের `12.1`–`12.12` (এবং 12.10c/12.10d/12.10e)
সবক'টা `[x]` (এবং প্রতিটার Windows-verification পাস) হয়ে গেছে — **Step 13 এখন শুরু করা যায়।**
(ঐতিহাসিক প্রসঙ্গ: ব্যবহারকারী স্পষ্টভাবে বলেছিলেন সব fix শেষ করেই Step 13-এ যেতে চান, তাই Claude
শুধু "প্রথম অসম্পূর্ণ `[ ]` ধাপ" নিত — তালিকার ক্রমেই (12.1 → 12.2 → …), লাফ দিত না। 12.12-এর একমাত্র
ব্যতিক্রম/backlog নোট: `ExampleRobolectricTest`-এর ৩টা Robolectric-SDK-36 fail, ব্যবহারকারীর সিদ্ধান্তে
out-of-scope ধরা হয়েছে — বিস্তারিত 12.12-এর নিচের এন্ট্রি ও progress doc-এ।)
⚠️ **Step 12.8 তিনটা উপ-ধাপে ভাগ করা (12.8a → 12.8b → 12.8c, ব্যবহারকারীর অনুরোধে ২০২৬-০৯-২১)** —
12.9-এ যাওয়ার আগে তিনটাই `[x]` হতে হবে, প্রতিটা আলাদা সেশনে, এই ক্রমেই।

**প্রতিটা 12.x সেশনের সাধারণ নিয়ম** (নিচের প্রতিটা ধাপে আলাদা করে আর লেখা হয়নি):
- একটা সেশন = শুধু ওই একটা 12.x ধাপ। শেষ করে zip + সারাংশ দিয়ে থেমে যাবে।
- **সেশনের শুরুতে**, ব্যবহারকারী যদি আগের ধাপের `WINDOWS RESULT:` (নিচের ফরম্যাটে) paste করে থাকেন,
  সেটা আগে প্রসেস করবে: pass হলে আগের ধাপ "Windows-verified" হিসেবে `CI_TEST_SUITE_PROGRESS.md`-এ
  লিখবে; প্রত্যাশা না মিললে (compile error/অপ্রত্যাশিত fail) **এই সেশনের কাজ সেটাই ঠিক করা** —
  নতুন ধাপে যাবে না।
- sandbox-এ Gradle/Maven host সাধারণত `host_not_allowed`। শুরুতে `curl -sI https://services.gradle.org`
  দিয়ে দেখবে; পৌঁছালে নিজেই টেস্ট চালাবে, না পৌঁছালে (স্বাভাবিক) নিচের Windows কমান্ড ব্যবহারকারীকে
  দেবে। **নিজে রান না করে "pass করবে" বলবে না** — কোড-লেভেল স্ট্যাটিক যাচাই আর real-run আলাদা করে লিখবে।
- প্রতিটা fix-এর ধরন একই (৩ ধাপ): (ক) `SomadhanRepository.kt`-এর নির্দিষ্ট `.onFailure {}` ব্লকে
  `enqueueOutboxRetry(rpcName = "...", params = JsonObject(...), error = e)` — বিদ্যমান উদাহরণের
  (`admin_approve_kyc`, `refund_escrow_once` ইত্যাদি) হুবহু প্যাটার্নে; (খ) `OutboxRpcDispatcher.kt`-এ
  ওই `rpcName`-এর branch, যার param key-গুলো (ক)-এর সাথে অক্ষরে অক্ষরে মিলবে (না মিললে retry
  চিরকাল fail করবে); (গ) `DualWriteGapTest.kt`-এ কিছু বদলাবে না (12.2-এর পর লাইন-নির্ভর না)।
  আগে থেকেই থাকা কোনো `enqueueOutboxRetry` সাইট ছোঁবে না।
- **idempotency-চেক বাধ্যতামূলক** (rule #1a) — প্রতিটা RPC-র migration বডি (`grep -i`, rule 5a) খুলে দেখবে
  ২য়বার চললে কী হয়। ফলাফল (idempotent / guard আছে / নেই) প্রতিটা সাইটের জন্য progress doc-এ এক লাইনে।
- `DualWriteGapTest`-এর যে সাইটগুলো এই ধাপে ঠিক হলো, সেগুলো pass করার কথা; বাকিগুলো fail থাকবে
  (সংখ্যা প্রতিটা ধাপে দেওয়া আছে) — অপ্রত্যাশিত কিছু হলে সেটাই রিপোর্ট করবে।

**Windows verification (প্রতিটা 12.x-এর শেষে ব্যবহারকারীকে হুবহু এটা দেবে):**
```
# File Explorer-এ C:\somadhan খুলে address bar-এ powershell লিখে Enter, তারপর:
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.repository.DualWriteGapTest" --stacktrace > out.txt 2>&1
Select-String -Path out.txt -CaseSensitive -Pattern "^e: |What went wrong|tests? completed|BUILD" | ForEach-Object { $_.Line } | Out-File errors.txt -Encoding utf8
$x = Get-ChildItem app\build\test-results\testDebugUnitTest\*DualWriteGapTest.xml
Select-String -Path $x -Pattern '<failure message=' | ForEach-Object { $_.Line.Trim().Substring(0,[Math]::Min(200,$_.Line.Trim().Length)) } | Out-File msgs.txt -Encoding utf8
notepad errors.txt
notepad msgs.txt
```
⚠️ `C:\somadhan\local.properties` (`sdk.dir=...`) থাকা বাধ্যতামূলক — জিপে এটা ইচ্ছাকৃতভাবে থাকে না; না থাকলে টেস্ট চলার আগেই `SDK location not found` এরর আসে
(তখন `test-results` ফোল্ডারও তৈরি হয় না)। নতুন জিপ overwrite-extract করার পর ফাইলটা আছে কিনা দেখে নাও।
⚠️ প্রতিটা টেস্ট-রান পরের রানে আগের XML রিপোর্ট মুছে/বদলে দেয় (`test-results` ফোল্ডারে শুধু শেষ রানের XML থাকে) —
তাই একটা রান শেষ হলেই তার `msgs.txt`/XML-চেক করো, তারপর পরের ক্লাসের রান।
(Borderline ফাইল থাকলে আলাদা রান: `--tests "com.example.repository.DualWriteGapTestBorderline"`।)
ব্যবহারকারী পরের সেশনের শুরুতে master prompt-এর নিচে এই ফরম্যাটে paste করবেন:
`WINDOWS RESULT: Step 12.N — <"N tests completed, M failed" বা "BUILD SUCCESSFUL"> — errors.txt/msgs.txt-এর লেখা`

- [x] **Step 12.1 — Borderline ৭টা সাইট যাচাই (production কোড বদলাবে না)।** ✅ কোড-কাজ শেষ + ✅ Windows-verified (২০২৬-০৯-২০); — ফল: ৮ফাংশন/১০সাইটের মধ্যে ১টা MONEY-CRITICAL, ২টা MONEY-ADJACENT
      (ব্যবহারকারীর সিদ্ধান্ত), বাকিগুলো NON-MONEY; বিস্তারিত progress doc-এ।
      `CI_TEST_SUITE_PROGRESS.md`-এর PART 1 "🟡 BORDERLINE" তালিকা: `ownerResetOrphanedAcceptedBid`,
      `acceptDirectContractProposal`→`acceptDirectContract`, `adminManuallyFlagDispute`,
      `checkAndProcess48HourAutoReleases`→`systemNotify48HourAutoRelease`,
      `resolveCommissionRateForNewJob`→`syncSolverFreeJobQuota`, `submitKyc`, `adminUpdateKycInfo`,
      `adminResetKycToPending`। কাজ: (১) প্রতিটার RPC-র migration বডি খুলে (`grep -i`) দেখবে
      `balance*`/`escrow*`/`transactions`/`wallet` টেবিলে write করে কিনা (আন্দাজ না, বডি থেকে
      লাইন-উদ্ধৃতিসহ); (২) প্রতিটাকে MONEY-CRITICAL বা NON-MONEY চিহ্নিত করবে (KYC-তিনটা:
      "সরাসরি টাকা না, কিন্তু withdrawal gate করে" — এদের ক্ষেত্রে যুক্তিসহ সিদ্ধান্ত লিখবে, অস্পষ্ট
      হলে ব্যবহারকারীর কাছে রাখবে); (৩) যেগুলো MONEY-CRITICAL, তাদের জন্য **নতুন ফাইল**
      `app/src/test/java/com/example/repository/DualWriteGapTestBorderline.kt` (একই brace-matching
      পদ্ধতি; কিন্তু সাইট খুঁজবে **RPC-কলের নাম দিয়ে**, লাইন-নাম্বার হার্ডকোড না — 12.2-এর
      helper-এর ধারণা এখানেই আগে বসাতে পারো), যা এখন fail করার কথা। অনুমোদিত ফাইল: শুধু ওই নতুন
      টেস্ট ফাইল + progress doc। প্রত্যাশা: `DualWriteGapTest` আগের মতোই ২৬/২৬ fail; Borderline
      ক্লাসে N টা fail (N = confirmed money-critical সংখ্যা)। যেগুলো money-critical প্রমাণ হলো,
      সেগুলোর নাম তালিকাভুক্ত করবে (12.8-এর ইনপুট)।

- [x] **Step 12.2 — টেস্টকে লাইন-নাম্বার-নির্ভরতা থেকে মুক্ত করা।** ✅ কোড-কাজ শেষ (২০২৬-০৯-২১) + ✅ Windows-verified (২০২৬-০৯-২১: ২৬/২৬ fail, একই UNPROTECTED বার্তা ও লাইন, কম্পাইল OK)।
      ফল: `DualWriteGapTest.kt` এখন প্রতিটা সাইট খোঁজে `fun <নাম>` + ভেতরের `SupabaseSyncManager.<কল>(` (occurrence-তম,
      কমেন্ট-লাইন বাদ) + পরের প্রথম `.onFailure` দিয়ে — হার্ডকোড লাইন নাম্বার নেই। টেস্টের সংখ্যা (২৬)/নাম/বর্ণনা/ব্যর্থতা-বার্তা/
      brace-matching অপরিবর্তিত; সাইট-টেবিল টেস্ট ফাইলের হেডারে ও progress doc-এর "Step 12.2" সেকশনে। **sandbox-যাচাই static ছিল**
      (Python পোর্ট; sandbox-এ Gradle/Kotlin কম্পাইলার নেই) — ২৬টার প্রতিটা পুরনো লাইনেই পৌঁছায়, ব্লক ও বার্তা byte-identical; পরে Windows real-run-এ নিশ্চিত হয়েছে।
      ⚠️ **সংশোধন (12.7-এর জন্য):** পুরনো লাইন 9560-এর সাইট আসলে `fun adminUpdateDirectContractStatus`-এর নিজের ভেতরে,
      `adminCancelAndRefundDirectContract`-এর ভেতরে না (ওই ফাংশনের সাইট শুধু 9597); আর distinct ফাংশন ২৪ না, **২৫**
      (ডাবল-সাইট শুধু `trackExtraPaymentMissCycle`)। নিচের মূল বিবরণ ইতিহাসের জন্য অপরিবর্তিত রাখা হলো:
      সমস্যা: `DualWriteGapTest.kt` প্রতিটা সাইট `SomadhanRepository.kt`-এর **হার্ডকোড লাইন নাম্বার**
      (2829, 2952, …) দিয়ে খোঁজে। পরের ধাপগুলোতে `enqueueOutboxRetry` যোগ করলে লাইন শিফট হবে ও
      বাকি সব টেস্ট ভুল কারণে ভাঙবে। কাজ: `assertSiteIsOutboxProtected(line, desc)` বদলে এমন helper
      যা সাইট খোঁজে **ফাংশনের নাম + ভেতরের `SupabaseSyncManager.<rpcCall>(` কলের পরের প্রথম
      `.onFailure {`** দিয়ে (২৬টার জন্য একটা টেবিল: ফাংশন-নাম, কল-নাম, ওই ফাংশনে কতগুলো সাইট/কোনটা;
      `trackExtraPaymentMissCycle` আর `adminCancelAndRefundDirectContract`-এ ২টা করে)। brace-matching
      লজিক অপরিবর্তিত। **অনুমোদিত ফাইল: শুধু `DualWriteGapTest.kt`** (rule #1a-র ব্যতিক্রম,
      শুধু সাইট-খোঁজার পদ্ধতি বদল; টেস্টের সংখ্যা/নাম/assert-লজিক একই)। প্রত্যাশা (Windows):
      হুবহু আগের মতো **২৬টার ২৬টাই fail**, একই "UNPROTECTED" মেসেজ, কোনো "line X-এ `.onFailure`
      পাওয়া যায়নি" ধরনের মেসেজ নেই। ২৬টার প্রতিটা সাইট এখনো আগের মতোই একই সোর্স-ব্লকে পৌঁছাচ্ছে
      কিনা যাচাইয়ের জন্য পুরনো লাইন-নাম্বার বনাম নতুন পদ্ধতির ফল স্ট্যাটিকভাবে মিলিয়ে দেখবে।

- [x] **Step 12.3 — Pilot: `acceptBid` (১টা সাইট) + প্যাটার্ন চূড়ান্ত করা।** ✅ কোড-কাজ শেষ (২০২৬-০৯-২১) + ✅ **Windows-verified (২০২৬-০৯-২১: `26 tests completed, 25 failed`, লাইন-সেট প্রত্যাশার সাথে হুবহু, কম্পাইল OK)**
      (ফল: `accept_bid` idempotent (`ALREADY_ACCEPTED` গার্ড), সাইট BLOCKED নয়; অবশিষ্ট ঝুঁকি — stale replay, anon `auth.uid()` NULL ফাঁক (সম্ভাব্য, যাচাই-না-হওয়া),
      non-OK `result` enqueue হয় না — সব progress doc-এর "Step 12.3" সেকশনে; সেখানেই "12.x fix recipe"। **Windows-result প্রসেস হয়ে গেছে; পরের সেশন সরাসরি 12.4।**)
      `acceptBid → accept_bid` (এটাই আসল `acceptBid()` escrow বাগের root cause)। কাজ: (১) `accept_bid`
      RPC-র migration বডি থেকে idempotency যাচাই (একই bid দুইবার accept হলে কী হয়);
      (২) `SomadhanRepository.kt`-এর ওই ফাংশনের `.onFailure {}`-এ `enqueueOutboxRetry`;
      (৩) `OutboxRpcDispatcher.kt`-এ `"accept_bid"` branch, param key মিলিয়ে;
      (৪) `OutboxSyncWorker.kt` পড়ে লিখবে replay কোন user-session-এ চলে ও তাতে এই RPC-র auth-guard
      (যেমন `auth.uid()` মালিক-চেক) ঠিক থাকে কিনা — সমস্যা থাকলে সাইটটা BLOCKED রাখবে;
      (৫) শিখে আসা প্যাটার্ন (param-key-নাম কনভেনশন, error হ্যান্ডলিং, idempotency-চেকলিস্ট)
      progress doc-এ ছোট একটা "12.x fix recipe" হিসেবে লিখবে — পরের ধাপগুলো সেটাই অনুসরণ করবে।
      অনুমোদিত: `SomadhanRepository.kt` (শুধু `acceptBid`), `OutboxRpcDispatcher.kt` (শুধু নতুন branch)।
      প্রত্যাশা: **১টা pass, ২৫টা fail** (acceptBid ছাড়া)। ⚠️ `acceptBid`-এর পরের সব সাইটের `msgs.txt`-লাইন-নাম্বার +২০ সরবে
      (`SomadhanRepository.kt`-এ ২০ লাইন যোগ হয়েছে; টেস্ট লাইন-নির্ভর না, শুধু বার্তার নাম্বার বদলায়) — তালিকা progress doc-এ।

- [x] **Step 12.4 — Job release ও dispute গ্রুপ (৬টা সাইট)।** ✅ কোড-কাজ শেষ (২০২৬-০৯-২১, static) + ✅ **Windows-verified
      (২০২৬-০৯-২১: `26 tests completed, 19 failed`, লাইন-সেট প্রত্যাশার সাথে হুবহু, কম্পাইল OK)** — বিস্তারিত
      `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.4" সেকশনে (idempotency-চেকলিস্ট: সব ৬টা IDEMPOTENT/non-money,
      কোনোটা BLOCKED হয়নি; `requestJobRelease`-এর ডুপ্লিকেট-PENDING-চার্জ residual risk নথিভুক্ত)।
      `requestJobRelease`, `cancelJobReleaseRequest`, `rejectJobReleaseRequest`, `withdrawDispute`,
      `settleDispute`, `adminResolveDisputeLocked → resolveDisputeSplit`। 12.3-এর recipe অনুযায়ী।
      বিশেষ সতর্কতা: `settleDispute`/`resolveDisputeSplit` টাকা ভাগ করে — ২য়বার চললে দ্বিগুণ ভাগ হয়
      কিনা কড়াভাবে যাচাই (Step 40 migration `resolve_dispute_split`-এর বডি দ্রষ্টব্য)।
      অনুমোদিত: `SomadhanRepository.kt` (শুধু এই ৬ ফাংশন), `OutboxRpcDispatcher.kt` (নতুন branch)।
      প্রত্যাশা: **৭টা pass, ১৯টা fail** (BLOCKED থাকলে সেই অনুযায়ী কম pass, আলাদা করে ব্যাখ্যাসহ)।

- [x] **Step 12.5 — Admin reconcile/refund/cleanup + cancel/complete গ্রুপ (৬টা সাইট)।** ✅ কোড-কাজ শেষ
      (২০২৬-০৯-২১, static) + ✅ **Windows-verified (২০২৬-০৯-২১: `26 tests completed, 13 failed`, কম্পাইল OK, ৬টা ফাংশনের নাম কোনো fail-এ নেই; ১৩ লাইনের ১২টা প্রত্যাশার সাথে হুবহু, শুধু `9796,9796` প্রত্যাশার জায়গায় বাস্তবে `9796,9833` — দুটো আলাদা ব্লক, কোনো ক্ষতি নেই)** — বিস্তারিত `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.5"
      সেকশনে (idempotency-চেকলিস্ট: সব ৬টা IDEMPOTENT, কোনোটা BLOCKED হয়নি; `solverCancelJob`-এর reopen-as-open
      পাথে stale-replay residual risk ও `mark_additional_charge_settled`-এর NOT_AUTHORIZED-replay residual risk
      নথিভুক্ত)।
      `reconcileEscrowStates → adminReconcileEscrowStates`, `reconcileUserBalances →
      adminReconcileUserBalances`, `cleanupDuplicateRefunds → adminCleanupDuplicateRefunds`,
      `repairMissingRefunds → adminRepairMissingRefunds`, `solverCancelJob`,
      `confirmReleaseAndComplete → markAdditionalChargeSettled`। সতর্কতা: reconcile/cleanup RPC বড়
      ব্যাচ-অপারেশন — retry-তে ঝুঁকি বেশি; `solverCancelJob`-এর RPC আসলে refund ঘটায় কিনা আগের
      PART 1-এ "সবচেয়ে কম নিশ্চিত" ছিল — বডি খুলে নিশ্চিত করবে (money-critical না হলে ব্যাখ্যাসহ
      NON-MONEY-তে নামিয়ে টেস্ট থেকে সরানোর প্রস্তাব দেবে, নিজে টেস্ট মুছবে না — ব্যবহারকারী অনুমোদন দিলে
      পরের সেশনে)। অনুমোদিত: `SomadhanRepository.kt` (শুধু এই ৬), `OutboxRpcDispatcher.kt`।
      প্রত্যাশা: **১৩টা pass, ১৩টা fail** (আগের ধাপের ফলের ওপর ভিত্তি করে হিসাব — BLOCKED ব্যতিক্রম বাদে)।

- [x] **Step 12.6 — Wallet/withdrawal/refund/additional-charge গ্রুপ (৫টা সাইট)।** ✅ কোড-কাজ শেষ
      (২০২৬-০৯-২১, static) + ✅ **Windows-verified (২০২৬-০৯-২১: `26 tests completed, 9 failed`, কম্পাইল OK
      (কোনো `e: ` লাইন নেই), ৯টা fail-লাইন প্রত্যাশার সাথে হুবহু `{7717, 9394, 9422, 9872, 9909, 10625, 10735,
      10786, 10806}`, ৪টা fix-করা ফাংশনের নাম কোনো fail-এ নেই; `7717` = `depositMoneyViaGateway` — BLOCKED,
      প্রত্যাশিত fail)** — বিস্তারিত `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.6" ও "Step 12.6 — Windows-verified" সেকশনে
      (idempotency-চেকলিস্ট: ৪টা IDEMPOTENT, `depositMoneyViaGateway → requestWalletDeposit` **BLOCKED**
      auth.uid()-ভিত্তিক ভুল-ওয়ালেট ঝুঁকির কারণে। ✅ **ব্যবহারকারীর সিদ্ধান্ত এসে গেছে (২০২৬-০৯-২১): বিকল্প (ক)
      — সার্ভার-সাইড fix**; কাজটা Step 12.8-এ ঢোকানো হয়েছে, 12.6 এই ধাপেই বন্ধ)।
      `adminUpdateWithdrawalTrxId`, `depositMoneyViaGateway → requestWalletDeposit` (BLOCKED),
      `adminRefundEscrow → adminRefundAndReopenProblem`, `requestAdditionalCharge`,
      `respondToAdditionalCharge`। অনুমোদিত: `SomadhanRepository.kt` (শুধু এই ৫, ১টা BLOCKED তাই ছোঁয়া হয়নি),
      `OutboxRpcDispatcher.kt`। প্রত্যাশা (Windows): **১৭টা pass, ৯টা fail** (`26 tests completed, 9 failed`)।

- [x] **Step 12.7 — Extra-payment / direct-contract / commission গ্রুপ (৮টা সাইট)।** ✅ কোড-কাজ শেষ
      (২০২৬-০৯-২১, static) + ✅ **Windows-verified (২০২৬-০৯-২১: `26 tests completed, 3 failed`, কম্পাইল OK,
      fail-লাইন `{7717, 10803, 10885}` প্রত্যাশার সাথে হুবহু, ৬টা fix-করা সাইটের নাম কোনো fail-এ নেই —
      তিনটে fail-ই BLOCKED সাইট, বাগ না)** — ৬টা retry-added, **২টা BLOCKED** (`userConfirmExtraAmount`,
      `cleanupCorruptedCommissionRates → adminUpdateProblemCommissionRate`; কারণ ও বিকল্প ক/খ
      `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.7" সেকশনে, ব্যবহারকারীর সিদ্ধান্ত বাকি — Step 12.8-এ যাবে)।
      ✅ `admin_update_direct_contract_status` migrations-এ আছে কিনা — মীমাংসিত (নিচে)।
      `trackExtraPaymentMissCycle` (২টা সাইট → `systemTrackExtraPaymentMiss`), `adminCancelAndRefundDirectContract`
      (২টা সাইট → `adminUpdateDirectContractStatus`), `requestExtraAmount`, `userConfirmExtraAmount`,
      `userRejectExtraAmount`, `cleanupCorruptedCommissionRates → adminUpdateProblemCommissionRate`।
      ⚠️ (Step 12.2 সংশোধন, স্ট্যাটিকভাবে যাচাই) `adminCancelAndRefundDirectContract`-এর "২টা সাইট"-এর একটা (পুরনো লাইন 9560) আসলে
      `fun adminUpdateDirectContractStatus`-এর নিজের `.onFailure`-এ; অন্যটা (9597) `adminCancelAndRefundDirectContract`-এ। তাই এই ধাপে
      এডিট হবে **দুটো ফাংশনের** একটা করে ব্লক (`adminUpdateDirectContractStatus` ও `adminCancelAndRefundDirectContract`) — সাইট-সংখ্যা ৮-ই থাকে।
      সতর্কতা: PART 1-এ `adminUpdateDirectContractStatus` আর `adminUpdateProblemCommissionRate` "কম
      নিশ্চিত" ছিল, এবং `SomadhanRepository.kt`-এর লাইন 9899–9903-এর কমেন্টে সন্দেহ লেখা ছিল যে
      `admin_update_direct_contract_status` ফাংশনটা `supabase/migrations`-এ আদৌ আছে কিনা —
      ✅ **মীমাংসিত (২০২৬-০৯-২১, `grep`): আছে**, `supabase/migrations/recovered_admin_moderation.sql:412`,
      signature `(p_problem_id text, p_status text, p_direct_contract_status text)`। বডি খুলে idempotency
      যাচাই ও dispatcher param-key মেলানো এখনো 12.7-এর কাজ।
      ✅ (Windows-verified 12.5 ফলে নিশ্চিত) দুটো ব্লক আসলেই আলাদা: `msgs.txt`-এ `9796` (`adminUpdateDirectContractStatus`-এর নিজের ব্লক) ও `9833` (`adminCancelAndRefundDirectContract`-এর ব্লক)।
      অনুমোদিত: `SomadhanRepository.kt` (শুধু এই ৮ সাইট), `OutboxRpcDispatcher.kt`।
      প্রত্যাশা (সংশোধিত, ২টা নতুন BLOCKED হওয়ার পর): **২৩টা pass / ৩টা fail**
      (`26 tests completed, 3 failed`) — লাইন `{7717, 10803, 10885}`। তিনটেই BLOCKED সাইট, Step 12.8-এ যাবে।

🔷 **Step 12.8 তিনটা আলাদা উপ-ধাপে ভাগ করা হয়েছে (ব্যবহারকারীর অনুরোধে, ২০২৬-০৯-২১) — 12.8a → 12.8b → 12.8c,
প্রতিটা আলাদা Claude সেশনে, এই ক্রমেই।** GATE-এর হিসেবে 12.8 তখনই সম্পূর্ণ ধরা হবে যখন **তিনটাই** `[x]`। সাধারণ
12.x নিয়ম (একটা সেশন = একটা ধাপ, sandbox Gradle-চেক, ইত্যাদি) প্রতিটা উপ-ধাপেও অপরিবর্তিতভাবে প্রযোজ্য।

🧾 **প্রতিটা 12.8x সেশনের বাড়তি বাধ্যতামূলক নিয়ম (শেষে, zip বানানোর ঠিক পরে):** নিজের কোড-কাজ কমিট করার পর zip
বানিয়ে **ফাইল-সংখ্যা গুনে uploaded zip-এর সাথে মিলিয়ে** দেখবে —
```
unzip -l <নতুন_zip> | tail -2      # মোট ফাইল-সংখ্যা
unzip -l <uploaded_zip> | tail -2  # আগের zip-এর মোট ফাইল-সংখ্যা (তুলনার জন্য)
```
নতুন zip-এ ফাইল-সংখ্যা আগের zip-এর **সমান বা বেশি** হওয়া উচিত (নতুন ফাইল যোগ হলে বেশি — যেমন নতুন proposed
migration), **কখনো কম না**। তারপর dotfile-গুলো (`.github/workflows/full-test.yml`, `.env`, `.env.example`,
`.gitignore`) `unzip -l`-এর আউটপুটে **নাম ধরে খুঁজে** নিশ্চিত করবে প্রতিটা আছে (`local.properties` ছাড়া, যেটা
ইচ্ছাকৃতভাবে বাদ)। কোনো ফাইল কম পাওয়া গেলে বা কোনো dotfile না পাওয়া গেলে zip আবার বানাবে (কারণ ধরে) — কখনো
"সম্ভবত ঠিক আছে" ধরে এগোবে না, ফলাফলটা (সংখ্যা ও dotfile-লিস্ট) progress doc-এ ১-২ লাইনে লিখে রাখবে।

- [x] **Step 12.8a — Borderline-নিশ্চিত `acceptDirectContractProposal` fix + MONEY-ADJACENT ২টার অন্তর্ভুক্তি-সিদ্ধান্ত।** ✅ কোড-কাজ শেষ (২০২৬-০৯-২১, static) + ✅ **Windows-verified (২০২৬-০৯-২১: BUILD SUCCESSFUL, ০টা failure)**.
      ব্যবহারকারীর সিদ্ধান্ত (২০২৬-০৯-২১, সেশনের শুরুতে): `syncSolverFreeJobQuota` ও `adminManuallyFlagDispute`
      দুটোই নেওয়া হয়েছে; `adminManuallyFlagDispute`-এর product-প্রশ্নে উত্তর "হ্যাঁ, admin COMPLETED/CANCELLED
      কাজেও dispute flag করতে পারবেন" — তাই তিনটা সাইট/গ্রুপ-ই (বাধ্যতামূলক ১ + ঐচ্ছিক ২) এই সেশনে fix হয়েছে।
      বিস্তারিত (idempotency-চেক, কোড-বদল টেবিল, static যাচাই ৪/৪ pass, Windows-verified ফলাফল):
      `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.8a" ও "Step 12.8a — Windows-verified" সেকশন। **সংশোধিত মূল স্কোপ ইতিহাসের জন্য নিচে অপরিবর্তিত রাখা হলো:**
      **বাধ্যতামূলক অংশ:** 12.1-এ MONEY-CRITICAL নিশ্চিত হওয়া একমাত্র borderline সাইট — `acceptDirectContractProposal`
      → `accept_direct_contract` RPC (`SomadhanRepository.kt`-এর ~লাইন 2386, `recovered_bidding_contracts.sql:185`)।
      migration বডি 12.1-এ পড়া হয়েছিল (idempotent, `direct_contract_status <> 'PENDING_ACCEPTANCE'` হলে
      `ALREADY_PROCESSED` — লাইন 210-212), তবু এই সেশনে `grep -i` দিয়ে নিজে আবার নিশ্চিত করবে (rule 5a)। 12.3-এর
      fix-recipe অনুযায়ী `.onFailure`-এ `enqueueOutboxRetry(rpcName = "accept_direct_contract", ...)` +
      dispatcher branch। যাচাই: `DualWriteGapTestBorderline`-এর বিদ্যমান ১টা টেস্ট (Python পোর্ট দিয়ে static)
      `26 tests completed`-এর অংশ না, আলাদা ফাইল — pass প্রত্যাশা।

      **সেশনের শুরুতে ব্যবহারকারীকে জিজ্ঞেস করবে (সিদ্ধান্ত না পেলে দুটোই বাদ রেখে শুধু বাধ্যতামূলক অংশ করবে):**
      `syncSolverFreeJobQuota` (`resolveCommissionRateForNewJob`-এর ২টা সাইট, লাইন 274/291,
      `sync_solver_free_job_quota` RPC — MONEY-ADJACENT, quota-bypass ঝুঁকি) ও `adminManuallyFlagDispute`
      (লাইন 3260, `admin_manually_flag_dispute` RPC — MONEY-ADJACENT, dispute-flag sync ঝুঁকি) 12.8a-তে
      অন্তর্ভুক্ত হবে কিনা। ⚠️ `adminManuallyFlagDispute` নেওয়া হলে মনে রাখবে: RPC-তে `ALREADY_DISPUTED` গার্ড
      আছে কিন্তু **problem status-এর চেক নেই** — retry অনেক দেরিতে চললে ইতিমধ্যে COMPLETED/CANCELLED কাজও
      disputed হয়ে যেতে পারে। শুধু ব্যবহারকারীর স্পষ্ট product-সিদ্ধান্ত (admin কি COMPLETED কাজেও dispute flag
      করতে পারে?) থাকলে retry যোগ করবে, নাহলে BLOCKED রেখে কারণ নথিভুক্ত করবে। দুটোর কোনোটাই এখনো কোনো টেস্ট
      ফাইলে নেই — নেওয়া হলে `DualWriteGapTestBorderline.kt`-এ ১টা করে নতুন `@Test` যোগ করবে (বিদ্যমান
      `acceptDirectContractProposal`-টেস্টের হুবহু প্যাটার্নে, funName+rpcCall+occurrence দিয়ে, লাইন-নাম্বার
      হার্ডকোড না)।

      অনুমোদিত ফাইল: `SomadhanRepository.kt` (শুধু এই ৩টা ফাংশনের/সাইটের `.onFailure` ব্লক — অন্তর্ভুক্তি-সিদ্ধান্ত
      অনুযায়ী কম হতে পারে), `OutboxRpcDispatcher.kt`, `DualWriteGapTestBorderline.kt` (নতুন `@Test` যোগ করার
      জন্য, শুধু যোগ — বিদ্যমান টেস্ট বদলাবে না)।
      প্রত্যাশা: `DualWriteGapTestBorderline` সব pass (১টা বাধ্যতামূলক + যতগুলো অন্তর্ভুক্ত হলো)।
      `DualWriteGapTest`-এর ২৬টার সংখ্যা এই ধাপে **অপরিবর্তিত** (এই ৩টা সাইট ওই টেস্ট ফাইলে নেই)।

- [x] **Step 12.8b — `depositMoneyViaGateway`-এর সার্ভার-সাইড fix (বিকল্প ক, সিদ্ধান্ত ২০২৬-০৯-২১)।** ✅ **সম্পূর্ণ + Windows-verified (২০২৬-০৯-২১)।**
      `depositMoneyViaGateway → requestWalletDeposit` (Step 12.6-এ BLOCKED রাখা সাইট,
      `SomadhanRepository.kt:7717`, `DualWriteGapTest`-এর একটা অবশিষ্ট fail)। করণীয়, এই **ক্রমে**:
      1. **প্রস্তাবিত migration লিখবে** (apply করবে না, rule #1):
         `docs/proposed_migrations/PROPOSED_step12_6_request_wallet_deposit_auth_guard.sql` —
         (ক) `request_wallet_deposit(...)`-এ নতুন প্যারামিটার `p_expected_user_id uuid` এবং বডির শুরুতে
         `if auth.uid() is null or auth.uid() <> p_expected_user_id then return ... 'NOT_AUTHORIZED'`
         (অন্য owner-ভিত্তিক RPC-গুলোর হুবহু প্যাটার্নে, exception না — non-OK result, যাতে
         `enqueueOutboxRetry`-র non-OK-enqueue-করি-না নিয়মের সাথে মেলে);
         (খ) `gateway_trx_id`-এ `unique index` (partial/`where gateway_trx_id is not null`), যাতে
         concurrent double-credit race বন্ধ হয়। পুরনো signature-টা `drop` করবে না নিজে থেকে —
         Step 12.9-এর duplicate-overload সমস্যার সাথে সংঘর্ষ হতে পারে; migration-এর হেডার-কমেন্টে
         ব্যবহারকারীর জন্য স্পষ্ট করে লিখবে পুরনো signature নিয়ে কী করতে হবে ও কেন।
      2. **ব্যবহারকারী migration apply করেছেন কিনা জিজ্ঞেস/নিশ্চিত করবে।** apply না হওয়া পর্যন্ত
         `SomadhanRepository.kt`/`OutboxRpcDispatcher.kt`-এ **retry যোগ করবে না** (আগে retry বসালে ভুল
         ওয়ালেটে টাকা যাওয়ার ঝুঁকি বাড়ে, কমে না)। apply না হলে সাইটটা BLOCKED-ই থাকবে এবং এই ধাপ সেশনেই
         `[x]` করা যাবে **না** — পরের সেশনে (এখনো 12.8b হিসেবে) আবার চেষ্টা করবে।
      3. apply নিশ্চিত হলে তবেই 12.3-এর recipe-তে `depositMoneyViaGateway`-এর `.onFailure`-এ
         `enqueueOutboxRetry(rpcName = "request_wallet_deposit", ...)` + dispatcher branch, param-এ
         `expectedUserId` (deposit-কারীর id, `auth.uid()` নয়) অবশ্যই থাকবে, এবং
         `OutboxRpcDispatcher.kt`-এর 12.6-এ রাখা "ইচ্ছাকৃতভাবে branch নেই" কমেন্টটা মুছে/হালনাগাদ করবে।
      4. Step 12.6-এর idempotency-টেবিলের `depositMoneyViaGateway` সারি আপডেট করবে (BLOCKED → retry ✅),
         এবং tracker-এর BLOCKED-তালিকা থেকে সাইটটা সরাবে।
      অনুমোদিত ফাইল: `SomadhanRepository.kt` (শুধু `depositMoneyViaGateway`-এর `.onFailure` ব্লক),
      `OutboxRpcDispatcher.kt`, নতুন ফাইল `docs/proposed_migrations/PROPOSED_step12_6_request_wallet_deposit_auth_guard.sql`।
      প্রত্যাশা: apply+retry হলে `DualWriteGapTest`-এ 7717 আর fail করবে না; apply না হলে এই সেশনে `[ ]`-ই থাকবে।
      📌 **অবস্থা (সেশন ১, ২০২৬-০৯-২১): ধাপ ১ সম্পন্ন** — `docs/proposed_migrations/PROPOSED_step12_6_request_wallet_deposit_auth_guard.sql`
      লেখা হয়েছে (পূর্ব-যাচাই কুয়েরি, staging-যাচাই, rollback সহ)। **ধাপ ২ সম্পন্ন — migration Supabase MCP দিয়ে apply হয়েছে (২০২৬-০৯-২১, ব্যবহারকারীর নির্দেশে)**; live-এ আগে থেকেই পূর্ণ UNIQUE index
      `gateway_payments_gateway_trx_id_key` ছিল (তাই index বানানো হয়নি) ও live বডি step36 থেকে আলাদা ছিল (live থেকে বানানো) — বিস্তারিত progress doc।
      ✅ **ধাপ ৩–৪ সম্পন্ন (২০২৬-০৯-২১, static)** — ব্যবহারকারী `SupabaseSyncManager.requestWalletDeposit`-এ `expectedUserId` যোগের অনুমতি দিয়েছেন;
      retry + dispatcher branch যোগ হয়েছে। ✅ কোড-কাজ শেষ + ✅ **Windows-verified (২০২৬-০৯-২১: `26 tests completed, 2 failed`, লাইন `{10889, 10971}` প্রত্যাশার সাথে হুবহু,
      কম্পাইল OK, `depositMoneyViaGateway` কোনো fail-এ নেই)** — **Step 12.8b সম্পূর্ণ।** বিস্তারিত progress doc-এর "Step 12.8b — retry যোগ" ও
      "Step 12.8b — Windows-verified" সেকশন। Windows-result প্রসেস হয়ে গেছে; পরের সেশন সরাসরি 12.8c। ⚠️ **ধাপ ৩-এর জন্য একটা অমীমাংসিত অনুমতি-প্রশ্ন:** `OutboxRpcDispatcher.kt` শুধু
      `SupabaseSyncManager.requestWalletDeposit(...)` wrapper-ই ডাকে, আর ওই wrapper আজ `p_expected_user_id` পাঠাতে পারে না।
      তাই ধাপ ৩-এ `SupabaseSyncManager.kt`-এর **শুধু `requestWalletDeposit`-এ** একটা ঐচ্ছিক `expectedUserId: String? = null`
      প্যারামিটার (non-null হলে `p_expected_user_id` পাঠায়; null হলে আজকের আচরণ হুবহু — অনলাইন-পথ অপরিবর্তিত) যোগ লাগবে,
      যেটা উপরের "অনুমোদিত ফাইল" তালিকায় নেই — ব্যবহারকারীর স্পষ্ট সম্মতি নিয়ে তবেই এই ফাইল ছোঁবে (rule #1a)।

- [x] **Step 12.8c — `userConfirmExtraAmount` + `adminUpdateProblemCommissionRate`-এর fix (দুটোই বিকল্প ক, সিদ্ধান্ত ২০২৬-০৯-২১)।** ✅ সম্পূর্ণ + Windows-verified (২০২৬-০৯-২১): `BUILD SUCCESSFUL`, `tests="26" failures="0" errors="0"`।
      **`userConfirmExtraAmount`** (`user_confirm_extra_amount`, `SomadhanRepository.kt:10803`) — `depositMoneyViaGateway`-এর
      হুবহু প্যাটার্নে (12.8b দেখো), এই **ক্রমে**:
      1. প্রস্তাবিত migration: `docs/proposed_migrations/PROPOSED_step12_7_user_confirm_extra_amount_amount_guard.sql` —
         `user_confirm_extra_amount(p_problem_id text)`-এ নতুন প্যারামিটার `p_expected_amount numeric` যোগ,
         বডিতে `v_amt := v_problem.pending_extra_amount;`-এর ঠিক পরে
         `if v_amt is distinct from p_expected_amount then return jsonb_build_object('result', 'AMOUNT_CHANGED');`
         (non-OK result, exception না)।
      2. ব্যবহারকারী apply করেছেন কিনা নিশ্চিত করবে — apply না হলে retry যোগ করবে না, ধাপ `[ ]`-ই থাকবে।
      3. apply নিশ্চিত হলে `.onFailure`-এ `enqueueOutboxRetry(rpcName = "user_confirm_extra_amount", ...)`,
         param-এ `problemId` + `expectedAmount` (Double, `amt` থেকে) + dispatcher branch।

      **`adminUpdateProblemCommissionRate`** (`cleanupCorruptedCommissionRates`, `SomadhanRepository.kt:10885`) —
      এটা RPC না, তাই migration/apply লাগে না, সরাসরি করা যাবে:
      1. `SupabaseSyncManager.kt`-এর `adminUpdateProblemCommissionRate(problemId, rate)` wrapper-এ
         `.filter { eq("id", problemId) }`-এর সাথে `eq("status", "OPEN")` ও `is_("accepted_solver_id", null)`
         যোগ করবে (rule #1a exception, শুধু এই ফাংশন)। ⚠️ এর ফলে replay-এর সময় solver ইতিমধ্যে accept হয়ে
         গেলে ০ row ম্যাচ করবে — `requireAffectedRowOrThrow` তখন exception ছুঁড়বে (worker আবার retry করবে,
         কখনো সফল হবে না)। তাই এই ফাংশনেই `requireAffectedRowOrThrow`-এর বদলে ০-row-কে non-OK/benign
         হিসেবে ধরার একটা ছোট বিকল্প-পথ দরকার (যেমন row না মিললে exception না ছুঁড়ে সরাসরি `Result.success(Unit)`
         রিটার্ন, কমেন্টে ব্যাখ্যাসহ) — নাহলে outbox চিরস্থায়ী retry-loop-এ আটকাবে (`MAX_RETRY_COUNT = 10`-এর
         পর `FAILED_PERMANENT`, কিন্তু ততক্ষণে অকারণে ১০ বার চেষ্টা)। এটাও এই সেশনের অনুমোদিত কাজ।
      2. তারপর `cleanupCorruptedCommissionRates`-এর `.onFailure`-এ `enqueueOutboxRetry(rpcName =
         "admin_update_problem_commission_rate", ...)` (নতুন rpcName, কারণ এটা RPC-ভিত্তিক না —
         dispatcher branch সরাসরি `SupabaseSyncManager.adminUpdateProblemCommissionRate(...)` কল করবে,
         `Result<Unit>` তাই `.map { JsonPrimitive("OK") as JsonElement }` লাগবে, `admin_set_verified_badge`-এর
         প্যাটার্নে), param-এ `problemId` + ঐচ্ছিক `rate` (Double?, null হতে পারে — `x?.let { put(...) }`)।

      অনুমোদিত ফাইল: `SomadhanRepository.kt` (শুধু এই ২টা `.onFailure` ব্লক), `OutboxRpcDispatcher.kt`,
      `SupabaseSyncManager.kt` (শুধু `adminUpdateProblemCommissionRate` ফাংশন), নতুন ফাইল
      `docs/proposed_migrations/PROPOSED_step12_7_user_confirm_extra_amount_amount_guard.sql`।
      📌 (12.8b-র Windows-ফল অনুযায়ী) 12.8c শুরুর আগে `DualWriteGapTest`-এর বাকি ২টা fail-লাইন: `10889` (`userConfirmExtraAmount`), `10971` (`cleanupCorruptedCommissionRates`) — নিচের `10803`/`10885` পুরনো লাইন-নাম্বার (12.8a/12.8b-তে লাইন সরেছে)। ফাইল-সংখ্যা/লাইন সরা নিয়ে ভয় নেই: টেস্ট লাইন-নির্ভর না।
      প্রত্যাশা: `adminUpdateProblemCommissionRate` অংশ এই সেশনেই সম্পূর্ণ করা সম্ভব (migration লাগে না);
      `userConfirmExtraAmount` অংশ apply-নির্ভর, apply না হলে সেই একটা সাইট বাকি রেখে বাকিটা শেষ করবে এবং
      ধাপ আংশিক-সম্পূর্ণ হিসেবে নথিভুক্ত করবে (`[ ]` থাকবে যতক্ষণ না দুটোই শেষ হয়)।
      📌 **অবস্থা (সেশন ১+২, ২০২৬-০৯-২১):** `adminUpdateProblemCommissionRate` অংশ **সম্পূর্ণ** (কোড-কাজ, static) —
      `SupabaseSyncManager.kt`-এ status='OPEN'/accepted_solver_id-is-null filter-গার্ড + ০-row বেনাইন no-op,
      `SomadhanRepository.kt`-এর `.onFailure`-এ retry, `OutboxRpcDispatcher.kt`-এ নতুন branch + `optionalDouble` helper।
      `userConfirmExtraAmount`: প্রস্তাবিত migration লেখা হয়েছে ও ব্যবহারকারীর নির্দেশে Supabase MCP দিয়ে **apply হয়ে গেছে**
      (migration নাম: `step12_8c_user_confirm_extra_amount_expected_amount_guard`, প্রজেক্ট mghvvpndkxnscwryfkib) —
      পুরনো ১-arg signature অক্ষত (grants অপরিবর্তিত: PUBLIC/anon/authenticated/service_role), নতুন ২-arg overload-এ
      grants টাইট (শুধু authenticated + service_role, PUBLIC/anon revoke করা হয়েছে) — দুটোই MCP দিয়ে যাচাই করা হয়েছে।
      ব্যবহারকারীর স্পষ্ট সম্মতিতে (২০২৬-০৯-২১) `SupabaseSyncManager.kt`-এর `userConfirmExtraAmount()`-এ ঐচ্ছিক
      `expectedAmount: Double? = null` প্যারামিটার যোগ হয়েছে (null = আগের আচরণ অপরিবর্তিত, non-null = নতুন
      গার্ডযুক্ত overload) + `SomadhanRepository.kt`-এর `.onFailure`-এ retry + dispatcher branch — **উভয় অংশই
      কোড-কাজে সম্পূর্ণ (static)।** Windows-verification বাকি — নিচের কমান্ড দিয়ে চালিয়ে ফল পেস্ট করলে
      `[x]` করা হবে (প্রত্যাশা: `26 tests completed, 0 failed`)।

- [x] **Step 12.9 — Duplicate RPC-overload (৭টা ফাংশন): live-যাচাই ও প্রস্তাবিত migration।** ✅ সম্পূর্ণ (২০২৬-০৯-২১)
      — সব ৭টার পুরনো overload live-এ DROP হয়ে গেছে (Supabase MCP দিয়ে, ব্যবহারকারীর স্পষ্ট নির্দেশে, দুই ব্যাচে)।
      `admin_adjust_balance`, `admin_notify_user`, `admin_set_banned`, `admin_set_restricted`,
      `create_notification`, `log_admin_action`, `submit_reputation_event` — পুরনো (ছোট) ও নতুন (`p_role`
      সহ) signature দুটোই live, PostgREST-এ PGRST203 ambiguity হতে পারে, কিন্তু live DB-তে কখনো
      যাচাই হয়নি। কাজ: (১) ব্যবহারকারীকে একটা **read-only** SQL দেবে যা Supabase SQL editor-এ চালালে
      প্রতিটা ফাংশনের সব signature দেখায় (`pg_proc`/`pg_get_function_identity_arguments`, `proname =
      ANY(...)`) + একটা নিরাপদ PostgREST কল-উদাহরণ (কোনো টাকা/state বদলায় না এমন কল বা dry-run নয়
      — না থাকলে ব্যবহারকারীকে শুধু signature-তালিকা চালাতে বলবে, কোনো state-বদলানো RPC live-এ
      চালাতে বলবে না); (২) ব্যবহারকারীর ফলাফল পেয়ে সিদ্ধান্ত: কোন পুরনো overload `DROP` হবে;
      (৩) `SupabaseSyncManager.kt`-এর `role == null` কল-সাইটগুলো (পুরনো ছোট overload-এর key পাঠায়)
      কীভাবে মিলবে তার বিশ্লেষণ — **কোড না বদলে** প্রস্তাব; (৪) `docs/proposed_migrations/
      PROPOSED_step12_9_drop_old_overloads.sql` (শুধু প্রস্তাব, `supabase/migrations/`-এ কিছু না)।
      ব্যবহারকারী নিজে review ও apply করবেন (rule #1)। এই ধাপে Gradle-টেস্ট নেই; verification হলো
      `scripts/scan_duplicate_overloads.sh` চালিয়ে (migration বসানোর পরের zip-এ) ৭টা আর
      "live duplicate" থাকছে না — এটা দেখা।

- [x] **Step 12.10 — `release_escrow`-এ server-side dispute guard (প্রস্তাবিত migration যাচাই ও apply)।** ✅ **সম্পূর্ণ (২০২৬-০৯-২১, সেশন ২): A+B live-এ apply (MCP, `step12_10_release_escrow_dispute_guard`), Q1 md5 মিলেছে, rollback-DO টেস্ট ৬/৬ পাস — বিস্তারিত progress doc-এর "Step 12.10 সম্পূর্ণ" সেকশন। migration-ফাইল-সিঙ্ক 12.12-এ বাকি। KYC-withdrawal সিদ্ধান্ত: শুধু SOLVER (নতুন ধাপ, অনুমোদন সাপেক্ষে)।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত:
      সমস্যা (Step 12.1-এ যাচাইকৃত): `release_escrow` RPC-র সর্বশেষ বডিতে `is_disputed` চেক নেই — admin dispute
      flag করলেও owner টাকা ছাড়তে পারে। প্রস্তাবিত fix ইতিমধ্যে লেখা:
      `docs/proposed_migrations/PROPOSED_step12_10_release_escrow_dispute_guard.sql` (মূল বডির ওপর শুধু
      ১১টা লাইন *যোগ*, কিছু বাদ নেই; SQL parser-এ syntax OK; কোনো DB-তে চালানো হয়নি)। এই সেশনের কাজ:
      (১) ব্যবহারকারী staging-এ ফাইলের হেডারের ৩টা যাচাই চালিয়ে ফল দিলে সেটা প্রসেস; (২) live-এ
      `release_escrow`-এর আসল বডি step36-এর সাথে মেলে কিনা (ব্যবহারকারীর দেওয়া `prosrc`) যাচাই — না মিললে
      guard নতুন বডির ওপর পুনর্লিখন; (৩) outbox worker `PROBLEM_DISPUTED`-এর মতো permanent error কীভাবে
      সামলায় (`OutboxSyncWorker.kt` পড়ে) — অসীম retry loop হলে সেটার প্রস্তাব; (৪) ব্যবহারকারী apply করার পর
      নিশ্চিতকরণ। অনুমোদিত: শুধু `docs/`; production কোড/migration সরাসরি না।
      সিদ্ধান্ত-নোট: KYC-withdrawal প্রশ্ন (নিচে) ও flag-dispute-retry-এর status-guard প্রশ্ন এই সেশনে
      ব্যবহারকারীর কাছ থেকে উত্তর নেওয়া হবে।
      📌 **অবস্থা (সেশন ১, ২০২৬-০৯-২১): live-যাচাই সম্পূর্ণ, apply বাকি — ধাপ `[ ]`-ই।** Supabase MCP-তে (শুধু read-only SELECT) live `release_escrow`
      টেনে দেখা গেছে **live বডি step36-এর সাথে মেলে না** (live-এ `notifications` insert-এ `role='SOLVER'` আছে) — তাই প্রস্তাবিত migration
      **live বডির ওপর পুনর্লিখিত** (step36-ভিত্তিক সংস্করণ apply করলে notification `role` হারাত)। নতুন আবিষ্কার: `release_escrow`-এর auth-চেক
      `auth.uid()` NULL (anon) হলে পাশ হয়ে যায় (`anon`-এর EXECUTE grant আছে) — প্রস্তাবে ঐচ্ছিক ব্লক B; একই প্যাটার্নের candidate
      `refund_escrow_once` (anon EXECUTE ✅) ও `deposit_money_via_gateway` (anon ❌) — scope-এর বাইরে, ব্যবহারকারীর সিদ্ধান্ত বাকি। outbox worker-এ অসীম loop নেই
      (১০ retry → `FAILED_PERMANENT`), কিন্তু `PROBLEM_DISPUTED`-ও enqueue হয় ও local phantom-release থেকে যায় (Kotlin-এডিট লাগবে, প্রস্তাব মাত্র)।
      flag-dispute status-guard প্রশ্ন ✅ মীমাংসিত (12.8a: admin COMPLETED/CANCELLED-এও flag করতে পারবেন → server status-guard নয়); KYC-withdrawal প্রশ্ন ❓ এখনো উত্তরহীন।
      বিস্তারিত ও ব্যবহারকারীর ৫টা সিদ্ধান্ত-প্রশ্ন: `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.10" সেকশন; কুয়েরি: `docs/diagnostics/step12_10_release_escrow_live_check.sql`।
      পরের সেশনে ব্যবহারকারী apply-এর নির্দেশ দিলে: (ক) "APPLY-এর আগে" md5-চেক (`443500c9…1593`), (খ) MCP `apply_migration`, (গ) "APPLY-এর পরে" চেক + rollback-DO-block টেস্ট, (ঘ) `[x]`।

- [x] **Step 12.10c — `refund_escrow_once` hardening (NULL-uid + refund-শতাংশ সীমা) [ব্যবহারকারী অনুমোদিত ২০২৬-০৯-২১; ধাপ-তালিকায় যোগ]।** ✅ **সম্পূর্ণ (২০২৬-০৯-২১, সেশন ৩): B+C live-এ apply (MCP, `step12_10c_refund_escrow_once_hardening`), Q1 md5 মিলেছে, rollback-DO টেস্ট ১০/১০ পাস — বিস্তারিত progress doc-এর "Step 12.10c সম্পূর্ণ" সেকশন। migration-ফাইল-সিঙ্ক 12.12-এ বাকি।
      📌 অবস্থা (সেশন ২): live যাচাই ✅ (md5 `7b9d7d2c…8a21`, len 2195, anon+authenticated EXECUTE ✅), প্রস্তাব
      `docs/proposed_migrations/PROPOSED_step12_10c_refund_escrow_once_hardening.sql` লেখা ✅, rollback-DO টেস্ট (DDL সহ) ১০/১০ পাস ✅, live অপরিবর্তিত;
      **apply বাকি — ব্যবহারকারীর স্পষ্ট "apply koro" লাগবে**; apply-এর পর: md5-চেক → `apply_migration` → "পরে" চেক → রিটেস্ট → `[x]`।
      `deposit_money_via_gateway`: live-এ শুধু postgres/service_role EXECUTE → বদলানো লাগেনি (যাচাইকৃত)।

- [x] **Step 12.10d — ✅ কোড-কাজ শেষ + ✅ Windows-verified (২০২৬-০৯-২১, সেশন ৪; 12.11-এর আগে নেওয়া; দুই রানই BUILD SUCCESSFUL, টেস্ট-সংখ্যা XML-এ যাচাই বাকি; ডিভাইস-runtime যাচাই বাকি) — বিস্তারিত progress doc-এর "Step 12.10d" সেকশন; payout RPC-first + REJECT-refund role সহ সব fix এই ধাপেই।** Kotlin: user-withdraw role-রুট ফিক্স + outbox permanent-error/local-rollback (Q4) [অনুমোদিত ২০২৬-০৯-২১, নতুন ধাপ; Kotlin-এডিট, rule #1a-র ব্যতিক্রম শুধু এই ধাপে নাম-ধরা জায়গায়]।**
      (১) `UserWithdrawScreen`→`viewModel.requestWithdrawal`→`repository.requestWithdrawal` সবসময় `balanceSolver` ও role `"SOLVER"` — user-role withdraw-এ role/balance সঠিক রুটে;
      (২) known-permanent error (`PROBLEM_DISPUTED`, `NOT_AUTHORIZED`, `KYC_REQUIRED`, `INVALID_REFUND_PERCENTAGE`…) `enqueueOutboxRetry`-তে না পাঠানো + local optimistic write rollback
      (`payoutEscrowToSolver`, `requestWithdrawal`)। সেশনের শুরুতে নকশা ব্যবহারকারীকে দেখাবে, কোড লেখার আগে; Windows-verify বাধ্যতামূলক।

- [x] **Step 12.10e — solver-withdrawal-এ KYC বাধ্যতামূলক (সিদ্ধান্ত ২০২৬-০৯-২১: শুধু SOLVER; unverified আটকাবে, grandfather নয়; `admin_revoke_kyc`-এও জমা ব্যালেন্স আটকাবে)।** ✅ **সম্পূর্ণ (২০২৬-০৯-২১): live-এ apply (MCP, `step12_10e_request_withdrawal_solver_kyc_guard`), guard=true কনফার্ম, post-apply live sanity ২/২ পাস (unverified SOLVER→KYC_REQUIRED, verified SOLVER→OK), client প্রি-চেক (`SolverBalanceWithdrawScreen`) আগের সেশনেই যোগ হয়েছিল — বিস্তারিত progress doc-এর "Step 12.10e" সেকশন। migration-ফাইল-সিঙ্ক 12.12-এ বাকি।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত:
      12.10d-এর পরে। live `request_withdrawal` বডি টেনে (drift-ঝুঁকি) → `ROLE_INACTIVE`-চেকের পরে `p_role='SOLVER' and not is_kyc_verified → 'KYC_REQUIRED'` প্রস্তাবিত migration →
      apply (ব্যবহারকারীর "apply koro") → client প্রি-চেক বার্তা (`SolverBalanceWithdrawScreen`)।
      📌 অবস্থা (২০২৬-০৯-২১, সেশন): ✅ live md5-drift-check (repo file-এর সাথে বাইট-বাই-বাইট মিল), ✅ প্রস্তাবিত migration লেখা (`docs/proposed_migrations/PROPOSED_step12_10e_request_withdrawal_solver_kyc_guard.sql`), ✅ rollback-DO টেস্ট ৪/৪ পাস, ✅ client প্রি-চেক (`SolverBalanceWithdrawScreen.kt`) যোগ হয়েছে। **বাকি: শুধু live apply** (ব্যবহারকারীর স্পষ্ট "apply koro" ছাড়া করা হয়নি)। বিস্তারিত: `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.10e" সেকশন।

- [x] **Step 12.11 — escrow id mismatch: live-যাচাই, তারপর নকশা।** ✅ কোড-কাজ + live migration (আগের সেশনে) + ✅ **Windows-verified (২০২৬-০৯-২১): `BUILD SUCCESSFUL`; `EscrowIdWiringTest` `tests="2" failures="0"`, `RpcErrorClassifierTest` `tests="5" failures="0"`, `DualWriteGapTest` `tests="26" failures="0"` — সব XML-এ কনফার্ম।
      কোড পড়ে সন্দেহ (অযাচাইকৃত): local escrow id `ESCROW_<৮>`, cloud RPC-র id `ESC_<uuid>`; client
      `release_escrow(local id)` পাঠায়, RPC id দিয়ে খোঁজে; realtime cloud row-কে আলাদা id-তে local-এ insert
      করে। সম্ভাব্য ফল: release/refund dual-write ESCROW_NOT_FOUND (log-only), local-এ duplicate escrow row।
      ধাপ: (১) ব্যবহারকারী `docs/diagnostics/step12_11_escrow_id_check.sql`-এর ৪টা read-only কুয়েরি চালিয়ে
      ফল দেবেন; (২) ফল অনুযায়ী নিশ্চিত করা এটা আসল বাগ কিনা (Q3-এ client-style id-র ট্রানজ্যাকশন আছে কিনা
      ইত্যাদি); (৩) আসল হলে নকশার বিকল্প (ক: client RPC-র রিটার্ন-করা `escrow_id` local-এ গ্রহণ করবে;
      খ: server-এ problem-ভিত্তিক fallback lookup; গ: id একীকরণ) — ট্রেড-অফসহ ব্যবহারকারীর কাছে, কোড
      লেখার আগে। **ফল না আসা পর্যন্ত কোনো কোড/migration নয়।** এই ধাপের fix হলে `acceptDirectContract`
      (12.8) ও `acceptBid` (12.3)-এর retry-নকশার সাথে সংগতি দেখা বাধ্যতামূলক।
      📌 অবস্থা (২০২৬-০৯-২১, সেশন ১): ✅ live কুয়েরি (MCP) — cloud-এ escrow/problem খালি, ডেটা নির্ণায়ক নয়; কোড + live ফাংশন-বডি থেকে বাগ নিশ্চিত। ✅ fix live-এ apply (`step12_11_client_supplied_escrow_id`: `accept_bid`/`accept_direct_contract` client-এর `p_escrow_id` নেয়) + Kotlin (client/outbox/`ESCROW_NOT_FOUND` transient) + `EscrowIdWiringTest`। **বাকি: শুধু Windows-verify** (`WINDOWS RESULT:` পেলে `[x]`)। বিস্তারিত: `CI_TEST_SUITE_PROGRESS.md`-এর "Step 12.11 — সম্পূর্ণ" সেকশন।

- [x] **Step 12.12 — চূড়ান্ত gate: পুরো suite সবুজ, তারপরই Step 13।**
      (১) Windows-এ **পুরো** `:app:testDebugUnitTest` (কোনো `--tests` ফিল্টার ছাড়া) — সব টেস্ট
      (আগে থেকে থাকা সহ) সবুজ কিনা; (২) `DualWriteGapTest` ২৬/২৬ pass (BLOCKED ব্যতিক্রম ছাড়া, যেগুলো
      ব্যবহারকারী সিদ্ধান্ত দিয়ে "গ্রহণযোগ্য" বা "পরে" বলেছেন); (৩) `full-test.yml`-এর
      `rpc-overload-scan` job-এ 12.9-এর পর `continue-on-error: true` সরানো যাবে কিনা মূল্যায়ন
      (ব্যবহারকারীর সম্মতি নিয়ে); (৩ক) ⚠️ **migration-ফাইল-সিঙ্ক** — MCP দিয়ে live-এ apply-করা বদলগুলো (12.8b `request_wallet_deposit`, 12.8c `user_confirm_extra_amount`,
      12.9-এর ৭টা DROP, 12.10 `release_escrow`, 12.10c `refund_escrow_once`, 12.11 `accept_bid`/`accept_direct_contract` (নতুন `p_escrow_id` + পুরনো overload DROP)) `supabase/migrations/`-এ প্রকৃত ফাইল হিসেবে commit করা (ব্যবহারকারীর review-সাপেক্ষে; তালিকা progress doc-এ) —
      নাহলে `rpc-overload-scan` ও fresh-DB CI live-এর চেয়ে পিছিয়ে থাকবে; (৪) `CI_TEST_SUITE_PROGRESS.md`-এ Step 12 চূড়ান্তভাবে বন্ধ
      ("সব fix done, Windows-verified" — BLOCKED/deferred তালিকাসহ); (৫) master-prompt-এ Step 13-এর
      GATE সরানো। **শুধু এই ধাপ `[x]` হলে পরের সেশন Step 13 নিতে পারবে।** (12.10/12.11-এর ব্যবহারকারী-নির্ভর যাচাই-ফলও এর আগে আসতে হবে।)
      ✅ **সম্পূর্ণ (২০২৬-০৯-২১, সেশন ৩)।** `WINDOWS RESULT` পাওয়া গেছে: `73 tests completed, 3 failed`।
      ৩টা fail-ই `com.example.ExampleRobolectricTest`-এ (`tests="16" failures="3"`), সবকটার
      মেসেজ একই: `java.lang.IllegalStateException: Failed to create default settings for
      SettingsSessionManager` — এটা Robolectric SDK 36 (Baklava)-এর একটা environment/
      framework-স্তরের সমস্যা (কোনো app/business-লজিক assertion ব্যর্থ হয়নি), এই সেশনের বা
      আগের কোনো সেশনের কোনো কোড-পরিবর্তনের সাথে সম্পর্কহীন (এই ফাইলটা কখনো ছোঁয়া হয়নি) —
      সম্ভবত প্রথমবার filter-ছাড়া পুরো suite রান হওয়ায় প্রথম ধরা পড়ল (আগের সব সেশনে `--tests`
      দিয়ে নির্দিষ্ট ক্লাস টার্গেট করা হয়েছিল, `ExampleRobolectricTest` কখনো রান হয়নি)।
      **ব্যবহারকারীর সিদ্ধান্ত (২০২৬-০৯-২১): এটা out-of-scope/pre-existing ধরে GATE খুলে দেওয়া হলো,
      ফিক্স না করেই** — Step 12/12.x-এর মূল লক্ষ্য (dual-write coverage, KYC guard, overload-scan) ও
      তার নিজস্ব সব টেস্ট (`DualWriteGapTest` ২৬/২৬, `DualWriteGapTestBorderline` ৪/৪, `EscrowIdWiringTest`
      ২/২, `RpcErrorClassifierTest` ৫/৫ — সব `failures="0" errors="0"`) সম্পূর্ণ সবুজ। `ExampleRobolectricTest`-এর
      এই ৩টা fail একটা **আলাদা, নতুন backlog item** হিসেবে নোট করা থাকল (progress doc-এর "Step 12.12 —
      সেশন ৩" সেকশন দেখুন) — Step 12-এর scope-এর অংশ না, তাই ভবিষ্যতে আলাদা সেশনে চাইলে ডায়াগনোজ/ফিক্স
      করা যাবে। বিস্তারিত: progress doc-এর "Step 12.12 — সেশন ৩ (GATE বন্ধ)" সেকশন।

- [x] **Step 13 — Database trigger coverage (realtime broadcast):** ✅ সম্পূর্ণ (13.1–13.8,
      সর্বশেষ ২০২৬-০৯-২২) — বিস্তারিত নিচের উপ-ধাপগুলোতে ও progress doc-এ। মূল সারাংশ: ১০টা
      trigger-ই কভার্ড, real-run-এ `broadcast_messages_changes`-এ একটা CI-only migration-order
      বাগ পাওয়া গিয়েছিল — **ব্যবহারকারীর স্পষ্ট অনুমতিতে একই সেশনে ফিক্স করা হয়েছে ও fresh-DB
      real-run দিয়ে ভেরিফাই করা হয়েছে (১১৩২/১১৩২ pass)।** shimmer-লুপ সন্দেহ: users ও
      messages উভয় trigger-ই ruled-out — root cause client-side-এ খোঁজা উচিত।
      case-insensitive স্ক্যানে ধরা পড়েছে migrations-এ **১০টা trigger**
      আছে (আগে ভুলবশত "০টা" বলা হয়েছিল, কারণ সেগুলো সব ছোট হাতের
      `create trigger` দিয়ে লেখা) — সবগুলোই realtime broadcast-এর জন্য:
      `broadcast_users_changes`, `broadcast_escrows_changes`,
      `broadcast_notifications_changes`, `broadcast_bids_changes`,
      `broadcast_transactions_changes`, `broadcast_withdrawals_changes`,
      `broadcast_gateway_payments_changes`,
      `broadcast_additional_charges_changes`,
      `broadcast_messages_changes`, `trg_set_display_uid` — এগুলোর
      পেছনের trigger-function গুলো (`notify_users_broadcast`,
      `notify_escrows_broadcast`, `notify_notifications_broadcast`,
      `notify_bids_broadcast`, `notify_transactions_broadcast`,
      `notify_withdrawals_broadcast`,
      `notify_gateway_payments_broadcast`,
      `notify_additional_charges_broadcast`,
      `notify_messages_broadcast`, `set_display_uid_on_insert`)
      কোনো pgTAP function-test-এ কভার হয় না, কারণ এগুলো সরাসরি কল হয় না
      — টেবিলে `INSERT`/`UPDATE`/`DELETE` হলে trigger হয়ে চলে।

      **সন্দেহ:** chat/profile screen-এ যে "শুধু শিমার লোড হতেই থাকে"
      বাগ রিপোর্ট হয়েছিল, সেটা সম্ভবত `broadcast_messages_changes` বা
      `broadcast_users_changes` trigger বারবার/ভুল payload দিয়ে fire
      হওয়ার কারণে হতে পারে — তাই এই ধাপ শুধু "coverage-এর জন্য" না,
      directly সেই বাগ ধরার সম্ভাবনাও আছে।

      🔀 **Step 13 আটটা উপ-ধাপে ভাগ করা হয়েছে (13.1 → 13.8), প্রতিটা আলাদা Claude
      সেশনে, এই ক্রমেই** — ১০টা trigger + বিশেষ loop-check + display_uid টেস্ট একসাথে
      একটা সেশনে লিখতে গেলে tool/context limit শেষ হয়ে যাওয়ার ঝুঁকি বেশি। **GATE:**
      13.1–13.8 সবক'টা `[x]` না হওয়া পর্যন্ত Step 14 শুরু করা যাবে না। প্রতিটা
      13.x সেশনের সাধারণ নিয়ম (নিচে আলাদা করে আর লেখা হয়নি):
      - একটা সেশন = শুধু ওই একটা 13.x ধাপ। শেষ করে zip + সারাংশ দিয়ে থেমে যাবে।
      - প্রতিটা trigger-এর broadcast payload/channel **অনুমান না করে** migration ফাইল
        থেকেই verify করবে (rule #5, rule #5a — case-insensitive grep)।
      - `supabase/tests/`-এ ফাইলের নাম-কনভেনশন: `1X_trigger_<table_group>.sql` (13.1-এ
        ঠিক করা হবে, বাকি ধাপগুলো সেটাই অনুসরণ করবে)।
      - প্রতিটা 13.x-এর শেষে `CI_TEST_SUITE_PROGRESS.md`-এ "Step 13.N" সেকশনে কোন
        trigger(গুলো) কভার হলো, কী payload/channel পাওয়া গেছে, আর কোনো সন্দেহজনক
        আচরণ (duplicate fire, ভুল payload) পাওয়া গেলে সেটা লিখে রাখবে।

  - [x] **Step 13.1 — Broadcast মেকানিজম investigation + test scaffold।** ✅ সম্পূর্ণ
        (২০২৬-০৯-২২) — এই সেশনে real psql+pgTAP দিয়ে সরাসরি চালিয়ে যাচাই করা হয়েছে
        (sandbox-এ এবার network/apt allowlist কাজ করেছে)। বিস্তারিত
        `CI_TEST_SUITE_PROGRESS.md`-এর "Step 13.1 সম্পূর্ণ" সেকশনে — সংক্ষেপে: mechanism
        (`realtime.broadcast_changes` perform-call) কনফার্ম, local stub-এর no-op bug ফিক্স,
        reusable helper (`supabase/tests/13_trigger_helpers.sql`), POC (৭/৭ pass) + পুরো
        suite real-run (১০৪২টা pgTAP assertion, ০ fail)। 🔴 একটা গুরুত্বপূর্ণ নতুন আবিষ্কার:
        CI-র alphabetical migration-apply-order `fix_users_broadcast_event_naming_collision.sql`-কে
        `realtime_scoping_step5_users_escrows_broadcast.sql`-এর আগে চালায় ⇒ CI-তে
        `notify_users_broadcast()`-এর event bare TG_OP থেকে যায়, যদিও live Supabase-এ ফিক্সটা
        সঠিকভাবে প্রযোজ্য (মিলিয়ে যাচাই করা হয়েছে MCP দিয়ে)। rule #1 অনুযায়ী migration ফাইল
        এই ধাপে ছোঁয়া হয়নি — শুধু ধরা ও রিপোর্ট করা হয়েছে, ব্যবহারকারীর সিদ্ধান্তের অপেক্ষায়। কোনো trigger-test
        এখনো লেখা হবে না, এই ধাপের কাজ হলো বাকি সাতটা ধাপের ভিত্তি তৈরি করা।
        কাজ: (১) migrations-এ `broadcast_*_changes` ও `notify_*_broadcast`
        function-বডিগুলো (case-insensitive grep) পড়ে ঠিক কীভাবে broadcast পাঠানো হয়
        সেটা নিশ্চিত করবে — `pg_notify`, `realtime.broadcast_changes`,
        `supabase_realtime.messages` টেবিলে সরাসরি insert, নাকি অন্য কিছু (অনুমান না
        করে বডি থেকে); (২) pgTAP-এ সেই broadcast আসলেই fire হয়েছে কিনা কীভাবে assert
        করা যায় (যেমন broadcast টেবিলে নতুন row এসেছে কিনা গোনা, বা `pg_notify`
        হলে pgTAP-এর যে limitation আছে সেটা নোট করা) সেটা একটা ছোট প্রুফ-অফ-কনসেপ্ট
        টেস্ট দিয়ে যাচাই করবে (যেকোনো একটা সহজ trigger, যেমন `broadcast_users_changes`
        দিয়ে); (৩) এই প্যাটার্নটা একটা reusable pgTAP helper/function হিসেবে
        (`00_helpers.sql`-এ যোগ করে, বা নতুন `13_trigger_helpers.sql`) লিখবে, যাতে
        13.2–13.7 শুধু এই helper কল করে টেস্ট লিখতে পারে, প্রতিবার mechanism নতুন করে
        বের করা না লাগে; (৪) ফাইল-নাম-কনভেনশন ঠিক করে progress doc-এ লিখবে।
        প্রত্যাশা: কোনো ফাংশনাল trigger-টেস্ট এখনো pass/fail না, শুধু infrastructure
        + ১টা প্রুফ-অফ-কনসেপ্ট (users trigger) কাজ করছে এটা নিশ্চিত।

  - [x] **Step 13.2 — `broadcast_users_changes` + `broadcast_escrows_changes` টেস্ট।** ✅
        সম্পূর্ণ (২০২৬-০৯-২২, real psql+pgTAP-এ ১৭/১৭ pass, পুরো suite ১০৫৯ assertion সবুজ)।
        এই সেশনে অতিরিক্তভাবে (ব্যবহারকারীর স্পষ্ট অনুমতিতে) Step 13.1-এ ধরা পড়া
        `notify_users_broadcast()`-এর CI-migration-order বাগও ফিক্স করা হয়েছে (নতুন
        `supabase/migrations/zz_20260913145059_users_broadcast_event_naming_fix_ci_order.sql`
        — শুধু CI/sandbox-reconstruction, live অপরিবর্তিত)। বিস্তারিত
        `CI_TEST_SUITE_PROGRESS.md`-এর "Step 13.2 সম্পূর্ণ" সেকশনে।

  - [x] **Step 13.3 — `broadcast_notifications_changes` + `broadcast_bids_changes` টেস্ট।** ✅
        সম্পূর্ণ (২০২৬-০৯-২২, real psql+pgTAP-এ ১৭/১৭ pass, পুরো suite ১০৭৬ assertion সবুজ)।
        🔴→✅ notifications-এর bare-TG_OP বাগ (live-এও বিদ্যমান ছিল, শুধু CI-order সমস্যা না) —
        ব্যবহারকারীর স্পষ্ট অনুমতিতে এই ও পরের সেশনের মাঝে **ফিক্স করা হয়েছে** (migration live-এ
        apply + Kotlin `SupabaseRealtimeManager.kt` আপডেট)। বিস্তারিত `CI_TEST_SUITE_PROGRESS.md`-এর
        "Step 13.3 সম্পূর্ণ" ও পরবর্তী "notifications broadcast fix" সেকশনে।

  - [x] **Step 13.4 — `broadcast_transactions_changes` + `broadcast_withdrawals_changes` টেস্ট।** ✅
        সম্পূর্ণ (২০২৬-০৯-২২, real psql+pgTAP-এ ১৮/১৮ pass, পুরো suite ১০৯৪ assertion সবুজ)।
        💰 money-leak চেক: nullable-guard verified (solver_id NULL হলে duplicate broadcast নেই),
        dual-owner উভয় topic-এ একই amount দেখায় (ইচ্ছাকৃত)। 🔍 নতুন observation: solver_id
        reassignment-এ পুরনো solver update মিস করে (bug না, design-limitation, rule #1-এ শুধু
        রিপোর্ট করা হলো, fix হয়নি) — বিস্তারিত `CI_TEST_SUITE_PROGRESS.md`-এ।

  - [x] **Step 13.5 — `broadcast_gateway_payments_changes` + `broadcast_additional_charges_changes` টেস্ট।** ✅
        সম্পূর্ণ (২০২৬-০৯-২২, real psql+pgTAP-এ ১৬/১৬ pass, পুরো suite ১১১০ assertion সবুজ,
        fresh-DB rebuild করে verify)। 🔴→✅ এই সেশনেই ধরা পড়েছিল ও ঠিক করা হয়েছে: নতুন টেস্ট
        ফাইলের মূল নাম alphabetically `13_trigger_helpers.sql`-এর **আগে** sort হচ্ছিল (ফাইল-নামের
        নিজস্ব CI-order বাগ, migration-এর না) — rename করে ঠিক করা হয়েছে, বিস্তারিত progress
        doc-এ। 💰 gateway_payments-এর nullable-guard ও additional_charges-এর
        unconditional-dual-broadcast দুটোই migration বডি অনুযায়ী সঠিক পাওয়া গেছে।

  - [x] **Step 13.6 — `broadcast_messages_changes` টেস্ট + multi-update loop-check (সর্বোচ্চ
        অগ্রাধিকার — shimmer-লুপ বাগের সন্দেহভাজন কারণ)।** ✅ কোড-কাজ শেষ (২০২৬-০৯-২২) — ⚠️ এই
        সেশনে sandbox network/apt কাজ করেনি (403 Forbidden), তাই **static-only যাচাই** (migration
        বডি থেকে topic/event নিশ্চিত + ম্যানুয়াল parens/syntax check, real psql+pgTAP run হয়নি) —
        বিস্তারিত progress doc-এর "Step 13.6 সম্পূর্ণ" সেকশনে। loop-check-এ কোনো সন্দেহজনক
        loop/extra-fire আচরণ কোড/migration-এ পাওয়া যায়নি (trigger শুধু AFTER + `return null`,
        কোনো recursive UPDATE নেই) — কিন্তু যেহেতু real run হয়নি, এই উপসংহার **পরবর্তী সেশনে বা
        GitHub Actions-এ real run দিয়ে নিশ্চিত করা উচিত**, এখনই চূড়ান্ত ধরা হচ্ছে না। এই ধাপ আলাদা রাখা হয়েছে
        কারণ এখানেই মূল সন্দেহ। কাজ: (১) স্বাভাবিক INSERT/UPDATE/DELETE broadcast
        টেস্ট (13.2-এর প্যাটার্নে); (২) **বিশেষ টেস্ট:** একই row পরপর n বার আপডেট করে
        broadcast ঠিক n বার fire হচ্ছে কিনা, নাকি তার বেশি/loop-এ আটকাচ্ছে — এটাই
        মূল assertion, তাই এই টেস্টে স্পষ্ট মন্তব্য থাকবে কেন এটা লেখা হচ্ছে;
        (৩) `broadcast_users_changes`-এর জন্যও (13.2-এ বেসিক টেস্ট হয়ে গেলেও) একই
        multi-update loop-check আলাদা করে চালাবে, কারণ সন্দেহের তালিকায় এটাও আছে —
        দুটো trigger-ই ইচ্ছাকৃতভাবে এই ধাপে (একসাথে) পুনরায় ছোঁয়া হচ্ছে, ডুপ্লিকেট
        টেস্ট রাখা হচ্ছে না, বরং 13.2-এর ফাইলেই নতুন loop-check টেস্ট যোগ হবে;
        (৪) সন্দেহজনক কিছু পাওয়া গেলে সেটা progress doc-এ স্পষ্টভাবে "সম্ভাব্য
        shimmer-লুপ root cause" হিসেবে চিহ্নিত করবে, কিন্তু rule #1 অনুযায়ী নিজে fix
        করবে না — শুধু ধরবে ও রিপোর্ট করবে।

  - [x] **Step 13.7 — `trg_set_display_uid`/`set_display_uid_on_insert` টেস্ট।** ✅ কোড-কাজ শেষ
        (২০২৬-০৯-২২) — ⚠️ এই সেশনেও sandbox network/apt কাজ করেনি (403 Forbidden, Step 13.6-এর
        মতোই), তাই **static-only যাচাই** (migration বডি থেকে logic নিশ্চিত + ম্যানুয়াল
        parens/plan-count check)। 🔴 এই ধাপেই স্পষ্টভাবে ডকুমেন্ট করা হলো (আগে থেকেই progress
        doc-এ পরিচিত ছিল "৬টা known-expected fail"-এর একটা হিসেবে): migration ফাইলের backfill
        DO-ব্লক CI-schema-stub-এ `created_at` কলাম না থাকায় ব্যর্থ হয়, কিন্তু trigger/function
        তৈরির statement-গুলো তার আগেই কমিট হয়ে যায় বলে টেস্টের জন্য ব্লকার না — বিস্তারিত
        progress doc-এর "Step 13.7 সম্পূর্ণ" সেকশনে। **real psql+pgTAP run এখনো বাকি।**
        নতুন user insert হলে `display_uid` সঠিকভাবে generate হচ্ছে কিনা, format
        ঠিক আছে কিনা, আর duplicate/collision হলে কী হয় (retry করে নাকি error দেয়) —
        migration বডি থেকে verify করে টেস্ট লিখবে।

  - [x] **Step 13.8 — `full-test.yml`-এ ওয়্যারিং + Step 13 চূড়ান্ত সারাংশ।** ✅ সম্পূর্ণ
        (২০২৬-০৯-২২) — এই সেশনে sandbox network কাজ করেছে, তাই পুরো Step 1–13 suite
        real psql+pgTAP দিয়ে fresh DB-তে চালানো হয়েছে (`local_pgtap_bootstrap.sh`)।
        wiring/cross-check সব ঠিক পাওয়া গেছে (১০টা trigger-ই কভার্ড, `full-test.yml`
        এডিট লাগেনি)। 🔴→✅ **CI-ব্লকিং বাগ ধরা পড়েছিল ও একই সেশনে ফিক্স হয়েছে
        (ব্যবহারকারীর স্পষ্ট অনুমতিতে):** `realtime_scoping_step3_messages_broadcast.sql`
        alphabetically-আগে-আসা `realtime_scoping_messages_solver_thread_isolation.sql`-এর
        সাথে policy-নাম collide করে ব্যর্থ হচ্ছিল ⇒ `broadcast_messages_changes`
        trigger/ফাংশন CI-তে তৈরিই হতো না। **ফিক্স:** migration-এ `drop policy if
        exists ...` গার্ড যোগ (১ লাইন, শুধু idempotency, logic অপরিবর্তিত)। fresh-DB
        real-run দিয়ে ভেরিফাই করা হয়েছে — এখন `13_trigger_messages.sql`-এর ১০/১০
        pass, পুরো suite ১১৩২/১১৩২ pass। shimmer-লুপ সন্দেহ: `users` ও `messages`
        দুটোরই জন্য এখন real-run দিয়ে **ruled-out** — root cause client-side-এ
        খোঁজা উচিত (এই suite-এর স্কোপের বাইরে)। বিস্তারিত progress doc-এর "Step
        13.8 সম্পূর্ণ" ও "Step 13.8-পরবর্তী ফলো-আপ" সেকশনে। **Step 13 সম্পূর্ণরূপে
        বন্ধ, GATE অনুযায়ী Step 14 এখন খোলা।**

- [x] **Step 14 — RLS policy coverage (নিরাপত্তা):** ✅ সম্পূর্ণ (14.1–14.4, ২০২৬-০৯-২২ সেশনসমূহ) —
      ৪টা distinct CREATE POLICY-ই কভারড (`messages_select`+broadcast [14.2], notifications
      broadcast [14.3], bids broadcast [14.4])। `bids_select`/`problems_select` (ALTER-only, কোনো
      CREATE POLICY নেই কোথাও) ইচ্ছাকৃতভাবে formal স্কোপের বাইরে রাখা হয়েছে — বিস্তারিত progress
      doc-এর "Step 14 চূড়ান্ত সারাংশ" সেকশনে। 14.3-এর broadcast policy real-run দিয়ে re-confirm
      এখনো বাকি (network sandbox-এ পাওয়া যায়নি 14.3/14.4 কোনোটাতেই) — backlog হিসেবে নথিভুক্ত।
      Migrations-এ **৫টা `CREATE POLICY`** statement আছে (মূলত
      `realtime_scoping_*` ফাইলগুলোতে — messages, notifications, bids)।
      এগুলো নির্ধারণ করে কোন user কার data দেখতে/এডিট করতে পারবে। এখনো
      পর্যন্ত কোনো টেস্ট নেই যেটা verify করে যে একজন user সত্যিই
      আরেকজনের row দেখতে/বদলাতে পারছে **না**। বিশেষভাবে `messages`/
      `notifications`/`bids` টেবিলে — এই তিনটাই আগে থেকেই আলাদাভাবে
      "scoping" migration পেয়েছে, মানে এখানে আগে কোনো leak-bug ধরা
      পড়েছিল বলেই আলাদা migration লাগানো হয়েছিল — সেই ফিক্সটা এখনো
      ঠিকমতো কাজ করছে কিনা সেটাই আসলে সবচেয়ে জরুরি regression-check
      এখানে।

      🔀 **Step 14 চারটা উপ-ধাপে ভাগ করা হয়েছে (14.1 → 14.4), প্রতিটা আলাদা Claude
      সেশনে, এই ক্রমেই।** **GATE:** 14.1–14.4 সবক'টা `[x]` না হওয়া পর্যন্ত Step 15
      শুরু করা যাবে না। প্রতিটা টেস্টে দুইটা ভিন্ন user হিসেবে
      (`set_config('request.jwt.claims', ...)` দিয়ে auth context সুইচ করে) একই query
      চালিয়ে verify করবে: (ক) নিজের row দেখা/এডিট করা যাচ্ছে, (খ) অন্যের row দেখা/
      এডিট করা যাচ্ছে **না**।

  - [x] **Step 14.1 — ৫টা policy-র exact discovery + dual-auth-context test helper।** ✅ সম্পূর্ণ
        (২০২৬-০৯-২২) — ৫টা CREATE POLICY statement → ৪টা distinct policy (পূর্ণ discovery টেবিল
        `CI_TEST_SUITE_PROGRESS.md`-এর "Step 14.1 সম্পূর্ণ" সেকশনে ও `supabase/tests/14_rls_00_helpers.sql`-এর
        হেডারে), reusable helper (`test.set_topic`/`test.clear_topic`/`test.count_as`/`test.seed_rls_users`)
        তৈরি — কোনো ফাংশনাল policy-টেস্ট এখনো লেখা হয়নি (নিয়ম অনুযায়ী)। বোনাস আবিষ্কার:
        `problems_select`/`bids_select` (core table policy, শুধু ALTER, কোথাও CREATE নেই) 14.4-এর জন্য
        প্রাসঙ্গিক হতে পারে। real psql+pgTAP-এ পুরো suite (Step 1-13 + নতুন helper) zero regression
        ভেরিফাইড। কোনো ফাংশনাল policy-টেস্ট এখনো লেখা হবে না। কাজ: (১)
        `supabase/migrations/*.sql`-এ `create policy` (case-insensitive, rule #5a)
        খুঁজে সব ৫টা policy-র exact টেবিল, কোন role/condition-এর জন্য, আর কী
        allow/deny করে সেটা একটা তালিকা বানিয়ে progress doc-এ লিখবে; (২)
        `set_config('request.jwt.claims', ...)` দিয়ে দুইটা fake user (user A, user B)
        তৈরি করে auth context সুইচ করার একটা reusable pgTAP helper লিখবে
        (`00_helpers.sql`-এ যোগ করে, বা নতুন `14_rls_helpers.sql`) — 14.2–14.4 এটাই
        ব্যবহার করবে।

  - [x] **Step 14.2 — `messages` টেবিলের policy টেস্ট।** ✅ সম্পূর্ণ (২০২৬-০৯-২২) —
        `supabase/tests/14_rls_messages.sql` (plan(14), real psql+pgTAP-এ ১৪/১৪ pass,
        পুরো suite ১১৪৬/১১৪৬ zero regression)। table SELECT (`messages_select`) +
        broadcast SELECT ("problem participants can receive problem-topic broadcasts")
        দুটোই কভার — owner/sender/receiver/admin দেখতে পারে, অন্য solver (reassigned-
        সদৃশ) ও সম্পূর্ণ অসম্পর্কিত user/anon দেখতে পারে না (⭐ solver-thread-isolation
        regression-check pass)। বিস্তারিত progress doc-এর "Step 14.2 সম্পূর্ণ" সেকশনে
        (grant-gap + RESET ROLE ফিক্স + trigger-generated broadcast-row আবিষ্কার সহ)।
        `messages_insert` policy কভার করা হয়নি (migrations-এ কোথাও CREATE/ALTER নেই,
        শুধু কমেন্টে উল্লেখ — rule #5 অনুযায়ী স্কোপের বাইরে রাখা হয়েছে)।

  - [x] **Step 14.3 — `notifications` টেবিলের policy টেস্ট।** ✅ সম্পূর্ণ (২০২৬-০৯-২২, static-only —
        network sandbox-এ কাজ করেনি এই সেশনে, পরের সেশনে real-run দিয়ে re-confirm বাকি) —
        `supabase/tests/14_rls_notifications.sql` (plan(6), broadcast-only কারণ table-level
        policy migrations-এ নেই)। আগের সেশনে ধরা পড়া root cause (প্রতিটা assertion-এর আগে
        `test.set_topic()` কল মিসিং ছিল) ফিক্স করা হয়েছে + `seed_rls_users()`-এর side-effect
        broadcast `test.clear_broadcasts()` দিয়ে সরানো হয়েছে। বিস্তারিত progress doc-এর
        "Step 14.3 সম্পূর্ণ" সেকশনে (static trace verification সহ)।

  - [x] **Step 14.4 — `bids` টেবিলের policy টেস্ট + Step 14 ওয়্যারিং ও সারাংশ।** ✅ সম্পূর্ণ
        (২০২৬-০৯-২২, static-only — network sandbox-এ ছিল না) — `supabase/tests/14_rls_bids.sql`
        (plan(10)) broadcast policy #৪ ("problem bids visibility broadcasts") কভার করে:
        owner/bid-solver/admin visibility + অন্য solver/unrelated user/anon ব্লক (৬টা), OPEN+public
        branch-এর positive+negative parity (২টা — Firebase-parity broad visibility আর is_public=false
        হলে সেই branch honored হয় কিনা), is_admin() bypass বোনাস edge-case (২টা)। `full-test.yml`
        এডিট লাগেনি (auto-glob)। ৪টা distinct policy-ই (৫টা CREATE POLICY statement-এর মধ্যে)
        এখন কভারড। `bids_select`/`problems_select` (ALTER-only) স্কোপের বাইরে — বিস্তারিত progress
        doc-এ।

- [x] **Step 15 — UI-layer display/calculation logic bug-class:** ✅ সম্পূর্ণ (২০২৬-০৯-২২, 15.1–15.4
      সবক'টা `[x]`, static-only — Windows real-run confirmation বাকি সব উপ-ধাপেই)। সারাংশ:
      `TransactionHistoryScreen.kt`-এ যে বাগ পাওয়া গিয়েছিল (`isEarning`/
      `isPositive` classification `ADMIN_ADJUSTMENT` টাইপের transaction-এ
      সবসময় "−" দেখায়, add/deduct দুটোতেই) — এটা কোনো SQL বা dual-write
      বাগ না, pure Compose UI রেন্ডারিং লজিকে ভুল branching। এই ক্লাসের
      বাগ কোনো আগের step ধরে না।

      🔀 **Step 15 চারটা উপ-ধাপে ভাগ করা হয়েছে (15.1 → 15.4), প্রতিটা আলাদা Claude
      সেশনে, এই ক্রমেই।** **GATE:** 15.1–15.4 (এবং 15.3-এর ভেতরের সব উপ-উপ-ধাপ, নিচে
      দেখো) সবক'টা `[x]` না হওয়া পর্যন্ত Step 16 শুরু করা যাবে না। Kotlin JVM টেস্ট
      হওয়ায় প্রতিটা 15.x-এর শেষে ব্যবহারকারীকে Windows-verification কমান্ড দেবে
      (Step 12.x-এর ফরম্যাটেই, শুধু `--tests` আর্গুমেন্টে সংশ্লিষ্ট নতুন test class-এর
      নাম বসাবে) এবং সেশনের শুরুতে আগের `WINDOWS RESULT:` থাকলে সেটা আগে প্রসেস করবে —
      Step 12.x-এর "প্রতিটা 12.x সেশনের সাধারণ নিয়ম" এখানেও প্রযোজ্য (sandbox
      Gradle-চেক, নিজে রান না করে pass বলবে না ইত্যাদি)।

  - [x] **Step 15.1 — Inventory (কোনো কোড/টেস্ট এখনো লেখা হবে না)।** ✅ সম্পূর্ণ (২০২৬-০৯-২২) —
        সব ৬৪টা screen ফাইল স্ক্যান করা হয়েছে। **তালিকা ক (সরাসরি ঝুঁকিপূর্ণ, একই বাগ-ক্লাস):**
        `TransactionHistoryScreen.kt` (মূল রিপোর্ট-করা বাগ), `UserWalletScreen.kt` (হুবহু ডুপ্লিকেট
        লজিক), `DashboardScreen.kt` (স্বতন্ত্র/আরও ঝুঁকিপূর্ণ — `TransactionHelper`-ই ব্যবহার করে না,
        `isDeposit` চেক নেই)। **তালিকা খ (সম্পর্কিত কিন্তু sign-bug না, বাদ):**
        `SolverCompletedJobsScreen.kt`, `AdminTransactionsView.kt`, `AdminUserLookupView.kt`,
        `AdminUsersView.kt`, `ReputationDetailScreen.kt`, `AdminReputationEngineView.kt`,
        `AdminCancelledBidsView.kt`, `ChatScreen.kt`, `InstantJobHistoryScreen.kt`,
        `WithdrawalHistoryScreen.kt`, `SolverBalanceWithdrawScreen.kt`, `AdminWithdrawalsView.kt` —
        যুক্তিসহ বিস্তারিত টেবিল progress doc-এর "Step 15.1 সম্পূর্ণ" সেকশনে। Step 15.3 সম্ভবত
        ২টা sub-step লাগবে (`UserWalletScreen.kt`, `DashboardScreen.kt`) — চূড়ান্ত ভাঙন 15.3-এর
        নিজস্ব সেশনে।

  - [x] **Step 15.2 — `TransactionHistoryScreen.kt` (আসল রিপোর্ট-করা বাগ) fix ও টেস্ট।** ✅ কোড-কাজ
        শেষ (২০২৬-০৯-২২) — sandbox network blocked (Windows-verification বাকি)। root cause:
        migrations-এ `net_amount` সব transaction-এ sign-সহ লেখা হয় (positive=credit,
        negative=debit), কিন্তু পুরনো `isPositive = isEarning || isUserRefund || isUserDeposit`
        মাত্র ৩টা শেপ চিনত, বাকি সব type-এ নীরবে `false`। ফিক্স: নতুন
        `transactionDisplaySign(trx, viewerId): Boolean` pure function — সরাসরি `trx.netAmount
        >= 0.0` ব্যবহার করে (এই স্ক্রিনের row-গুলো ইতিমধ্যে `matchesRoleForHistory()` দিয়ে
        pre-filtered বলে sign নির্ভরযোগ্য), টাইপ-এনুমারেশনের দরকারই নেই — future-proof। বোনাস
        বাগ পাওয়া গেছে: `WITHDRAWAL_REFUND`ও একই বাগে ভুগছিল। ২৩টা টেস্ট কেস — বিস্তারিত progress
        doc-এর "Step 15.2 সম্পূর্ণ" সেকশনে।

  - [x] **Step 15.3 — 15.1-এ পাওয়া বাকি স্ক্রিনগুলোর জন্য একই treatment।** ✅ কোড-কাজ
        শেষ (২০২৬-০৯-২২, static-only — sandbox network blocked, Windows-verification বাকি)।
        সাব-স্টেপ ভাঙা লাগেনি — 15.1-এর inventory অনুযায়ী ঠিক ২টা স্ক্রিন বাকি ছিল
        (`UserWalletScreen.kt`, `DashboardScreen.kt`), যা নিয়মের "১–২টার বেশি না" সীমার
        মধ্যেই পড়ে, তাই একই সেশনে দুটোই নেওয়া হলো (15.3a/15.3b তৈরি করা হয়নি)। **নতুন
        কোনো pure function বের করা হয়নি** — Step 15.2-এর `transactionDisplaySign()`
        (`TransactionHistoryScreen.kt`, একই `com.example.ui.screens` প্যাকেজ, import
        ছাড়াই visible) দুটো স্ক্রিনেই পুনর্ব্যবহার করা হয়েছে: `UserWalletScreen.kt`
        (লাইন ~1340, `viewerId = currentUid`) আর `DashboardScreen.kt` (লাইন ~944,
        `viewerId = currentSolverId`, যেটা `recentTransactions`-এর
        `matchesRoleForHistory(it, "SOLVER", currentSolverId)` স্কোপিং-এর সাথে সঙ্গতিপূর্ণ)।
        যেহেতু sign-লজিকের behavior ইতিমধ্যে Step 15.2-এর `TransactionDisplaySignTest.kt`-এ
        (২৩ কেস) সম্পূর্ণ কভার, নতুন সোর্স-স্ক্যান রেগ্রেশন টেস্ট
        `app/src/test/java/com/example/ui/screens/WalletDashboardSignWiringTest.kt` লেখা
        হয়েছে যেটা শুধু wiring যাচাই করে (সঠিক ফাংশন + সঠিক viewerId কল হচ্ছে কিনা, পুরনো
        buggy inline expression ফিরে আসেনি কিনা, কোনো প্রতিদ্বন্দ্বী duplicate ফাংশন declare
        হয়নি কিনা) — বিস্তারিত progress doc-এর "Step 15.3 সম্পূর্ণ" সেকশনে।

  - [x] **Step 15.4 — Step 15 ওয়্যারিং ও চূড়ান্ত সারাংশ।** ✅ সম্পূর্ণ (২০২৬-০৯-২২,
        static-only — sandbox network blocked, Windows-verification বাকি)। (১) `app/build.gradle.kts`-এ
        কোনো custom test-source-set/include-exclude ফিল্টার নেই বলে যাচাই করা হয়েছে — Android Gradle
        Plugin-এর ডিফল্ট `src/test/java` auto-discovery-তেই `TransactionDisplaySignTest.kt` ও
        `WalletDashboardSignWiringTest.kt` দুটোই ধরা পড়ার কথা, কোনো এডিট লাগেনি (Step 12-এর মতোই);
        (২) 15.1-এর তালিকা ক-এর ৩টা স্ক্রিনই (`TransactionHistoryScreen.kt` [15.2],
        `UserWalletScreen.kt`, `DashboardScreen.kt` [দুটোই 15.3]) কভার হয়েছে — ক্রস-চেক pass;
        (৩) নিচে সম্মিলিত Windows-verification কমান্ড; (৪) নিচে ও progress doc-এ Step 15-এর চূড়ান্ত
        সারাংশ।

- [x] **Step 16 — Offline-action-gating (client-side feature, network/role
      state):** এই zip-এর নাম নিজেই ("...-offline-action-gating-...")
      বলে দেয় এটা একটা বড়, সক্রিয়ভাবে develop-হওয়া client-side feature —
      কোনো একক RPC/trigger না, বরং `MainActivity.kt`,
      `SomadhanViewModel.kt`, `NoInternetScreen.kt`, `AppDatabase.kt`,
      `AdminSettingsView.kt`, `ChatScreen.kt`, `NetworkConnectivityObserver.kt`
      জুড়ে ছড়ানো state-machine লজিক — network না থাকলে কোন action
      block হবে, admin settings-এ "strict online block" toggle disable
      করলে সেটা mandatory হবে কিনা। আগে রিপোর্ট হয়েছিল এই toggle disable
      করলেও network ছাড়া app-এ ঢোকা যাচ্ছিল (ফিচারটাই কাজ করছিল না)।

      🔀 **Step 16 চারটা উপ-ধাপে ভাগ করা হয়েছে (16.1 → 16.4), প্রতিটা আলাদা Claude
      সেশনে, এই ক্রমেই।** **GATE:** 16.1–16.4 সবক'টা `[x]` না হওয়া পর্যন্ত Step 17
      শুরু করা যাবে না। 16.3-এ Windows-verification প্রযোজ্য (Kotlin JVM/Robolectric
      টেস্ট, Step 15-এর নিয়মেই)। rule #1 অনুযায়ী এই স্টেপেও কোনো production
      bug নিজে থেকে fix করা যাবে না (Step 12.x-এর মতো কোনো ব্যতিক্রম এখানে নেই) —
      শুধু investigate ও টেস্ট লিখে ধরবে, ফলাফল ও প্রস্তাব progress doc-এ রাখবে।

  - [x] **Step 16.1 — Gating-লজিক ইনভেন্টরি (কোনো টেস্ট এখনো না)।** ✅ সম্পূর্ণ (২০২৬-০৯-২২, দেখুন
        progress doc) — সাতটা নির্দিষ্ট ফাইলের ভূমিকা + broader grep-এ নতুন প্রাসঙ্গিক ফাইল
        (`SomadhanRepository.kt`) + `SomadhanViewModel.kt`-এর ৯৬টা গার্ডেড ফাংশন (ডোমেইন-গ্রুপড) +
        ১৩টা ইচ্ছাকৃতভাবে গার্ডবিহীন ফাংশন, সবই টেবিল আকারে progress doc-এ। Step 16.2-এর জন্য দুটো
        তদন্ত-কোণ ফ্ল্যাগ করা হয়েছে (AppDatabase-এ seed row অনুপস্থিত, NetworkConnectivityObserver-এর
        আগের ফিক্স re-verify)। (১) উপরের ৭টা
        ফাইলে gating-লজিক কোথায় কোথায় আছে (কোন action network-check-এর পেছনে
        gate করা, কোনটা না) — সেটার একটা টেবিল বানাবে; (২) `app` ফোল্ডারের বাকি
        কোন কোন Kotlin ফাইল "offline"/"gating"/"network" শব্দ নিয়ে কাজ করে সেটাও
        grep করে দেখে নেবে, যাতে এই inventory-টা সম্পূর্ণ হয়, শুধু উপরের ৭টা ফাইলে
        সীমাবদ্ধ না থেকে — নতুন ফাইল পাওয়া গেলে তালিকায় যোগ করবে; (৩) পুরো তালিকা
        `CI_TEST_SUITE_PROGRESS.md`-এ "Step 16 inventory" সেকশনে রাখবে — 16.2/16.3-এর
        স্কোপ এখান থেকেই আসবে।

  - [x] **Step 16.2 — Admin flag persistence trace + root-cause investigation।** ✅ সম্পূর্ণ
        (২০২৬-০৯-২২, দেখুন progress doc "Step 16.2 সম্পূর্ণ" সেকশন) — flag persistence পুরোপুরি
        ট্রেস করা হয়েছে (Room ↔ Supabase `platform_settings.strict_offline_block`, admin-only RLS
        write, targeted single-row cloud→Room pull শুধু startup/login-এ)। তিনটা স্বতন্ত্র,
        কোড-কনফার্মড root-cause মেকানিজম পাওয়া গেছে: (ক) `updatePlatformSetting()`-এর cloud
        dual-write সম্পূর্ণ best-effort/log-only, কোনো outbox-retry বা admin-visible failure নেই
        (Step 12-এর inventory-তে আগে মিস হয়ে গিয়েছিল, নতুন শনাক্ত unprotected সাইট); (খ)
        `requireOnlineOrWarn()`-এর নিজস্ব `_isOnline` (ViewModel-এর, `NetworkConnectivityObserver.kt`
        থেকে আলাদা) শুধু `NET_CAPABILITY_INTERNET` চেক করে, `NET_CAPABILITY_VALIDATED` না —
        captive-portal/"wifi আছে ইন্টারনেট নেই" false-positive সম্ভব; (গ) `loginAsAdmin()` real
        Supabase Auth সাইন-ইন ব্যর্থ হলেও local-only degraded সেশনে এগিয়ে যায় — সেই অবস্থায়
        `is_admin(auth.uid())` RLS-গেটেড সব cloud write deterministically ব্যর্থ হবে, নীরবে। এই
        তিনটার যেকোনোটা (বা সমন্বয়) app-restart-এ `syncStrictOfflineBlockSettingFromCloud()`-এর
        stale-cloud-value ওভাররাইটের মাধ্যমে ঠিক রিপোর্ট-করা বাগ তৈরি করতে পারে। কোনো কোড বদলানো
        হয়নি (rule ১) — শুধু root cause + প্রস্তাবিত ফিক্স-দিকনির্দেশনা progress doc-এ লেখা হয়েছে।

  - [x] **Step 16.3 — Robolectric/JVM টেস্ট: flag বনাম actual gating sync।** ✅ সম্পূর্ণ
        (২০২৬-০৯-২২, দেখুন progress doc "Step 16.3 সম্পূর্ণ" সেকশন) — sub-step-এ ভাঙার দরকার
        হয়নি (regex-লুপ দিয়ে ৯৫টা ফাংশনই এক টেস্টে কভার হয়, Step 15.3-এর মতোই এক সেশনে শেষ)।
        Sandbox-এর একই architectural সীমাবদ্ধতার কারণে (`DualWriteGapTest.kt`/Step 12 দ্রষ্টব্য —
        কোনো DI seam নেই, `mockk` নেই) real Robolectric ViewModel-instantiation-এর বদলে static
        source-scan টেস্ট (`OfflineGatingSyncTest.kt`, ৬টা `@Test`) — `requireOnlineOrWarn()`-এর
        structural verification, ৯৫টা গার্ডেড ফাংশনের সব occurrence, dry-run-conditional exception,
        ১১টা ইচ্ছাকৃতভাবে গার্ডবিহীন ফাংশন, আর `MainActivity.kt`-এর strict/non-strict branching।
        এই ধাপেই Step 16.1-এর inventory-তে auth-domain-এ একটা citation-শিফট এরর ধরা পড়েছে ও
        সংশোধন করা হয়েছে (`loginAsAdmin` আসলে গার্ডবিহীন, `validateLoginCredentials`/`register`
        আসলে গার্ডেড — কোনো production bug না, শুধু ইনভেন্টরি-ডকুমেন্টেশন ভুল, rule ১ অনুযায়ী কোনো
        কোড বদলানো হয়নি)। Windows real-run confirmation এখনো বাকি (Step 15-এর নিয়মেই)।

  - [x] **Step 16.4 — Step 16 ওয়্যারিং ও চূড়ান্ত সারাংশ।** ✅ সম্পূর্ণ (২০২৬-০৯-২২, দেখুন progress doc
        "Step 16.4 সম্পূর্ণ" সেকশন) — `OfflineGatingSyncTest.kt` ডিফল্ট Gradle discovery-তে ধরা পড়ার
        কথা তা structurally কনফার্মড (কোনো wiring এডিট লাগেনি), Windows real-run কমান্ড দেওয়া হয়েছে,
        আর 16.1 (সংশোধিত ইনভেন্টরি) ও 16.2 (তিনটা root-cause finding, ফিক্স প্রয়োগ করা হয়নি)
        রেফারেন্স করে Step 16-এর চূড়ান্ত সারাংশ লেখা হয়েছে। (১) নতুন সব test file
        `./gradlew test`-এ ধরা পড়ছে কিনা নিশ্চিত; (২) Windows-এ চালিয়ে ফল নিশ্চিত
        করার কমান্ড দেবে; (৩) 16.2-এর root-cause finding আর 16.1-এর inventory
        রেফারেন্স করে progress doc-এ Step 16-এর চূড়ান্ত সারাংশ লিখবে (বাগ থাকলে
        সেটা "ফিক্স-প্রস্তাব, প্রয়োগ করা হয়নি" হিসেবে স্পষ্ট করে)।

- [x] **Step 17 — Supabase Edge Function coverage (Deno runtime, আলাদা
      test-layer):** `supabase/functions/admin-reset-user-password/index.ts` —
      এটা SQL RPC না, Kotlin-ও না, **Deno-তে চলা TypeScript serverless
      function**, তাই আগের কোনো step-এর pgTAP বা JVM test-runner এটা
      cover করে না। এটা security-critical (admin panel থেকে যেকোনো
      user/solver-এর auth password reset করে)। কোডের কমেন্টেই লেখা
      আছে এটা "আগের/আনলগড সেশনে লেখা ও লাইভ প্রজেক্ট থেকে কপি করা" —
      মানে এটাই একমাত্র জায়গা যেখানে এই ফাংশনের লজিক দেখা যাচ্ছে,
      তাই ভুল থাকলে সেটা ধরার এটাই একমাত্র সুযোগ।

      🔀 **Step 17 তিনটা উপ-ধাপে ভাগ করা হয়েছে (17.1 → 17.3), প্রতিটা আলাদা Claude
      সেশনে, এই ক্রমেই।** **GATE:** 17.1–17.3 সবক'টা `[x]` না হওয়া পর্যন্ত Step 18
      শুরু করা যাবে না।

  - [x] **Step 17.1 — সম্পূর্ণ Edge Function স্ক্যান + `is_admin()` stub + Deno test
        scaffold।** ✅ সম্পূর্ণ (২০২৬-০৯-২৩, দেখুন progress doc "Step 17.1 সম্পূর্ণ"
        সেকশন) — (১) `supabase/functions/` পুরোটা আবার scan করে কনফার্মড শুধু
        `admin-reset-user-password`-ই একমাত্র Edge Function (নতুন কিছু পাওয়া যায়নি);
        (২) `is_admin()`-এর জন্য কোনো নতুন SQL stub বানানো হয়নি — বরং `installMockSupabaseFetch()`
        হেল্পারে একটা mocked HTTP-response স্টাব বানানো হয়েছে (Deno-layer এই কখনো
        সরাসরি Postgres ছোঁয় না), আর এই সেশনেই একটা গুরুত্বপূর্ণ mismatch ধরা পড়েছে:
        index.ts কল করে `rpc("is_admin", { uid })` (parameter-নাম "uid") কিন্তু
        Step 1-এর pgTAP `is_admin(p_user_id uuid)` স্টাব parameter-নাম "p_user_id" —
        দুটো independent অনুমান নিজেদের মধ্যেই অমিল, তাই real live signature অনিশ্চিত
        (progress doc-এ বিস্তারিত, কোনো ফিক্স করা হয়নি, rule ১); (৩) standalone Deno
        test আর্কিটেকচার বেছে নেওয়া হয়েছে (Postgres service লাগবে না — supabase-js
        সব network কল `globalThis.fetch` mock দিয়ে intercept করা হয়), `Deno.serve`-capture
        কৌশল দিয়ে (index.ts এডিট না করেই) trivial smoke-test লেখা হয়েছে যেটা প্রমাণ
        করে handler import+invoke করা যায় — কিন্তু **এই sandbox-এ real run verify করা
        যায়নি** (jsr.io এই sandbox-এর network allowlist-এ নেই, 403 host_not_allowed,
        curl+deno check দুই জায়গাতেই reproduce করা হয়েছে) — capture-mechanism নিজে
        একটা jsr-import-ছাড়া POC ফাইল দিয়ে সত্যিই sandbox-এ চালিয়ে যাচাই করা হয়েছে।
        Windows/CI real-run verify command প্রতিটা নতুন ফাইলের হেডার-কমেন্টে ও progress
        doc-এ দেওয়া আছে। full-test.yml-এ এখনো কোনো wiring করা হয়নি — Step 13-এর
        প্যাটার্নের মতোই সেটা 17.3-এ (চূড়ান্ত সারাংশের সাথে) হবে।

  - [x] **Step 17.2 — Authorization টেস্ট (৪০১/৪০৩) — সবচেয়ে বেশি অগ্রাধিকার।**
        17.1-এর scaffold ব্যবহার করে: (ক) কোনো `Authorization` header ছাড়া কল
        করলে ৪০১; (খ) authenticated কিন্তু non-admin user কল করলে ৪০৩ (এখানেই যদি
        বাগ থাকে — যেমন `is_admin()` সবসময় true রিটার্ন করে বা exception-কে
        silently pass হিসেবে treat করে — সেটাই সবচেয়ে বড় ঝুঁকি, তাই এই দুটো কেসই
        আলাদা ধাপে, careful ভাবে লেখা হচ্ছে)। প্রত্যাশা: বাগ থাকলে এই সেশনেই
        fail দেখানোর কথা (rule #1 অনুযায়ী নিজে fix করবে না, শুধু ধরবে)।

  - [x] **Step 17.3 — Success/validation টেস্ট + `full-test.yml`-এ ওয়্যারিং + চূড়ান্ত
        সারাংশ।** ✅ সম্পূর্ণ (২০২৬-০৯-২৩, দেখুন progress doc "Step 17.3 সম্পূর্ণ" সেকশন)
        — (১) (গ) admin সঠিক call → ২০০ {result:"OK"}; (ঘ) new_password < 6 char → ৪০০
        INVALID_PASSWORD; প্লাস target_user_id missing/non-string → ৪০০
        INVALID_TARGET_USER_ID, আর admin+auth ঠিক কিন্তু `updateUserById` নিজে ব্যর্থ হলে
        → ৪০০ (real `updateErr.message`, npm থেকে টানা `@supabase/auth-js` সোর্স দিয়ে
        mock-শেপ এই সেশনেই cross-verified); (২) `full-test.yml`-এ `edge-function-tests`
        নামে নতুন স্বতন্ত্র job বসানো হয়েছে (`denoland/setup-deno`, কোনো আগের job-এর সাথে
        মেশানো হয়নি); (৩) 17.1-এ `admin-reset-user-password`-ই একমাত্র Edge Function
        কনফার্মড ছিল, নতুন কিছু পাওয়া যায়নি বলে এই বুলেট প্রযোজ্য হয়নি; (৪) progress
        doc-এ Step 17-এর ও সামগ্রিক master prompt সম্পূর্ণ হওয়ার চূড়ান্ত সারাংশ লেখা
        হয়েছে। **GATE অনুযায়ী Step 17 সম্পূর্ণরূপে বন্ধ, Step 18 এখন খোলা।**

- [x] **Step 18 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, GATE: 18.1–18.4 সব `[x]`) — Multi-account/same-device
      balance-escrow desync (ব্যবহারকারীর রিপোর্ট করা বাগ, ২০২৬-০৯-২৩ চ্যাট-সেশনে যোগ করা
      হয়েছিল)। Root cause নিশ্চিত (`SupabaseRealtimeManager`-এর per-user realtime broadcast
      subscription account-switch-এ re-scope হয় না), regression test লেখা, প্রস্তাবিত ফিক্স
      লেখা (প্রয়োগ করা হয়নি) — বিস্তারিত progress doc-এর "🎉 সম্পূর্ণ CI Test Suite Master
      Prompt — চূড়ান্ত সারাংশ" সেকশনে।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত:

      **রিপোর্ট-করা repro (ব্যবহারকারীর ভাষায়, হুবহু):** একই ফোন থেকে (ক) USER অ্যাকাউন্টে
      লগইন করে একটা problem post করা হয়; (খ) একই ফোনে logout করে অন্য (SOLVER) অ্যাকাউন্টে
      লগইন করে সেই নিজেরই post-এ bid দেওয়া হয়; (গ) আবার logout করে আগের USER অ্যাকাউন্টে
      লগইন করে সেই bid accept করা হয় — balance ৮০০→৬০০ (২০০ টাকা কাটা), escrow-এ ২০০ টাকা
      locked দেখায় (এই পর্যন্ত প্রত্যাশিতই); (ঘ) logout করে SOLVER অ্যাকাউন্টে লগইন করে
      কাজ complete করা হয় (বা না করা হয়, ব্যবহারকারীর ভাষায় "no bropar na" — সিদ্ধান্তহীন,
      দুটো কেসই investigate করা লাগবে); (ঙ) আবার logout করে USER অ্যাকাউন্টে ফিরে আসলে
      **balance আবার ৮০০ দেখায় (৬০০ থেকে রিসেট হয়ে গেছে)**, অথচ **escrow তখনও ২০০ টাকা
      locked দেখায়** — অর্থাৎ balance আর escrow পরস্পরবিরোধী অবস্থায়; (চ) সেই escrow release
      করার পরও **balance ৮০০-ই থেকে যায়** (release-এর কোনো প্রভাব balance-এ দেখা যায় না);
      (ছ) SOLVER-এর দিকে transaction log-এ commission বাদ দিয়ে ১৮০ টাকা "received" লেখা
      আছে, কিন্তু SOLVER-এর **balance ০ দেখায়** (ledger/transaction-row আর balance-column
      সম্পূর্ণ বিচ্ছিন্ন)।

      **কেন এটা একটা নতুন/স্বতন্ত্র Step:** এই একটা repro-তে অন্তত ৩টা স্বতন্ত্র সন্দেহভাজন
      প্রক্রিয়া জড়িত, যেগুলো আগের কোনো Step একসাথে টেস্ট করেনি — (১) একই ডিভাইসে বারবার
      account-switch (logout→login) করলে local (Room) balance/escrow ক্যাশ ঠিকভাবে
      user-scoped ভাবে reload/invalidate হচ্ছে কিনা (Step 12.1/12.11-এ escrow-id
      local-vs-cloud mismatch নিয়ে একটা অসমাপ্ত সন্দেহ ছিল, কিন্তু account-switch নিয়ে
      আলাদাভাবে কখনো investigate করা হয়নি); (২) balance ও escrow-lock — দুটো ভিন্ন
      local table/query-path থেকে আসে কিনা, আর accept_bid/release_escrow RPC-র পরে
      দুটোই সমানভাবে re-sync হচ্ছে কিনা (Step 12.6-এ `depositMoneyViaGateway`-তে
      "device-এ যে session active সেটাতেই outbox replay হয়" জাতীয় session-scoping বাগের
      পরিবারের সাথে সম্পর্কিত হতে পারে, কিন্তু এটা accept_bid/release নিয়ে ভিন্ন কল-সাইট);
      (৩) SOLVER-এর balance column বনাম transaction-row লেখার মধ্যে atomicity/consistency —
      RPC-স্তরে (SQL) নাকি client dual-write-স্তরে (Kotlin) এই ডিসকানেক্ট হচ্ছে সেটা এখনো
      অজানা।

      🔀 **Step 18 চারটা উপ-ধাপে ভাগ করা হয়েছে (18.1 → 18.4), প্রতিটা আলাদা Claude
      সেশনে, এই ক্রমেই।** **GATE:** 18.1–18.4 সবক'টা `[x]` না হওয়া পর্যন্ত এই master
      prompt-এর ধাপ-তালিকা সম্পূর্ণ ধরা হবে না।

  - [x] **Step 18.1 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — সার্ভার-সাইড (SQL) ট্রেস: `accept_bid`/
        `release_escrow`/`refund_escrow_once`-এ balance ও escrow লেখার atomicity, আর SOLVER-এর
        commission-deduction লজিক। ফলাফল: SQL-স্তরে কোনো atomicity gap/ভুল user-scoping/balance-vs-
        transaction ডিসকানেক্ট পাওয়া যায়নি (৩ নম্বর সন্দেহ কার্যকরভাবে বাতিল) — বিস্তারিত progress
        doc-এর "Step 18.1 সম্পূর্ণ" সেকশন।** সার্ভার-সাইড (SQL) ট্রেস: `accept_bid`/`release_escrow`-এ
        balance ও escrow লেখার atomicity, আর SOLVER-এর commission-deduction লজিক।
        rule #5/#5a অনুযায়ী `accept_bid`, `release_escrow`, আর SOLVER-side
        commission-deduction যেখানেই হয় (সম্ভবত `release_escrow`-এর ভেতরেই, বা আলাদা
        RPC) — এই ফাংশনগুলোর সর্বশেষ migration বডি পুরোপুরি পড়ে (case-insensitive
        grep দিয়ে সব সংস্করণ খুঁজে, শুধু সবচেয়ে নতুনটাই সঠিক ধরে) নিশ্চিত করবে: (ক)
        `users.balance`/`balance_user` কলাম UPDATE আর `transactions` টেবিলে INSERT
        একই transaction-এ atomic কিনা (একটা partial-fail হলে অন্যটা রোলব্যাক হয়
        কিনা); (খ) SOLVER-এর জন্য commission-deduction (২০০→১৮০, ১০% ধরে) কোন কলামে
        কীভাবে লেখা হয় — `resolve_commission_rate()`-এর রিটার্ন-ভ্যালু কোথায় ব্যবহার
        হয়, balance-এ কি আদৌ যোগ হয় নাকি শুধু transaction-row-এ amount লেখা হয়ে
        balance-update মিস হয়ে যায় (এটাই সবচেয়ে সন্দেহভাজন কারণ, রিপোর্টে); (গ)
        escrow-এর status/lock কলাম (`HELD`/`RELEASED` জাতীয়) release-এর পরে ঠিকভাবে
        আপডেট হয় কিনা, আর client কোথা থেকে "escrow locked" দেখানোর জন্য read করে
        (`AppDaos.kt`-এর escrow query) সেটা migration-এর সাথে মেলে কিনা। rule #6
        অনুযায়ী কোনো real schema না থাকলে RPC বডি থেকে reverse-engineer করা তথ্যই
        চূড়ান্ত ধরবে। কোনো কোড/migration বদলানো হবে না — শুধু findings।

  - [x] **Step 18.2 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — ক্লায়েন্ট-সাইড (Kotlin) ট্রেস: account-switch cache-scoping। ফলাফল: Room DB query-স্তরে কোনো cross-account leak নেই, কিন্তু `SupabaseRealtimeManager`-এর `user:<uuid>` broadcast subscription (balance/transactions/escrows/withdrawals সবক'টা কভার করে) `logout()`/`completeLoginAfterOtp()`/`switchRoleToSolver()`/`switchRoleToUser()`/`loginAsAdmin()` — কোনো জায়গা থেকেই re-subscribe হয় না, পুরো app-process লাইফটাইমে একবারই (`attachDatabase()`-এর idempotency guard-এর কারণে) সেট হয় — এটাই সবচেয়ে সম্ভাব্য root cause, বিস্তারিত progress doc-এর "Step 18.2 সম্পূর্ণ" সেকশনে।** ক্লায়েন্ট-সাইড (Kotlin) ট্রেস: account-switch (logout/login)
        ফ্লো-তে balance/escrow local cache কীভাবে scope/invalidate হয়।** (১)
        `logout()`/`loginAsUser()`/OTP-login ফাংশনগুলো (Step 16.1-এর inventory-তে
        চিহ্নিত ফাংশন-নামগুলো reuse করবে যেখানে সম্ভব) Room-এর কোন টেবিল/DAO clear বা
        re-fetch করে, আর কোনটা করে না — বিশেষভাবে `users`/balance-সংক্রান্ত টেবিল আর
        escrow টেবিল; (২) একাধিক অ্যাকাউন্ট যদি একই local DB instance শেয়ার করে (single
        Room DB, per-user filter দিয়ে row আলাদা করা, নাকি সত্যিই user-scoped আলাদা
        storage) সেটা নিশ্চিত করবে — যদি single shared DB হয়, তাহলে stale/leftover row
        অন্য অ্যাকাউন্টে দেখা যাওয়ার সবচেয়ে সম্ভাব্য কারণ; (৩) `syncStrictOfflineBlockSettingFromCloud()`-এর
        (Step 16.2) মতো "app-startup/login-এ cloud→local এক-মুখী pull" প্যাটার্ন
        balance/escrow-এর জন্যও আছে কিনা, থাকলে race/order (কোন সিকোয়েন্সে local
        stale value UI-তে দেখা যায় pull সম্পূর্ণ হওয়ার আগে) খুঁজবে। কোনো কোড বদলানো
        হবে না — শুধু findings, 18.1-এর সাথে মিলিয়ে একটা সম্পূর্ণ root-cause hypothesis
        (বা একাধিক স্বতন্ত্র hypothesis, Step 16.2-এর প্যাটার্নে) progress doc-এ লিখবে।

  - [x] **Step 18.3 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — Kotlin static-source-scan regression test
        (`RealtimeSubscriptionScopeTest.kt`, ৪টা `@Test`): মূল বাগ-demonstrating টেস্ট **ইচ্ছাকৃতভাবে
        ৫টা failure দেখানোর কথা** (`logout`/`completeLoginAfterOtp`/`switchRoleToSolver`/
        `switchRoleToUser`/`loginAsAdmin` কেউই `startRealtimeListeners()`/`stopRealtimeListeners()`
        কল করে না — Step 18.2-এর confirmed root cause), sandbox-এ Python পোর্ট দিয়ে static-ভাবে ৫টাই
        fail করা যাচাই করা হয়েছে; বাকি ৩টা drift-guard টেস্ট (idempotent unsubscribe প্যাটার্ন,
        `attachDatabase` singleton-guard, `AppDatabase` single-shared-DB) currently pass — বিস্তারিত
        progress doc-এর "Step 18.3 সম্পূর্ণ" সেকশনে। Windows real-run (Step 12.x/15.x-এর ফরম্যাটে)
        বাকি — কমান্ড progress doc-এ দেওয়া আছে। rule #1 অনুযায়ী কোনো production কোড বদলানো হয়নি,
        শুধু নতুন টেস্ট ফাইল।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত: টেস্ট লেখা: root cause অনুযায়ী pgTAP (SQL atomicity) ও/বা
        Robolectric/JVM (account-switch cache) টেস্ট। 18.1/18.2-এর findings অনুযায়ী
        যে স্তরে (SQL/Kotlin/উভয়) সমস্যা confirmed হয়েছে, সেই স্তরেই regression-টেস্ট
        লিখবে — যেমন SQL-স্তরে হলে `accept_bid`→`release_escrow` পুরো চক্র চালিয়ে
        balance+escrow+transaction তিনটাই expected state-এ আছে কিনা (pgTAP), আর
        Kotlin-স্তরে হলে account-switch simulate করে local cache-এর প্রত্যাশিত
        invalidation/re-fetch হচ্ছে কিনা (Step 16.3-এর static-source-scan প্যাটার্নে,
        যদি real DI/mock সীমাবদ্ধতার কারণে সরাসরি integration test সম্ভব না হয়)। যদি
        18.1/18.2-এ root cause পুরোপুরি নিশ্চিত না হয় (শুধু hypothesis), তাহলে এই ধাপে
        প্রতিটা hypothesis আলাদা করে verify/rule-out করার চেষ্টা করবে টেস্ট দিয়েই,
        অনুমান-নির্ভর না থেকে। rule #1 অনুযায়ী কোনো production কোড এখনও বদলানো হবে
        না — শুধু টেস্ট যোগ হবে (fail করলেও, ঠিক Step 17.2-এর "বাগ থাকলে fail
        দেখানোর কথা" নীতিতে)।

  - [x] **Step 18.4 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — চূড়ান্ত রুট-কজ রিপোর্ট লেখা হয়েছে:
        `SupabaseRealtimeManager`-এর `user:<uuid>` broadcast subscription কোনো account-switch
        lifecycle ফাংশন (`logout`/`completeLoginAfterOtp`/`switchRoleToSolver`/`switchRoleToUser`/
        `loginAsAdmin`) থেকেই re-scope হয় না বলে নিশ্চিত হয়েছে (ফাইল/ফাংশন/লাইন-নম্বরসহ), ন্যূনতম
        প্রস্তাবিত ফিক্স (diff-আকারে) লেখা হয়েছে কিন্তু rule #1 অনুযায়ী প্রয়োগ করা হয়নি,
        `full-test.yml` auto-discovery re-confirm হয়েছে (কোনো এডিট লাগেনি), আর Step 18/সামগ্রিক
        master prompt চূড়ান্ত সারাংশ লেখা হয়েছে — বিস্তারিত progress doc-এর "Step 18.4 সম্পূর্ণ" ও
        "🎉 সম্পূর্ণ CI Test Suite Master Prompt — চূড়ান্ত সারাংশ" সেকশনে। rule #1 অনুযায়ী কোনো
        production কোড এডিট হয়নি।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত: চূড়ান্ত রুট-কজ রিপোর্ট + প্রস্তাবিত ফিক্স (প্রয়োগ করা হবে না,
        rule #1) + `full-test.yml` ওয়্যারিং + Step 18/সামগ্রিক চূড়ান্ত সারাংশ।** (১)
        18.1–18.3-এর findings একত্র করে একটা সম্পূর্ণ, নির্দিষ্ট root-cause বিবরণ
        লিখবে (কোন ফাইল/ফাংশন/লাইন, কেন); (২) সমাধানের জন্য ন্যূনতম প্রস্তাবিত
        ফিক্স লিখবে (SQL হলে `docs/proposed_migrations/PROPOSED_step18_...sql`,
        Kotlin হলে diff-আকারে progress doc-এ — rule #1a-এর মতো ব্যতিক্রম ছাড়া
        এখানে production কোড এডিট করার অনুমতি নেই, তাই শুধু প্রস্তাব); (৩) নতুন
        টেস্ট ফাইল(গুলো) `full-test.yml`-এ auto-discover হচ্ছে কিনা নিশ্চিত করবে;
        (৪) progress doc-এ Step 18-এর, এবং GATE অনুযায়ী পুরো master prompt আবার
        সম্পূর্ণ হওয়ার (বা যদি ব্যবহারকারী ফিক্স apply করতে চান, Step 12.x-এর
        মতো একটা নতুন 18.x ট্র্যাকার খোলা হতে পারে কিনা সেই সুপারিশসহ) চূড়ান্ত
        সারাংশ লিখবে।

      **GATE অনুযায়ী Step 18 সম্পূর্ণরূপে বন্ধ, Step 19 এখন খোলা।**

---

- [x] **Step 19 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, GATE: 19.1–19.6 সব `[x]`) — Admin panel: overview
      metrics realtime-desync + admin action buttons execute/reflect না করা। Root cause উভয়
      bug-class-এর জন্যই নিশ্চিত (ফাইল/ফাংশন/লাইনসহ), regression test লেখা হয়েছে (আংশিক
      ইচ্ছাকৃতভাবে fail করছে), প্রস্তাবিত ফিক্স লেখা হয়েছে (প্রয়োগ করা হয়নি) — বিস্তারিত progress
      doc-এর "GATE অনুযায়ী Step 19 সম্পূর্ণ" সেকশনে।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত (ব্যবহারকারীর রিপোর্ট করা নতুন বাগ, ২০২৬-০৯-২৩ চ্যাট-সেশনে যোগ
      করা হলো):

      **রিপোর্ট-করা সমস্যা (ব্যবহারকারীর ভাষায়, সংক্ষেপে):**
      1. Admin panel-এর **Overview** page-এ যত ধরনের metric দেখানো হয়, সবগুলোই realtime-এ সঠিক
         ডেটা দিয়ে update হচ্ছে না।
      2. Users/Withdrawals ও এই ধরনের অন্যান্য menu-তে থাকা **অনেক admin action button** কাজ
         করছে না — কিছু কিছু হয়তো করে, কিন্তু অনেকগুলোই action execute-ই হচ্ছে না। উদাহরণ (হুবহু
         ব্যবহারকারীর ভাষায়): "withdrawal admin approve বা reject করতে পারছে না, action করলে
         কিছুই হচ্ছে না।" এছাড়াও, যেসব action সত্যিই execute হয় (backend-এ লেখা সফল হয়), সেগুলোও
         **realtime-এ same page-এ প্রতিফলিত হচ্ছে না** — কোনো reload ছাড়া update হওয়ার কথা,
         কিন্তু হচ্ছে না। ব্যবহারকারী স্পষ্ট করে বলেছেন এটা শুধু withdrawal-এর উদাহরণ — **অন্য আরও
         অনেক admin action-এও একই ধরনের সমস্যা থাকতে পারে**, তাই এই ধাপে withdrawal-এ সীমাবদ্ধ না
         থেকে পুরো admin panel-এর প্রতিটা action-button ও প্রতিটা overview-metric আলাদাভাবে
         inventory করে validate করতে হবে।

      **কেন এটা একটা নতুন/স্বতন্ত্র Step:** Step 18 account-switch-কেন্দ্রিক realtime-desync
      (normal USER/SOLVER সেশনে) নিয়ে ছিল, এই বাগটা **admin session-কেন্দ্রিক** এবং স্কোপ ভিন্ন —
      (ক) admin overview-এর aggregate metric query/RPC-গুলো (যেগুলো আগের কোনো Step টেস্ট করেনি —
      Step 7 admin moderation & balance-এ individual moderation RPC টেস্ট হয়েছিল, কিন্তু
      dashboard-level aggregate metric আলাদা জিনিস); (খ) admin action button click থেকে RPC call
      পর্যন্ত পুরো chain — অনেক button-এর ক্ষেত্রে **action-ই সিলেন্টলি না-execute হওয়া**, যেটা
      realtime সমস্যার চেয়ে আরও মৌলিক (ডেটা লেখাই হচ্ছে না, শুধু UI দেখাচ্ছে না তা না); (গ) যেসব
      action সত্যিই execute হয়, তাদের realtime reflection — এটা সম্ভবত Step 18.2-এ পাওয়া
      `SupabaseRealtimeManager`-এর ফাইন্ডিং-এর সাথে সম্পর্কিত হতে পারে: `startRealtimeListeners()`
      (লাইন ১৭৫৯-১৭৬৮)-এ per-user broadcast subscription (`startUserTopicBroadcastSubscription`)
      **admin session-এর জন্য ইচ্ছাকৃতভাবে চালু হয় না** (`isCurrentSessionAdmin()` চেক, লাইন
      ১৫৪৯-১৫৫৩, কমেন্ট "Admin session-এ নতুন পথ চালু হয় না") — কিন্তু এটা শুধু notifications-এর
      per-user broadcast channel-এর কথা বলে, table-backed channel-গুলো (`realtime-manager-users`,
      `realtime-manager-withdrawals` ইত্যাদি, লাইন ১৬৭৯-১৭৫৭) role-নির্বিশেষে সবসময় subscribe হয়
      বলে code-এ মনে হয় — **এটা নিশ্চিত না, একটা প্রাথমিক সন্দেহ মাত্র, 19.3/19.5-এ সরাসরি কোড
      পড়ে যাচাই করতে হবে, অনুমান করে ধরে নেওয়া যাবে না (rule #5)।**

      🔀 **Step 19 ছয়টা উপ-ধাপে ভাগ করা হয়েছে (19.1 → 19.6), প্রতিটা আলাদা Claude সেশনে, এই
      ক্রমেই।** **GATE:** 19.1–19.6 সবক'টা `[x]` না হওয়া পর্যন্ত এই master prompt-এর ধাপ-তালিকা
      সম্পূর্ণ ধরা হবে না।

  - [x] **Step 19.1 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — সম্পূর্ণ admin-panel inventory (কোনো কোড বদলানো হবে না, শুধু ইনভেন্টরি +
        প্রাথমিক সন্দেহ-চিহ্নিতকরণ)।** (Step 16.1-এর "gating-logic inventory" প্যাটার্নে) admin
        panel-এর সব Composable screen (`AdminOverviewView`/`AdminDashboard` জাতীয়,
        `AdminUsersView.kt`, `AdminWithdrawalsView.kt` ইত্যাদি — `app/src/main/java/com/example/ui/`-এ
        `grep -il "admin"` দিয়ে case-insensitive খুঁজে বের করবে, ধরে নেবে না নাম আগে থেকে জানা) থেকে
        দুটো তালিকা বানাবে, দুটোই markdown table আকারে এই ধাপেই progress doc-এ যোগ হবে:
        (ক) **Overview metrics তালিকা** — প্রতিটা metric-এর নাম, কোন Composable/ViewModel state
        থেকে আসে, সেই state কোন ফাংশন populate করে (one-shot pull নাকি Room `Flow`-backed reactive),
        আর সেই ফাংশন cloud থেকে কোন RPC/query কল করে;
        (খ) **Admin action button তালিকা** — প্রতিটা button/menu-item-এর নাম (approve/reject/ban/
        KYC-verify/role-change/balance-adjust ইত্যাদি যা কিছু পাওয়া যায়, শুধু withdrawal না), কোন
        `onClick`/ViewModel ফাংশন কল হয়, সেই ফাংশন কোন RPC কল করে, আর কোড পড়ে **প্রাথমিকভাবে**
        সন্দেহজনক মনে হলে (যেমন: `onClick` খালি/TODO, ফাংশন cloud RPC-ই কল করে না শুধু local state
        বদলায়, `try/catch`-এ silent swallow আছে, বা RPC নাম নিজেই migration-এ নেই) সেটা নোট করবে —
        কিন্তু **এই ধাপে কোনো চূড়ান্ত সিদ্ধান্ত না**, শুধু পরের ধাপগুলোর (19.2–19.5) জন্য একটা
        গাইডেড ম্যাপ। rule #5/#5a অনুযায়ী সব scan case-insensitive হবে। withdrawal
        approve/reject-কে (ব্যবহারকারীর দেওয়া concrete example) সবচেয়ে আগে, সবচেয়ে বিস্তারিতভাবে
        ট্রেস করবে যাতে 19.4/19.5-এর জন্য একটা confirmed starting point থাকে।

  - [x] **Step 19.2 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — Overview metrics: SQL/query-স্তর ট্রেস (কোনো কোড বদলানো হবে না, শুধু
        findings)।** 19.1-এর তালিকা অনুযায়ী প্রতিটা overview-metric যে RPC/query থেকে আসে তার
        সর্বশেষ migration বডি (rule #5/#5a: case-insensitive grep দিয়ে সব সংস্করণ খুঁজে শুধু
        সবচেয়ে নতুনটাই সঠিক ধরে) পুরোপুরি পড়ে যাচাই করবে: (ক) হিসাবের লজিক সঠিক কিনা (যেমন
        pending-withdrawal-count আসলেই status = 'PENDING' ফিল্টার করে কিনা, total-balance
        সব ইউজারের balance যোগ করে কিনা ইত্যাদি — যা যা metric পাওয়া যায় তার জন্য); (খ) কোনো
        metric যদি client-side-এ Kotlin দিয়ে aggregate/compute হয় (RPC না, raw row টেনে local
        loop-এ যোগফল), সেটাও আলাদা করে চিহ্নিত করবে, কারণ ভুল সেখানে হলে SQL-স্তরে কোনো বাগ
        থাকবে না, এটা 19.3-এর স্কোপ হবে। rule #6 অনুযায়ী কোনো real schema না থাকলে RPC বডি থেকেই
        reverse-engineer করা তথ্য চূড়ান্ত ধরবে।

  - [x] **Step 19.3 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — Overview metrics: Kotlin-স্তর ট্রেস — কেন realtime-এ update হচ্ছে না
        (কোনো কোড বদলানো হবে না, শুধু findings)।** প্রতিটা overview-metric-এর জন্য: (ক) এটা
        Room `Flow`-backed reactive নাকি এক-বারের (`viewModelScope.launch` + suspend fun) pull —
        যদি এক-বারের pull হয় আর কোনো periodic/realtime-triggered re-pull না থাকে, সেটাই মূল কারণ
        হতে পারে (কোনো বাগ ছাড়াই "realtime না" — ডিজাইন-গ্যাপ); (খ) যদি সত্যিই realtime-backed
        দাবি করা হয়ে থাকে (কোনো comment/নামকরণে), তাহলে সেটা কোন `SupabaseRealtimeManager` channel/
        handler-এর উপর নির্ভর করে, আর সেই channel/handler admin session-এর জন্য আদৌ সক্রিয় থাকে
        কিনা — উপরের "কেন এটা নতুন Step" সেকশনে উল্লেখ করা `isCurrentSessionAdmin()` সন্দেহটা এখানেই
        নিশ্চিত/বাতিল করবে (সরাসরি কোড পড়ে, অনুমান না); (গ) admin panel প্রতিটা screen-এ ঢোকার সময়
        কোনো explicit "refresh"/pull কল হয় কিনা যেটা আড়ালে সমস্যা মাস্ক করে (manual reload করলে ঠিক
        দেখায় কেন, এটাই তার কারণ হতে পারে)।

  - [x] **Step 19.4 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — Admin action buttons: কেন execute হচ্ছে না (কোনো কোড বদলানো হবে না, শুধু
        findings)।** 19.1-এ চিহ্নিত সন্দেহজনক action-গুলো (withdrawal approve/reject আগে, বিস্তারিত)
        এক-এক করে ট্রেস করবে: (ক) `onClick` handler আদৌ ViewModel ফাংশন কল করে কিনা (UI-স্তরেই যদি
        wiring miss থাকে সেটাই root cause, কোনো backend সমস্যা ছাড়াই); (খ) ViewModel ফাংশন RPC কল
        করে কিনা, নাকি শুধু local state/optimistic-UI বদলে থেমে যায়; (গ) RPC কল আসলে হলে
        `try/catch`/`runCatching`-এ error silent swallow হচ্ছে কিনা (Log হয় কিন্তু user-facing কোনো
        toast/error না থাকলে ব্যবহারকারীর কাছে "কিছুই হয়নি" মনে হবে, যদিও আসলে ব্যর্থ হয়েছে —
        এটা "execute-ই হচ্ছে না" আর "execute হয় কিন্তু silently fail করে" দুটো আলাদা কিন্তু একই
        উপসর্গ তৈরি করা কারণ, দুটোই আলাদা করে নোট করবে); (ঘ) admin-এর auth context দিয়ে RPC কল করলে
        RLS/permission-check (`is_admin()` জাতীয় helper, migration বডিতে) fail করছে কিনা — rule #6
        অনুযায়ী real helper না থাকলে RPC বডি থেকে reverse-engineer করা লজিক ব্যবহার করবে। প্রতিটা
        action-এর জন্য একটা clear conclusion লিখবে: "UI-wiring গ্যাপ" / "RPC call হয় না" / "RPC কল
        হয় কিন্তু silent-fail" / "RLS/permission block" / "আসলে ঠিক আছে, বাগ পাওয়া যায়নি" — এর
        মধ্যে যেটা প্রযোজ্য। যদি action-গুলোর সংখ্যা/বৈচিত্র্য বেশি হয় (একাধিক স্বতন্ত্র root-cause
        প্যাটার্ন দেখা যায়), Step 15.3a/16.3a-এর প্যাটার্নে dynamic উপ-উপ-ধাপ (19.4a, 19.4b...)
        এই ধাপকেই ভাগ করে নেবে, প্রতিটা আলাদা সেশনে।

  - [x] **Step 19.5 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন) — Admin action buttons: যেগুলো execute হয়, সেগুলো কেন realtime-এ same-page-এ
        প্রতিফলিত হয় না (কোনো কোড বদলানো হবে না, শুধু findings)।** 19.4-এ "RPC কল হয়" হিসেবে
        চিহ্নিত প্রতিটা action-এর জন্য: সেই action-এর টেবিলের (withdrawals/users/ইত্যাদি) জন্য
        admin screen-এর UI Room `Flow` দিয়ে reactive কিনা (কমেন্টে যেমন দাবি করা আছে
        `SupabaseRealtimeManager.kt`-এর withdrawals-সেকশনে, "কোনো নতুন UI কোড লাগেনি" — এই দাবি
        সত্যিই সঠিক কিনা কোড পড়ে verify করবে); যদি সত্যিই Room Flow-backed হয়, তাহলে সমস্যা এটাই
        হতে পারে যে সেই টেবিলের realtime channel/broadcast admin session-এর জন্য subscribe-ই হয়নি
        (19.3-এর ফাইন্ডিং-এর সাথে ক্রস-রেফারেন্স করবে — root cause একই হতে পারে overview-metrics ও
        action-reflection দুটোর জন্যই)। প্রতিটা distinct root-cause hypothesis (rule অনুযায়ী,
        নিশ্চিতভাবে না — যদি একাধিক independent কারণ পাওয়া যায় Step 16.2-এর প্যাটার্নে আলাদা করে
        লিখবে) progress doc-এ লিখবে।

  - [x] **Step 19.6 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩, সেশন, GATE: 19.1–19.6 সব `[x]`) — regression test
        (`AdminPanelStep19RegressionTest.kt`, ১০টা `@Test`), চূড়ান্ত রুট-কজ রিপোর্ট (উভয় বাগ-ক্লাস),
        প্রস্তাবিত ফিক্স (diff-আকারে, প্রয়োগ করা হয়নি) — বিস্তারিত progress doc-এর "Step 19.6
        সম্পূর্ণ" ও "GATE অনুযায়ী Step 19 সম্পূর্ণ" সেকশনে।** পুরনো বিবরণ ইতিহাসের জন্য অপরিবর্তিত: regression test(s) + চূড়ান্ত রুট-কজ রিপোর্ট + প্রস্তাবিত ফিক্স (প্রয়োগ করা
        হবে না, rule #1) + `full-test.yml` ওয়্যারিং + Step 19/সামগ্রিক চূড়ান্ত সারাংশ।** (১)
        19.2–19.5-এ যে স্তরে (SQL/Kotlin) root cause confirmed হয়েছে সেই স্তরে regression test
        লিখবে (Step 18.3-এর প্যাটার্নে — SQL হলে pgTAP, Kotlin-এ real DI/mock সীমাবদ্ধতা থাকলে
        static-source-scan; বাগ থাকলে টেস্ট ইচ্ছাকৃতভাবে fail করবে, Step 17.2-এর নীতিতে); (২)
        19.2–19.5-এর findings একত্র করে overview-metrics ও action-buttons দুটো বাগ-ক্লাসের জন্যই
        আলাদা আলাদা সম্পূর্ণ, নির্দিষ্ট root-cause বিবরণ লিখবে (কোন ফাইল/ফাংশন/লাইন, কেন); (৩)
        সমাধানের জন্য ন্যূনতম প্রস্তাবিত ফিক্স লিখবে (SQL হলে
        `docs/proposed_migrations/PROPOSED_step19_...sql`, Kotlin হলে diff-আকারে progress
        doc-এ — rule #1a-এর মতো ব্যতিক্রম ছাড়া এখানে production কোড এডিট করার অনুমতি নেই, তাই শুধু
        প্রস্তাব); (৪) নতুন টেস্ট ফাইল(গুলো) `full-test.yml`-এ auto-discover হচ্ছে কিনা নিশ্চিত
        করবে; (৫) progress doc-এ Step 19-এর, এবং GATE অনুযায়ী পুরো master prompt আবার সম্পূর্ণ
        হওয়ার (বা ব্যবহারকারী ফিক্স apply করতে চাইলে Step 12.x-এর মতো একটা নতুন 19.x ট্র্যাকার
        খোলা হতে পারে কিনা সেই সুপারিশসহ) চূড়ান্ত সারাংশ লিখবে।

## Step 20 — NON-MONEY call-site audit ও প্রয়োজনীয় fix (Step 12-এর বাকি ~১০২টা)

**পটভূমি:** Step 12 PART 1-এ ~১০২টা call-site "⚪ NON-MONEY" ধরা হয়েছিল **নাম/মন্তব্য দেখে**, RPC
migration বডি পড়ে না (rule ৫-এর সময়-সীমাবদ্ধতায়)। ২০২৬-০৯-২০-এ (Step 12.1 সেশনে) ব্যবহারকারীকে
স্পষ্ট বলা হয়েছিল Step 12-এর সব টাকা-সংক্রান্ত ধাপ শেষ হলে এই ১০২টা নিয়ে আলোচনা হবে — সেই শর্তটা
একটা মাঝের সেশনে (ফাইল rewrite-এর সময়) **হারিয়ে গিয়েছিল**, তাই স্বয়ংক্রিয়ভাবে কখনো জিজ্ঞেস করা
হয়নি। ব্যবহারকারী এখন (Step 19 সম্পূর্ণ হওয়ার পর) স্পষ্টভাবে এই অডিট শুরু করতে বলেছেন। **এই Step 20
এখন থেকে একমাত্র সূত্র — কোনো পুরনো "জিজ্ঞেস করবে" শর্তের ওপর নির্ভর করবে না।**

⚠️ **নতুন সেশনের জন্য সতর্কতা:** Step 12.3–19-এ `SomadhanRepository.kt`-এ অনেক লাইন যোগ/বদল হয়েছে,
তাই PART 1-এ লেখা লাইন-নাম্বারগুলো এখন **অবিশ্বস্ত** — Step 20.1-এ ফাংশনের নাম দিয়ে আবার খুঁজে
current লাইন-নাম্বার বসাতে হবে (Step 12.2-এর name-based lookup পদ্ধতি পুনর্ব্যবহার করা যেতে পারে)।

**কাঠামো:** Step 12-এর মতোই উপ-ধাপ, একটা সেশন = একটা উপ-ধাপ। ২০.২ ও ২০.৪-এর ভেতরের ব্যাচ-সংখ্যা
স্থির না — Step 20.1/20.3-এ ঠিক হবে, তখন progress doc-এর ট্র্যাকারে ধাপ যোগ হবে (`20.2a`, `20.2b`, …
প্যাটার্নে, Step 16/19-এর dynamic উপ-উপ-ধাপের মতো)।

**🚧 GATE:** নিচের সবক'টা `[x]` না হওয়া পর্যন্ত Step 20 "সম্পূর্ণ" ধরা হবে না। এই Step-এর পরে এখনো
কোনো Step 21 সংজ্ঞায়িত নেই — তাই এই GATE আপাতত কিছু আটকায় না, কিন্তু ভবিষ্যতে নতুন Step যোগ হলে
Step 12.x-এর প্যাটার্নেই এটাও GATE হিসেবে গণ্য হবে।

- [x] **Step 20.1 — Inventory ও ব্যাচ-ভাগ (production কোড বদলাবে না)।** ✅ সম্পূর্ণ (২০২৬-০৯-২৩, static) —
      ১০৩টা কল-সাইট, ৬টা ব্যাচে (A–F) ভাগ করা হয়েছে। বিস্তারিত টেবিল progress doc-এর "Step 20.1" সেকশনে।
      `CI_TEST_SUITE_PROGRESS.md`-এর Step 12 PART 1-এর "⚪ NON-MONEY" তালিকা (আনুমানিক ১০২টা call-site,
      "`createNotification` (২৫টা কল-সাইট)…" দিয়ে শুরু হওয়া অনুচ্ছেদ খুঁজবে) আর Step 12.1-এর Borderline
      NON-MONEY ৬টা সাইট (`ownerResetOrphanedAcceptedBid`, 48-ঘণ্টার notify ×২, `submitKyc`,
      `adminUpdateKycInfo`, `adminResetKycToPending`) — দুটো মিলিয়ে সম্পূর্ণ তালিকা বানাবে। প্রতিটার জন্য:
      (ক) `SomadhanRepository.kt`-এ ফাংশনের নাম দিয়ে খুঁজে **বর্তমান** লাইন-নাম্বার বসাবে (rule 5a
      case-insensitive); (খ) কোনো ফাংশন এই সেশন পর্যন্ত (Step 15/16/18/19-এ) rename/মোছা হয়ে থাকলে
      নোট করবে; (গ) rough ঝুঁকি-সংকেত দেবে শুধু নাম/আশেপাশের কোড দেখে (RPC বডি এখনো পড়বে না) — বিশেষভাবে
      যেগুলো কোনো entity-র **status/state** বদলায় (নিছক notification/log/read-flag না) সেগুলো চিহ্নিত
      করবে, কারণ এই ধরনেরগুলোই আগে (12.1-এ) সত্যিকারের money-critical বেরিয়েছিল। তালিকাকে **৮–১০টা করে
      ব্যাচে** ভাগ করবে, ঝুঁকিপূর্ণ (state-বদলানো) ব্যাচগুলো আগে রেখে। progress doc-এ একটা টেবিল লিখবে:
      ব্যাচ-নং | সাইটের নাম | ফাংশন | বর্তমান লাইন | rough ঝুঁকি-সংকেত। master prompt-এ (এই সেশনেই)
      নিচের placeholder-এর জায়গায় আসল ব্যাচ-সংখ্যা বসিয়ে `Step 20.2a ... Step 20.2<শেষ অক্ষর>` সারি
      যোগ করবে (নিচের নির্দেশনা অনুযায়ী), যাতে GATE-কাঠামো ঠিকঠাক লেখা থাকে।

      প্রতিটা ব্যাচ-অডিটের কাজ (Step 12.1-এর প্যাটার্নে): সেই ব্যাচের প্রতিটা সাইটের migration RPC বডি
      (`grep -i`) পড়ে **লাইন-উদ্ধৃতিসহ** 🔴 MONEY-CRITICAL / 🟠 MONEY-ADJACENT / ⚪ সত্যিই NON-MONEY-তে
      ভাগ করা; local-only (RPC-বিহীন) সাইট হলে সেটাও লিখবে (retry অপ্রাসঙ্গিক); ইতিমধ্যে-নিশ্চিত
      রেফারেন্স-সাইট (🔵 চিহ্নিত) পুনরায় audit করবে না। কোনো কোড/টেস্ট ফাইল বদলাবে না, শুধু progress
      doc-এর ব্যাচ-টেবিলে প্রতিটা row-এ ফলাফল-কলাম যোগ। প্রতিটা ব্যাচ = একটা সেশন।

      - [x] **Step 20.2a — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সবগুলোই ⚪ NON-MONEY নিশ্চিত (বিস্তারিত progress doc-এর
      "Step 20.2a" সেকশনে; গুরুত্বপূর্ণ ফাইন্ডিং: `adminReassignSolver` নিরাপদ কারণ payout escrow-র
      নিজস্ব `solver_id` ব্যবহার করে, `problems.accepted_solver_id` না)।** পুরনো বর্ণনা: অডিট ব্যাচ A (Problem/User state-mutation, ৯টা: adminUpdateProblemStatus,
            adminUpdateProblemBudget, adminReassignSolver, adminRejectBid, deleteProblem→adminSoftDeleteProblem,
            declineDirectContractProposal, userDeleteProblem, deleteUser→adminSoftDeleteUser,
            adminResetUserPassword)।**
      - [x] **Step 20.2b — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সবগুলোই ⚪ NON-MONEY নিশ্চিত (বিস্তারিত progress doc-এর
      "Step 20.2b" সেকশনে; মূল প্যাটার্ন: সব সাইটই flag/counter/timestamp-কে নির্দিষ্ট মানে সেট করে
      (increment না) — স্বাভাবিকভাবেই retry-নিরাপদ; `admin_wipe_all_data` destructive হলেও সব money-table
      সম্পূর্ণ খালি করে, আংশিক/ভুল-দিকের নড়াচড়া না)।** পুরনো বর্ণনা: অডিট ব্যাচ B (Destructive/bulk admin + KYC, ১০টা: clearAllDatabaseAndReset,
            maybeAutoReconcileBalances/updatePlatformSetting→upsertPlatformSetting, applyReputationChange→
            submitReputationEvent, runMonthlyFreeQuotaReset, adminResetSolverFreeQuota,
            adminResetSolverMissCycle, submitKyc, adminUpdateKycInfo, adminResetKycToPending — শেষ ৩টা
            Step 12.1-এ ইতিমধ্যে NON-MONEY নিশ্চিত, শুধু নিশ্চিতকরণ-নোট লিখবে, বডি আবার পড়বে না)।**
      - [x] **Step 20.2c — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সবগুলোই ⚪ NON-MONEY নিশ্চিত (বিস্তারিত progress doc-এর
      "Step 20.2c" সেকশনে; escrow `accept_bid` RPC-এর ভেতরে তৈরি হয় (আলাদা, ইতিমধ্যে retry-protected) —
      এই ১০টা instant-job-lifecycle সাইট শুধু status/timestamp/GPS, কোনো money-table ছোঁয় না; scope-বহির্ভূত
      পর্যবেক্ষণ: `admin_force_cancel_instant_job` নিজে escrow refund করে না কিন্তু RPC-ই money-table লেখে না
      বলে retry-gap নতুন ঝুঁকি তৈরি করে না)।** পুরনো বর্ণনা: অডিট ব্যাচ C (Instant-job lifecycle, ১০টা: createInstantJob→broadcastInstantJob,
            acceptInstantJobBid→updateProblemFull, markSolverOnWay, markSolverArrived, markJobStarted,
            cancelInstantJob, updateSolverLiveLocation, adminForceCancelInstantJob,
            checkAndExpireInstantJobs→expireBroadcastingInstantJob, updateInstantJobToggle→
            syncInstantJobNotificationToggle)।**
      - [x] **Step 20.2d — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সবগুলোই ⚪ NON-MONEY নিশ্চিত (বিস্তারিত progress doc-এর
      "Step 20.2d" সেকশনে; বিশেষ নোট: `syncAllLocalToSupabase`-এর bootstrap-sync (`createProblem`/`createBid`)
      নিজেই একটা স্ব-ডিজাইনকৃত idempotent retry-loop — duplicate-key ব্যর্থতা ইচ্ছাকৃতভাবে নিরাপদ ধরা হয়,
      তাই ভবিষ্যতে সাধারণ `enqueueOutboxRetry` প্যাটার্ন যোগ করার আগে এই পার্থক্য বিবেচনা করতে হবে)।** পুরনো বর্ণনা: অডিট ব্যাচ D (Category/FAQ/profile/sync-bootstrap, ১০টা: updateOwnProfile
            (৫টা কল-সাইট একসাথে), upsertCategory ×২, setCategoryActive, deleteCategoryRemote,
            adminRemoveCategoryFromSolvers, upsertFaq ×২, deleteFaqRow ×২, createProblem/createBid
            (sync-bootstrap, `syncAllLocalToSupabase`-এর ভেতরে — retry-নকশা অন্য সাইটের চেয়ে ভিন্ন
            হতে পারে তা নোট করবে), ownerResetOrphanedAcceptedBid — শেষটা Step 12.1-এ ইতিমধ্যে NON-MONEY
            নিশ্চিত, শুধু নিশ্চিতকরণ-নোট)।**
      - [x] **Step 20.2e — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সবগুলোই ⚪ NON-MONEY নিশ্চিত (বিস্তারিত progress doc-এর
      "Step 20.2e" সেকশনে; গুরুত্বপূর্ণ ক্রস-চেক: `adminCancelAndRefundDirectContract`-এর ভেতরের
      `adminSendMessageToProblemChat` কল সত্যিই স্বাধীন non-money সাইড-এফেক্ট, সেই ফাংশনের money-critical
      অংশ থেকে আলাদা; backlog নোট: `submit_rating`-এ one-time-per-problem গার্ড নেই কিন্তু money-ঝুঁকি না)।** পুরনো বর্ণনা: অডিট ব্যাচ E (Notification/message/rating, ১০টা: createNotification-এর ২৬টা
            কল-সাইট (এক অডিটে, প্যাটার্ন একই কিনা যাচাই), markAllNotificationsAsRead,
            markNotificationAsRead, adminDeleteNotificationGroup, adminBroadcastNotification→
            sendManualNotification, logAdminAction, adminSendMessageToProblemChat (৩টা কল-সাইট, বিশেষ
            করে `adminCancelAndRefundDirectContract`-এর ভেতরেরটা — সেই ফাংশন আগে থেকেই MONEY-CRITICAL
            fix হয়ে গেছে, তাই এই নির্দিষ্ট কলটা তার বাইরের আলাদা non-money সাইড-এফেক্ট কিনা স্পষ্ট করে
            লিখবে), adminDeleteMessage, adminDeleteRating, submitRating ×২)।**
      - [x] **Step 20.2f — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সবগুলোই ⚪ NON-MONEY নিশ্চিত (বিস্তারিত progress doc-এর
      "Step 20.2f" সেকশনে; স্কোপ-প্রশ্ন ১: `sendSystemEventMessage`→`SupabaseSyncManager.systemEventMessage`→
      `system_event_message` RPC, ব্যাচে যোগ করে অডিট করা হয়েছে; স্কোপ-প্রশ্ন ২: `setTypingStatus`/৪টা
      broadcast join-leave নিশ্চিতভাবে সরাসরি `SupabaseRealtimeManager` ephemeral channel broadcast/
      subscribe (কোনো DB write নেই) — outbox-retry স্কোপের সম্পূর্ণ বাইরে, চূড়ান্তভাবে বন্ধ; এছাড়া
      `getPlatformSetting`/`getAllAdditionalCharges` আসলে read-only cloud **pull**, push না — সেই কারণেও
      retry-স্কোপের বাইরে)। GATE অনুযায়ী 20.2a–20.2f সব `[x]` — Step 20.2 সম্পূর্ণ।** পুরনো বর্ণনা: অডিট ব্যাচ F (Seen-flag/misc + ২টা স্কোপ-প্রশ্নের সমাধান, ৯টা: getPlatformSetting,
            getAllAdditionalCharges, markDisputeResultSeen, markCompletionResultSeen, markProblemSeen,
            markMessagesAsReadForProblem, requestAdminAssistance/notifyAdmins, clearSolverCancelledNotice)
            — প্লাস দুটো স্কোপ-প্রশ্ন প্রথমে মীমাংসা করবে: (১) `sendSystemEventMessage` এখন আসলে কোন
            wrapper/নামে কল হয় তা `grep -i "systemEventMessage\|SystemEvent"` দিয়ে পুনরায় বের করে অডিট
            তালিকায় যোগ করবে; (২) `setTypingStatus`/৪টা broadcast join-leave ফাংশন সরাসরি
            `SupabaseRealtimeManager` channel broadcast (persisted DB state না) কিনা কোড পড়ে নিশ্চিত করে
            লিখবে এগুলো `enqueueOutboxRetry` প্যাটার্নের আওতার বাইরে কিনা — বাইরে হলে ⚪ NON-MONEY
            (out-of-scope, ephemeral) হিসেবে চূড়ান্তভাবে বন্ধ করবে, ভেতরে হলে তখনই অডিট করবে।**

- [x] **Step 20.3 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — সব ব্যাচ একত্র: ~১০৩টা কল-সাইটের একটাও 🔴 বা 🟠 না,
      সবগুলোই ⚪ সত্যিই NON-MONEY প্রমাণিত (বিস্তারিত progress doc-এর "Step 20.3" সেকশনে)। তাই কোনো
      🟠-সিদ্ধান্ত-প্রশ্ন ব্যবহারকারীকে জিজ্ঞাসা করার দরকার হয়নি, আর চূড়ান্ত fix-তালিকা খালি (০+০) —
      নিচের Step 20.4 প্লেসহোল্ডারে কোনো ব্যাচ যোগ হয়নি (প্রয়োজন নেই)। GATE ভ্যাকুয়াসলি satisfied —
      পরের ধাপ সরাসরি Step 20.5।** পুরনো নির্দেশনা (ইতিহাসের জন্য অপরিবর্তিত): 🚧 GATE: 20.2a–20.2f সবক'টা `[x]` হতে হবে। কাজ: (১) সব ব্যাচের ফল একত্র করে চূড়ান্ত তালিকা —
      কতটা 🔴, কতটা 🟠, কতটা সত্যিই ⚪; (২) 🔴-গুলোর জন্য rule #1a অনুযায়ী প্রতিটার idempotency
      migration বডি থেকে যাচাই (BLOCKED হলে কারণসহ আলাদা); (৩) 🟠 (MONEY-ADJACENT) প্রতিটার জন্য
      Step 12.1-এর মতো যুক্তি লিখে ব্যবহারকারীর কাছে অন্তর্ভুক্তির সিদ্ধান্ত চাইবে (একসাথে, ছোট প্রশ্ন
      তালিকা — বারবার জিজ্ঞেস না করে); (৪) চূড়ান্ত fix-তালিকা (🔴 সব + ব্যবহারকারী-অনুমোদিত 🟠) কে
      ৫–৮টা করে fix-ব্যাচে ভাগ করবে, **[PLACEHOLDER]**-এ `Step 20.4a, 20.4b, …` সারি একইভাবে বসাবে
      (12.3–12.7-এর প্যাটার্নে, প্রতিটায় প্রত্যাশিত pass-সংখ্যা সহ)। ব্যবহারকারীর উত্তর না পাওয়া পর্যন্ত
      20.4-এর কোনো ব্যাচ শুরু হবে না — উত্তর এলে সেটা পরের সেশনের শুরুতে প্রসেস হবে (12.x-এর
      `WINDOWS RESULT`-এর মতোই, কিন্তু এটা "USER DECISION:" হিসেবে)।

      **[PLACEHOLDER — Step 20.3 সম্পূর্ণ হয়েছে, ফলাফল: ০টা fix-ব্যাচ লাগবে না (কোনো 🔴/🟠 পাওয়া
      যায়নি) — তাই এখানে কোনো `Step 20.4a/b/...` সারি যোগ করা হয়নি। Step 20.4 সম্পূর্ণভাবে স্কিপড।]**
      মূল প্যাটার্ন (যদি ভবিষ্যতে কখনো নতুন 🔴/🟠 পাওয়া যায় — বর্তমানে প্রযোজ্য না):
      `- [ ] **Step 20.4<অক্ষর> — fix ব্যাচ <N> (<সাইটগুলোর নাম>)।**` প্রতিটার কাজ: rule #1a-এর
      ব্যতিক্রম প্রয়োগ করে `SomadhanRepository.kt`-এর `.onFailure {}` ব্লকে `enqueueOutboxRetry(...)`
      (12.3-এর "fix recipe" অনুসরণ করবে) + `OutboxRpcDispatcher.kt`-এ branch + Windows-verification
      কমান্ড (12.x-এর টেমপ্লেট, শুধু `--tests` আর্গুমেন্টে উপযুক্ত টেস্ট ক্লাসের নাম বসাবে — এই নতুন
      সাইটগুলোর জন্য কোনো টেস্ট ক্লাস আগে থেকে নেই, তাই প্রথম fix-ব্যাচেই
      `app/src/test/java/com/example/repository/DualWriteGapTestNonMoney.kt` বানাবে, `DualWriteGapTest.kt`
      (Step 12.2-পরবর্তী, নাম-ভিত্তিক সাইট-খোঁজা) সংস্করণের হুবহু কাঠামো কপি করে)। প্রতিটা ব্যাচ = একটা সেশন।

- [x] **Step 20.5 — ✅ সম্পূর্ণ (২০২৬-০৯-২৩) — WINDOWS RESULT প্রসেস করা হয়েছে।** পুরো
      `:app:testDebugUnitTest` রান হয়েছে (SDK-path সেটআপের পর) — ৩টা ক্লাসে ৯টা ফেইলিউর
      (`AdminPanelStep19RegressionTest` ৫/৮, `RealtimeSubscriptionScopeTest` ১/৪,
      `ExampleRobolectricTest` ৩/১৬) কিন্তু **সবক'টাই আগে-থেকে-পরিচিত/ইচ্ছাকৃত bug-demonstrating
      টেস্ট বা পূর্ব-পরিচিত unrelated backlog** — Step 20-এর কোনো রিগ্রেশন না (বিস্তারিত progress
      doc-এর "Step 20.5" সেকশনে)। বাকি সব ক্লাস (DualWriteGapTest ২৬/২৬ সহ ~১০০+ টেস্ট) সম্পূর্ণ
      সবুজ। migration-সিঙ্ক সাব-চেক স্কিপড (নতুন RPC/migration নেই)। **GATE অনুযায়ী Step 20.1–20.5
      সবক'টা `[x]` — Step 20 সম্পূর্ণ।**
      🚧 GATE: 20.4-এর সবক'টা fix-ব্যাচ `[x]` (Windows-verified সহ) — **নোট (২০২৬-০৯-২৩, Step 20.3-এর
      ফলাফল): Step 20.4-এ কোনো ব্যাচ তৈরিই হয়নি (০টা 🔴/🟠 পাওয়া গেছে), তাই এই GATE ভ্যাকুয়াসলি
      satisfied ধরে সরাসরি Step 20.5 নেওয়া যাবে, নতুন কোনো fix-ব্যাচের জন্য অপেক্ষা করার দরকার নেই।**
      কাজ: (১) Windows-এ পুরো
      `:app:testDebugUnitTest` (filter ছাড়া) সবুজ কিনা; (২) `CI_TEST_SUITE_PROGRESS.md`-এ Step 20
      চূড়ান্তভাবে বন্ধ (BLOCKED/deferred তালিকাসহ, যদি কিছু থাকে); (৩) migration-ফাইল-সিঙ্ক চেক (Step
      12.12-এর ৩ক ধাপের প্যাটার্নে) — এই Step-এ কোনো নতুন RPC/migration লেখা না হলে (সাধারণত হবে না,
      কারণ retry শুধু client-সাইড কোড) এই সাব-চেক স্কিপ করা যাবে, নোটসহ।

---

## প্রতিটা session-এ Claude যা করবে

1. আপলোড করা zip থেকে project দেখবে, `CI_TEST_SUITE_PROGRESS.md` (যদি আগের session-এর
   থাকে, সেটাও আপলোড করা থাকবে) পড়ে বুঝবে কোন ধাপ পরের।
2. উপরের তালিকা থেকে **প্রথম অসম্পূর্ণ (`[ ]`) ধাপটা** নেবে।
   ⚠️ Step 12.x গ্রুপ `[ ]` থাকা অবস্থায় Step 13 বা তার পরের কোনো ধাপ **কখনোই** নেবে না
   (ব্যবহারকারী চাইলেও না, যতক্ষণ না তিনি স্পষ্টভাবে এই GATE তুলে দিচ্ছেন এমন কিছু লিখছেন)।
   ব্যবহারকারী `WINDOWS RESULT:` paste করলে সেটা এই ধাপ নেওয়ার **আগে** প্রসেস করবে
   (Step 12.x-এর সাধারণ নিয়ম দ্রষ্টব্য)।
   ⚠️ **Step 13, 14, 15, 16, 17, 18, 19, 20 — প্রতিটাই উপ-ধাপে ভাগ করা (13.1–13.8, 14.1–14.4,
   15.1–15.4, 16.1–16.4, 17.1–17.3, 18.1–18.4, 19.1–19.6, 20.1/20.2x/20.3/20.4x/20.5; কিছু ক্ষেত্রে আরও ভেতরে
   15.3a/16.3a/19.4a-জাতীয় dynamic উপ-উপ-ধাপ — Step 20.2x/20.4x-ও এই একই প্যাটার্নের, সংখ্যা Step 20.1/20.3-এ ঠিক হয়)।** একটা সেশন = শুধু একটা উপ-ধাপ (12.x-এর মতোই); প্রতিটা মূল
   Step-এর নিজস্ব GATE আছে (যেমন 13.1–13.8 সব `[x]` না হলে Step 14 শুরু হবে না) —
   বিস্তারিত প্রতিটা Step-এর নিজের সেকশনে লেখা আছে। "প্রথম অসম্পূর্ণ ধাপ" নেওয়ার সময়
   মূল Step-লেভেলে না, সরাসরি সেই Step-এর ভেতরের প্রথম অসম্পূর্ণ উপ-ধাপ নেবে।
3. সংশ্লিষ্ট ফাংশন/কোডের জন্য:
   - SQL ধাপের ক্ষেত্রে: আসল SQL migration থেকে signature + logic
     verify করবে, প্রয়োজনীয় টেবিল/কলাম zip থেকে যাচাই করবে,
     `supabase/tests/0X_<feature_name>.sql` নামে pgTAP test file লিখবে
     (happy path + কমপক্ষে একটা failure/edge case per function)
   - Kotlin ধাপের ক্ষেত্রে (Step 12, 15, 16, 18, 19): আসল repository/UI/viewmodel
     কোড থেকে লজিক verify করবে, `app/src/test/java/com/example/`-এ
     JVM/Robolectric test লিখবে (Step 15-এ প্রয়োজনে display-লজিক আলাদা
     pure function-এ বের করে আনবে, existing Composable ভেঙে না ফেলে)
   - RLS ধাপের ক্ষেত্রে (Step 14): একই query একাধিক ভিন্ন auth-context
     দিয়ে চালিয়ে verify করবে, শুধু single-user happy-path না
4. দরকার হলে `full-test.yml`-এ নতুন test file/job যোগ করার লাইন বসাবে।
5. `CI_TEST_SUITE_PROGRESS.md`-এ সেই ধাপের `[ ]` কে `[x]` করবে + এক লাইন নোট (কী
   সমস্যা পাওয়া গেছে, কী assumption নেওয়া হয়েছে)।
6. **পুরো repo (app কোড + migrations + নতুন/আপডেট হওয়া `supabase/tests/`,
   `scripts/`, `.github/workflows/`, `CI_TEST_SUITE_MASTER_PROMPT.md`,
   `CI_TEST_SUITE_PROGRESS.md` সহ — শুধু এই session-এ পরিবর্তিত ফাইল না,
   পুরোটাই) **একটা zip ফাইলে** প্যাক করে সেটাই দেবে, ব্যবহারকারী যেন পরের
   session-এ এই একটা zip আপলোড করলেই যথেষ্ট হয় — আলাদা আলাদা ফাইল
   দিয়ে ব্যবহারকারীকে নিজে merge করতে না হয়।
   ⚠️ **zip বানানোর সময় dotfile/dot-folder (`.github/`, `.env.example`
   ইত্যাদি) যেন বাদ না পড়ে যায়** — `zip -r out.zip . -x ".*"`-এর মতো
   কমান্ড ব্যবহার করলে `-x ".*"` প্যাটার্নে `.github/` পুরো ফোল্ডারই
   silently বাদ পড়ে যায় (এটা এই প্রজেক্টেই একবার সত্যিই ঘটেছিল — CI
   workflow ফাইলটাই zip-এ ছিল না, অথচ zip command কোনো error দেয়নি)।
   তাই zip বানানোর পরে **অবশ্যই `unzip -l` দিয়ে ফাইল-তালিকা verify
   করবে** যে `.github/workflows/full-test.yml` (এবং অন্য কোনো dotfile
   থাকলে সেটাও) আসলেই ভেতরে আছে — assume করবে না যে zip command ঠিকমতো
   সব ধরেছে।
7. সংক্ষিপ্ত সারাংশ দিয়ে থেমে যাবে — পরের ধাপে যাবে না, এমনকি সময়/context
   থাকলেও না।
