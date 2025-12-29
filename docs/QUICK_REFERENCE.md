# Quick Reference Guide

## 📁 Project Structure

```
retrowatch/
├── android_apps/          # Android Applications
├── desktop_tools/         # Desktop Java Tools  
├── hardware/              # Arduino Firmware
├── legacy/                # Legacy Code
└── docs/                  # Documentation
```

## 🚀 Quick Commands

### Build Android APKs

**In Android Studio:**
- **Build → Build APK(s)**
- APKs in: `android_apps/*/build/outputs/apk/debug/*-debug.apk`

**Via Gradle:**
```bash
./gradlew :android_apps:retrowatch:assembleDebug
./gradlew :android_apps:smartglasses_companion:assembleDebug
```

### Run Desktop Simulator

```bash
cd desktop_tools/arduino_simulator
../../gradlew run
```

### Run Bluetooth Test Device

```bash
cd desktop_tools/bluetooth_test_device
../../gradlew run
```

## 📱 APK Locations

After building:
- **RetroWatch:** `android_apps/retrowatch/build/outputs/apk/debug/app-debug.apk`
- **SmartGlasses:** `android_apps/smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

## 🔧 TCP Connection Setup

1. **Start Simulator:**
   - Run `desktop_tools/arduino_simulator`
   - Select "TCP Server" mode
   - Note IP address

2. **Configure Android App:**
   - Open app
   - Enter IP in TCP settings
   - Enter port (8888)
   - Connect

## 📚 Documentation

All docs in `docs/` folder:
- `BUILD_WITH_ANDROID_STUDIO.md` - Build instructions
- `FOLDER_STRUCTURE.md` - Detailed structure
- `REORGANIZATION_COMPLETE.md` - Reorganization details

