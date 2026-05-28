package com.wolza.arduinoapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TextView tvWelcomeMessage, tvUserEmail, tvMoistureDisplay;
    private ProgressBar pbMoistureMain;
    private CircleImageView profileImage;

    private View cardSearch, cardCare, cardDoctor, cardMarket, cardCommunity, cardMyPlant, cardWifiData, cardReminders;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private BluetoothService bluetoothService;
    private boolean isBound = false;

    // MQTT variables
    private MqttClient mqttClient;
    private final String MQTT_TOPIC = "home/plants/moisture";
    private final String BROKER_URL = "tcp://broker.hivemq.com:1883";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupToolbar();
        setupNavigationDrawer();
        setupFeatureButtons();
        loadUserData();

        // Bind to Bluetooth service for live moisture data
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Start MQTT connection
        startMQTT();
    }

    private void startMQTT() {
        // Clean up any previous client (Refresh button re-invokes this).
        try {
            if (mqttClient != null && mqttClient.isConnected()) mqttClient.disconnect();
        } catch (MqttException ignored) {}
        mqttClient = null;

        if (tvMoistureDisplay != null) tvMoistureDisplay.setText("MQTT: connecting...");

        // Paho's connect() does blocking network I/O — must run off the UI thread.
        new Thread(() -> {
            try {
                String clientId = MqttClient.generateClientId();
                MqttClient client = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());

                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        runOnUiThread(() -> {
                            if (tvMoistureDisplay != null) tvMoistureDisplay.setText("MQTT Disconnected");
                        });
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        final String payload = new String(message.getPayload());
                        runOnUiThread(() -> {
                            try {
                                int moistureValue = Integer.parseInt(payload.trim());
                                MoistureData moistureData = new MoistureData(moistureValue, 0);
                                if (tvMoistureDisplay != null) {
                                    tvMoistureDisplay.setText("MQTT: " + moistureValue + "% (" + moistureData.getMoistureStatus() + ")");
                                }
                                if (pbMoistureMain != null) {
                                    pbMoistureMain.setProgress(moistureValue);
                                    pbMoistureMain.setProgressTintList(android.content.res.ColorStateList.valueOf(moistureData.getStatusColor()));
                                }
                            } catch (NumberFormatException e) {
                                if (tvMoistureDisplay != null) tvMoistureDisplay.setText("Soil: " + payload);
                            }
                        });
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                MqttConnectOptions opts = new MqttConnectOptions();
                opts.setAutomaticReconnect(true);
                opts.setCleanSession(true);
                opts.setConnectionTimeout(10);
                opts.setKeepAliveInterval(30);

                client.connect(opts);
                client.subscribe(MQTT_TOPIC);
                mqttClient = client;

                runOnUiThread(() -> {
                    if (tvMoistureDisplay != null) tvMoistureDisplay.setText("MQTT: connected, waiting for data...");
                });
            } catch (MqttException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (tvMoistureDisplay != null) tvMoistureDisplay.setText("MQTT error: " + e.getMessage());
                });
            }
        }, "mqtt-init").start();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            isBound = true;
            setupBluetoothListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void setupBluetoothListener() {
        if (isBound && bluetoothService != null) {
            bluetoothService.setListener(new BluetoothService.DataReceivedListener() {
                @Override
                public void onDataReceived(String data) {
                    runOnUiThread(() -> {
                        try {
                            String cleanData = data.trim();
                            int moistureValue = 0;
                            if (cleanData.contains("%")) moistureValue = Integer.parseInt(cleanData.replace("%", ""));
                            else moistureValue = Integer.parseInt(cleanData);

                            MoistureData moistureData = new MoistureData(moistureValue, 0);
                            if (tvMoistureDisplay != null) {
                                tvMoistureDisplay.setText("Bluetooth: " + moistureValue + "% (" + moistureData.getMoistureStatus() + ")");
                            }
                            if (pbMoistureMain != null) {
                                pbMoistureMain.setProgress(moistureValue);
                                pbMoistureMain.setProgressTintList(android.content.res.ColorStateList.valueOf(moistureData.getStatusColor()));
                            }
                        } catch (Exception ignored) {}
                    });
                }

                @Override
                public void onConnectionStatusChanged(boolean connected, String message) {
                    runOnUiThread(() -> {
                        if (!connected && tvMoistureDisplay != null) {
                            if (!tvMoistureDisplay.getText().toString().startsWith("MQTT")) {
                                tvMoistureDisplay.setText("Sensor Offline");
                                if (pbMoistureMain != null) pbMoistureMain.setProgress(0);
                            }
                        }
                    });
                }
            });
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        cardSearch = findViewById(R.id.cardSearch);
        cardCare = findViewById(R.id.cardCare);
        cardDoctor = findViewById(R.id.cardDoctor);
        cardMarket = findViewById(R.id.cardMarket);
        cardCommunity = findViewById(R.id.cardCommunity);
        cardMyPlant = findViewById(R.id.cardMyPlant);
        cardWifiData = findViewById(R.id.cardWifiData);
        cardReminders = findViewById(R.id.cardReminders);

        tvMoistureDisplay = findViewById(R.id.tvMoistureDisplay);
        pbMoistureMain = findViewById(R.id.pbMoistureMain);

        View headerView = navigationView.getHeaderView(0);
        tvWelcomeMessage = headerView.findViewById(R.id.tvWelcomeMessage);
        tvUserEmail = headerView.findViewById(R.id.tvUserEmail);
        profileImage = headerView.findViewById(R.id.profile_image);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Garden Assistant");
        }
    }

    private void setupNavigationDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupFeatureButtons() {
        cardSearch.setOnClickListener(v -> showSearchOptions());
        cardCare.setOnClickListener(v -> showCareOptions());
        cardDoctor.setOnClickListener(v -> openImagePicker());
        cardMarket.setOnClickListener(v -> startActivity(new Intent(this, MarketActivity.class)));
        cardCommunity.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class)));
        cardMyPlant.setOnClickListener(v -> startActivity(new Intent(this, BluetoothConnectionActivity.class)));
        cardWifiData.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing WiFi data...", Toast.LENGTH_SHORT).show();
            startMQTT();
        });
        cardReminders.setOnClickListener(v -> startActivity(new Intent(this, MyGardenActivity.class)));
    }

    private void showCareOptions() {
        String[] options = {"📋 Smart Tracker", "📅 Vacation Mode"};
        new AlertDialog.Builder(this)
                .setTitle("Plant Care")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startActivity(new Intent(this, MyGardenActivity.class));
                    else startActivity(new Intent(this, AutoCareActivity.class));
                }).show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 300);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 300 && resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                String base64Image = ImageUtils.bitmapToBase64(bitmap, 70);
                Intent intent = new Intent(this, DoctorActivity.class);
                intent.putExtra("image", base64Image);
                startActivity(intent);
            } catch (IOException e) {
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSearchOptions() {
        String[] options = {"📖 Manual Search", "📸 Auto Identify"};
        new AlertDialog.Builder(this)
                .setTitle("Search Flowers")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startActivity(new Intent(this, FlowerListActivity.class));
                    else startActivity(new Intent(this, AutoSearchActivity.class));
                }).show();
    }

    private void loadUserData() {
        if (currentUser != null) {
            if (tvWelcomeMessage != null) tvWelcomeMessage.setText("Welcome, " + (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User") + "!");
            if (tvUserEmail != null) tvUserEmail.setText(currentUser.getEmail());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_garden || id == R.id.nav_reminders) startActivity(new Intent(this, MyGardenActivity.class));
        else if (id == R.id.nav_history) startActivity(new Intent(this, HistoryActivity.class));
        else if (id == R.id.nav_community) startActivity(new Intent(this, CommunityActivity.class));
        else if (id == R.id.nav_smart_watering) startActivity(new Intent(this, AutoCareActivity.class));
        else if (id == R.id.nav_logout) logout();

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        try {
            if (mqttClient != null && mqttClient.isConnected()) mqttClient.disconnect();
        } catch (MqttException e) { e.printStackTrace(); }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
}