# SmartGlasses Pro Micro v2 (Arduino)

This is a **production-hardened** Arduino sketch for ATmega32U4 (Arduino Pro Micro) that:

- Uses **Serial1** for Bluetooth Classic SPP modules (HC-05/HC-06)
- Parses a robust **framed + escaped + CRC16** protocol (v2)
- Displays link status + time + last CALL/NOTIFY on SSD1306 OLED (I2C `0x3C`)

## Wiring (Pro Micro)

- **Bluetooth module**
  - BT TX → Pro Micro RX1 (pin 0)
  - BT RX → Pro Micro TX1 (pin 1) (use proper level shifting if needed)
  - GND common
  - Baud: `9600`

- **OLED SSD1306 I2C**
  - SDA → pin 2
  - SCL → pin 3
  - VCC/GND

## Protocol

See `smartglasses_companion/PROTOCOL.md`.



