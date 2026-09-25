# RPC Sync / Dual-Write Fix — Progress Log

এই ফাইলে প্রতিটা ধাপের Claude session কাজ শেষে নতুন এন্ট্রি যোগ করে। পুরনো এন্ট্রি কখনো
এডিট/মুছে ফেলা হয় না।

---

## [Step 1 — RPC ইনভেন্টরি নিশ্চিতকরণ] — 2026-09-18

- **কী করা হয়েছে:** `app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt`-এ
  কল হওয়া সব RPC (৯১টা ইউনিক) আর `supabase/migrations/*.sql`-এ ডিফাইন করা সব RPC (৪০টা)
  বের করে ডিফ করা হয়েছে। মিসিং RPC-দের প্রতিটার Kotlin caller ফাংশন ম্যাপ করা হয়েছে এবং
  টাকা-সংক্রান্তগুলো আলাদা ফ্ল্যাগ করা হয়েছে। কোনো কোড/SQL ফাইল পরিবর্তন করা হয়নি — শুধু
  একটা নতুন রিপোর্ট ফাইল যোগ হয়েছে।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - যোগ: `docs/RPC_INVENTORY_REPORT.md` (নতুন)
  - যোগ: `RPC_SYNC_FIX_PROGRESS.md` (এই ফাইল, নতুন)
  - বিদ্যমান কোনো ফাইল পরিবর্তন হয়নি
- **যাচাই কীভাবে করা হয়েছে:**
  - Kotlin RPC কল কাউন্ট (`grep -A1 'postgrest\.rpc('` + regex extract) → ৯১টা ইউনিক নাম
  - migration ফাইলের `CREATE (OR REPLACE) FUNCTION public.<name>` কাউন্ট → ৪০টা
  - `public.` prefix ছাড়া কোনো ফাংশন সংজ্ঞা আছে কিনা আলাদা যাচাই করা হয়েছে → পাওয়া যায়নি
  - `comm -23` দিয়ে ডিফ → ৬৪টা RPC মিসিং পাওয়া গেছে
  - প্রতিটা মিসিং RPC-এর Kotlin caller সোর্স স্ক্যান করে ম্যাপ করা হয়েছে এবং একটা ভুল
    ম্যাপিং (একই-লাইন RPC কল প্যাটার্নের কারণে `admin_credentials_get_phone` →
    `getAdminPhoneSecure` হওয়ার কথা ছিল, স্ক্রিপ্ট ভুলভাবে UNKNOWN দেখিয়েছিল) ম্যানুয়ালি
    সোর্স দেখে ঠিক করা হয়েছে
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** কোনো প্রভাব নেই — শুধু দুইটা নতুন ডকুমেন্টেশন
  ফাইল যোগ হয়েছে, কোনো বিদ্যমান কোড/স্কিমা ছোঁয়া হয়নি।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - Step 2-এ Supabase MCP কানেক্টর দিয়ে এই ৬৪টা RPC-এর verbatim সংজ্ঞা লাইভ DB থেকে
    উদ্ধার করে নতুন migration ফাইলে বসাতে হবে
  - `admin_credentials_get_phone`-এর caller ম্যানুয়ালি ভেরিফাই করা হয়েছে (`getAdminPhoneSecure`),
    কিন্তু Step 2 শুরুর আগে পুরো caller-ম্যাপিং স্ক্রিপ্টের একই-লাইন-বাগ (মাত্র ৩টা RPC কল
    এতে প্রভাবিত হয়, বাকি ৬১টা সঠিক) আরেকবার ক্রস-চেক করে নেওয়া ভালো
  - **⚠️ নতুন আবিষ্কার (স্কোপের বাইরে, নতুন ধাপ লাগবে):** `SupabaseSyncManager.kt`-এ
    ডকুমেন্টেড একটা পুরনো বাগ পাওয়া গেছে যেখানে raw `postgrest.from(...).update()`
    (RLS-নির্ভর, row-count চেক ছাড়া) কল ব্যবহারের কারণে পুরো প্রজেক্টে কোনো ইউজারের
    `kyc_status` কখনো cloud-এ আপডেট হয়নি অথচ কোনো error-ও থ্রো হয়নি (RLS ০ row ম্যাচ করলে
    PostgREST "সফল" রিটার্ন করে)। এটা `submit_kyc` RPC দিয়ে ফিক্স হয়েছে, কিন্তু একই
    রিস্কি প্যাটার্ন (raw `.update`/`.upsert`/`.delete`, row-count-চেক ছাড়া) আরও ~২২ জায়গায়
    এখনো আছে (`upsertCategory`, `setCategoryActive`, `deleteCategoryRemote`, `upsertFaq`,
    `deleteFaqRow`, `adminSoftDeleteProblem` ইত্যাদি)। মাস্টার প্ল্যানে এর জন্য এখনো কোনো
    ধাপ নেই — বিস্তারিত `docs/RPC_INVENTORY_REPORT.md`-এর "বাড়তি আবিষ্কার" অংশে। ইউজারকে
    জানানো দরকার একটা নতুন ধাপ (সাময়িক নাম: "Step 4.5 — RLS silent-noop audit") যোগ করা
    উচিত কিনা।
- **zip ফাইলে মোট ফাইল সংখ্যা:** যাচাই নিচে দেখুন (আগে vs পরে তুলনা)।

---

## [Step 4.5 — RLS silent-noop audit] — 2026-09-18

- **কী করা হয়েছে:** `SupabaseSyncManager.kt`-এ Step 1-এ চিহ্নিত raw `.update`/`.upsert`/`.delete`
  প্যাটার্ন (row-count চেক ছাড়া) পুনরায় স্ক্যান করে **১৯টা** সাইট পাওয়া গেছে (Step 1 রিপোর্টে ২২
  বলা হয়েছিল — এই zip ভিন্ন ট্র্যাকের snapshot বলে সংখ্যায় অমিল, বিস্তারিত নিচে)। এর মধ্যে **১৬টা**
  সাইটে একটা নতুন প্রাইভেট হেল্পার `requireAffectedRowOrThrow()` যোগ করে ওয়্যার করা হয়েছে —
  builder lambda-তে `select()` যোগ করে রেসপন্স থেকে affected row গোনা হয়, ০ হলে
  `IllegalStateException` থ্রো হয় যা বিদ্যমান `catch(Exception)` ব্লক ধরে `Result.failure()`
  রিটার্ন করে। বাকি **৩টা সাইট ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছে** (কারণ নিচে)।
- **যে ১৬টা ফাংশনে গার্ড যোগ হয়েছে:**
  `updateOwnProfile`, `syncInstantJobNotificationToggle`, `syncFreeJobQuota`, `upsertCategory`,
  `setCategoryActive`, `deleteCategoryRemote`, `upsertFaq`, `deleteFaqRow`,
  `adminSoftDeleteProblem`, `adminUpdateKycInfo`, `adminResetKycToPending`,
  `upsertPlatformSetting`, `adminUpdateProblemCommissionRate`, `updateProblemFull`,
  `explorerUpdateRow`, `explorerDeleteRow` — সবগুলোই এমন কেস যেখানে টার্গেট row/key সবসময়
  বিদ্যমান থাকা উচিত (নির্দিষ্ট userId/id/key দিয়ে টার্গেট করা), তাই ০ row মানেই আসলে কিছু ভুল হয়েছে।
- **যে ৩টা ফাংশন ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছে (গার্ড যোগ করলে নতুন false-positive বাগ তৈরি হতো):**
  - `markMessagesAsReadForProblem` — filter শুধু `problem_id`+`receiver_id`, `is_read` দিয়ে না;
    এই থ্রেডে receiver-এর কোনো মেসেজ না থাকলে ০ row **বৈধ**, বাগ না।
  - `markNotificationAsRead` — ফাংশনের নিজের comment-এই ডকুমেন্টেড: notification অন্য কারো হলে
    RLS-এর কারণে ০ row **ইচ্ছাকৃতভাবে চুপচাপ no-op** (এটাই এখানে authorization boundary,
    silent-noop বাগ না)।
  - `markAllNotificationsAsRead` — ইউজারের কোনো unread notification না থাকলে ০ row **স্বাভাবিক
    empty-state**, বাগ না।
  এই তিনটাতে গার্ড বসালে "কিছু ভাঙেনি" নীতি ভঙ্গ হতো — legitimate ০-row সাফল্যকে ভুলভাবে
  exception/failure বানিয়ে ফেলতো। তাই এগুলো অপরিবর্তিত রাখা হয়েছে।
- **কোন ফাইল পরিবর্তন হয়েছে:**
  - পরিবর্তন: `app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt` (নতুন
    প্রাইভেট হেল্পার + ১৬টা ফাংশনে additive গার্ড; কোনো ফাংশনের নাম/প্যারামিটার/রিটার্ন টাইপ
    বদলায়নি, কোনো caller/ViewModel/UI কোড ছোঁয়া হয়নি)
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)
- **যাচাই কীভাবে করা হয়েছে:**
  - পুরো ফাইলের brace/paren balance পাইথন স্ক্রিপ্ট দিয়ে গণনা করে মিলিয়ে দেখা হয়েছে (630/630
    braces, 2053/2053 parens)
  - প্রতিটা গার্ড করা সাইটের DTO/ফিল্টার-কলাম (`CategoryDto.id`, `FaqDto.id` ইত্যাদি) সোর্স
    দেখে যাচাই করা হয়েছে যাতে ভুল ফিল্ড নাম না বসে
  - **⚠️ Android Studio/Gradle build চালিয়ে কম্পাইল ভেরিফাই করা হয়নি (এই সেশনে বিল্ড এনভায়রনমেন্ট
    নেই) — `select()` DSL কল supabase-kt (bom ৩.৬.০)-তে update/upsert/delete builder-এ ঠিক
    কম্পাইল হয় কিনা, এটা মূলত insert()-এ পরিচিত প্যাটার্ন থেকে অনুমান করা হয়েছে। পরবর্তী ধাপে
    যাওয়ার আগে একবার Android Studio-তে বিল্ড করে দেখে নেওয়া জরুরি।**
  - কোনো ইউনিট/ইন্টিগ্রেশন টেস্ট লেখা হয়নি এই ধাপে (Supabase client mock করা জটিল, স্কোপ ছোট
    রাখতে বাদ দেওয়া হয়েছে) — পরের ধাপে দরকার হলে যোগ করা যাবে।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** happy-path (>=1 row ম্যাচ) কেসে কোনো আচরণ বদলায়নি —
  শুধু আগে যেসব কেসে silently "success" রিটার্ন হতো (০ row ম্যাচ, প্রকৃতপক্ষে বাগ) সেগুলো এখন
  `Result.failure()` রিটার্ন করবে ১৬টা ফাংশনে। ৩টা বাদ-দেওয়া ফাংশনে কোনো পরিবর্তন নেই।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - Android Studio-তে আসল বিল্ড করে `select()` DSL কম্পাইল হচ্ছে কিনা যাচাই করা এখনো বাকি —
    এটা এই ধাপের সবচেয়ে বড় ওপেন রিস্ক।
  - Step 1-এর রিপোর্ট করা ২২ বনাম এই সেশনের পাওয়া ১৯ — সংখ্যার অমিল এই zip ভিন্ন ট্র্যাক (Role-UID
    sync fix) থেকে আসা বলে হতে পারে; পরের কোনো সেশনে আসল RPC-ট্র্যাক zip দিয়ে আবার ক্রস-চেক করা
    ভালো, যাতে কোনো সাইট বাদ না পড়ে।
  - `Result.failure()` এখন নতুন করে এই ১৬টা ফাংশনে আসতে পারবে বলে caller-সাইড (ViewModel/UI)-এ
    এই নতুন failure case কীভাবে হ্যান্ডল হচ্ছে সেটা এই ধাপে চেক করা হয়নি (স্কোপের বাইরে রাখা হয়েছে,
    যেহেতু বেশিরভাগ caller ইতিমধ্যেই generic `Result.failure` handle করে) — যদি কোনো UI স্ক্রিনে
    এই নতুন failure-এ কোনো bad UX দেখা যায়, সেটা আলাদাভাবে রিপোর্ট করতে হবে।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৬৭ (dotfile সহ) — এই ধাপে কোনো ফাইল যোগ/মুছা হয়নি, শুধু
  বিদ্যমান ২টা ফাইলের কনটেন্ট পরিবর্তন হয়েছে।

## [Step 2 — Live Supabase থেকে মিসিং RPC-এর আসল সংজ্ঞা উদ্ধার] — 2026-09-18

- **কী করা হয়েছে:** Supabase MCP কানেক্টর (project: `somadhan`, id `mghvvpndkxnscwryfkib`)
  দিয়ে লাইভ DB-তে কানেক্ট করে `docs/RPC_INVENTORY_REPORT.md`-এর ৬৪টা মিসিং RPC-এর প্রতিটার
  actual সংজ্ঞা `pg_get_functiondef()` দিয়ে verbatim বের করা হয়েছে (function body, params,
  return type, SECURITY DEFINER/INVOKER, search_path সব সহ), সাথে সংশ্লিষ্ট `GRANT EXECUTE`
  স্টেটমেন্টও (`aclexplode` দিয়ে)। ৭টা নামের একাধিক overload পাওয়া গেছে — কোনটা "সঠিক" সেটা
  অনুমান না করে **সবগুলো overload verbatim ক্যাপচার করা হয়েছে** (নিচে দেখুন)। কোনো ফাংশনের
  লজিক/হোয়াইটস্পেস/কমা এক বিন্দুও বদলানো হয়নি — শুধু raw output নতুন migration ফাইলে কপি করা
  হয়েছে।
- **সব ৬৪টা RPC লাইভে পাওয়া গেছে — কোনোটাই "NOT FOUND LIVE" না।**
- **একাধিক overload পাওয়া গেছে যাদের জন্য:** `admin_adjust_balance` (2), `admin_notify_user`
  (2), `admin_set_banned` (2), `admin_set_restricted` (2), `create_notification` (2),
  `log_admin_action` (2), `submit_reputation_event` (2) — মোট ৭১টা ফাংশন সংজ্ঞা ক্যাপচার
  করা হয়েছে (৬৪ ইউনিক নাম + ৭ অতিরিক্ত overload)।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - যোগ (নতুন migration ফাইল, সব `supabase/migrations/`-এ, নামের শুরুতে `recovered_`):
    - `recovered_admin_kyc_ban_role.sql` (7 নাম: admin_approve_kyc, admin_reject_kyc,
      admin_revoke_kyc, admin_set_banned, admin_set_restricted, admin_set_verified_badge,
      admin_change_role)
    - `recovered_admin_credentials.sql` (3 নাম: admin_credentials_get_phone,
      admin_credentials_update, admin_credentials_verify_password)
    - `recovered_admin_money.sql` (3 নাম: admin_adjust_balance, admin_confirm_gateway_deposit,
      admin_refund_and_reopen_problem)
    - `recovered_admin_moderation.sql` (14 নাম: admin_delete_message,
      admin_delete_notification_group, admin_delete_rating, admin_manually_flag_dispute,
      admin_send_message_to_problem_chat, admin_broadcast_notification, admin_notify_user,
      admin_reassign_solver, admin_remove_category_from_solvers,
      admin_update_direct_contract_status, admin_update_problem_budget,
      admin_update_problem_status, admin_force_cancel_instant_job, log_admin_action)
    - `recovered_bidding_contracts.sql` (5 নাম: accept_bid, cancel_bid, reject_bid,
      accept_direct_contract, decline_direct_contract)
    - `recovered_instant_jobs.sql` (8 নাম: broadcast_instant_job, cancel_instant_job,
      expire_broadcasting_instant_job, mark_job_started, mark_solver_arrived,
      mark_solver_on_way, solver_cancel_job, system_notify_48hour_auto_release)
    - `recovered_job_release.sql` (3 নাম: cancel_job_release_request,
      reject_job_release_request, request_job_release)
    - `recovered_money_flow.sql` (10 নাম: deposit_money_via_gateway, process_withdrawal,
      refund_escrow_once, release_escrow, request_extra_amount, request_wallet_deposit,
      request_withdrawal, respond_additional_charge, user_confirm_extra_amount,
      user_reject_extra_amount)
    - `recovered_disputes.sql` (5 নাম: raise_dispute, resolve_dispute, settle_dispute,
      withdraw_dispute, request_admin_assistance)
    - `recovered_notifications.sql` (2 নাম: create_notification, notify_admins)
    - `recovered_kyc_rating_reputation.sql` (3 নাম: submit_kyc, submit_rating,
      submit_reputation_event)
    - `recovered_role_switch.sql` (1 নাম: switch_role_get_or_create_linked_profile)
  - পরিবর্তন: `docs/RPC_INVENTORY_REPORT.md` (নতুন "Step 2 আপডেট" সেকশন যোগ, আগের কন্টেন্ট
    অক্ষত)
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)
  - বিদ্যমান কোনো migration ফাইল এডিট/মুছা হয়নি
- **যাচাই কীভাবে করা হয়েছে:**
  - প্রতিটা নতুন ফাইলে `$function$` দোলার-কোট ট্যাগের সংখ্যা জোড় (even) কিনা যাচাই করা
    হয়েছে (open/close মিলছে) — সব ১২টা ফাইলে ঠিক পাওয়া গেছে
  - প্রতিটা ফাইলে ব্যবহৃত দোলার-কোট ট্যাগ শুধু `$function$` কিনা (অন্য কোনো ট্যাগ মিশে যায়নি)
    যাচাই করা হয়েছে
  - `CREATE OR REPLACE FUNCTION` স্টেটমেন্ট কাউন্ট করে মোট ৭১টা পাওয়া গেছে — লাইভ DB
    থেকে পাওয়া ৭১টা row-এর সাথে মিলেছে (৬৪ + ৭ overload)
  - প্রতিটা গ্রুপের RPC নাম লিস্ট ম্যানুয়ালি Step 1-এর ৬৪-নামের লিস্টের সাথে ক্রস-চেক করা
    হয়েছে — কোনো নাম বাদ পড়েনি, কোনো ডুপ্লিকেট নেই
  - ⚠️ এই ধাপে migration ফাইলগুলো লাইভ DB-তে **apply করা হয়নি** (শুধু repo-তে যোগ করা
    হয়েছে) — এগুলো ইতিমধ্যে লাইভে বিদ্যমান ফাংশনের ডকুমেন্টেশন কপি মাত্র, তাই apply করার
    দরকারও নেই এই মুহূর্তে; ভবিষ্যতে fresh DB সেটআপ/migration replay-এর সময় এগুলো কাজে
    লাগবে
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** কোনো প্রভাব নেই — কোনো লাইভ RPC-এর লজিক বদলানো
  হয়নি, শুধু বিদ্যমান লাইভ সংজ্ঞা নতুন migration ফাইলে ডকুমেন্ট করা হয়েছে (verbatim কপি)।
  Kotlin কোডের কোনো caller ছোঁয়া হয়নি।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - **⚠️ Overload অস্পষ্টতা (Step 2-এর স্কোপের বাইরে রাখা হয়েছে):** ৭টা RPC নামের একাধিক
    overload লাইভে আছে। এই ধাপে সবগুলো capture হয়েছে, কিন্তু কোনটা Kotlin থেকে actual কল
    হয় সেটা যাচাই করা হয়নি। Step 3 (sync verification) বা পরে কোনো ধাপে এই ফাংশনগুলো
    ছুঁতে হলে আগে `SupabaseSyncManager.kt`-এ প্রতিটার call-site দেখে confirm করে নেওয়া
    জরুরি, নাহলে ভুল overload edit হয়ে যেতে পারে।
  - **⚠️ zip ট্র্যাক নোট (Step 4.5 এন্ট্রিতে flagged, এখনো resolve হয়নি):** এই zip-টা
    "Role-UID sync fix" ট্র্যাকের snapshot বলে মনে হচ্ছে, RPC-fix ট্র্যাকের ধারাবাহিক আউটপুট
    না-ও হতে পারে (Step 1 → Step 4.5-এ ফাইল-কাউন্ট অমিল পাওয়া গিয়েছিল)। এই সেশনে যা পাওয়া
    গেছে (`RPC_SYNC_FIX_PROGRESS.md`, `docs/RPC_INVENTORY_REPORT.md` উভয়ই Step 1/4.5-এর
    এন্ট্রি সহ ঠিকঠাক উপস্থিত) তার ভিত্তিতে ধরে নেওয়া হয়েছে এই zip-ই সঠিক ধারাবাহিক ফাইল,
    কিন্তু ইউজারের একবার নিশ্চিত করা ভালো যে এই zip-এই মূল RPC-fix ট্র্যাকের সব আগের কাজ
    (বিশেষত Step 1-এর ২২ vs Step 4.5-এর ১৯ সাইট অমিলের কারণ) ধরা আছে, আগে Step 3-এ যাওয়ার
    আগে।
  - Step 3-এ আবার RPC ডিফ চালিয়ে নিশ্চিত করতে হবে মিসিং লিস্ট এখন খালি (এই ১২টা নতুন
    migration ফাইল গণনায় ধরে)।
- **zip ফাইলে মোট ফাইল সংখ্যা:** নিচে "zip সম্পূর্ণতা" ভেরিফিকেশন দেখুন।

## [Step 3 — যাচাই: এখন সব RPC-ই migration ফাইলে আছে] — 2026-09-18

- **কী করা হয়েছে:** Step 1-এর মতোই RPC ডিফ আবার চালানো হয়েছে (Kotlin-এ `postgrest.rpc(...)`
  দিয়ে কল হওয়া বনাম `supabase/migrations/*.sql`-এ `CREATE (OR REPLACE) FUNCTION public.<name>`
  দিয়ে ডিফাইন করা)। **মিসিং লিস্ট এখন খালি (০টা)** — Step 2-এ recover করা ১২টা migration
  ফাইল Kotlin-এর সব ৯১টা RPC কল কভার করছে। এছাড়া Step 2-এ flag করা ৭টা overload-এর মধ্যে
  কোনটা আসলে Kotlin থেকে কল হয় সেটাও ভেরিফাই করা হয়েছে (কোড পড়ে, প্রতিটার একমাত্র call-site
  দেখে) এবং `docs/RPC_INVENTORY_REPORT.md`-এ যোগ করা হয়েছে — ৪টাতে দুটো overload-ই সচল
  (role nullable), ৩টাতে (`admin_notify_user`, `create_notification`, `log_admin_action`)
  পুরনো ছোট overload এখন dead code (role non-nullable ডিফল্ট `""` বলে সবসময় বড় overload
  resolve হয়)।
  একটা নতুন `scripts/check_rpc_sync.sh` স্ক্রিপ্ট বানানো হয়েছে যা এই ডিফ-চেক স্বয়ংক্রিয়ভাবে
  চালায়, মিসিং পেলে non-zero exit code দেয় — সাফল্য ও ব্যর্থতা দুই পাথই আলাদা একটা টেস্ট
  ফাইলে (আসল repo-র বাইরে, /tmp-এ) কৃত্রিমভাবে একটা fake RPC ঢুকিয়ে যাচাই করা হয়েছে।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - পরিবর্তন: `docs/RPC_INVENTORY_REPORT.md` (নতুন "Step 2 ফলো-আপ — Overload resolution
    যাচাই" সেকশন যোগ; Step 3-এ কোনো migration ফাইল বা মিসিং-লিস্ট বদলায়নি কারণ মিসিং লিস্ট
    ইতিমধ্যেই খালি ছিল)
  - যোগ: `scripts/check_rpc_sync.sh` (নতুন, executable — `chmod +x`)
  - পরিবর্তন: `README.md` (নিচে একটা "RPC / migration sync check" সেকশন যোগ, এক লাইনে
    স্ক্রিপ্ট চালানোর কথা বলা)
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)
  - বিদ্যমান কোনো migration ফাইল এডিট/মুছা হয়নি
- **যাচাই কীভাবে করা হয়েছে:**
  - Kotlin RPC এক্সট্র্যাকশন python regex দিয়ে (`postgrest\.rpc\(\s*"..."`) করে ৯১টা ইউনিক
    নাম পাওয়া গেছে — Step 1-এর ৯১-এর সাথে হুবহু মেলে
  - Migration RPC এক্সট্র্যাকশন (`CREATE (OR REPLACE) FUNCTION public.<name>`) দিয়ে ১০৪টা
    ইউনিক নাম পাওয়া গেছে (৪০ আগের + ৬৪ Step 2-এ recover করা)
  - `comm -23` দিয়ে ডিফ → **০টা মিসিং**
  - `scripts/check_rpc_sync.sh` আসল repo-তে চালিয়ে exit code ০ ও "কোনো মিসিং RPC নেই" মেসেজ
    কনফার্ম করা হয়েছে
  - স্ক্রিপ্টের failure-path টেস্ট করার জন্য repo-র বাইরে (`/tmp/test_repo`) একটা কপি বানিয়ে
    সেখানে ইচ্ছাকৃতভাবে একটা fake RPC কল যোগ করে স্ক্রিপ্ট চালানো হয়েছে — সঠিকভাবে ১টা মিসিং
    ধরেছে, exit code ১ দিয়েছে, তারপর টেস্ট কপি মুছে ফেলা হয়েছে (আসল repo-তে এই fake RPC কখনো
    ছিল না)
  - স্ক্রিপ্ট লেখার সময় একটা bug ধরা পড়েছিল (একটা অব্যবহৃত dead-code লাইন `set -o pipefail`-এর
    সাথে মিলে SIGPIPE (exit 141) দিচ্ছিল) — লাইনটা সরিয়ে ফিক্স করে আবার দুই পাথই রি-টেস্ট করা
    হয়েছে
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** কোনো প্রভাব নেই — এই ধাপে শুধু একটা নতুন tooling
  স্ক্রিপ্ট আর ডকুমেন্টেশন যোগ হয়েছে, কোনো app কোড/RPC/migration ছোঁয়া হয়নি।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - কোনো ব্লকার নেই — RPC-migration sync সমস্যা (মূল লক্ষ্য) এখন সমাধান হয়ে গেছে।
  - Step 4 (flat `balance` কলামের reconcile ফিক্স) শুরু করা যায় — এটা independent, ছোট ধাপ।
  - `scripts/check_rpc_sync.sh`-এর সীমাবদ্ধতা (স্ক্রিপ্টের নিজের কমেন্টেই লেখা): শুধু
    "Kotlin-এ কল হয় কিন্তু migration-এ নেই" দিক থেকে চেক করে, উল্টো দিক (migration-এ আছে কিন্তু
    Kotlin থেকে কখনো কল হয় না — dead RPC) চেক করে না।
- **zip ফাইলে মোট ফাইল সংখ্যা:** নিচে "zip সম্পূর্ণতা" ভেরিফিকেশন দেখুন।

## [Step 4 — flat `balance` কলামের reconcile ফিক্স] — 2026-09-18

- **কী করা হয়েছে:** `admin_reconcile_user_balances`-এর জন্য একটা নতুন `CREATE OR REPLACE`
  migration ফাইল যোগ হয়েছে যা বিদ্যমান `step32_6_...sql`/`step36_...sql`-এর কোনোটাই এডিট
  করেনি (ইতিহাস অক্ষত)। **গুরুত্বপূর্ণ আবিষ্কার:** `step32_6` ফাইলটা আসলে stale ছিল —
  `step36_transaction_role_column_and_rpc_dual_write.sql`-এ একই ফাংশনের আরেকটা
  `CREATE OR REPLACE` আছে (transactions.role কলাম-সহ dual-write), যেটাই আসল লাইভ সংজ্ঞা। তাই
  নতুন v2 ফাইলটা step32_6 না, step36-এর ভার্সনের উপর ভিত্তি করে বানানো হয়েছে, যাতে role
  কলাম dual-write হারিয়ে না যায়। বিদ্যমান `balance_user`/`balance_solver` লজিক (ledger
  গণনা, tolerance, dry_run, audit log, transactions insert) একবিন্দু বদলানো হয়নি — শুধু
  প্রতিটা ইউজারের জন্য একটা তৃতীয় চেক যোগ হয়েছে: flat `balance` কলাম == effective
  (post-correction) `balance_user + balance_solver` কিনা। এই ফর্মুলা নতুন বানানো হয়নি —
  `MIGRATION_PROGRESS.md`-এর "MONEY_FLOW_AND_ADMIN_BUGS ধাপ ৬ ফলো-আপ" এন্ট্রিতে আগে থেকেই
  একই ফর্মুলা দিয়ে একটা stale row ম্যানুয়ালি sync করা হয়েছিল, সেটা রেফারেন্স হিসেবে ব্যবহার
  করা হয়েছে। মিসম্যাচ পেলে রিপোর্টে `role: "FLAT"` আইটেম যোগ হয় (বিদ্যমান USER/SOLVER আইটেম
  স্টাইলেই)। `dry_run` ডিফল্ট `true` অপরিবর্তিত।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - যোগ: `supabase/migrations/step37_admin_reconcile_user_balances_v2_flat_balance.sql` (নতুন)
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)
  - বিদ্যমান কোনো migration ফাইল (`step32_6`, `step36` সহ) এডিট/মুছা হয়নি
  - `SomadhanRepository.kt`-এর `reconcileUserBalances`/সংশ্লিষ্ট কোনো Kotlin ফাইল ছোঁয়া হয়নি
- **যাচাই কীভাবে করা হয়েছে:**
  - নতুন ফাইলে `$function$` ট্যাগ জোড় (২টা — open/close) কিনা যাচাই করা হয়েছে
  - প্যারেন্থেসিস ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে গণনা করা হয়েছে (৫৮/৫৮ মিলেছে)
  - `grep -rl admin_reconcile_user_balances supabase/migrations/*.sql` দিয়ে নিশ্চিত করা
    হয়েছে যে `step32_6`/`step36`-ই একমাত্র আগের সংজ্ঞা, আর নতুন `step37` সবচেয়ে বড়
    step-নম্বর (repo-র বিদ্যমান সর্বোচ্চ ছিল ৩৬)
  - flat `balance == balance_user + balance_solver` ফর্মুলাটা অনুমান না করে
    `MIGRATION_PROGRESS.md`-এ আগের একটা ম্যানুয়াল ফিক্স এন্ট্রি থেকে কনফার্ম করা হয়েছে
  - ⚠️ Supabase MCP দিয়ে এই নতুন ফাংশন সরাসরি লাইভ DB-তে execute/dry-run করে যাচাই করা
    হয়নি এই সেশনে (শুধু ফাইল আকারে যোগ হয়েছে) — file-level static checks-ই একমাত্র যাচাই
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** এই migration এখনো লাইভ DB-তে apply করা হয়নি,
  তাই এই মুহূর্তে কোনো প্রভাব নেই। Apply করার পরও: `p_dry_run` ডিফল্ট `true` বলে সরাসরি কল
  করলে কোনো ডেটা বদলাবে না, শুধু রিপোর্ট রিটার্ন করবে। `dry_run=false` দিয়ে চালালেই প্রথমবার
  flat `balance` কলাম সংশোধন শুরু হবে (আগে এই RPC এই কলাম touch করতো না)।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - `step32_6`/`step36` দুটো ফাইলই এখন repo-তে আছে কিন্তু `step36`-ই আগে কার্যকর ছিল (পরে
    define হওয়ায় জিতেছিল) — future কোনো সেশন যদি `admin_reconcile_user_balances` নিয়ে আবার
    কাজ করে, এই `step37`-ই এখন সবচেয়ে সাম্প্রতিক সংজ্ঞা, এটা ভুলে `step32_6`/`step36` থেকে
    আবার শুরু না করে।
  - Master plan-এর Step 4-এর কাজ শেষ (independent ধাপ ছিল) — পরবর্তী ধাপ Step 4.5b
    (money-সংক্রান্ত raw `.update`/`.upsert`/`.delete` ফাংশনের row-count safety-net, যেগুলো
    Step 4.5-এ ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছিল) বা Step 5 (outbox ডিজাইন ডক) — ইউজার ঠিক করবে
    কোনটা আগে।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮১ (dotfile সহ) — এই ধাপে ১টা নতুন migration ফাইল যোগ
  হয়েছে, বাকি সব অপরিবর্তিত (২৮০ + ১)।

### Follow-up — migration লাইভে apply করা হলো (একই দিন, ইউজারের সরাসরি নির্দেশে)

- **কী করা হয়েছে:** ইউজার স্পষ্টভাবে বলার পর, Supabase MCP (`apply_migration`, project
  `mghvvpndkxnscwryfkib`) দিয়ে উপরের `step37_admin_reconcile_user_balances_v2_flat_balance`
  migration-টা **লাইভ DB-তে apply করা হয়েছে** — `admin_reconcile_user_balances(boolean)`
  ফাংশনটা এখন লাইভে flat-balance-চেক-সহ ভার্সন।
- **যা apply করা হয়নি:** ফাংশনটা শুধু **define/replace** করা হয়েছে — কোনো actual
  `admin_reconcile_user_balances(false)` কল (dry_run=false, যেটা টাকার মান বদলাতো) এই
  সেশনে চালানো হয়নি। ফাংশনটার নিজের `p_dry_run default true` গার্ড অপরিবর্তিত, তাই ভুলে
  parameter ছাড়া কল হলেও কোনো ডেটা বদলাবে না।
- **কীভাবে যাচাই করা হয়েছে:**
  - `pg_proc`-এ ফাংশনটা এখন বিদ্যমান কনফার্ম করা হয়েছে
  - পুরো reconciliation লজিক (ledger CTE + mismatch গণনা, USER/SOLVER/FLAT তিনটাই) একটা
    read-only SQL কুয়েরি দিয়ে ম্যানুয়ালি রান করে দেখা হয়েছে (RPC-এর `is_admin()` গার্ড এড়িয়ে,
    যেহেতু SQL editor সেশনে `auth.uid()` NULL থাকে) — কোনো SQL এরর হয়নি
  - বর্তমান লাইভ ডেটায় ফলাফল: ৩টা ইউজার স্ক্যান হয়েছে, **০টা USER মিসম্যাচ, ০টা SOLVER
    মিসম্যাচ, ০টা FLAT মিসম্যাচ** — এই মুহূর্তে কোনো correction প্রয়োজন নেই (আগের সেশনে
    একটা stale legacy row ম্যানুয়ালি sync করা হয়েছিল বলে সামঞ্জস্যপূর্ণ)
  - **⚠️ প্রকৃত RPC (`select admin_reconcile_user_balances(true);`) অ্যাডমিন authentication
    সহ এই সেশনে চালানো হয়নি** (Supabase MCP সেশনে কোনো admin auth.uid() সেট করা যায় না) —
    যাচাই শুধু সমতুল্য read-only কুয়েরি দিয়ে হয়েছে, real app/admin panel-এ RPC কল একবার
    ম্যানুয়ালি টেস্ট করে দেখা এখনো ভালো
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** কোনো ডেটা বদলায়নি (dry_run কল না চালানোয়), শুধু
  ফাংশনের সংজ্ঞা আপডেট হয়েছে। বর্তমান লাইভ ডেটায় যেহেতু flat-balance মিসম্যাচ নেই, পরে
  `dry_run=false` চালালেও এই মুহূর্তে কোনো balance বদলাবে না।
- **zip ফাইলে মোট ফাইল সংখ্যা:** অপরিবর্তিত, ২৮১ (এই ফলো-আপে কোনো নতুন ফাইল যোগ হয়নি, শুধু
  live DB-তে আগের ফাইলের SQL apply হয়েছে + এই progress এন্ট্রি)।

## [Step 5 — Outbox/retry ইনফ্রা ডিজাইন] — 2026-09-18

- **কী করা হয়েছে:** `SomadhanRepository.kt`-এর বিদ্যমান dual-write প্যাটার্ন (Room instant
  write → best-effort RPC কল → fail হলে শুধু `Log.w`) কোড পড়ে বোঝা হয়েছে (উদাহরণ হিসেবে
  `payoutEscrowToSolver`, লাইন ~৩৩২, বিস্তারিত পড়া হয়েছে)। এর ভিত্তিতে একটা সম্পূর্ণ ডিজাইন
  ডকুমেন্ট লেখা হয়েছে — `PendingSyncOutboxEntity` স্কিমা (id, rpc_name, params JSON,
  created_at, retry_count, last_error, status), dual-write ফাংশনের fail-ব্লকে কীভাবে যুক্ত
  হবে তার before/after কোড উদাহরণ (স্পষ্ট করে দেখানো হয়েছে existing success path/Log.w কোনোটাই
  সরবে না, শুধু fail-ব্লকে outbox insert additive যোগ হবে), WorkManager periodic sync ডিজাইন
  (NetworkType.CONNECTED constraint, exponential backoff, ১৫-মিনিট floor + app-open/
  pull-to-refresh one-off trigger), Step 7-এর জন্য প্রস্তাবিত অগ্রাধিকার লিস্ট (money-সংক্রান্ত
  ৭টা ফাংশন, মাস্টার প্ল্যানের Step 7 ব্যাচ ১-এর সাথে ক্রস-চেক করা), UI ইন্ডিকেটর প্রস্তাব
  (badge + retry বাটন, non-blocking), আর রোলব্যাক/মাইগ্রেশন নোট (Room schema version ৫২ →
  ৫৩, non-destructive Migration আবশ্যক)। **কোনো প্রোডাকশন `.kt`/`.sql` ফাইল স্পর্শ করা হয়নি** —
  এই ধাপের বাধ্যতামূলক সীমা অনুযায়ী।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - যোগ: `docs/OUTBOX_RETRY_DESIGN.md` (নতুন)
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)
  - বিদ্যমান কোনো `.kt`/`.sql` ফাইল স্পর্শ করা হয়নি
- **যাচাই কীভাবে করা হয়েছে:**
  - `AppDatabase.kt` থেকে বর্তমান Room schema version (৫২) সরাসরি কোড দেখে কনফার্ম করা হয়েছে
    (অনুমান করা হয়নি)
  - `payoutEscrowToSolver`-এর `.onFailure` ব্লকের exact বর্তমান কোড কোট করা হয়েছে (before/after
    উদাহরণে) যাতে Step 7-এ যে কেউ এই ডকটা পড়ে সহজে diff বুঝতে পারে
  - Step 5 প্রস্তাবিত অগ্রাধিকার লিস্ট মাস্টার প্ল্যানের Step 7 ব্যাচ ১ (payoutEscrowToSolver,
    refundEscrowOnceLocked, addToEscrow) আর ব্যাচ ২ (requestWithdrawal,
    updateWithdrawalStatus, adminAdjustBalance)-এর সাথে মিলিয়ে দেখা হয়েছে — মেলে
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** কোনো প্রভাব নেই — শুধু একটা নতুন ডিজাইন ডকুমেন্ট
  ফাইল যোগ হয়েছে, কোনো বিদ্যমান কোড/স্কিমা ছোঁয়া হয়নি।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - Step 6 (outbox entity/DAO/WorkManager ক্লাস তৈরি, additive-only, কিছু wire হবে না) শুরু
    করার আগে ইউজারের এই ডিজাইন ডক (বিশেষত সেকশন ৮-এর ৩টা পয়েন্ট: schema, retry
    threshold/backoff সংখ্যা, priority order) একবার রিভিউ/approve করা ভালো — মাস্টার প্ল্যানের
    Step 5-এর নিজস্ব "কাজ শেষে জানাও" নির্দেশনা অনুযায়ী।
  - Step 4.5b (money-সংক্রান্ত raw `.update`/`.upsert`/`.delete` row-count safety-net,
    এখনো master plan-এ প্রম্পট লেখা হয়নি) এখনো ইউজারের সিদ্ধান্তের অপেক্ষায় — Step 5/6/7 থেকে
    independent, যেকোনো সময় শুরু করা যায়।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮২ (dotfile সহ) — এই ধাপে ১টা নতুন ডিজাইন-ডক ফাইল যোগ
  হয়েছে, বাকি সব অপরিবর্তিত (২৮১ + ১)।

## [Step 6 — Outbox ইনফ্রা তৈরি (additive only, কোনো dual-write ফাংশন wire হয়নি)] — 2026-09-18

- **কী করা হয়েছে:** `RPC_SYNC_FIX_PROGRESS.md` আর `docs/OUTBOX_RETRY_DESIGN.md` (Step 5-এর
  ডিজাইন) পুরোটা পড়ে সেই অনুযায়ী নতুন Room entity, DAO, RPC dispatch টেবিল, আর WorkManager
  worker ক্লাস তৈরি হয়েছে — **৫টা নতুন ফাইল, বিদ্যমান কোনো ফাইলের বিদ্যমান কোড এডিট হয়নি**
  (`AppDatabase.kt`-এ শুধু entity রেজিস্ট্রেশন + schema version bump + migration যোগ, যা এই
  ধাপের নিজস্ব অনুমোদিত ব্যতিক্রম)।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - যোগ: `app/src/main/java/com/example/data/entity/PendingSyncOutboxEntity.kt` — ডিজাইন ডক
    সেকশন ২-এর স্কিমা হুবহু (id, rpcName, paramsJson, createdAt, retryCount, lastError,
    lastAttemptAt, status)
  - যোগ: `app/src/main/java/com/example/data/dao/PendingSyncOutboxDao.kt` — ইচ্ছাকৃতভাবে
    বিদ্যমান `AppDaos.kt`-এ যোগ না করে **আলাদা নতুন ফাইলে** রাখা হয়েছে, যাতে সেই বিদ্যমান
    ফাইল এই ধাপে একদম অস্পৃষ্ট থাকে। insert/update/getPending/observePendingCount/getById/
    getAllSync/deleteById — ডিজাইন ডকের DAO স্পেক + টেস্টের জন্য দুইটা অতিরিক্ত সুবিধাজনক
    query (getById, getAllSync)
  - যোগ: `app/src/main/java/com/example/data/sync/OutboxRpcDispatcher.kt` — নতুন
    `rpcName → SupabaseSyncManager.xxx()` dispatch টেবিল (ডিজাইন ডক সেকশন ৪), ডিজাইন ডকের
    প্রস্তাবিত ৭টা টাকা-সংক্রান্ত RPC-র জন্য এন্ট্রি (release_escrow, refund_escrow_once,
    increment_escrow_extra_amount [addToEscrow-এর RPC], deposit_money_via_gateway,
    request_withdrawal, process_withdrawal, admin_adjust_balance)
  - যোগ: `app/src/main/java/com/example/worker/OutboxSyncWorker.kt` — `CoroutineWorker`,
    বিদ্যমান `InstantJobExpiryWorker`-এর কনভেনশন অনুসরণ করে (companion object-এ
    `schedulePeriodic()`/`triggerImmediate()`)
  - যোগ: `app/src/test/java/com/example/OutboxSyncTest.kt` — Robolectric ইউনিট টেস্ট (নিচে
    বিস্তারিত)
  - পরিবর্তন: `app/src/main/java/com/example/data/database/AppDatabase.kt` — শুধু: (ক)
    `PendingSyncOutboxEntity`/`PendingSyncOutboxDao` import, (খ) entity `@Database` লিস্টে
    যোগ, (গ) `version = 52` → `53` + ব্যাখ্যা-কমেন্ট, (ঘ) `abstract fun
    pendingSyncOutboxDao(): PendingSyncOutboxDao` যোগ, (ঙ) `MIGRATION_52_53` (শুধু `CREATE
    TABLE IF NOT EXISTS pending_sync_outbox`) যোগ ও `addMigrations(...)` লিস্টে রেজিস্টার।
    বিদ্যমান কোনো লাইন সরানো/বদলানো হয়নি, শুধু additive যোগ
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)
- **ডিজাইন ডক থেকে একটা ছোট, স্পষ্টভাবে-নথিভুক্ত বিচ্যুতি:** ডকের pseudocode
  `SupabaseSyncManager.callRpcByName(...)` লিখেছিল (যেন dispatch টেবিলটা
  `SupabaseSyncManager` object-এরই অংশ)। কিন্তু `SupabaseSyncManager.kt` একটা ৩২০০+ লাইনের
  বিদ্যমান ফাইল, আর Step 6-এর কমন রুল অনুযায়ী তার ভেতরের বিদ্যমান কোড এডিট করা যাবে না।
  তাই dispatch টেবিলটা সম্পূর্ণ নতুন, আলাদা `OutboxRpcDispatcher` object/ফাইলে রাখা হয়েছে,
  যেটা `SupabaseSyncManager`-এর বিদ্যমান public ফাংশনগুলো শুধু কল করে — সেই ফাইলের একটা
  অক্ষরও বদলায়নি। আচরণগতভাবে সমতুল্য, শুধু লোকেশন আলাদা — `OutboxRpcDispatcher.kt`-এর নিজের
  doc comment-এ এটা বিস্তারিত লেখা আছে।
- **⚠️ যাচাই না-হওয়া অনুমান (Step 7-এর জন্য গুরুত্বপূর্ণ নোট):** `OutboxRpcDispatcher`-এর
  প্রতিটা RPC-র জন্য `paramsJson`-এ কোন camelCase key আশা করা হচ্ছে (যেমন release_escrow-এর
  জন্য `{"escrowId": ...}`) তা ডিজাইন ডকের একটামাত্র উদাহরণ (payoutEscrowToSolver) থেকে
  এক্সট্রাপোলেট করে ঠিক করা হয়েছে — **কোনো actual dual-write ফাংশন এখনো insert করছে না বলে
  এই key-নামগুলো কোনো real call-site-এর বিপরীতে যাচাই করা যায়নি।** Step 7-এ
  `payoutEscrowToSolver`/`refundEscrowOnceLocked`/`addToEscrow`-এর fail-ব্লকে যখন প্রকৃত
  outbox insert কোড লেখা হবে, তখন insert-এর সময়কার key নাম আর
  `OutboxRpcDispatcher.kt`-এর `when` branch-এ কমেন্ট করা key নাম হুবহু মিলছে কিনা নিশ্চিত
  করে নেওয়া আবশ্যক (নাহলে worker প্রতিটা retry-তে নীরবে parse-error/failure পাবে, exception
  ছুঁড়বে না — `Result.failure` হিসেবে ফেরত আসবে, `IllegalArgumentException` মেসেজ সহ)।
- **⚠️ অসম্পর্কিত (out-of-scope) আবিষ্কার, ছোঁয়া হয়নি:** `AppDatabase.kt`-এ `version = 49`
  থেকে `50`-এ বাড়ানোর জন্য কোনো `MIGRATION_49_50` কখনো ডিফাইন/রেজিস্টার করা হয়নি
  (`addMigrations(...)` লিস্টে `MIGRATION_48_49`-এর পরেই সরাসরি `MIGRATION_50_51` আসে) —
  আগের `MIGRATION_45_46` গ্যাপের (আগে ফিক্স হয়ে গেছে) মতো একই ধরনের সমস্যা: কোনো ডিভাইস যদি
  version 49-এ থাকা অবস্থায় আপগ্রেড করে, `fallbackToDestructiveMigration` ট্রিগার হয়ে পুরো
  local DB মুছে যাবে। এটা RPC_SYNC_FIX ট্র্যাকের স্কোপের বাইরে (rule অনুযায়ী শুধু "entity
  রেজিস্টার/version bump" ছাড়া বিদ্যমান কোড এডিট নিষেধ, আর এটা আলাদা একটা bug-fix হতো) —
  ছোঁয়া হয়নি, শুধু ইউজারকে জানানোর জন্য এখানে নোট করা হলো। ঠিক করতে হলে আলাদা ছোট ধাপ/সেশন
  লাগবে (৪৫→৪৬-এর ফিক্সের প্যাটার্নে, no-op bridge migration অথবা প্রকৃত schema diff বের করে)।
- **যাচাই কীভাবে করা হয়েছে:**
  - প্রতিটা নতুন/পরিবর্তিত `.kt` ফাইলে ব্রেস/প্যারেন্থেসিস ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে গণনা
    করে মিলিয়ে দেখা হয়েছে (সব ৬টা ফাইলেই brace ও paren কাউন্ট সমান — মিসম্যাচ নেই)
  - `grep`-এ নিশ্চিত করা হয়েছে `MIGRATION_52_53`/`pendingSyncOutboxDao`/`PendingSyncOutboxEntity`
    প্রতিটা প্রত্যাশিত জায়গায় (import, entity লিস্ট, version কমেন্ট, abstract fun, migration
    অবজেক্ট, addMigrations রেজিস্ট্রেশন) ঠিক একবার করে আছে
  - `AppDatabase.kt`-এ আর কোথাও `version = 52` বা অন্য কোনো hardcoded পুরনো ভার্সন রেফারেন্স
    অবশিষ্ট নেই কিনা grep দিয়ে চেক করা হয়েছে
  - ফাইল কাউন্ট: শুরুতে ২৮২, শেষে ২৮৭ — `comm -23` (before vs after) দিয়ে নিশ্চিত করা হয়েছে
    **০টা ফাইল হারায়নি**, আর `comm -13` দিয়ে দেখানো ৫টা নতুন ফাইলই ঠিক সেই ৫টা যা উপরে তালিকাভুক্ত
  - **⚠️ Android Studio/Gradle build চালিয়ে কম্পাইল ভেরিফাই করা হয়নি (এই সেশনেও বিল্ড
    এনভায়রনমেন্ট নেই, আগের ধাপগুলোর মতোই)** — WorkManager/kotlinx-serialization-json উভয়ই
    `app/build.gradle.kts`-এ আগে থেকেই dependency হিসেবে আছে (grep করে কনফার্ম করা হয়েছে,
    নতুন করে যোগ করতে হয়নি), আর নতুন কোড বিদ্যমান `InstantJobExpiryWorker`/`EscrowDao`-র
    established প্যাটার্ন অনুসরণ করে লেখা হয়েছে — কিন্তু Android Studio-তে একবার build করে
    দেখে নেওয়া এখনো বাকি, বিশেষত `OutboxRpcDispatcher.kt`-এর kotlinx.serialization JSON
    extension ফাংশনগুলো (`jsonPrimitive`, `doubleOrNull`, `booleanOrNull`, `isString`)
  - `OutboxSyncTest.kt`-এ ৫টা টেস্ট লেখা হয়েছে (শুধু entity/DAO লজিক, Supabase/network মক করা
    হয়নি — master plan-এর Step 6 স্কোপ অনুযায়ী): (১) insert → getPending রিটার্ন করে, (২)
    getPending শুধু PENDING/RETRYING রিটার্ন করে, DONE/FAILED_PERMANENT বাদ দেয়, (৩)
    observePendingCount শুধু pending-eligible row গোনে, (৪) update() দিয়ে retryCount বাড়িয়ে
    threshold (১০) পার হলে status FAILED_PERMANENT হয় এমন simulate করা হয়েছে
    (`OutboxSyncWorker.MAX_RETRY_COUNT` constant ব্যবহার করে, hardcoded ১০ না — যাতে constant
    বদলালে টেস্টও সাথে সাথে সঠিক থাকে), (৫) deleteById row সম্পূর্ণ মুছে দেয়। **এই টেস্টগুলো
    এই সেশনে Gradle দিয়ে actually রান করা হয়নি (build environment নেই) — Android
    Studio-তে/CI-তে একবার রান করে pass করছে কিনা যাচাই করে নেওয়া জরুরি।**
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** **কোনো প্রভাব নেই।** নতুন টেবিল খালি থাকবে (কোনো
  dual-write ফাংশন এখনো insert করছে না), নতুন worker কোথাও schedule করা হয়নি (WorkManager
  কখনো `OutboxSyncWorker.doWork()` রান করবে না যতক্ষণ না কেউ explicitly
  `schedulePeriodic()`/`triggerImmediate()` কল করে, যা এই ধাপে হয়নি), `SomadhanApp.kt` এই
  ধাপে অস্পৃষ্ট। বিদ্যমান কোনো dual-write ফাংশনের (`payoutEscrowToSolver` ইত্যাদি) কোনো
  লাইন বদলায়নি। schema version bump non-destructive migration দিয়ে হ্যান্ডল করা, তাই
  বিদ্যমান ব্যবহারকারীদের কোনো local ডেটা হারানোর ঝুঁকি নেই।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - Android Studio-তে আসল Gradle build + এই সেশনের ৫টা নতুন টেস্ট রান করে যাচাই করা এখনো
    বাকি — এটাই এই ধাপের সবচেয়ে বড় open risk (উপরে "যাচাই কীভাবে করা হয়েছে" দেখুন)
  - Step 7 শুরুর আগে `OutboxRpcDispatcher.kt`-এর param-key কনভেনশন (উপরে ফ্ল্যাগ করা) একবার
    ক্রস-চেক করে নেওয়া, যাতে Step 7-এর insert কোড আর dispatcher-এর parse কোড একই key-নাম
    ব্যবহার করে
  - `MIGRATION_49_50` গ্যাপ (উপরে flag করা) এখনো resolve হয়নি, ইউজার সিদ্ধান্ত নিলে আলাদা
    ছোট ধাপে ফিক্স করা যাবে (এই RPC_SYNC_FIX ট্র্যাকের স্কোপের বাইরে)
  - Step 7 (ব্যাচ ১: `payoutEscrowToSolver`, `refundEscrowOnceLocked`, `addToEscrow`-এর
    fail-ব্লকে outbox insert wire করা) master plan অনুযায়ী পরের ধাপ
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮৭ (dotfile সহ) — এই ধাপে ৫টা নতুন ফাইল যোগ হয়েছে, বাকি
  সব অপরিবর্তিত (২৮২ + ৫)।

---

## ✅ ধাপ ৬-এর পর গ্যাপ-ফিক্স (mini-session, master plan-এর বাইরে, Step 7-এর আগে)

ব্যবহারকারীর অনুরোধে ধাপ ৬-এ ফ্ল্যাগ করা ২টা জিনিসের মধ্যে একটা এখন ঠিক করা হলো:

**১. `MIGRATION_49_50` — ঠিক করা হয়েছে।** রুট কজ: `UserEntity.verifiedBadgeUser`/
`verifiedBadgeSolver` (ডিফল্ট `true`) কলাম যোগ হওয়ার কারণে version ৪৯→৫০ বাড়ানো হয়েছিল
(কমেন্ট: "বাগ D৩"), কিন্তু `MIGRATION_49_50` কখনো ডিফাইন/রেজিস্টার হয়নি। ফলে schema ঠিক
৪৯-এ আটকে থাকা কোনো পুরনো ইনস্টল আপডেট করলে `fallbackToDestructiveMigration
(dropAllTables = true)` ট্রিগার হয়ে পুরো local DB (balance/job history/message/
notification) মুছে যেত — নতুন ইউজার বা যারা ইতিমধ্যে schema ৫০+-এ আছে, তাদের কোনো প্রভাব
নেই। **ফিক্স:** `AppDatabase.kt`-এ `MIGRATION_48_49` আর `MIGRATION_50_51`-এর মাঝে
`MIGRATION_49_50` যোগ করা হয়েছে — `ALTER TABLE users ADD COLUMN verifiedBadgeUser INTEGER
NOT NULL DEFAULT 1` + একই `verifiedBadgeSolver` (৪৬→৪৭-এর role-scoped boolean কলাম
কনভেনশন অনুসরণ করে), আর `addMigrations()` লিস্টে রেজিস্টার করা হয়েছে। শুধু ২টা নতুন কলাম
যোগ — কোনো বিদ্যমান row/কলাম মোছা/ওভাররাইট হয়নি (rule #৪)।

**২. `OutboxRpcDispatcher.kt` আলাদা ফাইলে থাকা — ইচ্ছাকৃতভাবে ছোঁয়া হয়নি।** এটা খাঁটি
cosmetic (functionality অপরিবর্তিত, শুধু "কোড কোথায়" সংক্রান্ত) — merge করতে হলে ৩২০০+
লাইনের `SupabaseSyncManager.kt` এডিট করতে হতো, যেখানে ঝুঁকি বাস্তব কিন্তু ফাংশনাল বেনিফিট
শূন্য। তাই স্কিপ করা হয়েছে; future-এ কেউ চাইলে আলাদা ছোট cosmetic-refactor ধাপে করা যাবে।

**পরিবর্তিত ফাইল:** `AppDatabase.kt` (শুধু এই একটা) + এই progress ফাইল।

**যাচাই:**
- brace/paren balance (এডিটের পর): `{}` ৯০=৯০, `()` ৫৩৪=৫৩৪
- zip file-list diff: মূল ২৮৭টা ফাইলই (dotfile সহ) অক্ষত আছে, কোনো ফাইল হারায়নি/যোগ হয়নি
  (শুধু কনটেন্ট বদলেছে)
- `grep MIGRATION_49_50` — সংজ্ঞা + `addMigrations()`-এ ঠিক একবার করে রেজিস্টার্ড

**⚠️ real device migration test এখনো বাকি** — schema version ৪৯-এ থাকা একটা ডিভাইস/emulator
থেকে আপডেট করে দেখতে হবে যে local ডেটা টিকে থাকছে (destructive fallback trigger হচ্ছে না) আর
নতুন `verifiedBadgeUser`/`verifiedBadgeSolver` কলাম `1` (true) দিয়ে populate হচ্ছে।

Step 7 (dual-write ফাংশনগুলো outbox-এর সাথে wire করা) এখনো শুরু হয়নি — এই মিনি-সেশন শুধু
gap-fix, master plan-এর মূল ট্র্যাকে কোনো পরিবর্তন না।

---

## [Step 7 (ব্যাচ ১) — সবচেয়ে ঝুঁকিপূর্ণ (টাকা-সংক্রান্ত) ফাংশনগুলোতে outbox wire করা: escrow payout/refund] — 2026-09-18

- **কী করা হয়েছে:** Step 6-এ তৈরি হওয়া outbox ইনফ্রা (`PendingSyncOutboxEntity`/`PendingSyncOutboxDao`)
  `SomadhanRepository.kt`-এর ঠিক ৩টা dual-write ফাংশনের Supabase fail-ব্লকে wire করা হলো:
  `payoutEscrowToSolver`, `refundEscrowOnceLocked`, `addToEscrow` — মাস্টার প্ল্যানের Step 7
  ব্যাচ ১ নির্দেশনা অনুযায়ী, শুধু এই ৩টাই। প্রতিটার happy-path লজিক (Room write, UI-তে যা দেখা
  যায়) এক বিন্দুও বদলায়নি — শুধু আগে যেখানে `.onFailure { e -> Log.w(...) }` ছিল, সেখানে
  outbox-এ একটা PENDING entry insert করা যোগ হয়েছে (নতুন শেয়ার্ড `enqueueOutboxRetry()`
  হেল্পার দিয়ে)। ফাংশন সিগনেচার (নাম/প্যারামিটার/রিটার্ন টাইপ) কোনোটারই একবিন্দু বদলায়নি।
  `OutboxSyncWorker` (Step 6-এ তৈরি, কিন্তু schedule করা হয়নি) এখন `SomadhanApp.kt`-এ periodic
  schedule করা হলো, নাহলে নতুন insert হওয়া entry কখনো retry হতো না।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - পরিবর্তন: `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — ৪টা
    additive হাংক (কোনো বিদ্যমান লাইন সরানো/বদলানো হয়নি, grep+diff দিয়ে নিশ্চিত করা হয়েছে):
    (ক) নতুন `private val pendingSyncOutboxDao = db.pendingSyncOutboxDao()` (অন্য DAO field-দের
    পাশে) — Step 6 এ যোগ হওয়া DAO ব্যবহারের জন্য প্রয়োজনীয় রেফারেন্স;
    (খ) নতুন শেয়ার্ড `private suspend fun enqueueOutboxRetry(rpcName, params, error)` হেল্পার
    (৩টা ফাংশনই এটা ব্যবহার করে, কোড ডুপ্লিকেশন এড়াতে) — শুধু outbox-এ insert করে, নিজে RPC
    আবার কল করে না, exception ছোড়ে না;
    (গ) `payoutEscrowToSolver`-এর `.onFailure` ব্লকে `enqueueOutboxRetry("release_escrow", ...)`
    কল যোগ (paramsJson: `{"escrowId": ...}`);
    (ঘ) `refundEscrowOnceLocked`-এর `.onFailure` ব্লকে
    `enqueueOutboxRetry("refund_escrow_once", ...)` কল যোগ (paramsJson: `{"escrowId",
    "refundType", "refundPercentage"}`);
    (ঙ) `addToEscrow`-এর `.onFailure` ব্লকে
    `enqueueOutboxRetry("increment_escrow_extra_amount", ...)` কল যোগ (paramsJson: `{"escrowId",
    "amount"}`)
  - পরিবর্তন: `app/src/main/java/com/example/SomadhanApp.kt` — `onCreate()`-এ
    `InstantJobExpiryWorker`-এর schedule-কলের ঠিক পাশে, একই try/catch কনভেনশনে, নতুন
    `OutboxSyncWorker.schedulePeriodic(this)` কল যোগ (additive, বিদ্যমান কোনো লাইন বদলায়নি)।
    ইচ্ছাকৃতভাবে `triggerImmediate()` এই ধাপে কল করা হয়নি (app-start-এ outbox স্বাভাবিকভাবে
    খালি থাকে) — সেটা Step 8-এ UI "retry now" বাটনের জন্য রাখা হলো।
  - পরিবর্তন: `app/src/test/java/com/example/OutboxSyncTest.kt` — ৩টা নতুন টেস্ট যোগ (নিচে
    বিস্তারিত)।
  - পরিবর্তন: `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)।
- **স্কোপ-শৃঙ্খলা:** মাস্টার প্ল্যানের নির্দেশনা অনুযায়ী এই ব্যাচে অন্য কোনো dual-write ফাংশন
  (`requestWithdrawal`, `updateWithdrawalStatus`, `adminAdjustBalance` ইত্যাদি) ছোঁয়া হয়নি —
  Step 5-এর ডিজাইন ডক অনুযায়ী এগুলো পরের ব্যাচগুলোতে (৭খ, ৭গ...) যাবে।
- **যাচাই কীভাবে করা হয়েছে:**
  - প্রতিটা পরিবর্তিত ফাইলে brace `{}`/paren `()` কাউন্ট বেস (আপলোড করা zip)-এর সাথে তুলনা করে
    দেখা হয়েছে শুধু নতুন যোগ হওয়া কোডটুকুই balanced (`SomadhanRepository.kt`-এ paren-imbalance
    edit-এর আগে-পরে অভিন্ন -৯ রয়ে গেছে, যা বেস ফাইলেই প্রি-এক্সিস্টিং কমেন্ট-প্রোজ থেকে আসা,
    নতুন কোডে কোনো নতুন imbalance যোগ হয়নি); `SomadhanApp.kt`/`OutboxSyncTest.kt` উভয়েই
    brace/paren diff ঠিক ০।
  - `diff -u` করে নিশ্চিত করা হয়েছে `SomadhanRepository.kt`-এ ঠিক ৫টা hunk আছে (আগে-থেকে
    পরিকল্পিত ৫টা লোকেশনেই — DAO field, হেল্পার ফাংশন, আর ৩টা fail-ব্লক), অন্য কোনো লাইন
    touch হয়নি।
  - grep দিয়ে cross-check করা হয়েছে: `enqueueOutboxRetry()`-এ পাঠানো প্রতিটা `rpcName`/param-key
    ঠিক `OutboxRpcDispatcher.kt`-এর সংশ্লিষ্ট `when` branch-এর
    `requireString`/`requireDouble` কী-নামের সাথে অক্ষরে-অক্ষরে মিলছে (release_escrow →
    escrowId; refund_escrow_once → escrowId/refundType/refundPercentage;
    increment_escrow_extra_amount → escrowId/amount) — Step 6-এ ফ্ল্যাগ করা "যাচাই না-হওয়া
    অনুমান" ঝুঁকি এখন resolved।
  - `OutboxSyncTest.kt`-এ ৩টা নতুন টেস্ট যোগ হয়েছে (Step 6-এর ৫টার সাথে মোট ৮টা), প্রতিটা: (ক)
    ঠিক একই JsonObject-বানানোর কোড reproduce করে যাচাই করে key-নাম/মান parse করার পর ঠিক আছে
    কিনা, (খ) সেই paramsJson একটা outbox entry হিসেবে DAO দিয়ে insert-get করে raw string
    হুবহু অক্ষত থাকছে কিনা। **⚠️ এই টেস্টগুলো SomadhanRepository/SupabaseSyncManager-এর real
    dual-write ফাংশন actually কল করে না (network/Supabase mock করা হয়নি, Step 6-এর মতোই
    ইচ্ছাকৃতভাবে স্কোপের বাইরে) — শুধু "outbox-এ যে paramsJson লেখা হবে, সেটার শেপ/কী-নাম সঠিক"
    আর "DAO ঠিকভাবে persist করে" এই দুইটা অংশ কভার করে, পুরো fail-থেকে-retry-থেকে-success
    পুরো লুপ কভার করে না।**
  - ফাইল কাউন্ট: শুরু ও শেষে ২৮৭ (অপরিবর্তিত — এই ব্যাচে কোনো নতুন ফাইল যোগ হয়নি, শুধু ৩টা
    বিদ্যমান ফাইলের কনটেন্ট বদলেছে); `comm -23`/`comm -13` উভয়ই খালি।
  - **⚠️ Android Studio/Gradle build চালিয়ে কম্পাইল ভেরিফাই করা হয়নি** (এই সেশনেও বিল্ড
    এনভায়রনমেন্ট নেই, প্রতিটা আগের ধাপের মতোই) — `Result.onFailure { }` ইনলাইন lambda-র ভেতরে
    suspend `enqueueOutboxRetry()` কল করা Kotlin-এ বৈধ (`onFailure` stdlib-এর `inline fun`,
    আর ৩টা কল-সাইটই suspend ফাংশনের ভেতরে), কিন্তু এটা ধরে নেওয়া (assumption), Gradle দিয়ে
    আসলে compile করে দেখা হয়নি।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** **কোনো প্রভাব নেই যতক্ষণ dual-write সফল হয়** (যেটা
  বেশিরভাগ সময় হবে) — Room-এ instant write, UI-তে যা দেখা যায়, সব অপরিবর্তিত। **যখন dual-write
  fail হয় (আগে যা শুধু নীরবে log হতো)**, এখন থেকে একটা অতিরিক্ত outbox entry তৈরি হয় যেটা
  `OutboxSyncWorker` (এখন schedule করা, প্রতি ১৫ মিনিটে চলে) পরে retry করবে — এটা একটা
  নতুন, খাঁটি additive behavior, বিদ্যমান কোনো flow-কে block/change করে না। outbox insert
  নিজেই fail করলেও (try/catch দিয়ে ধরা) caller-এর কাছে কিছুই propagate হয় না।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - Android Studio-তে আসল Gradle build + `OutboxSyncTest.kt`-এর ৮টা টেস্ট রান করে যাচাই করা
    এখনো বাকি — এটাই এখন পর্যন্ত সবচেয়ে বড় open risk (Step 6 থেকেই ক্যারি-ওভার হওয়া, এখনো
    resolve হয়নি)।
  - real device-এ (বা লোকাল Postgres/Supabase টেস্ট প্রজেক্টে) নেটওয়ার্ক বন্ধ রেখে আসলে একবার
    `payoutEscrowToSolver`/`refundEscrowOnceLocked`/`addToEscrow` ট্রিগার করে দেখা এখনো বাকি —
    outbox টেবিলে সত্যিই entry পড়ছে কিনা, আর নেটওয়ার্ক ফিরে এলে (বা পরের periodic রান-এ)
    `OutboxSyncWorker` সেটা সত্যিই সফলভাবে retry করে delete করছে কিনা, এটাই এই পুরো ফিচারের
    আসল end-to-end যাচাই — কোনো session-এই এখনো হয়নি (build/network environment না থাকায়)।
  - Step 7 ব্যাচ ২ (`requestWithdrawal`, `updateWithdrawalStatus`, `adminAdjustBalance`) আর
    ব্যাচ ৩ (KYC/ban/role ফাংশন) মাস্টার প্ল্যান অনুযায়ী পরের ধাপ — প্রতিটা একই প্যাটার্নে
    (শুধু সেই ব্যাচের ফাংশনগুলোই ছোঁয়া হবে)।
  - Step 6-এর নোট করা `MIGRATION_49_50` গ্যাপ আগের মিনি-সেশনেই ফিক্স হয়ে গেছে (উপরের এন্ট্রি
    দেখুন) — এই ধাপে আর কোনো নতুন blocker না।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮৭ (dotfile সহ, অপরিবর্তিত) — এই ধাপে কোনো নতুন ফাইল যোগ
  হয়নি, শুধু ৩টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে।

---

## [Step 7 (ব্যাচ ২) — outbox wire: withdrawal request/reject/complete + admin balance adjustment] — 2026-09-18

- **কী করা হয়েছে:** মাস্টার প্ল্যানের Step 7 ব্যাচ ২ অনুযায়ী ঠিক ৩টা dual-write ফাংশনে outbox
  wire করা হলো — `requestWithdrawal`, `updateWithdrawalStatus` (এর ভেতরে ২টা আলাদা
  `.onFailure` ব্লক আছে: REJECTED ও COMPLETED, দুটোই একই RPC `process_withdrawal`-এর ভিন্ন
  `action`), `adminAdjustBalance` (সব `SomadhanRepository.kt`-তে)। ব্যাচ ১-এর ঠিক একই
  প্যাটার্ন — Step 6-এর `enqueueOutboxRetry()` হেল্পার পুনর্ব্যবহার করা হয়েছে (নতুন কোনো হেল্পার
  লাগেনি), প্রতিটা ফাংশনের happy-path/সিগনেচার অক্ষত, শুধু `.onFailure` ব্লকে outbox-insert কল
  যোগ হয়েছে।
- **কোন ফাইল পরিবর্তন হয়েছে:**
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — ৪টা নতুন additive
    হাংক (ব্যাচ ১-এর ৫টার উপরে, মোট এখন ৯টা — `diff` দিয়ে কনফার্ম করা হয়েছে, অন্য কোনো লাইন
    touch হয়নি):
    (ক) `requestWithdrawal`-এর `.onFailure` ব্লকে `enqueueOutboxRetry("request_withdrawal", ...)`
    (paramsJson: `{"amount","method","accountNumber","bankName"?,"branchName"?,
    "accountHolderName"?,"role"}`);
    (খ) `updateWithdrawalStatus`-এর REJECTED ব্রাঞ্চে `enqueueOutboxRetry("process_withdrawal",
    ...)` (action="REJECT");
    (গ) একই ফাংশনের COMPLETED ব্রাঞ্চে একই RPC, action="COMPLETE";
    (ঘ) `adminAdjustBalance`-এর `.onFailure` ব্লকে
    `enqueueOutboxRetry("admin_adjust_balance", ...)` (paramsJson:
    `{"userId","amount","isAddition","reason","role"?}`)
  - `app/src/test/java/com/example/OutboxSyncTest.kt` — ৩টা নতুন টেস্ট যোগ (মোট এখন ১১টা)।
  - `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)।
  - `SomadhanApp.kt` এই ব্যাচে ছোঁয়া হয়নি (ব্যাচ ১-এই `OutboxSyncWorker.schedulePeriodic()`
    যোগ হয়ে গেছে, নতুন কিছু লাগেনি)।
- **⚠️ গুরুত্বপূর্ণ, ব্যবহারকারীর রিভিউ দরকার — `requestWithdrawal`-এর id-mismatch ঝুঁকি
  (নতুন বাগ না, এটা এই RPC-র already-existing best-effort ডিজাইনের একটা পরিণতি, কোড কমেন্টে
  আগে থেকেই ডকুমেন্টেড ছিল, শুধু এখন outbox retry যোগ হওয়ায় এর প্রভাব একটু বদলেছে):**
  `request_withdrawal` RPC সার্ভার-সাইডে নিজে থেকে withdrawal_id বানায় (client পাঠাতে পারে
  না)। RPC fail হলে local flow-এ locally-generated id দিয়ে এগিয়ে যাওয়া হয় (আগে থেকেই এই
  আচরণ)। এখন এই fail-এর outbox entry পরে retry করে সফল হলে, cloud-এ একটা **নতুন,
  ভিন্ন id-র** withdrawal row তৈরি হবে — local record-এর সাথে link থাকবে না (একটা "এতিম"
  cloud row)। ফলে সেই একই local withdrawal-এর জন্য পরে admin যখন reject/complete করবে
  (`updateWithdrawalStatus`), `process_withdrawal` RPC (এখন এটাও outbox-wired) local id
  দিয়ে cloud-এ খুঁজবে, পাবে না (`WITHDRAWAL_NOT_FOUND`), যেটাও নীরবে fail করে
  outbox-এ retry entry রেখে যাবে, শেষে বার বার fail হয়ে `FAILED_PERMANENT`-এ চলে যাবে।
  **এটা কোনো ক্ষতি করে না** (local status/refund/completion সব ঠিকই হয়, এটা শুধু cloud
  mirror-এর একটা ব্যর্থ প্রচেষ্টা) কিন্তু cloud-এ একটা orphan withdrawal row + কিছু
  ব্যর্থ retry entry জমা হতে পারে। **সম্পূর্ণ সঠিক ফিক্স** (যেমন: client-side deterministic
  id generate করে RPC-কে সেটা accept করানোর জন্য RPC-ই বদলাতে হবে, অথবা পরে
  reconcile করার একটা আলাদা admin টুল বানানো) **এই ব্যাচের স্কোপের বাইরে** — ব্যবহারকারীর
  সিদ্ধান্তের জন্য এখানে স্পষ্টভাবে ফ্ল্যাগ করা হলো। `adminAdjustBalance` আর
  `updateWithdrawalStatus`-এর নিজস্ব কোনো id-mismatch ঝুঁকি নেই (userId/withdrawalId
  সবসময় client-known, RPC নতুন id বানায় না)।
- **যাচাই কীভাবে করা হয়েছে:**
  - `diff -u` (বেস zip বনাম এখনকার): এখন মোট ৯টা hunk, নির্দিষ্ট ৯টা প্রত্যাশিত লোকেশনেই
    (৫টা ব্যাচ ১-এর, ৪টা এই ব্যাচের) — অন্য কোনো লাইন touch হয়নি।
  - brace/paren balance: `SomadhanRepository.kt`-এ brace_diff এখনো ০, paren_diff এখনো ঠিক
    বেস ফাইলের মতোই -৯ (নতুন কোনো imbalance যোগ হয়নি); `OutboxSyncTest.kt`-এ brace/paren
    diff দুটোই ০।
  - grep cross-check: `enqueueOutboxRetry()`-এ পাঠানো প্রতিটা নতুন `rpcName`/param-key
    (request_withdrawal → amount/method/accountNumber/bankName/branchName/
    accountHolderName/role; process_withdrawal → withdrawalId/action/trxId;
    admin_adjust_balance → userId/amount/isAddition/reason/role) `OutboxRpcDispatcher.kt`-এর
    সংশ্লিষ্ট `when` branch-এর `requireString`/`requireDouble`/`requireBoolean` কী-নামের
    সাথে অক্ষরে-অক্ষরে মিলছে।
  - `OutboxSyncTest.kt`-এ ৩টা নতুন টেস্ট (মোট ১১টা) — একই পদ্ধতি: exact param-shape
    reproduce + parse + DAO round-trip যাচাই, real network/Supabase মক করা হয়নি
    (আগের সব ধাপের মতোই এই স্কোপের বাইরে)।
  - ফাইল কাউন্ট: শুরু ও শেষে ২৮৭ (অপরিবর্তিত), `comm -23`/`comm -13` দুটোই খালি।
  - **⚠️ Android Studio/Gradle build চালিয়ে যাচাই করা হয়নি** (প্রতিটা আগের ধাপের মতোই এই
    সেশনেও build environment নেই)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** dual-write সফল হলে (বেশিরভাগ সময়) কোনো প্রভাব
  নেই। fail হলে, আগে যা নীরবে log হতো তার জায়গায় এখন একটা outbox entry তৈরি হয়
  (`admin_adjust_balance`/`process_withdrawal`-এর জন্য নিরাপদে retry হবে; `request_withdrawal`
  retry হলেও উপরে বর্ণিত id-mismatch caveat-সহ) — local balance/status/notification flow-এ
  কোনো পরিবর্তন নেই, সবই আগের মতোই কাজ করে।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - উপরের `requestWithdrawal` id-mismatch ঝুঁকি ব্যবহারকারীর রিভিউ/সিদ্ধান্তের অপেক্ষায় —
    চাইলে ভবিষ্যতে আলাদা ছোট ধাপে ফিক্স করা যাবে (এই RPC_SYNC_FIX ট্র্যাকের বাইরে বা একটা
    নতুন Step হিসেবে)।
  - Gradle build + ১১টা `OutboxSyncTest.kt` টেস্ট আসলে রান করে যাচাই করা এখনো বাকি
    (Step 6 থেকে ক্যারি-ওভার হওয়া open risk, এখনো resolve হয়নি)।
  - Step 7 ব্যাচ ৩ (KYC/ban/role ফাংশন) মাস্টার প্ল্যান অনুযায়ী পরের ধাপ — একই প্যাটার্ন,
    শুধু সেই ব্যাচের নির্দিষ্ট ফাংশনগুলো।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮৭ (dotfile সহ, অপরিবর্তিত) — এই ধাপে কোনো নতুন ফাইল যোগ
  হয়নি, শুধু ৩টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে (SomadhanRepository.kt,
  OutboxSyncTest.kt) + progress note।

## [Step 7 (ব্যাচ ২)-পরবর্তী ফিক্স — requestWithdrawal id-mismatch] — 2026-09-18

- **কী করা হয়েছে:** আগের এন্ট্রিতে ফ্ল্যাগ-করা `requestWithdrawal` id-mismatch ঝুঁকির (outbox
  retry পরে সফল হলে cloud-এ "এতিম" withdrawal row তৈরি হওয়া) সমাধান করা হলো। নতুন migration
  `request_withdrawal` RPC-তে ঐচ্ছিক `p_client_withdrawal_id` প্যারামিটার যোগ করেছে (দেওয়া
  হলে RPC সেই id-ই ব্যবহার করে, না দিলে আগের মতোই server-side random id — pure additive,
  backward-compatible)। Kotlin সাইডে `SomadhanRepository.requestWithdrawal` এখন তার আগে
  থেকে বানানো local `withdrawId` তাৎক্ষণিক RPC কলে *এবং* outbox retry params-এ পাঠায়, তাই
  local ও cloud সবসময় একই id শেয়ার করে (তাৎক্ষণিক সাফল্য বা পরে retry, দুই ক্ষেত্রেই)। আগের
  "cloud-এর ফেরত দেওয়া id দিয়ে local id override করা" লজিকটা সরানো হয়েছে (সেটাই মূল
  mismatch-এর উৎস ছিল) — এখন শুধু অমিল হলে log হয়, override হয় না।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - যোগ: `supabase/migrations/step38_request_withdrawal_client_id.sql`
  - পরিবর্তন: `app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt`
    (`requestWithdrawal()`-এ ঐচ্ছিক `clientWithdrawalId` প্যারামিটার)
  - পরিবর্তন: `app/src/main/java/com/example/data/repository/SomadhanRepository.kt`
    (`requestWithdrawal()`-এ `withdrawId` `var` থেকে `val`, RPC কল ও outbox enqueue দুই
    জায়গায়ই `clientWithdrawalId`/`"clientWithdrawalId"` পাঠানো, override লজিক সরানো)
  - পরিবর্তন: `app/src/main/java/com/example/data/sync/OutboxRpcDispatcher.kt`
    (`request_withdrawal` branch-এ `clientWithdrawalId = params.optionalString("clientWithdrawalId")`)
- **যাচাই কীভাবে করা হয়েছে:**
  - `request_withdrawal` RPC-এর বিদ্যমান লজিক (validation, balance deduction, insert
    statement-গুলো) অপরিবর্তিত রাখা হয়েছে — শুধু id-জেনারেশন লাইন আর নতুন ঐচ্ছিক প্যারামিটার
    যোগ হয়েছে (diff-এ আলাদা করে চেক করা)।
  - brace/paren ব্যালেন্স চেক (Python স্ক্রিপ্ট দিয়ে): তিনটা এডিট করা Kotlin ফাইলের অফসেট
    (imbalance, যেটা বিদ্যমান কমেন্টের কারণে বেসলাইনেও ছিল) এডিটের আগে-পরে অভিন্ন —
    `SomadhanRepository.kt`: parens 5614/5623 (বেসলাইন) → 5606/5615 (৮টা করে সরানো,
    offset -৯ অপরিবর্তিত), braces 1686/1686 → 1685/1685। `SupabaseSyncManager.kt` আর
    `OutboxRpcDispatcher.kt` দুটোই perfectly balanced, আগে আর পরে দুই অবস্থাতেই।
  - `SomadhanRepository.kt`-এ `withdrawId` ব্যবহারের সব জায়গা (৯টা occurrence) manually
    দেখা হয়েছে — `var` থেকে `val`-এ বদলানোর পরেও কোথাও reassignment নেই, তাই কম্পাইল-ব্রেক
    হবে না।
  - zip সম্পূর্ণতা: `comm -23`/`comm -13` — কোনো ফাইল হারায়নি, শুধু ১টা নতুন migration ফাইল
    যোগ হয়েছে (baseline ২৮৭ → ২৮৮)।
  - **⚠️ Android Studio/Gradle build বা লাইভ Postgres-এ migration চালিয়ে যাচাই করা হয়নি**
    (আগের সব ধাপের মতোই এই সেশনেও build/DB environment নেই)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** `p_client_withdrawal_id` DEFAULT NULL, তাই
  এই প্যারামিটার ছাড়া পুরনো কোনো কলার (যদি থাকে) আগের মতোই server-generated random id
  পাবে — কোনো behavior ভাঙে না। এই কোডবেসের একমাত্র কলার (`SomadhanRepository`) এখন থেকে
  সবসময় local id পাঠাবে, তাই ভবিষ্যতে (তাৎক্ষণিক বা outbox retry, দুই পথেই) local আর cloud
  withdrawal id সবসময় মিলবে — আগের "এতিম cloud row" ঝুঁকি দূর হলো। বাকি সব validation/balance/
  notification লজিক অপরিবর্তিত।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - এই migration আসলে Supabase প্রজেক্টে apply করে (এবং একটা RPC fail সিমুলেট করে outbox
    retry manually ট্রিগার করে) end-to-end যাচাই করা এখনো বাকি — কোনো live DB access এই
    সেশনে ছিল না।
  - Gradle build + `OutboxSyncTest.kt` টেস্ট রান করে যাচাই করা এখনো বাকি (আগের এন্ট্রি থেকে
    ক্যারি-ওভার হওয়া open risk, এখনো resolve হয়নি)।
  - Step 7 ব্যাচ ৩ (KYC/ban/role ফাংশন) মাস্টার প্ল্যান অনুযায়ী পরের ধাপ, অপরিবর্তিত আছে।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮৮ (dotfile সহ) — ১টা নতুন migration ফাইল যোগ হয়েছে,
  আগের কোনো ফাইল সরানো হয়নি।

## [migration apply — request_withdrawal id-fix লাইভ Supabase-এ প্রয়োগ] — 2026-09-18

- **কী করা হয়েছে:** `step38_request_withdrawal_client_id.sql` migration-টা লাইভ Supabase
  প্রজেক্টে (`somadhan`, project_id `mghvvpndkxnscwryfkib`) সরাসরি apply করা হলো।
  **একটা গুরুত্বপূর্ণ সমস্যা ধরা পড়েছে এবং সাথে সাথে ঠিক করা হয়েছে:** `CREATE OR REPLACE
  FUNCTION` নতুন argument (`p_client_withdrawal_id`) সহ পুরনো ৭-আর্গুমেন্ট ফাংশনটাকে
  replace করেনি — Postgres argument list-কে ফাংশনের identity-র অংশ ধরে, তাই signature
  বদলানোয় এটা একটা সম্পূর্ণ নতুন, আলাদা **overload** তৈরি করেছিল (পুরনো ৭-আর্গুমেন্ট ভার্সন
  ডাটাবেসে থেকেই গিয়েছিল)। দুইটা `request_withdrawal(...)` ওভারলোড থাকলে PostgREST-এর RPC
  call resolution ambiguous হয়ে যেতে পারতো (client কোন ভার্সন কল করবে অনিশ্চিত, রানটাইমে
  এরর)। এটা ধরার পর একটা দ্বিতীয় migration (`step38b_drop_old_request_withdrawal_overload`)
  দিয়ে পুরনো ৭-আর্গুমেন্ট overload-টা explicit `DROP FUNCTION` করা হয়েছে — শুধু নতুন
  ৮-আর্গুমেন্ট (p_client_withdrawal_id সহ) ভার্সনটাই এখন ডাটাবেসে আছে।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে (এই এন্ট্রিতে):** কোনো নতুন লোকাল ফাইল না — সরাসরি লাইভ DB-তে
  ২টা migration apply (`step38_request_withdrawal_client_id`,
  `step38b_drop_old_request_withdrawal_overload`); দ্বিতীয়টার SQL এই zip-এ কোথাও ফাইল
  আকারে সংরক্ষিত নেই (শুধু এই progress note-এই লেখা আছে) — চাইলে পরের সেশনে
  `supabase/migrations/`-এ একটা corresponding `.sql` ফাইল যোগ করে নেওয়া ভালো, যাতে
  migration history আর লোকাল ফাইলগুলো সিঙ্কে থাকে।
- **যাচাই কীভাবে করা হয়েছে:** apply-এর পর `pg_proc`/`pg_get_function_identity_arguments`
  কোয়েরি করে নিশ্চিত করা হয়েছে যে `request_withdrawal`-এর এখন ঠিক ১টাই overload আছে
  (৮-আর্গুমেন্ট, `p_client_withdrawal_id text` সহ)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** এখন Kotlin অ্যাপ থেকে যেকোনো `requestWithdrawal`
  কল (তাৎক্ষণিক বা outbox retry) নতুন ৮-আর্গুমেন্ট RPC-টাই hit করবে (কোনো ambiguity নেই)।
  `p_client_withdrawal_id` না পাঠালে (কোনো ভবিষ্যৎ কলার) আগের মতোই server-generated random
  id পাবে — কোনো behavior ভাঙেনি।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - `step38b`-এর `DROP FUNCTION` SQL-টা `supabase/migrations/` ফোল্ডারে একটা ফাইল হিসেবেও
    যোগ করে নেওয়া উচিত (এই সেশনে শুধু লাইভ DB-তে apply হয়েছে, লোকাল zip-এ migration ফাইল
    হিসেবে যোগ করা হয়নি) — যাতে future migration replay/fresh-DB setup-এ এই ধাপটা মিস না
    হয়।
  - Android অ্যাপ থেকে একটা প্রকৃত withdrawal request পাঠিয়ে end-to-end যাচাই (RPC সফল হলে
    cloud id = local id) এখনো বাকি — এই সেশনে শুধু SQL-লেভেলে ফাংশন সিগনেচার যাচাই হয়েছে।

## [Step 7 (ব্যাচ ৩) — KYC গ্রুপের প্রথম অংশ: adminApproveKyc/adminRejectKyc/adminRevokeKyc outbox wire] — 2026-09-18

- **কী করা হয়েছে:** মাস্টার প্ল্যানের ৭গ ("KYC/ban/role ফাংশন") ব্যাচ শুরু করা হলো। ঝুঁকি ছোট
  রাখতে (আগের প্রতিটা ব্যাচের মতোই ৩টা ফাংশনের সীমা মেনে) পুরো ৭টা-ফাংশনের KYC/ban/role
  গ্রুপ থেকে শুধু বিশুদ্ধ KYC উপ-গ্রুপ এই ব্যাচে করা হলো: `adminApproveKyc`, `adminRejectKyc`,
  `adminRevokeKyc` (তিনটাই `SomadhanRepository.kt`-তে)। `adminSetBanned`/`adminSetRestricted`/
  `adminChangeRole`/`adminSetVerifiedBadge` (ব্যান/রোল/ব্যাজ উপ-গ্রুপ, ৪টা ফাংশন) ইচ্ছাকৃতভাবে
  এই ব্যাচে বাদ — পরের ব্যাচে (৭ঘ) যাবে।
- **কোন ফাইল পরিবর্তন হয়েছে:**
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — তিনটা ফাংশনের
    `.onFailure` ব্লকে `enqueueOutboxRetry(...)` কল যোগ: (ক) `adminApproveKyc`-এ
    `enqueueOutboxRetry("admin_approve_kyc", {"userId"})`; (খ) `adminRejectKyc`-এ
    `enqueueOutboxRetry("admin_reject_kyc", {"userId","reason"})`; (গ) `adminRevokeKyc`-এ
    `enqueueOutboxRetry("admin_revoke_kyc", {"userId","reason"})`। প্রতিটার বিদ্যমান
    happy-path (local Room update, notification insert, reputation/logAdminAction কল)
    একবিন্দুও বদলানো হয়নি — শুধু `.onFailure` ব্লকে আগের `Log.w(...)`-এর ঠিক পরে নতুন কল
    যোগ হয়েছে।
  - `app/src/main/java/com/example/data/sync/OutboxRpcDispatcher.kt` — ৩টা নতুন `when`
    branch: `"admin_approve_kyc"` → `SupabaseSyncManager.adminApproveKyc(userId)`,
    `"admin_reject_kyc"` → `SupabaseSyncManager.adminRejectKyc(userId, reason)`,
    `"admin_revoke_kyc"` → `SupabaseSyncManager.adminRevokeKyc(userId, reason)`।
  - `app/src/test/java/com/example/OutboxSyncTest.kt` — ৩টা নতুন টেস্ট যোগ (মোট এখন ১৪টা),
    আগের ব্যাচগুলোর মতোই প্যাটার্নে: exact param-shape তৈরি → parse করে key/value যাচাই →
    outbox DAO-তে insert করে round-trip যাচাই।
  - `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)।
  - `SomadhanApp.kt`/worker schedule এই ব্যাচে ছোঁয়া হয়নি (ব্যাচ ১-এই
    `OutboxSyncWorker.schedulePeriodic()` যোগ হয়ে গেছে, নতুন কিছু লাগেনি)।
- **যাচাই কীভাবে করা হয়েছে:**
  - brace/paren ব্যালেন্স (Python স্ক্রিপ্ট দিয়ে, এডিটের আগে ও পরে): `SomadhanRepository.kt` —
    braces 1685/1685 → 1685/1685 (অপরিবর্তিত, perfectly balanced দুই অবস্থাতেই); parens
    5606/5615 (offset −৯, বেসলাইন কমেন্ট-জনিত, আগের ব্যাচগুলোতেও একই) → 5623/5632 (offset
    এখনো ঠিক −৯, নতুন কোনো imbalance যোগ হয়নি)। `OutboxRpcDispatcher.kt` — braces 13/13 →
    16/16, parens 78/78 → 86/86 (দুটোই perfectly balanced, আগে ও পরে)। `OutboxSyncTest.kt` —
    braces 25/25 → 28/28, parens 266/266 → 323/323 (perfectly balanced)।
  - grep cross-check: প্রতিটা নতুন `enqueueOutboxRetry()`-এ পাঠানো `rpcName`/param-key
    (admin_approve_kyc → userId; admin_reject_kyc → userId/reason; admin_revoke_kyc →
    userId/reason) `OutboxRpcDispatcher.kt`-এর সংশ্লিষ্ট branch-এর `requireString` কী-নামের
    সাথে অক্ষরে-অক্ষরে মিলছে; `SupabaseSyncManager.adminApproveKyc/adminRejectKyc/
    adminRevokeKyc`-এর প্যারামিটার নাম/ক্রম (userId, reason) dispatcher-এর কলের সাথেও মিলছে।
  - প্রতিটা এডিট করা ফাংশনে happy-path-এর কোনো লাইন (local update/notification/reputation/
    logAdminAction) স্পর্শ করা হয়নি তা ম্যানুয়ালি diff দেখে নিশ্চিত করা হয়েছে — শুধু
    `.onFailure` ব্লকের ভেতরে নতুন কোড যোগ, `Log.w(...)` লাইনও অপরিবর্তিত রাখা হয়েছে।
  - ফাইল কাউন্ট: শুরু ও শেষে ২৮৯ (অপরিবর্তিত, dotfile সহ) — এই ব্যাচে কোনো নতুন ফাইল যোগ
    হয়নি, শুধু ৩টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে + progress note।
  - **⚠️ Android Studio/Gradle build বা `OutboxSyncTest.kt`-এর ১৪টা টেস্ট আসলে রান করে
    যাচাই করা হয়নি** (আগের প্রতিটা ধাপের মতোই এই সেশনেও build environment নেই — carry-over
    open risk, নিচে আবার নোট করা হলো)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** dual-write সফল হলে (বেশিরভাগ সময়) কোনো প্রভাব
  নেই। fail হলে, আগে যা নীরবে log হতো তার জায়গায় এখন একটা outbox entry তৈরি হয় (তিনটার
  কোনোটারই id-mismatch বা অন্য কোনো caveat নেই — `userId`/`reason` সবসময় client-known,
  server নতুন কোনো id বানায় না, requestWithdrawal-এর মতো ঝুঁকি নেই)। local
  KYC status/notification/reputation flow-এ কোনো পরিবর্তন নেই।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - Step 7 ব্যাচ ৪ (৭ঘ) — বাকি ৪টা ফাংশন: `adminSetBanned`, `adminSetRestricted`,
    `adminChangeRole`, `adminSetVerifiedBadge` — একই প্যাটার্নে outbox wire করা বাকি।
  - Gradle build + `OutboxSyncTest.kt`-এর ১৪টা টেস্ট রান করে যাচাই করা এখনো বাকি (আগের
    এন্ট্রি থেকে ক্যারি-ওভার হওয়া open risk, এখনো resolve হয়নি)।
  - `requestWithdrawal` id-mismatch ফিক্স (আগের এন্ট্রিতে বর্ণিত) লাইভ DB-তে apply হয়ে
    গেছে; `step38b`-এর `DROP FUNCTION` SQL এখনো `supabase/migrations/`-এ ফাইল হিসেবে যোগ
    করা বাকি (আগের এন্ট্রি থেকে carry-over)।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮৯ (dotfile সহ, অপরিবর্তিত) — এই ব্যাচে কোনো নতুন ফাইল
  যোগ হয়নি, শুধু ৩টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে (SomadhanRepository.kt,
  OutboxRpcDispatcher.kt, OutboxSyncTest.kt) + progress note।

## [Step 7 (ব্যাচ ৪ / ৭ঘ) — ব্যান/রোল/ব্যাজ গ্রুপ: adminSetBanned/adminSetRestricted/adminSetVerifiedBadge/adminChangeRole outbox wire] — 2026-09-18

- **কী করা হয়েছে:** মাস্টার প্ল্যানের ৭গ ব্যাচের বাকি অংশ (KYC/ban/role ৭-ফাংশন গ্রুপের শেষ
  ৪টা) এই ব্যাচে (৭ঘ) সম্পন্ন হলো: `adminSetBanned`, `adminSetRestricted`,
  `adminSetVerifiedBadge`, `adminChangeRole` (সব `SomadhanRepository.kt`-তে)। এর মাধ্যমে
  মাস্টার প্ল্যানের ৭গ-এ তালিকাভুক্ত পুরো ৭টা ফাংশনই (৩টা KYC + এই ৪টা) এখন outbox-wired —
  Step 5-এর প্রায়োরিটি লিস্টের টাকা-সংক্রান্ত + অ্যাডমিন-মডারেশন গ্রুপের মূল অংশ সম্পন্ন।
- **কোন ফাইল পরিবর্তন হয়েছে:**
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — চারটা
    ফাংশনের `.onFailure` ব্লকে `enqueueOutboxRetry(...)` কল যোগ: (ক) `adminSetBanned`-এ
    `enqueueOutboxRetry("admin_set_banned", {"userId","banned","role"?})`; (খ)
    `adminSetRestricted`-এ `enqueueOutboxRetry("admin_set_restricted",
    {"userId","restricted","role"?})`; (গ) `adminSetVerifiedBadge`-এ
    `enqueueOutboxRetry("admin_set_verified_badge", {"userId","verified","role"?})`; (ঘ)
    `adminChangeRole`-এ `enqueueOutboxRetry("admin_change_role", {"userId","newRole"})`।
    প্রতিটার বিদ্যমান happy-path (local update, reputation event, notification, logAdminAction)
    অক্ষত — শুধু `.onFailure` ব্লকের ভেতরে, আগের `Log.w(...)`-এর পরে নতুন কল যোগ।
  - `app/src/main/java/com/example/data/sync/OutboxRpcDispatcher.kt` — ৪টা নতুন `when`
    branch। নোট: `admin_set_verified_badge`-এর জন্য `SupabaseSyncManager.adminSetVerifiedBadge()`
    রিটার্ন করে `Result<Unit>` (অন্যগুলোর মতো `Result<JsonElement>` না) — তাই
    `.map { JsonPrimitive("OK") as JsonElement }` দিয়ে `callRpcByName()`-এর কমন রিটার্ন
    টাইপে ম্যাপ করা হয়েছে (worker শুধু success/failure দেখে, ভেতরের মান ব্যবহার করে না)।
  - `app/src/test/java/com/example/OutboxSyncTest.kt` — ৪টা নতুন টেস্ট যোগ (মোট এখন ১৮টা),
    আগের ব্যাচগুলোর মতোই প্যাটার্নে। `admin_set_verified_badge`-এর টেস্টে ইচ্ছাকৃতভাবে
    ঐচ্ছিক `role` বাদ দিয়ে (`assertNull(parsed["role"])`) null-role কেসও কভার করা হয়েছে।
  - `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)।
- **যাচাই কীভাবে করা হয়েছে:**
  - brace/paren ব্যালেন্স (এডিটের আগে ও পরে): `SomadhanRepository.kt` — braces 1685/1685 →
    1691/1691 (দুই অবস্থাতেই perfectly balanced); parens 5623/5632 (offset −৯) →
    5656/5665 (offset এখনো −৯, নতুন imbalance নেই)। `OutboxRpcDispatcher.kt` — braces
    16/16 → 21/21, parens 86/86 → 107/107 (সব balanced)। `OutboxSyncTest.kt` — braces
    28/28 → 32/32, parens 323/323 → 414/414 (সব balanced)।
  - grep cross-check: প্রতিটা নতুন `enqueueOutboxRetry()` কলের `rpcName`/param-key
    (admin_set_banned → userId/banned/role?; admin_set_restricted →
    userId/restricted/role?; admin_set_verified_badge → userId/verified/role?;
    admin_change_role → userId/newRole) dispatcher-এর সংশ্লিষ্ট branch-এর
    `requireString`/`requireBoolean`/`optionalString` কী-নামের সাথে অক্ষরে-অক্ষরে মিলছে।
    `SupabaseSyncManager`-এর চারটা ফাংশনের প্যারামিটার নাম/ক্রমও dispatcher-এর কলের সাথে
    মিলছে।
  - প্রতিটা এডিট করা ফাংশনে happy-path-এর কোনো লাইন (local update/reputation/
    notification/logAdminAction) স্পর্শ করা হয়নি — শুধু `.onFailure` ব্লকের ভেতরে নতুন কোড,
    `Log.w(...)` লাইনও অপরিবর্তিত।
  - ফাইল কাউন্ট: শুরু ও শেষে ২৮৯ (অপরিবর্তিত) — `comm -23`/`comm -13` দুটোই খালি, এই ব্যাচে
    কোনো নতুন ফাইল যোগ হয়নি, শুধু ৩টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে।
  - **⚠️ Android Studio/Gradle build বা `OutboxSyncTest.kt`-এর ১৮টা টেস্ট আসলে রান করে
    যাচাই করা হয়নি** (আগের প্রতিটা ধাপের মতোই এই সেশনেও build environment নেই — carry-over
    open risk, এখনো resolve হয়নি)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** dual-write সফল হলে কোনো প্রভাব নেই। fail হলে,
  আগে যা নীরবে log হতো তার জায়গায় এখন একটা outbox entry তৈরি হয় — চারটার কোনোটারই
  id-mismatch ঝুঁকি নেই (`userId`/`newRole`/boolean flag/`role` সবসময় client-known, server
  নতুন কোনো id বানায় না)। local ban/restrict/badge/role-change flow-এ কোনো পরিবর্তন নেই।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - মাস্টার প্ল্যানের ৭গ (KYC/ban/role, ৭টা ফাংশন) এখন সম্পূর্ণ outbox-wired। এর সাথে
    আগের ব্যাচ ১/২ মিলিয়ে মোট ১৩টা dual-write ফাংশন outbox-wired: escrow (৩টা,
    ব্যাচ ১), withdrawal/balance-adjust (৩টা, ব্যাচ ২/৭খ), KYC/ban/role (৭টা, ব্যাচ ৩+৪/৭গ)।
  - Step 5-এর মূল প্রায়োরিটি লিস্টের বাকি dual-write ফাংশনগুলো (non-priority, ~৮২টার
    মধ্যে এখনো cover না-হওয়া অংশ) ভবিষ্যতে ধীরে ধীরে যোগ হবে যদি ইউজার চায় — মাস্টার
    প্ল্যানের ব্যাকলগ সেকশন দেখুন।
  - Gradle build + `OutboxSyncTest.kt`-এর ১৮টা টেস্ট রান করে যাচাই করা এখনো বাকি (আগের
    এন্ট্রি থেকে ক্যারি-ওভার হওয়া open risk, এখনো resolve হয়নি)।
  - `requestWithdrawal` id-mismatch ফিক্স লাইভ DB-তে apply হয়ে গেছে; `step38b`-এর
    `DROP FUNCTION` SQL এখনো `supabase/migrations/`-এ ফাইল হিসেবে যোগ করা বাকি
    (আগের এন্ট্রি থেকে carry-over)।
  - মাস্টার প্ল্যানের ধাপ ৮ (UI-তে pending/failed sync দেখানো, wallet/withdrawal স্ক্রিনে)
    এখন যেকোনো সময় শুরু করা যায় — Step 7-এর সব ব্যাচ এখন code-complete।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৮৯ (dotfile সহ, অপরিবর্তিত) — এই ব্যাচে কোনো নতুন ফাইল
  যোগ হয়নি, শুধু ৩টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে (SomadhanRepository.kt,
  OutboxRpcDispatcher.kt, OutboxSyncTest.kt) + progress note।

## [Step 8 — Wallet/Withdrawal স্ক্রিনে pending/failed sync UI ইন্ডিকেটর] — 2026-09-18

- **কী করা হয়েছে:** মাস্টার প্ল্যানের ধাপ ৮ সম্পন্ন হলো। Step 7-এ outbox-wired হওয়া
  escrow/withdrawal/balance-adjust/KYC/ban/role ফাংশনগুলোর জন্য এখন ইউজার ও অ্যাডমিন উভয়
  সাইডে একটা ছোট, non-blocking "cloud sync পেন্ডিং" ইন্ডিকেটর দেখানো হয় (আগে সম্পূর্ণ নীরব
  ছিল)। outbox খালি থাকলে ইন্ডিকেটর কিছুই রেন্ডার করে না (`AnimatedVisibility(visible =
  pendingCount > 0, ...)`)। শুধু read (pending count observe) + একটা ম্যানুয়াল-ট্রিগার কল
  যোগ হয়েছে — outbox-এর ভেতরের ডেটা এই ধাপে কোথাও বদলানো হয়নি (retry/status-update এখনো শুধু
  `OutboxSyncWorker.doWork()`-এর দায়িত্ব)।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:**
  - `app/src/main/java/com/example/ui/components/OutboxPendingIndicator.kt` (নতুন) —
    reusable `@Composable` — amber card, "কিছু লেনদেন (Nটি) cloud-এ sync হতে বাকি আছে,
    নেটওয়ার্ক এলে স্বয়ংক্রিয়ভাবে হবে" টেক্সট + "এখনই চেষ্টা করুন" `TextButton`।
    `pendingCount: Int`, `onRetryClick: () -> Unit`, `modifier: Modifier` প্যারামিটার নেয়।
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — নতুন public
    read-only ফাংশন `fun observeOutboxPendingCount(): Flow<Int> =
    pendingSyncOutboxDao.observePendingCount()` (Step 6-এর DAO Flow সরাসরি এক্সপোজ,
    বিদ্যমান কোনো ফাংশন/লজিক ছোঁয়া হয়নি)।
  - `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt` — দুটো নতুন সদস্য যোগ:
    `val outboxPendingCount: StateFlow<Int>` (বিদ্যমান `allCategories`/`allPlatformSettings`-এর
    মতোই `repository.observeOutboxPendingCount().stateIn(...)` কনভেনশনে) এবং `fun
    retryOutboxSyncNow()` (শুধু `OutboxSyncWorker.triggerImmediate(getApplication())` কল
    করে, Step 6-এ রেডি করা ফাংশন — এই ধাপে প্রথমবার আসলে কল হলো)।
  - `app/src/main/java/com/example/ui/screens/UserWalletScreen.kt` — ব্যালেন্স কার্ডের ঠিক
    নিচে, এসক্রো সেকশনের আগে `OutboxPendingIndicator` বসানো হয়েছে + `outboxPendingCount`
    state আর import যোগ। বিদ্যমান কোনো লেআউট/কম্পোনেন্ট সরানো হয়নি, শুধু নতুন একটা optional
    (conditionally-visible) সেকশন যোগ হয়েছে।
  - `app/src/main/java/com/example/ui/screens/WithdrawalHistoryScreen.kt` — `LazyColumn`-এর
    প্রথম `item {}` ব্লকে, সামারি স্ট্যাটস কার্ডের ঠিক আগে ইন্ডিকেটর বসানো হয়েছে।
  - `app/src/main/java/com/example/ui/screens/AdminWithdrawalsView.kt` — হেডার কার্ডের ঠিক
    নিচে, "Select Mode Action Bar" সেকশনের আগে ইন্ডিকেটর বসানো হয়েছে। এই ফাইলে `viewModel`
    ঐচ্ছিক (`SomadhanViewModel?`) — বিদ্যমান `recentlyChangedWithdrawalIds`-এর একই null-safe
    fallback প্যাটার্ন অনুসরণ করে `outboxPendingCount` ডিফল্ট `0`, আর `onRetryClick`-এ
    `viewModel?.retryOutboxSyncNow()`।
  - `RPC_SYNC_FIX_PROGRESS.md` (এই এন্ট্রি)।
- **যাচাই কীভাবে করা হয়েছে:**
  - brace/paren ব্যালেন্স (এডিটের আগে ও পরে, Python স্ক্রিপ্ট): `SomadhanRepository.kt` —
    braces 1691/1691 → 1691/1691 (অপরিবর্তিত); parens 5656/5665 (offset −৯, বেসলাইন,
    আগের ব্যাচগুলোতেও একই) → 5663/5672 (offset এখনো −৯, নতুন imbalance নেই)।
    `SomadhanViewModel.kt` — braces 1330/1330 → 1331/1331, parens 2853/2853 → 2865/2865
    (সব perfectly balanced)। `UserWalletScreen.kt` — braces 410/410 → 411/411, parens
    1454/1454 → 1462/1462 (balanced)। `WithdrawalHistoryScreen.kt` — braces 52/52 →
    53/53, parens 145/145 → 153/153 (balanced)। `AdminWithdrawalsView.kt` — braces
    305/305 → 307/307, parens 782/782 → 792/792 (balanced)। নতুন
    `OutboxPendingIndicator.kt` নিজেও balanced (braces 7/7, parens 39/39)।
  - grep cross-check: তিনটা call site-এর (`UserWalletScreen.kt`,
    `WithdrawalHistoryScreen.kt`, `AdminWithdrawalsView.kt`) `OutboxPendingIndicator(...)`
    কলের `pendingCount`/`onRetryClick` প্যারামিটার-নাম কম্পোজেবলের সিগনেচারের সাথে অক্ষরে-অক্ষরে
    মিলছে; `viewModel.outboxPendingCount`/`viewModel.retryOutboxSyncNow()` তিন জায়গাতেই
    ViewModel-এ ডিফাইন করা নাম-এর সাথে মিলছে (AdminWithdrawalsView-তে ঐচ্ছিক `?.`)।
  - কোনো বিদ্যমান স্ক্রিন লেআউট/নেভিগেশন/কম্পোনেন্ট মুছে ফেলা বা রি-অর্ডার করা হয়নি — শুধু নতুন
    সেকশন বসানো হয়েছে, ম্যানুয়ালি diff দেখে নিশ্চিত করা হয়েছে।
  - outbox DAO-এর দিক থেকে: `observeOutboxPendingCount()`/`retryOutboxSyncNow()` কোনোটাই
    `pending_sync_outbox` টেবিলে insert/update/delete করে না — শুধু read
    (`observePendingCount()`) আর WorkManager-কে trigger (existing `triggerImmediate()`)।
  - ফাইল কাউন্ট: শুরুতে ২৮৯ → শেষে ২৯০ (`comm -23` খালি — কিছু হারায়নি; `comm -13`-এ শুধু
    নতুন `OutboxPendingIndicator.kt` দেখাচ্ছে, ইচ্ছাকৃত)।
  - **⚠️ Android Studio/Gradle build বা Compose Preview আসলে রান করে যাচাই করা হয়নি**
    (আগের প্রতিটা ধাপের মতোই এই সেশনেও build environment নেই — carry-over open risk,
    এখনো resolve হয়নি)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** outbox খালি থাকলে (বেশিরভাগ ব্যবহারকারীর জন্য
  সবসময়) UI-তে দৃশ্যমান কোনো পরিবর্তন নেই — ইন্ডিকেটর সম্পূর্ণ অদৃশ্য থাকে, স্ক্রিনের বাকি অংশে
  কোনো প্রভাব ফেলে না (স্পেস/height নেয় না)। outbox-এ pending entry থাকলে (dual-write fail
  হওয়ার পর) এখন প্রথমবারের মতো একটা দৃশ্যমান ইঙ্গিত দেখা যাবে — এটা ইচ্ছাকৃত নতুন আচরণ
  (আগে সম্পূর্ণ নীরব ছিল), কোনো ব্লকিং অ্যাকশন বাধ্য করে না।
- **পরের ধাপের জন্য নোট / ব্লকার:**
  - এই ধাপ ইচ্ছাকৃতভাবে শুধু "wallet/withdrawal" স্কোপে সীমাবদ্ধ রাখা হয়েছে (মাস্টার প্ল্যানের
    ধাপ ৮ শিরোনাম অনুযায়ী) — `AdminUsersView.kt`/`AdminUserLookupView.kt` (যেখানে
    `adminAdjustBalance` কল হয়) আর `AdminKycView.kt`-সদৃশ KYC/ban/role অ্যাডমিন স্ক্রিন
    (যেখানে `adminApproveKyc`/`adminSetBanned` ইত্যাদি কল হয়) স্পর্শ করা হয়নি — এগুলোতে
    এখনো কোনো outbox ইন্ডিকেটর নেই। ইউজার চাইলে ভবিষ্যতে একটা ছোট অতিরিক্ত ব্যাচে (একই
    `OutboxPendingIndicator` কম্পোজেবল পুনর্ব্যবহার করে) এগুলোতেও যোগ করা যাবে।
  - Gradle build + `OutboxSyncTest.kt`-এর ১৮টা টেস্ট রান করে যাচাই করা এখনো বাকি (আগের
    প্রতিটা এন্ট্রি থেকে ক্যারি-ওভার হওয়া open risk, এখনো resolve হয়নি)।
  - `step38b`-এর `DROP FUNCTION` SQL এখনো `supabase/migrations/`-এ ফাইল হিসেবে যোগ করা
    বাকি (আগের এন্ট্রি থেকে carry-over)।
  - এই ইন্ডিকেটর শুধু outbox-এর সামগ্রিক pending count দেখায় (কোন নির্দিষ্ট withdrawal/escrow
    entry outbox-এ আছে তা row-level দেখায় না) — future ধাপে চাইলে per-item ইন্ডিকেটর
    (rpcName/paramsJson পার্স করে সংশ্লিষ্ট withdrawal/escrow row-এর সাথে ম্যাচ করে) আরও
    granular করা যেতে পারে, কিন্তু মাস্টার প্ল্যানের ধাপ ৮-এর বর্ণনা অনুযায়ী এই ধাপে
    সেটা দরকার ছিল না।
  - মাস্টার প্ল্যানের মূল ৩-ধাপের সিকোয়েন্স (Step 1-8) এখন সম্পূর্ণ। বাকি কাজ ব্যাকলগ সেকশনে
    তালিকাভুক্ত (Step 4.5b, বাকি non-money dual-write ফাংশন, migration dry-run রিভিউ,
    flat balance deprecation)।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৯০ (dotfile সহ) — এই ব্যাচে ১টা নতুন ফাইল যোগ হয়েছে
  (`OutboxPendingIndicator.kt`), ৫টা বিদ্যমান ফাইলের কনটেন্ট বদলেছে (`SomadhanRepository.kt`,
  `SomadhanViewModel.kt`, `UserWalletScreen.kt`, `WithdrawalHistoryScreen.kt`,
  `AdminWithdrawalsView.kt` — মোট ৫টা) + progress note।

## [সংশোধনী — step38b migration ফাইল আসলে আগে থেকেই বিদ্যমান, stale "বাকি" নোট ঠিক করা হলো] — 2026-09-18

- **কী করা হয়েছে:** আগের কয়েকটা এন্ট্রিতে (Step 7 ব্যাচ ২/৭খ, ব্যাচ ৩, ব্যাচ ৪, আর Step 8)
  বারবার নোট করা হয়েছিল যে `step38b`-এর `DROP FUNCTION` SQL এখনো `supabase/migrations/`-এ
  ফাইল হিসেবে যোগ করা বাকি — কিন্তু আসলে **এই zip-এ ফাইলটা ইতিমধ্যেই বিদ্যমান**
  (`supabase/migrations/step38b_drop_old_request_withdrawal_overload.sql`)। এই সেশনে
  যাচাই করে নিশ্চিত হওয়া গেছে যে ফাইলটার `DROP FUNCTION IF EXISTS
  public.request_withdrawal(numeric, text, text, text, text, text, text)` স্টেটমেন্টের
  ৭-আর্গুমেন্ট সিগনেচার `step38_request_withdrawal_client_id.sql`-এর নতুন ৮-আর্গুমেন্ট
  `CREATE OR REPLACE FUNCTION`-এর প্যারামিটার লিস্ট (p_client_withdrawal_id বাদ দিয়ে বাকি
  ৭টা) থেকে অক্ষরে-অক্ষরে মিলছে — অর্থাৎ ফাইলটা সঠিক, শুধু পুরনো ৭-আর্গুমেন্ট overload-টাই drop
  করছে, নতুন ৮-আর্গুমেন্ট ভার্সন অক্ষত থাকে। ঠিক কোন সেশনে এই ফাইলটা যোগ হয়েছিল তা এই
  progress note-এ স্পষ্ট করে লেখা নেই (সম্ভবত কোনো একটা ব্যাচের ফাইল-এডিটে যোগ হয়েছিল কিন্তু
  সেই এন্ট্রিতে আলাদা করে উল্লেখ করা হয়নি) — কিন্তু ফাইলটা এখন repo-তে আছে এটাই গুরুত্বপূর্ণ।
- **কোন ফাইল যোগ/পরিবর্তন হয়েছে:** `RPC_SYNC_FIX_PROGRESS.md` (এই সংশোধনী এন্ট্রি) ছাড়া কিছু
  না — কোনো কোড/SQL/migration ফাইল এই এন্ট্রিতে বদলায়নি বা নতুন যোগ হয়নি।
- **যাচাই কীভাবে করা হয়েছে:** `supabase/migrations/step38b_drop_old_request_withdrawal_overload.sql`
  আর `supabase/migrations/step38_request_withdrawal_client_id.sql` দুটোই সরাসরি পড়ে
  পাশাপাশি তুলনা করে প্যারামিটার-লিস্ট মিল যাচাই করা হয়েছে (ম্যানুয়ালি, নতুন কোনো live-DB
  কোয়েরি এই সেশনে চালানো হয়নি — শুধু লোকাল ফাইল-লেভেল যাচাই)।
- **অ্যাপের existing ফাংশনালিটিতে প্রভাব:** কোনো প্রভাব নেই — শুধু ডকুমেন্টেশন সংশোধন, কোনো
  কোড/SQL/লজিক বদলায়নি।
- **পরের ধাপের জন্য নোট / ব্লকার:** এখন থেকে `step38b`-এর "ফাইল যোগ করা বাকি" নোটটা আর
  ব্যালিড না — ভবিষ্যতের কোনো সেশন এই আইটেমটা carry-over ব্লকার হিসেবে দেখলে, এই সংশোধনী
  এন্ট্রিটা দেখে নিশ্চিত হয়ে নেবে যে এটা আসলে resolved, পুরনো এন্ট্রিগুলোর নোট stale ছিল
  (progress note-এর আগের এন্ট্রিগুলো edit/delete করা হয়নি, শুধু এই নতুন সংশোধনী যোগ হলো —
  কমন রুল অনুযায়ী পুরনো এন্ট্রি কখনো মোছা/এডিট করা হয় না)। Gradle build + `OutboxSyncTest.kt`
  টেস্ট রান করে যাচাই করা এখনো বাকি (অপরিবর্তিত, আগের এন্ট্রি থেকে carry-over)।
- **zip ফাইলে মোট ফাইল সংখ্যা:** ২৯০ (dotfile সহ, অপরিবর্তিত) — এই এন্ট্রিতে কোনো ফাইল
  যোগ/মুছে যায়নি, শুধু progress note-এর কনটেন্ট বেড়েছে।
