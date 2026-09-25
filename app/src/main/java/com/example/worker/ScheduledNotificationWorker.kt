package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.repository.SomadhanRepository

class ScheduledNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE)
        val message = inputData.getString(KEY_MESSAGE)
        val role = inputData.getString(KEY_ROLE) ?: "ALL"
        val type = inputData.getString(KEY_TYPE) ?: "general"
        val scheduledFor = inputData.getLong(KEY_SCHEDULED_FOR, 0L).takeIf { it > 0L }

        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = SomadhanRepository(db)

            // Check & run inactivity reputation decay
            try {
                repository.runInactivityReputationDecay()
            } catch (e: Exception) {
                Log.w("ScheduledNotifWorker", "Inactivity reputation decay check error: ${e.message}")
            }

            try {
                repository.runMonthlyFreeQuotaReset()
            } catch (e: Exception) {
                Log.w("ScheduledNotifWorker", "Monthly free quota reset error: ${e.message}")
            }

            try {
                repository.checkAndExpireInstantJobs()
            } catch (e: Exception) {
                Log.w("ScheduledNotifWorker", "Instant job expiry check error: ${e.message}")
            }

            // Send or confirm scheduled notification
            if (title != null && message != null) {
                repository.sendManualNotification(
                    targetRole = role,
                    title = title,
                    message = message,
                    targetType = type,
                    scheduledFor = scheduledFor
                )
                Log.d("ScheduledNotifWorker", "Successfully executed scheduled notification: $title")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("ScheduledNotifWorker", "Error executing scheduled notification", e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_TITLE = "key_title"
        const val KEY_MESSAGE = "key_message"
        const val KEY_ROLE = "key_role"
        const val KEY_TYPE = "key_type"
        const val KEY_SCHEDULED_FOR = "key_scheduled_for"
        const val WORK_TAG_PREFIX = "scheduled_notif_"
    }
}
