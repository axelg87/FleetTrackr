package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.model.MonthlyComparison
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import kotlin.math.abs

/**
 * Monthly Comparison component showing current vs previous month performance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyComparisonCard(
    monthlyComparison: MonthlyComparison?,
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
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Monthly Comparison",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Current vs previous month performance",
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
            } else if (monthlyComparison == null) {
                EmptyMonthlyComparisonState()
            } else {
                // Comparison content
                MonthlyComparisonContent(monthlyComparison = monthlyComparison)
            }
        }
    }
}

@Composable
private fun MonthlyComparisonContent(monthlyComparison: MonthlyComparison) {
    val growthColor = AnalyticsUtils.getGrowthColor(monthlyComparison.growthPercentage)
    
    val growthIcon = when {
        monthlyComparison.growthPercentage > 5 -> Icons.Default.TrendingUp
        monthlyComparison.growthPercentage < -5 -> Icons.Default.TrendingDown
        else -> Icons.Default.TrendingFlat
    }
    
    Column {
        // Main comparison card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = growthColor.copy(alpha = 0.1f)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(growthColor.copy(alpha = 0.3f))
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Growth indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = growthIcon,
                        contentDescription = null,
                        tint = growthColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AED{if (monthlyComparison.growthPercentage > 0) "+" else ""}AED{AnalyticsUtils.formatDecimal(monthlyComparison.growthPercentage)}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = growthColor
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = AnalyticsUtils.getGrowthDescription(monthlyComparison.growthPercentage),
                    style = MaterialTheme.typography.bodyMedium,
                    color = growthColor,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount difference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "AED{if (monthlyComparison.growthAmount > 0) "+" else ""}AED{AnalyticsUtils.formatCurrency(monthlyComparison.growthAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = growthColor
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Month-by-month breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Previous month
            MonthCard(
                title = monthlyComparison.previousMonth,
                amount = monthlyComparison.previousTotal,
                subtitle = "Previous Month",
                modifier = Modifier.weight(1f),
                isPrimary = false
            )
            
            // Current month
            MonthCard(
                title = monthlyComparison.currentMonth,
                amount = monthlyComparison.currentTotal,
                subtitle = "Current Month",
                modifier = Modifier.weight(1f),
                isPrimary = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress comparison
        MonthlyProgressComparison(monthlyComparison = monthlyComparison)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Insights
        MonthlyInsights(monthlyComparison = monthlyComparison)
    }
}

@Composable
private fun MonthCard(
    title: String,
    amount: Double,
    subtitle: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = AnalyticsUtils.formatCurrency(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MonthlyProgressComparison(monthlyComparison: MonthlyComparison) {
    val maxAmount = maxOf(monthlyComparison.currentTotal, monthlyComparison.previousTotal)
    val currentProgress = if (maxAmount > 0) (monthlyComparison.currentTotal / maxAmount).toFloat() else 0f
    val previousProgress = if (maxAmount > 0) (monthlyComparison.previousTotal / maxAmount).toFloat() else 0f
    
    val animatedCurrentProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(1000),
        label = "current_progress"
    )
    
    val animatedPreviousProgress by animateFloatAsState(
        targetValue = previousProgress,
        animationSpec = tween(1000),
        label = "previous_progress"
    )
    
    Column {
        Text(
            text = "Visual Comparison",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Current month bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Current",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(60.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedCurrentProgress)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Previous month bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Previous",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(60.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedPreviousProgress)
                        .fillMaxHeight()
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun MonthlyInsights(monthlyComparison: MonthlyComparison) {
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
                text = "Insights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = generateMonthlyInsight(monthlyComparison),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (abs(monthlyComparison.growthPercentage) > 10) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = generateActionableInsight(monthlyComparison),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyMonthlyComparisonState() {
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
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No comparison data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Need data from multiple months to compare",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// REFACTOR: getGrowthDescription moved to AnalyticsUtils.getGrowthDescription

private fun generateMonthlyInsight(monthlyComparison: MonthlyComparison): String {
    val absGrowth = abs(monthlyComparison.growthPercentage)
    
    return when {
        monthlyComparison.growthPercentage > 15 -> 
            "Excellent performance! Revenue increased by AED{String.format("%.1f", monthlyComparison.growthPercentage)}% compared to AED{monthlyComparison.previousMonth}. This represents an additional AED{AnalyticsCalculator.formatCurrency(monthlyComparison.growthAmount)} in revenue."
        
        monthlyComparison.growthPercentage > 5 -> 
            "Positive trend with AED{String.format("%.1f", monthlyComparison.growthPercentage)}% growth. You're on the right track with AED{AnalyticsCalculator.formatCurrency(monthlyComparison.growthAmount)} more revenue than last month."
        
        absGrowth <= 5 -> 
            "Performance is relatively stable with minimal change (AED{String.format("%.1f", monthlyComparison.growthPercentage)}%). This consistency can be valuable for planning."
        
        monthlyComparison.growthPercentage < -15 -> 
            "Revenue decreased by AED{String.format("%.1f", abs(monthlyComparison.growthPercentage))}% this month. This represents AED{AnalyticsCalculator.formatCurrency(abs(monthlyComparison.growthAmount))} less revenue than AED{monthlyComparison.previousMonth}."
        
        else -> 
            "Revenue declined by AED{String.format("%.1f", abs(monthlyComparison.growthPercentage))}%. While concerning, this could be due to seasonal factors or market conditions."
    }
}

private fun generateActionableInsight(monthlyComparison: MonthlyComparison): String {
    return if (monthlyComparison.growthPercentage > 10) {
        "💡 Tip: Analyze what contributed to this growth and try to replicate those strategies next month."
    } else {
        "💡 Tip: Review your operations to identify areas for improvement. Consider driver performance, route optimization, or market expansion."
    }
}