# Quick Start Guide

## Prerequisites

- Java 11+ installed
- Bluetooth enabled on your PC
- Android phone paired with your PC

## Step 1: Build the App

```bash
cd bluetooth_test_device
../gradlew build
```

Or double-click: `build_and_run.bat`

## Step 2: Find Your Bluetooth COM Port

1. Open **Device Manager** (Win+X → Device Manager)
2. Expand **Ports (COM & LPT)**
3. Find your Bluetooth device (e.g., "Standard Serial over Bluetooth link (COM3)")
4. Note the COM number (e.g., COM3)

## Step 3: Run the App

Double-click: `run.bat`

Or:
```bash
java -jar build/libs/bluetooth_test_device.jar
```

## Step 4: Connect

1. Click **Refresh** - You should see your COM port in the list
2. Select the COM port (e.g., "COM3 - Standard Serial over Bluetooth link")
3. Click **Connect**
4. Status should turn green: "Status: Connected"

## Step 5: Test with Android App

1. On your Android phone, open **smartglasses_companion** app
2. Pair/connect to the same Bluetooth device
3. Send messages from Android (STATUS, TIME, CALL, NOTIFY)
4. Watch them appear in the desktop app log window
5. Desktop app automatically sends ACK responses

## Troubleshooting

### No COM ports found
- Make sure Bluetooth is enabled on PC
- Pair your Android phone with PC first
- Check Device Manager for COM ports

### Connection fails
- Make sure the COM port isn't already in use
- Try a different COM port
- Restart Bluetooth on PC

### Messages not received
- Verify both devices are connected
- Check baud rate is 9600 (default)
- Make sure Protocol v2 matches on both sides


