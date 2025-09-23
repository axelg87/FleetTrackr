package com.fleetmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fleetmanager.domain.model.Vehicle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormDialog(
    title: String,
    initialVehicle: Vehicle? = null,
    confirmButtonLabel: String = "Save Vehicle",
    onDismiss: () -> Unit,
    onConfirm: (Vehicle) -> Unit
) {
    var vehicleId by remember { mutableStateOf(initialVehicle?.id.orEmpty()) }
    var make by remember { mutableStateOf(initialVehicle?.make.orEmpty()) }
    var model by remember { mutableStateOf(initialVehicle?.model.orEmpty()) }
    var year by remember { mutableStateOf(initialVehicle?.year?.takeIf { it > 0 }?.toString().orEmpty()) }
    var licensePlate by remember { mutableStateOf(initialVehicle?.licensePlate.orEmpty()) }
    var price by remember { mutableStateOf(initialVehicle?.price?.takeIf { it > 0 }?.toString().orEmpty()) }
    var deposit by remember { mutableStateOf(initialVehicle?.deposit?.toString().orEmpty()) }
    var installment by remember { mutableStateOf(initialVehicle?.installment?.toString().orEmpty()) }
    var installmentDuration by remember { mutableStateOf(initialVehicle?.installmentDurationMonths?.toString().orEmpty()) }
    var serviceStartDate by remember { mutableStateOf(initialVehicle?.serviceStartDate?.toLocalDateString().orEmpty()) }
    var serviceEndDate by remember { mutableStateOf(initialVehicle?.serviceEndDate?.toLocalDateString().orEmpty()) }
    var annualInsuranceAmount by remember { mutableStateOf(initialVehicle?.annualInsuranceAmount?.takeIf { it > 0 }?.toString().orEmpty()) }
    var fuelTankCapacity by remember { mutableStateOf(initialVehicle?.fuelTankCapacity?.toString().orEmpty()) }
    var fuelConsumption by remember { mutableStateOf(initialVehicle?.fuelConsumptionPer100Km?.toString().orEmpty()) }
    var isActive by remember { mutableStateOf(initialVehicle?.isActive ?: true) }

    LaunchedEffect(initialVehicle) {
        vehicleId = initialVehicle?.id.orEmpty()
        make = initialVehicle?.make.orEmpty()
        model = initialVehicle?.model.orEmpty()
        year = initialVehicle?.year?.takeIf { it > 0 }?.toString().orEmpty()
        licensePlate = initialVehicle?.licensePlate.orEmpty()
        price = initialVehicle?.price?.takeIf { it > 0 }?.toString().orEmpty()
        deposit = initialVehicle?.deposit?.toString().orEmpty()
        installment = initialVehicle?.installment?.toString().orEmpty()
        installmentDuration = initialVehicle?.installmentDurationMonths?.toString().orEmpty()
        serviceStartDate = initialVehicle?.serviceStartDate?.toLocalDateString().orEmpty()
        serviceEndDate = initialVehicle?.serviceEndDate?.toLocalDateString().orEmpty()
        annualInsuranceAmount = initialVehicle?.annualInsuranceAmount?.takeIf { it > 0 }?.toString().orEmpty()
        fuelTankCapacity = initialVehicle?.fuelTankCapacity?.toString().orEmpty()
        fuelConsumption = initialVehicle?.fuelConsumptionPer100Km?.toString().orEmpty()
        isActive = initialVehicle?.isActive ?: true
    }

    val yearValue = year.trim().toIntOrNull()
    val priceValue = price.trim().toDoubleOrNull()
    val depositValue = deposit.trim().toDoubleOrNull()
    val installmentValue = installment.trim().toDoubleOrNull()
    val installmentDurationValue = installmentDuration.trim().toIntOrNull()
    val annualInsuranceValue = annualInsuranceAmount.trim().toDoubleOrNull()
    val fuelTankValue = fuelTankCapacity.trim().toDoubleOrNull()
    val fuelConsumptionValue = fuelConsumption.trim().toDoubleOrNull()

    val serviceStartDateValue = serviceStartDate.parseAsDate()
    val serviceEndDateValue = serviceEndDate.parseAsDate()
    val areDatesValid = (serviceStartDate.isBlank() || serviceStartDateValue != null) &&
        (serviceEndDate.isBlank() || serviceEndDateValue != null)

    val isFormValid = vehicleId.isNotBlank() &&
        make.isNotBlank() &&
        model.isNotBlank() &&
        yearValue != null &&
        licensePlate.isNotBlank() &&
        priceValue != null &&
        annualInsuranceValue != null &&
        areDatesValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = vehicleId,
                    onValueChange = { vehicleId = it },
                    label = { Text("Vehicle ID *") },
                    placeholder = { Text("e.g., VEH-1001") },
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
                    label = { Text("Year *") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = licensePlate,
                    onValueChange = { licensePlate = it },
                    label = { Text("License Plate *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Purchase Price (AED) *") },
                    placeholder = { Text("e.g., 85000") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = deposit,
                    onValueChange = { deposit = it },
                    label = { Text("Deposit (AED)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = installment,
                    onValueChange = { installment = it },
                    label = { Text("Monthly Installment (AED)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = installmentDuration,
                    onValueChange = { installmentDuration = it },
                    label = { Text("Installment Duration (months)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serviceStartDate,
                    onValueChange = { serviceStartDate = it },
                    label = { Text("Service Start Date (YYYY-MM-DD)") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (serviceStartDate.isNotBlank() && serviceStartDateValue == null) {
                            Text("Invalid date format")
                        }
                    }
                )
                OutlinedTextField(
                    value = serviceEndDate,
                    onValueChange = { serviceEndDate = it },
                    label = { Text("Service End Date (YYYY-MM-DD)") },
                    placeholder = { Text("Optional") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (serviceEndDate.isNotBlank() && serviceEndDateValue == null) {
                            Text("Invalid date format")
                        }
                    }
                )
                OutlinedTextField(
                    value = annualInsuranceAmount,
                    onValueChange = { annualInsuranceAmount = it },
                    label = { Text("Annual Insurance Amount (AED) *") },
                    placeholder = { Text("e.g., 4500") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fuelTankCapacity,
                    onValueChange = { fuelTankCapacity = it },
                    label = { Text("Fuel Tank Capacity (L)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fuelConsumption,
                    onValueChange = { fuelConsumption = it },
                    label = { Text("Fuel Consumption (L/100km)") },
                    placeholder = { Text("Optional") },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Active Vehicle")
                        Text(
                            text = "Inactive vehicles stay archived for history.",
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
                        Vehicle(
                            id = vehicleId.trim(),
                            make = make.trim(),
                            model = model.trim(),
                            year = yearValue ?: 0,
                            licensePlate = licensePlate.trim(),
                            isActive = isActive,
                            price = priceValue ?: 0.0,
                            deposit = depositValue,
                            installment = installmentValue,
                            installmentDurationMonths = installmentDurationValue,
                            serviceStartDate = serviceStartDateValue,
                            serviceEndDate = serviceEndDateValue,
                            annualInsuranceAmount = annualInsuranceValue ?: 0.0,
                            fuelTankCapacity = fuelTankValue,
                            fuelConsumptionPer100Km = fuelConsumptionValue,
                            userId = initialVehicle?.userId.orEmpty()
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
fun VehicleDetailDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = vehicle.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                VehicleDetailRow(label = "Vehicle ID", value = vehicle.id)
                VehicleDetailRow(label = "Make", value = vehicle.make)
                VehicleDetailRow(label = "Model", value = vehicle.model)
                VehicleDetailRow(label = "Year", value = vehicle.year.toString())
                VehicleDetailRow(label = "License Plate", value = vehicle.licensePlate)
                VehicleDetailRow(label = "Price", value = "AED ${"%.2f".format(vehicle.price)}")
                VehicleDetailRow(label = "Deposit", value = vehicle.deposit?.let { "AED ${"%.2f".format(it)}" } ?: "—")
                VehicleDetailRow(label = "Installment", value = vehicle.installment?.let { "AED ${"%.2f".format(it)}" } ?: "—")
                VehicleDetailRow(label = "Installment Duration", value = vehicle.installmentDurationMonths?.let { "$it months" } ?: "—")
                VehicleDetailRow(label = "Service Start", value = vehicle.serviceStartDate?.toLocalDateString() ?: "—")
                VehicleDetailRow(label = "Service End", value = vehicle.serviceEndDate?.toLocalDateString() ?: "—")
                VehicleDetailRow(label = "Insurance", value = "AED ${"%.2f".format(vehicle.annualInsuranceAmount)}")
                VehicleDetailRow(label = "Fuel Tank", value = vehicle.fuelTankCapacity?.let { "$it L" } ?: "—")
                VehicleDetailRow(label = "Fuel Consumption", value = vehicle.fuelConsumptionPer100Km?.let { "$it L/100km" } ?: "—")
                VehicleDetailRow(label = "Status", value = if (vehicle.isActive) "Active" else "Inactive")
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
private fun VehicleDetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun String.parseAsDate(): java.util.Date? {
    if (isBlank()) return null
    return try {
        val localDate = java.time.LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        java.util.Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    } catch (e: DateTimeParseException) {
        null
    }
}

private fun java.util.Date.toLocalDateString(): String {
    return toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
}
