package com.fleetmanager.domain.model

object PermissionManager {
    fun canEdit(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canDelete(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canViewAll(userRole: UserRole): Boolean = userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
}