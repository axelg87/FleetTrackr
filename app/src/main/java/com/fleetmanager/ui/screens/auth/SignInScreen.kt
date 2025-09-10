package com.fleetmanager.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleetmanager.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import android.util.Log

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("SignInScreen", "Activity result received with code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("SignInScreen", "Account retrieved: ${account?.email}")
                account?.idToken?.let { token ->
                    Log.d("SignInScreen", "ID token received, signing in...")
                    viewModel.signInWithGoogle(token)
                } ?: run {
                    Log.e("SignInScreen", "No ID token received from Google account")
                    viewModel.onError("No ID token received from Google")
                }
            } catch (e: ApiException) {
                Log.e("SignInScreen", "ApiException during sign-in", e)
                viewModel.onError("Google Sign-In failed: ${e.message}")
            }
        } else {
            Log.d("SignInScreen", "Sign-in was cancelled or failed with result code: ${result.resultCode}")
            viewModel.onError("Google Sign-In was cancelled")
        }
    }
    
    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            onSignInSuccess()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // App Logo/Icon placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚗",
                        style = MaterialTheme.typography.displayLarge
                    )
                }
                
                Text(
                    text = stringResource(R.string.welcome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = stringResource(R.string.manage_your_fleet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                uiState.errorMessage?.let { error ->
                    if (error.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                Button(
                    onClick = {
                        Log.d("SignInScreen", "Sign-in button clicked")
                        
                        // Check if Google Play Services is available
                        val googleApiAvailability = GoogleApiAvailability.getInstance()
                        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
                        if (resultCode != ConnectionResult.SUCCESS) {
                            Log.e("SignInScreen", "Google Play Services not available: $resultCode")
                            viewModel.onError("Google Play Services is not available")
                            return@Button
                        }
                        
                        val signInIntent = viewModel.getGoogleSignInIntent()
                        Log.d("SignInScreen", "Launching Google Sign-In intent")
                        googleSignInLauncher.launch(signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50), // Green color
                        contentColor = Color.White
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.sign_in_with_google),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}