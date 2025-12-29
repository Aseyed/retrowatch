# Server Code Fixes - Data Reception Issues

## Issues Found and Fixed

### 1. ✅ Decoder Not Reset on New Connection
**Problem:** When a new client connected, the decoder retained partial frame state from the previous connection, causing frame parsing errors.

**Fix:** Added `decoder.reset()` when a new client connects to clear any partial frame state.

```java
// Reset decoder for new connection (clear any partial frame state)
decoder.reset();
```

### 2. ✅ No Socket Timeout
**Problem:** `inputStream.read()` could block indefinitely if client sent data slowly, causing the server to hang.

**Fix:** Added socket timeout and timeout exception handling:
```java
clientSocket.setSoTimeout(30000); // 30 second read timeout
```

### 3. ✅ Race Condition with Read Thread
**Problem:** When a new client connected, the old read thread might still be reading from the old inputStream, causing data corruption or missed frames.

**Fix:** Properly stop old read thread before starting new one:
```java
if (readThread != null && readThread.isAlive()) {
    readThread.interrupt();
    readThread.join(500);
}
```

### 4. ✅ No Raw Byte Logging
**Problem:** Server didn't log raw bytes before decoding, making it impossible to debug when frames weren't being decoded.

**Fix:** Added raw byte logging:
```java
gui.log("[RAW] Received " + bytesRead + " bytes");
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

### 6. ✅ Better Payload Display
**Problem:** Payload display failed for non-UTF-8 data (like TIME messages with binary data).

**Fix:** Improved payload string conversion to handle binary data:
```java
try {
    payloadStr = new String(payload, StandardCharsets.UTF_8);
} catch (Exception e) {
    // Show hex representation for binary data
    payloadStr = "[" + hex.toString().trim() + "]";
}
```

### 7. ✅ Better Decoder Error Messages
**Problem:** Decoder silently ignored bad frames, making debugging impossible.

**Fix:** Added error logging for frame parsing issues:
```java
System.err.println("[ProtoV2] Frame too short: " + body.length + " bytes");
System.err.println("[ProtoV2] Bad version: 0x" + String.format("%02X", ver));
System.err.println("[ProtoV2] Length mismatch: got " + body.length + ", expected " + expected);
System.err.println("[ProtoV2] CRC mismatch: read=0x" + String.format("%04X", crcRead) + ", calc=0x" + String.format("%04X", crcCalc));
```

### 8. ✅ Socket Configuration
**Problem:** Socket wasn't configured for optimal performance.

**Fix:** Added socket options:
```java
clientSocket.setTcpNoDelay(true); // Disable Nagle's algorithm
clientSocket.setKeepAlive(true); // Enable keep-alive
```

## Testing After Fixes

After rebuilding the server, you should see:

1. **Better logging:**
   ```
   [RAW] Received 10 bytes
   [RAW] Received 28 bytes
   RECV: STATUS | [1]
   RECV: NOTIFY | Connected to server
   ```

2. **Error messages if frames are malformed:**
   ```
   [ProtoV2] Frame too short: 5 bytes (min 7)
   [ProtoV2] Bad version: 0x02 (expected 0x01)
   [ProtoV2] CRC mismatch: read=0x1234, calc=0x5678
   ```

3. **No hanging:** Read thread handles timeouts gracefully

4. **Clean reconnection:** New connections properly reset decoder state

## Rebuild Required

After these fixes, rebuild the server:
```bash
cd desktop_tools/bluetooth_test_device
../../gradlew build
java -jar build/libs/bluetooth_test_device.jar
```

## Expected Behavior

- ✅ Server receives raw bytes and logs them
- ✅ Decoder processes frames correctly
- ✅ Messages appear in log as "RECV: TYPE | payload"
- ✅ No hanging or blocking
- ✅ Clean reconnection when client disconnects/reconnects
- ✅ Error messages help debug protocol issues

