package com.fleetmanager.ui.viewmodel

import android.net.Uri
import com.fleetmanager.domain.model.DailyEntry
import com.fleetmanager.domain.model.Driver
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
import java.math.BigDecimal
import java.util.Calendar

enum class IncomeProvider {
    UBER,
    YANGO,
    PRIVATE
}

enum class ProviderDetailField {
    HOURS_ONLINE,
    CASH,
    CARD,
    TIPS
}

data class ProviderBreakdownUiState(
    val hoursOnline: String = "",
    val cashEarnings: String = "",
    val cardEarnings: String = "",
    val tips: String = "",
    val hoursError: String? = null,
    val cashError: String? = null,
    val cardError: String? = null,
    val tipsError: String? = null
) {
    val hasErrors: Boolean
        get() = listOfNotNull(hoursError, cashError, cardError, tipsError).isNotEmpty()
}

data class AddEntryUiState(
    val drivers: List<Driver> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedVehicle: Vehicle? = null,
    val driverInput: String = "",
    val vehicleInput: String = "",
    val odometer: String = "",
    val odometerError: String? = null,
    val date: Date,
    val uberEarnings: String = "",
    val yangoEarnings: String = "",
    val privateJobsEarnings: String = "",
    val uberBreakdown: ProviderBreakdownUiState = ProviderBreakdownUiState(),
    val yangoBreakdown: ProviderBreakdownUiState = ProviderBreakdownUiState(),
    val privateJobsBreakdown: ProviderBreakdownUiState = ProviderBreakdownUiState(),
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
    val uberEarningsError: String? = null,
    val yangoEarningsError: String? = null,
    val privateJobsEarningsError: String? = null,
    val notesError: String? = null,
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
                odometerError == null &&
                uberEarningsError == null &&
                yangoEarningsError == null &&
                privateJobsEarningsError == null &&
                notesError == null &&
                listOf(uberBreakdown, yangoBreakdown, privateJobsBreakdown).all { !it.hasErrors } &&
                (uberEarnings.isNotBlank() || yangoEarnings.isNotBlank() || privateJobsEarnings.isNotBlank())

    val hasValidationErrors: Boolean
        get() = uberEarningsError != null ||
                yangoEarningsError != null ||
                privateJobsEarningsError != null ||
                notesError != null ||
                odometerError != null ||
                listOf(uberBreakdown, yangoBreakdown, privateJobsBreakdown).any { it.hasErrors }
    
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

    fun updateOdometer(value: String) {
        val sanitized = value.filter { it.isDigit() }
        val error = validator.validateOptionalNonNegativeInt(value, "Odometer").getErrorMessage()
        updateState {
            it.copy(
                odometer = sanitized,
                odometerError = error
            )
        }
    }

    fun updateUberEarnings(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Uber earnings").getErrorMessage()
        updateState {
            it.copy(
                uberEarnings = sanitized,
                uberEarningsError = error
            ) 
        }
    }
    
    fun updateYangoEarnings(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Yango earnings").getErrorMessage()
        updateState { 
            it.copy(
                yangoEarnings = sanitized,
                yangoEarningsError = error
            ) 
        }
    }
    
    fun updatePrivateJobsEarnings(value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateEarnings(sanitized, "Private jobs earnings").getErrorMessage()
        updateState { 
            it.copy(
                privateJobsEarnings = sanitized,
                privateJobsEarningsError = error
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

    fun updateProviderDetail(provider: IncomeProvider, field: ProviderDetailField, value: String) {
        val sanitized = validator.sanitizeNumericInput(value)
        val error = validator.validateOptionalAmount(value, providerFieldName(provider, field)).getErrorMessage()
        updateProviderBreakdown(provider) { breakdown ->
            when (field) {
                ProviderDetailField.HOURS_ONLINE -> breakdown.copy(hoursOnline = sanitized, hoursError = error)
                ProviderDetailField.CASH -> breakdown.copy(cashEarnings = sanitized, cashError = error)
                ProviderDetailField.CARD -> breakdown.copy(cardEarnings = sanitized, cardError = error)
                ProviderDetailField.TIPS -> breakdown.copy(tips = sanitized, tipsError = error)
            }
        }
    }

    private fun updateProviderBreakdown(
        provider: IncomeProvider,
        transform: (ProviderBreakdownUiState) -> ProviderBreakdownUiState
    ) {
        updateState { state ->
            when (provider) {
                IncomeProvider.UBER -> state.copy(uberBreakdown = transform(state.uberBreakdown))
                IncomeProvider.YANGO -> state.copy(yangoBreakdown = transform(state.yangoBreakdown))
                IncomeProvider.PRIVATE -> state.copy(privateJobsBreakdown = transform(state.privateJobsBreakdown))
            }
        }
    }

    private fun providerFieldName(provider: IncomeProvider, field: ProviderDetailField): String {
        val providerName = when (provider) {
            IncomeProvider.UBER -> "Uber"
            IncomeProvider.YANGO -> "Yango"
            IncomeProvider.PRIVATE -> "Private jobs"
        }

        val suffix = when (field) {
            ProviderDetailField.HOURS_ONLINE -> "hours online"
            ProviderDetailField.CASH -> "cash earnings"
            ProviderDetailField.CARD -> "card earnings"
            ProviderDetailField.TIPS -> "tips"
        }

        return "$providerName $suffix"
    }

    private fun buildProviderBreakdown(
        hoursOnline: Double,
        cash: Double,
        card: Double,
        tips: Double
    ): ProviderBreakdownUiState {
        return ProviderBreakdownUiState(
            hoursOnline = formatDoubleInput(hoursOnline),
            cashEarnings = formatDoubleInput(cash),
            cardEarnings = formatDoubleInput(card),
            tips = formatDoubleInput(tips)
        )
    }

    private fun formatDoubleInput(value: Double): String {
        if (value == 0.0) {
            return ""
        }

        return BigDecimal(value).stripTrailingZeros().toPlainString()
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
                odometer = currentState.odometer.toIntOrNull(),
                uberEarnings = currentState.uberEarnings.toDoubleOrNull() ?: 0.0,
                uberHoursOnline = currentState.uberBreakdown.hoursOnline.toDoubleOrNull() ?: 0.0,
                uberCashEarnings = currentState.uberBreakdown.cashEarnings.toDoubleOrNull() ?: 0.0,
                uberCardEarnings = currentState.uberBreakdown.cardEarnings.toDoubleOrNull() ?: 0.0,
                uberTips = currentState.uberBreakdown.tips.toDoubleOrNull() ?: 0.0,
                yangoEarnings = currentState.yangoEarnings.toDoubleOrNull() ?: 0.0,
                yangoHoursOnline = currentState.yangoBreakdown.hoursOnline.toDoubleOrNull() ?: 0.0,
                yangoCashEarnings = currentState.yangoBreakdown.cashEarnings.toDoubleOrNull() ?: 0.0,
                yangoCardEarnings = currentState.yangoBreakdown.cardEarnings.toDoubleOrNull() ?: 0.0,
                yangoTips = currentState.yangoBreakdown.tips.toDoubleOrNull() ?: 0.0,
                privateJobsEarnings = currentState.privateJobsEarnings.toDoubleOrNull() ?: 0.0,
                privateJobsHoursOnline = currentState.privateJobsBreakdown.hoursOnline.toDoubleOrNull() ?: 0.0,
                privateJobsCashEarnings = currentState.privateJobsBreakdown.cashEarnings.toDoubleOrNull() ?: 0.0,
                privateJobsCardEarnings = currentState.privateJobsBreakdown.cardEarnings.toDoubleOrNull() ?: 0.0,
                privateJobsTips = currentState.privateJobsBreakdown.tips.toDoubleOrNull() ?: 0.0,
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
                        state.copy(
                            entryId = entry.id,
                            userId = entry.userId,
                            isEditing = true,
                            date = entry.date,
                            driverInput = entry.driverName,
                            vehicleInput = entry.vehicle,
                            odometer = entry.odometer?.takeIf { it > 0 }?.toString() ?: "",
                            odometerError = null,
                            uberEarnings = entry.uberEarnings.takeIf { it != 0.0 }?.toString() ?: "",
                            yangoEarnings = entry.yangoEarnings.takeIf { it != 0.0 }?.toString() ?: "",
                            privateJobsEarnings = entry.privateJobsEarnings.takeIf { it != 0.0 }?.toString() ?: "",
                            uberBreakdown = buildProviderBreakdown(
                                entry.uberHoursOnline,
                                entry.uberCashEarnings,
                                entry.uberCardEarnings,
                                entry.uberTips
                            ),
                            yangoBreakdown = buildProviderBreakdown(
                                entry.yangoHoursOnline,
                                entry.yangoCashEarnings,
                                entry.yangoCardEarnings,
                                entry.yangoTips
                            ),
                            privateJobsBreakdown = buildProviderBreakdown(
                                entry.privateJobsHoursOnline,
                                entry.privateJobsCashEarnings,
                                entry.privateJobsCardEarnings,
                                entry.privateJobsTips
                            ),
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
}