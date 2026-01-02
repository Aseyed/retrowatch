package com.hardcopy.smartglasses.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.hardcopy.smartglasses.R;
import com.hardcopy.smartglasses.protocol.ProtoV2;
import com.hardcopy.smartglasses.protocol.ProtoV2StreamDecoder;
import com.hardcopy.smartglasses.ui.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Minimal production-oriented skeleton:
 * - Runs as a foreground service (so it can stay alive for a connected device).
 * - Manages a single classic SPP connection (HC-05/HC-06 style).
 *
 * NOTE: For a real product, persist the target device MAC + use exponential backoff + watchdog.
 * This skeleton is intentionally small but correctly structured.
 */
public class CompanionForegroundService extends Service {

    private static final String CHANNEL_ID = "smartglasses_companion";
    private static final int NOTI_ID = 1001;

    private static final String ACTION_CONNECT = "com.hardcopy.smartglasses.action.CONNECT";
    private static final String ACTION_DISCONNECT = "com.hardcopy.smartglasses.action.DISCONNECT";
    private static final String EXTRA_MAC = "mac";

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static volatile boolean runningHint = false;
    private static volatile CompanionForegroundService instance = null;

    private Handler mainHandler;
    private Thread ioThread;
    private Handler timerHandler;
    private Runnable timeSender;
    private Handler notificationDelayHandler;
    private Runnable pendingNotificationSender;

    private BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;

    private final ProtoV2StreamDecoder decoder = new ProtoV2StreamDecoder(new ProtoV2StreamDecoder.Listener() {
        @Override
        public void onFrame(byte ver, byte type, byte flags, byte seq, byte[] payload) {
            // TODO: handle ACKs / device telemetry. For now, ignore.
        }

        @Override
        public void onBadFrame(String reason) {
            // ignore; decoder is designed to recover
        }
    });

    private byte txSeq = 0;

    public static void start(Context context) {
        Intent i = new Intent(context, CompanionForegroundService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
        else context.startService(i);
    }

    public static void connect(Context context, String macAddress) {
        Intent i = new Intent(context, CompanionForegroundService.class);
        i.setAction(ACTION_CONNECT);
        i.putExtra(EXTRA_MAC, macAddress);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
        else context.startService(i);
    }
    
    public static void disconnect(Context context) {
        Intent i = new Intent(context, CompanionForegroundService.class);
        i.setAction(ACTION_DISCONNECT);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i);
        else context.startService(i);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, CompanionForegroundService.class));
    }

    public static String isRunningHint() {
        return runningHint ? "RUNNING" : "STOPPED";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            runningHint = true;
            mainHandler = new Handler(Looper.getMainLooper());
            timerHandler = new Handler(Looper.getMainLooper());
            notificationDelayHandler = new Handler(Looper.getMainLooper());
            ensureNotificationChannel();
            // Don't start periodic time sender until connected
            // startPeriodicTimeSender();
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error in onCreate: " + e.getMessage(), e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            startForeground(NOTI_ID, buildNotification("Idle"));
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error starting foreground: " + e.getMessage(), e);
            // Try to continue anyway
        }

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CONNECT.equals(action)) {
                String mac = intent.getStringExtra(EXTRA_MAC);
                if (mac != null && mac.length() > 0) {
                    connectInternal(mac);
                } else {
                    updateNoti("Missing device MAC");
                }
            } else if (ACTION_DISCONNECT.equals(action)) {
                // Send a test message before disconnecting
                synchronized (this) {
                    if (isConnected()) {
                        try {
                            sendNotify("Disconnecting...");
                            Thread.sleep(200); // Give it a moment to send
                        } catch (Exception e) {
                            android.util.Log.e("CompanionService", "Error sending disconnect message: " + e.getMessage());
                        }
                    }
                }
                updateNoti("Disconnected");
                shutdownIo();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        instance = null;
        runningHint = false;
        stopPeriodicTimeSender();
        // Cancel pending notification
        if (notificationDelayHandler != null && pendingNotificationSender != null) {
            notificationDelayHandler.removeCallbacks(pendingNotificationSender);
            pendingNotificationSender = null;
        }
        shutdownIo();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connectInternal(String macAddress) {
        shutdownIo();
        ioThread = new Thread(() -> runIo(macAddress), "SmartGlasses-IO");
        ioThread.start();
    }

    public synchronized void sendStatusConnected() {
        sendFrame(ProtoV2.TYPE_STATUS, ProtoV2.FLAG_ACK_REQ, new byte[]{ProtoV2.STATUS_CONNECTED});
    }

    public synchronized void sendStatusDisconnected() {
        sendFrame(ProtoV2.TYPE_STATUS, ProtoV2.FLAG_ACK_REQ, new byte[]{ProtoV2.STATUS_DISCONNECTED});
    }

    public synchronized void sendNotify(String text) {
        if (text == null) text = "";
        sendFrame(ProtoV2.TYPE_NOTIFY, ProtoV2.FLAG_ACK_REQ, text.getBytes(StandardCharsets.UTF_8));
    }
    
    // Send plain text message (for App Inventor compatibility)
    public synchronized void sendPlainText(String text) {
        if (text == null || text.isEmpty()) return;
        if (!isConnected()) {
            android.util.Log.w("CompanionService", "Cannot send plain text - not connected");
            return;
        }
        try {
            synchronized (this) {
                if (out != null) {
                    byte[] data = text.getBytes(StandardCharsets.UTF_8);
                    out.write(data);
                    out.flush();
                    android.util.Log.d("CompanionService", "Sent plain text: " + text);
                    
                    // Print message in output box (remove newline for display)
                    String displayText = text.trim();
                    if (displayText.length() > 0) {
                        com.hardcopy.smartglasses.ui.MainActivity.appendToOutput(displayText + "\n");
                    }
                }
            }
        } catch (IOException e) {
            android.util.Log.e("CompanionService", "Error sending plain text: " + e.getMessage(), e);
        }
    }
    
    // Forward notification (App Inventor format: "N:title:text\n")
    // Delays sending by 5 seconds and resets clock timer
    public synchronized void forwardNotification(String packageName, String title, String text) {
        if (!isConnected()) {
            android.util.Log.d("CompanionService", "Cannot forward notification - not connected");
            return;
        }
        
        // Reset clock timer when notification arrives
        android.util.Log.d("CompanionService", "Notification received - resetting clock timer");
        startPeriodicTimeSender();
        
        // Cancel any pending notification send
        if (notificationDelayHandler != null && pendingNotificationSender != null) {
            notificationDelayHandler.removeCallbacks(pendingNotificationSender);
            pendingNotificationSender = null;
        }
        
        // Create delayed notification sender
        final String message = "N:" + (title != null ? title : "") + "\n" + ":" + (text != null ? text : "");
        pendingNotificationSender = new Runnable() {
            @Override
            public void run() {
                try {
                    if (isConnected()) {
                        sendPlainText(message);
                        android.util.Log.d("CompanionService", "Forwarded notification after 5s delay: " + message);
                    }
                } catch (Exception e) {
                    android.util.Log.e("CompanionService", "Error forwarding notification: " + e.getMessage(), e);
                }
                pendingNotificationSender = null;
            }
        };
        
        // Schedule notification to be sent after 5 seconds
        if (notificationDelayHandler != null) {
            notificationDelayHandler.postDelayed(pendingNotificationSender, 5000);
            android.util.Log.d("CompanionService", "Scheduled notification to be sent in 5 seconds");
        }
    }
    
    // Send time message (App Inventor format: "T:HH:MM\n")
    public synchronized void sendTimeMessage() {
        if (!isConnected()) {
            return;
        }
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = cal.get(java.util.Calendar.MINUTE);
            String timeStr = String.format("T:%02d:%02d\n", hour, minute);
            sendPlainText(timeStr);
            android.util.Log.d("CompanionService", "Sent time message: " + timeStr);
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error sending time message: " + e.getMessage(), e);
        }
    }
    
    // Start periodic time sender (every few seconds like App Inventor Clock.Timer)
    private void startPeriodicTimeSender() {
        stopPeriodicTimeSender(); // Stop any existing timer
        timeSender = new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    sendTimeMessage();
                }
                // Schedule next execution (every 5 seconds)
                if (timerHandler != null) {
                    timerHandler.postDelayed(this, 5000);
                }
            }
        };
        if (timerHandler != null) {
            timerHandler.postDelayed(timeSender, 5000);
        }
    }
    
    // Stop periodic time sender
    private void stopPeriodicTimeSender() {
        if (timerHandler != null && timeSender != null) {
            timerHandler.removeCallbacks(timeSender);
            timeSender = null;
        }
    }
    
    public synchronized void sendCall(String callerInfo) {
        if (callerInfo == null) callerInfo = "Unknown";
        // Truncate to 64 bytes for protocol
        byte[] payload = callerInfo.getBytes(StandardCharsets.UTF_8);
        if (payload.length > 64) {
            payload = java.util.Arrays.copyOf(payload, 64);
        }
        sendFrame(ProtoV2.TYPE_CALL, ProtoV2.FLAG_ACK_REQ, payload);
    }
    
    public synchronized void sendTime() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        byte[] payload = new byte[7];
        int year = cal.get(java.util.Calendar.YEAR);
        payload[0] = (byte) (year & 0xFF);
        payload[1] = (byte) ((year >> 8) & 0xFF);
        payload[2] = (byte) (cal.get(java.util.Calendar.MONTH) + 1); // 1-12
        payload[3] = (byte) cal.get(java.util.Calendar.DAY_OF_MONTH); // 1-31
        payload[4] = (byte) cal.get(java.util.Calendar.HOUR_OF_DAY); // 0-23
        payload[5] = (byte) cal.get(java.util.Calendar.MINUTE); // 0-59
        payload[6] = (byte) cal.get(java.util.Calendar.SECOND); // 0-59
        sendFrame(ProtoV2.TYPE_TIME, ProtoV2.FLAG_ACK_REQ, payload);
    }
    
    public synchronized void sendBatteryStatus() {
        try {
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Context appContext = getApplicationContext();
            if (appContext == null) {
                android.util.Log.e("CompanionService", "Cannot get application context for battery status");
                return;
            }
            android.content.Intent batteryStatus = appContext.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (scale > 0) {
                    int batteryPct = (level * 100) / scale;
                    String batteryText = "Battery: " + batteryPct + "%";
                    sendFrame(ProtoV2.TYPE_NOTIFY, ProtoV2.FLAG_ACK_REQ, batteryText.getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error getting battery status: " + e.getMessage(), e);
        }
    }
    
    // Check if service is connected
    public boolean isConnected() {
        synchronized (this) {
            // For Bluetooth: check socket and output stream
            if (socket != null && out != null) {
                boolean connected = true;
                android.util.Log.d("CompanionService", "isConnected() BT check: socket=" + (socket != null) + ", out=" + (out != null) + ", result=" + connected);
                return connected;
            }
            android.util.Log.d("CompanionService", "isConnected() check: not connected (no active connection)");
            return false;
        }
    }
    
    // Static methods for UI access
    public static void sendNotify(Context context, String text) {
        if (instance == null) {
            android.util.Log.w("CompanionService", "Cannot send notify - service not running");
            android.widget.Toast.makeText(context, "Service not running", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (!instance.isConnected()) {
            android.util.Log.w("CompanionService", "Cannot send notify - not connected");
            android.widget.Toast.makeText(context, "Not connected", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Send in App Inventor format: "M:message\n"
            String message = "M:" + text + "\n";
            instance.sendPlainText(message);
            android.widget.Toast.makeText(context, "Message sent: " + text, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error sending notify: " + e.getMessage(), e);
            android.widget.Toast.makeText(context, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void sendTime(Context context) {
        if (instance == null) {
            android.util.Log.w("CompanionService", "Cannot send time - service not running");
            android.widget.Toast.makeText(context, "Service not running", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (!instance.isConnected()) {
            android.util.Log.w("CompanionService", "Cannot send time - not connected");
            android.widget.Toast.makeText(context, "Not connected to server", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.util.Log.d("CompanionService", "Sending time");
            instance.sendTime();
            android.widget.Toast.makeText(context, "Clock data sent", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error sending time: " + e.getMessage(), e);
            android.widget.Toast.makeText(context, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void sendBatteryStatus(Context context) {
        if (instance == null) {
            android.util.Log.w("CompanionService", "Cannot send battery status - service not running");
            android.widget.Toast.makeText(context, "Service not running", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (!instance.isConnected()) {
            android.util.Log.w("CompanionService", "Cannot send battery status - not connected");
            android.widget.Toast.makeText(context, "Not connected to server", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.util.Log.d("CompanionService", "Sending battery status");
            instance.sendBatteryStatus();
            android.widget.Toast.makeText(context, "Battery status sent", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error sending battery status: " + e.getMessage(), e);
            android.widget.Toast.makeText(context, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    public static void sendCall(Context context, String callerInfo) {
        if (instance != null) {
            try {
                instance.sendCall(callerInfo);
            } catch (Exception e) {
                android.util.Log.e("CompanionService", "Error sending call: " + e.getMessage(), e);
            }
        } else {
            android.util.Log.w("CompanionService", "Cannot send call - service not running");
        }
    }
    
    public static CompanionForegroundService getInstance() {
        return instance;
    }

    private void runIo(String mac) {
        updateNoti("Connecting...");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            updateNoti("No Bluetooth adapter");
            return;
        }
        if (!hasConnectPermission()) {
            updateNoti("Missing BLUETOOTH_CONNECT permission");
            return;
        }
        try {
            BluetoothDevice device = adapter.getRemoteDevice(mac);
            BluetoothSocket s = device.createRfcommSocketToServiceRecord(SPP_UUID);
            adapter.cancelDiscovery();
            s.connect();

            synchronized (this) {
                socket = s;
                in = s.getInputStream();
                out = s.getOutputStream();
            }

            updateNoti("Connected");
            sendStatusConnected();
            // Restart periodic time sender when connected
            startPeriodicTimeSender();
            // Notify UI about connection (will be handled by status update)
            android.util.Log.d("CompanionService", "Connected successfully");

            byte[] buf = new byte[256];
            while (!Thread.currentThread().isInterrupted()) {
                int n = in.read(buf);
                if (n <= 0) break;
                decoder.feed(buf, n);
            }
        } catch (IOException e) {
            updateNoti("Disconnected (I/O)");
        } catch (SecurityException se) {
            updateNoti("Bluetooth permission denied");
        } finally {
            try {
                sendStatusDisconnected();
            } catch (Throwable ignored) {}
            shutdownIo();
        }
    }

    private synchronized void sendFrame(byte type, byte flags, byte[] payload) {
        // Get output stream from Bluetooth socket
        OutputStream streamToUse = null;
        
        // Try stored reference first
        if (out != null && socket != null) {
            streamToUse = out;
            android.util.Log.d("CompanionService", "Using stored output stream reference (Bluetooth)");
        }
        
        // Try Bluetooth socket as fallback
        if (streamToUse == null && socket != null) {
            try {
                streamToUse = socket.getOutputStream();
                if (streamToUse != null) {
                    synchronized (this) {
                        out = streamToUse;
                    }
                    android.util.Log.d("CompanionService", "Recovered output stream from Bluetooth socket");
                }
            } catch (IOException e) {
                android.util.Log.e("CompanionService", "Failed to get output stream from socket: " + e.getMessage());
            }
        }
        
        // Final check - if still null, we can't send
        if (streamToUse == null) {
            android.util.Log.e("CompanionService", "Cannot send frame - output stream is null (not connected)");
            android.util.Log.e("CompanionService", "  socket=" + (socket != null));
            return;
        }
        
        try {
            byte[] frame = ProtoV2.encode(type, flags, txSeq++, payload);
            
            // Log frame details before sending
            android.util.Log.d("CompanionService", "Sending frame: type=" + type + ", flags=" + flags + ", seq=" + (txSeq-1) + ", payloadLen=" + (payload != null ? payload.length : 0) + ", frameSize=" + frame.length + " bytes");
            
            // Write frame
            streamToUse.write(frame);
            
            // CRITICAL: Flush immediately to ensure data is sent
            // Flush must be called to ensure data is actually transmitted over the network
            streamToUse.flush();
            
            android.util.Log.d("CompanionService", "Frame sent and flushed successfully - " + frame.length + " bytes transmitted");
            
            // Verify stream is still valid
            if (streamToUse != out) {
                android.util.Log.w("CompanionService", "Warning: stream reference changed after write");
            }
        } catch (java.net.SocketException e) {
            android.util.Log.e("CompanionService", "Socket error sending frame: " + e.getMessage(), e);
            // Connection is broken - clear output stream
            synchronized (this) {
                out = null;
            }
        } catch (IOException e) {
            android.util.Log.e("CompanionService", "Error sending frame: " + e.getMessage(), e);
            // Best effort: connection will be torn down by read loop or next write.
            // Clear output stream on error
            synchronized (this) {
                if (streamToUse == out) {
                    out = null;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Unexpected error sending frame: " + e.getMessage(), e);
        }
    }

    private void shutdownIo() {
        Thread t = ioThread;
        ioThread = null;
        if (t != null) t.interrupt();

        synchronized (this) {
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
            socket = null;
            in = null;
            out = null;
        }
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) return true;
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT < 26) return;
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) {
                android.util.Log.e("CompanionService", "NotificationManager is null");
                return;
            }
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SmartGlasses Companion", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Connection status for SmartGlasses");
            nm.createNotificationChannel(channel);
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error creating notification channel: " + e.getMessage(), e);
        }
    }

    private Notification buildNotification(String text) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(
                    this, 0, intent,
                    Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0
            );
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("SmartGlasses")
                    .setContentText(text)
                    .setOngoing(true)
                    .setContentIntent(pi);
            
            // Try to set icon, fallback to default if not found
            try {
                builder.setSmallIcon(R.drawable.ic_stat_glasses);
            } catch (Exception e) {
                android.util.Log.w("CompanionService", "Icon not found, using default: " + e.getMessage());
                builder.setSmallIcon(android.R.drawable.ic_dialog_info);
            }
            
            return builder.build();
        } catch (Exception e) {
            android.util.Log.e("CompanionService", "Error building notification: " + e.getMessage(), e);
            // Return a minimal notification as fallback
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("SmartGlasses")
                    .setContentText(text != null ? text : "Running")
                    .build();
        }
    }

    private void updateNoti(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID, buildNotification(text));
    }
}


