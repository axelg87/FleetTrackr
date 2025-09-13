package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.domain.model.ExpenseType
import com.fleetmanager.ui.screens.analytics.model.ExpenseBreakdown
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsCalculator
import kotlin.math.*

/**
 * Expense Deep Dive component showing detailed expense analysis with pie chart and breakdowns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDeepDive(
    expenseBreakdown: List<ExpenseBreakdown>,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(ExpenseViewMode.PIE_CHART) }
    
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
                        text = "Expense Deep Dive",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Breakdown of expenses by category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (expenseBreakdown.isEmpty()) {
                EmptyExpenseState()
            } else {
                // View mode selector
                ExpenseViewModeSelector(
                    selectedMode = viewMode,
                    onModeSelected = { viewMode = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when (viewMode) {
                    ExpenseViewMode.PIE_CHART -> {
                        ExpensePieChart(expenseBreakdown = expenseBreakdown)
                    }
                    ExpenseViewMode.BAR_CHART -> {
                        ExpenseBarChart(expenseBreakdown = expenseBreakdown)
                    }
                    ExpenseViewMode.LIST_VIEW -> {
                        ExpenseListView(expenseBreakdown = expenseBreakdown)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Summary
                ExpenseSummary(expenseBreakdown = expenseBreakdown)
            }
        }
    }
}

@Composable
private fun ExpenseViewModeSelector(
    selectedMode: ExpenseViewMode,
    onModeSelected: (ExpenseViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ExpenseViewMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            
            TextButton(
                onClick = { onModeSelected(mode) },
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
                    text = mode.displayName,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ExpensePieChart(expenseBreakdown: List<ExpenseBreakdown>) {
    val totalAmount = expenseBreakdown.sumOf { it.totalAmount }
    
    Column {
        // Pie chart
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawPieChart(
                    expenseData = expenseBreakdown,
                    totalAmount = totalAmount,
                    canvasSize = size
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        ExpenseLegend(expenseBreakdown = expenseBreakdown)
    }
}

private fun DrawScope.drawPieChart(
    expenseData: List<ExpenseBreakdown>,
    totalAmount: Double,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val center = canvasSize.center
    val radius = minOf(canvasSize.width, canvasSize.height) / 2 * 0.8f
    
    var startAngle = -90f // Start from top
    
    expenseData.forEach { expense ->
        val sweepAngle = if (totalAmount > 0) {
            ((expense.totalAmount / totalAmount) * 360).toFloat()
        } else 0f
        
        val color = getExpenseTypeColor(expense.expenseType)
        
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
        )
        
        // Draw stroke between segments
        drawArc(
            color = Color.White,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 2.dp.toPx())
        )
        
        startAngle += sweepAngle
    }
    
    // Draw center circle for donut effect
    drawCircle(
        color = Color.White,
        radius = radius * 0.4f,
        center = center
    )
}

@Composable
private fun ExpenseBarChart(expenseBreakdown: List<ExpenseBreakdown>) {
    val maxAmount = expenseBreakdown.maxOfOrNull { it.totalAmount } ?: 1.0
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        expenseBreakdown.forEach { expense ->
            ExpenseBar(
                expense = expense,
                maxAmount = maxAmount
            )
        }
    }
}

@Composable
private fun ExpenseBar(
    expense: ExpenseBreakdown,
    maxAmount: Double
) {
    val progressPercentage = if (maxAmount > 0) (expense.totalAmount / maxAmount).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1000),
        label = "expense_progress"
    )
    
    val expenseColor = getExpenseTypeColor(expense.expenseType)
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = expense.expenseType.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.3f)
            )
            
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
                            color = expenseColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = AnalyticsCalculator.formatCurrency(expense.totalAmount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Text(
                text = AnalyticsCalculator.formatPercentage(expense.percentage),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = expenseColor,
                modifier = Modifier.weight(0.2f)
            )
        }
        
        Text(
            text = "${expense.count} transactions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ExpenseListView(expenseBreakdown: List<ExpenseBreakdown>) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenseBreakdown) { expense ->
            ExpenseListItem(expense = expense)
        }
    }
}

@Composable
private fun ExpenseListItem(expense: ExpenseBreakdown) {
    val expenseColor = getExpenseTypeColor(expense.expenseType)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = expenseColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(expenseColor, CircleShape)
                )
                
                Text(
                    text = expense.expenseType.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = AnalyticsCalculator.formatCurrency(expense.totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = expenseColor
                )
                Text(
                    text = "${AnalyticsCalculator.formatPercentage(expense.percentage)} • ${expense.count} transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseLegend(expenseBreakdown: List<ExpenseBreakdown>) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(expenseBreakdown) { expense ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(getExpenseTypeColor(expense.expenseType), CircleShape)
                    )
                    
                    Text(
                        text = expense.expenseType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = "${AnalyticsCalculator.formatPercentage(expense.percentage)} (${AnalyticsCalculator.formatCurrency(expense.totalAmount)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseSummary(expenseBreakdown: List<ExpenseBreakdown>) {
    val totalAmount = expenseBreakdown.sumOf { it.totalAmount }
    val totalTransactions = expenseBreakdown.sumOf { it.count }
    val averageTransaction = if (totalTransactions > 0) totalAmount / totalTransactions else 0.0
    val mostExpensiveCategory = expenseBreakdown.maxByOrNull { it.totalAmount }
    val mostFrequentCategory = expenseBreakdown.maxByOrNull { it.count }
    
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
                text = "Expense Summary",
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
                        text = "Total Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsCalculator.formatCurrency(totalAmount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                
                Column {
                    Text(
                        text = "Total Transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalTransactions.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = "Avg Transaction",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AnalyticsCalculator.formatCurrency(averageTransaction),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (mostExpensiveCategory != null && mostFrequentCategory != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Highest Cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = mostExpensiveCategory.expenseType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = AnalyticsCalculator.formatCurrency(mostExpensiveCategory.totalAmount),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                    }
                    
                    Column {
                        Text(
                            text = "Most Frequent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = mostFrequentCategory.expenseType.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${mostFrequentCategory.count} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyExpenseState() {
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
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No expense data available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Add some expense entries to see breakdown",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getExpenseTypeColor(expenseType: ExpenseType): Color {
    return when (expenseType) {
        ExpenseType.FUEL -> Color(0xFF2196F3) // Blue
        ExpenseType.SERVICE -> Color(0xFF4CAF50) // Green
        ExpenseType.CAR_WASH -> Color(0xFF00BCD4) // Cyan
        ExpenseType.FINE -> Color(0xFFF44336) // Red
        ExpenseType.MAINTENANCE -> Color(0xFFFF9800) // Orange
        ExpenseType.OTHER -> Color(0xFF9C27B0) // Purple
    }
}

enum class ExpenseViewMode(val displayName: String) {
    PIE_CHART("Pie Chart"),
    BAR_CHART("Bar Chart"),
    LIST_VIEW("List View")
}