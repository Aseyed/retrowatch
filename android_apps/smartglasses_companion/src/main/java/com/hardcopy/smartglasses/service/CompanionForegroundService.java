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
        android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
        android.content.Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (level * 100) / scale;
            String batteryText = "Battery: " + batteryPct + "%";
            sendFrame(ProtoV2.TYPE_NOTIFY, ProtoV2.FLAG_ACK_REQ, batteryText.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    // Static methods for UI access
    public static void sendNotify(Context context, String text) {
        if (instance != null) {
            instance.sendNotify(text);
        }
    }
    
    public static void sendTime(Context context) {
        if (instance != null) {
            instance.sendTime();
        }
    }
    
    public static void sendBatteryStatus(Context context) {
        if (instance != null) {
            instance.sendBatteryStatus();
        }
    }
    
    public static void sendCall(Context context, String callerInfo) {
        if (instance != null) {
            instance.sendCall(callerInfo);
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
            }

            updateNoti("Connected (TCP)");
            sendStatusConnected();

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
                sendStatusDisconnected();
            } catch (Throwable ignored) {}
            shutdownIo();
        }
    }

    private synchronized void sendFrame(byte type, byte flags, byte[] payload) {
        if (out == null) return;
        byte[] frame = ProtoV2.encode(type, flags, txSeq++, payload);
        try {
            out.write(frame);
            out.flush();
        } catch (IOException e) {
            // Best effort: connection will be torn down by read loop or next write.
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


