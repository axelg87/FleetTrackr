package com.fleetmanager.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Domain model for expense type collection item.
 * This allows for dynamic expense types that can be managed by admins.
 */
data class ExpenseTypeItem(
    @get:PropertyName("id")
    val id: String = "",
    
    @get:PropertyName("name")
    val name: String = "",
    
    @get:PropertyName("displayName") 
    val displayName: String = "",
    
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    
    @get:PropertyName("createdAt")
    val createdAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    
    @get:PropertyName("updatedAt")
    val updatedAt: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now()
) {
    fun isValid(): Boolean {
        return id.isNotBlank() &&
                name.isNotBlank() &&
                displayName.isNotBlank()
    }
}