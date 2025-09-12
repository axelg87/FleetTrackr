package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.auth.AuthService
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.domain.model.User
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.domain.model.RolePermissions
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserRoleUiState(
    val currentUser: User? = null,
    val userRole: UserRole = UserRole.DRIVER,
    val isLoading: Boolean = true,
    val error: String? = null
) {
    // Convenience methods for UI
    val canViewAllData: Boolean get() = RolePermissions.canViewAllData(userRole)
    val canEdit: Boolean get() = RolePermissions.canEdit(userRole)
    val canDelete: Boolean get() = RolePermissions.canDelete(userRole)
    val canCreate: Boolean get() = RolePermissions.canCreate(userRole)
    val canCreateForOthers: Boolean get() = RolePermissions.canCreateForOthers(userRole)
    val canManageUsers: Boolean get() = RolePermissions.canManageUsers(userRole)
    val roleDescription: String get() = RolePermissions.getRoleDescription(userRole)
}

@HiltViewModel
class UserRoleViewModel @Inject constructor(
    private val authService: AuthService,
    private val firestoreService: FirestoreService,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserRoleUiState())
    val uiState: StateFlow<UserRoleUiState> = _uiState.asStateFlow()
    
    init {
        observeAuthState()
    }
    
    private fun observeAuthState() {
        viewModelScope.launch {
            authService.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadUserRole(firebaseUser.uid)
                } else {
                    // User signed out
                    _uiState.value = UserRoleUiState(
                        currentUser = null,
                        userRole = UserRole.DRIVER,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    private suspend fun loadUserRole(uid: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        try {
            val user = firestoreService.getUserByUid(uid)
            
            if (user != null) {
                // Existing user found
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    userRole = user.getUserRole(),
                    isLoading = false
                )
            } else {
                // First-time user - create user document with default role
                createNewUser(uid)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load user role: ${e.message}"
            )
        }
    }
    
    private suspend fun createNewUser(uid: String) {
        try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                val user = firestoreService.createUserFromAuth(
                    uid = uid,
                    displayName = firebaseUser.displayName,
                    email = firebaseUser.email,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    role = UserRole.DRIVER // Default role for new users
                )
                
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    userRole = user.getUserRole(),
                    isLoading = false
                )
            } else {
                throw IllegalStateException("Firebase user is null")
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to create user: ${e.message}"
            )
        }
    }
    
    /**
     * Refresh user role from Firestore
     */
    fun refreshUserRole() {
        val currentUserId = authService.getCurrentUserId()
        if (currentUserId != null) {
            viewModelScope.launch {
                loadUserRole(currentUserId)
            }
        }
    }
    
    /**
     * Update user role (admin only)
     */
    fun updateUserRole(targetUid: String, newRole: UserRole, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                firestoreService.updateUserRole(targetUid, newRole)
                onResult(true, null)
                
                // If updating current user, refresh the state
                if (targetUid == authService.getCurrentUserId()) {
                    refreshUserRole()
                }
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
    
    /**
     * Check if current user can perform an action on a resource
     */
    fun canAccessResource(resourceUserId: String): Boolean {
        val currentUser = _uiState.value.currentUser ?: return false
        val userRole = _uiState.value.userRole
        
        return when (userRole) {
            UserRole.DRIVER -> currentUser.uid == resourceUserId // Only own resources
            UserRole.MANAGER, UserRole.ADMIN -> true // Can access all resources
        }
    }
    
    /**
     * Check if current user can edit a resource
     */
    fun canEditResource(resourceUserId: String): Boolean {
        val currentUser = _uiState.value.currentUser ?: return false
        val userRole = _uiState.value.userRole
        
        return when (userRole) {
            UserRole.DRIVER, UserRole.MANAGER -> false // Cannot edit anything
            UserRole.ADMIN -> true // Can edit everything
        }
    }
    
    /**
     * Check if current user can delete a resource
     */
    fun canDeleteResource(resourceUserId: String): Boolean {
        val currentUser = _uiState.value.currentUser ?: return false
        val userRole = _uiState.value.userRole
        
        return when (userRole) {
            UserRole.DRIVER, UserRole.MANAGER -> false // Cannot delete anything
            UserRole.ADMIN -> true // Can delete everything
        }
    }
}