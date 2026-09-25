package com.example.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.repository.SomadhanRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InstantJobAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val problemId = intent?.getStringExtra("problemId")
        if (BuildConfig.DEBUG) {
            Log.d("InstantJobDebug", "InstantJobAlarmReceiver.onReceive() triggered for problemId=$problemId at timestamp=${System.currentTimeMillis()}")
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context.applicationContext)
                val repository = SomadhanRepository(db)
                val expiredCount = repository.checkAndExpireInstantJobs()
                if (BuildConfig.DEBUG) {
                    Log.d("InstantJobDebug", "InstantJobAlarmReceiver goAsync completed. Expired count: $expiredCount")
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("InstantJobDebug", "InstantJobAlarmReceiver goAsync error: ${e.message}", e)
                }
            } finally {
                pendingResult.finish()
            }
        }

        InstantJobExpiryWorker.triggerImmediate(context)
    }
}
