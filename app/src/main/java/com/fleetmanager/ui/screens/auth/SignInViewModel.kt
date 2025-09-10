package com.fleetmanager.ui.screens.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.auth.AuthResult
import com.fleetmanager.auth.AuthService
import com.fleetmanager.data.repository.FleetRepository
import com.fleetmanager.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class SignInUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authService: AuthService,
    private val fleetRepository: FleetRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            authService.isSignedIn.collect { isSignedIn ->
                _uiState.value = _uiState.value.copy(isSignedIn = isSignedIn)
                if (isSignedIn) {
                    // Start periodic sync when user is signed in
                    syncManager.startPeriodicSync()
                }
            }
        }
    }
    
    fun getGoogleSignInIntent(): Intent {
        Log.d("SignInViewModel", "Getting Google Sign-In intent")
        return authService.getGoogleSignInClient().signInIntent
    }
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            Log.d("SignInViewModel", "Starting Google Sign-In with ID token")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = authService.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    Log.d("SignInViewModel", "Google Sign-In successful")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    // Trigger initial sync
                    syncManager.triggerManualSync()
                }
                is AuthResult.Error -> {
                    Log.e("SignInViewModel", "Google Sign-In failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
    
    fun onError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message, isLoading = false)
    }
}