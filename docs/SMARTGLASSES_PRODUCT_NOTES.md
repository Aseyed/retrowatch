# Smart Glasses System – Production Notes (Android ↔ Bluetooth ↔ Arduino ↔ OLED)

## Architecture (recommended)

- **Android (phone)**
  - **Foreground Service** (single source of truth for connection state)
    - Maintains BT SPP socket
    - Retries with exponential backoff
    - Sends STATUS + TIME periodic updates
    - Accepts events from system (calls, notifications)
  - **NotificationListenerService**
    - Filters + condenses notifications
    - Pushes short events into a bounded queue for the foreground service
  - Optional: Boot receiver to start service **only after user opt-in**

- **Bluetooth**
  - Use SPP (`00001101-0000-1000-8000-00805F9B34FB`)
  - Application-level framing + CRC is mandatory (SPP is a stream, not messages)

- **Arduino Pro Micro (ATmega32U4)**
  - Streaming parser: resync, length-bound, CRC validated
  - Tiny display model: “link status + time + last event”
  - Never allocate dynamic Strings; fixed buffers only

## Protocol choice

Use **Protocol v2** (`smartglasses_companion/PROTOCOL.md`):

- **Start/end framing**
- **Byte-stuffing** for robust resync
- **CRC16** for integrity
- **ACKs** when requested (for “user-visible” events)

## Reliability / UX risks & mitigations

- **Bluetooth drops / background limits**
  - Keep the connection in a **foreground service** with a persistent notification.
  - Reconnect with backoff, don’t spam the stack.
  - Detect BT OFF and pause reconnect until ON.

- **Noisy notifications**
  - Filter ongoing/system notifications.
  - Rate-limit (e.g., max 1 update/sec).
  - Deduplicate by `(package, title, text)` hash for ~5–10 seconds.

- **ATmega32U4 RAM constraints**
  - Hard cap payload sizes (64 bytes).
  - Prefer Android-side truncation/pagination.
  - Use fixed arrays; avoid `String`.

- **Partial / malformed frames**
  - Decoder must: bound frame size, resync on SOF, discard on CRC fail.
  - Arduino should **fail silent** (ignore bad frames) and keep UI responsive.

- **Latency**
  - Keep “critical” events (CALL) ack-required.
  - Use short payloads; avoid JSON.

## Next steps (implementation)

- Android:
  - Add device picker UI + persist MAC
  - Implement connect/reconnect loop inside `CompanionForegroundService`
  - Implement TIME/CALL and hook `NotificationBridgeService` → service queue
  - Add runtime permission flows (Android 12+: BLUETOOTH_CONNECT, Android 13+: POST_NOTIFICATIONS)

- Arduino:
  - Flash `RetroWatch_Arduino/SmartGlasses_ProMicro_v2/SmartGlasses_ProMicro_v2.ino`
  - Verify display + ACKs using serial logging (optional)



