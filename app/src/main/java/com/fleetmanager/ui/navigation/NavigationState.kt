package com.fleetmanager.ui.navigation

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized pager navigation state.
 * Single source of truth for the current main page index.
 */
@Stable
object NavigationState {
	private val _currentPageIndex = MutableStateFlow(0)
	val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

	fun setCurrentPage(index: Int) {
		if (index != _currentPageIndex.value) {
			_currentPageIndex.value = index
		}
	}
}