# RetroWatch Arduino Simulator - Setup Guide

## Quick Start

1. **Build the simulator:**
   ```bash
   cd retrowatch_arduino_simulator
   ../gradlew build
   ```

2. **Run the simulator:**
   ```bash
   ../gradlew run
   ```

3. **Choose connection method:**
   - **COM Port**: For virtual serial port connection (requires driver)
   - **TCP Server**: For TCP connection (requires Android app modification)

## Connection Methods

### Method 1: Virtual COM Port (Recommended for Production Testing)

This method uses a virtual COM port to bridge Bluetooth and the simulator.

#### Windows Setup

1. **Install Virtual COM Port Driver:**
   - Download and install [com0com](http://com0com.sourceforge.net/) or
   - [Virtual Serial Port Driver](https://www.eltima.com/products/vspdxp/) (commercial)

2. **Create Virtual Port Pair:**
   - Using com0com: Run `setupc.exe` and create a port pair (e.g., COM3 ↔ COM4)
   - Note the port names (e.g., COM3 and COM4)

3. **Configure Bluetooth:**
   - Pair your Android phone with your PC via Bluetooth
   - In Windows Bluetooth settings, configure the paired device to use one COM port (e.g., COM3)
   - Set baud rate to 9600

4. **Connect Simulator:**
   - In the simulator, select "COM Port" mode
   - Select the other COM port from the pair (e.g., COM4)
   - Click "Connect"
   - Status should show "Connected"

5. **Connect Android App:**
   - Open RetroWatch Android app
   - Connect to the Bluetooth device
   - Messages should flow between Android and simulator

#### Linux Setup

1. **Install socat:**
   ```bash
   sudo apt-get install socat
   ```

2. **Create virtual port pair:**
   ```bash
   socat -d -d pty,raw,echo=0 pty,raw,echo=0
   ```
   Note the two PTY device paths (e.g., `/dev/pts/2` and `/dev/pts/3`)

3. **Configure Bluetooth:**
   - Use `rfcomm` to bind Bluetooth to one PTY
   - Connect simulator to the other PTY

#### macOS Setup

1. **Install socat:**
   ```bash
   brew install socat
   ```

2. **Follow Linux instructions** (similar process)

### Method 2: TCP Server (Recommended for Development)

This method uses TCP sockets instead of Bluetooth.

#### Setup

1. **Start Simulator:**
   - Run the simulator
   - Select "TCP Server" mode
   - Click "Start Server"
   - Note the IP address and port (e.g., `192.168.1.100:8888`)

2. **Modify Android App (Temporary):**
   
   Option A: Add TCP support to Android app
   - Modify `BluetoothManager.java` to support TCP connections
   - Add TCP connection option alongside Bluetooth
   
   Option B: Use TCP-to-Bluetooth Bridge
   - Use a bridge tool to convert TCP to Bluetooth
   - Or use Android emulator with port forwarding

3. **Connect:**
   - Android app connects to `IP:PORT` (e.g., `192.168.1.100:8888`)
   - Messages flow over TCP

#### Advantages

- ✅ Easy to set up
- ✅ No drivers needed
- ✅ Works over WiFi
- ✅ Can test from Android emulator
- ✅ Faster than Bluetooth

#### Disadvantages

- ⚠️ Requires Android app modification or proxy
- ⚠️ Not the same as production (production uses Bluetooth)

## Testing Workflow

### 1. Start Simulator

```bash
cd retrowatch_arduino_simulator
../gradlew run
```

### 2. Connect

- Choose connection method (COM Port or TCP Server)
- Click "Connect" or "Start Server"
- Verify status shows "Connected"

### 3. Connect Android App

- Open RetroWatch Android app
- Pair/connect to device
- App should connect to simulator

### 4. Test Messages

- **Send Normal Message:**
  - From Android app, send a notification
  - Watch it appear in simulator LCD display
  - Verify it shows in NORMAL_MSG mode for 3 seconds

- **Send Emergency Message:**
  - From Android app, send an emergency notification
  - Watch it appear in simulator LCD display
  - Verify it shows in EMERGENCY_MSG mode for 5 seconds

- **Set Time:**
  - From Android app, sync time
  - Verify clock updates in simulator

- **Test Limits:**
  - Send 7 normal messages (should cycle through)
  - Send 3 emergency messages (should cycle through)

### 5. Verify Display Behavior

- **Startup:** Logo shows for 2 seconds
- **Clock:** Shows current time, transitions to IDLE after 5 minutes
- **Messages:** Display in sequence with correct timing
- **Indicators:** Show message counts when enabled

## Troubleshooting

### Connection Issues

**Problem:** COM port not found
- **Solution:** Verify virtual COM port driver is installed and ports are created
- **Check:** Run `setupc.exe` (com0com) or check Device Manager

**Problem:** Connection timeout
- **Solution:** Verify Bluetooth pairing and COM port configuration
- **Check:** Bluetooth device is paired and using correct COM port

**Problem:** TCP connection fails
- **Solution:** Check firewall settings and network connectivity
- **Check:** Android app and PC are on same WiFi network
- **Check:** Firewall allows connections on port 8888

### Protocol Issues

**Problem:** Messages not appearing
- **Solution:** Check protocol format matches Android app exactly
- **Debug:** Check log panel for "RECV" and "CMD" messages
- **Verify:** Transaction format: `[0xFC][CMD][Data...][0xFD]`

**Problem:** Display not updating
- **Solution:** Verify display update callback is being called
- **Check:** Log panel for display update messages
- **Verify:** Arduino simulator update timer is running

**Problem:** Time not updating
- **Solution:** Check time update interval (60 seconds)
- **Verify:** Time state is being updated correctly

### Display Issues

**Problem:** LCD not rendering
- **Solution:** Check display update callback is set
- **Verify:** LCD simulator is receiving display updates

**Problem:** Text not visible
- **Solution:** Verify text rendering and font settings
- **Check:** Text color is white on black background

**Problem:** Icons not showing
- **Solution:** Check icon bitmap data
- **Note:** Current implementation uses simplified icon rendering (boxes)

## Advanced Configuration

### Custom Baud Rate

Default is 9600 baud. To change:
- Modify `connect()` method in `MainWindow.java`
- Change baud rate parameter in `comPortBridge.connect()`

### Custom TCP Port

Default is 8888. To change:
- Modify `startServer()` method in `MainWindow.java`
- Change port parameter in `tcpServerBridge.startServer()`

### Display Update Interval

Default is 100ms. To change:
- Modify update timer interval in `MainWindow.java`
- Change `scheduleAtFixedRate` interval

## Next Steps

1. **Test with Real Android App:**
   - Connect using COM port method
   - Send various message types
   - Verify behavior matches real hardware

2. **Debug Protocol:**
   - Use log panel to inspect messages
   - Verify transaction format
   - Check command parsing

3. **Enhance Display:**
   - Add icon bitmaps
   - Improve text rendering
   - Add scrolling support

4. **Add Features:**
   - Record/playback messages
   - Debug mode with state inspection
   - Performance metrics

## Support

For issues or questions:
- Check [ARCHITECTURE.md](ARCHITECTURE.md) for design details
- Check [README.md](README.md) for general information
- Review Arduino code for protocol details

