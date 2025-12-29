# Troubleshooting: No COM Ports Found

## Issue
The app shows "(No COM ports found)" when you click Refresh.

## Solutions

### Step 1: Check Device Manager

1. Press `Win + X` and select **Device Manager**
2. Look for **Ports (COM & LPT)** section
3. Expand it - do you see any COM ports listed?

**If you see COM ports:**
- Note the COM number (e.g., COM3, COM4)
- The port might be named something like:
  - "Standard Serial over Bluetooth link (COM3)"
  - "Bluetooth Serial Port (COM4)"
  - "HC-05" or "HC-06" (COM3)

**If you DON'T see any COM ports:**
- Your Bluetooth device might not be creating a virtual COM port
- Continue to Step 2

### Step 2: Pair Your Android Phone

1. On Windows, open **Settings** → **Bluetooth & devices**
2. Make sure Bluetooth is **On**
3. On your Android phone, enable Bluetooth
4. On Windows, click **Add device** → **Bluetooth**
5. Select your phone from the list
6. Accept the pairing on both devices
7. Wait for pairing to complete

### Step 3: Check if COM Port Appears After Pairing

1. After pairing, check Device Manager again
2. Look for a new COM port under **Ports (COM & LPT)**
3. If it appears, go back to the app and click **Refresh**

### Step 4: Alternative - Use Android as Server

If your Android phone doesn't create a COM port on Windows, you can:

**Option A: Use the Android app in "Server Mode"**
- The Android app can act as a Bluetooth server
- The desktop app would need to connect as a client
- This requires different connection code (not COM port based)

**Option B: Use a Bluetooth USB Adapter**
- Some USB Bluetooth adapters create COM ports more reliably
- Try a different Bluetooth adapter if available

**Option C: Test with Actual Arduino**
- Instead of desktop app, test directly with your Arduino Pro Micro
- Flash the Arduino with the v2 sketch
- Connect Android app to Arduino via Bluetooth

### Step 5: Verify jSerialComm is Working

The app uses jSerialComm library. To verify it's detecting ports:

1. Check if any other COM ports exist (even non-Bluetooth ones)
2. If you have a USB-to-Serial adapter, plug it in
3. Click Refresh - you should see that COM port
4. If you see other COM ports but not Bluetooth, the issue is Bluetooth-specific

### Common Issues

**"COM port already in use"**
- Another app might be using the port
- Close other serial port applications
- Try disconnecting and reconnecting

**"Permission denied"**
- On some systems, you need admin rights
- Try running the app as Administrator

**"Port disappears after pairing"**
- Some Bluetooth drivers remove COM ports after pairing
- Try unpairing and pairing again
- Check Bluetooth driver settings

## Still Not Working?

If none of these work, the issue might be:
1. Your Bluetooth stack doesn't support virtual COM ports
2. You need a different Bluetooth-to-Serial bridge
3. Direct Bluetooth SPP connection is needed (not COM port based)

In that case, we'd need to implement a different connection method using javax.bluetooth or a platform-specific library.


