# RetroWatch Connect Button Fix

## Issue Fixed

**Problem:** Connect button not working and app not connecting to server.

**Root Causes:**
1. TCP manager might not be initialized when connect button is clicked
2. Service handler might be null
3. TCP settings might not be loaded from preferences
4. Service might not be fully set up when button is clicked

## Changes Made

### 1. Enhanced `connectDevice()` Method
- **Loads TCP settings** from preferences before connecting
- **Validates settings** and uses defaults if invalid
- **Creates service handler** if null
- **Initializes TCP manager** if needed
- **Sets up transaction builder/receiver** if needed
- **Better error handling** and logging

### 2. Improved Activity Callback Handler
- **Checks if service is bound** before connecting
- **Calls `setupService()`** to ensure service is ready
- **Better error messages** for user

### 3. Added Logging
- Detailed logs at each step
- Error logs with full context
- Connection state logging

## How It Works Now

1. **User clicks Connect button**
2. **Activity checks** if service is available
3. **If service not bound**, tries to start it
4. **Calls `setupService()`** to ensure handler is set up
5. **Service loads TCP settings** from preferences
6. **Validates and uses defaults** if needed
7. **Creates TCP manager** if it doesn't exist
8. **Updates TCP address** from current settings
9. **Calls `connect()`** on TCP manager

## Testing Steps

1. **Enter TCP settings:**
   - Go to "Watch Control" tab
   - Enter server IP (e.g., `192.168.1.100`)
   - Enter port (e.g., `8888`)
   - Settings save automatically

2. **Start simulator:**
   ```bash
   cd desktop_tools/arduino_simulator
   ../../gradlew run
   ```
   - Select "TCP Server"
   - Click "Start Server"

3. **Click Connect button:**
   - Should show "Connecting..." toast
   - Should connect to server
   - Should show "Connected to TCP Simulator" toast

4. **Check logs:**
   ```bash
   adb logcat | grep -E "RetroWatchService|TcpConnectionManager"
   ```

## Expected Log Messages

**On Connect:**
```
RetroWatchService: connectDevice() called - USE_TCP_FOR_TESTING=true
RetroWatchService: Connecting via TCP to 192.168.1.100:8888
RetroWatchService: Creating new TcpConnectionManager (if needed)
TcpConnectionManager: connect to 192.168.1.100:8888
TcpConnectionManager: Socket created, calling connected()
TcpConnectionManager: connected to 192.168.1.100:8888
TcpConnectionManager: Connection established and thread started
```

## Troubleshooting

### Still not connecting?

1. **Check service is running:**
   - Look for notification "Retro Watch Service"
   - Check logs for service startup

2. **Check TCP settings:**
   - Verify IP and port are correct
   - Check settings are saved (they save automatically)

3. **Check simulator:**
   - Is TCP server started?
   - Is it listening on correct port?
   - Check simulator log for "Client connected"

4. **Check network:**
   - Both devices on same WiFi?
   - Can ping PC from Android device?
   - Firewall blocking port?

5. **Check logs:**
   ```bash
   adb logcat | grep -E "RetroWatchService|TcpConnectionManager|Error|Exception"
   ```

### Common Issues

**"Service not available" toast:**
- Service might not be bound yet
- Wait a few seconds and try again
- Or restart the app

**No connection but no error:**
- Check if TCP manager was created
- Check if `connect()` was called
- Check logs for connection attempts

**Connection fails immediately:**
- Check IP address is correct
- Check port is correct
- Check simulator is running
- Check firewall settings

## Notes

- Settings are saved automatically when you type in the fields
- Service must be running before connecting
- TCP manager is created automatically if needed
- Default values: IP=`192.168.1.100`, Port=`8888`

