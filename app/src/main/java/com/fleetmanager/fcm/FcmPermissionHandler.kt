package com.fleetmanager.fcm

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Composable that handles FCM permission requests and initialization
 * This should be included in your main app composable or in screens where FCM is needed
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FcmPermissionHandler(
    viewModel: FcmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Only request permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        ) { granted ->
            viewModel.onPermissionResult(granted)
        }
        
        // Handle permission state changes
        LaunchedEffect(notificationPermissionState.status) {
            if (notificationPermissionState.status.isGranted) {
                viewModel.onPermissionResult(true)
            }
        }
        
        // Show permission request dialog if needed
        if (uiState.showPermissionRequest && !notificationPermissionState.status.isGranted) {
            NotificationPermissionDialog(
                permissionState = notificationPermissionState,
                onDismiss = { /* Handle dismiss if needed */ }
            )
        }
    } else {
        // On Android 12 and below, permissions are not required
        LaunchedEffect(Unit) {
            viewModel.onPermissionResult(true)
        }
    }
}

/**
 * Dialog that explains why notification permission is needed
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NotificationPermissionDialog(
    permissionState: PermissionState,
    onDismiss: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismiss()
            },
            title = {
                Text("Enable Notifications")
            },
            text = {
                Text(
                    if (permissionState.status.shouldShowRationale) {
                        "Push notifications help you stay updated with important fleet information, " +
                        "maintenance reminders, and alerts. Please grant notification permission to " +
                        "receive these updates."
                    } else {
                        "To receive important updates about your fleet, maintenance reminders, and " +
                        "alerts, please allow notifications for this app."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        permissionState.launchPermissionRequest()
                        showDialog = false
                    }
                ) {
                    Text("Allow")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Not Now")
                }
            }
        )
    }
}

/**
 * Simple composable to show FCM status (useful for debugging)
 */
@Composable
fun FcmStatusIndicator(
    viewModel: FcmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // This is mainly for debugging - you can remove or customize as needed
    if (uiState.fcmStatus != null) {
        val status = uiState.fcmStatus!!
        Text(
            text = "FCM: ${if (status.isFullySetup) "✓ Ready" else "⚠ Setup needed"}",
            color = if (status.isFullySetup) 
                androidx.compose.material3.MaterialTheme.colorScheme.primary 
            else 
                androidx.compose.material3.MaterialTheme.colorScheme.error
        )
    }
}