package com.fleetmanager.ui.navigation

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
import com.fleetmanager.ui.viewmodel.NavigationViewModel as UserNavigationViewModel

/**
 * Clean Navigation Routes
 */
sealed class Screen(val route: String) {
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
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
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
@Composable
private fun MainNavigation(
    navController: NavHostController
) {
    val navigationState = rememberNavigationState(navController)
    val currentRoute = navigationState.currentRoute
    
    // Get user role for bottom nav filtering
    val userNavigationViewModel: UserNavigationViewModel = hiltViewModel()
    val userRole by userNavigationViewModel.userRole.collectAsState()
    val bottomNavItems = userRole?.let { getBottomNavItemsForRole(it) } ?: allBottomNavItems
    
    Scaffold(
        bottomBar = {
            if (shouldShowBottomNav(currentRoute)) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    bottomNavItems = bottomNavItems,
                    onNavigate = { route -> navigationState.navigateTo(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main tab screens
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddEntryClick = { navigationState.navigateTo(Screen.AddEntry.route) },
                    onAddExpenseClick = { navigationState.navigateTo(Screen.AddExpense.route) },
                    onNavigateToProfile = { navigationState.navigateTo(Screen.Profile.route) },
                    onEntryClick = { entryId -> navigationState.navigateTo(Screen.EntryDetail.createRoute(entryId)) }
                )
            }
            
            composable(Screen.History.route) {
                EntryListScreen(
                    onAddEntryClick = { navigationState.navigateTo(Screen.AddEntry.route) },
                    onAddExpenseClick = { navigationState.navigateTo(Screen.AddExpense.route) },
                    onEntryClick = { entryId -> navigationState.navigateTo(Screen.EntryDetail.createRoute(entryId)) },
                    onNavigateToProfile = { navigationState.navigateTo(Screen.Profile.route) }
                )
            }
            
            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    onNavigateToProfile = { navigationState.navigateTo(Screen.Profile.route) }
                )
            }
            
            composable(Screen.Reports.route) {
                ReportScreen(
                    onNavigateToProfile = { navigationState.navigateTo(Screen.Profile.route) }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToProfile = { navigationState.navigateTo(Screen.Profile.route) }
                )
            }
            
            // Secondary screens
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navigationState.navigateBack() }
                )
            }
            
            composable(Screen.AddEntry.route) {
                AddEntryScreen(
                    onNavigateBack = { navigationState.navigateBack() }
                )
            }
            
            composable(Screen.AddExpense.route) {
                NewExpenseEntryScreen(
                    onNavigateBack = { navigationState.navigateBack() }
                )
            }
            
            composable(
                route = Screen.EntryDetail.route,
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                EntryDetailScreen(
                    entryId = entryId,
                    onNavigateBack = { navigationState.navigateBack() }
                )
            }
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
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Clean Bottom Navigation Bar
 */
@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    bottomNavItems: List<BottomNavItem>,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.screen.route
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = { onNavigate(item.screen.route) }
            )
        }
    }
}

/**
 * Determine if bottom navigation should be shown
 */
private fun shouldShowBottomNav(currentRoute: String?): Boolean {
    return when (currentRoute) {
        Screen.Dashboard.route,
        Screen.History.route,
        Screen.Analytics.route,
        Screen.Reports.route,
        Screen.Settings.route -> true
        else -> false
    }
}