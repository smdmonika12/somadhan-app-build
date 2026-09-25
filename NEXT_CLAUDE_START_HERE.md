# পরবর্তী Claude সেশনের জন্য — এখান থেকে শুরু করো

তুমি Role-Switch / Cross-Device Sync বাগ ফিক্সের কাজ চালিয়ে যাচ্ছো। প্রথমে এই ৪টা ফাইল
ক্রমানুসারে পড়ো:

1. `ROLE_UID_SYNC_FIX_MASTER_PROMPT.md` — পুরো প্ল্যান (ধাপ ০–৭) এবং **General Rules**
   (বিশেষ করে rule #৯: প্রতিটা ধাপের পর confirmation ছাড়া এগোবে না)।
2. `ROLE_UID_AUDIT.md` — ধাপ ০-এর আউটপুট। **⚠️ এই ফাইলের লাইন নাম্বার আর মেলে না** (ধাপ ২-৩-এ
   নতুন কোড যোগ হওয়ায় শিফট হয়ে গেছে) — ফাংশনের নাম দিয়ে রেফারেন্স করো, লাইন নাম্বার না। আসল
   বর্তমান লাইন নাম্বার লাগলে `grep -n "currentUserId() =="
   app/src/main/java/com/example/data/repository/SomadhanRepository.kt` চালাও।
3. `ROLE_UID_FIX_DESIGN.md` — ধাপ ১-এর আউটপুট।
4. `ROLE_UID_SYNC_FIX_PROGRESS.md` — progress log। **এটাই সর্বশেষ সত্য** (এই ফাইলের সাথে
   কখনো গরমিল হলে progress log মানবে)।

## এখন কী করতে হবে

**ধাপ ০, ১, ২, ৩, ৪, ৫ (সবগুলো ৭টা ব্যাচ), ৬ সম্পন্ন — `ROLE_UID_FIX_TEST_CHECKLIST.md` তৈরি হয়ে
গেছে। এখন ব্যবহারকারী নিজে টেস্ট করে ফলাফল (pass/fail) জানানোর অপেক্ষায়। ফলাফল না আসা পর্যন্ত
পরের কোনো ধাপ/ফিক্স শুরু হবে না (rule #৯)। কোনো কেস fail করলে সেটা নিয়ে আলাদা ছোট ফিক্স-ধাপ
শুরু হবে (ধাপ ৭-এর আগেই)।**

ধাপ ৪-এ যা হয়েছে (সংক্ষেপে): `SomadhanViewModel.completeLoginAfterOtp()`-এ login-এর পর
`migrateLegacyDualRowsIntoRoot()` কল যোগ হয়েছে; `switchRoleToSolver()`/`switchRoleToUser()`
এখন পুরনো `repository.switchRole()`-এর বদলে `repository.switchRoleInPlace()` কল করে (role
switch-এ `id` আর বদলায় না)। পুরনো `switchRole()` repository-তে dead code হিসেবে রয়ে গেছে।

ধাপ ৫-এ `SomadhanRepository.kt`-এর ৫১টা `SupabaseAuthManager.currentUserId() == ...` guard
(৭টা ব্যাচে, feature area অনুযায়ী) verify করা হয়েছে — **সবগুলোই "self" কেস** বেরিয়েছে, ধাপ
৪-এর পর এমনিতেই সঠিকভাবে pass করছে। **কোনো ব্যাচেই কোনো কোড/ফাইল বদলাতে হয়নি।** বিস্তারিত
`ROLE_UID_SYNC_FIX_PROGRESS.md`-এর "ব্যাচ ১" থেকে "ব্যাচ ৭" পর্যন্ত সেকশনে, আর "ধাপ ৫ —
সম্পূর্ণ" সামারি সেকশনে (স্কোপের বাইরে পাওয়া ৩টা আলাদা, আগে থেকে-নথিভুক্ত/অ-role-switch গ্যাপসহ)।

**পরবর্তী ধাপ (৬) অনুযায়ী কাজ (master prompt দেখুন):** কোনো নতুন ফিচার কোড না, শুধু
`ROLE_UID_FIX_TEST_CHECKLIST.md` নামে একটা manual test checklist বানানো (দুই ডিভাইস + admin
panel cross-sync verify করার জন্য, master prompt-এর ধাপ ৬ section-এ কেসগুলো দেওয়া আছে)।
চেকলিস্ট বানানোর পর থামো — ব্যবহারকারী নিজে টেস্ট করে ফলাফল জানাবেন। **ব্যবহারকারীর explicit
confirmation ছাড়া শুরু করা যাবে না (rule #৯)।**

## এই সেশনে পাওয়া একটা আলাদা (স্কোপের বাইরে) ইস্যু — ঠিক করা হয়নি

`resolveCommissionRateForNewJob(solverId)`-এর ভেতরের ২টা `currentUserId() == solverId` guard
`acceptBid`-এর ভেতর থেকে কল হলে caller = problem owner (solver না) — তাই সেই path-এ solver-এর
free-quota counter কখনো Supabase-এ sync হয় না। এটা role-switch/dual-row বাগ না, একটা আলাদা
structural caller-mismatch গ্যাপ (rule #১ অনুযায়ী স্কোপের বাইরে, স্পর্শ করা হয়নি)। বিস্তারিত
progress log-এর "ব্যাচ ২" সেকশনে।

## 📌 role-uid sync fix শেষ হওয়ার পর হাতে নিতে হবে (আলাদা মাস্টার প্রম্পট/সেশন)

`adminCancelAndRefundDirectContract()`-এ একটা cloud-sync গ্যাপ পাওয়া গেছে (ব্যাচ ৬-এর
`sendMessage()` verify করার সময়) — এসক্রো রিফান্ড ঠিকমতো cloud-sync হয়, কিন্তু প্রবলেম স্ট্যাটাস
আপডেট, দুটো নোটিফিকেশন, আর চ্যাট মেসেজ (এই তিনটাই) কোনো cloud dual-write ছাড়াই শুধু local
Room-এ থেকে যায়। এটাও role-switch/dual-row বাগ না (rule #১ অনুযায়ী স্কোপের বাইরে) — **ধাপ ৫-৬
শেষ হওয়ার পর, আলাদা একটা সেশনে/মাস্টার প্রম্পটে** এটা ফিক্স করা উচিত। সম্পূর্ণ বিস্তারিত (কী
ভেঙেছে, কেন, আর সমাধানের প্রস্তাবিত পথ) `MIGRATION_PROGRESS.md`-এর একদম শেষে "📌 ট্র্যাকিং
নোট: `adminCancelAndRefundDirectContract()` cloud-sync গ্যাপ" সেকশনে।

## খোলা ইস্যু (ধাপ ৬-এর আগে সিদ্ধান্ত দরকার)

`AppDatabase.kt`-এ `MIGRATION_45_46` অনুপস্থিত → version ৪৫-এ থাকা ইনস্টল আপগ্রেডে
`fallbackToDestructiveMigration` চলে, পুরো local DB মুছে যায়। আগে থেকেই ছিল, এখনো ঠিক করা হয়নি
(স্কোপের বাইরে, rule #১)।

## প্রজেক্ট গঠন

মূল প্রাসঙ্গিক ফাইল: `app/src/main/java/com/example/data/repository/SomadhanRepository.kt`
(~১০১৫৬ লাইন), `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt`,
`app/src/main/java/com/example/data/entity/UserEntity.kt`,
`app/src/main/java/com/example/data/database/AppDatabase.kt`,
`app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt`।

