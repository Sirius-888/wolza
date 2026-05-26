package com.wolza.arduinoapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class FlowerDetailActivity extends AppCompatActivity {

    private ImageView imgFlower;
    private TextView tvName, tvScientificName, tvDescription;
    private Button btnSelect;
    private ImageView btnBack;
    private Toolbar toolbar;

    private FlowerListActivity.Flower flower;
    private SharedPreferences sharedPreferences;
    private List<MyGardenActivity.GardenPlant> myGardenList;

    private static final String PREFS_NAME = "MyGardenPrefs";
    private static final String KEY_GARDEN_PLANTS = "garden_plants";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flower_detail);

        initViews();
        setupToolbar();
        getFlowerData();
        setupSharedPreferences();
        setupClickListeners();
        displayFlowerDetails();
    }

    private void initViews() {
        imgFlower = findViewById(R.id.imgFlower);
        tvName = findViewById(R.id.tvName);
        tvScientificName = findViewById(R.id.tvScientificName);
        tvDescription = findViewById(R.id.tvDescription);
        btnSelect = findViewById(R.id.btnSelect);
        btnBack = findViewById(R.id.btnBack);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Flower Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void getFlowerData() {
        // Get flower object from intent
        flower = (FlowerListActivity.Flower) getIntent().getSerializableExtra("flower");
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadMyGardenList();
    }

    private void loadMyGardenList() {
        String json = sharedPreferences.getString(KEY_GARDEN_PLANTS, "");
        if (json.isEmpty()) {
            myGardenList = new ArrayList<>();
        } else {
            Gson gson = new Gson();
            Type type = new TypeToken<List<MyGardenActivity.GardenPlant>>() {}.getType();
            myGardenList = gson.fromJson(json, type);
        }
    }

    private void saveMyGardenList() {
        Gson gson = new Gson();
        String json = gson.toJson(myGardenList);
        sharedPreferences.edit().putString(KEY_GARDEN_PLANTS, json).apply();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSelect.setOnClickListener(v -> {
            if (isFlowerInGarden()) {
                removeFromGarden();
            } else {
                addToGarden();
            }
        });
    }

    private boolean isFlowerInGarden() {
        for (MyGardenActivity.GardenPlant plant : myGardenList) {
            if (plant.getFlower().getName().equals(flower.getName())) {
                return true;
            }
        }
        return false;
    }

    private void addToGarden() {
        // Check if already in garden
        for (MyGardenActivity.GardenPlant plant : myGardenList) {
            if (plant.getFlower().getName().equals(flower.getName())) {
                Toast.makeText(this, "Already in your garden!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create new garden plant
        MyGardenActivity.GardenPlant newPlant = new MyGardenActivity.GardenPlant(flower);
        myGardenList.add(newPlant);
        saveMyGardenList();

        btnSelect.setText("🌿 In Garden");
        btnSelect.setBackgroundColor(getColor(android.R.color.holo_green_light));
        Toast.makeText(this, "🌱 " + flower.getName() + " planted in your garden!", Toast.LENGTH_SHORT).show();
    }

    private void removeFromGarden() {
        // Remove the flower from garden
        for (int i = 0; i < myGardenList.size(); i++) {
            if (myGardenList.get(i).getFlower().getName().equals(flower.getName())) {
                myGardenList.remove(i);
                break;
            }
        }
        saveMyGardenList();

        btnSelect.setText("🌸 Add to Garden");
        btnSelect.setBackgroundColor(getColor(android.R.color.holo_orange_light));
        Toast.makeText(this, flower.getName() + " removed from garden", Toast.LENGTH_SHORT).show();
    }

    private void displayFlowerDetails() {
        if (flower != null) {
            tvName.setText(flower.getName());
            tvScientificName.setText(flower.getScientificName());
            tvDescription.setText(flower.getDescription());

            // Load image
            if (flower.getImageUrl() != null && !flower.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(flower.getImageUrl())
                        .placeholder(R.drawable.ic_flower)
                        .error(R.drawable.ic_flower)
                        .into(imgFlower);
            }

            // Update button state
            if (isFlowerInGarden()) {
                btnSelect.setText("🌿 In Garden");
                btnSelect.setBackgroundColor(getColor(android.R.color.holo_green_light));
            } else {
                btnSelect.setText("🌸 Add to Garden");
                btnSelect.setBackgroundColor(getColor(android.R.color.holo_orange_light));
            }
        }
    }
}