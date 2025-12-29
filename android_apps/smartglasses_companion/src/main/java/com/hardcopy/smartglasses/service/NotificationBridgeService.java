package com.hardcopy.smartglasses.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;

/**
 * Captures notifications (when user grants notification access) and forwards condensed text
 * to the companion transport. Also handles phone calls.
 */
public class NotificationBridgeService extends NotificationListenerService {

    private static PhoneStateListener phoneListener = null;

    @Override
    public void onCreate() {
        super.onCreate();
        // Register phone state listener for incoming calls
        registerPhoneListener();
    }

    @Override
    public void onDestroy() {
        unregisterPhoneListener();
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Check for incoming call notifications
        String packageName = sbn.getPackageName();
        if (packageName != null && (packageName.equals("com.android.server.telecom") || 
            packageName.equals("com.android.phone") ||
            packageName.contains("dialer"))) {
            // This might be a phone call notification
            android.app.Notification notification = sbn.getNotification();
            if (notification != null) {
                CharSequence title = notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
                CharSequence text = notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
                if (title != null || text != null) {
                    String callerInfo = (title != null ? title.toString() : "") + 
                                      (text != null ? " " + text.toString() : "");
                    if (!callerInfo.trim().isEmpty()) {
                        CompanionForegroundService.sendCall(this, callerInfo);
                    }
                }
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optional: could send a "clear" message.
    }

    private void registerPhoneListener() {
        if (phoneListener != null) return; // Already registered
        
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony == null) return;

        // Use PhoneStateListener (works on all Android versions, though deprecated on API 31+)
        phoneListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    String callerInfo = phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "Unknown";
                    CompanionForegroundService.sendCall(CompanionForegroundService.getInstance(), callerInfo);
                }
            }
        };
        
        try {
            telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
            android.util.Log.e("NotificationBridge", "Failed to register phone listener: " + e.getMessage());
            phoneListener = null;
        }
    }

    private void unregisterPhoneListener() {
        if (phoneListener == null) return;
        
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony != null) {
            try {
                telephony.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
            } catch (Exception e) {
                android.util.Log.e("NotificationBridge", "Failed to unregister phone listener: " + e.getMessage());
            }
        }
        phoneListener = null;
    }
}



