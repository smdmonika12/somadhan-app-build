# ROLE_SEPARATION_AUDIT.md
## ধাপ ০ — শুধু অডিট (কোনো কোড বদলানো হয়নি)

স্কোপ: `app/src/main/java` (main source set)। নিচের প্রতিটা ক্যাটাগরি `BALANCE_REPUTATION_ROLE_SEPARATION_MASTER_PROMPT.md`-এর ধাপ ০-এর ৭টা আইটেম অনুসরণ করে।

---

## ১. `TransactionEntity(` তৈরি হয় এমন প্রতিটা জায়গা

`SomadhanRepository.kt`-এ ১৪টা সাইট + `MessageTransactionMappers.kt`-এ ১টা DTO→Entity ম্যাপার (cloud থেকে আসা রো)। `TransactionEntity` ক্লাসে (`MarketEntities.kt:117`) এখন কোনো `role` ফিল্ড নেই — কনফার্মড।

| # | ফাইল:লাইন | ফাংশন | userId/solverId কীভাবে সেট হয় | রোল নির্ধারণ |
|---|---|---|---|---|
| 1 | SomadhanRepository.kt:415 | `payoutEscrowToSolver()` | `solverId` = payout পাওয়া solver, `userId` = job poster | **SOLVER** (solver-এর earning; default `type="PAYMENT"`) |
| 2 | SomadhanRepository.kt:2624 | bid accept-এর wallet deduction | `userId`=poster, `solverId=""` (ইচ্ছাকৃতভাবে ফাঁকা — কমেন্টে ব্যাখ্যা আছে, নইলে solver-এর earning history-তে ভুলভাবে দেখাবে) | **USER** (`BID_ACCEPT_DEDUCTION`) |
| 3 | SomadhanRepository.kt:3783 | dispute split settlement, solver-এর অংশ | `solverId`=solverId, type=`PAYMENT`, `releaseType="SPLIT_RELEASE"` | **SOLVER** |
| 4 | SomadhanRepository.kt:4275 | `refundEscrowOnce()` | `userId`=poster (`userDao.addBalanceForUserRole` উপরেই কল হয়েছে) | **USER** (`REFUND`) |
| 5 | SomadhanRepository.kt:4587 | `reconcileUserBalances(dryRun=false)`-এর correction | role আগেই স্থানীয় ভ্যারিয়েবল `correctionRole` (`user.role` থেকে) হিসেবে গণনা হয়ে DAO কলে ব্যবহৃত হচ্ছে (৪৫৭৬-৪৫৮৪ লাইন) | **AMBIGUOUS/DYNAMIC** — role ইতিমধ্যে জানা আছে (`correctionRole`) কিন্তু `TransactionEntity`-তে বসছে না, কারণ ফিল্ডই নেই |
| 6 | SomadhanRepository.kt:4784 | `cleanupDuplicateRefunds()`-এর ডুপ্লিকেট সংশোধন | `userId`=dup.userId (পোস্টার), balanceUser mirror deduct হয় | **USER** |
| 7 | SomadhanRepository.kt:4948 | `repairMissingRefunds()` | `userId`=escrow.userId (পোস্টার) | **USER** (`REFUND`) |
| 8 | SomadhanRepository.kt:5591 | job release-এর সময় অতিরিক্ত বিল deduction | `userId`=problem.userId, `solverId=""` | **USER** (`RELEASE_DEDUCTION`) |
| 9 | SomadhanRepository.kt:6718 | solver withdrawal request deduction | `userId`=solver.id (ভেরিয়েবল নাম বিভ্রান্তিকর — আসলে solver), balanceSolver mirror deduct হয় | **SOLVER** (`WITHDRAWAL_DEDUCTION`) |
| 10 | SomadhanRepository.kt:6806 | withdrawal reject হলে রিফান্ড | `effectiveUserId`, balanceSolver mirror-এ credit হয় | **SOLVER** (`WITHDRAWAL_REFUND`) |
| 11 | SomadhanRepository.kt:7101 | `depositMoneyViaGateway()` | role আগেই স্থানীয় ভ্যারিয়েবল `depositRole` (`user.role` থেকে, ৭০৮১ লাইন) হিসেবে গণনা হয়ে cloud RPC-তে পাস হচ্ছে | **AMBIGUOUS/DYNAMIC** — role জানা আছে (`depositRole`) কিন্তু local `TransactionEntity`-তে সংরক্ষণ হচ্ছে না |
| 12 | SomadhanRepository.kt:7410 | `adminAdjustBalance()` | role আগেই স্থানীয় ভ্যারিয়েবল `adjustRole` (target user-এর `.role` থেকে, ৭৪০০ লাইন) হিসেবে গণনা হয়ে DAO কলে ব্যবহৃত হচ্ছে | **AMBIGUOUS/DYNAMIC** — একই প্যাটার্ন |
| 13 | SomadhanRepository.kt:8250 | `respondToAdditionalCharge()`-এর wallet deduction | `userId`=charge.userId (পোস্টার), `solverId=""` | **USER** (`EXTRA_CHARGE_DEDUCTION`) |
| 14 | SomadhanRepository.kt:9747 | `confirmExtraAmount()`-এর deduction | `userId`=freshProblem.userId, `solverId=""` | **USER** (`EXTRA_CHARGE_DEDUCTION`) |
| 15 | MessageTransactionMappers.kt:92 | `TransactionDto.toTransactionEntity()` — cloud থেকে realtime/pull-এ আসা রো-কে local entity-তে ম্যাপ করে | `TransactionDto`-তেও (dto/TransactionDto.kt) কোনো role কলাম নেই | **AMBIGUOUS** — যতক্ষণ না Supabase `transactions` টেবিলে role কলাম যোগ হয় এবং DTO-তে আসে, ততক্ষণ cloud-origin রো-এর role স্থানীয়ভাবে recover করা অসম্ভব |

**সারসংক্ষেপ:** ১৪টার মধ্যে ৯টা সাইটে (২,৩,৪,৬,৭,৮,৯,১০,১৩,১৪ — মোট ১০টা প্রকৃতপক্ষে) role নিঃসন্দেহে call-site প্যাটার্ন (deduction/refund/payment কার কাছে যাচ্ছে) থেকে বোঝা যায়। ৩টা সাইটে (৫, ১১, ১২) role **ইতিমধ্যেই একটা লোকাল ভ্যারিয়েবলে গণনা করা আছে** (`correctionRole`/`depositRole`/`adjustRole`) — এগুলো `TransactionEntity`-তে role ফিল্ড যোগ হলে trivially পাস করা যাবে, নতুন লজিক লাগবে না। সাইট ১ (payout)-এ role আলাদা ভ্যারিয়েবলে নেই কিন্তু ফাংশনের নাম/সিগনেচার থেকেই স্পষ্ট SOLVER। সাইট ১৫ (DTO mapper) সত্যিকারের ambiguous — এটা Supabase-সাইড কলাম যোগ না হলে সমাধান হবে না।

---

## ২. `reputationScore` (shared, role-suffix ছাড়া) সরাসরি read/write

### Write (assignment) সাইট — repository-তে

| ফাইল:লাইন | ফাংশন | সমস্যা কিনা |
|---|---|---|
| SomadhanRepository.kt:616, 630 | `mergeLegacyDualRoleDataFromCloud()` | **সমস্যা না** — `roleScoped(role)` হেল্পার দিয়ে **সংশ্লিষ্ট রো-এর নিজের role** অনুযায়ী সঠিক cloud role-scoped মান (`reputationScoreUser`/`reputationScoreSolver`) থেকে plain কলাম বসানো হয়। ইচ্ছাকৃত "active mirror" — role-aware। |
| SomadhanRepository.kt:1157 | `switchRole()` (পুরনো dual-row ভ্যারিয়েন্ট) | **সমস্যা না** — `newRole` অনুযায়ী cloud RPC রেসপন্স থেকে সঠিক scoped মান (`activeRep`) বেছে plain কলামে বসায়। |
| SomadhanRepository.kt:1216 | `switchRoleInPlace()` (নতুন single-row ভ্যারিয়েন্ট) | **সমস্যা না** — একই প্যাটার্ন, local root row থেকেই `activeRep` বের করে। |
| SomadhanRepository.kt:1261 | `switchRoleInPlace()`-এর cloud RPC callback | **সমস্যা না** — cloud RPC রেসপন্স থেকে `cloudActiveRep` বের করে plain কলাম sync করে। |
| **SomadhanRepository.kt:8600** | **`applyReputationChange()`** | **⚠️ সমস্যা — এটাই master prompt-এ চিহ্নিত মূল বাগ।** `user.copy(reputationScore = newScore)` শুধু plain কলাম লেখে; `reputationScoreUser`/`reputationScoreSolver`-এর জন্য কোনো role-aware write নেই (এই তিনটা উপরের ফাংশনের মতো `user.role` চেক করে সঠিক scoped ফিল্ডও বসায় না)। |

তাহলে repository-লেয়ারে সমস্যা **একটাই জায়গায়** (৮৬০০) — বাকি ৪টা write-site ইতিমধ্যেই role-aware প্যাটার্ন মেনে চলছে (ধাপ ১-এ যা নতুন করে বানানো হবে, এই ৪টাই তার রেফারেন্স প্যাটার্ন)।

### Read সাইট — UI স্ক্রিন

| ফাইল:লাইন | কী দেখায় | সমস্যা কিনা |
|---|---|---|
| CommonComponents.kt:231 | `ReputationBadge(score = currentUser.reputationScore)` | বর্তমান active role-এর reputation দেখানোর কথা — যেহেতু `reputationScore` (plain) সবসময় switchRole/switchRoleInPlace-এ active role অনুযায়ী sync হয় (উপরের টেবিল দেখুন), **আপাতত সঠিক থাকার কথা**, তবে এটা `applyReputationChange()`-এর বাগের কারণে ঘটনা ঘটার মুহূর্তেই stale হয়ে যেতে পারে (role switch না করা পর্যন্ত)। |
| ProfileScreen.kt:757, 984 | নিজের প্রোফাইলে স্কোর | same as above |
| ReputationDetailScreen.kt:178-179, 636, 724 | নিজের reputation detail/quota eligibility | same as above — **প্রভাবিত**: quota eligibility (৬৩৬) ভুল role-এর stale স্কোর দিয়ে হিসাব হতে পারে ঘটনার পরপরই |
| FavoriteSolversScreen.kt:571, PublicProfileScreen.kt:445, JobTrackingScreen.kt (৪টা লাইন), InstantJobHistoryScreen.kt (২টা লাইন) | **অন্য** ইউজার/সলভারের স্কোর দেখানো (নিজের না) | এগুলো `otherUser?.reputationScore`/`solver.reputationScore` — যেহেতু সেই অন্য ইউজারের row তার নিজের সর্বশেষ role-switch অনুযায়ী sync থাকে, ধারণাগতভাবে ঠিক আছে যদি না সেই ইউজারের সাম্প্রতিক reputation event ঘটেছে আর সে role switch করেনি — **applyReputationChange বাগের downstream প্রভাব**, আলাদা কোনো নতুন সমস্যা না |
| AdminProblemsView.kt:1543, AdminSolverQuotaView.kt:609,698, AdminStatsView.kt:1541,1588,1700, AdminUserLookupView.kt:643,1232,1235, AdminUsersView.kt:371,798 | Admin panel-এ ইউজারের স্কোর/র‍্যাংকিং | এগুলোও plain কলাম পড়ে — **role-scoped তথ্য admin-এর প্রয়োজন হতে পারে** (কোন role-এর স্কোর দেখাচ্ছে তা UI-তে অস্পষ্ট), কিন্তু এটা ধাপ ৭-এর স্কোপ (generic balance-এর মতোই generic reputation ডিসপ্লে সমস্যা — নতুন করে flag করা হলো, নিচে নোট দ্রষ্টব্য) |

**নতুন পাওয়া গেছে (master prompt-এ উল্লেখ ছিল না):** balance-এর মতোই, admin screen-গুলোতে (`AdminStatsView`, `AdminSolverQuotaView`, `AdminUserLookupView`, `AdminUsersView`, `AdminProblemsView`) generic `reputationScore` দেখানো হচ্ছে — dual-role ইউজারের ক্ষেত্রে কোন role-এর স্কোর সেটা UI-তে স্পষ্ট না। এটা ধাপ ৭-এর (generic balance) সমান্তরাল সমস্যা, নিচে "নতুন পাওয়া সমস্যা" সেকশনে আলাদা flag করা হলো।

---

## ৩. `user.balance`/`target.balance` (generic, role-suffix ছাড়া) UI-তে

| ফাইল:লাইন | প্রেক্ষাপট |
|---|---|
| AdminUsersView.kt:596 | balance-adjust dialog header-এ "বর্তমান ব্যালেন্স" |
| AdminUserLookupView.kt:471 | ইউজার lookup ডিটেইল কার্ডে "বর্তমান ব্যালেন্স" |
| AdminStatsView.kt:1701 | ইউজার এক্সপোর্ট/টেবিলে balance কলাম |

আগের সেশনের flag কনফার্মড — এই তিনটাই এখনো generic `balance` (role-suffix ছাড়া) সরাসরি পড়ছে, `balanceUser`/`balanceSolver` না। এই তিনটা ধাপ ৭-এর মূল স্কোপ। (DAO-লেয়ার এই সেশনে আগেই ফিক্স হয়েছে — এটা শুধু UI display-লেয়ারের গ্যাপ, master prompt-এ যেমন বলা হয়েছে।)

---

## ৪. অন্য role-scoped column-pair — local write path shared কলাম-নির্ভর কিনা

`UserEntity.kt` আর `AppDaos.kt` (UserDao) পাশাপাশি মিলিয়ে:

| column pair | role-scoped DAO ফাংশন আছে কিনা |
|---|---|
| `balanceUser`/`balanceSolver` | ✅ আছে — `addBalanceForUserRole`/`addBalanceForSolverRole`/`deductBalanceForUserRole`/`deductBalanceForSolverRole` |
| `isBannedUser`/`isBannedSolver` | ✅ আছে — `setBannedStatusForUserRole`/`setBannedStatusForSolverRole` |
| `isRestrictedUser`/`isRestrictedSolver` | ✅ আছে — `setRestrictedStatusForUserRole`/`setRestrictedStatusForSolverRole` |
| `verifiedBadgeUser`/`verifiedBadgeSolver` | ✅ আছে — `setVerifiedBadgeForUserRole`/`setVerifiedBadgeForSolverRole` |
| **`reputationScoreUser`/`reputationScoreSolver`** | **❌ নেই** — কোনো `updateReputationForUserRole`/`updateReputationForSolverRole` ফাংশন `UserDao`-তে নেই |

**নিশ্চিত করা হলো: শুধু reputation-এরই role-scoped DAO ফাংশন অনুপস্থিত।** বাকি সব pair-এর জন্য patterns আগে থেকেই আছে, ধাপ ১-এ ঠিক এই প্যাটার্ন কপি করা যাবে।

---

## ৫. `reconcileUserBalances()`-এর মতো "ledger থেকে recompute করে সংশোধন" ফাংশন

| ফাইল:লাইন | ফাংশন | role-mixing সমস্যা |
|---|---|---|
| SomadhanRepository.kt:4496 | `reconcileUserBalances(dryRun)` | **⚠️ কনফার্মড বাগ (master prompt-এ যা বলা আছে)।** `ledgerByUser` HashMap `trx.userId`/`trx.solverId` (role-নিরপেক্ষ id) দিয়ে key করা — role ফিল্ড না থাকায় একই ইউজারের User-role আর Solver-role transaction (উভয়েরই id একই root/dual-row id হতে পারে প্রেক্ষাপট অনুযায়ী) একই bucket-এ যোগ হয়, তারপর `user.balance - ledgerSum` (একক plain কলাম) দিয়ে mismatch ধরা হয়। |
| SomadhanRepository.kt:4340 | `reconcileEscrowStates()` | role-নিরপেক্ষ — escrow status নিয়ে কাজ করে, balance/reputation-এর মতো role-dependent সংখ্যা টাচ করে না বলে মনে হচ্ছে; **যাচাই দরকার হলে আলাদা ছোট অডিট আইটেম** (এই ধাপের স্কোপের বাইরে, শুধু নোট রাখা হলো) |
| SomadhanRepository.kt:4652 | `maybeAutoReconcileBalances()` | শুধু উপরের `reconcileUserBalances()`-কে wrap/trigger করে — একই বাগ inherit করে, আলাদা কোনো নতুন লজিক নেই |

**Reputation-এর জন্য "recompute from events" ফাংশন খোঁজা হলো — কোনোটা পাওয়া যায়নি।** `ReputationEventEntity`/`reputationEventDao` টেবিল থেকে reputationScore পুনর্গণনা করে এমন কোনো ফাংশন নেই (শুধু forward-apply আছে, `applyReputationChange()`-এর মাধ্যমে; কোনো audit/reconcile পাথ নেই)। তাই এই মুহূর্তে reputation-এর জন্য role-mixing-এর কোনো "reconcile" বাগ **নেই**, কারণ reconcile ফাংশনটাই অনুপস্থিত — এটা বাগ না, স্কোপ-বহির্ভূত অনুপস্থিত ফিচার হিসেবে নোট করা হলো (master prompt চাইলে future ধাপে যোগ করা যায়, কিন্তু এই মাস্টার প্রম্পটের ঘোষিত স্কোপে নেই)।

---

## ৬. `NotificationEntity(` তৈরি হয় এমন প্রতিটা জায়গা — প্রাথমিক তালিকা

`SomadhanRepository.kt`-এ মোট **৮২টা** creation site। `NotificationEntity`-তে বর্তমানে কোনো `role` ফিল্ড নেই, `NotificationDao.getNotificationsForUser(userId)` শুধু `userId` দিয়ে ফিল্টার করে — কনফার্মড (master prompt-এর দাবি অনুযায়ী)।

ভ্যারিয়েবল-নামের প্যাটার্ন দিয়ে প্রাথমিক গ্রুপিং (প্রতিটা লাইনের পূর্ণ context ধাপ ৬-এ যাচাই হবে, এখানে শুধু একটা দ্রুত ইনভেন্টরি):

| গ্রুপ | লাইনসংখ্যা | লাইন নম্বরগুলো | প্রাথমিক ধারণা |
|---|---|---|---|
| `userNotif`/`notifUser`/`notifClient` (স্পষ্টত User-role-কেন্দ্রিক) | ১৮ | 2288, 2340, 3173, 3511, 5447, 5815, 7907, 8023, 8936, 9035, 9371, 9417, 9475, 9589, 9679, 9992, 10046, 10086, 10169 | **role = "USER"** (যাচাই সাপেক্ষে) |
| `solverNotif`/`notifSolver` (স্পষ্টত Solver-role-কেন্দ্রিক) | ১৯ | 3185, 3541, 5460, 5803, 7896, 8034, 8948, 9063, 9429, 9487, 9601, 9784, 9837, 10005, 10059, 10098, 10155 | **role = "SOLVER"** (যাচাই সাপেক্ষে) |
| `roleNotif`/`roleNotifInPlace` (role-switch confirmation, `resultUser.id`/root-এর নিজের বর্তমান role-এর জন্য) | ২ | 1170, 1275 | role dynamic, ওই মুহূর্তের active role থেকে জানা যায় |
| `adminNotif` (admin থেকে পাঠানো সাধারণ বার্তা) | ১ | 3421 | সম্ভবত role-neutral, verify করতে হবে |
| `welcomeNotif` (নতুন অ্যাকাউন্ট স্বাগত বার্তা) | ১ | 932 | role-neutral হওয়ার কথা (একাউন্ট লেভেলে, কোনো নির্দিষ্ট role-এর ঘটনা না) — verify করতে হবে |
| জেনেরিক নাম (`notif`) — context-নির্ভর, প্রতিটা আলাদা করে দেখতে হবে | ২৯ | 1547, 2219, 2404, 2800, 2908, 2974, 3060, 3115, 3283, 3356, 4066, 4686, 5260, 6469, 6526, 6556, 7131, 7265, 7309, 7453, 7480, 7515, 7580, 8156, 8287, 9251 | mixed — ধাপ ৬-এ এক এক করে function-context দেখে role নির্ধারণ করতে হবে |
| bare-call (কোনো var name নেই, সরাসরি `notificationDao.insertNotification(NotificationEntity(...))`) | ১২ | 2122, 2513, 3864, 3877, 3956, 3980, 6275, 6429, 6735, 6849, 6902, 7614, 7656, 7667, 7722, 7782 | mixed — একইভাবে ধাপ ৬-এ যাচাই দরকার |

**গুরুত্বপূর্ণ পর্যবেক্ষণ:** ৩৭টা সাইটে (`userNotif`+`solverNotif` গ্রুপ) ভ্যারিয়েবল নাম থেকেই role স্পষ্ট বোঝা যাচ্ছে — এগুলো ধাপ ৬-এ সরাসরি ম্যাপ করা যাবে। বাকি ~৪৫টা সাইট (bare-call + generic `notif`) প্রতিটার জন্য আশেপাশের ফাংশন-কন্টেক্সট পড়ে সিদ্ধান্ত নিতে হবে — এই অডিট ধাপে পুরো লিস্ট এক এক করে classify করা হয়নি (সময়-স্কোপ বাঁচাতে), ধাপ ৬-এর কাজের অংশ হিসেবেই এটা যথাযথ (master prompt নিজেই ধাপ ৬-কে "একই প্যাটার্নে, ambiguous হলে জিজ্ঞেস করবে" বলে ডিজাইন করেছে)।

---

## ৭. `AdminAuditLogEntity`/`logAdminAction()` কল সাইট

`AdminAuditLogEntity` (AdminAuditLogEntity.kt:7) — কনফার্মড, কোনো `role` ফিল্ড নেই। `logAdminAction()` মোট **৭১বার** কল হয়েছে SomadhanRepository.kt-এ।

`actionType` অনুযায়ী classification:

| Role-scoped actionType (caller-এর কাছে role ইতিমধ্যে জানা/গণনাযোগ্য) | লাইন | role কীভাবে জানা |
|---|---|---|
| `ADD_BALANCE`/`DEDUCT_BALANCE` | 7464 | `adjustRole` ভ্যারিয়েবল (৭৪০০-এ গণনা করা, উপরে #১ আইটেম ১২ দ্রষ্টব্য) — **ইতিমধ্যেই লোকালি জানা** |
| `BAN_USER`/`UNBAN_USER` | 7287 | caller (`setBannedStatusForUserRole`/`SolverRole` কল হয় ঠিক এর আশেপাশে) থেকে role জানা যায় — যাচাই দরকার এই কলের কাছে সরাসরি role ভ্যারিয়েবল আছে কিনা |
| `RESTRICT_USER`/`UNRESTRICT_USER` | 7328 | একই প্যাটার্ন |
| `ADD_VERIFIED_BADGE`/`REMOVE_VERIFIED_BADGE` | 7366 | একই প্যাটার্ন |
| `ADJUST_REPUTATION` | 7569 | `adminAdjustReputation()`-এর ভেতরে — role caller-এর কাছে জানা থাকার কথা (verify করতে হবে ঠিক কোন ভ্যারিয়েবলে) |

Role-নিরপেক্ষ (সাধারণ system/content action, role লাগে না বলে মনে হচ্ছে): `DELETE_USER`, `UPDATE_SETTING`, `ADD_CATEGORY`/`UPDATE_CATEGORY`/`DELETE_CATEGORY`/`ENABLE_CATEGORY`/`DISABLE_CATEGORY`, `DELETE_PROBLEM`, `DELETE_MESSAGE`, `DELETE_RATING`, `DELETE_NOTIFICATION`, `SEND_NOTIFICATION`, `APPROVE_KYC`/`REJECT_KYC`/`REVOKE_KYC`/`UPDATE_KYC_INFO`/`RESET_KYC_PENDING` (KYC নিজেই role-নিরপেক্ষ একটা প্রোফাইল-লেভেল স্ট্যাটাস, dual-row role-scoped কলামের অংশ না — **তবে এটা একটা assumption, master prompt-এর মূল ডিজাইন প্রিন্সিপলে KYC role-scoped কিনা স্পষ্ট বলা নেই, তাই confirm করা দরকার**), `RESET_PASSWORD`, `CHANGE_ROLE`, `FACTORY_RESET`, ইত্যাদি — এগুলোতে role-tagging প্রযোজ্য নয় বলে মনে হচ্ছে।

Escrow/withdrawal/দিspute-সম্পর্কিত action (`RELEASE_ESCROW`, `REFUND_ESCROW`, `REJECT_WITHDRAWAL`, `COMPLETE_WITHDRAWAL`, `ADMIN_RESOLVE_DISPUTE` ইত্যাদি) — এগুলো সবসময় নির্দিষ্ট escrow/problem-এর সাথে জড়িত, তাই role টেকনিক্যালি "SOLVER-side money movement" হলেও, `targetId`/`details`-এ escrow/problem id থাকায় role indirect ভাবে বোঝা যায় — সরাসরি role ট্যাগ না থাকাটা কম গুরুত্বপূর্ণ (master prompt "অংশ খ"-কে "কম অগ্রাধিকার" বলেছে, এই পর্যবেক্ষণ তার সাথে সামঞ্জস্যপূর্ণ)।

---

## নতুন পাওয়া সমস্যা (master prompt-এ যা উল্লেখ ছিল না)

1. **Admin panel-এ generic `reputationScore` ডিসপ্লে** (আইটেম ২-এর টেবিল দ্রষ্টব্য) — `AdminStatsView`, `AdminSolverQuotaView`, `AdminUserLookupView`, `AdminUsersView`, `AdminProblemsView`-তে ৯টা জায়গায়। এটা ধাপ ৭-এ বর্তমানে শুধু "generic balance" নিয়ে বলা আছে — একই সমস্যা reputation-এর জন্যও আছে। **সুপারিশ:** ধাপ ৭-এর স্কোপ generic reputation display পর্যন্ত সম্প্রসারণ করা বিবেচনা করুন, অথবা আলাদা ধাপ ৭খ হিসেবে যোগ করুন।
2. **৩টা `TransactionEntity` সাইটে (আইটেম ১-এর #৫,#১১,#১২) role ইতিমধ্যেই একটা লোকাল ভ্যারিয়েবলে (`correctionRole`/`depositRole`/`adjustRole`) গণনা করা আছে** — এটা ধাপ ২-৩-এর কাজ সহজ করে দেয় (নতুন role-নির্ণয় লজিক লাগবে না, শুধু বিদ্যমান ভ্যারিয়েবল পাস করলেই হবে), কিন্তু মূল master prompt-এ এই পর্যবেক্ষণ স্পষ্ট করে বলা ছিল না।
3. **`AdminAuditLogEntity`-তে KYC action-গুলো role-scoped কিনা তা ডিজাইন-স্তরে স্পষ্ট না** (আইটেম ৭ দ্রষ্টব্য) — একজন dual-role ইউজারের KYC কি User/Solver উভয় role-এর জন্য একই, নাকি role-ভিত্তিক আলাদা? এটা মূলত schema/business-rule প্রশ্ন, `UserEntity`-তে KYC ফিল্ডগুলো (`isKycVerified`, `kycStatus` ইত্যাদি) role-suffix ছাড়া একক — তাই সম্ভবত role-neutral বাই-ডিজাইন, কিন্তু নিশ্চিত করার জন্য ব্যবহারকারীর কনফার্মেশন দরকার।

---

## পরবর্তী পদক্ষেপ

এই রিপোর্ট অনুযায়ী মূল master prompt-এর ধাপ ১-৮ যথেষ্ট মনে হচ্ছে; উপরের ৩টা "নতুন পাওয়া সমস্যা" নিয়ে সিদ্ধান্ত নিলে ভালো হয় (বিশেষ করে #১ — চাইলে ধাপ ৭-এর স্কোপ বাড়ানো যায়)।

**কোনো কোড এডিট করা হয়নি এই ধাপে।** থামলাম, confirmation-এর অপেক্ষায়।
