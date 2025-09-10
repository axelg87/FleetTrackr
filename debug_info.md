# Google Sign-In Debug Information

## Current Configuration

### OAuth Client ID (Web): 
`905385497658-bhkg6pbfl2l38aq6p5ve0rcggisl0r55.apps.googleusercontent.com`

### OAuth Client ID (Android for com.fleetmanager):
`905385497658-kpno4t06k92hkqvbbn3r94nu4p0iie91.apps.googleusercontent.com`

### Certificate Hash (from google-services.json):
`141c26bf25773298e5aafcf441c72855624cd558`

## Common Issues and Solutions

1. **Certificate Hash Mismatch**: The most common cause of Google Sign-In not working
   - Debug builds use a different certificate than release builds
   - The certificate hash in Firebase must match the signing certificate

2. **Package Name Mismatch**: Ensure the package name in Firebase matches the app
   - Current app package: `com.fleetmanager`
   - Firebase config package: `com.fleetmanager` ✓

3. **Client ID Usage**:
   - Use Web Client ID for `requestIdToken()` ✓
   - Android Client ID is used automatically by the Google Sign-In SDK

## Debugging Steps Added

1. ✅ Added comprehensive logging to identify where the flow breaks
2. ✅ Added Google Play Services availability check
3. ✅ Improved error handling and user feedback
4. ✅ Verified client ID configuration

## Next Steps for User

1. Check the Android device/emulator logs using: `adb logcat | grep -E "(SignInScreen|SignInViewModel|AuthService)"`
2. Verify that Google Play Services is installed and updated
3. If using an emulator, ensure it has Google Play Services
4. Check if the certificate hash matches (this is the most likely issue)

## Certificate Hash Generation

For debug builds, generate the SHA-1 hash using:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

For release builds, use your production keystore.