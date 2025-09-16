package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class NavigationUiState(
    val userProfile: UserDto? = null,
    val userRole: UserRole? = null
)

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val userFirestoreService: UserFirestoreService
) : BaseViewModel<NavigationUiState>() {
    
    override fun getInitialState() = NavigationUiState()
    
    // Expose userRole for backward compatibility
    val userRole: StateFlow<UserRole?> = uiState.map { it.userRole }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = null
    )
    
    // Expose userProfile
    val userProfile: StateFlow<UserDto?> = uiState.map { it.userProfile }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = null
    )
    
    init {
        loadUserRole()
    }
    
    private fun loadUserRole() {
        executeAsync(
            onError = { error ->
                // Default to DRIVER role if there's an error
                updateState { it.copy(userRole = UserRole.DRIVER) }
            }
        ) {
            userFirestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { 
                    it.copy(
                        userProfile = userProfile,
                        userRole = userProfile.role
                    ) 
                }
            }
        }
    }
}