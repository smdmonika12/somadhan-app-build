# সমাধান (Somadhan) — Realtime Broadcast/Topic Scoping ফিক্স: প্রোগ্রেস ফাইল

এই ফাইলটা `somadhan-realtime-scoping-fix-master-prompt.md`-এর কাজের জন্য, মূল Firebase→Supabase
migration-এর `MIGRATION_PROGRESS.md` থেকে **সম্পূর্ণ আলাদা** রাখা হচ্ছে (মাস্টার-প্রম্পটের নিয়ম #১০
অনুযায়ী)।

---

## ✅ resolved — `messages` broadcast RLS গ্যাপ + সলভার থ্রেড আইসোলেশন

নিচের "🔍 Firebase-parity অডিট" এন্ট্রিতে পাওয়া `messages` broadcast RLS গ্যাপ, এবং ব্যবহারকারীর
নতুন করে চাওয়া "নতুন solver যেন আগের বাতিল হওয়া solver-এর পুরনো চ্যাট না দেখে" — এই দুটো সমস্যা
একই migration-এ একসাথে সমাধান হয়ে গেছে (সরু sender/receiver-`exists` যোগ করার বদলে,
`accepted_solver_id`-ভিত্তিক ব্লানকেট এক্সেসটাই সম্পূর্ণ সরিয়ে sender/receiver-স্কোপড এক্সেস
বসানো হয়েছে — যেটা আসল root-cause ছিল)। বিস্তারিত এন্ট্রি ফাইলের নিচের দিকে, "✅ Messages
সলভার থ্রেড আইসোলেশন" শিরোনামে।

---

## 🟡 ধাপ ১ — Foundation + `notifications` Pilot — **আংশিক সম্পন্ন (DB-সাইড ✅, Kotlin-সাইড ❌, broadcast ডেলিভারি ❌ — একটা ব্লকিং ইনফ্রা-গ্যাপ পাওয়া গেছে)**

**সেশনের প্রেক্ষাপট:** ব্যবহারকারী `somadhan-e164-followup-fix-v6.zip` (E.164 follow-up ফিক্সের
zip, ২২৭টা ফাইল) ও মাস্টার-প্রম্পট ফাইল আপলোড করে জানান যে আগে এই realtime scoping কাজ দেওয়া
হয়েছিল, migration apply হয়ে গেছে দেখেছেন, কিন্তু Supabase MCP অ্যাক্সেসের approval না দেওয়ার কারণে
কাজ আটকে গিয়েছিল। এই সেশনে Supabase MCP দিয়ে সরাসরি DB-এর বর্তমান অবস্থা যাচাই করা হলো।

### ✅ DB-সাইড যা যাচাই করে পাওয়া গেছে (আগের কোনো সেশনে ইতিমধ্যে apply করা ছিল)

`Supabase:list_migrations` (project: `mghvvpndkxnscwryfkib`) চালিয়ে দেখা গেছে
**`realtime_scoping_step1_notifications_broadcast`** (version `20260913085320`) migration
আগে থেকেই DB-তে apply করা আছে। কনটেন্ট verify করে দেখা হলো:

1. **RLS policy** (`realtime.messages`-এ, `pg_policies` দিয়ে verify করা):
   - নাম: `users can receive own user-topic broadcasts`
   - কমান্ড: SELECT
   - কন্ডিশন: `extension = 'broadcast' AND realtime.topic() = 'user:' || auth.uid()::text`
   - ✅ মাস্টার-প্রম্পটের নির্দেশনা অনুযায়ী সঠিক — শুধু নিজের `user:<uuid>` topic-ই subscribe করা যাবে।

2. **Trigger** (`notifications` টেবিলে, `pg_trigger`/`pg_get_triggerdef` দিয়ে verify করা):
   - নাম: `broadcast_notifications_changes`
   - `AFTER INSERT OR DELETE OR UPDATE ON public.notifications FOR EACH ROW`
   - ফাংশন `notify_notifications_broadcast()` — `realtime.broadcast_changes('user:' ||
     coalesce(NEW.user_id, OLD.user_id)::text, TG_OP, TG_OP, TG_TABLE_NAME, TG_TABLE_SCHEMA,
     NEW, OLD)` কল করে।
   - ✅ টপিক-ফরম্যাট, তিনটা ইভেন্ট-টাইপ (INSERT/UPDATE/DELETE) কভারেজ — সব সঠিক।

3. **`notifications` স্কিমা**: `user_id` কলাম `uuid`, `NOT NULL`, FK → `users(id)` — ✅ প্রম্পটের
   প্রি-কন্ডিশন পূরণ করে।

### ❌ বাধ্যতামূলক SQL-টেস্ট (নিয়ম #৮) চালিয়ে যা পাওয়া গেছে — **ব্লকিং ইনফ্রা-গ্যাপ**

Trigger/RLS কোড সঠিক হলেও, broadcast আসলে কাজ করে কিনা টেস্ট করতে গিয়ে দেখা গেল **broadcast কোথাও
পৌঁছাচ্ছে না**:

- `realtime.broadcast_changes()` সরাসরি কল করে (দুইটা আলাদা টেস্ট topic দিয়ে,
  `user:11111111-...`/`user:22222222-...`/`user:33333333-...`) `realtime.messages`-এ row আসে
  কিনা চেক করা হলো — **কোনো row insert হয়নি**, এমনকি একই statement-এর মধ্যে চেক করেও না।
- মূল কারণ খুঁজে বের করা হলো: `realtime.messages` টেবিলটা `RANGE (inserted_at)` দিয়ে
  **partition করা**, কিন্তু এই প্রজেক্টে **একটাও child partition তৈরি হয়নি** (`pg_inherits` কুয়েরি
  করে ০টা partition পাওয়া গেছে — আজকেরটাও না)।
- `realtime.send()` ফাংশনের সংজ্ঞা দেখে নিশ্চিত হওয়া গেছে যে এর ভেতরে `EXCEPTION WHEN OTHERS THEN
  RAISE WARNING ...` আছে — অর্থাৎ partition-না-থাকার কারণে insert ব্যর্থ হলেও এটা **silently গিলে
  ফেলা হয়, কোনো error client পর্যন্ত পৌঁছায় না**। এটাই ঠিক সেই "silent event-miss" ঝুঁকি যা
  মাস্টার-প্রম্পটের নিয়ম #৮-এ সবচেয়ে বড় বিপদ হিসেবে চিহ্নিত করা ছিল — বাস্তবে ঘটে যাচ্ছে।
- `realtime.messages`-এ সার্বিকভাবে `min(inserted_at)`/`max(inserted_at)` — দুটোই `null`, অর্থাৎ
  **এই প্রজেক্টে কখনোই কোনো broadcast message সফলভাবে insert হয়নি**, শুধু notifications-এর জন্যই
  না, পুরো প্রজেক্টেই।
- নিজে থেকে partition বানানোর চেষ্টা করা হয়েছিল (`create table realtime.messages_2026_09_13
  partition of realtime.messages ...`) — **`ERROR: permission denied for schema realtime`**।
  অর্থাৎ MCP-এর ডাটাবেস রোল দিয়ে `realtime` স্কিমায় নতুন অবজেক্ট বানানো সম্ভব না — এটা Supabase-এর
  ম্যানেজড ইনফ্রাস্ট্রাকচারের অংশ, platform-level-এই resolve হতে হবে (সাধারণত Realtime সার্ভার
  নিজে থেকে আগে-থেকে কয়েকদিনের partition বানিয়ে রাখে; এই প্রজেক্টে কেন এখনো হয়নি সেটা অজানা)।

**এই কারণে এই সেশনে Kotlin-সাইড কোড (dual-run broadcast subscription) লেখা হয়নি** — কারণ
broadcast delivery নিজেই কাজ করছে না প্রমাণিত হওয়ায়, সেই কোড লিখলেও এখনই টেস্ট/ভেরিফাই করার কোনো
উপায় নেই, আর ব্যবহারকারীকে ভুল আত্মবিশ্বাস দেওয়া ঠিক হবে না।

### ⏸️ যাচাই করা হয়নি (স্কোপ/সময়ের কারণে)

- **"Allow public access" Realtime সেটিং** (নিয়ম #৭) — এটা project-এর Postgres schema থেকে
  SQL দিয়ে verify করার কোনো উপায় পাওয়া যায়নি (`_realtime.tenants`-জাতীয় কোনো টেবিল এই প্রজেক্টের
  visible schema-তে নেই — platform-level কনফিগ)। ব্যবহারকারীকে Dashboard-এ গিয়ে
  **Project Settings → Realtime → "Allow public access"** বন্ধ আছে কিনা সরাসরি চেক করতে হবে।

### 🔜 এই মুহূর্তে যা করা দরকার (ব্যবহারকারীকে সিদ্ধান্ত নিতে বলা হয়েছিল)

ব্যবহারকারীকে জিজ্ঞেস করা হয়েছিল partition-সমস্যা নিয়ে কীভাবে এগোতে চান (Kotlin কোড এখনই লেখা /
আগে Supabase-এ ঠিক করে নেওয়া / শুধু রিপোর্ট করে থামা) — **এই সেশনে চূড়ান্ত উত্তর পাওয়ার আগেই**
ব্যবহারকারী প্রোগ্রেস ফাইল লিখে zip ডেলিভার করতে বলেছেন, যাতে অন্য একটা Claude session থেকে এখান
থেকে continue করা যায়।

**পরের session/ব্যবহারকারীর জন্য প্রথম কাজ:**
1. Supabase Dashboard-এ গিয়ে **Realtime → partition/broadcast** সংক্রান্ত কোনো status/error আছে
   কিনা দেখা, অথবা Supabase support-কে জিজ্ঞেস করা কেন `realtime.messages`-এ কোনো partition
   তৈরি হয়নি। (বিকল্প: কিছুক্ষণ/কয়েক ঘণ্টা অপেক্ষা করে আবার SQL দিয়ে
   `select count(*) from pg_inherits where inhparent = 'realtime.messages'::regclass;` চালিয়ে
   দেখা প্ল্যাটফর্ম নিজে থেকে partition বানিয়েছে কিনা)।
2. Partition সমস্যা resolve হলে, উপরের SQL-টেস্ট (দুই আলাদা topic-এ `realtime.broadcast_changes`
   কল করে `realtime.messages`-এ row আসছে কিনা) আবার চালিয়ে নিশ্চিত হওয়া broadcast আসলেই কাজ করছে।
3. এরপরই মাস্টার-প্রম্পটের ধাপ ১-এর বাকি অংশ করা: `SupabaseRealtimeManager.kt`-এ notifications-এর
   জন্য নতুন broadcast subscription (dual-run, পুরনো `postgresChangeFlow` অক্ষত, admin-এর জন্য
   branch করে বাদ) যোগ করা, তারপর real device/client দিয়ে বা আরেকবার SQL simulation দিয়ে
   end-to-end টেস্ট।
4. "Allow public access" সেটিং সরাসরি Dashboard-এ চেক/বন্ধ করা (এখনো ভেরিফাই করা হয়নি)।

### 📦 এই সেশনে ফাইল পরিবর্তন

- কোনো Kotlin/কোড ফাইল এডিট করা হয়নি (উপরের কারণে)।
- কোনো নতুন migration ফাইল যোগ করা হয়নি (DB-তে যে migration আছে সেটা আগের সেশনের, এই zip-এ
  `supabase/migrations/`-এ কোনো `realtime_scoping_step1_*.sql` নেই — **এটা একটা gap**, পরের
  সেশনে DB থেকে migration content বের করে `.sql` ফাইল হিসেবে repo-তে যোগ করে রাখা উচিত, ইতিহাসের
  জন্য — মাস্টার-প্রম্পটের নিয়ম #৬-এর চেতনা অনুযায়ী)।
- শুধু এই `/REALTIME_SCOPING_PROGRESS.md` ফাইলটা নতুন যোগ হলো।
- মূল zip (`somadhan-e164-followup-fix-v6.zip`) ২২৭টা ফাইল ছিল, এখন ২২৮টা (এই প্রোগ্রেস ফাইল
  যোগ হওয়ায়) — file-count diff করে confirm করা হয়েছে বাকি ২২৭টা ফাইল byte-for-byte অপরিবর্তিত।

---

## 🔵 পরবর্তী সেশন (continuation) — ব্লকিং কারণ চিহ্নিত করা হলো, কিন্তু সমাধান আমাদের হাতে না

**যা করা হলো:** Supabase MCP দিয়ে আবার পার্টিশন-সংখ্যা চেক করা হলো (`pg_inherits` কুয়েরি) —
এখনো **০টা**। `realtime.broadcast_changes()` সরাসরি কল করে (বৈধ `TG_OP` মান `'INSERT'` দিয়ে)
আবার টেস্ট করা হলো — `realtime.messages`-এ কোনো row insert হয়নি (আগের সেশনের ফলাফল একই, সমস্যা
persist করছে)।

**মূল কারণ এখন সুনির্দিষ্টভাবে জানা গেছে** (Supabase-এর official troubleshooting docs
"WarnSendingBroadcastMessage" পড়ে — সরাসরি এই সমস্যা নিয়েই লেখা):
- `realtime.messages`-এর দৈনিক partition তৈরি হয় শুধু দুইটা ঘটনায়: (ক) **কোনো client প্রথমবার
  WebSocket দিয়ে connect করে project-এর কোনো channel-এ join করলে** (migration চালানোর পর প্রথমবার),
  অথবা (খ) platform-এর periodic "janitor" প্রসেস — কিন্তু এটাও শুধু **যেসব প্রজেক্টে ইতিমধ্যে কোনো
  client connect করে আছে** সেগুলোই কভার করে।
- অর্থাৎ **এটা কোনো misconfiguration বা permission-সমস্যা না** — এই প্রজেক্টে এখনো পর্যন্ত
  বাস্তবে **কোনো client (app/device/dashboard) Supabase Realtime-এ WebSocket দিয়ে connect করে
  কোনো channel join করেনি** (dev-পর্যায়ে থাকায়, আগের ৮টা টেবিলের `postgresChangeFlow`
  subscription থাকলেও সেগুলো বাস্তবে কোনো ডিভাইসে চালানো হয়নি বলেই মনে হচ্ছে)।
- এই সমস্যা **SQL/migration দিয়ে ঠিক করা সম্ভব না** — partition বানানোর জন্য `realtime` স্কিমায়
  সরাসরি অ্যাক্সেস লাগবে যা আগের সেশনে `permission denied` দিয়ে প্রমাণিত হয়েছে, আর এটাই docs-এও
  নিশ্চিত করে যে এটা platform-managed, client-connection-নির্ভর behavior।

**সমাধানের জন্য ব্যবহারকারীর একটা ছোট, one-time ম্যানুয়াল পদক্ষেপ দরকার** (কোনো কোড লাগবে না):
1. Supabase Dashboard → প্রজেক্ট `mghvvpndkxnscwryfkib` → **Realtime → Inspector** পেজে যাওয়া।
2. যেকোনো নামের একটা channel টাইপ করে **"Join channel"** ক্লিক করা (এটা নিজেই একটা real
   WebSocket connection তৈরি করবে, যেটাই partition বানানোর ট্রিগার)।
3. এরপর (কয়েক সেকেন্ডের মধ্যেই) পরের session-এ আবার
   `select count(*) from pg_inherits where inhparent = 'realtime.messages'::regclass;` চালিয়ে
   partition তৈরি হয়েছে কিনা যাচাই করা, তারপর ধাপ ১-এর broadcast SQL-টেস্ট আবার চালানো।
4. এই একই সুযোগে Dashboard-এই থাকা অবস্থায় **Project Settings → Realtime → "Allow public
   access"** বন্ধ আছে কিনা যাচাই/নিশ্চিত করা যাবে (নিয়ম #৭, এখনো ভেরিফাই করা হয়নি)।

**partition তৈরি হয়ে গেলে এবং broadcast-টেস্ট পাস করলে**, সাথে সাথেই ধাপ ১-এর বাকি অংশ
(Kotlin-সাইড `SupabaseRealtimeManager.kt`-এ notifications dual-run broadcast subscription) করা
যাবে — কোনো নতুন blocker আশা করা হচ্ছে না, যেহেতু DB-সাইড (RLS policy + trigger) আগে থেকেই সঠিক
বলে verify করা আছে।

---

## ✅ ধাপ ১ — Foundation + `notifications` Pilot — **সম্পন্ন (DB-সাইড টেস্ট পাস, Kotlin-সাইড কোড লেখা হয়েছে, build-এ যাচাই বাকি)**

**partition ব্লকার সমাধান হলো:** ব্যবহারকারী Supabase Dashboard-এর **Realtime → Inspector**-এ
একটা channel join করলেন (স্ক্রিনশট দিয়ে নিশ্চিত করা হয়েছে)। এরপর `pg_inherits` কুয়েরি করে দেখা
গেল ৫টা daily partition তৈরি হয়ে গেছে (`messages_2026_09_12` থেকে `messages_2026_09_16`)।

**বাধ্যতামূলক SQL-টেস্ট (নিয়ম #৮) — সবগুলো পাস করেছে:**
1. `realtime.broadcast_changes()` সরাসরি দুইটা ভিন্ন topic দিয়ে কল করে `realtime.messages`-এ
   row আসছে কিনা যাচাই — ✅ দুটোই সঠিক topic-এ insert হয়েছে।
2. আসল `notifications` টেবিলে একটা real user-এর জন্য INSERT → UPDATE (is_read=true) → DELETE
   করে verify করা হলো trigger তিনটা event-ই (`INSERT`/`UPDATE`/`DELETE`) সঠিক `user:<uuid>`
   topic-এ broadcast করছে — ✅ তিনটাই সঠিক।
3. RLS policy simulation: `set local role authenticated; set local request.jwt.claim.sub`
   দিয়ে (ক) ভুল ইউজারের topic পড়ার চেষ্টা → **০ row** (সঠিকভাবে reject), (খ) নিজের topic পড়ার
   চেষ্টা → **১ row** (সঠিকভাবে allow) — পজিটিভ ও নেগেটিভ দুই কন্ট্রোলই পাস।

**Kotlin-সাইড (`SupabaseRealtimeManager.kt`) যা যোগ করা হলো:**
- `userTopicBroadcastChannel: RealtimeChannel?` — normal (non-admin) session-এর `user:<uuid>`
  private broadcast topic-এর জন্য একটাই channel (পরের ধাপগুলো এই একই channel-এ আরও
  broadcastFlow listener যোগ করবে, নতুন channel বানাবে না — topic শেয়ার্ড)।
- `startUserTopicBroadcastSubscription(userId)` — channel `"user:$userId"` (`isPrivate = true`)
  বানিয়ে তাতে `NotificationChangeBroadcastPayload`-এর INSERT/UPDATE/DELETE তিনটা broadcastFlow
  আলাদাভাবে সাবস্ক্রাইব করে, `applyNotificationBroadcastChange()`-এ পাঠায় (যেটা
  `handleNotificationAction()`-এর মতোই idempotent Room upsert/delete করে)। idempotent (আগেরটা
  বন্ধ করে নতুন করে subscribe করে)।
- `stopUserTopicBroadcastSubscription()` — কাউন্টারপার্ট, `stopRealtimeListeners()`-এ যোগ করা।
- `isCurrentSessionAdmin(userId)` — local Room `UserEntity.role == "ADMIN"` দিয়ে চেক করে
  (SomadhanViewModel-এর প্যাটার্নের মতোই) — admin হলে নতুন broadcast subscription **চালু হয় না**
  (নিয়ম #৪), শুধু পুরনো টেবিল-ওয়াইড subscription-ই চলবে।
- `startRealtimeListeners()`-এ wiring: `SupabaseAuthManager.currentUserId()` পাওয়া গেলে এবং
  admin না হলে নতুন subscription শুরু হয়, নাহলে বন্ধ থাকে/বন্ধ করে দেওয়া হয়।
- **dual-run নিশ্চিত করা হয়েছে**: পুরনো `notificationsChannel` (postgresChangeFlow, টেবিল-ওয়াইড)
  কোনোভাবেই সরানো/পরিবর্তন করা হয়নি — শুধু পাশে নতুন subscription যোগ হয়েছে।

**⚠️ এখনো যা verify করা হয়নি:**
- **Kotlin কোড build/compile করা যায়নি** (network/Gradle সুবিধা নেই এই sandbox-এ) — শুধু
  manual bracket/paren balance চেক করা হয়েছে (২৭৬/২৭৬ braces, ৭৪৫/৭৪৫ parens মিলেছে)। বিশেষভাবে
  অনিশ্চিত: `realtime.broadcast_changes()`-এর broadcast payload-এ `record`/`old_record`
  key-নাম ঠিক এভাবেই আসে কিনা (Supabase অফিসিয়াল ডকুমেন্টেশনের trigger-ফাংশন উদাহরণ থেকে
  positional argument-এর নাম অনুযায়ী অনুমান করে লেখা হয়েছে, কোনো literal JSON উদাহরণ
  ডকুমেন্টেশনে broadcast_changes-এর জন্য নির্দিষ্ট করে পাওয়া যায়নি) — প্রথম Android Studio
  build/device test-এ **অগ্রাধিকার দিয়ে** verify করা উচিত।
- **"Allow public access" Realtime সেটিং** (নিয়ম #৭) — এখনো Dashboard-এ গিয়ে সরাসরি
  চেক/বন্ধ করা হয়নি।
- **real device দিয়ে end-to-end টেস্ট** (দুই আলাদা account লগইন করে cross-account-leak-না-হওয়া
  verify করা) — SQL-level simulation-এই সীমাবদ্ধ ছিল এই সেশনে।

### 📦 এই সেশনে ফাইল পরিবর্তন
- `app/src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt` — উপরে বর্ণিত
  dual-run broadcast subscription কোড যোগ (ফাইলের শীর্ষ docstring-ও আপডেট করা হলো)।
- নতুন `supabase/migrations/realtime_scoping_step1_notifications_broadcast.sql` — DB-তে আগে
  থেকেই apply করা migration-টা (RLS policy + trigger function + trigger) DB থেকে
  reconstruct করে repo-তে যোগ করা হলো (আগের সেশনের ফ্ল্যাগ করা gap পূরণ)।
- এই প্রোগ্রেস ফাইলে "ধাপ ১ — সম্পন্ন" এন্ট্রি যোগ।

### 🔜 পরের ধাপ (ধাপ ২ — Wallet/Money গ্রুপ)
প্রথমে **Android Studio-তে একটা Gradle sync/build** করে উপরের ⚠️ payload-ডিকোডিং অংশটা confirm
করে নেওয়া ভালো (নাহলেও ধাপ ২ এগোনো যায়, কিন্তু ঝুঁকি জমে যাবে)। এরপর ধাপ ২-এ `transactions`
(dual-owner, দুইবার broadcast)/`withdrawals`/`gateway_payments`/`additional_charges`
(dual-party, owner_id lookup লাগবে) — একই `user:<uuid>` topic-ভিত্তিক প্যাটার্ন, কিন্তু এই ৪টা
টাকা-সংক্রান্ত টেবিল বলে টেস্টিং (নিয়ম #৮) আরও কঠোরভাবে করা দরকার (silent event-miss মানে টাকার
status-update মিস করা)।

---

## 🔍 এই সেশন — Duplicate-migration চেক + ধাপ ২ (wallet/money গ্রুপ) সম্পন্ন

**ব্যবহারকারীর অনুরোধ:** আগের migration আসলেই apply হয়ে গেছে কিনা, আর কোথাও duplicate migration
তৈরি হয়ে গেছে কিনা যাচাই করা, duplicate থাকলে ঠিক করা।

### ✅ Duplicate-migration চেক — কোনো duplicate পাওয়া যায়নি

`Supabase:list_migrations` (project `mghvvpndkxnscwryfkib`) দিয়ে DB-এর পুরো migration history
(৯৫টা version) দেখা হলো এবং সরাসরি SQL দিয়ে verify করা হলো:

- সব migration version সংখ্যা **ইউনিক** — কোনো version দুইবার apply হয়নি।
- `realtime.messages`-এর উপর policy চেক করে দেখা গেল `"users can receive own user-topic
  broadcasts"` policy-টা **ঠিক একবারই** আছে (duplicate policy তৈরি হয়নি)।
- ৪টা নতুন wallet trigger (`broadcast_transactions_changes`/`broadcast_withdrawals_changes`/
  `broadcast_gateway_payments_changes`/`broadcast_additional_charges_changes`) আর তাদের ৪টা
  function (`notify_*_broadcast`) — প্রতিটাই **ঠিক একবার** আছে, কোনো ডুপ্লিকেট trigger/function নেই।

**অর্থাৎ DB-সাইডে "duplicate migration" সমস্যা আসলে ছিল না।**

### ⚠️ আসল সমস্যা যা পাওয়া গেল — repo (এই zip) DB-এর চেয়ে পিছিয়ে ছিল

`list_migrations` চালিয়ে দেখা গেল DB-তে **ধাপ ১-এর পরেও আরও ২টা migration আগে থেকেই apply করা**
ছিল, যেগুলো এই zip-এ (`supabase/migrations/`) এবং আগের progress-এন্ট্রিতে **উল্লেখই ছিল না**:

- `20260913093147` — `realtime_scoping_step2_wallet_broadcast` (transactions/withdrawals/
  gateway_payments/additional_charges-এর জন্য ৪টা trigger, bare TG_OP event নামে)
- `20260913093831` — `realtime_scoping_step2_wallet_event_naming_fix` (একই ৪টা function
  `create or replace` করে event নাম table-prefixed করা হলো, যেমন `'transactions_' || TG_OP` —
  কারণ bare event নাম notifications-এর bare event নামের সাথে collision করতো, silent
  event-misroute ঝুঁকি, নিয়ম #৮)

অর্থাৎ **ধাপ ২ (wallet/money গ্রুপ)-এর DB-সাইড কাজ আসলে আগের একটা সেশনে ইতিমধ্যেই সম্পূর্ণ হয়ে
গিয়েছিল**, কিন্তু সেই সেশনের delivery zip/`.sql` ফাইল/progress-এন্ট্রি এই zip-এ কখনো আসেনি — সেটাই
আসল gap (duplicate না, বরং **missing/stale record**)। এছাড়া `realtime_scoping_step1_
notifications_broadcast.sql`-এর আগের reconstruction-এও DB-এর আসল কনটেন্টের সাথে কিছু ছোট syntax
পার্থক্য ছিল (RLS policy-তে `(select realtime.topic())`/`(select auth.uid())` performance-প্যাটার্ন
মিসিং ছিল, আর `drop trigger if exists` লাইনটা ছিল না)।

### ✅ ফিক্স করা হলো

1. `realtime_scoping_step1_notifications_broadcast.sql` — DB-এর `supabase_migrations.
   schema_migrations.statements`-এর আসল কনটেন্ট দিয়ে হুবহু ঠিক করা হলো।
2. `realtime_scoping_step2_wallet_broadcast.sql` (নতুন) — DB থেকে হুবহু reconstruct করে যোগ করা
   হলো।
3. `realtime_scoping_step2_wallet_event_naming_fix.sql` (নতুন) — DB থেকে হুবহু reconstruct করে
   যোগ করা হলো।

### ✅ ধাপ ২-এর SQL-টেস্ট (নিয়ম #৮, আগের সেশনে করা হয়নি/রেকর্ড ছিল না — এই সেশনে করা হলো)

`transactions`/`withdrawals`/`gateway_payments`/`additional_charges` — dev DB-তে এই ৪টা টেবিলই
বর্তমানে **খালি** (০ row প্রতিটাতে), তাই real-row insert/update দিয়ে trigger fire করানো যায়নি।
বদলে `realtime.broadcast_changes()` সরাসরি কল করে (ঠিক trigger function-গুলো যা করে, একই
argument-pattern, দুইটা ভিন্ন test topic দিয়ে) mechanism-level verify করা হলো:

- ৪টা টেবিলের table-prefixed event নাম (`transactions_INSERT`, `withdrawals_UPDATE`,
  `gateway_payments_INSERT`, `additional_charges_DELETE`) — সবগুলোই সঠিক topic-এ, সঠিক নামে
  `realtime.messages`-এ পৌঁছেছে। ✅
- কোনো cross-table event-name collision হয়নি, notifications-এর bare event নামের সাথেও কোনো
  সংঘর্ষ হয়নি। ✅
- ⚠️ real row দিয়ে end-to-end টেস্ট (আসল trigger fire করিয়ে) এখনো বাকি — dev DB-তে ডেটা না থাকায়
  সম্ভব হয়নি। পরের সেশনে/ব্যবহারকারীর real device টেস্টে অগ্রাধিকার দিয়ে verify করা উচিত।

### ✅ Kotlin-সাইড (`SupabaseRealtimeManager.kt`) — ধাপ ২ কোড এই সেশনে লেখা হলো

`startUserTopicBroadcastSubscription()`-এ (ধাপ ১-এর notifications-এর মতোই, **একই** `user:$userId`
channel পুনর্ব্যবহার করে, নতুন channel না) ৪টা নতুন broadcastFlow listener যোগ হলো —
`transactions_$op`/`withdrawals_$op`/`gateway_payments_$op`/`additional_charges_$op` (op =
INSERT/UPDATE/DELETE), প্রতিটার নিজস্ব payload data class (`TransactionChangeBroadcastPayload`
ইত্যাদি) আর apply-ফাংশন (`applyTransactionBroadcastChange` ইত্যাদি) — বিদ্যমান DAO দিয়ে idempotent
upsert/delete করে, ঠিক পুরনো `handleTransactionAction`/`handleWithdrawalAction`/
`handleGatewayPaymentAction`/`handleAdditionalChargeAction` (postgresChangeFlow পথ)-এর মতোই
আচরণ। পুরনো টেবিল-ওয়াইড channel ৪টা (dual-run) অক্ষত রাখা হয়েছে, admin session-এ নতুন পথ চালু হয়
না (আগের মতোই `isCurrentSessionAdmin()` check পুনর্ব্যবহার)।

**⚠️ এখনো যা verify করা হয়নি:** Kotlin build/compile (আগের ধাপগুলোর মতোই এই sandbox-এ সম্ভব না —
শুধু bracket/paren balance ম্যানুয়ালি চেক করা হয়েছে, ২৯৭/২৯৭ braces, ৮১৫/৮১৫ parens, ৩৩/৩৩ bracket
মিলেছে), real device/real-row end-to-end টেস্ট, আর "Allow public access" Realtime সেটিং (নিয়ম #৭,
এখনো Dashboard-এ গিয়ে সরাসরি চেক করা হয়নি — এখনো পেন্ডিং, আগের সেশনগুলোর মতোই)।

### 📦 এই সেশনে ফাইল পরিবর্তন
- `supabase/migrations/realtime_scoping_step1_notifications_broadcast.sql` — DB-এর আসল কনটেন্ট
  দিয়ে সংশোধন।
- `supabase/migrations/realtime_scoping_step2_wallet_broadcast.sql` — নতুন (DB থেকে reconstruct)।
- `supabase/migrations/realtime_scoping_step2_wallet_event_naming_fix.sql` — নতুন (DB থেকে
  reconstruct)।
- `app/src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt` — ধাপ ২-এর dual-run
  broadcast subscription কোড যোগ (ফাইলের শীর্ষ docstring-ও আপডেট করা হলো)।
- এই প্রোগ্রেস ফাইলে এই এন্ট্রি যোগ।

### 🔜 পরের ধাপ (ধাপ ৩ — Chat/`messages`)
ধাপ ৩-এ `messages` টেবিলের জন্য `problem:<problem_id>` topic-ভিত্তিক broadcast (owner ও solver
দুই পক্ষই একই topic থেকে পাবে) বসানো হবে — এখনো শুরু হয়নি। এর আগে ব্যবহারকারীকে "Allow public
access" সেটিং আর real-device টেস্ট নিয়ে সিদ্ধান্ত জানাতে হবে চাইলে।

---

## ✅ "Allow public access" বন্ধ করা হয়েছে (নিয়ম #৭ সম্পন্ন)

ব্যবহারকারী নিজে Supabase Dashboard-এ গিয়ে **Project Settings → Realtime → "Allow public
access"** বন্ধ করে দিয়েছেন (ব্যবহারকারীর নিশ্চিতকরণ অনুযায়ী)। এটা master-প্রম্পটের নিয়ম #৭-এর
বাধ্যতামূলক এককালীন কাজ ছিল, যা ধাপ ১-২ পর্যন্ত পেন্ডিং হিসেবে ফ্ল্যাগ করা ছিল — এখন সম্পন্ন।
পরের প্রতিটা ধাপে এটা যেন ভুলে আবার চালু না হয়ে যায় সেটা মনে রাখতে হবে (নিয়ম #৭-এর অংশ)।

---

## ✅ ধাপ ৩ — Chat (`messages`) — সম্পন্ন (DB-সাইড apply+টেস্ট, Kotlin-সাইড কোড লেখা ও ওয়্যার করা হয়েছে)

**Topic-scheme:** `problem:<problem_id>` — resource-based (user-based না), তাই owner ও
accepted_solver উভয়েই একই topic থেকে পাবে, dual-broadcast লাগে না।

### DB-সাইড (এই সেশনেই Supabase MCP দিয়ে সরাসরি apply করা হলো, migration
`realtime_scoping_step3_messages_broadcast.sql`, version `20260913...` — নিচে বিস্তারিত)

1. **RLS policy** (`realtime.messages`-এ) — `"problem participants can receive problem-topic
   broadcasts"`: topic `problem:%` প্যাটার্নের হলে, caller admin অথবা সেই problem-এর owner/
   accepted_solver হলেই allow। এটা `public.messages`-এর বিদ্যমান `messages_select` RLS policy-র
   owner/accepted_solver লজিকের সাথে **ইচ্ছাকৃতভাবে সামঞ্জস্যপূর্ণ** রাখা হয়েছে (সাধারণ bidder,
   accepted হওয়ার আগে, প্রকৃত chat message-ও দেখে না — তাই broadcast topic-ও তাদের জন্য খোলা হয়নি,
   over-authorization এড়াতে)।
2. **Trigger function + trigger** (`notify_messages_broadcast`/`broadcast_messages_changes`,
   `after insert or update or delete`) — `'problem:' || problem_id` topic-এ, event নাম
   **শুরু থেকেই table-prefixed** (`'messages_' || TG_OP`) — কারণ ধাপ ৪-এ `bids`-ও এই একই
   `problem:<id>` topic ব্যবহার করবে (roadmap অনুযায়ী), তাই ধাপ ২-তে wallet গ্রুপে যে bare-TG_OP
   collision বাগ পাওয়া গিয়েছিল সেটা এবার প্রথম থেকেই এড়ানো হলো, আলাদা "fix" migration লাগবে না।

**SQL-টেস্ট (নিয়ম #৮):**
- `messages`/`problems` — dev DB-তে দুটোই বর্তমানে **খালি**, তাই real-row insert/update দিয়ে
  trigger fire করানো যায়নি (ধাপ ২-এর মতোই সীমাবদ্ধতা)।
- বদলে `realtime.broadcast_changes()` সরাসরি কল করে দুইটা ভিন্ন problem topic
  (`problem:AAAA1111`/`problem:BBBB2222`) দিয়ে mechanism-level verify করা হলো — ✅ কোনো cross-talk
  হয়নি, প্রতিটা টপিক-এ শুধু তার নিজের event-ই গেছে, event নাম (`messages_INSERT`) সঠিক।
- RLS policy-র positive/negative simulation (real problem row দিয়ে) dev DB-তে `problems` টেবিল
  খালি থাকায় করা যায়নি — policy-র subquery লজিক `messages_select`-এর ইতিমধ্যে-verified লজিকের
  সাথে হুবহু মিলিয়ে লেখা হয়েছে বলে সেই ভরসাতেই এগোনো হলো, তবে **এটা এখনো real-data দিয়ে
  ভেরিফাই করা বাকি** — পরের সেশনে/real device টেস্টে অগ্রাধিকার দিতে হবে।

### Kotlin-সাইড (`SupabaseRealtimeManager.kt` + wiring) — এই সেশনেই লেখা ও ওয়্যার করা হলো

- `SupabaseRealtimeManager.kt`: `joinProblemMessagesBroadcastChannel(problemId)`/
  `leaveProblemMessagesBroadcastChannel(problemId)` — ঠিক বিদ্যমান `joinTypingChannel`/
  `leaveTypingChannel`-এর idempotent map প্যাটার্ন অনুসরণ করে (আলাদা map, `messagesBroadcastChannels`),
  `messages_INSERT/UPDATE/DELETE` তিনটা event শোনে, `applyMessageBroadcastChange()`-এ
  পুরনো `handleMessageAction`-এর মতোই isRead-রেগ্রেশন-গার্ড সহ upsert/delete করে। `stopRealtimeListeners()`-
  এ (logout leak-guard) সব খোলা problem-channel বন্ধ করার লজিকও যোগ হয়েছে।
- **dynamic wiring (এই ধাপের সবচেয়ে গুরুত্বপূর্ণ অংশ, screen lifecycle-বাউন্ড):**
  - `SomadhanRepository.kt` — `joinProblemChatBroadcast`/`leaveProblemChatBroadcast` (suspend,
    runCatching non-fatal, typing-এর মতোই dual-run-safe)।
  - `SomadhanViewModel.kt` — `joinProblemChatBroadcast`/`leaveProblemChatBroadcast` (non-suspend
    wrapper, `viewModelScope.launch` — কারণ `DisposableEffect`-এর `onDispose` non-suspend lambda,
    `setTypingStatus`-এর প্যাটার্নই অনুসরণ করা হলো)।
  - `ChatScreen.kt` — বিদ্যমান `LaunchedEffect(problemId) { viewModel.selectProblem(...) ... }`
    ব্লকে `viewModel.joinProblemChatBroadcast(problemId)` যোগ, আর বিদ্যমান
    `DisposableEffect(problemId) { onDispose { viewModel.setTypingStatus(problemId, false) } }`
    ব্লকে `viewModel.leaveProblemChatBroadcast(problemId)` যোগ — dual-run, পুরনো কোনো লাইন
    সরানো/বদলানো হয়নি, শুধু পাশে নতুন কল যোগ হয়েছে।

**⚠️ এখনো যা verify করা হয়নি:** Kotlin build/compile (আগের ধাপগুলোর মতোই sandbox-এ সম্ভব না —
bracket/paren balance ম্যানুয়ালি চেক করা হয়েছে ৪টা বদলানো ফাইলেই, সব মিলেছে — `SomadhanRepository.kt`-
তে paren-count crude script-এ ৩টা mismatch দেখাচ্ছিল কিন্তু সেটা এই সেশনের এডিটের আগেও ছিল, Bengali
মন্তব্যের ভেতরের literal বন্ধনী থেকে, তাই এই সেশনের এডিট দায়ী না — নতুন যোগ হওয়া `(`/`)` ঠিক ১১/১১
মিলেছে); RLS policy-র real-data positive/negative test; real device end-to-end (দুই problem-এ দুই
device দিয়ে cross-talk-না-হওয়া)।

### 📦 এই সেশনে ফাইল পরিবর্তন (ধাপ ৩)
- `supabase/migrations/realtime_scoping_step3_messages_broadcast.sql` — নতুন, DB-তে apply করা
  হয়েছে।
- `app/src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt` — ধাপ ৩-এর broadcast
  join/leave/apply কোড + docstring আপডেট।
- `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — `joinProblemChatBroadcast`/
  `leaveProblemChatBroadcast` যোগ।
- `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt` — একই নামে non-suspend wrapper
  যোগ।
- `app/src/main/java/com/example/ui/screens/ChatScreen.kt` — `LaunchedEffect`/`DisposableEffect`-এ
  join/leave কল যোগ।
- এই প্রোগ্রেস ফাইলে "Allow public access বন্ধ" + "ধাপ ৩" এন্ট্রি যোগ।

### 🔜 পরের ধাপ (ধাপ ৪ — `bids`)
Topic আবারও `problem:<problem_id>` (ধাপ ৩-এর RLS policy পুনর্ব্যবহারযোগ্য কিনা প্রথমে যাচাই করতে
হবে — সম্ভবত হ্যাঁ, যেহেতু policy টপিক-প্যাটার্নভিত্তিক, টেবিল-নির্দিষ্ট না)। Trigger: bid submit/
accept/reject/cancel-এ broadcast, event নাম `bids_$op` (table-prefixed, ধাপ ৩-এর কনভেনশন অনুসরণ
করে, একই topic-এ messages-এর সাথে collision এড়াতে)। Kotlin-সাইডে ProblemDetailScreen খোলা থাকা
অবস্থায় dynamic subscribe (ঠিক ধাপ ৩-এর ChatScreen প্যাটার্নের মতোই)।

---

## ✅ ধাপ ৪ — Bids (`bids`) — সম্পন্ন (DB-সাইড আগের সেশনেই apply হয়েছিল + এই সেশনে duplicate-policy
cleanup, Kotlin-সাইড কোড এই সেশনে লেখা ও ওয়্যার করা হয়েছে)

**সেশনের সূত্রপাত:** ব্যবহারকারী `somadhan-realtime-scoping-step3-done.zip` (ধাপ ৩ পর্যন্ত) আপলোড
করে বলেন কাজ চালিয়ে যেতে, এবং মনে করিয়ে দেন — **সব সিস্টেম Firebase-এ যেমন ছিল ঠিক তেমনই হতে
হবে** (functional parity)। এই নীতিটা এই ধাপে বিশেষভাবে প্রাসঙ্গিক হয়ে উঠলো, নিচে দেখুন।

### 🔍 প্রথমে যা পাওয়া গেল — DB আবারও repo-র চেয়ে এগিয়ে ছিল (ধাপ ২-এর মতোই একই ধরনের gap)

`Supabase:list_migrations` চালিয়ে দেখা গেল DB-তে ধাপ ৪-এর migration **ইতিমধ্যে ৩ বার** apply করা
ছিল (একই নাম `realtime_scoping_step4_bids_broadcast`, ভার্সন `20260913130104` →
`20260913130559` → `20260913131810` — প্রতিটাই আগের ভার্সনের refinement), কিন্তু এই zip-এ
(`supabase/migrations/`) এবং আগের progress-এন্ট্রিতে এর কোনো উল্লেখ ছিল না — অর্থাৎ কোনো আগের
সেশনে DB-তে apply করা হলেও zip/progress-ফাইল কখনো deliver হয়নি।

তিনটা ভার্সনের ডিফ পড়ে বোঝা গেল **ঠিক কী সিদ্ধান্ত নেওয়া হয়েছিল**, আর এটাই ব্যবহারকারীর আজকের
"Firebase-এর মতোই" রিমাইন্ডারের প্রেক্ষাপট:

- **ভার্সন ১ (`130104`):** RLS policy `bids_select`-এর owner/নিজের-bid/admin অংশ কভার করেছিল, কিন্তু
  policy-র মন্তব্যে OPEN+public visibility clause-টা আলাদা `exists` ব্লকে ছিল।
- **ভার্সন ২ (`130559`):** policy পুনর্লিখে সবগুলো শর্ত একটাই nested `exists (problems p where ...)`
  ব্লকে আনা হলো (bids_select-এর structure-এর সাথে আরও কাছাকাছি মিলিয়ে), কিন্তু policy-র নাম বদলে
  ফেলায় ভার্সন-১-এর policy DB-তে **drop না করেই রয়ে গিয়েছিল** (নিচে দেখুন, এই সেশনে ধরা পড়েছে)।
- **ভার্সন ৩ (`131810`, চূড়ান্ত):** যুক্তি অভিন্নই রইলো, শুধু মন্তব্য আপডেট হলো স্পষ্ট করে বলতে যে
  ব্যবহারকারীকে ৩টা ডিজাইন-অপশন দিয়ে জিজ্ঞেস করা হয়েছিল, আর ব্যবহারকারী **"Firebase-এর মতোই সিস্টেম"
  (broad OPEN+public visibility parity)** বেছে নিয়েছিলেন — ঠিক যেমন `ProblemDetailScreen.kt`-এ ধাপ
  ২৩-এর কমেন্টে আগে থেকেই নথিভুক্ত আছে (`bids_select` RLS policy-ও একই কারণে "Firebase-এর মতোই
  সম্পূর্ণ ফাংশনালিটি চাই" রিমাইন্ডারে broaden করা হয়েছিল, যেকোনো visitor OPEN+public problem-এর
  সব বিড দেখতে পান)।

**Topic-ডিজাইন:** `problem:<problem_id>:bids` — roadmap-এ প্রস্তাবিত `problem:<problem_id>` (messages-
এর সাথে শেয়ার্ড) থেকে **ইচ্ছাকৃতভাবে সরে আসা হয়েছে**, কারণ bids-এর visibility (OPEN+public হলে যে
কেউ) messages-এর visibility (শুধু owner/accepted_solver) থেকে বেশি broad — একই topic-এ RLS policy-টা
broaden করলে non-accepted bidder/public visitor-রাও chat broadcast পড়তে পারতো, প্রাইভেসি-রিগ্রেশন
হতো (নিয়ম #৯ অনুযায়ী চিহ্নিত ও এড়ানো)। তাই আলাদা topic + আলাদা RLS policy, শর্ত `bids_select`-এর
সাথে হুবহু মিলিয়ে।

### ✅ এই সেশনে DB-সাইড cleanup + টেস্ট

1. **Duplicate-policy পরিষ্কার করা হলো**: `pg_policies` চেক করে দেখা গেল ভার্সন-১-এর policy
   (`"bid participants can receive problem-bids-topic broadcasts"`) সত্যিই এখনো DB-তে ছিল,
   ভার্সন-২/৩-এর `"problem bids visibility broadcasts"`-এর পাশাপাশি — দুটোই কার্যত-অভিন্ন লজিক
   (RLS policy-গুলো SELECT-এ OR হয়ে যায় বলে এটা কোনো নিরাপত্তা-বাগ ছিল না, শুধু messy duplicate)।
   নতুন migration `realtime_scoping_step4_bids_drop_orphan_policy` apply করে পুরনোটা drop করা হলো,
   verify করা হয়েছে এখন শুধু ৩টা policy আছে (`user:*`, `problem:*` (messages), `problem:*:bids`)।
2. **Trigger/function ডুপ্লিকেট-চেক**: `public.bids`-এ ঠিক দুইটা trigger আছে
   (`broadcast_bids_changes` — নতুন, আর `on_bid_inserted` — পুরনো, স্কোপের বাইরে/আলাদা কাজ), আর
   `notify_bids_broadcast()` ফাংশন ঠিক একবারই আছে — কোনো duplicate trigger/function নেই
   (`create or replace`/`drop ... if exists` দিয়ে প্রতিটা re-apply idempotent ছিল)।
3. **SQL-টেস্ট (নিয়ম #৮)**: `bids`/`problems` dev DB-তে খালি থাকায় (ধাপ ২-৩-এর মতোই সীমাবদ্ধতা)
   real-row trigger fire সম্ভব হয়নি। বদলে `realtime.broadcast_changes()` সরাসরি দুইটা ভিন্ন
   problem-topic (`problem:AAAA1111:bids` INSERT, `problem:BBBB2222:bids` UPDATE) দিয়ে সঠিক
   `public.bids` কলাম-টাইপ মিলিয়ে row-record বানিয়ে (14 কলাম, `ROW(...)::public.bids`) call করা
   হলো — ✅ কোনো cross-talk হয়নি, প্রতিটা টপিকে শুধু তার নিজের event গেছে, payload-এর
   `problem_id`/`status` সঠিক।
4. **RLS policy real-data simulation** — `public.users`-এ `auth.users`-এর সাথে FK থাকায় সিন্থেটিক
   test-user insert সম্ভব হয়নি (FK violation), তাই real-data positive/negative simulation এই
   সেশনেও করা যায়নি (ধাপ ৩-এর মতোই সীমাবদ্ধতা)। বদলে policy-র condition structurally `bids_select`
   policy-র (`pg_policies` দিয়ে সরাসরি পড়ে) সাথে পাশাপাশি মিলিয়ে ভেরিফাই করা হলো — দুটোই যুক্তিগতভাবে
   সমতুল্য (topic-policy-তে "এই problem-এ আমার কোনো বিড আছে" `exists` subquery দিয়ে চেক হয়, যেটা
   bids_select-এর সরাসরি `solver_id = auth.uid()` কলাম-চেকের রিসোর্স-লেভেল সমতুল্য)। ⚠️ real-data
   দিয়ে positive/negative test এখনো বাকি — পরের সেশনে/real device টেস্টে অগ্রাধিকার দিতে হবে।

### ✅ Kotlin-সাইড (`SupabaseRealtimeManager.kt` + wiring) — এই সেশনেই লেখা ও ওয়্যার করা হলো

- `SupabaseRealtimeManager.kt`: `joinProblemBidsBroadcastChannel(problemId)`/
  `leaveProblemBidsBroadcastChannel(problemId)` — ঠিক ধাপ ৩-এর `joinProblemMessagesBroadcastChannel`-
  এর idempotent map প্যাটার্ন অনুসরণ করে (আলাদা map, `bidsBroadcastChannels`, আলাদা topic
  `problem:<id>:bids`), `bids_INSERT/UPDATE/DELETE` তিনটা event শোনে, `applyBidBroadcastChange()`-এ
  পুরনো `handleBidAction`-এর মতোই idempotent upsert/delete করে। `stopRealtimeListeners()`-এ
  (logout leak-guard) সব খোলা bids-channel বন্ধ করার লজিকও যোগ হয়েছে।
- **dynamic wiring:**
  - `SomadhanRepository.kt` — `joinProblemBidsBroadcast`/`leaveProblemBidsBroadcast` (suspend,
    runCatching non-fatal, ধাপ ৩-এর chat-প্যাটার্নের মতোই dual-run-safe)।
  - `SomadhanViewModel.kt` — একই নামে non-suspend wrapper (`viewModelScope.launch`)।
  - `ProblemDetailScreen.kt` — বিদ্যমান `LaunchedEffect(problemId) { ... viewModel.selectProblem(...) }`
    ব্লকে `viewModel.joinProblemBidsBroadcast(problemId)` যোগ, আর নতুন
    `DisposableEffect(problemId) { onDispose { viewModel.leaveProblemBidsBroadcast(problemId) } }`
    যোগ করা হলো (এই ফাইলে আগে কোনো `DisposableEffect` ছিল না) — dual-run, পুরনো কোনো লাইন সরানো/
    বদলানো হয়নি, শুধু পাশে নতুন কল যোগ হয়েছে। যেকোনো ভিজিটর (শুধু owner না) এই স্ক্রিন খুললে
    subscribe হবে — bids_select-এর broad (Firebase-parity) visibility-র সাথে সামঞ্জস্যপূর্ণ।

**⚠️ এখনো যা verify করা হয়নি:** Kotlin build/compile (sandbox-এ সম্ভব না — bracket/paren balance
ম্যানুয়ালি চেক করা হয়েছে ৪টা বদলানো ফাইলেই; `SomadhanRepository.kt`-তে crude paren-script ৩টা
mismatch দেখাচ্ছিল কিন্তু সেটা যাচাই করে নিশ্চিত হওয়া গেছে এই সেশনের এডিটের **আগে থেকেই** ছিল
(uploaded zip-এও একই ৩-মিসম্যাচ পাওয়া গেছে, Bengali কমেন্টের ভেতরের literal বন্ধনী থেকে) — এই
সেশনের নতুন যোগ হওয়া অংশ নিজে ১০/১০ প্যারেন মিলেছে); RLS policy real-data positive/negative test;
real device end-to-end (দুই problem-এ দুই device দিয়ে cross-talk-না-হওয়া, submit/accept/reject/
cancel চারটা event-টাইপই আলাদাভাবে)।

### 📦 এই সেশনে ফাইল পরিবর্তন (ধাপ ৪)
- DB migration `realtime_scoping_step4_bids_drop_orphan_policy` — নতুন apply (duplicate policy
  cleanup)।
- `supabase/migrations/realtime_scoping_step4_bids_broadcast.sql` — নতুন, DB-র চূড়ান্ত
  (`20260913131810`) ভার্সন অনুযায়ী repo-তে reconstruct করে যোগ করা হলো (আগের সেশনগুলোর gap পূরণ)।
- `supabase/migrations/realtime_scoping_step4_bids_drop_orphan_policy.sql` — নতুন, এই সেশনের
  cleanup migration repo-তে যোগ।
- `app/src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt` — ধাপ ৪-এর broadcast
  join/leave/apply কোড।
- `app/src/main/java/com/example/data/repository/SomadhanRepository.kt` — `joinProblemBidsBroadcast`/
  `leaveProblemBidsBroadcast` যোগ।
- `app/src/main/java/com/example/ui/viewmodel/SomadhanViewModel.kt` — একই নামে non-suspend wrapper।
- `app/src/main/java/com/example/ui/screens/ProblemDetailScreen.kt` — `LaunchedEffect`-এ join কল +
  নতুন `DisposableEffect`-এ leave কল।
- এই প্রোগ্রেস ফাইলে "ধাপ ৪" এন্ট্রি যোগ।

### 🔜 পরের ধাপ
ধাপ ৪ পর্যন্ত (notifications, wallet-গ্রুপ, chat, bids) সব DB+Kotlin-সাইড করা হয়ে গেছে। এখন
ব্যবহারকারীকে জিজ্ঞেস করতে হবে: **ধাপ ৫ (ঐচ্ছিক — `users`/`escrows`)** করতে চান, নাকি সরাসরি
**ধাপ ৬ (cleanup — পুরনো postgresChangeFlow subscription সরানো + চূড়ান্ত QA)**-এ যেতে চান।

---

## 🔍 Firebase-parity অডিট (ধাপ ৪-এর পরে, ব্যবহারকারীর অনুরোধে) — ৪টা টেবিল ক্লিন, `messages`-এ একটা গ্যাপ পাওয়া গেছে

**সূত্রপাত:** ব্যবহারকারী জিজ্ঞেস করলেন ধাপ ১-৩ (notifications/wallet/messages) আসল Firebase-এর
মতোই আচরণ করছে কিনা। DB-তে সরাসরি প্রতিটা টেবিলের deployed trigger function + broadcast topic RLS
policy বের করে সেই টেবিলের বিদ্যমান table-level SELECT (ও প্রাসঙ্গিক ক্ষেত্রে UPDATE) RLS policy-র
সাথে লাইন-বাই-লাইন মিলিয়ে যাচাই করা হলো (নোট: মূল Firebase-এর `firestore.rules` ফাইল প্রজেক্ট থেকে
আগেই ডিলিট হয়ে গেছে, তাই direct Firebase-rules-diff সম্ভব হয়নি — বদলে Supabase-সাইড table-level RLS
policy-কেই "Firebase-parity বজায় রাখার জন্য ইতিমধ্যে-সঠিক রেফারেন্স" হিসেবে ধরা হয়েছে, যেহেতু মূল
migration ধাপ ৩৪ পর্যন্ত সেটাই Firebase-এর সাথে মিলিয়ে বানানো হয়েছিল বলে নথিভুক্ত আছে)।

### ✅ ক্লিন — কোনো গ্যাপ পাওয়া যায়নি

- **`notifications`**: trigger `'user:' || NEW.user_id` (bare event নাম) ↔ `notifications_select`
  (`user_id = auth.uid() OR is_admin`) — হুবহু মেলে।
- **`transactions`**: trigger dual-broadcast (`user_id`+`solver_id`, উভয় কলামই NULL-চেক সহ) ↔
  `transactions_select` (`user_id OR solver_id OR is_admin`) — হুবহু মেলে।
- **`withdrawals`**: trigger `'user:' || NEW.solver_id` ↔ `withdrawals_select`
  (`solver_id OR is_admin`) — হুবহু মেলে।
- **`gateway_payments`**: trigger `'user:' || NEW.user_id` (NULL-চেক সহ) ↔ `gateway_payments_select`
  (`user_id OR is_admin`) — হুবহু মেলে।
- **`additional_charges`**: trigger dual-broadcast (`user_id`+`solver_id`, সরাসরি কলাম, কোনো
  `problems`-JOIN লাগেনি) ↔ `additional_charges_select` (`user_id OR solver_id OR is_admin`) — হুবহু
  মেলে। (নোট: মাস্টার-প্রম্পটের ধাপ ২-এর নির্দেশনায় ভুলভাবে ধরে নেওয়া হয়েছিল যে owner_id পেতে
  `problems`-টেবিলে JOIN লাগবে — কিন্তু `additional_charges`-এ সরাসরি `user_id` কলাম আছে, আর deployed
  trigger সেটাই সঠিকভাবে ব্যবহার করেছে, কোনো bug না।)
- **Event-নাম collision-চেক**: `notifications` bare `INSERT`/`UPDATE`/`DELETE` ব্যবহার করে, wallet-
  গ্রুপের ৪টা টেবিল table-prefixed (`transactions_INSERT` ইত্যাদি) — একই `user:<id>` topic-এ থাকা
  সত্ত্বেও Kotlin-সাইড `broadcastFlow(event = ...)` exact-match হওয়ায় কোনো cross-listen হয় না —
  ✅ সঠিক, আগের সেশনের ডিজাইন-সিদ্ধান্ত অনুযায়ীই।

### ⚠️ গ্যাপ পাওয়া গেছে — `messages`

উপরের "🚩 পরের সেশনের জন্য প্রথম কাজ" এন্ট্রিতে (ফাইলের শুরুতে) পুরো detail লেখা আছে — সারসংক্ষেপ:
`messages_select`-এর sender_id/receiver_id-ভিত্তিক শর্ত broadcast topic RLS-এ কপি হয়নি, ফলে solver-
reassignment-এর edge case-এ ex-participant-এর পুরনো chat thread-এর live broadcast miss হতে পারে।
**ব্যবহারকারীর নির্দেশ অনুযায়ী এই সেশনে ফিক্স প্রয়োগ করা হয়নি** — শুধু নোট করে রাখা হলো, পরের
সেশনের প্রথম কাজ হিসেবে (ফাইলের শুরুর 🚩 এন্ট্রি দেখো, প্রস্তাবিত SQL fix-সহ)।

### 📦 এই সেশনে ফাইল পরিবর্তন
- শুধু এই progress ফাইলে দুটো নতুন এন্ট্রি (ফাইলের শুরুতে 🚩 flag + এই অডিট-বিস্তারিত এন্ট্রি) —
  কোনো Kotlin/SQL কোড এই সেশনে বদলানো হয়নি (নিয়ম #৯: audit শুধু চালানো হয়েছে, ফিক্স প্রয়োগ করা হয়নি,
  ব্যবহারকারীর explicit অনুমতি ছাড়া)।

---

## ✅ Messages সলভার থ্রেড আইসোলেশন + Detail-স্ক্রিনে বাতিল-বিড নোটিশ (ধাপ ৪-এর পরের সেশন)

**প্রেক্ষাপট:** ব্যবহারকারী নতুন করে জানতে চাইলেন/চাইলেন — কোনো solver-এর bid বাতিল হয়ে অন্য
solver accept হলে (ক) নতুন solver যেন পুরনো solver-এর সাথে owner-এর পুরনো চ্যাট না দেখতে পারে
(owner/admin অবশ্যই সব দেখবে), এবং (খ) পুরনো solver নিজে সেই পোস্ট খুললে যেন একটা নোটিশ দেখে যে
সে এই পোস্টে বিড বাতিল করেছে।

### 🔍 রুট-কজ যাচাই (DB সরাসরি চেক করে)

`messages_select` (table RLS) verify করে দেখা গেল — `sender_id`/`receiver_id`/`admin` ছাড়াও একটা
৪র্থ শর্ত ছিল: `exists (problems p where p.id = messages.problem_id and (p.user_id = auth.uid()
or p.accepted_solver_id = auth.uid()))`। এখানে **`accepted_solver_id` অংশটা** sender/receiver
filter না করেই পুরো problem-এর সব মেসেজ পড়ার অনুমতি দিচ্ছিল — মানে বর্তমান accepted solver
আগের/পুরনো সব solver-এর সাথে owner-এর চ্যাটও দেখতে পারতো। এটাই ছিল আসল বাগ, এবং এটাই আগের সেশনের
audit-এ পাওয়া broadcast RLS গ্যাপের (sender/receiver শর্ত অনুপস্থিত) মূল কারণ — broadcast policy
সেই একই ভুল table policy-র সাথে "মিরর" করে বসানো হয়েছিল।

### 🧪 SQL-টেস্ট (নিয়ম #৮) — ফিক্সের আগে ও পরে

সিন্থেটিক ডেটা দিয়ে (এক owner, দুই solver, দুই থ্রেড — একটা transaction-এ বানিয়ে শেষে rollback,
প্রোডাকশন ডেটা অক্ষত) `set local role authenticated; set local request.jwt.claim.sub` দিয়ে
সিমুলেশন:

- **ফিক্সের আগে:** old_solver_sees = ১, **new_solver_sees = ২ (বাগ কনফার্ম — পুরনো থ্রেডও দেখছিল)**,
  owner_sees = ২।
- **ফিক্সের পরে:** old_solver_sees = ১, **new_solver_sees = ১ (ঠিক হয়ে গেছে)**, owner_sees = ২
  (অপরিবর্তিত)।

### 📦 এই সেশনে ফাইল পরিবর্তন

- **নতুন migration** `supabase/migrations/realtime_scoping_messages_solver_thread_isolation.sql`
  (Supabase MCP দিয়ে সরাসরি DB-তে apply করা হয়ে গেছে, `apply_migration` টুল দিয়ে):
  1. `public.messages`-এর `messages_select` policy পুনর্লিখন — `accepted_solver_id`-ভিত্তিক
     ব্লানকেট শর্ত সরিয়ে শুধু `p.user_id = auth.uid()` (owner) + sender_id/receiver_id/admin।
  2. `realtime.messages`-এর `"problem participants can receive problem-topic broadcasts"`
     broadcast policy একইভাবে পুনর্লিখন — `accepted_solver_id` শর্তের বদলে
     `exists (messages m where m.problem_id = p.id and (m.sender_id/receiver_id = auth.uid()))`।
  3. `messages_insert` policy **স্পর্শ করা হয়নি** — মেসেজ পাঠানো এখনো owner/বর্তমান
     accepted_solver-এর জন্যই সীমাবদ্ধ, শুধু read/broadcast visibility বদলেছে। (ব্যবহারকারী
     স্পষ্টভাবে জিজ্ঞেস করেছিলেন এতে sending ভাঙবে কিনা — না, ভাঙে না।)
- **`app/src/main/java/com/example/ui/screens/ProblemDetailScreen.kt`**:
  - `myEndedBid` নামে একটা derived val যোগ — `currentUser.role == "SOLVER"` এবং সে বর্তমান
    `accepted_solver_id` না হলে, তার নিজের CANCELLED/WITHDRAWN/REJECTED বিড (যদি থাকে) খুঁজে বের
    করে।
  - Status/category row আর "Problem Title"-এর মাঝখানে একটা নোটিশ Card যোগ — bid status অনুযায়ী
    ("বিড বাতিল/প্রত্যাহার করেছেন" / "বিড প্রত্যাহার করেছেন" / "ক্লায়েন্ট গ্রহণ করেননি") টেক্সট +
    একটা সতর্কতা লাইন ("এই পোস্টের সাম্প্রতিক তথ্য/আপডেট এখানে নাও দেখাতে পারে") — কারণ RLS ফিক্সের
    পর এই পোস্টের নতুন কোনো লাইভ আপডেট আর পুরনো solver-এর ফোনে sync হবে না, তাই local cache-এ যা
    আছে সেটাই দেখাবে, স্থবির (stale) হতে পারে। বিদ্যমান UI প্যাটার্ন (`SomadhanOrange`/
    `SomadhanTextHint`/`Icons.Default.Info` কার্ড স্টাইল) অনুসরণ করে লেখা, কোনো নতুন import লাগেনি।
  - অন্য কোনো লজিক/ফাংশন স্পর্শ করা হয়নি (surgical change)।
- এই progress ফাইলে: শুরুর "🚩 পরের সেশনের জন্য প্রথম কাজ" এন্ট্রি "✅ resolved" পয়েন্টার-এ বদলানো
  হয়েছে, আর এই বিস্তারিত এন্ট্রি যোগ হলো।

### ⚠️ এখনো যা ভেরিফাই করা হয়নি

- **Kotlin কোড build/compile করা যায়নি** (এই sandbox-এ network/Gradle সুবিধা নেই) — শুধু ম্যানুয়াল
  ব্র্যাকেট/প্যারেন-ব্যালেন্স ও ইম্পোর্ট-নির্ভরতা চোখে দেখে যাচাই করা হয়েছে। প্রথম Android Studio
  build-এ compile error এলে জানাবেন।
- **local Room cache-এর "stale data" আচরণ** নিজে ভেরিফাই করা হয়নি (রিয়াল ডিভাইসে/ইমুলেটরে) —
  কোড-লেভেলে দেখা গেছে `selectProblem()` লোকাল DB থেকে পড়ে, তাই নোটিশে যা লেখা আছে সেটা যুক্তিসঙ্গত
  অনুমান, কিন্তু বাস্তবে reassignment-এর পর ঠিক কী দেখাবে তা real-device টেস্ট দিয়ে নিশ্চিত করা
  ভালো।
- `problems_select` RLS (owner/current-accepted_solver/admin/OPEN+public) — cancelled-bids ট্যাবে
  পুরনো solver পোস্টের **বেসিক তথ্য** (title/status নোটিশ) দেখতে পারবে কিনা, এটা আলাদাভাবে
  আলোচনা হয়েছিল কিন্তু **এখনো কোনো সিদ্ধান্ত/পরিবর্তন হয়নি** — `problems` টেবিল মাস্টার-প্রম্পটের
  নিয়ম #৫ অনুযায়ী এই কাজের স্কোপের বাইরে, ব্যবহারকারীর স্পষ্ট অনুমতি লাগবে হাত দেওয়ার আগে।

### 🔜 পরের ধাপ

উপরের অসমাপ্ত আইটেমগুলো (Android Studio build verify, real-device stale-cache আচরণ) ছাড়া বাকি
সব কাজ শেষ। এরপর মাস্টার-প্রম্পটের ধাপ ৫ (ঐচ্ছিক — users/escrows) বা ধাপ ৬ (cleanup + QA)-এ যাওয়া
যেতে পারে — ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী।

---

## ✅ `problems_select` RLS ফিক্স — cancelled solver নিজের "বাতিল" ট্যাবে পোস্ট দেখতে পারবে (উপরের ⚠️ আইটেম সমাধান, ব্যবহারকারীর সুনির্দিষ্ট অনুমতিতে)

**প্রেক্ষাপট:** এটা মাস্টার-প্রম্পটের মূল realtime-scoping কাজের অংশ না — `problems` টেবিল
নিয়ম #৫ অনুযায়ী স্কোপের বাইরে (broadcast/topic migrate করা হয়নি, হচ্ছেও না)। এটা উপরের
"এখনো যা ভেরিফাই করা হয়নি" সেকশনে ফ্ল্যাগ করা একটা **plain SELECT RLS গ্যাপ** — realtime না,
সাধারণ `problems_select` policy। ব্যবহারকারী স্পষ্টভাবে অনুমতি দিয়ে এটা এই সেশনেই সমাধান করতে
বলেছেন। অনুরোধ: cancelled solver যেন (ক) নিজের "বাতিল" ট্যাবে পোস্টটা cancelled হিসেবে দেখতে
পায়, (খ) পোস্ট খুললে বেসিক তথ্য দেখতে পায়, কিন্তু (গ) নতুন কোনো live update এই পোস্টে না আসে।

### 🔍 রুট-কজ

`problems_select` policy আগে ছিল: owner (`user_id`) / current `accepted_solver_id` / admin /
(OPEN+public+not-deleted)। এই চারটার কোনোটাতেই পুরনো (cancelled/withdrawn/rejected bid)
solver পড়ে না, কারণ একবার অন্য solver accept হয়ে গেলে/problem আর OPEN+public থাকে না তখন
`accepted_solver_id` আর তার id না, ফলে পুরনো solver-এর client `problems.select()` করলে এই
row-ই ফেরত আসে না — Room cache-এ কখনো ঢোকেই না। এই কারণেই `SolverMyBidsScreen.kt`-এর "বাতিল"
ট্যাবে `allProblems.find { it.id == bid.problemId }` null হয়ে যায় (কোড ভেরিফাই করে দেখা গেছে,
লাইন ~775)।

### ✅ DB-সাইড ফিক্স (Supabase MCP দিয়ে সরাসরি apply + verify করা হয়েছে)

দুইটা migration লাগলো (প্রথমটায় একটা recursion বাগ ধরা পড়ে, দ্বিতীয়টায় ফিক্স):

1. **`problems_select_allow_ended_bid_solver`** — `problems_select` policy-তে নতুন শর্ত যোগ:
   `EXISTS (bids টেবিলে এই solver-এর CANCELLED/WITHDRAWN/REJECTED bid আছে কিনা)`। **সমস্যা:**
   apply করার পর টেস্ট চালাতে গিয়ে `ERROR: 42P17 infinite recursion detected in policy for
   relation "problems"` পাওয়া গেল — কারণ `bids_select` policy নিজেই `problems`-এ subquery করে
   (owner/OPEN-public চেক করতে), আর নতুন `problems_select` policy `bids`-এ subquery করছিল —
   দুটো RLS policy একে অপরকে ট্রিগার করে সার্কুলার হয়ে গেল।
2. **`fix_problems_select_bids_rls_recursion`** — সমাধান: কোডবেসে ইতিমধ্যে থাকা
   `is_admin(uuid)` ফাংশনের প্যাটার্ন অনুসরণ করে একটা নতুন `public.solver_has_ended_bid(problem_id,
   solver_id)` **SECURITY DEFINER** ফাংশন বানানো হলো (RLS বাইপাস করে টেবিল-owner হিসেবে চলে,
   তাই আর recursion হয় না) — `problems_select` policy-তে সরাসরি `bids` subquery-র বদলে এই
   ফাংশন কল বসানো হলো।

### 🧪 SQL-টেস্ট (নিয়ম #৮) — সিন্থেটিক ডেটা দিয়ে (production DB আসলে খালি ছিল — 1 user, 0
problems, 0 bids — তাও synthetic auth.users+public.users+problems+bids ঢুকিয়ে টেস্টের পর
সব মুছে/cleanup করে আগের অবস্থায় ফেরানো হয়েছে, rollback-ভিত্তিক transaction ব্যবহার করা যায়নি
কারণ multi-statement execute_sql শুধু শেষ SELECT-এর ফলাফল ফেরত দেয় — তাই setup আলাদা call-এ
কমিট করে, প্রতিটা role-switch আলাদা call-এ টেস্ট করে, শেষে explicit DELETE দিয়ে cleanup করা
হয়েছে):

- **old_solver (CANCELLED bid)** → `problems` SELECT-এ `cnt = 1` ✅ (আগে ছিল ০)
- **stranger solver (কোনো bid-ই নেই এই problem-এ)** → `cnt = 0` ✅ (leak হচ্ছে না)
- **current accepted solver** → `cnt = 1` ✅ (অপরিবর্তিত পথ, ভাঙেনি)

### ⚠️ গুরুত্বপূর্ণ — এটা realtime broadcast/postgres_changes না, শুধু plain SELECT RLS

- `problems` টেবিলের কোনো broadcast trigger নেই (নিয়ম #৫, touch করা হয়নি) এবং normal-user
  session-এ `SupabaseRealtimeManager.startRealtimeListeners()` (যেটা `problems` টেবিলের
  table-wide `postgresChangeFlow` চালু করে) **কখনো call হয় না** — এটা শুধু
  `AdminPanelScreen.kt`-এর "Force Sync"/Supabase-reconfigure dialog থেকেই reachable, যেগুলো
  admin-only স্ক্রিন (কোড ভেরিফাই করে দেখা হয়েছে)। তাই এই RLS ওপেন করাতে old-solver-এর ডিভাইসে
  কোনো live/postgres_changes push শুরু হবে না।
- তবে normal user-দের একটা **periodic, throttled full bulk-pull** আছে (app-open-এ,
  `FULL_SYNC_MIN_INTERVAL_MS` থ্রেশহোল্ড মেনে — `pullBulkDataFromSupabase()`)। এই fix-এর পর
  পরবর্তী পুল-এ old-solver-এর ডিভাইস প্রথমবার এই problem row-টা Room cache-এ পাবে, আর তারপর
  সেই periodic pull-এর ছন্দেই (live না) মাঝে-মধ্যে রিফ্রেশ হতে পারে — ঠিক আগের সেশনের
  chat-thread-isolation ফিক্সের নোটিশে যে "স্থবির (stale) হতে পারে" disclaimer দেওয়া হয়েছিল,
  একই চরিত্রের আচরণ। সম্পূর্ণ "frozen forever" গ্যারান্টি দেওয়া হচ্ছে না, কিন্তু live/push
  আপডেট হবে না — এটাই মূল অনুরোধ ছিল।
- Kotlin-সাইডে **কোনো পরিবর্তন লাগেনি** — `SolverMyBidsScreen.kt`-এর "বাতিল" ট্যাব ও
  `ProblemDetailScreen.kt`-এর আগের সেশনের নোটিশ-কার্ড — দুটোই ইতিমধ্যে লেখা প্যাটার্ন অনুযায়ী
  local Room cache থেকে পড়ে, শুধু এতদিন RLS-এর কারণে row-টাই cache-এ ঢুকতো না। এখন RLS খুলে
  দেওয়ায় বাকি UI logic এমনিতেই কাজ করবে বলে কোড-রিভিউ দিয়ে যাচাই করা হয়েছে (compile/run
  sandbox-এ সম্ভব না, নিয়ম #১১ অনুযায়ী)।

### 📦 এই সেশনে ফাইল পরিবর্তন

- **নতুন migration** `supabase/migrations/problems_select_allow_ended_bid_solver.sql` (DB-তে
  apply হয়েছে)।
- **নতুন migration** `supabase/migrations/fix_problems_select_bids_rls_recursion.sql`
  (recursion ফিক্স, DB-তে apply হয়েছে; `public.solver_has_ended_bid()` ফাংশন যোগ)।
- Kotlin কোডে কোনো পরিবর্তন লাগেনি।
- এই progress ফাইলে এই এন্ট্রি যোগ।

### 🔜 পরের ধাপ

মাস্টার-প্রম্পটের ধাপ ৫ (ঐচ্ছিক — users/escrows) বা ধাপ ৬ (cleanup+QA)-এ যাওয়া যেতে পারে —
ব্যবহারকারীর সিদ্ধান্ত অনুযায়ী।

---

## ✅ ধাপ ৫ (ঐচ্ছিক) — `users`/`escrows` — সম্পন্ন (ব্যবহারকারী নিশ্চিত করে এগিয়ে যেতে বলেছেন)

### DB-সাইড (Supabase MCP দিয়ে সরাসরি apply + verify)

- **RLS**: নতুন policy লাগেনি — `realtime.messages`-এ আগে থেকেই একটা generic
  `"users can receive own user-topic broadcasts"` policy আছে (`topic = 'user:' || auth.uid()`),
  যেটা notifications/wallet-গ্রুপও ব্যবহার করছে — `users`/`escrows` একই `user:<id>` topic-scheme
  ব্যবহার করায় এটাই যথেষ্ট।
- **`users`** (single-owner, `user:<id>`): `notify_users_broadcast()` trigger function +
  `broadcast_users_changes` trigger (AFTER INSERT/UPDATE/DELETE)। migration:
  `realtime_scoping_step5_users_escrows_broadcast.sql`।
- **`escrows`** (dual-owner, `user_id`+`solver_id`): `notify_escrows_broadcast()` trigger
  function (transactions-এর প্যাটার্নের মতো দুই topic-এই broadcast, null-check সহ) +
  `broadcast_escrows_changes` trigger। একই migration ফাইলে।

### ⚠️ এই ধাপে পাওয়া বাগ — event-name collision (ধাপ ২-এর wallet-গ্রুপে যা হয়েছিল, ঠিক সেটাই আবার)

`notify_users_broadcast()` প্রথম সংস্করণ bare `TG_OP` ("INSERT"/"UPDATE"/"DELETE") event নাম
ব্যবহার করছিল — একই `user:<id>` channel-এ আগে থেকে থাকা `notifications`-এর bare event-নামের
সাথে সরাসরি collide করতো (দুটো ভিন্ন টেবিলের payload একই event-এ আসলে ভুল টেবিলের
struct-এ decode হওয়ার/silent-miss হওয়ার ঝুঁকি, নিয়ম #৮)। SQL-টেস্টেই এটা ধরা পড়ে —
`fix_users_broadcast_event_naming_collision.sql` দিয়ে সাথে সাথে ঠিক করা হয়েছে
(`'users_' || TG_OP`-এ বদলানো)। `escrows`-এর trigger শুরু থেকেই সঠিকভাবে `'escrows_' || TG_OP`
ব্যবহার করছিল, ওটায় হাত দেওয়া লাগেনি।

### 🧪 SQL-টেস্ট (নিয়ম #৮) — সিন্থেটিক ডেটা দিয়ে, পরে সব cleanup করা হয়েছে

- **users**: synthetic user আপডেট করে verify করা হলো broadcast শুধু তারই `user:<id>` topic-এ
  যাচ্ছে (fix-এর পর event নাম `users_INSERT`/`users_UPDATE` — কোনো collision নেই)।
- **escrows dual-broadcast**: synthetic escrow INSERT ও UPDATE দুটোতেই owner ও solver দুইজনেরই
  আলাদা `user:<id>` topic-এ broadcast পৌঁছেছে ভেরিফাই করা হয়েছে (`escrows_INSERT`/
  `escrows_UPDATE`)।
- **RLS topic-permission** (`realtime.topic()` session-config দিয়ে সিমুলেট করে, আগের ধাপগুলোর
  থেকে বেশি সঠিক পদ্ধতি — `realtime.topic()` আসলে `current_setting('realtime.topic')` পড়ে,
  যেটা `set local "realtime.topic" = '...'` দিয়ে সরাসরি সিমুলেট করা যায়): নিজের topic থেকে
  ৪টা broadcast row দেখা গেছে (`visible_rows = 4`), অন্য user-এর topic থেকে `visible_rows = 0` —
  cross-user leak হচ্ছে না।
- সব synthetic auth.users/public.users/problems/escrows/realtime.messages row টেস্ট শেষে মুছে
  আগের অবস্থায় (1 user, 0 problems, 0 escrows) ফেরানো হয়েছে।

### ✅ Kotlin-সাইড (`SupabaseRealtimeManager.kt`, dual-run) — এই সেশনেই লেখা ও ওয়্যার করা হলো

- `startUserTopicBroadcastSubscription()`-এ (আগের notifications/wallet-গ্রুপের একই
  `user:$userId` channel পুনর্ব্যবহার করে) নতুন দুইটা `broadcastFlow` লিসেনার-পেয়ার যোগ:
  `UserChangeBroadcastPayload` (event `users_$op`) ও `EscrowChangeBroadcastPayload`
  (event `escrows_$op`)।
- `applyUserBroadcastChange()` — বিদ্যমান `mergeAndSaveUser()` merge-logic (password-preserve +
  balance-flicker-guard) পুনর্ব্যবহার করে, postgresChangeFlow পথের `handleUserAction()`-এর
  সমান আচরণ।
- `applyEscrowBroadcastChange()` — বিদ্যমান `mergeAndSaveEscrow()` (stale-snapshot-skip +
  `resolveIncomingEscrowStatus()`) পুনর্ব্যবহার করে, `handleEscrowAction()`-এর সমান আচরণ।
- **dual-run**: পুরনো টেবিল-ওয়াইড `usersChannel`/`escrowsChannel` (postgresChangeFlow,
  `startRealtimeListeners()`-এর ভেতরে) অক্ষত রাখা হয়েছে, প্রতিস্থাপন করা হয়নি।
- Admin session অপরিবর্তিত (`isCurrentSessionAdmin()` চেক আগে থেকেই এই ব্লকে আছে, নতুন কিছু
  বদলাতে হয়নি)।

### 📦 এই সেশনে ফাইল পরিবর্তন

- **নতুন migration** `supabase/migrations/realtime_scoping_step5_users_escrows_broadcast.sql`
  (DB-তে apply হয়েছে)।
- **নতুন migration** `supabase/migrations/fix_users_broadcast_event_naming_collision.sql`
  (event-naming বাগ ফিক্স, DB-তে apply হয়েছে)।
- **`app/src/main/java/com/example/data/remote/SupabaseRealtimeManager.kt`**: নতুন payload
  data class দুটো, `applyUserBroadcastChange()`/`applyEscrowBroadcastChange()` ফাংশন দুটো,
  `startUserTopicBroadcastSubscription()`-এ নতুন subscription ব্লক, আর ফাইলের উপরের KDoc
  ইতিহাস-কমেন্টে ধাপ ৫-এর এন্ট্রি।

### ⚠️ এখনো যা ভেরিফাই করা হয়নি

- Kotlin কোড build/compile করা যায়নি (sandbox-এ network/Gradle নেই, নিয়ম #১১) — শুধু ম্যানুয়াল
  bracket/paren-balance ও import-নির্ভরতা চেক করা হয়েছে।

### 🔜 পরের ধাপ

মাস্টার-প্রম্পটের ধাপ ৬ (cleanup — dual-run সমাপ্তি + চূড়ান্ত QA)।

---

## 🟡 ধাপ ৬ — Cleanup + চূড়ান্ত QA — **আংশিক (DB/কোড অডিট সম্পন্ন, কোনো পুরনো subscription সরানো হয়নি — নিয়ম #১-এর কারণে ইচ্ছাকৃতভাবে)**

### ✅ DB-state পুনরায় যাচাই — কোনো drift নেই

`Supabase:list_migrations` (project `mghvvpndkxnscwryfkib`) আবার চালিয়ে দেখা গেল সর্বশেষ migration
`fix_users_broadcast_event_naming_collision` (`20260913145059`) — এই zip/progress-ফাইলে যা লেখা
আছে তার সাথে হুবহু মিলেছে। ধাপ ১-৫-এর মাঝে কোনো নতুন/মিসিং migration নেই।

### ✅ ফাংশনাল চেকলিস্ট (কোড-রিভিউ ভিত্তিক, নিয়ম #১১ অনুযায়ী build/run সম্ভব না)

প্রতিটা migrate-হওয়া ৯টা টেবিলের broadcast path সম্পূর্ণ, কোথাও অর্ধেক-লেখা কল নেই — নিচেরগুলো
সরাসরি গ্রেপ করে ভেরিফাই করা হলো:
- ৯টা `apply*BroadcastChange()` ফাংশনই বিদ্যমান (`applyNotificationBroadcastChange`,
  `applyTransactionBroadcastChange`, `applyWithdrawalBroadcastChange`,
  `applyGatewayPaymentBroadcastChange`, `applyAdditionalChargeBroadcastChange`,
  `applyUserBroadcastChange`, `applyEscrowBroadcastChange`, `applyMessageBroadcastChange`,
  `applyBidBroadcastChange`)।
- `notifications`/wallet-৪টা/`users`/`escrows` — সবই `startUserTopicBroadcastSubscription()`-এর
  ভেতরে একই `user:$userId` channel-এ wired, `startRealtimeListeners()`-এ
  `isCurrentSessionAdmin()` চেক দিয়ে admin-এ বন্ধ থাকে (অপরিবর্তিত)।
- `messages`/`bids` — dynamic lifecycle-bound join/leave চেইন সম্পূর্ণ ট্রেস করা হলো:
  `SupabaseRealtimeManager.kt` → `SomadhanRepository.kt`
  (`joinProblemChatBroadcast`/`leaveProblemChatBroadcast`,
  `joinProblemBidsBroadcast`/`leaveProblemBidsBroadcast`) → `SomadhanViewModel.kt` (non-suspend
  wrapper) → `ChatScreen.kt` (`LaunchedEffect`/`DisposableEffect`) ও `ProblemDetailScreen.kt`
  (`LaunchedEffect`/`DisposableEffect`) — কোথাও ভাঙা লিংক নেই।
- `stopRealtimeListeners()`-এ leak-guard সম্পূর্ণ: পুরনো ৮টা টেবিল-চ্যানেল + নতুন
  `userTopicBroadcastChannel` + typing channels + `messagesBroadcastChannels`/
  `bidsBroadcastChannels` (dynamic map) — সবই বন্ধ হয়, কিছু বাদ পড়েনি।
- `problems` টেবিলের পুরনো table-wide channel অক্ষত আছে (নিয়ম #৫, কখনো touch করা হয়নি) — যাচাই
  করা হলো এটা normal-user session-এ কখনোই start হয় না (শুধু admin-only `AdminPanelScreen.kt`-এর
  "Force Sync" থেকে reachable, আগের একটা সেশনেই এটা ভেরিফাই হয়ে গিয়েছিল)।

### ❌ পুরনো `postgresChangeFlow` subscription **এই সেশনে সরানো হয়নি** — নিয়ম #১-এর সিদ্ধান্ত

মাস্টার-প্রম্পটের ধাপ ৬-এর নিয়ম #১ অনুযায়ী: কোনো টেবিলের নতুন broadcast mechanism নিয়ে সন্দেহ
থাকলে ("verify করা হয়নি" নোট থাকলে) সেই টেবিলের পুরনো subscription **এখনই সরানো যাবে না**।
প্রতিটা ৯টা migrate-হওয়া টেবিলের টেস্ট-রেকর্ড রিভিউ করে দেখা গেল **একটা সর্বজনীন গ্যাপ + কিছু
টেবিল-নির্দিষ্ট গ্যাপ** এখনো বিদ্যমান:

**সর্বজনীন (৯টা টেবিলেই প্রযোজ্য):**
- **Kotlin কোড কখনো build/compile করা হয়নি** — ধাপ ১ থেকে ধাপ ৫ পর্যন্ত প্রতিটা সেশনেই এই sandbox-এ
  network/Gradle না থাকায় শুধু ম্যানুয়াল bracket/paren-balance চেক হয়েছে (নিয়ম #১১-এই স্পষ্ট লেখা
  আছে এটা যথেষ্ট প্রমাণ না)। এই sandbox-এও (`bash_tool`) network বন্ধ, তাই এই সেশনেও build সম্ভব
  হয়নি। যদি কোনো compile error থাকে (যেমন ধাপ ১-এর প্রোগ্রেস-এন্ট্রিতে যে সন্দেহ প্রকাশ করা হয়েছিল
  — `broadcast_changes()`-এর payload-এ `record`/`old_record` key-নাম আসলে ঠিক এভাবেই আসে কিনা,
  positional-argument অনুমান করে লেখা), সেটা পুরনো subscription সরানোর পর ধরা পড়লে **কোনো fallback
  থাকবে না**।
- **কোনো real device/emulator দিয়ে end-to-end টেস্ট হয়নি** — দুই আলাদা account একসাথে লগইন করে
  cross-account-leak-না-হওয়া, বা লাইভ UI-তে (notification bell, wallet screen, chat bubble,
  bid-status) নতুন broadcast path আসলে dispatch হচ্ছে কিনা — সবটাই SQL-simulation/কোড-রিভিউ
  পর্যন্ত সীমাবদ্ধ।
- **"Allow public access" সেটিং** — ব্যবহারকারী বন্ধ করেছেন বলে কনফার্ম করা আছে (এন্ট্রি "✅ Allow
  public access বন্ধ করা হয়েছে" দেখুন), কিন্তু ধাপ ৩-এর পর থেকে আর কোনো সেশনে পুনরায়
  Dashboard থেকে verify করা হয়নি যে এটা কোনোভাবে আবার চালু হয়ে যায়নি (নিয়ম #৭-এর সতর্কতা)।

**টেবিল-নির্দিষ্ট (তুলনামূলক ঝুঁকির ক্রম অনুযায়ী, কম থেকে বেশি ঝুঁকিপূর্ণ):**
1. **`notifications`** (ধাপ ১) — সবচেয়ে শক্ত প্রমাণ: real INSERT/UPDATE/DELETE টেবিলে সরাসরি চালিয়ে
   trigger fire ভেরিফাই হয়েছে + RLS positive/negative (ভুল user-এর topic reject, নিজের topic
   allow) real role-simulation দিয়ে টেস্ট হয়েছে।
2. **`users`/`escrows`** (ধাপ ৫) — সিন্থেটিক হলেও real UPDATE/INSERT দিয়ে trigger fire + dual-broadcast
   (escrows) + RLS topic-simulation (`set local "realtime.topic"`) দিয়ে ভেরিফাই হয়েছে, তারপর
   cleanup করে prod অবস্থায় ফেরানো হয়েছে।
3. **`messages`** (ধাপ ৩), **`bids`** (ধাপ ৪), **`transactions`/`withdrawals`/`gateway_payments`/
   `additional_charges`** (ধাপ ২) — এই ৬টাতে শুধু `realtime.broadcast_changes()` সরাসরি কল করে
   mechanism-level (topic-routing সঠিক কিনা) টেস্ট হয়েছে — **আসল টেবিলে real row INSERT/UPDATE
   করে trigger নিজে থেকে fire হওয়া কখনো টেস্ট করা যায়নি** (dev DB-তে এই টেবিলগুলো খালি ছিল)। এছাড়া
   `messages`/`bids`-এর RLS policy real-data positive/negative simulation-ও এখনো বাকি (structural
   কমপারিজন দিয়ে যুক্তিসঙ্গত মনে হলেও, real-data দিয়ে কনফার্ম হয়নি)।

**এই কারণে এই সেশনে কোনো টেবিলের পুরনো table-wide `postgresChangeFlow` subscription
(`SupabaseRealtimeManager.kt`-এর `startRealtimeListeners()`/`stopRealtimeListeners()`-এ)
সরানো হয়নি — dual-run পুরোপুরি অক্ষত আছে।** এটা মানে এখনো Realtime billing fan-out সাশ্রয় শুরু
হয়নি (মূল লক্ষ্য এখনো অর্জিত হয়নি), কিন্তু নিয়ম #১-এর নিরাপত্তা-অগ্রাধিকার অনুযায়ী এটাই সঠিক সিদ্ধান্ত
যতক্ষণ না উপরের গ্যাপগুলো (বিশেষত build verification) পূরণ হয়।

### 📦 এই সেশনে ফাইল পরিবর্তন
- কোনো Kotlin/SQL ফাইল বদলানো হয়নি (শুধু অডিট, কোনো কোড পরিবর্তন না — নিয়ম #৯-এর চেতনা অনুযায়ী,
  যেহেতু কোনো নতুন বাগ পাওয়া যায়নি, শুধু pre-existing verification-gap ফ্ল্যাগ করা হলো)।
- এই progress ফাইলে "ধাপ ৬ — আংশিক" এন্ট্রি যোগ।

### 🔜 পরের ধাপ — ব্যবহারকারীর সিদ্ধান্ত দরকার
1. **সবচেয়ে গুরুত্বপূর্ণ:** Android Studio-তে একটা আসল Gradle build/sync চালিয়ে compile-error আছে
   কিনা কনফার্ম করা (বিশেষত `broadcast_changes()` payload-এর key-নাম নিয়ে সন্দেহ)।
2. Real device/emulator দিয়ে অন্তত `notifications` (সবচেয়ে বেশি-verified) দিয়ে একটা end-to-end
   স্মোক-টেস্ট করা (দুই account, cross-leak-না-হওয়া)।
3. এরপর ব্যবহারকারী ঠিক করবেন — **table-by-table ক্রমান্বয়ে cleanup** (আগে শুধু `notifications`/
   `users`/`escrows`-এর পুরনো subscription সরানো, wallet/chat/bids-এর জন্য আরও টেস্টের অপেক্ষা করা)
   নাকি **সব একসাথে** সরানো ঝুঁকি নিয়ে এগোনো — এটা ব্যবহারকারীর explicit সিদ্ধান্ত লাগবে (নিয়ম #৯)।
4. "Allow public access" আবার Dashboard-এ গিয়ে একবার re-verify করা ভালো (নিয়ম #৭)।
