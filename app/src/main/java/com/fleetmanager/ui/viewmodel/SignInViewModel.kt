package com.fleetmanager.ui.viewmodel

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.fleetmanager.domain.repository.AuthRepository
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.data.remote.FirestoreService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    private val firestoreService: FirestoreService
) : BaseViewModel<SignInUiState>() {
    
    override fun getInitialState() = SignInUiState()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        executeAsync {
            authRepository.isSignedIn.collect { isSignedIn ->
                updateState { it.copy(isSignedIn = isSignedIn) }
                if (isSignedIn) {
                    // Create user document whenever user is signed in (app start or fresh sign-in)
                    try {
                        firestoreService.saveUserIfMissing()
                    } catch (e: Exception) {
                        android.util.Log.e("SignInViewModel", "Failed to create user document in auth observer", e)
                    }
                    
                    // Start periodic sync when user is signed in
                    syncManager.startPeriodicSync()
                }
            }
        }
    }
    
    fun getGoogleSignInIntent(): Intent {
        return authRepository.getGoogleSignInIntent()
    }
    
    fun signInWithGoogle(idToken: String) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isLoading = false, errorMessage = error) }
            }
        ) {
            val result = authRepository.signInWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    updateState { it.copy(isLoading = false) }
                    
                    // Create user document after a short delay to ensure Firebase Auth is ready
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(500) // Wait 500ms for auth to stabilize
                        try {
                            firestoreService.saveUserIfMissing()
                        } catch (e: Exception) {
                            android.util.Log.e("SignInViewModel", "User creation failed", e)
                        }
                    }
                    
                    // Trigger initial sync
                    syncManager.triggerManualSync()
                },
                onFailure = { error ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Sign in failed"
                        )
                    }
                }
            )
        }
    }
    
    fun onError(message: String) {
        updateState { it.copy(errorMessage = message, isLoading = false) }
    }
}