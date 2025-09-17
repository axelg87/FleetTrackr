package com.fleetmanager.auth

import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {
    
    val currentUser: Flow<FirebaseUser?> = 
        firebaseAuth.authStateReceptionFlow().map { it }
    
    val isSignedIn: Flow<Boolean> = currentUser.map { it != null }
    
    private val googleClientInternal: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("905385497658-bhkg6pbfl2l38aq6p5ve0rcggisl0r55.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Unknown error occurred")
        }
    }
    
    fun getGoogleSignInClient(): GoogleSignInClient = googleClientInternal
    
    suspend fun signOut() {
        firebaseAuth.signOut()
        googleClientInternal.signOut().await()
    }
    
    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}

// Extension function to convert FirebaseAuth to Flow
private fun FirebaseAuth.authStateReceptionFlow(): Flow<FirebaseUser?> = 
    callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        addAuthStateListener(listener)
        awaitClose { removeAuthStateListener(listener) }
    }