# Loading/Sync Fix Roadmap v2 — প্রোগ্রেস ফাইল

এই ফাইলটা `SupabaseRealtimeManager.kt`/`SomadhanRepository.kt`/`SomadhanViewModel.kt`/
`MotionToolkit.kt`/`FaqScreen.kt`-এর ভেতরে ছড়িয়ে থাকা "Loading/Sync Fix Roadmap v2" কোড-কমেন্ট
সিরিজের জন্য — আগে এই কাজের কোনো আলাদা progress-ফাইল ছিল না (শুধু কোড-কমেন্টেই ধাপ ১-৩ ট্র্যাক করা
হচ্ছিল)। এই সেশনে ব্যবহারকারী "ধাপ ১,২,৩ সম্পন্ন হয়েছে, রিচেক করে ধাপ ৪ শুরু করো" বলায়, প্রথমে
ধাপ ১-৩ পুরোপুরি কোড পড়ে ভেরিফাই করা হলো, তারপর ধাপ ৪ শুরু হলো — ভবিষ্যতের সেশনের জন্য এখন থেকে এই
আলাদা progress-ফাইলে ট্র্যাক করা হবে (`REALTIME_SCOPING_PROGRESS.md`-এর প্যাটার্ন অনুসরণ করে, সম্পূর্ণ
আলাদা/স্বতন্ত্র কাজ হিসেবে)।

---

## ✅ ধাপ ১-৩ রিচেক — তিনটাই সঠিকভাবে বাস্তবায়িত পাওয়া গেছে (কোনো ফিক্স লাগেনি)

কোড সরাসরি পড়ে (build/run সম্ভব না এই sandbox-এ, নিয়ম যথারীতি) নিচেরগুলো ভেরিফাই করা হলো:

- **ধাপ ১** (`SupabaseRealtimeManager.kt`): `SyncPhase { LOADING, LOADED, ERROR }` enum, `_initialSyncPhase`
  StateFlow। `attachDatabase()`-এ pull শুরুর ঠিক আগে `LOADING`, `withTimeout(20_000L)` দিয়ে
  network-hang গার্ড, সফল/ব্যর্থ/timeout — তিন ক্ষেত্রেই সঠিকভাবে `LOADED`/`ERROR` সেট হচ্ছে। ✅
- **ধাপ ২** (`SomadhanRepository.kt`): bulk-pull-এর বাইরের ডাটার জন্য `categoriesSyncPhase`/
  `faqsSyncPhase` (আসল local-seed অপারেশনের চারপাশে LOADING/LOADED/ERROR) আর
  `platformSettingsSyncPhase`/`auditLogsSyncPhase`/`ratingsSyncPhase` (কোনো async অপারেশন নেই
  বলে init-এই সরাসরি LOADED, ডকুমেন্টেড কারণসহ)। সব কটাই `SomadhanViewModel.kt`-এ সঠিকভাবে forward
  করা আছে। ✅
- **ধাপ ৩** (`MotionToolkit.kt` + `FaqScreen.kt`): `SyncAwareContent` composable (LOADING→skeleton,
  LOADED→content, ERROR→`SyncErrorState` + রিট্রাই বাটন) + `SomadhanViewModel.retryFaqsSync()` +
  `FaqScreen.kt`-এ পরীক্ষামূলক ওয়্যারিং — কাঠামো সঠিক, `faqsSyncPhase` আর `retryFaqsSync()`
  সঠিকভাবে যুক্ত করা আছে। ✅

**কোনো ফিক্স লাগেনি — ধাপ ৪ সরাসরি শুরু করা হলো।**

---

## 🔍 ধাপ ৪ শুরুর আগে পাওয়া একটা নতুন ডিজাইন-প্রশ্ন — সমাধান করে এগোনো হলো

ধাপ ৩-এর পাইলট স্ক্রিন (`FaqScreen.kt`)-এ **আগে থেকে কোনো loading-skeleton mechanism ছিল না**,
তাই `SyncAwareContent` বসানো সহজ ছিল (একমাত্র loading-layer)। কিন্তু বাকি স্ক্রিনগুলোর বেশিরভাগেই
(দেখুন নিচের তালিকা) **আগে থেকেই** একটা ভিন্ন, বেশি পুরোনো loading-mechanism আছে —
`SessionAwareLoadingContent`/`rememberPageDataReady` (local Room data "ready" কিনা তার উপর
ভিত্তি করে skeleton দেখায়, network error/retry-এর কোনো ধারণাই নেই)। রোডম্যাপ-কমেন্টে এই দুটোর
সম্পর্ক কীভাবে হবে তা লেখা ছিল না — তাই এই সেশনেই সিদ্ধান্ত নেওয়া হলো:

**সিদ্ধান্ত:** `SyncAwareContent` বাইরে বসবে (network-level sync error/retry — সম্পূর্ণ নতুন
উদ্বেগ), আর বিদ্যমান `SessionAwareLoadingContent` ভেতরে অক্ষত থাকবে (local Room data-ready
skeleton, আগে যেমন ছিল)। দুটো ভিন্ন স্তরের উদ্বেগ — একটা আরেকটাকে প্রতিস্থাপন করে না। প্রতিটাতে
**আলাদা `sessionKey`** ব্যবহার করা বাধ্যতামূলক (যেমন `"messages_sync"` বনাম বিদ্যমান
`"messages"`) — কারণ `SomadhanViewModel.sessionLoadedScreens`/`markLoadedOnce()` সব
sessionKey-ভিত্তিক কল-সাইট জুড়ে **একই shared `Set<String>`** — একই key দুই জায়গায় ব্যবহার
করলে একটা loaded হওয়া মাত্র অন্যটাও ভুলভাবে "already loaded" ধরে নিত (cross-contamination)।

⚠️ **এই ডিজাইন-সিদ্ধান্তটা ব্যবহারকারীর কাছ থেকে স্পষ্ট অনুমোদন নেওয়া হয়নি** (মূল প্রম্পটের
আগের কাজগুলোতে যেমন RLS-broadening-জাতীয় বড় সিদ্ধান্তে করা হয়েছিল) — যুক্তিসঙ্গত ও সবচেয়ে
কম-ঝুঁকিপূর্ণ পথ মনে হওয়ায় এগিয়ে যাওয়া হলো, কিন্তু ব্যবহারকারী চাইলে ভিন্ন approach (যেমন
দুটো mechanism merge করে একটাই বানানো) নিয়ে আলোচনা করতে পারেন।

---

## ✅ ধাপ ৪ — এই সেশনে যা সম্পন্ন হয়েছে

### নতুন shared infrastructure

- **`SupabaseRealtimeManager.kt`**: `retryInitialSync()` — `attachDatabase()`-এর ভেতরের
  LOADING→`pullBulkDataFromSupabase()`(20s timeout)→LOADED/ERROR চক্রটাই পুনরায় চালায়, কিন্তু
  `attachDatabase()`-এর `localDb === database` idempotency guard ছাড়া (retry বাটনে চাপলে
  সবসময় নতুন করে pull হওয়া উচিত)।
- **`SomadhanViewModel.kt`**: `retryInitialSync()` — উপরেরটা সরাসরি ফরওয়ার্ড করে (plain object
  কল, `viewModelScope.launch` লাগে না)। এটাই `initialSyncPhase`-নির্ভর সব স্ক্রিনের
  `SyncAwareContent.onRetry`-এর জন্য common entry point।

### স্ক্রিন-ওয়ারিং সম্পন্ন (২টা, উভয়ই `initialSyncPhase` ব্যবহার করে, কারণ messages/withdrawals
দুটোই bulk-pull-এর অংশ)

1. **`MessagesScreen.kt`** — `SyncAwareContent` (sessionKey `"messages_sync"`) বাইরে,
   বিদ্যমান `SessionAwareLoadingContent` (sessionKey `"messages"`, অপরিবর্তিত) ভেতরে।
2. **`WithdrawalHistoryScreen.kt`** — একই প্যাটার্ন (sessionKey `"withdrawal_history_sync"` /
   `"withdrawal_history"`)।

দুটো ফাইলেই ম্যানুয়াল bracket/paren-balance চেক করা হয়েছে (মিলেছে): `MessagesScreen.kt`
৫৩/৫৩ brace, ১৪৪/১৪৪ paren; `WithdrawalHistoryScreen.kt` ৪৯/৪৯ brace, ১৩০/১৩০ paren।

**⚠️ যা ভেরিফাই করা হয়নি (আগের সব ধাপের মতোই এই sandbox-এ network/Gradle নেই):** আসল Kotlin
build/compile, আর real device-এ network বন্ধ করে দেখা যে ERROR স্টেট + রিট্রাই বাটন আসলেই কাজ
করে কিনা।

---

## 🔜 বাকি স্ক্রিনগুলো — পরের সেশনের জন্য তালিকা ও প্রস্তাবিত syncPhase

মূল রোডম্যাপ-কমেন্টে "বাকি ৩২টা স্ক্রিন" বলা হয়েছিল একটা আনুমানিক সংখ্যা হিসেবে, কোনো নির্দিষ্ট
তালিকা কোথাও লেখা ছিল না। এই সেশনে পুরো `ui/screens/` ফোল্ডার স্ক্যান করে নিচের categorization
করা হলো (পরের সেশন এটাকেই চেকলিস্ট হিসেবে ব্যবহার করতে পারে):

### গ্রুপ A — আগে থেকেই `SessionAwareLoadingContent` আছে (Messages/WithdrawalHistory-এর মতোই
প্যাটার্ন প্রযোজ্য, `initialSyncPhase` + `retryInitialSync()`, শুধু আলাদা `_sync` sessionKey suffix
দিয়ে) — **১৭টা বাকি:**

- AllOpenProblemsScreen.kt, BidManagementScreen.kt, DashboardScreen.kt,
  FavoriteSolversScreen.kt, HomeScreen.kt, InstantJobHistoryScreen.kt, InstantJobsScreen.kt,
  ProfileScreen.kt, SolverAllPostsScreen.kt, SolverCompletedJobsScreen.kt,
  SolverMyBidsScreen.kt, SolverProblemsScreen.kt, TransactionHistoryScreen.kt,
  UserProblemsScreen.kt, UserWalletScreen.kt — সবগুলোই problems/bids/transactions/escrows/
  gateway_payments/users ডাটা দেখায়, তাই `initialSyncPhase` সঠিক।
- **SolverReviewsScreen.kt, UserReviewsScreen.kt** — ব্যতিক্রম: এরা `ratings` ডাটা দেখায়,
  তাই `ratingsSyncPhase` ব্যবহার করা উচিত (`initialSyncPhase` না) — কিন্তু `ratingsSyncPhase`
  ডিজাইন অনুযায়ী কখনো LOADING/ERROR-এ যায়ই না (সবসময় LOADED, ধাপ ২-এর নোট দেখুন), তাই এই দুটো
  স্ক্রিনে `SyncAwareContent` বসালেও বাস্তবে কোনো visible পরিবর্তন হবে না — **অগ্রাধিকার সবচেয়ে
  কম**, চাইলে skip করা যায় বা শুধু consistency-র জন্য বসানো যায় (ব্যবহারকারীর সিদ্ধান্ত)।

### গ্রুপ B — আগে থেকে কোনো loading-mechanism নেই (FaqScreen-এর মতোই সরল, সরাসরি `SyncAwareContent`
বসানো যাবে, nesting-জটিলতা নেই) — candidate, কিন্তু প্রতিটাতে গিয়ে **আগে যাচাই করতে হবে এরা
আদৌ কোন ডাটার উপর নির্ভর করে** (তার ভিত্তিতে কোন syncPhase — initialSyncPhase/categoriesSyncPhase/
অন্য কিছু):

- NotificationDetailScreen.kt, ReputationDetailScreen.kt, PublicProfileScreen.kt,
  PublicProfileReviewsScreen.kt, SolverKycScreen.kt, SolverBalanceWithdrawScreen.kt,
  SolverCategoryPostsScreen.kt, UserWithdrawScreen.kt, PostProblemScreen.kt (এখানে
  `categoriesSyncPhase` প্রাসঙ্গিক হতে পারে — ক্যাটাগরি-লিস্ট দেখায়), JobTrackingScreen.kt,
  DisputeResultScreen.kt, SupportCenterScreen.kt, UserInfoScreen.kt,
  ActiveJobsPopupScreen.kt, SolverSkillsScreen.kt।
- **ChatScreen.kt, ProblemDetailScreen.kt** — আলাদাভাবে সতর্কতা: এই দুটোতে ইতিমধ্যেই
  "Realtime Scoping ফিক্স" (সম্পূর্ণ ভিন্ন, স্বতন্ত্র কাজ — `REALTIME_SCOPING_PROGRESS.md`)-এর
  নিজস্ব join/leave broadcast-subscription wiring আছে (LaunchedEffect/DisposableEffect)।
  এই দুটোতে হাত দেওয়ার আগে বাড়তি সতর্কতা দরকার যাতে সেই বিদ্যমান wiring না ভাঙে।

### স্কোপের বাইরে (এই রোডম্যাপে টাচ করা হবে না)

- LoginScreen.kt, RegisterScreen.kt, SplashScreen.kt, MaintenanceScreen.kt,
  PlaceholderScreens.kt — cloud-data-নির্ভর কন্টেন্ট দেখায় না।
- সব `Admin*View.kt` (১৯টা) — এগুলো ভিন্ন loading/data-path (admin RPC aggregation) ব্যবহার
  করে, আর এই zip-এর অন্য কাজের (৭৯-এরর CSV-import ফিক্স) সরাসরি স্কোপ — এই রোডম্যাপ থেকে
  ইচ্ছাকৃতভাবে আলাদা রাখা হলো, ব্যবহারকারীর সিদ্ধান্ত ছাড়া মেশানো হবে না।

---

## 📦 প্রথম সেশনে ফাইল পরিবর্তন

- `app/src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt` — নতুন
  `retryInitialSync()` ফাংশন।
- `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt` — নতুন `retryInitialSync()`
  wrapper।
- `app/src/main/java/com/example/ui/screens/MessagesScreen.kt` — `SyncAwareContent` ওয়্যারিং।
- `app/src/main/java/com/example/ui/screens/WithdrawalHistoryScreen.kt` — `SyncAwareContent`
  ওয়্যারিং।
- এই নতুন প্রোগ্রেস ফাইল।

---

## ✅ দ্বিতীয় সেশন — যা সম্পন্ন হয়েছে (টাকা-সংক্রান্ত স্ক্রিন আগে, তারপর বাকি গ্রুপ A)

মূল ধাপ ৪ প্রম্পটের নিয়ম #৪ অনুযায়ী ("প্রথমে টাকা-সংক্রান্ত স্ক্রিন ঠিক করো") এই ক্রমে করা হলো:

### টাকা-সংক্রান্ত স্ক্রিন (৬টা, সব `initialSyncPhase` ব্যবহার করে) — সব শেষ
- `WithdrawalHistoryScreen.kt` — আগের সেশনেই করা হয়েছিল।
- `UserWalletScreen.kt` — sessionKey `"user_wallet_sync"`। `SomadhanPullToRefresh` →
  `SyncAwareContent` → বিদ্যমান `SessionAwareLoadingContent`("user_wallet") → `Column`।
- `TransactionHistoryScreen.kt` — sessionKey `"transaction_history_sync"`।
- `AdminPanelScreen.kt`-এর ৩টা ট্যাব (Withdrawals ট্যাব ইনডেক্স ২, Escrow ইনডেক্স ৭,
  Transactions ইনডেক্স ৯) — sessionKey যথাক্রমে `"admin_withdrawals_sync"`,
  `"admin_escrow_sync"`, `"admin_transactions_sync"`। একটাই শেয়ার্ড `initialSyncPhase`
  কালেকশন ফাংশনের টপে (অন্যান্য `rememberAdminTabReady` কলগুলোর কাছেই) যোগ করা হয়েছে,
  প্রতিটা ট্যাব সেটাই re-use করে।

### গ্রুপ A-এর বাকি ১৩টা স্ক্রিন — সব শেষ (একই মেকানিক্যাল প্যাটার্ন, প্রতিটাতে বিদ্যমান
sessionKey-এর সাথে `_sync` সাফিক্স যোগ করে নতুন sessionKey বানানো হয়েছে):

`AllOpenProblemsScreen.kt` (`all_open_problems_sync`), `BidManagementScreen.kt`
(`bid_management_sync`), `DashboardScreen.kt` (`dashboard_sync`), `FavoriteSolversScreen.kt`
(`favorite_solvers_sync`), `HomeScreen.kt` (`home_sync`), `InstantJobHistoryScreen.kt`
(`instant_job_history_sync`), `InstantJobsScreen.kt` (`instant_jobs_sync`), `ProfileScreen.kt`
(`profile_sync`), `SolverAllPostsScreen.kt` (`solver_all_posts_sync`),
`SolverCompletedJobsScreen.kt` (`solver_completed_jobs_sync`), `SolverMyBidsScreen.kt`
(`solver_my_bids_sync`), `SolverProblemsScreen.kt` (`solver_problems_sync`),
`UserProblemsScreen.kt` (`user_problems_sync`).

প্রতিটা ফাইলে একই প্যাটার্ন: import-এ `SyncAwareContent` যোগ, বিদ্যমান
`rememberPageDataReady(...)` কলের ঠিক পরে `val initialSyncPhase by
viewModel.initialSyncPhase.collectAsStateWithLifecycle()` যোগ, আর বিদ্যমান
`SessionAwareLoadingContent(...)` কলটাকে অপরিবর্তিত রেখে তার বাইরে
`SyncAwareContent(sessionKey = "<key>_sync", viewModel = viewModel, syncPhase =
initialSyncPhase, onRetry = { viewModel.retryInitialSync() })`-এ মুড়ে দেওয়া হয়েছে।
`isNotEmpty()`/empty-state যুক্তি স্পর্শ করা হয়নি — সেটা এখনো `SessionAwareLoadingContent`-এর
`content` ল্যাম্বডার ভেতরেই আগের মতো আছে।

**ভেরিফিকেশন:** প্রতিটা পরিবর্তিত ফাইলে ব্র্যাকেট/প্যারেন-ব্যালেন্স স্ক্রিপ্ট দিয়ে চেক করা
হয়েছে (`{`/`}` আর `(`/`)` কাউন্ট মিলেছে, diff = 0) — build/run সম্ভব হয়নি এই sandbox-এ, শুধু
স্ট্যাটিক ব্র্যাকেট-চেক।

**যা এখনো এই সেশনে করা হয়নি (⚠️ বাকি):**
- `SolverReviewsScreen.kt`, `UserReviewsScreen.kt` — Group A-এর শেষ ২টা, ইচ্ছাকৃতভাবে স্কিপ
  করা হয়েছে (ratingsSyncPhase কখনো ERROR হয় না বলে কম-মূল্যবান, আগের সেশনেই এই preference
  নোট করা ছিল, ব্যবহারকারীর কনফার্মেশন এখনো নেওয়া হয়নি)।
- Group B-এর ১৫টা স্ক্রিন (নিচের তালিকা দেখুন — অপরিবর্তিত)।
- `ChatScreen.kt`, `ProblemDetailScreen.kt` — realtime-scoping wiring-এর কারণে অতিরিক্ত
  সতর্কতা দরকার, স্পর্শ করা হয়নি।
- ১৯টা `Admin*View.kt`-এর CSV-import বিল্ড-এরর ফিক্স — `NEXT_SESSION_PROMPT.txt` অনুযায়ী
  আগেই ৭৯/৭৯ ফিক্স হয়ে গেছে বলে লেখা আছে, কিন্তু এই সেশনে সেটা রি-ভেরিফাই করা হয়নি।
- `rememberAdminTabReady`/`rememberPageDataReady` ডেড-কোড ক্লিনআপ — এখনো করা হয়নি (ঠিকই,
  কারণ Group A-এর ২টা আর সব Group B এখনো migrate হয়নি, তাই এই হেল্পার দুটো এখনো ব্যবহৃত
  হচ্ছে)।
- Android Studio Gradle build — এখনো কখনো করা হয়নি (sandbox-এ সম্ভব না)।

## ✅ চতুর্থ সেশন (এই সেশন) — Group B বাকি অংশ সম্পূর্ণ + ধাপ ৫ আংশিক শুরু

আপলোড করা zip-এর নাম ছিল `somadhan-loading-sync-step4-complete.zip` (ব্যবহারকারীর নামকরণ,
বাস্তবে ধাপ ৪ তখনো ১০০% শেষ ছিল না) — এই সেশনে প্রথমে পুরো কোড স্ক্যান করে যাচাই করা হলো ঠিক
কোন স্ক্রিনগুলো ইতিমধ্যে `SyncAwareContent`/`SyncBlockedRetryState` ব্যবহার করছে (তৃতীয় সেশনের
নোটে যা লেখা ছিল তার চেয়ে বাস্তবে কিছুটা বেশি এগিয়ে ছিল — `SolverReviewsScreen.kt`/
`UserReviewsScreen.kt` ইতিমধ্যেই `worstSyncPhase(initialSyncPhase, ratingsSyncPhase)` দিয়ে
সঠিকভাবে ওয়্যার করা পাওয়া গেছে, আর `MotionToolkit.kt`-এ `worstSyncPhase()` কম্বাইন-হেল্পার আর
dead-code cleanup — দুটোই ইতিমধ্যে সম্পন্ন পাওয়া গেছে)।

### ✅ Group B-এর বাকি ১০টা স্ক্রিন — সব শেষ, প্রতিটাতে bracket/paren-balance ভেরিফাইড

- **`ActiveJobsPopupScreen.kt`** — `ActiveJobsPopup` composable-এর "খালি/লিস্ট" if-else
  ব্লকটা `SyncAwareContent`-এ মোড়ানো হয়েছে (`initialSyncPhase`, retry = `retryInitialSync()`)।
  sessionKey `isSolver`-ভিত্তিক আলাদা (`solver_active_jobs_popup_sync` /
  `user_active_jobs_popup_sync`) — একই composable দুই রোলের জন্য reuse হওয়ায়।
- **`SolverSkillsScreen.kt`** — পুরো body `SyncAwareContent`-এ, sessionKey
  `solver_skills_sync`, `worstSyncPhase(initialSyncPhase, categoriesSyncPhase)` (currentUser +
  allCategories দুটোর উপরই নির্ভরশীল)।
- **`SolverBalanceWithdrawScreen.kt`** — পুরো body `SyncAwareContent`-এ, sessionKey
  `solver_balance_withdraw_sync`, `initialSyncPhase`। নিচের `WithdrawalDetailBottomSheet`
  Scaffold-এর বাইরে, তাই স্পর্শ করা হয়নি।
- **`UserWithdrawScreen.kt`** — একই প্যাটার্ন, sessionKey `user_withdraw_sync`।
- **`SolverKycScreen.kt`** — পুরো body `SyncAwareContent`-এ, sessionKey `solver_kyc_sync`,
  `initialSyncPhase` (currentUser-এর KYC ফিল্ড বাল্ক-পুলের অংশ)।
- **`UserInfoScreen.kt`** — body-র `Column` (প্রোফাইল ফর্ম) `SyncAwareContent`-এ, sessionKey
  `user_info_sync`, `initialSyncPhase`। **সতর্কতা:** নিচের ফোন-পরিবর্তন `AlertDialog`
  (`showChangePhoneFlow`) ইচ্ছাকৃতভাবে `SyncAwareContent`-এর বাইরে রাখা হয়েছে, যাতে সেই ফ্লো
  সবসময় অ্যাক্সেসযোগ্য থাকে sync state নির্বিশেষে।
- **`PostProblemScreen.kt`** — পুরো body `SyncAwareContent`-এ, sessionKey `post_problem_sync`,
  `worstSyncPhase(initialSyncPhase, categoriesSyncPhase)` (currentUser + activeCategories)।
- **`DisputeResultScreen.kt`** — `JobTrackingScreen`-এর মতোই early-return প্যাটার্ন
  (`if (problem == null) { ...; return }`) — এখানে `initialSyncPhase == ERROR` হলে
  `SyncBlockedRetryState` দেখানো হচ্ছে, নাহলে আগের মতোই `CircularProgressIndicator`।
- **`SupportCenterScreen.kt`** — পুরো ফাইল পড়ে নিশ্চিত করা হলো এটা সম্পূর্ণ static ("শীঘ্রই
  আসছে" প্লেসহোল্ডার) — কোনো `viewModel` কল নেই, কোনো ডাটা-নির্ভরতা নেই। **কোনো sync-wiring
  লাগেনি, ইচ্ছাকৃতভাবে অপরিবর্তিত রাখা হয়েছে।**
- **`SolverCategoryPostsScreen.kt`** — এটা আগে থেকেই `rememberSessionAwareSkeletonGate`
  early-return প্যাটার্ন ব্যবহার করত (pull-to-refresh + pagination সহ জটিল স্ক্রিন)। নতুন
  `combinedSyncPhase = worstSyncPhase(initialSyncPhase, categoriesSyncPhase)` যোগ করে,
  বিদ্যমান `isInitialLoading` স্কেলিটন-শর্তের ঠিক আগে একটা নতুন branch বসানো হয়েছে:
  `isInitialLoading && combinedSyncPhase == ERROR` হলে `SyncBlockedRetryState`, নাহলে আগের
  মতোই স্কেলিটন/category-not-found/লিস্ট।

### ✅ `SomadhanViewModel.kt`-এ নতুন ফাংশন
- **`retryCategoriesSync()`** — `retryFaqsSync()`-এর মতোই প্যাটার্ন
  (`viewModelScope.launch { repository.ensureCategoriesSeeded() }`), `categoriesSyncPhase`-নির্ভর
  স্ক্রিনগুলোর (`SolverSkillsScreen`, `PostProblemScreen`, `SolverCategoryPostsScreen`)
  `onRetry`-তে ব্যবহৃত।

**এর ফলে roadmap-এর মূল "৩৩টা স্ক্রিন" (Group A ১৭ + Group B ১৫ + Reviews ২ — Admin*View,
ChatScreen, ProblemDetailScreen, আর auth/static স্ক্রিন বাদে) এখন সম্পূর্ণভাবে
`SyncAwareContent`/`SyncBlockedRetryState`-এ ওয়্যার করা। এটা পুরো কোডবেস স্ক্যান করে
সরাসরি ভেরিফাই করা হয়েছে (`grep -L`-জাতীয় চেক দিয়ে) — বাদ পড়া ফাইলগুলো সবই ইচ্ছাকৃতভাবে
স্কোপের বাইরে (Admin*View ১৯টা, ChatScreen, ProblemDetailScreen, LoginScreen, RegisterScreen,
SplashScreen, MaintenanceScreen, PlaceholderScreens, SupportCenterScreen)।**

**ধাপ ৪ এখন কার্যত সম্পূর্ণ** (Android Studio build/run-এ ভেরিফাই করা বাকি, নিচে দেখুন)।

---

### 🔶 ধাপ ৫ (page re-entry automatic retry) — আংশিক শুরু হয়েছে, সম্পূর্ণ হয়নি

`somadhan-loading-fix-roadmap-v2.md`-এর ধাপ ৫ প্রম্পট অনুযায়ী, `MotionToolkit.kt`-এর
`SyncAwareContent`-এর **ভেতরে** (সিগনেচার/কল-সাইট অপরিবর্তিত রেখে) নিচেরগুলো যোগ করা হয়েছে:

- **`SyncResumeRetryCooldown`** (নতুন private object) — প্রতিটা `sessionKey`-এর সর্বশেষ
  automatic-retry timestamp রাখে, ৫ সেকেন্ডের cooldown guard (`COOLDOWN_MS = 5_000L`)।
- `SyncAwareContent`-এর ভেতরে `rememberUpdatedState(syncPhase)` / `rememberUpdatedState(onRetry)`
  দিয়ে স্টেল ক্লোজার এড়ানো হয়েছে, আর `DisposableEffect(sessionKey, lifecycleOwner)` দিয়ে
  `LocalLifecycleOwner.current.lifecycle`-এ একটা `LifecycleEventObserver` বসানো হয়েছে:
  `Lifecycle.Event.ON_RESUME` এলে, যদি সেই মুহূর্তের `syncPhase == ERROR` হয় *এবং* cooldown
  অতিক্রান্ত হয়ে থাকে, তাহলে `onRetry()` স্বয়ংক্রিয়ভাবে কল হয়। প্রথমবার স্ক্রিনে ঢোকার সময়
  আলাদা কোনো গার্ড ছাড়াই এমনিতেই ঠিকভাবে কাজ করবে বলে ধারণা করা হচ্ছে, কারণ initial phase
  সাধারণত `LOADING` থাকে (`ERROR` না), তাই ON_RESUME-এ প্রথমবার কিছুই ট্রিগার হবে না — **এই
  ধারণাটা code review-এ যৌক্তিক মনে হয়েছে কিন্তু device/emulator-এ চালিয়ে যাচাই করা হয়নি।**

**⚠️ ধাপ ৫-এর যা এখনো বাকি/অসম্পূর্ণ (পরের সেশনের প্রথম কাজ এটাই হওয়া উচিত):**

1. **Duplicate/racing fetch guard (ধাপ ৫ প্রম্পটের নিয়ম #২) — এখনো বাস্তবায়ন করা হয়নি।**
   `SupabaseRealtimeManager.retryInitialSync()` (আর একইভাবে
   `SomadhanViewModel.retryFaqsSync()`/`retryCategoriesSync()`) সরাসরি `managerScope.launch`/
   `viewModelScope.launch` করে ফেলে — বর্তমান phase ইতিমধ্যে `LOADING` কিনা তা **চেক করে না**।
   অর্থাৎ যদি pull-to-refresh (ধাপ ৬, এখনো শুরু হয়নি) বা ম্যানুয়াল বাটন থেকে একটা fetch চলমান
   থাকা অবস্থায় ON_RESUME automatic-retry ট্রিগার হয়, তাহলে **দুটো সমান্তরাল
   `pullBulkDataFromSupabase()` কল চলতে পারে** — এটা এখনো একটা open race-condition gap।
   প্রম্পট নিজেই বলেছে এই guard হয় retry ফাংশনের ভেতরে (ViewModel/Manager-এর দিকে) অথবা
   `SyncAwareContent`-এর ভেতরে বসানো যেতে পারে — এই সেশনে সময়ের অভাবে কোনোটাই করা হয়নি।
   প্রস্তাবিত পন্থা: `retryInitialSync()`/`retryCategoriesSync()`/`retryFaqsSync()`-এর শুরুতে
   `if (_initialSyncPhase.value == SyncPhase.LOADING) return` (বা সংশ্লিষ্ট phase) জাতীয় early
   guard বসানো — কিন্তু এটা `ensureFaqsSeeded()`/`ensureCategoriesSeeded()`-এর ভেতরেই phase সেট
   হয় বলে ঠিক কোথায় guard বসালে race condition-মুক্ত হবে তা সাবধানে ভাবতে হবে (পরের সেশনে)।
2. **Device/emulator-এ কোনো ভেরিফিকেশন হয়নি** — শুধু `MotionToolkit.kt`-এর bracket/paren-balance
   চেক করা হয়েছে (মিলেছে)। ON_RESUME lifecycle observer আসলেই সব রকম navigation
   pattern-এ (bottom-nav saveState/restoreState সহ) সঠিকভাবে ফায়ার করে কিনা তা Android
   Studio-তেই যাচাই করতে হবে।
3. **Cooldown-এর মান (৫ সেকেন্ড)** প্রম্পটের suggestion অনুযায়ী বসানো হয়েছে, ব্যবহারকারীর
   সাথে কনফার্ম করা হয়নি।

### ❌ ধাপ ৬/৭ — শুরুই হয়নি
- ধাপ ৬ (pull-to-refresh-কে আসল retry বানানো + বাকি ৫ স্ক্রিনে/AdminPanel ট্যাবে
  pull-to-refresh যোগ করা) এবং ধাপ ৭ (ঐচ্ছিক, connectivity-ফিরলে automatic retry) — এখনো
  একদমই ছোঁয়া হয়নি। বিস্তারিত প্রম্পট `somadhan-loading-fix-roadmap-v2.md`-এ আছে।
- **লক্ষ্য করার বিষয়:** ধাপ ৬ শুরু করার আগে উপরের duplicate-fetch guard (ধাপ ৫-এর বাকি অংশ)
  আগে ঠিক করে নেওয়া ভালো — কারণ ধাপ ৬ pull-to-refresh থেকেও `retryInitialSync()`/per-domain
  retry কল করবে, এবং তখন ON_RESUME + pull-to-refresh + ম্যানুয়াল বাটন — তিনটা path একসাথে
  ওভারল্যাপ করার ঝুঁকি আরও বাড়বে যদি guard-টা তখনও না থাকে।

### অন্যান্য এখনো-বাকি আইটেম (আগের সেশনগুলো থেকে, এখনো অপরিবর্তিত)
- Android Studio Gradle build — পুরো রোডম্যাপের কোনো ধাপই এখনো একবারও real build/run দিয়ে
  ভেরিফাই করা হয়নি (এই sandbox-এ Gradle/network নেই)। **এটা এখন সবচেয়ে জরুরি —** ৪টা সেশন
  ধরে ~২৫টা ফাইলে পরিবর্তন জমেছে, শুধু bracket-balance static check দিয়ে; প্রকৃত টাইপ-চেক/
  import-resolution/Compose compiler এরর কিছুই ধরা পড়েনি।
- ৭৯-এরর CSV-import বিল্ড-এরর ফিক্স রি-ভেরিফিকেশন — এই সেশনেও করা হয়নি।
- Admin*View.kt (১৯টা) — roadmap-এর মূল স্কোপের বাইরে বলে ইচ্ছাকৃতভাবে অপরিবর্তিত (ধাপ ৬-এ
  শুধু pull-to-refresh যোগ হবে AdminPanelScreen-এর ট্যাবে, কিন্তু SyncAwareContent wiring না)।

## ✅ পঞ্চম সেশন (এই সেশন) — ধাপ ৫ সম্পূর্ণ + ধাপ ৬ সম্পূর্ণ

আপলোড করা zip-এর নাম ছিল `somadhan-loading-sync-step5-partial.zip` — অর্থাৎ ধাপ ৫ আংশিক অবস্থায়
ছিল। এই সেশনে প্রথমে ধাপ ৫-এর বাকি অংশ (duplicate/racing-fetch guard) শেষ করা হলো, তারপর সরাসরি
ধাপ ৬ (pull-to-refresh-কে আসল retry বানানো + বাকি ৫ স্ক্রিনে/AdminPanel-এর ১৩ ট্যাবে
pull-to-refresh যোগ) সম্পূর্ণ করা হলো।

### ✅ ধাপ ৫ — duplicate/racing-fetch guard (আগের সেশনের "সবচেয়ে জরুরি" আইটেম) — সম্পূর্ণ

**মূল ডিজাইন-সিদ্ধান্ত:** guard-টা `SyncPhase`-এর মান নিজেই ব্যবহার করে বসানো হয়নি (যেমন
"phase == LOADING হলে স্কিপ করো") — কারণ `_initialSyncPhase`/`_categoriesSyncPhase`/
`_faqsSyncPhase`-এর initial value নিজেই `LOADING` (fetch শুরু হওয়ার আগেই), তাই এমন guard বসালে
একদম প্রথম কলটাই ভুলভাবে স্কিপ হয়ে যেত। তার বদলে প্রতিটাতে একটা আলাদা, স্বতন্ত্র
`AtomicBoolean` "in-flight" ফ্ল্যাগ যোগ করা হয়েছে যেটা শুধু আসল fetch চলাকালীন সময়েই `true`
থাকে:

- **`SupabaseRealtimeManager.kt`** — `initialSyncInFlight` (AtomicBoolean)। `attachDatabase()`
  আর `retryInitialSync()`-এর ভেতরের ডুপ্লিকেট LOADING→pull(20s)→LOADED/ERROR কোডটা একটা নতুন
  শেয়ার্ড প্রাইভেট `performInitialSync()`-এ তুলে আনা হয়েছে (guard + `try`/`finally` সহ), দুটো
  পাবলিক ফাংশনই এখন শুধু `managerScope.launch { performInitialSync() }` কল করে। ফলে
  attachDatabase()/retryInitialSync()/pull-to-refresh (নিচে ধাপ ৬) — এই তিনটা path একসাথে
  চললেও কখনোই দুটো সমান্তরাল bulk-pull শুরু হবে না।
- **`SomadhanRepository.kt`** — `categoriesSyncInFlight`/`faqsSyncInFlight` (AtomicBoolean),
  একই প্যাটার্নে `ensureCategoriesSeeded()`/`ensureFaqsSeeded()`-এর ভেতরে guard + `finally`।

**ভেরিফিকেশন:** তিনটা ফাইলেই bracket/paren-balance স্ক্রিপ্ট দিয়ে চেক করা হয়েছে (diff = 0
SupabaseRealtimeManager.kt/SomadhanViewModel.kt-এ; SomadhanRepository.kt-এ paren diff -3 আছে
কিন্তু সেটা **মূল আপলোড করা zip-এই আগে থেকে ছিল** (৫১১৭/৫১২০, সরাসরি zip থেকে যাচাই করা) — এই
সেশনের পরিবর্তনের কারণে না, তাই চিন্তার কিছু নেই।

### ✅ ধাপ ৬ — pull-to-refresh-কে আসল retry বানানো + বাকি ৫ স্ক্রিনে/AdminPanel-এ যোগ — সম্পূর্ণ

**অংশ ১ — বিদ্যমান ১৬টা স্ক্রিনের retry-if-ERROR (কেন্দ্রীয়ভাবে):**
`SomadhanViewModel.kt`-এর `refreshData()` আর `refreshWalletData()` দুটোতেই শুরুতে একটা নতুন চেক
যোগ করা হয়েছে — `initialSyncPhase.value == SyncPhase.ERROR` হলে `retryInitialSync()` কল করে,
যেটা এখন ধাপ ৫-এর in-flight guard-এর কারণে নিরাপদ (অন্য কোথাও থেকে চলমান থাকলে no-op)। যাচাই
করে দেখা হয়েছে বিদ্যমান ১৬টা pull-to-refresh স্ক্রিনের প্রতিটাই (HomeScreen, DashboardScreen,
MessagesScreen, ProfileScreen, UserWalletScreen, InstantJobsScreen, BidManagementScreen,
UserProblemsScreen, SolverProblemsScreen, SolverAllPostsScreen, SolverCompletedJobsScreen,
SolverMyBidsScreen, SolverCategoryPostsScreen, FavoriteSolversScreen, UserReviewsScreen,
SolverReviewsScreen) সরাসরি বা পরোক্ষভাবে `refreshData()`/`refreshWalletData()` কল করে — তাই
এই একটা কেন্দ্রীয় পরিবর্তনেই সবগুলো স্ক্রিন স্বয়ংক্রিয়ভাবে আসল retry পায়, কোনো স্ক্রিন
আলাদাভাবে স্পর্শ করতে হয়নি।

**অংশ ২ — বাকি ৫ স্ক্রিনে/জায়গায় নতুন pull-to-refresh:**
- **`WithdrawalHistoryScreen.kt`**, **`TransactionHistoryScreen.kt`** — বিদ্যমান
  `SyncAwareContent`-কে `SomadhanPullToRefresh(isRefreshing = viewModel.isRefreshing,
  onRefresh = { viewModel.refreshData() })`-এ মুড়ানো হয়েছে (Scaffold-এর content lambda-র
  ভেতরে, `SyncAwareContent`-এর বাইরে)।
- **`AllOpenProblemsScreen.kt`** — `HorizontalPager`-কে (দুই ট্যাব: উন্মুক্ত/চলমান) মুড়ানো
  হয়েছে, `onRefresh = { viewModel.resetOpenProblemsPagination(); viewModel.refreshData() }`
  (বিদ্যমান pagination-reset প্যাটার্ন অনুসরণ করে, যেমন SolverAllPostsScreen-এ আছে)।
- **`InstantJobHistoryScreen.kt`** — একইভাবে `HorizontalPager`-কে (তিন ট্যাব) মুড়ানো হয়েছে,
  `onRefresh = { viewModel.refreshData() }` (এই স্ক্রিনের নিজস্ব pagination-reset ফাংশন নেই,
  সব ডাটা সরাসরি realtime flow থেকে আসে)।
- **`AdminPanelScreen.kt`-এর ১৩টা ট্যাব** — প্রতিটা ট্যাবে আলাদা wrapper না বসিয়ে, পুরো
  `when (selectedTabIndex) { ... }` ব্লকটাকেই একটা একক `SomadhanPullToRefresh`-এ মুড়ানো
  হয়েছে (সবচেয়ে কম-ঝুঁকিপূর্ণ পথ, মূল ধাপ ৬ প্রম্পটেই এই অপশনের কথা বলা ছিল) —
  `onRefresh` একটা নতুন `onAdminPullToRefresh` lambda যেটা `initialSyncPhase == ERROR` হলে
  `retryInitialSync()` কল করে। **বিদ্যমান "Force Sync" আইকন-বাটন
  (`viewModel.triggerCloudSync()`, টপ-বারে) অপরিবর্তিত রাখা হয়েছে** — সেটা সম্পূর্ণ ভিন্ন,
  ব্যাপক push+pull অপারেশন (local→cloud sync + bulk pull, কিন্তু `initialSyncPhase` আপডেট করে
  না), pull-to-refresh-এর সরল retry-if-ERROR-এর সাথে গুলিয়ে ফেলা হয়নি।

**ভেরিফিকেশন:** সবগুলো edited ফাইলে bracket/paren-balance ভেরিফাই করা হয়েছে (diff = 0, বা
পূর্ব-বিদ্যমান imbalance যেটা মূল zip-এর সাথে তুলনা করে নিশ্চিত করা হয়েছে)।

**⚠️ যা এখনো ভেরিফাই করা হয়নি (সব সেশনের মতোই এই sandbox-এ network/Gradle নেই):** আসল Kotlin
build/compile/run। বিশেষভাবে মনোযোগ দরকার:
- `AdminPanelScreen.kt`-এ `SomadhanPullToRefresh`-এর ভেতরে পুরো `when` ব্লক মোড়ানোর ফলে
  pull-gesture আসলেই সব ট্যাবে ঠিকভাবে কাজ করছে কিনা (বিশেষত ট্যাব ৩ — AdminManualNotificationView,
  ট্যাব ১ — AdminKycView, যেগুলো `SyncAwareContent`/`initialSyncPhase` ব্যবহার করে না, তাই
  pull-to-refresh স্পিনার দেখাবে কিন্তু ERROR-recovery প্রাসঙ্গিক না — এটা প্রত্যাশিত, কারণ ওই
  দুটো ট্যাব ভিন্ন ডাটা-সোর্সের উপর নির্ভরশীল, roadmap-এর মূল স্কোপে ছিল না)।
- `InstantJobHistoryScreen.kt`/`AllOpenProblemsScreen.kt`-এ `HorizontalPager` + `PullToRefreshBox`
  nesting — ট্যাব সোয়াইপ (হরাইজন্টাল) বনাম পুল-রিফ্রেশ (ভার্টিক্যাল) gesture-conflict হয় কিনা
  device-এ টেস্ট করে দেখতে হবে (দুটো ভিন্ন axis হওয়ায় সাধারণত conflict হওয়ার কথা না, কিন্তু
  verify করা হয়নি)।

## ❌ বাকি — ধাপ ৭ (ঐচ্ছিক/বোনাস)

ধাপ ৭ (connectivity ফিরলে automatic retry, `ConnectivityManager.NetworkCallback`-ভিত্তিক) এখনো
শুরু হয়নি — এটা roadmap-এই "ঐচ্ছিক" হিসেবে চিহ্নিত, বাকি কোনো ধাপ এটার উপর নির্ভর করে না।
পূর্ণ প্রম্পট `somadhan-loading-fix-roadmap-v2.md`-এ আছে। ব্যবহারকারী চাইলে এটা একটা নতুন সেশনে
করা যাবে।

## 🔜 পরের সেশন ঠিক এখান থেকে শুরু করবে

1. **সবচেয়ে জরুরি:** ব্যবহারকারীকে Android Studio-তে পুরো প্রজেক্ট build করে compile-এরর আছে
   কিনা জানাতে বলা — ধাপ ১-৬ সম্পূর্ণ হয়ে গেছে (৫টা সেশন ধরে ~৩০টা ফাইলে পরিবর্তন জমেছে), কিন্তু
   কখনো real build/compile-এ ভেরিফাই করা হয়নি, শুধু bracket-balance static check।
2. Build-এ কোনো এরর এলে সেগুলো ফিক্স করা।
3. তারপর ধাপ ৮-এর টেস্ট চেকলিস্ট (`somadhan-loading-fix-roadmap-v2.md`-এর শেষে) অনুযায়ী
   ব্যবহারকারী নিজে Android Studio/emulator-এ ম্যানুয়ালি সব verify করবেন — বিশেষত: AdminPanelScreen
   pull-to-refresh, HorizontalPager+pull-to-refresh gesture-conflict, আর টাকা-সংক্রান্ত পেজ।
4. যদি ব্যবহারকারী চান, ঐচ্ছিক ধাপ ৭ (connectivity-ফিরলে automatic retry) একটা নতুন সেশনে করা।

---

## ⚠️ তৃতীয় সেশন (এই সেশন) — অসম্পূর্ণ অবস্থায় থামতে হয়েছে, নিচে ঠিক কী অবস্থায় আছে

ব্যবহারকারী এই সেশনে "Group B ১৫টা + Reviews ২টা + ডেড-কোড ক্লিনআপ শেষ করো, তারপর ধাপ ৫" —
এই নির্দেশ দিয়েছিলেন। **এই কাজ সম্পূর্ণ শেষ হয়নি** — সময়/টুল-কল সীমার কারণে মাঝপথে থামতে
হয়েছে। নিচে ঠিক কোনটা হয়েছে আর কোনটা এখনো বাকি তার নির্ভুল তালিকা।

### একটা নতুন design সিদ্ধান্ত নেওয়া হয়েছে এই সেশনে
Group B-এর কিছু স্ক্রিন (`ReputationDetailScreen`, `JobTrackingScreen`) `SyncAwareContent`-এর
নেস্টেড content-lambda প্যাটার্নে যায়নি — এরা আগে থেকেই নিজস্ব
`rememberSessionAwareSkeletonGate` + early-return (`if (x == null) { skeleton; return }`)
প্যাটার্ন ব্যবহার করে, আর `JobTrackingScreen.kt` এতটাই বড় (৫৮০০+ লাইন) ও জটিল (payment/escrow/
dispute ফ্লো) যে পুরো ফাংশন-বডি একটা content-lambda-তে মোড়ানো অপ্রয়োজনীয় ঝুঁকি তৈরি করত।
তাই `MotionToolkit.kt`-এ একটা নতুন ছোট পাবলিক কম্পোনেন্ট `SyncBlockedRetryState(onRetry)` যোগ
করা হয়েছে (বিদ্যমান private `SyncErrorState`-এরই একটা পাবলিক wrapper) — এই স্ক্রিনগুলোর
বিদ্যমান early-return গেটের ভেতরেই, `data == null && syncPhase == ERROR` হলে এটা কল করে
স্কেলিটনের বদলে এরর+রিট্রাই UI দেখানো হয়, কল-সাইটের বাকি কাঠামো/সিগনেচার অপরিবর্তিত রেখে।

### ✅ এই সেশনে যা শেষ হয়েছে (মোট ৫টা ফাইল, সব bracket-balance ভেরিফাইড)

1. **`MotionToolkit.kt`** — নতুন পাবলিক `SyncBlockedRetryState(onRetry, modifier)` কম্পোনেন্ট
   (বিদ্যমান private `SyncErrorState`-কে ভেতর থেকে কল করে)।
2. **`NotificationDetailScreen.kt`** — sessionKey `notification_detail_sync`,
   `initialSyncPhase` (notifications বাল্ক-পুলের অংশ), পুরো `if (notification != null) {...}
   else {...}` ব্লক (empty-state সহ) `SyncAwareContent`-এ মোড়ানো হয়েছে।
3. **`ReputationDetailScreen.kt`** — early-return গেটে (`effectiveUser == null ||
   minimumSkeletonActive`) `SyncBlockedRetryState` যোগ, `initialSyncPhase == ERROR` হলে।
4. **`JobTrackingScreen.kt`** — একই প্যাটার্ন, early-return গেটে (`problem == null ||
   minimumSkeletonActive`) `SyncBlockedRetryState` যোগ। **সতর্কতা:** এই ফাইলে শুধু এই একটা
   ছোট, সার্জিক্যাল পরিবর্তনই করা হয়েছে — বাকি ৫৮০০+ লাইন স্পর্শ করা হয়নি।
5. **`PublicProfileScreen.kt`** — `targetUser == null` case-টাকে দুই ভাগে ভাগ করা হয়েছে:
   `initialSyncPhase == ERROR` হলে `SyncBlockedRetryState` (বাল্ক-পুল ব্যর্থ, তাই ইউজার হয়তো
   Room-এই আসেনি), নাহলে আগের "ব্যবহারকারীর তথ্য পাওয়া যায়নি" মেসেজ (সত্যিকারের not-found)।
6. **`PublicProfileReviewsScreen.kt`** — বাইরে নতুন `SyncAwareContent` (sessionKey
   `public_profile_reviews_sync_$userId`, `initialSyncPhase`), ভেতরে বিদ্যমান
   `isLoading`/`minimumSkeletonActive`-ভিত্তিক local-loading আচরণ অক্ষত রাখা হয়েছে
   (Messages/WithdrawalHistory-এর প্যাটার্নের মতোই nested)।

### ❌ এই সেশনে যা শুরুই করা হয়নি (এখনো সম্পূর্ণ অপরিবর্তিত)

- **Group B বাকি ৯টা স্ক্রিন:** `SolverKycScreen.kt`, `SolverBalanceWithdrawScreen.kt`,
  `SolverCategoryPostsScreen.kt`, `UserWithdrawScreen.kt`, `PostProblemScreen.kt`,
  `DisputeResultScreen.kt`, `SupportCenterScreen.kt`, `UserInfoScreen.kt`,
  `ActiveJobsPopupScreen.kt`, `SolverSkillsScreen.kt` (এটা ১০টা, তালিকাটা মূলত ১৫টার মধ্যে
  বাকি অংশ)।
  - এর মধ্যে `PostProblemScreen.kt`, `SolverCategoryPostsScreen.kt`, `SolverSkillsScreen.kt` —
    এই ৩টা একাধিক SyncPhase-এর উপর নির্ভরশীল (categories + users/problems), তাই এদের জন্য
    আগে `MotionToolkit.kt`/ViewModel-এ একটা ছোট `combineSyncPhases(vararg phases): SyncPhase`
    হেল্পার (ERROR > LOADING > LOADED অগ্রাধিকারে) বানাতে হবে — এখনো বানানো হয়নি।
  - `SupportCenterScreen.kt`-এ কোনো `viewModel`-নির্ভর ডাটা পাওয়া যায়নি (গ্রেপ করে যাচাই করা
    হয়েছে) — সম্ভবত এটা static নেভিগেশন-অনলি স্ক্রিন, তাই হয়তো `SyncAwareContent` লাগবেই না;
    পরের সেশনে ফাইলটা পুরো পড়ে নিশ্চিত করতে হবে।
- **`SolverReviewsScreen.kt`, `UserReviewsScreen.kt`** — এখনো স্পর্শ করা হয়নি
  (`ratingsSyncPhase` ব্যবহার করার কথা, কিন্তু সেটা কখনো ERROR/LOADING হয় না বলে
  ব্যবহারিক প্রভাব কম — আগের সেশনের নোট অনুযায়ী)।
- **ডেড-কোড ক্লিনআপ (`rememberAdminTabReady`/`rememberPageDataReady`)** — **করা হয়নি, এবং
  করা ঠিকও হতো না।** এই সেশনে যাচাই করে দেখা গেছে এই দুটো হেল্পার আসলে dead code না — এগুলো
  network-sync (SyncPhase) থেকে সম্পূর্ণ আলাদা একটা concern (local Room data readiness/
  empty-state timeout) হ্যান্ডেল করে, আর Group A-এর সব স্ক্রিনেই (already migrated) এখনো
  সক্রিয়ভাবে ব্যবহৃত হচ্ছে (`SessionAwareLoadingContent`-এর ভেতরে/পাশে)। তাই ভবিষ্যতেও এই
  দুটো ফাংশন মোছার সুযোগ কম, যদি না পুরো local-readiness মেকানিজমটাই আলাদাভাবে রিফ্যাক্টর
  করার সিদ্ধান্ত নেওয়া হয় (এই রোডম্যাপের স্কোপের বাইরে)।
- **`ChatScreen.kt`, `ProblemDetailScreen.kt`** — স্পর্শ করা হয়নি (realtime-scoping wiring-এর
  কারণে বাড়তি সতর্কতা দরকার, আগের সেশনের নোট অনুযায়ী)।
- **৭৯-এরর CSV-import ফিক্স রি-ভেরিফিকেশন** — এই সেশনে করা হয়নি।
- **ধাপ ৫/৬/৭ (page re-entry retry, pull-to-refresh retry, connectivity retry)** — শুরুই
  হয়নি, কারণ ব্যবহারকারী স্পষ্টভাবে বলেছিলেন আগে ধাপ ৪ বাকি অংশ শেষ করতে।

### 🔜 পরের সেশন ঠিক এখান থেকে শুরু করবে

1. `combineSyncPhases()` হেল্পার বানানো (`MotionToolkit.kt`-এ, বা ViewModel-এ)।
2. উপরের তালিকার বাকি ৯টা Group B স্ক্রিন + Reviews ২টা স্ক্রিন একই প্যাটার্নে (স্ক্রিনের
   কাঠামো অনুযায়ী — Scaffold if/else হলে NotificationDetail/PublicProfileReviews-এর মতো
   পূর্ণ `SyncAwareContent` wrap, early-return গেট হলে ReputationDetail/JobTracking-এর মতো
   `SyncBlockedRetryState`)।
3. `SupportCenterScreen.kt` পুরো পড়ে দেখা এটার আদৌ কোনো sync-নির্ভর ডাটা আছে কিনা।
4. তারপরই ধাপ ৫ শুরু করা (ব্যবহারকারীর নির্দেশ অনুযায়ী)।

**⚠️ যা ভেরিফাই করা হয়নি (সব সেশনের মতোই এই sandbox-এ network/Gradle নেই):** আসল Kotlin
build/compile। এই সেশনে যা যাচাই করা হয়েছে তা শুধু bracket/paren-balance (প্রতিটা edited
ফাইলে {}/() কাউন্ট মিলেছে) — সেটা syntax-এর একটা দুর্বল প্রক্সি মাত্র, প্রকৃত টাইপ-চেক/কম্পাইল
এরর ধরবে না।

## ✅ ষষ্ঠ সেশন (এই সেশন) — ধাপ ৭ (ঐচ্ছিক/বোনাস, connectivity-ফিরলে automatic retry) — সম্পূর্ণ

আপলোড করা zip-এর নাম ছিল `somadhan-loading-sync-step6-complete.zip` — অর্থাৎ ধাপ ১-৬ সম্পূর্ণ।
এই সেশনে `somadhan-loading-fix-roadmap-v2.md`-এর ধাপ ৭ প্রম্পট অনুযায়ী কাজ করা হলো।

### কোথায় বসানো হয়েছে
`SomadhanViewModel.kt`-এ সরাসরি (আলাদা helper ক্লাস না — যেহেতু ব্যবহার-স্থান একটাই, আর
`AndroidViewModel.getApplication()` থেকেই দরকারি `Context` পাওয়া যায়, `LocationHelper`/
`ImageStorageUtil` কলগুলোর মতোই প্যাটার্নে):

- নতুন `_isOnline`/`isOnline: StateFlow<Boolean>` — বর্তমান connectivity অবস্থা এক্সপোজ করে
  (ভবিষ্যতে UI-তে অফলাইন ব্যানার দেখাতে চাইলে কাজে লাগতে পারে, এই ধাপে UI-তে ব্যবহার হয়নি)।
- `startConnectivityObserver()` — `ConnectivityManager.registerNetworkCallback()` দিয়ে
  `NetworkCallback` বসায় (`NET_CAPABILITY_INTERNET` ক্যাপাবিলিটি ফিল্টার সহ), `init {}`
  ব্লকে (`attachDatabase()`-এর ঠিক পরে) কল হয়।
- `onCleared()` override — `unregisterNetworkCallback()` দিয়ে cleanup করে (আগে এই ViewModel-এ
  `onCleared()` override ছিলই না, নতুন যোগ করা হয়েছে)।

### Retry logic — শুধু ৩টা SyncPhase observe করা হয়েছে
`initialSyncPhase`/`categoriesSyncPhase`/`faqsSyncPhase` — কারণ ডিজাইন অনুযায়ী এই তিনটাই একমাত্র
phase যা কখনো `ERROR`-এ যেতে পারে (`platformSettingsSyncPhase`/`auditLogsSyncPhase`/
`ratingsSyncPhase` সবসময় `LOADED`, কোনো retry-ফাংশনও নেই — ধাপ ২-এর নোট দেখুন)। `onAvailable()`-এ
`false → true` transition-এ (শুধু, বারবার `onAvailable` থেকে না) `retryAllErroredSyncPhasesOnReconnect()`
কল হয়, যেটা প্রতিটা phase-এর বর্তমান মান `ERROR` কিনা চেক করে সংশ্লিষ্ট বিদ্যমান retry-ফাংশন
(`retryInitialSync()`/`retryCategoriesSync()`/`retryFaqsSync()`) কল করে।

### Cooldown/duplicate-guard
- **Cooldown:** নতুন ছোট `connectivityRetryLastAttemptMs`/`tryConsumeConnectivityRetryCooldown()`
  (ধাপ ৫-এর `MotionToolkit.kt`-এর `SyncResumeRetryCooldown`-এর মতোই প্যাটার্ন, ৫ সেকেন্ড) —
  কিন্তু সেটা UI-স্তরে per-sessionKey (প্রতিটা স্ক্রিনের জন্য আলাদা), এটা ViewModel-স্তরে
  per-sync-domain (`"initial"`/`"categories"`/`"faqs"`, স্ক্রিন-নির্বিশেষে একবারই) — কারণ
  connectivity ফিরে আসা একটা app-wide ইভেন্ট। এটা মূলত flapping network (দ্রুত বারবার অন/অফ)
  থেকে সুরক্ষা দেয়।
- **Duplicate/racing-fetch guard:** এটা নতুন করে বানাতে হয়নি — ধাপ ৫-এই ইতিমধ্যে
  `SupabaseRealtimeManager.initialSyncInFlight`/`SomadhanRepository.categoriesSyncInFlight`/
  `faqsSyncInFlight` (তিনটাই `AtomicBoolean`) বসানো ছিল, যেগুলো `performInitialSync()`/
  `ensureCategoriesSeeded()`/`ensureFaqsSeeded()`-এর ভেতরেই গার্ড করে — তাই এখন retry চারটা
  ভিন্ন উৎস থেকে (ম্যানুয়াল বাটন, ধাপ ৫-এর resume-retry, ধাপ ৬-এর pull-to-refresh, আর এখন
  connectivity-retry) একসাথে ট্রিগার হলেও কখনো দুটো সমান্তরাল fetch চলবে না — যেটাই একই fetch
  চালাচ্ছে সেটাই আগে শেষ করবে, বাকিরা no-op হয়ে ফিরে আসবে।
- Automatic retry নীরবে ব্যাকগ্রাউন্ডে হয় — কোনো নতুন toast/snackbar/notification যোগ করা হয়নি
  (SyncPhase/Room দুটোই reactive বলে ডাটা এলে UI নিজে থেকেই আপডেট হবে)।

### ভেরিফিকেশন
`SomadhanViewModel.kt`-এ bracket/paren-balance চেক করা হয়েছে — এই সেশনের এডিটের আগে/পরে দুটোই
মিলেছে (আগে ১২৯০/১২৯০ brace, ২৬৯৭/২৬৯৭ paren; এখন ১৩০৮/১৩০৮ brace, ২৭৫২/২৭৫২ paren — পরিবর্তনের
পরিমাণের সাথে সামঞ্জস্যপূর্ণ)। `AndroidManifest.xml`-এ `ACCESS_NETWORK_STATE` পারমিশন আগে থেকেই
ছিল (নতুন কিছু যোগ করা লাগেনি)। zip-এর আগে file-list diff করে নিশ্চিত করা হয়েছে মূল zip-এর সব
২৪৩টা ফাইল (dotfile `.env`/`.gitignore` সহ) নতুন zip-এও অক্ষত আছে।

**⚠️ যা ভেরিফাই করা হয়নি (সব সেশনের মতোই এই sandbox-এ network/Gradle নেই):** আসল Kotlin
build/compile/run। বিশেষভাবে: Android Studio-তে build করে দেখা, তারপর ডিভাইসে airplane mode
অন করে একটা পেজে (যেমন টাকা-সংক্রান্ত কোনো পেজ) এরর দেখে, সেই একই পেজে বসে থেকেই (অন্য ট্যাবে না
গিয়ে, pull না করেই) airplane mode অফ করে যাচাই করা যে ডাটা নিজে থেকেই লোড হয়ে যাচ্ছে কিনা।

## 🎉 রোডম্যাপ v2-এর সব ধাপ (১-৭) এখন সম্পূর্ণ

বাকি আছে শুধু ব্যবহারকারীর নিজের Android Studio build/run আর `somadhan-loading-fix-roadmap-v2.md`-এর
ধাপ ৮ (চূড়ান্ত ম্যানুয়াল টেস্ট চেকলিস্ট) — বিশেষ মনোযোগ: টাকা-সংক্রান্ত পেজ, AdminPanel-এর
প্রতিটা ট্যাব, HorizontalPager+pull-to-refresh gesture-conflict, আর নতুন connectivity-retry
চেকলিস্ট আইটেমগুলো।
