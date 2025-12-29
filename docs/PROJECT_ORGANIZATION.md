# Project Organization Guide

## Current Project Structure

```
retrowatch/
├── app/                          # RetroWatch Android App (Main)
├── smartglasses_companion/       # SmartGlasses Companion Android App
├── retrowatch_arduino_simulator/ # Desktop Arduino Simulator (Java)
├── bluetooth_test_device/        # Bluetooth Test Device (Java)
├── RetroWatch_Arduino/           # Arduino Source Code
└── RetroWatch_Android/           # Legacy Android Code
```

## Project Separation

All projects are already separated as independent modules:

1. **app/** - RetroWatch Android App
   - Standalone Android module
   - Can be built independently
   - APK: `app/build/outputs/apk/debug/app-debug.apk`

2. **smartglasses_companion/** - SmartGlasses Companion Android App
   - Standalone Android module
   - Can be built independently
   - APK: `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

3. **retrowatch_arduino_simulator/** - Desktop Simulator
   - Standalone Java project
   - Has its own `settings.gradle`
   - JAR: `retrowatch_arduino_simulator/build/libs/retrowatch_arduino_simulator-1.0.0.jar`

4. **bluetooth_test_device/** - Bluetooth Test Device
   - Standalone Java project
   - Has its own `settings.gradle`
   - JAR: `bluetooth_test_device/build/libs/bluetooth_test_device.jar`

## TCP Connection Settings (NEW)

Both Android apps now have **UI input fields** for TCP server IP and port:

### RetroWatch App

**Location:** Watch Control tab → TCP Server section

**Fields:**
- **Server IP:** Text input (default: 192.168.52.99)
- **Server Port:** Number input (default: 8888)

**How to use:**
1. Open RetroWatch app
2. Go to "Watch Control" tab
3. Scroll to "TCP Server (Simulator)" section
4. Enter your PC's IP address
5. Enter port (usually 8888)
6. Settings are saved automatically
7. App will connect using these settings when TCP mode is enabled

### SmartGlasses Companion

**Location:** Main screen → TCP Server Settings section

**Fields:**
- **Server IP:** Text input (default: 192.168.52.99)
- **Server Port:** Number input (default: 8888)

**How to use:**
1. Open SmartGlasses Companion app
2. Enter TCP server IP in "Server IP" field
3. Enter port in "Server Port" field
4. Click "Connect" button
5. Settings are saved automatically

## Building APKs in Android Studio

### Step 1: Open Project

1. Open Android Studio
2. **File → Open** → Select `D:\projects\retrowatch`
3. Wait for Gradle sync to complete

### Step 2: Build RetroWatch App

1. In Android Studio, select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Or use Gradle panel: **retrowatch → app → Tasks → build → assembleDebug**
3. APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Step 3: Build SmartGlasses Companion

1. In Android Studio, select **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Or use Gradle panel: **retrowatch → smartglasses_companion → Tasks → build → assembleDebug**
3. APK location: `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### Step 4: Build Both at Once

In Android Studio Terminal:
```bash
./gradlew :app:assembleDebug :smartglasses_companion:assembleDebug
```

## Installing APKs

### Via Android Studio

1. Connect Android device via USB
2. Enable USB Debugging
3. Click **Run** button (green play icon)
4. Select your device
5. APK installs automatically

### Via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb install smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk
```

### Via File Transfer

1. Copy APK files to Android device
2. Enable "Install from Unknown Sources"
3. Open APK file and install

## Testing with Simulator

1. **Start Simulator:**
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew run
   ```
   - Select "TCP Server" mode
   - Click "Start Server"
   - Note the IP address shown

2. **Configure Android App:**
   - Open app on Android device
   - Enter simulator IP address in TCP settings
   - Enter port (8888)
   - Connect

3. **Test:**
   - Send messages from Android app
   - Watch them appear in simulator LCD display

## Project Dependencies

### Root `settings.gradle`
```gradle
include ':app'
include ':smartglasses_companion'
```

### Independent Projects
- `retrowatch_arduino_simulator/` - Has own `settings.gradle`
- `bluetooth_test_device/` - Has own `settings.gradle`

## Configuration Files

### Android Apps
- `app/build.gradle` - RetroWatch app build config
- `smartglasses_companion/build.gradle` - SmartGlasses build config
- `local.properties` - Android SDK location (auto-generated)

### Simulator
- `retrowatch_arduino_simulator/build.gradle` - Java app build config
- `retrowatch_arduino_simulator/settings.gradle` - Standalone project

## Switching Between TCP and Bluetooth

### RetroWatch App

**To use TCP:**
- `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`
- Set: `USE_TCP_FOR_TESTING = true`
- Enter IP/port in app UI

**To use Bluetooth:**
- Set: `USE_TCP_FOR_TESTING = false`
- Use Bluetooth device selection

### SmartGlasses Companion

**To use TCP:**
- `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java`
- Set: `USE_TCP_FOR_TESTING = true`
- Enter IP/port in app UI

**To use Bluetooth:**
- Set: `USE_TCP_FOR_TESTING = false`
- Use Bluetooth device picker

## Summary of Changes

### ✅ Added TCP IP/Port Input UI

**RetroWatch App:**
- Added TCP Server settings section to Watch Control fragment
- IP and Port input fields
- Settings saved to SharedPreferences
- Auto-reconnect when settings change

**SmartGlasses Companion:**
- Added TCP Server Settings section to MainActivity
- IP and Port input fields
- Settings saved to SharedPreferences
- Used when connecting

### ✅ Updated Services

**RetroWatchService:**
- Loads TCP settings from SharedPreferences
- Updates connection when settings change
- Methods: `setTcpHost()`, `setTcpPort()`

**CompanionForegroundService:**
- Uses IP/port from UI input
- Saves settings to SharedPreferences

### ✅ Settings Management

**Settings.java:**
- Added `setTcpHost()`, `getTcpHost()`
- Added `setTcpPort()`, `getTcpPort()`
- Stored in SharedPreferences

## Next Steps

1. **Build APKs in Android Studio** (see instructions above)
2. **Install on device**
3. **Start simulator** and note IP address
4. **Enter IP/port in app** and connect
5. **Test message flow**

