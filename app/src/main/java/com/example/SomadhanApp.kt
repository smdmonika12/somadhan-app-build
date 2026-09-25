package com.example

import android.app.Application
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.remote.SupabaseClientProvider
import com.example.data.remote.SupabaseRealtimeManager
import com.example.util.OtpService

class SomadhanApp : Application() {
    companion object {
        lateinit var instance: SomadhanApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        installCrashLogger()
        OtpService.init(this)

        // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৪] অ্যাডমিন প্যানেল থেকে সেভ করা Supabase URL/anon
        // key override (যদি থাকে) লোড করা হচ্ছে, `SupabaseClientProvider.client` প্রথমবার
        // অ্যাক্সেস হওয়ার আগেই -- নাহলে override সেট থাকা সত্ত্বেও প্রথম client BuildConfig-এর
        // ডিফল্ট মান দিয়ে তৈরি হয়ে যেত।
        SupabaseClientProvider.initFromSavedConfig(this)

        // [SUPABASE-MIGRATED - ধাপ ৩৩.২] আগে এখানে initializeFirebase() কল হতো, তারপর
        // FirebaseSyncManager.attachDatabase(db) দিয়ে local database attach করে real-time
        // Firestore sync শুরু হতো -- দুটোই সরানো হলো। নিচের SupabaseRealtimeManager.attachDatabase()
        // (ধাপ ২২ থেকে সক্রিয়) একাই এখন attach + bulk-pull + realtime listener শুরু করার পুরো
        // দায়িত্ব পালন করে।
        try {
            val db = AppDatabase.getDatabase(this)
            SupabaseRealtimeManager.attachDatabase(db)
        } catch (e: Exception) {
            Log.w("SomadhanApp", "Error attaching database to SupabaseRealtimeManager: ${e.message}")
        }

        // Schedule background worker for instant job auto-expiry & soft-deletion
        try {
            com.example.worker.InstantJobExpiryWorker.schedulePeriodic(this)
            com.example.worker.InstantJobExpiryWorker.triggerImmediate(this)
        } catch (e: Exception) {
            Log.w("SomadhanApp", "Error scheduling InstantJobExpiryWorker: ${e.message}")
        }

        // [OUTBOX WIRE - ধাপ ৭ (RPC_SYNC_FIX ট্র্যাক)] Step 6-এ OutboxSyncWorker তৈরি হয়েছিল
        // কিন্তু কোথাও schedule করা হয়নি (worker কখনো চলতোই না)। এই ধাপে
        // payoutEscrowToSolver/refundEscrowOnceLocked/addToEscrow-এর fail-ব্লকে outbox insert
        // শুরু হলো, তাই এখন থেকে periodic worker চালু করা দরকার -- নাহলে insert হওয়া entry
        // কখনো retry হবে না। InstantJobExpiryWorker-এর ঠিক পাশে, একই try/catch কনভেনশনে,
        // সম্পূর্ণ additive (কোনো বিদ্যমান লাইন বদলায়নি)। triggerImmediate() ইচ্ছাকৃতভাবে এখানে
        // কল করা হয়নি -- app-start মুহূর্তে outbox স্বাভাবিকভাবেই খালি থাকে (নতুন fail না হওয়া
        // পর্যন্ত), তাই শুধু periodic (১৫ মিনিট পরপর) যথেষ্ট; Step 8-এ UI থেকে ম্যানুয়াল
        // "retry now" বাটন triggerImmediate() ব্যবহার করবে।
        try {
            com.example.worker.OutboxSyncWorker.schedulePeriodic(this)
        } catch (e: Exception) {
            Log.w("SomadhanApp", "Error scheduling OutboxSyncWorker: ${e.message}")
        }

        // [SUPABASE-MIGRATED - ধাপ ৩৩.৩ কাজ ৫-৭] আগে এখানে WalletSyncWorker.schedulePeriodic(this)
        // কল হতো -- এই worker সম্পূর্ণ Firestore-নির্দিষ্ট ছিল (pendingCloudSync flag দিয়ে
        // Firestore-এ retry করা), Supabase-সাইডে এর কোনো সমতুল্য দরকার নেই কারণ প্রতিটা
        // money-related RPC (release_escrow/refund_escrow_once/request_withdrawal/ইত্যাদি)
        // কল-টাইমেই atomically ব্যালেন্স আপডেট করে, আলাদা কোনো "pending" retry-queue নেই।
        // WalletSyncWorker.kt ফাইলটাই মুছে ফেলা হয়েছে (FirebaseSyncManager.kt-এর সাথে)।
    }

    /**
     * TEMPORARY DEBUG HELPER (safe to remove once the crash is fixed).
     * Catches any uncaught crash anywhere in the app, saves the full stack trace to a text file
     * (backup, in case a file manager is needed), AND immediately opens a simple on-screen
     * activity showing the same text with a "copy" button -- no PC/USB/adb/file-manager needed,
     * the crash text is just right there on screen to read and copy.
     */
    private fun installCrashLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val writer = java.io.StringWriter()
            throwable.printStackTrace(java.io.PrintWriter(writer))
            val crashText = "Thread: ${thread.name}\n\n" + writer.toString()

            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                val file = java.io.File(dir, "crash_log.txt")
                file.appendText("===== CRASH at ${java.util.Date()} =====\n$crashText\n\n")
            } catch (e: Throwable) {
                Log.e("CrashLogger", "Failed to write crash log file: ${e.message}")
            }

            try {
                val intent = android.content.Intent(this, CrashActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("crash_text", crashText)
                }
                startActivity(intent)
                Runtime.getRuntime().exit(0)
            } catch (e: Throwable) {
                Log.e("CrashLogger", "Failed to show crash screen: ${e.message}")
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

}

