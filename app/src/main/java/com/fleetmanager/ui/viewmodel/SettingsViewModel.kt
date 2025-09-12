package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.repository.AuthRepository
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.remote.ExpenseTypeFirestoreService
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
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
    val currentUserRole: UserRole? = null
) {
    val canSeeAdminControls: Boolean
        get() = currentUserRole?.let { PermissionManager.canSeeAdminControls(it) } ?: false
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val expenseTypeFirestoreService: ExpenseTypeFirestoreService,
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
            userFirestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { 
                    it.copy(currentUserRole = userProfile.role) 
                }
            }
        }
    }
    
    // Admin-only functions with permission checks
    fun addDriver(name: String, email: String = "") {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canCreateDrivers(currentRole)) {
            updateState { it.copy(error = "You don't have permission to create drivers") }
            return
        }
        
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to add driver: $error") }
            }
        ) {
            val driver = userFirestoreService.createDriverUser(name, email)
            updateState { 
                it.copy(message = "Driver '${driver.name}' added successfully") 
            }
        }
    }
    
    fun addVehicle(make: String, model: String, year: Int, licensePlate: String) {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canCreateVehicles(currentRole)) {
            updateState { it.copy(error = "You don't have permission to create vehicles") }
            return
        }
        
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to add vehicle: $error") }
            }
        ) {
            val vehicle = vehicleFirestoreService.createVehicle(make, model, year, licensePlate)
            updateState { 
                it.copy(message = "Vehicle '${vehicle.displayName}' added successfully") 
            }
        }
    }
    
    fun addExpenseType(name: String, displayName: String) {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canCreateExpenseTypes(currentRole)) {
            updateState { it.copy(error = "You don't have permission to create expense types") }
            return
        }
        
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Failed to add expense type: $error") }
            }
        ) {
            val expenseType = expenseTypeFirestoreService.createExpenseType(name, displayName)
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