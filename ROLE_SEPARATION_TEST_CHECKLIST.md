# Balance/Reputation Role-Separation — ধাপ ৮ Manual Regression Test Checklist

এই চেকলিস্ট `BALANCE_REPUTATION_ROLE_SEPARATION_MASTER_PROMPT.md`-এর ধাপ ০ থেকে ৭ (অডিট,
reputation DAO ফিক্স, TransactionEntity role-scoping, Notification/Admin-Audit-Log
role-scoping, আর AdminUsersView/AdminUserLookupView/AdminStatsView-এর generic balance
ফিক্স) শেষ হওয়ার পর — পুরো ব্যাচের **একসাথে regression verify** করার জন্য। এই ধাপে কোনো
নতুন কোড লেখা হয়নি, শুধু এই checklist। প্রতিটা কেসের পাশে [ ] থেকে [x]/[FAIL] মার্ক করে
রাখো; fail হলে নিচের "ফলাফল রিপোর্ট করার সময়" ফরম্যাট অনুযায়ী নোট রেখো।

**Prerequisite:**
- একজন **dual-role টেস্ট ইউজার** (হাতে `hasUserRole = true` আর `hasSolverRole = true`,
  User আর Solver দুই role-এই কিছু আগে থেকে balance/reputation history আছে এমন অ্যাকাউন্ট)।
- ডিভাইস/ইমুলেটরে সেই অ্যাকাউন্ট দিয়ে লগইন করা, role switch (`switchRoleInPlace`) করার
  সুবিধা।
- Admin panel অ্যাক্সেস (আলাদা ডিভাইস/ব্রাউজার), যাতে ওই একই dual-role ইউজারকে খুঁজে
  AdminUserLookupView-এ User/Solver দুই perspective ট্যাবই দেখা যায়।
- ফ্রেশ বিল্ড (ধাপ ০–৭-এর সব ফিক্স সহ) দুই জায়গাতেই (ডিভাইস + admin panel) ইনস্টল থাকা।

প্রতিটা কেসে সংখ্যা নোট করে রাখো (আগে কত ছিল → action-এর পর কত হওয়া উচিত → আসলে কত দেখাচ্ছে),
যাতে mismatch হলে ঠিক কোথায় ভুল হলো এক নজরে বোঝা যায়।

---

## ১. Wallet — role switch না করেই cross-role isolation

- [ ] dual-role ইউজার **User-role-এ সক্রিয়** থাকা অবস্থায়, ওই একই অ্যাকাউন্টের Solver-role-এর
      জন্য একটা escrow release ঘটাও (হয় ডিভাইসে Solver-role-এ সুইচ না করেই admin panel থেকে
      trigger করে, অথবা অন্য কোনো flow দিয়ে যা সরাসরি ব্যাকগ্রাউন্ডে solver-side escrow release
      করে):
  - [ ] User-role-এর balance (`balanceUser`) **অপরিবর্তিত** থাকছে কিনা।
  - [ ] শুধু Solver-role-এর balance (`balanceSolver`) বেড়েছে কিনা (role switch করে বা admin
        panel-এ Solver perspective ট্যাব থেকে verify করো)।

## ২. Reputation — event trigger করলে শুধু সংশ্লিষ্ট role-এর স্কোর বদলায়

- [ ] একই dual-role ইউজারের জন্য একটা reputation event ট্রিগার করো (যেমন Solver হিসেবে একটা
      job সম্পন্ন করা):
  - [ ] শুধু `reputationScoreSolver` বদলেছে কিনা, `reputationScoreUser` অপরিবর্তিত থাকছে কিনা।
  - [ ] এই পরিবর্তন **তাৎক্ষণিক লোকাল UI-তে** দেখা যাচ্ছে কিনা — app restart/manual sync ছাড়াই
        (ধাপ ১-এর `applyReputationChange` role-scoped DAO ফিক্স ভেরিফাই করছে)।

## ৩. Admin — `reconcileUserBalances(dryRun=false)` role-scoped correction

- [ ] টেস্ট ডেটা বানাও যাতে dual-role ইউজারের **দুই role-এই** সামান্য mismatch থাকে (stored
      balance vs ledger-থেকে-গণনা করা balance আলাদা, দুই role-এ আলাদা আলাদা পরিমাণে)।
- [ ] Admin panel থেকে `reconcileUserBalances(dryRun=false)` রান করো:
  - [ ] User-role আর Solver-role-এর correction **আলাদা আলাদা এবং সঠিক পরিমাণে** হচ্ছে কিনা
        (একটার সংশোধন অন্যটার সংখ্যায় লিক করছে না)।
  - [ ] সংশোধনের পর দুই role-এর balance-ই ledger-এর সাথে মিলছে কিনা।

## ৪. Role switch রিগ্রেশন (আগের ফিক্স ভাঙেনি)

- [ ] `switchRoleInPlace` দিয়ে role switch করার পর সব জায়গায় সঠিক role-এর ডেটা দেখাচ্ছে
      কিনা আবার নিশ্চিত হও:
  - [ ] Wallet balance
  - [ ] Reputation score
  - [ ] Ban/restrict status
  - (এগুলো আগের ধাপগুলোতে (৭খ, ৭গ ইত্যাদি) আলাদাভাবে ফিক্স হয়েছিল — এখানে regression হিসেবে
        একসাথে আবার চেক করা)

## ৫. Transaction History — role-scoped ফিল্টার (ধাপ ৫)

- [ ] একই dual-role ইউজার: User-role-এ থাকা অবস্থায় কিছু deposit/bid-deduction করো।
- [ ] Solver-role-এ switch করে Transaction History স্ক্রিন খোলো:
  - [ ] ধাপ ৫-এ তোমার নিজের দেওয়া business-rule উত্তর অনুযায়ী শুধু Solver-role-এর প্রাসঙ্গিক
        history-ই দেখাচ্ছে কিনা।
  - [ ] User-role-এর অপ্রাসঙ্গিক এন্ট্রি এখানে মিশে যাচ্ছে না কিনা।
- [ ] একই যাচাই উল্টো দিক থেকেও করো (Solver-role-এ deposit/transaction করে User-role-এ
      switch করে history দেখা)।

## ৬. Notification inbox — role-scoped leak বন্ধ (ধাপ ৬, অংশ ক)

- [ ] একই dual-role ইউজার: **Solver-role-এ থাকা অবস্থায়** admin থেকে কিছু approve/reject
      করাও (যা একটা role-specific notification generate করে, যেমন KYC/ban/balance-adjust)।
- [ ] তারপর **User-role-এ switch করে** notification inbox খোলো:
  - [ ] সেই Solver-side notification-টা এখন **আর দেখাচ্ছে না** তা নিশ্চিত হও।
  - [ ] role-নিরপেক্ষ (role ফাঁকা/general announcement) নোটিফিকেশনগুলো এখনও দুই role-এই
        ঠিকমতো দেখা যাচ্ছে কিনা (leak ফিক্স করতে গিয়ে সাধারণ নোটিফিকেশন ভেঙে যায়নি তো)।
- [ ] Unread notification badge-count-ও এই role-scoping মেনে সঠিক সংখ্যা দেখাচ্ছে কিনা
      (দুই role-এ আলাদা count হওয়া উচিত কিনা, ধাপ ৬-এ ঠিক করা design অনুযায়ী)।

## ৭. Admin Audit Log — role traceability (ধাপ ৬, অংশ খ, যদি করা হয়ে থাকে)

- [ ] dual-role ইউজারের ওপর Solver perspective থেকে একটা ban/restrict/balance-adjust অ্যাকশন
      নাও, তারপর User perspective থেকেও একটা অ্যাকশন নাও।
- [ ] Admin Audit Log-এ দুটো এন্ট্রি খুঁজে দেখো — প্রতিটাতে সঠিক `role` (যেটাতে অ্যাকশন হয়েছিল)
      লগ হয়েছে কিনা, একটার role অন্যটায় ভুলভাবে বসে যায়নি তো।

## ৮. Admin display স্ক্রিন — perspective-scoped সংখ্যা (ধাপ ৭)

- [ ] AdminUserLookupView-এ dual-role ইউজারকে সার্চ করে User ↔ Solver perspective ট্যাব
      বদলে বদলে দেখো — ব্যালেন্স, রেপুটেশন স্কোর, ban/restrict badge — সবগুলো একসাথে সেই
      একই perspective-এর সংখ্যা দেখাচ্ছে কিনা (একটা ট্যাবে থেকে অন্য role-এর সংখ্যা "leak"
      করছে না তো — এটাই ধাপ ৭গ-এর মূল বাগ ছিল)।
- [ ] AdminUsersView-এর balance-adjust dialog আর AdminStatsView-এর CSV এক্সপোর্ট/"টপ সলভার"
      র‍্যাংকিং — dual-role ইউজারের জন্য প্রতিটাই সঠিক role-scoped সংখ্যা দেখাচ্ছে কিনা
      (ধাপ ৭-এ যা ফিক্স হয়েছিল তার regression-check)।

---

## ফলাফল রিপোর্ট করার সময়

প্রতিটা fail হওয়া কেসের জন্য জানিও:
- কোন সেকশন/বুলেট (উপরের নাম্বারিং অনুযায়ী, যেমন "৫ নং সেকশনের ২য় বুলেট")
- কী ফলাফল আশা করেছিলে vs. আসলে কী দেখালো (আগে কত/কী ছিল, action কী হলো, তারপর কী হওয়া উচিত,
  আসলে কী দেখালো)
- কোন role-এ ছিলে (User/Solver), আর কোন স্ক্রিন/ডিভাইসে (ইউজার ডিভাইস, admin panel)
- app restart/sync/role-switch করার আগে না পরে সমস্যাটা ধরা পড়েছে

একটা সেকশন pass করলেই পরের সেকশনে যেও — পুরো ফাইল একবারে শেষ করার দরকার নেই। কোনো সেকশন
fail করলে সেটা নিয়ে আলাদা ছোট ফিক্স-রাউন্ড শুরু হবে, তারপর শুধু সেই সেকশনটাই আবার রি-টেস্ট
করলেই হবে।
