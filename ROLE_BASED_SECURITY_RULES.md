# Role-Based Access Control - Firebase Security Rules

This document outlines the Firebase Firestore security rules needed to enforce the role-based access control system implemented in the app.

## User Roles

- **DRIVER**: Can only view and create their own entries/expenses. Cannot edit or delete anything.
- **MANAGER**: Can view all data but can only create entries/expenses for themselves. Cannot edit or delete.
- **ADMIN**: Can view, create, edit, and delete everything.

## Required Firestore Security Rules

Replace your existing Firestore security rules with the following:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to get user role
    function getUserRole(userId) {
      return get(/databases/$(database)/documents/users/$(userId)).data.role;
    }
    
    // Helper function to check if user is admin
    function isAdmin(userId) {
      return getUserRole(userId) == 'admin';
    }
    
    // Helper function to check if user is manager or admin
    function isManagerOrAdmin(userId) {
      let role = getUserRole(userId);
      return role == 'manager' || role == 'admin';
    }
    
    // Users collection - users can read their own data, admins can read/write all
    match /users/{userId} {
      allow read: if request.auth != null && 
                     (request.auth.uid == userId || isAdmin(request.auth.uid));
      allow write: if request.auth != null && 
                      (request.auth.uid == userId || isAdmin(request.auth.uid));
    }
    
    // Daily entries collection
    match /entries/{entryId} {
      allow read: if request.auth != null && 
                     (resource.data.userId == request.auth.uid || 
                      isManagerOrAdmin(request.auth.uid));
      
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      
      allow update: if request.auth != null && 
                       isAdmin(request.auth.uid);
      
      allow delete: if request.auth != null && 
                       isAdmin(request.auth.uid);
    }
    
    // Expenses collection
    match /expenses/{expenseId} {
      allow read: if request.auth != null && 
                     (resource.data.userId == request.auth.uid || 
                      isManagerOrAdmin(request.auth.uid));
      
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      
      allow update: if request.auth != null && 
                       isAdmin(request.auth.uid);
      
      allow delete: if request.auth != null && 
                       isAdmin(request.auth.uid);
    }
    
    // Drivers collection - role-based access
    match /drivers/{driverId} {
      allow read: if request.auth != null && 
                     (resource.data.userId == request.auth.uid || 
                      isManagerOrAdmin(request.auth.uid));
      
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      
      allow update: if request.auth != null && 
                       isAdmin(request.auth.uid);
      
      allow delete: if request.auth != null && 
                       isAdmin(request.auth.uid);
    }
    
    // Vehicles collection - role-based access
    match /vehicles/{vehicleId} {
      allow read: if request.auth != null && 
                     (resource.data.userId == request.auth.uid || 
                      isManagerOrAdmin(request.auth.uid));
      
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      
      allow update: if request.auth != null && 
                       isAdmin(request.auth.uid);
      
      allow delete: if request.auth != null && 
                       isAdmin(request.auth.uid);
    }
  }
}
```

## User Collection Structure

Each user document in the `users` collection should have the following structure:

```json
{
  "uid": "firebase-user-id",
  "displayName": "John Doe",
  "email": "john@example.com",
  "role": "driver", // or "manager", "admin" 
  "createdAt": "2023-XX-XX",
  "photoUrl": "https://..." // optional
}
```

## How It Works

1. **User Creation**: When a user signs in for the first time, the app creates a user document with default role "driver".

2. **Role Assignment**: Admins can update user roles through the app (if you implement user management screens).

3. **Data Access**: 
   - Drivers can only read/write their own data
   - Managers can read all data but only write their own
   - Admins have full access to everything

4. **Security**: The Firebase security rules enforce these permissions at the database level, so even if someone bypasses the UI restrictions, they cannot access unauthorized data.

## Testing the Rules

To test the security rules:

1. Create test users with different roles
2. Try accessing data that should be restricted
3. Verify that the security rules properly deny unauthorized access

## Important Notes

- The app UI should reflect these permissions to avoid user confusion
- Always test security rules thoroughly before deploying to production
- Consider implementing audit logging for admin actions
- Regularly review and update user roles as needed