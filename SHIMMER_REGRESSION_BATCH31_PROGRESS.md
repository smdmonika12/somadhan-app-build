# শিমার/লোডিং রিগ্রেশন — ব্যাচ ৩১ প্রোগ্রেস ফাইল

এই ফাইলটা `SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md`-এর জন্য। ধাপ ৩ (বাধ্যতামূলক চেকলিস্ট,
নিয়ম #৬) অনুযায়ী প্রতিটা সেশনের কাজ/অবস্থা এখানে ট্র্যাক করা হবে।

**কোনো কোড এডিট এখনো হয়নি** — এই পুরো ফাইলটা শুধু ধাপ ১ (রুট-কজ ডায়াগনসিস, শুধু রিপোর্ট, কোনো
এডিট না) সেশনের কাজ, এবং সেই ধাপটাই এখনো অসম্পূর্ণ (নিচে দেখুন)।

---

## ✅ প্রথম সেশন (এই সেশন) — ধাপ ১ ডায়াগনসিস: বাগ ১ সম্পূর্ণ, বাগ ২ আংশিক

### ✅ বাগ ১ — "স্টাক শিমার" (ওয়ালেট রিচার্জের পরে) — রুট-কজ পাওয়া গেছে, উচ্চ আত্মবিশ্বাস

**ফাইল/ফাংশন:** `MotionToolkit.kt` → `SyncAwareRefreshableContent<T>()` (লাইন ~৮১৩-৯৫৩)।

**রুট-কজ:** এই কম্পোনেন্টে `flashRequestCount` নামের একটা কাউন্টার (`Int`, `remember`) আছে যেটা
`isRefreshFlashing = flashRequestCount > 0` দিয়ে ঠিক করে কখন ভেতরের skeleton দেখানো হবে। ডেটা
বদলালে (`LaunchedEffect(data) { ... }`, লাইন ৮৭৬-৯০৬):

```
flashRequestCount++
delay(refreshFlashMs)   // ডিফল্ট ৩৫০ms
lastShownData = data
flashRequestCount--
```

এই `LaunchedEffect` কী `data` (পুরো `walletSyncData` লিস্ট, `UserWalletScreen.kt`-এ
`currentUser`/`userTransactions`/ইত্যাদির একটা `listOf(...)`)। **সমস্যা:** `data` আবার বদলালে
Compose পুরনো coroutine-টা **cancel** করে নতুন করে লঞ্চ করে — কিন্তু cancellation
`delay(refreshFlashMs)`-এর সময় ঘটলে, তার পরের লাইন (`flashRequestCount--`) **কখনো রান হয় না**
(কোনো `finally` ব্লক নেই)। ফলে:

- t=0: ডেটা বদলাল (যেমন `_currentUser.value = freshUser`, balance আপডেট) → `flashRequestCount`
  ০→১, ৩৫০ms delay শুরু।
- t≈৫০ms (এখনো delay চলছে): দ্বিতীয়বার ডেটা বদলাল (যেমন recharge-এর ফলে ইনসার্ট হওয়া নতুন
  transaction রো-টা Room Flow দিয়ে `userTransactions` StateFlow-তে পৌঁছাল, `_currentUser`-এর
  আপডেটের চেয়ে সামান্য পরে/আলাদা emission হিসেবে) → `LaunchedEffect(data)` নতুন key দেখে পুরনো
  coroutine cancel করে, `flashRequestCount--` স্কিপ হয়ে যায়, নতুন coroutine শুরু হয় →
  `flashRequestCount` ১→২।
- t≈৪০০ms: এই শেষ coroutine স্বাভাবিকভাবে শেষ হয় → `flashRequestCount--` → ২→**১**, ০ না।

**ফল: `flashRequestCount` স্থায়ীভাবে ১-এ আটকে যায় → `isRefreshFlashing` চিরকাল `true` →
skeleton আর কখনো সরে না, ঠিক ব্যবহারকারীর বর্ণনা করা বাগের মতোই (অন্য পেজে গিয়ে আবার এলে ঠিক হয়ে
যায়, কারণ `flashRequestCount` ওই sessionKey-র জন্য `remember(sessionKey)`-তে নতুন composition-এ
আবার ০ থেকে শুরু হয়)।**

রিচার্জ-ফ্লো এই বাগ ট্রিগার করার সবচেয়ে সহজ জায়গা কেন: `depositMoneyViaGateway()`
(`SomadhanViewModel.kt` লাইন ৪৭৩৮) একসাথে **দুটো আলাদা সোর্স থেকে ডেটা বদলায়** —
(ক) `_currentUser.value = freshUser` (balance) সরাসরি লাইন ৪৭৬৪-এ, আর
(খ) নতুন `transactions` রো ইনসার্ট (repository-তে) যেটা Room-এর নিজস্ব Flow দিয়ে
`userTransactions` StateFlow-কে আলাদা, সামান্য দেরিতে (DB commit + Flow re-query-এর সময়) আপডেট
করে। এই দুটো emission ৩৫০ms উইন্ডোর মধ্যে প্রায় নিশ্চিতভাবেই পড়ে, তাই এই একই ক্লাসের বাগ থাকা
সত্ত্বেও সাধারণ single-field আপডেটে (যেমন শুধু নোটিফিকেশন read মার্ক করা) কম দেখা যায় — খুব কম
সময়ের ব্যবধানে ২+ বার ডেটা বদলানোর কনজেশন লাগে।

**গ্রেপ-স্কোপ:** এই একই coroutine-cancellation-mid-flash প্যাটার্ন `SyncAwareRefreshableContent`
ব্যবহার করা **সবগুলো** স্ক্রিনেই সম্ভাব্য ঝুঁকি (২৮টা ফাইলে ৯৭ বার রেফারেন্স আছে — গ্রেপে পাওয়া
গেছে, প্রতিটা কল-সাইট আলাদাভাবে ভেরিফাই করা হয়নি এই সেশনে) — যেকোনো স্ক্রিনে যদি কোনো action-এর
পরে ৩৫০ms-এর মধ্যে ২+ বার আলাদা উৎস থেকে ডেটা বদলায় (একাধিক টেবিল/StateFlow প্রায় একই সময়ে
আপডেট হলে) তাহলে একই "স্টাক শিমার" হতে পারে। একই ফাইলের `isManualRefreshing` ব্লকেও (লাইন
৯১২-৯২১, pull-to-refresh শেষ হওয়ার flash) হুবহু একই প্যাটার্ন (`flashRequestCount++` →
`delay` → `flashRequestCount--`, `finally` ছাড়া) আছে, যদিও `isManualRefreshing` boolean একবারই
সত্যি→মিথ্যা হয় বলে ওই ব্লকে ডাবল-ট্রিগার হওয়ার সুযোগ কম (কিন্তু ডেটা-চেঞ্জ effect আর এই effect
একই সময়ে ওভারল্যাপ করলে ঝুঁকি থেকেই যায়)।

**প্রস্তাবিত ফিক্স-approach (কোড লেখা হয়নি, শুধু দিক-নির্দেশনা, ধাপ ২.৩ সেশনের জন্য):**
`flashRequestCount--` কে `finally { flashRequestCount-- }`-এ নেওয়া (দুই জায়গাতেই — data-change
effect আর isManualRefreshing effect) যাতে cancellation হলেও decrement গ্যারান্টিড রান করে। এটা
`SyncAwareRefreshableContent`-এর ভেতরের একটামাত্র শেয়ার্ড ফাংশন বলে এই একটা ফিক্সই ২৮টা স্ক্রিনের
জন্যই কাজ করবে (আলাদা আলাদা স্ক্রিন ছুঁতে হবে না) — কিন্তু ধাপ ০.১৬ অনুযায়ী এটা একটা systemic/শেয়ার্ড
কম্পোনেন্ট বদলানোর সিদ্ধান্ত, তাই ব্যবহারকারীর কনফার্মেশন ছাড়া এই সেশনে ছোঁয়া হয়নি (এটা diagnosis-only
সেশন, তাছাড়া মূল প্রম্পটেও এই ফিক্স `UserWalletScreen.kt`/ধাপ ২.৩-এ করার কথা বলা আছে)।

### 🔶 বাগ ২ — "random auto full-page shimmer" — আংশিক তদন্ত, রুট-কজ এখনো পাওয়া যায়নি

**যা রুল-আউট করা হয়েছে (গ্রেপ + কোড পড়ে নিশ্চিত, এগুলো বাগ ২-এর কারণ না):**

- **Connectivity-retry (ধাপ ৭):** `retryAllErroredSyncPhasesOnReconnect()`
  (`SomadhanViewModel.kt` লাইন ১৬৮৪) প্রতিটা phase আগে `== ERROR` কিনা চেক করেই তবে
  `retryInitialSync()`/ইত্যাদি কল করে — সত্যিকারের ERROR ছাড়া phase LOADING-এ ফেরে না এখান
  থেকে। (edge case: flapping network — সংক্ষিপ্ত সময়ের জন্য সত্যিই ERROR হয়ে আবার ঠিক হয়ে
  গেলে এটা "reproducible legitimate" হবে, "random" না — তবু নোট করা হলো)।
- **Pull-to-refresh-এর retry-if-ERROR (ধাপ ৬):** `refreshData()`/`refreshWalletData()`-ও একই
  রকম `if (initialSyncPhase.value == ERROR)` গার্ড দিয়ে করা — ম্যানুয়াল pull-to-refresh ছাড়া
  এটা ট্রিগার হয় না।
- **`attachDatabase()` বারবার কল হওয়া:** `SomadhanApp.kt` (Application-level, একবার) আর
  `SomadhanViewModel.kt`-এর `init{}` (লাইন ১৮৯৭) — এই দুই জায়গা থেকেই কল হয়, কিন্তু
  `attachDatabase()`-এর ভেতরের `if (localDb === database) return` (একই singleton `AppDatabase`
  instance) দ্বিতীয় কলটাকে no-op করে দেয় স্বাভাবিক অবস্থায়। প্রসেস রিস্টার্ট ছাড়া এটা phase
  আবার LOADING করার কথা না।
- **`ViewModel.init{}`-এর ভেতরে থাকা স্টার্টআপ `pullBulkDataFromSupabase()` সরাসরি কল**
  (লাইন ১৯৫৪, `FULL_SYNC_MIN_INTERVAL_MS` থ্রেশহোল্ড পার হলে) — এই ফাংশনটা নিজে
  `_initialSyncPhase` **টাচই করে না** (শুধু `performInitialSync()` করে, যেটা এটাকে র‍্যাপ করে
  না) — তাই এটা full-page re-skeleton ট্রিগার করার কথা না, যদিও এটা `initialSyncInFlight`
  গার্ডের বাইরে দিয়ে চলে বলে `attachDatabase()`-এর সাথে race করে সমান্তরাল দুটো বাল্ক-পুল একসাথে
  চলার সুযোগ থেকে যায় (আলাদা, সম্ভবত নিরীহ কিন্তু নোট করার মতো issue — বাগ ২-এর সরাসরি কারণ
  মনে হচ্ছে না যেহেতু phase-ই বদলায় না এখান থেকে)।

**এখনো যাচাই করা হয়নি (পরের সেশনের প্রথম কাজ):**

1. `sessionLoadedScreens`/`hasLoadedOnce()`/`markLoadedOnce()` (লাইন ১২৭-১২৯, plain
   `mutableSetOf`) — এটা Compose main-thread-এই সবসময় অ্যাক্সেস হয় ধরে নেওয়া হচ্ছে, কিন্তু
   এখনো নিশ্চিত করা হয়নি এমন কোনো কল-সাইট নেই যেটা ViewModel-এর `viewModelScope`/অন্য কোনো
   background coroutine থেকে এটা কল করে (হলে race/ConcurrentModificationException-জাতীয়
   কিছু হতে পারে, কিন্তু সেটা shimmer-stuck-in-loading না, বরং crash হওয়ার কথা — কম সম্ভাবনাময়
   হাইপোথিসিস, তবু বাদ দেওয়া হয়নি)।
2. **`rememberSessionAwareSkeletonGate()`-এর নিজের ইমপ্লিমেন্টেশন এখনো পড়া হয়নি** (শুধু
   কল-সাইট দেখা হয়েছে) — `MotionToolkit.kt` লাইন ৪৭১-এ সংজ্ঞায়িত। এটাই `withinFirstVisitWindow`
   ঠিক করে, যেটা `effectivePhase`-কে জোর করে LOADING বানাতে পারে (লাইন ৮৩৫-৮৩৯) এমনকি আসল
   `syncPhase` LOADED থাকা সত্ত্বেও। যদি এই গেট কোনো timer/key ভুলভাবে রিসেট হয় (যেমন কোনো
   `remember(key)`-এর key অসাবধানে recomposition-এ বদলে যাচ্ছে), সেটাই "random full re-skeleton"
   এর ব্যাখ্যা হতে পারে — **এটাই এই মুহূর্তে সবচেয়ে সম্ভাবনাময় পরবর্তী লিড, পরের সেশন এখান থেকে
   শুরু করবে।**
3. `SupabaseRealtimeManager.kt`-এর `startRealtimeListeners()`/realtime-subscription
   reconnect-লজিক এখনো পড়া হয়নি — Supabase Realtime channel disconnect/resubscribe (যেমন
   app background→foreground-এ Android কর্তৃক socket বন্ধ হয়ে আবার কানেক্ট হওয়া) কোনো
   phase/state রিসেট করে কিনা তা যাচাই বাকি।
4. `ActiveJobsPopupScreen.kt`/অন্যান্য যেসব স্ক্রিন একাধিক sessionKey/একাধিক
   `SyncAwareContent`/`SyncAwareRefreshableContent` instance ব্যবহার করে (role-based দুইটা
   আলাদা sessionKey, ইত্যাদি) — সেখানে কোনো ক্রস-কন্টামিনেশন আছে কিনা এখনো চেক করা হয়নি।
5. ব্যবহারকারীর মূল বর্ণনা ("মাঝে মাঝে বিভিন্ন পেজে হয়, নির্দিষ্ট কোনো পেজ না") অনুযায়ী এটা কোনো
   pattern/trigger-নির্দিষ্ট নাকি সত্যিই random — এটা নিশ্চিত করতে আরও নির্দিষ্ট পুনরাবৃত্তি-শর্ত
   (background→foreground, না নির্দিষ্ট নেভিগেশন সিকোয়েন্স) ব্যবহারকারীর কাছ থেকে জেনে নেওয়া
   দরকার হতে পারে (ধাপ ০.১৬ — অনুমান না করে জিজ্ঞাসা)।

---

## ✅ দ্বিতীয় সেশন (এই সেশন) — বাগ ২ ডায়াগনসিস সম্পূর্ণ (কোনো এডিট হয়নি, শুধু পড়া হয়েছে)

আগের সেশনের "পরের সেশন এখান থেকে শুরু করবে" তালিকার ১-৩ নম্বর আইটেম এই সেশনে যাচাই করা হলো:

**১. `rememberSessionAwareSkeletonGate()`-এর পূর্ণ ইমপ্লিমেন্টেশন (`MotionToolkit.kt` লাইন
৪৭১-৪৭৮) পড়া হয়েছে — এই হাইপোথিসিস রুল-আউট।** এই গেট শুধু `viewModel.hasLoadedOnce(sessionKey)
== false` অবস্থায়ই `effectivePhase`-কে জোর করে LOADING বানাতে পারে (`SyncAwareContent`-এর লাইন
৭১১-৭১৬)। আর `sessionLoadedScreens` (`SomadhanViewModel.kt` লাইন ১২৭-১২৯, `mutableSetOf`)
সম্পূর্ণ কোডবেসে **কোথাও clear/remove হয় না** — শুধু `add()` হয়, কখনো না। তাই কোনো sessionKey
একবার `markLoadedOnce` হয়ে গেলে সেই অ্যাপ-সেশনে চিরকালের জন্য `hasLoadedOnce == true` থাকে, আর
গেট আর কখনো effectivePhase বদলাতে পারে না। **ফলে এই গেট দিয়ে আগে-ভিজিট-করা কোনো স্ক্রিনে
"random re-skeleton" ব্যাখ্যা করা সম্ভব না** — এটা শুধু প্রতিটা sessionKey-র *প্রথম* ভিজিটেই
প্রভাব ফেলে, তারপর কখনো না। এই হাইপোথিসিস (আগের সেশনে "সবচেয়ে সম্ভাবনাময় লিড" বলা হয়েছিল) বাতিল।

**৩. `SupabaseRealtimeManager.kt`-এর `startRealtimeListeners()` পড়া হয়েছে — রুল-আউট।** এই
ফাংশন (লাইন ১৪৭৭ থেকে) `_initialSyncPhase`/`_categoriesSyncPhase`/`_faqsSyncPhase` কোনোটাই
**স্পর্শই করে না** — পুরো ফাইলে `_initialSyncPhase`-এ লেখা হয় মাত্র দুই জায়গায় (লাইন ২৪৪, ২৪৯/২৫৩),
দুটোই `performInitialSync()`-এর ভেতরে, `startRealtimeListeners()`-এর বাইরে। তাই channel
disconnect/resubscribe (app background→foreground ইত্যাদি) সরাসরি কোনো phase রিসেট করতে পারে
না। রুল-আউট।

**অতিরিক্ত যাচাই (এই সেশনে নতুন):** `categoriesSyncPhase`/`faqsSyncPhase` (repository-লেভেল, আলাদা
StateFlow) কোথায় LOADING-এ সেট হয় তা-ও ট্রেস করা হলো — `ensureCategoriesSeeded()`/
`ensureFaqsSeeded()` শুধু (ক) app startup (`ViewModel.init{}`, একবারই) আর (খ) `retryCategoriesSync()`/
`retryFaqsSync()` (শুধু screen-এর `onRetry` callback থেকে, যেটা শুধু ERROR অবস্থায় বাটনে চাপলে বা
`SyncAwareContent`-এর ON_RESUME-ERROR-গার্ডেড অটো-রিট্রাই থেকেই কল হয়) — এই দুই উৎস থেকেই কল হয়।
কোনো unconditional/periodic কল-সাইট পাওয়া যায়নি। `worstSyncPhase()` (লাইন ৯৬৮-৯৭৪) নিজেও একটা
pure function, কোনো state-mutation নেই।

**উপসংহার — বাগ ২-এর জন্য এখন পর্যন্ত কোনো নতুন deterministic কোড-বাগ পাওয়া যায়নি।** যত জায়গায়
`SyncPhase` LOADED থেকে LOADING-এ ফিরতে পারে (`_initialSyncPhase`/`_categoriesSyncPhase`/
`_faqsSyncPhase` — এই তিনটাই একমাত্র non-permanently-LOADED phase, বাকিগুলো ডিজাইন অনুযায়ীই সবসময়
LOADED), সবগুলো পথই হয় (ক) app-startup-এ একবার, বা (খ) genuine `ERROR` অবস্থা থেকে retry (ম্যানুয়াল
বাটন/connectivity-reconnect/ON_RESUME) দিয়েই ট্রিগার হয় — কোনোটাই সত্যিকারের "কোনো কারণ ছাড়াই
LOADED থেকে LOADING" না। তাই সবচেয়ে সম্ভাবনাময় ব্যাখ্যা এখন এটাই দাঁড়াচ্ছে: **এটা আসলে "random" না,
বরং সংক্ষিপ্ত/flaky network condition-এ সত্যিই phase ERROR-এ যাচ্ছে, তারপর কোনো একটা retry-path
(connectivity observer/ON_RESUME/ম্যানুয়াল ট্যাব-সুইচ) সেটা LOADING-এ নিয়ে যাচ্ছে এবং তারপর আবার
LOADED হচ্ছে** — পুরো ব্যাপারটা এত দ্রুত ঘটতে পারে যে ব্যবহারকারীর কাছে মনে হচ্ছে "হঠাৎ কোনো কারণ
ছাড়াই শিমার", যদিও আসলে একটা real (কিন্তু খুব সংক্ষিপ্ত) sync-error ঘটে যাচ্ছে পেছনে।

এটা একটা অনুমান/সবচেয়ে-সম্ভাব্য-ব্যাখ্যা, নিশ্চিত রুট-কজ না — তাই ধাপ ০.১৬ অনুযায়ী এখানেই থেমে
ব্যবহারকারীর কাছে জিজ্ঞাসা করা দরকার (নিচে "পরের সেশন" সেকশনে প্রশ্নটা লেখা আছে), অনুমান করে ফিক্স
না লিখে।

**৪ নম্বর আইটেম (`ActiveJobsPopupScreen.kt` ক্রস-কন্টামিনেশন) — এই (তৃতীয়) সেশনে চেক করা হলো, রুল-আউট।**
এই একটামাত্র composable (`ActiveJobsPopup`) solver/user দুই রোলেই ব্যবহৃত হয়, কিন্তু:
- `sessionKey` role-ভিত্তিক আলাদা (`"solver_active_jobs_popup_sync"` বনাম
  `"user_active_jobs_popup_sync"`, লাইন ১৯০) — কোডবেসে গ্রেপ করে নিশ্চিত করা হলো এই key দুটো আর
  কোথাও ব্যবহার হয় না।
- আসল call-site দুটো আলাদা wrapper composable দিয়ে (`SolverActiveJobsPopup`/
  `UserActiveJobsPopup`, লাইন ৪৫৫-৪৮৩) — প্রতিটাতে `role` hardcoded পাস করা, আর এই দুটো
  `HomeScreen.kt`-এর দুটো আলাদা জায়গা থেকে (লাইন ৫৪৩ ও ১৩৭৫) কল হয়, একই সময়ে দুটো একসাথে mount
  হওয়ার কোনো পথ নেই (কোনো shared/single call-site না যেখানে role রানটাইমে বদলে যেতে পারে)।
তাই এই স্ক্রিনে দুই role-এর মধ্যে state/sessionKey ক্রস-কন্টামিনেশনের কোনো ঝুঁকি পাওয়া যায়নি —
রুল-আউট।

**৫ নম্বর আইটেম — ব্যবহারকারীর কাছে জিজ্ঞাসা করা হয়েছে, উত্তর পাওয়া গেছে:** ব্যবহারকারী নিশ্চিত
করেছেন এটা সত্যিই এলোমেলো — কোনো নির্দিষ্ট ট্রিগার (background→foreground, network-switch)
লক্ষ্য করেননি, কোনো প্যাটার্ন বোঝা যাচ্ছে না। এটা "flaky network → সংক্ষিপ্ত real ERROR → দ্রুত
retry → LOADED" হাইপোথিসিসের সাথে সামঞ্জস্যপূর্ণ (এরকম network blip-ও ব্যবহারকারীর কাছে patternless/
random-ই মনে হওয়ার কথা)। কোনো deterministic কোড-বাগ (গ্রেপ দিয়ে) পাওয়া যায়নি বলে, এটাই এখন
**working theory** — চূড়ান্ত প্রমাণিত রুট-কজ না, কিন্তু কোনো পাল্টা প্রমাণও নেই।

**বাগ ২-এর ডায়াগনসিস স্ট্যাটাস: সম্পন্ন ধরা হচ্ছে (working theory সহ)।** নিশ্চিতভাবে প্রমাণ করতে হলে
রানটাইমে `_initialSyncPhase`/`_categoriesSyncPhase`/`_faqsSyncPhase`-এর প্রতিটা transition
(timestamp সহ) লগ করে আসল ডিভাইসে reproduce করে verify করা লাগবে — কিন্তু সেটা real
Android Studio/device লাগবে (ধাপ ৩, নিয়ম #৭ — sandbox-এ সম্ভব না), তাই এটা ব্যবহারকারীর
নিজের যাচাইয়ের কাজ, এই সেশনের কাজ না।

---

## ✅ তৃতীয় সেশন (এই সেশন) — বাকি ছিল শুধু আইটেম ৪, চেক শেষ, ডায়াগনসিস ধাপ (ধাপ ১) পুরোপুরি সম্পূর্ণ

আগের সেশনের "পরের সেশন এখান থেকে শুরু করবে" তালিকার ১ ও ৩ নম্বর আইটেম দ্বিতীয় সেশনেই ইতিমধ্যে
রুল-আউট হয়ে গিয়েছিল (উপরে দেখুন); বাকি ছিল শুধু ৪ নম্বর (`ActiveJobsPopupScreen.kt`
ক্রস-কন্টামিনেশন), যেটা এই সেশনে চেক করে রুল-আউট করা হলো (উপরে বাগ ২ সেকশনে বিস্তারিত)।

**বাগ ১ (স্টাক শিমার) এবং বাগ ২ (random full-page shimmer) — দুটোরই ডায়াগনসিস এখন সম্পূর্ণ:**
- বাগ ১: নিশ্চিত রুট-কজ পাওয়া গেছে (`SyncAwareRefreshableContent`-এর `flashRequestCount`
  coroutine-cancellation বাগ, `MotionToolkit.kt`), প্রস্তাবিত ফিক্স approach লেখা আছে (উপরে)।
- বাগ ২: কোনো deterministic কোড-বাগ পাওয়া যায়নি (সব সম্ভাব্য কোড-পাথ রুল-আউট হয়েছে); working
  theory হলো সংক্ষিপ্ত flaky-network → real (কিন্তু ক্ষণস্থায়ী) `ERROR` → দ্রুত retry → `LOADED`,
  যেটা ব্যবহারকারীর কাছে patternless মনে হচ্ছে। ব্যবহারকারী নিজেও কোনো নির্দিষ্ট ট্রিগার লক্ষ্য
  করেননি বলে নিশ্চিত করেছেন, যা এই থিওরির সাথে সামঞ্জস্যপূর্ণ। এটা runtime logging দিয়ে real
  ডিভাইসে প্রমাণ করা বাকি (ব্যবহারকারীর নিজের কাজ, sandbox-এ সম্ভব না)।

**এই সেশনে কোনো ফাইল এডিট করা হয়নি** — শুধু `ActiveJobsPopupScreen.kt` পড়া/গ্রেপ করা হয়েছে
(ধাপ ১ পুরোপুরি diagnosis-only বলে brace-balance/zip-file-count ভেরিফিকেশনের দরকার নেই)। zip-এর
ভেতরে শুধু এই আপডেটেড প্রোগ্রেস ফাইলটাই বদলেছে, বাকি সব ফাইল আপলোড করা
`somadhan-batch30-fix4-composeBom.zip`-এর হুবহু অপরিবর্তিত কপি।

---

## ✅ চতুর্থ সেশন (এই সেশন) — ২.১১ (KYC সেলফি ক্যামেরা পারমিশন বাগ) — সম্পন্ন

ব্যবহারকারীর কনফার্মেশনের পর ধাপ ৪-এর সাজেস্টেড অর্ডার অনুযায়ী **২.১১ ফিক্স করা হলো** (শুধু এই
একটা সেশন প্রম্পট, বাকিগুলো এখনো বাকি)।

**ফাইল এডিট হয়েছে:** `app/src/main/java/com/example/ui/screens/SolverKycScreen.kt` (শুধু এই
একটা ফাইল)।

**রুট-কজ:** `launchCameraForSelfie()` সরাসরি `FileProvider.getUriForFile(...)` +
`selfieCameraLauncher.launch(uri)` কল করত, রানটাইম `CAMERA` পারমিশন আছে কিনা কখনো চেক করত না।
`AndroidManifest.xml`-এ `<uses-permission android:name="android.permission.CAMERA" />` declare
করা থাকলেও (dangerous permission, Android 6+ এ রানটাইমে আলাদা করে চাইতে হয়), কোথাও কোনো
`ActivityResultContracts.RequestPermission()` লঞ্চার ছিল না। ফলে পারমিশন আগে থেকে না থাকলে
`TakePicture()` কন্ট্রাক্ট সরাসরি ব্যর্থ হতো/সিস্টেম error দিত, কোনো standard Android
permission-popup দেখাত না — ব্যবহারকারীর বর্ণনার সাথে হুবহু মিলে যায়।

**ফিক্স:**
- আসল ক্যামেরা-ওপেন করার কোড (`cacheFile` + `FileProvider` + `selfieCameraLauncher.launch`)
  নতুন প্রাইভেট ফাংশন `openSelfieCamera()`-এ সরানো হয়েছে (behavior অপরিবর্তিত, hুধু নাম বদলেছে
  আর এখন দুই জায়গা থেকে কল হয়)।
- নতুন `cameraPermissionLauncher` (`rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`)
  যোগ করা হয়েছে — Allow করলে `openSelfieCamera()` সাথে সাথে কল হয় (আলাদা কোনো দ্বিতীয় ট্যাপ
  লাগে না), Deny করলে rationale-সহ নির্দিষ্ট বাংলা মেসেজ `errorMessage`-এ সেট হয় (আগের generic
  exception-মেসেজ প্যাটার্নের বদলে)।
- `launchCameraForSelfie()` (দুই জায়গা থেকে কল হওয়া পুরনো পাবলিক ফাংশন নাম, `clickable {
  launchCameraForSelfie() }` — লাইন ৭১১, ৭৪০ — অপরিবর্তিত রাখা হয়েছে, শুধু ভেতরের বডি বদলেছে)
  এখন `ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
  PackageManager.PERMISSION_GRANTED` চেক করে — থাকলে সরাসরি `openSelfieCamera()`, না থাকলে
  `cameraPermissionLauncher.launch(Manifest.permission.CAMERA)`।
- নতুন import: `android.Manifest`, `android.content.pm.PackageManager`,
  `androidx.core.content.ContextCompat`।

**স্কোপ/role-check (ধাপ ৩, নিয়ম ৪a):** `SolverKycScreen` সোলভার-রোল-only, কোনো শেয়ার্ড
user-side সমতুল্য নেই (`AdminKycView.kt` আলাদা — সেটা অ্যাডমিনের KYC-রিভিউ/অ্যাপ্রুভাল স্ক্রিন,
ক্যামেরা ক্যাপচারের কোনো কোড নেই, আর মূল প্রম্পটে অ্যাডমিন-প্যানেল স্পষ্টভাবে বাদ দেওয়া বলা আছে,
তাই ছোঁয়া হয়নি)। শুধু এই একটা call-site (`MainActivity.kt` লাইন ৯১৭) থেকে
`SolverKycScreen` ব্যবহৃত হয়।

**যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট):**
1. `{}`/`()`/`[]` ব্যালেন্স পুরো ফাইলে স্ক্রিপ্ট দিয়ে গোনা হয়েছে — সব মিলেছে (`{`: ১২৪=১২৪,
   `(`: ৪১৮=৪১৮, `[`: ০=০)।
2. `grep` দিয়ে নিশ্চিত করা হয়েছে `launchCameraForSelfie()`-এর দুটো পুরনো call-site
   (লাইন ৭১১, ৭৪০) অপরিবর্তিত আছে — কোনো নতুন call-site ভাঙেনি।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু `SolverKycScreen.kt` + এই প্রোগ্রেস ফাইল)।
4. বিদ্যমান কোনো ফাংশনালিটি/নেভিগেশন বদলানো হয়নি — শুধু নতুন permission-request পাথ যোগ হয়েছে,
   আগের document-picker (front/back, `GetContent()`) লজিক অস্পর্শিত।
5. zip-এ পুরো প্রজেক্ট আছে (২৪৭টা ফাইল, আগের zip-এর সাথে ফাইল-লিস্ট diff করে মিলিয়ে দেখা হয়েছে —
   কোনো ফাইল miss হয়নি)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — এই ফিক্স কখনো কম্পাইল করে
   দেখা হয়নি। ব্যবহারকারীকে অবশ্যই Android Studio-তে বিল্ড করে (বিশেষত: পারমিশন popup আসছে
   কিনা, Allow করলে ক্যামেরা খুলছে কিনা, Deny করলে rationale মেসেজ ঠিকমতো দেখাচ্ছে কিনা, আর
   "Don't ask again"-এর পর দ্বিতীয়বার Deny করলে কী হয় সেটাও — সিস্টেম dialog না দেখিয়ে সরাসরি
   `false` কলব্যাক দেবে, তখনও rationale মেসেজ দেখাবে যা এই মুহূর্তে যথেষ্ট, সেটিংস-এ সরাসরি নিয়ে
   যাওয়ার deep-link যোগ করা হয়নি) verify করতে হবে।

## 🔶 পঞ্চম সেশন (এই সেশন) — ২.৩ (Wallet) — অর্ধেক সম্পন্ন, ইচ্ছাকৃতভাবে অসম্পূর্ণ রাখা হলো

ব্যবহারকারীর স্পষ্ট নির্দেশ অনুযায়ী এই সেশনে ২.৩-এর **শুধু অর্ধেক** কাজ করা হয়েছে — বাকি অর্ধেক
পরের session-এর জন্য নিচে বিস্তারিত লেখা হলো, এই সেশনে সেটা ছোঁয়া হয়নি।

### ✅ যা এই সেশনে করা হয়েছে — বাগ ১ (স্টাক শিমার)-এর `finally` ফিক্স

**ফাইল এডিট হয়েছে:** `app/src/main/java/com/example/ui/components/MotionToolkit.kt` (শুধু এই
একটা ফাইল, `SyncAwareRefreshableContent<T>()` ফাংশনের ভেতরে)।

প্রথম সেশনের ডায়াগনসিসে প্রস্তাবিত approach অনুযায়ী, `flashRequestCount--` কে `finally` ব্লকে
নেওয়া হয়েছে — কিন্তু আসলে প্রোগ্রেস নোটে "দুই জায়গা" বলা হলেও কোডে গিয়ে দেখা গেছে **তিনটা জায়গায়**
এই প্যাটার্ন ছিল (`LaunchedEffect(data)`-এর ভেতরে দুইটা branch — re-entry flash আর real data-change
flash — আর `LaunchedEffect(isManualRefreshing)`-এর একটা), তিনটাতেই ফিক্স করা হয়েছে:
```kotlin
flashRequestCount++
try {
    delay(refreshFlashMs)
    lastShownData = data
} finally {
    flashRequestCount--
}
```
এখন কোনো একটা `LaunchedEffect(data)` coroutine mid-delay অবস্থায় cancel হলেও (দ্বিতীয় দ্রুত
data-change বা recomposition-এর কারণে) `flashRequestCount--` গ্যারান্টিড রান করবে (Kotlin
coroutine cancellation `finally` ব্লক রান করেই cancel হয়, `CancellationException` swallow করা
হয়নি — শুধু normal flow-এ decrement নিশ্চিত করা হয়েছে)। এই একটা শেয়ার্ড কম্পোনেন্ট ফিক্স বলে
২৮টা স্ক্রিনের (যেখানেই `SyncAwareRefreshableContent` ব্যবহৃত হয়) জন্যই কার্যকর হবে, শুধু
Wallet-এর জন্য না — কিন্তু এই সেশনের মূল টার্গেট (ওয়ালেট রিচার্জ-পরবর্তী ইনফিনিট-শিমার) দিয়েই
প্রথম ট্রিগার/রিপোর্ট হয়েছিল।

**যাচাই করা হয়েছে:**
- ফাইলের `{`/`}` এবং `(`/`)` ব্যালেন্স পুরো ফাইলে স্ক্রিপ্ট দিয়ে গোনা হয়েছে — সব মিলেছে
  (`{`: ১১৮=১১৮, `(`: ৪৮৩=৪৮৩)।
- `grep flashRequestCount` দিয়ে নিশ্চিত করা হয়েছে তিনটা `++`/`--` জোড়াই এখন `try`/`finally`-এর
  ভেতরে, আর ভ্যারিয়েবল ডিক্লেয়ারেশন/`isRefreshFlashing` হিসাব (লাইন ৮৭২-৮৭৩) অপরিবর্তিত আছে।
- zip-এ ফাইল-সংখ্যা মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে (২৪৭/২৪৭, কোনো ফাইল miss হয়নি) —
  শুধু `MotionToolkit.kt` আর এই প্রোগ্রেস ফাইলই বদলেছে।
- **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — কম্পাইল করে দেখা হয়নি।
  `UserWalletScreen.kt`-এ রিচার্জ করে (বা যেকোনো স্ক্রিনে দ্রুত পরপর দুইবার ডেটা বদলে) verify
  করা এখনো বাকি।

### ⏳ যা এই সেশনে ইচ্ছাকৃতভাবে করা হয়নি — পরের সেশনের জন্য বাকি (২.৩-এর বাকি অর্ধেক)

মূল প্রম্পট (`SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md` লাইন ১৪৫-১৬২, সেশন ২.৩) অনুযায়ী এই
আইটেমগুলো এখনো বাকি, `UserWalletScreen.kt` ফাইলে (এখনো একদম স্পর্শ করা হয়নি এই সেশনে):

1. **re-entry scoping:** re-entry-তে শুধু balance amount আর transaction list অংশ শিমার হওয়া
   উচিত, বাকি পেজ কাঠামো (header, action বাটন) অপরিবর্তিত থাকা উচিত — এখন grep করে verify করে
   দেখতে হবে বর্তমানে পুরো পেজ auto-reload হচ্ছে কিনা (ধাপ ০.১৪(খ)-এর systemic বাগ কিনা)।
2. **pull-to-refresh scoping:** pull-to-refresh-এ শুধু balance + transaction list শিমার হওয়া
   উচিত, বাকি কাঠামো অপরিবর্তিত।
3. **ট্যাব-চেঞ্জ scoping:** transaction history-এর ট্যাব বদলালে শুধু transaction list অংশ শিমার
   হবে, বাকি পেজ না — এখন কী হচ্ছে grep করে verify করার পর ফিক্স করতে হবে।
4. **রিচার্জ-পরবর্তী end-to-end verify:** উপরে করা `finally` ফিক্সটাই আসল রিচার্জ-ফ্লো-তে
   ইনফিনিট-শিমার বাগ পুরোপুরি সমাধান করলো কিনা grep দিয়ে কোড-পাথ ধরে ট্রেস করে নিশ্চিত হওয়া
   (`depositMoneyViaGateway()`, `SomadhanViewModel.kt` লাইন ~৪৭৩৮ এর আশেপাশে) — শুধু
   `MotionToolkit.kt`-এর জেনেরিক ফিক্সে ভরসা না করে এই নির্দিষ্ট ফ্লো-টাও আলাদাভাবে verify করা,
   কারণ dual-emission (currentUser + transactions, দুটো আলাদা StateFlow) থেকে অন্য কোনো
   সমস্যা অবশিষ্ট আছে কিনা তা এখনো নিশ্চিত করা হয়নি।
5. **role-branch verify (ধাপ ০.৫):** `UserWalletScreen.kt` শেয়ার্ড স্ক্রিন (`isSolver` দিয়ে
   user/solver দুই role-ই ব্যবহার করে) — উপরের ৪টা আইটেমই দুই role-branch-এ আলাদাভাবে grep করে
   verify করতে হবে, শুধু user-role টেস্ট করে যথেষ্ট ধরা যাবে না।

## ✅ ষষ্ঠ সেশন (এই সেশন) — ২.৩ (Wallet)-এর বাকি অর্ধেক সম্পন্ন

পঞ্চম সেশনে যে ৫টা আইটেম বাকি রাখা হয়েছিল, তার সবগুলোই এই সেশনে করা হলো। **ব্যবহারকারীকে একটা
স্পষ্টীকরণ প্রশ্ন করা হয়েছিল** (ধাপ ০.১৬ অনুযায়ী, escrow সেকশন balance-এর মতো pulse করবে নাকি
header/বাটনের মতো স্থির থাকবে তা মূল প্রম্পটে স্পষ্ট ছিল না) — **উত্তর: escrow সেকশনও balance-এর
মতো একটা dynamic ভ্যালু, তাই এটাও pulse করবে।** নিচের সব ফিক্স এই সিদ্ধান্ত অনুযায়ী করা হয়েছে।

**ফাইল এডিট হয়েছে:**
- `app/src/main/java/com/example/ui/screens/UserWalletScreen.kt`
- `app/src/main/java/com/example/ui/components/MotionToolkit.kt` (আরেকটা ছোট বাড়তি ফিক্স, নিচে
  "বাড়তি" সেকশনে বিস্তারিত)

### ১. re-entry scoping + ২. pull-to-refresh scoping (একসাথে ফিক্স হয়েছে)

**গ্রেপ-ভেরিফাই করা রুট-কজ:** পুরনো কোড (ব্যাচ ৭-এর মাইগ্রেশন) পুরো `Column`-টাকেই (balance card
লেবেল/বাটন, escrow header, transaction ট্যাব-সিলেক্টর সব-সহ) একটা একক
`SyncAwareRefreshableContent(data = walletSyncData, ...)`-এর ভেতরে রেখেছিল, যেখানে
`walletSyncData = listOf(currentUser, userEscrows, userTransactions, userGatewayPayments,
allProblems, allAdditionalCharges, minWithdrawalAmount)` — এর যেকোনো একটা বদলালেই (এমনকি
`allProblems`/`allAdditionalCharges`-এর মতো ওয়ালেট-পেজে সরাসরি না-দেখানো ডেটাও) পুরো Column
সংক্ষিপ্তভাবে flash করতো, যেটাকে ব্যবহারকারীর কাছে "মাঝে মাঝে পুরো পেজ auto-reload" মনে হচ্ছিল।
এটাই ধাপ ০.১৪(খ)-এর ইঙ্গিত করা প্যাটার্নের এই স্ক্রিনের নির্দিষ্ট instance (bug ২-এর সাধারণ
"flaky-network" working theory থেকে আলাদা — এটা একটা নির্দিষ্ট, deterministic UI-কোড কারণ)।

**ফিক্স:**
- বাইরের wrapper `SyncAwareRefreshableContent` থেকে `SyncAwareContent`-এ পাল্টানো হয়েছে (আর
  `data`/`isManualRefreshing` প্যারামিটার সরানো হয়েছে, `{ _ -> ... }` থেকে `{ ... }`)। এই
  কম্পোনেন্ট শুধু cold-load (এই app session-এ প্রথমবার)-এর জন্য পুরো-পেজ স্কেলিটন/এরর দেখায়,
  একবার LOADED হয়ে গেলে content() যা দেওয়া হয়েছে ঠিক তাই দেখায় — নিজে থেকে আর কখনো re-animate
  করে না।
- Balance amount (শুধু ভ্যালু টেক্সট, লেবেল/বাটন না) এখন `rememberFieldChangePulse(value =
  currentBalance, ...) + PulsingValue` দিয়ে র‍্যাপ করা — `currentUser`/pull-to-refresh
  সমাপ্তি/re-entry — তিন কারণেই সংক্ষিপ্ত pulse দেখাবে, বাকি balance card (লেবেল, আইকন, রিচার্জ/
  টাকা-তোলা বাটন) সবসময় স্থির।
- Escrow সেকশন (ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী পুরো সেকশন — হেডার/ব্যাজ/টোটাল/লিস্ট একসাথে)
  `rememberFieldChangePulse(value = Pair(totalHeldEscrowAmount, heldEscrows), ...) +
  PulsingValue` দিয়ে র‍্যাপ — একই তিন কারণে pulse করে, Card-এর background/border কাঠামো হিসেবে
  স্থির।
- পুরনো কম্পোজিট `walletSyncData` val সরিয়ে ফেলা হয়েছে (আর কোথাও ব্যবহৃত হতো না ছাড়া এই একটা
  জায়গা)।
- সংশ্লিষ্ট stale কমেন্ট (যেগুলো আগে "পুরো content-level SyncAwareRefreshableContent-এর আওতায়"
  বলে explain করছিল, যেমন balance Text-এর উপরে আর escrow total amount-এর উপরে) আপডেট করা হয়েছে
  যেন নতুন আচরণ সঠিকভাবে বর্ণনা করে।

### ৩. ট্যাব-চেঞ্জ scoping

**গ্রেপ-ভেরিফাই করা আগের আচরণ:** `selectedHistoryTab`/`selectedAppSubFilter` পুরো Column-এর
ভেতরের `remember` state হওয়ায়, আগে ট্যাব বদলালে পুরো Column-ই (উপরের বাগের কারণে) re-flash
হতো — কারণ walletSyncData-তে থাকা কোনো একটা ফিল্ড টাচ না হলেও Column পুরোটাই একই
`SyncAwareRefreshableContent`-এর content()-এর ভেতরে ছিল, তাই তার নিজের কোনো ফিক্স/isolation
ছিল না ট্যাব-নির্দিষ্ট।

**ফিক্স:** ট্যাব-সিলেক্টর `Row` (APP/GATEWAY বাটন, count-badge-সহ) অপরিবর্তিত/আগের জায়গাতেই
রাখা হয়েছে (pulse-এর বাইরে)। শুধু নিচের list body (`if (selectedHistoryTab == "GATEWAY") {
... } else { ... }` ব্লক) একটা নতুন `transactionListPulse` (`rememberFieldChangePulse(value =
if (GATEWAY) Pair(selectedHistoryTab, sortedGatewayPayments) else
Triple(selectedHistoryTab, selectedAppSubFilter, filteredUserTransactions), ...)`) দিয়ে
`PulsingValue`-এর ভেতরে মোড়ানো হয়েছে — `selectedHistoryTab` pulse-key-এর অংশ বলে ট্যাব বদলালেই
(ডেটা সত্যিই বদলেছে কিনা না দেখেই) এই body-টুকু সংক্ষিপ্ত pulse দেখায়, বাকি পেজ (ট্যাব বাটন-সহ)
অপরিবর্তিত থাকে।

### ৪. রিচার্জ-পরবর্তী ইনফিনিট-শিমার — end-to-end ভেরিফাই সম্পন্ন

পঞ্চম সেশনে `MotionToolkit.kt`-এর `SyncAwareRefreshableContent`-এ `finally` ফিক্স হয়ে গিয়েছিল
(অপরিবর্তিত, শুধু পুনরায় গ্রেপ করে নিশ্চিত করা হলো)। এই সেশনে বাকি ট্রেসিং:

- `SomadhanViewModel.kt` → `depositMoneyViaGateway()` (লাইন ৪৭৩৮) পড়া হয়েছে — এই ফাংশন
  `_initialSyncPhase` **স্পর্শই করে না** (শুধু `_currentUser.value = freshUser` আর
  `resetTransactionsPagination()` কল করে) — তাই "phase reset মিসিং" ধরনের কোনো বাগ এখানে নেই,
  bug টা সম্পূর্ণভাবে UI-লেয়ারের কাউন্টার-বাগ (আগেই diagnose হয়েছে)।
- `resetTransactionsPagination()` → `loadNextTransactionsPage()` (লাইন ৬৮২) পড়া হয়েছে —
  ইতিমধ্যেই সঠিক `try/catch/finally` প্যাটার্নে লেখা (`finally { transactionsLoadingMore =
  false }`), কোনো exception swallow পাওয়া যায়নি।
- **dual-emission race নিশ্চিত করা হলো (গ্রেপ দিয়ে):** `_userTransactions` (যেটা
  `UserWalletScreen.kt` সরাসরি observe করে, `transactionsPaged`-এর থেকে আলাদা) একটা independent
  `viewModelScope.launch { repository.getTransactionsForUser(userId).collect { _userTransactions.value
  = it } }` (লাইন ~২৭৬৩) দিয়ে আপডেট হয় — `depositMoneyViaGateway()`-এর `_currentUser.value =
  freshUser` (লাইন ৪৭৬৪)-এর থেকে সম্পূর্ণ স্বতন্ত্র একটা Room Flow emission। ঠিক ধাপ ১-এর
  ডায়াগনসিসে বর্ণিত dual-emission race-টাই এখানে বাস্তবে ঘটে বলে গ্রেপ-ভেরিফাই হলো।
- **উপসংহার:** `finally`-ফিক্স (পঞ্চম সেশন) সরাসরি এই race-টাই handle করে। এছাড়া এই সেশনে
  balance/escrow/transaction-list-কে আলাদা আলাদা `rememberFieldChangePulse` instance-এ ভাগ করার
  ফলে (আইটেম ১-৩) এখন `currentUser` আর `userTransactions` বদলালে **দুইটা আলাদা** pulse-instance
  ট্রিগার হয় (একটার coroutine আরেকটাকে cancel করতে পারে না, যেহেতু আলাদা composable call-site,
  আলাদা `remember` state) — এটা `finally`-ফিক্সের উপরে একটা বাড়তি প্রতিরক্ষা স্তর, যদিও মূল ফিক্স
  `finally`-ব্লকটাই।
- **⚠️ real device-এ রিচার্জ করে verify করা এখনো বাকি** (sandbox-এ Gradle build সম্ভব না) —
  ব্যবহারকারীকে এটা টেস্ট করতে হবে।

### ৫. role-branch verify

মূল প্রম্পটের নিজস্ব নোট (লাইন ১৫৭-১৬২) অনুযায়ী `UserWalletScreen.kt` একটা শেয়ার্ড ফাইল
(`isSolver` দিয়ে ভেতরেই branch করে, আলাদা `SolverWalletScreen.kt` নেই) — গ্রেপ করে নিশ্চিত করা
হলো এই ফাইলে সোলভার-নির্দিষ্ট কোনো আলাদা balance/escrow/transaction-history রেন্ডারিং কোড নেই
(শুধু bottom-nav/title-এর মতো ছোট UI ডিটেইল `isSolver` দিয়ে বদলায়)। তাই উপরের ১-৪ নম্বর সব
ফিক্স একই render tree-তে হওয়ায় দুই role-ই স্বয়ংক্রিয়ভাবে কভার হয়ে গেছে — আলাদা কোনো
role-নির্দিষ্ট এডিট লাগেনি।

### বাড়তি ফিক্স (নাম-করা বাগের বাইরে, ধাপ ৩ নিয়ম #৩ অনুযায়ী) — `rememberFieldChangePulse`-এও
একই ক্লাসের বাগ পাওয়া গেছে

আইটেম ১-৩ ফিক্স করতে গিয়ে `rememberFieldChangePulse` (এই সেশনে নতুন করে ভারীভাবে ব্যবহার করা
হচ্ছে — balance/escrow/transaction-list তিনটাতেই) পড়ার সময় দেখা গেল এর ভেতরের তিনটা
`LaunchedEffect`-ও (`Unit`/reentry, `value`/data-change, `isManualRefreshing`) হুবহু একই
`activePulseReasons++` → `delay` → `activePulseReasons--` প্যাটার্নে লেখা ছিল, **কোনো
try/finally ছাড়াই** — অর্থাৎ `SyncAwareRefreshableContent`-এর `flashRequestCount`-এ যে বাগ ছিল
(পঞ্চম সেশনে ফিক্স হয়েছে), সেই একই বাগ এখানেও ছিল, এখনো unfixed। যেহেতু এই সেশনের নতুন
UserWalletScreen.kt কোড এখন এই ফাংশনের উপর ভারীভাবে নির্ভরশীল, এটা ফিক্স না করলে সেশনের নিজের
নতুন কোডেই একই "স্টাক pulse" বাগ থেকে যেত — তাই এটাও এই সেশনে ফিক্স করা হলো (৩টা জায়গাতেই
`try { delay(durationMs) } finally { activePulseReasons-- }`)। এটাও একটা শেয়ার্ড কম্পোনেন্ট
ফিক্স বলে `rememberFieldChangePulse` ব্যবহার করা অন্য সব স্ক্রিনের জন্যও কার্যকর হবে (কোন কোন
স্ক্রিন এটা ব্যবহার করে তার সম্পূর্ণ তালিকা এই সেশনে গ্রেপ করে বের করা হয়নি — পরের কোনো broad-audit
সেশনে দরকার হলে করা যেতে পারে)।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)

1. `{`/`}`/`(`/`)`/`[`/`]` ব্যালেন্স script দিয়ে গোনা হয়েছে দুটো ফাইলেই — সব মিলেছে:
   - `UserWalletScreen.kt`: `{}` ৩৯৭=৩৯৭, `()` ১৩৮৮=১৩৮৮, `[]` ০=০।
   - `MotionToolkit.kt`: `{}` ১২৫=১২৫, `()` ৪৮৯=৪৮৯, `[]` ১০১=১০১।
2. grep দিয়ে নিশ্চিত করা হয়েছে `UserWalletScreen.kt`-এ আর কোনো `SyncAwareRefreshableContent(`
   কল নেই (শুধু পুরনো/নতুন কমেন্টে নাম আছে), `walletSyncData` আর সংজ্ঞায়িত/ব্যবহৃত না। নতুন
   ব্যবহৃত `PulsingValue`/`SyncAwareContent`/`rememberFieldChangePulse` তিনটাই আগে থেকেই
   `MotionToolkit.kt`-এ (একই প্যাকেজ `com.example.ui.components`) সংজ্ঞায়িত আছে বলে নিশ্চিত করা
   হয়েছে — নতুন কোনো হেল্পার বানাতে হয়নি, শুধু import যোগ (আর অব্যবহৃত `SyncAwareRefreshableContent`
   import সরানো) হয়েছে।
3. `MotionToolkit.kt`-এ `flashRequestCount--`/`activePulseReasons--` উভয়ের সব
   occurrence grep করে নিশ্চিত করা হয়েছে সবগুলো এখন `finally` ব্লকের ভেতরে।
4. কোনো বিদ্যমান ফাংশনালিটি/নেভিগেশন/বিজনেস-লজিক বদলানো হয়নি — শুধু কোন কম্পোনেন্ট কীভাবে
   loading/pulse দেখায় তার wiring বদলেছে, `currentBalance`/`heldEscrows`/`sortedUserTransactions`
   ইত্যাদির হিসাব-লজিক অপরিবর্তিত।
4a. উপরে "৫. role-branch verify" দেখুন — কভার হয়ে গেছে, আলাদা এডিট লাগেনি।
5. zip-এর ফাইল-সংখ্যা আগের zip-এর (এই zip-এই আপলোড করা মূল প্রজেক্ট) সাথে মিলিয়ে দেখা হয়েছে —
   শুধু `UserWalletScreen.kt`, `MotionToolkit.kt`, আর এই প্রোগ্রেস ফাইলই বদলেছে, বাকি সব ফাইল
   অপরিবর্তিত কপি।
6. এই প্রোগ্রেস নোট আপডেট করা হলো (এই সেকশন)।
7. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — উপরের সব ফিক্স কম্পাইল/রান
   করে দেখা হয়নি। ব্যবহারকারীকে অবশ্যই real device/emulator-এ যাচাই করতে হবে, বিশেষত:
   - প্রথমবার ওয়ালেট পেজে ঢুকলে পুরো-পেজ স্কেলিটন দেখাচ্ছে কিনা (আগের মতোই)।
   - re-entry-তে (অন্য পেজে গিয়ে ফিরে এলে) শুধু balance/escrow/transaction-list সংক্ষিপ্ত pulse
     করছে, header/বাটন/ট্যাব-বার স্থির থাকছে কিনা।
   - pull-to-refresh করলে একই রকম শুধু ঐ তিনটা অংশ pulse করছে কিনা।
   - ট্যাব (APP/GATEWAY) বদলালে শুধু নিচের লিস্ট pulse করছে, ট্যাব বাটন/উপরের সব static থাকছে
     কিনা।
   - **সবচেয়ে গুরুত্বপূর্ণ:** রিচার্জ (Add Money) সম্পন্ন করার পর balance আপডেট হয়ে pulse
     স্বাভাবিকভাবে শেষ হয়ে যাচ্ছে কিনা (আটকে থাকছে না) — কয়েকবার পরপর দ্রুত রিচার্জ করে/অন্য
     পেজে গিয়ে-এসে আবার টেস্ট করে নিশ্চিত হওয়া ভালো।

## 🔶 সপ্তম সেশন (এই সেশন) — ২.৭ (Problem Detail + সকল সমস্যা + বিড লিস্ট) — আংশিক, একটা প্রশ্ন সহ থেমে গেছে

ধাপ ০.১৬ অনুযায়ী: ProblemDetailScreen.kt-এর "re-entry-তে critical infinite-shimmer" অংশটার জন্য
কোনো নতুন deterministic কোড-বাগ **কোডে গ্রেপ করে পাওয়া যায়নি** (নিচে বিস্তারিত), আর এটা একটা
critical-labeled বাগ বলে অনুমান করে "ঠিক আছে" ধরে না নিয়ে ব্যবহারকারীর কাছে confirm করা দরকার —
তাই এই সেশনে যা নিশ্চিতভাবে ফিক্স করা গেছে তা করে, বাকিটা প্রশ্ন-সহ থামা হলো।

### ✅ যা এই সেশনে ফিক্স হয়েছে — শিমার-shape মিসম্যাচ (`UserProblemsScreen.kt`, `SolverProblemsScreen.kt`)

**রুট-কজ (গ্রেপ-ভেরিফাই):** দুটো ফাইলেই `SyncAwareRefreshableContent`-এ কোনো কাস্টম `skeleton`
প্যারামিটার পাস করা হচ্ছিল না, তাই ডিফল্ট `ListScreenSkeleton()` ব্যবহৃত হচ্ছিল। এই ডিফল্টে একটা
অতিরিক্ত ৬৪dp "header" শিমার-ব্লক আছে (`showHeader = true`) — কিন্তু এই দুই স্ক্রিনেই আসল
tab/filter হেডার `SyncAwareRefreshableContent`-এর *বাইরে* (সবসময় স্থির/visible), তাই re-entry/
pull-to-refresh flash-এ এই ভুয়া হেডার-ব্লকটা এমন একটা জায়গায় দেখাত যেখানে আসলে শুধু পোস্ট-কার্ড
লিস্ট থাকার কথা — এটাই ব্যবহারকারীর/মূল প্রম্পটের বলা "শিমারের style আসল পোস্ট-কার্ড layout-এর
সাথে মিলছে না" পর্যবেক্ষণ।

**ফিক্স:** দুটো ফাইলেই `skeleton = { LazyColumn(contentPadding = PaddingValues(16.dp)) { items(4)
{ ProblemCardSkeleton(...) } } }` — নিচের real body (`LazyColumn` + `ProblemCard`)-এর হুবহু একই
`contentPadding` আর কার্ড-শেপে (আগে থেকেই বিদ্যমান `ProblemCardSkeleton`, `MotionToolkit.kt`-এ,
নতুন কিছু বানানো হয়নি — HomeScreen-এও এটাই ব্যবহৃত হয়) কাস্টম skeleton যোগ হয়েছে, কোনো অতিরিক্ত
header-ব্লক ছাড়া। Tint: `UserProblemsScreen.kt`-এ `Color(0xFF1D4ED8)` (এই স্ক্রিনের নিজস্ব
accentColor, `ProblemCard`-এও একই রং পাস করা হয় বলে মিলিয়ে দেওয়া হলো), `SolverProblemsScreen.kt`-এ
`SomadhanOrange` (ডিফল্ট, এই স্ক্রিনে `ProblemCard`-এ কোনো কাস্টম accentColor পাস হয় না)।

### 🔶 তদন্ত করা হয়েছে, কিন্তু নতুন কোনো কোড-বাগ পাওয়া যায়নি — ProblemDetailScreen.kt-এর "re-entry critical বাগ"

মূল প্রম্পটে বলা হয়েছে: re-entry করলে (দ্বিতীয়বার একই problem detail-এ ঢুকলে) পুরো পেজ আবার লোড
হওয়া শুরু করে এবং **কখনো থামে না**। এই সেশনে কোড গ্রেপ করে যা পাওয়া গেছে:

1. `ProblemDetailScreen.kt` **ইতিমধ্যেই** (ব্যাচ ২১-এ, ফাইলের নিজস্ব কমেন্ট অনুযায়ী)
   `SyncAwareRefreshableContent` (sessionKey = `"problem_detail_$problemId"`, data =
   `listOf(currentProblem, bids)`) ব্যবহার করে — অর্থাৎ এই স্ক্রিনটা ইতিমধ্যেই "standard"
   প্যাটার্নে migrate করা, কোনো পুরনো ম্যানুয়াল/ভাঙা early-return গেট নেই।
2. এই একই `SyncAwareRefreshableContent`-এর ভেতরের `flashRequestCount` কাউন্টার-বাগ (যেটা পঞ্চম
   সেশনে ওয়ালেট ডায়াগনসিসে পাওয়া গিয়েছিল — mid-delay coroutine-cancellation-এ decrement স্কিপ
   হয়ে কাউন্টার স্থায়ীভাবে >0 আটকে থাকা) **ইতিমধ্যেই পঞ্চম সেশনে `MotionToolkit.kt`-এ ফিক্স হয়ে
   গেছে** (`finally { flashRequestCount-- }`, তিনটা জায়গাতেই) — এই ফিক্সটা শেয়ার্ড কম্পোনেন্টে
   হওয়ায় `SyncAwareRefreshableContent` ব্যবহার করা **সব স্ক্রিনেই** (`ProblemDetailScreen.kt`-সহ)
   ইতিমধ্যেই প্রযোজ্য — গ্রেপ করে নিশ্চিত করা হলো এই ফাইলে এখনো `finally` ব্লকেই আছে, আলাদা কোনো
   copy/পুরনো ভার্সন নেই।
3. এই স্ক্রিনের নিজস্ব অতিরিক্ত লজিক (`isCheckingProblem`, লাইন ~229/272-283) — `problemId`-কী দিয়ে
   `remember`, আর `LaunchedEffect(problemId) { isCheckingProblem = true; ...; delay(1200);
   isCheckingProblem = false }` — এটাও গ্রেপ করে পড়া হয়েছে। এই effect-টা শুধু `problemId` বদলালেই
   cancel হয় (একই problemId-তে re-entry করলে composable নতুন হলেও `problemId` প্যারামিটার অপরিবর্তিত
   থাকে বলে এই effect স্বাভাবিকভাবেই ১২০০ms পরে শেষ হয়ে `false`-এ পৌঁছায়, কোনো mid-delay
   cancellation-এর সুযোগ নেই যেহেতু এর key-তে অন্য কিছু নেই)। তাছাড়া এই ফ্ল্যাগটা যেই
   `if (currentProblem == null)` ব্রাঞ্চের ভেতরেই শুধু ব্যবহৃত হয় (লাইন ২১৫১) — আর `currentProblem`
   একটা **শেয়ার্ড** `viewModel.selectedProblem` StateFlow থেকে আসা বলে (screen ছাড়ার সময় কখনো
   null-এ রিসেট হয় না), একই problem-এ re-entry করলে সাধারণত এটা আগে থেকেই non-null থাকে — তাই এই
   `isCheckingProblem`-নির্ভর ব্রাঞ্চটাই re-entry-তে কার্যকরভাবে স্কিপ হয়ে যায়।

**উপসংহার (working theory, নিশ্চিত না):** এই বাগ-রিপোর্টটা সম্ভবত মূল প্রম্পট লেখার সময়কার (পঞ্চম
সেশনের `finally`-ফিক্সের **আগের**) অবস্থা বর্ণনা করছিল, আর শেয়ার্ড কম্পোনেন্ট ফিক্স হওয়ার ফলে এটা
ইতিমধ্যেই সমাধান হয়ে থাকতে পারে — কিন্তু এটা **নিশ্চিতভাবে প্রমাণ করা যায়নি** (sandbox-এ real
device/emulator-এ reproduce করে verify করার সুযোগ নেই, Ground Rule ২)। ধাপ ০.১৬ অনুযায়ী অনুমান
করে "ফিক্সড, বাকি নেই" দাবি না করে **ব্যবহারকারীকে জিজ্ঞাসা করা হচ্ছে:**

> আগে (এই zip পাওয়ার আগে) কি আপনি real ডিভাইসে ProblemDetailScreen-এ এই "re-entry-তে কখনো না থামা
> শিমার" বাগটা `somadhan-batch31-fix2_3-wallet-half2.zip` (এই zip, wallet-এর `finally` ফিক্স
> এতে অলরেডি আছে) দিয়ে বিল্ড করার **পরেও** দেখেছেন, নাকি এটা তার **আগের** কোনো zip-এ দেখা বাগের
> বর্ণনা যেটা এখনো re-test করা হয়নি? যদি wallet-ফিক্স করা এই zip দিয়ে বিল্ড করার পরেও বাগটা এখনো
> থেকে যায়, please আমাকে বলুন — তাহলে ধরে নিতে হবে ভিন্ন কোনো root-cause আছে যেটা আমি এই
> static-code-read দিয়ে খুঁজে পাইনি, আর real ডিভাইস-লগ/logcat বা reproduce-steps দরকার হবে সেই
> ভিন্ন কারণটা বের করতে।

**এই প্রশ্নের উত্তর পাওয়ার আগে** ProblemDetailScreen.kt-এ "re-entry-তে শুধু bid list অংশ শিমার হবে"
(scoping অংশ, মূল প্রম্পটের বাকি অংশ) বা `BidManagementScreen.kt`/`SolverAllPostsScreen.kt`/
`SolverMyBidsScreen.kt`-এ "একই বাগ প্যাটার্ন" ফিক্স করা হয়নি — কারণ গ্রেপ করে দেখা গেছে এই তিনটা
ফাইলই ইতিমধ্যে সঠিক standard `SyncAwareRefreshableContent` প্যাটার্নেই আছে (কোনো `isChecking*`-জাতীয়
ভাঙা নেস্টেড গেট নেই কোনোটাতেই), অর্থাৎ এই তিনটাতে নতুন কোনো কোড-এডিট দরকার এই মুহূর্তে দেখা যাচ্ছে
না — কিন্তু ProblemDetailScreen-এর প্রশ্নটার উত্তর অনুযায়ী এই সিদ্ধান্তও বদলাতে পারে (যদি আসল
root-cause অন্য কিছু হয় যেটা এই স্ক্রিনগুলোতেও থাকতে পারে)।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট, এই সেশনে যা এডিট হয়েছে তার জন্য)

1. `{}`/`()` ব্যালেন্স স্ক্রিপ্ট দিয়ে গোনা হয়েছে দুটো এডিট-করা ফাইলেই — মিলেছে:
   `UserProblemsScreen.kt` `{}` ১৩৪=১৩৪, `()` ৩৩২=৩৩২। `SolverProblemsScreen.kt` `{}` ১০২=১০২,
   `()` ২৬০=২৬০।
2. `ProblemCardSkeleton`/`PaddingValues`/`items`/`Color`/`SomadhanOrange` — সবগুলো import আগে
   থেকেই ফাইলে ছিল বা নতুন যোগ করা হয়েছে (`ProblemCardSkeleton` দুটো ফাইলেই নতুন import, বাকি সব
   আগে থেকেই বিদ্যমান)।
3. zip-এর ফাইল-লিস্ট (dotfile-সহ, ২৪৭টা) মূল আপলোড করা zip-এর সাথে diff করে হুবহু মিলিয়ে দেখা
   হয়েছে — শুধু `UserProblemsScreen.kt`, `SolverProblemsScreen.kt`, আর এই প্রোগ্রেস ফাইলই বদলেছে।
4. কোনো বিদ্যমান ফাংশনালিটি/নেভিগেশন/ফিল্টার-লজিক বদলানো হয়নি — শুধু `skeleton` প্যারামিটার যোগ
   হয়েছে, `content` lambda-র ভেতরের real-list/empty-state লজিক অপরিবর্তিত।
5. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — উপরের skeleton-shape ফিক্স
   কম্পাইল/রান করে দেখা হয়নি। ব্যবহারকারীকে verify করতে হবে: "সকল সমস্যা" (user) আর সোলভারের
   "সকল সমস্যা/বিড লিস্ট" মেনুতে re-entry/pull-to-refresh করলে এখন শুধু post-card-shaped শিমার
   দেখাচ্ছে কিনা (আগের ভুয়া হেডার-ব্লক ছাড়া)।

## ✅ অষ্টম সেশন (এই সেশন) — ২.৭-এর বাকি অংশ (ProblemDetailScreen.kt re-entry scoping) সম্পন্ন

**ব্যবহারকারীর সিদ্ধান্ত (এই সেশনে):** সপ্তম সেশনের প্রশ্নের (ProblemDetailScreen-এর re-entry বাগ
wallet-fix সহ zip দিয়ে টেস্ট করে বাগ আছে কিনা) সরাসরি উত্তর না দিয়ে ব্যবহারকারী জানিয়েছেন তিনি এখনো
টেস্ট করেননি — পুরো ব্যাচ ৩১-এর কাজ শেষ হলে একসাথে সব টেস্ট করবেন, আর এখন কাজ চালিয়ে যেতে বলেছেন।
তাই এই সেশনে সপ্তম সেশনের "working theory" (`finally`-ফিক্সই ProblemDetailScreen-এর re-entry বাগ
সমাধান করে দিয়েছে) সঠিক ধরে নিয়ে ধাপ ৪-এর সাজেস্টেড অর্ডার অনুযায়ী ২.৭-এর বাকি অংশ বাস্তবায়ন করা
হলো। **⚠️ গুরুত্বপূর্ণ ক্যাভিয়েট:** এই working theory এখনো real device-এ verify হয়নি — ব্যবহারকারী
শেষে একসাথে টেস্ট করবেন বলেছেন, তাই যদি টেস্ট করার পর দেখা যায় ProblemDetailScreen-এ আসলে ভিন্ন
কোনো root-cause ছিল, তাহলে এই সেশনের নিচের এডিটও পুনর্বিবেচনা করতে হতে পারে।

**ফাইল এডিট হয়েছে:** `app/src/main/java/com/example/ui/screens/ProblemDetailScreen.kt` (শুধু এই
একটা ফাইল)।

### যা করা হয়েছে

মূল প্রম্পটের কাঙ্ক্ষিত আচরণ: "re-entry-তে পুরো পেজ কাঠামো অপরিবর্তিত রেখে শুধু bid list অংশ শিমার
হবে, বাকি সব (problem details) সবসময় visible/অপরিবর্তিত থাকবে।"

- বাইরের wrapper `SyncAwareRefreshableContent(sessionKey = "problem_detail_$problemId", data =
  listOf(currentProblem, bids), isManualRefreshing = isRefreshing, ...)` থেকে `SyncAwareContent`
  (একই sessionKey, `data`/`isManualRefreshing` প্যারামিটার ছাড়া)-তে পাল্টানো হয়েছে — এটাই আগে
  পুরো LazyColumn (problem details + bid list দুটোই একসাথে, একই কম্পোজিট `data`-তে) re-flash
  করার কারণ ছিল (Wallet স্ক্রিনে যে একই ক্লাসের বাগ পঞ্চম/ষষ্ঠ সেশনে পাওয়া গিয়েছিল, ঠিক সেই একই
  প্যাটার্ন — এই স্ক্রিনটাও একই কম্পোজিট-`data` ভুলের শিকার ছিল)। এখন cold-load-এর পরে content()
  আর নিজে থেকে re-animate করে না — problem details অংশ (পুরো LazyColumn-এর সিংহভাগ) সবসময় স্থির।
- নতুন `val bidsPulse = rememberFieldChangePulse(value = bids, isManualRefreshing = isRefreshing,
  sessionKey = "problem_detail_$problemId", viewModel = viewModel)` — বাইরের `SyncAwareContent`-এর
  সাথে **একই sessionKey** ব্যবহার করা হয়েছে (Wallet-এর প্যাটার্ন অনুসরণ করে) যাতে re-entry
  সনাক্তকরণ (`hasLoadedOnce`) সঠিকভাবে কাজ করে।
- শুধু bid-list অংশের ৫টা `item`/`items` ব্লক (winning bid card, "অন্যান্য বিড/সকল বিড" হেডার,
  দুটো empty-state কার্ড, আর `items(bidsToDisplay) { bid -> BidCard(...) }`) — প্রতিটার ভেতরের
  content `PulsingValue(isUpdating = bidsPulse) { ... }` দিয়ে মোড়ানো হয়েছে (দুই জায়গায় ভেতরে
  একাধিক element থাকায় `Column { ... }`-এ মুড়ে তারপর `PulsingValue`-এর ভেতরে দেওয়া হয়েছে,
  `androidx.compose.foundation.layout.Column` আগে থেকেই import করা ছিল)। ফলে শুধু `bids`
  সত্যিই বদলালে/pull-to-refresh শেষে/re-entry-তে এই অংশটুকুই সংক্ষিপ্ত pulse করে, উপরের সব
  problem-details section (title, description, status, location, timeline, payment-related
  card/dialog-trigger UI ইত্যাদি) অপরিবর্তিত/স্থির থাকে।
- পুরনো `isCheckingProblem` লজিক (currentProblem == null-এর সাব-কেসে) অক্ষত রাখা হয়েছে, স্পর্শ
  করা হয়নি।
- Import: `SyncAwareRefreshableContent` সরিয়ে `SyncAwareContent`, `PulsingValue`,
  `rememberFieldChangePulse` যোগ করা হয়েছে (তিনটাই আগে থেকেই `MotionToolkit.kt`-এ বিদ্যমান,
  Wallet স্ক্রিনেই একই তিনটা ব্যবহৃত হয়েছে — নতুন কোনো হেল্পার বানাতে হয়নি)।

### যা যাচাই করে "ঠিক আছে, নতুন এডিট লাগবে না" ধরে নেওয়া হয়েছে

সপ্তম সেশনে বলা হয়েছিল `BidManagementScreen.kt`/`SolverAllPostsScreen.kt`/`SolverMyBidsScreen.kt`
ইতিমধ্যেই সঠিক standard প্যাটার্নে আছে — এই সেশনে আবার grep করে পুনরায় নিশ্চিত করা হলো: তিনটাতেই
`SyncAwareRefreshableContent`-এর `data` প্যারামিটার সরাসরি সেই স্ক্রিনের নিজস্ব তালিকা
(`pageProblems`/`displayedPosts`/`pageFilteredBids`, ProblemDetailScreen-এর মতো কোনো
`currentProblem`-জাতীয় অতিরিক্ত/অপ্রাসঙ্গিক ফিল্ড কম্পোজিট করা হয়নি), আর wrapping শুধু list-অংশ
ঘিরে (ট্যাব-বার/হেডার বাইরে)। তাই ধাপ ০.১৪(খ)-এর "পুরো পেজ re-flash" প্যাটার্ন এই তিনটাতে নেই —
কোনো এডিট করা হয়নি।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)

1. `{}`/`()`/`[]` ব্যালেন্স স্ক্রিপ্ট দিয়ে (Kotlin-aware, string/comment স্কিপ করে) গোনা হয়েছে —
   মিলেছে: `{}` ৯৩৮=৯৩৮, `()` ২৭১২=২৭১২, `[]` ০=০।
2. grep দিয়ে নিশ্চিত করা হয়েছে ফাইলে আর কোনো `SyncAwareRefreshableContent(` কল নেই (শুধু কমেন্টে
   পুরনো নাম-উল্লেখ আছে), আর `bidsPulse`/`PulsingValue`/`rememberFieldChangePulse` সব জায়গায়
   সঠিকভাবে ব্যবহৃত হয়েছে।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু `ProblemDetailScreen.kt` + এই প্রোগ্রেস ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি/নেভিগেশন/বিজনেস-লজিক বদলানো হয়নি — শুধু কোন কম্পোনেন্ট কীভাবে
   loading/pulse দেখায় তার wiring বদলেছে (BidCard/WinningBidPinnedCard-এর props/callbacks
   অপরিবর্তিত)।
4a. role-branch: `ProblemDetailScreen.kt` role-agnostic (isOwner/currentUser?.role দিয়ে ভেতরে
    branch করে, কিন্তু sync/pulse wiring উভয় role-এর জন্য একই render tree — আলাদা এডিট লাগেনি)।
5. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — উপরের ফিক্স কম্পাইল/রান করে
   দেখা হয়নি। ব্যবহারকারীকে (তার নিজের পরিকল্পনা অনুযায়ী, ব্যাচ শেষে একসাথে) verify করতে হবে,
   বিশেষত:
   - প্রথমবার কোনো problem detail-এ ঢুকলে পুরো-পেজ স্কেলিটন (আগের মতোই) দেখাচ্ছে কিনা।
   - **সবচেয়ে গুরুত্বপূর্ণ:** একই problem-এ দ্বিতীয়বার (re-entry) ঢুকলে problem details অংশ
     সাথে সাথে/স্থিরভাবে দেখা যাচ্ছে কিনা (আগের মতো পুরো পেজ শিমারে আটকে না থেকে), আর শুধু bid-list
     অংশটুকু সংক্ষিপ্ত pulse করছে কিনা।
   - pull-to-refresh করলে একই রকম শুধু bid-list অংশ pulse করছে কিনা।
   - নতুন বিড জমা পড়লে/কেউ বিড accept করলে (realtime আপডেট) শুধু bid-list অংশ pulse করছে কিনা,
     বাকি পেজ অপরিবর্তিত থাকছে কিনা।

## ✅ নবম সেশন (এই সেশন) — ২.১ (Dashboard, `DashboardScreen.kt`) সম্পন্ন

মূল প্রম্পটে বলা ৩টা অংশ (মোট পোস্ট/মোট সম্পন্ন/ক্যাটাগরি-লিস্ট) নিয়ে অস্পষ্টতা ছিল — স্ক্রিনে আরও
২টা user-side stat card (চলমান কাজ, মোট ব্যয়) আর সোলভার-সাইডে ৫টা section (balance banner + ৪ card +
২ লিস্ট) একই ডেটা থেকে বদলায়, কিন্তু মূল প্রম্পটে কোনগুলো pulse করবে তা স্পষ্ট ছিল না। ধাপ ০.১৬
অনুযায়ী অনুমান না করে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছিল। **উত্তর:**
- User-role: ৪টা stat card + ক্যাটাগরি-লিস্ট — সবগুলোই pulse করবে (Wallet-এর escrow-সিদ্ধান্তের মতো)।
- Solver-role (rule ০.৫ সমতুল্য): balance banner + ৪টা stat card + recent completed jobs লিস্ট +
  recent transactions লিস্ট — সবগুলো ডাইনামিক ভ্যালু/লিস্ট pulse করবে, শুধু header/button/লেবেল
  স্থির।

**ফাইল এডিট হয়েছে:** `app/src/main/java/com/example/ui/screens/DashboardScreen.kt` (শুধু এই একটা
ফাইল)।

### রুট-কজ (গ্রেপ-ভেরিফাই, একই ক্লাসের বাগ যা Wallet/ProblemDetail-এ পাওয়া গিয়েছিল)

`DashboardScreen`-এর বাইরের wrapper (দুই role-branch-ই) `SyncAwareRefreshableContent`-এ একটা
কম্পোজিট `data` (`Triple`/`Pair`, ভেতরের কিছু ভ্যালু বাইরের স্কোপে আলাদা করে collect করে বানানো)
পাস করছিল। এর ফলে ভেতরের যেকোনো একটা ভ্যালু বদলালেই পুরো sub-composable
(`SolverDashboardContent`/`UserDashboardContent`, হেডার/লেআউট-সহ পুরোটাই) re-flash করতো — যদিও
কাঙ্ক্ষিত আচরণ শুধু নির্দিষ্ট কিছু value/list অংশ pulse করা (rule ২/৩)।

### ফিক্স

- বাইরের wrapper দুই role-branch-ই `SyncAwareRefreshableContent` থেকে `SyncAwareContent`-এ পাল্টানো
  হয়েছে (`data`/`isManualRefreshing` প্যারামিটার সরানো, `{ _ -> ... }` থেকে `{ ... }`) — এখন cold-load
  (এই app session-এ প্রথমবার)-এর জন্যই শুধু পুরো-পেজ স্কেলিটন/এরর দেখায়, একবার LOADED হয়ে গেলে নিজে
  থেকে আর কখনো re-animate করে না। `SolverDashboardContent`/`UserDashboardContent` দুটোকেই এখন নতুন
  `isRefreshing: Boolean = false` প্যারামিটার পাস করা হচ্ছে (pull-to-refresh সমাপ্তিতে pulse
  ট্রিগার করার জন্য)।
- **`UserDashboardContent`:** দুইটা আলাদা `rememberFieldChangePulse` (একটা ৪টা stat-value নিয়ে
  `Column`-এ মোড়ানো দুই `Row`-এর জন্য, আরেকটা `categoryCounts` নিয়ে ক্যাটাগরি `Card`-এর জন্য) +
  সংশ্লিষ্ট `PulsingValue` wrap। পেজ-হেডার ("আপনার পরিসংখ্যান ও সারাংশ") pulse-এর বাইরে, সবসময় স্থির।
- **`SolverDashboardContent`:** চারটা আলাদা pulse — (১) balance banner-এর ভ্যালু-টেক্সট
  (`Formatters.formatTaka(totalEarnings)`) শুধু, ব্যালেন্স-কার্ডের টাইটেল লেবেল/"সক্রিয়" ব্যাজ/
  "টাকা উইথড্র করুন" বাটন স্থির রেখে; (২) ৪টা stat card একসাথে (দুই `Row`); (৩) "সম্প্রতি সম্পন্ন
  কাজ" লিস্টের empty-state কার্ড + প্রতিটা job-কার্ড; (৪) "লেনদেন হিস্ট্রি" লিস্টের empty-state কার্ড +
  প্রতিটা transaction-কার্ড। সেকশন-হেডার ("কাজের পারফরম্যান্স পরিসংখ্যান", "সম্প্রতি সম্পন্ন কাজ",
  "লেনদেন হিস্ট্রি") আর দুটো "আরও দেখুন" বাটন pulse-এর বাইরে, সবসময় স্থির।
- Granularity সিদ্ধান্ত (নথিভুক্ত রাখা হলো, যেহেতু ব্যবহারকারীর উত্তরে স্পষ্টভাবে বলা ছিল না): "৪টা
  stat card pulse করবে" মানে পুরো কার্ড (title+icon+value+subtitle) একটা একক ইউনিট হিসেবে pulse
  করবে ধরে নেওয়া হয়েছে (Wallet-এর escrow সেকশনের "হেডার/ব্যাজ/টোটাল/লিস্ট একসাথে" সিদ্ধান্তের মতোই),
  আর "header/button/লেবেল স্থির" মানে পেজ/সেকশন-লেভেল হেডিং, বাটন, আর কার্ডের বাইরের স্ট্যান্ডঅ্যালোন
  লেবেল (যেমন ব্যালেন্স-ব্যানারের টাইটেল/ব্যাজ) বোঝানো হয়েছে — কার্ডের ভেতরের title/subtitle আলাদা
  করে static রাখা হয়নি (StatCard শেয়ার্ড কম্পোনেন্ট, ওটার নিজের সিগনেচার/ইন্টারনাল স্ট্রাকচার
  বদলানো এই সেশনের স্কোপের বাইরে ধরা হয়েছে)। যদি ব্যবহারকারী রিয়াল ডিভাইসে টেস্ট করার পর মনে করেন
  কার্ডের ভেতরে শুধু value-অংশটাই pulse করা উচিত (label/subtitle static রেখে), সেটা পরের সেশনে
  আলাদাভাবে ফিক্স করা যাবে (StatCard-এ একটা `isValuePulsing`-জাতীয় নতুন optional প্যারামিটার যোগ
  করে, শেয়ার্ড কম্পোনেন্ট বদলানো — user confirmation লাগবে, ধাপ ০.১৬)।
- অপ্রাসঙ্গিক/অব্যবহৃত হয়ে যাওয়া দুটো import (`ListScreenSkeleton`, `LoadingAwareContent` — এই
  ফাইলে ইতিমধ্যেই dead ছিল, এই সেশনের আগে থেকেই আর কোথাও কল হতো না) পরিষ্কার করা হয়েছে।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)

1. `{`/`}`/`(`/`)`/`[`/`]` ব্যালেন্স পুরো ফাইলে স্ক্রিপ্ট দিয়ে গোনা হয়েছে — মিলেছে: `{}` ১৫৯=১৫৯,
   `()` ৪৫৩=৪৫৩, `[]` ১=১।
2. grep দিয়ে নিশ্চিত করা হয়েছে ফাইলে আর কোনো real `SyncAwareRefreshableContent(` কল নেই (শুধু
   কমেন্টে পুরনো নাম-উল্লেখ আছে, ব্যাখ্যার জন্য)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু `DashboardScreen.kt` + এই প্রোগ্রেস ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি/নেভিগেশন/বিজনেস-লজিক বদলানো হয়নি — শুধু কোন অংশ কীভাবে
   loading/pulse দেখায় তার wiring বদলেছে (`StatCard`/`BidCard`-জাতীয় props/callbacks, ডেটা-হিসাব
   লজিক অপরিবর্তিত)।
4a. role-branch verify (ধাপ ০.৫): `DashboardScreen.kt` শেয়ার্ড ফাইল, `isSolver` দিয়ে দুই role-ই একই
    ফাইলে ব্রাঞ্চ করে — উপরের ফিক্স দুই role-branch-এই (আলাদা `UserDashboardContent`/
    `SolverDashboardContent` sub-composable-এ) প্রয়োগ করা হয়েছে, দুটোই grep করে আলাদাভাবে
    ভেরিফাই করা হলো।
5. zip-এর ফাইল-লিস্ট (dotfile-সহ, ২৪৭টা) মূল আপলোড করা zip-এর সাথে diff করে হুবহু মিলিয়ে দেখা
   হয়েছে — শুধু `DashboardScreen.kt` আর এই প্রোগ্রেস ফাইলই বদলেছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — উপরের ফিক্স কম্পাইল/রান করে
   দেখা হয়নি। ব্যবহারকারীকে verify করতে হবে (ব্যাচ শেষে একসাথে, তার নিজের পরিকল্পনা অনুযায়ী),
   বিশেষত:
   - প্রথমবার Dashboard-এ ঢুকলে পুরো-পেজ স্কেলিটন (আগের মতোই) দেখাচ্ছে কিনা, দুই role-ই।
   - re-entry-তে (অন্য পেজে গিয়ে ফিরে এলে) শুধু stat card/category-list (user) বা balance-value/
     stat card/দুই লিস্ট (solver) সংক্ষিপ্ত pulse করছে, বাকি পেজ (হেডার, বাটন) স্থির থাকছে কিনা।
   - pull-to-refresh করলে একই রকম আচরণ হচ্ছে কিনা।
   - উপরে উল্লেখিত granularity সিদ্ধান্ত (পুরো stat card pulse, শুধু value না) দেখতে ঠিক লাগছে
     কিনা — না লাগলে জানাতে হবে, পরের সেশনে ফিক্স করা যাবে।

## ✅ দশম সেশন (এই সেশন) — ২.২ (Instant Jobs + Instant Job History) সম্পূর্ণ

**ব্যবহারকারীর স্পষ্টীকরণ (এই সেশনের শুরুতে, ধাপ ০.১৬ অনুযায়ী প্রশ্ন করে):** মূল প্রম্পটে
`InstantJobsScreen.kt`-এর জন্য "শুধু location-সংক্রান্ত section" শিমার হবে বলা ছিল, কিন্তু এই
স্ক্রিনে literal কোনো আলাদা location-card নেই। ব্যবহারকারী নিশ্চিত করেছেন: পুরো পেজ শিমার হবে না,
শুধু নিচের "আশেপাশের লাইভ জরুরি পোস্ট" (active-bid) লিস্ট অংশ re-entry/pull-to-refresh-এ শিমার
হবে — active-job banner ও toggle card স্থির থাকবে।

### ✅ `InstantJobsScreen.kt` (সোলভার-অনলি, hardcoded `isSolver = true`, কোনো user-side সমতুল্য
নেই — ধাপ ২.২-এর নিজস্ব নোট অনুযায়ী)

**রুট-কজ (গ্রেপ-ভেরিফাই, Wallet/Dashboard/ProblemDetail-এর একই ক্লাসের বাগ):** বাইরের wrapper
`SyncAwareRefreshableContent`-এ একটা কম্পোজিট `Triple(activeJob, isToggleOn, displayNearbyJobs)`
পাস হচ্ছিল — এর যেকোনো একটা বদলালেই পুরো LazyColumn বডি (active-job banner + toggle card +
জব-লিস্ট, সব একসাথে) re-flash করতো।

**ফিক্স:**
- বাইরের wrapper `SyncAwareContent`-এ পাল্টানো হয়েছে (শুধু cold-load-এর জন্য পুরো-পেজ
  skeleton/error, LOADED হওয়ার পর নিজে থেকে আর re-animate করে না)।
- নতুন `val nearbyJobsPulse = rememberFieldChangePulse(value = displayNearbyJobs, isManualRefreshing
  = isRefreshing, sessionKey = "instant_jobs_sync", viewModel = viewModel)` — শুধু
  "section_header" আইটেম + "empty_jobs_state"/প্রতিটা `NearbyJobCard` `PulsingValue(isUpdating =
  nearbyJobsPulse) { ... }` দিয়ে মোড়ানো হয়েছে। active-job banner (`item("active_job_card")`) আর
  toggle card (`item("toggle_card")`/`"toggle_off_state"`) pulse-এর বাইরে, সবসময় স্থির —
  ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী।
- অব্যবহৃত dead import (`ListScreenSkeleton`, `LoadingAwareContent`) সরানো হয়েছে,
  `SyncAwareRefreshableContent` import-এর জায়গায় `SyncAwareContent`/`PulsingValue`/
  `rememberFieldChangePulse` যোগ হয়েছে (তিনটাই আগে থেকেই `MotionToolkit.kt`-এ বিদ্যমান)।

### ✅ `InstantJobHistoryScreen.kt` (শেয়ার্ড স্ক্রিন — `isSolver` দিয়ে user/solver দুই role-ই
ব্যবহার করে, cancelled-ট্যাবের কনটেন্ট role-ভিত্তিক ভিন্ন হলেও একই wrapper/skeleton দুই
role-branch-ই কভার করে)

**১. pull-to-refresh সম্পূর্ণ বন্ধ (মূল প্রম্পটের স্পষ্ট নির্দেশ):** বাইরের
`SomadhanPullToRefresh(isRefreshing = ..., onRefresh = { viewModel.refreshData() }) { ... }`
wrapper সম্পূর্ণ সরানো হয়েছে (import-সহ) — এখন `HorizontalPager` সরাসরি Scaffold content-এর
ভেতরে। বাকি সব আচরণ (tab-switch, initial sync, ট্যাব-কাউন্ট) অপরিবর্তিত। `isRefreshing`
ভ্যারিয়েবল রাখা হয়েছে (global viewModel state, `SyncAwareRefreshableContent`-এর
`isManualRefreshing`-এ এখনো পাস হয় consistency-র জন্য) যদিও এই স্ক্রিনে আর কোনো pull-gesture এটা
ট্রিগার করে না।

**২. শিমার-shape ফিক্স (একই সুযোগে, মূল প্রম্পটের নির্দেশ অনুযায়ী):** আগে `SyncAwareRefreshableContent`-এ
কোনো কাস্টম `skeleton` না থাকায় ডিফল্ট `ListScreenSkeleton()` (ProblemCard-শেপ, chip+title+২
description-লাইন+footer) দেখাত — যেটা history-card-এর আসল লেআউটের (chip+badge হেডার, title,
id-chip+date রো, ডিভাইডার, avatar+name+amount বটম-রো) সাথে মিলছিল না। নতুন প্রাইভেট composable
`HistoryCardSkeleton()` (এই ফাইলেই সংজ্ঞায়িত, `InstantJobHistoryCard`/
`SolverCancelledHistoryCard` দুটোরই — কাঠামো কার্যত অভিন্ন — real layout-এর সাথে মেলানো) যোগ করে
`skeleton = { LazyColumn(... contentPadding/spacing আসল লিস্টের মতোই ...) { items(4) {
HistoryCardSkeleton() } } }` পাস করা হয়েছে — দুই ট্যাবেই (cancelled-for-solver বনাম
completed/cancelled/disputed) একই shape কাজ করে যেহেতু দুই কার্ডের কাঠামো একই।
- নতুন import: `ShimmerBlock` (আগে থেকেই `MotionToolkit.kt`-এ বিদ্যমান)। অব্যবহৃত
  `ListScreenSkeleton` import সরানো হয়েছে (আর কোথাও কল হয় না)।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)

1. `{}`/`()`/`[]` ব্যালেন্স script দিয়ে (Kotlin-aware, string/comment স্কিপ করে) গোনা হয়েছে দুটো
   ফাইলেই — মিলেছে: `InstantJobsScreen.kt` `{}` ১৭১=১৭১, `()` ৫৬৬=৫৬৬, `[]` ০=০।
   `InstantJobHistoryScreen.kt` `{}` ৫০২=৫০২, `()` ১৬৬৬=১৬৬৬, `[]` ৫=৫।
2. grep দিয়ে নিশ্চিত করা হয়েছে: `InstantJobsScreen.kt`-এ আর কোনো `SyncAwareRefreshableContent(`
   কল নেই; `InstantJobHistoryScreen.kt`-এ আর কোনো `SomadhanPullToRefresh(` কল নেই (import-ও
   সরানো হয়েছে)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু `InstantJobsScreen.kt`, `InstantJobHistoryScreen.kt`,
   আর এই প্রোগ্রেস ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি/নেভিগেশন/বিজনেস-লজিক বদলানো হয়নি — শুধু কোন অংশ কীভাবে loading/pulse
   দেখায় তার wiring বদলেছে, আর pull-to-refresh gesture সরানো হয়েছে (`InstantJobHistoryScreen.kt`,
   ইচ্ছাকৃত)। `NearbyJobCard`/`InstantJobHistoryCard`/`SolverCancelledHistoryCard`-এর
   props/callbacks/ডেটা-হিসাব-লজিক অপরিবর্তিত।
4a. role-branch verify (ধাপ ০.৫): `InstantJobsScreen.kt` সোলভার-অনলি (grep-verified, কোনো
    user-branch নেই) — অতিরিক্ত কিছু লাগেনি। `InstantJobHistoryScreen.kt` শেয়ার্ড — উপরের দুটো
    ফিক্স (pull-to-refresh বন্ধ + skeleton shape) একই wrapper-এ হওয়ায় দুই role-branch-ই
    (`isSolver` true/false, দুই জায়গায় grep করে verify করা হয়েছে: লাইন ৪৪২, ৪৭৪) স্বয়ংক্রিয়ভাবে
    কভার হয়ে গেছে — আলাদা কোনো role-নির্দিষ্ট এডিট লাগেনি।
5. zip-এর ফাইল-লিস্ট (dotfile-সহ) মূল আপলোড করা zip-এর সাথে diff করে হুবহু মিলিয়ে দেখা হয়েছে —
   কোনো ফাইল miss/যোগ হয়নি, শুধু এই দুটো স্ক্রিন-ফাইল আর এই প্রোগ্রেস ফাইলই বদলেছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — উপরের ফিক্স কম্পাইল/রান করে
   দেখা হয়নি। ব্যবহারকারীকে verify করতে হবে (ব্যাচ শেষে একসাথে, তার পরিকল্পনা অনুযায়ী),
   বিশেষত:
   - জরুরি জব হাবে re-entry/pull-to-refresh-এ শুধু নিয়ারবাই-জব লিস্ট অংশ pulse করছে, active-job
     banner/toggle card স্থির থাকছে কিনা।
   - Instant Job History-তে pull-to-refresh gesture (নিচে টেনে) আর কাজ করছে না (কোনো spinner/
     refresh-প্রতিক্রিয়া নেই) কিনা, কিন্তু বাকি সব (ট্যাব-সুইচ, cold-load skeleton, ডেটা) আগের
     মতোই কাজ করছে কিনা।
   - Instant Job History-র প্রথম-ভিজিট/tab-switch skeleton এখন history-card-shaped (chip/badge/
     avatar-সহ) দেখাচ্ছে, আগের জেনেরিক ProblemCard-শেপ না — এটা দুই role/সব ট্যাবেই।

## 🔜 পরের ধাপ

২.৩, ২.৭, ২.১ (Dashboard) ও ২.২ (Instant Jobs + History) এখন সম্পূর্ণ। ধাপ ৪-এর সাজেস্টেড অর্ডার
অনুযায়ী বাকি আছে: ২.৪ (Transaction History), ২.৫ (Withdrawal pull-to-refresh যোগ), ২.৬ (Profile
pull-to-refresh বন্ধ), ২.৮ (Favorite Solvers), ২.৯ (FAQ shape) — এই ৫টা প্রায়োরিটি-স্বাধীন,
যেকোনো ক্রমে করা যাবে। সবার শেষে ২.১০ (সোলভার অ্যাকাউন্টের broad audit, আগের ফিক্স-প্যাটার্ন reuse
করে)।

**⚠️ ২.১ (Dashboard)-এর একটা খোলা প্রশ্ন পরের সেশনের জন্য নোট রাখা হলো:** stat card-গুলো এখন পুরো
কার্ড (title+value+subtitle+icon) একটা একক ইউনিট হিসেবে pulse করছে, শুধু value-অংশ আলাদা করে না —
এটা ব্যবহারকারীর real-device টেস্টে ঠিক না লাগলে (label/subtitle স্থির রেখে শুধু value pulse করা
উচিত মনে হলে) `StatCard` (শেয়ার্ড কম্পোনেন্ট, `CommonComponents.kt`) বদলাতে হতে পারে — সেটা user
confirmation-সাপেক্ষ (ধাপ ০.১৬)।

**⚠️ মনে রাখা জরুরি:** ব্যবহারকারী পুরো ব্যাচ ৩১ শেষ হলে একসাথে সব real device-এ টেস্ট করবেন
(এখনো কিছুই ভেরিফাই হয়নি) — তাই যদি টেস্টের পর কোনো সেশনের working theory ভুল প্রমাণিত হয় (বিশেষত
ProblemDetailScreen-এর re-entry বাগ, উপরে দেখুন), সেই নির্দিষ্ট সেশনে ফিরে গিয়ে নতুন করে
diagnosis/fix লাগতে পারে।

---

## সেশন — PostProblemScreen.kt (জরুরি/সাধারণ সমস্যা পোস্ট ফর্ম)

**ব্যবহারকারীর কনফার্মড স্কোপ:** `InstantJobsScreen.kt` সোলভার-অনলি হওয়ায় (ধাপ ০.৫-এর নোট
অনুযায়ী), বটম-ন্যাভে "জরুরি" ট্যাপে ইউজার আসলে যায় `PostProblemScreen` (নতুন জরুরি পোস্ট,
instant=true) বা `JobTrackingScreen`-এ। ব্যবহারকারী নিশ্চিত করেছেন এই সেশনের কাজ হলো
`PostProblemScreen`: প্রথম visit-এ ফুল-পেজ শিমার, re-entry এবং pull-to-refresh দুটোতেই শুধু
লোকেশন কার্ড pulse করবে।

### পাওয়া অবস্থা (edit-এর আগে)
- `SyncAwareContent` (sessionKey `"post_problem_sync"`) আগে থেকেই cold-load-only ফুল-পেজ
  skeleton ঠিকমতো করছিল (`rememberSessionAwareSkeletonGate` দিয়ে re-entry-তে flash হয় না) —
  এটা স্পর্শ করা হয়নি।
- পুরো ফাইলে কোনো pull-to-refresh ছিলই না (`SomadhanPullToRefresh`/`pullRefresh` কোনো grep-হিট
  ছিল না)।

### ফিক্স
- `SomadhanPullToRefresh` যোগ করা হয়েছে (Scaffold-এর paddingValues লেভেলে, `SyncAwareContent`-এর
  বাইরে মুড়িয়ে) — `isRefreshing = isLocationUpdating` (viewModel-এর বিদ্যমান StateFlow, নতুন
  কিছু বানানো হয়নি), `onRefresh = { viewModel.refreshLiveLocation() }`।
- `rememberFieldChangePulse(value = selectedLocation, isManualRefreshing = isLocationUpdating,
  sessionKey = "post_problem_sync", viewModel = viewModel)` — ডিফল্ট `flashOnReentry = true`
  ব্যবহার করা হয়েছে যাতে re-entry mount-এও একবার pulse হয় (ঠিক `UserWalletScreen.kt`-এর
  balancePulse/escrowPulse প্যাটার্ন reuse, নতুন মেকানিজম বানানো হয়নি)।
- "Read-Only Location Display" কার্ডটাকে `PulsingValue(isUpdating = locationPulse) { Card(...) }`
  দিয়ে মুড়ানো হয়েছে — এটাই একমাত্র অংশ যা re-entry/pull-to-refresh-এ pulse করে, ফর্মের বাকি সব
  (title, category grid, budget, submit বাটন ইত্যাদি) অপরিবর্তিত থাকে।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()`/`[]` ব্যালেন্স Kotlin-aware script দিয়ে গোনা হয়েছে (comment/string বাদ দিয়ে):
   `{}` ১৭৩=১৭৩, `()` ৫৩০=৫৩০, `[]` ২=২ — মিলেছে।
2. মূল আপলোড করা zip-এর `PostProblemScreen.kt`-এর সাথে সরাসরি diff করে দেখা হয়েছে — শুধু ৩টা নতুন
   import, pull-to-refresh+pulse wiring (৩টা নতুন লাইন+১টা বাড়তি ক্লোজিং ব্রেস) আর লোকেশন কার্ডের
   চারপাশে `PulsingValue` wrapper (২ লাইন) — এর বাইরে একটা অক্ষরও বদলায়নি।
3. `rememberFieldChangePulse`/`PulsingValue`/`SomadhanPullToRefresh` — তিনটাই আগে থেকে
   `MotionToolkit.kt`/`CommonComponents.kt`-এ বিদ্যমান (নতুন হেল্পার লাগেনি), import path
   `UserWalletScreen.kt`-এ যেভাবে ব্যবহার হয়েছে হুবহু সেভাবেই।
4. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি — লোকেশন
   অটো-ডিটেক্ট/পারমিশন-ফ্লো, ফর্ম ভ্যালিডেশন, submit লজিক সব অপরিবর্তিত।
4a. `PostProblemScreen.kt` user/solver উভয়ের জন্যই একই ফাইল/ফ্লো (role অনুযায়ী শাখা নেই এই
    ফর্মে, শুধু `effectiveInstantMode` অনুযায়ী রঙ/টেক্সট বদলায়) — তাই আলাদা role-branch verify
    প্রযোজ্য না।
5. পুরো প্রজেক্টের zip আগের আপলোড করা zip-এর সাথে ফাইল-লিস্ট diff করে মিলিয়ে দেখা হয়েছে (২৪৭টি
   ফাইলই অক্ষত, শুধু `PostProblemScreen.kt` আর এই progress ফাইল বদলেছে)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না — এখনো compile/run করে verify হয়নি।**
   ব্যবহারকারীকে বিশেষভাবে দেখতে হবে:
   - প্রথমবার "জরুরি" পোস্ট ফর্মে ঢুকলে পুরো ফর্ম ফুল-পেজ skeleton দেখাচ্ছে কিনা (অপরিবর্তিত থাকার
     কথা)।
   - এই ফর্ম থেকে বেরিয়ে আবার ঢুকলে (re-entry) পুরো ফর্ম আর re-skeleton না হয়ে শুধু লোকেশন কার্ডটা
     এক ঝলক pulse করছে কিনা, বাকি ফর্ম (টাইটেল/ক্যাটাগরি/বাজেট) স্থির থাকছে কিনা।
   - ফর্মে নিচে টেনে (pull-to-refresh) ছাড়লে লোকেশন রিফ্রেশ হচ্ছে ও শুধু লোকেশন কার্ডটাই pulse
     করছে কিনা, আর "অটো ডিটেক্ট" বাটনের ম্যানুয়াল লোকেশন-ফেচ ফ্লো আগের মতোই কাজ করছে কিনা
     (দুটো আলাদা মেকানিজম, একে অপরকে প্রভাবিত করার কথা না)।

## 🔜 পরের ধাপ
বাকি আছে (ধাপ ৪-এর সাজেস্টেড অর্ডার অনুযায়ী, প্রায়োরিটি-স্বাধীন): ২.৪ (Transaction History), ২.৫
(Withdrawal pull-to-refresh যোগ), ২.৬ (Profile pull-to-refresh বন্ধ), ২.৮ (Favorite Solvers), ২.৯
(FAQ shape) — সবার শেষে ২.১০ (সোলভার অ্যাকাউন্টের broad audit)।

---

## সেশন — ২.৪ (Transaction History, `TransactionHistoryScreen.kt`)

**ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছিল (ধাপ ০.১৬, প্রকৃত অস্পষ্টতা):** উপরের "মোট লেনদেন / ফিল্টার
ফলাফল" সামারি কার্ডটা লিস্টের সাথে pulse করবে, নাকি header/tab-bar-এর মতো স্থির থাকবে — যেহেতু
"ফিল্টার ফলাফল" সংখ্যা লিস্টের সাথেই বদলায়। **উত্তর: লিস্টের সাথে pulse করবে।**

### রুট-কজ (গ্রেপ-ভেরিফাই, একই ক্লাসের বাগ যা Wallet/ProblemDetail/Dashboard-এ পাওয়া গিয়েছিল)
বাইরের wrapper `SyncAwareRefreshableContent`-এ একটা কম্পোজিট `data = Pair(filteredTransactions,
sortedGatewayPayments)` পাস করছিল, যেটার ভেতরে পুরো `LazyColumn`-ই (summary stats card + sticky
tab/filter header + transaction/gateway সব রো) একসাথে মোড়ানো ছিল। ফলে ট্যাব পাল্টালে
(`selectedSubTab` বদলালে `filteredTransactions` remember-key-তে থাকায় রেফারেন্স বদলে যেত) বা
pull-to-refresh/re-entry-তে পুরো এই ব্লকটাই (sticky header-সহ) একসাথে re-flash করতো।

### ফিক্স
- বাইরের wrapper `SyncAwareRefreshableContent` থেকে `SyncAwareContent`-এ পাল্টানো হয়েছে (`data`/
  `isManualRefreshing` সরানো, `{ _ -> ... }` থেকে `{ ... }`) — এখন cold-load-only পুরো-পেজ
  skeleton/error দেখায়, একবার LOADED হয়ে গেলে নিজে থেকে আর কখনো re-animate করে না। sticky
  tab/filter header (APP/GATEWAY toggle, সব/পেমেন্ট/রিচার্জ/রিফান্ড চিপ, ভেতরের count-সহ)
  এখন সবসময় স্থির।
- একটাই `transactionListPulse = rememberFieldChangePulse(value = Triple(selectedMainTab,
  selectedSubTab, Pair(filteredTransactions, sortedGatewayPayments)), isManualRefreshing =
  isRefreshing, sessionKey = "transaction_history_sync", viewModel = viewModel)` — ডিফল্ট
  `flashOnReentry = true` দিয়ে re-entry-ও কভার হয়, আর `selectedMainTab`/`selectedSubTab`
  pulse-key-এর অংশ বলে ট্যাব পাল্টালেও (ডেটা সত্যিই বদলেছে কিনা না দেখেই) সংক্ষিপ্ত pulse হয়।
- `PulsingValue(isUpdating = transactionListPulse) { ... }` দিয়ে ৪টা জায়গা আলাদাভাবে মোড়ানো
  হয়েছে (ProblemDetailScreen-এর `bidsPulse` per-item-wrap প্যাটার্ন অনুসরণ করে, `items()`
  কল নিজেই মোড়ানো হয়নি, শুধু ভেতরের ভিজ্যুয়াল Card/Column): (১) Summary Stats Card (ব্যবহারকারীর
  কনফার্মড সিদ্ধান্ত অনুযায়ী), (২) GATEWAY ট্যাবের empty-state কার্ড, (৩)
  `items(sortedGatewayPayments)`-এর প্রতিটা gateway-কার্ড, (৪) APP ট্যাবের filteredTransactions
  empty-state কার্ড, (৫) `items(filteredTransactions)`-এর প্রতিটা transaction-কার্ড (val
  computation, যেমন brandColor/purposeText/escrow lookup ইত্যাদি, PulsingValue-এর বাইরে রাখা
  হয়েছে, শুধু চূড়ান্ত ভিজ্যুয়াল Card মোড়ানো হয়েছে)। Infinite-scroll লোড-মোর স্পিনার ইচ্ছাকৃতভাবে
  pulse-এর বাইরে রাখা হয়েছে (স্কোপ-বহির্ভূত, ছোট স্পিনার এমনিতেই নিজস্ব loading state দেখায়)।
- অব্যবহৃত হয়ে যাওয়া `SyncAwareRefreshableContent` import সরানো হয়েছে, `PulsingValue`/
  `rememberFieldChangePulse` import যোগ হয়েছে।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()`/`[]` ব্যালেন্স Kotlin-aware script দিয়ে গোনা হয়েছে (comment/string বাদ দিয়ে):
   `{}` ২৫৫=২৫৫, `()` ৮৯১=৮৯১, `[]` ০=০ — মিলেছে।
2. grep দিয়ে নিশ্চিত করা হয়েছে ফাইলে আর কোনো `SyncAwareRefreshableContent(` কল নেই।
3. মূল আপলোড করা zip-এর `TransactionHistoryScreen.kt`-এর সাথে সরাসরি diff করে দেখা হয়েছে — শুধু
   ২টা import বদল, wrapper-এর রি-রাইট (comment+pulse ডিক্লেয়ারেশন), আর ৫ জায়গায়
   `PulsingValue(isUpdating = transactionListPulse) { ... }` wrap (open+close জোড়া, ভেতরের
   বিদ্যমান কোনো Card/Text/লজিক একচুলও বদলায়নি)।
4. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি — ফিল্টার-লজিক
   (isDepositTrx/isRefundTrx/isPaymentTrx), pagination (`loadNextTransactionsPage`), ক্লিপবোর্ড
   কপি, gateway receipt ডায়ালগ সব অপরিবর্তিত।
4a. role-branch verify (ধাপ ০.৫): `TransactionHistoryScreen.kt` শেয়ার্ড ফাইল (`isSolver` লাইন
    ১২১/১৫৭/৩১৩/৭৪৯-এ ব্যবহৃত) — কিন্তু `transactionListPulse`/`SyncAwareContent`/৫টা
    `PulsingValue` wrap কোনোটাই `isSolver`-নির্দিষ্ট শাখার ভেতরে না, তাই দুই role-ই একই ওয়্যারিং
    দিয়ে স্বয়ংক্রিয়ভাবে কভার হয়ে গেছে — আলাদা কোনো role-নির্দিষ্ট এডিট লাগেনি (grep করে দুই
    isSolver ব্রাঞ্চই দেখে নিশ্চিত করা হয়েছে যে filteredTransactions/sortedGatewayPayments এবং
    উপরের সব wrap উভয় role-এর জন্যই প্রযোজ্য)।
5. পুরো প্রজেক্ট zip-এর ফাইল-লিস্ট (dotfile-সহ, ২৪৮টা) মূল আপলোড করা zip-এর সাথে diff করে হুবহু
   মিলিয়ে দেখা হয়েছে — শুধু `TransactionHistoryScreen.kt`, `PostProblemScreen.kt` (আগের সেশন)
   আর এই progress ফাইলই বদলেছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — compile/run করে verify হয়নি।
   ব্যবহারকারীকে বিশেষভাবে দেখতে হবে:
   - প্রথমবার এই পেজে ঢুকলে পুরো-পেজ skeleton (আগের মতোই) দেখাচ্ছে কিনা।
   - re-entry-তে সামারি কার্ড + লিস্ট (দুটোই) সংক্ষিপ্ত pulse করছে, কিন্তু sticky tab/filter
     header (APP/GATEWAY toggle, filter chip, ভেতরের count-সহ) সম্পূর্ণ স্থির থাকছে কিনা।
   - APP/GATEWAY main-tab পাল্টালে বা সব/পেমেন্ট/রিচার্জ/রিফান্ড sub-tab পাল্টালে একই রকম
     (শুধু সামারি+লিস্ট pulse, header স্থির) আচরণ হচ্ছে কিনা।
   - pull-to-refresh করলে একই রকম আচরণ হচ্ছে কিনা, আর infinite-scroll (নিচে স্ক্রল করলে পরের
     পেজ লোড হওয়া) আগের মতোই কাজ করছে কিনা।

## সেশন — ২.৫ (Withdrawal, `UserWithdrawScreen.kt` / `SolverBalanceWithdrawScreen.kt` /
`WithdrawalHistoryScreen.kt`)

মূল প্রম্পট অনুযায়ী: এই তিনটা স্ক্রিনের বাকি সব আচরণ ঠিক আছে — শুধু pull-to-refresh মেকানিজম
যোগ/ঠিক করতে হবে, যেটাতে re-entry-এর মতোই শুধু উইথড্রয়াল-ইতিহাস লিস্ট শিমার হবে, বাকি কাঠামো
অপরিবর্তিত।

### `UserWithdrawScreen.kt` ও `SolverBalanceWithdrawScreen.kt` (গ্রেপ-ভেরিফাই, দুটো আলাদা
সোলভার/ইউজার ফাইল, ধাপ ০.৫-এর নিজস্ব শ্রেণিবিভাগ অনুযায়ী — শেয়ার্ড ফাইল না)

**পাওয়া অবস্থা:** দুটোতেই `SyncAwareContent` (cold-load-only) আগে থেকেই সঠিক ছিল, কিন্তু কোনো
pull-to-refresh ছিলই না (`SomadhanPullToRefresh`/`pullRefresh` কোনো grep-হিট ছিল না)।

**ফিক্স (দুটো ফাইলেই অভিন্ন প্যাটার্নে, শুধু sessionKey/accentColor ভিন্ন):**
- `SomadhanPullToRefresh(isRefreshing = viewModel.isRefreshing, onRefresh = {
  viewModel.refreshData() })` যোগ করা হয়েছে (Scaffold-এর paddingValues লেভেলে, বিদ্যমান
  `SyncAwareContent`-এর বাইরে মুড়িয়ে — `PostProblemScreen.kt`-এর প্যাটার্ন অনুসরণ করে)।
- `rememberFieldChangePulse(value = withdrawals, isManualRefreshing = isRefreshing, sessionKey
  = <ঐ স্ক্রিনের বিদ্যমান sessionKey>, viewModel = viewModel)` — ডিফল্ট `flashOnReentry = true`
  দিয়ে re-entry-ও কভার হয়।
- "উইথড্রয়াল ইতিহাস" সেকশন — টাইটেল-রো (কাউন্ট-সহ) + see-more বাটন + empty-state/২টা প্রিভিউ কার্ড,
  পুরোটা একসাথে `PulsingValue(isUpdating = withdrawalHistoryPulse) { Column { ... } }` দিয়ে
  মোড়ানো হয়েছে (Wallet-এর escrow-সেকশনের সিদ্ধান্তের মতোই — টাইটেলের কাউন্ট ডেটার সাথেই বদলায় বলে
  পুরো সেকশন একসাথে)। ফর্মের বাকি সব অংশ (balance card, withdraw form, submit বাটন) pulse-এর বাইরে,
  সবসময় স্থির।

### `WithdrawalHistoryScreen.kt` (শেয়ার্ড স্ক্রিন — `isSolver`/`currentUser.role` দিয়ে accentColor
ছাড়া বাকি রেন্ডারিং একই)

**রুট-কজ (গ্রেপ-ভেরিফাই, Wallet/ProblemDetail/Dashboard/TransactionHistory-তে পাওয়া একই ক্লাসের
বাগ):** এই স্ক্রিনে আগে থেকেই pull-to-refresh ছিল (আগের রোডম্যাপ ধাপে যোগ হয়েছিল), কিন্তু
`SyncAwareRefreshableContent`-এ কম্পোজিট `data = withdrawalsSnapshot` পুরো `LazyColumn`-কেই
(Summary Stats Card + লিস্ট, সবটা) মুড়ে রেখেছিল — `withdrawalsPaged` বদলালেই পুরো ব্লক একসাথে
re-flash করতো।

**ফিক্স:** বাইরের wrapper `SyncAwareRefreshableContent` থেকে `SyncAwareContent`-এ পাল্টানো হয়েছে
(cold-load-only)। নতুন `withdrawalListPulse = rememberFieldChangePulse(value = withdrawalsList,
isManualRefreshing = isRefreshing, sessionKey = "withdrawal_history_sync", viewModel = viewModel)`
— Summary Stats Card (TransactionHistoryScreen-এর একই সিদ্ধান্ত, কারণ "মোট লোড হয়েছে" সংখ্যাও
ডেটার সাথেই বদলায়), empty-state কার্ড, আর প্রতিটা history-item কার্ড — সবগুলো আলাদাভাবে
`PulsingValue(isUpdating = withdrawalListPulse) { ... }` দিয়ে মোড়ানো হয়েছে। Infinite-scroll
footer pulse-এর বাইরে রাখা হয়েছে (নিজস্ব স্পিনার থাকে)। অব্যবহৃত `ListScreenSkeleton` import
সরানো হয়েছে (গ্রেপ করে নিশ্চিত করা হলো এই ফাইলে আর কোথাও ব্যবহৃত হতো না)।

### যাচাই করা হয়েছে (ধাপ ৩ চেকলিস্ট)

1. `{}`/`()`/`[]` ব্যালেন্স Kotlin-aware script দিয়ে (comment/string বাদ দিয়ে) তিনটা ফাইলেই
   গোনা হয়েছে — মিলেছে: `UserWithdrawScreen.kt` `{}` ৮৩=৮৩, `()` ৩০৩=৩০৩। `SolverBalanceWithdrawScreen.kt`
   `{}` ৮০=৮০, `()` ২৭১=২৭১। `WithdrawalHistoryScreen.kt` `{}` ৪৫=৪৫, `()` ১২৭=১২৭।
2. grep দিয়ে নিশ্চিত করা হয়েছে কোনো ফাইলেই আর `SyncAwareRefreshableContent(` কল নেই, আর
   `PulsingValue`/`rememberFieldChangePulse`/`SomadhanPullToRefresh` তিনটাই আগে থেকেই
   `MotionToolkit.kt`/`CommonComponents.kt`-এ বিদ্যমান (নতুন হেল্পার লাগেনি)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু এই তিনটা স্ক্রিন-ফাইল + এই progress ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি — উইথড্র-ফর্ম
   ভ্যালিডেশন/submit-লজিক, pagination (`loadNextWithdrawalsPage`), ক্লিপবোর্ড কপি,
   `WithdrawalDetailBottomSheet` সব অপরিবর্তিত।
4a. role-branch verify (ধাপ ০.৫): `UserWithdrawScreen.kt`/`SolverBalanceWithdrawScreen.kt`
    সত্যিকারের আলাদা সোলভার/ইউজার ফাইল (কোনো `isSolver`-branch নেই এদের ভেতরে) — দুটোতেই আলাদাভাবে
    একই ফিক্স প্রয়োগ করা হয়েছে। `WithdrawalHistoryScreen.kt` শেয়ার্ড — উপরের সব ফিক্স
    `isSolver`-নির্দিষ্ট কোনো শাখার ভেতরে না (শুধু `accentColor` ভিন্ন, যেটা অপরিবর্তিত), তাই দুই
    role-ই স্বয়ংক্রিয়ভাবে কভার হয়ে গেছে — আলাদা কোনো role-নির্দিষ্ট এডিট লাগেনি।
5. পুরো প্রজেক্টের zip-এর ফাইল-লিস্ট (dotfile-সহ, ২৪৭টা) মূল আপলোড করা zip-এর সাথে diff করে হুবহু
   মিলিয়ে দেখা হয়েছে — শুধু এই তিনটা স্ক্রিন-ফাইল আর এই progress ফাইলই বদলেছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — compile/run করে verify হয়নি।
   ব্যবহারকারীকে ব্যাচ শেষে (তার পরিকল্পনা অনুযায়ী) বিশেষভাবে দেখতে হবে:
   - User Withdraw ও Solver Balance/Withdraw পেজে এখন নিচে টেনে (pull-to-refresh) রিফ্রেশ করলে
     শুধু "উইথড্রয়াল ইতিহাস" সেকশন সংক্ষিপ্ত pulse করছে, balance card/ফর্ম স্থির থাকছে কিনা।
   - এই দুই পেজে re-entry করলে (অন্য পেজে গিয়ে আবার ফিরে এলে) একই রকম শুধু ইতিহাস সেকশন pulse
     করছে কিনা।
   - Withdrawal History (আরও দেখুন) পেজে এখন pull-to-refresh/re-entry-তে Summary Stats Card +
     লিস্ট (দুটোই) সংক্ষিপ্ত pulse করছে, আর পুরো পেজ আর re-skeleton হচ্ছে না কিনা, আর
     infinite-scroll (নিচে স্ক্রল করলে পরের পেজ লোড) আগের মতোই কাজ করছে কিনা।

## 🔜 পরের ধাপ
বাকি আছে (প্রায়োরিটি-স্বাধীন): ২.৬ (Profile pull-to-refresh বন্ধ), ২.৮ (Favorite Solvers), ২.৯
(FAQ shape) — সবার শেষে ২.১০ (সোলভার অ্যাকাউন্টের broad audit)।

---

## সেশন — ২.৬ (Profile pull-to-refresh বন্ধ), ২.৮ (Favorite Solvers), ২.৯ (FAQ shape) — সম্পূর্ণ

### ✅ `ProfileScreen.kt` — ২.৬

মূল প্রম্পটের নির্দেশ অনুযায়ী শুধু pull-to-refresh সরানো হয়েছে (cold-load/re-entry আচরণ ব্যবহারকারী
আগেই "ঠিক আছে" বলে কনফার্ম করেছিলেন বলে সেটা ছোঁয়া হয়নি)।

- বাইরের `SomadhanPullToRefresh(isRefreshing=..., onRefresh={ viewModel.refreshData() }) { ... }`
  wrapper সম্পূর্ণ সরানো হয়েছে (import-সহ) — তার জায়গায় একটা প্লেইন `run { ... }` ব্লক (ব্রেস-গঠন
  অপরিবর্তিত রাখতে, নিচের `SyncAwareRefreshableContent`/`Column` কাঠামো একবিন্দুও না ছুঁয়ে) এবং
  তার `modifier` (fillMaxSize/padding(paddingValues)/background(SomadhanBg)) সরাসরি ভেতরের
  `SyncAwareRefreshableContent`-এর নিজস্ব `modifier` প্যারামিটারে সরানো হয়েছে।
- `isManualRefreshing = isRefreshing` অপরিবর্তিত রাখা হয়েছে (global viewModel state, consistency-র
  জন্য — `InstantJobHistoryScreen.kt`-এ আগের সেশনে করা একই সিদ্ধান্তের প্যাটার্ন অনুসরণ করে) —
  এই স্ক্রিনে আর কোনো pull-gesture এটা ট্রিগার করে না, কিন্তু ভ্যারিয়েবল/ওয়্যারিং বদলানো স্কোপের
  বাইরে ছিল।
- ডাটা-লোড লজিক (`data = listOf(currentUser, ...)`, `sessionKey`, `onRetry`) একবিন্দুও বদলানো হয়নি।

**যাচাই:** `{}` ১৭৪=১৭৪, `()` ৫৬২=৫৬২ (Kotlin-aware script)। grep দিয়ে নিশ্চিত করা হয়েছে
`SomadhanPullToRefresh` আর কোথাও কল হয় না (শুধু নতুন কমেন্টে ব্যাখ্যা হিসেবে নাম আছে)। role-branch:
`ProfileScreen.kt` শেয়ার্ড ফাইল, কিন্তু pull-to-refresh wrapper role-নির্দিষ্ট কোনো শাখার ভেতরে
ছিল না বলে সরানোর ফলে দুই role-ই স্বয়ংক্রিয়ভাবে কভার হয়ে গেছে।

### ✅ `FavoriteSolversScreen.kt` — ২.৮

**পাওয়া অবস্থা (গ্রেপ-ভেরিফাই):** মূল প্রম্পটে "pull-to-refresh সম্পূর্ণ নতুন যোগ করতে হবে" বলা
থাকলেও, এই zip-এ গিয়ে দেখা গেছে pull-to-refresh (`SomadhanPullToRefresh`) আর re-entry/refresh-এ
শুধু তালিকা-অংশ শিমার করা (`SyncAwareRefreshableContent(data = favoriteItems, ...)`, শুধু
list-এর চারপাশে মোড়ানো, header/AppBar বাইরে) — দুটোই **ইতিমধ্যে সঠিকভাবে বাস্তবায়িত ছিল** (সম্ভবত
কোনো আগের অলিখিত/broad-audit সেশনে, বা প্রথম থেকেই সঠিকভাবে লেখা — এই সেশনের প্রোগ্রেস ফাইলে এর
আগে কোনো এন্ট্রি ছিল না)। তাই সেই অংশে নতুন কোনো এডিট লাগেনি।

**যা নতুন করে ফিক্স হয়েছে (ধাপ ০.১৫ অনুযায়ী, এই ফাইল স্পর্শ করার সুযোগে):** `SyncAwareRefreshableContent`-এ
কোনো কাস্টম `skeleton` না থাকায় ডিফল্ট `ListScreenSkeleton()` (ProblemCard-শেপ) দেখাত, যেটা আসল
`FavoriteSolverCard`-এর (avatar circle + name/badge/rating + heart-আইকন, তারপর "সম্পন্ন কাজ" ব্যাজ-রো,
তারপর দুই-বাটনের রো) লেআউটের সাথে মিলছিল না। নতুন প্রাইভেট `FavoriteSolverCardSkeleton()` composable
(এই ফাইলেই, `ShimmerBlock` বিল্ডিং-ব্লক দিয়ে, real card-এর চারটা সেকশন হুবহু অনুসরণ করে) যোগ করে
`skeleton = { LazyColumn(...) { items(4) { FavoriteSolverCardSkeleton() } } }` পাস করা হয়েছে।
সুযোগে অব্যবহৃত dead import (`ListScreenSkeleton`, `LoadingAwareContent` — কোনোটাই এই ফাইলে আর
কোথাও কল হতো না) পরিষ্কার করা হয়েছে, `ShimmerBlock` import যোগ হয়েছে।

**যাচাই:** `{}` ৮৩=৮৩, `()` ২৭৫=২৭৫। role: মূল প্রম্পটে ও আগের সেশনে ব্যবহারকারী কনফার্ম করেছিলেন
এই স্ক্রিন সম্পূর্ণ user-side-only, সোলভার সমতুল্য নেই — তাই আলাদা কিছু লাগেনি।

### ✅ `FaqScreen.kt` — ২.৯

**রুট-কজ (গ্রেপ-ভেরিফাই):** `SyncAwareRefreshableContent`-এ `data = faqs` (সরাসরি স্ক্রিনের নিজস্ব
ডেটা, কোনো composite/unrelated ফিল্ড না — এই স্ক্রিনে সেই ক্লাসের বাগ ছিল না) ঠিকই ছিল, কিন্তু কোনো
কাস্টম `skeleton` না থাকায় ডিফল্ট `ListScreenSkeleton()` (ProblemCard-শেপ: chip+title+২ লাইন+ফুটার)
দেখাত — আসল accordion FAQ কার্ডের (নম্বর-ব্যাজ সার্কেল + প্রশ্ন-টেক্সট + expand-আইকন, একটামাত্র রো)
লেআউটের সাথে মিলছিল না।

**ফিক্স:** নতুন প্রাইভেট `FaqCardSkeleton()` composable (`ShimmerBlock` দিয়ে, real accordion-item-এর
collapsed-state রো হুবহু অনুসরণ করে: circle badge + text-line (weight ১f) + icon-spot) যোগ করে
`skeleton = { LazyColumn(...) { item { <count-bar shimmer> }; items(5) { FaqCardSkeleton() } } }`
পাস করা হয়েছে। `ShimmerBlock` import যোগ হয়েছে।

**যাচাই:** `{}` ৭০=৭০, `()` ২৯৯=২৯৯, `[]` ১=১। role-branch: `FaqScreen.kt` শেয়ার্ড ফাইল
(`userRole` দিয়ে শুরুর ট্যাব ভিন্ন) — skeleton/data wiring role-নির্দিষ্ট কোনো শাখার ভেতরে না, তাই
দুই role-ই স্বয়ংক্রিয়ভাবে কভার হয়ে গেছে।

### সাধারণ যাচাই (ধাপ ৩ চেকলিস্ট, তিনটা ফাইলের জন্যই)

1. তিনটা ফাইলেই `{}`/`()`/`[]` ব্যালেন্স Kotlin-aware স্ক্রিপ্ট দিয়ে (comment/string বাদ দিয়ে)
   গোনা হয়েছে — উপরে প্রতিটা সেকশনে উল্লেখিত সংখ্যা মিলেছে।
2. grep দিয়ে নিশ্চিত করা হয়েছে পুরনো broken প্যাটার্ন কোথাও নেই, আর নতুন ব্যবহৃত
   `ShimmerBlock`/`SomadhanPullToRefresh` (যেখানে প্রযোজ্য) আগে থেকেই
   `MotionToolkit.kt`/`CommonComponents.kt`-এ বিদ্যমান — নতুন হেল্পার বানাতে হয়নি।
3. এই সেশনে অন্য কোনো ফাইল ছোঁয়া হয়নি (শুধু `ProfileScreen.kt`, `FavoriteSolversScreen.kt`,
   `FaqScreen.kt`, আর এই progress ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি — শুধু
   loading/pull-to-refresh/skeleton-shape wiring বদলেছে।
4a. role-branch verify: প্রতিটা ফাইলের নিজস্ব সেকশনে উপরে আলাদাভাবে লেখা হয়েছে — কোথাও নতুন
    role-নির্দিষ্ট এডিট লাগেনি।
5. পুরো প্রজেক্টের ফাইল-সংখ্যা (dotfile-সহ, ২৪৭টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে —
   কোনো ফাইল miss/যোগ হয়নি, শুধু এই তিনটা স্ক্রিন-ফাইল আর এই progress ফাইলই বদলেছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — কম্পাইল/রান করে দেখা হয়নি।
   ব্যবহারকারীকে ব্যাচ শেষে (তার পরিকল্পনা অনুযায়ী) বিশেষভাবে দেখতে হবে:
   - Profile পেজে এখন নিচে টেনে আর কোনো রিফ্রেশ-প্রতিক্রিয়া হচ্ছে না কিনা (বাকি সব আগের মতোই)।
   - পছন্দের সমাধানকারী পেজে re-entry/pull-to-refresh-এ এখন কার্ড-শেপড শিমার দেখাচ্ছে কিনা (আগের
     জেনেরিক ProblemCard-শেপ না)।
   - FAQ পেজে (দুই ট্যাব, দুই role) cold-load/refresh-এ এখন accordion-শেপড শিমার (নম্বর-ব্যাজ +
     প্রশ্ন-লাইন + আইকন) দেখাচ্ছে কিনা।

## 🔜 পরের ধাপ

ধাপ ৪-এর সাজেস্টেড অর্ডার অনুযায়ী এখন শুধু ২.১০ (সোলভার অ্যাকাউন্টের broad audit — SolverKycScreen,
SolverSkillsScreen, SolverCategoryPostsScreen, SolverCompletedJobsScreen, SolverReviewsScreen-এ
একই "phase কখনো LOADED-এ ফেরে না"/কম্পোজিট-`data` প্যাটার্ন আছে কিনা grep দিয়ে যাচাই ও ফিক্স) বাকি
আছে — ধাপ ২-এর তালিকার সব নামকরা স্ক্রিন-সেশন এখন সম্পূর্ণ।

---

## সেশন — ২.১০ (সোলভার অ্যাকাউন্টের broad audit) — সম্পূর্ণ, কোনো নতুন কোড-বাগ পাওয়া যায়নি

মূল প্রম্পটের নির্দেশ অনুযায়ী পাঁচটা স্ক্রিনেই grep করে "phase কখনো LOADED-এ ফেরে না" (ধাপ ০.১৪(ক),
স্টাক-শিমার) প্যাটার্ন খোঁজা হলো — **কোনো ফাইলেই এডিট করা হয়নি**, শুধু diagnosis/audit।

### যা চেক করা হয়েছে (পাঁচটা ফাইলেই)

1. **`SyncAwareRefreshableContent`-এর পুরনো `flashRequestCount`/`rememberFieldChangePulse`-এর
   `activePulseReasons` কাউন্টার-বাগ (পঞ্চম/ষষ্ঠ সেশনে `MotionToolkit.kt`-এ `finally`-ফিক্স হয়ে
   গেছে)** — grep করে নিশ্চিত করা হলো কোনো ফাইলেই এই মেকানিজমের **কোনো লোকাল রি-ইমপ্লিমেন্টেশন**
   নেই (সবাই শেয়ার্ড কম্পোনেন্ট ব্যবহার করে) — তাই এই ফিক্স স্বয়ংক্রিয়ভাবে সবগুলোতেই প্রযোজ্য
   হয়ে গেছে, আলাদা কিছু করার দরকার নেই।
2. **কম্পোজিট/unrelated-field `data` প্যারামিটার (Wallet/Dashboard/ProblemDetail-এ পাওয়া ধাপ
   ০.১৪(খ)-এর প্যাটার্ন)** — পাঁচটা ফাইলের `data`/`syncPhase` wiring আলাদাভাবে গ্রেপ করে দেখা হলো:
   - `SolverKycScreen.kt`, `SolverSkillsScreen.kt` — plain `SyncAwareContent` (`data` প্যারামিটারই
     নেই, শুধু cold-load), তাই এই ক্লাসের বাগের ঝুঁকিই নেই।
   - `SolverCategoryPostsScreen.kt` — `data = displayedProblems` (স্ক্রিনের নিজস্ব পোস্ট-লিস্ট,
     unrelated field মেশানো নেই)।
   - `SolverCompletedJobsScreen.kt` — `data = completedJobs` (নিজস্ব লিস্ট, একই রকম ক্লিন)।
   - `SolverReviewsScreen.kt` — `data = reviewsSnapshot` (`.toList()` স্ন্যাপশট, নিজস্ব লিস্ট
     ছাড়া আর কিছু নেই); `syncPhase = worstSyncPhase(initialSyncPhase, ratingsSyncPhase)` —
     আগের (দ্বিতীয় সেশনের) ডায়াগনসিসে নিশ্চিত হওয়া গিয়েছিল `ratingsSyncPhase` কখনো LOADING/ERROR-এ
     যায় না (স্টাব, সবসময় LOADED), তাই এটা নিরাপদ — নতুন কোনো রিস্ক না।
3. **ম্যানুয়াল লোকাল লোডিং-ফ্ল্যাগ (ProblemDetailScreen-এর পুরনো `isCheckingProblem`-জাতীয়
   কোনো ভাঙা গেট)** — grep করে পাঁচটা ফাইলে খোঁজা হলো: শুধু `SolverKycScreen.kt`-এ `isUploading`
   পাওয়া গেছে (সেলফি/ডকুমেন্ট আপলোড বাটনের স্পিনার, `SyncAwareContent`-এর সাথে সম্পর্কহীন) —
   success/failure সব ব্রাঞ্চে explicit `isUploading = false` আছে (৬টা জায়গায়), কোনো
   `LaunchedEffect(data)`-মিড-ডিলে cancellation-ঝুঁকি নেই (এটা button-click handler, coroutine
   cancellation-প্রবণ কোনো key-based effect না) — নিরাপদ।

### উপসংহার

পাঁচটা স্ক্রিনের কোনোটাতেই নতুন কোনো deterministic কোড-বাগ পাওয়া যায়নি — আগের সেশনগুলোর শেয়ার্ড-কম্পোনেন্ট
ফিক্স (`finally` ব্লক) ইতিমধ্যেই এই স্ক্রিনগুলো কভার করে ফেলেছে, আর কোনোটাতেই composite-data বা
ভাঙা ম্যানুয়াল লোডিং-গেট নেই। তাই এই সেশনে **কোনো ফাইল এডিট করা হয়নি**।

**সাইড-নোট (এই সেশনের স্কোপের বাইরে, কোনো এডিট হয়নি):** `SolverCompletedJobsScreen.kt`-এর
job-কার্ডের skeleton (ডিফল্ট `ListScreenSkeleton`) real কার্ডের ফুটার-লেআউটের (দুই কলাম: বাম দিকে
ক্লায়েন্ট/ঠিকানা ২ লাইন, ডান দিকে আয়/তারিখ ২-৩ লাইন) সাথে পুরোপুরি না-ও মিলতে পারে (ধাপ ০.১৫-এর
আওতায়, কিন্তু এই সেশনের নাম-করা স্কোপ শুধু "stuck shimmer" audit ছিল, ব্যবহারকারীর confirmation
ছাড়া ধাপ ০.১৬ অনুযায়ী স্পর্শ করা হয়নি)। ভবিষ্যতে দরকার মনে হলে আলাদা ছোট সেশনে ফিক্স করা যাবে।

### যাচাই

- এই সেশনে কোনো কোড এডিট হয়নি বলে brace-balance/zip-file-count ভেরিফিকেশনের দরকার নেই
  (diagnosis-only, আগের ১ম/২য়/৩য় সেশনের মতোই)।
- zip-এর ভেতরে শুধু এই আপডেটেড progress ফাইলটাই বদলেছে।

## ✅ ব্যাচ ৩১ সম্পূর্ণ

ধাপ ৪-এর সাজেস্টেড অর্ডারের সব সেশন (১, ২.১১, ২.৩, ২.৭, ২.১, ২.২, PostProblemScreen, ২.৪, ২.৫,
২.৬, ২.৮, ২.৯, ২.১০) এখন সম্পূর্ণ। **⚠️ কোনো এডিটই এখনো real Android Studio/Gradle build-এ
verify হয়নি** — ব্যবহারকারী তার পরিকল্পনা অনুযায়ী পুরো ব্যাচ শেষে একসাথে real device-এ সব টেস্ট
করবেন। টেস্টের পর কোনো session-এর working theory ভুল প্রমাণিত হলে (বিশেষত ProblemDetailScreen-এর
re-entry বাগ, সপ্তম সেশনে নোট করা), সেই নির্দিষ্ট সেশনে ফিরে গিয়ে নতুন diagnosis/fix লাগতে পারে।

---

## সেশন — Dashboard stat card granularity ফিক্স (নবম সেশনের খোলা প্রশ্নের উত্তর)

**ব্যবহারকারীর সিদ্ধান্ত:** Dashboard-এর ৪টা stat card-এ পুরো কার্ড (label+icon+value+subtitle)
একসাথে pulse করার বদলে, শুধু ভেতরের **value**-অংশটুকুই pulse করবে (label/subtitle/icon স্থির
থাকবে) — user ও solver **দুই role-ই**।

**ফাইল এডিট হয়েছে:**
- `app/src/main/java/com/example/ui/components/CommonComponents.kt` — শেয়ার্ড `StatCard`
  কম্পোনেন্ট (`AdminStatsView.kt`-সহ আরও কয়েকটা স্ক্রিন এটা ব্যবহার করে)।
- `app/src/main/java/com/example/ui/screens/DashboardScreen.kt` — `UserDashboardContent` ও
  `SolverDashboardContent` দুটোই।

### ফিক্স

- `StatCard`-এ নতুন ঐচ্ছিক প্যারামিটার `isValuePulsing: Boolean = false` যোগ করা হয়েছে (ডিফল্ট
  `false` — অন্য যেসব কলার এখনো এটা পাস করে না, যেমন `AdminStatsView.kt`, তাদের আচরণ একবিন্দুও
  বদলায়নি)। ভেতরের শুধু value `Text` (label/icon/subtitle না) এখন `PulsingValue(isUpdating =
  isValuePulsing) { Text(...) }` দিয়ে মোড়ানো।
- `DashboardScreen.kt`-এ দুই role-branchেই বাইরের `PulsingValue(isUpdating = statsPulse) { Column
  { Row{...}; Row{...} } }` wrapper সরিয়ে প্লেইন `Column { Row{...}; Row{...} }` করা হয়েছে, আর
  প্রতিটা `StatCard(...)` কলে (মোট ৮টা — ৪টা user + ৪টা solver) `isValuePulsing = statsPulse`
  পাস করা হয়েছে। `statsPulse` (`rememberFieldChangePulse`) ভ্যারিয়েবল/লজিক অপরিবর্তিত, শুধু কোথায়
  wire করা হচ্ছে সেটা বদলেছে।
- ক্যাটাগরি-লিস্ট সেকশন (user) আর balance-banner/completed-jobs/transactions সেকশন (solver) —
  এই প্রশ্নের আওতার বাইরে ছিল বলে অপরিবর্তিত রাখা হয়েছে (আগের মতোই পুরো সেকশন একসাথে pulse করে)।

### যাচাই (ধাপ ৩ চেকলিস্ট)

1. `{}`/`()`/`[]` ব্যালেন্স Kotlin-aware script দিয়ে গোনা হয়েছে দুটো ফাইলেই: `DashboardScreen.kt`
   `{}` ১৪১=১৪১, `()` ৩৯৯=৩৯৯, `[]` ১=১। `CommonComponents.kt` `{}` ২৮১=২৮১, `()` ৮৭৪=৮৭৪।
2. grep দিয়ে নিশ্চিত করা হয়েছে `PulsingValue` import এখনো ব্যবহৃত হচ্ছে (category/balance/
   completed-jobs/transactions সেকশনে, dead হয়ে যায়নি), আর `statsPulse` দুই role-branchেই
   ঠিকভাবে ৪ বার করে (মোট ৮ বার) `isValuePulsing`-এ পাস হয়েছে।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু এই দুটো ফাইল + progress ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক বদলানো হয়নি — শুধু pulse কোন লেভেলে (পুরো কার্ড বনাম শুধু
   value) প্রয়োগ হচ্ছে তার wiring বদলেছে। `StatCard`-এর নতুন প্যারামিটার ডিফল্ট `false` বলে
   `AdminStatsView.kt`-এর (admin panel, ইচ্ছাকৃতভাবে বাদ) existing কলগুলো অপ্রভাবিত।
4a. role-branch verify: দুই role-ই (`UserDashboardContent`/`SolverDashboardContent`) আলাদাভাবে
    এডিট/grep করে verify করা হয়েছে।
5. zip-এর ফাইল-সংখ্যা (২৪৭টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু এই দুটো ফাইল
   আর progress ফাইলই বদলেছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — compile/run করে verify হয়নি।
   ব্যবহারকারীকে দেখতে হবে: Dashboard-এ re-entry/pull-to-refresh/realtime ভ্যালু-বদলে এখন শুধু
   stat card-গুলোর ভেতরের সংখ্যা/টাকা-অংশটুকু pulse করছে, title/subtitle/আইকন স্থির থাকছে কিনা —
   user ও solver দুই role-ই।

## ✅ ব্যাচ ৩১ — এই খোলা প্রশ্নটাও এখন বন্ধ। বাকি শুধু real-device টেস্ট।
