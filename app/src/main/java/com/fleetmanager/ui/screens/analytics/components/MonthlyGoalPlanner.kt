package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fleetmanager.ui.screens.analytics.model.ProjectionData
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils
import kotlin.math.abs

/**
 * Monthly revenue goal planner card.
 * Allows the user to enter a target amount and understand the pace required to reach it.
 */
@Composable
fun MonthlyGoalPlanner(
    projectionData: ProjectionData?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var targetInput by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Monthly Goal Planner",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Set a revenue goal for the current month and see how you're pacing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = targetInput,
                onValueChange = { newValue ->
                    targetInput = newValue
                        .replace(",", "")
                        .filter { it.isDigit() || it == '.' }
                },
                label = { Text("Monthly revenue goal") },
                placeholder = { Text("15000") },
                leadingIcon = { Text(text = "AED") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            val targetAmount = targetInput.toDoubleOrNull() ?: 0.0
            val currentTotal = projectionData?.currentMonthTotal ?: 0.0
            val projectedTotal = projectionData?.projectedMonthTotal ?: currentTotal
            val totalDays = projectionData?.totalDaysInMonth ?: 0
            val daysElapsed = projectionData?.daysElapsed ?: 0
            val remainingDays = (totalDays - daysElapsed).coerceAtLeast(0)
            val remainingAmountRaw = targetAmount - currentTotal
            val remainingAmount = remainingAmountRaw.coerceAtLeast(0.0)
            val requiredDailyAverage = when {
                remainingAmount <= 0.0 -> 0.0
                remainingDays > 0 -> remainingAmount / remainingDays
                else -> remainingAmount
            }
            val progress = if (targetAmount > 0) {
                (currentTotal / targetAmount).coerceIn(0.0, 1.0)
            } else 0.0
            val projectedGap = targetAmount - projectedTotal

            Spacer(modifier = Modifier.height(16.dp))

            if (targetAmount <= 0.0) {
                Text(
                    text = "Enter a goal to calculate how much revenue you still need and the daily pace required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    progress = progress.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "AED{AnalyticsUtils.formatCurrency(currentTotal)} of AED{AnalyticsUtils.formatCurrency(targetAmount)} (AED{AnalyticsUtils.formatPercentage(progress * 100)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                GoalStatRow(
                    primaryLabel = "Current total",
                    primaryValue = AnalyticsUtils.formatCurrency(currentTotal),
                    secondaryLabel = "Goal amount",
                    secondaryValue = AnalyticsUtils.formatCurrency(targetAmount)
                )

                GoalStatRow(
                    primaryLabel = "Amount left",
                    primaryValue = if (remainingAmount > 0) {
                        AnalyticsUtils.formatCurrency(remainingAmount)
                    } else {
                        "Goal reached"
                    },
                    primaryValueColor = if (remainingAmount > 0) {
                        AnalyticsUtils.Colors.ERROR
                    } else {
                        AnalyticsUtils.Colors.SUCCESS
                    },
                    secondaryLabel = if (remainingDays > 0) "Needed per day" else "Needed today",
                    secondaryValue = AnalyticsUtils.formatCurrency(requiredDailyAverage),
                    secondaryValueColor = if (remainingAmount > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        AnalyticsUtils.Colors.SUCCESS
                    }
                )

                if (projectionData != null) {
                    GoalStatRow(
                        primaryLabel = "Remaining days",
                        primaryValue = AnalyticsUtils.formatWholeNumber(remainingDays.toDouble()),
                        secondaryLabel = "Projected total",
                        secondaryValue = AnalyticsUtils.formatCurrency(projectedTotal)
                    )

                    GoalStatRow(
                        primaryLabel = if (projectedGap <= 0) "Projected surplus" else "Projected shortfall",
                        primaryValue = AnalyticsUtils.formatCurrency(abs(projectedGap)),
                        primaryValueColor = if (projectedGap <= 0) AnalyticsUtils.Colors.SUCCESS else AnalyticsUtils.Colors.ERROR,
                        secondaryLabel = "Days elapsed",
                        secondaryValue = AnalyticsUtils.formatWholeNumber(daysElapsed.toDouble()),
                        secondaryValueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val statusMessage = when {
                    remainingAmount <= 0.0 -> "Great job! You're already ahead of this month's goal."
                    remainingDays == 0 -> "Goal deadline is here. Every additional AED{AnalyticsUtils.formatCurrency(remainingAmount)} gets you to the finish line."
                    else -> "You need AED{AnalyticsUtils.formatCurrency(requiredDailyAverage)} per day for the next AEDremainingDays days to hit your goal."
                }

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remainingAmount <= 0.0) AnalyticsUtils.Colors.SUCCESS else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GoalStatRow(
    primaryLabel: String,
    primaryValue: String,
    secondaryLabel: String? = null,
    secondaryValue: String? = null,
    primaryValueColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryValueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GoalStatItem(label = primaryLabel, value = primaryValue, valueColor = primaryValueColor, modifier = Modifier.weight(1f))
            if (secondaryLabel != null && secondaryValue != null) {
                Spacer(modifier = Modifier.width(16.dp))
                GoalStatItem(label = secondaryLabel, value = secondaryValue, valueColor = secondaryValueColor, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GoalStatItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
