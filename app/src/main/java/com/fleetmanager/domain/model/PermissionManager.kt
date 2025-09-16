package com.fleetmanager.domain.model

/**
 * Centralized permission manager for role-based access control.
 * All role-based UI and business logic decisions should go through this class.
 */
object PermissionManager {
    
    // Core permissions
    fun canEdit(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canDelete(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canViewAll(userRole: UserRole): Boolean = userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
    
    // UI-specific permissions
    fun canAccessReports(userRole: UserRole): Boolean = userRole != UserRole.DRIVER
    fun canAccessAnalytics(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canSeeAdminControls(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    
    // Data management permissions
    fun canCreateDrivers(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canCreateVehicles(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canCreateExpenseTypes(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    
    // Report filtering permissions
    fun canViewAllDriverData(userRole: UserRole): Boolean = userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
    fun canViewAllVehicleData(userRole: UserRole): Boolean = userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
    
    // Navigation permissions
    fun getAvailableScreens(userRole: UserRole): Set<String> {
        return when (userRole) {
            UserRole.DRIVER -> setOf("dashboard", "history", "settings")
            UserRole.MANAGER -> setOf("dashboard", "history", "reports", "settings")
            UserRole.ADMIN -> setOf("dashboard", "history", "analytics", "reports", "settings")
        }
    }
}