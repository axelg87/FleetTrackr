package com.fleetmanager.domain.model

/**
 * Enum representing different user roles in the fleet management system.
 * 
 * Role Hierarchy and Permissions:
 * - DRIVER: Can only view their own entries/expenses. Cannot edit or delete anything.
 * - MANAGER: Can view all data but can only create entries/expenses for themselves. Cannot edit or delete.
 * - ADMIN: Can view, create, edit, and delete everything.
 */
enum class UserRole(val displayName: String) {
    DRIVER("Driver"),
    MANAGER("Manager"), 
    ADMIN("Admin");
    
    companion object {
        /**
         * Parse UserRole from string value (case-insensitive)
         */
        fun fromString(value: String): UserRole {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                DRIVER // Default to most restrictive role
            }
        }
        
        /**
         * Get UserRole from display name
         */
        fun fromDisplayName(displayName: String): UserRole? {
            return values().find { it.displayName.equals(displayName, ignoreCase = true) }
        }
    }
}

/**
 * Constants for role-based permissions and access control
 */
object RolePermissions {
    
    /**
     * Check if user can view all data (not just their own)
     */
    fun canViewAllData(role: UserRole): Boolean {
        return when (role) {
            UserRole.DRIVER -> false
            UserRole.MANAGER, UserRole.ADMIN -> true
        }
    }
    
    /**
     * Check if user can edit entries/expenses
     */
    fun canEdit(role: UserRole): Boolean {
        return when (role) {
            UserRole.DRIVER, UserRole.MANAGER -> false
            UserRole.ADMIN -> true
        }
    }
    
    /**
     * Check if user can delete entries/expenses
     */
    fun canDelete(role: UserRole): Boolean {
        return when (role) {
            UserRole.DRIVER, UserRole.MANAGER -> false
            UserRole.ADMIN -> true
        }
    }
    
    /**
     * Check if user can create entries/expenses
     */
    fun canCreate(role: UserRole): Boolean {
        return true // All roles can create, but managers/drivers only for themselves
    }
    
    /**
     * Check if user can create entries/expenses for others
     */
    fun canCreateForOthers(role: UserRole): Boolean {
        return when (role) {
            UserRole.DRIVER, UserRole.MANAGER -> false
            UserRole.ADMIN -> true
        }
    }
    
    /**
     * Check if user can manage other users (create, edit user roles, etc.)
     */
    fun canManageUsers(role: UserRole): Boolean {
        return when (role) {
            UserRole.DRIVER, UserRole.MANAGER -> false
            UserRole.ADMIN -> true
        }
    }
    
    /**
     * Get description of role capabilities
     */
    fun getRoleDescription(role: UserRole): String {
        return when (role) {
            UserRole.DRIVER -> "Can view and create own entries/expenses only"
            UserRole.MANAGER -> "Can view all data, create own entries/expenses only"
            UserRole.ADMIN -> "Full access - can view, create, edit, and delete everything"
        }
    }
}