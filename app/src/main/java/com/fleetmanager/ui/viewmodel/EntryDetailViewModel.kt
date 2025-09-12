package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetEntryByIdUseCase
import com.fleetmanager.domain.usecase.DeleteDailyEntryUseCase
import com.fleetmanager.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EntryDetailUiState(
    val entry: DailyEntry? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userRole: UserRole = UserRole.DRIVER,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isDeleting: Boolean = false
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val getEntryByIdUseCase: GetEntryByIdUseCase,
    private val deleteDailyEntryUseCase: DeleteDailyEntryUseCase,
    private val authService: AuthService,
    private val userRoleViewModel: UserRoleViewModel
) : BaseViewModel<EntryDetailUiState>() {
    
    override fun getInitialState() = EntryDetailUiState()
    
    init {
        observeUserRole()
    }
    
    private fun observeUserRole() {
        executeAsync {
            userRoleViewModel.uiState.collect { roleState ->
                updateState {
                    it.copy(
                        userRole = roleState.userRole,
                        canEdit = roleState.canEdit,
                        canDelete = roleState.canDelete
                    )
                }
            }
        }
    }
    
    fun loadEntry(entryId: String) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            getEntryByIdUseCase(entryId)
                .collect { entry ->
                    val currentUserId = authService.getCurrentUserId()
                    val roleState = userRoleViewModel.uiState.value
                    
                    // Check if user can access this specific entry
                    val canAccess = userRoleViewModel.canAccessResource(entry?.userId ?: "")
                    val canEditThis = userRoleViewModel.canEditResource(entry?.userId ?: "")
                    val canDeleteThis = userRoleViewModel.canDeleteResource(entry?.userId ?: "")
                    
                    updateState {
                        it.copy(
                            entry = if (canAccess) entry else null,
                            isLoading = false,
                            errorMessage = if (!canAccess && entry != null) "Access denied" else null,
                            canEdit = canEditThis,
                            canDelete = canDeleteThis
                        )
                    }
                }
        }
    }
    
    fun deleteEntry(entryId: String, onSuccess: () -> Unit) {
        val entry = _uiState.value.entry
        if (entry == null || !userRoleViewModel.canDeleteResource(entry.userId)) {
            updateState { it.copy(errorMessage = "Not authorized to delete this entry") }
            return
        }
        
        executeAsync(
            onLoading = { isDeleting ->
                updateState { it.copy(isDeleting = isDeleting, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isDeleting = false, errorMessage = "Failed to delete entry: $error") }
            }
        ) {
            deleteDailyEntryUseCase(entryId)
            updateState { it.copy(isDeleting = false) }
            onSuccess()
        }
    }
}