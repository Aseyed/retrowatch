# Desktop Simulator - Build and Run Commands

## Quick Commands

### Option 1: From Project Root (Recommended)

**Build:**
```bash
cd desktop_tools/arduino_simulator
../../gradlew build
```

**Run:**
```bash
cd desktop_tools/arduino_simulator
../../gradlew run
```

**Build and Run in one command:**
```bash
cd desktop_tools/arduino_simulator && ../../gradlew run
```

### Option 2: From Simulator Directory

If you're already in the simulator directory:

**Build:**
```bash
../../gradlew build
```

**Run:**
```bash
../../gradlew run
```

### Option 3: Build JAR and Run Manually

**Build JAR:**
```bash
cd desktop_tools/arduino_simulator
../../gradlew jar
```

**Run JAR:**
```bash
java -jar build/libs/retrowatch_arduino_simulator-1.0.0.jar
```

## Windows Commands

**Build:**
```cmd
cd desktop_tools\arduino_simulator
..\..\gradlew.bat build
```

**Run:**
```cmd
cd desktop_tools\arduino_simulator
..\..\gradlew.bat run
```

## What Happens When You Run

1. **Simulator window opens** with:
   - LCD display panel (128x64)
   - Connection options (COM Port / TCP Server)
   - Log panel
   - Control buttons

2. **To connect via TCP:**
   - Select **"TCP Server"** radio button
   - Click **"Start Server"**
   - Note the IP address shown (e.g., `192.168.52.99:8888`)
   - Use this IP in your Android app

3. **To connect via COM Port:**
   - Select **"COM Port"** radio button
   - Choose a COM port from dropdown
   - Click **"Connect"**

## Troubleshooting

**"gradlew: command not found" (Linux/Mac):**
```bash
chmod +x ../../gradlew
```

**"Java not found":**
- Make sure Java 11+ is installed
- Check: `java -version`

**"Port already in use":**
- Another instance might be running
- Check Task Manager (Windows) or `ps aux | grep java` (Linux/Mac)
- Kill the process or use a different port

## Output Files

After building:
- **JAR:** `desktop_tools/arduino_simulator/build/libs/retrowatch_arduino_simulator-1.0.0.jar`
- **Distribution ZIP:** `desktop_tools/arduino_simulator/build/distributions/retrowatch_arduino_simulator-1.0.0.zip`

