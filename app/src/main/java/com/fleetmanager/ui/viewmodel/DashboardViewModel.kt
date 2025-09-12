package com.fleetmanager.ui.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import com.fleetmanager.domain.usecase.GetDashboardDataRealtimeUseCase
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.ui.components.StatItem
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
    val recentEntries: List<RecentEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardDataRealtimeUseCase: GetDashboardDataRealtimeUseCase,
    private val syncManager: SyncManager
) : BaseViewModel<DashboardUiState>() {

    override fun getInitialState() = DashboardUiState()

    init {
        loadDashboardData()
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
                        label = "This Month"
                    ),
                    StatItem(
                        icon = Icons.Default.TrendingUp,
                        value = "$${String.format("%.0f", dashboardData.thisWeekEarnings)}",
                        label = "This Week"
                    ),
                    StatItem(
                        icon = Icons.Default.Schedule,
                        value = "$${String.format("%.0f", dashboardData.last24hEarnings)}",
                        label = "Last 24h"
                    ),
                    StatItem(
                        icon = Icons.Default.People,
                        value = dashboardData.activeDriversCount.toString(),
                        label = "Active Drivers"
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
                        recentEntries = recentEntries,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
}