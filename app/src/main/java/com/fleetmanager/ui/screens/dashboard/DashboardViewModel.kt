package com.fleetmanager.ui.screens.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.repository.FleetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickStat(
    val icon: ImageVector,
    val value: String,
    val label: String
)

data class RecentEntry(
    val id: String,
    val driverName: String,
    val date: String,
    val totalEarnings: Double,
    val totalRides: Int
)

data class DashboardUiState(
    val quickStats: List<QuickStat> = emptyList(),
    val recentEntries: List<RecentEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: FleetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
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
                    
                    // Calculate quick stats
                    val totalEarnings = entries.sumOf { it.totalEarnings }
                    val totalEntries = entries.size
                    val activeDrivers = entries.distinctBy { it.driverId }.size
                    val avgEarningsPerEntry = if (totalEntries > 0) totalEarnings / totalEntries else 0.0

                    val quickStats = listOf(
                        QuickStat(
                            icon = Icons.Default.AttachMoney,
                            value = "$${String.format("%.0f", totalEarnings)}",
                            label = "Total Earnings"
                        ),
                        QuickStat(
                            icon = Icons.Default.Assignment,
                            value = totalEntries.toString(),
                            label = "Total Entries"
                        ),
                        QuickStat(
                            icon = Icons.Default.People,
                            value = activeDrivers.toString(),
                            label = "Active Drivers"
                        ),
                        QuickStat(
                            icon = Icons.Default.TrendingUp,
                            value = "$${String.format("%.0f", avgEarningsPerEntry)}",
                            label = "Avg per Entry"
                        )
                    )

                    // Get recent entries (last 5)
                    val recentEntries = entries
                        .sortedByDescending { it.date }
                        .take(5)
                        .map { entry ->
                            val driver = driverMap[entry.driverId]
                            RecentEntry(
                                id = entry.id,
                                driverName = driver?.name ?: "Unknown Driver",
                                date = entry.date,
                                totalEarnings = entry.totalEarnings,
                                totalRides = entry.uberRides + entry.yangoRides + entry.privateRides
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