# RetroWatch Project

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Open source smart watch project with Android apps, Arduino hardware, and desktop simulation tools.

## Project Structure

```
retrowatch/
├── android_apps/              # Android Applications
│   ├── retrowatch/           # RetroWatch Android App (Main)
│   └── smartglasses_companion/ # SmartGlasses Companion App
├── desktop_tools/             # Desktop Development Tools
│   ├── arduino_simulator/    # Arduino Simulator (Java)
│   └── bluetooth_test_device/ # Bluetooth Test Device (Java)
├── hardware/                  # Hardware Source Code
│   └── arduino/              # Arduino Firmware
├── legacy/                    # Legacy Code
│   └── RetroWatch_Android/   # Old Android implementations
├── docs/                      # Documentation
└── scripts/                   # Build Scripts
```

## Quick Start

### Android Apps

**Build APKs:**
```bash
# In Android Studio: Build → Build APK(s)
# Or via Gradle:
./gradlew :android_apps:retrowatch:assembleDebug
./gradlew :android_apps:smartglasses_companion:assembleDebug
```

**APK Locations:**
- RetroWatch: `android_apps/retrowatch/build/outputs/apk/debug/app-debug.apk`
- SmartGlasses: `android_apps/smartglasses_companion/build/outputs/apk/debug/smartglasses_companion-debug.apk`

### Desktop Simulator

**Run Simulator:**
```bash
cd desktop_tools/arduino_simulator
../../gradlew run
```

### Arduino Hardware

**Arduino Code:**
- Location: `hardware/arduino/`
- Main: `hardware/arduino/RetroWatchArduino_ProMicro/`

## Features

- ✅ **Android Apps** - RetroWatch and SmartGlasses Companion
- ✅ **TCP Support** - Connect to simulator via TCP (IP/Port input in UI)
- ✅ **Arduino Simulator** - Desktop simulator for testing
- ✅ **Bluetooth Support** - Production Bluetooth connectivity
- ✅ **Arduino Firmware** - Multiple hardware variants

## Documentation

Comprehensive documentation is available in the `docs/` folder:

- **[Documentation Index](docs/INDEX.md)** - Complete documentation index
- **Build Guides** - See `docs/build/` for build instructions
- **Testing** - See `docs/testing/` for testing guides
- **Troubleshooting** - See `docs/troubleshooting/` for connection issues
- **Android Apps** - See `docs/android_apps/` for app-specific documentation

Quick links:
- [Quick Reference](docs/QUICK_REFERENCE.md)
- [Build APK Guide](docs/build/BUILD_APK_GUIDE.md)
- [Testing Guide](docs/testing/TESTING_GUIDE.md)

## Building

### Prerequisites
- Android Studio (for Android apps)
- Java 11+ (for desktop tools)
- Gradle (included)

### Android Apps
Open in Android Studio and build, or use Gradle commands above.

### Desktop Tools
```bash
cd desktop_tools/arduino_simulator
../../gradlew build
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Topics

This project uses the following technologies:
- Arduino
- Android
- Bluetooth
- IoT
- Wearable Technology

