# RetroWatch TCP Connection Fix

## Issue Fixed

**Problem:** App shows "Connected" toast but server doesn't see connection and can't send data.

**Root Causes:**
1. No null checks before using input/output streams
2. Socket reference not properly stored
3. Silent failures when streams couldn't be created
4. No validation that socket is actually connected before declaring success

## Changes Made

### 1. Added Null Checks
- Check if streams are null before reading/writing
- Validate socket is connected before using it
- Better error handling throughout

### 2. Improved Logging
- Added detailed logging at each step
- Log when data is written
- Log connection state changes
- Log errors with full context

### 3. Better Error Handling
- Validate streams after creation
- Check socket state before operations
- Proper cleanup on errors
- Flush output stream to ensure data is sent

### 4. Fixed Socket Reference
- Properly store socket reference in `connected()`
- Use local socket reference in `ConnectThread`
- Validate socket state before operations

## Testing Steps

1. **Rebuild the app:**
   ```bash
   ./gradlew :android_apps:retrowatch:assembleDebug
   ```

2. **Reinstall on device**

3. **Start simulator:**
   ```bash
   cd desktop_tools/arduino_simulator
   ../../gradlew run
   ```
   - Select "TCP Server"
   - Click "Start Server"
   - Note IP address (e.g., `192.168.52.99`)

4. **In RetroWatch app:**
   - Go to "Watch Control" tab
   - Enter server IP address
   - Enter port (8888)
   - Settings save automatically
   - App should reconnect automatically

5. **Check logs:**
   ```bash
   adb logcat | grep -E "TcpConnectionManager|RetroWatch"
   ```

## Expected Behavior

### Success Indicators:
- ✅ Toast: "Connected to TCP Simulator"
- ✅ Simulator log: "Client connected"
- ✅ App can send messages (notifications appear in simulator)
- ✅ Simulator can receive data (see "RECV" messages in log)

### Log Messages to Look For:
```
TcpConnectionManager: connect to 192.168.52.99:8888
TcpConnectionManager: BEGIN mConnectThread - connecting to 192.168.52.99:8888
TcpConnectionManager: Socket created, calling connected()
TcpConnectionManager: connected to 192.168.52.99:8888
TcpConnectionManager: Streams created successfully
TcpConnectionManager: Connection established and thread started
TcpConnectionManager: BEGIN mConnectedThread
```

### When Sending Data:
```
TcpConnectionManager: Writing X bytes
TcpConnectionManager: Wrote X bytes
```

## Troubleshooting

### Still shows "Connected" but no data?

1. **Check logs for errors:**
   ```bash
   adb logcat | grep -E "TcpConnectionManager|Exception|Error"
   ```

2. **Check if streams are null:**
   - Look for "Streams are null" in logs
   - If seen, connection failed silently

3. **Check socket state:**
   - Look for "Socket is not connected" in logs
   - May indicate connection was closed immediately

4. **Verify simulator is receiving:**
   - Check simulator log panel
   - Should see "Client connected" message
   - Should see "RECV" messages when app sends data

### Connection fails immediately?

1. **Check network:**
   - Both devices on same WiFi?
   - Can ping PC from Android device?
   - Firewall blocking port 8888?

2. **Check IP address:**
   - Is IP address correct?
   - Is port correct (8888)?
   - Try `telnet [IP] 8888` from Android device (if available)

3. **Check simulator:**
   - Is TCP server started?
   - Is it listening on correct port?
   - Any errors in simulator log?

### Data not sending?

1. **Check connection state:**
   - Look for "Cannot write - not connected" in logs
   - App may think it's connected but actually isn't

2. **Check buffer:**
   - Look for "Cannot write - buffer is null" in logs
   - Transaction may not be building correctly

3. **Check write errors:**
   - Look for "Exception during write" in logs
   - May indicate connection was lost

## Debug Commands

**View all TCP-related logs:**
```bash
adb logcat | grep TcpConnectionManager
```

**View connection state changes:**
```bash
adb logcat | grep "setState\|connected\|disconnected"
```

**View write operations:**
```bash
adb logcat | grep "Writing\|Wrote"
```

**Test TCP connection from PC:**
```bash
telnet [ANDROID_DEVICE_IP] 8888
# Or
nc [ANDROID_DEVICE_IP] 8888
```

## Notes

- TCP connection is **only for testing** - production uses Bluetooth
- Make sure `USE_TCP_FOR_TESTING = true` in `RetroWatchService.java`
- Rebuild and reinstall after code changes
- Check Android Studio Logcat for detailed error messages
- Simulator must be running before app connects

