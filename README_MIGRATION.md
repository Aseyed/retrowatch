# RetroWatch Android Migration

This project has been upgraded to a modern Android Gradle project, compatible with Android 14 (API 34).

## Key Changes

1.  **Project Structure**: Converted from Eclipse ADT to standard Gradle structure (`app/src/main/java`, `app/src/main/res`).
2.  **Dependencies**:
    *   Removed `android-support-v4.jar`.
    *   Migrated to AndroidX (`androidx.appcompat`, `androidx.core`, `androidx.fragment`).
    *   Updated `build.gradle` to target SDK 34 (Android 14) and min SDK 23 (Android 6.0).
3.  **Permissions**:
    *   Added Runtime Permission requests for Android 6.0+ (Location), Android 12+ (Bluetooth Scan/Connect), and Android 13+ (Notifications).
    *   Updated `AndroidManifest.xml` with `exported` attributes for components (required for Android 12+).
4.  **Code Fixes**:
    *   Updated `RetroWatchActivity` to handle runtime permissions before starting the service.
    *   Updated `NotificationReceiverService` and `RetroWatchService` to use correct flags (`RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED`) for `registerReceiver` on Android 14.
    *   Replaced deprecated `FragmentPagerAdapter` constructor.

## How to Build

### Option 1: GitHub Actions (Recommended)
This repository includes a GitHub Actions workflow that will automatically build the APK.
1.  Push this code to GitHub.
2.  Go to the "Actions" tab in your repository.
3.  Wait for the build to complete.
4.  Download the `app-debug` artifact (APK).

### Option 2: Command Line
If you have the Android SDK installed:
1.  Run `./gradlew build`.
    *   The wrapper script will attempt to download the `gradle-wrapper.jar` if it's missing (requires `curl` or `wget`).
2.  The APK will be in `app/build/outputs/apk/debug/`.

### Option 3: Android Studio
1.  Open the project root in **Android Studio**.
2.  Sync Gradle.
3.  Build > Build Bundle(s) / APK(s) > Build APK(s).

## Notes

*   **Bluetooth**: The app uses Classic Bluetooth (SPP). On Android 12+, `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN` permissions are required and handled.
*   **Notifications**: Grant "Notification Access" in Android Settings when prompted by the app is still required for the `NotificationListenerService` to work, in addition to the Runtime Permission for posting notifications (which is different).
