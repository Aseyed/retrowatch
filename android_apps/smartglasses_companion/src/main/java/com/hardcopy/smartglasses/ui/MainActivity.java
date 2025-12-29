package com.hardcopy.smartglasses.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hardcopy.smartglasses.R;
import com.hardcopy.smartglasses.service.CompanionForegroundService;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PICK_DEVICE = 10;
    private static final int REQ_BT_CONNECT = 11;
    private static final int REQ_POST_NOTI = 12;

    private TextView statusText;
    private TextView deviceText;
    private EditText tcpHostInput;
    private EditText tcpPortInput;
    private EditText messageInput;

    private static final String PREFS = "smartglasses_prefs";
    private static final String KEY_MAC = "device_mac";
    private static final String KEY_NAME = "device_name";
    private static final String KEY_TCP_HOST = "tcp_host";
    private static final String KEY_TCP_PORT = "tcp_port";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        deviceText = findViewById(R.id.deviceText);
        tcpHostInput = findViewById(R.id.tcpHostInput);
        tcpPortInput = findViewById(R.id.tcpPortInput);

        // Load saved TCP settings
        String savedHost = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TCP_HOST, CompanionForegroundService.TCP_HOST);
        int savedPort = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_TCP_PORT, CompanionForegroundService.TCP_PORT);
        tcpHostInput.setText(savedHost);
        tcpPortInput.setText(String.valueOf(savedPort));

        Button startBtn = findViewById(R.id.startServiceBtn);
        Button stopBtn = findViewById(R.id.stopServiceBtn);
        Button notiAccessBtn = findViewById(R.id.openNotiAccessBtn);
        Button pickBtn = findViewById(R.id.pickDeviceBtn);
        Button connectBtn = findViewById(R.id.connectBtn);
        Button disconnectBtn = findViewById(R.id.disconnectBtn);
        messageInput = findViewById(R.id.messageInput);
        Button sendMessageBtn = findViewById(R.id.sendMessageBtn);
        Button sendClockBtn = findViewById(R.id.sendClockBtn);
        Button sendBatteryBtn = findViewById(R.id.sendBatteryBtn);

        startBtn.setOnClickListener(v -> CompanionForegroundService.start(this));
        stopBtn.setOnClickListener(v -> CompanionForegroundService.stop(this));
        notiAccessBtn.setOnClickListener(v -> openNotificationAccessSettings());
        pickBtn.setOnClickListener(v -> pickDevice());
        connectBtn.setOnClickListener(v -> connect());
        disconnectBtn.setOnClickListener(v -> {
            CompanionForegroundService.disconnect(this);
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        });
        sendMessageBtn.setOnClickListener(v -> sendMessage());
        sendClockBtn.setOnClickListener(v -> sendClockData());
        sendBatteryBtn.setOnClickListener(v -> sendBatteryStatus());

        ensureRuntimePermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusText.setText(buildStatusText());
        deviceText.setText(getSavedDeviceLine());
    }

    private String buildStatusText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Service: ").append(CompanionForegroundService.isRunningHint()).append("\n");
        sb.append("Notification access: ").append(isNotificationListenerEnabled(this) ? "ENABLED" : "DISABLED").append("\n");
        sb.append("Android: ").append(Build.VERSION.SDK_INT);
        return sb.toString();
    }

    private String getSavedDeviceLine() {
        String name = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_NAME, null);
        String mac = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MAC, null);
        if (mac == null) return "Device: (not selected)";
        if (name == null) name = "(unknown)";
        return "Device: " + name + " (" + mac + ")";
    }

    private void pickDevice() {
        if (!ensureBtConnectPermission()) return;
        startActivityForResult(new Intent(this, DevicePickerActivity.class), REQ_PICK_DEVICE);
    }

    private void connect() {
        // Use TCP if enabled in service, otherwise use Bluetooth
        if (CompanionForegroundService.USE_TCP_FOR_TESTING) {
            // Get IP and port from input fields
            String host = tcpHostInput.getText().toString().trim();
            String portStr = tcpPortInput.getText().toString().trim();
            
            if (host.isEmpty()) {
                host = CompanionForegroundService.TCP_HOST;
            }
            
            int port = CompanionForegroundService.TCP_PORT;
            try {
                if (!portStr.isEmpty()) {
                    port = Integer.parseInt(portStr);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid port number, using default: " + port, Toast.LENGTH_SHORT).show();
            }
            
            // Save settings
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_TCP_HOST, host)
                    .putInt(KEY_TCP_PORT, port)
                    .apply();
            
            Toast.makeText(this, "Connecting to " + host + ":" + port + "...", Toast.LENGTH_SHORT).show();
            CompanionForegroundService.connectTcp(this, host, port);
        } else {
            if (!ensureBtConnectPermission()) return;
            String mac = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MAC, null);
            if (mac == null || mac.length() == 0) {
                pickDevice();
                return;
            }
            CompanionForegroundService.connect(this, mac);
        }
    }
    
    private void sendMessage() {
        if (!CompanionForegroundService.isRunningHint().equals("RUNNING")) {
            Toast.makeText(this, "Service not running. Please start the service first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }
        
        CompanionForegroundService.sendNotify(this, message);
        messageInput.setText(""); // Clear input
    }
    
    private void sendClockData() {
        if (!CompanionForegroundService.isRunningHint().equals("RUNNING")) {
            Toast.makeText(this, "Service not running. Please start the service first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        CompanionForegroundService.sendTime(this);
    }
    
    private void sendBatteryStatus() {
        if (!CompanionForegroundService.isRunningHint().equals("RUNNING")) {
            Toast.makeText(this, "Service not running. Please start the service first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        CompanionForegroundService.sendBatteryStatus(this);
    }

    private void openNotificationAccessSettings() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void ensureRuntimePermissions() {
        ensureBtConnectPermission();
        ensurePostNotificationsPermission();
    }

    private boolean ensureBtConnectPermission() {
        if (Build.VERSION.SDK_INT < 31) return true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQ_BT_CONNECT);
        return false;
    }

    private void ensurePostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < 33) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTI);
    }

    private static boolean isNotificationListenerEnabled(Context context) {
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (TextUtils.isEmpty(flat)) return false;
        String pkg = context.getPackageName();
        for (String cn : flat.split(":")) {
            ComponentName componentName = ComponentName.unflattenFromString(cn);
            if (componentName != null && pkg.equals(componentName.getPackageName())) return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_DEVICE && resultCode == RESULT_OK && data != null) {
            String name = data.getStringExtra(DevicePickerActivity.EXTRA_DEVICE_NAME);
            String mac = data.getStringExtra(DevicePickerActivity.EXTRA_DEVICE_MAC);
            if (mac != null && mac.length() > 0) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putString(KEY_NAME, name)
                        .putString(KEY_MAC, mac)
                        .apply();
                deviceText.setText(getSavedDeviceLine());
            }
        }
    }
}


