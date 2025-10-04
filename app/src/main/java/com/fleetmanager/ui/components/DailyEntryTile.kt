package com.fleetmanager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleetmanager.domain.model.DailyEntry
import androidx.compose.ui.res.stringResource
import com.fleetmanager.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Centralised tile used to render daily entries across the application (Dashboard & History).
 */
@Composable
fun DailyEntryTile(
    entry: DailyEntry,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    showDeleteButton: Boolean = false,
    showEditButton: Boolean = false,
    onEdit: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (() -> Unit)? = null
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    ListItemCard(
        onClick = {
            if (selectionMode) {
                onSelectionChange?.invoke()
            } else {
                onClick()
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionChange?.invoke() },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .align(Alignment.Top)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.driverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.vehicle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormatter.format(entry.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entry.odometer?.let { odometer ->
                        Text(
                            text = stringResource(
                                R.string.odometer_reading,
                                formatOdometer(odometer)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "AED ${String.format("%.2f", entry.totalEarnings)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!entry.isSynced) {
                            Text(
                                text = "⏳ Syncing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    if (showEditButton && onEdit != null && !selectionMode) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit entry",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showDeleteButton && onDelete != null && !selectionMode) {
                        IconButton(
                            onClick = { onDelete() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (entry.notes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val earningItems = entry.earnings
            .filter { it.totalAmount > 0 }
            .map { earning ->
                EarningItem(
                    label = earning.provider.ifBlank { "Other" },
                    amount = earning.totalAmount
                )
            }

        if (earningItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            EarningsChips(earnings = earningItems)
        }
    }
}

private fun formatOdometer(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}
