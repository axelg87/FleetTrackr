package com.fleetmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.fleetmanager.ui.navigation.FleetNavigation
import com.fleetmanager.ui.navigation.Screen
import com.fleetmanager.ui.theme.FleetManagerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            FleetManagerTheme {
                val navController = rememberNavController()
                // Bypass signin screen for now - go directly to EntryList
                val startDestination = Screen.EntryList.route
                
                FleetNavigation(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}