# Build APKs Now - Quick Guide

## ✅ What's Ready

Both Android apps now have:
- ✅ TCP IP/Port input fields in UI
- ✅ Settings persistence (saved automatically)
- ✅ Auto-reconnect when settings change
- ✅ TCP support fully integrated

## 🚀 Build in Android Studio

### Quick Steps

1. **Open Android Studio**
2. **File → Open** → `D:\projects\retrowatch`
3. Wait for Gradle sync

4. **Build RetroWatch App:**
   - **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - APK: `app/build/outputs/apk/debug/app-debug.apk`

5. **Build SmartGlasses Companion:**
   - **Build → Build Bundle(s) / APK(s) → Build APK(s)**
   - APK: `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### Or Use Gradle Panel

1. Open **Gradle** panel (right side)
2. Expand: **retrowatch → app → Tasks → build**
3. Double-click: **assembleDebug**
4. Repeat for: **smartglasses_companion → Tasks → build → assembleDebug**

### Or Use Terminal in Android Studio

```bash
./gradlew :app:assembleDebug :smartglasses_companion:assembleDebug
```

## 📱 Install APKs

### Method 1: Via Android Studio
- Connect device via USB
- Click **Run** button (green play icon)
- Select device

### Method 2: Via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb install smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk
```

### Method 3: Manual
- Copy APKs to device
- Enable "Install from Unknown Sources"
- Open and install

## 🔧 Configure TCP Connection

### After Installing APKs

1. **Start Simulator:**
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew run
   ```
   - Select "TCP Server"
   - Click "Start Server"
   - Note IP address (e.g., `192.168.52.99`)

2. **In RetroWatch App:**
   - Open app
   - Go to "Watch Control" tab
   - Scroll to "TCP Server (Simulator)" section
   - Enter IP: `192.168.52.99` (or your PC's IP)
   - Enter Port: `8888`
   - Settings save automatically
   - App connects automatically

3. **In SmartGlasses Companion:**
   - Open app
   - Enter IP in "Server IP" field
   - Enter Port in "Server Port" field
   - Click "Connect"

## 📍 APK File Locations

After building, APKs will be at:

- **RetroWatch:** `app/build/outputs/apk/debug/app-debug.apk`
- **SmartGlasses:** `smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

## ⚠️ Troubleshooting

**"SDK location not found":**
- Android Studio should handle this automatically
- If error: **File → Project Structure → SDK Location**
- Verify Android SDK path

**"Build failed":**
- Check **Build** tab for errors
- Try: **Build → Clean Project**, then rebuild
- Or: **File → Invalidate Caches / Restart**

**"TCP connection failed":**
- Verify IP address is correct
- Check Windows Firewall (allow port 8888)
- Ensure Android and PC on same WiFi network

## ✨ Features Added

- ✅ **IP/Port Input UI** - Enter server address in app
- ✅ **Settings Persistence** - Saved automatically
- ✅ **Auto-reconnect** - Reconnects when settings change
- ✅ **User-friendly** - No code changes needed after install

