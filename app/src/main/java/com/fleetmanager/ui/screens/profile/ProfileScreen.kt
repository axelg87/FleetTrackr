package com.fleetmanager.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fleetmanager.domain.model.UserRole
import com.fleetmanager.ui.components.StatusCard
import com.fleetmanager.ui.components.StatusType
import com.fleetmanager.ui.components.ScreenBackground

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateProfilePicture(it) }
    }
    
    ScreenBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header with back button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Profile Picture Section
            item {
                ProfilePictureSection(
                    profilePictureUrl = uiState.userProfile?.profilePictureUrl,
                    userName = uiState.userProfile?.name ?: "",
                    isUploading = uiState.isUploadingPicture,
                    onPictureClick = { imagePickerLauncher.launch("image/*") }
                )
            }
        
            // User Information Section
            uiState.userProfile?.let { user ->
                item {
                    UserInfoSection(
                        name = user.name,
                        email = user.email,
                        role = user.role,
                        onEditClick = { viewModel.setEditMode(true) }
                    )
                }
            }

            // Edit Mode Dialog
            if (uiState.isEditMode) {
                item {
                    EditProfileDialog(
                        currentName = uiState.userProfile?.name ?: "",
                        currentEmail = uiState.userProfile?.email ?: "",
                        isLoading = uiState.isUpdating,
                        onDismiss = { viewModel.setEditMode(false) },
                        onSave = { name, email -> viewModel.updateProfile(name, email) }
                    )
                }
            }

            // Loading State
            if (uiState.isLoading) {
                item {
                    StatusCard(
                        type = StatusType.Loading,
                        message = "Loading profile..."
                    )
                }
            }

            // Error State
            uiState.error?.let { error ->
                item {
                    StatusCard(
                        type = StatusType.Error,
                        message = error
                    )
                }
            }

            // Success State
            uiState.successMessage?.let { message ->
                item {
                    StatusCard(
                        type = StatusType.Success,
                        message = message
                    )
                }
            }
    }
}

@Composable
private fun ProfilePictureSection(
    profilePictureUrl: String?,
    userName: String,
    isUploading: Boolean,
    onPictureClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable { onPictureClick() },
            contentAlignment = Alignment.Center
        ) {
            // Profile picture or initials
            if (profilePictureUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback to initials
                ProfileInitials(
                    userName = userName,
                    size = 120
                )
            }
            
            // Upload overlay
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }
            
            // Camera icon overlay
            if (!isUploading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Picture",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Tap to change profile picture",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileInitials(
    userName: String,
    size: Int
) {
    val initials = remember(userName) {
        getInitials(userName)
    }
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                CircleShape
            )
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = (size * 0.35).sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size((size * 0.5).dp)
            )
        }
    }
}

@Composable
private fun UserInfoSection(
    name: String,
    email: String,
    role: UserRole,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name Field
            InfoField(
                label = "Full Name",
                value = name,
                icon = Icons.Default.Person
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email Field
            InfoField(
                label = "Email",
                value = email.ifEmpty { "No email provided" },
                icon = Icons.Default.Email
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Role Field
            InfoField(
                label = "Role",
                value = formatRole(role),
                icon = Icons.Default.Badge
            )
        }
    }
}

@Composable
private fun InfoField(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    currentName: String,
    currentEmail: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text("Edit Profile")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    placeholder = { Text("Enter your full name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    placeholder = { Text("Enter your email address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), email.trim()) },
                enabled = !isLoading && name.trim().isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun getInitials(name: String): String {
    if (name.isBlank()) return ""
    
    val words = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> {
            "${words.first().first().uppercase()}${words.last().first().uppercase()}"
        }
        words.size == 1 -> {
            val word = words.first()
            if (word.length >= 2) {
                word.take(2).uppercase()
            } else {
                word.take(1).uppercase()
            }
        }
        else -> ""
    }
}

private fun formatRole(role: UserRole): String {
    return when (role) {
        UserRole.ADMIN -> "Administrator"
        UserRole.MANAGER -> "Manager"
        UserRole.DRIVER -> "Driver"
    }
}