# RetroWatch Arduino Simulator - Implementation Summary

## Overview

This document provides a technical summary of the RetroWatch Arduino simulator implementation, including architecture decisions, component mapping, and usage instructions.

## Architecture Decisions

### Technology Stack: Java + Swing

**Rationale:**
- ✅ Cross-platform (Windows, macOS, Linux)
- ✅ Native GUI support
- ✅ Easy serial port access (jSerialComm)
- ✅ TCP socket support built-in
- ✅ Matches existing `bluetooth_test_device` codebase
- ✅ Good graphics libraries for LCD simulation

### Protocol Implementation

**Legacy Protocol (0xFC/0xFD):**
- Matches original RetroWatch Arduino code exactly
- State machine parser replicates Arduino parsing logic
- Transaction format: `[0xFC][CMD][Data...][0xFD]`
- Message format: `[0xFC][CMD][0xF0][ID][Icon][Text...][0xFD]`

### Connection Methods

**Option 1: Virtual COM Port (Recommended)**
- Uses virtual serial port driver (com0com, etc.)
- Android app connects via Bluetooth → COM port → Simulator
- Most realistic simulation
- Works with existing Android app

**Option 2: TCP Server (Development)**
- Simulator runs TCP server (port 8888)
- Android app connects via TCP
- Easier setup, no drivers needed
- Requires Android app modification or proxy

## Component Mapping

### Arduino Code → Simulator Components

| Arduino Component | Simulator Component | Location |
|------------------|-------------------|----------|
| `receiveBluetoothData()` | `LegacyProtocolParser.processByte()` | `protocol/LegacyProtocolParser.java` |
| `parseStartSignal()` | `parseStartSignal()` | `protocol/LegacyProtocolParser.java` |
| `parseCommand()` | `parseCommand()` | `protocol/LegacyProtocolParser.java` |
| `parseMessage()` | `parseMessage()` | `protocol/LegacyProtocolParser.java` |
| `processTransaction()` | `ArduinoSimulator.processCommand()` | `arduino/ArduinoSimulator.java` |
| `msgBuffer[][]` | `MessageBuffer` | `arduino/MessageBuffer.java` |
| `emgBuffer[][]` | `MessageBuffer` | `arduino/MessageBuffer.java` |
| `onDraw()` | `ArduinoSimulator.onDraw()` | `arduino/ArduinoSimulator.java` |
| `drawClock()` | `LCDSimulator.drawClock()` | `display/LCDSimulator.java` |
| `drawMessage()` | `LCDSimulator.drawMessage()` | `display/LCDSimulator.java` |
| `drawEmergency()` | `LCDSimulator.drawEmergency()` | `display/LCDSimulator.java` |
| `updateTime()` | `TimeState.updateTime()` | `arduino/TimeState.java` |
| `BTSerial` | `ComPortBridge` / `TcpServerBridge` | `communication/` |

## Key Implementation Details

### Protocol Parser

**State Machine:**
```java
TR_MODE_IDLE → TR_MODE_WAIT_CMD → TR_MODE_WAIT_MESSAGE/TIME/ID → TR_MODE_WAIT_COMPLETE → TR_MODE_IDLE
```

**Message Parsing:**
- Android sends: `[0xFC][CMD][0xF0][ID][Icon][Text...][0xFD]`
- Arduino stores: `buffer[0]=0x01, buffer[1]=0x00, buffer[2]=0xF0, buffer[3]=ID, buffer[4]=Icon, buffer[5+]=Text`
- Parser extracts: ID, Icon, Text from buffer

### Arduino Simulator

**Message Buffers:**
- Normal messages: 7 max, 19 bytes each
- Emergency messages: 3 max, 19 bytes each
- Circular buffer implementation (wraps around)

**Display State Machine:**
```
START_UP → CLOCK → (after 5 min) → IDLE
CLOCK → (new emergency) → EMERGENCY_MSG → NORMAL_MSG → CLOCK
CLOCK → (new normal) → NORMAL_MSG → CLOCK
```

**Timing:**
- Clock update: Every 60 seconds
- Emergency display: 5 seconds per message
- Normal display: 3 seconds per message
- Clock mode: 5 minutes, then idle

### LCD Display

**Rendering:**
- 128x64 pixel buffer (BufferedImage)
- Scaled 4x for visibility (512x256 display)
- Graphics rendering matches Arduino GFX library behavior

**Clock Styles:**
- Analog: Circle with hour/minute hands
- Digital: Text-based time display
- Mixed: Analog + digital combined

## File Structure

```
retrowatch_arduino_simulator/
├── src/main/java/com/hardcopy/retrowatch/simulator/
│   ├── RetroWatchSimulatorApp.java          # Main entry point
│   ├── protocol/
│   │   ├── CommandTypes.java                # Command constants
│   │   └── LegacyProtocolParser.java        # Protocol parser
│   ├── arduino/
│   │   ├── ArduinoSimulator.java            # Main Arduino logic
│   │   ├── MessageBuffer.java              # Message buffer management
│   │   ├── TimeState.java                   # Time management
│   │   └── DisplayMode.java                 # Display mode enum
│   ├── display/
│   │   └── LCDSimulator.java                # LCD rendering
│   ├── communication/
│   │   ├── ComPortBridge.java               # COM port bridge
│   │   └── TcpServerBridge.java             # TCP server bridge
│   └── ui/
│       ├── MainWindow.java                  # Main GUI window
│       ├── LCDPanel.java                    # LCD display panel
│       └── LogPanel.java                    # Log display panel
├── build.gradle                             # Build configuration
├── README.md                                # User documentation
├── ARCHITECTURE.md                          # Architecture design
├── SETUP_GUIDE.md                           # Setup instructions
└── IMPLEMENTATION_SUMMARY.md                # This file
```

## Usage Example

### 1. Build and Run

```bash
cd retrowatch_arduino_simulator
../gradlew build
../gradlew run
```

### 2. Connect (COM Port Method)

1. Install virtual COM port driver
2. Create port pair (e.g., COM3 ↔ COM4)
3. Configure Bluetooth to use COM3
4. In simulator: Select COM4, click "Connect"

### 3. Connect (TCP Method)

1. In simulator: Select "TCP Server", click "Start Server"
2. Note IP address (e.g., `192.168.1.100:8888`)
3. Modify Android app to connect via TCP
4. Connect from Android app

### 4. Test Messages

- Send normal message → Appears in NORMAL_MSG mode (3 seconds)
- Send emergency message → Appears in EMERGENCY_MSG mode (5 seconds)
- Set time → Clock updates
- Change clock style → Display changes

## Testing Checklist

- [ ] Simulator starts and displays correctly
- [ ] COM port connection works
- [ ] TCP server connection works
- [ ] Protocol parser handles all command types
- [ ] Message buffers work correctly (7 normal, 3 emergency)
- [ ] Display modes transition correctly
- [ ] Clock updates every 60 seconds
- [ ] Time setting works
- [ ] Clock styles render correctly
- [ ] Indicators show message counts
- [ ] Android app can connect and send messages
- [ ] Messages appear in LCD display
- [ ] Display timing matches Arduino behavior

## Known Limitations

1. **Icon Rendering:** Currently uses simplified boxes instead of bitmap icons
   - **Fix:** Add icon bitmap loading and rendering

2. **Button Input:** No button simulation (button press triggers)
   - **Fix:** Add button simulation in GUI

3. **Text Scrolling:** Long text may overflow (Arduino has scrolling)
   - **Fix:** Implement text scrolling logic

4. **Bluetooth SPP Server:** Not implemented (only COM port and TCP)
   - **Fix:** Add native Bluetooth SPP server support

## Future Enhancements

1. **Icon Bitmaps:** Load and render actual icon bitmaps
2. **Button Simulation:** Add button press simulation
3. **Text Scrolling:** Implement scrolling for long messages
4. **Bluetooth SPP Server:** Native Bluetooth support
5. **Record/Playback:** Capture and replay message sequences
6. **Debug Mode:** Detailed state inspection and debugging
7. **Performance Metrics:** Timing analysis and profiling
8. **Multiple Devices:** Support multiple Android device connections

## Protocol Reference

### Command Types

| Command | Hex | Payload | Description |
|---------|-----|---------|-------------|
| RESET_EMERGENCY_OBJ | 0x05 | None | Clear emergency buffer |
| RESET_NORMAL_OBJ | 0x02 | None | Clear normal buffer |
| ADD_EMERGENCY_OBJ | 0x11 | `[0xF0][ID][Icon][Text...]` | Add emergency message |
| ADD_NORMAL_OBJ | 0x12 | `[0xF0][ID][Icon][Text...]` | Add normal message |
| SET_TIME | 0x31 | `[Month][Day][Week][AMPM][Hour][Min]` | Set clock time |
| SET_CLOCK_STYLE | 0x33 | `[Style]` | Set clock style |
| SET_INDICATOR | 0x34 | `[Enable]` | Show/hide indicators |
| PING | 0x51 | None | Connection test |

### Transaction Format

```
[0xFC] [Command] [Data...] [0xFD]
```

### Message Format

```
[0xFC] [0x12] [0xF0] [ID] [Icon] [Text...] [0xFD]
```

## Conclusion

The RetroWatch Arduino simulator provides a complete desktop-based simulation of the Arduino ProMicro device, enabling efficient development and testing of the Android application without physical hardware. The implementation closely matches the Arduino code behavior, ensuring accurate simulation of device functionality.

## Support

For detailed information:
- **Architecture:** See [ARCHITECTURE.md](ARCHITECTURE.md)
- **Setup:** See [SETUP_GUIDE.md](SETUP_GUIDE.md)
- **Usage:** See [README.md](README.md)

