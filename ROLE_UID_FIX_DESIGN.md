# ROLE_UID_FIX_DESIGN.md — ধাপ ১: ডেটা মডেল ডিজাইন (কোনো .kt ফাইল এডিট হয়নি)

তৈরি: এই সেশনে, `ROLE_UID_SYNC_FIX_MASTER_PROMPT.md`-এর ধাপ ১ অনুযায়ী, `ROLE_UID_AUDIT.md`
(ধাপ ০)-এর ভিত্তিতে। **কোনো `.kt`/`.sql` ফাইল এডিট করা হয়নি — শুধু পড়া ও ডিজাইন লেখা হয়েছে।**

---

## ১. বর্তমান `UserEntity` schema (role-সংক্রান্ত ফিল্ডগুলো)

`app/src/main/java/com/example/data/entity/UserEntity.kt` (৮২ লাইন, `@Entity(tableName = "users")`)।
প্রাসঙ্গিক ফিল্ডগুলো:

| ফিল্ড | টাইপ | ব্যাখ্যা |
|---|---|---|
| `id` | `String` (PK) | বর্তমানে dual-row মডেলে root account-এর জন্য Supabase Auth UID, কিন্তু linked row-এর জন্য `SOLVER_xxxxxxxx`/`USER_xxxxxxxx` (লাইন ১০১২, `switchRole()`)। |
| `role` | `String` | `"USER"` / `"SOLVER"` / `"ADMIN"` — dual-row মডেলে প্রতি row-এর নিজস্ব role। |
| `hasUserRole`, `hasSolverRole` | `Boolean` | ইতিমধ্যেই আছে, root ও linked দুই row-তেই `true`/`true` সেট করা হয় `switchRole()`-এ (লাইন ৯৬৩, ৯৮৯-৯৯০) — অর্থাৎ **এই দুটো ফ্ল্যাগ single-row মডেলের জন্য যথেষ্ট প্রস্তুত**, নতুন কিছু লাগবে না। |
| `linkedAccountId` | `String?` | root ও linked row-কে জোড়া লাগানোর FK (self-referential, root-এর ক্ষেত্রে নিজের id-ই)। |
| `displayUid` | `String` | আগে থেকেই root-এর real server UID দুই row-তেই কপি হয় (audit-এর "গুরুত্বপূর্ণ প্রেক্ষাপট" অংশ দেখুন) — এটা ইতিমধ্যে single-identity ধারণা মেনে চলে। |
| `balanceUser`, `balanceSolver` | `Double` | **role-scoped, আগে থেকেই আছে** (ধাপ ১৪.৫ কলাম, `UserMappers.kt`-এ pass-through, `switchRole()`-এ cloud RPC থেকে populate হয়)। plain `balance` ফিল্ড "active role"-এর ভ্যালু ক্যাশ করে। |
| `reputationScoreUser`, `reputationScoreSolver`, `isBannedUser`, `isBannedSolver`, `isRestrictedUser`, `isRestrictedSolver` | — | একই প্যাটার্ন, role-scoped, আগে থেকেই আছে। |
| `solverCategories`, `hasCompletedSolverSetup` | — | শুধু SOLVER role-প্রাসঙ্গিক, single-row মডেলে root row-এর ফিল্ড হিসেবেই থাকবে (role switch করলে হারানোর দরকার নেই)। |
| `kyc*` (৯টা ফিল্ড) | — | **role-scoped না** — একটাই সেট। বর্তমানে dual-row মডেলে নতুন SOLVER linked-row বানানোর সময় root-এর KYC কপি হয় (লাইন partial: `isKycVerified`/`kycStatus` শুধু SOLVER role হলে কপি হয়, USER হলে `"none"`/`false` বসে — লাইন ~১০৩৫-১০৩৬)। single-row মডেলে এই role-conditional reset-এর দরকার নেই, কারণ row-ই একটা থাকবে — KYC ডেটা এমনিতেই টিকে থাকবে switch করলেও। |
| `freeJobsUsedThisMonth`, `freeJobsMonthKey`, `cycleJobCount`, `cycleMissCount`, `instantJobNotificationsEnabled` | — | root row-এর ফিল্ড, role-independent — single-row মডেলে এমনিতেই ঠিক থাকে। |
| `legacyDualRoleMergeDoneAt` | `Long` | **ভিন্ন উদ্দেশ্যে আগে থেকেই আছে** — cloud→local role-scoped merge marker (`mergeLegacyDualRoleDataFromCloud()`)। এটা এই ধাপ ৩-এর local dual-row (SOLVER_xxx/USER_xxx row) migration marker হিসেবে reuse করা ঠিক হবে না (অর্থ গুলিয়ে যাবে) — নিচে ৪ নং সেকশনে নতুন ফিল্ডের প্রস্তাব দেখুন। |

**উপসংহার: schema-তে নতুন কোনো ফিল্ড লাগবে না** single-row role-switch লজিকের জন্য —
`hasUserRole`/`hasSolverRole`/`role`/`balanceUser`/`balanceSolver`/`solverCategories`/
`hasCompletedSolverSetup` সবই আগে থেকেই আছে এবং সঠিক অর্থেই ব্যবহৃত হচ্ছে। শুধু migration-এর
জন্য একটা নতুন optional marker ফিল্ড লাগতে পারে (৪ নং সেকশন দেখুন, ধাপ ৩-এ চূড়ান্ত হবে)।

---

## ২. মূল ডিজাইন প্রস্তাব — "নতুন row" থেকে "field flip"-এ যাওয়া

### ইতিমধ্যে যা ঠিক আছে (audit + এই ধাপে সোর্স পড়ে confirm করা, অনুমান না)

1. **Cloud RPC (`switch_role_get_or_create_linked_profile`, `SupabaseSyncManager.switchRole()`
   লাইন ১৪২২-১৪৪২)** — কমেন্ট ও কল-সাইট পড়ে নিশ্চিত হওয়া গেছে এটা ইতিমধ্যেই single-row
   মডেলে rewrite করা (caller-এর নিজের root row, `id = auth.uid()`, update করে — কোনো নতুন
   row insert করে না)। **এটা বদলানোর দরকার নেই।**
2. `SomadhanRepository.switchRole()`-এর ভেতরে (লাইন ৯৬০-৯৭০) root row (`updatedOriginal`)
   আপডেট করার অংশটা — `hasUserRole = true`, `hasSolverRole` conditional flip — **এটাই
   আসলে single-row মডেলের core logic**, শুধু এটা root row-এর পাশাপাশি **আরেকটা dual row-ও
   বানায়/আপডেট করে** (লাইন ৯৭০-এর পরের অংশ)। অর্থাৎ single-row logic ইতিমধ্যে root
   row-এর উপর প্রয়োগ হচ্ছে, শুধু সেটাই "একমাত্র সত্য" না রেখে পাশে dual-row-ও রাখা হচ্ছে।

### প্রস্তাবিত নতুন ফাংশন: `switchRoleInPlace()`

`SomadhanRepository`-তে (ধাপ ২-এ লেখা হবে, এই ধাপে শুধু ডিজাইন):

```
switchRoleInPlace(user: UserEntity, newRole: String, newCategories: List<String>): UserEntity
```

যা করবে (dual-row অংশ **বাদ দিয়ে**, শুধু root row flip):

1. `rootAccountId = user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id` — একই
   root-id resolution যেটা `switchRole()`-এ আছে (migration-এর সময় পুরনো linked row থেকে কল
   হলেও সঠিক root বের করার জন্য দরকার, নিচে ধাপ ৩-এর নোট দেখুন)।
2. root row-কে (`id = rootAccountId`) load করে (linked row থেকে না, root থেকেই) কপি করে
   আপডেট করবে:
   - `role = newRole`
   - `hasUserRole = true`, `hasSolverRole = if (newRole == "SOLVER" || user.hasSolverRole) true else user.hasSolverRole`
   - SOLVER হলে: `solverCategories` (নতুন দিলে সেটা, নাহলে বিদ্যমান), `hasCompletedSolverSetup`
   - `balance = if (newRole == "SOLVER") balanceSolver else balanceUser` (active-role cache
     আপডেট, plain ফিল্ড UI পড়ে)
   - অনুরূপভাবে `reputationScore`/`isBanned`/`isRestricted` active cache আপডেট
     role-scoped কলাম থেকে (existing pattern, audit-এর ক্যাটাগরি ১ নোট অনুযায়ী)
   - **`id` কখনো বদলাবে না।**
3. `userDao.updateUser(updatedRoot)` — কোনো `insertUser()` কল নেই (এটাই মূল পার্থক্য পুরনো
   `switchRole()`-এর তুলনায়)।
4. বিদ্যমান `SupabaseSyncManager.switchRole()` RPC-ই কল করবে (root guard
   `SupabaseAuthManager.currentUserId() == rootAccountId` সহ, অপরিবর্তিত — এটা ইতিমধ্যেই
   সঠিক, ১ নং পয়েন্ট দেখুন) এবং রিটার্ন হওয়া role-scoped কলামগুলো একইভাবে persist করবে।
5. role-change notification পাঠানো (বিদ্যমান লজিকের মতোই, কিন্তু `resultUser.id` এখন
   সবসময় root id, তাই আলাদা কিছু বদলাতে হবে না)।

**যা এই ফাংশন করবে না (ইচ্ছাকৃতভাবে):** কোনো নতুন `UserEntity` তৈরি/insert করবে না, কোনো
`SOLVER_xxx`/`USER_xxx` id বানাবে না, `getLinkedUserByRole`/`getUserByContactAndRole` কল
করবে না (এগুলো dual-row lookup-এর জন্য, single-row মডেলে অপ্রয়োজনীয়)।

পুরনো `switchRole()` ফাংশন **অপরিবর্তিত কোডে থেকে যাবে** (ধাপ ২ অনুযায়ী) — ViewModel এখনও
সেটাই কল করবে যতক্ষণ না ধাপ ৪-এ wiring বদলানো হয়।

---

## ৩. ধাপ ০-এর অডিট আইটেমগুলোর ম্যাপিং (item-by-item)

### ক্যাটাগরি ১ — ৫১টা `currentUserId() == ...` guard

**গুরুত্বপূর্ণ পর্যবেক্ষণ (audit-এর ক্যাটাগরি ২ ও ৫-এর সাথে সামঞ্জস্যপূর্ণ, এই ধাপে আবার
verify করা হয়েছে):** এই ৫১টা guard-এর **একটাও** সরাসরি `id.startsWith("SOLVER_")` বা
`id.startsWith("USER_")` টাইপ prefix-চেকের উপর ভিত্তি করে আলাদা আচরণ করে না। এগুলো সবই
সরল ভ্যালু-তুলনা: `SupabaseAuthManager.currentUserId() == <row-এর>.id`। বাগটা প্রকাশ পায়
কারণ role-switched অবস্থায় `<row>.id` (যেমন `user.id`, `problem.userId`, `solver.id`)
root UID না হয়ে `SOLVER_xxx`/`USER_xxx` হয়ে যায় — guard-এর কোডটা নিজে ভুল না, **ইনপুট
(`user.id`) ভুল**।

তাই এই ৫১টার জন্য item-by-item field-ভিত্তিক rewrite (`role`/`hasSolverRole` চেক দিয়ে
প্রতিস্থাপন) **প্রয়োজন নেই এবং করাও ঠিক হবে না** — কারণ এই guard-গুলোর উদ্দেশ্যই হলো "এই
action যে করছে সে কি এই row-এর মালিক কিনা" যাচাই করা, role না। সঠিক ফিক্স হলো: ধাপ ২-এ
`switchRoleInPlace()` চালু হওয়ার পর (এবং ধাপ ৪-এ ViewModel wiring বদলানোর পর)
`currentUser.id` সবসময় root Supabase UID-ই থাকবে — তখন এই ৫১টা guard এমনিতেই সঠিকভাবে
pass করবে, **কোনো কোড পরিবর্তন ছাড়াই**। এটাই master prompt-এর ধাপ ৫-এর "verify, বদলাবে না"
পদ্ধতির ভিত্তি — audit-এর ক্যাটাগরি ৫-এর পর্যবেক্ষণের সাথে মিলে যায়।

সংক্ষেপে ম্যাপিং (ফাংশন-গ্রুপ অনুযায়ী, audit টেবিলের ক্রম অনুসরণ করে):

| ফাংশন-গ্রুপ (audit ক্যাটাগরি ১ থেকে) | প্রয়োজনীয় পরিবর্তন |
|---|---|
| KYC (`submitKyc`) | কোনো কোড পরিবর্তন না — id স্থিতিশীল হলেই ঠিক হয়ে যাবে |
| Wallet (`requestWithdrawal`, `depositMoneyViaGateway`) | কোনো কোড পরিবর্তন না |
| Jobs/Bids (`createProblem`, `placeBid`, `acceptBid`, `withdrawBid`, ইত্যাদি) | কোনো কোড পরিবর্তন না |
| Instant Jobs (`createInstantJob`, `markSolverOnWay/Arrived`, `cancelInstantJob`, ইত্যাদি) | কোনো কোড পরিবর্তন না |
| Disputes (`raiseDispute`, `withdrawDispute`, `settleDispute`) | কোনো কোড পরিবর্তন না |
| Chat/Notifications (`sendMessage`, `markMessagesAsReadForProblem`, `markAllNotificationsAsRead`) | কোনো কোড পরিবর্তন না |
| Ratings (`submitUserRatingForSolver`, `submitSolverRatingForUser`) | কোনো কোড পরিবর্তন না |
| Direct Contract (`createDirectContract`, `acceptDirectContractProposal`, `declineDirectContractProposal`) | কোনো কোড পরিবর্তন না |
| `switchRole` (লাইন ১০৮১, `rootAccountId` ব্যবহার করে) | ইতিমধ্যে সঠিক ✅ (audit-এ চিহ্নিত) |
| `updateUser`, `updateUserPassword`, `registerUser` | কোনো কোড পরিবর্তন না |
| `toggleFavoriteSolver`, `addFavoriteSolver` | কোনো কোড পরিবর্তন না |
| `resolveCommissionRateForNewJob` | কোনো কোড পরিবর্তন না |
| `requestAdminAssistance` | কোনো কোড পরিবর্তন না |

**তবে** — ধাপ ৫-এ প্রতিটা এখনও ব্যাচে ব্যাচে বাস্তবে verify করা হবে (master prompt অনুযায়ী),
কারণ এই ডিজাইন ধাপে শুধু সোর্স পড়ে যুক্তি দেওয়া হয়েছে, রানটাইমে test করা হয়নি।

### ক্যাটাগরি ২ — `SOLVER_`/`USER_` id generation (১টা সাইট, লাইন ১০১২)

`switchRoleInPlace()`-এ এই লাইনটা **থাকবে না** (dual-row branch বাদ)। পুরনো `switchRole()`-এ
এটা অপরিবর্তিত থাকবে (dead code হওয়ার আগ পর্যন্ত, ধাপ ৪-৭)।

### ক্যাটাগরি ৩ — `linkedAccountId` (২২টা occurrence)

- `UserEntity.linkedAccountId`, `UserDto`, `UserMappers` ম্যাপিং — **অপরিবর্তিত থাকবে**
  (schema বদলানো হচ্ছে না)।
- `SomadhanRepository.kt`-এর root-id resolution প্যাটার্ন (`user.linkedAccountId?.takeIf {...} ?: user.id`,
  ৪ জায়গায় কপি-পেস্ট) — `switchRoleInPlace()`-এও এই একই প্যাটার্ন ব্যবহার হবে (ধাপ ৩-এর
  migration-এর আগে পুরনো linked row থেকে এই ফাংশন কল হলে root খুঁজে বের করার জন্য দরকার)।
  এটাকে common helper-এ বের করা যেতে পারে, কিন্তু **এই ফিক্সের স্কোপে না** (general rule #১,
  শুধু বর্ণিত স্কোপ) — আলাদা refactor হিসেবে ভবিষ্যতে বিবেচনা করা যায়, এখানে শুধু পর্যবেক্ষণ
  হিসেবে নোট রাখা হলো।
- `AppDaos.kt`-এর `getLinkedUserByRole`/`getLinkedAccounts` কোয়েরি — **অপরিবর্তিত থাকবে**
  (ধাপ ৩-এর migration ফাংশন পুরনো linked row খুঁজতে এগুলোই ব্যবহার করবে)।
- `AdminSolverQuotaView.kt` লাইন ৩৫৪-এর admin-সাইড matching — এই ফিক্সের স্কোপের বাইরে
  (UI ফাইল, admin quota view) — touch করা হবে না।

### ক্যাটাগরি ৪ — `getLinkedUserByRole`/`getUserByContactAndRole` (২টা call site)

- `hasExistingSolverProfile()` (লাইন ৭৬৭-৭৬৮) — dual-row lookup, single-row মডেলে
  প্রাসঙ্গিকতা কমে যায় কিন্তু এই ফাংশনটা `switchRoleInPlace()`-এর স্কোপের বাইরে (আলাদা
  ফাংশন, প্রম্পটে উল্লেখ নেই) — **টাচ করা হবে না** (general rule #১)।
- `switchRole()` (লাইন ৯৭৩-৯৭৪) — পুরনো ফাংশনেই থাকবে, `switchRoleInPlace()`-এ এই কল
  থাকবে না (২ নং সেকশনে বলা হয়েছে)।

### ক্যাটাগরি ৫ — FK হিসেবে `user.id`/`solver.id` ব্যবহার

audit-এর নিজের পর্যবেক্ষণের সাথে একমত: এই সাইটগুলো ক্যাটাগরি ১-এর একই ফাংশনের ভেতরে,
তাই id স্থিতিশীল হলে এগুলোও স্বয়ংক্রিয়ভাবে সঠিক হয়ে যায় — **আলাদা পরিবর্তন লাগবে না।**

---

## ৪. Migration/Merge প্ল্যান (existing installs, plan-only, ধাপ ৩-এ implement হবে)

**লক্ষ্য:** যে ডিভাইসে আগে থেকে role switch করা হয়েছে (তাই local Room-এ পুরনো
`SOLVER_xxx`/`USER_xxx` linked row আছে), সেই row-এর KYC/categories/balance ডেটা হারানো
যাবে না।

### প্ল্যান (ধাপ ৩-এ কোড হবে)

1. **কখন চলবে:** app শুরুর সময় বা login-এর ঠিক পরে, প্রতি logged-in ইউজারের জন্য একবার।
2. **সনাক্তকরণ:** `userDao.getLinkedAccounts(rootAccountId)` কল করে root-এর সব linked
   row বের করা (এটা ইতিমধ্যেই আছে DAO-তে, ক্যাটাগরি ৩ দেখুন) — filter করে `id != rootAccountId`
   এবং `id.startsWith("SOLVER_") || id.startsWith("USER_")` (dual-row চেনার একমাত্র
   নির্ভরযোগ্য উপায়, যেহেতু root id সবসময় UUID-shape Supabase Auth UID)।
3. **Merge নিয়ম (non-destructive, root-কে override না করে):**
   - প্রতিটা KYC ফিল্ড: root-এ যদি ফাঁকা/none থাকে (`kycStatus == "none"` অথবা
     `kycDocumentType == null` ইত্যাদি) এবং linked row-এ ডেটা থাকে, তাহলে root-এ কপি করা হবে।
     root-এ ইতিমধ্যে ডেটা থাকলে **touch করা হবে না** (rule #৫: existing verified KYC হারানো
     যাবে না)।
   - `solverCategories`/`hasCompletedSolverSetup`: root-এ ফাঁকা হলে linked থেকে কপি।
   - `balanceSolver`/`balanceUser`: **conflict হলে merge করা হবে না, শুধু log/flag করা হবে**
     (assumption হিসেবে চিহ্নিত — কোনটা "সঠিক" balance সেটা নির্ধারণ করা এই migration-এর কাজ
     না, বরং যদি root-এর role-scoped কলাম ইতিমধ্যে non-zero থাকে সেটাই রাখা হবে; শুধু root-এর
     ভ্যালু 0.0/default হলে linked row-এর ভ্যালু কপি হবে — এটা ধাপ ৩-এ implement করার আগে
     ব্যবহারকারীর (তোমার) সাথে আরেকবার confirm করা দরকার, কারণ এটা টাকা-সংক্রান্ত)।
4. **পুরনো row মোছা হবে না** — শুধু চিহ্নিত করা হবে migrated হিসেবে, যাতে rollback সম্ভব
   হয় (rule #৩, #৫)। এর জন্য **নতুন একটা field লাগতে পারে** যেমন
   `localDualRowArchived: Boolean = false` (প্রস্তাবিত নাম, ধাপ ৩-এ চূড়ান্ত সিদ্ধান্ত) —
   বিদ্যমান `legacyDualRoleMergeDoneAt` reuse করা হবে না কারণ সেটা অন্য (cloud-merge)
   অর্থে আগে থেকেই ব্যবহৃত (উপরে ১ নং সেকশনে নোট)। এই নতুন ফিল্ড schema migration
   (`AppDatabase.kt`-এ নতুন Room migration) চাইবে — ধাপ ৩-এ implement করার সময় এটা
   explicit করে দেখানো হবে, এখানে শুধু পরিকল্পনা।
5. **Cloud data touch হবে না** — এটা pure local Room merge, কোনো Supabase write না
   (rule #৫, destructive না)।

### ঝুঁকি/open প্রশ্ন (ধাপ ৩ শুরুর আগে confirm করা উচিত)

- balance conflict হলে কী করা হবে (উপরে ৩ নং পয়েন্ট) — এটা টাকা-সংক্রান্ত, তাই বিশেষ সতর্কতা।
- নতুন `localDualRowArchived` ফিল্ড যোগ করলে Room migration লাগবে — `AppDatabase.kt`-এর
  বর্তমান migration chain দেখে সঠিক পরবর্তী version number ঠিক করতে হবে (ধাপ ৩-এ)।

---

## ৫. সারসংক্ষেপ — ধাপ ২-এর জন্য প্রস্তুত স্কোপ

- নতুন ফাংশন: `SomadhanRepository.switchRoleInPlace()` (২ নং সেকশনের স্পেক অনুযায়ী)।
- কোনো নতুন schema field লাগছে না ধাপ ২-এ (শুধু ধাপ ৩-এর migration marker-এর জন্য পরে লাগতে
  পারে)।
- ক্যাটাগরি ১-এর ৫১টা guard-এর কোনোটাই এই ধাপে/ধাপ ৫-এ কোড-পরিবর্তন দাবি করে না বলে মনে
  হচ্ছে (৩ নং সেকশন) — ধাপ ৫ মূলত verification, rewrite না।
- Migration প্ল্যান (৪ নং সেকশন) ধাপ ৩-এ implement হবে, balance-conflict সিদ্ধান্তটা
  implement করার ঠিক আগে user confirm করে নেওয়া উচিত।

**এই ধাপে কোনো `.kt`/`.sql` ফাইল এডিট করা হয়নি।** পরবর্তী ধাপ (২) শুরুর আগে ব্যবহারকারীর
confirmation দরকার।
