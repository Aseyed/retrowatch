package com.hardcopy.smartglasses.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.hardcopy.smartglasses.R;

import java.util.ArrayList;
import java.util.Set;

public class DevicePickerActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "device_name";
    public static final String EXTRA_DEVICE_MAC = "device_mac";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_picker);

        ListView list = findViewById(R.id.pairedList);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "No Bluetooth adapter", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= 31 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Grant Bluetooth permission first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        ArrayList<String> rows = new ArrayList<>();
        ArrayList<BluetoothDevice> devices = new ArrayList<>();

        if (bonded != null) {
            for (BluetoothDevice d : bonded) {
                String name = (d.getName() == null) ? "(unknown)" : d.getName();
                rows.add(name + "\n" + d.getAddress());
                devices.add(d);
            }
        }

        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        list.setAdapter(a);

        list.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice d = devices.get(position);
            Intent r = new Intent();
            r.putExtra(EXTRA_DEVICE_NAME, d.getName());
            r.putExtra(EXTRA_DEVICE_MAC, d.getAddress());
            setResult(RESULT_OK, r);
            finish();
        });
    }
}



