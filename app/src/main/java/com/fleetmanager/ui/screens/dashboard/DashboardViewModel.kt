package com.fleetmanager.ui.screens.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.repository.FleetRepository
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.ui.components.StatItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
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
    private val repository: FleetRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun syncNow() {
        viewModelScope.launch {
            try {
                syncManager.syncNow()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Sync failed: ${e.message}"
                )
            }
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Combine flows for real-time updates
                combine(
                    repository.getAllEntries(),
                    repository.getAllDrivers()
                ) { entries, drivers ->
                    val driverMap = drivers.associateBy { it.id }
                    
                    // Calculate time-based stats
                    val now = Date()
                    val calendar = Calendar.getInstance()
                    
                    // This month
                    calendar.time = now
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfMonth = calendar.time
                    val thisMonthEntries = entries.filter { it.date >= startOfMonth }
                    val thisMonthEarnings = thisMonthEntries.sumOf { it.totalEarnings }
                    
                    // This week (Monday to Sunday)
                    calendar.time = now
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                    calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startOfWeek = calendar.time
                    val thisWeekEntries = entries.filter { it.date >= startOfWeek }
                    val thisWeekEarnings = thisWeekEntries.sumOf { it.totalEarnings }
                    
                    // Last 24 hours
                    val last24Hours = Date(now.time - TimeUnit.HOURS.toMillis(24))
                    val last24hEntries = entries.filter { it.date >= last24Hours }
                    val last24hEarnings = last24hEntries.sumOf { it.totalEarnings }
                    
                    // Active drivers (from all entries)
                    val activeDrivers = entries.distinctBy { it.driverName }.size

                    val quickStats = listOf(
                        StatItem(
                            icon = Icons.Default.CalendarToday,
                            value = "$${String.format("%.0f", thisMonthEarnings)}",
                            label = "This Month"
                        ),
                        StatItem(
                            icon = Icons.Default.TrendingUp,
                            value = "$${String.format("%.0f", thisWeekEarnings)}",
                            label = "This Week"
                        ),
                        StatItem(
                            icon = Icons.Default.Schedule,
                            value = "$${String.format("%.0f", last24hEarnings)}",
                            label = "Last 24h"
                        ),
                        StatItem(
                            icon = Icons.Default.People,
                            value = activeDrivers.toString(),
                            label = "Active Drivers"
                        )
                    )

                    // Get recent entries (last 5)
                    val recentEntries = entries
                        .sortedByDescending { it.date }
                        .take(5)
                        .map { entry ->
                            RecentEntry(
                                id = entry.id,
                                driverName = entry.driverName,
                                date = entry.date.toString(),
                                totalEarnings = entry.totalEarnings,
                                totalRides = 0 // We don't have ride counts in the current model
                            )
                        }

                    DashboardUiState(
                        quickStats = quickStats,
                        recentEntries = recentEntries,
                        isLoading = false
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}