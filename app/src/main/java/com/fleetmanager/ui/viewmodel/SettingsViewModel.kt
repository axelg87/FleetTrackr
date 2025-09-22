package com.fleetmanager.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.ExpenseTypeFirestoreService
import com.fleetmanager.data.excel.ExcelImportManager
import com.fleetmanager.data.excel.ImportProgress
import com.fleetmanager.data.preferences.SettingsPreferencesDataStore
import com.fleetmanager.data.preferences.SettingsPreferences
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.repository.AuthRepository
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.usecase.SaveDriverUseCase
import com.fleetmanager.domain.usecase.SaveVehicleUseCase
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.ui.utils.ReportExporter
import com.fleetmanager.ui.utils.ExportResult
import com.fleetmanager.ui.model.ReportEntry
import com.fleetmanager.ui.model.toReportEntries
import com.fleetmanager.ui.model.toReportEntry
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class SettingsUiState(
    val autoSyncEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val dailyRemindersEnabled: Boolean = true,
    val selectedTheme: String = "System",
    val appVersion: String = "1.1.0",
    val lastSyncTime: String = "Never",
    val isSyncing: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null,
    val isSignedIn: Boolean = false,
    val message: String? = null,
    val currentUserRole: UserRole? = null,
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null
) {
    val canSeeAdminControls: Boolean
        get() = currentUserRole?.let { PermissionManager.canSeeAdminControls(it) } ?: false
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userFirestoreService: UserFirestoreService,
    private val expenseTypeFirestoreService: ExpenseTypeFirestoreService,
    private val syncManager: SyncManager,
    private val excelImportManager: ExcelImportManager,
    private val settingsPreferencesDataStore: SettingsPreferencesDataStore,
    private val fleetRepository: FleetRepository,
    private val reportExporter: ReportExporter,
    private val saveDriverUseCase: SaveDriverUseCase,
    private val saveVehicleUseCase: SaveVehicleUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<SettingsUiState>() {

    override fun getInitialState() = SettingsUiState()
    
    // User profile state
    val userProfile: StateFlow<com.fleetmanager.data.dto.UserDto> = userFirestoreService.getCurrentUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.fleetmanager.data.dto.UserDto("", "Loading...", "", UserRole.DRIVER, null)
        )

    init {
        observeAuthState()
        loadUserRole()
        observeSettingsPreferences()
    }
    
    private fun observeAuthState() {
        executeAsync {
            authRepository.isSignedIn.collect { isSignedIn ->
                updateState { it.copy(isSignedIn = isSignedIn) }
            }
        }
    }

    private fun observeSettingsPreferences() {
        executeAsync {
            settingsPreferencesDataStore.settingsPreferences.collect { preferences ->
                updateState { currentState ->
                    currentState.copy(
                        autoSyncEnabled = preferences.autoSyncEnabled,
                        notificationsEnabled = preferences.notificationsEnabled,
                        dailyRemindersEnabled = preferences.dailyRemindersEnabled,
                        selectedTheme = preferences.selectedTheme,
                        lastSyncTime = preferences.lastSyncTime
                    )
                }
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        executeAsync {
            settingsPreferencesDataStore.setAutoSyncEnabled(enabled)
            if (enabled) {
                syncManager.startPeriodicSync()
            } else {
                syncManager.stopPeriodicSync()
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        executeAsync {
            settingsPreferencesDataStore.setNotificationsEnabled(enabled)
            // TODO: Update notification settings in system
        }
    }

    fun toggleDailyReminders(enabled: Boolean) {
        executeAsync {
            settingsPreferencesDataStore.setDailyRemindersEnabled(enabled)
            // TODO: Schedule/cancel daily reminders
        }
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
            settingsPreferencesDataStore.updateLastSyncTimestamp()
            updateState {
                it.copy(
                    isSyncing = false,
                    message = "Data synced successfully"
                )
            }
        }
    }

    fun exportData() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isExporting = isLoading, error = null) }
            },
            onError = { error ->
                updateState { it.copy(isExporting = false, error = "Export failed: $error") }
            }
        ) {
            // Fetch all data for export
            val allReportEntries = mutableListOf<ReportEntry>()
            
            // Get all daily entries and convert to report entries
            val dailyEntries = fleetRepository.getAllDailyEntries().first()
            val drivers = fleetRepository.getAllDrivers().first()
            val vehicles = fleetRepository.getAllActiveVehicles().first()
            val driverNameMap = drivers.associateBy({ it.id }, { it.name })
            val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })
            dailyEntries.forEach { dailyEntry ->
                val enrichedEntry = dailyEntry.withResolvedDisplayData(
                    driverDisplayName = driverNameMap[dailyEntry.driverId],
                    vehicleDisplayName = vehicleNameMap[dailyEntry.vehicleId]
                )
                allReportEntries.addAll(enrichedEntry.toReportEntries())
            }
            
            // Get all expenses and convert to report entries
            val expenses = fleetRepository.getAllExpenses().first()
            expenses.forEach { expense ->
                allReportEntries.add(expense.toReportEntry())
            }
            
            // Sort by date descending
            allReportEntries.sortByDescending { it.date }
            
            if (allReportEntries.isEmpty()) {
                updateState { 
                    it.copy(
                        isExporting = false,
                        message = "No data to export"
                    )
                }
                return@executeAsync
            }
            
            // Use the ReportExporter to export data
            val result = reportExporter.exportToCsv(
                context = context,
                entries = allReportEntries
            )
            
            when (result) {
                is ExportResult.Success -> {
                    updateState { 
                        it.copy(
                            isExporting = false,
                            message = "Data exported successfully to: ${result.filePath}"
                        )
                    }
                }
                is ExportResult.Error -> {
                    updateState { 
                        it.copy(
                            isExporting = false,
                            error = result.message
                        )
                    }
                }
            }
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
                    // Clear preferences when signing out
                    settingsPreferencesDataStore.clearAllPreferences()
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
    fun addDriver(driver: Driver) {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canCreateDrivers(currentRole)) {
            updateState { it.copy(error = "You don't have permission to create drivers") }
            return
        }
        executeAsync {
            val result = saveDriverUseCase(driver)
            result.fold(
                onSuccess = {
                    updateState {
                        it.copy(
                            message = "Driver '${driver.name}' saved successfully",
                            error = null
                        )
                    }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            error = "Failed to add driver: ${throwable.message ?: "Unknown error"}",
                            message = null
                        )
                    }
                }
            )
        }
    }

    fun addVehicle(vehicle: Vehicle) {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canCreateVehicles(currentRole)) {
            updateState { it.copy(error = "You don't have permission to create vehicles") }
            return
        }

        executeAsync {
            val result = saveVehicleUseCase(vehicle)
            result.fold(
                onSuccess = {
                    updateState {
                        it.copy(
                            message = "Vehicle '${vehicle.displayName}' added successfully",
                            error = null
                        )
                    }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            error = "Failed to add vehicle: ${throwable.message ?: "Unknown error"}",
                            message = null
                        )
                    }
                }
            )
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

    
    // Excel Import functionality
    private var pendingExcelImport: Uri? = null
    
    fun importExcelEntries() {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canEdit(currentRole)) {
            updateState { it.copy(error = "You don't have permission to import Excel files") }
            return
        }
        
        // This would trigger file picker in the UI
        // For now, we'll show a message that file picker is needed
        updateState { 
            it.copy(message = "Please select an Excel file to import. This feature requires file picker integration.") 
        }
    }
    
    fun importExcelFromUri(uri: Uri) {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canEdit(currentRole)) {
            updateState { it.copy(error = "You don't have permission to import Excel files") }
            return
        }
        
        executeAsync(
            onLoading = { isLoading ->
                updateState { 
                    it.copy(
                        isImporting = isLoading,
                        error = null,
                        importProgress = if (isLoading) ImportProgress(
                            currentStep = "Starting import...",
                            progress = 0
                        ) else null
                    ) 
                }
            },
            onError = { error ->
                updateState { 
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        error = "Import failed: $error"
                    ) 
                }
            }
        ) {
            val finalProgress = excelImportManager.importExcelEntries(uri) { progress ->
                updateState { 
                    it.copy(importProgress = progress) 
                }
            }
            
            updateState { 
                it.copy(
                    isImporting = false,
                    importProgress = finalProgress,
                    message = if (finalProgress.errors.isEmpty()) {
                        "✅ Successfully imported ${finalProgress.processedEntries} entries"
                    } else {
                        "⚠️ Import completed with ${finalProgress.errors.size} errors"
                    }
                ) 
            }
        }
    }
    
    fun clearImportProgress() {
        updateState { it.copy(importProgress = null) }
    }
    
    fun setError(error: String) {
        updateState { it.copy(error = error) }
    }
}