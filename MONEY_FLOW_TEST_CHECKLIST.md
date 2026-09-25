# Money/Wallet Flow — Manual Regression Test Checklist

এই চেকলিস্টটা balance flicker-guard বাগ (এবং একই ক্লাসের অন্যান্য money-sync ইস্যু) ফিক্স করার
পর, একটা একটা করে ধাপ ফিক্স → টেস্ট → পরেরটা — এই পদ্ধতিতে ব্যবহারের জন্য। প্রতিটা সেকশন আলাদাভাবে
রান করা যায়; পুরো ফাইল একসাথে শেষ করতে হবে না। প্রতিটা কেসের পাশে [ ] থেকে [x]/[FAIL] মার্ক করে
রাখো, fail হলে "ফলাফল রিপোর্ট করার সময়" সেকশনের ফরম্যাট অনুযায়ী নোট রেখো — এটাই পরে Claude-কে
জানানোর সময় কাজে লাগবে।

**Prerequisite:** দুইটা আলাদা ডিভাইস/ইমুলেটর —
- **ডিভাইস A** = problem poster (USER role)
- **ডিভাইস B** = solver (SOLVER role, অন্য অ্যাকাউন্ট)
- **Admin panel** অ্যাক্সেস (আলাদা ব্রাউজার/ডিভাইসে)

টেস্টের আগে দুই ডিভাইসেই ফ্রেশ বিল্ড (fix সহ) ইনস্টল থাকা ভালো। প্রতিটা কেসে টাকার অঙ্ক
নোট করে রাখো (আগে কত ছিল → action-এর পর কত হওয়া উচিত → আসলে কত দেখাচ্ছে), যাতে mismatch হলে
ঠিক কোথায় সংখ্যা ভুল হলো সেটা এক নজরে বোঝা যায়।

---

## ০. Baseline (প্রতিটা সেকশন শুরুর আগে)

- [ ] ডিভাইস A (user) আর ডিভাইস B (solver) — দুইজনেরই বর্তমান balance স্ক্রিনশট/নোট করে রাখো
      (wallet screen-এ দেখানো সংখ্যা)।
- [ ] Admin panel-এও একই দুই ইউজারের balance দেখে নোট করো — এই মুহূর্তে ডিভাইসের সাথে মিলছে কিনা।

## ১. Post → Broadcast → Bid → Accept (escrow lock)

- [ ] ডিভাইস A থেকে নতুন problem পোস্ট করা — ডিভাইস B-তে (broadcast/realtime দিয়ে, refresh
      ছাড়াই) সেটা লিস্টে ভেসে ওঠে কিনা।
- [ ] ডিভাইস B থেকে বিড দেওয়া — ডিভাইস A-তে (realtime) সেই বিড দেখা যায় কিনা, refresh ছাড়াই।
- [ ] ডিভাইস A থেকে বিড accept করা — **ঠিক তখনই**, refresh না করে:
  - [ ] ডিভাইস A-তে balance কমে যাওয়া (আগের balance − bid amount) সাথে সাথে দেখাচ্ছে কিনা।
  - [ ] ডিভাইস A-তে escrow/লকড amount সঠিক দেখাচ্ছে কিনা।
  - [ ] ডিভাইস B-তে notification/chat message আসছে কিনা (বিড accepted)।
- [ ] app বন্ধ করে আবার খুলে (cold restart) দুই ডিভাইসেই balance/escrow আগের মতোই ঠিক আছে
      কিনা (flicker-guard fail করলে এখানে পুরনো/ভুল সংখ্যা ফিরে আসতে পারে)।

## ২. Additional charge (অতিরিক্ত বিল) — request → accept/reject

- [ ] ডিভাইস B (solver) থেকে additional charge request পাঠানো:
  - [ ] ডিভাইস A-তে (realtime) notification/chat আসছে কিনা।
  - [ ] **শুধু request পাঠানোর পরেই** — accept করার আগেই — ডিভাইস A-এর balance অপরিবর্তিত
        থাকছে কিনা (কোনোভাবে বেড়ে/কমে যাচ্ছে না তো — এটাই আগের রিপোর্ট করা বাগ)।
- [ ] ডিভাইস A থেকে charge **accept** করা:
  - [ ] ডিভাইস A-তে balance ঠিক charge amount দিয়ে কমেছে কিনা, সাথে সাথে।
  - [ ] escrow-এর extra_amount ঠিক charge amount দিয়ে বেড়েছে কিনা।
  - [ ] ডিভাইস B-তে transaction history আর escrow-এর projected payout — দুই জায়গাতেই
        নতুন amount যোগ হয়েছে দেখাচ্ছে কিনা।
- [ ] আরেকটা নতুন problem/charge দিয়ে **reject** flow টেস্ট করা — reject করলে কোনো balance
      পরিবর্তন হচ্ছে না (ঠিকই আছে), শুধু status/notification ঠিকমতো আপডেট হচ্ছে কিনা।
- [ ] Double-tap/দ্রুত দুইবার accept চাপ দিয়ে দেখা — balance দুইবার কাটছে না তো (idempotency)।

## ৩. Refund (solver cancel / user cancel / dispute)

- [ ] সলভার job cancel করলে (বা dispute-এ refund হলে):
  - [ ] ডিভাইস A-তে (poster) balance ঠিক refund amount দিয়ে বেড়েছে কিনা, refresh ছাড়াই।
  - [ ] escrow status "REFUNDED" দেখাচ্ছে কিনা, আর locked amount ০ হয়ে গেছে কিনা।
  - [ ] transaction history-তে REFUND এন্ট্রি ঠিক amount সহ দেখাচ্ছে কিনা।
  - [ ] refund-এর পর ডিভাইস A cold-restart করে balance আগের ভুল মানে "ফিরে যাচ্ছে না" সেটা
        নিশ্চিত করা (এটাই মূল রিপোর্ট করা bug)।
- [ ] Base amount + accepted extra charge — দুটোই মিলিয়ে refund হচ্ছে কিনা (শুধু base না,
      escrow-এ থাকা পুরো amount)।

## ৪. Re-select অন্য solver (escrow reuse edge case)

- [ ] একজন solver-এর বিড accept করে, তারপর সেই solver cancel/withdraw করলো, এরপর ডিভাইস A
      **অন্য একজন solver**-কে select করলো একই problem-এ:
  - [ ] প্রথম solver-এর escrow ঠিকমতো refund/release হয়েছে (দ্বিতীয় solver-এর escrow-এর
        সাথে গুলিয়ে যায়নি)।
  - [ ] দ্বিতীয় solver-এর জন্য নতুন/reused escrow সঠিক amount নিয়ে তৈরি হয়েছে।
  - [ ] দুই solver-এর কারো balance/transaction history-ই একে অপরের সাথে মিশে যায়নি (আলাদা
        আলাদা ঠিকমতো attribute হয়েছে)।

## ৫. Job complete → Release (normal, user নিজে release করলে)

- [ ] ডিভাইস A থেকে fund release করা:
  - [ ] ডিভাইস B-তে (solver) **সাথে সাথে, refresh ছাড়াই** balance বেড়েছে কিনা (এটাই আগের
        রিপোর্ট করা মূল বাগ — transaction দেখাতো, balance বাড়তো না)।
  - [ ] ডিভাইস B-তে transaction history-তে payment entry এসেছে কিনা, আর amount balance-এর
        বৃদ্ধির সাথে মিলছে কিনা।
  - [ ] ডিভাইস B cold-restart করেও balance ঠিক থাকছে কিনা (পুরনো মানে ফিরে যাচ্ছে না)।
  - [ ] ডিভাইস B-তে অন্য কোনো সাধারণ action (যেমন notification read করা, লোকেশন আপডেট)
        করার **ঠিক পরপরই** release হলে — তারপরও balance ঠিকমতো আপডেট হচ্ছে কিনা (flicker-guard
        race condition বিশেষভাবে এই timing-এই ধরা পড়ে)।

## ৬. Admin release (admin panel থেকে জোর করে escrow release)

- [ ] Admin panel থেকে কোনো escrow admin release করা:
  - [ ] সলভারের ডিভাইসে balance আপডেট হচ্ছে কিনা, refresh ছাড়াই।
  - [ ] Admin panel নিজেই ঐ ইউজারের balance আপডেটেড দেখাচ্ছে কিনা।
  - [ ] transaction history/escrow status তিন জায়গাতেই (ডিভাইস, admin panel, DB-তে
        সরাসরি চেক করলে) একই সংখ্যা দেখাচ্ছে কিনা।

## ৭. Withdrawal

- [ ] সলভার withdrawal request করলে balance সাথে সাথে কমে যাচ্ছে (hold-এ চলে যাচ্ছে) কিনা।
- [ ] Admin panel থেকে withdrawal process/approve করলে সলভারের ডিভাইসে status আপডেট
      আসছে কিনা, আর reject হলে balance ঠিকমতো ফেরত আসছে কিনা।

## ৮. Deposit (gateway)

- [ ] ডিভাইস A থেকে গেটওয়ে দিয়ে ডিপোজিট করলে balance ঠিক deposit amount দিয়ে বেড়েছে কিনা,
      refresh ছাড়াই।

## ৯. Admin adjust balance / reconcile

- [ ] Admin panel থেকে কোনো ইউজারের balance ম্যানুয়ালি add/deduct করলে সেই ইউজারের ডিভাইসে
      সাথে সাথে প্রতিফলিত হচ্ছে কিনা।
- [ ] Admin-এর "reconcile user balances" (ledger vs stored balance) টুল চালিয়ে দেখা —
      উপরের সব টেস্টের পর কোনো mismatch দেখাচ্ছে কিনা (দেখালে সেটাই নির্দেশ করে balance-guard
      fix সম্পূর্ণ কাজ করেনি বা অন্য কোথাও নতুন mismatch তৈরি হয়েছে)।

## ১০. Cross-check — balance বনাম ledger

প্রতিটা সেকশন শেষে (বা সব শেষে একবারে):

- [ ] ডিভাইস A আর B-এর wallet screen-এ দেখানো balance, admin panel-এর balance, আর
      transaction history-র যোগফল (manually যোগ করে) — তিনটাই একে অপরের সাথে মিলছে কিনা।

## ১১. DB migration (v45→v46) সেফটি — ধাপ ২ ফিক্স

এটা সরাসরি money-flow না, কিন্তু balance/transaction ডেটা টিকে থাকা নিয়ে — তাই এখানেই রাখা হলো।
আগে `MIGRATION_45_46` কোডে ছিলই না, ফলে version 45-এ থাকা যেকোনো পুরনো ইনস্টল আপগ্রেড করলে
`fallbackToDestructiveMigration` চলে পুরো local DB (সব balance/transaction/escrow history সহ)
মুছে যেত। এখন একটা no-op bridge migration যোগ করা হয়েছে।

- [ ] যদি হাতে version 45 বা তার আগের কোনো পুরনো APK/build থাকে: সেটা দিয়ে আগে একটা ডিভাইসে
      ডেটা তৈরি করো (কিছু balance/transaction থাকুক), তারপর নতুন (fix-করা) build দিয়ে আপগ্রেড
      ইনস্টল করো — আগের সব ডেটা (balance, transaction history, escrow) অক্ষত আছে কিনা যাচাই
      করো (app খুলেই লগইন স্ক্রিনে না গিয়ে সরাসরি আগের সেশন/ডেটা দেখাচ্ছে কিনা)।
- [ ] পুরনো build হাতে না থাকলে: শুধু নিশ্চিত করো যে ফ্রেশ ইনস্টলে (version 49 থেকে সরাসরি শুরু)
      অ্যাপ স্বাভাবিকভাবে ওপেন/সাইনআপ হচ্ছে, কোনো crash/schema error আসছে না (এটা নিশ্চিত করে যে
      নতুন migration যোগ করাটা fresh-install path ভাঙেনি)।

## ১২. Admin — Direct contract cancel & refund (cloud dual-write ফিক্স, ধাপ ৩)

- [ ] ডিভাইস A (user, direct-contract owner) আর ডিভাইস B (solver) দুইজনকে জড়িয়ে একটা direct
      contract active অবস্থায় রেখে, admin panel থেকে "cancel & refund" অ্যাকশন নেওয়া:
  - [ ] ডিভাইস A-তে refresh ছাড়াই (realtime দিয়ে) problem status "CANCELLED" দেখাচ্ছে কিনা।
  - [ ] ডিভাইস A আর B — দুইজনের ডিভাইসেই notification আসছে কিনা (আগে এটা শুধু admin-এর নিজের
        local DB-তে থেকে যেত, অন্য ডিভাইসে কখনো পৌঁছাত না)।
  - [ ] Problem chat-এ admin-এর "বাতিল ও রিফান্ড" নোটিস মেসেজ ডিভাইস A/B-তে দেখা যাচ্ছে কিনা।
  - [ ] ডিভাইস A-এর balance ঠিক refund amount দিয়ে বেড়েছে কিনা (এটা আগে থেকেই কাজ করার কথা,
        শুধু confirm করা)।
- [ ] Admin panel/DB সরাসরি চেক করে দেখা যে problem row-এর `status`/`direct_contract_status`
      cloud-এ সত্যিই আপডেট হয়েছে (যদি RPC আসলে DB-তে না থাকে, dual-write silently fail করে
      শুধু log-এ warning আসবে — app crash করবে না, কিন্তু cloud-এ sync হবে না। সেক্ষেত্রে
      Supabase dashboard-এ `admin_update_direct_contract_status` ফাংশনটা আছে কিনা যাচাই করে
      জানিও)।

## ১৩. Free-quota (কমিশন-ফ্রি) sync — ধাপ ৪ ফিক্স

- [ ] উচ্চ reputation-এর একজন solver (থ্রেশহোল্ডের উপরে, ফ্রি-কোটা এখনও শেষ হয়নি এমন) দিয়ে একটা
      বিড accept করানো (**owner-এর ডিভাইস থেকে accept**, অর্থাৎ `acceptBid()` flow):
  - [ ] সেই job কমিশন-ফ্রি (০%) হিসেবে accept হচ্ছে কিনা (আগের behavior অক্ষত আছে কিনা)।
  - [ ] Solver-এর নিজের ডিভাইসে (বা admin panel-এ) গিয়ে দেখা — `free_jobs_used_this_month`
        বেড়েছে কিনা, **refresh ছাড়াই** বা অন্তত app restart-এর পর (আগে এটা owner-এর local
        cache-এই আটকে থাকতো, solver-এর নিজের ডিভাইস/cloud-এ কখনো পৌঁছাত না)।
  - [ ] একই solver-কে দিয়ে ধারাবাহিকভাবে কোটা-লিমিট (ডিফল্ট ১০টা) পর্যন্ত জব accept করিয়ে
        দেখা — লিমিট পার হওয়ার পর পরের জব থেকে স্বাভাবিক কমিশন কাটছে কিনা।
- [ ] Direct contract flow-তে (solver নিজে accept করে, `acceptDirectContractProposal()`)ও
      একইভাবে টেস্ট করা — এটা আগেও কাজ করতো, শুধু নিশ্চিত করো নতুন RPC-তে যাওয়ার পরও এখনও
      ঠিকভাবে কাজ করছে (regression)।
- [ ] **Deploy-prerequisite:** `supabase/migrations/step_money_flow_fix4_sync_solver_free_job_quota.sql`
      ফাইলটা Supabase project-এ apply করা হয়েছে কিনা নিশ্চিত করা — না করা থাকলে উপরের সব কেসেই
      RPC কল ব্যর্থ হবে (log warning আসবে, app crash করবে না, কিন্তু sync হবে না)।

---

## ফলাফল রিপোর্ট করার সময়

প্রতিটা fail হওয়া কেসের জন্য জানিও:
- কোন সেকশন/বুলেট (উপরের নাম্বারিং অনুযায়ী, যেমন "৫ নং সেকশনের ২য় বুলেট")
- কী amount আশা করেছিলে vs. আসলে কী দেখালো (আগে কত ছিল, action কী হলো, তারপর কত হওয়া উচিত,
  আসলে কত দেখালো)
- কোন ডিভাইসে (A/user, B/solver, admin panel)
- refresh/cold-restart করার আগে না পরে সমস্যাটা ধরা পড়েছে

একটা সেকশন pass করলেই পরের সেকশনে যেও — পুরো ফাইল একবারে শেষ করার দরকার নেই। কোনো সেকশন fail
করলে সেটা নিয়ে আলাদা ছোট ফিক্স-ধাপ শুরু হবে, তারপর শুধু সেই সেকশনটাই আবার রি-টেস্ট করলেই হবে।
