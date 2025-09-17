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

data class RecentEntry(
    val id: String,
    val driverName: String,
    val date: String,
    val totalEarnings: Double,
    val totalRides: Int
)

data class DashboardUiState(
    val quickStats: List<StatItem> = emptyList(),
    val earningsStats: List<StatItem> = emptyList(), // New tiles for specific earnings
    val recentEntries: List<RecentEntry> = emptyList(),
    val userProfile: UserDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataRealtimeUseCase: GetDashboardDataRealtimeUseCase,
    private val syncManager: SyncManager,
    private val userFirestoreService: UserFirestoreService
) : BaseViewModel<DashboardUiState>() {

    override fun getInitialState() = DashboardUiState()

    // Navigation callback for tile clicks
    private var onNavigateToReportsWithFilter: ((FilterContext) -> Unit)? = null

    init {
        loadDashboardData()
        loadUserProfile()
    }
    
    fun setNavigationCallback(callback: (FilterContext) -> Unit) {
        onNavigateToReportsWithFilter = callback
    }

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
                val quickStats = listOf(
                    StatItem(
                        icon = Icons.Default.CalendarToday,
                        value = "$${String.format("%.0f", dashboardData.thisMonthEarnings)}",
                        label = "This Month",
                        onClick = {
                            onNavigateToReportsWithFilter?.invoke(
                                FilterContextFactory.createThisMonthFilter()
                            )
                        }
                    ),
                    StatItem(
                        icon = Icons.Default.TrendingUp,
                        value = "$${String.format("%.0f", dashboardData.thisWeekEarnings)}",
                        label = "This Week",
                        onClick = {
                            onNavigateToReportsWithFilter?.invoke(
                                FilterContextFactory.createThisWeekFilter()
                            )
                        }
                    ),
                    StatItem(
                        icon = Icons.Default.Schedule,
                        value = "$${String.format("%.0f", dashboardData.last24hEarnings)}",
                        label = "Last 24h",
                        onClick = {
                            onNavigateToReportsWithFilter?.invoke(
                                FilterContextFactory.createLast24HFilter()
                            )
                        }
                    ),
                    StatItem(
                        icon = Icons.Default.People,
                        value = dashboardData.activeDriversCount.toString(),
                        label = "Active Drivers"
                        // No click action for active drivers count
                    )
                )
                
                val earningsStats = listOf(
                    StatItem(
                        icon = Icons.Default.AttachMoney,
                        value = "$${String.format("%.0f", dashboardData.thisMonthUberEarnings)}",
                        label = "Uber (Month)",
                        onClick = {
                            onNavigateToReportsWithFilter?.invoke(
                                FilterContextFactory.createUberEarningsFilter(TimeRange.THIS_MONTH)
                            )
                        }
                    ),
                    StatItem(
                        icon = Icons.Default.AttachMoney,
                        value = "$${String.format("%.0f", dashboardData.thisMonthYangoEarnings)}",
                        label = "Yango (Month)",
                        onClick = {
                            onNavigateToReportsWithFilter?.invoke(
                                FilterContextFactory.createYangoEarningsFilter(TimeRange.THIS_MONTH)
                            )
                        }
                    ),
                    StatItem(
                        icon = Icons.Default.AttachMoney,
                        value = "$${String.format("%.0f", dashboardData.thisMonthPrivateEarnings)}",
                        label = "Private (Month)",
                        onClick = {
                            onNavigateToReportsWithFilter?.invoke(
                                FilterContextFactory.createPrivateEarningsFilter(TimeRange.THIS_MONTH)
                            )
                        }
                    ),
                    StatItem(
                        icon = Icons.Default.Assignment,
                        value = "${dashboardData.recentEntries.size}",
                        label = "Recent Entries"
                        // No click action for recent entries count
                    )
                )

                val recentEntries = dashboardData.recentEntries.map { entry ->
                    RecentEntry(
                        id = entry.id,
                        driverName = entry.driverName,
                        date = entry.date.toString(),
                        totalEarnings = entry.totalEarnings,
                        totalRides = 0 // We don't have ride counts in the current model
                    )
                }

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