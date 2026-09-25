# ROLE_UID_SYNC_FIX_PROGRESS.md

এই ফাইলটা `ROLE_UID_SYNC_FIX_MASTER_PROMPT.md`-এর general rule #১০ অনুযায়ী — প্রতিটা ধাপ
শেষে এখানে log যোগ হবে, যাতে সেশন হারিয়ে গেলেও পরবর্তী ধাপ কোথা থেকে শুরু করতে হবে বোঝা যায়।

---

## ধাপ ০ — অডিট ✅ সম্পন্ন

- **আউটপুট:** `ROLE_UID_AUDIT.md`
- **কী করা হয়েছে:** repository-জুড়ে ৫টা ক্যাটাগরির প্যাটার্ন গ্রেপ/পড়ে লিস্ট করা হয়েছে
  (৫১টা cloud-sync guard, ১টা id-generation সাইট, ২২টা `linkedAccountId` occurrence, ২টা
  `getLinkedUserByRole`/`getUserByContactAndRole` call site, ~২০টা FK-usage সাইট)।
- **কোনো কোড বদলানো হয়নি।**
- **কী টেস্ট করা উচিত:** কিছু না — এটা শুধু অডিট, বিল্ড-এ প্রভাব নেই।

## ধাপ ১ — ডেটা মডেল ডিজাইন ✅ সম্পন্ন

- **আউটপুট:** `ROLE_UID_FIX_DESIGN.md`
- **কী করা হয়েছে:**
  - `UserEntity` schema পড়ে দেখানো হয়েছে যে single-row মডেলের জন্য প্রয়োজনীয় ফিল্ডগুলো
    (`hasUserRole`, `hasSolverRole`, `balanceUser`/`balanceSolver` role-scoped কলাম ইত্যাদি)
    **আগে থেকেই আছে** — নতুন schema field লাগবে না ধাপ ২-এ।
  - `SupabaseSyncManager.switchRole()` RPC-এর সোর্স পড়ে নিশ্চিত করা হয়েছে এটা ইতিমধ্যেই
    single-row (root `auth.uid()`)-ভিত্তিক — বদলানোর দরকার নেই।
  - নতুন ফাংশন `switchRoleInPlace()`-এর স্পেক লেখা হয়েছে (dual-row insert বাদ দিয়ে, শুধু
    root row-এর field flip)।
  - ধাপ ০-এর ৫১টা guard-এর item-by-item review করে দেখানো হয়েছে এগুলোর কোনোটাই id-prefix
    branch করে না — তাই এগুলোতে সরাসরি কোনো কোড পরিবর্তন লাগবে না, id স্থিতিশীল হলেই
    (ধাপ ৪-এর পর) এগুলো এমনিতেই সঠিক হয়ে যাবে। ধাপ ৫ মূলত verification, rewrite না।
  - existing-install migration/merge প্ল্যান লেখা হয়েছে (non-destructive, balance-conflict
    সিদ্ধান্তটা ধাপ ৩ implement করার আগে আলাদাভাবে confirm করা দরকার বলে চিহ্নিত করা হয়েছে)।
- **কোনো `.kt`/`.sql` ফাইল এডিট করা হয়নি।**
- **কী টেস্ট করা উচিত:** কিছু না (কোড বদলায়নি) — শুধু ডিজাইন ডকুমেন্টটা রিভিউ করে দেখা উচিত:
  1. `switchRoleInPlace()`-এর স্পেক (সেকশন ২) balance/KYC handling ঠিক মনে হচ্ছে কিনা।
  2. Migration প্ল্যানে (সেকশন ৪) balance-conflict নিয়ে open প্রশ্নটা কীভাবে সমাধান করা
     হবে সেটা ঠিক করা।

## ধাপ ২ — এখনো শুরু হয়নি ⏳ (⚠️ এই সেকশনটা পুরনো/অচল — নিচের "ধাপ ২ ✅ সম্পন্ন" এন্ট্রিটাই সঠিক)

পরবর্তী ধাপ: `ROLE_UID_FIX_DESIGN.md`-এর সেকশন ২ অনুযায়ী `SomadhanRepository.switchRoleInPlace()`
ফাংশন লেখা (পুরনো `switchRole()` অপরিবর্তিত রেখে)। মাস্টার প্রম্পটের ধাপ ২ ব্লক ব্যবহার করতে
হবে। **ব্যবহারকারীর confirmation ছাড়া এই ধাপ শুরু করা যাবে না।**

## ধাপ ২ — নতুন single-row role-switch লজিক ✅ সম্পন্ন

- **আউটপুট:** `SomadhanRepository.kt`-এ নতুন `switchRoleInPlace(user, newRole, newCategories)`
  ফাংশন (পুরনো `switchRole()`-এর ঠিক পরে যোগ করা হয়েছে, লাইন ~১১৪২)।
- **কী করা হয়েছে:**
  - কোনো নতুন `UserEntity` insert হয় না, `SOLVER_xxxx`/`USER_xxxx` id বানানো হয় না — শুধু
    root row (`rootAccountId = user.linkedAccountId ?: user.id`) load করে তার role-সংক্রান্ত
    ফিল্ড (`role`, `hasUserRole`, `hasSolverRole`, `solverCategories`,
    `hasCompletedSolverSetup`) এবং active-role cache ফিল্ড (`balance`, `reputationScore`,
    `isBanned`, `isRestricted` — role-scoped কলাম থেকে) update করে `userDao.updateUser()` কল
    করে (কোনো `insertUser()` নেই)।
  - `id` কখনো পরিবর্তন হয় না।
  - বিদ্যমান `SupabaseSyncManager.switchRole()` RPC-ই কল হয় (সোর্স পড়ে নিশ্চিত করা হয়েছে এটা
    আগে থেকেই root-uid-based — ROLE_UID_FIX_DESIGN.md সেকশন ২, পয়েন্ট ১ অনুযায়ী, এই RPC
    বদলানো হয়নি), একই guard (`currentUserId() == rootAccountId`) আর response-handling
    প্যাটার্ন পুরনো `switchRole()` থেকে কপি করা হয়েছে (শুধু dual-row অংশ বাদ)।
  - role-change notification পাঠানো হয় (`resultUser`-এর বদলে `updatedRoot`, id সবসময় root)।
  - `getLinkedUserByRole`/`getUserByContactAndRole` কল করা হয়নি (design অনুযায়ী, dual-row
    lookup-এর দরকার নেই)।
- **পুরনো `switchRole()` অপরিবর্তিত** — এখনো কোডে আছে, এখনো ViewModel থেকেই কল হচ্ছে।
- **এই ধাপে ViewModel/UI-এর কোনো কল-সাইট বদলানো হয়নি** — `switchRoleInPlace()` এখনো কোথাও
  থেকে কল হয় না (wiring ধাপ ৪-এ)।
- **যাচাই করা হয়েছে:** পুরো ফাইলে `{`/`}` কাউন্ট আলাদাভাবে মিলিয়ে দেখা হয়েছে (১৬৩০/১৬৩০) —
  ম্যাচ করেছে। **আসল Gradle/Kotlin compiler দিয়ে build করে দেখা হয়নি** (এই পরিবেশে Android
  SDK/Gradle/network নেই) — এটাই এখন সবচেয়ে জরুরি ব্যবহারকারীর নিজে করা টেস্ট।
- **কী টেস্ট করা উচিত:** Android Studio-তে zip খুলে actual Gradle build/compile চালিয়ে দেখা —
  বিশেষ করে নতুন `switchRoleInPlace()` ফাংশনে টাইপ/সিনট্যাক্স এরর নেই তা নিশ্চিত করা। যেহেতু
  ফাংশনটা এখনো কোথাও কল হয় না, runtime টেস্টের দরকার নেই এই ধাপে — শুধু কম্পাইল হচ্ছে কিনা।

**পরবর্তী ধাপ (৩):** existing installs-এর জন্য migration/merge লজিক — ব্যবহারকারীর explicit
confirmation দরকার শুরুর আগে (rule #৯), এবং balance-conflict handling নিয়ে আলাদাভাবে confirm
করা দরকার (ROLE_UID_FIX_DESIGN.md সেকশন ৪-এ ফ্ল্যাগ করা)।

## ধাপ ৩ — existing installs-এর migration/merge লজিক ✅ সম্পন্ন

- **আউটপুট (৩টা ফাইল বদলেছে):**
  1. `UserEntity.kt` — নতুন local-only ফিল্ড `localDualRowArchived: Boolean = false`
     (`legacyDualRoleMergeDoneAt`-এর ঠিক পরে)। বিদ্যমান `legacyDualRoleMergeDoneAt` reuse
     করা হয়নি (ডিজাইন সেকশন ৪ অনুযায়ী — সেটার অর্থ আলাদা)।
  2. `AppDatabase.kt` — DB version ৪৮ → ৪৯, নতুন `MIGRATION_48_49`
     (`ALTER TABLE users ADD COLUMN localDualRowArchived INTEGER NOT NULL DEFAULT 0`),
     `addMigrations(...)` তালিকায় যোগ করা হয়েছে। শুধু কলাম যোগ, কোনো ডেটা মোছা/বদলানো হয়নি।
  3. `SomadhanRepository.kt` —
     - নতুন `migrateLegacyDualRowsIntoRoot(rootUserId): LegacyDualRowMigrationResult`
       (`switchRoleInPlace()`-এর পরে, CATEGORIES সেকশনের ঠিক আগে) + সহযোগী nested
       `data class LegacyDualRowMigrationResult`।
     - `cacheUserLocally()`-তে ৪ লাইন যোগ: `localDualRowArchived` preserve করা
       (`legacyDualRoleMergeDoneAt` preserve করার মতোই কারণ — cloud refresh-এ ফ্ল্যাগ
       রিসেট হয়ে migration যেন বারবার না চলে)।

- **Merge নিয়ম (সবই non-destructive):**
  - candidate = `getLinkedAccounts(root.id)` → `id != root.id` **এবং** id-তে `SOLVER_`/`USER_`
    prefix **এবং** `!localDualRowArchived` (idempotency guard)।
  - **KYC পুরো ব্লক একসাথে** কপি হয় (field-by-field না), শুধু তখনই যখন root-এর KYC ফাঁকা
    (`kycStatus == "none" && !isKycVerified`) আর legacy row-এ KYC আছে। root-এ KYC থাকলে
    **স্পর্শ করা হয় না**। *কারণ:* দুই আলাদা submission-এর ফিল্ড মিশে গেলে অসঙ্গত KYC হতো।
  - `solverCategories` শুধু root ফাঁকা হলে কপি; `hasUserRole`/`hasSolverRole`/
    `hasCompletedSolverSetup` শুধু OR করা হয় (false → true হতে পারে, উল্টোটা কখনো না)।
  - **Balance: কিছুই লেখা হয় না** (ব্যবহারকারীর সাথে confirm করা সিদ্ধান্ত, ধাপ ৩)। conflict
    শুধু detect করে `Log.w` + result-এর `balanceConflicts` লিস্টে ফেরত দেওয়া হয়। *কারণ:*
    ধাপ ১৪.৫ক-খ-এর পর Supabase role-scoped কলামই টাকার একমাত্র source-of-truth, আর legacy
    local row-গুলোর balance কখনো cloud-এ যায়নি (নিজস্ব auth session ছিল না) — সেটা root-এ
    কপি করলে cloud-এ নেই এমন টাকা তৈরি হতো, পরে reconcile করা যেত না (rule #৫)।
  - পুরনো row **মোছা হয় না** — শুধু `localDualRowArchived = true` (rollback সম্ভব, rule #৩)।
  - সব লেখা হয় হিসেব শেষ হওয়ার পর; মাঝপথে ব্যতিক্রম ঘটলে কোনো row archived হবে না, পরের রানে
    আবার পুরোটা চেষ্টা হবে।
  - **কোনো Supabase/cloud write নেই** — pure local Room merge।

- **এই ফাংশন এখনো কোথাও থেকে কল হয় না** (grep করে যাচাই করা হয়েছে) — login flow-এ wiring ধাপ ৪-এ।
- **পুরনো `switchRole()` ও `mergeLegacyDualRoleDataFromCloud()` অপরিবর্তিত।**

### Manual verification note (কী input → কী output)

| Input (local Room অবস্থা) | প্রত্যাশিত output |
|---|---|
| root row আছে, কোনো linked row নেই | `candidateRows=0, archivedRows=0, rootChanged=false`; কোনো DB write নেই |
| root-এর KYC `none`, linked `SOLVER_ab12`-এ `kycStatus="verified"` | root-এ পুরো KYC ব্লক কপি, `kycMerged=true`, linked row `localDualRowArchived=true` (row টিকে থাকে) |
| root-এর KYC ইতিমধ্যে `verified`, linked-এও KYC আছে | root-এর KYC **অপরিবর্তিত**, `kycMerged=false`, linked তবুও archived |
| root `solverCategories=""`, linked `"CAT_ELEC,CAT_PLUMB"` | root-এ কপি, `categoriesMerged=true` |
| linked `balance=500.0`, root `balanceSolver=0.0` | কোনো balance বদলায় না; `balanceConflicts`-এ ১টা এন্ট্রি + `Log.w` |
| একই ফাংশন দ্বিতীয়বার কল | `candidateRows=0` (archived guard), কোনো পরিবর্তন নেই |
| `rootUserId` ভুল/অস্তিত্বহীন | ফাঁকা result + `Log.w`, কোনো write নেই |

- **যাচাই করা হয়েছে:** তিনটা ফাইলের `{`/`}` কাউন্ট মিলেছে (Repository ১৬৪৭/১৬৪৭, AppDatabase ৮০/৮০);
  `UserEntity(` সব call site named-argument ব্যবহার করে (grep করা) তাই নতুন ফিল্ড যোগে কোনো
  positional-constructor ভাঙছে না; নতুন ফাংশনের কোনো caller নেই। **আসল Gradle/Kotlin compiler
  দিয়ে build করা হয়নি** (এই পরিবেশে Android SDK/Gradle/network নেই)।

- **কী টেস্ট করা উচিত:**
  1. Android Studio-তে Gradle build/compile — বিশেষ করে Room schema (version ৪৯) generate হচ্ছে কিনা।
  2. পুরনো (ইতিমধ্যে role switch করা) ডেটাওয়ালা একটা ডিভাইসে app আপগ্রেড করে দেখা — DB migration
     ৪৮→৪৯ crash ছাড়াই হচ্ছে কিনা, কোনো ডেটা হারাচ্ছে না।
  3. runtime merge টেস্ট এখনো সম্ভব না (ফাংশন কল হয় না) — সেটা ধাপ ৪-এর পর।

**⚠️ আলাদা করে লক্ষণীয় (এই ধাপের স্কোপের বাইরে, ইচ্ছাকৃতভাবে ঠিক করা হয়নি — rule #১):**
`AppDatabase.kt`-এর migration chain-এ `MIGRATION_45_46` **নেই** (৪৪→৪৫ আছে, তারপর সরাসরি ৪৬→৪৭)।
অর্থাৎ version ৪৫-এ থাকা কোনো ইনস্টল আপগ্রেড করলে `fallbackToDestructiveMigration(dropAllTables = true)`
চলবে — পুরো local DB মুছে যাবে। এটা আগে থেকেই ছিল, ধাপ ৩ এটা তৈরি করেনি, কিন্তু ধাপ ৬-এর
regression টেস্টের আগে সিদ্ধান্ত নেওয়া দরকার।

**পরবর্তী ধাপ (৪):** ViewModel wiring — login-এর পরে `migrateLegacyDualRowsIntoRoot()` কল করা এবং
`switchRoleToSolver()`/`switchRoleToUser()`-কে `switchRoleInPlace()`-এ সরানো। **ব্যবহারকারীর explicit
confirmation ছাড়া শুরু করা যাবে না (rule #৯)।**

## ধাপ ৪ — ViewModel wiring (আসল সুইচ) ✅ সম্পন্ন

- **শুধু ১টা ফাইল বদলেছে:** `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt`
  (৩টা জায়গায় পরিবর্তন)।

- **সিদ্ধান্ত (ব্যবহারকারীর সাথে confirm করা হয়েছে):** `migrateLegacyDualRowsIntoRoot()` **শুধু**
  `completeLoginAfterOtp()`-এ কল হবে, মাস্টার প্রম্পট অনুযায়ী কঠোরভাবে — `restoreSession()`
  (app restart-এ session restore) **ইচ্ছাকৃতভাবে স্পর্শ করা হয়নি**। এর মানে: যে ইউজার আগে থেকেই
  লগইন করা আছে এবং শুধু app restart করে (নতুন করে login করে না), তার ডিভাইসে migration কখনো চলবে
  না যতক্ষণ না সে আবার explicit login করে (logout করে আবার ঢোকে)। এটা স্কোপ-সীমিত সিদ্ধান্ত, বাগ না।

1. **`completeLoginAfterOtp()`** (লাইন ~২৫৩৫) — `freshUser` লোড ও `_currentUser.value`/
   `saved_user_id` সেট হওয়ার ঠিক পরে, `observeUserData()`-এর আগে —
   `runCatching { repository.migrateLegacyDualRowsIntoRoot(freshUser.id) }.onFailure { Log.e(...) }`
   যোগ করা হয়েছে। রেজাল্ট ব্যবহার করা হয়নি (শুধু side-effect হিসেবে merge করে), ব্যর্থ হলে শুধু
   `Log.e` — কোনোভাবেই login flow ব্যর্থ/আটকে যাবে না (`runCatching` দিয়ে wrap করা)।
2. **`switchRoleToSolver()`** (লাইন ~২৯১৮) — `repository.switchRole(user, "SOLVER", newCategories)`
   → `repository.switchRoleInPlace(user, "SOLVER", newCategories)`।
3. **`switchRoleToUser()`** (লাইন ~২৯৪২) — `repository.switchRole(user, "USER")` →
   `repository.switchRoleInPlace(user, "USER")`।
4. **পুরনো `repository.switchRole()`** — কোনো পরিবর্তন করা হয়নি, repository-তে **অবিকল আগের মতোই
   আছে**। grep করে যাচাই করা হয়েছে: এই দুটো ViewModel ফাংশনই ছিল এর একমাত্র caller, তাই এখন এটা
   সত্যিকারের dead code (rule #৩ অনুযায়ী delete করা হয়নি)।
5. **অডিটের ৪ নং পয়েন্ট (id-prefix-নির্ভর আচরণ বদলানো) — কোনো কোড পরিবর্তন লাগেনি।**
   `ROLE_UID_AUDIT.md` (ক্যাটাগরি ২, লাইন ১৩৪-১৩৭) অনুযায়ী পুরো main source set-এ কোথাও
   `id.startsWith("SOLVER_")`/`startsWith("USER_")` স্টাইলের behavior-চেঞ্জিং প্রি-চেক পাওয়া
   যায়নি — বাগটা শুধু value-comparison-এ (`currentUserId() == user.id`) প্রকাশ পায়, যা ধাপ ৫-এ
   ধরা হবে। তাই এই ধাপে এই পয়েন্টের জন্য কোনো "আগে/পরে" দেখানোর মতো পরিবর্তন নেই — no-op, শুধু
   পুনঃনিশ্চিত করা হলো grep চালিয়ে (আগের অডিটের সাথে মিলেছে)।

- **Verify করা হয়েছে (logic পড়ে, কম্পাইলার ছাড়া):**
  - `switchRoleInPlace()`-এর সিগনেচার (`suspend fun switchRoleInPlace(user: UserEntity, newRole:
    String, newCategories: List<String> = emptyList()): UserEntity`) পুরনো `switchRole()`-এর
    সিগনেচারের সাথে হুবহু মেলে — তাই call-site বদলাতে অন্য কোনো প্যারামিটার/টাইপ বদলায়নি।
  - `switchRoleInPlace()`-এর ভেতরের কোড (ধাপ ২) নিশ্চিত করে `id` কখনো বদলায় না (`updatedRoot =
    rootUser.copy(role = ..., ...)` — `id` ফিল্ড copy-তে touch করা হয়নি), তাই role switch-এর
    আগে-পরে `currentUser.id` অভিন্ন থাকবে।
  - পুরো ফাইলের `{`/`}` কাউন্ট মিলিয়ে দেখা হয়েছে (১৩১৮/১৩১৮ — ম্যাচ করেছে)।
  - `Log` import (`android.util.Log`) আগে থেকেই ফাইলের শুরুতে আছে, নতুন করে import লাগেনি।
  - পুরো `app/src/main/java/` জুড়ে grep করে নিশ্চিত করা হয়েছে `repository.switchRole(` আর
    কোথাও কল হচ্ছে না (`SupabaseSyncManager.switchRole(...)` আলাদা, RPC কল — সেটা অপরিবর্তিত)।
  - **আসল Gradle/Kotlin compiler দিয়ে build করা হয়নি** (এই পরিবেশে Android SDK/Gradle/network
    নেই) — আগের ধাপগুলোর মতোই, এটা এখন সবচেয়ে জরুরি ব্যবহারকারীর নিজের করা টেস্ট।

- **এই ধাপে ইচ্ছাকৃতভাবে বদলানো হয়নি:** `register()`, `loginAsAdmin()`, `restoreSession()` —
  কোনোটাতেই migration/switchRoleInPlace সংক্রান্ত কল যোগ করা হয়নি (স্কোপের বাইরে, উপরে ব্যাখ্যা
  করা হয়েছে)। কোনো UI/screen/button/flow বদলানো হয়নি (rule #২)।

- **কী টেস্ট করা উচিত:**
  1. Android Studio-তে Gradle build — কম্পাইল এরর নেই তা নিশ্চিত করা (সবচেয়ে জরুরি)।
  2. একটা "পুরনো" (আগে role switch করা, তাই local Room-এ SOLVER_xxx/USER_xxx linked row আছে)
     টেস্ট অ্যাকাউন্টে logout করে আবার phone+OTP দিয়ে login করা — logcat-এ
     `migrateLegacyDualRowsIntoRoot` সংক্রান্ত কোনো `Log.e` আসছে কিনা দেখা (না আসাই প্রত্যাশিত),
     এবং merge সঠিকভাবে হয়েছে কিনা (KYC/categories) যাচাই করা।
  3. role switch করে (Solver ↔ User) emulator/device-এ `currentUser.id` logcat-এ print করে
     আগে-পরে অভিন্ন থাকছে কিনা verify করা (মাস্টার প্রম্পটের নির্দেশ অনুযায়ী)।
  4. role switch-এর পরের UX (toast, navigation, categories দেখানো) আগের মতোই আছে কিনা — কোনো
     regression নেই তা নিশ্চিত করা।

**পরবর্তী ধাপ (৫):** cloud sync guard-গুলো (৫১ জায়গা, ব্যাচে ব্যাচে) ঠিক করা। **ব্যবহারকারীর
explicit confirmation ছাড়া শুরু করা যাবে না (rule #৯)।**

## ধাপ ৫ — cloud sync guard verify (ব্যাচে ব্যাচে) — শুরু

**গুরুত্বপূর্ণ নোট:** ধাপ ০-এর অডিটের পর ধাপ ২-৩-এ নতুন ফাংশন (`switchRoleInPlace`,
`migrateLegacyDualRowsIntoRoot`) যোগ হওয়ায় ফাইলের লাইন নাম্বার শিফট হয়ে গেছে (audit-এর
লাইন নাম্বার আর মেলে না)। এই ধাপে লাইন নাম্বারের বদলে **ফাংশনের নাম** দিয়ে রেফারেন্স করা
হচ্ছে — পুরো ফাইল আবার grep করে re-verify করা হয়েছে যে audit-এর তালিকার সব ৫০টা (+২টা
`switchRole`/`switchRoleInPlace`, যেগুলো ইতিমধ্যে সঠিক) guard এখনো ঠিক ৪৩টা ফাংশনেই আছে,
কোনোটা হারিয়ে যায়নি বা নতুন কোনো guard তৈরি হয়নি।

ব্যাচ বিভাজন (feature area অনুযায়ী, ৭টা ব্যাচ):
1. KYC + প্রোফাইল/অ্যাকাউন্ট (৬ ফাংশন) — **এই ব্যাচ**
2. প্রবলেম/বিড তৈরি ও লাইফসাইকেল (৭ ফাংশন)
3. জব/বিড বাতিল ও ডিসপিউট (৮ ফাংশন)
4. সলভার জব অ্যাকশন ও ইনস্ট্যান্ট জব পার্ট ১ (৮ ফাংশন)
5. ইনস্ট্যান্ট জব সমাপ্তি/এক্সট্রা চার্জ (৫ ফাংশন)
6. মেসেজ/রেটিং/নোটিফিকেশন (৬ ফাংশন)
7. ওয়ালেট (৩ ফাংশন)

### ব্যাচ ১ — KYC + প্রোফাইল/অ্যাকাউন্ট ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `updateUser` (২টা guard), `updateUserPassword`, `registerUser`, `toggleFavoriteSolver`,
`addFavoriteSolver`, `submitKyc` (২টা guard) — মোট ৮টা guard।

- প্রতিটা ফাংশনের পুরো সোর্স পড়ে যাচাই করা হয়েছে: সবগুলোই **"self" কেস** — caller নিজের
  account/profile-এর উপর কাজ করছে (`user.id`/`userId` সবসময় caller নিজেরই id), কোথাও admin
  অন্য কারো পক্ষে এই ফাংশনগুলো কল করছে এমন প্রমাণ পাওয়া যায়নি (`repository.updateUser(`-এর
  সব call-site ViewModel-এ grep করে দেখা হয়েছে — সব নিজের প্রোফাইল আপডেট)।
- ধাপ ৪-এর পর role switch-এর সময়ও `user.id`/`userId` root Supabase UID-ই থাকে (নতুন
  SOLVER_xxx/USER_xxx id আর তৈরি হয় না), তাই এই ৮টা guard **এমনিতেই এখন সঠিকভাবে pass করছে**
  — কোনো কোড পরিবর্তনের দরকার নেই।
- **কোনো ফাইল বদলানো হয়নি এই ব্যাচে।**

**কী টেস্ট করা উচিত (এই ব্যাচের জন্য):** Solver role-এ থেকে profile edit করে / password change
করে / KYC সাবমিট করে / কোনো solver-কে favorite করে — অন্য ডিভাইস বা admin panel-এ (Supabase
টেবিল সরাসরি দেখেও) sync হচ্ছে কিনা যাচাই করা, বিশেষ করে role switch করার **পরে** এই একই টেস্ট
আবার করে দেখা (আগে যেটা sync হতো না, এখন হওয়া উচিত)।

**পরবর্তী ব্যাচ (২):** প্রবলেম/বিড তৈরি ও লাইফসাইকেল (`resolveCommissionRateForNewJob`,
`createProblem`, `createDirectContract`, `acceptDirectContractProposal`,
`declineDirectContractProposal`, `placeBid`, `acceptBid`)। **ব্যবহারকারীর explicit confirmation
ছাড়া শুরু করা যাবে না (rule #৯)।**

### ব্যাচ ২ — প্রবলেম/বিড তৈরি ও লাইফসাইকেল ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `resolveCommissionRateForNewJob` (২টা guard), `createProblem` (২টা), `createDirectContract`
(২টা), `acceptDirectContractProposal`, `declineDirectContractProposal`, `placeBid` (২টা),
`acceptBid` — মোট ১০টা guard।

- `createProblem`, `createDirectContract`, `placeBid` — caller নিজের id (`user.id`/`solver.id`)
  দিয়ে guard, নিজের জিনিস নিজে পোস্ট/বিড করছে — **self, ঠিক আছে**, ধাপ ৪-এর পর এমনিতেই pass করবে।
- `acceptDirectContractProposal`, `declineDirectContractProposal` — caller = solver (প্রস্তাব
  accept/decline করছে), guard `== solverId` — **self, ঠিক আছে**।
- `acceptBid` — caller = problem owner (bid accept করছে), guard `== problem.userId` — **self,
  ঠিক আছে**।

**⚠️ পাশাপাশি একটা পাওয়া গেছে (এই ধাপের স্কোপের বাইরে, ইচ্ছাকৃতভাবে ঠিক করা হয়নি — rule #১):**
`resolveCommissionRateForNewJob(solverId)`-এর ভেতরের ২টা guard (`== solverId`) সরাসরি **টার্গেট
functionটার নিজের caller-এর উপর নির্ভর করে না** — এটা একটা private helper, দুই জায়গা থেকে কল
হয়:
- `placeBid`-এর ভেতর থেকে (caller = solver নিজেই) → guard ঠিক আছে।
- `acceptBid`-এর ভেতর থেকে (caller = **problem owner**, `bid.solverId` না) → এখানে
  `currentUserId() == solverId` **কখনোই true হবে না**, কারণ caller solver না, owner। এটা
  role-switch/dual-row বাগের সাথে সম্পর্কিত না (ধাপ ৪ এটা ঠিক করবে না, করা উচিতও না এই স্কোপে) —
  বরং একটা আলাদা, আগে থেকে থাকা কাঠামোগত ফাঁক: `acceptBid`-এর মাধ্যমে বিড accept হলে solver-এর
  free-quota counter (`freeJobsUsedThisMonth`/`freeJobsMonthKey`) Supabase-এ sync হয় না (শুধু
  local Room-এ আপডেট হয়)। **এই master prompt-এর scope-এর বাইরে** (rule #১, শুধু SOLVER_xxx/
  USER_xxx dual-row বাগ ফিক্স করার কথা, এই সাধারণ caller-mismatch গ্যাপ না) — কোনো কোড বদলানো
  হয়নি, শুধু ফ্ল্যাগ করা হলো ভবিষ্যতে আলাদাভাবে সিদ্ধান্ত নেওয়ার জন্য।

**কী টেস্ট করা উচিত:** নতুন সমস্যা পোস্ট, direct contract অফার/accept/decline, normal bid
placement/acceptance — role switch করার আগে ও পরে দুইভাবেই করে cross-device/admin sync
verify করা।

**পরবর্তী ব্যাচ (৩):** জব/বিড বাতিল ও ডিসপিউট (`withdrawBid`, `requestJobRelease`,
`cancelJobReleaseRequest`, `rejectJobReleaseRequest`, `raiseDispute`, `withdrawDispute`,
`settleDispute`, `requestAdminAssistance`)। **ব্যবহারকারীর explicit confirmation ছাড়া শুরু করা
যাবে না (rule #৯)।**

### ব্যাচ ৩ — জব/বিড বাতিল ও ডিসপিউট ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `withdrawBid`, `requestJobRelease`, `cancelJobReleaseRequest`, `rejectJobReleaseRequest`,
`raiseDispute`, `withdrawDispute`, `settleDispute`, `requestAdminAssistance` — মোট ৮টা guard।

- `withdrawBid` — guard `== bid.solverId`; caller নিজের বিড cancel করছে — **self, ঠিক আছে**।
- `requestJobRelease`, `cancelJobReleaseRequest` — guard `== problem.acceptedSolverId`; caller
  নিজে accepted solver হয়েই নিজের কাজ রিলিজ/ক্যান্সেল করছে — **self, ঠিক আছে**।
- `rejectJobReleaseRequest` — guard `== problem.userId`; caller নিজে problem owner হয়েই
  reject করছে — **self, ঠিক আছে**।
- `raiseDispute`, `withdrawDispute`, `settleDispute`, `requestAdminAssistance` — এই ৪টার guard
  প্যারামিটার হিসেবে পাওয়া `userId`/`requesterId`-এর বিরুদ্ধে (caller যেই হোক, নিজের id-ই পাস
  করে) — ViewModel-এর সব call-site (`SomadhanViewModel.kt`) grep করে যাচাই করা হয়েছে, সব জায়গায়
  `user.id`/`_currentUser.value?.id` (caller নিজের id) পাস করা হচ্ছে, admin অন্য কারো পক্ষে এই
  ফাংশনগুলো কল করছে এমন কোনো call-site পাওয়া যায়নি। `withdrawDispute`-এ এমনিতেই একটা বাড়তি
  guard আছে (`problem.disputeInitiatorId == requesterId`) যেটা শুধু আসল initiator-কেই allow
  করে। **সবগুলো self, ঠিক আছে**।
- ধাপ ৪-এর পর role switch-এর সময়ও এই id-গুলো root Supabase UID-ই থাকে, তাই এই ৮টা guard
  **এমনিতেই এখন সঠিকভাবে pass করছে** — কোনো কোড পরিবর্তনের দরকার নেই।
- **কোনো ফাইল বদলানো হয়নি এই ব্যাচে।**

**কী টেস্ট করা উচিত:** বিড withdraw, job release request/cancel/reject, dispute raise/withdraw/
settle, admin assistance request — role switch করার আগে ও পরে দুইভাবেই করে cross-device/admin
panel sync verify করা।

**পরবর্তী ব্যাচ (৪):** সলভার জব অ্যাকশন ও ইনস্ট্যান্ট জব পার্ট ১ (`solverCancelJob`,
`clearSolverCancelledNotice`, `createInstantJob`, `updateInstantJobToggle`,
`acceptInstantJobBid`, `markSolverOnWay`, `markSolverArrived`, `markJobStarted`)।
**ব্যবহারকারীর explicit confirmation ছাড়া শুরু করা যাবে না (rule #৯)।**

### ব্যাচ ৪ — সলভার জব অ্যাকশন ও ইনস্ট্যান্ট জব পার্ট ১ ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `solverCancelJob` (২টা guard), `clearSolverCancelledNotice`, `createInstantJob`,
`updateInstantJobToggle`, `acceptInstantJobBid`, `markSolverOnWay`, `markSolverArrived`,
`markJobStarted` — মোট ৮টা guard।

- `solverCancelJob` — উভয় guard `== solverId` (প্যারামিটার); caller নিজেই সেই solver — ViewModel
  overload-চেইন (`solverCancelAcceptedJob(problem, bid)` → `solverCancelAcceptedJob(problem,
  reason)` → `solverCancelJob(problemId, solverId, ...)`) grep করে দেখা হয়েছে, `solverId` সবসময়
  `_currentUser.value?.id` থেকে আসে (একটা জায়গায় null-safety fallback হিসেবে
  `problem.acceptedSolverId` আছে, কিন্তু সেটাও একই ব্যক্তির id বোঝানোর জন্য, অন্য কারো পক্ষে না) —
  **self, ঠিক আছে**।
- `clearSolverCancelledNotice` — guard `== problem.userId`; caller নিজে problem owner হয়েই কল
  করছে — **self, ঠিক আছে**।
- `createInstantJob` — guard `== user.id`; caller নিজের জরুরি জব পোস্ট করছে — **self, ঠিক আছে**।
- `updateInstantJobToggle` — guard `== userId`; caller নিজের নোটিফিকেশন toggle বদলাচ্ছে —
  **self, ঠিক আছে**।
- `acceptInstantJobBid` — guard `== updated.userId` (problem owner); caller নিজে owner হয়েই
  বিড accept করছে (ভেতরে `acceptBid()`-ও কল হয়, যেটা ব্যাচ ২-তে ইতিমধ্যে verify করা) —
  **self, ঠিক আছে**।
- `markSolverOnWay`, `markSolverArrived`, `markJobStarted` — সবগুলোর guard
  `== problem.acceptedSolverId`; caller নিজে accepted solver হয়েই নিজের কাজের status আপডেট
  করছে — **self, ঠিক আছে**।
- ধাপ ৪-এর পর role switch-এর সময়ও এই id-গুলো root Supabase UID-ই থাকে, তাই এই ৮টা guard
  **এমনিতেই এখন সঠিকভাবে pass করছে** — কোনো কোড পরিবর্তনের দরকার নেই।
- **কোনো ফাইল বদলানো হয়নি এই ব্যাচে।**

**কী টেস্ট করা উচিত:** solver-এর accepted job cancel, ইনস্ট্যান্ট জব পোস্ট/toggle/accept, on-way/
arrived/job-started status আপডেট — role switch করার আগে ও পরে দুইভাবেই করে cross-device/admin
panel sync verify করা।

**পরবর্তী ব্যাচ (৫):** ইনস্ট্যান্ট জব সমাপ্তি/এক্সট্রা চার্জ (`cancelInstantJob`,
`requestExtraAmount`, `userConfirmExtraAmount`, `userRejectExtraAmount`,
`checkAndExpireInstantJobs`)। **ব্যবহারকারীর explicit confirmation ছাড়া শুরু করা যাবে না
(rule #৯)।**

### ব্যাচ ৫ — ইনস্ট্যান্ট জব সমাপ্তি/এক্সট্রা চার্জ ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `cancelInstantJob`, `requestExtraAmount`, `userConfirmExtraAmount`, `userRejectExtraAmount`,
`checkAndExpireInstantJobs` — মোট ৫টা guard।

- `cancelInstantJob` — guard `== problem.userId`; caller নিজে owner হয়েই নিজের ইনস্ট্যান্ট জব
  cancel করছে — **self, ঠিক আছে**।
- `requestExtraAmount` — guard `== problem.acceptedSolverId`; caller নিজে accepted solver —
  **self, ঠিক আছে**।
- `userConfirmExtraAmount`, `userRejectExtraAmount` — guard `== problem.userId`
  (`freshProblem.userId`); caller নিজে owner হয়েই এক্সট্রা চার্জ confirm/reject করছে —
  **self, ঠিক আছে**।
- `checkAndExpireInstantJobs` — একটু ভিন্ন প্যাটার্ন: এটা সব ইউজারের broadcasting job periodic
  sweep করে (worker/alarm/startup থেকে), কিন্তু guard প্রতিটা problem-এর জন্য আলাদা করে
  `== problem.userId` (সেই নির্দিষ্ট problem-এর owner) চেক করে — অর্থাৎ dual-write শুধু তখনই হয়
  যখন current logged-in ডিভাইসটা সেই problem-এর owner নিজে। এটাও **self pattern** (নিজের জব
  নিজে sync করা), role-switch বাগের সাথে সম্পর্কিত অংশটা ঠিকই আছে — ধাপ ৪-এর পর
  `problem.userId` role switch করলেও অপরিবর্তিত থাকে। **অন্য ইউজারের job এই sweep-এ dual-write
  না হওয়াটা একটা আলাদা, আগে থেকে-নথিভুক্ত architectural সীমাবদ্ধতা** (কোড কমেন্টেই "জানা
  সীমাবদ্ধতা" হিসেবে লেখা আছে) — role-switch/dual-row বাগ না, তাই এই master prompt-এর স্কোপের
  বাইরে (rule #১), ছোঁয়া হয়নি।
- ধাপ ৪-এর পর role switch-এর সময়ও এই id-গুলো root Supabase UID-ই থাকে, তাই এই ৫টা guard
  **এমনিতেই এখন সঠিকভাবে pass করছে** — কোনো কোড পরিবর্তনের দরকার নেই।
- **কোনো ফাইল বদলানো হয়নি এই ব্যাচে।**

**কী টেস্ট করা উচিত:** ইনস্ট্যান্ট জব cancel, এক্সট্রা চার্জ request/confirm/reject, broadcasting
timeout-এ auto-expire — role switch করার আগে ও পরে দুইভাবেই করে cross-device/admin panel sync
verify করা।

**পরবর্তী ব্যাচ (৬):** মেসেজ/রেটিং/নোটিফিকেশন (`markMessagesAsReadForProblem`, `sendMessage`,
`submitUserRatingForSolver`, `submitSolverRatingForUser`, `markAllNotificationsAsRead`,
`markNotificationAsRead`)। **ব্যবহারকারীর explicit confirmation ছাড়া শুরু করা যাবে না
(rule #৯)।**

### ব্যাচ ৬ — মেসেজ/রেটিং/নোটিফিকেশন ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `markMessagesAsReadForProblem`, `sendMessage`, `submitUserRatingForSolver`,
`submitSolverRatingForUser`, `markAllNotificationsAsRead`, `markNotificationAsRead` — মোট ৬টা
guard।

- `markMessagesAsReadForProblem` — guard `== userId`; caller নিজের পাওয়া মেসেজ read মার্ক করছে —
  **self, ঠিক আছে**।
- `sendMessage` — guard `== senderId`; পুরো repository-জুড়ে সব internal call-site grep করে
  দেখা হয়েছে — প্রায় সবগুলোতে `senderId` caller নিজের id (owner/solver, প্রসঙ্গ অনুযায়ী) —
  **self, ঠিক আছে**। ⚠️ একটা ব্যতিক্রম পাওয়া গেছে: `adminCancelAndRefundDirectContract`-এর
  ভেতরে `senderId = "ADMIN"` (আসল UID না, sentinel string) দিয়ে sendMessage() কল হয় — এই guard
  কখনোই true হবে না (currentUserId() বাস্তব UUID, "ADMIN" স্ট্রিং না), তাই সেই একটা নির্দিষ্ট
  chat message-এর Supabase dual-write সবসময় স্কিপ হয়ে যায়। এটা role-switch/dual-row বাগ না —
  admin অন্য কারো পক্ষে (বা এখানে কোনো real-id ছাড়াই) মেসেজ পাঠানোর একটা পুরনো, ইচ্ছাকৃত gap
  (rule #১: admin-অন্য-কারো-পক্ষে কেস বদলানো যাবে না) — **ছোঁয়া হয়নি**। (পরে ব্যবহারকারীর
  প্রশ্নে পুরো ফাংশনটা আরও বিস্তারিতভাবে দেখা হয়েছে — সেই একই ফাংশনে প্রবলেম স্ট্যাটাস ও
  নোটিফিকেশনও cloud-sync হয় না বেরিয়েছে, সম্পূর্ণ বিস্তারিত `MIGRATION_PROGRESS.md`-এর শেষে
  "📌 ট্র্যাকিং নোট" সেকশনে, আর `NEXT_CLAUDE_START_HERE.md`-এও পয়েন্টার রাখা হয়েছে যাতে ধাপ ৫-৬
  শেষ হওয়ার পর আলাদা সেশনে এটা হাতে নেওয়া যায়।)
- `submitUserRatingForSolver` — guard `== problem.userId`; caller নিজে owner হয়েই solver-কে
  রেট করছে — **self, ঠিক আছে**।
- `submitSolverRatingForUser` — guard `== solverId`; caller নিজে solver হয়েই user-কে রেট করছে —
  **self, ঠিক আছে**।
- `markAllNotificationsAsRead` — guard `== userId`; caller নিজের নোটিফিকেশন read মার্ক করছে —
  **self, ঠিক আছে**।
- `markNotificationAsRead` — guard `== notif.userId` (local lookup দিয়ে owner বের করা হয়); caller
  নিজের notification read মার্ক করছে — **self, ঠিক আছে**।
- ধাপ ৪-এর পর role switch-এর সময়ও এই id-গুলো root Supabase UID-ই থাকে, তাই এই ৬টা guard
  **এমনিতেই এখন সঠিকভাবে pass করছে** — কোনো কোড পরিবর্তনের দরকার নেই।
- **কোনো ফাইল বদলানো হয়নি এই ব্যাচে।**

**কী টেস্ট করা উচিত:** চ্যাট মেসেজ পাঠানো/read মার্ক, user/solver পারস্পরিক রেটিং সাবমিট,
নোটিফিকেশন read মার্ক (একক ও সব) — role switch করার আগে ও পরে দুইভাবেই করে cross-device/admin
panel sync verify করা।

**পরবর্তী ব্যাচ (৭, শেষ ব্যাচ):** ওয়ালেট (`requestWithdrawal`, `depositMoneyViaGateway`,
`requestAdditionalCharge`)। **ব্যবহারকারীর explicit confirmation ছাড়া শুরু করা যাবে না
(rule #৯)।**

### ব্যাচ ৭ (শেষ ব্যাচ) — ওয়ালেট ✅ verify সম্পন্ন (কোনো কোড বদলায়নি)

ফাংশন: `requestWithdrawal`, `depositMoneyViaGateway`, `requestAdditionalCharge` — মোট ৩টা guard।

- `requestWithdrawal` — guard `== solver.id`; caller নিজেই সেই solver, নিজের ব্যালেন্স থেকে
  উইথড্র করছেন (ViewModel-এ `solver = user` (_currentUser.value)) — **self, ঠিক আছে**।
- `depositMoneyViaGateway` — guard `== userId`; caller নিজের ওয়ালেটে ডিপোজিট করছেন
  (ViewModel-এ `userId = user.id`) — **self, ঠিক আছে**।
- `requestAdditionalCharge` — guard `== solverId`; caller নিজে accepted solver হয়েই অতিরিক্ত
  বিলের অনুরোধ করছেন (ViewModel-এ `solverId = _currentUser.value?.id`) — **self, ঠিক আছে**।
- ধাপ ৪-এর পর role switch-এর সময়ও এই id-গুলো root Supabase UID-ই থাকে, তাই এই ৩টা guard
  **এমনিতেই এখন সঠিকভাবে pass করছে** — কোনো কোড পরিবর্তনের দরকার নেই।
- **কোনো ফাইল বদলানো হয়নি এই ব্যাচে।**

**কী টেস্ট করা উচিত:** solver উইথড্র রিকোয়েস্ট, ওয়ালেট গেটওয়ে ডিপোজিট, অতিরিক্ত বিল রিকোয়েস্ট —
role switch করার আগে ও পরে দুইভাবেই করে cross-device/admin panel sync verify করা।

---

## ধাপ ৫ — সম্পূর্ণ ✅ (৭টা ব্যাচ, ৫১টা guard, কোনো কোড বদলায়নি)

৭টা ব্যাচেই (KYC/প্রোফাইল, প্রবলেম/বিড লাইফসাইকেল, জব/বিড বাতিল ও ডিসপিউট, সলভার জব অ্যাকশন ও
ইনস্ট্যান্ট জব পার্ট ১, ইনস্ট্যান্ট জব সমাপ্তি/এক্সট্রা চার্জ, মেসেজ/রেটিং/নোটিফিকেশন, ওয়ালেট)
মোট ৫১টা `currentUserId() == ...` guard যাচাই করা হয়েছে — **সবগুলোই "self" কেস**, ধাপ ৪-এর
(role switch-এর সময়ও `user.id`/foreign-key id root Supabase UID-ই থাকা) পরিবর্তনের কারণে
এমনিতেই এখন সঠিকভাবে pass করছে। **কোনো ব্যাচেই কোনো ফাইল/ফাংশন বদলাতে হয়নি।**

স্কোপের বাইরে (rule #১, ইচ্ছাকৃতভাবে ছোঁয়া হয়নি) হিসেবে যা যা পাওয়া/নোট করা হয়েছে:
- `resolveCommissionRateForNewJob`-এর ভেতরের `acceptBid` caller-path (ব্যাচ ২) — structural
  caller-mismatch, role-switch বাগ না।
- `checkAndExpireInstantJobs`-এর owner-scoped Kotlin guard (ব্যাচ ৫) — ইতিমধ্যেই ধাপ ২৮-এর
  `pg_cron` সার্ভার-সাইড ব্যাকস্টপ দিয়ে বাস্তবে resolve হয়ে গেছে (সেশনের মধ্যে ব্যবহারকারীর
  প্রশ্নে নিশ্চিত করা)।
- `sendMessage`-এ `adminCancelAndRefundDirectContract`-এর `senderId = "ADMIN"` sentinel কেস
  (ব্যাচ ৬) — admin-অন্য-কারো-পক্ষে, বদলানো যাবে না।

**পরবর্তী ধাপ (৬):** পুরো regression টেস্ট checklist বানানো (`ROLE_UID_FIX_TEST_CHECKLIST.md`,
কোনো নতুন কোড না)। **ব্যবহারকারীর explicit confirmation ছাড়া শুরু করা যাবে না (rule #৯)।**

---

## ধাপ ৬ — সম্পূর্ণ ✅ (শুধু checklist তৈরি, কোনো কোড বদলায়নি)

`ROLE_UID_FIX_TEST_CHECKLIST.md` তৈরি করা হয়েছে (master prompt-এর ধাপ ৬-এ উল্লেখিত সব কেস সহ):
১. ডিভাইস A বেসিক ফ্লো (signup→KYC→post→role switch→bid→role switch back, id/UID অপরিবর্তিত
   থাকা যাচাই), ২. ডিভাইস B cross-device দেখা যাওয়া, ৩. ডিভাইস A-তে পরিবর্তন → ডিভাইস B refresh
   করে sync verify, ৪. Admin panel ↔ ডিভাইস sync (KYC approve/reject ইত্যাদি), ৫. পুরনো
   (migration-এর আগের) টেস্ট অ্যাকাউন্ট থাকলে migration verify।

কোনো `.kt`/`.sql` ফাইল স্পর্শ করা হয়নি। **কোনো নতুন কোড লেখা হয়নি** — শুধু manual test checklist।

**⚠️ খোলা ইস্যু, ব্যবহারকারীর সিদ্ধান্ত এখনও বাকি:** `NEXT_CLAUDE_START_HERE.md`-এ নোট করা
`AppDatabase.kt`-এ `MIGRATION_45_46` অনুপস্থিত থাকার ইস্যুটা (version ৪৫ থেকে আপগ্রেডে
`fallbackToDestructiveMigration` চলে local DB মুছে যাওয়ার ঝুঁকি) এই সেশনে ছোঁয়া হয়নি — এটা
role-switch/dual-row বাগ না (rule #১, স্কোপের বাইরে), কিন্তু regression টেস্ট (উপরের সেকশন ৫,
পুরনো অ্যাকাউন্ট migration টেস্ট) চালানোর সময় প্রভাব ফেলতে পারে যদি টেস্ট ডিভাইসে আগের কোনো
version ৪৫-এর বিল্ড ইনস্টল করা থাকে এবং তার উপর নতুন বিল্ড আপগ্রেড করা হয় — তখন local DB মুছে
যাবে এবং migration টেস্ট কেসটা validate করা যাবে না (পুরনো ডেটাই থাকবে না)। ব্যবহারকারীকে এটা
নিয়ে সিদ্ধান্ত জানাতে হবে (আলাদাভাবে ফিক্স করা হবে কিনা, নাকি টেস্টের সময় শুধু ফ্রেশ ইনস্টল
ব্যবহার করে এড়িয়ে যাওয়া হবে)।

**পরবর্তী ধাপ:** ব্যবহারকারী checklist ধরে বাস্তবে টেস্ট করে ফলাফল (pass/fail) জানাবেন। কিছু
fail করলে সেটা নিয়ে আলাদা ছোট ফিক্স-ধাপ শুরু হবে (ধাপ ৭-এর আগেই)। **ব্যবহারকারীর explicit
confirmation/ফলাফল ছাড়া এগোনো হবে না (rule #৯)।**
