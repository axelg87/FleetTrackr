package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.model.DayOfWeekAnalysis
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Day of Week Analysis component showing average income patterns by weekday
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekChart(
    dayOfWeekAnalysis: List<DayOfWeekAnalysis>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
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
                        text = "Day of Week Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Average income patterns by weekday",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.CalendarToday,
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
            } else if (dayOfWeekAnalysis.isEmpty()) {
                EmptyDayOfWeekState()
            } else {
                // Toggle for details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDetails = !showDetails }
                    ) {
                        Text(
                            text = if (showDetails) "Hide Details" else "Show Details",
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Chart
                DayOfWeekBarChart(
                    data = dayOfWeekAnalysis,
                    showDetails = showDetails
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Insights
                DayOfWeekInsights(dayOfWeekAnalysis = dayOfWeekAnalysis)
            }
        }
    }
}

@Composable
private fun DayOfWeekBarChart(
    data: List<DayOfWeekAnalysis>,
    showDetails: Boolean
) {
    if (data.isEmpty()) return
    
    val maxIncome = data.maxOfOrNull { it.averageIncome } ?: 1.0
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { dayData ->
            DayOfWeekBar(
                dayData = dayData,
                maxIncome = maxIncome,
                showDetails = showDetails
            )
        }
    }
}

@Composable
private fun DayOfWeekBar(
    dayData: DayOfWeekAnalysis,
    maxIncome: Double,
    showDetails: Boolean
) {
    val progressPercentage = if (maxIncome > 0) (dayData.averageIncome / maxIncome).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "day_progress"
    )
    
    val dayColor = AnalyticsUtils.getDayOfWeekColor(dayData.dayOfWeek)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AnalyticsUtils.getDayDisplayName(dayData.dayOfWeek),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.3f)
            )
            
            // Progress bar
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .height(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(
                            color = dayColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                
                // Value text inside bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = AnalyticsUtils.formatCurrency(dayData.averageIncome),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Text(
                text = AnalyticsUtils.formatWholeNumber((progressPercentage * 100).toDouble()) + "%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = dayColor,
                modifier = Modifier.weight(0.2f)
            )
        }
        
        if (showDetails) {
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailItem(
                    label = "Total Days",
                    value = "AED{dayData.totalDays}"
                )
                DetailItem(
                    label = "Total Income",
                    value = AnalyticsUtils.formatCurrency(dayData.totalIncome)
                )
                DetailItem(
                    label = "Performance",
                    value = AnalyticsUtils.getPerformanceLevel(dayData.averageIncome)
                )
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column {
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
private fun DayOfWeekInsights(dayOfWeekAnalysis: List<DayOfWeekAnalysis>) {
    val bestDay = dayOfWeekAnalysis.maxByOrNull { it.averageIncome }
    val worstDay = dayOfWeekAnalysis.minByOrNull { it.averageIncome }
    val weekendAverage = dayOfWeekAnalysis.filter { 
        it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY 
    }.takeIf { it.isNotEmpty() }?.let { weekend ->
        weekend.sumOf { it.averageIncome } / weekend.size
    } ?: 0.0
    
    val weekdayAverage = dayOfWeekAnalysis.filter { 
        it.dayOfWeek !in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) 
    }.takeIf { it.isNotEmpty() }?.let { weekdays ->
        weekdays.sumOf { it.averageIncome } / weekdays.size
    } ?: 0.0
    
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
                text = "Weekly Insights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (bestDay != null && worstDay != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Best Day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AnalyticsUtils.getDayDisplayName(bestDay.dayOfWeek),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnalyticsUtils.Colors.SUCCESS
                        )
                        Text(
                            text = AnalyticsUtils.formatCurrency(bestDay.averageIncome),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Lowest Day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = AnalyticsUtils.getDayDisplayName(worstDay.dayOfWeek),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AnalyticsUtils.Colors.ERROR
                        )
                        Text(
                            text = AnalyticsUtils.formatCurrency(worstDay.averageIncome),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (weekendAverage > 0 && weekdayAverage > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Weekend Avg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AnalyticsUtils.formatCurrency(weekendAverage),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Weekday Avg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AnalyticsUtils.formatCurrency(weekdayAverage),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        Column {
                            Text(
                                text = "Weekend vs Weekday",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val comparison = if (weekdayAverage > 0) {
                                ((weekendAverage - weekdayAverage) / weekdayAverage) * 100
                            } else 0.0
                            Text(
                                text = "AED{if (comparison > 0) "+" else ""}AED{AnalyticsUtils.formatDecimal(comparison)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (comparison > 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = generateInsightText(dayOfWeekAnalysis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyDayOfWeekState() {
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
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No weekly pattern data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add more entries across different days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// REFACTOR: getDayColor and getPerformanceLevel moved to AnalyticsUtils

private fun generateInsightText(dayOfWeekAnalysis: List<DayOfWeekAnalysis>): String {
    val bestDay = dayOfWeekAnalysis.maxByOrNull { it.averageIncome }
    val worstDay = dayOfWeekAnalysis.minByOrNull { it.averageIncome }
    
    if (bestDay == null || worstDay == null) return "Not enough data for insights."
    
    val difference = bestDay.averageIncome - worstDay.averageIncome
    val percentageDiff = if (worstDay.averageIncome > 0) {
        (difference / worstDay.averageIncome) * 100
    } else 100.0
    
    return when {
        percentageDiff > 50 -> "AED{AnalyticsUtils.getDayDisplayName(bestDay.dayOfWeek)} significantly outperforms other days. Consider focusing marketing efforts on high-performing days."
        percentageDiff > 25 -> "There's a notable difference between your best and worst performing days. Analyze what makes AED{AnalyticsUtils.getDayDisplayName(bestDay.dayOfWeek, TextStyle.SHORT)} successful."
        percentageDiff > 10 -> "Your income is fairly consistent across the week with slight variations."
        else -> "Very consistent performance across all days of the week."
    }
}