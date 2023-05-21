package io.github.doorstepdefender;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Log.e("MainActivity", "Enabling Bluetooth");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (!BluetoothService.getIsRunning()) {
            // Start Bluetooth service.
            Intent serviceIntent = new Intent(this, BluetoothService.class);
            startForegroundService(serviceIntent);
        }
    }
}