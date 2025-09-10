package com.fleetmanager.domain.repository

import android.content.Intent
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for authentication operations.
 */
interface AuthRepository {
    val isSignedIn: Flow<Boolean>
    val currentUserId: String?
    
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    fun getGoogleSignInIntent(): Intent
    suspend fun signOut(): Result<Unit>
}