package com.fleetmanager.ui.viewmodel

import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.data.remote.UserFirestoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProfileUiState(
    val userProfile: UserDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userFirestoreService: UserFirestoreService
) : BaseViewModel<ProfileUiState>() {

    override fun getInitialState() = ProfileUiState()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading, error = null) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, error = "Failed to load profile: $error") }
            }
        ) {
            userFirestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { 
                    it.copy(
                        userProfile = userProfile,
                        isLoading = false,
                        error = null
                    ) 
                }
            }
        }
    }

    fun refreshProfile() {
        loadUserProfile()
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}