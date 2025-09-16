package com.fleetmanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings_preferences"
)

/**
 * Data class representing user's settings preferences
 */
data class SettingsPreferences(
    val autoSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val dailyRemindersEnabled: Boolean = true,
    val selectedTheme: String = "System",
    val lastSyncTimestamp: Long = 0L
) {
    val lastSyncTime: String
        get() = if (lastSyncTimestamp == 0L) {
            "Never"
        } else {
            val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            dateFormatter.format(Date(lastSyncTimestamp))
        }
}

@Singleton
class SettingsPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val DAILY_REMINDERS_ENABLED = booleanPreferencesKey("daily_reminders_enabled")
        private val SELECTED_THEME = stringPreferencesKey("selected_theme")
        private val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    }
    
    /**
     * Get settings preferences flow
     */
    val settingsPreferences: Flow<SettingsPreferences> = context.settingsPreferencesDataStore.data.map { preferences ->
        SettingsPreferences(
            autoSyncEnabled = preferences[AUTO_SYNC_ENABLED] ?: true,
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
            dailyRemindersEnabled = preferences[DAILY_REMINDERS_ENABLED] ?: true,
            selectedTheme = preferences[SELECTED_THEME] ?: "System",
            lastSyncTimestamp = preferences[LAST_SYNC_TIMESTAMP] ?: 0L
        )
    }
    
    /**
     * Update auto sync preference
     */
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.settingsPreferencesDataStore.edit { preferences ->
            preferences[AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    /**
     * Update notifications preference
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsPreferencesDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    /**
     * Update daily reminders preference
     */
    suspend fun setDailyRemindersEnabled(enabled: Boolean) {
        context.settingsPreferencesDataStore.edit { preferences ->
            preferences[DAILY_REMINDERS_ENABLED] = enabled
        }
    }
    
    /**
     * Update selected theme preference
     */
    suspend fun setSelectedTheme(theme: String) {
        context.settingsPreferencesDataStore.edit { preferences ->
            preferences[SELECTED_THEME] = theme
        }
    }
    
    /**
     * Update last sync timestamp
     */
    suspend fun updateLastSyncTimestamp() {
        context.settingsPreferencesDataStore.edit { preferences ->
            preferences[LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }
    
    /**
     * Clear all preferences (useful for sign out)
     */
    suspend fun clearAllPreferences() {
        context.settingsPreferencesDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}