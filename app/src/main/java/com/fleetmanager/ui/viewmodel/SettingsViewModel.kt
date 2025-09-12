package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.repository.AuthRepository
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.domain.model.UserRole
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
    val error: String? = null,
    val isSignedIn: Boolean = false,
    val message: String? = null,
    val currentUserRole: UserRole? = null,
    val isAdmin: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreService: FirestoreService,
    private val syncManager: SyncManager
) : BaseViewModel<SettingsUiState>() {

    override fun getInitialState() = SettingsUiState()

    init {
        loadSettings()
        observeAuthState()
        loadUserRole()
    }
    
    private fun observeAuthState() {
        executeAsync {
            authRepository.isSignedIn.collect { isSignedIn ->
                updateState { it.copy(isSignedIn = isSignedIn) }
            }
        }
    }

    private fun loadSettings() {
        executeAsync {
            // TODO: Load settings from preferences
            // For now, using default values
            updateState { it.copy(lastSyncTime = getLastSyncTime()) }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        updateState { it.copy(autoSyncEnabled = enabled) }
        // TODO: Save to preferences
        if (enabled) {
            syncManager.startPeriodicSync()
        } else {
            syncManager.stopPeriodicSync()
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        updateState { it.copy(notificationsEnabled = enabled) }
        // TODO: Save to preferences and update notification settings
    }

    fun toggleDailyReminders(enabled: Boolean) {
        updateState { it.copy(dailyRemindersEnabled = enabled) }
        // TODO: Save to preferences and schedule/cancel daily reminders
    }

    fun syncNow() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSyncing = isLoading, error = null) }
            },
            onError = { error ->
                updateState { it.copy(isSyncing = false, error = "Sync failed: $error") }
            }
        ) {
            syncManager.syncNow()
            updateState {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = getCurrentTime(),
                    message = "Data synced successfully"
                )
            }
        }
    }

    fun exportData() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Export failed: $error") }
            }
        ) {
            // TODO: Implement data export functionality
            updateState { it.copy(error = "Export feature coming soon!") }
        }
    }

    fun signOut() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Sign out failed: $error") }
            }
        ) {
            val result = authRepository.signOut()
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Signed out successfully") }
                },
                onFailure = { error ->
                    updateState { it.copy(error = "Sign out failed: ${error.message}") }
                }
            )
        }
    }
    
    fun clearMessage() {
        updateState { it.copy(message = null, error = null) }
    }
    
    private fun loadUserRole() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to load user role: $error") }
            }
        ) {
            firestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { 
                    it.copy(
                        currentUserRole = userProfile.role,
                        isAdmin = userProfile.role == UserRole.ADMIN
                    ) 
                }
            }
        }
    }
    
    // Admin-only functions
    fun addDriver(name: String, email: String = "") {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to add driver: $error") }
            }
        ) {
            val driver = firestoreService.createDriverUser(name, email)
            updateState { 
                it.copy(message = "Driver '${driver.name}' added successfully") 
            }
        }
    }
    
    fun addVehicle(make: String, model: String, year: Int, licensePlate: String) {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to add vehicle: $error") }
            }
        ) {
            val vehicle = firestoreService.createVehicle(make, model, year, licensePlate)
            updateState { 
                it.copy(message = "Vehicle '${vehicle.displayName}' added successfully") 
            }
        }
    }
    
    fun addExpenseType(name: String, displayName: String) {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to add expense type: $error") }
            }
        ) {
            val expenseType = firestoreService.createExpenseType(name, displayName)
            updateState { 
                it.copy(message = "Expense type '${expenseType.displayName}' added successfully") 
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