package com.fleetmanager.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
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
import com.fleetmanager.ui.model.FilterContext
import com.fleetmanager.ui.model.FilterContextFactory
import com.fleetmanager.ui.model.TimeRange
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
                        value = "$${String.format("%.0f", dashboardData.thisMonthEarnings)}",
                        label = "This Month",
                        filterContext = FilterContextFactory.createThisMonthFilter()
                    ))
                    add(StatItem(
                        icon = Icons.Default.TrendingUp,
                        value = "$${String.format("%.0f", dashboardData.thisWeekEarnings)}",
                        label = "This Week",
                        filterContext = FilterContextFactory.createThisWeekFilter()
                    ))
                    add(StatItem(
                        icon = Icons.Default.Schedule,
                        value = "$${String.format("%.0f", dashboardData.last24hEarnings)}",
                        label = "Last 24h",
                        filterContext = FilterContextFactory.createLast24HFilter()
                    ))
                    // Only show Active Drivers tile for admin users
                    if (isAdmin) {
                        add(StatItem(
                            icon = Icons.Default.People,
                            value = dashboardData.activeDriversCount.toString(),
                            label = "Active Drivers"
                            // No click action for active drivers count
                        ))
                    }
                }
                
                // Only show earnings stats for admin users
                val earningsStats = if (isAdmin) {
                    listOf(
                        StatItem(
                            icon = Icons.Default.AttachMoney,
                            value = "$${String.format("%.0f", dashboardData.thisMonthUberEarnings)}",
                            label = "Uber (Month)",
                            filterContext = FilterContextFactory.createUberEarningsFilter(TimeRange.THIS_MONTH)
                        ),
                        StatItem(
                            icon = Icons.Default.AttachMoney,
                            value = "$${String.format("%.0f", dashboardData.thisMonthYangoEarnings)}",
                            label = "Yango (Month)",
                            filterContext = FilterContextFactory.createYangoEarningsFilter(TimeRange.THIS_MONTH)
                        ),
                        StatItem(
                            icon = Icons.Default.AttachMoney,
                            value = "$${String.format("%.0f", dashboardData.thisMonthPrivateEarnings)}",
                            label = "Private (Month)",
                            filterContext = FilterContextFactory.createPrivateEarningsFilter(TimeRange.THIS_MONTH)
                        ),
                        StatItem(
                            icon = Icons.Default.Assignment,
                            value = "${dashboardData.recentEntries.size}",
                            label = "Recent Entries"
                            // No click action for recent entries count
                        )
                    )
                } else {
                    emptyList() // Hide all earnings stats for non-admin users
                }

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