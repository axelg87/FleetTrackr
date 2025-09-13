package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetmanager.ui.screens.analytics.model.AnomalyData
import com.fleetmanager.ui.screens.analytics.model.AnomalyType
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import java.time.format.DateTimeFormatter

/**
 * Anomaly Detection component showing unusual patterns in income and expenses
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnomalyDetection(
    anomalies: List<AnomalyData>,
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AnalyticsUtils.Colors.WARNING,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Anomaly Detection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Unusual patterns and outliers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Anomaly count badge
                if (anomalies.isNotEmpty()) {
                    Badge(
                        containerColor = AnalyticsUtils.Colors.WARNING
                    ) {
                        Text(
                            text = anomalies.size.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
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
            } else if (anomalies.isEmpty()) {
                NoAnomaliesState()
            } else {
                // Anomaly summary
                AnomalySummary(anomalies = anomalies)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Anomaly list
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(anomalies) { anomaly ->
                        AnomalyCard(anomaly = anomaly)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnomalySummary(anomalies: List<AnomalyData>) {
    val anomalyCounts = anomalies.groupBy { it.type }.mapValues { it.value.size }
    
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
                text = "Anomaly Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnomalyStat(
                    label = "Low Income",
                    count = anomalyCounts[AnomalyType.LOW_INCOME] ?: 0,
                    color = AnalyticsUtils.Colors.ERROR
                )
                AnomalyStat(
                    label = "High Expenses",
                    count = anomalyCounts[AnomalyType.HIGH_EXPENSES] ?: 0,
                    color = AnalyticsUtils.Colors.WARNING
                )
                AnomalyStat(
                    label = "Zero Income",
                    count = anomalyCounts[AnomalyType.ZERO_INCOME] ?: 0,
                    color = Color(0xFF9C27B0)
                )
                AnomalyStat(
                    label = "Other",
                    count = anomalyCounts[AnomalyType.UNUSUAL_PATTERN] ?: 0,
                    color = Color(0xFF607D8B)
                )
            }
        }
    }
}

@Composable
private fun AnomalyStat(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AnomalyCard(anomaly: AnomalyData) {
    val (icon, color) = getAnomalyIconAndColor(anomaly.type)
    val anomalyColor = AnalyticsUtils.getAnomalyColor(anomaly.type)
    val severityLevel = AnalyticsUtils.getSeverityLevel(anomaly.deviation)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(color.copy(alpha = 0.3f))
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with icon, date, and severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = anomaly.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Severity badge
                Badge(
                    containerColor = AnalyticsUtils.getSeverityColor(severityLevel)
                ) {
                    Text(
                        text = severityLevel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Anomaly type and reason
            Text(
                text = getAnomalyTypeDescription(anomaly.type),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = color
            )
            
            Text(
                text = anomaly.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Values comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Actual Value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(anomaly.actualValue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                
                Column {
                    Text(
                        text = "Expected Value",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(anomaly.expectedValue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = "Deviation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatDecimal(anomaly.deviation * 100) + "%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            
            // Action recommendation (if applicable)
            val recommendation = getRecommendation(anomaly)
            if (recommendation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoAnomaliesState() {
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
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFF4CAF50), // Green for good news
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No anomalies detected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Your income and expenses are following normal patterns",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getAnomalyIconAndColor(type: AnomalyType): Pair<ImageVector, Color> {
    return when (type) {
        AnomalyType.LOW_INCOME -> Icons.Default.TrendingDown to Color(0xFFF44336)
        AnomalyType.HIGH_EXPENSES -> Icons.Default.TrendingUp to Color(0xFFFF9800)
        AnomalyType.ZERO_INCOME -> Icons.Default.Error to Color(0xFF9C27B0)
        AnomalyType.UNUSUAL_PATTERN -> Icons.Default.Warning to Color(0xFF607D8B)
    }
}

private fun getAnomalyTypeDescription(type: AnomalyType): String {
    return when (type) {
        AnomalyType.LOW_INCOME -> "Low Income Alert"
        AnomalyType.HIGH_EXPENSES -> "High Expenses Alert"
        AnomalyType.ZERO_INCOME -> "Zero Income Alert"
        AnomalyType.UNUSUAL_PATTERN -> "Unusual Pattern"
    }
}

// REFACTOR: getSeverityLevel and getSeverityColor moved to AnalyticsUtils

private fun getRecommendation(anomaly: AnomalyData): String {
    return when (anomaly.type) {
        AnomalyType.LOW_INCOME -> "Consider analyzing what caused the low income on this day. Check driver schedules and market conditions."
        AnomalyType.HIGH_EXPENSES -> "Review the expenses for this day. Ensure all charges are legitimate and necessary."
        AnomalyType.ZERO_INCOME -> "Investigate why no income was recorded. This could indicate a system issue or operational problem."
        AnomalyType.UNUSUAL_PATTERN -> "Monitor this pattern closely to determine if it's a one-time occurrence or emerging trend."
    }
}