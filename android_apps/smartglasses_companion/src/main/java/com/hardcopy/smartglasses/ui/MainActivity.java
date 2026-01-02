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
import android.os.Handler;
import android.os.Looper;
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
    private TextView outputText;
    private EditText messageInput;
    private Button selectDeviceBtn;
    private Button sendMessageBtn;

    private static final String PREFS = "smartglasses_prefs";
    private static final String KEY_MAC = "device_mac";
    private static final String KEY_NAME = "device_name";
    
    private Handler statusUpdateHandler;
    private Runnable statusUpdateRunnable;
    
    // Static reference for service to update output
    private static MainActivity instance = null;
    private static Handler mainHandler = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        outputText = findViewById(R.id.outputText);
        messageInput = findViewById(R.id.messageInput);
        selectDeviceBtn = findViewById(R.id.selectDeviceBtn);
        sendMessageBtn = findViewById(R.id.sendMessageBtn);
        Button clearLogBtn = findViewById(R.id.clearLogBtn);

        // Initialize status
        statusText.setText("disconnected");
        
        // Start service automatically
        CompanionForegroundService.start(this);
        
        // Set up select device button (top left) - combines pick and connect
        selectDeviceBtn.setOnClickListener(v -> selectAndConnectDevice());
        
        // Set up send button
        sendMessageBtn.setOnClickListener(v -> sendMessage());
        
        // Set up clear log button
        clearLogBtn.setOnClickListener(v -> {
            if (outputText != null) {
                outputText.setText("");
            }
        });
        
        // Request all necessary permissions on startup (show only one dialog)
        requestAllPermissions();
        
        // Check notification service on startup
        checkNotificationService();
        
        // Start periodic status updates
        statusUpdateHandler = new Handler(Looper.getMainLooper());
        startStatusUpdates();
        
        // Set static instance for service access
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
        mainHandler = null;
        if (statusUpdateHandler != null && statusUpdateRunnable != null) {
            statusUpdateHandler.removeCallbacks(statusUpdateRunnable);
        }
    }
    
    private void startStatusUpdates() {
        statusUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                updateStatus();
                statusUpdateHandler.postDelayed(this, 1000); // Update every second
            }
        };
        statusUpdateHandler.post(statusUpdateRunnable);
    }
    
    
    private void selectAndConnectDevice() {
        // Request permission first if needed
        if (!ensureBtConnectPermission()) {
            return;
        }
        
        // Check if we already have a device selected
        String mac = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MAC, null);
        if (mac != null && mac.length() > 0) {
            // Already have device, just connect
            connectToDevice(mac);
        } else {
            // No device selected, show picker
            pickDevice();
        }
    }
    
    private void connectToDevice(String mac) {
        String name = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_NAME, null);
        CompanionForegroundService.connect(this, mac);
        // Status will be updated by statusUpdateRunnable
    }
    
    private void checkNotificationService() {
        if (isNotificationListenerEnabled(this)) {
            appendOutput("notification started\n");
        } else {
            appendOutput("notification service not enabled\n");
            // Request notification access
            requestNotificationAccess();
        }
    }
    
    private void requestNotificationAccess() {
        if (!isNotificationListenerEnabled(this)) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Notification Access Required")
                    .setMessage("Please enable notification access to send notifications to Bluetooth device.")
                    .setPositiveButton("Settings", (dialog, which) -> openNotificationAccessSettings())
                    .setNegativeButton("Later", null)
                    .show();
        }
    }
    
    private void appendOutput(String text) {
        if (outputText != null) {
            String current = outputText.getText().toString();
            outputText.setText(current + text);
            // Auto-scroll to bottom
            android.widget.ScrollView scrollView = findViewById(R.id.outputScrollView);
            if (scrollView != null) {
                scrollView.post(() -> {
                    scrollView.fullScroll(android.view.View.FOCUS_DOWN);
                });
            }
        }
    }
    
    // Static method for service to append to output
    public static void appendToOutput(String text) {
        if (mainHandler != null && instance != null) {
            mainHandler.post(() -> {
                if (instance.outputText != null) {
                    instance.appendOutput(text);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        checkNotificationService();
    }
    
    private void updateStatus() {
        // Update status text based on connection (like App Inventor)
        CompanionForegroundService service = CompanionForegroundService.getInstance();
        boolean wasConnected = statusText.getText().toString().equals("connected");
        boolean isConnected = (service != null && service.isConnected());
        
        if (isConnected && !wasConnected) {
            // Just connected - show device name in output
            String name = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_NAME, null);
            String mac = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MAC, null);
            if (mac != null) {
                String deviceInfo = (name != null ? name : mac);
                appendOutput(deviceInfo + " connected\n");
            }
            statusText.setText("connected");
        } else if (!isConnected && wasConnected) {
            // Just disconnected
            statusText.setText("disconnected");
            appendOutput("disconnected\n");
        } else if (isConnected) {
            statusText.setText("connected");
        } else {
            statusText.setText("disconnected");
        }
    }

    private void pickDevice() {
        if (!ensureBtConnectPermission()) return;
        startActivityForResult(new Intent(this, DevicePickerActivity.class), REQ_PICK_DEVICE);
    }
    
    private void sendMessage() {
        CompanionForegroundService service = CompanionForegroundService.getInstance();
        if (service == null || !service.isConnected()) {
            Toast.makeText(this, "به بلوتوث وصل نیستید", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "لطفاً پیامی وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }
        
        CompanionForegroundService.sendNotify(this, message);
        messageInput.setText(""); // Clear input
    }

    private void openNotificationAccessSettings() {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void requestAllPermissions() {
        // Check what permissions are needed
        boolean needBtPermission = false;
        boolean needNotiPermission = false;
        boolean needNotiListener = false;
        
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                needBtPermission = true;
            }
        }
        
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                needNotiPermission = true;
            }
        }
        
        if (!isNotificationListenerEnabled(this)) {
            needNotiListener = true;
        }
        
        // If no permissions needed, return
        if (!needBtPermission && !needNotiPermission && !needNotiListener) {
            return;
        }
        
        // Create final copies for lambda
        final boolean finalNeedBt = needBtPermission;
        final boolean finalNeedNoti = needNotiPermission;
        final boolean finalNeedNotiListener = needNotiListener;
        
        // Show single dialog explaining permissions, then request them sequentially
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Permissions Required")
                .setMessage("This app needs Bluetooth and Notification permissions to function properly. Please grant all permissions when prompted.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Request permissions sequentially
                    requestPermissionsSequentially(finalNeedBt, finalNeedNoti, finalNeedNotiListener);
                })
                .setCancelable(false)
                .show();
    }
    
    private void requestPermissionsSequentially(boolean needBt, boolean needNoti, boolean needNotiListener) {
        // Request Bluetooth permissions first
        if (needBt && Build.VERSION.SDK_INT >= 31) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 
                REQ_BT_CONNECT);
            // Note: onRequestPermissionsResult will handle continuing with next permission
            return;
        }
        
        // Request notification permission
        if (needNoti && Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                REQ_POST_NOTI);
            return;
        }
        
        // Request notification listener access last
        if (needNotiListener) {
            requestNotificationAccess();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // After Bluetooth permissions, request notification permission
        if (requestCode == REQ_BT_CONNECT) {
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        REQ_POST_NOTI);
                    return;
                }
            }
            // If notification permission already granted or not needed, check notification listener
            if (!isNotificationListenerEnabled(this)) {
                requestNotificationAccess();
            }
            return;
        }
        
        // After notification permission, request notification listener access
        if (requestCode == REQ_POST_NOTI) {
            if (!isNotificationListenerEnabled(this)) {
                requestNotificationAccess();
            }
        }
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
                // Show which device was selected in output box
                String deviceInfo = (name != null ? name : mac);
                appendOutput(deviceInfo + "\n");
                // Automatically connect to selected device
                connectToDevice(mac);
            }
        }
    }
}


