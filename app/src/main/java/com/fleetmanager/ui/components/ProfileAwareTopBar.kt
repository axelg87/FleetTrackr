package com.fleetmanager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.ProfileViewModel

/**
 * A top app bar that automatically displays the current user's profile information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAwareTopBar(
    title: String,
    onProfileClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Create a dedicated ProfileViewModel to ensure we get fresh user data
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileState by profileViewModel.uiState.collectAsState()
    
    FleetTopAppBar(
        title = title,
        profilePictureUrl = profileState.userProfile?.profilePictureUrl,
        profileInitials = profileState.userProfile?.initials ?: "U",
        onProfileClick = onProfileClick,
        actions = actions,
        modifier = modifier
    )
}