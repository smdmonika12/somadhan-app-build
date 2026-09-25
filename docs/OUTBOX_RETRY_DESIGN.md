# Outbox / Retry ইনফ্রা — ডিজাইন স্পেক

> **স্ট্যাটাস:** ডিজাইন-অনলি। এই ডকুমেন্ট লেখার সময় কোনো `.kt`/`.sql` ফাইল স্পর্শ করা হয়নি।
> কোনো actual কোড implement হয়নি — Step 6/7-এর ভিত্তি হিসেবে এই স্পেক ব্যবহার হবে।
> রেফারেন্স: `RPC_SYNC_FIX_PROGRESS.md` Step 5।

## ১. পটভূমি — বর্তমান dual-write প্যাটার্ন

`SomadhanRepository.kt`-এর প্রায় প্রতিটা মানি/স্টেট-পরিবর্তনকারী ফাংশন একই প্যাটার্ন অনুসরণ
করে (উদাহরণ: `payoutEscrowToSolver`, লাইন ~332):

1. Room-এ instant local write (এই মুহূর্তেই UI আপডেট হয়ে যায়, ইউজার অপেক্ষা করে না)।
2. যদি সেশন থাকে (`SupabaseAuthManager.currentUserId() != null`), সংশ্লিষ্ট
   `SupabaseSyncManager.xxx()` RPC কল "best-effort" — `.onSuccess { }` / `.onFailure { e -> Log.w(...) }`।
3. Fail হলে **শুধু `Log.w(...)`** — কোনো retry, কোনো persistent record, কোনো ইউজার-ভিজিবল
   সংকেত না। Cloud state local state থেকে silently ড্রিফট করে, এবং সেটা ধরার একমাত্র উপায়
   এখন ম্যানুয়াল অডিট (যেমন `admin_reconcile_user_balances`, যেটা [ধাপ ৪]-এ flat balance
   কভার করার জন্য এক্সটেন্ড করা হয়েছে — কিন্তু সেটা reactive, root cause ফিক্স না)।

Outbox প্যাটার্নের লক্ষ্য: **step ২-৩ এর fail-case-টাকে "silent log" থেকে "persistent,
retry-able record"-এ রূপান্তর করা**, ধাপ ১ (Room instant write, happy-path UX) একবিন্দুও না
বদলে।

## ২. Room entity — `PendingSyncOutboxEntity`

```kotlin
@Entity(tableName = "pending_sync_outbox")
data class PendingSyncOutboxEntity(
    @PrimaryKey val id: String,          // ULID/UUID, ক্লায়েন্টে জেনারেট
    val rpcName: String,                 // e.g. "release_escrow", "admin_adjust_balance"
    val paramsJson: String,              // RPC-এর প্যারামিটারগুলো JSON-সিরিয়ালাইজড (kotlinx.serialization)
    val createdAt: Long,                 // System.currentTimeMillis(), প্রথম fail-এর সময়
    val retryCount: Int = 0,
    val lastError: String? = null,       // সর্বশেষ retry-র error message (truncated, ৫০০ char cap)
    val lastAttemptAt: Long? = null,
    val status: String = "PENDING"       // PENDING | RETRYING | FAILED_PERMANENT | DONE
)
```

- `status` একটা enum না, `String` — বিদ্যমান codebase-এ অন্যান্য entity-ও status কলামে plain
  `String` ব্যবহার করে (যেমন `EscrowEntity.status`, `WithdrawalEntity.status`), তাই এই
  কনভেনশনই অনুসরণ করা হলো।
- `DONE` স্ট্যাটাস রাখা হয়েছে soft-delete-এর মতো (row মুছে ফেলার বদলে) যাতে সাম্প্রতিক sync
  history admin panel-এ দেখানো যায় ভবিষ্যতে চাইলে; পুরনো `DONE` row periodically prune করার
  একটা সহজ cleanup (`retryCount` বা `createdAt` ভিত্তিক) পরে যোগ করা যাবে, এই ধাপের স্কোপে না।
- `retryCount` একটা threshold (প্রস্তাবিত: ১০) পার হলে `status = FAILED_PERMANENT`-এ চলে যাবে
  আর worker আর retry করবে না — admin panel-এ ম্যানুয়াল রিভিউ/রিট্রাই বাটন দরকার হবে (নিচে UI
  সেকশন দেখুন)।

### সংশ্লিষ্ট DAO

```kotlin
@Dao
interface PendingSyncOutboxDao {
    @Insert suspend fun insert(entry: PendingSyncOutboxEntity)
    @Update suspend fun update(entry: PendingSyncOutboxEntity)
    @Query("SELECT * FROM pending_sync_outbox WHERE status IN ('PENDING','RETRYING') ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingSyncOutboxEntity>
    @Query("SELECT COUNT(*) FROM pending_sync_outbox WHERE status IN ('PENDING','RETRYING')")
    fun observePendingCount(): Flow<Int>   // UI ইন্ডিকেটরের জন্য (Step 8)
    @Query("DELETE FROM pending_sync_outbox WHERE id = :id") suspend fun deleteById(id: String)
}
```

## ৩. Dual-write ফাংশনের সাথে ইন্টিগ্রেশন — কী বদলাবে, কী বদলাবে না

**বদলাবে না:** ধাপ ১ (Room instant write) এবং success path (`.onSuccess { }` ব্লক) —
caller/ViewModel/UI-এর কাছে কোনো আচরণ বদলায় না, রিটার্ন টাইপ/সিগনেচার অপরিবর্তিত।

**বদলাবে (শুধু fail-case):**

```kotlin
// বর্তমান (উদাহরণ payoutEscrowToSolver থেকে):
.onFailure { e ->
    Log.w("SomadhanRepo", "payoutEscrowToSolver: Supabase dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
}

// প্রস্তাবিত (Step 7-এ wire হবে):
.onFailure { e ->
    Log.w("SomadhanRepo", "payoutEscrowToSolver: Supabase dual-write failed for escrow ${escrow.id} (local flow unaffected): ${e.message}")
    outboxDao.insert(PendingSyncOutboxEntity(
        id = generateId(),
        rpcName = "release_escrow",
        paramsJson = Json.encodeToString(mapOf("escrowId" to escrow.id)),
        createdAt = System.currentTimeMillis(),
        lastError = e.message?.take(500)
    ))
}
```

`Log.w` কল **সরানো হবে না** — শুধু outbox insert যোগ হবে (additive), যাতে logcat-ভিত্তিক
ডিবাগিং আগের মতোই কাজ করে।

## ৪. WorkManager periodic sync

```kotlin
class OutboxSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val pending = outboxDao.getPending()
        for (entry in pending) {
            val rpcResult = SupabaseSyncManager.callRpcByName(entry.rpcName, entry.paramsJson) // dispatch table
            if (rpcResult.isSuccess) {
                outboxDao.deleteById(entry.id)
            } else {
                val newCount = entry.retryCount + 1
                outboxDao.update(entry.copy(
                    retryCount = newCount,
                    lastError = rpcResult.exceptionOrNull()?.message?.take(500),
                    lastAttemptAt = System.currentTimeMillis(),
                    status = if (newCount >= 10) "FAILED_PERMANENT" else "RETRYING"
                ))
            }
        }
        return Result.success() // worker নিজে কখনো "fail" করবে না, প্রতিটা entry আলাদাভাবে handle হয়
    }
}
```

- **Constraint:** `NetworkType.CONNECTED` (network না থাকলে worker চলবেই না, ব্যাটারি বাঁচাতে)।
- **Backoff policy:** WorkManager-এর built-in `BackoffPolicy.EXPONENTIAL`, base delay ৩০ সেকেন্ড
  (WorkManager-এর নিজস্ব retry না — worker প্রতিবার সব pending entry ট্রাই করবে, individual
  entry-র `retryCount` নিজস্বভাবে ট্র্যাক হয়; WorkManager-এর backoff শুধু পরের পুরো `doWork()`
  রান কখন হবে সেটা নিয়ন্ত্রণ করে)।
- **Periodic trigger:** `PeriodicWorkRequest`, ন্যূনতম ইন্টারভাল ১৫ মিনিট (WorkManager-এর হার্ড
  ফ্লোর — এর কমে যায় না)। এছাড়া app-open এবং pull-to-refresh মুহূর্তেও একটা
  one-off `OneTimeWorkRequest` enqueue করার প্রস্তাব, যাতে network ফিরে আসার সাথে সাথে
  দ্রুত sync হয় (বিদ্যমান `syncPendingCloudRefunds()`-এর মতো প্যাটার্নে, যেটা এখনই app-open-এ
  ট্রিগার হয়)।
- **`callRpcByName` dispatch টেবিল:** এটা নতুন — `rpcName` স্ট্রিং থেকে সঠিক
  `SupabaseSyncManager.xxx()` ফাংশন কল করার একটা `when` ব্লক/ম্যাপ। Step 6-এ implement হবে,
  শুরুতে শুধু Step 7-এ wire হওয়া কয়েকটা RPC-এর জন্য এন্ট্রি থাকবে (বাকিগুলো ধীরে ধীরে যোগ হবে)।

## ৫. অগ্রাধিকার লিস্ট (Step ৬/৭-এর জন্য প্রস্তাবিত ক্রম)

টাকা-সংক্রান্ত ফাংশন আগে (silent dual-write fail এখানে সবচেয়ে ক্ষতিকর — ইউজারের ব্যালেন্স
cloud-এ sync না হলেও app কিছু বলবে না):

1. `payoutEscrowToSolver` (→ RPC `release_escrow`)
2. `refundEscrowOnceLocked` (→ RPC `refund_escrow_once`)
3. `addToEscrow` (→ সংশ্লিষ্ট escrow RPC)
4. `depositMoneyViaGateway` (→ RPC `deposit_money_via_gateway`)
5. `requestWithdrawal` (→ RPC `request_withdrawal`)
6. `updateWithdrawalStatus` (→ RPC `process_withdrawal`)
7. `adminAdjustBalance` (→ RPC `admin_adjust_balance`)

Step 7-এর মাস্টার প্ল্যান প্রম্পটে ইতিমধ্যে এই লিস্টের প্রথম ৩টা (ব্যাচ ১) নির্দিষ্ট করা আছে;
বাকিগুলো ব্যাচ ২ (৭খ)-এ। এই ধাপে (Step ৫) শুধু prioritization প্রস্তাব করা হলো, কোনো ফাংশন
এখনো ছোঁয়া হয়নি।

## ৬. UI-তে pending/failed sync দেখানো (প্রস্তাব — কোনো কোড না, Step 8-এর কাজ)

- Wallet/Withdrawal স্ক্রিনে একটা ছোট, non-blocking ব্যাজ/টেক্সট: "⚠️ কিছু লেনদেন cloud-এ sync
  হতে বাকি আছে" — `outboxDao.observePendingCount()` থেকে আসা count > 0 হলে দেখাবে, খালি হলে
  পুরোপুরি অদৃশ্য।
- পাশে একটা "এখনই আবার চেষ্টা করুন" বাটন — ম্যানুয়ালি `OneTimeWorkRequest` enqueue করবে।
- `FAILED_PERMANENT` স্ট্যাটাসের entry থাকলে (retryCount ≥ 10) আলাদা, একটু বেশি জোরালো
  ইঙ্গিত প্রস্তাব করা যেতে পারে (যেমন admin-কে যোগাযোগ করার পরামর্শ) — চূড়ান্ত UX Step 8-এ
  ঠিক হবে।
- কোনো blocking modal/dialog না — ইউজার এই ইন্ডিকেটর উপেক্ষা করে বাকি অ্যাপ স্বাভাবিকভাবে
  ব্যবহার করতে পারবে।

## ৭. Rollback / migration প্ল্যান

- বর্তমান Room schema version: **৫২** (`AppDatabase.kt`)। নতুন `pending_sync_outbox` টেবিল
  যোগ করলে version **৫৩**-এ বাড়াতে হবে, এবং একটা non-destructive `Migration(52, 53)` অবজেক্ট
  লিখতে হবে (`CREATE TABLE pending_sync_outbox (...)`) — `fallbackToDestructiveMigration()`
  ব্যবহার করা যাবে না (এতে ইউজারের বিদ্যমান লোকাল ডেটা, যেমন balance/job history, হারিয়ে
  যেতে পারে, যা কখনো গ্রহণযোগ্য না — এই কনভেনশন কমন রুলসে আগে থেকেই আছে)।
- Rollback path: যেহেতু outbox টেবিল সম্পূর্ণ additive এবং কোনো বিদ্যমান টেবিল/কলাম বদলায় না,
  rollback মানে শুধু নতুন টেবিলটা ড্রপ করা (আগের কোনো ডেটা প্রভাবিত হয় না) — কিন্তু এটা এই
  ধাপের স্কোপে implement করা হচ্ছে না, শুধু নোট করা হলো ভবিষ্যতের রেফারেন্সের জন্য।
- Step 6 (পরের ধাপ)-এ entity/DAO/Migration অবজেক্ট তৈরি হবে কিন্তু **কোনো বিদ্যমান dual-write
  ফাংশন তখনো wire হবে না** — সেটা Step 7-এর কাজ, master plan অনুযায়ী।

## ৮. Step 6-এ যাওয়ার আগে যা কনফার্ম করা দরকার

এই ডিজাইন ডকটা রিভিউ করে, নিচের পয়েন্টগুলো ইউজার approve করলে Step 6 (implementation) শুরু
করা যাবে:

- Entity/DAO schema (সেকশন ২) ঠিক আছে কিনা, বিশেষত `paramsJson` স্ট্রিং-হিসেবে রাখার সিদ্ধান্ত
  (বিকল্প: প্রতিটা RPC-র জন্য আলাদা টাইপড entity — কিন্তু সেটা অনেক বেশি boilerplate তৈরি করত)
- Retry threshold ১০ আর backoff base delay ৩০ সেকেন্ড — এই সংখ্যাগুলো প্রস্তাবিত, চূড়ান্ত না
- Priority order (সেকশন ৫) মাস্টার প্ল্যানের Step 7 ব্যাচ ১-এর সাথে মেলে কিনা
