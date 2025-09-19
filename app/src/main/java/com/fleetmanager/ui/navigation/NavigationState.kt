package com.fleetmanager.ui.navigation

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized pager navigation state.
 * Single source of truth for the current main page index.
 */
@Stable
object NavigationState {
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _reportShortcuts = MutableSharedFlow<DashboardShortcut>(extraBufferCapacity = 1)
    val reportShortcuts: SharedFlow<DashboardShortcut> = _reportShortcuts.asSharedFlow()

    fun setCurrentPage(index: Int) {
        if (index != _currentPageIndex.value) {
            _currentPageIndex.value = index
        }
    }

    suspend fun emitReportShortcut(shortcut: DashboardShortcut) {
        _reportShortcuts.emit(shortcut)
    }
}