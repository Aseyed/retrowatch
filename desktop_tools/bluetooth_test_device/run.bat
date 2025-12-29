@echo off
REM Run the Bluetooth Test Device desktop app (requires build first)

cd /d "%~dp0"

if not exist "build\libs\bluetooth_test_device.jar" (
    echo JAR not found. Building first...
    call build_and_run.bat
    exit /b
)

echo Starting Bluetooth Test Device...
java -jar build\libs\bluetooth_test_device.jar

pause


