package com.fleetmanager.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.screens.analytics.components.CalendarView

/**
 * Analytics Screen with modular structure for future expansion.
 * Currently contains only CalendarView but designed to be scalable
 * for adding graphs, stats cards, tabs, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Future: This is where we can add tabs for different analytics views
        // TabRow or segmented buttons can go here
        
        // Calendar Section
        AnalyticsSection(
            title = "Calendar Overview",
            description = "Daily earnings visualization"
        ) {
            CalendarView(
                entriesData = uiState.entriesData,
                isLoading = uiState.isLoading,
                onDayClick = { date, entries ->
                    viewModel.onDaySelected(date, entries)
                }
            )
        }
        
        // Future sections can be added here:
        // - Stats Cards Section
        // - Charts Section (earnings trends, driver performance, etc.)
        // - Insights Section
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Handle day selection dialog
    if (uiState.selectedDayEntries != null) {
        DayEntriesDialog(
            date = uiState.selectedDate,
            entries = uiState.selectedDayEntries,
            onDismiss = { viewModel.clearDaySelection() }
        )
    }
}

/**
 * Reusable component for analytics sections.
 * This provides consistent styling and makes it easy to add new sections.
 */
@Composable
private fun AnalyticsSection(
    title: String,
    description: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            content()
        }
    }
}