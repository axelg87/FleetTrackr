package com.fleetmanager.fcm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPermissionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "NotificationPermissionHandler"
        const val POST_NOTIFICATIONS_PERMISSION = Manifest.permission.POST_NOTIFICATIONS
    }
    
    /**
     * Check if notification permission is required (Android 13+)
     */
    fun isNotificationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }
    
    /**
     * Check if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (isNotificationPermissionRequired()) {
            ContextCompat.checkSelfPermission(
                context,
                POST_NOTIFICATIONS_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Get the permission status with detailed information
     */
    fun getPermissionStatus(): PermissionStatus {
        return when {
            !isNotificationPermissionRequired() -> PermissionStatus.NotRequired
            isNotificationPermissionGranted() -> PermissionStatus.Granted
            else -> PermissionStatus.Denied
        }
    }
    
    /**
     * Log the current permission status
     */
    fun logPermissionStatus() {
        val status = getPermissionStatus()
        Log.d(TAG, "Notification permission status: AEDstatus")
        Log.d(TAG, "Android version: AED{Build.VERSION.SDK_INT}")
        Log.d(TAG, "Permission required: AED{isNotificationPermissionRequired()}")
        Log.d(TAG, "Permission granted: AED{isNotificationPermissionGranted()}")
    }
}

/**
 * Represents the different states of notification permission
 */
sealed class PermissionStatus {
    object NotRequired : PermissionStatus()
    object Granted : PermissionStatus()
    object Denied : PermissionStatus()
    
    fun isGrantedOrNotRequired(): Boolean {
        return this is Granted || this is NotRequired
    }
    
    override fun toString(): String {
        return when (this) {
            is NotRequired -> "Not Required (Android < 13)"
            is Granted -> "Granted"
            is Denied -> "Denied"
        }
    }
}