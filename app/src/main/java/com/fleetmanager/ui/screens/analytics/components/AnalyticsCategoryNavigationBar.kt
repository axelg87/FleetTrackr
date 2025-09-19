package com.fleetmanager.ui.screens.analytics.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fleetmanager.ui.screens.analytics.model.AnalyticsCategory

@Composable
fun AnalyticsCategoryNavigationBar(
    selectedCategory: AnalyticsCategory,
    onCategorySelected: (AnalyticsCategory) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<AnalyticsCategory> = AnalyticsCategory.values().toList()
) {
    NavigationBar(
        modifier = modifier
    ) {
        categories.forEach { category ->
            NavigationBarItem(
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) },
                icon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.displayName
                    )
                },
                label = {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
