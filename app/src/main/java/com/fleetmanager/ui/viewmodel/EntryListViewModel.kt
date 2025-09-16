package com.fleetmanager.ui.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetAllEntriesRealtimeUseCase
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.dto.UserDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EntryListUiState(
    val entries: List<DailyEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val getAllEntriesRealtimeUseCase: GetAllEntriesRealtimeUseCase,
    private val firestoreService: FirestoreService
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
        observeEntriesWithRole()
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
                firestoreService.getDailyEntriesFlowForRole(role)
                    .catch { e ->
                        Log.e(TAG, "Firestore snapshot listener error", e)
                        updateState { 
                            it.copy(
                                isLoading = false, 
                                errorMessage = "Failed to load entries: ${e.message}"
                            ) 
                        }
                    }
                    .collect { entries ->
                        Log.d(TAG, "Received ${entries.size} entries for role $role")
                        updateState {
                            it.copy(
                                entries = entries,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
            }
        }
    }
}