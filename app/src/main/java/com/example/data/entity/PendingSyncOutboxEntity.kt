package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Outbox/retry ইনফ্রা — ধাপ ৬ (RPC_SYNC_FIX ট্র্যাক)।
 *
 * ডিজাইন রেফারেন্স: `docs/OUTBOX_RETRY_DESIGN.md` (ধাপ ৫-এ লেখা, ইউজার-approved)। বর্তমান
 * dual-write প্যাটার্নে (Room instant write → best-effort Supabase RPC → fail হলে শুধু
 * `Log.w`) fail-case-টাকে persistent, retry-able record-এ রূপান্তর করার জন্য এই entity।
 *
 * **⚠️ এই ধাপে (Step 6) এই entity সম্পূর্ণ নতুন এবং এখনো কোনো dual-write ফাংশন এটা ব্যবহার
 * করছে না** — কোনো row কখনো insert হবে না যতক্ষণ না Step 7-এ বিদ্যমান ফাংশনের fail-ব্লকে
 * insert কল যোগ হয় (additive)।
 */
@Entity(tableName = "pending_sync_outbox")
data class PendingSyncOutboxEntity(
    @PrimaryKey val id: String,          // ULID/UUID, ক্লায়েন্টে জেনারেট (java.util.UUID.randomUUID().toString())
    val rpcName: String,                 // e.g. "release_escrow", "admin_adjust_balance" (postgrest.rpc()-এর সাথে মিলবে)
    val paramsJson: String,              // RPC-এর প্যারামিটারগুলো JSON-সিরিয়ালাইজড (kotlinx.serialization)
    val createdAt: Long,                 // System.currentTimeMillis(), প্রথম fail-এর সময়
    val retryCount: Int = 0,
    val lastError: String? = null,       // সর্বশেষ retry-র error message (truncated, ৫০০ char cap)
    val lastAttemptAt: Long? = null,
    // status একটা enum না, plain String -- বিদ্যমান কনভেনশন অনুসরণ করা হয়েছে (EscrowEntity.status,
    // WithdrawalEntity.status ইত্যাদিও plain String)। মান: PENDING | RETRYING | FAILED_PERMANENT | DONE।
    // retryCount থ্রেশহোল্ড (প্রস্তাবিত ১০, দেখুন OutboxSyncWorker.MAX_RETRY_COUNT) পার হলে
    // FAILED_PERMANENT। DONE soft-delete-এর মতো রাখা হয়েছে (row মুছে ফেলার বদলে), ভবিষ্যতে
    // admin panel sync-history দেখানোর জন্য -- এই ধাপে prune/cleanup implement করা হচ্ছে না।
    val status: String = "PENDING"
)
