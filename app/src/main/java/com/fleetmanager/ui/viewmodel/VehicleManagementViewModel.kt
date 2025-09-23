package com.fleetmanager.ui.viewmodel

import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.domain.model.Vehicle
import com.fleetmanager.domain.usecase.DeleteVehicleUseCase
import com.fleetmanager.domain.usecase.SaveVehicleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

data class VehicleManagementUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class VehicleManagementViewModel @Inject constructor(
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val saveVehicleUseCase: SaveVehicleUseCase,
    private val deleteVehicleUseCase: DeleteVehicleUseCase
) : BaseViewModel<VehicleManagementUiState>() {

    override fun getInitialState(): VehicleManagementUiState = VehicleManagementUiState()

    init {
        observeVehicles()
    }

    private fun observeVehicles() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = error, isLoading = false) }
            }
        ) {
            updateState { it.copy(isLoading = true) }
            vehicleFirestoreService.getVehiclesFlow().collect { vehicles ->
                updateState {
                    it.copy(
                        vehicles = vehicles.sortedBy { vehicle -> vehicle.displayName },
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun saveVehicle(vehicle: Vehicle) {
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = saveVehicleUseCase(vehicle)
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Vehicle saved successfully", error = null) }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            error = throwable.message ?: "Failed to save vehicle"
                        )
                    }
                }
            )
        }
    }

    fun deleteVehicle(vehicleId: String) {
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = deleteVehicleUseCase(vehicleId)
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Vehicle deleted", error = null) }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(
                            error = throwable.message ?: "Failed to delete vehicle"
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
