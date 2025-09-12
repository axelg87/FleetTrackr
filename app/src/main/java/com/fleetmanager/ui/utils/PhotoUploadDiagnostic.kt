package com.fleetmanager.ui.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fleetmanager.auth.AuthService
import com.fleetmanager.data.remote.StorageService
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Diagnostic utility to help troubleshoot photo upload issues
 */
@Singleton
class PhotoUploadDiagnostic @Inject constructor(
    private val authService: AuthService,
    private val storageService: StorageService,
    private val firebaseStorage: FirebaseStorage,
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "PhotoUploadDiagnostic"
    }
    
    /**
     * Run comprehensive diagnostics for photo upload issues
     */
    suspend fun runDiagnostics(photoUri: Uri? = null): DiagnosticResult {
        val results = mutableListOf<DiagnosticStep>()
        
        // Step 1: Check authentication
        results.add(checkAuthentication())
        
        // Step 2: Check Firebase Storage connectivity
        results.add(checkFirebaseStorage())
        
        // Step 3: Check Storage configuration
        results.add(checkStorageConfiguration())
        
        // Step 4: If photo URI provided, check file access
        photoUri?.let { uri ->
            results.add(checkPhotoAccess(uri))
        }
        
        // Step 5: Test storage permissions
        results.add(checkStoragePermissions())
        
        val hasErrors = results.any { !it.success }
        val recommendations = generateRecommendations(results)
        
        return DiagnosticResult(
            success = !hasErrors,
            steps = results,
            recommendations = recommendations
        )
    }
    
    private fun checkAuthentication(): DiagnosticStep {
        return try {
            val userId = authService.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                DiagnosticStep(
                    name = "Authentication Check",
                    success = false,
                    message = "User is not authenticated. Sign in required for photo uploads.",
                    details = "getCurrentUserId() returned null or empty"
                )
            } else {
                DiagnosticStep(
                    name = "Authentication Check",
                    success = true,
                    message = "User is authenticated successfully",
                    details = "User ID: $userId"
                )
            }
        } catch (e: Exception) {
            DiagnosticStep(
                name = "Authentication Check",
                success = false,
                message = "Authentication check failed: ${e.message}",
                details = "Exception: ${e.javaClass.simpleName}"
            )
        }
    }
    
    private suspend fun checkFirebaseStorage(): DiagnosticStep {
        return try {
            // Test basic Firebase Storage connectivity
            val rootRef = firebaseStorage.reference
            rootRef.metadata.await()
            
            DiagnosticStep(
                name = "Firebase Storage Connectivity",
                success = true,
                message = "Firebase Storage is accessible",
                details = "Successfully connected to storage bucket"
            )
        } catch (e: Exception) {
            DiagnosticStep(
                name = "Firebase Storage Connectivity",
                success = false,
                message = "Cannot connect to Firebase Storage: ${e.message}",
                details = "This could indicate Firebase Storage is not enabled or configured properly"
            )
        }
    }
    
    private suspend fun checkStorageConfiguration(): DiagnosticStep {
        return try {
            val isConfigured = storageService.verifyStorageConfiguration()
            if (isConfigured) {
                DiagnosticStep(
                    name = "Storage Configuration",
                    success = true,
                    message = "Storage configuration is valid",
                    details = "User storage path is accessible"
                )
            } else {
                DiagnosticStep(
                    name = "Storage Configuration",
                    success = false,
                    message = "Storage configuration verification failed",
                    details = "Check Firebase Storage security rules and user authentication"
                )
            }
        } catch (e: Exception) {
            DiagnosticStep(
                name = "Storage Configuration",
                success = false,
                message = "Storage configuration check failed: ${e.message}",
                details = "Exception: ${e.javaClass.simpleName}"
            )
        }
    }
    
    private fun checkPhotoAccess(uri: Uri): DiagnosticStep {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytesAvailable = inputStream.available()
                if (bytesAvailable > 0) {
                    DiagnosticStep(
                        name = "Photo File Access",
                        success = true,
                        message = "Photo file is accessible",
                        details = "File size: $bytesAvailable bytes, URI: $uri"
                    )
                } else {
                    DiagnosticStep(
                        name = "Photo File Access",
                        success = false,
                        message = "Photo file is empty or not accessible",
                        details = "File size: 0 bytes, URI: $uri"
                    )
                }
            } ?: DiagnosticStep(
                name = "Photo File Access",
                success = false,
                message = "Cannot open input stream for photo",
                details = "URI: $uri"
            )
        } catch (e: SecurityException) {
            DiagnosticStep(
                name = "Photo File Access",
                success = false,
                message = "Permission denied accessing photo",
                details = "Grant storage permissions and try again. URI: $uri"
            )
        } catch (e: Exception) {
            DiagnosticStep(
                name = "Photo File Access",
                success = false,
                message = "Error accessing photo: ${e.message}",
                details = "Exception: ${e.javaClass.simpleName}, URI: $uri"
            )
        }
    }
    
    private suspend fun checkStoragePermissions(): DiagnosticStep {
        return try {
            val userId = authService.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                DiagnosticStep(
                    name = "Storage Permissions",
                    success = false,
                    message = "Cannot check storage permissions - user not authenticated",
                    details = "Sign in first to test storage permissions"
                )
            } else {
                // Try to access the user's storage path
                val userStorageRef = firebaseStorage.reference
                    .child("users")
                    .child(userId)
                    .child("photos")
                
                userStorageRef.metadata.await()
                
                DiagnosticStep(
                    name = "Storage Permissions",
                    success = true,
                    message = "User has proper storage permissions",
                    details = "Can access user storage path: users/$userId/photos"
                )
            }
        } catch (e: Exception) {
            DiagnosticStep(
                name = "Storage Permissions",
                success = false,
                message = "Storage permission check failed: ${e.message}",
                details = "This may indicate incorrect Firebase Storage security rules"
            )
        }
    }
    
    private fun generateRecommendations(steps: List<DiagnosticStep>): List<String> {
        val recommendations = mutableListOf<String>()
        
        steps.forEach { step ->
            if (!step.success) {
                when (step.name) {
                    "Authentication Check" -> {
                        recommendations.add("• Sign in to your account before uploading photos")
                        recommendations.add("• Check if authentication service is properly configured")
                    }
                    "Firebase Storage Connectivity" -> {
                        recommendations.add("• Verify Firebase Storage is enabled in Firebase Console")
                        recommendations.add("• Check internet connectivity")
                        recommendations.add("• Verify google-services.json is properly configured")
                    }
                    "Storage Configuration" -> {
                        recommendations.add("• Configure Firebase Storage security rules in Firebase Console")
                        recommendations.add("• Ensure rules allow authenticated users to access their own photos")
                    }
                    "Photo File Access" -> {
                        recommendations.add("• Grant storage permissions to the app")
                        recommendations.add("• Try selecting the photo again")
                        recommendations.add("• Ensure the photo file still exists")
                    }
                    "Storage Permissions" -> {
                        recommendations.add("• Update Firebase Storage security rules")
                        recommendations.add("• Verify user authentication is working properly")
                    }
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("• All diagnostics passed - photo upload should work normally")
        }
        
        return recommendations.distinct()
    }
    
    /**
     * Log diagnostic results to console for debugging
     */
    fun logDiagnosticResults(result: DiagnosticResult) {
        Log.d(TAG, "=== Photo Upload Diagnostic Results ===")
        Log.d(TAG, "Overall Status: ${if (result.success) "✅ PASS" else "❌ FAIL"}")
        Log.d(TAG, "")
        
        result.steps.forEach { step ->
            Log.d(TAG, "${if (step.success) "✅" else "❌"} ${step.name}")
            Log.d(TAG, "   Message: ${step.message}")
            Log.d(TAG, "   Details: ${step.details}")
            Log.d(TAG, "")
        }
        
        Log.d(TAG, "Recommendations:")
        result.recommendations.forEach { recommendation ->
            Log.d(TAG, recommendation)
        }
        Log.d(TAG, "=====================================")
    }
}

data class DiagnosticResult(
    val success: Boolean,
    val steps: List<DiagnosticStep>,
    val recommendations: List<String>
)

data class DiagnosticStep(
    val name: String,
    val success: Boolean,
    val message: String,
    val details: String
)