# ROLE_UID_AUDIT.md — ধাপ ০: শুধু অডিট (কোনো কোড পরিবর্তন হয়নি)

তৈরি: এই সেশনে, `ROLE_UID_SYNC_FIX_MASTER_PROMPT.md`-এর ধাপ ০ অনুযায়ী।
**কোনো `.kt`/`.sql` ফাইল এডিট করা হয়নি — শুধু grep/পড়া হয়েছে।**

মাস্টার প্রম্পটে অনুমান করা হয়েছিল "৫১+ জায়গা" — audit করে দেখা গেছে **ঠিক ৫১টা**
`SomadhanRepository.kt`-এ পাওয়া গেছে (নিচে ক্যাটাগরি ১)। সংখ্যাটা মিলে যাওয়ায় স্কোপ ধারণা
সঠিক মনে হচ্ছে।

---

## ⚠️ গুরুত্বপূর্ণ প্রেক্ষাপট (কোড পড়ে পাওয়া, অনুমান না)

কোড ও কমেন্ট পড়ে দেখা গেছে এই একই বাগ **আগেই একবার শনাক্ত হয়েছিল** এবং ইচ্ছাকৃতভাবে ভবিষ্যতের
জন্য ফেলে রাখা হয়েছিল — এই master prompt সেই "ভবিষ্যৎ ধাপ"-টাই। `updateUser()`-এর ঠিক উপরের
comment (লাইন ৬৫৬-৬৬৪):

> "linked account sync loop (নিচে) Supabase-এ migrate করা হয়নি — কারণ Supabase RLS-এ users
> টেবিলে কোনো INSERT policy নেই... linked account গুলো ভিন্ন id-এর... বর্তমান session দিয়ে
> সেগুলো লেখার অনুমতিই নেই। এটা মূলত switchRole()-এর dual-row মডেলের সাথে জড়িত একই architecture
> সমস্যা... এই ধাপে সমাধান করা হয়নি, পরের ধাপের জন্য flag করা হলো।"

আরেকটা গুরুত্বপূর্ণ ব্যাপার: `switchRole()`-এর ভেতরেই (লাইন ৯৬০-১১০০) আগের একটা সেশনে (ধাপ
১৪.৫খ, ৩১ক, "UID ফিক্স", "ব্যালেন্স ফিক্স" কমেন্ট ট্যাগ অনুযায়ী) ইতিমধ্যে আংশিক কাজ হয়ে গেছে:

- root row-এর জন্য cloud RPC (`SupabaseSyncManager.switchRole()`) কল হয় — কিন্তু **শুধু তখনই
  যখন `currentUserId() == rootAccountId`**, অর্থাৎ linked (SOLVER_xxx/USER_xxx) row থেকে
  switchRole কল হলে এই sync স্কিপ হয় না (কারণ কোডটাই root id ব্যবহার করে) — এই একটা জায়গা এই
  বাগ থেকে **ইতিমধ্যেই মুক্ত**।
- `displayUid` এখন linked row-তেও root-এর আসল server-generated UID কপি হয় (তাই UI-তে UID
  same দেখায়), যদিও local Room `id` field-এ এখনও `SOLVER_xxx`/`USER_xxx` prefix থেকেই যায়।
- `mergeLegacyDualRoleDataFromCloud()` (লাইন ৫৬৫-৬৪৮, ধাপ ১৪.৫গ) প্রতি login-এ **cloud → local**
  দিকে (role-scoped balance/reputation/ban/restrict) root ও linked row দুটোই sync করে — কিন্তু
  এটা এই বাগের উল্টো দিক সমাধান করে (cloud থেকে টেনে আনা), **local → cloud** দিকটা (এই মাস্টার
  প্রম্পটের আসল টার্গেট) এখনও অসমাধিত।

**মানে:** master prompt-এর মূল premise সঠিক এবং আগে থেকেই কোডে স্বীকৃত/flag করা ছিল, কিন্তু
`switchRole()` নিজে এবং legacy-merge অংশ কিছুটা আগেই কাজ হয়ে গেছে — ধাপ ১ (ডিজাইন)-এ এটা
বিবেচনায় রাখা দরকার (পুরোপুরি নতুন করে ডিজাইন না করে, যা ইতিমধ্যে আছে সেটার উপর ভিত্তি করে)।

---

## ক্যাটাগরি ১ — `SupabaseAuthManager.currentUserId() ==` guard (৫১টা)

সব `app/src/main/java/com/example/data/repository/SomadhanRepository.kt`-এ। ফাংশন অনুযায়ী
গ্রুপ করা:

| লাইন | ফাংশন | কোড |
|---|---|---|
| 251, 264 | `resolveCommissionRateForNewJob` | `== solverId` |
| 670, 715 | `updateUser` | `== user.id` |
| 741 | `updateUserPassword` | `== userId` |
| 907 | `registerUser` | `== newUser.id` |
| 1081 | `switchRole` | `== rootAccountId` ✅ (আগে থেকেই root-ভিত্তিক, নিচে নোট দেখুন) |
| 1320 | `toggleFavoriteSolver` | `== userId` |
| 1342 | `addFavoriteSolver` | `== userId` |
| 1819, 1840 | `createProblem` | `== user.id` |
| 1905, 1935 | `createDirectContract` | `== user.id` |
| 2007 | `acceptDirectContractProposal` | `== solverId` |
| 2057 | `declineDirectContractProposal` | `== solverId` |
| 2203, 2232 | `placeBid` | `== solver.id` |
| 2432 | `acceptBid` | `== problem.userId` |
| 2530 | `withdrawBid` | `== bid.solverId` |
| 2561 | `requestJobRelease` | `== problem.acceptedSolverId` |
| 2660 | `cancelJobReleaseRequest` | `== problem.acceptedSolverId` |
| 2738 | `rejectJobReleaseRequest` | `== problem.userId` |
| 2802 | `raiseDispute` | `== userId` |
| 3006 | `withdrawDispute` | `== requesterId` |
| 3071 | `settleDispute` | `== userId` |
| 3139 | `requestAdminAssistance` | `== requesterId` |
| 4910, 4976 | `solverCancelJob` | `== solverId` |
| 5064 | `clearSolverCancelledNotice` | `== problem.userId` |
| 5620 | `markMessagesAsReadForProblem` | `== userId` |
| 5666 | `sendMessage` | `== senderId` |
| 5839 | `submitUserRatingForSolver` | `== problem.userId` |
| 5899 | `submitSolverRatingForUser` | `== solverId` |
| 5934 | `markAllNotificationsAsRead` | `== userId` |
| 5950 | `markNotificationAsRead` | `== notif.userId` |
| 6098, 6131 | `submitKyc` | `== user.id` |
| 6349 | `requestWithdrawal` | `== solver.id` |
| 6749 | `depositMoneyViaGateway` | `== userId` |
| 7773 | `requestAdditionalCharge` | `== solverId` |
| 8791 | `createInstantJob` | `== user.id` |
| 8858 | `updateInstantJobToggle` | `== userId` |
| 8931 | `acceptInstantJobBid` | `== updated.userId` |
| 8970 | `markSolverOnWay` | `== problem.acceptedSolverId` |
| 9027 | `markSolverArrived` | `== problem.acceptedSolverId` |
| 9086 | `markJobStarted` | `== problem.acceptedSolverId` |
| 9203 | `cancelInstantJob` | `== problem.userId` |
| 9284 | `requestExtraAmount` | `== problem.acceptedSolverId` |
| 9392 | `userConfirmExtraAmount` | `== freshProblem.userId` |
| 9442 | `userRejectExtraAmount` | `== problem.userId` |
| 9773 | `checkAndExpireInstantJobs` | `== problem.userId` |

**নোট:**
- এই সবগুলো "self" কেস মনে হচ্ছে (নিজের action নিজে sync করা), কোনোটাতেই স্পষ্টভাবে "admin
  অন্য কারো row edit করছে" প্যাটার্ন দেখা যায়নি — কিন্তু ধাপ ৫-এ প্রতিটা ব্যাচ ধরে verify করার
  সময় আরেকবার নিশ্চিত হওয়া দরকার (admin-সংক্রান্ত ফাংশনগুলো — `admin*` prefix — এই তালিকায়
  আসেনি, তাই সেই ঝুঁকি কম)।
- `switchRole` (লাইন ১০৮১) ইতিমধ্যে `rootAccountId` ব্যবহার করে, `user.id` না — তাই এটা সম্ভবত
  ধাপ ৫-এ "already correct" ক্যাটাগরিতে পড়বে, বাকি ৫০টা প্রকৃত টার্গেট।
- এছাড়া `SupabaseSyncManager.kt`-এর লাইন ১৯৫৭-এ এই প্যাটার্নের উল্লেখ পাওয়া গেছে কিন্তু সেটা
  **শুধু একটা comment**, আসল guard কোড না — গণনায় ধরা হয়নি।
- `SupabaseRealtimeManager.kt`-এর লাইন ১৭০০-এ `currentUserId()` আছে কিন্তু `==` guard না
  (broadcast filtering-এর জন্য variable assignment) — স্কোপের বাইরে।

---

## ক্যাটাগরি ২ — `"SOLVER_"` / `"USER_"` prefix দিয়ে id বানানো/চেক

**আসল id-generation সাইট মাত্র ১টা**, `SomadhanRepository.kt` লাইন ১০১২ (`switchRole()`
ফাংশনের ভেতর, "existing linked row নেই" branch-এ):

```kotlin
val newId = if (newRole == "SOLVER") "SOLVER_${UUID.randomUUID().toString().take(8)}" else "USER_${UUID.randomUUID().toString().take(8)}"
```

সংশ্লিষ্ট কমেন্ট (লাইন ৯৯৯-১০১২, ৭১৮৯) নিশ্চিত করে এটাই সেই দ্বৈত-row id, যেটা root
Supabase Auth UID না।

grep-এ আরও অনেক `"SOLVER_"`/`"USER_"` টেক্সট ম্যাচ পাওয়া গেছে, কিন্তু সেগুলো **id prefix
না** — এগুলো স্কোপের বাইরে (তালিকাভুক্ত শুধু রেফারেন্সের জন্য, কোনো পরিবর্তন লাগবে না):
- Transaction/refund `type`/`category` string ("SOLVER_CANCEL", "USER_CANCEL" ইত্যাদি) —
  `TransactionHelper.kt`, `SomadhanRepository.kt`-এর বিভিন্ন জায়গায়
- Job/bid status string ("SOLVER_ACCEPTED", "SOLVER_EN_ROUTE") — `JobTrackingScreen.kt`,
  `InstantJobsScreen.kt`, `ProblemEntity.kt`
- Admin audit `actionType` string ("SOLVER_CANCEL_JOB", "USER_DELETE_PROBLEM",
  "RESET_SOLVER_FREE_QUOTA" ইত্যাদি)
- Rating filter string ("USER_TO_SOLVER", "SOLVER_TO_USER") — `AdminRatingsView.kt`
- FAQ entity id ("FAQ_USER_01", "FAQ_SOLVER_01" ইত্যাদি) — `AppDatabase.kt`,
  `SomadhanRepository.kt`
- পুরনো (এখন মুছে ফেলা) demo seed id-এর কমেন্ট রেফারেন্স — `AppDatabase.kt` লাইন ৬৫৩-৬৫৪

`startsWith("SOLVER_")`/`startsWith("USER_")` স্টাইলের কোনো id-prefix **চেক** (অর্থাৎ কোথাও
`id.startsWith(...)` দিয়ে dual-row কিনা যাচাই করা) — পুরো main source set-এ **পাওয়া যায়নি**।
তার মানে বর্তমান কোড কোথাও সরাসরি id-এর prefix দেখে আচরণ বদলায় না — বাগটা প্রকাশ পায় শুধু
মান-তুলনায় (`currentUserId() == user.id` false হয়ে যাওয়ার মাধ্যমে), prefix-চেক নিজে থেকে না।

---

## ক্যাটাগরি ৩ — `linkedAccountId` read/write

মোট ২২টা occurrence:

| ফাইল | লাইন | প্রসঙ্গ |
|---|---|---|
| `data/entity/UserEntity.kt` | 13, 51 | schema: `Index("linkedAccountId")`, ফিল্ড ডেফিনিশন `val linkedAccountId: String? = null` |
| `data/remote/dto/UserDto.kt` | 49 | DTO: `linked_account_id` কলাম ম্যাপিং (`uuid, self-FK` কমেন্টসহ) |
| `data/remote/UserMappers.kt` | 32, 98 | DTO ↔ Entity ম্যাপিং + কমেন্ট ("ধাপ ১৪-এ migrate...") |
| `data/dao/AppDaos.kt` | 53, 59 | `getLinkedUserByRole` কোয়েরি, `getLinkedAccounts` কোয়েরি (নিচে ক্যাটাগরি ৪) |
| `data/database/AppDatabase.kt` | 330 | migration: index তৈরি |
| `data/repository/SomadhanRepository.kt` | 687, 706, 766, 961, 963, 965, 993, 1008, 1062, 7195 | read (`rootAccountId = user.linkedAccountId ?: user.id` প্যাটার্ন — ৪ বার পুনরাবৃত্ত) + write (`linkedAccountId = rootAccountId` — নতুন/আপডেট করা row-তে) |
| `data/repository/SomadhanRepository.kt` | 6462 | fallback lookup: `it.linkedAccountId == solverId` |
| `ui/screens/AdminSolverQuotaView.kt` | 354 | admin-সাইড matching: `it.id == user.linkedAccountId \|\| it.linkedAccountId == user.id` |

**নোট:** `user.linkedAccountId?.takeIf { it.isNotBlank() } ?: user.id` প্যাটার্নটা ৪ জায়গায়
(৬৮৭, ৭৬৬, ৯৬১, ৭১৯৫) হুবহু কপি-পেস্ট হয়ে আছে — root id বের করার এই লজিক ডিজাইন ধাপে একটা
common helper হিসেবে বিবেচনা করা যেতে পারে (এখনই বদলানো হচ্ছে না, শুধু পর্যবেক্ষণ)।

---

## ক্যাটাগরি ৪ — `getLinkedUserByRole` / `getUserByContactAndRole` কল

| ফাইল | লাইন | প্রসঙ্গ |
|---|---|---|
| `data/dao/AppDaos.kt` | 54 | DAO ডেফিনিশন: `getLinkedUserByRole(linkedId, role)` — query: `linkedAccountId = :linkedId OR id = :linkedId` |
| `data/dao/AppDaos.kt` | 57 | DAO ডেফিনিশন: `getUserByContactAndRole(phone, email, role)` |
| `data/dao/AppDaos.kt` | 59 | সংশ্লিষ্ট: `getLinkedAccounts(linkedId)` — সব role-এর linked row |
| `data/repository/SomadhanRepository.kt` | 767-768 | `hasExistingSolverProfile()` — SOLVER role-এর linked/contact-matched row খোঁজে |
| `data/repository/SomadhanRepository.kt` | 973-974 | `switchRole()` — target role-এর জন্য existing linked row খোঁজে (থাকলে reuse, না থাকলে নতুন বানায়) |

মাত্র এই ২টা call site — ছোট, ধাপ ২-৪ এ wiring করার সময় manageable।

---

## ক্যাটাগরি ৫ — `user.id`/`currentUser.id`/`solver.id` foreign key হিসেবে ব্যবহার

`SomadhanRepository.kt`-এ যেসব ফাংশন নতুন entity তৈরির সময় সরাসরি বর্তমান role-row-এর `.id`
FK হিসেবে বসায় (মানে role switch-এর পর যদি `user.id`/`solver.id` root UID না হয়ে
SOLVER_xxx/USER_xxx হয়ে যায়, তাহলে সেই FK-ও ভুল/অসামঞ্জস্যপূর্ণ id বহন করবে):

| ফাংশন | লাইন | কোন FK-এ বসছে |
|---|---|---|
| `updateUser` | 672 | notification `userId = user.id` |
| `updateUser` (linked-list ফিল্টার) | 688, 690 | `it.id != user.id` (data leak-প্রতিরোধী ফিল্টার, FK না কিন্তু id-নির্ভর) |
| (ফাংশন — আশেপাশের কোড দেখে নাম যাচাই করা দরকার, লাইন ১২৫২-১২৬৮ এলাকা) | 1252, 1256, 1268 | `userId`/`targetId`/`targetUserId = user.id` |
| `createProblem` | 1796, 1827 | problem/notification `userId = user.id` |
| `createProblem` (self-notify) | 1842 | `targetUserId = user.id` |
| `createDirectContract` | 1875, 1891, 1913, 1914, 1924, 1937 | contract `userId`/`acceptedSolverId = user.id`, `receiverId`/`senderId`/`userId`/`targetUserId = solver.id` |
| `placeBid` | 2188 | bid `solverId = solver.id` |
| `submitKyc` | 6100, 6116, 6120, 6133, 6137 | `userId`/`targetId`/`targetUserId = user.id` |
| `requestWithdrawal` | 6377, 6404, 6419, 6423 | withdrawal/notification `solverId`/`userId`/`targetId = solver.id` |
| (আশেপাশের কোড — লাইন ৪২৪৭, ৪২৯০, ৪৩০৪ এলাকা) | 4247, 4290, 4304 | `userId`/`targetId = user.id` |
| `createInstantJob` | 8760 | `userId = user.id` |
| (লাইন ৯৮০৭ এলাকা) | 9807 | `userId = user.id` |

**পর্যবেক্ষণ:** এই ক্যাটাগরির প্রায় প্রতিটা সাইটই ক্যাটাগরি ১-এর একই ফাংশনের ভেতরে বা তার
কাছাকাছি — অর্থাৎ যে ফাংশনে cloud-sync guard আছে, সেই একই ফাংশনে সাধারণত FK-ও বসছে। তাই ধাপ
৪-এর "role switch-এর পরেও `currentUser.id` অভিন্ন থাকা" ফিক্সটা ঠিকমতো হলে এই FK-গুলোও
স্বয়ংক্রিয়ভাবে সঠিক হয়ে যাবে বলে মনে হচ্ছে — আলাদা করে এই সাইটগুলো টাচ করা লাগার কথা না। কিছু
লাইন নাম্বারের ফাংশন-নাম নিশ্চিত করার জন্য directly view করা দরকার (উপরে "যাচাই করা দরকার"
চিহ্নিত করা হয়েছে) — এখনই অনুমান করে ফাংশনের নাম বসানো হয়নি।

`problem.userId`, `bid.solverId`, `transaction.userId`/`solverId`, `notif.userId` — এগুলো
**পড়ার সময়** (ক্যাটাগরি ১-এর guard-এর ডানপাশে) ব্যবহার হচ্ছে, সেগুলো ইতিমধ্যে ক্যাটাগরি ১-এর
তালিকায় আছে, এখানে আবার তোলা হয়নি।

---

## পরবর্তী ধাপের জন্য open প্রশ্ন (কোনো কোড বদলানো ছাড়াই, শুধু নোট)

1. ধাপ ১ (ডিজাইন)-এ `switchRole()`-এর বিদ্যমান আংশিক কাজ (root-ভিত্তিক RPC কল, ইতিমধ্যে ঠিক)
   এবং `mergeLegacyDualRoleDataFromCloud()` (cloud→local দিক, ইতিমধ্যে ঠিক)-কে নতুন ডিজাইনের
   ভিত্তি হিসেবে ধরা উচিত কিনা, নাকি সম্পূর্ণ প্রতিস্থাপন করা হবে — এটা তোমার সিদ্ধান্ত।
2. ক্যাটাগরি ৫-এর ৩টা লাইন-এলাকার (১২৫২, ৪২৪৭, ৯৮০৭ আশেপাশে) সঠিক ফাংশন নাম এখনো নিশ্চিত করা
   হয়নি (audit-এর সময় শুধু grep দিয়ে বের করা হয়েছে, প্রতিটা লাইন context ধরে verify করিনি) —
   ধাপ ৫-এ ব্যাচ ভাগ করার সময় প্রয়োজনে সেগুলো নির্দিষ্টভাবে দেখে নেওয়া হবে।
