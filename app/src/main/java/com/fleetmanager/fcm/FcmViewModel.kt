package com.fleetmanager.fcm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FcmViewModel @Inject constructor(
    private val fcmManager: FcmManager,
    private val authService: AuthService,
    private val notificationPermissionHandler: NotificationPermissionHandler
) : ViewModel() {
    
    companion object {
        private const val TAG = "FcmViewModel"
    }
    
    private val _fcmStatus = MutableStateFlow<FcmStatus?>(null)
    val fcmStatus: StateFlow<FcmStatus?> = _fcmStatus.asStateFlow()
    
    private val _permissionStatus = MutableStateFlow(PermissionStatus.Denied)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Combined state for UI
    val uiState: StateFlow<FcmUiState> = combine(
        fcmStatus,
        permissionStatus,
        isLoading,
        error
    ) { status, permission, loading, errorMsg ->
        FcmUiState(
            fcmStatus = status,
            permissionStatus = permission,
            isLoading = loading,
            error = errorMsg,
            isSetupComplete = status?.isFullySetup == true
        )
    }.distinctUntilChanged()
    
    init {
        // Initialize permission status
        updatePermissionStatus()
        
        // Observe authentication state and initialize FCM when user signs in
        viewModelScope.launch {
            authService.isSignedIn.collect { isSignedIn ->
                if (isSignedIn) {
                    initializeFcm()
                } else {
                    cleanupFcm()
                }
            }
        }
    }
    
    /**
     * Initialize FCM for the current user
     */
    fun initializeFcm() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "Initializing FCM...")
                val success = fcmManager.initializeFcm()
                
                if (success) {
                    Log.d(TAG, "FCM initialized successfully")
                    refreshFcmStatus()
                } else {
                    _error.value = "Failed to initialize FCM"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FCM", e)
                _error.value = "Error initializing FCM: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clean up FCM when user signs out
     */
    private fun cleanupFcm() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cleaning up FCM...")
                fcmManager.cleanupFcm()
                _fcmStatus.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up FCM", e)
            }
        }
    }
    
    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission() {
        updatePermissionStatus()
    }
    
    /**
     * Handle permission result
     */
    fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                Log.d(TAG, "Notification permission granted")
                // Reinitialize FCM after permission is granted
                initializeFcm()
            } else {
                Log.d(TAG, "Notification permission denied")
                _error.value = "Notification permission is required for push notifications"
            }
            updatePermissionStatus()
        }
    }
    
    /**
     * Refresh FCM token
     */
    fun refreshToken() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "Refreshing FCM token...")
                val success = fcmManager.refreshToken()
                
                if (success) {
                    Log.d(TAG, "FCM token refreshed successfully")
                    refreshFcmStatus()
                } else {
                    _error.value = "Failed to refresh FCM token"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing FCM token", e)
                _error.value = "Error refreshing token: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Show test notification
     */
    fun showTestNotification() {
        fcmManager.showTestNotification()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Refresh FCM status
     */
    private fun refreshFcmStatus() {
        viewModelScope.launch {
            try {
                val status = fcmManager.getFcmStatus()
                _fcmStatus.value = status
                Log.d(TAG, "FCM Status: $status")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing FCM status", e)
            }
        }
    }
    
    /**
     * Update permission status
     */
    private fun updatePermissionStatus() {
        _permissionStatus.value = notificationPermissionHandler.getPermissionStatus()
    }
    
    /**
     * Subscribe to a topic
     */
    fun subscribeToTopic(topic: String) {
        viewModelScope.launch {
            try {
                val success = fcmManager.subscribeToTopic(topic)
                if (success) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    _error.value = "Failed to subscribe to topic: $topic"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to topic: $topic", e)
                _error.value = "Error subscribing to topic: ${e.message}"
            }
        }
    }
    
    /**
     * Unsubscribe from a topic
     */
    fun unsubscribeFromTopic(topic: String) {
        viewModelScope.launch {
            try {
                val success = fcmManager.unsubscribeFromTopic(topic)
                if (success) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    _error.value = "Failed to unsubscribe from topic: $topic"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unsubscribing from topic: $topic", e)
                _error.value = "Error unsubscribing from topic: ${e.message}"
            }
        }
    }
}

/**
 * UI state for FCM-related screens
 */
data class FcmUiState(
    val fcmStatus: FcmStatus? = null,
    val permissionStatus: PermissionStatus = PermissionStatus.Denied,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSetupComplete: Boolean = false
) {
    val showPermissionRequest: Boolean
        get() = permissionStatus is PermissionStatus.Denied && 
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
    
    val canShowNotifications: Boolean
        get() = permissionStatus.isGrantedOrNotRequired()
}