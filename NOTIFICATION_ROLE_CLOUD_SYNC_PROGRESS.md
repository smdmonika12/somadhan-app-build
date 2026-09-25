# NOTIFICATION_ROLE_CLOUD_SYNC_PROGRESS.md
## Supabase cloud-sync role গ্যাপ ফিক্স — এই সেশনের কাজ

**প্রেক্ষাপট:** local Room-এ `NotificationEntity.role`/`AdminAuditLogEntity.role` আগে থেকেই ছিল
(step6 zip, "notification-auditlog-role-aware")। কিন্তু multi-device sync-এ গ্যাপ ছিল: অন্য
ডিভাইস থেকে cloud হয়ে sync হয়ে আসা notification সবসময় `role=""` (role-neutral) হয়ে যেত, তাই
ban/restrict role-filter এড়িয়ে দুই role-এই দেখাত।

## ✅ আবিষ্কার (গুরুত্বপূর্ণ — সময় বাঁচাবে)

Supabase MCP দিয়ে লাইভ DB (project `mghvvpndkxnscwryfkib`) সরাসরি query করে যাচাই করা হয়েছে:
- `public.notifications` ও `public.admin_audit_logs` টেবিলে **`role` কলাম ইতিমধ্যেই আছে**
  (text, default `''`)।
- `create_notification`, `admin_notify_user`, `log_admin_action` — প্রতিটার **`p_role`-সহ নতুন
  overload ইতিমধ্যেই লাইভ DB-তে আছে** (পুরনো role-বিহীন overload-ও এখনো আছে, dead/unused
  থাকবে)।
- অর্থাৎ **Supabase-সাইড migration (table + RPC) আগে থেকেই সম্পূর্ণ ছিল** — কোনো নতুন `.sql`
  migration ফাইল বা `apply_migration` লাগেনি এই ফিক্সে।
- আসল গ্যাপ ছিল শুধু **Kotlin-সাইড wiring**:
  1. `NotificationDto.kt`/`AdminAuditLogDto.kt`-তে `role` ফিল্ডই ছিল না।
  2. `NotificationDto.toNotificationEntity()` mapper role pass করত না (**এটাই আসল bug** —
     cloud pull path, `SupabaseRealtimeManager.pullAllCloudDataToLocal()` এই mapper ব্যবহার
     করে)।
  3. `SupabaseSyncManager.kt`-এর `createNotification()`/`adminNotifyUser()`/`logAdminAction()`
     wrapper-গুলো RPC-তে `p_role` পাঠাত না — exact param-count match-এর কারণে PostgREST পুরনো
     role-বিহীন overload resolve করত।
- `admin_audit_logs` টেবিলটা **কখনো cloud→local bulk-pull হয় না** (কোনো mapper/pull ফাংশন নেই,
  `SomadhanRepository.kt`-এর comment এ আগে থেকেই নথিভুক্ত) — তাই ওই টেবিলে DTO-তে role যোগ করা
  শুধু completeness-এর জন্য, বাস্তবে কোনো mapper এটা ব্যবহার করে না।

## ✅ এই সেশনে যা করা হয়েছে (সম্পন্ন)

1. `app/src/main/java/com/example/data/remote/dto/NotificationDto.kt` — `val role: String = ""` যোগ।
2. `app/src/main/java/com/example/data/remote/dto/AdminAuditLogDto.kt` — `val role: String = ""` যোগ (future-proofing, mapper নেই)।
3. `app/src/main/java/com/example/data/remote/MessageTransactionMappers.kt` —
   `NotificationDto.toNotificationEntity()`-তে `role = role` pass-through যোগ (**মূল ফিক্স**)।
4. `app/src/main/java/com/example/data/remote/SupabaseSyncManager.kt` —
   - `createNotification(...)`, `adminNotifyUser(...)`, `logAdminAction(...)` — প্রতিটায়
     `role: String = ""` প্যারামিটার যোগ, RPC কলে `p_role` পাঠানো হচ্ছে।
5. `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` —
   - প্রাইভেট `logAdminAction()` wrapper (লাইন ~444) — `role` এখন
     `SupabaseSyncManager.logAdminAction(...)`-এ পাস হচ্ছে (আগের comment "migration লাগবে,
     স্কোপের বাইরে" — এখন resolve হয়ে গেছে, comment আপডেট করা হয়েছে)।
   - **`SupabaseSyncManager.createNotification(...)`-এর সবগুলো (২৬টা) call-site** এখন `role =`
     প্যারামিটার পাস করে — পাশের local `NotificationEntity`-এর role থেকে (`notif.role` ইত্যাদি)
     অথবা hardcoded মান যেখানে entity inline literal ছিল। প্রতিটা call-site python script দিয়ে
     programmatically verify করা হয়েছে (২৫/২৫ real call site-এ `role` আছে, ২৬তম একটা comment
     লাইন যেখানে literal মিল পাওয়া গিয়েছিল কিন্তু আসল কল না)।

## ✅ যাচাই করা হয়েছে

- `SomadhanRepository.kt`-এ curly-brace balance: 0 (bracket ভাঙেনি)।
- paren imbalance আগে থেকেই ছিল (Bengali কমেন্ট/স্ট্রিং-এ stray `(`/`)` থাকার কারণে, pre-existing,
  এই সেশনের এডিটের সাথে সম্পর্কহীন — নতুন কোনো paren যোগ হয়নি, শুধু `role = X` টোকেন)।
- প্রতিটা `SupabaseSyncManager.createNotification(...)` call block-এ line-by-line স্ক্যান করে
  `role` টোকেন আছে কিনা নিশ্চিত করা হয়েছে (script-এ দেখুন, সব পাস)।
- **Build/compile করে দেখা হয়নি** (এই environment-এ Android SDK/Gradle নেই) — শুধু bracket-balance
  + role-presence static check। পরবর্তী সেশনে/ব্যবহারকারীর নিজের Android Studio-তে একবার পুরো
  প্রজেক্ট build করে compile error নেই কিনা যাচাই করা উচিত (বিশেষত `role = X.role` রেফারেন্সগুলো —
  প্রতিটা `X` variable (`notif`, `welcomeNotif`, `userNotif`, `solverNotif`, `notifClient`,
  `notifSolver` ইত্যাদি) সেই স্কোপে সত্যিই আছে কিনা, যদিও প্রতিটা call-site view করেই বসানো হয়েছে)।

## ❌ যা বাকি (পরবর্তী সেশনের জন্য)

1. **Gradle build verify** — উপরে বলা হয়েছে।
2. **Manual/device টেস্ট (আসল লক্ষ্য):**
   - দুইটা ডিভাইসে (বা একই ডিভাইসে দুইটা সেশন simulate করে) dual-role ইউজার দিয়ে লগইন করে,
     একটা ডিভাইসে notification generate করান (যেমন bid accept), অন্য ডিভাইসে
     pull/realtime sync-এর পর সেই notification role-filter মেনে সঠিক role-এই দেখাচ্ছে কিনা
     (অন্য role-এ browse করলে দেখা উচিত না) — এটাই আসল bug যেটা ফিক্স করা হয়েছে, এখন confirm
     করা দরকার।
   - Admin panel থেকে ban/restrict করার notification আর KYC approve/reject notification —
     এই দুটো `create_notification`-এর মাধ্যমে যায়, role ঠিকভাবে ট্যাগ হচ্ছে কিনা যাচাই করুন।
3. **স্কোপের বাইরে, ইচ্ছাকৃতভাবে বাদ (ভবিষ্যতে দরকার হলে আলাদা সেশনে):**
   - `admin_broadcast_notification`, `notify_admins`, `admin_delete_notification_group` — এই
     RPC-গুলোর জন্য কোনো role প্যারামিটার/wiring যোগ করা হয়নি (এগুলো broadcast/all-target টাইপ,
     সাধারণত role-neutral হওয়াই স্বাভাবিক — যাচাই করা হয়নি, assume করা হয়েছে ঠিক আছে)।
   - `admin_audit_logs`-এর জন্য কোনো cloud→local pull path নেই, তাই ওই DTO-র role ফিল্ড এখন
     অব্যবহৃত থাকবে যতক্ষণ না ভবিষ্যতে কেউ admin audit log-এর জন্যও multi-device pull বানায়
     (এই মুহূর্তে দরকার নেই বলে মনে হচ্ছে, শুধু নোট করে রাখা হলো)।
   - বাকি ~১০০টা RPC (যেগুলো raw `insert into public.notifications` করে, `create_notification`
     এর মধ্য দিয়ে না) — এগুলো **এই ফিক্সের স্কোপের বাইরে**। এদের বেশিরভাগ সরাসরি server-side
     event handle করে (যেমন release_escrow, refund_escrow_once) আর সেগুলোর role স্ট্রাকচারালি
     fixed/known (SOLVER পেমেন্ট, USER রিফান্ড ইত্যাদি) — লাইভ DB-তে `role` কলাম না থাকলে insert
     এ role বসত না, কিন্তু কলাম থাকায় ওগুলো ডিফল্ট `''`-এ পড়বে (role-neutral, ভাঙবে না)। এই
     RPC-গুলোতে role ট্যাগ করা দরকার কিনা — সেটা user-এর সিদ্ধান্ত, আলাদা মাস্টার প্রম্পট/সেশনে
     নেওয়া উচিত (`TRANSACTION_ROLE_FIELD_DESIGN.md`/step36 migration-এর প্যাটার্ন অনুসরণ করে,
     কিন্তু transactions-এর ১২টা RPC-এর তুলনায় notifications-এ পরিধি অনেক বড়)।

## পরবর্তী সেশনের জন্য নির্দেশ

এই ফাইল + zip দিলেই যথেষ্ট, নতুন করে root-cause investigate করার দরকার নেই। প্রথমে "যা বাকি" #১-২
(build verify + manual test) করুন। #৩ (broader RPC role-tagging) দরকার হলে আলাদাভাবে scope করে
শুরু করবেন — ব্যবহারকারীকে জিজ্ঞেস করুন সেটা দরকার কিনা, কারণ এটা অনেক বড় কাজ (~১০০ RPC)।
