package com.fleetmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Expense
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetAllEntriesRealtimeUseCase
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.Vehicle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.util.Date

data class EntryListUiState(
    val entries: List<DailyEntry> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val filter: HistoryFilter = HistoryFilter.INCOME,
    val isSelectionMode: Boolean = false,
    val selectedEntryIds: Set<String> = emptySet(),
    val driverUsers: List<UserDto> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val isBulkEditing: Boolean = false,
    val bulkEditMessage: String? = null,
    val bulkEditError: String? = null
)

data class HistoryData(
    val entries: List<DailyEntry>,
    val expenses: List<Expense>,
    val driverUsers: List<UserDto>,
    val vehicles: List<Vehicle>
)

enum class HistoryFilter {
    INCOME,
    EXPENSE
}

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val getAllEntriesRealtimeUseCase: GetAllEntriesRealtimeUseCase,
    private val firestoreService: FirestoreService,
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val authRepository: com.fleetmanager.domain.repository.AuthRepository,
    private val deleteDailyEntryUseCase: com.fleetmanager.domain.usecase.DeleteDailyEntryUseCase,
    private val saveDailyEntryUseCase: com.fleetmanager.domain.usecase.SaveDailyEntryUseCase
) : BaseViewModel<EntryListUiState>() {
    
    companion object {
        private const val TAG = "EntryListViewModel"
    }
    
    override fun getInitialState() = EntryListUiState()
    
    // Expose user profile from Firestore
    val userProfile: StateFlow<UserDto> = firestoreService.getCurrentUserProfile()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserDto("", "Loading...", "", UserRole.DRIVER, null)
        )
    
    // Expose user role for convenience
    val userRole: StateFlow<UserRole> = userProfile
        .map { it.role }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserRole.DRIVER
        )
    
    init {
        observeAuthStateChanges()
        observeEntriesWithRole()
    }
    
    private fun observeAuthStateChanges() {
        executeAsync {
            authRepository.isSignedIn.collect { isSignedIn ->
                if (!isSignedIn) {
                    // User signed out, reset the ViewModel
                    resetToInitialState()
                } else {
                    // User signed in, reload data
                    observeEntriesWithRole()
                }
            }
        }
    }
    
    private fun observeEntriesWithRole() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                Log.e(TAG, "Error observing entries: $error")
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            userRole.collect { role ->
                combine(
                    firestoreService.getDailyEntriesFlowForRole(role),
                    firestoreService.getExpensesFlowForRole(role),
                    userFirestoreService.getDriverUsersFlow(),
                    vehicleFirestoreService.getVehiclesFlow()
                ) { entries, expenses, driverUsers, vehicles ->
                    HistoryData(entries, expenses, driverUsers, vehicles)
                }
                    .catch { e ->
                        Log.e(TAG, "Firestore snapshot listener error", e)
                        updateState {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Failed to load entries: ${e.message}"
                            ) 
                        }
                    }
                    .collect { (entries, expenses, driverUsers, vehicles) ->
                        Log.d(TAG, "Received ${entries.size} entries for role $role")
                        val driverNameMap = driverUsers.associateBy({ it.id }, { it.name })
                        val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })
                        val enrichedEntries = entries.map { entry ->
                            entry.withResolvedDisplayData(
                                driverDisplayName = driverNameMap[entry.driverId],
                                vehicleDisplayName = vehicleNameMap[entry.vehicleId]
                            )
                        }
                        // Sort entries by date descending (most recent first)
                        val sortedEntries = enrichedEntries.sortedByDescending { it.date }
                        val sortedExpenses = expenses.sortedByDescending { it.date }
                        updateState {
                            it.copy(
                                entries = sortedEntries,
                                expenses = sortedExpenses,
                                driverUsers = driverUsers,
                                vehicles = vehicles,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
            }
        }
    }
    
    fun deleteEntry(entryId: String, onSuccess: (() -> Unit)? = null) {
        executeAsync(
            onError = { error ->
                Log.e(TAG, "Error deleting entry: $error")
                updateState { it.copy(errorMessage = "Failed to delete entry: $error") }
            }
        ) {
            deleteDailyEntryUseCase(entryId)
            onSuccess?.invoke()
        }
    }

    fun updateFilter(filter: HistoryFilter) {
        updateState {
            if (it.filter == filter) it else it.copy(
                filter = filter,
                isSelectionMode = false,
                selectedEntryIds = emptySet()
            )
        }
    }

    fun startBulkEdit() {
        updateState { it.copy(isSelectionMode = true, selectedEntryIds = emptySet()) }
    }

    fun cancelBulkEdit() {
        updateState {
            it.copy(
                isSelectionMode = false,
                selectedEntryIds = emptySet(),
                isBulkEditing = false
            )
        }
    }

    fun toggleEntrySelection(entryId: String) {
        updateState { currentState ->
            val mutableSelection = currentState.selectedEntryIds.toMutableSet()
            if (!mutableSelection.add(entryId)) {
                mutableSelection.remove(entryId)
            }
            currentState.copy(
                selectedEntryIds = mutableSelection,
                isSelectionMode = true && mutableSelection.isNotEmpty()
            )
        }
    }

    fun clearSelectionIfEmpty() {
        val currentState = uiState.value
        if (currentState.selectedEntryIds.isEmpty() && currentState.isSelectionMode) {
            updateState { it.copy(isSelectionMode = false) }
        }
    }

    fun selectAllEntries() {
        val allIds = uiState.value.entries.map { it.id }.toSet()
        updateState { it.copy(selectedEntryIds = allIds, isSelectionMode = allIds.isNotEmpty()) }
    }

    fun clearBulkEditMessage() {
        updateState { it.copy(bulkEditMessage = null) }
    }

    fun clearBulkEditError() {
        updateState { it.copy(bulkEditError = null) }
    }

    fun bulkUpdateSelectedEntries(driverId: String, vehicleId: String) {
        val currentState = uiState.value
        val driver = currentState.driverUsers.firstOrNull { it.id == driverId }
        val vehicle = currentState.vehicles.firstOrNull { it.id == vehicleId }

        if (driver == null || vehicle == null) {
            updateState {
                it.copy(bulkEditError = "Please select valid driver and vehicle")
            }
            return
        }

        val entriesToUpdate = currentState.entries.filter { currentState.selectedEntryIds.contains(it.id) }
        if (entriesToUpdate.isEmpty()) {
            updateState { it.copy(bulkEditError = "Select at least one entry to update") }
            return
        }

        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isBulkEditing = isLoading, bulkEditError = null) }
            },
            onError = { error ->
                updateState {
                    it.copy(
                        isBulkEditing = false,
                        bulkEditError = error,
                        bulkEditMessage = null
                    )
                }
            }
        ) {
            entriesToUpdate.forEach { entry ->
                val updatedEntry = entry.copy(
                    driverId = driver.id,
                    driverName = driver.name,
                    vehicleId = vehicle.id,
                    vehicle = vehicle.displayName,
                    updatedAt = Date(),
                    userId = entry.userId
                )

                val result = saveDailyEntryUseCase(updatedEntry)
                result.getOrThrow()
            }

            updateState {
                it.copy(
                    isBulkEditing = false,
                    isSelectionMode = false,
                    selectedEntryIds = emptySet(),
                    bulkEditMessage = "Updated ${entriesToUpdate.size} entries",
                    bulkEditError = null
                )
            }
        }
    }
}