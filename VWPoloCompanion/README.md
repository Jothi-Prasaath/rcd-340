# VW Polo Companion (Android)

A native Kotlin + Jetpack Compose starter app for a futuristic, touch-first companion interface.

## Current features
- Dark futuristic cockpit-style dashboard with red accents
- Polo profile card
- Touch-selectable local drive profiles (UI only; does not change vehicle behavior)
- Navigation shortcut that attempts to open Google Maps navigation
- Music shortcut that opens Spotify if installed, otherwise YouTube Music
- Phone shortcut that opens the dialer
- Placeholder assistant/settings/vehicle profile dialogs

## Important limitations
- This app does not read live vehicle data and does not connect to the ECU or RCD 330.
- It does not modify infotainment firmware.
- A normal Android app cannot replace the RCD 330's native interface.
- Android Auto integration requires compatible app categories and Google's applicable development/testing requirements. It is not automatically provided by this starter project.
- The app does not yet implement voice input, media session control, or persistent settings.

## Build an APK (free)
1. Install Android Studio on Windows: https://developer.android.com/studio
2. Extract this ZIP.
3. In Android Studio choose **Open** and select the `VWPoloCompanion` folder.
4. Allow Gradle sync to finish. If prompted, install Android SDK Platform 35 and a compatible JDK (Android Studio's bundled JDK is fine).
5. Connect an Android phone with USB debugging enabled, or use an emulator.
6. Click **Run** to install and test.
7. To create an APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
8. The debug APK is normally created at `app/build/outputs/apk/debug/app-debug.apk`.

## Package
`com.budz.vwpolo`

## Toolchain
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21
- Jetpack Compose
- Minimum Android version: 8.0 (API 26)
