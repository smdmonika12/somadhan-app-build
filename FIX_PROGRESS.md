# Somadhan Bug-Fix Progress Log

এই ফাইল `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এর progress log। প্রতিটা session তার কাজ শেষে এখানে
নতুন এন্ট্রি + HANDOFF নোট যোগ করবে (`CI_TEST_SUITE_PROGRESS.md`-এর একই format-এ)।

**⚠️ পরের session যেন সঠিক জায়গা থেকে শুরু করতে পারে, তাই প্রতিটা HANDOFF নোট এই ফরম্যাট মেনে চলবে
(master prompt-এর "0️⃣ Session বুটস্ট্র্যাপ" অংশ এটার উপরই নির্ভর করে):**
```
## 🔁 HANDOFF (তারিখ, Step X[.sub] সম্পূর্ণ/আংশিক)
- **পরের ধাপ: Step N[.sub]।** (কোন Step, স্পষ্ট নাম্বার সহ — Step 2-5/6-এর ভেতরে কোন sub-item বাকি তাও লিখতে হবে)
- এই সেশনে কী ফিক্স/টেস্ট হলো (১-২ লাইন)।
- Windows-verify লাগবে কিনা, লাগলে exact কমান্ড।
- `BUG_INVENTORY.md`-এ কোন সারির Status বদলেছে।
```

এখনো কোনো session চলেনি — `BUG_INVENTORY.md` বানানো হয়েছে (consolidated, CI_TEST_SUITE_PROGRESS.md
থেকে), `SOMADHAN_BUG_FIX_MASTER_PROMPT.md` তৈরি — Step 0 প্রথম অসম্পূর্ণ ধাপ।

## 🔁 HANDOFF (২০২৬-০৯-২৩, initial setup)
- **এখনো কোনো ফিক্স শুরু হয়নি। Step 0 প্রথম ধাপ (setup + re-confirm)।**
- `BUG_INVENTORY.md`-তে ৬টা বাগ ফিক্স-প্রয়োজন হিসেবে চিহ্নিত (গ্রুপ ১, ২ (৪টা উপ-বাগ), ৩) + ৫টা
  backlog আইটেম (গ্রুপ ৭)।
- সবচেয়ে জরুরি (money-critical, ব্যবহারকারী নিজে দুটো উদাহরণ দিয়ে confirm করেছেন): **Step 1 —
  realtime subscription re-scoping**।

## ✅ Step 0 সম্পূর্ণ (২০২৬-০৯-২৩, এই সেশন) — Setup + re-confirm, কোনো কোড এডিট হয়নি

**যা verify করা হলো:**
- `BUG_INVENTORY.md`-কে `CI_TEST_SUITE_PROGRESS.md` (৯৭৭৯+ লাইন)-এর বিপরীতে cross-check করা
  হলো — বিশেষভাবে Step 17 (Edge Function/Deno) এবং Step 20 (money-critical audit, ~১০৩ কল-সাইট)
  ঘেঁটে দেখা হলো কোনো ছড়ানো finding বাদ পড়েছে কিনা।
  - **Step 17.1–17.3:** শুধু test-coverage (Deno `admin-reset-user-password` authz/validation),
    কোনো নতুন app-bug না। একটা known/unresolved সীমাবদ্ধতা আছে (`is_admin()`-এর real parameter-নাম
    অনিশ্চিত, inferred schema-stub-ভিত্তিক) কিন্তু এটা schema-verification গ্যাপ, confirmed বাগ না
    — তাই `BUG_INVENTORY.md`-এ নতুন সারি হিসেবে যোগ করা হলো না (rule ৩ অনুযায়ী শুধু প্রকৃত বাগ/গ্যাপ
    backlog-এ যোগ হয়)।
  - **Step 20.1–20.5:** ~১০৩টা dual-write কল-সাইট পুরোপুরি অডিট করে **০টা** নতুন 🔴/🟠 পাওয়া গেছে
    (সবগুলোই ⚪ non-money নিশ্চিত) — তাই Step 20 থেকে `BUG_INVENTORY.md`-তে যোগ করার মতো কিছু নেই।
  - বাকি সব Step (1–16, 18–19) ইতিমধ্যে `BUG_INVENTORY.md`-এর গ্রুপ ১–৭-এ সঠিকভাবে consolidated
    আছে বলে নিশ্চিত হওয়া গেছে (৬টা fix-প্রয়োজনীয় বাগ + ৫টা backlog আইটেম, কোনোটা বাদ পড়েনি)।
- **গ্রুপ ৪ (UI transaction sign) re-confirm:** এই sandbox-এ Android/Gradle build tooling-এর জন্য
  প্রয়োজনীয় network access (Google/Maven repositories) নেই (network allowlist-এ শুধু pypi/npm/
  crates/github আছে), তাই এই সেশনে real `.\gradlew.bat test` চালানো সম্ভব হয়নি। `CI_TEST_SUITE_
  PROGRESS.md`-এর Step 20.5 এন্ট্রি অনুযায়ী `TransactionDisplaySignTest`/`WalletDashboardSignWiringTest`
  (২৮/২৮) ইতিমধ্যেই real Windows-run দিয়ে pass কনফার্মড ছিল — সেই রেকর্ডকেই বহাল রাখা হলো, নতুন করে
  re-run প্রয়োজন নেই যতক্ষণ না ব্যবহারকারী নিজের Windows মেশিনে চালিয়ে ভিন্ন কিছু না দেখান।
- **ফলাফল: `BUG_INVENTORY.md`-এ কোনো এডিট লাগেনি** (rule অনুযায়ী এই Step-এ কোনো কোড এডিটও হয়নি)।

- **পরের ধাপ: Step 1 — 🔴 Realtime subscription re-scoping (money-critical, সর্বোচ্চ অগ্রাধিকার)।**
- এই সেশনে কী হলো: শুধু Step 0 cross-check (কোনো কোড/টেস্ট বদলায়নি), master prompt-এ Step 0 `[x]`।
- Windows-verify লাগবে কিনা: না (এই Step-এ কোনো কোড বদলায়নি)।
- `BUG_INVENTORY.md`-এ কোনো সারির Status বদলায়নি।

## ✅ Step 1 — কোড-ফিক্স সম্পূর্ণ (২০২৬-০৯-২৩, এই সেশন) — Realtime subscription re-scoping, Windows-verify বাকি

**ফিক্স করার আগে যা করা হলো (rule #২):** `BUG_INVENTORY.md`-এর গ্রুপ ১.১ সারি আবার পড়া হলো, তারপর
`grep -n` দিয়ে `SomadhanViewModel.kt`-এ ৫টা ফাংশনের বর্তমান লাইন-নাম্বার বের করা হলো (আগের সেশনের
লেখা লাইন-নাম্বার ~১৯২০ ইত্যাদির মতো stale হতে পারত, তাই নতুন করে খোঁজা হলো — পাওয়া গেছে:
`completeLoginAfterOtp` ২৭৭৮, `loginAsAdmin` ২৯৫০, `logout` ৩০২৪, `switchRoleToSolver` ৩২১০,
`switchRoleToUser` ৩২৪৩)। `CI_TEST_SUITE_PROGRESS.md`-এর Step 18.2/18.3/18.4 সেকশন পুরোটা পড়ে
root-cause আবার confirm করা হলো (per-user `user:<uuid>` broadcast channel প্রথম login-এই আটকে
থাকা, `SupabaseRealtimeManager.startRealtimeListeners()` নিজেই idempotent ও নিজে থেকে
`isCurrentSessionAdmin()` চেক করে admin-এর জন্য notifications broadcast স্কিপ করে — rule #৪ অক্ষত
রাখে)।

**যা বদলেছে (rule #৩, ন্যূনতম টার্গেটেড এডিট — শুধু এই ৫টা ফাংশন, প্রতিটাতে ১-৩ লাইন যোগ, কিছু
মুছে/refactor করা হয়নি):**
- `completeLoginAfterOtp()` — `observeUserData()`-এর ঠিক আগে `SupabaseRealtimeManager.startRealtimeListeners()` (runCatching + log, বাকি কোডের প্যাটার্নে)।
- `loginAsAdmin()` — `onReady()`-এর ঠিক আগে একই কল (admin-এর জন্য broadcast topic নিজে থেকেই স্কিপ হবে)।
- `logout()` — `SupabaseAuthManager.signOut()`-এর পরের লাইনেই `SupabaseRealtimeManager.stopRealtimeListeners()` (একই `viewModelScope.launch` ব্লকে, যেহেতু suspend fun)।
- `switchRoleToSolver()`/`switchRoleToUser()` — `observeUserData(updated.id)`-এর ঠিক পরে একই `startRealtimeListeners()` কল (role বদলালেও user id অপরিবর্তিত থাকে, তাই এখানে মূলত pre-emptive re-scope, drift এড়াতে)।

**কীভাবে verify করা হলো (rule #৪):** `RealtimeSubscriptionScopeTest.kt`-এর
`accountSwitchFunctionsShouldRescopeBroadcast` টেস্টের ঠিক একই regex লজিক (member-fun boundary
detection + `startRealtimeListeners()|stopRealtimeListeners()` regex) Python দিয়ে reproduce করে
সম্পাদিত ফাইলের বিপরীতে চালানো হলো (sandbox-এ network না থাকায় real Gradle/JVM রান সম্ভব না) —
**৫টা ফাংশনেরই region-এ এখন match পাওয়া গেছে** (আগে ০টায় ছিল)। test class-এর বাকি ৩টা drift-guard
টেস্ট (`startUserTopicBroadcastSubscription`/`attachDatabase`/`AppDatabase` singleton pattern)
এই সেশনে কিছু ছোঁয়া হয়নি বলে অপরিবর্তিত থাকার কথা।

**rule #৭ অনুযায়ী manual test checklist:** নতুন ফাইল `REALTIME_RESCOPING_STEP1_TEST_CHECKLIST.md` —
৩টা user-confirmed উপসর্গ (bid-accept reset, release-এর পর balance না-আপডেট, withdrawal reset) +
role-switch + admin-login regression, একই ডিভাইসে logout/login করে verify করার জন্য।

**Windows-verify লাগবে, exact কমান্ড:**
```
.\gradlew.bat test --tests "*RealtimeSubscriptionScopeTest*" --stacktrace
```
প্রত্যাশা: ৪/৪ pass। এছাড়া `REALTIME_RESCOPING_STEP1_TEST_CHECKLIST.md`-এর manual checklist
(কমপক্ষে সেকশন ১-৩, user-confirmed উপসর্গ) বাস্তব ডিভাইসে করে দেখা ভালো।

**`BUG_INVENTORY.md` Status:** গ্রুপ ১.১ 🔴 → 🟡 (FIXED, NOT VERIFIED — কোনো real Windows-run এখনো
confirm করেনি)। সারসংক্ষেপ টেবিলও আপডেট হয়েছে।

**Master prompt checkbox:** Step 1 এখনো `[ ]` (rule #৫ অনুযায়ী — শুধু real+verified হলে `[x]` হয়),
তবে টাইটেলে 🔴 থেকে 🟡 করে নোট যোগ করা হয়েছে যে কোড-কাজ শেষ।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 1 কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি) — [পরের এন্ট্রিতে superseded]
- এই এন্ট্রিটা এই সেশনেরই পরের অংশে (নিচে) WINDOWS RESULT প্রসেস করার পর updated হয়েছে — নিচের
  সর্বশেষ HANDOFF-ই বিশ্বাস করতে হবে (rule অনুযায়ী সবচেয়ে শেষেরটাই সত্য)।
- Windows-verify কমান্ড ছিল: `.\gradlew.bat test --tests "*RealtimeSubscriptionScopeTest*" --stacktrace`
  (এটা ভুল ছিল — নিচের এন্ট্রিতে সঠিক কমান্ড ও ফলাফল লেখা আছে)।

## ✅ Step 1 — WINDOWS RESULT প্রসেস করা হলো (২০২৬-০৯-২৩, একই সেশন) — Windows-verified, Step 1 সম্পূর্ণ

ব্যবহারকারী real Windows PowerShell আউটপুট পেস্ট করেছেন (rule অনুযায়ী নতুন কোনো Step নেওয়ার আগে
প্রথমে প্রসেস করা হলো)।

**পথে যা সমস্যা হয়েছিল (রেফারেন্সের জন্য, ভবিষ্যতে কাজে লাগবে):**
- প্রথমে `JAVA_HOME` সেট ছিল না — `C:\Program Files\Android\Android Studio\jbr` (Android Studio-এর
  bundled JDK) ব্যবহার করে ঠিক হয়েছে।
- PowerShell-এ current-folder script চালাতে `.\` লাগে (`gradlew.bat` একা কাজ করে না)।
- Android-প্রজেক্টের `test` lifecycle-task `--tests` ফিল্টার নেয় না (এটা `Test` টাইপের টাস্ক না,
  শুধু `testDebugUnitTest`/`testReleaseUnitTest`-এর group) — সরাসরি `testDebugUnitTest` টাস্ক
  টার্গেট করে ঠিক হয়েছে (এখন থেকে ভবিষ্যতের সব Windows-verify কমান্ডে `testDebugUnitTest` ব্যবহার
  করা হবে, শুধু `test` না)।

**ফলাফল:** `> Task :app:compileDebugKotlin` সফল (শুধু pre-existing deprecation warning, কোনো নতুন
কম্পাইল এরর না — Step 1-এর এডিট syntax-এর দিক থেকে ঠিক আছে তার প্রমাণ), তারপর
`BUILD SUCCESSFUL in 2m 48s`। `--tests` ফিল্টার real ম্যাচ না পেলে বা কোনো টেস্ট fail করলে Gradle
`BUILD FAILED` দেখাত (Gradle-এর standard আচরণ) — তাই `BUILD SUCCESSFUL` মানেই
`RealtimeSubscriptionScopeTest`-এর ৪টা টেস্টই pass করেছে, Step 1-এর মূল bug-demonstrating assertion
সহ।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ১.১ সারি: 🟡 → **✅ FIXED + VERIFIED**। সারসংক্ষেপ টেবিলও আপডেট হলো।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 1 checkbox: `[ ]` → **`[x]`**।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 1 সম্পূর্ণ ✅ Windows-verified) — পরের ধাপ Step 2
- **পরের ধাপ: Step 2 — 🔴 Admin panel ক্লাস A: Overview metrics permanent-override (গ্রুপ ২.১)।**
  `adminDashboardMetrics`-এর ১০টা `if (supa.X > 0) supa.X else local.X` প্যাটার্ন বাদ দিয়ে সবসময়
  local reactive aggregate ব্যবহার করতে হবে (`SomadhanViewModel.kt`, লাইন ~১৯২০-২০১২, grep দিয়ে
  নতুন করে exact লোকেশন বের করতে হবে)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 2 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- এই সেশনে Step 1 পুরোপুরি সম্পূর্ণ হলো: কোড-ফিক্স + real Windows `gradlew testDebugUnitTest` run
  দিয়ে pass কনফার্মড (`BUILD SUCCESSFUL`)। `REALTIME_RESCOPING_STEP1_TEST_CHECKLIST.md`-এর manual
  checklist (বাস্তব ডিভাইসে bid-accept/release/withdrawal scenario) এখনো ব্যবহারকারীর নিজের ইচ্ছায়
  optional — কোড ও automated test দুটোই pass করায় Step 1-এ blocking কিছু বাকি নেই।
- **পরবর্তী Windows-verify কমান্ডে `testDebugUnitTest` ব্যবহার করতে হবে (শুধু `test` না)**, আর
  `JAVA_HOME`/`ANDROID_HOME` env var সেট করে নিতে হবে প্রতিটা নতুন PowerShell window-এ।
- `BUG_INVENTORY.md`-এ গ্রুপ ১.১ ও সারসংক্ষেপ টেবিল: 🟡 → ✅।

## ✅ Step 2 — Admin panel ক্লাস A: Overview metrics permanent-override (২০২৬-০৯-২৩) — কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি

ব্যবহারকারীর "Continue next step" মেসেজে কোনো `WINDOWS RESULT:` ছিল না, তাই সরাসরি Step 2 (rule #৯
অনুযায়ী আগের HANDOFF-এ যে confirmation চাওয়া হয়েছিল, এই মেসেজেই সেটা পাওয়া গেছে) নেওয়া হলো।

**Bootstrap-এর ৩টা pre-edit verification (কোনো কোড ছোঁয়ার আগে):**
1. zip ফাইল-সংখ্যা/গঠন — sanity-check করা হয়েছে, আগের সেশনের "যা বদলেছে" লিস্টের সাথে সঙ্গতিপূর্ণ।
2. `grep -n "adminDashboardMetrics" SomadhanViewModel.kt` → লাইন ১৯২০, `BUG_INVENTORY.md`-এর
   "~১৯২০-২০১২" রেফারেন্সের সাথে হুবহু মিলেছে (stale হয়নি)।
3. `CI_TEST_SUITE_PROGRESS.md`-এর Step 19.6 সেকশন (regression test + চূড়ান্ত রুট-কজ রিপোর্ট +
   প্রস্তাবিত ফিক্স, "Option খ") আবার পড়া হয়েছে — শুধু `BUG_INVENTORY.md`-এর সংক্ষিপ্ত সারাংশের উপর
   ভিত্তি করে শুরু করা হয়নি।

**যা বদলানো হয়েছে:**
- `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt` — `adminDashboardMetrics`
  (val block, লাইন ১৯২০-২০১০)-এর ভেতরে ১০টা `if (supa.totalUsers > 0) supa.X else local.X`
  (আর ৩টা কম্পাউন্ড ভ্যারিয়েন্ট — totalVolume/platformRev/compWith) প্যাটার্ন বাদ দিয়ে সবসময়
  local reactive aggregate (`localUsers.size`, `localProblems.count { ... }` ইত্যাদি) ব্যবহার করা
  হয়েছে — ঠিক CI_TEST_SUITE Step 19.6-এর "Option খ" প্রস্তাব অনুযায়ী। `supa` প্যারামিটার এখনো ব্যবহৃত
  হয় `catProbCounts`/`catBidCounts`-এ (এই বাগের স্কোপের বাইরে, ছোঁয়া হয়নি)।
- `app/src/test/java/com/example/ui/viewmodel/AdminPanelStep19RegressionTest.kt` —
  `testOverviewMetricsPermanentOverrideBugStillPresent` রিনেম করে
  `testOverviewMetricsPermanentOverrideBugFixed` করা হয়েছে (master prompt-এর নিজের Step 2 নোট
  অনুযায়ী — "StillPresent" নামটা আর সত্যি না)। assertion-এর লজিক (`overrideCount == 0`) অপরিবর্তিত
  আছে, শুধু নাম আর ব্যাখ্যা-মন্তব্য আপডেট হয়েছে (প্রত্যাশিত ফলাফল এখন pass, আগে ইচ্ছাকৃতভাবে fail)।

**Verification (sandbox-এ, network নেই বলে real Gradle চালানো যায়নি — Step 18.3/19.6-এর একই
সীমাবদ্ধতা):**
- Python দিয়ে test-এর `valBlockRegion`/regex লজিক হুবহু পোর্ট করে ফিক্সড কোডের উপর চালানো হয়েছে —
  `adminDashboardMetrics` ব্লকে `if\s*\(\s*supa\.totalUsers\s*>\s*0\s*\)` প্যাটার্নের সংখ্যা এখন
  **০** (আগে ১০ ছিল) → `testOverviewMetricsPermanentOverrideBugFixed` এখন pass করার কথা।
- Kotlin syntax manually re-check করা হয়েছে (brace/paren balance, `supa` প্যারামিটার এখনো
  downstream-এ ব্যবহৃত থাকায় unused-parameter warning হওয়ার কথা না)।
- ফাইলের বাকি ৭টা টেস্ট (withdrawal no-op, stuck spinner, paged-cache staleness — Step 3-5-এর
  স্কোপ) এই সেশনে ছোঁয়া হয়নি, তাই তাদের pass/fail polarity অপরিবর্তিত থাকার কথা।
- **real Windows verify কমান্ড বাকি:**
  `.\gradlew.bat testDebugUnitTest --tests "com.example.ui.viewmodel.AdminPanelStep19RegressionTest" --stacktrace`
  (JAVA_HOME/ANDROID_HOME সেট করে) — `BUILD SUCCESSFUL` হলে Step 2 পুরোপুরি ✅ ধরা হবে।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.১ সারি: 🔴 → **🟡 FIXED, NOT VERIFIED** (real Windows run কনফার্ম করেনি
  এখনো)। test-নাম রেফারেন্সও আপডেট করা হয়েছে। "প্রস্তাবিত ফিক্স" বুলেট-লিস্টেও ২.১-এর পাশে ✅ প্রয়োগ
  হয়েছে নোট যোগ। সারসংক্ষেপ টেবিলের গ্রুপ ২ সারি: এখন "🟡🔴🔴🔴 (২.১ fixed-not-verified, ২.২–২.৪ বাকি)"।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 2 checkbox **এখনো `[ ]`** (rule #৫ অনুযায়ী — শুধু
  real+verified হলে `[x]` হয়) — টাইটেলে নোট যোগ করা হয়েছে যে কোড-কাজ শেষ, Windows-verify বাকি।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 2 কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি) — পরের ধাপ: Step 2-এর WINDOWS RESULT অথবা (যদি ব্যবহারকারী verify স্কিপ করে এগোতে চান) Step 3
- **যদি পরের মেসেজে `WINDOWS RESULT:` আসে** (উপরের কমান্ডের আউটপুট) — সেটা প্রথমে প্রসেস করতে হবে:
  pass হলে `BUG_INVENTORY.md`-এ গ্রুপ ২.১: 🟡 → ✅, master prompt Step 2 checkbox `[ ]` → `[x]`,
  তারপর rule #৯ অনুযায়ী Step 3 শুরুর জন্য explicit confirmation চাইতে হবে। fail হলে Step 2 আবার
  খুলে root-cause ধরতে হবে (Step 3-এ যাওয়া যাবে না)।
- **যদি কোনো WINDOWS RESULT ছাড়াই ব্যবহারকারী এগোতে বলেন** — Step 2 এখনো 🟡 (not verified) অবস্থায়
  থাকবে, কিন্তু GATE rule অনুযায়ী Step 3 নেওয়ার জন্য blocking না (GATE শুধু checkbox `[x]` চায় না,
  "আগের Step সম্পূর্ণ" বোঝায়; এখানে ambiguity আছে — master prompt স্পষ্ট করে বলেনি 🟡-অবস্থায় পরের
  Step নেওয়া যাবে কিনা, তাই এই কেসে **ব্যবহারকারীকে জিজ্ঞেস করে** এগোনো নিরাপদ, অনুমান করে না)।
- **Step 3 — 🔴 Admin panel ক্লাস B১: Withdrawal status silent no-op (গ্রুপ ২.২)।**
  `updateWithdrawalStatus()`-এর status-guard fail হলে caller-কে জানানো লাগবে (sealed
  `WithdrawalUpdateResult`, CI_TEST_SUITE_PROGRESS.md Step 19.6-এ diff দেওয়া আছে),
  `adminUpdateWithdrawalStatus()` ও bulk-approve dialog দুটোতেই conditional toast।
  `SomadhanRepository.updateWithdrawalStatus()`, `SomadhanViewModel.kt` লাইন ৫৭০৪-৫৭১১ (grep দিয়ে
  নতুন করে exact লোকেশন বের করতে হবে), `AdminWithdrawalsView.kt:499-513`।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 3 শুরুর আগে।

## ✅ Step 2 — WINDOWS RESULT প্রসেস করা হলো (২০২৬-০৯-২৩, একই সেশন) — Windows-verified, Step 2 সম্পূর্ণ

ব্যবহারকারী real Windows PowerShell আউটপুট পেস্ট করেছেন (rule অনুযায়ী নতুন কোনো Step নেওয়ার আগে
প্রথমে প্রসেস করা হলো)।

**ফলাফল বিশ্লেষণ:** `compileDebugKotlin` সফল (শুধু pre-existing deprecation warning, Step 2-এর
এডিট syntax-এর দিক থেকে ঠিক)। `testDebugUnitTest` টাস্ক **৮টার মধ্যে ৪টা টেস্ট FAILED** দেখিয়ে
`BUILD FAILED` দিয়েছে — কিন্তু এই ৪টা ব্যর্থতা যাচাই করে দেখা গেছে **Step 2-এর টেস্ট না**:
- `testAdminWithdrawalsPagedStalenessBugStillPresent`
- `testReconcileAndRepairStuckSpinnerBugStillPresent`
- `testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent`
- `testWithdrawalStatusUpdateSwallowsFailureBugStillPresent`

এই চারটাই যথাক্রমে গ্রুপ ২.৪, ২.৩, ২.৪, ২.২-এর bug-demonstrating test — যেগুলো Step 3/4/5-এর কাজ,
এই সেশনে touch করা হয়নি, তাই এখনো fail করাই প্রত্যাশিত ছিল (rule #1 অনুযায়ী শুধু ঠিক সেই একটা
sub-step-এর কোড বদলানো হয়, বাকিগুলো ইচ্ছাকৃতভাবে অস্পৃষ্ট)। **`testOverviewMetricsPermanentOverrideBugFixed`
(Step 2-এর নিজের টেস্ট, আগের সেশনে রিনেম করা) এই ৪টা ব্যর্থ-তালিকায় নেই** — অর্থাৎ সেটা **pass করেছে**,
যেটা sandbox-এর static regex-simulation-এর পূর্বাভাসের সাথে হুবহু মেলে (overrideCount 10 → 0)।

**উপসংহার:** Step 2-এর ফিক্স real Gradle run-এ pass কনফার্মড। `BUILD FAILED` শুধু Step 3-5-এর
এখনো-না-হওয়া কাজের কারণে, Step 2-এর কোনো রিগ্রেশন না।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.১ সারি: 🟡 → **✅ FIXED + VERIFIED**। সারসংক্ষেপ টেবিলের গ্রুপ ২ সারি
  আপডেট: "✅🔴🔴🔴 (২.১ fixed+verified, ২.২–২.৪ বাকি)"।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 2 checkbox: `[ ]` → **`[x]`**।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 2 সম্পূর্ণ ✅ Windows-verified) — পরের ধাপ Step 3
- **পরের ধাপ: Step 3 — 🔴 Admin panel ক্লাস B১: Withdrawal status silent no-op (গ্রুপ ২.২)।**
  `updateWithdrawalStatus()`-এর client-side status-guard fail হলে caller-কে জানাতে হবে (sealed
  `WithdrawalUpdateResult` টাইপ, `CI_TEST_SUITE_PROGRESS.md` Step 19.6-এ diff দেওয়া আছে),
  `adminUpdateWithdrawalStatus()` ও bulk-approve dialog দুটোতেই conditional toast করতে হবে।
  `SomadhanRepository.updateWithdrawalStatus()`, `SomadhanViewModel.kt` লাইন ৫৭০৪-৫৭১১ (grep দিয়ে
  নতুন করে exact লোকেশন বের করতে হবে), `AdminWithdrawalsView.kt:499-513`। Target test:
  `testWithdrawalStatusUpdateSwallowsFailureBugStillPresent` (এই সেশনের Windows-run-এই confirmed
  fail করছে, ঠিক যেভাবে প্রত্যাশিত)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 3 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- **পরবর্তী Windows-verify কমান্ডে `testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*"`
  ব্যবহার করা হয়েছে এই সেশনে, ঠিকভাবে কাজ করেছে** — একই কমান্ড প্যাটার্ন Step 3-এর পরেও কাজে লাগবে,
  শুধু ফলাফলে ব্যর্থ-তালিকা এক ঘর ছোট হওয়ার কথা (৪টা থেকে ৩টা)।
- `BUG_INVENTORY.md`-এ গ্রুপ ২.১ ও সারসংক্ষেপ টেবিল: 🟡 → ✅।

## ✅ Step 3 — কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি (২০২৬-০৯-২৩, নতুন সেশন)

ব্যবহারকারী "ha continue" বলে rule #৯-এর confirmation দিয়েছেন। কোনো `WINDOWS RESULT:` এই সেশনের
মেসেজে ছিল না।

**কী বদলেছে:**
- `SomadhanRepository.kt` (এখন লাইন ~৫৬): নতুন `sealed class WithdrawalUpdateResult` (`Updated` /
  `GuardBlocked(reason: String)`), class-এর ঠিক উপরে, `OtpSendResult`/`OtpVerifyResult`
  (`util/OtpService.kt`)-এর একই কনভেনশনে।
- `SomadhanRepository.kt::updateWithdrawalStatus()` (এখন লাইন ~৭৪৭৮, আগের grep-করা লাইন-নাম্বার
  শিফট হয়ে গিয়েছিল): রিটার্ন টাইপ `Unit` → `WithdrawalUpdateResult`। দুটো guard branch
  (terminal-state COMPLETED/REJECTED, non-PENDING) এখন `GuardBlocked(reason)` রিটার্ন করে (আগে
  silent `return`)। প্রতিটা সফল mutation path (TrxID-only আপডেট, REJECTED সম্পূর্ণ, COMPLETED
  সম্পূর্ণ) এখন `WithdrawalUpdateResult.Updated` রিটার্ন করে। একটা নতুন fallback
  `GuardBlocked("অসমর্থিত স্ট্যাটাস")` যোগ হয়েছে targetStatus REJECTED/COMPLETED কোনোটাই না হলে
  (আগে implicit `Unit` — টাইপ-সেফটির জন্য দরকার ছিল)।
- `SomadhanViewModel.kt`: import যোগ (`WithdrawalUpdateResult`)। `adminUpdateWithdrawalStatus()`
  (লাইন ~৫৭৩০) এখন `when (result) { Updated -> showToast("...আপডেট হয়েছে...") ; GuardBlocked ->
  showToast(result.reason) }` — আগে unconditional success toast ছিল।
- `AdminWithdrawalsView.kt` bulk-approve dialog confirmButton (লাইন ~৪৯৭-৫১৩): redundant
  unconditional `Toast.makeText("N-টি সফলভাবে অনুমোদন করা হয়েছে")` বাদ দেওয়া হয়েছে — প্রতিটা আইটেমের
  real status এখন `onUpdateStatus()` → ViewModel-এর নিজস্ব conditional toast দিয়েই জানানো হয়
  (single-item approve/reject dialog দুটোতে আগে থেকেই এই redundant toast ছিল না, শুধু bulk dialog-এ
  ছিল)।

**Test:** `AdminPanelStep19RegressionTest.kt::testWithdrawalStatusUpdateSwallowsFailureBugStillPresent`
→ rule অনুযায়ী (Step 2-এর precedent) রিনেম করা হয়েছে `testWithdrawalStatusUpdateSurfacesFailureBugFixed`-এ,
assertion অপরিবর্তিত (শুধু নাম/মন্তব্য আপডেট, ঠিক Step 2-এর প্যাটার্নে)। Sandbox real Gradle চালাতে
পারে না (network নেই), তাই টেস্টের regex logic Python-এ replicate করে বর্তমান
`adminUpdateWithdrawalStatus()` ফাংশনের টেক্সটের বিপরীতে যাচাই করা হয়েছে: `callsRepo=True`,
`unconditionalToastRightAfter=False` — দুটোই প্রত্যাশিতভাবে pass করার কথা। Brace-balance sanity-check
(edited ৪টা ফাইলেই open==close) পাস করেছে।

**Windows-verify লাগবে (ব্যবহারকারীর মেশিনে):**
```
.\gradlew.bat testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*"
```
প্রত্যাশিত: `testWithdrawalStatusUpdateSurfacesFailureBugFixed` pass করবে; বাকি ৩টা bug-demonstrating
টেস্ট (গ্রুপ ২.৩, ২.৪ x2, Step 4/5-এর কাজ) আগের মতোই fail করবে (৪টা থেকে ৩টা ব্যর্থতা)। rule #৭
Step 1-এর জন্য নির্দিষ্ট (money-critical realtime), Step 3-এর জন্য প্রযোজ্য না — তাই আলাদা manual
checklist বানানো হয়নি।

**Status আপডেট (কোড-ফিক্স সম্পূর্ণ, এখনো Windows-verified না):**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.২ সারি: 🔴 → 🟡 (FIXED, NOT VERIFIED)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 3 checkbox এখনো `[ ]` (Windows real-run pass
  কনফার্মড না হওয়া পর্যন্ত `[x]` হবে না, Step 1/2-এর precedent অনুযায়ী)।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 3 কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি) — পরের ধাপ: Step 3-এর WINDOWS RESULT অথবা (যদি ব্যবহারকারী verify স্কিপ করে এগোতে চান) Step 4
- **যদি পরের মেসেজে `WINDOWS RESULT:` আসে** (উপরের কমান্ডের আউটপুট) — সেটা প্রথমে প্রসেস করতে হবে:
  pass হলে `BUG_INVENTORY.md`-এ গ্রুপ ২.২: 🟡 → ✅, master prompt Step 3 checkbox `[ ]` → `[x]`,
  তারপর rule #৯ অনুযায়ী Step 4 শুরুর জন্য explicit confirmation চাইতে হবে। fail হলে Step 3 আবার
  খুলে root-cause ধরতে হবে (Step 4-এ যাওয়া যাবে না)।
- **Step 4 — 🔴 Admin panel ক্লাস B২: Reconcile/repair stuck spinner (গ্রুপ ২.৩)।**
  `adminRepairMissingRefunds()`/`adminReconcileBalances()`-এর `catch` ব্লকে `onComplete()` যোগ করা।
  `SomadhanViewModel.kt` লাইন ~৬৪৬৭-৬৫৪৩ (grep দিয়ে নতুন করে exact লোকেশন বের করতে হবে — Step 3-এর
  এডিটে ফাইলে ~১০ লাইন যোগ হয়েছে, তাই এই লাইন-নাম্বার শিফট হয়ে থাকতে পারে)। Target test:
  `testReconcileAndRepairStuckSpinnerBugStillPresent`।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 4 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।

## ✅ Step 3 — WINDOWS RESULT প্রসেস করা হলো (২০২৬-০৯-২৩, একই সেশন) — Windows-verified, Step 3 সম্পূর্ণ

ব্যবহারকারী real Windows PowerShell আউটপুট পেস্ট করেছেন (rule অনুযায়ী নতুন কোনো Step নেওয়ার আগে
প্রথমে প্রসেস করা হলো)। এই সেশনে setup-সমস্যাও ছিল: প্রথম চেষ্টায় `JAVA_HOME` সেট না করেই
`Set-Content local.properties "sdk.dir=..."` চালানোয় PowerShell স্ট্রিং-টাকে কমান্ড হিসেবে পার্স
করার চেষ্টা করেছিল (কোড-সম্পর্কহীন, ব্যবহারকারীর কমান্ড-অর্ডার ভুল ছিল) — এরপর `JAVA_HOME`/`PATH`
সেট করে আবার `Set-Content` চালিয়ে ঠিক হয়ে গেছে, তারপর টেস্ট কমান্ড সফলভাবে চলেছে।

**ফলাফল বিশ্লেষণ:** `compileDebugKotlin` সফল (শুধু pre-existing deprecation warning, Step 3-এর
এডিট syntax-এর দিক থেকে ঠিক)। `testDebugUnitTest` টাস্ক **৮টার মধ্যে ৩টা টেস্ট FAILED** দেখিয়ে
`BUILD FAILED` দিয়েছে (Step 2-এর সেশনে ৪টা ছিল, এখন ৩টা — ঠিক ১ ঘর কমেছে, প্রত্যাশিত অনুযায়ী) —
এই ৩টা যাচাই করে দেখা গেছে **Step 3-এর টেস্ট না**:
- `testAdminWithdrawalsPagedStalenessBugStillPresent` (গ্রুপ ২.৪, Step 5-এর কাজ)
- `testReconcileAndRepairStuckSpinnerBugStillPresent` (গ্রুপ ২.৩, Step 4-এর কাজ)
- `testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent` (গ্রুপ ২.৪, Step 5-এর কাজ)

**`testWithdrawalStatusUpdateSurfacesFailureBugFixed` (Step 3-এর নিজের টেস্ট, এই সেশনে রিনেম করা)
এই ৩টা ব্যর্থ-তালিকায় নেই** — অর্থাৎ সেটা **pass করেছে**, যেটা sandbox-এর static regex-simulation-এর
পূর্বাভাসের সাথে হুবহু মেলে (`callsRepo=True`, `unconditionalToastRightAfter=False`)।

**উপসংহার:** Step 3-এর ফিক্স real Gradle run-এ pass কনফার্মড। `BUILD FAILED` শুধু Step 4-5-এর
এখনো-না-হওয়া কাজের কারণে, Step 3-এর কোনো রিগ্রেশন না।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.২ সারি: 🟡 → **✅ FIXED + VERIFIED**। সারসংক্ষেপ টেবিলের গ্রুপ ২ সারি
  আপডেট: "✅✅🔴🔴 (২.১–২.২ fixed+verified, ২.৩–২.৪ বাকি)"।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 3 checkbox: `[ ]` → **`[x]`**।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 3 সম্পূর্ণ ✅ Windows-verified) — পরের ধাপ Step 4
- **পরের ধাপ: Step 4 — 🔴 Admin panel ক্লাস B২: Reconcile/repair stuck spinner (গ্রুপ ২.৩)।**
  `adminRepairMissingRefunds()`/`adminReconcileBalances()`-এর `catch` ব্লকে `onComplete()` কল যোগ
  করা লাগবে (`AdminEscrowView.kt`-এর isRepairRunning/isReconcileRunning স্পিনার নাহলে চিরস্থায়ী
  আটকে থাকে)। `SomadhanViewModel.kt` লাইন ~৬৪৬৭-৬৫৪৩ (grep দিয়ে নতুন করে exact লোকেশন বের করতে
  হবে — Step 3-এর এডিটে ফাইলে ~১০ লাইন যোগ হয়েছে, লাইন-নাম্বার শিফট হয়ে থাকতে পারে)। Target test:
  `testReconcileAndRepairStuckSpinnerBugStillPresent` (এই সেশনের Windows-run-এই confirmed fail
  করছে, ঠিক যেভাবে প্রত্যাশিত)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 4 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- **পরবর্তী Windows-verify কমান্ডে একই `testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*"`
  ব্যবহার করা যাবে** — শুধু ফলাফলে ব্যর্থ-তালিকা এক ঘর ছোট হওয়ার কথা (৩টা থেকে ২টা)। **Windows setup
  নোট (rule #৯-এর বাইরে, শুধু পরের সেশনের জন্য মনে রাখার জন্য):** নতুন PowerShell window-এ
  `$env:JAVA_HOME`/`$env:PATH` সেট করার *পরেই* `Set-Content local.properties "sdk.dir=..."` চালাতে
  হবে, উল্টোভাবে না (এই সেশনে প্রথমবার উল্টো অর্ডারে চালিয়ে PowerShell কমান্ড-পার্স এরর দিয়েছিল)।
- `BUG_INVENTORY.md`-এ গ্রুপ ২.২ ও সারসংক্ষেপ টেবিল: 🟡 → ✅।

## ✅ Step 4 — কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি (২০২৬-০৯-২৩, নতুন সেশন)

**Step:** Step 4 — 🔴 Admin panel ক্লাস B২: Reconcile/repair stuck spinner (গ্রুপ ২.৩)।

**বুটস্ট্র্যাপ-চেক (নতুন সেশন):** master prompt Step 0–3 `[x]`, Step 4 প্রথম `[ ]`; আগের সেশনের শেষ
HANDOFF এটাই কনফার্ম করেছিল ("পরের ধাপ Step 4") — checkbox আর HANDOFF মিলে গিয়েছিল, কোনো গরমিল ছিল
না। এই সেশনের ব্যবহারকারীর মেসেজে কোনো `WINDOWS RESULT:` ছিল না। rule #৯ অনুযায়ী প্রথমে ব্যবহারকারীর
কাছে Step 4 শুরুর explicit confirmation চাওয়া হয়েছিল ("Continue step 4" পেয়ে তারপর শুরু হলো)।

**Fix শুরুর আগে re-verify (rule #২):** `BUG_INVENTORY.md`-এ গ্রুপ ২.৩ সারি আবার পড়া হলো (ফাইল/ফাংশন
নাম, প্রত্যাশিত টেস্ট)। `CI_TEST_SUITE_PROGRESS.md`-এর Step 19.4 "বাগ #২" সেকশন (লাইন ~৮৯৯৩-৯০০৭)
আবার পড়ে root-cause পুরোপুরি বুঝে নেওয়া হয়েছে — `AdminEscrowView.kt`-এ শুধুমাত্র `onComplete`
callback-এর ভেতরেই `isRepairRunning`/`isReconcileRunning = false` হয় (লাইন ৫৭৭, ৭৭১, ৮৪৪, ৯৯১ —
grep দিয়ে কনফার্মড, সবই আগের মতোই আছে, ছোঁয়া হয়নি)। `grep -n "fun adminRepairMissingRefunds\|fun
adminReconcileBalances"` দিয়ে exact বর্তমান লাইন বের করা হলো: ৬৫০২ ও ৬৫৪২ (HANDOFF-এর পুরনো
৬৪৬৭-৬৫৪৩ রেঞ্জ থেকে সামান্য শিফট হয়েছে Step 3-এর এডিটের কারণে, প্রত্যাশিত অনুযায়ী)।

**কী বদলেছে:**
- `SomadhanViewModel.kt::adminRepairMissingRefunds()` (এখন লাইন ~৬৫০২): `catch (e: Exception)`
  ব্লকে error toast-এর পর এখন `onComplete(MissingRefundRepairReport(dryRun = dryRun,
  scannedEscrowsCount = 0, missingRefundCount = 0, repairedCount = 0,
  totalAmountRepairedOrAudited = 0.0, items = emptyList()))` কল যোগ হয়েছে — একটা zero/no-op
  রিপোর্ট দিয়ে callback ফায়ার করে `AdminEscrowView.kt`-এর `isRepairRunning` flag রিসেট করানো হচ্ছে
  (error toast আগেই দেখানো হয়েছে, তাই admin বিভ্রান্ত হবেন না, শুধু বাটন আবার সক্রিয় হবে)।
- `SomadhanViewModel.kt::adminReconcileBalances()` (এখন লাইন ~৬৫৫৭): একই প্যাটার্নে `catch` ব্লকে
  `onComplete(BalanceReconciliationReport(dryRun = dryRun, scannedUsersCount = 0, mismatchCount = 0,
  correctedCount = 0, totalAbsoluteDifference = 0.0, items = emptyList()))` কল যোগ হয়েছে।
- দুটো ফাংশনের উপরে থাকা পুরনো কমেন্ট (যেগুলো বলতো "catch ব্লক `onComplete()` কল করে না,
  pre-existing gap, এই ধাপের স্কোপের বাইরে") আপডেট করা হয়েছে, কারণ সেগুলো আর সত্যি না — এখন বলছে
  Step 4-এ ফিক্স হয়ে গেছে।
- `AdminEscrowView.kt` — **ছোঁয়া হয়নি**। কোনো নতুন `onError` প্যারামিটার/callback যোগ করা হয়নি (rule
  #৩ ন্যূনতম-এডিট অনুযায়ী) — বিদ্যমান `onComplete` callback-ই যথেষ্ট, যেহেতু সেখানে ইতিমধ্যে flag
  reset করার লজিক আছে।

**Test:** `AdminPanelStep19RegressionTest.kt::testReconcileAndRepairStuckSpinnerBugStillPresent` —
নাম রিনেম করা হয়নি এই সেশনে (rule অনুযায়ী রিনেম optional, শুধু pass-প্রত্যাশিত হওয়াটাই দরকার; নাম
"...BugStillPresent" রয়ে গেছে কিন্তু এখন pass করার কথা যেহেতু assertion `failures.isEmpty()` —
Step 2/3-এর মতো রিনেম না করেই রেখে দেওয়া হলো, ব্যবহারকারী চাইলে ভবিষ্যতে রিনেম করা যাবে)।
Sandbox real Gradle চালাতে পারে না (network নেই), তাই test-এর `functionRegions`/`singleFunctionRegionText`
হেল্পার লজিক হুবহু Python-এ replicate করে (regex-ভিত্তিক member-fun boundary matching, brace-counting
না) বর্তমান `SomadhanViewModel.kt`-এর টেক্সটের বিপরীতে যাচাই করা হয়েছে: দুটো ফাংশনেই এখন
`catch (e: Exception)`-এর পরের টেক্সটে `onComplete(` পাওয়া যাচ্ছে (আগে পাওয়া যেত না) — উভয়ের জন্য
`True`। পাশাপাশি বাকি এখনো-না-ফিক্সড টেস্টগুলো (`testAdminWithdrawalsPagedStalenessBugStillPresent`,
`testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent`, Step 5-এর কাজ) আগের
মতোই fail করার কথা তা-ও একই সিমুলেশনে re-confirm করা হয়েছে (কোনো নতুন রিগ্রেশন হয়নি)। Brace-balance
sanity-check (`SomadhanViewModel.kt`: open==close, ১৪৩৬==১৪৩৬, এডিটের আগে-পরে একই কারণ যোগ করা কোড
সম্পূর্ণ ব্যালেন্সড) পাস করেছে।

**Windows-verify লাগবে (ব্যবহারকারীর মেশিনে):**
```
.\gradlew.bat testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*"
```
প্রত্যাশিত: `testReconcileAndRepairStuckSpinnerBugStillPresent` pass করবে; আগের ৩টা ব্যর্থতা থেকে
এখন ২টা বাকি থাকার কথা (`testAdminWithdrawalsPagedStalenessBugStillPresent`,
`testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent` — দুটোই Step 5-এর কাজ)।
rule #৭ শুধু Step 1 (money-critical realtime)-এর জন্য নির্দিষ্ট, Step 4-এর জন্য প্রযোজ্য না — এই
বাগ balance/escrow-এর টাকার অঙ্ক বদলায় না, শুধু UI স্পিনার-স্টেট রিসেট করে, তাই আলাদা manual
checklist বানানো হয়নি।

**Status আপডেট (কোড-ফিক্স সম্পূর্ণ, এখনো Windows-verified না):**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৩ সারি: 🔴 → 🟡 (FIXED, NOT VERIFIED)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 4 checkbox এখনো `[ ]` (Windows real-run pass কনফার্মড
  না হওয়া পর্যন্ত rule #৫ অনুযায়ী `[x]` করা হবে না)।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 4 কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি) — পরের ধাপ: Step 4-এর WINDOWS RESULT অথবা (যদি ব্যবহারকারী verify স্কিপ করে এগোতে চান) Step 5
- **যদি পরের মেসেজে `WINDOWS RESULT:` আসে** (উপরের কমান্ডের আউটপুট) — সেটা প্রথমে প্রসেস করতে হবে:
  pass হলে `BUG_INVENTORY.md`-এ গ্রুপ ২.৩: 🟡 → ✅, master prompt Step 4 checkbox `[ ]` → `[x]`,
  তারপর rule #৯ অনুযায়ী Step 5 শুরুর জন্য explicit confirmation চাইতে হবে। fail হলে Step 4 আবার
  খুলে root-cause ধরতে হবে (Step 5-এ যাওয়া যাবে না)।
- **Step 5 — 🔴 Admin panel ক্লাস B৩: Paged-cache staleness (গ্রুপ ২.৪)।**
  `resetAdminWithdrawalsPagination()` mutation-এর পর ও `refreshAdminTab()`-এ কল করা লাগবে। rule #৮
  অনুযায়ী `AdminUsersView.kt`-এর একই architecture-ও এই Step-এই verify+fix করে ফেলতে হবে (আলাদা Step
  লাগবে না, যদি সত্যিই একই বাগ প্রমাণিত হয়)। `SomadhanViewModel.kt` লাইন ~১০২৯-১০৫৯ অঞ্চল (grep দিয়ে
  নতুন করে exact লোকেশন বের করতে হবে — Step 4-এর এডিটে ফাইলে লাইন-সংখ্যা সামান্য বেড়েছে)। Target
  test: `testAdminWithdrawalsPagedStalenessBugStillPresent` +
  `testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent` (দুটোই)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 5 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- **পরবর্তী Windows-verify কমান্ডে একই `testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*"`
  ব্যবহার করা যাবে** — শুধু ফলাফলে ব্যর্থ-তালিকা এক ঘর ছোট হওয়ার কথা (২টা থেকে ১টা, Step 4-এর নিজের
  test pass করলে)।
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৩ ও সারসংক্ষেপ টেবিল: 🟡 → ✅ (Windows pass পাওয়ার পর)।

## ✅ Step 4 — WINDOWS RESULT প্রসেস করা হলো (২০২৬-০৯-২৩, নতুন সেশন) — Windows-verified, Step 4 সম্পূর্ণ

ব্যবহারকারী real Windows PowerShell আউটপুট (স্ক্রিনশট + পরে পূর্ণ টেক্সট) পেস্ট করেছেন। rule অনুযায়ী
নতুন কোনো Step নেওয়ার আগে প্রথমে এটা প্রসেস করা হলো (এই সেশনে কোনো নতুন Step ধরাই হয়নি, শুধু এই
প্রসেসিং)।

**প্রথম মেসেজে (স্ক্রিনশট) শুধু সেটআপ-এরর ছিল:** `JAVA_HOME` সেট না করেই `.\gradlew.bat` চালানোয়
`ERROR: JAVA_HOME is not set` — টেস্ট আসলে চলেনি, তাই এটা `WINDOWS RESULT` হিসেবে গণ্য হয়নি, শুধু
troubleshooting guidance দেওয়া হয়েছিল (`$env:JAVA_HOME`/`$env:PATH` সেট করার পর আবার চালাতে বলা
হয়েছিল, ঠিক যেমন আগের সেশনের HANDOFF নোটে লেখা ছিল)।

**দ্বিতীয় মেসেজে (পূর্ণ টেক্সট) আসল ফলাফল এসেছে:** `$env:JAVA_HOME`/`$env:PATH` সেট করার পর
`java -version` সফল (OpenJDK 25.0.2), তারপর `.\gradlew.bat testDebugUnitTest --tests
"*AdminPanelStep19RegressionTest*"` সফলভাবে চলেছে। `compileDebugKotlin` সফল (শুধু pre-existing
deprecation warning, Step 4-এর এডিট syntax-এর দিক থেকে ঠিক)।

**ফলাফল বিশ্লেষণ:** `testDebugUnitTest` টাস্ক **৮টার মধ্যে ২টা টেস্ট FAILED** দেখিয়ে `BUILD FAILED`
দিয়েছে (Step 3-এর সেশনে ৩টা ছিল, এখন ২টা — ঠিক ১ ঘর কমেছে, প্রত্যাশিত অনুযায়ী) — এই ২টা যাচাই করে
দেখা গেছে **Step 4-এর টেস্ট না, বরং Step 5-এর কাজ**:
- `testAdminWithdrawalsPagedStalenessBugStillPresent` (গ্রুপ ২.৪, Step 5-এর কাজ)
- `testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent` (গ্রুপ ২.৪, Step 5-এর কাজ)

**`testReconcileAndRepairStuckSpinnerBugStillPresent` (Step 4-এর নিজের টেস্ট) এই ২টা ব্যর্থ-তালিকায়
নেই** — অর্থাৎ সেটা **pass করেছে**, যেটা এই সেশনের আগের অংশে করা static regex-simulation-এর
পূর্বাভাসের সাথে হুবহু মেলে (দুটো ফাংশনেই `catch` ব্লকে `onComplete(` পাওয়া গিয়েছিল)।

**উপসংহার:** Step 4-এর ফিক্স real Gradle run-এ pass কনফার্মড। `BUILD FAILED` শুধু Step 5-এর
এখনো-না-হওয়া কাজের কারণে, Step 4-এর কোনো রিগ্রেশন না।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৩ সারি: 🟡 → **✅ FIXED + VERIFIED**। সারসংক্ষেপ টেবিলের গ্রুপ ২ সারি
  আপডেট: "✅✅✅🔴 (২.১–২.৩ fixed+verified, ২.৪ বাকি)"।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 4 checkbox: `[ ]` → **`[x]`**।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 4 সম্পূর্ণ ✅ Windows-verified) — পরের ধাপ Step 5
- **পরের ধাপ: Step 5 — 🔴 Admin panel ক্লাস B৩: Paged-cache staleness (গ্রুপ ২.৪, শেষ admin-panel Step)।**
  `resetAdminWithdrawalsPagination()` mutation-এর পর ও `refreshAdminTab()`-এ কল করা লাগবে। rule #৮
  অনুযায়ী `AdminUsersView.kt`-এর একই architecture-ও এই Step-এই verify+fix করে ফেলতে হবে (আলাদা Step
  লাগবে না, যদি সত্যিই একই বাগ প্রমাণিত হয়)। `SomadhanViewModel.kt`-এ `adminUpdateWithdrawalStatus()`,
  `refreshAdminTab()`, `adminWithdrawalsPaged`/`loadNextAdminWithdrawalsPage()`/
  `resetAdminWithdrawalsPagination()` — grep দিয়ে নতুন করে exact লোকেশন বের করতে হবে (Step 4-এর
  এডিটে ফাইলে লাইন-সংখ্যা সামান্য বেড়েছে)। `AdminWithdrawalsView.kt` লাইন ~৩৮৯-৪২৭ (এটাও grep দিয়ে
  re-confirm করা উচিত)। Target test: `testAdminWithdrawalsPagedStalenessBugStillPresent` +
  `testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent` (দুটোই, দুটো এখনো
  fail করছে বলে এই সেশনের Windows-run-এই confirmed)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 5 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- **পরবর্তী Windows-verify কমান্ডে একই `testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*"`
  ব্যবহার করা যাবে** — Step 5 সঠিকভাবে ফিক্স হলে ফলাফলে **০টা failure** হওয়ার কথা (২টা থেকে ০টা) —
  এটাই গ্রুপ ২ (admin panel)-এর শেষ বাকি বাগ, তাই Step 5 pass করলে পুরো `AdminPanelStep19RegressionTest`
  ক্লাস green হয়ে যাবে। **Windows setup নোট (এই সেশনেও প্রথমবার JAVA_HOME ভুলে যাওয়া হয়েছিল):** নতুন
  PowerShell window-এ সবসময় প্রথমে `$env:JAVA_HOME`/`$env:PATH` সেট করে `java -version` দিয়ে কনফার্ম
  করে, তারপর `local.properties` লেখা ও gradlew কমান্ড চালানো উচিত।
- Step 5 শেষ হলে **Step 0–5 (Step 1: realtime, Step 2-5: পুরো admin panel গ্রুপ) সবই সম্পূর্ণ+verified
  হয়ে যাবে** — শুধু Step 6 (offline-gating, নতুন test লেখা লাগবে), Step 7 (backlog আলোচনা), Step 8
  (চূড়ান্ত full-suite regression) বাকি থাকবে।
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৪ ও সারসংক্ষেপ টেবিল: 🔴 → ✅ (Windows pass পাওয়ার পর)।
## ✅ Step 5 — Admin panel ক্লাস B৩: Paged-cache staleness (গ্রুপ ২.৪, ২০২৬-০৯-২৩) — কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি

ব্যবহারকারীর মেসেজে কোনো `WINDOWS RESULT:` ছিল না, তাই সরাসরি Step 5 (rule #৯ অনুযায়ী আগের HANDOFF-এ
যে confirmation চাওয়া হয়েছিল, standard bootstrap মেসেজেই সেটা পাওয়া হিসেবে ধরা হলো, ঠিক Step 2-4-এর
একই precedent অনুযায়ী) নেওয়া হলো। master prompt checkbox আর FIX_PROGRESS.md HANDOFF দুটোই Step 5-কে
নির্দেশ করছিল, কোনো গরমিল ছিল না।

**Bootstrap-এর ৩টা pre-edit verification (কোনো কোড ছোঁয়ার আগে):**
1. zip ফাইল-সংখ্যা/গঠন — আগের সেশনের "যা বদলেছে" লিস্টের সাথে সঙ্গতিপূর্ণ (sanity-check করা হয়েছে)।
2. `grep -n` দিয়ে `SomadhanViewModel.kt`-এ exact বর্তমান লোকেশন বের করা হয়েছে:
   `adminUsersPaged`/`loadNextAdminUsersPage()`/`resetAdminUsersPagination()` লাইন ৯৮৩-১০১৯,
   `adminWithdrawalsPaged`/`loadNextAdminWithdrawalsPage()`/`resetAdminWithdrawalsPagination()`
   লাইন ১০৩০-১০৬০, `refreshAdminTab()` লাইন ২৫১৯, `adminUpdateWithdrawalStatus()` লাইন ৫৭৪৫ —
   HANDOFF-এর "~১০২৯-১০৫৯" রেফারেন্সের কাছাকাছিই ছিল (Step 4-এর এডিটে সামান্য শিফট হয়েছিল)।
3. `AdminWithdrawalsView.kt`/`AdminUsersView.kt`-এ `isBrowsingUnfiltered` প্যাটার্ন সরাসরি পড়ে
   নিশ্চিত করা হয়েছে যে দুটো স্ক্রিনই হুবহু একই non-reactive-snapshot architecture ব্যবহার করে
   (শুধু filter-toggle/re-entry-তে reset হয়, mutation/pull-to-refresh-এ না) — rule #৮-এর
   generalization দাবি নিশ্চিত হলো, শুধু `BUG_INVENTORY.md`-এর সংক্ষিপ্ত সারাংশের ভিত্তিতে শুরু করা
   হয়নি।

**যা বদলানো হয়েছে (`app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt`):**
- `adminUpdateWithdrawalStatus()`-এর `WithdrawalUpdateResult.Updated` branch-এ
  `resetAdminWithdrawalsPagination()` কল যোগ করা হয়েছে — সফল status-update-এর পর Admin Withdrawals
  ট্যাবের ডিফল্ট (unfiltered) `adminWithdrawalsPaged` snapshot এখন রিফ্রেশ হবে।
- `refreshAdminTab()`-এ `resetAdminWithdrawalsPagination()` ও
  `resetAdminUsersPagination(currentAdminUsersRoleFilter)` — দুটোই unconditionally (tabIndex
  নির্বিশেষে, লাইটওয়েট page-size-10 কল বলে) যোগ করা হয়েছে। `resetAdminUsersPagination()`-কে
  default `null`-এর বদলে বর্তমান `currentAdminUsersRoleFilter` (private var, একই ক্লাসে অ্যাক্সেসযোগ্য)
  দিয়ে কল করা হয়েছে — কারণ `AdminUsersView.kt`-এর `isBrowsingUnfiltered` শুধু search/sort দেখে,
  role-filter দেখে না, তাই কোনো role-filter সিলেক্ট করা অবস্থায় pull-to-refresh টানলে default `null`
  filter-টাকে silently "ALL"-এ রিসেট করে দিত আর সেই LaunchedEffect আবার ফায়ার হয়ে ঠিক করে দিত না
  (এই edge-case-টা master prompt/HANDOFF-এ স্পষ্ট করে লেখা ছিল না, কিন্তু rule #৩-এর "targeted fix,
  নতুন bug না বসানো" নীতি অনুযায়ী প্রয়োজনীয় মনে হয়েছে)।
- `AdminUsersView.kt`-এর নিজস্ব mutation ফাংশন (ban/restrict/delete ইত্যাদি) **touch করা হয়নি** —
  কোনো test/bug-row নির্দিষ্টভাবে সেটা দাবি করেনি (শুধু `refreshAdminTab()`-এর মাধ্যমে
  pull-to-refresh কভারেজ চাওয়া হয়েছিল), rule #৩ অনুযায়ী স্কোপ-ক্রিপ এড়ানো হলো।

**Verification (sandbox-এ, network নেই বলে real Gradle চালানো যায়নি — আগের Step-গুলোর একই সীমাবদ্ধতা):**
- Python দিয়ে test-এর `functionRegions`/`singleFunctionRegionText` লজিক হুবহু পোর্ট করে ফিক্সড
  কোডের উপর চালানো হয়েছে:
  - `testAdminWithdrawalsPagedStalenessBugStillPresent`-এর assertion
    (`adminUpdateWithdrawalStatus` region-এ `"resetAdminWithdrawalsPagination()"` থাকা) → **True**
    → এই টেস্ট এখন pass করার কথা।
  - `testRefreshAdminTabDoesNotResetWithdrawalsOrUsersPagedCacheBugStillPresent`-এর assertion
    (`refreshAdminTab` region-এ `"resetAdminWithdrawalsPagination("` ও
    `"resetAdminUsersPagination("` দুটোই থাকা) → **True, True** → এই টেস্টও pass করার কথা।
  - `testAdminUsersViewSharesTheSameNonReactivePagedCacheArchitecture` (drift-guard, আগে থেকেই
    pass) — `isBrowsingUnfiltered`+`viewModel.adminUsersPaged` দুটোই এখনো `AdminUsersView.kt`-এ
    আছে, অপরিবর্তিত।
- ফাইলের brace balance ম্যানুয়ালি চেক করা হয়েছে (open/close `{`/`}` কাউন্ট সমান, ১৪৩৭/১৪৩৭) —
  edit-এ কোনো syntax ভাঙেনি।
- ফাইলের বাকি ৬টা টেস্ট (Step 2-4-এর নিজস্ব, আগেই pass কনফার্মড) এই সেশনে ছোঁয়া হয়নি, তাদের
  polarity অপরিবর্তিত থাকার কথা।
- **real Windows verify কমান্ড বাকি:**
  `.\gradlew.bat testDebugUnitTest --tests "com.example.ui.viewmodel.AdminPanelStep19RegressionTest" --stacktrace`
  (JAVA_HOME/ANDROID_HOME প্রথমে সেট করে, `java -version` দিয়ে কনফার্ম করে) — `BUILD SUCCESSFUL`
  (৮টার মধ্যে ০টা fail) হলে Step 5 পুরোপুরি ✅ ধরা হবে, আর সাথে সাথেই পুরো
  `AdminPanelStep19RegressionTest` ক্লাস green (গ্রুপ ২ admin panel-এর সব ৪টা বাগ)।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৪ সারি: 🔴 → **🟡 FIXED, NOT VERIFIED** (real Windows run কনফার্ম করেনি
  এখনো)। AdminUsersView.kt architecture-generalization নোটও আপডেট (verify করা হয়েছে বলে)।
  সারসংক্ষেপ টেবিলের গ্রুপ ২ সারি আপডেট: "✅✅✅🟡 (২.১–২.৩ fixed+verified, ২.৪ fixed, Windows-verify
  বাকি)"।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 5 checkbox **এখনো `[ ]`** (rule #৫ অনুযায়ী — শুধু
  real+verified হলে `[x]` হয়) — টাইটেলে নোট যোগ করা হয়েছে যে কোড-কাজ শেষ, Windows-verify বাকি।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 5 কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি) — পরের ধাপ: Step 5-এর WINDOWS RESULT অথবা (যদি ব্যবহারকারী verify স্কিপ করে এগোতে চান) Step 6
- **যদি পরের মেসেজে `WINDOWS RESULT:` আসে** (উপরের কমান্ডের আউটপুট) — সেটা প্রথমে প্রসেস করতে হবে:
  pass (৮টার মধ্যে ০টা fail) হলে `BUG_INVENTORY.md`-এ গ্রুপ ২.৪: 🟡 → ✅, সারসংক্ষেপ টেবিলের গ্রুপ ২
  সারি পুরো ✅✅✅✅ করা, master prompt Step 5 checkbox `[ ]` → `[x]`, তারপর rule #৯ অনুযায়ী Step 6
  শুরুর জন্য explicit confirmation চাইতে হবে। fail হলে (এই ২টা টেস্ট বাদে অন্য কিছু fail করলে
  বিশেষভাবে) Step 5 আবার খুলে root-cause ধরতে হবে (Step 6-এ যাওয়া যাবে না)। যদি শুধু এই ২টা টেস্ট
  ছাড়া অন্য কিছু নতুন fail করে সেটাও regression হিসেবে গণ্য হবে, নতুন কোনো Step-এ না গিয়ে এখানেই
  ঠিক করতে হবে।
- **Step 5 pass করলে পুরো `AdminPanelStep19RegressionTest` ক্লাস green হয়ে যাবে (৮/৮)** — এটাই
  গ্রুপ ২ (admin panel)-এর শেষ বাগ, তাই Step 0-5 (Step 1: realtime, Step 2-5: পুরো admin panel
  গ্রুপ) সবই সম্পূর্ণ+verified হয়ে যাবে।
- **পরের ধাপ: Step 6 — 🟠 Offline-action-gating toggle (গ্রুপ ৩, প্রথমে bug-demonstrating test লিখতে
  হবে)।** কোনো existing automated test নেই এই গ্রুপের জন্য (rule #৪ অনুযায়ী), তাই প্রথম কাজ একটা নতুন
  test লেখা (নতুন ফাইল বা `OfflineGatingSyncTest.kt`-এ যোগ) যেটা ৩.১a/৩.১b/৩.১c-এর অন্তত একটার
  বর্তমান buggy আচরণ demonstrate করে এখন fail করে, তারপর ৩টা উপ-ফিক্স একটার পর একটা sub-step হিসেবে।
  `BUG_INVENTORY.md`-এর গ্রুপ ৩ সেকশন আবার পড়ে exact sub-bug বিবরণ কনফার্ম করতে হবে (এই সেশনে সেটা
  পড়া হয়নি, Step 5-এর স্কোপে ছিল না)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 6 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- **Windows setup নোট (repeat, আগের সেশনগুলোতেও দরকার হয়েছিল):** নতুন PowerShell window-এ সবসময়
  প্রথমে `$env:JAVA_HOME`/`$env:PATH` সেট করে `java -version` দিয়ে কনফার্ম করে, তারপর
  `local.properties` লেখা ও gradlew কমান্ড চালানো উচিত।
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৪ ও সারসংক্ষেপ টেবিল: 🟡 → ✅ (Windows pass পাওয়ার পর)।

## ✅ Step 5 — WINDOWS RESULT প্রসেস করা হলো (২০২৬-০৯-২৩, একই সেশন) — Windows-verified, Step 5 সম্পূর্ণ

ব্যবহারকারী real Windows PowerShell আউটপুট পেস্ট করেছেন (প্রথমে `local.properties` লেখায় ভুল
quoting-এর এরর, তারপর `JAVA_HOME`-এ ভুলে literal placeholder path বসানোয় এরর — দুটোই setup-এরর,
`WINDOWS RESULT` হিসেবে গণ্য হয়নি; শেষে `$env:JAVA_HOME = "C:\Program Files\Android\Android
Studio\jbr"` দিয়ে ঠিক হওয়ার পর আসল রান হয়েছে)।

**আসল ফলাফল:** `java -version` সফল (OpenJDK 25.0.2), তারপর
`.\gradlew.bat testDebugUnitTest --tests "*AdminPanelStep19RegressionTest*" --stacktrace` চলেছে।
`compileDebugKotlin` সফল (শুধু pre-existing deprecation/condition warning-গুলো, Step 5-এর এডিট
syntax-এর দিক থেকে ঠিক)। শেষে **`BUILD SUCCESSFUL in 2m 15s`** — কোনো টেস্ট FAILED দেখায়নি (Step
4-এর সেশনে ২টা fail ছিল, Step 3-এ ৩টা, Step 2-এ ৪টা — এবার ধারাবাহিকভাবে কমে **০টা**তে এসেছে,
প্রত্যাশিত অনুযায়ী)।

**উপসংহার:** Step 5-এর ফিক্স real Gradle run-এ pass কনফার্মড, আর যেহেতু এটাই ছিল
`AdminPanelStep19RegressionTest`-এর শেষ বাকি বাগ, পুরো ৮/৮ টেস্ট ক্লাস এখন green — গ্রুপ ২
(admin panel)-এর ৪টা বাগই (২.১-২.৪) সম্পূর্ণ+verified।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৪ সারি: 🟡 → **✅ FIXED + VERIFIED**। সারসংক্ষেপ টেবিলের গ্রুপ ২ সারি
  আপডেট: "✅✅✅✅ (২.১–২.৪ সবই fixed+verified)"।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 5 checkbox: `[ ]` → **`[x]`**।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 5 সম্পূর্ণ ✅ Windows-verified) — পরের ধাপ Step 6
- **পরের ধাপ: Step 6 — 🟠 Offline-action-gating toggle (গ্রুপ ৩, প্রথমে bug-demonstrating test
  লিখতে হবে)।** এই গ্রুপের জন্য কোনো existing automated test নেই (rule #৪ অনুযায়ী), তাই প্রথম কাজ
  একটা নতুন bug-demonstrating test লেখা (নতুন ফাইল বা `OfflineGatingSyncTest.kt`-এ যোগ) — যেটা
  ৩.১a/৩.১b/৩.১c-এর অন্তত একটার বর্তমান buggy আচরণ demonstrate করে **এখন fail করে** — তারপরই ফিক্স
  শুরু। `BUG_INVENTORY.md`-এর গ্রুপ ৩ সেকশন এই সেশনে পড়া হয়নি (Step 5-এর স্কোপে ছিল না) — পরের
  সেশনের প্রথম কাজ হবে সেটা পুরোপুরি আবার পড়ে exact ৩টা sub-bug (৩.১a/b/c)-এর বিবরণ, ফাইল/ফাংশন,
  আর "প্রস্তাবিত ফিক্স" নোট কনফার্ম করা — অনুমান করে শুরু করা যাবে না।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে Step 6 শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- **এই সেশনে Step 5 পুরোপুরি সম্পূর্ণ হলো:** কোড-ফিক্স + real Windows `gradlew testDebugUnitTest`
  run দিয়ে pass কনফার্মড (`BUILD SUCCESSFUL`, ৮/৮ টেস্ট, ০টা fail)। **Step 0-5 (Step 1: realtime,
  Step 2-5: পুরো admin panel গ্রুপ) এখন সবই সম্পূর্ণ+verified** — শুধু Step 6 (offline-gating), Step 7
  (backlog আলোচনা), Step 8 (চূড়ান্ত full-suite regression) বাকি।
- **Step 6-এ Windows-verify কমান্ড ভিন্ন হবে** — নতুন test class-এর নাম অনুযায়ী (Step 6 শুরুর সময়
  ঠিক করতে হবে, সম্ভবত `--tests "*OfflineGatingSyncTest*"`)। **Windows setup নোট (repeat, প্রায়
  প্রতিটা সেশনেই দরকার হয়েছে):** নতুন PowerShell window-এ সবসময় প্রথমে `$env:JAVA_HOME =
  "C:\Program Files\Android\Android Studio\jbr"` (এটাই কাজ করা path, placeholder না) আর
  `$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"` সেট করে `java -version` দিয়ে কনফার্ম করে, তারপর
  `local.properties` লেখা ও gradlew কমান্ড চালানো উচিত। `local.properties` লিখতে
  `Set-Content local.properties 'sdk.dir=C:\Users\StepUp_Emp\AppData\Local\Android\Sdk'` — single-quote
  ব্যবহার করা উচিত (double-quote-এ ভেতরের `\\` PowerShell parse করে ভেঙে ফেলতে পারে, এই সেশনেও
  প্রথমবার এই এররই হয়েছিল)।
- `BUG_INVENTORY.md`-এ গ্রুপ ২.৪ ও সারসংক্ষেপ টেবিল ইতিমধ্যে ✅ করা হয়েছে এই সেশনেই।

## ✅ Step 6 — sub-step ১ (bug-demonstrating test) সম্পূর্ণ (২০২৬-০৯-২৩ সেশন) — কোনো production কোড বদলানো হয়নি

**GATE-অনুযায়ী প্রথম অসম্পূর্ণ ধাপ হিসেবে নেওয়া হলো (Step 0-5 সবই `[x]`)।** input zip
(`somadhan-bug-fix-step6-next__1_.zip`, ৩৮৪ ফাইল, `.env`/`.gitignore`/`.env.example` সহ) —
HANDOFF-এর প্রত্যাশার সাথে মিলে Step 6 প্রথম অসম্পূর্ণ ধাপ হিসেবে কনফার্ম হয়েছে। কোনো
`WINDOWS RESULT:` এই সেশনে paste হয়নি। ব্যবহারকারী স্পষ্টভাবে শুধু Step 6 শুরু করতে বলেছেন (সব
ধাপ একসাথে না) — তাই rule #১ অনুযায়ী Step 6-এর ভেতরে একটাই sub-item (এই সেশনে: bug-demonstrating
test লেখা) নেওয়া হলো, ফিক্স-সাব-স্টেপ শুরু হয়নি।

**Step নেওয়ার আগে ৩টা sanity check (bootstrap rule অনুযায়ী):**
- zip ফাইল-সংখ্যা (৩৮৪) ও dotfile উপস্থিতি — যাচাই করা হয়েছে, sane।
- `BUG_INVENTORY.md`-এর গ্রুপ ৩ সেকশন (৩.১/৩.১a/৩.১b/৩.১c) এই সেশনে পুরোপুরি আবার পড়া হয়েছে
  (আগের সেশনে Step 5-এর স্কোপে পড়া হয়নি, HANDOFF-এ এটাই পরের সেশনের প্রথম কাজ হিসেবে বলা ছিল)।
- `CI_TEST_SUITE_PROGRESS.md`-এর Step 16.2 root-cause investigation সেকশন (লাইন ৬৯৭২-৭১০৯) পুরোপুরি
  আবার পড়া হয়েছে — শুধু `BUG_INVENTORY.md`-এর সংক্ষিপ্ত সারাংশের উপর ভিত্তি করে না।

**কেন ৩.১b বেছে নেওয়া হলো (৩টার মধ্যে) প্রথম bug-demonstrating test-এর জন্য:** ৩.১a
(dual-write silent failure) ও ৩.১c (degraded-admin-session RLS failure)-এর বাস্তব রানটাইম
আচরণ যাচাই করতে হলে real Supabase call/RLS-context লাগবে (`SupabaseSyncManager`/
`SupabaseAuthManager` — দুটোই Kotlin `object`, কোনো DI seam নেই, `DualWriteGapTest.kt`/
`OfflineGatingSyncTest.kt`-এর নিজস্ব KDoc-এই এই সীমাবদ্ধতা আগে থেকেই ডকুমেন্টেড)। ৩.১b
(`NET_CAPABILITY_VALIDATED` অনুপস্থিতি) সম্পূর্ণভাবে static/structural — ঠিক এই টেস্ট ফাইলের
বিদ্যমান কনভেনশন (`functionRegions`/`regionText` হেল্পার, `DualWriteGapTest.kt`-এর প্যাটার্ন)
দিয়েই নির্ভরযোগ্যভাবে demonstrate করা যায়, `loginAsAdmin`-এর wiring-gap যেভাবে আগে ধরা পড়েছিল
(Step 16.3) ঠিক সেভাবেই। তাই এই সেশনে ৩.১b-এর জন্য test লেখা হয়েছে; ৩.১a/৩.১c-এর জন্য পরের
সাব-স্টেপগুলোতে ফিক্স করার সময় (root-cause যথেষ্ট স্পষ্ট বলে) সরাসরি ফিক্স + কোড-লেভেল
sanity-check (কোনো bug-demonstrating test ছাড়াই, ঠিক Step 4/5-এর প্যাটার্নে) করা হবে — কারণ
rule #৪ শুধু বলে "test নেই এমন বাগের প্রথম কাজ একটা bug-demonstrating test লেখা", কিন্তু ৩.১a/c-এর
জন্য নির্ভরযোগ্য structural test লেখা সম্ভব না (runtime/RLS-নির্ভর), তাই সেই দুটোর জন্য টেস্ট-না-লেখার
এই সিদ্ধান্তটা পরের সাব-স্টেপ শুরুর সময় ব্যবহারকারীর সামনে স্পষ্ট করে বলা হবে।

**যা যোগ হয়েছে:** `OfflineGatingSyncTest.kt`-এ একটা নতুন `@Test` —
`` `startConnectivityObserver checks NET_CAPABILITY_VALIDATED before marking isOnline true (BUG 3_1b, currently failing)` ``
— যেটা `functionRegions("startConnectivityObserver")` (বিদ্যমান হেল্পার, রিইউজ করা হয়েছে) দিয়ে
`SomadhanViewModel.kt`-এর `startConnectivityObserver()` ফাংশনের region বের করে assert করে সেই
টেক্সটে `NET_CAPABILITY_VALIDATED` string আছে কিনা। কোনো production কোড ছোঁয়া হয়নি (rule ১)।

**যাচাই — টেস্ট এখন সত্যিই fail করে তা কনফার্মড:** Sandbox real Gradle চালাতে পারে না (network
নেই, আগের সেশনগুলোর মতোই), তাই `functionRegions`-এর regex-ভিত্তিক member-fun boundary matching
লজিক হুবহু Python-এ replicate করে `SomadhanViewModel.kt`-এর বর্তমান টেক্সটের বিপরীতে যাচাই করা
হয়েছে: `startConnectivityObserver()`-এর region (লাইন ১৮৩০-১৮৭০) `NET_CAPABILITY_INTERNET` ৩ বার
ব্যবহার করে (শুরুর মান, `onAvailable`-সম্পর্কিত `stillHasOther` চেক, `NetworkRequest.Builder()`)
কিন্তু `NET_CAPABILITY_VALIDATED` একবারও না — অর্থাৎ `text.contains("NET_CAPABILITY_VALIDATED")`
`False`, তাই নতুন `assertTrue` **fail করবে**, ঠিক প্রত্যাশিতভাবে (বাগ এখনো আছে, শুধু এখন প্রমাণিত)।
নতুন test block-এর paren/brace balance আলাদাভাবে চেক করে সিনট্যাক্স-নিরাপদ কনফার্মড।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১b সারি: 🟠 (NOT FIXED, NO TEST) → 🔴 (NOT FIXED, TEST EXISTS) —
  `OfflineGatingSyncTest.kt::startConnectivityObserver checks NET_CAPABILITY_VALIDATED...` নতুন
  bug-demonstrating test যোগ হয়েছে। ৩.১a/৩.১c এখনো 🟠 (কোনো টেস্ট নেই, উপরের কারণেই — পরের
  সাব-স্টেপে সরাসরি ফিক্স হবে)। প্যারেন্ট ৩.১ সারির "প্রমাণ" কলামও আপডেট করা হয়েছে (আর "কোনো
  bug-demonstrating test নেই" পুরোপুরি সত্যি না, একটা সাব-বাগের জন্য এখন আছে)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 6 checkbox এখনো `[ ]` (শুধু sub-step ১/৪ সম্পূর্ণ —
  test লেখা হয়েছে, কোনো ফিক্সই এখনো প্রয়োগ হয়নি, rule #৫ অনুযায়ী পুরো Step সম্পূর্ণ+ভেরিফাইড না হলে
  `[x]` হবে না)।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 6 sub-step ১ সম্পূর্ণ — bug-demonstrating test লেখা হয়েছে, কোনো ফিক্স প্রয়োগ হয়নি) — পরের ধাপ: Step 6-এর ভেতরে sub-step ২ (৩.১b ফিক্স)
- **পরের ধাপ: Step 6-এর ভেতরে শুধু sub-step ২ বাকি প্রথমে — ৩.১b ফিক্স (`NET_CAPABILITY_VALIDATED`
  চেক যোগ করা `startConnectivityObserver()`-এ)।** এর পরে sub-step ৩ (৩.১a ফিক্স, dual-write
  ব্যর্থতা surface করা), তারপর sub-step ৪ (৩.১c ফিক্স, degraded-session সতর্কতা persistent করা) —
  প্রতিটার পর test pass নিশ্চিত (৩.১b-এর জন্য এই সেশনে লেখা নতুন test; ৩.১a/c-এর জন্য কোনো automated
  test নেই, শুধু কোড-লেভেল sanity-check, উপরে ব্যাখ্যা করা কারণেই)।
- **৩.১b ফিক্সের এক্সাক্ট লোকেশন (এই সেশনে গ্রেপ করে কনফার্মড, লাইন-নাম্বার শিফট হতে পারে পরের
  সেশনে — ফাংশন-নাম দিয়ে আবার খুঁজে বের করা উচিত):** `SomadhanViewModel.kt`-এর
  `startConnectivityObserver()` (~১৮৩০-১৮৭০) — ৩ জায়গায় বদলাতে হবে: (ক) শুরুর মান
  (~১৮৪২, `activeCaps?.hasCapability(NET_CAPABILITY_INTERNET)`), (খ) `onAvailable()`
  callback (~১৮৪৫-১৮৪৭, এখন unconditionally `_isOnline.value = true` করে, capability
  re-check ছাড়াই), (গ) `onLost()`-এর `stillHasOther` চেক (~১৮৫৭-১৮৫৮)। রেফারেন্স
  প্যাটার্ন: `NetworkConnectivityObserver.kt::isCurrentlyConnected()` (লাইন ৩৬-৭০, ইতিমধ্যে
  ফিক্সড) — `NET_CAPABILITY_INTERNET` বা `NET_CAPABILITY_VALIDATED` বা transport-চেক (WIFI/
  CELLULAR/ETHERNET/VPN)-এর OR কম্বিনেশন ব্যবহার করে, `startConnectivityObserver()`-এও একই
  প্যাটার্ন অনুসরণ করা উচিত (`NetworkRequest.Builder()`-এ শুধু `NET_CAPABILITY_INTERNET`
  request করা থাকলেও `getNetworkCapabilities()`-এ `NET_CAPABILITY_VALIDATED` উপস্থিত কিনা
  চেক করা সম্ভব, `NetworkRequest`-এ আলাদা করে VALIDATED add করার দরকার নেই — capability
  presence আলাদা জিনিস request filter থেকে)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে sub-step ২ (আসল ফিক্স) শুরুর আগে (যদি
  না ব্যবহারকারী "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- এই সেশনে **কোনো production Kotlin কোড/migration/SQL/MCP ছোঁয়া হয়নি** — শুধু test ফাইলে একটা
  নতুন `@Test` যোগ, progress doc, `BUG_INVENTORY.md`।
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১b: 🟠 → 🔴, প্যারেন্ট ৩.১-এর প্রমাণ-কলাম আপডেট — ইতিমধ্যে এই
  সেশনেই করা হয়েছে।

### 🧾 zip ফাইল-সংখ্যা ও dotfile যাচাই (Step 6 sub-step ১)
input (`somadhan-bug-fix-step6-next__1_.zip`): ৩৮৪ ফাইল, `.env`/`.gitignore`/`.env.example`
তিনটা dotfileই আছে। এই সেশনে **শুধু ৩টা ফাইল এডিট হয়েছে** (`OfflineGatingSyncTest.kt`,
`FIX_PROGRESS.md`, `BUG_INVENTORY.md`) — কোনো নতুন ফাইল যোগ/মুছে ফেলা হয়নি, তাই আউটপুট zip-এও
৩৮৪ ফাইল থাকা উচিত।

## ✅ Step 6 — sub-step ২ (৩.১b কোড-ফিক্স) সম্পূর্ণ (একই সেশন) — Windows-verify বাকি

ব্যবহারকারী "Continue" বলে sub-step ২ (আসল ফিক্স) শুরুর confirmation দিয়েছেন (rule #৯ satisfied)।
কোনো নতুন `WINDOWS RESULT:` এই মেসেজে ছিল না।

**যা বদলেছে:** `SomadhanViewModel.kt`-এর `startConnectivityObserver()` (~১৮৩০-১৯০২, লাইন-নাম্বার
sub-step ১-এর তুলনায় শিফট হয়েছে ফাংশনের ভেতরেই নতুন কোড যোগ হওয়ায়) —
- ফাংশনের ভেতরে একটা local `fun isReallyOnline(caps: NetworkCapabilities?): Boolean` যোগ করা
  হয়েছে যেটা `NET_CAPABILITY_INTERNET && NET_CAPABILITY_VALIDATED` (AND, OR না) চেক করে।
- শুরুর মান (`_isOnline.value = ...`), `onAvailable()`, `onLost()`-এর `stillHasOther` — তিনটাই
  এখন পুরনো `hasCapability(NET_CAPABILITY_INTERNET)`-এর বদলে `isReallyOnline(...)` ব্যবহার করে।
- একটা নতুন `onCapabilitiesChanged()` override যোগ করা হয়েছে (আগে ছিল না) — captive-portal
  নেটওয়ার্কে validation পরে সম্পূর্ণ হয়/হারায়, শুধু `onAvailable()`/`onLost()` দিয়ে সেই পরিবর্তন ধরা
  যায় না (Android framework matched network-এর যেকোনো capability-change-এ এই callback fire করে,
  request-এ ঠিক সেই capability চাওয়া না থাকলেও)।
- `onAvailable()`/`onCapabilitiesChanged()`-এ retry-trigger শর্ত `!wasOnline` থেকে
  `!wasOnline && nowOnline` করা হয়েছে (আগে `_isOnline.value = true` unconditional ছিল বলে
  `!wasOnline` যথেষ্ট ছিল, এখন `nowOnline` `false`-ও হতে পারে যদি captive-portal হয় — তাই সেই
  ক্ষেত্রে ভুলভাবে retry ট্রিগার করা এড়ানো হলো)।
- `NetworkRequest.Builder()` **অপরিবর্তিত** রাখা হয়েছে (শুধু `NET_CAPABILITY_INTERNET` request
  করে) — rule #৩ ন্যূনতম-এডিট: এতে করে callback matched-network-এর *সব পরবর্তী capability-change*
  পায় (Android framework আচরণ), request-এ VALIDATED যোগ করলে matched-network-এর সেট নিজেই বদলে
  যেত (ঝুঁকিপূর্ণ, না-প্রমাণিত সাইড-ইফেক্ট), যেখানে read-time AND-চেকই যথেষ্ট এই বাগ ফিক্স করতে।

**কেন `NetworkConnectivityObserver.kt`-এর OR-প্যাটার্ন অনুসরণ করা হয়নি (আগের HANDOFF-এ যা প্রস্তাবিত
ছিল, কিন্তু এই সেশনে সংশোধন করা হলো):** সেই ফাইলের বাগ ছিল false-negative (hardcoded `true`
fallback, বা কোনো capability-ই ধরা না পড়লে ভুলভাবে অফলাইন দেখানো) — সমাধান ছিল বেশি permissive
হওয়া (OR)। এখানকার বাগ (৩.১b) তার উল্টো — false-positive (captive-portal-এ `INTERNET` থাকলেও
`_isOnline=true` দেখানো, যদিও আসল ব্যবহার সম্ভব না) — তাই সমাধান বেশি strict হওয়া (AND) দরকার। দুটো
প্যাটার্ন ভিন্ন হওয়াটাই সঠিক, দুটো ভিন্ন সমস্যার জন্য।

**যাচাই:** Sandbox-এ real Gradle এখনো চালানো যায়নি (network নেই)। `functionRegions`-এর regex-লজিক
Python-এ replicate করে ফাইলের বর্তমান টেক্সটের বিপরীতে re-confirm করা হয়েছে: `startConnectivityObserver()`-এর
region (এখন লাইন ১৮৩০-১৯০২) এ `NET_CAPABILITY_VALIDATED` স্ট্রিং **এখন উপস্থিত** (`isReallyOnline`
লোকাল fun হিসেবে একই region-এর ভেতরেই, আলাদা member-fun হিসেবে extract করা হয়নি ইচ্ছাকৃতভাবে —
টেস্টের assertion স্কোপ `startConnectivityObserver()`-এর region-এর মধ্যেই, তাই AND-চেক লজিক আলাদা
helper member-fun-এ বের করলে টেস্ট নিজেই false-fail করত, এটাও একটা কারণ কেন local-fun ব্যবহার করা
হয়েছে, member-level না)। brace/paren balance পুরো `SomadhanViewModel.kt`-তে (open==close, ১৪৩৯==১৪৩৯
braces, ৩৫৮৪==৩৫৮৪ parens) — সিনট্যাক্স-নিরাপদ কনফার্মড। `functionRegions`-এর member-fun-boundary
detection-ও re-confirmed (২৫৩টা member fun total, `onCleared()`-এর মতো পরের ফাংশনের boundary
সঠিকভাবেই শিফট হয়েছে, কোনো ভাঙন নেই)। বাকি সব `OfflineGatingSyncTest.kt` টেস্ট (৯৫টা গার্ডেড ফাংশন,
`requireOnlineOrWarn`, `solverCancelAcceptedJob`, dry-run exception, intentionally-unguarded,
`MainActivity` branching) — কোনোটাই এই এডিটে প্রভাবিত হয়নি (সম্পূর্ণ ভিন্ন ফাংশন/ফাইল), regression
হওয়ার কারণ নেই।

**টেস্ট রিনেম:** `` `startConnectivityObserver checks NET_CAPABILITY_VALIDATED before marking isOnline true (BUG 3_1b, currently failing)` ``
→ `` `startConnectivityObserver checks NET_CAPABILITY_VALIDATED before marking isOnline true (BUG 3_1b fixed)` ``
(Step 2/3-এর precedent অনুযায়ী, "currently failing" আর সত্যি না)।

**Windows-verify লাগবে (ব্যবহারকারীর মেশিনে):**
```
.\gradlew.bat testDebugUnitTest --tests "*OfflineGatingSyncTest*"
```
প্রত্যাশিত: `BUILD SUCCESSFUL`, সবকয়টা টেস্ট pass (নতুন রিনেমড টেস্টটাসহ)।

**Status আপডেট (কোড-ফিক্স সম্পূর্ণ, এখনো Windows-verified না):**
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১b সারি: 🔴 (NOT FIXED, TEST EXISTS) → 🟡 (FIXED, NOT VERIFIED)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 6 checkbox এখনো `[ ]` (৩.১a/৩.১c এখনো বাকি, আর
  rule #৫ অনুযায়ী Windows real-run pass কনফার্মড না হওয়া পর্যন্ত `[x]` হবে না)।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 6 sub-step ২ কোড-ফিক্স সম্পূর্ণ, Windows-verify বাকি) — পরের ধাপ: Step 6-এর ৩.১b-এর WINDOWS RESULT অথবা (ব্যবহারকারী verify স্কিপ করে এগোতে চাইলে) sub-step ৩ (৩.১a ফিক্স)
- **যদি পরের মেসেজে `WINDOWS RESULT:` আসে** (উপরের কমান্ডের আউটপুট) — সেটা প্রথমে প্রসেস করতে হবে:
  pass হলে `BUG_INVENTORY.md`-এ ৩.১b: 🟡 → ✅, তারপর rule #৯ অনুযায়ী sub-step ৩ শুরুর জন্য
  confirmation চাইতে হবে। fail হলে sub-step ২ আবার খুলে root-cause ধরতে হবে (sub-step ৩-এ যাওয়া
  যাবে না) — বিশেষভাবে `onCapabilitiesChanged()` override real device/emulator-এ `onAvailable()`-এর
  সাথে duplicate/conflicting call করছে কিনা লক্ষ্য রাখতে হবে (sandbox-এ রানটাইম টেস্ট করা যায়নি)।
- **sub-step ৩ (৩.১a ফিক্স, dual-write ব্যর্থতা surface করা):** `SomadhanRepository.updatePlatformSetting()`
  (~লাইন ৮০০৫-৮০২৬, CI_TEST_SUITE_PROGRESS.md Step 16.2 রেফারেন্স, লাইন-নাম্বার ফাংশন-নাম দিয়ে
  আবার কনফার্ম করা উচিত)-এর cloud dual-write (`SupabaseSyncManager.upsertPlatformSetting`)
  ব্যর্থ হলে caller-কে জানানো — `updateWithdrawalStatus()`-এর sealed-result প্যাটার্ন (Step 3-এর
  precedent) অনুসরণ করে caller (`adminUpdatePlatformSetting()` ViewModel ফাংশন) conditional
  toast দেখাবে। কোনো automated test নেই (রানটাইম Supabase-নির্ভর) — শুধু কোড-লেভেল sanity-check
  (function-region diff)।
- **sub-step ৪ (৩.১c ফিক্স, degraded-session সতর্কতা persistent করা):** `loginAsAdmin()`-এর
  transient "⚠️ সীমিত ভিউ..." toast persistent/dismissible-not-auto-hide করা, বা Settings স্ক্রিনে
  সেশন-স্ট্যাটাস দেখানো — এক্সাক্ট UI approach ব্যবহারকারীর সাথে কনফার্ম করা উচিত sub-step ৩ শেষ
  হওয়ার পর (এখনো কোনো সিদ্ধান্ত নেওয়া হয়নি এই সেশনে)।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে sub-step ৩ শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- এই সেশনে **কোনো migration/SQL/MCP ছোঁয়া হয়নি** — শুধু `SomadhanViewModel.kt` (Kotlin প্রোডাকশন
  কোড), `OfflineGatingSyncTest.kt` (রিনেম), `FIX_PROGRESS.md`, `BUG_INVENTORY.md`।
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১b: 🔴 → 🟡 — ইতিমধ্যে এই সেশনেই করা হয়েছে।

## ✅ Step 6 — ৩.১b-এর WINDOWS RESULT প্রসেস করা হলো (২০২৬-০৯-২৩, নতুন সেশন) — Windows-verified

ব্যবহারকারী real Windows PowerShell আউটপুট পেস্ট করেছেন (document হিসেবে) — বুটস্ট্র্যাপ rule
অনুযায়ী নতুন কোনো Step নেওয়ার আগে এটা প্রথমে প্রসেস করা হলো।

**আসল ফলাফল:** `$env:JAVA_HOME`/`$env:PATH` সঠিকভাবে সেট করে `java -version` সফল (OpenJDK
25.0.2), তারপর `.\gradlew.bat testDebugUnitTest --tests "*OfflineGatingSyncTest*"` চলেছে —
ঠিক sub-step ২-এর HANDOFF-এ চাওয়া exact কমান্ড। `compileDebugKotlin` সফল (শুধু pre-existing
deprecation/condition warning — sub-step ২-এর এডিট-করা `startConnectivityObserver()` নিয়ে কোনো
নতুন warning/error নেই, সিনট্যাক্স ঠিক ছিল সেই সময়ের ম্যানুয়াল brace-balance চেকের সাথে সঙ্গতিপূর্ণ)।
শেষে **`BUILD SUCCESSFUL in 2m 19s`** — কোনো টেস্ট FAILED দেখায়নি, `--tests` ফিল্টার-করা
`OfflineGatingSyncTest`-এর সবকয়টা টেস্ট pass (নতুন রিনেমড `...BUG 3_1b fixed` টেস্টসহ, "No tests
found" এরর-ও হয়নি, তাই ফিল্টার-ম্যাচ ও এক্সিকিউশন দুটোই কনফার্মড)।

**উপসংহার:** Step 6 sub-step ২ (৩.১b ফিক্স)-এর কোড real Gradle run-এ pass কনফার্মড।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১b সারি: 🟡 → **✅ FIXED + VERIFIED**।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 6 checkbox এখনো `[ ]` (rule #৫ অনুযায়ী — ৩.১a/৩.১c
  এখনো বাকি, পুরো Step 6 (গ্রুপ ৩-এর তিনটা সাব-বাগই) সম্পূর্ণ+verified না হওয়া পর্যন্ত `[x]` হবে না,
  ঠিক Step 2-5-এর precedent-এ যেমন প্রতিটা sub-bug আলাদাভাবে ভেরিফাই হয়েছে কিন্তু checkbox শুধু
  গ্রুপের শেষ sub-bug-এই `[x]` হয়েছে)।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ৩.১b সম্পূর্ণ ✅ Windows-verified) — পরের ধাপ: Step 6 sub-step ৩ (৩.১a ফিক্স)
- **পরের ধাপ: Step 6-এর ভেতরে sub-step ৩ — ৩.১a ফিক্স (dual-write ব্যর্থতা surface করা)।**
  `SomadhanRepository.updatePlatformSetting()` (~লাইন ৮০০৫-৮০২৬, CI_TEST_SUITE_PROGRESS.md
  Step 16.2 রেফারেন্স — লাইন-নাম্বার ফাংশন-নাম দিয়ে আবার কনফার্ম করা উচিত, sub-step ২-এর মতো শিফট
  হয়ে থাকতে পারে) — cloud dual-write (`SupabaseSyncManager.upsertPlatformSetting`) ব্যর্থ হলে
  caller-কে জানানো, `updateWithdrawalStatus()`-এর sealed-result প্যাটার্ন (Step 3-এর precedent)
  অনুসরণ করে; caller (`SomadhanViewModel.adminUpdatePlatformSetting()`) তখন conditional toast
  দেখাবে (সবসময় success-toast না)। কোনো automated test নেই (runtime Supabase-নির্ভর) — শুধু
  কোড-লেভেল function-region diff দিয়ে sanity-check করা হবে, ঠিক Step 4/5-এর প্যাটার্নে।
- এরপর sub-step ৪ (৩.১c ফিক্স, degraded-session সতর্কতা persistent করা) — `loginAsAdmin()`-এর
  UI approach ব্যবহারকারীর সাথে কনফার্ম করা উচিত sub-step ৩ শেষ হওয়ার পর।
- rule #৯ অনুযায়ী ব্যবহারকারীর explicit confirmation লাগবে sub-step ৩ শুরুর আগে (যদি না ব্যবহারকারী
  "সব ধাপ একসাথে চালিয়ে যাও" বলেন)।
- এই সেশনে **কোনো কোড ছোঁয়া হয়নি** — শুধু `WINDOWS RESULT` প্রসেস করে `BUG_INVENTORY.md`/
  `FIX_PROGRESS.md` আপডেট।
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১b: 🟡 → ✅ — ইতিমধ্যে এই সেশনেই করা হয়েছে।

## ✅ Somadhan Bug-Fix Step 6 sub-step ৩ (৩.১a ফিক্স) — কোড-ফিক্স সম্পূর্ণ (২০২৬-০৯-২৩, নতুন সেশন)

Bootstrap অনুযায়ী `SOMADHAN_BUG_FIX_MASTER_PROMPT.md` → `BUG_INVENTORY.md` → `FIX_PROGRESS.md`-এর
শেষ HANDOFF (৩.১b Windows-verified, পরের ধাপ sub-step ৩) পড়ে কনফার্ম করা হলো। এই সেশনের মেসেজে
কোনো `WINDOWS RESULT:` ছিল না। ব্যবহারকারী প্রতি সেশনে এই একই bootstrap prompt পেস্ট করাকেই
"যেখানে আগের session ছেড়ে গেছে সেখান থেকে চালিয়ে যাও" নির্দেশ হিসেবে গণ্য করতে বলেছেন (rule #৯-এর
continuation-confirmation হিসেবে নেওয়া হলো), তাই সরাসরি sub-step ৩ শুরু হলো।

**Verify (কোড না ছুঁয়ে):**
- zip-এর ফাইল-সংখ্যা/গঠন গত entry-র সাথে সঙ্গতিপূর্ণ (নতুন কিছু যোগ হয়নি, শুধু আগের সেশনের এডিট)।
- `grep -n "fun updatePlatformSetting"`/`"fun adminUpdatePlatformSetting"` দিয়ে exact লাইন কনফার্ম
  করা হলো (HANDOFF-এর ~৮০০৫-৮০২৬ রেফারেন্স সামান্য শিফটেড — actual লাইন ৮০২৬ ও ৬২৬৩)।

**কী বদলেছে:**
- `SomadhanRepository.kt`: নতুন sealed class `PlatformSettingUpdateResult` (`Updated` /
  `CloudSyncFailed(reason)`) — `WithdrawalUpdateResult`-এর একই কনভেনশনে, `WithdrawalUpdateResult`
  ঘোষণার ঠিক নিচে। `updatePlatformSetting()`-এর রিটার্ন টাইপ `Unit` থেকে
  `PlatformSettingUpdateResult`-এ বদলানো — cloud dual-write success/failure অনুযায়ী `.fold()` দিয়ে
  result তৈরি, local write ও `logAdminAction()` behavior অপরিবর্তিত।
- `SomadhanViewModel.kt`-এর `adminUpdatePlatformSetting()` (যেটা `strict_offline_block` টগলও সেভ
  করে) — নতুন রিটার্ন-ভ্যালু `when`-এ চেক করে conditional toast (`Updated` → সফল বার্তা,
  `CloudSyncFailed` → লোকাল-সেভ-হয়েছে-কিন্তু-ক্লাউড-ব্যর্থ বার্তা reason-সহ)।
- **অন্য কোনো caller ছোঁয়া হয়নি** (rule #৩, minimal/targeted): `adminSetPhysicalWorkEnabled()`,
  `adminSetVirtualWorkEnabled()`, `adminBatchUpdatePlatformSettings()`,
  `adminSaveCustomReputationEvent()`, `adminDeleteCustomReputationEvent()`,
  `adminToggleReputationEvent()` — এগুলো `repository.updatePlatformSetting()`-কে statement/forEach
  lambda হিসেবে কল করে, রিটার্ন-ভ্যালু discard করে; Kotlin-এর unit-coercion-এর কারণে এগুলো compile
  হবে (breaking change না), কিন্তু এই বাগ-রিপোর্ট শুধু `strict_offline_block`/generic admin-settings
  toggle নিয়ে বলে সেগুলোর caller-সাইড conditional-toast এই সেশনে যোগ করা হয়নি — চাইলে ভবিষ্যতে
  একই প্যাটার্নে যোগ করা যাবে (এখন backlog না, শুধু note)।
- এই সেশনে **কোনো migration/SQL/MCP ছোঁয়া হয়নি** — শুধু `SomadhanRepository.kt`,
  `SomadhanViewModel.kt`, `FIX_PROGRESS.md`, `BUG_INVENTORY.md`।

**Verify পদ্ধতি:** master prompt sub-step ৩-এর নির্দেশ অনুযায়ী কোনো automated test নেই (runtime
Supabase-নির্ভর) — শুধু কোড-লেভেল sanity: brace-balance কাউন্ট (দুটো ফাইলেই `{`/`}` সংখ্যা সমান),
`when` exhaustive কিনা (sealed class-এর দুটো subclass-ই কভার করা, `else` লাগে না) ম্যানুয়ালি
যাচাই করা হয়েছে। প্রকৃত compile/manual-app-test ব্যবহারকারীর Windows মেশিনে বাকি।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ ৩.১a সারি: 🟠 → 🟡 ("FIXED, NOT VERIFIED" — automated test নেই, শুধু
  code-level sanity, real-device manual verify বাকি)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 6 checkbox এখনো `[ ]` (৩.১c এখনো বাকি)।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ৩.১a কোড-ফিক্স সম্পূর্ণ, কোনো automated test নেই তাই manual-verify) — পরের ধাপ: Step 6 sub-step ৪ (৩.১c ফিক্স)
- **পরের ধাপ: Step 6-এর ভেতরে sub-step ৪ — ৩.১c ফিক্স (degraded-session সতর্কতা persistent করা)।**
  `SomadhanViewModel.loginAsAdmin()`-এর transient "⚠️ সীমিত ভিউ..." toast persistent/dismissible-
  not-auto-hide করা, বা Settings স্ক্রিনে সেশন-স্ট্যাটাস দেখানো — master prompt অনুযায়ী **এক্সাক্ট UI
  approach ব্যবহারকারীর সাথে কনফার্ম করা উচিত** শুরুর আগে (এখনো কোনো সিদ্ধান্ত নেওয়া হয়নি)।
- rule #৯ অনুযায়ী sub-step ৪ শুরুর আগে ব্যবহারকারীর explicit confirmation/UI-approach সিদ্ধান্ত লাগবে
  (এটা শুধু "চালিয়ে যাও" বললেই যথেষ্ট না — UI approach নিজেই একটা open প্রশ্ন)।
- এই সেশনে ৩.১a-এর জন্য **`SomadhanRepository.kt` ও `SomadhanViewModel.kt`** এডিট হয়েছে (উপরে
  বিস্তারিত) — কোনো migration/SQL ছোঁয়া হয়নি।
- `BUG_INVENTORY.md`-এ গ্রুপ ৩.১a: 🟠 → 🟡 — ইতিমধ্যে এই সেশনেই করা হয়েছে।
- sub-step ৪ শেষ হলে (এবং কোনো fashion-এ verify হলে) পুরো Step 6 (গ্রুপ ৩-এর তিনটাই) সম্পূর্ণ ধরা
  হবে এবং master prompt-এ Step 6 checkbox `[x]` করা যাবে (rule #৫ অনুযায়ী — ৩.১b Windows-verified
  ✅, ৩.১a/৩.১c code-level, চূড়ান্ত checkbox-এর জন্য অন্তত compile/manual sanity সব কয়টাতেই লাগবে)।

## ✅ Somadhan Bug-Fix Step 6 sub-step ৪ (৩.১c ফিক্স) — কোড-ফিক্স সম্পূর্ণ (২০২৬-০৯-২৩, একই সেশন)

sub-step ৩ শেষে HANDOFF অনুযায়ী sub-step ৪ শুরুর আগে UI approach ব্যবহারকারীর সাথে কনফার্ম করা হলো
(master prompt-এর নির্দেশ অনুযায়ী)। ব্যবহারকারী দুটো অপশনের মধ্যে **"(ক) persistent
dismissible bar"** approach বেছে নিয়েছেন (Settings-এ status দেখানোর বদলে)।

**কী বদলেছে:**
- `SomadhanViewModel.kt`: দুটো নতুন `StateFlow` — `isDegradedAdminSession: StateFlow<Boolean>` ও
  `degradedAdminSessionReason: StateFlow<String?>` (`_isRefreshing`-এর ঠিক নিচে, একই কনভেনশনে)।
  `loginAsAdmin()`-এর শুরুতে প্রতিটা লগইন-চেষ্টায় রিসেট (`false`/`null`), `onFailure`/`catch` দুটো
  ব্যর্থতা-পথেই (আগে যেখানে শুধু transient toast ছিল) এখন `true` + reason সেট হয়। নতুন
  `dismissDegradedAdminSessionWarning()` ফাংশন (admin "✕"-এ ট্যাপ করলে কল হয়, শুধু UI dismiss করে,
  auth state বদলায় না)। `logout()`-এ দুটো StateFlow রিসেট করা হয়েছে (নাহলে পরের non-admin লগইনেও
  পুরনো bar persist করত)।
- নতুন ফাইল `app/src/main/java/com/example/ui/components/DegradedAdminSessionBar.kt` —
  `RealtimeLocationBar.kt`-এর একই স্টাইল-কনভেনশন অনুসরণ করে একটা লাল (`SomadhanError`/
  `SomadhanErrorLight`) `AnimatedVisibility` bar, ওয়ার্নিং আইকন + reason টেক্সট + dismiss "✕"
  `IconButton`।
- `AdminPanelScreen.kt`: `isDegradedAdminSession`/`degradedAdminSessionReason` StateFlow
  `collectAsStateWithLifecycle()` দিয়ে collect করা হয়েছে, `DegradedAdminSessionBar` টপ-বারের
  `Column`-এ `RealtimeLocationBar`-এর ঠিক নিচে বসানো হয়েছে — যেহেতু পুরো AdminPanelScreen (সব ১৩+
  ট্যাব) একই `Scaffold`-এর ভেতরে একটা `when (selectedTabIndex)`-এ আছে, তাই এই একটা জায়গায় বসালেই
  bar-টা প্রতিটা admin ট্যাবে persistent থাকবে (ট্যাব বদলালেও হারাবে না, ঠিক যেমন
  `RealtimeLocationBar` এখনই সব ট্যাবে থাকে)।
- এই সেশনে **কোনো migration/SQL/MCP ছোঁয়া হয়নি** — শুধু `SomadhanViewModel.kt`,
  `AdminPanelScreen.kt`, নতুন `DegradedAdminSessionBar.kt`, `FIX_PROGRESS.md`, `BUG_INVENTORY.md`।

**Verify পদ্ধতি:** master prompt অনুযায়ী কোনো automated test নেই (Compose UI + runtime
Supabase-নির্ভর, structural unit test দিয়ে reliably demonstrate করা সম্ভব না)। শুধু কোড-লেভেল
sanity করা হয়েছে: তিনটা এডিট-করা/নতুন ফাইলেই brace/paren balance কাউন্ট মিলেছে, `Icons.Default.
Warning`/`Icons.Default.Close` কোডবেসের অন্য জায়গায় already-working ব্যবহার আছে তা `grep` দিয়ে
কনফার্ম করা হয়েছে (নতুন `WarningAmber` ব্যবহার করা হয়নি ঝুঁকি এড়াতে)। প্রকৃত compile ও ম্যানুয়াল
app-test (admin phone/password ইচ্ছাকৃতভাবে ভুল দিয়ে বা network বন্ধ রেখে login করে bar দেখা যাচ্ছে
কিনা, dismiss কাজ করছে কিনা, ট্যাব বদলালে bar থেকে যাচ্ছে কিনা, সফল লগইনে bar না আসছে কিনা,
logout-এর পর আবার লগইন করলে পুরনো bar না থেকে যাচ্ছে কিনা) ব্যবহারকারীর Windows/ডিভাইসে বাকি।

**Status আপডেট:**
- `BUG_INVENTORY.md`-এ ৩.১c সারি: 🟠 → 🟡 ("FIXED, NOT VERIFIED")।
- `BUG_INVENTORY.md`-এ ৩.১ (গ্রুপ-লেভেল সারি): 🟠 → 🟡 (৩.১b ✅, ৩.১a/৩.১c 🟡 — কোনোটাই আর 🔴/অ-শুরু
  না, কিন্তু পুরো গ্রুপ এখনো verified না)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 6 checkbox এখনো `[ ]` — rule #৫ ও Step 2-5-এর
  precedent অনুযায়ী পুরো Step 6 (গ্রুপ ৩-এর তিনটা সাব-বাগ) সম্পূর্ণ**+verified** না হওয়া পর্যন্ত
  `[x]` হবে না। এখন তিনটাই কোড-ফিক্স সম্পূর্ণ, কিন্তু ৩.১a/৩.১c-এর real-device/manual verify বাকি।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 6-এর তিনটা সাব-বাগই কোড-ফিক্স সম্পূর্ণ — ৩.১b Windows-verified, ৩.১a/৩.১c manual-verify বাকি) — পরের ধাপ: ৩.১a/৩.১c-এর manual verify, তারপর Step 7
- **পরের ধাপ (দুটো বিকল্প, ব্যবহারকারীর সিদ্ধান্ত লাগবে rule #৯ অনুযায়ী):**
  (ক) ব্যবহারকারী নিজে app চালিয়ে ৩.১a (settings/toggle পরিবর্তনে cloud-fail হলে সঠিক toast আসছে
  কিনা) ও ৩.১c (degraded admin session-এ persistent bar, dismiss, tab-persistence, সফল লগইনে না
  আসা, logout-এ ক্লিয়ার হওয়া) ম্যানুয়ালি verify করে `BUG_INVENTORY.md`-এ 🟡 → ✅ করবেন (verify
  ফলাফল এই থ্রেডে জানালে GATE অনুযায়ী Step 6 checkbox `[x]` করা হবে), অথবা
  (খ) ব্যবহারকারী manual-verify স্কিপ করে সরাসরি **Step 7** (backlog আইটেম আলোচনা)-এ যেতে চাইলে
  বলবেন — তখন Step 6 checkbox `[ ]`-ই থাকবে (কোড-ফিক্স সম্পূর্ণ কিন্তু verified-না হিসেবে নোট থেকে
  যাবে) এবং সেই অনুযায়ী `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ একটা নোট যোগ করা হবে।
- rule #৯ অনুযায়ী কোনো Step শুরুর আগে (এখানে Step 7) ব্যবহারকারীর explicit confirmation লাগবে।
- এই সেশনে edit হয়েছে: `SomadhanViewModel.kt`, `AdminPanelScreen.kt`, নতুন
  `DegradedAdminSessionBar.kt` (Kotlin production code + নতুন Compose ফাইল) — কোনো migration/SQL
  ছোঁয়া হয়নি।
- `BUG_INVENTORY.md`-এ ৩.১c: 🟠 → 🟡, গ্রুপ-লেভেল ৩.১: 🟠 → 🟡 — ইতিমধ্যে এই সেশনেই করা হয়েছে।

## 🔁 HANDOFF (২০২৬-০৯-২৩, WINDOWS RESULT প্রসেস করা হয়েছে — `installDebug` compile-fail, root-cause ফিক্স করা হয়েছে) — পরের ধাপ: ব্যবহারকারী আবার `.\gradlew.bat installDebug` চালাবেন, তারপর ৩.১a/৩.১c manual verify
- **WINDOWS RESULT (ব্যবহারকারী পেস্ট করেছেন):** `.\gradlew.bat installDebug` → `compileDebugKotlin FAILED`।
  `SomadhanViewModel.kt:6308-6312`-এ `PlatformSettingUpdateResult.Updated`/`.CloudSyncFailed`/`.reason`
  "Unresolved reference" + non-exhaustive `when` error।
- **Root cause:** ৩.১a-এর আগের সেশনে (Step 6 sub-step ৩) `PlatformSettingUpdateResult` sealed class
  `SomadhanRepository.kt` (প্যাকেজ `com.example.data.repository`)-এ যোগ করা হয়েছিল এবং
  `SomadhanViewModel.kt` (প্যাকেজ `com.example.ui.viewmodel`)-এ ব্যবহার করা হয়েছিল, কিন্তু
  `import com.example.data.repository.PlatformSettingUpdateResult` লাইনটা যোগ করা হয়নি — তাই এতদিন
  code-level sanity-check (brace-balance) এটা ধরতে পারেনি, শুধু real compile-এই ধরা পড়ল।
- **ফিক্স:** `SomadhanViewModel.kt`-এ `import com.example.data.repository.WithdrawalUpdateResult`-এর
  পরে `import com.example.data.repository.PlatformSettingUpdateResult` যোগ করা হয়েছে। আর কোনো লাইন
  বদলানো হয়নি (minimal, targeted — শুধু missing import)।
- এই সেশনে edit হয়েছে: শুধু `SomadhanViewModel.kt` (১ লাইন import যোগ)। কোনো migration/SQL ছোঁয়া হয়নি।
- `BUG_INVENTORY.md`-এ ৩.১a-এর নোটে এই compile-fail + fix উল্লেখ করা হয়েছে, Status এখনো 🟡 (real
  build দিয়ে re-verify বাকি, তারপর manual UI verify)।
- **পরের ধাপ:** ব্যবহারকারী আবার `.\gradlew.bat installDebug` চালাবেন (নতুন zip দিয়ে, একই
  `local.properties`/`JAVA_HOME` সেটআপ বজায় রেখে)। এবার build সফল হলে app ইনস্টল হয়ে যাবে, তারপর
  আগের মেসেজের ৩.১a/৩.১c manual checklist অনুযায়ী verify করে ফলাফল জানাবেন।

## 🔁 HANDOFF (২০২৬-০৯-২৩, WINDOWS RESULT প্রসেস করা হয়েছে — compile fix কনফার্মড, `installDebug` device না থাকায় fail) — পরের ধাপ: device/emulator কানেক্ট করে আবার `installDebug`
- **WINDOWS RESULT:** `compileDebugKotlin` এবার **কোনো error ছাড়াই সফল** (আগের import-fix কাজ করেছে,
  শুধু deprecation warning-গুলো এসেছে যেগুলো এই বাগ-ফিক্স effort-এর স্কোপের বাইরে, touch করা হয়নি)।
  `installDebug` fail হয়েছে কিন্তু কোড-সংক্রান্ত কারণে না — `DeviceException: No connected devices!`,
  অর্থাৎ কোনো physical device/emulator connected/running ছিল না তখন।
- এটা কোনো নতুন কোড বাগ না, শুধু পরিবেশ (environment) সমস্যা — কোনো ফাইল এডিট করা হয়নি এই রাউন্ডে।
- **পরের ধাপ:** ব্যবহারকারী একটা device/emulator কানেক্ট/চালু করে আবার
  `.\gradlew.bat installDebug` চালাবেন, তারপর app খুলে ৩.১a/৩.১c manual checklist verify করবেন।

## 🔁 HANDOFF (২০২৬-০৯-২৩, Step 1-এর regression ধরা পড়েছে ও ফিক্স করা হয়েছে — real device-এ user/solver login, role-switch, admin-login সব আটকে যাচ্ছিল) — পরের ধাপ: device-এ re-verify (build+install), তারপর ৩.১a/৩.১c checklist
- **ব্যবহারকারীর রিপোর্ট (real device):** শুধু admin-ই না — normal user/solver হিসেবে OTP verify করার পরও
  loading আটকে থেকে যাচ্ছিল (login সম্পন্নই হচ্ছিল না UI-তে), role switch (user↔solver) করলেও
  loading বাটন চিরকাল ঘুরতে থাকছিল। App force-kill করে আবার খুললে দেখা যাচ্ছিল login/role-switch
  আসলে হয়ে গেছে (state persisted) — শুধু UI কখনো এগোয়নি।
- **Root cause:** Somadhan Bug-Fix **Step 1**-এ (`completeLoginAfterOtp`/`switchRoleToSolver`/
  `switchRoleToUser`/`loginAsAdmin`-এ `startRealtimeListeners()` কল যোগ করা) `_currentUser`/role
  state আগে কমিট হয়, **তারপর** `runCatching { startRealtimeListeners() }`, `onSuccess()/onReady()`
  তার পরে। কিন্তু `RealtimeChannel.subscribe()`-এর কোনো নিজস্ব timeout নেই — network/WebSocket কোনো
  কারণে (internet থাকা অবস্থাতেও, ব্যবহারকারী-কনফার্মড) subscribe আটকে গেলে `runCatching` কোনো
  exception না পেয়ে suspend অবস্থাতেই থেকে যায়, `onSuccess()/onReady()` কখনো কল হয় না — UI চিরকাল
  loading-এ আটকে থাকে। এটা Step 1-এর `RealtimeSubscriptionScopeTest.kt` ধরতে পারেনি কারণ সেটা
  সম্ভবত network layer mock করে (real WebSocket hang সিমুলেট করে না)।
- **ফিক্স (`SupabaseRealtimeManager.kt`):** নতুন প্রাইভেট `RealtimeChannel.subscribeWithTimeout(label)`
  হেল্পার — এই ফাইলেই আগে থেকে থাকা `pullBulkDataFromSupabase()`-এর `withTimeout(20_000L)` precedent
  অনুসরণ করে, ১০ সেকেন্ড টাইমআউট, `runCatching` + `Log.w` (non-fatal)। `startRealtimeListeners()`-এর
  ১০টা `.subscribe()` কল (users/problems/bids/messages/transactions/escrows/gateway_payments/
  additional_charges/notifications/withdrawals) আর `startUserTopicBroadcastSubscription()`-এর ১টা
  — মোট ১১টা `.subscribe()` কল প্রতিস্থাপন করা হয়েছে। Login/role-switch path-এর বাইরের আরও ৩টা
  `.subscribe()` (চ্যাট/ডিসপিউট-সংশ্লিষ্ট, `joinProblemMessagesBroadcastChannel` ইত্যাদি)
  ইচ্ছাকৃতভাবে ছোঁয়া হয়নি (স্কোপ-ক্রিপ এড়াতে rule #৩) — `BUG_INVENTORY.md` গ্রুপ ৭.৬-এ নোট করা হয়েছে।
- এই সেশনে edit হয়েছে: `SupabaseRealtimeManager.kt` (১টা নতুন হেল্পার ফাংশন + ১১টা call-site বদল)।
  কোনো migration/SQL ছোঁয়া হয়নি।
- `BUG_INVENTORY.md`: নতুন সারি ১.২ (🟡, Step 1-এর regression হিসেবে গ্রুপ ১-এ যোগ), গ্রুপ ৭-এ নতুন
  ৭.৬ (backlog, বাকি ৩টা `.subscribe()`)।
- **পরের ধাপ:** ব্যবহারকারী নতুন zip দিয়ে আবার `.\gradlew.bat assembleDebug` চালিয়ে APK বানিয়ে
  ফোনে ইনস্টল করবেন, তারপর user/solver login + role-switch স্বাভাবিকভাবে কাজ করছে কিনা প্রথমে
  কনফার্ম করবেন (১.২-এর re-verify), তারপর আগের ৩.১a/৩.১c checklist অনুযায়ী verify করবেন।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ব্যবহারকারীর manual re-verify-তে ১.১/১.২-এর বাইরে আরেকটা root cause ধরা পড়েছে ও ফিক্স করা হয়েছে — ১.৩) — পরের ধাপ: real device-এ re-verify (১.২ ও ১.৩ দুটোই), তারপর ৩.১a/৩.১c checklist
- **ব্যবহারকারীর রিপোর্ট (real device, ১.১/১.২-এর ফিক্স verify করতে গিয়ে):** আগের মতোই bid-accept →
  account-switch করলে balance পুরনো মানে "reset" হয়ে যাচ্ছে, escrow ঠিকই locked দেখাচ্ছে, release
  করলেও balance আপডেট হচ্ছে না — কিন্তু এবার user wallet স্ক্রিনে নতুন একটা warning-ও দেখা গেছে:
  "কিছু লেনদেন cloud-এ sync হতে বাকি আছে" (`OutboxPendingIndicator`)।
- **Root cause (১.৩, ১.১/১.২ থেকে আলাদা):** `completeLoginAfterOtp()`-এর শুরুতে
  `repository.refreshUserDataFromCloud(userId)` কল হয়, যেটা Supabase থেকে user row টেনে
  `cacheUserLocally()` দিয়ে local balance/reputationScore/isBanned/isRestricted **unconditionally**
  ওভাররাইট করত — escrow/wallet dual-write RPC (`addToEscrow`/`payoutEscrowToSolver`/
  `refundEscrowOnceLocked`) cloud-push ব্যর্থ হয়ে `enqueueOutboxRetry()`-এর মাধ্যমে
  `pending_sync_outbox`-এ entry জমা থাকলেও (ঠিক সেটাই user-এর দেখা warning) চেক করত না। ফলে সেই
  window-এ Supabase-এর balance এখনো stale, আর account-switch করলে local (সঠিক, sync-pending)
  balance cloud-এর পুরনো মান দিয়ে চাপা পড়ে যাচ্ছিল। এটা Step 1 (realtime subscription re-scope)-এর
  থেকে সম্পূর্ণ আলাদা code path — তাই Step 1-এর ফিক্স/টেস্ট এটা কভার করেনি।
- **ফিক্স (`SomadhanRepository.kt`, `refreshUserDataFromCloud()`, লাইন ~৬৯৬):** `supabaseUser`
  পাওয়ার পর, cache করার আগে `pendingSyncOutboxDao.getPending()` চেক করা হচ্ছে — কোনো
  PENDING/RETRYING entry থাকলে mapped user-এর `balance`/`reputationScore`/`isBanned`/
  `isRestricted` (money/status-critical "active" ফিল্ড) `existingLocal`-এর মান দিয়ে overwrite
  করে (অর্থাৎ cloud-এর stale মান ignore করে local-কেই authoritative ধরা হয়), বাকি সব
  (profile/KYC/categories ইত্যাদি non-money) ফিল্ড আগের মতোই cloud থেকে আপডেট হয়। Legacy
  dual-role merge (`mergeLegacyDualRoleDataFromCloud`, `legacyDualRoleMergeDoneAt` guard) এই
  পরিবর্তনের বাইরে রাখা হয়েছে — সেটা আলাদা এককালীন per-device migration, রুল #৩ (স্কোপ-ক্রিপ
  এড়ানো) অনুযায়ী touch করা হয়নি।
- এই সেশনে edit হয়েছে: `SomadhanRepository.kt` (`refreshUserDataFromCloud()`-এর ভেতরে টার্গেটেড
  এডিট, নতুন কোনো ফাংশন/ফাইল না)। কোনো migration/SQL ছোঁয়া হয়নি।
- `BUG_INVENTORY.md`: গ্রুপ ১-এ নতুন সারি ১.৩ (🟡)।
- **কোনো automated test যোগ করা হয়নি** — ১.২-এর মতোই এটা Room+outbox+network টাইমিং-নির্ভর
  (real-device-only reproduce হয়, mock-based unit test-এ ধরা কঠিন), তাই rule #৪-এর "test না থাকলে
  bug-demonstrating test লেখা" নীতিটা এখানে ১.২-এর প্রতিষ্ঠিত precedent অনুসরণ করে বাদ দেওয়া হলো —
  পরিবর্তে manual re-verify-ই যাচাইয়ের একমাত্র পথ।
- **পরের ধাপ / manual verify (ব্যবহারকারীর জন্য):** নতুন zip দিয়ে `.\gradlew.bat assembleDebug` →
  ফোনে ইনস্টল → `REALTIME_RESCOPING_STEP1_TEST_CHECKLIST.md`-এর সেকশন ১ ও ২ আবার চালানো (একই bid-
  accept → account-switch → balance/escrow চেক)। এবার লক্ষ্য রাখতে হবে: (ক) login/role-switch আর
  আটকে থাকছে না তো (১.২), (খ) "sync বাকি" warning দেখা গেলেও balance আর পুরনো মানে "reset" হচ্ছে
  না (১.৩)। এরপর আগের মতোই ৩.১a/৩.১c checklist।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ব্যবহারকারীর real-device re-test-এ ১.২-এর ফিক্সের নিজেরই regression ধরা পড়েছে ও ফিক্স করা হয়েছে — ১.৪) — পরের ধাপ: real device-এ re-verify (১.২/১.৩/১.৪ তিনটাই), তারপর ৩.১a/৩.১c checklist
- **ব্যবহারকারীর রিপোর্ট:** ১.৩-এর ফিক্স দেওয়া zip থেকে APK বানিয়ে টেস্ট করার সময় জানিয়েছেন
  "৩.১a/৩.১c-এর login issue এখনও fix হয়নি" — অর্থাৎ ১.২ (login/role-switch UI loading-এ আটকে
  থাকা)-এর ফিক্সও পুরোপুরি কাজ করছে না।
- **Root cause (১.৪, ১.২-এর ফিক্সের নিজেরই regression):** ১.২-এ `subscribeWithTimeout()` হেল্পার
  (১০ সেকেন্ড হার্ড টাইমআউট) যোগ করা হয়েছিল ঠিকই, কিন্তু `startRealtimeListeners()`-এর ১০টা
  `subscribeWithTimeout()` কল (users/problems/bids/messages/transactions/escrows/
  gateway_payments/additional_charges/notifications/withdrawals) **sequentially** (একটার পর
  একটা `await` করে) কল হচ্ছিল। একই ডিগ্রেডেড network/WebSocket অবস্থায় (যেটা মূল ১.২ বাগের
  কারণ) প্রতিটা channel-ই নিজের পুরো ১০ সেকেন্ড টাইমআউট ব্যবহার করে — সিকোয়েন্সিয়াল হওয়ায় মোট
  worst-case wait ~১০×১০=১০০ সেকেন্ড (+ পরের broadcast subscription-এর আরও ~১০) হয়ে যাচ্ছিল।
  login চিরকালের জন্য আটকে থাকছিল না (আগের বাগের চেয়ে টেকনিক্যালি better), কিন্তু ~১-২ মিনিট
  UI loading-এ আটকে থাকাটা ব্যবহারকারীর কাছে "এখনও ভাঙা"-ই মনে হচ্ছিল।
- **ফিক্স (`SupabaseRealtimeManager.kt`, `startRealtimeListeners()`, লাইন ~১৬৯২):** ১০টা
  `subscribeWithTimeout()` কল এখন `coroutineScope { launch { ... } }` দিয়ে **সমান্তরালে**
  চালানো হচ্ছে — worst-case wait এখন সবচেয়ে ধীর একটা channel-এর ~১০ সেকেন্ডেই সীমাবদ্ধ। channel
  তৈরি/`postgresChangeFlow`/`launchIn` (network-নির্ভর না, সিঙ্ক্রোনাস সেটআপ) আগের মতোই sequential
  রাখা হয়েছে, শুধু নেটওয়ার্ক-নির্ভর `.subscribe()` কলগুলো parallel করা হয়েছে। প্রতিটা channel-এর
  নিজস্ব `runCatching` (`subscribeWithTimeout`-এর ভেতরেই) অক্ষত, তাই একটা channel fail/timeout
  হলেও বাকিগুলো/পুরো block ব্যর্থ হয় না। `startUserTopicBroadcastSubscription()`-এর নিজস্ব ১টা
  subscribe ইচ্ছাকৃতভাবে এই parallel block-এর বাইরে/sequential-ই রাখা হয়েছে (rule #৩, ন্যূনতম
  এডিট — worst-case বড়জোর আরও ~১০ সেকেন্ড যোগ করতে পারে, ভবিষ্যতে চাইলে একই scope-এ আনা যায়)।
- এই সেশনে edit হয়েছে: `SupabaseRealtimeManager.kt` (`startRealtimeListeners()`-এর ভেতরে
  টার্গেটেড রিফ্যাক্টর — নতুন কোনো ফাইল/পাবলিক API বদল না)। কোনো migration/SQL ছোঁয়া হয়নি।
- `BUG_INVENTORY.md`: গ্রুপ ১-এ নতুন সারি ১.৪ (🟡)।
- **কোনো automated test যোগ করা হয়নি** — ১.২/১.৩-এর মতোই এটা real network timing-নির্ভর, unit
  test-এ meaningfully simulate করা কঠিন (rule #৪-এর precedent অনুসরণ করা হলো)।
- **পরের ধাপ / manual verify:** নতুন zip দিয়ে আবার `.\gradlew.bat assembleDebug` → ইনস্টল → login/
  role-switch/admin-login এবার কত দ্রুত সম্পন্ন হয় লক্ষ্য করা (আগে যদি দৃশ্যত "আটকে" মনে হতো,
  এখন worst-case ~১০ সেকেন্ডের মধ্যেই এগিয়ে যাওয়ার কথা)। এরপর ১.৩-এর balance-mismatch checklist,
  তারপর ৩.১a/৩.১c।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ব্যবহারকারীর "কিছুই ঠিক হয়নি" রিপোর্টের পর — ১.৩-এর বাইরে আরও দুটো root cause (১.৫, ১.৬) খুঁজে ফিক্স করা হয়েছে) — পরের ধাপ: real device-এ পুরো ১.২/১.৩/১.৪/১.৫/১.৬ একসাথে re-verify
- **ব্যবহারকারীর রিপোর্ট:** ১.৩-এর ফিক্স দেওয়া zip দিয়ে টেস্ট করার পরও "কিছুই ঠিক হয়নি, balance আবার
  reset হয়ে যাচ্ছে, এই একটা সমস্যা ফিক্স করার জন্য কত রকম ফিক্স try করলাম কোনোভাবেই হচ্ছে না।"
- **কেন ১.৩ যথেষ্ট ছিল না — পুরো cloud→local overwrite chain-টা লাইন-বাই-লাইন ট্রেস করে দুটো
  আলাদা অতিরিক্ত root cause পাওয়া গেছে (দুটোই ১.৩-এর মতোই আসল কারণ, কিন্তু সম্পূর্ণ আলাদা
  code path — ১.৩ ভুল ছিল না, শুধু partial ছিল):**
  1. **(১.৬, কালানুক্রমে সবচেয়ে আগে ঘটে)** `SomadhanRepository.loginWithPhonePassword()` —
     এটাই প্রতিটা লগইন/account-switch-এর **Step 1** (OTP verify-এরও আগে চলে) — নিজেই
     unconditionally cloud থেকে balance/status টেনে local ওভাররাইট করত। ১.৩-এর ফিক্স
     `completeLoginAfterOtp()`-এর Step 2-এ (`refreshUserDataFromCloud()`) থাকা, যেটা Step
     1-এরও পরে চলে — অর্থাৎ Step 1-ই আগে balance নষ্ট করে দিত, Step 2-এর গার্ড ততক্ষণে
     অকেজো (গার্ড যে `existingLocal` পড়ে সেটা ততক্ষণে ইতিমধ্যেই Step 1-এর ওভাররাইটের শিকার)।
  2. **(১.৫)** সম্পূর্ণ আলাদা ফাইলে (`SupabaseRealtimeManager.kt`) থাকা `mergeAndSaveUser()`
     — realtime `users`-table UPDATE event, listener-health fallback pull, আর app-শুরুর
     bulk pull — এই তিনটা ভিন্ন caller-এরই একমাত্র merge পয়েন্ট — একটা আগের সেশনের ইচ্ছাকৃত
     "balance flicker guard" রিমুভালের (money-flow ফিক্স ধাপ ১, তখনকার জন্য সঠিক যুক্তিতেই)
     পর থেকে balance/status ফিল্ডের জন্য কোনো protection ছাড়াই সরাসরি cloud dto নিত। তাই
     লগইনের মুহূর্তে ১.৩/১.৬ ঠিক থাকলেও, তার ঠিক পরপরই realtime subscription চালু হয়ে বা
     health-check/bulk-pull চলে আবার balance ওভাররাইট হয়ে যেত।
  Login-time-এর দুটো call-site (১.৩, ১.৬) + realtime/background-এর একটা shared call-site
  (১.৫) — এই তিনটাই এখন একসাথে ফিক্স হলো, তাই "balance reset" বাগের প্রতিটা পরিচিত entry
  point এখন কভার্ড।
- **ফিক্স (দুই ফাইলে):**
  - `SomadhanRepository.kt` → `loginWithPhonePassword()`-এ (নতুন, ১.৬): `cacheUserLocally()`
    কল করার আগে pending outbox চেক করে legacy + role-scoped mirror ফিল্ড local-ই রাখা হচ্ছে।
  - `SomadhanRepository.kt` → `refreshUserDataFromCloud()`-এ (১.৩-এর সম্প্রসারণ): আগে শুধু
    legacy ফিল্ড (balance/reputationScore/isBanned/isRestricted) প্রোটেক্ট হতো, এখন
    role-scoped mirror কলামও (balanceUser/balanceSolver/ইত্যাদি) একসাথে প্রোটেক্ট হচ্ছে —
    নাহলে ঠিক সেই mismatch তৈরি হতো যা `mergeAndSaveUser()`-এর ক্লাস-ডক নিজেই সতর্ক করেছিল।
  - `SupabaseRealtimeManager.kt` → `mergeAndSaveUser()`-এ (নতুন, ১.৫): pending outbox থাকলে
    legacy + role-scoped mirror দুটো ফিল্ড-সেটই local রাখা হচ্ছে, বাকি সব ফিল্ড dto থেকেই।
- **কোনো automated test যোগ করা হয়নি** — ১.২/১.৩/১.৪-এর মতোই real network/Room/outbox
  timing-নির্ভর, mock-based unit test-এ reproduce করা কঠিন (একই established precedent)।
- **পরের ধাপ / manual verify (ব্যবহারকারীর জন্য):** নতুন zip দিয়ে `.\gradlew.bat assembleDebug`
  → ফোনে ইনস্টল → `REALTIME_RESCOPING_STEP1_TEST_CHECKLIST.md`-এর সেকশন ১ ও ২ আবার চালানো
  (bid-accept → account-switch বারবার, কয়েকবার সামনে-পিছনে সুইচ করে দেখা)। **এবার যদি আবারও
  balance reset হয়** সেটা এই থ্রেডেই জানাবেন — এবার exact কোন মুহূর্তে দেখা যাচ্ছে সেটা
  (লগইনের সাথে সাথেই, না কিছুক্ষণ পরে/app খোলা অবস্থায় হঠাৎ) নির্দিষ্ট করে বললে বাকি থাকা কোনো
  চতুর্থ path (যদি থাকে) খুঁজে বের করা সহজ হবে।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ১.৩/১.৫/১.৬-এর পরও balance-reset persist করার রিপোর্টের পর — Supabase MCP দিয়ে সরাসরি cloud verify করে সম্পূর্ণ ভিন্ন root cause (১.৭) পাওয়া গেছে ও ফিক্স করা হয়েছে) — পরের ধাপ: real device-এ পুরো repro আবার চালানো
- **ব্যবহারকারীর রিপোর্ট:** ১.৩/১.৫/১.৬ সবগুলো ফিক্স থাকা সত্ত্বেও নির্দিষ্ট repro (post → অন্য account দিয়ে bid → account switch করে accept → logout/role-switch করে ফিরে আসা) করলে balance আবার ৭৫০-এ ফিরে যাচ্ছিল, escrow ২০০ HELD আলাদাভাবে দেখাচ্ছিল।
- **কেন ১.৩/১.৫/১.৬ যথেষ্ট ছিল না — Supabase MCP দিয়ে test user-এর cloud row সরাসরি query করে ধরা পড়েছে:**
  cloud `balance`/`balance_user` = ৭৫০.০০ (কখনো কমেইনি), আর `escrows` টেবিলে সর্বমোট **০ row** — অর্থাৎ
  dual-write "সফল হয়ে stale" ছিল না (যেটা ১.৩/১.৫/১.৬ ধরে নিয়েছিল), বরং dual-write **কখনো ঘটেইনি**।
- **আসল root cause (১.৭):** `acceptBid()`-এ Supabase RPC call + ব্যর্থ হলে `enqueueOutboxRetry()` —
  দুটোই `if (SupabaseAuthManager.currentUserId() == problem.userId)`-এর ভেতরে। শর্ত false হলে outbox-এ
  entry-ও জমা হয় না — transaction চিরকাল unsynced, `OutboxPendingIndicator`-এও অদৃশ্য থেকে যায়। শর্তটা
  false হওয়ার কারণ: `logout()`-এর fire-and-forget `SupabaseAuthManager.signOut()` (ধাপ ১৪, ইচ্ছাকৃতভাবে
  offline-logout আটকানো এড়াতে) দ্রুত পরের account-এর login-এর *পরে* ফায়ার হয়ে নতুন session মুছে দিতে
  পারত — race condition, timing-নির্ভর।
- **ফিক্স (দুই ফাইলে):**
  - `SupabaseAuthManager.kt` → নতুন `pendingSignOutJob` (Job?) ট্র্যাকার + `trackPendingSignOut(job)` +
    private `awaitPendingSignOutIfAny()` (bounded ৫ সেকেন্ড timeout)। `signInWithPhonePassword()` শুরুতেই
    এটা কল করে।
  - `SomadhanViewModel.kt` → `logout()`-এর fire-and-forget `viewModelScope.launch { ... }`-এর Job এখন
    `SupabaseAuthManager.trackPendingSignOut()`-এ রেজিস্টার করা হচ্ছে।
  - অফলাইন-logout behaviour অপরিবর্তিত (local session তাৎক্ষণিক clear, signOut() এখনও fire-and-forget) —
    শুধু *পরের* login (অন্য বা একই account) নিশ্চিত করে যে আগের signOut() সত্যিই শেষ হয়েছে।
- **কোনো automated test যোগ করা হয়নি** — ১.২/১.৪-এর মতোই real Supabase-Auth-session-timing-নির্ভর race, mock-based unit test-এ reproduce করা কঠিন।
- **পরের ধাপ / manual verify (ব্যবহারকারীর জন্য):** নতুন zip দিয়ে `.\gradlew.bat assembleDebug` → ইনস্টল
  → আগের exact repro আবার চালান (post as Shamim → bid as Shanto → switch back to Shamim → accept →
  logout/role-switch → re-enter)। balance এবার ৫৫০-ই থাকা উচিত, escrow ২০০ HELD। চাইলে Supabase-এ সরাসরি
  verify করেও দেখা যাবে (`escrows` row + user-এর `balance`/`balance_user`)। আবারও সমস্যা থাকলে ঠিক কোন
  ধাপে দেখা যাচ্ছে জানাবেন।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ব্যবহারকারীর real-device re-test-এ ১.৭-এর পরও একই symptom persist — screenshot দিয়ে proof, Supabase MCP দিয়ে cloud verify, সম্পূর্ণ ভিন্ন root cause ১.৮ পাওয়া গেছে ও ফিক্স করা হয়েছে) — পরের ধাপ: real device-এ re-verify (ব্যালেন্স-রিসেট repro আবার), এবং আলাদাভাবে ১.৭-এর accept_bid dual-write এখনো cloud-এ পৌঁছাচ্ছে কিনা সেটাও যাচাই
- **ব্যবহারকারীর রিপোর্ট:** ১.৭-এর ফিক্স সহ zip দিয়ে ঠিক আগের exact repro (Shamim post → Shanto bid →
  Shamim accept → role switch USER→SOLVER→USER) আবার চালিয়ে জানিয়েছেন "kono poriborton hoi nai" —
  balance আবার ৭৫০-এ ফিরে গেছে। একটা screenshot দিয়ে দেখিয়েছেন যে app-এর ভেতরে job-tracking screen-এ
  escrow/job তখনও live/active (Shanto solver হিসেবে, escrow ID #ESC_B33F620D, ৳২০০ locked)।
- **Cloud-verify (Supabase MCP, `mghvvpndkxnscwryfkib`):** `users` টেবিলে Shamim-এর
  `balance`/`balance_user` = ৭৫০.০০ (কখনো কমেইনি); `escrows` টেবিলে Shamim/Shanto সংক্রান্ত ০ row;
  `transactions`-এ এই accept-bid-এর জন্য কোনো `TRX_BID_DEDUCT_*` নেই — অর্থাৎ ১.৭-এর হাইপোথিসিস
  অনুযায়ী `accept_bid` RPC-এর cloud dual-write এখনও কখনো ঘটেনি (এবারও)।
- **ব্যবহারকারীর প্রশ্ন ও তার উত্তর:** "cloud-এ কিছু নেই তাহলে app-এ live escrow কীভাবে দেখাচ্ছে?" —
  Contradiction না: app-এর পুরো architecture-ই local-Room-first (balance deduct/escrow-তৈরি/
  problem-status সব Room-এ **সাথে সাথে**, UI-ও Room থেকেই render হয়), cloud dual-write সম্পূর্ণ
  best-effort ও asynchronous/independent — তাই cloud খালি থাকা সত্ত্বেও app-এর UI পুরোপুরি সঠিক ও
  জীবন্ত থাকা সম্পূর্ণ স্বাভাবিক আচরণ, বাগ না।
- **আসল root cause (১.৮, ১.৭ থেকে সম্পূর্ণ স্বতন্ত্র, ভিন্ন কোড-পথ):** `switchRoleInPlace()`
  (User↔Solver switch-এর মূল ফাংশন)-এর নিজস্ব cloud-sync ব্লক
  (`SupabaseSyncManager.switchRole(...).onSuccess { ... }`) `switch_role_get_or_create_linked_profile`
  RPC-এর ফেরত দেওয়া `balance_user`/`balance_solver` দিয়ে সরাসরি, **কোনো pending-outbox গার্ড ছাড়াই**
  local balance/status ওভাররাইট করছিল। এই RPC নিজে `accept_bid`-এর identity-guard বাগে আটকায় না
  (আলাদা RPC), তাই সবসময় সফল হয় — কিন্তু cloud-এর `balance_user` মানটাই stale (যেহেতু `accept_bid`-এর
  dual-write আগে থেকেই cloud-এ পৌঁছায়নি, ১.৭ অনুযায়ী)। ১.৩/১.৫/১.৬-এর গার্ড অন্য তিনটা call-site
  (`refreshUserDataFromCloud`, `mergeAndSaveUser`, `loginWithPhonePassword`) প্রোটেক্ট করেছিল, কিন্তু
  role-switch-এর নিজস্ব এই চতুর্থ call-site কখনো ছোঁয়া হয়নি — এটাই কেন role switch করা মাত্রই balance
  আবার stale cloud মানে ফিরে যাচ্ছিল তার সরাসরি ব্যাখ্যা।
- **ফিক্স (`SomadhanRepository.kt`, `switchRoleInPlace()`):** ১.৩/১.৫/১.৬-এর হুবহু একই
  `pendingSyncOutboxDao.getPending()` চেক এই cloud-sync ব্লকেও যোগ করা হয়েছে — pending entry থাকলে
  পুরো cloud-sync ব্লক স্কিপ হয়ে যায় (`updatedRoot` local মানই থেকে যায়, `userDao.updateUser(cloudSynced)`
  কলই হয় না, শুধু একটা `Log.w` নোটিশ), নাহলে আগের মতোই cloud থেকে sync হয়।
- এই সেশনে edit হয়েছে: `SomadhanRepository.kt` (`switchRoleInPlace()`-এর `onSuccess` ব্লকে টার্গেটেড
  if/else — নতুন কোনো ফাইল/ফাংশন সিগনেচার বদল না)। কোনো migration/SQL ছোঁয়া হয়নি। ব্রেস-ব্যালেন্স
  সানিটি-চেক করা হয়েছে (edit-এর আগে/পরে paren imbalance অপরিবর্তিত ৯, brace imbalance ০ — নতুন কোনো
  imbalance তৈরি হয়নি)।
- `BUG_INVENTORY.md`: গ্রুপ ১-এ নতুন সারি ১.৮ (🟡)।
- **কোনো automated test যোগ করা হয়নি** — ১.৩/১.৫/১.৬-এর মতোই real Room+outbox+network timing-নির্ভর,
  mock-based unit test-এ reproduce করা কঠিন (একই established precedent)।
- **পরের ধাপ / manual verify (ব্যবহারকারীর জন্য):** নতুন zip দিয়ে `.\gradlew.bat assembleDebug` →
  ফোনে ইনস্টল → ঠিক আগের exact repro আবার চালান (post → bid অন্য account দিয়ে → accept → role switch
  USER→SOLVER→USER)। **এবার লক্ষ্য রাখতে হবে দুটো আলাদা জিনিস:** (ক) role switch করার পরও balance
  ৫৫০-ই থাকছে কিনা (১.৮-এর মূল target), (খ) এর independent-এ, Supabase-এ সরাসরি `escrows`/
  `transactions` টেবিল চেক করে `accept_bid`-এর dual-write আদৌ এবার cloud-এ পৌঁছাচ্ছে কিনা (১.৭ এখনো
  সত্যিই কার্যকর কিনা, যেটা এখনো নিশ্চিত হয়নি)। দুটো bug independent, তাই (ক) ঠিক হয়ে গেলেও (খ) আলাদাভাবে
  এখনো open থাকতে পারে — escrow/transaction cloud-এ না পৌঁছালে ভবিষ্যতে অন্য কোনো symptom (যেমন
  admin panel-এ এই লেনদেন না দেখা, অন্য ডিভাইস থেকে sync না হওয়া) হতে পারে।

## 🔁 HANDOFF (২০২৬-০৯-২৩, ব্যবহারকারী নিজেই ধরিয়ে দিয়েছেন যে সমস্যাটা শুধু role-switch-এ সীমাবদ্ধ না — logout/login-এও একই রিসেট হয়, একত্রীকরণকারী root cause ১.৯ পাওয়া গেছে ও ফিক্স করা হয়েছে) — পরের ধাপ: real device-এ role-switch এবং logout/login দুটো পথই আবার verify
- **ব্যবহারকারীর মন্তব্য:** "eta sudhu role switch er bisoy chilo na, logout kore abar login korlew ei
  somossa hoto" — অর্থাৎ ১.৮ (role-switch-নির্দিষ্ট ফিক্স) যথেষ্ট না, কারণ একই balance-reset
  logout করে আবার login করলেও ঘটে (যেটা ১.৩/১.৫/১.৬-এর কভার করার কথা ছিল)।
- **এটাই আসল, একত্রীকরণকারী insight — কোড রিভিউ করে নিশ্চিত হওয়া গেছে:** ১.৩/১.৫/১.৬/১.৮ — চারটা
  গার্ডই `pendingSyncOutboxDao.getPending()`-এর উপর নির্ভরশীল (pending entry থাকলেই শুধু local
  balance protect হয়)। কিন্তু `acceptBid()`-এর `if (currentUserId() == problem.userId) { ...RPC...
  }` ব্লকের **কোনো `else` শাখাই ছিল না** — শর্ত false হলে (১.৭-এর session-race অনুযায়ী) RPC কলই হতো
  না, `enqueueOutboxRetry()`-ও কল হতো না, এমনকি কোনো log-ও না। ফলে identity-mismatch হলে
  `pending_sync_outbox`-এ কখনোই entry তৈরি হতো না — আর তাই ১.৩/১.৫/১.৬/১.৮ প্রতিটাই "pending কিছু
  নেই" দেখে stale cloud balance দিয়ে local overwrite হতে দিত, path যাই হোক (login বা role-switch বা
  realtime merge)। এটাই কেন উভয় repro (role-switch এবং logout/login) একই symptom দেখাচ্ছিল তার
  সম্পূর্ণ ব্যাখ্যা — চারটা path-ই একই একক root cause-এর শিকার ছিল।
- **ফিক্স (`SomadhanRepository.kt`, `acceptBid()`):** identity-check `if`-এর একটা `else` শাখা যোগ
  করা হয়েছে — `onFailure`-এর মতোই shape-এর একটা `enqueueOutboxRetry("accept_bid", ...)` কল করে
  (params: problemId/bidId/escrowId + ঐচ্ছিক gatewayTrxId/gateway/gatewayAmount, ঠিক
  `OutboxRpcDispatcher`-এর accept_bid branch-এর সাথে মিলিয়ে) + একটা `Log.w`। এখন mismatch হলেও একটা
  outbox entry তৈরি হবে, তাই ১.৩/১.৫/১.৬/১.৮-এর সব গার্ডই সঠিকভাবে কাজ করবে, path যাই হোক না কেন।
- এই সেশনে edit হয়েছে: `SomadhanRepository.kt` (`acceptBid()`-এর if-ব্লকে একটা নতুন `else` শাখা যোগ
  — বিদ্যমান কোনো লাইন মুছে/বদলানো হয়নি)। কোনো migration/SQL ছোঁয়া হয়নি। ব্রেস-ব্যালেন্স সানিটি-চেক
  করা হয়েছে (brace ১৭৮১/১৭৮১ ব্যালেন্সড, paren imbalance অপরিবর্তিত ৯ — কোনো নতুন imbalance তৈরি হয়নি)।
- `BUG_INVENTORY.md`: গ্রুপ ১-এ নতুন সারি ১.৯ (🟡, "সবচেয়ে গুরুত্বপূর্ণ, একত্রীকরণকারী root cause"
  হিসেবে চিহ্নিত)।
- **কোনো automated test যোগ করা হয়নি** — ১.৭-এর মতোই auth-session-timing-নির্ভর real race, mock-based
  unit test-এ reproduce করা কঠিন।
- **পরের ধাপ / manual verify (ব্যবহারকারীর জন্য):** নতুন zip দিয়ে build+install করে **দুটো আলাদা
  repro-ই** আবার চালান: (ক) role-switch (USER→SOLVER→USER) এবং (খ) logout করে আবার সেই account-এ
  login — দুটোতেই balance এখন ৫৫০-ই থাকা উচিত। এবার এটাও লক্ষ্য করবেন: wallet স্ক্রিনে "কিছু লেনদেন
  cloud-এ sync হতে বাকি আছে" warning-টা দেখা উচিত (যেহেতু outbox entry এখন সবসময় তৈরি হচ্ছে, RPC
  আসলে cloud-এ পৌঁছাক বা না পৌঁছাক) — এই warning-এর উপস্থিতি নিজেই এই ফিক্স কাজ করছে তার একটা ইঙ্গিত।
  একইসাথে Supabase-এ সরাসরি চেক করেও দেখা যেতে পারে `escrows`/`transactions` এবার পৌঁছাচ্ছে কিনা।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ব্যবহারকারীর real-device রিপোর্ট — OTP verify-এর পর popup হারিয়ে যাচ্ছে, কোনো loading না দেখিয়ে অনেকক্ষণ login/role-select popup আসছে না; admin login-এও same; profile role-switch-এও অনেক সময় লাগছে শুধু loading দেখিয়ে যদিও background-এ role switch হয়ে গেছে — নতুন root cause ১.১০ পাওয়া গেছে ও ফিক্স করা হয়েছে) — পরের ধাপ: real device-এ OTP login (উভয়-role/single-role), admin login, আর profile role-switch — চারটাই আবার চালিয়ে দেখা লাগবে এখন দ্রুত + visible loading হচ্ছে কিনা
- **ব্যবহারকারীর রিপোর্ট (verbatim ভাবানুবাদ):** "Windows-এ একটা bug দেখেছি, login করার সময় OTP popup
  open হচ্ছে, OTP দিয়ে verify করলে popup হারিয়ে যাচ্ছে, তারপর যে instant login হয়ে যাবে বা role select
  করার popup আসবে সেটা হচ্ছে না — অনেক সময় লাগছে। এটা বিভ্রান্তিকর মনে হচ্ছে, app login হচ্ছে না, OTP
  দিলে কোনো loading-ও দেখাচ্ছে না, আবার role select করার popup আসতে late হচ্ছে — মনে হচ্ছে app login
  হচ্ছে না। Admin-এর ক্ষেত্রেও same। আবার profile-এর মধ্যে login অবস্থায় role switch করলেও অনেক বেশি
  সময় লাগছে, শুধু loading হচ্ছে, এদিকে background-এ দেখাচ্ছে role switch হয়ে গেছে।"
- **কোড রিভিউ করে root cause পাওয়া গেছে (কোনো automated test নেই, UI timing-নির্ভর):** ১.১-এর ফিক্স
  করা ৪টা caller-ই (`completeLoginAfterOtp`/`loginAsAdmin`/`switchRoleToSolver`/`switchRoleToUser`,
  `SomadhanViewModel.kt`) `runCatching { SupabaseRealtimeManager.startRealtimeListeners() }` কে
  **await করে** — ১.৪-এর পর parallel হওয়ায় worst-case ~১০০ সেকেন্ড থেকে ~১০ সেকেন্ডে নামলেও,
  `onSuccess()/onReady()` তবুও সেই পুরো সময়টা আটকে থাকে, যদিও local state ততক্ষণে already committed
  (realtime resubscribe শুধু live-update-এর জন্য, সঠিকতার জন্য না)।
- **`LoginScreen.kt`-এ আলাদা, দ্বিতীয় সমস্যা:** OTP confirm button-এর `verifyOtp()` onSuccess-এই আগে
  `isVerifyingLoginOtp = false` আর `showLoginOtpDialog = false` করে dialog বন্ধ করে দিত, **তারপর**
  `completeLoginAfterOtp()` কল হতো — অর্থাৎ dialog বন্ধ হওয়া আর role-select popup/login-success আসার
  মাঝের পুরো gap-এ কোনো loading UI-ই ছিল না। এটাই "popup হারিয়ে যাচ্ছে, কোনো loading দেখাচ্ছে না"-র
  সরাসরি কারণ। (Admin login button আর profile role-switch dialog-এর নিজস্ব `isLoading`/`isSwitchingRole`
  spinner আগে থেকেই সঠিক callback পর্যন্ত আটকে থাকত, তাই ওদের ক্ষেত্রে শুধু delay-টাই সমস্যা ছিল, missing
  loading UI না।)
- **ফিক্স (BUG_INVENTORY.md-এ ১.১০, উভয়ই একসাথে):**
  1. `SomadhanViewModel.kt`-এর ৪টা call-site-এই `startRealtimeListeners()`-কে
     `viewModelScope.launch { runCatching { ... }.onFailure { ... } }`-এ মুড়ে fire-and-forget করা
     হয়েছে — `onSuccess()/onReady()` আর এর জন্য অপেক্ষা করে না।
  2. `LoginScreen.kt`-এ OTP confirm button-এর `verifyOtp()`-এর onSuccess থেকে
     `isVerifyingLoginOtp = false`/`showLoginOtpDialog = false` সরিয়ে `completeLoginAfterOtp()`-এর
     onSuccess-এ নেওয়া হয়েছে — dialog এখন "যাচাই হচ্ছে..." spinner-সহ পুরো সময় খোলা থাকবে, ফাঁকা gap
     থাকবে না।
- **রেঞ্জ:** এই ফিক্স money-critical balance/escrow logic ছোঁয়নি (শুধু UI-timing/callback-sequencing),
  তাই ১.৩/১.৫/১.৬/১.৭/১.৮/১.৯-এর pending-outbox গার্ড-ভিত্তিক ফিক্সগুলো অক্ষত/অপরিবর্তিত আছে।
- real-device manual verify বাকি (এখনো karo Windows real-run/manual confirm আসেনি এই বাগের জন্য)।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ব্যবহারকারীর real-device confirm — গ্রুপ ১ সম্পূর্ণ verified) — পরের ধাপ: গ্রুপ ৩ (Step 6, ৩.১a/৩.১c) real-device verify বাকি, তারপর Step 7 (backlog decisions)
- ব্যবহারকারী কনফার্ম করেছেন: "Group 1 real verified sob kichu kaj korche normally" — অর্থাৎ ১.২ থেকে
  ১.১০ পর্যন্ত সবগুলো (OTP login উভয়-role/single-role, admin login, profile role-switch, bid-accept
  → role-switch/logout-login balance-persist, escrow release/withdraw balance-persist) real device-এ
  স্বাভাবিকভাবে কাজ করছে।
- `BUG_INVENTORY.md`-এ ১.২-১.১০ সবগুলো 🟡 → ✅ করা হলো (নোট আপডেট করে confirm-এর তারিখ/উক্তি যোগ করা
  হয়েছে)। গ্রুপ ১ (money-critical realtime/balance) এখন **সম্পূর্ণরূপে verified** — ১.১ থেকে ১.১০
  সব ✅।
- Master prompt-এর Step 1 checkbox আগে থেকেই `[x]` ছিল (stale ছিল, কিন্তু এখন বাস্তবেই মিলে গেছে) —
  আলাদা করে কিছু বদলানোর দরকার নেই।
- বাকি: গ্রুপ ৩ (offline-gating, Step 6) — ৩.১a ও ৩.১c এখনও 🟡, real-device verify বাকি (৩.১b আগে থেকেই
  ✅)। এরপর Step 7-এর ৭টা backlog আইটেম নিয়ে সিদ্ধান্ত, তারপর Step 8 (final regression pass)।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ব্যবহারকারীর real-device confirm — গ্রুপ ৩ সম্পূর্ণ verified) — পরের ধাপ: Step 7 (গ্রুপ ৭-এর ৭টা backlog আইটেম নিয়ে সিদ্ধান্ত), তারপর Step 8 (final regression pass)
- ব্যবহারকারী প্রথমে "Group 3 kaj korche na" বলেছিলেন, পরে স্পষ্ট করেছেন: "Amar test vul chilo ekdom
  parfect kaj korche" — অর্থাৎ ৩.১a (dual-write ব্যর্থতা surface করা) ও ৩.১c (degraded-session persistent
  বার) দুটোই real device-এ ঠিকমতো কাজ করছে।
- `BUG_INVENTORY.md`-এ ৩.১, ৩.১a, ৩.১c সবগুলো 🟡 → ✅, গ্রুপ ৩-এর heading 🟠 → ✅ করা হলো। গ্রুপ ৩ এখন
  **সম্পূর্ণরূপে verified** (৩.১a/৩.১b/৩.১c সব ✅)।
- `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এ Step 6 checkbox `[ ]` → `[x]` করা হলো।
- **এখন গ্রুপ ১, ২, ৩, ৪, ৫, ৬ — সবগুলোই সম্পূর্ণ+verified।** বাকি শুধু: গ্রুপ ৭-এর ৭টা backlog
  আইটেম (Step 7, সিদ্ধান্ত দরকার — নতুন কোড ফিক্স না) আর তারপর Step 8 (সব একসাথে ধরে একটা final
  regression/summary pass)।

## 🔁 HANDOFF (২০২৬-০৯-২৪, Step 7 শুরু — ব্যবহারকারী ৭.১ এখনই ফিক্স করতে বলেছেন, ৭.২-৭.৭ pending রাখা হলো) — পরের ধাপ: ৭.২-এর প্রশ্নের উত্তর (ইচ্ছাকৃত ডিজাইন নাকি গ্যাপ?) নিয়ে সিদ্ধান্ত, তারপর ৭.৩-৭.৭ একে একে
- ব্যবহারকারী গ্রুপ ৭-এর ৭টা backlog আইটেমের সংক্ষিপ্ত বিবরণ দেখে বলেছেন: "৭.১ এখনই ফিক্স করো, বাকিগুলো
  progress note-এ handoff করে রাখো (এই সেশনে touch না করে)।"
- **৭.১ ফিক্স সম্পূর্ণ:**
  - নতুন migration `supabase/migrations/zz_20260924120000_step7_1_submit_rating_idempotency_guard.sql`
    — `submit_rating(text,integer,text,text)`-এ insert-এর আগে (problem_id, rater_role) দিয়ে existing
    rating চেক, পাওয়া গেলে নতুন row না বানিয়ে existing rating_id + `already_rated: true` ফেরত দেয়।
  - `supabase/tests/11_notifications_ratings_part2.sql`-এর duplicate-rating টেস্ট ব্লক (আগে
    "DOCUMENTED CURRENT BEHAVIOUR — duplicate গার্ড নেই" নামে পুরনো buggy আচরণ ডকুমেন্ট করত) নতুন
    idempotent আচরণ assert করার জন্য rewrite করা হয়েছে — ২টা নতুন assertion যোগ হওয়ায় `plan(160)` →
    `plan(162)` (actual assertion-count গুনে যাচাই করা হয়েছে: throws_ok ৫৭ + ok ৪০ + is ৩৪ + results_eq
    ৩১ = ১৬২, মেলে)।
  - Kotlin caller (`SupabaseSyncManager.submitRating()`) touch করার দরকার হয়নি — raw `JsonElement`
    রিটার্ন করে, নতুন `already_rated` ফিল্ড existing contract ভাঙে না।
  - **বাকি:** এই migration real Supabase project-এ apply করে (`supabase db push` বা dashboard-এ SQL
    editor দিয়ে) pgTAP test suite (part2) আবার চালিয়ে ১৬২/১৬২ pass কনফার্ম করা — এই সেশনে sandbox-এ
    কোনো Supabase/psql access নেই, তাই এটা করা যায়নি।
- **৭.২-৭.৭ ইচ্ছাকৃতভাবে touch করা হয়নি এই সেশনে** — `BUG_INVENTORY.md`-এ যেমন ছিল (📝) তেমনই আছে।
  বিশেষভাবে **৭.২**-এর জন্য একটা সিদ্ধান্ত লাগবে আগে (fix করার আগে): `admin_force_cancel_instant_job`
  escrow refund/release নিজে না করাটা কি ইচ্ছাকৃত ডিজাইন (admin আলাদা flow-এ handle করে) নাকি সত্যিই
  একটা গ্যাপ? — এটা ব্যবহারকারীর কাছে এখনো জিজ্ঞাসা করা আছে, উত্তর আসেনি।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.১ migration সরাসরি live Supabase-এ apply করা হলো Supabase MCP দিয়ে) — পরের ধাপ: pgTAP suite (11_notifications_ratings_part2.sql) app-এর নিজস্ব CI/local run-এ চালিয়ে ১৬২/১৬২ pass চূড়ান্ত কনফার্ম, তারপর ৭.২-এর প্রশ্নের উত্তর
- ব্যবহারকারী বলেছেন "supabase mcp আছে, তুমিই করে দাও" — তাই migration আর manual apply-এর জন্য অপেক্ষা না
  করিয়ে সরাসরি Supabase MCP tool (`apply_migration`) দিয়ে live project-এ (নাম `somadhan`, project_id
  `mghvvpndkxnscwryfkib`) apply করা হলো।
- Apply করার আগে `execute_sql`-এ `pg_get_functiondef()` দিয়ে cloud-এর তখনকার `submit_rating()` definition
  পড়ে নিশ্চিত করা হয়েছে সেটা প্রি-ফিক্স ভার্সনের সাথে হুবহু মিলছে (যাতে ভুল state-এর উপর migration না বসে)।
- Apply-এর পর আবার `pg_get_functiondef()` দিয়ে re-read করে কনফার্ম করা হয়েছে cloud-এর function
  definition এখন নতুন idempotency-guard-সহ কোডের সাথে **হুবহু মিলছে**।
- `BUG_INVENTORY.md`-এ ৭.১ এখন পুরোপুরি ✅ (code+test+live-migration সব সম্পূর্ণ)।
- **এখনো বাকি:** `supabase/tests/11_notifications_ratings_part2.sql` pgTAP suite-টা প্রজেক্টের
  নিজস্ব CI pipeline/local pg_prove run-এ চালিয়ে সত্যিই ১৬২/১৬২ pass হচ্ছে কিনা confirm করা (এই
  MCP দিয়ে শুধু raw SQL/migration apply করা গেছে, পুরো pgTAP test-runner চালানো এখান থেকে করা হয়নি)।
- ৭.২-৭.৭ এখনো touch করা হয়নি — ৭.২-এর প্রশ্ন (`admin_force_cancel_instant_job` escrow handle না করাটা
  ইচ্ছাকৃত ডিজাইন নাকি গ্যাপ?) এখনো খোলা।

## 🔁 HANDOFF (২০২৬-০৯-২৪, নতুন session — Step 7 চলমান; ৭.২-এর তদন্ত সম্পন্ন, ব্যবহারকারীর সিদ্ধান্তের অপেক্ষায়; কোনো কোড/migration বদলানো হয়নি) — পরের ধাপ: ৭.২-এর সিদ্ধান্ত (ক/খ/গ), তারপর ৭.৩-৭.৭ একে একে; সবশেষে Step 8
- **Bootstrap:** master prompt, `BUG_INVENTORY.md`, শেষ HANDOFF পড়া হয়েছে। এই মেসেজে কোনো `WINDOWS RESULT:` ছিল না।
  checkbox (Step 7 `[ ]`, Step 8 `[ ]`) আর HANDOFF মিলছে। zip: ৩৮৬ ফাইল (dotfile `.env`, `.env.example`,
  `.gitignore` আছে), শেষ entry-র সাথে সঙ্গতিপূর্ণ (নতুন migration `zz_20260924120000_...` সহ)।
- **ছোট গরমিল (নোট মাত্র, ঠিক করা হয়নি):** master prompt-এর Step 7 লাইনে "৫টা আইটেম" লেখা, বাস্তবে
  `BUG_INVENTORY.md` গ্রুপ ৭-এ এখন ৭টা (৭.১–৭.৭); আর ইনভেন্টরির নিচের "সারসংক্ষেপ" টেবিলে গ্রুপ ৩ এখনো 🟠
  ও গ্রুপ ৭ "৫" দেখাচ্ছে — এগুলো Step 8-এ সারসংক্ষেপ আপডেটের সময় ঠিক হবে।
- **৭.২ তদন্ত (`admin_force_cancel_instant_job`), কোড পড়ে:**
  - RPC (`recovered_admin_moderation.sql:551`) শুধু `problems`/`bids` state + notification বদলায়, `escrows`/
    `transactions`/balance ছোঁয় না — `supabase/tests/10_admin_moderation_balance_part2.sql`-ও এটাই
    documented behaviour হিসেবে assert করে।
  - একমাত্র caller `SomadhanRepository.adminForceCancelInstantJob()` (~১১২৭৪) RPC-কলের আগেই লোকালি
    `refundEscrowOnce()` দিয়ে refund করে (নিজস্ব dual-write/outbox সহ) এবং কমেন্টে স্পষ্ট লেখা যে refund
    ইচ্ছাকৃতভাবে RPC-তে ডুপ্লিকেট করা হয়নি। Step 20.2c-ও এটাকে "retry-gap না, RPC-ডিজাইনের প্রশ্ন" বলেছিল।
  - অনুমান: ইচ্ছাকৃত layering; কিন্তু সিদ্ধান্ত ব্যবহারকারীর (master prompt Step 7)। `BUG_INVENTORY.md` ৭.২ সারিতে
    এই তদন্ত-নোট যোগ হয়েছে, Status 📝-ই আছে।
- **এখনো বাকি:** (১) ৭.২ সিদ্ধান্ত; (২) ৭.৩–৭.৭; (৩) `11_notifications_ratings_part2.sql` pgTAP ১৬২/১৬২ —
  এই sandbox-এ psql/pg_prove/network নেই, তাই আবারও চালানো যায়নি; (৪) Step 8 (Windows-এ পুরো
  `.\gradlew.bat test --stacktrace`)।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.২ — ব্যবহারকারী (ক) বেছেছেন; migration ফাইল + pgTAP আপডেট করা হয়েছে, live apply এখনো না) — পরের ধাপ: ব্যবহারকারীর কনফার্মেশনে migration live Supabase-এ apply + `pg_get_functiondef()` দিয়ে যাচাই, pgTAP (10_…part2 = ১৯৮, 11_…part2 = ১৬২) real-run; তারপর ৭.৩-৭.৭
- ব্যবহারকারীর উত্তর: "(ক) এখনই ফিক্স করতে চাই (RPC-তে refund যোগ)" — master prompt rule #১১ অনুযায়ী এটাকে এই একটা
  migration ফাইল বসানোর অনুমতি ধরা হয়েছে। **Live DB-তে apply-এর অনুমতি আলাদাভাবে চাওয়া হয়েছে, এখনো পাওয়া যায়নি।**
- **নতুন migration:** `supabase/migrations/zz_20260924130000_step7_2_force_cancel_instant_job_refunds_held_escrow.sql`
  — `admin_force_cancel_instant_job` বডি `recovered_admin_moderation.sql:551`-এর সাথে হুবহু এক (diff করে যাচাই),
  শুধু `v_escrow_id text` declare + `v_had_solver` হলে HELD escrow-গুলোর উপর `perform public.refund_escrow_once(id,
  'ADMIN_FORCE_ACTION', 100)` লুপ। grants CREATE OR REPLACE-এ অপরিবর্তিত।
- **কেন নিরাপদ:** Kotlin (`adminForceCancelInstantJob()`) আগে থেকেই একই `refundEscrowOnce()` (refundType =
  "ADMIN_FORCE_ACTION") চালায় ও তার cloud dual-write আছে; `refund_escrow_once` escrow REFUNDED/RELEASED বা `TRX_REFUND_<id>`
  থাকলে no-op — তাই দুই দিকের যেকোনো ক্রমে double-refund নেই, আর app-এর cloud-write ব্যর্থ হলেও এই RPC escrow ছাড়িয়ে দেয়।
- **আচরণ-পরিবর্তন (জানানো হয়েছে):** refund ব্যর্থ হলে (যেমন owner-এর USER_ROLE_INACTIVE) cancel পুরো rollback — আগে cancel
  সফল হয়ে escrow HELD থাকত। ইচ্ছাকৃত।
- **pgTAP:** `supabase/tests/10_admin_moderation_balance_part2.sql` — E_FC1-এর পুরনো "HELD-ই থাকে" assertion → 'REFUNDED';
  নতুন: E_FC1-এর ১টা REFUND transaction (৫০০, ADMIN_FORCE_ACTION); fixture-এ আগে-REFUNDED `E_FC2` যোগ + FC2 REBROADCAST-এর পর
  তার কোনো নতুন transaction নেই। হেডার ঘ) কমেন্ট আপডেট। plan ১৯৬→১৯৮ (script দিয়ে গুনে মেলানো, baseline ১৯৬ও মেলে)।
  **sandbox-এ psql/pg_prove নেই — real-run হয়নি, শুধু static যাচাই।**
- Kotlin কোড অপরিবর্তিত। `BUG_INVENTORY.md` ৭.২ → 🟡 (FIXED, NOT VERIFIED)।
- **বাকি:** live apply (অনুমতি সাপেক্ষে) → ✅; ৭.৩–৭.৭; ৭.১-এর pgTAP ১৬২ real-run; Step 8।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.২ সংশোধন — ব্যবহারকারীর সিদ্ধান্ত: USER role নিষ্ক্রিয় থাকলেও cancel হবে, refund pending থাকবে, role active হলে স্বয়ংক্রিয় refund; live apply এখনো না) — পরের ধাপ: ব্যবহারকারীর কনফার্মেশনে migration live-এ apply + `pg_get_functiondef()`/`pg_trigger` যাচাই + pgTAP real-run (10_…part2 = ২০১, 11_…part2 = ১৬২); তারপর ৭.৩-৭.৯
- ব্যবহারকারীর প্রশ্ন ছিল: USER role নিষ্ক্রিয় হলে escrow কি কখনো refund হবে না? উত্তর: `refund_escrow_once` আগে থেকেই
  USER_ROLE_INACTIVE-এ refund করে না (refund `balance_user`-এ যায়); escrow HELD থাকে, role active হলে refund সম্ভব — কিন্তু
  আমার প্রথম ভার্সনে cancel-ই fail করত। ব্যবহারকারী বলেছেন: cancel হোক, refund pending থাকুক, role active হলে balance-ও refund হোক।
- **একই migration ফাইল (এখনো live-এ apply হয়নি) সংশোধিত:** `zz_20260924130000_step7_2_force_cancel_instant_job_refunds_held_escrow.sql`
  (১) RPC: `refund_escrow_once` কলের চারপাশে exception block — শুধু `sqlerrm = 'USER_ROLE_INACTIVE'` হলে `v_refund_pending := true`
  ও cancel চলে; অন্য error → `raise` (পুরো rollback)। ফেরত: `{result:'OK', refund_pending:bool}`।
  (২) নতুন `refund_pending_escrows_on_user_role_activation()` + trigger `trg_refund_pending_escrows_on_user_role_activation`
  (AFTER UPDATE OF has_user_role, WHEN OLD≠true AND NEW=true) — HELD escrow + problem CANCELLED/(OPEN & accepted_solver null)
  (reconcile Case 2-এর একই শর্ত) → `refund_escrow_once(...,'ROLE_REACTIVATION_REFUND',100)`; প্রতিটা per-escrow exception-safe
  (WARNING), তাই role-activation কখনো আটকায় না। `refund_type` কোথাও display-logic-এ ব্যবহৃত নয় (grep)।
  সীমা: refund_escrow_once auth.uid() চায় (owner/solver/admin) — activation admin/user নিজে করলে কাজ করবে; auth.uid() null
  context-এ (যেমন raw service_role SQL) refund হবে না, HELD-ই থাকবে — পরে reconcile ধরবে।
- **pgTAP:** `10_admin_moderation_balance_part2.sql` plan ১৯৮→২০১: নতুন FC6/E_FC6 fixture (has_user_role=false owner) — cancel
  OK+refund_pending=true, problem CANCELLED ও escrow HELD/০ transaction; তারপর `UPDATE users SET has_user_role=true` → escrow
  REFUNDED + ১টা `ROLE_REACTIVATION_REFUND` transaction (script দিয়ে গুনে ২০১ মেলানো)। real Postgres-এ চালানো হয়নি।
  অনুমান: `test.login_as` শুধু `request.jwt.claim.sub` GUC বসায়, তাই RESET ROLE-এর পরেও auth.uid()=admin থাকে (trigger-test এর উপর নির্ভর)।
- Backlog-এ নতুন: ৭.৮ (`admin_reconcile_escrow_states` একটা INACTIVE-role escrow-তে পুরো run abort করে), ৭.৯ (`refund_pending` Kotlin/admin UI দেখায় না)।
- Kotlin অপরিবর্তিত। `BUG_INVENTORY.md` ৭.২ 🟡।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.২ migration live Supabase-এ apply করা হলো Supabase MCP দিয়ে) — পরের ধাপ: pgTAP real-run (10_…part2 = ২০১, 11_…part2 = ১৬২) বা নিষ্ক্রিয়-role scenario ম্যানুয়াল verify → ৭.২ ✅; তারপর ৭.৩-৭.৯ একে একে; সবশেষে Step 8
- ব্যবহারকারী "Ha" বলে live apply-এর অনুমতি দিয়েছেন।
- Apply-এর আগে cloud-এ পড়ে দেখা: `admin_force_cancel_instant_job` পুরনো বডি (refund_escrow_once নেই); users-এ trigger ছিল `broadcast_users_changes`,
  `trg_set_display_uid`; HELD-orphan (নিষ্ক্রিয় role + CANCELLED/unassigned-OPEN) escrow ০টা — অর্থাৎ apply-এ কোনো বিদ্যমান টাকা নড়েনি।
- `apply_migration` (name: `step7_2_force_cancel_instant_job_refunds_held_escrow`) → success। পরে যাচাই: নতুন RPC-বডি আছে,
  trigger-ফাংশন আছে (SECURITY DEFINER), trigger `trg_refund_pending_escrows_on_user_role_activation` enabled + সঠিক WHEN শর্ত;
  RPC-র grants অপরিবর্তিত (anon/authenticated/service_role)।
- Repo-র migration ফাইলের "এখনো apply হয়নি" হেডার-লাইন "applied" করা হয়েছে (SQL বডি অপরিবর্তিত)। `BUG_INVENTORY.md` ৭.২ 🟡-ই (বাকি শুধু behavioural verify)।
- **সতর্কতা:** নতুন trigger production `users` টেবিলে বসেছে — কোনো user-এর `has_user_role` false→true হলে তার pending escrow refund হবে (এটাই উদ্দেশ্য)।
  কিছু অপ্রত্যাশিত দেখলে rollback: `DROP TRIGGER trg_refund_pending_escrows_on_user_role_activation ON public.users;`

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৩ — ব্যবহারকারী (গ) বেছেছেন: closed, intentional) — পরের ধাপ: ৭.৪ (`ExampleRobolectricTest` flaky) সিদ্ধান্ত, তারপর ৭.৫-৭.৯; ৭.২-এর behavioural verify (pgTAP/ম্যানুয়াল) এখনো খোলা; সবশেষে Step 8
- ৭.৩ (`syncAllLocalToSupabase` retry-ধরন) কোড পড়ে যাচাই (`SomadhanRepository.kt` ~১১৬০০): own-row best-effort push, idempotent, money-free,
  outbox ব্যবহার করে না — ইচ্ছাকৃত ডিজাইন হিসেবে `BUG_INVENTORY.md`-এ closed। কোনো কোড/migration বদলানো হয়নি।
- ৭.২ 🟡 (live-applied, behavioural verify বাকি), ৭.৪-৭.৯ 📝 অপরিবর্তিত।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৪ — ব্যবহারকারী (গ): closed, intentional) — পরের ধাপ: ৭.৫ (`bids_select`/`problems_select` RLS test-coverage-gap) সিদ্ধান্ত, তারপর ৭.৬-৭.৯; ৭.২-এর behavioural verify খোলা; সবশেষে Step 8
- ৭.৪ (`ExampleRobolectricTest` flaky) closed। Step 8-এ full `.\gradlew.bat test`-এ শুধু এই ক্লাসের ৩টা intermittent fail উপেক্ষা করে বাকি সব green চাওয়া হবে। কোনো কোড বদলানো হয়নি।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৫ — ব্যবহারকারী (খ): পরে, খোলা) — পরের ধাপ: ৭.৬ (`joinProblemMessagesBroadcastChannel`/`joinProblemBidsBroadcastChannel` ইত্যাদির `.subscribe()`-এ timeout নেই) সিদ্ধান্ত, তারপর ৭.৭-৭.৯; ৭.২-এর behavioural verify খোলা; সবশেষে Step 8
- ৭.৫ (RLS `bids_select`/`problems_select` test-coverage-gap) deferred — 📝 হিসেবেই আছে, কোনো কোড/test বদলানো হয়নি।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৬ — ব্যবহারকারী (ক): ফিক্স করা হলো) — পরের ধাপ: real device-এ ৭.৬ verify (Windows build + chat/problem-detail স্ক্রিন) ও ৭.২ behavioural verify; তারপর ৭.৭ (`pendingCloudSync` dead-reference audit) সিদ্ধান্ত, ৭.৮, ৭.৯; সবশেষে Step 8
- **বদল (শুধু `SupabaseRealtimeManager.kt`, ৩ জায়গা):** `joinProblemMessagesBroadcastChannel()`, `joinProblemBidsBroadcastChannel()`, `joinTypingChannel()`-এর `ch.subscribe()` →
  `ch.subscribeWithTimeout("<label>")`। হেল্পার আগে থেকেই আছে (১০ সেকেন্ড, ব্যর্থ হলে শুধু Log.w, non-fatal)। প্রতিটার উপরে `[Somadhan Bug-Fix Step 7.6]` কমেন্ট।
- **যাচাই:** byte-level diff শুধু এই ৩টা জায়গা; ফাইল UTF-8 বৈধ; `{`/`}` গণনা আগের সমান (৩৭৬/৩৭৬); test-ফোল্ডারে কোনো test এই `.subscribe()` লাইনের উপর নির্ভর করে না (grep)।
  sandbox-এ Gradle/network নেই — compile/test চালানো হয়নি (কল-সাইট আগের `subscribeWithTimeout` ব্যবহারের হুবহু প্যাটার্ন; `RealtimeChannel` এক্সটেনশন, `suspend` context-এই আছে)।
- **আচরণ-নোট:** timeout হলে channel `messagesBroadcastChannels`/`bidsBroadcastChannels`/`typingChannels`-এ cached থাকে (live push ছাড়া); স্ক্রিন বেরোলে `leave…()` সেটা সরায়, তাই পরের open-এ retry হয়।
- automated test যোগ করা হয়নি (network-timing-নির্ভর; ১.২/১.৪-এর precedent)। `BUG_INVENTORY.md` ৭.৬ 🟡।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৭ — ব্যবহারকারী (ক) বেছেছেন: এখনই অডিট+ফিক্স) — পরের ধাপ: ৭.৬ real-device verify ও ৭.২ behavioural verify এখনো খোলা (আগের HANDOFF থেকে); তারপর ৭.৮ (`admin_reconcile_escrow_states` exception-handling, money-critical) সিদ্ধান্ত, তারপর ৭.৯; সবশেষে Step 8
- ৭.৭ (`pendingCloudSync` dead-reference audit) — `SomadhanRepository.kt`-এ `pendingCloudSync = true` সেট করা সব ১৪টা call-site
  (`payoutEscrowToSolver`, `acceptBid`, `adminResolveDisputeLocked`, `refundEscrowOnceLocked`, `reconcileUserBalances`,
  `cleanupDuplicateRefunds`, `repairMissingRefunds`, `confirmReleaseAndComplete`, `requestWithdrawal`, `updateWithdrawalStatus`,
  `depositMoneyViaGateway`, `adminAdjustBalance`, `respondToAdditionalCharge`, `userConfirmExtraAmount`) grep+read করে পূর্ণ অডিট।
- **সিদ্ধান্ত: কোনো functional বাগ নেই।** প্রতিটা call-site-এ আসল cloud-sync একটা Supabase RPC dual-write (প্রতিটাতে যাচাই করা হয়েছে
  `enqueueOutboxRetry(`/`.onFailure` presence দিয়ে) — ব্যর্থ হলে outbox queue (`OutboxRpcDispatcher`/`OutboxSyncWorker`) রিয়েল retry দেয়।
  শুধু কমেন্টগুলো Firebase-যুগের এখন-অস্তিত্বহীন `syncPendingCloudRefunds()`/`FirebaseSyncManager`-কে "আসল retry মেকানিজম" বলে ভুল
  বর্ণনা দিচ্ছিল — pure documentation issue।
- **ফিক্স (কমেন্ট-only, কোনো behavior/RPC/DB বদল নেই):** `SomadhanRepository.kt`-এর ১৪ call-site কমেন্ট + ২টা বড় helper doc-comment
  (`refundEscrowOnceLocked`-এর ধাপ-২ ব্লক, `reconcileUserBalances`-এর KDoc — এটাতে প্রথম ড্রাফটে ভুল লেখা হয়েছিল যে dryRun=false RPC
  push করে না, পরে কোড পড়ে দেখা গেল `adminReconcileUserBalances()` RPC + outbox retry আসলে আছে, সংশোধন করা হয়েছে) + `OutboxSyncWorker.kt`-এর
  ১টা রেফারেন্স — মোট ~১৯ জায়গা, সব এখন সঠিকভাবে RPC dual-write + outbox retry-কে আসল মেকানিজম হিসেবে বর্ণনা করছে।
- `MarketEntities.kt`-এ `TransactionEntity.pendingCloudSync`/`cloudBalanceSynced` field declaration-এর উপরে স্থায়ী নোট যোগ (দুটোই
  unread Firebase-era leftover, ইচ্ছাকৃতভাবে সরানো হয়নি — schema/migration change, এই cleanup-এর স্কোপের বাইরে)।
- **যাচাই:** ৩টা ফাইল বদলেছে (`SomadhanRepository.kt`, `MarketEntities.kt`, `OutboxSyncWorker.kt`); ফাইল-সংখ্যা অপরিবর্তিত (৩৮৭=৩৮৭);
  dotfile (.env/.env.example/.gitignore) সব আছে; brace `{}` count প্রতিটা ফাইলে আগে-পরে হুবহু সমান; paren imbalance আগে থেকেই ছিল
  (কমেন্টের ভেতরের টেক্সটে) এবং মূল ফাইলের সাথে একই নেট পরিবর্তন (৩৩/৩৩) দেখায় — নতুন কোনো imbalance যোগ হয়নি।
- আচরণ/RPC/DB/test কিছুই বদলায়নি বলে **নতুন test বা Windows-run লাগেনি**। `BUG_INVENTORY.md` ৭.৭ 🟡 না, সরাসরি ✅ (কোনো verify-pending অংশ নেই)।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৮ — ব্যবহারকারী (ক) বেছেছেন: এখনই ফিক্স করুন) — পরের ধাপ: ৭.৬ real-device verify ও ৭.২ behavioural verify (pgTAP/ম্যানুয়াল) এখনো খোলা (আগের HANDOFF থেকেই); তারপর ৭.৯ (`refund_pending` UI surfacing) সিদ্ধান্ত; সবশেষে Step 8
- ৭.৮ (`admin_reconcile_escrow_states()`-এর Case 2/3 লুপে per-escrow exception-handling না থাকা, money-critical) — ব্যবহারকারী (ক) বেছেছেন।
- **রুট-কজ:** Case 2 (`refund_escrow_once`)/Case 3 (`release_escrow`)-এর `perform` কলে কোনো exception-handling ছিল না — একটা escrow ব্যর্থ হলে (owner USER_ROLE_INACTIVE বা solver SOLVER_ROLE_INACTIVE বা PROBLEM_DISPUTED) পুরো ফাংশন-কল (পুরো লুপ) rollback হয়ে যেত, বাকি সব সুস্থ escrow-ও সেই রানে repair হতো না।
- **ফিক্স:** নতুন migration `supabase/migrations/zz_20260924140000_step7_8_admin_reconcile_escrow_states_per_escrow_exception_handling.sql` — `admin_force_cancel_instant_job` (Step 7.2)-এর precedent অনুযায়ী Case 2/3-এর `perform` কল নেস্টেড `begin...exception when others...end;` ব্লকে মোড়ানো (নেস্টেড ব্লক = implicit savepoint, তাই একটা escrow ব্যর্থ হলে শুধু সেটাই বাদ পড়ে)। সফল হলেই `v_case2_repaired`/`v_case3_repaired` বাড়ে (আগে perform কলের আগে বাড়ত); ব্যর্থ হলে `v_case2_failed`/`v_case3_failed` বাড়ে আর `v_items`-এ `'status':'failed','error':sqlerrm` যোগ হয়। রিটার্ন jsonb-এ নতুন `case2_failed`/`case3_failed` কী যোগ; audit-log `details`-এর টেক্সট-ফরম্যাট অপরিবর্তিত রাখা হয়েছে যাতে বিদ্যমান exact-string pgTAP assertion না ভাঙে। Case 1 ও dry-run path অপরিবর্তিত।
- **টেস্ট:** `supabase/tests/10_admin_moderation_balance_part2.sql`-এ নতুন ৩য়-লাইভ-রান fixture (users `e6000003`/`e6000004` — একজন role-নিষ্ক্রিয় owner, একজন role-নিষ্ক্রিয় solver; escrows `E_RC10..E_RC13` — RC10/RC12 সুস্থ, RC11/RC13 role-নিষ্ক্রিয়) + ৭টা নতুন assertion (plan ২০১→২০৮): (১) `case2_repaired=1,case2_failed=1,case3_repaired=1,case3_failed=1`, (২) সফল দুটো escrow ঠিক status-এ (REFUNDED/RELEASED) গেছে আর ব্যর্থ দুটো HELD-ই আছে (মূল প্রমাণ যে rollback হয়নি), (৩)(৪) owner/solver balance শুধু সফল দুটোর টাকা পেয়েছে, (৫) transaction শুধু সফল দুটোর জন্য তৈরি হয়েছে, (৬) `items`-এ ব্যর্থ দুটোই `status=failed`+`error` (real `sqlerrm`) সহ আছে (একই রান-৩ result থেকে, TEMP TABLE দিয়ে re-use করে অতিরিক্ত side-effecting কল এড়ানো হয়েছে), (৭) ৩য় রানেও audit log ঠিক ১টা যোগ হয়েছে (মোট ৩)।
- **যাচাই:** ফাইল-সংখ্যা অপরিবর্তিত + ১টা নতুন migration ফাইল যোগ (৩৮৭+১=৩৮৮); dotfile সব আছে। নতুন migration ফাইলের নাম `zz_` prefix দিয়ে শুরু (CI-এর অক্ষরক্রম-apply-এর সাথে সামঞ্জস্যপূর্ণ, `zz_20260924130000_step7_2_...`-এর ঠিক পরে বসবে)। Kotlin কোনো ফাইল স্পর্শ করা হয়নি (caller শুধু raw JSON রিটার্ন-ভ্যালু log/toast করে, নতুন কী ভাঙে না)।
- **Live Supabase MCP apply:** apply-এর আগে `pg_get_functiondef()`/`md5(prosrc)` দিয়ে cloud-এর পুরনো বডি (per-escrow exception-handling ছাড়া) কনফার্ম করা হয়েছে; `apply_migration` দিয়ে নতুন বডি push করা হয়েছে; apply-এর পর আবার `pg_get_functiondef()` দিয়ে cloud-এর বডি নতুন কোডের সাথে হুবহু মিলছে কনফার্ম, আর grants (`postgres`/`authenticated`/`service_role`) অপরিবর্তিত কনফার্ম করা হয়েছে।
- **বাকি:** এই সেশনে sandbox network বন্ধ থাকায় pgTAP suite real Postgres-এ চালানো যায়নি (static ভাবেই ফাংশন-বডি ও `refund_escrow_once`/`release_escrow`-এর real exception-message — `USER_ROLE_INACTIVE`/`SOLVER_ROLE_INACTIVE`/`PROBLEM_DISPUTED` — কোড পড়ে মিলিয়ে যাচাই করা হয়েছে)। `10_admin_moderation_balance_part2.sql` (plan ২০৮) app-এর নিজস্ব CI/local pgTAP-এ চালিয়ে ২০৮/২০৮ pass চূড়ান্ত কনফার্ম বাকি। `BUG_INVENTORY.md` ৭.৮ তাই 📝 না, কিন্তু ✅-ও না — 🟡 (৭.২-এর মতো একই কারণে: migration live-এ আছে ও static-verified, কিন্তু behavioural/pgTAP-run confirm বাকি)।
- master prompt Step 7 checkbox এখনো `[ ]`-ই থাকল (Step 7-এর ভেতরের বাকি sub-item — ৭.৬ real-device verify, ৭.২ behavioural verify, ৭.৯ decision — এখনো বাকি বলে)।

## 🔁 HANDOFF (২০২৬-০৯-২৪, ৭.৯ — ব্যবহারকারী (ক) বেছেছেন: এখনই ফিক্স করুন) — পরের ধাপ: গ্রুপ ৭-এর ৯টা আইটেমের সবগুলোরই এখন সিদ্ধান্ত/ফিক্স হয়ে গেছে; বাকি শুধু verify — ৭.২ (behavioural, pgTAP/ম্যানুয়াল), ৭.৬ (real-device), ৭.৮ (pgTAP real-run), ৭.৯ (real-device/ম্যানুয়াল) — এবং ৭.৫ ইচ্ছাকৃতভাবে খোলা (deferred, "পরে")। এই ৪টা verify সম্পন্ন হলে (বা ব্যবহারকারী স্কিপ করতে বললে) Step 7 checkbox `[x]` হবে, তারপর Step 8 (full-suite regression)।
- ৭.৯ (`admin_force_cancel_instant_job`-এর `refund_pending: true` ফ্ল্যাগ Kotlin/admin UI-তে পড়া হতো না) — ব্যবহারকারী (ক) বেছেছেন।
- **রুট-কজ:** Step 7.2-এর ফিক্সের পর RPC `admin_force_cancel_instant_job` মাঝে মাঝে `{'result':'OK','refund_pending':true}` ফেরত দিতে পারে (owner-এর USER role নিষ্ক্রিয় থাকলে — cancel/rebroadcast/to-normal-bidding তবু সম্পন্ন হয়, শুধু escrow HELD থেকে যায়)। কিন্তু `SupabaseSyncManager.adminForceCancelInstantJob()` শুধু `Result<JsonElement>` রিটার্ন করত, `SomadhanRepository.adminForceCancelInstantJob()` সেই ফলাফল শুধু `onFailure`-এ log করত (`onSuccess`-এর ভেতরের `refund_pending` কখনো পড়া হতো না) — তাই admin-কে কোনো toast/সতর্কতা দেওয়া হতো না।
- **ফিক্স (২টা Kotlin ফাইল, কোনো SQL/UI ফাইল না):**
  - `SomadhanRepository.kt`-এর `adminForceCancelInstantJob(problemId, reason, targetAction)` রিটার্ন-টাইপ `Unit` → `Boolean` (refundPending)। দুটো early-return path (`problem == null`, already-terminal status) এখন `return false`। ফাংশনের শেষে `SupabaseSyncManager.adminForceCancelInstantJob(...).onSuccess { json -> ... }` ব্লকে `acceptBid()`-এর (লাইন ~৩০৩৯) precedent অনুযায়ী fully-qualified `(json as? kotlinx.serialization.json.JsonObject)?.get("refund_pending") as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false` দিয়ে ফ্ল্যাগ পার্স করা হয়, `var refundPending` variable-এ সেট হয়, ফাংশনের শেষে `return refundPending`। কোনো session না থাকলে বা dual-write ব্যর্থ হলে false-ই থাকে (নতুন কোনো import লাগেনি, ফাইলের বিদ্যমান fully-qualified `kotlinx.serialization.json.*` প্যাটার্নই অনুসরণ করা হয়েছে)।
  - `SomadhanViewModel.kt`-এর `adminForceCancelInstantJob()`-এ `val refundPending = repository.adminForceCancelInstantJob(...)` — `refundPending == true` হলে "অ্যাডমিন অ্যাকশন সফল হয়েছে, কিন্তু ইউজারের অ্যাকাউন্ট নিষ্ক্রিয় থাকায় রিফান্ড আটকে আছে — অ্যাকাউন্ট সক্রিয় হলে স্বয়ংক্রিয়ভাবে রিফান্ড হবে" toast, নাহলে আগের "জরুরি জবের উপর অ্যাডমিন অ্যাকশন সফল হয়েছে" toast-ই। `onSuccess()` callback (UI dialog বন্ধ করা) দুই ক্ষেত্রেই আগের মতো কল হয় — এখানে ইচ্ছাকৃতভাবে UI dialog/`AdminInstantJobsView.kt` বদলানো হয়নি (রুল অনুযায়ী ন্যূনতম স্কোপ — toast-ই যথেষ্ট, নতুন কোনো UI state/banner দরকার নেই)।
- **টেস্ট:** কোনো automated test নেই — real Supabase RPC response-নির্ভর (owner USER role নিষ্ক্রিয় করে instant job accept+admin force-cancel করার real scenario), ঠিক ৭.২/৭.৬/৭.৮-এর মতোই network-এর সাথে সম্পর্কিত। `OfflineGatingSyncTest.kt`-এর ২৩৪ নং লাইনে `adminForceCancelInstantJob` শুধু action-name string হিসেবে আছে (gated-actions তালিকা), signature/return-type-নির্ভর না — অপ্রভাবিত, unaffected।
- **যাচাই:** ফাইল-সংখ্যা অপরিবর্তিত (৩৮৮=৩৮৮, ৭.৮-এর zip-এর সাথে — নতুন কোনো ফাইল যোগ হয়নি, শুধু ২টা বিদ্যমান Kotlin ফাইল edit); dotfile (.env/.env.example/.gitignore) সব আছে। brace `{}` count দুই ফাইলেই আগে-পরে হুবহু সমান (`SomadhanRepository.kt`: ১৭৮১=১৭৮১, `SomadhanViewModel.kt`: ১৪৪৮=১৪৪৮)। `AdminInstantJobsView.kt`/অন্য কোনো caller `adminForceCancelInstantJob()`-এর রিটার্ন-ভ্যালু ব্যবহার করে না (শুধু `onSuccess` callback), তাই `Unit`→`Boolean` সিগনেচার-বদল compile-safe (Kotlin-এ statement-position-এ রিটার্ন-ভ্যালু discard করা বৈধ)।
- **বাকি:** real-device/ম্যানুয়াল verify — একটা instant job-এ solver accept করিয়ে, owner-এর USER role নিষ্ক্রিয় করে (admin panel দিয়ে), তারপর admin force-cancel করে নতুন "রিফান্ড আটকে আছে" toast দেখা যাচ্ছে কিনা, আর normal (role active) ক্ষেত্রে আগের মতোই সাধারণ success toast আসছে কিনা — দুটোই। `BUG_INVENTORY.md` ৭.৯ তাই 📝 না, কিন্তু ✅-ও না — 🟡 (কোড-সম্পূর্ণ, behavioural verify বাকি, ৭.২/৭.৬/৭.৮-এর একই প্যাটার্ন)।
- master prompt Step 7 checkbox এখনো `[ ]`-ই থাকল — গ্রুপ ৭-এর সবগুলো আইটেমেরই (৭.১-৭.৯) সিদ্ধান্ত হয়ে গেছে এবং যেখানে ফিক্স দরকার ছিল সেগুলোও কোড-সম্পূর্ণ, কিন্তু ৪টা behavioural/pgTAP verify (৭.২, ৭.৬, ৭.৮, ৭.৯) এখনো ব্যবহারকারীর করা বাকি — এবং ৭.৫ ইচ্ছাকৃতভাবে "পরে" হিসেবে খোলা রাখা হয়েছে (কোনো কাজ নেই)। এই ৪টা verify-এর ফলাফল (WINDOWS RESULT: বা সরাসরি "কনফার্মড" বার্তা) এলে Step 7 বন্ধ হবে, তারপর Step 8 (`.\gradlew.bat test --stacktrace` চূড়ান্ত full-suite regression) শুরু হবে।

## 🔁 HANDOFF (২০২৬-০৯-২৪, Step 7 → Step 8 — ব্যবহারকারীর explicit নির্দেশে override: "এগিয়ে যাও এভাবেই") — পরের ধাপ: Step 8 (চূড়ান্ত full-suite regression) — Windows-এ `.\gradlew.bat test --stacktrace` চালিয়ে `WINDOWS RESULT:` পেস্ট করা বাকি
- কোনো কোড এডিট হয়নি এই সেশনে — শুধু ২টা doc আপডেট।
- **সিদ্ধান্ত:** Step 7-এর ৪টা বাকি behavioural/pgTAP verify (৭.২ owner-role-নিষ্ক্রিয় scenario, ৭.৬ chat/problem-detail real-device, ৭.৮ pgTAP real-run plan ২০৮, ৭.৯ refund-pending toast real-device) এখনো 🟡 (কোড-সম্পূর্ণ, শুধু ভেরিফাই বাকি), আর ৭.৫ ইচ্ছাকৃতভাবে 📝 deferred ("পরে") — এই ৫টার কোনোটাই এখনই ব্লক করছে না, ব্যবহারকারী স্পষ্টভাবে বলেছেন এগুলো ছাড়াই এগিয়ে যেতে (bootstrap rule #9-এর "সব ধাপ একসাথে চালিয়ে যাও"-এর মতো একটা explicit override হিসেবে এটাকে ধরা হলো)।
- **master prompt-এ Step 7 checkbox `[x]` করা হয়েছে** — এই override-এর নোটসহ (checkbox-এর টেক্সটেই লেখা আছে যে ৪টা 🟡 এখনো খোলা আছে, ভবিষ্যতে verify হলে `BUG_INVENTORY.md`-তে ✅ হবে)। `BUG_INVENTORY.md`-তে কোনো Status বদলানো হয়নি (আগের সেশনের ৭.২/৭.৬/৭.৮/৭.৯ = 🟡, ৭.৫ = 📝 deferred, বাকিগুলো ✅/✅ closed — সবই ঠিক আছে, override শুধু GATE-এর প্রশ্ন, verify-status-এর না)।
- **এখন Step 8 candidate** — `.\gradlew.bat test --stacktrace` (Step 20.5-এর প্যাটার্নে) Windows-এ পুরো টেস্ট suite চালিয়ে নিশ্চিত হওয়া যে Step 1-7 জুড়ে করা কোনো ফিক্সই নতুন রিগ্রেশন তৈরি করেনি। **এটা এই sandbox-এ চালানো সম্ভব না** (Android SDK/Gradle Windows toolchain এখানে নেই, network-ও restricted) — এই কাজটা ব্যবহারকারীকেই নিজের Windows মেশিনে করতে হবে, ফলাফল `WINDOWS RESULT:` হিসেবে পরের মেসেজে পেস্ট করলে সেটা প্রসেস করে `BUG_INVENTORY.md`-এর সারসংক্ষেপ টেবিল ও এই ফাইলে চূড়ান্ত সারাংশ লেখা হবে।
- **যাচাই:** কোনো কোড-ফাইল বদলায়নি, শুধু `SOMADHAN_BUG_FIX_MASTER_PROMPT.md` (Step 7 checkbox) ও এই `FIX_PROGRESS.md` — ফাইল-সংখ্যা অপরিবর্তিত (৩৮৮=৩৮৮); dotfile সব আছে।
