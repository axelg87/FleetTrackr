package com.fleetmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.fleetmanager.auth.AuthService
import com.fleetmanager.ui.navigation.AppNavigation
import com.fleetmanager.ui.navigation.NotificationNavigationCommand
import com.fleetmanager.ui.theme.FleetManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    @Inject
    lateinit var authService: AuthService

    private val notificationNavigationState: MutableStateFlow<NotificationNavigationCommand?> =
        MutableStateFlow(null)

    private val notificationNavigationCommands: StateFlow<NotificationNavigationCommand?>
        get() = notificationNavigationState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)

        setContent {
            FleetManagerTheme {
                val navController = rememberNavController()
                val isSignedIn by authService.isSignedIn.collectAsState(initial = false)
                val notificationCommand by notificationNavigationCommands.collectAsState(initial = null)

                AppNavigation(
                    navController = navController,
                    isSignedIn = isSignedIn,
                    notificationCommand = notificationCommand,
                    onNotificationCommandConsumed = { notificationNavigationState.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
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

    private fun handleNotificationIntent(intent: Intent?) {
        val command = NotificationNavigationCommand.fromIntent(intent)
        if (command != null) {
            notificationNavigationState.value = command
        }
    }
}
