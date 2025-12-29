# Quick Start: TCP Connection (No COM Port Needed)

Since you don't have COM ports available, use **TCP Server mode** to connect your Android app to the simulator.

## Step 1: Start Simulator TCP Server

1. **Run the simulator:**
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew run
   ```

2. **In the simulator window:**
   - Select **"TCP Server"** radio button (not COM Port)
   - Click **"Start Server"**
   - You'll see: `Status: Server Running (192.168.x.x:8888)`
   - **Note the IP address** shown (e.g., `192.168.1.100`)

## Step 2: Find Your PC's IP Address

The simulator shows it in the status, or:

**Windows:**
```cmd
ipconfig
```
Look for "IPv4 Address" (usually `192.168.x.x` or `10.x.x.x`)

## Step 3: Enable TCP in Android App

The Android app (`app/` folder) currently only supports Bluetooth. You need to add TCP support:

### Quick Method:

1. **Open:** `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`

2. **Find the BluetoothManager initialization** (around line 200-300)

3. **Add TCP support** - See `app/TCP_CONNECTION_GUIDE.md` for detailed instructions

4. **Set your PC's IP address:**
   ```java
   private static final String TCP_HOST = "192.168.1.100"; // Change to your PC's IP
   private static final int TCP_PORT = 8888;
   ```

5. **Rebuild and install the Android app**

## Step 4: Connect

1. **Make sure Android and PC are on the same WiFi network**

2. **In Android app:** Connect (it will now use TCP instead of Bluetooth)

3. **In simulator:** You should see "Client connected" in the log

4. **Test:** Send messages from Android app → Watch them appear in simulator LCD display

## Troubleshooting

**"Connection failed" in Android:**
- Check PC IP address is correct
- Check Windows Firewall (allow port 8888)
- Make sure Android and PC are on same WiFi

**"No client connected" in simulator:**
- Make sure TCP server is running (green status)
- Check Android app is trying to connect
- Check Android app logs with `adb logcat`

**Messages not appearing:**
- Check simulator log panel for "RECV" messages
- Verify protocol format matches (legacy 0xFC/0xFD)

## Alternative: Use Bluetooth-to-TCP Bridge

If you don't want to modify the Android app, you can use a bridge tool:
- Install a Bluetooth-to-TCP bridge on your PC
- Configure it to forward Bluetooth connections to TCP port 8888
- Android app connects via Bluetooth → Bridge forwards to simulator

But modifying the Android app is simpler for testing.

## Notes

- TCP connection is **only for testing** - production uses Bluetooth
- No pairing needed with TCP
- Works over WiFi (both devices must be on same network)
- Faster than Bluetooth for development

