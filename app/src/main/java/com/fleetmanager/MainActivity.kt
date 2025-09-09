package com.fleetmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.fleetmanager.auth.AuthService
import com.fleetmanager.ui.navigation.FleetNavigation
import com.fleetmanager.ui.navigation.Screen
import com.fleetmanager.ui.theme.FleetManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authService: AuthService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FleetManagerTheme {
                val navController = rememberNavController()
                val isSignedIn by authService.isSignedIn.collectAsState(initial = false)
                
                val startDestination = if (isSignedIn) {
                    Screen.EntryList.route
                } else {
                    Screen.SignIn.route
                }
                
                FleetNavigation(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}