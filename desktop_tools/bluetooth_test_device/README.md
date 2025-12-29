# Bluetooth Test Device (Desktop App)

A Java desktop application that simulates the Arduino device for testing the Android smartglasses_companion app.

## Features

- **Protocol v2 implementation** - Same protocol as Android/Arduino
- **GUI interface** - Easy-to-use Swing interface
- **Message logging** - See all received/sent messages
- **Auto-ACK** - Automatically responds to messages requiring ACK

## Building

### Prerequisites

- Java 11 or higher
- Gradle (or use the Gradle wrapper)

### Build

```bash
cd bluetooth_test_device
../gradlew build
```

### Run

```bash
../gradlew run
```

Or build a JAR and run it:

```bash
../gradlew jar
java -jar build/libs/bluetooth_test_device.jar
```

## Bluetooth Connection

**✅ Implemented!** This app uses `jSerialComm` to connect via COM ports.

### How It Works

On Windows, Bluetooth devices often appear as **virtual COM ports**. This app:
1. Scans for all available COM ports
2. Lets you select the one that corresponds to your Bluetooth device
3. Connects at 9600 baud (standard for HC-05/HC-06 modules)

### Finding Your Bluetooth COM Port

1. **Pair your Android phone** with your PC's Bluetooth
2. Open **Device Manager** (Win+X → Device Manager)
3. Look under **Ports (COM & LPT)** for your Bluetooth device
4. Note the COM port number (e.g., COM3, COM4)
5. In the app, click **Refresh** and look for that COM port in the list

### Alternative: Direct Bluetooth (Future)

For direct Bluetooth SPP connection without COM ports, you would need:
- javax.bluetooth (JSR-82) library, or
- Platform-specific JNI wrapper

## Usage

1. **Start the app** - Run `build_and_run.bat` or `java -jar build/libs/bluetooth_test_device.jar`
2. **Pair devices** - Make sure your Android phone is paired with your PC via Bluetooth
3. **Refresh ports** - Click "Refresh" to scan for COM ports (your Bluetooth device should appear)
4. **Select port** - Choose the COM port that corresponds to your Bluetooth device
5. **Connect** - Click "Connect" to establish connection (9600 baud)
6. **Test** - Send messages from Android app and see them in the log
7. **Auto-ACK** - The app automatically sends ACK responses when required

### Testing with Android App

1. On your Android phone, open the **smartglasses_companion** app
2. Make sure the phone is paired with your PC
3. The Android app should connect to the same Bluetooth device
4. When Android sends STATUS/TIME/CALL/NOTIFY messages, you'll see them in the desktop app log
5. The desktop app automatically responds with ACK messages

## Protocol

The app implements Protocol v2 as defined in `smartglasses_companion/PROTOCOL.md`:

- Framed with SOF/EOF
- Byte-stuffed for special characters
- CRC-16 checksum
- Auto-ACK for messages with ACK_REQ flag

## Troubleshooting

- **No devices found**: Make sure Bluetooth is enabled and a library is properly configured
- **Connection fails**: Check that Android app is in pairing mode and device is discoverable
- **Messages not received**: Verify Protocol v2 implementation matches on both sides

