# সমাধান অ্যাপ — Loading/Refresh Pattern: সম্পূর্ণ মাস্টার প্রম্পট (একক সোর্স অফ ট্রুথ)

> **এই ফাইলটা কেন বানানো হয়েছে:** আগে এই প্রজেক্টে দুইটা আলাদা roadmap ডকুমেন্ট ছিল
> (`LOADING_SYNC_FIX_ROADMAP_PROGRESS.md` যেটা একটা বাইরের ফাইল `somadhan-loading-fix-roadmap-v2.md`
> রেফারেন্স করতো যেটা আসলে zip-এ ছিলই না, আর `REALTIME_STRUCTURE_PRESERVING_REFRESH_MASTER_PROMPT.md`)।
> দুটো মিলিয়ে ৪টা rule-এর মধ্যে মাত্র ২টার (rule ২, ৪) পুরো "কীভাবে বানাতে হবে" লেখা ছিল, বাকি ২টার
> (rule ১, ৩) না। **এই একটা ফাইলেই এখন ৪টা rule-এর পূর্ণ ডিজাইন + কোড-প্যাটার্ন + এই মুহূর্তের
> সঠিক স্ট্যাটাস (কোন স্ক্রিনে কী আছে, কী বাকি) সব লেখা আছে।** নতুন কোনো session/AI agent-কে শুধু
> এই ফাইল + প্রজেক্ট zip দিলেই সে বুঝে যাবে ঠিক কী করতে হবে।
>
> **ব্যবহারকারীর নিজের ভাষায় ৪টা rule:**
> ১. পেজ প্রথমবার open করলে ফুল পেজ স্কেলিটন লোডিং।
> ২. রি-এন্ট্রি করলে same page-এর পুরো কাঠামো ঠিক রেখে শুধু list বা value-এর অংশে স্কেলিটন লোডিং।
> ৩. Pull to refresh করলে পেজের সব কাঠামো ঠিক রেখে শুধু list বা value অংশে স্কেলিটন হয়।
> ৪. যে সব পেজে pagination আছে — সেগুলোতে আগের সব ঠিক রেখে শুধু pagination-এর (next/prev/scroll to
>    load/see more) নতুন অংশে স্কেলিটন লোডিং হয়ে ডেটা দেখায়।
>
> **ব্যতিক্রম:** `MessagesScreen` — এই একটা স্ক্রিনে rule ২/৩ প্রযোজ্য না (নিচে বিস্তারিত)।
>
> **⚠️ (ব্যাচ ১৯ নোট, ২০২৬-০৯-১৪):** এই ফাইলের আগের ভার্সনে "বর্তমান স্ট্যাটাস" সেকশন ব্যাচ ১০
> পর্যন্ত লেখা ছিল, কিন্তু ব্যবহারকারীর দেওয়া zip (`somadhan-batch18-adminpanel-complete.zip`)
> ব্যাচ ১৮ পর্যন্ত এগিয়ে ছিল — মাঝের ব্যাচ ১১-১৮-এর কোনো সেশন-লগ এই ফাইলে নেই (সম্ভবত অন্য
> কোনো সেশনে migrate হয়েছে কিন্তু এই single-source-of-truth ফাইলটা আপডেট করা হয়নি, Ground Rule
> ১২-এর ব্যতিক্রম)। ব্যাচ ১৯-এর শুরুতে `grep` দিয়ে সরাসরি কোড verify করে দেখা গেছে
> `AdminPanelScreen` ইতিমধ্যেই পূর্ণাঙ্গ migrate হয়ে গেছে (নিচে ক্যাটেগরি A′-এ সরানো হলো)। বাকি
> কোনো স্ক্রিনে (B2/C) মধ্যবর্তী ব্যাচগুলোতে কোনো পরিবর্তন পাওয়া যায়নি (grep-এ verify করা)।
>
> **⚠️ (ব্যাচ ২৭ নোট, ২০২৬-০৯-১৪) — পরবর্তী সেশনের জন্য সরাসরি next-step:** `JobTrackingScreen`
> (৫৯২৫ লাইন, আগে ৫৮২৯ ছিল, এই ব্যাচের এডিটে লাইন-সংখ্যা বেড়েছে)-এর **৬টা zone-ই এখন সম্পূর্ণ** —
> zone ৬ (action CTA বাটন, ব্যাচ ২৬-এ কনফার্মড ডিজাইন) এই ব্যাচে বাস্তবায়ন হয়ে গেছে (বিস্তারিত নিচে
> ধাপ ৭, ব্যাচ ২৭ এন্ট্রিতে)। **`JobTrackingScreen`-এর সব কাজ সম্পূর্ণ।** এখন পুরো প্রজেক্টের বাকি
> কাজ শুধু **`HomeScreen`** (২৮১০ লাইন) — rule ১+২ (cold-load, re-entry flash-বিহীন) আগে থেকেই ঠিক
> আছে, বাকি rule ৩ (pull-to-refresh-এ নিচের problem-list-এ শিমার) আর rule ৪ (pagination শিমার),
> target design ব্যাচ ১০-এ আগে থেকেই কনফার্ম করা আছে (উপরে/নিচে খুঁজে দেখো) — migrate এখনো শুরু
> হয়নি। এরপর ⚠️ **real Gradle/Android Studio build এখনো একবারও verify হয়নি** (Ground Rule ২),
> সেটা সবচেয়ে জরুরি next-step হিসেবে ব্যবহারকারীকে মনে করিয়ে দেওয়া বাধ্যতামূলক।
>
> **⚠️ (ব্যাচ ২৮ নোট, ২০২৬-০৯-১৪) — ঐচ্ছিক পলিশ-আপগ্রেড, next-step:** ব্যবহারকারী `JobTrackingScreen`
> zone ৬ (action CTA বাটন)-এর বর্তমান `PulsingValue` (opacity-pulse-only, ব্যাচ ২৭-এ বাস্তবায়িত)
> আচরণ দেখে সিদ্ধান্ত নিয়েছেন এটাকে **সত্যিকারের crossfade**-এ upgrade করতে চান (`AnimatedContent`
> দিয়ে) — কারণ বর্তমান প্যাটার্নে state বদলালে পুরনো বাটন-সেট থেকে নতুনটায় প্রায় সাথে সাথেই swap হয়ে
> যায় (opacity pulse-টা শুধু একটা visual cue, কোনো true old→new crossfade না), যেখানে বাকি ৫টা
> zone-এ (শুধু leaf ভ্যালু বদলায় বলে) এটা সমস্যা না, zone ৬-এ পুরো structure বদলায় বলে এটা একটু
> চোখে পড়ার মতো। **এই ব্যাচে কোনো কোড পরিবর্তন হয়নি**, শুধু নিচে "খোলা প্রশ্ন" সেকশনে
> `JobTrackingScreen` bullet-এর ভেতরে "zone ৬-এর crossfade আপগ্রেড ডিজাইন (ব্যাচ ২৮)" অংশে টেকনিক্যাল
> ডিজাইন কনফার্ম করে লেখা হলো, যাতে পরের সেশন এই ফাইল + zip দিয়ে সরাসরি বাস্তবায়ন শুরু করতে পারে —
> migrate শুরুর আগে শুধু ফাইলে গিয়ে exact current line-number/brace-boundary যাচাই করে নিতে হবে
> (Ground Rule ১৩, কারণ ব্যাচ ২৭-এর এডিটে লাইন-নাম্বার শিফট হয়ে থাকতে পারে)। **এটা সম্পূর্ণ ঐচ্ছিক
> visual polish** — `JobTrackingScreen`-এর কার্যকরী কাজ (Ground Rule ১-এর অর্থে) ব্যাচ ২৭-এই
> সম্পূর্ণ হয়ে গেছে, তাই এই আপগ্রেড না করলেও অ্যাপ পুরোপুরি কাজ করবে।
>
> **✅ (ব্যাচ ২৯ নোট, ২০২৬-০৯-১৪) — zone ৬-এর crossfade আপগ্রেড বাস্তবায়িত:** ব্যাচ ২৮-এ কনফার্ম-করা
> ডিজাইন এই ব্যাচে কোডে বাস্তবায়ন করা হলো (বিস্তারিত নিচে ধাপ ৭, ব্যাচ ২৯ এন্ট্রিতে)। zone ৬-এ এখন
> `PulsingValue`-এর বদলে `AnimatedContent` (true old→new crossfade) ব্যবহার হচ্ছে। **`JobTrackingScreen`-এর
> সব কাজ (কার্যকরী + ঐচ্ছিক পলিশ, ৬টা zone-ই) এখন সম্পূর্ণ।** এখনো বাকি: শুধু `HomeScreen` (rule
> ৩+৪) এবং ⚠️ **real Gradle/Android Studio build এখনো একবারও verify হয়নি** (Ground Rule ২) — এটাই
> সবচেয়ে জরুরি next-step হিসেবে ব্যবহারকারীকে মনে করিয়ে দেওয়া বাধ্যতামূলক।
>
> **✅ (ব্যাচ ৩০ নোট, ২০২৬-০৯-১৪) — `HomeScreen` rule ৩+৪ বাস্তবায়িত, প্রজেক্টের সব স্ক্রিন এখন সম্পূর্ণ:**
> শুরুর আগে (Ground Rule ১৩) ফাইলে গিয়ে সরাসরি `grep`-এ current লাইন-নম্বর/গঠন যাচাই করা হলো —
> `UserHomeContent`/`SolverHomeContent` দুটোতেই `completedCurrentPage`/`pagedCompletedProblems`/
> `totalCompletedPages` pagination-state এবং `items(items = pagedCompletedProblems, ...)` +
> next/prev `OutlinedButton` pagination-row হুবহু ব্যাচ ১০-এর নোটে বর্ণিত কাঠামোতেই পাওয়া গেছে, কোনো
> শিফট হয়নি। ব্যাচ ১০-এ কনফার্ম-করা ডিজাইন অনুযায়ী বাস্তবায়ন: `HomeScreen`-এর rule ২ ("কিছুই শিমার
> হবে না — আর কখনো flash না, `MessagesScreen`-এর মতো") অক্ষত রাখতে outer `SyncAwareContent` (data-বিহীন
> ভার্সন) **কোনোভাবেই ছোঁয়া হয়নি** — তার বদলে `UserHomeContent`/`SolverHomeContent` দুটোতেই আলাদাভাবে
> `isCompletedListRefreshing = rememberFieldChangePulse(value = safeCompletedPage, isManualRefreshing =
> isRefreshing)` যোগ হলো। ইচ্ছাকৃতভাবে `value`-তে পুরো তালিকা/আইটেম না দিয়ে শুধু `safeCompletedPage`
> (পেজ-ইনডেক্স) দেওয়া হয়েছে — এতে realtime-এ কোনো completed-problem-এর field বদলে গেলেও flash হয় না
> (rule ২ রক্ষিত থাকে), কিন্তু next/prev পেজ-ক্লিকে page index বদলালেই flash হয় (rule ৪)।
> `isManualRefreshing = isRefreshing` দিয়ে pull-to-refresh সাইকেল শেষ হলে flash (rule ৩)।
> `sessionKey`/`viewModel` ইচ্ছাকৃতভাবে বাদ দেওয়া হয়েছে যাতে `rememberFieldChangePulse`-এর ভেতরের
> re-entry-flash মেকানিজম ("পথ B") এখানে সম্পূর্ণ নিষ্ক্রিয় থাকে। `isRefreshing` দুটো সাব-কম্পোজেবলেই
> আলাদাভাবে `viewModel.isRefreshing.collectAsStateWithLifecycle()` দিয়ে collect করা হলো (একই shared
> StateFlow, `HomeScreen()`-এর `SomadhanPullToRefresh`-এ ব্যবহৃত মানের সাথেই সমান — কোনো নতুন state
> তৈরি হয়নি)। `LazyListScope`-এর ভেতরে `items(completedPageSize) { ProblemCardSkeleton() }` — page-এ
> যত কার্ড আশা করা হয় (২টা) ততগুলোই skeleton দেখায়, তারপর আগের empty-state/real-list ব্রাঞ্চ দুটো
> অক্ষত রাখা হয়েছে (শুধু `isCompletedListRefreshing` হলে প্রথমে branch, Ground Rule ১)। Pagination
> Controls (Previous/Next/পৃষ্ঠা-ইনডিকেটর) rule ৩+৪-এর "কাঠামো ঠিক থাকবে" শর্ত মেনে if/else ব্লকের
> *বাইরে* সরানো হলো — শিমার-অবস্থাতেও সবসময় স্থির/দৃশ্যমান থাকে (আগে এটা শুধু `else` (non-empty)
> ব্রাঞ্চের ভেতরে ছিল, কিন্তু `totalCompletedPages > 1` সত্যি হওয়া মানেই তালিকা non-empty, তাই এই
> পুনর্বিন্যাসে আচরণ অপরিবর্তিত)। বাটনগুলোর enabled/disabled লজিক অপরিবর্তিত (Ground Rule ১)। দুটো
> নতুন import (`ProblemCardSkeleton`, `rememberFieldChangePulse`, দুটোই আগে থেকে বিদ্যমান
> `MotionToolkit.kt`-এর ফাংশন, নতুন কিছু বানানো হয়নি) যোগ হয়েছে। এডিটের পর brace/paren/bracket
> balance একটা Kotlin-aware (string/string-template/comment-aware) Python স্ক্রিপ্টে zero-imbalance
> ভেরিফাই হয়েছে (`{`:৪৪৪/৪৪৪, `(`:১৩৭৮/১৩৭৮, `[`:২/২)। ফাইল-সংখ্যা (২৪৫/২৪৫, dotfile-সহ) input zip
> (`somadhan-batch29-jobtracking-zone6-crossfade-implemented.zip`)-এর সাথে ফাইল-লিস্ট diff করে হুবহু
> মিলিয়ে দেখা হয়েছে, কোনো ফাইল যোগ/বাদ/rename হয়নি। **এর ফলে `HomeScreen`-এর rule ১+২+৩+৪ সবগুলোই
> এখন সম্পূর্ণ — গোটা প্রজেক্টের (ধাপ ৫-এ তালিকাভুক্ত) সব স্ক্রিনের migrate কাজ এখন সম্পূর্ণ, আর কোনো
> স্ক্রিন বাকি নেই।** ⚠️ real Gradle/Android Studio build এই কাজের কোনো ধাপেই এখনো একবারও verify
> হয়নি (Ground Rule ২) — এটাই এখন **একমাত্র এবং সবচেয়ে জরুরি** next-step, ব্যবহারকারীকে স্পষ্টভাবে
> মনে করিয়ে দেওয়া হচ্ছে যে বাস্তব Android Studio/Gradle build/run ছাড়া এই পুরো কাজ "সম্পূর্ণ" দাবি
> করা যায় না।

---

## ধাপ ০ — Non-negotiable Ground Rules (প্রতিটা কাজেই বাধ্যতামূলক)

1. **অ্যাপের কোনো বিদ্যমান ফাংশনালিটি/বিজনেস লজিক নষ্ট করা যাবে না।** bid/escrow/payment/notification
   ইত্যাদি লজিক, navigation, permission/RLS আচরণ — কিছুই বদলাবে না। শুধু *কীভাবে skeleton/content
   দেখানো হয়* সেটাই বদলাবে, *কী দেখানো হয়* তা না।
2. **প্রতিটা ধাপ শেষে প্রজেক্ট সম্পূর্ণ/কম্পাইলযোগ্য থাকতে হবে।** এই sandbox-এ Gradle/Android SDK
   নেই বলে আসল build সম্ভব না — তাই প্রতিটা এডিট করা ফাইলে ম্যানুয়ালি bracket/paren-balance, import,
   parameter-signature মিলিয়ে যাচাই করতে হবে, আর প্রতিবার ব্যবহারকারীকে স্পষ্ট মনে করিয়ে দিতে হবে
   যে **আসল Android Studio build/run এখনো বাকি** — এটা কখনো "সব ঠিক আছে" বলে claim করা যাবে না যতক্ষণ
   না real build verify হয়।
3. **Zip ডেলিভারির নিয়ম:** পুরো প্রজেক্টের zip (শুধু বদলানো ফাইল না), কোনো ফাইল miss না (dotfile সহ),
   প্রতিবার আগের zip-এর ফাইল-লিস্টের সাথে diff করে ফাইল-সংখ্যা/নাম মিলিয়ে যাচাই, ফোল্ডার-স্ট্রাকচার
   flat রাখা।
4. **ছোট, independently-testable ধাপে কাজ ভাগ করা** — একসাথে অনেকগুলো ফাইল/স্ক্রিন বদলানো যাবে না।
5. **আগের ফিক্স (session-aware skeleton gate ইত্যাদি) revert/override করা যাবে না** — নতুন কাজ তার
   *উপরে* যোগ হবে।
6. **"খালি" আর "এখনো লোড হয়নি" গুলিয়ে ফেলা যাবে না** — সত্যিকারের খালি লিস্ট একটা বৈধ LOADED অবস্থা।
7. **বিদ্যমান কোড-স্টাইল মেনে চলা** — বাংলা-ইংরেজি মিশ্র মন্তব্য, "কেন এই সিদ্ধান্ত" ব্যাখ্যা, ধাপ-রেফারেন্স
   কমেন্ট।
8. **অপ্রাসঙ্গিক ফাইলে হাত না দেওয়া** — `supabase/migrations/*`, `.env*`, `gradle*`, build config।
9. **প্রতিটা ধাপের শুরুতে কী বদলাচ্ছে/কেন, শেষে কী verify করা হয়েছে বলা।**
10. **সন্দেহ থাকলে অনুমান না করে ব্যবহারকারীকে জিজ্ঞেস করা।**
11. **Pagination-যুক্ত স্ক্রিনেও ব্যতিক্রম ছাড়া একই standard প্যাটার্ন** (নিচে rule ৪ দেখুন) —
    `isLoadingMore`-জাতীয় কোনো suppression লজিক ছাড়াই, "load more"-এ শিমার হওয়াটাই কাঙ্ক্ষিত।
12. **(নতুন)** প্রতিটা সেশনের শেষে এই ফাইলের "বর্তমান স্ট্যাটাস" সেকশন (নিচে) আপডেট করতে হবে — কোন
    স্ক্রিন migrate হলো, কোনটা বাকি — যাতে এই ফাইলটাই সবসময় সত্যিকারের একক সোর্স অফ ট্রুথ থাকে,
    আলাদা কোনো progress-note ফাইলে ছড়িয়ে না যায়।
13. **(নতুন)** কোনো দাবি ("সব হয়ে গেছে", "২৭টা স্ক্রিনে আছে") করার আগে **`grep` দিয়ে সরাসরি কোডে
    verify করতে হবে** — শুধু আগের progress-note পড়ে বিশ্বাস করে বলা যাবে না। নিচের "কীভাবে verify
    করবে" সেকশনে exact কমান্ড দেওয়া আছে।

---

## ধাপ ১ — চারটা বিল্ডিং ব্লক (কোর কম্পোনেন্ট, সব `MotionToolkit.kt`/`CommonComponents.kt`-এ আছে)

এই পুরো সিস্টেম ৪টা কম্পোনেন্টের সমন্বয়ে কাজ করে। নতুন কোনো কম্পোনেন্ট বানানোর দরকার নেই — যা
আছে সেটাই সঠিকভাবে প্রতিটা স্ক্রিনে বসাতে হবে।

### ১.১ `rememberSessionAwareSkeletonGate` — rule ১-এর মূল ভিত্তি

```kotlin
@Composable
fun rememberSessionAwareSkeletonGate(
    sessionKey: String,
    viewModel: SomadhanViewModel,
    minimumDurationMs: Long = 220L
): Boolean {
    if (viewModel.hasLoadedOnce(sessionKey)) return false
    return rememberMinimumSkeletonGate(minimumDurationMs) // ~220ms ন্যূনতম skeleton window
}
```

**কেন দরকার:** অ্যাপের `syncPhase` (bulk-pull sync signal) পুরো অ্যাপ-জুড়ে একটাই শেয়ার্ড সিগন্যাল —
একবার `LOADED` হলে সারা session-এ আর কখনো `LOADING`-এ ফেরে না। শুধু এটার উপর ভিত্তি করলে: bulk-pull
আগেই (বা cache থেকে) `LOADED` হয়ে গেলে, পরে *যেকোনো* স্ক্রিনে প্রথমবার ঢুকলেও কোনো skeleton flash
ছাড়াই সরাসরি content দেখানো হতো — প্রতিটা স্ক্রিনের নিজস্ব "প্রথম-ভিজিট cold-load" অনুভূতিটাই হারিয়ে
যেত। এই গেট প্রতিটা `sessionKey`-এর জন্য আলাদাভাবে **প্রথমবার** ন্যূনতম ~220ms skeleton window
guarantee করে; `viewModel.markLoadedOnce(sessionKey)` কল হওয়ার পর সেই key-তে আর কখনো (re-entry-তে)
এই গেট skeleton দেখাবে না।

### ১.২ `SyncAwareContent` — rule ১ (+ single/non-diffable rule ২) নন-লিস্ট বা diff-দরকার-নেই স্ক্রিনের জন্য

```kotlin
@Composable
fun SyncAwareContent(
    sessionKey: String,
    viewModel: SomadhanViewModel,
    syncPhase: SupabaseRealtimeManager.SyncPhase,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit = { ListScreenSkeleton() },
    content: @Composable () -> Unit
)
```

- `LOADING` → skeleton, `LOADED` → content, `ERROR` → এরর মেসেজ + "আবার চেষ্টা করুন" বাটন (ট্যাপে
  `onRetry`)।
- ভেতরে `rememberSessionAwareSkeletonGate` ব্যবহার করে rule ১ (প্রথম-ভিজিট ফুল স্কেলিটন) নিশ্চিত
  করে।
- `ON_RESUME` lifecycle event-এ (`syncPhase == ERROR` থাকলে) স্বয়ংক্রিয় retry, ৫ সেকেন্ড cooldown
  সহ (দ্রুত বারবার ট্যাব সুইচে retry-storm এড়াতে)।
- **এই কম্পোনেন্ট কোনো `data` প্যারামিটার নেয় না** — তাই realtime-এ ডেটা বদলালে বা re-entry করলে
  কোনো diff-aware শিমার এখানে হয় না (rule ২ পুরোপুরি প্রযোজ্য না)। যেসব স্ক্রিনে rule ২ (diff শিমার)
  পুরোপুরি দরকার, সেখানে **অবশ্যই** `SyncAwareRefreshableContent` ব্যবহার করতে হবে (নিচে ১.৩)।
- **কল-সাইট প্যাটার্ন (সবসময় এভাবেই):**
  ```kotlin
  SyncAwareContent(
      sessionKey = "user_wallet_sync",           // প্রতিটা স্ক্রিনের নিজস্ব ইউনিক key
      viewModel = viewModel,
      syncPhase = initialSyncPhase,              // বা combineSyncPhases()/worstSyncPhase()
      onRetry = { viewModel.retryInitialSync() }
  ) {
      // header/filter/tab এই কম্পোনেন্টের *বাইরে* রাখতে হবে (Scaffold-এর content lambda-তে,
      // SyncAwareContent-এর বাইরে) — ভেতরে শুধু আসল body/list/value অংশ।
      ...
  }
  ```

### ১.৩ `SyncAwareRefreshableContent<T>` — rule ২ + rule ৪ পূর্ণাঙ্গভাবে (ডেটা-ডিফ শিমার)

```kotlin
@Composable
fun <T> SyncAwareRefreshableContent(
    sessionKey: String,
    viewModel: SomadhanViewModel,
    syncPhase: SupabaseRealtimeManager.SyncPhase,
    data: T,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    refreshFlashMs: Long = 350L,
    isManualRefreshing: Boolean = false,   // rule ৩-এর সাথে যুক্ত করতে (নিচে ১.৪ দেখুন)
    flashOnReentry: Boolean = true,        // rule ২ (re-entry)
    skeleton: @Composable () -> Unit = { ListScreenSkeleton() },
    content: @Composable (T) -> Unit
)
```

**এটাই rule ২ এবং rule ৪-এর আসল ইঞ্জিন।** কীভাবে কাজ করে:

- **প্রথম ভিজিট (rule ১):** `SyncAwareContent`-এর মতোই — একই `rememberSessionAwareSkeletonGate`,
  ফুল স্কেলিটন, কোনো flash ছাড়াই প্রথম data রেকর্ড হয়ে যায়।
- **Re-entry (rule ২):** mount হওয়ার মুহূর্তে যদি `viewModel.hasLoadedOnce(sessionKey) == true`
  হয় (আগে একবার লোড হয়ে গেছে, মানে এই visit প্রথম না) — তাহলে data বদলেছে কিনা না দেখেই একবার
  সংক্ষিপ্ত (~350ms) flash হয়, তারপর content।
- **Realtime data-change (rule ২):** প্রতিবার নতুন `data` এলে আগের রাখা মানের সাথে Kotlin `==`
  দিয়ে তুলনা — অমিল হলে সংক্ষিপ্ত flash দেখিয়ে নতুন data দেখানো হয়; মিলে গেলে (সত্যিই অপরিবর্তিত)
  **কিচ্ছু হয় না, flash হবেই না।**
- **Pull-to-refresh শেষ হলে (rule ৩, `isManualRefreshing` দিয়ে wire করলে):** ডেটা বদলাক বা না
  বদলাক, রিফ্রেশ সাইকেল শেষ হলে একবার flash — ব্যবহারকারীকে "রিফ্রেশ হয়েছে" এই ফিডব্যাক দেওয়ার জন্য।
- **Pagination/"load more" (rule ৪):** `data`-তে paginated list পাস করলে, নতুন পেজ যোগ হয়ে
  list-এর কনটেন্ট বদলালেই এটা "ভিন্ন" ধরে সংক্ষিপ্ত শিমার দেখায় — ঠিক rule ৪ যা চেয়েছে। **কোনো
  আলাদা `isLoadingMore` সাপ্রেশন লজিক নেই/লাগবে না** (Ground Rule ১১)।
- সবগুলো কারণ (data-change/manual-refresh/re-entry) একটা কাউন্টার-ভিত্তিক (`flashRequestCount`)
  মেকানিজমে combine — একসাথে ট্রিগার হলেও একটা শেষ হয়ে গেলেও অন্যটা চললে flash অকালে বন্ধ হয় না।
- বাইরের `Crossfade` (পুরো cold-load ফেজ) আর ভেতরের `Crossfade` (শুধু flash window) আলাদা —
  caller-এর header/filter/tab (এই কম্পোনেন্টের বাইরে) কখনো re-animate হয় না।

**⚠️ pagination/mutable-list স্ক্রিনে বসানোর সময় অবশ্যই মনে রাখতে হবে:**
`mutableStateListOf<T>()`-ভিত্তিক ডেটা (ViewModel-এ `*Paged` নামের ফিল্ড) সরাসরি `data` প্যারামিটারে
পাস করলে (একই mutable reference থাকায়) `!=` diff কাজ নাও করতে পারে — তাই **`.toList()` দিয়ে
immutable snapshot বানিয়ে পাস করতে হবে।** ব্যতিক্রম: ডেটা ইতিমধ্যেই `remember{...}`/`.filter{}`-এর
মতো plain immutable list হলে `.toList()` লাগবে না।

**কল-সাইট প্যাটার্ন (pagination screen-এর উদাহরণ):**
```kotlin
val displayedItems = viewModel.someItemsPaged.toList()   // mutable snapshot immutable বানানো

SyncAwareRefreshableContent(
    sessionKey = "some_screen_sync",
    viewModel = viewModel,
    syncPhase = initialSyncPhase,
    data = displayedItems,
    isManualRefreshing = isRefreshing,          // SomadhanPullToRefresh-এর isRefreshing state
    onRetry = { viewModel.retryInitialSync() }
) { items ->
    // header/filter/tab বাইরে, এখানে শুধু LazyColumn/list body
    LazyColumn { items(items, key = { it.id }) { ... } }
}
```
> সতর্কতা: content lambda-র প্যারামিটারের নাম `items` রাখা যাবে না যদি ফাইলে
> `androidx.compose.foundation.lazy.items(...)` ফাংশনও ব্যবহার হয় — নাম-শ্যাডোয়িং কম্পাইল এরর
> দেবে (`FavoriteSolversScreen.kt`-এ তাই প্যারামিটারের নাম `favs` রাখা হয়েছিল)।

### ১.৪ `SomadhanPullToRefresh` — rule ৩-এর গায়ের মেকানিজম (native pull gesture)

```kotlin
@Composable
fun SomadhanPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
)
```

Material3-এর `PullToRefreshBox` + `Indicator`-এর একটা থিমড wrapper। **এটা শুধু pull-gesture আর
স্পিনার দেখায় — এর নিজের কোনো diff/flash লজিক নেই।** rule ৩ (structure ঠিক রেখে শুধু
list/value অংশে শিমার) আসলে **এটা একা করে না** — এটা কাজ করে `SyncAwareContent`/
`SyncAwareRefreshableContent`-এর `isManualRefreshing` প্যারামিটারের সাথে মিলে। তাই **সবসময় এই
combo-তে বসাতে হবে:**

```kotlin
Scaffold(topBar = {...}, bottomBar = {...}) { padding ->
    SomadhanPullToRefresh(
        isRefreshing = isRefreshing,             // viewModel থেকে collect করা StateFlow<Boolean>
        onRefresh = { viewModel.refreshData() },
        modifier = Modifier.fillMaxSize().padding(padding)
    ) {
        SyncAwareRefreshableContent(              // বা SyncAwareContent, স্ক্রিন-টাইপ অনুযায়ী
            sessionKey = "...",
            viewModel = viewModel,
            syncPhase = initialSyncPhase,
            data = displayedItems,
            isManualRefreshing = isRefreshing,     // <-- এটাই rule ৩-কে কার্যকর করে
            onRetry = { ... }
        ) { items -> /* body */ }
    }
}
```

**গুরুত্বপূর্ণ:** `SomadhanPullToRefresh` বাইরে, `SyncAwareContent`/`SyncAwareRefreshableContent`
ভেতরে — উল্টো করা যাবে না। আর `isManualRefreshing = isRefreshing` পাস করতে **ভুলে গেলে** rule ৩
(pull-to-refresh-এ শিমার ফিডব্যাক) কাজ করবে না, যদিও pull-gesture/স্পিনার তখনও ঠিকই দেখাবে — এই
বাগটা ধরা কঠিন কারণ স্ক্রিন সুপারফিশিয়ালি ঠিকই কাজ করে মনে হয়।

### ১.৫ `rememberFieldChangePulse<T>` — single-value ফিল্ডের জন্য (rule ২ + ৩-এর ছোট ভার্সন)

Wallet balance/escrow total-এর মতো একটা একক সংখ্যা/টেক্সট আপডেট হলে পুরো লিস্ট শিমার না করে শুধু
সেই একটা `Text`/`Card`-কে হালকা pulse করানোর জন্য:

```kotlin
@Composable
fun <T> rememberFieldChangePulse(
    value: T,
    isManualRefreshing: Boolean = false,   // rule ৩
    durationMs: Long = 350L,
    sessionKey: String? = null,            // rule ২ (re-entry) — না দিলে সেটা নিষ্ক্রিয় থাকে
    viewModel: SomadhanViewModel? = null,
    flashOnReentry: Boolean = true
): Boolean   // true হলে PulsingValue-এর ভেতরে pulse দেখাও
```

**কল-সাইট প্যাটার্ন:**
```kotlin
PulsingValue(
    isUpdating = rememberFieldChangePulse(
        value = currentBalance,
        isManualRefreshing = isRefreshing,
        sessionKey = "user_wallet_sync",   // এই স্ক্রিনের SyncAwareContent-এর একই sessionKey
        viewModel = viewModel
    )
) {
    Text(text = Formatters.formatTaka(currentBalance), ...)
}
```
> লক্ষণীয়: `sessionKey` এখানে সেই স্ক্রিনের বাইরের `SyncAwareContent`-এর সাথে **একই** key দিতে
> হবে — আলাদা key/আলাদা `markLoadedOnce()` কল-সাইট বানানোর দরকার নেই, কারণ বাইরের গেটই ইতিমধ্যে
> সেই key মার্ক করে রাখে।

---

## ধাপ ২ — নতুন কোনো স্ক্রিনে ৪টা rule বসানোর চেকলিস্ট (স্টেপ-বাই-স্টেপ)

একটা নতুন স্ক্রিনে (বা কোনো unmigrated স্ক্রিনে) কাজ করার সময় এই ক্রমেই এগোতে হবে:

1. **স্ক্রিনের টাইপ চিহ্নিত করো:**
   - (ক) single-value/object (প্রোফাইল, ওয়ালেট, ড্যাশবোর্ড) → `SyncAwareContent` + দরকার হলে
     `rememberFieldChangePulse` নির্দিষ্ট ফিল্ডে।
   - (খ) সাধারণ লিস্ট, pagination নেই → `SyncAwareRefreshableContent<T>` (data হিসেবে পুরো
     লিস্ট)।
   - (গ) pagination/"load more" আছে (ViewModel-এ `*Paged`/`mutableStateListOf`) →
     `SyncAwareRefreshableContent<T>` + `.toList()` snapshot (Ground Rule ১১)।
   - (ঘ) খুব বড়/জটিল স্ক্রিন (৫০০০+ লাইন, একাধিক payment/escrow flow) যেখানে পুরো body-কে
     content-lambda-তে মোড়ানো ঝুঁকিপূর্ণ → বিকল্প: বিদ্যমান early-return গেট
     (`rememberSessionAwareSkeletonGate` + `if (x == null) { skeleton; return }`) + এরর হলে
     `SyncBlockedRetryState(onRetry)` (এই কম্পোনেন্টই rule ১ কভার করে, rule ২ পুরোপুরি না — এটা
     ইচ্ছাকৃত ব্যতিক্রম, নিচে "known exceptions" দেখো)।
2. **sessionKey ঠিক করো** — প্রতিটা স্ক্রিনের/ট্যাবের নিজস্ব ইউনিক key (যেমন `"user_wallet_sync"`,
   `"admin_withdrawals_tab_sync"`)।
3. **কোন `syncPhase` লাগবে** ঠিক করো — একটা হলে সরাসরি, একাধিক sync-domain-এর উপর নির্ভরশীল হলে
   `worstSyncPhase(phase1, phase2, ...)` দিয়ে combine করো।
4. **header/filter/tab কম্পোনেন্টের বাইরে রাখো** — ভেতরে শুধু body/list/value।
5. **Pull-to-refresh আছে এমন স্ক্রিনে** `SomadhanPullToRefresh` দিয়ে বাইরে মুড়িয়ে
   `isManualRefreshing = isRefreshing` পাস করতে ভুলো না (rule ৩)।
6. **Pagination স্ক্রিনে** mutable list-কে `.toList()` করে data হিসেবে পাস করো (rule ৪)।
7. **এডিট শেষে:** bracket-balance ম্যানুয়ালি গুনে, পুরনো `SyncAwareContent(`/ভ্যারিয়েবল রেফারেন্স
   বাদ পড়েনি কিনা grep করে, পুরো zip-এর ফাইল-লিস্ট আগেরটার সাথে diff করে নিশ্চিত হও।
8. **এই ফাইলের "বর্তমান স্ট্যাটাস" সেকশন আপডেট করো** — নতুন স্ক্রিনটাকে সঠিক ক্যাটেগরিতে সরিয়ে দাও।

---

## ধাপ ৩ — কীভাবে verify করবে (কোনো দাবি করার আগে বাধ্যতামূলক)

```bash
# rule ১+২+৪ (SyncAwareRefreshableContent, ডেটা-ডিফ সহ পূর্ণাঙ্গ প্যাটার্ন) কোন স্ক্রিনে আছে:
grep -rl "SyncAwareRefreshableContent(" app/src/main/java/com/example/ui/screens

# rule ১ (শুধু cold-load, ডেটা-ডিফ ছাড়া) কোন স্ক্রিনে আছে:
grep -rl "SyncAwareContent(" app/src/main/java/com/example/ui/screens

# rule ৩ (pull-to-refresh wrapper) কোন স্ক্রিনে আছে:
grep -rl "SomadhanPullToRefresh(" app/src/main/java/com/example/ui/screens

# early-return গেট (ব্যতিক্রম প্যাটার্ন) কোন স্ক্রিনে আছে:
grep -rl "rememberSessionAwareSkeletonGate" app/src/main/java/com/example/ui/screens

# isManualRefreshing আসলেই wire করা আছে কিনা (শুধু PullToRefresh থাকলেই rule ৩ সম্পূর্ণ না):
grep -n "isManualRefreshing" app/src/main/java/com/example/ui/screens/<ScreenName>.kt
```

**একটা স্ক্রিনে "৪টা rule-ই আছে" বলার আগে চেকলিস্ট:**
- [ ] `SyncAwareRefreshableContent(` কল আছে (rule ১, ২, ৪-এর জন্য — pagination থাকলে)
- [ ] `isManualRefreshing = isRefreshing` পাস করা আছে (rule ৩)
- [ ] `SomadhanPullToRefresh` দিয়ে বাইরে মোড়ানো আছে (rule ৩)
- [ ] pagination থাকলে data `.toList()` snapshot হয়ে পাস হচ্ছে

চারটাই সত্যি হলে তবেই বলা যাবে সেই স্ক্রিনে ৪টা rule সম্পূর্ণ।

---

## ধাপ ৪ — বর্তমান স্ট্যাটাস (এই সেশন পর্যন্ত যাচাই করা, ব্যাচ ১০ পর্যন্ত, ২০২৬-০৯-১৪)

### ✅ ক্যাটেগরি A — ৪টা rule-ই সম্পূর্ণ (SyncAwareRefreshableContent + isManualRefreshing + pagination-safe)
`ActiveJobsPopupScreen`, `AllOpenProblemsScreen`, `BidManagementScreen`, `FavoriteSolversScreen`,
`InstantJobHistoryScreen`, `SolverAllPostsScreen`, `SolverCategoryPostsScreen` (ব্যাচ ৮, নতুন —
নিচে দেখুন), `SolverCompletedJobsScreen`, `SolverMyBidsScreen`, `SolverProblemsScreen`,
`SolverReviewsScreen`, `TransactionHistoryScreen`, `UserProblemsScreen`, `UserReviewsScreen`,
`WithdrawalHistoryScreen`
(১৫টা স্ক্রিন — `ActiveJobsPopupScreen`-এ pull-to-refresh প্রযোজ্যই না, এটা popup dialog)

> **ব্যাচ ৮-এ ক্যাটেগরি C থেকে এখানে migrate হলো:** `SolverCategoryPostsScreen` — আগের ম্যানুয়াল
> `minimumSkeletonActive`/`isInitialLoading` গেট এবং সরাসরি `ListScreenSkeleton`/
> `SyncBlockedRetryState` কল সরিয়ে পুরোপুরি `SyncAwareRefreshableContent`-এ migrate করা হলো।
> bulk-sync (`combinedSyncPhase`) আর এই ক্যাটাগরির নিজস্ব paginated fetch-এর প্রথম-পাতা-লোডিং
> অবস্থা — দুটো স্বাধীন loading-উৎসকে `worstSyncPhase` দিয়ে একটাই `syncPhase`-এ মিলিয়ে দেওয়া
> হয়েছে। `data = displayedProblems` (paginated non-search পথে `.toList()` স্ন্যাপশট, Ground
> Rule ১১ অনুযায়ী; সার্চ পথে আগে থেকেই immutable)। `isManualRefreshing = isRefreshing` (আগে থেকেই
> `SomadhanPullToRefresh` wrapper ছিল)। "ক্যাটাগরি পাওয়া যায়নি" fallback আর মূল `LazyColumn`
> দুটোই এখন content lambda-র ভেতরে (`{ displayedProblemsShown -> ... }`)। এডিটের পর
> brace/paren/bracket balance মূল আপলোডের সাথে তুলনা করে verify করা হয়েছে (কোনো imbalance নেই)।

### ✅ ক্যাটেগরি A′ — rule ১+২ সম্পূর্ণ, pagination নেই (B2 থেকে migrate হওয়া নতুন লিস্ট/অবজেক্ট স্ক্রিন, ব্যাচ ৬-৭ + AdminPanelScreen ব্যাচ ১১-১৮-এর কোনো এক পর্যায়ে)
`FaqScreen`, `InstantJobsScreen`, `NotificationDetailScreen`, `ProfileScreen`, `UserWalletScreen`
(ব্যাচ ৭), `AdminPanelScreen` (ব্যাচ ১১-১৮-এর মধ্যে কোনো এক সেশনে, এই ফাইলে বিস্তারিত লগ নেই —
ব্যাচ ১৯-এর শুরুতে `grep`-এ verify করা হয়েছে) — এই ৬টা `SyncAwareRefreshableContent`-এ migrate
হয়েছে। বিস্তারিত:

- **`AdminPanelScreen` (ব্যাচ ১১-১৮, বিস্তারিত লগ নেই, শুধু grep-verified বর্তমান অবস্থা):**
  `grep -c "SyncAwareRefreshableContent("` → ১১টা কল, `isManualRefreshing` ১১ জায়গায় wire করা।
  ১৩টা ট্যাবের ১২টা standard rule ১+২+৩(+৪) প্যাটার্নে। "পরিসংখ্যান ও চার্ট" (Overview) ট্যাবে ২টা
  `SyncAwareContent(` কল বাকি আছে (ব্যাচ ১০-এ ঠিক হওয়া ডিজাইন অনুযায়ী — independent শিমার-জোন +
  progressive/lazy লোড, যা `SyncAwareRefreshableContent`-এর single-wrap প্যাটার্নে হয় না বলেই
  ইচ্ছাকৃতভাবে আলাদা — `JobTrackingScreen`-এর planned প্যাটার্নের মতো)। Overview ট্যাবে
  pull-to-refresh ঠিক কীভাবে চান সেই খোলা প্রশ্নটা (ধাপ ৪-এ আগে লেখা ছিল) এই মধ্যবর্তী ব্যাচগুলোতে
  সমাধান হয়েছে কিনা এই ফাইলে জানা নেই — **পরের সেশনে ব্যবহারকারীর কাছে নিশ্চিত করে নেওয়া উচিত।**
- **`UserWalletScreen` (ব্যাচ ৭, নতুন):** আগে এই স্ক্রিন আংশিক ছিল — শুধু `rememberFieldChangePulse`
  দিয়ে balance/escrow দুটো ফিল্ডে আলাদা pulse (ক্যাটেগরি B2)। ব্যবহারকারীর স্পষ্ট সিদ্ধান্তে
  (full migrate) এখন পুরো `Column` body-ই `SyncAwareRefreshableContent`-এর ভেতরে —
  `data = listOf(currentUser, userEscrows, userTransactions, userGatewayPayments, allProblems,
  allAdditionalCharges, minWithdrawalAmount)` (ProfileScreen-এর মতো কম্পোজিট `listOf` diff key,
  content lambda-র প্যারামিটার ignore করা হয়েছে `{ _ -> ... }`, ভেতরের body আগের মতোই বাইরের
  scope-এর ভেরিয়েবল সরাসরি ব্যবহার করে)। দুটো পুরনো `PulsingValue`/`rememberFieldChangePulse`
  কল-সাইট (balance card, held-escrow total) সরিয়ে plain `Text` করা হয়েছে — কারণ এখন পুরো
  content-লেভেলেই diff/re-entry/manual-refresh শিমার হয়, তাই ফিল্ড-স্তরের আলাদা pulse আর দরকার
  নেই। `isManualRefreshing = isRefreshing` নতুন যোগ হলো `SyncAwareRefreshableContent`-এর কলে
  (আগে থেকেই `SomadhanPullToRefresh` wrapper ছিল, শুধু নতুন কম্পোনেন্টের প্যারামিটারে wire করা
  হলো)। `PulsingValue`/`rememberFieldChangePulse` ইম্পোর্ট দুটোও সরানো হয়েছে (এই ফাইলে আর কোথাও
  ব্যবহার হয় না)। Pagination নেই এই স্ক্রিনে।
- **`NotificationDetailScreen`:** pull-to-refresh নেই এই স্ক্রিনে, তাই rule ৩ প্রযোজ্যই না।
  `data = notification` (single nullable `NotificationEntity`, data class)।
- **`FaqScreen`:** pull-to-refresh নেই এই স্ক্রিনেও। `data = faqs` (raw `activeFaqs` StateFlow
  list, plain immutable, `.toList()` লাগেনি)। HorizontalPager ট্যাব/সার্চ ভেতরে থাকায়
  ট্যাব-সুইচ/টাইপিং-এ flash হয় না, শুধু আসল FAQ ডেটা বদলালে হয়।
- **`InstantJobsScreen`:** `data = Triple(activeJob, isToggleOn, displayNearbyJobs)`।
  **গুরুত্বপূর্ণ note:** এই স্ক্রিনে `InstantJobBidBottomSheet` (বিড দেওয়ার ফর্ম) আছে কিন্তু সেটা
  একটা আলাদা top-level `@Composable` ফাংশন হিসেবে `SyncAwareRefreshableContent` ব্লকের
  **বাইরে** (Scaffold বন্ধ হওয়ার পরে, `biddingJob?.let { ... }` দিয়ে) কল হয় — তাই এই migration
  বিড ফর্মকে স্পর্শ করেনি, শুধু মূল `LazyColumn` (active job card/toggle/nearby jobs list)।
  `isManualRefreshing = isRefreshing` নতুন যোগ হলো (আগে `SyncAwareContent`-এ এই প্যারামিটারই
  ছিল না, rule ৩ কাজ করছিল না)।
- **`ProfileScreen`:** `data = listOf(currentUser, myActiveCount, myCancelledCount,
  myCompletedCount, myBidsCount)` — `currentUser` (data class) সরাসরি বদলালে, বা
  `allProblems`/`allBids` বদলে derived count-গুলো বদলালে (দুটোই content-এ ব্যবহৃত), উভয় ক্ষেত্রেই
  `List.equals()`-এর structural diff-এ শিমার হবে। content lambda-র প্যারামিটার ইচ্ছাকৃতভাবে
  ignore করা হয়েছে (`{ _ -> ... }`) — ভেতরের body আগের মতোই বাইরের scope-এর
  currentUser/myActiveCount ইত্যাদি সরাসরি ব্যবহার করে। `isManualRefreshing = isRefreshing`
  যোগ হলো (আগে ছিল না)।

> **ব্যাচ ৫-এ নতুন যা migrate হলো:** পুরনো B1 তালিকার শেষ বাকি থাকা স্ক্রিন `TransactionHistoryScreen`
> (`SyncAwareContent` → `SyncAwareRefreshableContent`)। এই স্ক্রিনে একসাথে দুইটা আলাদা লিস্ট থাকে
> (APP ট্যাবের `filteredTransactions`, GATEWAY ট্যাবের `sortedGatewayPayments`) যেটা `selectedMainTab`
> দিয়ে সুইচ হয় — তাই শুধু সক্রিয় ট্যাবের লিস্ট `data`-তে পাঠালে ট্যাব-সুইচেও (আসল ডেটা না বদলালেও)
> অযথা diff-flash হতো। সমাধান: `data = Pair(filteredTransactions, sortedGatewayPayments)` — দুটো
> লিস্টই একসাথে পাঠানো হয়, flash শুধু প্রকৃত ডেটা-বদলে (realtime/pull-to-refresh) হবে, ট্যাব-সুইচে
> না। বাকি ৯টা B1 স্ক্রিন (`InstantJobHistoryScreen`, `SolverAllPostsScreen`,
> `SolverCompletedJobsScreen`, `SolverMyBidsScreen`, `SolverProblemsScreen`, `SolverReviewsScreen`,
> `UserProblemsScreen`, `UserReviewsScreen`, `WithdrawalHistoryScreen`) আগের ব্যাচেই migrate হয়ে
> গিয়েছিল এবং এই সেশনে `grep` দিয়ে পুনরায় verify করা হয়েছে (checklist অনুযায়ী: সবগুলোতেই
> `SyncAwareRefreshableContent(` কল, `isManualRefreshing = isRefreshing` wiring, আর
> `SomadhanPullToRefresh` wrapper তিনটাই আছে)। এই ৯টার pagination data-প্যারামিটারগুলো
> (`displayedHistoryItems`, `displayedPosts`, `completedJobs`, `pageFilteredBids`,
> `currentFilteredList` ইত্যাদি) সবই ইতিমধ্যে `remember{}`-এ derive করা immutable filtered
> list — raw `mutableStateListOf` সরাসরি পাঠানো হচ্ছে না, তাই Ground Rule ১১ (pagination-safe
> snapshot) সবগুলোতেই সন্তুষ্ট।
>
> ⚠️ `FavoriteSolversScreen.kt`-এ একটা pre-existing bug নোট করা আছে: এর `isRefreshing` একটা
> local `remember { mutableStateOf(false) }` যেটা কখনো `true` হয় না — তাই এই স্ক্রিনে
> `isManualRefreshing` পাস করা থাকলেও rule ৩-এর pulse বাস্তবে কখনো trigger হবে না। এটা এখনো
> ফিক্স করা হয়নি (এই সেশনেও touch করা হয়নি, scope-এর বাইরে)।

### ⚠️ ক্যাটেগরি B — rule ১ + ৩ আছে, কিন্তু rule ২/৪ (ডেটা-ডিফ শিমার) নেই — শুধু `SyncAwareContent` (data প্যারামিটার-বিহীন)

**B1 এখন খালি — Pagination-যুক্ত সবগুলো B1 স্ক্রিন `SyncAwareRefreshableContent`-এ migrate সম্পূর্ণ
(Ground Rule ১১ পুরোপুরি coverage, ব্যাচ ৫ শেষে)।**

**B2 — Pagination নেই, শুধু `SyncAwareContent` (rule ২ diff-শিমার এখনো নেই, কিন্তু rule ৪ প্রযোজ্যও না):**
`AdminPanelScreen` (১৩টা ট্যাব), `DashboardScreen`, `HomeScreen`, `PublicProfileReviewsScreen`

> **ব্যাচ ৬-এ B2 থেকে migrate হয়ে ক্যাটেগরি A′-এ গেছে:** `FaqScreen`, `InstantJobsScreen`,
> `NotificationDetailScreen`, `ProfileScreen`। **ব্যাচ ৭-এ আরও একটা:** `UserWalletScreen`
> (বিস্তারিত ক্যাটেগরি A′-এ দেখুন)।

> ⚠️ **[ব্যাচ ১০, ২০২৬-০৯-১৪] বাকি ৪টা B2 স্ক্রিনের target design এখন চূড়ান্ত (ব্যবহারকারী
> confirm করেছেন) — নিচের "প্রতিটা স্ক্রিনের target ডিজাইন" সাবসেকশনই এখন single source of
> truth। এখনো কোনোটাই migrate/implement হয়নি (শুধু ডিজাইন-সিদ্ধান্ত, কোড এখনো বদলায়নি) — নিচে
> "কেন এখনো migrate হয়নি" অংশে লেখা structural ঝুঁকি/কারণগুলো (বিশেষত `DashboardScreen`-এর
> sub-composable রিফ্যাক্টর প্রশ্ন) migrate শুরুর সময় সামলাতে হবে, ডিজাইনের সিদ্ধান্ত এতে বদলায়নি।**

> **বাকি থাকা B2 স্ক্রিনগুলো কেন এখনো migrate হয়নি (structural কারণ, ডিজাইন না):**
> - **`DashboardScreen`:** এর `SyncAwareContent`-এর content সরাসরি কিছু render করে না — পুরোটাই
>   `SolverDashboardContent(viewModel)` / `UserDashboardContent(viewModel)`-এ delegate করে, আর এই
>   দুটো sub-composable নিজেরাই আলাদাভাবে `viewModel.xxx.collectAsStateWithLifecycle()` কল করে
>   নিজেদের state আনে। তাই `DashboardScreen`-এর লেভেলে diff করার মতো কোনো একক `data` নেই —
>   `SyncAwareRefreshableContent`-এ migrate করতে হলে sub-composable দুটোকে state আনার বদলে
>   parameter হিসেবে data নিতে refactor করতে হবে, যেটা ছোট wrapper-swap না, ডেটা-ফ্লো বদলানোর
>   কাজ — ঝুঁকি বেশি, তাই এখনো হাত দেওয়া হয়নি।
> - **`HomeScreen`** (২৮১০ লাইন) **ও `AdminPanelScreen`** (১৩টা আলাদা ট্যাব, প্রতিটাতে নিজস্ব
>   `SyncAwareContent` কল) — আকার/জটিলতার কারণে এখনো ধরা হয়নি, নিজস্ব আলাদা সেশন/ব্যাচ দরকার।
> - **`PublicProfileReviewsScreen`:** ইচ্ছাকৃতভাবে ছোঁয়া হয়নি — এর একটা pre-existing দুই-স্তরের
>   প্যাটার্ন আছে (বাইরে bulk-pull sync-এর জন্য `SyncAwareContent`, ভেতরে ওই স্ক্রিনের নিজস্ব
>   per-user fetch-এর জন্য `rememberSessionAwareSkeletonGate` — কমেন্টেই লেখা আছে "বিদ্যমান
>   local-readiness অক্ষত রাখা হলো")। migrate শুরুর সময় এই দুই-স্তর ঠিক কীভাবে নিচের target
>   design-এর সাথে মিলবে সেটা কোডে বাস্তবায়নের সময় ঠিক করে নিতে হবে।
> - **`UserWalletScreen`:** ✅ ব্যাচ ৭-এ full migrate সম্পূর্ণ হয়েছে — দেখো ক্যাটেগরি A′।

#### প্রতিটা স্ক্রিনের target ডিজাইন (ব্যবহারকারীর নিজের ভাষায়, ব্যাচ ১০)

**১. `DashboardScreen`** (user + solver উভয় ড্যাশবোর্ড) — পুরো standard rule ১+২+৩(+৪) প্যাটার্ন:
   - **rule ১ (cold-load):** প্রথমবার পুরো পেজ শিমার দিয়ে লোড হবে।
   - **rule ২ (re-entry):** পুরো পেজের কাঠামো ঠিক থাকবে, শুধু যা যা ভ্যালু আছে সেগুলো শিমার দিয়ে
     লোড হবে — যেমন `UserDashboardContent`-এ মোট পোস্ট/মোট সম্পন্ন-এর ভ্যালু। `SolverDashboardContent`-এ
     এর পাশাপাশি সম্পন্ন কাজ ও transaction record-ও একই নিয়মে ভ্যালুর মতো শিমার হবে।
   - **rule ৩ (pull-to-refresh):** একইভাবে পুরো পেজের কাঠামো ঠিক রেখে শুধু ভ্যালুগুলো শিমার
     দিয়ে লোড হবে। user/solver দুই ড্যাশবোর্ডেই প্রযোজ্য।
   - **rule ৪ (pagination থাকলে):** next/prev-এ ক্লিক করলে শিমার দিয়ে পরের পেজ খুলবে।

**২. `HomeScreen`** (user + solver উভয়):
   - **rule ১:** প্রথমবার পুরো পেজ শিমার দিয়ে লোড হবে।
   - **rule ২ (re-entry):** কিছুই শিমার হবে না — আর কখনো flash না (`MessagesScreen`-এর মতো)।
   - **rule ৩ (pull-to-refresh):** পেজের নিচের দিকে থাকা সমস্যা-লিস্ট-এর কাঠামো ঠিক রেখে শুধু
     লিস্টটাই শিমার দিয়ে লোড হবে।
   - **rule ৪ (pagination):** next/prev পেজে ক্লিক করলে শিমার দিয়ে সেই পেজ লোড হবে।

**৩. `PublicProfileReviewsScreen`** — পুরো standard rule ১+২+৩ প্যাটার্ন:
   - **rule ১:** প্রথমবার পুরো পেজ শিমার দিয়ে লোড হবে।
   - **rule ২ (re-entry):** পুরো পেজের কাঠামো ঠিক রেখে শুধু রিভিউগুলো শিমার দিয়ে লোড হবে।
   - **rule ৩ (pull-to-refresh):** একইভাবে পুরো পেজের কাঠামো ঠিক রেখে শুধু রিভিউগুলো শিমার
     দিয়ে লোড হবে।

**৪. `AdminPanelScreen`** — ১৩টা ট্যাবের মধ্যে ১২টায় standard rule ১+২+৩(+৪), শুধু "Overview"
ট্যাবের (index ০) জন্য আলাদা ডিজাইন:
   - **১২টা সাধারণ ট্যাব** (`admin_withdrawals_sync`, `admin_users_sync`, `admin_problems_sync`,
     `admin_escrow_sync`, `admin_transactions_sync`, `admin_chat_monitoring_sync`,
     `admin_direct_contracts_sync`, `admin_user_lookup_sync`, `admin_solver_quota_sync`,
     `admin_instant_jobs_sync`, `admin_dispute_center_sync`, `admin_gateway_payments_sync`) —
     প্রতিটাতে overall standard ডিজাইন: প্রথমবার পুরো ট্যাব শিমার (rule ১), re-entry/ট্যাব-সুইচে
     কাঠামো ঠিক রেখে শুধু ভ্যালু/লিস্ট শিমার (rule ২), pull-to-refresh-এ একইভাবে (rule ৩),
     pagination থাকলে next/prev-এ শিমার (rule ৪)।
   - **Index ০ — "পরিসংখ্যান ও চার্ট" (`admin_overview_sync`, `AdminStatsView`, Admin Panel-এর
     Home/Overview ট্যাব) — আলাদা ডিজাইন, বাকি ১২টার মতো না:**
     - প্রথমবার পুরো ট্যাব শিমার দিয়ে লোড হবে (rule ১, বাকিদের মতোই)।
     - **re-entry-তে পুরো কাঠামো ঠিক থাকবে, কিন্তু শুধু যেসব value/card সত্যিই realtime-এ বদলায়
       সেগুলোই আলাদাভাবে শিমার করবে** — যেমন amount/balance বদলালে শুধু সেই balance-এর card-টাই
       শিমার করবে, বাকি সব card/value স্থির থাকবে। মানে `JobTrackingScreen`-এর মতোই একাধিক
       independent শিমার-জোন লাগবে, একটা single wrapper দিয়ে পুরো ট্যাব শিমার করানো যাবে না।
     - **স্ক্রল-এর সাথে progressive/lazy শিমার-লোড:** পেজ খোলার সাথে সাথে সব value/chart একসাথে
       লোড হবে না — যতটুকু স্ক্রল করা হবে ঠিক ততটুকুই ধীরে ধীরে শিমার দিয়ে লোড হবে। পুরো পেজের
       কাঠামো সবসময় ঠিক থাকবে, শুধু ভ্যালু/লিস্টগুলোই শিমার হবে।
     - pull-to-refresh (rule ৩) ব্যবহারকারী আলাদা করে উল্লেখ করেননি এই ট্যাবের জন্য — বাকি ১২টার
       মতোই কাঠামো ঠিক রেখে ভ্যালু-শিমার ধরে নেওয়া যেতে পারে, কিন্তু migrate শুরুর আগে নিশ্চিত
       করে নেওয়া ভালো।

### 🟣 ক্যাটেগরি E — এডিট-ফর্ম স্ক্রিন (ইচ্ছাকৃত ব্যতিক্রম, ব্যবহারকারীর সিদ্ধান্ত, ব্যাচ ৬)
`PostProblemScreen`, `SolverBalanceWithdrawScreen`, `SolverKycScreen`, `SolverSkillsScreen`,
`UserInfoScreen`, `UserWithdrawScreen` — এই ৬টা স্ক্রিন **rule ১-এই থাকবে** (`SyncAwareContent`,
শুধু cold-load), **rule ২ (diff-শিমার) ইচ্ছাকৃতভাবে বসানো হবে না।** কারণ: এগুলো active edit
form (ব্যবহারকারী মাঝপথে ফর্ম পূরণ করছে — যেমন `SolverSkillsScreen`-এ ক্যাটাগরি বাছাই করার
মাঝখানে, `selectedCategoryIds` local state)। এই সময় background-এ realtime data বদলে গেলে
(`currentUser`/`allCategories` ইত্যাদি) diff-শিমার পুরো ফর্মের উপর flash করলে ব্যবহারকারীর
মাঝপথের ইনপুট বিঘ্নিত হওয়ার ঝুঁকি আছে (Ground Rule ১ লঙ্ঘন হতে পারে)। এটা `MessagesScreen`
(ক্যাটেগরি D)-এর মতোই একটা সচেতন সিদ্ধান্ত, migrate করা যাবে না যতক্ষণ না ব্যবহারকারী স্পষ্টভাবে
অন্য সিদ্ধান্ত দেয়।

### 🔷 ক্যাটেগরি C — **[সিদ্ধান্ত পাল্টেছে, ব্যাচ ৮] আর ইচ্ছাকৃত ব্যতিক্রম না — migrate করতে হবে (২টা সম্পূর্ণ, ৪টা বাকি)**

> ⚠️ **গুরুত্বপূর্ণ — আগের কোনো session/note যদি বলে "ক্যাটেগরি C ইচ্ছাকৃত ব্যতিক্রম, migrate করার
> প্ল্যান নেই" — সেটা এখন আর সত্যি না।** ব্যবহারকারী স্পষ্টভাবে (ব্যাচ ৮, ২০২৬-০৯-১৪) এই সিদ্ধান্ত
> বদলে দিয়েছেন: এই ৬টা স্ক্রিনও migrate করতে হবে, প্রতিটার জন্য নিচে তার নিজস্ব সুনির্দিষ্ট target
> ডিজাইন বর্ণনা করা আছে (ব্যবহারকারীর নিজের ভাষায় বলা, হুবহু captured)। **এটাই এখন থেকে single
> source of truth — পুরোনো "migrate করার প্ল্যান নেই" প্যারাগ্রাফটা অপ্রচলিত (obsolete), মুছে এই
> নতুন স্পেসিফিকেশন দিয়ে প্রতিস্থাপিত করা হলো।**
>
> **ব্যাচ ৮-এ এই দফায় ২টা সম্পূর্ণ হয়েছে:** `SolverCategoryPostsScreen` (এখন ক্যাটেগরি A-তে, উপরে
> দেখুন) এবং `ChatScreen` (নিচে ক্যাটেগরি D-এর ঠিক আগে বিস্তারিত)। বাকি ৪টা (`ReputationDetailScreen`,
> `JobTrackingScreen`, `ProblemDetailScreen`, `PublicProfileScreen`) এখনো migrate হয়নি — এদের target
> ডিজাইন নিচে অপরিবর্তিত আছে।

`ReputationDetailScreen`, `JobTrackingScreen` (৫৮২৯ লাইন), `ProblemDetailScreen` (৫৫০৮ লাইন) —
বাকি ৩টা, এখনো migrate হয়নি। (`PublicProfileScreen` ব্যাচ ১৯-এ সম্পূর্ণ হয়ে ক্যাটেগরি A′-এ সরে
গেছে, নিচে ৪ নম্বর পয়েন্টের target ডিজাইন রেফারেন্সের জন্য রাখা হলো।)

#### ✅ `ChatScreen` (ব্যাচ ৮-এ সম্পূর্ণ)
`MessagesScreen`-এর প্যাটার্ন কপি করা হয়েছে (ক্যাটেগরি D)। `ChatScreen`-এ আগে থেকেই
`rememberSessionAwareSkeletonGate` + early-return গেট ছিল (rule ১ কভার করছিল), কিন্তু
early-return-এর শর্ত ছিল `problem == null || minimumSkeletonActive` — sessionKey একবার
`markLoadedOnce()` হয়ে যাওয়ার পরেও `problem` StateFlow কোনো কারণে (যেমন realtime resync)
ক্ষণিকের জন্য `null` হয়ে গেলে আবার পুরো `ChatSkeleton` flash হয়ে যেতে পারতো — এটাই
`MessagesScreen`-এর "sessionKey একবার লোড হয়ে গেলে আর কখনো flash না" গ্যারান্টি থেকে ব্যতিক্রম
ছিল। ফিক্স: শর্তে `!viewModel.hasLoadedOnce("chat_$problemId")` যোগ করা হলো — এখন sessionKey
একবার মার্ক-লোডেড হয়ে গেলে, `problem` পরে কখনো null হলেও আর early-return/skeleton হবে না।
এটুকুই ছিল আসল পরিবর্তন (বাকি স্ক্রিন অক্ষত)।

> **✅ ব্যাচ ৯-এ ফিক্স হয়েছে (আগের "খোলা প্রশ্ন"):** `ChatScreen`-এ এতদিন কোনো sync-error/retry
> UI ছিল না (শুধু null/loading চেক)। এখন `ReputationDetailScreen`/`JobTrackingScreen`-এর মতো
> একই `SyncBlockedRetryState` প্যাটার্ন যোগ হলো: cold-load early-return গেটের ভেতরেই,
> `problem == null && initialSyncPhase == ERROR` হলে (মানে শুরুর bulk-pull সম্পূর্ণ ব্যর্থ, তাই
> `problem` আর কখনো আসবে না) `SyncBlockedRetryState(onRetry = { viewModel.retryInitialSync() })`
> দেখায়, নাহলে আগের মতোই `ChatSkeleton`। rule ১-এর `hasLoadedOnce`/`minimumSkeletonActive`
> গেট-লজিক অপরিবর্তিত। শুধু ২টা import + এই একটা error-check ব্লক যোগ হয়েছে, বাকি স্ক্রিন অক্ষত
> (brace/paren/bracket balance আগের zip-এর সাথে তুলনা করে verify করা হয়েছে)।

**বর্তমান অবস্থা (ব্যাচ ৮-এর শুরুতে `grep`/`wc -l` দিয়ে verify করা, Ground Rule ১৩):** সবগুলোই
`rememberSessionAwareSkeletonGate` + early-return (`if (x == null) {...; return}`) প্যাটার্নে, error
হলে `SyncBlockedRetryState(onRetry)`। rule ১ কভার করে, rule ২ পুরোপুরি না। `SomadhanPullToRefresh`
**শুধু** `SolverCategoryPostsScreen`-এ আছে (`ProblemDetailScreen`/`ReputationDetailScreen`/
`PublicProfileScreen`/`ChatScreen`/`JobTrackingScreen`-এ pull-to-refresh এখনো নেইই — মানে
`ProblemDetailScreen`-এর জন্য নিচের target অনুযায়ী rule ৩ যোগ করতে হলে এটা নতুন ফিচার হিসেবে
**যোগ** করতে হবে, শুধু re-pattern না)। `SolverCategoryPostsScreen`-এ pagination আছে
(`solverCategoryPostsPaged`, `mutableStateListOf`-ভিত্তিক)।

#### প্রতিটা স্ক্রিনের target ডিজাইন (ব্যবহারকারীর নিজের ভাষায়, ব্যাচ ৮)

**১. `ChatScreen`** — **`MessagesScreen`-এ যেভাবে করা হয়েছে ঠিক সেভাবেই** (ক্যাটেগরি D প্যাটার্ন
হুবহু কপি): app session-এ প্রথমবার ঢোকার সময় একবার loading/skeleton, তারপর realtime-এ নতুন মেসেজ
এলে বা re-entry হলে **আর কখনো skeleton/shimmer দেখাবে না** (কোনো flash-ই না)। rule ২/৩-এর সাধারণ
diff/flash প্যাটার্নে না।

**২. `ProblemDetailScreen`** — পুরো standard rule ১+২+৩ প্যাটার্ন:
   - **rule ১:** প্রথমবার পুরো পেজ স্কেলিটন লোড।
   - **rule ২ (re-entry):** ইউজার বা সলভার re-entry করলে পুরো পেজের কাঠামো ঠিক রেখে শুধু
     list/value অংশে শিমার।
   - **rule ৩ (pull-to-refresh):** কাঠামো ঠিক রেখে শুধু list/value অংশে শিমার — **⚠️ এই স্ক্রিনে
     pull-to-refresh বর্তমানে নেইই, তাই এটা নতুন যোগ করতে হবে** (`SomadhanPullToRefresh` wrapper +
     `isRefreshing`/`onRefresh` wiring থেকে শুরু করে)।
   - rule ৪ (pagination) ব্যবহারকারী উল্লেখ করেননি — সম্ভবত প্রযোজ্য না, migrate শুরুর আগে
     নিশ্চিত করে নিতে হবে।

**৩. `ReputationDetailScreen`** —
   - **rule ১:** প্রথমবার পুরো পেজ লোড।
   - **rule ২ (re-entry):** পুরো পেজের কাঠামো ঠিক রেখে শুধু list/value শিমার।
   - **rule ৩:** ব্যবহারকারী উল্লেখ করেননি এবং এই স্ক্রিনে বর্তমানে pull-to-refresh নেই —
     **অনিশ্চিত, migrate শুরুর আগে নিশ্চিত করে নিতে হবে** এই স্ক্রিনে pull-to-refresh যোগ করা
     লাগবে কিনা।

**৪. `PublicProfileScreen`** — ✅ **ব্যাচ ১৯-এ সম্পূর্ণ হয়েছে।**
   - **rule ১:** প্রথমবার পুরো পেজ শিমার লোড। (অপরিবর্তিত, আগে থেকেই ছিল।)
   - **rule ২ (re-entry):** **শিমার দেখানোর দরকার নেই** (আর কখনো flash না, অনেকটা
     `MessagesScreen`/`ChatScreen`-এর মতোই এই স্ক্রিনের জন্য)।
   - **rule ৩:** প্রযোজ্যই না — এই স্ক্রিনে pull-to-refresh নেই (নতুন যোগ করা হয়নি, স্কোপের বাইরে)।
   - **যা পাওয়া গেছিল (root cause):** টার্গেট ডিজাইনের অনুমান সঠিক ছিল — বড় refactor লাগেনি।
     আসল সমস্যা ছিল: `userProfile`/`isLoading` local state `remember { }` (কোনো key ছাড়া) দিয়ে
     সবসময় `null`/`true` দিয়ে শুরু হতো, তারপর একটা `LaunchedEffect` async-ভাবে
     `allUsers.find { }`-এর ফলাফল বসাতো — যদিও ডেটা আসলে ইতিমধ্যেই `allUsers` StateFlow-তে ক্যাশ
     থাকতো (সাধারণ re-entry কেসে), তাও প্রথম ফ্রেম(গুলো)-এ `isLoading == true` থাকায়
     `DetailScreenSkeleton` এক ঝলক flash করতো।
   - **ফিক্স:** `userProfile`/`isLoading`/`ratings`-কে `remember(userId) { }`-এ key করে,
     `allUsers.find { it.id == userId }`-এর সিঙ্ক্রোনাস ফলাফল (`initialFoundUser`) দিয়ে সরাসরি
     initial state বসানো হলো (`mutableStateOf(initialFoundUser)` /
     `mutableStateOf(initialFoundUser == null)`) — ফলে ক্যাশে থাকা প্রোফাইলে re-entry করলে কোনো
     null-window/flash-ই তৈরি হয় না। পুরনো `LaunchedEffect(userId, foundUser)` (যেটা realtime
     আপডেট আর প্রথমবার-না-থাকা-ইউজারের DB fallback দুটোই হ্যান্ডল করে) **অপরিবর্তিত রাখা হয়েছে** —
     শুধু তার initial-state-নির্ভরতা সরানো হলো। `rememberSessionAwareSkeletonGate`/
     `markLoadedOnce`/skeleton-condition (`if (isLoading || minimumSkeletonActive)`)-এ কোনো হাত
     দেওয়া হয়নি (ইচ্ছাকৃতভাবে — `hasLoadedOnce`-ভিত্তিক override যোগ করলে `targetUser == null`
     হলে "প্রোফাইল পাওয়া যায়নি" স্ক্রিন ক্ষণিকের জন্য flash হওয়ার নতুন ঝুঁকি তৈরি হতো, এই root-cause
     ফিক্সে সেই ঝুঁকি নেই)। এডিটের পর brace/paren/bracket balance zero-imbalance ভেরিফাই করা
     হয়েছে, ফাইল-সংখ্যা (২৪৫/২৪৫, dotfile সহ) মূল zip-এর সাথে হুবহু মিলিয়ে দেখা হয়েছে।
   - **⚠️ এখনো বাকি:** real Gradle/Android Studio build এই ফিক্সেও verify হয়নি — শুধু ম্যানুয়াল
     bracket-balance আর লজিক-রিভিউ।

**৫. `SolverCategoryPostsScreen`** — পুরো standard rule ১+২+৩(+৪) প্যাটার্ন:
   - **rule ১:** প্রথমবার শিমার লোড।
   - **rule ২ (re-entry):** পুরো পেজ শিমার **না** — শুধু list/value অংশে শিমার।
   - **rule ৩ (pull-to-refresh):** কাঠামো ঠিক রেখে শুধু list/value অংশে শিমার (ইতিমধ্যেই
     `SomadhanPullToRefresh` আছে, `SyncAwareRefreshableContent`-এ full migrate করে
     `isManualRefreshing` wire করতে হবে)।
   - **rule ৪:** এই স্ক্রিনে pagination আছে (`solverCategoryPostsPaged`) — Ground Rule ১১
     অনুযায়ী `.toList()` snapshot নিয়ে migrate করতে হবে।
   - এই স্ক্রিনটা আসলে ক্যাটেগরি A-এর ১৪টা স্ক্রিনের মতোই standard full migrate — অন্য ৫টার
     তুলনায় সবচেয়ে সরল।

**৬. `JobTrackingScreen`** — **সবচেয়ে জটিল, standard single-wrapper প্যাটার্নে হবে না।**
ব্যবহারকারীর বর্ণনা অনুযায়ী এই স্ক্রিনের flow-টা কয়েকটা আলাদা ধাপ/জোনে ভাগ, প্রতিটার নিজস্ব
independent শিমার লাগবে (একটা মিলিয়ে-একটা `SyncAwareRefreshableContent`-এ পুরোটা wrap করলে হবে
না, কারণ তাতে একটা ছোট অংশ বদলালেও পুরো body একসাথে শিমার করবে, যেটা এখানে চাওয়া হচ্ছে না):
   - **ধাপ ক (post creation → tracking open):** ইউজার পোস্ট create করার সাথে সাথেই কিছুক্ষণ
     পুরো পেজ শিমার দিয়ে লোড হবে, তারপর live tracking ভিউ খুলবে (rule ১-এর মতো cold-load)।
   - **ধাপ খ (bid list, realtime):** সলভাররা বিড দিলে সেগুলো realtime-এ আসবে — **পুরো স্ক্রিনের
     কাঠামো ঠিক রেখে শুধু বিড-লিস্টের অংশটাই** হালকা শিমার দিয়ে নতুন বিড দেখাবে (বাকি সব অংশ
     অপরিবর্তিত/স্থির থাকবে)।
   - **ধাপ গ (bid select → live tracking transition):** ইউজার একটা বিড সিলেক্ট করার পর শিমার
     দিয়ে লাইভ-ট্র্যাকিং পেজ খুলবে (এই ট্রানজিশনটারও নিজস্ব ছোট cold-load শিমার আছে)।
   - **ধাপ ঘ (live tracking-এর ভেতরে, realtime updates):** ইউজার/সলভারের প্রতিটা রিয়েল-টাইম
     আপডেট **শুধু সেই নির্দিষ্ট value/list/card-টার উপরেই** শিমার করবে, বাকি পুরো স্ক্রিনের কাঠামো
     খোলা/স্থির থাকবে — মানে একই স্ক্রিনে **একাধিক স্বাধীন শিমার-জোন** থাকবে, একটা আপডেট হলে
     অন্যগুলো নড়বে না।
   - **ডিজাইন-নোট:** এই প্যাটার্নটা বিদ্যমান ৪টা বিল্ডিং ব্লকের কোনোটার (বিশেষত
     `SyncAwareRefreshableContent`) সরাসরি single-wrap ব্যবহারে হবে না, কারণ ওটা `data`-এর
     *যেকোনো* অংশ বদলালে পুরো `content` lambda-টা একসাথে শিমার করে। এখানে বরং প্রতিটা independent
     জোনের (বিড-লিস্ট, প্রতিটা live-tracking value/card) জন্য **আলাদা আলাদা** ছোট diff-scope
     লাগবে — পুরনো `UserWalletScreen`-এর pre-batch-৭ `rememberFieldChangePulse` পদ্ধতির মতোই,
     কিন্তু একাধিক স্বাধীন zone-এ, বা প্রতিটা জোনকে নিজের ছোট `SyncAwareRefreshableContent`-এ
     আলাদাভাবে wrap করে (একটার data বদলালে শুধু সেটাই শিমার করবে, বাকিগুলো তাদের নিজস্ব অপরিবর্তিত
     `lastShownData`-এর কারণে স্থির থাকবে — এটা তাত্ত্বিকভাবে সম্ভব যদি প্রতিটা জোন তার নিজস্ব
     `sessionKey` আর নিজস্ব ছোট `data` নিয়ে আলাদাভাবে কল হয়)। **migrate শুরুর আগে ফাইলটা
     (৫৮২৯ লাইন) ভালোভাবে পড়ে ঠিক কোন কোন widget/card আলাদা zone হবে সেটা ম্যাপ করে নিতে হবে —
     ছোট, independently-testable ধাপে ভাগ করে করতে হবে (Ground Rule ৪)।**

#### খোলা প্রশ্ন (migrate শুরুর আগে ব্যবহারকারীর কাছ থেকে নিশ্চিত করে নিতে হবে)
- `ReputationDetailScreen`-এ pull-to-refresh (rule ৩) যোগ করা লাগবে কিনা — ব্যবহারকারী উল্লেখ
  করেননি।
- ~~`ProblemDetailScreen`-এ rule ৪ (pagination) প্রযোজ্য কিনা।~~ — **অপ্রাসঙ্গিক হয়ে গেছে,**
  `ProblemDetailScreen` ব্যাচ ২০-২৩-এর কোনো এক পর্যায়ে ইতিমধ্যে migrate হয়ে গেছে (দেখো ধাপ ৫)।
- ✅ **`JobTrackingScreen`-এর ৬টা zone-ই এখন সম্পূর্ণ (ব্যাচ ২৭ শেষে)** — (১) bid list
  (`UserJobBroadcastingScreen`, ✅ ব্যাচ ২৪), (২) status badge, (৩) stepper timeline, (৪)
  other-party profile row, (৫) money/problem-brief card (✅ ব্যাচ ২৫), (৬) action CTA বাটন (✅
  ব্যাচ ২৭ — ডিজাইন কনফার্ম হয়েছিল ব্যাচ ২৬-এ, কোড এই ব্যাচে লেখা হলো)। সব ৬টা zone-ই
  `JobTrackingBottomSheet`-এর ভেতরে, প্রতিটা `PulsingValue`+`rememberFieldChangePulse` প্যাটার্নে,
  `sessionKey = "job_tracking_${problem.id}"`।
  **zone ৬-এর বাস্তবায়ন (ব্যাচ ২৭, আগে ব্যাচ ২৬-এ শুধু ডিজাইন কনফার্ম হয়েছিল):** এই zone অন্য ৫টার
  মতো "একই structure-এ শুধু ভ্যালু বদলায়" না — role(User/Solver) ×
  status(ACCEPTED/ARRIVED/WORK_IN_PROGRESS/COMPLETED) × sub-state (`hasPendingExtra`,
  `hasReleaseReq`, `isDisputeActive`)-এর কম্বিনেশনে সম্পূর্ণ আলাদা বাটন-সেট/কার্ড দেখায়, তাই আলাদা
  আলাদা leaf না মুড়িয়ে পুরো `if (COMPLETED) {...} else if (!isUserRole) { when(status) {...} }
  else { when(status) {...} }` ব্লকটাকেই (`JobTrackingScreen.kt`, `// 5. Role-Specific Action CTA
  Buttons` কমেন্ট থেকে শুরু, ব্যাচ ২৭-এর zip-এ লাইন ~২২৫৬) একটা single `PulsingValue`-তে মোড়ানো
  হলো: `value = listOf(status, hasPendingExtra, ctaHasReleaseReq, ctaIsDisputeActive)`।
  `ctaHasReleaseReq`/`ctaIsDisputeActive` নামে দুটো নতুন local val যোগ হয়েছে
  (`problem.hasReleaseRequest` / `problem.isDisputed && problem.disputeSettledAt == null`) শুধু
  এই pulse-trigger value-এর জন্য — if/when ব্লকের ভেতরের বিদ্যমান local val
  `hasReleaseReq`/`isDisputeActive` (solver/user শাখায় আলাদা আলাদাভাবে, একই ফর্মুলায়) ইচ্ছাকৃতভাবে
  অপরিবর্তিত রাখা হয়েছে (Ground Rule ১ — বিদ্যমান লজিক স্পর্শ না করে শুধু visual pulse যোগ করা)।
  `isSubmittingSolverArrived`/`isSubmittingSolverStartJob`/`isRequestingRelease`/
  `isRejectingExtraAmount`-এর মতো submit-লোডিং ফ্ল্যাগগুলো ইচ্ছাকৃতভাবে `value`-তে নেই — ওগুলো
  নিজেরাই spinner-এ দেখায়, `pulse` দিলে ডাবল-ফিডব্যাক হতো। বিস্তারিত নিচে ধাপ ৭, ব্যাচ ২৭ এন্ট্রিতে।
- ✅✅ **zone ৬-এর crossfade আপগ্রেড (ব্যাচ ২৮-এ ডিজাইন কনফার্ম, ব্যাচ ২৯-এ কোড বাস্তবায়িত — সম্পূর্ণ):**
  ব্যবহারকারী ব্যাচ ২৭-এর `PulsingValue` (opacity-pulse-only) ফলাফল দেখে confirm করেছেন যে zone ৬-এ
  (action CTA) সত্যিকারের **old→new crossfade** চান, কারণ এখানে leaf ভ্যালু না, পুরো বাটন-সেট/
  কার্ড-structure বদলায় — `PulsingValue` শুধু opacity pulse করে, ভেতরের content সাথে সাথেই (কোনো
  crossfade ছাড়া) swap হয়ে যায়, যেটা zone ৬-এ খানিকটা "পপ" করে বদলানোর মতো লাগে।
  **টেকনিক্যাল সিদ্ধান্ত:** এই একটা zone-এর জন্য (শুধু zone ৬, বাকি ৫টা zone-এ `PulsingValue`-ই
  থাকবে, ওগুলোতে leaf-ভ্যালু-বদল হয় বলে pulse-ই যথেষ্ট এবং সঠিক প্যাটার্ন) `PulsingValue`-এর বদলে
  Compose-এর **`AnimatedContent`** ব্যবহার হবে (import: `androidx.compose.animation.AnimatedContent`,
  `androidx.compose.animation.togetherWith` — `fadeIn`/`fadeOut` ইতিমধ্যেই import করা আছে, নতুন
  করে লাগবে না)। **কেন `AnimatedContent`, শুধু `Crossfade` না:** `AnimatedContent`-এ প্রতিটা
  targetState-এর জন্য আলাদা `transitionSpec` ও (দরকার হলে) `SizeTransform` কনফিগার করা যায় — এই
  zone-এ branch-ভেদে Column-এর height ভিন্ন (completed-summary vs solver-buttons vs user-approval-
  card), তাই height পরিবর্তনও smooth animate করা দরকার, যেটা প্লেইন `Crossfade` করে না।
  **Compose gotcha (গুরুত্বপূর্ণ, অবশ্যই মেনে চলতে হবে):** `AnimatedContent`-এর বাইরে যাওয়া
  (exiting) content-টা যদি বাইরের live/ambient state (যেমন সরাসরি `status` ভ্যারিয়েবল) পড়ে, তাহলে
  সেই exit-composable-ও recompose হয়ে নতুন ভ্যালু দেখিয়ে ফেলতে পারে — তাতে crossfade ভেঙে যাবে
  (পুরনো UI freeze না হয়ে ওটাও নতুন হয়ে যাবে)। তাই exit-content-কে "freeze" রাখতে হলে content
  lambda-টা **শুধু নিজের parameter (targetState)-এর উপর নির্ভর করতে হবে**, বাইরের live var-এর
  উপর না। এই কারণে নিচের প্যাটার্নে `status`/`hasPendingExtra`-কে content lambda-র ভেতরে
  targetState থেকে **shadow** করা হচ্ছে (if/when ব্লক অপরিবর্তিত রেখে, শুধু নামের ভ্যালু-সোর্স
  বদলে)।
  **কোড-প্যাটার্ন (কনফার্মড, বাস্তবায়নের আগে file-এ exact line-number আবার যাচাই করা বাধ্যতামূলক,
  Ground Rule ১৩ — ব্যাচ ২৭-এর zip-এ `// 5. Role-Specific Action CTA Buttons` কমেন্ট লাইন ২২৫৬-এ,
  `PulsingValue(` কল লাইন ২২৭১-এ, if/else-if/else ব্লক লাইন ২২৭৮-২৮১০-এ, ক্লোজিং লাইন ২৮১১-এ):**
  ```kotlin
  // JobTrackingScreen.kt-এ ফাইলের top-level-এ (অন্য data class-গুলোর কাছাকাছি) নতুন যোগ হবে:
  private data class JobTrackingCtaKey(
      val status: String,
      val hasPendingExtra: Boolean,
      val hasReleaseReq: Boolean,
      val isDisputeActive: Boolean
  )

  // বর্তমান PulsingValue(...) { ... } ব্লকের বদলে (ctaHasReleaseReq/ctaIsDisputeActive val দুটো
  // অপরিবর্তিত/আগের মতোই থাকবে, শুধু এখন ওগুলো ctaKey বানাতে ব্যবহৃত হবে):
  val ctaKey = JobTrackingCtaKey(status, hasPendingExtra, ctaHasReleaseReq, ctaIsDisputeActive)
  AnimatedContent(
      targetState = ctaKey,
      transitionSpec = {
          fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
          // SizeTransform override করা লাগবে না — AnimatedContent-এর default SizeTransform
          // bounds-টা spring দিয়ে smooth animate করে, যেটা এই height-ভিন্ন branch-গুলোর জন্যই দরকার।
      },
      contentAlignment = Alignment.TopStart,
      label = "job_tracking_cta_crossfade"
  ) { key ->
      // Compose gotcha (উপরে ব্যাখ্যা করা) এড়াতে shadow — if/when ব্লকের ভেতরের বাকি সব কোড
      // (ব্যাচ ২৭-এর মতোই) অপরিবর্তিত থাকবে, শুধু এই দুইটা নাম এখন key থেকে আসছে বাইরের
      // live var-এর বদলে:
      val status = key.status
      val hasPendingExtra = key.hasPendingExtra
      if (status == "JOB_COMPLETED" || status == "COMPLETED") { ... }
      else if (!isUserRole) { when (status) { ... } }
      else { when (status) { ... } }
  }
  ```
  **ইচ্ছাকৃতভাবে যা স্পর্শ করা হচ্ছে না (Ground Rule ১, ঝুঁকি কমাতে):** if/when ব্লকের *ভেতরের*
  বিদ্যমান local val `hasReleaseReq`/`isDisputeActive` (solver শাখায় ~লাইন ২৪৪৬/২৩৩৯, user শাখায়
  ~২৬১৫/২৫৪৮ — line-number নতুন করে verify করা লাগবে) আগের মতোই সরাসরি `problem.hasReleaseRequest`/
  `problem.isDisputed && problem.disputeSettledAt == null` থেকে **live** পড়বে, `key`-থেকে না।
  **তাত্ত্বিক ট্রেড-অফ:** transition-এর ~১৬০-২২০ms জানালায় যদি `problem`-এর এই sub-fields
  exact ওই মুহূর্তে বদলায়, exit-হওয়া branch-এর কোনো নেস্টেড sub-text/sub-card একটু শিফট করতে
  পারে (top-level বাটন-সেট crossfade তবু ঠিকই কাজ করবে, কারণ সেটা `key`-নির্ভর) — এটা খুবই বিরল
  edge case এবং ইচ্ছাকৃতভাবে গ্রহণ করা হয়েছে যাতে ভেতরের বিদ্যমান বিজনেস-লজিক declaration-গুলো
  স্পর্শ করতে না হয়। পুরোপুরি frozen চাইলে ভবিষ্যতে `hasReleaseReq`/`isDisputeActive`-ও `key`
  থেকে shadow করা যায় — কিন্তু সেটা আরও বেশি লাইন স্পর্শ করবে, তাই এই ব্যাচে out-of-scope রাখা হলো।
  submit-লোডিং ফ্ল্যাগ ও অন্যান্য money-related ভ্যালু (যেমন `netSolverAmount`) আগের মতোই
  `ctaKey`-এর বাইরে/live থাকবে (ব্যাচ ২৬-এর একই যুক্তি — ডাবল-ফিডব্যাক এড়ানো)।
  **@OptIn সতর্কতা:** এই Compose BOM (`2024.09.00`)-এ `AnimatedContent`/`togetherWith` স্টেবল হওয়ার
  কথা (`@ExperimentalAnimationApi` লাগার কথা না), কিন্তু যেহেতু এই sandbox-এ real build করা যায়
  না (Ground Rule ২), বাস্তবায়নের সময় compile error এলে `@OptIn(ExperimentalAnimationApi::class)`
  যোগ করে দেখতে হবে।
- `AdminPanelScreen`-এর "পরিসংখ্যান ও চার্ট" (Overview, index ০) ট্যাবে pull-to-refresh (rule ৩)
  ঠিক কীভাবে চান তা ব্যবহারকারী স্পষ্ট করেননি (B2 target design দ্রষ্টব্য) — migrate শুরুর আগে
  নিশ্চিত করে নিতে হবে।

### 🟡 ক্যাটেগরি D — MessagesScreen (স্পষ্ট ব্যতিক্রম, ব্যবহারকারীর সিদ্ধান্ত)
`MessagesScreen` **ইচ্ছাকৃতভাবে** rule ২/৩-এর diff/re-entry-flash প্যাটার্নে নেই। এখানে
`SyncAwareContent` (data-বিহীন ভার্সন) ব্যবহার হয় — আচরণ: app session-এ **প্রথমবার** স্ক্রিনে
ঢোকার সময় একবার loading/skeleton, তারপর realtime-এ নতুন মেসেজ এলে বা re-entry হলে **আর কখনো
skeleton/shimmer দেখাবে না** (কোনো flash-ই না)। এটা rule ১-৪-এর সাধারণ প্যাটার্ন থেকে সচেতনভাবে
আলাদা — এই স্ক্রিনের নিজস্ব ডিজাইন হিসেবেই রাখতে হবে, migrate করা যাবে না। **(ব্যাচ ৮ নোট:
`ChatScreen`-কেও এখন এই একই ডিজাইনে আনতে হবে, উপরে ক্যাটেগরি C-এর ১ নম্বর পয়েন্ট দেখুন — কিন্তু
`MessagesScreen` নিজে এখনো migrate হবে না, শুধু `ChatScreen` এটার প্যাটার্ন অনুসরণ করবে।)**

### সংক্ষিপ্ত সারণী

| স্ক্রিন | rule ১ (cold) | rule ২ (re-entry/realtime diff) | rule ৩ (pull-to-refresh শিমার) | rule ৪ (pagination শিমার) |
|---|---|---|---|---|
| ১৫টা (ক্যাটেগরি A, pagination সহ/ছাড়া উভয়, `SolverCategoryPostsScreen` ব্যাচ ৮-এ যোগ) | ✅ | ✅ | ✅ (১৪টায়; popup-এ প্রযোজ্য না) | ✅ (যেখানে pagination আছে) |
| ৫টা (ক্যাটেগরি A′, ব্যাচ ৬-৭-এ migrate, `UserWalletScreen` সহ) | ✅ | ✅ | ✅ (৩টায়; `FaqScreen`/`NotificationDetailScreen`-এ pull-to-refresh প্রযোজ্যই না) | প্রযোজ্য না |
| ৩টা non-pagination বাকি (B2, `DashboardScreen`/`HomeScreen`/`PublicProfileReviewsScreen`, target design ব্যাচ ১০-এ চূড়ান্ত, migrate এখনো বাকি — `AdminPanelScreen` ব্যাচ ১১-১৮-এর কোনো সেশনে সম্পূর্ণ হয়ে A′-এ সরে গেছে) | ✅ | ❌ | ✅ | `DashboardScreen`-এ প্রযোজ্য (pagination থাকলে), বাকি ২টায় না |
| `PublicProfileScreen` (আগে ক্যাটেগরি C-এর অংশ, ব্যাচ ১৯-এ সম্পূর্ণ) | ✅ | ✅ (flash-বিহীন target, `initialFoundUser`/keyed-remember ফিক্স, নিচে ব্যাচ ১৯ দেখুন) | প্রযোজ্য না (pull-to-refresh নেই) | প্রযোজ্য না |
| ৬টা এডিট-ফর্ম (E, ইচ্ছাকৃত ব্যতিক্রম) | ✅ | ❌ (ইচ্ছাকৃত) | আংশিক | প্রযোজ্য না |
| ৪টা বড়/জটিল বাকি (C, `SolverCategoryPostsScreen`+`ChatScreen` ব্যাচ ৮-এ সম্পূর্ণ হয়ে বেরিয়ে গেছে) | বর্তমানে ✅ (আলাদা মেকানিজমে) | বর্তমানে ❌, প্রতিটার নিজস্ব target ঠিক হয়েছে (উপরে দেখুন) | বর্তমানে কোথাও নেই, যোগ করতে হবে (কিছু ক্ষেত্রে খোলা প্রশ্ন) | প্রযোজ্য না (এই ৪টায় pagination নেই) |
| MessagesScreen + `ChatScreen` (D, ব্যাচ ৮-এ `ChatScreen` যোগ) | ✅ | ❌ (ইচ্ছাকৃত, ভিন্ন ডিজাইন — আর কখনো flash না) | ✅ (শুধু `MessagesScreen`-এ; `ChatScreen`-এ pull-to-refresh নেই/প্রযোজ্য না) | প্রযোজ্য না |

---

## ধাপ ৫ — পরের সেশনের জন্য অগ্রাধিকার (এই মুহূর্তে সবচেয়ে জরুরি যা বাকি)

> ⚠️ **(ব্যাচ ২০-২৩, কোনো সেশন-লগ এই ফাইলে ছিল না — Ground Rule ১২-এর আরেকটা ব্যতিক্রম, ঠিক
> ব্যাচ ১১-১৮-এর গ্যাপের মতোই):** ব্যাচ ২৪-এর শুরুতে (নিচে দেখুন) `grep` দিয়ে সরাসরি কোডে verify
> করে জানা গেছে, এই মধ্যবর্তী ব্যাচগুলোতে B2/C-এর বাকি থাকা ৪টা স্ক্রিন ইতিমধ্যে পূর্ণাঙ্গ migrate
> হয়ে গিয়েছিল: **`DashboardScreen`** (২টা `SyncAwareRefreshableContent` কল —
> Solver/UserDashboardContent দুটোর জন্য আলাদা, `isManualRefreshing` দুটোতেই wire করা),
> **`PublicProfileReviewsScreen`**, **`ReputationDetailScreen`**, **`ProblemDetailScreen`** — এই
> ৩টাতেই standard `SomadhanPullToRefresh` + `SyncAwareRefreshableContent` +
> `isManualRefreshing` + (pagination-যুক্ত `PublicProfileReviewsScreen`-এ) `.toList()` snapshot
> checklist পুরোপুরি পাশ করেছে (grep-verified, কোনো পুরনো gate leftover নেই, bracket-balance
> zero-imbalance)। **real Gradle build তবুও এখনো কোনোটাতেই verify হয়নি (নিচে ১ নম্বর পয়েন্ট
> অপরিবর্তিত)।**
>
> **এখন বাকি মাত্র ১টা স্ক্রিন:**
> - **`HomeScreen`** — rule ১+২ (cold-load, re-entry flash-বিহীন, `SyncAwareContent`) ঠিক আছে,
>   কিন্তু rule ৩ (pull-to-refresh-এ নিচের problem-list-এ শিমার) আর rule ৪ (pagination শিমার)
>   এখনো যোগ হয়নি — এখনো migrate শুরু হয়নি।
> - ~~`JobTrackingScreen`~~ — **✅ ব্যাচ ২৭ শেষে সম্পূর্ণ**, ৬টা zone-ই migrate হয়ে গেছে (bid list,
>   status badge, stepper, other-party row, money card, action CTA — সব `PulsingValue`+
>   `rememberFieldChangePulse` প্যাটার্নে)।

1. **সবচেয়ে জরুরি — কখনোই real Gradle/Android Studio build এই কাজের কোনো ধাপেই verify হয়নি।**
   ব্যবহারকারীকে প্রথমেই বলা উচিত পুরো প্রজেক্ট build করে দেখতে। ব্যাচ ৬-এ edit হওয়া প্রতিটা
   ফাইলে ম্যানুয়ালি bracket/paren-balance (Python দিয়ে গোনা) মিলিয়ে যাচাই করা হয়েছে, কিন্তু
   real compile এখনো হয়নি।
2. ~~ক্যাটেগরি B1-এর ১০টা pagination স্ক্রিন migrate করা~~ — **✅ ব্যাচ ৫ শেষে সম্পূর্ণ**, B1 এখন খালি।
3. ~~`FavoriteSolversScreen.kt`-এর `isRefreshing` বাগ~~ — **✅ ব্যাচ ৬-এ ফিক্স হয়েছে** (এখন
   `viewModel.isRefreshing` shared StateFlow ব্যবহার করে, আগের dead local `remember` state বাদ)।
4. **`ratingsSyncPhase`** কখনো `LOADING`/`ERROR`-এ যায় না (স্টাব) — এখনো বাকি, এই ব্যাচে ছোঁয়া হয়নি।
5. **`HorizontalPager` + pull-to-refresh gesture-conflict** (`AllOpenProblemsScreen`,
   `InstantJobHistoryScreen`) ডিভাইসে টেস্ট করা বাকি।
6. **B2 migration approach ঠিক হয়ে গেছে (ব্যাচ ৬-এ ব্যবহারকারীর সিদ্ধান্তে):** নতুন কোনো
   কম্পোনেন্ট লাগেনি — বিদ্যমান `SyncAwareRefreshableContent<T>`-ই pagination ছাড়া ব্যবহার করা
   হচ্ছে (list-type/single-object উভয় ক্ষেত্রে, `data`-তে যা লাগে তাই: object/List/Pair/Triple/
   `listOf(...)`)। **এডিট-ফর্ম স্ক্রিন rule ২-এ migrate হবে না** — ইচ্ছাকৃত ব্যতিক্রম, দেখো
   ক্যাটেগরি E।
7. **পরের ব্যাচের লক্ষ্য — বাকি B2 (৪টা, `UserWalletScreen` ব্যাচ ৭-এ সম্পূর্ণ হয়ে গেছে):**
   - `DashboardScreen` migrate করতে হলে আগে `SolverDashboardContent`/`UserDashboardContent`
     sub-composable দুটোকে নিজে state কালেক্ট করার বদলে parameter নেওয়ার মতো refactor করা লাগবে
     কিনা — এটা ব্যবহারকারীর সাথে ঠিক করে নেওয়া উচিত (ঝুঁকিপূর্ণ ডেটা-ফ্লো পরিবর্তন, নাকি
     skip করে রাখা হবে)।
   - `HomeScreen` (২৮১০ লাইন) ও `AdminPanelScreen` (১৩টা ট্যাব) — বড়, নিজস্ব আলাদা ব্যাচ দরকার,
     সম্ভবত একাধিক সাব-ব্যাচে ভাগ করে (`AdminPanelScreen`-এর প্রতিটা ট্যাব আলাদাভাবে)।
   - `PublicProfileReviewsScreen`-এর দুই-স্তর প্যাটার্ন (bulk-pull + local per-user fetch)
     কীভাবে rule ২-এর সাথে মেলানো যায় তা নিয়ে সিদ্ধান্ত দরকার।

---

## পরিশিষ্ট — সংক্ষিপ্ত সেশন-ইতিহাস (রেফারেন্সের জন্য, নতুন সিদ্ধান্ত নেওয়ার সময় লাগতে পারে)

- **ধাপ ২:** `MotionToolkit.kt`-এ `SyncAwareRefreshableContent` কম্পোনেন্ট তৈরি (কোনো স্ক্রিন স্পর্শ হয়নি)।
- **ধাপ ৩:** পাইলট #১ `ActiveJobsPopupScreen` migrate।
- **ধাপ ৪:** পাইলট #২ `UserWalletScreen` — `rememberFieldChangePulse` তৈরি + balance/escrow ফিল্ডে বসানো।
- **ধাপ ৫:** "extra feature" সিদ্ধান্ত — ম্যানুয়াল pull-to-refresh শেষ হলে সবসময় pulse (`isManualRefreshing` প্যারামিটার)।
- **ধাপ ৬:** re-entry-কেও "রিফ্রেশ" হিসেবে গণ্য করার সিদ্ধান্ত (পথ B) — `flashOnReentry` প্যারামিটার।
- **ধাপ ৭ (ব্যাচ ১):** `MessagesScreen`-কে প্রথমে `SyncAwareRefreshableContent`-এ migrate করা হয়েছিল, পরে ব্যবহারকারীর স্পষ্ট নির্দেশে ফেরত `SyncAwareContent`-এ আনা হয় (ক্যাটেগরি D-এর ব্যতিক্রম এখান থেকেই)।
- **ধাপ ৭ (ব্যাচ ২):** Pagination প্রশ্নের সমাধান (Ground Rule ১১) + প্রথম ৩টা pagination স্ক্রিন (`AllOpenProblemsScreen`, `BidManagementScreen`, `FavoriteSolversScreen`) migrate।
- **ধাপ ৭ (ব্যাচ ৩-৪, এই ফাইলে আলাদা বিস্তারিত লগ নেই):** বাকি B1 স্ক্রিনগুলোর মধ্যে ৯টা (`InstantJobHistoryScreen`, `SolverAllPostsScreen`, `SolverCompletedJobsScreen`, `SolverMyBidsScreen`, `SolverProblemsScreen`, `SolverReviewsScreen`, `UserProblemsScreen`, `UserReviewsScreen`, `WithdrawalHistoryScreen`) migrate করা হয়েছিল।
- **ধাপ ৭ (ব্যাচ ৫):** B1-এর শেষ বাকি স্ক্রিন `TransactionHistoryScreen` migrate — দুইটা ট্যাব-নির্ভর আলাদা লিস্ট (APP/GATEWAY) থাকায় `data = Pair(filteredTransactions, sortedGatewayPayments)` প্যাটার্ন ব্যবহার করা হলো যাতে ট্যাব-সুইচে অযথা flash না হয়। বাকি ৯টা B1 স্ক্রিন `grep`-এ পুনরায় verify করা হলো (checklist অনুযায়ী সবগুলোই সম্পূর্ণ পাওয়া গেছে)। B1 ক্যাটেগরি এখন খালি।
- **ধাপ ৭ (ব্যাচ ৬):** সেশনের শুরুতে `grep` দিয়ে ব্যাচ ৫-এর সব দাবি পুনরায় verify করা হলো (সব মিলেছে)। তারপর: (ক) `FavoriteSolversScreen.kt`-এর pre-existing `isRefreshing` বাগ ফিক্স (dead local state → `viewModel.isRefreshing` shared StateFlow)। (খ) ব্যবহারকারীর স্পষ্ট সিদ্ধান্তে এডিট-ফর্ম স্ক্রিন (`SolverSkillsScreen`, `PostProblemScreen`, `UserInfoScreen`, `SolverKycScreen`, `SolverBalanceWithdrawScreen`, `UserWithdrawScreen`) rule ২-এ migrate না করে ইচ্ছাকৃত ব্যতিক্রম (ক্যাটেগরি E) হিসেবে রাখা হলো — active edit-এর মাঝে diff-শিমার ফর্ম বিঘ্নিত করতে পারে বলে। (গ) B2 থেকে ৪টা list/single-object স্ক্রিন `SyncAwareRefreshableContent`-এ migrate: `NotificationDetailScreen` (single nullable object), `FaqScreen` (raw plain list, ট্যাব/সার্চ ভেতরে থাকায় flash হয় না), `InstantJobsScreen` (`Triple`, বিড বটম-শিট ব্লকের বাইরে থাকায় অক্ষত), `ProfileScreen` (`listOf(currentUser, ...)` কম্পোজিট diff key, content lambda-র প্যারামিটার ignore করে বাইরের scope ব্যবহার)। প্রতিটা এডিটের পর bracket/paren-balance Python স্ক্রিপ্ট দিয়ে verify করা হয়েছে। `DashboardScreen`/`HomeScreen`/`AdminPanelScreen`/`PublicProfileReviewsScreen`/`UserWalletScreen` স্ট্রাকচারাল কারণে (sub-composable নিজে state আনে / আকার-জটিলতা / বিদ্যমান দুই-স্তর প্যাটার্ন / আংশিক field-pulse) এই ব্যাচে ছোঁয়া হয়নি, কারণ ও পরবর্তী পদক্ষেপ ধাপ ৫-এ লেখা আছে।
- **ধাপ ৭ (ব্যাচ ৭):** সেশনের শুরুতে `grep` দিয়ে ব্যাচ ৬-এর সব দাবি পুনরায় verify করা হলো (সব
  মিলেছে)। তারপর ব্যবহারকারীর সিদ্ধান্তে B2-এর `UserWalletScreen` full migrate করা হলো
  (`SyncAwareContent` + আংশিক `rememberFieldChangePulse` → পুরো `SyncAwareRefreshableContent`,
  `data = listOf(currentUser, userEscrows, userTransactions, userGatewayPayments, allProblems,
  allAdditionalCharges, minWithdrawalAmount)`, `isManualRefreshing = isRefreshing` যোগ, দুটো
  পুরনো field-pulse কল-সাইট plain `Text`-এ ফিরিয়ে আনা হলো)। এডিটের পর braces (`{`/`}`, string
  interpolation-সহ) একটা Kotlin-aware Python স্ক্রিপ্ট দিয়ে zero-imbalance ভেরিফাই করা হয়েছে।
  বাকি B2 (৪টা: `DashboardScreen`/`HomeScreen`/`AdminPanelScreen`/`PublicProfileReviewsScreen`)
  এই ব্যাচে ছোঁয়া হয়নি, কারণ ধাপ ৫-এ লেখা আছে।
- **ধাপ ৭ (ব্যাচ ৮):** ব্যবহারকারীর সিদ্ধান্তে ক্যাটেগরি C আর ইচ্ছাকৃত ব্যতিক্রম না ধরে প্রতিটা
  স্ক্রিনের জন্য নির্দিষ্ট target ডিজাইন লেখা হলো (উপরে ধাপ ৪-এ)। তারপর এই দফায় ২টা বাস্তবায়ন করা
  হলো: (ক) `SolverCategoryPostsScreen` — সবচেয়ে সরল হওয়ায় প্রথমে — ম্যানুয়াল
  `minimumSkeletonActive`/`isInitialLoading` গেট সরিয়ে পুরো standard `SyncAwareRefreshableContent`
  migrate (rule ১+২+৩+৪), এখন ক্যাটেগরি A-তে। (খ) `ChatScreen` — `MessagesScreen`-এর প্যাটার্ন
  কপি, early-return গেটের শর্তে `!viewModel.hasLoadedOnce(...)` যোগ করে "sessionKey একবার
  লোড হয়ে গেলে আর কখনো flash না" গ্যারান্টি সম্পূর্ণ করা হলো। দুটো এডিটের পরই brace/paren/bracket
  balance মূল আপলোডের (`somadhan-batch7-complete.zip`) সাথে তুলনা করে zero-imbalance ভেরিফাই করা
  হয়েছে, ফাইল-সংখ্যা (২৪৫/২৪৫) হুবহু মিলিয়ে দেখা হয়েছে। বাকি ৪টা ক্যাটেগরি C স্ক্রিন
  (`ReputationDetailScreen`, `JobTrackingScreen`, `ProblemDetailScreen`, `PublicProfileScreen`)
  এই ব্যাচে ছোঁয়া হয়নি।
- **ধাপ ৭ (ব্যাচ ৯):** `ChatScreen`-এ ব্যাচ ৮-এর "খোলা প্রশ্ন" ফিক্স করা হলো — `MessagesScreen`-এ
  sync-error হলে retry-বাটনসহ এরর-স্টেট আছে, কিন্তু `ChatScreen`-এ তা ছিল না। এখন
  `ReputationDetailScreen`/`JobTrackingScreen`-এর মতো একই `SyncBlockedRetryState` প্যাটার্ন যোগ
  হলো: `initialSyncPhase` কালেক্ট করে, cold-load early-return গেটের ভেতরেই
  `problem == null && initialSyncPhase == ERROR` হলে `SyncBlockedRetryState(onRetry =
  { viewModel.retryInitialSync() })` দেখায়, নাহলে আগের মতোই `ChatSkeleton`। rule ১-এর
  `hasLoadedOnce`/`minimumSkeletonActive` গেট-লজিক অপরিবর্তিত, বাকি স্ক্রিন অক্ষত (২টা import +
  একটা error-check ব্লক যোগ হয়েছে মাত্র)। এডিটের পর brace/paren/bracket balance আগের zip-এর
  সাথে তুলনা করে zero-imbalance ভেরিফাই করা হয়েছে (নতুন কোডে ১২টা `()` + ২টা `{}` যোগ, দুটোই
  balanced), ফাইল-সংখ্যা (২৪৫/২৪৫) হুবহু মিলেছে।
- **ধাপ ৭ (ব্যাচ ১০):** বাকি থাকা ৪টা B2 স্ক্রিনের (`DashboardScreen`, `HomeScreen`,
  `PublicProfileReviewsScreen`, `AdminPanelScreen`) target design ব্যবহারকারীর কাছ থেকে নেওয়া
  হলো এবং confirm হলো (উপরে B2 সেকশনে "প্রতিটা স্ক্রিনের target ডিজাইন" দ্রষ্টব্য) — এই ব্যাচে
  **কোনো কোড পরিবর্তন হয়নি**, শুধু ডিজাইন-সিদ্ধান্ত নথিভুক্ত হলো। সংক্ষেপে: `DashboardScreen`
  (user+solver) ও `PublicProfileReviewsScreen`-এ পুরো standard rule ১+২+৩(+৪); `HomeScreen`-এ
  rule ২ (re-entry) flash-বিহীন (`MessagesScreen`-এর মতো) কিন্তু rule ৩+৪ standard;
  `AdminPanelScreen`-এর ১৩টা ট্যাবের ১২টায় standard rule ১+২+৩(+৪), শুধু "পরিসংখ্যান ও চার্ট"
  (Overview, index ০, `admin_overview_sync`) ট্যাবে আলাদা ডিজাইন — re-entry-তে শুধু realtime-এ
  সত্যিই বদলানো value/card-টাই independent শিমার-জোন হিসেবে শিমার করবে (`JobTrackingScreen`-এর
  মতো), আর স্ক্রলের সাথে সাথে progressive/lazy শিমার-লোড হবে (একসাথে সব লোড হবে না)। খোলা প্রশ্ন
  (migrate শুরুর আগে নিশ্চিত করতে হবে): Overview ট্যাবে pull-to-refresh ঠিক কীভাবে চান তা
  ব্যবহারকারী স্পষ্ট করেননি।
- **ধাপ ৭ (ব্যাচ ১১-১৮):** এই ব্যাচগুলোর কোনো সেশন-লগ এই ফাইলে নেই (Ground Rule ১২-এর ব্যতিক্রম,
  সম্ভবত অন্য সেশনে কাজ হয়েছে কিন্তু এই single-source-of-truth ফাইল আপডেট হয়নি)। ব্যাচ ১৯-এর
  শুরুতে `grep`-এ verify করে শুধু এটুকু নিশ্চিত করা গেছে: `AdminPanelScreen` (আগে B2-তে "migrate
  এখনো বাকি" ছিল) এই মধ্যবর্তী কোনো এক ব্যাচে সম্পূর্ণ migrate হয়ে গেছে (১১টা
  `SyncAwareRefreshableContent` কল + `isManualRefreshing` wiring, ২টা ট্যাবে ইচ্ছাকৃতভাবে
  `SyncAwareContent` — Overview ট্যাবের আলাদা ডিজাইন অনুযায়ী)। বাকি স্ক্রিনগুলোতে (B2-এর ৩টা,
  C-এর ৪টা) কোনো পরিবর্তন পাওয়া যায়নি।
- **ধাপ ৭ (ব্যাচ ১৯, ২০২৬-০৯-১৪):** ব্যবহারকারীর সিদ্ধান্তে ক্যাটেগরি C দিয়ে আবার শুরু করা হলো।
  ৪টার মধ্যে `PublicProfileScreen` প্রথমে বাছাই করা হলো — কারণ এটার কোনো "খোলা প্রশ্ন" ছিল না
  (বাকি ৩টায় pull-to-refresh/pagination/zone-mapping নিয়ে অনিশ্চয়তা আছে, ব্যবহারকারীর কাছ থেকে
  নিশ্চিত করা দরকার) এবং Ground Rule ৪ (ছোট, independently-testable ধাপ) অনুযায়ী সবচেয়ে সহজ।
  বিস্তারিত ফিক্স উপরে "প্রতিটা স্ক্রিনের target ডিজাইন" সেকশনে `৪. PublicProfileScreen`-এর নিচে
  লেখা আছে (মূল কারণ: keyed না-হওয়া `remember`-এর কারণে re-entry-তে এক-ফ্রেমের null-window,
  ফিক্স: `remember(userId)` + সিঙ্ক্রোনাস `initialFoundUser` দিয়ে initial state)। এডিটের পর
  brace/paren/bracket balance zero-imbalance (Python স্ক্রিপ্টে) ভেরিফাই হয়েছে, ফাইল-সংখ্যা
  (২৪৫/২৪৫, dotfile সহ) `somadhan-batch18-adminpanel-complete.zip`-এর সাথে হুবহু মিলিয়ে দেখা
  হয়েছে। বাকি ৩টা ক্যাটেগরি C স্ক্রিন (`ReputationDetailScreen`, `JobTrackingScreen`,
  `ProblemDetailScreen`) এই ব্যাচে ছোঁয়া হয়নি — এদের "খোলা প্রশ্ন" (ধাপ ৪-এর শেষে) migrate শুরুর
  আগে ব্যবহারকারীর কাছ থেকে নিশ্চিত করে নিতে হবে।
- **ধাপ ৭ (ব্যাচ ২০-২৩, তারিখ/বিস্তারিত অজানা — এই ফাইলে কোনো সেশন-লগ ছিল না, Ground Rule ১২-এর
  ব্যতিক্রম):** ব্যাচ ২৪-এর শুরুতে `grep` verify করে শুধু এটুকু নিশ্চিত করা গেছে —
  `DashboardScreen`, `PublicProfileReviewsScreen`, `ReputationDetailScreen`, `ProblemDetailScreen`
  এই ৪টা স্ক্রিন কোনো এক মধ্যবর্তী ব্যাচে পূর্ণাঙ্গ `SyncAwareRefreshableContent` migrate হয়ে
  গেছে (checklist অনুযায়ী সবগুলোই pass)। কীভাবে/কোন সিদ্ধান্তে করা হয়েছিল তার বিস্তারিত narrative
  এই ফাইলে নেই।
- **ধাপ ৭ (ব্যাচ ২৪, ২০২৬-০৯-১৪):** সেশনের শুরুতে `grep`-এ ব্যাচ ১৯-পরবর্তী অবস্থা recheck করে
  উপরের ব্যাচ ২০-২৩ গ্যাপ আবিষ্কার হলো (`somadhan-batch23-dashboardscreen-complete.zip` আপলোড
  থেকে)। তারপর ব্যবহারকারীর সিদ্ধান্তে বাকি ২টা স্ক্রিনের (`HomeScreen`, `JobTrackingScreen`)
  মধ্যে `JobTrackingScreen` দিয়ে শুরু হলো। প্রথমে পুরো ফাইল (৫৮২৯ লাইন) ম্যাপ করে zone-ভাঙন
  প্রস্তাব করা হলো (bid-list zone + JobTrackingBottomSheet-এর ৫টা numbered zone: status badge,
  stepper, other-party row, money card, action CTA) — ব্যবহারকারী confirm করলেন যে প্রতিটা
  dynamic zone-এই "professional/Uber-স্টাইল" আচরণ লাগবে: হঠাৎ পপ-আপ না করে হালকা শিমার/pulse
  দিয়ে smooth-এ লোড হবে, বাকি পেজের কাঠামো স্থির থাকবে (উদাহরণ: solver-এর extra bill request)।
  **টেকনিক্যাল সিদ্ধান্ত (কোডে verify করে নেওয়া):** এই nested zone-গুলোর জন্য
  `SyncAwareRefreshableContent` ব্যবহার না করে **`PulsingValue` + `rememberFieldChangePulse`**
  ব্যবহার করা হচ্ছে — কারণ `MotionToolkit.kt`-এ (লাইন ৪৯৩-৬৪৪) সরাসরি কোড পড়ে নিশ্চিত হওয়া গেছে
  `rememberFieldChangePulse`-এর নিজস্ব কোনো cold-skeleton গেট নেই (শুধু আগের/নতুন value-এর
  Kotlin `!=` তুলনায় pulse করে), যেখানে `SyncAwareRefreshableContent`/`SyncAwareContent` দুটোই
  ভেতরে `rememberSessionAwareSkeletonGate` কল করে — নতুন sessionKey দিলে প্রথমবার নিজস্ব একটা
  cold-skeleton flash দেখাতো, যেটা বাইরের rule ১ (এতক্ষণে already pass হয়ে যাওয়া) থেকে আলাদা
  একটা দ্বিতীয় flash তৈরি করতো — এই ঝুঁকিটাই আগের ব্যাচগুলোতে "তাত্ত্বিকভাবে সম্ভব" বলে খোলা
  প্রশ্ন হিসেবে রাখা হয়েছিল, এই ব্যাচে সোর্স পড়ে সমাধান হলো। **`PulsingValue`-এর docstring
  (লাইন ৪৮২-৪৯০)-এ নিজেই লেখা আছে এটা ঠিক এই ব্যবহারের জন্যই বানানো** ("Uber's live fare/ETA,
  a bank app's balance refresh")।
  **zone ১/৬ সম্পূর্ণ — bid list (`UserJobBroadcastingScreen`, "BIDS SECTION (PHASE C)"
  কমেন্ট থেকে শুরু):** পুরো if(empty)/else(বিড-লিস্ট+অ্যাকশন বাটন) ব্লকটা
  `PulsingValue(isUpdating = rememberFieldChangePulse(value = sortedBids, sessionKey =
  "job_tracking_${problem.id}", viewModel = viewModel)) { Column { ... } }`-এ মোড়ানো হলো।
  `sessionKey` ইচ্ছাকৃতভাবে বাইরের `JobTrackingScreen`-এর cold-load gate-এর সাথেই মেলানো হয়েছে
  (যেটা ইতিমধ্যে `markLoadedOnce` হয়ে গেছে) — তাই কোনো নতুন/আলাদা flash হবে না। `sortedBids`
  (data class `BidEntity`-র `remember{}`-করা `List`) বদলালেই (নতুন বিড/বিড সরে যাওয়া/
  empty↔non-empty) পুরো zone-টা এক সাথে pulse করে আপডেট দেখাবে, countdown timer card/job
  summary card/top bar/radar background অস্পৃষ্ট থাকবে। ২টা import (`PulsingValue`,
  `rememberFieldChangePulse`) যোগ হলো। এডিটের পর brace/paren/bracket balance zero-imbalance
  ভেরিফাই হয়েছে (Python স্ক্রিপ্টে), ফাইল-সংখ্যা (২৪৫/২৪৫, dotfile-সহ, আগেরবার ভুলে `.env`
  বাদ পড়েছিল প্রথম zip-এ — সেটা ধরা পড়ে দ্বিতীয়বারে ঠিক করা হয়েছে) `somadhan-batch23-...zip`-এর
  সাথে হুবহু মিলিয়ে দেখা হয়েছে, ফাইল-লিস্ট diff করেও কনফার্ম করা হয়েছে (কোনো ফাইল যোগ/বাদ/rename
  হয়নি)। **বাকি ৫টা zone** (JobTrackingBottomSheet-এর status badge/stepper/other-party/money-card/
  action-CTA) এই ব্যাচে ছোঁয়া হয়নি — পরের ধাপ, একই `PulsingValue`+`rememberFieldChangePulse`
  প্যাটার্নে, প্রতিটা নিজস্ব ছোট value নিয়ে আলাদাভাবে। ⚠️ real Gradle/Android Studio build এই
  ফিক্সেও verify হয়নি — শুধু ম্যানুয়াল bracket-balance আর লজিক-রিভিউ (Ground Rule ১ অনুযায়ী এটা
  বারবার মনে করিয়ে দেওয়া বাধ্যতামূলক)।
- **ধাপ ৭ (ব্যাচ ২৫, ২০২৬-০৯-১৪):** ব্যবহারকারীর "continue করো" নির্দেশে ব্যাচ ২৪-এ বাকি রাখা
  `JobTrackingBottomSheet`-এর ৫টা zone-ই এই ব্যাচে বাস্তবায়ন করা হলো (zone ২/৬ থেকে ৬/৬): ২.
  স্ট্যাটাস ব্যাজ + ডিসপিউট বাটন, ৩. ৪-ধাপ স্টেপার, ৪. other-party প্রোফাইল রো, ৫. মানি/এসক্রো
  কার্ড (+ pending-extra নোটিশ)। প্রতিটাই ব্যাচ ২৪-এর bid-zone-এর সাথে অভিন্ন প্যাটার্নে:
  `PulsingValue(isUpdating = rememberFieldChangePulse(value = ..., sessionKey =
  "job_tracking_${problem.id}", viewModel = viewModel)) { ... }`, `sessionKey` ইচ্ছাকৃতভাবে
  বাইরের `JobTrackingScreen`-এর cold-load gate-এর সাথে মিলিয়ে (আগে থেকেই `markLoadedOnce`),
  তাই কোনো নতুন/আলাদা cold-skeleton flash হয়নি। প্রতিটা zone-এ শুধু সেই zone-এর dynamic
  value(গুলো) `value` প্যারামিটারে দেওয়া হলো (স্ট্যাটাস-ব্যাজে `status`+`isDisputed`+
  `disputeSettledAt`; স্টেপারে `currentStepIdx`+ওই দুটো; other-party রো-তে `displayName`+
  `metaText`+`unreadChatCount`; money-card-এ `totalGross`/`netSolverAmount`/`confirmedExtra`/
  `pendingExtra`/`wasFreeQuota`-এর `listOf`)। money-card zone-এ `commissionBreakdown` fetch
  করা `LaunchedEffect` ইচ্ছাকৃতভাবে wrapper-এর *বাইরে* রাখা হয়েছে (business-logic side-effect,
  visual zone না — Ground Rule ১)। এর ফলে এখন `JobTrackingBottomSheet`-এর ৫টা zone-ই (৬টার
  মধ্যে বাকি ছিল) migrate সম্পূর্ণ, মিলিয়ে zone ১-৬ (bid-list + এই ৫টা) সব `PulsingValue`+
  `rememberFieldChangePulse` প্যাটার্নে। এডিটের পর brace/paren/bracket balance একটা
  Kotlin-aware (string/string-template/comment-aware) Python স্ক্রিপ্টে zero-imbalance
  ভেরিফাই হয়েছে (`{`:৮৪১/৮৪১, `(`:২৩৫৭/২৩৫৭, `[`:৩/৩)। ফাইল-সংখ্যা (২৪৫/২৪৫, dotfile-সহ,
  `.env` সহ — ব্যাচ ২৪-এর নোট অনুযায়ী ফাইল-লিস্ট ডিফ করে explicit ভেরিফাই করা হয়েছে যাতে আবার
  বাদ না পড়ে) `somadhan-batch24-jobtracking-step1-bidzone.zip`-এর সাথে হুবহু মিলেছে, কোনো
  ফাইল যোগ/বাদ/rename হয়নি। ⚠️ real Gradle/Android Studio build এই ফিক্সেও verify হয়নি —
  শুধু ম্যানুয়াল bracket-balance আর লজিক-রিভিউ। `JobTrackingScreen`-এর ৬টার মধ্যে ৫টা zone শেষ
  হয়েছে — বাকি আছে zone ৬ (action CTA বাটন, সবচেয়ে জটিল, আলাদা ব্যাচে করা হবে Ground Rule ৪
  অনুযায়ী) এবং `HomeScreen` (rule ২ re-entry flash-বিহীন ডিজাইন আগে থেকেই confirm করা আছে,
  ব্যাচ ১০-এর নোট দ্রষ্টব্য) — এই দুইটাই এখন পুরো প্রজেক্টের বাকি কাজ।
- **ধাপ ৭ (ব্যাচ ২৬, ২০২৬-০৯-১৪):** এই ব্যাচে **কোনো কোড পরিবর্তন হয়নি**, শুধু `JobTrackingScreen`
  zone ৬/৬ (action CTA বাটন)-এর target ডিজাইন ব্যবহারকারীর সাথে আলোচনা করে confirm করা হলো (ব্যাচ
  ১০-এ অন্য স্ক্রিনগুলোর design-শুধু-doc-করার প্যাটার্নের মতোই)। সিদ্ধান্ত: বাকি ৫টা zone-এর মতো
  আলাদা আলাদা leaf-value pulse না করে, পুরো CTA if/when ব্লকটাকে একটা single `PulsingValue`-তে
  মোড়ানো হবে, `value = listOf(status, hasPendingExtra, hasReleaseReq, isDisputeActive)` (submit-
  লোডিং ফ্ল্যাগ বাদে, কারণ ওগুলো নিজেরাই spinner দেখায়) — কারণ এই zone-এ state বদলালে পুরো
  বাটন-সেট/কার্ড-structure-ই বদলে যায় (leaf-value pulse যথেষ্ট না)। বিস্তারিত কোড-প্যাটার্ন + কেন
  এভাবে উপরে "খোলা প্রশ্ন" সেকশনে `JobTrackingScreen` bullet-এ লেখা হলো, যাতে পরের সেশনে শুধু এই
  ফাইল + zip দিলেই সরাসরি বাস্তবায়ন শুরু করা যায় — migrate শুরুর আগে শুধু ফাইলে exact current
  line-number/brace-boundary যাচাই করে নিতে হবে (Ground Rule ১৩)।
- **ধাপ ৭ (ব্যাচ ২৭, ২০২৬-০৯-১৪):** ব্যবহারকারীর নির্দেশে ব্যাচ ২৬-এ কনফার্ম-করা zone ৬ (action CTA
  বাটন) ডিজাইন এই ব্যাচে বাস্তবায়ন করা হলো। শুরুর আগে (Ground Rule ১৩ অনুযায়ী) `grep` দিয়ে ফাইলে
  exact current line-number/brace-boundary যাচাই করা হলো — `// 5. Role-Specific Action CTA
  Buttons` কমেন্ট লাইন ২২৫৬-এ পাওয়া গেল (ব্যাচ ২৬-এর নোটে অনুমান করা ~২২৫৬-এর সাথে মিলে গেছে, কোনো
  শিফট হয়নি), if/else-if/else ব্লকটা লাইন ২২৫৭ থেকে ২৭৮৯ পর্যন্ত (২৭৮৯-এর `}` বাইরের `else`-এর
  ক্লোজিং ব্রেস, তার পরের লাইনগুলো (২৭৯০-২৭৯৩) `expandedContent` lambda/Column/composable-এর
  বিদ্যমান ক্লোজিং — সেগুলো অক্ষত রাখা হয়েছে)। পুরো ব্লকটা `PulsingValue(isUpdating =
  rememberFieldChangePulse(value = listOf(status, hasPendingExtra, ctaHasReleaseReq,
  ctaIsDisputeActive), sessionKey = "job_tracking_${problem.id}", viewModel = viewModel)) { ... }`-এ
  মোড়ানো হলো। `ctaHasReleaseReq`/`ctaIsDisputeActive` নামে **দুটো নতুন local val** যোগ হয়েছে
  (wrapper-এর ঠিক আগে, `problem.hasReleaseRequest` / `problem.isDisputed &&
  problem.disputeSettledAt == null`) — এগুলো শুধু pulse-trigger value গণনার জন্য, if/when ব্লকের
  ভেতরের বিদ্যমান local val `hasReleaseReq`/`isDisputeActive` (সোলভার শাখায় লাইন ~২৪৪৬,
  ইউজার শাখায় ~২৬১৫; `isDisputeActive` সোলভার শাখায় ~২৩৩৯, ইউজার শাখায় ~২৫৪৮) ইচ্ছাকৃতভাবে
  অপরিবর্তিত/অস্পৃষ্ট রাখা হয়েছে (Ground Rule ১ — বিদ্যমান বিজনেস লজিক স্পর্শ না করে শুধু visual
  pulse-wrapper যোগ করা, ডাবল-কম্পিউটেশন হলেও রিস্ক-ফ্রি)। সাবমিট-লোডিং ফ্ল্যাগ
  (`isSubmittingSolverArrived`/`isSubmittingSolverStartJob`/`isRequestingRelease`/
  `isRejectingExtraAmount`) ইচ্ছাকৃতভাবে `value`-তে বাদ দেওয়া হয়েছে (ব্যাচ ২৬-এর ডিজাইন-সিদ্ধান্ত
  অনুযায়ী, ডাবল-ফিডব্যাক এড়াতে)। `sessionKey` আগের ৫টা zone-এর মতোই বাইরের cold-load gate-এর
  সাথে মেলানো (আগে থেকেই `markLoadedOnce`), তাই নতুন/আলাদা কোনো cold-skeleton flash হয়নি। এডিটের
  পর একটা Kotlin-aware (string/string-template/comment-aware) Python স্ক্রিপ্টে brace/paren/bracket
  balance zero-imbalance ভেরিফাই হয়েছে (`{`:৮৪২/৮৪২, আগের ব্যাচে ছিল ৮৪১/৮৪১; `(`:২৩৬০/২৩৬০, আগে
  ২৩৫৭/২৩৫৭; `[`:৩/৩ অপরিবর্তিত — যোগ হওয়া ১টা `PulsingValue{...}` ব্লক + ৩টা নতুন প্যারেন যথাযথ)।
  ফাইল-সংখ্যা (২৪৫/২৪৫, dotfile-সহ) `somadhan-batch26-jobtracking-zone6-design-confirmed.zip`-এর
  সাথে ফাইল-লিস্ট diff করে হুবহু মিলিয়ে দেখা হয়েছে, কোনো ফাইল যোগ/বাদ/rename হয়নি। এর ফলে এখন
  `JobTrackingScreen`-এর **৬টা zone-ই সম্পূর্ণ** — `JobTrackingScreen`-এর সব কাজ শেষ, বাকি আছে শুধু
  `HomeScreen` (rule ৩+৪, target design ব্যাচ ১০-এ আগে থেকেই কনফার্ম)। ⚠️ real Gradle/Android
  Studio build এই ফিক্সেও verify হয়নি — শুধু ম্যানুয়াল bracket-balance আর লজিক-রিভিউ (Ground Rule
  ২ অনুযায়ী এটা বারবার মনে করিয়ে দেওয়া বাধ্যতামূলক)।
- **ধাপ ৭ (ব্যাচ ২৯, ২০২৬-০৯-১৪):** ব্যাচ ২৮-এ কনফার্ম-করা zone ৬ crossfade আপগ্রেড ডিজাইন এই
  ব্যাচে বাস্তবায়ন করা হলো। শুরুর আগে (Ground Rule ১৩) `grep` দিয়ে ফাইলে exact current
  line-number যাচাই করা হলো — `// 5. Role-Specific Action CTA Buttons` কমেন্ট লাইন ২২৫৬-এ পাওয়া
  গেল (ব্যাচ ২৮-এর নোটে অনুমান করা ~২২৫৬-এর সাথে মিলে গেছে, কোনো শিফট হয়নি), if/else-if/else
  ব্লক লাইন ২৮১০-এ শেষ হয় (আগের `PulsingValue` লাইন ২৮১১-এ ক্লোজ হতো)। ঠিক ব্যাচ ২৮-এ লেখা
  কোড-প্যাটার্ন অনুযায়ী বাস্তবায়ন হলো: (১) top-level-এ `private data class JobTrackingCtaKey`
  যোগ হলো (`TrackingStepNode`-এর ঠিক আগে), (২) `PulsingValue(isUpdating = rememberFieldChangePulse(...))`
  কলের বদলে `val ctaKey = JobTrackingCtaKey(...)` + `AnimatedContent(targetState = ctaKey,
  transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, contentAlignment =
  Alignment.TopStart, label = "job_tracking_cta_crossfade") { ctaTargetKey -> ... }` বসানো হলো,
  (৩) content lambda-র ভেতরে `status`/`hasPendingExtra`-কে `ctaTargetKey` থেকে shadow করা হলো
  (Compose exit-content-freeze gotcha এড়াতে, ব্যাচ ২৮-এর ডকুমেন্টেড কারণেই), if/when ব্লকের
  ভেতরের বাকি সব কোড (নেস্টেড `hasReleaseReq`/`isDisputeActive` local val সহ) অপরিবর্তিত রাখা
  হয়েছে (Ground Rule ১)। `androidx.compose.animation.AnimatedContent` ও
  `androidx.compose.animation.togetherWith` — এই ২টা নতুন import যোগ হলো (`fadeIn`/`fadeOut`/
  `tween` আগে থেকেই import করা ছিল, ব্যাচ ২৮-এর অনুমান সঠিক প্রমাণিত)। এডিটের পর একটা
  Kotlin-aware (string/string-template/comment-aware) Python স্ক্রিপ্টে brace/paren/bracket
  balance zero-imbalance ভেরিফাই হয়েছে (`{`:৮৪৩/৮৪৩, আগের ব্যাচে ছিল ৮৪২/৮৪২; `(`:২৩৬৪/২৩৬৪, আগে
  ২৩৬০/২৩৬০; `[`:৩/৩ অপরিবর্তিত)। ফাইল-সংখ্যা (২৪৫/২৪৫, dotfile-সহ) input zip-এর সাথে ফাইল-লিস্ট
  diff করে হুবহু মিলিয়ে দেখা হয়েছে, কোনো ফাইল যোগ/বাদ/rename হয়নি। এর ফলে এখন `JobTrackingScreen`-এর
  **৬টা zone-ই সম্পূর্ণ, zone ৬-এ true crossfade-সহ** — `JobTrackingScreen`-এর সব কাজ (কার্যকরী +
  ঐচ্ছিক পলিশ) শেষ। বাকি আছে শুধু `HomeScreen` (rule ৩+৪, target design ব্যাচ ১০-এ আগে থেকেই
  কনফার্ম)। ⚠️ real Gradle/Android Studio build এই ফিক্সেও verify হয়নি — শুধু ম্যানুয়াল
  bracket-balance আর লজিক-রিভিউ (Ground Rule ২ অনুযায়ী এটা বারবার মনে করিয়ে দেওয়া বাধ্যতামূলক)।
- **(আলাদা roadmap থেকে, একত্রিত করা হয়েছে):** cold-load skeleton gate, `SomadhanPullToRefresh` কম্পোনেন্ট, connectivity-ফিরলে automatic retry, dead-code cleanup (`SessionAwareLoadingContent`/`rememberAdminTabReady` মোছা) — এগুলোর মূল ডিজাইন-ডকুমেন্ট (`somadhan-loading-fix-roadmap-v2.md`) হারিয়ে গেছে/zip-এ ছিল না, তাই এই ফাইলের ধাপ ১ (বিল্ডিং ব্লক) সেকশনে কোড থেকে reverse-engineer করে লেখা হয়েছে যাতে আর কখনো এই তথ্য হারিয়ে না যায়।
