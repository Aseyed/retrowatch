# RetroWatch Arduino Simulator

A desktop-based simulator/emulator of the RetroWatch Arduino ProMicro device for testing and debugging the Android RetroWatch application without physical hardware.

## Features

- ✅ **Legacy Protocol Support** - Implements the exact 0xFC/0xFD transaction protocol used by RetroWatch
- ✅ **Arduino Logic Simulation** - Matches Arduino behavior (message buffers, display modes, time management)
- ✅ **Visual LCD Display** - 128x64 OLED display simulator with graphics rendering
- ✅ **Multiple Connection Methods** - COM port (virtual serial) and TCP server support
- ✅ **Real-time Display Updates** - Clock, messages, icons, and indicators
- ✅ **Message Management** - Normal messages (7 max) and emergency messages (3 max)

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed design documentation.

## Requirements

- Java 11 or higher
- Gradle (or use Gradle wrapper)
- For COM port support: jSerialComm library (included)
- For TCP server: Built-in Java networking

## Building

```bash
cd retrowatch_arduino_simulator
../gradlew build
```

## Running

### Option 1: Run directly with Gradle

```bash
../gradlew run
```

### Option 2: Build and run JAR

```bash
../gradlew jar
java -jar build/libs/retrowatch_arduino_simulator.jar
```

## Connection Methods

### Method 1: Virtual COM Port (Recommended for Windows)

**Setup:**
1. Install a virtual COM port driver (e.g., com0com, Virtual Serial Port Driver)
2. Create a virtual COM port pair (e.g., COM3 ↔ COM4)
3. Pair your Android phone with your PC via Bluetooth
4. Configure Bluetooth to use one COM port (e.g., COM3)
5. In the simulator, select the other COM port (e.g., COM4)
6. Connect

**Pros:**
- Works with existing Android app without modifications
- Most realistic simulation
- Uses actual Bluetooth connection

**Cons:**
- Requires COM port driver installation
- Windows-specific (though alternatives exist for Linux/macOS)

### Method 2: TCP Server (Recommended for Development)

**Setup:**
1. Start the simulator
2. Select "TCP Server" mode
3. Click "Start Server"
4. Note the IP address and port (e.g., `192.168.52.99:8888`)
5. Modify Android app to connect via TCP (or use a Bluetooth-to-TCP bridge)

**Pros:**
- Easy to set up
- No drivers needed
- Works over WiFi
- Can test from Android emulator

**Cons:**
- Requires Android app modification or proxy
- Not the same as production (production uses Bluetooth)

## Protocol Details

### Transaction Format

```
[0xFC] [Command] [Data...] [0xFD]
```

### Supported Commands

| Command | Hex | Description |
|---------|-----|-------------|
| RESET_EMERGENCY_OBJ | 0x05 | Clear emergency message buffer |
| RESET_NORMAL_OBJ | 0x02 | Clear normal message buffer |
| ADD_EMERGENCY_OBJ | 0x11 | Add emergency message |
| ADD_NORMAL_OBJ | 0x12 | Add normal message |
| SET_TIME | 0x31 | Set clock time |
| SET_CLOCK_STYLE | 0x33 | Set clock display style |
| SET_INDICATOR | 0x34 | Show/hide indicators |
| PING | 0x51 | Connection test |

### Message Format (ADD_NORMAL_OBJ / ADD_EMERGENCY_OBJ)

```
[0xFC] [0x12] [0xF0] [ID] [Icon] [Text...] [0xFD]
```

Example:
```
0xFC 0x12 0xF0 0x01 0x03 "Hello World" 0xFD
```

### Time Format (SET_TIME)

```
[0xFC] [0x31] [Month] [Day] [Week] [AMPM] [Hour] [Min] [0xFD]
```

Week: 1=Sun, 2=Mon, ..., 7=Sat
AMPM: 0=AM, 1=PM

## Display Behavior

### Display Modes

1. **START_UP** - Shows RetroWatch logo for 2 seconds
2. **CLOCK** - Displays current time (5 minutes, then transitions to IDLE)
3. **EMERGENCY_MSG** - Shows emergency messages (5 seconds each)
4. **NORMAL_MSG** - Shows normal messages (3 seconds each)
5. **IDLE** - Minimal clock display (60 second update interval)

### Clock Styles

- **Analog** (0x01) - Analog clock face with hands
- **Digital** (0x02) - Digital time display
- **Mixed** (0x03) - Analog + digital combined

### Timing Constants

- Clock update: Every 60 seconds
- Emergency message display: 5 seconds per message
- Normal message display: 3 seconds per message
- Clock mode duration: 5 minutes (then idle)
- Idle update interval: 60 seconds

## Testing with Android App

1. **Start Simulator**
   - Run the simulator application
   - Select connection method (COM port or TCP)
   - Connect

2. **Connect Android App**
   - Open RetroWatch Android app
   - Pair/connect to device
   - App should connect to simulator

3. **Send Messages**
   - Send notifications from Android app
   - Watch messages appear in simulator LCD display
   - Verify display behavior matches real hardware

4. **Test Scenarios**
   - Send normal messages
   - Send emergency messages
   - Set time
   - Change clock style
   - Test message limits (7 normal, 3 emergency)

## Troubleshooting

### Connection Issues

- **COM port not found**: Check that virtual COM port driver is installed and ports are created
- **Connection timeout**: Verify Bluetooth pairing and COM port configuration
- **TCP connection fails**: Check firewall settings and ensure Android app and PC are on same network

### Protocol Issues

- **Messages not appearing**: Check protocol format matches Android app exactly
- **Display not updating**: Verify display update callback is being called
- **Time not updating**: Check time update interval (60 seconds)

### Display Issues

- **LCD not rendering**: Check display update callback is set
- **Text not visible**: Verify text rendering and font settings
- **Icons not showing**: Check icon bitmap data

## Development

### Project Structure

```
retrowatch_arduino_simulator/
├── src/main/java/com/hardcopy/retrowatch/simulator/
│   ├── protocol/          # Protocol parsing
│   ├── arduino/           # Arduino logic simulation
│   ├── display/           # LCD display rendering
│   ├── communication/     # Bluetooth/COM/TCP bridges
│   └── ui/                # GUI components
├── build.gradle
├── README.md
└── ARCHITECTURE.md
```

### Adding Features

1. **New Commands**: Add to `CommandTypes.java` and handle in `ArduinoSimulator.processCommand()`
2. **Display Features**: Extend `LCDSimulator.java` rendering methods
3. **Connection Methods**: Implement new bridge in `communication/` package

## License

See LICENSE file in project root.

## Credits

- Original Arduino code: Suh Young Bae (godstale@hotmail.com)
- Simulator implementation: Based on Arduino ProMicro code analysis

