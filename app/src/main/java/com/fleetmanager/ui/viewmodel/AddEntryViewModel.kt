package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.EarningEntry
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.usecase.GetAllEntriesRealtimeUseCase
import com.fleetmanager.domain.usecase.SaveDailyEntryUseCase
import com.fleetmanager.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import java.util.Calendar

data class EarningInputState(
    val id: String = UUID.randomUUID().toString(),
    val provider: String = "",
    val card: String = "",
    val cash: String = "",
    val tips: String = "",
    val tripCount: String = "",
    val hoursOnline: String = "",
    val providerError: String? = null,
    val amountError: String? = null
) {
    fun totalAmount(): Double {
        val cardValue = card.toDoubleOrNull() ?: 0.0
        val cashValue = cash.toDoubleOrNull() ?: 0.0
        val tipsValue = tips.toDoubleOrNull() ?: 0.0
        return cardValue + cashValue + tipsValue
    }
}

data class AddEntryUiState(
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedVehicle: Vehicle? = null,
    val driverInput: String = "",
    val vehicleInput: String = "",
    val date: Date,
    val earnings: List<EarningInputState> = listOf(EarningInputState()),
    val odometer: String = "",
    val notes: String = "",
    val photoUri: Uri? = null,
    val photoUris: List<Uri> = emptyList(),
    val existingPhotoUrls: List<String> = emptyList(),
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val notesError: String? = null,
    val odometerError: String? = null,
    val userRole: UserRole? = null,
    val currentUserProfile: UserDto? = null,
    val entryId: String? = null,
    val userId: String = "",
    val createdAt: Date? = null,
    val isEditing: Boolean = false
) {
    val canSave: Boolean
        get() = driverInput.isNotBlank() &&
                vehicleInput.isNotBlank() &&
                notesError == null &&
                odometerError == null &&
                earnings.any { it.providerError == null && it.amountError == null && it.provider.isNotBlank() && it.totalAmount() > 0.0 }

    val hasValidationErrors: Boolean
        get() = notesError != null || odometerError != null || earnings.any { it.providerError != null || it.amountError != null }

    // Driver names from Firestore only
    val allDriverNames: List<String>
        get() = drivers.map { it.name }.sorted()
    
    // Vehicle names from Firestore only
    val allVehicleNames: List<String>
        get() = vehicles.map { it.displayName }.sorted()
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val firestoreService: FirestoreService,
    private val userFirestoreService: UserFirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val saveDailyEntryUseCase: SaveDailyEntryUseCase,
    private val validator: InputValidator,
    private val getEntryByIdUseCase: com.fleetmanager.domain.usecase.GetEntryByIdUseCase,
    private val getAllEntriesRealtimeUseCase: GetAllEntriesRealtimeUseCase
) : BaseViewModel<AddEntryUiState>() {

    override fun getInitialState() = AddEntryUiState(
        date = getDefaultDate()
    )

    private var notificationPrefillHandled = false
    
    /**
     * Calculate the default date based on the 2PM rule:
     * - If current time is before 2:00 PM, use yesterday's date
     * - Otherwise, use today's date
     */
    private fun getDefaultDate(): Date {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        return if (currentHour < 14) { // Before 2:00 PM (14:00)
            // Use yesterday's date
            now.add(Calendar.DAY_OF_MONTH, -1)
            now.time
        } else {
            // Use today's date
            now.time
        }
    }
    
    init {
        loadFirestoreData()
        loadUserProfile()
    }
    
    
    private fun loadFirestoreData() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(errorMessage = "Failed to load data: $error") }
            }
        ) {
            combine(
                firestoreService.getDriversFlow(),
                vehicleFirestoreService.getVehiclesFlow()
            ) { drivers, vehicles ->
                Pair(drivers, vehicles)
            }.collect { (drivers, vehicles) ->
                updateState { currentState ->
                    val shouldAutoFill = shouldAutoFillDriver(currentState)
                    val userProfile = currentState.currentUserProfile
                    val autoSelectedDriver = if (shouldAutoFill) {
                        drivers.firstOrNull { it.userId == userProfile?.id }
                    } else {
                        currentState.selectedDriver
                    }

                    val autoFilledDriverName = if (shouldAutoFill) {
                        autoSelectedDriver?.name
                            ?: userProfile?.name
                            ?: currentState.driverInput
                    } else {
                        currentState.driverInput
                    }

                    currentState.copy(
                        drivers = drivers,
                        vehicles = vehicles,
                        selectedDriver = autoSelectedDriver,
                        driverInput = autoFilledDriverName
                    )
                }
            }
        }
    }

    private fun loadUserProfile() {
        executeAsync(
            onError = { error ->
                // Don't show error for user profile loading, just continue
            }
        ) {
            userFirestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { currentState ->
                    val updatedState = currentState.copy(
                        currentUserProfile = userProfile,
                        userRole = userProfile.role
                    )

                    if (shouldAutoFillDriver(updatedState)) {
                        val matchingDriver = updatedState.drivers.firstOrNull { it.userId == userProfile.id }
                        val driverName = matchingDriver?.name ?: userProfile.name

                        updatedState.copy(
                            driverInput = driverName,
                            selectedDriver = matchingDriver ?: updatedState.selectedDriver
                        )
                    } else {
                        updatedState
                    }
                }
            }
        }
    }

    private fun shouldAutoFillDriver(state: AddEntryUiState): Boolean {
        val role = state.userRole ?: state.currentUserProfile?.role
        if (state.isEditing || state.driverInput.isNotBlank()) {
            return false
        }
        return role == UserRole.DRIVER || role == UserRole.MANAGER
    }
    
    fun selectDriver(driver: Driver) {
        updateState { it.copy(selectedDriver = driver, driverInput = driver.name) }
    }
    
    fun selectVehicle(vehicle: Vehicle) {
        updateState { it.copy(selectedVehicle = vehicle, vehicleInput = vehicle.displayName) }
    }
    
    fun updateDriverInput(input: String) {
        updateState { 
            it.copy(
                driverInput = input,
                selectedDriver = null // We no longer maintain selectedDriver state
            ) 
        }
    }
    
    fun updateVehicleInput(input: String) {
        updateState {
            it.copy(
                vehicleInput = input,
                selectedVehicle = it.vehicles.find { vehicle -> vehicle.displayName == input }
            )
        }
    }

    fun addEarningInput() {
        updateEarnings { it + EarningInputState() }
    }

    fun removeEarningInput(id: String) {
        updateEarnings { current ->
            val updated = current.filterNot { it.id == id }
            if (updated.isEmpty()) listOf(EarningInputState()) else updated
        }
    }

    fun updateEarningProvider(id: String, value: String) {
        val sanitized = validator.sanitizeText(value)
        updateEarnings { list ->
            list.map { earning ->
                if (earning.id == id) earning.copy(provider = sanitized) else earning
            }
        }
    }

    fun updateEarningCard(id: String, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        updateEarnings { list ->
            list.map { earning ->
                if (earning.id == id) earning.copy(card = sanitized) else earning
            }
        }
    }

    fun updateEarningCash(id: String, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        updateEarnings { list ->
            list.map { earning ->
                if (earning.id == id) earning.copy(cash = sanitized) else earning
            }
        }
    }

    fun updateEarningTips(id: String, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        updateEarnings { list ->
            list.map { earning ->
                if (earning.id == id) earning.copy(tips = sanitized) else earning
            }
        }
    }

    fun updateEarningTripCount(id: String, value: String) {
        val sanitized = value.filter { it.isDigit() }
        updateEarnings { list ->
            list.map { earning ->
                if (earning.id == id) earning.copy(tripCount = sanitized) else earning
            }
        }
    }

    fun updateEarningHoursOnline(id: String, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        updateEarnings { list ->
            list.map { earning ->
                if (earning.id == id) earning.copy(hoursOnline = sanitized) else earning
            }
        }
    }

    fun updateOdometer(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateNonNegativeAmount(sanitized.toDoubleOrNull(), "Odometer", required = false).getErrorMessage()
        updateState {
            it.copy(
                odometer = sanitized,
                odometerError = error
            )
        }
    }

    private fun updateEarnings(transform: (List<EarningInputState>) -> List<EarningInputState>) {
        updateState { currentState ->
            val transformed = transform(currentState.earnings)
            currentState.copy(earnings = validateEarnings(transformed))
        }
    }

    private fun validateEarnings(inputs: List<EarningInputState>): List<EarningInputState> {
        val providerCounts = inputs
            .mapNotNull { input -> input.provider.trim().takeIf { it.isNotBlank() }?.lowercase(Locale.getDefault()) }
            .groupingBy { it }
            .eachCount()

        return inputs.map { input ->
            val provider = validator.sanitizeText(input.provider)
            val normalized = provider.trim().lowercase(Locale.getDefault())
            val requiresProvider = input.totalAmount() > 0.0
            val providerError = when {
                requiresProvider && provider.isBlank() -> "Provider is required"
                provider.isNotBlank() && (providerCounts[normalized] ?: 0) > 1 -> "Duplicate provider"
                else -> null
            }

            val amountError = sequenceOf(
                validator.validateNonNegativeAmount(input.card.toDoubleOrNull(), "Card earnings", required = false),
                validator.validateNonNegativeAmount(input.cash.toDoubleOrNull(), "Cash earnings", required = false),
                validator.validateNonNegativeAmount(input.tips.toDoubleOrNull(), "Tips", required = false),
                validator.validateNonNegativeAmount(input.hoursOnline.toDoubleOrNull(), "Hours online", required = false)
            ).firstOrNull { it.isError }?.getErrorMessage()
                ?: validateTripCount(input.tripCount)

            input.copy(
                provider = provider,
                providerError = providerError,
                amountError = amountError
            )
        }
    }

    private fun validateTripCount(value: String): String? {
        if (value.isBlank()) return null
        val parsed = value.toIntOrNull()
        return when {
            parsed == null -> "Trip count must be a whole number"
            parsed < 0 -> "Trip count cannot be negative"
            else -> null
        }
    }

    fun updateNotes(value: String) {
        val sanitized = validator.sanitizeText(value)
        val error = validator.validateNotes(sanitized).getErrorMessage()
        updateState {
            it.copy(
                notes = sanitized,
                notesError = error
            ) 
        }
    }
    
    fun updatePhotoUri(uri: Uri?) {
        updateState { it.copy(photoUri = uri) }
    }
    
    fun addPhotoUris(uris: List<Uri>) {
        updateState { currentState ->
            val currentUris = currentState.photoUris.toMutableList()
            currentUris.addAll(uris)
            currentState.copy(photoUris = currentUris)
        }
    }

    fun removePhotoUri(uri: Uri) {
        updateState { currentState ->
            val currentUris = currentState.photoUris.toMutableList()
            currentUris.remove(uri)
            currentState.copy(photoUris = currentUris)
        }
    }

    fun applyNotificationPrefill(prefillDate: String, driverId: String?) {
        if (notificationPrefillHandled) {
            return
        }

        val parsedDate = parseNotificationDate(prefillDate) ?: return
        notificationPrefillHandled = true

        updateState { currentState ->
            if (currentState.isEditing) {
                currentState
            } else {
                currentState.copy(date = parsedDate)
            }
        }

        if (driverId.isNullOrBlank()) {
            return
        }

        executeAsync { 
            val entries = getAllEntriesRealtimeUseCase().firstOrNull().orEmpty()
            val matchingEntry = entries.firstOrNull { entry ->
                entry.driverId.equals(driverId, ignoreCase = true) &&
                        isSameDay(entry.date, parsedDate)
            }

            matchingEntry?.let { entry ->
                loadEntryForEdit(entry.id)
            }
        }
    }

    private fun parseNotificationDate(dateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    private fun isSameDay(first: Date, second: Date): Boolean {
        val calendarOne = Calendar.getInstance().apply { time = first }
        val calendarTwo = Calendar.getInstance().apply { time = second }

        return calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR) &&
                calendarOne.get(Calendar.DAY_OF_YEAR) == calendarTwo.get(Calendar.DAY_OF_YEAR)
    }

    fun updateDate(date: Date) {
        updateState { it.copy(date = date) }
    }
    
    fun toggleDriverDropdown(expanded: Boolean) {
        updateState { it.copy(driverDropdownExpanded = expanded) }
    }
    
    fun toggleVehicleDropdown(expanded: Boolean) {
        updateState { it.copy(vehicleDropdownExpanded = expanded) }
    }
    
    fun toggleDatePicker(show: Boolean) {
        updateState { it.copy(showDatePicker = show) }
    }
    
    fun saveEntry() {
        val currentState = uiState.value
        if (!currentState.canSave) return

        val driverId = currentState.selectedDriver?.id
            ?: currentState.drivers.firstOrNull { it.name.equals(currentState.driverInput, ignoreCase = true) }?.id
            ?: currentState.drivers.firstOrNull { driver -> driver.userId == currentState.currentUserProfile?.id }?.id
            ?: ""
        val vehicleId = currentState.selectedVehicle?.id
            ?: currentState.vehicles.firstOrNull { it.displayName == currentState.vehicleInput }?.id
            ?: ""

        if (driverId.isBlank() || vehicleId.isBlank()) {
            updateState {
                it.copy(errorMessage = "Please select a valid driver and vehicle")
            }
            return
        }

        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSaving = isLoading, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(isSaving = false, errorMessage = error) }
            }
        ) {
            val now = Date()
            val entryIdToUse = currentState.entryId ?: UUID.randomUUID().toString()
            val createdAt = currentState.createdAt ?: now

            val entry = DailyEntry(
                id = entryIdToUse,
                userId = currentState.userId,
                date = currentState.date,
                driverId = driverId,
                driverName = currentState.driverInput,
                vehicleId = vehicleId,
                vehicle = currentState.vehicleInput,
                earnings = currentState.earnings
                    .mapNotNull { input ->
                        val provider = input.provider.trim()
                        val card = input.card.toDoubleOrNull() ?: 0.0
                        val cash = input.cash.toDoubleOrNull() ?: 0.0
                        val tips = input.tips.toDoubleOrNull() ?: 0.0
                        val total = card + cash + tips
                        if (provider.isBlank() || total <= 0.0) {
                            null
                        } else {
                            EarningEntry(
                                provider = provider,
                                cardEarnings = card,
                                cashEarnings = cash,
                                tips = tips,
                                tripCount = input.tripCount.toIntOrNull() ?: 0,
                                hoursOnline = input.hoursOnline.toDoubleOrNull() ?: 0.0
                            )
                        }
                    },
                odometer = currentState.odometer.toDoubleOrNull(),
                notes = currentState.notes,
                photoUrls = currentState.existingPhotoUrls,
                createdAt = createdAt,
                updatedAt = now
            )

            val result = saveDailyEntryUseCase(entry, currentState.photoUri, currentState.photoUris)
            result.fold(
                onSuccess = {
                    updateState { it.copy(isSaving = false, isSaved = true) }
                },
                onFailure = { error ->
                    updateState {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Failed to save entry"
                        )
                    }
                }
            )
        }
    }

    fun loadEntryForEdit(entryId: String) {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isSaving = false, errorMessage = null) }
            },
            onError = { error ->
                updateState { it.copy(errorMessage = error) }
            }
        ) {
            getEntryByIdUseCase(entryId)
                .filterNotNull()
                .firstOrNull()
                ?.let { entry ->
                    updateState { state ->
                        val earningsInputs = entry.earnings.takeIf { it.isNotEmpty() }
                            ?.map { earning ->
                                EarningInputState(
                                    provider = earning.provider,
                                    card = formatDouble(earning.cardEarnings),
                                    cash = formatDouble(earning.cashEarnings),
                                    tips = formatDouble(earning.tips),
                                    tripCount = earning.tripCount.takeIf { it > 0 }?.toString() ?: "",
                                    hoursOnline = formatDouble(earning.hoursOnline)
                                )
                            }
                            ?: listOf(EarningInputState())

                        state.copy(
                            entryId = entry.id,
                            userId = entry.userId,
                            isEditing = true,
                            date = entry.date,
                            driverInput = entry.driverName,
                            vehicleInput = entry.vehicle,
                            earnings = validateEarnings(earningsInputs),
                            odometer = entry.odometer?.let { formatDouble(it) } ?: "",
                            odometerError = null,
                            notes = entry.notes,
                            existingPhotoUrls = entry.photoUrls,
                            createdAt = entry.createdAt,
                            errorMessage = null,
                            isSaved = false
                        )
                    }
                } ?: updateState {
                it.copy(errorMessage = "Entry not found")
            }
        }
    }

    private fun formatDouble(value: Double): String {
        if (value == 0.0) return ""
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }
}