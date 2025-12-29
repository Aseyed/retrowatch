# Arduino Simulator Server Fixes - Data Reception Issues

## Issues Found and Fixed

### 1. ✅ No Socket Timeout
**Problem:** `inputStream.read()` could block indefinitely if client sent data slowly, causing the server to hang.

**Fix:** Added socket timeout and timeout exception handling:
```java
clientSocket.setSoTimeout(30000); // 30 second read timeout
```

### 2. ✅ Race Condition with Read Thread
**Problem:** When a new client connected, the old read thread might still be reading from the old inputStream, causing data corruption or missed frames.

**Fix:** Properly stop old read thread before starting new one:
```java
if (clientThread != null && clientThread.isAlive()) {
    clientThread.interrupt();
    clientThread.join(500);
}
```

### 3. ✅ No Raw Byte Logging
**Problem:** Server didn't log raw bytes before parsing, making it impossible to debug when frames weren't being decoded.

**Fix:** Added raw byte logging in both TcpServerBridge and MainWindow:
```java
System.out.println("[TcpServerBridge] Received " + read + " bytes");
log("RAW", hex.toString().trim()); // In MainWindow
```

### 4. ✅ Protocol Parser Not Reset on New Connection
**Problem:** When a new client connected, the protocol parser retained partial frame state from the previous connection, causing frame parsing errors.

**Fix:** Added connection callback to reset parser when new client connects:
```java
tcpServerBridge.setConnectionCallback(this::onClientConnected);
// In onClientConnected():
protocolParser.reset();
```

### 5. ✅ Better Error Handling
**Problem:** Socket timeout exceptions weren't handled, causing read thread to exit unexpectedly.

**Fix:** Added timeout exception handling:
```java
catch (java.net.SocketTimeoutException e) {
    // Timeout is normal - just continue reading
    continue;
}
```

### 6. ✅ Better Socket Configuration
**Problem:** Socket wasn't configured for optimal performance.

**Fix:** Added socket options:
```java
clientSocket.setTcpNoDelay(true); // Disable Nagle's algorithm
clientSocket.setKeepAlive(true); // Enable keep-alive
```

### 7. ✅ Handle Zero-Byte Reads
**Problem:** `read()` returning 0 bytes wasn't handled, which could cause issues.

**Fix:** Added handling for zero-byte reads:
```java
else if (read == 0) {
    System.out.println("[TcpServerBridge] Read returned 0 bytes");
    Thread.sleep(100); // Small delay to avoid tight loop
}
```

## Testing After Fixes

After rebuilding the simulator, you should see:

1. **Better logging:**
   ```
   [TcpServerBridge] Read thread started - waiting for data...
   [TcpServerBridge] Received 10 bytes
   RECV: Received 10 bytes
   RAW: FC 31 07 0E 0B 1F 17 FD
   CMD: SET_TIME
   ```

2. **Parser reset on connection:**
   ```
   Client connected - parser reset
   ```

3. **No hanging:** Read thread handles timeouts gracefully

4. **Clean reconnection:** New connections properly reset parser state

## Rebuild Required

The simulator has been rebuilt with all fixes:
```bash
cd desktop_tools/arduino_simulator
../../gradlew build
java -jar build/libs/retrowatch_arduino_simulator-1.0.0.jar
```

## Expected Behavior

- ✅ Server receives raw bytes and logs them
- ✅ Protocol parser processes frames correctly
- ✅ Commands appear in log as "CMD: COMMAND_NAME"
- ✅ No hanging or blocking
- ✅ Clean reconnection when client disconnects/reconnects
- ✅ Parser state reset on new connection

## Debugging Tips

If frames aren't being parsed correctly:

1. **Check raw bytes:** Look for "RAW: ..." in log to see what was received
2. **Check parser state:** Parser should reset on each new connection
3. **Check protocol:** Verify bytes match expected format:
   - Start: `0xFC`
   - Command: `0x31` (SET_TIME), `0x12` (ADD_NORMAL_OBJ), etc.
   - End: `0xFD`
4. **Check for partial frames:** If parser doesn't reset, partial frames from previous connection can corrupt new ones

