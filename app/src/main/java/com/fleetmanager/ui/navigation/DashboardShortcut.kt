package com.fleetmanager.ui.navigation

/**
 * Dashboard shortcuts that map statistic tiles to report filters.
 */
sealed class DashboardShortcut {
    sealed class TimeRange : DashboardShortcut() {
        object Last24Hours : TimeRange()
        object ThisWeek : TimeRange()
        object ThisMonth : TimeRange()
    }

    sealed class IncomeSource : DashboardShortcut() {
        object Uber : IncomeSource()
        object Yango : IncomeSource()
        object Private : IncomeSource()
    }

    object AllEntries : DashboardShortcut()
}
