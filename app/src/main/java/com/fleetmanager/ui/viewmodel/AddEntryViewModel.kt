package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.model.ProviderEarning
import com.fleetmanager.domain.model.ProviderType
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.usecase.GetAllEntriesRealtimeUseCase
import com.fleetmanager.domain.usecase.SaveDailyEntryUseCase
import com.fleetmanager.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collect
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

private fun Double.toInputString(): String {
    return if (this % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", this)
    } else {
        String.format(Locale.US, "%.2f", this)
    }
}

data class ProviderInputState(
    val hoursOnline: String = "",
    val cashEarnings: String = "",
    val cardEarnings: String = "",
    val tips: String = "",
    val trips: String = "",
    val hoursOnlineError: String? = null,
    val cashEarningsError: String? = null,
    val cardEarningsError: String? = null,
    val tipsError: String? = null,
    val tripsError: String? = null
) {
    fun totalAmount(): Double {
        val cash = cashEarnings.toDoubleOrNull() ?: 0.0
        val card = cardEarnings.toDoubleOrNull() ?: 0.0
        val tipsValue = tips.toDoubleOrNull() ?: 0.0
        return cash + card + tipsValue
    }

    fun hasErrors(): Boolean {
        return listOf(hoursOnlineError, cashEarningsError, cardEarningsError, tipsError, tripsError).any { it != null }
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
    val notes: String = "",
    val photoUri: Uri? = null,
    val photoUris: List<Uri> = emptyList(),
    val existingPhotoUrls: List<String> = emptyList(),
    val driverDropdownExpanded: Boolean = false,
    val vehicleDropdownExpanded: Boolean = false,
    val showDatePicker: Boolean = false,
    val odometer: String = "",
    val odometerError: String? = null,
    val uberInput: ProviderInputState = ProviderInputState(),
    val yangoInput: ProviderInputState = ProviderInputState(),
    val privateInput: ProviderInputState = ProviderInputState(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val notesError: String? = null,
    val userRole: UserRole? = null,
    val currentUserProfile: UserDto? = null,
    val entryId: String? = null,
    val userId: String = "",
    val createdAt: Date? = null,
    val isEditing: Boolean = false
) {
    private fun providerInputs(): List<ProviderInputState> = listOf(uberInput, yangoInput, privateInput)

    val hasProviderTotals: Boolean
        get() = providerInputs().any { it.totalAmount() > 0.0 }

    val canSave: Boolean
        get() = driverInput.isNotBlank() &&
            vehicleInput.isNotBlank() &&
            !hasValidationErrors &&
            hasProviderTotals

    val hasValidationErrors: Boolean
        get() = notesError != null ||
            odometerError != null ||
            providerInputs().any { it.hasErrors() }

    val allDriverNames: List<String>
        get() = drivers.map { it.name }.sorted()

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

    private fun getDefaultDate(): Date {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        return if (currentHour < 14) {
            now.add(Calendar.DAY_OF_MONTH, -1)
            now.time
        } else {
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
                updateState { it.copy(error = "Failed to load data: $error") }
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
            onError = { }
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
                selectedDriver = null
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

    fun updateOdometer(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateOptionalDecimal(sanitized, "Odometer").getErrorMessage()
        updateState {
            it.copy(
                odometer = sanitized,
                odometerError = error
            )
        }
    }

    fun updateProviderHours(providerType: ProviderType, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateOptionalHours(sanitized, "${providerType.displayName()} hours online").getErrorMessage()
        updateProviderState(providerType) { providerState ->
            providerState.copy(
                hoursOnline = sanitized,
                hoursOnlineError = error
            )
        }
    }

    fun updateProviderCash(providerType: ProviderType, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateOptionalEarnings(sanitized, "${providerType.displayName()} cash").getErrorMessage()
        updateProviderState(providerType) { providerState ->
            providerState.copy(
                cashEarnings = sanitized,
                cashEarningsError = error
            )
        }
    }

    fun updateProviderCard(providerType: ProviderType, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateOptionalEarnings(sanitized, "${providerType.displayName()} card").getErrorMessage()
        updateProviderState(providerType) { providerState ->
            providerState.copy(
                cardEarnings = sanitized,
                cardEarningsError = error
            )
        }
    }

    fun updateProviderTips(providerType: ProviderType, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateOptionalEarnings(sanitized, "${providerType.displayName()} tips").getErrorMessage()
        updateProviderState(providerType) { providerState ->
            providerState.copy(
                tips = sanitized,
                tipsError = error
            )
        }
    }

    fun updateProviderTrips(providerType: ProviderType, value: String) {
        val sanitized = validator.sanitizeIntegerInput(value)
        val error = validator.validateOptionalTrips(sanitized, "${providerType.displayName()} trips").getErrorMessage()
        updateProviderState(providerType) { providerState ->
            providerState.copy(
                trips = sanitized,
                tripsError = error
            )
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
                it.copy(error = "Please select a valid driver and vehicle")
            }
            return
        }

        executeAsync(
            onLoading = { loading ->
                updateState { it.copy(isLoading = loading, error = null) }
            },
            onError = { errorMsg ->
                updateState { it.copy(isLoading = false, error = errorMsg) }
            }
        ) {
            val now = Date()
            val entryIdToUse = currentState.entryId ?: UUID.randomUUID().toString()
            val createdAt = currentState.createdAt ?: now

            val providers = buildList {
                currentState.uberInput.toProviderEarning(ProviderType.UBER)?.let { add(it) }
                currentState.yangoInput.toProviderEarning(ProviderType.YANGO)?.let { add(it) }
                currentState.privateInput.toProviderEarning(ProviderType.PRIVATE)?.let { add(it) }
            }

            if (providers.isEmpty()) {
                updateState { it.copy(isLoading = false, error = "Please add earnings for at least one provider") }
                return@executeAsync
            }

            val entry = DailyEntry(
                id = entryIdToUse,
                userId = currentState.userId,
                date = currentState.date,
                driverId = driverId,
                driverName = currentState.driverInput,
                vehicleId = vehicleId,
                vehicle = currentState.vehicleInput,
                providers = providers,
                notes = currentState.notes,
                photoUrls = currentState.existingPhotoUrls,
                odometer = currentState.odometer.toDoubleOrNull(),
                createdAt = createdAt,
                updatedAt = now
            )

            val result = saveDailyEntryUseCase(entry, currentState.photoUri, currentState.photoUris)
            result.fold(
                onSuccess = {
                    updateState { it.copy(isLoading = false, isSaved = true) }
                },
                onFailure = { errorThrown ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = errorThrown.message ?: "Failed to save entry"
                        )
                    }
                }
            )
        }
    }

    fun loadEntryForEdit(entryId: String) {
        executeAsync(
            onLoading = { loading ->
                updateState { it.copy(isLoading = false, error = null) }
            },
            onError = { errorMsg ->
                updateState { it.copy(error = errorMsg) }
            }
        ) {
            getEntryByIdUseCase(entryId)
                .filterNotNull()
                .firstOrNull()
                ?.let { entry ->
                    updateState { state ->
                        state.copy(
                            entryId = entry.id,
                            userId = entry.userId,
                            isEditing = true,
                            date = entry.date,
                            driverInput = entry.driverName,
                            vehicleInput = entry.vehicle,
                            uberInput = entry.providerFor(ProviderType.UBER)?.toInputState() ?: ProviderInputState(),
                            yangoInput = entry.providerFor(ProviderType.YANGO)?.toInputState() ?: ProviderInputState(),
                            privateInput = entry.providerFor(ProviderType.PRIVATE)?.toInputState() ?: ProviderInputState(),
                            odometer = entry.odometer?.let { value -> value.toInputString() } ?: "",
                            notes = entry.notes,
                            existingPhotoUrls = entry.photoUrls,
                            createdAt = entry.createdAt,
                            odometerError = null,
                            notesError = null,
                            error = null,
                            isSaved = false
                        )
                    }
                } ?: updateState {
                it.copy(error = "Entry not found")
            }
        }
    }

    private fun updateProviderState(providerType: ProviderType, transform: (ProviderInputState) -> ProviderInputState) {
        updateState { currentState ->
            when (providerType) {
                ProviderType.UBER -> currentState.copy(uberInput = transform(currentState.uberInput))
                ProviderType.YANGO -> currentState.copy(yangoInput = transform(currentState.yangoInput))
                ProviderType.PRIVATE -> currentState.copy(privateInput = transform(currentState.privateInput))
                else -> currentState
            }
        }
    }

    private fun ProviderType.displayName(): String {
        return when (this) {
            ProviderType.UBER -> "Uber"
            ProviderType.YANGO -> "Yango"
            ProviderType.PRIVATE -> "Private"
            ProviderType.CAREEM -> "Careem"
            ProviderType.OTHER -> "Other"
        }
    }

    private fun ProviderInputState.toProviderEarning(type: ProviderType): ProviderEarning? {
        val cash = cashEarnings.toDoubleOrNull() ?: 0.0
        val card = cardEarnings.toDoubleOrNull() ?: 0.0
        val tipsValue = tips.toDoubleOrNull() ?: 0.0
        val total = cash + card + tipsValue
        if (total <= 0.0) {
            return null
        }

        val hours = hoursOnline.toDoubleOrNull()
        val tripsCount = trips.toIntOrNull()?.takeIf { it >= 0 }

        return ProviderEarning(
            type = type,
            amount = total,
            cardAmount = card,
            cashAmount = cash,
            tipsAmount = tipsValue,
            hoursOnline = hours,
            currency = "AED",
            tripsCount = tripsCount
        )
    }

    private fun ProviderEarning.toInputState(): ProviderInputState {
        val computedCard = if (cardAmount > 0) cardAmount else if (!hasBreakdown && amount > 0) amount else 0.0
        val computedCash = if (cashAmount > 0) cashAmount else 0.0
        val computedTips = if (tipsAmount > 0) tipsAmount else 0.0

        return ProviderInputState(
            hoursOnline = hoursOnline?.takeIf { it > 0 }?.toInputString() ?: "",
            cashEarnings = computedCash.takeIf { it > 0 }?.toInputString() ?: "",
            cardEarnings = computedCard.takeIf { it > 0 }?.toInputString() ?: "",
            tips = computedTips.takeIf { it > 0 }?.toInputString() ?: "",
            trips = tripsCount?.takeIf { it >= 0 }?.toString() ?: ""
        )
    }
}
