package com.fleetmanager.ui.navigation

/**
 * Dashboard shortcuts that map statistic tiles to report filters.
 */
sealed class DashboardShortcut {
    sealed class TimeRange : DashboardShortcut() {
        object Last24Hours : TimeRange()
        object ThisWeek : TimeRange()
        object ThisMonth : TimeRange()
        object LastMonth : TimeRange()
    }

    data class Provider(val name: String) : DashboardShortcut()

    object AllEntries : DashboardShortcut()
}
