package com.fleetmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.fleetmanager.auth.AuthService
import com.fleetmanager.ui.navigation.AppNavigation
import com.fleetmanager.ui.navigation.Screen
import com.fleetmanager.ui.theme.FleetManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    @Inject
    lateinit var authService: AuthService

    private val notificationActionState = MutableStateFlow<NotificationAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        processNotificationIntent(intent)

        setContent {
            FleetManagerTheme {
                val navController = rememberNavController()
                val isSignedIn by authService.isSignedIn.collectAsState(initial = false)
                val notificationAction by notificationActionState.collectAsState()

                AppNavigation(
                    navController = navController,
                    isSignedIn = isSignedIn
                )

                LaunchedEffect(isSignedIn, notificationAction) {
                    val action = notificationAction ?: return@LaunchedEffect
                    if (!isSignedIn) {
                        return@LaunchedEffect
                    }

                    if (action.action == ACTION_MISSING_INCOME_ENTRY) {
                        if (navController.currentBackStackEntry == null) {
                            snapshotFlow { navController.currentBackStackEntry }
                                .filterNotNull()
                                .first()
                        }

                        navController.navigate(
                            Screen.AddEntry.createRoute(
                                prefillDate = action.entryDate,
                                prefillDriverId = action.driverId
                            )
                        ) {
                            launchSingleTop = true
                        }

                        notificationActionState.value = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
        }
        processNotificationIntent(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!isGranted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun processNotificationIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        val action = intent.getStringExtra(EXTRA_NOTIFICATION_ACTION)
        val entryDate = intent.getStringExtra(EXTRA_NOTIFICATION_ENTRY_DATE)
        val driverId = intent.getStringExtra(EXTRA_NOTIFICATION_DRIVER_ID)
            ?: intent.getStringExtra("driver_id")

        if (!action.isNullOrBlank() && !entryDate.isNullOrBlank()) {
            notificationActionState.value = NotificationAction(
                action = action,
                driverId = driverId,
                entryDate = entryDate
            )
        }

        clearNotificationExtras(intent)
    }

    private fun clearNotificationExtras(intent: Intent) {
        intent.removeExtra(EXTRA_NOTIFICATION_ACTION)
        intent.removeExtra(EXTRA_NOTIFICATION_ENTRY_DATE)
        intent.removeExtra(EXTRA_NOTIFICATION_DRIVER_ID)
    }

    private data class NotificationAction(
        val action: String,
        val driverId: String?,
        val entryDate: String
    )

    companion object {
        private const val EXTRA_NOTIFICATION_ACTION = "action"
        private const val EXTRA_NOTIFICATION_DRIVER_ID = "driverId"
        private const val EXTRA_NOTIFICATION_ENTRY_DATE = "entryDate"
        private const val ACTION_MISSING_INCOME_ENTRY = "MISSING_INCOME_ENTRY"
    }
}
