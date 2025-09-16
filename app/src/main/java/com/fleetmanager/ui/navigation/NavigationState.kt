package com.fleetmanager.ui.navigation

import com.fleetmanager.ui.model.FilterContext

/**
 * Simple navigation state holder for passing filter context between screens.
 * This is a temporary solution - in a production app, you might want to use
 * SavedStateHandle or a more sophisticated navigation argument system.
 */
object NavigationState {
    private var pendingFilterContext: FilterContext? = null
    
    fun setPendingFilterContext(filterContext: FilterContext) {
        pendingFilterContext = filterContext
    }
    
    fun consumePendingFilterContext(): FilterContext? {
        val context = pendingFilterContext
        pendingFilterContext = null // Clear after consumption
        return context
    }
}