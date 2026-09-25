# BALANCE_REPUTATION_ROLE_SEPARATION_PROGRESS.md

এই ফাইলটা `BALANCE_REPUTATION_ROLE_SEPARATION_MASTER_PROMPT.md`-এর general rule #১০ অনুযায়ী —
প্রতিটা ধাপ শেষে এখানে log যোগ হবে, যাতে সেশন হারিয়ে গেলেও পরবর্তী ধাপ কোথা থেকে শুরু করতে হবে
বোঝা যায়।

---

## ধাপ ০ — অডিট ✅ সম্পন্ন

- **আউটপুট:** `ROLE_SEPARATION_AUDIT.md`
- **কী করা হয়েছে:** মাস্টার প্রম্পটের ৭টা ক্যাটাগরি গ্রেপ/পড়ে অডিট করা হয়েছে —
  - `TransactionEntity(` তৈরি হয় এমন ১৫টা সাইট (১৪টা repository + ১টা DTO mapper), প্রতিটার
    role নির্ধারণ। ৩টা সাইটে role ইতিমধ্যেই লোকাল ভ্যারিয়েবলে (`correctionRole`/
    `depositRole`/`adjustRole`) গণনা করা পাওয়া গেছে।
  - shared `reputationScore` write — ৫টা সাইট চেক করে ৪টা role-aware (switchRole/legacy-merge)
    আর শুধু `applyReputationChange()` (৮৬০০) বাগযুক্ত বলে কনফার্ম করা হয়েছে।
  - generic `user.balance` UI (৩ স্ক্রিন) কনফার্মড, আগের flag ঠিক।
  - role-scoped column pair চেক — শুধু reputation-এরই role-scoped DAO ফাংশন নেই, বাকি সব
    (balance/isBanned/isRestricted/verifiedBadge) আছে।
  - `reconcileUserBalances()` বাগ কনফার্মড; reputation-এর কোনো recompute ফাংশনই নেই।
  - `NotificationEntity` ৮২ সাইট — ৩৭টা variable-name থেকে role স্পষ্ট, বাকি ~৪৫টা প্রাথমিক
    তালিকা হিসেবে গ্রুপ করা হয়েছে (পূর্ণ classification ধাপ ৬-এ হবে)।
  - `logAdminAction()` ৭১ কল — role-scoped actionType গ্রুপ চিহ্নিত।
  - ৩টা নতুন পর্যবেক্ষণ flag করা হয়েছে (সবচেয়ে গুরুত্বপূর্ণ: admin panel-এ generic
    `reputationScore` display-ও balance-এর মতোই role-unaware, ৯ জায়গা — ধাপ ৭-এর স্কোপ
    বাড়ানো বিবেচনার জন্য)।
- **কোনো কোড বদলানো হয়নি।**
- **কী টেস্ট করা উচিত:** কিছু না — শুধু অডিট, বিল্ডে প্রভাব নেই।

## ধাপ ১ — Reputation role-scoped DAO ফিক্স ✅ সম্পন্ন

- **বদলানো ফাইল:**
  - `app/src/main/java/com/example/data/dao/AppDaos.kt`
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt`
- **কী করা হয়েছে:**
  1. `UserDao`-তে দুইটা নতুন ফাংশন যোগ হয়েছে (`setBannedStatus` ফাংশনের ঠিক আগে,
     `addBalanceForUserRole`/`SolverRole` গ্রুপের পরে):
     - `updateReputationForUserRole(userId, newScore, timestamp)`
     - `updateReputationForSolverRole(userId, newScore, timestamp)`
     এগুলো `addBalanceForUserRole`/`SolverRole`-এর ঠিক একই প্যাটার্নে লেখা — শেয়ার্ড plain
     `reputationScore` কলামও একই cross-role-mix-বিরোধী `CASE WHEN role = '...' THEN ... ELSE
     reputationScore END` গার্ড দিয়ে আপডেট হয়। পার্থক্য শুধু এটুকু: balance ফাংশনগুলো একটা
     ডেল্টা amount +/- করে, কিন্তু এই দুটো caller-এর কাছ থেকে আগে থেকেই clamp করা (0.0–100.0)
     একটা absolute `newScore` নিয়ে সরাসরি SET করে (কারণ `applyReputationChange()` নিজেই
     ক্ল্যাম্পিং করে কল করে)।
  2. `SomadhanRepository.applyReputationChange()`-এ, পুরনো `user.copy(reputationScore =
     newScore)` + `userDao.updateUser(updated)` কলটা **অপরিবর্তিত/পাশে রেখে** (মুছে ফেলা
     হয়নি), তার ঠিক পরে `user.role` দেখে (`correctionRole`/`adjustRole`-এর ঠিক একই প্যাটার্নে,
     `reputationRole` নামের ভ্যারিয়েবল) সঠিক নতুন role-scoped DAO ফাংশনটাও কল করা হচ্ছে:
     - `role == "SOLVER"` → `updateReputationForSolverRole()`
     - `role == "USER"` → `updateReputationForUserRole()`
     - অন্য কিছু/null (যেমন `"ADMIN"`) → **fallback**, শুধু উপরের plain-column write-ই থাকে,
       নতুন কোনো DAO কল হয় না (পুরনো আচরণ অক্ষত)।
  3. এই ফাংশনের নিচেই আগে থেকে একটা `reputationRole` ভ্যারিয়েবল (একই `when (user.role)`
     লজিক দিয়ে) cloud RPC dual-write-এর জন্য আলাদাভাবে গণনা হতো — সেই ডুপ্লিকেট গণনা
     সরিয়ে উপরে নতুন করে তোলা `reputationRole` ভ্যারিয়েবলটাই দুই জায়গায় (local DAO mirror +
     RPC push) পুনর্ব্যবহার করা হচ্ছে। ফাংশনের বাকি লজিক (RPC কল, guard, কমেন্ট) অপরিবর্তিত।
- **caller verification (race-condition check):** `applyReputationChange()`-এর সব caller
  (`applyCappedPerEventReputation`, `applyCappedPerProblemReputation`,
  `triggerDynamicReputationEvent`, `adminAdjustReputation`, `runInactivityReputationDecay`)
  সোর্স পড়ে যাচাই করা হয়েছে — কোনোটাই `applyReputationChange()` কলের আগের একটা stale
  `UserEntity` কপি ধরে রেখে পরে সেটা আবার `userDao.updateUser()`-এ লিখে ফেলে না।
  `runInactivityReputationDecay()` একমাত্র জায়গা যেখানে `applyReputationChange()`-এর পরে
  আবার `userDao.updateUser()` কল হয় (`lastReputationDecayCheckAt` সেট করতে) — কিন্তু এটা
  সেই কলের **আগে freshly re-fetch করে** (`userDao.getUserById(user.id)`), তাই নতুন
  role-scoped mirror ওভাররাইট হওয়ার ঝুঁকি নেই। বাকি ৪টা caller ইউজার এনটিটি স্পর্শই করে না,
  শুধু `applyReputationChange()`-কে delegate করে।
- **যাচাই করা হয়েছে:**
  - brace/paren balance: দুটো ফাইলেই edit-এর আগে-পরে balanced (paren-এর সামান্য ননজিরো diff
    কমেন্ট/স্ট্রিং লিটারেলের বন্ধনী থেকে, edit-এর আগেও ছিল, edit-এ কোনো নতুন imbalance
    আসেনি)।
  - file count: edit-এর আগে ও পরে প্রজেক্টে ২৬২টা ফাইল (এই progress log আর audit .md যোগ
    হওয়ার আগে) — কোনো ফাইল হারায়নি/অপ্রত্যাশিতভাবে যোগ হয়নি।
  - পূর্ণ `gradle build`/`gradlew` কম্পাইল এই sandbox-এ চালানো যায়নি (নেটওয়ার্ক/SDK ছাড়া) —
    শুধু syntax-level (brace/paren balance + ম্যানুয়াল রিভিউ) যাচাই হয়েছে। **ব্যবহারকারীকে
    নিজের ডিভাইস/CI-তে আসল কম্পাইল চালিয়ে নিশ্চিত হতে হবে।**
- **ইচ্ছাকৃতভাবে যা বদলানো হয়নি:** `TransactionEntity`/`reconcileUserBalances()` (এই ধাপের
  স্কোপের বাইরে, ধাপ ২-৫-এর কাজ) স্পর্শ করা হয়নি। `reputationScore` shared কলাম dual-write
  cutover (পুরনো plain-only write সরিয়ে ফেলা) এখনো হয়নি — ইচ্ছাকৃতভাবে, master prompt-এর rule
  #৩ অনুযায়ী।
- **কী টেস্ট করা উচিত (ব্যবহারকারীর পরবর্তী পদক্ষেপ):**
  1. প্রজেক্ট কম্পাইল করা (Android Studio/`./gradlew assembleDebug`) — শুধু সিনট্যাক্স
     ম্যানুয়ালি চেক হয়েছে, আসল Kotlin কম্পাইলার চালানো হয়নি।
  2. একজন dual-role ইউজারের ওপর একটা reputation event trigger করা (যেমন job completion বা
     admin adjust reputation) — শুধু active role-এর `reputationScoreUser`/`reputationScoreSolver`
     বদলাচ্ছে কিনা, অন্য role-এরটা অপরিবর্তিত থাকছে কিনা (role switch/app restart ছাড়াই,
     তাৎক্ষণিক লোকাল ডেটাবেসে — `adb shell` দিয়ে Room DB query করে বা admin panel দিয়ে
     verify করা যায়)।
  3. Admin panel থেকে `adminAdjustReputation()` চালিয়ে দেখা কাজ করছে কিনা (fallback পাথ
     সহ, যদি কোনো টেস্ট ইউজারের role অপ্রত্যাশিত ভ্যালু থাকে)।

## ধাপ ২ — TransactionEntity role field ডিজাইন ✅ সম্পন্ন

- **আউটপুট:** `TRANSACTION_ROLE_FIELD_DESIGN.md` (কোনো কোড এডিট হয়নি)
- **ব্যবহারকারীর কনফার্মড সিদ্ধান্ত (ধাপ ৩ শুরুর আগে):**
  1. Supabase RPC-গুলো (deposit/release/refund/withdraw/admin-adjust) role কলাম **লিখবে**
     (ডিজাইন ডকের প্রশ্ন #৩ এর সুপারিশ অনুযায়ী, "হ্যাঁ" কনফার্মড)।
  2. Legacy blank-role transaction হ্যান্ডলিং (ধাপ ৪-এর জন্য প্ল্যান, প্রশ্ন #৫) — ব্যবহারকারী
     "যেটা best হবে" বলে ডিজাইন ডকের **সুপারিশ (অপশন B: type-ভিত্তিক read-time backfill,
     `ADMIN_ADJUSTMENT`/`BALANCE_RECONCILIATION` আলাদা "UNCLASSIFIED_LEGACY")** accept করেছেন
     — এটাই ধাপ ৪-এ implement করতে হবে, অপশন A না।

## ধাপ ৩ — role ফিল্ড implement + creation site wiring ⚠️ আংশিক সম্পন্ন (IN PROGRESS)

**⚠️ এই ধাপ সম্পূর্ণ হয়নি — পরবর্তী সেশন এখান থেকেই চালিয়ে যাবে। এখনো zip ছাড়া হয়নি
(General Rule #৯ এখনো পূরণ হয়নি এই ধাপের জন্য, এই আপডেটের সাথেই প্রথম zip যাচ্ছে কিন্তু
কাজ অসম্পূর্ণ অবস্থায়)।**

### যা শেষ হয়েছে:

- **বদলানো ফাইল:**
  - `app/src/main/java/com/example/data/entity/MarketEntities.kt` — `TransactionEntity`-তে
    `val role: String = ""` ফিল্ড যোগ (ডিফল্ট blank, বিদ্যমান row ভাঙে না)।
  - `app/src/main/java/com/example/data/database/AppDatabase.kt` —
    - **`MIGRATION_50_51`** নতুন যোগ করা হয়েছে (`ALTER TABLE transactions ADD COLUMN role TEXT
      NOT NULL DEFAULT ''`), `addMigrations(...)` লিস্টে রেজিস্টার করা হয়েছে, `version = 50`
      থেকে `51`-এ বাড়ানো হয়েছে।
    - **⚠️ পর্যবেক্ষণ (এই ধাপের স্কোপের বাইরে, ছোঁয়া হয়নি, কিন্তু flag করা জরুরি):**
      বিদ্যমান কোডে `version = 49` থেকে `50`-এ যাওয়ার জন্য কোনো `MIGRATION_49_50` লেখা/
      রেজিস্টার করা নেই (শুধু `fallbackToDestructiveMigration`-এর ভরসায় রাখা হয়েছিল, ওপরের
      কমেন্টেই স্বীকার করা আছে) — মানে version 49-এ থাকা কোনো ডিভাইস আপগ্রেড করলে পুরো local DB
      (সব balance/reputation/transaction history) মুছে destructive recreate হয়ে যাবে। এই ধাপে
      নতুন `MIGRATION_50_51` লেখার সময় rule #৪ মানতে গিয়ে এটা চোখে পড়েছে, কিন্তু এই gap-টা fix
      করিনি (আলাদা সমস্যা, আলাদা ছোট ফিক্স-রাউন্ড দরকার — ব্যবহারকারীকে আলাদাভাবে জানানো হলো)।
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — **local ১৪টা
    creation site-ই wiring সম্পন্ন**, `TRANSACTION_ROLE_FIELD_DESIGN.md`-এর টেবিল অনুযায়ী:
    | # | লাইন (নতুন) | role |
    |---|---|---|
    | 1 | 435 | `"SOLVER"` (payoutEscrowToSolver) |
    | 2 | 2645 | `"USER"` (bid accept deduction) |
    | 3 | 3806 | `"SOLVER"` (dispute split solver share) |
    | 4 | 4301 | `"USER"` (refundEscrowOnce) |
    | 5 | 4603 | `correctionRole ?: ""` (reconcileUserBalances, বিদ্যমান ভ্যারিয়েবল) |
    | 6 | 4801 | `"USER"` (cleanupDuplicateRefunds) |
    | 7 | 4977 | `"USER"` (repairMissingRefunds) |
    | 8 | 5612 | `"USER"` (release-time extra bill deduction) |
    | 9 | 6739 | `"SOLVER"` (solver withdrawal deduction) |
    | 10 | 6827 | `"SOLVER"` (withdrawal reject refund) |
    | 11 | 7136 | `depositRole` (depositMoneyViaGateway) |
    | 12 | 7438 | `adjustRole ?: ""` (adminAdjustBalance, বিদ্যমান ভ্যারিয়েবল) |
    | 13 | 8283 | `"USER"` (extra charge accept deduction) |
    | 14 | 9807 | `"USER"` (confirm extra amount deduction) |
    - সাইট #১১-এ একটা সাইড-ফিক্স লেগেছে: `depositRole` ভ্যারিয়েবলটা আগে শুধু একটা ভেতরের
      `if (SupabaseAuthManager.currentUserId() == rootAccountId)` ব্লকের মধ্যে declare হতো
      (শুধু RPC কলের জন্য), যেখানে নিচের `TransactionEntity(...)` সাইট ওই if-ব্লকের বাইরে —
      তাই compile হতোই না (scope-এর বাইরে রেফারেন্স)। `val depositRole = ...` লাইনটা if-ব্লকের
      ঠিক আগে hoist করা হয়েছে (একই লজিক, `user.role == "SOLVER"` চেক), এখন দুই জায়গাতেই
      (RPC dual-write + TransactionEntity.role) একই ভ্যারিয়েবল reuse হচ্ছে — কোনো নতুন
      ডুপ্লিকেট গণনা তৈরি হয়নি।
- **যাচাই করা হয়েছে (এই তিনটা ফাইলের জন্য):**
  - brace balance: balanced (edit-এর আগে-পরে দুটোই সমান)।
  - paren balance: `SomadhanRepository.kt`-এ edit-এর আগে diff ছিল -৯ (৯টা বেশি closing,
    কমেন্ট/স্ট্রিং লিটারেল থেকে, ধাপ ১-এও একই পর্যবেক্ষণ হয়েছিল), edit-এর পরেও ঠিক -৯ — কোনো
    নতুন imbalance যোগ হয়নি। অন্য দুই ফাইল (`MarketEntities.kt`, `AppDatabase.kt`) সম্পূর্ণ
    balanced।
  - file count: zip-এর আগে-পরে ২৬৪টা ফাইল (অপরিবর্তিত) — `diff` দিয়ে দুই ফাইল-লিস্ট হুবহু মিলিয়ে
    দেখা হয়েছে, কোনো ফাইল হারায়নি/অপ্রত্যাশিতভাবে যোগ হয়নি।
  - পূর্ণ `gradle build` sandbox-এ চালানো যায়নি (আগের ধাপগুলোর মতোই) — শুধু syntax-level
    যাচাই। **ব্যবহারকারীকে নিজের ডিভাইস/CI-তে কম্পাইল চালিয়ে নিশ্চিত হতে হবে**, বিশেষ করে
    সাইট #১১-এর hoist-করা `depositRole` ভ্যারিয়েবলটা এখন সঠিক জায়গায় scope-এ আছে কিনা।

### যা এখনো বাকি (এই ধাপ-৩ ব্যাচেরই অংশ, পরের সেশনে এখান থেকে শুরু করতে হবে):

1. **Supabase migration SQL লেখা** — `transactions` টেবিলে `role text` কলাম যোগ করার নতুন
   `.sql` ফাইল (`supabase/migrations/`-এ, বিদ্যমান নামকরণ কনভেনশন অনুসরণ করে, যেমন
   `step36_transaction_role_column.sql` জাতীয় নাম) — **শুধু ফাইল লিখে রাখতে হবে, নিজে থেকে
   apply করা যাবে না** (rule #৩, ব্যবহারকারীর কাজ)।
2. **প্রতিটা সংশ্লিষ্ট RPC-তে `p_role` প্যারামিটার + `transactions.role` write যোগ করা** —
   deposit/release(payout)/refund/withdraw(request+process)/admin-adjust/reconcile —
   `supabase/migrations/`-এর ভেতরে এই RPC-গুলোর latest সংজ্ঞা কোন ফাইলে আছে তা এখনো
   পুরোপুরি খুঁজে বের করা হয়নি (এই সেশন এখানেই থেমেছে) — `docs/RPC_INVENTORY_REPORT.md`
   ফাইলটা থাকলে সেখান থেকে শুরু করা যেতে পারে, নাহলে প্রতিটা RPC নাম ধরে
   `grep -rn "CREATE OR REPLACE FUNCTION <name>"` দিয়ে `supabase/migrations/*.sql`-এ
   খুঁজতে হবে (৪৫টা .sql ফাইল আছে এই মুহূর্তে)। প্রতিটার জন্য **নতুন migration ফাইল** লিখতে
   হবে (পুরনোটা এডিট না করে, `CREATE OR REPLACE FUNCTION` দিয়ে নতুন ভার্সন), যেহেতু rule #৩
   অনুযায়ী কোনো কোড মোছা যাবে না।
3. **`MessageTransactionMappers.kt`-এর `TransactionDto.toTransactionEntity()` (লাইন ~৯২)-এ
   `role` pass-through যোগ করা** — এর জন্য প্রথমে `dto/TransactionDto.kt`-তে `role` ফিল্ড যোগ
   করতে হবে (Supabase কলাম আসার পর, উপরের #১-২ শেষ হলে)।
4. উপরের সব শেষ হলে: কম্পাইল/brace-paren/file-count আবার যাচাই, রিপোর্ট, **নতুন zip**
   (dotfile সহ), progress log-এ এই সেকশনটাকে "✅ সম্পন্ন"-এ আপডেট করা, তারপর ব্যবহারকারীর
   confirmation নিয়ে ধাপ ৪-এ যাওয়া।

### এই zip সম্পর্কে গুরুত্বপূর্ণ নোট:

এই zip-টা **ধাপ ৩ অসম্পূর্ণ অবস্থাতেই** দেওয়া হচ্ছে (ব্যবহারকারীর স্পষ্ট অনুরোধে, যাতে এই
মুহূর্ত পর্যন্ত local Kotlin-সাইড কাজ হারিয়ে না যায়)। **local ১৪টা creation site + entity +
Room migration সম্পূর্ণ ও self-consistent**, কিন্তু Supabase RPC dual-write এখনো হয়নি —
অর্থাৎ এই zip দিয়ে build/deploy করলে local role tracking কাজ করবে, কিন্তু ধাপ ২-এর সিদ্ধান্ত
#১ (RPC-ও role লিখবে) এখনো বাস্তবায়িত হয়নি, cloud-এ role sync হবে না যতক্ষণ না উপরের #১-৩ শেষ
হয়।


## ধাপ ৩ (বাকি অংশ) — Supabase RPC dual-write + DTO/mapper wiring ✅ সম্পন্ন

এই সেশনে আগের সেশনের "IN PROGRESS" ধাপ ৩ চালিয়ে নেওয়া হলো (local Kotlin অংশ আগেই শেষ ছিল)।

### যা করা হয়েছে:

1. **লাইভ Supabase-এর RPC সংজ্ঞা read-only ভাবে verify করা হয়েছে** (`mcp Supabase execute_sql`,
   শুধু `SELECT pg_get_functiondef(...)` — কোনো লেখা/apply হয়নি, শুধু পড়া হয়েছে) কারণ
   `docs/RPC_INVENTORY_REPORT.md` অনুযায়ী এই টাকা-সংক্রান্ত ১৪টা RPC-র কোনোটার সংজ্ঞাই
   `supabase/migrations/*.sql`-এ লেখা নেই (আলাদা RPC_SYNC_FIX প্রজেক্টের অডিট-করা সমস্যা) —
   তাই এই RPC-গুলোতে role যোগ করার আগে তাদের **আসল বর্তমান বডি** verify করে নেওয়া বাধ্যতামূলক
   ছিল, নাহলে অনুমান করে migration লিখলে rule #৪ (destructive/silent overwrite নিষেধ) ভঙ্গ হতো।
2. **নতুন migration ফাইল লেখা হয়েছে** (`supabase/migrations/step36_transaction_role_column_and_rpc_dual_write.sql`)
   — **শুধু লেখা হয়েছে, apply করা হয়নি** (rule #৩, ব্যবহারকারীর কাজ):
   - `public.transactions`-এ `role text not null default ''` কলাম (`add column if not exists`,
     বিদ্যমান row না ভেঙে)।
   - ১২টা RPC-এর `CREATE OR REPLACE FUNCTION` — প্রতিটাতে শুধু `public.transactions`-এ INSERT
     হওয়া জায়গায় `role` কলাম + মান যোগ করা হয়েছে, বাকি লজিক verbatim অপরিবর্তিত:
     `accept_bid`(USER), `admin_adjust_balance` **দুই ওভারলোডেই** (৪-arg → `''`, ৫-arg →
     `p_role`), `admin_confirm_gateway_deposit`(`v_pay.role`), `admin_reconcile_user_balances`
     (দুইটা আলাদা ব্লক: USER/SOLVER), `process_withdrawal`(`v_wd.role`), `refund_escrow_once`
     (USER), `release_escrow`(SOLVER), `request_wallet_deposit`(`p_role`), `request_withdrawal`
     (`p_role`), `respond_additional_charge`(USER), `user_confirm_extra_amount`(USER)।
   - `admin_refund_and_reopen_problem`/`request_extra_amount`/`user_reject_extra_amount` —
     কোনো transaction insert করে না, তাই স্পর্শ করা হয়নি।
3. **`TransactionDto.kt`-এ `val role: String = ""` যোগ**, **`MessageTransactionMappers.kt`-এর
   `toTransactionEntity()`-এ `role = role` pass-through যোগ** — এখন cloud থেকে sync হওয়া
   transaction-এ role আসবে (migration apply + RPC-গুলো role লেখা শুরু করার পর)।

### ⚠️ দুটো নতুন আবিষ্কার (এই ধাপের স্কোপের বাইরে, ছোঁয়া হয়নি, আলাদাভাবে জানানো হলো):

1. **`admin_adjust_balance`-এর লাইভ DB-তে দুটো আলাদা overload আছে** — পুরনো ৪-প্যারামিটার
   (কোনো `p_role` নেই) আর নতুন ৫-প্যারামিটার (`p_role default 'USER'`)। Kotlin-সাইড
   (`SupabaseSyncManager.adminAdjustBalance`) `role` null হলে `p_role` একেবারেই পাঠায় না —
   PostgREST-এর দিক থেকে তখন দুটো ওভারলোডই ম্যাচ করতে পারে (৪-arg সরাসরি, ৫-arg
   default দিয়ে), যেটা runtime-এ "ambiguous function" এরর দিতে পারে। এটা প্রি-এক্সিস্টিং বাগ,
   এই migration-এ শুধু দুটো ওভারলোডেই role-ট্যাগিং যোগ হয়েছে, ওভারলোড-দ্বন্দ্ব সমাধান হয়নি
   (সমাধান করতে হলে পুরনো ওভারলোড বাদ দেওয়া লাগবে, যেটা rule #৩ অনুযায়ী স্পষ্ট অনুমতি ছাড়া করা
   যাবে না)।
2. **`user_confirm_extra_amount()` এখনও `v_user.balance` (shared/legacy কলাম) থেকে deduction
   হিসাব করে**, তার sibling `respond_additional_charge()`-এর মতো `balance_user`-ভিত্তিক না।
   এটা একটা role-separation বাগ, কিন্তু এই migration-এ (যা শুধু role-ট্যাগিং করছে) স্কোপের
   বাইরে — শুধু role='USER' ট্যাগ করা হয়েছে, balance-হিসাব-লজিক অপরিবর্তিত।

### যাচাই করা হয়েছে:

- `TransactionDto.kt`/`MessageTransactionMappers.kt` brace/paren balance: দুটোই balanced।
- migration SQL-এ প্রতিটা edited `INSERT INTO public.transactions`-এর column-list ও
  values-list-এর সংখ্যা ম্যানুয়ালি গুনে মিলিয়ে দেখা হয়েছে (১২টা ফাংশনের ১৩টা insert
  statement-ই, ভুল কলাম-কাউন্ট নেই)।
- ১২টা `CREATE OR REPLACE FUNCTION` / ২৪টা `$function$` marker (১২ open + ১২ close) —
  সংখ্যা মিলেছে।
- file count: এই সেশনের আগে ২৬৪টা ফাইল, এই সেশনে ১টা নতুন migration ফাইল যোগ হওয়ায় ২৬৫টা —
  প্রত্যাশিত।
- `gradle build`/আসল SQL execution এই sandbox-এ চালানো যায়নি — **ব্যবহারকারীকে
  migration ফাইলটা রিভিউ করে নিজে apply করতে হবে, এবং Android প্রজেক্ট কম্পাইল করে
  নিশ্চিত হতে হবে।**

### কী টেস্ট/করণীয় (ব্যবহারকারীর পরবর্তী পদক্ষেপ):

1. `step36_transaction_role_column_and_rpc_dual_write.sql` রিভিউ করা, বিশেষভাবে
   `admin_adjust_balance`-এর দুই-overload অংশটা — চাইলে এই ধাপেই বা আলাদা ছোট ফিক্স-রাউন্ডে
   ওভারলোড-দ্বন্দ্ব সমাধানের সিদ্ধান্ত নেওয়া।
2. migration নিজে apply করা (Supabase dashboard/CLI বা আমাকে `apply_migration` দিয়ে explicit
   অনুমতি দিয়ে বলা)।
3. Android প্রজেক্ট কম্পাইল করে `TransactionDto`/mapper পরিবর্তন যাচাই করা।
4. Migration apply হওয়ার পর একটা নতুন deposit/release/refund করে cloud sync-এ role ঠিকভাবে
   আসছে কিনা verify করা (অন্য ডিভাইসে/reinstall করে দেখলে সবচেয়ে ভালো)।

**ধাপ ৩ এখন সম্পূর্ণ সম্পন্ন (local + Supabase migration ফাইল, শুধু apply বাকি)।**
Confirmation-এর অপেক্ষায় — ঠিক থাকলে zip নামিয়ে ধাপ ৪-এ যাওয়ার অনুমতি দিন।

## ধাপ ৩ — migration apply ✅ সম্পন্ন (২০২৬-০৯-১৮, ০২:৩২ +06:00, Asia/Dhaka)

- `step36_transaction_role_column_and_rpc_dual_write.sql`-এর কন্টেন্ট **হুবহু, কোনো পরিবর্তন
  ছাড়া** `apply_migration` টুল দিয়ে লাইভ Supabase প্রজেক্টে (`somadhan`, ref
  `mghvvpndkxnscwryfkib`) apply করা হয়েছে — migration নাম
  `step36_transaction_role_column_and_rpc_dual_write`।
- **Verify করা হয়েছে:**
  - `information_schema.columns` কোয়েরি করে নিশ্চিত হওয়া গেছে `public.transactions`-এ `role`
    কলাম যোগ হয়েছে।
  - `pg_get_functiondef` দিয়ে `accept_bid`-এর পূর্ণ সংজ্ঞা পড়ে নিশ্চিত হওয়া গেছে `role='USER'`
    লেখা হচ্ছে transactions insert-এ।
  - বাকি ১১টা ফাংশন (দুটো `admin_adjust_balance` ওভারলোড-সহ মোট ১২টা `CREATE OR REPLACE
    FUNCTION`) `pg_proc`/`pg_get_functiondef` দিয়ে চেক করে নিশ্চিত হওয়া গেছে সবগুলোতেই role
    কলাম-সংক্রান্ত রেফারেন্স আছে।
- **⚠️ migration-এ flagged, এখনো touch করা হয়নি (আগের নোট অনুযায়ী):** `admin_adjust_balance`-এর
  দুটো overload একসাথে থাকা প্রি-এক্সিস্টিং সম্ভাব্য bug, এবং `user_confirm_extra_amount()`
  এখনও legacy shared `balance` থেকে হিসাব করে — দুটোই এই migration-এর স্কোপের বাইরে, শুধু
  জানানোর জন্য repeat করা হলো।
- migration apply সফল হয়েছে বলে এই zip-এ updated progress log দেওয়া হলো।

## ধাপ ৪ — `reconcileUserBalances()` role-aware করা ✅ সম্পন্ন

- **বদলানো ফাইল:** `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` (শুধু
  এই একটা ফাইল — schema/DAO-তে কোনো নতুন কিছু লাগেনি, `addBalanceForUserRole`/
  `SolverRole`/`deductBalanceForUserRole`/`SolverRole` ধাপ ১ থেকেই বিদ্যমান ছিল)।

### যা করা হয়েছে:

1. **`ledgerByUser: HashMap<String, Double>` বাদ দিয়ে `ledgerByUserRole:
   HashMap<Pair<String, String>, Double>`** (key = `targetId to role`) — প্রতিটা
   `TransactionEntity`-র `role` ফিল্ড (ধাপ ৩) ব্যবহার করে সঠিক role-bucket-এ netAmount যোগ হয়।
   `BALANCE_RECONCILIATION` টাইপ আগের মতোই সম্পূর্ণ বাদ (নিজের mismatch perpetuate করা থেকে
   বাঁচাতে) — লজিক অপরিবর্তিত, শুধু জায়গা বদলেছে (এখন loop-এর `continue`)।
2. **Legacy blank-role (`role == ""`) row হ্যান্ডলিং** — নতুন প্রাইভেট ফাংশন
   `resolveLegacyTransactionRole(trx)` যোগ হয়েছে, `TRANSACTION_ROLE_FIELD_DESIGN.md` #৫-এর
   অপশন B (আপনার কনফার্মড সিদ্ধান্ত) হুবহু implement করে — `type` থেকে role অনুমান, শুধু
   read-time/in-memory (কোনো destructive DB write না, rule #৪ মেনে):
   - `PAYMENT` → `SOLVER`; `WALLET_DEPOSIT`/`REFUND`/`BID_ACCEPT_DEDUCTION`/
     `RELEASE_DEDUCTION`/`EXTRA_CHARGE_DEDUCTION`/`DUPLICATE_CORRECTION` → `USER`;
     `WITHDRAWAL_DEDUCTION`/`WITHDRAWAL_REFUND` → `SOLVER`; `ADMIN_ADJUSTMENT`/অজানা → `null`
     (truly unclassified)।
   - `null` হলে ওই transaction-টা কোনো role-এর ledger sum-এ যোগ হয় না — শুধু
     `unclassifiedLegacyCount`/`unclassifiedLegacyNetAmount` (নতুন, `BalanceReconciliationReport`-এ
     যোগ হওয়া দুটো ফিল্ড) counter-এ যোগ হয়, informational, এবং dry-run লগে একটা সামারি লাইন হিসেবে
     দেখানো হয়।
3. **প্রতিটা ইউজারের জন্য এখন দুইবার লুপ (`"USER"`, `"SOLVER"`)** — প্রতিবার
   `user.balanceUser`/`user.balanceSolver` (আগের merged shared `user.balance` না) বনাম
   role-scoped `ledgerSum` তুলনা হয়। দুই role-এই আলাদা আলাদা mismatch পাওয়া গেলে দুটো আলাদা
   `BalanceMismatchItem` যোগ হয় (নতুন `role` ফিল্ড সহ — নিচে দেখুন), একটার correction আরেকটাকে
   প্রভাবিত করে না।
4. **Correction path role-aware** — আগে `correctionRole` ইউজারের *বর্তমান active* `user.role`
   থেকে অনুমান করা হতো (ভুল হতে পারত — মাস্টার প্রম্পট নিজেই এই ঝুঁকি ফ্ল্যাগ করেছিল); এখন
   loop-এর `role` ভ্যারিয়েবলই (যেটা ওই নির্দিষ্ট mismatch-টা আসলে কোন role-এর) সরাসরি ব্যবহার
   হয় সংশোধনের জন্য — `addBalanceForUserRole`/`SolverRole`/`deductBalanceForUserRole`/
   `SolverRole` (ইতিমধ্যেই বিদ্যমান, নতুন কিছু বানাতে হয়নি)। correction
   `TransactionEntity`-র `role` ফিল্ড এখন এই নির্ধারিত role দিয়ে সঠিকভাবে বসে (আগের মতো
   `correctionRole ?: ""` fallback-blank না)।
5. **Data class আপডেট:**
   - `BalanceMismatchItem`-এ নতুন `val role: String` ফিল্ড (কোনো default না — একমাত্র
     construction site এই ফাংশনের ভেতরেই, তাই সব জায়গায় explicit)।
   - `BalanceReconciliationReport`-এ নতুন `unclassifiedLegacyCount: Int = 0` ও
     `unclassifiedLegacyNetAmount: Double = 0.0` (default-সহ, backward-compatible)।
   - `AdminEscrowView.kt`/`SomadhanViewModel.kt`-এর বিদ্যমান ব্যবহার (`item.userName`,
     `item.storedBalance`, `item.ledgerBalance`, `item.difference`) অপরিবর্তিত থাকা ফিল্ডের
     ওপরই নির্ভর করে, তাই এই দুই UI ফাইল স্পর্শ না করেই কম্পাইল হওয়ার কথা।

### ⚠️ একটা গুরুত্বপূর্ণ পূর্ব-বিদ্যমান বাগ এই ধাপে স্পষ্ট হয়েছে (এই ধাপেই ফিক্স হয়ে গেছে, আলাদা
করে জানানো হলো):

আগের কোড `user.balance` (শেয়ার্ড কলাম)-কে merged ledger sum-এর সাথে তুলনা করত। কিন্তু
`user.balance` আসলে merged sum **না** — এটা "এই মুহূর্তে ডিভাইসে active `user.role`-এর
balance"-এর mirror মাত্র (দেখুন `AppDaos.kt`-এর `addBalanceForUserRole`/`SolverRole`-এর
cross-role-mix-বিরোধী `CASE WHEN role = '...' THEN balance ± amount ELSE balance END` গার্ড,
ধাপ ১-এরও আগের একটা বাগ-ফিক্স রাউন্ডে যোগ হয়েছিল)। তাই dual-role ইউজারদের জন্য আগের
reconciliation আসলে তুলনা করছিল "active role-এর balance" বনাম "USER+SOLVER দুই role-এর
transaction মিলিয়ে merged sum" — এই দুটো সম্পূর্ণ ভিন্ন জিনিস, ফলে ভুল mismatch (বা কাকতালীয়
ভুল মিল) দুটোই হতে পারত। এই ধাপের role-split রিফ্যাক্টরেই এই বাগটা স্বয়ংক্রিয়ভাবে বন্ধ হয়ে
গেছে (এখন সবসময় `balanceUser`/`balanceSolver`-ই তুলনা হয়)।

### dry-run আগে/পরে তুলনা সম্পর্কে সততার সাথে একটা সীমাবদ্ধতা:

মাস্টার প্রম্পট ধাপ ৪-এ বলেছে dry-run চালিয়ে আগে/পরে log তুলনা করে রিপোর্টে দেখাতে। এই
sandbox-এ (আগের প্রতিটা ধাপের মতোই) আসল app চালানো বা আসল ডিভাইস/cloud ডেটার ওপর
`reconcileUserBalances()` রান করা সম্ভব না (কোনো emulator/build environment নেই)। তাই এই
তুলনাটা বাস্তব ডেটা দিয়ে করা যায়নি — উপরের বাগ-বিশ্লেষণটাই কোড-লেভেলে "আগে ভুল কী হতো, এখন
কীভাবে ঠিক হলো" তার ব্যাখ্যা। **আপনাকে নিজের ডিভাইসে dry-run (ডিফল্ট, অটো-ট্রিগার বা admin
প্যানেল থেকে ম্যানুয়ালি) চালিয়ে Logcat-এ (`SomadhanRepo` ট্যাগ) নতুন per-role log লাইনগুলো
দেখে যাচাই করতে হবে** — বিশেষভাবে যেসব ইউজারের আগে (এই ফিক্সের আগের zip-এ) merged mismatch
রিপোর্ট হয়েছিল, তাদের এখন role-split করার পর সংখ্যা কী দাঁড়ায় সেটা তুলনা করা।

### যাচাই করা হয়েছে:

- brace balance: edit-এর আগে-পরে দুটোই সমান (১৬৬৬/১৬৬৬)।
- paren balance: diff -৯ (কমেন্ট/স্ট্রিং লিটারেল থেকে, আগের প্রতিটা ধাপেও একই পর্যবেক্ষণ) —
  edit-এর আগে-পরে অপরিবর্তিত, কোনো নতুন imbalance যোগ হয়নি।
- file count: এই ধাপে কোনো নতুন ফাইল তৈরি হয়নি (শুধু একটা বিদ্যমান .kt ফাইল এডিট + এই progress
  log আপডেট) — ২৬৫টা ফাইল (আগের zip-এর মতোই) অপরিবর্তিত।
- `BalanceMismatchItem`/`BalanceReconciliationReport`-এর অন্য সব ব্যবহার (grep করে
  `AdminEscrowView.kt`, `SomadhanViewModel.kt`) শুধু বিদ্যমান ফিল্ড পড়ে, নতুন কোনো
  construction site নেই — তাই এই দুই ফাইল স্পর্শ করা হয়নি।
- পূর্ণ `gradle build`/আসল রান sandbox-এ সম্ভব হয়নি (আগের ধাপগুলোর মতোই) — শুধু syntax-level
  (brace/paren + ম্যানুয়াল রিভিউ) যাচাই। **ব্যবহারকারীকে কম্পাইল করে নিশ্চিত হতে হবে।**

### ইচ্ছাকৃতভাবে যা বদলানো হয়নি (স্কোপের বাইরে):

- `AdminEscrowView.kt`-এর mismatch-list UI এখনো `role` দেখায় না (শুধু userName/storedBalance/
  ledgerBalance/difference) — একই userId-এর দুটো role-ভিত্তিক আইটেম এখন পাশাপাশি দেখা যাবে
  role লেবেল ছাড়াই, যা বিভ্রান্তিকর হতে পারে। এই ধাপের প্রম্পট শুধু `reconcileUserBalances()`
  ফাংশন উল্লেখ করেছে, UI ফাইল না — তাই rule #১ (শুধু বর্ণিত স্কোপে হাত দেওয়া) মেনে UI স্পর্শ
  করা হয়নি। **আলাদাভাবে জানানো হলো — চাইলে এখনই একটা ছোট UI ফিক্স (role ব্যাজ/লেবেল যোগ করা)
  আলাদা অনুমতি দিয়ে করানো যায়, নাহলে ধাপ ৫/৭-এর সাথে একসাথে বিবেচনা করা যায়।**
- `admin_reconcile_user_balances` Supabase RPC (dual-write, নিচে অপরিবর্তিত কল) — ধাপ ৩-এর
  migration অনুযায়ী ইতিমধ্যেই role-scoped, এই ধাপে ছোঁয়া হয়নি।

### কী টেস্ট করা উচিত (ব্যবহারকারীর পরবর্তী পদক্ষেপ):

1. প্রজেক্ট কম্পাইল করা।
2. dual-role টেস্ট ইউজারের ওপর `reconcileUserBalances(dryRun=true)` (auto-trigger বা admin
   panel থেকে ম্যানুয়ালি) চালিয়ে Logcat-এ per-role log লাইন দেখা।
3. যদি সম্ভব হয়, ইচ্ছাকৃতভাবে একজন dual-role ইউজারের একটা role-এ mismatch তৈরি করে (যেমন
   টেস্ট ডেটাতে সরাসরি DB-তে `balanceUser` বদলে) `dryRun=false` চালিয়ে দেখা শুধু সেই role-ই
   সংশোধন হচ্ছে কিনা, অন্য role-এর balance/transaction অপ্রভাবিত থাকছে কিনা।
4. Logcat-এ `unclassifiedLegacyCount` লগ লাইন (যদি থাকে) দেখে বোঝা পুরনো কত transaction এখনো
   "UNCLASSIFIED_LEGACY" অবস্থায় আছে।

Confirmation-এর অপেক্ষায় — ঠিক থাকলে zip নামিয়ে ধাপ ৫-এ যাওয়ার অনুমতি দিন।

## ধাপ ৪ ফলো-আপ — AdminEscrowView.kt role-চিপ (আপনার অনুরোধে, ছোট UI ফিক্স) ✅ সম্পন্ন

- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminEscrowView.kt`
- **কী করা হয়েছে:** ব্যালেন্স-রিকনসিলিয়েশন mismatch-লিস্টে (`reconcileReport.items.take(20)`
  লুপ) প্রতিটা আইটেমের নাম-এর পাশে এখন একটা ছোট role-চিপ ("ইউজার"/"সলভার", `item.role` থেকে)
  দেখানো হচ্ছে — আগে `Column`-এর ভেতরে দুইটা `Text` ছিল (নাম, তারপর স্টোরড/লেজার লাইন); এখন
  প্রথম `Text`-টা একটা `Row`-এ mov করে তার পাশে চিপ `Text` যোগ হয়েছে, স্টোরড/লেজার লাইন
  অপরিবর্তিত।
- **কেন লাগল:** ধাপ ৪-এ `BalanceMismatchItem`-এ নতুন `role` ফিল্ড যোগ হয়েছিল, ফলে একই userId
  এখন দুইবার (USER + SOLVER আলাদা mismatch) তালিকায় আসতে পারে — চিপ ছাড়া কোনটা কোন role-এর
  তা আলাদা করা যেত না।
- **স্টাইল:** existing color token-ই ব্যবহার হয়েছে (`SomadhanDivider` ব্যাকগ্রাউন্ড,
  `SomadhanTextSecondary` টেক্সট) — কোনো নতুন color/component বানাতে হয়নি। প্রয়োজনীয় import
  (`androidx.compose.foundation.background`, `androidx.compose.ui.Alignment`) ফাইলে আগে থেকেই
  ছিল।
- **কোনো data/logic বদলায়নি** — শুধু একই `item.role` মান display করা হয়েছে, ফিল্টারিং/লজিক
  একই।
- **যাচাই করা হয়েছে:** brace balance ৩৬৩/৩৬৩ (সমান), paren balance ৯৮২/৯৮২ (সমান) —
  edit-এর আগে-পরে কোনো নতুন imbalance নেই। file count ২৬৫ (অপরিবর্তিত)।
- **টেস্ট করা উচিত:** Admin panel-এ Balance Reconciliation audit চালিয়ে (কমপক্ষে একজন
  dual-role ইউজারের দুই role-এই mismatch তৈরি করে টেস্ট করলে সবচেয়ে ভালো) দেখা চিপ ঠিকভাবে
  "ইউজার"/"সলভার" দেখাচ্ছে কিনা, লেআউট ভাঙছে না কিনা।

## ধাপ ৫ — Transaction History (UI + DAO) role অনুযায়ী আলাদা করা

### কনফার্ম করা প্রশ্নের উত্তর (কোড লেখার আগে নেওয়া হয়েছে):
1. User-role history-তে শুধু সেই account-এর USER-role transaction-ই দেখানো হবে — PAYMENT-এর
   মতো টাইপ, যেখানে এই account job poster হিসেবে জড়িত থাকলেও role টেকনিক্যালি SOLVER, সেটা
   দেখানো হবে না। দুই role সম্পূর্ণ আলাদা, শুধু নাম/ছবি/identity শেয়ার করে, তার বাইরে কোনো
   সংযোগ নেই (ব্যবহারকারীর নিজের ভাষায়)।
2. Solver-role-এও একই নিয়ম উল্টো দিক থেকে।
3. পুরনো (ধাপ ৩-এর আগের) blank-role transaction: "business model অনুযায়ী যেটা best" —
   টাইপ থেকে role অনুমানযোগ্য হলে (resolveLegacyTransactionRole) সেই role-এর history-তে;
   সত্যিকারের অনির্ধারণযোগ্য হলে (ADMIN_ADJUSTMENT/BALANCE_RECONCILIATION legacy কেস) দুই
   role-এর history-তেই দেখানো হবে (বাদ দিলে টাকা "হারিয়ে যাওয়ার" মতো মনে হতো) — এটা
   reconcileUserBalances()-এর UNCLASSIFIED_LEGACY আচরণ (ledger-sum থেকে বাদ) থেকে ইচ্ছাকৃতভাবে
   আলাদা, কারণ এখানে শুধু read-only display, ভুল balance-correction-এর আর্থিক ঝুঁকি নেই।

### বদলানো ফাইল:

- **`app/src/main/java/com/example/util/TransactionHelper.kt`** — তিনটা নতুন ফাংশন যোগ:
  - `resolveLegacyTransactionRole(trx)` — `SomadhanRepository.resolveLegacyTransactionRole()`
    (ধাপ ৪)-এর হুবহু কপি, util লেয়ারে UI স্ক্রিনগুলোর জন্য পুনরায় ব্যবহারযোগ্য করা হলো।
    **ইচ্ছাকৃত ঝুঁকি:** Repository-র প্রাইভেট ভার্সনটা এই ধাপের স্কোপের বাইরে বলে (General Rule
    #১, শুধু বর্ণিত ফাইলে হাত দেওয়া) touch করা হয়নি — এখন দুই জায়গায় একই ম্যাপিং-এর দুটো কপি
    আছে, ভবিষ্যতে কোনো টাইপ-role ম্যাপিং বদলালে দুই জায়গাতেই বদলাতে হবে।
  - `effectiveRole(trx)` — `trx.role.ifBlank { resolveLegacyTransactionRole(trx) }`।
  - `matchesRoleForHistory(trx, targetRole, accountId)` — identity ম্যাচ (userId/solverId ==
    accountId) + role ম্যাচ (effectiveRole == targetRole, অথবা truly-unclassified হলে
    null-ও মিলবে) একসাথে যাচাই করে। এটাই ধাপ ৫-এর মূল নতুন লজিক, সব UI ফিক্স এটা দিয়ে হয়েছে।

- **`app/src/main/java/com/example/data/dao/AppDaos.kt`** — `TransactionDao`-এর ৪টা query:
  - `getTransactionsForUser`/`getTransactionsForUserPage` থেকে পুরনো
    `(solverId = :userId AND type != 'REFUND')` হ্যাক সরানো হলো — এই হ্যাকটা শুধু REFUND টাইপ
    বাদ দিত, কিন্তু PAYMENT-এর মতো অন্য SOLVER-role টাইপ ঠিকই ঢুকে যেত (মূল বাগ, role-based
    বাছাই এখন caller-সাইডে `matchesRoleForHistory` দিয়ে হয়)। এখন শুধু ব্রড identity ম্যাচ
    (`userId = :userId OR solverId = :userId`)।
  - `getTransactionsForSolver`/`getTransactionsForSolverPage` — আগে শুধু `solverId` কলাম চেক
    করতো, কিন্তু WITHDRAWAL_DEDUCTION/WITHDRAWAL_REFUND (role=SOLVER) আসলে `userId` কলামে
    solver-এর id রাখে — এই দুটো ফাংশনের বর্তমানে কোনো caller নেই বলে বাগটা এতদিন ধরা পড়েনি,
    এখন `getTransactionsForUser`-এর মতোই দুই কলামেই ব্রড ম্যাচ করা হয়।

- **`app/src/main/java/com/example/ui/screens/TransactionHistoryScreen.kt`** —
  `allTransactionsList` এখন `TransactionHelper.matchesRoleForHistory()` দিয়ে ফিল্টার হয় (আগে
  শুধু identity ম্যাচ, active role দেখা হতো না — Solver মোডে `filteredTransactions = if
  (isSolver) sortedTransactions` কোনো ফিল্টারই করতো না, এটাই মাস্টার প্রম্পটে বর্ণিত মূল বাগ)।
  ডাউনস্ট্রিম sub-tab (PAYMENT/DEPOSIT/REFUND) ফিল্টার আর count-গুলো থেকে এখন-অপ্রয়োজনীয়
  `it.userId == currentUserId` চেক সরানো হয়েছে, কারণ `sortedTransactions` ইতিমধ্যেই
  role-স্কোপড।

- **`app/src/main/java/com/example/ui/screens/UserWalletScreen.kt`** — একই বাগ (এই স্ক্রিন
  User ও Solver দুই মোডেই দেখানো হয়, কিন্তু `sortedUserTransactions`-এ কোনো role ফিল্টার
  ছিল না) একই নিয়মে ঠিক করা হলো — `matchesRoleForHistory` ব্যবহার করে, sub-tab/count থেকে
  একই রকম redundant identity-চেক সরানো হয়েছে।

- **`app/src/main/java/com/example/ui/screens/DashboardScreen.kt`** (`SolverDashboardContent`)
  — দুই জায়গা:
  - `recentTransactions` (ড্যাশবোর্ডের "সাম্প্রতিক লেনদেন" কার্ড) — আগে
    `(solverId == currentSolverId && type != REFUND) || userId == currentSolverId` — শেষ অংশটা
    এই account-এর USER-role transaction-ও (deposit, bid-accept deduction ইত্যাদি) দেখিয়ে
    ফেলত। এখন শুধু `matchesRoleForHistory(it, "SOLVER", currentSolverId)`।
  - সাম্প্রতি সম্পন্ন কাজের earning amount বের করা (`solverTransactions.find { problemId ==
    job.id && type != REFUND }`) — `type != REFUND` যেকোনো non-refund টাইপ (USER-role
    deduction-সহ) ম্যাচ করতে পারত, `.find()` প্রথমটাই নিতো — ভুল amount দেখানোর ঝুঁকি। এখন
    সুনির্দিষ্টভাবে `matchesRoleForHistory(it, "SOLVER", currentSolverId)`।

- **`app/src/main/java/com/example/ui/screens/SolverCompletedJobsScreen.kt`** — ঠিক একই প্যাটার্নের
  ঠিক একই বাগ (`userTransactions.find { problemId == job.id && type != REFUND }`,
  সম্পন্ন কাজের "আয়" দেখানোর জন্য), একই নিয়মে ঠিক করা হলো।

### চেক করা হয়েছে, বাগ পাওয়া যায়নি (কোনো বদল করা হয়নি):
- **`ProblemDetailScreen.kt`** — `EXTRA_CHARGE_DEDUCTION` টাইপ-এক্সাক্ট ফিল্টার, এই টাইপ
  সবসময়ই role=USER (single call-site, TRANSACTION_ROLE_FIELD_DESIGN.md #২ টেবিল) — role
  অস্পষ্টতার কোনো সুযোগ নেই।
- **`InstantJobHistoryScreen.kt`** — চারটা জায়গাতেই (`CompletedSummaryBottomSheet`,
  `UserCancelledSummaryBottomSheet`, `SolverCancelledDetailBottomSheet`,
  `InstantJobHistoryCard`) `type == "REFUND"` টাইপ-এক্সাক্ট lookup, REFUND সবসময়ই role=USER —
  একই কারণে নিরাপদ।

### কোথায় ইচ্ছাকৃতভাবে পুরনো behavior রয়ে গেছে:
- `SomadhanRepository.kt`-এর প্রাইভেট `resolveLegacyTransactionRole()` (ধাপ ৪) অপরিবর্তিত —
  এই ধাপের বর্ণিত স্কোপ শুধু DAO + স্ক্রিনগুলো, তাই touch করা হয়নি (দুই কপির drift-ঝুঁকি উপরে
  আলাদা করে উল্লেখ করা হয়েছে)।
- `getTransactionsForUser`/`getTransactionsForSolver` DAO query দুটো এখন SQL-এর দিক থেকে
  identical (দুটোই ব্রড `userId OR solverId` ম্যাচ) — আসল role-বাছাই সম্পূর্ণ Kotlin-সাইডে
  (`matchesRoleForHistory`) হয়, কারণ blank-legacy row-এর টাইপ-ভিত্তিক role-resolution
  pure SQL-এ (দ্বিতীয় কপি ছাড়া) করা সম্ভব না — এটা ধাপ ৪-এ established প্যাটার্নের (in-memory
  resolution) সাথে সামঞ্জস্যপূর্ণ রাখতে ইচ্ছাকৃত সিদ্ধান্ত।

### যাচাই করা হয়েছে:
- সব বদলানো ফাইলে brace/paren balance সমান (TransactionHelper.kt 40/40, 140/140; AppDaos.kt
  16/16, 575/575; TransactionHistoryScreen.kt 309/309, 1005/1005; UserWalletScreen.kt 410/410,
  1454/1454; DashboardScreen.kt 157/157, 464/464; SolverCompletedJobsScreen.kt 92/92, 254/254)।
- File count: ৩১৩ (unchanged, ২৬৫ ফাইল + ৪৮ ডিরেক্টরি — আগের ধাপের zip-এর সাথে মিলছে)।
- Gradle compile করা হয়নি (এই environment-এ Android SDK নেই, আগের ধাপগুলোর মতোই শুধু
  brace-balance + ম্যানুয়াল কোড রিভিউ)।

### কী টেস্ট করা উচিত (ব্যবহারকারীর পরবর্তী পদক্ষেপ):
1. প্রজেক্ট কম্পাইল করা (Android Studio/Gradle-এ, এই environment-এ করা যায়নি)।
2. একই dual-role account-এ: User মোডে একটা deposit করে, তারপর Solver মোডে switch করে
   Transaction History/ড্যাশবোর্ড/ওয়ালেট স্ক্রিনে সেই deposit-টা **না** দেখা (আগে দেখাতো)।
3. একটা কাজ সম্পন্ন করে (solver হিসেবে payment পাওয়ার পর) User মোডে সেই PAYMENT transaction-টা
   history-তে **না** দেখা, Solver মোডে দেখা।
4. SolverCompletedJobsScreen/DashboardScreen-এ সদ্য-সম্পন্ন কাজের "আয়" amount সঠিক PAYMENT
   amount দেখাচ্ছে কিনা (আগে ভুল amount ধরারও সুযোগ ছিল bid-accept deduction amount চলে এলে)।
5. যদি সম্ভব হয়, ধাপ ৩-এর আগের (blank-role) কোনো পুরনো টেস্ট transaction থাকলে সেটা এখনো
   history-তে ঠিক role-এ (বা ADMIN_ADJUSTMENT/BALANCE_RECONCILIATION হলে দুই role-এই) দেখা
   যাচ্ছে কিনা।

Confirmation-এর অপেক্ষায় — ঠিক থাকলে zip নামিয়ে ধাপ ৬-এ যাওয়ার অনুমতি দিন।

## ধাপ ৬ — Notification + Admin Audit Log role-scoping ✅ সম্পন্ন

### অংশ ক — NotificationEntity.role
- `NotificationEntity`-তে নতুন `role: String = ""` কলাম (blank = role-নিরপেক্ষ, দুই role-এই
  দেখাবে; পুরনো row ডিফল্ট "" পাবে, ভাঙবে না)।
- `SomadhanRepository.kt`-এর ৮২টা `NotificationEntity(` creation site এক এক করে context পড়ে
  classify করে role বসানো হয়েছে (`"USER"` / `"SOLVER"` / `""` / dynamic এক্সপ্রেশন যেমন
  `depositRole`, `adjustRole`, `if (isSolver) "USER" else "SOLVER"` — যেখানে এই ভ্যারিয়েবল
  আগে থেকেই ওই ফাংশনে ছিল, নতুন করে গণনা করা হয়নি)।
- KYC-সম্পর্কিত ৪টা notification (`submitKyc`, `adminApproveKyc`, `adminRejectKyc`,
  `adminRevokeKyc`) — ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী **`role = "SOLVER"`** (KYC শুধু Solver
  role-এর জন্য প্রাসঙ্গিক)।
- `adminIssueWarningStrike()`-এ নতুন `targetRole: String = ""` প্যারামিটার যোগ হলো (UI-তে
  `AdminDisputeCenterView.kt`-এর `targetParty` আগে থেকেই জানা ছিল কিন্তু ফাংশনে পাস হতো না) —
  ViewModel wrapper দিয়ে end-to-end wire করা হয়েছে।
- `sendManualNotification()` (অ্যাডমিন broadcast) — নিজের `targetRole` প্যারামিটার
  ("USER"/"SOLVER"/"ALL") থেকে `notifRole` গণনা করে ব্যবহার করে ("ALL" = `""`)।
- `NotificationDao`-এর ৬টা query (`getNotificationsForUser`, `...Page`, `getUnreadCount`,
  `getUnreadCountForUser`, `getUnreadProblemNotificationsFlow`) এখন `currentRole` প্যারামিটার
  নেয় (ডিফল্ট `""` — পুরনো caller ভাঙবে না), WHERE ক্লজ: `(:currentRole = '' OR role =
  :currentRole OR role = '')` — অর্থাৎ role-neutral row সবসময় দেখাবে, নাহলে বর্তমান active
  role না মিললে বাদ পড়বে।
- `SomadhanRepository`-এর wrapper ফাংশনগুলো ও `SomadhanViewModel.observeUserData()`/
  `loadNextNotificationsPage()` আপডেট করে বর্তমান active role (`_currentUser.value?.role`)
  পাস করা হয়েছে — যাচাই করা হয়েছে যে role-switch (`switchRole`/`switchRoleInPlace`)-এর ঠিক
  পরেই `observeUserData(updated.id)` আবার কল হয় (আর তার আগেই `_currentUser.value` আপডেট
  হয়ে যায়), তাই নতুন role অনুযায়ী subscription আবার তৈরি হয়ে সঠিক ফিল্টার প্রয়োগ হবে।

### অংশ খ — AdminAuditLogEntity.role
- `AdminAuditLogEntity`-তে নতুন `role: String = ""` কলাম, `logAdminAction()`-এ নতুন
  `role: String = ""` প্যারামিটার (ডিফল্ট, পুরনো caller ভাঙবে না)।
- যে action-গুলোর role আগে থেকেই caller-এর স্কোপে জানা ছিল সেগুলোতেই role ট্যাগ করা হলো
  (নতুন লজিক লেখা হয়নি, বিদ্যমান ভ্যারিয়েবল পুনর্ব্যবহার):
  - `ADD_BALANCE`/`DEDUCT_BALANCE` → `adjustRole`
  - `BAN_USER`/`UNBAN_USER`, `RESTRICT_USER`/`UNRESTRICT_USER`,
    `ADD_VERIFIED_BADGE`/`REMOVE_VERIFIED_BADGE` → ফাংশনের `role` প্যারামিটার (`?: ""`)
  - `ADJUST_REPUTATION` → `applyReputationChange()`-এর মতোই `user.role` থেকে (শুধু
    "USER"/"SOLVER" হলে, নাহলে `""`)
  - `APPROVE_KYC`/`REJECT_KYC`/`REVOKE_KYC`/`UPDATE_KYC_INFO`/`RESET_KYC_PENDING` →
    `"SOLVER"` (অংশ ক-এর একই ব্যবহারকারী-সিদ্ধান্ত অনুযায়ী)
- বাকি role-নিরপেক্ষ action (`DELETE_PROBLEM`, `adminChangeRole`, `adminResetUserPassword`
  ইত্যাদি একাউন্ট-লেভেল action) role ডিফল্ট `""` রয়ে গেছে — ইচ্ছাকৃত, এই ধাপের বর্ণিত স্কোপ
  অনুযায়ী "already-known-role" action-গুলোই ট্যাগ করা হয়েছে।

### Room migration
- `NotificationEntity`/`AdminAuditLogEntity`-তে নতুন কলাম যোগ হওয়ায় (rule #৪ — কোনো
  destructive migration/data loss না) DB version ৫১ থেকে ৫২-এ বাড়িয়ে `MIGRATION_51_52`
  explicitly লেখা ও রেজিস্টার করা হয়েছে (`ALTER TABLE notifications ADD COLUMN role ...`,
  `ALTER TABLE admin_audit_logs ADD COLUMN role ...`, দুটোই `DEFAULT ''`) — যাতে
  `fallbackToDestructiveMigration(dropAllTables = true)` ট্রিগার হয়ে পুরনো ব্যবহারকারীদের
  notification/audit-log ইতিহাস মুছে না যায়।

### কোথায় ইচ্ছাকৃতভাবে পুরনো behavior রয়ে গেছে:
- `MessageTransactionMappers.kt`-এর `NotificationDto.toNotificationEntity()` (Supabase
  realtime sync mapper) role সেট করে না (ডিফল্ট `""`) — কারণ Supabase-সাইড
  `notifications` টেবিল/RPC-তে এখনো `role` কলাম নেই। এর মানে cloud থেকে sync হয়ে আসা
  notification role-নিরপেক্ষ হিসেবে (দুই role-এই) দেখাবে, স্থানীয়ভাবে তৈরি হওয়া
  notification-এর মতো role-scoped হবে না। Supabase migration + DTO/RPC আপডেট এই ধাপের
  বর্ণিত স্কোপের বাইরে বলে ইচ্ছাকৃতভাবে বাদ রাখা হলো (TransactionEntity.role-এর
  Supabase dual-write যেভাবে আলাদা ধাপে করা হয়েছিল, প্রয়োজনে সেই একই প্যাটার্নে পরে করা
  যেতে পারে)।
- `SupabaseSyncManager.logAdminAction()` RPC কলেও role পাস করা হয়নি একই কারণে
  (Supabase-সাইড `admin_audit_logs` টেবিলে role কলাম নেই)।

### যাচাই করা হয়েছে:
- সব বদলানো ফাইলে brace/paren balance আগের zip-এর সাথে মিলছে (শুধু নতুন যোগ করা কোডের
  সমান open/close): `SomadhanRepository.kt` (braces ১৬৬৮/১৬৬৮, parens ৫৪৯৩/৫৫০২ — pre-existing
  ৯-এর গ্যাপ কমেন্টের কারণে অক্ষত), `SomadhanViewModel.kt` (১৩৩০/১৩৩০, ২৮৫৩/২৮৫৩), `AppDaos.kt`
  (১৬/১৬, ৫৮৩/৫৮৩), `AdminDisputeCenterView.kt` (৪৪৮/৪৪৮, ১২৯০/১২৯০), `MarketEntities.kt`
  (৮৬/৮৬), `AdminAuditLogEntity.kt` (৫/৫), `AppDatabase.kt` (৮৬/৮৬, ৫১২/৫১২)।
- স্ক্রিপ্ট দিয়ে যাচাই করা হয়েছে যে ৮২টা `NotificationEntity(` call-সাইটের প্রতিটাতেই এখন
  `role =` আর্গুমেন্ট আছে (missing count: 0)।
- File count: ২৬৫ ফাইল + ৪৮ ডিরেক্টরি (আগের ধাপের zip-এর সাথে মিলছে, নতুন ফাইল যোগ হয়নি)।
- Gradle compile করা হয়নি (এই environment-এ Android SDK নেই, আগের ধাপগুলোর মতোই শুধু
  brace-balance + ম্যানুয়াল কোড রিভিউ)।

### কী টেস্ট করা উচিত (ব্যবহারকারীর পরবর্তী পদক্ষেপ):
1. প্রজেক্ট কম্পাইল করা (Android Studio/Gradle-এ)।
2. পুরনো ডেটাবেস (v৫১) থাকা অবস্থায় আপগ্রেড করে দেখা migration crash ছাড়া চলছে কিনা, আর
   পুরনো notification/audit-log হারিয়ে যাচ্ছে না।
3. dual-role account-এ: User মোডে একটা কাজ পোস্ট করে notification আসা, তারপর Solver মোডে
   switch করে সেই notification **না** দেখা (আগে দেখাতো), আর Solver-নির্দিষ্ট notification
   (bid accept ইত্যাদি) দেখা।
4. KYC জমা/অনুমোদন/বাতিল notification — Solver মোডে দেখা যাচ্ছে আর User মোডে **না** দেখা
   যাচ্ছে কিনা পরীক্ষা করা (role="SOLVER" ট্যাগ, আপনার সিদ্ধান্ত অনুযায়ী)।
5. AdminDisputeCenterView থেকে Warning Strike দিয়ে (targetParty পরিবর্তন করে দুইবার) দেখা
   role সঠিকভাবে notification-এ যাচ্ছে কিনা।
6. Admin Audit Log-এ ব্যালেন্স/ব্যান/রেপুটেশন/KYC action নেওয়ার পর ডেটাবেস ব্রাউজ করে
   (বা ভবিষ্যতে UI যোগ হলে) `role` কলাম সঠিক ভ্যালু পাচ্ছে কিনা।

Confirmation-এর অপেক্ষায় — ঠিক থাকলে zip নামিয়ে ধাপ ৭-এ যাওয়ার অনুমতি দিন।

## ধাপ ৭ — Admin display স্ক্রিনের generic balance ঠিক করা ✅ সম্পন্ন

`ROLE_SEPARATION_AUDIT.md`-এর আইটেম ৩-এ কনফার্মড ৩টা জায়গাই (AdminUsersView.kt:596,
AdminUserLookupView.kt:471, AdminStatsView.kt:1701) একে একে ধরে ঠিক করা হলো।

### ১. AdminUsersView.kt — ব্যালেন্স সমন্বয় ডায়ালগ
- **প্রেক্ষাপট:** এই ডায়ালগ ইতিমধ্যেই role-এর প্রেক্ষাপটে ছিল — per-card dropdown মেনু থেকে
  খোলা হয় যেখানে `card.cardRole` জানা থাকে (ঠিক ban/restrict টগলের মতোই, যেগুলো আগে থেকেই
  `AdminUserRoleCard?` স্টেট ব্যবহার করত)। কিন্তু `userForBalanceAdjust` স্টেট শুধু plain
  `UserEntity?` ছিল, তাই ক্লিকের মুহূর্তের role হারিয়ে যেত।
- **বদলানো ফাইল:**
  - `app/src/main/java/com/example/ui/screens/AdminUsersView.kt`
  - `app/src/main/java/com/example/data/repository/SomadhanRepository.kt`
  - `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt`
- **কী করা হয়েছে:**
  1. `userForBalanceAdjust` স্টেটের টাইপ `UserEntity?` থেকে `AdminUserRoleCard?`-এ বদলানো
     হলো (ban/restrict-এর সাথে সামঞ্জস্যপূর্ণ)।
  2. ডায়ালগে "বর্তমান ব্যালেন্স" এখন generic `target.balance`-এর বদলে role-scoped
     `target.cardBalance`, role লেবেল (গ্রাহক/সলভার) সহ।
  3. `adminAdjustBalance()` (repository + ViewModel) নতুন ঐচ্ছিক `role: String? = null`
     প্যারামিটার পেল (ঠিক `adminSetBanned`/`adminSetRestricted`-এর প্যাটার্নে)। ভেতরে
     `adjustRole` এখন প্রথমে এই এক্সপ্লিসিট `role`-কে প্রাধান্য দেয়, না থাকলে **পুরনো fallback**
     (`targetForAdjust?.role`, অ্যাকাউন্টের বর্তমান সক্রিয় role) অক্ষত রাখা হয়েছে — কোনো পুরনো
     পথ মোছা হয়নি।
  4. ডায়ালগের confirm বাটন এখন `role = target.cardRole` পাস করে — অর্থাৎ dual-role ইউজারের
     ক্ষেত্রে সঠিক role-টাই adjust হবে, অ্যাকাউন্ট বর্তমানে যেই role-এ সক্রিয় থাকুক না কেন।
  5. Dropdown-এর ক্লিক হ্যান্ডলার এখন `userForBalanceAdjust = user`-এর বদলে `= card` সেট করে।
- **আসল বাগ যা এতে ফিক্স হলো:** আগে `adminAdjustBalance()` ভেতরে ভেতরে
  `targetForAdjust?.role` (ইউজার নিজে যে role-এ বর্তমানে সক্রিয়, যেকোনো সময় switch করতে
  পারে) থেকে অনুমান করত কোন balance adjust হবে — admin যে card/role-এ ক্লিক করেছিল সেটা না।
  তাই ইউজার যদি এর মধ্যে অন্য role-এ switch করে ফেলত, admin ভুল role-এর balance
  adjust করে ফেলতে পারত (UI-তে দেখানো balance আর আসলে যা adjust হচ্ছে তা মিলত না)।

### ২. AdminUserLookupView.kt — ব্যালেন্স সমন্বয় ডায়ালগ
- **প্রেক্ষাপট:** একই বাগ, ভিন্ন প্যাটার্নে — এখানে per-card class নেই, বরং
  `userPerspectiveTab`/`currentPerspectiveRole` (ban/restrict টগল এই থেকেই
  `roleForBanToggle`/`roleForRestrictToggle` স্টেটে role সংরক্ষণ করে)।
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminUserLookupView.kt`
- **কী করা হয়েছে:**
  1. নতুন `roleForBalanceAdjust` স্টেট (ঠিক `roleForBanToggle`/`roleForRestrictToggle`-এর
     প্যাটার্নে), "ব্যালেন্স" বাটনের ক্লিকেই `currentPerspectiveRole` থেকে সেট হয়।
  2. ডায়ালগে balance এখন `roleForBalanceAdjust` অনুযায়ী `target.balanceUser`/
     `target.balanceSolver`, role লেবেল-সহ।
  3. Confirm বাটন `viewModel.adminAdjustBalance(..., role = roleForBalanceAdjust)` পাস করে
     (repository/ViewModel-এর নতুন প্যারামিটার, উপরে #১-এ যোগ হয়েছে)।

### ৩. AdminStatsView.kt — ইউজার CSV এক্সপোর্ট
- **প্রেক্ষাপট:** এটা flat per-user এক্সপোর্ট (কোনো card/perspective প্রেক্ষাপট নেই), তাই
  উপরের দুটোর মতো role-selector state যোগ করার সুযোগ নেই — role নির্বাচনের জন্য নতুন UI
  লাগত, যেটা এই ধাপের নির্দেশনা অনুযায়ী স্কোপের বাইরে (শুধু চিহ্নিত করে রাখার কথা)। কিন্তু CSV
  কলাম বিভাজন কোনো নতুন UI/role-selector ছাড়াই সম্ভব, তাই এটা করা হলো।
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminStatsView.kt`
- **কী করা হয়েছে:** এক্সপোর্টের একটাই অস্পষ্ট "Balance" কলামের বদলে দুইটা আলাদা কলাম —
  "Balance (User)" (`u.balanceUser`) আর "Balance (Solver)" (`u.balanceSolver`)।
- **ইচ্ছাকৃতভাবে বাদ:** একই সারিতে থাকা "Reputation" কলাম এখনো generic shared
  `u.reputationScore` — এটা `ROLE_SEPARATION_AUDIT.md`-এ আলাদা করে flag করা "নতুন পাওয়া
  সমস্যা" (generic reputation display, ৯ জায়গা), এই ধাপের ঘোষিত স্কোপ ("generic balance")-এর
  বাইরে বলে ইচ্ছাকৃতভাবে অপরিবর্তিত রাখা হলো।

### যাচাই করা হয়েছে
- সব বদলানো ফাইলে curly-brace balance = 0 (কোনো bracket ভাঙেনি); `SomadhanRepository.kt`-এর
  paren imbalance আগে থেকেই ছিল (Bengali কমেন্ট, পূর্ববর্তী ধাপগুলোতেও নথিভুক্ত), নতুন কোনো
  imbalance যোগ হয়নি।
- File count: ২৬৬ ফাইল (আগের ধাপের zip-এর সাথে মিলছে, নতুন কোনো ফাইল হারায়নি/অপ্রত্যাশিতভাবে
  যোগ হয়নি — শুধু এই progress log আপডেট হয়েছে)।
- Gradle compile করা হয়নি (এই sandbox-এ নেটওয়ার্ক বন্ধ, Android SDK/Gradle distribution
  ডাউনলোড করা যায় না — আগের সব ধাপের মতোই শুধু syntax-level ম্যানুয়াল রিভিউ)।

### কী টেস্ট করা উচিত (ব্যবহারকারীর পরবর্তী পদক্ষেপ)
1. প্রজেক্ট কম্পাইল করা (Android Studio/`./gradlew assembleDebug`) — এই environment-এ করা
   যায়নি।
2. একজন dual-role টেস্ট ইউজার তৈরি করে **Solver মোডে সক্রিয় অবস্থায়** রেখে, admin panel
   (AdminUsersView) থেকে তার **User-কার্ডে** ক্লিক করে ব্যালেন্স যোগ/কর্তন করা — নিশ্চিত হওয়া
   যে ডায়ালগে দেখানো "বর্তমান ব্যালেন্স" আসলেই তার balanceUser (balanceSolver না), আর
   adjustment-এর পর শুধু balanceUser বদলেছে, balanceSolver অপরিবর্তিত।
3. একই টেস্ট AdminUserLookupView-তেও — perspective ট্যাব (User/Solver) বদলে বদলে ব্যালেন্স
   বাটনে ক্লিক করে দেখা সঠিক role-এর balance দেখাচ্ছে ও adjust করছে কিনা।
4. AdminStatsView থেকে CSV এক্সপোর্ট করে ফাইলটা খুলে দেখা "Balance (User)"/"Balance
   (Solver)" দুইটা কলামই সঠিক মান দেখাচ্ছে কিনা।
5. Admin Audit Log-এ ব্যালেন্স সমন্বয়ের পর `role` কলাম এখন card/perspective অনুযায়ী সঠিক
   role পাচ্ছে কিনা (আগে অ্যাকাউন্টের সক্রিয় role থেকে ভুল বসতে পারত)।

Confirmation-এর অপেক্ষায় — ঠিক থাকলে zip নামিয়ে ধাপ ৮-এ যাওয়ার অনুমতি দিন। (reputation-এর
জন্য একই generic-display সমস্যা "ধাপ ৭খ" হিসেবে আলাদাভাবে scope করতে চাইলে জানাবেন, এই
ধাপে touch করা হয়নি।)

---

## ধাপ ৭খ — Reputation-এর জন্য UI display-লেয়ার ফিক্স (ধাপ ৭-এর ঠিক একই প্যাটার্ন)

ধাপ ৭-এ balance-এর জন্য যা করা হয়েছিল, ঠিক সেই একই বাগ ও একই সমাধান এখন
`ROLE_SEPARATION_AUDIT.md`-এ চিহ্নিত রেপুটেশন-সংক্রান্ত ৯টা জায়গায় (৫টা ফাইল) প্রয়োগ করা হলো।

### ০. প্রয়োজনীয় প্লাম্বিং (UI ফিক্সের আগে বাধ্যতামূলক)
- **বদলানো ফাইল:** `SomadhanRepository.kt`, `SomadhanViewModel.kt`
- **কী করা হয়েছে:**
  - `applyReputationChange()`-এ নতুন ঐচ্ছিক প্যারামিটার `role: String? = null` যোগ করা হলো।
    ভেতরের `reputationRole` গণনা এখন প্রথমে এই এক্সপ্লিসিট `role` দেখে, না থাকলে (fallback)
    আগের মতোই `user.role` থেকে অনুমান করে — ঠিক `adminAdjustBalance()`-এর `adjustRole`
    গণনার প্যাটার্নে। এই role RPC dual-write-এও (একই `reputationRole` ভ্যারিয়েবল পুনর্ব্যবহার
    করে) স্বয়ংক্রিয়ভাবে পৌঁছায়।
  - `adminAdjustReputation()` (repository ও viewmodel দুই জায়গাতেই) এখন `role: String? = null`
    প্যারামিটার নেয় এবং সেটা `applyReputationChange()`-এ পাস করে; `logAdminAction()`-এর
    `role` ফিল্ডও এখন এক্সপ্লিসিট role-কে প্রাধান্য দেয় (fallback: `user?.role`)।
  - পুরনো caller-রা (applyCappedPerEventReputation, applyCappedPerProblemReputation,
    triggerDynamicReputationEvent, runInactivityReputationDecay, এবং সব positional-arg কল)
    নতুন প্যারামিটার না পাঠালে ডিফল্ট `null` পায় — behavior অপরিবর্তিত (verify করা হয়েছে,
    সবাই ৫-পজিশনাল আর্গুমেন্ট বা named আর্গুমেন্ট ব্যবহার করে, কোনোটাই ৬ষ্ঠ positional আর্গুমেন্ট
    দেয় না)।

### ১. AdminUsersView.kt — "Reputation Adjustment Dialog" + sort
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminUsersView.kt`
- **কী করা হয়েছে:**
  - `userForReputationAdjust`: `UserEntity?` → `AdminUserRoleCard?` (balance-adjust-এর ঠিক
    একই টাইপ পরিবর্তন)।
  - ডায়ালগ এখন `target.cardReputationScore` (role-scoped) + role লেবেল ("গ্রাহক"/"সলভার")
    দেখায়; সংরক্ষণে `viewModel.adminAdjustReputation(target.user.id, change, note, role =
    target.cardRole)` কল হয়।
  - ড্রপডাউন মেনুর "রেপুটেশন সমন্বয় করুন" ক্লিক-সাইট এখন `card` পাস করে (আগে `user`)।
  - "সবচেয়ে কম রেপুটেশন আগে" sort টগল: `selectedRoleFilter` "USER"/"SOLVER" হলে
    role-scoped কলাম (`reputationScoreUser`/`reputationScoreSolver`) দিয়ে সাজায়; "ALL"-এ
    (কোন role "আসল" তা অস্পষ্ট বলে) পুরনো shared `reputationScore` fallback অপরিবর্তিত রাখা
    হলো — ইচ্ছাকৃত সীমাবদ্ধতা, edge case।
  - (রেপুটেশন হিস্ট্রি বটম-শিট, `selectedUserForReputation`, ইতিমধ্যেই `AdminUserRoleCard` +
    `cardReputationScore` ব্যবহার করছিল — এই ধাপে touch করার দরকার হয়নি।)

### ২. AdminUserLookupView.kt — Reputation Adjust Dialog + perspective tab display
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminUserLookupView.kt`
- **কী করা হয়েছে:**
  - নতুন state `roleForReputationAdjust` (ডিফল্ট "USER") — `roleForBalanceAdjust`-এর ঠিক
    একই প্যাটার্ন। "রেপুটেশন" অ্যাকশন বাটনের onClick এখন ক্লিকের মুহূর্তের
    `currentPerspectiveRole` এতে সংরক্ষণ করে।
  - ডায়ালগ এখন `roleForReputationAdjust` অনুযায়ী role-scoped স্কোর + লেবেল দেখায়;
    `viewModel.adminAdjustReputation(target.id, change, note, role = roleForReputationAdjust)`
    কল হয়।
  - পার্সপেক্টিভ ট্যাব সামারি কার্ডের "রেপুটেশন স্কোর" Text — আগে শেয়ার্ড
    `user.reputationScore` পড়ত (পার্সপেক্টিভ ট্যাব বদলালেও একই সংখ্যা দেখাত, ব্যালেন্স
    লাইনের ঠিক পাশেই থাকা সত্ত্বেও ওটা তখনো role-scope হয়নি বলে এই একই bug এখানেও ছিল) —
    এখন `currentPerspectiveRole` অনুযায়ী `reputationScoreUser`/`reputationScoreSolver`।
  - **ইচ্ছাকৃতভাবে বাদ:** পাশের "ব্যালেন্স" সামারি Text এখনো `user.activeRoleBalance`
    (অ্যাকাউন্টের সক্রিয় role, পার্সপেক্টিভ ট্যাব না) ব্যবহার করে — এটা balance-এর নিজস্ব
    বিদ্যমান সীমাবদ্ধতা (এই ধাপের ঘোষিত স্কোপ শুধু reputation), touch করা হয়নি। এটাও একই ধরনের
    latent বাগ কিনা আলাদাভাবে যাচাই করার সুপারিশ থাকল।

### ৩. AdminStatsView.kt — CSV এক্সপোর্ট + "টপ সলভার" র‍্যাংকিং
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminStatsView.kt`
- **কী করা হয়েছে:**
  - CSV এক্সপোর্টের একটাই অস্পষ্ট "Reputation" কলামের বদলে দুইটা আলাদা কলাম — "Reputation
    (User)" (`u.reputationScoreUser`) আর "Reputation (Solver)" (`u.reputationScoreSolver`) —
    ধাপ ৭-এর Balance কলাম বিভাজনের ঠিক একই প্যাটার্ন, কোনো নতুন UI/selector ছাড়াই। আগের ধাপের
    "ইচ্ছাকৃতভাবে বাদ" নোট (যেটা এই কাজ পরে করার কথা বলেছিল) এখন resolved।
  - "টপ সলভার" কার্ড: এই তালিকা ইতিমধ্যেই `role=="SOLVER" || hasSolverRole` দিয়ে ফিল্টার করা
    — তাই এখানে role আসলে অস্পষ্ট না, শেয়ার্ড `reputationScore`-এর বদলে সরাসরি role-scoped
    `reputationScoreSolver` দিয়ে র‍্যাংক ও স্কোর-ডিসপ্লে দুটোই ঠিক করা হলো (sort +
    score টেক্সট, দুই জায়গা)।

### ৪. AdminProblemsView.kt — সলভার-বাছাই তালিকা (bid দেওয়া সলভারদের কার্ড)
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminProblemsView.kt`
- **কী করা হয়েছে:** এই কার্ডে ঠিক ওপরেই `solver.verifiedBadgeSolver` (role-scoped) আগে থেকেই
  পড়া হচ্ছিল — এই একই কার্ডে "স্কোর" এখন শেয়ার্ড `reputationScore`-এর বদলে role-scoped
  `reputationScoreSolver` (এটা "role-context আছে এমন জায়গা" — এই তালিকার প্রতিটা এন্ট্রি
  অনস্বীকার্যভাবে bid দেওয়া সলভার, তাই সরাসরি role-scoped ফিল্ড, কোনো নতুন লেবেল/UI লাগেনি)।

### ৫. AdminSolverQuotaView.kt — সলভার কোটা eligibility + স্কোর ডিসপ্লে
- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminSolverQuotaView.kt`
- **কী করা হয়েছে:** পুরো স্ক্রিনটাই সলভার-কোটা সংক্রান্ত (unambiguous solver context) — তাই
  `isEligible = solver.reputationScore >= quotaThreshold` আর এর পাশের স্কোর-ডিসপ্লে টেক্সট,
  দুটোই এখন `solver.reputationScoreSolver` ব্যবহার করে। এতে dual-role ইউজারের User-role
  স্কোর দিয়ে ভুল eligibility/quota গণনার সম্ভাবনা দূর হলো।

### যাচাই করা হয়েছে
- সব বদলানো ফাইলে curly-brace balance পুরোপুরি মিলছে (প্রতিটা ফাইলে নতুন `{`/`}` সংখ্যা সমান)।
- `SomadhanRepository.kt`-এর paren imbalance (৯টা, Bengali কমেন্টের বন্ধনী থেকে) আগে থেকেই
  ছিল অরিজিনাল আপলোড করা zip-এই — এই ধাপে নতুন কোনো imbalance যোগ হয়নি।
- File count: ২৬৬টা ফাইল, আগের zip-এর সাথে ঠিক মিলছে (কোনো ফাইল হারায়নি/অপ্রত্যাশিতভাবে
  যোগ হয়নি — এই progress log আপডেট বাদে)।
- সব caller-site (positional ও named) হাতে ধরে চেক করা হয়েছে — নতুন `role` প্যারামিটার
  কোথাও পুরনো কলকে ভাঙেনি।
- Gradle compile করা হয়নি (এই sandbox-এ নেটওয়ার্ক বন্ধ, Android SDK/Gradle distribution
  ডাউনলোড করা যায় না — আগের সব ধাপের মতোই শুধু syntax-level ম্যানুয়াল রিভিউ)।

### কী টেস্ট করা উচিত (ব্যবহারকারীর পরবর্তী পদক্ষেপ)
1. প্রজেক্ট কম্পাইল করা (Android Studio/`./gradlew assembleDebug`) — এই environment-এ করা
   যায়নি।
2. একজন dual-role টেস্ট ইউজার তৈরি করে **Solver মোডে সক্রিয়** রেখে, AdminUsersView থেকে তার
   **User-কার্ডে** ক্লিক করে রেপুটেশন যোগ/কর্তন করা — ডায়ালগে "বর্তমান রেপুটেশন স্কোর (গ্রাহক)"
   দেখাচ্ছে কিনা, এবং adjustment-এর পর শুধু `reputationScoreUser` বদলেছে কিনা
   (`reputationScoreSolver` অপরিবর্তিত)।
3. একই টেস্ট AdminUserLookupView-তে — perspective ট্যাব (User/Solver) বদলে বদলে দেখা সামারি
   কার্ডের "রেপুটেশন স্কোর" এখন ঠিকভাবে বদলাচ্ছে কিনা, আর "রেপুটেশন" বাটনে ক্লিক করে দেখা
   সঠিক role-এর স্কোর দেখাচ্ছে ও adjust করছে কিনা।
4. AdminStatsView থেকে CSV এক্সপোর্ট করে "Reputation (User)"/"Reputation (Solver)" দুইটা
   কলামই সঠিক মান দেখাচ্ছে কিনা, আর "টপ সলভার" কার্ডের র‍্যাংকিং dual-role ইউজারের জন্য
   `reputationScoreSolver` অনুযায়ী হচ্ছে কিনা (User-role স্কোর দিয়ে না)।
5. AdminProblemsView-এ কোনো সমস্যায় bid দেওয়া সলভারদের তালিকায় স্কোর সঠিক
   (`reputationScoreSolver`) দেখাচ্ছে কিনা।
6. AdminSolverQuotaView-এ একজন dual-role সলভারের eligibility/quota থ্রেশহোল্ড
   `reputationScoreSolver`-এর ভিত্তিতে সঠিকভাবে হিসাব হচ্ছে কিনা।
7. Admin Audit Log-এ রেপুটেশন সমন্বয়ের পর `role` কলাম এখন card/perspective অনুযায়ী সঠিক
   role পাচ্ছে কিনা (আগে অ্যাকাউন্টের সক্রিয় role থেকে ভুল বসতে পারত)।

Confirmation-এর অপেক্ষায় — ঠিক থাকলে zip নামিয়ে পরবর্তী ধাপে যাওয়ার অনুমতি দিন। (উল্লেখ্য:
AdminUserLookupView-এর "ব্যালেন্স" সামারি Text এখনো perspective-scoped না, শুধু
active-role-scoped — সেটা এই ধাপের স্কোপের বাইরে, উপরে নোট করা হলো।)

---

## ধাপ ৭গ — AdminUserLookupView perspective-tab ব্যালেন্স সামারি ফিক্স (আলাদা সেশন)

আগের ধাপের "ইচ্ছাকৃতভাবে বাদ" নোট অনুযায়ী, AdminUserLookupView-এর perspective-tab সামারি
কার্ডে "ব্যালেন্স" Text তখনও `user.activeRoleBalance` (অ্যাকাউন্টের সক্রিয় role অনুযায়ী)
পড়ত, ঠিক পাশের "রেপুটেশন স্কোর" Text (`currentPerspectiveRole` অনুযায়ী) থেকে আলাদা আচরণ
করত। dual-role ইউজারের ক্ষেত্রে (যখন `user.role ≠ currentPerspectiveRole`) admin
Solver ট্যাবে থেকেও User-role-এর balance দেখত — reputation score আর balance-adjust
ডায়ালগের সংখ্যার সাথে মিলত না।

- **বদলানো ফাইল:** `app/src/main/java/com/example/ui/screens/AdminUserLookupView.kt`
- **কী করা হয়েছে:** perspective-tab সামারি কার্ডের "ব্যালেন্স" Text এখন reputation score-এর
  ঠিক একই প্যাটার্নে `currentPerspectiveRole` অনুযায়ী সরাসরি `user.balanceUser`/
  `user.balanceSolver` পড়ে (নতুন `perspectiveBalance` ভ্যাল), `activeRoleBalance` extension
  আর ব্যবহার হয় না এই ফাইলে — তাই অব্যবহৃত `import com.example.data.entity.activeRoleBalance`
  সরানো হয়েছে।
- **ইচ্ছাকৃতভাবে বাদ:** এটা শুধু read-only summary display ফিক্স — balance-adjust ডায়ালগ
  আগে থেকেই আলাদাভাবে `currentPerspectiveRole` ক্যাপচার করে সঠিক role adjust করত, তাই কোনো
  action-level সমস্যা ছিল না, শুধু এই একটা display Text mismatch করত।
- **যাচাই:** curly-brace balance ফাইলে মিলছে, ফাইল-কাউন্ট আগের zip-এর সাথে মিলছে। Gradle
  compile করা হয়নি (network-বিহীন sandbox)।

**পরবর্তী টেস্ট:** dual-role টেস্ট ইউজারকে User-role সক্রিয় রেখে AdminUserLookupView-এ
সার্চ করে Solver perspective ট্যাবে ক্লিক করা — সামারি কার্ডের ব্যালেন্স এখন Solver-role-এর
balance দেখাচ্ছে কিনা (আগে User-role-এর balance দেখাত), আর সেটা balance-adjust ডায়ালগের
সংখ্যার সাথে মিলছে কিনা।

---

## ধাপ ৮ — Regression Test Checklist (শুধু verify, কোনো নতুন কোড না)

Master prompt অনুযায়ী এই ধাপে কোনো .kt/.sql ফাইল বদলানো হয়নি — শুধু একটা manual regression
test checklist তৈরি করা হয়েছে।

- **নতুন ফাইল:** `ROLE_SEPARATION_TEST_CHECKLIST.md`
- **কী আছে এতে:** ধাপ ০–৭-এর সব ফিক্স (reputation DAO role-scoping, TransactionEntity
  role-scoping, Notification/Admin-Audit-Log role-scoping, Admin display স্ক্রিনের
  perspective-scoped balance/reputation) একসাথে regression-verify করার জন্য ৮টা সেকশন:
  ১) role-switch ছাড়াই wallet cross-role isolation, ২) reputation event শুধু সংশ্লিষ্ট
  role-এর স্কোর বদলায় কিনা, ৩) admin `reconcileUserBalances` role-scoped correction,
  ৪) role-switch রিগ্রেশন (wallet/reputation/ban-restrict), ৫) Transaction History
  role-scoped ফিল্টার, ৬) Notification inbox role-scoped leak বন্ধ, ৭) Admin Audit Log
  role traceability, ৮) Admin display স্ক্রিনের perspective-scoped সংখ্যা (ধাপ ৭গ-সহ)।
- মূল master prompt-এর ভাষায় দেওয়া টেস্ট-কেসগুলোর সাথে মিলিয়ে লেখা, dual-role টেস্ট
  অ্যাকাউন্ট আর admin panel অ্যাক্সেস প্রয়োজন (prerequisite হিসেবে উল্লেখ করা আছে)।

**পরবর্তী পদক্ষেপ (ব্যবহারকারীর):** চেকলিস্ট ধরে বাস্তবে টেস্ট করা, প্রতিটা কেসের ফলাফল
([x]/[FAIL]) নোট করে জানানো। কোনো কেস fail করলে সেটা নিয়ে আলাদা ছোট ফিক্স-রাউন্ড শুরু হবে।
Confirmation/ফলাফলের অপেক্ষায় থামা হলো — নিজে থেকে এগোনো হয়নি।
