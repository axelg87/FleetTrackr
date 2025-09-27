package com.fleetmanager.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fleetmanager.data.dto.UserDto
import com.fleetmanager.data.remote.StorageService
import com.fleetmanager.data.remote.UserFirestoreService
import com.fleetmanager.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userProfile: UserDto? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val isUploadingPicture: Boolean = false,
    val isEditMode: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userFirestoreService: UserFirestoreService,
    private val storageService: StorageService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                userFirestoreService.getCurrentUserProfile()
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to load profile: AED{exception.message}"
                        )
                    }
                    .collect { userProfile ->
                        _uiState.value = _uiState.value.copy(
                            userProfile = userProfile,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load profile: AED{e.message}"
                )
            }
        }
    }
    
    fun updateProfile(name: String, email: String) {
        val currentUser = _uiState.value.userProfile ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true, error = null)
            
            try {
                userFirestoreService.updateUserProfile(
                    name = name,
                    email = email
                )
                
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    isEditMode = false,
                    successMessage = "Profile updated successfully"
                )
                
                // Clear success message after a delay
                clearSuccessMessage()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = "Failed to update profile: AED{e.message}"
                )
            }
        }
    }
    
    fun updateProfilePicture(imageUri: Uri) {
        val currentUser = _uiState.value.userProfile ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingPicture = true, error = null)
            
            try {
                // Upload image to Firebase Storage
                val downloadUrl = storageService.uploadProfilePicture(imageUri)
                
                // Update user profile with new picture URL
                userFirestoreService.updateProfilePicture(downloadUrl)
                
                _uiState.value = _uiState.value.copy(
                    isUploadingPicture = false,
                    successMessage = "Profile picture updated successfully"
                )
                
                // Clear success message after a delay
                clearSuccessMessage()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingPicture = false,
                    error = "Failed to update profile picture: AED{e.message}"
                )
            }
        }
    }
    
    fun setEditMode(isEditMode: Boolean) {
        _uiState.value = _uiState.value.copy(isEditMode = isEditMode)
        if (!isEditMode) {
            // Clear any errors when closing edit mode
            clearError()
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun clearSuccessMessage() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Show success message for 3 seconds
            _uiState.value = _uiState.value.copy(successMessage = null)
        }
    }
}