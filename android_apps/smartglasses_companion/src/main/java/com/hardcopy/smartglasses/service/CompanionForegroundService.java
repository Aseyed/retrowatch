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
    private static final String EXTRA_TCP_HOST = "tcp_host";
    private static final String EXTRA_TCP_PORT = "tcp_port";
    
    // TCP connection for testing (set to true to use TCP instead of Bluetooth)
    public static final boolean USE_TCP_FOR_TESTING = true;
    public static final String TCP_HOST = "192.168.1.100"; // Default - user can change in UI
    public static final int TCP_PORT = 8888; // Default - user can change in UI

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static volatile boolean runningHint = false;
    private static volatile CompanionForegroundService instance = null;

    private Handler mainHandler;
    private Thread ioThread;

    private BluetoothSocket socket;
    private TcpConnectionHelper tcpConnection;
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
    
    public static void connectTcp(Context context, String host, int port) {
        Intent i = new Intent(context, CompanionForegroundService.class);
        i.setAction(ACTION_CONNECT);
        i.putExtra(EXTRA_TCP_HOST, host);
        i.putExtra(EXTRA_TCP_PORT, port);
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
        instance = this;
        runningHint = true;
        mainHandler = new Handler(Looper.getMainLooper());
        ensureNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTI_ID, buildNotification("Idle"));

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CONNECT.equals(action)) {
                if (USE_TCP_FOR_TESTING) {
                    String host = intent.getStringExtra(EXTRA_TCP_HOST);
                    int port = intent.getIntExtra(EXTRA_TCP_PORT, TCP_PORT);
                    if (host == null) host = TCP_HOST;
                    connectTcpInternal(host, port);
                } else {
                    String mac = intent.getStringExtra(EXTRA_MAC);
                    if (mac != null && mac.length() > 0) {
                        connectInternal(mac);
                    } else {
                        updateNoti("Missing device MAC");
                    }
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
    
    private void connectTcpInternal(String host, int port) {
        shutdownIo();
        ioThread = new Thread(() -> runTcpIo(host, port), "SmartGlasses-TCP-IO");
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
    private boolean isConnected() {
        synchronized (this) {
            boolean connected = out != null && (socket != null || (tcpConnection != null && tcpConnection.isConnected()));
            android.util.Log.d("CompanionService", "isConnected() check: out=" + (out != null) + ", socket=" + (socket != null) + ", tcpConnection=" + (tcpConnection != null) + ", tcpConnected=" + (tcpConnection != null && tcpConnection.isConnected()) + ", result=" + connected);
            return connected;
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
            android.widget.Toast.makeText(context, "Not connected to server", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.util.Log.d("CompanionService", "Sending notify: " + text);
            instance.sendNotify(text);
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
    
    private void runTcpIo(String host, int port) {
        updateNoti("Connecting TCP...");
        try {
            tcpConnection = new TcpConnectionHelper(host, port);
            if (!tcpConnection.connect()) {
                updateNoti("TCP connection failed: " + host + ":" + port);
                android.util.Log.e("CompanionService", "TCP connection failed to " + host + ":" + port);
                return;
            }

            synchronized (this) {
                in = tcpConnection.getInputStream();
                out = tcpConnection.getOutputStream();
                android.util.Log.d("CompanionService", "TCP streams set - out=" + (out != null));
            }

            updateNoti("Connected (TCP)");
            
            // Wait a moment for streams to be fully ready
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            sendStatusConnected();
            
            // Send a test message to verify connection
            try {
                Thread.sleep(200);
                sendNotify("Connected to server");
                android.util.Log.d("CompanionService", "Sent test message after connection");
            } catch (Exception e) {
                android.util.Log.e("CompanionService", "Error sending test message: " + e.getMessage());
            }

            byte[] buf = new byte[256];
            while (!Thread.currentThread().isInterrupted() && tcpConnection.isConnected()) {
                int n = in.read(buf);
                if (n <= 0) break;
                decoder.feed(buf, n);
            }
        } catch (IOException e) {
            updateNoti("Disconnected (I/O)");
        } finally {
            try {
                // Send a test message before disconnecting
                synchronized (this) {
                    if (out != null && tcpConnection != null && tcpConnection.isConnected()) {
                        try {
                            sendNotify("Disconnecting...");
                            Thread.sleep(200); // Give it a moment to send
                        } catch (Exception e) {
                            android.util.Log.e("CompanionService", "Error sending disconnect message: " + e.getMessage());
                        }
                    }
                }
                sendStatusDisconnected();
            } catch (Throwable ignored) {}
            shutdownIo();
        }
    }

    private synchronized void sendFrame(byte type, byte flags, byte[] payload) {
        // Check if we have a valid output stream
        OutputStream streamToUse = out;
        if (streamToUse == null) {
            android.util.Log.w("CompanionService", "Cannot send frame - output stream is null (not connected)");
            android.util.Log.w("CompanionService", "  socket=" + (socket != null) + ", tcpConnection=" + (tcpConnection != null));
            if (tcpConnection != null) {
                android.util.Log.w("CompanionService", "  tcpConnection.isConnected()=" + tcpConnection.isConnected());
            }
            // Try to get output stream again if TCP connection exists
            if (tcpConnection != null && tcpConnection.isConnected()) {
                android.util.Log.d("CompanionService", "Attempting to recover output stream from TCP connection");
                streamToUse = tcpConnection.getOutputStream();
                if (streamToUse != null) {
                    out = streamToUse;
                    android.util.Log.d("CompanionService", "Recovered output stream");
                } else {
                    android.util.Log.e("CompanionService", "Failed to recover output stream");
                    return;
                }
            } else {
                return;
            }
        }
        
        try {
            byte[] frame = ProtoV2.encode(type, flags, txSeq++, payload);
            streamToUse.write(frame);
            streamToUse.flush();
            android.util.Log.d("CompanionService", "Frame sent: type=" + type + ", flags=" + flags + ", seq=" + (txSeq-1) + ", payloadLen=" + (payload != null ? payload.length : 0) + ", frameSize=" + frame.length + " bytes");
        } catch (IOException e) {
            android.util.Log.e("CompanionService", "Error sending frame: " + e.getMessage(), e);
            // Best effort: connection will be torn down by read loop or next write.
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
            if (tcpConnection != null) {
                tcpConnection.disconnect();
                tcpConnection = null;
            }
            in = null;
            out = null;
        }
    }

    private boolean hasConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) return true;
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "SmartGlasses Companion", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Connection status for SmartGlasses");
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, intent,
                Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_glasses)
                .setContentTitle("SmartGlasses")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private void updateNoti(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID, buildNotification(text));
    }
}


