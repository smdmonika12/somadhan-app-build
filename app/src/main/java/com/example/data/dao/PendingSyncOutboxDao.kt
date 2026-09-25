package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.PendingSyncOutboxEntity
import kotlinx.coroutines.flow.Flow

/**
 * Outbox/retry ইনফ্রা — ধাপ ৬ (RPC_SYNC_FIX ট্র্যাক)। ডিজাইন রেফারেন্স:
 * `docs/OUTBOX_RETRY_DESIGN.md` সেকশন ২।
 *
 * বিদ্যমান `AppDaos.kt` ফাইলে যোগ না করে ইচ্ছাকৃতভাবে আলাদা ফাইলে রাখা হয়েছে, যাতে এই ধাপে
 * বিদ্যমান কোনো ফাইলের ভেতরের বিদ্যমান কোড এডিট করতে না হয় (শুধু নতুন ফাইল যোগ, Step 6-এর
 * কমন রুল)। `AppDatabase.kt`-এ entity রেজিস্টার করা আর abstract dao accessor যোগ করাটাই
 * এই ধাপের একমাত্র "বিদ্যমান ফাইল টাচ"।
 */
@Dao
interface PendingSyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PendingSyncOutboxEntity)

    @Update
    suspend fun update(entry: PendingSyncOutboxEntity)

    @Query("SELECT * FROM pending_sync_outbox WHERE status IN ('PENDING','RETRYING') ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingSyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM pending_sync_outbox WHERE status IN ('PENDING','RETRYING')")
    fun observePendingCount(): Flow<Int>   // UI ইন্ডিকেটরের জন্য (Step 8), এই ধাপে কোথাও collect করা হয় না

    @Query("SELECT * FROM pending_sync_outbox WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PendingSyncOutboxEntity?

    @Query("SELECT * FROM pending_sync_outbox ORDER BY createdAt ASC")
    suspend fun getAllSync(): List<PendingSyncOutboxEntity>   // ইউনিট টেস্টে assert করার জন্য সুবিধাজনক

    @Query("DELETE FROM pending_sync_outbox WHERE id = :id")
    suspend fun deleteById(id: String)
}
