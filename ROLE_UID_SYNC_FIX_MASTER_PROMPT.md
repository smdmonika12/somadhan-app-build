# Role-Switch / Cross-Device Sync Bug — ধাপে ধাপে Fix Master Prompt

## মূল সমস্যা (সংক্ষেপে)
`switchRole()` role পরিবর্তনের সময় local Room-এ একটা **নতুন id** (`SOLVER_xxxxxxxx` /
`USER_xxxxxxxx`) দিয়ে আলাদা row বানায় — যেটা আসল Supabase Auth UID না। Repository-জুড়ে ৫১+
জায়গায় cloud sync একটা guard-এর পেছনে আটকানো:

```kotlin
if (SupabaseAuthManager.currentUserId() == user.id) { /* cloud sync হবে */ }
```

Role-switched অবস্থায় `user.id` আর real auth UID মেলে না, তাই এই guard-গুলো false হয়ে যায় →
cloud sync স্কিপ হয়ে যায় → অন্য ডিভাইস/admin panel-এ কিছু sync হয় না।

**লক্ষ্য:** role-ভিত্তিক আলাদা local row বাদ দিয়ে, এক ব্যক্তির জন্য **একটাই row** রাখা (id সবসময়
Supabase Auth UID), role শুধু সেই row-এর একটা field হবে।

---

## 🔒 General Rules — প্রতিটা ধাপেই অবশ্যই মানতে হবে

এই rule-গুলো **প্রতিটা প্রম্পটের সাথে** কপি করে দেবে, অথবা প্রথমেই AI agent-কে বলে দেবে যে পুরো
কাজ জুড়ে এই rule-গুলো বলবৎ থাকবে।

1. **শুধু বর্ণিত স্কোপে হাত দেবে।** যে ফাইল/ফাংশন প্রম্পটে উল্লেখ করা হয়নি, সেটা স্পর্শ করবে না —
   এমনকি "ভালো করার" জন্যও না।
2. **কোনো existing UI/UX/feature বদলাবে না।** ব্যবহারকারীর কাছে app যেভাবে আচরণ করে (screen,
   button, flow) — bug ছাড়া — অবিকল একই থাকবে। এটা শুধু data/sync layer-এর ফিক্স, UI redesign না।
3. **কোনো কোড মুছবে না, যদি না প্রম্পটে স্পষ্ট করে বলা থাকে।** পুরনো/legacy path সরাসরি delete না
   করে, প্রথমে নতুন path যোগ করে, পাশাপাশি রেখে, ভ্যালিডেট করে, তারপর আলাদা ধাপে legacy সরানো হবে।
4. **প্রতিটা পরিবর্তনের পর build/compile চেক করবে** (Gradle sync/build), এবং যতটা সম্ভব existing
   test/flow ধরে manually reason করে verify করবে যে অন্য কিছু ভাঙেনি।
5. **কোনো destructive DB migration/data loss করা যাবে না।** existing local Room data বা Supabase
   cloud data মুছে ফেলা বা silently overwrite করা নিষেধ — বিশেষ করে ইতিমধ্যে verified হওয়া KYC,
   balance, বা completed job/transaction data।
6. **প্রতিটা ধাপ ছোট এবং স্বয়ংসম্পূর্ণ (self-contained) হবে** — একটা ধাপ শেষ না করে পরের ধাপে
   এগোবে না।
7. **কোনো অনুমান করলে সেটা স্পষ্ট করে লিখবে (assumption হিসেবে চিহ্নিত করে), চুপচাপ ধরে নেবে না।**
   কোনো কিছু নিশ্চিত না হলে, কোড পড়ে/সোর্স verify করে নিশ্চিত হবে, অনুমান-ভিত্তিক কোড লিখবে না।
8. **প্রতিটা ধাপ শেষে একটা সংক্ষিপ্ত রিপোর্ট দেবে:**
   - কী কী ফাইল/ফাংশন বদলানো হয়েছে
   - কেন বদলানো হয়েছে (এক লাইনে)
   - কোন কোন জায়গায় এখনও পুরনো (legacy) behavior রয়ে গেছে ইচ্ছাকৃতভাবে
   - এই ধাপের পর কী টেস্ট করা উচিত ব্যবহারকারীর
9. **⚠️ CONFIRMATION বাধ্যতামূলক:** প্রতিটা ধাপের কোড লেখা/পরিবর্তন করার পর, **commit বা পরের ধাপে
   যাওয়ার আগে** থামবে এবং ব্যবহারকারীর explicit "ঠিক আছে / confirm" না পাওয়া পর্যন্ত অপেক্ষা করবে।
   Confirmation ছাড়া নিজে থেকে পরের ধাপে এগোবে না, এমনকি ধাপগুলো এক নাগাড়ে দেওয়া থাকলেও না।
10. পুরনো progress-tracking convention অনুযায়ী (`MIGRATION_PROGRESS.md`, `*_PROGRESS.md` ফাইলগুলোর
    মতো), প্রতিটা ধাপ শেষে একটা নতুন ফাইলে (`ROLE_UID_SYNC_FIX_PROGRESS.md`) কী করা হলো তার log
    যোগ করবে, যাতে সেশন হারিয়ে গেলেও পরবর্তী ধাপ কোথা থেকে শুরু করতে হবে বোঝা যায়।

---

## ধাপ ০ — শুধু অডিট, কোনো কোড পরিবর্তন না

```
তুমি শুধু অডিট করবে, কোনো কোড বদলাবে না।

কাজ: গোটা repository-তে (main source set) নিচের প্যাটার্নগুলোর প্রতিটা occurrence খুঁজে বের করো
এবং একটা লিস্ট বানাও (ফাইল নাম + লাইন নাম্বার + এক লাইনে context):

1. `SupabaseAuthManager.currentUserId() ==` — সব জায়গা যেখানে এই guard ব্যবহার হয়েছে cloud
   sync-এর আগে।
2. `"SOLVER_"` এবং `"USER_"` prefix দিয়ে id বানানো/চেক করা হয় এমন সব জায়গা (UUID generation,
   startsWith চেক, ইত্যাদি)।
3. `linkedAccountId` ফিল্ড read/write করা হয় এমন সব জায়গা।
4. `getLinkedUserByRole` / `getUserByContactAndRole` কল করা হয় এমন সব জায়গা।
5. এমন সব জায়গা যেখানে `currentUser.id` বা `user.id`-কে foreign key হিসেবে ব্যবহার করা হচ্ছে
   (problem.userId, transaction.userId/solverId, notification.userId, chat participant id ইত্যাদি) —
   এগুলো role switch-এর পর ভাঙতে পারে কিনা বোঝার জন্য।

আউটপুট: একটা মার্কডাউন রিপোর্ট (`ROLE_UID_AUDIT.md`) যেখানে উপরের প্রতিটা ক্যাটাগরির জন্য একটা
টেবিল/লিস্ট থাকবে। কোনো কোড এডিট করবে না। রিপোর্ট তৈরি হলে থামো এবং আমার confirmation-এর অপেক্ষা করো।
```

**তুমি (ব্যবহারকারী) কনফার্ম করার পর করণীয়:** রিপোর্টটা পড়ে দেখো scope আন্দাজমতো লাগছে কিনা, তারপর
ধাপ ১-এ এগোতে বলো।

---

## ধাপ ১ — ডেটা মডেল ডিজাইন করা (এখনও কোনো লজিক বদলানো হবে না)

```
আগের ধাপে (ROLE_UID_AUDIT.md) যে অডিট হয়েছে সেটা পড়ো।

কাজ: `UserEntity`-র বর্তমান schema (Room entity ক্লাস) দেখাও, বিশেষ করে role-সংক্রান্ত ফিল্ডগুলো
(role, hasUserRole, hasSolverRole, balanceUser, balanceSolver, solverCategories,
hasCompletedSolverSetup, kyc* ফিল্ডগুলো, linkedAccountId, displayUid)।

তারপর একটা ডিজাইন প্রস্তাব লেখো (কোড না, শুধু ডিজাইন ডকুমেন্ট — `ROLE_UID_FIX_DESIGN.md`):
- কীভাবে role switch-কে "নতুন row বানানো" থেকে "একই row-এর একটা field flip করা"-তে বদলানো যায়,
  বিদ্যমান schema-তে কী কী field যথেষ্ট আছে আর নতুন কী লাগতে পারে (যদি লাগে) তা স্পষ্ট করে লেখো।
- যেসব জায়গায় (ধাপ ০-এর অডিট অনুযায়ী) `user.id`-এর SOLVER_xxx/USER_xxx prefix-এর উপর নির্ভর করে
  আলাদা আচরণ করা হচ্ছে, সেগুলোকে কীভাবে `role`/`hasSolverRole`/`hasUserRole` field-ভিত্তিক করা
  যায় তার একটা ম্যাপিং লিখো (item-by-item, ধাপ ০-এর লিস্ট অনুযায়ী)।
- **ইতিমধ্যে ইনস্টল করা থাকা ডিভাইসগুলোতে যে পুরনো SOLVER_xxx/USER_xxx row থাকতে পারে, সেগুলোর
  ডেটা (KYC, categories, balance) হারিয়ে যাবে না — কীভাবে migrate/merge করা হবে তার একটা প্ল্যান
  আলাদা করে লেখো (এটা implement করবে না, শুধু প্ল্যান)।**

কোনো .kt ফাইল এডিট করবে না। শুধু ডিজাইন ডকুমেন্ট লেখো, তারপর থামো এবং confirmation-এর অপেক্ষা করো।
```

**তুমি কনফার্ম করার পর করণীয়:** ডিজাইনটা পড়ে দেখো migration প্ল্যানে ডেটা হারানোর ঝুঁকি আছে কিনা
নিশ্চিত হও, তারপর ধাপ ২-এ যাওয়ার অনুমতি দাও।

---

## ধাপ ২ — নতুন single-row role-switch লজিক লেখা (পুরনোটা পাশে রেখে)

```
আগের ধাপের ডিজাইন (ROLE_UID_FIX_DESIGN.md) অনুযায়ী কাজ করবে। এখনও পুরনো `switchRole()` ফাংশন
ডিলিট/ওভাররাইট করবে না।

কাজ: `SomadhanRepository`-তে একটা **নতুন** ফাংশন লেখো (যেমন `switchRoleInPlace()`), যেটা:
- নতুন কোনো UserEntity row বানাবে না, id বদলাবে না — একই root row-এর role-সংক্রান্ত ফিল্ডগুলো
  (role, hasUserRole, hasSolverRole, solverCategories, hasCompletedSolverSetup) update করবে।
- বিদ্যমান `SupabaseSyncManager.switchRole()` RPC কলটাই ব্যবহার করবে (এটা আগে থেকেই root
  row/auth.uid()-ভিত্তিক, তাই এটা বদলানোর দরকার নেই — শুধু verify করো cloud RPC আসলেই root
  row-ভিত্তিক কিনা, সোর্স পড়ে confirm করো, অনুমান করবে না)।
- পুরনো `switchRole()` ফাংশনটা **অপরিবর্তিত অবস্থায় কোডে থেকে যাবে** (এখনও কল হচ্ছে,
  ViewModel এখনও এটাই ব্যবহার করবে — wiring পরের ধাপে হবে)।

এই ধাপে ViewModel বা UI-এর কোনো কল-সাইট বদলাবে না — শুধু নতুন ফাংশনটা repository-তে যোগ করো,
compile করে দেখো ভাঙছে না। রিপোর্ট লেখো, থামো, confirmation-এর অপেক্ষা করো।
```

**তুমি কনফার্ম করার পর করণীয়:** নতুন ফাংশনের কোড ডিফ রিভিউ করো, বিশেষ করে balance/KYC ফিল্ড
handling ঠিক আছে কিনা দেখো, তারপর ধাপ ৩-এ যাওয়ার অনুমতি দাও।

---

## ধাপ ৩ — existing installs-এর জন্য migration/merge লজিক

```
ধাপ ১-এর migration প্ল্যান (ROLE_UID_FIX_DESIGN.md-তে লেখা) অনুযায়ী কাজ করবে।

কাজ: একটা migration ফাংশন লেখো যেটা app চালু হওয়ার সময় (বা login-এর ঠিক পরে) একবার চেক করবে —
বর্তমান logged-in ইউজারের জন্য local Room-এ যদি `linkedAccountId` দিয়ে যুক্ত পুরনো SOLVER_xxx/
USER_xxx row থাকে, তাহলে সেই row-এর KYC/categories/balance ডেটা root row-এ merge করে দেবে
(root row-এর ডেটা যদি ইতিমধ্যে থাকে, সেটা overwrite করবে না — শুধু ফাঁকা/অনুপস্থিত ফিল্ড পূরণ
করবে), এবং merge-এর পর পুরনো linked row-টা delete না করে শুধু "migrated" হিসেবে চিহ্নিত করে
রাখবে (যাতে rollback সম্ভব হয়)।

এই ফাংশনটা এখনও কোথাও থেকে call হবে না (wiring পরের ধাপে) — শুধু ফাংশনটা লেখো এবং
unit-test-এর মতো একটা ছোট manual verification note লেখো (কী input দিলে কী output আশা করা
উচিত)। কম্পাইল করে দেখাও। থামো, confirmation-এর অপেক্ষা করো।
```

**তুমি কনফার্ম করার পর করণীয়:** এই ধাপটা সবচেয়ে ঝুঁকিপূর্ণ (ডেটা মার্জ) — ভালোভাবে দেখো merge
logic কোনোভাবে ভুল ডেটা ওভাররাইট করছে কিনা, তারপর অনুমতি দাও।

---

## ধাপ ৪ — ViewModel wiring বদলানো (আসল সুইচ)

```
আগের ৩ ধাপে তৈরি হওয়া নতুন ফাংশনগুলো (switchRoleInPlace, migration ফাংশন) এখন কাজে লাগাও।

কাজ:
1. Login flow-এর ঠিক পরে migration ফাংশনটা call করো (ধাপ ৩-এর)।
2. `SomadhanViewModel.switchRoleToSolver()` এবং `switchRoleToUser()`-কে পুরনো
   `repository.switchRole()`-এর বদলে নতুন `repository.switchRoleInPlace()` কল করতে বদলাও।
3. পুরনো `switchRole()` ফাংশনটা repository-তে **রেখে দাও** কিন্তু dead code হিসেবে (কোথাও থেকে
   কল হবে না) — এখনই ডিলিট করবে না।
4. ধাপ ০-এর অডিটে যেসব জায়গায় `user.id`-এর SOLVER_xxx/USER_xxx prefix-এর উপর নির্ভর করে আচরণ
   বদলাতো (যদি থাকে), সেগুলোকে `currentUser.role`/`hasSolverRole` চেক দিয়ে বদলাও — একটা একটা
   করে, প্রতিটার আগে-পরে দেখাও।

এই ধাপের পর role switch করলে UID/id বদলানোর কথা না — নিজে থেকে verify করো (logic পড়ে) যে
`currentUser.id` role switch-এর আগে-পরে অভিন্ন থাকছে। কম্পাইল করাও। থামো, রিপোর্ট দাও,
confirmation-এর অপেক্ষা করো।
```

**তুমি কনফার্ম করার পর করণীয়:** সম্ভব হলে emulator/device-এ আসলে টেস্ট করো — role switch করে
দেখো id বদলাচ্ছে কিনা (logcat-এ `currentUser.id` print করিয়ে), তারপর অনুমতি দাও।

---

## ধাপ ৫ — cloud sync guard-গুলো ঠিক করা (৫১ জায়গা)

```
ধাপ ৪ শেষে এখন role switch-এর পরেও user.id == root Supabase UID থাকার কথা। এই ধাপে
`SupabaseAuthManager.currentUserId() == user.id` guard-গুলো (ধাপ ০-এর লিস্ট অনুযায়ী, ৫১টা) এক
এক করে verify করো — প্রতিটার জন্য বলো:

- এই guard এখন ধাপ ৪-এর পরিবর্তনের কারণে এমনিতেই সঠিকভাবে pass করছে কিনা (কোনো কোড বদলানো
  লাগবে না), নাকি
- এখনও কোনো কারণে ভুল হতে পারে (যেমন admin অন্য কারো row edit করছে এমন কেস, যেখানে
  currentUserId() ≠ target user id হওয়াটাই সঠিক — সেগুলো বদলানো যাবে না, শুধু "self" কেসগুলো
  ঠিক করতে হবে)।

**একবারে সব ৫১টা বদলাবে না।** প্রতি ব্যাচে ৫-১০টা করে (একই feature area-র, যেমন প্রথমে
KYC-সংক্রান্ত সব guard, তারপর profile-সংক্রান্ত, তারপর wallet-সংক্রান্ত...) — প্রতি ব্যাচ শেষে
থামো, রিপোর্ট দাও, confirmation নাও, তারপর পরের ব্যাচে যাও।
```

**তুমি কনফার্ম করার পর করণীয়:** প্রতি ব্যাচের পর ছোট করে test করো (যেমন KYC ব্যাচের পর — solver
role-এ KYC সাবমিট করে অন্য ডিভাইস/admin panel-এ দেখো আসছে কিনা), তারপর পরের ব্যাচে অনুমতি দাও।

---

## ধাপ ৬ — পুরো regression টেস্ট (কোনো নতুন কোড না, শুধু verify)

```
এই ধাপে কোনো নতুন ফিচার কোড লিখবে না। শুধু একটা manual test checklist বানাও
(`ROLE_UID_FIX_TEST_CHECKLIST.md`) যেটাতে অন্তত এই কেসগুলো থাকবে:

- ডিভাইস A: নতুন account, KYC সাবমিট, role switch (User↔Solver), post করা, বিড করা — সব ঠিকমতো
  local-এ দেখাচ্ছে কিনা।
- ডিভাইস B (একই account, ভিন্ন device): login করে ডিভাইস A-তে করা সব ডেটা (KYC status, posts,
  balance) দেখা যাচ্ছে কিনা।
- ডিভাইস A-তে role switch করার পর ডিভাইস B-তে refresh করলে সেই পরিবর্তন দেখা যাচ্ছে কিনা।
- Admin panel থেকে ডিভাইস A/B-এর ইউজারের KYC approve/reject করলে সেটা user-এর ডিভাইসে পৌঁছাচ্ছে
  কিনা।
- পুরনো (migration-এর আগের) কোনো টেস্ট অ্যাকাউন্ট থাকলে — migration ধাপ ৩ ঠিকমতো তাদের ডেটা
  merge করেছে কিনা, কিছু হারায়নি কিনা।

চেকলিস্ট বানানোর পর থামো — আমি নিজে টেস্ট করে ফলাফল জানাবো।
```

**তুমি কনফার্ম করার পর করণীয়:** চেকলিস্ট ধরে ধরে বাস্তবে টেস্ট করো, ফলাফল (pass/fail) সহ পরের
মেসেজে জানাও। কোনোটা fail করলে সেটা নিয়ে আলাদা ছোট ফিক্স ধাপ শুরু হবে (ধাপ ৭ এর আগেই)।

---

## ধাপ ৭ (ঐচ্ছিক, অনেক পরে) — Legacy কোড পরিষ্কার করা

```
শুধুমাত্র যদি ধাপ ৬-এর সব টেস্ট বহুদিন (প্রোডাকশনে, একাধিক ইউজারের উপর) স্থিতিশীলভাবে pass করে
থাকে, তবেই এই ধাপ করবে। এখনই এটা করার দরকার নেই — এই প্রম্পটটা ভবিষ্যতের জন্য।

কাজ: পুরনো `switchRole()` (dead code), migration ফাংশন (যদি সব ইউজার migrate হয়ে গিয়ে থাকে),
এবং আর ব্যবহার না হওয়া SOLVER_xxx/USER_xxx-সংক্রান্ত helper ফাংশনগুলো একে একে সরাও — প্রতিটা
সরানোর আগে দেখাও কোথাও আর reference নেই, একটা একটা করে সরাও, প্রতিবার কম্পাইল করাও, থামো,
confirmation নাও।
```

---

## কীভাবে ব্যবহার করবে
প্রতিটা ধাপের কোড-ব্লক (```...```) আলাদা করে কপি করে AI agent-কে দাও — প্রথমবার **General
Rules** সেকশনটাও একসাথে জুড়ে দাও (বা বলে দাও "এই rule-গুলো পুরো কাজ জুড়ে মানতে হবে")। একটা ধাপ
শেষে agent থামবে ও রিপোর্ট দেবে — তুমি রিভিউ করে "ঠিক আছে, পরের ধাপে যাও" বললেই পরের ধাপের প্রম্পট
দেবে। কোনো ধাপে সমস্যা মনে হলে সেখানেই থামিয়ে rollback/আলোচনা করবে, জোর করে এগোবে না।
