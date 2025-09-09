# Fleet Manager Android App  

## Project Overview
A modern Android application built with Jetpack Compose for fleet managers to track daily income and expenses for multiple drivers. Features offline-first architecture with Firebase cloud sync.

## Architecture & Features Implemented
✅ **MVVM Architecture** with clean separation of concerns
✅ **Jetpack Compose** for modern UI
✅ **Firebase Integration** (Auth, Firestore, Storage)
✅ **Offline-First** with Room database
✅ **Google Sign-In** authentication
✅ **Hilt Dependency Injection**
✅ **WorkManager** for background sync
✅ **Material 3** design system

## Key Components Created

### Data Layer
- **Room Database**: Local offline storage
- **Firebase Services**: Cloud storage and authentication  
- **Repository Pattern**: Offline-first data access
- **Models**: DailyEntry, Driver, Vehicle entities

### UI Layer  
- **SignInScreen**: Google authentication with Firebase
- **AddEntryScreen**: Form with dropdowns, earnings input, photo upload
- **EntryListScreen**: Display entries with sync status
- **Navigation**: Compose Navigation between screens

### Background Services
- **SyncWorker**: Background sync using WorkManager
- **SyncManager**: Manages periodic and manual sync

## Package Structure
```
com.fleetmanager/
├── auth/           # Authentication services
├── data/
│   ├── local/      # Room database, DAOs
│   ├── model/      # Data models
│   ├── remote/     # Firebase services
│   └── repository/ # Data repositories
├── di/             # Hilt dependency injection
├── sync/           # Background sync components
└── ui/
    ├── navigation/ # Navigation setup
    ├── screens/    # Compose screens & ViewModels
    └── theme/      # Material 3 theming
```

## Recent Changes
- Created complete Android project structure (September 2025)
- Implemented all core features including offline sync
- Built modern Compose UI with Material 3
- Configured Firebase integration
- Set up Hilt DI and WorkManager

## Firebase Configuration
The app includes a sample `google-services.json` file. For production:
1. Create Firebase project at console.firebase.google.com
2. Add Android app with package name `com.fleetmanager`
3. Download and replace `google-services.json`
4. Configure Google Sign-In in Firebase Auth
5. Set up Firestore and Storage rules

## User Preferences
- Modern Android development practices
- Clean architecture with MVVM
- Material 3 design system
- Offline-first approach
- Firebase for cloud services
