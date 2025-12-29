# TCP Support Added to Android Apps - Summary

## ✅ What Was Done

### 1. RetroWatch App (`app/`)

**Files Modified:**
- ✅ `app/src/main/java/com/hardcopy/retrowatch/connectivity/TcpConnectionManager.java` - **NEW** (TCP connection manager)
- ✅ `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java` - Added TCP support
- ✅ `app/src/main/java/com/hardcopy/retrowatch/connectivity/TransactionBuilder.java` - Updated to support TCP

**Changes:**
- Added `TcpConnectionManager` class that mimics `BluetoothManager` interface
- Modified `RetroWatchService` to use TCP when `USE_TCP_FOR_TESTING = true`
- Updated `TransactionBuilder` to work with both Bluetooth and TCP
- TCP connection automatically connects on service start when enabled

**Configuration:**
```java
// In RetroWatchService.java
private static final boolean USE_TCP_FOR_TESTING = true; // Set to false for Bluetooth
private static final String TCP_HOST = "192.168.1.100"; // Change to your PC's IP
private static final int TCP_PORT = 8888;
```

### 2. SmartGlasses Companion (`smartglasses_companion/`)

**Files Modified:**
- ✅ `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/TcpConnectionHelper.java` - **NEW** (TCP helper)
- ✅ `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java` - Added TCP support
- ✅ `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/ui/MainActivity.java` - Updated to use TCP

**Changes:**
- Added `TcpConnectionHelper` for TCP socket management
- Modified `CompanionForegroundService` to support TCP connections
- Updated `MainActivity` to connect via TCP when enabled
- TCP connection uses same Protocol v2 as Bluetooth

**Configuration:**
```java
// In CompanionForegroundService.java
public static final boolean USE_TCP_FOR_TESTING = true; // Set to false for Bluetooth
public static final String TCP_HOST = "192.168.1.100"; // Change to your PC's IP
public static final int TCP_PORT = 8888;
```

## 📦 Building APKs

### Prerequisites

1. **Android SDK** installed
2. **local.properties** file exists with correct SDK path (already exists in your project)

### Build Commands

**Build RetroWatch App:**
```bash
cd app
../gradlew assembleDebug
```
APK location: `app/build/outputs/apk/debug/app-debug.apk`

**Build SmartGlasses Companion:**
```bash
cd smartglasses_companion
../gradlew assembleDebug
```
APK location: `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

**Build Both:**
```bash
../gradlew :app:assembleDebug :smartglasses_companion:assembleDebug
```

### If Build Fails

**"SDK location not found":**
- Check `local.properties` file exists
- Verify SDK path is correct: `sdk.dir=C\\:\\Users\\Ravin\\AppData\\Local\\Android\\Sdk`
- Or set `ANDROID_HOME` environment variable

**Other Issues:**
- Make sure Android SDK is installed at the specified path
- Check Java version (JDK 17+ recommended)
- Try: `../gradlew clean` then rebuild

## 🚀 Testing with Simulator

### Step 1: Start Simulator

```bash
cd retrowatch_arduino_simulator
../gradlew run
```

1. Select **"TCP Server"** mode
2. Click **"Start Server"**
3. Note the IP address shown (e.g., `192.168.1.100:8888`)

### Step 2: Update IP Address in Apps

**Before building APKs**, update the TCP_HOST in both apps:

1. **RetroWatch App:**
   - Edit: `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`
   - Change: `TCP_HOST = "192.168.1.100"` to your PC's actual IP

2. **SmartGlasses Companion:**
   - Edit: `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java`
   - Change: `TCP_HOST = "192.168.1.100"` to your PC's actual IP

### Step 3: Build and Install

1. Build APKs (see commands above)
2. Install on Android device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   adb install smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk
   ```

### Step 4: Connect

1. **Make sure Android and PC are on same WiFi network**
2. Open the Android app
3. App will automatically connect via TCP (if `USE_TCP_FOR_TESTING = true`)
4. Check simulator - you should see "Client connected" in log
5. Send messages from Android app → Watch them appear in simulator LCD

## 🔄 Switching Between TCP and Bluetooth

### To Use TCP (Testing):
- Set `USE_TCP_FOR_TESTING = true` in service files
- Rebuild APKs

### To Use Bluetooth (Production):
- Set `USE_TCP_FOR_TESTING = false` in service files
- Rebuild APKs

## 📝 Notes

- **TCP is for testing only** - Production should use Bluetooth
- Both apps are configured to use TCP by default (`USE_TCP_FOR_TESTING = true`)
- TCP works over WiFi - both devices must be on same network
- No Bluetooth pairing needed with TCP
- Faster than Bluetooth for development/testing

## 🐛 Troubleshooting

**"Connection failed" in Android:**
- Check PC IP address is correct
- Check Windows Firewall (allow port 8888)
- Verify Android and PC are on same WiFi

**"No client connected" in simulator:**
- Make sure TCP server is running (green status)
- Check Android app is trying to connect
- Check Android logs: `adb logcat | grep -i tcp`

**Messages not appearing:**
- Check simulator log for "RECV" messages
- Verify protocol format matches
- Check Android app logs for errors

## 📚 Related Files

- `app/TCP_CONNECTION_GUIDE.md` - Detailed TCP integration guide
- `retrowatch_arduino_simulator/QUICK_START_TCP.md` - Simulator quick start
- `BUILD_APK_GUIDE.md` - Detailed build instructions

