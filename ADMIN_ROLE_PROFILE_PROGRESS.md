# ADMIN_ROLE_PROFILE_PROGRESS.md

এই ফাইলটা `ADMIN_ROLE_PROFILE_MASTER_PROMPT.md` অনুযায়ী — প্রতিটা সেশন/সাব-স্টেপ শেষে এখানে log
যোগ হবে, যাতে সেশন হারিয়ে গেলেও পরবর্তী কাজ ঠিক কোথা থেকে শুরু করতে হবে বোঝা যায়। **এই ফাইলটাই
সবচেয়ে সাম্প্রতিক সত্য** — মাস্টার প্রম্পটের সেশন-লিস্টের সাথে গরমিল হলে এই ফাইল মানতে হবে।

> **কনফার্মেশন-ক্যাডেন্স (ব্যবহারকারীর সর্বশেষ নির্দেশ, ২০২৬-০৯-২৪):** প্রতিটা সেশন শেষে থামবে, ব্যবহারকারীর কনফার্মেশন
> ছাড়া পরের সেশনে যাবে না (সেশন ৭.০-৭.৯-এও প্রতি সাব-স্টেপে)। আগের "১-৬ টানা" নিয়ম বাতিল — নিচের ✅ নোট দেখুন।
>
> **📦 জিপ-ক্যাডেন্স (ব্যবহারকারীর নির্দেশ, ২০২৬-০৯-২৫):** সেশন ৭.০-৭.৯-এর **প্রতিটা সাব-স্টেপ শেষে**
> (শুধু পুরো সেশন শেষে না) — এই ফাইলে (ও প্রয়োজনে মাস্টার প্রম্পটে) এন্ট্রি/আপডেট লেখার পরপরই একটা
> **updated zip** (dotfile/hidden ফাইলসহ — `.env`, `.gitignore`, `.github/` ইত্যাদি, শুধু ভিজিবল ফাইল
> না) দিতে হবে, তারপর ব্যবহারকারীর কনফার্মেশনের জন্য থামতে হবে। অর্থাৎ প্রতিটা সাব-স্টেপের শেষে ক্রম:
> (১) সাব-স্টেপের কাজ শেষ → (২) এই PROGRESS ফাইলে এন্ট্রি → (৩) updated zip তৈরি ও দেওয়া → (৪) থামা,
> পরের সাব-স্টেপ কনফার্মেশন ছাড়া শুরু হবে না। এটা ৭.০-৭.৯-এর প্রতিটা সাব-স্টেপের (এবং ৭.২.১-এর মতো
> নেস্টেড সাব-সাব-স্টেপেরও) জন্য প্রযোজ্য।

---

## সেশন ০ — ডায়াগনসিস + মাস্টার প্রম্পট লেখা ✅ সম্পন্ন (২০২৬-০৯-২৪)

- **আউটপুট:** `ADMIN_ROLE_PROFILE_MASTER_PROMPT.md`, `admin-role-management.html`,
  `admin-profile.html` (দুটো রেফারেন্স HTML মকআপ — Claude চ্যাটে বানানো, কোড কপি করার জন্য না,
  শুধু UI/UX ও ফিচার স্পেক হিসেবে)।
- **কী করা হয়েছে:** এই zip গ্রেপ করে বর্তমান অবস্থা ভেরিফাই করা হয়েছে —
  - এখন পুরো অ্যাপে একটাই admin credential (`AdminCredentials.kt`), কোনো মাল্টি-এডমিন/রোল সিস্টেম
    নেই, কোনো স্ক্রিনেই পারমিশন-চেক নেই।
  - `admin_audit_logs` টেবিল + `log_admin_action(...)` RPC ইতিমধ্যে আছে কিন্তু admin-identity কলাম
    নেই (anonymous log)।
  - ব্র্যান্ড কালার (`SomadhanOrange = 0xFFFF6500`) মকআপের সাথে হুবহু মিলে গেছে।
  - `AdminPanelScreen.kt`-এর ড্রয়ার গ্রুপ/ইনডেক্স ম্যাপ (০-২৪ ব্যবহৃত) গ্রেপ করে ২৫টা
    `AdminXxxView.kt` ফাইলের সাথে মিলিয়ে তালিকা করা হয়েছে।
  - `ImageStorageUtil.kt`/`KycUploadManager.kt` প্রোফাইল-ছবি আপলোডের জন্য reusable বলে চিহ্নিত।
  - দুই মকআপ থেকে ফিচার লিস্ট বের করে Supabase স্কিমা (`admin_roles`, `admin_accounts`,
    `admin_audit_logs` alter) + RPC ডিজাইন + ক্লায়েন্ট স্কোপ + ৭টা সেশনের প্ল্যান (সেশন ৭
    সাব-স্টেপ ৭.০-৭.৯ সহ, ২৫টা স্ক্রিনের গ্রুপ-অনুযায়ী রিট্রফিট অর্ডার) লেখা হয়েছে।
- **কোনো `.kt`/`.sql` ফাইল এডিট হয়নি — শুধু ডকুমেন্টেশন।**
- **পরবর্তী ধাপ শুরুর আগে যা দরকার:** মাস্টার প্রম্পটের **ধাপ ৫-এর ৮টা প্রশ্নের** উত্তর ব্যবহারকারীর
  কাছ থেকে কনফার্ম করা (মাল্টি-এডমিন লগইন এখনই শুরু হবে কিনা, লাইভ প্রিভিউ প্রোডাকশনে থাকবে কিনা,
  অনলাইন-স্ট্যাটাস রিয়েল-টাইম হবে কিনা, ইত্যাদি) — উত্তর না আসা পর্যন্ত সেশন ১ শুরু হবে না।

## ধাপ ৫-এর ৮টা প্রশ্নের উত্তর ✅ কনফার্মড (২০২৬-০৯-২৪, ব্যবহারকারী)

1. মাল্টি-এডমিন লগইন — ব্যবহারকারী Claude-এর বিবেচনায় ছেড়েছেন ("যেটা perfect হবে"): **সেশন ১-এ স্কিমা/RPC (বিদ্যমান লগইন অক্ষত রেখে), সেশন ২-এ লগইন প্রতিস্থাপন।**
2. লাইভ প্রিভিউ সিমুলেটর — **শুধু সুপার অ্যাডমিনের জন্য।**
3. অনলাইন স্ট্যাটাস — **রিয়েল-টাইম (heartbeat) এবং last login — দুটোই।**
4. অ্যাক্টিভিটি লগে কারণ — **বিদ্যমান `details` ফিল্ডে** (আলাদা কলাম না)।
5. flagged — **active-এর পাশে আলাদা boolean।**
6. প্রোফাইল ভিজিবিলিটি — Claude-এর পরামর্শ অনুমোদিত: **প্রতিজন নিজেরটা দেখবে/এডিট করবে; সুপার অ্যাডমিন সবার দেখতে/এডিট করতে পারবে।**
7. পাসওয়ার্ড পরিবর্তন — Claude-এর পরামর্শ অনুমোদিত: **current-password re-verify প্যাটার্ন, per-account।**
8. প্রোফাইল নেভিগেশন — **টপ বারের ডান পাশে premium-look অবতার/আইকন, ক্লিকে প্রোফাইল স্ক্রিন (সবার জন্য সবসময়-দৃশ্যমান)।**

> ✅ **কনফার্মেশন-ক্যাডেন্স মীমাংসিত (২০২৬-০৯-২৪, ব্যবহারকারীর স্পষ্ট নির্দেশ):** প্রতিটা সেশন আলাদাভাবে —
> **প্রতিটা সেশন শেষে থামবে**, নতুন zip + এই ফাইলে এন্ট্রি দেবে, ব্যবহারকারীর কনফার্মেশন ছাড়া পরের সেশনে যাবে না।
> (START_HERE/মাস্টার প্রম্পটের "সেশন ১-৬ টানা" নিয়ম এতে ওভাররাইড হলো; সেশন ৭.x-এ আগের মতোই প্রতি সাব-স্টেপে থামা।)

## সেশন ১ — DB migration — ✅ সম্পন্ন + লাইভে apply হয়েছে (২০২৬-০৯-২৪)

- **ফাইল:** `supabase/migrations/zz_20260924150000_admin_role_system_session1_schema_rpc.sql` (লাইভে একই কনটেন্ট
  `admin_role_system_session1_schema_rpc` নামে, project mghvvpndkxnscwryfkib; সেশন-শুরুতে `list_migrations` চেক করা হয়েছিল —
  কোনো untracked admin migration ছিল না)।
- **যা তৈরি হয়েছে:** `admin_roles` (একটাই `is_super` রো, seed: `super`), `admin_accounts`, `admin_sessions`;
  `admin_audit_logs`-এ `admin_id`/`admin_name`/`admin_role_name` কলাম; DB-trigger গার্ড (সুপার অ্যাকাউন্ট
  deactivate/flag/delete/রোল-বদল এবং সুপার রোল delete/পারমিশন-বদল অসম্ভব — postgres/service_role থেকেও); RLS
  (লেখা শুধু RPC দিয়ে; পড়া: সুপার সব, বাকিরা শুধু নিজের); সিড: বিদ্যমান একমাত্র ADMIN user (Support Manager,
  01963533981) → `super` অ্যাকাউন্ট।
- **RPC (সব SECURITY DEFINER, শুধু authenticated):** `admin_me`, `admin_session_start`, `admin_heartbeat`
  (ফেরত মানে সর্বশেষ active/flagged/রোল/পারমিশন; নিষ্ক্রিয় হলে `ACCOUNT_INACTIVE`), `admin_session_end`,
  `admin_profile_update` (নিজের; সুপার হলে যেকারো), `admin_roles_list`, `admin_role_upsert`, `admin_role_delete`,
  `admin_accounts_list`, `admin_account_set_role`, `admin_account_set_active`, `admin_account_set_flagged`;
  সহায়ক: `is_super_admin`, `admin_can_act(action_key)` (সার্ভার-সাইড, সেশন ৭-এ ঐচ্ছিক hardening-এর জন্য), `_admin_*` internal।
- **⚠️ মাস্টার প্ল্যান থেকে ৩টা ইচ্ছাকৃত বিচ্যুতি (কারণ migration-এর হেডারেও লেখা):**
  1. `admin_accounts`-এ `password_hash` নেই, বদলে `auth_user_id` — প্রতিটা এডমিনের নিজস্ব Supabase Auth user;
     কারণ বিদ্যমান সব admin RPC/RLS `is_admin(auth.uid())`-এ দাঁড়িয়ে, আর এতে সার্ভার নিজেই জানে কে কল করছে।
  2. `log_admin_action`-এর signature **অপরিবর্তিত** — admin identity `auth.uid()` থেকে সার্ভার-সাইডে নেওয়া হয়;
     তাই বিদ্যমান ১৫+ কল-সাইট আপডেট লাগবে না (মাস্টার প্রম্পটের "সেশন ৫: কল-সাইট ধাপে ধাপে" ও "৭.x-এ p_admin_id/
     p_admin_name যোগ" — **এই কাজ আর দরকার নেই**), spoof-ও করা যায় না।
  3. `is_admin()` এখন নিষ্ক্রিয় এডমিনের জন্য false — ডিঅ্যাক্টিভেশন সব RPC/RLS-এ সাথে সাথে কার্যকর। (param নাম
     লাইভে `uid`, CI stub-এ `p_user_id` — migration বিদ্যমান নামটাই পড়ে নেয়।)
- **বিদ্যমান আচরণ অক্ষত:** `admin_credentials` টেবিল/RPC/`AdminCredentials.kt` লগইন গেট ছোঁয়া হয়নি; কোনো `.kt` ফাইল
  এডিট হয়নি। পুরনো ১৫১টা audit লগ backfill করা হয়নি (admin_name ফাঁকা → UI "লিগ্যাসি" দেখাবে)।
- **ভেরিফিকেশন (লাইভ DB-তে, rolled-back ট্রানজ্যাকশনে — কোনো residue নেই, পরে যাচাই করা):** ৩০+ চেক পাস — সিড, is_admin/
  is_super, session/heartbeat/online→offline, লগে এডমিনের নাম+রোল, রোল CRUD ও ভ্যালিডেশন, ফ্ল্যাগ/আনফ্ল্যাগ
  ও `admin_can_act`, ডিঅ্যাক্টিভেট → `is_admin` false + heartbeat `ACCOUNT_INACTIVE`, নন-সুপারের সুপার-অনলি
  RPC ব্লক, প্রোফাইল সেলফ/অন্যের এডিট, RLS (নন-সুপার ১টা রো দেখে, সুপার সব), সরাসরি insert ব্লক, anon execute বন্ধ,
  সুপার-অপরিবর্তনীয়তা (RPC + সরাসরি UPDATE/DELETE), সুপার রোল অ্যাসাইন নিষিদ্ধ, ব্যবহৃত রোল delete নিষিদ্ধ।
  `scripts/scan_duplicate_overloads.sh` সবুজ (exit 0)।
- **যা যাচাই হয়নি / বাকি (সৎ তালিকা):** (ক) CI-তে এই migration apply হবে কিনা রান করে দেখা হয়নি (কোড পড়ে defensive
  বানানো: dynamic seed, is_admin param-নাম, users.email নির্ভরতা নেই) — পরের CI রানে প্রথম নিশ্চিতকরণ; (খ) এই RPC-গুলোর
  pgTAP টেস্ট লেখা হয়নি (লাইভ dry-run টেস্টই একমাত্র ভেরিফিকেশন); (গ) `admin_account_create` (auth.users সহ) সেশন ৪-এ —
  real Supabase sign-in দিয়ে যাচাই করে; (ঘ) `admin_accounts`/`admin_sessions` realtime publication-এ নেই — রিয়েল-টাইম
  অনলাইন-ডট কীভাবে (broadcast_changes বনাম client polling/presence) সেটা সেশন ৪/৬-এ সিদ্ধান্ত; (ঙ) পুরনো
  `AdminAuditLogView`-এর RLS এখনো "যেকোনো admin" — সুপার-অনলি করা সেশন ৫-এ।
- **যা টেস্ট করা উচিত (অ্যাপে):** কিছু না — অ্যাপ-সাইড কোনো পরিবর্তন নেই; শুধু চেক করুন বিদ্যমান admin লগইন ও যেকোনো
  admin অ্যাকশন (যেমন ইউজার ব্যান) আগের মতোই কাজ করছে এবং নতুন লগে `admin_name = Support Manager` আসছে।
## সেশন ২ — রিয়েল মাল্টি-এডমিন লগইন ফ্লো + `AdminSession` — ✅ কোড সম্পন্ন, ⚠️ অ্যাপ বিল্ড/ডিভাইস-টেস্ট বাকি (২০২৬-০৯-২৪)

- **DB (লাইভে apply + যাচাই হয়েছে):** `supabase/migrations/zz_20260924160000_admin_login_check_session2.sql` —
  `admin_login_check(p_phone) → boolean` (anon+authenticated; ফোন `01…`/`+8801…`/`8801…`/`1…` সব ফরম্যাট মেলে) +
  ইন্টারনেল `_admin_norm_phone` (শুধু service_role)। লাইভে যাচাই: সুপারের ফোন সব ফরম্যাটে true, অন্য নম্বর/খালি/null false,
  anon-এর `_admin_norm_phone`-এ execute নেই, `admin_me`-তে anon execute নেই। (apply করার সময় একটা substr-অফসেট বাগ ধরা পড়ে
  ঠিক করা হয়েছে — লাইভ ও রিপো ফাইল এখন এক। `scan_duplicate_overloads.sh` সবুজ।) লাইভে দুটো migration-নাম:
  `admin_login_check_session2` + `admin_login_check_session2_fix_e164_substr` (রিপোতে একটাই ফাইল, চূড়ান্ত কনটেন্টসহ)।
- **নতুন ফাইল:** `data/security/AdminSession.kt` — `AdminAccountInfo` (সার্ভারের `_admin_account_view` JSON থেকে), `AdminSessionState`,
  `object AdminSession` (`state: StateFlow`, `current`, `set/update/clear`)। সেশন ৩-৭ এখান থেকেই পড়বে।
- **`SupabaseSyncManager`:** `adminLoginCheck`, `adminSessionStart`, `adminHeartbeat`, `adminSessionEnd`।
- **`SomadhanViewModel`:** `loginAsAdmin(phone, pass, onReady, onError)` নতুন — (১) real Supabase Auth sign-in = পাসওয়ার্ড যাচাই, ব্যর্থ = লগইন ব্যর্থ;
  (২) `admin_session_start` (নন-এডমিন/নিষ্ক্রিয় এখানে আটকায়, sign-out করে ক্লিন); (৩) `AdminSession.set`; (৪) `_currentUser` (id `ADMIN_SYSTEM` অপরিবর্তিত,
  নাম/ফোন অ্যাকাউন্ট থেকে); (৫) ৩০সে heartbeat → রোল/পারমিশন/flag লাইভ আপডেট, `ACCOUNT_INACTIVE`/`NOT_AN_ADMIN` হলে `adminForcedLogoutReason`।
  `logout()` এডমিন-সেশন বন্ধ করে (৩সে টাইমআউট) signOut-এর আগে। `isAdminPhone()` (লগইন স্ক্রিনের জন্য)।
  `adminUpdateCredentials` পুনর্লিখিত: বর্তমান এডমিনের নিজের Supabase Auth পাসওয়ার্ড বদল (re-sign-in দিয়ে বর্তমান পাসওয়ার্ড যাচাই); ফোন বদল প্রত্যাখ্যান।
- **`LoginScreen`:** এডমিন-ফোন শনাক্ত `isAdminPhone` দিয়ে; `AdminCredentials` ইম্পোর্ট/ব্যবহার সরানো; `onError`-এ ত্রুটি-বার্তা।
- **`AdminPanelScreen`:** `adminForcedLogoutReason` observer → টোস্ট + লগআউট। **`AdminSettingsView`:** ফোন লকড/নিষ্ক্রিয়, বর্তমান ফোন `AdminSession` থেকে,
  "ডিফল্ট ক্রেডেনশিয়াল" ব্যানার বন্ধ।
- **⚠️ আচরণ-পরিবর্তন (ইচ্ছাকৃত):** (ক) **অফলাইনে এডমিন লগইন আর সম্ভব না** — আগের লোকাল DataStore/`admin_credentials` fallback ছিল মাল্টি-এডমিন গেট
  এড়ানোর পিছনের দরজা; (খ) "সীমিত ভিউ"/degraded পথ আর কখনো চালু হয় না (বার-এর কোড বিদ্যমান, শুধু dead); (গ) `AdminCredentials.kt` ও `admin_credentials`
  টেবিল/RPC ফাইল/DB-তে **অক্ষত কিন্তু লগইন আর পড়ে না** (dead) — মুছে ফেলা সেশন ৭-এর পরের ক্লিনআপে; (ঘ) `admin_login_check` anon-কে বলে দেয় কোনো ফোন এডমিনের কিনা
  (আগে `admin_credentials_get_phone` সরাসরি ফোনটাই দিত — তাই খারাপ হয়নি)।
- **❗ লকআউট-ঝুঁকি ও রিকভারি:** সুপারের পাসওয়ার্ড এখন শুধু Supabase Auth-এ যাচাই হয়। যদি কখনো `admin_credentials` হ্যাশ আর Auth পাসওয়ার্ড আলাদা হয়ে থাকে (আগে এমন
  ঘটনা ছিল — "সীমিত ভিউ"), পুরনো পাসওয়ার্ডে আর ঢোকা যাবে না। রিকভারি (Supabase SQL Editor, নতুন পাসওয়ার্ড বসিয়ে):
  `update auth.users set encrypted_password = extensions.crypt('নতুন-পাসওয়ার্ড', extensions.gen_salt('bf')) where id = (select auth_user_id from public.admin_accounts where phone = '01963533981');`
- **যা যাচাই হয়নি (সৎ তালিকা):** (ক) **Kotlin কম্পাইল/বিল্ড করা হয়নি** (এই এনভায়রনমেন্টে Gradle/SDK নেই) — নতুন কোড বিদ্যমান প্যাটার্ন দেখে লেখা, Android Studio-তে
  বিল্ড করে প্রথম ধরা পড়বে; (খ) আসল ডিভাইসে sign-in → `admin_session_start` → heartbeat পুরো ফ্লো রান হয়নি (DB-পাশের RPC সেশন ১-এ যাচাই ছিল); (গ) ত্রুটি-বার্তা শ্রেণিবিন্যাস
  (ভুল পাসওয়ার্ড বনাম নেটওয়ার্ক) supabase-kt-র exception মেসেজ-টেক্সট ধরে — বাস্তব মেসেজ ভিন্ন হলে ভুল বার্তা আসতে পারে (লগইন নিজে ঠিক থাকবে); (ঘ) নিষ্ক্রিয় করার পর
  জোর-লগআউট ~৩০সে-এ (heartbeat) — তবে সার্ভার-সাইডে `is_admin` সাথে সাথেই false।
- **যা টেস্ট করা উচিত (অ্যাপে):** ১) বিল্ড হচ্ছে কিনা; ২) সুপারের ফোন+পাসওয়ার্ড দিয়ে লগইন → প্যানেল খোলে, ডেটা আসে, "⚠️ সীমিত ভিউ" আসে না; ৩) ভুল পাসওয়ার্ডে "ভুল অ্যাডমিন
  পাসওয়ার্ড"; ৪) সাধারণ ইউজার/সলভার লগইন আগের মতোই (OTP সহ); ৫) লগআউট → আবার লগইন কাজ করে; ৬) সেটিংসে পাসওয়ার্ড বদল → লগআউট → নতুন পাসওয়ার্ডে লগইন;
  ৭) লগইনের পর নতুন অডিট লগে `ADMIN_LOGIN` (admin_name = Support Manager) আসে; ৮) অন্যান্য admin অ্যাকশন (যেমন ইউজার ব্যান) আগের মতো।
## সেশন ৩ (অংশ ১/২) — পারমিশন ক্যাটালগ অডিট + ক্যাটালগ ফাইল — ✅ সম্পন্ন, UI বাকি (২০২৬-০৯-২৪)

> ব্যবহারকারীর নির্দেশে এই সেশনটা ইচ্ছাকৃতভাবে **অর্ধেক** করা হলো — ক্যাটালগ অডিট + ডেটা ফাইল এই সেশনে,
> `AdminRoleManagementView` UI (অংশ ২/২) পরের Claude সেশনে। নিচে হ্যান্ড-অফ নোট।

### নতুন প্রশ্নের উত্তর (২০২৬-০৯-২৪, ব্যবহারকারী) — কনফার্মড, অংশ ২-এ প্রয়োগ হবে
1. **পারমিশন ক্যাটালগ:** এখনই ২৫টা স্ক্রিন অডিট করে পূর্ণ ক্যাটালগ (মকআপের সরলীকৃত ৮-১০টা অ্যাকশনের
   ক্যাটালগ না) — **এই সেশনে সম্পন্ন**, নিচে দেখুন।
2. **রোল-ট্যাব লোডিং:** রোল-কার্ড per-item pulse + pull-to-refresh (`SyncAwareContent`/
   `SOMADHAN_LOADING_PATTERN_MASTER_PROMPT.md`-এর কনভেনশন মেনে) — অংশ ২-এ প্রয়োগ হবে।
3. **অ্যাকশন টিক → 'দেখুন' অটো-টিক:** পারমিশন-ট্রি এডিটরে যেকোনো অ্যাকশন টিক দিলে সেই আইটেমের 'view'
   অটোমেটিক টিক হয়ে যাবে (আনটিক করার সময় উল্টো না — view untick করলে বাকি সব অ্যাকশনও untick হবে, কারণ
   view ছাড়া অন্য অ্যাকশন অর্থহীন) — অংশ ২-এ প্রয়োগ হবে।

### কী তৈরি হয়েছে
- **`app/src/main/java/com/example/data/security/AdminPermissionCatalog.kt`** (নতুন ফাইল) — ২৫টা
  `AdminXxxView.kt` গ্রেপ করে (viewModel.adminXxx() কল, onXxx callback প্যারামিটার, actionType স্ট্রিং)
  ভেরিফাই করা সম্পূর্ণ পারমিশন ক্যাটালগ। প্রতিটা গ্রুপ/আইটেম `AdminPanelScreen.kt`-এর `drawerGroups`-এর
  সাথে টাইটেল/tabIndex মিলিয়ে ভেরিফাই করা (কপি-পেস্ট অনুমান না)। প্রতিটা অ্যাকশনের পাশে কমেন্টে আসল
  ফাংশন-নাম লেখা আছে। শেষে নতুন `admin_mgmt` গ্রুপ (role_mgmt/admin_accounts/activity_log,
  `superOnly=true`, `tabIndex=null` কারণ স্ক্রিন এখনো নেই)।
- কোনো `.kt` ফাইল এডিট হয়নি (শুধু নতুন ফাইল), কোনো UI/গেটিং ওয়্যার করা হয়নি, কোনো migration লাগেনি
  (`admin_roles.permissions` jsonb কলাম সেশন ১ থেকেই আছে, এই কী-গুলোই তাতে স্টোর হবে)।

### অডিট ফাইন্ডিংস — মকআপ বনাম বাস্তব কোড (গুরুত্বপূর্ণ, ব্যবহারকারীর কনফার্মেশন দরকার অংশ ২ শুরুর আগে)
1. **ক্রস-ডোমেইন অ্যাকশন (আর্কিটেকচার সিদ্ধান্ত, সবচেয়ে গুরুত্বপূর্ণ):** `AdminUserLookupView.kt`
   (ইনডেক্স ১৮, "ইউজার/সলভার সার্চ") থেকেই সরাসরি ব্যান/কেওয়াইসি/সমস্যা/উইথড্রয়াল অ্যাকশন কল হয়
   (`adminSetBanned`, `adminApproveKyc`, `adminDeleteProblem`, `adminUpdateWithdrawalStatus` ইত্যাদি)।
   ক্যাটালগে এগুলো `user_search`-এর নিজস্ব কী না দিয়ে তাদের **আসল ডোমেইনের কী** দিয়ে রাখা হয়েছে
   (যেমন `users:users:ban`, `users:kyc:approve`) — যুক্তি: সেশন ৭-এ `canAct()` লেখার সময় একই লজিক্যাল
   অ্যাকশনের জন্য দুটো ভিন্ন পারমিশন-গেট (একটা Users ট্যাবে, একটা Lookup ট্যাবে) তৈরি হওয়া উচিত না —
   একজন এডমিনের "KYC অনুমোদন" পারমিশন থাকলে সেটা Lookup থেকে করলেও, KYC ট্যাব থেকে করলেও একই কী চেক
   হবে। **✅ কনফার্মড (২০২৬-০৯-২৪, ব্যবহারকারী): অপশন ১ (নিজ নিজ ডোমেইনের কী দিয়ে চেক)।** কারণ: অপশন ২
   (Lookup-এর নিজস্ব আলাদা কী, যেমন `users:user_search:manage`) একটা নিরাপত্তা-ফাঁক তৈরি করত — কেউ
   নিজের Users/KYC ট্যাবে ব্যান/অ্যাপ্রুভ করতে না পারলেও Lookup ঘুরে গিয়ে করতে পারত। তাই
   `AdminPermissionCatalog.kt`-এ `user_search` আইটেমে শুধু `view` অ্যাকশন রাখা হয়েছে (ইতিমধ্যে তাই
   আছে, কোনো পরিবর্তন লাগেনি) — সেশন ৭.০-এর `AdminSession.canAct()` আর অংশ ২-এর রোল-এডিটর UI দুটোই
   এই নিয়ম মেনে বানাতে হবে: Lookup স্ক্রিনের ব্যান/কেওয়াইসি/উইথড্রয়াল বাটন `users:users:ban`/
   `users:kyc:approve`/`finance:withdrawals:approve` ইত্যাদি তাদের **আসল ডোমেইনের কী** দিয়ে গেট হবে,
   Lookup-এর নিজস্ব কোনো আলাদা কী দিয়ে না।
2. **মকআপে ছিল, বাস্তব কোডে পাওয়া যায়নি (ক্যাটালগ থেকে বাদ):** `finance:withdrawals:delete`,
   `finance:extra_charges:mark_settled` (মাইগ্রেশন `step29_mark_additional_charge_settled.sql` আছে
   কিন্তু `AdminAdditionalChargesView.kt`-এ কোনো UI বাটন/কল-সাইট নেই — সম্ভবত cron/RPC-only, বা
   ফিচার অসম্পূর্ণ রয়ে গেছে; ব্যবহারকারীকে জিজ্ঞেস করা দরকার এটা ইচ্ছাকৃত কিনা), `moderation:reviews:hide`
   (শুধু delete আছে), `system:refund_debug:run_fix` (স্ক্রিনটা আসলে pure read-only diagnostic + কপি,
   কোনো DB-মিউটেশন নেই)।
3. **বাস্তব কোডে আছে, মকআপে ছিল না (নতুন যোগ হয়েছে):** users:users-এ `verified_badge`/
   `reset_password`; users:kyc-এ `reset_pending`/`edit`; work:problems-এ `update_status`/
   `update_budget`/`reassign`; work:instant_jobs-এ `rebroadcast`/`convert_normal`;
   work:direct_contracts-এ `cancel_refund`/`escalate` (+ chat_monitoring-এর সাথে ওভারল্যাপিং
   send/delete message); finance:escrow-এ `release`/`refund`/`repair_refunds`;
   finance:gateway_payments-এ `update_status` (মকআপ ধরে নিয়েছিল read-only, বাস্তবে না);
   config:categories-এ `toggle_active`/`physical_work_toggle`/`virtual_work_toggle`;
   config:settings-এ `run_quota_reset`/`cleanup_commission`/`factory_reset`; disputes-এ
   `manual_flag`/`issue_warning`; contact:notifications-এ `cancel_scheduled`/`delete`।
4. **অত্যন্ত ঝুঁকিপূর্ণ অ্যাকশন, গ্র্যানুলার পারমিশন না দিয়ে সরাসরি "শুধু সুপার" করা উচিত কিনা:**
   `config:settings:factory_reset` (`adminFactoryResetAllData` — পুরো অ্যাপের ডেটা মুছে ফেলে),
   `config:settings:cleanup_commission`, এবং মাস্টার প্রম্পটে আগে থেকেই চিহ্নিত
   `system:explorer:*`/`system:refund_debug:view` (raw DB access/debug info) — মাস্টার প্রম্পটের
   সাব-স্টেপ ৭.৬-এর প্রস্তাবিত প্যাটার্নে এগুলো `isSuper` হার্ড-চেক হতে পারে, রোল-ভিত্তিক গ্র্যানুলার
   পারমিশনের বদলে। **এখনো গ্র্যানুলার হিসেবেই ক্যাটালগে রাখা হয়েছে, চূড়ান্ত সিদ্ধান্ত বাকি** (অংশ ২ শুরুর
   আগে বা তখনই কনফার্ম করা যেতে পারে)।
5. `adminUpdateCredentials` (নিজের পাসওয়ার্ড বদল, সেশন ২-এ করা) ইচ্ছাকৃতভাবে এই ক্যাটালগে নেই — এটা
   রোল-পারমিশনের বিষয় না, প্রতিটা এডমিন সবসময় নিজেরটা বদলাতে পারবে (প্রোফাইল স্কোপ, সেশন ৬)।

### পরবর্তী Claude সেশনের জন্য হ্যান্ড-অফ — সেশন ৩ (অংশ ২/২): `AdminRoleManagementView` UI
- **শুরুর আগে অবশ্যই পড়বে:** এই ফাইলের উপরের অডিট-ফাইন্ডিংস সেকশন (বিশেষত ফাইন্ডিং ১ ও ৪ —
  ব্যবহারকারীর সাথে কনফার্ম করে নেওয়া, তারপর এগোনো), `AdminPermissionCatalog.kt`-এর হেডার কমেন্ট,
  `admin-role-management.html` মকআপ (**শুধু ইন্টারঅ্যাকশন প্যাটার্নের জন্য** — এর ভেতরের `GROUPS`
  ডেটা ব্যবহার করা যাবে না, `AdminPermissionCatalog.kt`-ই আসল ডেটা), `SOMADHAN_LOADING_PATTERN_
  MASTER_PROMPT.md` + `SHIMMER_REGRESSION_BATCH31_MASTER_PROMPT.md` (per-item pulse/pull-to-refresh
  কনভেনশনের জন্য)।
- **বানাতে হবে:** `AdminRoleManagementView.kt` (নতুন, সুপার-অনলি, প্রস্তাবিত ট্যাব ইনডেক্স ২৫) —
  - রোল কার্ড গ্রিড (per-item pulse লোডিং + pull-to-refresh, প্রশ্ন ২ কনফার্মড) —
    `admin_roles_list` RPC (সেশন ১-এ তৈরি) থেকে ডেটা।
  - রোল এডিটর — `AdminPermissionCatalog.GROUPS` দিয়ে ট্রি রেন্ডার (গ্রুপ → আইটেম → অ্যাকশন
    চেকবক্স, tri-state গ্রুপ/আইটেম চেকবক্স মকআপের মতো), **অ্যাকশন টিক দিলে 'view' অটো-টিক, 'view'
    untick করলে বাকি সব untick** (প্রশ্ন ৩ কনফার্মড) — `admin_role_upsert` RPC দিয়ে সেভ।
  - রোল ডিলিট — `admin_role_delete` RPC; ইউজার-অ্যাসাইনড রোল ডিলিট ব্লক (মকআপের মতো, ইতিমধ্যে
    DB-trigger আছে সেশন ১ থেকে — শুধু UI-তে বার্তা দেখানো)।
  - লাইভ প্রিভিউ সিমুলেটর — **সুপার অ্যাডমিনের জন্যই থাকবে** (প্রশ্ন ২, আগেই কনফার্মড) —
    `AdminPermissionCatalog`-এর কী দিয়ে কোন মেনু/অ্যাকশন দেখাবে সিমুলেট করা।
  - `AdminPanelScreen.kt`-এ নতুন "অ্যাডমিন ব্যবস্থাপনা" ড্রয়ার গ্রুপ (সুপার-অনলি কন্ডিশনাল রেন্ডার,
    `AdminSession.current?.isSuper` দিয়ে) যোগ করা, ইনডেক্স ২৫ (রোল ম্যানেজমেন্ট) — এডমিন অ্যাকাউন্ট
    (ইনডেক্স ২৬) সেশন ৪-এ যোগ হবে একই গ্রুপে।
- **স্কোপ না — এই সেশনে (৩) না, পরের সেশনে:** এডমিন অ্যাকাউন্ট CRUD/flag/active টগল UI (সেশন ৪),
  অ্যাক্টিভিটি লগ ট্যাব (সেশন ৫), কোনো বিদ্যমান ২৫টা স্ক্রিনে গেটিং বসানো (সেশন ৭)।
## সেশন ৩ (অংশ ২.১/২.২) — `AdminRoleManagementView` UI — ✅ অর্ধেক সম্পন্ন, বাকি অর্ধেক হ্যান্ড-অফ (২০২৬-০৯-২৪)

> ব্যবহারকারীর নির্দেশে অংশ ২/২-টাও **আরও ভাগ করা হলো** — এই সেশনে অর্ধেক (২.১), বাকি অর্ধেক (২.২)
> পরের Claude সেশনে। কোনো `.kt` ফাইল কম্পাইল/রান করে যাচাই করা হয়নি (এই এনভায়রনমেন্টে Gradle/SDK
> নেই, সেশন ১-২-এর মতোই সীমাবদ্ধতা) — কোডটা বিদ্যমান `AdminCategoriesView.kt`/
> `AdminFaqManagementView.kt`/`AdminAuditLogView.kt`-এর কনভেনশন (import স্টাইল, RPC-call প্যাটার্ন,
> loading pattern) হুবহু গ্রেপ করে মিলিয়ে লেখা হয়েছে।

### এই সেশনে (২.১) যা সম্পন্ন হয়েছে
1. **`SupabaseSyncManager.kt`** — `adminRolesList()`, `adminRoleUpsert(id, name, permissions)`,
   `adminRoleDelete(id)` নতুন RPC বাইন্ডিং যোগ হয়েছে (সেশন ১-এ RPC নিজেই DB-তে তৈরি হয়েছিল, কিন্তু
   ক্লায়েন্ট-সাইড কল-সাইট এতদিন ছিল না)। প্যাটার্ন সেশন ২-এর `adminLoginCheck`/`adminSessionStart`
   থেকে হুবহু অনুসরণ করা — try/catch + `Result<T>`, `client.postgrest.rpc(...)`।
2. **`AdminPermissionCatalog.kt`** — নতুন `AdminRoleInfo` ডেটা ক্লাস + `fromJson(JsonObject)` পার্সার
   (`admin_roles_list`-এর জবাবের সাথে ১:১ মেলে — id/name/is_super/permissions/account_count/
   created_at/updated_at)।
3. **নতুন ফাইল `app/src/main/java/com/example/ui/screens/AdminRoleManagementView.kt`** —
   - রোল কার্ড গ্রিড: `rememberFieldChangePulse(sessionKey = "admin_role_mgmt_sync")` দিয়ে
     per-item pulse (AdminCategoriesView-এর "admin_categories_sync" প্যাটার্ন অনুসরণ করে) —
     **এই sessionKey স্ট্রিংটা অংশ ২.২-এ `AdminPanelScreen.kt`-এর ইনডেক্স ২৫-এর
     `SyncAwareContent`-এও হুবহু একই থাকতে হবে।**
   - রোল এডিটর: `AdminPermissionCatalog.GROUPS` দিয়ে গ্রুপ→আইটেম→অ্যাকশন tri-state ট্রি
     (`TriStateCheckbox`), গ্রুপ/আইটেম "সব নির্বাচন" টগল, **নতুন-প্রশ্ন ৩ কনফার্মড লজিক প্রয়োগ
     হয়েছে** (অ্যাকশন টিক → 'view' অটো-টিক; 'view' আনটিক → বাকি সব আনটিক) — এটা মকআপের
     `toggleAction`-এর চেয়ে আলাদা আচরণ, মকআপে এই অটো-লজিক ছিল না, নতুন করে লেখা হয়েছে।
   - `admin_mgmt` (superOnly) গ্রুপ এডিটরে 🔒-লকড দেখায় (মকআপের `locked-group` স্টাইল)।
   - সুপার রোলের কার্ডে এডিট/ডিলিট বাটন নেই (UI-প্রতিফলন, আসল গার্ড DB-তে সেশন ১ থেকেই আছে)।
   - ডিলিট কনফার্মেশন ডায়ালগে `account_count > 0` হলে সতর্কবার্তা + কনফার্ম বাটন disabled
     (মাস্টার প্রম্পটের "ইউজার-অ্যাসাইনড রোল ডিলিট ব্লক, UI-তে বার্তা দেখানো" অনুযায়ী)।
   - `onSaveRole`/`onDeleteRole` callback **fire-and-forget** (`(...) -> Unit`) — বিদ্যমান সব
     `AdminXxxView.kt`-এর (`onDeleteFaq`, `onEditCategory` ইত্যাদি) callback-শৈলী অনুসরণ করে,
     নতুন প্যাটার্ন আবিষ্কার করা হয়নি। এরর-হ্যান্ডলিং caller-এর (ViewModel) দায়িত্ব হিসেবে রাখা
     হয়েছে, ভিউ নিজে কোনো toast দেখায় না।
   - `roleErrorMessage(raw: String): String` হেল্পার — সার্ভারের raise-করা কোড
     (ROLE_NAME_REQUIRED/ROLE_NAME_TAKEN/ROLE_NOT_FOUND/SUPER_ROLE_IMMUTABLE/ROLE_IN_USE/
     PERMISSIONS_MUST_BE_*/SUPER_ADMIN_REQUIRED/AUTH_REQUIRED) → বাংলা বার্তা। এখনো **কোথাও কল হয়
     না** — অংশ ২.২-এ ViewModel-এর catch ব্লক থেকে কল হওয়ার জন্য বানানো।
   - কোনো real ডেটা এখনো দেখেনি — এই কম্পোজেবল কোথাও কল হয় না (নিচে দেখুন ২.২)।

### ⏳ পরবর্তী Claude সেশনের জন্য হ্যান্ড-অফ — সেশন ৩ (অংশ ২.২, বাকি অর্ধেক)
- **শুরুর আগে অবশ্যই পড়বে:** `AdminRoleManagementView.kt`-এর হেডার কমেন্ট (এই ফাইলের ভেতরেই
  বিস্তারিত স্কোপ-নোট আছে), `AdminSession.kt` (`AdminSession.current?.account?.isSuper` দিয়ে
  ড্রয়ার-গার্ড), `AdminPanelScreen.kt`-এর বিদ্যমান `drawerGroups`/`when(selectedTabIndex)`
  স্ট্রাকচার (লাইন ~৩৬০-৪৬০ গ্রুপ ডেফিনিশন, ~৯০০-১৫৬০ `SyncAwareContent` dispatch)।
- **যা বানাতে হবে:**
  1. `SomadhanViewModel`-এ নতুন state/ফাংশন: `adminRoles: StateFlow<List<AdminRoleInfo>>` (বা
     সমতুল্য), `loadAdminRoles()` (→ `SupabaseSyncManager.adminRolesList()` কল করে state আপডেট),
     `adminSaveRole(id, name, permissions)` (→ `adminRoleUpsert` কল, ব্যর্থ হলে
     `roleErrorMessage(e.message ?: "")` দিয়ে টোস্ট — বিদ্যমান admin-toast মেকানিজম যেটাই হোক,
     `adminDeleteFaq`-এর প্যাটার্ন অনুসরণ করা), `adminDeleteRole(role)` (→ `adminRoleDelete`)।
  2. `AdminPanelScreen.kt`-এ নতুন "অ্যাডমিন ব্যবস্থাপনা" ড্রয়ার গ্রুপ — **কন্ডিশনালি রেন্ডার**
     (`AdminSession.current?.account?.isSuper == true` হলেই দেখাবে, বিদ্যমান কোনো গ্রুপেই এখনো
     এই conditional-rendering প্যাটার্ন নেই বলে এটাই প্রথম উদাহরণ, সাবধানে বসাতে হবে), ইনডেক্স ২৫
     "রোল ম্যানেজমেন্ট" (এডমিন অ্যাকাউন্ট আইটেম ইনডেক্স ২৬ সেশন ৪-এ একই গ্রুপে যোগ হবে, এখন বসালে
     ফাঁকা/অসম্পূর্ণ স্ক্রিনে নিয়ে যাবে তাই এখনই না)।
  3. `when(selectedTabIndex)`-এ `25 -> SyncAwareContent(sessionKey = "admin_role_mgmt_sync", ...) { AdminRoleManagementView(roles = ..., onSaveRole = viewModel::adminSaveRole, onDeleteRole = viewModel::adminDeleteRole, viewModel = viewModel, isManualRefreshing = ...) }`
     — sessionKey **হুবহু** `"admin_role_mgmt_sync"` (স্ক্রিন ফাইলের ভেতরের কী-এর সাথে মিলতে হবে)।
  4. লাইভ প্রিভিউ সিমুলেটর (মকআপের ডানপাশের প্যানেল, প্রশ্ন ২ কনফার্মড থাকবে — সুপার অ্যাডমিনের
     জন্য) — এখনো লেখা হয়নি, এই সাব-স্টেপেই বা এর পরে আলাদা ছোট সেশনে করা যেতে পারে।
- **স্কোপ না (এই সেশনেও না, সেশন ৩-এরই বাইরে):** এডমিন অ্যাকাউন্ট CRUD (সেশন ৪), অ্যাক্টিভিটি লগ
  ট্যাব (সেশন ৫), ২৫টা বিদ্যমান স্ক্রিনে গেটিং (সেশন ৭)।
- **টেস্ট করা উচিত (২.২ শেষে, অ্যাপে):** ১) কম্পাইল হয় কিনা (এই দুই সেশনেই Gradle ছিল না, তাই এটাই
  প্রথম আসল কম্পাইল-চেক); ২) সুপার অ্যাডমিন লগইন করলে ড্রয়ারে "অ্যাডমিন ব্যবস্থাপনা" দেখা যায়, নন-সুপার
  করলে যায় না; ৩) রোল তৈরি/এডিট/ডিলিট + tri-state/auto-view-tick UI-তে ঠিকমতো কাজ করছে; ৪) সুপার
  রোলে এডিট/ডিলিট বাটন নেই; ৫) অ্যাসাইনড-অ্যাকাউন্ট থাকা রোল ডিলিট করতে গেলে বাটন disabled + সার্ভার
  থেকে `ROLE_IN_USE` এলে বাংলা বার্তা দেখায়; ৬) pull-to-refresh এ card pulse করে re-entry-তে না।

## সেশন ৩ (অংশ ২.২) — ViewModel + ড্রয়ার ওয়্যারিং + লাইভ প্রিভিউ — ✅ কোড সম্পন্ন (২০২৬-০৯-২৪) ⇒ **সেশন ৩ সম্পূর্ণ**, ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি

> এই এন্ট্রি দিয়ে সেশন ৩-এর হ্যান্ড-অফ ("অংশ ২.২, বাকি অর্ধেক") এর ৪টা কাজই শেষ। কোনো `.kt` কম্পাইল/রান
> করা হয়নি (এই এনভায়রনমেন্টে Gradle/SDK/kotlinc নেই — সেশন ১-২-২.১-এর মতোই)। যাচাই = বিদ্যমান কনভেনশন
> গ্রেপ করে মিলিয়ে লেখা + ৫টা পরিবর্তিত/নতুন ফাইলে ব্র্যাকেট-ব্যালেন্স (সব ঠিক)। প্রথম আসল কম্পাইল-চেক Android Studio-তে।

### কী করা হয়েছে
1. **`SomadhanViewModel.kt`** — `adminRoles: StateFlow<List<AdminRoleInfo>>`, `adminRolesSyncPhase` (নিজস্ব LOADING/LOADED/ERROR —
   `admin_roles_list` bulk-pull-এর অংশ না, তাই `initialSyncPhase`-এ ভর করা হয়নি), `loadAdminRoles()` (সুপার-অনলি, চলমান
   জব থাকলে দ্বিতীয়বার শুরু করে না), `adminSaveRole(id,name,perms,onDone)`, `adminDeleteRole(role)`। সার্ভার-এরর
   `roleErrorMessage(...)` দিয়ে বাংলা টোস্ট। `adminSaveRole` ক্যাটালগে-নেই এমন কী (বিশেষত `admin_mgmt:*`) সার্ভারে পাঠায়
   না। `refreshAdminTab(25)` রোল-তালিকা রিফ্রেশ করে (ব্যর্থ হলে "রিফ্রেশ করতে সমস্যা" টোস্ট, মিথ্যা "সম্পন্ন" না)।
   `logout()` রোল-স্টেট ক্লিয়ার করে।
2. **`AdminPanelScreen.kt`** — `AdminSession.state` কালেক্ট → `isSuperAdmin`; `drawerGroups` এখন `baseDrawerGroups` +
   (সুপার হলে) "অ্যাডমিন ব্যবস্থাপনা" গ্রুপ (ইনডেক্স ২৫ "রোল ম্যানেজমেন্ট")। `when(selectedTabIndex)`-এ ২৫ →
   `SyncAwareContent(sessionKey = "admin_role_mgmt_sync")` → `AdminRoleManagementView`, নন-সুপার fallback বার্তাসহ।
   ট্যাবে ঢুকলে `LaunchedEffect` একবার `loadAdminRoles()` (LOADED অবস্থায় চুপচাপ, skeleton flash নেই)।
3. **`AdminRolePreviewPanel.kt`** (নতুন) — লাইভ প্রিভিউ (সুপার-অনলি, প্রশ্ন ২): ফোন-স্টাইল মেনু-প্রিভিউ (ক্যাটালগ থেকে
   ডাইনামিক, মেনু ট্যাপ করলে অ্যাকশন-চিপ) + "লাইভ স্ক্রিন সিমুলেশন — ইউজারগণ" (`users:users`-এর আসল অ্যাকশন থেকে
   ডাইনামিক বাটন)। `isFlagged` প্যারামিটার আছে (view অক্ষত, বাকি ব্লকড) — এখনো কেউ `true` দেয় না, সেশন ৪-এ
   অ্যাকাউন্ট-ভিত্তিক প্রিভিউ এখানেই ওয়্যার হবে।
4. **`AdminRoleManagementView.kt`** — রোল-কার্ডে "লাইভ প্রিভিউ দেখুন" বাটন (সুপারসহ সব রোলে) → আলাদা প্রিভিউ-পেইন;
   এডিটরে collapsible লাইভ প্রিভিউ (টিক দিলে সাথে সাথে বদলায়)।
5. **`AdminPermissionCatalog.kt`** — `ROLE_MGMT_TAB_INDEX = 25` কনস্ট্যান্ট (ViewModel + Panel + ক্যাটালগ একই সোর্স);
   `role_mgmt`-এর `tabIndex` `null` থেকে ২৫।
6. **টেস্ট (নতুন, না-চালানো):** `AdminRolePreviewLogicTest.kt` — ফ্ল্যাগ/সুপার/view-গেট লজিক + ক্যাটালগ ইনভ্যারিয়েন্ট।

### ⚠️ হ্যান্ড-অফ নোট থেকে ইচ্ছাকৃত বিচ্যুতি (২টা, কারণসহ)
1. **`onSaveRole` সিগনেচার বদলেছে** — ২.১-এ fire-and-forget `(id,name,perms) -> Unit` ছিল, এখন চতুর্থ প্যারামিটার
   `onDone: (Boolean) -> Unit`। কারণ: আগের ডিজাইনে সেভ চাপলেই এডিটর বন্ধ হতো, তারপর সার্ভার `ROLE_NAME_TAKEN` দিলে
   ব্যবহারকারীর টিক-করা সব পারমিশন হারিয়ে যেত। এখন সফল হলেই বন্ধ; ব্যর্থ হলে এডিটর খোলা + টোস্টে কারণ; সেভ চলাকালীন
   বাটন disabled ("সংরক্ষণ হচ্ছে…")। `onDeleteRole` অপরিবর্তিত (fire-and-forget)।
2. **`SyncAwareContent`-এ ERROR ফেজ সরাসরি পাঠানো হয়নি** — `SyncAwareContent` ERROR-এ (`hasCleanSyncHistory()` সত্য হলে)
   cached কনটেন্ট দেখায়; এই ডেটার ক্ষেত্রে সেটা ভুল ("এখনো কোনো রোল নেই" দেখাতো, আসলে আনাই যায়নি)। তাই ERROR-কে
   গেটে LOADED হিসেবে পাঠিয়ে `AdminRoleManagementView` নিজে "আনা যায়নি + আবার চেষ্টা করুন" দেখায় (`loadFailed`/`onRetry`)।
   পাশের ফল: `SyncAwareContent`-এর ON_RESUME auto-retry এই ট্যাবে ERROR-এ কাজ করে না — ট্যাবে পুনরায় ঢুকলে
   `LaunchedEffect` retry চালায়, আর বাটনও আছে।

### যা যাচাই হয়নি (সৎ তালিকা)
(ক) কম্পাইল — সবচেয়ে সম্ভাব্য ঝুঁকির জায়গা: `AdminRolePreviewPanel.kt`-এ `FlowRow` (`ExperimentalLayoutApi`, প্রজেক্টে আগে থেকেই
ব্যবহৃত), `return@Column` লেবেল, `AdminPanelScreen`-এ `when`-শাখায় `AdminPermissionCatalog.ROLE_MGMT_TAB_INDEX` const শর্ত;
(খ) `AdminRolePreviewLogicTest` কখনো চলেনি; (গ) `admin_roles_list`/`admin_role_upsert`/`admin_role_delete` RPC-র আসল ডিভাইস
থেকে কল (DB-সাইড সেশন ১-এ যাচাই ছিল, ক্লায়েন্ট-সাইড এই প্রথম); (ঘ) প্রিভিউ-পেইনে সিস্টেম-ব্যাক বাটন — বিদ্যমান এডিটর-পেইনের
মতোই শুধু উপরের তীর-বাটন কাজ করে, সিস্টেম-ব্যাকে পুরো অ্যাডমিন প্যানেল থেকে বেরোয় (নতুন সমস্যা না, বিদ্যমান প্যাটার্ন);
(ঙ) রোল-ট্যাবে pull-to-refresh-এর সময় `refreshAdminTab` বাকি সাধারণ কাজও (48-ঘণ্টা auto-release ইত্যাদি) চালায় — বিদ্যমান আচরণ,
বদলানো হয়নি।

### যা টেস্ট করা উচিত (অ্যাপে) — সেশন ৩ সম্পূর্ণ, তাই একসাথে
১) বিল্ড হয় কিনা; ২) সুপার লগইনে ড্রয়ারের নিচে "রোল ম্যানেজমেন্ট" দেখা যায় (নন-সুপারের ড্রয়ারে নেই — সেশন ৪ শেষে
নন-সুপার অ্যাকাউন্ট তৈরি হলে আসলে যাচাই করা যাবে; এখন শুধু সুপারই আছে); ৩) ট্যাব খুললে প্রথমবার skeleton → তালিকা, শুরুতে
কমপক্ষে "Super Admin" রোল; ৪) নতুন রোল তৈরি (নাম + কিছু টিক) → তালিকায় আসে; ৫) একই নামে আবার তৈরি → টোস্ট "এই নামে আরেকটা
রোল…", **এডিটর খোলা থাকে ও টিক অক্ষত**; ৬) এডিট → পারমিশন বদল → সেভ; ৭) অ্যাকশন টিকে 'দেখুন' অটো-টিক, 'দেখুন' আনটিকে বাকি
সব আনটিক; ৮) সুপার রোলে এডিট/ডিলিট নেই, শুধু প্রিভিউ; ৯) এডিটরে "লাইভ প্রিভিউ" খুলে টিক বদলালে মেনু/চিপ/ইউজারগণ-সিমুলেশন
সাথে সাথে বদলায়; ১০) `users:users:view` আনটিক করলে সিমুলেশন "অ্যাক্সেস নেই" দেখায়; ১১) রোল ডিলিট + pull-to-refresh
(re-entry-তে পুরো ট্যাব flash করে না); ১২) অফলাইনে সেভ → "ইন্টারনেট সংযোগ ছাড়া…" টোস্ট, এডিটর খোলা।

### পরবর্তী: সেশন ৪ (এডমিন অ্যাকাউন্ট লিস্ট + active/inactive + flag/unflag + `admin_account_create`) — ব্যবহারকারীর কনফার্মেশনের অপেক্ষায়
- নতুন ড্রয়ার-আইটেম ইনডেক্স ২৬ একই "অ্যাডমিন ব্যবস্থাপনা" গ্রুপে (তখন গ্রুপ 2-item হয়ে expandable হবে)।
- অ্যাকাউন্ট-ভিত্তিক প্রিভিউ (`AdminRolePreviewPanel(isFlagged = account.flagged)`) সেশন ৪-এ এখান থেকেই ওয়্যার হবে।
- সেশন ১ থেকে পেন্ডিং সিদ্ধান্ত: রিয়েল-টাইম অনলাইন-ডট (broadcast/polling/presence — `admin_accounts` realtime publication-এ নেই)।
- এখনো খোলা (সেশন ৩ অডিট ফাইন্ডিং ৪): `factory_reset`/`cleanup_commission`/`system:explorer`/`refund_debug` গ্র্যানুলার না "শুধু সুপার"
  হার্ড-চেক — সেশন ৭.৪/৭.৬-এর শুরুতে কনফার্ম।

## সেশন ৪ — এডমিন অ্যাকাউন্ট লিস্ট + active/inactive + flag/unflag + `admin_account_create` — ✅ কোড সম্পন্ন (DB ২০২৬-০৯-২৪, UI + ওয়্যারিং ২০২৬-০৯-২৫), ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি

> ⚠️ **এই সেশন ব্যবহারকারীর অনুরোধে মাঝপথে থামানো হয়েছে** (zip চেকপয়েন্ট + হ্যান্ড-অফ)। সেশন ৪ **সম্পূর্ণ না**।
> পরের Claude: নিচের "পরবর্তী Claude সেশনের জন্য হ্যান্ড-অফ" থেকে শুরু করো। ব্যবহারকারীর কনফার্মেশন ছাড়া সেশন ৫-এ যাবে না।

### ব্যবহারকারীর টেস্ট-ফলাফল (সেশন ২-৩)
ব্যবহারকারী অ্যাপে সেশন ২-৩-এর চেকলিস্ট (লগইন ৮টা + রোল ম্যানেজমেন্ট ৮টা) চালিয়ে জানিয়েছেন **"সব ঠিক আছে"**
(২০২৬-০৯-২৪) — তাই সেশন ৪ শুরু হয়েছে। কোন ধাপে কী দেখেছেন তার আলাদা রেকর্ড নেই। সেশন ২-৩ এখন ✅ টেস্টেড ধরা যায়।

### ✅ যা সম্পন্ন
**১) DB — লাইভ Supabase (`mghvvpndkxnscwryfkib`)-এ apply হয়েছে, migration নাম `admin_account_create_session4`;
রিপোতে `supabase/migrations/zz_20260924170000_admin_account_create_session4.sql`** (লাইভ সংস্করণের সাথে শুধু কমেন্টে তফাত)।
- `admin_account_create(p_name, p_phone, p_password, p_role_id, p_designation default '', p_email default null) → jsonb`
  (সুপার-অনলি, SECURITY DEFINER): একই ট্রানজ্যাকশনে `auth.users` + `auth.identities` + `public.users` (role→'ADMIN') +
  `admin_accounts` + অডিট লগ `ADMIN_CREATED`। সুপারের সিড-করা লাইভ auth row-র গঠন হুবহু অনুকরণ (auth phone `8801XXXXXXXXX`
  '+' ছাড়া, টোকেন কলাম `''`, confirmed_at generated, identity `{sub, phone}`)। Edge Function বাছা হয়নি কারণ অ্যাটমিসিটি
  (অনাথ auth user ঝুঁকি নেই) আর রোলব্যাক-টেস্ট সম্ভব।
- `admin_account_reset_password(p_id uuid, p_new_password text) → boolean` (সুপার-অনলি; **মাস্টার প্ল্যানে ছিল না, আমি যোগ
  করেছি** — পাসওয়ার্ড হারালে সুপারের আর কোনো UI-উপায় থাকত না)। সুপার অ্যাকাউন্টে নিষিদ্ধ; `auth.sessions` মুছে + `admin_sessions`
  বন্ধ করে (best-effort); অডিট `ADMIN_PASSWORD_RESET`।
- ভ্যালিডেশন/এরর কোড: `INVALID_NAME|INVALID_PHONE|INVALID_PASSWORD(<৮ অক্ষর বা >৭২ বাইট)|INVALID_DESIGNATION|INVALID_EMAIL|
  ROLE_NOT_FOUND|CANNOT_ASSIGN_SUPER_ROLE|ADMIN_PHONE_TAKEN|PHONE_IN_USE_BY_USER|USER_ROW_MISSING|ACCOUNT_NOT_FOUND|
  SUPER_ADMIN_IMMUTABLE|ACCOUNT_HAS_NO_LOGIN` + সেশন ১-এর `SUPER_ADMIN_REQUIRED|CANNOT_CHANGE_SELF`।
- `PHONE_IN_USE_BY_USER`: যে ফোন `auth.users`/`public.users`-এ (যেকোনো ফরম্যাটে; লাইভে সাধারণ ইউজারের ফোন `8801…` ফরম্যাটে) আছে
  তাতে এডমিন বানানো যায় না — নইলে কারো বিদ্যমান অ্যাকাউন্ট চুপচাপ এডমিন হয়ে যেত।
- **লাইভ ভেরিফিকেশন (rolled-back ট্রানজ্যাকশন, residue নেই যাচাই করা):** তৈরি ✅ / ডুপ্লিকেট ফোন (লোকাল ও +880 ফরম) ✅ / সাধারণ
  ইউজারের ফোন ✅ / ছোট পাসওয়ার্ড, ভুল ফোন, ফাঁকা নাম, সুপার রোল, অজানা রোল, ভুল ইমেইল ✅ / নন-সুপার দিয়ে create/list/reset ব্লক ✅ /
  নতুন অ্যাডমিনের `admin_session_start`+`admin_heartbeat` ✅ / flag → `admin_can_act` ban=false, view=true ✅ / নিষ্ক্রিয় → `is_admin`=false,
  heartbeat ব্লক ✅ / রিসেট: নতুন পাসওয়ার্ড bcrypt ম্যাচ, পুরনোটা না ✅ / সুপারে রিসেট ব্লক ✅ / auth.users ও identities row সুপারের সিড-row-র
  সাথে কলাম-ধরে তুলনায় কোনো পার্থক্য নেই ✅ / অডিট লগে `admin_name=Support Manager` ✅ / anon-এর execute নেই ✅।
- **যা এখানে যাচাই হয়নি:** GoTrue-র আসল sign-in (নেটওয়ার্ক ছিল না) — নতুন অ্যাকাউন্ট দিয়ে ডিভাইসে লগইন করে দেখতে হবে।

**২) Kotlin (কখনো কম্পাইল/রান হয়নি — Gradle/SDK ছিল না):**
- `data/security/AdminSession.kt`: `AdminAccountInfo`-তে ঐচ্ছিক (ডিফল্ট-null) `createdAt`, `flaggedAt`, `lastSeenAt` + `fromJson` পার্সিং।
- `data/security/AdminPermissionCatalog.kt`: `const val ADMIN_ACCOUNTS_TAB_INDEX = 26`; `admin_mgmt:admin_accounts` আইটেমের `tabIndex = 26`।
- `data/remote/SupabaseSyncManager.kt`: `adminAccountsList()`, `adminAccountCreate(name, phone, password, roleId, designation, email)`,
  `adminAccountSetRole(id, roleId)`, `adminAccountSetActive(id, active, reason)`, `adminAccountSetFlagged(id, flagged, reason)`,
  `adminAccountResetPassword(id, newPassword)`।
- `ui/viewmodel/SomadhanViewModel.kt` (রোল-ব্লকের ঠিক পরে, "সেশন ৪" হেডার): `adminAccounts`/`adminAccountsSyncPhase` StateFlow;
  `loadAdminAccounts(silent = false)`; অ্যাকশন (সবগুলোতে শেষ প্যারামিটার `onDone: (Boolean) -> Unit`, সফল হলে true):
  `adminCreateAccount(name, phone, password, roleId, designation, email, onDone)`, `adminChangeAccountRole(account, roleId, onDone)`,
  `adminSetAccountActive(account, active, reason, onDone)`, `adminSetAccountFlagged(account, flagged, reason, onDone)`,
  `adminResetAccountPassword(account, newPassword, onDone)`। সব অ্যাকশন সাফল্যে টোস্ট + তালিকা ও রোল (account_count) রিফ্রেশ; ব্যর্থতায়
  বাংলা টোস্ট + তালিকা রিফ্রেশ। `refreshAdminTab(26)` (pull-to-refresh) ও logout-ক্লিনআপ যোগ হয়েছে।
- `ui/screens/AdminAccountsView.kt` — **এখন শুধু pure হেল্পার**: `accountErrorMessage`, `normalizeAdminPhoneInput`, `isValidAdminPhone`,
  `filterAdminAccounts`, `adminInitials`, `formatAdminTimestamp` (VM এগুলো ডাকে বলে ফাইলটা রাখা হয়েছে যাতে বিল্ড কম্পাইল হয়)।
- `app/src/test/.../AdminAccountsLogicTest.kt`: হেল্পার + ক্যাটালগ ইনডেক্স-২৬ টেস্ট (কখনো চালানো হয়নি)।

### ✅ সেশন ৪-এর বাকি অংশ সম্পন্ন (২০২৬-০৯-২৫)
- **`AdminAccountsView.kt`** (হেল্পারগুলো অপরিবর্তিত, শুধু `INVALID_PASSWORD` বার্তায় সার্ভারের ৭২-বাইট সীমা যোগ; নিচে UI):
  তালিকা-পেইন (সার্চ, "N জন এডমিন", "＋ নতুন এডমিন"), অ্যাকাউন্ট-কার্ড (অ্যাভাটার, নাম, 🚩 ট্যাগ, ফোন, পদবি, রোল-ব্যাজ, সক্রিয়/নিষ্ক্রিয় পিল,
  অনলাইন-ডট, সর্বশেষ লগইন + ডিভাইস, অ্যাকশন-বাটন), সুপার কার্ডে শুধু প্রিভিউ + 🔒 নোট, নিজের অ্যাকাউন্টে ফ্ল্যাগ/নিষ্ক্রিয় বাটন নেই,
  তৈরির ফর্ম (পূর্ণ-পেন; রোল **স্পষ্টভাবে বাছতে হয়** — কোনো রোল প্রি-সিলেক্টেড নয়, ইনলাইন এরর), অ্যাকাউন্ট-ভিত্তিক প্রিভিউ-পেইন
  (`AdminRolePreviewPanel(..., isFlagged)`), ডায়ালগ ৩ ধরনের: কারণ-ডায়ালগ (ফ্ল্যাগ/নিষ্ক্রিয়ে কারণ ≥৩ অক্ষর আবশ্যক; আনফ্ল্যাগ/সক্রিয়ে ঐচ্ছিক),
  রোল-বদল (নন-সুপার রেডিও-তালিকা), পাসওয়ার্ড-রিসেট (≥৮, দেখা/লুকানো)। সব কলব্যাকে `onDone(Boolean)` — সফল হলেই বন্ধ।
  ফ্ল্যাগ/সক্রিয় ডায়ালগ খোলার সময়ের লক্ষ্য-মান (`PendingToggle`) ধরে রাখে, যাতে সফল রিফ্রেশে শিরোনাম উল্টে না যায়।
- **loading নিয়ম:** per-item `rememberFieldChangePulse(account.copy(lastSeenAt = null), flashOnReentry = false)` (Ground Rule ১৯; heartbeat-এর
  `lastSeenAt` বাদ); সার্চ বদলালে দৃশ্যমান সব কার্ড pulse (Ground Rule ২০, `AdminUsersView`-এর `isFilterRefreshing` প্যাটার্ন হুবহু);
  pull-to-refresh আগে থেকেই `refreshAdminTab(26)`-এ; GR২১ (realtime-insert) প্রযোজ্য না — এই ডেটা realtime-এ নেই। sessionKey `"admin_accounts_sync"`।
- **`AdminPanelScreen.kt`:** "অ্যাডমিন ব্যবস্থাপনা" ড্রয়ার-গ্রুপে দ্বিতীয় আইটেম (ইনডেক্স ২৬, `Icons.Default.People`) — এখন গ্রুপ-শিরোনামসহ expandable;
  ট্যাব `when`-এ ২৬-এর শাখা (নন-সুপারের জন্য দ্বিতীয়-স্তর গেট + `SyncAwareContent` + ERROR→LOADED ম্যাপিং, রোল-ট্যাবের মতোই); ট্যাবে ঢুকলে
  `loadAdminAccounts()` + `loadAdminRoles()`।
- **পোলিং — হ্যান্ড-অফ নোট থেকে ইচ্ছাকৃত বিচ্যুতি (১টা):** নোটে `Lifecycle.State.STARTED` চেক ছিল; আমি `LifecycleEventObserver` দিয়ে ON_RESUME/ON_PAUSE
  ধরে `isScreenResumed` state বানিয়েছি (MotionToolkit-এর বিদ্যমান প্যাটার্ন; `repeatOnLifecycle` প্রজেক্টে কোথাও নেই, ডিপেন্ডেন্সি নিশ্চিত নয়)।
  ব্যাকগ্রাউন্ডে পোলিং থামে, ফিরলে সাথে সাথে একবার রিফ্রেশ করে ২০সে লুপ আবার শুরু হয়।
- **সার্ভার-কোড মিলিয়ে দেখা:** `admin_account_reset_password` সত্যিই `auth.sessions` মুছে ও `admin_sessions` বন্ধ করে — তাই পাসওয়ার্ড-ডায়ালগের
  "চলমান সেশন বন্ধ হবে" লেখা সঠিক। সার্ভার পাসওয়ার্ড ৭২ বাইটেও সীমিত (তাই বার্তা আপডেট)।
- **`TEMP-SESSION7` চিহ্ন:** তৈরির ফর্মের সতর্কতা-কার্ড + ফ্ল্যাগ-ডায়ালগের নোট — সেশন ৭ শেষে মুছতে হবে (`grep -rn TEMP-SESSION7 app/src`)।
- **টেস্ট:** `AdminAccountsLogicTest.kt`-এ ২টা নতুন (পাসওয়ার্ড-বার্তায় ৭২ সীমা; pulse-কী `lastSeenAt` উপেক্ষা করে কিন্তু অনলাইন/ফ্ল্যাগ বদল ধরে)।

### ⚠️ যা যাচাই হয়নি (সৎ তালিকা)
- **সেশন ৪-এর কোনো Kotlin কখনো কম্পাইল হয়নি** (Gradle/SDK/kotlinc নেই)। ব্র্যাকেট-ব্যালেন্স ঠিক, ইমপোর্ট/কনভেনশন গ্রেপ-মেলানো। সম্ভাব্য ঝুঁকির জায়গা:
  `FlowRow`/`ExperimentalLayoutApi` (`AccountCard`), `Modifier.weight(1f, fill = false)`, `androidx.compose.ui.platform.LocalLifecycleOwner`
  (MotionToolkit-এ একই ইমপোর্ট আছে, তাই সম্ভবত ঠিক), `RadioButton`/`OutlinedTextField.minLines`।
- ডিভাইসে কিছুই চালানো হয়নি: নতুন এডমিনের GoTrue sign-in, ২০সে পোলিং-এ অনলাইন-ডট, ফ্ল্যাগ/নিষ্ক্রিয়, রোল-বদল, পাসওয়ার্ড-রিসেট — নিচের "যা টেস্ট করা উচিত" দেখো।
- সার্চ-বদলে whole-list pulse (GR২০) এন্ট্রির সময়েও একবার চলে (`AdminUsersView`-এর প্যাটার্ন হুবহু, তাই ইচ্ছাকৃত) — অপছন্দ হলে জানাও।

### সিদ্ধান্ত (এই সেশনে নেওয়া — ব্যবহারকারী দ্বারা আলাদা কনফার্ম হয়নি, আপত্তি থাকলে বদলাও)
- **অনলাইন-ডট = সার্ভারের ৯০সে-heartbeat হিসাব + ট্যাব খোলা থাকলে ~২০সে নিঃশব্দ পোলিং।** realtime publication/presence বাদ (নতুন মাইগ্রেশন/RLS
  লাগে না; সর্বোচ্চ ~২০সে দেরি)। সেশন ১-এর মুলতুবি সিদ্ধান্ত এভাবে বন্ধ।
- `admin_account_reset_password` মাস্টার প্ল্যানের বাইরে যোগ (উপরে কারণ)। নাম/পদবি/ইমেইল এডিট এই ট্যাবে নেই — সেশন ৬-এ প্রোফাইল থেকে
  (সুপার সবার প্রোফাইল এডিট করে, `admin_profile_update` আগে থেকেই আছে)।
- পাসওয়ার্ড ন্যূনতম ৮ অক্ষর (অ্যাপের বিদ্যমান এডমিন-পাসওয়ার্ড নিয়ম)।

### ⚠️ সতর্কতা (ব্যবহারকারীকে জানানো হয়েছে)
১) **সেশন ৭ শেষ না হওয়া পর্যন্ত** যেকোনো নন-সুপার এডমিন সুপার-অনলি ট্যাব বাদে *সব* বিদ্যমান স্ক্রিনে পূর্ণ অ্যাক্সেস পাবে (ড্রয়ার/বাটন এখনো
   পারমিশনে গেটেড নয়; DB-র RLS শুধু `is_admin` দেখে)। ফ্ল্যাগেরও অ্যাকশন-লক এখনো বিদ্যমান স্ক্রিনে নেই। তাই আসল স্টাফের অ্যাকাউন্ট নয়, শুধু টেস্ট।
২) **নিরাপত্তা ফাঁক (সেশন ৭-এ ঠিক করো):** বিদ্যমান Edge Function `admin-reset-user-password` শুধু `is_admin` দেখে — অর্থাৎ যেকোনো এডমিন
   যেকোনো ইউজারের (সুপার এডমিন সহ!) পাসওয়ার্ড রিসেট করতে পারে। একাধিক এডমিন থাকলে এটা privilege-escalation; টার্গেট এডমিন হলে শুধু সুপারকে
   অনুমতি দিতে হবে (বা `users:users:reset_password` গেট + টার্গেট-অ্যাডমিন ব্লক)।
৩) সেশন ৪-এ তৈরি করা টেস্ট অ্যাকাউন্ট দিয়ে GoTrue sign-in ডিভাইসে যাচাই বাকি (কেন: ওপরে)।

### যা টেস্ট করা উচিত (UI শেষ হলে, ডিভাইসে)
বিল্ড → সুপার লগইন → ড্রয়ারে "এডমিন অ্যাকাউন্ট" → তালিকায় শুধু সুপার (🔒) → একটা রোল বানিয়ে নতুন এডমিন তৈরি → **অন্য ডিভাইস/লগআউট করে সেই
ফোন+পাসওয়ার্ডে লগইন** (সবচেয়ে গুরুত্বপূর্ণ) → সুপারে ফিরে অনলাইন-ডট (~২০সে) → ফ্ল্যাগ/আনফ্ল্যাগ, নিষ্ক্রিয়/সক্রিয় (নিষ্ক্রিয় করলে ওই ডিভাইস ~৩০সে-এ
জোর-লগআউট) → রোল বদল → পাসওয়ার্ড রিসেট ও নতুন পাসওয়ার্ডে লগইন → প্রিভিউ → লগ ট্যাবে কিছু না (সেশন ৫) → নন-সুপার লগইনে ড্রয়ারে রোল ম্যানেজমেন্ট/এডমিন
অ্যাকাউন্ট নেই।
## সেশন ৫ — অ্যাক্টিভিটি লগ ট্যাব (সুপার-অনলি, নাম-ফিল্টার) — ✅ কোড সম্পন্ন (DB ২০২৬-০৯-২৫ লাইভে apply + যাচাই), ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি

ব্যবহারকারীর সিদ্ধান্ত (২০২৬-০৯-২৫): সেশন ৫ ও ৬ শেষ হওয়ার পর একসাথে রিয়েল ডিভাইসে টেস্ট (সেশন ৪-এর ডিভাইস-টেস্টও তখনই)।

### সবচেয়ে গুরুত্বপূর্ণ আবিষ্কার (মাস্টার প্ল্যানের দুটো ধারণা বদলেছে)
1. **`log_admin_action` কল-সাইট বদলানোর দরকার নেই** (মাস্টার প্রম্পটের "স্ক্রিন বাই স্ক্রিন কল-সাইট আপডেট" বাতিল): সেশন ১-এ ফাংশনটা
   `admin_id/admin_name/admin_role_name` **auth.uid() থেকে সার্ভারে নিজেই** বসায়, আর সেশন ২-এর পর প্রতিটা এডমিন নিজের Supabase Auth সেশনে কাজ করে।
   লাইভ ডেটায় যাচাই: ২০২৬-০৯-২৪-এর পর `ADMIN_LOGIN`, `ROLE_CREATED`, `UPDATE_SETTING` (২৯/৬৪), `ENABLE/DISABLE_CATEGORY`, `COMPLETE/REJECT_WITHDRAWAL`,
   `ADMIN_RESOLVE_DISPUTE` ইত্যাদিতে admin_id বসে আছে। পরিচয়হীন যা আছে: ২০২৬-০৯-২৪-এর আগের লগ, আর সিস্টেম/ইউজার-ট্রিগার্ড ইভেন্ট
   (`DISPUTE_RAISED`, `WALLET_DEPOSIT`, cron-ধরনের `ADMIN_RECONCILE_ESCROW_STATES`) — UI এগুলোকে "সিস্টেম / লিগ্যাসি" দেখায়।
2. **বিদ্যমান লগ-ট্যাব (১২) ডিভাইস-লোকাল**: `admin_audit_logs` কখনো cloud→Room pull হয় না (রিপোর ভেতরের কমেন্টেও লেখা), তাই `AdminAuditLogView` শুধু ওই ফোনের
   নিজের লগ দেখায়। মাল্টি-এডমিনে সুপারের দরকার সবার লগ → **নতুন আলাদা ট্যাব ২৭, ক্লাউড থেকে সরাসরি**। ট্যাব ১২ অপরিবর্তিত (ক্যাটালগে "লিগ্যাসি" নামে আলাদা
   permission আইটেম হিসেবে থাকল)।

### কী তৈরি হয়েছে
- **DB (লাইভ `mghvvpndkxnscwryfkib`-এ apply, migration নাম `admin_activity_log_session5`; রিপো ফাইল `zz_20260925100000_admin_activity_log_session5.sql`):**
  `admin_activity_logs_list(p_admin_id, p_name_query, p_unattributed_only, p_before, p_before_id, p_limit)` — সুপার-অনলি (`_admin_require_super`), সার্ভার-সাইড ফিল্টার
  (এডমিন-আইডি / নামের অংশ ILIKE (`% _ \` escape করা) / শুধু পরিচয়হীন) + পেজিনেশন। ফেরত `{"rows":[...], "has_more":bool}` (limit+1 কৌশল)। কার্সর = **(timestamp, id) জোড়া**
   — শুধু timestamp ভুল হতো, কারণ একই ট্রানজ্যাকশনের অনেক লগ একই `now()` পায় (লাইভে reconcile-এর ৮৯টা সারি)। limit ১-১০০-এ ক্ল্যাম্প। সাথে `admin_audit_logs_ts_idx`।
  **লাইভে যাচাই:** (ক) লগইন ছাড়া → `AUTH_REQUIRED`; (খ) সুপার হিসেবে ৩০-এর পেজে কার্সর ধরে পুরো টেবিল হাঁটা: টেবিলে ২১৪ সারি → ৮ পেজে ২১৪ সারি, ২১৪ ইউনিক (ডুপ্লিকেট/বাদ নেই);
  (গ) নাম-ফিল্টার, `%`/`_` লিটারেল (০ ফল), এডমিন-আইডি ফিল্টার, পরিচয়হীন ফিল্টার, limit ক্ল্যাম্প ঠিক; (ঘ) `anon`-এর execute নেই, `authenticated`/`service_role`-এর আছে।
  ⚠️ নন-সুপার এডমিনের `SUPER_ADMIN_REQUIRED` পাথ লাইভে টেস্ট হয়নি (লাইভে এখনো কোনো নন-সুপার অ্যাকাউন্ট নেই) — গার্ড বাকি সব সুপার-অনলি RPC-র হুবহু একই `_admin_require_super()`।
- **Kotlin:**
  - `AdminActivityLog.kt` (নতুন): `AdminActivityLogEntry`/`AdminActivityLogPage`/`AdminActivityLogFilter`। timestamp সার্ভারের ISO স্ট্রিং **হুবহু** রাখা হয় (মাইক্রোসেকেন্ড অক্ষত, কার্সর হিসেবে ফেরত যায়)।
  - `SupabaseSyncManager.adminActivityLogsList(...)`।
  - `SomadhanViewModel`: state (`adminActivityLogs`, `…HasMore`, `…SyncPhase`, `…Filter`, `…Filtering`, `…LoadingMore`), `loadAdminActivityLogs()`,
    `applyAdminActivityLogFilter()`, `loadMoreAdminActivityLogs()`, আর `refreshAdminTab(27)` (pull-to-refresh = বর্তমান ফিল্টারে প্রথম পেজ)। ফিল্টার শুধু **সফল fetch-এর পরে** বদলায় — ব্যর্থ হলে
    আগের ফিল্টার + তালিকা অক্ষত (তালিকা-ফিল্টার গরমিল হয় না); generation-কাউন্টারে দেরিতে-আসা পুরনো রেসপন্স ফেলে দেওয়া হয়।
  - `AdminActivityLogView.kt` (নতুন): ফিল্টার-কার্ড — এডমিনের নামের **সার্চ-ইনপুট + ড্রপডাউন-সিলেক্ট (দুটোই)** + **explicit "ফিল্টার করুন" বাটন** (লাইভ-ফিল্টার না; কিবোর্ডের Search-ও একই কাজ) + "রিসেট";
    ড্রপডাউনে "সব এডমিন" / প্রতিটা এডমিন (নাম · রোল) / "সিস্টেম / লিগ্যাসি"। প্রতিটা কার্ডের হেডলাইনে **এডমিনের নাম + রোল-ব্যাজ পাশাপাশি**, তারপর অ্যাকশন-ভার্ব + টার্গেট + কারণ/বিবরণ (`details`) + টাইমস্ট্যাম্প।
    scroll-to-load (শেষের ~৩ আইটেমের কাছে) + "আরও দেখুন" ফলব্যাক বাটন। ৩০টা করে পেজ।
  - `AdminPanelScreen.kt`: "অ্যাডমিন ব্যবস্থাপনা" গ্রুপে তৃতীয় আইটেম "অ্যাক্টিভিটি লগ" (ইনডেক্স ২৭, `Icons.Default.History`), সুপার-অনলি দুই-স্তরের গেট, `SyncAwareContent` (sessionKey `"admin_activity_log_sync"`)।
  - `AdminPermissionCatalog`: `ACTIVITY_LOG_TAB_INDEX = 27`; `admin_mgmt:activity_log`-এর `tabIndex` null → 27।
  - `AdminAuditLogView.kt`: শুধু `getBengaliActionName` আর `getAuditActionVisuals`-এর `private` → `internal` (নতুন ভিউ পুনর্ব্যবহার করে; আচরণ অপরিবর্তিত)।
- **loading নিয়ম:** cold-load skeleton (`SyncAwareContent`); GR১৮ — পুরো-পেজ কখনো ঝলকায় না; GR২০ — ফিল্টার/পেজিনেশন (`appliedFilter`, `logs.size`) বদলে দৃশ্যমান সব কার্ড pulse (`AdminUsersView`-এর `isFilterRefreshing` প্যাটার্ন,
  try/finally), pull-to-refresh শেষেও pulse; ফিল্টার চলাকালীন শুধু ছোট `LinearProgressIndicator`; scroll-to-top শুধু raw `appliedFilter` বদলে। GR১৯ (per-item ডেটা-বদল) ও GR২১ (realtime-insert) প্রযোজ্য না — লগ immutable ও realtime-এ নেই।
- **টেস্ট:** `AdminActivityLogLogicTest.kt` (নতুন, ৯টা): মডেল-পার্সিং (timestamp হুবহু, পরিচয়হীন সারি, ভাঙা সারি), পেজ-পার্সিং, ফিল্টার-গঠন/বিবরণ, অ্যাকশন-লেবেল (নতুন কোড + লিগ্যাসি ফলব্যাক + অজানা কোড), এরর-বার্তা, ক্যাটালগ ইনডেক্স ২৭ ও ইউনিক।

### সিদ্ধান্ত (এই সেশনে নেওয়া — ব্যবহারকারী দ্বারা আলাদা কনফার্ম হয়নি, আপত্তি থাকলে বদলাও)
- ট্যাব ১২ (লিগ্যাসি, ডিভাইস-লোকাল) **অপরিবর্তিত, সবার জন্যই দৃশ্যমান** থাকল — ক্যাটালগের কমেন্টে "সেশন ৫-এ সুপার-অনলি হবে" লেখা ছিল, কিন্তু ক্যাটালগে ওটা আলাদা গেটযোগ্য permission আইটেম, তাই সুপার-অনলি করা সেশন ৭.৫-এ (মডারেশন গ্রুপের সাথে) ঠিক করা ভালো। ট্যাব ১২-কে লুকিয়ে দিলে সিস্টেম-অ্যালার্টও (`*_AUTO_REPAIRED`) ওই ফোনে দেখা বন্ধ হবে, তাই সিদ্ধান্তটা জরুরি না, ইচ্ছে করে থামিয়ে রাখা।
- "সিস্টেম / লিগ্যাসি" ফিল্টার-অপশন যোগ করেছি (মাস্টার প্ল্যানে ছিল না) — কারণ ৮৯টা cron-ধরনের `ADMIN_RECONCILE_ESCROW_STATES` সারি নাম-ফিল্টার ছাড়া আসল এডমিনের কাজ চাপা দেয়।
- লগ-তালিকা রিয়েল-টাইম/পোলিং না — ট্যাব খোলা ও pull-to-refresh-এ আসে (এডমিনের কাজ-ট্র্যাকিংয়ে ২০সে-পোলিং-এর মতো লাইভ দরকার নেই, আর ট্যাবে পড়তে পড়তে তালিকা লাফালে বিরক্তিকর)।

### ⚠️ যা যাচাই হয়নি (সৎ তালিকা)
- **সেশন ৪ + ৫-এর কোনো Kotlin কখনো কম্পাইল হয়নি** (Gradle/SDK/kotlinc নেই); ব্র্যাকেট-ব্যালেন্স ঠিক, ইমপোর্ট/কনভেনশন গ্রেপ-মেলানো। সম্ভাব্য ঝুঁকি: `DropdownMenu`/`DropdownMenuItem(text = …)` (M3 বর্তমান API), `rememberSaveable { mutableStateOf<String?>(…) }`,
  `KeyboardActions/ImeAction`, `LinearProgressIndicator(color = …)`, `val (icon, tint, tintBg) = getAuditActionVisuals(...)` (@Composable-এর Triple ভাঙা)।
- লাইভে শুধু SQL-লেভেল যাচাই হয়েছে; অ্যাপ থেকে RPC-কল (postgrest JSON প্যারামিটার: `p_admin_id`/`p_before` null পাঠানো, `timestamptz` স্ট্রিং কার্সর) ডিভাইসে যাচাই বাকি।
- ডিভাইসে ফিল্টার-প্রয়োগ, scroll-to-load, "সিস্টেম / লিগ্যাসি" অপশন, pull-to-refresh, দ্বিতীয় এডমিনের কাজ প্রথম এডমিনের নামে লগ হয় কিনা — সব বাকি।

### যা টেস্ট করা উচিত (সেশন ৫-৬ শেষে একসাথে, ডিভাইসে)
সুপার লগইন → ড্রয়ার "অ্যাডমিন ব্যবস্থাপনা" → "অ্যাক্টিভিটি লগ" → স্কেলিটন তারপর তালিকা (ওপরে সবচেয়ে নতুন) → প্রতিটা কার্ডে এডমিনের নাম + রোল-ব্যাজ → নিচে স্ক্রল করে আরও পেজ আসে ও ডুপ্লিকেট নেই →
নাম লিখে "ফিল্টার করুন" → শুধু ওই এডমিনের লগ → ড্রপডাউন থেকে এডমিন বাছা → "সিস্টেম / লিগ্যাসি" বাছলে নামের ইনপুট বন্ধ → "রিসেট" → pull-to-refresh → **দ্বিতীয় (টেস্ট) এডমিন দিয়ে একটা কাজ করে (যেমন সেটিংস বদল) সুপারের লগে সেই এডমিনের নামে এন্ট্রি আসে কিনা** (সবচেয়ে গুরুত্বপূর্ণ) → নন-সুপার লগইনে ড্রয়ারে এই আইটেম নেই।

### পরবর্তী: সেশন ৬ (`AdminProfileView`) — ব্যবহারকারীর কনফার্মেশনের অপেক্ষায়
পরের ফ্রি ট্যাব-ইনডেক্স = ২৮ (কিন্তু ধাপ ৫-এর ৮ নং উত্তর অনুযায়ী প্রোফাইল ড্রয়ারে না — টপ-বারের ডান পাশে সবার জন্য অবতার/আইকন থেকে খোলে, তাই ট্যাব-ইনডেক্স আদৌ লাগবে কিনা সেশন ৬-এর শুরুতে দেখতে হবে)।
## সেশন ৬ — `AdminProfileView` (ছবি, পদবি, পারমিশন চিপ, লগইন তথ্য, সিকিউরিটি) — ✅ কোড সম্পন্ন (DB ২০২৬-০৯-২৫ লাইভে apply + যাচাই), ✅ রিয়েল-ডিভাইস টেস্ট (ব্যবহারকারী, ২০২৬-০৯-২৫) — ডিজাইন-ইস্যু পাওয়া গিয়ে এই সেশনেই ফিক্স হয়েছে, নিচে দ্রষ্টব্য

শুরুতে সিদ্ধান্ত: প্রোফাইল ড্রয়ারে আলাদা ট্যাব-ইনডেক্স হিসেবে না — ধাপ ৫-এর ৮ নং উত্তর অনুযায়ী টপ-বারের ডান পাশে
সবার জন্য অবতার-আইকন থেকে ফুলস্ক্রিন `Dialog` ওভারলে হিসেবে খোলে (`AdminPermissionCatalog`-এ তাই কোনো নতুন
`*_TAB_INDEX` যোগ হয়নি, ২৮ এখনো ফাঁকা)।

### কী তৈরি হয়েছে
- **DB (লাইভ `mghvvpndkxnscwryfkib`-এ apply, migration নাম `admin_profile_photos_session6`; রিপো ফাইল
  `zz_20260925120000_admin_profile_photos_session6.sql`):** নতুন public bucket `admin-profile-photos`
  (২MB সীমা, jpeg/png/webp)। RLS: সবাই পড়তে পারে; লেখা/আপডেট/ডিলিট শুধু সক্রিয় এডমিনের নিজের ফোল্ডারে
  (`(storage.foldername(name))[1] = auth.uid()::text` + `is_admin(auth.uid())`, session ১-এর
  `is_admin`-এর নিয়মেই নিষ্ক্রিয় এডমিনের জন্য false)। বিদ্যমান `profile-photos`/`kyc-docs` bucket থেকে
  ইচ্ছাকৃত আলাদা (এডমিনের ছবির লেখার-অধিকার শুধু এডমিনের, সাধারণ ইউজারের bucket-এর সাথে না মেশানো)।
  ⚠️ সম্পূর্ণ ব্লক CI-safe `to_regclass('storage.buckets') is null` গার্ডে — CI-র plain Postgres-এ
  `storage` স্কিমা নেই বলে নিঃশব্দে skip হয় (বিদ্যমান bucket-গুলোর মতোই)। **লাইভে bucket + ৪টা পলিসি
  SQL কোয়েরি চালিয়ে যাচাই করা হয়েছে (bucket public=true, size=2097152, mime-types ঠিক আছে)।**
  `admin_profile_update` RPC সেশন ১-এই তৈরি ছিল (`p_id`/`p_name`/`p_designation`/`p_email`/`p_bio`/
  `p_photo_url`, null=অপরিবর্তিত, নিজের/সুপার হলে যেকারো) — এই সেশনে নতুন কোনো RPC/মাইগ্রেশন RPC-অংশ লাগেনি।
- **Kotlin (নতুন ফাইল):**
  - `AdminProfile.kt` (`data/security`): `AdminSessionRecord` (`admin_sessions` সারি → মডেল, RLS-সুরক্ষিত
    টেবিল থেকে সরাসরি select, আলাদা RPC ছাড়াই), `AdminProfileRules` (নাম/পদবি/ইমেইল/bio-র ক্লায়েন্ট-সাইড
    ভ্যালিডেশন, সার্ভারের সীমার সাথে হুবহু মেলানো — সার্ভার আবার যাচাই করে), `adminPermissionSummary()`
    (রোলের পারমিশন-সেট → "আইটেম — অ্যাকশন, অ্যাকশন" চিপ-টেক্সট, `AdminPermissionCatalog` থেকে; সুপারের
    জন্য একটাই "সব এক্সেস" চিপ)।
  - `AdminProfilePhotoUploader.kt` (`util`): `admin-profile-photos` bucket-এ আপলোড। ⚠️ ইচ্ছাকৃতভাবে
    বিদ্যমান `ImageStorageUtil.uploadProfilePhoto()`-এর প্যাটার্ন **অনুসরণ করেনি** — ওটা ব্যর্থ হলে লোকাল
    `file://` URI ফেরত দেয় (সাধারণ ইউজারের নিজের ফোনে ঠিক আছে), কিন্তু এডমিনের ছবি অন্য এডমিনের/সুপারের
    ফোনেও দেখাতে হয়, যেখানে `file://` ভাঙা URL। তাই এখানে লোকাল fallback নেই — ব্যর্থ হলে সরাসরি
    `Result.failure` (কোড: `PHOTO_TOO_LARGE`/`PHOTO_READ_FAILED`/`PHOTO_UPLOAD_TIMEOUT`), কিছুই সেভ হয় না।
  - `AdminAvatar.kt` (`ui/components`): শেয়ার্ড অবতার কম্পোজেবল (ছবি থাকলে ছবি, নইলে আদ্যক্ষর) — টপ-বার,
    প্রোফাইল-কার্ড, প্রোফাইলের এডমিন-তালিকা, ও `AdminAccountsView`-এর `AccountCard`-এ (৪ জায়গাতেই) ব্যবহৃত,
    যাতে ছবি আপডেট সব জায়গায় সাথে সাথে দেখা যায়।
  - `AdminProfileView.kt` (`ui/screens`): মূল প্রোফাইল স্ক্রিন। সুপার হলে বাম কলামে সার্চেবল এডমিন-তালিকা
    (`filterAdminAccounts` পুনর্ব্যবহার) + ডানে নির্বাচিত অ্যাকাউন্টের কার্ড; নন-সুপার শুধু নিজেরটা দেখে।
    কার্ডে: ছবি (ক্যামেরা-বাটনে গ্যালারি-পিকার, `ActivityResultContracts.GetContent()` — বিদ্যমান
    `ProfileScreen.kt`-এর প্যাটার্ন), নাম/পদবি/bio এডিটেবল ফিল্ড, ফোন disabled + নোট, ইমেইল, ফ্ল্যাগ-ব্যানার,
    রোল-অনুযায়ী পারমিশন-চিপ (শুধু-পড়ার), অনলাইন-ডট + সর্বশেষ লগইন/ডিভাইস/আইপি, সাম্প্রতিক ৫টা সেশন
    (`admin_sessions` থেকে, `viewModel.fetchAdminSessions`), শুধু নিজের জন্য পাসওয়ার্ড-বদল বাটন (সেশন
    ২-এর `adminUpdateCredentials` পুনর্ব্যবহার, বর্তমান পাসওয়ার্ড re-verify করে)। বাতিল/সংরক্ষণ বাটন শুধু
    ড্রাফট সত্যিই বদলালে সক্রিয় (`dirty` চেক)। `profileErrorMessage()`/`photoUploadErrorMessage()` —
    সার্ভার/আপলোডার কোড → বাংলা বার্তা (`accountErrorMessage`-এর প্যাটার্নে)।
- **Kotlin (বিদ্যমান ফাইলে সংযোজন, কোনো বিদ্যমান সিগনেচার/আচরণ বদলায়নি):**
  - `SupabaseSyncManager.kt`: `adminProfileUpdate(targetId, name, designation, email, bio, photoUrl)` ও
    `adminSessionsList(adminId, limit)` (সরাসরি `admin_sessions` টেবিল select, RLS-নির্ভর)।
  - `SomadhanViewModel.kt`: `adminSaveProfile(...)` (সুপার/নিজ-চেক → ভ্যালিডেশন → ছবি-অবস্থা তিন রকম
    [সরানো/আগে-আপলোড-হওয়া-URL/নতুন-আপলোড] অগ্রাধিকার-ক্রমে হ্যান্ডল করে যাতে RPC ব্যর্থ হলেও দ্বিতীয়বার
    রি-আপলোড না লাগে → RPC → নিজের হলে `AdminSession.update` + `_currentUser` মিলিয়ে দেওয়া, সুপার হলে
    অ্যাকাউন্ট-তালিকা রিফ্রেশ) ও `fetchAdminSessions(adminId, limit, onResult)`।
  - `AdminAccountsView.kt`-এর `AccountCard`: আদ্যক্ষর-বক্সের জায়গায় `AdminAvatar` (ছবি-সাপোর্ট)।
  - `AdminPanelScreen.kt`: টপ-বারের `actions`-এ ক্লাউড-সিঙ্ক আইকনের পাশে বর্তমান এডমিনের `AdminAvatar`
    (রিং-সহ, `testTag("admin_top_bar_profile_avatar")`) — ট্যাপে `showAdminProfileOverlay = true`,
    ফুলস্ক্রিন `Dialog(usePlatformDefaultWidth = false)`-এ `AdminProfileView` খোলে (`onDismissRequest`
    দিয়ে সিস্টেম-ব্যাক/বাইরে-ট্যাপেও বন্ধ হয়)। ওভারলে খোলার মুহূর্তে সুপার হলে `loadAdminAccounts()`
    (অ্যাকাউন্ট-ট্যাবে না গিয়েও সরাসরি প্রোফাইল খুললে তালিকা ফাঁকা না থাকে) — দ্বিতীয় `collectAsStateWithLifecycle()`
    সাবস্ক্রাইবার (`adminAccountsForProfile`) টপ-লেভেলে, কারণ বিদ্যমান `adminAccounts` কালেকশনটা ট্যাব
    ২৬-এর নিজস্ব `when` শাখার ভেতরে স্কোপড ছিল।

### সিদ্ধান্ত (এই সেশনে নেওয়া — ব্যবহারকারী দ্বারা আলাদা কনফার্ম হয়নি, আপত্তি থাকলে বদলাও)
- ফ্ল্যাগড এডমিন নিজের নাম/ছবি/bio বদলাতে পারবে কিনা — সার্ভার আটকায় না, আমিও UI-তে আটকাইনি (পাসওয়ার্ড বদল
  স্বাভাবিকভাবেই চলবে, ওটা আলাদা path)। **ব্যবহারকারীকে জিজ্ঞেস করা হয়েছে, উত্তর এখনো আসেনি** — চাইলে সার্ভারে
  `admin_profile_update`-এ `if v_me.flagged and v_target = v_me.id then raise ...` টাইপ গার্ড যোগ করা যাবে।
- পুরনো ছবির ফাইল bucket থেকে ডিলিট করা হয় না (নতুন ছবি আপলোডের পর) — ছোট ফাইল (≤২MB, কম্প্রেসড), ঝুঁকি/খরচ
  কম বলে এই সেশনের স্কোপে রাখা হয়নি।
- মকআপের "মোট অ্যাক্টিভিটি" সংখ্যা (`admins[].actions`) বাদ দেওয়া হয়েছে — পুরনো `admin_audit_logs`-এর
  RLS/অ্যাক্সেস-নিয়ম সেশন ৭.৫-এ বদলাতে পারে (দেখো সেশন ৫-এর নোট), তার আগে একটা নতুন কাউন্ট-RPC না বানানোই ভালো।
- ২FA টগল মকআপে ছিল কিন্তু কোনো ব্যাকএন্ড নেই — এই সেশনে বাদ দেওয়া হয়েছে (আধা-কাজ করা UI দেখানো ঠিক না)।
- প্রোফাইল-ওভারলে বন্ধ করলে (বাতিল-না-করে) আধা-লেখা ড্রাফট হারায় — মকআপেও কোনো "সংরক্ষণ ছাড়া বন্ধ করলে
  সতর্ক করবে" আচরণ ছিল না, তাই যোগ করা হয়নি।

### ⚠️ যা যাচাই হয়নি (সৎ তালিকা)
- **এই সেশনের কোনো Kotlin কখনো কম্পাইল হয়নি** (Gradle/SDK/kotlinc নেই এই এনভায়রনমেন্টে) — ব্র্যাকেট-ব্যালেন্স
  ও ইমপোর্ট হাতে-গ্রেপ করে মিলানো হয়েছে, কিন্তু নিশ্চিত না। সম্ভাব্য ঝুঁকির জায়গা: `AdminAvatar`-এর
  `photo: Any?` প্যারামিটারে `Uri?`/`String?` দুটোই Coil-এ পাস করা (কম্পাইল-টাইমে ঠিক থাকা উচিত, রানটাইমে
  `AsyncImage`-এর `model` রেজোলিউশন), `Modifier.align(Alignment.BottomEnd)`-এর নেস্টেড `Box` স্কোপ,
  `when { sessionList == null -> …; sessionList.isEmpty() -> … }`-এর smart-cast।
- bucket + RLS পলিসি শুধু **SQL-লেভেলে** যাচাই হয়েছে (bucket সেটিংস + policy-টেক্সট সরাসরি পড়ে); আসল
  আপলোড (client → Supabase Storage SDK → bucket, RLS `auth.uid()`-এর সাথে path-এর প্রথম ফোল্ডার মেলা)
  ডিভাইসে টেস্ট হয়নি।
- ছবি-ছাড়া/ছবি-সহ সংরক্ষণ, ছবি-সরানো, সুপার হিসেবে অন্যের প্রোফাইল বদলানো, ফ্ল্যাগড অ্যাকাউন্টে সংরক্ষণ,
  পাসওয়ার্ড-বদল ডায়ালগ, সাম্প্রতিক সেশন লিস্ট রেন্ডার — সব ডিভাইসে বাকি।

### যা টেস্ট করা উচিত (সেশন ৫-৬ শেষে একসাথে, ডিভাইসে — সেশন ৫-এর তালিকার পরে)
টপ-বারের ডান পাশে অবতার দেখা যায় (ছবি না থাকলে আদ্যক্ষর) → ট্যাপ করলে প্রোফাইল খোলে → নাম/পদবি/bio/ইমেইল
বদলে "সংরক্ষণ করুন" → টোস্ট + টপ-বার/তালিকায় সাথে সাথে প্রতিফলিত → ক্যামেরা-বাটনে ছবি বাছাই → প্রিভিউ দেখায়
→ সংরক্ষণে আপলোড হয়ে সব জায়গায় (টপ-বার, এডমিন-অ্যাকাউন্ট তালিকা, প্রোফাইল-কার্ড) নতুন ছবি দেখা যায় → "ছবি
সরান" → সংরক্ষণে আদ্যক্ষরে ফিরে যায় → সুপার লগইনে বাম কলামে সব এডমিন, অন্য একজন বেছে তার প্রোফাইল বদলানো
→ নন-সুপার নিজের প্রোফাইল ছাড়া কিছু দেখে না → ফ্ল্যাগড এডমিনের প্রোফাইলে ব্যানার দেখা যায় → পাসওয়ার্ড
পরিবর্তনে ভুল বর্তমান-পাসওয়ার্ডে এরর, সঠিকে সফল হয়ে পরের লগইনে নতুন পাসওয়ার্ড কাজ করে → সাম্প্রতিক সেশন
তালিকায় বর্তমান ডিভাইস "এখন সক্রিয়" দেখায়।

### ✅ রিয়েল-ডিভাইস টেস্ট (ব্যবহারকারী, ২০২৬-০৯-২৫) — সেশন ১-৬ পর্যন্ত ব্যবহারকারী কর্তৃক ভেরিফায়েড

ব্যবহারকারী রিয়েল ডিভাইসে টেস্ট করে কনফার্ম করেছেন সেশন ১-৬ পর্যন্ত ফিচার কাজ করছে। এই টেস্টে দুটো অবজারভেশন এসেছে:

1. **প্রোফাইল স্ক্রিনের ডিজাইন আনপ্রফেশনাল ছিল — এই সেশনে ফিক্স করা হলো।** স্ক্রিনশটে দেখা গেছে
   নাম-কলাম (Row/SpaceBetween-এ রোল-ব্যাজের পাশাপাশি) জায়গা না পেয়ে "Sup/port/Ma/nag/er"-এর মতো
   অক্ষর-বাই-অক্ষর ভেঙে যাচ্ছিল। `AdminProfileView.kt`-এর `AdminProfileCard` হেডার সম্পূর্ণ
   রিডিজাইন করা হয়েছে — ফুল-ব্লিড গ্র্যাডিয়েন্ট ব্যান্ড, অ্যাভাটার/ক্যামেরা-বাটন/নাম/পদবি/রোল-ব্যাজ
   সব উলম্বভাবে কেন্দ্রে সাজানো (আগের পাশাপাশি Row-লেআউটের বদলে), নাম/পদবিতে `maxLines = 1` +
   ellipsis সেফটি-নেট যোগ করা হয়েছে যাতে ভবিষ্যতে কোনো লম্বা নাম/পদবিতেও একই সমস্যা না হয়। অ্যাভাটার
   সাইজ ৮৪dp থেকে ৯২dp, ক্যামেরা-বাটনে সাদা বর্ডার-রিং, রোল-ব্যাজে সূক্ষ্ম বর্ডার যোগ — সামগ্রিক
   প্রিমিয়াম-কার্ড লুক। **শুধু UI/লেআউট বদল — কোনো ডেটা/RPC/লজিক ছোঁয়া হয়নি, সেশন ৬-এর বাকি সব
   অপরিবর্তিত।** ⚠️ এই ফিক্সও কম্পাইল/ডিভাইস-টেস্ট হয়নি (এই এনভায়রনমেন্টে Gradle/SDK নেই, ব্র্যাকেট-
   ব্যালেন্স হাতে-গ্রেপ করে মিলানো হয়েছে) — পরের ডিভাইস-টেস্টে যাচাই করা দরকার।
2. **ফ্ল্যাগড/রোল-রেস্ট্রিক্টেড এডমিন এখনো সব মেনু/অ্যাকশন করতে পারছে (যেমন শুধু-KYC রোলের এডমিন সব
   মেনু দেখছে) — এটা নতুন বাগ না, প্রত্যাশিতই।** সেশন ১-৬-এ শুধু রোল/পারমিশন/ফ্ল্যাগ ডেটা তৈরি ও
   সেভ করার ব্যবস্থা হয়েছে; ২৫টা বিদ্যমান স্ক্রিনে সেই ডেটা অনুযায়ী মেনু-ফিল্টার ও অ্যাকশন-লক
   বসানো (সেশন ৭.০-৭.৯) এখনো শুরুই হয়নি — নিচের সেশন ৭ সেকশন দ্রষ্টব্য।

### ✅ সেশন ৭-পূর্ব সব প্রশ্ন কনফার্মড (ব্যবহারকারী, ২০২৬-০৯-২৫) — বিস্তারিত মাস্টার প্রম্পটে যোগ হয়েছে
1. **`admin-reset-user-password` নিরাপত্তা-ফাঁক** — "যেভাবে করলে নিরাপত্তা ঠিক থাকবে ও প্রফেশনাল সিস্টেম
   হবে" (ব্যবহারকারী Claude-এর প্রস্তাবিত সমাধানে সম্মত) — টার্গেট নিজে এডমিন হলে caller সুপার না হলে
   ব্লক + সাধারণ ইউজার টার্গেটে `users:users:reset_password` পারমিশন/flagged-চেক Edge Function-এও
   independently verify। বিস্তারিত: মাস্টার প্রম্পট সেশন ৭, সাব-স্টেপ **৭.১.১** (নতুন)।
2. **`factory_reset`/`cleanup_commission`/`system:explorer:*`/`refund_debug:*`** — **শুধু সুপার
   অ্যাডমিন** (গ্র্যানুলার পারমিশন না, সরাসরি `isSuper` হার্ড-চেক)। বিস্তারিত: মাস্টার প্রম্পট সেশন ৭,
   সাব-স্টেপ **৭.৪** ও **৭.৬** (আপডেটেড)।
3. **`finance:extra_charges:mark_settled`** — ক্যাটালগ থেকে বাদ (মূল প্রশ্নের উত্তরে দেখা গেছে এটা
   সবসময় অন্য অ্যাকশনের অটোমেটিক সাইড-ইফেক্ট, কোনো এডমিন সরাসরি চাপে না)। **সাথে এই আলোচনা থেকেই নতুন,
   বড় বিজনেস-রুল বেরিয়েছে (ব্যবহারকারীর অনুরোধ, ২০২৬-০৯-২৫):** PENDING extra bill থাকা অবস্থায়
   সলভার রিলিজ-রিকোয়েস্ট করতে পারবে না; dispute-এ গেলে admin নিজে Accept/Cancel করতে পারবে, তারপরই
   release/split; refund এই গার্ডের বাইরে। বিস্তারিত সাব-সাব-স্টেপসহ: মাস্টার প্রম্পট সেশন ৭, সাব-স্টেপ
   **৭.২.১** (নতুন, ৭.২-এর ভেতরে ক-খ-গ-ঘ ধাপে ভাঙা) — এটা ৭.২ (আর্থিক)-এর সাথেই একই সাব-স্টেপে করা হবে,
   আলাদা সেশন না, কারণ `AdminEscrowView`/`AdminAdditionalChargesView`/`AdminDisputeCenterView` স্পর্শ
   করে যা যথাক্রমে ৭.২ ও ৭.৫-এর স্কোপে পড়ে — ৭.২.১-এর (ক)/(খ)/(গ)/(ঘ) ধাপগুলো `AdminDisputeCenterView`
   ছোঁবে বলে ৭.৫ শুরুর আগেই ৭.২.১ শেষ করতে হবে (dependency)।
4. **ফ্ল্যাগড এডমিনের নিজের প্রোফাইল** — বদলাতে **পারবে না** (পাসওয়ার্ড বদল ছাড়া)। বিস্তারিত: মাস্টার
   প্রম্পট সেশন ৬-এর এন্ট্রিতে নতুন বুলেট, বাস্তবায়ন সেশন **৭.১**-এ (`admin_profile_update` RPC গার্ড)।

### পরবর্তী: সেশন ৭.০ ✅ সম্পন্ন (নিচে দেখুন) — এখন ৭.১ (+৭.১.১)-এর অপেক্ষায়
উপরের ৪টা প্রশ্নই কনফার্মড ছিল। ৭.০ (শেয়ার্ড `AdminSession.canAct()` ফাউন্ডেশন) কোড সম্পন্ন — নিচের
"সেশন ৭.০" এন্ট্রি দ্রষ্টব্য। প্রতিটা সাব-স্টেপ (৭.০ ✅ → ৭.১ (+৭.১.১) → ৭.২ (+৭.২.১) → ৭.৩ → ৭.৪ → ৭.৫ →
৭.৬ → ৭.৭ → ৭.৮ → ৭.৯) শেষে থেমে কনফার্মেশন নিয়ে পরেরটায়।

**সেশন ৭ শেষ হওয়ার পর:** সেশন ৮ — এডমিন ফোন নম্বর এডিট গ্যাপ ফিক্স (বিস্তারিত মাস্টার প্রম্পটের নতুন
"সেশন ৮" এন্ট্রিতে) — ব্যবহারকারীর রিয়েল-ডিভাইস টেস্টে ধরা পড়েছে (২০২৬-০৯-২৫): সুপার অ্যাডমিনও এখন
কারো ফোন বদলাতে পারেন না, কোনো এডিট RPC নেই। **সেশন ৯** — কোল্ড-স্টার্ট সেশন-পার্সিস্টেন্স + রিফ্রেশ
(বিস্তারিত মাস্টার প্রম্পটের নতুন "সেশন ৯" এন্ট্রিতে) — ব্যবহারকারীর অনুরোধ (২০২৬-০৯-২৫): অ্যাপ কিল/
ক্লোজ করলে এডমিন লগআউট হবে না, সেশন ক্যাশে থাকবে, অ্যাপ আবার খুললে সব ডেটা রিয়েল-টাইম ফিচারসহ নতুন
করে সিঙ্ক হবে।
## সেশন ৭.০ — শেয়ার্ড `AdminSession.canAct()` ফাউন্ডেশন — ✅ কোড সম্পন্ন (২০২৬-০৯-২৫), ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি

> শুধু ফাউন্ডেশন — কোনো বিদ্যমান ২৫টা `AdminXxxView.kt` স্ক্রিন এই সাব-স্টেপে ছোঁয়া হয়নি (ইচ্ছাকৃতভাবে,
> স্কোপ সংকীর্ণ রাখতে + ঝুঁকি কমাতে)। স্ক্রিন-বাই-স্ক্রিন ওয়্যারিং ৭.১ থেকে শুরু। কোনো migration/DB পরিবর্তন
> লাগেনি (server-side `admin_can_act` সেশন ১ থেকেই আছে, নিচে দ্রষ্টব্য)।

### ✅ যা তৈরি হয়েছে
1. **`data/security/AdminSession.kt`-এ যোগ (বিদ্যমান কিছু বদলায়নি, শুধু সংযোজন):**
   - `fun adminCanAct(actionKey: String, account: AdminAccountInfo?): Boolean` — pure ফাংশন (কোনো
     গ্লোবাল-স্টেট পড়ে না, `AdminRolePreviewPanel.kt`-এর `previewActionAllowed`-এর স্টাইলে): অ্যাকাউন্ট
     null/নিষ্ক্রিয় → false; হার্ড-সুপার-অনলি কী হলে শুধু `isSuper`; সুপার (হার্ড-সুপার-অনলি না হলে) →
     সবসময় true; নন-সুপার + flagged + `view` ছাড়া অন্য অ্যাকশন → false; বাকি → রোলের `permissions`-এ
     কী আছে কিনা।
   - `ADMIN_HARD_SUPER_ONLY_PREFIXES`/`ADMIN_HARD_SUPER_ONLY_EXACT_KEYS` + `isHardSuperOnlyActionKey(...)` —
     সেশন ৭-পূর্ব কনফার্মেশন #২ প্রয়োগ: `system:explorer:*`/`system:refund_debug:*` (পুরো স্ক্রিন, view-সহ)
     এবং `config:settings:factory_reset`/`config:settings:cleanup_commission` — রোলের `permissions`-এ
     এই কী টিক করা থাকলেও উপেক্ষা করে, শুধু `isSuper` দিয়ে সিদ্ধান্ত।
   - `AdminSession.canAct(actionKey: String): Boolean` (সিঙ্গলটন `current?.account` দিয়ে
     `adminCanAct` ডাকে) + `AdminSession.canView(groupId, itemId): Boolean` শর্টহ্যান্ড
     (`"$groupId:$itemId:view"`)। সেশন ৭.১-৭.৮-এর প্রতিটা স্ক্রিন/বাটন এই দুটো ফাংশনই ব্যবহার করবে।
2. **নতুন ফাইল `ui/components/AdminAccessLockedState.kt`** — শেয়ার্ড "এই মেনুতে অ্যাক্সেস নেই" খালি-স্টেট
   কম্পোজেবল, মকআপ ১-এর `.sim-locked` স্টাইলের প্রোডাকশন সংস্করণ (বর্ডার-কার্ড + লক-আইকন + শিরোনাম + বার্তা,
   কাস্টমাইজযোগ্য টেক্সট)। সেশন ৭.১-৭.৮-এ যখন `AdminSession.canView(...)` false হবে তখন স্ক্রিনের বাকি
   কন্টেন্টের বদলে এটা বসানো হবে (ডেটা-লোড এড়িয়ে যাওয়া caller-এর দায়িত্ব)। ফ্ল্যাগড-অ্যাকশন-লকের জন্য আলাদা
   কোনো কম্পোজেবল বানানো হয়নি — প্রতিটা অ্যাকশন-বাটন নিজেই `enabled = AdminSession.canAct(key)` দিয়ে গেট হবে
   (মকআপেও বাটন শুধু disabled দেখায়, আলাদা বার্তা-বক্স না)।
3. **নতুন টেস্ট `app/src/test/.../data/security/AdminSessionCanActLogicTest.kt`** (১৩টা, কখনো চালানো হয়নি) —
   pure-লজিক (নেই-অ্যাকাউন্ট/নিষ্ক্রিয়/রোল-পারমিশন/flagged/সুপার/হার্ড-সুপার-অনলি-ওভাররাইড-সহ-বিদ্যমান-কী)
   ও সিঙ্গলটন-ওয়্যারিং (`AdminSession.canAct`/`canView`) দুটোই কভার করে।

### ⚠️ ইচ্ছাকৃত অসামঞ্জস্য/ফ্ল্যাগ — ৭.৪/৭.৬-এ মনে রাখতে হবে
- **সার্ভার-সাইড `admin_can_act(p_action_key, p_uid)` RPC (সেশন ১)** এখনো হার্ড-সুপার-অনলি ওভাররাইড জানে
  না (`system:explorer`/`refund_debug`/`factory_reset`/`cleanup_commission`-কে সাধারণ গ্র্যানুলার-কী
  হিসেবেই ট্রিট করে) — এটা তৈরি হয়েছিল এই সিদ্ধান্তের আগে। **কোনো নিরাপত্তা-রিগ্রেশন এখনই না** (SQL কমেন্টে
  লেখা আছে "এখনো কোনো বিদ্যমান RPC এটা ডাকে না" — সার্ভার-সাইড এই ফাংশন এখনো কোথাও enforcement-এ ব্যবহৃত
  হচ্ছে না), কিন্তু সাব-স্টেপ ৭.৪/৭.৬-এ (যখন এই action-গুলোর জন্য সার্ভার-সাইড hardening যোগ করার কথা) নতুন
  migration দিয়ে এই ফাংশনেও একই ওভাররাইড যোগ করতে হবে যাতে client ও server নিয়ম না মেলার সুযোগ না থাকে।
- **`AdminRolePreviewPanel.kt`-এর `previewActionAllowed`/`previewItemVisible` (সেশন ৩)**-ও এখনো এই হার্ড-
  সুপার-অনলি ওভাররাইড জানে না — ইচ্ছাকৃতভাবে এই সাব-স্টেপে ছোঁয়া হয়নি (স্কোপ সংকীর্ণ রাখতে)। এখনই কোনো
  ভুল প্রিভিউ দেখানোর সুযোগ নেই যেহেতু রোল-এডিটর UI এখনো এই কী-গুলোকে সাধারণ চেকবক্স হিসেবেই দেখায় (৭.৪/
  ৭.৬-এ বদলানোর কথা) — কিন্তু দুটো ফিক্স (RPC + প্রিভিউ-প্যানেল) একসাথে ৭.৪/৭.৬-এ করা উচিত, নইলে প্রিভিউ
  বাস্তব গেটিং থেকে ভিন্ন কিছু দেখাতে পারে।

### ⚠️ যা যাচাই হয়নি (সৎ তালিকা)
- **কোনো Kotlin কম্পাইল/রান হয়নি** (এই এনভায়রনমেন্টে Gradle/SDK/kotlinc নেই, আগের সব সেশনের মতোই) —
  ব্র্যাকেট/প্যারেন-ব্যালেন্স স্ক্রিপ্ট দিয়ে ৩টা ফাইলেই যাচাই করা হয়েছে (সব মিলেছে), ইমপোর্ট বিদ্যমান
  কনভেনশন (`AdminAvatar.kt`/`AdminUserLookupView.kt`-এর `EmptyStateView`) থেকে গ্রেপ করে মেলানো।
- নতুন টেস্ট (১৩টা) কখনো চালানো হয়নি — প্রথম CI/Android Studio রানে পাস/ফেল যাচাই হবে।
- `AdminAccessLockedState` এখনো কোথাও কল হয় না (কোনো স্ক্রিন এখনো ব্যবহার করে না) — তাই ভিজ্যুয়ালি ডিভাইসে
  দেখা যায়নি; প্রথম আসল ব্যবহার ৭.১-এ।
- `AdminSession.canAct`/`canView` এখনো কোথাও কল হয় না (কোনো বিদ্যমান স্ক্রিন/বাটন এখনো এটা ব্যবহার করে না) —
  তাই বর্তমান অ্যাপ-আচরণে কোনো পরিবর্তন নেই, এই সাব-স্টেপে কোনো রিগ্রেশন-ঝুঁকি নেই।

### যা টেস্ট করা উচিত (এই সাব-স্টেপে, ডিভাইসে)
১) বিল্ড হয় কিনা (এটাই প্রথম আসল কম্পাইল-চেক এই নতুন ফাইল দুটোর জন্য); ২) অ্যাপের বিদ্যমান কোনো আচরণ না
বদলানো (এই সাব-স্টেপ কিছু ওয়্যার করেনি, তাই আগের মতোই সব কাজ করা উচিত); ৩) (ঐচ্ছিক, দ্রুত sanity-check
চাইলে) `AdminSessionCanActLogicTest`-এর ১৩টা টেস্ট Android Studio-তে রান করে সব পাস কিনা।

### ✅ নতুন গ্যাপ ধরা পড়েছে + কনফার্মড — ৭.১.২ (ব্যবহারকারী, ২০২৬-০৯-২৫, দুই দফা আলোচনায় ফাইনালাইজড)
Usergon (ইউজার) মেনুর রোল-চেঞ্জ ডায়ালগে "ADMIN" অপশন বাছলে শুধু লেগ্যাসি `public.users.role='ADMIN'`
সেট হয় — কোনো `admin_accounts` রো তৈরি হয় না, তাই সেই ব্যক্তি নতুন মাল্টি-অ্যাডমিন প্যানেলে লগইনই করতে
পারে না (`NOT_AN_ADMIN`)। **ব্যবহারকারী কনফার্মড সমাধান (পথ ২ — আলাদা লগইন, নিরাপদ):** "ADMIN" অপশন
সরিয়ে "বানাও অ্যাডমিন" অ্যাকশন — বিদ্যমান `admin_account_create` RPC-ই পুনর্ব্যবহার (কোনো DB/RPC বদল
না, `PHONE_IN_USE_BY_USER` গার্ড অক্ষত)। টার্গেট ইউজারের পুরনো `public.users` row (balance/reputation/
active jobs/escrow/KYC সহ) **সম্পূর্ণ অক্ষত** থাকে — নতুন এডমিন সম্পূর্ণ আলাদা auth-identity/ফোন/পাসওয়ার্ড
দিয়ে তৈরি হয় (এক ফোনে দুটো ভূমিকা করা যাবে না)।

**দ্বিতীয় দফার কনফার্মেশন (একই দিন, ফলো-আপ প্রশ্নে):**
- **পদবি vs রোল স্পষ্টীকরণ + সংশোধনী:** পদবি ফ্রি-টেক্সট থাকবে (রোলের সাথে অবাঁধা), কিন্তু আগের
  খসড়ায় "পদবি প্রি-ফিল হবে" যেটা লেখা হয়েছিল সেটা **ভুল ছিল** — `users` টেবিলে পদবি কলামই নেই, তাই
  প্রি-ফিল শুধু **নাম + ইমেইল**। রোল (`role_id`) Role Management-এ তৈরি যত রোল আছে (যেমন সত্যিই
  "KYC ম্যানেজার" নামে বানানো থাকলে) তার ড্রপডাউন থেকে explicit select — prefill না।
- **নেভিগেশন কনফার্মড:** ট্যাব ২৬-এ যাওয়া না, **একই পেজে (`AdminUsersView.kt`) in-place বটম-শিট
  পপআপ** — বিদ্যমান রোল-চেঞ্জ ডায়ালগের `BottomSlideAlertDialog` ডিজাইন-প্যাটার্নেই।
- **পারমিশন-গেটিং কনফার্মড (সাধারণ নীতি, ইতিমধ্যেই ৭.০-৭.৯-এর মূল ডিজাইন — এখানে স্পষ্ট করা হলো):**
  কোনো রোলে নির্দিষ্ট পারমিশন টিক না থাকলে সেই অ্যাকশন/বাটন **hidden** থাকবে (disabled না) — "বানাও
  অ্যাডমিন" বাটনও এই নিয়মেই গেট হবে, এবং যেহেতু `admin_account_create` RPC সার্ভার-সাইডে শুধু
  সুপার-অ্যাডমিন caller নেয়, এই অ্যাকশনকে ৭.০-এর hard-super-only তালিকায়ও যোগ করা হবে।
- **সুপার অ্যাডমিন কখনো তৈরি/অ্যাসাইন করা যাবে না (গুরুত্বপূর্ণ কনফার্মেশন) — ইতিমধ্যে DB-তে ৩ স্তরে
  গ্যারান্টিড, নতুন কোড লাগবে না:** `admin_account_create` ও `admin_account_set_role` দুটোতেই
  `CANNOT_ASSIGN_SUPER_ROLE` গার্ড (কাউকে সুপার বানানো/পরে-আপগ্রেড দুটোই ব্লকড), `admin_role_upsert`
  কখনো `is_super=true` রোল বানাতেই দেয় না (`is_super` প্যারামিটারই নেয় না)। ক্লায়েন্ট-সাইডে শুধু UX
  পালিশ বাকি: "বানাও অ্যাডমিন" ফর্মের রোল-ড্রপডাউন থেকে সুপার রোল শুরুতেই বাদ রাখা।

বিস্তারিত (সব কটা পয়েন্ট): মাস্টার প্রম্পট সেশন ৭, সাব-স্টেপ **৭.১.২** (নতুন, ৭.১-এর ভেতরে)।

### পরবর্তী: সেশন ৭.১ (+৭.১.১ +৭.১.২) — ইউজার ব্যবস্থাপনা গ্রুপ — ব্যবহারকারীর কনফার্মেশনের অপেক্ষায়
`AdminUserLookupView.kt`/`AdminUsersView.kt`/`AdminKycView.kt`/`AdminSolverQuotaView.kt` — view-গেট
(`AdminAccessLockedState` দিয়ে) + প্রতিটা অ্যাকশন-বাটনে `AdminSession.canAct(...)` + flagged-নিজের-প্রোফাইল
গার্ড (`admin_profile_update` RPC, সেশন ৬-এর পেন্ডিং আইটেম) + `admin-reset-user-password` Edge Function
নিরাপত্তা-ফিক্স (৭.১.১) + রোল-চেঞ্জ ডায়ালগের "ADMIN"-অপশন → "বানাও অ্যাডমিন" ফ্লো (৭.১.২, বিস্তারিত
মাস্টার প্রম্পটে)। **এখনো কোনো কোড লেখা হয়নি — শুধু প্ল্যান/ডকুমেন্টেশন এন্ট্রি এই আপডেটে যোগ হয়েছে।**

## সেশন ৭.১ — ইউজার ব্যবস্থাপনা গ্রুপ — 🔶 আংশিক (২০২৬-০৯-২৫): `AdminSolverQuotaView.kt` সম্পন্ন, বাকি ৩টা স্ক্রিন + ৭.১.১ + ৭.১.২ বাকি

> **স্কোপ-সিদ্ধান্ত (এই সাব-স্টেপেই নেওয়া হয়েছে):** ৭.১-এ ৪টা স্ক্রিন (মোট ~৭২০০ লাইন) + Edge Function
> ফিক্স + নতুন "বানাও অ্যাডমিন" ফ্লো — একসাথে করলে রিভিউ করা কঠিন হয়ে যেত, তাই ৭.১-কেই আরও ছোট
> সাব-সাব-স্টেপে ভাগ করা হলো (প্রতিটার পর zip + কনফার্মেশন, ঠিক ৭.x-এর মূল নিয়মেই):
> ৭.১(ক) `AdminSolverQuotaView.kt` [এই এন্ট্রি], ৭.১(খ) `AdminUsersView.kt`, ৭.১(গ) `AdminKycView.kt`,
> ৭.১(ঘ) `AdminUserLookupView.kt`, তারপর ৭.১.১ (Edge Function ফিক্স), তারপর ৭.১.২ ("বানাও অ্যাডমিন")।

### ✅ ৭.১(ক) `AdminSolverQuotaView.kt` — কোড সম্পন্ন, ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি
- **view-গেট:** ফাংশনের একদম শুরুতে (কোনো composable কল হওয়ার আগেই) —
  `if (!AdminSession.canView("users", "solver_quota")) { AdminAccessLockedState(); return }`।
- **অ্যাকশন-বাটন গেটিং (২টা, `AdminPermissionCatalog`-এ আগে থেকেই থাকা কী দিয়ে):**
  - "এই সলভারের কোটা ০ করুন" Button → `enabled = AdminSession.canAct("users:solver_quota:reset_quota")`
  - "এই সলভারের সাইকেল রিসেট করুন" OutlinedButton → `enabled = AdminSession.canAct("users:solver_quota:reset_miss_cycle")`
- নতুন ইমপোর্ট: `com.example.ui.components.AdminAccessLockedState`, `com.example.data.security.AdminSession`।
- **যা বদলায়নি:** সুপার অ্যাডমিনের (বিদ্যমান একমাত্র সিড অ্যাকাউন্ট) আচরণে কোনো পরিবর্তন নেই যেহেতু
  `adminCanAct` সুপারের জন্য সবসময় true (হার্ড-সুপার-অনলি কী না হলে) — শুধু নন-সুপার/ফ্ল্যাগড অ্যাকাউন্টের
  ক্ষেত্রে গেটিং কার্যকর হবে, যা এখনো তৈরি হয়নি (এখনো একমাত্র সুপার অ্যাকাউন্টই আছে)।
- **যাচাই হয়নি (সৎ তালিকা):** কোনো Kotlin কম্পাইল/রান হয়নি (এই এনভায়রনমেন্টে Gradle/SDK নেই, আগের সব
  সেশনের মতোই) — ব্র্যাকেট/প্যারেন-ব্যালেন্স স্ক্রিপ্ট দিয়ে ফাইলটা যাচাই করা হয়েছে (মিলেছে: braces ১৬৩/১৬৩,
  parens ৫১৩/৫১৩), ইমপোর্ট বিদ্যমান কনভেনশন (`AdminAccessLockedState.kt`-এর হেডার কমেন্টের প্যাটার্ন) থেকে
  মিলিয়ে বসানো। প্রথম আসল ব্র্যাকেট/টাইপ-চেক পরের Android Studio/CI বিল্ডে।
- **টেস্ট করা উচিত (ডিভাইসে):** ১) বিল্ড হয় কিনা; ২) সুপার অ্যাডমিন দিয়ে লগইন করে এই স্ক্রিনে আগের মতোই
  সব কাজ করছে কিনা (কোনো রিগ্রেশন না); ৩) (নন-সুপার/ফ্ল্যাগড টেস্ট অ্যাকাউন্ট এখনো নেই বলে) এই মুহূর্তে
  গেটিং সরাসরি ডিভাইসে verify করা যাচ্ছে না — সেশন ৪-এর `AdminAccountsView` দিয়ে একটা টেস্ট নন-সুপার/
  ফ্ল্যাগড অ্যাকাউন্ট বানিয়ে যাচাই পরবর্তীতে করা উচিত (৭.৯-এর শেষ QA পাসেও এটা কভার হবে)।

### ✅ ৭.১(খ) `AdminUsersView.kt` — কোড সম্পন্ন, ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি (২০২৬-০৯-২৫)
- **view-গেট:** ফাংশনের শুরুতে (কোনো composable কল হওয়ার আগে) —
  `if (!AdminSession.canView("users", "users")) { AdminAccessLockedState(); return }`।
- **"More Actions" ড্রপডাউনের ৮টা `DropdownMenuItem`-ই আলাদাভাবে গেটেড (`enabled = AdminSession.canAct(...)`):**
  ব্যান/আনব্যান → `users:users:ban`; রেস্ট্রিক্ট → `users:users:restrict`; ভেরিফাইড ব্যাজ →
  `users:users:verified_badge`; ব্যালেন্স সমন্বয় → `users:users:balance_adjust`; রোল পরিবর্তন →
  `users:users:change_role`; রেপুটেশন সমন্বয় → `users:users:score_adjust`; পাসওয়ার্ড রিসেট →
  `users:users:reset_password`; মুছে ফেলুন → `users:users:delete`। কোনো ডায়ালগ (ban/restrict/balance/
  role/reputation/password-reset/delete confirm) নিজে ছোঁয়া হয়নি — মেনু-আইটেমের `enabled=false`-ই
  যথেষ্ট, ডায়ালগ খোলার পথই বন্ধ থাকে।
- **"বানাও অ্যাডমিন" রোল-চেঞ্জ ডায়ালগের ভেতরের "ADMIN" অপশন সরানো/নতুন ফ্লো এখনো করা হয়নি** — সেটা
  ৭.১.২-এ, এই সাব-সাব-স্টেপে না (স্কোপ আলাদা রাখা হয়েছে যাতে রিভিউ সহজ থাকে)।
- নতুন ইমপোর্ট: `AdminAccessLockedState`, `AdminSession` (৭.১(ক)-এর মতোই)।
- **যাচাই হয়নি:** Kotlin কম্পাইল/রান হয়নি — ব্র্যাকেট/প্যারেন-ব্যালেন্স স্ক্রিপ্টে মিলেছে (braces ৩০৬/৩০৬,
  parens ৭৬৩/৭৬৩)। `DropdownMenuItem`-এ `enabled` প্যারামিটার Material3-এ স্ট্যান্ডার্ড বলে ধরে নেওয়া
  হয়েছে (এই ফাইলেই আগে থেকে অন্য কোথাও ব্যবহৃত হয়েছে কিনা আলাদা করে গ্রেপ করা হয়নি — প্রথম আসল
  কম্পাইল-চেকে ধরা পড়বে যদি ভুল হয়ে থাকে)।

### ✅ ৭.১(গ) `AdminKycView.kt` — কোড সম্পন্ন, ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি (২০২৬-০৯-২৫)
- **view-গেট:** ফাংশনের শুরুতে — `if (!AdminSession.canView("users", "kyc")) { AdminAccessLockedState(); return }`।
- **অ্যাকশন-এন্ট্রি-পয়েন্ট গেটেড (৬টা বাটন/মেনু-আইটেম, `enabled = AdminSession.canAct(...)`):**
  - পেন্ডিং ট্যাব: "তথ্য এডিট করুন" → `users:kyc:edit`; "অনুমোদন করুন" → `users:kyc:approve`;
    "বাতিল করুন" (রিজেক্ট-ডায়ালগ ট্রিগার) → `users:kyc:reject`
  - ভেরিফাইড ট্যাবের ⋮ মেনু: "তথ্য এডিট করুন" → `users:kyc:edit`; "ভেরিফিকেশন বাতিল করুন (Revoke)" →
    `users:kyc:revoke`
  - রিজেক্টেড ট্যাব: "পেন্ডিং-এ পাঠান" → `users:kyc:reset_pending`
  - ফ্লোটিং "বাল্ক অনুমোদন" বার → `users:kyc:approve` (single approve-এর সাথে একই কী, ক্যাটালগ-কমেন্টে
    আগে থেকেই নোট করা ছিল যে `onApproveKyc`/`onBulkApprove` একই অ্যাকশন)
  - "ডকুমেন্ট দেখুন" বাটন/মেনু-আইটেম গেট করা হয়নি (view-এর অংশ, স্ক্রিন view-গেটের আওতায় আগে থেকেই)
- ডায়ালগ (reject/revoke/edit/bulk-approve confirm) নিজে ছোঁয়া হয়নি — আগের দুই স্ক্রিনের একই যুক্তি।
- নতুন ইমপোর্ট: `AdminAccessLockedState`, `AdminSession`।
- **যাচাই হয়নি:** কম্পাইল/রান হয়নি — ব্র্যাকেট/প্যারেন-ব্যালেন্স মিলেছে (braces ৩৬৮/৩৬৮, parens ৯৭৭/৯৭৭)।

### ✅ ৭.১(ঘ) `AdminUserLookupView.kt` — কোড সম্পন্ন, ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি (২০২৬-০৯-২৫)
- **view-গেট:** ফাংশনের শুরুতে (কোনো composable কল হওয়ার আগে) —
  `if (!AdminSession.canView("users", "user_search")) { AdminAccessLockedState(); return }`।
- **অ্যাকশন-বাটন গেটিং — `AdminPermissionCatalog.kt`-এর হেডার-কমেন্টের কনফার্মড নিয়ম অনুযায়ী
  (Lookup-এর নিজস্ব কী না, প্রতিটা তার আসল ডোমেইনের কী দিয়ে গেটেড, `enabled = AdminSession.canAct(...)`):**
  - "অ্যাডমিন অ্যাকশন ও নিয়ন্ত্রণ" প্যানেলের ৯টা বাটন: ব্যান → `users:users:ban`; রেস্ট্রিক্ট →
    `users:users:restrict`; ব্যালেন্স → `users:users:balance_adjust`; রোল → `users:users:change_role`;
    রেপুটেশন → `users:users:score_adjust`; পাসওয়ার্ড → `users:users:reset_password`; মুছে ফেলুন →
    `users:users:delete`; KYC অনুমোদন → `users:kyc:approve`; KYC প্রত্যাহার → `users:kyc:revoke`।
  - `ProblemItemLookupCard`-এর "স্ট্যাটাস" বাটন → `work:problems:update_status`; ডিলিট
    `IconButton` → `work:problems:delete`। ("বিড"/"চ্যাট" বাটন গেট করা হয়নি — এগুলো
    ক্যাটালগে কোনো নির্দিষ্ট অ্যাকশন-কী না, দেখার/নেভিগেশনের অংশ, ঠিক ডকুমেন্ট-দেখুন বাটনের মতোই।)
  - `RatingLookupCard`-এর ডিলিট `IconButton` (সরাসরি `onDelete` কল করে, কোনো কনফার্ম-ডায়ালগ নেই) →
    `moderation:reviews:delete`।
  - `SolverFinancialDetailsCard`-এর Approve/Reject বাটন → `finance:withdrawals:approve` /
    `finance:withdrawals:reject`।
- **⚠️→✅ পর্যবেক্ষণ ও সাথে সাথে ফিক্স (ব্যবহারকারীর প্রশ্নে ধরা পড়ে, একই দিনে ফিক্স করা হলো):**
  কোড-গ্রেপে ধরা পড়েছিল `kycRejectTargetUser` (KYC রিজেক্ট ডায়ালগ) ও `problemForBudgetEdit`
  (বাজেট-এডিট ডায়ালগ) — দুটোই state ভ্যারিয়েবল ও ডায়ালগ-কম্পোজেবল হিসেবে আগে থেকেই বাস্তবায়িত
  ছিল, কিন্তু এই স্ক্রিনে কোনো বাটন এই state সেট করত না — তাই Lookup স্ক্রিন থেকে এই দুটো ফ্লো
  ট্রিগার করা যেত না (dead UI hook; ফিচার নিজে ভাঙা ছিল না — KYC reject `AdminKycView.kt`-তে ও
  budget edit `AdminProblemsView.kt`-তে ঠিকই কাজ করত, শুধু Lookup থেকে পথ ছিল না)। **স্পষ্ট করে
  বলে রাখা ভালো: `AdminKycView.kt`/`AdminProblemsView.kt`-এর ভেতরের এই দুটো ফ্লো এই সাব-স্টেপে
  একদম ছোঁয়া হয়নি — শুধু এখানে, `AdminUserLookupView.kt`-এ, নতুন এন্ট্রি-পয়েন্ট যোগ হয়েছে।**
  এই সাব-স্টেপেই ফিক্স করা হলো:
  - "অ্যাডমিন অ্যাকশন" প্যানেলে (pending KYC অবস্থায়) "KYC অনুমোদন"-এর পাশে নতুন "KYC বাতিল"
    `OutlinedButton` → `kycRejectReason = ""; kycRejectTargetUser = user` → গেটেড
    `enabled = AdminSession.canAct("users:kyc:reject")`।
  - `ProblemItemLookupCard`-এর অ্যাকশন-রোতে "স্ট্যাটাস"-এর পাশে নতুন "বাজেট"
    `OutlinedButton` → `problemForBudgetEdit = problem` → গেটেড
    `enabled = AdminSession.canAct("work:problems:update_budget")`।
  - দুটোরই বিদ্যমান ডায়ালগ-ইমপ্লিমেন্টেশন (`viewModel.adminRejectKyc(...)`,
    `AdminEditBudgetDialog`/`viewModel.adminUpdateProblemBudget(...)`) অপরিবর্তিত — শুধু
    এন্ট্রি-পয়েন্ট বাটন যোগ হলো, নতুন কোনো dialog/RPC/ViewModel ফাংশন লাগেনি।
- ডায়ালগ (ban/restrict/balance/role/reputation/password-reset/delete/kyc-revoke/withdrawal-approve/
  withdrawal-reject confirm) নিজে ছোঁয়া হয়নি — আগের তিন স্ক্রিনের একই যুক্তি (এন্ট্রি-পয়েন্ট বন্ধ
  থাকলে ডায়ালগ খোলার পথই নেই)।
- নতুন ইমপোর্ট: `AdminAccessLockedState`, `AdminSession` (আগের তিনটার মতোই)।
- **যাচাই হয়নি:** Kotlin কম্পাইল/রান হয়নি (এই এনভায়রনমেন্টে Gradle/SDK নেই) — ব্র্যাকেট/প্যারেন-
  ব্যালেন্স স্ক্রিপ্টে মিলেছে (braces ৫৬০/৫৬০, parens ১২৩৯/১২৩৯, উপরের KYC-reject/বাজেট ফিক্সসহ)।
- **টেস্ট করা উচিত (ডিভাইসে):** ১) বিল্ড হয় কিনা; ২) সুপার অ্যাডমিন দিয়ে আগের মতোই সব কাজ করছে
  কিনা (রিগ্রেশন-চেক); ৩) নন-সুপার/ফ্ল্যাগড টেস্ট অ্যাকাউন্ট দিয়ে গেটিং verify (এখনো টেস্ট অ্যাকাউন্ট
  নেই — ৭.৯-এর শেষ QA পাসে কভার হবে)।

**৭.১ গ্রুপের ৪টা স্ক্রিনই (ক/খ/গ/ঘ) এখন কোড-সম্পন্ন।** বাকি: ৭.১.১ (Edge Function নিরাপত্তা-ফিক্স),
৭.১.২ ("বানাও অ্যাডমিন" ফ্লো)।

### পরবর্তী: ৭.১.১ — `admin-reset-user-password` Edge Function নিরাপত্তা-ফাঁক ফিক্স — ব্যবহারকারীর কনফার্মেশনের অপেক্ষায়

## ✅ সেশন ৭.১.১ — `admin-reset-user-password` Edge Function নিরাপত্তা-ফাঁক ফিক্স — সম্পন্ন + **লাইভে deploy করা হয়েছে** (২০২৬-০৯-২৫)

- **সমস্যা:** আগের কোড শুধু `is_admin(caller)` চেক করত — caller *যেকোনো* সক্রিয় admin হলেই
  *যেকোনো* target_user_id-এর পাসওয়ার্ড রিসেট করতে পারত: টার্গেট নিজে admin (এমনকি সুপার)
  হলেও ব্লক হতো না, caller-এর রোলে `users:users:reset_password` পারমিশন না থাকলেও/caller
  flagged থাকলেও কিছুই চেক হতো না।
- **নতুন migration:** `supabase/migrations/zz_20260925130000_admin_reset_password_precheck_session7_1_1.sql`
  — **লাইভে apply করা হয়েছে** (project mghvvpndkxnscwryfkib; apply-এর আগে `list_migrations`
  চেক করা হয়েছিল, কোনো untracked admin migration ছিল না)। একটাই নতুন SECURITY DEFINER RPC:
  `admin_reset_password_precheck(p_target_user_id uuid) returns jsonb` — caller-এর `auth.uid()`
  নিয়ে (client-পাঠানো কোনো flag বিশ্বাস না করে) একবারেই সিদ্ধান্ত নেয়:
  ১) caller সক্রিয় admin না হলে → `ADMIN_ONLY`; ২) target কোনো admin identity হলে
  (`admin_accounts.auth_user_id` মিলে বা `public.users.role='ADMIN'`, active/flagged/inactive
  নির্বিশেষে) এবং caller সুপার না হলে → `TARGET_IS_ADMIN`; ৩) সাধারণ ইউজার-টার্গেটে
  `admin_can_act('users:users:reset_password', caller)` false হলে (রোলে পারমিশন নেই বা
  flagged) → `PERMISSION_DENIED`; ৪) বাকি সব ঠিক থাকলে `{allowed: true}`। শুধু `authenticated`+
  `service_role`-কে grant, `anon`/`public`-এ revoke।
- **Edge Function আপডেট:** `supabase/functions/admin-reset-user-password/index.ts`-এ পুরনো
  `is_admin` RPC কল সরিয়ে নতুন `admin_reset_password_precheck` RPC কল বসানো হয়েছে —
  `precheck.allowed !== true` হলে `precheck.reason`-ই error হিসেবে ফেরত (NOT_AUTHENTICATED→401,
  বাকি সব→403); RPC কলই ব্যর্থ হলে (network/schema সমস্যা) fail-closed ADMIN_ONLY (fail-open না)।
  **এই Edge Function-ও লাইভে deploy করা হয়েছে (version 2 → version 3, ACTIVE, `verify_jwt: true`
  অপরিবর্তিত)** — শুধু migration/repo-ফাইল না, আসল প্রোডাকশন এন্ডপয়েন্টও এখন নতুন কোড চালাচ্ছে।
- **লাইভে ভেরিফাই করা হয়েছে (রোলড-ব্যাক ট্রানজ্যাকশনে, `set_config('request.jwt.claim.sub', ...)`
  দিয়ে caller সিমুলেট করে — কোনো residue নেই, পরে চেক করা):** ৯টা কেস, সবগুলো প্রত্যাশিত ফলাফল
  দিয়েছে —
  unauthenticated→NOT_AUTHENTICATED, non-admin caller→ADMIN_ONLY, নন-সুপার caller
  (বিদ্যমান দ্বিতীয় admin অ্যাকাউন্ট "Monika", রোল "KYC Manager") → target=সুপার অ্যাডমিন
  →TARGET_IS_ADMIN, নন-সুপার caller→target=নিজে (নিজেও admin)→TARGET_IS_ADMIN, নন-সুপার caller
  (যার রোলে reset_password পারমিশন আছে)→target=সাধারণ ইউজার→allowed:true, সুপার caller→
  target=অন্য admin→allowed:true (সুপার বলে), সুপার caller→target=সাধারণ ইউজার→allowed:true,
  caller-এর রোলে reset_password পারমিশন না থাকলে→PERMISSION_DENIED, caller flagged থাকলে→
  PERMISSION_DENIED। এছাড়া `anon` execute করতে পারে না, `authenticated` পারে — এটাও লাইভে
  কনফার্মড।
- **টেস্ট ফাইল আপডেট (deno, `supabase/tests/deno/`):** `_edge_function_test_helpers.ts`-এর
  `MockFetchOptions`/mock-fetch পুরনো `isAdmin`/`isAdminRpcFails` (`/rpc/is_admin` মক) থেকে
  নতুন `precheckAllowed`/`precheckReason`/`precheckRpcFails` (`/rpc/admin_reset_password_precheck`
  মক) দিয়ে প্রতিস্থাপিত। `01_admin_reset_user_password_authz_test.ts`-এ বিদ্যমান ৪টা টেস্ট নতুন
  মক-শেপে আপডেট + **নতুন দুটো টেস্ট যোগ** ((ঙ) TARGET_IS_ADMIN, (চ) PERMISSION_DENIED) —
  মোট ৬টা। `02_admin_reset_user_password_success_validation_test.ts`-এ success-path মক আপডেট
  (`isAdmin: true` → `precheckAllowed: true`)। মোট প্রত্যাশিত: "ok | 12 passed | 0 failed"
  (00 স্মোক ১টা + 01 authz ৬টা + 02 success/validation ৫টা)। **⚠️ এই ডেনো টেস্টগুলো এই সেশনেও
  চালানো যায়নি** — `_edge_function_test_helpers.ts`-এর হেডারে আগে থেকেই নোট করা sandbox
  network-allowlist ব্লকার (`jsr.io` host_not_allowed) অপরিবর্তিত, Windows/CI-তে
  `deno test --allow-net --allow-env --allow-read supabase/tests/deno/` দিয়ে ভেরিফাই করা দরকার।
- **যা বদলায়নি:** ক্লায়েন্ট (Kotlin) সাইডে এই সাব-স্টেপে কোনো ফাইল ছোঁয়া হয়নি — Edge Function
  কল-সাইট (যেখান থেকেই ডাকা হোক) আগের মতোই `target_user_id`/`new_password` পাঠায়, রেসপন্স-শেপ
  (success/error JSON) অপরিবর্তিত, শুধু নতুন সম্ভাব্য error কোড (`TARGET_IS_ADMIN`,
  `PERMISSION_DENIED`) যোগ হয়েছে — ক্লায়েন্ট এই দুটো না চিনলে সাধারণ generic error handling-এ
  পড়বে (কোনো ক্র্যাশ না)।
- **যাচাই হয়নি/বাকি (সৎ তালিকা):** (ক) Windows/CI-তে real `deno test` রান (উপরে বলা হয়েছে);
  (খ) ডিভাইসে আসল admin panel থেকে "পাসওয়ার্ড রিসেট" ফিচার ট্রিগার করে end-to-end verify —
  এখন পর্যন্ত শুধু SQL-লেভেল simulation ও deno mock-test লেখা হয়েছে, Kotlin UI থেকে real কল
  এখনো টেস্ট করা হয়নি।
- **টেস্ট করা উচিত (ডিভাইসে/ব্রাউজারে):** সুপার অ্যাডমিন দিয়ে কোনো সাধারণ ইউজারের পাসওয়ার্ড
  রিসেট আগের মতোই কাজ করছে কিনা (রিগ্রেশন-চেক, একমাত্র সুপার অ্যাকাউন্টই এখন আছে); "Monika"
  (KYC Manager, non-super) দিয়ে সাধারণ ইউজারের পাসওয়ার্ড রিসেট (তার রোলে permission আছে →
  কাজ করা উচিত) বনাম সুপারের পাসওয়ার্ড রিসেট করার চেষ্টা (ব্লক হওয়া উচিত, TARGET_IS_ADMIN)।

### 🟡 সেশন ৭.১.২ — "বানাও অ্যাডমিন" ফ্লো — অর্ধেক সম্পন্ন, বাকিটা হ্যান্ডঅফ (২০২৬-০৯-২৫)

মাস্টার প্রম্পটের ৭.১.২ স্কোপ (৪টা ধাপ + পারমিশন-গেটিং) দুই ভাগে ভাগ করা হলো — **এই সেশনে যা হয়েছে**
তার নিচেই **পরের Claude সেশনের জন্য ঠিক কী বাকি** তার সৎ, নির্দিষ্ট তালিকা।

#### ✅ এই সেশনে সম্পন্ন (কোড লেখা হয়েছে, ⚠️ বিল্ড/ডিভাইস-টেস্ট বাকি — নিচে দেখুন)

1. **`AdminPermissionCatalog.kt`** — `users:users` আইটেমে নতুন অ্যাকশন-কী `create_admin`
   ("বানাও অ্যাডমিন") যোগ হয়েছে, `delete`-এর ঠিক আগে, কমেন্টসহ।
2. **`AdminSession.kt`** — `ADMIN_HARD_SUPER_ONLY_EXACT_KEYS`-এ `"users:users:create_admin"` যোগ
   হয়েছে (মাস্টার প্রম্পটের নির্দেশ অনুযায়ী — RPC নিজেই সুপার-অনলি, তাই ক্লায়েন্ট-গেটও হার্ড-সুপার,
   রোলে টিক থাকলেও নন-সুপারের জন্য বাটন hidden থাকবে)।
3. **`AdminUsersView.kt` — রোল-চেঞ্জ ডায়ালগ থেকে "ADMIN" রেডিও-অপশন বাদ** (বাগ-ফিক্স অংশ) —
   `roles` লিস্টে এখন শুধু USER/SOLVER Triple, ADMIN Triple মুছে ফেলা হয়েছে, কমেন্টসহ ব্যাখ্যা।
   এই ডায়ালগ এখন legacy USER↔SOLVER টগলের জন্যই, যেটা অপরিবর্তিত/ঠিকই কাজ করে।
4. **`AdminUsersView.kt` — ইউজার-কার্ড মেনুতে নতুন "বানাও অ্যাডমিন" এন্ট্রি-পয়েন্ট (আংশিক)** —
   নতুন `var userForCreateAdmin by remember { mutableStateOf<UserEntity?>(null) }` state (
   `userForRoleChange`-এর ঠিক পরে) + নতুন `DropdownMenuItem` (মুছে ফেলুন-এর ঠিক আগে, `user.role !=
   "ADMIN"` ব্লকের ভেতরে — তাই শুধু নন-অ্যাডমিন কার্ডে দেখা যাবে), `enabled =
   AdminSession.canAct("users:users:create_admin")`, `AdminPanelSettings` আইকন (ইমপোর্ট যোগ
   হয়েছে)। ক্লিক করলে শুধু `userForCreateAdmin = user` সেট হয় — **এখনো কোনো bottom sheet/dialog
   এই state পড়ে না, তাই বর্তমানে ক্লিক করলে দৃশ্যত কিছুই ঘটবে না (no-op, ক্র্যাশ না)।**
5. ব্র্যাকেট/প্যারেন-ব্যালেন্স স্ক্রিপ্টে যাচাই করা হয়েছে (তিনটা ফাইলেই মিলেছে) — আগের সাব-স্টেপগুলোর
   মতোই এই এনভায়রনমেন্টে Kotlin কম্পাইল/Gradle চালানো যায়নি, তাই **আসল কম্পাইল এখনো ভেরিফাই হয়নি।**

#### ⏳ পরের Claude সেশনের জন্য বাকি (হ্যান্ডঅফ — মাস্টার প্রম্পটের ৭.১.২ স্কোপ অনুযায়ী)

- **ধাপ ৩ (সবচেয়ে বড় বাকি অংশ) — in-place bottom sheet ফর্ম:** `userForCreateAdmin != null` হলে
  `BottomSlideAlertDialog` (রোল-চেঞ্জ ডায়ালগের ঠিক একই প্যাটার্ন, উপরের কোডেই রেফারেন্স আছে) দেখাতে
  হবে, ভেতরে `AdminAccountsView.kt`-এর তৈরির ফর্মের ফিল্ড-সেট/ভ্যালিডেশন **পুনর্ব্যবহার** করে (এখনো
  কপি-পেস্ট করা হয়নি) — এর জন্য প্রথমে `AdminAccountsView.kt`-এর তৈরির ফর্ম (৯৭৭ লাইন ফাইল, ফর্ম
  অংশ এখনো এই সেশনে গ্রেপ/পড়া হয়নি) থেকে একটা শেয়ার্ড composable/ফাংশন বের করে আনতে হবে, যাতে
  ট্যাব ২৬-এর ফুল-পেজ ফর্ম ও এখানকার bottom sheet দুটোই একই কোড কল করে (শুধু কন্টেইনার আলাদা)।
  প্রি-ফিল নিয়ম: শুধু **নাম + ইমেইল** `userForCreateAdmin`-এর `UserEntity` থেকে (পদবি না — কোনো
  সোর্স-ডেটা নেই); ফোন ও পাসওয়ার্ড ফিল্ড ফাঁকা/নতুন দিতে হবে; রোল ড্রপডাউনে **explicit select
  বাধ্যতামূলক** (প্রি-সিলেক্ট না) এবং **সুপার রোল তালিকা থেকে বাদ** (UX, সার্ভার-এরর এড়াতে —
  `AdminAccountsView.kt`-এর তৈরির ফর্মে এটা ইতিমধ্যে থাকলে সেই একই লজিক পুনর্ব্যবহার)।
- **ধাপ ৪ — সফল-হলে হ্যান্ডলিং:** RPC (`admin_account_create`, বিদ্যমান, কোনো পরিবর্তন লাগবে না)
  থেকে account row ফেরত এলে bottom sheet বন্ধ (`userForCreateAdmin = null`) + টোস্ট + দুই জায়গার
  তালিকাই রিফ্রেশ — Usergon-এর লিস্ট টেকনিক্যালি রিফ্রেশ না করলেও চলে (টার্গেট user row অস্পৃষ্ট),
  কিন্তু "এডমিন অ্যাকাউন্ট" ট্যাব ২৬-এর তালিকা অবশ্যই রিফ্রেশ করতে হবে যাতে নতুন এডমিন সাথে সাথে
  দেখা যায়। এরর-হ্যান্ডলিং: `PHONE_IN_USE_BY_USER`/`CANNOT_ASSIGN_SUPER_ROLE`-সহ RPC-এর বিদ্যমান
  এরর-কোডগুলো `AdminAccountsView.kt`-এর তৈরির ফর্মে যেভাবে হ্যান্ডল হয় ঠিক সেভাবেই (শেয়ার্ড
  composable হলে এটা এমনিই আসবে)।
- **ভেরিফাই করা হয়নি (নতুন কোড লেখার আগে করা ভালো):** মাস্টার প্রম্পটে লেখা অনুমান যে
  `AdminAccountsView.kt`-এর তৈরির ফর্মে রোল-ড্রপডাউনে সুপার রোল ইতিমধ্যেই বাদ দেওয়া আছে ("ধরে নেওয়া
  হচ্ছে, বাস্তবায়নের শুরুতে যাচাই করে নেওয়া") — এই সেশনে `AdminAccountsView.kt` ফাইলটা এখনো খুলে
  দেখা হয়নি, তাই এটা সত্যিই কনফার্মড না।
- **সব ধাপ শেষে:** ব্র্যাকেট/প্যারেন-ব্যালেন্স রিচেক (নতুন dialog/composable যোগ হওয়ার পর), এবং
  ৭.১-এর আগের সাব-স্টেপগুলোর মতো ডিভাইসে টেস্ট করা উচিত এমন তালিকা যোগ করা (সুপার দিয়ে সফল create,
  ড্রপডাউনে সুপার রোল না-দেখা, নন-সুপার দিয়ে বাটনই hidden, দুই ফোনে দুই আলাদা লগইন কাজ করছে কিনা)।
- **এই সেশনে যা ছোঁয়া হয়নি (স্কোপের বাইরে রাখা হলো):** কোনো migration/RPC পরিবর্তন (দরকার নেই,
  মাস্টার প্রম্পট অনুযায়ী), `AdminAccountsView.kt` ফাইলের কোনো এডিট, `AdminRolePreviewPanel.kt`।

### পরবর্তী: ৭.১.২ (বাকি অংশ, উপরের হ্যান্ডঅফ) — ব্যবহারকারীর কনফার্মেশনের অপেক্ষায়

## সেশন ৭.১-৭.৯ (বাকি সাব-স্টেপ) — ⏳ শুরু হয়নি
