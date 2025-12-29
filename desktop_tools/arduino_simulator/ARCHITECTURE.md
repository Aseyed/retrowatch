# RetroWatch Arduino Simulator - Architecture Document

## Executive Summary

This document describes the architecture for a desktop-based simulator/emulator of the RetroWatch Arduino ProMicro device. The simulator enables testing and debugging of the Android RetroWatch application without requiring physical hardware.

## Project Context

- **Target Device**: Arduino ProMicro (ATmega32U4)
- **Arduino Code**: `RetroWatch_Arduino/RetroWatchArduino_ProMicro/RetroWatchArduino_ProMicro.ino`
- **Android App**: `app/` (RetroWatch Android application)
- **Protocol**: Legacy transaction protocol (0xFC/0xFD framing)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Android App (Java)                        │
│              (RetroWatch Android Application)                │
└──────────────────────┬──────────────────────────────────────┘
                       │ Bluetooth Classic SPP (RFCOMM)
                       │ Protocol: Legacy (0xFC/0xFD)
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Bluetooth Communication Layer                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Option A: Virtual Serial Port (COM Port)              │   │
│  │ Option B: TCP Bridge (Android connects to PC)        │   │
│  │ Option C: Bluetooth SPP Server (Native BT)           │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Protocol Parser Layer                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Transaction State Machine                            │   │
│  │ - TR_MODE_IDLE, TR_MODE_WAIT_CMD, etc.              │   │
│  │ - Command parsing (0x11, 0x12, 0x31, etc.)          │   │
│  │ - Message buffer management                          │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Arduino Logic Simulator                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Message Buffers                                       │   │
│  │ - Normal messages (MSG_COUNT_MAX = 7)                │   │
│  │ - Emergency messages (EMG_COUNT_MAX = 3)               │   │
│  │ - Message buffer size: 19 bytes                      │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Display State Machine                                 │   │
│  │ - DISPLAY_MODE_START_UP                               │   │
│  │ - DISPLAY_MODE_CLOCK                                  │   │
│  │ - DISPLAY_MODE_EMERGENCY_MSG                          │   │
│  │ - DISPLAY_MODE_NORMAL_MSG                             │   │
│  │ - DISPLAY_MODE_IDLE                                   │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Time Management                                        │   │
│  │ - Clock update (every 60 seconds)                     │   │
│  │ - Display timing (3s, 5s, 10s intervals)             │   │
│  │ - Mode transitions                                    │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│              Visual LCD Simulator                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 128x64 OLED Display                                   │   │
│  │ - Graphics rendering (Adafruit GFX compatible)       │   │
│  │ - Text rendering with scrolling                      │   │
│  │ - Icon display (bitmap rendering)                    │   │
│  │ - Clock display (analog/digital/mixed)               │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Recommended: Java + Swing

**Rationale:**
- ✅ Cross-platform (Windows, macOS, Linux)
- ✅ Native GUI support (Swing/JavaFX)
- ✅ Easy serial port access (jSerialComm library)
- ✅ TCP socket support built-in
- ✅ Good graphics libraries for LCD simulation
- ✅ Matches existing `bluetooth_test_device` codebase

**Alternative: Python + Tkinter/PyQt**
- ✅ Rapid prototyping
- ✅ Excellent serial libraries (pyserial)
- ⚠️ Less native feel on Windows
- ⚠️ Different codebase from existing Java tools

**Alternative: C# + Windows Forms/WPF**
- ✅ Excellent Windows integration
- ✅ Native Bluetooth support
- ⚠️ Windows-only (unless using .NET Core)
- ⚠️ Different codebase from existing Java tools

**Decision: Java + Swing** (matches existing codebase, cross-platform)

## Component Design

### 1. Protocol Parser (`LegacyProtocolParser.java`)

**Responsibilities:**
- Parse incoming bytes using state machine
- Extract commands and data
- Validate transaction structure

**State Machine:**
```java
enum TransactionState {
    IDLE,           // TR_MODE_IDLE
    WAIT_CMD,       // TR_MODE_WAIT_CMD
    WAIT_MESSAGE,   // TR_MODE_WAIT_MESSAGE
    WAIT_TIME,      // TR_MODE_WAIT_TIME
    WAIT_ID,        // TR_MODE_WAIT_ID
    WAIT_COMPLETE  // TR_MODE_WAIT_COMPLETE
}
```

**Command Types:**
- `0x05` - RESET_EMERGENCY_OBJ
- `0x02` - RESET_NORMAL_OBJ
- `0x11` - ADD_EMERGENCY_OBJ
- `0x12` - ADD_NORMAL_OBJ
- `0x31` - SET_TIME
- `0x33` - SET_CLOCK_STYLE
- `0x34` - SET_INDICATOR
- `0x51` - PING
- etc.

### 2. Arduino Logic Simulator (`ArduinoSimulator.java`)

**Responsibilities:**
- Maintain message buffers (normal + emergency)
- Manage display state machine
- Handle time updates
- Process commands and update state

**Message Buffers:**
```java
class MessageBuffer {
    private byte[][] msgBuffer;      // [7][19] - Normal messages
    private byte[][] emgBuffer;       // [3][19] - Emergency messages
    private int msgParsingLine;
    private int emgParsingLine;
    private int msgCurDisp;
    private int emgCurDisp;
}
```

**Display State:**
```java
enum DisplayMode {
    START_UP,
    CLOCK,
    EMERGENCY_MSG,
    NORMAL_MSG,
    IDLE
}
```

**Time Management:**
```java
class TimeState {
    byte month, day, week;  // week: 1=Sun, 2=Mon, etc.
    byte amPm;              // 0=AM, 1=PM
    byte hour, minutes, second;
}
```

### 3. LCD Display Simulator (`LCDSimulator.java`)

**Responsibilities:**
- Render 128x64 pixel display
- Draw text, icons, clock faces
- Handle scrolling and paging
- Update display based on state

**Display Features:**
- Clock styles: Analog, Digital, Mixed
- Icon rendering (16x16 bitmaps)
- Text centering and scrolling
- Indicator display (message counts)

**Implementation:**
- Use `BufferedImage` for pixel buffer
- Custom rendering methods matching Arduino GFX library
- Real-time display updates

### 4. Bluetooth Communication Layer (`BluetoothBridge.java`)

**Responsibilities:**
- Provide virtual Bluetooth interface
- Support multiple connection methods
- Handle connection/disconnection events

**Connection Options:**

**Option A: Virtual Serial Port (Recommended for Windows)**
- Use COM port emulator (e.g., com0com, Virtual Serial Port Driver)
- Android connects via Bluetooth → PC COM port
- Simulator reads/writes to COM port
- **Pros**: Works with existing Android app without changes
- **Cons**: Requires COM port driver installation

**Option B: TCP Bridge (Recommended for Development)**
- Simulator runs TCP server (port 8888)
- Android app modified to connect via TCP (or use proxy)
- **Pros**: Easy to set up, no drivers needed
- **Cons**: Requires Android app modification or proxy

**Option C: Native Bluetooth SPP Server**
- Use javax.bluetooth (JSR-82) or platform-specific library
- Simulator acts as Bluetooth SPP server
- **Pros**: Most realistic, no Android changes
- **Cons**: Complex, platform-specific, may require native libraries

**Decision: Support Option A (COM port) and Option B (TCP) initially**

### 5. Main Application (`RetroWatchSimulatorApp.java`)

**Responsibilities:**
- GUI window management
- Component integration
- User controls (connect/disconnect, settings)
- Logging and debugging

**GUI Layout:**
```
┌─────────────────────────────────────────────────┐
│  RetroWatch Arduino Simulator                   │
├─────────────────────────────────────────────────┤
│  [Connection Panel]                              │
│  Mode: [COM Port] [TCP Server]                  │
│  Device: [COM3 ▼] [Connect] [Disconnect]       │
│  Status: Connected                               │
├─────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌────────────────────────┐ │
│  │               │  │  Message Log            │ │
│  │  LCD Display  │  │  [RECV] ADD_NORMAL_OBJ │ │
│  │  (128x64)     │  │  [SEND] ACK            │ │
│  │               │  │  ...                    │ │
│  └───────────────┘  └────────────────────────┘ │
├─────────────────────────────────────────────────┤
│  [Controls] [Settings] [Clear Log]              │
└─────────────────────────────────────────────────┘
```

## Protocol Mapping

### Android → Arduino Messages

| Command | Hex | Payload Format | Arduino Action |
|---------|-----|----------------|----------------|
| RESET_EMERGENCY_OBJ | 0x05 | None | Clear emergency buffer |
| RESET_NORMAL_OBJ | 0x02 | None | Clear normal buffer |
| ADD_EMERGENCY_OBJ | 0x11 | `[0xF0][ID][Icon][Text...]` | Add to emergency buffer |
| ADD_NORMAL_OBJ | 0x12 | `[0xF0][ID][Icon][Text...]` | Add to normal buffer |
| SET_TIME | 0x31 | `[Month][Day][Week][AMPM][Hour][Min]` | Update clock |
| SET_CLOCK_STYLE | 0x33 | `[Style]` | Change clock display |
| SET_INDICATOR | 0x34 | `[Enable]` | Show/hide indicators |
| PING | 0x51 | None | No-op (connection test) |

### Transaction Format

```
[0xFC] [Command] [Data...] [0xFD]
```

**Example: ADD_NORMAL_OBJ**
```
0xFC 0x12 0xF0 0x01 0x03 "Hello World" 0xFD
```

**Example: SET_TIME**
```
0xFC 0x31 0x0C 0x1F 0x01 0x01 0x0A 0x2D 0xFD
(Dec 31, Sunday, PM, 10:45)
```

## Display Behavior Simulation

### Display Modes

1. **START_UP** (2 seconds)
   - Show RetroWatch logo
   - Transition to CLOCK mode

2. **CLOCK** (5 minutes, then IDLE)
   - Display current time
   - Show indicators (message counts)
   - Button press → EMERGENCY_MSG mode

3. **EMERGENCY_MSG** (5 seconds per message)
   - Display emergency messages in sequence
   - Show icon + text
   - After all messages → NORMAL_MSG mode

4. **NORMAL_MSG** (3 seconds per message)
   - Display normal messages in sequence
   - Show icon + text
   - After all messages → CLOCK mode

5. **IDLE** (60 seconds update interval)
   - Minimal clock display
   - Button press → CLOCK mode

### Timing Constants

```java
UPDATE_TIME_INTERVAL = 60000;      // 60 seconds
CLOCK_DISP_INTERVAL = 60000;       // 60 seconds
EMERGENCY_DISP_INTERVAL = 5000;    // 5 seconds
MESSAGE_DISP_INTERVAL = 3000;      // 3 seconds
CLOCK_DISPLAY_TIME = 300000;       // 5 minutes
EMER_DISPLAY_TIME = 10000;         // 10 seconds
MSG_DISPLAY_TIME = 5000;           // 5 seconds
IDLE_DISP_INTERVAL = 60000;        // 60 seconds
```

## Implementation Phases

### Phase 1: Core Protocol & State Machine
- [ ] Implement protocol parser
- [ ] Implement Arduino state machine
- [ ] Basic message buffer management
- [ ] Unit tests for protocol parsing

### Phase 2: Display Simulator
- [ ] LCD rendering engine (128x64)
- [ ] Text rendering
- [ ] Clock display (all styles)
- [ ] Icon rendering
- [ ] Display state transitions

### Phase 3: Communication Layer
- [ ] COM port support
- [ ] TCP server support
- [ ] Connection management
- [ ] Error handling

### Phase 4: Integration & Testing
- [ ] GUI integration
- [ ] End-to-end testing with Android app
- [ ] Performance optimization
- [ ] Documentation

## File Structure

```
retrowatch_arduino_simulator/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── hardcopy/
│                   └── retrowatch/
│                       └── simulator/
│                           ├── RetroWatchSimulatorApp.java
│                           ├── protocol/
│                           │   ├── LegacyProtocolParser.java
│                           │   └── CommandTypes.java
│                           ├── arduino/
│                           │   ├── ArduinoSimulator.java
│                           │   ├── MessageBuffer.java
│                           │   ├── DisplayState.java
│                           │   └── TimeState.java
│                           ├── display/
│                           │   ├── LCDSimulator.java
│                           │   ├── GFXRenderer.java
│                           │   └── IconRenderer.java
│                           ├── communication/
│                           │   ├── BluetoothBridge.java
│                           │   ├── ComPortBridge.java
│                           │   └── TcpServerBridge.java
│                           └── ui/
│                               ├── MainWindow.java
│                               ├── LCDPanel.java
│                               └── LogPanel.java
├── resources/
│   ├── icons/          # Icon bitmaps
│   └── logo.png        # Startup logo
├── build.gradle
├── README.md
└── ARCHITECTURE.md
```

## Testing Strategy

### Unit Tests
- Protocol parser correctness
- State machine transitions
- Message buffer operations
- Time calculations

### Integration Tests
- End-to-end message flow
- Display updates
- Connection handling

### Manual Testing
- Connect Android app
- Send various message types
- Verify display behavior
- Test timing and transitions

## Deployment

### Build Artifacts
- JAR file (executable)
- Batch/shell scripts for easy launch
- Documentation (README, setup guide)

### Dependencies
- Java 11+
- jSerialComm (for COM port support)
- Gradle (build tool)

### Distribution
- Single JAR file (fat JAR with dependencies)
- Platform-specific launchers
- Setup instructions

## Future Enhancements

1. **Bluetooth SPP Server** (native Bluetooth support)
2. **Record/Playback** (capture and replay message sequences)
3. **Debug Mode** (detailed state inspection)
4. **Performance Metrics** (timing analysis)
5. **Multiple Device Simulation** (test with multiple Android devices)

## Conclusion

This architecture provides a complete simulation of the RetroWatch Arduino ProMicro device, enabling efficient development and testing of the Android application without physical hardware. The modular design allows for incremental implementation and easy maintenance.

