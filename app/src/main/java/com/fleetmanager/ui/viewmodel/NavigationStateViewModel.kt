package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.fleetmanager.ui.navigation.NavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel wrapper for NavigationState singleton
 * Provides proper lifecycle management for the navigation state
 */
@HiltViewModel
class NavigationStateViewModel @Inject constructor(
    val navigationState: NavigationState
) : ViewModel()