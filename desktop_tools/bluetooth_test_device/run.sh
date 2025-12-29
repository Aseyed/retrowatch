#!/bin/bash
# Run the Bluetooth Test Device app (Git Bash / Linux)

cd "$(dirname "$0")"

# Set Java path (adjust if your Java is elsewhere)
if [ -d "/c/Program Files/Android/Android Studio/jbr" ]; then
    export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
    export PATH="$JAVA_HOME/bin:$PATH"
elif [ -d "/c/Program Files/Java" ]; then
    # Try to find latest Java
    JAVA_DIR=$(ls -d "/c/Program Files/Java"/* 2>/dev/null | head -1)
    if [ -n "$JAVA_DIR" ]; then
        export JAVA_HOME="$JAVA_DIR"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
fi

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found!"
    echo "Please install Java 11+ or set JAVA_HOME manually"
    exit 1
fi

# Check if JAR exists
if [ ! -f "build/libs/bluetooth_test_device.jar" ]; then
    echo "JAR not found. Building first..."
    ../gradlew -p . build
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
fi

echo "Starting Bluetooth Test Device..."
echo "Java: $(java -version 2>&1 | head -1)"
echo ""

java -jar build/libs/bluetooth_test_device.jar


