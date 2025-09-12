package com.fleetmanager.domain.manager

import com.fleetmanager.domain.model.User
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.RolePermissions
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized permission manager for role-based access control.
 * This class provides a single point to check permissions across the app.
 */
@Singleton
class PermissionManager @Inject constructor() {
    
    /**
     * Check if user can view all data (not just their own)
     */
    fun canViewAllData(user: User?): Boolean {
        return user?.let { RolePermissions.canViewAllData(it.getUserRole()) } ?: false
    }
    
    /**
     * Check if user can edit entries/expenses
     */
    fun canEdit(user: User?): Boolean {
        return user?.let { RolePermissions.canEdit(it.getUserRole()) } ?: false
    }
    
    /**
     * Check if user can delete entries/expenses
     */
    fun canDelete(user: User?): Boolean {
        return user?.let { RolePermissions.canDelete(it.getUserRole()) } ?: false
    }
    
    /**
     * Check if user can create entries/expenses
     */
    fun canCreate(user: User?): Boolean {
        return user?.let { RolePermissions.canCreate(it.getUserRole()) } ?: false
    }
    
    /**
     * Check if user can create entries/expenses for others
     */
    fun canCreateForOthers(user: User?): Boolean {
        return user?.let { RolePermissions.canCreateForOthers(it.getUserRole()) } ?: false
    }
    
    /**
     * Check if user can manage other users
     */
    fun canManageUsers(user: User?): Boolean {
        return user?.let { RolePermissions.canManageUsers(it.getUserRole()) } ?: false
    }
    
    /**
     * Check if current user can access a resource owned by another user
     */
    fun canAccessResource(currentUser: User?, resourceOwnerId: String): Boolean {
        if (currentUser == null) return false
        
        return when (currentUser.getUserRole()) {
            UserRole.DRIVER -> currentUser.uid == resourceOwnerId // Only own resources
            UserRole.MANAGER, UserRole.ADMIN -> true // Can access all resources
        }
    }
    
    /**
     * Check if current user can edit a resource owned by another user
     */
    fun canEditResource(currentUser: User?, resourceOwnerId: String): Boolean {
        if (currentUser == null) return false
        
        return when (currentUser.getUserRole()) {
            UserRole.DRIVER, UserRole.MANAGER -> false // Cannot edit anything
            UserRole.ADMIN -> true // Can edit everything
        }
    }
    
    /**
     * Check if current user can delete a resource owned by another user
     */
    fun canDeleteResource(currentUser: User?, resourceOwnerId: String): Boolean {
        if (currentUser == null) return false
        
        return when (currentUser.getUserRole()) {
            UserRole.DRIVER, UserRole.MANAGER -> false // Cannot delete anything
            UserRole.ADMIN -> true // Can delete everything
        }
    }
    
    /**
     * Get user-friendly description of what the user can do
     */
    fun getPermissionSummary(user: User?): String {
        return if (user != null) {
            RolePermissions.getRoleDescription(user.getUserRole())
        } else {
            "No permissions (not signed in)"
        }
    }
    
    /**
     * Check if user has a specific role
     */
    fun hasRole(user: User?, role: UserRole): Boolean {
        return user?.getUserRole() == role
    }
    
    /**
     * Check if user is admin
     */
    fun isAdmin(user: User?): Boolean = hasRole(user, UserRole.ADMIN)
    
    /**
     * Check if user is manager
     */
    fun isManager(user: User?): Boolean = hasRole(user, UserRole.MANAGER)
    
    /**
     * Check if user is driver
     */
    fun isDriver(user: User?): Boolean = hasRole(user, UserRole.DRIVER)
}