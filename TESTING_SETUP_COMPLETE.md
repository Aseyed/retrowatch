# ✅ Testing Setup Complete

## What's Ready

1. ✅ **Desktop Server Built** - `bluetooth_test_device` is compiled and ready
2. ✅ **IP Address Identified** - Your PC's Wi-Fi IP: `192.168.52.99`
3. ✅ **Testing Guides Created** - See `docs/TESTING_GUIDE.md` and `QUICK_TEST_START.md`

## 🎯 Next Steps - Start Testing

### Step 1: Start Desktop Server

**Option A: Double-click (Windows)**
```
desktop_tools\bluetooth_test_device\run.bat
```

**Option B: Command line**
```bash
cd desktop_tools/bluetooth_test_device
java -jar build/libs/bluetooth_test_device.jar
```

**In the server window:**
1. Select **"TCP Server (Android connects to PC)"** radio button
2. Click **"Start Server"**
3. ✅ Should show "Server Running" in green
4. Note the IP shown in log (should be `0.0.0.0:8888` or your actual IP)

### Step 2: Start Android Emulator

1. Open Android Studio
2. Start an Android emulator (any API level)
3. Install both apps:
   - `android_apps/smartglasses_companion` 
   - `android_apps/retrowatch`

### Step 3: Configure & Connect

#### For Android Emulator:
**Use IP: `10.0.2.2`** (this is the emulator's special IP for your host PC)

#### SmartGlasses Companion:
1. Open app
2. Enter Server IP: `10.0.2.2`
3. Enter Port: `8888`
4. Click **"Start companion service"**
5. Click **"Connect"**
6. ✅ Should show "Connected (TCP)" in notification

#### RetroWatch:
1. Open app
2. Go to Watch Control settings
3. Enter TCP Host: `10.0.2.2`
4. Enter TCP Port: `8888`
5. Click **"Connect"**
6. ✅ Should show "Connected" status

### Step 4: Test Functionality

#### SmartGlasses Companion Tests:
- [ ] **Send Message**: Enter text, click "Send Message" → Check server log
- [ ] **Send Clock Data**: Click "Send Clock Data" → Check server log
- [ ] **Send Battery Status**: Click "Send Battery Status" → Check server log
- [ ] **Auto-connect message**: Should see "Connected to server" in server log

#### RetroWatch Tests:
- [ ] **Connect**: Should connect without hanging
- [ ] **Send Clock Data**: Click "Send Clock Data" → Check server log
- [ ] **Auto-connect message**: Should send clock data automatically after connection

### Step 5: Verify Server Log

**Expected format in server log:**
```
[HH:MM:SS] Server started on 0.0.0.0:8888
[HH:MM:SS] Server Running
[HH:MM:SS] Client connected from /10.0.2.2:xxxxx
[HH:MM:SS] RECV: STATUS | [1]
[HH:MM:SS] RECV: NOTIFY | Connected to server
[HH:MM:SS] RECV: TIME | [year, month, day, hour, minute, second]
[HH:MM:SS] RECV: NOTIFY | Battery: XX%
```

## 🔧 If Something Doesn't Work

### Connection Issues:
1. **Check server is running** - Should show "Server Running" in green
2. **Verify IP address** - Emulator MUST use `10.0.2.2`
3. **Check port** - Must be `8888`
4. **Check service** - SmartGlasses needs "Start companion service" first

### Send Button Issues:
1. **Check connection** - Must be connected before sending
2. **Check service** - Service must be running
3. **Check Android logcat**:
   ```bash
   adb logcat | grep -i "CompanionService\|RetroWatch"
   ```
4. **Check server log** - Should show received messages

### Server Not Receiving:
1. **Check connection** - Server log should show "Client connected"
2. **Check protocol** - Server should decode ProtoV2 automatically
3. **Check firewall** - Windows Firewall might block port 8888

## 📊 Testing Results Template

```
Date: ___________
Emulator: Android API ___

SmartGlasses Companion:
[ ] Connects successfully
[ ] Send Message works
[ ] Send Clock Data works
[ ] Send Battery Status works
[ ] Server receives messages
[ ] Messages decoded correctly

RetroWatch:
[ ] Connects successfully (no hanging)
[ ] Send Clock Data works
[ ] Server receives messages
[ ] Messages decoded correctly

Issues Found:
1. 
2. 
3. 

Fixed:
1. 
2. 
3. 
```

## 🎉 Success Criteria

✅ Both apps connect to server
✅ Send buttons work without errors
✅ Server receives and decodes messages
✅ No connection timeouts or hanging
✅ No crashes
✅ Messages appear in correct format

---

**Ready to test!** Start the server, configure the apps with `10.0.2.2:8888`, and begin testing.

