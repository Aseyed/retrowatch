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
    private TextView outputText;
    private EditText messageInput;

    private static final String PREFS = "smartglasses_prefs";
    private static final String KEY_MAC = "device_mac";
    private static final String KEY_NAME = "device_name";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        deviceText = findViewById(R.id.deviceText);
        outputText = findViewById(R.id.outputText);

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

        startBtn.setOnClickListener(v -> {
            CompanionForegroundService.start(this);
            appendOutput("Service started\n");
        });
        stopBtn.setOnClickListener(v -> {
            CompanionForegroundService.stop(this);
            appendOutput("Service stopped\n");
        });
        notiAccessBtn.setOnClickListener(v -> openNotificationAccessSettings());
        pickBtn.setOnClickListener(v -> pickDevice());
        connectBtn.setOnClickListener(v -> connect());
        disconnectBtn.setOnClickListener(v -> {
            CompanionForegroundService.disconnect(this);
            appendOutput("Disconnected\n");
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        });
        sendMessageBtn.setOnClickListener(v -> sendMessage());
        sendClockBtn.setOnClickListener(v -> sendClockData());
        sendBatteryBtn.setOnClickListener(v -> sendBatteryStatus());

        // Check notification service on startup (like App Inventor Screen1.Initialize)
        checkNotificationService();
        ensureRuntimePermissions();
    }
    
    private void checkNotificationService() {
        if (isNotificationListenerEnabled(this)) {
            appendOutput("notification started\n");
        } else {
            appendOutput("notification service not enabled\n");
        }
    }
    
    private void appendOutput(String text) {
        if (outputText != null) {
            String current = outputText.getText().toString();
            outputText.setText(current + text);
            // Auto-scroll to bottom (find parent ScrollView)
            android.view.ViewParent parent = outputText.getParent();
            if (parent instanceof android.widget.ScrollView) {
                final android.widget.ScrollView scrollView = (android.widget.ScrollView) parent;
                scrollView.post(() -> {
                    scrollView.fullScroll(android.view.View.FOCUS_DOWN);
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        deviceText.setText(getSavedDeviceLine());
        checkNotificationService();
    }
    
    private void updateStatus() {
        String status = buildStatusText();
        // Update status text based on connection (like App Inventor)
        CompanionForegroundService service = CompanionForegroundService.getInstance();
        if (service != null && service.isConnected()) {
            statusText.setText("connected");
        } else if (status.contains("STOPPED")) {
            statusText.setText("disconnected");
        } else {
            statusText.setText(status);
        }
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
        if (!ensureBtConnectPermission()) return;
        String mac = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MAC, null);
        if (mac == null || mac.length() == 0) {
            pickDevice();
            return;
        }
        String name = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_NAME, null);
        appendOutput((name != null ? name : mac) + " connected\n");
        statusText.setText("connected");
        CompanionForegroundService.connect(this, mac);
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
        
        appendOutput(message + "\n");
        CompanionForegroundService.sendNotify(this, message);
        messageInput.setText(""); // Clear input
    }
    
    private void sendClockData() {
        if (!CompanionForegroundService.isRunningHint().equals("RUNNING")) {
            Toast.makeText(this, "Service not running. Please start the service first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);
        int second = cal.get(java.util.Calendar.SECOND);
        String timeStr = String.format("%02d:%02d:%02d", hour, minute, second);
        appendOutput(timeStr + "\n");
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
                appendOutput((name != null ? name : mac) + "\n");
            }
        }
    }
}


