package com.wolza.arduinoapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ArduinoControlActivity extends AppCompatActivity {

    private TextView tvMoistureValue;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private InputStream inputStream;
    private Handler handler = new Handler();

    private String deviceAddress;
    private boolean isConnected = false;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_BLUETOOTH_CONNECT = 1;
    private StringBuilder dataBuffer = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_control);

        tvMoistureValue = findViewById(R.id.tvMoistureValue);
        deviceAddress = getIntent().getStringExtra("device_address");

        checkPermissionsAndConnect();
    }

    private void checkPermissionsAndConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_CONNECT);
                return;
            }
        }
        connectToDevice();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToDevice();
            } else {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectToDevice() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

                    // Fix: Cancel discovery before connecting
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }

                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();

                    inputStream = bluetoothSocket.getInputStream();
                    isConnected = true;

                    runOnUiThread(() -> Toast.makeText(ArduinoControlActivity.this,
                            "Connected!", Toast.LENGTH_SHORT).show());

                    listenForData();

                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ArduinoControlActivity.this,
                            "Connection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } catch (SecurityException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ArduinoControlActivity.this,
                            "Bluetooth permission error", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void listenForData() {
        byte[] buffer = new byte[1024];
        int bytesRead;

        while (isConnected) {
            try {
                bytesRead = inputStream.read(buffer);

                if (bytesRead > 0) {
                    String receivedData = new String(buffer, 0, bytesRead);
                    dataBuffer.append(receivedData);

                    String bufferStr = dataBuffer.toString();

                    int newlineIndex;
                    while ((newlineIndex = bufferStr.indexOf("\n")) != -1) {

                        String line = bufferStr.substring(0, newlineIndex).trim();
                        bufferStr = bufferStr.substring(newlineIndex + 1);

                        if (!line.isEmpty()) {
                            try {
                                int value = Integer.parseInt(line);

                                handler.post(() -> {
                                    tvMoistureValue.setText(value + "%");
                                });

                            } catch (NumberFormatException ignored) {
                                // Ignore broken data
                            }
                        }
                    }

                    dataBuffer = new StringBuilder(bufferStr);
                }

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isConnected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}