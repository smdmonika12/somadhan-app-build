# Loading/Sync Fix Roadmap v2 — ধাপ ৪ — সম্পূর্ণ (v4, চূড়ান্ত)

এই নোটটা `CONTINUE-FROM-HERE.md` (v2, v3)-এর চূড়ান্ত আপডেট। **ধাপ ৪ (৩৩টা স্ক্রিন রিফ্যাক্টর +
dead-code cleanup) এখন সম্পূর্ণ।**

## ✅ সম্পূর্ণ — সবকিছু

### অংশ ১ — নেস্টিং বাগ ফিক্স: ১০০%
১৯টা জায়গায় (১৭টা স্ক্রিন + AdminPanelScreen.kt-এর Withdrawals/Escrow/Transactions ৩টা ট্যাব)
পুরনো নেস্টেড `SessionAwareLoadingContent` মুছে `SyncAwareContent` একমাত্র মেকানিজম করা হয়েছে।

### অংশ ২ — বাকি ১২ জায়গার কনভার্শন: ১০০%
- **AdminPanelScreen.kt-এর বাকি ১০টা ট্যাব** (Stats `0`, Users `4`, Problems `5`,
  ChatMonitoring `15`, DirectContracts `16`, UserLookup `18`, SolverQuota `19`,
  InstantJobs `21`, DisputeCenter `22`, GatewayPayments `23`) — সবগুলো `SyncAwareContent`
  (`syncPhase = initialSyncPhase`, `onRetry = { viewModel.retryInitialSync() }`) দিয়ে
  কনভার্ট করা হয়েছে, প্রতিটার নিজস্ব `_sync` sessionKey দিয়ে। পুরনো import (`SessionAwareLoadingContent`,
  `rememberAdminTabReady`) ফাইল থেকে সরানো হয়েছে।
- **SolverReviewsScreen.kt** ও **UserReviewsScreen.kt** — দুটোই সম্পূর্ণ, একই প্যাটার্নে (এই দুটো
  স্ক্রিন `users` (currentUser, initialSyncPhase-এর অংশ) আর `ratings` — দুটো sync-phase-এর উপর
  নির্ভরশীল বলে `worstSyncPhase(initialSyncPhase, ratingsSyncPhase)` ব্যবহার করা হয়েছে,
  `onRetry = { viewModel.retryInitialSync() }`)।

### নতুন হেল্পার: `worstSyncPhase()`
`MotionToolkit.kt`-এ একটা ছোট নতুন ফাংশন যোগ করা হয়েছে — একাধিক `SyncPhase`-এর মধ্যে সবচেয়ে
"খারাপ" অবস্থাটা বের করে (কোনোটা ERROR → ERROR, নাহলে কোনোটা LOADING → LOADING, নাহলে LOADED)।
Reviews স্ক্রিন দুটোতে ব্যবহৃত হয়েছে।

**⚠️ পর্যবেক্ষণ (পরের সেশনের জন্য নোট, এই সেশনের স্কোপের বাইরে):** `SomadhanRepository.kt`-এ
`_ratingsSyncPhase` কখনো `LOADING`/`ERROR`-এ mutate হয় না — সবসময় `LOADED` থাকে (স্টাব,
`categoriesSyncPhase`/`faqsSyncPhase`-এর মতো সত্যিকারের ওয়্যারিং নেই)। তাই `worstSyncPhase(...)`
আপাতত কার্যকরভাবে শুধু `initialSyncPhase`-এর মতোই আচরণ করে — এটা নিরাপদ (রিগ্রেশন হয়নি, আগে
`currentUser`-ভিত্তিক readiness-ও কার্যত `users`-টেবিল/initialSyncPhase-এরই প্রক্সি ছিল) কিন্তু
`ratingsSyncPhase`-এর প্রকৃত ওয়্যারিং এখনো বাকি একটা ফাঁক।

### Dead-code cleanup: সম্পূর্ণ
পুরো প্রজেক্টে গ্রেপ করে নিশ্চিত হওয়া গেছে `rememberAdminTabReady`/`rememberPageDataReady`/
`SessionAwareLoadingContent`-এর আর **কোনো real call-site নেই** (শুধু `MessagesScreen.kt`-এ একটা
পুরনো মন্তব্যে নাম আছে, আসল কল না)। তাই `MotionToolkit.kt` থেকে তিনটা ফাংশনই (এবং তাদের KDoc)
মুছে ফেলা হয়েছে, আর `SyncAwareContent`-এর নিজের KDoc-এ থাকা dangling রেফারেন্সও (এই তিনটা নাম
উল্লেখ করে) পরিষ্কার করা হয়েছে। `rememberSessionAwareSkeletonGate` (আলাদা, এখনো ব্যবহৃত হেল্পার —
`JobTrackingScreen`/`ReputationDetailScreen`-জাতীয় early-return প্যাটার্নের জন্য) **অক্ষত রাখা
হয়েছে**, ওটা এই cleanup-এর আওতায় ছিল না।

## যাচাই করা হয়েছে

- সবগুলো edit করা ফাইলে (`AdminPanelScreen.kt`, `SolverReviewsScreen.kt`, `UserReviewsScreen.kt`,
  `MotionToolkit.kt`) `{`/`}` কাউন্ট আলাদাভাবে মিলিয়ে দেখা হয়েছে — সবগুলো মিলেছে।
- পুরো প্রজেক্টে গ্রেপ করে নিশ্চিত হওয়া হয়েছে `rememberAdminTabReady(`/`rememberPageDataReady(`/
  `SessionAwareLoadingContent(` — কোনো real call-site অবশিষ্ট নেই।
- ফাইল-লিস্ট diff করে নিশ্চিত করা হয়েছে — dotfile সহ কোনো ফাইল বাদ পড়েনি (শুধু এই
  `CONTINUE-FROM-HERE.md` নিজেই নতুন/আপডেটেড, বাকি সব অপরিবর্তিত)।
- **Build/run করে টেস্ট করা হয়নি** (এই পরিবেশে Gradle/Android SDK/network নেই) — এটাই এখন সবচেয়ে
  জরুরি পরের ধাপ।

## 🔧 পরের সেশনের জন্য — যা এখনো বাকি

1. **Android Studio-তে আসল build/compile/run টেস্ট** — এই পুরো ধাপ ৪-এর কাজ কখনো কম্পাইল করে
   দেখা হয়নি। বিশেষভাবে মনোযোগ দিতে হবে:
   - AdminPanelScreen-এর নতুন কনভার্ট করা ১৩টা ট্যাবই (Withdrawals/Escrow/Transactions +
     Stats/Users/Problems/ChatMonitoring/DirectContracts/UserLookup/SolverQuota/InstantJobs/
     DisputeCenter/GatewayPayments) — brace-balance ম্যানুয়ালি গোনা হলেও Kotlin সিনট্যাক্স/টাইপ
     এরর থাকতে পারে যা শুধু আসল কম্পাইলার ধরবে।
   - `worstSyncPhase()` হেল্পার আর reviews স্ক্রিন দুটো — নতুন কোড, আগে কখনো compile-verify হয়নি।
   - খালি-ডাটা অবস্থা (fresh project, কোনো ডাটা নেই) আর slow-network/error-state সিমুলেশনে সব
     ৩৩টা স্ক্রিন টেস্ট করে দেখা, বিশেষত টাকা-সংক্রান্ত স্ক্রিনগুলো (Wallet, Withdrawal, Escrow,
     Transactions)।
2. **`ratingsSyncPhase` প্রকৃতপক্ষে wire করা** (উপরে উল্লেখিত ফাঁক) — যদি ভবিষ্যতে `ratings`-এর
   জন্য সত্যিকারের LOADING/ERROR সিগন্যাল দরকার হয় (এই মুহূর্তে reviews স্ক্রিন দুটো কার্যকরভাবে
   শুধু `initialSyncPhase`-এর উপর নির্ভর করছে, এটা কাজ করছে বলে জরুরি না, কিন্তু সম্পূর্ণতার জন্য
   নোট রাখা হলো)।
3. ~~ধাপ ৪ সম্পূর্ণ হওয়ায় roadmap-এর ধাপ ৫...~~ **[আপডেট, পঞ্চম সেশন]** ধাপ ৫ ও ধাপ ৬ এখন
   সম্পূর্ণ — বিস্তারিত `LOADING_SYNC_FIX_ROADMAP_PROGRESS.md` ফাইলে দেখুন (এই ফাইলটা শুধু ধাপ
   ৪-এর ঐতিহাসিক রেকর্ড হিসেবে রাখা হলো, নতুন আপডেট এখন থেকে ওই ফাইলেই থাকবে)। বাকি আছে শুধু
   ঐচ্ছিক ধাপ ৭, আর ব্যবহারকারীর Android Studio build/test।
