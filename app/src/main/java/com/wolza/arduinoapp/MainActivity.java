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
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.RatingBar;
import android.widget.EditText;
import android.widget.Button;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

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
    private final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private final String[] MQTT_TOPICS = {
        "home/plants/moisture",
        "home/plants/temperature",
        "home/plants/humidity"
    };

    // Telemetry fields for main dashboard
    private String lastMoisture = "--";
    private String lastTemperature = "--";
    private String lastHumidity = "--";

    private void updateMainStatusDisplay(String source) {
        if (tvMoistureDisplay == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(source).append(": 🌱 ").append(lastMoisture);
        if (!lastMoisture.equals("--")) {
            sb.append("%");
        }
        sb.append(" | 🌡️ ").append(lastTemperature);
        if (!lastTemperature.equals("--")) {
            sb.append("°C");
        }
        sb.append(" | 💧 ").append(lastHumidity);
        if (!lastHumidity.equals("--")) {
            sb.append("%");
        }
        tvMoistureDisplay.setText(sb.toString());
    }

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
                        final String payload = new String(message.getPayload()).trim();
                        runOnUiThread(() -> {
                            if (topic.equals("home/plants/moisture")) {
                                lastMoisture = payload;
                                try {
                                    int moistureValue = Integer.parseInt(payload);
                                    MoistureData moistureData = new MoistureData(moistureValue, 0);
                                    if (pbMoistureMain != null) {
                                        pbMoistureMain.setProgress(moistureValue);
                                        pbMoistureMain.setProgressTintList(android.content.res.ColorStateList.valueOf(moistureData.getStatusColor()));
                                    }
                                } catch (NumberFormatException ignored) {}
                            } else if (topic.equals("home/plants/temperature")) {
                                lastTemperature = payload;
                            } else if (topic.equals("home/plants/humidity")) {
                                lastHumidity = payload;
                            }
                            updateMainStatusDisplay("MQTT");
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
                client.subscribe(MQTT_TOPICS);
                mqttClient = client;

                runOnUiThread(() -> {
                    if (tvMoistureDisplay != null) tvMoistureDisplay.setText("MQTT: connected, waiting for data...");
                });
            } catch (MqttException e) {
                e.printStackTrace();
                Throwable cause = e.getCause();
                String detail = "rc=" + e.getReasonCode()
                        + " msg=" + e.getMessage()
                        + (cause != null ? " cause=" + cause.getClass().getSimpleName() + ":" + cause.getMessage() : "");
                runOnUiThread(() -> {
                    if (tvMoistureDisplay != null) tvMoistureDisplay.setText("MQTT error: " + detail);
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

                            lastMoisture = String.valueOf(moistureValue);
                            updateMainStatusDisplay("Bluetooth");

                            if (pbMoistureMain != null) {
                                MoistureData moistureData = new MoistureData(moistureValue, 0);
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
            getSupportActionBar().setTitle(R.string.app_name);
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
        cardCare.setOnClickListener(v -> startActivity(new Intent(this, AutoCareActivity.class)));
        cardDoctor.setOnClickListener(v -> openImagePicker());
        cardMarket.setOnClickListener(v -> startActivity(new Intent(this, MarketActivity.class)));
        cardCommunity.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class)));
        cardMyPlant.setOnClickListener(v -> startActivity(new Intent(this, MoistureDetailActivity.class)));
        cardWifiData.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing WiFi data...", Toast.LENGTH_SHORT).show();
            startMQTT();
        });
        cardReminders.setOnClickListener(v -> startActivity(new Intent(this, MyGardenActivity.class)));
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
        String[] options = {getString(R.string.manual_search_option), getString(R.string.auto_identify_option)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.search_flowers_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) startActivity(new Intent(this, FlowerListActivity.class));
                    else startActivity(new Intent(this, AutoSearchActivity.class));
                }).show();
    }

    private void loadUserData() {
        if (currentUser != null) {
            if (tvWelcomeMessage != null) {
                String name = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "User";
                tvWelcomeMessage.setText(getString(R.string.welcome_message, name));
            }
            if (tvUserEmail != null) tvUserEmail.setText(currentUser.getEmail());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Always highlight Home when this screen is visible
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_garden) startActivity(new Intent(this, MyGardenActivity.class));
        else if (id == R.id.nav_history) startActivity(new Intent(this, HistoryActivity.class));
        else if (id == R.id.nav_community) startActivity(new Intent(this, CommunityActivity.class));
        else if (id == R.id.nav_logout) logout();
        else if (id == R.id.nav_about) startActivity(new Intent(this, AboutActivity.class));
        else if (id == R.id.nav_rate) showRateUsDialog();
        else if (id == R.id.nav_lang_en) {
            updateLanguage("en");
        } else if (id == R.id.nav_lang_ru) {
            updateLanguage("ru");
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showRateUsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rate_us, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etSuggestion = dialogView.findViewById(R.id.etSuggestion);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmit);
        Button btnViewAll = dialogView.findViewById(R.id.btnViewAll);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnSubmit.setOnClickListener(v -> {
            String suggestion = etSuggestion.getText().toString().trim();
            float rating = ratingBar.getRating();

            if (suggestion.isEmpty()) {
                Toast.makeText(this, "Please enter your suggestion", Toast.LENGTH_SHORT).show();
                return;
            }

            String userName = "Guest User";
            if (currentUser != null) {
                if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                    userName = currentUser.getDisplayName();
                } else if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                    userName = currentUser.getEmail().split("@")[0];
                }
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> feedback = new HashMap<>();
            feedback.put("userName", userName);
            feedback.put("userId", currentUser != null ? currentUser.getUid() : "anonymous");
            feedback.put("rating", rating);
            feedback.put("suggestion", suggestion);
            feedback.put("timestamp", System.currentTimeMillis());

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Submitting...");

            db.collection("feedbacks")
                    .add(feedback)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnViewAll.setOnClickListener(v -> {
            dialog.dismiss();
            showAllSuggestionsDialog();
        });

        dialog.show();
    }

    private void showAllSuggestionsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_view_all_suggestions, null);
        RecyclerView rvSuggestions = dialogView.findViewById(R.id.rvSuggestions);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView tvNoSuggestions = dialogView.findViewById(R.id.tvNoSuggestions);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        progressBar.setVisibility(View.VISIBLE);
        rvSuggestions.setVisibility(View.GONE);
        tvNoSuggestions.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("feedbacks")
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<SuggestionItem> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            SuggestionItem item = doc.toObject(SuggestionItem.class);
                            list.add(item);
                        }

                        // Sort locally by timestamp descending
                        list.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                        if (list.isEmpty()) {
                            tvNoSuggestions.setVisibility(View.VISIBLE);
                        } else {
                            rvSuggestions.setVisibility(View.VISIBLE);
                            SuggestionsAdapter adapter = new SuggestionsAdapter(list);
                            rvSuggestions.setAdapter(adapter);
                            rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
                        }
                    } else {
                        String errMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to load suggestions: " + errMsg, Toast.LENGTH_SHORT).show();
                    }
                });

        dialog.show();
    }

    private void updateLanguage(String lang) {
        LocaleHelper.setLocale(this, lang);
        Intent intent = getIntent();
        finish();
        startActivity(intent);
        overridePendingTransition(0, 0);
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
