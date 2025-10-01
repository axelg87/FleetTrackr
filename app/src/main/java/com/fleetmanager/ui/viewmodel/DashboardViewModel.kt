package com.fleetmanager.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.data.remote.VehicleFirestoreService
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.domain.usecase.DashboardData
import com.fleetmanager.domain.usecase.GetDashboardDataRealtimeUseCase
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.ui.components.StatItem
import com.fleetmanager.ui.navigation.DashboardShortcut
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject


enum class MonthFilter(
    val chipLabel: String,
    val quickStatLabel: String,
    val earningsLabelSuffix: String
) {
    CURRENT("Current Month", "This Month", "Current"),
    LAST("Last Month", "Last Month", "Last")
}

data class DashboardUiState(
    val quickStats: List<StatItem> = emptyList(),
    val earningsStats: List<StatItem> = emptyList(), // New tiles for specific earnings
    val recentEntries: List<com.fleetmanager.domain.model.DailyEntry> = emptyList(),
    val userProfile: UserDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val monthFilter: MonthFilter = MonthFilter.CURRENT
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataRealtimeUseCase: GetDashboardDataRealtimeUseCase,
    private val syncManager: SyncManager,
    private val userFirestoreService: UserFirestoreService,
    private val firestoreService: FirestoreService,
    private val vehicleFirestoreService: VehicleFirestoreService,
    private val authRepository: com.fleetmanager.domain.repository.AuthRepository
) : BaseViewModel<DashboardUiState>() {

    private var latestDashboardData: DashboardData? = null

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
                rebuildStatsWithCurrentFilter()
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
            combine(
                getDashboardDataRealtimeUseCase(),
                firestoreService.getDriversFlow(),
                vehicleFirestoreService.getVehiclesFlow()
            ) { dashboardData, drivers, vehicles ->
                val driverNameMap = drivers.associateBy({ it.id }, { it.name })
                val vehicleNameMap = vehicles.associateBy({ it.id }, { it.displayName })

                dashboardData.copy(
                    recentEntries = dashboardData.recentEntries.map { entry ->
                        entry.withResolvedDisplayData(
                            driverDisplayName = driverNameMap[entry.driverId],
                            vehicleDisplayName = vehicleNameMap[entry.vehicleId]
                        )
                    }
                )
            }.collect { dashboardData ->
                latestDashboardData = dashboardData
                val currentUserRole = uiState.value.userProfile?.role ?: UserRole.DRIVER
                val isAdmin = PermissionManager.canSeeAdminControls(currentUserRole)
                val (quickStats, earningsStats) = buildDashboardStats(
                    dashboardData,
                    uiState.value.monthFilter,
                    isAdmin
                )

                updateState {
                    it.copy(
                        quickStats = quickStats,
                        earningsStats = earningsStats,
                        recentEntries = dashboardData.recentEntries,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onMonthFilterSelected(filter: MonthFilter) {
        if (filter == uiState.value.monthFilter) return

        val dashboardData = latestDashboardData
        if (dashboardData == null) {
            updateState { it.copy(monthFilter = filter) }
            return
        }

        val isAdmin = PermissionManager.canSeeAdminControls(
            uiState.value.userProfile?.role ?: UserRole.DRIVER
        )
        val (quickStats, earningsStats) = buildDashboardStats(dashboardData, filter, isAdmin)

        updateState {
            it.copy(
                monthFilter = filter,
                quickStats = quickStats,
                earningsStats = earningsStats
            )
        }
    }

    private fun buildDashboardStats(
        dashboardData: DashboardData,
        monthFilter: MonthFilter,
        isAdmin: Boolean
    ): Pair<List<StatItem>, List<StatItem>> {
        val quickStats = buildQuickStats(dashboardData, monthFilter, isAdmin)
        val earningsStats = buildEarningsStats(dashboardData, monthFilter)
        return quickStats to earningsStats
    }

    private fun buildQuickStats(
        dashboardData: DashboardData,
        monthFilter: MonthFilter,
        isAdmin: Boolean
    ): List<StatItem> {
        val monthEarnings = when (monthFilter) {
            MonthFilter.CURRENT -> dashboardData.thisMonthEarnings
            MonthFilter.LAST -> dashboardData.lastMonthEarnings
        }
        val monthShortcut = when (monthFilter) {
            MonthFilter.CURRENT -> DashboardShortcut.TimeRange.ThisMonth
            MonthFilter.LAST -> DashboardShortcut.TimeRange.LastMonth
        }

        return buildList {
            add(
                StatItem(
                    icon = Icons.Default.CalendarToday,
                    value = formatCurrency(monthEarnings),
                    label = monthFilter.quickStatLabel,
                    shortcut = monthShortcut
                )
            )
            add(
                StatItem(
                    icon = Icons.Default.TrendingUp,
                    value = formatCurrency(dashboardData.thisWeekEarnings),
                    label = "This Week",
                    shortcut = DashboardShortcut.TimeRange.ThisWeek
                )
            )
            add(
                StatItem(
                    icon = Icons.Default.Schedule,
                    value = formatCurrency(dashboardData.last24hEarnings),
                    label = "Last 24h",
                    shortcut = DashboardShortcut.TimeRange.Last24Hours
                )
            )
            if (isAdmin) {
                add(
                    StatItem(
                        icon = Icons.Default.People,
                        value = dashboardData.activeDriversCount.toString(),
                        label = "Active Drivers",
                        shortcut = DashboardShortcut.AllEntries
                    )
                )
            }
        }
    }

    private fun buildEarningsStats(
        dashboardData: DashboardData,
        monthFilter: MonthFilter
    ): List<StatItem> {
        val (uberEarnings, yangoEarnings, privateEarnings) = when (monthFilter) {
            MonthFilter.CURRENT -> Triple(
                dashboardData.thisMonthUberEarnings,
                dashboardData.thisMonthYangoEarnings,
                dashboardData.thisMonthPrivateEarnings
            )

            MonthFilter.LAST -> Triple(
                dashboardData.lastMonthUberEarnings,
                dashboardData.lastMonthYangoEarnings,
                dashboardData.lastMonthPrivateEarnings
            )
        }

        val labelSuffix = monthFilter.earningsLabelSuffix

        return listOf(
            StatItem(
                icon = Icons.Default.AccountBalance,
                value = formatCurrency(uberEarnings),
                label = "Uber ($labelSuffix)",
                shortcut = DashboardShortcut.IncomeSource.Uber,
                trend = dashboardData.uberTrend,
                trendColor = Color(0xFF1A73E8)
            ),
            StatItem(
                icon = Icons.Default.AccountBalance,
                value = formatCurrency(yangoEarnings),
                label = "Yango ($labelSuffix)",
                shortcut = DashboardShortcut.IncomeSource.Yango,
                trend = dashboardData.yangoTrend,
                trendColor = Color(0xFFF9A825)
            ),
            StatItem(
                icon = Icons.Default.AccountBalance,
                value = formatCurrency(privateEarnings),
                label = "Private ($labelSuffix)",
                shortcut = DashboardShortcut.IncomeSource.Private,
                trend = dashboardData.privateTrend,
                trendColor = Color(0xFF8E24AA)
            ),
            StatItem(
                icon = Icons.Default.Assignment,
                value = "${dashboardData.recentEntries.size}",
                label = "Recent Entries",
                shortcut = DashboardShortcut.AllEntries
            )
        )
    }

    private fun formatCurrency(amount: Double): String {
        return "AED ${String.format("%.0f", amount)}"
    }

    private fun rebuildStatsWithCurrentFilter() {
        val dashboardData = latestDashboardData ?: return
        val isAdmin = PermissionManager.canSeeAdminControls(
            uiState.value.userProfile?.role ?: UserRole.DRIVER
        )
        val (quickStats, earningsStats) = buildDashboardStats(
            dashboardData,
            uiState.value.monthFilter,
            isAdmin
        )

        updateState {
            it.copy(
                quickStats = quickStats,
                earningsStats = earningsStats
            )
        }
    }
}
