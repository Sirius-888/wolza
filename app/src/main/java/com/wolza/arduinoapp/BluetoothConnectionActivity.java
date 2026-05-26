package com.wolza.arduinoapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Set;

public class BluetoothConnectionActivity extends AppCompatActivity {

    private ListView listViewDevices;
    private Button btnScan, btnEnableBluetooth;
    private TextView tvStatus, tvNoDevices;
    private ProgressBar progressBar;
    private ImageView btnBack;

    private BluetoothAdapter bluetoothAdapter;
    private DeviceListAdapter deviceListAdapter;
    private ArrayList<BluetoothDevice> discoveredDevices;
    private ArrayList<String> deviceNames;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;
    private static final int REQUEST_LOCATION_PERMISSIONS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        initViews();
        setupToolbar();
        initBluetooth();
        setupClickListeners();
        registerBluetoothReceiver();
    }

    private void initViews() {
        listViewDevices = findViewById(R.id.listViewDevices);
        btnScan = findViewById(R.id.btnScan);
        btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        tvStatus = findViewById(R.id.tvStatus);
        tvNoDevices = findViewById(R.id.tvNoDevices);
        progressBar = findViewById(R.id.progressBar);
        btnBack = findViewById(R.id.btnBack);

        discoveredDevices = new ArrayList<>();
        deviceNames = new ArrayList<>();

        // FIX 1: Pass discoveredDevices to adapter constructor
        deviceListAdapter = new DeviceListAdapter(this, deviceNames, discoveredDevices,
                new DeviceListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(BluetoothDevice device) {
                        // This will be called when ANY part of the item is clicked
                        connectToDevice(device);
                    }
                });
        listViewDevices.setAdapter(deviceListAdapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Connect to Arduino");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            tvStatus.setText("❌ Bluetooth not supported on this device");
            btnEnableBluetooth.setEnabled(false);
            btnScan.setEnabled(false);
            return;
        }

        checkPermissions();
    }

    private boolean checkBluetoothPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.BLUETOOTH_SCAN,
                                android.Manifest.permission.BLUETOOTH_CONNECT
                        },
                        REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                checkLocationPermission();
            }
        } else {
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_LOCATION_PERMISSIONS);
            } else {
                checkBluetoothEnabled();
            }
        } else {
            checkBluetoothEnabled();
        }
    }

    private void checkBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled()) {
            tvStatus.setText("⚠️ Bluetooth is disabled");
            btnEnableBluetooth.setVisibility(View.VISIBLE);
            btnScan.setVisibility(View.GONE);
            listViewDevices.setVisibility(View.GONE);
            tvNoDevices.setVisibility(View.GONE);
        } else {
            tvStatus.setText("✓ Bluetooth is enabled");
            btnEnableBluetooth.setVisibility(View.GONE);
            btnScan.setVisibility(View.VISIBLE);
            listViewDevices.setVisibility(View.VISIBLE);
            showPairedDevices();
        }
    }

    private void showPairedDevices() {
        discoveredDevices.clear();
        deviceNames.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                return;
            }
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            tvStatus.setText("📱 Paired Devices:");
            for (BluetoothDevice device : pairedDevices) {
                discoveredDevices.add(device);

                String deviceName = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                        deviceName = device.getName();
                    }
                } else {
                    deviceName = device.getName();
                }
                if (deviceName == null) deviceName = "Unknown";

                String deviceAddress = device.getAddress();
                String deviceInfo = deviceName + "\n" + deviceAddress;
                deviceNames.add(deviceInfo);
            }
            deviceListAdapter.notifyDataSetChanged();
            tvNoDevices.setVisibility(View.GONE);
        } else {
            tvNoDevices.setVisibility(View.VISIBLE);
            tvNoDevices.setText("No paired devices found. Scan for HC-06");
        }
    }

    private void setupClickListeners() {
        btnEnableBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableBluetooth();
            }
        });

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBluetoothScan();
            }
        });
    }

    private void enableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        // FIX 2: startActivityForResult is correct
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void startBluetoothScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                Toast.makeText(this, "Bluetooth scan permission required", Toast.LENGTH_SHORT).show();
                checkPermissions();
                return;
            }
        }

        discoveredDevices.clear();
        deviceNames.clear();
        deviceListAdapter.notifyDataSetChanged();

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("🔍 Scanning for HC-06 devices...");
        tvNoDevices.setVisibility(View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
            }
        } else {
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
        }

        bluetoothAdapter.startDiscovery();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                    }
                } else {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                }
                progressBar.setVisibility(View.GONE);

                if (discoveredDevices.isEmpty()) {
                    tvNoDevices.setVisibility(View.VISIBLE);
                    tvNoDevices.setText("No HC-06 devices found. Make sure your Arduino is powered on.");
                }
            }
        }, 12000);
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                        deviceName = device.getName();
                    }
                } else {
                    deviceName = device.getName();
                }
                if (deviceName == null) deviceName = "Unknown";

                if (deviceName != null && (deviceName.contains("HC-06") || deviceName.contains("HC-05") || deviceName.contains("Arduino"))) {

                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        String deviceInfo = deviceName + "\n" + device.getAddress() + " ✓ HC-06";
                        deviceNames.add(deviceInfo);
                        deviceListAdapter.notifyDataSetChanged();
                        tvNoDevices.setVisibility(View.GONE);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                progressBar.setVisibility(View.GONE);
                if (discoveredDevices.isEmpty()) {
                    tvNoDevices.setVisibility(View.VISIBLE);
                    tvNoDevices.setText("No HC-06 devices found. Check your Arduino power.");
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        String deviceName = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_CONNECT)) {
                deviceName = device.getName();
            }
        } else {
            deviceName = device.getName();
        }
        if (deviceName == null) deviceName = "HC-06";

        // Just open the next page directly
        Toast.makeText(this, "Opening monitor for " + deviceName, Toast.LENGTH_SHORT).show();

        // Go to ArduinoControlActivity
        Intent intent = new Intent(BluetoothConnectionActivity.this, ArduinoControlActivity.class);
        intent.putExtra("device_name", deviceName);
        intent.putExtra("device_address", device.getAddress());
        startActivity(intent);
        finish();
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
            } else {
                Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_LONG).show();
                finish();
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothEnabled();
            } else {
                Toast.makeText(this, "Location permission required for Bluetooth scanning",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                checkBluetoothEnabled();
            } else {
                Toast.makeText(this, "Bluetooth is required for Arduino connection",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothAdapter != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (checkBluetoothPermission(android.Manifest.permission.BLUETOOTH_SCAN)) {
                        if (bluetoothAdapter.isDiscovering()) {
                            bluetoothAdapter.cancelDiscovery();
                        }
                    }
                } else {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                }
            }
            unregisterReceiver(bluetoothReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}