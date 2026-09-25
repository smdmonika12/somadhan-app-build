# Realtime-Aware, Structure-Preserving Refresh — Master Prompt / Roadmap

এই ডকুমেন্ট একটা ধাপে-ধাপে (step-by-step) execution plan — যেকোনো session/AI agent এই প্রম্পট
অনুসরণ করে কাজটা নিরাপদে, ছোট-ছোট verifiable ধাপে করতে পারবে। প্রতিটা ধাপ নিজে থেকেই
সম্পূর্ণ/compile-হওয়া/zip-করা অবস্থায় শেষ হবে, যাতে কোনো ধাপে সমস্যা হলে ঠিক কোন ধাপে ভাঙলো তা
সহজে বোঝা যায় (bisectable)।

**লক্ষ্য (Goal):** কোনো স্ক্রিনে প্রথমবার ঢুকলে এখনকার মতোই পুরো পেজ skeleton দিয়ে লোড হবে। কিন্তু
সেই স্ক্রিন থেকে বেরিয়ে আবার ঢুকলে বা স্ক্রিন খোলা অবস্থায় realtime-এ যদি ডেটা বদলে/নতুন যোগ
হয়ে থাকে, তাহলে পুরো পেজ আর re-skeleton হবে না — পেজের কাঠামো (header/filter/tab/layout)
অপরিবর্তিত/স্থির থেকে শুধু লিস্ট/ভ্যালুর জায়গাটুকু সংক্ষিপ্ত সময়ের জন্য shimmer করে নতুন ডেটা
নিয়ে আসবে। ডেটা অপরিবর্তিত থাকলে কোনো flash/shimmer একদমই হবে না।

---

## ধাপ ০ — Non-negotiable Ground Rules (প্রতিটা ধাপেই বাধ্যতামূলক)

এই নিয়মগুলো পুরো কাজ জুড়ে (প্রতিটা ধাপে, ছোট হোক বা বড়) মেনে চলতে হবে। কোনো ধাপ এই নিয়ম
ভেঙে "শেষ" গণ্য হবে না।

1. **অ্যাপের কোনো বিদ্যমান ফাংশনালিটি নষ্ট করা যাবে না।** কোনো স্ক্রিনের ব্যবসায়িক লজিক
   (bid/escrow/payment/notification ইত্যাদি), navigation, permission/RLS-নির্ভর আচরণ,
   pull-to-refresh, retry/error flow — কোনোটাই এই কাজের ফলে বদলাবে না। শুধু *কীভাবে
   skeleton/content দেখানো হয়* সেটাই বদলাবে, *কী দেখানো হয়* তা না।
2. **প্রতিটা ধাপ শেষে প্রজেক্টটা সম্পূর্ণ, কম্পাইলযোগ্য অবস্থায় থাকতে হবে** — আধা-সম্পন্ন/ভাঙা
   অবস্থায় কোনো ধাপ ছেড়ে দেওয়া যাবে না। Gradle বিল্ড এই পরিবেশে চালানো সম্ভব না হলে, প্রতিটা
   এডিট করা ফাইল ম্যানুয়ালি পড়ে ব্র্যাকেট/সিনট্যাক্স ব্যালেন্স, ইম্পোর্ট, প্যারামিটার সিগনেচার
   মিলিয়ে যাচাই করতে হবে।
3. **Zip ডেলিভারির নিয়ম (প্রতিটা ধাপের শেষে বাধ্যতামূলক):**
   - পুরো প্রজেক্টের zip দিতে হবে (শুধু বদলানো ফাইল না) — user সবসময় একটা সম্পূর্ণ, চালানোর
     উপযোগী প্রজেক্ট পাবে।
   - **কোনো ফাইল miss করা যাবে না** — dotfile সহ (`.env`, `.env.example`, `.gitignore` ইত্যাদি)।
   - প্রতিটা zip বানানোর পর **আগের zip-এর ফাইল-লিস্টের সাথে diff করে যাচাই করতে হবে** যে
     ফাইল-সংখ্যা ও নাম হুবহু মিলছে (নতুন ইচ্ছাকৃত ফাইল ছাড়া কিছু বাদ পড়েনি)। `unzip -l` দিয়ে
     দুই লিস্ট বের করে `diff`/`wc -l` দিয়ে confirm করা।
   - zip-এর ভেতরের ফোল্ডার স্ট্রাকচার original zip-এর মতোই ফ্ল্যাট (কোনো wrapper folder ছাড়া)
     রাখতে হবে, যেমনটা এখন আছে।
4. **ছোট, independently-testable ধাপে কাজ ভাগ করতে হবে।** একসাথে ৪২টা কল-সাইট বা ৩০টা ফাইল
   এক ধাপে বদলানো যাবে না — এতে কিছু ভাঙলে কোথায় ভাঙলো বোঝা কঠিন হয়ে যায়। প্রতিটা ধাপে
   সর্বোচ্চ কয়েকটা ফাইল/স্ক্রিন বদলাবে।
5. **আগের ফিক্সগুলো (session-aware skeleton gate) revert/override করা যাবে না** — নতুন কাজ
   তার *উপরে* যোগ হবে, প্রতিস্থাপন না। "প্রথম ভিজিটে পুরো পেজ skeleton, তারপর আর flash না"
   এই আচরণ অক্ষত থাকবে; এই নতুন কাজ শুধু "যদি ডেটা সত্যিই বদলে থাকে" কেসটা handle করবে।
6. **"খালি" আর "এখনো লোড হয়নি" গুলিয়ে ফেলা যাবে না।** কোনো লিস্ট সত্যিই খালি হওয়া (যেমন "কোনো
   কাজ নেই") একটা বৈধ অবস্থা — সেটাকে কখনো "ডেটা রেডি না" ধরে নিয়ে চিরকাল skeleton দেখানো
   যাবে না।
7. **বিদ্যমান কোড-স্টাইল মেনে চলতে হবে** — ফাইলে ইতিমধ্যে থাকা বাংলা-ইংরেজি মিশ্র মন্তব্যের
   ধরন, "কেন এই সিদ্ধান্ত" ব্যাখ্যা করার অভ্যাস, এবং comment-এ ধাপের রেফারেন্স
   ("Loading/Sync Fix Roadmap v2, ধাপ ৯" ইত্যাদি) বজায় রাখা।
8. **অপ্রাসঙ্গিক ফাইলে হাত দেওয়া যাবে না** — `supabase/migrations/*`, `.env*`,
   `gradle*`, build config — এসব এই কাজের আওতার বাইরে, স্পর্শ করা যাবে না।
9. **প্রতিটা ধাপের শুরুতে সংক্ষেপে বলতে হবে** এই ধাপে ঠিক কোন ফাইল(গুলো) বদলাচ্ছে এবং কেন,
   আর ধাপের শেষে সংক্ষেপে বলতে হবে কী কী পরীক্ষা করে নিশ্চিত হওয়া হয়েছে।
10. **কোনো ধাপে সন্দেহ/দ্বিধা থাকলে (যেমন কোন ধরনের ডেটার জন্য কী ইকুয়ালিটি-চেক ব্যবহার হবে)
    — অনুমান করে এগিয়ে না গিয়ে ব্যবহারকারীকে জিজ্ঞেস করে নিশ্চিত হওয়া, বিশেষত ধাপ ২-এর আগে।**
11. **⚠️ Pagination-যুক্ত স্ক্রিন নিয়ে চূড়ান্ত সিদ্ধান্ত (ব্যবহারকারীর স্পষ্ট নির্দেশ, ধাপ ৭
    ব্যাচ ২-তে নেওয়া হয়েছে — এই নিয়মটা মিস করা যাবে না):** `mutableStateListOf`-ভিত্তিক
    pagination/"load more" থাকা স্ক্রিনগুলোতেও (তালিকা: `AllOpenProblemsScreen`,
    `BidManagementScreen`, `FavoriteSolversScreen` — এই ৩টা ইতিমধ্যে migrate হয়ে গেছে;
    বাকি `InstantJobHistoryScreen`, `SolverAllPostsScreen`, `SolverCompletedJobsScreen`,
    `SolverMyBidsScreen`, `SolverProblemsScreen`, `SolverReviewsScreen`,
    `TransactionHistoryScreen`, `UserProblemsScreen`, `UserReviewsScreen`,
    `WithdrawalHistoryScreen` — এই ১০টা বাকি) **অন্য সব স্ক্রিনের মতোই একই standard
    `SyncAwareRefreshableContent` প্যাটার্ন ব্যবহার করতে হবে, কোনো ব্যতিক্রম/বিশেষ
    suppression-লজিক ছাড়াই।** ব্যবহারকারীর নিজের ভাষায়: "user/solver/admin pagination-এ
    next/prev click করে, scroll করে load হলে, অথবা 'see more'-এ click করলে — পরের
    list/value শিমার করে (loading-এর মতো) open হবে, এটাই একটা professional system।"
    অর্থাৎ **আগে ভাবা "Path C" (migrate না করা) আর "Path D" (`isLoadingMore` দিয়ে
    flash suppress করা) — দুটোর কোনোটাই নেওয়া হয়নি, দুটোই বাতিল।** "Load more" এর ফলে
    তালিকা বদলে গেলে সংক্ষিপ্ত শিমার হওয়াটাই **কাঙ্ক্ষিত** আচরণ, অবাঞ্ছিত না — তাই কোনো নতুন
    `isLoadingMore`-জাতীয় প্যারামিটার তৈরি/যোগ করার দরকার নেই। শুধু মনে রাখতে হবে:
    `mutableStateListOf`-ভিত্তিক ডেটা সরাসরি `data` প্যারামিটারে পাস করলে (একই mutable
    reference থাকায়) `!=` diff কাজ নাও করতে পারে — তাই `.toList()` দিয়ে immutable snapshot
    বানিয়ে পাস করতে হবে (ব্যতিক্রম: ডেটা ইতিমধ্যেই `remember { ... }`/`.filter{}`-এর মতো
    plain immutable list হলে `.toList()` লাগবে না, যেমন `BidManagementScreen`/
    `FavoriteSolversScreen`-এ হয়েছে)। বিস্তারিত reasoning ও কোড-লেভেল উদাহরণ নিচে
    "ধাপ ৭ (ব্যাচ ২)" এন্ট্রিতে আছে।

---

## ধাপ ১ (সম্পন্ন) — চূড়ান্ত সিদ্ধান্তসমূহ

- **নতুন কম্পোনেন্টের নাম:** `SyncAwareRefreshableContent<T>` — পুরনো `SyncAwareContent` অক্ষত
  থাকবে, পাশাপাশি এটা যোগ হবে।
- **Equality check:** সাধারণ Kotlin `==` (data class-এ structural equality এমনিতেই কাজ করে)।
- **Flash/shimmer duration:** ~350ms ডিফল্ট, প্যারামিটার দিয়ে override করা যাবে।
- **Single-value/object স্ক্রিনে granularity: (খ) — প্রতিটা আলাদা ফিল্ডকে আলাদা `PulsingValue`
  দিয়ে wrap করা হবে**, পুরো কার্ড/অবজেক্ট একসাথে না। শুধু যে ফিল্ডটা সত্যিই বদলেছে সেটাই পালস
  করবে, বাকি ফিল্ড স্থির থাকবে — এটা বেশি ধাপ/কল-সাইট লাগবে প্রতি স্ক্রিনে কিন্তু বেশি
  professional দেখাবে।
- **স্ক্রিন classification:**
  - *List-shaped* (নতুন `SyncAwareRefreshableContent<T>`-এ যাবে): ActiveJobsPopupScreen,
    AllOpenProblemsScreen, BidManagementScreen, FavoriteSolversScreen,
    InstantJobHistoryScreen, InstantJobsScreen, MessagesScreen, SolverAllPostsScreen,
    SolverCompletedJobsScreen, SolverMyBidsScreen, SolverProblemsScreen, SolverReviewsScreen,
    TransactionHistoryScreen, UserProblemsScreen, UserReviewsScreen, WithdrawalHistoryScreen
  - *Single value/object/form* (per-field `PulsingValue`): DashboardScreen, FaqScreen,
    HomeScreen, NotificationDetailScreen, PostProblemScreen, ProfileScreen,
    PublicProfileReviewsScreen, SolverBalanceWithdrawScreen, SolverKycScreen,
    SolverSkillsScreen, UserInfoScreen, UserWalletScreen, UserWithdrawScreen
  - *জটিল/মিশ্র, নিজস্ব ব্যাচ*: AdminPanelScreen (১১টা কল-সাইট, ট্যাব-ভিত্তিক ভাগ হবে)

---

## ধাপ ১ (মূল টেমপ্লেট, রেফারেন্সের জন্য রাখা হলো) — ডিজাইন ফাইনালাইজ করা (কোনো কোড পরিবর্তন না, শুধু সিদ্ধান্ত)

লক্ষ্য: বাস্তবায়ন শুরুর আগে মূল কাঠামোগত প্রশ্নগুলোর উত্তর ঠিক করা, যাতে পরের ধাপে বারবার
রিডিজাইন করতে না হয়।

কাজ:
- ঠিক করা হবে নতুন কম্পোনেন্টের নাম কী হবে (যেমন `SyncAwareContent` নিজেই generic করা হবে,
  নাকি পাশাপাশি নতুন `SyncAwareRefreshableContent<T>` বানিয়ে ধাপে-ধাপে migrate করা হবে)।
  **সুপারিশ: নতুন, আলাদা কম্পোনেন্ট বানানো** — কারণ বিদ্যমান `SyncAwareContent`-এর ৪২টা
  কল-সাইট এক ধাপে বদলাতে গেলে নিয়ম ৪ ভঙ্গ হয়। নতুন কম্পোনেন্ট থাকলে স্ক্রিনগুলো একে একে,
  স্বাধীনভাবে migrate করা যায়, আর migrate না-হওয়া স্ক্রিনগুলো পুরনো (কার্যকর) আচরণেই থেকে যায়।
- ঠিক করা হবে "ডেটা বদলেছে কিনা" কীভাবে বোঝা হবে — লিস্টের ক্ষেত্রে সাধারণ `==`
  (data class হলে kotlin-এ এমনিতেই structural equality কাজ করে) যথেষ্ট কিনা, নাকি
  id + updatedAt-ভিত্তিক তুলনা লাগবে। single-value স্ক্রিন (ব্যালেন্স ইত্যাদি) আলাদা করে
  বিবেচনা করা হবে (সেখানে `PulsingValue` আগে থেকেই আছে — সেটাই ব্যবহার হবে, নতুন কিছু বানানোর
  দরকার নেই)।
- shimmer/flash-এর ন্যূনতম ও সর্বোচ্চ সময় ঠিক করা (যেমন ~300-450ms)।
- কোন কোন স্ক্রিন "list-shaped" (নতুন কম্পোনেন্টে যাবে) আর কোন কোন স্ক্রিন "single value/object
  shaped" (PulsingValue-তে যাবে, আলাদা migration path) তার একটা প্রাথমিক তালিকা বানানো।
- এই ধাপের আউটপুট: শুধু একটা ছোট ডিজাইন-নোট (এই ফাইলেই বা কমেন্টে) — কোনো `.kt` ফাইল
  বদলাবে না। **তারপর zip দেওয়ার দরকার নেই এই ধাপে**, যেহেতু কোড অপরিবর্তিত।

---

## ধাপ ২ — কোর কম্পোনেন্ট তৈরি (শুধু সংযোজন, কোনো বিদ্যমান কল-সাইট স্পর্শ না)

লক্ষ্য: `MotionToolkit.kt`-এ নতুন, স্বয়ংসম্পূর্ণ, generic কম্পোনেন্ট যোগ করা যা পুরনো ডেটা vs
নতুন ডেটা তুলনা করে সিদ্ধান্ত নেয় — flash করবে নাকি চুপচাপ swap করবে নাকি (প্রথমবার হলে) পুরো
skeleton দেখাবে। বিদ্যমান `SyncAwareContent`/`rememberSessionAwareSkeletonGate`/`PulsingValue`
কোনোটাই এই ধাপে বদলাবে না বা মুছবে না।

কাজ:
- নতুন `@Composable fun <T> SyncAwareRefreshableContent(...)` (বা ধাপ ১-এ ঠিক হওয়া নাম) লেখা।
- ভেতরের লজিক: প্রথমবার (syncPhase == LOADING) → পুরো skeleton। এরপর `data` প্যারামিটার
  আগের রাখা value থেকে ভিন্ন হলে → aged/previous content-এর জায়গায় সংক্ষিপ্ত সময়ের জন্য
  skeleton, তারপর নতুন data দিয়ে content। অপরিবর্তিত থাকলে → কোনো পরিবর্তন নেই, নীরবে একই
  content থাকবে।
- আগের ধাপের `rememberSessionAwareSkeletonGate` reuse করা প্রথম-ভিজিট behavior-এর জন্য —
  নতুন করে বানানো লাগবে না।
- ফাইলের উপরে/ফাংশনের উপরে বাংলা কমেন্টে স্পষ্ট লেখা: এটা কী সমস্যার সমাধান, কীভাবে কাজ করে,
  আর কেন পুরনো `SyncAwareContent`-এর পাশে নতুন করে বানানো হলো (migration নিরাপত্তার জন্য)।
- **এই ধাপে কোনো স্ক্রিন ফাইল (`app/src/main/java/com/example/ui/screens/*.kt`) স্পর্শ করা
  হবে না।**

ধাপ শেষে:
- `MotionToolkit.kt` কম্পাইল-সঠিক কিনা ম্যানুয়ালি যাচাই (ব্র্যাকেট ব্যালেন্স, import, generic
  syntax)।
- যেহেতু কোনো বিদ্যমান কল-সাইট বদলায়নি, পুরো অ্যাপের বাকি অংশ 100% আগের মতোই আচরণ করবে —
  এটা explicitly উল্লেখ করা।
- **পুরো প্রজেক্টের zip দেওয়া (dotfile সহ, ফাইল-লিস্ট diff করে যাচাই করে)।**

---

## ধাপ ৩ — পাইলট #১: একটা "লিস্ট" স্ক্রিন migrate করা

লক্ষ্য: নতুন কম্পোনেন্ট বাস্তব একটা লিস্ট-স্ক্রিনে কাজ করে কিনা যাচাই করা, বাকি স্ক্রিনে
ছড়ানোর আগে।

কাজ:
- একটা মাঝারি-জটিলতার লিস্ট স্ক্রিন বেছে নেওয়া (যেমন `SolverProblemsScreen` বা
  `FavoriteSolversScreen` — pagination/filter থাকা স্ক্রিন ইচ্ছাকৃতভাবে এড়িয়ে প্রথমে সহজতরটা
  বেছে নেওয়া ভালো)।
- সেই স্ক্রিনের `SyncAwareContent(...)` কল-সাইটটাকে নতুন `SyncAwareRefreshableContent(...)`-এ
  বদলানো, আসল লিস্ট ভ্যারিয়েবলকে `data` হিসেবে পাস করা।
- স্ক্রিনের বাইরের কাঠামো (header, filter chip, pull-to-refresh wrapper) অপরিবর্তিত রাখা —
  শুধু ভেতরের লিস্ট-অংশটুকু নতুন কম্পোনেন্টের ভেতরে যাবে।
- **শুধু এই একটা ফাইল বদলাবে এই ধাপে।**

ধাপ শেষে:
- এই স্ক্রিনে কী কী ম্যানুয়ালি verify করা হলো তা জানানো (syntax/signature মিল, import ঠিক
  আছে কিনা)।
- **পুরো প্রজেক্টের zip দেওয়া (dotfile সহ, diff-যাচাই সহ)।**
- ব্যবহারকারীকে এই একটা স্ক্রিন বিল্ড করে সত্যিকারের ডিভাইসে/এমুলেটরে টেস্ট করতে অনুরোধ করা,
  পরের ধাপে যাওয়ার আগে — যাতে বাস্তব আচরণ আশানুরূপ কিনা নিশ্চিত হওয়া যায়।

---

## ধাপ ৪ — পাইলট #২: একটা "single value/object" স্ক্রিন migrate করা

লক্ষ্য: শুধু লিস্ট না, একক ভ্যালু (ব্যালেন্স, প্রোফাইল অবজেক্ট) কেসেও প্যাটার্নটা কাজ করে
কিনা যাচাই করা — কারণ ধাপ ৩ শুধু লিস্ট-শেপ ভ্যালিডেট করে।

কাজ:
- একটা single-value/object-নির্ভর স্ক্রিন বেছে নেওয়া (যেমন `UserWalletScreen` এর ব্যালেন্স
  অংশ, বা `ProfileScreen`)।
- এখানে সাধারণত পুরো `SyncAwareRefreshableContent` না লাগিয়ে, শুধু বদলানো ভ্যালুটাকে
  বিদ্যমান `PulsingValue` দিয়ে wrap করাই যথেষ্ট হতে পারে — ধাপ ১-এ এই সিদ্ধান্ত আগেই নেওয়া
  থাকবে।
- **শুধু এই একটা ফাইল বদলাবে এই ধাপে।**

ধাপ শেষে:
- **পুরো প্রজেক্টের zip দেওয়া (dotfile সহ, diff-যাচাই সহ)।**
- ব্যবহারকারীকে টেস্ট করতে অনুরোধ করা, পরের ধাপে যাওয়ার আগে।

---

## ধাপ ৫ — বাকি "লিস্ট" স্ক্রিনগুলো ব্যাচে migrate করা

লক্ষ্য: ধাপ ৩-এ ভ্যালিডেট হওয়া প্যাটার্নটা বাকি লিস্ট-স্ক্রিনগুলোতে ছড়ানো, কিন্তু একসাথে সব
না করে ছোট ব্যাচে।

কাজ:
- বাকি লিস্ট-স্ক্রিনগুলোকে ৩-৫টার ছোট ব্যাচে ভাগ করা (যেমন ব্যাচ ১: `UserProblemsScreen`,
  `AllOpenProblemsScreen`, `BidManagementScreen`, `SolverMyBidsScreen`, `MessagesScreen`)।
- প্রতিটা ব্যাচে শুধু ওই ব্যাচের ফাইলগুলোই বদলানো।

ধাপ শেষে (**প্রতিটা ব্যাচের পরেই, পুরো ধাপ ৫ শেষে না**):
- সেই ব্যাচে ঠিক কোন কোন ফাইল বদলালো তার তালিকা দেওয়া।
- **পুরো প্রজেক্টের zip দেওয়া (dotfile সহ, diff-যাচাই সহ)।**

*(এই ধাপ যতগুলো ব্যাচ লাগে ততবার পুনরাবৃত্তি হবে, প্রতি ব্যাচ শেষে একটা করে zip।)*

---

## ধাপ ৬ — বাকি "single value/object" ও "মিশ্র" স্ক্রিনগুলো ব্যাচে migrate করা

লক্ষ্য: `AdminPanelScreen` (১১টা কল-সাইট, সবচেয়ে বেশি জটিল — নিজেই একটা আলাদা ব্যাচ/উপ-ব্যাচ
হওয়া উচিত), detail স্ক্রিন (`ProblemDetailScreen`, `PublicProfileScreen`,
`ReputationDetailScreen` ইত্যাদি), আর remaining single-value স্ক্রিনগুলো migrate করা।

কাজ:
- `AdminPanelScreen`-কে নিজের একাধিক ছোট ব্যাচে ভাগ করা (একেকটা ট্যাব একেক ব্যাচে) — এই একটা
  ফাইলে ১১টা কল-সাইট একসাথে বদলানো নিয়ম ৪ ভঙ্গ করবে।
- বাকি detail/single-value স্ক্রিনগুলোকেও ৩-৫টার ব্যাচে ভাগ করা।

ধাপ শেষে (**প্রতিটা ব্যাচের পরেই**):
- **পুরো প্রজেক্টের zip দেওয়া (dotfile সহ, diff-যাচাই সহ)।**

---

## ধাপ ৭ — পুরনো কম্পোনেন্ট পরিষ্কার করা (cleanup)

লক্ষ্য: যদি সব স্ক্রিন সফলভাবে migrate হয়ে যায় এবং ব্যবহারকারী নিশ্চিত করে যে সবকিছু ঠিকঠাক
কাজ করছে — তাহলে পুরনো `SyncAwareContent` (non-generic) আর কোনো call-site ব্যবহার না করলে,
সেটা dead code হিসেবে মুছে ফেলা যায়, প্রজেক্টের বাকি dead-code cleanup entries-এর মতোই একটা
কমেন্ট রেখে (কেন মোছা হলো, কোন কমিটে কী ছিল — বিদ্যমান `MIGRATION_PROGRESS.md`-এর স্টাইলে)।

**সতর্কতা:** যদি কোনো স্ক্রিন ইচ্ছাকৃতভাবে পুরনো আচরণেই থেকে যায় (যেমন কোনো কারণে সেখানে
diff-based refresh দরকার নেই), তাহলে পুরনো কম্পোনেন্ট মোছা যাবে না — শুধু ব্যবহার না-হওয়া অংশ
নিশ্চিত হলেই মোছা হবে।

ধাপ শেষে:
- **পুরো প্রজেক্টের zip দেওয়া (dotfile সহ, diff-যাচাই সহ)।**

---

## ধাপ ৮ — চূড়ান্ত যাচাই ও সারাংশ

লক্ষ্য: পুরো কাজ শেষে একটা সম্পূর্ণ sanity pass।

কাজ:
- `grep` দিয়ে নিশ্চিত হওয়া যে কোনো স্ক্রিনে ভুলবশত পুরনো আর নতুন কম্পোনেন্ট মিশিয়ে ফেলা হয়নি,
  বা কোনো import বাদ পড়েনি।
- প্রতিটা বদলানো ফাইলে ব্র্যাকেট/সিনট্যাক্স ব্যালেন্স আরেকবার চেক করা।
- মূল zip-এর ফাইল-লিস্টের সাথে চূড়ান্ত zip-এর ফাইল-লিস্ট diff করে ১০০% মিল নিশ্চিত করা
  (dotfile সহ)।
- একটা সংক্ষিপ্ত সারাংশ দেওয়া: কোন কোন স্ক্রিন migrate হলো, কোনগুলো পুরনো আচরণে রয়ে গেল (যদি
  থাকে) এবং কেন।

ধাপ শেষে:
- **চূড়ান্ত, সম্পূর্ণ প্রজেক্টের zip দেওয়া (dotfile সহ, diff-যাচাই সহ)।**

---

## Handoff Note — ধাপ ২ অন্য session-এ শুরু করার জন্য প্রয়োজনীয় প্রেক্ষাপট

এই সেকশনটা যোগ করা হলো যাতে ধাপ ২ শুরুর আগে নতুন session-এ আবার প্রথম থেকে কোডবেস
explore/ডিরাইভ করা না লাগে।

**বর্তমান কোডের অবস্থা (ধাপ ০-১ পর্যন্ত যা যা হয়ে গেছে):**
- `MotionToolkit.kt`-এ `SyncAwareContent` ইতিমধ্যে fix হয়ে গেছে (session-aware first-visit
  skeleton gate, `rememberSessionAwareSkeletonGate` ব্যবহার করে) — এটা অক্ষত রাখতে হবে।
- `SyncAwareContent`-এর বর্তমান সিগনেচার:
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
  ) { ... }
  ```
  (এটা এখনো non-generic, `content` কোনো প্যারামিটার নেয় না — নতুন
  `SyncAwareRefreshableContent<T>` এর পাশে বসবে, এটাকে বদলানো হবে না।)
- ইতিমধ্যে বিদ্যমান রিইউজেবল হেল্পার: `rememberMinimumSkeletonGate`,
  `rememberSessionAwareSkeletonGate(sessionKey, viewModel, minimumDurationMs = 220L)`,
  `PulsingValue(isUpdating, modifier, content)`, `worstSyncPhase(vararg phases)`,
  `SyncErrorState` (private), `SyncBlockedRetryState`।
- `SomadhanViewModel`-এ `hasLoadedOnce(key)`/`markLoadedOnce(key)` (in-memory
  `sessionLoadedScreens: MutableSet<String>`, process/app-session-scoped) আগে থেকেই আছে —
  নতুন কম্পোনেন্ট এটাই reuse করবে, নতুন state আলাদা করে ViewModel-এ বানানোর দরকার নেই।
- `SyncAwareContent`-এর ৪২টা কল-সাইট (৩০টা স্ক্রিন ফাইলে) — এখনো একটাও `SyncAwareRefreshableContent`-এ
  migrate হয়নি। ফাইলের নাম ও sessionKey-এর সম্পূর্ণ তালিকা উপরে "ধাপ ১ (সম্পন্ন)"-এর
  classification টেবিলে আছে।

**ধাপ ২ শুরু করার সময় যা করতে হবে (সংক্ষিপ্ত পুনরাবৃত্তি):**
1. `MotionToolkit.kt`-এ নতুন `@Composable fun <T> SyncAwareRefreshableContent(...)` যোগ করা —
   parameters: `sessionKey: String`, `viewModel: SomadhanViewModel`,
   `syncPhase: SupabaseRealtimeManager.SyncPhase`, `data: T`, `onRetry: () -> Unit`,
   `modifier: Modifier = Modifier`, `refreshFlashMs: Long = 350L`,
   `skeleton: @Composable () -> Unit = { ListScreenSkeleton() }`,
   `content: @Composable (T) -> Unit`।
2. ভেতরের লজিক (ধাপ ১-এর সিদ্ধান্ত অনুযায়ী): প্রথমবার `rememberSessionAwareSkeletonGate` সত্যি
   থাকা পর্যন্ত পুরো skeleton (আগের `SyncAwareContent`-এর প্যাটার্নই)। এরপর `data` আগের রাখা
   value থেকে `!=` হলে (সাধারণ structural equality) → সংক্ষিপ্ত `refreshFlashMs` সময়ের জন্য
   skeleton, তারপর নতুন `data` নিয়ে `content(data)` — বাইরের স্ক্রিন-কাঠামো অপরিবর্তিত থাকবে
   কারণ caller নিজেই header/filter এই কম্পোনেন্টের বাইরে রাখবে।
3. **এই ধাপে কোনো স্ক্রিন ফাইল স্পর্শ করা হবে না** — শুধু `MotionToolkit.kt`।
4. ধাপ ২ শেষে পুরো প্রজেক্টের zip (dotfile সহ, diff-যাচাই সহ) দিতে হবে — উপরের ধাপ ০-এর নিয়ম
   অনুযায়ী।

তারপর ধাপ ৩ থেকে যথারীতি এগোনো (উপরে বর্ণিত)।

---

## Quick Checklist (প্রতিটা ধাপের আগে/পরে দ্রুত মিলিয়ে নেওয়ার জন্য)

- [ ] এই ধাপে কোন ফাইল(গুলো) বদলাচ্ছে তা স্পষ্টভাবে বলা হয়েছে
- [ ] অন্য কোনো ফাইল/ফাংশনালিটি অনিচ্ছাকৃতভাবে বদলায়নি
- [ ] খালি ডেটা vs "এখনো লোড হয়নি" গুলিয়ে ফেলা হয়নি
- [ ] বিদ্যমান session-aware first-visit skeleton আচরণ অক্ষত আছে
- [ ] পরিবর্তিত ফাইলের ব্র্যাকেট/ইম্পোর্ট/সিগনেচার ম্যানুয়ালি যাচাই করা হয়েছে
- [ ] zip-এ dotfile সহ **সব** ফাইল আছে (diff করে যাচাই করা হয়েছে)
- [ ] zip ফাইল-সংখ্যা আগের zip-এর সাথে (নতুন ইচ্ছাকৃত ফাইল বাদে) মিলছে
- [ ] zip ব্যবহারকারীকে present করা হয়েছে

---

## ধাপ ২ (সম্পন্ন) — কোর কম্পোনেন্ট তৈরি

`MotionToolkit.kt`-এ নতুন `@Composable fun <T> SyncAwareRefreshableContent(...)` যোগ করা হয়েছে
(পুরনো `SyncAwareContent`-এর ঠিক নিচে, `worstSyncPhase()`-এর আগে)। কোনো স্ক্রিন ফাইল স্পর্শ করা
হয়নি — শুধু `MotionToolkit.kt` বদলেছে। বিস্তারিত ডিজাইন যুক্তি ইনলাইন KDoc কমেন্টে লেখা আছে।

**যাচাই করা হয়েছে:**
- ফাইলের `{`/`(`/`[` ব্র্যাকেট ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে (০ imbalance)।
- সব top-level ফাংশন/অবজেক্ট ঘোষণা তালিকা করে দেখা হয়েছে — কোনোটা ভাঙেনি, নতুনটা ঠিক জায়গায় বসেছে।
- `grep` করে নিশ্চিত হওয়া হয়েছে বিদ্যমান কোনো স্ক্রিন ফাইলে `SyncAwareRefreshableContent` এখনো
  ব্যবহৃত হয়নি (যেমনটা এই ধাপে হওয়ার কথা)।
- zip-এর ফাইল-লিস্ট আগের zip-এর সাথে diff করে ১০০% মিল নিশ্চিত করা হয়েছে (dotfile সহ, ২৪৪টা
  ফাইল, কোনোটা যোগ/বিয়োগ হয়নি) — শুধু `MotionToolkit.kt` (এডিট) আর এই প্রোগ্রেস নোট বদলেছে।
- Gradle build/run এই sandbox-এ সম্ভব না (আগের সব সেশনের মতোই) — Android Studio-তে প্রথম sync-এ
  আসল কম্পাইল নিশ্চিত করা উচিত পরের ধাপে যাওয়ার আগে বা সাথে।

**পরবর্তী ধাপ (ধাপ ৩):** একটা পাইলট লিস্ট-স্ক্রিন (সুপারিশ: `SolverProblemsScreen` বা
`FavoriteSolversScreen`) বেছে নিয়ে তার `SyncAwareContent(...)` কল-সাইটকে নতুন
`SyncAwareRefreshableContent(...)`-এ migrate করা, শুধু সেই একটা ফাইল বদলে।

---

## ধাপ ৩ (সম্পন্ন) — পাইলট #১: `ActiveJobsPopupScreen.kt` migrate করা

**বদলানো ফাইল:** শুধু `app/src/main/java/com/example/ui/screens/ActiveJobsPopupScreen.kt` (আর
এই প্রোগ্রেস নোট)। **কেন এই স্ক্রিন বেছে নেওয়া হলো:** roadmap-এর ১৬টা list-shaped স্ক্রিনের মধ্যে
এটাই একমাত্র যেখানে কোনো pagination/filter নেই, ঠিক একটা `SyncAwareContent` কল-সাইট আছে, আর
`activePosts`-এর টাইপ (`List<ActivePostWithActivity>`, একটা `data class`) সাধারণ Kotlin `==`
দিয়ে অর্থপূর্ণ তুলনা করা যায় — pilot হিসেবে সবচেয়ে কম-ঝুঁকিপূর্ণ।

**কী বদলালো:** import `SyncAwareContent` → `SyncAwareRefreshableContent`; কল-সাইটে
`data = activePosts` যোগ, content lambda-র প্যারামিটার `posts` (আগের বাইরের `activePosts`-এর
বদলে ভেতরে `posts` ব্যবহার) — dialog-এর হেডার/ক্লোজ-বাটন/`activePosts.size` কাউন্টার (এই
কম্পোনেন্টের বাইরে) অপরিবর্তিত রয়ে গেছে, শুধু নিচের empty-state/LazyColumn অংশটুকু এখন
diff-aware।

**যাচাই করা হয়েছে:**
- `grep` করে নিশ্চিত হওয়া হয়েছে ফাইলে আর কোনো `SyncAwareContent(` কল-সাইট নেই (শুধু কমেন্টে
  পুরনো নাম উল্লেখ আছে) আর নতুন import ঠিকমতো বসেছে।
- ফাইলের `{`/`(`/`[` ব্র্যাকেট ব্যালেন্স আলাদাভাবে গুনে যাচাই করা হয়েছে (০ imbalance)।
- পুরো প্রজেক্ট মূল zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু
  `MotionToolkit.kt` (ধাপ ২) আর `ActiveJobsPopupScreen.kt` (এই ধাপ) কনটেন্ট বদলেছে, বাকি সব
  ফাইল বাইট-বাই-বাইট অপরিবর্তিত।
- zip-এর ফাইল-লিস্ট diff করে ১০০% মিল নিশ্চিত করা হয়েছে (dotfile সহ, ফাইল-সংখ্যা অপরিবর্তিত)।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের মতোই)।

**পরের ধাপ (ধাপ ৪):** একটা single-value/object স্ক্রিন (সুপারিশ: `UserWalletScreen` বা
`ProfileScreen`) বেছে নিয়ে সেখানে বিদ্যমান `PulsingValue` দিয়ে বদলানো ফিল্ডটুকু wrap করা।
ব্যবহারকারীকে এই ধাপ ৩-এর পাইলট টেস্ট করার অনুরোধ করা হচ্ছে, পরের ধাপে যাওয়ার আগে।

---

## ধাপ ৪ (সম্পন্ন) — পাইলট #২: `UserWalletScreen.kt` (single-value granular pulse)

**বদলানো ফাইল:** `app/src/main/java/com/example/ui/components/MotionToolkit.kt` (শুধু সংযোজন) আর
`app/src/main/java/com/example/ui/screens/UserWalletScreen.kt` (আর এই প্রোগ্রেস নোট)। ধাপ ৩-এর
পাইলট এখনো Android Studio-তে টেস্ট করা হয়নি — ব্যবহারকারীর অনুরোধেই ধাপ ৪ এগিয়ে নেওয়া হলো।

**সমস্যা ধরা পড়েছিল:** `UserWalletScreen.kt`-এ ব্যালেন্স কার্ডে আগে থেকেই
`PulsingValue(isUpdating = isRefreshing)` ব্যবহার হচ্ছিল — কিন্তু এটা গ্লোবাল pull-to-refresh
ফ্ল্যাগ, নির্দিষ্ট মান সত্যিই বদলেছে কিনা তা না। ফলে balance অপরিবর্তিত থাকলেও যেকোনো রিফ্রেশে
flash হতো — যা এই পুরো সাব-প্রজেক্টের মূল লক্ষ্যের ("ডেটা অপরিবর্তিত থাকলে flash হবে না") সরাসরি
বিপরীত।

**কী যোগ হলো:**
- `MotionToolkit.kt`-এ নতুন `@Composable fun <T> rememberFieldChangePulse(value: T, durationMs: Long = 350L): Boolean`
  — আগের মান মনে রাখে, `value` সত্যিই বদলালে ৩৫০ms (ধাপ ১-এর সিদ্ধান্ত অনুযায়ী ডিফল্ট) `true`
  রিটার্ন করে তারপর নিজে থেকে `false`-এ ফেরে; প্রথমবার composition-এ pulse হয় না। এটা
  `SyncAwareRefreshableContent`-এর list-diffing লজিকেরই scalar-ভার্সন, বিদ্যমান `PulsingValue`
  স্পর্শ করেনি।
- `UserWalletScreen.kt`-এ দুটো ফিল্ড আলাদাভাবে wire করা হয়েছে: ব্যালেন্স কার্ডের `currentBalance`
  (আগের `isRefreshing`-নির্ভর কল বদলে) আর এসক্রো কার্ডের `totalHeldEscrowAmount` (আগে কোনো
  pulse ছিলই না)। দুটো এখন সম্পূর্ণ স্বাধীন — একটা বদলালে অন্যটা স্থির থাকবে।

**যাচাই করা হয়েছে:**
- `MotionToolkit.kt` আর `UserWalletScreen.kt` দুটোতেই `(`/`{`/`[` ব্র্যাকেট ব্যালেন্স আলাদাভাবে
  পাইথন স্ক্রিপ্ট দিয়ে গুনে মিলিয়ে দেখা হয়েছে (imbalance নেই)।
- পুরো প্রজেক্ট মূল zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু এই দুটো ফাইলের
  কনটেন্ট বদলেছে, বাকি সব বাইট-বাই-বাইট অপরিবর্তিত (dotfile সহ, ফাইল-সংখ্যা অভিন্ন)।
- `rememberFieldChangePulse` কল-সাইট দুটো grep করে নিশ্চিত করা হয়েছে সঠিক ফিল্ড
  (`currentBalance`, `totalHeldEscrowAmount`) দিয়ে কল হচ্ছে, আর `isRefreshing` ভ্যারিয়েবলটা
  এখনো `SomadhanPullToRefresh`-এ ব্যবহৃত হচ্ছে বলে unused হয়ে যায়নি।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের ধাপগুলোর মতোই) — এটাই **এখন সবচেয়ে জরুরি**, কারণ
  ধাপ ৩ আর ৪ দুটোই এখনো Android Studio-তে যাচাই করা হয়নি।

**⚠️ পরের সেশনের জন্য নোট (এই নোটটা এখন পুরনো, নিচের ধাপ ৫ দ্রষ্টব্য):** ~~ধাপ ৩
(`ActiveJobsPopupScreen.kt`) আর ধাপ ৪ (`UserWalletScreen.kt`, `MotionToolkit.kt`) — দুটোই
compile/UI টেস্ট বাকি।~~

---

## ধাপ ৫ (সম্পন্ন) — নতুন সিদ্ধান্ত: "extra feature" (ম্যানুয়াল pull-to-refresh-এ সবসময় pulse)

**ব্যবহারকারীর সিদ্ধান্ত:** এখন পর্যন্ত পুরো ডিজাইন ছিল শুধু *diff-based* — ডেটা সত্যিই বদলালে
তবেই flash/pulse হবে, নাহলে একদম নীরব। ব্যবহারকারী একটা **অতিরিক্ত** নিয়ম যোগ করতে বলেছেন,
যেটা diff-based আচরণের *উপরে* বসবে, প্রতিস্থাপন করবে না:

> ব্যবহারকারী নিজে হাতে pull-to-refresh করলে — নতুন ডেটা আসুক বা না আসুক — সংশ্লিষ্ট
> লিস্ট/ভ্যালুটুকু সংক্ষিপ্ত সময়ের (loading-এর মতো) shimmer/pulse দেখাবে, যাতে "রিফ্রেশ হয়েছে"
> এই ফিডব্যাক সবসময় পাওয়া যায়। পেজের কাঠামো (header/filter/tab) তখনও অপরিবর্তিত থাকবে —
> শুধু pull-to-refresh-এর ক্ষেত্রেই এই "সবসময় pulse" নিয়ম প্রযোজ্য; realtime-এ automatic
> background আপডেটে আগের নিয়মই (শুধু আসল বদলে flash) বহাল থাকবে।

**ডিজাইন:** দুই কম্পোনেন্টেই একটা ঐচ্ছিক `isManualRefreshing: Boolean = false` প্যারামিটার
যোগ হলো ([SyncAwareRefreshableContent] আর [rememberFieldChangePulse], দুটোই `MotionToolkit.kt`-এ)।
কলার তার স্ক্রিনের বিদ্যমান pull-to-refresh state (`isRefreshing`, `SomadhanPullToRefresh`-এ যা
ব্যবহৃত হয়) সরাসরি এখানে পাস করবে। এটা সত্যি থেকে মিথ্যায় ফিরলেই (মানে refresh সাইকেল শেষ) — data
সত্যিই বদলেছে কিনা তা না দেখেই সংক্ষিপ্ত ([refreshFlashMs]/[durationMs], ডিফল্ট ৩৫০ms) flash।
ডিফল্ট `false` হওয়ায় pull-to-refresh নেই এমন স্ক্রিনে (যেমন `ActiveJobsPopupScreen`, যেটা একটা
popup dialog, ওখানে `SomadhanPullToRefresh` নেই) — কোনো কল-সাইট পরিবর্তন ছাড়াই আগের আচরণ অক্ষত
থাকে।

দুটো ট্রিগার (data-change vs manual-refresh-সমাপ্তি) সম্পূর্ণ স্বাধীন effect-এ চলে, আর একটা
বুলিয়ানের বদলে **কাউন্টার** (`flashRequestCount` / `activePulseReasons`) দিয়ে combine করা হয়েছে
— যাতে দুটো একই সময়ে ওভারল্যাপ করলে একটা শেষ হয়ে গেলেও অন্যটা চলতে থাকলে flash অকালে বন্ধ হয়ে না
যায়।

**বদলানো ফাইল:**
- `MotionToolkit.kt` — `rememberFieldChangePulse()` নতুন `isManualRefreshing` প্যারামিটার +
  কাউন্টার-ভিত্তিক করা হলো (আগে বুলিয়ান ছিল)। `SyncAwareRefreshableContent()`-এও একই
  `isManualRefreshing` প্যারামিটার + `isRefreshFlashing`-কে বুলিয়ান থেকে
  `flashRequestCount`-ভিত্তিক করা হলো। উভয় জায়গায় KDoc আপডেট করা হয়েছে।
- `UserWalletScreen.kt` — দুটো `PulsingValue` কল-সাইট (`currentBalance`,
  `totalHeldEscrowAmount`) এখন `isManualRefreshing = isRefreshing` পাস করছে (এই স্ক্রিনে আগে
  থেকেই `isRefreshing` state ছিল pull-to-refresh চালাতে)।
- `ActiveJobsPopupScreen.kt` **স্পর্শ করা হয়নি** — এই স্ক্রিনে কোনো pull-to-refresh মেকানিজম
  (`SomadhanPullToRefresh`/`isRefreshing`) নেই বলে নতুন প্যারামিটার প্রযোজ্যই না; ডিফল্ট
  `false`-এর কারণে এই স্ক্রিন বিদ্যমান diff-based আচরণেই থাকবে, যেটা এখানে সঠিক (popup-এ
  ম্যানুয়াল রিফ্রেশের ধারণাই নেই)।

**যাচাই করা হয়েছে:**
- `MotionToolkit.kt` আর `UserWalletScreen.kt` — দুটোতেই `(`/`{`/`[` ব্র্যাকেট ব্যালেন্স
  আলাদাভাবে গুনে মিলিয়ে দেখা হয়েছে (imbalance নেই)।
- আগের ধাপের zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু এই দুটো ফাইল বদলেছে,
  বাকি সব (dotfile সহ) বাইট-বাই-বাইট অপরিবর্তিত, ফাইল-সংখ্যা অভিন্ন।
- `rememberFieldChangePulse`/`SyncAwareRefreshableContent`-এর নতুন প্যারামিটার ঐচ্ছিক + ডিফল্ট
  `false` হওয়ায় `ActiveJobsPopupScreen.kt`-সহ অন্য কোনো (এখনো migrate না-হওয়া) কল-সাইট ভাঙার
  কথা না।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের ধাপগুলোর মতোই)।

**⚠️ পরের সেশনের জন্য (এখন পুরনো, নিচের ধাপ ৬ দ্রষ্টব্য):**
1. ~~ধাপ ৩, ৪, ৫ — তিনটাই এখনো Android Studio-তে compile/UI টেস্ট বাকি। এটাই এখন সবচেয়ে জরুরি।~~
2. ~~এই "extra feature" এখনো শুধু `UserWalletScreen.kt`-এর ২টা ফিল্ডে wire করা হয়েছে। বাকি
   list-shaped আর single-value স্ক্রিনগুলো migrate করার সময় (ভবিষ্যতে) যাদের pull-to-refresh
   আছে, তাদের প্রত্যেকের জন্যও `isManualRefreshing`/`isRefreshing` পাস করে দিতে হবে — এটা এখন
   থেকে migration-এর একটা standard অংশ, আলাদা করে মনে রাখতে হবে না ভুলে গেলে বাগ হবে না, শুধু
   "extra feature"-টা মিস হবে (ডিফল্ট `false` নিরাপদ fallback)।~~
3. তালিকার পরের প্রার্থী: বাকি single-value স্ক্রিনগুলো (`ProfileScreen`, `DashboardScreen`
   ইত্যাদি) অথবা বাকি list-shaped স্ক্রিনগুলো `SyncAwareRefreshableContent`-এ migrate করা —
   কোনটা আগে করা হবে তা পরের সেশনে ব্যবহারকারীকে জিজ্ঞেস করে ঠিক করতে হবে। ব্যবহারকারীর সবশেষ
   নির্দেশ ছিল "৩টা করে স্ক্রিন migrate করে তারপর zip দেওয়া" (batch size 3)। **(এখনো বাকি —
   ধাপ ৬-এ এটা এগোয়নি, শুধু re-entry ফিচারের সিদ্ধান্ত/বাস্তবায়ন হয়েছে।)**

---

## ধাপ ৬ (সম্পন্ন) — নতুন সিদ্ধান্ত: re-entry-কেও "রিফ্রেশ" হিসেবে গণ্য করা (পথ B)

**প্রশ্ন যা তোলা হয়েছিল:** কোনো স্ক্রিন থেকে বেরিয়ে (navigate away) আবার সেই স্ক্রিনে ঢুকলে
(re-entry) — যেহেতু global `syncPhase` ইতিমধ্যে `LOADED` আর ওই sessionKey আগে থেকেই
`markLoadedOnce()` দিয়ে চিহ্নিত, তাই cold-load skeleton গেট আর দেখায় না। তাহলে re-entry-তে কী
হওয়া উচিত?
- **পথ A:** সরাসরি বিদ্যমান ডেটা দেখানো, কোনো flash/shimmer ছাড়াই (Instagram-ধাঁচ)।
- **পথ B:** re-entry-কেও একধরনের "রিফ্রেশ" ধরে নেওয়া — স্ক্রিন বন্ধ থাকা অবস্থায় realtime-এ
  ডেটা সত্যিই বদলে থাকতে পারে, তাই ডেটা বদলাক বা না বদলাক, mount হওয়ার সাথে সাথে একবার সংক্ষিপ্ত
  (~৩৫০ms) flash দেখিয়ে "সর্বশেষ ডেটা আনা হচ্ছে" এই ফিডব্যাক দেওয়া — অনেকটা pull-to-refresh-এর
  ধাপ ৫-এর "extra feature"-এর মতোই, শুধু ট্রিগার এখানে ম্যানুয়াল বাটনের বদলে "স্ক্রিনে আবার ঢোকা"।

**ব্যবহারকারীর সিদ্ধান্ত:** পথ B। বাস্তবায়নের আগে যা যা ঠিক হয়েছিল আগের ধাপগুলোতে (ধাপ ১-এর
৩৫০ms flash duration, ধাপ ৫-এর কাউন্টার-ভিত্তিক combine প্যাটার্ন) — সেগুলোরই উপরে এই তৃতীয়,
স্বাধীন ট্রিগার-কারণ যোগ হলো, আগের দুটো (data-change, manual-refresh-সমাপ্তি) প্রতিস্থাপন না
করে।

**ডিজাইন:**
- **"re-entry" কীভাবে বোঝা হয়:** প্রতিটা কম্পোজেবল ইনস্ট্যান্স mount হওয়ার মুহূর্তে
  (`remember`-এ ক্যাপচার করে, শুধু একবার) চেক করা হয় — `viewModel.hasLoadedOnce(sessionKey)`
  তখনই `true` কিনা। `true` মানে এই sessionKey এই app session-এ *আগেই* একবার লোড হয়ে গেছে
  (অর্থাৎ এটা প্রথম cold visit না, বরং স্ক্রিন থেকে বেরিয়ে আবার ঢোকা)। `false` মানে সত্যিকারের
  প্রথম visit — তখন cold-load skeleton গেট নিজেই পুরো skeleton দেখিয়ে দেয়, এই re-entry flash
  আলাদাভাবে সেখানে যোগ হয় না (দুটো একসাথে ঘটবে না, ডিজাইন অনুযায়ী পরস্পর exclusive)।
- **`SyncAwareRefreshableContent<T>`-এ (list-shaped):** নতুন ঐচ্ছিক `flashOnReentry: Boolean = true`
  প্যারামিটার। `isReentryVisit` mount-এ ক্যাপচার করে, `lastShownData` এখনো
  `SyncAwareRefreshableUninitialized` (মানে এই ইনস্ট্যান্সে এখনো প্রথমবার data সেট হয়নি) এমন
  অবস্থায় যদি `flashOnReentry && isReentryVisit` সত্যি হয় — data সত্যিই বদলেছে কিনা তা না
  দেখেই একবার `refreshFlashMs` সময়ের flash, তারপর data-কে "সবশেষ দেখানো" হিসেবে রেকর্ড।
  নাহলে (সত্যিকারের প্রথম visit, বা flashOnReentry বন্ধ) আগের মতোই flash ছাড়া সরাসরি রেকর্ড।
- **`rememberFieldChangePulse<T>`-এ (single-value):** নতুন ঐচ্ছিক `sessionKey: String? = null`,
  `viewModel: SomadhanViewModel? = null`, `flashOnReentry: Boolean = true` প্যারামিটার। mount-এ
  একটা স্বাধীন `LaunchedEffect(Unit)` — `flashOnReentry && isReentryVisit` সত্যি হলে একবার
  pulse করে (data-change আর manual-refresh-এর effect দুটো থেকে সম্পূর্ণ আলাদা)। `sessionKey`/
  `viewModel` না দিলে (ডিফল্ট `null`) এই তৃতীয় কারণ সম্পূর্ণ নিষ্ক্রিয় থাকে — পুরনো কল-সাইট
  (parameter না দেওয়া, এই মুহূর্তে অবশ্য বাস্তবে কোনো নেই) অক্ষত থাকতো।
- তিনটা কারণ (data-change, manual-refresh-সমাপ্তি, re-entry) — সবগুলোই একই
  counter-ভিত্তিক (`flashRequestCount`/`activePulseReasons`) প্যাটার্নে combine, তাই একাধিক
  একসাথে ট্রিগার হলেও একটা শেষ হয়ে গেলেও অন্যটা চলতে থাকলে flash অকালে বন্ধ হবে না।

**বদলানো ফাইল:**
- `MotionToolkit.kt` — `SyncAwareRefreshableContent<T>()`-এ `flashOnReentry` প্যারামিটার + মাউন্ট
  ক্যাপচার করা `isReentryVisit` + প্রথম-data-effect-এর ভেতরে শাখা যোগ। `rememberFieldChangePulse<T>()`-এ
  `sessionKey`/`viewModel`/`flashOnReentry` প্যারামিটার + নতুন স্বাধীন mount `LaunchedEffect`।
  উভয় জায়গায় KDoc আপডেট।
- `UserWalletScreen.kt` — দুটো `rememberFieldChangePulse(...)` কল-সাইট (`currentBalance`,
  `totalHeldEscrowAmount`) এখন `sessionKey = "user_wallet_sync"` (এই স্ক্রিনের বিদ্যমান
  `SyncAwareContent` গেটের সাথেই একই key — নতুন আলাদা key/markLoadedOnce() call-site বানানো
  হয়নি, যেহেতু সেই গেটই ইতিমধ্যে এই key মার্ক করে) আর `viewModel = viewModel` পাস করছে।
- `ActiveJobsPopupScreen.kt` **স্পর্শ করা হয়নি** — `SyncAwareRefreshableContent`-এ `flashOnReentry`
  ডিফল্ট `true`, sessionKey/viewModel আগে থেকেই পাস করা ছিল, তাই কোনো কোড পরিবর্তন ছাড়াই এই
  popup-ও (বন্ধ করে আবার খুললে) নতুন re-entry flash আচরণ পায় — যা উদ্দেশ্যের সাথেই সামঞ্জস্যপূর্ণ
  (popup বন্ধ থাকা অবস্থাতেও realtime-এ ডেটা বদলে থাকতে পারে)।

**যাচাই করা হয়েছে:**
- `MotionToolkit.kt` আর `UserWalletScreen.kt` — দুটোতেই `(`/`{`/`[` ব্র্যাকেট ব্যালেন্স আলাদাভাবে
  পাইথন স্ক্রিপ্ট দিয়ে গুনে মিলিয়ে দেখা হয়েছে (imbalance নেই)।
- `grep` করে প্রজেক্টের সব `rememberFieldChangePulse(`/`SyncAwareRefreshableContent(` কল-সাইট
  (মোট ৩টা — `UserWalletScreen.kt`-এ ২টা, `ActiveJobsPopupScreen.kt`-এ ১টা) একে একে দেখা হয়েছে;
  নতুন প্যারামিটারগুলো সবগুলোতেই ঐচ্ছিক + নিরাপদ ডিফল্ট (`true`/`null`) হওয়ায় কোনোটা ভাঙার কথা না।
- আগের ধাপের zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু এই দুটো ফাইল (+ এই
  প্রোগ্রেস নোট) বদলেছে, বাকি সব (dotfile সহ) বাইট-বাই-বাইট অপরিবর্তিত, ফাইল-সংখ্যা অভিন্ন।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের সব ধাপের মতোই, `gradlew`/Gradle distribution এই
  পরিবেশে চালানো যায় না) — Android Studio-তে প্রথম sync-এ আসল কম্পাইল নিশ্চিত করা জরুরি।

**⚠️ পরের সেশনের জন্য (এখনো বাকি):**
1. ধাপ ৩, ৪, ৫, ৬ — সবগুলোই এখনো Android Studio-তে compile/UI টেস্ট বাকি। এটাই এখন সবচেয়ে জরুরি।
2. re-entry flash (ধাপ ৬) এখনো শুধু `UserWalletScreen.kt`-এর ২টা ফিল্ড আর
   `ActiveJobsPopupScreen.kt`-এর ১টা কল-সাইটে কার্যকর (ডিফল্ট `true` হওয়ায় auto)। বাকি
   list-shaped/single-value স্ক্রিন migrate করার সময় sessionKey/viewModel দিয়ে কল করলেই এই
   ফিচার আপনাআপনি কাজ করবে — আলাদা করে কিছু on করতে হবে না, শুধু কোনো স্ক্রিনে ইচ্ছাকৃতভাবে বন্ধ
   রাখতে চাইলে `flashOnReentry = false` পাস করতে হবে।
3. তালিকার পরের প্রার্থী (অপরিবর্তিত, ধাপ ৫ থেকে): বাকি single-value স্ক্রিনগুলো (`ProfileScreen`,
   `DashboardScreen` ইত্যাদি) অথবা বাকি list-shaped স্ক্রিনগুলো `SyncAwareRefreshableContent`-এ
   migrate করা — কোনটা আগে করা হবে তা পরের সেশনে ব্যবহারকারীকে জিজ্ঞেস করে ঠিক করতে হবে। batch
   size 3 নির্দেশ অক্ষত।

---

## ধাপ ৭ (আংশিক সম্পন্ন — ব্যাচ ১, ১টা স্ক্রিন) — বাকি "লিস্ট" স্ক্রিন migrate করা শুরু

**বদলানো ফাইল:** শুধু `app/src/main/java/com/example/ui/screens/MessagesScreen.kt` (আর এই
প্রোগ্রেস নোট)। **কেন শুধু ১টা, ৩টা না:** ১৬টা list-shaped স্ক্রিনের মধ্যে যাচাই করে দেখা গেছে
১৩টাতেই ViewModel-এ `mutableStateListOf<T>()`-ভিত্তিক ম্যানুয়াল "load more" pagination আছে
(যেমন `favoriteSolversPaged`, `withdrawalsPaged`, `solverReviewsPaged` ইত্যাদি — নাম প্যাটার্ন
`*Paged` + `*LoadingMore` ফ্ল্যাগ)। এই ব্যাচে শুধু pagination-বিহীন স্ক্রিনই migrate করা হলো
(`ActiveJobsPopupScreen` আগেই হয়ে গেছে, `InstantJobsScreen`-ও pagination-বিহীন কিন্তু অনেক বড়/
জটিল বলে এই ব্যাচে নেওয়া হয়নি) — বাকি ২টা (ব্যাচ পূরণ করতে) pagination-যুক্ত স্ক্রিন থেকে বেছে
নেওয়ার আগে **একটা নতুন ডিজাইন প্রশ্ন** ব্যবহারকারীর কাছে তোলা দরকার (নিচে "পরের সেশনের জন্য"
দ্রষ্টব্য), তাই Ground Rule ১০ অনুযায়ী অনুমান করে এগোনো হয়নি।

**`MessagesScreen.kt`-এ কী বদলালো:** import `SyncAwareContent` → `SyncAwareRefreshableContent`;
আগে বাইরে গণনা করা `val threads = conversations.groupBy { it.problemId }` সরিয়ে content
lambda-র ভেতরে নেওয়া হয়েছে (`val threads = convos.groupBy { it.problemId }`, যেখানে `convos`
হলো পাস-করা `data = conversations`)। TopBar/BottomBar (এই কম্পোনেন্টের বাইরে) অপরিবর্তিত।
`isManualRefreshing = isRefreshing` (বিদ্যমান pull-to-refresh state) পাস করা হয়েছে, তাই
ম্যানুয়াল রিফ্রেশ + re-entry দুটো "extra feature"-ই (ধাপ ৫, ৬) স্বয়ংক্রিয়ভাবে কার্যকর হলো।

**যাচাই করা হয়েছে:**
- `grep` করে নিশ্চিত হওয়া হয়েছে ফাইলে আর কোনো `SyncAwareContent(` কল-সাইট নেই আর নতুন import ঠিকমতো
  বসেছে; বাকি সব `threads` রেফারেন্স content lambda-র ভেতরেই (স্কোপের বাইরে কোনো ব্যবহার নেই)।
- ফাইলের `(`/`{`/`[` ব্র্যাকেট ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে যাচাই করা হয়েছে (০ imbalance)।
- পুরো প্রজেক্ট আগের zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু এই ফাইল (+ এই
  প্রোগ্রেস নোট) বদলেছে, বাকি সব (dotfile সহ) বাইট-বাই-বাইট অপরিবর্তিত, ফাইল-সংখ্যা অভিন্ন।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের সব ধাপের মতোই)।

**⚠️ পরের সেশনের জন্য — নতুন ডিজাইন প্রশ্ন (ব্যবহারকারীকে জিজ্ঞেস করে ঠিক করতে হবে):**

বাকি ১৩টা list-shaped স্ক্রিনে (`AllOpenProblemsScreen`, `BidManagementScreen`,
`FavoriteSolversScreen`, `InstantJobHistoryScreen`, `SolverAllPostsScreen`,
`SolverCompletedJobsScreen`, `SolverMyBidsScreen`, `SolverProblemsScreen`, `SolverReviewsScreen`,
`TransactionHistoryScreen`, `UserProblemsScreen`, `UserReviewsScreen`, `WithdrawalHistoryScreen`)
তালিকাটা ViewModel-এর `mutableStateListOf<T>()`-ভিত্তিক (যেমন `favoriteSolversPaged`) —
ব্যবহারকারী নিচে স্ক্রল করলে আরো আইটেম যোগ হয় ("load more")। এই লিস্টটাকেই যদি সরাসরি
`SyncAwareRefreshableContent`-এর `data` হিসেবে পাস করা হয়, তাহলে প্রতিবার "load more" পেজ যোগ
হলেও (কারণ লিস্টের কনটেন্ট/আকার বদলে গেছে) পুরো `==` তুলনায় "ভিন্ন" ধরা হবে — অর্থাৎ ব্যবহারকারী
স্ক্রল করে পরের পেজ লোড করলেই ইতিমধ্যে দেখা পুরনো আইটেমগুলোও সহ পুরো তালিকা সংক্ষিপ্ত সময়ের জন্য
shimmer করবে, যেটা "load more" স্ক্রল অভিজ্ঞতার জন্য বিরক্তিকর/অপ্রত্যাশিত হতে পারে (আসল লক্ষ্য —
শুধু *realtime*-এ সত্যিকারের ডেটা বদল/re-entry/ম্যানুয়াল-রিফ্রেশে flash করা, pagination
scroll-load-এ না)।

সম্ভাব্য দুটো পথ (আগেরবার re-entry নিয়ে যেমন পথ A/B জিজ্ঞেস করা হয়েছিল, এটাও একইভাবে সিদ্ধান্ত
দরকার):

> **✅ সমাধান হয়ে গেছে (ধাপ ৭ ব্যাচ ২, নিচে দেখুন) — নিচের পথ C/D-এর কোনোটাই নেওয়া হয়নি।**
> ব্যবহারকারীর সিদ্ধান্ত: pagination স্ক্রিনেও কোনো ব্যতিক্রম ছাড়া সরাসরি
> `SyncAwareRefreshableContent`, "load more"-এ শিমার হওয়াটাই কাঙ্ক্ষিত। বিস্তারিত: Ground
> Rule ১১ (উপরে ধাপ ০-তে) আর নিচের "ধাপ ৭ (ব্যাচ ২)" এন্ট্রি দেখুন। নিচের পথ C/D আলোচনা এখন
> শুধু ঐতিহাসিক রেফারেন্স হিসেবে রাখা হলো (কেন এই প্রশ্নটা উঠেছিল বোঝার জন্য), অনুসরণ করার জন্য না।

- **পথ C:** pagination-যুক্ত স্ক্রিনে `SyncAwareRefreshableContent` migrate না করে আপাতত পুরনো
  `SyncAwareContent`-এই রেখে দেওয়া (শুধু cold-load/error/retry আচরণ পাবে, realtime-diff-flash/
  re-entry-flash/manual-refresh-flash কোনোটাই পাবে না) — সবচেয়ে কম-ঝুঁকিপূর্ণ, কিন্তু বেশিরভাগ
  স্ক্রিনই এই নতুন ফিচার থেকে বাদ পড়ে যাবে।
- **পথ D:** `SyncAwareRefreshableContent`-এ migrate করা, কিন্তু "load more" পেজ-যোগের সময়টুকু
  আলাদাভাবে detect করে সেই সময় flash suppress করার একটা নতুন প্যারামিটার/মেকানিজম যোগ করা (যেমন
  `isLoadingMore: Boolean` প্যারামিটার — সত্যি থাকা অবস্থায় data-change flash suppress হবে,
  শুধু pagination শেষ হওয়ার পরেই আবার স্বাভাবিক diff-check শুরু হবে)। এটা বেশি সঠিক আচরণ দেবে
  কিন্তু কম্পোনেন্টে নতুন জটিলতা যোগ করবে এবং প্রতিটা paginated স্ক্রিনে wiring লাগবে।

~~**পরের সেশনে শুরু করার আগে এই প্রশ্নটা ব্যবহারকারীকে জিজ্ঞেস করতে হবে** — তারপর যেটা বেছে নেওয়া
হবে সেই অনুযায়ী বাকি ১৩টা list-shaped স্ক্রিন ব্যাচে (৩টা করে) migrate করা যাবে। এর পাশাপাশি
single-value স্ক্রিনগুলো (`ProfileScreen`, `DashboardScreen` ইত্যাদি, যেখানে এই pagination
সমস্যা নেই) নিয়ে এগোনোও একটা বিকল্প — সেটাও ব্যবহারকারীর অগ্রাধিকার জিজ্ঞেস করে ঠিক করতে হবে।

---

## ধাপ ৭ (সংশোধন) — MessagesScreen-কে `SyncAwareRefreshableContent` থেকে ফেরত `SyncAwareContent`-এ আনা

**কারণ (ব্যবহারকারীর স্পষ্ট সিদ্ধান্ত):** উপরের ধাপ ৭ (ব্যাচ ১) এন্ট্রিতে যে migration হয়েছিল
(`SyncAwareContent` → `SyncAwareRefreshableContent`), সেটা MessagesScreen-এর জন্য অনাকাঙ্ক্ষিত
ছিল। ব্যবহারকারী স্পষ্টভাবে জানিয়েছেন — MessagesScreen-এ কোনো শিমার/ফ্ল্যাশ থাকবে না; শুধু app
session-এ প্রথমবার স্ক্রিনে ঢোকার সময় একবার loading/skeleton দেখাবে, তারপর realtime-এ নতুন মেসেজ
এলে বা re-entry হলেও আর কখনো skeleton/shimmer দেখাবে না। এই নিয়ম **শুধু MessagesScreen-এর জন্য
প্রযোজ্য** — অন্য কোনো স্ক্রিনে (`ActiveJobsPopupScreen` সহ) `SyncAwareRefreshableContent`-এর
ব্যবহার এতে বদলায়নি।

**বদলানো ফাইল:** শুধু `app/src/main/java/com/example/ui/screens/MessagesScreen.kt` (আর এই
প্রোগ্রেস নোট)।

**`MessagesScreen.kt`-এ কী বদলালো:** import `SyncAwareRefreshableContent` → `SyncAwareContent`
(ফেরত); কল-সাইটে `data = conversations` আর `isManualRefreshing = isRefreshing` প্যারামিটার দুটো
সরানো হয়েছে (এই কম্পোনেন্টে এই প্যারামিটার নেই); content lambda এখন কোনো আর্গুমেন্ট নেয় না (আগে
`{ convos -> ... }`, এখন `{ ... }`) — ভেতরের `val threads = convos.groupBy { ... }` বদলে
`val threads = conversations.groupBy { ... }` (সরাসরি `viewModel.userConversations`
StateFlow থেকে collect করা state ব্যবহার হচ্ছে, আলাদা data প্যারামিটার হিসেবে পাস করার দরকার নেই,
কারণ এখন আর কোনো diff/flash লজিক নেই)। `isRefreshing` variable এখনো ব্যবহৃত হচ্ছে (উপরের
`SomadhanPullToRefresh`-এর `isRefreshing` প্যারামিটারে), তাই আনইউজড হয়নি। TopBar/BottomNav (এই
কম্পোনেন্টের বাইরে) অপরিবর্তিত।

**যাচাই করা হয়েছে:**
- `grep` করে নিশ্চিত হওয়া হয়েছে ফাইলে আর কোনো `SyncAwareRefreshableContent(`/`convos` রেফারেন্স
  নেই, নতুন import ঠিকমতো বসেছে।
- ফাইলের `(`/`{`/`[` ব্র্যাকেট ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে যাচাই করা হয়েছে (০ imbalance)।
- পুরো প্রজেক্ট আগের zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু
  `MessagesScreen.kt` (+ এই প্রোগ্রেস নোট) বদলেছে, বাকি সব (dotfile সহ) বাইট-বাই-বাইট অপরিবর্তিত,
  ফাইল-সংখ্যা অভিন্ন (২৪৪/২৪৪)।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের সব ধাপের মতোই) — Android Studio-তে
  build/compile/run করে ভেরিফাই করা এখনো বাকি।

---

## ধাপ ৭ (ব্যাচ ২, ৩টা স্ক্রিন) — Pagination-প্রশ্নের সমাধান + প্রথম ৩টা pagination-স্ক্রিন migrate

**ব্যবহারকারীর সিদ্ধান্ত (Path C/D প্রশ্নের সমাধান):** আগে তোলা Path C (pagination স্ক্রিনে migrate
না করা) বনাম Path D (`isLoadingMore` দিয়ে flash suppress করা) — কোনোটাই বেছে নেওয়া হয়নি।
ব্যবহারকারী স্পষ্ট জানিয়েছেন: **এই ১৩টা pagination-যুক্ত স্ক্রিনেও বাকি সব স্ক্রিনের মতোই একই
standard প্যাটার্ন থাকবে, ব্যতিক্রম ছাড়া (MessagesScreen বাদে)।** অর্থাৎ next/prev ক্লিক,
scroll-to-load, বা "see more" ক্লিকে যখন নতুন পেজ/আইটেম যোগ হয়ে তালিকা বদলায়, তখন সংক্ষিপ্ত
শিমার হওয়াটাই কাঙ্ক্ষিত আচরণ (এটাকে annoying না, বরং professional practice হিসেবে ধরা হয়েছে) —
তাই **কোনো নতুন suppression প্যারামিটার (`isLoadingMore`) যোগ করা হয়নি**; সরাসরি
`SyncAwareRefreshableContent`-এ migrate করা হয়েছে, ঠিক অন্য স্ক্রিনগুলোর মতোই। Path C/D প্রশ্নটা
তাই বাতিল হয়ে গেছে — এই সিদ্ধান্তটাই ভবিষ্যতের বাকি ১০টা pagination-স্ক্রিনেও প্রযোজ্য হবে।

**বদলানো ফাইল (৩টা, ব্যাচ সাইজ অনুযায়ী):**

1. **`AllOpenProblemsScreen.kt`** — import `SyncAwareContent` → `SyncAwareRefreshableContent`।
   `val displayedProblems = if (isCurrentOpen) openProblemsList.toList() else inProgressProblems`
   কল-সাইটের *বাইরে* নেওয়া হয়েছে (data প্যারামিটার হিসেবে পাস করতে হবে বলে), `.toList()` দিয়ে
   `openProblemsList` (viewModel.openProblemsPaged, একটা mutableStateListOf) কে immutable
   snapshot করা হয়েছে — নাহলে একই mutable reference বারবার পাঠালে `!=` diff কাজ করতো না।
   `data = displayedProblems`, `isManualRefreshing = isRefreshing` যোগ হয়েছে। content lambda
   প্যারামিটার `probs`, ভেতরের সব `displayedProblems` রেফারেন্স `probs`-এ বদলানো হয়েছে।
2. **`BidManagementScreen.kt`** — একই import বদল। লক্ষণীয়: এই স্ক্রিনে pagination আসলে
   dead/stub (`inProgressHasMore`/`historyHasMore`/`*LoadingMore` সব হার্ডকোডেড `false`,
   ডেটা সরাসরি `allProblems.filter { ... }` থেকে আসা plain immutable list) — তাই
   `.toList()` লাগেনি, সরাসরি `data = pageProblems` পাস করা হয়েছে। content lambda
   প্যারামিটার `probs`।
3. **`FavoriteSolversScreen.kt`** — একই import বদল। `favoriteItems` ইতিমধ্যেই
   `remember(favoriteSolversPaged.toList(), allProblems, allRatings) { ... }` দিয়ে হিসাব করা
   plain immutable list, তাই সরাসরি `data = favoriteItems` পাস করা গেছে। **সতর্কতা:** content
   lambda-র প্যারামিটারের নাম `items` রাখা যায়নি — এই ফাইলে `androidx.compose.foundation.lazy.items`
   ফাংশনও ব্যবহৃত হয় (`items(favoriteItems, key = ...)`), `items` নামে প্যারামিটার নিলে সেটা
   শ্যাডো করে ফেলতো এবং কম্পাইল এরর হতো — তাই প্যারামিটারের নাম `favs` রাখা হয়েছে।

**⚠️ পূর্ব-বিদ্যমান পর্যবেক্ষণ (এই ব্যাচের স্কোপের বাইরে, শুধু নোট রাখা হলো):**
`FavoriteSolversScreen.kt`-এ `isRefreshing` একটা local `remember { mutableStateOf(false) }`
যেটা কোথাও `true`-তে সেট হয় না (`onRefresh = { viewModel.refreshData() }`-এ শুধু ডেটা রিফ্রেশ
হয়, ফ্ল্যাগ আপডেট হয় না) — তাই এই স্ক্রিনে `isManualRefreshing = isRefreshing` পাস করা হলেও
pull-to-refresh-pulse ফিচারটা বাস্তবে কখনো trigger হবে না, যতক্ষণ না এই pre-existing bug আলাদাভাবে
ঠিক করা হয় (এই কাজের স্কোপের বাইরে, ব্যবহারকারীকে জানিয়ে রাখা হলো)।

**যাচাই করা হয়েছে:**
- প্রতিটা ফাইলে আলাদাভাবে `(`/`{`/`[` ব্র্যাকেট ব্যালেন্স পাইথন স্ক্রিপ্ট দিয়ে (০ imbalance)।
- `grep` করে নিশ্চিত হওয়া হয়েছে তিনটা ফাইলেই আর কোনো পুরনো `SyncAwareContent(`/পুরনো ভ্যারিয়েবল
  নাম (`displayedProblems`, `pageProblems` শুধু ঘোষণায়, `favoriteItems` content lambda-র ভেতরে)
  অবশিষ্ট নেই।
- পুরো প্রজেক্ট আগের zip-এর সাথে রিকার্সিভ ফাইল-ডিফ করে নিশ্চিত করা হয়েছে — শুধু এই ৩টা ফাইল
  (+ এই প্রোগ্রেস নোট) বদলেছে, বাকি সব (dotfile সহ) বাইট-বাই-বাইট অপরিবর্তিত, ফাইল-সংখ্যা অভিন্ন।
- Gradle build এই sandbox-এ সম্ভব হয়নি (আগের সব ধাপের মতোই) — Android Studio-তে
  build/compile/run করে ভেরিফাই করা এখনো বাকি, বিশেষত `FavoriteSolversScreen.kt`-এর
  `favs` রিনেমিং আর `items(...)` শ্যাডোয়িং এড়ানো ঠিকমতো কম্পাইল হয় কিনা।

**পরের ব্যাচ (৩টা):** বাকি ১০টা pagination-স্ক্রিন — `InstantJobHistoryScreen`,
`SolverAllPostsScreen`, `SolverCompletedJobsScreen`, `SolverMyBidsScreen`, `SolverProblemsScreen`,
`SolverReviewsScreen`, `TransactionHistoryScreen`, `UserProblemsScreen`, `UserReviewsScreen`,
`WithdrawalHistoryScreen` — একই সিদ্ধান্ত (কোনো suppression ছাড়া সরাসরি migrate) অনুযায়ী পরের
সেশনে/ব্যাচে চালিয়ে যাওয়া হবে।
