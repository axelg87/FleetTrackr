package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.domain.repository.FleetRepository
import com.fleetmanager.domain.usecase.DeleteDriverUseCase
import com.fleetmanager.domain.usecase.SaveDriverUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

/**
 * UI state for driver management screen.
 */
data class DriverManagementUiState(
    val drivers: List<Driver> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class DriverManagementViewModel @Inject constructor(
    private val fleetRepository: FleetRepository,
    private val saveDriverUseCase: SaveDriverUseCase,
    private val deleteDriverUseCase: DeleteDriverUseCase
) : BaseViewModel<DriverManagementUiState>() {

    override fun getInitialState(): DriverManagementUiState = DriverManagementUiState()

    init {
        observeDrivers()
    }

    private fun observeDrivers() {
        executeAsync(
            onError = { error -> updateState { it.copy(error = error, isLoading = false) } }
        ) {
            updateState { it.copy(isLoading = true) }
            fleetRepository.getAllDrivers().collect { drivers ->
                updateState {
                    it.copy(
                        drivers = drivers.sortedBy { driver -> driver.name.lowercase() },
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun saveDriver(driver: Driver) {
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = saveDriverUseCase(driver)
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Driver saved successfully", error = null) }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            error = throwable.message ?: "Failed to save driver"
                        )
                    }
                }
            )
        }
    }

    fun deleteDriver(driverId: String) {
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = deleteDriverUseCase(driverId)
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Driver deleted", error = null) }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            error = throwable.message ?: "Failed to delete driver"
                        )
                    }
                }
            )
        }
    }

    fun consumeMessage() {
        updateState { it.copy(message = null) }
    }

    fun clearError() {
        updateState { it.copy(error = null) }
    }
}
