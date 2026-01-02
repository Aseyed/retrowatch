# TCP Connection Guide for RetroWatch Simulator

This guide explains how to use TCP connection to test the RetroWatch Android app with the desktop simulator.

## Quick Setup

### Step 1: Start Simulator TCP Server

1. Run the simulator:
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew run
   ```

2. In the simulator:
   - Select "TCP Server" radio button
   - Click "Start Server"
   - Note the IP address shown (e.g., `192.168.52.99:8888`)

### Step 2: Find Your PC's IP Address

**Windows:**
```cmd
ipconfig
```
Look for "IPv4 Address" under your active network adapter (usually starts with 192.168.x.x or 10.x.x.x)

**Or check the simulator log** - it displays the IP when server starts.

### Step 3: Enable TCP Mode in Android App

The Android app needs to be modified to support TCP connections. Two options:

#### Option A: Quick Test (Temporary Code Change)

1. Open `app/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java`

2. Find where `BluetoothManager` is initialized (around line 200-300)

3. Add TCP connection option. You can either:
   - Add a flag to switch between Bluetooth and TCP
   - Or temporarily replace BluetoothManager with TcpConnectionManager

4. Set the TCP address:
   ```java
   // In RetroWatchService initialization
   private static final boolean USE_TCP = true; // Set to true for testing
   private static final String TCP_HOST = "192.168.52.99"; // Your PC IP
   private static final int TCP_PORT = 8888;
   ```

5. Modify connection logic to use TCP when flag is set

#### Option B: Use TcpConnectionManager (Recommended)

1. The `TcpConnectionManager.java` file has been added to the project

2. In `RetroWatchService.java`, add TCP support:
   ```java
   private TcpConnectionManager mTcpManager = null;
   private boolean mUseTcp = false; // Set to true to enable TCP
   
   // In initialization
   if (mUseTcp) {
       mTcpManager = new TcpConnectionManager(mHandler);
       mTcpManager.setTcpAddress("192.168.52.99", 8888); // Your PC IP
   }
   
   // In connect method
   if (mUseTcp && mTcpManager != null) {
       mTcpManager.connect();
   } else {
       // Existing Bluetooth code
   }
   ```

### Step 4: Connect

1. Make sure Android phone and PC are on the **same WiFi network**

2. In Android app, the connection should now use TCP instead of Bluetooth

3. Check simulator log - you should see "Client connected"

4. Send messages from Android app and watch them appear in simulator

## Troubleshooting

### Connection Fails

- **Check IP address**: Make sure you're using the correct PC IP (check with `ipconfig`)
- **Check firewall**: Windows Firewall may block port 8888. Allow it or temporarily disable firewall
- **Check network**: Android and PC must be on same WiFi network
- **Check simulator**: Make sure TCP server is running and shows "Server Running"

### Messages Not Appearing

- **Check protocol**: Simulator uses legacy protocol (0xFC/0xFD), make sure Android app sends correct format
- **Check log**: Look at simulator log panel for "RECV" messages
- **Check display**: Messages should appear in LCD display panel

### Android App Crashes

- **Check permissions**: TCP connection doesn't need Bluetooth permissions, but check for network permissions
- **Check code**: Make sure TcpConnectionManager is properly integrated
- **Check logs**: Use `adb logcat` to see Android app errors

## Reverting to Bluetooth

To switch back to Bluetooth:
1. Set `mUseTcp = false` in RetroWatchService
2. Rebuild and install app
3. Use normal Bluetooth connection

## Notes

- TCP connection is **only for testing** - production uses Bluetooth
- TCP works over WiFi, so both devices must be on same network
- No pairing needed with TCP (unlike Bluetooth)
- TCP is faster than Bluetooth for testing

## Example Integration

Here's a minimal example of adding TCP support to RetroWatchService:

```java
// Add at top of class
private static final boolean USE_TCP_FOR_TESTING = true; // Change to false for Bluetooth
private static final String TCP_HOST = "192.168.52.99"; // Your PC IP
private static final int TCP_PORT = 8888;

private TcpConnectionManager mTcpManager;

// In onCreate or initialization
if (USE_TCP_FOR_TESTING) {
    mTcpManager = new TcpConnectionManager(mHandler);
    mTcpManager.setTcpAddress(TCP_HOST, TCP_PORT);
} else {
    // Existing BluetoothManager initialization
}

// In connect method
if (USE_TCP_FOR_TESTING && mTcpManager != null) {
    mTcpManager.connect();
} else if (mBtManager != null) {
    // Existing Bluetooth connection code
}
```

