package com.hardcopy.smartglasses.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.hardcopy.smartglasses.service.CompanionForegroundService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // TODO: gate behind user setting; start service so it can reconnect.
            CompanionForegroundService.start(context);
        }
    }
}



