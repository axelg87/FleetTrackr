package com.fleetmanager.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager

/**
 * Reusable driver input component that handles role-based restrictions.
 * When the user is a driver, it automatically fills and disables the driver field.
 * For other roles, it provides a dropdown for driver selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverInputComponent(
    driverInput: String,
    allDriverNames: List<String>,
    isDropdownExpanded: Boolean,
    onDriverInputChange: (String) -> Unit,
    onToggleDropdown: (Boolean) -> Unit,
    userRole: UserRole?,
    currentUserName: String?,
    modifier: Modifier = Modifier,
    label: String = "Driver"
) {
    // Check if current user is a driver and should have restricted input
    val isDriverRole = userRole == UserRole.DRIVER
    val shouldAutoFill = isDriverRole && currentUserName != null
    
    // Auto-fill driver name for driver role users
    LaunchedEffect(shouldAutoFill, currentUserName) {
        if (shouldAutoFill && currentUserName != null && driverInput.isBlank()) {
            onDriverInputChange(currentUserName)
        }
    }
    
    if (isDriverRole) {
        // For drivers, show a disabled field with their name
        OutlinedTextField(
            value = currentUserName ?: driverInput,
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            enabled = false,
            modifier = modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            supportingText = {
                Text(
                    text = "Auto-filled based on your profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        )
    } else {
        // For non-drivers, show the regular dropdown
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = onToggleDropdown,
            modifier = modifier
        ) {
            OutlinedTextField(
                value = driverInput,
                onValueChange = onDriverInputChange,
                readOnly = false,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { onToggleDropdown(false) }
            ) {
                if (allDriverNames.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No drivers available") },
                        onClick = { }
                    )
                } else {
                    allDriverNames.forEach { driverName ->
                        DropdownMenuItem(
                            text = { Text(driverName) },
                            onClick = {
                                onDriverInputChange(driverName)
                                onToggleDropdown(false)
                            }
                        )
                    }
                }
            }
        }
    }
}