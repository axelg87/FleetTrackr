package com.fleetmanager.ui.navigation

import android.content.Intent
import com.fleetmanager.fcm.NotificationConstants

/**
 * Describes navigation intents triggered by notifications.
 */
sealed class NotificationNavigationCommand {
    data class OpenMissingIncome(val missingDateIso: String?) : NotificationNavigationCommand()
    object OpenDashboard : NotificationNavigationCommand()

    companion object {
        fun fromIntent(intent: Intent?): NotificationNavigationCommand? {
            if (intent == null) return null

            val action = intent.getStringExtra(NotificationConstants.KEY_ACTION) ?: return null
            return when (action) {
                NotificationConstants.ACTION_MISSING_INCOME -> {
                    val date = intent.getStringExtra(NotificationConstants.KEY_MISSING_DATE)
                    OpenMissingIncome(date)
                }

                else -> null
            }
        }
    }
}
