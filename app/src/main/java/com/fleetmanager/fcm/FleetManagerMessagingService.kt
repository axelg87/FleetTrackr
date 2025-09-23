package com.fleetmanager.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.fleetmanager.auth.AuthService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FleetManagerMessagingService : FirebaseMessagingService() {
    
    @Inject
    lateinit var notificationHelper: NotificationHelper
    
    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository
    
    @Inject
    lateinit var authService: AuthService
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "FleetManagerMessagingService"
        
        // FCM payload keys
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_NOTIFICATION_TYPE = "notification_type"
        private const val KEY_VEHICLE_ID = "vehicle_id"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val KEY_EXPENSE_ID = "expense_id"
        private const val KEY_ACTION = "action"
        private const val KEY_ENTRY_DATE = "entryDate"

        // Notification types
        const val TYPE_MAINTENANCE_REMINDER = "maintenance_reminder"
        const val TYPE_EXPENSE_ALERT = "expense_alert"
        const val TYPE_VEHICLE_ALERT = "vehicle_alert"
        const val TYPE_GENERAL = "general"

        const val ACTION_MISSING_INCOME_ENTRY = "MISSING_INCOME_ENTRY"
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        Log.d(TAG, "Message notification: ${remoteMessage.notification}")
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
        
        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            handleNotificationMessage(notification, remoteMessage.data)
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        Log.d(TAG, "Handling data message: $data")
        
        val title = data[KEY_TITLE] ?: "Fleet Manager"
        val body = data[KEY_BODY] ?: "You have a new notification"
        val channelId = determineChannelId(data)

        // Show notification
        notificationHelper.showNotification(
            title = title,
            body = body,
            channelId = channelId,
            data = data
        )

        // Handle specific notification types
        when (data[KEY_NOTIFICATION_TYPE]) {
            TYPE_MAINTENANCE_REMINDER -> handleMaintenanceReminder(data)
            TYPE_EXPENSE_ALERT -> handleExpenseAlert(data)
            TYPE_VEHICLE_ALERT -> handleVehicleAlert(data)
            TYPE_GENERAL -> handleGeneralNotification(data)
        }

        when (data[KEY_ACTION]) {
            ACTION_MISSING_INCOME_ENTRY -> handleMissingIncomeEntry(data)
        }
    }
    
    private fun handleNotificationMessage(
        notification: RemoteMessage.Notification,
        data: Map<String, String>
    ) {
        Log.d(TAG, "Handling notification message")
        
        val title = notification.title ?: "Fleet Manager"
        val body = notification.body ?: "You have a new notification"
        val channelId = determineChannelId(data)
        
        // Show notification
        notificationHelper.showNotification(
            title = title,
            body = body,
            channelId = channelId,
            data = data
        )
    }
    
    private fun determineChannelId(data: Map<String, String>): String {
        // Check if channel is explicitly specified
        data[KEY_CHANNEL_ID]?.let { return it }

        // Determine channel based on notification type
        return when (data[KEY_NOTIFICATION_TYPE]) {
            TYPE_MAINTENANCE_REMINDER -> NotificationHelper.CHANNEL_ID_REMINDERS
            TYPE_EXPENSE_ALERT, TYPE_VEHICLE_ALERT -> NotificationHelper.CHANNEL_ID_HIGH_PRIORITY
            else -> when (data[KEY_ACTION]) {
                ACTION_MISSING_INCOME_ENTRY -> NotificationHelper.CHANNEL_ID_REMINDERS
                else -> NotificationHelper.CHANNEL_ID_DEFAULT
            }
        }
    }

    private fun handleMissingIncomeEntry(data: Map<String, String>) {
        Log.d(TAG, "Missing income entry notification received")
        val entryDate = data[KEY_ENTRY_DATE]
        val driverId = data[KEY_DRIVER_ID] ?: data["driverId"]
        Log.d(TAG, "Missing entry for date=$entryDate, driverId=$driverId")
    }
    
    private fun handleMaintenanceReminder(data: Map<String, String>) {
        Log.d(TAG, "Handling maintenance reminder")
        val vehicleId = data[KEY_VEHICLE_ID]
        // TODO: Add specific handling for maintenance reminders
        // For example, update local database, schedule follow-up notifications, etc.
    }
    
    private fun handleExpenseAlert(data: Map<String, String>) {
        Log.d(TAG, "Handling expense alert")
        val expenseId = data[KEY_EXPENSE_ID]
        // TODO: Add specific handling for expense alerts
        // For example, update expense status, notify relevant users, etc.
    }
    
    private fun handleVehicleAlert(data: Map<String, String>) {
        Log.d(TAG, "Handling vehicle alert")
        val vehicleId = data[KEY_VEHICLE_ID]
        // TODO: Add specific handling for vehicle alerts
        // For example, update vehicle status, log incidents, etc.
    }
    
    private fun handleGeneralNotification(data: Map<String, String>) {
        Log.d(TAG, "Handling general notification")
        // TODO: Add specific handling for general notifications
        // For example, update user preferences, log analytics, etc.
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: ${token.take(20)}...")
        
        // Save the new token to Firestore
        serviceScope.launch {
            try {
                val userId = authService.getCurrentUserId()
                if (userId != null) {
                    val success = fcmTokenRepository.saveTokenForUser(userId, token)
                    if (success) {
                        Log.d(TAG, "New FCM token saved successfully")
                    } else {
                        Log.e(TAG, "Failed to save new FCM token")
                    }
                } else {
                    Log.w(TAG, "User not authenticated, cannot save FCM token")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving new FCM token", e)
            }
        }
    }
    
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.d(TAG, "Messages were deleted on the server")
        // TODO: Handle case where messages were deleted
        // This might happen if the app was offline for too long
    }
    
    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "Message sent: $msgId")
    }
    
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "Send error for message $msgId", exception)
    }
}