# Elarvic User App

Reconstructed Android source baseline from the supplied `base(1).apk`, rebranded as **Elarvic V1**.

## Current scope
- Android/Kotlin + Jetpack Compose foundation
- Firebase Authentication integration
- Google Sign-In flow
- Elarvic V1 branding
- Firebase Firestore dependency for account/expiry integration
- GitHub-safe `.gitignore`

## Firebase setup
1. Create/select the Firebase project.
2. Add Android app with application ID `com.elarvic.user`.
3. Enable Google provider in Firebase Authentication.
4. Download `google-services.json` into `app/`.
5. Put the Firebase Web client ID in `app/src/main/res/values/strings.xml` as `firebase_web_client_id`.
6. Configure the SHA-1/SHA-256 fingerprints for the debug/release signing keys.

`google-services.json` is intentionally ignored by Git.

## Important
The supplied APK is compiled/protected rather than an original Android Studio source tree. This repository is therefore a clean reconstruction baseline, not a byte-for-byte decompilation of the APK. Existing APK behavior/assets should be mapped into this source as they are recovered and verified.
