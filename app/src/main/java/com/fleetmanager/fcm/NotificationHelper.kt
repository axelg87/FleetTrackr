package com.fleetmanager.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fleetmanager.MainActivity
import com.fleetmanager.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID_DEFAULT = "fleet_manager_default"
        const val CHANNEL_ID_HIGH_PRIORITY = "fleet_manager_high_priority"
        const val CHANNEL_ID_REMINDERS = "fleet_manager_reminders"
        
        private const val NOTIFICATION_ID_DEFAULT = 1001
        private const val NOTIFICATION_ID_HIGH_PRIORITY = 1002
        private const val NOTIFICATION_ID_REMINDERS = 1003
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Default channel
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General fleet management notifications"
                enableLights(true)
                enableVibration(true)
            }
            
            // High priority channel
            val highPriorityChannel = NotificationChannel(
                CHANNEL_ID_HIGH_PRIORITY,
                "Important Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important fleet alerts and emergencies"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Reminders channel
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Vehicle maintenance and task reminders"
                enableLights(false)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannels(
                listOf(defaultChannel, highPriorityChannel, remindersChannel)
            )
        }
    }
    
    fun showNotification(
        title: String,
        body: String,
        channelId: String = CHANNEL_ID_DEFAULT,
        data: Map<String, String>? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add any extra data from FCM payload
            data?.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo) // Using your existing app logo
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(getNotificationPriority(channelId))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        
        val notificationId = getNotificationId(channelId)
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    
    private fun getNotificationPriority(channelId: String): Int {
        return when (channelId) {
            CHANNEL_ID_HIGH_PRIORITY -> NotificationCompat.PRIORITY_HIGH
            CHANNEL_ID_REMINDERS -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }
    
    private fun getNotificationId(channelId: String): Int {
        return when (channelId) {
            CHANNEL_ID_HIGH_PRIORITY -> NOTIFICATION_ID_HIGH_PRIORITY
            CHANNEL_ID_REMINDERS -> NOTIFICATION_ID_REMINDERS
            else -> NOTIFICATION_ID_DEFAULT
        }
    }
    
    fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Assume enabled on older versions
        }
    }
    
    fun isChannelEnabled(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true // Channels don't exist on older versions
        }
    }
}