# TCP and Bluetooth Compatibility

## Overview

Both Android applications (RetroWatch and SmartGlasses Companion) support both TCP and Bluetooth connections. The mode is controlled by a flag, and the implementations are designed to **not interfere** with each other.

## Mode Selection

### RetroWatch App
- **Flag**: `USE_TCP_FOR_TESTING` in `RetroWatchService.java`
- **Default**: `true` (TCP mode)
- **Location**: `android_apps/retrowatch/src/main/java/com/hardcopy/retrowatch/service/RetroWatchService.java:88`

### SmartGlasses Companion App
- **Flag**: `USE_TCP_FOR_TESTING` in `CompanionForegroundService.java`
- **Default**: `true` (TCP mode)
- **Location**: `android_apps/smartglasses_companion/src/main/java/com/hardcopy/smartglasses/service/CompanionForegroundService.java:55`

## How It Works

### Connection Mode Selection
The code uses conditional logic based on `USE_TCP_FOR_TESTING`:

```java
if (USE_TCP_FOR_TESTING) {
    // Use TCP connection
    mTcpManager.connect();
    mTransactionBuilder = new TransactionBuilder(mTcpManager, mActivityHandler);
} else {
    // Use Bluetooth connection
    mBtManager.connect();
    mTransactionBuilder = new TransactionBuilder(mBtManager, mActivityHandler);
}
```

### TransactionBuilder Compatibility
The `TransactionBuilder` class is designed to work with **both** connection types:
- It accepts either `TcpConnectionManager` or `BluetoothManager` in its constructor
- The `sendTransaction()` method automatically detects which manager is active:
  - First tries TCP if `mTcpManager` is set and connected
  - Falls back to Bluetooth if `mBTManager` is set and connected

### No Interference
1. **Separate Managers**: TCP and Bluetooth use completely separate manager instances
2. **Conditional Initialization**: Only the active mode's manager is initialized
3. **State Checks**: All connection state checks respect the active mode
4. **Message Handling**: Both modes use the same message constants (they share integer values)

## Switching Between Modes

To switch from TCP to Bluetooth (or vice versa):

1. **RetroWatch**: Change `USE_TCP_FOR_TESTING = false` in `RetroWatchService.java`
2. **SmartGlasses**: Change `USE_TCP_FOR_TESTING = false` in `CompanionForegroundService.java`
3. Rebuild the app

## Important Notes

- ✅ **Bluetooth functionality is preserved** - When `USE_TCP_FOR_TESTING = false`, all Bluetooth code works exactly as before
- ✅ **No conflicts** - TCP and Bluetooth managers are never both active simultaneously
- ✅ **TransactionBuilder is mode-agnostic** - It automatically uses the correct connection based on which manager was passed to it
- ✅ **All features work in both modes** - Send messages, clock data, battery status, call notifications, etc.

## Testing

To test Bluetooth mode:
1. Set `USE_TCP_FOR_TESTING = false`
2. Rebuild the app
3. Connect to a Bluetooth device as normal
4. All functionality should work identically to TCP mode

To test TCP mode:
1. Set `USE_TCP_FOR_TESTING = true` (default)
2. Enter server IP and port in the UI
3. Click Connect
4. All functionality should work over TCP

