package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.Car
import com.fleetmanager.domain.usecase.DeleteCarUseCase
import com.fleetmanager.domain.usecase.GetCarsUseCase
import com.fleetmanager.domain.usecase.SaveCarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collect

data class CarManagementUiState(
    val cars: List<Car> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class CarManagementViewModel @Inject constructor(
    private val getCarsUseCase: GetCarsUseCase,
    private val saveCarUseCase: SaveCarUseCase,
    private val deleteCarUseCase: DeleteCarUseCase
) : BaseViewModel<CarManagementUiState>() {

    override fun getInitialState(): CarManagementUiState = CarManagementUiState()

    init {
        observeCars()
    }

    private fun observeCars() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = error, isLoading = false) }
            }
        ) {
            updateState { it.copy(isLoading = true) }
            getCarsUseCase().collect { cars ->
                updateState {
                    it.copy(
                        cars = cars.sortedBy { car -> car.displayName },
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun saveCar(car: Car) {
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = saveCarUseCase(car)
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Car saved successfully", error = null) }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(error = throwable.message ?: "Failed to save car")
                    }
                }
            )
        }
    }

    fun deleteCar(carId: String) {
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = deleteCarUseCase(carId)
            result.fold(
                onSuccess = {
                    updateState { it.copy(message = "Car deleted", error = null) }
                },
                onFailure = { throwable ->
                    updateState {
                        it.copy(error = throwable.message ?: "Failed to delete car")
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
