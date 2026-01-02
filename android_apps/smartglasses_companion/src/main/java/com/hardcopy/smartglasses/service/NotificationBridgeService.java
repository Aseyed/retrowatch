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
        try {
            String packageName = sbn.getPackageName();
            String title = "";
            String text = "";
            
            // Extract notification content (App Inventor format)
            if (sbn.getNotification() != null) {
                if (sbn.getNotification().extras != null) {
                    CharSequence titleSeq = sbn.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
                    CharSequence textSeq = sbn.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
                    
                    if (titleSeq != null) title = titleSeq.toString();
                    if (textSeq != null) text = textSeq.toString();
                    
                    // Try to get big text if available
                    if (text == null || text.isEmpty()) {
                        CharSequence bigText = sbn.getNotification().extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT);
                        if (bigText != null) {
                            text = bigText.toString();
                        }
                    }
                }
            }
            
            android.util.Log.d("NotificationBridge", "Notification received - Package: " + packageName + ", Title: " + title + ", Text: " + text);
            
            // Forward to CompanionForegroundService in App Inventor format: "N:text:title\n"
            // Only send if we have both title and text, or at least one of them
            if ((title != null && !title.isEmpty()) || (text != null && !text.isEmpty())) {
                if (CompanionForegroundService.getInstance() != null) {
                    CompanionForegroundService.getInstance().forwardNotification(packageName, title, text);
                }
            }
            
            // Also handle phone calls separately
            if (packageName != null && (packageName.equals("com.android.server.telecom") || 
                packageName.equals("com.android.phone") ||
                packageName.contains("dialer"))) {
                if (!title.isEmpty() || !text.isEmpty()) {
                    String callerInfo = (title != null ? title : "") + 
                                      (text != null && !text.isEmpty() ? " " + text : "");
                    if (!callerInfo.trim().isEmpty()) {
                        CompanionForegroundService.sendCall(this, callerInfo);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("NotificationBridge", "Error processing notification: " + e.getMessage(), e);
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



