package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object SolverReminderPrefs {
    private val Context.reminderDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "solver_reminder_dismissed_store")

    private fun dismissedKey(solverId: String) = stringSetPreferencesKey("dismissed_$solverId")

    suspend fun getDismissedJobIds(context: Context, solverId: String): Set<String> {
        return context.reminderDataStore.data.map { prefs ->
            prefs[dismissedKey(solverId)] ?: emptySet()
        }.first()
    }

    suspend fun markDismissed(context: Context, solverId: String, problemId: String) {
        context.reminderDataStore.edit { prefs ->
            val current = prefs[dismissedKey(solverId)] ?: emptySet()
            prefs[dismissedKey(solverId)] = current + problemId
        }
    }
}
