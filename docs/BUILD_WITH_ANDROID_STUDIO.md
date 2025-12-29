# Building APKs with Android Studio

This guide shows how to build the APKs with TCP support using Android Studio.

## Prerequisites

- Android Studio installed
- Project opened in Android Studio
- Android SDK configured (usually automatic)

## Step 1: Open Project in Android Studio

1. **Open Android Studio**
2. **File → Open** (or **Open Project**)
3. Navigate to: `D:\projects\retrowatch`
4. Click **OK**

Android Studio will:
- Sync Gradle files
- Download dependencies
- Configure SDK paths automatically

## Step 2: Update TCP IP Address

**Before building**, update the TCP host IP in both apps:

### RetroWatch App

1. Navigate to: `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`
2. Find line with: `private static final String TCP_HOST = "192.168.52.99";`
3. Change to your PC's IP address (e.g., `"192.168.1.105"`)

### SmartGlasses Companion

1. Navigate to: `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java`
2. Find line with: `public static final String TCP_HOST = "192.168.52.99";`
3. Change to your PC's IP address (e.g., `"192.168.1.105"`)

**To find your PC's IP:**
- Windows: Open Command Prompt, type `ipconfig`, look for "IPv4 Address"
- Or check the simulator when it starts - it shows the IP

## Step 3: Build APKs

### Option A: Build Menu (Recommended)

**For RetroWatch App:**
1. In Android Studio, select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Or use shortcut: **Ctrl+Shift+A** (Windows) → type "Build APK"
3. Wait for build to complete
4. When done, click **locate** in the notification to find the APK
5. APK location: `app/build/outputs/apk/debug/app-debug.apk`

**For SmartGlasses Companion:**
1. In the **Build Variants** panel (bottom left), select `smartglasses_companion` module
2. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
3. Wait for build to complete
4. APK location: `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### Option B: Gradle Panel

1. Open **Gradle** panel (right side, or **View → Tool Windows → Gradle**)
2. Expand: **retrowatch → app → Tasks → build**
3. Double-click: **assembleDebug**
4. Repeat for: **retrowatch → smartglasses_companion → Tasks → build → assembleDebug**

### Option C: Terminal in Android Studio

1. Open **Terminal** tab (bottom of Android Studio)
2. Run:
   ```bash
   ./gradlew :app:assembleDebug
   ./gradlew :smartglasses_companion:assembleDebug
   ```

## Step 4: Install APKs

### Via Android Studio

1. Connect Android device via USB
2. Enable **USB Debugging** on device
3. In Android Studio: **Run → Run 'app'** (or **Run → Run 'smartglasses_companion'**)
4. Select your device
5. APK will be installed automatically

### Via ADB (Command Line)

1. Connect device via USB
2. In Android Studio Terminal, run:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   adb install smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk
   ```

### Via File Transfer

1. Copy APK files from:
   - `app/build/outputs/apk/debug/app-debug.apk`
   - `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`
2. Transfer to Android device
3. On device: Enable "Install from Unknown Sources"
4. Open APK file and install

## Step 5: Test with Simulator

1. **Start Simulator:**
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew run
   ```
   - Select "TCP Server" mode
   - Click "Start Server"
   - Note the IP address

2. **Update IP in Apps** (if different from what you set):
   - Change `TCP_HOST` in both service files
   - Rebuild APKs in Android Studio

3. **Run Apps:**
   - Open RetroWatch or SmartGlasses Companion app
   - App will automatically connect via TCP
   - Check simulator - should see "Client connected"
   - Send messages from app → Watch in simulator LCD

## Troubleshooting

### Build Errors

**"SDK not found":**
- Android Studio should handle this automatically
- If error persists: **File → Project Structure → SDK Location**
- Verify Android SDK path is set

**"Gradle sync failed":**
- **File → Invalidate Caches / Restart → Invalidate and Restart**
- Or: **File → Sync Project with Gradle Files**

**"Build failed":**
- Check **Build** tab (bottom) for error details
- Common issues: Missing dependencies, SDK version mismatch
- Try: **Build → Clean Project**, then rebuild

### Connection Issues

**"TCP connection failed":**
- Verify PC IP address is correct in service files
- Check Windows Firewall (allow port 8888)
- Ensure Android and PC are on same WiFi network
- Check simulator is running and shows "Server Running"

**"No client connected":**
- Make sure TCP server is running in simulator
- Check Android app logs: **View → Tool Windows → Logcat**
- Filter by app package name

## Quick Reference

### File Locations

- **RetroWatch Service:** `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`
- **SmartGlasses Service:** `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java`
- **APK Output:** `app/build/outputs/apk/debug/app-debug.apk`
- **APK Output:** `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### Configuration Variables

```java
// In RetroWatchService.java
private static final boolean USE_TCP_FOR_TESTING = true; // false = Bluetooth
private static final String TCP_HOST = "192.168.52.99"; // Your PC IP
private static final int TCP_PORT = 8888;

// In CompanionForegroundService.java  
public static final boolean USE_TCP_FOR_TESTING = true; // false = Bluetooth
public static final String TCP_HOST = "192.168.52.99"; // Your PC IP
public static final int TCP_PORT = 8888;
```

## Switching Between TCP and Bluetooth

To use **Bluetooth** (production):
1. Set `USE_TCP_FOR_TESTING = false` in both service files
2. Rebuild APKs
3. Install and use Bluetooth connection

To use **TCP** (testing):
1. Set `USE_TCP_FOR_TESTING = true` in both service files
2. Update `TCP_HOST` to your PC's IP
3. Rebuild APKs
4. Start simulator TCP server
5. Connect from Android app

