# Somadhan — Engineering Notes

এই ফাইলে অ্যাপের কিছু গুরুত্বপূর্ণ ডিজাইন-সিদ্ধান্ত ও নিয়ম লেখা আছে, যেগুলো ঠিক থাকলে অ্যাপ
নিরাপদ ও স্কেলেবল থাকে। **নতুন কোনো feature, menu, বা system বানানোর আগে এই ফাইলটা পড়ে নিন —
এটা স্বয়ংক্রিয়ভাবে কার্যকর হয় না, প্রতিটা নতুন কোডে সচেতনভাবে মেনে লিখতে হয়।**

---

## ১. Balance/Wallet নিয়ে কোনো কাজ করলে

**নিয়ম: balance পরিবর্তনের জন্য কখনো `user.copy(balance = ...)` লিখবেন না।**

সবসময় atomic increment/decrement ব্যবহার করুন:
```kotlin
// ✅ সঠিক
FirebaseSyncManager.incrementUserBalance(userId, amount)   // Firestore-এ FieldValue.increment()
userDao.addBalance(userId, amount)                          // Room-এ SQL UPDATE balance = balance + :amount

// ❌ ভুল — race condition ও data loss তৈরি করে
val user = userDao.getUserById(userId)
val updated = user.copy(balance = user.balance + amount)
userDao.updateUser(updated)
```

নতুন কোনো ফিচার যদি টাকার সাথে জড়িত (referral bonus, cashback, penalty, ইত্যাদি), তাহলে:

1. উপরের atomic pattern ব্যবহার করুন
2. একটা **idempotency guard** যোগ করুন — একই action দুইবার (double-tap, retry, একাধিক ডিভাইস)
   ঘটলে যেন দুইবার balance না বদলায়। উদাহরণ: `refundEscrowOnce()`, deterministic transaction ID,
   বা status-check (দেখুন `adminReleaseEscrow()`-এর স্টাইল)
3. `syncUser()`-এর পেলোড থেকে balance ফিল্ড বাদ রাখুন — এটা আগে থেকেই এভাবে করা আছে, নতুন কোনো
   জায়গায় পুরো `UserEntity` sync করার দরকার হলে একই প্যাটার্ন অনুসরণ করুন

**Reference implementations:** `depositMoneyViaGateway()`, `refundEscrowOnce()`,
`adminReleaseEscrow()`, `adminAdjustBalance()` — নতুন কিছুর সাথে balance জড়িত থাকলে এই
ফাংশনগুলোর প্যাটার্ন অনুসরণ করুন, নতুন করে চাকা বানানোর দরকার নেই।

---

## ২. নতুন Firestore collection বানালে

- **কখনো পুরো collection পুল/listen করবেন না।** সবসময় `.whereEqualTo()` + `.orderBy()` +
  `.limit()` দিয়ে scope করুন।
- **নিজের ডেটা vs অন্যদের ডেটা আলাদা করে ভাবুন।** সাইন-ইন করা ইউজারের নিজের ডকুমেন্ট/রেকর্ড
  সবসময় পূর্ণভাবে ও real-time sync হবে; বাকি সবার ডেটা browsing/list-এর জন্য scoped/paginated
  থাকবে।
- **নতুন filtered+sorted query মানেই নতুন Firestore composite index লাগতে পারে।** প্রথমবার
  চালানোর সময় Firebase index-missing error দিলে, সেই লিংক থেকে index তৈরি করে
  `firestore.indexes.json`-এ যোগ করুন এবং `firebase deploy --only firestore:indexes` চালান।
- **`firestore.rules`-এ নতুন collection-এর জন্য access rule যোগ করতে ভুলবেন না** — ডিফল্টে কিছু
  খোলা রাখবেন না।

---

## ৩. নতুন লিস্ট/ফিড বানালে (Room + UI)

- `SELECT * FROM table` (LIMIT ছাড়া) দিয়ে সরাসরি বড় লিস্ট UI-তে দেখাবেন না। `LIMIT`/`OFFSET`
  বা Paging3 ব্যবহার করুন — deposit/withdrawal history, admin panel-এর user list ইত্যাদির মতো
  জায়গায় ডেটা বাড়ার সাথে সাথে এটা জরুরি হয়ে ওঠে।
- LazyColumn-এ আইটেম রেন্ডার করার সময় সবসময় স্থিতিশীল `key = { it.id }` ব্যবহার করুন (এটা
  ইতিমধ্যে transaction history-তে করা আছে — নতুন লিস্টেও একই নিয়ম মানুন)।
- অনেক বড় `combine()` Flow (যেমন `allProblems`, `allUsers`) থেকে সরাসরি নতুন feature বানানোর
  বদলে, দরকার হলে সেই টেবিলের জন্য আলাদা, ছোট, scoped একটা DAO query/Flow বানান।

---

## ৪. নতুন menu/ফিচার Admin Panel-এ যোগ করলে

- বিদ্যমান group-এর (ড্যাশবোর্ড, ইউজার ব্যবস্থাপনা, আর্থিক, ইত্যাদি) কাঠামো অনুসরণ করুন —
  `AdminPanelScreen.kt`-এর group প্যাটার্ন দেখুন।
- Admin action যদি কারো balance/status বদলায়, সেটাও উপরের §১-এর নিয়ম মেনে করুন —
  admin panel থেকে করা মানেই automatic safe না।
- বড় ডেটাসেট (সব ইউজার, সব transaction) দেখানোর সময় pagination রাখুন, `AdminUsersView`-এর
  বিদ্যমান pattern অনুসরণ করুন।

---

## ৫. ছবি/ফাইল আপলোড জড়িত ফিচারে

- সবসময় resize/compress করা thumbnail সংরক্ষণ ও ব্যবহার করুন, লিস্ট/ফিডে ফুল-রেজোলিউশন
  ছবি লোড করবেন না — শুধু ডিটেইল স্ক্রিনে।

---

## ৬. এই ফাইলটা আপডেট করুন যখন

- নতুন কোনো balance-touching ফাংশন বা idempotency pattern তৈরি করলে
- নতুন কোনো বড় collection/query pattern চালু করলে
- কোনো পুরনো নিয়ম বদলে গেলে (যেমন backend migration হলে, balance mutation client-side থেকে
  Cloud Function-এ সরে গেলে — তখন §১ পুরোপুরি rewrite করা লাগবে)

## ৭. Firestore scoping — সম্পন্ন কাজের রেকর্ড (২০২৬)

`users`, `problems`, `bids`, `transactions` — এই ৪টা collection-এর one-shot pull ও real-time
listener দুটোই এখন scoped (§২-এর নিয়ম অনুযায়ী):

- **নিজের ডেটা:** সবসময়, সম্পূর্ণ, live/real-time (নিজের user doc, নিজের posted problem, নিজের
  solved job, নিজের bid, নিজের transaction)
- **Browsing feed:** সাম্প্রতিক ৩০০টা OPEN problem, live
- **Counterparty প্রোফাইল ও অন্যদের পোস্টে করা bid:** live listen করা হয় না (dynamic ID set
  Firestore listener-এর জন্য উপযুক্ত না) — periodic scoped pull দিয়ে freshness বজায় থাকে
  (app startup, pull-to-refresh, ১০-মিনিট stale-fallback)
- **Admin session:** সব collection-এ scoping বাদ, আগের মতো পুরো ডেটা লাইভ

**জানা trade-off:** কেউ নিজের পোস্টে নতুন bid পেলে, আগে সাথে সাথে দেখা যেত — এখন সেটা পরবর্তী
periodic sync পর্যন্ত (worst case ~১০ মিনিট) দেরি হতে পারে। সত্যিকারের real-time দরকার হলে সঠিক
সমাধান হলো ProblemDetailScreen-এ owner থাকা অবস্থায় শুধু সেই একটা problemId-এর জন্য আলাদা
screen-level listener বসানো, background sync-এ না।

**Admin metrics:** এখন `FirebaseSyncManager.refreshAdminMetricsViaAggregation()` দিয়ে
(Firestore `.count()`/`.aggregate(sum())`, কোনো document ডাউনলোড ছাড়াই) — পুরনো
`recalculateMetrics()`/`rawUsers`/`rawProblems`/`rawBids`/`rawTransactions`-নির্ভর হিসাব আর
listener থেকে feed হয় না (শুধু `withdrawals` listener এখনো বাকি — এখনো scope করা হয়নি)।

## ৮. Feed pagination — সম্পন্ন কাজের রেকর্ড (আংশিক, ২০২৬)

`SomadhanViewModel`-এ pagination infrastructure (`*Paged` state list + `loadNext*Page()` +
`reset*Pagination()`) আগে থেকেই ছিল, কিন্তু কিছু স্ক্রিন সেটা ব্যবহার না করে সরাসরি পুরো
`allProblems`/`openProblems` reactive Flow থেকে রেন্ডার করছিল — অর্থাৎ infinite-scroll বাটন
pages লোড করত, কিন্তু সেই page-গুলো UI-তে কখনো দেখানো হতো না (dead work)।

ঠিক করা হয়েছে:
- **`AllOpenProblemsScreen`:** এখন `viewModel.openProblemsPaged` থেকে রেন্ডার করে (badge/tab
  count-এর জন্য আলাদাভাবে পুরো `allProblems` থেকে cheap filter+count হয়)
- **`SolverAllPostsScreen`:** filter/search না থাকলে `viewModel.solverAllPostsPaged` থেকে
  paginated রেন্ডার; search/category filter active থাকলে পুরো scoped `openProblems`-এ ফিরে যায়
  (নইলে শুধু ইতিমধ্যে লোড হওয়া পেজগুলোর মধ্যেই সার্চ হতো) — filter active থাকা অবস্থায়
  infinite-scroll footer হাইড থাকে

**এখনো বাকি ছিল / এখন করা হয়েছে:** `SolverCategoryPostsScreen`-এ একই bug ছিল
(`categoryEligibleProblems` পুরো `allProblems` থেকে রেন্ডার হতো, paginated buffer অব্যবহৃত
থাকত) — কিন্তু এখানে সমস্যা ছিল distance-based (lat/lng, `isProblemVisibleToSolver`)
filtering client-side হয় বলে সরাসরি DB pagination বসালে physical-job visibility ভেঙে যেত।

সমাধান করা হলো এভাবে:
- **DAO/Repository:** নতুন `getOpenProblemsByCategoryPage(categoryId, limit, offset)` যোগ
  হয়েছে (আগের `getOpenProblemsPage` global ছিল, category filter পরে client-side হতো —
  সেটা ছিল নিজেই একটা অদক্ষ পুরনো bug)
- **ViewModel:** `loadNextSolverCategoryPostsPage`/`resetSolverCategoryPostsPagination`
  এখন `solverLat`, `solverLon`, `radiusKm`, `isSolver` নেয়; প্রতিটা raw DB page distance
  দিয়ে filter হওয়ার পর যদি visible item কম পড়ে (sparse category), সর্বোচ্চ ৫টা অতিরিক্ত DB
  page পর্যন্ত আরও টেনে আনে যাতে এক স্ক্রলেই একটা পূর্ণ পেজ visible card পাওয়া যায়
- **Screen:** সার্চ না থাকলে `viewModel.solverCategoryPostsPaged` থেকে রেন্ডার; সার্চ চালু
  থাকলে (আগের মতোই) পুরো scoped+distance-filtered `categoryEligibleProblems`-এ সার্চ হয় এবং
  infinite-scroll footer হাইড থাকে। `LaunchedEffect(categoryId)` GPS-এর ঘন ঘন আপডেটে যেন
  বারবার reset না হয় সেজন্য ইচ্ছাকৃতভাবে `solverLat/solverLon`-কে key করা হয়নি — pull-to-
  refresh-এই fresh location দিয়ে reset হয়, বাকি সময় প্রথম লোডের লোকেশনেই pagination চলে।

**জানা সীমাবদ্ধতা:** DB-তে `categoryId` দিয়ে filter হয় ঠিকই, কিন্তু physical-distance
filter এখনও client-side (SQLite-এ haversine নেই) — তাই খুব sparse/দূরের ক্যাটাগরিতে একটা
page load-এ কখনো কখনো ৫ বারের বেশি round-trip লাগলে ওই কলে কম আইটেম যোগ হবে, পরের স্ক্রলে
বাকিটা আসবে (crash/data-loss না, শুধু loading একটু ধীর মনে হতে পারে)।

## ৯. Live listener বনাম on-demand pull — সম্পন্ন কাজের রেকর্ড (সেপ্টেম্বর ২০২৬)

**সমস্যা যেটা পাওয়া গিয়েছিল:** `withdrawals`, `categories`, `ratings`, `messages`,
`escrows`, `additional_charges`, `reputation_events`, `notifications`, `platform_settings`,
`faqs`, `gateway_payments` — এই ১১টা collection-এর `addSnapshotListener` **কোনো
`isCurrentUserAdmin` চেক ছাড়াই, প্রতিটা session-এ (admin হোক বা সাধারণ user)** পুরো collection
unscoped live listen করত। প্রতিবার app restart/login/pull-to-refresh-এ এই ১১টা listener নতুন
করে attach হতো, আর প্রতিটা attach মানেই পুরো collection আবার document-বাই-document read হিসেবে
billable হওয়া। এটাই ছিল read quota exceed হওয়ার মূল কারণ — active connection মাত্র ১-২টা হলেও
snapshot listener count 30-40+ দেখাচ্ছিল, কারণ প্রতিটা session-ই ১৫-২০টা করে listener বহন করছিল।

**সমাধান — প্রতিটা collection-কে দুই ভাগে ভাগ করা হয়েছে:**

- **সত্যিকারের time-sensitive (live রাখা হয়েছে, কিন্তু scoped):** `messages` (active চ্যাটে
  reply-র জন্য অপেক্ষা), `escrows` (টাকা hold/release হওয়া), `gateway_payments` (payment
  confirm হওয়ার অপেক্ষা) — এই ৩টাতে এখন `isCurrentUserAdmin` অনুযায়ী branch করা হয়েছে: admin
  session-এ পুরো collection live (monitoring/dispute-এর জন্য সত্যিই দরকার), সাধারণ user
  session-এ শুধু নিজের সাথে সম্পর্কিত অংশ (`whereEqualTo` দিয়ে scoped) live।
- **Time-sensitive না (listener পুরোপুরি সরানো হয়েছে, on-demand pull-এ বদলানো হয়েছে):**
  `withdrawals`, `categories`, `ratings`, `additional_charges`, `reputation_events`,
  `notifications`, `platform_settings`, `faqs` — এগুলো এখন `startRealtimeListeners()` চলার
  সময় একবার one-shot `.get()` (বিদ্যমান `pullXxx()` helper ফাংশন পুনর্ব্যবহার করে) দিয়ে টেনে
  আনা হয়, কোনো খোলা socket রাখা হয় না। Freshness আসে app startup, pull-to-refresh
  (`triggerCloudSync()`/`pullAllCloudDataToLocal()`), আর প্রতিবার login/role-switch-এ
  `startRealtimeListeners()` আবার চলা থেকে।

**জানা trade-off:** withdrawal approve/reject, notification, category/FAQ পরিবর্তন — এগুলো
এখন instant push না হয়ে পরবর্তী app-open/pull-to-refresh পর্যন্ত দেরি হতে পারে। বেশিরভাগ ক্ষেত্রে
এটা সমস্যা না (কেউ ২৪ ঘণ্টা admin dashboard খুলে বসে নোটিফিকেশনের জন্য stare করে না)। কোনো
নির্দিষ্ট admin স্ক্রিনে সত্যিই instant push দরকার মনে হলে, সঠিক সমাধান হলো সেই একটা স্ক্রিন খোলা
থাকা অবস্থায় screen-level scoped listener বসানো (bids/problems-এর counterparty trade-off-এর
জন্য উপরে যেমন বলা হয়েছে) — পুরো background sync manager-এ সব সময়ের জন্য না।

**নতুন কোনো collection/listener যোগ করার সময় মনে রাখবেন:** "লাইভ দরকার" মানে "সত্যিই second-by-
second push দরকার" — dashboard/history/reference-টাইপ ডেটার জন্য প্রায় সবসময় on-demand pull
যথেষ্ট এবং সস্তা। `isCurrentUserAdmin` চেক ছাড়া কোনো collection-wide listener কখনোই লেখা উচিত না
(§২-এর নিয়মের মতোই) — admin-এর জন্যও, কারণ সত্যিই live দরকার কিনা সেটা আলাদাভাবে যাচাই করা উচিত।

---

## ১০. Admin-এর "unscoped-কিন্তু-live" listener-গুলো bound করা (সেপ্টেম্বর ২০২৬, পার্ট ২)

§৯-এ যে ৭টা collection admin-এর জন্য ইচ্ছাকৃতভাবে unscoped live রাখা হয়েছিল (`users`,
`problems`, `bids`, `transactions`, `escrows`, `messages`, `gateway_payments`) — সেগুলোর একটা
লুকানো স্কেলিং সমস্যা ছিল: **initial-snapshot cost সরাসরি সেই collection-এর মোট document
সংখ্যার সমানুপাতিক**। প্ল্যাটফর্মে ইউজার/ডেটা যত বাড়বে, admin app খোলার প্রতিবার তত বেশি read
হবে — এটা ঠিক সেই একই সমস্যার ভবিষ্যৎ-স্কেল ভার্সন যেটার জন্য প্রথমবার read quota exceed
হয়েছিল।

**সমাধানের মূল ধারণা:** admin-এর "সব ডেটা লাইভ" আসলে দরকার না, দরকার **"যেটাতে এখনই action
নিতে হবে সেটা লাইভ, বাকিটা periodic/on-demand"**। প্রতিটা collection-এর live listener-কে দুই
রকমের bound-এর একটা দিয়ে সীমাবদ্ধ করা হয়েছে, যাতে collection যতই বড় হোক, listener-এর cost
বাড়ে না:

| Collection | Live bound (bounded, সবসময় সস্তা) | পুরো history কীভাবে পাওয়া যায় |
|---|---|---|
| `users` | `kycStatus == "pending"` (KYC queue) | `pullUsers()` — unscoped one-shot, startup/refresh/login-এ |
| `problems` | `isDisputed == true` + `hasReleaseRequest == true` (দুটো আলাদা query) | `pullProblems()` — same |
| `bids` | সাম্প্রতিক ৫০০টা (`orderBy(createdAt DESC).limit(500)`) | `pullBids()` — same |
| `transactions` | সাম্প্রতিক ৫০০টা | `pullTransactions()` — same |
| `escrows` | `status == "HELD"` (টাকা এখনও আটকে আছে) | `pullEscrows()` — same |
| `messages` | সাম্প্রতিক ৫০০টা (platform-wide monitoring feed) | নির্দিষ্ট conversation/problemId ধরে on-demand pull (dispute-এ ঢোকার সময়) |
| `gateway_payments` | `status == "PENDING"` (এখনও confirm হয়নি) | `pullGatewayPayments()` — same |

**কেন এটা নিরাপদ:** existing unscoped one-shot pull ফাংশনগুলো (`pullUsers`, `pullProblems`
ইত্যাদি) touch করা হয়নি — সেগুলো এখনও পুরো collection টেনে Room-এ ভরে, আগের মতোই
`pullAllCloudDataToLocal()`-এর মাধ্যমে app startup, pull-to-refresh, আর login-এ চলে। তাই
`AdminUsersView`/`AdminProblemsView`/ইত্যাদির মতো screen-এ browse/search আগের মতোই কাজ
করবে — শুধু **সবসময় খোলা থাকা socket-টা** এখন bound করা, যেটা প্রতি doc-change-এ (এমনকি
যেটাতে admin-এর কোনো আগ্রহই নেই) re-cost করত।

**⚠️ এখনও যেটা bound করা হয়নি (পরবর্তী ধাপ, যদি দরকার মনে হয়):** `pullUsers()`,
`pullProblems()` ইত্যাদি নিজেরাও এখনো **unscoped one-shot** — মানে এগুলোও প্ল্যাটফর্ম অনেক
বড় হয়ে গেলে (লাখো user/problem) নিজেরাই ভারী হয়ে উঠতে পারে, যদিও এগুলো প্রতি doc-change-এ না,
শুধু login/pull-to-refresh-এ চলে (অনেক কম frequent)। এর প্রকৃত সমাধান হলো এই one-shot
pull-গুলোকেও pagination-এ (`.limit()` + `startAfter()` + "Load more" UI) নিয়ে যাওয়া, যেটা
`AdminUsersView`-এ আংশিক আগে থেকেই আছে (§৪-এ উল্লেখ) — বাকি admin screen-গুলোতে এই প্যাটার্ন
বিস্তৃত করাই পরবর্তী optimization হওয়া উচিত, ডেটা যদি সত্যিই বড় স্কেলে পৌঁছায়।

---

## ১১. Admin panel screen — দুইটা আলাদা thread (সেপ্টেম্বর ২০২৬)

এই section-এ দুইটা সম্পূর্ণ আলাদা optimization thread-এর কাজ হয়েছে admin panel নিয়ে। গুলিয়ে
ফেলবেন না — একটা memory/CPU নিয়ে, আরেকটা ছিল UI-render নিয়ে যেটা শেষে বেশিরভাগ ক্ষেত্রেই
প্রয়োজনই ছিল না বলে প্রমাণিত হয়েছে।

### থ্রেড A — DB-level pagination (next/prev, scroll-to-load) — বন্ধ, বেশিরভাগ ক্ষেত্রে অপ্রয়োজনীয় প্রমাণিত

**শুরুর premise ছিল:** `SomadhanViewModel`-এ প্রায় প্রতিটা বড় collection-এর জন্য
Room-level paged DAO query আগে থেকেই লেখা ছিল (`getAllXxxPage(limit, offset)`), কিন্তু
admin screen-গুলো সেটা ব্যবহার না করে পুরো (fully Room-loaded) list-এর উপর client-side
"পেজ N" navigation করছিল। ধারণা ছিল DB pagination LazyColumn-এ হাজার হাজার item রেন্ডার
করার জ্যাঙ্ক/মেমোরি সমস্যা ঠিক করবে।

**যা আসলে পাওয়া গেল, স্ক্রিন-বাই-স্ক্রিন যাচাই করে:**

- **`AdminUsersView`** ✅ এবং **`AdminWithdrawalsView`** ✅ — সত্যিই কাজ করেছে, কারণ এই দুটো
  screen-এ কোনো "মোট X" ধরনের header/badge নেই যেটার জন্য পুরো লিস্ট লাগে। `adminUsersPaged`/
  `loadNextAdminUsersPage()`/`resetAdminUsersPagination()` আর সমতুল্য withdrawals ফাংশন
  দিয়ে বাস্তব DB paging হয় (unfiltered browse-এ), filter/search চালু থাকলে পুরনো
  in-memory আচরণে ফিরে যায় (§৮-এর precedent)।

  ⚠️ **এখানে একটা bug এড়ানো হয়েছে:** `loadNextTransactionsPage`/`resetTransactionsPagination`
  আর সমতুল্য withdrawals ফাংশন `TransactionHistoryScreen`/`WithdrawalHistoryScreen`-এর জন্য
  লেখা (একজন নির্দিষ্ট solver-এর নিজের history) — `solverId=null` দিলে `currentUser.id`-এ
  resolve হয়, admin হলে এটা **admin-এর নিজের** transaction/withdrawal দেখাত, সবার না।
  তাই admin-এর জন্য সম্পূর্ণ আলাদা `adminWithdrawalsPaged` ফাংশন লেখা হয়েছিল, পুরনোগুলো reuse
  করা হয়নি। **ভবিষ্যতে কোথাও `solverId`-based paging reuse করতে গেলে এই ফাঁদ মনে রাখবেন।**

- **`AdminCancelledBidsView`**, **`AdminChatMonitoringView`**, **`AdminRatingsView`**,
  **`AdminAdditionalChargesView`**, **`AdminEscrowView`**, **`AdminGatewayPaymentsView`**,
  **`AdminManualNotificationView`** — সবগুলোতে DB pagination-এর **কোনো real benefit নেই**,
  একই কারণে:
  1. LazyColumn-গুলো ইতিমধ্যেই client-side paginated slice (`paginatedXxx`, ১০টা/পেজ)
     render করে — জ্যাঙ্ক সমস্যা এমনিতেই নেই।
  2. প্রতিটাতেই header/tab-label-এ পুরো (unfiltered) লিস্টের total count দেখানো হয় —
     যেমন Escrow-এর ট্যাব লেবেল "HELD (X) / সম্পন্ন (Y) / বাতিল (Z)", Ratings/Charges/
     GatewayPayments-এ "মোট X" — filter অবস্থা যাই হোক না কেন এই count-এর জন্য পুরো লিস্ট
     লাগে, তাই DB-only-page-লোড করলে এই সংখ্যাগুলো ভুল দেখাবে বা আলাদা query লাগবে যেটা
     আবার পুরো ডেটাই টানবে।
  3. Search সব সময় পুরো লিস্টের উপর চলে (message content, solver name, ইত্যাদি) — আংশিক
     ডেটায় সঠিক search সম্ভব না (§৮-এর একই সীমাবদ্ধতা)।

  `AdminTransactionsView`-ও এই একই কারণে আগে ইচ্ছাকৃতভাবে skip করা হয়েছিল ("totals পুরো
  data লাগে")। **`AdminReputationEngineView`** checklist-এ ভুলভাবে এসেছিল — এটা মূলত static
  predefined event blueprint + কিছু custom key, "বড় লিস্ট browse" ধরনের screen-ই না।

**সিদ্ধান্ত: থ্রেড A এখানেই বন্ধ।** যে ২টা স্ক্রিনে সত্যিই সুবিধা ছিল (Users, Withdrawals)
সেটা আগেই করা হয়ে গেছে। বাকি ~২০টা admin screen-এ DB-level pagination না করাই সঠিক — কোড
জটিলতা বাড়াবে, বাস্তব memory/CPU/UX উপকার দেবে না। ভবিষ্যতে কোনো নতুন admin screen বানানোর
সময় এই checklist অনুসরণ করে সময় নষ্ট করবেন না — বরং নিচের প্রশ্নটা জিজ্ঞেস করুন: **"এই
screen-এ কি filter-independent কোনো total/count দেখানো হয়, বা পুরো ডেটায় search করতে হয়?"**
হ্যাঁ হলে DB pagination-এর মানে নেই।

### থ্রেড B — parent-level hoisted state lazy করা (সম্পন্ন ✅)

`AdminPanelScreen.kt`-এ ~১৫টা `collectAsStateWithLifecycle()` কল parent-level-এ hoisted
ছিল (সব tab-এর কমন প্যারেন্টে) — Tab ০ (Stats Dashboard, panel খুললে প্রথমে যেটা দেখায়) নিজেই
`allUsers`, `allProblems`, `allTransactions`, `allHeldEscrows`, `allAdditionalCharges` টেনে
নেয়, তাই এই ৫টা lazy করার মানে নেই (hoisted-ই থাকা উচিত, এবং আছে)।

বাকি ১০টা variable — যেগুলো Tab ০ ছোঁয় না — parent hoisting থেকে সরিয়ে প্রতিটা ব্যবহারকারী
`when (selectedTabIndex)` branch-এর ভেতরে local `collectAsStateWithLifecycle()` হিসেবে
বসানো হয়েছে, যাতে admin ওই tab-এ না গেলে Room query চলবেই না (সব variable-ই ViewModel-এ
`SharingStarted.WhileSubscribed(5000)` দিয়ে আছে, তাই সরানোর ফলে বাস্তবেই lazy হলো):

- **Single-tab usage (৩টা):** `recentAuditLogs` (tab ১২), `allFaqsForAdmin` (tab ১৩),
  `allAdminNotifications` (tab ৩)
- **Multi-tab usage (৭টা):** `allCategories` (tab ৬, ২১), `allWithdrawals` (tab ২, ১৮),
  `allCancelledBids` (tab ১৪, ১৮), `allGatewayPayments` (tab ৯, ২৩), `allRatings`
  (tab ১০, ১৮), `allReleasedEscrows` (tab ৭, ২১), `allRefundedEscrows` (tab ৭, ১৮, ২১ —
  move করার আগে verify করতে গিয়ে দেখা গেল এটা আসলে ৩-tab usage, আগে ২ ধরা হয়েছিল)

**উল্লেখযোগ্য:** Tab ১৮ (`AdminUserLookupView`) নিজেই একটা cross-cutting search screen —
`allWithdrawals`, `allRefundedEscrows`, `allCancelledBids`, `allRatings` সহ প্রায় সব বড়
list একসাথে টানে (অনেকটা Tab ০-এর মতোই, শুধু default landing tab না বলে সবসময় ছোঁয়া হয় না)।
তাই এই ৪টার real lazy-loading benefit নির্ভর করে admin আদৌ User Lookup tab-এ যান কিনা তার উপর।

`AdminChatMonitoringView`-এর `allAdminMessages` আলাদা — এটা কখনোই parent-এ hoisted ছিল না,
নিজের composable-এর ভেতরেই `collectAsStateWithLifecycle()` করা, তাই এমনিতেই lazy ছিল,
কিছু করার দরকার হয়নি।

---

এই ফাইলটা কোনো automatic enforcement না — শুধু একটা checklist/reference, যাতে ভবিষ্যতে নতুন
কাজ করার সময় (নিজে বা অন্য কেউ) আগের সমাধান করা সমস্যাগুলো আবার তৈরি না হয়।


## ১২. Startup crash-proofing — init{} ব্লকের ৪টা step isolate করা (২০২৬)

**সমস্যা:** `SomadhanViewModel.init{}`-এ `ensureCategoriesSeeded()`, `ensureFaqsSeeded()`,
`checkAndProcess48HourAutoReleases()`, `checkAndExpireInstantJobs()` — এই ৪টা কোনো try/catch
ছাড়াই পরপর `viewModelScope.launch{}`-এর ভেতরে চলত। এর যেকোনো একটা exception ছুঁড়লে (যেকোনো
কারণে — bad Room migration, corrupted row, ইত্যাদি) পুরো coroutine crash করত, আর
`viewModelScope`-এ uncaught exception মানেই পুরো app crash — উপসর্গ ছিল "app খোলে, কিছুক্ষণ
চলে, তারপর বন্ধ হয়ে যায়"।

**সমাধান:** প্রতিটা step এখন নিজের `runCatching { }.onFailure { Log.e(...) }`-এ isolate করা —
একটা fail করলে সেটা log হয়ে skip হয়, বাকিগুলো ঠিকমতো চলে, app কখনো crash করে না এই ব্লক থেকে।

**সাথে যোগ হয়েছে:** `FirebaseSyncManager.isFirebaseConfigured()` — Firebase (google-services.json)
আসলেই init হয়েছে কিনা এক-লাইনে চেক করার পাবলিক ফাংশন, `init{}`-এর শুরুতে একবার call করে
logcat-এ স্পষ্ট warning দেয় যদি Firebase configured না থাকে। উল্লেখ্য: Firestore-touching সব
sync ফাংশন (`syncProblem`, `syncBid`, `syncNotification` ইত্যাদি) আগে থেকেই `requireDb()`
দিয়ে null-safe ছিল এবং নিজেদের try/catch-এ fire-and-forget (`syncScope.launch`) — তাই এই
ফাংশনগুলো Firebase মিসিং থাকলেও crash করাতো না। এই diagnostic শুধু silent failure-কে visible
করার জন্য, নতুন কোনো crash-path বন্ধ করার জন্য না।

**এখনো নিশ্চিত না:** এই isolate করাটা root cause fix করেছে কিনা তা actual crash log (logcat)
দিয়ে যাচাই করা হয়নি — এটা defensive hardening, guaranteed root-cause fix না।

## ১৩. Escrow mutation সবসময় escrow-id দিয়ে scope করুন, problemId দিয়ে না (২০২৬)

**সমস্যা:** `EscrowDao.addExtraAmount(problemId, ...)` আর `updateStatus(problemId, ...)` —
দুটোই `WHERE problemId = :problemId` দিয়ে লেখা ছিল, escrow-এর নির্দিষ্ট `id` দিয়ে না।
`acceptBid()`-এ ইচ্ছাকৃতভাবে **প্রতিটা bid-accept cycle-এ নতুন escrow row (নতুন id)** তৈরি হয়
(দেখুন সেই ফাংশনের কমেন্ট) — তাই একই `problemId`-তে সময়ের সাথে একাধিক escrow row জমা হওয়া
স্বাভাবিক (যেমন: প্রথম solver বাতিল করলে তার row REFUNDED থেকে যায়, পরের solver-এর জন্য নতুন
HELD row তৈরি হয়)। `problemId`-scoped UPDATE একসাথে সব row touch করত — ফলে দ্বিতীয় solver-এর
extra bill accept করলে পুরনো (ইতিমধ্যে REFUNDED) row-এও ভুলভাবে extraAmount/status বদলে যেত।

**সমাধান:** `addExtraAmountById(escrowId, ...)` — id-scoped নতুন query যোগ করা হয়েছে;
`addToEscrow()` এখন প্রথমে `getByProblemId()` দিয়ে বর্তমান (সর্বশেষ) escrow row resolve করে,
তারপর সেই id দিয়ে আপডেট করে। `releaseEscrow(problemId)`-ও একইভাবে ঠিক করা হয়েছে — এটা এখন
আগে থেকেই থাকা id-scoped `escrowDao.releaseEscrow(escrowId, timestamp)` ব্যবহার করে, পুরনো
problemId-scoped `updateStatus()` না (সেটা `@Deprecated` মার্ক করা হয়েছে, ভবিষ্যতে নতুন কোনো
কলার যোগ করার আগে এই নোট পড়ুন)।

**নিয়ম:** escrows টেবিলে যেকোনো নতুন write লেখার সময় সবসময় escrow-এর `id` দিয়ে scope করুন,
`problemId` দিয়ে না — `refundEscrowOnce()`/`adminReleaseEscrow()`/reconcile-এর Case ১ ইতিমধ্যে
এই প্যাটার্ন সঠিকভাবে মেনে চলে, নতুন কিছু লেখার সময় এদের অনুসরণ করুন।

## ১৪. Realtime listener rebuild — ঘন ঘন login/logout/role-switch-এ debounce ও snapshot (২০২৬)

**সমস্যা (স্লো হয়ে যাওয়া):** `FirebaseSyncManager.setCurrentUserId()` প্রতিবার (login, logout,
session restore, role-switch — সবক্ষেত্রেই) call হয়, আর প্রতিবারই একটা নতুন
`startRealtimeListeners()` কোরুটিন ছোঁড়ে, যেটা `realtimeListenersMutex`-এর পেছনে queue হয়ে
একে একে চলে। কেউ ঘন ঘন role/login বদলালে (একই ডিভাইসে বারবার) প্রতিটা switch তার নিজের একটা
সম্পূর্ণ listener teardown-and-rebuild pass (~১৫-২০টা আসল Firestore `addSnapshotListener()`
কল) queue-তে যোগ করত — শুধু সর্বশেষটার ফলাফলই আসলে দরকার ছিল, কিন্তু বাকিগুলোও পুরোপুরি চলত।
এটাই ছিল ঘন ঘন role change-এর পর app স্লো হয়ে যাওয়ার মূল কারণ।

**সমাধান:** `setCurrentUserIdGeneration` কাউন্টার যোগ হয়েছে — প্রতিটা `setCurrentUserId()` কল
নিজের generation number নেয়, ২৫০ms debounce-এর পর যদি এই generation-ই এখনো সর্বশেষ থাকে
তবেই আসল rebuild চালায়; এর মধ্যে আরেকটা কল এসে গেলে এই কলটা চুপচাপ skip করে (নতুন কলটাই
rebuild করবে)।

**সমস্যা (mismatch-এর সম্ভাবনা):** `currentAppUserId`/`isCurrentUserAdmin` — দুটোই shared
`@Volatile var`, আর `startRealtimeListenersLocked()`-এর ভেতরে এগুলো ৭-৮ বার আলাদা আলাদা
জায়গায় (users/problems/bids/messages/transactions/escrows/gateway সেকশনে) সরাসরি পড়া হতো।
এই একটা rebuild pass চলাকালীন যদি অন্য কোনো `setCurrentUserId()` কল এসে এই var বদলে দেয়
(rapid switching-এ এটা বাস্তবসম্মত), তাহলে একই pass-এর মধ্যে ভিন্ন ভিন্ন সেকশন ভিন্ন ভিন্ন
user-এর জন্য scope হয়ে যেতে পারত — users-listener এক user-এর, problems-listener আরেক user-এর।

**সমাধান:** `startRealtimeListenersLocked()`-এর একদম শুরুতেই `snapshotUserId`/`snapshotIsAdmin`
নামে local val-এ একবার snapshot নেওয়া হয়, এবং পুরো ফাংশনের সব জায়গায় (৮টা সেকশনেই) সরাসরি
`currentAppUserId`/`isCurrentUserAdmin` না পড়ে এই snapshot ব্যবহার করা হয় — ফলে একটা rebuild
pass সবসময় শুরু থেকে শেষ পর্যন্ত একই user-এর জন্য consistent থাকে, মাঝপথে বদলে গেলেও না।

**নিয়ম:** কোনো suspend ফাংশনের ভেতরে যদি একটা shared mutable (`@Volatile`/singleton-level)
var একাধিকবার পড়ার দরকার হয়, ফাংশনের শুরুতে একবার local val-এ snapshot নিন — মাঝপথে সেই var
বদলে গেলেও পুরো ফাংশন internally consistent থাকবে।
