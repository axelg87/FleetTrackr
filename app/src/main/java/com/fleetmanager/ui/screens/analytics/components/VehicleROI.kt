package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TrendingDown
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
import com.fleetmanager.ui.screens.analytics.model.VehicleROI
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils

/**
 * Vehicle ROI component showing profitability analysis for each vehicle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleROIAnalysis(
    vehicleROI: List<VehicleROI>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var sortBy by remember { mutableStateOf(VehicleROISortOption.ROI) }
    
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
                        text = "Vehicle ROI Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Return on investment for each vehicle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
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
            } else if (vehicleROI.isEmpty()) {
                EmptyVehicleROIState()
            } else {
                // Sort options
                VehicleROISortSelector(
                    selectedSort = sortBy,
                    onSortSelected = { sortBy = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Vehicle list
                val sortedVehicles = when (sortBy) {
                    VehicleROISortOption.ROI -> vehicleROI.sortedByDescending { it.roi }
                    VehicleROISortOption.NET_PROFIT -> vehicleROI.sortedByDescending { it.netProfit }
                    VehicleROISortOption.INCOME -> vehicleROI.sortedByDescending { it.totalIncome }
                    VehicleROISortOption.EXPENSES -> vehicleROI.sortedByDescending { it.totalExpenses }
                    VehicleROISortOption.ALPHABETICAL -> vehicleROI.sortedBy { it.vehicleName }
                }
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedVehicles) { vehicle ->
                        VehicleROICard(vehicle = vehicle)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary
                VehicleROISummary(vehicleROI = vehicleROI)
            }
        }
    }
}

@Composable
private fun VehicleROISortSelector(
    selectedSort: VehicleROISortOption,
    onSortSelected: (VehicleROISortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        VehicleROISortOption.values().forEach { option ->
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
private fun VehicleROICard(vehicle: VehicleROI) {
    val isProfit = vehicle.netProfit >= 0
    val roiColor = AnalyticsUtils.getROIColor(vehicle.roi)
    
    val profitPercentage = if (vehicle.totalIncome > 0) {
        (vehicle.netProfit / vehicle.totalIncome).toFloat()
    } else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (profitPercentage > 0) profitPercentage else 0f,
        animationSpec = tween(1000),
        label = "profit_progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (vehicle.roi > 50) {
                AnalyticsUtils.getAlphaColor(AnalyticsUtils.Colors.SUCCESS)
            } else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Vehicle name and ROI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.vehicleName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = roiColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = AnalyticsUtils.formatDecimal(vehicle.roi) + "%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = roiColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Financial metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinancialMetric(
                    label = "Income",
                    value = AnalyticsUtils.formatCurrency(vehicle.totalIncome),
                    color = AnalyticsUtils.Colors.SUCCESS
                )
                FinancialMetric(
                    label = "Expenses",
                    value = AnalyticsUtils.formatCurrency(vehicle.totalExpenses),
                    color = AnalyticsUtils.Colors.ERROR
                )
                FinancialMetric(
                    label = "Net Profit",
                    value = AnalyticsUtils.formatCurrency(vehicle.netProfit),
                    color = if (isProfit) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Profit margin bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Profit Margin",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatDecimal(profitPercentage * 100) + "%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = roiColor
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(3.dp)
                        )
                ) {
                    if (animatedProgress > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(
                                    color = roiColor,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }
            
            // ROI interpretation
            if (vehicle.roi != 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = roiColor.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = AnalyticsUtils.getROIInterpretation(vehicle.roi),
                        style = MaterialTheme.typography.bodySmall,
                        color = roiColor,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialMetric(
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
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun VehicleROISummary(vehicleROI: List<VehicleROI>) {
    val totalIncome = vehicleROI.sumOf { it.totalIncome }
    val totalExpenses = vehicleROI.sumOf { it.totalExpenses }
    val totalProfit = totalIncome - totalExpenses
    val averageROI = if (vehicleROI.isNotEmpty()) {
        vehicleROI.sumOf { it.roi } / vehicleROI.size
    } else 0.0
    
    val bestPerformer = vehicleROI.maxByOrNull { it.roi }
    val worstPerformer = vehicleROI.minByOrNull { it.roi }
    
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
                text = "Fleet Summary",
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
                        text = "Total Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalIncome),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnalyticsUtils.Colors.SUCCESS
                    )
                }
                
                Column {
                    Text(
                        text = "Total Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalExpenses),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnalyticsUtils.Colors.ERROR
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Net Profit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatCurrency(totalProfit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalProfit >= 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR
                    )
                }
                
                Column {
                    Text(
                        text = "Average ROI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsUtils.formatDecimal(averageROI) + "%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AnalyticsUtils.getROIColor(averageROI)
                    )
                }
            }
            
            if (bestPerformer != null && worstPerformer != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Best Performer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${bestPerformer.vehicleName.take(15)}...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = AnalyticsUtils.formatDecimal(bestPerformer.roi) + "% ROI",
                            style = MaterialTheme.typography.bodySmall,
                            color = AnalyticsUtils.Colors.SUCCESS
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Needs Attention",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${worstPerformer.vehicleName.take(15)}...",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = AnalyticsUtils.formatDecimal(worstPerformer.roi) + "% ROI",
                            style = MaterialTheme.typography.bodySmall,
                            color = AnalyticsUtils.Colors.ERROR
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVehicleROIState() {
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
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No vehicle data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add entries and expenses to see ROI analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// REFACTOR: getROIInterpretation moved to AnalyticsUtils.getROIInterpretation

enum class VehicleROISortOption(val displayName: String) {
    ROI("ROI %"),
    NET_PROFIT("Profit"),
    INCOME("Income"),
    EXPENSES("Expenses"),
    ALPHABETICAL("A-Z")
}