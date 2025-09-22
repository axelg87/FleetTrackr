package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetEntryByIdUseCase
import com.fleetmanager.domain.usecase.DeleteDailyEntryUseCase
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EntryDetailUiState(
    val entry: DailyEntry? = null,
    val isLoading: Boolean = true, // Start with loading state since we expect to load data
    val errorMessage: String? = null
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val deleteDailyEntryUseCase: DeleteDailyEntryUseCase,
    private val firestoreService: FirestoreService,
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService
) : BaseViewModel<EntryDetailUiState>() {
    
    override fun getInitialState() = EntryDetailUiState()
    
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
    
    fun loadEntry(entryId: String) {
        if (entryId.isBlank()) {
            updateState { it.copy(isLoading = false, errorMessage = "Invalid entry ID") }
            return
        }
        
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading, errorMessage = null) }
            },
            onError = { error ->
                val errorMessage = when {
                    error.contains("not found", ignoreCase = true) -> "Entry not found. It may have been deleted or you don't have permission to view it."
                    error.contains("network", ignoreCase = true) || error.contains("connection", ignoreCase = true) -> "Network error. Please check your connection and try again."
                    error.contains("permission", ignoreCase = true) -> "You don't have permission to view this entry."
                    else -> "Failed to load entry: $error"
                }
                updateState { it.copy(isLoading = false, errorMessage = errorMessage) }
            }
        ) {
            combine(
                getEntryByIdUseCase(entryId),
                userFirestoreService.getDriverUsersFlow(),
                vehicleFirestoreService.getVehiclesFlow()
            ) { entry, driverUsers, vehicles ->
                if (entry == null) {
                    null
                } else {
                    val driverNameMap = driverUsers.associateBy({ it.id }, { it.name })
                    val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })
                    entry.withResolvedDisplayData(
                        driverDisplayName = driverNameMap[entry.driverId],
                        vehicleDisplayName = vehicleNameMap[entry.vehicleId]
                    )
                }
            }.collect { enrichedEntry ->
                updateState {
                    it.copy(
                        entry = enrichedEntry,
                        isLoading = false,
                        errorMessage = if (enrichedEntry == null) "Entry not found. It may have been deleted or moved." else null
                    )
                }
            }
        }
    }
    
    fun deleteEntry(entryId: String, onSuccess: () -> Unit) {
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = "Failed to delete entry: $error") }
            }
        ) {
            deleteDailyEntryUseCase(entryId)
            onSuccess()
        }
    }
}