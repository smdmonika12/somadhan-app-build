package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.sync.OutboxRpcDispatcher
import java.util.concurrent.TimeUnit

/**
 * Outbox/retry ইনফ্রা — ধাপ ৬ (RPC_SYNC_FIX ট্র্যাক)। ডিজাইন রেফারেন্স:
 * `docs/OUTBOX_RETRY_DESIGN.md` সেকশন ৪।
 *
 * **⚠️ Step 6-এ এই worker কোথাও schedule করা হয় না** — `schedulePeriodic()`/`triggerImmediate()`
 * নিচে রেডি আছে (বিদ্যমান `InstantJobExpiryWorker`-এর কনভেনশন অনুসরণ করে), কিন্তু
 * `SomadhanApp.kt`-এ (বা অন্য কোথাও) এখনো কল করা হয়নি। তাই WorkManager কখনো এই worker রান করবে
 * না, `pending_sync_outbox` টেবিলও কখনো খালি ছাড়া কিছু হবে না (কোনো dual-write ফাংশন এখনো
 * insert করছে না, Step 7-এর কাজ) — অ্যাপের বিদ্যমান আচরণে শূন্য প্রভাব।
 */
class OutboxSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val outboxDao = db.pendingSyncOutboxDao()
            val pending = outboxDao.getPending()

            for (entry in pending) {
                val rpcResult = OutboxRpcDispatcher.callRpcByName(entry.rpcName, entry.paramsJson)
                if (rpcResult.isSuccess) {
                    outboxDao.deleteById(entry.id)
                    Log.d("OutboxSyncWorker", "synced & cleared outbox entry ${entry.id} (rpc=${entry.rpcName})")
                } else {
                    val newCount = entry.retryCount + 1
                    val newStatus = if (newCount >= MAX_RETRY_COUNT) "FAILED_PERMANENT" else "RETRYING"
                    outboxDao.update(
                        entry.copy(
                            retryCount = newCount,
                            lastError = rpcResult.exceptionOrNull()?.message?.take(500),
                            lastAttemptAt = System.currentTimeMillis(),
                            status = newStatus
                        )
                    )
                    Log.w(
                        "OutboxSyncWorker",
                        "retry failed for outbox entry ${entry.id} (rpc=${entry.rpcName}, retryCount=$newCount, status=$newStatus): ${rpcResult.exceptionOrNull()?.message}"
                    )
                }
            }
            // worker নিজে কখনো "fail" করবে না -- প্রতিটা entry আলাদাভাবে handle হয়, একটার fail
            // অন্যগুলোর retry আটকাবে না (ডিজাইন ডক সেকশন ৪-এর পুরনো pseudocode-এর সাথে সমতুল্য)।
            Result.success()
        } catch (e: Exception) {
            // পুরো doWork()-এর কোনো অপ্রত্যাশিত ব্যর্থতা (যেমন DB access সমস্যা) -- WorkManager-কে
            // নিজের backoff অনুযায়ী পুরো রান আবার শিডিউল করতে বলা, individual entry retryCount
            // অক্ষত থাকবে (কোনো entry touch হয়নি)।
            Log.e("OutboxSyncWorker", "doWork() failed unexpectedly: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        // প্রস্তাবিত থ্রেশহোল্ড (ডিজাইন ডক সেকশন ২/৮) -- চূড়ান্ত না, প্রয়োজনে পরে বদলানো যাবে।
        const val MAX_RETRY_COUNT = 10

        const val UNIQUE_WORK_NAME = "outbox_sync_periodic_work"
        const val WORK_TAG = "outbox_sync"

        /**
         * PeriodicWorkRequest, WorkManager-এর হার্ড ফ্লোর ১৫ মিনিট (ডিজাইন ডক সেকশন ৪)।
         * NetworkType.CONNECTED constraint + BackoffPolicy.EXPONENTIAL (base delay ৩০ সেকেন্ড) --
         * এই backoff শুধু পরের পুরো `doWork()` রান কখন হবে সেটা নিয়ন্ত্রণ করে (`Result.retry()`
         * হলে), individual outbox entry-র নিজস্ব retryCount/status এর থেকে independent।
         *
         * **⚠️ Step 6-এ এই ফাংশন কোথাও কল করা হচ্ছে না** — Step 7-এ `SomadhanApp.kt`-এ
         * (existing `InstantJobExpiryWorker.schedulePeriodic(this)` কলের পাশে, additive) যোগ
         * হওয়ার কথা, master plan অনুযায়ী।
         */
        fun schedulePeriodic(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<OutboxSyncWorker>(
                    15, TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30, TimeUnit.SECONDS
                    )
                    .addTag(WORK_TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } catch (e: Exception) {
                Log.w("OutboxSyncWorker", "Failed to schedule periodic work: ${e.message}")
            }
        }

        /**
         * app-open / pull-to-refresh মুহূর্তে network ফিরে আসার সাথে সাথে দ্রুত sync-এর জন্য
         * one-off ট্রিগার (ডিজাইন ডক সেকশন ৪)। [Step 7.7 cleanup] `syncPendingCloudRefunds()`
         * নামের কোনো ফাংশন এখন কোডবেসে নেই (Firebase-era leftover রেফারেন্স ছিল) -- আসল retry
         * মেকানিজম হলো `enqueueOutboxRetry()`/outbox queue, এটাই সেটার periodic + on-demand
         * ট্রিগার।
         * **এটাও Step 6-এ কোথাও কল করা হচ্ছে না**, শুধু ফাংশনটা রেডি রাখা হলো।
         */
        fun triggerImmediate(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                    .setConstraints(constraints)
                    .addTag(WORK_TAG)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (e: Exception) {
                Log.w("OutboxSyncWorker", "Failed to trigger immediate work: ${e.message}")
            }
        }
    }
}
