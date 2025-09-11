# 🔥 Firebase Setup Instructions for Fleet Manager

## 📋 Overview
Your Fleet Manager app now saves **Expense entries** to **Firebase Firestore**! Here's what you need to configure on the Firebase side.

## ✅ Current Status
- ✅ Firebase project configured (`ag-motion`)
- ✅ Google Services JSON file present
- ✅ Firebase dependencies added
- ✅ Firebase Authentication enabled
- ✅ Firebase Storage configured
- ⚠️ **Firestore Database needs to be enabled** (see steps below)

---

## 🚀 Required Setup Steps

### 1. Enable Firestore Database

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your project: **`ag-motion`**
3. Navigate to **"Firestore Database"** in the left sidebar
4. Click **"Create database"**
5. Choose **"Start in production mode"** (recommended for security)
6. Select a location closest to your users (e.g., `us-central1` for US)

### 2. Configure Firestore Security Rules

Replace the default rules with these secure rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Specific rules for expenses collection
    match /users/{userId}/expenses/{expenseId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      allow create: if request.auth != null && 
                   request.auth.uid == userId &&
                   request.resource.data.keys().hasAll(['id', 'type', 'amount', 'date', 'driver', 'car']) &&
                   request.resource.data.amount is number &&
                   request.resource.data.amount > 0;
    }
  }
}
```

### 3. Configure Firebase Storage Security Rules

Replace the default Storage rules with these secure rules in Firebase Console → Storage → Rules:

```javascript
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {
    // Users can only access their own photos
    match /users/{userId}/photos/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 4. Verify Required Firebase Services

Ensure these services are enabled in your Firebase project:

- ✅ **Authentication** (already configured)
- ⚠️ **Firestore Database** (enable this now)
- ✅ **Cloud Storage** (for photo uploads, already configured with rules above)

---

## 📊 Data Structure

### Collection Structure
The app creates these Firestore collections automatically:

```
users/
  └── {userId}/
      ├── expenses/
      │   └── {expenseId}/
      ├── dailyEntries/
      │   └── {entryId}/
      ├── drivers/
      │   └── {driverId}/
      └── vehicles/
          └── {vehicleId}/
```

### Expense Document Fields

Each expense document contains:

```json
{
  "id": "uuid-string",
  "type": "FUEL" | "MAINTENANCE" | "SERVICE" | "CAR_WASH" | "FINE" | "OTHER",
  "amount": 50.75,
  "date": "2025-01-15T10:30:00Z",
  "driver": "Ahmed",
  "car": "Mitsubishi Outlander 1",
  "notes": "Fill up tank",
  "photos": ["https://storage.googleapis.com/ag-motion.firebasestorage.app/..."],
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z",
  "isSynced": true
}
```

---

## 🔧 How It Works

1. **User fills expense form** in the app
2. **Validation occurs** before submission
3. **Photos uploaded** to Firebase Storage (if any)
4. **Expense saved** to local database first (offline-first)
5. **Expense synced** to Firestore (when online)
6. **Collection auto-created** under `users/{userId}/expenses/`

---

## 🔍 Monitoring & Verification

### Check if it's working:

1. **Firebase Console** → **Firestore Database** → **Data tab**
2. Look for structure: `users/{userId}/expenses/`
3. Submit a test expense from the app
4. Verify the document appears in Firestore

### Troubleshooting:

#### Common Issues:

**1. "Object doesn't exist at location" error:**
- ✅ **FIXED**: Ensure user is signed in before uploading photos
- ✅ **FIXED**: Added proper authentication checks in StorageService
- Verify Firebase Storage is enabled in Firebase Console
- Check that Storage security rules are configured correctly

**2. No data appearing in Firestore:**
- Check that user is signed in (see Authentication status in app)
- Verify Firestore Database is enabled in Firebase Console
- Check internet connectivity
- Verify security rules allow read/write for authenticated users

**3. Permission errors:**
- Verify Firestore security rules are set correctly
- Verify Storage security rules are set correctly
- Ensure user is authenticated before operations

**4. Photo upload failures:**
- Check internet connectivity
- Verify user is signed in
- Ensure Firebase Storage is enabled
- Check Storage security rules

**5. App crashes:**
- Check logs for Firebase initialization errors
- Verify google-services.json is in app/ directory
- Check Firebase dependencies in build.gradle.kts

---

## 🛡️ Security Features

- ✅ **User isolation**: Each user can only access their own data
- ✅ **Authentication required**: Must be signed in to write/read
- ✅ **Field validation**: Amount must be positive number
- ✅ **Required fields**: ID, type, amount, date, driver, car are mandatory
- ✅ **Offline-first**: Works without internet, syncs when online

---

## 📱 Testing

1. Sign in to the app
2. Go to **"Add Expense"** screen
3. Fill out all required fields:
   - Date: Select any date
   - Type: Choose from dropdown (Fuel, Maintenance, etc.)
   - Amount: Enter positive number
   - Driver: Select or type driver name
   - Vehicle: Select or type vehicle
   - Notes: Optional
   - Photos: Optional
4. Tap **"Save Expense"**
5. Check Firebase Console → Firestore → Data tab
6. Verify expense appears under `users/{your-user-id}/expenses/`

---

## ⚡ Performance Notes

- **Offline-first**: App works without internet
- **Automatic sync**: Syncs when connection restored
- **Photo optimization**: Images uploaded to Cloud Storage
- **Efficient queries**: Only user's data is fetched
- **Real-time updates**: Changes sync across devices

---

## 🎯 Next Steps After Setup

Once Firestore is enabled and rules are set:

1. ✅ Test expense creation
2. ✅ Verify data appears in Firestore console
3. ✅ Test offline functionality
4. ✅ Test photo uploads
5. ✅ Test data sync across multiple devices

**The app is ready to use once Firestore Database is enabled!** 🚀