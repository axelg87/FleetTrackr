package com.fleetmanager.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.domain.usecase.GetDashboardDataRealtimeUseCase
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.ui.components.StatItem
import com.fleetmanager.ui.navigation.DashboardShortcut
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


data class DashboardUiState(
    val quickStats: List<StatItem> = emptyList(),
    val earningsStats: List<StatItem> = emptyList(), // New tiles for specific earnings
    val recentEntries: List<com.fleetmanager.domain.model.DailyEntry> = emptyList(),
    val userProfile: UserDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataRealtimeUseCase: GetDashboardDataRealtimeUseCase,
    private val syncManager: SyncManager,
    private val userFirestoreService: UserFirestoreService,
    private val authRepository: com.fleetmanager.domain.repository.AuthRepository
) : BaseViewModel<DashboardUiState>() {

    override fun getInitialState() = DashboardUiState()

    // Navigation is now handled by NavigationViewModel - no need for callbacks here

    // Expose user role for role-based UI decisions
    val userRole: StateFlow<UserRole> = uiState
        .map { it.userProfile?.role ?: UserRole.DRIVER }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserRole.DRIVER
        )

    init {
        observeAuthStateChanges()
        loadDashboardData()
        loadUserProfile()
    }
    
    private fun observeAuthStateChanges() {
        executeAsync {
            authRepository.isSignedIn.collect { isSignedIn ->
                if (!isSignedIn) {
                    // User signed out, reset the ViewModel
                    resetToInitialState()
                } else {
                    // User signed in, reload data
                    loadDashboardData()
                    loadUserProfile()
                }
            }
        }
    }
    
    // Navigation callback is no longer needed - handled by NavigationViewModel

    fun syncNow() {
        executeAsync(
            onError = { error ->
                updateState { it.copy(error = "Sync failed: $error") }
            }
        ) {
            syncManager.syncNow()
        }
    }

    private fun loadUserProfile() {
        executeAsync(
            onError = { error ->
                // Don't show error for user profile loading, just use default
            }
        ) {
            userFirestoreService.getCurrentUserProfile().collect { userProfile ->
                updateState { it.copy(userProfile = userProfile) }
            }
        }
    }

    private fun loadDashboardData() {
        executeAsync(
            onLoading = { isLoading ->
                updateState { it.copy(isLoading = isLoading) }
            },
            onError = { error ->
                updateState { it.copy(error = error, isLoading = false) }
            }
        ) {
            getDashboardDataRealtimeUseCase().collect { dashboardData ->
                val currentUserRole = uiState.value.userProfile?.role ?: UserRole.DRIVER
                val isAdmin = PermissionManager.canSeeAdminControls(currentUserRole)
                
                val quickStats = buildList {
                    add(StatItem(
                        icon = Icons.Default.CalendarToday,
                        value = "AED ${String.format("%.0f", dashboardData.thisMonthEarnings)}",
                        label = "This Month",
                        shortcut = DashboardShortcut.TimeRange.ThisMonth
                    ))
                    add(StatItem(
                        icon = Icons.Default.TrendingUp,
                        value = "AED ${String.format("%.0f", dashboardData.thisWeekEarnings)}",
                        label = "This Week",
                        shortcut = DashboardShortcut.TimeRange.ThisWeek
                    ))
                    add(StatItem(
                        icon = Icons.Default.Schedule,
                        value = "AED ${String.format("%.0f", dashboardData.last24hEarnings)}",
                        label = "Last 24h",
                        shortcut = DashboardShortcut.TimeRange.Last24Hours
                    ))
                    // Only show Active Drivers tile for admin users
                    if (isAdmin) {
                        add(StatItem(
                            icon = Icons.Default.People,
                            value = dashboardData.activeDriversCount.toString(),
                            label = "Active Drivers",
                            shortcut = DashboardShortcut.AllEntries
                        ))
                    }
                }
                
                // Always show earnings stats so the layout stays consistent across roles
                val earningsStats = listOf(
                    StatItem(
                        icon = Icons.Default.AccountBalance,
                        value = "AED ${String.format("%.0f", dashboardData.thisMonthUberEarnings)}",
                        label = "Uber (Month)",
                        shortcut = DashboardShortcut.IncomeSource.Uber
                    ),
                    StatItem(
                        icon = Icons.Default.AccountBalance,
                        value = "AED ${String.format("%.0f", dashboardData.thisMonthYangoEarnings)}",
                        label = "Yango (Month)",
                        shortcut = DashboardShortcut.IncomeSource.Yango
                    ),
                    StatItem(
                        icon = Icons.Default.AccountBalance,
                        value = "AED ${String.format("%.0f", dashboardData.thisMonthPrivateEarnings)}",
                        label = "Private (Month)",
                        shortcut = DashboardShortcut.IncomeSource.Private
                    ),
                    StatItem(
                        icon = Icons.Default.Assignment,
                        value = "${dashboardData.recentEntries.size}",
                        label = "Recent Entries",
                        shortcut = DashboardShortcut.AllEntries
                    )
                )

                // Use actual DailyEntry objects for complete data
                val recentEntries = dashboardData.recentEntries

                updateState {
                    it.copy(
                        quickStats = quickStats,
                        earningsStats = earningsStats,
                        recentEntries = recentEntries,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}