# সমাধান অ্যাপ — Admin Panel Loading/Shimmer ফিক্স — মাস্টার প্রম্পট

> **এই ফাইল কেন:** `SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md`-এ user/solver সাইডের loading/shimmer
> বাগ ফিক্স করা হয়েছে (এবং সেই ফাইলে স্পষ্ট করে **Admin panel বাদ** রাখা হয়েছিল — "ব্যবহারকারী পরে
> নিজে দেখবেন")। এখন সেই একই পদ্ধতিতে (diagnosis-first, ছোট independently-shippable সেশন, প্রতি
> সেশন শেষে zip + progress-note + কনফার্মেশন) Admin panel-এর ২৫টা ট্যাবের জন্য এই ফাইল।
>
> **আবশ্যিক পূর্বশর্ত:** কাজ শুরুর আগে অবশ্যই এই দুটো ফাইল পুরোটা পড়ে নিতে হবে —
> ১) `SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md` (৪টা কোর কম্পোনেন্ট: `rememberSessionAwareSkeletonGate`,
> `SyncAwareContent`, `SyncAwareRefreshableContent`, `SomadhanPullToRefresh`/`rememberFieldChangePulse`,
> আর Ground Rules ১-১৩), ২) `SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md` (Ground Rules ১৪-১৬ —
> systemic stuck-shimmer চেক, skeleton-shape পেজ-নির্দিষ্ট হওয়া বাধ্যতামূলক, অস্পষ্টতায় অনুমান নয়)।
> এই তিনটা ফাইলের কোনো নিয়মই এখানে পুনরায় লেখা হয়নি — সবই বহাল থাকবে।

---

## ধাপ ০ — Admin panel-এর জন্য কেন আলাদা ফাইল, এবং একটা নতুন Ground Rule

Admin panel user/solver সাইড থেকে গঠনগতভাবে আলাদা একটা জায়গায় আছে — এখানে role-branching
(ধাপ ০.৫-এর মতো user/solver সমতুল্য পেজ) নেই, একটাই admin role। কিন্তু loading-pattern মাইগ্রেশনের
অবস্থা প্রতিটা ট্যাবে **একই রকম না** — কিছু ট্যাব আগের ব্যাচেই (১১-১৮) পূর্ণাঙ্গ মাইগ্রেট হয়ে গেছে,
কিছু ট্যাব কখনোই ছোঁয়া হয়নি (এখনো fallback loading state)। তাই কাজ শুরুর আগে গ্রেপ-ভেরিফায়েড একটা
ম্যাপ দরকার — এটাই এই ধাপের মূল কাজ, অনুমান না করে।

17. **(Ground Rule ১৩ + ১৬-এর সম্প্রসারণ, Admin-স্পেসিফিক):** কোনো ট্যাবে কাজ শুরুর আগে অবশ্যই
    `AdminPanelScreen.kt`-এর `when (selectedTabIndex) { ... }` ব্লকে সেই ইনডেক্সের কোডটা নিজে পড়ে
    নিশ্চিত করতে হবে এটা এখন `SyncAwareContent`/`SyncAwareRefreshableContent`-এ wrapped কিনা —
    নিচের ম্যাপ (২০২৬-০৯-১৫ পর্যন্ত আপলোড করা zip অনুযায়ী গ্রেপ-ভেরিফায়েড) শুধু **শুরুর পয়েন্ট**,
    সোর্স অফ ট্রুথ না — কোডবেস এর মধ্যে বদলে থাকতে পারে।

18. **(নতুন, ব্যবহারকারীর স্ট্যান্ডিং সিদ্ধান্ত, সেশন ২.৫-এর পর যোগ করা হলো):** ক্যাটেগরি ৩-এর
    (এবং ভবিষ্যতে migrate হওয়া) কোনো ট্যাবেই **re-entry বা pull-to-refresh সম্পন্ন হওয়াতে পুরো
    ট্যাব (পুরো `AdminXxxView`-এর কনটেন্ট, cold-load-স্টাইল ফুল skeleton) কখনো ফ্ল্যাশ করবে না।**
    শুধু নির্দিষ্ট, ছোট অংশ (সাধারণত তালিকার কার্ড/আইটেমগুলো — KYC/Categories-এর
    `rememberFieldChangePulse` + `PulsingValue` প্যাটার্নে) pulse করবে; ফর্ম, হেডার, টগল/বাটন,
    pagination bar-এর মতো structural অংশ কখনো এই pulse-এর অংশ হবে না। **শুধু প্রথম ভিজিটে**
    (cold-load, `SyncAwareContent`-এর `rememberSessionAwareSkeletonGate`) পুরো ট্যাব skeleton
    দেখানো চলবে — এটা rule ১৮-এর ব্যতিক্রম না, কারণ সেটা "re-entry"-ও না, "pull-to-refresh"-ও না।
    এর ফলে প্রতিটা নতুন সেশনে "পুরো ট্যাব flash নাকি শুধু তালিকা" এই প্রশ্নটা আর করার দরকার নেই —
    উত্তর সবসময় "শুধু তালিকা (বা কিছুই না, যদি ব্যবহারকারী pulse-ই না চান, যেমন ২.৪)"। তবে **ঠিক
    কোন নির্দিষ্ট অংশ(গুলো)** pulse করবে (একটা তালিকা/একাধিক তালিকা/অন্য কিছু) আর pull-to-refresh
    এই ট্যাবে দরকার কিনা — এই দুটো প্রশ্ন Ground Rule ১৬ অনুযায়ী প্রতিটা ট্যাবের আগে এখনো আলাদাভাবে
    জিজ্ঞাসা করতে হবে (ট্যাব-ভেদে গঠন আলাদা হতে পারে)। **স্কোপ (আপডেটেড, ব্যবহারকারীর সিদ্ধান্ত):**
    শুধু ক্যাটেগরি ৩/২.২ না — **admin panel-এর প্রতিটা পেজেই** এই rule প্রযোজ্য, ক্যাটেগরি ১-এর
    ১১টা আগে-migrate-হওয়া ট্যাব (যেগুলো এখন `SyncAwareRefreshableContent`-এ পুরো content data
    হিসেবে পাস করে re-entry-তে whole-content flash করে) সহ, আর admin panel থেকে navigate করা
    যেকোনো আলাদা স্ক্রিন (যেমন `ReputationDetailScreen`, `AdminUsersView`-এর
    `onNavigateToReputation` থেকে পৌঁছানো যায়) সহ। ক্যাটেগরি ১-এর ১১টা ট্যাব retrofit করার জন্য
    নিচে ধাপ ২-এ নতুন সেশন তালিকা (২.১৫-২.২৫) যোগ করা হলো — প্রতিটাতেও Ground Rule ১৬ অনুযায়ী
    শুরুর আগে target design (ঠিক কোন অংশ pulse করবে) কনফার্ম করে নিতে হবে, ধরে নেওয়া যাবে না যে
    KYC/Categories-এর মতোই হবে।

19. **(নতুন, ব্যবহারকারীর স্ট্যান্ডিং সিদ্ধান্ত, সেশন ২.১৬-এর পর যোগ করা হলো):** যেকোনো তালিকা-ভিত্তিক
    ট্যাবে (LazyColumn `items(...)`) pulse/skeleton বসানোর সময় **per-item + no-scroll-pulse**
    ডিজাইনই এখন থেকে ডিফল্ট স্ট্যান্ডার্ড — Ground Rule ১৮-এর "শুধু তালিকার কার্ড/আইটেম pulse করবে"
    কথাটার বাস্তবায়ন এভাবেই হতে হবে, whole-visible-list pulse দিয়ে না। মানে:
    - `rememberFieldChangePulse` কল হবে `items(list, key = { it.id }) { item -> ... }`-এর
      **ভেতরে**, `value = item` দিয়ে (পুরো লিস্ট/visible-sublist না) — যাতে data class-এর
      equals-ভিত্তিক তুলনায় শুধু যে আইটেমের ডেটা সত্যিই বদলেছে তার কার্ডই pulse করে, একই তালিকার
      অন্য কার্ড না।
    - সেই per-item কলে **`flashOnReentry = false`** বাধ্যতামূলক — কারণ per-item কলে
      `sessionKey`/`viewModel` দিলে LazyColumn-এ স্ক্রল করে নতুন কার্ড প্রথমবার compose হওয়ার
      মুহূর্তেই সেটা "re-entry" ধরে নিয়ে অনিচ্ছাকৃত স্ক্রল-pulse দেখায়; শুধু genuine data-change আর
      `isManualRefreshing` সম্পন্ন হওয়াতেই pulse হবে।
    - এই স্ট্যান্ডার্ড **স্কোপে সব migration সেশনে প্রযোজ্য** — AdminUsersView (সেশন ২.১৬-এ প্রথম
      প্রয়োগ), AdminKycView (সেশন ২.১৭), AdminWithdrawalsView (আসন্ন সেশন — এখনো পুরনো
      whole-list প্যাটার্নে, retrofit বাকি), আর ধাপ ২-এর বাকি সব সেশন (২.৩-২.১৪, ২.১৫-২.২৫)।
      প্রতিটা নতুন সেশনে এই rule ধরে নিয়েই কাজ শুরু করতে হবে — Ground Rule ১৬ অনুযায়ী "ঠিক কোন
      অংশ pulse করবে" এখনো আলাদাভাবে জিজ্ঞাসা করা লাগবে, কিন্তু "per-item নাকি whole-list" এই
      প্রশ্নটা আর করার দরকার নেই, উত্তর সবসময় per-item।

20. **(নতুন, সেশন ২.২৮-এর ডায়াগনসিসে ব্যবহারকারীর কনফার্মেশন, কোনো কোড এডিট হয়নি সেই সেশনে):**
    Ground Rule ১৯-এর per-item pulse ডিজাইন **প্রতিস্থাপন না, সম্প্রসারণ** — item-level equality
    দিয়ে ফিল্টার/সার্চ/ট্যাব বদল ধরা পড়ে না (item নিজে বদলায় না) বলে সেই কেসে আলাদা হ্যান্ডলিং
    লাগবে। pulse-এর স্কোপ এখন থেকে তিন ভাগে standing rule:
    - **Action trigger** (approve/reject/status-update-এর মতো single-item action) → শুধু সেই
      নির্দিষ্ট item-এর কার্ড pulse করবে — Ground Rule ১৯-এর বিদ্যমান per-item
      `rememberFieldChangePulse(value = item, ...)` প্যাটার্নই যথেষ্ট, অপরিবর্তিত থাকবে।
    - **ফিল্টার/সার্চ/ট্যাব/pagination বদল** → বর্তমানে visible সব কার্ড pulse করবে — নতুন বুলিয়ান
      ফ্ল্যাগ (`isFilterRefreshing`) দরকার, `LaunchedEffect(ফিল্টার-কী, সার্চ-কী, পাতা-কী)`-তে
      true→ছোট delay→false।
      **আপডেট (২০২৬-০৯-১৫, ব্যবহারকারীর কনফার্মড সিদ্ধান্ত):** আগে এই বুলেট আর নিচের
      "শুধু pagination" বুলেট আলাদা ছিল — pagination-only বদলে কোনো pulse হতো না, `currentPage`/
      pagination-key ইচ্ছাকৃতভাবে এই effect-এর কী থেকে বাদ রাখা হতো। এখন এই দুটো বুলেট এক হয়ে
      গেছে: pagination বদলও (পাতা পাল্টানো, ফিল্টার/সার্চ/ট্যাব অপরিবর্তিত থাকলেও) filter/search/
      tab বদলের মতোই visible সব কার্ড pulse করাবে — তাই `currentPage`/pagination-key **এখন থেকে
      এই effect-এর কী-তে অন্তর্ভুক্ত করতে হবে** (আগের নির্দেশ "থাকবে না" ছিল, এখন উল্টে গেছে)।
      সেশন ২.২৯-৩১ (Users/KYC/Withdrawal, নিচে) শুরু হওয়ার আগেই এই আপডেট আসে বলে ওই তিনটা
      সেশনের টেমপ্লেটেও (নিচে) এই সংশোধিত আচরণ ধরে কাজ করতে হবে — পুরনো "pagination-এ pulse না"
      অংশ আর প্রযোজ্য না।

    **সাথে একটা আলাদা root-cause bug-ও একই ডায়াগনসিসে ধরা পড়েছে (scroll-jump):** তালিকার টপে
    auto-scroll করা `LaunchedEffect`-টা coerced/clamped page ভ্যারিয়েবলের (`safePage`/
    `safePendingPage`/ইত্যাদি) বদলে raw `currentPage`(/`pendingCurrentPage`/...) state-এর উপর key
    করতে হবে — কোনো item action-এ (approve/reject/delete) তালিকা থেকে বাদ পড়লে `totalPages` কমে
    যেতে পারে, তখন coerced ভ্যারিয়েবলটা ইউজার pagination না ছুঁলেও automatically ক্ল্যাম্প হয়ে বদলে
    যায় — এটা ভুলভাবে scroll-to-top effect ট্রিগার করে। raw `currentPage` state শুধু দুই জায়গায়
    বদলায় (ক) ফিল্টার/সার্চ রিসেট, (খ) explicit next/prev ক্লিক — কখনো action-এর side-effect-এ না
    — তাই এতে key করলে ইচ্ছাকৃত filter/pagination change-এ ঠিকই টপে scroll হবে, কিন্তু action-জনিত
    passive page-shrink-এ স্ক্রল পজিশন অক্ষত থাকবে।

    **স্কোপ:** ডায়াগনসিস ইতিমধ্যে গ্রেপ-ভেরিফাই করা হয়েছে — per-item retrofit হয়ে যাওয়া তিনটা
    ট্যাবেই (Users/ইনডেক্স ৪ সেশন ২.১৬, KYC/ইনডেক্স ১ সেশন ২.১৭, Withdrawal/ইনডেক্স ২ সেশন ২.২৭)
    একই দুই সমস্যা আছে (scroll effect coerced page-তে key করা, filter change-এ whole-visible pulse
    নেই) — এই rule কোনোটাতেই এখনো বসানো হয়নি। কাজের অর্ডার (ব্যবহারকারীর কনফার্মড সিদ্ধান্ত, নিচে
    সেশন ২.২৯-৩১): **প্রথমে Users, তারপর KYC, তারপর Withdrawal** — একবারে সব না, প্রতিটার পর
    কনফার্মেশন নিয়ে পরেরটায়। ভবিষ্যতে migrate/retrofit হওয়া বাকি সব ট্যাবেও (Ground Rule ১৯-এর
    মতোই) এটা এখন থেকে ডিফল্ট স্ট্যান্ডার্ড।

21. **(নতুন, ব্যবহারকারীর কনফার্মড সিদ্ধান্ত, ২০২৬-০৯-১৬ যোগ করা হলো, সর্বোচ্চ অগ্রাধিকার —**
    **আপডেট, সেশন ২.১৯.৩: ডিজাইন কনফার্ম হয়ে গেছে আর Users-এ implement করা হয়েছে, real-device
    ভেরিফিকেশন এখনো বাকি — বিস্তারিত ADMIN_PANEL_LOADING_PROGRESS.md-এর সেশন ২.১৯.৩ এন্ট্রি,
    আর নিচের ধাপ ৪-এর সাজেস্টেড অর্ডার):** per-item ডিজাইনের
    (Ground Rule ১৯) পেজগুলোতে একটা নিশ্চিত গ্যাপ ধরা পড়েছে — `rememberFieldChangePulse`-এর "ডেটা
    বদলেছে" তুলনা সবসময় *আগের* value-এর বিপরীতে করা হয়; কোনো item realtime-এ সম্পূর্ণ **নতুন** যোগ
    হলে তার কোনো "আগের value"-ই নেই (এটাই তার প্রথম composition) — তাই তুলনা হওয়ার কিছু নেই, pulse
    ট্রিগার হয় না। ফলে নতুন কোনো ডেটা (যেমন: একজন নতুন ইউজার সাইন-আপ করলে Users ট্যাবে, নতুন KYC
    সাবমিশন এলে KYC ট্যাবে, নতুন withdrawal request এলে Withdrawal ট্যাবে, ইত্যাদি) realtime-এ এসে
    **কোনো pulse ছাড়াই চুপচাপ তালিকায় যুক্ত হয়ে যায়** — এটাই এখন পর্যন্ত ব্যবহারকারীর দেখা আচরণ,
    আর কনফার্ম করা হয়েছে এটা কাঙ্ক্ষিত না।

    **ব্যবহারকারীর কনফার্মড কাঙ্ক্ষিত আচরণ:** realtime-এ নতুন কোনো ডেটা এলে **শুধু সেই নতুন
    ডেটার কার্ড/ভ্যালুই** pulse করবে (বাকি বিদ্যমান কার্ডগুলো অপরিবর্তিত/স্থির থাকবে) — এটা
    shared-value ডিজাইনের (AdditionalCharges/CancelledBids/Ratings-জাতীয়) "পুরো দৃশ্যমান তালিকা
    একসাথে pulse" আচরণ থেকে আলাদা, per-item গ্র্যানুলারিটিই বজায় থাকবে, শুধু নতুন আইটেমের জন্য।

    **কেন এটা সহজ ফিক্স না ছিল (technical challenge — সেশন ২.১৯.৩-এ সমাধান হয়েছে, নিচে দেখো):**
    Ground Rule ১৯-এ ইচ্ছাকৃতভাবে `flashOnReentry = false` রাখা হয়েছিল ঠিক এই কারণে যে per-item
    composable নিজে থেকে বুঝতে পারে না তার "প্রথম composition"-টা *কেন* ঘটলো — ব্যবহারকারী স্ক্রল
    করে আগে-থেকে-থাকা কোনো আইটেম প্রথমবার viewport-এ আনলো, নাকি realtime-এ সত্যিই নতুন একটা আইটেম
    ডেটাবেসে যোগ হলো — এই দুটো কেস কম্পোজেবলের দৃষ্টিকোণ থেকে হুবহু একই রকম দেখায় ("নতুন composition,
    কোনো আগের value নেই")।

    **চূড়ান্ত ডিজাইন (কনফার্মড + Users-এ implement করা হয়েছে, সেশন ২.১৯.৩):** উপরের সম্ভাব্য
    দিকনির্দেশই (viewModel/repository লেভেলে সাম্প্রতিক realtime-insert id-গুলোর short-lived
    tracking) চূড়ান্ত করা হয়েছে — কারণ `SupabaseRealtimeManager.kt`-এর `handleUserAction()`
    ইতিমধ্যেই `PostgresAction.Insert` কে `Update`/আলাদা ধরে, আর `pullBulkDataFromSupabase()`
    (bulk/cold-load) এই কোড-পাথ দিয়ে যায় না — তাই এই দুই case কোডের এই স্তরেই স্বাভাবিকভাবে
    আলাদা, নতুন heuristic লাগেনি। `PostgresAction.Insert` branch-এ সেই id একটা
    `MutableStateFlow<Set<String>>`-এ ২০০০ms TTL-এর জন্য যোগ হয় (`markRecentlyInserted()`,
    table-agnostic হেল্পার); ViewModel এটা এক্সপোজ করে; UI-তে per-item `PulsingValue`-এর
    `isUpdating`-এ তৃতীয় স্বাধীন কারণ হিসেবে OR করা হয় (বিদ্যমান `rememberFieldChangePulse`/
    `isFilterRefreshing` লজিক অপরিবর্তিত)। বিস্তারিত কোড-লেভেল বিবরণ ও real-device ভেরিফিকেশন
    checklist `ADMIN_PANEL_LOADING_PROGRESS.md`-এর সেশন ২.১৯.৩ এন্ট্রিতে।

    **স্কোপ:** Users (২.১৬/২.২৯), KYC (২.৩/২.১৭/২.৩০), Withdrawal (২.১৫/২.২৭/২.৩১), Problems (২.১৮),
    Escrow (২.১৯) — এখন পর্যন্ত সম্পন্ন সব per-item ট্যাব — এবং ভবিষ্যতে migrate/retrofit হওয়া
    যেকোনো নতুন per-item ট্যাবও (Ground Rule ১৯-এর মতোই এখন থেকে ডিফল্ট বিবেচ্য বিষয়, যদিও ফিক্সটা
    এখনো ডিজাইন/implement করা হয়নি)।

    **স্কোপ স্পষ্টীকরণ (ব্যবহারকারীর কনফার্মড নির্দেশ, ২০২৬-০৯-১৬ যোগ করা হলো — Ground Rule ২০-এর
    "স্কোপ সম্প্রসারণ" নোটের মতোই এটাও শুধু ৫টা তালিকাভুক্ত ট্যাবে সীমাবদ্ধ না):** এই রুল **যেকোনো
    ডেটা পেজে প্রযোজ্য যেখানে realtime-এ নতুন ডেটা আসতে পারে** — বর্তমান pending সেশনগুলো সহ:
    সেশন ২.২০-২.২৫ (লেনদেন হিস্ট্রি, চ্যাট মনিটরিং, সরাসরি চুক্তি, সলভার কোটা, ইনস্ট্যান্ট জবস, বিরোধ
    কেন্দ্র, গেটওয়ে পেমেন্ট — এই ৬টাতে Ground Rule ১৮-এর সাথে প্রথমবার per-item `PulsingValue`
    (Ground Rule ১৯) বসানো হবে), সেশন ২.২৬ (ReputationDetailScreen, যদি retrofit দরকার হয় বলে
    কনফার্ম হয়), আর ধাপ ২-এর বাকি যেকোনো এখনো-un-migrated ট্যাব (২.৩-২.১৪) যেগুলোতে পরে per-item
    pulse বসবে। **নিয়ম:** যে কোনো সেশনে প্রথমবার Ground Rule ১৯ (per-item pulse) কোনো ট্যাবে বসানো
    হচ্ছে, সেই একই সেশনে Ground Rule ২১-ও (ডিজাইন কনফার্মড থাকলে) একসাথে বসাতে হবে — দুটো আলাদা
    সেশনে ভাগ করে GR21 "পরে করবো" বলে ফেলে রাখা যাবে না (ঠিক Ground Rule ১৮+২০-এর মতো একই সেশনে
    bundling নিয়ম, নিচের ধাপ ২-এর "সেশন ২.১৫ থেকে ২.২৫" অংশে GR21 ওভারল্যাপ নোট দেখো)। ডিজাইন
    (realtime-নতুন বনাম স্ক্রল-এ-প্রথমবার আলাদা করার পদ্ধতি) একবার Users/KYC/Withdrawal/Problems/
    Escrow-এ কনফার্ম+implement হয়ে গেলে, সেই একই প্যাটার্ন (viewModel-লেভেল recent-insert-id
    tracking বা যা-ই চূড়ান্ত হয়) বাকি সব ট্যাবে reuse হবে — প্রতিটা ট্যাবে নতুন করে ডিজাইন
    আলোচনা লাগবে না, শুধু প্রয়োগ (apply) করতে হবে, কোড-প্যাটার্নটা কনফার্ম হয়ে গেলে সরাসরি reference
    ধরে implement করা যাবে।

    **অগ্রাধিকার (ব্যবহারকারীর স্পষ্ট নির্দেশ):** পরবর্তী Claude সেশন **সবার আগে** এই গ্যাপ নিয়ে কাজ
    করবে (আগে ডিজাইন নিয়ে ব্যবহারকারীর কনফার্মেশন নেবে, তারপর implement করবে) — এটা confirm/সমাধান
    না হওয়া পর্যন্ত বাকি কোনো pending সেশন (২.২০ থেকে শুরু — Transactions/ChatMonitoring/
    DirectContracts/SolverQuota/InstantJobs/DisputeCenter/GatewayPayments, বা ReputationDetailScreen/
    ২.২৬) শুরু করা যাবে না।

---

## ধাপ ০.৫ — বর্তমান অবস্থার ম্যাপ (গ্রেপ-ভেরিফায়েড, ২০২৬-০৯-১৫)

`AdminPanelScreen.kt`-এর `when (selectedTabIndex)`-এর প্রতিটা ব্লক নিজে পড়ে যাচাই করা হয়েছে
(শুধু ফাইলের নাম অনুমান করা না) — প্রতিটা ইনডেক্স কোন wrapper ব্যবহার করছে:

### ক্যাটেগরি ১ — `SyncAwareRefreshableContent` (rule ১+২+৩+৪, ডেটা-ডিফ শিমার + pull-to-refresh সহ পূর্ণাঙ্গ) — ১১টা ট্যাব
```
২  উইথড্রয়াল (AdminWithdrawalsView)         ৪  ইউজারগণ (AdminUsersView)
৫  সমস্যাসমূহ (AdminProblemsView)            ৭  Escrow (AdminEscrowView)
৯  লেনদেন হিস্ট্রি (AdminTransactionsView)   ১৫ চ্যাট মনিটরিং (AdminChatMonitoringView)
১৬ সরাসরি চুক্তি (AdminDirectContractsView)  ১৯ সলভার কোটা (AdminSolverQuotaView)
২১ ইনস্ট্যান্ট জবস (AdminInstantJobsView)    ২২ বিরোধ কেন্দ্র (AdminDisputeCenterView)
২৩ গেটওয়ে পেমেন্ট (AdminGatewayPaymentsView)
```
এই ১১টা ট্যাব শেয়ার্ড `MotionToolkit.kt` কম্পোনেন্ট ব্যবহার করে বলে, **`SHIMMER_REGRESSION_BATCH31...`
-এর বাগ ১ (স্টাক-শিমার, `flashRequestCount` কখনো `finally`-তে decrement হয় না) এই সবগুলো ট্যাবেই
সমানভাবে প্রযোজ্য** — user-সাইডে (সেশন ২.৩, Wallet) এই বাগের ফিক্স শেয়ার্ড কম্পোনেন্টে বসানো হলে
এখানেও এমনিতেই প্রযোজ্য হয়ে যাবে, কিন্তু **admin ট্যাবে আলাদাভাবে real-device এ verify করা হয়নি
এখনো** — ধরে নেওয়া যাবে না।

### ক্যাটেগরি ২ — `SyncAwareContent` (rule ১+২ শুধু, ডেটা-ডিফ/pull-to-refresh নেই) — ২টা ট্যাব
```
০  পরিসংখ্যান ও চার্ট (AdminStatsView, sessionKey "admin_overview_sync")
১৮ ইউজার/সলভার সার্চ (AdminUserLookupView)
```
ইনডেক্স ০ (`AdminStatsView`) নিজেই ভেতরে ব্যাচ ১১-১৫-এ বানানো "independent shimmer-zone" প্যাটার্নে
১২+টা আলাদা pilot section আছে (কোড-কমেন্টে "শিমার-জোন পাইলট #১" থেকে "#১২" পর্যন্ত নাম্বার করা) —
এটা সবচেয়ে ম্যাচিউর/জটিল ট্যাব, আলাদা যত্ন লাগবে।

### ক্যাটেগরি ৩ — কোনো wrapper নেই (rule ১/২/৩/৪ কোনোটাই নেই, raw prop pass করে সরাসরি view কল হয়) — ১২টা ট্যাব
```
১  কেওয়াইসি (AdminKycView)                   ৩  বিজ্ঞপ্তি প্রেরণ (AdminManualNotificationView)
৬  ক্যাটাগরি (AdminCategoriesView)             ৮  অতিরিক্ত চার্জ (AdminAdditionalChargesView)
১০ রিভিউ মডারেশন (AdminRatingsView)            ১১ সেটিংস (AdminSettingsView)
১২ অ্যাক্টিভিটি লগ (AdminAuditLogView)         ১৩ FAQ ম্যানেজমেন্ট (AdminFaqManagementView)
১৪ বিড বাতিলের ইতিহাস (AdminCancelledBidsView) ১৭ Supabase এক্সপ্লোরার (AdminSupabaseExplorerView)
২০ রেপুটেশন ইঞ্জিন (AdminReputationEngineView) ২৪ রিফান্ড ডায়াগনস্টিক (AdminRefundDebugView)
```
এই ১২টা ট্যাবে rule ১ (cold-load skeleton gate) কাঠামোগতভাবেই নেই — ভেতরের ফাইলে যা-ই লোডিং UI
থাকুক (বেশিরভাগে plain `CircularProgressIndicator`, গ্রেপ-ভেরিফায়েড), সেটা এই মাস্টার প্রম্পট সিস্টেমের
অংশ না। **এগুলোকে ধরে নেওয়া যাবে না যে "বাগযুক্ত" — এখনো migrate-ই করা হয়নি, এটা category B২-এর মতো
(নতুন migration কাজ), bug-fix না।**

---

## ধাপ ১ — প্রথম সেশন: ডায়াগনসিস + স্কোপ কনফার্মেশন (কোনো কোড এডিট না)

```
প্রম্পট (সেশন ১ — শুধু ডায়াগনসিস, কোনো কোড এডিট না):

SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md আর SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md পড়ে
কোর কম্পোনেন্ট আর Ground Rules বুঝে নাও। উপরের ধাপ ০.৫-এর ম্যাপ নিজে grep/view দিয়ে পুনরায়
ভেরিফাই করো (কোডবেস বদলে থাকতে পারে) — কোনো ট্যাব ভুল ক্যাটেগরিতে থাকলে ঠিক করে দাও।

তারপর দুটো জিনিস রিপোর্ট করো (কোনো এডিট না):

১. ক্যাটেগরি ১-এর (Refreshable) ১১টা ট্যাবের মধ্যে কোনোটায় বাগ ১-এর (স্টাক শিমার, `flashRequestCount`
   `finally` ছাড়া) লক্ষণ ব্যবহারকারী real device-এ দেখেছেন কিনা জিজ্ঞাসা করো (Ground Rule ১৬)। যদি
   দেখে থাকেন, কোন ট্যাবে/কোন action-এর পরে তা নোট করো।
২. ক্যাটেগরি ৩-এর ১২টা ট্যাবের প্রতিটার জন্য এখন ভেতরে (Admin*View.kt ফাইলে) ঠিক কী loading UI আছে
   (plain spinner/কিছুই না/আংশিক) তা grep করে একটা সংক্ষিপ্ত টেবিল বানাও, যাতে ধাপ ২-এর মাইগ্রেশন
   সেশনগুলো শুরুর আগে জানা থাকে ঠিক কতটুকু কাজ বাকি।

আউটপুট: টেক্সট রিপোর্ট, কোনো কোড এডিট/zip না।
```

এই রিপোর্টের পর ব্যবহারকারী প্রতিটা ট্যাবের জন্য **priority ঠিক করে দেবেন** (সবগুলো migrate করা
লাগবে কিনা, নাকি শুধু বেশি-ব্যবহৃত কয়েকটা) — ধাপ ২-এ যাওয়ার আগে এই কনফার্মেশন বাধ্যতামূলক
(Ground Rule ১৬)।

---

## ধাপ ২ — সেশন-ভিত্তিক কাজের তালিকা

### সেশন ২.১ — ক্যাটেগরি ১-এর ১১টা Refreshable ট্যাব: bug-1 verify (fix না, শুধু চেক)
```
ক্যাটেগরি ১-এর ১১টা ট্যাব (উইথড্রয়াল, ইউজারগণ, সমস্যাসমূহ, Escrow, লেনদেন হিস্ট্রি, চ্যাট মনিটরিং,
সরাসরি চুক্তি, সলভার কোটা, ইনস্ট্যান্ট জবস, বিরোধ কেন্দ্র, গেটওয়ে পেমেন্ট) — প্রতিটাতে কোনো action
(status update, approve/reject, bulk action ইত্যাদি) করার পর ট্যাব স্টাক-শিমারে পড়ে কিনা সেটা user/solver
ওয়ালেট-বাগের (ধাপ ১ ডায়াগনসিস, SHIMMER_REGRESSION_BATCH31...) একই root-cause pattern কিনা কোড
পড়ে যাচাই করো (একই `SyncAwareRefreshableContent`-এর `data = listOf(...)` কী একাধিক সোর্স থেকে
কাছাকাছি সময়ে বদলায় কিনা — যেমন `AdminWithdrawalsView`-এ status আপডেট + notification insert)।
যদি শেয়ার্ড কম্পোনেন্ট ফিক্স (`finally { flashRequestCount-- }`) ইতিমধ্যে অন্য ব্যাচে বসানো হয়ে
থাকে, শুধু নিশ্চিত করো সেটা এখনো আছে (grep) — নতুন করে বসানোর দরকার নেই, ডুপ্লিকেট ফিক্স করবে না।
কোনো নতুন কোড-বাগ না পেলে "বহাল আছে" লিখে প্রোগ্রেস নোটে রাখো।
```

### সেশন ২.২ — Overview (`AdminStatsView`, ইনডেক্স ০) + ইউজার/সলভার সার্চ (ইনডেক্স ১৮)
```
এই দুটো ট্যাব শুধু `SyncAwareContent` (rule ১+২) ব্যবহার করে, pull-to-refresh (rule ৩) নেই।
ব্যবহারকারীকে জিজ্ঞাসা করো (Ground Rule ১৬, অনুমান না করে) — এই দুই ট্যাবে pull-to-refresh ইচ্ছাকৃতভাবে
বাদ দেওয়া হয়েছিল (যেমন `ProfileScreen`-এ ইচ্ছাকৃত ব্যতিক্রম আছে) নাকি এটা যোগ করা দরকার। উত্তর
অনুযায়ী `SyncAwareRefreshableContent`-এ upgrade করো বা ইচ্ছাকৃত ব্যতিক্রম হিসেবে নোট করে রাখো।
`AdminStatsView`-এর ১২টা "independent shimmer-zone" পাইলট আলাদাভাবে grep করে দেখো সবগুলোর
skeleton-shape এখনো নিজ নিজ real content-এর layout-এর সাথে মিলছে কিনা (ধাপ ০.১৫)।
```

### সেশন ২.৩ থেকে ২.১৪ — ক্যাটেগরি ৩-এর ১২টা un-migrated ট্যাব (একটা একটা করে, নিজস্ব সেশন)

> নিচের প্রতিটা সেশনের টেমপ্লেট একই — `SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md`-এর ধাপ ২ (নতুন
> স্ক্রিনে ৪টা rule বসানোর চেকলিস্ট) হুবহু অনুসরণ করতে হবে। **Ground Rule ১৮ (উপরে) অনুযায়ী পুরো
> ট্যাব কখনো re-entry/pull-to-refresh-এ flash করবে না — শুধু ঠিক কোন নির্দিষ্ট অংশ(গুলো) pulse
> করবে আর pull-to-refresh এই ট্যাবে দরকার কিনা, এই দুটো প্রতিটা ট্যাবের আগে ব্যবহারকারীর কাছ থেকে
> confirm করে নিতে হবে — কোডে দেখে অনুমান করা যাবে না (Ground Rule ১৬)।**

```
টেমপ্লেট (প্রতিটা ট্যাবের জন্য আলাদা প্রম্পট, [X] জায়গায় নিচের তালিকা থেকে বসাও):

[X] ট্যাবে (Admin[X]View.kt) এখনো rule ১-৪ কিছুই বসানো হয়নি — বর্তমানে [grep করে যা পাওয়া গেছে তা
লেখো, যেমন plain CircularProgressIndicator]। Ground Rule ১৮ অনুযায়ী `AdminPanelScreen.kt`-এর
ইনডেক্স [N]-এ সবসময় plain `SyncAwareContent` (rule ১+২, `data` প্যারামিটার-বিহীন) দিয়ে **পুরো
ট্যাব** wrap করো (কখনো `SyncAwareRefreshableContent`-এ পুরো ভিউ-এর props/data হিসেবে পাস করে
whole-content flash করানো যাবে না — সেটাই Ground Rule ১৮-এর নিষেধ), নতুন `sessionKey` দাও
("admin_[x]_sync" প্যাটার্নে, অন্য ট্যাবের সাথে না মেলা করে)। এর ভেতরে যদি re-entry/data-change/
pull-to-refresh-এ pulse দরকার হয় (ব্যবহারকারী জানালে), সেটা `Admin[X]View.kt`-এর **ভেতরে** আলাদাভাবে
`rememberFieldChangePulse` + `PulsingValue` দিয়ে শুধু সেই নির্দিষ্ট অংশে (সাধারণত তালিকার
কার্ড/আইটেম) বসাও — KYC (সেশন ২.৩)/ক্যাটাগরি (সেশন ২.৫) রেফারেন্স প্যাটার্ন। শুরুর আগে ব্যবহারকারীকে
জিজ্ঞাসা করো: (ক) ঠিক কোন নির্দিষ্ট অংশ(গুলো) pulse করবে (নাকি কিছুই না, Manual Notification/সেশন
২.৪-এর মতো), (খ) pull-to-refresh (`isManualRefreshing`) এই ট্যাবে দরকার কিনা (কিছু dev/debug-টাইপ
ট্যাবে হয়তো দরকার নেই — ধরে নেওয়া যাবে না)। cold-load skeleton-এর shape এই ট্যাবের real layout
অনুযায়ী বানাও (ধাপ ০.১৫, জেনেরিক বক্স-শিমার না)। কোনো বিদ্যমান ফাংশনালিটি/action বদলাবে না।

**নোট (Ground Rule ২০ ওভারল্যাপ — এড়ানো যাবে না, ২০২৬-০৯-১৫ যোগ করা হলো):** এই তালিকার
`AdminManualNotificationView.kt` (সেশন ২.৪) Ground Rule ২০-এর candidate-তালিকায় আছে (নিচে "সেশন
২.২৯ থেকে শুরু" অংশের "স্কোপ সম্প্রসারণ" নোট দেখো) — filter/pagination state + safePage clamp +
scroll-effect ইতিমধ্যে আছে, শুধু per-item `PulsingValue` নেই বলে এখনো candidate হিসেবে গণ্য না।
এই সেশনে ওপরের টেমপ্লেট অনুযায়ী rule ১-৪ + per-item `PulsingValue` বসানোর ঠিক পরেই — একই সেশনে,
ভবিষ্যতের জন্য ফেলে না রেখে — Ground Rule ২০-এর ৩-শর্তের গ্রেপ (`coerceIn(1,` + `PulsingValue` +
`scrollToItem(0)`) আবার চালাও। এখন candidate হয়ে গেলে Ground Rule ২০-ও (নিচের সেশন ২.২৯-থেকে
টেমপ্লেট) একই সেশনে প্রয়োগ করো — দুই সেশনে ভাগ করা যাবে না।

তালিকা (ইনডেক্স — ট্যাব — নোট):
২.৩  ১  কেওয়াইসি — approve/reject/bulk-approve action ভারী, action-এর পর stuck-shimmer না হয় সেটা
        বিশেষভাবে যাচাই করো (bug-1 class রিস্ক নতুন কোডেও ঢুকতে পারে)
২.৪  ৩  বিজ্ঞপ্তি প্রেরণ — scheduled/cancel/delete action আছে, একই সতর্কতা
২.৫  ৬  ক্যাটাগরি — সাধারণ CRUD লিস্ট
২.৬  ৮  অতিরিক্ত চার্জ — settle action আছে
২.৭  ১০ রিভিউ মডারেশন
২.৮  ১১ সেটিংস — এটা এডিট-ফর্ম-ঘেঁষা হতে পারে; আগে ব্যবহারকারীকে জিজ্ঞাসা করো এটা
        SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md-এর ক্যাটেগরি E (এডিট-ফর্ম, ইচ্ছাকৃত ব্যতিক্রম,
        rule ২ থেকে বাদ)-এর মতো ট্রিট করা উচিত কিনা, নাকি সাধারণ list-migration
২.৯  ১২ অ্যাক্টিভিটি লগ — সম্ভবত pagination আছে কিনা grep করে দেখো (থাকলে pagination-এর rule-ও
        যোগ করতে হবে, SOMADHAN_LOADING_PATTERN...-এর pagination প্যাটার্ন অনুসরণ করো)
২.১০ ১৩ FAQ ম্যানেজমেন্ট
২.১১ ১৪ বিড বাতিলের ইতিহাস — সম্ভবত pagination, ২.৯-এর মতো চেক করো
২.১২ ১৭ Supabase এক্সপ্লোরার — dev-tool, ব্যবহারকারীকে জিজ্ঞাসা করো এই মাস্টার প্রম্পটের স্কোপে
        রাখা দরকার কিনা নাকি ইচ্ছাকৃতভাবে বাদ (debug screen বলে UX polish-এর অগ্রাধিকার কম হতে পারে)
২.১৩ ২০ রেপুটেশন ইঞ্জিন
২.১৪ ২৪ রিফান্ড ডায়াগনস্টিক — dev-tool, ২.১২-এর মতো একই প্রশ্ন
```

### সেশন ২.১৫ থেকে ২.২৫ — ক্যাটেগরি ১-এর ১১টা আগে-migrate-হওয়া ট্যাব: Ground Rule ১৮-এ retrofit

> Ground Rule ১৮-এর আপডেটেড স্কোপ (ব্যবহারকারীর সিদ্ধান্ত) অনুযায়ী এই ১১টা ট্যাবও এখন কাজের আওতায়।
> এগুলো **rule ১/২/৩/৪ ইতিমধ্যে পূর্ণাঙ্গ কাজ করছে** — এটা bug-fix না, শুধু re-entry/pull-to-refresh
> behavior-এর presentation বদল (whole-content flash → শুধু নির্দিষ্ট অংশ pulse)। প্রতিটার টেমপ্লেট:
>
> **নোট (Ground Rule ২০ ওভারল্যাপ — এড়ানো যাবে না, ২০২৬-০৯-১৫ যোগ করা হলো):** এই ১১টা ট্যাবের
> মধ্যে ৭টা — `AdminProblemsView.kt` (২.১৭), `AdminEscrowView.kt` (২.১৮),
> `AdminTransactionsView.kt` (২.১৯), `AdminChatMonitoringView.kt` (২.২০),
> `AdminDirectContractsView.kt` (২.২১), `AdminInstantJobsView.kt` (২.২৩),
> `AdminGatewayPaymentsView.kt` (২.২৫) — Ground Rule ২০-এর candidate-তালিকায় আছে (নিচে "সেশন
> ২.২৯ থেকে শুরু" অংশের "স্কোপ সম্প্রসারণ" নোট দেখো): filter/pagination state + safePage clamp +
> scroll-effect ইতিমধ্যে আছে, শুধু per-item `PulsingValue` নেই বলে এখনো candidate হিসেবে গণ্য না।
> এই ৭টার যেকোনোটায় নিচের টেমপ্লেট অনুযায়ী Ground Rule ১৮ (per-item `PulsingValue` সহ) বসানোর ঠিক
> পরেই — একই সেশনে, ভবিষ্যতের জন্য ফেলে না রেখে — Ground Rule ২০-এর ৩-শর্তের গ্রেপ (`coerceIn(1,`
> + `PulsingValue` + `scrollToItem(0)`) আবার চালাও। এখন candidate হয়ে গেলে Ground Rule ২০-ও (নিচের
> সেশন ২.২৯-থেকে টেমপ্লেট) একই সেশনে প্রয়োগ করো। **ব্যবহারকারীর নির্দেশ: Ground Rule ১৮ আর Ground
> Rule ২০ কখনো দুই আলাদা সেশনে ভাগ করা যাবে না যদি একই সেশনে candidate হয়ে যায়।**
>
> **নোট (Ground Rule ২১ ওভারল্যাপ — এড়ানো যাবে না, ২০২৬-০৯-১৬ যোগ করা হলো):** এই ১১টা ট্যাবের
> প্রতিটাতেই (বিশেষত এখনো-বাকি ৬টা: ২.২০ চ্যাট মনিটরিং, ২.২১ সরাসরি চুক্তি, ২.২২ সলভার কোটা, ২.২৩
> ইনস্ট্যান্ট জবস, ২.২৪ বিরোধ কেন্দ্র, ২.২৫ গেটওয়ে পেমেন্ট) Ground Rule ১৮-এর সাথে যখন প্রথমবার
> per-item `PulsingValue` (Ground Rule ১৯) বসানো হবে, ঠিক তখনই realtime-এ সম্পূর্ণ **নতুন** আইটেম
> (নতুন লেনদেন, নতুন চ্যাট মেসেজ, নতুন direct contract, ইত্যাদি) এলে Ground Rule ২১-এর একই গ্যাপ
> (কোনো pulse ছাড়াই চুপচাপ যুক্ত হওয়া) স্বাভাবিকভাবেই তৈরি হবে — কারণ এটা `rememberFieldChangePulse`-এর
> মৌলিক ডিজাইন-সীমাবদ্ধতা, প্রতিটা নতুন ট্যাবেই নতুন করে ঘটবে। তাই **Ground Rule ১৯ বসানোর ঠিক
> পরেই — একই সেশনে — Ground Rule ২১-ও বসাতে হবে** (যদি ততক্ষণে Users/KYC/Withdrawal/Problems/
> Escrow-এ ডিজাইন কনফার্ম+implement হয়ে গিয়ে থাকে; সেই কনফার্মড প্যাটার্নটাই এখানে reuse/apply
> করা হবে, নতুন করে আলোচনার দরকার নেই)। GR18/GR19/GR20-এর মতোই GR21-ও এই সেশনগুলোর অংশ, আলাদা
> ভবিষ্যৎ সেশনের জন্য ফেলে রাখা যাবে না।

```
[X] ট্যাবে (Admin[X]View.kt) বর্তমানে `SyncAwareRefreshableContent`-এ পুরো content
(withdrawals/users/... সহ allUsers-এর মতো related props) `data` হিসেবে পাস করা হয় বলে re-entry/
pull-to-refresh/data-change-এ পুরো তালিকা+যেকোনো summary card/header (যা এই `data`-এর ভেতরে) একসাথে
flash হয়। Ground Rule ১৮ অনুযায়ী এটা বদলাতে হবে: `AdminPanelScreen.kt`-এর ইনডেক্স [N]-কে plain
`SyncAwareContent` (rule ১+২ cold-load শুধু, `data`-বিহীন) দিয়ে wrap করো (`SyncAwareRefreshableContent`
বাদ দাও), sessionKey অপরিবর্তিত রাখো। `Admin[X]View.kt`-এর ভেতরে নতুন `viewModel`/`isManualRefreshing`
প্যারামিটার (KYC/Categories প্যাটার্নে) যোগ করে `rememberFieldChangePulse` + `PulsingValue` দিয়ে
শুধু আসল তালিকার কার্ড/আইটেমে pulse বসাও — সামারি/স্ট্যাট কার্ড, ফিল্টার/সার্চ বার, হেডার এর বাইরে
রেখো (এর আগে ব্যবহারকারীকে জিজ্ঞাসা করো ঠিক কোন অংশ(গুলো), যেমন যদি একাধিক লিস্ট/ট্যাব থাকে)। bug-1
ক্লাস রিস্ক (স্টাক-শিমার) নতুন কোডেও ঢুকতে পারে বলে action-এর পর বিশেষভাবে যাচাই করো। কোনো বিদ্যমান
ফাংশনালিটি/action/pagination behavior বদলাবে না।

তালিকা (ইনডেক্স — ট্যাব):
২.১৫ ২  উইথড্রয়াল          ২.১৬ ৪  ইউজারগণ
২.১৭ ৫  সমস্যাসমূহ          ২.১৮ ৭  Escrow
২.১৯ ৯  লেনদেন হিস্ট্রি      ২.২০ ১৫ চ্যাট মনিটরিং
২.২১ ১৬ সরাসরি চুক্তি        ২.২২ ১৯ সলভার কোটা
২.২৩ ২১ ইনস্ট্যান্ট জবস      ২.২৪ ২২ বিরোধ কেন্দ্র
২.২৫ ২৩ গেটওয়ে পেমেন্ট
```

### সেশন ২.২৬ — `ReputationDetailScreen` (admin panel থেকে navigate করা, tab-index সিস্টেমের বাইরে)

```
এই স্ক্রিন `AdminUsersView`-এর `onNavigateToReputation`-এ navigate হয়, `SOMADHAN_LOADING_PATTERN_
MASTER_PROMPT.md`-এর আওতায় আগে rule ১+২+৩ পূর্ণাঙ্গ প্যাটার্নে মাইগ্রেট হয়েছিল (ওই ফাইলের নিজস্ব
ব্যাচে, admin-panel-স্পেসিফিক এই ফাইলের বাইরে)। আগে গিয়ে grep করে দেখো এখনো whole-content flash
করে কিনা (SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md-এর "বর্তমান স্ট্যাটাস" সেকশন প্রথমে দেখে নাও,
কোডে গিয়ে ভেরিফাই করো, অনুমান না করে) — যদি করে, Ground Rule ১৮ অনুযায়ী একই retrofit দরকার কিনা তা
ব্যবহারকারীকে জিজ্ঞাসা করো (এই স্ক্রিনটা user/solver সাইডের প্যাটার্নেরও অংশ বলে দুটো master prompt
ফাইলেই progress আপডেট করতে হতে পারে)।
```

### সেশন ২.২৯ থেকে শুরু — Ground Rule ২০ রেট্রোফিট: Users → KYC → Withdrawal → বাকি সব ম্যাচিং ট্যাব (ওপেন-এন্ডেড, একটা একটা করে)

> **স্কোপ সম্প্রসারণ (ব্যবহারকারীর কনফার্মড সিদ্ধান্ত, ২০২৬-০৯-১৫, ২.৩০-এর ঠিক আগে যোগ করা হলো):**
> Ground Rule ২০ শুধু Users/KYC/Withdrawal-এ সীমাবদ্ধ না — **যেকোনো Admin ট্যাবে যদি একই তিনটা
> উপাদান একসাথে থাকে** (ক) filter/search/sort/pagination state, (খ) `coerceIn(1, ...)` দিয়ে
> ক্ল্যাম্প করা "safe page" ভ্যারিয়েবল (`safePage`/`safeXxxPage`), (গ) per-item `PulsingValue`
> (মানে Ground Rule ১৯-এর action-pulse ইতিমধ্যে বসানো আছে) — তাহলে সেই ট্যাবেও এই একই দুই বাগ
> (stuck-shimmer risk + scroll-jump) থাকার সম্ভাবনা আছে আর একই রেট্রোফিট (নিচের টেমপ্লেট)
> প্রযোজ্য। KYC (২.৩০) শেষ হওয়ার পর Withdrawal (২.৩১)-এই থেমে যাওয়া যাবে না — তারপরের সেশনে
> (২.৩২ থেকে) **নিচের ৩-শর্তের গ্রেপ পুরো `app/src/main/java/com/example/ui/screens/Admin*.kt`-এ
> নতুন করে চালিয়ে** যে কোনো ট্যাব ম্যাচ করে (আর তখনো `isFilterRefreshing`-জাতীয় ফ্ল্যাগ নেই) সেগুলোর
> একটা তালিকা বানাও, ব্যবহারকারীকে দেখাও, কনফার্ম করে একটা একটা করে (Ground Rule ১৬) একই প্যাটার্নে
> রেট্রোফিট করতে থাকো — যতক্ষণ না পুরো তালিকা শেষ হয়। **অনুমান না করে প্রতিবার নিজে গ্রেপ করে
> ভেরিফাই করো** — কোডবেস সময়ের সাথে বদলে যেতে পারে।
>
> গ্রেপ কমান্ড (রেফারেন্স, প্রতিটা `Admin*.kt`-এ চালাও):
> `coerceIn(1,` + `PulsingValue` + `scrollToItem(0)` — তিনটাই ≥১ বার থাকলে candidate;
> `isFilterRefreshing` ইতিমধ্যে থাকলে (Users/Withdrawal-এর মতো) সেটা বাদ দাও (already done)।
>
> **২০২৬-০৯-১৫ পর্যন্ত এই zip-এ প্রাথমিক গ্রেপ-স্ক্যানে পাওয়া candidate (পরের সেশনে অবশ্যই আবার
> নিজে গ্রেপ করে কনফার্ম করতে হবে, এই তালিকা শুধু শুরুর পয়েন্ট, সোর্স অফ ট্রুথ না):**
> - `AdminAdditionalChargesView.kt` — `coerceIn(1,`×১, `PulsingValue`×২, `scrollToItem(0)`×১
> - `AdminCancelledBidsView.kt` — `coerceIn(1,`×১, `PulsingValue`×৭, `scrollToItem(0)`×১
> - `AdminRatingsView.kt` — `coerceIn(1,`×১, `PulsingValue`×২, `scrollToItem(0)`×১
>
> এই তিনটাতে এখনো `isFilterRefreshing` নেই বলে সম্ভাব্য পরের টার্গেট, কিন্তু KYC/Withdrawal-এর মতো
> এদেরও প্রতিটার filter/pagination state নাম আলাদাভাবে কোডে গিয়ে পড়ে নিশ্চিত হতে হবে (অনুমান না
> করে)। এছাড়া কিছু ট্যাবে (`AdminChatMonitoringView.kt`, `AdminDirectContractsView.kt`,
> `AdminEscrowView.kt`, `AdminGatewayPaymentsView.kt`, `AdminInstantJobsView.kt`,
> `AdminManualNotificationView.kt`, `AdminProblemsView.kt`, `AdminTransactionsView.kt`) safePage +
> scroll-effect আছে কিন্তু `PulsingValue` নেই — মানে Ground Rule ১৯-এর per-item action-pulse এখনো
> বসানো হয়নি, তাই Ground Rule ২০ সরাসরি প্রযোজ্য না (আগে Ground Rule ১৯ লাগবে কিনা সেটা আলাদা প্রশ্ন,
> এই রেট্রোফিটের স্কোপের বাইরে — ব্যবহারকারীকে জানিয়ে রাখা ভালো, কিন্তু নিজে থেকে সেটা শুরু করা যাবে
> না)। যে ট্যাবগুলোতে `PulsingValue` আছে কিন্তু `coerceIn(1,`/`scrollToItem(0)` নেই
> (`AdminAuditLogView.kt`, `AdminCategoriesView.kt`, `AdminFaqManagementView.kt`,
> `AdminStatsView.kt`, `AdminUserLookupView.kt`) — এদের pagination হয় নেই বা ভিন্ন প্যাটার্নে, তাই
> এই নির্দিষ্ট scroll-jump বাগটা প্রযোজ্য নাও হতে পারে, তবুও কাজ শুরুর আগে গ্রেপ করে নিশ্চিত হওয়া
> বাধ্যতামূলক।

> `ADMIN_PANEL_LOADING_PROGRESS.md`-এর "✅ সেশন ২.২৮ — ডায়াগনসিস" এন্ট্রি আগে পড়ে নাও (root-cause
> ইতিমধ্যে দুই ফাইলেই লাইন-নাম্বার সহ গ্রেপ-ভেরিফায়েড, নতুন করে অনুমান করার দরকার নেই)। প্রতিটা
> ট্যাবের টেমপ্লেট একই — Ground Rule ২০-এর তিন-ভাগের স্কোপ + scroll-jump ফিক্স:

```
[X] ট্যাবে (Admin[X]View.kt) Ground Rule ১৯-এর per-item pulse ইতিমধ্যে আছে (action-এ শুধু সেই
কার্ডই pulse করে — এই অংশ অপরিবর্তিত রাখো)। Ground Rule ২০ অনুযায়ী যোগ করো:
১. নতুন `isFilterRefreshing` (বা প্রতি-ট্যাব নাম, একাধিক sub-tab থাকলে যেমন KYC-এ প্রতিটার জন্য
   আলাদা লাগতে পারে) বুলিয়ান স্টেট — `LaunchedEffect(ফিল্টার-স্টেট-কী(গুলো), সার্চ-কী, pagination-কী)`-তে
   true → ছোট delay (বাকি per-item pulse-এর delay-র সাথে সামঞ্জস্যপূর্ণ duration) → false।
   **আপডেট (২০২৬-০৯-১৫):** `currentPage`(/প্রতি-ট্যাব pagination state) **এখন এই effect-এর
   কী-তে থাকতে হবে** (আগে এই টেমপ্লেটে "দেওয়া যাবে না" লেখা ছিল — ব্যবহারকারীর নতুন কনফার্মড
   সিদ্ধান্তে pagination বদলও filter/search/tab বদলের মতোই visible সব কার্ড pulse করাবে, দেখো
   Ground Rule ২০-এর আপডেট নোট)।
২. `items(...)`-এর ভেতরের per-item `PulsingValue(isUpdating = ...)`-এ এই ফ্ল্যাগটাও OR করে
   দাও (action-pulse অথবা filter-pulse, যেটাই true সেটাতেই কার্ড pulse করবে) — item-level
   `rememberFieldChangePulse` লজিক স্পর্শ না করে।
৩. টপ-এ auto-scroll করা `LaunchedEffect(safePage...)` (বা multi-tab হলে
   `safePendingPage/safeVerifiedPage/...`)-কে raw, un-coerced page state
   (`currentPage`/`pendingCurrentPage`/...)-এ re-key করো — coerced ভ্যারিয়েবলটা action-জনিত
   passive page-shrink-এ নিজে থেকে বদলে গিয়ে ভুল scroll-to-top ট্রিগার না করে সেটা নিশ্চিত করো।
   pagination bounds-check (out-of-range হলে ক্ল্যাম্প করে দেখানো) অন্য কোথাও এখনো `safePage`
   ব্যবহার করবে — শুধু এই একটা scroll-effect-এর key বদলাবে।
কোনো বিদ্যমান ফাংশনালিটি/action/pagination bounds-logic বদলাবে না, শুধু pulse-scope আর
scroll-trigger-key।

তালিকা (নিশ্চিত/সম্পূর্ণ অংশ — এই অর্ডারেই, একবারে সব না — প্রতিটার পর ব্যবহারকারীর কনফার্মেশন নিয়ে পরেরটায়):
✅ ২.২৯  ৪  ইউজারগণ (AdminUsersView.kt) — সম্পূর্ণ
✅ ২.৩১  ২  উইথড্রয়াল (AdminWithdrawalsView.kt) — সম্পূর্ণ (অর্ডার-বিচ্যুতি: zip-নাম সিগন্যালে ২.৩০-এর
         আগেই হয়ে গেছে, ADMIN_PANEL_LOADING_PROGRESS.md-এর সেশন ২.৩১ এন্ট্রিতে নোট করা আছে)
✅ ২.৩০  ১  কেওয়াইসি (AdminKycView.kt — তিনটা sub-tab, শেয়ার্ড `isFilterRefreshing` একটা ফ্ল্যাগ
         দিয়েই তিনটা sub-tab-এর pagination-কী একসাথে কভার করা হয়েছে) — সম্পূর্ণ
✅ ২.৩২  ৮  অতিরিক্ত চার্জ (AdminAdditionalChargesView.kt) — সম্পূর্ণ (shared-value pulse প্যাটার্ন
         আগে থেকেই ছিল বলে `isFilterRefreshing` লাগেনি, শুধু scroll-jump ফিক্স করা হয়েছে)
✅ ২.৩৩  ১৪ বিড বাতিলের ইতিহাস (AdminCancelledBidsView.kt) — সম্পূর্ণ (একই shared-value প্যাটার্ন,
         শুধু scroll-jump ফিক্স)
✅ ২.৩৪  ১০ রিভিউ মডারেশন (AdminRatingsView.kt) — সম্পূর্ণ (একই shared-value প্যাটার্ন, শুধু
         scroll-jump ফিক্স) — candidate তালিকার শেষ আইটেম

**Ground Rule ২০ রেট্রোফিট এখন সম্পূর্ণ (২০২৬-০৯-১৫, ৬টা ট্যাবই — Users→KYC→Withdrawal→
AdditionalCharges→CancelledBids→Ratings)।** সেশন ২.৩৪-এর শেষে পুরো `Admin*View.kt` তালিকায়
৩-শর্তের গ্রেপ (`coerceIn(1,` + `PulsingValue` + `scrollToItem(0)`) আরেকবার চালিয়ে নিশ্চিত করা
হয়েছে — এখনো এই তিনটাই (AdditionalCharges/CancelledBids/Ratings) ফেরত আসে, কিন্তু এটা প্রত্যাশিত
false-positive (shared-value pulse ডিজাইনে `isFilterRefreshing` স্ট্রিং কখনোই যোগ হবে না, বিস্তারিত
ADMIN_PANEL_LOADING_PROGRESS.md-এর সেশন ২.৩৪ এন্ট্রিতে) — নতুন pending কাজ নেই এই রুলের আওতায়।
ভবিষ্যতে নতুন কোনো ট্যাব migrate/retrofit হলে (উদাহরণ: নিচের ধাপ ২-এর ২.১৮-২.২৬, যেগুলো এখনো Ground
Rule ১৮-ই পায়নি) সেগুলো Ground Rule ১৯ পাওয়ার পরেই এই ৩-শর্তের গ্রেপে candidate হতে পারে — তখন এই
রেট্রোফিট তালিকায় নতুন এন্ট্রি যোগ হবে, কিন্তু আপাতত এই সেকশনের কাজ শেষ।
```

---

## ধাপ ৩ — প্রতিটা সেশনের জন্য বাধ্যতামূলক চেকলিস্ট

`SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md`-এর ধাপ ৩-এর চেকলিস্ট (৭টা আইটেম — brace-balance,
grep-verify, systemic ১৪(ক)/(খ) চেক, ফাংশনালিটি অপরিবর্তিত, পূর্ণ zip + diff, progress-note,
real-build sandbox-এ সম্ভব না মনে করিয়ে দেওয়া) হুবহু প্রযোজ্য — এখানে পুনরায় লেখা হলো না। শুধু এই
ফাইলের জন্য একটা বাড়তি আইটেম:

```
4b. এই ট্যাবের ইনডেক্স নাম্বার AdminPanelScreen.kt-এর drawer group ম্যাপিং-এ (ধাপ ০.৫, মূল চ্যাটে
    দেওয়া গ্রুপ তালিকা) যে গ্রুপে আছে সেটা বদলায়নি তা নিশ্চিত করো — sessionKey/wrapper বদলানো ছাড়া
    navigation/drawer-এর কোনো আচরণ স্পর্শ করা যাবে না।
```

---

## ধাপ ৪ — সাজেস্টেড অর্ডার

১ (ডায়াগনসিস + স্কোপ কনফার্মেশন, বাধ্যতামূলক প্রথমে) → ২.১ (bug-1 verify, ১১টা ট্যাব, ঝুঁকিপূর্ণ
production data-এর জন্য আগে চেক করা ভালো) → ২.৩, ২.৪ (KYC, Manual Notification — action-ভারী, বাগ
আসার ঝুঁকি বেশি) → ২.২ (Overview/UserLookup upgrade-সিদ্ধান্ত) → ২.৫–২.১১ (বাকি সাধারণ
CRUD/list ট্যাব, প্রায়োরিটি স্বাধীন) → ২.১২, ২.১৪ (dev-tool ট্যাব, স্কোপ-প্রশ্নের উত্তর অনুযায়ী শেষে
বা বাদ) → ২.১৫-২.২৫ (ক্যাটেগরি ১-এর ১১টা retrofit — কম ঝুঁকিপূর্ণ presentation-only বদল, তাই
ক্যাটেগরি ৩-এর নতুন-migration কাজ শেষ হওয়ার পর করাই ভালো) → ২.২৬ (ReputationDetailScreen)।

প্রতিটা সেশন শেষে ব্যবহারকারীর কনফার্মেশন নিয়ে পরেরটায় যাওয়া হবে — একসাথে সব ট্যাব না ছুঁয়ে।

**আপডেট (Ground Rule ২০ যোগ হওয়ার পর, ২০২৬-০৯-১৫):** ২.২৯-২.৩১ (Users → KYC → Withdrawal,
Ground Rule ২০ রেট্রোফিট — filter-scope whole-visible pulse + scroll-jump ফিক্স) এখন সবচেয়ে
বেশি অগ্রাধিকার পাবে, বাকি সব pending সেশনের (২.২, ২.৩-২.১৪, ২.১৮-২.২৫, ২.২৬) আগে — কারণ এই তিনটা
ট্যাবই production-এ সবচেয়ে বেশি ব্যবহৃত আর ব্যবহারকারী real device-এ বাগটা নিজে দেখেছেন। এর পরে
আগের ক্রম (২.২ থেকে) যথারীতি চলবে।

**আপডেট ২ (সেশন ২.৩৪-এর পর, ২০২৬-০৯-১৫ — এই zip-এর অবস্থা):** Ground Rule ২০ candidate তালিকা
(Users, KYC, Withdrawal, AdditionalCharges, CancelledBids, Ratings — ৬টাই) সম্পূর্ণ। `AdminPanelScreen.kt`-এর
`when (selectedTabIndex)` ব্লক এই zip-এ সরাসরি গ্রেপ করে (অনুমান না করে) নিচের বর্তমান অবস্থা
কনফার্ম করা হয়েছে — পরবর্তী সেশন শুরুর আগে আবার নিজে গ্রেপ করে ভেরিফাই করা বাধ্যতামূলক, কোডবেস বদলে
থাকতে পারে:

- **সম্পূর্ণ/আপ-টু-ডেট:** ইনডেক্স ০(Overview), ১(KYC), ২(Withdrawal), ৪(Users), ৫(সমস্যাসমূহ,
  ✅ সেশন ২.১৮ — Ground Rule ১৮+১৯+২০ একসাথে), ৬(Categories), ৮(AdditionalCharges), ১০(Ratings),
  ১১(Settings), ১২(AuditLog), ১৩(FAQ), ১৪(CancelledBids), ১৮(UserLookup), ২০(ReputationEngine) —
  সব প্লেইন `SyncAwareContent`, Ground Rule ১৮ অনুযায়ী (০ ও ১৮-এ pull-to-refresh সেশন ২.২-এ
  ভেরিফাই করা হয়েছে, নিচে দেখো)।
- **এখনো `SyncAwareRefreshableContent` (Ground Rule ১৮ retrofit বাকি, ধাপ ২-এর সেশন ২.১৯-২.২৫
  টেমপ্লেট অনুযায়ী, কিন্তু ইনডেক্স-ম্যাপিং অনুযায়ী):**
  ৭(Escrow/AdminEscrowView), ৯(লেনদেন হিস্ট্রি/AdminTransactionsView), ১৫(চ্যাট মনিটরিং/
  AdminChatMonitoringView), ১৬(সরাসরি চুক্তি/AdminDirectContractsView), ১৯(সলভার কোটা/
  AdminSolverQuotaView), ২১(ইনস্ট্যান্ট জবস/AdminInstantJobsView), ২২(বিরোধ কেন্দ্র/
  AdminDisputeCenterView), ২৩(গেটওয়ে পেমেন্ট/AdminGatewayPaymentsView) — **৮টা ট্যাব বাকি** (মূল
  ১১টার মধ্যে Withdrawal, Users, Problems আগেই retrofit হয়ে গেছে)। এই ৮টাতে প্রতিটার আগে নতুন করে
  grep করে `PulsingValue`/`coerceIn(1,`/`scrollToItem` আছে কিনা যাচাই করা বাধ্যতামূলক (অনুমান না
  করে) — Problems (২.১৮)-এ দেখা গেছে safePage+scroll-effect আগে থেকে ছিল কিন্তু `PulsingValue`
  ছিল না, তাই একই ধরে নেওয়া যাবে না বাকি ৮টাতেও ঠিক একই অবস্থা থাকবে।
- **আপডেট ৩ (সেশন ২.২৩-এর পর):** ১৯(সলভার কোটা/AdminSolverQuotaView) উপরের তালিকা থেকে সরানো
  উচিত — ব্যবহারকারীর কনফার্মড সিদ্ধান্তে এই ট্যাবে GR18 **ইচ্ছাকৃতভাবে স্কিপ করা হয়েছে** (তাই
  `AdminPanelScreen.kt`-এ এখনো `SyncAwareRefreshableContent`, এটা প্রত্যাশিত এবং চূড়ান্ত, retrofit
  বাকি না), আর GR19 (সিলেক্টেড সলভারের ডিটেইল কার্ড pulse) প্রয়োগ হয়ে গেছে। বিস্তারিত
  `ADMIN_PANEL_LOADING_PROGRESS.md`-এর সেশন ২.২৩ এন্ট্রিতে।
- **ইচ্ছাকৃতভাবে বাদ (ADMIN_PANEL_LOADING_PROGRESS.md-এ নোট করা):** ইনডেক্স ১৭(Supabase এক্সপ্লোরার),
  ২৪(রিফান্ড ডায়াগনস্টিক) — কোনো wrapper নেই, dev-tool বলে স্কোপের বাইরে রাখা হয়েছে।
- **✅ সেশন ২.২ (সম্পন্ন) — ইনডেক্স ০(Overview/AdminStatsView), ১৮(UserLookup):** ব্যবহারকারী
  pull-to-refresh যোগ করতে বলেছিলেন, কিন্তু এই zip-এ ফ্রেশ গ্রেপ করে দেখা গেছে **এটা ইতিমধ্যেই আছে**
  (আগের কোনো সেশনের ডায়াগনসিস ভুল ছিল — শুধু `SyncAwareContent`-এর outer wrapper-এ
  `isManualRefreshing` param আছে কিনা দেখা হয়েছিল, ভেতরের কম্পোজেবলের বডি গ্রেপ করা হয়নি):
  (ক) `AdminPanelScreen.kt`-এর গোটা `when (selectedTabIndex) {...}` ব্লক আগে থেকেই একটা একক গ্লোবাল
  `SomadhanPullToRefresh` wrapper দিয়ে মোড়ানো (লাইন ~৯৫০, ধাপ ৬-এর মন্তব্য অনুযায়ী ১৩টা ট্যাবের জন্য
  ইচ্ছাকৃতভাবে করা হয়েছিল) — তাই pull-gesture ইনডেক্স ০/১৮ সহ **সব ট্যাবেই** সমানভাবে কাজ করে;
  (খ) `AdminStatsView`-এ `isRefreshing: Boolean` প্যারামিটার আগে থেকেই পাস করা হয় আর ভেতরে ৯টা
  independent shimmer-zone (`heroCardPulsing`, `row1Pulsing`, ... `topSolversPulsing`) প্রতিটাই
  `rememberFieldChangePulse(..., isManualRefreshing = isRefreshing)` ব্যবহার করে — pull-to-refresh
  সম্পন্ন হলে প্রতিটা জোন pulse করে; (গ) `AdminUserLookupView`-এ `isRefreshing`
  প্যারামিটার হিসেবে পাস করা হয় না, বরং ফাংশনের ভেতরেই সরাসরি
  `viewModel.isRefreshing.collectAsStateWithLifecycle()` কল করে (একই গ্লোবাল স্টেট) — সারসংক্ষেপ/
  আর্থিক/রো-লেভেল pulse-গুলো (`summaryPulse`, `financialPulse`, `rowPulse`) সবই এটা ব্যবহার করে।
  **`AdminStatsView`-এর ১২টা independent shimmer-zone পাইলট গ্রেপ করে skeleton-shape verify করা**
  হয়েছে (ধাপ ০.১৫ অনুযায়ী) — প্রতিটা zone-এর ভেতরের real-content layout-এর সাথে তার নিজস্ব pulse
  wrap মিলেছে, কোনো mismatch পাওয়া যায়নি। **সিদ্ধান্ত:** কোনো কোড এডিট করা হয়নি (redundant/
  harmful হতো — `SyncAwareRefreshableContent`-এ upgrade করলে Ground Rule ১৮-এর "whole-content flash
  করা যাবে না" নিষেধ ভঙ্গ হতো) — শুধু এই ডকুমেন্টেশন-গ্যাপ বন্ধ করা হলো, নিচের তালিকায় সরানো হলো।
- **`ReputationDetailScreen.kt` (সেশন ২.২৬, tab-index সিস্টেমের বাইরে, `AdminUsersView`-এর
  `onNavigateToReputation`-এ navigate হয়):** এই zip-এ গ্রেপ করে নিশ্চিত করা হয়েছে এখনো
  `SyncAwareRefreshableContent` (whole-content flash) — Ground Rule ১৮ retrofit দরকার কিনা এখনো
  ব্যবহারকারীকে জিজ্ঞাসা করা হয়নি।

**আপডেট (সেশন ২.২৭-এর পর, ২০২৬-০৯-১৬ — চূড়ান্ত আপডেট):** `ReputationDetailScreen` GR18+19 সহ
সম্পূর্ণ (GR20 প্রযোজ্য না, GR21 ইচ্ছাকৃতভাবে বাদ; বিস্তারিত `ADMIN_PANEL_LOADING_PROGRESS.md`-এর
সেশন ২.২৭ এন্ট্রি)। **admin panel-এর GR18-২১ রোডম্যাপ এখন কোড-লেভেলে সম্পূর্ণ** — বাকি শুধু
real-device verification (১২টা ট্যাব/স্ক্রিন pending, তালিকা progress ফাইলের সেশন ২.২৭ এন্ট্রির
শেষে)।

**পরবর্তী Claude সেশনের জন্য সাজেস্টেড অর্ডার (আপডেটেড, সেশন ২.১৯.৩/Users-এর GR২১ implementation-এর
পর, ২০২৬-০৯-১৬):**
**Ground Rule ২১** ডিজাইন হয়ে গেছে আর **Users**-এ implement করা হয়েছে (সেশন ২.১৯.৩,
`ADMIN_PANEL_LOADING_PROGRESS.md`-এ পূর্ণ বিবরণ — সংক্ষেপে: `SupabaseRealtimeManager`-এর
`PostgresAction.Insert` হ্যান্ডলারে ২০০০ms TTL-এর একটা `recentlyInsertedUserIds` সেট, UI-তে
per-item `PulsingValue`-তে OR করা)। **কিন্তু এখনো real device-এ verify করা হয়নি** (sandbox-এ
build/run সম্ভব না) — পরের সেশন **সবার আগে** এই ভেরিফিকেশন করবে (progress ফাইলের সেশন ২.১৯.৩-এর
"পরবর্তী সেশনের জন্য যা এখনো বাকি" অংশের ৩টা চেক)। ভেরিফাই করে ঠিক থাকলে (বা ছোটখাটো টিউনিং লাগলে
সেটা করে) তারপরই একই কনফার্মড প্যাটার্ন (নতুন ডিজাইন-আলোচনা ছাড়াই, `markRecentlyInserted()` reuse
করে) **KYC → Withdrawal → Problems → Escrow**-এ প্রয়োগ করো — একটা একটা করে, প্রতিটার পর
ব্যবহারকারীর কনফার্মেশন নিয়ে। **এই ৫টা (Users সহ) সম্পন্ন/কনফার্ম না হওয়া পর্যন্ত নিচের কোনো সেশনে
যাওয়া যাবে না।** এরপর: সেশন ২.২০-২.২৫ (বাকি ৬টা GR১৮ retrofit ট্যাব —
১৫ চ্যাট মনিটরিং, ১৬ সরাসরি চুক্তি, ১৯ সলভার কোটা, ২১ ইনস্ট্যান্ট জবস, ২২ বিরোধ কেন্দ্র, ২৩ গেটওয়ে
পেমেন্ট — ধাপ ২-এর টেমপ্লেট অনুযায়ী একটা একটা করে, **Ground Rule ১৮, ১৯, ২০ (candidate হলে), আর
২১ (ডিজাইন কনফার্মড থাকলে) — এই চারটাই একই সেশনে একসাথে বসাতে হবে, কোনোটাকে ভবিষ্যতের জন্য ভাগ করে
রাখা যাবে না — Problems/২.১৮ ও Escrow/২.১৯-এর রেফারেন্স প্যাটার্ন অনুসরণ করা যায়, GR২১ ওভারল্যাপ
নোট উপরে ধাপ ২-এর সেশন ২.১৫-২.২৫ অংশে দেখো**) → সেশন ২.২৬ (ReputationDetailScreen, GR১৮ retrofit
দরকার হলে GR১৯/২০/২১-ও একই সেশনে প্রযোজ্যতা যাচাই করে বসাও)। এরপর ধাপ ২-এর বাকি যেকোনো un-migrated
ট্যাব (২.৩-২.১৪, যেগুলোতে per-item pulse এখনো নেই)-এ per-item pulse প্রথমবার বসানো হলে সেখানেও
একই নিয়মে GR২১ সাথে সাথেই বসবে (উপরের Ground Rule ২১-এর "স্কোপ স্পষ্টীকরণ" নোট দেখো)। প্রতিটা ধাপের
আগে Ground Rule ১৬ অনুযায়ী ব্যবহারকারীর কাছ থেকে scope confirm করে নিতে হবে, অনুমান না করে।
