package com.fleetmanager.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.fleetmanager.ui.screens.auth.SignInScreen
import com.fleetmanager.ui.screens.entry.AddEntryScreen
import com.fleetmanager.ui.screens.entry.EntryDetailScreen
import com.fleetmanager.ui.screens.entry.NewExpenseEntryScreen
import com.fleetmanager.ui.screens.profile.ProfileScreen
import com.fleetmanager.ui.screens.splash.SplashScreen
import com.fleetmanager.ui.viewmodel.NavigationStateViewModel

/**
 * Enterprise-grade App Navigation with Single Source of Truth
 * 
 * Uses the new MainScreen for main navigation with StateFlow architecture.
 * Traditional NavController is only used for secondary screens (Profile, AddEntry, etc.)
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
            // Use the new MainScreen with NavigationState for main navigation
            MainAppNavigation()
        } else {
            SignInOnlyNavigation(navController = navController)
        }
    }
}

/**
 * Main App Navigation for Signed-in Users
 * Uses NavigationState singleton and MainScreen for primary navigation
 */
@Composable
private fun MainAppNavigation() {
    // Get the NavigationState singleton via Hilt ViewModel
    val navigationStateViewModel: NavigationStateViewModel = hiltViewModel()
    val navigationState = navigationStateViewModel.navigationState
    
    // Create a separate NavController for secondary screens only
    val secondaryNavController = rememberNavController()
    
    // Track if we're in a secondary screen
    var isInSecondaryScreen by remember { mutableStateOf(false) }
    var secondaryScreenType by remember { mutableStateOf<SecondaryScreen?>(null) }
    
    if (isInSecondaryScreen && secondaryScreenType != null) {
        // Show secondary screen navigation
        NavHost(
            navController = secondaryNavController,
            startDestination = secondaryScreenType!!.route
        ) {
            composable("profile") {
                ProfileScreen(
                    onNavigateBack = { 
                        isInSecondaryScreen = false
                        secondaryScreenType = null
                    }
                )
            }
            
            composable("add_entry") {
                AddEntryScreen(
                    onNavigateBack = { 
                        isInSecondaryScreen = false
                        secondaryScreenType = null
                    }
                )
            }
            
            composable("add_expense") {
                NewExpenseEntryScreen(
                    onNavigateBack = { 
                        isInSecondaryScreen = false
                        secondaryScreenType = null
                    }
                )
            }
            
            composable(
                route = "entry_detail/{entryId}",
                arguments = listOf(navArgument("entryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                EntryDetailScreen(
                    entryId = entryId,
                    onNavigateBack = { 
                        isInSecondaryScreen = false
                        secondaryScreenType = null
                    }
                )
            }
        }
    } else {
        // Show main screen with horizontal swipe navigation
        MainScreen(
            navigationState = navigationState,
            onAddEntryClick = {
                isInSecondaryScreen = true
                secondaryScreenType = SecondaryScreen.AddEntry
                secondaryNavController.navigate("add_entry")
            },
            onAddExpenseClick = {
                isInSecondaryScreen = true
                secondaryScreenType = SecondaryScreen.AddExpense
                secondaryNavController.navigate("add_expense")
            },
            onNavigateToProfile = {
                isInSecondaryScreen = true
                secondaryScreenType = SecondaryScreen.Profile
                secondaryNavController.navigate("profile")
            },
            onEntryClick = { entryId ->
                isInSecondaryScreen = true
                secondaryScreenType = SecondaryScreen.EntryDetail
                secondaryNavController.navigate("entry_detail/$entryId")
            }
        )
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
        startDestination = "sign_in"
    ) {
        composable("sign_in") {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("sign_in") { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Secondary screen types for navigation management
 */
private enum class SecondaryScreen(val route: String) {
    Profile("profile"),
    AddEntry("add_entry"),
    AddExpense("add_expense"),
    EntryDetail("entry_detail")
}