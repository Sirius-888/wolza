package com.wolza.arduinoapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyGardenActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty, tvGardenStats;
    private ImageView btnBack;
    private FloatingActionButton fabAddFlower;
    private GardenAdapter adapter;

    private List<GardenPlant> gardenPlants;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "MyGardenPrefs";
    private static final String KEY_GARDEN_PLANTS = "garden_plants";
    private static final int REQUEST_SCAN_ADD = 101;
    private static final int REQUEST_DOCTOR_PICKER = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_garden);

        initViews();
        setupToolbar();
        setupFab();
        loadGardenPlants();
        setupRecyclerView();
        updateGardenStats();
        
        requestNotificationPermission();
        scheduleWateringReminders();
        
        handleIntent(getIntent());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("open_plant_name")) {
            String plantName = intent.getStringExtra("open_plant_name");
            for (int i = 0; i < gardenPlants.size(); i++) {
                if (gardenPlants.get(i).getFlower().getName().equalsIgnoreCase(plantName)) {
                    showWaterDialog(i);
                    break;
                }
            }
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvGardenStats = findViewById(R.id.tvGardenStats);
        btnBack = findViewById(R.id.btnBack);
        fabAddFlower = findViewById(R.id.fabAddFlower);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("🌻 My Garden");
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupFab() {
        fabAddFlower.setOnClickListener(v -> showAddFlowerDialog());
    }

    private void scheduleWateringReminders() {
        PeriodicWorkRequest wateringWorkRequest =
                new PeriodicWorkRequest.Builder(WateringWorker.class, 12, TimeUnit.HOURS)
                        .setInitialDelay(1, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "WateringReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                wateringWorkRequest
        );
    }

    private void showAddFlowerDialog() {
        String[] options = {"📋 Choose from List", "📸 Scan Flower (AI)"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Add to Garden")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showListAddDialog();
                    else {
                        Intent intent = new Intent(this, AutoSearchActivity.class);
                        intent.putExtra("mode", "add_to_garden");
                        startActivityForResult(intent, REQUEST_SCAN_ADD);
                    }
                }).show();
    }

    private void showListAddDialog() {
        List<FlowerListActivity.Flower> allFlowers = loadFlowersFromJSON();
        String[] names = new String[allFlowers.size()];
        for (int i = 0; i < allFlowers.size(); i++) names[i] = allFlowers.get(i).getName();

        new android.app.AlertDialog.Builder(this)
                .setTitle("Select Flower")
                .setItems(names, (dialog, which) -> addPlantToGarden(allFlowers.get(which), null))
                .show();
    }

    private void addPlantToGarden(FlowerListActivity.Flower flower, String base64Image) {
        GardenPlant newPlant = new GardenPlant(flower);
        fetchWateringData(newPlant);
        if (base64Image != null) {
            newPlant.setCustomImageBase64(base64Image);
            newPlant.setVerified(true);
        }
        gardenPlants.add(newPlant);
        saveGardenPlants();
        adapter.updateList(gardenPlants);
        updateEmptyState();
        updateGardenStats();
    }

    private void fetchWateringData(GardenPlant plant) {
        try {
            InputStream is = getAssets().open("watering_info.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject obj = new JSONObject(new String(buffer, "UTF-8"));
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                JSONObject flowerObj = obj.getJSONObject(keys.next());
                if (flowerObj.getString("name").equalsIgnoreCase(plant.getFlower().getName())) {
                    plant.setWeeklyFrequency(flowerObj.getInt("weekly_frequency"));
                    JSONArray planArray = flowerObj.getJSONArray("weekly_plan");
                    boolean[] plan = new boolean[7];
                    for (int i = 0; i < 7; i++) plan[i] = planArray.getBoolean(i);
                    plant.setWeeklyPlan(plan);
                    return;
                }
            }
        } catch (Exception ignored) {}
    }

    private void openDoctorImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_DOCTOR_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_SCAN_ADD) {
                String name = data.getStringExtra("identified_plant");
                String img = data.getStringExtra("image");
                if (name != null) {
                    FlowerListActivity.Flower f = new FlowerListActivity.Flower();
                    f.setName(name);
                    addPlantToGarden(f, img);
                }
            } else if (requestCode == REQUEST_DOCTOR_PICKER) {
                // Copy the way from MainActivity
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
    }

    private List<FlowerListActivity.Flower> loadFlowersFromJSON() {
        try {
            InputStream inputStream = getAssets().open("flowers.json");
            return new Gson().fromJson(new InputStreamReader(inputStream), new TypeToken<List<FlowerListActivity.Flower>>(){}.getType());
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private void loadGardenPlants() {
        String json = sharedPreferences.getString(KEY_GARDEN_PLANTS, "");
        if (json.isEmpty()) gardenPlants = new ArrayList<>();
        else gardenPlants = new Gson().fromJson(json, new TypeToken<List<GardenPlant>>(){}.getType());
        for (GardenPlant p : gardenPlants) {
            p.updateGrowth();
            if (p.getWeeklyPlan() == null) fetchWateringData(p);
        }
    }

    private void saveGardenPlants() {
        sharedPreferences.edit().putString(KEY_GARDEN_PLANTS, new Gson().toJson(gardenPlants)).apply();
    }

    private void setupRecyclerView() {
        adapter = new GardenAdapter(this, gardenPlants);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(this::showWaterDialog);
        adapter.setOnDeleteClickListener(pos -> {
            gardenPlants.remove(pos);
            saveGardenPlants();
            adapter.updateList(gardenPlants);
            updateEmptyState();
            updateGardenStats();
        });
    }

    private void showWaterDialog(int position) {
        GardenPlant plant = gardenPlants.get(position);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_water_plant, null);
        ImageView img = dialogView.findViewById(R.id.imgPlant);
        TextView name = dialogView.findViewById(R.id.tvPlantName);
        TextView planText = dialogView.findViewById(R.id.tvWateringInstructions);
        Button btnWater = dialogView.findViewById(R.id.btnWater);
        Button btnCheckHealth = dialogView.findViewById(R.id.btnCheckHealth);

        name.setText(plant.getFlower().getName());
        planText.setText("Weekly Plan: " + plant.getWeeklyFrequency() + " sessions");

        // Watering Graphic
        View[] bars = {
                dialogView.findViewById(R.id.bar0), dialogView.findViewById(R.id.bar1),
                dialogView.findViewById(R.id.bar2), dialogView.findViewById(R.id.bar3),
                dialogView.findViewById(R.id.bar4), dialogView.findViewById(R.id.bar5),
                dialogView.findViewById(R.id.bar6)
        };
        boolean[] plan = plant.getWeeklyPlan();
        if (plan != null) {
            for (int i = 0; i < 7; i++) {
                bars[i].setBackgroundColor(plan[i] ? 0xFF2196F3 : 0xFFE0E0E0);
            }
        }

        if (plant.getCustomImageBase64() != null) {
            byte[] decoded = Base64.decode(plant.getCustomImageBase64(), Base64.DEFAULT);
            Glide.with(this).load(decoded).centerCrop().into(img);
        } else if (plant.getFlower().getImageUrl() != null && !plant.getFlower().getImageUrl().isEmpty()) {
            Glide.with(this).load(plant.getFlower().getImageUrl()).placeholder(R.drawable.ic_flower).into(img);
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(dialogView).create();
        
        btnWater.setOnClickListener(v -> {
            plant.water();
            saveGardenPlants();
            adapter.notifyItemChanged(position);
            updateGardenStats();
            dialog.dismiss();
        });

        btnCheckHealth.setOnClickListener(v -> {
            dialog.dismiss();
            openDoctorImagePicker();
        });

        dialog.show();
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(gardenPlants.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(gardenPlants.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateGardenStats() {
        if (tvGardenStats != null) tvGardenStats.setText("🌱 " + gardenPlants.size() + " plants in garden");
    }

    public static class GardenPlant {
        private FlowerListActivity.Flower flower;
        private long lastWateredTimestamp, plantedDate;
        private float growthProgress;
        private boolean isVerified;
        private int weeklyFrequency;
        private boolean[] weeklyPlan;
        private String customImageBase64;

        public GardenPlant(FlowerListActivity.Flower flower) {
            this.flower = flower;
            this.plantedDate = System.currentTimeMillis();
            this.weeklyFrequency = 3;
        }

        public FlowerListActivity.Flower getFlower() { return flower; }
        public float getGrowthProgress() { return growthProgress; }
        public boolean isVerified() { return isVerified; }
        public void setVerified(boolean verified) { isVerified = verified; }
        public int getWeeklyFrequency() { return weeklyFrequency; }
        public void setWeeklyFrequency(int freq) { this.weeklyFrequency = freq; }
        public boolean[] getWeeklyPlan() { return weeklyPlan; }
        public void setWeeklyPlan(boolean[] plan) { this.weeklyPlan = plan; }
        public String getCustomImageBase64() { return customImageBase64; }
        public void setCustomImageBase64(String b64) { this.customImageBase64 = b64; }

        public void water() {
            lastWateredTimestamp = System.currentTimeMillis();
            growthProgress = Math.min(100, growthProgress + 15);
        }

        public boolean isWateredToday() {
            if (lastWateredTimestamp == 0) return false;
            Calendar today = Calendar.getInstance();
            Calendar last = Calendar.getInstance();
            last.setTimeInMillis(lastWateredTimestamp);
            return today.get(Calendar.YEAR) == last.get(Calendar.YEAR) &&
                   today.get(Calendar.DAY_OF_YEAR) == last.get(Calendar.DAY_OF_YEAR);
        }

        public void updateGrowth() {
            long days = (System.currentTimeMillis() - plantedDate) / (1000 * 60 * 60 * 24);
            growthProgress = Math.max(growthProgress, Math.min(100, days * 10));
        }

        public int getGrowthStage() { return growthProgress < 25 ? 0 : growthProgress < 50 ? 1 : growthProgress < 75 ? 2 : 3; }

        public String getGrowthStatusText() {
            int s = getGrowthStage();
            return s == 0 ? "🌱 Sprout" : s == 1 ? "🌿 Seedling" : s == 2 ? "🌼 Budding" : "🌸 Blooming!";
        }

        public String getLastWateredText() {
            Calendar cal = Calendar.getInstance();
            int dayIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if (weeklyPlan != null && weeklyPlan[dayIndex]) {
                return isWateredToday() ? "Watered! ✅" : "Need water! 💧";
            }
            return isWateredToday() ? "Watered! ✅" : "Off-plan day";
        }
    }

    static class GardenAdapter extends RecyclerView.Adapter<GardenAdapter.GardenViewHolder> {
        private List<GardenPlant> plants;
        private OnItemClickListener listener;
        private OnDeleteClickListener deleteListener;
        private Context context;

        public interface OnItemClickListener { void onItemClick(int pos); }
        public interface OnDeleteClickListener { void onDeleteClick(int pos); }

        public GardenAdapter(Context context, List<GardenPlant> plants) {
            this.context = context;
            this.plants = plants;
        }

        public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }
        public void setOnDeleteClickListener(OnDeleteClickListener l) { this.deleteListener = l; }

        @NonNull
        @Override
        public GardenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new GardenViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_garden_plant, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull GardenViewHolder holder, int position) {
            GardenPlant plant = plants.get(position);
            holder.name.setText(plant.getFlower().getName());
            holder.status.setText(plant.getGrowthStatusText());
            holder.water.setText(plant.getLastWateredText());
            holder.progress.setProgress((int) plant.getGrowthProgress());

            if (plant.getCustomImageBase64() != null) {
                byte[] decoded = Base64.decode(plant.getCustomImageBase64(), Base64.DEFAULT);
                Glide.with(context).load(decoded).centerCrop().into(holder.flowerImg);
            } else if (plant.getFlower().getImageUrl() != null && !plant.getFlower().getImageUrl().isEmpty()) {
                Glide.with(context).load(plant.getFlower().getImageUrl()).placeholder(R.drawable.ic_flower).into(holder.flowerImg);
            } else {
                holder.flowerImg.setImageResource(R.drawable.ic_flower);
            }

            TextView[] dots = {holder.d0, holder.d1, holder.d2, holder.d3, holder.d4, holder.d5, holder.d6};
            boolean[] plan = plant.getWeeklyPlan();
            if (plan != null) {
                for (int i = 0; i < 7; i++) {
                    dots[i].setTextColor(plan[i] ? 0xFF2196F3 : 0xFFBDBDBD);
                    dots[i].setTypeface(null, plan[i] ? Typeface.BOLD : Typeface.NORMAL);
                }
            }

            holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(position); });
            holder.ivDelete.setOnClickListener(v -> { if (deleteListener != null) deleteListener.onDeleteClick(position); });
        }

        @Override
        public int getItemCount() { return plants.size(); }
        public void updateList(List<GardenPlant> nl) { this.plants = nl; notifyDataSetChanged(); }

        static class GardenViewHolder extends RecyclerView.ViewHolder {
            ImageView flowerImg, ivDelete;
            TextView name, status, water, d0, d1, d2, d3, d4, d5, d6;
            ProgressBar progress;
            public GardenViewHolder(View v) {
                super(v);
                flowerImg = v.findViewById(R.id.ivFlower);
                ivDelete = v.findViewById(R.id.ivDelete);
                name = v.findViewById(R.id.tvPlantName);
                status = v.findViewById(R.id.tvGrowthStatus);
                water = v.findViewById(R.id.tvWaterStatus);
                progress = v.findViewById(R.id.progressGrowth);
                d0 = v.findViewById(R.id.day0); d1 = v.findViewById(R.id.day1); d2 = v.findViewById(R.id.day2);
                d3 = v.findViewById(R.id.day3); d4 = v.findViewById(R.id.day4); d5 = v.findViewById(R.id.day5); d6 = v.findViewById(R.id.day6);
            }
        }
    }
}
