package com.fleetmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.remote.FirestoreService
import com.fleetmanager.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val firestoreService: FirestoreService
) : ViewModel() {
    
    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()
    
    init {
        loadUserRole()
    }
    
    private fun loadUserRole() {
        viewModelScope.launch {
            try {
                firestoreService.getCurrentUserProfile().collect { userProfile ->
                    _userRole.value = userProfile.role
                }
            } catch (e: Exception) {
                // Default to DRIVER role if there's an error
                _userRole.value = UserRole.DRIVER
            }
        }
    }
}