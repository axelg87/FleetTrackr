package com.fleetmanager.ui.viewmodel

import com.fleetmanager.domain.model.Driver
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.domain.model.UserRole
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
    val message: String? = null,
    val currentUserRole: UserRole? = null,
    val canManageDrivers: Boolean = false
)

@HiltViewModel
class DriverManagementViewModel @Inject constructor(
    private val userFirestoreService: UserFirestoreService,
    private val fleetRepository: FleetRepository,
    private val saveDriverUseCase: SaveDriverUseCase,
    private val deleteDriverUseCase: DeleteDriverUseCase
) : BaseViewModel<DriverManagementUiState>() {

    override fun getInitialState(): DriverManagementUiState = DriverManagementUiState()

    init {
        observeDrivers()
        observeUserRole()
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

    private fun observeUserRole() {
        executeAsync(
            onError = { _ ->
                updateState { it.copy(currentUserRole = UserRole.DRIVER, canManageDrivers = false) }
            }
        ) {
            userFirestoreService.getCurrentUserProfile().collect { user ->
                val role = user.role
                updateState {
                    it.copy(
                        currentUserRole = role,
                        canManageDrivers = PermissionManager.canManageDrivers(role)
                    )
                }
            }
        }
    }

    fun saveDriver(driver: Driver) {
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canManageDrivers(currentRole)) {
            updateState {
                it.copy(error = "You do not have permission to manage drivers.")
            }
            return
        }
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = saveDriverUseCase(driver, currentRole)
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
        val currentRole = _uiState.value.currentUserRole
        if (currentRole == null || !PermissionManager.canManageDrivers(currentRole)) {
            updateState {
                it.copy(error = "You do not have permission to manage drivers.")
            }
            return
        }
        executeAsync(
            onLoading = { loading -> updateState { it.copy(isLoading = loading) } },
            onError = { error -> updateState { it.copy(error = error) } }
        ) {
            val result = deleteDriverUseCase(driverId, currentRole)
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
