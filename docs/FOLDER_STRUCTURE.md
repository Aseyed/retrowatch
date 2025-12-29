# Project Folder Structure

## Overview

The project has been reorganized into a clear, logical structure for better maintainability.

## Directory Structure

```
retrowatch/
│
├── android_apps/                    # 📱 Android Applications
│   ├── retrowatch/                  # RetroWatch Main App
│   │   ├── src/main/
│   │   │   ├── java/                # Java source code
│   │   │   ├── res/                 # Resources (layouts, strings, etc.)
│   │   │   └── AndroidManifest.xml
│   │   ├── build.gradle             # App build configuration
│   │   └── build/outputs/apk/       # APK output (after build)
│   │
│   └── smartglasses_companion/      # SmartGlasses Companion App
│       ├── src/main/
│       │   ├── java/
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       ├── build.gradle
│       └── build/outputs/apk/       # APK output (after build)
│
├── desktop_tools/                   # 🖥️ Desktop Development Tools
│   ├── arduino_simulator/           # Arduino Simulator (Java)
│   │   ├── src/main/java/          # Simulator source code
│   │   ├── build.gradle
│   │   ├── settings.gradle         # Standalone project
│   │   └── build/libs/             # JAR output (after build)
│   │
│   └── bluetooth_test_device/        # Bluetooth Test Device (Java)
│       ├── src/main/java/
│       ├── build.gradle
│       ├── settings.gradle         # Standalone project
│       └── build/libs/             # JAR output (after build)
│
├── hardware/                        # 🔧 Hardware/Firmware
│   └── arduino/                     # Arduino Source Code
│       ├── RetroWatchArduino_ProMicro/    # ProMicro variant
│       ├── RetroWatchArduino/             # Standard variant
│       ├── RetroWatchArduino_spi/         # SPI variant
│       ├── RetroWatchArduino_u8glib/      # u8glib variant
│       └── SmartGlasses_ProMicro_v2/      # SmartGlasses variant
│
├── legacy/                          # 📦 Legacy Code
│   └── RetroWatch_Android/         # Old Android implementations
│       ├── RetroWatch/              # Legacy RetroWatch
│       └── RetroWatchLE/           # Legacy RetroWatch LE
│
├── docs/                            # 📚 Documentation
│   ├── BUILD_WITH_ANDROID_STUDIO.md
│   ├── BUILD_APKS_NOW.md
│   ├── PROJECT_ORGANIZATION.md
│   ├── REORGANIZATION_COMPLETE.md
│   └── ... (all documentation files)
│
├── scripts/                          # 🔨 Build Scripts
│   └── android_smoke_startup.sh
│
├── build.gradle                      # Root build configuration
├── settings.gradle                  # Gradle module settings
├── gradle.properties                # Gradle properties
├── local.properties                 # Local SDK paths
├── gradlew                          # Gradle wrapper
└── README.md                        # Main project README
```

## Project Categories

### Android Apps (`android_apps/`)
- **Purpose:** Android applications for RetroWatch ecosystem
- **Build:** Android Studio or Gradle
- **Output:** APK files
- **Modules:**
  - `retrowatch/` - Main RetroWatch Android app
  - `smartglasses_companion/` - SmartGlasses Companion app

### Desktop Tools (`desktop_tools/`)
- **Purpose:** Desktop Java applications for development/testing
- **Build:** Gradle (Java projects)
- **Output:** JAR files
- **Modules:**
  - `arduino_simulator/` - Arduino device simulator
  - `bluetooth_test_device/` - Bluetooth protocol test tool

### Hardware (`hardware/`)
- **Purpose:** Firmware and hardware source code
- **Build:** Arduino IDE
- **Output:** .hex files (for Arduino)
- **Contents:**
  - Arduino firmware variants for different hardware configurations

### Legacy (`legacy/`)
- **Purpose:** Old/legacy code (reference only)
- **Status:** Not actively maintained
- **Contents:**
  - Old Android app implementations

### Documentation (`docs/`)
- **Purpose:** All project documentation
- **Contents:**
  - Build guides
  - Setup instructions
  - Architecture docs
  - API documentation

## Building Projects

### Android Apps
```bash
# Build RetroWatch
./gradlew :android_apps:retrowatch:assembleDebug

# Build SmartGlasses
./gradlew :android_apps:smartglasses_companion:assembleDebug

# Build Both
./gradlew :android_apps:retrowatch:assembleDebug :android_apps:smartglasses_companion:assembleDebug
```

### Desktop Tools
```bash
# Arduino Simulator
cd desktop_tools/arduino_simulator
../../gradlew build

# Bluetooth Test Device
cd desktop_tools/bluetooth_test_device
../../gradlew build
```

## File Paths Reference

### APK Files (After Build)
- RetroWatch: `android_apps/retrowatch/build/outputs/apk/debug/app-debug.apk`
- SmartGlasses: `android_apps/smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### JAR Files (After Build)
- Simulator: `desktop_tools/arduino_simulator/build/libs/retrowatch_arduino_simulator-1.0.0.jar`
- Test Device: `desktop_tools/bluetooth_test_device/build/libs/bluetooth_test_device.jar`

### Source Code Locations
- RetroWatch Java: `android_apps/retrowatch/src/main/java/`
- SmartGlasses Java: `android_apps/smartglasses_companion/src/main/java/`
- Simulator Java: `desktop_tools/arduino_simulator/src/main/java/`
- Arduino Code: `hardware/arduino/RetroWatchArduino_ProMicro/`

## Benefits of New Structure

1. **Clear Separation** - Each project type in its own folder
2. **Easy Navigation** - Find what you need quickly
3. **Better Organization** - Related projects grouped together
4. **Maintainable** - Easier to manage and update
5. **Scalable** - Easy to add new projects

## Migration Notes

- All build configurations updated
- Gradle settings updated for new paths
- Android Studio will auto-detect structure
- No code changes needed (package names unchanged)

