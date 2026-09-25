# ধাপ ১৪ — ফাইনাল QA চেকলিস্ট (Offline Action Gating)

এই চেকলিস্ট **Android Studio / আসল ডিভাইসে ম্যানুয়ালি রান করার জন্য** — এই sandbox-এ Gradle
build বা app রান সম্ভব না (rule ৯), তাই ধাপ ১–১৩-এ যা কোড-রিভিউ দিয়ে যাচাই হয়েছে, সেটার
বিরুদ্ধে আসল ডিভাইসে/ইমুলেটরে টেস্ট করার জন্য এই ডকুমেন্ট। প্রতিটা সেকশন master
prompt-এর ধাপ ১৪ + পুরো প্রজেক্ট জুড়ে যেসব ⚠️ ফলো-আপ টেস্ট ফ্ল্যাগ করা হয়েছিল, তার সবটা
একত্রে এখানে সংকলিত।

প্রথমে build ভেরিফাই করুন (rule ৯ — এই sandbox-এ কখনো করা হয়নি): `./gradlew assembleDebug`।

---

## ০. প্রস্তুতি

- [ ] দুইটা টেস্ট অ্যাকাউন্ট রাখুন: একজন admin, একজন সাধারণ ইউজার (client/solver উভয় role-ই ব্যবহার করুন)।
- [ ] Admin Panel → সেটিংস-এ গিয়ে "Strict Offline Block" টগলের বর্তমান মান নোট করে রাখুন (ডিফল্ট: ON)।
- [ ] অফলাইন সিমুলেট করতে **airplane mode** ব্যবহার করুন (শুধু wifi অফ না — নিচের সেকশন ৪-এ "wifi আছে, ইন্টারনেট নেই" আলাদা টেস্ট আছে)।

---

## ১. টগল **ON** (Strict) — পুরনো আচরণ অপরিবর্তিত আছে কিনা

- [ ] অনলাইনে অ্যাপ খুলুন, স্বাভাবিকভাবে ব্যবহার করুন — কোনো পরিবর্তন চোখে পড়া উচিত না।
- [ ] Airplane mode ON করে অ্যাপ (রিস্টার্ট সহ) খুলুন → Splash-এর পরেই পুরো-স্ক্রিন
      `NoInternetOverlay` আসা উচিত, নিচের UI ক্লিক-ইন্টারসেপ্টেড (আগের মতোই)।
- [ ] `manualOverrideConnected` লজিক (যদি UI-তে কোনো "আমি জানি, তাও এগিয়ে যান"-জাতীয় override
      বাটন থাকে) আগের মতোই কাজ করছে কিনা।
- [ ] Airplane mode বন্ধ করলে overlay সরে গিয়ে স্বাভাবিক অ্যাপ ফিরে আসছে কিনা।

## ২. টগল **OFF** — নতুন non-strict আচরণ

প্রথমে (অনলাইন অবস্থায়) Admin Panel → সেটিংস থেকে টগল **OFF** করুন, তারপর সেই ডিভাইসেই
(বা টগল OFF হওয়ার পর অন্তত একবার অনলাইনে login করা অন্য ডিভাইসে) airplane mode ON করে টেস্ট করুন।

### ২.১ গ্লোবাল ব্লক সরে যাওয়া
- [ ] Airplane mode ON করে অ্যাপ খুলুন — পুরো-স্ক্রিন ব্লকের বদলে একটা চিকন
      "ইন্টারনেট নেই — শুধু আগের ডেটা দেখা যাচ্ছে" ব্যানার দেখা উচিত, নিচের UI স্বাভাবিকভাবে
      ব্যবহারযোগ্য (ক্লিক ইন্টারসেপ্টেড না)।
- [ ] কোনো স্ক্রিন ক্র্যাশ করছে না — home, wallet, chat, problem-list, admin panel-এর প্রতিটা ট্যাব
      একবার করে ঘুরে দেখুন।

### ২.২ Group C (cached read) — cached ডেটা দেখানো, blank/error না
- [ ] Problem list, bid list, wallet transaction history, notifications, profile, categories/FAQ —
      প্রতিটাতে আগে থেকে sync হওয়া ডেটা দেখা যাচ্ছে কিনা (blank বা `SyncErrorState` না)।
- [ ] Admin Panel-এর প্রতিটা ট্যাব (users, KYC, withdrawals, categories, ইত্যাদি) একইভাবে চেক করুন।
- [ ] Pull-to-refresh করুন — success toast-এর বদলে
      "ইন্টারনেট নেই — শেষ সংগৃহীত তথ্য দেখানো হচ্ছে" toast আসা উচিত, ডেটা বদলাবে না (ধাপ ১৩ Gap B)।

### ২.৩ Group A (Outbox-covered, non-critical) — queue হওয়া উচিত, ব্লক না
- [ ] Solver: KYC জমা দিন (`submitKyc` আসলে Group B hard-guard — এটা ব্লক *হওয়াই* উচিত, confuse হবেন না)।
- [ ] Admin: একজন ইউজারকে ban/restrict/verified-badge টগল করুন, অথবা KYC approve/reject/revoke করুন
      → action সফল toast দেখানো উচিত (local queue), এবং **`OutboxPendingIndicator`** দেখা উচিত:
  - [ ] `AdminUsersView` (ব্যান/রেস্ট্রিক্ট/ভেরিফায়েড-ব্যাজ)
  - [ ] `AdminUserLookupView`
  - [ ] `AdminKycView` (approve/reject/revoke)
  - [ ] `UserWalletScreen`, Withdrawal History, `AdminWithdrawalsView` (আগে থেকেই ছিল)
- [ ] Airplane mode বন্ধ করার পর কিছুক্ষণের মধ্যে pending count শূন্যে নেমে আসছে কিনা (auto-sync)।
- [ ] Admin categories/FAQ/platform-settings CRUD একইভাবে queue হয়ে পরে sync হচ্ছে কিনা।

### ২.৪ Group B (soft-guard, non-money) — disabled + স্পষ্ট ফিডব্যাক
নিচের প্রতিটাতে চেষ্টা করলে silent fail না হয়ে toast/snackbar আসা উচিত এবং স্ক্রিন/নেভিগেশন অক্ষত থাকা উচিত:
- [ ] Login/Register/OTP verify (নতুন login সেশনে, cached টগল না থাকলে fallback ON ধরেই ব্লক থাকবে —
      এটা প্রত্যাশিত, নিচে সীমাবদ্ধতা সেকশনে দেখুন)
- [ ] সমস্যা পোস্ট করা (`createProblem`), instant job broadcast/cancel
- [ ] বিড সাবমিট করা (money move করে না এমন পাথ), problem delete, mark-seen ছাড়া অন্য solver quota action
- [ ] চ্যাট মেসেজ পাঠানো (নতুন মেসেজ, এবং ব্যর্থ মেসেজের "আবার পাঠান" রিট্রাই)
- [ ] Rating/review সাবমিট করা
- [ ] Dispute raise/withdraw করা, admin assistance রিকোয়েস্ট
- [ ] প্রোফাইল এডিট (নাম/ফোন/স্কিল/ছবি বদল)
- [ ] Admin: manual notification পাঠানো, ইউজার ক্রেডেনশিয়াল রিসেট, ক্যাটাগরি/FAQ CRUD (guard আগে আসে, তারপর Outbox — দুটোই চেক করুন)

### ২.৫ Money-critical (rule ২) — **সবসময়** হার্ড-ব্লক, toggle OFF-এও exception না
নিচের প্রতিটা action অফলাইনে (টগল OFF অবস্থায়ও) **কখনো execute হওয়া উচিত না** — শুধু স্পষ্ট
"ইন্টারনেট সংযোগ ছাড়া করা যাবে না" মেসেজ, কোনো optimistic local balance/escrow পরিবর্তন না:
- [ ] বিড accept করা (client: `acceptBid`) ও instant-job বিড accept (`acceptInstantJobBid`)
- [ ] Solver: accepted job cancel করা (escrow refund পাথ, `solverCancelJob`/`solverCancelAcceptedJob`)
- [ ] Instant job cancel (accepted solver থাকা অবস্থায়, `cancelInstantJob`) ও admin force-cancel (`adminForceCancelInstantJob`)
- [ ] ওয়ালেট ডিপোজিট (`depositMoneyViaGateway`)
- [ ] উইথড্রয়াল রিকোয়েস্ট (`requestWithdrawal`)
- [ ] Admin: ব্যালেন্স অ্যাডজাস্ট (`adminAdjustBalance`), উইথড্রয়াল স্ট্যাটাস আপডেট (`adminUpdateWithdrawalStatus`),
      এসক্রো রিফান্ড (`adminRefundEscrow`), balance reconcile/repair (live-run, dryRun=false)
- [ ] Additional charge accept/reject (`respondToAdditionalCharge`) ও extra-amount confirm (`userConfirmExtraAmountPaid`)
- [ ] Job সম্পন্ন/রিলিজ কনফার্ম করা (`confirmReleaseAndComplete` / `markProblemCompleted`)
- [ ] Admin dispute resolve (release/split/refund — `adminResolveDispute`)
- [ ] Direct contract accept (`acceptDirectContractProposal`), admin status-update (COMPLETED/CANCELLED — `adminUpdateDirectContractStatus`), admin cancel+refund (`adminCancelAndRefundDirectContract`)
- [ ] প্রতিটাতে: local balance/escrow **একচুলও না বদলানো** নিশ্চিত করুন (আগে-পরে ব্যালেন্স স্ক্রিনশট মিলিয়ে দেখুন)।

## ৩. টগল রানটাইম-সুইচ

- [ ] অ্যাপ খোলা অবস্থায় (অনলাইনে) admin অন্য জায়গা থেকে টগল বদলালে, বর্তমান ডিভাইসে পরের
      app-open/login-এ সঠিক মোডে যাচ্ছে কিনা (নিচের সেকশন ৫-এ propagation-lag বিস্তারিত)।
- [ ] Admin নিজে সেটিংস স্ক্রিন থেকে টগল বদলে সাথে সাথে সেভ হচ্ছে ও UI আপডেট হচ্ছে কিনা।

## ৪. "Wifi আছে, ইন্টারনেট নেই" (dead network) — ধাপ ১৩ ফলো-আপ

শুধু টগল OFF-এ প্রযোজ্য, এবং শুধু সেই ইউজারের জন্য যার আগে অন্তত একবার clean bulk-sync হয়েছে:

- [ ] ডিভাইস wifi-তে কানেক্টেড রেখে রাউটারের ইন্টারনেট/ব্যাকহল বন্ধ করুন (বা ফায়ারওয়াল দিয়ে ব্লক করুন)।
- [ ] অ্যাপ খুলুন — প্রথম ~১৫s স্কেলেটন/লোডিং, তারপর (connect hang করলে ~২০s-এ timeout) —
      `SyncErrorState`-এর বদলে cached Room ডেটা দেখানো উচিত (Strict মোডে **না** — সেখানে আগের
      মতোই ERROR/retry স্ক্রিন প্রত্যাশিত)।
- [ ] একই টেস্ট টগল **ON**-এ করুন — সেখানে পুরনো ERROR স্ক্রিন (retry বাটনসহ) আগের মতোই আসা উচিত,
      কোনো পরিবর্তন না (ফলব্যাক শুধু non-strict মোডে সক্রিয়)।
- [ ] সম্পূর্ণ ফ্রেশ লগইন/ইনস্টলে (কখনো clean sync হয়নি) একই পরীক্ষা করুন — এবার ফলব্যাক কাজ করবে
      না, আগের মতোই ERROR স্ক্রিন আসবে (জানা সীমা, ইচ্ছাকৃত)।

## ৫. টগল propagation-lag (ধাপ ২ ফলো-আপ)

- [ ] Device A (admin) অনলাইনে টগল **OFF** করুন।
- [ ] Device B (অন্য ইউজার, যে টগল বদলানোর *আগে* শেষ লগইন করেছিল এবং তারপর থেকে অ্যাপ খোলেনি)
      airplane mode ON করে অ্যাপ খুলুন → এখনো পুরনো cached মান (Strict full-block) দেখা উচিত —
      এটা bug না, ডিজাইনের trade-off (নিচের সীমাবদ্ধতা সেকশন দেখুন)।
- [ ] Device B airplane mode বন্ধ করে একবার অনলাইনে অ্যাপ খুলুন/লগইন করুন → নতুন মান cache হওয়া
      উচিত; তারপর আবার airplane mode ON করলে এখন non-strict ব্যানার-মোডে যাওয়া উচিত।
- [ ] সম্পূর্ণ ফ্রেশ ইনস্টল, কখনো অনলাইন হয়নি এমন ডিভাইসে সরাসরি airplane mode-এ অ্যাপ খুলুন →
      ডিফল্ট fallback `true` (Strict) প্রযোজ্য হওয়া উচিত, admin আগেই OFF করে রাখলেও।

---

## জানা সীমাবদ্ধতা (bug না, ডিজাইন ট্রেড-অফ — টেস্ট রেজাল্ট ব্যাখ্যা করার সময় মাথায় রাখুন)

1. **টগল push-based instant sync না** — প্রতিটা ডিভাইস নিজের পরের "অনলাইন মুহূর্তে" নতুন মান
   পায় (app-open/login hook), রিয়েলটাইম push না। (সেকশন ৫)
2. **ফ্রেশ ইনস্টল যেটা কখনো অনলাইন হয়নি** → ডিফল্ট `true` (Strict), admin আগেই OFF করলেও।
3. **Dead-network cached-fallback** শুধু non-strict মোডে, এবং শুধু আগে অন্তত একবার clean sync
   হওয়া ইউজারের জন্য কাজ করে (সেকশন ৪)।
4. Toggle OFF করতে admin-কে অন্তত একবার অনলাইনে থেকে সেটিংসে গিয়ে সুইচ করতে হবে — সম্পূর্ণ
   অফলাইনে admin নিজেও Strict মোডে ঢুকতে পারবেন না (rule ৫, splash exception ছাড়া অন্য কোথাও ঢোকা যায় না)।
5. `syncUserLocationToDb()`, `reconcileEscrowStates()`, `ScheduledNotificationWorker`,
   mark-read/audit-log — ইচ্ছাকৃতভাবে guard ছাড়া (ব্যাকগ্রাউন্ড/cron-জাতীয়, ইউজার-ট্যাপ অ্যাকশন না)।

---

## রেজাল্ট লগ করার জায়গা

টেস্ট শেষে ফলাফল (pass/fail + স্ক্রিনশট/নোট) `OFFLINE_ACTION_GATING_PROGRESS.md`-তে একটা নতুন
এন্ট্রি হিসেবে যোগ করুন, অথবা এই ফাইলেই প্রতিটা `[ ]`-কে `[x]`/`[FAIL: ...]` করে আপডেট করে রাখুন।
কোনো ফেইল পাওয়া গেলে সেটা নতুন একটা ছোট ফিক্স-ধাপ হিসেবে (rule ১১, ছোট স্কোপ) পরবর্তী সেশনে
হ্যান্ডেল করা উচিত — পুরো মাস্টার প্রম্পট আবার শুরু করার দরকার নেই।
