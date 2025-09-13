package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.model.ProjectionData
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import kotlin.math.max

/**
 * Projection/Estimation component showing end-of-month revenue projections
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionEstimation(
    projectionData: ProjectionData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
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
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Revenue Projection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "End-of-month revenue estimation",
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
            } else if (projectionData == null) {
                EmptyProjectionState()
            } else {
                // Projection content
                ProjectionContent(projectionData = projectionData)
            }
        }
    }
}

@Composable
private fun ProjectionContent(projectionData: ProjectionData) {
    Column {
        // Main projection display
        ProjectionMainCard(projectionData = projectionData)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress visualization
        MonthProgressVisualization(projectionData = projectionData)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detailed metrics
        ProjectionMetrics(projectionData = projectionData)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Projection insights
        ProjectionInsights(projectionData = projectionData)
    }
}

@Composable
private fun ProjectionMainCard(projectionData: ProjectionData) {
    val projectionColor = when {
        projectionData.projectedMonthTotal > projectionData.currentMonthTotal * 1.2 -> Color(0xFF4CAF50) // Green for optimistic
        projectionData.projectedMonthTotal > projectionData.currentMonthTotal -> Color(0xFF2196F3) // Blue for positive
        else -> Color(0xFFFF9800) // Orange for conservative
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = projectionColor.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(projectionColor.copy(alpha = 0.3f))
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = projectionColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Projected Month-End Total",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = AnalyticsCalculator.formatCurrency(projectionData.projectedMonthTotal),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = projectionColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val remainingDays = projectionData.totalDaysInMonth - projectionData.daysElapsed
            val projectedRemaining = projectionData.dailyAverage * remainingDays
            
            Text(
                text = "Expected additional: ${AnalyticsCalculator.formatCurrency(projectedRemaining)} over $remainingDays days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthProgressVisualization(projectionData: ProjectionData) {
    val progressPercentage = projectionData.daysElapsed.toFloat() / projectionData.totalDaysInMonth.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "month_progress"
    )
    
    Column {
        Text(
            text = "Month Progress",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Day ${projectionData.daysElapsed} of ${projectionData.totalDaysInMonth}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "${String.format("%.1f", progressPercentage * 100)}% complete",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProjectionMetrics(projectionData: ProjectionData) {
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
                text = "Projection Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "Current Total",
                    value = AnalyticsCalculator.formatCurrency(projectionData.currentMonthTotal),
                    color = MaterialTheme.colorScheme.primary
                )
                
                MetricItem(
                    label = "Daily Average",
                    value = AnalyticsCalculator.formatCurrency(projectionData.dailyAverage),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val remainingDays = projectionData.totalDaysInMonth - projectionData.daysElapsed
                val projectedRemaining = projectionData.dailyAverage * remainingDays
                
                MetricItem(
                    label = "Days Remaining",
                    value = remainingDays.toString(),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                MetricItem(
                    label = "Projected Remaining",
                    value = AnalyticsCalculator.formatCurrency(projectedRemaining),
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Accuracy indicator
            val accuracyPercentage = calculateAccuracyPercentage(projectionData)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Projection Confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.0f", accuracyPercentage)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getConfidenceColor(accuracyPercentage)
                    )
                    Text(
                        text = getConfidenceLabel(accuracyPercentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = getConfidenceColor(accuracyPercentage)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    color: Color
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ProjectionInsights(projectionData: ProjectionData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Insights & Recommendations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = generateProjectionInsight(projectionData),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = generateActionableRecommendation(projectionData),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyProjectionState() {
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
                imageVector = Icons.Default.QueryStats,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No projection data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add more entries this month to see projections",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun calculateAccuracyPercentage(projectionData: ProjectionData): Double {
    // Higher confidence with more days of data
    val daysPercentage = (projectionData.daysElapsed.toDouble() / projectionData.totalDaysInMonth.toDouble()) * 100
    
    return when {
        daysPercentage >= 75 -> 95.0
        daysPercentage >= 50 -> 85.0
        daysPercentage >= 25 -> 75.0
        daysPercentage >= 10 -> 65.0
        else -> 50.0
    }
}

private fun getConfidenceColor(confidence: Double): Color {
    return when {
        confidence >= 90 -> Color(0xFF4CAF50) // Green
        confidence >= 75 -> Color(0xFF2196F3) // Blue
        confidence >= 60 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

private fun getConfidenceLabel(confidence: Double): String {
    return when {
        confidence >= 90 -> "Very High"
        confidence >= 75 -> "High"
        confidence >= 60 -> "Moderate"
        else -> "Low"
    }
}

private fun generateProjectionInsight(projectionData: ProjectionData): String {
    val remainingDays = projectionData.totalDaysInMonth - projectionData.daysElapsed
    val currentPace = projectionData.currentMonthTotal / projectionData.daysElapsed
    val targetDaily = projectionData.projectedMonthTotal / projectionData.totalDaysInMonth
    
    return when {
        currentPace > targetDaily * 1.1 -> 
            "You're ahead of pace! Your daily average of ${AnalyticsCalculator.formatCurrency(projectionData.dailyAverage)} is ${String.format("%.1f", ((currentPace / targetDaily - 1) * 100))}% above the projected rate."
        
        currentPace < targetDaily * 0.9 -> 
            "You're slightly behind the projected pace. Your current daily average is ${AnalyticsCalculator.formatCurrency(projectionData.dailyAverage)}, but you'd need ${AnalyticsCalculator.formatCurrency(targetDaily)} daily to meet typical projections."
        
        else -> 
            "You're on track with your current performance. At ${AnalyticsCalculator.formatCurrency(projectionData.dailyAverage)} per day, you're maintaining a steady pace toward the month-end projection."
    }
}

private fun generateActionableRecommendation(projectionData: ProjectionData): String {
    val remainingDays = projectionData.totalDaysInMonth - projectionData.daysElapsed
    val neededDaily = if (remainingDays > 0) {
        (projectionData.projectedMonthTotal - projectionData.currentMonthTotal) / remainingDays
    } else 0.0
    
    return when {
        remainingDays <= 5 -> 
            "💡 Month is almost over! Focus on maximizing the remaining ${remainingDays} days with high-value activities."
        
        neededDaily > projectionData.dailyAverage * 1.2 -> 
            "💡 To exceed projections, aim for ${AnalyticsCalculator.formatCurrency(neededDaily)} per day. Consider peak hour optimization or additional drivers."
        
        neededDaily < projectionData.dailyAverage * 0.8 -> 
            "💡 You're in a strong position! Maintain current performance or use this opportunity to focus on operational improvements."
        
        else -> 
            "💡 Maintain your current pace of ${AnalyticsCalculator.formatCurrency(projectionData.dailyAverage)} per day to meet projections."
    }
}