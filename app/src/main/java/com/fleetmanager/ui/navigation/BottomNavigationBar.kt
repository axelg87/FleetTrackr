package com.fleetmanager.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom Navigation Bar Component
 * 
 * Observes NavigationStateManager's currentPageIndex StateFlow and updates accordingly.
 * Handles click events by calling NavigationStateManager.onBottomNavClick().
 * 
 * Clean separation of concerns:
 * - Only responsible for UI rendering and user interaction
 * - State management delegated to NavigationStateManager
 * - No direct navigation logic
 */
@Composable
fun BottomNavigationBar(
    navigationStateManager: NavigationStateManager,
    bottomNavItems: List<BottomNavItem>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val currentPageIndex by navigationStateManager.currentPageIndex.collectAsState()
    
    NavigationBar(modifier = modifier) {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentPageIndex == index,
                onClick = { 
                    navigationStateManager.onBottomNavClick(item.screen.route)
                }
            )
        }
    }
}
