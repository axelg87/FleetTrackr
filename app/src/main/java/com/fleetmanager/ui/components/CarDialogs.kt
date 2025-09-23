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
import com.fleetmanager.domain.model.Car

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarFormDialog(
    title: String,
    initialCar: Car? = null,
    confirmButtonLabel: String = "Save Car",
    onDismiss: () -> Unit,
    onConfirm: (Car) -> Unit
) {
    var id by remember { mutableStateOf(initialCar?.id.orEmpty()) }
    var nickname by remember { mutableStateOf(initialCar?.nickname.orEmpty()) }
    var make by remember { mutableStateOf(initialCar?.make.orEmpty()) }
    var model by remember { mutableStateOf(initialCar?.model.orEmpty()) }
    var year by remember { mutableStateOf(initialCar?.year?.toString().orEmpty()) }
    var licensePlate by remember { mutableStateOf(initialCar?.licensePlate.orEmpty()) }
    var color by remember { mutableStateOf(initialCar?.color.orEmpty()) }
    var isActive by remember { mutableStateOf(initialCar?.isActive ?: true) }

    LaunchedEffect(initialCar) {
        id = initialCar?.id.orEmpty()
        nickname = initialCar?.nickname.orEmpty()
        make = initialCar?.make.orEmpty()
        model = initialCar?.model.orEmpty()
        year = initialCar?.year?.toString().orEmpty()
        licensePlate = initialCar?.licensePlate.orEmpty()
        color = initialCar?.color.orEmpty()
        isActive = initialCar?.isActive ?: true
    }

    val yearValue = year.trim().toIntOrNull()
    val isFormValid = id.isNotBlank() && make.isNotBlank() && model.isNotBlank() && licensePlate.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = id,
                    onValueChange = { id = it },
                    label = { Text("Car ID *") },
                    placeholder = { Text("e.g., CAR-01") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = { Text("Make *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = { Text("Year") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (year.isNotBlank() && yearValue == null) {
                            Text("Invalid year")
                        }
                    }
                )
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("License Plate *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Active Car")
                        Text(
                            text = "Inactive cars remain available for history only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        Car(
                            id = id.trim(),
                            nickname = nickname.trim(),
                            make = make.trim(),
                            model = model.trim(),
                            year = yearValue,
                            licensePlate = licensePlate.trim(),
                            color = color.trim(),
                            isActive = isActive,
                            userId = initialCar?.userId.orEmpty()
                        )
                    )
                },
                enabled = isFormValid
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
fun CarDetailDialog(
    car: Car,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = car.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow(label = "Car ID", value = car.id)
                DetailRow(label = "Nickname", value = car.nickname.ifBlank { "—" })
                DetailRow(label = "Make", value = car.make)
                DetailRow(label = "Model", value = car.model)
                DetailRow(label = "Year", value = car.year?.toString() ?: "—")
                DetailRow(label = "License Plate", value = car.licensePlate)
                DetailRow(label = "Color", value = car.color.ifBlank { "—" })
                DetailRow(label = "Status", value = if (car.isActive) "Active" else "Inactive")
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
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
