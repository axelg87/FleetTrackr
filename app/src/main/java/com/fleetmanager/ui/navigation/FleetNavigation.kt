package com.fleetmanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fleetmanager.ui.screens.auth.SignInScreen
import com.fleetmanager.ui.screens.entry.AddEntryScreen
import com.fleetmanager.ui.screens.entry.EntryListScreen

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object EntryList : Screen("entry_list")
    object AddEntry : Screen("add_entry")
}

@Composable
fun FleetNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.EntryList.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.EntryList.route) {
            EntryListScreen(
                onAddEntryClick = {
                    navController.navigate(Screen.AddEntry.route)
                }
            )
        }
        
        composable(Screen.AddEntry.route) {
            AddEntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}