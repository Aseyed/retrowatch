# SmartGlasses Companion (Android)

This module is a clean, modern Android companion app intended to talk to an Arduino Pro Micro (ATmega32U4) over **Bluetooth Classic SPP** (e.g., HC-05/HC-06).

## What’s in here

- **Foreground service**: `com.hardcopy.smartglasses.service.CompanionForegroundService`
- **Protocol v2**: `com.hardcopy.smartglasses.protocol.ProtoV2` (framed, escaped, CRC16)
- **Notification listener stub**: `NotificationBridgeService` (hook point for SMS/app notifications)

## How to run

- This repo build requires a working JDK/Android toolchain. If Gradle complains about `JAVA_HOME`, install a JDK (17 recommended for AGP 8.x) and set `JAVA_HOME`.
- Build the module: `./gradlew :smartglasses_companion:assembleDebug`

## Next implementation steps

- Add UI for choosing a paired BT device + persist its MAC address.
- Start service on boot only after user opt-in.
- Implement forwarding from `NotificationBridgeService` to `CompanionForegroundService` via a small event queue.
- Add TIME + CALL support (currently only STATUS/NOTIFY helpers exist in the service).



