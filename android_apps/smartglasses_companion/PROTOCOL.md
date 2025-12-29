# Protocol v2 (Android ↔ Arduino)

Design goals: **robust against partial data**, **fast resync**, **tiny MCU parser**, **short payloads**, and **explicit integrity**.

## Transport

- Bluetooth Classic SPP (RFCOMM), 8N1, typical module baud `9600` on Arduino UART.
- Payloads are **application framed** (you can read/write arbitrarily sized chunks at the OS level).

## Framing (byte-stuffed)

### Special bytes

- `SOF = 0x7E`
- `EOF = 0x7F`
- `ESC = 0x7D`
- `ESC_XOR = 0x20`

### Raw frame layout (before stuffing)

```
SOF
  VER   (1)  = 0x02
  TYPE  (1)
  FLAGS (1)
  SEQ   (1)  (wraps 0..255)
  LEN   (1)  payload length, 0..64
  PAYLOAD (LEN)
  CRC16 (2)  CRC-16/CCITT-FALSE over VER..PAYLOAD
EOF
```

### Byte-stuffing rules

Inside the body (everything between `SOF` and `EOF`):

- Any byte equal to `SOF/EOF/ESC` is encoded as:
  - `ESC`, then `(byte XOR 0x20)`

This enables fast resync: if the MCU sees `SOF` mid-frame, it can restart the frame.

## CRC

- CRC-16/CCITT-FALSE
  - poly: `0x1021`
  - init: `0xFFFF`
  - refin/refout: false
  - xorout: `0x0000`
- Stored as big-endian: `CRC16_H`, `CRC16_L`

## Flags

- `0x01` = **ACK required**

## Message types

### Android → Arduino

- `0x01 STATUS`
  - Payload: `status (1B)`
  - `0x01 = CONNECTED`
  - `0x02 = DISCONNECTED`

- `0x02 TIME`
  - Payload: `yearLE(2B), month(1B 1..12), day(1B 1..31), hour(1B 0..23), min(1B 0..59), sec(1B 0..59)`

- `0x03 CALL`
  - Payload: UTF-8 text (caller display name), truncated to 64 bytes

- `0x04 NOTIFY`
  - Payload: UTF-8 text, truncated to 64 bytes

- `0x05 PING`
  - Payload: empty

### Arduino → Android

- `0x10 ACK`
  - Payload: `ackType(1B), ackSeq(1B), result(1B)`
  - `result`: `0x00 OK`, non-zero reserved for errors

## Efficiency guidance

- Keep `LEN <= 64`.
- Prefer **Android-side pagination**: if notification text is long, split into multiple NOTIFY frames with prefixes like `1/3 `, `2/3 `, etc. (avoids a big MCU buffer + scrolling logic).



