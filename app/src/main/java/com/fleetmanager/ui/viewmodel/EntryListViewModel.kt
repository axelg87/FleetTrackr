package com.fleetmanager.ui.viewmodel

import android.util.Log
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetAllEntriesRealtimeUseCase
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EntryListUiState(
    val entries: List<DailyEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userRole: UserRole = UserRole.DRIVER,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false
)

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val getAllEntriesRealtimeUseCase: GetAllEntriesRealtimeUseCase,
    private val firestoreService: FirestoreService,
    private val authService: AuthService,
    private val userRoleViewModel: UserRoleViewModel
) : BaseViewModel<EntryListUiState>() {
    
    companion object {
        private const val TAG = "EntryListViewModel"
    }
    
    override fun getInitialState() = EntryListUiState()
    
    init {
        observeUserRoleAndEntries()
    }
    
    private fun observeUserRoleAndEntries() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                Log.e(TAG, "Error observing entries: $error")
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            // First observe user role, then switch to appropriate entries flow
            userRoleViewModel.uiState.collect { roleState ->
                if (!roleState.isLoading && roleState.currentUser != null) {
                    val userId = authService.getCurrentUserId() ?: ""
                    
                    // Update state with role permissions
                    updateState {
                        it.copy(
                            userRole = roleState.userRole,
                            canEdit = roleState.canEdit,
                            canDelete = roleState.canDelete
                        )
                    }
                    
                    // Switch to appropriate entries flow based on role
                    firestoreService.getDailyEntriesFlowForRole(roleState.userRole, userId)
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
                            Log.d(TAG, "Received ${entries.size} entries for role ${roleState.userRole}")
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
}