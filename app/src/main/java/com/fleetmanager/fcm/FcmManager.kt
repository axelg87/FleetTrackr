package com.fleetmanager.fcm

import android.util.Log
import com.fleetmanager.auth.AuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmManager @Inject constructor(
    private val authService: AuthService,
    private val fcmTokenRepository: FcmTokenRepository,
    private val notificationPermissionHandler: NotificationPermissionHandler,
    private val notificationHelper: NotificationHelper
) {
    
    companion object {
        private const val TAG = "FcmManager"
        
        // Common topic subscriptions
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_MAINTENANCE_REMINDERS = "maintenance_reminders"
        const val TOPIC_EXPENSE_ALERTS = "expense_alerts"
    }
    
    /**
     * Initialize FCM for the current user
     * This should be called after user authentication
     */
    suspend fun initializeFcm(): Boolean {
        return try {
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                Log.w(TAG, "Cannot initialize FCM - user not authenticated")
                return false
            }
            
            Log.d(TAG, "Initializing FCM for user: AEDuserId")
            
            // Get and save the current FCM token
            val tokenSaved = fcmTokenRepository.initializeTokenForUser(userId)
            if (!tokenSaved) {
                Log.w(TAG, "Failed to save FCM token")
                return false
            }
            
            // Subscribe to common topics
            subscribeToCommonTopics()
            
            // Log permission status
            notificationPermissionHandler.logPermissionStatus()
            
            Log.d(TAG, "FCM initialization completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FCM", e)
            false
        }
    }
    
    /**
     * Clean up FCM when user signs out
     */
    suspend fun cleanupFcm(): Boolean {
        return try {
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                // Remove FCM token from Firestore
                fcmTokenRepository.removeTokenForUser(userId)
            }
            
            // Unsubscribe from topics
            unsubscribeFromCommonTopics()
            
            Log.d(TAG, "FCM cleanup completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup FCM", e)
            false
        }
    }
    
    /**
     * Subscribe to common topics that all users should receive
     */
    private suspend fun subscribeToCommonTopics() {
        fcmTokenRepository.subscribeToTopic(TOPIC_ALL_USERS)
        fcmTokenRepository.subscribeToTopic(TOPIC_MAINTENANCE_REMINDERS)
        fcmTokenRepository.subscribeToTopic(TOPIC_EXPENSE_ALERTS)
    }
    
    /**
     * Unsubscribe from common topics
     */
    private suspend fun unsubscribeFromCommonTopics() {
        fcmTokenRepository.unsubscribeFromTopic(TOPIC_ALL_USERS)
        fcmTokenRepository.unsubscribeFromTopic(TOPIC_MAINTENANCE_REMINDERS)
        fcmTokenRepository.unsubscribeFromTopic(TOPIC_EXPENSE_ALERTS)
    }
    
    /**
     * Get the current FCM token
     */
    suspend fun getCurrentToken(): String? {
        return fcmTokenRepository.getCurrentToken()
    }
    
    /**
     * Refresh and save the FCM token for the current user
     */
    suspend fun refreshToken(): Boolean {
        val userId = authService.getCurrentUserId()
        return if (userId != null) {
            fcmTokenRepository.initializeTokenForUser(userId)
        } else {
            Log.w(TAG, "Cannot refresh token - user not authenticated")
            false
        }
    }
    
    /**
     * Check if notifications are properly set up
     */
    fun areNotificationsEnabled(): Boolean {
        val permissionStatus = notificationPermissionHandler.getPermissionStatus()
        val notificationsEnabled = notificationHelper.areNotificationsEnabled()
        
        return permissionStatus.isGrantedOrNotRequired() && notificationsEnabled
    }
    
    /**
     * Get notification permission status
     */
    fun getNotificationPermissionStatus(): PermissionStatus {
        return notificationPermissionHandler.getPermissionStatus()
    }
    
    /**
     * Flow that emits true when FCM is ready to use
     */
    fun fcmReadyFlow(): Flow<Boolean> {
        return combine(
            authService.isSignedIn,
            fcmTokenRepository.observeTokenChanges()
        ) { isSignedIn, token ->
            isSignedIn && !token.isNullOrEmpty() && areNotificationsEnabled()
        }.distinctUntilChanged()
    }
    
    /**
     * Flow that emits the current user's FCM token
     */
    fun userTokenFlow(): Flow<String?> {
        return authService.currentUser
            .filterNotNull()
            .map { user ->
                fcmTokenRepository.getTokenForUser(user.uid)
            }
            .distinctUntilChanged()
    }
    
    /**
     * Subscribe to a custom topic
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return fcmTokenRepository.subscribeToTopic(topic)
    }
    
    /**
     * Unsubscribe from a custom topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return fcmTokenRepository.unsubscribeFromTopic(topic)
    }
    
    /**
     * Show a test notification (useful for testing)
     */
    fun showTestNotification() {
        notificationHelper.showNotification(
            title = "Test Notification",
            body = "FCM is working correctly!",
            channelId = NotificationHelper.CHANNEL_ID_DEFAULT
        )
    }
    
    /**
     * Get FCM setup status for debugging
     */
    suspend fun getFcmStatus(): FcmStatus {
        val userId = authService.getCurrentUserId()
        val token = getCurrentToken()
        val permissionStatus = getNotificationPermissionStatus()
        val notificationsEnabled = notificationHelper.areNotificationsEnabled()
        
        return FcmStatus(
            isUserAuthenticated = userId != null,
            hasToken = !token.isNullOrEmpty(),
            permissionStatus = permissionStatus,
            areNotificationsEnabled = notificationsEnabled,
            token = token?.take(20) + "..." // Show only first 20 chars for security
        )
    }
}

/**
 * Data class representing the current FCM setup status
 */
data class FcmStatus(
    val isUserAuthenticated: Boolean,
    val hasToken: Boolean,
    val permissionStatus: PermissionStatus,
    val areNotificationsEnabled: Boolean,
    val token: String?
) {
    val isFullySetup: Boolean
        get() = isUserAuthenticated && hasToken && permissionStatus.isGrantedOrNotRequired() && areNotificationsEnabled
}