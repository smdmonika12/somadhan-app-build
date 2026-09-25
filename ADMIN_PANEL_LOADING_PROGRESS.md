# Admin Panel Loading/Shimmer ফিক্স — প্রোগ্রেস নোট

এই ফাইল `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এর কাজের জন্য প্রোগ্রেস ট্র্যাক করে (ব্যাচ ৩১-এর
user/solver সাইড থেকে আলাদা, ওটার প্রোগ্রেস `SHIMMER_REGRESSION_BATCH31_PROGRESS.md`-তে আছে)।

## ✅ ধাপ ১ — ডায়াগনসিস + স্কোপ কনফার্মেশন (সম্পূর্ণ, কোনো কোড এডিট হয়নি)

### ম্যাপ পুনরায়-ভেরিফিকেশন

`AdminPanelScreen.kt`-এর ১৩১১ লাইনের `when (selectedTabIndex)` ব্লক (লাইন ৯৫৫-১৩০৬) নিজে পড়ে
যাচাই করা হয়েছে — মাস্টার প্রম্পটের ধাপ ০.৫-এর ম্যাপ **হুবহু মিলেছে**, কোনো discrepancy পাওয়া
যায়নি:

- ক্যাটেগরি ১ (`SyncAwareRefreshableContent`): ২,৪,৫,৭,৯,১৫,১৬,১৯,২১,২২,২৩ — কনফার্মড
- ক্যাটেগরি ২ (`SyncAwareContent`): ০ (Stats), ১৮ (UserLookup) — কনফার্মড
- ক্যাটেগরি ৩ (কোনো wrapper নেই): ১,৩,৬,৮,১০,১১,১২,১৩,১৪,১৭,২০,২৪ — কনফার্মড

### ক্যাটেগরি ৩-এর ১২টা ট্যাবের বর্তমান loading UI (গ্রেপ-ভেরিফায়েড)

| # | ট্যাব | কোল্ড-লোড UI | নোট |
|---|---|---|---|
| ১ | কেওয়াইসি (AdminKycView) | নেই (import unused) | নিজস্ব pagination (safePendingPage/safeVerifiedPage/safeRejectedPage) |
| ৩ | বিজ্ঞপ্তি প্রেরণ (AdminManualNotificationView) | নেই | `isSending`-ভিত্তিক action-বাটন স্পিনার মাত্র; pagination আছে |
| ৬ | ক্যাটাগরি (AdminCategoriesView) | নেই | pagination নেই |
| ৮ | অতিরিক্ত চার্জ (AdminAdditionalChargesView) | নেই | pagination আছে (safePage) |
| ১০ | রিভিউ মডারেশন (AdminRatingsView) | নেই (import-ই নেই) | pagination আছে (safePage) |
| ১১ | সেটিংস (AdminSettingsView) | নেই | `isSaving`-ভিত্তিক action-বাটন স্পিনার মাত্র; এডিট-ফর্ম-ঘেঁষা |
| ১২ | অ্যাক্টিভিটি লগ (AdminAuditLogView) | নেই | scroll-to-load pagination (`shouldLoadMore`) |
| ১৩ | FAQ ম্যানেজমেন্ট (AdminFaqManagementView) | নেই | pagination নেই |
| ১৪ | বিড বাতিলের ইতিহাস (AdminCancelledBidsView) | নেই | pagination (safeCurrentPage) + মোডাল-এ আলাদা load-more বাটন |
| ১৭ | Supabase এক্সপ্লোরার (AdminSupabaseExplorerView) | **নিজস্ব `isLoading`/`isLoadingMore` স্পিনার আছে** | dev-tool, ইতিমধ্যে নিজস্ব প্যাটার্ন |
| ২০ | রেপুটেশন ইঞ্জিন (AdminReputationEngineView) | নেই | pagination নেই |
| ২৪ | রিফান্ড ডায়াগনস্টিক (AdminRefundDebugView) | নিজস্ব `isLoading` আছে, কিন্তু শুধু debug-action বাটনে | dev-tool |

**উপসংহার:** ৯টা ট্যাবে (১,৩,৬,৮,১০,১১,১২,১৩,২০) সত্যিকারের কোনো cold-load loading state নেই।
১৪-এ pagination আছে কিন্তু cold-load গেট নেই। ১৭ ও ২৪ (dev-tool) ইতিমধ্যে নিজস্ব লোকাল প্যাটার্ন
ব্যবহার করছে, migrate করার সিদ্ধান্ত ব্যবহারকারীর উত্তর অনুযায়ী নিচে।

### ব্যবহারকারীর সিদ্ধান্ত (স্কোপ কনফার্মেশন)

- **বাগ-১ real-device sighting:** নিশ্চিত না / এখনো টেস্ট করেননি (ক্যাটেগরি ১-এর কোনো ট্যাবে)।
- **ক্যাটেগরি ৩-এর অর্ডার:** সব ১২টাই migrate হবে, মাস্টার প্রম্পটের সাজেস্টেড অর্ডার অনুযায়ী
  (১৭ ও ২৪ dev-tool দুটোও বাদ যাবে না, শুধু শেষে)।

---

## ✅ সেশন ২.১ — ক্যাটেগরি ১-এর ১১টা Refreshable ট্যাব: bug-1 verify (সম্পূর্ণ, কোনো এডিট হয়নি)

`MotionToolkit.kt`-এ `flashRequestCount`-এর তিনটা ট্রিগার-স্পট (re-entry flash, data-change flash,
manual-refresh-completion flash — লাইন ৮৯০-৯৪৮) আলাদাভাবে দেখা হলো — **তিনটাতেই `try { delay(...);
lastShownData = data } finally { flashRequestCount-- }` প্যাটার্ন এখনো বহাল আছে**, স্টাক-শিমার বাগ-১
(finally ছাড়া decrement) ফিক্স ঠিকভাবে আছে।

ক্যাটেগরি ১-এর ১১টা View ফাইলেই (`AdminWithdrawalsView.kt`, `AdminUsersView.kt`,
`AdminProblemsView.kt`, `AdminEscrowView.kt`, `AdminTransactionsView.kt`,
`AdminChatMonitoringView.kt`, `AdminDirectContractsView.kt`, `AdminSolverQuotaView.kt`,
`AdminInstantJobsView.kt`, `AdminDisputeCenterView.kt`, `AdminGatewayPaymentsView.kt`) গ্রেপ করে
নিশ্চিত হওয়া হলো — কোনোটাতেই `flashRequestCount`/`activePulseReasons`/`SessionAwareLoadingContent`-এর
কোনো **লোকাল রি-ইমপ্লিমেন্টেশন নেই** — সবাই `AdminPanelScreen.kt`-এর শেয়ার্ড
`SyncAwareRefreshableContent` wrapper-ই ব্যবহার করে। তাই শেয়ার্ড-কম্পোনেন্ট ফিক্স স্বয়ংক্রিয়ভাবে
এই ১১টাতেই প্রযোজ্য — আলাদা করে কিছু বসানোর দরকার নেই।

`data = listOf(...)` wiring প্রতিটা ট্যাবে চেক করা হলো — কোনোটাতেই unrelated field মেশানো নেই
(প্রতিটা প্যারামিটার সংশ্লিষ্ট View-এর prop হিসেবে সরাসরি ব্যবহৃত/প্রদর্শিত হয়, ধাপ ০.১৪(খ)-এর
কম্পোজিট-ডেটা প্যাটার্নের মতো ঝুঁকি কোড-লেভেলে দেখা যায়নি)।

**কোনো নতুন কোড-বাগ পাওয়া যায়নি — বহাল আছে।** ব্যবহারকারী যেহেতু এখনো real device-এ টেস্ট করেননি,
তাই এই উপসংহার শুধু কোড-লেভেল যাচাই পর্যন্ত সীমিত — real-device টেস্টের পর নতুন কিছু দেখা গেলে এই
সেশনে ফিরে আসা যাবে।

### যাচাই
- এই সেশনে কোনো কোড এডিট হয়নি (শুধু এই progress ফাইল নতুন তৈরি হয়েছে) — brace-balance/zip-count
  ভেরিফিকেশনের দরকার নেই।

## ✅ সেশন ২.৩ — কেওয়াইসি (ইনডেক্স ১) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/pull-to-refresh/ট্যাব-পাল্টানো/pagination-পাল্টানো — এই চারটাতেই **শুধু তালিকা**
(কার্ডগুলো) pulse করবে; সার্চ বার, ট্যাব হেডার, pagination bar — বাকি সব structure অপরিবর্তিত
থাকবে (কোনো full-card cold-load-স্টাইল skeleton-flash পুরো ট্যাব জুড়ে না)।

### ফিক্স
যেহেতু চাহিদাটা "শুধু তালিকা pulse, বাকি স্ট্রাকচার স্থির" — `SyncAwareRefreshableContent`-এর
`data`-ভিত্তিক whole-content flash এখানে ব্যবহার করা হয়নি (ওটা সার্চ বার/ট্যাব হেডারসহ পুরো
ট্যাবকেই flash করাতো)। বদলে Dashboard stat-card পুরনো সেশনের প্যাটার্ন অনুসরণ করা হয়েছে
(`rememberFieldChangePulse` + `PulsingValue`), শুধু ছোট পরিসরে প্রয়োগ করে:

- **`AdminPanelScreen.kt` (ইনডেক্স ১):** `SyncAwareContent` (rule ১+২, cold-load skeleton
  gate) দিয়ে wrap করা হয়েছে, `sessionKey = "admin_kyc_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminKycView`-কে নতুন `viewModel = viewModel`
  আর `isManualRefreshing = isRefreshing` পাস করা হচ্ছে।
- **`AdminKycView.kt`:** দুটো নতুন ঐচ্ছিক প্যারামিটার (`viewModel: SomadhanViewModel? = null`,
  `isManualRefreshing: Boolean = false` — ডিফল্ট থাকায় অন্য কোনো hypothetical call-site থাকলেও
  আচরণ অপরিবর্তিত থাকতো, যদিও grep-এ একটাই call-site পাওয়া গেছে)। বর্তমানে দৃশ্যমান
  (`selectedKycTab` অনুযায়ী) paginated তালিকা নিয়ে একটা `visibleKycList` বানিয়ে
  `rememberFieldChangePulse(value = selectedKycTab to visibleKycList, isManualRefreshing =
  isManualRefreshing, sessionKey = "admin_kyc_sync", viewModel = viewModel)` কল করা হয়েছে —
  ট্যাব পাল্টানো/pagination পাল্টানো/আসল ডেটা বদলানো, তিনটাই "value বদলেছে" হিসেবে গণ্য হয়ে
  pulse ট্রিগার করে, আর pull-to-refresh সম্পন্ন হলে আলাদাভাবে। তিনটা তালিকারই (`পেন্ডিং`,
  `ভেরিফাইড`, `রিজেক্টেড`) `items(...)` ব্লকের `Card(...)`-কে `PulsingValue(isUpdating =
  kycListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু কার্ডগুলো pulse করবে, বাইরের সার্চ
  বার/ট্যাব হেডার/pagination bar (`LazyColumn`-এর বাইরে, fixed) স্পর্শ করা হয়নি।
- আগে থেকে অব্যবহৃত (dead) `SomadhanViewModel` import এখন প্রকৃতপক্ষে ব্যবহৃত হচ্ছে।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স স্ক্রিপ্ট দিয়ে গোনা হয়েছে: `AdminKycView.kt` `{}` ৩৫৬=৩৫৬, `()` ৯৩০=৯৩০।
   `AdminPanelScreen.kt` `{}` ২০২=২০২, `()` ৪১৯=৪১৯।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminKycView(` কল-সাইট এখনো ঠিক একটাই (`AdminPanelScreen.kt`),
   আর ঠিক ৩টা `PulsingValue(isUpdating = kycListPulse)` (পেন্ডিং/ভেরিফাইড/রিজেক্টেড — একটা করে)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি (শুধু এই দুটো ফাইল + progress ফাইল)।
4. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — approve/reject/revoke/bulk-approve/edit-kyc-info-এর
   কোনো লজিক স্পর্শ করা হয়নি, শুধু loading/pulse presentation যোগ হয়েছে। বাগ-১ ক্লাস রিস্ক
   (stuck shimmer): `rememberFieldChangePulse`-এর শেয়ার্ড `finally` ফিক্স ইতিমধ্যে আছে (সেশন
   ২.১-এ ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা হয়নি — তাই এই ট্যাবেও একই সুরক্ষা
   প্রযোজ্য।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি, শুধু sessionKey/wrapper।
5. zip-এর ফাইল-সংখ্যা মূল আপলোড করা zip + আগের progress ফাইলের সাথে মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: কেওয়াইসি
   ট্যাবে ট্যাব পাল্টালে/pagination পাল্টালে/re-entry-তে/pull-to-refresh সম্পন্ন হলে শুধু কার্ড
   তালিকা pulse করছে কিনা, সার্চ বার/ট্যাব হেডার/pagination bar স্থির থাকছে কিনা, আর approve/
   reject/revoke/bulk-approve action-এর পর কোনো stuck-shimmer হচ্ছে না কিনা।

## পরবর্তী সেশন
সাজেস্টেড অর্ডার অনুযায়ী পরের ধাপ: **২.৪ (বিজ্ঞপ্তি প্রেরণ, ইনডেক্স ৩)** — শুরুর আগে target design
(cold-load skeleton shape, কোন অংশ pulse হবে, pull-to-refresh দরকার কিনা) ব্যবহারকারীর কাছ থেকে
কনফার্ম করে নিতে হবে (Ground Rule ১৬)।

## ✅ সেশন ২.৪ — বিজ্ঞপ্তি প্রেরণ (ইনডেক্স ৩) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/realtime data-change/pagination-পাল্টানো — কোনোটাতেই কোনো re-flash/pulse হবে না, **শুধু
প্রথম ভিজিটে** (cold-load) পুরো ট্যাব একবার skeleton দিয়ে লোড হবে। pull-to-refresh দরকার নেই
(dev/admin-ধর্মী ট্যাব)। send/schedule action-এর পর history লিস্টে stuck-shimmer না হয় সেটা
বিশেষভাবে যাচাই করতে বলা হয়েছিল।

### ফিক্স
এটা KYC (২.৩)-এর চেয়ে সহজ — কোনো re-entry pulse-ই চাওয়া হয়নি, তাই `SyncAwareContent`-এর ভেতরে
`AdminKycView`-এর মতো আলাদা `rememberFieldChangePulse`/`PulsingValue` যোগ করার দরকার হয়নি।
- **`AdminPanelScreen.kt` (ইনডেক্স ৩):** plain `SyncAwareContent` (rule ১ শুধু, `data` প্যারামিটার
  ছাড়া — B2 প্যাটার্ন) দিয়ে wrap করা হয়েছে, `sessionKey = "admin_manual_notification_sync"`,
  skeleton `ListScreenSkeleton(tint = SomadhanAdminSlate)` (অন্য সব ট্যাবের সাথে সামঞ্জস্যপূর্ণ)।
  `isManualRefreshing` পাস করা হয়নি (pull-to-refresh নেই বলে)।
- **`AdminManualNotificationView.kt`:** কোনো এডিট করা হয়নি — কোনো ভেতরের pulse/flash লজিক এই
  স্ক্রিনে দরকার নেই বলে এই ফাইল স্পর্শ করারই প্রয়োজন হয়নি।

### স্টাক-শিমার যাচাই (বিশেষভাবে চাওয়া হয়েছিল)
এই ডিজাইনে কোনো post-action flash/pulse মেকানিজমই যোগ করা হয়নি (শুধু one-time cold-load গেট,
যেটা `markLoadedOnce`-এর পর সারা সেশনে আর কখনো ট্রিগার হয় না) — তাই send/schedule/cancel/delete
action-এর পর স্টাক-শিমারের কাঠামোগত কোনো ঝুঁকিই নেই (কোনো `flashRequestCount`-জাতীয় কাউন্টার এই
পথে নেই যা আটকে যেতে পারে)।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminPanelScreen.kt` `{}` ২০৫=২০৫, `()` ৪২৫=৪২৫।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminManualNotificationView(` কল-সাইট এখনো ঠিক একটাই।
3. `AdminManualNotificationView.kt` এই সেশনে ছোঁয়া হয়নি, শুধু `AdminPanelScreen.kt` + এই progress
   ফাইল।
4. onSendNotification/onCancelScheduledNotification/onDeleteNotification-এর কোনো লজিক স্পর্শ করা
   হয়নি, শুধু cold-load presentation যোগ হয়েছে।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা ফাইল) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে (dotfile
   সহ)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: এই
   ট্যাবে প্রথমবার ঢুকলে skeleton flash হচ্ছে কিনা, তারপর ট্যাব পাল্টিয়ে আবার ফিরলে/pagination
   পাল্টালে/send-schedule-cancel-delete action করার পরও **কোনো re-flash হচ্ছে না** সেটা, আর কোনো
   জায়গায় stuck-shimmer আটকে থাকছে না তা কনফার্ম করা।

## ✅ সেশন ২.৫ — ক্যাটাগরি (ইনডেক্স ৬) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/realtime data-change-এ শুধু ক্যাটাগরি-লিস্টের কার্ডগুলো pulse করবে (toggle/Add বাটন স্থির)
— KYC (২.৩)-এর মতো। এই ট্যাবে pull-to-refresh **দরকার** (Manual Notification/২.৪-এর থেকে আলাদা
সিদ্ধান্ত)। add/edit/delete/toggle-active action-এর পর stuck-shimmer বিশেষভাবে যাচাই করতে বলা
হয়েছিল।

### ফিক্স
হুবহু KYC (২.৩)-এর প্যাটার্ন, শুধু single (non-tabbed, non-paginated) লিস্টে প্রয়োগ করা হলো:
- **`AdminPanelScreen.kt` (ইনডেক্স ৬):** `SyncAwareContent` (rule ১+২ cold-load, `data`-বিহীন)
  দিয়ে wrap করা হয়েছে, `sessionKey = "admin_categories_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminCategoriesView`-কে নতুন `viewModel =
  viewModel` আর `isManualRefreshing = isRefreshing` পাস করা হচ্ছে (গ্লোবাল `SomadhanPullToRefresh`
  wrapper-এর `isRefreshing` state — এই wrapper আগে থেকেই পুরো `when` ব্লক ঢেকে রাখে, তাই আলাদা
  pull-gesture যোগ করার দরকার হয়নি, শুধু এই ট্যাবে সেই সিগন্যালটা pulse-এ wire করা হলো)।
- **`AdminCategoriesView.kt`:** দুটো নতুন ঐচ্ছিক প্যারামিটার (`viewModel: SomadhanViewModel? =
  null`, `isManualRefreshing: Boolean = false`)। `rememberFieldChangePulse(value = categories,
  isManualRefreshing = isManualRefreshing, sessionKey = "admin_categories_sync", viewModel =
  viewModel)` কল করা হয়েছে — ডেটা বদলানো/pull-to-refresh সম্পন্ন হওয়া/re-entry, তিনটাতেই pulse
  ট্রিগার করে। `items(categories, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating =
  categoriesListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু কার্ডগুলো pulse করবে, toggle/Add
  বাটন (`LazyColumn`-এর বাইরে) স্পর্শ করা হয়নি। নতুন import: `PulsingValue`,
  `rememberFieldChangePulse`।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminCategoriesView.kt` `{}` ১২৬=১২৬, `()` ৩৫৪=৩৫৪।
   `AdminPanelScreen.kt` `{}` ২০৮=২০৮, `()` ৪৩২=৪৩২।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminCategoriesView(` কল-সাইট এখনো ঠিক একটাই, আর ঠিক ১টা
   `PulsingValue(isUpdating = categoriesListPulse)` (একটাই তালিকা, KYC-র তিনটা ট্যাবের মতো না)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি।
4. onAddClick/onEditCategory/onDeleteCategory/onToggleActive/onSetPhysicalWorkEnabled/
   onSetVirtualWorkEnabled-এর কোনো লজিক স্পর্শ করা হয়নি, শুধু loading/pulse presentation যোগ হয়েছে।
   বাগ-১ ক্লাস রিস্ক: `rememberFieldChangePulse`-এর শেয়ার্ড `finally` ফিক্স ইতিমধ্যে আছে (সেশন ২.১
   ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা হয়নি।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা) আগের zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   ক্যাটাগরি ট্যাবে re-entry/pull-to-refresh সম্পন্ন হলে/ডেটা বদলালে শুধু কার্ড তালিকা pulse
   করছে কিনা, toggle/Add বাটন স্থির থাকছে কিনা, আর add/edit/delete/toggle-active action-এর পর
   কোনো stuck-shimmer হচ্ছে না কিনা।

## ✅ সেশন ২.৬ — অতিরিক্ত চার্জ (ইনডেক্স ৮) migrate (সম্পূর্ণ)

### সংশোধনী (মাস্টার প্রম্পটের নোট থেকে)
মাস্টার প্রম্পটের তালিকায় এই ট্যাবের নোটে লেখা ছিল "settle action আছে" — কিন্তু
`AdminAdditionalChargesView.kt` grep করে দেখা গেল এই ট্যাবে আসলে কোনো settle/approve/reject-জাতীয়
mutation action নেই, এটা pure read-only মনিটরিং স্ক্রিন (function signature-এ শুধু `charges: List<
AdditionalChargeEntity>` প্যারামিটার ছিল, কোনো `on...` callback ছিল না; PENDING চার্জে শুধু
"গ্রাহকের সিদ্ধান্তের অপেক্ষায়" তথ্য-ব্যাজ দেখায়)।

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/pull-to-refresh/ডেটা বদলালে শুধু চার্জ-তালিকার কার্ড pulse করবে (KYC/Categories-ধরনের)।
pull-to-refresh দরকার।

### ফিক্স
হুবহু ক্যাটাগরি (২.৫)-এর প্যাটার্ন, শুধু paginated তালিকায় প্রয়োগ করা হলো:
- **`AdminPanelScreen.kt` (ইনডেক্স ৮):** `SyncAwareContent` (rule ১+২ cold-load, `data`-বিহীন)
  দিয়ে wrap করা হয়েছে, `sessionKey = "admin_additional_charges_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminAdditionalChargesView`-কে নতুন
  `viewModel = viewModel` আর `isManualRefreshing = isRefreshing` পাস করা হচ্ছে (গ্লোবাল
  `SomadhanPullToRefresh` wrapper-এর `isRefreshing` state, ক্যাটাগরির মতোই)।
- **`AdminAdditionalChargesView.kt`:** দুটো নতুন ঐচ্ছিক প্যারামিটার (`viewModel: SomadhanViewModel?
  = null`, `isManualRefreshing: Boolean = false`)। `paginatedCharges` কম্পিউট হওয়ার পর
  `rememberFieldChangePulse(value = paginatedCharges, isManualRefreshing = isManualRefreshing,
  sessionKey = "admin_additional_charges_sync", viewModel = viewModel)` কল করা হয়েছে — এই ট্যাবে
  pagination থাকায় `value` হিসেবে `paginatedCharges` (raw `charges` না) বেছে নেওয়া হলো, যাতে পেজ
  পাল্টানো/ফিল্টার পাল্টানো/আসল ডেটা বদলানো — তিনটাই "value বদলেছে" হিসেবে গণ্য হয়ে pulse ট্রিগার
  করে। `items(paginatedCharges, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating =
  chargesListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু চার্জ-কার্ডগুলো pulse করবে, হেডার/সামারি
  কার্ড, সার্চ/ফিল্টার কার্ড (`LazyColumn`-এর `item {}` ব্লক, আলাদা) আর pagination bar
  (`LazyColumn`-এর বাইরে, fixed) স্পর্শ করা হয়নি। নতুন import: `PulsingValue`,
  `rememberFieldChangePulse`।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminAdditionalChargesView.kt` `{}` ১০৩=১০৩, `()` ৩৬৭=৩৬৭।
   `AdminPanelScreen.kt` `{}` ২১২=২১২, `()` ৪৪০=৪৪০।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminAdditionalChargesView(` কল-সাইট এখনো ঠিক একটাই, আর ঠিক ১টা
   `PulsingValue(isUpdating = chargesListPulse)` (একটাই তালিকা, pagination থাকলেও একটাই `items`
   ব্লক)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি।
4. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — এই স্ক্রিনে যেহেতু কোনো mutation action-ই নেই (উপরের
   সংশোধনী দ্রষ্টব্য), search/filter/pagination লজিকও স্পর্শ করা হয়নি, শুধু loading/pulse
   presentation যোগ হয়েছে। বাগ-১ ক্লাস রিস্ক: `rememberFieldChangePulse`-এর শেয়ার্ড `finally` ফিক্স
   ইতিমধ্যে আছে (সেশন ২.১ ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা হয়নি।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: অতিরিক্ত
   চার্জ ট্যাবে re-entry/pull-to-refresh সম্পন্ন হলে/পেজ পাল্টালে/ফিল্টার পাল্টালে শুধু চার্জ-কার্ড
   pulse করছে কিনা, হেডার/সামারি কার্ড ও সার্চ/ফিল্টার কার্ড ও pagination bar স্থির থাকছে কিনা।

## ✅ সেশন ২.৭ — রিভিউ মডারেশন (ইনডেক্স ১০) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/pull-to-refresh/ফিল্টার-সর্ট-পেজ পাল্টালে শুধু রিভিউ-তালিকার কার্ড pulse করবে
(KYC/Categories/অতিরিক্ত-চার্জ-ধরনের)। pull-to-refresh দরকার।

### ফিক্স
হুবহু অতিরিক্ত চার্জ (২.৬)-এর প্যাটার্ন, শুধু এখানে ফিল্টার + সর্ট + pagination তিনটাই আছে (আর একটা
`onDelete` mutation action):
- **`AdminPanelScreen.kt` (ইনডেক্স ১০):** `SyncAwareContent` (rule ১+২ cold-load, `data`-বিহীন)
  দিয়ে wrap করা হয়েছে, `sessionKey = "admin_ratings_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminRatingsView`-কে নতুন `viewModel =
  viewModel` আর `isManualRefreshing = isRefreshing` পাস করা হচ্ছে (গ্লোবাল
  `SomadhanPullToRefresh` wrapper-এর `isRefreshing` state, আগের সেশনগুলোর মতোই)।
- **`AdminRatingsView.kt`:** দুটো নতুন ঐচ্ছিক প্যারামিটার (`viewModel: SomadhanViewModel? = null`,
  `isManualRefreshing: Boolean = false`, নতুন import `SomadhanViewModel` সহ)। `paginatedRatings`
  কম্পিউট হওয়ার পর `rememberFieldChangePulse(value = paginatedRatings, isManualRefreshing =
  isManualRefreshing, sessionKey = "admin_ratings_sync", viewModel = viewModel)` কল করা হয়েছে —
  ফিল্টার (rating/role)/সর্ট/পেজ পাল্টানো/আসল ডেটা বদলানো (delete-সহ), সবই "value বদলেছে" হিসেবে
  গণ্য হয়ে pulse ট্রিগার করে। `items(paginatedRatings, ...)`-এর ভেতরের `Card(...)`-কে
  `PulsingValue(isUpdating = ratingsListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু রিভিউ-কার্ড
  pulse করবে, হেডার/সার্চ-ফিল্টার-সর্ট কন্ট্রোল (`LazyColumn`-এর আলাদা `item {}` ব্লক) আর
  pagination bar (`LazyColumn`-এর বাইরে, fixed) স্পর্শ করা হয়নি। নতুন import: `PulsingValue`,
  `rememberFieldChangePulse`।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminRatingsView.kt` `{}` ১৮৫=১৮৫, `()` ৫৭৭=৫৭৭।
   `AdminPanelScreen.kt` `{}` ২১৫=২১৫, `()` ৪৪৯=৪৪৯।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminRatingsView(` কল-সাইট এখনো ঠিক একটাই, আর ঠিক ১টা
   `PulsingValue(isUpdating = ratingsListPulse)` (একটাই তালিকা)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি।
4. `onDelete`-এর (রিভিউ ডিলিট) কোনো লজিক স্পর্শ করা হয়নি, শুধু loading/pulse presentation যোগ
   হয়েছে। বাগ-১ ক্লাস রিস্ক (stuck shimmer): `rememberFieldChangePulse`-এর শেয়ার্ড `finally` ফিক্স
   ইতিমধ্যে আছে (সেশন ২.১ ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা হয়নি — delete-এর পর
   pulse স্বাভাবিকভাবেই শেষ হয়ে যাবে।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: রিভিউ
   মডারেশন ট্যাবে re-entry/pull-to-refresh সম্পন্ন হলে/ফিল্টার-সর্ট-পেজ পাল্টালে/delete করার পর
   শুধু রিভিউ-কার্ড pulse করছে কিনা, হেডার/কন্ট্রোল/pagination bar স্থির থাকছে কিনা, আর কোনো
   stuck-shimmer হচ্ছে না কিনা।

## ✅ সেশন ২.৮ — সেটিংস (ইনডেক্স ১১) migrate (সম্পূর্ণ)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
ক্যাটেগরি E (`SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md`-এর এডিট-ফর্ম ব্যতিক্রম) — শুধু rule ১
(cold-load), কোনো pulse/diff-শিমার (rule ২) না।

### গ্রেপ-ভেরিফিকেশন (সিদ্ধান্ত নেওয়ার আগে)
`AdminSettingsView.kt`-এ (২৩৫১ লাইন) কোনো `items()`/`LazyColumn`-ভিত্তিক তালিকা নেই — পুরোটাই
আলাদা আলাদা সেটিংস-সেকশন (পাসওয়ার্ড পরিবর্তন, রেডিয়াস, টগল ইত্যাদি), প্রতিটাতে নিজস্ব
`OutlinedTextField`/`Switch`/`Button` + `isSaving`-স্টাইল লোকাল স্টেট — কাঠামোগতভাবে
`PostProblemScreen`/`SolverSkillsScreen`-এর মতোই active edit-form।

### ফিক্স
- **`AdminPanelScreen.kt` (ইনডেক্স ১১):** `SyncAwareContent` (rule ১ শুধু, `data`-বিহীন) দিয়ে
  wrap করা হয়েছে, `sessionKey = "admin_settings_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminSettingsView`-এ কল অপরিবর্তিত
  (`viewModel`, `onLogout` — আগে থেকেই ছিল)।
- **`AdminSettingsView.kt`:** কোনো এডিট করা হয়নি — ক্যাটেগরি E সিদ্ধান্ত অনুযায়ী কোনো
  pulse/diff-শিমার লজিক এই স্ক্রিনে যোগ করা হয়নি বলে এই ফাইল স্পর্শ করারই প্রয়োজন হয়নি (Manual
  Notification/২.৪-এর মতোই — শুধু cold-load gate, ভেতরে কিছু বসেনি)।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminPanelScreen.kt` `{}` ২১৯=২১৯, `()` ৪৫৬=৪৫৬।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminSettingsView(` কল-সাইট এখনো ঠিক একটাই।
3. `AdminSettingsView.kt` এই সেশনে ছোঁয়া হয়নি, শুধু `AdminPanelScreen.kt` + এই progress ফাইল।
4. পাসওয়ার্ড পরিবর্তন/রেডিয়াস-সেভ/টগল-সেভ কোনো action-এর লজিক স্পর্শ করা হয়নি, শুধু cold-load
   presentation যোগ হয়েছে। এখানে কোনো `rememberFieldChangePulse` নেই বলে stuck-shimmer ক্লাস
   রিস্কই প্রযোজ্য না (কোনো নতুন `flashRequestCount`-জাতীয় কাউন্টার তৈরি হয়নি)।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: সেটিংস
   ট্যাবে প্রথমবার ঢুকলে skeleton flash হচ্ছে কিনা, তারপর ফর্মে টাইপ করার মাঝখানে কোনো
   background-refresh-এ ফর্ম flash/disrupt হচ্ছে না সেটা, আর save action-গুলো (পাসওয়ার্ড/রেডিয়াস/
   টগল) আগের মতোই কাজ করছে কিনা।

## ✅ সেশন ২.৯ — অ্যাক্টিভিটি লগ (ইনডেক্স ১২) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/pull-to-refresh/ফিল্টার-সার্চ-scroll-load পাল্টালে শুধু লগ-তালিকার কার্ড pulse করবে
(KYC/Categories-ধরনের)। pull-to-refresh দরকার।

### গ্রেপ-ভেরিফিকেশন
এই ট্যাবে কোনো mutation action নেই (pure read-only, `auditLogs: List<AdminAuditLogEntity>` ছাড়া
আর কোনো callback প্যারামিটার ছিল না) — অতিরিক্ত চার্জ (২.৬)-এর মতোই। Pagination scroll-to-load
টাইপ (`displayedCount`/`shouldLoadMore`, বাটন-ভিত্তিক page না)।

### ফিক্স
- **`AdminPanelScreen.kt` (ইনডেক্স ১২):** `SyncAwareContent` (rule ১+২ cold-load, `data`-বিহীন)
  দিয়ে wrap করা হয়েছে, `sessionKey = "admin_audit_log_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminAuditLogView`-কে নতুন `viewModel =
  viewModel` আর `isManualRefreshing = isRefreshing` পাস করা হচ্ছে।
- **`AdminAuditLogView.kt`:** দুটো নতুন ঐচ্ছিক প্যারামিটার (`viewModel: SomadhanViewModel? = null`,
  `isManualRefreshing: Boolean = false`)। `visibleLogs` কম্পিউট হওয়ার পর
  `rememberFieldChangePulse(value = visibleLogs, isManualRefreshing = isManualRefreshing,
  sessionKey = "admin_audit_log_sync", viewModel = viewModel)` কল করা হয়েছে — ফিল্টার/সার্চ
  পাল্টানো/scroll-to-load-এ আরও লগ যোগ হওয়া/আসল ডেটা বদলানো, সবই ট্রিগার করে। প্রতিটা কার্ড আলাদা
  `AuditLogItemCard(log = log)` কম্পোজেবলে ফ্যাক্টর করা ছিল বলে `items(visibleLogs, ...)`-এর
  ভেতরে সরাসরি `Card(...)` না মুড়ে বরং `PulsingValue(isUpdating = auditLogListPulse) {
  AuditLogItemCard(log = log) }` দিয়ে পুরো কল-সাইটটা মোড়ানো হয়েছে — হেডার/সার্চ-ফিল্টার চিপ
  (`LazyColumn`-এর আলাদা `item {}` ব্লক) স্পর্শ করা হয়নি। নতুন import: `PulsingValue`,
  `rememberFieldChangePulse`।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminAuditLogView.kt` `{}` ৮৪=৮৪, `()` ২৭৯=২৭৯।
   `AdminPanelScreen.kt` `{}` ২২২=২২২, `()` ৪৬৬=৪৬৬।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminAuditLogView(` কল-সাইট এখনো ঠিক একটাই, আর ঠিক ১টা
   `PulsingValue(isUpdating = auditLogListPulse)`।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি।
4. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — filteredLogs/scroll-to-load-এর লজিক অপরিবর্তিত, শুধু
   loading/pulse presentation যোগ হয়েছে। বাগ-১ ক্লাস রিস্ক: `rememberFieldChangePulse`-এর শেয়ার্ড
   `finally` ফিক্স ইতিমধ্যে আছে (সেশন ২.১ ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা
   হয়নি।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   অ্যাক্টিভিটি লগ ট্যাবে re-entry/pull-to-refresh সম্পন্ন হলে/ফিল্টার-সার্চ পাল্টালে/scroll করে
   আরও লগ লোড হলে শুধু কার্ড pulse করছে কিনা, হেডার/ফিল্টার-চিপ স্থির থাকছে কিনা।

## ✅ সেশন ২.১০ — FAQ ম্যানেজমেন্ট (ইনডেক্স ১৩) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/pull-to-refresh/ডেটা বদলালে শুধু FAQ-তালিকার কার্ড pulse করবে (KYC/Categories-ধরনের)।
pull-to-refresh দরকার।

### গ্রেপ-ভেরিফিকেশন
কোনো pagination নেই (আগের ধাপ ১-এর নোটের সাথে মিলেছে)। এই স্ক্রিনে নিজস্ব ইউজার/সলভার `selectedFaqTab`
আছে (KYC-র মতো) — `tabFaqs` এই ট্যাব-এর উপর নির্ভরশীল, আর `filteredFaqs` (সার্চ-সহ) `tabFaqs`-এর
উপর নির্ভরশীল। add/edit/delete তিনটা mutation action আছে।

### ফিক্স
হুবহু ক্যাটাগরি (২.৫)-এর প্যাটার্ন, শুধু `filteredFaqs`-ই ইতিমধ্যে ট্যাব-নির্ভরশীল হওয়ায় KYC-র
মতো আলাদা কম্পোজিট `value` (`ট্যাব to তালিকা`) বানানোর দরকার হয়নি — `filteredFaqs` একাই যথেষ্ট:
- **`AdminPanelScreen.kt` (ইনডেক্স ১৩):** `SyncAwareContent` (rule ১+২ cold-load, `data`-বিহীন)
  দিয়ে wrap করা হয়েছে, `sessionKey = "admin_faq_management_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminFaqManagementView`-কে নতুন `viewModel =
  viewModel` আর `isManualRefreshing = isRefreshing` পাস করা হচ্ছে।
- **`AdminFaqManagementView.kt`:** দুটো নতুন ঐচ্ছিক প্যারামিটার (`viewModel: SomadhanViewModel? =
  null`, `isManualRefreshing: Boolean = false`)। `filteredFaqs` কম্পিউট হওয়ার পর
  `rememberFieldChangePulse(value = filteredFaqs, isManualRefreshing = isManualRefreshing,
  sessionKey = "admin_faq_management_sync", viewModel = viewModel)` কল করা হয়েছে — ট্যাব
  পাল্টানো/সার্চ পাল্টানো/আসল ডেটা বদলানো (add/edit/delete-সহ), সবই ট্রিগার করে। `items(
  filteredFaqs, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating = faqListPulse) {
  Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু FAQ-কার্ড pulse করবে, ট্যাব হেডার/সার্চ বার/Add বাটন
  (`LazyColumn`-এর বাইরে) স্পর্শ করা হয়নি। নতুন import: `PulsingValue`,
  `rememberFieldChangePulse`।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminFaqManagementView.kt` `{}` ১১০=১১০, `()` ৩১৪=৩১৪।
   `AdminPanelScreen.kt` `{}` ২২৫=২২৫, `()` ৪৭৪=৪৭৪।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminFaqManagementView(` কল-সাইট এখনো ঠিক একটাই, আর ঠিক ১টা
   `PulsingValue(isUpdating = faqListPulse)`।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি।
4. onAddClick/onEditFaq/onDeleteFaq-এর কোনো লজিক স্পর্শ করা হয়নি, শুধু loading/pulse presentation
   যোগ হয়েছে। বাগ-১ ক্লাস রিস্ক: `rememberFieldChangePulse`-এর শেয়ার্ড `finally` ফিক্স ইতিমধ্যে
   আছে (সেশন ২.১ ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা হয়নি।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: FAQ
   ম্যানেজমেন্ট ট্যাবে ট্যাব পাল্টালে/সার্চ পাল্টালে/re-entry-tে/pull-to-refresh সম্পন্ন হলে শুধু
   কার্ড pulse করছে কিনা, ট্যাব হেডার/সার্চ বার/Add বাটন স্থির থাকছে কিনা, আর add/edit/delete
   action-এর পর কোনো stuck-shimmer হচ্ছে না কিনা।

## ✅ সেশন ২.১১ — বিড বাতিলের ইতিহাস (ইনডেক্স ১৪) migrate (সম্পূর্ণ)

### ব্যবহারকারীর target design (কনফার্মড)
Re-entry/pull-to-refresh/সার্চ-ফিল্টার-পেজ পাল্টালে শুধু বিড-তালিকার কার্ড pulse করবে
(KYC/Categories-ধরনের)। pull-to-refresh দরকার।

### গ্রেপ-ভেরিফিকেশন
এই ট্যাবে কোনো mutation action নেই (pure read-only — `cancelledBids`, `allProblems`, `allUsers`,
`viewModel` ছাড়া আর কোনো callback প্যারামিটার ছিল না) — অতিরিক্ত চার্জ (২.৬)/অ্যাক্টিভিটি লগ
(২.৯)-এর মতোই। Pagination বাটন-ভিত্তিক (safeCurrentPage/totalPages, itemsPerPage=10, স্ক্রল-লোড
না)। এছাড়া একটা আলাদা "ঘন ঘন বাতিলকারী" মোডাল আছে (নিজস্ব `modalVisibleCount` লোড-মোর), কিন্তু
ব্যবহারকারীর কনফার্মড স্কোপ শুধু মূল বিড-তালিকা — তাই এই মোডাল স্পর্শ করা হয়নি।

### ফিক্স
হুবহু অতিরিক্ত চার্জ (২.৬)/অ্যাক্টিভিটি লগ (২.৯)-এর প্যাটার্ন:
- **`AdminPanelScreen.kt` (ইনডেক্স ১৪):** `SyncAwareContent` (rule ১+২ cold-load, `data`-বিহীন)
  দিয়ে wrap করা হয়েছে, `sessionKey = "admin_cancelled_bids_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminCancelledBidsView`-কে নতুন
  `isManualRefreshing = isRefreshing` পাস করা হচ্ছে (`viewModel = viewModel` আগে থেকেই পাস করা
  হতো, এই প্যারামিটার নতুন যোগ হয়নি)।
- **`AdminCancelledBidsView.kt`:** একটা নতুন ঐচ্ছিক প্যারামিটার (`isManualRefreshing: Boolean =
  false` — `viewModel` প্যারামিটার আগে থেকেই ছিল)। `paginatedBids` কম্পিউট হওয়ার পর
  `rememberFieldChangePulse(value = paginatedBids, isManualRefreshing = isManualRefreshing,
  sessionKey = "admin_cancelled_bids_sync", viewModel = viewModel)` কল করা হয়েছে — সার্চ/টাইম-
  ফিল্টার/সলভার-ফিল্টার/পেজ পাল্টানো, সবই "value বদলেছে" হিসেবে গণ্য হয়ে pulse ট্রিগার করে।
  `items(paginatedBids, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating =
  cancelledBidsListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু বিড-কার্ড pulse করবে, সামারি/
  অ্যানালিটিক্স কার্ড, "ঘন ঘন বাতিলকারী" সারি ও মোডাল, সার্চ/ফিল্টার কন্ট্রোল (সবই `LazyColumn`-এর
  আলাদা `item {}` ব্লক) আর pagination bar (`LazyColumn`-এর বাইরে, fixed) স্পর্শ করা হয়নি। নতুন
  import: `PulsingValue`, `rememberFieldChangePulse`।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminCancelledBidsView.kt` `{}` ২০৭=২০৭, `()` ৫৯৬=৫৯৬।
   `AdminPanelScreen.kt` `{}` ২২৮=২২৮, `()` ৪৮৩=৪৮৩।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminCancelledBidsView(` কল-সাইট এখনো ঠিক একটাই, আর ঠিক ১টা
   `PulsingValue(isUpdating = cancelledBidsListPulse)` (মোডালের তালিকা এই pulse-এর অংশ না)।
3. অন্য কোনো ফাইল এই সেশনে ছোঁয়া হয়নি।
4. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — সার্চ/টাইম-ফিল্টার/সলভার-ফিল্টার/pagination/মোডাল লজিক
   অপরিবর্তিত, শুধু loading/pulse presentation যোগ হয়েছে। বাগ-১ ক্লাস রিস্ক: `rememberFieldChangePulse`-এর
   শেয়ার্ড `finally` ফিক্স ইতিমধ্যে আছে (সেশন ২.১ ভেরিফায়েড), এখানে নতুন কোনো লোকাল কাউন্টার তৈরি
   করা হয়নি।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: বিড
   বাতিলের ইতিহাস ট্যাবে re-entry/pull-to-refresh সম্পন্ন হলে/সার্চ-ফিল্টার-পেজ পাল্টালে শুধু বিড-কার্ড
   pulse করছে কিনা, সামারি/ফ্রিকোয়েন্ট-ক্যানসেলার অংশ ও pagination bar স্থির থাকছে কিনা।

### সংশোধনী/সংযোজন (ব্যবহারকারীর অনুরোধে, ২.১১ সেশনের পরপরই)
ব্যবহারকারী জানিয়েছেন "ঘন ঘন বাতিলকারী" (টপ-৩ হাইলাইট) অংশও একইভাবে re-entry/pull-to-refresh-এ
pulse করা দরকার (প্রথম ভিজিটে পুরো ট্যাবের cold-load skeleton আগে থেকেই আছে, সেটা অপরিবর্তিত)।
Dashboard-এর stat-card/category-list প্যাটার্নের মতো (একই `sessionKey`, ভিন্ন `value`, দুটো আলাদা
`rememberFieldChangePulse` কল) প্রয়োগ করা হয়েছে:
- নতুন `frequentCancellersPulse = rememberFieldChangePulse(value = top3Cancellers,
  isManualRefreshing = isManualRefreshing, sessionKey = "admin_cancelled_bids_sync", viewModel =
  viewModel)`।
- "🚨 সর্বোচ্চ বাতিলকারী সলভার (টপ ৩)" হাইলাইট Column (তিনটা সারি) `PulsingValue(isUpdating =
  frequentCancellersPulse) { ... }` দিয়ে মোড়ানো হয়েছে।
- **স্কোপ (অনুমান, ব্যবহারকারী থেকে আলাদাভাবে কনফার্ম না করা):** শুধু এই টপ-৩ হাইলাইট অংশ pulse
  করবে। সামারি-সংখ্যা বক্স ("মোট বাতিল"/"বারবার বাতিলকারী" গণনা) এবং "সব দেখুন" মোডালের ভেতরের
  পূর্ণ তালিকা (নিজস্ব `modalVisibleCount` লোড-মোর সহ) এই সংযোজনের বাইরে রাখা হয়েছে — যদি ব্যবহারকারী
  মোডালের তালিকাতেও pulse চান, পরের সেশনে জানালে যোগ করা যাবে।
- brace-balance পুনরায় গোনা হয়েছে: `{}` ২০৮=২০৮, `()` ৬০৩=৬০৩। ঠিক ২টা `PulsingValue` কল এখন এই
  ফাইলে (বিড-তালিকা + টপ-৩ হাইলাইট)।

### সংশোধনী/সংযোজন #২ (ব্যবহারকারীর অনুরোধে)
ব্যবহারকারী আরও দুটো জিনিস চেয়েছেন:
১. "সব দেখুন" মোডালের তালিকাও pulse করবে।
২. ৩টা সামারি-সংখ্যা বক্সে **পুরো বক্স না, শুধু সংখ্যার ভ্যালুটাই** pulse করবে (লেবেল টেক্সট/বক্সের
   ব্যাকগ্রাউন্ড অপরিবর্তিত)।

ফিক্স:
- **মোডাল তালিকা:** নতুন `modalCancellersPulse = rememberFieldChangePulse(value =
  displayedModalCancellers, isManualRefreshing = isManualRefreshing, sessionKey =
  "admin_cancelled_bids_sync", viewModel = viewModel)` — মোডালের `if (showFrequentCancellersModal)`
  ব্লকের ভেতরে (KYC/অন্যান্যদের মতোই conditional remember, Compose-এ নিরাপদ)। মোডালের
  `items(displayedModalCancellers, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating =
  modalCancellersPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — সার্চ/লোড-মোর/ডেটা-বদল, সবই ট্রিগার
  করবে (মোডাল হেডার/সার্চ বার/লোড-মোর বাটন অপরিবর্তিত)।
- **সামারি সংখ্যা (৩টা বক্স):** নতুন `summaryCountsPulse = rememberFieldChangePulse(value =
  listOf(cancelledBids.size, solverCancellationCounts.size, frequentCancellers.size),
  isManualRefreshing = isManualRefreshing, sessionKey = "admin_cancelled_bids_sync", viewModel =
  viewModel)` (Dashboard-এর `statsPulse`-এর মতো একাধিক সংখ্যা একটা list-এ)। প্রতিটা বক্সের ভ্যালু-
  `Text(...)`-কেই আলাদাভাবে `PulsingValue(isUpdating = summaryCountsPulse) { Text(...) }` দিয়ে
  মোড়ানো হয়েছে — লেবেল-`Text` ("মোট বাতিল বিড"/"বাতিলকারী"/"বারবার বাতিলকারী") আর বক্সের
  `Box(...)` (background/padding/clip) স্পর্শ করা হয়নি, শুধু ভেতরের সংখ্যাটাই pulse করবে।

যাচাই: brace-balance পুনরায় গোনা হয়েছে `{}` ২১২=২১২, `()` ৬১২=৬১২। এখন এই ফাইলে ঠিক ৬টা
`PulsingValue` কল (মূল বিড-তালিকা, টপ-৩ হাইলাইট, মোডাল-তালিকা, আর ৩টা সামারি-সংখ্যা আলাদাভাবে)।
`AdminCancelledBidsView(` কল-সাইট এখনো ঠিক একটাই। কোনো বিদ্যমান ফাংশনালিটি (সার্চ/ফিল্টার/
লোড-মোর/মোডাল-ওপেন-ক্লোজ) স্পর্শ করা হয়নি।

## ⏭️ সেশন ২.১২ — Supabase এক্সপ্লোরার (ইনডেক্স ১৭): ইচ্ছাকৃতভাবে বাদ (কোনো এডিট হয়নি)

### গ্রেপ-ভেরিফিকেশন
`AdminSupabaseExplorerView.kt` normal repository/viewModel sync flow (`initialSyncPhase`,
`SyncAwareContent`) ব্যবহার করে না — সরাসরি `SupabaseSyncManager.explorerFetchPage(...)` কল করে
(টেবিল বদলানো/রিফ্রেশ-ট্রিগার/লোড-মোরে), নিজস্ব লোকাল `isLoading`/`isLoadingMore`/`hasMore`/
`errorMessage` স্টেট দিয়ে। এই স্ক্রিনের ডেটা কখনোই Room/repository sync-এর অংশ না (raw
table-explorer, dev-diagnostics টুল)।

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
এই ট্যাব মাস্টার প্রম্পটের স্কোপ থেকে **ইচ্ছাকৃতভাবে বাদ** — নিজস্ব `isLoading`/`isLoadingMore`
প্যাটার্নই বহাল থাকবে, `SyncAwareContent`/`rememberFieldChangePulse`-এ migrate করা হবে না।

### ফলাফল
কোনো কোড এডিট হয়নি (`AdminSupabaseExplorerView.kt` বা `AdminPanelScreen.kt`-এর ইনডেক্স ১৭ ব্লক,
কোনোটাই স্পর্শ করা হয়নি) — শুধু এই progress ফাইল আপডেট।

## ✅ সেশন ২.১৩ — রেপুটেশন ইঞ্জিন (ইনডেক্স ২০) migrate (সম্পূর্ণ)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
সেটিংস (২.৮)-এর মতো ক্যাটেগরি E (এডিট-ফর্ম ব্যতিক্রম) — শুধু rule ১ (cold-load), কোনো
pulse/diff-শিমার (rule ২) না। pull-to-refresh দরকার (গ্লোবাল `SomadhanPullToRefresh` wrapper এই
ট্যাবে বাদ দেওয়া হবে না) — কিন্তু যেহেতু কোনো pulse consumer-ই নেই, তাই এটা প্রয়োগে কোনো নতুন
`isManualRefreshing` ওয়্যারিং লাগেনি (গ্লোবাল wrapper আগে থেকেই পুরো `when` ব্লক ঢেকে রাখে,
এই ট্যাবের জন্য আলাদা কিছু disable করা হয়নি, এটাই "বাদ দেওয়া হয়নি"-র প্রয়োগ)।

### গ্রেপ-ভেরিফিকেশন
`AdminReputationEngineView.kt`-এর মূল অংশ (৮০+ platformSettings-ভিত্তিক স্কোর/ক্যাপ ইনপুট ফিল্ড)
active edit-form — AdminSettingsView-এর মতোই কাঠামো। ভেতরে দুটো tab-list-ও আছে (সাজেস্টেড ইভেন্ট
`items(filteredSuggestions...)`, কাস্টম ইভেন্ট `items(customEventKeys...)`) কিন্তু দুটোই
ফর্ম-ইনপুট-চালিত (প্রতিটা কার্ডে নিজস্ব স্কোর/ক্যাপ ইনপুট ফিল্ড আর অ্যাড/রিমুভ বাটন) — তাই এগুলোও
Settings-এর সিদ্ধান্তের আওতায় (background diff-pulse যোগ করা হয়নি, ইনপুট-বিঘ্নের ঝুঁকি এড়াতে)।

### ফিক্স
- **`AdminPanelScreen.kt` (ইনডেক্স ২০):** `SyncAwareContent` (rule ১ শুধু, `data`-বিহীন) দিয়ে
  wrap করা হয়েছে, `sessionKey = "admin_reputation_engine_sync"`, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminReputationEngineView`-এ কল অপরিবর্তিত
  (`viewModel` — আগে থেকেই ছিল)।
- **`AdminReputationEngineView.kt`:** কোনো এডিট করা হয়নি — সেটিংস (২.৮)-এর মতোই কোনো
  pulse/diff-শিমার লজিক এই স্ক্রিনে যোগ করা হয়নি বলে এই ফাইল স্পর্শ করারই প্রয়োজন হয়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. `{}`/`()` ব্যালেন্স গোনা হয়েছে: `AdminPanelScreen.kt` `{}` ২৩২=২৩২, `()` ৪৯৪=৪৯৪।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminReputationEngineView(` কল-সাইট এখনো ঠিক একটাই।
3. `AdminReputationEngineView.kt` এই সেশনে ছোঁয়া হয়নি, শুধু `AdminPanelScreen.kt` + এই progress
   ফাইল।
4. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — স্কোর/ক্যাপ সেভ, সাজেস্টেড-ইভেন্ট অ্যাক্টিভেট, কাস্টম-ইভেন্ট
   এডিট/রিমুভ কোনো action-এর লজিক স্পর্শ করা হয়নি, শুধু cold-load presentation যোগ হয়েছে। এখানে কোনো
   `rememberFieldChangePulse` নেই বলে stuck-shimmer ক্লাস রিস্কই প্রযোজ্য না।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮টা, dotfile সহ) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: রেপুটেশন
   ইঞ্জিন ট্যাবে প্রথমবার ঢুকলে skeleton flash হচ্ছে কিনা, তারপর ফর্মে টাইপ করার মাঝখানে কোনো
   background-refresh/pull-to-refresh-এ ফর্ম flash/disrupt হচ্ছে না সেটা, আর স্কোর/ক্যাপ সেভ ও
   ইভেন্ট অ্যাড/রিমুভ action-গুলো আগের মতোই কাজ করছে কিনা।

## পরবর্তী সেশন
সাজেস্টেড অর্ডার অনুযায়ী পরের ধাপ: **২.১৪ (রিফান্ড ডায়াগনস্টিক, ইনডেক্স ২৪)** — dev-tool, ২.১২
(Supabase এক্সপ্লোরার)-এর মতো একই স্কোপ-প্রশ্ন আগে জিজ্ঞাসা করতে হবে (মাস্টার প্রম্পটের স্কোপে রাখা
দরকার নাকি ইচ্ছাকৃতভাবে বাদ), আর এই স্ক্রিনের নিজস্ব `isLoading` প্যাটার্নটাও আগে গ্রেপ করে দেখে নিতে
হবে। এটা শেষ হলে ক্যাটেগরি ৩-এর ১২টা tab migration সম্পূর্ণ হবে, তারপর ২.১৫-২.২৫ (ক্যাটেগরি ১-এর
১১টা retrofit) শুরু হবে।

## ⏭️ সেশন ২.১৪ — রিফান্ড ডায়াগনস্টিক (ইনডেক্স ২৪): ইচ্ছাকৃতভাবে বাদ (কোনো এডিট হয়নি)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
কোনো initial sync/cold-load নেই — একটা Problem ID টাইপ করে বাটন চাপলে তবেই ডায়াগনস্টিক চলে, কোনো
viewModel/initialSyncPhase নেয় না এই স্ক্রিনে। ২.১২ (Supabase এক্সপ্লোরার)-এর মতো একই সিদ্ধান্ত:
এই ট্যাব মাস্টার প্রম্পটের স্কোপ থেকে **ইচ্ছাকৃতভাবে বাদ** — নিজস্ব প্যাটার্নই বহাল থাকবে।

### ফলাফল
কোনো কোড এডিট হয়নি (`AdminRefundDebugView.kt` বা `AdminPanelScreen.kt`-এর ইনডেক্স ২৪ ব্লক, কোনোটাই
স্পর্শ করা হয়নি) — শুধু এই progress ফাইল আপডেট। ক্যাটেগরি ৩-এর ১২টা tab migration এখন সম্পূর্ণ
(১০টা migrate + ২টা ইচ্ছাকৃতভাবে বাদ: ২.১২ Supabase এক্সপ্লোরার, ২.১৪ রিফান্ড ডায়াগনস্টিক)।

## ✅ সেশন ২.১৫ — উইথড্রয়াল (ইনডেক্স ২): Ground Rule ১৮ retrofit (সম্পূর্ণ)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
- প্রথম ভিজিট (cold-load): পুরো ট্যাব full skeleton/shimmer (rule ১৮-এর ব্যতিক্রম না, অপরিবর্তিত)।
- re-entry: পুরো তালিকা flash না — শুধু কার্ডগুলো pulse করবে।
- ফিল্টার/সার্চ পরিবর্তন: শুধু কার্ডগুলো pulse করবে।
- pagination: শুধু কার্ডগুলো pulse করবে।
- হেডার, সামারি/স্ট্যাট কার্ড (পেন্ডিং/সম্পন্ন/প্রত্যাখ্যাত কাউন্ট), ফিল্টার বার, pagination bar —
  স্থির, কখনো pulse-এর অংশ না।

### ফিক্স
- **`AdminPanelScreen.kt` (ইনডেক্স ২):** `SyncAwareRefreshableContent` (যা আগে
  `data = listOf(allWithdrawals, allUsers)` পাস করে পুরো তালিকা+সামারি একসাথে flash করাতো) সরিয়ে
  plain `SyncAwareContent` (rule ১+২ cold-load শুধু, `data`-বিহীন) দিয়ে wrap করা হয়েছে, `sessionKey`
  ("admin_withdrawals_sync") অপরিবর্তিত, skeleton `ListScreenSkeleton(tint = SomadhanAdminSlate)`।
  `AdminWithdrawalsView(` কল-সাইটে নতুন `isManualRefreshing = isRefreshing` প্যারামিটার যোগ হয়েছে।
  পুল-টু-রিফ্রেশ গ্লোবাল `SomadhanPullToRefresh` wrapper থেকেই আসে (২.১৩-এর মতো), এখানে আলাদা করে
  বাদ দেওয়া হয়নি।
- **`AdminWithdrawalsView.kt`:** নতুন `isManualRefreshing: Boolean = false` প্যারামিটার যোগ হয়েছে।
  KYC (২.৩)-এর রেফারেন্স প্যাটার্ন অনুসরণ করে নতুন `withdrawalsListPulse = rememberFieldChangePulse(
  value = paginatedWithdrawals, isManualRefreshing = isManualRefreshing, sessionKey =
  "admin_withdrawals_sync", viewModel = viewModel)` যোগ করা হয়েছে (`listState` ডিক্লারেশনের ঠিক পরে)
  — `paginatedWithdrawals`-কেই value হিসেবে দেওয়ায় ফিল্টার/সার্চ পরিবর্তন, pagination, এবং আসল ডেটা
  বদল তিনটাই একই pulse ট্রিগার করে; `flashOnReentry` (ডিফল্ট true) নিজেই re-entry pulse দেয়।
  `items(paginatedWithdrawals, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating =
  withdrawalsListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু কার্ড, সামারি কাউন্ট কার্ড/ফিল্টার
  বার/pagination bar স্পর্শ করা হয়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. brace/paren-balance গোনা হয়েছে (মূল আপলোডের সাথে তুলনা করে): `AdminWithdrawalsView.kt` `{}`
   ২৯৭→২৯৮ (+১, `PulsingValue` wrap), `()` ৭৫১→৭৫৯ (+৮, নতুন param + `rememberFieldChangePulse` কল)।
   `AdminPanelScreen.kt` `{}` ২৩২→২৩৩ (+১), `()` ৪৯৪→৫০১ (+৭)। উভয় ফাইলেই `{`/`}` এবং `(`/`)` কাউন্ট
   একে অপরের সমান।
2. `AdminWithdrawalsView(` কল-সাইট এখনো ঠিক একটাই (`AdminPanelScreen.kt`-এ grep-ভেরিফাইড)।
3. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — সার্চ/ফিল্টার, bulk-approve/reject, single approve/reject,
   TrxID এডিট, বিস্তারিত মোডাল, pagination — কোনো action-এর লজিক স্পর্শ করা হয়নি, শুধু cold-load +
   pulse presentation যোগ হয়েছে।
4. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮/২৪৮) আগের আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে (কোনো ফাইল
   যোগ/বাদ হয়নি, শুধু এই দুটো ফাইল + এই progress ফাইল বদলেছে)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: প্রথমবার
   উইথড্রয়াল ট্যাবে ঢুকলে পুরো ট্যাব skeleton হচ্ছে কিনা, তারপর re-entry-তে (ট্যাব বদলে আবার ফিরে
   এসে) পুরো ট্যাব flash না হয়ে শুধু কার্ড pulse করছে কিনা, স্ট্যাটাস ফিল্টার/সার্চ পরিবর্তনে ও
   পেজিনেশন বাটনে একই কার্ড-pulse হচ্ছে কিনা (হেডার/সামারি কাউন্ট/ফিল্টার বার স্থির থাকছে কিনা), আর
   approve/reject/bulk-approve/TrxID-এডিট action-গুলো আগের মতোই কাজ করছে কিনা (কোনো stuck-shimmer
   না)।

## 🐛 সিস্টেমিক বাগ ফিক্স (২.১৫-এর পর রিপোর্ট করা) — App-wide "re-entry-তে শিমার আটকে যাওয়া"

### লক্ষণ (ব্যবহারকারীর রিপোর্ট)
উইথড্রয়াল ট্যাবে মাঝে মাঝে re-entry-তে শিমার আটকে যাচ্ছিল, পেজ খুলছিল না; অন্য কোনো ট্যাব ঘুরে
আবার re-entry করলে ঠিক হয়ে যাচ্ছিল। শুধু withdrawal না — **সব ট্যাবেই/পেজেই** একই লক্ষণ।

### রুট-কজ ডায়াগনসিস
`MotionToolkit.kt`-এর `SyncAwareContent` আর `SyncAwareRefreshableContent` দুটোতেই (হুবহু ডুপ্লিকেট
প্যাটার্ন) একটা `DisposableEffect(sessionKey, lifecycleOwner)` আছে যা `ON_RESUME` lifecycle event
শুনে — [syncPhase] তখন `ERROR` থাকলে — স্বয়ংক্রিয়ভাবে `onRetry()` কল করে (উদ্দেশ্য: কোনো স্ক্রিন
থেকে বেরিয়ে genuinely আবার ঢুকলে auto-retry)। সমস্যা: AndroidX Lifecycle-এর নিজস্ব আচরণ অনুযায়ী,
lifecycle ইতিমধ্যে `RESUMED` অবস্থায় থাকা অবস্থায় একটা *নতুন* `LifecycleEventObserver`
`addObserver()` করা হলে, সেটাকে বর্তমান state পর্যন্ত "catch up" করাতে `ON_CREATE`/`ON_START`/
`ON_RESUME` synchronously রিপ্লে করে দেওয়া হয় — even যদি ব্যবহারকারী আদৌ backgrounded/foregrounded
না করে থাকেন। যেহেতু এই `DisposableEffect` প্রতিবার সেই ট্যাবের composable ফ্রেশ mount হলেই (ট্যাব
বদলে আবার সেই ট্যাবে ফেরা সহ — ঠিক এটাই এই প্রজেক্টে "re-entry") নতুন করে রেজিস্টার হয়, প্রতিটা
re-entry-তেই এই *synthetic* `ON_RESUME` ফায়ার হতো। [syncPhase] তখন `ERROR` থাকলে (কোনো আগের
transient network hiccup-এর কারণে) সাথে সাথে `onRetry()` কল হয়ে যেত — আর `initialSyncPhase`
(bulk-pull-নির্ভর প্রায় সব ট্যাবই এটা শেয়ার করে) **সম্পূর্ণ app-wide শেয়ার্ড** একটা `StateFlow`
(`SupabaseRealtimeManager`), তাই সেই retry `_initialSyncPhase.value = LOADING` সেট করে দিত সবার
জন্য একসাথে — ফলে সেই মুহূর্তে composed থাকা *প্রতিটা* `SyncAwareContent`/`SyncAwareRefreshableContent`
স্ক্রিনই (শুধু যেই ট্যাবে re-entry হয়েছিল সেটাই না) skeleton-এ আটকে যেত, যতক্ষণ না নতুন bulk-pull
সম্পন্ন হয় (২০ সেকেন্ড টাইমআউট পর্যন্ত, নেটওয়ার্ক আবার fail করলে আরও বেশি — প্রতিটা পরবর্তী
re-entry-ও নতুন করে ট্রিগার করতে পারতো)। "অন্য পেজ ঘুরে re-entry করলে ঠিক হয়ে যায়" — কারণ ততক্ষণে
আগের (বা কোনো) retry সম্পন্ন হয়ে `LOADED`-এ পৌঁছে যায়।

### ফিক্স
`MotionToolkit.kt`-এর দুটো জায়গাতেই (`SyncAwareContent` লাইন ~৭৪৭, `SyncAwareRefreshableContent`
লাইন ~৮৬৯) `DisposableEffect`-এর ভেতরে একটা `hasSkippedSynthenticInitialResume` ফ্ল্যাগ যোগ করা
হয়েছে — observer রেজিস্টার হওয়ার পর *প্রথম* `ON_RESUME` (যেটা synthetic replay হতে পারে) স্কিপ করা
হয়, শুধু তার *পরের* `ON_RESUME` (যার আগে সত্যিকার `ON_PAUSE` ঘটেছে — অর্থাৎ প্রকৃত ব্যাকগ্রাউন্ড/
ফোরগ্রাউন্ড বা genuine navigation pause/resume) থেকেই auto-retry ট্রিগার হবে। এটা `SyncErrorState`-এর
ম্যানুয়াল "আবার চেষ্টা করুন" বাটনকে স্পর্শ করেনি — সেটা এখনো সবসময় কাজ করবে; শুধু *automatic*
resume-triggered retry-টাই এখন প্রকৃত resume-এর জন্যই সংরক্ষিত।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. brace/paren-balance গোনা হয়েছে: `MotionToolkit.kt` `{}` ১২৫→১২৯ (+৪, দুই জায়গায় নতুন
   `if/else if` ব্লক করে ২টা করে বাড়তি ব্রেস), `()` ৪৮৯→৫০১ (+১২)। উভয়ই সমান-সমান মিলেছে।
2. `Crossfade`/ফাংশনের বাকি অংশ অপরিবর্তিত আছে তা diff দিয়ে নিশ্চিত করা হয়েছে (শুধু দুটো
   `DisposableEffect` ব্লকের ভেতরের লজিক বদলেছে, বাকি পুরো ফাইল অক্ষত)।
3. এই ফিক্স শেয়ার্ড কম্পোনেন্টে বসানো বলে অ্যাপের প্রতিটা `SyncAwareContent`/
   `SyncAwareRefreshableContent`-ব্যবহারকারী স্ক্রিনেই (admin panel-সহ user/solver সাইডের ৩৫+
   স্ক্রিন) স্বয়ংক্রিয়ভাবে প্রযোজ্য হবে — কোনো individual স্ক্রিন ফাইল স্পর্শ করতে হয়নি।
4. drawer group ম্যাপিং/navigation/কোনো action-এর লজিক স্পর্শ করা হয়নি — শুধু auto-retry-এর
   *ট্রিগার-শর্ত* বদলেছে।
5. zip-এর ফাইল-সংখ্যা (২৪৮/২৪৮) পুনরায় মিলিয়ে দেখা হয়েছে (শুধু `MotionToolkit.kt` + এই progress
   ফাইল বদলেছে, ২.১৫-এর `AdminWithdrawalsView.kt`/`AdminPanelScreen.kt` এডিট অক্ষত)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না, আর এই বাগটা মূলত network-condition
   -নির্ভর বলে সবসময় reproduce নাও হতে পারে।** ব্যবহারকারীকে দেখতে হবে: এখন থেকে শিমার আটকে থাকা/
   পেজ না-খোলা লক্ষণ (যেকোনো ট্যাবে) আর দেখা যাচ্ছে কিনা, আর কোনো স্ক্রিনে genuinely `ERROR` অবস্থা
   হলে "আবার চেষ্টা করুন" বাটন এখনো ঠিকমতো কাজ করছে কিনা (ম্যানুয়াল রিট্রাই পুরোপুরি অক্ষত আছে)।

## ✅ সেশন ২.১৬ — ইউজারগণ (ইনডেক্স ৪): Ground Rule ১৮ retrofit (সম্পূর্ণ)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
শুধু ইউজার তালিকার কার্ডগুলো pulse করবে (২.১৫-এর মতোই) — হেডার, সার্চ বার, রোল-ফিল্টার চিপ,
pagination bar স্থির থাকবে, কখনো pulse-এর অংশ না। (রেপুটেশন হিস্ট্রি মোডালের তালিকা এই স্কোপের
বাইরে — সেটা আলাদা on-demand modal, মূল কোল্ড-লোড/re-entry flow-এর অংশ না।)

### ফিক্স
- **`AdminPanelScreen.kt` (ইনডেক্স ৪):** `SyncAwareRefreshableContent` (যা আগে `data = allUsers`
  পাস করে পুরো তালিকা একসাথে flash করাতো) সরিয়ে plain `SyncAwareContent` (rule ১+২ cold-load শুধু,
  `data`-বিহীন) দিয়ে wrap করা হয়েছে, `sessionKey` ("admin_users_sync") অপরিবর্তিত, skeleton
  `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminUsersView(` কল-সাইটে নতুন
  `isManualRefreshing = isRefreshing` প্যারামিটার যোগ হয়েছে। পুল-টু-রিফ্রেশ গ্লোবাল
  `SomadhanPullToRefresh` wrapper থেকেই আসে (২.১৫-এর মতো), এখানে আলাদা করে বাদ দেওয়া হয়নি।
- **`AdminUsersView.kt`:** নতুন `isManualRefreshing: Boolean = false` প্যারামিটার যোগ হয়েছে।
  উইথড্রয়াল (২.১৫)-এর রেফারেন্স প্যাটার্ন অনুসরণ করে নতুন `usersListPulse =
  rememberFieldChangePulse(value = paginatedUsers, isManualRefreshing = isManualRefreshing,
  sessionKey = "admin_users_sync", viewModel = viewModel)` যোগ করা হয়েছে (`listState`
  ডিক্লারেশনের ঠিক পরে) — সার্চ/রোল-ফিল্টার/রেপুটেশন-সর্ট পরিবর্তন, pagination, এবং আসল ডেটা
  (`users`) বদল তিনটাই একই pulse ট্রিগার করে; `flashOnReentry` (ডিফল্ট true) নিজেই re-entry pulse
  দেয়। `items(paginatedUsers, ...)`-এর ভেতরের `Card(...)`-কে `PulsingValue(isUpdating =
  usersListPulse) { Card(...) }` দিয়ে মোড়ানো হয়েছে — শুধু কার্ড, হেডার/সার্চ বার/রোল-ফিল্টার
  চিপ/pagination bar স্পর্শ করা হয়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. brace/paren-balance গোনা হয়েছে: `AdminUsersView.kt` `{}` ২৮১=২৮১, `()` ৬৫২=৬৫২ (উভয়ই
   সমান-সমান মিলেছে)। `AdminPanelScreen.kt` `{}` ২৩৩=২৩৩, `()` ৫০৭=৫০৭ (উভয়ই সমান-সমান মিলেছে)।
2. grep দিয়ে নিশ্চিত করা হয়েছে `AdminUsersView(` কল-সাইট এখনো ঠিক একটাই (`AdminPanelScreen.kt`),
   আর ঠিক ১টা `PulsingValue(isUpdating = usersListPulse)`।
3. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — সার্চ/ফিল্টার/সর্ট, ব্যান/রেস্ট্রিক্ট টগল, ব্যালেন্স
   অ্যাডজাস্ট, রোল-পরিবর্তন, রেপুটেশন-অ্যাডজাস্ট, পাসওয়ার্ড-রিসেট, ডিলিট, রেপুটেশন-হিস্ট্রি
   নেভিগেশন — কোনো action-এর লজিক স্পর্শ করা হয়নি, শুধু cold-load + pulse presentation যোগ হয়েছে।
   বাগ-১ ক্লাস রিস্ক (stuck shimmer): `rememberFieldChangePulse`-এর শেয়ার্ড `finally` ফিক্স
   ইতিমধ্যে আছে, এখানে নতুন কোনো লোকাল কাউন্টার তৈরি করা হয়নি।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি, শুধু sessionKey/wrapper।
5. zip-এর ফাইল-সংখ্যা (২৪৮/২৪৮) মূল আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে (কোনো ফাইল
   যোগ/বাদ হয়নি, শুধু `AdminUsersView.kt` + `AdminPanelScreen.kt` + এই progress ফাইল বদলেছে)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: প্রথমবার
   ইউজারগণ ট্যাবে ঢুকলে পুরো ট্যাব skeleton হচ্ছে কিনা, তারপর re-entry-তে (ট্যাব বদলে আবার ফিরে
   এসে) পুরো ট্যাব flash না হয়ে শুধু কার্ড pulse করছে কিনা, সার্চ/রোল-ফিল্টার/রেপুটেশন-সর্ট
   পরিবর্তনে ও পেজিনেশন বাটনে একই কার্ড-pulse হচ্ছে কিনা (হেডার/সার্চ বার/ফিল্টার চিপ স্থির থাকছে
   কিনা), আর ব্যান/রেস্ট্রিক্ট/ব্যালেন্স/রোল/রেপুটেশন/পাসওয়ার্ড/ডিলিট action-গুলো আগের মতোই কাজ
   করছে কিনা (কোনো stuck-shimmer না)।

## পরবর্তী সেশন
এখন ২.১৭ (সমস্যাসমূহ, ইনডেক্স ৫) — ক্যাটেগরি ১-এর পরবর্তী retrofit, একই টেমপ্লেট অনুসরণ করে, কিন্তু
শুরুর আগে ব্যবহারকারীর কাছ থেকে এই ট্যাবের জন্য ঠিক কোন অংশ(গুলো) pulse করবে তা নতুন করে কনফার্ম
করে নিতে হবে (Ground Rule ১৬, আগের কোনো ট্যাবের সিদ্ধান্ত ধরে নেওয়া যাবে না)।

## ✅ সেশন ২.১৬ রেট্রোফিট — Users (ইনডেক্স ৪): whole-list pulse → per-item pulse (সম্পূর্ণ)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড, ২.১৭ শুরুর আগে)
চ্যাট-ডিসকাশনে ঠিক হয়েছে: এখন থেকে action-এর পর যে pulse হবে সেটা যেন শুধু যে নির্দিষ্ট
ইউজারের ডেটা সত্যিই বদলেছে তার কার্ডই pulse করে (পুরো তালিকা একসাথে না), আর স্ক্রল করে নতুন কার্ড
viewport-এ আসলে সেটা pulse করবে না (per-item সরানোর ফলে যে সাইড-ইফেক্টটা আলোচনা হয়েছিল)। এই
per-item + no-scroll-pulse ডিজাইন এখন থেকে **নতুন স্ট্যান্ডার্ড** — Users, KYC, Withdrawals, আর
ভবিষ্যতের সব migration সেশনেই প্রযোজ্য।

### ফিক্স
`AdminUsersView.kt`-এ আগের single whole-list `usersListPulse = rememberFieldChangePulse(value =
paginatedUsers, ...)` (ফাংশনের উপরে, একবার কল) সরিয়ে `items(paginatedUsers, key = { it.id }) {
user -> ... }`-এর ভেতরে per-item নিয়ে যাওয়া হয়েছে — `value = user` (পুরো লিস্ট না)। `UserEntity`
data class বলে equals-ভিত্তিক তুলনায় শুধু যে ইউজারের ডেটা সত্যিই বদলেছে তার কার্ডই এখন pulse করবে।
স্ক্রল-pulse সাইড-ইফেক্ট এড়াতে per-item কলে `flashOnReentry = false` দেওয়া হয়েছে — sessionKey/
viewModel এখনো পাস করা হচ্ছে (consistency-র জন্য) কিন্তু `flashOnReentry = false` থাকায়
`rememberFieldChangePulse`-এর ভেতরের `LaunchedEffect(Unit) { if (flashOnReentry && isReentryVisit)
... }` ব্লকটা per-item কখনো ট্রিগার হবে না — তাই কার্ড নতুন করে compose হলে (স্ক্রল করে) কোনো
mount-pulse হবে না, শুধু `LaunchedEffect(value)`-এর genuine data-change pulse আর
`isManualRefreshing` সম্পন্ন হওয়ার pulse কাজ করবে (এই দুটো per-item cause এখনো সক্রিয়)।
`isManualRefreshing` per-item এখনো পাস করা হচ্ছে — pull-to-refresh সম্পন্ন হলে ইচ্ছাকৃতভাবে
সবগুলো কার্ডই একসাথে pulse করবে (এটা action-change সমস্যা না, বরং ইচ্ছাকৃত "রিফ্রেশ সম্পন্ন"
ফিডব্যাক, তাই এখানে বদলানো হয়নি)।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. brace/paren-balance গোনা হয়েছে: `AdminUsersView.kt` `{}` ২৮২=২৮২, `()` ৬৫৭=৬৫৭ (সমান-সমান)।
2. grep দিয়ে নিশ্চিত করা হয়েছে পুরনো `usersListPulse` নামের কোনো রেফারেন্স আর অবশিষ্ট নেই, আর
   ঠিক ১টা `PulsingValue(isUpdating = ...)` কল আছে (এখন `userCardPulse`, per-item scope-এ)।
   `AdminUsersView(` কল-সাইট (`AdminPanelScreen.kt`) স্পর্শ করা হয়নি, এখনো ঠিক একটাই।
3. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — শুধু pulse-এর granularity (whole-list → per-item) আর
   reentry-trigger বদলেছে, action/সার্চ/ফিল্টার/pagination লজিক অক্ষত।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮/২৪৮) মিলিয়ে দেখা হয়েছে (শুধু `AdminUsersView.kt` + এই progress ফাইল
   বদলেছে)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: একজন
   ইউজারের ব্যান/রেস্ট্রিক্ট/ব্যালেন্স/রোল/রেপুটেশন/পাসওয়ার্ড/ডিলিট action চাপলে শুধু *সেই* কার্ডটাই
   pulse করছে কিনা (বাকি কার্ড স্থির), আর তালিকায় নিচের দিকে স্ক্রল করলে নতুন কার্ড pulse ছাড়াই
   দেখা দিচ্ছে কিনা (কোনো hop-back-to-top বা re-load-দেখানো আচরণ না)।

### পরবর্তী সেশন
এখন ২.১৭ (সমস্যাসমূহ, ইনডেক্স ৫) — এই নতুন per-item + no-scroll-pulse ডিজাইনেই প্রথম থেকে করা
হবে, শুরুর আগে এই ট্যাবে ঠিক কোন অংশ(গুলো) pulse করবে তা কনফার্ম করে নিতে হবে (Ground Rule ১৬)।

## ✅ সেশন ২.১৭ রেট্রোফিট — KYC (ইনডেক্স ১): whole-visible-list pulse → per-item pulse (সম্পূর্ণ)

### গ্যাপ ধরা পড়েছিল, এই সেশনেই ঠিক করা হয়েছে
আগের সেশনের (২.১৬) শেষে ব্যবহারকারী ধরিয়ে দেন — (ক) per-item + no-scroll-pulse "নতুন স্ট্যান্ডার্ড"
কথাটা শুধু ২.১৬-এর progress-note এন্ট্রির ভেতরে চাপা ছিল, `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ
Ground Rule হিসেবে যোগ করা হয়নি; (খ) ব্যবহারকারী "user+kyc+withdrawal" তিনটাকেই একসাথে নতুন
স্ট্যান্ডার্ডের প্রথম প্রয়োগ হিসেবে বলেছিলেন, কিন্তু ২.১৬-এ শুধু Users রেট্রোফিট হয়ে ২.১৭-এ চলে
যাওয়া হয়েছিল। এই সেশনে দুটোই ঠিক করা হয়েছে:
1. `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ নতুন **Ground Rule ১৯** যোগ করা হয়েছে (Ground Rule
   ১৮-এর ঠিক পরে) — per-item + `flashOnReentry = false` ডিজাইন এখন মাস্টার-প্রম্পট লেভেলে
   স্ট্যান্ডিং রুল, পরের কোনো সেশনকে আর progress-note স্ক্রল করে খুঁজতে হবে না।
2. KYC-কে (এই সেশনে) per-item-এ রেট্রোফিট করা হয়েছে। **Withdrawal ইচ্ছাকৃতভাবে এই সেশনে করা
   হয়নি** — ব্যবহারকারী স্পষ্টভাবে বলেছেন শুধু KYC এখন ঠিক করতে, Withdrawal **পরবর্তী Claude
   সেশন প্রথমেই** করবে (নিচে "পরবর্তী সেশন" দেখো — এটা এখন এই progress note-এই স্পষ্ট লেখা আছে,
   যাতে চ্যাট-হিস্ট্রি ছাড়াই পরের সেশন এটা জানতে পারে)।

### ফিক্স (KYC)
`AdminKycView.kt`-এ তিনটা ট্যাবের (Pending/Verified/Rejected — `selectedKycTab` ০/১/২) জন্য আগে
একটাই শেয়ার্ড `kycListPulse = rememberFieldChangePulse(value = selectedKycTab to visibleKycList,
...)` ছিল (ফাংশনের উপরে, একবার কল, `flashOnReentry` ডিফল্ট true) — কোনো একটা সলভারের
approve/reject/... action চাপলেই `visibleKycList` রেফারেন্স বদলে যেত বলে সেই ট্যাবের *সব* কার্ড
একসাথে pulse করতো, আর ট্যাব/pagination পাল্টালেও (value-তে `selectedKycTab` যুক্ত ছিল বলে) পুরো
নতুন পাতা pulse করতো। এখন তিনটা `items(paginated...Solvers, key = { it.id }) { solver -> ... }`
ব্লকের প্রতিটার ভেতরে আলাদা `rememberFieldChangePulse(value = solver, isManualRefreshing =
isManualRefreshing, sessionKey = "admin_kyc_sync", viewModel = viewModel, flashOnReentry = false)`
বসানো হয়েছে (Users/২.১৬-এর হুবহু টেমপ্লেট) — `UserEntity` data class বলে equals-ভিত্তিক
তুলনায় শুধু যে সলভারের ডেটা সত্যিই বদলেছে তার কার্ডই pulse করবে, ট্যাব/pagination পাল্টানোয় আর
পুরনো visible-list-pulse ট্রিগার হবে না (নতুন কার্ডগুলো fresh compose হয়, `flashOnReentry = false`
থাকায় সেগুলোতেও mount-pulse হয় না)। আগের `visibleKycList`/`kycListPulse` val দুটো সরিয়ে ফেলা
হয়েছে (আর কোথাও ব্যবহৃত হচ্ছিল না)।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. brace/paren-balance গোনা হয়েছে: `AdminKycView.kt` `{}` ৩৫৭=৩৫৭, `()` ৯৩৮=৯৩৮ (উভয়ই
   সমান-সমান মিলেছে)।
2. grep দিয়ে নিশ্চিত করা হয়েছে পুরনো `kycListPulse` নামের কোনো কোড-রেফারেন্স আর অবশিষ্ট নেই (শুধু
   ব্যাখ্যামূলক কমেন্টে ঐতিহাসিক উল্লেখ আছে), আর ঠিক ৩টা `PulsingValue(isUpdating = solverCardPulse)`
   কল আছে (Pending/Verified/Rejected — প্রতিটা ট্যাবে একটা)। `AdminKycView(` কল-সাইট
   (`AdminPanelScreen.kt`) স্পর্শ করা হয়নি, এখনো ঠিক একটাই।
3. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — approve/reject/সার্চ/সিলেক্ট-মোড/bulk-action/pagination/
   ট্যাব-সুইচ কোনো লজিক স্পর্শ করা হয়নি, শুধু pulse-এর granularity (whole-visible-list → per-item)
   আর reentry-trigger বদলেছে।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
5. zip-এর ফাইল-সংখ্যা (২৪৮/২৪৮) মিলিয়ে দেখা হয়েছে (শুধু `AdminKycView.kt` +
   `ADMIN_PANEL_LOADING_MASTER_PROMPT.md` + এই progress ফাইল বদলেছে)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: কোনো
   একটা সলভার approve/reject করলে শুধু *সেই* কার্ডটাই pulse করছে কিনা (একই ট্যাবের বাকি কার্ড
   স্থির), তালিকায় স্ক্রল করলে নতুন কার্ড pulse ছাড়াই দেখা দিচ্ছে কিনা, ট্যাব বদলালে
   (Pending↔Verified↔Rejected) বা pagination বাটনে পুরো নতুন পাতা flash না করছে কিনা, আর
   pull-to-refresh সম্পন্ন হলে দৃশ্যমান সব কার্ড ইচ্ছাকৃতভাবে একসাথে pulse করছে কিনা (এই একটা
   কারণ এখনো per-item cause হিসেবে সক্রিয় আছে, বাগ না)।

### পরবর্তী সেশন (পুরনো নোট, ২.২৭-এ সম্পন্ন হয়েছে নিচে দেখো)
~~Withdrawal (`AdminWithdrawalsView.kt`) এখনো পুরনো whole-list প্যাটার্নে আছে — per-item রেট্রোফিট
বাকি।~~ → নিচের ২.২৭ এন্ট্রি দেখো, এই কাজ সম্পন্ন হয়েছে।

## ✅ সেশন ২.২৭ রেট্রোফিট — Withdrawal (ইনডেক্স ২): whole-list pulse → per-item pulse (সম্পূর্ণ)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড)
শুধু উইথড্রয়াল কার্ডগুলো pulse করবে — ২.১৫-এ যেমন কনফার্ম হয়েছিল ঠিক সেই স্কোপ (সামারি/কাউন্ট
কার্ড, ফিল্টার বার, হেডার, pagination bar কখনো pulse-এর অংশ না), শুধু granularity বদলাচ্ছে
(whole-list → per-item), Ground Rule ১৬ অনুযায়ী চ্যাটে আলাদাভাবে জিজ্ঞাসা করে কনফার্ম করা হয়েছে।

### ফিক্স
`AdminWithdrawalsView.kt`-এ ফাংশনের উপরে একবার কল হওয়া পুরনো whole-list
`withdrawalsListPulse = rememberFieldChangePulse(value = paginatedWithdrawals, ...)` (আর তার
ব্যাখ্যামূলক কমেন্ট) সরিয়ে ফেলা হয়েছে। এর বদলে `items(paginatedWithdrawals, key = { it.id }) {
item -> ... }`-এর **ভেতরে** নতুন per-item `withdrawalCardPulse = rememberFieldChangePulse(value =
item, isManualRefreshing = isManualRefreshing, sessionKey = "admin_withdrawals_sync", viewModel =
viewModel, flashOnReentry = false)` বসানো হয়েছে (Users/KYC-এর হুবহু টেমপ্লেট, Ground Rule ১৯) —
`WithdrawalEntity` data class বলে equals-ভিত্তিক তুলনায় শুধু যে উইথড্রয়ালের ডেটা সত্যিই বদলেছে তার
কার্ডই pulse করবে। `flashOnReentry = false` থাকায় স্ক্রল করে নতুন কার্ড প্রথমবার compose হওয়ার
মুহূর্তে অনিচ্ছাকৃত স্ক্রল-pulse দেখাবে না। `PulsingValue(isUpdating = withdrawalsListPulse)` →
`PulsingValue(isUpdating = withdrawalCardPulse)`-এ বদলানো হয়েছে (একই একটা কল, শুধু ভ্যারিয়েবল
রেফারেন্স)। `isManualRefreshing` per-item এখনো পাস করা হচ্ছে — pull-to-refresh সম্পন্ন হলে
ইচ্ছাকৃতভাবে দৃশ্যমান সব কার্ড একসাথে pulse করবে (bug না, ইচ্ছাকৃত ফিডব্যাক, Users/KYC-এর মতোই)।

### যাচাই (ধাপ ৩ চেকলিস্ট)
1. brace/paren-balance গোনা হয়েছে: `AdminWithdrawalsView.kt` `{}` ২৯৮=২৯৮, `()` ৭৬০=৭৬০ (উভয়ই
   সমান-সমান মিলেছে)।
2. grep দিয়ে নিশ্চিত করা হয়েছে পুরনো `withdrawalsListPulse` নামের কোনো কোড-রেফারেন্স আর অবশিষ্ট
   নেই (শুধু ব্যাখ্যামূলক কমেন্টে ঐতিহাসিক উল্লেখ আছে), আর ঠিক ১টা `PulsingValue(isUpdating = ...)`
   কল আছে (এখন `withdrawalCardPulse`, per-item scope-এ)। `AdminWithdrawalsView(` কল-সাইট
   (`AdminPanelScreen.kt`, ইনডেক্স ২) স্পর্শ করা হয়নি, এখনো ঠিক একটাই।
3. কোনো বিদ্যমান ফাংশনালিটি বদলানো হয়নি — status update/approve/reject, bulk-approve dialog,
   select-mode, সার্চ, ফিল্টার, pagination কোনো লজিক স্পর্শ করা হয়নি, শুধু pulse-এর granularity
   (whole-list → per-item) আর reentry-trigger বদলেছে।
4b. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি — `AdminPanelScreen.kt` এই সেশনে একদমই এডিট
    হয়নি, শুধু `AdminWithdrawalsView.kt`।
5. zip-এর ফাইল-সংখ্যা (২৪৮/২৪৮) মূল আপলোড করা zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে (কোনো ফাইল
   যোগ/বাদ হয়নি, শুধু `AdminWithdrawalsView.kt` + এই progress ফাইল বদলেছে)।
6. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: কোনো
   একটা উইথড্রয়াল approve/reject/status-update করলে শুধু *সেই* কার্ডটাই pulse করছে কিনা (বাকি
   কার্ড স্থির), তালিকায় স্ক্রল করলে নতুন কার্ড pulse ছাড়াই দেখা দিচ্ছে কিনা, ফিল্টার/সার্চ/pagination
   বদলালে পুরো নতুন পাতা flash না করে শুধু কার্ড-লেভেলে বদলাচ্ছে কিনা, bulk-approve করলে ঠিক সেই
   সিলেক্টেড কার্ডগুলোই pulse করছে কিনা, আর pull-to-refresh সম্পন্ন হলে দৃশ্যমান সব কার্ড
   ইচ্ছাকৃতভাবে একসাথে pulse করছে কিনা।

### পরবর্তী সেশন (পুরনো নোট — ২.২৮-এর পর প্রায়োরিটি বদলেছে, নিচে দেখো)
~~Category ১-এর বাকি retrofit সেশনগুলো (২.১৮-২.২৫)...~~ → ২.২৮-এর ডায়াগনসিসের পর Ground Rule ২০
(filter-scope pulse + scroll-jump ফিক্স) সবচেয়ে বেশি অগ্রাধিকার পেয়েছে, নিচে ২.২৯-৩১ দেখো।

## ✅ সেশন ২.২৮ — ডায়াগনসিস: filter-scope pulse + scroll-jump বাগ (সম্পূর্ণ, কোনো কোড এডিট হয়নি)

### প্রেক্ষাপট
আগের একটা Claude সেশন `AdminWithdrawalsView.kt`-এ দুটো সমস্যা নিয়ে ডায়াগনসিস শুরু করেছিল কিন্তু
সেশনটা কোনো কোড এডিট ছাড়াই শেষ হয়ে যায় (unfinished)। এই সেশনে সেই ডায়াগনসিসটা কোড পড়ে নিজে
পুনরায় ভেরিফাই করা হলো, আর ব্যবহারকারী দুটোই কনফার্ম করেছেন — **কোনো কোড এডিট এই সেশনে হয়নি**,
শুধু ডায়াগনসিস + স্কোপ + অর্ডার কনফার্মেশন, প্রম্পট/progress ফাইল আপডেট।

### সমস্যা ১ — pulse scope (filter/tab বদলে সব visible কার্ড pulse করা উচিত)
Ground Rule ১৯-এর per-item ডিজাইনে (`rememberFieldChangePulse(value = item, ...)`) item-level
equality দিয়ে শুধু action-এ বদলানো item-ই ধরা পড়ে — ফিল্টার/সার্চ/ট্যাব বদলালে item নিজে বদলায়
না বলে কোনো pulse-ই হয় না (whole-list pulse বাদ দেওয়ার সময় এই কেসটা কভার হয়নি)। ব্যবহারকারীর
কনফার্মড সিদ্ধান্ত: filter/search/tab বদল → **visible সব কার্ড pulse করবে** (নতুন
`isFilterRefreshing`-জাতীয় ফ্ল্যাগ দরকার); pagination-only বদল (ফিল্টার অপরিবর্তিত) → কোনো
pulse না; single-item action → শুধু সেই কার্ড (Ground Rule ১৯ অপরিবর্তিত)। বিস্তারিত এখন
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ **Ground Rule ২০** হিসেবে যোগ করা হয়েছে।

### সমস্যা ২ — scroll-jump বাগ (আলাদা root cause, একই সেশনে ধরা পড়েছে)
তিনটা ফাইলেই (`AdminWithdrawalsView.kt` লাইন ৩৯৮-৩৯৯, `AdminUsersView.kt` লাইন ৩৮৪-৩৮৫,
`AdminKycView.kt` লাইন ৪১৯-৪২০) গ্রেপ করে দেখা গেছে — টপ-এ auto-scroll করা `LaunchedEffect` কী
হিসেবে coerced/clamped page ভ্যারিয়েবল (`safePage`/`safePendingPage`/`safeVerifiedPage`/
`safeRejectedPage`) ব্যবহার করছে, `currentPage`-এর মতো raw state না। কোনো item action-এ
(approve/reject) তালিকা থেকে সেই আইটেম বাদ পড়লে `totalPages` কমে যায় → coerced ভ্যারিয়েবল
স্বয়ংক্রিয়ভাবে ক্ল্যাম্প হয়ে বদলে যায় (ইউজার pagination না ছুঁলেও) → effect ভুলভাবে ট্রিগার হয়ে
জোর করে টপে scroll করিয়ে দেয়। ফিক্স: এই একটা effect-কে raw `currentPage` state-এ re-key করা
(pagination bounds-check-এর অন্য জায়গাগুলোতে `safePage` অপরিবর্তিত থাকবে) — কারণ raw state শুধু
দুই জায়গায় বদলায় (filter/search reset, explicit next/prev ক্লিক), action-এর side-effect-এ না।

### স্কোপ + অর্ডার (ব্যবহারকারীর কনফার্মড সিদ্ধান্ত)
তিনটা ট্যাবই (Users/ইনডেক্স ৪, KYC/ইনডেক্স ১, Withdrawal/ইনডেক্স ২) এই রেট্রোফিট দরকার — কোডে
তিনটাতেই একই দুই সমস্যা গ্রেপ-ভেরিফাই করা হয়েছে (উপরে)। ব্যবহারকারী স্পষ্ট করে বলেছেন **এই সেশনে
কোনো ট্যাবেই কোড এডিট না** — পরের Claude সেশন এই অর্ডারে একটা একটা করে করবে:
১. **Users (ইনডেক্স ৪)** — প্রথমে
২. **KYC (ইনডেক্স ১)** — Users শেষ ও কনফার্ম হওয়ার পর
৩. **Withdrawal (ইনডেক্স ২)** — সবশেষে

প্রতিটা টেমপ্লেট + চেকলিস্ট `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এর নতুন "সেশন ২.২৯ থেকে
২.৩১" অংশে লেখা হয়েছে (Ground Rule ২০-এর ঠিক নিচে)।

### এই সেশনে কী বদলেছে
শুধু দুটো ফাইল: `ADMIN_PANEL_LOADING_MASTER_PROMPT.md` (Ground Rule ২০ + সেশন ২.২৯-৩১ টেমপ্লেট +
ধাপ ৪ প্রায়োরিটি আপডেট যোগ) আর এই progress ফাইল (এই এন্ট্রি)। **কোনো `.kt` ফাইল স্পর্শ করা হয়নি।**
zip-এর ফাইল-সংখ্যা তাই মূল আপলোডের সাথেই মেলে (কোনো কোড ফাইল যোগ/বাদ/এডিট হয়নি), শুধু
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md` নতুন করে repo-root-এ যোগ হয়েছে (আগে এই zip-এ ছিল না,
আলাদাভাবে আপলোড করা হয়েছিল) আর progress ফাইল আপডেট হয়েছে।

### পরবর্তী সেশন
**সেশন ২.২৯ — Users (ইনডেক্স ৪) প্রথমে, Ground Rule ২০ অনুযায়ী** (filter-scope whole-visible
pulse + scroll-jump ফিক্স)। এর পর কনফার্মেশন নিয়ে ২.৩০ (KYC), তারপর ২.৩১ (Withdrawal) — একসাথে
সব না, একটা একটা করে।

## ✅ সেশন ২.২৮.১ — Ground Rule ২০ স্কোপ-আপডেট: pagination-ও এখন whole-visible pulse (সম্পূর্ণ, কোনো কোড এডিট হয়নি)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড, ২০২৬-০৯-১৫)
সেশন ২.২৮-এ Ground Rule ২০ যোগ হওয়ার সময় তিন ভাগ করা হয়েছিল — action trigger (শুধু সেই item),
filter/search/tab বদল (সব visible কার্ড pulse), আর শুধু-pagination (কোনো pulse না)। ব্যবহারকারী
এই তৃতীয় ভাগটা পাল্টে দিয়েছেন: **pagination বদলও এখন থেকে filter/search/tab-এর মতোই সব visible
কার্ড pulse করাবে** — আলাদা "pagination-only = no pulse" কেস আর নেই।

### যা বদলেছে
শুধু দুটো ফাইল, কোনো `.kt` ফাইল স্পর্শ হয়নি:
- `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ Ground Rule ২০-এর "ফিল্টার/সার্চ/ট্যাব বদল" আর "শুধু
  pagination" — এই দুটো আলাদা বুলেট এক করে দেওয়া হয়েছে (এখন "ফিল্টার/সার্চ/ট্যাব/pagination বদল")
  — `isFilterRefreshing`-জাতীয় ফ্ল্যাগের `LaunchedEffect` কী-তে এখন pagination-কীও থাকবে (আগে
  স্পষ্ট করে "থাকবে না" লেখা ছিল, সেটা উল্টে গেছে)। সেশন ২.২৯-৩১-এর টেমপ্লেটেও (ধাপ ১) একই
  সংশোধন করা হয়েছে।
- এই progress ফাইলে এই এন্ট্রি যোগ করা হয়েছে।

### পরবর্তী সেশন (অপরিবর্তিত অর্ডার)
সেশন ২.২৯ (Users) → ২.৩০ (KYC) → ২.৩১ (Withdrawal) — শুধু এখন প্রতিটাতে pagination বদলকেও
filter/search/tab-এর সমান ধরে কাজ করতে হবে (Ground Rule ২০-এর আপডেটেড ভার্সন, উপরে)। কোনো কোড
এখনো লেখা হয়নি — পরের সেশন Users দিয়ে শুরু করবে, আগের মতোই।

## ✅ সেশন ২.৩১ — Withdrawal (AdminWithdrawalsView.kt), Ground Rule ২০ রেট্রোফিট — সম্পূর্ণ

### প্রেক্ষাপট / অর্ডার-বিচ্যুতি (স্বচ্ছভাবে নোট করা হলো)
আগের কনফার্মড অর্ডার ছিল Users (২.২৯) → KYC (২.৩০) → Withdrawal (২.৩১), একটা একটা করে। এই সেশনে
আপলোড করা zip-এর নামই ছিল `somadhan-admin-panel-withdrawal-retrofit.zip` — সেই সিগন্যাল অনুযায়ী
সরাসরি Withdrawal (২.৩১) ধরে কাজ করা হয়েছে, Users/KYC এখনো **স্পর্শ করা হয়নি**। কোড গ্রেপ করে
নিশ্চিত করা হয়েছে (`isFilterRefreshing` কোথাও নেই, scroll-effect এখনো `safePage`/
`safePendingPage`/`safeVerifiedPage`/`safeRejectedPage`-এ key করা) — তাই ২.২৯/২.৩০ এখনো বাকি,
পরবর্তী সেশনে ব্যবহারকারীর সাথে অর্ডার নিয়ে কনফার্ম করে নিতে হবে (Users/KYC আগে করবেন নাকি এখন
স্কিপ করেই রাখবেন)।

### যা করা হয়েছে (`AdminWithdrawalsView.kt`-এ, শুধু এই একটা ফাইল)
১. নতুন `isFilterRefreshing` বুলিয়ান স্টেট, `LaunchedEffect(searchQuery, selectedStatusFilter,
   currentPage)`-এ true → `delay(350L)` (rememberFieldChangePulse-এর ডিফল্ট duration-এর সাথে
   সামঞ্জস্যপূর্ণ) → `finally`-তে false (stuck-shimmer বাগ-১ ক্লাস এড়াতে try/finally বাধ্যতামূলক)।
   `currentPage` ইচ্ছাকৃতভাবে key-তে আছে (২০২৬-০৯-১৫ সিদ্ধান্ত অনুযায়ী pagination বদলও
   filter/search-এর মতো pulse করাবে)।
২. per-item `PulsingValue(isUpdating = withdrawalCardPulse)` → `PulsingValue(isUpdating =
   withdrawalCardPulse || isFilterRefreshing)` — action-pulse (Ground Rule ১৯, অপরিবর্তিত) আর
   filter-pulse দুটোর একটা true হলেই কার্ড pulse করবে।
৩. টপ-এ auto-scroll effect `LaunchedEffect(safePage) { listState.scrollToItem(0) }` →
   `LaunchedEffect(currentPage) { ... }` — scroll-jump ফিক্স (approve/reject-এ totalPages কমে
   coerced safePage স্বয়ংক্রিয়ভাবে বদলে ভুল scroll-to-top ট্রিগার করত)। pagination bounds-check
   অন্য জায়গায় (paginatedWithdrawals গণনায়) এখনো `safePage` ব্যবহার করে, অপরিবর্তিত।
৪. `import kotlinx.coroutines.delay` যোগ করা হয়েছে (আগে ছিল না)।

কোনো বিদ্যমান ফাংশনালিটি (status update/approve/reject, bulk-approve dialog, select-mode,
সার্চ, ফিল্টার, pagination bounds-logic) বদলানো হয়নি — শুধু pulse-scope আর scroll-trigger-key।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. brace/paren-balance: এডিটের আগে `{}` ২৯৮=২৯৮, `()` ৭৬০=৭৬০; এডিটের পরে `{}` ৩০২=৩০২,
   `()` ৭৭৩=৭৭৩ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safePage)` আর অবশিষ্ট নেই (এখন `LaunchedEffect(currentPage)`),
   ঠিক ১টা `isFilterRefreshing` state + effect, ঠিক ১টা `PulsingValue(isUpdating = ...)` কল
   (এখন দুটো ফ্ল্যাগ OR করা)।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি (drawer/navigation/ইনডেক্স ম্যাপিং অক্ষত)।
৪. zip-এর আগে ফাইল-লিস্ট diff করা হয়েছে — মূল আপলোড করা zip-এর ঠিক ২৪৯টা ফাইলই (dotfile সহ) নতুন
   zip-এ অক্ষত আছে, শুধু `AdminWithdrawalsView.kt` + এই progress ফাইল বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: ফিল্টার/
   সার্চ/স্ট্যাটাস-ট্যাব/pagination বদলালে দৃশ্যমান সব কার্ড ছোট্ট করে একসাথে pulse করছে কিনা (ফর্ম/
   হেডার/সামারি কার্ড pulse না করে); কোনো একটা approve/reject করার পর যদি সেই কারণে totalPages কমে
   যায়, তাহলে scroll পজিশন ঠিক আছে কিনা (জোর করে টপে না গিয়ে); সাধারণ next/prev pagination ক্লিকে
   এখনো ঠিকমতো টপে scroll হচ্ছে কিনা; bulk-approve-এ শুধু সিলেক্টেড কার্ডগুলোই pulse করছে কিনা।

### পরবর্তী সেশন
ব্যবহারকারীর সাথে কনফার্ম করা — Users (২.২৯) আর KYC (২.৩০)-এ এখনো Ground Rule ২০ প্রয়োগ করা হয়নি,
সেই দুটো এখন করা হবে কিনা (আগের কনফার্মড অর্ডার অনুযায়ী), নাকি অগ্রাধিকার বদলে অন্য কোনো ট্যাবে
যাওয়া হবে।

## ✅ সেশন ২.২৯ — Users (AdminUsersView.kt), Ground Rule ২০ রেট্রোফিট — সম্পূর্ণ

### প্রেক্ষাপট
ব্যবহারকারী কনফার্ম করেছেন আগের অর্ডার অনুযায়ী এগোতে — Users (২.২৯) প্রথমে, তারপর KYC (২.৩০)।

### যা করা হয়েছে (`AdminUsersView.kt`-এ, শুধু এই একটা ফাইল)
১. নতুন `isFilterRefreshing` বুলিয়ান স্টেট, `LaunchedEffect(searchQuery, selectedRoleFilter,
   sortByLowestReputation, currentPage)`-এ true → `delay(350L)` → `finally`-তে false (২.৩১-এর
   মতোই try/finally বাধ্যতামূলক, stuck-shimmer এড়াতে)। `currentPage` key-তে আছে (pagination বদলও
   filter/search-এর সমান pulse করাবে, ২০২৬-০৯-১৫ সিদ্ধান্ত)।
২. per-item `PulsingValue(isUpdating = userCardPulse)` → `PulsingValue(isUpdating = userCardPulse
   || isFilterRefreshing)` — action-pulse (সেশন ২.১৬, অপরিবর্তিত) আর filter-pulse একটা true হলেই
   কার্ড pulse করবে।
৩. টপ-এ auto-scroll effect `LaunchedEffect(safePage) { listState.scrollToItem(0) }` →
   `LaunchedEffect(currentPage) { ... }` — স্ক্রল-জাম্প ফিক্স (ban/restrict/role-change-এ কোনো
   ইউজার তালিকা থেকে বাদ পড়ে totalPages কমে coerced safePage স্বয়ংক্রিয়ভাবে বদলে যাওয়ার বাগ)।
   pagination bounds-check অন্য জায়গায় (`paginatedUsers` গণনায়) এখনো `safePage` ব্যবহার করে,
   অপরিবর্তিত।
৪. `import kotlinx.coroutines.delay` যোগ করা হয়েছে (আগে ছিল না)।

কোনো বিদ্যমান ফাংশনালিটি (ban/restrict toggle, balance/role/reputation adjust, password reset,
delete confirm, search, role filter, reputation sort, DB-backed pagination) বদলানো হয়নি — শুধু
pulse-scope আর scroll-trigger-key।

### যাচাই
১. brace/paren-balance: এডিটের পরে `{}` ২৮৬=২৮৬, `()` ৬৬৬=৬৬৬ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safePage)` আর অবশিষ্ট নেই, ঠিক ১টা `LaunchedEffect(currentPage)`
   (স্ক্রল effect), ঠিক ১টা `PulsingValue(isUpdating = ...)` কল (এখন দুটো ফ্ল্যাগ OR করা)।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি (drawer/navigation/ইনডেক্স ম্যাপিং অক্ষত)।
৪. zip-এর ফাইল-লিস্ট diff করা হয়েছে — মূল আপলোড করা zip-এর ফাইলই অক্ষত আছে, শুধু
   `AdminUsersView.kt` + এই progress ফাইল বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   সার্চ/রোল-ফিল্টার/রেপুটেশন-সর্ট/pagination বদলালে দৃশ্যমান সব ইউজার-কার্ড ছোট্ট করে একসাথে
   pulse করছে কিনা (হেডার/সার্চ-বার/ফিল্টার-চিপ pulse না করে); ban/restrict/role-change-এর পর যদি
   totalPages কমে যায়, scroll পজিশন ঠিক আছে কিনা (জোর করে টপে না গিয়ে); সাধারণ next/prev
   pagination ক্লিকে এখনো ঠিকমতো টপে scroll হচ্ছে কিনা।

### পরবর্তী সেশন
KYC (২.৩০) — একই Ground Rule ২০ রেট্রোফিট প্যাটার্ন (`AdminKycView.kt`-এ, filter/search/tab/
pagination state names আগে grep করে যাচাই করে নিতে হবে, অনুমান না করে)। KYC শেষ হলে ২.২৯-৩১ তিনটাই
সম্পূর্ণ হবে। **(২০২৬-০৯-১৫ আপডেট: KYC-এর পরেও থামা যাবে না — নিচের নতুন এন্ট্রি দেখো, স্কোপ এখন
Users/KYC/Withdrawal-এ সীমাবদ্ধ না, বাকি সব ম্যাচিং ট্যাবেও এই একই রেট্রোফিট চলবে।)**

## ✅ সেশন ২.৩১.১ — Ground Rule ২০ স্কোপ সম্প্রসারণ: শুধু তিনটা ট্যাব না, বাকি সব ম্যাচিং ট্যাবেও (সম্পূর্ণ, কোনো `.kt` ফাইল এডিট হয়নি)

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড, ২০২৬-০৯-১৫, ২.৩০ শুরুর ঠিক আগে)
এতদিন Ground Rule ২০ রেট্রোফিট শুধু Users (২.২৯, ✅) → KYC (২.৩০, বাকি) → Withdrawal (২.৩১, ✅) —
এই তিনটা ট্যাবে সীমাবদ্ধ রাখা হয়েছিল। ব্যবহারকারী স্পষ্ট করেছেন: এই একই ফরম্যাট (filter/search/
pagination বদলে whole-visible-card pulse + coerced-page scroll-jump ফিক্স) **যে কোনো Admin ট্যাবে
প্রযোজ্য যেখানে একই তিনটা উপাদান একসাথে আছে** — তাই KYC-এর পরে থেমে না গিয়ে বাকি সব ম্যাচিং ট্যাবেও
এই প্যাটার্ন একটা একটা করে (কনফার্মেশন নিয়ে) প্রয়োগ করে যেতে হবে।

### যা করা হয়েছে (শুধু ডকুমেন্টেশন, কোনো কোড এডিট না)
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এর "সেশন ২.২৯ থেকে ২.৩১" সেকশন আপডেট করে "২.২৯ থেকে শুরু
(ওপেন-এন্ডেড)" করা হয়েছে —
১. একটা ৩-শর্তের গ্রেপ-পদ্ধতি লেখা হয়েছে (`coerceIn(1,` + `PulsingValue` + `scrollToItem(0)`
   তিনটাই থাকলে candidate, `isFilterRefreshing` না থাকলে টার্গেট), যাতে পরের সেশন প্রতিবার নিজে
   scan করে candidate বের করতে পারে, অনুমান না করে।
২. এই zip-এ প্রাথমিক গ্রেপ-স্ক্যান চালিয়ে candidate পাওয়া গেছে (নিশ্চিতভাবে না — পরের সেশনে আবার
   গ্রেপ করে ভেরিফাই বাধ্যতামূলক): `AdminAdditionalChargesView.kt`, `AdminCancelledBidsView.kt`,
   `AdminRatingsView.kt` — এই তিনটাতে safePage-জাতীয় ক্ল্যাম্প + per-item `PulsingValue`
   (Ground Rule ১৯ ইতিমধ্যে আছে) + scroll-to-top effect তিনটাই আছে, কিন্তু `isFilterRefreshing`
   এখনো নেই।
৩. এটাও নোট করা হয়েছে কোন ট্যাবগুলোতে এই নির্দিষ্ট রেট্রোফিট প্রযোজ্য **না** (এখনই): যেগুলোতে
   safePage+scroll আছে কিন্তু per-item `PulsingValue` নেই (`AdminChatMonitoringView.kt`,
   `AdminDirectContractsView.kt`, `AdminEscrowView.kt`, `AdminGatewayPaymentsView.kt`,
   `AdminInstantJobsView.kt`, `AdminManualNotificationView.kt`, `AdminProblemsView.kt`,
   `AdminTransactionsView.kt` — এদের আগে Ground Rule ১৯ লাগবে কিনা সেটা আলাদা, স্কোপের বাইরের
   প্রশ্ন), আর যেগুলোতে `PulsingValue` আছে কিন্তু safePage/scroll-effect নেই
   (`AdminAuditLogView.kt`, `AdminCategoriesView.kt`, `AdminFaqManagementView.kt`,
   `AdminStatsView.kt`, `AdminUserLookupView.kt` — pagination নেই বা ভিন্ন প্যাটার্নে)।
৪. তালিকায় ✅/[ ] চেকমার্ক যোগ করে বর্তমান স্ট্যাটাস স্পষ্ট করা হয়েছে (Users ✅, Withdrawal ✅
   অর্ডার-বিচ্যুতির নোট সহ, KYC [ ] এখন পরের পালা)।
৫. এই progress ফাইলে এই এন্ট্রি যোগ করা হয়েছে।

### যাচাই
কোনো `.kt` ফাইল স্পর্শ হয়নি — শুধু দুটো `.md` ফাইল। zip-এর ফাইল-সংখ্যা তাই অপরিবর্তিত থাকবে
(শুধু এই দুটো ফাইলের কন্টেন্ট বদলেছে)।

### পরবর্তী সেশন
সেশন ২.৩০ — KYC (`AdminKycView.kt`), আগের প্ল্যান মতোই (তিনটা sub-tab, ফ্ল্যাগ-ডিজাইন প্রশ্ন করে
নিতে হবে)। **তারপর ২.৩২ থেকে** — এই এন্ট্রির ৩-শর্তের গ্রেপ আবার চালিয়ে candidate ভেরিফাই করে
(উপরের প্রাথমিক তালিকা দিয়ে শুরু করা যায়: AdminAdditionalChargesView → AdminCancelledBidsView →
AdminRatingsView, কিন্তু চূড়ান্ত অর্ডার ব্যবহারকারীর সাথে কনফার্ম করে) বাকি ম্যাচিং ট্যাবগুলোতে একটা
একটা করে একই রেট্রোফিট চালিয়ে যাওয়া, পুরো তালিকা শেষ না হওয়া পর্যন্ত।

## ✅ সেশন ২.৩১.২ — Ground Rule ১৮/২০ "দুই-সেশনে-ভাগ" ফাঁক বন্ধ (সম্পূর্ণ, কোনো `.kt` ফাইল এডিট হয়নি)

### সমস্যা যা ধরা পড়েছিল
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এর সেশন ২.৩-২.১৪ আর ২.১৫-২.২৫ টেমপ্লেট (Ground Rule ১৮
retrofit) লেখা হয়েছিল Ground Rule ২০ তৈরির আগে। ফলে ৮টা ট্যাবের (safePage clamp + scroll-effect
আছে, per-item `PulsingValue` নেই) সেশন-এন্ট্রিতে Ground Rule ২০-এর কথা ছিল না —
`AdminManualNotificationView.kt` (২.৪), `AdminProblemsView.kt` (২.১৭), `AdminEscrowView.kt`
(২.১৮), `AdminTransactionsView.kt` (২.১৯), `AdminChatMonitoringView.kt` (২.২০),
`AdminDirectContractsView.kt` (২.২১), `AdminInstantJobsView.kt` (২.২৩),
`AdminGatewayPaymentsView.kt` (২.২৫)। এই ৮টায় Ground Rule ১৮ (per-item `PulsingValue` সহ) বসানোর
পরে তারা Ground Rule ২০-এর candidate হয়ে যেতে পারে, কিন্তু টেমপ্লেটে লেখা না থাকায় সেটা মিস হয়ে
আলাদা ভবিষ্যৎ সেশন লাগতে পারত।

### যা করা হয়েছে (শুধু ডকুমেন্টেশন, কোনো কোড এডিট না)
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ দুই জায়গায় নোট যোগ করা হয়েছে:
১. সেশন ২.৩-২.১৪ টেমপ্লেটে (`AdminManualNotificationView.kt`/২.৪-এর জন্য) — per-item
   `PulsingValue` বসানোর পরেই একই সেশনে Ground Rule ২০-এর ৩-শর্তের গ্রেপ আবার চালিয়ে candidate
   কিনা যাচাই করার নির্দেশ।
২. সেশন ২.১৫-২.২৫ টেমপ্লেটে (বাকি ৭টা ট্যাবের জন্য) — একই নির্দেশ, নাম ধরে ৭টা ট্যাব-ইনডেক্স সহ।
দুই জায়গাতেই স্পষ্ট করে লেখা হয়েছে: "Ground Rule ১৮ আর Ground Rule ২০ কখনো দুই আলাদা সেশনে ভাগ করা
যাবে না যদি একই সেশনে candidate হয়ে যায়।"

### যাচাই
কোনো `.kt` ফাইল স্পর্শ হয়নি — শুধু দুটো `.md` ফাইল (মাস্টার প্রম্পট + এই প্রোগ্রেস ফাইল)। zip-এর
ফাইল-সংখ্যা তাই অপরিবর্তিত থাকবে।

### পরবর্তী সেশন
সেশন ২.৩০ — KYC (`AdminKycView.kt`)। এরপর ২.৩২ থেকে বাকি ম্যাচিং ট্যাব, আর ২.৩-২.১৪ / ২.১৫-২.২৫
তালিকার যেকোনো সেশনে গেলে এখন থেকে ওপরের নতুন নোট অনুযায়ী Ground Rule ১৮+২০ একসাথে চেক/প্রয়োগ করতে
হবে (আলাদা করা যাবে না)।

## ✅ সেশন ২.৩০ — KYC (AdminKycView.kt), Ground Rule ২০ রেট্রোফিট — সম্পূর্ণ

### প্রেক্ষাপট
Users (২.২৯) ও Withdrawal (২.৩১) সম্পন্ন হওয়ার পর অবশিষ্ট ছিল KYC (২.৩০)। ব্যবহারকারী নির্দেশ
দিয়েছেন: এই স্ক্রিন Users/Withdrawal-এর মতো হুবহু না মিললে যেটা সবচেয়ে সঠিক (perfect) হয় সেটাই
করতে হবে, তবে চূড়ান্ত আউটপুট মাস্টার-প্রম্পটের ফরম্যাট/নীতি মেনে হতে হবে।

### কেন এটা হুবহু Users/Withdrawal-এর প্যাটার্ন না
`AdminKycView.kt` গ্রেপ করে দেখা গেছে এই স্ক্রিনের গঠন ভিন্ন:
- একটাই শেয়ার্ড `searchQuery`, কিন্তু ৩টা সাব-ট্যাবের (Pending/Verified/Rejected) সম্পূর্ণ আলাদা
  pagination state (`safePendingPage`/`safeVerifiedPage`/`safeRejectedPage`)।
- স্ক্রল-জাম্প effect (`LaunchedEffect(safePendingPage, safeVerifiedPage, safeRejectedPage,
  selectedKycTab) { listState.scrollToItem(0) }`) আগে থেকেই coerced-page-change ধরে ঠিকভাবে কাজ
  করছিল — Users/Withdrawal-এ যে scroll-jump বাগ ফিক্স করা হয়েছিল (`safePage` → `currentPage` key
  বদল), সেই বাগ এখানে ছিলই না, তাই এই অংশে কোনো এডিট করা হয়নি।
- প্রতিটা সাব-ট্যাবে আলাদা `solverCardPulse` (Ground Rule ১৯, per-item) — মোট ৩ জায়গায়, একই না।

### যা করা হয়েছে (`AdminKycView.kt`-এ, শুধু এই একটা ফাইল)
১. নতুন শেয়ার্ড `isFilterRefreshing` বুলিয়ান স্টেট, `LaunchedEffect(searchQuery, safePendingPage,
   safeVerifiedPage, safeRejectedPage)`-এ true → `delay(350L)` → `finally`-তে false। `selectedKycTab`
   ইচ্ছাকৃতভাবে এই key-তে রাখা হয়নি — ট্যাব বদল শুধু ভিউ বদলায়, ডেটা রিফিল্টার হয় না (Users/Withdrawal-এ
   `currentPage` রাখার যুক্তির সমান্তরাল প্রয়োগ, কিন্তু এখানে ৩টা আলাদা page-var থাকায় সবগুলোই key-তে
   রাখা হয়েছে)।
২. তিনটা সাব-ট্যাবেরই `PulsingValue(isUpdating = solverCardPulse)` → `PulsingValue(isUpdating =
   solverCardPulse || isFilterRefreshing)` (sed দিয়ে ৩ জায়গাতেই একসাথে, যেহেতু টেক্সট হুবহু একই)।
৩. `import kotlinx.coroutines.delay` যোগ করা হয়েছে (আগে ছিল না)।
৪. স্ক্রল-জাম্প অংশ স্পর্শ করা হয়নি (উপরে ব্যাখ্যা করা কারণে — এখানে সেই বাগ ছিলই না)।

কোনো বিদ্যমান ফাংশনালিটি (approve/reject/revoke, bulk-approve, select-mode, ৩-ট্যাব সুইচ, সার্চ,
পার-ট্যাব pagination) বদলানো হয়নি — শুধু pulse-scope যোগ হয়েছে।

### যাচাই
১. brace/paren-balance: এডিটের পরে `{}` ৩৬১=৩৬১, `()` ৯৪৩=৯৪৩ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: ঠিক ১টা `isFilterRefreshing` state + effect (মোট ৬টা রেফারেন্স — ১ ডিক্লেয়ার + ১
   effect key + ১ effect বডিতে সেট/রিসেট (try+finally) + ৩টা PulsingValue-তে ব্যবহার), ঠিক ৩টা
   `PulsingValue(isUpdating = solverCardPulse || isFilterRefreshing)` কল।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি।
৪. zip-এর ফাইল-লিস্ট diff করা হয়েছে — মূল আপলোড করা zip-এর ফাইলই অক্ষত আছে, শুধু `AdminKycView.kt` +
   এই progress ফাইল বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: সার্চ
   করলে বা যেকোনো সাব-ট্যাবের pagination বদলালে সেই মুহূর্তে দৃশ্যমান সব সলভার-কার্ড ছোট্ট করে একসাথে
   pulse করছে কিনা (হেডার/সার্চ-বার/ট্যাব-বার/সিলেক্ট-মোড বার pulse না করে); শুধু ট্যাব বদলালে (সার্চ/পেজ
   অপরিবর্তিত রেখে) pulse না হওয়া উচিত; approve/reject/revoke/bulk-approve-এর নিজস্ব per-item
   pulse (Ground Rule ১৯) আগের মতোই কাজ করছে কিনা।

### পরবর্তী সেশন
Users → KYC → Withdrawal — তিনটাই এখন সম্পূর্ণ। ২.৩২ থেকে: Ground Rule ২০-এর ৩-শর্তের গ্রেপ
(`coerceIn(1,` + `PulsingValue` + `scrollToItem(0)`) আবার চালিয়ে বাকি ম্যাচিং ট্যাব candidate
ভেরিফাই করা (প্রাথমিক তালিকা: `AdminAdditionalChargesView.kt`, `AdminCancelledBidsView.kt`,
`AdminRatingsView.kt` — তিনটাতেই safePage-ক্ল্যাম্প + per-item `PulsingValue` (Ground Rule ১৯)
আছে কিন্তু `isFilterRefreshing` নেই), চূড়ান্ত অর্ডার ব্যবহারকারীর সাথে কনফার্ম করে।

## ✅ সেশন ২.৩২ — AdditionalCharges (AdminAdditionalChargesView.kt), Ground Rule ২০ রেট্রোফিট — সম্পূর্ণ (patternভিন্ন — নিচে ব্যাখ্যা)

### প্রেক্ষাপট
Users → KYC → Withdrawal সম্পন্ন হওয়ার পর ৩-শর্তের গ্রেপ (`coerceIn(1,` + `PulsingValue` +
`scrollToItem(0)`, `isFilterRefreshing` অনুপস্থিত) পুরো `Admin*View.kt` তালিকায় আবার চালিয়ে
নিশ্চিত করা হয়েছে candidate তালিকা এখনো তিনটাই: `AdminAdditionalChargesView.kt`,
`AdminCancelledBidsView.kt`, `AdminRatingsView.kt` (বাকি সবগুলোয় হয় শর্ত অসম্পূর্ণ, নাহলে ইতিমধ্যে
রেট্রোফিট করা)। ব্যবহারকারীর নির্দেশ অনুযায়ী ("Users/Withdrawal-এর মতো হুবহু না মিললে যেটা সঠিক
সেটাই করো") এই সেশনে প্রথমটা — AdditionalCharges — নেওয়া হয়েছে।

### কেন এখানে `isFilterRefreshing` যোগ করা হয়নি (Users/Withdrawal/KYC থেকে ইচ্ছাকৃত বিচ্যুতি)
গ্রেপ করে দেখা গেছে এই ফাইলের pulse-ডিজাইন মূলগতভাবে ভিন্ন এবং আগে থেকেই Ground Rule ২০-এর
লক্ষ্য অর্জন করে ফেলেছে: এখানে per-item pulse না থাকে, বরং একটা single `chargesListPulse =
rememberFieldChangePulse(value = paginatedCharges, ...)` — `items(paginatedCharges) { charge ->
PulsingValue(isUpdating = chargesListPulse) { ... } }`-এর ভেতরে ব্যবহৃত হয় প্রতিটা আইটেমে, কিন্তু
value সম্পূর্ণ `paginatedCharges` লিস্ট (single shared value)। তাই সার্চ/ফিল্টার/পেজ বদলালে
`paginatedCharges`-এর কনটেন্ট বদলে যায় → পুরো দৃশ্যমান লিস্টই একসাথে pulse করে — এটাই ঠিক
Ground Rule ২০-এর কাঙ্ক্ষিত আচরণ, শুধু ভিন্নভাবে (Users/Withdrawal-এ যেখানে per-item pulse +
আলাদা `isFilterRefreshing` OR করা হয়, এখানে একটা single shared-value pulse-ই কাজটা করে)।
`isFilterRefreshing` এখানে যোগ করলে সদৃশ/রিডানডেন্ট আচরণ হতো (একই ট্রিগারে দুইবার pulse করানো),
তাই সেটা এড়ানো হয়েছে।

### যা করা হয়েছে (`AdminAdditionalChargesView.kt`-এ, শুধু এই একটা ফাইল)
আসল গ্যাপ ছিল শুধু scroll-jump বাগ — `LaunchedEffect(safePage) { listState.scrollToItem(0) }` কে
`LaunchedEffect(currentPage) { ... }` করা হয়েছে (Users/Withdrawal-এ প্রয়োগ করা একই ফিক্স — কোনো
চার্জ approve/reject-এর পর `totalPages` কমে গিয়ে coerced `safePage` স্বয়ংক্রিয়ভাবে বদলালে জোর করে
টপে scroll হওয়া বন্ধ করা)। `paginatedCharges` গণনায় bounds-check এখনো `safePage` ব্যবহার করে,
অপরিবর্তিত। একটা ব্যাখ্যামূলক Bengali কমেন্ট যোগ করা হয়েছে কেন এই এডিট।

কোনো বিদ্যমান ফাংশনালিটি (approve/reject/settle, সার্চ, স্ট্যাটাস/টাইম ফিল্টার, pagination)
বদলানো হয়নি।

### যাচাই
১. brace/paren-balance: এডিটের পরে `{}` ১০৩=১০৩, `()` ৩৬৯=৩৬৯ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safePage)` আর নেই, ঠিক ১টা `LaunchedEffect(currentPage)`।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি।
৪. zip-এর ফাইল-লিস্ট diff করা হয়েছে — মূল আপলোডের ফাইলই অক্ষত আছে, শুধু
   `AdminAdditionalChargesView.kt` + এই progress ফাইল বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   কোনো চার্জ approve/reject/settle করার পর যদি totalPages কমে যায়, scroll পজিশন ঠিক আছে কিনা
   (জোর করে টপে না গিয়ে); সাধারণ next/prev pagination ক্লিকে এখনো ঠিকমতো টপে scroll হচ্ছে কিনা;
   সার্চ/ফিল্টার বদলালে দৃশ্যমান সব চার্জ-কার্ড আগের মতোই একসাথে pulse করছে কিনা (এই আচরণ আগে থেকেই
   ছিল, এই সেশনে বদলায়নি)।

### পরবর্তী সেশন
২.৩৩ — `AdminCancelledBidsView.kt`, একই ৩-শর্তের candidate তালিকার পরবর্তী। প্রথমেই grep করে
এর pulse-ডিজাইন per-item না shared-list-value, সেটা যাচাই করে নিতে হবে (AdditionalCharges-এর
মতো shared-value হলে শুধু scroll-jump ফিক্স যথেষ্ট; Users/KYC/Withdrawal-এর মতো per-item হলে
`isFilterRefreshing` যোগ করতে হবে) — অনুমান না করে প্রতিবার নতুন করে ভেরিফাই করা বাধ্যতামূলক।

## ✅ সেশন ২.৩৩ — CancelledBids (AdminCancelledBidsView.kt), Ground Rule ২০ রেট্রোফিট — সম্পূর্ণ (AdditionalCharges-এর মতোই shared-value প্যাটার্ন)

### প্রেক্ষাপট
২.৩২-এর candidate তালিকার পরেরটা — `AdminCancelledBidsView.kt` — নেওয়ার আগে নতুন করে গ্রেপ
করে কনফার্ম করা হয়েছে: `coerceIn(1,`×১, `PulsingValue`×৭ (কিন্তু আসল বিড-তালিকা pulse একটাই —
বাকিগুলো `summaryCountsPulse`/`frequentCancellersPulse`/`modalCancellersPulse`, আলাদা উদ্দেশ্যে),
`scrollToItem(0)`×১, `isFilterRefreshing` অনুপস্থিত।

### কেন এখানেও `isFilterRefreshing` যোগ করা হয়নি (AdditionalCharges/২.৩২-এর মতোই বিচ্যুতি)
এই ফাইলেরও pulse-ডিজাইন AdditionalCharges-এর অনুরূপ shared-list-value: `cancelledBidsListPulse =
rememberFieldChangePulse(value = paginatedBids, ...)` — `paginatedBids`-এর কনটেন্ট সরাসরি
value হিসেবে দেওয়া, তাই সার্চ/ফিল্টার/পেজ বদলালে এই value বদলে যায় আর দৃশ্যমান পুরো তালিকা এমনিতেই
একসাথে pulse করে (Ground Rule ২০-এর কাঙ্ক্ষিত আচরণ, এই ফাইলে per-item action-pulse এর বদলে
single shared-value দিয়েই অর্জিত)। `isFilterRefreshing` যোগ করলে রিডানডেন্ট হতো, তাই এড়ানো হয়েছে।

### যা করা হয়েছে (`AdminCancelledBidsView.kt`-এ, শুধু এই একটা ফাইল)
আসল গ্যাপ ছিল শুধু scroll-jump বাগ (এই স্ক্রিনে কোনো approve/reject অ্যাকশন বাটন নেই — এটা একটা
রিড-অনলি রিপোর্ট/ইতিহাস ভিউ — কিন্তু realtime সিঙ্কে অন্য কোনো জায়গা থেকে বিড-স্ট্যাটাস বদলে
`cancelledBids` লিস্ট প্যাসিভভাবে ছোট হয়ে গেলে `totalPages` কমে যেতে পারে, তখনও এই বাগ প্রযোজ্য):
`LaunchedEffect(safeCurrentPage) { listState.scrollToItem(0) }` কে `LaunchedEffect(currentPage)
{ ... }` করা হয়েছে — একই ফিক্স যা Users/Withdrawal/KYC/AdditionalCharges-এ প্রয়োগ করা হয়েছিল।
`paginatedBids` গণনায় bounds-check এখনো `safeCurrentPage` ব্যবহার করে, অপরিবর্তিত।

কোনো বিদ্যমান ফাংশনালিটি (সার্চ, সময়/সলভার ফিল্টার, বারবার-বাতিলকারী মডাল, pagination) বদলানো হয়নি।

### যাচাই
১. brace/paren-balance: এডিটের পরে `{}` ২১২=২১২, `()` ৬১৪=৬১৪ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safeCurrentPage)` আর নেই, ঠিক ১টা `LaunchedEffect(currentPage)`।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি।
৪. zip-এর ফাইল-লিস্ট মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে (unzip -l header বাদে ২৪৯টা
   ফাইলই মিলেছে) — শুধু `AdminCancelledBidsView.kt` + এই progress ফাইল বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   সাধারণ next/prev pagination ক্লিকে এখনো ঠিকমতো টপে scroll হচ্ছে কিনা; realtime-এ কোনো বাতিল-বিড
   স্ট্যাটাস বদলে গিয়ে (অন্য ডিভাইস থেকে) totalPages কমে গেলে জোর করে টপে scroll না হয়ে বর্তমান
   scroll পজিশন ঠিক থাকছে কিনা; সার্চ/ফিল্টার বদলালে দৃশ্যমান বিড-কার্ডগুলো আগের মতোই একসাথে pulse
   করছে কিনা (এই আচরণ আগে থেকেই ছিল, এই সেশনে বদলায়নি)।

### পরবর্তী সেশন
২.৩৪ — `AdminRatingsView.kt`, একই ৩-শর্তের candidate তালিকার শেষটা। প্রথমেই গ্রেপ করে এর pulse-ডিজাইন
per-item না shared-list-value সেটা যাচাই করে নিতে হবে (AdditionalCharges/CancelledBids-এর মতো
shared-value হলে শুধু scroll-jump ফিক্স যথেষ্ট; Users/KYC/Withdrawal-এর মতো per-item হলে
`isFilterRefreshing` যোগ করতে হবে) — অনুমান না করে প্রতিবার নতুন করে ভেরিফাই করা বাধ্যতামূলক। এটা
শেষ হলে Ground Rule ২০-এর candidate তালিকা (Users→KYC→Withdrawal→AdditionalCharges→CancelledBids→
Ratings) সম্পূর্ণ হবে।

## ✅ সেশন ২.৩৪ — Ratings (AdminRatingsView.kt), Ground Rule ২০ রেট্রোফিট — সম্পূর্ণ (AdditionalCharges/CancelledBids-এর মতোই shared-value প্যাটার্ন) — candidate তালিকা এখন শেষ

### প্রেক্ষাপট
২.৩৩-এর candidate তালিকার শেষটা — `AdminRatingsView.kt` — নেওয়ার আগে নতুন করে গ্রেপ করে কনফার্ম
করা হয়েছে: `coerceIn(1,`×১, `PulsingValue`×১ (`ratingsListPulse`), `scrollToItem(0)`×১,
`isFilterRefreshing` অনুপস্থিত।

### কেন এখানেও `isFilterRefreshing` যোগ করা হয়নি (AdditionalCharges/CancelledBids-এর মতোই বিচ্যুতি)
গ্রেপ করে দেখা গেছে এই ফাইলেরও pulse-ডিজাইন shared-list-value: `ratingsListPulse =
rememberFieldChangePulse(value = paginatedRatings, ...)` — ফাইলের নিজস্ব বিদ্যমান কমেন্টেই লেখা
ছিল যে ফিল্টার/সর্ট/পেজ পাল্টানো বা ডেটা বদলানো (ডিলিট-সহ) সবই এই value বদলে দিয়ে পুরো দৃশ্যমান
তালিকা একসাথে pulse করায় — এটাই Ground Rule ২০-এর কাঙ্ক্ষিত আচরণ, আলাদা `isFilterRefreshing`
যোগ করলে রিডানডেন্ট হতো, তাই এড়ানো হয়েছে।

### যা করা হয়েছে (`AdminRatingsView.kt`-এ, শুধু এই একটা ফাইল)
আসল গ্যাপ ছিল শুধু scroll-jump বাগ (কোনো রেটিং ডিলিট হয়ে `totalPages` কমে গিয়ে coerced `safePage`
স্বয়ংক্রিয়ভাবে বদলালে জোর করে টপে scroll হওয়া বন্ধ করা): `LaunchedEffect(safePage) {
listState.scrollToItem(0) }` কে `LaunchedEffect(currentPage) { ... }` করা হয়েছে — Users/Withdrawal/
KYC/AdditionalCharges/CancelledBids-এ প্রয়োগ করা একই ফিক্স। `paginatedRatings` গণনায় bounds-check
এখনো `safePage` ব্যবহার করে, অপরিবর্তিত। একটা ব্যাখ্যামূলক Bengali কমেন্ট যোগ করা হয়েছে।

কোনো বিদ্যমান ফাংশনালিটি (সার্চ, তারকা/সময় ফিল্টার, সর্ট, ডিলিট, pagination) বদলানো হয়নি।

### যাচাই
১. brace/paren-balance: এডিটের পরে `{}` ১৮৫=১৮৫, `()` ৫৭৮=৫৭৮ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safePage)` আর নেই, ঠিক ১টা `LaunchedEffect(currentPage)`।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি।
৪. zip-এর ফাইল-সংখ্যা মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে (২৪৯টা ফাইলই মিলেছে) — শুধু
   `AdminRatingsView.kt` + এই progress ফাইল বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: সাধারণ
   next/prev pagination ক্লিকে এখনো ঠিকমতো টপে scroll হচ্ছে কিনা; কোনো রেটিং ডিলিট করার পর
   totalPages কমে গেলে জোর করে টপে scroll না হয়ে বর্তমান scroll পজিশন ঠিক থাকছে কিনা; সার্চ/
   ফিল্টার/সর্ট বদলালে দৃশ্যমান রেটিং-কার্ডগুলো আগের মতোই একসাথে pulse করছে কিনা (এই আচরণ আগে
   থেকেই ছিল, এই সেশনে বদলায়নি)।

### পরবর্তী সেশন
Ground Rule ২০-এর candidate তালিকা (Users→KYC→Withdrawal→AdditionalCharges→CancelledBids→Ratings)
**এখন সম্পূর্ণ** — এই সেশনেই পুরো `Admin*View.kt` তালিকায় ৩-শর্তের গ্রেপ আরেকবার চালিয়ে নিশ্চিত করা
হয়েছে। **⚠️ নোট:** এই গ্রেপ এখনো ৩টা ফাইল ফেরত দেয় — `AdminAdditionalChargesView.kt`,
`AdminCancelledBidsView.kt`, `AdminRatingsView.kt` (এই তিনটাই)। এটা নতুন miss না — শুধু এই কারণে যে
এই তিনটার shared-value-pulse ফিক্স (২.৩২/২.৩৩/২.৩৪-এ) `isFilterRefreshing` স্ট্রিং যোগ করে না
(ইচ্ছাকৃতভাবে, কারণ ব্যাখ্যা ওপরের এন্ট্রিগুলোতে আছে), তাই সাদামাটা string-গ্রেপ এদের এখনো
"অসম্পূর্ণ" হিসেবে ফ্ল্যাগ করবে চিরকালই — এটা প্রত্যাশিত/জানা false-positive, নতুন কাজ বাকি নেই।
ভবিষ্যতে এই গ্রেপ চালানোর সময় এই তিনটা বাদ দিয়ে দেখলেই হবে। এরপর
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ Ground Rule ২০ রেট্রোফিট সেকশনটা "সম্পূর্ণ" হিসেবে
চিহ্নিত করা যেতে পারে। বাকি আছে শুধু ব্যবহারকারীর নিজের Android Studio build/run আর উপরের ৬টা
সেশনের (২.২৯, ২.৩০, ২.৩১, ২.৩২, ২.৩৩, ২.৩৪) ম্যানুয়াল টেস্ট চেকলিস্ট একসাথে verify করা।

## ✅ সেশন ২.৩৪.১ — ডকুমেন্টেশন সিঙ্ক: Ground Rule ২০ সেকশন "সম্পূর্ণ" চিহ্নিত + বাকি কাজের ম্যাপ রিফ্রেশ (সম্পূর্ণ, কোনো `.kt` ফাইল এডিট হয়নি)

### প্রেক্ষাপট
সেশন ২.৩৪-এর "পরবর্তী সেশন" নোটে বলা ছিল Ground Rule ২০ candidate তালিকা সম্পূর্ণ হওয়ার পর
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ সেকশনটা "সম্পূর্ণ" হিসেবে চিহ্নিত করা দরকার — এটা এখনো
করা হয়নি বলে এই সেশনে শুধু সেই ডকুমেন্টেশন-সিঙ্ক করা হলো, এবং একই সাথে পুরো admin panel-এর বাকি
pending কাজ (ধাপ ১-এর ডায়াগনসিসের পরে যা যা এখনো বাকি) `AdminPanelScreen.kt` সরাসরি গ্রেপ করে
নতুন করে ভেরিফাই করা হলো, যাতে পরের যেকোনো Claude সেশন এই zip থেকেই বাকি কাজ শুরু করতে পারে।

### যা করা হয়েছে (শুধু `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ, কোনো `.kt` ফাইল স্পর্শ হয়নি)
১. সেশন ২.২৯-থেকে-শুরু সেকশনের তালিকায় ✅ ২.৩০ (KYC), ✅ ২.৩২ (AdditionalCharges), ✅ ২.৩৩
   (CancelledBids), ✅ ২.৩৪ (Ratings) এন্ট্রি যোগ করা হয়েছে (আগে শুধু ২.২৯/২.৩১ ছিল, বাকিগুলো
   progress ফাইলে থাকলেও master prompt-এর checklist-এ যোগ হয়নি — এই gap বন্ধ করা হলো)।
২. Ground Rule ২০ রেট্রোফিট সেকশনকে স্পষ্টভাবে "সম্পূর্ণ" নোট দেওয়া হয়েছে, ৩-শর্তের গ্রেপে এখনো
   AdditionalCharges/CancelledBids/Ratings ফেরত আসার কারণ (shared-value pattern, প্রত্যাশিত
   false-positive) আবার স্পষ্ট করা হয়েছে যাতে ভবিষ্যতে কেউ ভুল করে এগুলোকে "বাকি কাজ" না ধরে।
৩. **`AdminPanelScreen.kt`-এর `when (selectedTabIndex)` ব্লক এই zip-এ সরাসরি গ্রেপ করে** (অনুমান
   না করে) বর্তমান প্রকৃত অবস্থার একটা তাজা ম্যাপ মাস্টার প্রম্পটে যোগ করা হয়েছে:
   - ইনডেক্স ৫, ৭, ৯, ১৫, ১৬, ১৯, ২১, ২২, ২৩ (Problems/Escrow/Transactions/ChatMonitoring/
     DirectContracts/SolverQuota/InstantJobs/DisputeCenter/GatewayPayments) — এই ৯টা এখনো
     `SyncAwareRefreshableContent` (Ground Rule ১৮ retrofit বাকি)। প্রতিটাতে `PulsingValue`
     গ্রেপ করে কনফার্ম করা হয়েছে — কোনোটাতেই এখনো নেই (Ground Rule ১৯ও বাকি)।
   - ইনডেক্স ০ (Overview) আর ১৮ (UserLookup) — সেশন ২.২ (pull-to-refresh upgrade সিদ্ধান্ত)
     এখনো চালানোই হয়নি, প্লেইন `SyncAwareContent`-এই আছে।
   - `ReputationDetailScreen.kt` গ্রেপ করে কনফার্ম করা হয়েছে এখনো `SyncAwareRefreshableContent`
     (সেশন ২.২৬ বাকি)।
   - বাকি ১২টা ইনডেক্স (১,২,৪,৬,৮,১০,১১,১২,১৩,১৪,১৮*,২০) সবই প্লেইন `SyncAwareContent`-এ
     আপ-টু-ডেট (*১৮-এ শুধু pull-to-refresh প্রশ্নটা বাকি, wrapper নিজে ঠিক আছে)।
   - ১৭ (Supabase এক্সপ্লোরার) আর ২৪ (রিফান্ড ডায়াগনস্টিক) — আগের সেশনের সিদ্ধান্ত অনুযায়ী
     ইচ্ছাকৃতভাবে বাদ, অপরিবর্তিত।
৪. মাস্টার প্রম্পটের "ধাপ ৪ — সাজেস্টেড অর্ডার" সেকশনে একটা নতুন "আপডেট ২" প্যারাগ্রাফ যোগ করা
   হয়েছে যেটা পরের সেশনের জন্য সরাসরি অর্ডার বলে দেয়: সেশন ২.২ → সেশন ২.১৮-২.২৬ (৯টা বাকি GR১৮
   retrofit, প্রতিটায় per-item PulsingValue বসানোর পরপরই একই সেশনে GR২০ গ্রেপ চালানো বাধ্যতামূলক,
   ভাগ করা যাবে না) → সেশন ২.২৬ (ReputationDetailScreen)।

### যাচাই
কোনো `.kt` ফাইল স্পর্শ হয়নি — শুধু `ADMIN_PANEL_LOADING_MASTER_PROMPT.md` + এই progress ফাইল
বদলেছে। zip-এর ফাইল-সংখ্যা অপরিবর্তিত (২৪৯টা কোড/কনফিগ ফাইল + এই দুটো `.md`)। উপরের ম্যাপের প্রতিটা
লাইন `AdminPanelScreen.kt`/সংশ্লিষ্ট `Admin*View.kt`/`ReputationDetailScreen.kt`-এ সরাসরি গ্রেপ
করে (line-by-line না হলেও wrapper-নাম আর `PulsingValue` কাউন্ট দিয়ে) কনফার্ম করা হয়েছে, অনুমান
করা হয়নি।

### পরবর্তী সেশন
**সেশন ২.২** — Overview (ইনডেক্স ০, `AdminStatsView`) আর UserLookup (ইনডেক্স ১৮,
`AdminUserLookupView`)-এ pull-to-refresh ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছিল কিনা নাকি যোগ করা দরকার —
এই প্রশ্নটা ব্যবহারকারীকে করে (Ground Rule ১৬, এখনো করা হয়নি) সিদ্ধান্ত অনুযায়ী upgrade বা
ইচ্ছাকৃত-ব্যতিক্রম হিসেবে নোট করা। এরপর **সেশন ২.১৮-২.২৬** — বাকি ৯টা Ground Rule ১৮ retrofit ট্যাব
(ইনডেক্স ৫, ৭, ৯, ১৫, ১৬, ১৯, ২১, ২২, ২৩ — উপরে নাম-সহ তালিকা), একটা একটা করে, প্রতিটার আগে Ground
Rule ১৬ অনুযায়ী "ঠিক কোন অংশ pulse করবে" আর "pull-to-refresh দরকার কিনা" জিজ্ঞাসা করে, আর প্রতিটায়
per-item `PulsingValue` বসানোর পরপরই একই সেশনে Ground Rule ২০-এর ৩-শর্তের গ্রেপ চালিয়ে candidate
কিনা যাচাই করা (ভাগ করা যাবে না)। সবশেষে **সেশন ২.২৬** — `ReputationDetailScreen.kt`
(`SyncAwareRefreshableContent`, whole-content flash এখনো আছে বলে কনফার্মড) একই retrofit দরকার
কিনা ব্যবহারকারীকে জিজ্ঞাসা করা।

## ✅ সেশন ২.২ — Overview (ইনডেক্স ০) + UserLookup (ইনডেক্স ১৮): pull-to-refresh ভেরিফিকেশন (সম্পূর্ণ, কোনো `.kt` ফাইল এডিট হয়নি)

### ব্যবহারকারীর সিদ্ধান্ত
জিজ্ঞাসা করা হয়েছিল (Ground Rule ১৬) — এই দুই ট্যাবে pull-to-refresh ইচ্ছাকৃত ব্যতিক্রম নাকি যোগ করা
দরকার। ব্যবহারকারী বলেছেন: যোগ করো।

### গ্রেপ-ফলাফল (কোড এডিট করার আগে)
ফ্রেশ গ্রেপ করে দেখা গেল মাস্টার প্রম্পটের আগের ডায়াগনসিস ভুল ছিল — pull-to-refresh **ইতিমধ্যেই
পুরোপুরি আছে** দুটো ট্যাবেই:
- `AdminPanelScreen.kt`-এর গোটা `when (selectedTabIndex) {...}` ব্লক আগে থেকেই একটা একক গ্লোবাল
  `SomadhanPullToRefresh(isRefreshing = isRefreshing, onRefresh = onAdminPullToRefresh, ...)` দিয়ে
  মোড়ানো (লাইন ~৯৫০-৯৫৪) — এটা ২৫টা ট্যাবের প্রতিটাতেই সমানভাবে প্রযোজ্য, ইনডেক্স ০/১৮ সহ।
- `AdminStatsView` (ইনডেক্স ০) নিজেই `isRefreshing: Boolean` প্যারামিটার নেয় (call-site-এ
  `isRefreshing = isRefreshing` পাস করা হয়) আর ভেতরে ৯টা independent shimmer-zone
  (`heroCardPulsing`/`row1Pulsing`/`escrowChargesPulsing`/`row2Pulsing`/`row3Pulsing`/
  `chart1Pulsing`...`chart4Pulsing`/`revenueTrendPulsing`/`topCategoriesPulsing`/
  `topSolversPulsing` — মোট ১২টা, তালিকায় গোনা হয়েছে) প্রতিটাই
  `rememberFieldChangePulse(..., isManualRefreshing = isRefreshing)` ব্যবহার করে।
- `AdminUserLookupView` (ইনডেক্স ১৮) কল-সাইটে `isRefreshing` প্যারামিটার নেয় না, কিন্তু ফাংশনের
  ভেতরেই সরাসরি `viewModel.isRefreshing.collectAsStateWithLifecycle()` কল করে (একই গ্লোবাল
  `StateFlow`) — `summaryPulse`/`financialPulse`/`rowPulse`-জাতীয় সারসংক্ষেপ/আর্থিক/রো-লেভেল
  pulse-গুলো সবই এই local `isRefreshing`-ই ব্যবহার করে।

**১২টা independent shimmer-zone পাইলট (`AdminStatsView`, ধাপ ০.১৫ চেক):** প্রতিটা zone আলাদাভাবে
গ্রেপ করে তার ভেতরের real-content (হিরো কার্ড/৪টা চার্ট/৩টা রো/টপ-ক্যাটাগরি/টপ-সলভার) skeleton-shape
তার নিজস্ব layout-এর সাথে মিলছে কিনা যাচাই করা হয়েছে — কোনো mismatch পাওয়া যায়নি।

### সিদ্ধান্ত ও ফলাফল
যেহেতু pull-to-refresh (গ্লোবাল gesture + ভেতরের section/row-level pulse) ইতিমধ্যেই সঠিকভাবে আছে
(Ground Rule ১৯-এর section-scope সমতুল্য প্যাটার্নে — AdminStatsView-এ per-section, AdminUserLookupView-এ
per-row/summary), **কোনো কোড এডিট করা হয়নি**। `SyncAwareContent`-কে `SyncAwareRefreshableContent`-এ
upgrade করা এক্ষেত্রে ভুল হতো — সেটা Ground Rule ১৮-এর "পুরো ট্যাব কখনো whole-content flash করবে না"
নিষেধ ভঙ্গ করত (এই দুই ট্যাব ইতিমধ্যে ঠিক Ground Rule ১৮/১৯-এর কাঙ্ক্ষিত প্যাটার্নেই আছে, শুধু আগের
সেশনের ডায়াগনসিসে outer wrapper-টাই চেক হয়েছিল, ভেতরের কম্পোজেবলের বডি গ্রেপ করা হয়নি)। শুধু
`ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এর ম্যাপ (ইনডেক্স ০/১৮-কে "এখনো সিদ্ধান্ত হয়নি" থেকে
"সম্পূর্ণ/আপ-টু-ডেট"-এ সরানো) + এই progress ফাইল আপডেট হয়েছে।

### যাচাই
কোনো `.kt` ফাইল স্পর্শ হয়নি — শুধু দুটো `.md` ফাইল। zip-এর ফাইল-সংখ্যা অপরিবর্তিত। ব্যবহারকারীর
কাছে এই ফলাফল স্পষ্টভাবে জানানো হয়েছে (তার "যোগ করো" উত্তরের বিপরীতে, যেহেতু গ্রেপ-ফলাফল দেখিয়েছে
এটা redundant/ক্ষতিকর হতো) — Ground Rule ১৬-এর চেতনা অনুযায়ী কোডে যা সত্যি তাই প্রাধান্য পেয়েছে।

### পরবর্তী সেশন
সেশন ২.১৮ — সমস্যাসমূহ (`AdminProblemsView.kt`, ইনডেক্স ৫), Ground Rule ১৮ retrofit, বাকি ৯টা
ট্যাবের প্রথমটা। শুরুর আগে ব্যবহারকারীকে দুটো প্রশ্ন করতে হবে (Ground Rule ১৬): (ক) ঠিক কোন অংশ
pulse করবে (তালিকার কার্ড/আইটেম, নাকি কিছুই না), (খ) pull-to-refresh দরকার কিনা।

## ✅ সেশন ২.১৮ — সমস্যাসমূহ (AdminProblemsView.kt, ইনডেক্স ৫): Ground Rule ১৮+১৯+২০ একসাথে retrofit — সম্পূর্ণ

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড, Ground Rule ১৬)
(ক) শুধু তালিকার কার্ড/আইটেম pulse করবে (KYC/Users-এর মতো)। (খ) pull-to-refresh (`isManualRefreshing`)
দরকার। ব্যবহারকারীর নির্দেশ অনুযায়ী (নোট ২.৩১.২) Ground Rule ১৮ আর ২০ একই সেশনে একসাথে করা হয়েছে,
ভাগ করা হয়নি।

### গ্রেপ-ভেরিফিকেশন (শুরুর আগে)
`AdminProblemsView.kt`-এ আগে থেকেই `coerceIn(1,` (safePage) + `scrollToItem(0)` ছিল (pagination
বাটন-ভিত্তিক, ১০-per-page) কিন্তু কোনো `PulsingValue`/`rememberFieldChangePulse` ছিল না — অর্থাৎ
Ground Rule ১৯/২০ দুটোই প্রথম থেকে বসাতে হয়েছে (এতদিন ৩-শর্তের গ্রেপ এই ফাইলে match করেনি কারণ
`PulsingValue` শর্তটা পূরণ হতো না)।

### যা করা হয়েছে
- **`AdminPanelScreen.kt` (ইনডেক্স ৫):** `SyncAwareRefreshableContent(data = listOf(allProblems,
  allUsers), isManualRefreshing = isRefreshing)` সরিয়ে plain `SyncAwareContent` (rule ১+২,
  `data`-বিহীন) দিয়ে wrap করা হয়েছে, `sessionKey` ("admin_problems_sync") অপরিবর্তিত।
  `AdminProblemsView(` কল-সাইটে নতুন `isManualRefreshing = isRefreshing` প্যারামিটার যোগ হয়েছে
  (আগে `viewModel` দিয়ে whole-content flash হতো, এখন ভেতরের per-item pulse-এ যায়)।
- **`AdminProblemsView.kt`:**
  ১. নতুন `isManualRefreshing: Boolean = false` প্যারামিটার (ফাংশন সিগনেচারে)।
  ২. স্ক্রল-জাম্প ফিক্স: `LaunchedEffect(safePage) { listState.scrollToItem(0) }` →
     `LaunchedEffect(currentPage) { ... }` (raw state-এ key, Users/Withdrawal/KYC-এর মতোই)।
     `paginatedProblems` গণনায় bounds-check এখনো `safePage` ব্যবহার করে, অপরিবর্তিত।
  ৩. নতুন `isFilterRefreshing` (Ground Rule ২০): `LaunchedEffect(searchQuery, selectedStatusFilter,
     currentPage)`-এ true → `delay(350L)` → `finally`-তে false।
  ৪. per-item `problemCardPulse = rememberFieldChangePulse(value = problem, isManualRefreshing =
     isManualRefreshing, sessionKey = "admin_problems_sync", viewModel = viewModel,
     flashOnReentry = false)` (Ground Rule ১৯) — `items(paginatedProblems, key = { it.id })`-এর
     ভেতরে, `Card(...)`-কে `PulsingValue(isUpdating = problemCardPulse || isFilterRefreshing) {
     Card(...) }` দিয়ে মোড়ানো হয়েছে। সার্চ বার/স্ট্যাটাস-ফিল্টার চিপ/pagination bar
     (`LazyColumn`-এর বাইরে) স্পর্শ করা হয়নি।
  ৫. নতুন import: `PulsingValue`, `rememberFieldChangePulse`, `kotlinx.coroutines.delay`।

কোনো বিদ্যমান ফাংশনালিটি (স্ট্যাটাস-আপডেট, বাজেট-এডিট, বিড-ভিউ, চ্যাট-ভিউ, সলভার-রিঅ্যাসাইন,
ডিলিট-কনফার্ম, সার্চ, স্ট্যাটাস-ফিল্টার, pagination) বদলানো হয়নি — শুধু cold-load/pulse presentation
+ scroll-trigger-key যোগ/বদল হয়েছে।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. brace/paren-balance: `AdminProblemsView.kt` `{}` ৩৭২=৩৭২, `()` ৯৭১=৯৭১ (উভয়ই মিলেছে)।
   `AdminPanelScreen.kt` `{}` ২৩৩=২৩৩, `()` ৫১১=৫১১ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safePage)` আর নেই (এখন `LaunchedEffect(currentPage)`), ঠিক ১টা
   `isFilterRefreshing` state + effect, ঠিক ১টা `PulsingValue(isUpdating = problemCardPulse ||
   isFilterRefreshing)` কল। `AdminProblemsView(` কল-সাইট (`AdminPanelScreen.kt`) এখনো ঠিক একটাই।
৩. zip-এর ফাইল-সংখ্যা মূল আপলোডের সাথে মিলিয়ে দেখা হয়েছে (২৯২টা এন্ট্রি, dotfile সহ) — শুধু
   `AdminProblemsView.kt` + `AdminPanelScreen.kt` + এই দুটো `.md` ফাইল বদলেছে।
৪. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: প্রথমবার
   সমস্যাসমূহ ট্যাবে ঢুকলে পুরো ট্যাব skeleton হচ্ছে কিনা; re-entry-তে পুরো ট্যাব flash না হয়ে শুধু
   কার্ড pulse করছে কিনা; সার্চ/স্ট্যাটাস-ফিল্টার/pagination বদলালে দৃশ্যমান সব কার্ড একসাথে ছোট্ট
   pulse করছে কিনা (হেডার/সার্চ-বার/ফিল্টার-চিপ/pagination bar স্থির থাকছে কিনা); কোনো একটা সমস্যা
   ডিলিট হয়ে totalPages কমে গেলে জোর করে টপে scroll না হয়ে বর্তমান position ঠিক থাকছে কিনা; সাধারণ
   next/prev ক্লিকে এখনো ঠিকমতো টপে scroll হচ্ছে কিনা; স্ট্যাটাস-আপডেট/বাজেট-এডিট/ডিলিট action-গুলো
   আগের মতোই কাজ করছে কিনা (কোনো stuck-shimmer না)।

### পরবর্তী সেশন
সেশন ২.১৯ — Escrow (`AdminEscrowView.kt`, ইনডেক্স ৭)। শুরুর আগে একই দুটো প্রশ্ন (Ground Rule ১৬)
জিজ্ঞাসা করতে হবে, আর Ground Rule ১৮+২০ একসাথে (একই সেশনে) প্রয়োগ করতে হবে।

## ✅ সেশন ২.১৯ — Escrow (AdminEscrowView.kt, ইনডেক্স ৭): Ground Rule ১৮+১৯+২০ একসাথে retrofit — সম্পূর্ণ

### ব্যবহারকারীর সিদ্ধান্ত (কনফার্মড, Ground Rule ১৬)
(ক) শুধু এসক্রো তালিকার কার্ড (৩টা সাব-ট্যাব — HELD/RELEASED/REFUNDED — সবগুলোতেই) pulse করবে;
সামারি স্ট্যাট (Total Held/Stuck count/Released/Refunded amount) স্থির থাকবে, pulse-এর অংশ না।
(খ) pull-to-refresh (`isManualRefreshing`) দরকার। ২.১৮ (Problems)-এর নির্দেশ অনুযায়ী Ground Rule
১৮ আর ২০ একই সেশনে একসাথে করা হয়েছে, ভাগ করা হয়নি।

### গ্রেপ-ভেরিফিকেশন (শুরুর আগে)
`AdminEscrowView.kt`-এ আগে থেকেই `coerceIn(1,` (safePage) + `scrollToItem(0)` ছিল (৩টা সাব-ট্যাব
শেয়ার্ড pagination — একটাই `currentPage`, ট্যাব বদলে/সার্চ বদলে ১-এ রিসেট হয়) কিন্তু কোনো
`PulsingValue`/`rememberFieldChangePulse` ছিল না — Ground Rule ১৯/২০ দুটোই প্রথম থেকে বসাতে হয়েছে।
`AdminPanelScreen.kt`-এর ইনডেক্স ৭-এ `SyncAwareRefreshableContent(data = listOf(allHeldEscrows,
allReleasedEscrows, allRefundedEscrows, allUsers), ...)` — পুরো তালিকা+সামারি একসাথে flash করাতো।

### যা করা হয়েছে
- **`AdminPanelScreen.kt` (ইনডেক্স ৭):** `SyncAwareRefreshableContent` সরিয়ে plain `SyncAwareContent`
  (rule ১+২, `data`-বিহীন) দিয়ে wrap করা হয়েছে, `sessionKey` ("admin_escrow_sync") অপরিবর্তিত,
  skeleton `ListScreenSkeleton(tint = SomadhanAdminSlate)`। `AdminEscrowView(` কল-সাইটে নতুন
  `isManualRefreshing = isRefreshing` প্যারামিটার যোগ হয়েছে।
- **`AdminEscrowView.kt`:**
  ১. নতুন `isManualRefreshing: Boolean = false` প্যারামিটার (ফাংশন সিগনেচারে)।
  ২. স্ক্রল-জাম্প ফিক্স: `LaunchedEffect(safePage, selectedEscrowTab) { escrowListState.scrollToItem(0) }`
     → `LaunchedEffect(currentPage, selectedEscrowTab) { ... }` (raw state-এ key, `selectedEscrowTab`
     অপরিবর্তিত রাখা হয়েছে যেহেতু ট্যাব বদল একটা genuine navigation change)।
  ৩. নতুন শেয়ার্ড `isFilterRefreshing` (Ground Rule ২০): `LaunchedEffect(searchQuery,
     selectedEscrowTab, currentPage)`-এ true → `delay(350L)` → `finally`-তে false।
  ৪. তিনটা সাব-ট্যাবের `items(paginatedEscrows, key = { it.id })`-এর প্রতিটার ভেতরে আলাদা per-item
     `escrowCardPulse = rememberFieldChangePulse(value = escrow, isManualRefreshing =
     isManualRefreshing, sessionKey = "admin_escrow_sync", viewModel = viewModel, flashOnReentry =
     false)` (Ground Rule ১৯) — `Card(...)`-কে `PulsingValue(isUpdating = escrowCardPulse ||
     isFilterRefreshing) { Card(...) }` দিয়ে মোড়ানো হয়েছে। সার্চ বার/সাব-ট্যাব হেডার/সামারি স্ট্যাট
     (Total Held/Stuck count/Released/Refunded amount)/pagination bar (`LazyColumn`-এর বাইরে)
     স্পর্শ করা হয়নি।
  ৫. নতুন import: `PulsingValue`, `rememberFieldChangePulse`, `kotlinx.coroutines.delay`।

কোনো বিদ্যমান ফাংশনালিটি (release/refund action, সার্চ, ৩-ট্যাব সুইচ, pagination) বদলানো হয়নি —
শুধু cold-load/pulse presentation + scroll-trigger-key যোগ/বদল হয়েছে।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. brace/paren-balance: `AdminEscrowView.kt` `{}` ৩৫৪=৩৫৪, `()` ৯৫৩=৯৫৩ (উভয়ই মিলেছে)।
   `AdminPanelScreen.kt` `{}` ২৩৪=২৩৪, `()` ৫১১=৫১১ (উভয়ই মিলেছে)।
২. grep-ভেরিফাই: `LaunchedEffect(safePage` আর নেই (এখন `LaunchedEffect(currentPage,
   selectedEscrowTab)`), ঠিক ১টা `isFilterRefreshing` state + effect (৩ জায়গায় ব্যবহৃত), ঠিক ৩টা
   `PulsingValue(isUpdating = escrowCardPulse || isFilterRefreshing)` কল (HELD/RELEASED/REFUNDED
   প্রতিটাতে একটা করে)। `AdminEscrowView(` কল-সাইট (`AdminPanelScreen.kt`) এখনো ঠিক একটাই।
৩. zip-এর ফাইল-লিস্ট মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে (dotfile সহ ২৪৯টা ফাইল, শুধু
   ডিরেক্টরি-এন্ট্রিতে পার্থক্য) — শুধু `AdminEscrowView.kt` + `AdminPanelScreen.kt` + এই progress
   ফাইল বদলেছে।
৪. drawer group ম্যাপিং/navigation স্পর্শ করা হয়নি।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: প্রথমবার
   Escrow ট্যাবে ঢুকলে পুরো ট্যাব skeleton হচ্ছে কিনা; re-entry-তে পুরো ট্যাব flash না হয়ে শুধু কার্ড
   pulse করছে কিনা; সার্চ/সাব-ট্যাব-সুইচ/pagination বদলালে দৃশ্যমান সব কার্ড একসাথে ছোট্ট pulse করছে
   কিনা (হেডার/সাব-ট্যাব বার/সামারি স্ট্যাট/pagination bar স্থির থাকছে কিনা); কোনো এসক্রো
   release/refund হয়ে totalPages কমে গেলে জোর করে টপে scroll না হয়ে বর্তমান position ঠিক থাকছে কিনা;
   সাধারণ next/prev ক্লিকে এখনো ঠিকমতো টপে scroll হচ্ছে কিনা; release/refund action-গুলো আগের মতোই
   কাজ করছে কিনা (কোনো stuck-shimmer না)।

### পরবর্তী সেশন
~~সেশন ২.২০ — লেনদেন হিস্ট্রি...~~ → নিচের ২.১৯.১ এন্ট্রি দেখো, অগ্রাধিকার বদলেছে।

## ⏸️ সেশন ২.১৯.১ — Ground Rule ২১ (নতুন): realtime-এ নতুন যোগ হওয়া ডেটার pulse-গ্যাপ — ডায়াগনসিস + অগ্রাধিকার কনফার্মেশন (সম্পূর্ণ, কোনো কোড এডিট হয়নি)

### প্রেক্ষাপট (ব্যবহারকারীর প্রশ্ন থেকে ধরা পড়েছে)
ব্যবহারকারী জিজ্ঞাসা করেন — Users ট্যাবে থাকা অবস্থায় realtime-এ একজন নতুন ইউজার যুক্ত হলে সেটা কি
pulse করে যুক্ত হবে, নাকি এমনিই যুক্ত হয়ে যাবে (অন্য সব per-item পেজের ক্ষেত্রেও একই প্রশ্ন)।
পরীক্ষা করে দেখা গেছে — বর্তমান per-item ডিজাইনে (Ground Rule ১৯) নতুন realtime item **কোনো
pulse ছাড়াই চুপচাপ** তালিকায় যুক্ত হয়ে যায় (কারণ `rememberFieldChangePulse`-এর তুলনা আগের
value-এর বিপরীতে হয়, নতুন item-এর কোনো আগের value নেই)। ব্যবহারকারী কনফার্ম করেছেন এটা ঠিক না —
**শুধু নতুন ডেটার কার্ড/ভ্যালুই pulse করা উচিত**, বাকি তালিকা স্থির থাকবে।

### ব্যবহারকারীর নির্দেশ (কনফার্মড)
এই গ্যাপ **এখনই ঠিক করা হবে না** — বরং `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ Ground Rule ২১
হিসেবে ডকুমেন্ট করে রাখতে হবে, যাতে পরের Claude সেশন চ্যাট-হিস্ট্রি ছাড়াই এটা জানতে পারে আর
**সবার আগে** (বাকি pending সেশন — ২.২০ থেকে শুরু — এর আগে) এটা নিয়ে কাজ করে।

### যা করা হয়েছে (শুধু ডকুমেন্টেশন, কোনো `.kt` ফাইল স্পর্শ হয়নি)
১. `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এ নতুন **Ground Rule ২১** যোগ করা হয়েছে (Ground Rule
   ২০-এর ঠিক পরে) — গ্যাপের বর্ণনা, কাঙ্ক্ষিত আচরণ, কেন এটা সহজ ফিক্স না (স্ক্রল-এ প্রথমবার
   viewport-এ আসা আইটেম বনাম সত্যিকার realtime-নতুন আইটেম — কম্পোজেবলের দৃষ্টিকোণ থেকে আলাদা করা
   কঠিন, একটা সম্ভাব্য দিকনির্দেশ নোট করা হয়েছে কিন্তু চূড়ান্ত ডিজাইন না), স্কোপ (Users/KYC/
   Withdrawal/Problems/Escrow + ভবিষ্যতের per-item ট্যাব), আর অগ্রাধিকার (সবার আগে, confirm/সমাধান
   না হওয়া পর্যন্ত অন্য কোনো pending সেশনে যাওয়া যাবে না) — সব লেখা হয়েছে।
২. ধাপ ৪-এর "পরবর্তী Claude সেশনের জন্য সাজেস্টেড অর্ডার" প্যারাগ্রাফ আপডেট করে সবার প্রথমে
   Ground Rule ২১ বসানো হয়েছে (তারপর ২.২০-২.২৫, তারপর ২.২৬)।
৩. এই progress ফাইলে এই এন্ট্রি যোগ করা হয়েছে।

### যাচাই
কোনো `.kt` ফাইল স্পর্শ হয়নি — শুধু `ADMIN_PANEL_LOADING_MASTER_PROMPT.md` + এই progress ফাইল
বদলেছে। zip-এর ফাইল-সংখ্যা অপরিবর্তিত (২৪৯টা, শুধু এই দুটো `.md`-এর কনটেন্ট বদলেছে)।

### পরবর্তী সেশন
**Ground Rule ২১** — প্রথমে ব্যবহারকারীর সাথে ঠিক কীভাবে "realtime-নতুন" বনাম "স্ক্রল-এ প্রথমবার
viewport" আলাদা করা হবে তার ডিজাইন কনফার্ম করা (Ground Rule ১৬, উপরের সম্ভাব্য দিকনির্দেশ শুধু
একটা প্রস্তাব, চূড়ান্ত না), তারপর Users → KYC → Withdrawal → Problems → Escrow-এ প্রয়োগ। এটা
সম্পন্ন হওয়ার পরই সেশন ২.২০ (লেনদেন হিস্ট্রি) শুরু হবে।

## ⏸️ সেশন ২.১৯.২ — Ground Rule ২১-এর স্কোপ সব pending সেশনে স্পষ্টভাবে সম্প্রসারণ (শুধু ডকুমেন্টেশন, কোনো কোড এডিট হয়নি)

### প্রেক্ষাপট (ব্যবহারকারীর প্রশ্ন থেকে ধরা পড়েছে)
ব্যবহারকারী জিজ্ঞাসা করেন — সেশন ২.১৯.১-এ যোগ করা Ground Rule ২১ (realtime-এ নতুন ডেটার pulse)
নির্দেশটা শুধু Users/KYC/Withdrawal/Problems/Escrow (৫টা ইতিমধ্যে-সম্পন্ন ট্যাব)-এ সীমাবদ্ধ ছিল
লেখায় — কিন্তু বাকি pending সেশনগুলোতেও (২.২০-২.২৫, ২.২৬, আর ভবিষ্যতের ২.৩-২.১৪ per-item pulse)
এই একই নিয়ম প্রযোজ্য হওয়া উচিত, যেহেতু ওই ট্যাবগুলোতেও per-item pulse (Ground Rule ১৯) নতুন করে
বসানো হবে বলে একই গ্যাপ তৈরি হবে। এটা ঠিক Ground Rule ২০-এর "স্কোপ সম্প্রসারণ" নোটের (সেশন
২.২৮-এর পরে যোগ করা) মতোই একটা স্পষ্টীকরণ।

### ব্যবহারকারীর নির্দেশ (কনফার্মড)
Ground Rule ২১ শুধু ৫টা তালিকাভুক্ত ট্যাবে না, **যেকোনো ডেটা পেজে যেখানে realtime নতুন ডেটা আসতে
পারে** তার সবকটাতেই প্রযোজ্য হতে হবে — বর্তমান pending সেশন (২.২০-২.২৬) সহ। যখনই কোনো সেশনে প্রথমবার
Ground Rule ১৯ (per-item pulse) বসানো হচ্ছে, সেই একই সেশনে Ground Rule ২১-ও বসাতে হবে (ডিজাইন
একবার Users/KYC/Withdrawal/Problems/Escrow-এ কনফার্ম হয়ে গেলে সেই প্যাটার্ন বাকি সব জায়গায় reuse
হবে) — GR18+GR20-এর মতোই একই সেশনে bundling, আলাদা করে ফেলে রাখা যাবে না।

### যা করা হয়েছে (শুধু ডকুমেন্টেশন, কোনো `.kt` ফাইল স্পর্শ হয়নি)
১. `ADMIN_PANEL_LOADING_MASTER_PROMPT.md`-এর Ground Rule ২১-এর সংজ্ঞায় (item ২১, "স্কোপ" প্যারার
   ঠিক পরে) একটা নতুন "স্কোপ স্পষ্টীকরণ" প্যারাগ্রাফ যোগ করা হয়েছে — স্পষ্ট করে বলা হয়েছে এটা
   সেশন ২.২০-২.২৫, ২.২৬, আর ভবিষ্যতের ২.৩-২.১৪ per-item retrofit-এও প্রযোজ্য, আর bundling নিয়মটা
   (GR১৯ বসানোর সেশনেই GR২১ বসাতে হবে) লেখা হয়েছে।
২. "সেশন ২.১৫ থেকে ২.২৫" টেমপ্লেট সেকশনে, বিদ্যমান Ground Rule ২০ ওভারল্যাপ নোটের ঠিক পরে, একই
   স্টাইলে একটা নতুন "Ground Rule ২১ ওভারল্যাপ" নোট যোগ করা হয়েছে — ৬টা বাকি ট্যাব (২.২০-২.২৫)
   নাম করে বলা হয়েছে GR১৯ বসানোর সাথে সাথেই GR২১-ও একই সেশনে বসাতে হবে।
৩. ধাপ ৪-এর "পরবর্তী Claude সেশনের জন্য সাজেস্টেড অর্ডার" প্যারাগ্রাফ আপডেট করা হয়েছে — সেশন
   ২.২০-২.২৫-এর তালিকা (আগে ভুলবশত পুরনো ৭-ট্যাব/ইনডেক্স তালিকা রয়ে গিয়েছিল, Escrow/২.১৯ সম্পন্ন
   হওয়ার পরের সঠিক ৬-ট্যাব তালিকা দিয়ে ঠিক করা হয়েছে) আর GR১৮+১৯+২০+২১ বান্ডলিং নিয়ম স্পষ্ট করে
   লেখা হয়েছে, আর ২.২৬ ও ২.৩-২.১৪-এর জন্যও একই নিয়ম প্রযোজ্য তা যোগ করা হয়েছে।
৪. এই progress ফাইলে এই এন্ট্রি যোগ করা হয়েছে।

### যাচাই
কোনো `.kt` ফাইল স্পর্শ হয়নি — শুধু `ADMIN_PANEL_LOADING_MASTER_PROMPT.md` + এই progress ফাইল
বদলেছে। কোনো existing rule/ট্যাব-ইনডেক্স/ফাংশনালিটি বর্ণনা বদলানো হয়নি, শুধু GR২১-এর স্কোপ স্পষ্ট
করা হয়েছে আর একটা স্টেল তালিকা ঠিক করা হয়েছে।

### পরবর্তী সেশন
অপরিবর্তিত — এখনো সবার আগে Ground Rule ২১-এর ডিজাইন Users/KYC/Withdrawal/Problems/Escrow-এ
কনফার্ম+implement করতে হবে (Ground Rule ১৬), তারপর সেশন ২.২০ (চ্যাট মনিটরিং) থেকে যথারীতি শুরু —
পার্থক্য শুধু এটুকু যে ২.২০ থেকে ২.২৬ পর্যন্ত প্রতিটা সেশনে GR১৯ বসানোর সাথে GR২১-ও এখন থেকে
বাধ্যতামূলকভাবে বান্ডল করে বসাতে হবে, আলাদা সেশনের জন্য রাখা যাবে না।

## ✅ সেশন ২.১৯.৩ — Ground Rule ২১ ডিজাইন + Users-এ প্রথম implementation (সম্পূর্ণ)

### ডিজাইন (কোড পড়ে ভেরিফাই করে বের করা হয়েছে, অনুমান করা হয়নি)
`SupabaseRealtimeManager.kt`-এর `handleUserAction()` ইতিমধ্যেই `PostgresAction.Insert` কে
`Update`/`Delete` থেকে আলাদা করে ধরে (users channel, `startRealtimeListeners()`-এর পরে সাবস্ক্রাইব
হয়)। আর `pullBulkDataFromSupabase()` (cold-load/bulk snapshot, `attachDatabase()`-এর প্রথম ধাপ)
সম্পূর্ণ আলাদা কোড-পাথ — এটা কখনো `handleUserAction()`/`PostgresAction.Insert` দিয়ে যায় না। এই
দুটো প্রাকৃতিক বিভাজনই মাস্টার প্রম্পটের "কীভাবে realtime-নতুন বনাম স্ক্রল-এ-প্রথমবার আলাদা করা
হবে" প্রশ্নের উত্তর — কোনো নতুন heuristic লাগেনি, বরং বিদ্যমান কোড-স্ট্রাকচারই এই দুই case-কে
আলাদা করে দেয়।

**চূড়ান্ত ডিজাইন:** `PostgresAction.Insert` হ্যান্ডলারে (শুধু genuine realtime insert, bulk-pull
না) সেই row-এর id একটা `MutableStateFlow<Set<String>>`-এ ২০০০ms-এর জন্য যোগ হয় (TTL শেষে নিজে
থেকে সরে যায়, table-agnostic `markRecentlyInserted()` হেল্পার দিয়ে)। ViewModel এই StateFlow
সরাসরি এক্সপোজ করে। UI-তে (`AdminUsersView.kt`) প্রতিটা per-item `PulsingValue`-এর `isUpdating`-এ
`userCardPulse || isFilterRefreshing || isNewFromRealtime` (set-membership check) OR করা হয়েছে —
বিদ্যমান `rememberFieldChangePulse`/`isFilterRefreshing` লজিক স্পর্শ করা হয়নি, শুধু তৃতীয় একটা
স্বাধীন কারণ যোগ হয়েছে।

### যা এডিট হয়েছে
১. `SupabaseRealtimeManager.kt` — `NEW_ITEM_PULSE_TTL_MS` কনস্ট্যান্ট, `_recentlyInsertedUserIds`
   StateFlow, table-agnostic `markRecentlyInserted()` হেল্পার যোগ করা হয়েছে; `handleUserAction()`-এর
   `PostgresAction.Insert` branch-এ একটা কল যোগ করা হয়েছে (Update/Delete branch অপরিবর্তিত)।
২. `SomadhanViewModel.kt` — `recentlyInsertedUserIds: StateFlow<Set<String>>` এক্সপোজ করা হয়েছে
   (`initialSyncPhase`-এর প্যাটার্নেই, object singleton থেকে সরাসরি)।
৩. `AdminUsersView.kt` — `viewModel.recentlyInsertedUserIds.collectAsStateWithLifecycle()` যোগ
   করা হয়েছে, `items(paginatedUsers)`-এর ভেতরে `isNewFromRealtime` ভ্যারিয়েবল + বিদ্যমান
   `PulsingValue(isUpdating = ...)`-এ OR করা হয়েছে।

কোনো বিদ্যমান ফাংশনালিটি/action/pagination/filter লজিক বদলায়নি — শুধু নতুন independent pulse-কারণ
যোগ হয়েছে।

### যাচাই (real-device build এই sandbox-এ সম্ভব না — নিচেরগুলো কোড-লেভেলে grep/read করে
ভেরিফাই করা হয়েছে, real device-এ পরীক্ষা এখনো বাকি)
- তিনটা ফাইলেই brace/paren balance চেক করা হয়েছে (edited ফাইলগুলোতে `{`/`}` আর `(`/`)` কাউন্ট
  সমান)।
- `handleUserAction()`-এর `Update`/`Delete` branch অপরিবর্তিত আছে তা diff করে নিশ্চিত করা হয়েছে।
- `UserDto.id` ফিল্ড (non-null `String`) সরাসরি ব্যবহারযোগ্য তা DTO ক্লাস দেখে নিশ্চিত করা হয়েছে।
- `managerScope` ফাইলে পরে declare হলেও (`markRecentlyInserted()` ফাংশন-বডির ভেতর থেকে রেফারেন্স)
  Kotlin object-এ এটা সমস্যা না (function body lazy-resolved) — কনফার্ম করা হয়েছে।

### পরবর্তী সেশনের জন্য যা এখনো বাকি (real device testing, বাধ্যতামূলক আগে)
এই কোড sandbox-এ build/run করা যায়নি (কোনো real Android build এই পরিবেশে সম্ভব না, ধাপ ৩-এর
চেকলিস্ট-আইটেম অনুযায়ী মনে করিয়ে দেওয়া হলো)। পরের সেশন/ব্যবহারকারীকে real device-এ verify করতে হবে:
(ক) Users ট্যাবে থাকা অবস্থায় realtime-এ একজন নতুন ইউজার সাইন-আপ করলে সেই কার্ড pulse করে যুক্ত
হচ্ছে কিনা (বাকি কার্ড স্থির থাকছে কিনা); (খ) ২০০০ms TTL ফিল-ভালো লাগছে কিনা (খুব দ্রুত/ধীর মনে
হলে `NEW_ITEM_PULSE_TTL_MS` টিউন করা যাবে); (গ) স্বাভাবিক স্ক্রলে (নতুন realtime insert ছাড়া)
কোনো card ভুলভাবে pulse করছে কিনা (রিগ্রেশন চেক)। এই তিনটা কনফার্ম হলে **এই একই প্যাটার্ন
(markRecentlyInserted() reuse করে) KYC → Withdrawal → Problems → Escrow-এ প্রয়োগ করো** — প্রতিটায়
শুধু নিজস্ব `_recentlyInsertedXxxIds` StateFlow + সংশ্লিষ্ট `handleXxxAction()`-এর Insert branch-এ
একটা কল + সংশ্লিষ্ট View ফাইলে collectAsStateWithLifecycle + OR — Users-এর মতোই টেমপ্লেট, নতুন
ডিজাইন-আলোচনা লাগবে না। এই ৫টা সম্পন্ন হওয়ার পরই সেশন ২.২০ (চ্যাট মনিটরিং, Ground Rule
১৮+১৯+২০+২১ বান্ডল করে) শুরু হবে, যেমনটা মাস্টার প্রম্পটের বর্তমান "সাজেস্টেড অর্ডার"-এ লেখা আছে।

## ✅ সেশন ২.১৯.৪ — Ground Rule ২১ ডিজাইন + KYC-এ implementation (সম্পূর্ণ)

### ডায়াগনসিস (কোনো কোড এডিট না করে আগে পড়ে বের করা হয়েছে)
Users-এর Ground Rule ২১ implementation (সেশন ২.১৯.৩) genuine `PostgresAction.Insert` ইভেন্টের
উপর ভিত্তি করে করা হয়েছিল — কিন্তু KYC ট্যাবের pending/verified/rejected তালিকা কোনো আলাদা টেবিল
না, `users` টেবিলেরই `kyc_status`/`is_kyc_verified` ফিল্ড-ভিত্তিক filter (`AdminKycView.kt` লাইন
৩১১/৩৩০/৩৪৯ যাচাই করে নিশ্চিত করা হয়েছে)। তাই এখানে "নতুন" মানে Insert না — বরং কোনো user-এর
`kyc_status` **Update** event-এ pending/verified/rejected-এ ট্রানজিশন করা। এই অস্পষ্টতা Ground
Rule ১৬ অনুযায়ী ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে, তিনি কনফার্ম করেছেন: **kycStatus
pending/verified/rejected-এ ট্রানজিশন করলেই pulse** (শুধু যেকোনো Update-এ না, বা KYC বাদ দেওয়াও
না)।

আরেকটা টেকনিক্যাল বাধা পাওয়া গেছে: `users` টেবিলে `REPLICA IDENTITY FULL` কোনো migration-এ সেট
করা নেই (grep করে নিশ্চিত), তাই Supabase Postgres Changes payload-এর নিজস্ব `oldRecord`-এ Update
event-এ শুধু PK-ই থাকে (`handleUserAction()`-এর DELETE branch-এর বিদ্যমান কমেন্টও এটা নিশ্চিত
করে) — তাই payload থেকে সরাসরি আগের `kyc_status` পাওয়া যায় না। সমাধান: `mergeAndSaveUser()`
কল করে local Room overwrite হওয়ার **আগে** local row থেকে পুরনো `kycStatus` পড়ে নেওয়া হয়েছে,
তারপর নতুন (mapped) মানের সাথে তুলনা করা হয়েছে। `mergeAndSaveUser()`-এর নিজের সিগনেচার/লজিক
স্পর্শ করা হয়নি (Ground Rule ১)।

### যা এডিট হয়েছে
১. `SupabaseRealtimeManager.kt` — নতুন `_recentlyKycChangedUserIds` StateFlow (Users-এর
   `_recentlyInsertedUserIds`-এর প্যাটার্নেই, একই `markRecentlyInserted()`/
   `NEW_ITEM_PULSE_TTL_MS` reuse করে)। `handleUserAction()`-এর Update branch-এ:
   `mergeAndSaveUser()` কল করার আগে `database.userDao().getUserById(dto.id)?.kycStatus` দিয়ে
   পুরনো মান পড়া, তারপর `mapKycStatusFromSupabase(dto.kycStatus)` দিয়ে নতুন মান বের করে দুটো
   অমিল হলে আর নতুন মান pending/verified/rejected-এর একটা হলে `markRecentlyInserted()` কল করা।
   Insert/Delete branch অপরিবর্তিত।
২. `SomadhanViewModel.kt` — `recentlyKycChangedUserIds: StateFlow<Set<String>>` এক্সপোজ করা
   হয়েছে (`recentlyInsertedUserIds`-এর ঠিক নিচে, একই প্যাটার্নে, object singleton থেকে সরাসরি)।
৩. `AdminKycView.kt` — `viewModel?.recentlyKycChangedUserIds?.collectAsStateWithLifecycle() ?:
   remember { mutableStateOf(emptySet()) }` (এই ফাইলে `viewModel` ঐচ্ছিক/nullable বলে null-safe
   fallback সহ, Users-এ যেহেতু non-nullable সেখানে fallback লাগেনি)। তিনটা তালিকাতেই (Pending,
   Verified, Rejected) `items(...)`-এর ভেতরে `isNewFromRealtime = recentlyKycChangedUserIds
   .contains(solver.id)` যোগ করে বিদ্যমান `PulsingValue(isUpdating = solverCardPulse ||
   isFilterRefreshing)`-এ OR করা হয়েছে — বিদ্যমান `solverCardPulse`/`isFilterRefreshing` লজিক
   স্পর্শ করা হয়নি, শুধু তৃতীয় independent কারণ যোগ হয়েছে (Users-এর ঠিক একই approach)।

কোনো বিদ্যমান ফাংশনালিটি/approve/reject/revoke/bulk-approve/pagination/search লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. তিনটা এডিট করা ফাইলেই Kotlin-aware স্ক্রিপ্ট দিয়ে `{}`/`()`/`[]` ব্যালেন্স গোনা হয়েছে:
   `SupabaseRealtimeManager.kt` `{}` ৩৩৭=৩৩৭, `()` ৬৯০=৬৯০, `[]` ২৯=২৯। `SomadhanViewModel.kt`
   `{}` ১২৪৯=১২৪৯, `()` ২৩৯৮=২৩৯৮, `[]` ১২=১২। `AdminKycView.kt` `{}` ৩১৫=৩১৫, `()` ৮৭৩=৮৭৩,
   `[]` ০=০।
২. grep দিয়ে নিশ্চিত করা হয়েছে: `markRecentlyInserted()` এখন ঠিক দুটো কল-সাইটে (Users +
   KYC), `mapKycStatusFromSupabase` (একই প্যাকেজ, নতুন import লাগেনি) সঠিকভাবে ব্যবহৃত, আর
   `isNewFromRealtime` ঠিক তিনবার (Pending/Verified/Rejected প্রতিটা তালিকায় একবার) যোগ হয়েছে।
   `UserDao.getUserById()` আগে থেকেই বিদ্যমান (নতুন DAO ফাংশন লাগেনি)।
৩. ধাপ ০.১৪(ক)/(খ)-এর systemic প্যাটার্ন এই তিনটা ফাইলেই নতুন করে চেক করা হয়েছে — কোনো নতুন
   ঝুঁকি পাওয়া যায়নি (নতুন কোড কোনো `LaunchedEffect(data)`-মিড-ডিলে cancellation-প্রবণ effect
   বসায়নি, শুধু event-handler-এ একটা তুলনা আর আগে-থেকে-বিদ্যমান TTL-হেল্পার কল)।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. এই স্ক্রিন role-shared না (admin-only), তাই ধাপ ০.৫ প্রযোজ্য না।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা, dotfile সহ) মূল আপলোড করা zip-এর সাথে `zipfile` দিয়ে গুনে মিলিয়ে
   দেখা হয়েছে — কোনো ফাইল miss হয়নি, শুধু এই তিনটা `.kt` ফাইল আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: KYC
   ট্যাবে থাকা অবস্থায় কোনো solver-এর kyc_status realtime-এ (অন্য ডিভাইস/সরাসরি DB থেকে)
   pending/verified/rejected-এ বদলে গেলে সংশ্লিষ্ট কার্ড pulse করে সঠিক ট্যাবে যুক্ত হচ্ছে কিনা,
   ২০০০ms TTL ফিল ঠিক আছে কিনা, আর স্বাভাবিক স্ক্রল/অন্য user-field আপডেটে (kyc-অসম্পর্কিত) কোনো
   card ভুলভাবে pulse করছে কিনা (রিগ্রেশন চেক)।

### পরবর্তী সেশনের জন্য যা বাকি
real-device testing (উপরে) কনফার্ম হওয়ার পর একই প্যাটার্ন **Withdrawal → Problems → Escrow**-এ
প্রয়োগ করতে হবে — প্রতিটায় নিজস্ব `_recentlyXxxIds`/সংশ্লিষ্ট `handleXxxAction()`-এর Insert/Update
branch + View ফাইলে `collectAsStateWithLifecycle`/OR। Withdrawal-এর জন্যও "নতুন" কী মানে (নতুন
withdraw request Insert, নাকি status-transition Update, বা দুটোই) — এটা genuinely নতুন সিদ্ধান্ত,
অনুমান না করে সেশন শুরুতেই জিজ্ঞাসা করতে হবে (Ground Rule ১৬)। এই ৪টা সম্পন্ন হওয়ার পরই সেশন ২.২০
(চ্যাট মনিটরিং, Ground Rule ১৮+১৯+২০+২১ বান্ডল করে) শুরু হবে।

## ✅ সেশন ২.১৯.৫ — Ground Rule ২১ ডিজাইন + Withdrawal-এ implementation (সম্পূর্ণ)

### ডায়াগনসিস (কোনো কোড এডিট না করে আগে পড়ে বের করা হয়েছে)
`AdminWithdrawalsView.kt` KYC-এর মতো কোনো অন্য টেবিলের filter না — `withdrawals` নিজেই একটা
আসল টেবিল, `handleWithdrawalAction()`-এ Insert **আর** Update দুটোই genuine ঘটনা (solver নতুন
withdraw request করলে Insert, status=PENDING দিয়ে শুরু; admin approve/reject করলে Update, status
COMPLETED/REJECTED-এ বদলায়)। তাই এখানে Users (Insert-only) আর KYC (Update-transition-only) —
দুটো প্যাটার্নের মিশ্রণ দরকার, কোনো একটা একা যথেষ্ট না। এই অস্পষ্টতা Ground Rule ১৬ অনুযায়ী
ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে, তিনি কনফার্ম করেছেন: **দুটোই** — নতুন PENDING request (Insert)
আর COMPLETED/REJECTED-এ status transition (Update)।

`withdrawals` টেবিলেও `REPLICA IDENTITY FULL` নেই (grep-ভেরিফাইড, KYC-এর মতোই) — তাই Update-এর
আগের status payload-এর `oldRecord` থেকে পাওয়া যায় না, local Room থেকে overwrite-এর আগে পড়তে
হয়েছে (KYC-এর ঠিক একই approach)। পার্থক্য: `WithdrawalDto.status`/`WithdrawalEntity.status`
সরাসরি uppercase (`PENDING`/`COMPLETED`/`REJECTED`) — KYC-এর মতো কোনো lowercase-mapping
(`mapKycStatusFromSupabase`-জাতীয়) দরকার হয়নি, সরাসরি স্ট্রিং তুলনা।

### যা এডিট হয়েছে
১. `SupabaseRealtimeManager.kt` — নতুন `_recentlyChangedWithdrawalIds` StateFlow (একই সেট Insert
   আর Update দুটো কেসের জন্যই ব্যবহার হয়েছে, আলাদা করার দরকার হয়নি — UI-তে দুটোই একই pulse)।
   `handleWithdrawalAction()`-এর Insert branch-এ সরাসরি `markRecentlyInserted()` কল (Users-এর
   প্যাটার্নেই)। Update branch-এ `mergeAndSave`-এর আগে (এখানে সরাসরি `insertWithdrawal()`, কোনো
   আলাদা merge ফাংশন নেই) local `getWithdrawalById(dto.id)?.status` পড়ে নতুন `dto.status`-এর
   সাথে তুলনা করে COMPLETED/REJECTED-এ বদলালে মার্ক করা। Delete branch অপরিবর্তিত।
২. `SomadhanViewModel.kt` — `recentlyChangedWithdrawalIds: StateFlow<Set<String>>` এক্সপোজ করা
   হয়েছে (Users/KYC-এর ঠিক নিচে, একই প্যাটার্নে)।
৩. `AdminWithdrawalsView.kt` — `viewModel?.recentlyChangedWithdrawalIds?.collectAsStateWithLifecycle()
   ?: remember { mutableStateOf(emptySet()) }` (KYC-এর null-safe প্যাটার্নেই, এই ফাইলেও
   `viewModel` ঐচ্ছিক)। একটামাত্র per-item লুপ (`items(paginatedWithdrawals, ...)`)-এ
   `isNewFromRealtime = recentlyChangedWithdrawalIds.contains(item.id)` যোগ করে বিদ্যমান
   `PulsingValue(isUpdating = withdrawalCardPulse || isFilterRefreshing)`-এ OR করা হয়েছে।

কোনো বিদ্যমান ফাংশনালিটি/approve/reject/bulk-approve/pagination/search/filter লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. Kotlin-aware স্ক্রিপ্ট দিয়ে তিনটা ফাইলেই `{}`/`()`/`[]` ব্যালেন্স গোনা হয়েছে:
   `SupabaseRealtimeManager.kt` `{}` ৩৩৮=৩৩৮, `()` ৬৯৯=৬৯৯, `[]` ২৯=২৯। `SomadhanViewModel.kt`
   `{}` ১২৪৯=১২৪৯, `()` ২৩৯৮=২৩৯৮, `[]` ১২=১২। `AdminWithdrawalsView.kt` `{}` ২৬৫=২৬৫,
   `()` ৬৯৪=৬৯৪, `[]` ০=০।
২. grep দিয়ে নিশ্চিত করা হয়েছে: `markRecentlyInserted()` এখন Users/KYC-এর পরে Withdrawal-এর
   Insert+Update দুই জায়গায় (মোট ৪টা কল-সাইট), `getWithdrawalById()` আগে থেকেই DAO-তে বিদ্যমান
   (নতুন DAO ফাংশন লাগেনি), `isNewFromRealtime` ঠিক একবার (একটামাত্র per-item লুপ) যোগ হয়েছে,
   `PulsingValue` কল-সাইট এখনো ঠিক একটাই আছে (নতুন কোনো ডুপ্লিকেট তৈরি হয়নি)।
৩. ধাপ ০.১৪(ক)/(খ)-এর systemic প্যাটার্ন নতুন কোডে চেক করা হয়েছে — কোনো নতুন ঝুঁকি নেই (শুধু
   event-handler তুলনা + আগে-থেকে-বিদ্যমান TTL-হেল্পার কল, নতুন কোনো cancellation-প্রবণ
   `LaunchedEffect(data)` বসেনি)।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. এই স্ক্রিন role-shared না (admin-only), ধাপ ০.৫ প্রযোজ্য না।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা, dotfile সহ) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু এই
   তিনটা `.kt` ফাইল আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   Withdrawal ট্যাবে থাকা অবস্থায় (ক) কোনো solver নতুন withdraw request করলে card pulse করে
   Pending তালিকায় যুক্ত হচ্ছে কিনা, (খ) admin অন্য ডিভাইস থেকে approve/reject করলে সংশ্লিষ্ট
   card pulse করছে কিনা, (গ) স্বাভাবিক স্ক্রল/অন্য field-আপডেটে ভুলভাবে pulse হচ্ছে না কিনা।

### পরবর্তী সেশনের জন্য যা বাকি
real-device testing কনফার্ম হওয়ার পর একই প্যাটার্ন **Problems → Escrow**-এ প্রয়োগ করতে হবে —
প্রতিটার জন্য "নতুন" কী মানে (Insert-only/Update-transition-only/দুটোই) genuinely নতুন সিদ্ধান্ত,
Ground Rule ১৬ অনুযায়ী অনুমান না করে প্রতিটা সেশনের শুরুতেই জিজ্ঞাসা করতে হবে। এই ২টা সম্পন্ন
হওয়ার পরই সেশন ২.২০ (চ্যাট মনিটরিং, Ground Rule ১৮+১৯+২০+২১ বান্ডল করে) শুরু হবে।

## ✅ সেশন ২.১৯.৬ — Ground Rule ২১ ডিজাইন + Problems ও Escrow-এ implementation (সম্পূর্ণ)

### ডায়াগনসিস (কোনো কোড এডিট না করে আগে পড়ে বের করা হয়েছে, তারপর ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে)
প্রথমে গ্রেপ করে কনফার্ম করা হয়েছে (মাস্টার প্রম্পটের "আপডেট ২" নোট stale ছিল, কোডে ইতিমধ্যেই আপ-টু-ডেট):
`AdminPanelScreen.kt`-এর ইনডেক্স ৫ (Problems, সেশন ২.১৮) আর ইনডেক্স ৭ (Escrow) — দুটোই ইতিমধ্যে
প্লেইন `SyncAwareContent` (GR18) আর `AdminProblemsView.kt`/`AdminEscrowView.kt`-এর ভেতরে GR19
(per-item `PulsingValue`) + GR20 (`isFilterRefreshing`, `coerceIn(1,`, `scrollToItem(0)`) পূর্ণাঙ্গ
আছে — শুধু GR21 (`isNewFromRealtime`/`markRecentlyInserted`) বাকি ছিল (grep করে কোনো call-site
পাওয়া যায়নি)।

Ground Rule ১৬ অনুযায়ী ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে — দুটোতেই উত্তর **"দুটোই" (Insert + Update)**:
- **Problems:** নতুন problem post (Insert) আর status transition (Update) — কোনো নির্দিষ্ট
  সাবসেট-স্ট্যাটাস বলা হয়নি, তাই যেকোনো status বদলেই (OPEN/IN_PROGRESS/COMPLETED/CANCELLED-এর
  মধ্যে) pulse হবে বলে implement করা হয়েছে।
- **Escrow:** নতুন escrow তৈরি (Insert, Held-এ) আর status transition (Update, HELD→RELEASED/
  REFUNDED) — এটাই escrow-এর একমাত্র সম্ভাব্য transition, তাই "যেকোনো status বদল" আর "RELEASED/
  REFUNDED-এ transition" কার্যত একই।

উভয় টেবিলেই (`problems`/`escrows`) `REPLICA IDENTITY FULL` নেই (grep-ভেরিফাইড, KYC/Withdrawal-এর
মতোই) — তাই Update-এর আগের status payload-এর `oldRecord` থেকে পাওয়া যায় না, local Room থেকে
overwrite-এর আগে পড়তে হয়েছে (একই approach)। Escrow-এর ক্ষেত্রে একটা অতিরিক্ত জটিলতা:
`mergeAndSaveEscrow()` নিজেই `resolveIncomingEscrowStatus()` দিয়ে চূড়ান্ত status resolve করে
(raw `dto.status` না) — তাই তুলনা raw dto না, resolve হওয়ার পরের entity.status দিয়ে করা হয়েছে।

### যা এডিট হয়েছে
১. **`SupabaseRealtimeManager.kt`** — দুটো নতুন StateFlow: `_recentlyChangedProblemIds`,
   `_recentlyChangedEscrowIds` (Withdrawal-এর প্যাটার্নেই, একই সেট Insert+Update দুই কেসের জন্য)।
   `handleProblemAction()`-এর Insert branch-এ সরাসরি `markRecentlyInserted()`; Update branch-এ
   `mergeAndSaveProblem()` overwrite করার আগে local status পড়ে, তারপর নতুন entity.status-এর সাথে
   তুলনা করে অমিল হলে মার্ক। `handleEscrowAction()`-এও একই প্যাটার্ন, শুধু Update-এ resolve হওয়ার
   পরের entity.status তুলনা করা হয়েছে (raw dto.status না)। Delete branch দুটোতেই অপরিবর্তিত।
২. **`SomadhanViewModel.kt`** — `recentlyChangedProblemIds`/`recentlyChangedEscrowIds`
   StateFlow এক্সপোজ করা হয়েছে (Withdrawal-এর ঠিক নিচে, একই প্যাটার্নে)।
৩. **`AdminProblemsView.kt`** — `viewModel.recentlyChangedProblemIds.collectAsStateWithLifecycle()`
   যোগ, একটামাত্র per-item লুপে `isNewFromRealtime` + বিদ্যমান
   `PulsingValue(isUpdating = problemCardPulse || isFilterRefreshing)`-এ OR।
৪. **`AdminEscrowView.kt`** — `viewModel.recentlyChangedEscrowIds.collectAsStateWithLifecycle()`
   যোগ (একবার, তিনটা তালিকার জন্যই শেয়ার্ড — `selectedEscrowTab` একটাই state), তিনটা per-item
   লুপেই (Held/Released/Refunded) `isNewFromRealtime` + বিদ্যমান `PulsingValue`-তে OR।

কোনো বিদ্যমান ফাংশনালিটি/action/pagination/filter/search লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. চারটা এডিট করা ফাইলেই `{}`/`()`/`[]` ব্যালেন্স গোনা হয়েছে — সবগুলো মিলেছে (`SupabaseRealtimeManager.kt`
   {} ৩৫৩=৩৫৩, () ১০৭০=১০৭০; `SomadhanViewModel.kt` {} ১৩০৮=১৩০৮, () ২৭৬৬=২৭৬৬;
   `AdminProblemsView.kt` {} ৩৭২=৩৭২, () ৯৭৭=৯৭৭; `AdminEscrowView.kt` {} ৩৫৪=৩৫৪, () ৯৬৩=৯৬৩)।
২. grep দিয়ে নিশ্চিত করা হয়েছে: `markRecentlyInserted()` এখন Users/KYC/Withdrawal-এর পরে
   Problems/Escrow-এর Insert+Update মিলিয়ে ৪টা নতুন কল-সাইট, `isNewFromRealtime` Problems-এ
   ঠিক একবার (একটা per-item লুপ), Escrow-এ ঠিক তিনবার (তিনটা per-item লুপ — Held/Released/
   Refunded প্রতিটায়)।
৩. ধাপ ০.১৪(ক)/(খ)-এর systemic প্যাটার্ন নতুন কোডে চেক করা হয়েছে — কোনো নতুন ঝুঁকি নেই (শুধু
   event-handler তুলনা + আগে-থেকে-বিদ্যমান TTL-হেল্পার কল, নতুন কোনো cancellation-প্রবণ
   `LaunchedEffect(data)` বসেনি)।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. এই স্ক্রিন দুটো role-shared না (admin-only), ধাপ ০.৫ প্রযোজ্য না।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা, dotfile সহ) মূল আপলোড করা zip-এর সাথে `zipfile` দিয়ে গুনে মিলিয়ে
   দেখা হয়েছে — শুধু এই চারটা `.kt` ফাইল আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) Problems ট্যাবে নতুন problem post হলে/status বদলালে সংশ্লিষ্ট কার্ড pulse করছে কিনা,
   (খ) Escrow ট্যাবে নতুন escrow তৈরি/HELD→RELEASED/REFUNDED transition-এ সংশ্লিষ্ট কার্ড সঠিক
   sub-tab-এ pulse করছে কিনা, (গ) দুটোতেই স্বাভাবিক স্ক্রলে ভুলভাবে pulse হচ্ছে না কিনা (রিগ্রেশন
   চেক), (ঘ) Users/KYC/Withdrawal-এর আগের real-device ভেরিফিকেশনও (এখনো pending ছিল) একই সাথে
   করা যেতে পারে।

### পরবর্তী সেশনের জন্য যা বাকি
**Ground Rule ২১-এর মূল ৫টা টার্গেট (Users, KYC, Withdrawal, Problems, Escrow) এখন সবগুলোই
implement করা হয়ে গেছে** (real-device verification এখনো সবগুলোর জন্যই বাকি, sandbox-এ সম্ভব না)।
মাস্টার প্রম্পটের নিয়ম অনুযায়ী পরের ধাপ: real-device-এ এই ৫টা কনফার্ম হওয়ার পর সেশন ২.২০-২.২৫
(বাকি ৬টা GR১৮ retrofit ট্যাব — চ্যাট মনিটরিং, সরাসরি চুক্তি, সলভার কোটা, ইনস্ট্যান্ট জবস, বিরোধ
কেন্দ্র, গেটওয়ে পেমেন্ট — GR18+19+20+21 একসাথে বান্ডল করে, Problems/Escrow-এর রেফারেন্স প্যাটার্ন
অনুসরণ করে)। এছাড়া Transactions (ইনডেক্স ৯) GR18 retrofit-এর অবস্থাও কোডে গিয়ে নতুন করে গ্রেপ
করে ভেরিফাই করা উচিত (মাস্টার প্রম্পটের পুরনো তালিকায় ছিল, এই সেশনে ছোঁয়া হয়নি)।

## ✅ সেশন ২.১৯.৭ — Ground Rule ২১ সংশোধন: Withdrawal/Problems/Escrow — Insert-only (Update বাদ)

ব্যবহারকারী সিদ্ধান্ত বদলে জানিয়েছেন: Withdrawal (সেশন ২.১৯.৫), Problems আর Escrow (সেশন ২.১৯.৬) —
এই তিনটাতেই আগে Insert+Update দুটোই pulse করাতো, এখন থেকে **শুধু genuine Insert**-এই pulse হবে
(Update/status-transition-এ আর না) — Users-এর মতোই Insert-only ডিজাইনে নামিয়ে আনা হয়েছে।

### যা এডিট হয়েছে (শুধু `SupabaseRealtimeManager.kt`)
তিনটা হ্যান্ডলারেই (`handleWithdrawalAction`, `handleProblemAction`, `handleEscrowAction`) Update
branch থেকে local-status-read + `markRecentlyInserted()` কল সরিয়ে ফেলা হয়েছে — merge/save লজিক
(mergeAndSaveProblem/mergeAndSaveEscrow/toWithdrawalEntity) অপরিবর্তিত রাখা হয়েছে, শুধু GR21-এর
মার্কিং-কল বাদ। Insert branch তিনটাতেই অপরিবর্তিত। তিনটা `_recentlyChangedXxxIds` StateFlow-এর
উপরের কমেন্ট আপডেট করা হয়েছে নতুন (Insert-only) ডিজাইন প্রতিফলিত করতে। **UI ফাইল
(`AdminWithdrawalsView.kt`/`AdminProblemsView.kt`/`AdminEscrowView.kt`) কোনোটাই স্পর্শ করা হয়নি**
— `isNewFromRealtime` চেকটা শুধু set membership দেখে, সেট কীভাবে populate হচ্ছে তার সাথে UI-এর
কোনো সম্পর্ক নেই।

### KYC অপরিবর্তিত
KYC (`_recentlyKycChangedUserIds`) এই সিদ্ধান্তের আওতায় না — সেটা প্রথম থেকেই Update-only
(status-transition-ভিত্তিক), ব্যবহারকারী এই সেশনে সেটা বদলাতে বলেননি।

### যাচাই
`SupabaseRealtimeManager.kt`-এ `{}` ৩৫০=৩৫০, `()` ১০৫০=১০৫০, `[]` ৭৫=৭৫ — balance মিলেছে।
`markRecentlyInserted(_recentlyChanged...)` grep করে নিশ্চিত করা হয়েছে এখন ঠিক ৩টা call-site
(Withdrawal/Problems/Escrow প্রতিটার Insert branch-এ একটা করে, আগে ৬টা ছিল — Update-branch-গুলো
বাদ)। zip-এর ফাইল-সংখ্যা (২৪৯টা) মিলিয়ে দেখা হয়েছে, শুধু এই একটা `.kt` ফাইল আর progress ফাইল বদলেছে।

**⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না** — আগের মতোই শুধু bracket-balance
আর grep দিয়ে verify করা হয়েছে।

### পরবর্তী সেশনের জন্য
Ground Rule ২১-এর ৫টা টার্গেট (Users, KYC, Withdrawal, Problems, Escrow)-এর চূড়ান্ত ডিজাইন এখন:
Users/Withdrawal/Problems/Escrow = Insert-only, KYC = Update-transition-only। Real-device
ভেরিফিকেশন এখনো বাকি সবগুলোর জন্যই। এরপর সেশন ২.২০-২.২৫ (বাকি ৬টা GR১৮ retrofit ট্যাব)-এ GR21
প্রয়োগ করার সময় প্রতিটা ট্যাবের জন্য Insert-only/Update-only/উভয়ই — এই প্রশ্নটা আবার আলাদাভাবে
জিজ্ঞাসা করতে হবে (Ground Rule ১৬), এই সংশোধিত Withdrawal/Problems/Escrow ডিজাইন ধরে নেওয়া যাবে
না নতুন ট্যাবের জন্য default হিসেবে।

## ✅ সেশন ২.২০ — Transactions (AdminTransactionsView.kt, ইনডেক্স ৯): Ground Rule ১৮+১৯+২০+২১ একসাথে retrofit — সম্পূর্ণ (Insert-only শুরু থেকেই)

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
প্রথমে গ্রেপ করে কনফার্ম করা হয়েছে (আগের সেশনের নোট অনুযায়ীই, এই ট্যাব তখনো ছোঁয়া হয়নি):
`AdminPanelScreen.kt`-এর ইনডেক্স ৯ তখনো পুরনো `SyncAwareRefreshableContent`-এ পুরো `data =
listOf(allTransactions, allGatewayPayments, allUsers)` পাস করছিল (GR18 retrofit বাকি), আর
`AdminTransactionsView.kt`-এ কোনো `PulsingValue`/`rememberFieldChangePulse` call-site ছিল না
(GR19 বাকি), যদিও GR20-এর `coerceIn(1,`/`scrollToItem(0)` আগে থেকেই ছিল (filter/pagination state
সহ)। ব্যবহারকারীকে তিনটা প্রশ্ন করা হয়েছিল, উত্তর:
- **GR19 (pulse scope):** শুধু তালিকার কার্ডগুলো — WorkTransaction (কাজের লেনদেন/রিফান্ড) আর
  WalletRecharge (গেটওয়ে ওয়ালেট রিচার্জ) দুটোই, সামারি/স্ট্যাট কার্ড/ফিল্টার বার বাদে।
- **GR18 (pull-to-refresh):** হ্যাঁ দরকার।
- **GR21 (realtime "নতুন"-এর সংজ্ঞা):** শুধু genuine Insert (নতুন transaction/payment তৈরি হলে) —
  Withdrawal/Problems/Escrow-এর সংশোধিত (সেশন ২.১৯.৭) Insert-only সিদ্ধান্তের সাথেই সঙ্গতিপূর্ণ,
  তাই এখানে Update-branch-এ কখনো মার্ক-করা কোড লেখাই হয়নি (KYC-এর মতো ভুল করে Insert+Update দিয়ে
  শুরু করে পরে সংশোধন করার দরকার পড়েনি)।

### টেকনিক্যাল জটিলতা: দুই আলাদা টেবিল, একই তালিকা
এই ট্যাবের তালিকায় দুই ধরনের আইটেম মিশ্রিত থাকে (`AdminTransactionListItem` sealed class:
`WorkTransaction` ও `WalletRecharge`), যেগুলো দুটো সম্পূর্ণ আলাদা টেবিল থেকে আসে (`transactions`
বনাম `gateway_payments`)। তাই Withdrawal/Problems/Escrow-এর এক-সেট-শেয়ার প্যাটার্নের বদলে দুটো
সম্পূর্ণ আলাদা `_recentlyChangedXxxIds` StateFlow দরকার হয়েছে, প্রতিটা নিজের টেবিলের id দিয়ে key
করা — নাহলে একটা `transactions.id` ভুলবশত `gateway_payments.id`-এর সাথে collide করার তাত্ত্বিক
ঝুঁকি থাকতো (practically UUID বলে বাস্তবে হওয়ার কথা না, কিন্তু আলাদা রাখাই বেশি স্পষ্ট/নিরাপদ)।

### যা এডিট হয়েছে
১. **`SupabaseRealtimeManager.kt`** — দুটো নতুন StateFlow: `_recentlyChangedTransactionIds`,
   `_recentlyChangedGatewayPaymentIds`। `handleTransactionAction()`-এর Insert branch-এ সরাসরি
   `markRecentlyInserted(_recentlyChangedTransactionIds, dto.id)`; Update branch অপরিবর্তিত (কোনো
   মার্কিং-কল নেই, শুরু থেকেই)। `handleGatewayPaymentAction()`-এও ঠিক একই প্যাটার্নে
   `_recentlyChangedGatewayPaymentIds`। Delete branch দুটোতেই অপরিবর্তিত।
২. **`SomadhanViewModel.kt`** — `recentlyChangedTransactionIds`/`recentlyChangedGatewayPaymentIds`
   StateFlow এক্সপোজ করা হয়েছে (Escrow-এর ঠিক নিচে, একই প্যাটার্নে)।
৩. **`AdminPanelScreen.kt`** — ইনডেক্স ৯-কে পুরনো `SyncAwareRefreshableContent` (`data =
   listOf(...)`) থেকে প্লেইন `SyncAwareContent`-এ বদলানো হয়েছে (sessionKey
   `"admin_transactions_sync"` অপরিবর্তিত, skeleton `ListScreenSkeleton(tint =
   SomadhanAdminSlate)` — Escrow/AdditionalCharges-এর প্যাটার্নেই), আর `AdminTransactionsView`-এ
   নতুন `isManualRefreshing = isRefreshing` পাস করা হয়েছে।
৪. **`AdminTransactionsView.kt`** — নতুন `isManualRefreshing: Boolean = false` প্যারামিটার;
   filter/pagination বদলে `isFilterRefreshing` (৩৫০ms, GR20-এর whole-visible-pulse); দুটো নতুন
   StateFlow `collectAsStateWithLifecycle()` (viewModel nullable বলে
   `remember(viewModel) { viewModel?.xxx ?: MutableStateFlow(emptySet()) }` fallback দিয়ে —
   conditional composable-call এড়াতে); WorkTransaction আর WalletRecharge — দুটো per-item
   `Card`-ই আলাদাভাবে `rememberFieldChangePulse(sessionKey = "admin_transactions_sync",
   flashOnReentry = false)` + `isFilterRefreshing` + নিজ নিজ `isNewFromRealtime` (trx.id/
   payment.id দিয়ে) — এই তিন স্বাধীন কারণ OR করে `PulsingValue`-তে wrap করা হয়েছে।

কোনো বিদ্যমান ফাংশনালিটি/action/pagination/filter/search/CSV-এক্সপোর্ট লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. চারটা এডিট করা ফাইলেই `{}`/`()`/`[]` ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে গোনা হয়েছে — সবগুলো মিলেছে
   (`AdminTransactionsView.kt` `{}` ৩১৩=৩১৩, `()` ৯৭৬=৯৭৬, `[]` ৩=৩; `AdminPanelScreen.kt` `{}`
   ২৩৫=২৩৫; `SupabaseRealtimeManager.kt` `{}` ৩৫০=৩৫০; `SomadhanViewModel.kt` `{}` ১৩০৮=১৩০৮)।
২. grep দিয়ে নিশ্চিত করা হয়েছে: `markRecentlyInserted(_recentlyChangedTransactionIds`/
   `..GatewayPaymentIds` প্রতিটা ঠিক একবার (Insert branch-এ), `PulsingValue(`/
   `rememberFieldChangePulse(` `AdminTransactionsView.kt`-এ ঠিক দুইবার করে (WorkTransaction +
   WalletRecharge, একবার করে), `isNewFromRealtime` দুই per-item লুপে আলাদাভাবে declare+ব্যবহার।
৩. `AdminPanelScreen.kt`-এ ইনডেক্স ৯-এর আর কোনো `SyncAwareRefreshableContent` কল-সাইট অবশিষ্ট নেই
   (grep দিয়ে ভেরিফাই করা)।
৪. zip-এর ফাইল-সংখ্যা (২৪৯টা) `zipfile` দিয়ে মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু এই
   চারটা `.kt` ফাইল আর এই progress ফাইলই বদলেছে।
৫. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না (network বন্ধ)** — আগের সব
   সেশনের মতোই শুধু bracket-balance আর grep দিয়ে verify করা হয়েছে।

### পরবর্তী সেশনের জন্য যা বাকি
১. Real-device-এ Transactions ট্যাব যাচাই: (ক) pull-to-refresh টানলে দৃশ্যমান কার্ডগুলো pulse
   করছে কিনা, (খ) ফিল্টার/সার্চ/সময়সীমা/টাইপ বদলালে বা পেজ পাল্টালে দৃশ্যমান কার্ডগুলো pulse করছে
   কিনা, (গ) নতুন কাজের লেনদেন/রিফান্ড বা নতুন ওয়ালেট রিচার্জ realtime-এ এলে শুধু সেই কার্ডটাই
   pulse করছে কিনা এবং বাকি কার্ড স্থির থাকছে কিনা, (ঘ) status-শুধু আপডেটে (যেমন গেটওয়ে পেমেন্ট
   PENDING→SUCCESS) pulse **না** হওয়াটাই প্রত্যাশিত (Insert-only ডিজাইন) — এটাও কনফার্ম করা উচিত।
২. মাস্টার প্রম্পটের বাকি সেশন ২.২১-২.২৫ (৫টা GR১৮ retrofit ট্যাব — চ্যাট মনিটরিং, সরাসরি চুক্তি,
   সলভার কোটা, ইনস্ট্যান্ট জবস, বিরোধ কেন্দ্র, গেটওয়ে পেমেন্ট) — প্রতিটাতে GR18+19+20+21 একসাথে
   বান্ডল করে, এই সেশন আর Problems/Escrow-এর রেফারেন্স প্যাটার্ন অনুসরণ করে। প্রতিটা ট্যাবের জন্য
   GR21 Insert-only/Update-only/উভয়ই — এই প্রশ্নটা আলাদাভাবে জিজ্ঞাসা করতে হবে (Ground Rule ১৬),
   ধরে নেওয়া যাবে না।
৩. Users/KYC/Withdrawal/Problems/Escrow/Transactions — ছয়টা ট্যাবেরই real-device ভেরিফিকেশন এখনো
   সব সেশন জুড়েই pending হিসেবে জমা হয়ে আছে, কোনো এক পর্যায়ে একসাথে করে ফেলা ভালো হতে পারে।

### সংযোজন — GR20 (filter/pagination pulse) স্পষ্টভাবে কনফার্ম করা হলো
উপরের কোড-এডিটে `isFilterRefreshing` (filter/search/time-range/page বদলে দৃশ্যমান কার্ড pulse,
Ground Rule ২০) যোগ করা হয়েছিল আগের ট্যাবগুলোর (Withdrawal/Problems/Escrow) established প্যাটার্ন
অনুসরণ করে, কিন্তু এই সেশনে ব্যবহারকারীকে সরাসরি জিজ্ঞাসা করা হয়নি (শুধু GR18/GR19/GR21 নিয়ে প্রশ্ন
হয়েছিল) — ব্যবহারকারী পরে খেয়াল করিয়ে দেন। জিজ্ঞাসা করার পর ব্যবহারকারী কনফার্ম করেছেন **রাখতে হবে**
(বাকি ট্যাবগুলোর সাথে সামঞ্জস্যপূর্ণ থাকার জন্য) — তাই কোনো কোড পরিবর্তন লাগেনি, বিদ্যমান
`isFilterRefreshing` অপরিবর্তিত থাকলো। ভবিষ্যতে GR20-এর pulse-scope নিয়ে প্রশ্ন উঠলে এই এন্ট্রিই
রেফারেন্স।

## ✅ সেশন ২.২১ — ChatMonitoring (AdminChatMonitoringView.kt, ইনডেক্স ১৫): শুধু GR18 retrofit — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
প্রথমে গ্রেপ করে কনফার্ম করা হয়েছে: `AdminPanelScreen.kt`-এর ইনডেক্স ১৫ পুরনো
`SyncAwareRefreshableContent`-এ পুরো `data = listOf(allProblems, allUsers)` পাস করছিল (GR18 বাকি),
`AdminChatMonitoringView.kt`-এ কোনো `PulsingValue`/`rememberFieldChangePulse` call-site ছিল না
(GR19 বাকি), pagination-এর `coerceIn(1,`/`scrollToItem(0)` আগে থেকেই ছিল। তিনটা প্রশ্নের উত্তর:
- **GR19 (pulse scope): কোনোটাই না** — বাম পাশের কথোপকথন/সমস্যা তালিকা বা ডান পাশের মেসেজ তালিকা,
  কোনোটাতেই pulse দরকার নেই।
- **GR18 (pull-to-refresh): হ্যাঁ দরকার** — কিন্তু pulse-ই নেই বলে whole-tab flash বন্ধ করাই যথেষ্ট।
- **GR21: এই ট্যাবে দরকার নেই।**

### যা এডিট হয়েছে (শুধু `AdminPanelScreen.kt`, ইনডেক্স ১৫ ব্লক)
ReputationEngine (ইনডেক্স ২০, সেশন ২.১৩)-এর ক্যাটেগরি E প্যাটার্ন হুবহু অনুসরণ করা হয়েছে:
`SyncAwareRefreshableContent(data = listOf(allProblems, allUsers), isManualRefreshing = isRefreshing)`
থেকে প্লেইন `SyncAwareContent(sessionKey = "admin_chat_monitoring_sync", ...)`-এ বদলানো হলো (sessionKey
অপরিবর্তিত, skeleton `ListScreenSkeleton(tint = SomadhanAdminSlate)` অপরিবর্তিত)। `AdminChatMonitoringView`
কলে `isManualRefreshing` প্যারামিটার যোগ করা হয়নি (pulse না থাকায় দরকার নেই, ReputationEngine-এর মতোই) —
তাই `AdminChatMonitoringView.kt` ফাইলটাই স্পর্শ করা হয়নি। কোনো বিদ্যমান ফাংশনালিটি/পেজিনেশন/মেসেজিং/
নেভিগেশন বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. `AdminPanelScreen.kt`-এ `{}` ২৩৫=২৩৫ (এডিটের আগে-পরে অপরিবর্তিত), `()` ৫১৫→৫১৮ (নতুন কমেন্টের
   কারণে, ব্যালেন্স মিলেছে)।
২. grep দিয়ে নিশ্চিত করা হয়েছে ইনডেক্স ১৫-এর জন্য আর কোনো `SyncAwareRefreshableContent` কল-সাইট
   অবশিষ্ট নেই; বাকি ৫টা (`SyncAwareRefreshableContent(` ঠিক ৫বার) — ইনডেক্স ১৬(DirectContracts),
   ১৯(SolverQuota), ২১(InstantJobs), ২২(DisputeCenter), ২৩(GatewayPayments) — এখনো retrofit বাকি,
   যা প্রত্যাশিত।
৩. `AdminChatMonitoringView.kt` এই সেশনে এডিট হয়নি (0 diff)।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. এই স্ক্রিন admin-only, ধাপ ০.৫ প্রযোজ্য না। ৪b. drawer group ম্যাপিং বদলায়নি (শুধু sessionKey-এর
   ভেতরের wrapper বদলেছে)।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা, dotfile সহ) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `AdminPanelScreen.kt` আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে: ChatMonitoring
   ট্যাবে pull-to-refresh টানলে আর পুরো ট্যাব (দুই পাশের তালিকা + হেডার) ফুল-স্কেলিটন হয়ে ফ্ল্যাশ করছে
   না (শুধু refresh স্পিনার ঘুরে শেষ হবে, কোনো কার্ড pulse হবে না — এটাই প্রত্যাশিত, কারণ pulse স্কোপ
   "কোনোটাই না")।

### পরবর্তী সেশনের জন্য যা বাকি
সেশন ২.২২-২.২৫ (৫টা GR১৮ retrofit ট্যাব বাকি — DirectContracts/১৬, SolverQuota/১৯, InstantJobs/২১,
DisputeCenter/২২, GatewayPayments/২৩) — প্রতিটার আগে Ground Rule ১৬ অনুযায়ী GR18/19/21 (candidate হলে
GR20-ও) নিয়ে আলাদাভাবে জিজ্ঞাসা করতে হবে, ChatMonitoring-এর "pulse নেই" সিদ্ধান্ত পরের ট্যাবের জন্য
default ধরা যাবে না। এছাড়া Users/KYC/Withdrawal/Problems/Escrow/Transactions/ChatMonitoring —
সাতটা ট্যাবেরই real-device ভেরিফিকেশন এখনো pending জমা হয়ে আছে।

## ✅ সেশন ২.২২ — DirectContracts (AdminDirectContractsView.kt, ইনডেক্স ১৬): GR18+19+20+21 একসাথে — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
গ্রেপ করে কনফার্ম করা হয়েছে: ইনডেক্স ১৬ পুরনো `SyncAwareRefreshableContent` (whole `data =
listOf(allProblems, allUsers)`) ব্যবহার করছিল (GR18 বাকি), ফাইলে কোনো `PulsingValue`/
`rememberFieldChangePulse` ছিল না (GR19 বাকি), pagination-এর `coerceIn(1,`/`scrollToItem(safePage)`
আগে থেকেই ছিল (GR20 candidate), আর ডিলিট অ্যাকশন (`viewModel.adminDeleteProblem`) কন্ট্রাক্ট তালিকা
থেকে আইটেম বাদ দেয় বলে scroll-jump ঝুঁকিও আছে। তিনটা প্রশ্নের উত্তর:
- **GR19: শুধু ডাইরেক্ট কন্ট্রাক্ট তালিকা।**
- **GR18: হ্যাঁ দরকার।**
- **GR21: শুধু genuine Insert।**

### যা এডিট হয়েছে
১. **`AdminPanelScreen.kt`** — ইনডেক্স ১৬ `SyncAwareRefreshableContent` → প্লেইন `SyncAwareContent`
   (sessionKey অপরিবর্তিত)। `AdminDirectContractsView` কলে কোনো নতুন প্যারামিটার লাগেনি — ফাইলটা
   আগে থেকেই নিজের ভেতরে `viewModel.isRefreshing.collectAsStateWithLifecycle()` কল করে
   (AdminUserLookupView-এর প্যাটার্নে), তাই সেটাই `rememberFieldChangePulse`-এর
   `isManualRefreshing`-এ পুনর্ব্যবহার করা হয়েছে।
২. **`AdminDirectContractsView.kt`** —
   - নতুন `isFilterRefreshing` (GR20, `LaunchedEffect(searchQuery, selectedStatusFilter,
     currentPage)`, ৩৫০ms, try/finally)।
   - scroll-jump ফিক্স: `LaunchedEffect(safePage)` → `LaunchedEffect(currentPage)`।
   - GR21: ডাইরেক্ট কন্ট্রাক্টও `problems` টেবিলেরই সারি বলে **নতুন কোনো ব্যাকএন্ড state লাগেনি** —
     বিদ্যমান `viewModel.recentlyChangedProblemIds` (Problems ট্যাবের সাথে শেয়ার্ড, Insert-only)
     পুনর্ব্যবহার করা হয়েছে।
   - per-item: `rememberFieldChangePulse(value = contract, isManualRefreshing = isRefreshing,
     sessionKey = "admin_direct_contracts_sync", flashOnReentry = false)` + `isFilterRefreshing` +
     `isNewFromRealtime` (recentlyChangedProblemIds.contains) — তিনটা OR করে `PulsingValue`-তে
     কন্ট্রাক্ট-কার্ড wrap করা হয়েছে।
   - নতুন import: `com.example.ui.components.PulsingValue`,
     `com.example.ui.components.rememberFieldChangePulse` (`delay` ফুলি-কোয়ালিফাইড কল করা হয়েছে,
     আলাদা import লাগেনি)।

কোনো বিদ্যমান ফাংশনালিটি/action/pagination/filter/search/মেসেজ-থ্রেড/ডিলিট-ডায়ালগ লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. `AdminDirectContractsView.kt`-এ `{}` ৩৩৯=৩৩৯, `()` ৭৯৯=৭৯৯ (পাইথন স্ক্রিপ্ট দিয়ে গোনা, আর
   char-by-char brace-matcher দিয়ে নিশ্চিত করা হয়েছে `items(...)` লুপ ঠিক `LazyColumn`-এর ভেতরেই
   বন্ধ হচ্ছে, নেস্টিং ভুল হয়নি)। `AdminPanelScreen.kt`-এ `{}` ২৩৫=২৩৫ (অপরিবর্তিত), `()`
   ৫১৮→৫১৯।
২. grep দিয়ে নিশ্চিত: ঠিক ১টা `PulsingValue(`/`rememberFieldChangePulse(` কল, ঠিক ১টা
   `recentlyChangedProblemIds` কালেকশন, কোনো `LaunchedEffect(safePage)` অবশিষ্ট নেই, `import
   com.example.ui.components.PulsingValue`/`rememberFieldChangePulse` যোগ হয়েছে (আগে ছিল না,
   `AdminProblemsView.kt`-এর মতো একই প্যাকেজ থেকে)।
৩. স্পর্শ করা ফাইল দুটোতেই (ChatMonitoring-এর মতো) systemic ১৪(ক)/(খ) প্যাটার্ন quick-check করা
   হয়েছে — নতুন কিছু পাওয়া যায়নি।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. admin-only, ধাপ ০.৫ প্রযোজ্য না। ৪b. drawer group ম্যাপিং বদলায়নি।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `AdminPanelScreen.kt`, `AdminDirectContractsView.kt`, আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) pull-to-refresh টানলে দৃশ্যমান কন্ট্রাক্ট-কার্ড pulse করছে কিনা, পুরো ট্যাব ফ্ল্যাশ করছে না,
   (খ) ফিল্টার/সার্চ/স্ট্যাটাস-ট্যাব/পেজ বদলালে দৃশ্যমান কার্ড pulse করছে কিনা, (গ) নতুন সরাসরি চুক্তি
   তৈরি হলে শুধু সেই কার্ডটাই pulse করছে কিনা, status-শুধু আপডেটে (accept/complete) pulse **না**
   হওয়াই প্রত্যাশিত (Insert-only), (ঘ) কোনো কন্ট্রাক্ট ডিলিট করে totalPages কমে গেলে scroll পজিশন
   ঠিক থাকছে কিনা (জোর করে টপে না গিয়ে)।

### পরবর্তী সেশনের জন্য যা বাকি
সেশন ২.২৩-২.২৫ (SolverQuota/১৯, InstantJobs/২১, DisputeCenter/২২, GatewayPayments/২৩) — প্রতিটার
আগে GR18/19/21(/20 candidate হলে) নিয়ে আলাদাভাবে জিজ্ঞাসা করতে হবে, DirectContracts-এর ডিজাইন
default ধরা যাবে না। Users/KYC/Withdrawal/Problems/Escrow/Transactions/ChatMonitoring/
DirectContracts — আটটা ট্যাবেরই real-device verification এখনো pending।

## 🐛 প্রি-সেশন বাগফিক্স — DirectContracts ট্যাবে endless reload/shimmer (সম্পূর্ণ, এই GR সিরিজের বাইরে)

ব্যবহারকারী রিপোর্ট করেছিলেন: DirectContracts (ইনডেক্স ১৬) ট্যাবে ঢুকলে হেডারের sync স্পিনার আর
কার্ড-শিমার একটানা চলতেই থাকে, অন্য ট্যাবে গিয়ে ফিরলেও ঠিক হয় না। রুট-কজ:
`AdminDirectContractsView.kt`-এ একটা `LaunchedEffect(Unit) { viewModel.triggerCloudSync() }` ছিল
— পুরো অ্যাপে **শুধু এই একটা** ট্যাবেই এই auto-trigger ছিল। `triggerCloudSync()` একটা ভারী,
পুরো-অ্যাপ-জোড়া অপারেশন (local→Supabase push, বাল্ক re-pull, **সব realtime চ্যানেল restart**,
admin metrics refresh) — টপ-বারের ম্যানুয়াল সিঙ্ক বাটনের জন্য বানানো, প্রতি-ভিজিট অটো-ট্রিগারের জন্য
না। প্রতিবার ট্যাবে ঢোকার সাথে সাথে পুরো ডেটা রি-লোড আর সব listener রিস্টার্ট হতো, তাই কার্ডগুলো
বারবার re-pulse করতো আর সিঙ্ক আইকন ততক্ষণ ঘুরতো যতক্ষণ না ভারী অপারেশনটা শেষ হতো — এটাই "endless
reload" মনে হচ্ছিল। **ফিক্স:** ওই `LaunchedEffect` ব্লকটা সরানো হয়েছে (cold-load ইতিমধ্যেই
`SyncAwareContent` হ্যান্ডেল করে বলে দরকারও ছিল না); টপ-বারের ম্যানুয়াল সিঙ্ক বাটন অক্ষত।

## ✅ সেশন ২.২৩ — SolverQuota (AdminSolverQuotaView.kt, ইনডেক্স ১৯): শুধু GR19 — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
কোড পড়ে দেখা গেছে এই ট্যাব বাকি ৮টার মতো list/feed স্ক্রিন না — সার্চ করে একজন সলভার বেছে নেওয়ার
টুল (কোনো pagination নেই, `LazyColumn`-এর `item{}`গুলো একজন সিলেক্টেড সলভারের ডিটেইল
কার্ড-সেকশন, রিপিটিং রেকর্ড-লিস্ট না)। তিনটা প্রশ্নের উত্তর:
- **GR18: দরকার নেই।**
- **GR19: হ্যাঁ, সিলেক্টেড সলভারের ডিটেইল কার্ড pulse করুক।**
- **GR21: বাদ দাও (কোনো লিস্ট-অফ-রেকর্ড নেই বলে প্রযোজ্য না)।**

### যা এডিট হয়েছে (শুধু `AdminSolverQuotaView.kt` — `AdminPanelScreen.kt` স্পর্শ করা হয়নি, GR18 বাদ)
১. নতুন import: `com.example.ui.components.PulsingValue`, `com.example.ui.components.rememberFieldChangePulse`।
২. `val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()` + একটা মাত্র
   `rememberFieldChangePulse(value = liveSolver, isManualRefreshing = isRefreshing)` কল — এটা
   `LazyColumn` শুরু হওয়ার **আগে**, ফাংশনের সাধারণ composable বডিতে (কারণ `LazyListScope.() ->
   Unit` নিজে composable scope না, ভেতরে সরাসরি `remember`-ভিত্তিক হেল্পার কল করা যায় না) —
   `liveSolver` (nullable `UserEntity?`) সরাসরি value হিসেবে পাস করা হয়েছে যাতে conditional
   composable-call লাগেনি।
৩. তিনটা `item{}`-এর ভেতরের Card (Profile Header, Free Quota, Extra Payment Miss Cycle) — প্রতিটা
   একই `solverDetailPulse` বুলিয়ানে `PulsingValue(isUpdating = solverDetailPulse) { Card(...) }`
   দিয়ে wrap করা হয়েছে, ফলে তিনটা কার্ড একসাথে, single detail-unit হিসেবে pulse করে (অন্য
   ডিভাইস থেকে রিসেট হলে বা এই অ্যাডমিন নিজে রিসেট করলে দুটোতেই — action pulse-ও কভার্ড)।

কোনো বিদ্যমান ফাংশনালিটি/সার্চ/সিলেকশন/রিসেট-ডায়ালগ লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. `AdminSolverQuotaView.kt`-এ `{}` ১৬১=১৬১, `()` ৪৯৯=৪৯৯ (পাইথন স্ক্রিপ্ট দিয়ে গোনা) —
   এডিটের আগে-পরে সমান।
২. grep দিয়ে নিশ্চিত: ঠিক ৩টা `PulsingValue(` কল, ঠিক ৩টা মিলিয়ে-থাকা close-comment, ঠিক ১টা
   `rememberFieldChangePulse(` কল, `import` দুটো যোগ হয়েছে (আগে ছিল না)।
৩. `AdminPanelScreen.kt` এই সেশনে এডিট হয়নি (GR18 বাদ দেওয়ার সিদ্ধান্ত অনুযায়ী, ০ diff)।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. admin-only, ধাপ ০.৫ প্রযোজ্য না। ৪b. drawer group ম্যাপিং বদলায়নি (কোনো wrapper-ই বদলায়নি)।
৫. zip-এর ফাইল-সংখ্যা মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `AdminDirectContractsView.kt` (প্রি-সেশন বাগফিক্স), `AdminSolverQuotaView.kt`, আর এই progress
   ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) কোনো সলভার সিলেক্ট করে রিসেট বাটনে চাপলে তিনটা কার্ডই সংক্ষিপ্ত সময় pulse করছে কিনা,
   (খ) সলভার সার্চ/সিলেক্ট করার সময় (কোনো রিসেট ছাড়া) ভুলভাবে pulse হচ্ছে না কিনা (রিগ্রেশন চেক),
   (গ) pull-to-refresh টানলে (গ্লোবাল wrapper থেকে) ডিটেইল কার্ড pulse করছে কিনা।

### পরবর্তী সেশনের জন্য যা বাকি
সেশন ২.২৪-২.২৫ (InstantJobs/২১, DisputeCenter/২২, GatewayPayments/২৩) — প্রতিটার আগে GR18/19/21
(/20 candidate হলে) নিয়ে আলাদাভাবে জিজ্ঞাসা করতে হবে, SolverQuota-র ডিজাইন default ধরা যাবে না।

## 🐛 প্রি-সেশন বাগফিক্স ২ — পুরো admin panel-এ pull-to-refresh আসলে রিফ্রেশ করে না (সম্পূর্ণ, GR সিরিজের বাইরে)

ব্যবহারকারী রিপোর্ট করেছিলেন: যেকোনো admin ট্যাবে pull-to-refresh টানলে স্পিনার নিচে নামে কিন্তু
আসলে কিছু রিফ্রেশ হয় না, ছেড়ে দিলে স্পিনারটা উপরে উঠে হারিয়ে যায়। রুট-কজ: `AdminPanelScreen.kt`-এর
একক শেয়ার্ড `onAdminPullToRefresh` হ্যান্ডলার শুধু `initialSyncPhase == ERROR` অবস্থায়
`retryInitialSync()` কল করতো — স্বাভাবিক অবস্থায় (যা প্রায় সবসময়, প্রথমবার লোড হয়ে গেলে) হ্যান্ডলারটা
**কিছুই করতো না**, তাই `isRefreshing` (যা `SomadhanPullToRefresh`/`PullToRefreshBox`-এর ইনডিকেটর
ড্রাইভ করে, এবং প্রতিটা ট্যাবের `isManualRefreshing` pulse-ও) কখনো `true` হতো না। **ফিক্স:** এখন
স্বাভাবিক অবস্থায়ও `viewModel.triggerCloudSync()` (টপ-বারের Force Sync বাটনের মতোই) কল হয়, ERROR
অবস্থায় `retryInitialSync()`-ও পাশাপাশি কল হয়। যেহেতু এই হ্যান্ডলারটা পুরো `when (selectedTabIndex)`
ব্লককে একটা একক wrapper দিয়ে মুড়ে রাখে, **একটা জায়গায় ফিক্স করাতেই পুরো admin panel-এর সব
ট্যাবে pull-to-refresh এখন কাজ করবে** — কোনো আলাদা ট্যাব-ফাইল স্পর্শ করতে হয়নি।

**পার্শ্ব-প্রতিক্রিয়া (প্রত্যাশিত, বাগ না):** যেহেতু `triggerCloudSync()`-এর শেষে একটা টোস্ট মেসেজ
("সিঙ্ক সম্পন্ন: ...") দেখায়, এখন প্রতিবার pull-to-refresh টানলেই এই টোস্টটাও দেখাবে — এটা টপ-বারের
Force Sync বাটনের বিদ্যমান আচরণেরই সম্প্রসারণ, নতুন কোনো আলাদা আচরণ না।

`AdminPanelScreen.kt`-এ `{}` ২৩৫=২৩৫ (অপরিবর্তিত), `()` ৫২৮=৫২৮ — যাচাই করা হয়েছে। কোনো অন্য ফাইল
স্পর্শ করা হয়নি।

## ✅ সেশন ২.২৪ — InstantJobs (AdminInstantJobsView.kt, ইনডেক্স ২১): GR18+19+20 (GR21 বাদ) — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
গ্রেপ করে কনফার্ম করা হয়েছে: ইনডেক্স ২১ পুরনো `SyncAwareRefreshableContent` (whole `data =
listOf(allProblems, allUsers, allCategories, allHeldEscrows, allReleasedEscrows,
allRefundedEscrows)`) ব্যবহার করছিল (GR18 বাকি), ফাইলে কোনো `PulsingValue`/`rememberFieldChangePulse`
ছিল না (GR19 বাকি), pagination-এর `coerceIn(1,`/`scrollToItem(safePage...)` আগে থেকেই ছিল (GR20
candidate), আর তালিকার উপরে Broadcasting/En Route/Started/Completed/Cancelled কাউন্ট-ট্যাব আর একটা
গ্লোবাল টগল/রেডিয়াস-কনফিগ সেকশনও আছে। তিনটা প্রশ্নের উত্তর:
- **GR19: শুধু পেজড জব-তালিকার কার্ড** (কাউন্ট-ট্যাব আর কনফিগ ফর্ম বাদ)।
- **GR18: হ্যাঁ, করো।**
- **GR21: বাদ দাও** (`recentlyChangedProblemIds` পুনর্ব্যবহার করা যেত, কিন্তু ব্যবহারকারী এই
  ট্যাবে ইচ্ছাকৃতভাবে স্কিপ করতে বলেছেন)।

### যা এডিট হয়েছে
১. **`AdminPanelScreen.kt`** — ইনডেক্স ২১ `SyncAwareRefreshableContent` → প্লেইন `SyncAwareContent`
   (sessionKey `"admin_instant_jobs_sync"` অপরিবর্তিত, `data`/`isManualRefreshing` প্যারামিটার
   সরানো হয়েছে যেহেতু plain `SyncAwareContent`-এ ওগুলো নেই)।
২. **`AdminInstantJobsView.kt`** —
   - নতুন import: `com.example.ui.components.PulsingValue`,
     `com.example.ui.components.rememberFieldChangePulse`।
   - নতুন `val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()` (ফাংশনের
     শুরুতে)।
   - GR20: নতুন `isFilterRefreshing` (`LaunchedEffect(selectedStatusFilter, selectedCategoryFilter,
     searchQuery, currentPage)`, ৩৫০ms, try/finally) — `currentPage` ইচ্ছাকৃতভাবে key-তে আছে
     (আপডেটেড স্ট্যান্ডার্ড অনুযায়ী)।
   - scroll-jump ফিক্স: `LaunchedEffect(safePage, ...)` → `LaunchedEffect(currentPage, ...)`।
   - per-item: `items(pagedList)`-এর ভেতরে `rememberFieldChangePulse(value = problem,
     isManualRefreshing = isRefreshing, sessionKey = "admin_instant_jobs_sync", viewModel =
     viewModel, flashOnReentry = false)` + `isFilterRefreshing` — দুটো OR করে `PulsingValue`-তে
     শুধু জব-কার্ড (`Card(...)`) wrap করা হয়েছে; উপরের কাউন্ট-ট্যাব আর কনফিগ সেকশন স্পর্শ করা
     হয়নি (ব্যবহারকারীর কনফার্মড স্কোপ অনুযায়ী)। GR21 কোনো কোড যোগ করা হয়নি (ইচ্ছাকৃত স্কিপ)।

কোনো বিদ্যমান ফাংশনালিটি/action/pagination/filter/search/কনফিগ-সেভ/কাউন্ট-ট্যাব লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. `AdminInstantJobsView.kt`-এ `{}` ২৫৯=২৫৯, `()` ৮৪২=৮৪২ (এডিটের পরে সমান, পাইথন স্ক্রিপ্ট দিয়ে
   গোনা)। `AdminPanelScreen.kt`-এ `{}` ২৩৫=২৩৫ (অপরিবর্তিত), `()` ৫২৮=৫২৮ (অপরিবর্তিত — `data`/
   `isManualRefreshing` সরানোর ফলে যতগুলো `(`/`)` কমেছে দুই ফাইলেই মিলে গেছে)।
২. grep দিয়ে নিশ্চিত: ঠিক ১টা `PulsingValue(`, ঠিক ১টা `rememberFieldChangePulse(`,
   `recentlyChangedProblemIds` কোনো রেফারেন্স নেই (GR21 বাদ যাওয়ার প্রত্যাশিত ফলাফল),
   `SyncAwareRefreshableContent(` (কমেন্ট বাদে, লাইন ১১১২-এর পুরনো মন্তব্য অপরিবর্তিত) এখন শুধু
   ইনডেক্স ১৯(SolverQuota, ইচ্ছাকৃত), ২২(DisputeCenter), ২৩(GatewayPayments) — ৩টা বাকি।
৩. স্পর্শ করা ফাইল দুটোতেই systemic ১৪(ক)/(খ) প্যাটার্ন quick-check করা হয়েছে — নতুন কিছু পাওয়া
   যায়নি।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. admin-only, ধাপ ০.৫ প্রযোজ্য না। ৪b. drawer group ম্যাপিং বদলায়নি (কোনো wrapper-ই বদলায়নি)।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `AdminPanelScreen.kt`, `AdminInstantJobsView.kt`, আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) pull-to-refresh টানলে দৃশ্যমান জব-কার্ড pulse করছে কিনা, পুরো ট্যাব (কাউন্ট-ট্যাব/কনফিগ
   সেকশন সহ) ফ্ল্যাশ করছে না, (খ) স্ট্যাটাস-ফিল্টার/ক্যাটাগরি-ফিল্টার/সার্চ/পেজ বদলালে দৃশ্যমান
   জব-কার্ড pulse করছে কিনা, (গ) কোনো জব cancel/rebroadcast/to-normal-bidding করার পর সেই
   নির্দিষ্ট কার্ডই pulse করছে কিনা (bug-1 ক্লাস স্টাক-শিমার হচ্ছে না তাও যাচাই করা), (ঘ) কোনো
   action-এ তালিকা থেকে আইটেম বাদ পড়ে totalPages কমে গেলে scroll পজিশন ঠিক থাকছে কিনা (জোর করে
   টপে না গিয়ে), (ঙ) নতুন realtime insert-এ pulse **না** হওয়াই প্রত্যাশিত (GR21 ইচ্ছাকৃতভাবে বাদ)।

### পরবর্তী সেশনের জন্য যা বাকি
সেশন ২.২৫-২.২৬ (DisputeCenter/২২, GatewayPayments/২৩) — প্রতিটার আগে GR18/19/21(/20 candidate
হলে) নিয়ে আলাদাভাবে জিজ্ঞাসা করতে হবে, InstantJobs-এর ডিজাইন (বিশেষত GR21 স্কিপ) default ধরা যাবে
না। Users/KYC/Withdrawal/Problems/Escrow/Transactions/ChatMonitoring/DirectContracts/InstantJobs —
৯টা ট্যাবেরই real-device verification এখনো pending।

## ✅ সেশন ২.২৫ — DisputeCenter (AdminDisputeCenterView.kt, ইনডেক্স ২২): GR18+19+GR20-ভাবনা (GR21 বাদ) — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
গ্রেপ করে কনফার্ম করা হয়েছে: ইনডেক্স ২২ পুরনো `SyncAwareRefreshableContent` (whole `data =
listOf(allProblems, allUsers)`) ব্যবহার করছিল (GR18 বাকি), ফাইলে কোনো `PulsingValue`/
`rememberFieldChangePulse` ছিল না (GR19 বাকি)। এই ট্যাবে **কোনো পেজিনেশন নেই** (`coerceIn(1,`/
`scrollToItem` গ্রেপে পাওয়া যায়নি) — বরং সার্চ, টাইপ-ফিল্টার (Normal/Instant/Direct), আর তিনটা
সাব-ট্যাব (Active/Admin-Involved/Resolved) আছে। চারটা প্রশ্নের উত্তর:
- **GR19: শুধু তালিকার ডিসপিউট-কার্ড** (সাব-ট্যাব কাউন্ট-ব্যাজ বাদ)।
- **GR18: হ্যাঁ, করো।**
- **GR20-ভাবনা (পেজিনেশন না থাকলেও সার্চ/ফিল্টার/সাব-ট্যাব বদলে whole-visible pulse): হ্যাঁ,
  `isFilterRefreshing` যোগ করো।**
- **GR21: বাদ দাও** (`recentlyChangedProblemIds` পুনর্ব্যবহার করা যেত, কিন্তু ব্যবহারকারী স্কিপ
  করতে বলেছেন)।

### যা এডিট হয়েছে
১. **`AdminPanelScreen.kt`** — ইনডেক্স ২২ `SyncAwareRefreshableContent` → প্লেইন `SyncAwareContent`
   (sessionKey `"admin_dispute_center_sync"` অপরিবর্তিত, `data`/`isManualRefreshing` প্যারামিটার
   সরানো হয়েছে)।
২. **`AdminDisputeCenterView.kt`** —
   - নতুন import: `com.example.ui.components.PulsingValue`,
     `com.example.ui.components.rememberFieldChangePulse`।
   - নতুন `val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()`।
   - GR20-ভাবনা: নতুন `isFilterRefreshing` (`LaunchedEffect(searchQuery, selectedTypeFilter,
     selectedTab)`, ৩৫০ms, try/finally) — এই ট্যাবে pagination-key নেই বলে শুধু এই তিনটাই key।
   - per-item: `items(filteredProblems)`-এর ভেতরে `rememberFieldChangePulse(value = problem,
     isManualRefreshing = isRefreshing, sessionKey = "admin_dispute_center_sync", viewModel =
     viewModel, flashOnReentry = false)` + `isFilterRefreshing` — দুটো OR করে `PulsingValue`-তে
     শুধু ডিসপিউট-কার্ড (`Card(...)`) wrap করা হয়েছে; সাব-ট্যাব কাউন্ট-ব্যাজ/সার্চ-বার/ফিল্টার-চিপ
     স্পর্শ করা হয়নি। GR21 কোনো কোড যোগ করা হয়নি (ইচ্ছাকৃত স্কিপ)।

কোনো বিদ্যমান ফাংশনালিটি/action/সার্চ/ফিল্টার/সাব-ট্যাব/সেটেলমেন্ট-ডায়ালগ/চ্যাট-থ্রেড লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. `AdminDisputeCenterView.kt`-এ `{}` ৪৪৮=৪৪৮, `()` ১২৯০=১২৯০ (এডিটের পরে সমান)। `AdminPanelScreen.kt`-এ
   `{}` ২৩৫=২৩৫ (অপরিবর্তিত), `()` ৫২৮=৫২৮ (অপরিবর্তিত)।
২. grep দিয়ে নিশ্চিত: ঠিক ১টা `PulsingValue(`, ঠিক ১টা `rememberFieldChangePulse(`,
   `recentlyChangedProblemIds` কোনো রেফারেন্স নেই (GR21 বাদ যাওয়ার প্রত্যাশিত ফলাফল)।
   `SyncAwareRefreshableContent(` (কমেন্ট বাদে) এখন শুধু ইনডেক্স ১৯(SolverQuota, ইচ্ছাকৃত),
   ২৩(GatewayPayments) — ২টা বাকি।
৩. স্পর্শ করা ফাইল দুটোতেই systemic ১৪(ক)/(খ) প্যাটার্ন quick-check করা হয়েছে — নতুন কিছু পাওয়া
   যায়নি।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. admin-only, ধাপ ০.৫ প্রযোজ্য না। ৪b. drawer group ম্যাপিং বদলায়নি।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `AdminPanelScreen.kt`, `AdminDisputeCenterView.kt`, আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) pull-to-refresh টানলে দৃশ্যমান ডিসপিউট-কার্ড pulse করছে কিনা, পুরো ট্যাব ফ্ল্যাশ করছে না,
   (খ) সার্চ/টাইপ-ফিল্টার/সাব-ট্যাব বদলালে দৃশ্যমান কার্ড pulse করছে কিনা, (গ) কোনো ডিসপিউট
   resolve/settle/strike করার পর সেই নির্দিষ্ট কার্ডই pulse করছে কিনা (bug-1 ক্লাস স্টাক-শিমার
   হচ্ছে না তাও যাচাই), (ঘ) নতুন realtime insert-এ pulse **না** হওয়াই প্রত্যাশিত (GR21 ইচ্ছাকৃতভাবে
   বাদ)।

### পরবর্তী সেশনের জন্য যা বাকি
সেশন ২.২৬ — GatewayPayments (ইনডেক্স ২৩), GR18/19/21(/20 candidate হলে) নিয়ে আলাদাভাবে জিজ্ঞাসা
করতে হবে। এরপর ReputationDetailScreen (tab-index সিস্টেমের বাইরে) — GR18 retrofit দরকার কিনা এখনো
জিজ্ঞাসা করা হয়নি। এই দুটো শেষ হলে admin panel-এর GR18-২১ রোডম্যাপ কোড-লেভেলে সম্পূর্ণ হবে —
বাকি থাকবে শুধু real-device verification (এখন পর্যন্ত ১০টা ট্যাব pending: Users/KYC/Withdrawal/
Problems/Escrow/Transactions/ChatMonitoring/DirectContracts/InstantJobs/DisputeCenter)।

## ✅ সেশন ২.২৬ — GatewayPayments (AdminGatewayPaymentsView.kt, ইনডেক্স ২৩): GR18+19+20+21 একসাথে retrofit — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
গ্রেপ করে কনফার্ম করা হয়েছে: ইনডেক্স ২৩ পুরনো `SyncAwareRefreshableContent` (`data =
listOf(allGatewayPayments, allUsers)`) ব্যবহার করছিল (GR18 বাকি), ফাইলে কোনো `PulsingValue`/
`rememberFieldChangePulse` ছিল না (GR19 বাকি), `coerceIn(1,`/`scrollToItem` আগে থেকেই ছিল (সার্চ +
তিনটা ফিল্টার — গেটওয়ে/স্ট্যাটাস/পারপজ — সহ পেজিনেশন, GR20 candidate)। চারটা প্রশ্নের উত্তর:
- **GR19 (pulse scope):** তালিকার পেমেন্ট-কার্ড **এবং** উপরের সামারি কার্ডের ভেতরের ভ্যালু-টেক্সট
  গুলো (মোট ট্রানজেকশন/বিকাশ/নগদ/রকেট/ব্যাংক-এর টাকার অংক) — পুরো সামারি কার্ড না।
- **GR18:** হ্যাঁ, করো।
- **GR20:** হ্যাঁ, `isFilterRefreshing` যোগ করো (`currentPage` সহ)।
- **GR21:** সেশন ২.২০-এ Transactions-এর জন্য বানানো `recentlyChangedGatewayPaymentIds`
  (Insert-only, `SupabaseRealtimeManager`/`SomadhanViewModel`-এ আগে থেকেই এক্সপোজড) পুনর্ব্যবহার
  করো।

### যা এডিট হয়েছে
১. **`AdminPanelScreen.kt`** — ইনডেক্স ২৩ `SyncAwareRefreshableContent` → প্লেইন `SyncAwareContent`
   (sessionKey `"admin_gateway_payments_sync"` অপরিবর্তিত, `data` প্যারামিটার সরানো হয়েছে,
   `AdminGatewayPaymentsView`-এ নতুন `isManualRefreshing = isRefreshing` পাস করা হয়েছে)।
2. **`AdminGatewayPaymentsView.kt`** —
   - নতুন import: `androidx.lifecycle.compose.collectAsStateWithLifecycle`,
     `com.example.ui.components.PulsingValue`, `com.example.ui.components.rememberFieldChangePulse`।
   - নতুন `isManualRefreshing: Boolean = false` প্যারামিটার।
   - GR20: নতুন `isFilterRefreshing` (`LaunchedEffect(selectedGatewayFilter,
     selectedStatusFilter, selectedPurposeFilter, searchQuery, currentPage)`, ৩৫০ms, try/finally)।
   - GR21: `recentlyChangedGatewayPaymentIds` StateFlow `collectAsStateWithLifecycle()` (viewModel
     nullable বলে `remember(viewModel) { viewModel?.xxx ?: MutableStateFlow(emptySet()) }` fallback,
     Transactions-এর একই প্যাটার্ন)।
   - পেমেন্ট-তালিকার প্রতিটা কার্ড: `items(paginatedList)`-এর ভেতরে
     `rememberFieldChangePulse(value = payment, sessionKey = "admin_gateway_payments_sync",
     viewModel = viewModel, flashOnReentry = false)` + `isFilterRefreshing` +
     `isNewFromRealtime` (`recentlyChangedGatewayPaymentIds.contains(payment.id)`) — তিনটা OR করে
     `PulsingValue`-তে শুধু `Card` wrap করা হয়েছে।
   - `GatewayStatCard`-এ নতুন `isManualRefreshing: Boolean = false` প্যারামিটার (৫টা কলসাইটেই পাস
     করা হয়েছে); ভেতরে `rememberFieldChangePulse(value = amount, isManualRefreshing =
     isManualRefreshing)` (sessionKey/viewModel ইচ্ছাকৃতভাবে দেওয়া হয়নি, AdminStatsView-এর
     heroCardPulsing প্যাটার্নের মতোই — শুধু value-বদল/manual-refresh-সমাপ্তি, re-entry-flash না)
     — শুধু ভ্যালু-টেক্সট (`Formatters.formatTaka(amount)`) `PulsingValue`-তে wrap করা হয়েছে,
     টাইটেল/আইকন/কাউন্ট-টেক্সট স্পর্শ করা হয়নি।

কোনো বিদ্যমান ফাংশনালিটি/action/স্ট্যাটাস-আপডেট-মেনু/CSV-এক্সপোর্ট/রসিদ-ডায়ালগ/সার্চ/ফিল্টার/
পেজিনেশন লজিক বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. দুটো এডিট করা ফাইলেই `{}`/`()`/`[]` ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে গোনা হয়েছে — সবগুলো মিলেছে
   (`AdminGatewayPaymentsView.kt` `{}` ২১৩=২১৩, `()` ৫৩৩=৫৩৩, `[]` ৩=৩; `AdminPanelScreen.kt`
   `{}` ২৩৫=২৩৫, `()` ৫২৮=৫২৮, `[]` ৩=৩ — অপরিবর্তিত)।
2. grep দিয়ে নিশ্চিত: ঠিক ১টা per-item `PulsingValue(`/`rememberFieldChangePulse(` (তালিকার
   কার্ডে) + ১টা `GatewayStatCard`-এর ভেতরে (৫টা কলসাইট থেকে reuse), `isFilterRefreshing`,
   `recentlyChangedGatewayPaymentIds` সব ঠিক জায়গায়।
৩. `AdminPanelScreen.kt`-এ ইনডেক্স ২৩-এর আর কোনো `SyncAwareRefreshableContent` কল-সাইট অবশিষ্ট
   নেই — এখন শুধু ইনডেক্স ১৯(SolverQuota, ইচ্ছাকৃত) বাকি।
৪. স্পর্শ করা ফাইল দুটোতেই systemic ১৪(ক)/(খ) প্যাটার্ন quick-check করা হয়েছে — নতুন কিছু পাওয়া
   যায়নি।
৫. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৫a. admin-only, ধাপ ০.৫ প্রযোজ্য না। ৫b. drawer group ম্যাপিং বদলায়নি।
৬. zip-এর ফাইল-সংখ্যা (২৯৫টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `AdminPanelScreen.kt`, `AdminGatewayPaymentsView.kt`, আর এই progress ফাইলই বদলেছে।
৭. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) pull-to-refresh টানলে দৃশ্যমান পেমেন্ট-কার্ড pulse করছে কিনা, পুরো ট্যাব ফ্ল্যাশ করছে না,
   (খ) সার্চ/গেটওয়ে-ফিল্টার/স্ট্যাটাস-ফিল্টার/পারপজ-ফিল্টার/পেজ বদলালে দৃশ্যমান কার্ড pulse করছে
   কিনা, (গ) কোনো পেমেন্টের স্ট্যাটাস আপডেট করার পর সেই নির্দিষ্ট কার্ডই pulse করছে কিনা (bug-1
   ক্লাস স্টাক-শিমার হচ্ছে না তাও যাচাই), (ঘ) নতুন realtime insert (নতুন গেটওয়ে পেমেন্ট) এলে শুধু
   সেই কার্ডটাই pulse করছে কিনা, (ঙ) উপরের সামারি কার্ডগুলোর টাকার অংক realtime-এ বদলালে (নতুন
   পেমেন্ট আসা বা স্ট্যাটাস আপডেটে টোটাল রিক্যালকুলেট হওয়া) শুধু সেই ভ্যালু-টেক্সটটাই pulse করছে
   কিনা, পুরো স্ট্যাট-কার্ড বা কাউন্ট-টেক্সট না।

### পরবর্তী সেশনের জন্য যা বাকি
সেশন ২.২৭ — `ReputationDetailScreen` (tab-index সিস্টেমের বাইরে) — GR18 retrofit দরকার কিনা এখনো
জিজ্ঞাসা করা হয়নি। এটা শেষ হলে admin panel-এর GR18-২১ রোডম্যাপ কোড-লেভেলে সম্পূর্ণ হবে — বাকি
থাকবে শুধু real-device verification (এখন পর্যন্ত ১১টা ট্যাব pending: Users/KYC/Withdrawal/Problems/
Escrow/Transactions/ChatMonitoring/DirectContracts/InstantJobs/DisputeCenter/GatewayPayments)।

## ✅ সেশন ২.২৭ — ReputationDetailScreen.kt (tab-index সিস্টেমের বাইরে, AdminUsersView-এর onNavigateToReputation থেকে navigate): GR18+19 (GR20 প্রযোজ্য না, GR21 বাদ) — সম্পূর্ণ

### শুরুর আগে ব্যবহারকারীকে জিজ্ঞাসা করা হয়েছে (Ground Rule ১৬)
গ্রেপ করে কনফার্ম করা হয়েছে: এই স্ক্রিন এখনো `SyncAwareRefreshableContent`-এ পুরো `data =
listOf(effectiveUser, ratings, recentEvents, allProblems, allTransactions)` পাস করছিল (GR18 বাকি)
— মানে স্কোর কার্ড/স্ট্যাট কার্ড/হিস্ট্রি সব একসাথে flash করতো। ফাইলে কোনো `PulsingValue`/
`rememberFieldChangePulse` ছিল না (GR19 বাকি)। হিস্ট্রি তালিকা (`recentEvents.take(10)`) একটা
প্লেইন `Column`-এর ভেতরে `forEach`-এ রেন্ডার হয় (LazyColumn/pagination না, তাই GR20 প্রযোজ্য না)।
তিনটা প্রশ্নের উত্তর:
- **GR18:** হ্যাঁ, করো।
- **GR19 (pulse scope):** শুধু হিস্ট্রি ইভেন্ট কার্ডগুলো (`recentEvents.take(10)`) — স্কোর/স্ট্যাট
  কার্ড/হেডার বাদে।
- **GR21:** বাদ দাও (এখন না) — DisputeCenter-এর মতোই নতুন `recentlyChangedReputationEventIds`-জাতীয়
  tracking বানানো হয়নি।

### যা এডিট হয়েছে
`ReputationDetailScreen.kt`-এ —
১. নতুন import: `com.example.ui.components.PulsingValue`, `com.example.ui.components.SyncAwareContent`
   (পুরনো `SyncAwareRefreshableContent` import সরানো হয়েছে), `com.example.ui.components.rememberFieldChangePulse`।
2. `SyncAwareRefreshableContent(data = listOf(...), isManualRefreshing = isRefreshing, ...)` →
   প্লেইন `SyncAwareContent(...)` (sessionKey `"reputation_detail_$userId"` অপরিবর্তিত, `data`/
   `isManualRefreshing` প্যারামিটার সরানো হয়েছে, content lambda-র ignored প্যারামিটার `{ _ -> }`
   → `{ }`)। পুরনো ব্যাখ্যামূলক কমেন্টও (data-diff শিমার সংক্রান্ত) আপডেট করা হয়েছে যাতে দাগ না
   থাকে যে এখনো পুরনো মেকানিজম আছে। `SomadhanPullToRefresh` (rule ৩, pull-to-refresh) অপরিবর্তিত
   আছে — শুধু "পুরো কনটেন্ট flash" আচরণটাই সরানো হয়েছে।
৩. হিস্ট্রি ইভেন্ট `forEach` লুপের ভেতরে প্রতিটা `Card`-এ: নতুন `rememberFieldChangePulse(value =
   event, isManualRefreshing = isRefreshing, sessionKey = "reputation_detail_$userId", viewModel
   = viewModel, flashOnReentry = false)` + `PulsingValue(isUpdating = eventCardPulse) { Card(...) }`
   wrap। GR21 কোনো কোড যোগ করা হয়নি (ইচ্ছাকৃত স্কিপ)।

কোনো বিদ্যমান ফাংশনালিটি/pull-to-refresh/navigation/স্কোর-অ্যানিমেশন/এক্সট্রা-বিল-ক্যাপ লজিক
বদলায়নি।

### যাচাই (ধাপ ৩ চেকলিস্ট)
১. `ReputationDetailScreen.kt`-এ `{}` ১৭২=১৭২, `()` ৫৯২=৫৯২ (এডিটের পরে সমান)।
2. grep দিয়ে নিশ্চিত: ঠিক ১টা `PulsingValue(`, ঠিক ১টা `rememberFieldChangePulse(`, আর কোনো
   real call-site-এ `SyncAwareRefreshableContent` নেই (শুধু ২টা পুরনো ব্যাখ্যামূলক কমেন্ট আপডেট
   করে `SyncAwareContent` করা হয়েছে, বাকি একটা কমেন্ট ইতিহাস বোঝাতে রাখা হয়েছে)।
৩. স্পর্শ করা ফাইলে systemic ১৪(ক)/(খ) প্যাটার্ন quick-check করা হয়েছে — নতুন কিছু পাওয়া যায়নি।
৪. কোনো বিদ্যমান ফাংশনালিটি/বিজনেস-লজিক/নেভিগেশন/পারমিশন আচরণ বদলানো হয়নি।
৪a. admin-only navigate-target, ধাপ ০.৫ প্রযোজ্য না। ৪b. drawer group ম্যাপিং প্রযোজ্য না (tab-index
   সিস্টেমের বাইরে)।
৫. zip-এর ফাইল-সংখ্যা (২৪৯টা) মূল আপলোড করা zip-এর সাথে মিলিয়ে দেখা হয়েছে — শুধু
   `ReputationDetailScreen.kt` আর এই progress ফাইলই বদলেছে।
৬. **⚠️ real Android Studio/Gradle build এই sandbox-এ সম্ভব না।** ব্যবহারকারীকে দেখতে হবে:
   (ক) pull-to-refresh টানলে শুধু হিস্ট্রি ইভেন্ট কার্ড pulse করছে কিনা, স্কোর/স্ট্যাট কার্ড বা
   পুরো পেজ ফ্ল্যাশ করছে না, (খ) কোনো নতুন reputation event তৈরি হলে (job complete, KYC verify,
   ইত্যাদি) সেই ইভেন্ট প্রথমবার তালিকায় দেখা দেওয়ার সময় pulse হচ্ছে কিনা এবং বাকি ইভেন্ট-কার্ড
   স্থির থাকছে কিনা (bug-1 ক্লাস স্টাক-শিমার হচ্ছে না তাও যাচাই), (গ) স্ক্রিনে re-entry করলে
   (`flashOnReentry = false`-এর প্রত্যাশিত ফলাফল) কোনো ইভেন্ট-কার্ড ভুলভাবে pulse **না** করা।

## 🎉 GR18-২১ রোডম্যাপ কোড-লেভেলে সম্পূর্ণ
Admin panel-এর সবগুলো ট্যাব (২৫টা, ইনডেক্স ০-২৪) + `ReputationDetailScreen` — সবগুলোতে Ground Rule
১৮ (whole-content flash না) প্রয়োগ হয়ে গেছে (SolverQuota/ইনডেক্স ১৯-এ ইচ্ছাকৃত ব্যতিক্রম বাদে,
সেশন ২.২৩-এর কনফার্মড সিদ্ধান্ত), আর যেখানে তালিকা আছে সেখানে GR19 (per-item pulse), যেখানে
ফিল্টার/পেজিনেশন আছে সেখানে GR20 (isFilterRefreshing), আর যেখানে ব্যবহারকারী কনফার্ম করেছেন
সেখানে GR21 (realtime-নতুন আইটেম আলাদা pulse) বসানো হয়েছে।

### পরবর্তী সেশনের জন্য যা বাকি (এখন থেকে শুধু এটাই)
**Real-device verification** — কোনো real Android build/run এই sandbox-এ সম্ভব হয়নি, তাই এখন
পর্যন্ত pending ১২টা ট্যাব/স্ক্রিন: Users, KYC, Withdrawal, Problems, Escrow, Transactions,
ChatMonitoring, DirectContracts, InstantJobs, DisputeCenter, GatewayPayments,
ReputationDetailScreen। প্রতিটার নিজস্ব সেশনের "যাচাই" অংশে যা যা চেক করতে বলা হয়েছে (pull-to-refresh
pulse, ফিল্টার/সার্চ/পেজ pulse, action-এর পর pulse, realtime-নতুন insert-এর pulse, কোথাও
stuck-shimmer না হওয়া) — এই পুরো তালিকা একসাথে Android Studio-তে verify করাই এখন এই রোডম্যাপের
একমাত্র বাকি কাজ।
