# TCP Server Mode - Testing Guide

## Overview

The desktop app now supports **TCP Server Mode**, which allows Android to connect via TCP instead of Bluetooth. This is useful for testing when Bluetooth setup is complex.

## How It Works

1. **Desktop app** runs a TCP server on port 8888
2. **Android app** connects to the PC's IP address via TCP
3. Messages flow: Android → TCP → Desktop App
4. Protocol v2 is used over TCP (same as Bluetooth)

## Setup Steps

### Step 1: Start Desktop Server

1. Run the desktop app
2. Select **"TCP Server (Android connects to PC)"** radio button
3. Click **"Start Server"**
4. Note the IP address shown in the log (e.g., `192.168.52.99:8888`)

### Step 2: Modify Android App (Temporary)

**Current Issue:** The Android app only supports Bluetooth, not TCP.

**Option A: Quick Test with TCP Bridge**
- Use a Bluetooth-to-TCP bridge tool
- Or modify Android app to support TCP (see below)

**Option B: Add TCP Support to Android App**

Add TCP connection option to `CompanionForegroundService.java`:

```java
// Add TCP socket support alongside Bluetooth
private Socket tcpSocket;
private boolean useTcp = false;
private String tcpHost = "192.168.52.99"; // Your PC IP
private int tcpPort = 8888;

// In connect method, add:
if (useTcp) {
    tcpSocket = new Socket(tcpHost, tcpPort);
    in = tcpSocket.getInputStream();
    out = tcpSocket.getOutputStream();
} else {
    // Existing Bluetooth code
}
```

### Step 3: Connect Android to PC

1. Make sure Android phone and PC are on the same WiFi network
2. In Android app, enter PC's IP address (shown in desktop app log)
3. Connect
4. Messages should flow between Android and desktop app

## Finding Your PC's IP Address

The desktop app shows it in the log, or:

**Windows:**
```cmd
ipconfig
```
Look for "IPv4 Address" under your active network adapter.

**Or check the desktop app log** - it displays the IP when server starts.

## Testing

1. **Desktop app** shows "Server Running" in green
2. **Android app** connects (you'll see "Client connected" in desktop log)
3. Send messages from Android app
4. Watch them appear in desktop app log
5. Desktop app auto-responds with ACK messages

## Advantages

- ✅ No Bluetooth pairing needed
- ✅ Works over WiFi
- ✅ Easy to test protocol
- ✅ Can test from emulator
- ✅ Faster than Bluetooth

## Limitations

- ⚠️ Requires Android app modification (add TCP support)
- ⚠️ Not the same as production (production uses Bluetooth)
- ⚠️ Requires same WiFi network

## Future: True Bluetooth Server

For production-like testing with actual Bluetooth:
- Need to implement Bluetooth SPP server using javax.bluetooth or platform-specific library
- More complex but matches production setup exactly

## Quick Test Without Android Changes

If you don't want to modify Android app, you can:
1. Use a Bluetooth-to-TCP bridge tool
2. Or test with actual Arduino hardware (recommended for production testing)



