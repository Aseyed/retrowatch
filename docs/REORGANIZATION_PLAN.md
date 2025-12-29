# Project Reorganization Plan

## New Structure

```
retrowatch/
├── android_apps/
│   ├── retrowatch/              # RetroWatch Android App (from app/)
│   └── smartglasses_companion/  # SmartGlasses Companion (from smartglasses_companion/)
├── desktop_tools/
│   ├── arduino_simulator/       # Arduino Simulator (from retrowatch_arduino_simulator/)
│   └── bluetooth_test_device/   # Bluetooth Test Device (from bluetooth_test_device/)
├── hardware/
│   └── arduino/                  # Arduino Source Code (from RetroWatch_Arduino/)
├── legacy/                       # Legacy Code (from RetroWatch_Android/)
├── docs/                         # Documentation files
└── scripts/                      # Build scripts (from scripts/)
```

## Benefits

- ✅ Clear separation: Android apps, desktop tools, hardware code
- ✅ Easier to navigate
- ✅ Better organization for development
- ✅ Each category is self-contained

## Migration Steps

1. Create new folder structure
2. Move files to new locations
3. Update build.gradle files
4. Update settings.gradle
5. Update import paths if needed
6. Test builds

