# Building APKs with TCP Support

Both Android apps now have TCP support enabled for testing with the simulator.

## Prerequisites

1. **Android SDK** installed
2. **ANDROID_HOME** environment variable set, OR
3. **local.properties** file with `sdk.dir` path

## Configuration

### Step 1: Set Android SDK Location

**Option A: Create/Edit `local.properties`**
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```
(Use double backslashes on Windows, or forward slashes)

**Option B: Set Environment Variable**
```bash
export ANDROID_HOME=/path/to/android/sdk
```

### Step 2: Update TCP IP Address

**For RetroWatch App (`app/`):**
- Edit: `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`
- Change: `TCP_HOST = "192.168.52.99"` to your PC's IP address

**For SmartGlasses Companion (`smartglasses_companion/`):**
- Edit: `smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java`
- Change: `TCP_HOST = "192.168.52.99"` to your PC's IP address

## Building APKs

### Build RetroWatch App

```bash
cd app
../gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Build SmartGlasses Companion

```bash
cd smartglasses_companion
../gradlew assembleDebug
```

APK will be at: `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### Build Both at Once

```bash
../gradlew :app:assembleDebug :smartglasses_companion:assembleDebug
```

## Installing APKs

### Via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb install smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk
```

### Via File Transfer

1. Copy APK files to your Android device
2. Enable "Install from Unknown Sources" in Android settings
3. Open APK file and install

## Testing with Simulator

1. **Start Simulator TCP Server:**
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew run
   ```
   - Select "TCP Server" mode
   - Click "Start Server"
   - Note the IP address shown

2. **Update IP in Android Apps:**
   - Change `TCP_HOST` in both service files to match simulator IP
   - Rebuild APKs

3. **Install and Run:**
   - Install APKs on Android device
   - Make sure Android and PC are on same WiFi network
   - Open app and connect
   - Messages should flow to simulator

## Switching Back to Bluetooth

To disable TCP and use Bluetooth:

**RetroWatch App:**
- Set `USE_TCP_FOR_TESTING = false` in `RetroWatchService.java`

**SmartGlasses Companion:**
- Set `USE_TCP_FOR_TESTING = false` in `CompanionForegroundService.java`

Then rebuild APKs.

## Troubleshooting

**"SDK location not found":**
- Create `local.properties` with `sdk.dir` path
- Or set `ANDROID_HOME` environment variable

**"Build failed":**
- Check Android SDK is installed
- Check Java version (JDK 17+ recommended)
- Check Gradle version compatibility

**"TCP connection failed":**
- Verify PC IP address is correct
- Check Windows Firewall (allow port 8888)
- Ensure Android and PC are on same WiFi network

**"APK won't install":**
- Enable "Install from Unknown Sources"
- Uninstall old version first
- Check APK signature

