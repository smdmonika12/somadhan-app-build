# RPC Inventory Report — Step 1

_তৈরি হয়েছে: 2026-09-17 — Somadhan RPC/Dual-Write Fix Project, Step 1_

## সারসংক্ষেপ

- Kotlin কোড থেকে কল হওয়া মোট ইউনিক RPC: **91**
- `supabase/migrations/*.sql`-এ `CREATE FUNCTION` দিয়ে ডিফাইন করা মোট RPC: **40**
- migration ফাইলে কোনো সংজ্ঞা নেই এমন RPC (মিসিং): **64**
- এর মধ্যে সরাসরি টাকা-সংক্রান্ত (escrow/withdrawal/deposit/balance/charge): **14**

## স্ক্যান পদ্ধতি (reproducibility-এর জন্য)

1. `app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt`-এ প্রতিটা
   `client.postgrest.rpc(` কলের পরের লাইনে RPC নামের স্ট্রিং literal বের করা হয়েছে
   (regex দিয়ে, ম্যানুয়ালি লাইন বাদ না দিয়ে পুরো ফাইল স্ক্যান করে)।
2. `supabase/migrations/*.sql`-এর সব ফাইলে `CREATE FUNCTION` / `CREATE OR REPLACE FUNCTION
   public.<name>` প্যাটার্ন থেকে ডিফাইন করা ফাংশনের নাম বের করা হয়েছে।
3. `public.` prefix ছাড়া কোনো ফাংশন সংজ্ঞা আছে কিনা আলাদাভাবে যাচাই করা হয়েছে (পাওয়া যায়নি)।
4. দুই লিস্ট `comm -23` দিয়ে ডিফ করা হয়েছে — যা Kotlin-এ কল হয় কিন্তু migration ফাইলে নেই।
5. প্রতিটা মিসিং RPC কোন Kotlin wrapper ফাংশন থেকে কল হয়, তা সোর্স স্ক্যান করে ম্যাপ করা হয়েছে
   (নিচের টেবিলে "Kotlin caller" কলামে)।

## মিসিং RPC-এর সম্পূর্ণ তালিকা

| # | RPC নাম | টাকা-সংক্রান্ত? | Kotlin caller (SupabaseSyncManager.kt) |
|---|---|---|---|
| 1 | `accept_bid` | 💰 হ্যাঁ | `acceptBid` |
| 2 | `accept_direct_contract` | না | `acceptDirectContract` |
| 3 | `admin_adjust_balance` | 💰 হ্যাঁ | `adminAdjustBalance` |
| 4 | `admin_approve_kyc` | না | `adminApproveKyc` |
| 5 | `admin_broadcast_notification` | না | `adminBroadcastNotification` |
| 6 | `admin_change_role` | না | `adminChangeRole` |
| 7 | `admin_confirm_gateway_deposit` | 💰 হ্যাঁ | `adminConfirmGatewayDeposit` |
| 8 | `admin_credentials_get_phone` | না | `getAdminPhoneSecure` |
| 9 | `admin_credentials_update` | না | `updateAdminCredentialsSecure` |
| 10 | `admin_credentials_verify_password` | না | `verifyAdminPasswordSecure` |
| 11 | `admin_delete_message` | না | `adminDeleteMessage` |
| 12 | `admin_delete_notification_group` | না | `adminDeleteNotificationGroup` |
| 13 | `admin_delete_rating` | না | `adminDeleteRating` |
| 14 | `admin_force_cancel_instant_job` | না | `adminForceCancelInstantJob` |
| 15 | `admin_manually_flag_dispute` | না | `adminManuallyFlagDispute` |
| 16 | `admin_notify_user` | না | `adminNotifyUser` |
| 17 | `admin_reassign_solver` | না | `adminReassignSolver` |
| 18 | `admin_refund_and_reopen_problem` | 💰 হ্যাঁ | `adminRefundAndReopenProblem` |
| 19 | `admin_reject_kyc` | না | `adminRejectKyc` |
| 20 | `admin_remove_category_from_solvers` | না | `adminRemoveCategoryFromSolvers` |
| 21 | `admin_revoke_kyc` | না | `adminRevokeKyc` |
| 22 | `admin_send_message_to_problem_chat` | না | `adminSendMessageToProblemChat` |
| 23 | `admin_set_banned` | না | `adminSetBanned` |
| 24 | `admin_set_restricted` | না | `adminSetRestricted` |
| 25 | `admin_set_verified_badge` | না | `adminSetVerifiedBadge` |
| 26 | `admin_update_direct_contract_status` | না | `adminUpdateDirectContractStatus` |
| 27 | `admin_update_problem_budget` | না | `adminUpdateProblemBudget` |
| 28 | `admin_update_problem_status` | না | `adminUpdateProblemStatus` |
| 29 | `broadcast_instant_job` | না | `broadcastInstantJob` |
| 30 | `cancel_bid` | না | `cancelBid` |
| 31 | `cancel_instant_job` | না | `cancelInstantJob` |
| 32 | `cancel_job_release_request` | না | `cancelJobReleaseRequest` |
| 33 | `create_notification` | না | `createNotification` |
| 34 | `decline_direct_contract` | না | `declineDirectContract` |
| 35 | `deposit_money_via_gateway` | 💰 হ্যাঁ | `depositMoneyViaGateway` |
| 36 | `expire_broadcasting_instant_job` | না | `expireBroadcastingInstantJob` |
| 37 | `log_admin_action` | না | `logAdminAction` |
| 38 | `mark_job_started` | না | `markJobStarted` |
| 39 | `mark_solver_arrived` | না | `markSolverArrived` |
| 40 | `mark_solver_on_way` | না | `markSolverOnWay` |
| 41 | `notify_admins` | না | `notifyAdmins` |
| 42 | `process_withdrawal` | 💰 হ্যাঁ | `processWithdrawal` |
| 43 | `raise_dispute` | না | `raiseDispute` |
| 44 | `refund_escrow_once` | 💰 হ্যাঁ | `refundEscrow` |
| 45 | `reject_bid` | না | `rejectBid` |
| 46 | `reject_job_release_request` | না | `rejectJobReleaseRequest` |
| 47 | `release_escrow` | 💰 হ্যাঁ | `releaseEscrow` |
| 48 | `request_admin_assistance` | না | `requestAdminAssistance` |
| 49 | `request_extra_amount` | 💰 হ্যাঁ | `requestExtraAmount` |
| 50 | `request_job_release` | না | `requestJobRelease` |
| 51 | `request_wallet_deposit` | 💰 হ্যাঁ | `requestWalletDeposit` |
| 52 | `request_withdrawal` | 💰 হ্যাঁ | `requestWithdrawal` |
| 53 | `resolve_dispute` | না | `resolveDispute` |
| 54 | `respond_additional_charge` | 💰 হ্যাঁ | `respondToAdditionalCharge` |
| 55 | `settle_dispute` | না | `settleDispute` |
| 56 | `solver_cancel_job` | না | `solverCancelJob` |
| 57 | `submit_kyc` | না | `submitKyc` |
| 58 | `submit_rating` | না | `submitRating` |
| 59 | `submit_reputation_event` | না | `submitReputationEvent` |
| 60 | `switch_role_get_or_create_linked_profile` | না | `switchRole` |
| 61 | `system_notify_48hour_auto_release` | না | `systemNotify48HourAutoRelease` |
| 62 | `user_confirm_extra_amount` | 💰 হ্যাঁ | `userConfirmExtraAmount` |
| 63 | `user_reject_extra_amount` | 💰 হ্যাঁ | `userRejectExtraAmount` |
| 64 | `withdraw_dispute` | না | `withdrawDispute` |

## টাকা-সংক্রান্ত RPC (আলাদা করে ফ্ল্যাগ করা — সর্বোচ্চ অগ্রাধিকার)

- `accept_bid` → Kotlin caller: `acceptBid`
- `admin_adjust_balance` → Kotlin caller: `adminAdjustBalance`
- `admin_confirm_gateway_deposit` → Kotlin caller: `adminConfirmGatewayDeposit`
- `admin_refund_and_reopen_problem` → Kotlin caller: `adminRefundAndReopenProblem`
- `deposit_money_via_gateway` → Kotlin caller: `depositMoneyViaGateway`
- `process_withdrawal` → Kotlin caller: `processWithdrawal`
- `refund_escrow_once` → Kotlin caller: `refundEscrow`
- `release_escrow` → Kotlin caller: `releaseEscrow`
- `request_extra_amount` → Kotlin caller: `requestExtraAmount`
- `request_wallet_deposit` → Kotlin caller: `requestWalletDeposit`
- `request_withdrawal` → Kotlin caller: `requestWithdrawal`
- `respond_additional_charge` → Kotlin caller: `respondToAdditionalCharge`
- `user_confirm_extra_amount` → Kotlin caller: `userConfirmExtraAmount`
- `user_reject_extra_amount` → Kotlin caller: `userRejectExtraAmount`

## ⚠️ বাড়তি আবিষ্কার (Step 1-এর স্কোপের বাইরে, কিন্তু গুরুত্বপূর্ণ — পরে নতুন ধাপ লাগবে)

`SupabaseSyncManager.kt` স্ক্যান করার সময় RPC-মিসিং সমস্যার বাইরে আরেকটা আলাদা, সম্ভবত আরও
বিপজ্জনক প্যাটার্ন চোখে পড়েছে — এটা এই ধাপে ঠিক করা হয়নি (স্কোপের বাইরে), শুধু নোট করে রাখা হলো:

- ফাইলে একটা ডকুমেন্টেড উদাহরণ আছে (comment, লাইন ~২৯৫-৩০৩) যেখানে লেখা: `submitKyc()` আগে
  raw `client.postgrest.from("users").update(...)` (PATCH, RLS-নির্ভর) কল করত, আর তার
  `Result` কখনো চেক হতো না। **RLS পলিসি ম্যাচ ০টা row করলে PostgREST কোনো error না দিয়েই
  "সফল" রিটার্ন করে** — ফলে পুরো প্রজেক্টে কোনো ইউজারের `kyc_status` কখনোই cloud-এ আপডেট
  হয়নি, অথচ কোনো এরর লগও হয়নি। এটা এখন `submit_kyc` RPC দিয়ে ফিক্স করা হয়েছে বলে কমেন্টে
  লেখা আছে।
- কিন্তু **এই একই raw-PATCH প্যাটার্ন এখনো অন্তত ২২ জায়গায় আছে** (এই ফাইলে
  `.update(` ১৬টা, `.upsert(` ৩টা, `.delete {` ৩টা — কোনোটাই affected-row-count চেক করে না)।
  উদাহরণ: `upsertCategory`, `setCategoryActive`, `deleteCategoryRemote`, `upsertFaq`,
  `deleteFaqRow`, `adminSoftDeleteProblem`। এগুলোর প্রতিটাই RLS পলিসির উপর নির্ভর করে যে
  write সত্যিই কার্যকর হয়েছে — যদি কোনো পলিসি ভুলভাবে লেখা থাকে বা ভবিষ্যতে বদলে যায়, এই
  ফাংশনগুলো **নীরবে কিছুই না করে "সফল" রিটার্ন করবে**, ঠিক আগের KYC বাগের মতো, কিন্তু এবার
  কোনো Exception-ও থ্রো হবে না বলে বিদ্যমান `catch(Exception)` ব্লকও এটা ধরতে পারবে না।
- **এটা RPC-মিসিং সমস্যা থেকে ভিন্ন একটা category** — এর জন্য আলাদা একটা ফিক্স-ধাপ দরকার হবে
  (row-count ভেরিফাই করে থ্রো করা, অথবা সবকিছু RPC-তে সরিয়ে নেওয়া)। মাস্টার প্ল্যানে এটা এখনো
  কোনো ধাপে কভার করা হয়নি — এই আবিষ্কারের ভিত্তিতে একটা নতুন ধাপ যোগ করা দরকার।
- **✅ আপডেট (Step 4.5, 2026-09-18):** row-count ভেরিফাই-করে-থ্রো অ্যাপ্রোচ দিয়ে ১৬টা সাইট
  ফিক্স করা হয়েছে। ৩টা সাইট (`markMessagesAsReadForProblem`, `markNotificationAsRead`,
  `markAllNotificationsAsRead`) ইচ্ছাকৃতভাবে বাদ রাখা হয়েছে কারণ সেগুলোতে ০-row একটা বৈধ,
  স্বাভাবিক কেস (বিস্তারিত `RPC_SYNC_FIX_PROGRESS.md` Step 4.5 এন্ট্রিতে)। এই সেশনে সাইট
  কাউন্ট ১৯ পাওয়া গেছে (Step 1-এর ২২ থেকে অমিল — ভিন্ন zip ট্র্যাক, পরে ক্রস-চেক দরকার)।
  ⚠️ Android Studio বিল্ড দিয়ে এখনো ভেরিফাই করা হয়নি।

## এই ধাপে যা করা হয়নি (পরের ধাপের কাজ)

- কোনো RPC-এর আসল SQL সংজ্ঞা এখানে বসানো হয়নি — এই ধাপ শুধু ইনভেন্টরি/ডিফ।
- কোনো কোড বা migration ফাইল পরিবর্তন করা হয়নি।
- Step 2-এ লাইভ Supabase DB থেকে এই ৬৪টা RPC-এর verbatim সংজ্ঞা উদ্ধার করে নতুন migration
  ফাইলে যোগ করা হবে।

## ✅ আপডেট (Step 2, 2026-09-18) — Recovery ফলাফল

- **সব ৬৪টা মিসিং RPC লাইভ Supabase DB-তে পাওয়া গেছে** — কোনোটাই "NOT FOUND LIVE" না।
- ৭টা নামের **একাধিক overload** লাইভে পাওয়া গেছে (Kotlin caller ঠিক কোন overload ব্যবহার
  করে সেটা এই ধাপে অনুমান করা হয়নি — নিয়ম অনুযায়ী, যা লাইভে আছে তার **সবগুলোই** verbatim
  ক্যাপচার করা হয়েছে, কোনোটা বাদ দেওয়া হয়নি):
  - `admin_adjust_balance` → ২টা overload: `(uuid,numeric,boolean,text)` এবং
    `(uuid,numeric,boolean,text,text)`
  - `admin_notify_user` → ২টা overload: ৬-প্যারামিটার ও ৭-প্যারামিটার ভার্সন
  - `admin_set_banned` → ২টা overload: `(uuid,boolean)` এবং `(uuid,boolean,text)`
  - `admin_set_restricted` → ২টা overload: `(uuid,boolean)` এবং `(uuid,boolean,text)`
  - `create_notification` → ২টা overload: ৬-প্যারামিটার ও ৭-প্যারামিটার ভার্সন
  - `log_admin_action` → ২টা overload: ৪-প্যারামিটার ও ৫-প্যারামিটার ভার্সন
  - `submit_reputation_event` → ২টা overload: ৫-প্যারামিটার ও ৬-প্যারামিটার ভার্সন
  - **⚠️ পরের ধাপের জন্য নোট:** যেহেতু কোন overload আসলে সচল (Kotlin থেকে কল হয়), সেটা এই
    ধাপে যাচাই করা হয়নি (স্কোপের বাইরে — Step 2 শুধু capture করে)। যদি ভবিষ্যতে কোনো ধাপে এই
    ফাংশনগুলোর লজিক বদলাতে হয়, আগে `SupabaseSyncManager.kt`-এ প্রতিটার actual call-site দেখে
    কোন প্যারামিটার-কাউন্ট ব্যবহৃত হচ্ছে সেটা নিশ্চিত করে নেওয়া দরকার।
- মোট **৭১টা ফাংশন ডেফিনিশন** (৬৪ নাম + ৭টা অতিরিক্ত overload) ১২টা লজিক্যাল গ্রুপ migration
  ফাইলে verbatim বসানো হয়েছে (তালিকা `RPC_SYNC_FIX_PROGRESS.md` Step 2 এন্ট্রিতে)। প্রতিটার
  সাথে সংশ্লিষ্ট `GRANT EXECUTE` স্টেটমেন্টও ক্যাপচার করা হয়েছে।

## ✅ আপডেট (Step 2 ফলো-আপ, 2026-09-18) — Overload resolution যাচাই

৭টা নামের multiple overload প্রশ্নে (উপরের Step 2 আপডেট দেখুন), `SupabaseSyncManager.kt`-এ
প্রতিটা RPC-এর **exactly একটা call-site** পাওয়া গেছে (একাধিক জায়গা থেকে কল হয় না), তাই সরাসরি
কোড পড়ে নিশ্চিত করা গেছে কোন overload আসলে ব্যবহৃত হয়:

**দুটো overload-ই সচল (role parameter nullable, conditionally `p_role` পাঠানো হয়):**
- `admin_adjust_balance` — `role: String? = null`; role দিলে ৫-প্যারামিটার, না দিলে ৪-প্যারামিটার
- `admin_set_banned` — একই প্যাটার্ন
- `admin_set_restricted` — একই প্যাটার্ন
- `submit_reputation_event` — একই প্যাটার্ন

**পুরনো/ছোট overload এখন dead code (role non-nullable ডিফল্ট `""`, `p_role` সবসময় unconditionally পাঠানো হয় — তাই শুধু বড় overload-ই resolve হয়):**
- `admin_notify_user` — শুধু ৭-প্যারামিটার (p_role সহ) overload কল হয়, ৬-প্যারামিটার ভার্সন এখন আর
  কখনো call হয় না
- `create_notification` — শুধু ৭-প্যারামিটার overload কল হয়, ৬-প্যারামিটার ভার্সন dead code
- `log_admin_action` — শুধু ৫-প্যারামিটার overload কল হয়, ৪-প্যারামিটার ভার্সন dead code

⚠️ **future cleanup candidate (এই ধাপের স্কোপের বাইরে, কোনো অ্যাকশন নেওয়া হয়নি):** উপরের ৩টা
RPC-এর পুরনো/ছোট overload লাইভ DB-তে এখনো বিদ্যমান কিন্তু Kotlin কোড থেকে unreachable। এগুলো
মুছে ফেলা নিরাপদ কিনা (অন্য কোনো ক্লায়েন্ট/স্ক্রিপ্ট থেকে কল হয় কিনা) যাচাই না করে ড্রপ করা
উচিত না — শুধু নোট করে রাখা হলো।

## নোট: `admin_credentials_get_phone`

এই একটার Kotlin caller UNKNOWN হিসেবে চিহ্নিত হয়েছে — মানে এটা সরাসরি কোনো নামযুক্ত wrapper
ফাংশনের ভেতর থেকে কল হয়নি বলে স্ক্রিপ্ট শনাক্ত করতে পারেনি (হতে পারে inline lambda/companion
object-এর ভেতর থেকে কল হয়েছে)। Step 2-এ এটা ম্যানুয়ালি আবার যাচাই করা দরকার।
