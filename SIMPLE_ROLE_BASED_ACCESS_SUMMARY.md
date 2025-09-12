# Simple Role-Based Access Control Implementation

This document summarizes the simplified role-based access control system for the Fleet Manager app.

## Overview

The implementation adds basic role-based access control with just **~200 lines of code** across a few key files.

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

### 3. FirestoreService Updates (~50 lines)
**File**: `data/remote/FirestoreService.kt`

Added simple role-based filtering:
- `getCurrentUserRole()` - Fetches user role from Firestore users collection
- `getDailyEntriesFlowForRole()` - Returns filtered entries based on role
- `getExpensesFlowForRole()` - Returns filtered expenses based on role

For DRIVER: `whereEqualTo("userId", currentUserId)`
For MANAGER/ADMIN: No filter (all data)

### 4. Updated ViewModels (~80 lines total)
**Files**: 
- `ui/viewmodel/EntryListViewModel.kt` 
- `ui/viewmodel/EntryDetailViewModel.kt`

Each ViewModel:
- Exposes `userRole` as StateFlow
- Uses role-based Firestore queries
- Keeps simple state management

### 5. UI Permission Checks (~30 lines)
**Files**:
- `ui/screens/entry/EntryListScreen.kt`
- `ui/screens/entry/EntryDetailScreen.kt`

Simple permission checks:
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
  "role": "ADMIN" | "MANAGER" | "DRIVER",
  "displayName": "User Name",
  "email": "user@example.com"
}
```

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
2. **Role Fetching**: `getCurrentUserRole()` method fetches role (defaults to DRIVER)
3. **Data Filtering**: Firestore queries filter based on role:
   - DRIVER: Only their own data (`whereEqualTo("userId", currentUserId)`)
   - MANAGER/ADMIN: All data (no filter)
4. **UI Permissions**: Simple checks using `PermissionManager` methods
5. **Real-time Updates**: Role refreshes every 5 seconds in ViewModels

## Benefits

- **Simple**: Only ~200 lines of code total
- **Maintainable**: Clear separation of concerns
- **Flexible**: Easy to extend with new roles
- **Secure**: Server-side filtering in Firestore queries
- **Performant**: Minimal overhead, efficient queries

## Usage Example

```kotlin
// In UI components
if (PermissionManager.canEdit(userRole)) {
    Button("Edit")
}

// In ViewModels  
firestoreService.getDailyEntriesFlowForRole(userRole)
```

## Testing

1. Create test users with different roles in Firestore
2. Verify data filtering works correctly
3. Check UI shows/hides actions appropriately
4. Test role changes take effect within 5 seconds

This implementation provides robust role-based access control while maintaining simplicity and avoiding over-engineering.