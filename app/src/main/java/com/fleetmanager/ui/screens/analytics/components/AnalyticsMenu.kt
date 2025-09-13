package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleetmanager.ui.screens.analytics.model.AnalyticsCategory
import com.fleetmanager.ui.screens.analytics.model.AnalyticsPanel
import com.fleetmanager.ui.screens.analytics.utils.AnalyticsUtils

/**
 * GENERALIZATION: Analytics menu system for panel navigation
 * Provides organized access to all analytics features
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsMenu(
    selectedPanel: AnalyticsPanel?,
    onPanelSelected: (AnalyticsPanel) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<AnalyticsCategory?>(null) }
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Analytics Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedPanel != null) {
                            "Viewing: ${selectedPanel.displayName}"
                        } else {
                            "Select an analytics panel to view"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val rotationAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = tween(300),
                    label = "expand_rotation"
                )
                
                IconButton(
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse menu" else "Expand menu",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick actions
                QuickActions(
                    selectedPanel = selectedPanel,
                    onShowAll = onShowAll,
                    onPanelSelected = onPanelSelected
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category tabs
                CategoryTabs(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Panel grid
                if (selectedCategory != null) {
                    PanelGrid(
                        category = selectedCategory,
                        selectedPanel = selectedPanel,
                        onPanelSelected = onPanelSelected
                    )
                } else {
                    AllPanelsOverview(
                        selectedPanel = selectedPanel,
                        onPanelSelected = onPanelSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    selectedPanel: AnalyticsPanel?,
    onShowAll: () -> Unit,
    onPanelSelected: (AnalyticsPanel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show All button
        OutlinedButton(
            onClick = onShowAll,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (selectedPanel == null) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else Color.Transparent
            )
        ) {
            Text(
                text = "Show All",
                fontSize = 12.sp,
                fontWeight = if (selectedPanel == null) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        // Quick access to popular panels
        listOf(
            AnalyticsPanel.TRENDS,
            AnalyticsPanel.TOP_DRIVERS,
            AnalyticsPanel.VEHICLE_ROI
        ).forEach { panel ->
            OutlinedButton(
                onClick = { onPanelSelected(panel) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedPanel == panel) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    } else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = panel.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: AnalyticsCategory?,
    onCategorySelected: (AnalyticsCategory?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All categories option
        item {
            CategoryTab(
                category = null,
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }
        
        items(AnalyticsCategory.values()) { category ->
            CategoryTab(
                category = category,
                isSelected = selectedCategory == category,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryTab(
    category: AnalyticsCategory?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "tab_color"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "content_color"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = category?.icon ?: Icons.Default.Dashboard,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = category?.displayName ?: "All",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PanelGrid(
    category: AnalyticsCategory,
    selectedPanel: AnalyticsPanel?,
    onPanelSelected: (AnalyticsPanel) -> Unit
) {
    val panels = AnalyticsPanel.getByCategory(category)
    
    LazyColumn(
        modifier = Modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(panels.chunked(2)) { panelRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                panelRow.forEach { panel ->
                    PanelCard(
                        panel = panel,
                        isSelected = selectedPanel == panel,
                        onClick = { onPanelSelected(panel) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Fill remaining space if odd number of panels
                if (panelRow.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AllPanelsOverview(
    selectedPanel: AnalyticsPanel?,
    onPanelSelected: (AnalyticsPanel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(AnalyticsCategory.values()) { category ->
            CategorySection(
                category = category,
                selectedPanel = selectedPanel,
                onPanelSelected = onPanelSelected
            )
        }
    }
}

@Composable
private fun CategorySection(
    category: AnalyticsCategory,
    selectedPanel: AnalyticsPanel?,
    onPanelSelected: (AnalyticsPanel) -> Unit
) {
    Column {
        // Category header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Panels in this category
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AnalyticsPanel.getByCategory(category)) { panel ->
                CompactPanelCard(
                    panel = panel,
                    isSelected = selectedPanel == panel,
                    onClick = { onPanelSelected(panel) }
                )
            }
        }
    }
}

@Composable
private fun PanelCard(
    panel: AnalyticsPanel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "panel_bg"
    )
    
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = panel.icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = panel.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = panel.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun CompactPanelCard(
    panel: AnalyticsPanel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "compact_bg"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "compact_content"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = panel.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = panel.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}