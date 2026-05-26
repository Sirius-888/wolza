package com.wolza.arduinoapp;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Handler handler;

    private boolean isConnected = false;
    private String deviceAddress;
    private String deviceName;

    private final IBinder binder = new LocalBinder();
    private StringBuilder dataBuffer = new StringBuilder();

    public interface DataReceivedListener {
        void onDataReceived(String data);
        void onConnectionStatusChanged(boolean connected, String message);
    }

    private DataReceivedListener listener;

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setListener(DataReceivedListener listener) {
        this.listener = listener;
    }

    private boolean checkBluetoothPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public void connectToDevice(String address, String name) {
        this.deviceAddress = address;
        this.deviceName = name;

        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                    notifyStatus(false, "Bluetooth permission denied");
                    return;
                }

                bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
                if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();

                inputStream = bluetoothSocket.getInputStream();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                notifyStatus(true, "Connected to " + deviceName);
                listenForData();

            } catch (IOException e) {
                isConnected = false;
                notifyStatus(false, "Connection failed: " + e.getMessage());
            }
        }).start();
    }

    private void notifyStatus(boolean connected, String message) {
        if (listener != null) {
            handler.post(() -> listener.onConnectionStatusChanged(connected, message));
        }
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
                        
                        if (!line.isEmpty() && listener != null) {
                            final String finalLine = line;
                            handler.post(() -> listener.onDataReceived(finalLine));
                        }
                    }
                    dataBuffer = new StringBuilder(bufferStr);
                }
            } catch (IOException e) {
                isConnected = false;
                notifyStatus(false, "Disconnected");
                break;
            }
        }
    }

    public void sendCommand(String command) {
        if (outputStream != null && isConnected) {
            try {
                outputStream.write(command.getBytes());
            } catch (IOException e) {
                notifyStatus(false, "Failed to send command");
            }
        }
    }

    public boolean isConnected() { return isConnected; }

    public void disconnect() {
        isConnected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException ignored) {}
        notifyStatus(false, "Disconnected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
