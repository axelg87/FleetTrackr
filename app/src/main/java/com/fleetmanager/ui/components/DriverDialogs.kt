package com.fleetmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fleetmanager.domain.model.Driver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverFormDialog(
    title: String,
    initialDriver: Driver? = null,
    confirmButtonLabel: String = "Save Driver",
    onDismiss: () -> Unit,
    onConfirm: (Driver) -> Unit
) {
    var driverId by remember { mutableStateOf(initialDriver?.id.orEmpty()) }
    var name by remember { mutableStateOf(initialDriver?.name.orEmpty()) }
    var salary by remember { mutableStateOf(initialDriver?.salary?.takeIf { it > 0 }?.toString().orEmpty()) }
    var annualLicenseCost by remember { mutableStateOf(initialDriver?.annualLicenseCost?.takeIf { it > 0 }?.toString().orEmpty()) }
    var annualVisaCost by remember { mutableStateOf(initialDriver?.annualVisaCost?.takeIf { it > 0 }?.toString().orEmpty()) }
    var isActive by remember { mutableStateOf(initialDriver?.isActive ?: true) }

    LaunchedEffect(initialDriver) {
        driverId = initialDriver?.id.orEmpty()
        name = initialDriver?.name.orEmpty()
        salary = initialDriver?.salary?.takeIf { it > 0 }?.toString().orEmpty()
        annualLicenseCost = initialDriver?.annualLicenseCost?.takeIf { it > 0 }?.toString().orEmpty()
        annualVisaCost = initialDriver?.annualVisaCost?.takeIf { it > 0 }?.toString().orEmpty()
        isActive = initialDriver?.isActive ?: true
    }

    val salaryValue = salary.trim().toDoubleOrNull()
    val licenseCostValue = annualLicenseCost.trim().toDoubleOrNull()
    val visaCostValue = annualVisaCost.trim().toDoubleOrNull()
    val isFormValid = driverId.isNotBlank() &&
        name.isNotBlank() &&
        salaryValue != null &&
        licenseCostValue != null &&
        visaCostValue != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Provide the driver's profile and compensation details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = driverId,
                    onValueChange = { driverId = it },
                    label = { Text("Driver ID *") },
                    placeholder = { Text("e.g., DRV-001") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g., John Smith") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salary = it },
                    label = { Text("Monthly Salary (AED) *") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = annualLicenseCost,
                    onValueChange = { annualLicenseCost = it },
                    label = { Text("Annual License Cost (AED) *") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = annualVisaCost,
                    onValueChange = { annualVisaCost = it },
                    label = { Text("Annual Visa Cost (AED) *") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Driver",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Toggle to mark driver availability",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isFormValid,
                onClick = {
                    val driver = Driver(
                        id = driverId.trim(),
                        userId = initialDriver?.userId.orEmpty(),
                        name = name.trim(),
                        isActive = isActive,
                        salary = salaryValue ?: 0.0,
                        annualLicenseCost = licenseCostValue ?: 0.0,
                        annualVisaCost = visaCostValue ?: 0.0
                    )
                    onConfirm(driver)
                }
            ) {
                Text(confirmButtonLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DriverDetailDialog(
    driver: Driver,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = driver.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DriverDetailRow(label = "Driver ID", value = driver.id)
                DriverDetailRow(label = "Status", value = if (driver.isActive) "Active" else "Inactive")
                DriverDetailRow(label = "Monthly Salary", value = "AED ${"%.2f".format(driver.salary)}")
                DriverDetailRow(label = "Annual License", value = "AED ${"%.2f".format(driver.annualLicenseCost)}")
                DriverDetailRow(label = "Annual Visa", value = "AED ${"%.2f".format(driver.annualVisaCost)}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DriverDetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
