@file:OptIn(ExperimentalFoundationApi::class)

package com.fleetmanager.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.ui.screens.analytics.AnalyticsScreen
import com.fleetmanager.ui.screens.auth.SignInScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.AddEntryScreen
import com.fleetmanager.ui.screens.entry.EntryDetailScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.entry.NewExpenseEntryScreen
import com.fleetmanager.ui.screens.profile.ProfileScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen
import com.fleetmanager.ui.screens.splash.SplashScreen
import com.fleetmanager.ui.screens.vehicles.VehicleManagementScreen
import com.fleetmanager.ui.viewmodel.NavigationViewModel as UserNavigationViewModel

/**
 * Clean Navigation Routes
 */
sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Splash : Screen("splash")
    object SignIn : Screen("sign_in")
    object Dashboard : Screen("dashboard")
    object History : Screen("history")
    object Analytics : Screen("analytics")
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object AddEntry : Screen("add_entry")
    object AddExpense : Screen("add_expense")
    object Vehicles : Screen("vehicles")
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
    object EditEntry : Screen("edit_entry/{entryId}") {
        fun createRoute(entryId: String) = "edit_entry/$entryId"
    }
    object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: String) = "edit_expense/$expenseId"
    }
}

/**
 * Bottom Navigation Item
 */
data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

/**
 * All available bottom navigation items
 */
val allBottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Default.Dashboard),
    BottomNavItem(Screen.History, "History", Icons.Default.History),
    BottomNavItem(Screen.Analytics, "Analytics", Icons.Default.Analytics),
    BottomNavItem(Screen.Reports, "Reports", Icons.Default.Assessment),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

/**
 * Get bottom navigation items based on user role
 */
fun getBottomNavItemsForRole(userRole: UserRole): List<BottomNavItem> {
    return allBottomNavItems.filter { navItem ->
        when (navItem.screen) {
            Screen.Analytics -> PermissionManager.canAccessAnalytics(userRole)
            Screen.Reports -> PermissionManager.canAccessReports(userRole)
            else -> true // Dashboard, History, Settings are available to all roles
        }
    }
}

/**
 * Main App Navigation Component
 * Clean, simple, centralized
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    isSignedIn: Boolean
) {
    var showSplash by remember { mutableStateOf(true) }
    
    if (showSplash) {
        SplashScreen(
            onSplashComplete = { showSplash = false }
        )
    } else {
        if (isSignedIn) {
            MainNavigation(navController = navController)
        } else {
            SignInOnlyNavigation(navController = navController)
        }
    }
}

/**
 * Main Navigation for Signed-in Users
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainNavigation(
    navController: NavHostController
) {
    // Get user role for bottom nav filtering
    val userNavigationViewModel: UserNavigationViewModel = hiltViewModel()
    val userRole by userNavigationViewModel.userRole.collectAsState()
    val bottomNavItems = userRole?.let { getBottomNavItemsForRole(it) } ?: allBottomNavItems
    
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        // Single main route hosting pager + bottom bar (no NavController tab navigation)
        composable(Screen.Main.route) {
            MainScreen(
                bottomNavItems = bottomNavItems,
                onAddEntryClick = { navController.navigate(Screen.AddEntry.route) },
                onAddExpenseClick = { navController.navigate(Screen.AddExpense.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onEntryClick = { entryId -> navController.navigate(Screen.EntryDetail.createRoute(entryId)) },
                onEditEntry = { entryId -> navController.navigate(Screen.EditEntry.createRoute(entryId)) },
                onEditExpense = { expenseId -> navController.navigate(Screen.EditExpense.createRoute(expenseId)) },
                onManageVehicles = { navController.navigate(Screen.Vehicles.route) }
            )
        }
        
        // Secondary screens
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AddExpense.route) {
            NewExpenseEntryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            EntryDetailScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() },
                onEditEntry = { navController.navigate(Screen.EditEntry.createRoute(entryId)) }
            )
        }

        composable(
            route = Screen.EditEntry.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            AddEntryScreen(
                entryId = entryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            NewExpenseEntryScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Vehicles.route) {
            VehicleManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

    }
}

/**
 * Sign-in Only Navigation
 */
@Composable
private fun SignInOnlyNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.SignIn.route
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }
    }
}