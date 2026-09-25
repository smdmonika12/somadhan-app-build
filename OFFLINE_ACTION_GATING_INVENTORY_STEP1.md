# ধাপ ১ — ইনভেন্টরি ও ক্যাটাগরাইজেশন (Offline Action Gating)

কোনো কোড পরিবর্তন হয়নি — শুধু `SomadhanSyncManager.kt` / `SomadhanRepository.kt` / `SupabaseAuthManager.kt`-এ সব `.rpc(...)` কল ও সরাসরি টেবিল `.insert()/.update()/.upsert()` কল গ্রেপ করে, প্রতিটার সাথে `enqueueOutboxRetry()` যুক্ত কিনা যাচাই করে এই ক্যাটাগরাইজেশন তৈরি হয়েছে। ইউজার কনফার্ম করলে ধাপ ২ (Admin toggle) শুরু হবে।

**গুরুত্বপূর্ণ নোট:** Outbox বর্তমানে "retry-on-failure" প্যাটার্নে কাজ করে — অর্থাৎ Group A-এর একটা RPC-ও অফলাইনে থাকলে app আগে সরাসরি কলটা **ট্রাই করে**, সেটা fail করলে তখন outbox-এ retry entry বসে। এখন network না থাকা অবস্থায় সরাসরি "না ট্রাই করেই" queue করা হয় না। এই নুয়্যান্সটা ধাপ ৪-এর guard ডিজাইন করার সময় মাথায় রাখতে হবে (Group A হলেও ইউজার একটা মুহূর্তের জন্য error/loading দেখতে পারে, তারপর queued হয়ে যাবে)।

---

## গ্রুপ A — ইতিমধ্যে Outbox-retry কভার্ড (১৩টা RPC)

`SomadhanRepository.kt`-এ সরাসরি `enqueueOutboxRetry(rpcName = ...)` কল পাওয়া গেছে এই RPC-গুলোর জন্য:

- `release_escrow`
- `refund_escrow_once`
- `request_withdrawal`
- `process_withdrawal`
- `admin_approve_kyc`
- `admin_reject_kyc`
- `admin_revoke_kyc`
- `admin_set_banned`
- `admin_set_restricted`
- `admin_set_verified_badge`
- `admin_adjust_balance`
- `admin_change_role`
- `increment_escrow_extra_amount`

→ ধাপ ১২-এ শুধু এগুলো ভেরিফাই হবে (কোড পরিবর্তন সাধারণত লাগার কথা না)।

---

## গ্রুপ B — সরাসরি network call, Outbox-এ নেই (guard লাগবে)

ডোমেইন-ব্যাচ অনুযায়ী ভাগ করা হয়েছে, যাতে মাস্টার প্রম্পটের ধাপ ৫–১১-এর সাথে সরাসরি মিলে যায়।

### ধাপ ৫ — Auth
- Login (`signInWith...`), Register/OTP (`signUpWithPhone`, OTP verify), `complete_registration_profile`, Logout

### ধাপ ৬ — Wallet/Payment 🔴 money
- `deposit_money_via_gateway`, `request_wallet_deposit`, `admin_confirm_gateway_deposit`, `record_gateway_payment_log`
- Admin money-repair টুল: `admin_reconcile_user_balances`, `admin_reconcile_escrow_states`, `admin_repair_missing_refunds`, `admin_cleanup_duplicate_refunds`

### ধাপ ৭ — Bidding lifecycle 🔴 money-adjacent
- `accept_bid`, `cancel_bid`, `reject_bid`, `solver_cancel_job`, `owner_reset_orphaned_accepted_bid`
- Job-release ফ্লো: `request_job_release`, `cancel_job_release_request`, `reject_job_release_request`
- লাইভ-স্ট্যাটাস: `mark_job_started`, `mark_solver_on_way`, `mark_solver_arrived`, `update_solver_live_location`, `mark_completion_result_seen`

### ধাপ ৮ — Problem posting / Instant jobs
- সরাসরি টেবিল insert: `createProblem` (`problems` insert), `submitBid`-এর ভেতরের `bids` insert
- `user_delete_problem`, `mark_problem_seen`
- Instant job: `broadcast_instant_job`, `cancel_instant_job`, `expire_broadcasting_instant_job` (cron-triggered, client-action না — যাচাই দরকার)
- Solver quota: `sync_solver_free_job_quota`, `system_track_extra_payment_miss`
- Admin: `admin_update_problem_status`, `admin_update_problem_budget`, `admin_refund_and_reopen_problem` 🔴, `admin_force_cancel_instant_job`, `admin_reset_free_job_quota`, `admin_bulk_reset_free_job_quota`, `admin_reset_miss_cycle`

### ধাপ ৯ — Chat/Messages
- সরাসরি insert: `sendMessage` (`messages` insert), messages `.update()` (mark-seen)
- Admin: `admin_send_message_to_problem_chat`, `admin_delete_message`, `system_event_message`

### ধাপ ১০ — Ratings/Reviews, Additional charges, Disputes 🔴 কিছু money-related
- Ratings: `submit_rating`, `admin_delete_rating`
- Additional charge: `request_additional_charge`, `respond_additional_charge`, `mark_additional_charge_settled` 🔴, `request_extra_amount`, `user_confirm_extra_amount`, `user_reject_extra_amount`
- Dispute: `raise_dispute`, `resolve_dispute` 🔴, `resolve_dispute_split` 🔴, `settle_dispute` 🔴, `withdraw_dispute`, `admin_manually_flag_dispute`, `mark_dispute_result_seen`, `request_admin_assistance`

### ধাপ ১১ — Admin misc, KYC, Role/Account, Notifications, Direct contracts
- KYC: `submit_kyc`
- Role/Account: `switch_role_get_or_create_linked_profile`, `sync_linked_account_profile`, `admin_soft_delete_user`, `admin_credentials_update`, `admin_credentials_verify_password`, `admin_remove_category_from_solvers`, `admin_reassign_solver`, `submit_reputation_event`, `clear_solver_cancelled_notice`
- Direct contract: `accept_direct_contract`, `decline_direct_contract`, `admin_update_direct_contract_status`
- Notification (write): `create_notification`, `notify_admins`, `admin_broadcast_notification`, `admin_notify_user`, `admin_delete_notification_group`, `log_admin_action`
- Withdrawal misc: `admin_update_withdrawal_trx_id`
- ⚠️ বিপজ্জনক/ডেভ-ওনলি, আলাদা করে ট্রিট করা দরকার: `admin_wipe_all_data`

---

## গ্রুপ C — Read-only (offline-এ cached data দেখানো উচিত, ব্লক না)

- Problems: `fetchOpenProblems`/`fetchProblemById`/category-wise fetch
- Bids: `fetchBidsForProblem`
- Wallet: `fetchMyTransactions`
- Profile/User fetch, categories/faqs fetch
- Notifications fetch
- Admin: `admin_get_dashboard_metrics` (read), `admin_credentials_get_phone` (read)
- Realtime subscriptions: problems-table, bids-per-problem (`SupabaseRealtimeManager.kt`)
- Loading/Sync Fix Roadmap v2-এ ট্র্যাক করা ~৩৩টা স্ক্রিনের pull-to-refresh/initial-load — এগুলোর অফলাইন আচরণ ধাপ ১৩-এ যাচাই হবে

---

## যাচাই দরকার (ইউজার কনফার্মেশনের অপেক্ষায়)

1. Group B-এর মধ্যে কিছু RPC (`expire_broadcasting_instant_job`, `system_notify_48hour_auto_release`-এর মতো) সম্ভবত cron/system-triggered, কোনো UI বাটনের সাথে সরাসরি যুক্ত না — এগুলোর জন্য guard লাগবে না, শুধু "client থেকে সরাসরি call হয় কিনা" প্রতিটা domain-ধাপে কনফার্ম করে বাদ দেওয়া হবে।
2. `admin_wipe_all_data`-এর মতো destructive টুল আলাদাভাবে ট্রিট করা উচিত কিনা (যেমন: সবসময় ব্লক করা থাকবে অফলাইনে, কোনো retry/queue না) — ধাপ ১১-এ সিদ্ধান্ত নেওয়া হবে।
3. এই লিস্ট RPC-নাম ও টেবিল-কল গ্রেপ করে তৈরি — প্রতিটার exact UI call-site (কোন স্ক্রিনের কোন বাটন) ধাপ ৫–১১-এর নিজ নিজ ব্যাচে কনফার্ম করে নেওয়া হবে, এখানে শুধু high-level ম্যাপিং।

**পরবর্তী ধাপ (২): Admin panel-এ `strict_offline_block` টগল ইনফ্রাস্ট্রাকচার — কনফার্মেশনের অপেক্ষায়।**
