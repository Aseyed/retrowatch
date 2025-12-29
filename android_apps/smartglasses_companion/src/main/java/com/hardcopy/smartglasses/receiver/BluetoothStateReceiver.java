package com.hardcopy.smartglasses.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives Bluetooth adapter state changes.
 * In a production app we'd trigger reconnect attempts when BT becomes ON.
 */
public class BluetoothStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            // TODO: if STATE_ON -> poke foreground service to reconnect
        }
    }
}



