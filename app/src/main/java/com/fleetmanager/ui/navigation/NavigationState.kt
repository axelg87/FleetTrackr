package com.fleetmanager.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Centralized Navigation State Management
 * Single source of truth for navigation state
 */
@Stable
class NavigationState(
    val navController: NavHostController
) {
    
    /**
     * Navigate to a screen by route
     * Simple, clean navigation without complex state management
     */
    fun navigateTo(route: String) {
        navController.navigate(route) {
            // Standard navigation behavior
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    
    /**
     * Navigate back
     */
    fun navigateBack() {
        navController.popBackStack()
    }
    
    /**
     * Get current route
     */
    val currentRoute: String?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination?.route
}

/**
 * Remember navigation state
 */
@Composable
fun rememberNavigationState(
    navController: NavHostController
): NavigationState {
    return remember(navController) {
        NavigationState(navController)
    }
}