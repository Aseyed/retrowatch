# TCP Connection Troubleshooting

## Issue: "Nothing happens when clicking Connect"

### ✅ Fixed Issues

1. **Missing INTERNET Permission** - Added to AndroidManifest.xml
2. **No User Feedback** - Added Toast messages and better error logging
3. **Silent Failures** - Added detailed logging for debugging

### 🔍 What to Check

#### 1. Make Sure Service is Running

Before connecting, make sure the service is started:
- Click **"Start Service"** button first
- You should see "Service: RUNNING" in the status

#### 2. Verify Simulator is Running

**On your PC:**
```bash
cd desktop_tools/arduino_simulator
../../gradlew run
```

**In Simulator:**
- Select **"TCP Server"** radio button
- Click **"Start Server"**
- Note the IP address shown (e.g., `192.168.1.100:8888`)

#### 3. Check IP Address and Port

**In SmartGlasses App:**
- Enter the **exact IP address** from the simulator (e.g., `192.168.1.100`)
- Enter the **port** (usually `8888`)
- Click **"Connect"**

#### 4. Check Network Connection

**Both devices must be on the same WiFi network:**
- Android device and PC must be on the same WiFi
- Check WiFi settings on both devices

**Find your PC's IP:**
- Windows: `ipconfig` (look for IPv4 Address)
- Linux/Mac: `ifconfig` or `ip addr`

#### 5. Check Firewall

**Windows Firewall might block the connection:**
- Allow port 8888 in Windows Firewall
- Or temporarily disable firewall for testing

#### 6. Check Logs

**View Android logs:**
```bash
adb logcat | grep -E "TcpConnectionHelper|CompanionService|SmartGlasses"
```

**Look for:**
- `Connecting to 192.168.1.100:8888`
- `Connected successfully` (success)
- `Connection failed` (failure with error message)

### 🐛 Common Issues

#### "Connection failed" in notification
- **Cause:** Can't reach the server
- **Fix:** 
  - Check IP address is correct
  - Check simulator is running
  - Check firewall settings
  - Check both devices on same WiFi

#### "TCP connection failed" message
- **Cause:** Socket connection refused
- **Fix:**
  - Verify simulator TCP server is started
  - Check port number matches (8888)
  - Try restarting simulator

#### No response at all
- **Cause:** Service might not be running
- **Fix:**
  - Click "Start Service" button first
  - Wait for "Service: RUNNING" status
  - Then click "Connect"

### ✅ Success Indicators

When connection works, you should see:
1. **Toast message:** "Connecting to 192.168.1.100:8888..."
2. **Notification updates:** "Connecting TCP..." → "Connected (TCP)"
3. **In simulator:** "Client connected" message in log panel
4. **Status:** Service notification shows "Connected (TCP)"

### 📱 Testing Steps

1. **Start Simulator:**
   ```bash
   cd desktop_tools/arduino_simulator
   ../../gradlew run
   ```
   - Select "TCP Server"
   - Click "Start Server"
   - Note IP address

2. **Start Service in App:**
   - Open SmartGlasses Companion app
   - Click "Start Service" button
   - Wait for "Service: RUNNING"

3. **Enter Connection Info:**
   - Enter IP address (from simulator)
   - Enter port (8888)
   - Click "Connect"

4. **Verify Connection:**
   - Check notification: Should show "Connected (TCP)"
   - Check simulator log: Should show "Client connected"
   - Try sending a notification to test

### 🔧 Debug Commands

**Check if service is running:**
```bash
adb shell dumpsys activity services | grep CompanionForegroundService
```

**View connection logs:**
```bash
adb logcat -s TcpConnectionHelper CompanionService
```

**Test TCP connection from PC:**
```bash
telnet 192.168.1.100 8888
# Or
nc 192.168.1.100 8888
```

If this connects, the server is reachable. If not, check firewall/network.

### 📝 Notes

- TCP connection is **only for testing** - production uses Bluetooth
- Make sure `USE_TCP_FOR_TESTING = true` in `CompanionForegroundService.java`
- Rebuild and reinstall the app after code changes
- Check Android Studio Logcat for detailed error messages

