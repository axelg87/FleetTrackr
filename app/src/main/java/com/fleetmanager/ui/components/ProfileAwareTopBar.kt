package com.fleetmanager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.ui.viewmodel.NavigationViewModel

/**
 * A top app bar that automatically displays the current user's profile information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAwareTopBar(
    title: String,
    onProfileClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel = hiltViewModel()
) {
    val userProfile by navigationViewModel.userProfile.collectAsState()
    
    FleetTopAppBar(
        title = title,
        profilePictureUrl = userProfile?.profilePictureUrl,
        profileInitials = userProfile?.initials ?: "U",
        onProfileClick = onProfileClick,
        actions = actions,
        modifier = modifier
    )
}