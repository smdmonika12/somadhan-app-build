# Somadhan Bug-Fix Master Prompt — CI_TEST_SUITE-এ পাওয়া বাগগুলো ধাপে ধাপে ফিক্স করা

## পটভূমি

`CI_TEST_SUITE_MASTER_PROMPT.md`/`CI_TEST_SUITE_PROGRESS.md` (Step 0–20, সম্পূর্ণ) দিয়ে অনেক সেশন
জুড়ে বাগ **খুঁজে বের করা ও প্রমাণ করা** হয়েছে (rule ছিল: কোনো production কোড বদলানো যাবে না, শুধু
Step 12.x-এ ব্যতিক্রম দেওয়া হয়েছিল)। ফলাফল consolidated আছে **`BUG_INVENTORY.md`**-তে।

**এই master prompt-এর উদ্দেশ্য পুরোপুরি আলাদা: এখন সেই বাগগুলো আসলেই ফিক্স করা হবে, একটার পর একটা,
প্রতিটা ফিক্সের পর test দিয়ে প্রমাণ করে, যাতে একটাও বাদ না পড়ে আর একটাও তাড়াহুড়া করে ভুলভাবে বসানো না
হয়।**

**এই prompt-টা প্রতিটা নতুন Claude session-এর শুরুতে হুবহু paste করবেন, সাথে project-এর সর্বশেষ zip
আবার আপলোড করবেন। নিচের "session বুটস্ট্র্যাপ" ধাপগুলো অনুসরণ করলে Claude নিজেই বুঝে নেবে ঠিক কোন
ধাপে আছি, কোনটা পরের কাজ — আলাদা করে কিছু ব্যাখ্যা করে দিতে হবে না।**

## 0️⃣ Session বুটস্ট্র্যাপ — প্রতিটা নতুন session প্রথমেই এই ক্রমে করবে

এই অংশটাই সবচেয়ে গুরুত্বপূর্ণ — এটা অনুসরণ করলে যেকোনো Claude (এই সেশনের কনটেক্সট ছাড়াই) শুধু zip আর
এই ফাইল পড়ে নিজে থেকেই সঠিক জায়গা থেকে শুরু করতে পারবে।

1. **আপলোড করা zip আনজিপ করে root-এ এই ৩টা ফাইল, এই ক্রমেই, সম্পূর্ণ পড়বে:**
   1. `SOMADHAN_BUG_FIX_MASTER_PROMPT.md` (এই ফাইল — নিয়ম ও "ধাপসমূহ" লিস্ট) — **আপনি এখন এটাই
      পড়ছেন যদি এই ফাইলটা zip-এর ভেতরে থাকে।**
   2. `BUG_INVENTORY.md` — কোন বাগ কোথায়, কোন Status (🔴/🟡/🟠/✅/📝) — Step নেওয়ার আগে অবশ্যই এখান
      থেকে ঠিক সারিটা আবার পড়বে, অনুমান করবে না।
   3. `FIX_PROGRESS.md` — আগের session-গুলোর লগ। **শুধু সবচেয়ে শেষের `## 🔁 HANDOFF` ব্লকটাই পড়লেই
      যথেষ্ট** (এটাই সবচেয়ে সাম্প্রতিক সত্য) — পুরো ফাইল উপর থেকে পড়ার দরকার নেই যদি না HANDOFF-এ
      কোনো confusion/contradiction থাকে।
2. **কোন Step-এ আছি সেটা এইভাবে ঠিক করবে (তিনটা সোর্স মিলিয়ে):**
   - এই ফাইলের নিচের "ধাপসমূহ" তালিকায় **প্রথম যে Step-এর checkbox এখনো `[ ]`** সেটাই candidate।
     GATE নিয়ম: তার আগের সব Step `[x]` না হলে এটা নেওয়া যাবে না।
   - `FIX_PROGRESS.md`-এর শেষ HANDOFF নোট এই candidate-কে কনফার্ম করবে (HANDOFF-এ স্পষ্ট লেখা থাকবে
     "পরের ধাপ: Step N")। দুটো না মিললে (যেমন কেউ ম্যানুয়ালি checkbox বদলে দিয়েছে), **HANDOFF নোটকে
     বিশ্বাস করবে, checkbox-কে না** — আর এই মিসম্যাচটা নতুন session-এর এন্ট্রিতে একটা লাইনে উল্লেখ
     করবে।
   - **Step 2–5 (admin panel, ৪টা আলাদা উপ-বাগ) এবং Step 6 (offline-gating, একাধিক উপ-ফিক্স)** —
     এই দুটো Step-এর ভেতরেও একবারে একটাই sub-item নেওয়া হয় (rule ১ দ্রষ্টব্য)। HANDOFF নোটে ঠিক কোন
     sub-item (যেমন "Step 6-এর ভেতরে শুধু ৩.১b বাকি") পরেরটা তা স্পষ্ট লেখা থাকবে — সেটাই নেবে, পুরো
     Step আবার শুরু থেকে না।
   - **কোনো `WINDOWS RESULT: ...` ব্যবহারকারীর বর্তমান মেসেজে থাকলে**, নতুন কোনো Step নেওয়ার **আগে**
     সেটা প্রসেস করবে (আগের কোনো Step-এর ফিক্স verify করার ফলাফল) — pass হলে `BUG_INVENTORY.md`-এ
     Status 🟡→✅ করবে, fail হলে আগের Step আবার খুলে root-cause ধরবে (পরের Step-এ না গিয়ে)।
3. **Step নেওয়ার সাথে সাথে, কোনো কোড না ছুঁয়ে, ৩টা জিনিস verify করবে:**
   - আপলোড করা zip-এর ফাইল-সংখ্যা/গঠন `FIX_PROGRESS.md`-এর শেষ entry-র "যা বদলেছে" লিস্টের সাথে
     সঙ্গতিপূর্ণ কিনা (sanity-check, CI suite-এর প্যাটার্নে)।
   - `BUG_INVENTORY.md`-এর সেই বাগের সারিতে দেওয়া ফাইল/ফাংশন-নাম দিয়ে `grep -n` করে বর্তমান কোডে
     exact লোকেশন বের করবে (আগের সেশনের লেখা লাইন-নাম্বার stale হতে পারে)।
   - `CI_TEST_SUITE_PROGRESS.md`-এ রেফারেন্স করা মূল investigation সেকশনটা (Step 16.2/18.4/19.6 —
     যেটাই প্রাসঙ্গিক) আরেকবার পড়ে নেবে পুরো context বুঝে নেওয়ার জন্য, শুধু `BUG_INVENTORY.md`-এর
     সংক্ষিপ্ত সারাংশের উপর ভিত্তি করে ফিক্স শুরু করবে না।
4. এরপর "প্রতিটা session-এ Claude যা করবে (General Rules)"-এর ১-১১ নং নিয়ম অনুযায়ী কাজ করবে।

**সব Step `[x]` হয়ে গেলে** (ভবিষ্যতে): এই ফাইলের একদম উপরে CI_TEST_SUITE_MASTER_PROMPT.md-এর মতো
একটা "🎉 সম্পূর্ণ" স্ট্যাটাস-লাইন যোগ হবে, আর নতুন session তখন কোনো Step অনুমান করে নেবে না — ব্যবহারকারী
নতুন কী চান জিজ্ঞেস করে শুরু করবে।

## প্রতিটা session-এ Claude যা করবে (General Rules)

1. **একবারে একটাই Step নেবে।** নিচের "ধাপসমূহ" থেকে GATE অনুযায়ী প্রথম অসম্পূর্ণ Step (checkbox
   `[ ]`) নেবে — আগের সব Step `[x]` না হলে পরেরটা শুরু হবে না। একটা Step-এর ভেতরে একাধিক উপ-বাগ
   থাকলেও (যেমন Step 2-5, admin panel-এর ৪টা) **প্রতিটা উপ-বাগ আলাদা sub-step**, একসাথে না।
2. **প্রতিটা fix-এর আগে:** `BUG_INVENTORY.md`-এ সেই বাগের সারি আবার পড়ে নিশ্চিত হবে ঠিক কোন ফাইল/
   ফাংশন/লাইন বদলাতে হবে — অনুমান করে না, `CI_TEST_SUITE_PROGRESS.md`-এর রেফারেন্স করা সেকশন
   (লাইন নাম্বার শিফট হতে পারে, তাই ফাংশন-নাম দিয়ে `grep -n` করে বর্তমান লাইন বের করবে) থেকে exact
   context আবার পড়বে।
3. **ন্যূনতম, টার্গেটেড এডিট।** শুধু সেই নির্দিষ্ট বাগ ফিক্স করার জন্য যা লাগে তাই বদলাবে — কোনো
   "ভালো লাগছে তাই" refactor/cleanup/rename না, স্কোপ-ক্রিপ না। অন্য কোনো বাগ/গ্যাপ চোখে পড়লে (এমনকি
   ছোট হলেও) `BUG_INVENTORY.md`-তে গ্রুপ ৭ (Backlog)-এ নতুন সারি হিসেবে যোগ করবে, **এই সেশনে ছোঁবে
   না**।
4. **টেস্ট-প্রথম নীতি।** যে বাগের জন্য ইতিমধ্যে failing test আছে (`RealtimeSubscriptionScopeTest.kt`,
   `AdminPanelStep19RegressionTest.kt`), ফিক্স করার আগে সেই test-টা আবার পড়ে নিশ্চিত হবে ঠিক কোন
   assertion-টা এখন fail করছে এবং কেন — ফিক্সের ঠিক লক্ষ্য এটাকেই pass করানো। যে বাগের কোনো test নেই
   (গ্রুপ ৩ — offline-gating), সেই Step-এর প্রথম কাজ **একটা bug-demonstrating test লেখা** (এখন fail
   করবে এমন), তারপরই ফিক্স।
5. **কাজ শেষ হলে (বা tool/context limit শেষ হওয়ার আগে থেমে যেতে হলে):**
   - `FIX_PROGRESS.md`-এ নতুন session-এর বিস্তারিত এন্ট্রি + HANDOFF নোট যোগ করবে (ঠিক কী বদলেছে,
     কোন ফাইলে, কেন, কীভাবে verify করা হয়েছে/হবে)।
   - `BUG_INVENTORY.md`-তে সেই বাগের সারির Status আপডেট করবে (🔴/🟠 → 🟡 "FIXED, NOT VERIFIED" যদি
     Windows-run confirm না হয়ে থাকে, অথবা সরাসরি ✅ যদি একই সেশনে `WINDOWS RESULT:` পেস্ট করে
     confirm হয়ে যায়)।
   - বাগ সত্যিই পুরোপুরি ফিক্স+ভেরিফাইড হলে master prompt-এ (এই ফাইল) সেই Step-এর চেকবক্স `[x]` করবে।
6. **rule #5/#5a (CI suite থেকে বজায় রাখা):** সব ফাংশন-নাম/লাইন-নাম্বার স্ক্যান case-insensitive
   হবে, আর লাইন-নাম্বার রেফারেন্সের বদলে ফাংশন-নাম দিয়ে খুঁজে বের করা primary পদ্ধতি হবে (আগের
   সেশনে edit হওয়ায় শিফট হতে পারে)।
7. **money-critical fix-এ (Step 1) extra সতর্কতা:** Step 1 (realtime subscription) সরাসরি balance/
   escrow display-কে প্রভাবিত করে বলে — ফিক্স বসানোর পর শুধু নতুন test pass করলেই যথেষ্ট না, `BUG_
   INVENTORY.md`-এ যে ৩টা user-confirmed উপসর্গ (bid-accept reset, release না-দেখানো, withdrawal
   reset) লেখা আছে, সেগুলোর জন্য একটা ছোট manual-test-checklist-ও বানাবে (Step 12-এর
   `ROLE_UID_FIX_TEST_CHECKLIST.md`-এর প্যাটার্নে) — যাতে ব্যবহারকারী নিজে দুই-account দিয়ে বাস্তবে
   verify করতে পারেন।
8. **কোনো bug ফিক্স করার সময় অন্য কোনো এখনো-`[ ]`-থাকা Step-এর ফাইল ছোঁয়া লাগলে** (ওভারল্যাপ থাকলে,
   যেমন গ্রুপ ২.৪-এর `AdminUsersView.kt` generalization-প্রশ্ন), সেই ওভারল্যাপ স্পষ্ট করে progress
   doc-এ নোট করবে এবং **সেই Step-এই একসাথে ফিক্স করবে যদি সেটা একই bug-এর অংশ হয়** (নতুন আলাদা Step
   বানাবে না), কিন্তু সত্যিই আলাদা বাগ হলে touch করবে না, Step ৭ (backlog)-এ নোট রাখবে।
9. **প্রতিটা Step-এর পর ব্যবহারকারীর explicit confirmation ছাড়া পরের Step শুরু হবে না**
   (`ROLE_UID_SYNC_FIX_MASTER_PROMPT.md`-এর rule #৯-এর মতো) — যদি না ব্যবহারকারী স্পষ্টভাবে "সব
   ধাপ একসাথে চালিয়ে যাও" বলে দেন। কোনো `WINDOWS RESULT:` পেস্ট করা থাকলে সেটা প্রথমে প্রসেস করবে,
   তারপর পরের ধাপ প্রশ্ন করবে।
10. **প্রতিটা session zip আকারে আসবে/যাবে** (CI suite-এর মতোই) — session শুরুতে পুরো tree দেখবে,
    input বনাম প্রত্যাশিত state sanity-check করবে, শেষে **প্রজেক্টের সব ফাইল অপরিবর্তিত রেখে শুধু এই
    সেশনে যা বদলেছে সেটাসহ নতুন zip বানাবে** (ফাইল-সংখ্যা ও dotfile উপস্থিতি যাচাই করে)।
11. **কোনো migration/SQL RPC বদলানো লাগলে** (এই মুহূর্তে কোনো Step-এই লাগার কথা না — গ্রুপ ১/২/৩
    সবই Kotlin-only অনুযায়ী Step 18.4/19.6/16.2-এ চিহ্নিত), তাহলে Step 12.x-এর precedent অনুযায়ী
    **ব্যবহারকারীর আলাদা explicit অনুমতি** লাগবে migration ফাইল বসানোর আগে (rule #1a-এর ব্যতিক্রম,
    শুধু সেই একটা নির্দিষ্ট migration-এর জন্য)।

## ধাপসমূহ (GATE: প্রতিটা `[ ]`-এর আগেরটা `[x]` না হলে পরেরটা শুরু হবে না)

- [x] **Step 0 — Setup + re-confirm।** `BUG_INVENTORY.md` আরেকবার পুরো progress doc-এর বিপরীতে
      cross-check করে নিশ্চিত হওয়া কোনো বাগ বাদ পড়েনি (বিশেষভাবে Step 17/অন্য কোনো ছোট ফাইলে ছড়ানো
      finding)। গ্রুপ ৪ (UI transaction sign)-এর `TransactionDisplaySignTest`/
      `WalletDashboardSignWiringTest` কোনো নতুন session-এ real Gradle run দিয়ে re-confirm করা (যদি
      network থাকে) — এটা ইতিমধ্যে Step 20.5-এ একবার pass কনফার্মড হয়েছিল, শুধু ডাবল-চেক। এই Step
      শেষে `BUG_INVENTORY.md`-তে কোনো এডিট লাগলে করবে, কোনো কোড এডিট না।

- [x] **Step 1 — ✅ Realtime subscription re-scoping (গ্রুপ ১, money-critical, সর্বোচ্চ অগ্রাধিকার) — কোড-ফিক্স + Windows real-run pass কনফার্মড (২০২৬-০৯-২৩)।**
      `SomadhanViewModel.kt`-এর `logout()`, `completeLoginAfterOtp()`, `switchRoleToSolver()`,
      `switchRoleToUser()`, `loginAsAdmin()` — প্রতিটাতে account/role পরিবর্তনের ঠিক পরে
      `SupabaseRealtimeManager.startRealtimeListeners()` আবার কল করা (আর `logout()`-এ
      `stopRealtimeListeners()`)। ফিক্সের পর `RealtimeSubscriptionScopeTest.kt` pass করার কথা।
      rule #৭ অনুযায়ী manual test checklist-ও বানাবে (bid-accept, release, withdrawal তিনটা
      scenario, দুই role/account দিয়ে)।

- [x] **Step 2 — ✅ Admin panel ক্লাস A: Overview metrics permanent-override (গ্রুপ ২.১) — কোড-ফিক্স + Windows real-run pass কনফার্মড (২০২৬-০৯-২৩)।**
      `adminDashboardMetrics`-এর ১০টা `if (supa.X > 0) supa.X else local.X` প্যাটার্ন বাদ দিয়ে
      সবসময় local reactive aggregate ব্যবহার। `AdminPanelStep19RegressionTest.kt`-এর
      `testOverviewMetricsPermanentOverrideBugStillPresent` pass করার কথা (নাম বদলাতে হতে পারে,
      "StillPresent" আর সত্যি না — টেস্ট রিনেম/আপডেট করে নেওয়া এই Step-এরই অংশ)।

- [x] **Step 3 — ✅ Admin panel ক্লাস B১: Withdrawal status silent no-op (গ্রুপ ২.২) — কোড-ফিক্স + Windows real-run pass কনফার্মড (২০২৬-০৯-২৩)।**
      `updateWithdrawalStatus()`-এর status-guard fail হলে caller-কে জানানো (sealed `WithdrawalUpdateResult`),
      `adminUpdateWithdrawalStatus()` ও bulk-approve dialog দুটোতেই conditional toast। টেস্ট রিনেম হয়েছে
      `testWithdrawalStatusUpdateSurfacesFailureBugFixed`-এ (আগে `...SwallowsFailureBugStillPresent`,
      "StillPresent" আর সত্যি না — Step 2-এর precedent অনুযায়ী রিনেম এই Step-এরই অংশ)।

- [x] **Step 4 — ✅ Admin panel ক্লাস B২: Reconcile/repair stuck spinner (গ্রুপ ২.৩) — কোড-ফিক্স + Windows real-run pass কনফার্মড (২০২৬-০৯-২৩)।**
      `adminRepairMissingRefunds()`/`adminReconcileBalances()`-এর `catch` ব্লকে `onComplete()`
      যোগ করা। `testReconcileAndRepairStuckSpinnerBugStillPresent` pass করার কথা।

- [x] **Step 5 — ✅ Admin panel ক্লাস B৩: Paged-cache staleness (গ্রুপ ২.৪) — কোড-ফিক্স + Windows real-run pass কনফার্মড (২০২৬-০৯-২৩)। পুরো গ্রুপ ২ (admin panel, Step 2-5) সম্পূর্ণ+verified।**
      `resetAdminWithdrawalsPagination()` mutation-এর পর ও `refreshAdminTab()`-এ কল করা। rule #৮
      অনুযায়ী `AdminUsersView.kt`-এর একই architecture-ও এই Step-এই verify+fix করে ফেলা (আলাদা করে
      Step লাগবে না, যদি সত্যিই একই বাগ প্রমাণিত হয়)। দুটো relevant test pass করার কথা।

- [x] **Step 6 — ✅ Offline-action-gating toggle (গ্রুপ ৩) — ৩টা উপ-ফিক্সই কোড-সম্পূর্ণ + real-device
      verified কনফার্মড (৩.১b Windows-run pass ২০২৬-০৯-২৩, ৩.১a/৩.১c real-device কনফার্ম ২০২৬-০৯-২৪, "ekdom
      perfect kaj korche")। পুরো গ্রুপ ৩ সম্পূর্ণ+verified।**
      dual-write ব্যর্থতা surface করা (`PlatformSettingUpdateResult`), `NET_CAPABILITY_VALIDATED`,
      degraded-session persistent সতর্কতা (`DegradedAdminSessionBar`) — তিনটাই কনফার্মড।

- [x] **Step 7 — ✅ Backlog আইটেম আলোচনা (fix না, শুধু সিদ্ধান্ত) — গ্রুপ ৭-এর ৯টা আইটেমের (৭.১-৭.৯)
      সবগুলোরই সিদ্ধান্ত নেওয়া হয়েছে ও প্রযোজ্য ক্ষেত্রে কোড-ফিক্স সম্পূর্ণ। ব্যবহারকারীর explicit
      নির্দেশে (২০২৬-০৯-২৪, "এগিয়ে যাও এভাবেই") ৪টা বাকি behavioural/pgTAP verify (৭.২, ৭.৬, ৭.৮,
      ৭.৯ — সবই 🟡) এবং ৭.৫ (ইচ্ছাকৃতভাবে "পরে" deferred, 📝) block না করেই Step 8-এ এগোনো হলো —
      এগুলো ভবিষ্যতে যেকোনো সময় ব্যবহারকারী verify করে `BUG_INVENTORY.md`-তে ✅ করাতে পারবেন।**

- [ ] **Step 8 — চূড়ান্ত full-suite regression + সামগ্রিক সারাংশ।** Windows-এ পুরো
      `.\gradlew.bat test --stacktrace` চালিয়ে (Step 20.5-এর প্যাটার্নে) নিশ্চিত করা যে এই পুরো
      fix-effort কোনো নতুন রিগ্রেশন তৈরি করেনি — সব test class green (আগে-থেকে-পরিচিত flaky
      `ExampleRobolectricTest` বাদে)। `BUG_INVENTORY.md`-এর সারসংক্ষেপ টেবিল আপডেট করে সব ✅ দেখানো
      (গ্রুপ ৭-এ যা open থেকে গেছে তা বাদে)। `FIX_PROGRESS.md`-এ চূড়ান্ত সারাংশ লেখা।

## zip workflow (প্রতিটা session-এর শুরু/শেষে)

CI_TEST_SUITE-এর মতোই: প্রতিটা session-এ আপলোড করা zip আনজিপ করে root-এর এই ফাইল ও
`FIX_PROGRESS.md`/`BUG_INVENTORY.md` আগে সম্পূর্ণ পড়া, শেষে সব ফাইল অপরিবর্তিত রেখে শুধু বদলানো
অংশসহ নতুন zip বানানো (ফাইল-সংখ্যা+dotfile যাচাই করে), zip-টা দেওয়া, আর সংক্ষেপে বলা কোন Step
শেষ হলো আর পরের Step কী।
