# Somadhan Bug-Fix Step 1 — Realtime Subscription Re-scoping: Manual Test Checklist

এই চেকলিস্ট `SOMADHAN_BUG_FIX_MASTER_PROMPT.md`-এর rule #৭ অনুযায়ী তৈরি — Step 1-এর ফিক্স
(`SomadhanViewModel.kt`-এর `logout()`/`completeLoginAfterOtp()`/`switchRoleToSolver()`/
`switchRoleToUser()`/`loginAsAdmin()`-এ `SupabaseRealtimeManager.startRealtimeListeners()`/
`stopRealtimeListeners()` আবার কল করা) বাস্তব ডিভাইসে verify করার জন্য। `BUG_INVENTORY.md`-তে
user-confirmed ৩টা উপসর্গ (bid-accept reset, release না-দেখানো, withdrawal reset) — নিচের ১, ২, ৩
নং সেকশন সরাসরি এই তিনটাই কভার করে।

**Prerequisite:** একই ফোনে/ডিভাইসে দুটো আলাদা অ্যাকাউন্ট (একটা USER, একটা SOLVER — অথবা একই
অ্যাকাউন্টের role-switch) দিয়ে বারবার logout/login করে টেস্ট করতে হবে (এটাই মূল bug scenario —
আলাদা ডিভাইস লাগবে না, বরং একই ডিভাইসে account-switch-ই আসল টেস্ট)। প্রতিটা কেসের পাশে ✅/❌ মার্ক
করে রাখো, fail হলে কোন ধাপে/কী দেখে বুঝলে সেটা এক লাইনে নোট করে রাখলে পরের সেশনে কাজে লাগবে।

---

## ১. Bid-accept balance-reset (মূল রিপোর্ট-করা উপসর্গ ১)

- [ ] Account A (USER) দিয়ে লগইন, একটা problem পোস্ট করা।
- [ ] Account B (SOLVER) দিয়ে সেই problem-এ bid দেওয়া, তারপর Account A দিয়ে সেই bid accept করা —
      balance কমে যাওয়া/escrow lock হওয়া দেখা যাচ্ছে **এই সেশনে, লগআউট না করেই**।
- [ ] এখন Account A থেকে logout করে Account B (বা অন্য কোনো account) দিয়ে login করা, তারপর আবার
      Account A দিয়ে login করা (account-switch সিমুলেট করা)।
- [ ] **চেক:** Account A-এর balance আগের (bid-accept-পরবর্তী, কমে যাওয়া) মানেই দেখাচ্ছে — পুরনো
      (বড়) মানে "reset" হয়ে যাচ্ছে না। Escrow-ও ঠিকমতো locked দেখাচ্ছে।

## ২. Escrow release-এর পর balance না-আপডেট হওয়া (উপসর্গ ২)

- [ ] উপরের bid accept করা problem-টা SOLVER (Account B) সম্পূর্ণ করে, Account A (USER) escrow
      release করে।
- [ ] Account A থেকে logout → অন্য কোনো account দিয়ে login → আবার Account A দিয়ে login (account-
      switch)।
- [ ] **চেক:** Account A-এর balance-এ release-এর প্রভাব (deduction) ঠিকমতো দেখাচ্ছে।
- [ ] Account B (SOLVER)-এ একইভাবে logout→login করে দেখা — transaction log-এ commission-বাদ amount
      "received" লেখা থাকলে balance-ও সেই commission-বাদ amount অনুযায়ী বেড়েছে (আগের মতো ০ দেখাচ্ছে
      না)।

## ৩. Withdrawal request-এর পর balance-reset (উপসর্গ ৩)

- [ ] Account B (SOLVER) দিয়ে একটা withdrawal request করা — balance কমে যাওয়া দেখা যাচ্ছে এই
      সেশনে।
- [ ] Account B থেকে logout → অন্য account দিয়ে login → আবার Account B দিয়ে login।
- [ ] **চেক:** balance এখনও কমে-যাওয়া মানেই দেখাচ্ছে (পুরনো বড় মানে ফিরে যাচ্ছে না), আর admin
      panel-এ withdrawal request-টা এখনও pending/approved হিসেবে দেখাচ্ছে (আগে যেমন ছিল, এখনও তাই)।

## ৪. Role-switch (একই user id, ভিন্ন role) — অতিরিক্ত regression-check

- [ ] Account A দিয়ে User role-এ balance/escrow দেখা, তারপর `switchRoleToSolver()` দিয়ে Solver-এ
      switch করা।
- [ ] **চেক:** switch-এর সাথে সাথেই (logout ছাড়াই) নতুন কোনো balance/escrow/notification আসলে সেটা
      লাইভ দেখা যাচ্ছে (আগে subscription পুরনো role-এই আটকে থাকত না তো, সেটা যাচাই)।
- [ ] `switchRoleToUser()` দিয়ে আবার ফেরত যাওয়া — একইভাবে লাইভ আপডেট কাজ করছে কিনা দেখা।

## ৫. Admin login/logout — regression-check (rule #৪ অক্ষত আছে কিনা)

- [ ] Admin হিসেবে login করা (`loginAsAdmin()`) — admin panel-এর users/problems/bids লিস্ট লাইভ
      আপডেট হচ্ছে (নতুন কোনো user/problem/bid অন্য device থেকে তৈরি করলে)।
- [ ] **চেক (নিয়ম #৪, বদলানো হয়নি বলে নিশ্চিত হওয়া):** admin session-এ per-user notifications
      broadcast topic চালু **হচ্ছে না** — অর্থাৎ কোনো ক্র্যাশ/অপ্রত্যাশিত notification popup admin
      panel-এ আসছে না।
- [ ] Admin logout করে normal user/solver দিয়ে login করা — সেই account-এর balance/escrow/
      notifications ঠিকমতো লাইভ দেখাচ্ছে (admin session-এর leftover subscription কোনো conflict
      করছে না)।

---

## Windows real-run কমান্ড (কোড-level regression test, static source-scan)

```
.\gradlew.bat test --tests "*RealtimeSubscriptionScopeTest*" --stacktrace
```

প্রত্যাশা: ৪টা টেস্টই pass (আগে যেটা bug-demonstrating হিসেবে ইচ্ছাকৃতভাবে fail করত, এখন ফিক্সের
পর pass করার কথা)। এই sandbox-এ Android/Gradle build-tooling-এর জন্য প্রয়োজনীয় network access
(Google/Maven repositories) না থাকায় এই কমান্ড এই সেশনে চালানো সম্ভব হয়নি — শুধু static
(regex-simulation-based) verification করা হয়েছে, `FIX_PROGRESS.md`-এর Step 1 এন্ট্রিতে বিস্তারিত।
