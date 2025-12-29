# Setup Guide: Testing Android App with Desktop Simulator

## Understanding the Connection

There are **two different scenarios**:

### Scenario A: Android → Arduino (Real Hardware)
- Android app connects to **Arduino Pro Micro** via Bluetooth module (HC-05/HC-06)
- This is the **production setup**
- No desktop app needed

### Scenario B: Android → PC Desktop App (Testing)
- Android app connects to **PC** (this desktop app)
- PC simulates the Arduino
- Requires PC to act as a **Bluetooth server**

## The Problem

**This desktop app currently uses COM ports**, which works when:
- You have a Bluetooth device that creates a virtual COM port on Windows
- You connect to that COM port

**But for Android-to-PC testing**, you typically need:
- PC to advertise a **Bluetooth SPP service** (like a server)
- Android app connects to that service (as a client)

## Solutions

### Option 1: Use Actual Arduino (Recommended for Testing)

**Easiest approach:**
1. Flash your Arduino Pro Micro with `SmartGlasses_ProMicro_v2.ino`
2. Connect HC-05/HC-06 Bluetooth module to Arduino
3. Pair Android phone with HC-05/HC-06
4. Test Android app directly with Arduino

**Pros:** Real hardware, actual production setup  
**Cons:** Requires Arduino hardware

### Option 2: Use Bluetooth USB Dongle with COM Port

1. Get a Bluetooth USB dongle that creates COM ports
2. Configure it to act as a serial port
3. Android app connects to it
4. Desktop app connects to the COM port
5. Messages flow: Android → Bluetooth → COM Port → Desktop App

**Pros:** Works with current desktop app  
**Cons:** Requires specific Bluetooth hardware

### Option 3: Implement Direct Bluetooth SPP Server (Future)

Modify desktop app to:
1. Use `javax.bluetooth` or similar library
2. Advertise a Bluetooth SPP service
3. Accept incoming connections from Android
4. Android app connects directly to PC

**Pros:** No COM port needed  
**Cons:** Requires additional library and code changes

### Option 4: Reverse Connection (Android as Server)

1. Modify Android app to act as Bluetooth server
2. Desktop app connects to Android (as client)
3. This is backwards from normal flow

**Pros:** Works with current desktop app  
**Cons:** Requires Android app changes, not realistic test

## Recommended Approach

**For now, use Option 1 (Actual Arduino)** because:
- It's the real production setup
- No special configuration needed
- Tests the actual hardware
- Desktop app can be used later for protocol debugging

**The desktop app is still useful for:**
- Protocol debugging (if you add a bridge)
- Testing protocol implementation
- Development when Arduino isn't available

## Quick Test with Arduino

1. **Flash Arduino:**
   ```
   Upload: RetroWatch_Arduino/SmartGlasses_ProMicro_v2/SmartGlasses_ProMicro_v2.ino
   ```

2. **Pair Android with HC-05/HC-06:**
   - Android Settings → Bluetooth
   - Find "HC-05" or your module name
   - Pair (default PIN: 1234 or 0000)

3. **Test Android App:**
   - Open smartglasses_companion app
   - Select paired device
   - Connect
   - Send messages

4. **Watch Arduino Display:**
   - OLED should show connection status
   - Messages should appear on screen

## Next Steps

If you want the desktop app to work for testing:
1. Get a Bluetooth USB dongle that creates COM ports, OR
2. I can help implement direct Bluetooth SPP server (requires library)

For now, testing with actual Arduino is the most practical approach.


