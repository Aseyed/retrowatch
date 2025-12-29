@echo off
REM Build and run the Bluetooth Test Device desktop app

cd /d "%~dp0"

echo Building Bluetooth Test Device...
call ..\gradlew.bat build

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Starting application...
call ..\gradlew.bat run

pause


