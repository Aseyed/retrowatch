package com.hardcopy.smartglasses.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * Captures notifications (when user grants notification access) and forwards condensed text
 * to the companion transport.
 *
 * NOTE: This is a stub skeleton. We'll route events into the foreground service after we add
 * a small shared dispatcher (or bind to the service).
 */
public class NotificationBridgeService extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // TODO: Extract title/text safely; rate-limit; filter noisy ongoing notifications.
        // TODO: Forward to transport (CompanionForegroundService) via a shared singleton/queue.
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Optional: could send a "clear" message.
    }
}



