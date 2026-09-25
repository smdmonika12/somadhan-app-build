package com.example.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.repository.SomadhanRepository
import java.util.concurrent.TimeUnit

class InstantJobExpiryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = SomadhanRepository(db)
            val expiredCount = repository.checkAndExpireInstantJobs()
            if (com.example.BuildConfig.DEBUG) {
                Log.d("InstantJobDebug", "InstantJobExpiryWorker.doWork() completed. Expired count: $expiredCount, timestamp=${System.currentTimeMillis()}")
            }
            if (expiredCount > 0) {
                Log.d("InstantJobExpiryWorker", "Successfully expired $expiredCount timed-out instant jobs")
            }
            Result.success()
        } catch (e: Exception) {
            if (com.example.BuildConfig.DEBUG) {
                Log.e("InstantJobDebug", "InstantJobExpiryWorker.doWork() error: ${e.message}", e)
            }
            Log.e("InstantJobExpiryWorker", "Error executing instant job expiry check", e)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "instant_job_expiry_periodic_work"
        const val WORK_TAG = "instant_job_expiry"
        const val ACTION_EXPIRE_INSTANT_JOB = "com.example.ACTION_EXPIRE_INSTANT_JOB"

        fun schedulePeriodic(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<InstantJobExpiryWorker>(
                    15, TimeUnit.MINUTES
                )
                    .addTag(WORK_TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } catch (e: Exception) {
                Log.w("InstantJobExpiryWorker", "Failed to schedule periodic work: ${e.message}")
            }
        }

        fun triggerImmediate(context: Context) {
            try {
                val workRequest = OneTimeWorkRequestBuilder<InstantJobExpiryWorker>()
                    .addTag(WORK_TAG)
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (e: Exception) {
                Log.w("InstantJobExpiryWorker", "Failed to trigger immediate work: ${e.message}")
            }
        }

        fun scheduleExactExpiryAlarm(context: Context, problemId: String, delaySeconds: Long) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, InstantJobAlarmReceiver::class.java).apply {
                    action = ACTION_EXPIRE_INSTANT_JOB
                    putExtra("problemId", problemId)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    problemId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAtMillis = System.currentTimeMillis() + (delaySeconds * 1000L)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                if (com.example.BuildConfig.DEBUG) {
                    Log.d("InstantJobDebug", "scheduleExactExpiryAlarm() scheduled for problemId=$problemId in ${delaySeconds}s (triggerAtMillis=$triggerAtMillis, now=${System.currentTimeMillis()})")
                }
                Log.d("InstantJobExpiryWorker", "Scheduled exact expiry alarm for $problemId in ${delaySeconds}s")
            } catch (e: Exception) {
                if (com.example.BuildConfig.DEBUG) {
                    Log.e("InstantJobDebug", "Failed to schedule exact expiry alarm for $problemId: ${e.message}", e)
                }
                Log.w("InstantJobExpiryWorker", "Failed to schedule exact expiry alarm for $problemId: ${e.message}")
            }
        }

        fun cancelExpiryAlarm(context: Context, problemId: String) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val intent = Intent(context, InstantJobAlarmReceiver::class.java).apply {
                    action = ACTION_EXPIRE_INSTANT_JOB
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    problemId.hashCode(),
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d("InstantJobExpiryWorker", "Cancelled expiry alarm for $problemId")
                }
            } catch (e: Exception) {
                Log.w("InstantJobExpiryWorker", "Failed to cancel alarm for $problemId: ${e.message}")
            }
        }
    }
}
