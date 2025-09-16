package com.fleetmanager.fcm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Example composable showing how to integrate FCM into your app
 * You can add this to any screen where you want to show FCM status or controls
 */
@Composable
fun FcmIntegrationExample(
    viewModel: FcmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FCM Permission Handler (handles permission requests automatically)
        FcmPermissionHandler(viewModel)
        
        // FCM Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Push Notifications",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status Information
                uiState.fcmStatus?.let { status ->
                    StatusRow("User Authenticated", status.isUserAuthenticated)
                    StatusRow("FCM Token", status.hasToken)
                    StatusRow("Permissions", status.permissionStatus.isGrantedOrNotRequired())
                    StatusRow("Notifications Enabled", status.areNotificationsEnabled)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (status.isFullySetup) "✓ Ready to receive notifications" else "⚠ Setup incomplete",
                        color = if (status.isFullySetup) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show token (first 20 chars for security)
                    status.token?.let { token ->
                        Text(
                            text = "Token: $token",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Loading indicator
                if (uiState.isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(16.dp).height(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Setting up notifications...")
                    }
                }
                
                // Error message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.refreshToken() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Refresh Token")
            }
            
            Button(
                onClick = { viewModel.showTestNotification() },
                modifier = Modifier.weight(1f),
                enabled = uiState.canShowNotifications
            ) {
                Text("Test Notification")
            }
        }
        
        // Topic Subscription Examples
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.subscribeToTopic("maintenance_reminders") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Subscribe Maintenance")
            }
            
            OutlinedButton(
                onClick = { viewModel.unsubscribeFromTopic("maintenance_reminders") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Unsubscribe")
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (isEnabled) "✓" else "✗",
            color = if (isEnabled) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}