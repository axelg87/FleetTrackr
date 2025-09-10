package com.fleetmanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.auth.AuthService
import com.fleetmanager.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val autoSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val dailyRemindersEnabled: Boolean = true,
    val selectedTheme: String = "System",
    val appVersion: String = "1.1.0",
    val lastSyncTime: String = "Never",
    val isSyncing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authService: AuthService,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // TODO: Load settings from preferences
            // For now, using default values
            _uiState.value = _uiState.value.copy(
                lastSyncTime = getLastSyncTime()
            )
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoSyncEnabled = enabled)
        // TODO: Save to preferences
        if (enabled) {
            syncManager.startPeriodicSync()
        } else {
            syncManager.stopPeriodicSync()
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
        // TODO: Save to preferences and update notification settings
    }

    fun toggleDailyReminders(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dailyRemindersEnabled = enabled)
        // TODO: Save to preferences and schedule/cancel daily reminders
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, error = null)
            
            try {
                syncManager.syncNow()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = getCurrentTime()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = "Sync failed: ${e.message}"
                )
            }
        }
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                // TODO: Implement data export functionality
                _uiState.value = _uiState.value.copy(error = "Export feature coming soon!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authService.signOut()
                // Navigation will be handled by the auth state observer in MainActivity
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Sign out failed: ${e.message}"
                )
            }
        }
    }

    private fun getLastSyncTime(): String {
        // TODO: Get actual last sync time from preferences
        return "Just now"
    }

    private fun getCurrentTime(): String {
        return java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
}