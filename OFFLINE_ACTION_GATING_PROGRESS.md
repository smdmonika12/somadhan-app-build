# Offline Action Gating — Progress Log

সংশ্লিষ্ট ফাইল: `somadhan-offline-action-guard-master-prompt.md` (মাস্টার প্রম্পট, ১৪ ধাপ),
`OFFLINE_ACTION_GATING_INVENTORY_STEP1.md` (ধাপ ১-এর আউটপুট)।

---

## ধাপ ১ — ইনভেন্টরি ও ক্যাটাগরাইজেশন — ✅ সম্পূর্ণ (আগের সেশনে)

কোনো কোড পরিবর্তন হয়নি। আউটপুট: `OFFLINE_ACTION_GATING_INVENTORY_STEP1.md` (গ্রুপ A ১৩টা
Outbox-covered RPC, গ্রুপ B ডোমেইন-ব্যাচ অনুযায়ী, গ্রুপ C read-only)।

**যাচাই দরকার সেকশনের ২টা প্রশ্নের ইউজার-কনফার্মেশন (এই সেশনে):**
1. Cron/system-triggered RPC (`expire_broadcasting_instant_job`,
   `system_notify_48hour_auto_release` ইত্যাদি) — যেহেতু এগুলো UI action trigger করে না, তাই
   offline-এ এভাবেই (guard ছাড়া) প্রসেস হতে থাকবে ধরে নিয়ে এগোনো হচ্ছে। প্রতিটা domain-ব্যাচে
   (ধাপ ৫–১১) আবার double-check হবে যে সত্যিই কোনো UI বাটন এদের সরাসরি কল করে না।
2. `admin_wipe_all_data` — ইউজার কনফার্ম করেছেন এটা "খুব বেশি strong network নির্ভর করবে,
   network না থাকলে action trigger-ই হবে না" — অর্থাৎ সবসময় সম্পূর্ণ ব্লক (কোনো retry/queue না)।
   ধাপ ১১-এ এই আচরণ implement হবে।

---

## ধাপ ২ — Admin Toggle ইনফ্রাস্ট্রাকচার — ✅ সম্পূর্ণ (এই সেশনে)

### যা পাওয়া গেছে (কোড পড়ে/গ্রেপ করে, এডিটের আগে)
- `platform_settings` টেবিলের জন্য Room-এ ইতিমধ্যেই `PlatformSettingEntity` (key/value) +
  `PlatformSettingDao` (getAllSettings Flow, getSetting suspend, insertSetting) আছে।
- `SomadhanRepository.updatePlatformSetting(key, value)` ইতিমধ্যেই established local-write +
  Supabase dual-write (`SupabaseSyncManager.upsertPlatformSetting`) + admin-audit-log প্যাটার্ন —
  নতুন কিছু বানাতে হয়নি, সরাসরি reuse করা হয়েছে।
- `SupabaseSyncManager.getPlatformSetting(key)` (single-row cloud read) আগে থেকেই ছিল কিন্তু
  codebase-এ কোথাও caller ছিল না (dead code) — এই ধাপে প্রথমবার ব্যবহার করা হলো।
- ⚠️ **গুরুত্বপূর্ণ pre-existing gap আবিষ্কৃত (কোড-কমেন্টে আগে থেকেই নথিভুক্ত,
  `SomadhanRepository.kt` লাইন ~১৭৪০):** `platform_settings`-এর জন্য কোনো bulk cloud→Room
  pull নেই — শুধু local write (dual-write সহ) আর local read। অর্থাৎ Admin এক ডিভাইস থেকে কোনো
  platform_settings key বদলালে, অন্য ডিভাইস (বা ওই admin-এর নিজের অন্য ডিভাইস/fresh install)
  সেই পরিবর্তন **কখনো নিজে থেকে পেত না**। rule ৪ অনুযায়ী `strict_offline_block`-এর জন্য এটা
  critical (এই key-টার ভুল/পুরনো cache মানে ভুল মোডে চলা), তাই এই ধাপে **শুধু এই একটা key-র
  জন্য** targeted single-row cloud-pull যোগ করা হলো (পুরো bulk-pull ফাংশন না বাড়িয়ে, যেটা
  ঝুঁকিপূর্ণ ও স্কোপের বাইরে হতো)।
- `AdminSettingsView.kt` (drawer আইটেম "সেটিংস") আগে থেকেই friendly admin settings UI —
  মাস্টার প্রম্পটের ধারণা ("শুধু raw AdminSupabaseExplorerView আছে") এই কোডবেসে সঠিক না, তাই
  raw explorer-এ হাত না দিয়ে বিদ্যমান `AdminSettingsView.kt`-তেই নতুন টগল কার্ড যোগ করা হলো
  (Maintenance Mode/Extra Bill Commission টগলের ঠিক একই established প্যাটার্ন অনুসরণ করে)।

### কী পরিবর্তন হলো (৩টা ফাইল, নতুন কোনো ফাইল/import লাগেনি)

1. **`SomadhanRepository.kt`** — `updatePlatformSetting()`-এর ঠিক পরে নতুন suspend ফাংশন
   `syncStrictOfflineBlockSettingFromCloud()` যোগ হলো: `SupabaseSyncManager.getPlatformSetting
   ("strict_offline_block")` কল করে, non-null হলে Room-এ `insertSetting()` দিয়ে cache আপডেট
   করে। ব্যর্থ হলে (network নেই) silently local cache অপরিবর্তিত থাকে, exception swallow করে
   শুধু `Log.w` — বিদ্যমান dual-write ব্যর্থতার প্যাটার্নের সাথে সামঞ্জস্যপূর্ণ।

2. **`SomadhanViewModel.kt`** — তিনটা যোগ:
   - `isStrictOfflineBlockEnabled: StateFlow<Boolean>` — `allPlatformSettings` (Room Flow)
     থেকে derive করা, key না পেলে ডিফল্ট `true` (backward-compatible)। MainActivity ধাপ ৩-এ
     এটা collect করে branch করবে (এই ধাপে এখনো ব্যবহার হচ্ছে না)।
   - App-startup init কোরুটিনে (অন্য `runCatching { repository.xxx() }` কলগুলোর ঠিক পাশে) একটা
     নতুন লাইন: `repository.syncStrictOfflineBlockSettingFromCloud()` — প্রতিবার app খোলার
     সময় চলে (bulk-sync থ্রেশহোল্ডের সাথে বাঁধা না, কারণ এটা single-row, সস্তা কল)।
   - `completeLoginAfterOtp()`-এ, `migrateLegacyDualRowsIntoRoot()`-এর ঠিক পরে, একই কল —
     fresh login-এর মুহূর্তেও (fresh install-সহ) সাথে সাথে সঠিক cloud মান cache হয়।
   - Save-path-এর জন্য কোনো নতুন ফাংশন লাগেনি — বিদ্যমান
     `adminUpdatePlatformSetting("strict_offline_block", "true"/"false")` সরাসরি reuse হয়েছে।

3. **`AdminSettingsView.kt`** — "সেটিংস" ট্যাবে (Maintenance Mode কার্ডের ঠিক পরে) নতুন
   "স্ট্রিক্ট অফলাইন ব্লক (Strict Offline Block)" কার্ড: Switch (default ON, `!= "false"`),
   Maintenance Mode-এর মতোই স্টাইল/রঙের প্যাটার্ন। কার্ডে একটা স্পষ্ট নোট আছে যে এই ধাপে টগল
   শুধু সংরক্ষিত হচ্ছে — MainActivity-এর প্রকৃত ব্লকিং আচরণ এখনো এর সাথে যুক্ত হয়নি (সেটা ধাপ
   ৩-এ হবে), যাতে সরাসরি admin-এর কনফিউশন না হয় যে toggle off করলে এখনই কিছু বদলাবে।

### ⚠️ জানা সীমাবদ্ধতা — টগল পরিবর্তনের propagation lag (ইউজারের সাথে আলোচনায় স্পষ্ট হয়েছে, ধাপ ১৪ QA-তে টেস্ট করা উচিত)

**রিস্ক ১ (মূল) — Admin-এর toggle পরিবর্তন instant/realtime না, প্রতিটা ডিভাইস নিজের পরের
"অনলাইন মুহূর্তে" গিয়ে নতুন মান পায়।**

উদাহরণ: সকাল ১০টায় admin (অনলাইন থেকে) toggle **OFF** করলেন — cloud + admin-এর নিজের ফোনের
Room দুটোতেই সাথে সাথে আপডেট হলো। কিন্তু রহিম নামের একজন ইউজার শেষ login করেছিলেন গতকাল রাতে
(তখন toggle ON ছিল) — তার ফোনের Room cache-এ এখনো `"true"` বসে আছে। সকাল ১০:৩০-এ রহিম
সিগন্যাল-হীন এলাকায় গিয়ে অফলাইনে অ্যাপ খুললে, তার ফোন এখনো পুরনো `"true"` মান অনুযায়ী পুরনো
strict full-block আচরণই দেখাবে (ধাপ ৩ implement হওয়ার পরও) — কারণ cloud-pull করতে নেটওয়ার্ক
লাগে, আর অফলাইন হয়ে যাওয়ার আগে সে অ্যাপ খোলেননি। রহিমের ফোন নতুন মান পাবে তার পরের বার
নেটওয়ার্ক-সহ app-open/login-এই (init/login hook চলবে তখন)। এটা bug না — rule ৪-এর local-cache
ডিজাইনের স্বাভাবিক trade-off (push-ভিত্তিক instant sync না থাকায়, শুধু pull-on-next-online) —
কিন্তু ধাপ ১৪-এর ম্যানুয়াল টেস্ট checklist-এ এই সময়ের ব্যবধানটা স্পষ্টভাবে অন্তর্ভুক্ত থাকা উচিত।

**রিস্ক ২ (কম গুরুত্বপূর্ণ, edge case) — একেবারে fresh install যেটা কখনো অনলাইন হয়নি।**

কেউ অ্যাপ ইনস্টল করেই সাথে সাথে ফ্লাইট মোডে গেলে (কখনো নেটওয়ার্ক পায়নি), তার Room-এ
`strict_offline_block` key-টাই নেই — fallback default `true` (Strict) প্রযোজ্য হবে, admin
আগেই OFF করে রাখলেও। এটা বর্তমান আচরণের চেয়ে খারাপ কিছু না (আগে থেকেই fresh install-এ
নেটওয়ার্ক ছাড়া ঢোকা যেত না), শুধু উল্লেখযোগ্য যে "OFF মোড"-এর সুবিধা পেতে প্রতিটা ডিভাইসকেই
অন্তত একবার অনলাইন হতে হবে।

**সম্পর্কিত ব্যবহারিক প্রভাব:** Toggle ON (ডিফল্ট) অবস্থায় `NoInternetOverlay` পুরো নেভিগেশন
গ্রাফে বসানো (শুধু Splash বাদে) — তাই সম্পূর্ণ অফলাইন অবস্থায় admin নিজেও login screen/admin
panel-এ ঢুকতে পারবেন না। Toggle OFF করতে admin-কে অন্তত একবার অনলাইন থেকে login → Admin Panel
→ সেটিংস-এ গিয়ে সুইচ করতে হবে — এটা toggle-এর মান cloud থেকেই আসে বলেই একটা inherent প্রয়োজনীয়তা,
bug না।

### ডিফল্ট মান — `'true'` (ব্যাকওয়ার্ড-কম্প্যাটিবল)
মাস্টার প্রম্পটের নিজস্ব সুপারিশ অনুযায়ী (লাইন ৫৬: "বর্তমান আচরণের সাথে ব্যাকওয়ার্ড-কম্প্যাটিবল
রাখতে ডিফল্ট 'true' রাখাই নিরাপদ") এই ডিফল্ট ব্যবহার করা হয়েছে — cloud-এ কোনো row না থাকলেও
(নতুন key, এখনো admin কখনো ছোঁননি) app-সাইড ফলব্যাক সবসময় `true`। **cloud `platform_settings`
টেবিলে কোনো নতুন migration/seed row লেখা হয়নি** — টেবিলটা আগে থেকেই generic key-value (কোনো
স্কিমা পরিবর্তন লাগে না), আর প্রথম admin toggle-ই (`updatePlatformSetting` দিয়ে upsert) cloud
row তৈরি করে দেবে। Supabase প্রজেক্টে সরাসরি কোনো লাইভ DB পরিবর্তন এই সেশনে করা হয়নি (Supabase
MCP connector থাকলেও ব্যবহার করা হয়নি — এই ধাপ শুধু অ্যাপ-সাইড কোড, স্কোপ ছোট রাখতে)।

### ⚠️ একটা ছোট নুয়্যান্স — পরের ধাপের জন্য নোট
Admin panel-এর এই নতুন টগল-সেভ action নিজেই একটা network-writing action (অন্য সব platform
setting-এর মতোই), কিন্তু এটা মূল ইনভেন্টরিতে (ধাপ ১) তালিকাভুক্ত ছিল না (তখন এই ফিচার
ছিল না)। বর্তমানে অন্য কোনো platform-setting সেভের মতোই এটাতেও কোনো offline-guard নেই (admin
offline হলে local Room-এ সেভ হয়ে যাবে, cloud dual-write নীরবে fail করবে, পরে sync হবে না
যতক্ষণ না আবার touch করা হয়)। এটা নতুন bug না — বিদ্যমান সব admin-settings সেভের identical
pre-existing সীমাবদ্ধতা। ধাপ ১১ (Admin misc)-এ এই toggle-save action-টাকেও (নতুন,
ইনভেন্টরির বাইরে পাওয়া একটা ছোট Group-B-সদৃশ আইটেম হিসেবে) `requireOnlineOrWarn()` গার্ড
দেওয়া বিবেচনা করা উচিত কিনা, সেটা তখন কনফার্ম করে নেওয়া হবে।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
তিনটা এডিট করা ফাইলে (`AdminSettingsView.kt`, `SomadhanRepository.kt`, `SomadhanViewModel.kt`)
`()`/`{}`/`[]` — এডিটের আগে-পরে ওভারঅল imbalance-এর delta = 0 সব ক্ষেত্রে (আগে থেকে থাকা কিছু
`()` imbalance শুধু বাংলা কমেন্টের বন্ধনী-টেক্সট থেকে আসা noise, edit-এর সাথে সম্পর্কহীন, তুলনা
করে যাচাই করা হয়েছে)। প্রতিটা নতুন যোগ হওয়া ব্লক আলাদাভাবেও (isolated) সম্পূর্ণ balanced।
আসল build ভেরিফাই Android Studio-তে হবে (rule ৯ অনুযায়ী)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
ধাপ ২-এর আগের zip (`somadhan-step8-outbox-ui-indicator.zip`, ২৯০টা ফাইল, dotfile-সহ) আর এই
ধাপ শেষের zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল হারায়নি/যোগ হয়নি।
শুধু উপরের ৩টা ফাইলের কন্টেন্ট বদলেছে। `.env`/`.gitignore` dotfile zip-এ অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে (আগের এন্ট্রি — এখন সম্পূর্ণ, নিচে দ্রষ্টব্য)
**ধাপ ৩ — `MainActivity.kt`-এর `NoInternetOverlay` ব্যবহারের জায়গা `isStrictOfflineBlockEnabled`
(এই ধাপে বানানো StateFlow) অনুযায়ী কন্ডিশনাল করা।** শুরুর আগে `MainActivity.kt`-এর বর্তমান
`NoInternetOverlay`/`manualOverrideConnected`/splash-exception লজিক সম্পূর্ণ পড়ে বুঝে নিতে হবে
(rule ১০)। কোনো 🟡 partial কাজ বাকি নেই এই মুহূর্তে — ধাপ ২ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## ধাপ ৩ — গ্লোবাল ব্লকিং লজিক টগল-অনুযায়ী শাখা করা — ✅ সম্পূর্ণ (এই সেশনে)

### শুরুর আগে যা পড়ে বোঝা হলো (rule ১০, কোড এডিটের আগে)
`MainActivity.kt`-এর `SomadhanAppNavigation()` সম্পূর্ণ পড়ে এই বিদ্যমান লজিক নিশ্চিত করা হলো:
- `isOnline = manualOverrideConnected ?: isNetworkConnected` — retry বাটনে manual override সেট
  হয়, real-time connectivity callback পরিবর্তন হলে আবার reset হয়ে যায় (null)।
- `isSplashScreen = currentRoute == null || currentRoute == Screen.Splash.route` — Splash
  screen-এ কখনো overlay দেখানো হয় না, exception।
- বিদ্যমান কল-সাইট: `NoInternetOverlay(isVisible = !isOnline && !isSplashScreen, isSolver, onRetry
  = { ... manualOverrideConnected = if (connected) true else null; connected })` — পুরো
  `SomadhanAppNavigation()`-এর একদম শেষে, `SomadhanActionBanner`-এর ঠিক পরে, Material3
  `Surface`-এর অন্তর্নিহিত `Box`-এর একটা sibling child হিসেবে (তাই আলাদা কোনো `Box`/align দরকার
  ছাড়াই এমনিতেই top-aligned, full-screen ওভারলে হিসেবে রেন্ডার হয়)।
- `NoInternetOverlay`/`NoInternetScreenContent` (`NoInternetScreen.kt`) — full-screen
  `Surface` + click-intercept (`clickable(onClick = {})`) দিয়ে নিচের UI ব্লক করে, রিলাই বোতাম
  আছে।

### কী পরিবর্তন হলো (২টা ফাইল)

1. **`NoInternetScreen.kt`** — নতুন composable `OfflineStatusBanner(isVisible, modifier)` যোগ
   হলো (বিদ্যমান `NoInternetOverlay`/`NoInternetScreenContent` অপরিবর্তিত, ডিলিট হয়নি —
   rule অনুযায়ী)। এটা একটা এজ-টু-এজ চিকন bar (`Surface` + `Row`, `fillMaxWidth` +
   `statusBarsPadding`, কোনো `fillMaxSize`/`clickable` intercept নেই — তাই নিচের UI পুরোপুরি
   ব্যবহারযোগ্য থাকে), `WifiOff` আইকন + "ইন্টারনেট নেই — শুধু আগের ডেটা দেখা যাচ্ছে" টেক্সট,
   dark bar background (`SomadhanTextPrimary`, `SomadhanActionBanner`-এর সাথে সামঞ্জস্যপূর্ণ
   স্টাইল কনভেনশন)। শুধু ১টা নতুন ইমপোর্ট (`statusBarsPadding`) লেগেছে।

2. **`MainActivity.kt`** — `SomadhanAppNavigation()`-এ:
   - নতুন `import ...OfflineStatusBanner`।
   - অন্য `collectAsStateWithLifecycle()` কলগুলোর পাশে `isStrictOfflineBlockEnabled` কালেক্ট
     করা হলো।
   - ফাইলের শেষের `NoInternetOverlay(...)` কলটা `if (isStrictOfflineBlockEnabled) { ... } else
     { ... }` দিয়ে wrap হলো — `true` (Strict, ডিফল্ট) হলে **হুবহু আগের** `NoInternetOverlay`
     কল (একই `isVisible`/`isSolver`/`onRetry` লজিক, কিছু পাল্টায়নি); `false` হলে নতুন
     `OfflineStatusBanner(isVisible = !isOnline && !isSplashScreen)`। `isOnline` (তাই
     `manualOverrideConnected` override-সহ পুরো লজিক) ও `isSplashScreen` exception দুই
     ব্রাঞ্চেই অভিন্নভাবে reuse হয়েছে — নতুন করে ডুপ্লিকেট করা হয়নি।

### ডিজাইন সিদ্ধান্ত — কেন retry বাটন নেই banner-এ
মাস্টার প্রম্পটের ধাপ ৩ বর্ণনায় শুধু একটা status text bar-এর কথা বলা হয়েছে (রিলাই বাটন না)।
যেহেতু non-strict মোডে ইউজার এমনিতেই পুরো অ্যাপ ব্যবহার করতে পারছে (block না), তাই আলাদা manual
retry বাটনের দরকার নেই — real-time connectivity observer এমনিতেই ফিরে এলে banner auto-hide করে
দেবে (`isVisible` সরাসরি `isOnline`-এর উপর নির্ভরশীল)। ভবিষ্যতে দরকার মনে হলে যোগ করা যাবে,
কিন্তু rule ১-এর "scope-এর বাইরে না যাওয়া" মেনে এই ধাপে যোগ করা হয়নি।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
দুটো এডিট করা ফাইলে (`MainActivity.kt`, `NoInternetScreen.kt`) `()`/`{}`/`[]` — এডিটের পরে
সম্পূর্ণ ফাইল-লেভেল ওভারঅল কাউন্ট প্রতিটাতেই সমান (delta = 0): `MainActivity.kt` → `()` 464/464,
`{}` 257/257, `[]` 4/4; `NoInternetScreen.kt` → `()` 112/112, `{}` 31/31, `[]` 2/2। আসল build
ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip (`somadhan-offline-action-gating-step2.zip`, ২৯৩টা ফাইল, dotfile-সহ) আর
এই ধাপ শেষের আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল
হারায়নি/যোগ হয়নি, শুধু উপরের ২টা ফাইলের কন্টেন্ট বদলেছে। `.env`/`.gitignore` dotfile
অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে (আগের এন্ট্রি — এখন সম্পূর্ণ, নিচে দ্রষ্টব্য)
**ধাপ ৪ — Reusable Online-Guard হেল্পার।** ViewModel-এ `requireOnlineOrWarn(): Boolean`-এর মতো
একটা শেয়ার্ড হেল্পার বানানো, যেটা গ্রুপ B-এর প্রতিটি action-কল করার আগে চেক করবে (offline +
toggle OFF হলে snackbar/toast দেখাবে, action এক্সিকিউট হবে না, স্ক্রিন/নেভিগেশন অক্ষত থাকবে)।
শুরুর আগে `SomadhanViewModel.kt`-এ existing toast/snackbar প্যাটার্ন (`uiToast` StateFlow,
`clearToast()` ইত্যাদি) এবং `isOnline`/connectivity-এর সাথে ViewModel-লেভেলে সংযোগ কীভাবে করা
যায় তা যাচাই করে নিতে হবে (rule ১০) — বর্তমানে connectivity observer শুধু `MainActivity.kt`
(Composable স্কোপে) আছে, ViewModel-এ নেই, তাই এই গ্যাপ কীভাবে পূরণ হবে তা ধাপ ৪-এর প্রথম কাজ।
কোনো 🟡 partial কাজ বাকি নেই — ধাপ ৩ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## ধাপ ৪ — Reusable Online-Guard হেল্পার — ✅ সম্পূর্ণ (এই সেশনে)

### ⚠️ শুরুর আগের অনুমান ভুল ছিল — কোড পড়ে সংশোধন হলো (rule ১০)
আগের এন্ট্রিতে ধরে নেওয়া হয়েছিল connectivity observer শুধু `MainActivity.kt`-এ আছে, ViewModel-এ
নেই। `SomadhanViewModel.kt` সম্পূর্ণ গ্রেপ করে দেখা গেল এই অনুমান ভুল: ViewModel-এ **ইতিমধ্যেই**
একটা স্বাধীন `_isOnline`/`isOnline: StateFlow<Boolean>` আছে (Loading/Sync Fix Roadmap v2 ধাপ
৭-এর জন্য আগে থেকে বানানো — offline→online transition-এ ERROR-এ আটকে থাকা SyncPhase-গুলো
retry করতে), নিজস্ব `ConnectivityManager.NetworkCallback` (`startConnectivityObserver()`,
`init{}`-এ চালু হয়, `onCleared()`-এ unregister হয়) দিয়ে। এটা MainActivity-এর
`NetworkConnectivityObserver` ইউটিলিটি ক্লাস থেকে সম্পূর্ণ আলাদা, স্বতন্ত্র ইনস্ট্যান্স —
দুটো এখন পাশাপাশি (redundant কিন্তু independent) চলে, একটাও ভাঙা হয়নি (rule ১)। যেহেতু এই
বিদ্যমান `isOnline` StateFlow-ই ঠিক দরকারি জিনিস (real-time, ViewModel-scope, ইতিমধ্যে সক্রিয়),
নতুন কোনো observer/util তৈরি করা হয়নি — শুধু reuse করা হলো।

### কী পরিবর্তন হলো (১টা ফাইল)
**`SomadhanViewModel.kt`** — বিদ্যমান `showToast(message: String)`-এর ঠিক পরে নতুন public
ফাংশন:
```kotlin
fun requireOnlineOrWarn(
    message: String = "ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না"
): Boolean {
    if (!isOnline.value) {
        showToast(message)
        return false
    }
    return true
}
```
- মাস্টার প্রম্পটের সাজেস্ট করা ডিফল্ট মেসেজ ("ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না")
  ব্যবহার করা হলো, সাথে একটা optional `message` প্যারামিটার (ধাপ ৫–১১-এর নির্দিষ্ট
  ডোমেইন/action-ভেদে দরকার হলে ভিন্ন মেসেজ দেওয়ার সুবিধার জন্য, যদিও কোনো caller এখনও
  ওভাররাইড করছে না — এই ধাপে শুধু ফাংশনটাই বানানো হলো, কোনো call-site-এ এখনো বসানো হয়নি)।
- টগল (`isStrictOfflineBlockEnabled`) এই ফাংশনের ভেতরে চেক করা হয়নি — মাস্টার প্রম্পটের নিজস্ব
  যুক্তি অনুযায়ী (ধাপ ৩-এর পর Strict/ON মোডে পুরো স্ক্রিনই `NoInternetOverlay`-এর আড়ালে অগম্য,
  তাই ইউজার কখনো এই কল পর্যন্ত পৌঁছাতেই পারবে না) — শুধু non-strict/OFF মোডেই এটা বাস্তবে
  ট্রিগার হবে।
- Money-critical action-গুলোর জন্যও (rule ২, toggle-independent সবসময় ব্লক) এই একই ফাংশন
  পুনঃব্যবহারযোগ্য — যেহেতু এটা টগল-independent শুধু real-time `isOnline` চেক করে, তাই আলাদা
  কোনো "money-critical ভ্যারিয়েন্ট" বানাতে হয়নি। ধাপ ৬/৭/১০-এ (wallet/bidding/dispute) এই
  একই `requireOnlineOrWarn()` কল হবে।
- এই ধাপে কোনো actual call-site-এ (গ্রুপ B-এর কোনো RPC/insert-এর আগে) guard বসানো হয়নি —
  শুধু reusable হেল্পার ফাংশন বানানো, মাস্টার প্রম্পট অনুযায়ী ধাপ ৫–১১-এ ডোমেইন-ভিত্তিক ব্যাচে
  বসানো হবে।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের পরে সম্পূর্ণ ফাইল-লেভেল ওভারঅল কাউন্ট: `()` 2890/2890, `{}`
1340/1340, `[]` 70/70 — সব delta = 0। আসল build ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip আর এই ধাপ শেষের আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ
অভিন্ন** — কোনো ফাইল হারায়নি/যোগ হয়নি, শুধু উপরের ১টা ফাইলের কন্টেন্ট বদলেছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে (আগের এন্ট্রি — এখন সম্পূর্ণ, নিচে দ্রষ্টব্য)
**ধাপ ৫ — Auth ডোমেইনে গার্ড ওয়্যারিং (Login, Register/OTP verify,
`complete_registration_profile`, Logout)।** শুরুর আগে এই ৪টা action-এর exact call-site
(কোন ফাইল/ফাংশন, কোন screen-এর কোন বাটন থেকে ট্রিগার হয়) `SomadhanViewModel.kt`/
`SupabaseAuthManager.kt`/সংশ্লিষ্ট `*Screen.kt`-এ খুঁজে পড়ে নিশ্চিত হতে হবে (rule ১০) — ধাপ
১-এর ইনভেন্টরি শুধু high-level ম্যাপিং, exact call-site কনফার্মেশন এখনো বাকি (ইনভেন্টরির নোট ৩
অনুযায়ী)। প্রতিটা call-site-এর ঠিক শুরুতে `if (!requireOnlineOrWarn()) return` (বা
suspend ফাংশনের ধরন অনুযায়ী উপযুক্ত variant) বসানো হবে, উপরে-নিচের eligibility/validation লজিক
অপরিবর্তিত রেখে। Logout সম্ভবত পুরোপুরি local/session-clear (network call নাই) হতে পারে —
কনফার্ম করে দরকার না হলে বাদ দেওয়া হবে। কোনো 🟡 partial কাজ বাকি নেই — ধাপ ৪ সম্পূর্ণ ও সেভাবেই
বন্ধ হচ্ছে।

---

## ধাপ ৫ — Auth ডোমেইনে গার্ড ওয়্যারিং — ✅ সম্পূর্ণ (এই সেশনে)

### ব্যবহারকারীর সাথে আলোচনা (এই ধাপ শুরুর আগে, ধাপ ৪-এর ফলো-আপ হিসেবে)
- অফলাইন-warning toast pull-to-refresh-এর "ডেটা রিফ্রেশ সম্পন্ন হয়েছে।"-এর মতো একই bar
  কিনা — কনফার্ম হলো এটা ইতিমধ্যেই তাই (`showToast()` → `_uiToast` → `SomadhanActionBanner`,
  দুটোই একই path)। কোনো কোড পরিবর্তন লাগেনি।
- Offline অবস্থায় action button visually লাল/disabled হবে কিনা — আলোচনার পর ব্যবহারকারী
  কনফার্ম করেছেন **শুধু toast (বর্তমান ডিজাইন) ঠিক আছে**, বাটনের visual state পাল্টানো হবে না।

### exact call-site কনফার্মেশন (rule ১০, কোড এডিটের আগে সম্পূর্ণ পড়ে)
`SomadhanViewModel.kt`/`LoginScreen.kt`/`OtpService.kt`/`SomadhanRepository.kt` পড়ে প্রতিটা
Auth action-এর real network-dependency যাচাই করা হলো:

| ফাংশন | নেটওয়ার্ক কল | গার্ড? |
|---|---|---|
| `validateLoginCredentials()` (Login বাটন) | `repository.loginWithPhonePassword` — Supabase Auth sign-in + cloud profile fetch, **কোনো local fallback নেই** | ✅ যোগ হলো |
| `register()` (Register বাটন) | `repository.registerUser` — Supabase sign-up + `complete_registration_profile` RPC + cloud fetch, কোনো fallback নেই | ✅ যোগ হলো |
| `sendOtp()` (OTP পাঠান/Resend, login+register+forgot-password তিন ফ্লোতেই শেয়ার্ড) | `OtpService.requestOtp` — SMS/Email গেটওয়ে HTTP কল | ✅ যোগ হলো |
| `verifyOtp()` | কোনো নেটওয়ার্ক কল নেই — `OtpService.verifyOtp()` সম্পূর্ণ local in-memory চেক | ❌ দরকার নেই |
| `completeLoginAfterOtp()` | `refreshUserDataFromCloud` (network), কিন্তু `?: user` দিয়ে already gracefully offline-safe fallback আছে | ❌ ইচ্ছাকৃতভাবে বাদ (নিচে দেখুন) |
| `logout()` | `SupabaseAuthManager.signOut()` fire-and-forget, local logout তার উপর নির্ভর করে না | ❌ ইচ্ছাকৃতভাবে বাদ (নিচে দেখুন) |
| `loginAsAdmin()` | `signInWithPhonePassword`, ইতিমধ্যেই try/catch দিয়ে graceful degrade করে | ❌ ইচ্ছাকৃতভাবে বাদ (নিচে দেখুন) |

**নতুন আবিষ্কার (ইনভেন্টরি সংশোধন):** `complete_registration_profile` RPC ধাপ ১-এর
ইনভেন্টরিতে আলাদা আইটেম হিসেবে তালিকাভুক্ত ছিল, কিন্তু কোড পড়ে দেখা গেল এটা কোনো স্বতন্ত্র
UI call-site না — `registerUser()`-এর ভেতরের একটা sub-step মাত্র। তাই `register()`-এর একটা
গার্ডই যথেষ্ট, আলাদা কিছু করতে হয়নি।

### ⚠️ কেন ৩টা ফাংশনে ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি (rule ১ রক্ষা করতে)
এই তিনটাই নেটওয়ার্ক কল করে, কিন্তু প্রতিটাই **ইতিমধ্যে বিদ্যমান graceful degradation** দিয়ে
ডিজাইন করা — এখানে হার্ড গার্ড বসালে বর্তমান আচরণ *খারাপ* হতো (rule ১ ভঙ্গ):
1. **`completeLoginAfterOtp()`** — ব্যর্থ হলে `?: user` fallback দিয়ে আগে-verify-করা local
   user দিয়েই session তৈরি হয়ে যায়। গার্ড বসালে OTP verify করার পরও ইউজারকে পুরো লগইন থেকে
   আটকে দিত।
2. **`logout()`** — local session-clear synchronous/unconditional, নিচের
   `SupabaseAuthManager.signOut()` একটা independent fire-and-forget কল যেটার ব্যর্থতা local
   logout-কে প্রভাবিত করে না। গার্ড বসালে **অফলাইনে ইউজার লগআউটই করতে পারত না** — নতুন, গুরুতর
   bug।
3. **`loginAsAdmin()`** — sign-in ব্যর্থ হলে (network না থাকা-সহ) already একটা "সীমিত ভিউ"
   সতর্কবার্তা দেখিয়ে local admin সেশন দিয়েই এগিয়ে যায় (ইচ্ছাকৃত, আগে থেকে ডকুমেন্টেড)। গার্ড
   বসালে admin অফলাইনে প্যানেলেই ঢুকতে পারত না।

এই তিনটার প্রতিটার doc-comment-এই ব্যাখ্যা যোগ করা হয়েছে যাতে পরবর্তী সেশন/ডেভেলপার বুঝতে পারে
এটা bug/missed-spot না, ইচ্ছাকৃত সিদ্ধান্ত।

### 📝 অতিরিক্ত আবিষ্কার (স্কোপের বাইরে, শুধু ডকুমেন্টেশন-সিঙ্ক, কোনো কোড পরিবর্তন নয়)
`loginAsAdmin()`-এর পুরনো doc-comment-এ একটা ⚠️ নোট ছিল যে normal user login/register-এ
E.164 phone normalize করা হয় না (likely broken bug)। এই ধাপে `SomadhanRepository.kt` পড়ার
সময় দেখা গেল এই নোটটা **stale** — `loginWithPhonePassword`/`registerUser`-এ
`OtpService.normalizeTarget()` দিয়ে E.164 normalize ইতিমধ্যেই যোগ করা হয়ে গেছে ("[ফিক্স -
ধাপ ৩৪ পরবর্তী]" কমেন্ট অনুযায়ী, offline-gating-এর বাইরের অন্য কোনো সেশনে)। শুধু doc-comment-এ
একটা আপডেট নোট যোগ করা হলো (ডকুমেন্টেশন সিঙ্কে আনতে) — bug-টা আসলেই ঠিক হয়েছে কিনা
টেস্ট/verify করা হয়নি (স্কোপের বাইরে)।

### কী পরিবর্তন হলো (১টা ফাইল, শুধু `SomadhanViewModel.kt`)
৩টা নতুন গার্ড-কল (`validateLoginCredentials`, `register`, `sendOtp`-এর `viewModelScope.launch`
ব্লকের শুরুতে) — প্রতিটাতেই ব্যর্থ হলে `onError("ইন্টারনেট সংযোগ ছাড়া এই কাজটি করা যাবে না")`-ও
কল করা হয়েছে (শুধু toast না) — কারণ `LoginScreen.kt` পড়ে দেখা গেছে caller-রা `isLoading`
Compose state সেট করে, যেটা শুধু `onSuccess`/`onError` callback-এই reset হয়; শুধু toast দেখিয়ে
`return` করলে spinner চিরস্থায়ীভাবে আটকে থাকত (নতুন bug হতো)। এছাড়া ৩টা ফাংশনে
(`completeLoginAfterOtp`, `logout`, `loginAsAdmin`) doc-comment-এ ব্যাখ্যা যোগ হয়েছে (কোনো
কোড-লজিক বদলায়নি)।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের পরে সম্পূর্ণ ফাইল-লেভেল ওভারঅল কাউন্ট: `()` 2927/2927, `{}`
1343/1343, `[]` 78/78 — সব delta = 0। আসল build ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip আর এই ধাপ শেষের আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ
অভিন্ন** — কোনো ফাইল হারায়নি/যোগ হয়নি, শুধু উপরের ১টা ফাইলের কন্টেন্ট বদলেছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ৬ — Wallet/Payment ডোমেইনে গার্ড ওয়্যারিং 🔴 money-critical।** এই ব্যাচে rule ২
(money-critical action সবসময় toggle-independent ব্লক) বিশেষভাবে প্রাসঙ্গিক — `deposit_money_via_gateway`,
`request_wallet_deposit`, `admin_confirm_gateway_deposit`, `record_gateway_payment_log`,
এবং admin money-repair টুল (`admin_reconcile_user_balances` ইত্যাদি)। শুরুর আগে
`SomadhanViewModel.kt`/`SomadhanRepository.kt`-এ এই RPC-গুলোর exact call-site, এবং প্রতিটা
কোনো callback-pattern (onSuccess/onError, নাকি StateFlow/UI state) ব্যবহার করে তা পড়ে বুঝে
নিতে হবে (rule ১০) — ধাপ ৫-এর মতোই, guard বসানোর সময় caller-এর loading-state reset হচ্ছে
কিনা প্রতিটাতে আলাদাভাবে যাচাই করতে হবে। কোনো 🟡 partial কাজ বাকি নেই — ধাপ ৫ সম্পূর্ণ ও সেভাবেই
বন্ধ হচ্ছে।

---

## ধাপ ৬ — Wallet/Payment ডোমেইনে গার্ড ওয়্যারিং 🔴 money-critical — ✅ সম্পূর্ণ (এই সেশনে)

### exact call-site কনফার্মেশন (rule ১০, কোড এডিটের আগে সম্পূর্ণ পড়ে) — ইনভেন্টরির ২টা আইটেম dead/no-op বলে আবিষ্কৃত হলো
`SomadhanRepository.kt`/`SomadhanViewModel.kt`/সংশ্লিষ্ট `*Screen.kt` পড়ে প্রতিটা wallet/payment
আইটেমের real network-dependency যাচাই করা হলো:

| ফাংশন | নেটওয়ার্ক কল | গার্ড? |
|---|---|---|
| `depositMoneyViaGateway()` (ওয়ালেট টপ-আপ, gateway callback) | LOCAL balance INSTANT (optimistic) + `request_wallet_deposit` RPC best-effort fire-and-forget (Outbox-এ নেই, শুধু log-warn) | ✅ যোগ হলো (সবসময়) |
| `recordGatewayPayment()` | **কোনো নেটওয়ার্ক কল নেই** — আগের এক বাগফিক্সে `record_gateway_payment_log` RPC dual-write সম্পূর্ণ সরানো হয়েছিল (duplicate-submit guard-এ ধরা পড়ে টাকা যোগ আটকাচ্ছিল বলে) | ❌ দরকার নেই (কোড-comment-এই confirm করা আছে) |
| `adminUpdateGatewayPaymentStatus()` | **কোনো নেটওয়ার্ক কল নেই** — `admin_confirm_gateway_deposit` RPC dual-write আগেই সম্পূর্ণ সরানো হয়েছিল (কোড পড়ে নিশ্চিত হওয়া গিয়েছিল এটা কখনো real কিছু করত না) — এখন pure local audit status-editor | ❌ দরকার নেই |
| `adminCleanupDuplicateRefunds()` (`cleanupDuplicateRefunds()`) | কোনো dryRun নেই — সবসময় LOCAL balance-correction INSTANT + best-effort cloud dual-write | ✅ যোগ হলো (সবসময়) |
| `adminRepairMissingRefunds(dryRun)` | dryRun=true: pure local audit/scan (network নেই)। dryRun=false: LOCAL balance credit INSTANT + best-effort cloud dual-write | ✅ যোগ হলো (শুধু dryRun=false-এ) |
| `adminReconcileBalances(dryRun)` (`reconcileUserBalances()`) | dryRun=true: pure local audit + একটা best-effort dual-write চেষ্টা (non-critical, কোনো ডাটা পরিবর্তন করে না)। dryRun=false: LOCAL balance correction INSTANT + best-effort cloud dual-write | ✅ যোগ হলো (শুধু dryRun=false-এ) |

**Inventory সংশোধন:** ধাপ ১-এর ইনভেন্টরিতে তালিকাভুক্ত `record_gateway_payment_log` এবং
`admin_confirm_gateway_deposit` — দুটোই কোড পড়ে দেখা গেল আগের অন্য কোনো সেশনে (বাগফিক্সের সময়)
ইতিমধ্যে সম্পূর্ণ dead/no-op করে ফেলা হয়েছে (RPC কলই আর হয় না) — তাই এই দুটোতে guard-এর
প্রয়োজনই নেই, শুধু doc-comment-এ ব্যাখ্যা যোগ করা হয়েছে (কোনো কোড-লজিক বদলায়নি)।

### ⚠️ কেন dryRun=true (audit-only) মোড ইচ্ছাকৃতভাবে গার্ড করা হয়নি
`adminRepairMissingRefunds`/`adminReconcileBalances`-এর dryRun=true মোড কোনো ডাটা পরিবর্তন করে
না (শুধু স্ক্যান করে mismatch/missing-refund রিপোর্ট তৈরি করে) — money movement নেই, তাই rule ২
প্রযোজ্য না। শুধু live-run (dryRun=false, যেটা আসলেই ব্যালেন্স পরিবর্তন করে) গার্ড করা হয়েছে।

### ⚠️ onComplete callback — pre-existing gap-এর সাথে সামঞ্জস্য রাখা হয়েছে (নতুন ফিক্স করা হয়নি)
`adminCleanupDuplicateRefunds`/`adminRepairMissingRefunds`/`adminReconcileBalances` — তিনটাতেই
UI (`AdminTransactionsView.kt`/`AdminEscrowView.kt`) একটা local `isXxxRunning` state সেট করে যেটা
শুধু `onComplete` callback-এই reset হয়। কোড পড়ে দেখা গেল এই তিনটা ফাংশনেরই বিদ্যমান
`catch (e: Exception)` ব্লক ইতিমধ্যেই `onComplete()` কল করে না (শুধু `showToast`) — অর্থাৎ
network/অন্য কোনো ব্যর্থতায় এমনিতেই স্পিনার আটকে থাকার একটা pre-existing সীমাবদ্ধতা আছে। এই
ধাপের offline-guard ইচ্ছাকৃতভাবে সেই একই বিদ্যমান আচরণ অনুসরণ করছে (guard ব্যর্থ হলে শুধু toast,
`onComplete` কল হয় না) — নতুন কোনো inconsistency তৈরি না করে, কিন্তু এই pre-existing
স্পিনার-আটকে-থাকা গ্যাপ ফিক্স করাও এই ধাপের স্কোপের বাইরে (rule ১১, ছোট রাখা)। `depositMoneyViaGateway()`-এ
এই সমস্যা নেই কারণ সেটা `onError` callback ব্যবহার করে (step ৫-এর login/register-এর মতোই
প্যাটার্ন) — তাই guard ব্যর্থ হলে `onError(...)` কল করা হয়েছে যাতে `UserWalletScreen.kt`-এর
"লেনদেন যাচাই হচ্ছে" ডায়ালগ আটকে না থাকে (retry বাটন দেখায়)।

### কী পরিবর্তন হলো (১টা ফাইল, শুধু `SomadhanViewModel.kt`)
- `depositMoneyViaGateway()`-এ `viewModelScope.launch` ব্লকের শুরুতে গার্ড (ব্যর্থ হলে
  `onError(...)` + `return@launch`)।
- `adminCleanupDuplicateRefunds()`-এ `viewModelScope.launch`-এর শুরুতে (try-এর আগে) গার্ড
  (ব্যর্থ হলে শুধু `return@launch`, বিদ্যমান catch-ব্লকের সাথে সামঞ্জস্যপূর্ণ)।
- `adminRepairMissingRefunds()`/`adminReconcileBalances()`-এ একই প্যাটার্নের গার্ড, কিন্তু
  `if (!dryRun && !requireOnlineOrWarn(...))`-এর মতো করে শুধু live-run-এ কার্যকর।
- `recordGatewayPayment()`/`adminUpdateGatewayPaymentStatus()`-এ কোনো গার্ড বসেনি, শুধু
  doc-comment যোগ হয়েছে ব্যাখ্যা করতে কেন (কোনো নেটওয়ার্ক কল নেই)।
- প্রতিটা guard-এ ডোমেইন-নির্দিষ্ট কাস্টম মেসেজ ব্যবহার করা হয়েছে (`requireOnlineOrWarn()`-এর
  optional `message` প্যারামিটার, ধাপ ৪-এ বানানো হলেও এই ধাপেই প্রথম ব্যবহার হলো) — ডিফল্ট
  জেনেরিক মেসেজের বদলে "ওয়ালেটে টাকা জমা দেওয়া যাবে না" / "ডুপ্লিকেট রিফান্ড ক্লিনআপ করা যাবে না"
  ইত্যাদি নির্দিষ্ট মেসেজ।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের আগে-পরে ওভারঅল `()`/`{}`/`[]` imbalance delta = 0 তিনটাতেই
(এডিটের আগে ইনপুট zip-এর ভার্সনের সাথে তুলনা করে)। প্রতিটা নতুন যোগ হওয়া ব্লক আলাদাভাবেও
(isolated) সম্পূর্ণ balanced। আসল build ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip (`somadhan-offline-action-gating-step5.zip`, ৩৪২টা ফাইল, dotfile-সহ) আর
এই ধাপ শেষের আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল
হারায়নি/যোগ হয়নি। ফোল্ডার-লেভেল `diff -rq` দিয়েও যাচাই করা হয়েছে — শুধু ১টা ফাইল
(`SomadhanViewModel.kt`) কন্টেন্ট বদলেছে, বাকি সব অভিন্ন। `.env`/`.gitignore` dotfile
অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ৭ — Bidding lifecycle ডোমেইনে গার্ড ওয়্যারিং 🔴 money-adjacent।** `accept_bid`,
`cancel_bid`, `reject_bid`, `solver_cancel_job`, `owner_reset_orphaned_accepted_bid`,
job-release ফ্লো (`request_job_release`, `cancel_job_release_request`,
`reject_job_release_request`), এবং লাইভ-স্ট্যাটাস আপডেট (`mark_job_started`,
`mark_solver_on_way`, `mark_solver_arrived`, `update_solver_live_location`,
`mark_completion_result_seen`)। শুরুর আগে `SomadhanViewModel.kt`/`SomadhanRepository.kt`-এ
প্রতিটার exact call-site, callback-pattern, এবং কোনটা ইতিমধ্যে Outbox/optimistic-local দিয়ে
কভার্ড (এই ধাপের মতোই কিছু হয়তো dead/no-op বা ইতিমধ্যে অন্যভাবে হ্যান্ডল্ড বেরিয়ে আসতে পারে)
তা যাচাই করে নিতে হবে (rule ১০) — বিশেষভাবে `accept_bid` money-adjacent (এসক্রো হোল্ড করে)
বলে rule ২-এর প্রাসঙ্গিকতা মাথায় রাখতে হবে, আর লাইভ-স্ট্যাটাস আপডেটগুলো সম্ভবত non-critical
(শুধু UI/ট্র্যাকিং তথ্য) — guard লাগবে কিনা তা কোড পড়ে নিশ্চিত হয়ে নেওয়া হবে। কোনো 🟡 partial
কাজ বাকি নেই — ধাপ ৬ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## ধাপ ৭ — Bidding lifecycle ডোমেইনে গার্ড ওয়্যারিং 🔴 money-adjacent — ✅ সম্পূর্ণ (এই সেশনে)

### exact call-site কনফার্মেশন (rule ১০) — প্রতিটা ফাংশনের real network-dependency ও money-path যাচাই
`SomadhanRepository.kt`/`SomadhanViewModel.kt` পড়ে প্রতিটা বিডিং-লাইফসাইকেল আইটেমের local-write
প্যাটার্ন (optimistic নাকি local-first+best-effort dual-write) ও exception-propagation যাচাই করা হলো:

| ফাংশন (ViewModel entry point) | রিপোজিটরি আচরণ | গার্ড? |
|---|---|---|
| `acceptBid()` | LOCAL wallet deduction + `openEscrow()` — উভয়ই optimistic Room write; `accept_bid` RPC শুধু best-effort dual-write, কখনো throw করে না | ✅ 🔴 rule ২ (সবসময়, toggle-independent) |
| `acceptInstantJobBid()` | ভেতরে `repository.acceptBid()`-কেই কল করে (একই money-path, ভিন্ন UI entry) | ✅ 🔴 rule ২ — **ইনভেন্টরি-গ্যাপ, নিচে দেখুন** |
| `withdrawBid()` (cancel_bid) | PENDING বিড cancel, কোনো টাকা/escrow জড়িত না (কোড-কমেন্টে কনফার্মড) — local-first + best-effort dual-write | ✅ সাধারণ Group B |
| `ownerResetOrphanedAcceptedBid()` | নিজেই locked-amount চেক করে real money থাকলে false রিটার্ন করে রিফিউজ করে — কখনো real escrow স্পর্শ করে না | ✅ সাধারণ Group B (rule ২ না, কারণ ফাংশনের নিজস্ব ডিজাইনেই money-safe) |
| `solverCancelAcceptedJob(problem, bid, ...)` | → `solverCancelJob()`-এর escrow-refund পাথে যায় | ✅ 🔴 rule ২ |
| `solverCancelJob(problemId, solverId, ...)` | সরাসরি `refundEscrowOnce()` দিয়ে LOCAL optimistic wallet-credit; `solver_cancel_job` RPC best-effort। এই ফাংশনই `solverCancelAcceptedJob(problem, reason, ...)` ডেলিগেট-ওভারলোডকেও কভার করে | ✅ 🔴 rule ২ |
| `solverCancelJob(problem, reason, ...)` (২য় ওভারলোড, dispute-flow entry) | একই repository escrow-refund পাথ, কিন্তু নিজস্ব আলাদা `viewModelScope.launch` | ✅ 🔴 rule ২ (আলাদাভাবে) |
| `requestJobRelease()` | local-first + best-effort dual-write; কোনো সরাসরি wallet-move না, কিন্তু owner-এর ৪৮-ঘণ্টা auto-release/payment প্রক্রিয়া শুরু করে + pending `AdditionalChargeEntity` তৈরি করে | ✅ money-adjacent Group B |
| `cancelJobReleaseRequest()` | একই কারণ | ✅ money-adjacent Group B |
| `rejectJobReleaseRequest()` | একই কারণ | ✅ money-adjacent Group B |
| `adminRejectBid()` (reject_bid) | PENDING বিড reject (accept-এর আগে), কোনো টাকা/escrow না | ✅ সাধারণ Group B |
| `markSolverOnWay()` / `markSolverArrived()` / `markJobStarted()` / `updateSolverLiveLocation()` | সবগুলো local-first (Room status/notification instant) + best-effort dual-write (`.onSuccess`/`.onFailure`-এ wrap করা, কখনো throw করে না) — শুধু UI/ট্র্যাকিং তথ্য, কোনো টাকা না | ❌ ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি |
| `markCompletionResultSeen()` | শুধু "দেখা হয়েছে" ফ্ল্যাগ, একই local-first+best-effort প্যাটার্ন | ❌ ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি |

### 🔍 ইনভেন্টরি-গ্যাপ আবিষ্কার: `acceptInstantJobBid()`
ধাপ ১-এর ইনভেন্টরিতে (`OFFLINE_ACTION_GATING_INVENTORY_STEP1.md`) ধাপ ৭-এর তালিকায় শুধু
`accept_bid` উল্লেখ ছিল, `acceptInstantJobBid()` আলাদা আইটেম হিসেবে ছিল না। কোড পড়ে দেখা গেল
এই ফাংশনটা ভেতরে সরাসরি `repository.acceptBid()`-কেই কল করে (instant-job accept flow) — অর্থাৎ
ঠিক একই local wallet-deduction + escrow-open money-path, শুধু ভিন্ন UI entry point দিয়ে। rule
২-এর "bid accept" স্পষ্টভাবে money-critical বলে চিহ্নিত করে বলে, `acceptBid()`-এর গার্ড থাকলেও
এই দ্বিতীয় entry point গার্ড ছাড়া থাকলে rule ২ কার্যত ফাঁকি দেওয়া যেত (instant-job flow দিয়ে)। এই
গ্যাপ এই ধাপেই বন্ধ করা হলো (একই ধরনের প্যাটার্ন যেমন ধাপ ৬-এ dead-RPC গ্যাপ পাওয়া গিয়েছিল, তবে
এখানে উল্টো — গ্যাপটা "মিসিং গার্ড", "অপ্রয়োজনীয় গার্ড" না)।

### ⚠️ কেন লাইভ-স্ট্যাটাস (৫টা ফাংশন) ইচ্ছাকৃতভাবে গার্ড করা হয়নি (rule ১ রক্ষা করতে)
`markSolverOnWay`/`markSolverArrived`/`markJobStarted`/`updateSolverLiveLocation`/
`markCompletionResultSeen` — এই পাঁচটাই কোড পড়ে নিশ্চিত হওয়া গিয়েছে সম্পূর্ণ local-first: Room
status/flag update সবসময় instant হয়, Supabase RPC dual-write `.onSuccess`/`.onFailure` দিয়ে wrap
করা (কখনো exception ছোঁড়ে না, caller পর্যন্ত পৌঁছায়ও না)। বর্তমানে অফলাইনে এই লোকাল
status-update/flag নির্বিঘ্নে কাজ করে, শুধু cloud sync পরে best-effort হয়। এখানে হার্ড গার্ড বসালে
পুরো ফাংশন কল-ই স্কিপ হয়ে যেত (গার্ড ফাংশনের শুরুতে বসে) — অর্থাৎ লোকাল status-update-টাও আর হতো
না, যেটা rule ১ ভঙ্গ করত (বর্তমান কার্যকর graceful-degradation ভেঙে একটা নতুন regression তৈরি
হতো) কোনো real সুবিধা ছাড়াই (এগুলোতে কোনো টাকা/state-integrity ঝুঁকি নেই — শুধু "সলভার রওনা
হয়েছেন" জাতীয় UI নোটিফিকেশন/GPS-ট্র্যাকিং)। প্রতিটার doc-comment-এ এই ব্যাখ্যা যোগ করা হয়েছে
(step 5-এর `completeLoginAfterOtp`/`logout`/`loginAsAdmin`-এর মতোই ইচ্ছাকৃত সিদ্ধান্ত)।

### কী পরিবর্তন হলো (১টা ফাইল, শুধু `SomadhanViewModel.kt`)
১১টা নতুন গার্ড-কল (`acceptBid`, `acceptInstantJobBid`, `withdrawBid`,
`ownerResetOrphanedAcceptedBid`, `solverCancelAcceptedJob`, `solverCancelJob` (উভয় ওভারলোড),
`requestJobRelease`, `cancelJobReleaseRequest`, `rejectJobReleaseRequest`, `adminRejectBid`) —
callback-থাকা ফাংশনগুলোতে ব্যর্থ হলে `onError(...)` কল (caller-এর loading/processing state
reset নিশ্চিত করতে, ঠিক ধাপ ৫-৬-এর প্যাটার্নে) + `return@launch`; `adminRejectBid()`-এ (কোনো
callback নেই বলে) শুধু `return@launch` (`requireOnlineOrWarn()` নিজেই toast দেখায়, ধাপ ৬-এর
`adminCleanupDuplicateRefunds()`-এর প্যাটার্ন অনুসরণ করে)। এছাড়া ৫টা লাইভ-স্ট্যাটাস ফাংশনে
(`markSolverOnWay`, `markSolverArrived`, `markJobStarted`, `updateSolverLiveLocation`,
`markCompletionResultSeen`) doc-comment যোগ হয়েছে ব্যাখ্যা করতে কেন গার্ড বসেনি (কোনো
কোড-লজিক বদলায়নি)।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের আগে-পরে ওভারঅল `()`/`{}`/`[]` imbalance delta = 0 তিনটাতেই
(ইনপুট zip-এর ভার্সনের সাথে তুলনা করে; উভয় ভার্সনই individually সম্পূর্ণ balanced — delta ০/০/০)।
`diff` দিয়ে যাচাই করা হয়েছে এডিটে শুধু ১০৬টা লাইন যোগ হয়েছে (comment + guard কোড), কোনো বিদ্যমান
লাইন মুছে যায়নি বা বদলায়নি। আসল build ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip (`somadhan-offline-action-gating-step6.zip`, ২৯৩টা ফাইল, dotfile-সহ) আর এই
ধাপ শেষের আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল হারায়নি/যোগ
হয়নি (diff শূন্য)। ফোল্ডার-লেভেল `diff -rq` দিয়েও যাচাই করা হয়েছে — শুধু ১টা ফাইল
(`SomadhanViewModel.kt`) কন্টেন্ট বদলেছে, বাকি সব বাইট-বাই-বাইট অভিন্ন। `.env`/`.gitignore`
dotfile অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ৮ — Problem posting / Instant jobs ডোমেইনে গার্ড ওয়্যারিং।** ইনভেন্টরি অনুযায়ী স্কোপ:
সরাসরি টেবিল insert (`createProblem`-এর `problems` insert, `submitBid`-এর ভেতরের `bids` insert
— এই দুটো placeBid()-এর জন্য ইতিমধ্যে `acceptBid()`-এর প্যাটার্নে local-first কিনা যাচাই করা
দরকার), `user_delete_problem`, `mark_problem_seen`; Instant job: `broadcast_instant_job`,
`cancel_instant_job` (`cancelInstantJob()` — এই ধাপেই কোড পড়ার সময় দেখা গেছে এটাও local-first
escrow-refund করে, rule ২ প্রাসঙ্গিক হতে পারে, ধাপ ৮-এ কনফার্ম করা হবে), `expire_broadcasting_instant_job`
(সম্ভবত cron-triggered, client call না — যাচাই দরকার); Solver quota:
`sync_solver_free_job_quota`, `system_track_extra_payment_miss`; Admin:
`admin_update_problem_status`, `admin_update_problem_budget`, `admin_refund_and_reopen_problem`
🔴, `admin_force_cancel_instant_job`, `admin_reset_free_job_quota`,
`admin_bulk_reset_free_job_quota`, `admin_reset_miss_cycle`। শুরুর আগে প্রতিটার exact call-site,
callback-pattern, এবং local-first/optimistic নাকি সত্যিকারের network-dependent তা কোড পড়ে যাচাই
করে নিতে হবে (rule ১০) — বিশেষভাবে `admin_refund_and_reopen_problem` money-critical (rule ২)
সম্ভাবনা মাথায় রাখতে হবে। কোনো 🟡 partial কাজ বাকি নেই — ধাপ ৭ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## ধাপ ৮ — Problem posting / Instant jobs ডোমেইনে গার্ড ওয়্যারিং — ✅ সম্পূর্ণ (এই সেশনে)

### exact call-site কনফার্মেশন (rule ১০) — প্রতিটা আইটেমের local-first/network-dependency যাচাই

| ফাংশন (ViewModel entry point) | রিপোজিটরি আচরণ | গার্ড? |
|---|---|---|
| `createProblem()` | `problems` insert local-first (Room), Supabase insert fire-and-forget | ✅ সাধারণ Group B |
| `createInstantJob()` | local-first insert; সফল হলে ভেতরেই `broadcast_instant_job` RPC (কোনো আলাদা entry point নেই) | ✅ সাধারণ Group B (broadcast-ও কভার্ড) |
| `placeBid()` | `bids` insert local-first, Supabase dual-write best-effort | ✅ সাধারণ Group B |
| `userDeleteProblem()` | soft-delete + bid-cancel local-first, RPC dual-write best-effort | ✅ সাধারণ Group B |
| `cancelInstantJob()` | accepted solver থাকলে `refundEscrowOnce()` দিয়ে LOCAL optimistic wallet-credit (`refund_escrow_once` RPC শুধু best-effort) | ✅ 🔴 rule ২ |
| `adminForceCancelInstantJob()` | REBROADCAST/TO_NORMAL_BIDDING/CANCEL — তিনটাতেই hadAcceptedSolver হলে একই `refundEscrowOnce()` পাথ | ✅ 🔴 rule ২ |
| `adminUpdateProblemStatus()` / `adminUpdateProblemBudget()` | local-first, কোনো টাকা না | ✅ সাধারণ Group B |
| `adminRunMonthlyFreeQuotaReset()` / `adminResetSolverFreeQuota()` / `adminResetSolverMissCycle()` | কাউন্টার রিসেট local-first, কোনো টাকা না | ✅ সাধারণ Group B |
| `markProblemSeen()` | "দেখা হয়েছে" flag, সম্পূর্ণ local-first, no callback | ❌ ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি (rule ১) |
| `triggerInstantJobExpiryCheck()` / `checkAndExpireInstantJobs()` / `expireBroadcastingInstantJob` | countdown-timer + Worker/AlarmReceiver থেকে ব্যাকগ্রাউন্ডে ট্রিগার হয়, কোনো UI বাটন না | ❌ ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি (cron-সদৃশ) |
| `runMonthlyFreeQuotaReset()`-এর Worker পথ | `ScheduledNotificationWorker`-এর ব্যাকগ্রাউন্ড কল | ❌ গার্ড হয়নি (শুধু admin-button entry point গার্ড হয়েছে, Worker পথ অপরিবর্তিত) |

### 🔍 ইনভেন্টরি-সংশোধন: `sync_solver_free_job_quota` ও `system_track_extra_payment_miss` আসলে এই ডোমেইনে নেই
ধাপ ১-এর ইনভেন্টরিতে এই দুটো RPC "ধাপ ৮ — Solver quota" হিসেবে তালিকাভুক্ত ছিল। কোড পড়ে
নিশ্চিত হওয়া গেছে:
- `syncSolverFreeJobQuota()` শুধু `resolveCommissionRateForNewJob()`-এর ভেতর থেকে কল হয়, যেটা
  শুধু `acceptBid()` (ধাপ ৭-এ ইতিমধ্যে 🔴 rule ২ গার্ড হয়ে গেছে) আর `acceptDirectContractProposal()`
  (ধাপ ১১-এর ডোমেইন) থেকে ডাকা হয় — `createProblem()`/`placeBid()` কোনোটাই এটা কল করে না।
- `systemTrackExtraPaymentMiss()` শুধু private `trackExtraPaymentMissCycle()`-এর ভেতর থেকে কল
  হয়, যেটা `confirmReleaseAndComplete()`-এর (job-completion/settlement choke-point, ধাপ ৭-এর
  release-flow/ধাপ ১০-এর rating-flow-সংলগ্ন) অংশ — problem-posting/instant-job ডোমেইনে অপ্রাসঙ্গিক।

এই দুটোর জন্য এই ধাপে তাই কোনো কোড পরিবর্তন লাগেনি (`syncSolverFreeJobQuota` ইতিমধ্যে তার একমাত্র
প্রাসঙ্গিক caller `acceptBid()`-এর মাধ্যমে গার্ডেড; `systemTrackExtraPaymentMiss` যে ধাপে
`confirmReleaseAndComplete()`-এর entry point গার্ড হবে সেখানে কভার্ড হয়ে যাবে)।

### 🔍 ইনভেন্টরি-গ্যাপ আবিষ্কার ও ফিক্স: `adminReleaseEscrow()` / `adminRefundEscrow()` (স্কোপ সামান্য বিস্তৃত হয়েছে)
`admin_refund_and_reopen_problem` RPC (`adminRefundEscrow()`-এর ভেতরে) অনুসরণ করতে গিয়ে কোড পড়ে
দেখা গেল `adminRefundEscrow()`/`adminReleaseEscrow()` উভয়ই ধাপ ৭-এর `acceptBid()`/`solverCancelJob()`-এর
হুবহু একই প্যাটার্নে LOCAL optimistic wallet write করে (`payoutEscrowToSolver()`/`refundEscrowOnce()`),
`release_escrow`/`refund_escrow_once` RPC শুধু best-effort dual-write। কিন্তু ধাপ ১-এর ইনভেন্টরিতে এই
দুটো RPC-ই **Group A** (Outbox-retry কভার্ড, ধাপ ১২-এ শুধু ভেরিফাই, কোড পরিবর্তন না) হিসেবে চিহ্নিত
ছিল — অর্থাৎ মাস্টার প্রম্পটের দুটো অংশের মধ্যে একটা সত্যিকারের দ্বন্দ্ব ছিল: Group A-র "শুধু retry,
ব্লক না" পরিকল্পনা বনাম rule ২-এর স্পষ্ট "escrow release/refund" নামোল্লেখ + "toggle-independent,
কখনো offline optimistic allow না" নির্দেশ। rule ২-কে প্রাধান্য দিয়ে (মাস্টার প্রম্পটে rule ২-কে
সবচেয়ে কঠোরভাবে, "toggle-independent" শব্দসহ লেখা হয়েছে) এই দুটো ফাংশনেই গার্ড বসানো হলো — ঠিক
যেমন ধাপ ৭-এ `acceptInstantJobBid()`-এর গ্যাপ আবিষ্কার হওয়া মাত্র বন্ধ করা হয়েছিল। এতে
`release_escrow`/`refund_escrow_once` RPC দুটোর Group A স্ট্যাটাস পাল্টায়নি (online অবস্থায় transient
network glitch-এ Outbox retry এখনো স্বাভাবিকভাবে কাজ করবে) — গার্ড শুধু সম্পূর্ণ-অফলাইন অবস্থায়
পুরো অ্যাকশনটা শুরু হওয়া আটকায়। এই দুটো ফাংশন কঠোরভাবে "Problem posting/Instant jobs" ডোমেইনের
বাইরে (এসক্রো release/refund ডোমেইন), কিন্তু rule ১-এর "কোনো গ্যাপ রেখে দেওয়া যাবে না" চেতনায়
এখানেই ফিক্স করা যুক্তিসঙ্গত মনে হয়েছে, যেহেতু `admin_refund_and_reopen_problem`-এর কারণে এই কোড
এমনিতেই এই সেশনে পড়া হচ্ছিল। ধাপ ১২ (Group A ভেরিফিকেশন) এই সিদ্ধান্তটা রিভিউ করে দেখবে।

### কী পরিবর্তন হলো (১টা ফাইল, শুধু `SomadhanViewModel.kt`)
১৩টা ফাংশনে গার্ড যোগ হলো:
- 🔴 rule ২ (৪টা): `cancelInstantJob`, `adminForceCancelInstantJob`, `adminReleaseEscrow`,
  `adminRefundEscrow` — সবগুলোতে `if (!requireOnlineOrWarn(...)) return@launch` (কোনো callback
  না থাকায় বা ধাপ ৭-এর প্যাটার্ন অনুসরণ করে)।
- সাধারণ Group B (৯টা): `createProblem`, `createInstantJob`, `placeBid`, `userDeleteProblem`
  (callback-থাকা চারটায় ব্যর্থ হলে `onError(...)` + `return@launch`), `adminUpdateProblemStatus`,
  `adminUpdateProblemBudget`, `adminRunMonthlyFreeQuotaReset`, `adminResetSolverFreeQuota`,
  `adminResetSolverMissCycle` (কোনো callback নেই বলে শুধু `return@launch`)।
- ২টা doc-comment (কোনো লজিক বদলায়নি): `markProblemSeen()` ও `triggerInstantJobExpiryCheck()`-এ
  কেন গার্ড বসেনি তার ব্যাখ্যা।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের আগে-পরে ওভারঅল `()`/`{}`/`[]` imbalance delta = 0 তিনটাতেই
(ইনপুট zip-এর ভার্সনের সাথে তুলনা করে; উভয় ভার্সনই individually সম্পূর্ণ balanced)। আসল build
ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip (`somadhan-offline-action-gating-step7.zip`, dotfile-সহ) আর এই ধাপ শেষের
আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল হারায়নি/যোগ হয়নি।
ফোল্ডার-লেভেল `diff -rq` দিয়েও যাচাই করা হয়েছে — শুধু ১টা ফাইল (`SomadhanViewModel.kt`) কন্টেন্ট
বদলেছে, বাকি সব বাইট-বাই-বাইট অভিন্ন। `.env`/`.gitignore` dotfile অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ৯ — Chat/Messages ডোমেইনে গার্ড ওয়্যারিং।** ইনভেন্টরি অনুযায়ী স্কোপ: `sendMessage()`
(`messages` insert), messages `.update()` (mark-seen), `admin_send_message_to_problem_chat`,
`admin_delete_message`, `system_event_message`। শুরুর আগে প্রতিটার exact call-site,
callback-pattern, এবং local-first/optimistic নাকি network-dependent তা কোড পড়ে যাচাই করে নিতে
হবে (rule ১০) — এই ডোমেইনে কোনো টাকা জড়িত থাকার কথা না (সাধারণ Group B হওয়ার সম্ভাবনা বেশি),
তবু নিশ্চিত হয়ে নেওয়া দরকার। এছাড়া `system_event_message` সম্ভবত system/internal-triggered
(client button না) কিনা তা-ও যাচাই করা হবে (ধাপ ৮-এর `checkAndExpireInstantJobs`-এর মতোই)। কোনো
🟡 partial কাজ বাকি নেই — ধাপ ৮ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## ধাপ ৯ — Chat/Messages ডোমেইনে গার্ড ওয়্যারিং — ✅ সম্পূর্ণ (এই সেশনে)

### ⚠️ ইউজার-নির্দেশিত UX বিচ্যুতি (ধাপ ৫–৮-এর স্ট্যান্ডার্ড প্যাটার্ন থেকে ইচ্ছাকৃত ব্যতিক্রম)
ইউজার এই সেশনে স্পষ্টভাবে জানিয়েছেন: কনজিউমার চ্যাটে (ChatScreen) অফলাইনে মেসেজ পাঠাতে গেলে
`requireOnlineOrWarn()`-এর হার্ড-ব্লক-আগে-toast প্যাটার্ন (ধাপ ৫-৮-এ সব Group B-তে ব্যবহৃত) না —
বরং মেসেঞ্জার-অ্যাপের মতো UX: মেসেজ সাথে সাথে চ্যাট-বক্সে (বাবল আকারে) দেখা যাবে, পাঠানো না গেলে
বাবলের ঠিক নিচে ছোট "পাঠানো যায়নি" লেখা + "আবার পাঠান" (retry) দেখাবে, নেটওয়ার্ক ফিরলে
স্বয়ংক্রিয়ভাবে (বা রিট্রাই-ট্যাপে) পাঠানো হবে। কোড পড়ে (rule ১০) নিশ্চিত হওয়া গেছে
`repository.sendMessage()` এমনিতেই আগে থেকে সম্পূর্ণ local-first ছিল (Room insert সবসময় হয়,
Supabase dual-write শুধু best-effort log-only silent-fail) — তাই rule ১ ভাঙা হয়নি, বরং আগের
"নীরব silent-fail"-কে ইউজার-দৃশ্যমান PENDING→SENT/FAILED স্ট্যাটাস + retry-তে upgrade করা হয়েছে।
এই ব্যতিক্রম **শুধু** কনজিউমার `sendMessage()`-এর জন্য — admin-side মেসেজিং
(`adminSendMessageToProblemChat`/`adminDeleteMessage`) আগের ধাপগুলোর স্ট্যান্ডার্ড হার্ড-গার্ড
প্যাটার্নেই থেকেছে (নিচে দেখুন), কারণ ইউজারের নির্দেশনা শুধু "চ্যাট বক্সে মেসেজ পাঠানো" নিয়ে ছিল।

### exact call-site কনফার্মেশন (rule ১০)

| ফাংশন | রিপোজিটরি আচরণ | সিদ্ধান্ত |
|---|---|---|
| `sendMessage()` | Room insert সবসময় local-first (আগে থেকেই); Supabase dual-write আগে silent best-effort ছিল | ✅ নতুন `sendStatus` (PENDING→SENT/FAILED) ট্র্যাকিং + ChatScreen ইনলাইন retry, কোনো হার্ড গার্ড না |
| `markMessagesAsReadForProblem()` (mark-seen) | সম্পূর্ণ local-first "পঠিত" flag, no callback | ❌ ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি (rule ১, ধাপ ৭/৮-এর markProblemSeen-এর মতোই) |
| `adminSendMessageToProblemChat()` | local-first insert, RPC dual-write best-effort | ✅ সাধারণ Group B (স্ট্যান্ডার্ড হার্ড গার্ড) |
| `adminDeleteMessage()` | local-first delete, RPC dual-write best-effort | ✅ সাধারণ Group B (স্ট্যান্ডার্ড হার্ড গার্ড) |
| `sendSystemEventMessage()` (`system_event_message`) | কোনো UI বাটন সরাসরি কল করে না — `adminManuallyFlagDispute`/`withdrawDispute` ইত্যাদির (ধাপ ১০, Dispute ডোমেইন) ভেতর থেকে সাব-স্টেপ হিসেবে কল হয় | ❌ এখানে গার্ড বসেনি (ধাপ ৮-এর cron-triggered RPC-র মতোই) — ধাপ ১০-এ outer entry-point গার্ড হলে কভার্ড হয়ে যাবে |

### কী পরিবর্তন হলো (৬টা ফাইল)

1. **`MarketEntities.kt`** — `MessageEntity`-তে নতুন `sendStatus: String = "SENT"` কলাম যোগ
   (মান: `"PENDING"`/`"SENT"`/`"FAILED"`)।
2. **`AppDaos.kt`** (`MessageDao`) — `updateMessageSendStatus(messageId, status)` ও
   `getFailedMessagesForSender(userId)` (reconnect auto-retry-এর জন্য, senderId-স্কোপড কারণ RLS-ও
   তাই বলে) যোগ হলো।
3. **`AppDatabase.kt`** — version ৫৩→৫৪, `MIGRATION_53_54` (শুধু
   `ALTER TABLE messages ADD COLUMN sendStatus TEXT NOT NULL DEFAULT 'SENT'`, সম্পূর্ণ additive,
   কোনো পুরনো মেসেজ/চ্যাট-ইতিহাস মুছে যায় না) যোগ ও রেজিস্টার হলো।
4. **`SomadhanRepository.kt`**:
   - `sendMessage()`: এখন `"PENDING"` দিয়ে insert করে, dual-write সফল/ব্যর্থ অনুযায়ী
     `"SENT"`/`"FAILED"`-এ আপডেট করে (বিদ্যমান local-insert-প্রথম আচরণ অপরিবর্তিত)।
   - নতুন `retrySendMessage(messageId): Boolean` — একটা FAILED মেসেজ আবার পাঠানোর চেষ্টা করে,
     নতুন insert না (একই `id` রাখে, যাতে ChatScreen-এর `LazyColumn` key স্থিতিশীল থাকে)।
   - নতুন `getFailedMessagesForSender(userId)` — reconnect auto-retry-এর জন্য wrapper।
   - `sendSystemEventMessage()`/`markMessagesAsReadForProblem()`-এ doc-comment যোগ (কোনো লজিক
     বদলায়নি) — কেন গার্ড বসেনি তার ব্যাখ্যা।
5. **`SomadhanViewModel.kt`**:
   - নতুন `retryFailedMessage(messageId)` — ইনলাইন retry ট্যাপের জন্য, `requireOnlineOrWarn()`
     দিয়ে গার্ডেড (এখনো অফলাইন হলে সাথে সাথে toast, silently আবার fail হতে দেয় না)।
   - নতুন `retryAllFailedMessagesOnReconnect()` — বিদ্যমান `startConnectivityObserver()`-এর
     false→true transition-এ (`retryAllErroredSyncPhasesOnReconnect()`-এর পাশে) কল হয়, একই
     `tryConsumeConnectivityRetryCooldown()` ম্যাপ পুনরায়-ব্যবহার করে (key="messages")।
   - `adminSendMessageToProblemChat()`/`adminDeleteMessage()`-এ স্ট্যান্ডার্ড
     `if (!requireOnlineOrWarn(...)) return@launch` গার্ড যোগ হলো (ধাপ ৫-৮-এর প্যাটার্ন)।
6. **`ChatScreen.kt`** (`ChatMessageItem`):
   - Time/read-tick Row-এ `sendStatus` অনুযায়ী শাখা: `"FAILED"` → লাল Warning আইকন,
     `"PENDING"` → কোনো tick না, else (SENT) → আগের মতোই isRead-ভিত্তিক DoneAll।
   - নতুন Row (শুধু `isMyMsg && sendStatus == "FAILED"` হলে দৃশ্যমান) — "পাঠানো যায়নি" (লাল) +
     "আবার পাঠান" (ব্র্যান্ড-কালার, ক্লিকযোগ্য) যেটা নতুন `onRetrySend` কলব্যাক ফায়ার করে, call-site-এ
     `viewModel.retryFailedMessage(messageId)`-এর সাথে ওয়্যার করা।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
সব ৬টা এডিট-করা ফাইলে `()`/`{}`/`[]` imbalance delta = 0 (ইনপুট zip-এর ভার্সনের সাথে তুলনা করে)।
`SomadhanRepository.kt`-এ raw `(`/`)` count-এ একটা পূর্ব-বিদ্যমান -৯ imbalance আছে (মূলত Bengali
কমেন্ট/স্ট্রিং-এর মধ্যে stray বন্ধনীর কারণে, কোড সিনট্যাক্সের সমস্যা না) — এই সেশনের এডিটের
আগে-পরে দুটোতেই একই -৯ delta, অর্থাৎ এই সেশনের এডিট নিজে সম্পূর্ণ balanced। `diff` দিয়ে যাচাই করা
হয়েছে সব ফাইলে শুধু ইচ্ছাকৃত লাইনগুলোই যোগ/পরিবর্তন হয়েছে, কোনো অপ্রত্যাশিত লাইন মোছেনি। আসল build
ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip (`somadhan-offline-action-gating-step8.zip`, ২৯৩টা ফাইল, dotfile-সহ) আর এই
ধাপ শেষের আউটপুট zip-এর ফাইল-লিস্ট **সম্পূর্ণ অভিন্ন** (diff শূন্য, ২৯৩টা ফাইল দুটোতেই) — কোনো
ফাইল হারায়নি/যোগ হয়নি। শুধু ৬টা ফাইল কন্টেন্ট বদলেছে (`MarketEntities.kt`, `AppDaos.kt`,
`AppDatabase.kt`, `SomadhanRepository.kt`, `SomadhanViewModel.kt`, `ChatScreen.kt`), বাকি সব
বাইট-বাই-বাইট অভিন্ন। `.env`/`.gitignore` dotfile অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ১০ — Ratings/Reviews, Additional charges, Disputes ডোমেইনে গার্ড ওয়্যারিং।** ইনভেন্টরি
অনুযায়ী স্কোপ: Ratings (`submit_rating`, `admin_delete_rating`); Additional charge
(`request_additional_charge`, `respond_additional_charge`, `mark_additional_charge_settled` 🔴,
`request_extra_amount`, `user_confirm_extra_amount`, `user_reject_extra_amount`); Dispute
(`raise_dispute`, `resolve_dispute` 🔴, `resolve_dispute_split` 🔴, `settle_dispute` 🔴,
`withdraw_dispute`, `admin_manually_flag_dispute`, `mark_dispute_result_seen`,
`request_admin_assistance`)। শুরুর আগে প্রতিটার exact call-site, callback-pattern, ও
local-first/optimistic নাকি money-critical (rule ২) তা কোড পড়ে যাচাই করে নিতে হবে (rule ১০) —
বিশেষভাবে `resolve_dispute`/`resolve_dispute_split`/`settle_dispute`/
`mark_additional_charge_settled` টাকা/এসক্রো স্পর্শ করে কিনা নিশ্চিত হওয়া জরুরি। এছাড়া
`withdrawDispute()`/`adminManuallyFlagDispute()` (যদি এই নামেই থাকে) গার্ডেড হলে ধাপ ৯-এর
`sendSystemEventMessage()` স্বয়ংক্রিয়ভাবে কভার্ড হয়ে যাবে কিনা তা-ও কনফার্ম করতে হবে। কোনো 🟡
partial কাজ বাকি নেই — ধাপ ৯ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## ধাপ ১০ — Ratings/Reviews, Additional charges, Disputes ডোমেইনে গার্ড ওয়্যারিং — ✅ সম্পূর্ণ (এই সেশনে)

### exact call-site কনফার্মেশন (rule ১০) — প্রতিটা আইটেমের local-first/network-dependency ও money-criticality যাচাই

| ফাংশন (ViewModel entry point) | রিপোজিটরি আচরণ | গার্ড? |
|---|---|---|
| `submitSolverRatingForUser()` / `submitUserRatingForSolver()` | rating insert + reputation আপডেট local-first, কোনো টাকা না, callback নেই | ✅ সাধারণ Group B |
| `adminDeleteRating()` | local-first delete, RPC dual-write best-effort | ✅ সাধারণ Group B |
| `requestAdditionalCharge()` | শুধু PENDING charge record তৈরি করে, কোনো টাকা না | ✅ সাধারণ Group B |
| `respondToAdditionalCharge()` | accept=true হলে LOCAL wallet deduction (`deductBalanceForUserRole`) + `addToEscrow()`, ঠিক `userConfirmExtraAmount()`-এর একই প্যাটার্ন; `respond_additional_charge` RPC শুধু best-effort | ✅ 🔴 rule ২ (accept/reject উভয় শাখাতেই, ফাংশন-লেভেলে) |
| `requestExtraAmount()` | শুধু pending fields সেট করে, কোনো টাকা না | ✅ সাধারণ Group B |
| `userConfirmExtraAmountPaid()` (→ `userConfirmExtraAmount`) | LOCAL wallet deduction + `addToEscrow()`, optimistic Room write | ✅ 🔴 rule ২ |
| `userRejectExtraAmount()` | শুধু pending fields ক্লিয়ার করে, কোনো টাকা না | ✅ সাধারণ Group B |
| `raiseDispute()` | dispute flags সেট করে + notification/chat-notice, কোনো টাকা না | ✅ সাধারণ Group B |
| `withdrawDispute()` | dispute flags রিসেট করে, কোনো টাকা না | ✅ সাধারণ Group B |
| `settleDispute()` | dispute flags রিসেট করে (উভয়পক্ষ-সমঝোতা), কোনো টাকা না | ✅ সাধারণ Group B (ইনভেন্টরি-সংশোধন, নিচে দেখুন) |
| `requestAdminAssistance()` | শুধু flag + admin-notification, কোনো টাকা না | ✅ সাধারণ Group B |
| `adminResolveDispute()` | তিনটা resolution branch-ই (RELEASE_TO_SOLVER/SPLIT/REFUND) LOCAL optimistic wallet/escrow write | ✅ 🔴 rule ২ |
| `markDisputeResultSeen()` | সম্পূর্ণ local-first "দেখা হয়েছে" flag, no callback | ❌ ইচ্ছাকৃতভাবে গার্ড বসানো হয়নি (rule ১, `markProblemSeen()`-এর মতোই) |
| `adminManuallyFlagDispute()` | dispute flags সেট করে, কোনো টাকা না | ✅ সাধারণ Group B |
| `confirmReleaseAndComplete()` (→ `markProblemCompleted()` উভয় overload) | LOCAL wallet deduction + escrow release (`payoutEscrowToSolver`), settlement choke-point | ✅ 🔴 rule ২ |

### 🔍 ইনভেন্টরি-সংশোধন: `settle_dispute` আসলে money-critical না, `resolve_dispute` (non-split) dead code
ধাপ ১-এর ইনভেন্টরিতে `settle_dispute`, `resolve_dispute`, `resolve_dispute_split`,
`mark_additional_charge_settled` — চারটাই 🔴 হিসেবে ট্যাগ করা ছিল। কোড পড়ে (rule ১০) দেখা গেল:
- `settleDispute()` (দুই পক্ষের সমঝোতায় dispute তুলে নেওয়া) কোনো টাকা স্পর্শ করে না —
  `adminResolveDispute()`-এর মতো escrow/wallet payout নেই। rule ২ প্রযোজ্য না; সাধারণ Group B
  গার্ড দেওয়া হলো (মূল সিদ্ধান্ত পাল্টায়নি — শুধু 🔴-এর বদলে সাধারণ গার্ড)।
- `resolve_dispute` (non-split) RPC/`SupabaseSyncManager.resolveDispute()` wrapper আসলে **dead
  code** — কোথাও থেকে কল হয় না (কোডের নিজস্ব comment-এই "ধাপ ২৯.৫" আবিষ্কার হিসেবে confirm করা
  আছে, ডাবল-রিফান্ড এড়াতে ইচ্ছাকৃতভাবে wire করা হয়নি)। বাস্তব admin resolve পাথ
  `resolve_dispute_split()` (narrow-scoped) ব্যবহার করে, যেটা `adminResolveDispute()`-এর ভেতরে
  আছে এবং সেটাই money-critical (কিন্তু RPC dual-write-এর কারণে না — নিচের LOCAL optimistic
  wallet/escrow write-এর কারণে, ঠিক `acceptBid()`-এর মতোই)।
- `mark_additional_charge_settled` সরাসরি কোনো ViewModel entry point থেকে কল হয় না — শুধু
  `confirmReleaseAndComplete()`-এর ভেতরে সাব-স্টেপ হিসেবে কল হয় (ধাপ ৯-এর progress-এন্ট্রিতে
  আগেই এই প্ল্যান লেখা ছিল)। তাই এই আইটেমের জন্য আলাদা কোনো গার্ড লাগেনি —
  `confirmReleaseAndComplete()`-এর entry-point গার্ড স্বয়ংক্রিয়ভাবে এটাও কভার করে।

### 🔍 নতুন আবিষ্কার: `confirmReleaseAndComplete()` — settlement-এর choke point, ৩টা প্রবেশপথ
`mark_additional_charge_settled` অনুসরণ করতে গিয়ে কোড পড়ে দেখা গেল `confirmReleaseAndComplete()`
আসলে পুরো অ্যাপের money-release-এর কেন্দ্রীয় ফাংশন, যেটা তিনটা ভিন্ন জায়গা থেকে কল হয়:
1. ViewModel `confirmReleaseAndComplete()` (এই সেশনে গার্ডেড) → UI-এর "Confirm & Complete" বাটন +
   দুটো `markProblemCompleted()` overload — user-facing, এই ধাপেই গার্ড হলো।
2. `checkAndProcess48HourAutoReleases()` (cron sweep) → repository-লেভেলে সরাসরি কল করে, এই
   ViewModel-গার্ড এড়িয়ে যায় (ইচ্ছাকৃতভাবে — ধাপ ৮-এর `expire_broadcasting_instant_job`-এর
   মতোই cron-triggered flow গার্ড করা ঠিক না)।
3. `adminUpdateDirectContractStatus()` (ধাপ ১১-এর Direct contract ডোমেইন) → repository-লেভেলে
   সরাসরি কল করে, RELEASE_TO_SOLVER-জাতীয় resolution-এ। এই ফাংশনের নিজস্ব ViewModel entry point
   এই সেশনের স্কোপের বাইরে (ধাপ ১১-এর ডোমেইন) — **ধাপ ১১-এ অবশ্যই আলাদাভাবে rule ২ গার্ড দিতে
   হবে**, কারণ এটাও একই money-critical `confirmReleaseAndComplete()` পাথে পৌঁছায়।

### কী পরিবর্তন হলো (১টা ফাইল, শুধু `SomadhanViewModel.kt`)
১৫টা ফাংশনে গার্ড/ডকুমেন্টেশন যোগ হলো:
- 🔴 rule ২ (৪টা): `userConfirmExtraAmountPaid`, `respondToAdditionalCharge`,
  `adminResolveDispute`, `confirmReleaseAndComplete` — সবগুলোতে callback থাকায়
  `if (!requireOnlineOrWarn(...)) { onError(...); return@launch }`।
- সাধারণ Group B (৯টা): `requestExtraAmount`, `userRejectExtraAmount`, `raiseDispute`,
  `withdrawDispute`, `settleDispute`, `requestAdminAssistance`, `adminManuallyFlagDispute`,
  `requestAdditionalCharge` (callback থাকায় `onError(...)` + `return@launch`);
  `submitSolverRatingForUser`, `submitUserRatingForSolver`, `adminDeleteRating` (কোনো callback
  নেই বলে শুধু `return@launch`)।
- ২টা doc-comment (কোনো লজিক বদলায়নি): `markDisputeResultSeen()`-এ কেন গার্ড বসেনি তার ব্যাখ্যা,
  এবং ইনভেন্টরি-সংশোধন নোট।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের আগে-পরে ওভারঅল `()`/`{}`/`[]` imbalance delta = 0 তিনটাতেই
(ইনপুট zip-এর ভার্সনের সাথে তুলনা করে; উভয় ভার্সনই individually সম্পূর্ণ balanced)। `diff` দিয়ে
যাচাই করা হয়েছে শুধু ইচ্ছাকৃত লাইনগুলোই যোগ হয়েছে, কোনো বিদ্যমান লাইন মোছেনি/বদলায়নি। আসল build
ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
এই সেশনের ইনপুট zip (`somadhan-offline-action-gating-step9.zip`, dotfile-সহ) আর এই ধাপ শেষের
আউটপুট zip-এর ফাইল-লিস্ট (normalize করে) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল হারায়নি/যোগ হয়নি।
শুধু ১টা ফাইল (`SomadhanViewModel.kt`) কন্টেন্ট বদলেছে, বাকি সব বাইট-বাই-বাইট অভিন্ন (progress/master
prompt দুটো doc ফাইল বাদে, যা এই সেশনেই আপডেট হচ্ছে)। `.env`/`.gitignore` dotfile অন্তর্ভুক্ত আছে।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ১১ — Admin misc, KYC, Role/Account, Notifications, Direct contracts ডোমেইনে গার্ড
ওয়্যারিং।** ইনভেন্টরি অনুযায়ী স্কোপ: KYC (`submit_kyc`); Role/Account
(`switch_role_get_or_create_linked_profile`, `sync_linked_account_profile`,
`admin_soft_delete_user`, `admin_credentials_update`, `admin_credentials_verify_password`,
`admin_remove_category_from_solvers`, `admin_reassign_solver`, `submit_reputation_event`,
`clear_solver_cancelled_notice`); Direct contract (`accept_direct_contract`,
`decline_direct_contract`, `admin_update_direct_contract_status`); এবং ধাপ ১-এর "যাচাই দরকার"
আইটেম হিসেবে `admin_wipe_all_data` (ইউজার আগেই কনফার্ম করেছেন — সবসময় সম্পূর্ণ ব্লক, কোনো
retry/queue না)। শুরুর আগে প্রতিটার exact call-site, callback-pattern, ও local-first/optimistic
নাকি money-critical (rule ২) তা কোড পড়ে যাচাই করে নিতে হবে (rule ১০) —
**বিশেষভাবে `admin_update_direct_contract_status()` অবশ্যই 🔴 rule ২ গার্ড লাগবে**, কারণ এই
ধাপে (ধাপ ১০) আবিষ্কার হয়েছে এটা `confirmReleaseAndComplete()` (money-critical
wallet-deduction/escrow-release choke point) সরাসরি কল করে RELEASE_TO_SOLVER-জাতীয়
resolution-এ। এছাড়া `admin_credentials_update`/`admin_credentials_verify_password` সত্যিই টাকা
স্পর্শ করে না তা-ও নিশ্চিত হতে হবে (নাম শুনে money-adjacent মনে হতে পারে কিন্তু সম্ভবত শুধু
login-credential পরিবর্তন)। কোনো 🟡 partial কাজ বাকি নেই — ধাপ ১০ সম্পূর্ণ ও সেভাবেই বন্ধ হচ্ছে।

---

## বাগ ফিক্স (ধাপ ১০-এর পরে, ধাপ ১১ শুরুর আগে, এই সেশনে) — `NetworkConnectivityObserver` সবসময় `true` রিটার্ন করছিল

### ইউজার-রিপোর্ট করা সমস্যা
Admin panel থেকে Strict Offline Block টগল **ON** করা থাকলেও অফলাইনে গেলে `NoInternetOverlay`
(পুরনো ফুল-ব্লক পেজ) দেখা যাচ্ছিল না।

### রুট-কজ (rule ১০ অনুযায়ী কোড পড়ে বের করা হলো)
এটা এই প্রজেক্টের কোনো ধাপের তৈরি করা বাগ না — **আগে থেকেই বিদ্যমান** একটা বাগ
`app/src/main/java/com/example/util/NetworkConnectivityObserver.kt`-এ, যেটা `MainActivity.kt`-এর
`isOnline` নির্ধারণ করে। ধাপ ৩-এর টগল-ব্রাঞ্চিং লজিক (`if (isStrictOfflineBlockEnabled) {
NoInternetOverlay(...) } else { OfflineStatusBanner(...) }`) নিজে সঠিক ছিল, কিন্তু
`isCurrentlyConnected()`-এর একদম শেষে একটা ক্যাচ-অল `return true` fallback ছিল (কমেন্ট: "prevent
false-positive lockouts") — অর্থাৎ `activeNetwork`/`activeNetworkInfo`/`allNetworks` কোনোটাতেই
সত্যিকারের সংযোগ না পেলেও (যেমন airplane mode-এ) ফাংশনটা তবু `true` রিটার্ন করত। একই সমস্যা
`isConnectedFlow`-এর `trySend(true)` কলগুলোতেও (initial emission, `onAvailable`,
`onCapabilitiesChanged`) ছিল — কমেন্টে "real-time check" লেখা থাকলেও আসলে hardcoded `true` ছিল।
ফলে `isOnline` কার্যত প্রায় সবসময়ই `true` থাকত, টগলের মান যা-ই হোক।

⚠️ গুরুত্বপূর্ণ: এই বাগ ধাপ ৪–১০-এর `requireOnlineOrWarn()` গার্ডগুলোকে প্রভাবিত করেনি —
`requireOnlineOrWarn()` `SomadhanViewModel.kt`-এর নিজস্ব `isOnline: StateFlow<Boolean>` ব্যবহার
করে, যেটা `startConnectivityObserver()`-এর সম্পূর্ণ আলাদা, ইতিমধ্যেই সঠিক ইমপ্লিমেন্টেশন থেকে
আসে (`_isOnline.value = activeCaps?.hasCapability(...) == true`, hardcoded true কোথাও নেই)। শুধু
`MainActivity.kt`-এর নিজস্ব `NetworkConnectivityObserver` ইনস্ট্যান্স (গ্লোবাল ওভারলে/ব্যানারের
জন্য ব্যবহৃত) প্রভাবিত ছিল।

### কী পরিবর্তন হলো (১টা ফাইল, শুধু `NetworkConnectivityObserver.kt`)
- `isCurrentlyConnected()`: শেষের fallback `return true` → `return false` (উপরের তিনটা বাস্তব
  চেক কোনোটাতে সংযোগ না পেলে এখন সত্যিই "অফলাইন" রিপোর্ট করে)।
- `isConnectedFlow`: initial `trySend(true)`, `onAvailable`, `onCapabilitiesChanged` — তিনটাতেই
  hardcoded `true`-এর বদলে এখন `trySend(isCurrentlyConnected())`, ঠিক `onLost`/`onUnavailable`-এ
  আগে থেকেই যেভাবে হতো সেভাবেই।
- `connectivityManager == null` (system service না পাওয়া গেলে) এবং `registerNetworkCallback`
  থ্রো করলে — এই দুই বিরল edge-case-এ `true` fallback ইচ্ছাকৃতভাবে অপরিবর্তিত রাখা হলো (device-level
  সমস্যা, প্রকৃত "নেটওয়ার্ক নেই" অবস্থা না — ঝুঁকিপূর্ণ পরিবর্তন এড়াতে রুল ১ অনুযায়ী স্পর্শ করা হয়নি)।

### ফলাফল (এখন যা কাজ করবে)
- **টগল ON + অফলাইন** → `isOnline` সঠিকভাবে `false` হবে → `NoInternetOverlay` (ফুল-ব্লক পেজ)
  দেখাবে, ঠিক ইউজারের চাওয়া অনুযায়ী।
- **নেটওয়ার্ক ফিরলে** → `onAvailable`/`onCapabilitiesChanged` এখন real-time `isCurrentlyConnected()`
  পাঠাবে → `isOnline` `true` হবে → overlay স্বয়ংক্রিয়ভাবে বন্ধ হয়ে যাবে (আগে থেকেই বিদ্যমান
  `AnimatedVisibility`/`manualOverrideConnected`-reset লজিক অপরিবর্তিত)।
- **টগল OFF + অফলাইন** → আগের মতোই অ্যাপে ঢোকা যাবে, শুধু `OfflineStatusBanner` (চিকন bar)
  দেখাবে, action-গুলো `requireOnlineOrWarn()` দিয়ে আলাদাভাবে ব্লক থাকবে (অপরিবর্তিত, এই ফিক্সে
  হাত পড়েনি)।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`NetworkConnectivityObserver.kt` — এডিটের আগে-পরে `()`/`{}`/`[]` প্রতিটাই সমান (৬৯/৬৯, ২২/২২,
২/২, delta = 0)। `diff` দিয়ে যাচাই করা হয়েছে শুধু ইচ্ছাকৃত লাইনগুলোই বদলেছে। আসল build ভেরিফাই
Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
ইনপুট (ধাপ ১০-এর আউটপুট, ৩৪২টা ফাইল) আর এই বাগ-ফিক্সের আউটপুট zip-এর ফাইল-লিস্ট সম্পূর্ণ
অভিন্ন — শুধু `NetworkConnectivityObserver.kt` কন্টেন্ট বদলেছে (এই progress ফাইল ছাড়া)।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
ধাপ ৯-এর "পরবর্তী সেশন" এন্ট্রিতে লেখা **ধাপ ১১ — Admin misc, KYC, Role/Account, Notifications,
Direct contracts** থেকেই শুরু হবে (এই বাগ-ফিক্স ধাপের numbering-এ হস্তক্ষেপ করেনি, শুধু ধাপ ১০ ও
১১-এর মাঝে একটা আলাদা bug-fix সেশন হিসেবে যোগ হলো)।

---

## ধাপ ১১ — Admin misc, KYC, Role/Account, Notifications, Direct contracts — ✅ সম্পূর্ণ (দুই সেশনে, দ্বিতীয়/শেষ অংশ এই সেশনে)

⚠️ **🟡→✅ partial flag ক্লিয়ার হলো (rule ৮):** ইউজারের নির্দেশে ধাপ ১১ দুই সেশনে ভাগ হয়েছিল। প্রথম
সেশনে (আগের এন্ট্রি দেখুন) ~অর্ধেক কোড-এডিট হয়েছিল। এই সেশনে বাকি অর্ধেক (নিচে ✅ (২য় অংশ) তালিকা)
শেষ হলো, তাই ধাপ ১১ এখন সম্পূর্ণ। পরের সেশন ইউজারের কনফার্মেশন নিয়ে ধাপ ১২-তে যাবে।

### কোড পড়ে যা নিশ্চিত হয়েছে (rule ১০) — সব VM entry point `SomadhanViewModel.kt`-এ
সাধারণ প্যাটার্ন: `viewModelScope.launch {`-এর ঠিক পরে `if (!requireOnlineOrWarn(msg)) { onError(msg); return@launch }`
(callback থাকলে) বা `... ) return@launch` (callback না থাকলে) — ধাপ ৫–১০-এর প্যাটার্নের হুবহু অনুরূপ।

### ✅ এই সেশনে সম্পন্ন (২ ফাইল: `SomadhanViewModel.kt`, `LoginScreen.kt`) — ২০টা গার্ড
| ফাংশন | ধরন | কারণ |
|---|---|---|
| `submitKyc` (৩টা overload সবগুলো) | সাধারণ | local Room-এ "pending" বসে, cloud ব্যর্থ হলে শুধু log → admin আবেদন দেখত না। (ছবি আপলোড `SolverKycScreen`-এ `KycUploadManager` দিয়ে আগেই হয়, অফলাইনে সেটা এমনিতেই error দেখায় — অপরিবর্তিত) |
| `switchRoleToSolver`, `switchRoleToUser` | সাধারণ | `switchRoleInPlace()` local role আগে flip করে |
| `clearSolverCancelledNotice` | সাধারণ | problem reset + bids CANCELLED local-first |
| `createDirectContractProject` | সাধারণ | problem insert + cloud dual-write |
| `acceptDirectContractProposal` | 🔴 rule ২ | local `EscrowEntity` বানায় (acceptBid-এর মতো) |
| `declineDirectContractProposal` | সাধারণ | টাকা নেই |
| `adminUpdateDirectContractStatus` | 🔴 rule ২ | COMPLETED→`confirmReleaseAndComplete()`, CANCELLED→refund (ধাপ ১০-এর choke-point আবিষ্কার অনুযায়ী) |
| `adminCancelAndRefundDirectContract` | 🔴 rule ২ | `refundEscrowOnce()` |
| `adminFactoryResetAllData` | সবসময় সম্পূর্ণ ব্লক | `clearAllDatabaseAndReset()` আগে local wipe, cloud ব্যর্থ হলে শুধু log → অফলাইনে local ডেটা যেত (ইউজার কনফার্মড, ধাপ ১) |
| `requestWithdrawal` | 🔴 rule ২ | Group A হলেও LOCAL INSTANT debit — ধাপ ৬-এর গ্যাপ (ধাপ ৮-এর `adminRefundEscrow` precedent) |
| `adminAdjustBalance` | 🔴 rule ২ | Group A হলেও LOCAL INSTANT credit/debit (precedent একই) |
| `adminUpdateWithdrawalStatus` | 🔴 rule ২ | Group A হলেও REJECTED-এ LOCAL optimistic refund |
| `adminUpdateWithdrawalTrxId`, `adminDeleteUser`, `adminDeleteProblem`, `adminReassignSolver`, `adminAdjustReputation` | সাধারণ (callback নেই, শুধু `return@launch`) | local-first + cloud best-effort |

**⚠️ compile-gotcha (গুরুত্বপূর্ণ):** `switchRoleToUser`-এর নতুন `onError` প্যারামিটার ইচ্ছাকৃতভাবে
`onSuccess`-এর **আগে** বসানো: `fun switchRoleToUser(onError: (String) -> Unit = {}, onSuccess: () -> Unit)`।
কারণ `LoginScreen.kt`-এর trailing-lambda কল `switchRoleToUser { ... }` শেষ প্যারামিটারে bind হয়; `onError` শেষে
থাকলে কম্পাইল ভাঙত। `requestSwitchRole()` এখন `switchRoleToUser(onSuccess = onSuccess, onError = onError)` কল করে
(ProfileScreen-এর spinner আটকে থাকা এড়াতে)। `LoginScreen.kt`-এ কলটা `switchRoleToUser(onError = { isSelectingLoginRole = false }) { ... }` হয়েছে।

**Group A সিদ্ধান্ত:** `requestWithdrawal`/`adminAdjustBalance`/`adminUpdateWithdrawalStatus`-এ rule ২-কে প্রাধান্য
দেওয়া হয়েছে (ধাপ ৮-এর precedent)। ⚠️ ইউজার এই প্রশ্নের স্পষ্ট উত্তর দেননি — assistant-এর সুপারিশ অনুযায়ী এগোনো
হয়েছে; ধাপ ১২ (Group A ভেরিফিকেশন) এই সিদ্ধান্ত রিভিউ করবে। বাকি Group A (`adminApproveKyc/RejectKyc/RevokeKyc/
BulkApproveKyc`, `adminSetBanned/Restricted/VerifiedBadge`, `adminChangeRole`) rule ৩ অনুযায়ী **অপরিবর্তিত** — টাকা নেই।

### ✅ (২য় অংশ, এই সেশনে সম্পন্ন) — বাকি ছিল যা, তার পুরোটাই — ৩০টা গার্ড, শুধু `SomadhanViewModel.kt`
কোড আগে পড়ে (rule ১০) প্রতিটার repository-বডি যাচাই করে বসানো হয়েছে। সবই সাধারণ Group B — কোনোটাই
money-critical (rule ২) না।

১. **Role/Account/Notification/Report:**
   - `adminUpdateCredentials(newPhone, currentPassword, newPassword, onResult)` — গার্ড `try`-এর ঠিক আগে,
     `verifyAdminPassword` (local)-এর আগেই বসানো হলো (আগেই প্ল্যান ছিল)। ব্যর্থ হলে `onResult(false, msg)`।
   - `adminResetUserPassword` — local bcrypt hash-এর আগে গার্ড; `admin-reset-user-password` Edge Function
     dual-write best-effort (কোনো callback নেই, শুধু `return@launch`)।
   - `adminSendManualNotification` (`onError` আছে → `onError(msg)` + `return@launch`), `adminCancelScheduledNotification`,
     `adminDeleteManualNotification` (দুটোতেই কোনো callback নেই)। `ScheduledNotificationWorker` (cron)
     কোড পড়ে যাচাই করা হলো — সেটা repository-লেভেলে সরাসরি চলে, VM গার্ড এড়িয়ে যায়, ইচ্ছাকৃতভাবে অপরিবর্তিত।
   - `reportAbuse(...)` — `onError` নেই, শুধু `onSuccess` → ব্যর্থ হলে toast + `return@launch`।
   - (`reportUser()`-এর caller কোড-গ্রেপ করে পুনঃনিশ্চিত করা হলো — সত্যিই কোনো UI caller নেই, dead, গার্ড দরকার নেই।)
২. **Admin config CRUD (আগের ইনভেন্টরিতে ছিল না, এই ধাপে আবিষ্কৃত গ্যাপ):** `adminAddCategory`,
   `adminUpdateCategory`, `adminToggleCategoryActive`, `adminDeleteCategory` (repository-বডি পড়ে কনফার্ম করা হলো
   `admin_remove_category_from_solvers` cascade-ও এর ভেতরেই কল হয় — একই গার্ড দুটোকেই কভার করে),
   `adminToggleCategoryInstantJob`, `adminAddFaq`/`adminUpdateFaq`/`adminDeleteFaq` (`onSuccess` আছে, `onError` নেই →
   শুধু toast + `return@launch`), `adminUpdatePlatformSetting`, `adminBatchUpdatePlatformSettings`,
   `adminSetPhysicalWorkEnabled`, `adminSetVirtualWorkEnabled`, `adminSaveCustomReputationEvent`,
   `adminDeleteCustomReputationEvent`, `adminToggleCustomReputationEventStatus`, `adminCleanupCorruptedCommissionRates`,
   `adminUpdateKycInfo`, `adminResetKycToPending`। সবগুলোতেই কোনো callback নেই (শুধু `return@launch`), বাদে
   `adminAddFaq`/`adminUpdateFaq`/`adminDeleteFaq` (`onSuccess` আছে) ও `adminCleanupCorruptedCommissionRates`
   (`onComplete` আছে)।
   ⚠️ `adminUpdatePlatformSetting` `strict_offline_block` টগলও সেভ করে (AdminSettingsView) — গার্ড এখানে **সঠিক**
   (অফলাইনে টগল বদলালে শুধু local cache বদলাত, পরের login-এ cloud মান আবার লোকাল ওভাররাইট করত)। `AdminInstantJobsView.kt`
   কোড পড়ে দেখা হলো — সেখানেও `adminUpdatePlatformSetting` একই ViewModel ফাংশন কল করে, তাই একই গার্ড স্বয়ংক্রিয়ভাবে কভার করে,
   আলাদা কোনো spinner-lock সমস্যা পাওয়া যায়নি।
৩. **প্রোফাইল-এডিট (প্রশ্ন ৩ — ইউজারকে জিজ্ঞেস করা হয়েছিল, উত্তর: গার্ড বসাও):** `updateProfile` (`onError` নেই →
   toast + `return@launch`), `updatePhoneNumber` (`onError` আছে → `onError(msg)` + `return@launch`),
   `updateSolverSkills` (`onError` আছে, একই প্যাটার্ন), `updateProfileImage` (callback নেই — `ProfileScreen.kt`-এ
   ছবি মুছার (`imageUri=""`) সরাসরি কল-ও পাওয়া গেছে কোড-গ্রেপে, তাই গার্ড দরকার ছিল, শুধু
   `uploadAndSaveProfileImage()`-এর সফল-আপলোড-পরবর্তী কল না), `updateUser` (callback নেই; কল-সাইট
   `ReputationDetailScreen.kt:632` কোড পড়ে কনফার্ম করা হলো, গার্ডে প্রভাবিত হবে কিন্তু নিরাপদ), `toggleFavoriteSolver`
   ও `addFavoriteSolver` (`onResult: (Boolean) -> Unit` → ব্যর্থ হলে `onResult(false)` + `return@launch`)।
   ⚠️ `syncUserLocationToDb()` (লোকেশন অটো-ট্র্যাকিং টিকার, `repository.updateUser()` সরাসরি কল করে)
   **ইচ্ছাকৃতভাবে গার্ড বসেনি** — এটা ব্যাকগ্রাউন্ড/cron-জাতীয় অটোমেটিক ফ্লো (ইউজার-ট্যাপ করা অ্যাকশন না), প্রশ্ন ৩-এর
   স্কোপ শুধু ইউজার-উদ্যোগী প্রোফাইল-এডিট বাটনগুলোকে নিয়ে ছিল।

**ইচ্ছাকৃতভাবে গার্ড নেই (rule ১, আগের মতোই অপরিবর্তিত):** `markNotificationRead`/`markAllNotificationsRead`
(mark-seen), `adminLogChatView`/`logAdminAction` (audit log), `reportUser` (dead), `ScheduledNotificationWorker`
(cron), `syncUserLocationToDb()` (auto-tracking ticker, উপরে ব্যাখ্যা)।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SomadhanViewModel.kt` — এডিটের আগে-পরে `()` ৩৩৮৮/৩৩৮৮ → ৩৪৮৪/৩৪৮৪, `{}` ১৪০৭/১৪০৭ → ১৪১৩/১৪১৩, `[]` ১৫৫/১৫৫ →
১৭১/১৭১ (delta = 0 তিনটাতেই, ইনপুট (`step11-part1.zip`) ভার্সনের সাথে তুলনা করে)। `diff` দিয়ে যাচাই করা হয়েছে: মোট
৮৯টা লাইন যোগ হয়েছে, বিদ্যমান **একটাও** লাইন মোছেনি/বদলায়নি (শুধু নতুন গার্ড-লাইন ও কমেন্ট সন্নিবেশ)। আসল build
ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
ইনপুট (`somadhan-offline-action-gating-step11-part1.zip`, ৩৪২টা ফাইল, dotfile-সহ) আর এই সেশনের আউটপুট
`somadhan-offline-action-gating-step11-part2.zip`-এর ফাইল-লিস্ট অভিন্ন (`unzip -Z1 | sort` + `diff` দিয়ে যাচাই:
৩৪২ = ৩৪২, কোনো ফাইল হারায়নি/যোগ হয়নি, `.env`/`.gitignore` সহ)। কনটেন্ট বদলেছে শুধু ২টা ফাইল: `SomadhanViewModel.kt`
ও এই progress ফাইল।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ১২ — গ্রুপ A ভেরিফিকেশন** (Outbox-covered action গুলো ধাপ ৩-এর টগল-ব্রাঞ্চিং-এর পরে সত্যিই offline-এ ঠিকভাবে
queue হচ্ছে ও `OutboxPendingIndicator` দেখাচ্ছে কিনা কোড-রিভিউ দিয়ে যাচাই)। বিশেষভাবে ধাপ ১১-এর প্রথম অংশে নেওয়া
Group A rule-২ সিদ্ধান্তগুলো (`requestWithdrawal`, `adminAdjustBalance`, `adminUpdateWithdrawalStatus` — এগুলোতে
rule ২-কে প্রাধান্য দিয়ে হার্ড-গার্ড বসানো হয়েছিল, ইউজার তখন স্পষ্ট উত্তর দেননি) রিভিউ করে দেখা দরকার এই সিদ্ধান্ত
ঠিক ছিল কিনা। ধাপ ১১ সম্পূর্ণ (rule ৮ partial flag ক্লিয়ার) — কোনো 🟡 কাজ বাকি নেই। কোনো Android Studio build
ভেরিফাই এখনো হয়নি (rule ৯), সেটা ইউজার নিজে করবেন।

---

## ধাপ ১২ — গ্রুপ A ভেরিফিকেশন — ✅ সম্পূর্ণ

### যা কোড-রিভিউ দিয়ে যাচাই হলো

**১. ধাপ ১১-এর rule-২ সিদ্ধান্ত রিভিউ (`requestWithdrawal`, `adminAdjustBalance`,
`adminUpdateWithdrawalStatus`) — ✅ সিদ্ধান্ত সঠিক ছিল।** তিনটার `SomadhanViewModel.kt`-এর
বডি সরাসরি পড়ে কনফার্ম করা হলো তিনটাতেই `requireOnlineOrWarn(...)` hard-guard বসানো আছে (rule ২
অনুযায়ী, toggle-independent) — অর্থাৎ এগুলো Group A ক্যাটাগরির হলেও আদতে কখনো Outbox-এ queue
*হতেই* পারে না (অফলাইনে সম্পূর্ণ ব্লক থাকে, action শুরুই হয় না)। এটাই সঠিক আচরণ, কারণ তিনটাই local
instant balance credit/debit করে (`requestWithdrawal`-এ solver-এর balance আগে থেকেই কমে যায়,
`adminAdjustBalance`-এ সরাসরি credit/debit, `adminUpdateWithdrawalStatus`-এর REJECTED পাথে
optimistic refund) — money-critical, তাই rule ২ (hard-block) Group A precedent-এর ওপরে প্রাধান্য
পাওয়াই ঠিক।

**২. Outbox dispatcher/repository wiring যাচাই।** `OutboxRpcDispatcher.kt` গ্রেপ করে কনফার্ম করা
হলো এই দুই স্ক্রিন-সম্পর্কিত সব RPC নাম (`admin_approve_kyc`, `admin_reject_kyc`,
`admin_revoke_kyc`, `admin_set_banned`, `admin_set_restricted`, `admin_set_verified_badge`,
`admin_change_role`) dispatcher-এ রেজিস্টার করা আছে। `SomadhanRepository.kt`-এ একটা নমুনা
(`adminSetBanned()`) সম্পূর্ণ পড়ে কনফার্ম করা হলো প্যাটার্ন ঠিক আছে: local write আগে → cloud
dual-write চেষ্টা → ব্যর্থ হলে `enqueueOutboxRetry(rpcName = "admin_set_banned", ...)` (dispatcher-এর
branch-এর প্যারাম-কী-এর সাথে মিলিয়ে কমেন্টে নোট করা আছে)।

**৩. `reconcileEscrowStates()` — self-healing escrow routine (প্রতি refresh/login-এ অটো চলে,
বাটন-চাপ নেই)।** কখনো সরাসরি escrow payout/refund করে (self-heal Case-2/Case-3, `SomadhanRepository.kt`
লাইন ~৪৪৭৫-এ)। এতদিন `checkAndProcess48HourAutoReleases()`-এর মতোই কোনো ViewModel-গার্ড ছাড়া রাখা
হয়েছিল। কোড পড়ে কনফার্ম করা হলো — এটা সত্যিই `SomadhanViewModel.kt`-এর একাধিক জায়গা (লগইন/রিফ্রেশ
flow) থেকে সরাসরি `repository.reconcileEscrowStates()` কল করে, ঠিক cron-জাতীয় sweep-এর মতোই কোনো
UI বাটন-ইভেন্ট থেকে না।

**⚠️ প্রশ্ন ১ (ইউজারকে জিজ্ঞেস করা হয়েছিল): এটা toggle OFF অবস্থায় অফলাইনে কী করবে?**
> **উত্তর: আগের মতোই অপরিবর্তিত রাখা হলো (cron-precedent অনুযায়ী, কোনো কোড পরিবর্তন হয়নি)।**
> কোনো money-loss ঝুঁকি নেই (network না থাকলে cloud dual-write নিজে থেকেই ব্যর্থ হবে, local self-heal
> শুধু already-held escrow-কে সঠিক লোকাল স্টেটে আনে) — pure design/UX প্রশ্ন ছিল, rule ১ অনুযায়ী কোড
> স্পর্শ করা হয়নি।

**৪. `OutboxPendingIndicator` কভারেজ গ্যাপ — Group A KYC/ban/restrict/verified-badge/role-change
অ্যাকশনের স্ক্রিনে কোনো ইন্ডিকেটর নেই।** ইন্ডিকেটর এতদিন শুধু ৩টা স্ক্রিনে ছিল (Wallet, Withdrawal
History, Admin Withdrawals) — কিন্তু Group A-তে KYC approve/reject/revoke/bulk-approve ও
ban/restrict/verified-badge/role-change-ও আছে, যেগুলো `AdminUsersView.kt`/`AdminUserLookupView.kt`-এ
হয় (ban/restrict/verified-badge/role-change, এবং `AdminUserLookupView.kt`-এ KYC reject/revoke/approve-ও)।
অফলাইনে queue হলে অ্যাডমিন pending sync দেখতে পেত না (শুধু Wallet স্ক্রিনে গেলে গ্লোবাল কাউন্ট দেখত)।

**⚠️ প্রশ্ন ২ (ইউজারকে জিজ্ঞেস করা হয়েছিল): কী করব?**
> **উত্তর: এই ধাপেই ঐ admin স্ক্রিনগুলোতেও ইন্ডিকেটর যোগ করা হলো।**

**🔍 এই ধাপে একটা সংশোধন পাওয়া গেছে (rule ১০ অনুযায়ী কোড পড়ে যাচাই করতে গিয়ে):** ইউজারের প্রশ্নে
তৃতীয় স্ক্রিন হিসেবে `SolverKycScreen.kt`-এর নাম ছিল, কিন্তু কোড গ্রেপ করে দেখা গেল
`adminApproveKyc()`/`adminRejectKyc()`/`adminRevokeKyc()`/`adminBulkApproveKyc()` (KYC-এর Group A
অংশ) আসলে `SolverKycScreen.kt`-এ নেই — ওটা শুধু সলভারের নিজের `submitKyc()` সাবমিশন স্ক্রিন, আর
`submitKyc()` নিজেই ধাপ ১১-এ Group B হিসেবে hard-guard পেয়েছে (`requireOnlineOrWarn`-এ ব্লক, Outbox
queue হয় না — কোড কমেন্টেই স্পষ্ট লেখা "সাধারণ Group B")। তাই `SolverKycScreen.kt`-এ ইন্ডিকেটর বসালে
আসলে কখনো দেখাতই না (কোনো Group A pending সেখানে জমা হয় না)। admin KYC approve/reject/revoke
আসলে wire হয় `AdminKycView.kt`-এ (`AdminPanelScreen.kt`-এর কলব্যাক গ্রেপ করে কনফার্ম করা হলো), যেখানে
আগে থেকেই কোনো ইন্ডিকেটর ছিল না। তাই `SolverKycScreen.kt`-এর বদলে **`AdminKycView.kt`**-তে ইন্ডিকেটর
বসানো হলো — এটাই আসল Group A gap পূরণ করে।

### কী পরিবর্তন হলো (৩টা ফাইল, শুধু ইন্ডিকেটর যোগ, কোনো বিদ্যমান লজিক বদলায়নি)
Wallet/WithdrawalHistory/AdminWithdrawals-এর মতোই একই প্যাটার্ন (import + `outboxPendingCount`
StateFlow collect + `OutboxPendingIndicator(...)` কল, স্ক্রিনের একদম ওপরে):
- `AdminUsersView.kt` — `viewModel` non-null, তাই সরাসরি `viewModel.outboxPendingCount`। মূল
  লিস্ট-কার্ডের ঠিক ওপরে বসানো হলো (`AdminWithdrawalsView`-এর প্যাটার্ন)।
- `AdminUserLookupView.kt` — `viewModel` non-null, সার্চ কার্ডের ঠিক ওপরে।
- `AdminKycView.kt` — `viewModel` এখানে ঐচ্ছিক (`SomadhanViewModel? = null`), তাই
  `viewModel?.outboxPendingCount?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(0) }`
  fallback (`AdminWithdrawalsView`-এর একই null-safe প্যাটার্ন)। এই ফাইলে মূল লেআউট `LazyColumn`
  (plain `Column` না) বলে ইন্ডিকেটরটা একটা `item { }` ব্লকে, সার্চ বারের ঠিক ওপরে বসাতে হলো।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
তিনটা ফাইলেই এডিটের পর `()`/`{}`/`[]` প্রতিটা জোড়া সমান: `AdminUsersView.kt` (৭৪৭/৭৪৭, ৩০৪/৩০৪,
১৬/১৬), `AdminUserLookupView.kt` (১২০০/১২০০, ৫৫৪/৫৫৪, ১৫/১৫), `AdminKycView.kt` (৯৬১/৯৬১, ৩৬৬/৩৬৬,
২/২)। `diff` দিয়ে ইনপুট zip-এর ভার্সনের সাথে তুলনা করে যাচাই করা হয়েছে — তিনটা ফাইলেই শুধু নতুন
import + state-val + `OutboxPendingIndicator(...)` ব্লক *যোগ* হয়েছে, বিদ্যমান একটা লাইনও মোছেনি বা
বদলায়নি। আসল build ভেরিফাই Android Studio-তে হবে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
ইনপুট (`somadhan-offline-action-gating-step11-part2.zip`, ৩৪২টা এন্ট্রি — ২৯৩ ফাইল + ৪৯ ডিরেক্টরি,
dotfile-সহ) আর এই ধাপের আউটপুট `somadhan-offline-action-gating-step12.zip`-এর ফাইল-লিস্ট
(`unzip -Z1 | sort` + `diff`) **সম্পূর্ণ অভিন্ন** — কোনো ফাইল হারায়নি/যোগ হয়নি, `.env`/`.gitignore`
সহ। কন্টেন্ট বদলেছে শুধু ৩টা কোড ফাইল (`AdminUsersView.kt`, `AdminUserLookupView.kt`,
`AdminKycView.kt`) ও এই progress ফাইল।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ১৩ — গ্রুপ C (read/cached-view) যাচাই।** প্রতিটা স্ক্রিনের initial-load/pull-to-refresh টগল OFF
মোডে অফলাইনে blank/error না দেখিয়ে cached Room ডেটা দেখাচ্ছে কিনা যাচাই করতে হবে; প্রয়োজনে ছোট
"অফলাইন — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে" ইনডিকেটর যোগ করতে হবে (full block না, master prompt-এর ধাপ
১৩ দেখুন)। ধাপ ১২ সম্পূর্ণ, কোনো 🟡 কাজ বাকি নেই। কোনো Android Studio build ভেরিফাই এখনো হয়নি
(rule ৯), সেটা ইউজার নিজে করবেন।

---

## ধাপ ১৩ — গ্রুপ C (read/cached-view) যাচাই — ✅ সম্পূর্ণ (এই সেশনে)

### কোড পড়ে যা নিশ্চিত হয়েছে (rule ১০; ডিভাইসে রান করা হয়নি — শুধু কোড-রিভিউ)
- `SupabaseRealtimeManager.pullBulkDataFromSupabase()`-এর প্রতিটা টেবিল আলাদা `runCatching`-এ মোড়ানো,
  তাই অফলাইনে সব টেবিল ব্যর্থ হলেও `isSuccess=true` (শুধু `error != null`) রিটার্ন হয় এবং
  `initialSyncPhase` `LOADED` হয়, `ERROR` না। ফলে `SyncAwareContent`/`SyncAwareRefreshableContent`-এর
  সব call-site (~৩৩ স্ক্রিন + AdminPanel-এর ২৪ ট্যাব) অফলাইনে `SyncErrorState`-এর বদলে cached Room
  ডেটাই দেখায় — এই অংশে কোনো কোড পরিবর্তন লাগেনি।
- `categoriesSyncPhase`/`faqsSyncPhase` local-first (Room seed), অফলাইনে ERROR হয় না — যাচাই সম্পূর্ণ।
- আলাদা per-screen "অফলাইন — শেষ সংগৃহীত তথ্য" ইন্ডিকেটর **যোগ করা হয়নি** — ধাপ ৩-এর গ্লোবাল
  `OfflineStatusBanner` ("ইন্টারনেট নেই — শুধু আগের ডেটা দেখা যাচ্ছে") ইতিমধ্যে সেই কাজ করে (ইউজার কনফার্মড)।
- ⚠️ "wifi আছে কিন্তু ইন্টারনেট নেই" (dead network) অবস্থা — নিচের "ফলো-আপ" সেকশনে সম্বোধন করা হয়েছে।

### 🔍 দুটো গ্যাপ পাওয়া গেছে ও ঠিক করা হয়েছে (ইউজার কনফার্মড)
**Gap A — অফলাইনে app খোলার পর reconnect-এ stale ডেটা।** phase `LOADED` থাকায়
`retryAllErroredSyncPhasesOnReconnect()` কিছু retry করত না, আর `checkListenerHealthAndFallbackSync()`
শুধু `listenerLastHeardMs != null` key-কে stale ধরে (অফলাইন-স্টার্টে সব null) — তাই অফলাইনে cloud-এ
হওয়া পরিবর্তন পরের অনলাইন app-open পর্যন্ত লোকালে আসত না।
- `SupabaseRealtimeManager.kt`: নতুন `initialPullIncomplete` (AtomicBoolean), `performInitialSync()`-এ
  `result.error != null || !isSuccess` (আর onFailure-এ `true`) দিয়ে সেট; নতুন `hasIncompleteInitialPull()`
  ও `catchUpIncompleteInitialPull()` (একই `initialSyncInFlight` guard, `_initialSyncPhase` **অপরিবর্তিত**
  — তাই skeleton ঝলক নেই)।
- `SomadhanViewModel.kt`: `retryAllErroredSyncPhasesOnReconnect()`-এর শুরুতে — phase ERROR না হলে ও
  ফ্ল্যাগ true হলে নীরব catch-up (আলাদা cooldown key `"initial_catchup"`)।
- নোট: কোনো টেবিল স্থায়ীভাবে ব্যর্থ হলে (যেমন RLS) ফ্ল্যাগ true থেকে যাবে, প্রতিটা false→true reconnect-এ
  (৫s cooldown সহ) catch-up আবার চলবে — ক্ষতিকর না, শুধু একটা বাড়তি pull।

**Gap B — অফলাইন pull-to-refresh-এ মিথ্যা success toast।** `refreshData()`, `refreshWalletData()`,
`refreshAdminTab()`-এর শুরুতে `if (!isOnline.value) { showToast("ইন্টারনেট নেই — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে"); return }`
(স্পিনার শুরুর আগে)। Strict মোডে অফলাইনে ইউজার এখানে পৌঁছায় না, তাই এটা শুধু non-strict মোডে কার্যকর।
পার্শ্বপ্রভাব: অফলাইনে ম্যানুয়াল pull আর `reconcileEscrowStates()`/48h-sweep চালায় না — অন্য auto-run
পথ (login/refresh online) অপরিবর্তিত, তাই ঝুঁকি নেই।
`triggerCloudSync()` ও `refreshAdminMetrics()` স্কোপের বাইরে রাখা হয়েছে, অপরিবর্তিত।

### কী পরিবর্তন হলো (৩টা ফাইল)
`SupabaseRealtimeManager.kt` (+৩৮), `SomadhanViewModel.kt` (+৬৪), `MotionToolkit.kt` (+২১, −২ ইচ্ছাকৃত ERROR-ব্রাঞ্চ প্রতিস্থাপন)।

### 🔧 ফলো-আপ (ইউজার কনফার্মড: "যেটা ভালো হবে সেটা করো, কোনো ফাংশন নষ্ট করো না") — dead-network ERROR স্ক্রিন
**সমস্যা (কোড পড়ে):** `SupabaseClientProvider`-এ connect timeout ১৫s, request/socket ৩০s, আর
`performInitialSync()`-এর `withTimeout(20s)`। তাই "wifi আছে কিন্তু ইন্টারনেট নেই" অবস্থায়: connect ১৫s-এর
মধ্যে ব্যর্থ হলে → ~১৫s skeleton → LOADED (cached ডেটা); connect হয়ে response hang করলে → ২০s-এ
`withTimeout` ফায়ার → phase `ERROR` → cached ডেটা থাকা সত্ত্বেও `SyncErrorState`। এই অবস্থায়
`_isOnline`-ও `true` (শুধু `NET_CAPABILITY_INTERNET` দেখে, `VALIDATED` না), তাই কোনো ব্যানারও আসে না।

**সিদ্ধান্ত:** `VALIDATED` চেক যোগ করা হয়নি (`requireOnlineOrWarn()`-সহ অনেক গার্ডকে প্রভাবিত করত —
ঝুঁকিপূর্ণ)। বদলে ERROR-ব্রাঞ্চে সংকীর্ণ, শর্তসাপেক্ষ cached-fallback:
- `SomadhanViewModel.kt`: `hasCleanSyncHistory()` + private `recordCleanSyncIfApplicable()` (SharedPreferences
  key `clean_bulk_sync_<userId>`, `saved_user_id`-ভিত্তিক); `init{}`-এ `initialSyncPhase` collector — `LOADED` ও
  `!hasIncompleteInitialPull()` হলে persist। শুধু observe, কোনো sync-আচরণ বদলায় না। (`last_full_sync_at`
  ব্যবহার করা যায়নি — ওটা ব্যর্থ pull-এর পরেও লেখা হয়।)
- `SupabaseRealtimeManager.kt`: `initialPullIncomplete` সেট এখন phase-এর *আগে* (collector-এর race এড়াতে)।
- `MotionToolkit.kt`: private `rememberShowCachedOnSyncError()` (`!strictBlock && hasCleanSyncHistory()`);
  `SyncAwareContent` ও `SyncAwareRefreshableContent`-এর ERROR ব্রাঞ্চ: শর্ত সত্য হলে `content()`/`content(data)`,
  নইলে আগের হুবহু `SyncErrorState`। (এই দুটো ছাড়া কোনো লাইন মোছা হয়নি — ঠিক ঐ দুই ERROR লাইন প্রতিস্থাপিত।)
- **যা অপরিবর্তিত:** Strict মোড (সবসময় আগের মতো ERROR স্ক্রিন); ইউজার যার কখনো clean sync হয়নি (fresh
  login ইত্যাদি) সে আগের মতোই retry-সহ ERROR স্ক্রিন পাবে; ON_RESUME auto-retry, reconnect retry,
  pull-to-refresh-এর retry-if-ERROR; `SyncBlockedRetryState`; LOADING/skeleton আচরণ (~১৫s skeleton-এর
  সম্ভাবনা রয়ে গেছে — ইচ্ছাকৃত, কারণ first-visit skeleton ডিজাইন অনলাইনেও এর উপর নির্ভর করে)।
- জানা সীমা: এই ইউজারের fresh login-এর পর প্রথম app-restart পর্যন্ত (যখন clean sync হয়ে key লেখা হবে)
  ফলব্যাক আগের আচরণেই থাকবে। Factory-reset-এর পর একই userId-তে re-login করলে key থেকে যেতে পারে (বিরল)।

### ম্যানুয়াল ব্যালেন্স-চেক ফলাফল (rule ৯)
`SupabaseRealtimeManager.kt`: `()` ১০৯১/১০৯১, `{}` ৩৫৬/৩৫৬, `[]` ৮৩/৮৩। `SomadhanViewModel.kt`: `()` ৩৫১৮/৩৫১৮,
`{}` ১৪২৭/১৪২৭, `[]` ১৭৭/১৭৭। `MotionToolkit.kt`: `()` ৫৪১/৫৪১, `{}` ১৩৫/১৩৫, `[]` ১১৭/১১৭। ইনপুট zip-এর
সাথে diff: মোছা লাইন শুধু `MotionToolkit.kt`-এর ঐ দুই ইচ্ছাকৃত ERROR-ব্রাঞ্চ লাইন, বাকি সব যোগ। আসল build
Android Studio-তে (rule ৯)।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
ইনপুট `somadhan-offline-action-gating-step12.zip` ও আউটপুট `somadhan-offline-action-gating-step13.zip`-এর
ফাইল-লিস্ট (`unzip -Z1 | sort` + `diff`) নিচে যাচাই করা হয়েছে — `.env`/`.gitignore` সহ।

### Android Studio-তে ম্যানুয়াল টেস্ট (ধাপ ১৪-এ অন্তর্ভুক্ত করতে)
১. টগল OFF + airplane mode-এ app খুলে কয়েকটা স্ক্রিন দেখা (cached ডেটা আসছে কিনা) → airplane বন্ধ → কিছুক্ষণ
   পর cloud-এ অন্য ডিভাইস থেকে পরিবর্তন করা ডেটা নিজে থেকে আসছে কিনা (Gap A)।
২. টগল OFF + অফলাইনে pull-to-refresh → নতুন toast, success toast না (Gap B)।
৩. টগল OFF, ইউজার আগে অন্তত একবার অনলাইনে app চালিয়েছে; এবার wifi-তে থেকে রাউটারের ইন্টারনেট বন্ধ করে app খোলা → ~১৫–২০s পর SyncErrorState-এর বদলে cached ডেটা আসছে কিনা (ফলো-আপ)। টগল ON-এ একই পরীক্ষায় আগের ERROR স্ক্রিনই আসা উচিত।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**ধাপ ১৪ — ফাইনাল QA:** দুই মোডে (টগল ON/OFF) airplane-mode চেকলিস্ট, master prompt-এর ধাপ ১৪ অনুযায়ী,
সাথে ধাপ ২-এর propagation-lag টেস্ট ও উপরের ধাপ ১৩-এর দুটো ম্যানুয়াল টেস্ট। ধাপ ১৩ সম্পূর্ণ, কোনো 🟡
কাজ বাকি নেই। Android Studio build এখনো ভেরিফাই হয়নি (rule ৯), সেটা ইউজার নিজে করবেন।

---

## ধাপ ১৪ — ফাইনাল QA — ✅ সম্পূর্ণ (এই সেশনে, ডকুমেন্ট-অনলি)

### কেন এই ধাপে কোনো কোড-পরিবর্তন নেই
Master prompt-এর ধাপ ১৪ নিজেই একটা **ম্যানুয়াল টেস্ট চেকলিস্ট** ("দুই মোডেই airplane-mode
toggle করে...") — কোনো নতুন ফিচার/ফিক্স চাওয়া হয়নি। এই sandbox-এ কোনো Android ডিভাইস/ইমুলেটর
বা Gradle build নেই (rule ৯, আগের প্রতিটা ধাপেই এই সীমাবদ্ধতা প্রযোজ্য ছিল), তাই airplane-mode
টেস্ট বাস্তবে চালানো সম্ভব না। তাই এই ধাপের আউটপুট: (ক) master prompt-এর ধাপ ১৪-এর তিনটা বুলেট
+ পুরো প্রজেক্ট জুড়ে ধাপ ২/১৩-এ ফ্ল্যাগ করা ৫টা ফলো-আপ টেস্ট (propagation-lag, dead-network
fallback, Gap A/B ম্যানুয়াল যাচাই) — সবকিছু একত্রে একটা single, ব্যবহারযোগ্য চেকলিস্ট ডকুমেন্টে
সংকলন করা; (খ) কোড ফাইনাল-সঠিকতার একটা হালকা sanity-cross-check (নিচে)।

### ফাইনাল sanity cross-check (কোড পড়ে, rule ১০)
- `MainActivity.kt` লাইন ~২০০/১১৮০-১২০৫: `isStrictOfflineBlockEnabled` collect করে
  `NoInternetOverlay` (true) বনাম `OfflineStatusBanner` (false) — ধাপ ৩-এর branch এখনো ঠিক
  জায়গায়, অক্ষত আছে বলে কনফার্ম করা হলো (grep দিয়ে)।
- `SomadhanViewModel.kt`-এ `requireOnlineOrWarn(` ব্যবহারের সব ১১২টা call-site গ্রেপ করে
  গণনা করা হলো (ধাপ ৪–১১-এর সমস্ত গার্ড একত্রে) — কোনো সংখ্যা কমে যায়নি বা duplicate/dangling
  কল নেই বলে নিশ্চিত হওয়া গেল।
- `rule ২` ট্যাগ করা কমেন্ট-ব্লকগুলো (money-critical hard-guard) গ্রেপ করে প্রতিটার আশেপাশের
  ফাংশন-নাম মিলিয়ে যাচাই করা হলো — `acceptBid`, `acceptInstantJobBid`, `solverCancelJob`
  (উভয় overload), `solverCancelAcceptedJob`, `cancelInstantJob`, `adminForceCancelInstantJob`,
  `depositMoneyViaGateway`, `requestWithdrawal`, `adminAdjustBalance`,
  `adminUpdateWithdrawalStatus`, `adminRefundEscrow`, `respondToAdditionalCharge`,
  `userConfirmExtraAmountPaid`, `confirmReleaseAndComplete`/`markProblemCompleted`,
  `adminResolveDispute`, `acceptDirectContractProposal`, `adminUpdateDirectContractStatus`,
  `adminCancelAndRefundDirectContract` — সবগুলোই এই চেকলিস্টের সেকশন ২.৫-এ তালিকাভুক্ত হয়েছে।
  কোনো নতুন গ্যাপ পাওয়া যায়নি (ধাপ ৫–১৩-এ যা যাচাই হয়েছে তার সাথে সামঞ্জস্যপূর্ণ)।

### নতুন ফাইল
`OFFLINE_ACTION_GATING_FINAL_QA_CHECKLIST.md` — প্রজেক্ট রুটে, ৫টা সেকশনে সংগঠিত (টগল ON,
টগল OFF-এর ৫টা সাব-সেকশন যেখানে সেকশন ২.৫ money-critical hard-block আইটেমগুলোর সম্পূর্ণ তালিকা,
রানটাইম টগল-সুইচ, dead-network fallback, propagation-lag) + "জানা সীমাবদ্ধতা" সেকশন (bug না,
ডিজাইন ট্রেড-অফ, যাতে টেস্ট-রেজাল্ট ভুল ব্যাখ্যা না হয়) + রেজাল্ট-লগ করার নির্দেশনা।

### zip ফাইল-লিস্ট ভেরিফিকেশন (rule ৬)
ইনপুট `somadhan-offline-action-gating-step13.zip` ও এই ধাপের আউটপুট
`somadhan-offline-action-gating-step14.zip`-এর ফাইল-লিস্ট (`unzip -Z1 | sort` + `diff`) নিচে
যাচাই করা হয়েছে — `.env`/`.gitignore` সহ, শুধু ২টা নতুন ফাইল যোগ (এই progress এন্ট্রি +
নতুন checklist ফাইল), কোনো বিদ্যমান ফাইল বদলায়নি/হারায়নি।

### পরবর্তী সেশন কোথা থেকে শুরু করবে
**Master prompt-এর ১৪টা ধাপই এখন সম্পূর্ণ (কোনো 🟡 partial নেই)।** পরের পদক্ষেপ ইউজারের —
Android Studio-তে build করে (rule ৯) `OFFLINE_ACTION_GATING_FINAL_QA_CHECKLIST.md`-এর প্রতিটা
আইটেম আসল ডিভাইসে/ইমুলেটরে টেস্ট করা। কোনো আইটেম fail করলে সেটা একটা ছোট, আলাদা ফলো-আপ
ফিক্স-ধাপ হিসেবে (rule ১১) নতুন সেশনে হ্যান্ডেল করা উচিত (পুরো মাস্টার প্রম্পট থেকে আবার শুরুর
দরকার নেই) — সেই সেশন এই progress ফাইলে "ধাপ ১৪ পরবর্তী — ফিক্স" জাতীয় একটা নতুন এন্ট্রি হিসেবে
লেখা শুরু করবে, ফেইল হওয়া নির্দিষ্ট আইটেম উল্লেখ করে।
