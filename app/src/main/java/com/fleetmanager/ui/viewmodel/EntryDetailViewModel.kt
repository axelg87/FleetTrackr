package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetEntryByIdUseCase
import com.fleetmanager.domain.usecase.DeleteDailyEntryUseCase
import com.fleetmanager.data.remote.FirestoreService
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
    private val firestoreService: FirestoreService
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
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, errorMessage = "Failed to load entry: $error") }
            }
        ) {
            getEntryByIdUseCase(entryId)
                .collect { entry ->
                    updateState {
                        it.copy(
                            entry = entry,
                            isLoading = false,
                            errorMessage = if (entry == null) "Entry not found" else null
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