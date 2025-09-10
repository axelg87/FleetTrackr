package com.fleetmanager.data.repository

import android.content.Intent
import com.fleetmanager.auth.AuthResult
import com.fleetmanager.auth.AuthService
import com.fleetmanager.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService
) : AuthRepository {
    
    override val isSignedIn: Flow<Boolean> = authService.isSignedIn
    
    override val currentUserId: String?
        get() = authService.getCurrentUserId()
    
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return when (val result = authService.signInWithGoogle(idToken)) {
            is AuthResult.Success -> Result.success(Unit)
            is AuthResult.Error -> Result.failure(Exception(result.message))
        }
    }
    
    override fun getGoogleSignInIntent(): Intent {
        return authService.getGoogleSignInClient().signInIntent
    }
    
    override suspend fun signOut(): Result<Unit> {
        return try {
            authService.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}