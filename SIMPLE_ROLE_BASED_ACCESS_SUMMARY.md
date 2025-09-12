# Simple Role-Based Access Control Implementation

This document summarizes the simplified role-based access control system for the Fleet Manager app.

## Overview

The implementation adds basic role-based access control with **~250 lines of code** across a few key files, featuring dynamic role fetching from Firestore.

### Roles
- **DRIVER**: Can only view their own entries/expenses
- **MANAGER**: Can view all entries/expenses 
- **ADMIN**: Can view all entries/expenses + edit/delete permissions

## Implementation Files

### 1. UserRole Enum (5 lines)
**File**: `domain/model/UserRole.kt`
```kotlin
enum class UserRole {
    ADMIN,
    MANAGER, 
    DRIVER
}
```

### 2. PermissionManager Object (7 lines)
**File**: `domain/model/PermissionManager.kt`
```kotlin
object PermissionManager {
    fun canEdit(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canDelete(userRole: UserRole): Boolean = userRole == UserRole.ADMIN
    fun canViewAll(userRole: UserRole): Boolean = userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
}
```

### 3. UserDto (5 lines)
**File**: `data/dto/UserDto.kt`
```kotlin
data class UserDto(
    val id: String,
    val name: String,
    val role: UserRole
)
```

### 4. FirestoreService Updates (~100 lines)
**File**: `data/remote/FirestoreService.kt`

Added role-based functionality:
- `getUserProfile(userId: String): Flow<UserDto>` - Real-time user profile from Firestore
- `getCurrentUserProfile(): Flow<UserDto>` - Current user's profile
- `getCurrentUserRole()` - Fetches current user role (with DRIVER default)
- `saveUserIfMissing()` - **Automatically creates user documents on first sign-in**
- `getDailyEntriesFlowForRole()` - Returns filtered entries based on role
- `getExpensesFlowForRole()` - Returns filtered expenses based on role

For DRIVER: `whereEqualTo("userId", currentUserId)`
For MANAGER/ADMIN: No filter (all data)

### 5. Updated ViewModels (~120 lines total)
**Files**: 
- `ui/viewmodel/EntryListViewModel.kt` 
- `ui/viewmodel/EntryDetailViewModel.kt`
- `ui/viewmodel/SignInViewModel.kt` - **Calls `saveUserIfMissing()` on sign-in**

Each ViewModel:
- Exposes `userProfile: StateFlow<UserDto>` from Firestore
- Exposes `userRole: StateFlow<UserRole>` for convenience
- Uses role-based Firestore queries dynamically
- Real-time role updates from Firestore
- **Automatic user document creation on first sign-in**

### 6. UI Permission Checks (~40 lines)
**Files**:
- `ui/screens/entry/EntryListScreen.kt`
- `ui/screens/entry/EntryDetailScreen.kt`

Features:
- Role indicator for managers/admins
- Dynamic edit/delete button visibility
- Real-time role-based UI updates

```kotlin
// Show edit/delete buttons only for admins
if (PermissionManager.canEdit(userRole)) {
    IconButton(onClick = { onEditEntry(entryId) }) {
        Icon(Icons.Default.Edit, contentDescription = "Edit")
    }
}
```

## Firestore Data Structure

### Users Collection
```
users/{uid} -> {
  "id": "firebase-user-uid",
  "name": "User Display Name",
  "role": "ADMIN" | "MANAGER" | "DRIVER",
  "email": "user@example.com",
  "createdAt": Timestamp
}
```

**Automatically created on first sign-in with:**
- Document ID = Firebase Auth UID
- Default role = DRIVER
- Name from Firebase Auth displayName
- Email from Firebase Auth

### Entries/Expenses
Each entry/expense includes:
```
{
  "userId": "firebase-user-uid",
  // ... other fields
}
```

## How It Works

1. **Role Storage**: User roles stored in Firestore `users/{uid}` collection
2. **Dynamic Role Fetching**: `getUserProfile()` provides real-time Flow of user data from Firestore
3. **Data Filtering**: Firestore queries filter based on role:
   - DRIVER: Only their own data (`whereEqualTo("userId", currentUserId)`)
   - MANAGER/ADMIN: All data (no filter)
4. **UI Permissions**: Simple checks using `PermissionManager` methods
5. **Real-time Updates**: Role changes reflect immediately via Firestore listeners

## Benefits

- **Simple**: Only ~250 lines of code total
- **Dynamic**: Real-time role fetching from Firestore
- **Maintainable**: Clear separation of concerns
- **Flexible**: Easy to extend with new roles
- **Secure**: Server-side filtering in Firestore queries
- **Performant**: Efficient Firestore listeners, minimal overhead

## Usage Example

```kotlin
// In UI components
val userRole by viewModel.userRole.collectAsStateWithLifecycle()
if (PermissionManager.canEdit(userRole)) {
    Button("Edit")
}

// In ViewModels  
val userProfile: StateFlow<UserDto> = firestoreService.getCurrentUserProfile()
firestoreService.getDailyEntriesFlowForRole(userRole)
```

## Testing

1. Create test users with different roles in Firestore
2. Verify data filtering works correctly
3. Check UI shows/hides actions appropriately
4. Test role changes reflect immediately via Firestore listeners
5. Confirm role indicator shows for managers/admins

This implementation provides robust role-based access control with real-time Firestore integration while maintaining simplicity and avoiding over-engineering.