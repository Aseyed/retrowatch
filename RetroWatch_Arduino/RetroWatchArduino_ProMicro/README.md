# RetroWatch for Arduino Pro Micro (ATmega32U4)

This firmware is a port of the original RetroWatch Arduino code, optimized for the Arduino Pro Micro (ATmega32U4).

## Compatibility & Changes

The Arduino Pro Micro uses the ATmega32U4 microcontroller, which has different pin capabilities compared to the standard Arduino Uno/Nano (ATmega328P).

### Key Changes Made:
1.  **Bluetooth Communication**: 
    *   **Original**: Used `SoftwareSerial` on pins 2 and 3.
    *   **Pro Micro Port**: Uses hardware `Serial1` on pins 0 and 1.
    *   **Reason**: On the Pro Micro, pins 2 and 3 are the hardware I2C pins (SDA/SCL). Using them for SoftwareSerial would conflict with the OLED display. Additionally, hardware Serial is more reliable and efficient.

2.  **Pin Mapping**:
    *   **I2C (OLED)**: Remapped physically to the Pro Micro's hardware I2C pins.
    *   **Bluetooth**: Remapped to Hardware Serial1.

## Wiring Guide

| Component | Pin Name | Pro Micro Pin | Notes |
|-----------|----------|---------------|-------|
| **OLED** | SDA | **2** | Hardware I2C SDA |
| | SCL | **3** | Hardware I2C SCL |
| | VCC | VCC | Check your OLED voltage (3.3V or 5V) |
| | GND | GND | |
| **Bluetooth** (HC-06) | TX | **0 (RX1)** | Connect HC-06 TX to Pro Micro RX |
| | RX | **1 (TX0)** | Connect HC-06 RX to Pro Micro TX |
| | VCC | VCC | Check module voltage |
| | GND | GND | |
| **Button** | Signal | **5** | Active Low (Input with internal pull-up? Check circuit) |

*Note: The original code sets `pinMode(buttonPin, INPUT)` but reads `LOW` as clicked. You likely need an external pull-up resistor or ensure the button connects to VCC/GND appropriately based on your specific circuit. If using a simple switch to ground, consider changing `INPUT` to `INPUT_PULLUP` in `setup()` if you don't have external resistors.*

## Arduino IDE Settings

1.  **Board**: Select **Arduino Leonardo** or **SparkFun Pro Micro** (if you have the board definitions installed). The Pro Micro is essentially a Leonardo in a smaller form factor.
2.  **Processor**: ATmega32U4 (5V, 16MHz) or (3.3V, 8MHz) depending on your specific board.
3.  **Port**: Select the COM port that appears when you plug in the board.

## Compilation

The code has been updated to automatically detect the `__AVR_ATmega32U4__` architecture and switch to `Serial1`. It remains backward compatible with other boards (Uno/Nano) which will revert to `SoftwareSerial`.

## Known Limitations

*   **Voltage Levels**: Ensure your OLED and Bluetooth modules match the logic voltage of your Pro Micro (3.3V vs 5V). Mixing 5V logic with 3.3V modules can damage them.
*   **Upload Reset**: If you have trouble uploading, quickly double-tap the reset button (if available) or short the RST pin to GND twice quickly to enter bootloader mode manually.





