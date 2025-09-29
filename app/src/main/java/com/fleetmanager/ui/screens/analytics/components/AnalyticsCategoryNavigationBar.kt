package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleetmanager.ui.screens.analytics.model.AnalyticsCategory

private const val DEFAULT_SELECTED_INDEX = 0

@Composable
fun AnalyticsCategoryNavigationBar(
    selectedCategory: AnalyticsCategory,
    onCategorySelected: (AnalyticsCategory) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<AnalyticsCategory> = AnalyticsCategory.values().toList()
) {
    val selectedIndex = categories.indexOf(selectedCategory).takeIf { it >= 0 }
        ?: DEFAULT_SELECTED_INDEX

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty() && selectedIndex < tabPositions.size) {
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        divider = {}
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                icon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.displayName
                    )
                }
            )
        }
    }
}
