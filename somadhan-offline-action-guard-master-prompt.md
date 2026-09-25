# Somadhan — Offline-First Action Gating: Master Prompt

## প্রেক্ষাপট (Context)

কোড চেক করে দেখা গেছে বর্তমান আর্কিটেকচার লক্ষ্যের ঠিক উল্টো:

- `MainActivity.kt`-এ `NoInternetOverlay` নেটওয়ার্ক না থাকলে **পুরো অ্যাপ** full-screen ব্লক করে দেয় এবং নিচের UI-তে ক্লিকও intercept করে (কমেন্টেই লেখা আছে "Strict - offline access blocked")।
- অথচ অ্যাপে ইতিমধ্যে একটা পরিপক্ব **Outbox queue system** আছে ঠিক এই কাজের জন্যই: `OutboxSyncWorker`, `OutboxRpcDispatcher`, `PendingSyncOutboxEntity`/`PendingSyncOutboxDao`, `OutboxPendingIndicator.kt`, `docs/OUTBOX_RETRY_DESIGN.md`।
- বর্তমানে `platform_settings`-এর জন্য কোনো user-friendly admin toggle UI নেই — শুধু `AdminSupabaseExplorerView.kt` (raw টেবিল এক্সপ্লোরার) আছে।

তাই আসল কাজ শুধু "নতুন ফিচার বানানো" না — বরং বিদ্যমান স্ট্রিক্ট ব্লকিং সরিয়ে সেই জায়গায় সঠিক, selective, per-action গেটিং বসানো, যাতে Outbox-এর মতো ইনফ্রা যেটা এখন কার্যত অকেজো হয়ে আছে (কারণ ইউজার offline হলে স্ক্রিনই দেখতে পারে না), সেটা আসলে কাজে লাগে — এবং সাথে একটা admin-controlled toggle রাখা, যাতে দরকার হলে পুরনো strict-block মোডে ফেরত যাওয়া যায়।

## লক্ষ্য (Goal)

- Admin panel-এর Settings থেকে একটা toggle থাকবে (নাম যেমন "Strict Offline Block")।
  - **Toggle ON** হলে: পুরনো আচরণ — network না থাকলে পুরো অ্যাপে ঢোকাই যাবে না (বর্তমান strict full-block, অপরিবর্তিত)।
  - **Toggle OFF** হলে: network না থাকলেও অ্যাপে ঢোকা যাবে, cached/local ডেটা **দেখা** যাবে, কিন্তু এই মাস্টার প্রম্পট অনুযায়ী কোনো **action trigger করা যাবে না** — শুধু read-only।
- শুধু যেসব action সরাসরি সার্ভারে কিছু write/change করে (post, bid, payment, withdrawal, message send, ইত্যাদি), সেগুলো network না থাকলে (এবং toggle OFF অবস্থায়) কাজ করবে না — এবং ইউজারকে স্পষ্ট ফিডব্যাক দেবে (silent fail না)।
- Money-critical action (payment, withdrawal, escrow release/refund, bid accept) কখনো optimistic local-write বা network ছাড়া allow করা যাবে না — toggle-এর অবস্থা যা-ই হোক।
- যেসব action ইতিমধ্যে Outbox দিয়ে queue হয় (non-critical), toggle OFF অবস্থায় সেগুলো offline-এও queue হয়ে থাকবে, পরে auto-sync হবে — ব্লক করার দরকার নেই।

## অলঙ্ঘনীয় নিয়ম (Inviolable Rules — প্রতিটি ধাপ প্রম্পটে পুনরায় লেখা থাকবে)

1. বিদ্যমান কোনো ফাংশনালিটি ভাঙা যাবে না। কোনো ধাপে সন্দেহ হলে কোড না পাল্টে প্রশ্ন করবে।
2. Money-critical পাথ (payment, withdrawal, escrow, admin balance adjust, bid accept) কখনো offline-এ optimistic/silent allow করা যাবে না — network confirm ছাড়া action block-ই থাকবে, শুধু স্পষ্ট মেসেজ দেখাবে। এটা toggle-independent — toggle OFF থাকলেও এই actionগুলো block-ই থাকবে।
3. যেসব action Outbox দিয়ে ইতিমধ্যে নিরাপদে queue হচ্ছে, সেগুলোর behavior পাল্টানো যাবে না — শুধু ভেরিফাই করা হবে।
4. Toggle-এর নিজের value app বন্ধ/অফলাইন থাকা অবস্থাতেও জানা থাকতে হবে — তাই এটা local (Room/SharedPreferences/DataStore) cache-এ রাখতে হবে, শুধু cloud `platform_settings`-এ রাখলে চলবে না (কারণ অফলাইনে সেটাই প্রথমে পড়তে হবে decide করতে কোন মোডে যাবে)।
5. Global blocking overlay নিয়ে কাজ করার সময় splash-screen exception এবং existing `manualOverrideConnected` লজিক বজায় রাখতে হবে।
6. প্রতিটি ধাপ শেষে সম্পূর্ণ প্রজেক্ট zip — dotfile (.env, .gitignore ইত্যাদি) সহ — দিতে হবে, এবং আগের zip-এর ফাইল-লিস্টের সাথে মিলিয়ে ভেরিফাই করতে হবে কোনো ফাইল miss হয়নি।
7. প্রতিটি ধাপ শেষে progress ফাইলে (নিচে বর্ণিত) একটা এন্ট্রি লিখতে হবে — কী করা হলো, কী বাকি, পরের সেশন কোথা থেকে শুরু করবে।
8. কোনো ধাপ আধা শেষ অবস্থায় সেশন শেষ হলে zip/progress ফাইলে স্পষ্ট 🟡 partial flag দিতে হবে, এবং পরের সেশন প্রথমে সেই বাকি অংশ শেষ করবে, তারপর ইউজারের কনফার্মেশন নিয়ে পরের ধাপে যাবে।
9. এই sandbox-এ Gradle build সম্ভব না (কোনো Maven/Google repo network path নেই) — প্রতি ধাপে শুধু bracket/brace/paren balance manually চেক করে রাখতে হবে, আসল build ভেরিফাই Android Studio-তে হবে।
10. কোড এডিটের আগে প্রাসঙ্গিক ফাইলগুলো আগে সম্পূর্ণ পড়ে/গ্রেপ করে বর্তমান আচরণ বোঝা, ধরে নেওয়া না।
11. প্রতিটি ধাপ একটা single Claude session-এর মধ্যে শেষ করার মতো ছোট রাখতে হবে।

## প্রগ্রেস ফাইল

`OFFLINE_ACTION_GATING_PROGRESS.md` নামে একটা নতুন ফাইল প্রজেক্ট রুটে তৈরি হবে (মূল `MIGRATION_PROGRESS.md`-তে মেশানো হবে না), যেখানে প্রতি ধাপ শেষে তারিখ/ব্যাচ নম্বর, কী করা হলো, ভেরিফিকেশন রেজাল্ট, এবং পরের ধাপ কোথা থেকে শুরু — এই ফরম্যাটে এন্ট্রি যোগ হবে।

---

## ধাপসমূহ

### ধাপ ১ — ইনভেন্টরি ও ক্যাটাগরাইজেশন (কোনো কোড পরিবর্তন নয়)

পুরো কোডবেসে (`SomadhanRepository.kt`, `SomadhanViewModel.kt`, `Supabase*Repository.kt`, সব `*Screen.kt`) প্রতিটি action call-site (button/onClick যেটা সরাসরি network hit করে — RPC, Postgrest insert/update, Auth call) খুঁজে বের করে ৩ ভাগে ভাগ করা:

- **গ্রুপ A** — ইতিমধ্যে Outbox দিয়ে queue হয় (নিরাপদে offline-capable)
- **গ্রুপ B** — সরাসরি network call, Outbox-এ নেই, এবং money-critical বা state-critical (login/register, payment, withdrawal, escrow, bid accept/cancel/reject, admin balance adjust, dispute resolve)
- **গ্রুপ C** — read-only call (initial load, pull-to-refresh, realtime subscribe) যেগুলো এখন full-block overlay-এর আড়ালে লুকানো, তাই offline-এ আসলে কেমন আচরণ করে তা যাচাই করা হয়নি

আউটপুট: একটা inventory ডকুমেন্ট (ফাইলনাম + ফাংশন + গ্রুপ), ইউজারকে দেখিয়ে কনফার্ম নেওয়া, তারপরই পরের ধাপ শুরু।

### ধাপ ২ — Admin Toggle ইনফ্রাস্ট্রাকচার (Strict Offline Block অন/অফ)

- `platform_settings`-এ নতুন key যোগ (যেমন `strict_offline_block`, ডিফল্ট মান নিয়ে ইউজারকে জিজ্ঞেস করে ঠিক করা — বর্তমান আচরণের সাথে ব্যাকওয়ার্ড-কম্প্যাটিবল রাখতে ডিফল্ট `'true'` রাখাই নিরাপদ, যাতে migration মুহূর্তে হুট করে সব ইউজারের জন্য আচরণ না পাল্টে যায়; admin পরে ইচ্ছেমতো `'false'` করবে)।
- যেহেতু বর্তমানে কোনো friendly admin-settings UI নেই (শুধু raw `AdminSupabaseExplorerView.kt`), AdminPanelScreen-এ একটা নতুন ছোট Settings সেকশন/ট্যাব বানিয়ে সেখানে এই টগল (Switch) বসানো, existing admin RPC/update-pattern অনুসরণ করে সেভ করা।
- অ্যাপ সাইডে: টগলের মান লগইন/সিঙ্কের সময় cloud থেকে টেনে **স্থানীয়ভাবে cache** করা (Room/DataStore) — যাতে ডিভাইস অফলাইন অবস্থাতেও (এমনকি অ্যাপ restart করলেও) জানে কোন মোডে চলতে হবে, কোনো নেটওয়ার্ক কল ছাড়াই।
- এই ধাপে শুধু infra + UI + local caching — এখনো `MainActivity.kt`-এর মূল ব্লকিং লজিক পাল্টানো হবে না (সেটা ধাপ ৩-এ, এই টগলের মান read করে branch করবে)।

### ধাপ ৩ — গ্লোবাল ব্লকিং লজিক টগল-অনুযায়ী শাখা করা

`MainActivity.kt`-এর `NoInternetOverlay` ব্যবহারের জায়গাটা কন্ডিশনাল করা:

- Cached টগল মান `true` (Strict) হলে → বর্তমান আচরণ অপরিবর্তিত (পুরো-অ্যাপ full-block, click-intercept)।
- Cached টগল মান `false` হলে → পুরো-অ্যাপ ব্লকের বদলে একটা ছোট, non-blocking status banner (উপরে/নিচে চিকন bar: "ইন্টারনেট নেই — শুধু আগের ডেটা দেখা যাচ্ছে") দেখানো, বাকি UI স্বাভাবিকভাবে ব্যবহারযোগ্য থাকবে।

Splash-screen exception ও `manualOverrideConnected` লজিক দুই মোডেই অপরিবর্তিত রাখা। `NoInternetOverlay`/`NoInternetScreenContent` কম্পোনেন্ট ডিলিট না করে রেখে দেওয়া (Strict মোডে এখনো দরকার)।

### ধাপ ৪ — Reusable Online-Guard হেল্পার

একটা শেয়ার্ড হেল্পার তৈরি (যেমন ViewModel-এ `requireOnlineOrWarn(): Boolean`), যেটা গ্রুপ B-এর প্রতিটি action কল করার আগে চেক করবে — অফলাইন হলে (টগল OFF অবস্থায়, কারণ টগল ON হলে ইউজার এমনিতেই স্ক্রিনে ঢুকতে পারবে না) snackbar/toast দেখাবে ("ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না") এবং action এক্সিকিউট হবে না, কিন্তু স্ক্রিন/নেভিগেশন অক্ষত থাকবে।

### ধাপ ৫–১১ — গ্রুপ B ওয়্যারিং (ডোমেইন অনুযায়ী ব্যাচ)

প্রতিটি নিচের ডোমেইন আলাদা ধাপ (session-size বজায় রাখতে):

- ধাপ ৫: Auth (login, register, OTP verify, logout)
- ধাপ ৬: Wallet/Payment (deposit, gateway payment, withdrawal request)
- ধাপ ৭: Bidding lifecycle (accept/cancel/reject bid, solver cancel job)
- ধাপ ৮: Problem posting (create problem, instant job create)
- ধাপ ৯: Chat/Messages send
- ধাপ ১০: Ratings/Reviews submit, Additional charges request/respond
- ধাপ ১১: Admin actions ও KYC upload

প্রতিটি ধাপে শুধু সেই ডোমেইনের গ্রুপ B call-site-গুলোতে ধাপ ৪-এর গার্ড বসানো হবে; উপরে-নিচের eligibility/validation লজিক অপরিবর্তিত থাকবে (শুধু guard যোগ হবে, রিরাইট না)।

### ধাপ ১২ — গ্রুপ A ভেরিফিকেশন

Outbox-covered action গুলো ধাপ ৩-এর পরিবর্তনের পর (টগল OFF মোডে) সত্যিই offline-এ সঠিকভাবে queue হচ্ছে ও `OutboxPendingIndicator` দেখাচ্ছে কিনা — কোড-রিভিউ দিয়ে যাচাই (দরকার না হলে কোড পরিবর্তন নেই)।

### ধাপ ১৩ — গ্রুপ C (read/cached-view) যাচাই

প্রতিটি স্ক্রিনের initial-load/pull-to-refresh টগল OFF মোডে অফলাইনে blank/error না দেখিয়ে cached Room ডেটা দেখাচ্ছে কিনা যাচাই; প্রয়োজনে ছোট "অফলাইন — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে" ইনডিকেটর যোগ করা (full block না)।

### ধাপ ১৪ — ফাইনাল QA

দুই মোডেই (টগল ON এবং OFF) airplane-mode toggle করে ম্যানুয়াল টেস্ট চেকলিস্ট:

- টগল ON: পুরনো strict full-block আচরণ ঠিকঠাক আছে কিনা।
- টগল OFF: কোনো স্ক্রিন ক্র্যাশ/full-block করছে না, প্রতিটি money-critical action সঠিকভাবে disabled+feedback দিচ্ছে, Outbox queue এখনো কাজ করছে, নেটওয়ার্ক ফিরলে auto-sync হচ্ছে।
- Admin panel থেকে টগল সুইচ করলে (রানটাইমে বা পরের অ্যাপ-ওপেনে) সঠিক মোডে switch হচ্ছে কিনা।

---

**পরবর্তী সেশন শুরু হবে ধাপ ১ (ইনভেন্টরি) দিয়ে।**
