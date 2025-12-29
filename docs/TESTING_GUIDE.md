# Testing Guide - Android Apps with Desktop Server

## Quick Start

### Step 1: Start Desktop Server

1. **Build the server** (if not already built):
   ```bash
   cd desktop_tools/bluetooth_test_device
   ../../gradlew build
   ```

2. **Run the server**:
   ```bash
   java -jar build/libs/bluetooth_test_device.jar
   ```

3. **Configure Server Mode**:
   - Select **"TCP Server (Android connects to PC)"** radio button
   - Click **"Start Server"**
   - Note the IP address shown in the log (e.g., `192.168.52.99:8888`)

### Step 2: Get Your PC's IP Address

**Windows:**
```cmd
ipconfig
```
Look for "IPv4 Address" under "Wireless LAN adapter Wi-Fi" (usually `192.168.x.x`)

**Common IPs:**
- Wi-Fi: Usually `192.168.1.x` or `192.168.0.x` or `192.168.52.x`
- Your current IP: Check the server log or use `ipconfig`

### Step 3: Configure Android Emulator

**Important:** Android emulator uses special IP addresses to reach the host PC:

- **Android Emulator → Host PC**: Use `10.0.2.2` (this is the emulator's special IP for the host)
- **Physical Android Device → Host PC**: Use your actual Wi-Fi IP (e.g., `192.168.52.99`)

### Step 4: Configure Android Apps

#### SmartGlasses Companion App

1. Open the app
2. Enter server address:
   - **Emulator**: `10.0.2.2`
   - **Physical device**: Your PC's Wi-Fi IP (e.g., `192.168.52.99`)
3. Enter port: `8888`
4. Click **"Start companion service"**
5. Click **"Connect"**

#### RetroWatch App

1. Open the app
2. Go to Watch Control settings
3. Enter TCP Host:
   - **Emulator**: `10.0.2.2`
   - **Physical device**: Your PC's Wi-Fi IP (e.g., `192.168.52.99`)
4. Enter TCP Port: `8888`
5. Click **"Connect"**

### Step 5: Test Connection

#### SmartGlasses Companion:
1. ✅ Connection should show "Connected (TCP)" in status
2. ✅ Click **"Send Message"** - enter text and send
3. ✅ Click **"Send Clock Data"** - should send time
4. ✅ Click **"Send Battery Status"** - should send battery info
5. ✅ Check desktop server log - should show received messages

#### RetroWatch:
1. ✅ Connection should show "Connected" status
2. ✅ Click **"Send Clock Data"** - should send time
3. ✅ Check desktop server log - should show received messages

## Troubleshooting

### Connection Issues

**Problem: "Connection timeout" or "Unable to connect"**
- ✅ Check server is running and shows "Server Running" in green
- ✅ Verify IP address:
  - Emulator: Must use `10.0.2.2`
  - Physical device: Must use PC's actual Wi-Fi IP
- ✅ Check port is `8888` (default)
- ✅ Ensure Android and PC are on the same network (for physical devices)
- ✅ Check Windows Firewall isn't blocking port 8888

**Problem: "Service not running"**
- ✅ Click **"Start companion service"** first (SmartGlasses)
- ✅ Service should show "RUNNING" in status

**Problem: "Not connected to server"**
- ✅ Make sure you clicked **"Connect"** after entering IP/port
- ✅ Check server log shows connection attempt
- ✅ Verify server is in TCP Server mode (not COM Port mode)

### Send Button Issues

**Problem: Buttons don't work / nothing happens**
- ✅ Check service is running
- ✅ Check connection is established
- ✅ Look at Android logcat for error messages:
  ```bash
  adb logcat | grep -i "CompanionService\|RetroWatch"
  ```
- ✅ Check desktop server log for received messages

**Problem: Messages received but not decoded**
- ✅ Server should decode ProtoV2 frames automatically
- ✅ Check server log shows "RECV: TYPE | payload" format
- ✅ If showing raw bytes, check protocol version matches

### Emulator-Specific Issues

**Problem: Emulator can't reach host PC**
- ✅ Use `10.0.2.2` instead of actual IP for emulator
- ✅ This is Android emulator's special IP for host machine
- ✅ Port forwarding: Emulator automatically forwards `10.0.2.2:8888` → `localhost:8888`

**Problem: Physical device can't reach PC**
- ✅ Use actual Wi-Fi IP (from `ipconfig`)
- ✅ Ensure phone and PC are on same Wi-Fi network
- ✅ Check Windows Firewall allows port 8888
- ✅ Try disabling firewall temporarily for testing

## Expected Behavior

### Desktop Server Log

When working correctly, you should see:
```
[13:32:22] Server started on 0.0.0.0:8888
[13:32:22] Server Running
[13:32:25] Client connected from /192.168.52.99:54321
[13:32:26] RECV: STATUS | [1]
[13:32:27] RECV: NOTIFY | Connected to server
[13:32:30] RECV: TIME | [year, month, day, hour, minute, second]
[13:32:35] RECV: NOTIFY | Battery: 85%
```

### Android App Status

**SmartGlasses Companion:**
- Status: "Service: RUNNING"
- Notification: "Connected (TCP)"
- Send buttons should work without errors

**RetroWatch:**
- Connection status: "Connected"
- Send Clock Data button should work
- No "Connecting..." stuck state

## Testing Checklist

- [ ] Desktop server starts and shows "Server Running"
- [ ] Server IP address is visible in log
- [ ] Android app connects successfully
- [ ] SmartGlasses: Send Message button works
- [ ] SmartGlasses: Send Clock Data button works
- [ ] SmartGlasses: Send Battery Status button works
- [ ] RetroWatch: Connect button works
- [ ] RetroWatch: Send Clock Data button works
- [ ] Desktop server receives and decodes messages
- [ ] Messages appear in correct format in server log
- [ ] No connection timeouts or hanging
- [ ] No crashes when sending data

## Advanced Testing

### Test Phone Call Detection
1. Make a test call to the Android device
2. SmartGlasses/RetroWatch should automatically send caller info
3. Check server log for "RECV: CALL | [caller name/number]"

### Test Multiple Connections
1. Connect SmartGlasses Companion
2. Connect RetroWatch (in separate instance or device)
3. Both should work simultaneously
4. Server should handle both connections

### Test Disconnection
1. Click "Disconnect" in Android app
2. Server should show "Client disconnected"
3. Reconnect should work without issues

