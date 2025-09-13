package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
 * Top Drivers Leaderboard component showcasing the best performing drivers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopDriversLeaderboard(
    driverPerformance: List<DriverPerformance>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    maxDrivers: Int = 5
) {
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AnalyticsUtils.Colors.GOLD,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Top Performers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Leading drivers by total revenue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                EmptyLeaderboardState()
            } else {
                val topDrivers = driverPerformance
                    .sortedByDescending { it.totalRevenue }
                    .take(maxDrivers)
                
                // Podium (Top 3)
                if (topDrivers.isNotEmpty()) {
                    PodiumDisplay(topDrivers = topDrivers.take(3))
                    
                    if (topDrivers.size > 3) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Remaining drivers
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Other Top Performers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            topDrivers.drop(3).forEachIndexed { index, driver ->
                                LeaderboardRow(
                                    driver = driver,
                                    rank = index + 4,
                                    maxRevenue = topDrivers.first().totalRevenue
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Leaderboard stats
                LeaderboardStats(topDrivers = topDrivers)
            }
        }
    }
}

@Composable
private fun PodiumDisplay(topDrivers: List<DriverPerformance>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd place
        if (topDrivers.size >= 2) {
            PodiumPosition(
                driver = topDrivers[1],
                rank = 2,
                height = 80.dp,
                color = AnalyticsUtils.Colors.SILVER
            )
        }
        
        // 1st place (tallest)
        if (topDrivers.isNotEmpty()) {
            PodiumPosition(
                driver = topDrivers[0],
                rank = 1,
                height = 100.dp,
                color = AnalyticsUtils.Colors.GOLD
            )
        }
        
        // 3rd place
        if (topDrivers.size >= 3) {
            PodiumPosition(
                driver = topDrivers[2],
                rank = 3,
                height = 60.dp,
                color = AnalyticsUtils.Colors.BRONZE
            )
        }
    }
}

@Composable
private fun PodiumPosition(
    driver: DriverPerformance,
    rank: Int,
    height: Dp,
    color: Color
) {
    val animatedHeight by animateFloatAsState(
        targetValue = height.value,
        animationSpec = tween(1000),
        label = "podium_height"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // Crown for 1st place
        if (rank == 1) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AnalyticsUtils.Colors.GOLD,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Driver avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = color.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Driver name
        Text(
            text = driver.driverName.split(" ").firstOrNull() ?: driver.driverName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        
        // Revenue
        Text(
            text = AnalyticsUtils.formatCurrency(driver.totalRevenue),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Podium base
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(animatedHeight.dp)
                .background(
                    color = color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    driver: DriverPerformance,
    rank: Int,
    maxRevenue: Double
) {
    val progressPercentage = if (maxRevenue > 0) (driver.totalRevenue / maxRevenue).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "leaderboard_progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Driver info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = driver.driverName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${driver.activeDays} days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${AnalyticsUtils.formatCurrency(driver.averageRevenuePerDay)}/day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Revenue and progress
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = AnalyticsUtils.formatCurrency(driver.totalRevenue),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardStats(topDrivers: List<DriverPerformance>) {
    if (topDrivers.isEmpty()) return
    
    val totalRevenue = topDrivers.sumOf { it.totalRevenue }
    val averageRevenue = totalRevenue / topDrivers.size
    val topPerformerAdvantage = if (topDrivers.size >= 2) {
        val first = topDrivers[0].totalRevenue
        val second = topDrivers[1].totalRevenue
        if (second > 0) ((first - second) / second) * 100 else 0.0
    } else 0.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Leaderboard Stats",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Combined Revenue",
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
                        text = "Average Revenue",
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
            
            if (topPerformerAdvantage > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Top Performer Lead",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${AnalyticsUtils.formatDecimal(topPerformerAdvantage)}% ahead",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnalyticsUtils.Colors.SUCCESS
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Champion",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = topDrivers[0].driverName.split(" ").firstOrNull() ?: topDrivers[0].driverName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnalyticsUtils.Colors.GOLD
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLeaderboardState() {
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
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No leaderboard data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add entries to see top performers",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}