package com.fleetmanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.fleetmanager.ui.screens.auth.SignInScreen
import com.fleetmanager.ui.screens.dashboard.DashboardScreen
import com.fleetmanager.ui.screens.entry.AddEntryScreen
import com.fleetmanager.ui.screens.entry.EntryDetailScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen
import com.fleetmanager.ui.screens.entry.NewExpenseEntryScreen
import com.fleetmanager.ui.screens.report.ReportScreen
import com.fleetmanager.ui.screens.settings.SettingsScreen
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.PermissionManager
import com.fleetmanager.ui.viewmodel.NavigationViewModel
import androidx.hilt.navigation.compose.hiltViewModel

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Dashboard : Screen("dashboard")
    object History : Screen("history") // This will be the EntryList screen
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object AddEntry : Screen("add_entry")
    object AddExpense : Screen("add_expense")
    object EntryDetail : Screen("entry_detail/{entryId}") {
        fun createRoute(entryId: String) = "entry_detail/$entryId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: ImageVector
)

val allBottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard, "Dashboard", Icons.Default.Dashboard),
    BottomNavItem(Screen.History, "History", Icons.Default.History),
    BottomNavItem(Screen.Reports, "Reports", Icons.Default.Assessment),
    BottomNavItem(Screen.Settings, "Settings", Icons.Default.Settings)
)

fun getBottomNavItemsForRole(userRole: UserRole): List<BottomNavItem> {
    return allBottomNavItems.filter { navItem ->
        when (navItem.screen) {
            Screen.Reports -> PermissionManager.canAccessReports(userRole)
            else -> true // Dashboard, History, Settings are available to all roles
        }
    }
}


@Composable
fun AppNavigation(
    navController: NavHostController,
    isSignedIn: Boolean
) {
    if (isSignedIn) {
        MainScreenWithBottomNav(navController = navController)
    } else {
        // Show only the sign-in screen when not authenticated
        FleetNavigation(
            navController = navController,
            startDestination = Screen.SignIn.route,
            modifier = Modifier
        )
    }
}

@Composable
fun MainScreenWithBottomNav(
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Create a ViewModel to get user role
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val userRole by navigationViewModel.userRole.collectAsState()
    
    val bottomNavItems = userRole?.let { getBottomNavItemsForRole(it) } ?: allBottomNavItems
    
    Scaffold(
        bottomBar = {
            if (shouldShowBottomNav(currentRoute, bottomNavItems)) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    bottomNavItems = bottomNavItems,
                    onNavigate = { route ->
                        navigateToBottomNavDestination(navController, route)
                    }
                )
            }
        }
    ) { innerPadding ->
        FleetNavigation(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    bottomNavItems: List<BottomNavItem>,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentRoute == item.screen.route,
                onClick = { onNavigate(item.screen.route) }
            )
        }
    }
}

private fun shouldShowBottomNav(currentRoute: String?, bottomNavItems: List<BottomNavItem>): Boolean {
    return currentRoute in bottomNavItems.map { it.screen.route }
}

private fun navigateToBottomNavDestination(
    navController: NavHostController,
    route: String
) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun FleetNavigation(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddEntryClick = {
                    navController.navigate(Screen.AddEntry.route)
                },
                onAddExpenseClick = {
                    navController.navigate(Screen.AddExpense.route)
                }
            )
        }
        
        composable(Screen.History.route) {
            EntryListScreen(
                onAddEntryClick = {
                    navController.navigate(Screen.AddEntry.route)
                },
                onAddExpenseClick = {
                    navController.navigate(Screen.AddExpense.route)
                },
                onEntryClick = { entryId ->
                    navController.navigate(Screen.EntryDetail.createRoute(entryId))
                }
            )
        }
        
        composable(Screen.Reports.route) {
            ReportScreen()
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        
        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.AddExpense.route) {
            NewExpenseEntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EntryDetail.route,
            arguments = listOf(navArgument("entryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
            EntryDetailScreen(
                entryId = entryId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}