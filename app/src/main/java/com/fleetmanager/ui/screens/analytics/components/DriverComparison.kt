package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.model.DriverPerformance
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils

/**
 * Driver Comparison component showing performance metrics for all drivers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverComparison(
    driverPerformance: List<DriverPerformance>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var sortBy by remember { mutableStateOf(DriverSortOption.TOTAL_REVENUE) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Driver Performance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Compare driver revenue and activity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (driverPerformance.isEmpty()) {
                EmptyDriverState()
            } else {
                // Sort options
                DriverSortSelector(
                    selectedSort = sortBy,
                    onSortSelected = { sortBy = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Driver list
                val sortedDrivers = when (sortBy) {
                    DriverSortOption.TOTAL_REVENUE -> driverPerformance.sortedByDescending { it.totalRevenue }
                    DriverSortOption.AVERAGE_DAILY -> driverPerformance.sortedByDescending { it.averageRevenuePerDay }
                    DriverSortOption.ACTIVE_DAYS -> driverPerformance.sortedByDescending { it.activeDays }
                    DriverSortOption.ALPHABETICAL -> driverPerformance.sortedBy { it.driverName }
                }
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sortedDrivers) { index, driver ->
                        DriverPerformanceCard(
                            driver = driver,
                            rank = index + 1,
                            maxRevenue = sortedDrivers.maxOfOrNull { it.totalRevenue } ?: 1.0
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary
                DriverSummary(driverPerformance = driverPerformance)
            }
        }
    }
}

@Composable
private fun DriverSortSelector(
    selectedSort: DriverSortOption,
    onSortSelected: (DriverSortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        DriverSortOption.values().forEach { option ->
            val isSelected = option == selectedSort
            
            TextButton(
                onClick = { onSortSelected(option) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else Color.Transparent,
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.displayName,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DriverPerformanceCard(
    driver: DriverPerformance,
    rank: Int,
    maxRevenue: Double
) {
    val progressPercentage = if (maxRevenue > 0) (driver.totalRevenue / maxRevenue).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rank <= 3) {
                AnalyticsUtils.getRankingBackgroundColor(rank)
            } else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rank badge
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = AnalyticsUtils.getRankingColor(rank),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rank.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    
                    Text(
                        text = driver.driverName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = AnalyticsUtils.formatCurrency(driver.totalRevenue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Avg/Day",
                    value = AnalyticsUtils.formatCurrency(driver.averageRevenuePerDay)
                )
                StatItem(
                    label = "Active Days",
                    value = "AED{driver.activeDays} days"
                )
                StatItem(
                    label = "Performance",
                    value = AnalyticsUtils.formatWholeNumber((progressPercentage * 100).toDouble()) + "%"
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DriverSummary(driverPerformance: List<DriverPerformance>) {
    val totalRevenue = driverPerformance.sumOf { it.totalRevenue }
    val averageRevenue = if (driverPerformance.isNotEmpty()) {
        totalRevenue / driverPerformance.size
    } else 0.0
    val topPerformer = driverPerformance.maxByOrNull { it.totalRevenue }
    val mostActive = driverPerformance.maxByOrNull { it.activeDays }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Team Summary",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Revenue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Column {
                    Text(
                        text = "Average/Driver",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(averageRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (topPerformer != null || mostActive != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (topPerformer != null) {
                        Column {
                            Text(
                                text = "Top Earner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = topPerformer.driverName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    if (mostActive != null) {
                        Column {
                            Text(
                                text = "Most Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "AED{mostActive.driverName} (AED{mostActive.activeDays}d)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDriverState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No driver data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add some entries to see driver performance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class DriverSortOption(val displayName: String) {
    TOTAL_REVENUE("Revenue"),
    AVERAGE_DAILY("Avg Daily"),
    ACTIVE_DAYS("Active Days"),
    ALPHABETICAL("A-Z")
}