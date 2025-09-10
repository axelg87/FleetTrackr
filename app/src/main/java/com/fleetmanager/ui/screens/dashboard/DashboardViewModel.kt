package com.fleetmanager.ui.screens.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TrendingUp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.repository.FleetRepository
import com.fleetmanager.sync.SyncManager
import com.fleetmanager.ui.components.StatItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
                    
                    // Calculate quick stats
                    val totalEarnings = entries.sumOf { it.totalEarnings }
                    val totalEntries = entries.size
                    val activeDrivers = entries.distinctBy { it.driverName }.size
                    val avgEarningsPerEntry = if (totalEntries > 0) totalEarnings / totalEntries else 0.0

                    val quickStats = listOf(
                        StatItem(
                            icon = Icons.Default.AttachMoney,
                            value = "$${String.format("%.0f", totalEarnings)}",
                            label = "Total Earnings"
                        ),
                        StatItem(
                            icon = Icons.Default.Assignment,
                            value = totalEntries.toString(),
                            label = "Total Entries"
                        ),
                        StatItem(
                            icon = Icons.Default.People,
                            value = activeDrivers.toString(),
                            label = "Active Drivers"
                        ),
                        StatItem(
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